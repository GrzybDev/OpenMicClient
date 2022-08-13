package pl.grzybdev.openmic.client.receivers.connectors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorState
import pl.grzybdev.openmic.client.receivers.signals.ConnectorSignalReceiver
import pl.grzybdev.openmic.client.singletons.AppData


class USBStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
            // Are we charging / charged?
            val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            // How are we charging?
            val chargePlug: Int = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
            val acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC

            val isConnectedToPC = isCharging && usbCharge && !acCharge

            OpenMic.changeConnectorStatus(context!!, Connector.USB,
                if (isConnectedToPC)
                    ConnectorState.USB_CONNECTED
                else
                    ConnectorState.USB_NOT_CONNECTED)
        }
    }

}
