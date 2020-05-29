package com.suhotrub.easywebrtc

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface SignallingService {
    fun setOnICEReceived(listener: (IceCandidate) -> Unit)
    fun setOnSDPReceived(listener: (SessionDescription) -> Unit)
    fun sendSDP(sdp: SessionDescription)
    fun sendICE(ice: IceCandidate)
}

