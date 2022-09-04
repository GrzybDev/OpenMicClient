package pl.grzybdev.openmic.client.activities.fragments

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import pl.grzybdev.openmic.client.BuildConfig
import pl.grzybdev.openmic.client.GoogleHelper
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.enumerators.network.ConnectionStatus
import pl.grzybdev.openmic.client.enumerators.network.Connector
import pl.grzybdev.openmic.client.enumerators.network.ConnectorState
import pl.grzybdev.openmic.client.interfaces.IConnector
import pl.grzybdev.openmic.client.receivers.connectors.BTStateReceiver
import pl.grzybdev.openmic.client.receivers.connectors.USBStateReceiver
import pl.grzybdev.openmic.client.receivers.connectors.WifiStateReceiver
import pl.grzybdev.openmic.client.receivers.signals.ConnectorSignalReceiver
import pl.grzybdev.openmic.client.singletons.AppData

class StartFragment : Fragment(), IConnector {

    private var connectorSignal: ConnectorSignalReceiver = ConnectorSignalReceiver()

    private val usbReceiver: USBStateReceiver = USBStateReceiver()
    private val wifiReceiver: WifiStateReceiver = WifiStateReceiver()
    private val btReceiver: BTStateReceiver = BTStateReceiver()

    private var usbStateLock: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        connectorSignal.addListener(this)
        context?.registerReceiver(connectorSignal, IntentFilter("UpdateState"))

