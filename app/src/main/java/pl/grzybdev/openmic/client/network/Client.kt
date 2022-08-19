package pl.grzybdev.openmic.client.network

import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.WebSocket
import pl.grzybdev.openmic.client.BuildConfig
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.enumerators.network.ConnectionStatus
import pl.grzybdev.openmic.client.enumerators.network.Connector
import pl.grzybdev.openmic.client.enumerators.DialogType
import pl.grzybdev.openmic.client.enumerators.ServerVersion
import pl.grzybdev.openmic.client.network.messages.ErrorCode
import pl.grzybdev.openmic.client.network.messages.Message
import pl.grzybdev.openmic.client.network.messages.client.ClientPacket
import pl.grzybdev.openmic.client.network.messages.client.SystemHello
import pl.grzybdev.openmic.client.network.messages.server.BasePacket
import pl.grzybdev.openmic.client.network.messages.server.ErrorPacket
import pl.grzybdev.openmic.client.network.messages.server.ServerPacket
import pl.grzybdev.openmic.client.singletons.AppData
import pl.grzybdev.openmic.client.singletons.ServerData

class Client(val context: Context?, private val connector: Connector?) {

    private var socket: Any? = null

    fun onOpen(socket: Any) {
        this.socket = socket

        val packet: ClientPacket = SystemHello(BuildConfig.APPLICATION_ID, BuildConfig.VERSION_NAME, AppData.deviceID)
        sendPacket(packet)
    }

    fun onMessage(socket: Any, text: String) {
        this.socket = socket

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
            val errorType: ErrorCode? = ErrorCode.values().find { it.code == packet.error }

            if (errorType != null) {
                if (errorType == ErrorCode.AUTH_CODE_INVALID)
                    OpenMic.showDialog(context!!, DialogType.AUTH, packet.message)

                OpenMic.showDialog(context!!, DialogType.SERVER_ERROR, packet.message)
            } else {
                val dialogStr = context?.getString(R.string.dialog_disconnect_unknown_error, packet.message)

                if (dialogStr != null) {
                    handleDisconnect(socket, reason = dialogStr)
                }
            }
        }
    }

    fun handleDisconnect(socket: Any, code: Int = 1000, reason: String = "")
    {
        if (context == null) { return }
        this.socket = socket

        OpenMic.changeConnectionStatus(context, ConnectionStatus.DISCONNECTING)

        if (connector != Connector.Bluetooth) {
            val webSocket = socket as WebSocket
            webSocket.close(code, reason)
        } else {
            val btSocket = socket as BluetoothSocket
            btSocket.close()
        }

        ServerData.id = ""
        ServerData.os = ""
        ServerData.version = ServerVersion.UNKNOWN
        ServerData.name = ""

        OpenMic.showDialog(context, DialogType.SERVER_DISCONNECT, reason)
        OpenMic.changeConnectionStatus(context, ConnectionStatus.DISCONNECTED)
    }

    fun sendPacket(packet: ClientPacket) {
        if (connector != Connector.Bluetooth) {
            val webSocket = socket as WebSocket
            webSocket.send(Json.encodeToString(packet))
        } else {
            val btSocket = socket as BluetoothSocket
            btSocket.outputStream.write(Json.encodeToString(packet).toByteArray())
        }
    }
}