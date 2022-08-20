package pl.grzybdev.openmic.client.network.messages

enum class ErrorCode(val code: Int) {
    NORMAL(0),
    VERSION_MISMATCH(1),
    CANCELED_AUTH_CODE_DIALOG(2),
    AUTH_CODE_INVALID(3),
    SERVER_RESTARTING(4),
    NOT_ACCEPTABLE_CONFIG(5)
}
