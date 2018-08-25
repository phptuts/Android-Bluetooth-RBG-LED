package net.noahglaser.bluetoothrxrgbled

import android.bluetooth.BluetoothDevice
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.recycler_bluetooth_view.view.*


class BleRecyclerAdapter(private val bleDevices: MutableList<BluetoothDevice>, private val connectListener: (BluetoothDevice) -> Unit) :
        RecyclerView.Adapter<BleRecyclerAdapter.ViewHolder>() {

    class ViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout)


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): BleRecyclerAdapter.ViewHolder {
        // create a new view
        val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_bluetooth_view, parent, false) as LinearLayout
        return ViewHolder(layout)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ble = bleDevices[position]
        holder.layout.tv_ble_number.text = "${position + 1})"
        holder.layout.tv_ble_name.text = ble.bleDisplay()
        holder.layout.setOnClickListener { connectListener(ble) }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = bleDevices.size
}

/**
 * Takes a long name a bluetooth device might have and filters it down
 * Example
 * Noah's Awesome bluetooth device is cool -> Noah's Awesome bluetooth ...
 */
fun BluetoothDevice.bleDisplay(): String {

    if (this.name.length > 25) {
        return this.name.substring(0, 25) + "..."
    }

    return this.name
}