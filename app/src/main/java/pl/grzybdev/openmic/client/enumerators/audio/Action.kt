package pl.grzybdev.openmic.client.enumerators.audio

enum class Action(val code: Int) {
    START(0),
    TOGGLE_MUTE(1),
    DISCONNECT(2),
    TOGGLE_MUTE_SELF(3),
}
