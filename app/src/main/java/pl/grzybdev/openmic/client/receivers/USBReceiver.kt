package pl.grzybdev.openmic.client.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import com.gazman.signals.Signals
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorEvent
import pl.grzybdev.openmic.client.interfaces.IConnector


class USBReceiver : BroadcastReceiver() {

    private var lastState: Boolean? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
            val connectSignal = Signals.signal(IConnector::class)

            // Are we charging / charged?
            val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            // How are we charging?
            val chargePlug: Int = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
            val acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC

            val isConnectedToPC = isCharging && usbCharge && !acCharge

            if (lastState == isConnectedToPC) return
            lastState = isConnectedToPC

            val event: ConnectorEvent = when (isConnectedToPC) {
                false -> ConnectorEvent.DISABLED
                true -> ConnectorEvent.CONNECTING
            }

            connectSignal.dispatcher.onEvent(Connector.USB, event)
        }
    }

}
