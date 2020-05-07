package com.suhotrub.webrtcandroiddemo

import android.content.Context
import android.util.Log
import android.widget.Toast
import org.webrtc.AudioSource
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import java.util.concurrent.Executors

abstract class WebRTCHooks(private val context: () -> Context) {

    private companion object {
        const val STUN = "stun:stun.l.google.com:19302"
    }

    val ectx by lazy {
        EglBase.create().eglBaseContext
    }

    abstract fun onRemoteVideoTrack(remoteVideoTrack: VideoTrack)
    abstract fun onLocalVideoTrack(localVideoTrack: VideoTrack)

    fun getContext() = context()

    fun getAudioMediaConstrains(): MediaConstraints {
        return MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("echoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("noiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("autoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("volume", "1"))
        }
    }

    fun getVideoDecoderFactory(): DefaultVideoDecoderFactory {
        return DefaultVideoDecoderFactory(ectx)
    }

    fun getVideoEncoderFactory(): DefaultVideoEncoderFactory {
        return DefaultVideoEncoderFactory(ectx, true, true)
    }

    fun getPeerConnectionFactoryOptions(): PeerConnectionFactory.Options {
        PeerConnectionFactory.Options().apply {
            disableEncryption = true
            disableNetworkMonitor = true
        }
        return PeerConnectionFactory.Options()
    }

    fun getPeerConnectionMediaConstraints(): MediaConstraints {
        return MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("RtpDataChannels", "false"))
        }
    }

    fun getIceServers() = listOf(PeerConnection.IceServer.builder(STUN).createIceServer())
}

interface SignallingService {
    fun setOnSDPReceived(listener: (SessionDescription) -> Unit)
    fun setOnICEReceived(listener: (IceCandidate) -> Unit)
    fun sendSDP(sdp: SessionDescription)
    fun sendICE(ice: IceCandidate)
    fun setActiveSessionId(sessionId: String)
}

class WebRTCContainer(
    private val webRTCHooks: WebRTCHooks,
    private val signallingService: SignallingService
) {

    private var cameraCapturer: VideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var mediaStream: MediaStream? = null
    private var executor = Executors.newSingleThreadExecutor()


    init {
        signallingService.setOnICEReceived {
            peerConnection?.addIceCandidate(it)
        }
        signallingService.setOnSDPReceived {
            setRemoteDescription(it)
        }

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(webRTCHooks.getContext().applicationContext)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
    }

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    fun call() {
        createPeerConnection()
        executor.execute {
            initMedia()
            createOffer()
        }
    }

    private fun createPeerConnection() {
        if (executor.isShutdown) {
            executor = Executors.newSingleThreadExecutor()
        }
        executor.submit {
            peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoDecoderFactory(webRTCHooks.getVideoDecoderFactory())
                .setVideoEncoderFactory(webRTCHooks.getVideoEncoderFactory())
                .setOptions(webRTCHooks.getPeerConnectionFactoryOptions())
                .createPeerConnectionFactory()

            val observer = CreatePeerConnectionObserver(
                onIceCandidate = signallingService::sendICE,
                onVideoTrack = webRTCHooks::onRemoteVideoTrack
            )
            peerConnection = peerConnectionFactory?.createPeerConnection(webRTCHooks.getIceServers(), observer)
        }.get()
    }

    fun switchCamera(surfaceView: SurfaceViewRenderer) {
        (cameraCapturer as? CameraVideoCapturer)?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFrontFacing: Boolean) {
                surfaceView.setMirror(isFrontFacing)
            }

            override fun onCameraSwitchError(p0: String?) {
                Toast.makeText(surfaceView.context, p0 ?: "Ошибка", Toast.LENGTH_SHORT).show()
            }

        })
    }

    fun hangup() {
        if (executor.isTerminated) executor = Executors.newSingleThreadExecutor()
        executor.execute {
            cameraCapturer?.dispose()
            cameraCapturer = null

            videoSource?.dispose()
            videoSource = null

            audioSource?.dispose()
            audioSource = null

            peerConnection?.dispose()
            peerConnection = null

            peerConnectionFactory?.dispose()
            peerConnectionFactory = null

            executor.shutdownNow()
        }
    }

    private fun initMedia() {
        val peerConnectionFactory = peerConnectionFactory!!
        audioSource = peerConnectionFactory.createAudioSource(webRTCHooks.getAudioMediaConstrains())
        videoSource = peerConnectionFactory.createVideoSource(false)

        cameraCapturer = createCameraCapturer(webRTCHooks.getContext()).apply {
            initialize(
                SurfaceTextureHelper.create("govno", webRTCHooks.ectx),
                webRTCHooks.getContext(),
                videoSource?.capturerObserver
            )
            startCapture(1280, 720, 60)
        }


        val localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource)
        webRTCHooks.onLocalVideoTrack(localVideoTrack)

        val localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource)
        localAudioTrack?.setEnabled(true)

        /*mediaStream = peerConnectionFactory.createLocalMediaStream("su4ka")?.apply {
            addTrack(localVideoTrack)
            addTrack(localAudioTrack)
        }*/

        peerConnection?.addTrack(localVideoTrack)
        peerConnection?.addTrack(localAudioTrack)
    }

    private fun createCameraCapturer(context: Context): VideoCapturer {
        val enumerator = getEnumerator(context)
        return findCameraPredicate(enumerator) { isFrontFacing(it) }
            ?: findCameraPredicate(enumerator) { !isFrontFacing(it) }
            ?: throw IllegalStateException()
    }

    private fun getEnumerator(context: Context): CameraEnumerator {
        return (if (Camera2Enumerator.isSupported(context)) Camera2Enumerator(context) else Camera1Enumerator())
    }

    private fun findCameraPredicate(
        enumerator: CameraEnumerator,
        predicate: CameraEnumerator.(String) -> Boolean
    ): VideoCapturer? {
        return enumerator.run { deviceNames.find { predicate(this, it) }?.let { createCapturer(it, null) } }
    }

    private fun createOffer() {
        peerConnection?.createOffer(
            CreateSDPObserver { executor.execute { setLocalDescription(it) } },
            webRTCHooks.getPeerConnectionMediaConstraints()
        )
    }

    private fun createAnswer() {
        peerConnection?.createAnswer(
            CreateSDPObserver { executor.execute { setLocalDescription(it) } },
            webRTCHooks.getPeerConnectionMediaConstraints()
        )
    }

    private fun setLocalDescription(sessionDescription: SessionDescription) {
        peerConnection?.setLocalDescription(
            SetSDPObserver { signallingService.sendSDP(sessionDescription) },
            sessionDescription
        )
    }

    private fun setRemoteDescription(sessionDescription: SessionDescription) {
        val receivingCall = sessionDescription.type == SessionDescription.Type.OFFER
        if (!receivingCall) return
        createPeerConnection()

        executor.execute {
            peerConnection?.setRemoteDescription(SetSDPObserver {
                if (receivingCall) {
                    initMedia()
                    createAnswer()
                }
            }, sessionDescription)
        }
    }
}


