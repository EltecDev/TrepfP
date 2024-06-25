package com.example.trepfp.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
class ConexionNewBle(private val context: Context, private val macAddress: String) {

    private var bluetoothLeService: BluetoothLeServiceVersion? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            val binder = service as BluetoothLeServiceVersion.LocalBinder
            bluetoothLeService = binder.getService()
            isBound = true

            // Observar el LiveData para obtener la respuesta de escritura
            bluetoothLeService?.getWriteResponseLiveData()?.observe(context as LifecycleOwner, Observer { data ->
                // Manejar la respuesta de la escritura
                Log.d("ConexionNewBle", "Received write response: ${data.joinToString(" ") { byte -> String.format("%02X", byte) }}")
            })

            // Conectar al dispositivo
            val device = getBluetoothDevice(macAddress)
            if (device != null) {
                bluetoothLeService?.connect(device)
            } else {
                Log.e("ConexionNewBle", "Device not found with MAC address: $macAddress")
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothLeService = null
            isBound = false
        }
    }

    private val gattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeServiceVersion.ACTION_GATT_CONNECTED -> {
                    Log.d("ConexionNewBle", "GATT Connected")
                }
                BluetoothLeServiceVersion.ACTION_GATT_DISCONNECTED -> {
                    Log.d("ConexionNewBle", "GATT Disconnected")
                }
                BluetoothLeServiceVersion.ACTION_GATT_SERVICES_DISCOVERED -> {
                    Log.d("ConexionNewBle", "GATT Services Discovered")
                    // Obtener y trabajar con las características aquí
                    val services = bluetoothLeService?.bluetoothGatt?.services
                    val service = services?.find { it.uuid == BluetoothLeServiceVersion.UUID_TREFPB_SERVICE }
                    val characteristic = service?.getCharacteristic(BluetoothLeServiceVersion.UUID_TREFPB_RW)
                    if (characteristic != null) {
                        // Escribir en la característica
                        writeDataToCharacteristic(characteristic, "Hello".toByteArray())
                    }
                }
                BluetoothLeServiceVersion.ACTION_DATA_AVAILABLE -> {
                    val data = intent.getByteArrayExtra(BluetoothLeServiceVersion.EXTRA_DATA)
                    Log.d("ConexionNewBle", "Data available: ${data?.joinToString(" ") { byte -> String.format("%02X", byte) }}")
                }
            }
        }
    }

    init {
        Intent(context, BluetoothLeServiceVersion::class.java).also { intent ->
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothLeServiceVersion.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeServiceVersion.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeServiceVersion.ACTION_GATT_SERVICES_DISCOVERED)
            addAction(BluetoothLeServiceVersion.ACTION_DATA_AVAILABLE)
        }
        context.registerReceiver(gattUpdateReceiver, filter)
    }

    private fun getBluetoothDevice(macAddress: String): BluetoothDevice? {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter.getRemoteDevice(macAddress)
    }

    private fun writeDataToCharacteristic(characteristic: BluetoothGattCharacteristic, data: ByteArray) {
        bluetoothLeService?.writeCharacteristic(characteristic, data)
    }

    fun unbind() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
        context.unregisterReceiver(gattUpdateReceiver)
    }
}
