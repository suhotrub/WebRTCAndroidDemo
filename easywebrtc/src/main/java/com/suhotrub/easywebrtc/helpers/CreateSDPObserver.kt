package com.suhotrub.easywebrtc.helpers

import android.util.Log
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class CreateSDPObserver(private val onSuccess: (SessionDescription) -> Unit) : SdpObserver {
    override fun onSetFailure(p0: String?) = Unit
    override fun onSetSuccess() = Unit
    override fun onCreateSuccess(p0: SessionDescription?) {
        Log.d(Consts.TAG, "Create SDP success")
        p0?.let(onSuccess)
    }

    override fun onCreateFailure(p0: String?) {
        Log.d(Consts.TAG, "Create SDP failure: $p0")
    }
}