class CreatePeerConnectionObserver(
    private val onIceCandidate: (IceCandidate) -> Unit,
    private val onVideoTrack: (VideoTrack) -> Unit
) : PeerConnection.Observer {

    override fun onRemoveStream(p0: MediaStream?) = Unit
    override fun onRenegotiationNeeded() = Unit
    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        p0?.let {
            if (it.track()?.kind() == MediaStreamTrack.VIDEO_TRACK_KIND) {
                onVideoTrack(it.track() as VideoTrack)
            }
        }
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) = Unit
    override fun onSignalingChange(p0: PeerConnection.SignalingState?) = Unit
    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) = Unit
    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) = Unit
    override fun onIceConnectionReceivingChange(p0: Boolean) = Unit
    override fun onDataChannel(p0: DataChannel?) = Unit

    override fun onIceCandidate(p0: IceCandidate?) {
        p0?.let(onIceCandidate)
    }

    override fun onAddStream(p0: MediaStream?) {
        p0?.let { mediaStream -> mediaStream.audioTracks.forEach { it.setVolume(1.0) } }
    }

}

class SetSDPObserver(private val onSuccess: () -> Unit) : SdpObserver {
    override fun onCreateFailure(p0: String?) = Unit
    override fun onCreateSuccess(p0: SessionDescription?) = Unit
    override fun onSetFailure(p0: String?) {
        Log.d("SET SDP", p0)
    }

    override fun onSetSuccess() {
        Log.d("SET SDP", "Success")
        onSuccess()
    }
}


class CreateSDPObserver(private val onSuccess: (SessionDescription) -> Unit) : SdpObserver {
    override fun onSetFailure(p0: String?) = Unit
    override fun onSetSuccess() = Unit
    override fun onCreateSuccess(p0: SessionDescription?) {
        Log.d("CREATE SDP", "Success")
        p0?.let(onSuccess)
    }

    override fun onCreateFailure(p0: String?) {
        Log.d("CREATE SDP", p0)
    }
}