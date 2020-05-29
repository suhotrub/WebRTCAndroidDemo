package com.suhotrub.webrtcandroiddemo.network.models

import org.webrtc.SessionDescription

data class NetworkSPD(val sessionId: String, val sdp: SessionDescription)