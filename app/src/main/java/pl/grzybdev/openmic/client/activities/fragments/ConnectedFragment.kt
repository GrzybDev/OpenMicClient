package pl.grzybdev.openmic.client.activities.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.gazman.signals.Signals
import pl.grzybdev.openmic.client.Action
import pl.grzybdev.openmic.client.AppData
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.databinding.FragmentConnectedBinding
import pl.grzybdev.openmic.client.interfaces.IAudio
import pl.grzybdev.openmic.client.network.Audio
import pl.grzybdev.openmic.client.services.AudioService

class ConnectedFragment : Fragment() {

    private var _binding: FragmentConnectedBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val audioSignal = Signals.signal(IAudio::class)

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val text: TextView = view.findViewById(R.id.connectedDesc)
        val string = getString(R.string.connected_desc, AppData.serverName)
        text.text = string

        val muteBtn: Button = view.findViewById(R.id.muteBtn)
        muteBtn.setOnClickListener {
            run {
                val intent = Intent(OpenMic.App.mainActivity, AudioService::class.java)

                if (AppData.isMuted) {
                    intent.putExtra("action", Action.UNMUTE.code)
                } else {
                    intent.putExtra("action", Action.MUTE.code)
                }

                OpenMic.App.mainActivity?.let { it1 -> ContextCompat.startForegroundService(it1, intent) }
            }
        }

        audioSignal.addListener {
            run {
                if (AppData.isMuted) {
                    muteBtn.setText(R.string.connected_btn_unmute)
                } else {
                    muteBtn.setText(R.string.connected_btn_mute)
                }
            }
        }

        val intent = Intent(OpenMic.App.mainActivity, AudioService::class.java)
        intent.putExtra("action", Action.GET_MUTE_STATUS.code)
        OpenMic.App.mainActivity?.let { it1 -> ContextCompat.startForegroundService(it1, intent) }

        val disconnectBtn: Button = view.findViewById(R.id.disconnectBtn)
        disconnectBtn.setOnClickListener {
            run {
                OpenMic.App.context?.disconnectFromServer()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}