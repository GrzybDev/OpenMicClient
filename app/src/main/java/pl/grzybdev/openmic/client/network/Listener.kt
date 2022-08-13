package pl.grzybdev.openmic.client.network

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import pl.grzybdev.openmic.client.enumerators.ConnectionStatus
import pl.grzybdev.openmic.client.singletons.AppData

class Listener : WebSocketListener() {

    private var socket: WebSocket? = null
    private var forceDisconnected = false

    override fun onOpen(webSocket: WebSocket, response: Response) {
        socket = webSocket

        AppData.openmic.wsClient.onOpen(webSocket)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        socket = webSocket

        AppData.openmic.wsClient.onMessage(webSocket, text)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        socket = webSocket

        handleDisconnect(code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        socket = webSocket

        handleDisconnect()
    }

    fun handleDisconnect(code: Int = 1000, reason: String = "")
    {
        if (forceDisconnected)
            return

        forceDisconnected = true
        socket?.let { AppData.openmic.wsClient.handleDisconnect(it, code, reason) }
    }
}
