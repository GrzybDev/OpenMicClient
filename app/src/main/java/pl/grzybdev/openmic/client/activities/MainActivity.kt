package pl.grzybdev.openmic.client.activities

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import pl.grzybdev.openmic.client.BuildConfig
import pl.grzybdev.openmic.client.GoogleHelper
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.databinding.ActivityMainBinding
import pl.grzybdev.openmic.client.enumerators.ConnectionStatus
import pl.grzybdev.openmic.client.enumerators.DialogType
import pl.grzybdev.openmic.client.interfaces.IConnection
import pl.grzybdev.openmic.client.receivers.signals.ConnectionSignalReceiver
import pl.grzybdev.openmic.client.singletons.AppData
import java.util.*

class MainActivity : AppCompatActivity(), IConnection {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private var connectionSignal: ConnectionSignalReceiver = ConnectionSignalReceiver()
    private var dialogListener = { t: DialogType, d: Any? -> spawnDialog(t, d) }

    private lateinit var dialog: AlertDialog

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

        AppData.resources = resources
        dialog = AlertDialog.Builder(this).create()

        if (savedInstanceState == null) {
            // This ain't our first rodeo ;P
            if (BuildConfig.FLAVOR == "google")
                GoogleHelper.initializeAds(this)

            startIntro()
        }

        connectionSignal.addListener(this)
        registerReceiver(connectionSignal, IntentFilter("UpdateStatus"))

        // AppData.connectionSignal.addListener(connectionListener)
        // AppData.dialogSignal.addListener(dialogListener)
    }

    override fun onDestroy() {
        super.onDestroy()

        connectionSignal.removeListener(this)
        unregisterReceiver(connectionSignal)

        // AppData.dialogSignal.removeListener(dialogListener)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (AppData.connectionStatus >= ConnectionStatus.CONNECTING)
        {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.connecting_fragment_disconnect_action))
                .setMessage(getString(R.string.connecting_fragment_disconnect_action_description))
                .setPositiveButton(R.string.connecting_fragment_disconnect_action_yes) { _, _ ->
                    AppData.openmic.forceDisconnect()
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

    private fun spawnDialog(type: DialogType, data: Any?)
    {
        runOnUiThread {
            if (dialog.isShowing)
                dialog.dismiss()

            val builder: AlertDialog.Builder = AlertDialog.Builder(this)

            when (type) {
                DialogType.SERVER_ERROR -> {
                    builder.setTitle(getString(R.string.dialog_srverr_title))
                    builder.setMessage(data as String)
                    builder.setPositiveButton(getString(R.string.dialog_srverr_btn_ok)) {
                        _, _ ->
                    }
                }

                DialogType.SERVER_DISCONNECT -> {

                }

                DialogType.AUTH -> {
                    builder.setTitle(getString(R.string.dialog_auth_title))

                    // Set up the input
                    val input = EditText(this)
                    input.inputType = InputType.TYPE_CLASS_NUMBER
                    builder.setView(input)

                    // Set up the buttons
                    builder.setPositiveButton(getString(R.string.dialog_auth_btn_ok)) { _, _ ->
                        run {
                            val authCode = Integer.parseInt(input.text.toString())

                            Log.d(javaClass.name, "Entered code: $authCode")
                        }
                    }

                    builder.setNegativeButton(getString(R.string.dialog_auth_btn_cancel)) { _, _ ->
                        run {
                            Log.d(javaClass.name, "Canceled auth dialog! Disconnecting...")
                        }
                    }
                }
            }

            val dialog = builder.create()
            dialog.show()
        }
    }

    override fun onConnectionStateChange(status: ConnectionStatus) {
        runOnUiThread {
            val navController: NavController

            try {
                navController = findNavController(R.id.nav_host_fragment_content_main)
            } catch (NullPointerException: Exception) {
                Log.e(javaClass.name, "NavController is null")
                return@runOnUiThread
            }

            when (status) {
                ConnectionStatus.NOT_CONNECTED -> {}

                ConnectionStatus.CONNECTING -> {
                    // Can only happen from StartFragment
                    navController.navigate(R.id.action_connect)
                }

                ConnectionStatus.CONNECTED -> {
                    // Can only happen from ConnectingFragment
                    navController.navigate(R.id.action_connected)
                }

                ConnectionStatus.DISCONNECTING -> {
                    navController.navigate(R.id.action_disconnect)
                }

                ConnectionStatus.DISCONNECTED -> {
                    navController.navigate(R.id.action_disconnected)
                    OpenMic.changeConnectionStatus(this, ConnectionStatus.NOT_CONNECTED)
                }
            }
        }
    }
}