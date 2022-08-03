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
import com.gazman.signals.Signals
import pl.grzybdev.openmic.client.BuildConfig
import pl.grzybdev.openmic.client.GoogleHelper
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.databinding.FragmentStartBinding
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorEvent
import pl.grzybdev.openmic.client.interfaces.IConnector
import pl.grzybdev.openmic.client.receivers.USBStateReceiver

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class StartFragment : Fragment() {

    private var connectSignal = Signals.signal(IConnector::class)

    private var usbReceiver: USBStateReceiver = USBStateReceiver()

    private var _binding: FragmentStartBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (BuildConfig.FLAVOR == "google")
        {
            GoogleHelper.showAd(view.findViewById(R.id.start_adView_top))
            GoogleHelper.showAd(view.findViewById(R.id.start_adView_bottom))
        }

        if (savedInstanceState == null)
        {
            // Init USB
            activity?.registerReceiver(usbReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            connectSignal.addListener { c, e -> updateConnectorStatus(c, e) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.unregisterReceiver(usbReceiver)
        _binding = null
    }

    private fun updateConnectorStatus(connector: Connector, event: ConnectorEvent) {
        when (connector) {
            Connector.USB -> updateUSBStatus(event)
            else -> {}
        }
    }

    private fun updateUSBStatus(event: ConnectorEvent)
    {
        val button: Button? = view?.findViewById(R.id.usbBtn)
        val progressBar: ProgressBar? = view?.findViewById(R.id.usbProgressBar)
        val statusIcon: ImageView? = view?.findViewById(R.id.usbStatusIcon)
        val status: TextView? = view?.findViewById(R.id.usbStatus)

        if (button == null
            || progressBar == null
            || statusIcon == null
            || status == null) {
            Log.e(javaClass.name, "updateUSBStatus: Required UI elements not found")
            return
        }

        progressBar.visibility = View.GONE
        statusIcon.visibility = View.VISIBLE

        when (event) {
            ConnectorEvent.USB_NOT_CONNECTED -> {
                button.isEnabled = false
                statusIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_block_48, activity?.theme))
                status.text = getString(R.string.start_fragment_status_usb_not_connected)
            }

            ConnectorEvent.USB_CONNECTED -> {
                button.isEnabled = false
                statusIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_warning_48, activity?.theme))
                status.text = getString(R.string.start_fragment_status_usb_connected)
            }

            ConnectorEvent.USB_CHECKING -> {
                button.isEnabled = false

                progressBar.visibility = View.VISIBLE
                statusIcon.visibility = View.GONE
                status.text = getString(R.string.start_fragment_status_usb_checking)
            }

            ConnectorEvent.READY -> {
                button.isEnabled = true
                statusIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_check_48, activity?.theme))
                status.text = getString(R.string.start_fragment_status_usb_ready)
            }

            else -> { }
        }
    }
}