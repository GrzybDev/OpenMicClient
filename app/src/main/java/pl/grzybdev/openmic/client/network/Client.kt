package pl.grzybdev.openmic.client.network

import android.app.AlertDialog
import android.util.Log
import com.gazman.signals.Signals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import pl.grzybdev.openmic.client.AppData
import pl.grzybdev.openmic.client.BuildConfig
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.dialogs.DialogShared
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorEvent
import pl.grzybdev.openmic.client.interfaces.IConnector
import pl.grzybdev.openmic.client.interfaces.IError
import pl.grzybdev.openmic.client.network.messages.ErrorCode
import pl.grzybdev.openmic.client.network.messages.Message
import pl.grzybdev.openmic.client.network.messages.client.ClientPacket
import pl.grzybdev.openmic.client.network.messages.client.SystemHello
import pl.grzybdev.openmic.client.network.messages.server.BasePacket
import pl.grzybdev.openmic.client.network.messages.server.ErrorPacket
import pl.grzybdev.openmic.client.network.messages.server.ServerPacket

class Client(private val connector: Connector) : WebSocketListener() {

    var isConnected: Boolean = false

    private val usbConnectSignal = Signals.signal(IConnector::class)

    override fun onOpen(webSocket: WebSocket, response: Response) {
        isConnected = true
        AppData.currentConn = connector

        val packet: ClientPacket = SystemHello(BuildConfig.APPLICATION_ID, BuildConfig.VERSION_NAME, AppData.deviceID)
        webSocket.send(Json.encodeToString(packet))
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val json = Json { ignoreUnknownKeys = true }
        val pBase: ServerPacket = json.decodeFromString(Handler.Companion.PacketSerializer, text)

        if (pBase is BasePacket) {
            val mType: Message? = Message.values().find { it.type == pBase.type }

            if (mType != null) {
                Handler.handlePacket(webSocket, mType, text)
            } else {
                Log.e(javaClass.name, "Unknown message type! ($mType) Disconnecting...")
                webSocket.close(1003, "Unknown message type")
            }
        } else {
            Log.e(javaClass.name, "Received error packet, showing dialog...")
            val packet = pBase as ErrorPacket
            val errorSignal = Signals.signal(IError::class)
            val errorType: ErrorCode? = ErrorCode.values().find { it.code == packet.error }

            if (errorType == null) {
                Log.e(javaClass.name, "Unknown error code! (${packet.error}), disconnecting...")
                webSocket.close(1003, "Unknown error code")
                return
            }

            errorSignal.dispatcher.onErrorMessage(errorType)

            OpenMic.App.mainActivity?.runOnUiThread {
                val builder: AlertDialog.Builder = AlertDialog.Builder(OpenMic.App.mainActivity)
                builder.setTitle(OpenMic.App.mainActivity?.getString(R.string.ErrorDialog_Title))
                builder.setMessage(packet.message)
                builder.setPositiveButton(OpenMic.App.mainActivity?.getString(R.string.ErrorDialog_Button_OK)) { _, _ -> AppData.connectLock = false }
                builder.show()
            }
        }
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
        AppData.connectLock = false
        isConnected = false

        DialogShared.current?.dismiss()

        if (connector == Connector.USB)
            usbConnectSignal.dispatcher.onEvent(connector, ConnectorEvent.NEED_MANUAL_LAUNCH)
        else
            usbConnectSignal.dispatcher.onEvent(connector, ConnectorEvent.CONNECTED_OR_READY)
    }
}
