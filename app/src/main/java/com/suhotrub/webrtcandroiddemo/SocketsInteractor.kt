package com.suhotrub.webrtcandroiddemo

import android.util.Log
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class SocketsInteractor(
    private val onMembers: (List<String>) -> Unit
) : SignallingService {

    private val webRTCSockets = WebRTCSockets(this::parseMessage).apply { connect() }

    private var localSessionId: String = ""
    private var remoteSessionId: String = ""
    private lateinit var onIceEvent: (IceCandidate) -> Unit
    private lateinit var onSdpEvent: (SessionDescription) -> Unit

    private fun sendMessage(message: Any) {
        if (webRTCSockets.isClosed) webRTCSockets.reconnect()
        webRTCSockets.send(prepareMessage(message))
    }

    override fun setActiveSessionId(sessionId: String) {
        remoteSessionId = sessionId
    }

    private fun prepareMessage(message: Any): String {
        val json = JSONObject(mapOf("sessionId" to remoteSessionId, "fromSessionId" to localSessionId))
        when (message) {
            is IceCandidate -> {
                val candidate = JSONObject(
                    mapOf(
                        "candidate" to message.sdp,
                        "sdpMid" to message.sdpMid,
                        "sdpMLineIndex" to message.sdpMLineIndex
                    )
                )
                json.put("candidate", candidate)
                Log.d("SEND ICE", json.toString())
            }
            is SessionDescription -> {
                val sdp = JSONObject(mapOf("sdp" to message.description, "type" to message.type.canonicalForm()))
                json.put("sdp", sdp)
                Log.d("SEND SDP", json.toString())
            }
        }
        return json.toString()
    }

    private fun parseMessage(message: String) {
        val json = JSONObject(message)

        when {
            json.has("sdp") -> {
                remoteSessionId = json.getString("fromSessionId")
                localSessionId = json.getString("sessionId")

                val sdp = json.getJSONObject("sdp").getString("sdp")
                val type = json.getJSONObject("sdp").getString("type")
                Log.d("RECEIVE SDP", json.toString())
                onSdpEvent(SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp))

            }
            json.has("ice") -> {
                val sdp = json.getJSONObject("candidate").getString("sdp")
                val sdpMid = json.getJSONObject("candidate").getString("sdpMid")
                val sdpMLineIndex = json.getJSONObject("candidate").getInt("sdpMLineIndex")
                Log.d("RECEIVE ICE", json.toString())
                onIceEvent(IceCandidate(sdpMid, sdpMLineIndex, sdp))
            }
        }
    }

    override fun sendICE(ice: IceCandidate) {
        sendMessage(ice)
    }

    override fun sendSDP(sdp: SessionDescription) {
        sendMessage(sdp)
    }

    override fun setOnICEReceived(listener: (IceCandidate) -> Unit) {
        onIceEvent = listener
    }

    override fun setOnSDPReceived(listener: (SessionDescription) -> Unit) {
        onSdpEvent = listener
    }
}