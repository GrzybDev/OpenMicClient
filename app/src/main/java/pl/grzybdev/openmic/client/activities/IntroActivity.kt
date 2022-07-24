package pl.grzybdev.openmic.client.activities

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.R

class IntroActivity : AppIntro() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addSlide(AppIntroFragment.createInstance(
            title = getString(R.string.intro_first_slide_title),
            description = getString(R.string.intro_first_slide_description),
            backgroundColorRes = R.color.intro_first_slide_bg,
            imageDrawable = R.drawable.ic_baseline_mic_256
        ))

        addSlide(AppIntroFragment.createInstance(
            title = getString(R.string.intro_permission_slide_title),
            description = getString(R.string.intro_permission_slide_description),
            backgroundColorRes = R.color.intro_permissions_slide_bg,
            imageDrawable = R.drawable.ic_baseline_perm_camera_mic_256
        ))

        addSlide(AppIntroFragment.createInstance(
            title = getString(R.string.intro_permission_bt_slide_title),
            description = getString(R.string.intro_permission_bt_slide_description),
            backgroundColorRes = R.color.intro_permissions_bt_slide_bg,
            imageDrawable = R.drawable.ic_baseline_bluetooth_audio_256
        ))

        addSlide(AppIntroFragment.createInstance(
            title = getString(R.string.intro_almost_ready_slide_title),
            description = getString(R.string.intro_almost_ready_slide_description),
            backgroundColorRes = R.color.intro_almost_ready_slide_bg,
            imageDrawable = R.drawable.ic_baseline_cast_white_256
        ))

        addSlide(AppIntroFragment.createInstance(
            title = getString(R.string.intro_open_source_slide_title),
            description = getString(R.string.intro_open_source_slide_description),
            backgroundColorRes = R.color.intro_open_source_slide_bg,
            imageDrawable = R.drawable.ic_baseline_code_256
        ))

        askForPermissions(
            permissions = arrayOf(
                Manifest.permission.RECORD_AUDIO,
            ),
            slideNumber = 2,
            required = true
        )

        askForPermissions(
            permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
            ),
            slideNumber = 3,
            required = false
        )

        isColorTransitionsEnabled = true
        isSkipButtonEnabled = false
        isSystemBackButtonLocked = true
        isWizardMode = true
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        // Decide what to do when the user clicks on "Done"

        val sharedPref = getSharedPreferences(getString(R.string.PREFERENCE_APP), Context.MODE_PRIVATE)

        with (sharedPref.edit()) {
            putBoolean(getString(R.string.PREFERENCE_APP_INTRO_SHOWN), true)
            apply()
        }

        finish()
    }
}