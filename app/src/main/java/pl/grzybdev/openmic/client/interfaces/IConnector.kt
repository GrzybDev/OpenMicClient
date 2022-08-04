package pl.grzybdev.openmic.client.interfaces

import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorStatus

fun interface IConnector {
    fun onEvent(connector: Connector, event: ConnectorStatus)
}
