package pl.grzybdev.openmic.client

import android.content.SharedPreferences
import pl.grzybdev.openmic.client.activities.MainActivity

object AppData {

    var appPreferences: SharedPreferences? = null
    var mainActivity: MainActivity? = null

    var deviceID: String = ""

}
