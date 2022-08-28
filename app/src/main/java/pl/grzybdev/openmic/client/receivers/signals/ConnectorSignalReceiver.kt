package pl.grzybdev.openmic.client.receivers.signals

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import pl.grzybdev.openmic.client.enumerators.network.Connector
import pl.grzybdev.openmic.client.enumerators.network.ConnectorState
import pl.grzybdev.openmic.client.interfaces.IConnector
import pl.grzybdev.openmic.client.singletons.AppData

class ConnectorSignalReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "UpdateState") {
            val connectorInt: Int = intent.getIntExtra("connector", -1)
            val connector: Connector = Connector.values()[connectorInt]

            val stateInt: Int = intent.getIntExtra("state", -1)
            val state: ConnectorState = ConnectorState.values()[stateInt]

            Log.d(javaClass.name, "Connector: $connector, State: $state")

            when (connector) {
                Connector.USB -> {
                    if (state == ConnectorState.USB_CONNECTED || state == ConnectorState.USB_CHECKING)
                    {
                        if (AppData.usbState == ConnectorState.READY
                            || AppData.usbState == ConnectorState.USB_CONNECTED_NO_SERVER)
                            return
                    }

                    AppData.usbState = state
                }
                else -> {}
            }

            notifyAllListeners(connector, state)
        }
    }

    private fun notifyAllListeners(connector: Connector, state: ConnectorState)
    {
        for (listener in AppData.connectorListeners)
        {
            when (connector)
            {
                Connector.USB -> {
                    listener.onUSBStateChange(state)
                }

                Connector.WiFi -> {
                    listener.onWiFiStateChange(state)
                }

                Connector.Bluetooth -> {
                    listener.onBluetoothStateChange(state)
                }
            }
        }
    }

    fun addListener(listener: IConnector)
    {
        AppData.connectorListeners.add(listener)
    }

    fun removeListener(listener: IConnector)
    {
        AppData.connectorListeners.remove(listener)
    }

}
