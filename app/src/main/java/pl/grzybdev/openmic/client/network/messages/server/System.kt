package pl.grzybdev.openmic.client.network.messages.server

import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.WebSocket
import pl.grzybdev.openmic.client.dialogs.AuthDialog
import pl.grzybdev.openmic.client.dialogs.DialogShared
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.network.messages.Message

@Serializable
data class SystemHello(
    override val type: String,
    val serverApp: String,
    val serverVersion: String,
    val serverOS: String,
    val serverName: String,
    val serverID: String,
    val needAuth: Boolean
) : ServerPacket()

@Serializable
data class SystemGoodbye(
    override val type: String,
    val exitCode: Int
) : ServerPacket()

class SystemPacket {
    companion object {
        fun handle(socket: Any, connector: Connector, type: Message, data: String) {
            when (type) {
                Message.SYSTEM_HELLO -> handleHello(socket, connector, data)
                Message.SYSTEM_GOODBYE -> handleGoodbye(socket, connector, data)
                Message.SYSTEM_IS_ALIVE -> handleIsAlive()
                else -> {}
            }
        }

        private fun handleHello(socket: Any, connector: Connector, data: String)  {
            val packet: SystemHello = Json.decodeFromString(data)

            Log.d(SystemPacket::class.java.name, "Connected to: " + packet.serverName)

            /*
            val serverCompat = OpenMic.getServerCompatibility(packet.serverApp, packet.serverVersion)

            if (serverCompat == ServerCompatibility.NOT_SUPPORTED) {
                Log.d(SystemPacket::class.java.name, "Version mismatch, not initializing...")
                // Server should respond with error so client can show error message.
                // Skip initialization, or wait till we get valid Hello packet
                // It's the server who should drop connection
                return
            }

            AppData.serverName = packet.serverName
            AppData.serverID = packet.serverID
            AppData.serverOS = OpenMic.getServerOS(packet.serverOS)

             */

            if (!packet.needAuth) {
                // Server recognizes us, make sure that we recognize server too
                Log.d(
                    SystemPacket::class.java.name,
                    "Server doesn't need auth, checking if server is in known devices list..."
                )

                /*

                val knownDevicesKey: String =
                    OpenMic.App.mainActivity?.getString(R.string.PREFERENCE_APP_KNOWN_DEVICES) ?: ""

                val knownDevices: Set<String> =
                    OpenMic.App.appPreferences?.getStringSet(knownDevicesKey, mutableSetOf()) as Set<String>

                if (knownDevices.contains(packet.serverID)) {
                    // We recognize the server!
                    Log.d(SystemPacket::class.java.name, "Server is in known devices list! Starting audio stream...")
                    Audio.start(socket, connector)
                } else {
                    // We don't recognize the server
                    Log.d(
                        SystemPacket::class.java.name,
                        "Server is not in known devices list! Sending auth request..."
                    )

                    val signal = Signals.signal(IConnector::class)
                    // AppData.currentConn?.let { signal.dispatcher.onEvent(it, ConnectorStatus.CONNECTING) }

                    val response: ClientPacket = AuthClientSide()

                    if (connector != Connector.Bluetooth) {
                        val webSocket = socket as WebSocket
                        webSocket.send(Json.encodeToString(response))
                    } else {
                        val btSocket = socket as BluetoothSocket
                        btSocket.outputStream.write(Json.encodeToString(response).toByteArray())
                    }

                    AuthDialog.show(socket, connector)
                }

                 */
            } else {
                Log.d(SystemPacket::class.java.name, "Server need auth, showing auth popup...")
                AuthDialog.show(socket, connector)
            }
        }

        private fun handleGoodbye(socket: Any, connector: Connector, data: String) {
            val packet: SystemGoodbye = Json.decodeFromString(data)

            Log.d(SystemPacket::class.java.name, "Server says goodbye, exit code ${packet.exitCode}")

            if (DialogShared.current?.isShowing == true)
                DialogShared.current?.dismiss()

            // TODO: Properly handle disconnect
            if (connector != Connector.Bluetooth) {
                val webSocket = socket as WebSocket
                webSocket.close(1000, "Normal disconnect")
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