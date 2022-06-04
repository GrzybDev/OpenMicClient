package pl.grzybdev.openmic.client.interfaces

import pl.grzybdev.openmic.client.network.messages.ErrorCode

fun interface IError {
    fun onErrorMessage(code: ErrorCode)
}
