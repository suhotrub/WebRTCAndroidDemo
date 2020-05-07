package com.suhotrub.webrtcandroiddemo

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class WebRTCSockets(
    private val onMessage: (String) -> Unit
) : WebSocketClient(URI.create("ws://82.146.49.33/socket")) {

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        reconnect()
    }

    override fun onError(ex: Exception?) {

    }

    override fun onMessage(message: String?) {
        message?.let(onMessage)
    }

    override fun onOpen(handshakedata: ServerHandshake?) {

    }

}