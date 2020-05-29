package com.suhotrub.easywebrtc

import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer


interface EasyWebRTCClient {
    fun initCall()
    fun answerCall(sdp: SessionDescription)
    fun hangup()
    fun switchCamera(surfaceView: SurfaceViewRenderer)
    fun setVideoEnabled(isEnabled: Boolean)
}