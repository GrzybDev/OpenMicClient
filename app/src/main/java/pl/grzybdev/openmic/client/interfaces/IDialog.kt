package pl.grzybdev.openmic.client.interfaces

import pl.grzybdev.openmic.client.enumerators.DialogType

fun interface IDialog {
    fun onEvent(type: DialogType, data: Any?)
}
