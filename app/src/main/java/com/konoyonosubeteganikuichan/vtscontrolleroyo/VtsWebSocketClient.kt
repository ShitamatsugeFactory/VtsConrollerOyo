package com.konoyonosubeteganikuichan.vtscontrolleroyo

import android.app.Activity
import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.lang.Exception
import java.net.URI


class VtsWebSocketClient (val activity: Activity, uri: URI, var stateChangedListener: ((String)->Unit)) : WebSocketClient(uri) {
    val TAG = "VtsWebSocketClient"
    var token = ""
    var status = "INIT" // todo: enum化する

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.i(TAG, "connected to vts ws server")
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.i(TAG, "disconnected to vts ws server :${reason}")
    }

    override fun onMessage(message: String?) {
        Log.d(TAG, "received message: $message")
        val d = JSONObject(message)
        when (d.getString("messageType")) {
            "APIStateResponse" -> {
                status = "AUTH_START"
            }
            "AuthenticationTokenResponse" -> {
                token = d.getJSONObject("data").getString("authenticationToken")
                status = "TOKEN_RECEIVED"
            }
            "AuthenticationResponse" -> {
                status = "AUTH_FINISHED"
            }
            "Live2DParameterListResponse" -> {
            }
            // todo: other response types...  https://github.com/DenchiSoft/VTubeStudio
            // 複数のView で役割り分担するならtoken や parameters list などはここではなく呼び出し元に持つべきかも...
        }
        stateChangedListener(status)
    }

    override fun onError(ex: Exception?) {
        Log.i(TAG, "onError。", ex)
    }
}