package com.example.trepfp.ui




import Utility.TextUtil.fromHexString
import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.delay
import mx.eltec.BluetoothServices.BluetoothLeService
import mx.eltec.BluetoothServices.BluetoothLeService.Companion


import java.util.UUID

class BluetoothLeServiceVersion (/* var context: Context*/): Service() {

    var bluetoothGatt: BluetoothGatt? = null
    private val writeResponseLiveData = MutableLiveData<ByteArray>()
    var bluetoothManager: BluetoothManager? = null
    var writeCharacteristic: BluetoothGattCharacteristic? = null
    var readCharacteristic: BluetoothGattCharacteristic? = null


    val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
    val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
    val ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
    val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
    val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
    val UUID_TREFPB_RW: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    val UUID_TREFPB_SERVICE: UUID = UUID.fromString( "0000ffe0-0000-1000-8000-00805f9b34fb")
    private val BLUETOOTH_LE_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    var payload = 20
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                payload = mtu - 3
            }
            Log.d("onCharacteristicChanged","onMtuChanged mtu $mtu gat $gatt $status")
            connectCharacteristics3(gatt)
        }

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED
                broadcastUpdate(intentAction)
                Log.i(TAG, "Connected to GATT server.")
                bluetoothGatt?.discoverServices()
                Log.i(TAG, "Attempting to start service discovery: ${bluetoothGatt?.discoverServices()} status $status")

              //  connectCharacteristics3(gatt)


            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED
                Log.i(TAG, "Disconnected from GATT server.")
                broadcastUpdate(intentAction)
            }
        }



        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt.requestMtu(512)
                val service = gatt.getService(UUID_TREFPB_SERVICE)
                val characteristic = service.getCharacteristic(UUID_TREFPB_RW)

                gatt.setCharacteristicNotification(characteristic, true)
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                val descriptor = characteristic.getDescriptor(BLUETOOTH_LE_CCCD)
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            } else {
                Log.w(TAG, "onServicesDiscovered recibido: $status")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status != BluetoothGatt.GATT_SUCCESS) {

                return
            }
            Log.d(
                "BluetoothController",
                "onCharacteristicWrite " + characteristic.uuid.toString() + "  gat " + gatt
            )
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
        ) {

            super.onCharacteristicRead(gatt, characteristic, status)

            if (bluetoothGatt == null) {
                Log.e( TAG, "BluetoothGatt object is null")
                return
            }
            else {
                Log.e( TAG, "BluetoothGatt ${bluetoothGatt}")
            }
            broadcastUpdate( BluetoothLeService.ACTION_DATA_AVAILABLE, characteristic)
            //     broadcastUpdateBytes(mx.eltec.BluetoothServices.BluetoothLeService.ACTION_DATA_AVAILABLE, characteristic)
            val hexString = characteristicToHexString(characteristic)
            Log.d("onCharacteristicChanged", "onCharacteristicRead ${hexString}  $status")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic
        ) {

            super.onCharacteristicChanged(gatt, characteristic)
            Log.d(TAG, "onCharacteristicChanged: ${characteristic.uuid}   ${characteristic.value}")
            val hexString = characteristicToHexString(characteristic)
            addToListLogger(hexString)
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    override fun onCreate() {
        super.onCreate()
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    @SuppressLint("MissingPermission")
    fun isConnected(): Boolean {
        bluetoothGatt?.let { gatt ->
            val devices = bluetoothManager?.getConnectedDevices(BluetoothProfile.GATT)
            return devices?.contains(gatt.device) ?: false
        }
        return false
    }

    @SuppressLint("MissingPermission")
    fun connectCharacteristics3(gatt: BluetoothGatt) {
        writeCharacteristic = bluetoothGatt!!.getService(UUID_TREFPB_SERVICE).getCharacteristic(
            UUID_TREFPB_RW
        )
        readCharacteristic = bluetoothGatt!!.getService(UUID_TREFPB_SERVICE).getCharacteristic(
            UUID_TREFPB_RW
        )
        val writeProperties = writeCharacteristic?.properties
        if (writeProperties != null) {
            if (writeProperties and BluetoothGattCharacteristic.PROPERTY_WRITE + BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE == 0) {
                //Log.d("connectCharacteristics3","write characteristic not writable");
                return
            }
        }
        if (!gatt.setCharacteristicNotification(readCharacteristic, true)) {
            Log.d(
                "connectCharacteristics3",
                "no notification for read characteristic $readCharacteristic"
            );
            return
        }
        val readDescriptor = readCharacteristic?.getDescriptor( BLUETOOTH_LE_CCCD)
            ?: //Log.d("connectCharacteristics3","no CCCD descriptor for read characteristic");
            return
        val readProperties = readCharacteristic?.properties
        if (readProperties != null) {
            if (readProperties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
                readDescriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            } else if (readProperties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                readDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            } else {
                return
            }
        }
        if (!gatt.writeDescriptor(readDescriptor)) {
            //Log.d("connectCharacteristics3","FAIL");
        }
    }
    @SuppressLint("MissingPermission")
    fun sendComandoWrit(command: String) {
        bluetoothGatt?.let { gatt ->
            val service = gatt.getService(UUID_TREFPB_SERVICE)?.getCharacteristic(UUID_TREFPB_RW)
            service?.value = hexStringToByteArray(command)
            val result = gatt.writeCharacteristic(service)
            Log.d(TAG, "sendComandoWrit: writeCharacteristic result: $result")
        } ?: Log.e(TAG, "sendComandoWrit: BluetoothGatt is null")
    }
    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        Log.i(TAG, "Disconnected from GATT server.")
    }


    @SuppressLint("MissingPermission")
    fun sendCommand(command: String) {
        bluetoothGatt?.let { gatt ->
            val service = gatt.getService(UUID_TREFPB_SERVICE)
            val characteristic = service?.getCharacteristic(UUID_TREFPB_RW)

            // Verificar si la característica es escribible
            val writeProperties = characteristic?.properties
            if (writeProperties != null &&
                (writeProperties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
            ) {
                characteristic.value = hexStringToByteArray(command)
                val result = gatt.writeCharacteristic(characteristic)
                Log.d(TAG, "sendCommand: writeCharacteristic result: $result")
            } else {
                Log.e(TAG, "sendCommand: Characteristic not writable")
            }
        } ?: Log.e(TAG, "sendCommand: BluetoothGatt is null")
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


    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)

    fun connect(device: BluetoothDevice): Boolean {
        // Check if BluetoothAdapter or address is null


        // Initiate a direct connection to the device
        bluetoothGatt = device.connectGatt(this, /* autoConnect= */ false, gattCallback)


        return true
    }


    @SuppressLint("MissingPermission")
    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, data: ByteArray) {
        bluetoothGatt?.let {
            characteristic.value = data
            val result = it.writeCharacteristic(characteristic)
            Log.d(TAG, "writeCharacteristic: writeCharacteristic result: $result")
        }
    }

    fun getWriteResponseLiveData(): LiveData<ByteArray> {
        return writeResponseLiveData
    }

    private fun characteristicToHexString(characteristic: BluetoothGattCharacteristic): String {
        return characteristic.value.joinToString(separator = " ") { byte -> String.format("%02X", byte) }.replace(" ", "")
    }

    private fun addToListLogger(hexString: String) {
        Log.d("ListLogger", hexString)
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        Log.d(TAG, "broadcastUpdate: $intent")
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
        val UUID_TREFPB_SERVICE: UUID = UUID.fromString( "0000ffe0-0000-1000-8000-00805f9b34fb")
        ///"0000ffe0-0000-1000-8000-00805f9b34fb")
        private const val TAG = "BluetoothController"
    }
}
