package pl.grzybdev.openmic.client.adapters

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import pl.grzybdev.openmic.client.AppData
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.dataclasses.ServerEntry
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ServerCompatibility
import pl.grzybdev.openmic.client.enumerators.ServerOS
import kotlin.concurrent.thread

class ServerListAdapter : RecyclerView.Adapter<ServerListAdapter.ViewHolder>() {

    // holder class to hold reference
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        var serverOS: TextView = view.findViewById(R.id.serverOS)
        var serverName: TextView = view.findViewById(R.id.serverName)
        var serverAddress: TextView = view.findViewById(R.id.serverAddress)
        var serverVerifyStatus: ImageView = view.findViewById(R.id.serverVerifyStatus)

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            thread(start = true) {
                OpenMic.App.context?.connectTo(if (serverAddress.text.split(":").size == 6) Connector.Bluetooth else Connector.WiFi, serverAddress.text.toString())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_server, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entries = mutableListOf<Map.Entry<String, ServerEntry>>()

        AppData.foundServers.forEach { entry -> run {
            if (entry.value.connector == AppData.showServers)
                entries.add(entry)
        }}

        val entryMap = entries[position]
        val entry = entryMap.value

        when (entry.serverOS)
        {
            ServerOS.WINDOWS -> run {
                holder.serverOS.text = OpenMic.App.mainActivity?.getString(R.string.OS_Windows)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    holder.serverOS.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                        OpenMic.App.mainActivity?.let { AppCompatResources.getDrawable(it, R.drawable.ic_baseline_computer_24) }, null, null)
                }
            }

            ServerOS.LINUX -> run {
                holder.serverOS.text = OpenMic.App.mainActivity?.getString(R.string.OS_Linux)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    holder.serverOS.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                        OpenMic.App.mainActivity?.let { AppCompatResources.getDrawable(it, R.drawable.ic_baseline_computer_24) }, null, null)
                }
            }

            ServerOS.OTHER -> run {
                holder.serverOS.text = OpenMic.App.mainActivity?.getString(R.string.OS_Unknown)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    holder.serverOS.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                        OpenMic.App.mainActivity?.let { AppCompatResources.getDrawable(it, R.drawable.ic_baseline_device_unknown_24) }, null, null)
                }
            }
        }

        holder.serverName.text = entry.serverName
        holder.serverAddress.text = entry.serverIP

        when (entry.serverCompat) {
            ServerCompatibility.OFFICIAL -> run { holder.serverVerifyStatus.setImageDrawable(OpenMic.App.mainActivity?.let { AppCompatResources.getDrawable(it, R.drawable.ic_baseline_verified_48) }) }
            ServerCompatibility.UNOFFICIAL -> run { holder.serverVerifyStatus.setImageDrawable(OpenMic.App.mainActivity?.let { AppCompatResources.getDrawable(it, R.drawable.ic_baseline_not_verified_48) }) }
            ServerCompatibility.NOT_SUPPORTED -> run { holder.serverVerifyStatus.setImageDrawable(OpenMic.App.mainActivity?.let { AppCompatResources.getDrawable(it, R.drawable.ic_baseline_block_48) }) }
            else -> run { holder.serverVerifyStatus.setImageDrawable(OpenMic.App.mainActivity?.let { AppCompatResources.getDrawable(it, R.drawable.ic_baseline_device_unknown_24) }) }
        }
    }

    override fun getItemCount(): Int {
        val servers = mutableListOf<String>()

        AppData.foundServers.forEach { entry -> run {
            if (entry.value.connector == AppData.showServers)
                servers.add(entry.key)
        }}

        return servers.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData()
    {
        OpenMic.App.mainActivity?.runOnUiThread {
            notifyDataSetChanged()
        }
    }

}