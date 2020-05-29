package com.suhotrub.easywebrtc.config

import android.content.Context
import com.suhotrub.easywebrtc.helpers.Consts
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory.Options
import org.webrtc.VideoTrack
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule

abstract class DefaultEasyWebRTCConfig : EasyWebRTCConfig {

    private val ectx by lazy {
        EglBase.create().eglBaseContext
    }

    override fun getEglBaseContext(): EglBase.Context = ectx

    abstract override fun onRemoteVideoTrack(remoteVideoTrack: VideoTrack)
    abstract override fun onLocalVideoTrack(localVideoTrack: VideoTrack)
    abstract override fun onHangup()

    override fun getAudioDeviceModule(context: Context): AudioDeviceModule {
        return JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()
    }

    override fun getVideoDecoderFactory(): DefaultVideoDecoderFactory {
        return DefaultVideoDecoderFactory(ectx)
    }

    override fun getVideoEncoderFactory(): DefaultVideoEncoderFactory {
        return DefaultVideoEncoderFactory(
            ectx,
            true,
            true
        )
    }

    override fun getPeerConnectionFactoryOptions(): Options {
        return Options()
    }

    override fun getPeerConnectionMediaConstraints(): MediaConstraints {
        return MediaConstraints()
    }

    override fun getRTCConfig(): RTCConfiguration {
        return RTCConfiguration(
            listOf(
                PeerConnection.IceServer
                    .builder(Consts.GOOGLE_STUN)
                    .createIceServer()
            )
        )
    }
}