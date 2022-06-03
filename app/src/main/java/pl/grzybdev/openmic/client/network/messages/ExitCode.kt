package pl.grzybdev.openmic.client.network.messages

enum class ExitCode(val code: Int) {
    NORMAL(0),
    VERSION_MISMATCH(1),
    CANCELED_AUTH_CODE_DIALOG(2),
    AUTH_CODE_INVALID(3)
}