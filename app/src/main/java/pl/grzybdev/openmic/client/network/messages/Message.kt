package pl.grzybdev.openmic.client.network.messages

enum class Message(val type: String) {
    SYSTEM_HELLO("System_Hello"),
    SYSTEM_GOODBYE("System_Goodbye"),
    AUTH_CODE_VERIFY("Auth_CodeVerify")
}
