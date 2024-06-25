package com.example.trepfp.ui

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.app.Service
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import java.util.UUID


class BluetoothLeServiceVersion(context : Context) : Service() {

     var bluetoothGatt: BluetoothGatt? = null
    private val writeResponseLiveData = MutableLiveData<ByteArray>()
    private var bluetoothManager: BluetoothManager? = null

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED
                broadcastUpdate(intentAction)
                Log.i(TAG, "Connected to GATT server.")
                bluetoothGatt?.discoverServices()
                Log.i(TAG, "Attempting to start service discovery: " + bluetoothGatt?.discoverServices())
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED
                Log.i(TAG, "Disconnected from GATT server.")
                broadcastUpdate(intentAction)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                writeResponseLiveData.postValue(characteristic.value)
                val hexString = characteristicToHexString(characteristic)
                Log.d("onCharacteristicWrite", "Write successful: $hexString")
            } else {
                Log.d("onCharacteristicWrite", "Write failed with status: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val hexString = characteristicToHexString(characteristic)
            Log.d("onCharacteristicChanged", "characteristic.value $hexString")

            addToListLogger(hexString)
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    init {
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    fun isConnected(): Boolean {
        bluetoothGatt?.let { gatt ->
            val devices = bluetoothManager?.getConnectedDevices(BluetoothProfile.GATT)
            return devices?.contains(gatt.device) ?: false
        }
        return false
    }
    fun sendComando(command: String) {
        bluetoothGatt?.let { gatt ->
            val service = gatt.getService(UUID_TREFPB_SERVICE)
            val characteristic = service?.getCharacteristic(UUID_TREFPB_RW)

            characteristic?.value = hexStringToByteArray(command) // Convierte el comando a formato adecuado

            gatt.writeCharacteristic(characteristic)
        }
    }

    private fun hexStringToByteArray(hexString: String): ByteArray {
        val length = hexString.length
        val data = ByteArray(length / 2)
        var i = 0
        while (i < length) {
            data[i / 2] = ((Character.digit(hexString[i], 16) shl 4) +
                    Character.digit(hexString[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun connect(device: BluetoothDevice): Boolean {
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
        return true
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, data: ByteArray) {
        bluetoothGatt?.let {
            characteristic.value = data
            it.writeCharacteristic(characteristic)
        }
    }

    fun getWriteResponseLiveData(): LiveData<ByteArray> {
        return writeResponseLiveData
    }

    private fun characteristicToHexString(characteristic: BluetoothGattCharacteristic): String {
        return characteristic.value.joinToString(separator = " ") { byte -> String.format("%02X", byte) }
    }

    private fun addToListLogger(hexString: String) {
        Log.d("ListLogger", hexString)
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)
        intent.putExtra(EXTRA_DATA, characteristic.value)
        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothLeServiceVersion = this@BluetoothLeServiceVersion
    }

    companion object {
        const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
        val UUID_TREFPB_RW: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        val UUID_TREFPB_SERVICE: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        private val BLUETOOTH_LE_CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val TAG = "BluetoothLeServiceVersion"
    }
}
