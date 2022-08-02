package pl.grzybdev.openmic.client.activities

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import pl.grzybdev.openmic.client.BuildConfig
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        sharedPrefs = getSharedPreferences(getString(R.string.PREFERENCE_APP), MODE_PRIVATE)

        if (savedInstanceState == null) {
            // This ain't our first rodeo ;P
            if (BuildConfig.FLAVOR == "google")
                MobileAds.initialize(this)

            startIntro()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startIntro() {
        if (!sharedPrefs.getBoolean(getString(R.string.PREFERENCE_APP_INTRO_SHOWN), false)) {
            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)
        } else {
            val requestPermissionLauncher =
                registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (!isGranted) {
                        val intent = Intent(this, IntroActivity::class.java)
                        startActivity(intent)
                    }
                }
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) -> {}
                else -> {
                    // You can directly ask for the permission.
                    // The registered ActivityResultCallback gets the result of this request.
                    requestPermissionLauncher.launch(
                        Manifest.permission.RECORD_AUDIO
                    )
                }
            }

            if (BuildConfig.FLAVOR == "google")
            {
                val adRequest = AdRequest.Builder().build()
                val loadCallback = object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        super.onAdLoaded(ad)
                        ad.show(this@MainActivity)
                    }

                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        super.onAdFailedToLoad(p0)
                        Log.d(this.javaClass.name, "onAppOpenAdFailedToLoad: ")
                    }
                }

                val orientation = resources.configuration.orientation

                AppOpenAd.load(
                    this,
                    getString(R.string.AD_UNIT_ID_BOOT),
                    adRequest,
                    when (orientation) {
                        Configuration.ORIENTATION_LANDSCAPE -> AppOpenAd.APP_OPEN_AD_ORIENTATION_LANDSCAPE
                        else -> AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT
                    },
                    loadCallback
                )
            }
        }
    }
}