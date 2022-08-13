package pl.grzybdev.openmic.client.dataclasses

import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ServerVersion

data class ServerEntry(val serverName: String,
                       val serverIP: String,
                       val serverCompat: ServerVersion,
                       val serverOS: String,
                       val serverID: String,
                       val connector: Connector)
