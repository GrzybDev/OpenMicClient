package pl.grzybdev.openmic.client.network

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import pl.grzybdev.openmic.client.enumerators.Connector

class Listener(private val connector: Connector) : WebSocketListener() {

    private var socket: WebSocket? = null

    override fun onOpen(webSocket: WebSocket, response: Response) {
        socket = webSocket

        //context?.client?.onOpen(webSocket)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        socket = webSocket

        //context?.client?.onMessage(webSocket, text)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        socket = webSocket

        handleDisconnect()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        socket = webSocket
        handleDisconnect()
    }

    private fun handleDisconnect()
    {
        //context?.client?.handleDisconnect()
    }

    fun forceClose()
    {
        socket?.close(1000, "")
    }
}
