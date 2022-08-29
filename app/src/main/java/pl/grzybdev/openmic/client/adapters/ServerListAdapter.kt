package pl.grzybdev.openmic.client.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.dataclasses.ServerEntry
import pl.grzybdev.openmic.client.enumerators.network.Connector
import pl.grzybdev.openmic.client.singletons.AppData
import pl.grzybdev.openmic.client.singletons.ServerData
import kotlin.concurrent.thread

class ServerListAdapter : RecyclerView.Adapter<ServerListAdapter.ViewHolder>() {

    lateinit var ctx: Activity

    fun setContext(ctx: Activity)
    {
        this.ctx = ctx
    }

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
                AppData.openmic.connectTo(ctx, if (serverAddress.text.split(":").size == 6) Connector.Bluetooth else Connector.WiFi, serverAddress.text.toString())
            }
        }
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

        /*
        when (entry.serverCompat) {
            ServerVersion.MATCH -> run { holder.serverVerifyStatus.setImageDrawable(AppCompatResources.getDrawable(ctx, R.drawable.ic_baseline_verified_48)) }
            ServerVersion.UNOFFICIAL -> run { holder.serverVerifyStatus.setImageDrawable(AppCompatResources.getDrawable(ctx, R.drawable.ic_baseline_not_verified_48)) }
            ServerVersion.MISMATCH -> run { holder.serverVerifyStatus.setImageDrawable(AppCompatResources.getDrawable(ctx, R.drawable.ic_baseline_block_48)) }
            else -> run { holder.serverVerifyStatus.setImageDrawable(AppCompatResources.getDrawable(ctx, R.drawable.ic_baseline_device_unknown_24)) }
        }
         */
    }

    override fun getItemCount(): Int {
        return ServerData.foundServers.size
    }

}