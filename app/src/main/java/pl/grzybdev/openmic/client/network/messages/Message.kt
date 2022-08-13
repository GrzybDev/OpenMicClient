package pl.grzybdev.openmic.client.network.messages

enum class Message(val type: String) {
    SYSTEM_HELLO("System_Hello"),
    SYSTEM_GOODBYE("System_Goodbye"),
    SYSTEM_IS_ALIVE("System_IsAlive"),
    AUTH_CLIENT("Auth_Client"),
    AUTH_CODE_VERIFY("Auth_CodeVerify")
}
