package pl.grzybdev.openmic.client.activities.fragments

import android.annotation.SuppressLint
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.interfaces.IRefresh
import pl.grzybdev.openmic.client.receivers.signals.RefreshSignalReceiver
import pl.grzybdev.openmic.client.singletons.AppData
import pl.grzybdev.openmic.client.singletons.ServerData

class WiFiServerSelectFragment : Fragment(), IRefresh {

    private var refreshSignal: RefreshSignalReceiver = RefreshSignalReceiver()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        refreshSignal.addListener(this)
        context?.registerReceiver(refreshSignal, IntentFilter("RefreshUI"))

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_wi_fi_server_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val serverList: RecyclerView = view.findViewById(R.id.availableServers)

        serverList.apply {
            // vertical layout
            layoutManager = LinearLayoutManager(requireContext())

            // set adapter
            adapter = ServerData.srvListAdapter
        }
    }

    override fun onResume() {
        super.onResume()

        AppData.openmic.startWirelessScan(requireActivity())

        ServerData.foundServers.clear()
        ServerData.foundServersTimestamps.clear()

        onRefresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        refreshSignal.removeListener(this)
        activity?.unregisterReceiver(refreshSignal)

        AppData.openmic.stopWirelessScan()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onRefresh() {
        val progressText = view?.findViewById<TextView>(R.id.serverPick_lookingTitle)
        val progressBar = view?.findViewById<ProgressBar>(R.id.serverPick_progressBar)

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

        ServerData.srvListAdapter.notifyDataSetChanged()
    }

}