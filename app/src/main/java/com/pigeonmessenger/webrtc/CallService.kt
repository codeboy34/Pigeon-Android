package com.pigeonmessenger.webrtc

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.google.gson.Gson
import com.pigeonmessenger.Session
import com.pigeonmessenger.activities.App
import com.pigeonmessenger.activities.CallActivity
import com.pigeonmessenger.api.AccountService
import com.pigeonmessenger.crypto.Base64
import com.pigeonmessenger.database.room.daos.MessageDao
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.extension.nowInUtc
import com.pigeonmessenger.extension.supportsOreo
import com.pigeonmessenger.extension.vibrate
import com.pigeonmessenger.job.PigeonJobManager
import com.pigeonmessenger.job.SendMessageJob
import com.pigeonmessenger.repo.ConversationRepo
import com.pigeonmessenger.utils.Constant
import com.pigeonmessenger.utils.Constant.ARGS_USER
import com.pigeonmessenger.vo.*
import io.reactivex.disposables.Disposable
import org.webrtc.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class CallService : Service(), PeerConnectionClient.PeerConnectionEvents {


    private val callExecutor = Executors.newSingleThreadExecutor()
    private val timeoutExecutor = Executors.newScheduledThreadPool(1)
    private var timeoutFuture: ScheduledFuture<*>? = null
    var message: MessageEntity? = null
    private val audioManager: CallAudioManager by lazy {
        CallAudioManager(this)
    }
    private var camera: Camera? = null

    private var audioEnable = true

    private var disposable: Disposable? = null

    private val peerConnectionClient: PeerConnectionClient by lazy {
        PeerConnectionClient(this, this)
    }


    @Inject
    lateinit var jobManager: PigeonJobManager
    @Inject
    lateinit var messageDao: MessageDao

    @Inject
    lateinit var accountService: AccountService

    @Inject
    lateinit var callState: com.pigeonmessenger.vo.CallState
    @Inject
    lateinit var conversationRepo: ConversationRepo

    private val gson = Gson()

    private var quoteMessageId: String? = null
    private lateinit var self: Account
    private var user: User? = null

    private lateinit var conversationId: String

    private var declineTriggeredByUser: Boolean = true
    var eglBase: EglBase? = null

    override fun onCreate() {
        super.onCreate()
        App.get().appComponent.inject(this)
        isRunning = true
        val option = PeerConnectionFactory.Options().apply {
            this.disableEncryption = true
            this.disableNetworkMonitor = true
        }

        if (callState.callType == com.pigeonmessenger.vo.CallState.CallType.VIDEO) {
            eglBase = EglBase.create()

            camera = Camera(this, object : Camera.CameraEventListener {
                override fun onCameraSwitchCompleted(newCameraState: CameraState) {

                }
            })

            callState.localSurfaceRenderer = SurfaceViewRenderer(applicationContext)
            callState.remoteSurfaceRenderer = SurfaceViewRenderer(applicationContext)
            callState.remoteSurfaceRenderer!!.setMirror(true)
            callState.localSurfaceRenderer!!.setMirror(true)
            callState.localSurfaceRenderer!!.init(eglBase?.eglBaseContext, null)
            callState.remoteSurfaceRenderer!!.init(eglBase?.eglBaseContext, null)
            audioManager.isSpeakerOn = true
        }
        peerConnectionClient.createPeerConnectionFactory(option, callState.localSurfaceRenderer, camera, eglBase)
        Session.getAccount()?.let { user ->
            self = user
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == null) return START_NOT_STICKY

        callExecutor.execute {
            when (intent.action) {
                ACTION_CALL_INCOMING -> handleCallIncoming(intent)
                ACTION_CALL_OUTGOING -> handleCallOutgoing(intent)
                ACTION_CALL_ANSWER -> handleAnswerCall(intent)
                ACTION_CANDIDATE -> handleCandidate(intent)
                ACTION_CALL_CANCEL -> handleCallCancel(intent)
                ACTION_CALL_DECLINE -> handleCallDecline()
                ACTION_CALL_LOCAL_END -> handleCallLocalEnd(intent)
                ACTION_CALL_REMOTE_END -> handleCallRemoteEnd()
                ACTION_CALL_BUSY -> handleCallBusy()
                ACTION_CALL_LOCAL_FAILED -> handleCallLocalFailed()
                ACTION_CALL_REMOTE_FAILED -> handleCallRemoteFailed()
                ACTION_CALL_DISCONNECT -> disconnect()

                ACTION_MUTE_AUDIO -> handleMuteAudio(intent)
                ACTION_SPEAKERPHONE -> handleSpeakerphone(intent)
                ACTION_CHECK_TIMEOUT -> handleCheckTimeout()
                ACTION_CHANGE_CAMERA -> handleChangeCamera()
            }
        }
        supportsOreo {
            updateNotification()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        audioManager.release()
        callState.reset()
        camera?.dispose()
        isRunning = false
    }

    private fun disconnect() {
        stopForeground(true)
        audioManager.stop()
        peerConnectionClient.close()
        eglBase?.releaseSurface()
        eglBase?.release()
        disposable?.dispose()
        timeoutFuture?.cancel(true)
    }

    private fun handleCallIncoming(intent: Intent) {
        if (!callState.isIdle() || isBusy()) {
            val category = MessageCategory.WEBRTC_BUSY.name
            val message = intent.getSerializableExtra(EXTRA_BLAZE) as MessageEntity
            val m = createCallMessage(UUID.randomUUID().toString(), message.conversationId, message.senderId, category, null,
                    nowInUtc(), MessageStatus.SENDING, message.id)

            jobManager.addJobInBackground(SendMessageJob(m))

            val savedMessage = createCallMessage(m.id, m.conversationId, m.senderId, m.type, m.message,
                    m.createdAt, MessageStatus.DELIVERED, m.id)
            if (checkConversation(m)) {
                messageDao.insert(savedMessage)
            }
            return
        }
        if (callState.callInfo.callState == CallState.STATE_RINGING) return

        callState.setCallState(CallState.STATE_RINGING)
        audioManager.start(false)
        message = intent.getSerializableExtra(EXTRA_BLAZE) as MessageEntity
        user = intent.getParcelableExtra(ARGS_USER)
        conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)

        val pendingCandidateData = intent.getStringExtra(EXTRA_PENDING_CANDIDATES)
        if (pendingCandidateData != null && pendingCandidateData.isNotEmpty()) {
            val list = gson.fromJson(pendingCandidateData, Array<IceCandidate>::class.java)
            list.forEach {
                peerConnectionClient.addRemoteIceCandidate(it)
            }
        }
        callState.user = user
        updateNotification()
        quoteMessageId = message!!.id
        callState.setMessageId(quoteMessageId!!)
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(this), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        peerConnectionClient.isInitiator = false
        callState.isInitiator = false
        CallActivity.show(this, user)
    }

    private fun handleCallOutgoing(intent: Intent) {
        if (callState.callInfo.callState == CallState.STATE_DIALING) return

        callState.setCallState(CallState.STATE_DIALING)
        audioManager.start(true)
        user = intent.getParcelableExtra(ARGS_USER)
        conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        callState.user = user
        updateNotification()
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(this), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        peerConnectionClient.isInitiator = true
        callState.isInitiator = true
        CallActivity.show(this, user)

        peerConnectionClient.createOffer(emptyList())
        // getTurnServer { peerConnectionClient.createOffer(it) }
    }

    private fun handleAnswerCall(intent: Intent) {
        if (callState.callInfo.callState == CallState.STATE_ANSWERING) return

        callState.setCallState(CallState.STATE_ANSWERING)
        updateNotification()
        audioManager.stop()
        if (peerConnectionClient.isInitiator) {
            val serializable = intent.getSerializableExtra(EXTRA_BLAZE) ?: return
            message = serializable as MessageEntity
            peerConnectionClient.setAnswerSdp(getRemoteSdp(Base64.decode(message!!.message)))
        } else {
            peerConnectionClient.createAnswer(emptyList(), getRemoteSdp(Base64.decode(message!!.message)))
            //   }
        }
    }

    private fun handleCandidate(intent: Intent) {
        val message = intent.getSerializableExtra(EXTRA_BLAZE) as MessageEntity
        val json = String(Base64.decode(message!!.message))
        val ices = gson.fromJson(json, Array<IceCandidate>::class.java)
        ices.forEach {
            peerConnectionClient.addRemoteIceCandidate(it)
        }
        updateNotification()
    }

    private fun handleIceConnected() {
        if (callState.callInfo.callState == CallState.STATE_CONNECTED) return

        callState.connectedTime = System.currentTimeMillis()
        callState.setCallState(CallState.STATE_CONNECTED)
        updateNotification()
        timeoutFuture?.cancel(true)
        vibrate(longArrayOf(0, 30))
        peerConnectionClient.setAudioEnable(audioEnable)
        peerConnectionClient.enableCommunication()

    }

    private fun handleCallCancel(intent: Intent? = null) {
        if (callState.callInfo.callState == CallState.STATE_IDLE) return

        if (peerConnectionClient.isInitiator) {
            val category = MessageCategory.WEBRTC_CANCEL.name
            sendCallMessage(category)
            val toIdle = intent?.getBooleanExtra(EXTRA_TO_IDLE, false)
            if (toIdle != null && toIdle) {
                callState.setCallState(CallState.STATE_IDLE)
            }
        } else {
            callState.setCallState(CallState.STATE_IDLE)
        }
        updateNotification()
        disconnect()
    }

    private fun handleCallDecline() {
        if (callState.callInfo.callState == CallState.STATE_IDLE) return

        if (peerConnectionClient.isInitiator) {
            callState.setCallState(CallState.STATE_IDLE)
        } else {
            val category = MessageCategory.WEBRTC_DECLINE.name
            sendCallMessage(category)
        }
        updateNotification()
        disconnect()
    }

    private fun handleCallLocalEnd(intent: Intent? = null) {
        if (callState.callInfo.callState == CallState.STATE_IDLE) return

        val category = MessageCategory.WEBRTC_END.name
        sendCallMessage(category)
        val toIdle = intent?.getBooleanExtra(EXTRA_TO_IDLE, false)
        if (toIdle != null && toIdle) {
            callState.setCallState(CallState.STATE_IDLE)
        }
        updateNotification()
        disconnect()
    }

    private fun handleCallRemoteEnd() {
        if (callState.callInfo.callState == CallState.STATE_IDLE) return

        callState.setCallState(CallState.STATE_IDLE)
        updateNotification()
        disconnect()
    }

    private fun handleCallBusy() {
        callState.setCallState(CallState.STATE_BUSY)
        updateNotification()
        disconnect()
    }

    private fun handleCallLocalFailed() {
        val state = callState.callInfo.callState
        if (state == CallState.STATE_DIALING && peerConnectionClient.hasLocalSdp()) {
            val mId = UUID.randomUUID().toString()
            val type = if (callState.callType == com.pigeonmessenger.vo.CallState.CallType.VIDEO) MessageCategory.WEBRTC_VIDEO_OFFER.name
            else MessageCategory.WEBRTC_FAILED.name

            val m = createCallMessage(mId, conversationId, self.userId!!, type,
                    null, nowInUtc(), MessageStatus.READ, mId)
            messageDao.insert(m)
            callState.setCallState(CallState.STATE_IDLE)
            disconnect()
        } else if (state != CallState.STATE_CONNECTED) {
            sendCallMessage(MessageCategory.WEBRTC_FAILED.name)
            callState.setCallState(CallState.STATE_IDLE)
            disconnect()
        }
        updateNotification()
    }

    private fun handleCallRemoteFailed() {
        callState.setCallState(CallState.STATE_IDLE)
        updateNotification()
        disconnect()
    }

    private fun handleMuteAudio(intent: Intent) {
        val extras = intent.extras ?: return

        audioEnable = !extras.getBoolean(EXTRA_MUTE)
        peerConnectionClient.setAudioEnable(audioEnable)
        updateNotification()
    }

    private fun handleSpeakerphone(intent: Intent) {
        val extras = intent.extras ?: return

        val speakerphone = extras.getBoolean(EXTRA_SPEAKERPHONE)
        audioManager.isSpeakerOn = speakerphone
        updateNotification()
    }

    private fun handleChangeCamera() {
        camera?.flip()
    }

    private fun handleCheckTimeout() {
        if (callState.callInfo.callState == CallState.STATE_IDLE && callState.callInfo.callState == CallState.STATE_CONNECTED) return

        updateNotification()
        handleCallCancel()
    }

    private fun updateNotification() {
        startForeground(CallNotificationBuilder.WEBRTC_NOTIFICATION, CallNotificationBuilder.getCallNotification(this, callState, user))
    }

    private fun getRemoteSdp(json: ByteArray): SessionDescription {
        val sdp = gson.fromJson(String(json), Sdp::class.java)
        return SessionDescription(getType(sdp.type), sdp.sdp)
    }

    private fun getRemoteSdp(json: String): SessionDescription {
        val sdp = gson.fromJson(json, Sdp::class.java)
        return SessionDescription(getType(sdp.type), sdp.sdp)
    }

    private fun getType(type: String): SessionDescription.Type {
        return when (type) {
            SessionDescription.Type.OFFER.canonicalForm() -> SessionDescription.Type.OFFER
            SessionDescription.Type.ANSWER.canonicalForm() -> SessionDescription.Type.ANSWER
            SessionDescription.Type.PRANSWER.canonicalForm() -> SessionDescription.Type.PRANSWER
            else -> SessionDescription.Type.OFFER
        }
    }

    private fun isBusy(): Boolean {
        val tm = getSystemService<TelephonyManager>()
        return callState.callInfo.callState != CallState.STATE_IDLE || tm?.callState != TelephonyManager.CALL_STATE_IDLE
    }

    // PeerConnectionEvents
    override fun onLocalDescription(sdp: SessionDescription) {
        callExecutor.execute {
            val category = if (peerConnectionClient.isInitiator) {
                if (callState.callType == com.pigeonmessenger.vo.CallState.CallType.VIDEO)
                    MessageCategory.WEBRTC_VIDEO_OFFER.name
                else MessageCategory.WEBRTC_AUDIO_OFFER.name
            } else {
                MessageCategory.WEBRTC_AUDIO_ANSWER.name
            }
            val json = gson.toJson(Sdp(sdp.description, sdp.type.canonicalForm()))
            sendCallMessage(category, json)
        }
    }

    override fun onIceCandidate(candidate: IceCandidate) {
        callExecutor.execute {
            val arr = arrayListOf(candidate)
            val json = gson.toJson(arr)
            sendCallMessage(MessageCategory.WEBRTC_ICE_CANDIDATE.name, json)
        }
    }

    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
    }

    override fun onIceConnected() {
        callExecutor.execute { handleIceConnected() }
    }

    override fun onIceDisconnected() {
    }

    override fun onIceConnectedFailed() {
        callExecutor.execute { handleCallLocalFailed() }
    }

    override fun onPeerConnectionClosed() {
        CallService.stopService(this)
    }

    override fun onPeerConnectionStatsReady(reports: Array<StatsReport>) {
    }

    override fun onPeerConnectionError(description: String) {
        callExecutor.execute { handleCallLocalFailed() }
    }

    override fun onAddStream(stream: MediaStream) {
        stream.audioTracks.forEach { it.setEnabled(true) }
        if (com.pigeonmessenger.vo.CallState.CallType.VIDEO == callState.callType && stream.videoTracks.size > 0) {
            val s = stream.videoTracks[0]
            s.addSink(callState.remoteSurfaceRenderer)
        }
    }


    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun sendCallMessage(category: String, content: String? = null) {
        val encoded: String? = if (content != null) Base64.encodeBytes(content.toByteArray()) else null

        val message = if (peerConnectionClient.isInitiator) {
            val messageId = UUID.randomUUID().toString()
            if (category == MessageCategory.WEBRTC_AUDIO_OFFER.name ||
                    category == MessageCategory.WEBRTC_VIDEO_OFFER.name) {
                quoteMessageId = messageId
                callState.setMessageId(messageId)

                createCallMessage(messageId, conversationId, self.userId!!, category, encoded,
                        nowInUtc(), MessageStatus.SENDING)
            } else {
                if (category == MessageCategory.WEBRTC_END.name) {
                    val duration = System.currentTimeMillis() - callState.connectedTime!!
                    createCallMessage(messageId, conversationId, self.userId!!, category, encoded,
                            nowInUtc(), MessageStatus.SENDING, quoteMessageId, duration.toString())
                } else {
                    createCallMessage(messageId, conversationId, self.userId!!, category, encoded,
                            nowInUtc(), MessageStatus.SENDING, quoteMessageId)
                }
            }
        } else {
            if (message == null) {
                Timber.e("Answer's blazeMessageData can not be null!")
                handleCallLocalFailed()
                return
            }
            if (category == MessageCategory.WEBRTC_END.name) {
                val duration = System.currentTimeMillis() - callState.connectedTime!!
                createCallMessage(UUID.randomUUID().toString(), conversationId, self.userId!!,
                        category, encoded, nowInUtc(), MessageStatus.SENDING, quoteMessageId,
                        duration.toString())
            } else {
                createCallMessage(UUID.randomUUID().toString(),
                        conversationId,
                        self.userId!!,
                        category, encoded,
                        nowInUtc(),
                        MessageStatus.SENDING,
                        quoteMessageId)
            }
        }

        Log.d(TAG, "message ${message} conversationId ${conversationId} ");
        if (quoteMessageId != null || message.type == MessageCategory.WEBRTC_AUDIO_OFFER.name ||
                message.type == MessageCategory.WEBRTC_VIDEO_OFFER.name) {
            jobManager.addJobInBackground(SendMessageJob(message))
        }

        saveMessage(message)
    }


    private fun saveMessage(m: MessageEntity) {
        if (!checkConversation(m)) return

        val uId = if (callState.isInitiator) {
            self.userId
        } else {
            callState.user!!.userId
        }
        when {
            m.type == MessageCategory.WEBRTC_DECLINE.name -> {
                m.type = if (callState.callType == com.pigeonmessenger.vo.CallState.CallType.VIDEO) MessageCategory.WEBRTC_VIDEO_DECLINE.name
                else MessageCategory.WEBRTC_AUDIO_DECLINE.name
                val status = if (declineTriggeredByUser) MessageStatus.READ else MessageStatus.DELIVERED
                messageDao.insert(createNewReadMessage(m, uId!!, status))
            }
            m.type == MessageCategory.WEBRTC_CANCEL.name -> {
                m.type = if (callState.callType == com.pigeonmessenger.vo.CallState.CallType.VIDEO) MessageCategory.WEBRTC_VIDEO_CANCEL.name
                else MessageCategory.WEBRTC_AUDIO_CANCEL.name
                val msg = createCallMessage(m.id, m.conversationId, uId!!, m.type, m.message,
                        m.createdAt, MessageStatus.READ, m.quoteMessageId, m.mediaDuration)
                messageDao.insert(msg)
            }
            m.type == MessageCategory.WEBRTC_END.name -> {
                m.type = if (callState.callType == com.pigeonmessenger.vo.CallState.CallType.VIDEO) MessageCategory.WEBRTC_VIDEO_END.name
                else MessageCategory.WEBRTC_AUDIO_END.name
                val msg = createNewReadMessage(m, uId!!, MessageStatus.READ)
                messageDao.insert(msg)
            }
            m.type == MessageCategory.WEBRTC_FAILED.name -> {
                m.type = if (callState.callType == com.pigeonmessenger.vo.CallState.CallType.VIDEO) MessageCategory.WEBRTC_VIDEO_FAILED.name
                else MessageCategory.WEBRTC_AUDIO_FAILED.name
                val msg = createNewReadMessage(m, uId!!, MessageStatus.READ)
                messageDao.insert(msg)
            }
        }
    }

    private fun createNewReadMessage(m: MessageEntity, userId: String, status: MessageStatus) =
            createCallMessage(quoteMessageId ?: message?.quoteMessageId ?: message?.id
            ?: UUID.randomUUID().toString(),
                    m.conversationId, userId, m.type, m.message, m.createdAt, status, m.quoteMessageId, m.mediaDuration)


    private fun checkConversation(message: MessageEntity): Boolean {
        val conversation = conversationRepo.findConversation(message.conversationId)
        if (conversation != null) return true
        else return false //TODO CREATE CONVERSATION IF NOT EXISTS
    }

    /*
        private fun getTurnServer(action: (List<PeerConnection.IceServer>) -> Unit) {
            disposable = accountService.getTurn().subscribeOn(Schedulers.io()).subscribe({
                if (it.isSuccess) {
                    val array = it.data as Array<TurnServer>
                    action.invoke(genIceServerList(array))
                } else {
                    handleFetchTurnError()
                }
            }, {
                ErrorHandler.handleError(it)
                handleFetchTurnError()
            })
        }
    */
    private fun handleFetchTurnError() {
        callExecutor.execute { handleCallLocalFailed() }
    }

    private fun genIceServerList(array: Array<TurnServer>): List<PeerConnection.IceServer> {
        val iceServer = arrayListOf<PeerConnection.IceServer>()
        array.forEach {
            iceServer.add(PeerConnection.IceServer.builder(it.url)
                    .setUsername(it.username)
                    .setPassword(it.credential)
                    .createIceServer())
        }
        return iceServer
    }

    private class TimeoutRunnable(private val context: Context) : Runnable {
        override fun run() {
            CallService.timeout(context)
        }
    }

    enum class CallState {
        STATE_IDLE, STATE_DIALING, STATE_RINGING, STATE_ANSWERING, STATE_CONNECTED, STATE_BUSY
    }

    companion object {
        const val TAG = "CallService"

        const val DEFAULT_TIMEOUT_MINUTES = 1L

        private const val ACTION_CALL_INCOMING = "call_incoming"
        private const val ACTION_CALL_OUTGOING = "call_outgoing"
        const val ACTION_CALL_ANSWER = "call_answer"
        const val ACTION_CANDIDATE = "candidate"
        const val ACTION_CALL_CANCEL = "call_cancel"
        const val ACTION_CALL_DECLINE = "call_decline"
        const val ACTION_CALL_LOCAL_END = "call_local_end"
        const val ACTION_CALL_REMOTE_END = "call_remote_end"
        const val ACTION_CALL_BUSY = "call_busy"
        const val ACTION_CALL_LOCAL_FAILED = "call_local_failed"
        const val ACTION_CALL_REMOTE_FAILED = "call_remote_failed"
        const val ACTION_CALL_DISCONNECT = "call_disconnect"
        const val ACTION_CHANGE_CAMERA = "call_camera_change"

        private const val ACTION_CHECK_TIMEOUT = "check_timeout"
        private const val ACTION_MUTE_AUDIO = "mute_audio"
        private const val ACTION_SPEAKERPHONE = "speakerphone"

        const val EXTRA_TO_IDLE = "from_notification"
        private const val EXTRA_CONVERSATION_ID = "conversationId"
        private const val EXTRA_BLAZE = "blaze"
        private const val EXTRA_MUTE = "mute"
        private const val EXTRA_SPEAKERPHONE = "speakerphone"
        private const val EXTRA_PENDING_CANDIDATES = "pending_candidates"
        private const val EXTRA_VIDEO_CALL = "video_call"
        var isRunning = false

        fun incoming(ctx: Context, user: User, conversationId: String, data: MessageEntity,
                     pendingCandidateData: String? = null) = startService(ctx, ACTION_CALL_INCOMING) {
            it.putExtra(ARGS_USER, user)
            it.putExtra(CallService.EXTRA_BLAZE, data)
            it.putExtra(EXTRA_CONVERSATION_ID, conversationId)
            if (pendingCandidateData != null) {
                it.putExtra(EXTRA_PENDING_CANDIDATES, pendingCandidateData)
            }
        }

        fun outgoing(ctx: Context, user: User, conversationId: String) = startService(ctx, ACTION_CALL_OUTGOING) {
            it.putExtra(Constant.ARGS_USER, user)
            it.putExtra(CallService.EXTRA_CONVERSATION_ID, conversationId)
        }

        fun answer(ctx: Context, data: MessageEntity? = null) = startService(ctx, CallService.ACTION_CALL_ANSWER) { intent ->
            data?.let {
                intent.putExtra(CallService.EXTRA_BLAZE, data)
            }
            intent.putExtra("df", "")
        }

        fun candidate(ctx: Context, data: MessageEntity) = startService(ctx, CallService.ACTION_CANDIDATE) {
            it.putExtra(CallService.EXTRA_BLAZE, data)
        }

        fun cancel(ctx: Context) = startService(ctx, CallService.ACTION_CALL_CANCEL)

        fun decline(ctx: Context) = startService(ctx, CallService.ACTION_CALL_DECLINE)

        fun localEnd(ctx: Context) = startService(ctx, CallService.ACTION_CALL_LOCAL_END)

        fun remoteEnd(ctx: Context) = startService(ctx, CallService.ACTION_CALL_REMOTE_END)

        fun busy(ctx: Context) = startService(ctx, CallService.ACTION_CALL_BUSY)

        fun remoteFailed(ctx: Context) = startService(ctx, CallService.ACTION_CALL_REMOTE_FAILED)

        fun disconnect(ctx: Context) {
            if (isRunning) {
                startService(ctx, ACTION_CALL_DISCONNECT)
            }
        }

        fun muteAudio(ctx: Context, checked: Boolean) = startService(ctx, CallService.ACTION_MUTE_AUDIO) {
            it.putExtra(CallService.EXTRA_MUTE, checked)
        }

        fun speakerPhone(ctx: Context, checked: Boolean) = startService(ctx, CallService.ACTION_SPEAKERPHONE) {
            it.putExtra(CallService.EXTRA_SPEAKERPHONE, checked)
        }

        fun changeCamera(ctx: Context) = startService(ctx, CallService.ACTION_CHANGE_CAMERA)

        fun timeout(ctx: Context) = startService(ctx, ACTION_CHECK_TIMEOUT)

        private fun startService(ctx: Context, action: String? = null, putExtra: ((intent: Intent) -> Unit)? = null) {
            val intent = Intent(ctx, CallService::class.java).apply {
                this.action = action
                putExtra?.invoke(this)
            }

            Log.d(TAG, "startService...")
            //ctx.startService(intent)
            ContextCompat.startForegroundService(ctx, intent)
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, CallService::class.java))
        }
    }
}