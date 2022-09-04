package pl.grzybdev.openmic.client.network

import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.singletons.AppData

class Listener(private var socket: Any?) : WebSocketListener() {

    private var forceDisconnected = false

    override fun onOpen(webSocket: WebSocket, response: Response) {
        socket = webSocket

        AppData.openmic.client.onOpen(webSocket)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        socket = webSocket

        AppData.openmic.client.onMessage(webSocket, text)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        socket = webSocket

        handleDisconnect(code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        socket = webSocket

        val unknownError = AppData.resources?.getString(R.string.dialog_failed_to_connect) ?: ""
        handleDisconnect(reason = t.message ?: unknownError)
    }

    fun handleDisconnect(code: Int = 1000, reason: String = "", client_initiated: Boolean = false)
    {
        if (forceDisconnected)
            return

        forceDisconnected = true
        socket.let { AppData.openmic.client.handleDisconnect(it, code, reason, client_initiated) }
    }
}
