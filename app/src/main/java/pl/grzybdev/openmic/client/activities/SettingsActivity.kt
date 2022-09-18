package pl.grzybdev.openmic.client.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.singletons.AppData

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val port: EditTextPreference? = findPreference(getString(R.string.PREFERENCE_APP_PORT))
            port?.setOnBindEditTextListener { editText ->
                editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
            
            port?.setOnPreferenceChangeListener { _, newValue ->
                val value = newValue.toString().toInt()

                if (value in 1..65535) {
                    AppData.communicationPort = value
                    true
                } else {
                    false
                }
            }

            val sampleRate: EditTextPreference? = findPreference(getString(R.string.PREFERENCE_APP_AUDIO_SAMPLE_RATE))
            sampleRate?.setOnBindEditTextListener { editText ->
                editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
        }
    }
}