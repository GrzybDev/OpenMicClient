package pl.grzybdev.openmic.client.activities.fragments

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import pl.grzybdev.openmic.client.*
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorStatus
import pl.grzybdev.openmic.client.receivers.USBStateReceiver

class StartFragment : Fragment() {

    private var connectListener = { c: Connector, s: ConnectorStatus -> updateConnectorStatus(c, s) }

    private var usbReceiver: USBStateReceiver = USBStateReceiver()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // USB
        activity?.registerReceiver(usbReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        AppData.connectSignal.addListener(connectListener)

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        activity?.unregisterReceiver(usbReceiver)
        AppData.connectSignal.removeListener(connectListener)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usbBtn: Button = view.findViewById(R.id.usbBtn)

        usbBtn.setOnClickListener {
            AppData.openmic.connectTo(Connector.USB, getString(R.string.INTERNAL_USB_ADDRESS))
            findNavController().navigate(R.id.action_connect)
        }

        if (BuildConfig.FLAVOR == "google")
            GoogleHelper.showAd(view.findViewById(R.id.start_adView))
    }

    private fun updateConnectorStatus(connector: Connector, status: ConnectorStatus) {
        activity?.runOnUiThread {
            when (connector) {
                Connector.USB -> updateUSBStatus(status)
                else -> {}
            }
        }
    }

    private fun updateUSBStatus(status: ConnectorStatus)
    {
        Log.d(javaClass.name, "Setting to: ${AppData.usbStatus}")

        AppData.usbStatus = status

        val button: Button? = view?.findViewById(R.id.usbBtn)
        val progressBar: ProgressBar? = view?.findViewById(R.id.usbProgressBar)
        val statusIcon: ImageView? = view?.findViewById(R.id.usbStatusIcon)
        val statusText: TextView? = view?.findViewById(R.id.usbStatus)

        if (button == null
            || progressBar == null
            || statusIcon == null
            || statusText == null) {
            Log.w(javaClass.name, "updateUSBStatus: Required UI elements not found")
            return
        }

        progressBar.visibility = View.GONE
        statusIcon.visibility = View.VISIBLE

        when (status) {
            ConnectorStatus.USB_NOT_CONNECTED -> {
                button.isEnabled = false
                statusIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_block_48, activity?.theme))
                statusText.text = getString(R.string.start_fragment_status_usb_not_connected)
            }

            ConnectorStatus.USB_CONNECTED -> {
                button.isEnabled = false

                progressBar.visibility = View.VISIBLE
                statusIcon.visibility = View.GONE

                context?.let { AppData.openmic.usbCheck(it) }
            }

            ConnectorStatus.USB_CHECKING -> {
                button.isEnabled = false

                progressBar.visibility = View.VISIBLE
                statusIcon.visibility = View.GONE
                statusText.text = getString(R.string.start_fragment_status_usb_checking)
            }

            ConnectorStatus.USB_CONNECTED_NO_SERVER -> {
                button.isEnabled = false
                statusIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_warning_48, activity?.theme))
                statusText.text = getString(R.string.start_fragment_status_usb_connected_no_server)
            }

            ConnectorStatus.READY -> {
                button.isEnabled = true
                statusIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_check_48, activity?.theme))
                statusText.text = getString(R.string.start_fragment_status_usb_ready)
            }

            else -> { }
        }
    }
}