package com.suhotrub.webrtcandroiddemo.network

import com.suhotrub.easywebrtc.SignallingService
import com.suhotrub.webrtcandroiddemo.network.models.Members
import com.suhotrub.webrtcandroiddemo.network.models.NetworkICE
import com.suhotrub.webrtcandroiddemo.network.models.NetworkSPD
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class SocketsInteractor : SignallingService {

    private val webRTCSockets = WebRTCSockets(this::parseMessage).apply { connect() }

    private var remoteSessionId: String = ""
    private var onIceEvent: ((IceCandidate) -> Unit)? = null
    private var onSdpEvent: ((SessionDescription) -> Unit)? = null

    private var onMembers: ((List<String>) -> Unit)? = null
    private var members: List<String> = emptyList()

    fun setOnMembersListener(listener: (List<String>) -> Unit) {
        onMembers = listener
        onMembers!!(members)
    }

    private fun sendMessage(message: String) {
        if (webRTCSockets.isClosed) webRTCSockets.reconnect()
        webRTCSockets.send(message)
    }

    fun setActiveSessionId(sessionId: String) {
        remoteSessionId = sessionId
    }

    private fun parseMessage(message: String) {
        val json = JSONObject(message)
        when{
            json.has("members") ->{
                MessageConverter.fromJs<Members>(message)?.let {
                    members = it.members
                    onMembers?.invoke(members)
                }
            }
            json.has("sdp") -> {
                MessageConverter.fromJs<NetworkSPD>(message)?.let {
                    remoteSessionId = it.sessionId
                    onSdpEvent?.invoke(it.sdp)
                }
            }
            json.has("candidate") -> {
                MessageConverter.fromJs<NetworkICE>(message)?.let {
                    onIceEvent?.invoke(it.candidate)
                }
            }
        }
    }

    override fun sendICE(ice: IceCandidate) {
        sendMessage(MessageConverter.toJs(
            NetworkICE(
                remoteSessionId,
                ice
            )
        ))
    }

    override fun sendSDP(sdp: SessionDescription) {
        sendMessage(MessageConverter.toJs(
            NetworkSPD(
                remoteSessionId,
                sdp
            )
        ))
    }

    override fun setOnICEReceived(listener: (IceCandidate) -> Unit) {
        onIceEvent = listener
    }

    override fun setOnSDPReceived(listener: (SessionDescription) -> Unit) {
        onSdpEvent = listener
    }

    companion object {
        val instance by lazy {
            SocketsInteractor()
        }
    }
}

