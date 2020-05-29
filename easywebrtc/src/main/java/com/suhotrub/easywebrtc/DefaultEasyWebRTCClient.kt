package com.suhotrub.easywebrtc

import android.content.Context
import android.widget.Toast
import com.suhotrub.easywebrtc.config.EasyWebRTCConfig
import com.suhotrub.easywebrtc.helpers.CreatePeerConnectionObserver
import com.suhotrub.easywebrtc.helpers.CreateSDPObserver
import com.suhotrub.easywebrtc.helpers.SetSDPObserver
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule
import org.webrtc.voiceengine.WebRtcAudioEffects
import org.webrtc.voiceengine.WebRtcAudioUtils
import java.util.concurrent.Executors

open class DefaultEasyWebRTCClient(
    private val context: Context,
    private val signallingService: SignallingService,
    private val easyWebRTCConfig: EasyWebRTCConfig
) : EasyWebRTCClient {

    private var cameraCapturer: VideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var executor = Executors.newSingleThreadExecutor()

    init {
        WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true)

        signallingService.setOnICEReceived {
            peerConnection?.addIceCandidate(it)
        }

        signallingService.setOnSDPReceived {
            if (it.type != SessionDescription.Type.OFFER) {
                peerConnection?.setRemoteDescription(SetSDPObserver {}, it)
            }
        }

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
    }

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    override fun initCall() {
        executor.execute {
            createPeerConnection()
            initMedia()
            createOffer()
        }
    }

    override fun answerCall(sdp: SessionDescription) {
        val receivingCall = sdp.type == SessionDescription.Type.OFFER
        if (!receivingCall) return

        executor.execute {
            createPeerConnection()
            initMedia()
            peerConnection?.setRemoteDescription(SetSDPObserver {
                createAnswer()
            }, sdp)
        }
    }

    override fun switchCamera(surfaceView: SurfaceViewRenderer) {
        (cameraCapturer as? CameraVideoCapturer)?.switchCamera(object :
            CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFrontFacing: Boolean) {
                surfaceView.setMirror(isFrontFacing)
            }

            override fun onCameraSwitchError(p0: String?) {
                Toast.makeText(surfaceView.context, p0 ?: "Ошибка", Toast.LENGTH_SHORT).show()
            }

        })
    }

    override fun hangup() {

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

            easyWebRTCConfig.onHangup()

            executor.shutdownNow()
        }
    }

    override fun setVideoEnabled(isEnabled: Boolean) {
        localVideoTrack?.setEnabled(isEnabled)
    }

    private fun createPeerConnection() {
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(easyWebRTCConfig.getVideoDecoderFactory())
            .setVideoEncoderFactory(easyWebRTCConfig.getVideoEncoderFactory())
            .setAudioDeviceModule(easyWebRTCConfig.getAudioDeviceModule(context))
            .setOptions(easyWebRTCConfig.getPeerConnectionFactoryOptions())
            .createPeerConnectionFactory()

        val observer = CreatePeerConnectionObserver(
            onIceCandidate = signallingService::sendICE,
            onVideoTrack = easyWebRTCConfig::onRemoteVideoTrack,
            onHangup = this::hangup
        )
        peerConnection = peerConnectionFactory?.createPeerConnection(easyWebRTCConfig.getRTCConfig(), observer)
    }

    private fun initMedia() {
        val peerConnectionFactory = peerConnectionFactory!!
        audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        videoSource = peerConnectionFactory.createVideoSource(false)

        cameraCapturer = createCameraCapturer(context).apply {
            initialize(
                SurfaceTextureHelper.create("govno", easyWebRTCConfig.getEglBaseContext()),
                context,
                videoSource?.capturerObserver
            )
            startCapture(1280, 720, 60)
        }


        localVideoTrack = peerConnectionFactory.createVideoTrack("ARDAMSv0", videoSource)
        easyWebRTCConfig.onLocalVideoTrack(localVideoTrack!!)

        localAudioTrack = peerConnectionFactory.createAudioTrack("ARDAMSa0", audioSource)
        localAudioTrack?.setEnabled(true)

        val mediaStreamIds = listOf("ARDAMS")
        peerConnection?.addTrack(localVideoTrack, mediaStreamIds)
        peerConnection?.addTrack(localAudioTrack, mediaStreamIds)
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
            easyWebRTCConfig.getPeerConnectionMediaConstraints()
        )
    }

    private fun createAnswer() {
        peerConnection?.createAnswer(
            CreateSDPObserver { executor.execute { setLocalDescription(it) } },
            easyWebRTCConfig.getPeerConnectionMediaConstraints()
        )
    }

    private fun setLocalDescription(sessionDescription: SessionDescription) {
        peerConnection?.setLocalDescription(
            SetSDPObserver { signallingService.sendSDP(sessionDescription) },
            sessionDescription
        )
    }
}