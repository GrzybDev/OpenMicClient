package pl.grzybdev.openmic.client.receivers

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.gazman.signals.Signals
import pl.grzybdev.openmic.client.AppData
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.dataclasses.ServerEntry
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorStatus
import pl.grzybdev.openmic.client.enumerators.ServerCompatibility
import pl.grzybdev.openmic.client.enumerators.ServerOS
import pl.grzybdev.openmic.client.interfaces.IConnector

class BTStateReceiver : BroadcastReceiver() {

    private val connectSignal = Signals.signal(IConnector::class)

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            BluetoothDevice.ACTION_FOUND -> run {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                /*
                if (OpenMic.App.mainActivity?.applicationContext?.let {
                        ActivityCompat.checkSelfPermission(
                            it,
                            Manifest.permission.BLUETOOTH_CONNECT
                        )
                    } != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d(javaClass.name, "App doesn't have BLUETOOTH_CONNECT permission, ignoring onReceive signal!")
                    return
                }

                if (device?.name != null) {
                    Log.d(javaClass.name, "Found Bluetooth device: ${device.name}")
                    AppData.foundServers[device.address] = ServerEntry(device.name, device.address, ServerCompatibility.UNKNOWN, ServerOS.OTHER, device.address, Connector.Bluetooth)
                    AppData.serverAdapter.updateData()
                    connectSignal.dispatcher.onEvent(Connector.Bluetooth, ConnectorStatus.NEED_MANUAL_LAUNCH)
                }

                 */
            }

            BluetoothAdapter.ACTION_STATE_CHANGED -> run {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                    BluetoothAdapter.STATE_OFF -> connectSignal.dispatcher.onEvent(Connector.Bluetooth, ConnectorStatus.DISABLED)
                    BluetoothAdapter.STATE_ON -> connectSignal.dispatcher.onEvent(Connector.Bluetooth, ConnectorStatus.CONNECTED_OR_READY)
                }
            }
        }
    }

}