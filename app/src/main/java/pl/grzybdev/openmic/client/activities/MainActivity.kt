package pl.grzybdev.openmic.client.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.PowerManager
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuCompat
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
import pl.grzybdev.openmic.client.enumerators.DialogType
import pl.grzybdev.openmic.client.enumerators.network.ConnectionStatus
import pl.grzybdev.openmic.client.interfaces.IConnection
import pl.grzybdev.openmic.client.interfaces.IDialog
import pl.grzybdev.openmic.client.network.messages.client.AuthCodeVerify
import pl.grzybdev.openmic.client.receivers.signals.ConnectionSignalReceiver
import pl.grzybdev.openmic.client.receivers.signals.DialogSignalReceiver
import pl.grzybdev.openmic.client.singletons.AppData
import pl.grzybdev.openmic.client.singletons.ServerData
import java.util.*

class MainActivity : AppCompatActivity(), IConnection, IDialog {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private var connectionSignal: ConnectionSignalReceiver = ConnectionSignalReceiver()
    private var dialogSignal: DialogSignalReceiver = DialogSignalReceiver()

    private lateinit var dialog: AlertDialog
    private var dialogSrvActive: Boolean = false

    lateinit var sharedPrefs: SharedPreferences

    private lateinit var wifiLock: WifiManager.WifiLock
    private lateinit var powerLock: PowerManager.WakeLock

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
        AppData.sharedPrefs = sharedPrefs

        val deviceIDKey = getString(R.string.PREFERENCE_APP_DEVICE_ID)

        if (!sharedPrefs.contains(deviceIDKey)) {
            val newID = UUID.randomUUID()
            Log.d(javaClass.name, "Device ID was not set, generated new one: $newID")

            with (sharedPrefs.edit()) {
                putString(deviceIDKey, newID.toString())
                apply()
            }
        }

        AppData.deviceID = sharedPrefs.getString(deviceIDKey, "INVALID").toString()
        AppData.resources = resources

        dialog = AlertDialog.Builder(this).create()

        if (savedInstanceState == null) {
            if (BuildConfig.FLAVOR == "google")
                GoogleHelper.initializeAds(this)

            startIntro()
        }

        connectionSignal.addListener(this)
        registerReceiver(connectionSignal, IntentFilter("UpdateStatus"))

