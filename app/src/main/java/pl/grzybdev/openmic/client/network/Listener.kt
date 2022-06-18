package pl.grzybdev.openmic.client.network

import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import pl.grzybdev.openmic.client.OpenMic

class Listener : WebSocketListener() {

    var isConnected: Boolean = false
    var context = OpenMic.App.context

    override fun onOpen(webSocket: WebSocket, response: Response) {
        isConnected = true
        context?.client?.onOpen(webSocket)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        context?.client?.onMessage(webSocket, text)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(javaClass.name, "onClosing")

        handleDisconnect()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.d(javaClass.name, "onFailure")
        Log.d(javaClass.name, t.message.toString())

        handleDisconnect()
    }

    private fun handleDisconnect()
    {
        isConnected = false
        context?.client?.handleDisconnect()
    }
}
