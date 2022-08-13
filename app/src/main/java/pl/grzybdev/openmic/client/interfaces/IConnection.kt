package pl.grzybdev.openmic.client.interfaces

import pl.grzybdev.openmic.client.enumerators.ConnectionStatus

interface IConnection {
    fun onConnectionStateChange(status: ConnectionStatus)
}
