package pl.grzybdev.openmic.client.network

import android.content.Context
import com.gazman.signals.Signals
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorStatus
import pl.grzybdev.openmic.client.interfaces.IConnector

class Listener(private val connector: Connector) : WebSocketListener() {

    lateinit var context: Context
    var connectSignal = Signals.signal(IConnector::class)

    override fun onOpen(webSocket: WebSocket, response: Response) {
        //context?.client?.onOpen(webSocket)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        //context?.client?.onMessage(webSocket, text)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        handleDisconnect()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        handleDisconnect()
        connectSignal.dispatcher.onEvent(connector, ConnectorStatus.NEED_MANUAL_LAUNCH)
    }

    private fun handleDisconnect()
    {
        //context?.client?.handleDisconnect()
    }
}
