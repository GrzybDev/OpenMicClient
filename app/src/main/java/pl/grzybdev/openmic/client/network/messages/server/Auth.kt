package pl.grzybdev.openmic.client.network.messages.server

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.WebSocket
import pl.grzybdev.openmic.client.AppData
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.network.Audio
import pl.grzybdev.openmic.client.network.messages.Message

@Serializable
data class AuthCodeVerify(
    override val type: String
) : ServerPacket()

class AuthPacket {
    companion object {
        fun handle(type: Message, data: String, socket: WebSocket) {
            when (type) {
                Message.AUTH_CODE_VERIFY -> handleCodeVerify(data, socket)
                else -> {}
            }
        }

        private fun handleCodeVerify(data: String, socket: WebSocket) {
            // Only "positive" packet is handled here
            Log.d(AuthPacket::class.java.name, "Received successful AuthCodeVerify: $data")
            Log.d(AuthPacket::class.java.name, "Authorization complete, adding ${AppData.serverID} to known servers list...")

            val knownDevicesKey: String =
                OpenMic.App.mainActivity?.getString(R.string.PREFERENCE_APP_KNOWN_DEVICES) ?: ""

            var knownDevices: Set<String> =
                OpenMic.App.appPreferences?.getStringSet(knownDevicesKey, mutableSetOf()) as Set<String>

            val mutableKnownDevices = knownDevices.toMutableSet()
            mutableKnownDevices.add(AppData.serverID)

            knownDevices = mutableKnownDevices.toSet()

            with (OpenMic.App.appPreferences!!.edit()) {
                putStringSet(knownDevicesKey, knownDevices)
                apply()
            }

            Audio.initAudio(socket)
        }
    }
}