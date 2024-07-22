package com.example.trepfp

import Utility.BLEDevices
import Utility.GetRealDataFromHexaOxxoDisplay

import com.example.trepfp.ui.BluetoothLeServiceVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import kotlinx.coroutines.*
import org.chromium.base.ThreadUtils.runOnUiThread
import java.util.ArrayList

class NewConnectionTrepf(private val context: Context) {

    private var bluetoothLeService: BluetoothLeServiceVersion? = null
    private var job: Job? = null

    inner class MyAsyncTaskConnectBLE(
        private val mac: String,
        private val name: String,
        private val callback: MyCallback
    ) {

        var finalProcess = false
        var dataHND: MutableList<String>? = mutableListOf<String>()

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun execute() {
            callback.onProgress("Iniciando")
            job = CoroutineScope(Dispatchers.IO).launch {
                connectToDevice()
                callback.onProgress("Finalizando")
            }
        }

        private suspend fun connectToDevice() {
            callback.onProgress("Realizando")
            withContext(Dispatchers.Main) {

            }

            val maxAttempts = 20
            var tryCount = 0
            do {
                val isConnected = isConnected()
                if (isConnected) {
                    var llave = pedirLlaveComunicacion()
                    if (!llave.equals("empty")) {
                        var result = mandarLlaveComunicacion(llave!!.trim())
                        //BanderaLLave = true
                        Log.d("MyAsyncTaskConnectBLE", "resultado $result  BanderaLLave = true")
                    } else {
                      //  BanderaLLave = false
                        Log.d("MyAsyncTaskConnectBLE", "BanderaLLave = false")
                    }
                    callback.onSuccess(true)
                    cancelExecution()
                    return // Salir del bucle si la conexión es exitosa
                } else {
                    tryCount++
                    if (tryCount < maxAttempts) {
                        delay(1000) // Esperar un segundo antes de intentar de nuevo
                    } else {
                        // Si ha intentado el número máximo de veces y no se ha conectado, manejar el error
                        cancelExecution()
                        callback.onError("No se pudo conectar después de $maxAttempts intentos")
                        callback.onSuccess(false)
                        finalProcess = true
                        return // Salir del bucle
                    }
                }
            } while (tryCount < maxAttempts)
        }

        private fun isConnected(): Boolean {
            // Implementa tu lógica para verificar la conexión aquí
            // Ejemplo:
            // return bluetoothLeService?.isConnected() ?: false
            return false // Ajusta según tu implementación real
        }

        private fun pedirLlaveComunicacion(): String {
            // Implementa la lógica para obtener la llave de comunicación
            return "dummy_key" // Reemplaza con tu implementación real
        }

        private fun mandarLlaveComunicacion(llave: String): Boolean {
            // Implementa la lógica para enviar la llave de comunicación
            // Ejemplo:
            // return bluetoothLeService?.sendKey(llave) ?: false
            return true // Ajusta según tu implementación real
        }

        fun cancelExecution() {
            Log.d("", "job cancel connect")
            job?.cancel()
        }
    }

    fun pedirLlaveComunicacion(callback: (String) -> Unit) {
        bluetoothLeService?.apply {


            // Envía el comando "4070"
        //    sendComando("4070")

            // Escucha la respuesta del comando
            getWriteResponseLiveData().observe(context as LifecycleOwner, Observer { data ->
                // Maneja la respuesta de la escritura
                val hexString = data.joinToString(" ") { byte -> String.format("%02X", byte) }
                Log.d("ConexionNewBle", "Received write response: $hexString")

                // Procesa la respuesta según tus necesidades
                val response = processResponse(data)
                callback(response)
            })
        } ?: run {
            callback("No se pudo enviar el comando. BluetoothLeServiceVersion no inicializado.")
        }
    }
    private fun processResponse(data: ByteArray): String {
        // Implementa aquí la lógica para procesar la respuesta recibida
        // Devuelve la llave de comunicación u otro resultado según sea necesario
        return "dummy_key"
    }

    interface MyCallback {
        fun onSuccess(result: Boolean)
        fun onError(error: String)
        fun onProgress(progress: String)
    }
}
