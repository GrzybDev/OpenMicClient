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
import pl.grzybdev.openmic.client.BuildConfig
import pl.grzybdev.openmic.client.GoogleHelper
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.enumerators.network.Connector
import pl.grzybdev.openmic.client.enumerators.network.ConnectorState
import pl.grzybdev.openmic.client.interfaces.IConnector
import pl.grzybdev.openmic.client.receivers.connectors.USBStateReceiver
import pl.grzybdev.openmic.client.receivers.signals.ConnectorSignalReceiver
import pl.grzybdev.openmic.client.singletons.AppData

class StartFragment : Fragment(), IConnector {

    private var connectorSignal: ConnectorSignalReceiver = ConnectorSignalReceiver()
    private var usbReceiver: USBStateReceiver = USBStateReceiver()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        connectorSignal.addListener(this)
        context?.registerReceiver(connectorSignal, IntentFilter("UpdateState"))

        // USB
        activity?.registerReceiver(usbReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        activity?.unregisterReceiver(usbReceiver)

        connectorSignal.removeListener(this)
        activity?.unregisterReceiver(connectorSignal)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usbBtn: Button = view.findViewById(R.id.usbBtn)

        usbBtn.setOnClickListener {
            AppData.openmic.connectTo(requireContext(), Connector.USB, getString(R.string.INTERNAL_USB_ADDRESS))
        }

        if (BuildConfig.FLAVOR == "google")
            GoogleHelper.showAd(view.findViewById(R.id.start_adView))

        onUSBStateChange(AppData.usbState)
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
            Log.w(javaClass.name, "updateUSBStatus: Required UI elements not found")
            return
        }

        progressBar.visibility = View.GONE
        statusIcon.visibility = View.VISIBLE

        when (status) {
            ConnectorState.USB_NOT_CONNECTED -> {
                button.isEnabled = false
                statusIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_block_48, activity?.theme))
                statusText.text = getString(R.string.start_fragment_status_usb_not_connected)
            }

            ConnectorState.USB_CONNECTED -> {
                button.isEnabled = false

                progressBar.visibility = View.VISIBLE
                statusIcon.visibility = View.GONE
                statusText.text = ""

                AppData.openmic.usbCheck(requireContext(), true)
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

            ConnectorState.READY -> {
                button.isEnabled = true
                statusIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_check_48, activity?.theme))
                statusText.text = getString(R.string.start_fragment_status_usb_ready)
            }

            else -> { }
        }
    }
}