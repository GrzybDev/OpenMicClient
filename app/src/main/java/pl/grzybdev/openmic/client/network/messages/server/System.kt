package pl.grzybdev.openmic.client.network.messages.server

import android.app.AlertDialog
import android.text.InputType
import android.util.Log
import android.widget.EditText
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.WebSocket
import pl.grzybdev.openmic.client.AppData
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.network.messages.ExitCode
import pl.grzybdev.openmic.client.network.messages.Message
import pl.grzybdev.openmic.client.network.messages.client.ClientPacket
import pl.grzybdev.openmic.client.network.messages.client.SystemGoodbye as ClientSystemGoodbye

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
            }
        }

        private fun handleHello(data: String, socket: WebSocket) {
            val packet: SystemHello = Json.decodeFromString(data)

            Log.d(SystemPacket::class.java.name, "Connected to: " + packet.serverName)

            if (!packet.needAuth) {
                // Server recognizes us, make sure that we recognize server too
                Log.d(
                    SystemPacket::class.java.name,
                    "Server doesn't need auth, checking if server is in known devices list..."
                )

                val knownDevicesKey: String =
                    AppData.mainActivity?.getString(R.string.PREFERENCE_APP_KNOWN_DEVICES) ?: ""

                val knownDevices: Set<String> =
                    AppData.appPreferences?.getStringSet(knownDevicesKey, null) as Set<String>

                if (knownDevices.contains(packet.serverID)) {
                    // We recognize the server!
                    // TODO: Setup audio and start transmitting
                    Log.d(SystemPacket::class.java.name, "Server is in known devices list!")
                } else {
                    // We don't recognize the server
                    Log.d(
                        SystemPacket::class.java.name,
                        "Server is not in known devices list! Sending auth request..."
                    )
                }
            } else {
                Log.d(SystemPacket::class.java.name, "Server need auth, showing auth popup...")

                AppData.mainActivity?.runOnUiThread {
                    // Build AlertDialog
                    val builder: AlertDialog.Builder = AlertDialog.Builder(AppData.mainActivity)
                    builder.setTitle(AppData.mainActivity?.getString(R.string.AuthDialog_Title))

                    // Set up the input
                    val input = EditText(AppData.mainActivity)
                    input.inputType = InputType.TYPE_CLASS_NUMBER
                    builder.setView(input)

                    // Set up the buttons
                    builder.setPositiveButton(AppData.mainActivity?.getString(R.string.AuthDialog_Button_OK)) { _, _ ->
                        run {
                            Log.d(
                                SystemPacket::class.java.name,
                                "Entered code: " + input.text.toString()
                            )
                        }
                    }

                    builder.setNegativeButton(AppData.mainActivity?.getString(R.string.AuthDialog_Button_Cancel)) { dialog, _ ->
                        run {
                            Log.d(SystemPacket::class.java.name, "Canceled auth dialog! Disconnecting...")

                            val goodbye: ClientPacket = ClientSystemGoodbye(ExitCode.CANCELED_AUTH_CODE_DIALOG.code)
                            socket.send(Json.encodeToString(goodbye))

                            dialog.cancel()
                        }
                    }

                    builder.show()
                }
            }
        }

        private fun handleGoodbye(data: String, socket: WebSocket) {
            val packet: SystemGoodbye = Json.decodeFromString(data)

            Log.d(SystemPacket::class.java.name, "Server says goodbye, exit code ${packet.exitCode}")

            // TODO: Properly handle disconnect
            socket.close(1000, "Normal disconnect")
        }
    }
}