package com.example.trepfp

import android.app.Activity


import android.content.Context

class newLibreriaBle(private val context: Context) {

    private var bluetoothController: BluetoothController = BluetoothController(context)

    init {
        bluetoothController.bindService()
    }

    fun connectToDevice(deviceAddress: String) {
        bluetoothController.connectToDevice(deviceAddress)
    }

    fun sendCommand(command: String) {
        bluetoothController.sendCommand(command)
    }

    fun isConnected(): Boolean {
        return bluetoothController.isConnected()
    }

    fun getWriteResponseLiveData() = bluetoothController.getWriteResponseLiveData()

    fun unbindService() {
        bluetoothController.unbindService()
    }
}