        // USB
        activity?.registerReceiver(usbReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Wi-Fi
        @Suppress("DEPRECATION")
        activity?.registerReceiver(wifiReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        // Bluetooth
        activity?.registerReceiver(btReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        activity?.unregisterReceiver(usbReceiver)
        activity?.unregisterReceiver(wifiReceiver)
        activity?.unregisterReceiver(btReceiver)

        connectorSignal.removeListener(this)
        activity?.unregisterReceiver(connectorSignal)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usbBtn: Button = view.findViewById(R.id.usbBtn)

        usbBtn.setOnClickListener {
            AppData.openmic.connectTo(requireContext(), Connector.USB, getString(R.string.INTERNAL_USB_ADDRESS))
        }

        val wifiBtn: Button = view.findViewById(R.id.wifiBtn)

        wifiBtn.setOnClickListener {
            OpenMic.changeConnectionStatus(requireContext(), ConnectionStatus.SELECTING_SERVER_WIFI)
        }

        val btBtn: Button = view.findViewById(R.id.btBtn)

        btBtn.setOnClickListener {
            OpenMic.changeConnectionStatus(requireContext(), ConnectionStatus.SELECTING_DEVICE_BT)
        }

        if (BuildConfig.FLAVOR == "google")
            GoogleHelper.showAd(view.findViewById(R.id.start_adView))

        onUSBStateChange(AppData.usbState)

        if (OpenMic.isBluetoothEnabled(requireContext()))
            onBluetoothStateChange(ConnectorState.READY)
        else
            onBluetoothStateChange(ConnectorState.NOT_READY)
    }

    override fun onUSBStateChange(status: ConnectorState) {
        val button: Button? = view?.findViewById(R.id.usbBtn)
        val progressBar: ProgressBar? = view?.findViewById(R.id.usbProgressBar)
        val statusIcon: ImageView? = view?.findViewById(R.id.usbStatusIcon)
        val statusText: TextView? = view?.findViewById(R.id.usbStatus)

        if (button == null
            || progressBar == null
            || statusIcon == null
            || statusText == null) {
            Log.w(javaClass.name, "onUSBStateChange: Required UI elements not found")
            return
        }

        progressBar.visibility = View.GONE
        statusIcon.visibility = View.VISIBLE

        when (status) {
            ConnectorState.NOT_READY -> {
                button.isEnabled = false
                statusIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_block_48, activity?.theme))
                statusText.text = getString(R.string.start_fragment_status_usb_not_connected)
            }

            ConnectorState.USB_CONNECTED -> {
                if (usbStateLock)
                    return

                button.isEnabled = false

                progressBar.visibility = View.VISIBLE
                statusIcon.visibility = View.GONE
                statusText.text = ""

                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED)
                    AppData.openmic.usbCheck(requireContext(), true)
                else
                    OpenMic.changeConnectorStatus(requireContext(), Connector.USB, ConnectorState.NO_PERMISSION)
            }

            ConnectorState.USB_CHECKING -> {
                button.isEnabled = false

                progressBar.visibility = View.VISIBLE
                statusIcon.visibility = View.GONE
                statusText.text = getString(R.string.start_fragment_status_usb_checking)
            }

            ConnectorState.USB_CONNECTED_NO_SERVER -> {
                button.isEnabled = false
                statusIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_warning_48, activity?.theme))
                statusText.text = getString(R.string.start_fragment_status_usb_connected_no_server)
            }

            ConnectorState.NO_PERMISSION -> {
                button.isEnabled = false
                statusIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_warning_48, activity?.theme))
                statusText.text = getString(R.string.start_fragment_status_usb_no_permissions)

                usbStateLock = true
            }

            ConnectorState.READY -> {
                button.isEnabled = true
                statusIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_check_48, activity?.theme))
                statusText.text = getString(R.string.start_fragment_status_usb_ready)
            }

            else -> { }
        }
    }

    override fun onWiFiStateChange(status: ConnectorState) {
        val button: Button? = view?.findViewById(R.id.wifiBtn)
        val progressBar: ProgressBar? = view?.findViewById(R.id.wifiProgressBar)
        val statusIcon: ImageView? = view?.findViewById(R.id.wifiStatusIcon)
        val statusText: TextView? = view?.findViewById(R.id.wifiStatus)

        if (button == null
            || progressBar == null
            || statusIcon == null
            || statusText == null) {
            Log.w(javaClass.name, "onWiFiStateChange: Required UI elements not found")
            return
        }

        progressBar.visibility = View.GONE
        statusIcon.visibility = View.VISIBLE

        when (status) {
            ConnectorState.NOT_READY -> {
                button.isEnabled = false
                statusIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_block_48, activity?.theme))
                statusText.text = getString(R.string.start_fragment_status_wifi_not_ready)
            }

            ConnectorState.NO_PERMISSION -> {
                button.isEnabled = false
                statusIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_warning_48, activity?.theme))
                statusText.text = getString(R.string.start_fragment_status_wifi_no_permissions)
            }

            ConnectorState.READY -> {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
                {
                    OpenMic.changeConnectorStatus(requireContext(), Connector.WiFi, ConnectorState.NO_PERMISSION)
                    return
                }

                button.isEnabled = true
                statusIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_check_48, activity?.theme))
                statusText.text = getString(R.string.start_fragment_status_wifi_ready)
            }

            else -> {}
        }
    }

    override fun onBluetoothStateChange(status: ConnectorState) {
        val button: Button? = view?.findViewById(R.id.btBtn)
        val progressBar: ProgressBar? = view?.findViewById(R.id.btProgressBar)
        val statusIcon: ImageView? = view?.findViewById(R.id.btStatusIcon)
        val statusText: TextView? = view?.findViewById(R.id.btStatus)

        if (button == null
            || progressBar == null
            || statusIcon == null
            || statusText == null) {
            Log.w(javaClass.name, "onBluetoothStateChange: Required UI elements not found")
            return
        }

        progressBar.visibility = View.GONE
        statusIcon.visibility = View.VISIBLE

        when (status) {
            ConnectorState.NOT_READY -> {
                button.isEnabled = false
                statusIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_block_48, activity?.theme))
                statusText.text = getString(R.string.start_fragment_status_bt_not_ready)
            }

            ConnectorState.NO_PERMISSION -> {
                button.isEnabled = false
                statusIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_warning_48, activity?.theme))
                statusText.text = getString(R.string.start_fragment_status_bt_no_permissions)
            }

            ConnectorState.READY -> {
                if (!OpenMic.haveBluetoothPermissions(requireContext()))
                {
                    OpenMic.changeConnectorStatus(requireContext(), Connector.Bluetooth, ConnectorState.NO_PERMISSION)
                    return
                }

                button.isEnabled = true
                statusIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_check_48, activity?.theme))
                statusText.text = getString(R.string.start_fragment_status_bt_ready)
            }

            else -> {}
        }
    }
}