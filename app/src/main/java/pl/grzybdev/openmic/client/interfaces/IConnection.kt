package pl.grzybdev.openmic.client.interfaces

import pl.grzybdev.openmic.client.enumerators.network.ConnectionStatus

interface IConnection {
    fun onConnectionStateChange(status: ConnectionStatus)
}
