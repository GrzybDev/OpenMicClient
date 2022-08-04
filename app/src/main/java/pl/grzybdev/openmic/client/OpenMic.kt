package pl.grzybdev.openmic.client

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorStatus
import pl.grzybdev.openmic.client.network.Listener
import pl.grzybdev.openmic.client.network.USBCheckListener
import java.util.concurrent.TimeUnit


class OpenMic {

    companion object {
        fun connectTo(connector: Connector, address: String) {
            AppData.connectSignal.dispatcher.onEvent(Connector.USB, ConnectorStatus.CONNECTING)

            Log.d(OpenMic::class.java.name, "connectTo: Trying to connect to $address, via $connector...")

            if (connector != Connector.Bluetooth) {
                val listener = Listener(connector)

                val httpClient = OkHttpClient.Builder()
                    .readTimeout(20, TimeUnit.SECONDS)
                    .pingInterval(20, TimeUnit.SECONDS)
                    .build()

                val webRequest = Request.Builder()
                    .url("ws://$address:${AppData.communicationPort}")
                    .build()

                httpClient.newWebSocket(webRequest, listener)
                httpClient.dispatcher.executorService.shutdown()
            } else {
                // val client = Client(connector)
            }
        }

        fun usbCheck(address: String) {
            Log.d(OpenMic::class.java.name, "usbCheck: Checking if USB device has OpenMic Server running...")

            AppData.connectSignal.dispatcher.onEvent(Connector.USB, ConnectorStatus.USB_CHECKING)

            val listener = USBCheckListener()

            val httpClient = OkHttpClient.Builder()
                .readTimeout(20, TimeUnit.SECONDS)
                .pingInterval(20, TimeUnit.SECONDS)
                .build()

            val webRequest = Request.Builder()
                .url("ws://$address:${AppData.communicationPort}")
                .build()

            httpClient.newWebSocket(webRequest, listener)
            httpClient.dispatcher.executorService.shutdown()
        }
    }
}
