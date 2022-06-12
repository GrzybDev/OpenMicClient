package pl.grzybdev.openmic.client.interfaces

import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorEvent

fun interface IConnector {
    fun onEvent(connector: Connector, event: ConnectorEvent)
}
