package net.noahglaser.bluetoothrxrgbled

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.widget.Toast
import io.vrinda.kotlinpermissions.PermissionCallBack
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.system.measureNanoTime
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import java.util.*


class MainActivity : AbstractMenuActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private lateinit var btAdapter: BluetoothAdapter

    private var blueToothDeviceList = mutableListOf<BluetoothDevice>()

    private var socket: BluetoothSocket? = null

    private val appUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val btFoundListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            Log.d(TAG, "FOUND BLUETOOTH")

            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            if (device.name != null && !blueToothDeviceList.any { it.name == device.name }) {
                blueToothDeviceList.add(device)
                rv_bluetooth_list.adapter.notifyDataSetChanged()
                return
            }
        }
    }

    private val btStateChangeListener = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent) {

            Log.d(TAG, "BLUE TOOTH STATE CHANGED")
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

            if (device.bondState == BluetoothDevice.BOND_BONDED) {
                createConnectedSocket(device)
            }
        }
    }

    private val btScanCompleteListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            tv_status.text = "Status: Scanning Complete"
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (application as App).socket?.close()
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        rv_bluetooth_list.layoutManager = LinearLayoutManager(applicationContext)
        rv_bluetooth_list.addItemDecoration(DividerItemDecoration(rv_bluetooth_list.context, DividerItemDecoration.VERTICAL))
        rv_bluetooth_list.adapter = BleRecyclerAdapter(blueToothDeviceList, {
            tv_status.text = "Status: Connecting..."
            socket?.close()
            btAdapter.cancelDiscovery()
            if (it.bondState != BluetoothDevice.BOND_BONDED) {
                it.createBond()
                return@BleRecyclerAdapter
            }
            createConnectedSocket(it)
        })

        requestPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, object : PermissionCallBack {
            override fun permissionGranted() {
                super.permissionGranted()
                refreshBluetoothList()
            }

            override fun permissionDenied() {
                super.permissionDenied()
                Toast.makeText(applicationContext, "Can't use bluetooth with location permission, ask google?", Toast.LENGTH_LONG).show()
            }
        })
    }

    fun createConnectedSocket(btDevice: BluetoothDevice)
    {

        socket = btDevice.createInsecureRfcommSocketToServiceRecord(appUuid)
        val time = measureNanoTime {
            try {
                socket?.connect()
                (application as App).setBTSocket(socket as BluetoothSocket)
                startActivity(Intent(applicationContext, ColorActivity::class.java))
            } catch (e: Exception) {
                tv_status.text = "Status: Connection Failed"
                Log.d(TAG, "SOCKET ERROR : ${e.message}")
                socket?.close()
                Toast.makeText(applicationContext, "Error connecting, please sure everything is paired.", Toast.LENGTH_LONG).show()
            }
        }
        Log.d(TAG, time.toString())
    }


    fun refreshBluetoothList() {

        btAdapter.cancelDiscovery()

        val actionFoundFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        val actionBondStateChangeFilter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        val actionScanComleteFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)

        registerReceiver(btFoundListener, actionFoundFilter)
        registerReceiver(btStateChangeListener, actionBondStateChangeFilter)
        registerReceiver(btScanCompleteListener,actionScanComleteFilter )

        blueToothDeviceList.clear()
        rv_bluetooth_list.adapter.notifyDataSetChanged()

        btAdapter.startDiscovery()
        tv_status.text = "Status: Scanning..."
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(btFoundListener)
        unregisterReceiver(btStateChangeListener)
    }
}