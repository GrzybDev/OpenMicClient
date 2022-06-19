package pl.grzybdev.openmic.client.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gazman.signals.Signals
import pl.grzybdev.openmic.client.AppData
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorEvent
import pl.grzybdev.openmic.client.interfaces.IConnector

class ServerSelectActivity : AppCompatActivity() {

    private val connectorSignal = Signals.signal(IConnector::class)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_select)

        connectorSignal.addListener {
            connector, event -> onConnectorEvent(connector, event)
        }

        initServerList()
    }

    private fun initServerList()
    {
        val serverList: RecyclerView = findViewById(R.id.availableServers)

        serverList.apply {
            // vertical layout
            layoutManager = LinearLayoutManager(applicationContext)

            // set adapter
            adapter = AppData.serverAdapter
        }
    }

    private fun onConnectorEvent(connector: Connector, event: ConnectorEvent)
    {
        if (event == ConnectorEvent.CONNECTING || (connector == Connector.WiFi && event == ConnectorEvent.CONNECTED_OR_READY))
            finish()
    }
}