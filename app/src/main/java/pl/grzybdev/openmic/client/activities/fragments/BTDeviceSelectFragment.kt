package pl.grzybdev.openmic.client.activities.fragments

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.interfaces.IRefresh
import pl.grzybdev.openmic.client.receivers.connectors.BTStateReceiver
import pl.grzybdev.openmic.client.receivers.signals.RefreshSignalReceiver
import pl.grzybdev.openmic.client.singletons.AppData
import pl.grzybdev.openmic.client.singletons.ServerData

class BTDeviceSelectFragment : Fragment(), IRefresh {

    private var refreshSignal: RefreshSignalReceiver = RefreshSignalReceiver()
    private val btReceiver: BTStateReceiver = BTStateReceiver()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        refreshSignal.addListener(this)

        context?.registerReceiver(refreshSignal, IntentFilter("RefreshUI"))
        context?.registerReceiver(btReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bt_device_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val serverList: RecyclerView = view.findViewById(R.id.availableDevices)

        serverList.apply {
            // vertical layout
            layoutManager = LinearLayoutManager(requireContext())

            // set adapter
            adapter = ServerData.devListAdapter
        }
    }

    override fun onResume() {
        super.onResume()

        AppData.openmic.startBluetoothScan(requireActivity())

        ServerData.foundServers.clear()
        onRefresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        refreshSignal.removeListener(this)

        activity?.unregisterReceiver(refreshSignal)
        activity?.unregisterReceiver(btReceiver)

        AppData.openmic.stopBluetoothScan(requireActivity())
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onRefresh() {
        val progressText = view?.findViewById<TextView>(R.id.devicePick_lookingTitle)
        val progressBar = view?.findViewById<ProgressBar>(R.id.devicePick_progressBar)

        if (progressText == null || progressBar == null) {
            Log.e(javaClass.name, "onRefresh: null views")
            return
        }

        if (ServerData.foundServers.isNotEmpty()) {
            progressText.visibility = View.GONE
            progressBar.visibility = View.GONE
        } else {
            progressText.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
        }

        ServerData.devListAdapter.notifyDataSetChanged()
    }

}