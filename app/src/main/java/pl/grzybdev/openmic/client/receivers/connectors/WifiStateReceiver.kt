@file:Suppress("DEPRECATION")

package pl.grzybdev.openmic.client.receivers.connectors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.enumerators.network.Connector
import pl.grzybdev.openmic.client.enumerators.network.ConnectorState


class WifiStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ConnectivityManager.CONNECTIVITY_ACTION)
        {
            var isConnected = false
            val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val networkCapabilities = connectivityManager.activeNetwork
                val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities)

                if (actNw != null) {
                    isConnected = actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                }
            } else {
                connectivityManager.run {
                    connectivityManager.activeNetworkInfo?.run {
                        isConnected = type == ConnectivityManager.TYPE_WIFI
                    }
                }
            }

            OpenMic.changeConnectorStatus(context, Connector.WiFi,
                if (isConnected) ConnectorState.READY
                else ConnectorState.NOT_READY)
        }
    }

}
