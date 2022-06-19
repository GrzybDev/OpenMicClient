package pl.grzybdev.openmic.client.activities

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.gazman.signals.Signals
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.databinding.ActivityMainBinding
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorEvent
import pl.grzybdev.openmic.client.interfaces.IConnector

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var openmic: OpenMic
    private lateinit var wm: WifiManager

    private var connectorSignal = Signals.signal(IConnector::class)

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        OpenMic.App.appPreferences = getSharedPreferences(getString(R.string.PREFERENCE_APP), Context.MODE_PRIVATE)
        OpenMic.App.mainActivity = this

        val initStr = getString(R.string.Status_InitFailed)

        TooltipCompat.setTooltipText(findViewById(R.id.usbStatus), initStr)
        TooltipCompat.setTooltipText(findViewById(R.id.wifiStatus), initStr)
        TooltipCompat.setTooltipText(findViewById(R.id.btStatus), initStr)

        initLogic()
        initButtons()

        wm = (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?)!!
        val lock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "OpenMic:WifiLock")
        lock.acquire()
    }

    private fun initLogic()
    {
        connectorSignal.addListener { connector, event -> updateConnectorStatus(connector, event) }

        openmic = OpenMic(applicationContext)
    }

    private fun initButtons()
    {
        val usbRetry: Button = findViewById(R.id.usbLaunchBtn)
        usbRetry.setOnClickListener {
            run {
                connectorSignal.dispatcher.onEvent(Connector.USB, ConnectorEvent.CONNECTING)
            }
        }

        val wifiLaunch: Button = findViewById(R.id.wifiLaunchBtn)
        wifiLaunch.setOnClickListener {
            run {
                val serverSelect = Intent(this, ServerSelectActivity::class.java)
                startActivity(serverSelect)
            }
        }

        val btLaunch: Button = findViewById(R.id.btLaunchBtn)
        btLaunch.setOnClickListener {
            run {
                val serverSelect = Intent(this, ServerSelectActivity::class.java)
                startActivity(serverSelect)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private fun updateConnectorStatus(connector: Connector, event: ConnectorEvent)
    {
        runOnUiThread {
            val connectorName: TextView = when (connector) {
                Connector.USB -> findViewById(R.id.usbName)
                Connector.WiFi -> findViewById(R.id.wifiName)
                Connector.Bluetooth -> findViewById(R.id.btName)
            }

            val statusView: ImageView = when (connector) {
                Connector.USB -> findViewById(R.id.usbStatus)
                Connector.WiFi -> findViewById(R.id.wifiStatus)
                Connector.Bluetooth -> findViewById(R.id.btStatus)
            }

            val launchBtn: Button = when (connector) {
                Connector.USB -> findViewById(R.id.usbLaunchBtn)
                Connector.WiFi -> findViewById(R.id.wifiLaunchBtn)
                Connector.Bluetooth -> findViewById(R.id.btLaunchBtn)
            }

            val progressBar: ProgressBar = when (connector) {
                Connector.USB -> findViewById(R.id.usbProgress)
                Connector.WiFi -> findViewById(R.id.wifiProgress)
                Connector.Bluetooth -> findViewById(R.id.btProgress)
            }

            val statusText: String = when (connector) {
                Connector.USB -> when (event) {
                    ConnectorEvent.DISABLED -> getString(R.string.Status_USB_Disabled)
                    ConnectorEvent.CONNECTING -> getString(R.string.Status_USB_Connecting)
                    ConnectorEvent.NEED_MANUAL_LAUNCH -> getString(R.string.Status_USB_Need_Launch)
                    ConnectorEvent.CONNECTED_OR_READY -> getString(R.string.Status_USB_Connected)
                }

                Connector.WiFi -> when (event) {
                    ConnectorEvent.DISABLED -> getString(R.string.Status_WiFi_Disabled)
                    ConnectorEvent.CONNECTING -> getString(R.string.Status_WiFi_Connecting)
                    ConnectorEvent.NEED_MANUAL_LAUNCH -> getString(R.string.Status_WiFi_Need_Launch)
                    ConnectorEvent.CONNECTED_OR_READY -> getString(R.string.Status_WiFi_Ready)
                }

                Connector.Bluetooth -> when (event) {
                    ConnectorEvent.DISABLED -> ""
                    ConnectorEvent.CONNECTING -> ""
                    ConnectorEvent.NEED_MANUAL_LAUNCH -> ""
                    ConnectorEvent.CONNECTED_OR_READY -> ""
                }
            }

            when (event) {
                ConnectorEvent.DISABLED -> run {
                    statusView.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_block_48, theme))
                    launchBtn.visibility = View.GONE
                }

                ConnectorEvent.CONNECTING -> run {
                    statusView.visibility = View.GONE
                    launchBtn.visibility = View.GONE
                    progressBar.visibility = View.VISIBLE
                }

                ConnectorEvent.NEED_MANUAL_LAUNCH -> run {
                    statusView.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_warning_48, theme))

                    launchBtn.visibility = View.VISIBLE
                    statusView.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                }

                ConnectorEvent.CONNECTED_OR_READY -> run {
                    statusView.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_check_48, theme))

                    launchBtn.visibility = View.GONE
                    statusView.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                }
            }

            // Change string to shorter one if button is visible, because long ones are... too long :P
            val newName = when (connector) {
                Connector.USB -> if (event == ConnectorEvent.NEED_MANUAL_LAUNCH) getString(R.string.type_USB_Short) else getString(R.string.type_USB)
                Connector.WiFi -> if (event == ConnectorEvent.NEED_MANUAL_LAUNCH) getString(R.string.type_WiFi_Short) else getString(R.string.type_WiFi)
                Connector.Bluetooth -> if (event == ConnectorEvent.NEED_MANUAL_LAUNCH) getString(R.string.type_BT_Short) else getString(R.string.type_BT)
            }

            connectorName.text = newName
            TooltipCompat.setTooltipText(statusView, statusText)
        }
    }
}