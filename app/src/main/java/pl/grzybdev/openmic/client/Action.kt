package pl.grzybdev.openmic.client

enum class Action(val code: Int) {
    MUTE(0),
    UNMUTE(1),
    GET_MUTE_STATUS(2)
}