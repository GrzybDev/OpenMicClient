package pl.grzybdev.openmic.client.network.messages.server

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.WebSocket
import pl.grzybdev.openmic.client.AppData
import pl.grzybdev.openmic.client.BuildConfig
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.dialogs.AuthDialog
import pl.grzybdev.openmic.client.dialogs.DialogShared
import pl.grzybdev.openmic.client.enumerators.ServerOS
import pl.grzybdev.openmic.client.network.Audio
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
        fun handle(type: Message, data: String, socket: WebSocket) {
            when (type) {
                Message.SYSTEM_HELLO -> handleHello(data, socket)
                Message.SYSTEM_GOODBYE -> handleGoodbye(data, socket)
                else -> {}
            }
        }

        private fun handleHello(data: String, socket: WebSocket) {
            val packet: SystemHello = Json.decodeFromString(data)

            Log.d(SystemPacket::class.java.name, "Connected to: " + packet.serverName)

            if (packet.serverApp == OpenMic.App.mainActivity?.getString(R.string.SERVER_APP_NAME)) {
                // It's official app, check if versions match

                if (packet.serverVersion != BuildConfig.VERSION_NAME) {
                    Log.d(SystemPacket::class.java.name, "Version mismatch, not initializing...")

                    // Server should respond with error so client can show error message.
                    // Skip initialization, or wait till we get valid Hello packet
                    // It's the server who should drop connection
                    return
                }
            }

            AppData.serverName = packet.serverName
            AppData.serverID = packet.serverID
            AppData.serverOS = ServerOS.values().find { it.kernelType == packet.serverOS }!!

            if (!packet.needAuth) {
                // Server recognizes us, make sure that we recognize server too
                Log.d(
                    SystemPacket::class.java.name,
                    "Server doesn't need auth, checking if server is in known devices list..."
                )

                val knownDevicesKey: String =
                    OpenMic.App.mainActivity?.getString(R.string.PREFERENCE_APP_KNOWN_DEVICES) ?: ""

                val knownDevices: Set<String> =
                    OpenMic.App.appPreferences?.getStringSet(knownDevicesKey, mutableSetOf()) as Set<String>

                if (knownDevices.contains(packet.serverID)) {
                    // We recognize the server!
                    Log.d(SystemPacket::class.java.name, "Server is in known devices list!")
                    Audio.initAudio(socket)
                } else {
                    // We don't recognize the server
                    Log.d(
                        SystemPacket::class.java.name,
                        "Server is not in known devices list! Sending auth request..."
                    )
                }
            } else {
                Log.d(SystemPacket::class.java.name, "Server need auth, showing auth popup...")
                AuthDialog.show(socket)
            }
        }

        private fun handleGoodbye(data: String, socket: WebSocket) {
            val packet: SystemGoodbye = Json.decodeFromString(data)

            Log.d(SystemPacket::class.java.name, "Server says goodbye, exit code ${packet.exitCode}")

            if (DialogShared.current?.isShowing == true)
                DialogShared.current?.dismiss()

            // TODO: Properly handle disconnect
            socket.close(1000, "Normal disconnect")
        }
    }
}