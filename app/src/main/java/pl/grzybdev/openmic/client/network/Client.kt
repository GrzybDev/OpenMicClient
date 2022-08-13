package pl.grzybdev.openmic.client.network

import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.WebSocket
import pl.grzybdev.openmic.client.BuildConfig
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.enumerators.ConnectionStatus
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.network.messages.ErrorCode
import pl.grzybdev.openmic.client.network.messages.Message
import pl.grzybdev.openmic.client.network.messages.client.ClientPacket
import pl.grzybdev.openmic.client.network.messages.client.SystemHello
import pl.grzybdev.openmic.client.network.messages.server.BasePacket
import pl.grzybdev.openmic.client.network.messages.server.ErrorPacket
import pl.grzybdev.openmic.client.network.messages.server.ServerPacket
import pl.grzybdev.openmic.client.singletons.AppData

class Client(val context: Context?, private val connector: Connector?) {

    fun onOpen(socket: Any) {
        val packet: ClientPacket = SystemHello(BuildConfig.APPLICATION_ID, BuildConfig.VERSION_NAME, AppData.deviceID)

        if (connector != Connector.Bluetooth) {
            val webSocket = socket as WebSocket
            webSocket.send(Json.encodeToString(packet))
        } else {
            val btSocket = socket as BluetoothSocket
            btSocket.outputStream.write(Json.encodeToString(packet).toByteArray())
        }
    }

    fun onMessage(socket: Any, text: String) {
        val json = Json { ignoreUnknownKeys = true }
        val pBase: ServerPacket = json.decodeFromString(Handler.Companion.PacketSerializer, text)

        if (pBase is BasePacket) {
            val mType: Message? = Message.values().find { it.type == pBase.type }

            if (mType != null) {
                if (connector != null) {
                    context?.let { Handler.handlePacket(it, socket, connector, mType, text) }
                }
            } else {
                Log.w(javaClass.name, "Unknown message type! ($mType) Disconnecting...")
                handleDisconnect(socket, 1003)
            }
        } else {
            Log.e(javaClass.name, "Received error packet, showing dialog...")
            val packet = pBase as ErrorPacket
            // val dialogSignal = Signals.signal(IDialog::class)
            val errorType: ErrorCode? = ErrorCode.values().find { it.code == packet.error }

            if (errorType != null) {
                // dialogSignal.dispatcher.onEvent(DialogType.SERVER_ERROR, packet.message)
            } else {
                handleDisconnect(socket)
            }
        }
    }

    fun handleDisconnect(socket: Any, code: Int = 1000)
    {
        if (context == null) { return }

        OpenMic.changeConnectionStatus(context, ConnectionStatus.DISCONNECTING)

        if (connector != Connector.Bluetooth) {
            val webSocket = socket as WebSocket
            webSocket.close(code, null)
        } else {
            val btSocket = socket as BluetoothSocket
            btSocket.close()
        }

        OpenMic.changeConnectionStatus(context, ConnectionStatus.DISCONNECTED)
    }

}