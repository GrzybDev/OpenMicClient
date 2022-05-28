package pl.grzybdev.openmic.client.network

import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import pl.grzybdev.openmic.client.network.messages.client.Message

class Client : WebSocketListener() {

    var isListening: Boolean = false

    override fun onOpen(webSocket: WebSocket, response: Response) {
        isListening = true

        webSocket.send(Command.Get(Message.SYSTEM_HELLO))
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(javaClass.name, "onMessage")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        isListening = false

        Log.d(javaClass.name, "onClosing")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        isListening = false

        Log.d(javaClass.name, "onFailure")
        Log.d(javaClass.name, t.message.toString())
    }
}
