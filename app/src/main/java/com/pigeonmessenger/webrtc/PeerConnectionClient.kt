package com.pigeonmessenger.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.*
import timber.log.Timber
import java.util.concurrent.Executors


class PeerConnectionClient(private val context: Context, private val events: PeerConnectionEvents) {
    private val executor = Executors.newSingleThreadExecutor()
    private var factory: PeerConnectionFactory? = null
    private var isError = false
    private var isVideoCall = false
    private var videoSink: VideoSink? = null
    private var camera: com.pigeonmessenger.webrtc.Camera? = null
    private var eglBase: EglBase? = null

    init {
        executor.execute {
            PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
            )
        }
    }

    private val pcObserver = PCObserver()
    private val iceServers = arrayListOf<PeerConnection.IceServer>()
    var isInitiator = false
    private var remoteCandidateCache = arrayListOf<IceCandidate>()
    private var remoteSdpCache: SessionDescription? = null
    private var peerConnection: PeerConnection? = null
    private var audioTrack: AudioTrack? = null
    private var audioSource: AudioSource? = null
    private val sdpConstraint = MediaConstraints()
    private val googleStunServer = PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()

    internal var videoCapturerAndroid: CameraVideoCapturer? = null


    fun createPeerConnectionFactory(options: PeerConnectionFactory.Options,
                                    videoSink: VideoSink?,
                                    camera: Camera? = null,
                                    eglBase: EglBase? = null) {
        if (factory != null) {
            reportError("PeerConnectionFactory has already been constructed")
            return
        }
        executor.execute { createPeerConnectionFactoryInternal(options, videoSink, camera, eglBase) }
    }

    fun createOffer(iceServerList: List<PeerConnection.IceServer>) {
        iceServers.addAll(iceServerList)
        iceServers.add(googleStunServer)
        executor.execute {
            if (peerConnection==null) peerConnection = createPeerConnectionInternal()
        //    val peerConnection = createPeerConnectionInternal()
            sdpConstraint.mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
            sdpConstraint.mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
            isInitiator = true
            val offerSdpObserver = object : SdpObserverWrapper() {
                override fun onCreateSuccess(sdp: SessionDescription) {
                    peerConnection?.setLocalDescription(object : SdpObserverWrapper() {
                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "onSetFailure ${error}")
                            reportError("createOffer setLocalSdp onSetFailure error: $error")
                        }

                        override fun onSetSuccess() {
                            Log.d(TAG, "createOffer setLocalSdp onSetSuccess")
                            events.onLocalDescription(sdp)
                        }
                    }, sdp)
                }

                override fun onCreateFailure(error: String?) {
                    Log.d(TAG, "onCreateFailure ${error}")
                    reportError("createOffer onCreateFailure error: $error")
                }
            }
            peerConnection?.createOffer(offerSdpObserver, sdpConstraint)
        }
    }

    fun createAnswer(iceServerList: List<PeerConnection.IceServer>, remoteSdp: SessionDescription) {
        iceServers.addAll(iceServerList)
        iceServers.add(googleStunServer)
        executor.execute {
            if (peerConnection==null) peerConnection = createPeerConnectionInternal()
            peerConnection?.setRemoteDescription(remoteSdpObserver, remoteSdp)
            isInitiator = false
            val answerSdpObserver = object : SdpObserverWrapper() {
                override fun onCreateSuccess(sdp: SessionDescription) {
                    peerConnection?.setLocalDescription(object : SdpObserverWrapper() {
                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, ":onSetFailer${error} ");
                            reportError("createAnswer setLocalSdp onSetFailure error: $error")
                        }

                        override fun onSetSuccess() {
                            Log.d(TAG, "createAnswer setLocalSdp onSetSuccess")
                            events.onLocalDescription(sdp)
                        }
                    }, sdp)
                }

                override fun onCreateFailure(error: String?) {
                    reportError("createAnswer onCreateFailure error: $error")
                }
            }
            peerConnection?.createAnswer(answerSdpObserver, sdpConstraint)
        }
    }

    fun addRemoteIceCandidate(candidate: IceCandidate) {
        Log.d(TAG, "addRemoteIceCandidate: ");
        executor.execute {
            if (peerConnection != null && peerConnection!!.remoteDescription != null) {
                peerConnection!!.addIceCandidate(candidate)
            } else {
                remoteCandidateCache.add(candidate)
            }
        }
    }

    fun removeRemoteIcetCandidate(candidates: Array<IceCandidate>) {
        executor.execute {
            if (peerConnection == null || isError) return@execute

            drainCandidatesAndSdp()
            peerConnection!!.removeIceCandidates(candidates)
        }
    }

    fun setAnswerSdp(sdp: SessionDescription) {
        Log.d(TAG, "setAnswerSdp: ");
        executor.execute {
            if (peerConnection != null) {
                peerConnection!!.setRemoteDescription(remoteSdpObserver, sdp)
            }
        }
    }

    fun setAudioEnable(enable: Boolean) {
        executor.execute {
            if (peerConnection == null || audioTrack == null || isError) return@execute

            audioTrack!!.setEnabled(enable)
        }
    }

    fun enableCommunication() {
        executor.execute {
            if (peerConnection == null || isError) return@execute

            peerConnection!!.setAudioPlayout(true)
            peerConnection!!.setAudioRecording(true)
        }
    }

    fun hasLocalSdp(): Boolean {
        if (peerConnection == null) return false

        return peerConnection!!.localDescription != null
    }

    fun close() {
        executor.execute {
            Log.d(TAG, "1")
            peerConnection?.dispose()
            Log.d(TAG, "1")

            peerConnection = null
            audioSource?.dispose()
         //   Log.d(TAG, "5")
            audioSource = null
         //   videoSource?.dispose()
            Log.d(TAG, "7")
          //  videoSource = null

            Log.d(TAG, "8")
          //  localVideoTrack = null
            factory?.dispose()
            Log.d(TAG, "9")
           factory = null
            events.onPeerConnectionClosed()
            Log.d(TAG, "10")
        }
    }

    private fun reportError(error: String) {
        executor.execute {
            if (!isError) {
                events.onPeerConnectionError(error)
                isError = true
            }
        }
    }

    private fun createPeerConnectionInternal(): PeerConnection? {

        if (factory == null || isError) {
            reportError("PeerConnectionFactory is not created")
            return null
        }
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            /// sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            // continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        peerConnection = factory!!.createPeerConnection(rtcConfig, pcObserver)
        if (peerConnection == null) {
            reportError("PeerConnection is not created")
            return null
        }
        val constraints = MediaConstraints()

        constraints.optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        val mediaStream = factory!!.createLocalMediaStream("ARDAMS")
        peerConnection!!.setAudioPlayout(false)
        peerConnection!!.setAudioRecording(false)
        // peerConnection!!.addTrack(createAudioTrack())
        mediaStream.addTrack(createAudioTrack())
        if (isVideoCall && camera != null && camera!!.capturer != null) {
            mediaStream.addTrack(createVideoTrack())
        }
        peerConnection!!.addStream(mediaStream)
        return peerConnection
    }

    private fun createPeerConnectionFactoryInternal(options: PeerConnectionFactory.Options,
                                                    videoSink: VideoSink?,
                                                    camera: Camera?,
                                                    eglBase: EglBase? = null) {

        this.videoSink = videoSink
        this.eglBase = eglBase
        this.camera = camera

        if (videoSink != null) this.isVideoCall = true

        val builder = PeerConnectionFactory.builder()
        builder.setOptions(options)

        if (isVideoCall) {
            val encoderFactory = DefaultVideoEncoderFactory(this.eglBase!!.eglBaseContext, true, true)
            val decoderFactory = DefaultVideoDecoderFactory(this.eglBase!!.eglBaseContext)
            builder.setVideoEncoderFactory(encoderFactory)
            builder.setVideoDecoderFactory(decoderFactory)
        }
        factory = builder.createPeerConnectionFactory()
        if (isVideoCall) peerConnection = createPeerConnectionInternal()
    }

    private fun drainCandidatesAndSdp() {
        if (peerConnection == null) return

        remoteCandidateCache.forEach {
            peerConnection!!.addIceCandidate(it)
            remoteCandidateCache.clear()
        }
        if (remoteSdpCache != null && peerConnection!!.remoteDescription == null) {
            peerConnection!!.setRemoteDescription(remoteSdpObserver, remoteSdpCache)
            remoteSdpCache = null
        }
    }

    private fun createAudioTrack(): AudioTrack {
        val audioConstraints = MediaConstraints()
        audioConstraints.optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        audioSource = factory!!.createAudioSource(audioConstraints)
        audioTrack = factory!!.createAudioTrack(AUDIO_TRACK_ID, audioSource)
        audioTrack!!.setEnabled(false) //TODO it was true
        return audioTrack!!
    }

    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null

    private fun createVideoTrack(): VideoTrack {
        videoSource = factory!!.createVideoSource(false)
        localVideoTrack = factory!!.createVideoTrack("ARDAMSv0", videoSource)
        camera!!.capturer!!.initialize(SurfaceTextureHelper.create("WebRTC-SurfaceTextureHelper",
                eglBase!!.eglBaseContext), context, videoSource!!.capturerObserver)
        localVideoTrack!!.addSink(videoSink)
        localVideoTrack!!.setEnabled(true)
        camera!!.setEnabled(true)
        return localVideoTrack!!
    }


    private val remoteSdpObserver = object : SdpObserverWrapper() {
        override fun onSetFailure(error: String?) {
            reportError("setRemoteSdp onSetFailure error: $error")
        }

        override fun onSetSuccess() {
            remoteCandidateCache.forEach {
                peerConnection?.addIceCandidate(it)
            }
            remoteCandidateCache.clear()
            Timber.d("setRemoteSdp onSetSuccess")
        }
    }

    private inner class PCObserver : PeerConnection.Observer {

        override fun onIceCandidate(candidate: IceCandidate) {
            executor.execute { events.onIceCandidate(candidate) }
        }

        override fun onDataChannel(dataChannel: DataChannel?) {
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {
        }

        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
            Log.d(TAG, "onIceConnectionChange: $newState")
            executor.execute {
                when (newState) {
                    PeerConnection.IceConnectionState.CONNECTED -> events.onIceConnected()
                    PeerConnection.IceConnectionState.DISCONNECTED -> events.onIceDisconnected()
                    PeerConnection.IceConnectionState.FAILED -> events.onIceConnectedFailed()
                    else -> {
                    }
                }
            }
        }

        override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
            Log.d(TAG, "onIceGatheringChange: $newState")
        }

        override fun onAddStream(stream: MediaStream) {
            Log.d(TAG, "onAddStream")
            events.onAddStream(stream)
        }

        override fun onSignalingChange(newState: PeerConnection.SignalingState) {
            Log.d(TAG, "SignalingState: $newState")
        }

        override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
            Log.d(TAG, "onIceCandidatesRemoved")
            executor.execute { events.onIceCandidatesRemoved(candidates) }
        }

        override fun onRemoveStream(stream: MediaStream) {
            stream.videoTracks[0].dispose()
        }

        override fun onRenegotiationNeeded() {
        }

        override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
        }
    }

    private open class SdpObserverWrapper : SdpObserver {
        override fun onSetFailure(error: String?) {
        }

        override fun onSetSuccess() {
        }

        override fun onCreateSuccess(sdp: SessionDescription) {
        }

        override fun onCreateFailure(error: String?) {
        }
    }

    /**
     * Peer connection events.
     */
    interface PeerConnectionEvents {
        /**
         * Callback fired once local SDP is created and set.
         */
        fun onLocalDescription(sdp: SessionDescription)

        /**
         * Callback fired once local Ice candidate is generated.
         */
        fun onIceCandidate(candidate: IceCandidate)

        /**
         * Callback fired once local ICE candidates are removed.
         */
        fun onIceCandidatesRemoved(candidates: Array<IceCandidate>)

        /**
         * Callback fired once connection is established (IceConnectionState is
         * CONNECTED).
         */
        fun onIceConnected()

        /**
         * Callback fired once connection is closed (IceConnectionState is
         * DISCONNECTED).
         */
        fun onIceDisconnected()

        /**
         * Callback fired once connection is closed (IceConnectionState is
         * FAILED).
         */
        fun onIceConnectedFailed()

        /**
         * Callback fired once peer connection is closed.
         */
        fun onPeerConnectionClosed()

        /**
         * Callback fired once peer connection statistics is ready.
         */
        fun onPeerConnectionStatsReady(reports: Array<StatsReport>)

        /**
         * Callback fired once peer connection error happened.
         */
        fun onPeerConnectionError(description: String)

        /*
            Callback fired once got mediaStream
         */
        fun onAddStream(stream: MediaStream)

    }

    companion object {
        const val TAG = "PeerConnectionClient"

        private const val AUDIO_TRACK_ID = "ARDAMSa0"
    }
}