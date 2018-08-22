package net.noahglaser.bluetoothrxrgbled

import android.app.Application
import android.bluetooth.BluetoothSocket

class App: Application() {

    var socket: BluetoothSocket? = null


    fun getBTSocket(): BluetoothSocket {
        if (socket == null) {
            throw Exception("No Socket Found")
        }

        return socket as BluetoothSocket
    }

    fun setBTSocket(socketBt: BluetoothSocket) {
        socket = socketBt
    }


}