package pl.grzybdev.openmic.client.network.messages.server

enum class ServerMessage {
    UNKNOWN,
    SYSTEM_HELLO
}

class MessageType {
    companion object {
        fun fromString(type: String): ServerMessage {
            return when (type.replace("\"", "")) {
                "System_Hello" -> ServerMessage.SYSTEM_HELLO
                else -> ServerMessage.UNKNOWN
            }
        }
    }
}
