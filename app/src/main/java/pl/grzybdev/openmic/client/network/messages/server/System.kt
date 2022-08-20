package pl.grzybdev.openmic.client.network.messages.server

import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.WebSocket
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.enumerators.network.ConnectionStatus
import pl.grzybdev.openmic.client.enumerators.network.Connector
import pl.grzybdev.openmic.client.enumerators.ServerVersion
import pl.grzybdev.openmic.client.network.messages.Message
import pl.grzybdev.openmic.client.singletons.ServerData

@Serializable
data class SystemHello(
    override val type: String,
    val serverApp: String,
    val serverVersion: String,
    val serverOS: String,
    val serverName: String,
    val serverID: String,
) : ServerPacket()

@Serializable
data class SystemGoodbye(
    override val type: String,
    val exitCode: Int
) : ServerPacket()

class SystemPacket {
    companion object {
        fun handle(context: Context, socket: Any, connector: Connector, type: Message, data: String) {
            when (type) {
                Message.SYSTEM_HELLO -> handleHello(context, data)
                Message.SYSTEM_GOODBYE -> handleGoodbye(socket, connector, data)
                Message.SYSTEM_IS_ALIVE -> handleIsAlive()
                else -> {}
            }
        }

        private fun handleHello(context: Context, data: String)  {
            val packet: SystemHello = Json.decodeFromString(data)

            Log.d(SystemPacket::class.java.name, "Connected to: " + packet.serverName)

            val serverVer = OpenMic.getServerVersion(packet.serverApp, packet.serverVersion)

            if (serverVer == ServerVersion.MISMATCH) {
                Log.d(SystemPacket::class.java.name, "Version mismatch, not initializing...")
                // Server should respond with error so client can show error message.
                // Skip initialization, or wait till we get valid Hello packet
                // It's the server who should drop connection
                return
            }

            ServerData.name = packet.serverName
            ServerData.id = packet.serverID
            ServerData.os = packet.serverOS
            ServerData.version = serverVer

            OpenMic.changeConnectionStatus(context, ConnectionStatus.CONNECTED)
        }

        private fun handleGoodbye(socket: Any, connector: Connector, data: String) {
            val packet: SystemGoodbye = Json.decodeFromString(data)

            Log.d(SystemPacket::class.java.name, "Server says goodbye, exit code ${packet.exitCode}")

            if (connector != Connector.Bluetooth) {
                val webSocket = socket as WebSocket
                webSocket.close(1000, null)
            } else {
                val btSocket = socket as BluetoothSocket
                btSocket.close()
            }
        }

        private fun handleIsAlive() {
            Log.d(SystemPacket::class.java.name, "Server is alive")
        }
    }
}