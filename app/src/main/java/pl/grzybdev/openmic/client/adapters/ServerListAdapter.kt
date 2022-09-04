package pl.grzybdev.openmic.client.adapters

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.dataclasses.ServerEntry
import pl.grzybdev.openmic.client.enumerators.ServerOS
import pl.grzybdev.openmic.client.enumerators.ServerVersion
import pl.grzybdev.openmic.client.enumerators.network.Connector
import pl.grzybdev.openmic.client.singletons.AppData
import pl.grzybdev.openmic.client.singletons.ServerData

class ServerListAdapter : RecyclerView.Adapter<ServerListAdapter.ViewHolder>() {

    // holder class to hold reference
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var serverOS: TextView = view.findViewById(R.id.serverOS)
        var serverName: TextView = view.findViewById(R.id.serverName)
        var serverAddress: TextView = view.findViewById(R.id.serverAddress)
        var serverVerifyStatus: ImageView = view.findViewById(R.id.serverVerifyStatus)
        var serverConnectBtn: Button = view.findViewById(R.id.serverConnect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_server, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entries = mutableListOf<Map.Entry<String, ServerEntry>>()

        ServerData.foundServers.forEach { entry -> run {
            entries.add(entry)
        }}

        val entryMap = entries[position]
        val entry = entryMap.value

        holder.serverName.text = entry.serverName
        holder.serverAddress.text = entry.serverIP

        when (entry.serverCompat) {
            ServerVersion.MATCH -> run { holder.serverVerifyStatus.setImageDrawable(AppCompatResources.getDrawable(holder.serverConnectBtn.context, R.drawable.ic_baseline_verified_48)) }
            ServerVersion.UNOFFICIAL -> run { holder.serverVerifyStatus.setImageDrawable(AppCompatResources.getDrawable(holder.serverConnectBtn.context, R.drawable.ic_baseline_not_verified_48)) }
            ServerVersion.MISMATCH -> run { holder.serverVerifyStatus.setImageDrawable(AppCompatResources.getDrawable(holder.serverConnectBtn.context, R.drawable.ic_baseline_block_48)) }
            else -> run { holder.serverVerifyStatus.setImageDrawable(AppCompatResources.getDrawable(holder.serverConnectBtn.context, R.drawable.ic_baseline_device_unknown_24)) }
        }

        when (entry.serverOS) {
            ServerOS.UNKNOWN -> run {
                holder.serverOS.text = AppData.resources?.getString(R.string.os_unknown)
                holder.serverOS.setCompoundDrawablesRelativeWithIntrinsicBounds(null, AppCompatResources.getDrawable(holder.serverOS.context, R.drawable.ic_baseline_device_unknown_24), null, null)
            }

            ServerOS.WINDOWS -> run {
                holder.serverOS.text = AppData.resources?.getString(R.string.os_windows)
                holder.serverOS.setCompoundDrawablesRelativeWithIntrinsicBounds(null, AppCompatResources.getDrawable(holder.serverOS.context, R.drawable.ic_os_windows), null, null)
            }

            ServerOS.MACOS -> run {
                holder.serverOS.text = AppData.resources?.getString(R.string.os_macos)
                holder.serverOS.setCompoundDrawablesRelativeWithIntrinsicBounds(null, AppCompatResources.getDrawable(holder.serverOS.context, R.drawable.ic_os_apple), null, null)
            }

            ServerOS.LINUX -> run {
                holder.serverOS.text = AppData.resources?.getString(R.string.os_linux)
                holder.serverOS.setCompoundDrawablesRelativeWithIntrinsicBounds(null, AppCompatResources.getDrawable(holder.serverOS.context, R.drawable.ic_os_linux), null, null)
            }

            ServerOS.LINUX_ARCH -> run {
                holder.serverOS.text = AppData.resources?.getString(R.string.os_linux_arch)
                holder.serverOS.setCompoundDrawablesRelativeWithIntrinsicBounds(null, AppCompatResources.getDrawable(holder.serverOS.context, R.drawable.ic_os_linux_arch), null, null)
            }

            ServerOS.LINUX_DEBIAN -> run {
                holder.serverOS.text = AppData.resources?.getString(R.string.os_linux_debian)
                holder.serverOS.setCompoundDrawablesRelativeWithIntrinsicBounds(null, AppCompatResources.getDrawable(holder.serverOS.context, R.drawable.ic_os_linux_debian), null, null)
            }

            ServerOS.LINUX_FEDORA -> run {
                holder.serverOS.text = AppData.resources?.getString(R.string.os_linux_fedora)
                holder.serverOS.setCompoundDrawablesRelativeWithIntrinsicBounds(null, AppCompatResources.getDrawable(holder.serverOS.context, R.drawable.ic_os_linux_fedora), null, null)
            }

            ServerOS.LINUX_MANJARO -> run {
                holder.serverOS.text = AppData.resources?.getString(R.string.os_linux_manjaro)
                holder.serverOS.setCompoundDrawablesRelativeWithIntrinsicBounds(null, AppCompatResources.getDrawable(holder.serverOS.context, R.drawable.ic_os_linux_manjaro), null, null)
            }

            ServerOS.LINUX_MINT -> run {
                holder.serverOS.text = AppData.resources?.getString(R.string.os_linux_mint)
                holder.serverOS.setCompoundDrawablesRelativeWithIntrinsicBounds(null, AppCompatResources.getDrawable(holder.serverOS.context, R.drawable.ic_os_linux_mint), null, null)
            }

            ServerOS.LINUX_POP_OS -> run {
                holder.serverOS.text = AppData.resources?.getString(R.string.os_linux_pop)
                holder.serverOS.setCompoundDrawablesRelativeWithIntrinsicBounds(null, AppCompatResources.getDrawable(holder.serverOS.context, R.drawable.ic_os_linux_pop), null, null)
            }

            ServerOS.LINUX_UBUNTU -> run {
                holder.serverOS.text = AppData.resources?.getString(R.string.os_linux_ubuntu)
                holder.serverOS.setCompoundDrawablesRelativeWithIntrinsicBounds(null, AppCompatResources.getDrawable(holder.serverOS.context, R.drawable.ic_os_linux_ubuntu), null, null)
            }
        }

        holder.serverConnectBtn.setOnClickListener {
            AppData.openmic.connectTo(holder.serverConnectBtn.context, Connector.WiFi, entry.serverIP)
        }
    }

    override fun getItemCount(): Int {
        return ServerData.foundServers.size
    }

}