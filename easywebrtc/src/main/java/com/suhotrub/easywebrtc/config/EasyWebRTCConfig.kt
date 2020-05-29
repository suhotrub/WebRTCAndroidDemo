package com.suhotrub.easywebrtc.config

import android.content.Context
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.VideoTrack
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule

interface EasyWebRTCConfig {

    fun onRemoteVideoTrack(remoteVideoTrack: VideoTrack)
    fun onLocalVideoTrack(localVideoTrack: VideoTrack)
    fun onHangup()

    fun getEglBaseContext(): EglBase.Context

    fun getPeerConnectionMediaConstraints(): MediaConstraints

    fun getAudioDeviceModule(context: Context): AudioDeviceModule
    fun getVideoDecoderFactory(): DefaultVideoDecoderFactory
    fun getVideoEncoderFactory(): DefaultVideoEncoderFactory

    fun getPeerConnectionFactoryOptions(): PeerConnectionFactory.Options

    fun getRTCConfig(): PeerConnection.RTCConfiguration
}

