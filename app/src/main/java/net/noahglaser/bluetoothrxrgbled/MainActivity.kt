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
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : PermissionsActivity()  {

    var canUseBluetooth = false

    var mBluetoothAdapter: BluetoothAdapter? = null

    var socket: BluetoothSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sb_blue.isEnabled = false
        sb_green.isEnabled = false
        sb_red.isEnabled = false


        requestPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, object : PermissionCallBack {
            override fun permissionGranted() {
                super.permissionGranted()
                setupBluetooth()
                Log.d("Call permissions", "Granted")
                if (!canUseBluetooth) {
                    Toast.makeText(applicationContext, "Please make sure your bluetooth is connected and paired.", Toast.LENGTH_LONG).show()
                    return
                }
                writeBluetooth()
            }

            override fun permissionDenied() {
                super.permissionDenied()
                Toast.makeText(applicationContext, "Can't use bluetooth with location permission, ask google?", Toast.LENGTH_LONG).show()
                Log.d("Call permissions", "Denied")
            }
        })

        val listener = object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                writeBluetooth()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        };

        sb_blue.setOnSeekBarChangeListener(listener)
        sb_green.setOnSeekBarChangeListener(listener)
        sb_red.setOnSeekBarChangeListener(listener)

    }

    fun setupBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mBluetoothAdapter.let {
            val device = mBluetoothAdapter?.bondedDevices?.first()
            val uuid = device?.uuids?.first()?.uuid
            socket = device?.createRfcommSocketToServiceRecord(uuid)
            socket?.connect()

            if (socket != null) {
                sb_red.isEnabled = true
                sb_blue.isEnabled = true
                sb_green.isEnabled = true
                canUseBluetooth = true
            }
        }
    }

    fun writeBluetooth() {
        val message = "${sb_red.progress}-${sb_green.progress}-${sb_blue.progress}|"
        if (socket?.isConnected == false) {
            Toast.makeText(applicationContext, "Bluetooth was disconnect, please connect and try again.", Toast.LENGTH_LONG).show()
            return
        }
        var hexColor = "#" + sb_red.progress.getHexColor() +
                        sb_green.progress.getHexColor() +
                        sb_blue.progress.getHexColor()
        socket?.outputStream?.write(message.toByteArray())
        main_layout.setBackgroundColor(Color.parseColor(hexColor))
        tv_hex_color.text = "Hex Color: $hexColor"
    }
}


fun Int.getHexColor(): String {
    var hexInit = this.toString(16)
    hexInit += if (hexInit.length == 1)   "0" else ""
    return hexInit.toUpperCase()
}