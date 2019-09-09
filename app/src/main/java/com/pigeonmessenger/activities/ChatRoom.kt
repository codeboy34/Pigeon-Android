package com.pigeonmessenger.activities

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.nfc.tech.MifareUltralight.PAGE_SIZE
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.View.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.core.view.children
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pigeonmessenger.R
import com.pigeonmessenger.Session
import com.pigeonmessenger.adapter.ConversationAdapter
import com.pigeonmessenger.adapter.MixinHeadersDecoration
import com.pigeonmessenger.adapter.holder.BaseViewHolder
import com.pigeonmessenger.customviews.ContentEditText
import com.pigeonmessenger.database.room.entities.ConversationStatus
import com.pigeonmessenger.database.room.entities.User
import com.pigeonmessenger.database.room.entities.generateConversationId
import com.pigeonmessenger.events.SeenStatusEvent
import com.pigeonmessenger.extension.*
import com.pigeonmessenger.job.LinkState
import com.pigeonmessenger.manager.TypingManager
import com.pigeonmessenger.utils.Attachment
import com.pigeonmessenger.utils.AudioPlayer
import com.pigeonmessenger.viewmodals.MessageViewModal
import com.pigeonmessenger.viewmodals.isGroup
import com.pigeonmessenger.vo.*
import com.pigeonmessenger.webrtc.CallService
import com.pigeonmessenger.widget.*
import com.pigeonmessenger.widget.AndroidUtilities.dp
import com.pigeonmessenger.widget.audio.OpusAudioRecorder
import com.pigeonmessenger.widget.audio.OpusAudioRecorder.Companion.STATE_NOT_INIT
import com.pigeonmessenger.widget.audio.OpusAudioRecorder.Companion.STATE_RECORDING
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.kotlin.autoDisposable
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import kotlinx.android.synthetic.main.activity_chat_room.*
import kotlinx.android.synthetic.main.layout_media.*
import kotlinx.android.synthetic.main.view_chat_controlview.view.*
import kotlinx.android.synthetic.main.view_reply.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.android.synthetic.main.view_tool.view.*
import kotlinx.coroutines.*
import one.mixin.android.widget.gallery.ui.GalleryActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.File
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class ChatRoom : AppCompatActivity(), OpusAudioRecorder.Callback {

    @Inject
    lateinit var callState: CallState

    @Inject
    lateinit var linkState: LinkState

    @Inject
    lateinit var typingManager: TypingManager

    companion object {
        private const val TAG = "ChatRoom"
        private const val ARGS_CONVERSATION_ID = "conversationId"
        private const val ARGS_RECIPIENT_ID = "recipient_id"
        private const val MESSAGE_ID = "message_id"
        private const val KEY_WORD = "key_word"
        private const val MESSAGES = "messages"

        fun show(context: Context,
                 conversationId: String?,
                 recipientId: String? = null,
                 messageId: String? = null,
                 keyword: String? = null) {
            val intent = Intent(context, ChatRoom::class.java)
            intent.putExtra(ARGS_CONVERSATION_ID, conversationId)
            intent.putExtra(ARGS_RECIPIENT_ID, recipientId)
            intent.putExtra(KEY_WORD, keyword)
            intent.putExtra(MESSAGE_ID, messageId)
            context.startActivity(intent)
        }

        fun putIntent(
                context: Context,
                conversationId: String? = null,
                senderId: String?): Intent {
            if (conversationId == null) {
                throw IllegalArgumentException("lose data")
            }
            return Intent(context, ChatRoom::class.java).apply {
                this.putExtra(ARGS_CONVERSATION_ID, conversationId)
                this.putExtra(ARGS_RECIPIENT_ID, senderId)
            }
        }

        fun show(context: Context, intent: Intent, recipientId: String) {
            intent.putExtra(ARGS_RECIPIENT_ID, recipientId)
            context.startActivity(intent)
        }
    }

    private val scopeProvider: AndroidLifecycleScopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    private val messageId: String? by lazy {
        intent.getStringExtra(MESSAGE_ID)
    }

    private val keyword: String? by lazy {
        intent.getStringExtra(KEY_WORD)
    }

    private val recipientId: String? by lazy {
        intent.getStringExtra(ARGS_RECIPIENT_ID)
    }

    private val conversationId: String by lazy {
        var cid = intent.getStringExtra(ARGS_CONVERSATION_ID)
        if (cid.isNullOrBlank()) {
            isFirstMessage = true
            cid = generateConversationId(senderId, recipientId!!)
        }
        cid
    }

    private var recipient: User? = null

    private var isFirstMessage = false
    private var isFirst = true
    private var unreadCount: Int = 0
    private lateinit var coroutineContext: CoroutineContext
    override fun onCancel() {
        chat_control?.cancelExternal()
    }

    override fun sendAudio(file: File, duration: Long, waveForm: ByteArray) {
        if (duration < 500) {
            file.deleteOnExit()
        } else
            createConversation {
                messageViewModal.sendAudioMessage(conversationId, senderId, file, duration, waveForm)
            }
    }

    private val adapter: ConversationAdapter by lazy {
        ConversationAdapter(keyword, itemClickListener, isGroup(conversationId)).apply {
            registerAdapterDataObserver(chatAdapterDataObserver)
        }
    }

    private val messageViewModal: MessageViewModal by lazy {
        ViewModelProviders.of(this@ChatRoom).get(MessageViewModal::class.java)
    }

    private val decoration by lazy {
        MixinHeadersDecoration(adapter)
    }

    private val senderId by lazy {
        Session.getUserId()
    }
    private var isFirstLoad = true

    private val chatAdapterDataObserver = object : RecyclerView.AdapterDataObserver() {
        var oldSize = 0

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            adapter.currentList?.let {
                oldSize = it.size
            }
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            when {
                isFirst -> {
                    isFirst = false
                    chat_rv.visibility = VISIBLE
                    val position = if (messageId != null) {
                        unreadCount + 1
                    } else {
                        unreadCount
                    }
                    (chat_rv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, chat_rv.measuredHeight * 3 / 4)
                }
                isBottom -> {
                    if (adapter.currentList != null && adapter.currentList!!.size > oldSize) {
                        chat_rv.layoutManager?.scrollToPosition(0)
                    }
                }
                else -> {
                    if (unreadTipCount > 0) {
                        down_unread.visibility = VISIBLE
                        down_unread.text = "$unreadTipCount"
                    } else {
                        down_unread.visibility = View.GONE
                    }
                }
            }

            adapter.currentList?.let {
                oldSize = it.size
            }
        }
    }

    private var keyboardLayoutListener = OnGlobalLayoutListener {
        val heightDiff = root_layout.rootView.height - root_layout.height
        val contentViewTop = window.findViewById<View>(Window.ID_ANDROID_CONTENT).top
        if (heightDiff <= contentViewTop) onHideKeyboard()
        else {
            val keyboardHeight = heightDiff - contentViewTop;
            onShowKeyboard(keyboardHeight);
        }
    }

    private fun onHideKeyboard() {
        Log.d(TAG, "HIDEKEYBOARD.....")

    }

    private fun onShowKeyboard(height: Int) {
        Log.d(TAG, "ONSHOWKEYBOARD.....")
        Log.d(TAG, "First Position $firstPosition")
        //if(adapter.currentList!!.size>0 )
        //chat_rv.layoutManager?.scrollToPosition(0)
    }

    private var keyboardListenersAttached: Boolean = false;

    private fun attachKeyboardListeners() {
        if (keyboardListenersAttached) return
        root_layout.viewTreeObserver.addOnGlobalLayoutListener(keyboardLayoutListener);
        keyboardListenersAttached = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        setContentView(R.layout.activity_chat_room)
        App.get().appComponent.inject(this)
        coroutineContext = Job()
        renderUser()
        initView()
        attachKeyboardListeners()
    }

    private fun renderUser() {
        if (isGroup(conversationId)) {
            titleview.sub_title_tv.visibility = VISIBLE
            titleview.sub_title_tv.text = getString(R.string.tap_to_more_info)
            titleview.setOnUserClickListener(View.OnClickListener {
                GroupInfo.show(this@ChatRoom, conversationId)
            })
            messageViewModal.findConversation(conversationId).observe(this, Observer {
                titleview.renderGroup(it)
                if(it.status== ConversationStatus.QUIT.ordinal){
                    bottom_cant_send.visibility = VISIBLE
                }
            })

            titleview.videocall_ib.visibility = GONE
            titleview.audiocall_ib.visibility = GONE

        } else {
            messageViewModal.findContactEntity(recipientId!!).observe(this@ChatRoom, Observer {
                recipient = it
                titleview.renderUser(it)
                titleview.setOnUserClickListener(View.OnClickListener { _ ->
                    UserProfileActivity.show(this@ChatRoom, it.userId, conversationId)
                })
            })
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        recipientId?.let {
            messageViewModal.registerOnlineStatus(it)
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
        recipientId?.let {
            messageViewModal.leaveOnlineStatus(it)
        }
    }

    override fun onResume() {
        super.onResume()
        EmojiManager.install(GoogleEmojiProvider())
        App.conversationWith = conversationId
        chat_control.setEmojiPopup(root_layout)
    }

    override fun onPause() {
        super.onPause()
        App.conversationWith = null
        AudioPlayer.pause()
        if (OpusAudioRecorder.state != STATE_NOT_INIT) {
            OpusAudioRecorder.get().stop()
        }
        if (chat_control.isRecording) {
            chat_control.cancelExternal()
        }
    }


    override fun onBackPressed() {
        if (!backPressed()) super.onBackPressed()
    }

    fun backPressed(): Boolean {
        return when {
            tool_view.visibility == VISIBLE -> {
                closeTool()
                true
            }
            mediaVisibility -> {
                hideMediaLayout()
                true
            }
            chat_control.isRecording -> {
                OpusAudioRecorder.get().stopRecording(false)
                chat_control.cancelExternal()
                true
            }
            reply_view.visibility == VISIBLE -> {
                reply_view.fadeOut()
                chat_control.showOtherInput()
                true
            }
            else -> false
        }
    }

    private var firstPosition: Int = 0

    private var isBottom: Boolean = true

    private var unreadTipCount: Int = 0

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOnlineStatusEvent(seenStatusEvent: SeenStatusEvent) {
        Log.d(TAG, "onOnlineStatusEvent $seenStatusEvent")
        titleview.sub_title_tv.visibility = VISIBLE
        if (seenStatusEvent.isOnline) {
            titleview.sub_title_tv.text = getString(R.string.online)
        } else {
            seenStatusEvent.lastSeenTimestamp?.run {
                titleview.sub_title_tv.text = this.lastSeenAt(this@ChatRoom)
            }
        }
    }

    private fun initView() {
        titleview.back_iv.setOnClickListener { onBackPressed() }
        chat_rv.visibility = INVISIBLE
        chat_control.activity = this
        chat_control.callback = chatCallback

        chat_control.chat_et.setOnClickListener {
            cover.alpha = 0f
            this.window?.statusBarColor = Color.TRANSPARENT
        }
        chat_control.chat_et.setCommitContentListener(object : ContentEditText.OnCommitContentListener {
            override fun onCommitContent(inputContentInfo: InputContentInfoCompat?, flags: Int, opts: Bundle?): Boolean {
                if (inputContentInfo != null) {
                    val url = inputContentInfo.contentUri.getFilePath(this@ChatRoom)
                            ?: return false
                    sendImageMessage(Uri.parse(url)) // changed from url.toUri()
                }
                return true
            }
        })


        titleview.audioCallClickListener(View.OnClickListener {
            if (!callState.isIdle()) {
                if (conversationId != null && callState.user?.userId == conversationId) {
                    CallActivity.show(this, recipient)
                } else {
                    AlertDialog.Builder(this, R.style.PigeonAlertDialogTheme)
                            .setMessage(getString(R.string.chat_call_warning_call))
                            .setNegativeButton(getString(android.R.string.ok)) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                }
            } else {
                RxPermissions(this)
                        .request(Manifest.permission.RECORD_AUDIO)
                        .subscribe({ granted ->
                            if (granted) {
                                callVoice(false)
                            } else {
                                openPermissionSetting()
                            }
                        }, {
                        })
            }
            hideMediaLayout()
        })

        titleview.videocall_ib.setOnClickListener {
            if (!callState.isIdle()) {
                if (callState.user?.userId == conversationId && callState.callType == CallState.CallType.VIDEO) {
                    CallActivity.show(this, recipient)
                } else {
                    toast(getString(R.string.chat_call_warning_call))
                }
            } else {
                RxPermissions(this)
                        .request(Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.CAMERA)
                        .subscribe({ granted ->
                            if (granted) {
                                callVoice(true)
                            } else {
                                openPermissionSetting()
                            }
                        }, {
                        })
            }
        }

        chat_control.setCircle(record_circle)
        chat_control.recordTipView = record_tip_tv
        chat_control.cover = cover
        chat_control.chat_more_ib.setOnClickListener { toggleMediaLayout() }
        chat_rv.adapter = adapter

        val ll = LinearLayoutManager(this)
        ll.stackFromEnd = true
        ll.reverseLayout = true
        chat_rv.layoutManager = ll
        chat_rv.addItemDecoration(decoration)
        chat_rv.itemAnimator = null
        val typingView = LayoutInflater.from(this).inflate(R.layout.item_typing_indicator, chat_rv, false);

        typingManager.typingListener = object : TypingManager.OnStartTypingListener {
            override fun onStartTyping(userId: String) {
                adapter.typingView = typingView
            }

            override fun onStopTyping() {
                adapter.typingView = null
            }
        }

        typingManager.getTypingUser(conversationId)?.let {
            adapter.typingView = typingView
        }

        chat_rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                firstPosition = (chat_rv.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                if (firstPosition > 0) {
                    if (isBottom) {
                        isBottom = false
                        showAlert()
                    }
                } else {
                    if (!isBottom) {
                        isBottom = true
                        hideAlert()
                    }
                    unreadTipCount = 0
                    down_unread.visibility = View.GONE
                }
            }
        })

        tool_view.forward_iv.setOnClickListener {
            val list = ArrayList<ForwardMessage>()
            list += adapter.selectSet.sortedBy { it.createdAt }.map {
                when {
                    it.type.endsWith("_TEXT") -> ForwardMessage(ForwardCategory.TEXT.name, content = it.message)
                    it.type.endsWith("_IMAGE") -> ForwardMessage(ForwardCategory.IMAGE.name, id = it.id)
                    it.type.endsWith("_DATA") -> ForwardMessage(ForwardCategory.DATA.name, id = it.id)
                    it.type.endsWith("_VIDEO") -> ForwardMessage(ForwardCategory.VIDEO.name, id = it.id)
                    //  it.type.endsWith("_CONTACT") -> ForwardMessage(ForwardCategory.UNREGISTERED_CONTACT.name, sharedUserId = it.sharedUserId)
                    // it.type.endsWith("_STICKER") -> ForwardMessage(ForwardCategory.STICKER.name, id = it.messageId)
                    it.type.endsWith("_AUDIO") -> ForwardMessage(ForwardCategory.AUDIO.name, id = it.id)
                    else -> ForwardMessage(ForwardCategory.TEXT.name)
                }
            }
            ForwardActivity.show(this, list)
            closeTool()
        }

        tool_view.reply_iv.setOnClickListener {
            adapter.selectSet.valueAt(0)?.let {
                reply_view.bind(it)
            }
            if (reply_view.visibility != View.VISIBLE) {
                reply_view.fadeIn()
                chat_control.hideOtherInput()
                ///hideStickerContainer()
                if (chat_control.isRecording) {
                    OpusAudioRecorder.get().stopRecording(false)
                    chat_control.cancelExternal()
                }
                chat_control.chat_et.showKeyboard()
            }
            closeTool()
        }
        chat_control.chat_et.requestFocus()

        tool_view.delete_iv.setOnClickListener {
            adapter.selectSet.filter { it.type.endsWith("_AUDIO") }.forEach {
                if (AudioPlayer.get().isPlay(it.id)) {
                    AudioPlayer.get().pause()
                }
                messageViewModal.deleteMessages(adapter.selectSet)
                closeTool()
            }
        }

        reply_view.reply_close_iv.setOnClickListener {
            reply_view.fadeOut()
            chat_control.showOtherInput()
        }
        tool_view.copy_iv.setOnClickListener {
            try {
                getClipboardManager().primaryClip =
                        ClipData.newPlainText(null, adapter.selectSet.valueAt(0)?.message)
                toast(R.string.copy_success)
            } catch (e: ArrayIndexOutOfBoundsException) {
            }
            closeTool()
        }

        media_layout.round(dp(8f))
        //  menu_rv.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        media_camera.setOnClickListener {
            RxPermissions(this@ChatRoom)
                    .request(Manifest.permission.CAMERA)
                    .subscribe({ granted ->
                        if (granted) {
                            imageUri = createImageUri()
                            imageUri?.let {
                                openCamera(it)
                            }
                        } else {
                            openPermissionSetting()
                        }
                    }, {
                    })
            hideMediaLayout()
        }
        media_gallery.setOnClickListener {
            RxPermissions(this@ChatRoom)
                    .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                    .subscribe({ granted ->
                        if (granted) {
                            openGallery()
                        } else {
                            this@ChatRoom.openPermissionSetting()
                        }
                    }, {

                    })
            hideMediaLayout()
        }

        media_files.setOnClickListener {
            RxPermissions(this@ChatRoom)
                    .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                    .subscribe({ granted ->
                        if (granted) {
                            selectDocument()
                        } else {
                            this@ChatRoom.openPermissionSetting()
                        }
                    }, {
                    })
            hideMediaLayout()
        }

        down_flag.setOnClickListener {
            chat_rv.layoutManager?.scrollToPosition(0)
        }
        bindData()
    }

    private fun bindData() {
        GlobalScope.launch(coroutineContext) {
            unreadCount = messageViewModal.indexUnreadCount(conversationId)
            withContext(Dispatchers.Main) {
                liveDataMessages(unreadCount)
            }
        }
    }

    private fun callVoice(videoCall: Boolean) {
        if (LinkState.isOnline(linkState.state)) {
            createConversation {
                if (videoCall) callState.callType = CallState.CallType.VIDEO
                else callState.callType = CallState.CallType.AUDIO
                CallService.outgoing(this, recipient!!, conversationId)
            }
        } else {
            toast(R.string.error_no_connection)
        }
        hideMediaLayout()
    }


    private fun closeTool() {
        adapter.selectSet.clear()
        adapter.notifyDataSetChanged()
        tool_view.fadeOut()
    }

    private fun showAlert(duration: Long = 100) {
        if (down_flag.translationY != 0f) {
            down_flag.translationY(0f, duration)
        }
    }

    private fun hideAlert() {
        down_flag.translationY(this.dpToPx(130f).toFloat(), 100)
    }

    private inline fun createConversation(crossinline action: () -> Unit) {
        if (isFirstMessage) {
            doAsync {
                messageViewModal.initConversation(conversationId, recipient!!.userId)
                isFirstMessage = false
                uiThread {
                    action()
                }
            }
        } else {
            action()
        }
    }

    private val chatCallback = object : ChatControlView.Callback {

        override fun onDispatchTyping() {
            Log.d(TAG, "onDispatchTyping :............. ");
            messageViewModal.sendTypingMessage(conversationId, recipientId)
        }

        override fun onRecordStart(audio: Boolean) {
            AudioPlayer.get().pause()
            OpusAudioRecorder.get().startRecording(this@ChatRoom)
        }

        override fun isReady(): Boolean {
            return OpusAudioRecorder.state == STATE_RECORDING
        }

        override fun onCalling() {

        }

        override fun onStickerClick() {

        }

        override fun onSendClick(text: String) {
            chat_control.chat_et.setText("")
            chat_control.chat_et.stopTyping()
            if (reply_view.visibility == View.VISIBLE
                    && reply_view.messageItem != null) {
                sendReplyMessage(text)
            } else {
                sendMessage(text)
            }
        }

        private fun sendMessage(message: String) {
            createConversation {
                val msg = createMessage(UUID.randomUUID().toString(), conversationId, senderId, message, nowInUtc(),
                        MessageCategory.SIGNAL_TEXT.name)
                messageViewModal.sendMessage(msg)
                scrollToDown()
            }
        }

        private fun sendReplyMessage(message: String) {
            if (message.isNotBlank() && reply_view.messageItem != null) {
                chat_control.chat_et.setText("")
                messageViewModal.sendReplyMessage(conversationId, senderId, message, reply_view.messageItem!!)
                // messageViewModal.sendReplyMessage(senderId, conversationId, message, reply_view.messageItem!!, true)
                reply_view.fadeOut()
                chat_control.showOtherInput()
                reply_view.messageItem = null
                scrollToDown()
            }
        }

        override fun onRecordEnd() {
            OpusAudioRecorder.get().stopRecording(true)
        }

        override fun onRecordCancel() {
            OpusAudioRecorder.get().stopRecording(false)
        }

        override fun onUp() {

        }

        override fun onDown() {

        }
    }


    private var imageUri: Uri? = null
    private fun createImageUri() = Uri.fromFile(this.getImagePath()?.createImageTemp())

    private var mediaVisibility = false
    private fun toggleMediaLayout() {
        if (!mediaVisibility) {
            showMediaLayout()
        } else {
            hideMediaLayout()
        }
    }

    private fun showMediaLayout() {
        if (!mediaVisibility) {
            media_layout.visibility = View.VISIBLE
            cover.fadeIn(alpha = 0.6f)
            //  cover.alpha = 0.4f
            cover.visibility = VISIBLE
            media_layout.translationY(16f)
            chat_control.chat_et.hideKeyboard()
            mediaVisibility = true
            if (reply_view.visibility == View.VISIBLE) {
                reply_view.fadeOut()
                chat_control.showOtherInput()
            }
        }
    }

    private fun hideMediaLayout() {
        if (mediaVisibility) {
            media_layout.translationY(dp(350f).toFloat()) {
                media_layout.visibility = View.GONE
            }
            mediaVisibility = false
            cover.fadeOut(apha = 0.6f)
            // cover.alpha = 0.0f
            cover.visibility = GONE
            if (reply_view.visibility == View.VISIBLE) {
                reply_view.fadeOut()
                chat_control.showOtherInput()
            }
        }
    }


    private var starTransition: Boolean = false

    private val itemClickListener: ConversationAdapter.OnItemListener by lazy {
        object : ConversationAdapter.OnItemListener() {

            override fun onCancel(id: String) {
                messageViewModal.cancel(id)
            }

            override fun onRetryDownload(messageId: String) {
                messageViewModal.retryDownload(messageId)
            }

            override fun onRetryUpload(messageId: String) {
                messageViewModal.retryUpload(messageId)
            }

            override fun onSelect(isSelect: Boolean, messageItem: MessageItem, position: Int) {
                if (isSelect) {
                    adapter.addSelect(messageItem)
                } else {
                    adapter.removeSelect(messageItem)
                }
                when {
                    adapter.selectSet.isEmpty() -> tool_view.fadeOut()
                    adapter.selectSet.size == 1 -> {
                        try {
                            if (adapter.selectSet.valueAt(0)?.type == MessageCategory.SIGNAL_TEXT.name ||
                                    adapter.selectSet.valueAt(0)?.type == MessageCategory.PLAIN_TEXT.name) {
                                tool_view.copy_iv.visibility = View.VISIBLE
                            } else {
                                tool_view.copy_iv.visibility = View.GONE
                            }
                        } catch (e: ArrayIndexOutOfBoundsException) {
                            tool_view.copy_iv.visibility = View.GONE
                        }
                        // if (adapter.selectSet.valueAt(0)?.supportSticker() == true) {
                        ///    tool_view.add_sticker_iv.visibility = View.VISIBLE
                        //} else {
                        //    tool_view.add_sticker_iv.visibility = View.GONE
                        // }
                        if (adapter.selectSet.valueAt(0)?.canNotReply() == true) {
                            tool_view.reply_iv.visibility = View.GONE
                        } else {
                            tool_view.reply_iv.visibility = View.VISIBLE
                        }
                    }
                    else -> {
                        tool_view.forward_iv.visibility = View.VISIBLE
                        tool_view.reply_iv.visibility = View.GONE
                        tool_view.copy_iv.visibility = View.GONE
                    }
                }
                if (adapter.selectSet.find {
                            it.canNotForward()
                        } != null) {
                    tool_view.forward_iv.visibility = View.GONE
                } else {
                    tool_view.forward_iv.visibility = View.VISIBLE
                }
                adapter.notifyDataSetChanged()
            }

            override fun onLongClick(messageItem: MessageItem, position: Int): Boolean {
                val b = adapter.addSelect(messageItem)
                if (b) {
                    if (messageItem.type == MessageCategory.SIGNAL_TEXT.name ||
                            messageItem.type == MessageCategory.PLAIN_TEXT.name) {
                        tool_view.copy_iv.visibility = View.VISIBLE
                    } else {
                        tool_view.copy_iv.visibility = View.GONE
                    }

                    //if (messageItem.supportSticker()) {
                    //  tool_view.add_sticker_iv.visibility = View.VISIBLE
                    //   } else {
                    //    tool_view.add_sticker_iv.visibility = View.GONE
                    // }

                    if (adapter.selectSet.find { it.canNotForward() } != null) {
                        tool_view.forward_iv.visibility = View.GONE
                    } else {
                        tool_view.forward_iv.visibility = View.VISIBLE
                    }
                    if (adapter.selectSet.find { it.canNotReply() } != null) {
                        tool_view.reply_iv.visibility = View.GONE
                    } else {
                        tool_view.reply_iv.visibility = View.VISIBLE
                    }
                    adapter.notifyDataSetChanged()
                    tool_view.fadeIn()
                }
                return b
            }


            override fun onImageClick(messageItem: MessageItem, view: View) {
                starTransition = true
                DragMediaActivity.show(this@ChatRoom, view, messageItem)
            }


            @TargetApi(Build.VERSION_CODES.O)
            override fun onFileClick(messageItem: MessageItem) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O &&
                        messageItem.mediaMimeType.equals("application/vnd.android.package-archive", true)) {
                    if (this@ChatRoom.packageManager.canRequestPackageInstalls()) {
                        openMedia(messageItem)
                    } else {
                        startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES))
                    }
                } else {
                    openMedia(messageItem)
                }
            }
        }
    }

    private fun openMedia(messageItem: MessageItem) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (messageItem.senderId == Session.getUserId()) {
            try {
                messageItem.mediaUrl?.let {
                    val uri = Uri.parse(it)
                    if (uri.scheme.equals("file", true)) {
                        intent.setDataAndType(this@ChatRoom.getUriForFile(File(it)), messageItem.mediaMimeType)
                        this@ChatRoom.startActivity(intent)
                    } else {
                        intent.setDataAndType(uri, messageItem.mediaMimeType)
                        this@ChatRoom.startActivity(intent)
                    }
                }
            } catch (e: ActivityNotFoundException) {
                this@ChatRoom.toast(R.string.error_unable_to_open_media)
            }
        } else {
            try {
                messageItem.mediaUrl?.let {

                    val file = Uri.parse(it).toFile()
                    if (!file.exists()) {
                        this@ChatRoom.toast(R.string.error_file_exists)
                    } else {
                        val uri = this@ChatRoom.getUriForFile(file)
                        intent.setDataAndType(uri, messageItem.mediaMimeType)
                        this@ChatRoom.startActivity(intent)
                    }
                }
            } catch (e: ActivityNotFoundException) {
                this.toast(R.string.error_unable_to_open_media)
            }
        }
    }

    private fun liveDataMessages(unreadCount: Int) {
        messageViewModal.getMessages(conversationId, unreadCount).observe(this, Observer {
            if (it.isNullOrEmpty()) {
                //isFirstMessage = true
            } else {
                if (!isFirstLoad && !isBottom && it.size > adapter.getRealItemCount()) {
                    unreadTipCount += (it.size - adapter.getRealItemCount())
                }

                if (isFirst && messageId == null && unreadCount > 0) {
                    adapter.unreadIndex = unreadCount
                } else if (it.size != adapter.getRealItemCount()) {
                    adapter.unreadIndex = null
                }

                adapter.submitList(it)
                messageViewModal.markMessageRead(conversationId)
            }
        })
    }

    private fun scrollToDown() {
        chat_rv.layoutManager?.scrollToPosition(0)
        if (firstPosition > PAGE_SIZE * 6) {
            adapter.notifyDataSetChanged()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                if (data.hasExtra(GalleryActivity.IS_VIDEO)) {
                    sendVideoMessage(it)
                } else {
                    sendImageMessage(it)
                }
            }
        } else if (requestCode == REQUEST_FILE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            getAttachment(uri)?.let {
                sendAttachmentMessage(it)
            }
        } else if (requestCode == REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
            imageUri?.let { imageUri ->
                showPreview(imageUri) { sendImageMessage(it) }
            }
        }
    }

    private var previewDialogFragment: PreviewDialogFragment? = null

    private fun showPreview(uri: Uri, action: (Uri) -> Unit) {
        if (previewDialogFragment == null) {
            previewDialogFragment = PreviewDialogFragment.newInstance()
        }
        previewDialogFragment?.show(supportFragmentManager, uri, action)
    }


    private fun sendAttachmentMessage(attachment: Attachment) {
        messageViewModal.sendAttachmentMessage(conversationId, senderId, attachment)

    }


    @SuppressLint("CheckResult")
    private fun sendImageMessage(uri: Uri) {
        createConversation {
            messageViewModal.sendImageMessage(conversationId, senderId, uri)
                    ?.autoDisposable(scopeProvider)?.subscribe({
                        when (it) {
                            0 -> scrollToDown()
                            -1 -> toast("Error")
                            -2 -> toast("Error format")
                        }
                    }, {
                        Log.e(TAG, "Error ", it)
                    })
        }
    }

    private fun sendVideoMessage(uri: Uri) {
        createConversation {
            messageViewModal.sendVideoMessage(conversationId, senderId, uri)
                    .autoDisposable(scopeProvider)
                    .subscribe({
                        scrollToDown()
                    }, {
                        //  Timber.e(it)
                    })
        }
    }

    inner class CustomLayoutManager constructor(context: Context) : androidx.recyclerview.widget.LinearLayoutManager(context) {

        override fun smoothScrollToPosition(recyclerView: androidx.recyclerview.widget.RecyclerView, state: androidx.recyclerview.widget.RecyclerView.State?, position: Int) {
            val smoothScroller = CenterSmoothScroller(recyclerView.context)
            smoothScroller.targetPosition = position
            startSmoothScroll(smoothScroller)
        }

        private inner class CenterSmoothScroller internal constructor(context: Context) : androidx.recyclerview.widget.LinearSmoothScroller(context) {

            override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
                return boxStart + (boxEnd - boxStart) / 2 - (viewStart + (viewEnd - viewStart) / 2)
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return 3f //pass as per your requirement
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        chat_rv?.let { rv ->
            rv.children.forEach {
                val vh = rv.getChildViewHolder(it)
                if (vh != null && vh is BaseViewHolder) {
                    vh.stopListen()
                }
            }
        }
        adapter.unregisterAdapterDataObserver(chatAdapterDataObserver)

        AudioPlayer.release()
    }


}

