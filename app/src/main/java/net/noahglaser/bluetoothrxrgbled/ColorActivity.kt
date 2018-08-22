package net.noahglaser.bluetoothrxrgbled

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.widget.Toast
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_color.*
import java.io.IOException


class ColorActivity : AbstractMenuActivity() {

    private val TAG = ColorActivity::class.java.simpleName

    lateinit var socket: BluetoothSocket

    private val disconnectedListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (sb_blue.isEnabled) {
                disableApp()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color)
        registerReceiver(disconnectedListener, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED) )
        socket = (application as App).getBTSocket()
        updateColor()
        progressBarsEnabled(true)
        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateColor()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        }

        sb_blue.setOnSeekBarChangeListener(listener)
        sb_green.setOnSeekBarChangeListener(listener)
        sb_red.setOnSeekBarChangeListener(listener)
    }

    fun reconnect(v: View) {
        startActivity(Intent(applicationContext, MainActivity::class.java))
    }


    /**
     * Sends a color message to the bluetooth device
     * Update the background color and the color text
     * If it can't send a message it tries to set up a connection again
     */
    fun updateColor() {

        val message = "${sb_red.progress}-${sb_green.progress}-${sb_blue.progress}|"
        val hexColor = "#" + sb_red.progress.getHexColor() +
                sb_green.progress.getHexColor() +
                sb_blue.progress.getHexColor()

        main_layout.setBackgroundColor(Color.parseColor(hexColor))
        tv_hex_color.text = "Hex Color: $hexColor"

        try {
            socket.outputStream.write(message.toByteArray())
        } catch (e: IOException) {
            Log.e(TAG, e.message)
        }
    }

    fun disableApp() {
        Toast.makeText(applicationContext, "Bluetooth Disconnected, Trying to re establish connection", Toast.LENGTH_LONG).show()
        progressBarsEnabled(false)
    }

    override fun onBackPressed() {
        socket.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.close()
    }

    /**
     * In ables and disables the progress bar
     */
    private fun progressBarsEnabled(enabled: Boolean) {
        sb_blue.isEnabled = enabled
        sb_green.isEnabled = enabled
        sb_red.isEnabled = enabled
    }


}


fun Int.getHexColor(): String {
    var hexInit = this.toString(16)
    hexInit += if (hexInit.length == 1) "0" else ""
    return hexInit.toUpperCase()
}