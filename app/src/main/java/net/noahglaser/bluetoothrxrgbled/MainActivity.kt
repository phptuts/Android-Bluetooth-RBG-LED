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
import android.view.View
import java.util.*


class MainActivity : AbstractMenuActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private  var btAdapter: BluetoothAdapter? = null

    private var blueToothDeviceList = mutableListOf<BluetoothDevice>()

    private var socket: BluetoothSocket? = null

    private var canConnectBluetooth = false

    private val appUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /**
     * Every time a device is discovered onReceive will be called
     * We make sure the device has a name and add it to the recycler view
     */
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

    /**
     * This is called when a bluetooth device's state changes
     * We use this when we need to bond to a device so that we know we can connect to it.
     */
    private val btStateChangeListener = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent) {

            Log.d(TAG, "BLUE TOOTH STATE CHANGED")
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

            if (device.bondState == BluetoothDevice.BOND_BONDED) {
                createConnectedSocket(device)
            }
        }
    }

    /**
     * This is just used to update the status to the user.
     */
    private val btScanCompleteListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            tv_status.text = getString(R.string.MAIN_BT_SCAN_COMPLETE)
        }
    }

    /**
     * This is just used to update the status to the user.
     */
    private val btScanStartedListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            tv_status.text = getString(R.string.MAIN_BT_DISCOVERY)
        }
    }

    /**
     * check if bluetooth is usable
     * Sets up the bluetooth listeners
     * Sets the recycler view
     * Asks for permission from the user
     * Calls a method to start discovery of bluetooth if permission is given
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rv_bluetooth_list.layoutManager = LinearLayoutManager(applicationContext)
        rv_bluetooth_list.addItemDecoration(DividerItemDecoration(rv_bluetooth_list.context, DividerItemDecoration.VERTICAL))
        rv_bluetooth_list.adapter = BleRecyclerAdapter(blueToothDeviceList, {
            tv_status.text = getString(R.string.MAIN_BT_CONNECTING)
            socket?.close()
            btAdapter?.cancelDiscovery()
            if (it.bondState != BluetoothDevice.BOND_BONDED) {
                it.createBond()
                return@BleRecyclerAdapter
            }
            createConnectedSocket(it)
        })

        requestPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, object : PermissionCallBack {
            override fun permissionGranted() {
                super.permissionGranted()
                canConnectBluetooth = true

                // We only want to do this once so we do it right after we get permission
                val actionFoundFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                val actionBondStateChangeFilter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                val actionScanCompleteFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                val actionScanStartFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)

                registerReceiver(btFoundListener, actionFoundFilter)
                registerReceiver(btStateChangeListener, actionBondStateChangeFilter)
                registerReceiver(btScanStartedListener,actionScanStartFilter )
                registerReceiver(btScanCompleteListener,actionScanCompleteFilter )

                refreshBluetoothList()
            }

            override fun permissionDenied() {
                super.permissionDenied()
                canConnectBluetooth = false
                Toast.makeText(applicationContext, getString(R.string.BT_ERROR_NO_PERMISSION), Toast.LENGTH_LONG).show()
            }
        })
    }

    /**
     * Checks for device is ready for bluetooth
     */
    private fun btCheck(): Boolean {

        if (!canConnectBluetooth) {
            Toast.makeText(applicationContext, getString(R.string.BT_ERROR_NO_PERMISSION), Toast.LENGTH_LONG).show()
            return false
        }

        if (btAdapter == null) {
            Toast.makeText(applicationContext, getString(R.string.MAIN_ERROR_BT_AVAILABLE), Toast.LENGTH_LONG).show()
            tv_status.text = getString(R.string.MAIN_BT_NOT_AVAILABLE)
            return false
        }

        if (btAdapter?.isEnabled == false) {
            Toast.makeText(applicationContext, getString(R.string.MAIN_ERROR_BT_NOT_ENABLED), Toast.LENGTH_LONG).show()
            tv_status.text = getString(R.string.MAIN_BT_NOT_ENABLED)
            return false
        }

        return true
    }

    /**
     * Makes sure the bluetooth device can connect
     */
    fun refresh(v: View) {
        if (btCheck()) {
            refreshBluetoothList()
        }
    }

    /**
     * Starts the discovery process for bluetooth and register's listener
     * Clears all the list from the recycler view
     */
    fun refreshBluetoothList() {

        btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!btCheck()) {
            Log.d(TAG, "NOT READY FOR BLUETOOTH")
            return
        }

        btAdapter?.cancelDiscovery()

        blueToothDeviceList.clear()
        rv_bluetooth_list.adapter.notifyDataSetChanged()

        btAdapter?.startDiscovery()
    }

    /**
     * Creates a socket connection for the bluetooth
     */
    private fun createConnectedSocket(btDevice: BluetoothDevice)
    {
        socket = btDevice.createInsecureRfcommSocketToServiceRecord(appUuid)
        val time = measureNanoTime {
            try {
                socket?.connect()
                (application as App).setBTSocket(socket as BluetoothSocket)
                startActivity(Intent(applicationContext, ColorActivity::class.java))
            } catch (e: Exception) {
                tv_status.text = getString(R.string.MAIN_BT_CONNECTION_FAILED)
                Log.d(TAG, "SOCKET ERROR : ${e.message}")
                socket?.close()
                Toast.makeText(applicationContext, getString(R.string.MAIN_BT_ERROR_SOCKET), Toast.LENGTH_LONG).show()
            }
        }
        Log.d(TAG, time.toString())
    }

    /**
     * Unregisters the listeners once the activity goes away to save on memory and what not
     */
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(btFoundListener)
        unregisterReceiver(btStateChangeListener)
    }
}