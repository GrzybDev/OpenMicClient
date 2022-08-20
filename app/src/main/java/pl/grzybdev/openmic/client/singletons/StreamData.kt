package pl.grzybdev.openmic.client.singletons

import android.content.Intent
import pl.grzybdev.openmic.client.enumerators.audio.Channels
import pl.grzybdev.openmic.client.enumerators.audio.Format

object StreamData {
    var sampleRate: Int = -1

    var channels: Channels = Channels.INVALID
    var channelsInt: Int = -1

    var format: Format = Format.INVALID
    var formatInt: Int = -1

    var bufferSize: Int = -1

    var intent: Intent = Intent()
    var intentActive: Boolean = false
}
