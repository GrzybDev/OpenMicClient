package pl.grzybdev.openmic.client.interfaces

import pl.grzybdev.openmic.client.enumerators.DialogType

interface IDialog {
    fun showDialog(type: DialogType, data: String?)
}
