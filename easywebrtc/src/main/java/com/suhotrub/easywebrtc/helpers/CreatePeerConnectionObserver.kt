package com.suhotrub.easywebrtc.helpers

import android.util.Log
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.VideoTrack

class CreatePeerConnectionObserver(
    private val onIceCandidate: (IceCandidate) -> Unit,
    private val onVideoTrack: (VideoTrack) -> Unit,
    private val onHangup: () -> Unit
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
    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        if (p0 == PeerConnection.SignalingState.CLOSED) onHangup()
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) = Unit
    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        Log.d(Consts.TAG,p0.toString())
        when (p0) {
            PeerConnection.IceConnectionState.DISCONNECTED, PeerConnection.IceConnectionState.FAILED -> onHangup()
        }
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) = Unit
    override fun onDataChannel(p0: DataChannel?) = Unit

    override fun onIceCandidate(p0: IceCandidate?) {
        Log.d(Consts.TAG,p0.toString())
        p0?.let(onIceCandidate)
    }

    override fun onAddStream(p0: MediaStream?) {
        p0?.let { mediaStream -> mediaStream.audioTracks.forEach { it.setVolume(1.0) } }
    }

}