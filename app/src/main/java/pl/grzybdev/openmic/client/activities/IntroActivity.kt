package pl.grzybdev.openmic.client.activities

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import pl.grzybdev.openmic.client.BuildConfig
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.singletons.AppData

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askForPermissions(
                permissions = arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS,
                ),
                slideNumber = 1,
                required = false
            )
        }

        askForPermissions(
            permissions = arrayOf(
                Manifest.permission.RECORD_AUDIO,
            ),
            slideNumber = 2,
            required = true
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            askForPermissions(
                permissions = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                ),
                slideNumber = 3,
                required = false
            )
        } else {
            askForPermissions(
                permissions = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN,
                ),
                slideNumber = 3,
                required = false
            )
        }

        if (BuildConfig.FLAVOR == "foss")
        {
            addSlide(AppIntroFragment.createInstance(
                title = getString(R.string.intro_foss_slide_title),
                description = getString(R.string.intro_foss_slide_description),
                backgroundColorRes = R.color.intro_foss_slide_bg,
                imageDrawable = R.drawable.ic_baseline_attach_money_256
            ))
        }

        isColorTransitionsEnabled = true
        isSkipButtonEnabled = false
        isSystemBackButtonLocked = true
        isWizardMode = true
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        // Decide what to do when the user clicks on "Done"

        with (AppData.sharedPrefs?.edit()) {
            this?.putBoolean(getString(R.string.PREFERENCE_APP_INTRO_SHOWN), true)
            this?.apply()
        }

        finish()
    }
}