package pl.grzybdev.openmic.client.interfaces

import pl.grzybdev.openmic.client.enumerators.network.ConnectorState

interface IConnector {
    fun onUSBStateChange(status: ConnectorState)
    fun onWiFiStateChange(status: ConnectorState)
    fun onBluetoothStateChange(status: ConnectorState)
}
