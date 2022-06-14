package pl.grzybdev.openmic.client.dataclasses

import pl.grzybdev.openmic.client.enumerators.ServerCompatibility
import pl.grzybdev.openmic.client.enumerators.ServerOS

data class ServerEntry(val serverName: String, val serverIP: String, val serverPort: Short, val serverCompat: ServerCompatibility, val serverOS: ServerOS, val serverID: String)
