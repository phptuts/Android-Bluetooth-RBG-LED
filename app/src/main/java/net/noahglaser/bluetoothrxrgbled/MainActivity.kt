package net.noahglaser.bluetoothrxrgbled

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import io.vrinda.kotlinpermissions.PermissionCallBack
import io.vrinda.kotlinpermissions.PermissionsActivity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.graphics.Color
import android.view.Menu
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import android.view.MenuItem
import android.content.Intent
import android.net.Uri


class MainActivity : PermissionsActivity()  {

    val STATUS_CONNECTTING = "Status: Connecting..."

    val STATUS_DISCONNECTED = "Status: Disconnected"

    val STATUS_CONNECTTED = "Status: Connected"

    var mBluetoothAdapter: BluetoothAdapter? = null

    var socket: BluetoothSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tv_status.text = STATUS_CONNECTTING
        progressBarsEnabled(false)

        requestPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, object : PermissionCallBack {
            override fun permissionGranted() {
                super.permissionGranted()
                if (!setupBluetooth()) {
                    Toast.makeText(applicationContext, "Please make sure your bluetooth is connected and paired.", Toast.LENGTH_LONG).show()
                    return
                }
                updateColor()
            }

            override fun permissionDenied() {
                super.permissionDenied()
                Toast.makeText(applicationContext, "Can't use bluetooth with location permission, ask google?", Toast.LENGTH_LONG).show()
            }
        })

        val listener = object: SeekBar.OnSeekBarChangeListener{
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

    /**
     * This function tries to open a connection between the phone and the bluetooth
     * It returns true if successful
     */
    fun setupBluetooth(): Boolean {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mBluetoothAdapter.let {
            val device = mBluetoothAdapter?.bondedDevices?.first()
            val uuid = device?.uuids?.first()?.uuid
            socket = device?.createRfcommSocketToServiceRecord(uuid)
            return try {
                socket?.connect()
                progressBarsEnabled(true)
                tv_status.text = STATUS_CONNECTTED
                true
            } catch (e: IOException) {
                Log.d("setupBluetooth", e.message)
                progressBarsEnabled(false)
                tv_status.text = STATUS_DISCONNECTED
                false
            }
        }
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
        try {
            socket?.outputStream?.write(message.toByteArray())
            main_layout.setBackgroundColor(Color.parseColor(hexColor))
            tv_hex_color.text = "Hex Color: $hexColor"
        } catch (e: IOException) {
            tv_status.text = STATUS_CONNECTTING
            Toast.makeText(applicationContext, "Bluetooth Disconnected, Trying to re establish connection", Toast.LENGTH_LONG).show()
            setupBluetooth()
        }
    }

    /**
     * In ables and disables the progress bar
     */
    private fun progressBarsEnabled(enabled: Boolean) {
        sb_blue.isEnabled = enabled
        sb_green.isEnabled = enabled
        sb_red.isEnabled = enabled
    }

    /**
     * Event listener for when the Menu Option is clicked
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_refresh -> {
                Toast.makeText(applicationContext, "Refreshing Bluetooth", Toast.LENGTH_SHORT).show()
                if (setupBluetooth()) updateColor()
                true
            }
            R.id.bug_report -> {
                val browserIntent = Intent("android.intent.action.VIEW", Uri.parse("https://github.com/phptuts/Android-Bluetooth-RBG-LED/issues"))
                startActivity(browserIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Boiler plate code that attaches the menu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu to use in the action bar
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_layout, menu)
        return super.onCreateOptionsMenu(menu)
    }
}


fun Int.getHexColor(): String {
    var hexInit = this.toString(16)
    hexInit += if (hexInit.length == 1)   "0" else ""
    return hexInit.toUpperCase()
}