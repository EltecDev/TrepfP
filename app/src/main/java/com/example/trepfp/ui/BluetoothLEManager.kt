package com.example.trepfp.ui

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import java.util.*

class BluetoothLEManager(private val context: Context) : Service() {

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private val binder = LocalBinder()

    val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
    val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
    val ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
    val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
    val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
    val UUID_TREFPB_RW: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    val UUID_TREFPB_SERVICE: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val BLUETOOTH_LE_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothLEManager = this@BluetoothLEManager
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        initialize()
    }

    private fun initialize() {
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        bluetoothAdapter = bluetoothManager?.adapter
    }

    fun connectToDevice(deviceAddress: String) {
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        bluetoothGatt = device?.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    fun sendCommand(data: ByteArray) {
        bluetoothGatt?.let { gatt ->
            val service = gatt.getService(UUID_TREFPB_SERVICE)
            val characteristic = service?.getCharacteristic(UUID_TREFPB_RW)
            characteristic?.value = data
            characteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            gatt.writeCharacteristic(characteristic)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    broadcastUpdate(ACTION_GATT_CONNECTED)
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    broadcastUpdate(ACTION_GATT_DISCONNECTED)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic?.value)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic?.value)
        }
    }

    private fun broadcastUpdate(action: String, data: ByteArray? = null) {
        val intent = Intent(action)
        data?.let { intent.putExtra(EXTRA_DATA, it) }
        sendBroadcast(intent)
    }
}
