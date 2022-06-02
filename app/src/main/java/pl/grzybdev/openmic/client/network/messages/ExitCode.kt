package pl.grzybdev.openmic.client.network.messages

enum class ExitCode(val code: Int) {
    NORMAL(0),
    CANCELED_AUTH_CODE_DIALOG(1)
}