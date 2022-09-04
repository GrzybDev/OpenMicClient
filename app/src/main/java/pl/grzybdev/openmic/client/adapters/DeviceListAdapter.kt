package pl.grzybdev.openmic.client.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.dataclasses.ServerEntry
import pl.grzybdev.openmic.client.enumerators.network.Connector
import pl.grzybdev.openmic.client.singletons.AppData
import pl.grzybdev.openmic.client.singletons.ServerData

class DeviceListAdapter : RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {

    // holder class to hold reference
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var deviceName: TextView = view.findViewById(R.id.deviceName)
        var deviceAddress: TextView = view.findViewById(R.id.deviceAddress)
        var deviceBtn: Button = view.findViewById(R.id.deviceConnect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entries = mutableListOf<Map.Entry<String, ServerEntry>>()

        ServerData.foundServers.forEach { entry -> run {
            entries.add(entry)
        }}

        val entryMap = entries[position]
        val entry = entryMap.value

        holder.deviceName.text = entry.serverName
        holder.deviceAddress.text = entry.serverIP
        holder.deviceBtn.setOnClickListener {
            AppData.openmic.connectTo(holder.deviceBtn.context, Connector.Bluetooth, entry.serverIP)
        }
    }

    override fun getItemCount(): Int {
        return ServerData.foundServers.size
    }

}