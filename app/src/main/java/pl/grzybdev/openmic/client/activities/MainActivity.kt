package pl.grzybdev.openmic.client.activities

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import pl.grzybdev.openmic.client.AppData
import pl.grzybdev.openmic.client.BuildConfig
import pl.grzybdev.openmic.client.GoogleHelper
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.databinding.ActivityMainBinding
import pl.grzybdev.openmic.client.enumerators.ConnectionStatus
import pl.grzybdev.openmic.client.enumerators.ConnectorStatus
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    lateinit var sharedPrefs: SharedPreferences

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

        val deviceIDKey = getString(R.string.PREFERENCE_APP_DEVICE_ID)

        if (!sharedPrefs.contains(deviceIDKey)) {
            val newID = UUID.randomUUID()
            Log.d(javaClass.name, "Device ID was not set, generated new one: $newID")

            with (sharedPrefs.edit()) {
                putString(deviceIDKey, newID.toString())
                apply()
            }
        }


        if (savedInstanceState == null) {
            // This ain't our first rodeo ;P
            if (BuildConfig.FLAVOR == "google")
                GoogleHelper.initializeAds(this)

            startIntro()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        if (AppData.connectionStatus >= ConnectionStatus.CONNECTING)
        {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.connecting_fragment_disconnect_action))
                .setMessage(getString(R.string.connecting_fragment_disconnect_action_description))
                .setPositiveButton(R.string.connecting_fragment_disconnect_action_yes) { _, _ ->
                    AppData.openmic.forceDisconnect(navController)
                }
                .setNegativeButton(R.string.connecting_fragment_disconnect_action_no) { _, _ ->
                    // Do nothing
                }
                .show()
        }

        return false
    }

    override fun onBackPressed() {
        onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        if (BuildConfig.FLAVOR == "foss") {
            val item = menu.findItem(R.id.action_donate)
            item.isVisible = true
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            R.id.action_donate -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.INTERNAL_DONATE_URL))))
                true
            }
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
                GoogleHelper.showStartupAd(this, this)
        }
    }
}