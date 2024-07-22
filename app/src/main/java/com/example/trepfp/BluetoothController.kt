package com.example.trepfp


import BluetoothServices.BluetoothServices
import BluetoothServices.BluetoothServices.Companion
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import com.example.trepfp.ui.BluetoothLeServiceVersion
import mx.eltec.BluetoothServices.BluetoothLeService


class BluetoothController(context: Context) : Activity() {

    var getContext: Context? = context

    init {
//        bluetoothLeService =
      //  bluetoothLeService.bindService()
    }
    var bluetoothLeService : BluetoothLeServiceVersion? =  null //BluetoothLeServiceVersion(getContext!!)
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as BluetoothLeServiceVersion.LocalBinder
            bluetoothLeService = binder.getService()
            isBound = true
            Log.d("BluetoothController", "Service connected")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            bluetoothLeService = null
            isBound = false
            Log.d("BluetoothController", "Service disconnected")
        }
    }



    companion object {


        var Status: Boolean = false//sp!!.getString("userId", "") + false
        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
            return intentFilter
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun bindService() {
        val intent = Intent(getContext, BluetoothLeServiceVersion::class.java)
        getContext!!.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        getContext!!.registerReceiver(
            mGattUpdateReceiver,
            makeGattUpdateIntentFilter(),
            Context.RECEIVER_EXPORTED
        )
    }
    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if ((BluetoothLeService.ACTION_GATT_CONNECTED == action)) {



            }
            else if ((BluetoothLeService.ACTION_GATT_DISCONNECTED == action)) {

            }
            else if ((BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action)) {
                // Show all the supported services and characteristics on the user interface.
                //Log.d("ACTION_GATT_SERV_Dicovered","TRUE");
            } else if ((BluetoothLeService.ACTION_DATA_AVAILABLE == action)) {
                val data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA)
            }
        }
    }
    fun unbindService() {
        if (isBound) {
            getContext!!.unbindService(serviceConnection)
            isBound = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun connectToDevice(deviceAddress: String) {
        bindService() // Inicia la vinculación del servicio

        // Utiliza un handler para esperar a que el servicio se vincule completamente
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            if (isBound) {
                val device = bluetoothLeService?.bluetoothManager?.adapter?.getRemoteDevice(deviceAddress)
                device?.let {
                    val result = bluetoothLeService?.connect(it)
                    Log.d("BluetoothController", "connectToDevice result: $result")
                } ?: run {
                    Log.e("BluetoothController", "Device not found with address: $deviceAddress")
                }
            } else {
                Log.e("BluetoothController", "Service is not bound")
            }
        }, 1000) // Espera 1 segundo (ajusta este valor según sea necesario)
    }




    fun sendCommand(command: String) {
        bluetoothLeService?.sendCommand(command) ?: Log.e(
            "BluetoothLeServiceVersion", "Service is not bound"
        )
    }

    fun sendComandoWrit(command: String) {
        bluetoothLeService?.sendComandoWrit(command) ?: Log.e(
            "BluetoothLeServiceVersion", "Service is not bound"
        )
    }
    fun disconnect (){
        bluetoothLeService?.bluetoothGatt?.disconnect()
    }

    fun isConnected(): Boolean {
        return bluetoothLeService?.isConnected() ?: false
    }

    fun getWriteResponseLiveData(): LiveData<ByteArray>? {
        return bluetoothLeService?.getWriteResponseLiveData()
    }


}
