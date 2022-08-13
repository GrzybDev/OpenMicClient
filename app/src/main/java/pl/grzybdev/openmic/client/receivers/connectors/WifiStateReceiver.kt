@file:Suppress("DEPRECATION")

package pl.grzybdev.openmic.client.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager


class WifiStateReceiver : BroadcastReceiver() {

    // private val connectSignal = Signals.signal(IConnector::class)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ConnectivityManager.CONNECTIVITY_ACTION)
        {
            var isConnected = false

            /*

            val connectivityManager = OpenMic.App.mainActivity?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

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
            */

            // connectSignal.dispatcher.onEvent(Connector.WiFi, if (isConnected) ConnectorStatus.CONNECTED_OR_READY else ConnectorStatus.DISABLED)
        }
    }

}