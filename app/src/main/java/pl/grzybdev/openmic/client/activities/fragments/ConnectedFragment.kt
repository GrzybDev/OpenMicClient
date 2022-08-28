package pl.grzybdev.openmic.client.activities.fragments

import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.interfaces.IRefresh
import pl.grzybdev.openmic.client.network.messages.client.StreamVolume
import pl.grzybdev.openmic.client.receivers.signals.RefreshSignalReceiver
import pl.grzybdev.openmic.client.singletons.AppData
import pl.grzybdev.openmic.client.singletons.StreamData

class ConnectedFragment : Fragment(), IRefresh {

    private var refreshSignal: RefreshSignalReceiver = RefreshSignalReceiver()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        refreshSignal.addListener(this)
        context?.registerReceiver(refreshSignal, IntentFilter("RefreshUI"))

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_connected, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        refreshSignal.removeListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val disconnectBtn = view.findViewById<Button>(R.id.disconnectBtn)
        val toggleMuteBtn = view.findViewById<Button>(R.id.toggleMuteBtn)
        val volumeSlider = view.findViewById<SeekBar>(R.id.streamVolume)

        disconnectBtn.setOnClickListener {
            // Simulate back press to show confirmation dialog
            activity?.onBackPressed()
        }

        toggleMuteBtn.setOnClickListener {
            AppData.audio.toggleMute(requireContext())
        }

        volumeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    AppData.openmic.client.sendPacket(StreamVolume(progress))
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        if (savedInstanceState == null)
        {
            val defaultVol = AppData.sharedPrefs?.getFloat(getString(R.string.PREFERENCE_APP_AUDIO_VOLUME), 1f)

            if (defaultVol != null) {
                volumeSlider.progress = (defaultVol * 100).toInt()
                AppData.openmic.client.sendPacket(StreamVolume(volumeSlider.progress))
            }
        }

        onRefresh()
    }

    override fun onRefresh() {
        val toggleMuteBtn = view?.findViewById<Button>(R.id.toggleMuteBtn)
        val volumeSlider = view?.findViewById<SeekBar>(R.id.streamVolume)

        if (StreamData.muted) {
            toggleMuteBtn?.text = getString(R.string.connected_fragment_btn_unmute)
            toggleMuteBtn?.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_volume_up_24, context?.theme), null, null, null)
        } else {
            toggleMuteBtn?.text = getString(R.string.connected_fragment_btn_mute)
            toggleMuteBtn?.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_volume_off_24, context?.theme), null, null, null)
        }

        volumeSlider?.progress = (StreamData.volume * 100).toInt()
    }
}
