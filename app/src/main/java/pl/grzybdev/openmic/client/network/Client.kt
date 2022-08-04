package pl.grzybdev.openmic.client.network

import android.bluetooth.BluetoothSocket
import android.util.Log
import com.gazman.signals.Signals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.WebSocket
import pl.grzybdev.openmic.client.AppData
import pl.grzybdev.openmic.client.BuildConfig
import pl.grzybdev.openmic.client.dialogs.DialogShared
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.interfaces.IError
import pl.grzybdev.openmic.client.network.messages.ErrorCode
import pl.grzybdev.openmic.client.network.messages.Message
import pl.grzybdev.openmic.client.network.messages.client.ClientPacket
import pl.grzybdev.openmic.client.network.messages.client.SystemHello
import pl.grzybdev.openmic.client.network.messages.server.BasePacket
import pl.grzybdev.openmic.client.network.messages.server.ErrorPacket
import pl.grzybdev.openmic.client.network.messages.server.ServerPacket

class Client(private val connector: Connector?) {

    var isConnected: Boolean = false

    fun onOpen(socket: Any) {
        isConnected = true

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
                    Handler.handlePacket(socket, connector, mType, text)
                }
            } else {
                Log.e(javaClass.name, "Unknown message type! ($mType) Disconnecting...")

                if (connector != Connector.Bluetooth) {
                    val webSocket = socket as WebSocket
                    webSocket.close(1003, "Unknown message type")
                } else {
                    val btSocket = socket as BluetoothSocket
                    btSocket.close()
                }
            }
        } else {
            Log.e(javaClass.name, "Received error packet, showing dialog...")
            val packet = pBase as ErrorPacket
            val errorSignal = Signals.signal(IError::class)
            val errorType: ErrorCode? = ErrorCode.values().find { it.code == packet.error }

            if (errorType == null) {
                Log.e(javaClass.name, "Unknown error code! (${packet.error}), disconnecting...")

                if (connector != Connector.Bluetooth) {
                    val webSocket = socket as WebSocket
                    webSocket.close(1003, "Unknown error code")
                } else {
                    val btSocket = socket as BluetoothSocket
                    btSocket.close()
                }

                return
            }

            errorSignal.dispatcher.onErrorMessage(errorType)

            /*
            OpenMic.App.mainActivity?.runOnUiThread {
                val builder: AlertDialog.Builder = AlertDialog.Builder(OpenMic.App.mainActivity)
                builder.setTitle(OpenMic.App.mainActivity?.getString(R.string.ErrorDialog_Title))
                builder.setMessage(packet.message)
                builder.setPositiveButton(OpenMic.App.mainActivity?.getString(R.string.ErrorDialog_Button_OK)) { _, _ -> AppData.connectLock = false }
                builder.show()
            }

             */
        }
    }

    fun handleDisconnect()
    {
        isConnected = false
        // AppData.connectLock = false

        DialogShared.current?.dismiss()
        // OpenMic.App.mainActivity?.onDisconnect()
        Audio.stop()

        // bOpenMic.App.context?.initClient()
    }

}