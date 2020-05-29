package com.suhotrub.webrtcandroiddemo.network.models

import org.webrtc.IceCandidate

data class NetworkICE(val sessionId: String, val candidate: IceCandidate)