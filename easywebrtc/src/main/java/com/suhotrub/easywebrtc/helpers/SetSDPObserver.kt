package com.suhotrub.easywebrtc.helpers

import android.util.Log
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class SetSDPObserver(private val onSuccess: () -> Unit) : SdpObserver {
    override fun onCreateFailure(p0: String?) = Unit
    override fun onCreateSuccess(p0: SessionDescription?) = Unit
    override fun onSetFailure(p0: String?) {
        Log.d(Consts.TAG, "Set SDP failure: $p0")
    }

    override fun onSetSuccess() {
        Log.d(Consts.TAG, "Set SDP success")
        onSuccess()
    }
}