        dialogSignal.addListener(this)
        registerReceiver(connectionSignal, IntentFilter("ShowDialog"))
    }

    override fun onResume() {
        super.onResume()

        val navController = findNavController(R.id.nav_host_fragment_content_main)

        Log.d(javaClass.name, "Restoring fragment: ${AppData.connectionStatus}")

        when (AppData.connectionStatus) {
            ConnectionStatus.NOT_CONNECTED -> navController.navigate(R.id.MainFragment)
            ConnectionStatus.CONNECTING -> navController.navigate(R.id.ConnectingFragment)
            ConnectionStatus.CONNECTED -> navController.navigate(R.id.ConnectedFragment)
            ConnectionStatus.DISCONNECTING -> navController.navigate(R.id.DisconnectingFragment)
            ConnectionStatus.DISCONNECTED -> navController.navigate(R.id.MainFragment)
            ConnectionStatus.SELECTING_SERVER_WIFI -> navController.navigate(R.id.WiFiServerSelect)
        }

        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "OpenMic:WifiLock")

        val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        powerLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "OpenMic:PowerLock")
    }

    override fun onPause() {
        super.onPause()

        if (wifiLock.isHeld)
            wifiLock.release()

        if (powerLock.isHeld)
            powerLock.release()
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            connectionSignal.removeListener(this)
            unregisterReceiver(connectionSignal)
        }
        catch (e: IllegalArgumentException) {
            Log.e(javaClass.name, "Connection signal receiver not registered")
        }

        try {
            dialogSignal.removeListener(this)
            unregisterReceiver(dialogSignal)
        }
        catch (e: IllegalArgumentException) {
            Log.e(javaClass.name, "Dialog signal receiver not registered")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (AppData.connectionStatus == ConnectionStatus.CONNECTING)
        {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.connecting_fragment_disconnect_connecting_action))
                .setMessage(getString(R.string.connecting_fragment_disconnect_connecting_action_description))
                .setPositiveButton(R.string.connecting_fragment_disconnect_connecting_action_yes) { _, _ ->
                    AppData.openmic.forceDisconnect()
                }
                .setNegativeButton(R.string.connecting_fragment_disconnect_connecting_action_no) { _, _ ->
                    // Do nothing
                }
                .show()
        }
        else if (AppData.connectionStatus == ConnectionStatus.SELECTING_SERVER_WIFI)
        {
            OpenMic.changeConnectionStatus(this, ConnectionStatus.NOT_CONNECTED)

            val navController = findNavController(R.id.nav_host_fragment_content_main)
            navController.navigateUp()
        }
        else if (AppData.connectionStatus > ConnectionStatus.CONNECTING)
        {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.connecting_fragment_disconnect_action, ServerData.name))
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
        MenuCompat.setGroupDividerEnabled(menu, true)

        if (BuildConfig.FLAVOR == "foss") {
            val item = menu.findItem(R.id.action_donate)
            item.isVisible = true
        } else {
            val item = menu.findItem(R.id.action_privacy_policy)
            item.isVisible = true
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_donate -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.INTERNAL_DONATE_URL))))
                true
            }
            R.id.action_privacy_policy -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.PRIVACY_POLICY_URL))))
                true
            }
            R.id.action_faq -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.FAQ_URL))))
                true
            }
            R.id.action_tutorial -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.TUTORIAL_URL))))
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

                ConnectionStatus.SELECTING_SERVER_WIFI -> {
                    navController.navigate(R.id.action_select_server_wifi)
                }
            }
        }
    }

    override fun showDialog(type: DialogType, data: String?) {
        runOnUiThread {
            if (dialog.isShowing && type != DialogType.SERVER_ERROR)
                dialog.dismiss()

            val builder: AlertDialog.Builder = AlertDialog.Builder(this)

            when (type) {
                DialogType.SERVER_ERROR -> {
                    builder.setTitle(getString(R.string.dialog_srverr_title))
                    builder.setMessage(data)
                    builder.setPositiveButton(getString(R.string.dialog_srverr_btn_ok)) {
                            _, _ ->
                    }

                    dialogSrvActive = true
                }

                DialogType.SERVER_DISCONNECT -> {
                    builder.setTitle(getString(R.string.dialog_disconnect_title))

                    if (data == "")
                        builder.setMessage(getString(R.string.dialog_disconnect_no_reason))
                    else
                        builder.setMessage(data)

                    builder.setPositiveButton(getString(R.string.dialog_disconnect_btn_ok)) {
                            _, _ ->
                    }
                }

                DialogType.CLIENT_CONFIG_NOT_COMPATIBLE,
                DialogType.CLIENT_CONFIG_INVALID,
                DialogType.SERVER_CONFIG_NOT_COMPATIBLE -> {
                    when (type) {
                        DialogType.CLIENT_CONFIG_NOT_COMPATIBLE -> builder.setTitle(getString(R.string.dialog_client_cfg_error_title))
                        DialogType.CLIENT_CONFIG_INVALID -> builder.setTitle(getString(R.string.dialog_client_cfg_invalid_title))
                        else -> builder.setTitle(getString(R.string.dialog_server_cfg_error_title))
                    }

                    builder.setMessage(getString(R.string.dialog_cfg_error))

                    builder.setPositiveButton(getString(R.string.dialog_cfg_btn_ok)) {
                            _, _ ->
                    }

                    AppData.openmic.forceDisconnect()
                }

                DialogType.CLIENT_INTERNAL_ERROR -> {
                    builder.setTitle(getString(R.string.dialog_internal_error_title))
                    builder.setMessage(getString(R.string.dialog_internal_error_desc))

                    builder.setPositiveButton(getString(R.string.dialog_internal_error_btn_ok)) {
                            _, _ ->
                    }

                    dialogSrvActive = true
                    AppData.openmic.forceDisconnect()
                }

                DialogType.AUTH -> {
                    builder.setTitle(getString(R.string.dialog_auth_title))

                    // Set up the input
                    val input = EditText(this)
                    input.inputType = InputType.TYPE_CLASS_NUMBER
                    input.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) {
                            // Not needed
                        }
                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                            // Not needed
                        }

                        override fun afterTextChanged(s: Editable?) {
                            // Disable verify button if input is empty
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !TextUtils.isEmpty(s)
                        }
                    })

                    builder.setView(input)

                    // Set up the buttons
                    builder.setPositiveButton(getString(R.string.dialog_auth_btn_ok)) { _, _ ->
                        run {
                            val authCode = Integer.parseInt(input.text.toString())
                            Log.d(javaClass.name, "Entered code: $authCode")

                            AppData.openmic.client.sendPacket(AuthCodeVerify(authCode))
                        }
                    }

                    builder.setNegativeButton(getString(R.string.dialog_auth_btn_cancel)) { _, _ ->
                        run {
                            Log.d(javaClass.name, "Canceled auth dialog! Disconnecting...")
                            AppData.openmic.forceDisconnect(getString(R.string.dialog_disconnect_auth_dismissed))
                        }
                    }
                }
            }

            if (type == DialogType.SERVER_ERROR || type == DialogType.CLIENT_INTERNAL_ERROR) {
                val srvErrDialog = builder.create()
                srvErrDialog.setOnDismissListener {
                    dialogSrvActive = false
                }
                srvErrDialog.show()
            }
            else
            {
                // Ignore server disconnects when they're caused by server error
                if (dialogSrvActive && type == DialogType.SERVER_DISCONNECT)
                    return@runOnUiThread

                dialog = builder.create()
                dialog.show()
            }

            if (type == DialogType.AUTH)
            {
                // Disable verify button (initially)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                dialog.setOnCancelListener {
                    run {
                        Log.d(javaClass.name, "Auth dialog was dismissed! Disconnecting...")
                        AppData.openmic.forceDisconnect(getString(R.string.dialog_disconnect_auth_dismissed))
                    }
                }
            }
        }
    }
}
