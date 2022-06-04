package pl.grzybdev.openmic.client.dialogs

import android.app.AlertDialog
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import com.gazman.signals.Signals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.WebSocket
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.interfaces.IError
import pl.grzybdev.openmic.client.network.messages.ErrorCode
import pl.grzybdev.openmic.client.network.messages.client.AuthCodeVerify
import pl.grzybdev.openmic.client.network.messages.client.ClientPacket
import pl.grzybdev.openmic.client.network.messages.client.SystemGoodbye
import pl.grzybdev.openmic.client.network.messages.server.SystemPacket

class AuthDialog {

    init {
        val errorSignal = Signals.signal(IError::class)

        errorSignal.addListener { error ->
            run {
                if (error == ErrorCode.AUTH_CODE_INVALID) {
                    DialogData.socket?.let { show(it) }
                }
            }
        }
    }

    object DialogData {
        var socket: WebSocket? = null
    }

    companion object {

        fun show(socket: WebSocket) {
            DialogData.socket = socket

            OpenMic.App.mainActivity?.runOnUiThread {
                // Build AlertDialog
                val builder: AlertDialog.Builder = AlertDialog.Builder(OpenMic.App.mainActivity)
                builder.setTitle(OpenMic.App.mainActivity?.getString(R.string.AuthDialog_Title))

                // Set up the input
                val input = EditText(OpenMic.App.mainActivity)
                input.inputType = InputType.TYPE_CLASS_NUMBER
                builder.setView(input)

                // Set up the buttons
                builder.setPositiveButton(OpenMic.App.mainActivity?.getString(R.string.AuthDialog_Button_OK)) { _, _ ->
                    run {
                        val authCode = Integer.parseInt(input.text.toString())

                        Log.d(
                            SystemPacket::class.java.name,
                            "Entered code: $authCode"
                        )

                        val authPacket: ClientPacket = AuthCodeVerify(authCode)
                        socket.send(Json.encodeToString(authPacket))
                    }
                }

                builder.setNegativeButton(OpenMic.App.mainActivity?.getString(R.string.AuthDialog_Button_Cancel)) { dialog, _ ->
                    run {
                        Log.d(SystemPacket::class.java.name, "Canceled auth dialog! Disconnecting...")

                        val goodbye: ClientPacket = SystemGoodbye(ErrorCode.CANCELED_AUTH_CODE_DIALOG.code)
                        socket.send(Json.encodeToString(goodbye))

                        dialog.cancel()
                    }
                }

                if (DialogShared.current?.isShowing == true)
                    DialogShared.current?.dismiss()

                val dialog = builder.create()

                input.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {

                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {}

                    override fun afterTextChanged(s: Editable?) {
                        // Disable verify button if input is empty
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !TextUtils.isEmpty(s)
                    }

                })

                dialog.show()

                // Disable verify button (initially)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

                // Save reference for dialog
                DialogShared.current = dialog
            }
        }
    }
}