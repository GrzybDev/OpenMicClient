package pl.grzybdev.openmic.client.network.messages.server

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.enumerators.DialogType
import pl.grzybdev.openmic.client.network.messages.Message
import pl.grzybdev.openmic.client.singletons.AppData
import pl.grzybdev.openmic.client.singletons.ServerData
import pl.grzybdev.openmic.client.network.messages.client.AuthClient as Client_AuthClient

@Serializable
data class AuthClient(
    override val type: String,
    val authorized: Boolean,
): ServerPacket()

@Serializable
data class AuthCodeVerify(
    override val type: String
) : ServerPacket()

class AuthPacket {
    companion object {
        fun handle(context: Context, socket: Any, type: Message, data: String) {
            when (type) {
                Message.AUTH_CLIENT -> handleAuthClient(context, socket, data)
                Message.AUTH_CODE_VERIFY -> handleCodeVerify(context)
                else -> {}
            }
        }

        private fun handleAuthClient(context: Context, socket: Any, data: String) {
            val packet = Json.decodeFromString<AuthClient>(data)

            val knownDevices: Set<String> = AppData.sharedPrefs?.getStringSet(context.getString(R.string.PREFERENCE_APP_KNOWN_DEVICES), mutableSetOf()) as Set<String>

            if (packet.authorized && knownDevices.contains(ServerData.id))
                AppData.audio.initialize()
            else
                OpenMic.showDialog(context, DialogType.AUTH, null)
        }

        private fun handleCodeVerify(context: Context) {
            // Only "positive" packet is handled here
            Log.d(AuthPacket::class.java.name, "Authorization complete, adding ${ServerData.id} to known servers list...")

            val knownDevicesKey: String = context.getString(R.string.PREFERENCE_APP_KNOWN_DEVICES)

            var knownDevices: Set<String> = AppData.sharedPrefs?.getStringSet(knownDevicesKey, mutableSetOf()) as Set<String>
            val mutableKnownDevices = knownDevices.toMutableSet()
            mutableKnownDevices.add(ServerData.id)

            knownDevices = mutableKnownDevices.toSet()

            with (AppData.sharedPrefs!!.edit()) {
                putStringSet(knownDevicesKey, knownDevices)
                apply()
            }

            AppData.openmic.client.sendPacket(Client_AuthClient())
        }
    }
}
