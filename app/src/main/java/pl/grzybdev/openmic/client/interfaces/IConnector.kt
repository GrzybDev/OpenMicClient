package pl.grzybdev.openmic.client.interfaces

import pl.grzybdev.openmic.client.enumerators.ConnectorState

interface IConnector {
    fun onUSBStateChange(status: ConnectorState)
}
