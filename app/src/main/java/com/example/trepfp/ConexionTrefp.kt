package com.example123.trepfp

import BluetoothServices.BluetoothServices
import BluetoothServices.ListenerInfoBle
import Utility.*
import Utility.GetHexFromRealDataImbera.calculateChacksumString
import Utility.GetRealDataFromHexaOxxoDisplay.cleanSpace
import Utility.GetRealDataFromHexaOxxoDisplay.hexToAsciiWifi
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.*
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import mx.eltec.BluetoothServices.BluetoothLeService
import mx.eltec.imberatrefp.BuildConfig
import mx.eltec.imberatrefp.R
import java.lang.Long.toHexString
import java.math.BigInteger
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow


class ConexionTrefp(
    context: Context, tvconnectionState: TextView?,
    tvfwversion: TextView?
) : ListenerInfoBle, BluetoothServices.ListenerSTATUS,
    BluetoothLeService.ListenergetMAC, Activity(), BluetoothLeService.BluetoothLeServiceListener {


    ////VERSION 1.0
    var progressdialog: AlertDialog? = null
    var dialogViewProgressBar: View? = null
    var deviceMacAddress: String? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    var name = ""
    var mac = ""
    var fw = ""
    var getContext: Context? = null
    var SalidaConnect: String? = null
    var listaTemporalFINAL = mutableListOf<String>()
    var listaTemporalEVENTFINAL = mutableListOf<String>()
    var locationResultListener: LocationResultListener? = null
    var listaTemporalFINALEVENTO = mutableListOf<String>()
    var listenerTREFP: ListenerTREFP? = null
    var FinalList = mutableListOf<String>()
    var newFimwareModelo = ""
    var newFirmwareVersion = ""
    var checksumTotal = 0
    var FinalFirmwareCommands = mutableListOf<String>()
    var command: String = ""
    var command2 = ""
    var titulo = ""
    val MapaFirmware = mutableListOf<Pair<String, String>>()
    var ListapruebaTime = mutableListOf<String>()

    var BanderaLLave: Boolean = false

    //val listener = getContext as ListenerTREFP
    var bluetoothLeService: BluetoothLeService? = null

    //var bluetoothServices: BluetoothServices
    lateinit var bluetoothServices: BluetoothServices
    var FinalListTest: MutableList<String?> = ArrayList()
    var listData: MutableList<String?> = ArrayList()
    var listDataGENERAL: MutableList<String?> = ArrayList()
    var realDataList: MutableList<String> = ArrayList()
    var FinalListData2: MutableList<String> = ArrayList()
    var FinalListData: MutableList<String?> = ArrayList()
    var FinalListDataRealState: MutableList<String> = ArrayList()
    var FinalListDataHandshake: MutableList<String?> = ArrayList()

    lateinit var tvconnectionState: TextView
    val ListDataR: MutableList<String>? = null
    var FinalListDataFinalT: MutableList<String?> = ArrayList()
    var FinalListDataFinalE: MutableList<String?> = ArrayList()

    var dataListPlantilla: MutableList<String> = ArrayList()
    var BanderaTiempo: Boolean = false
    var BanderaEvento: Boolean = false

    private val progressDialog2 by lazy { MakeProgressBar(getContext!!) }

    ////////////////////////////////////////////////////////////////////////////
    var onSuccess: Boolean = false
    var onError: String? = null
    var getInfo: MutableList<String?> = ArrayList()
    var onProgress: String? = null
    val sdf = SimpleDateFormat(
        "EEEE, d 'de' MMMM 'de' yyyy HH:mm:ss z",
        Locale.getDefault()
    )


    private var mHandler: Handler? = null
    private var mScanning = false
    private var mBluetoothAdapter: BluetoothAdapter? = null


    /////////////////////////////////////////////////////////////////////////
    var listaDevices: MutableList<BLEDevices>? = arrayListOf()

    var ListaD: MutableList<String>? = arrayListOf()
    var firmCommandCut: MutableList<String>? = arrayListOf()

    private var listMcc: MutableList<String>? = null

    //Pantalla de peticion inicial de permisos
    var sp: SharedPreferences? = null
    var esp: Editor? = null

    var FinalListDataTiempo: MutableList<String?> = ArrayList()
    var FinalListDataEvento: MutableList<String?> = ArrayList()

    fun clearLogger() {
        bluetoothLeService?.clearListLogger()
    }

    init {
        bluetoothServices =
            BluetoothServices(context, null, tvconnectionState, tvfwversion)

        bluetoothServices.registerBluetoothServiceListener(this)

        getContext = context
        if (tvconnectionState != null) {
            this.tvconnectionState = tvconnectionState
        }
        sp = context.getSharedPreferences(
            "connection_preferences",
            android.content.Context.MODE_PRIVATE
        )
        this.esp = sp?.edit()
        mBluetoothAdapter = bluetoothServices.bluetoothAdapter
        bluetoothLeService = bluetoothServices.bluetoothLeService
        // Toast.makeText(getContext,"dddddd",Toast.LENGTH_LONG).show()
        //  listener = getContext as ListenerTREFP
        //listener = getContext as ListenerTREFP// as ListenerTREFP


    }


    /*@Override
    public void conectado() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //new MyAsyncTaskGetHandshake().execute();
    }

    @Override
    public void desconectado() {
        new MyAsyncTaskDesconnectBLE().execute();
    }*/


    fun createProgressDialog(string: String?) {
        if (progressdialog == null) {
            //Crear dialogos de "pantalla de carga" y "popups if"


            val inflater =
                getContext!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            dialogViewProgressBar = inflater.inflate(R.layout.show_progress_bar, null, false)

            val adb =
                AlertDialog.Builder(getContext!!, R.style.Theme_AppCompat_Light_Dialog_Alert_eltc)
            adb.setView(dialogViewProgressBar)

            val txt = dialogViewProgressBar?.findViewById<View>(R.id.txtInfoProgressBar) as TextView
            txt.text = string

            progressdialog = adb.create()
            progressdialog!!.setCanceledOnTouchOutside(false)
            progressdialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            progressdialog!!.show()

        }
        progressdialog!!.show()
    }

    fun validaVacios(editTextsList: MutableList<EditText>): Boolean {
        var allEditTextsFilled = true

        for (editText in editTextsList) {
            if (editText.text.toString().isEmpty()) {
                // Establecer el error en el EditText si está vacío
                editText.error = "Campo vacío"
                allEditTextsFilled = false
                break  // No es necesario continuar si encontramos uno vacío
            } else {
                // Borrar el mensaje de error si el EditText no está vacío
                editText.error = null
            }
        }
        return allEditTextsFilled
    }

    fun ConvierteExa(valor: String): kotlin.String {
        var numf = valor.toString().toFloat()
        var num = numf.toInt()
        if (Integer.signum(num) == 1) {
            return GetHexFromRealDataCEOWF.convertDecimalToHexa(valor.toString())
                .toUpperCase() //decimales con punto //get temp positivo
        } else if (Integer.signum(num) == -1) {
            return GetHexFromRealDataCEOWF.getNeg(
                valor.toString().toFloat()
            ) //Integer.parseInt(String.valueOf(num))); //get negativos
        } else { //Es 0 cero
            val df: kotlin.String =
                GetHexFromRealDataCEOWF.convertDecimalToHexa(valor.toString().toString())
            Log.d("asdasdasdasdd", "-----> $df")
            return df //get negativos
        }
    }

    fun ConvierteExa2(valor: String): kotlin.String {
        var numf = valor.toString().toFloat()
        var num = numf.toInt() / 10
        if (Integer.signum(num) == 1) {
            return GetHexFromRealDataCEOWF.convertDecimalToHexa(valor.toString())
                .toUpperCase() //decimales con punto //get temp positivo
        } else if (Integer.signum(num) == -1) {
            return GetHexFromRealDataCEOWF.getNeg(
                valor.toString().toFloat()
            ) //Integer.parseInt(String.valueOf(num))); //get negativos
        } else { //Es 0 cero
            val df: kotlin.String =
                GetHexFromRealDataCEOWF.convertDecimalToHexa(valor.toString().toString())
            Log.d("asdasdasdasdd", "-----> $df")
            return df //get negativos
        }
    }

    fun traducirValores(valor: kotlin.String): kotlin.String? {
        val j = GetRealDataFromHexaImbera.getDecimalFloat(
            valor.toString()
        ) // decimales con punto

        if (j > 99.9) {
            //Extraccion de temperaturas en decimales
            //newData.add(getNegativeTemp("FFFF"+data.get(i)));
            Log.d(
                "pruebaTC",
                "entrada " + "FFFF" + valor.toString()
            )
            return getNegativeTemp(
                "FFFF" + valor.toString()
            )
        } else {
            //newData.add(String.valueOf(getDecimalFloat(data.get(i)) )); // decimales con punto
            return java.lang.String.valueOf(
                GetRealDataFromHexaImbera.getDecimalFloat(
                    valor.toString()
                )
            )
        }
    }

    fun validateEditTextRange(editText: EditText, lowerLimit: Float, upperLimit: Float): Boolean {
        val text = editText.text.toString().trim()
        if (text.isEmpty()) {
            // Si el texto está vacío, establece el mensaje de error y retorna false
            editText.error = "Campo vacío"
            return false
        }
        val value = text.toFloatOrNull()
        if (value == null) {
            // Si el texto no se puede convertir a un número, establece el mensaje de error y retorna false
            editText.error = "Formato inválido"
            return false
        }
        if (value < lowerLimit) {
            editText.error = "Por debajo del valor mínimo: " + lowerLimit
            return false
        }
        if (value > upperLimit) {
            editText.error = "Por encima del valor máximo: $upperLimit"
            return false
        }
        // Si el valor está dentro del rango, quita cualquier mensaje de error y retorna true
        editText.error = null
        return true

    }

    fun getNegativeTemp(hexaTemp: String): String? {
        val parsedResult = hexaTemp.toLong(16).toInt()
        val result = parsedResult.toDouble() / 10.0
        return result.toString()
    }


    /*
        inner class MyAsyncTaskConnectBLE(
            mac2: String,
            name2: String

        ) :
            AsyncTask<Int?, Int?, String>() {
            private var mac = mac2
            private var name = name2
            override fun onPostExecute(result: String) {
                //   MyAsyncTaskGetHandshake().execute()

                Log.d("progressConection","result post $result")
                if (result.equals("true"))
                {
                    Thread.sleep(800L)
                    clearLogger()
                    if (sp!!.getBoolean("isconnected", true)) {
                        bluetoothLeService = bluetoothServices.bluetoothLeService// getBluetoothLeService();
                        Thread.sleep(1000L)
                        clearLogger()
                        bluetoothLeService!!.sendFirstComando("4021")
                        Thread.sleep(2000L)

                        var data = getInfoList()
                        Log.d("MyAsyncTaskConnectBLE","data $data")
                        if (data.isNullOrEmpty()){
                            desconectar()
                            callback.onError("sin handshake")
                        }
                        else{
                            var llave = pedirLlaveComunicacion()
                            if (!llave.equals("empty")) {
                                var result = mandarLlaveComunicacion(llave!!.trim())
                                Log.d("mandarLlaveComunicacion", "resultado $result")
                            }
                            callback.getInfo(data)
                            callback.onSuccess(true)
                        }
                        /*if (bluetoothLeService!!.sendFirstComando("4021")) {
                            Log.d("", "dataChecksum total:7");

                            Log.d("funcioinToken", "chsk ${getInfoList()}")
                            Thread.sleep(500)
                            var llave = pedirLlaveComunicacion()


                            Log.d(
                                "funcioinToken",
                                "result llave ${llave} mac ${
                                    sp!!.getString(
                                        "mac",
                                        ""
                                    )
                                } name ${sp!!.getString("name", "")} "
                            )
                            if (!llave.equals("empty")) {
                                var result = mandarLlaveComunicacion(llave!!.trim())
                                Log.d("mandarLlaveComunicacion", "resultado $result")

                            }

                            Thread.sleep(500)
                            bluetoothLeService!!.sendComando("4021")
                            Thread.sleep(500)
                            listData.clear()
                            listData = bluetoothLeService!!.dataFromBroadcastUpdate as MutableList<String?>


                            callback.getInfo(listData as MutableList<String>)
                            /*
                                                FinalListData = GetRealDataFromHexaImbera.convert(
                                                    listData as List<String>,
                                                    titulo,
                                                    sp!!.getString("numversion", "")!!,
                                                    sp!!.getString("modelo", "")!!
                                                ) as MutableList<String?>
                                                var s =
                                                    GetRealDataFromHexaImbera.GetRealData(FinalListData as List<String> , "Handshake", "", "")
                                                s.map{
                                                    Log.d("ConexionBLE"," ss $s")
                                                }
                                                */
                            callback.onSuccess(true)
                            Resp = "ok"
                        } else {
                            callback.onSuccess(false)
                            Log.d("", "dataChecksum total:8");
                            Resp = "not"
                        }
                        */

                    }
                    else {
                        desconectar()
                        callback.onSuccess(false)
                        callback.onError("no conectado")
                    }
                }
                else{
                   callback.onSuccess(false)
                   callback.onError("Ya estas conectado a un dispositivo")
                }
             //   Resp = "noconnected"
                callback.onProgress("Finalizando")
            }

            override fun doInBackground(vararg params: Int?): String {
                bluetoothLeService = bluetoothServices.bluetoothLeService
              //  var resp = ""
                Log.d("progressConection","result doInBackground ${sp!!.getBoolean("isconnected", false)}")
                if (sp!!.getBoolean("isconnected", false)){
                  //  callback.onError("Ya estas conectado a un dispositivo")
                   return "falso"
                }

               else{
                    bluetoothServices.connect(name, mac)
                    listaDevices!!.clear()
                    listaDevices?.add(BLEDevices(name, mac, null))
                    ListaD?.add(mac)
                    callback.onProgress("Realizando")
                    return  "true"
               }
               // Thread.sleep(2500)
             //   return ""//if (isConnected?.name?.isNotEmpty() == true) "Conexión exitosa" else "Conexión fallida"
            }

            override fun onPreExecute() {
                callback.onProgress("Iniciando")
            }

            /* private fun connectToDevice(macAddress: String, name2: String): BluetoothDevice? {
                 // Aquí puedes implementar la lógica para conectarte a un dispositivo utilizando la dirección MAC proporcionada
                 // Devuelve true si la conexión es exitosa, false de lo contrario


                 // Ejemplo ficticio: Simplemente devuelve true si la dirección MAC no está vacía
                 bluetoothLeService = bluetoothServices.bluetoothLeService
                 Thread.sleep(500)
                 bluetoothServices.connect(name2, mac)
                 bluetoothLeService?.let { service ->
                     val gatt = service.mBluetoothGatt
                     val device = gatt?.device
                     Log.d("CONNECTEDConeccion", " bluetoothLeService?.let   $gatt $device")
                     if (device != null) {
                         try {
                             return gatt?.device
                         } catch (e: Exception) {
                             //return null
                         }
                         val handler = Handler()
                         handler.postDelayed({
                             Log.d(
                                 "CONNECTEDConeccion",
                                 "onPostExecuteHandler   ${getStatusConnectBle()}"
                             )

                             Thread.sleep(500)

                             callback.getInfo(getInfoList())
                             callback.onSuccess(getStatusConnectBle())

                             var HourNow: String? = null
                             HourNow = GetNowDateExa()
                             listData.clear()
                             var Command: String? = null
                             HourNow?.let {
                                 val CHECKSUMGEO = mutableListOf<String>()
                                 CHECKSUMGEO.add("4058")
                                 CHECKSUMGEO.add(HourNow.uppercase())
                                 CHECKSUMGEO.add(sumarParesHexadecimales(CHECKSUMGEO))
                                 val result = CHECKSUMGEO.joinToString("")

                                 Log.d(
                                     "MyAsyncTaskSendDateHour",
                                     "resultado $CHECKSUMGEO sin espacios  $result"
                                 )
                                 bluetoothLeService = bluetoothServices.bluetoothLeService
                                 if (bluetoothLeService?.sendFirstComando(result) == true) {
                                     Thread.sleep(450)
                                     listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                                     Thread.sleep(450)
                                     Log.d(
                                         "MyAsyncTaskSendDateHour",
                                         "->>>>>>>>>>>>>>>>>>>>>>>>${listData[0]}"
                                     )
                                     if (listData[0]?.equals("F1 3D") == true) {
                                         Log.d("MyAsyncTaskSendDateHour", "F1 3D ")
                                     } else {
                                         callback.onError("error hora F1 3E")
                                         Log.d("MyAsyncTaskSendDateHour", "error F1 3E")
                                     }
                                 } else {
                                     Log.d("", "dataChecksum total:8")
                                 }
                             } ?: "sin hora"

                             callback.onProgress("Finalizado")
                         }, 3000)
                     } else {
                         return null
                     }
                 }
                 return null
             }
             */
        }

        */

    private inner class MyAsyncTaskConnectBLE22(
        mac2: String,
        name2: String,
        private val callback: MyCallback
    ) :
        AsyncTask<Int?, Int?, String>() {
        private var mac = mac2
        private var name = name2
        override fun onPostExecute(result: String) {
            //   MyAsyncTaskGetHandshake().execute()
            var Resp = ""
            Thread.sleep(2500)
            clearLogger()
            if (sp!!.getBoolean("isconnected", false)) {
                bluetoothLeService = bluetoothServices.bluetoothLeService// getBluetoothLeService();
                if (bluetoothLeService!!.sendFirstComando("4021")) {
                    Log.d("", "dataChecksum total:7");

                    Log.d("funcioinToken", "chsk ${getInfoList()}")
                    Thread.sleep(500)
                    var llave = pedirLlaveComunicacion()


                    Log.d(
                        "funcioinToken",
                        "result llave ${llave} mac ${
                            sp!!.getString(
                                "mac",
                                ""
                            )
                        } name ${sp!!.getString("name", "")} "
                    )
                    if (!llave.equals("empty")) {
                        var result = mandarLlaveComunicacion(llave!!.trim())
                        Log.d("mandarLlaveComunicacion", "resultado $result")
                        /*  if(llave!!.length>=2){
                              var result = mandarLlaveComunicacion( llave!!.substring(result.length - 2))
                              Log.d("mandarLlaveComunicacion", "resultado $result")
                          }else{
                              var result = mandarLlaveComunicacion( llave!!)
                              Log.d("mandarLlaveComunicacion", "resultado $result")
                          }*/

                    }

                    Thread.sleep(500)
                    bluetoothLeService!!.sendComando("4021")
                    Thread.sleep(500)
                    listData.clear()
                    listData = bluetoothLeService!!.dataFromBroadcastUpdate as MutableList<String?>
                    Log.d("ConexionBLE", " CSK $listData")

                    callback.getInfo(listData as MutableList<String>)
                    /*
                                        FinalListData = GetRealDataFromHexaImbera.convert(
                                            listData as List<String>,
                                            titulo,
                                            sp!!.getString("numversion", "")!!,
                                            sp!!.getString("modelo", "")!!
                                        ) as MutableList<String?>
                                        var s =
                                            GetRealDataFromHexaImbera.GetRealData(FinalListData as List<String> , "Handshake", "", "")
                                        s.map{
                                            Log.d("ConexionBLE"," ss $s")
                                        }
                                        */
                    callback.onSuccess(true)
                    Resp = "ok"
                } else {
                    callback.onSuccess(false)
                    Log.d("", "dataChecksum total:8");
                    Resp = "not"
                }
            } else {
                callback.onSuccess(false)
                callback.onError("no conectado")
            }
            Resp = "noconnected"
            callback.onProgress("Finalizando")
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun doInBackground(vararg params: Int?): String {
            bluetoothLeService = bluetoothServices.bluetoothLeService
            bluetoothServices.connect(name, mac)
            listaDevices!!.clear()
            listaDevices?.add(BLEDevices(name, mac, null))
            ListaD?.add(mac)


            // val isConnected = connectToDevice(mac, name)
            //Thread.sleep(3000)
            // Devuelve el resultado de la prueba de conexión
            callback.onProgress("Realizando")
            Thread.sleep(2500)
            return ""//if (isConnected?.name?.isNotEmpty() == true) "Conexión exitosa" else "Conexión fallida"
        }

        override fun onPreExecute() {
            callback.onProgress("Iniciando")
        }

        /* private fun connectToDevice(macAddress: String, name2: String): BluetoothDevice? {
             // Aquí puedes implementar la lógica para conectarte a un dispositivo utilizando la dirección MAC proporcionada
             // Devuelve true si la conexión es exitosa, false de lo contrario


             // Ejemplo ficticio: Simplemente devuelve true si la dirección MAC no está vacía
             bluetoothLeService = bluetoothServices.bluetoothLeService
             Thread.sleep(500)
             bluetoothServices.connect(name2, mac)
             bluetoothLeService?.let { service ->
                 val gatt = service.mBluetoothGatt
                 val device = gatt?.device
                 Log.d("CONNECTEDConeccion", " bluetoothLeService?.let   $gatt $device")
                 if (device != null) {
                     try {
                         return gatt?.device
                     } catch (e: Exception) {
                         //return null
                     }
                     val handler = Handler()
                     handler.postDelayed({
                         Log.d(
                             "CONNECTEDConeccion",
                             "onPostExecuteHandler   ${getStatusConnectBle()}"
                         )

                         Thread.sleep(500)

                         callback.getInfo(getInfoList())
                         callback.onSuccess(getStatusConnectBle())

                         var HourNow: String? = null
                         HourNow = GetNowDateExa()
                         listData.clear()
                         var Command: String? = null
                         HourNow?.let {
                             val CHECKSUMGEO = mutableListOf<String>()
                             CHECKSUMGEO.add("4058")
                             CHECKSUMGEO.add(HourNow.uppercase())
                             CHECKSUMGEO.add(sumarParesHexadecimales(CHECKSUMGEO))
                             val result = CHECKSUMGEO.joinToString("")

                             Log.d(
                                 "MyAsyncTaskSendDateHour",
                                 "resultado $CHECKSUMGEO sin espacios  $result"
                             )
                             bluetoothLeService = bluetoothServices.bluetoothLeService
                             if (bluetoothLeService?.sendFirstComando(result) == true) {
                                 Thread.sleep(450)
                                 listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                                 Thread.sleep(450)
                                 Log.d(
                                     "MyAsyncTaskSendDateHour",
                                     "->>>>>>>>>>>>>>>>>>>>>>>>${listData[0]}"
                                 )
                                 if (listData[0]?.equals("F1 3D") == true) {
                                     Log.d("MyAsyncTaskSendDateHour", "F1 3D ")
                                 } else {
                                     callback.onError("error hora F1 3E")
                                     Log.d("MyAsyncTaskSendDateHour", "error F1 3E")
                                 }
                             } else {
                                 Log.d("", "dataChecksum total:8")
                             }
                         } ?: "sin hora"

                         callback.onProgress("Finalizado")
                     }, 3000)
                 } else {
                     return null
                 }
             }
             return null
         }
         */
    }


    fun returnLlaveConecct(): Boolean {
        return BanderaLLave
    }

    inner class MyAsyncTaskConnectBLE(
        private val mac: String,
        private val name: String,
        private val callback: MyCallback

    ) {
        private var job: Job? = null
        var finalProcess = false
        var dataHND: MutableList<String>? = mutableListOf<String>()
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun execute() {
            callback.onProgress("Iniciando")
            job = CoroutineScope(Dispatchers.IO).launch {// CoroutineScope(Dispatchers.IO).launch {
                connectToDevice()
                callback.onProgress("Finalizando")
            }
        }

        /*
                private suspend fun connectToDevice() {

                    callback.onProgress("Realizando")
                    bluetoothLeService = bluetoothServices.bluetoothLeService


                    var tryCount = 0
                    do {
        //              val isConnected = isConnected()
                        val isConnected = nuewIsConnected()
                        delay(1000)
                      //  Log.d("mandarLlaveComunicacion", "isConnected $isConnected")
                        if (isConnected) {
                            var llave = pedirLlaveComunicacion()
                            Log.d("funcioinToken","llave $llave ")
                            if (!llave.equals("empty")) {
                                var result = mandarLlaveComunicacion(llave!!.trim())
                                Log.d("mandarLlaveComunicacion", "resultado $result")
                            }
                            delay(1000)
                            callback.onSuccess(true)
                        }
                        else {
                            tryCount++
                            if (tryCount < 3) {
                                // Intenta reconectar después de un breve tiempo
                                delay(1000) // Simula un retraso antes de intentar la reconexión
                                Log.d("pruebaconexion", "Intento de reconexión número $tryCount")
                                // desconectar()
                            } else {
                                // Si ha intentado tres veces y no pudo conectar, desconecta
                                callback.onError("No se pudo conectar")
                                callback.onSuccess(false)
                                delay(1000)
                                desconectar()
                            }
                            delay(1000)
                        }
                        delay(1000)
                    } while (!isConnected && tryCount < 3)
                    delay(1000)
                    callback.onProgress("Finalizando")

                }
        */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private suspend fun connectToDevice() {
            callback.onProgress("Realizando")
            bluetoothLeService = bluetoothServices.bluetoothLeService
            var tryCount = 0

            val maxAttempts = 20
            do {
                val isConnected = nuewIsConnected()
                if (isConnected) {
                    var llave = pedirLlaveComunicacion()
                    if (!llave.equals("empty")) {
                        var result = mandarLlaveComunicacion(llave!!.trim())

                        BanderaLLave = true
                        Log.d("MyAsyncTaskConnectBLE", "resultado $result  BanderaLLave = true")
                    } else {
                        BanderaLLave = false
                        Log.d("MyAsyncTaskConnectBLE", "BanderaLLave = false")
                    }
                    callback.onSuccess(true)

                    cancelExecution()
                    return // Salir del bucle si la conexión es exitosa
                } else {
                    /* tryCount++
                     if (tryCount < maxAttempts) {
                         delay(1000) // Esperar un segundo antes de intentar de nuevo
                     } else {
                         */
                    // Si ha intentado el número máximo de veces y no se ha conectado, manejar el error
                    cancelExecution()
                    callback.onError("No se pudo conectar después de $maxAttempts intentos")
                    callback.onSuccess(false)
                    finalProcess = true


                    desconectar()

                    return // Salir del bucle
                    //  }
                }
            } while (tryCount < maxAttempts)

        }

        fun cancelExecution() {
            Log.d("", "job cancel connect")
            job?.cancel()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private suspend fun nuewIsConnected(): Boolean {
            bluetoothServices.connect(name, mac)
            listaDevices!!.clear()
            listaDevices?.add(BLEDevices(name, mac, null))
            ListaD?.add(mac)
            val maxAttempts = 20
            var isConnected = false
            var dataHND: List<String>? = null // Define dataHND fuera del bucle
            clearLogger()
            delay(5000)
            for (attempt in 1..maxAttempts) {
                delay(1000)
                bluetoothLeService = bluetoothServices.bluetoothLeService
                isConnected =
                    isBLEGattConnected() // sp?.getString("ACTION_GATT_CONNECTED", "") == "CONNECTED"
                //    bluetoothLeService!!.isGattConnected

                //    if (finalProcess == true) return break
                Log.d(
                    "pruebaConexionLOg",
                    "nuewIsConnected isConnected $isConnected attempt $attempt"
                )
                if (isConnected) {
                    bluetoothLeService?.sendComando("4021")
                    delay(800) // Simula un retraso en el envío del comando
                    dataHND = getInfoList()
                    if (!dataHND.isNullOrEmpty()) {
                        Log.d("mandarLlaveComunicacion", "dataHND $dataHND")
                        callback.getInfo(dataHND)
                        break
                    }
                } else {
                    clearLogger()
                    delay(1000)

                }

            }
            delay(1000)
            return isConnected && !dataHND.isNullOrEmpty()
        }


    }

    inner class MyAsyncTaskConnectBLE_Eltec(
        private val mac: String,
        private val name: String,
        private val callback: MyCallback

    ) {
        private var job: Job? = null
        var finalProcess = false
        var dataHND: MutableList<String>? = mutableListOf<String>()
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun execute() {
            callback.onProgress("Iniciando")
            job = CoroutineScope(Dispatchers.IO).launch {// CoroutineScope(Dispatchers.IO).launch {
                connectToDevice()
                callback.onProgress("Finalizando")
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private suspend fun connectToDevice() {
            callback.onProgress("Realizando")
            bluetoothLeService = bluetoothServices.bluetoothLeService
            var tryCount = 0

            val maxAttempts = 3
            do {
                val isConnected = nuewIsConnected()
                if (isConnected) {
                    var llave = pedirLlaveComunicacion()
                    if (!llave.equals("empty")) {
                        var result = mandarLlaveComunicacion(llave!!.trim())

                        BanderaLLave = true
                        Log.d("MyAsyncTaskConnectBLE", "resultado $result  BanderaLLave = true")
                    } else {
                        BanderaLLave = false
                        Log.d("MyAsyncTaskConnectBLE", "BanderaLLave = false")
                    }
                    callback.onSuccess(true)

                    cancelExecution()
                    return // Salir del bucle si la conexión es exitosa
                } else {
                    /* tryCount++
                     if (tryCount < maxAttempts) {
                         delay(1000) // Esperar un segundo antes de intentar de nuevo
                     } else {
                         */
                    // Si ha intentado el número máximo de veces y no se ha conectado, manejar el error
                    cancelExecution()
                    callback.onError("No se pudo conectar después de $maxAttempts intentos")
                    callback.onSuccess(false)
                    finalProcess = true


                    desconectar()

                    return // Salir del bucle
                    //  }
                }
            } while (tryCount < maxAttempts)

        }

        fun cancelExecution() {
            Log.d("", "job cancel connect")
            job?.cancel()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private suspend fun nuewIsConnected(): Boolean {
            bluetoothServices.connect(name, mac)
            listaDevices!!.clear()
            listaDevices?.add(BLEDevices(name, mac, null))
            ListaD?.add(mac)
            val maxAttempts = 3
            var isConnected = false
            var dataHND: List<String>? = null // Define dataHND fuera del bucle
            clearLogger()
            delay(2500)
            for (attempt in 1..maxAttempts) {
                delay(1000)
                bluetoothLeService = bluetoothServices.bluetoothLeService
                isConnected =
                    isBLEGattConnected() // sp?.getString("ACTION_GATT_CONNECTED", "") == "CONNECTED"
                //    bluetoothLeService!!.isGattConnected

                //    if (finalProcess == true) return break
                Log.d(
                    "pruebaConexionLOg",
                    "nuewIsConnected isConnected $isConnected attempt $attempt"
                )
                if (isConnected) {
                    bluetoothLeService?.sendComando("4021")
                    delay(800) // Simula un retraso en el envío del comando
                    dataHND = getInfoList()
                    if (!dataHND.isNullOrEmpty()) {
                        Log.d("mandarLlaveComunicacion", "dataHND $dataHND")
                        callback.getInfo(dataHND)
                        break
                    }
                } else {
                    clearLogger()
                    delay(1000)

                }

            }
            delay(1000)
            return isConnected && !dataHND.isNullOrEmpty()
        }


    }

    fun isBLEGattConnected(): Boolean {
        return bluetoothLeService?.retunisGattConnected ?: false
    }

    fun pedirLlaveComunicacion(): String? {
        //después de pedir la plantilla se debe pedir desbloquear la comunicación con la llave
        //pasos
        //pedir el numero aleatorio através de 4060 (4070)
        //si el número de la derecha es menor a 11 se pasa directo su valor hexa, sino habrías que sacarle con un 0 extra
        //eso da la posición dentro de la cadena de la MAC
        //se suba el complemento(numero negativo) del número aleatorio + el hexadecimal correspondiente al ascii de la MAC
        //el total se manda al 4061(4071)
        Log.d("funcioinToken", "funcioinToken")
        bluetoothLeService!!.sendComando("4070")
        // bluetoothServices.sendComando("askpassconnection")
        try {
            Thread.sleep(500)
            var listData: List<String?> = ArrayList()
            return if (bluetoothLeService == null) {
                //noconnected
                "noconnected"
            } else {
                runOnUiThread {
                    // getInfoList()?.clear()
                    clearLogger()

                }
                listData = bluetoothLeService!!.dataFromBroadcastUpdate
                if (listData.isEmpty()) { //si es vacio es que es un control que no tiene comando de password disponible
                    "empty"
                } else {
                    //    getInfoList()
                    cleanSpace(listData as MutableList<String?>).toString() //-24 posición de mdelo 2bytes

                }
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return "false"
    }

    fun mandarLlaveComunicacion(randomNum: String): String? {
        val nibleBajo = randomNum.substring(1)
        var mac = sp!!.getString("mac", "")
        var letraMacElegida = ""
        Log.d("mandarLlaveComunicacion", "randomNum:$randomNum")
        Log.d("mandarLlaveComunicacion", "mac:$mac")
        Log.d("mandarLlaveComunicacion", "mac:" + mac!!.replace(":", ""))
        mac = mac.replace(":", "")

        letraMacElegida = when (nibleBajo) {
            "0" -> mac.substring(0, 1)
            "1" -> mac.substring(1, 2)
            "2" -> mac.substring(2, 3)
            "3" -> mac.substring(3, 4)
            "4" -> mac.substring(4, 5)
            "5" -> mac.substring(5, 6)
            "6" -> mac.substring(6, 7)
            "7" -> mac.substring(7, 8)
            "8" -> mac.substring(8, 9)
            "9" -> mac.substring(9, 10)
            "A" -> mac.substring(10, 11)
            "B" -> mac.substring(11, 12)
            else -> {
                ""
            }
        }

        /*  when (nibleBajo) {
              "0" -> {
                  letraMacElegida = mac.substring(0, 1)
              }

              "1" -> {
                  letraMacElegida = mac.substring(1, 2)
              }

              "2" -> {
                  letraMacElegida = mac.substring(2, 3)
              }

              "3" -> {
                  letraMacElegida = mac.substring(3, 4)
              }

              "4" -> {
                  letraMacElegida = mac.substring(4, 5)
              }

              "5" -> {
                  letraMacElegida = mac.substring(5, 6)
              }

              "6" -> {
                  letraMacElegida = mac.substring(6, 7)
              }

              "7" -> {
                  letraMacElegida = mac.substring(7, 8)
              }

              "8" -> {
                  letraMacElegida = mac.substring(8, 9)
              }

              "9" -> {
                  letraMacElegida = mac.substring(9, 10)
              }

              "A" -> {
                  letraMacElegida = mac.substring(10, 11)
              }

              "B" -> {
                  letraMacElegida = mac.substring(11, 12)
              }
          }*/
        if (letraMacElegida == "") {
            val binario: String = GetRealDataFromHexaImbera.HexToBinary(nibleBajo)
            Log.d("binario", ":$binario")
            letraMacElegida = when (binario.substring(5)) {
                "100" -> mac.substring(4, 5) //posiicon 4
                "101" -> mac.substring(5, 6)
                "110" -> mac.substring(6, 7)
                "111" -> mac.substring(7, 8)
                else -> {
                    ""
                }
            }
        }
        val subtrahend = BigInteger(randomNum, 16)
        // input, you can take input from user and use after validation
        val array = CharArray(subtrahend.toString(16).length)
        // construct a character array of the given length
        Arrays.fill(array, 'F')
        // fill the array by F, look at the first source there the FF is subtracted by 2D
        val minuend = BigInteger(String(array), 16)
        // construct FFF... Biginteger of that length
        val difference = minuend.subtract(subtrahend)
        // calculate minus
        val result = difference.add(BigInteger.ZERO)
        // add one to it
        println(result.toString(16))
        // print it in hex format
        val num1: Int = GetRealDataFromHexaImbera.getDecimal(result.toString(16))
        val ch = letraMacElegida[0]
        var hexaFinal =
            GetHexFromRealDataOxxoDisplay.convertDecimalIntToHexa((num1 + ch.code).toString())
        bluetoothLeService = bluetoothServices.bluetoothLeService
        if (hexaFinal.length > 2) {
            hexaFinal = hexaFinal.substring(1)
        }
        Log.d("hexaFinal", "22:$hexaFinal")
        bluetoothLeService!!.sendFirstComando("4071$hexaFinal")
        try {
            Thread.sleep(500)
            var listData: List<String?> = ArrayList()
            return if (bluetoothLeService == null) {
                //noconnected
                "noconnected"
            } else {
                listData = bluetoothLeService!!.dataFromBroadcastUpdate
                if (listData.isEmpty()) { //si es vacio es que es un control que no tiene comando de password disponible
                    "empty"
                } else {
                    val s =
                        cleanSpace(listData as MutableList<String?>) //-24 posición de mdelo 2bytes
                    if (s.toString() == "F13D") {
                        "ok"
                    } else {
                        "notok"
                    }
                }
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return "false"
    }

    inner class MyAsyncTaskConnTest(mac2: String, name2: String, private val callback: MyCallback) :
        AsyncTask<Void, Void, String?>() {
        private var mac = mac2
        private var name = name2
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun doInBackground(vararg params: Void?): String? {
            callback.onProgress("Realizando")
            try {
                listaDevices?.add(BLEDevices(name, mac, null))
                ListaD?.add(mac)
                Log.d(
                    "Devices FINAL:",
                    " $mac ${listaDevices.toString()}    new ${ListaD.toString()}"
                )
                // Thread.sleep(700)

            } catch (e: Exception) {
                e.message?.let { callback.onError(it) }
            }
            // val isConnected = connectToDevice(mac, name)
            bluetoothLeService = bluetoothServices.bluetoothLeService
            bluetoothServices.connect(name, mac)
            //Thread.sleep(3000)
            // Devuelve el resultado de la prueba de conexión
            callback.onProgress("Finalizado")
            return "Conexión exitosa" // if (isConnected?.name?.isNotEmpty() == true) "Conexión exitosa" else "Conexión fallida"

        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            val handler = Handler()
            handler.postDelayed({

                bluetoothLeService = bluetoothServices.bluetoothLeService
                bluetoothLeService?.let { service ->
                    val gatt = service.mBluetoothGatt
                    val device = gatt?.device
                    val d = if (device != null) false else true
                    //  val Con = if (gatt.disconnect())
                    Log.d("CONNECTEDConeccion", " bluetoothLeService?.let   $gatt $device  ${d}")


                    if (device != null) {
                        // callback.onSuccess(getStatusConnectBle())

                        /*  try {
                              Thread.sleep(1000)
                              Log.d(
                                  "CONNECTEDConeccion",
                                  "Gantt HND ${bluetoothLeService?.mBluetoothGatt?.device} ${getStatusConnectBle()}"
                              )
                              bluetoothServices.bluetoothLeService?.getList()?.clear()
                              bluetoothServices.bluetoothLeService?.getLogeer()?.clear()
                              Thread.sleep(500)
                              if (/*sp?.getBoolean("isconnected", false) == true*/ device != null) {
                                  bluetoothLeService = bluetoothServices.bluetoothLeService
                                  if (bluetoothLeService?.sendFirstComando("4021") == true) {
                                      Log.d("CONNECTEDConeccion", "dataChecksum total:7")
                                      //     return "ok"
                                  } else {
                                      Log.d("CONNECTEDConeccion", "dataChecksum total:8")
                                      //   return "not"
                                  }
                              } else {
                                  //  return "noconnected"
                              }
                          } catch (e: Exception) {
                              //return null
                          }
                          val handler = Handler()
                          handler.postDelayed({

                              Log.d(
                                  "CONNECTEDConeccion",
                                  "onPostExecuteHandler   ${getStatusConnectBle()}"
                              )

                              Thread.sleep(500)

                              callback.onSuccess(getStatusConnectBle())
                              callback.getInfo(getInfoList())

                              callback.onProgress("Finalizado")
                          }, 4000)
                          */
                    } else {
                        callback.onSuccess(getStatusConnectBle())
                    }
                }

            }, 4000)

        }

        override fun onPreExecute() {
            callback.onProgress("Iniciando")
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private fun connectToDevice(macAddress: String, name2: String): BluetoothDevice? {
            // Aquí puedes implementar la lógica para conectarte a un dispositivo utilizando la dirección MAC proporcionada
            // Devuelve true si la conexión es exitosa, false de lo contrario


            // Ejemplo ficticio: Simplemente devuelve true si la dirección MAC no está vacía
            bluetoothLeService = bluetoothServices.bluetoothLeService

            bluetoothServices.connect(name2, mac)
            bluetoothLeService?.let { service ->
                val gatt = service.mBluetoothGatt
                val device = gatt?.device
                Log.d("CONNECTEDConeccion", " bluetoothLeService?.let   $gatt $device")
                if (device != null) {
                    try {
                        return gatt?.device
                    } catch (e: Exception) {
                        //return null
                    }
                    val handler = Handler()
                    handler.postDelayed({
                        Log.d(
                            "CONNECTEDConeccion",
                            "onPostExecuteHandler   ${getStatusConnectBle()}"
                        )

                        Thread.sleep(500)

                        callback.getInfo(getInfoList())
                        callback.onSuccess(getStatusConnectBle())

                        var HourNow: String? = null
                        HourNow = GetNowDateExa()
                        listData.clear()
                        var Command: String? = null
                        HourNow?.let {
                            val CHECKSUMGEO = mutableListOf<String>()
                            CHECKSUMGEO.add("4058")
                            CHECKSUMGEO.add(HourNow.uppercase())
                            CHECKSUMGEO.add(sumarParesHexadecimales(CHECKSUMGEO))
                            val result = CHECKSUMGEO.joinToString("")

                            Log.d(
                                "MyAsyncTaskSendDateHour",
                                "resultado $CHECKSUMGEO sin espacios  $result"
                            )
                            bluetoothLeService = bluetoothServices.bluetoothLeService
                            if (bluetoothLeService?.sendFirstComando(result) == true) {
                                Thread.sleep(450)
                                listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                                Thread.sleep(450)
                                Log.d(
                                    "MyAsyncTaskSendDateHour",
                                    "->>>>>>>>>>>>>>>>>>>>>>>>${listData[0]}"
                                )
                                if (listData[0]?.equals("F1 3D") == true) {
                                    Log.d("MyAsyncTaskSendDateHour", "F1 3D ")
                                } else {
                                    callback.onError("error hora F1 3E")
                                    Log.d("MyAsyncTaskSendDateHour", "error F1 3E")
                                }
                            } else {
                                Log.d("", "dataChecksum total:8")
                            }
                        } ?: "sin hora"


                    }, 3000)
                } else {
                    return null
                }
            }
            return null
        }
    }


    fun convertDecimalToHexa4(s: Double): String {

        val f = s * 10000
        val nm = f.toInt()
        val c = Integer.toHexString(nm)
        Log.d("checkGooglePlayServices", "----convertDecimalToHexa nm $nm c $c  s $s f $f")
        val paddedHex = c.padStart(4, '0')
        return paddedHex

    }

    fun getNegativeTempfloat4(hexaTemp: String): String {

        val parsedResult: Double = hexaTemp.toLong(16).toInt().toDouble() // as  Int.toFloat()
        Log.d("checkGooglePlayServices", "parsedResult $parsedResult hexaTemp $hexaTemp")
        val result = parsedResult / 100000.0

        return String.format("%.4f", result)
    }

    fun getDecimalFloat2(hex: String): String {
        val decimalPlaces = 6
        var hex = hex
        val digits = "0123456789ABCDEF"
        hex = hex.uppercase(Locale.getDefault())
        var `val` = 0.0
        for (element in hex) {
            val c = element
            val d = digits.indexOf(c)
            `val` = 16 * `val` + d
        }
        Log.d("checkGooglePlayServices", "`val` ${`val`}")
        val result = `val` / 10000.0
        return String.format("%.4f", result)
    }

    fun GetMacBle(): String? {
        return getMAC()
    }


    fun agregar00AlPenultimoCaracter(
        lista: MutableList<String>?,
        posicion: Int
    ): MutableList<String>? {
        lista?.let { // Verificar si la lista no es nula
            for (i in 0 until lista.size) {
                val registro = lista[i]
                if (registro.length >= posicion) {
                    val nuevoRegistro = registro.substring(
                        0,
                        registro.length - posicion
                    ) + "00" + registro.substring(registro.length - posicion)
                    lista[i] = nuevoRegistro
                }
            }
            return lista // Devolver la lista modificada
        }
        return null // Devolver null si la lista es nula
    }

    fun ObtenerLogger(callback: CallbackLogger) {
        val isEVENTOK = AtomicBoolean(false)
        val isTIMEOK = AtomicBoolean(false)
        var registros = mutableListOf<String>()
        var registrosPRE = mutableListOf<String>()
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            // Operaciones dentro de la corrutina

            val pre = withContext(Dispatchers.Default) {
                runOnUiThread {
                    // getInfoList()?.clear()
                    clearLogger()
                    callback.onProgress("Iniciando Logger")
                    callback.onProgress("Iniciando Tiempo")
                }
                BanderaTiempo = true
            }
            val result = withContext(Dispatchers.Default) {
                FinalListDataTiempo.clear()
                FinalListDataFinalT.clear()
                var listDataGT: MutableList<String>? = ArrayList()

                return@withContext listDataGT// getInfoList()//
            }
            val post = withContext(Dispatchers.Default) {
                /***************************************************COMIENZA LA FUNCION DE TIEMPO*******************************************/

                delay(700)
                //   Log.d("getInfoListFinal", getInfoList().toString())
                delay(700)
                FinalListDataTiempo.clear()
                FinalListDataFinalT.clear()
                var listDataGT: MutableList<String>? = ArrayList()
                var listDataEVENT: MutableList<String>? = ArrayList()
                // callback.onProgress("Realizando Tiempo")
                if (ValidaConexion()) {
                    bluetoothLeService = bluetoothServices.bluetoothLeService()
                    delay(500)
                    bluetoothServices.sendCommand("handshake", "4021")
                    delay(500)

                    runOnUiThread {
                        // getInfoList()?.clear()
                        clearLogger()
                    }
                    delay(1000)
                    bluetoothServices.sendCommand("time", "4060")
                    delay(3000)
                    if (listDataGT != null) {
                        synchronized(listDataGT) {
                            do {
                                bluetoothLeService?.dataFromBroadcastUpdateString?.let {
                                    listDataGT.add(
                                        it
                                    )
                                }
                                Thread.sleep(700)
                            } while (bluetoothLeService?.dataFromBroadcastUpdateString?.isNotEmpty() == true)
                        }
                    }

                    Thread.sleep(2000)
                    var FINALLISTA = mutableListOf<String?>()
                    runOnUiThread {
                        FINALLISTA = VTIME(
                            getInfoList() as MutableList<String?>
                        )
                    }
                    Thread.sleep(1000)
                    Log.d("crudotiempoTIEMPO", "salida de FINALLISTA $FINALLISTA")
                    runOnUiThread {
                        clearLogger()
                        //  getInfoList()!!.clear()
                    }
                    Thread.sleep(1000)
                    bluetoothServices.sendCommand("handshake", "4021")
                    Thread.sleep(700)
                    //getInfoList()!!.clear()
                    runOnUiThread {
                        clearLogger()
                        //  getInfoList()!!.clear()
                    }
                    Thread.sleep(1000)
                    bluetoothServices.sendCommand("event", "4061")

                    if (listDataEVENT != null) {
                        synchronized(listDataEVENT) {
                            do {
                                bluetoothLeService?.dataFromBroadcastUpdateString?.let {
                                    listDataEVENT.add(
                                        it
                                    )
                                }
                                Thread.sleep(700)
                            } while (bluetoothLeService?.dataFromBroadcastUpdateString?.isNotEmpty() == true)
                        }
                    }
                    runOnUiThread {
                        registrosPRE = VEvent(
                            getInfoList() as MutableList<String?>
                        ) as MutableList<String>
                    }
                    Thread.sleep(1000)
                    for (item in registrosPRE) {
                        if (item.length > 15) {
                            registros.add(item)
                        }
                    }

                    if (!registros.isNullOrEmpty()) {
                        var smallestDifference: Long = Long.MAX_VALUE
                        //   var closestIndex = -1

                        delay(1000)
                        var listFTIME: MutableList<String>? = ArrayList()

                        val iterator = FINALLISTA.iterator()
                        while (iterator.hasNext()) {
                            val item = iterator.next()
                            //  Log.d("FINALLISTA", item)
                            if (item!!.length >= 10) {
                                listFTIME!!.add(item)
                            }
                        }
                        delay(1000)
                        if (listFTIME != null) {
                            for ((index, item) in listFTIME.withIndex()) {

                                Log.d(
                                    "FINALLISTAFINALLISTA",
                                    " $index $item ${convertirHexAFecha(item.substring(0, 8))}"
                                )
                            }
                        }

                        runOnUiThread {
                            //  getInfoList()?.clear()
                            clearLogger()
                        }
                        bluetoothLeService!!.sendFirstComando("405B")
                        Thread.sleep(700)
                        listData.clear()
                        listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                        val s = listData[0]
                        val sSinEspacios = s?.replace(" ", "")
                        var TIMEUNIX: String? = null
                        if (sSinEspacios?.length!! >= 24) TIMEUNIX =
                            sSinEspacios?.substring(16, 24) else TIMEUNIX = "612F6B47"
                        var listTIMEUNIX: MutableList<String>? = ArrayList()
                        TIMEUNIX?.let {
                            listTIMEUNIX?.add(it)
                        };"612F6B47"
                        val TIMEUNIX2022 = "62840A56"//"75840A56"// "62840A56"
                        if (TIMEUNIX/*"75840A56"*/!! <= TIMEUNIX2022!!) {
                            //  if (/*TIMEUNIX/*/*"75840A56"*/!!<=TIMEUNIX2022!!){
                            println("------------------------------------------------------ este es menor")
                            Log.d(
                                "getInfoListFinal",
                                "------------------------------------------------------ este es menor"
                            )

                            if (!listFTIME.isNullOrEmpty()) {
                                try {
                                    val registros = mutableListOf<String>()
                                    FINALLISTA.map {
                                        if (it!!.length == 18) {
                                            registros.add(it)
                                        }
                                    }
                                    val ListaTiempoUPdate =
                                        LOGGERTIME(registros) //mutableListOf<String>()

                                    val sttiempo = java.lang.StringBuilder()
                                    Log.d("TAMAÑO", "T:$ListaTiempoUPdate")
                                    Log.d("TAMAÑO", "TSize:" + ListaTiempoUPdate.size)
                                    for (h in ListaTiempoUPdate/*lista*/.indices) {
                                        if (h > 0) {
                                            sttiempo.append(ListaTiempoUPdate/*lista*/[h])
                                            sttiempo.append(";")
                                        }

                                    }
                                    FinalListDataFinalT.add(sttiempo.toString())
                                    runOnUiThread {

                                        callback.getTime(
                                            agregar00AlPenultimoCaracter(
                                                FinalListDataFinalT as MutableList<String>?,
                                                2
                                            )
                                        )
                                    }
                                    /*runOnUiThread {
                                        callback.getTime(FinalListDataFinalT as MutableList<String>?)
                                    }*/
                                    FinalListDataFinalT?.map { Log.d("TAMAÑO", "DATA :$it") }
                                    runOnUiThread {
                                        callback.onSuccess(GetStatusBle())
                                    }
                                    // sendInfoFirestoreNEW()
                                    BanderaTiempo = false
                                } catch (Ex: Exception) {
                                    runOnUiThread {
                                        callback.onError("Tiempo $Ex")
                                    }
                                }
                            } else {
                                //   callback.onSuccess(false)
                                runOnUiThread {
                                    callback.onError("Lista vacia Tiempo")
                                }
                            }
                            runOnUiThread {
                                callback.onProgress("Finalizado Tiempo")
                            }
                            //   callback.onSuccess(true)

                        } else {

                            /* val registros = arrayListOf(
                                 "6467B5AC6467BD1901004110097E",
                                 "6467B5E06467BD7302004020097E",
                                 "6467BD1C6467BDA001004030097E",
                                 "6467B0006467B12504003F40097F",
                                 "6467B1C56467B1F503003F50097F",
                                 "6467B1E56467B2F503003660097F"
                             )*/
                            /* val registrosTIMEPODUMMY = arrayListOf(
                                  "6467ABAC0040000971",
                                  "6467ABFC0040000972",
                                  "6467B0000040000973",
                                  "6467BE350040000974",
                                  "6467BE9D0040000975",
                                  "6467BED90040000976",
                                  "6467BF150040000977",
                                  "6467BF510040000978",
                                  "6467BF8D0040000979"
                              )*/
                            val indexCount: MutableList<Int> = ArrayList()
                            var count = 0
                            for ((index, item) in registros.withIndex()) {
                                val EVENT_TYPE = item!!.substring(16, 18)
                                if (EVENT_TYPE == "04") {
                                    count++
                                    println("-> $item  $count index $index")
                                    indexCount.add(index!!)
                                }
                            }
                            val iC = indexCount.maxOrNull()
                            var listaTemporalFINALTIME = mutableListOf<String>()
                            when (count) {

                                0 -> {
                                    Log.d(
                                        "FFFFFFFFFFFFFFFFF",
                                        "No hay elementos con EVENT_TYPE == \"04\""
                                    )
                                    var listD: MutableList<String>? = ArrayList()
                                    for (item in FINALLISTA) {
                                        if (item!!.length >= 10) {
                                            listD!!.add(item!!)
                                        }
                                    }
                                    Log.d(
                                        "FFFFFFFFFFFFFFFFF",
                                        "Hay $count elemento con EVENT_TYPE == \"04\""
                                    )
                                    isTIMEOK.set(true)
                                    runOnUiThread {
                                        callback.getTime(agregar00AlPenultimoCaracter(listD, 2))
                                        //callback.getTime(listD)
                                    }
                                }

                                /*   1 -> {

                                       registros?.map {
                                           val hexString =it.substring(0, 8) // valor hexadecimal que se desea convertir
                                           val hexString2 =it.substring(8, 16)
                                           Log.d(
                                               "datosResult->>>",
                                               "->S $it   ${convertirHexAFecha(hexString)}   ${
                                                   convertirHexAFecha(hexString2)
                                               } "
                                           )
                                       } ;Log.d(
                                           "datosResult->>>",
                                           "->S VACIO "
                                       )
               //                        Log.d("FFFFFFFFFFFFFFFFF","Índice correspondiente: $closestIndex  ${listFTIME?.get(closestIndex)}")
                                       var closestTimestamp: String? = null
                                       var closestIndex = -1
                                       var smallestDifference: Long = Long.MAX_VALUE
                                         val indexCount: MutableList<Int> = ArrayList()
                                       var count = 0
                                       for ((index, item) in registros.withIndex()) {
                                           val EVENT_TYPE = item!!.substring(16, 18)
                                           if (EVENT_TYPE == "04") {
                                               count++
                                               println("-> $item  $count index $index")
                                               indexCount.add(index!!)
                                           }
                                       }
                                       val iC = indexCount.maxOrNull()
                                       Log.d("FFFFFFFFFFFFFFFFF","Hay $count elemento con EVENT_TYPE == \"04\"")
                                       val targetTimestamp =  registros[iC!!].substring(8, 16)//"6467B000" // Valor objetivo
                                       for ((index, registro) in listFTIME!!.withIndex()) {
                                           val timestampDifference = Math.abs(targetTimestamp.toLong(16) - registro.substring(0, 8).toLong(16))
                                           if (timestampDifference < smallestDifference) {
                                               smallestDifference = timestampDifference
                                               closestTimestamp = registro
                                               closestIndex = index
                                           }
                                       }
                                       Log.d("FFFFFFFFFFFFFFFFF","El valor más cercano al timestamp objetivo ($targetTimestamp) ${convertirHexAFecha(targetTimestamp)} ")

                                       Log.d("FFFFFFFFFFFFFFFFF","El valor más cercano al timestamp objetivo  es: $closestTimestamp   ")
                                       Log.d("FFFFFFFFFFFFFFFFF","Índice correspondiente: $closestIndex  ${listFTIME?.get(closestIndex)}  ${convertirHexAFecha(listFTIME?.get(closestIndex)!!.substring(0,8))} ")


                                       for ((index, registro) in listFTIME.withIndex()) {
                                           val hexString =registro.substring(0, 8)  // valor hexadecimal que se desea convertir
                                           //   val hexString2 =it.substring(8, 16)
                                           println("->S $registro   ${convertirHexAFecha(hexString)}  $index ")
                                       }
                                       registros.map{
                                           val hexString =it.substring(0, 8) // valor hexadecimal que se desea convertir
                                           val hexString2 =it.substring(8, 16)
                                          Log.d("FFFFFFFFFFFFFFFFF","->S $it   ${convertirHexAFecha(hexString)}   ${convertirHexAFecha(hexString2)}")
                                       }
                                       val hexStringFINALTIME = listFTIME.last().substring(0, 8)
                                       val currentTimeMillis = System.currentTimeMillis()
                                       val currentTimeHexMILLNOWMINUSDIF =
                                           java.lang.Long.toHexString(currentTimeMillis / 1000).uppercase()

                                       listaTemporalFINALTIME.add("$currentTimeHexMILLNOWMINUSDIF${listFTIME.last().substring(8,listFTIME.last().length)}")
                                       //   println("------------------------------------ $listaTemporalFINALTIME")
                                       for (i in listFTIME.size - 2 downTo closestIndex +1) {
                                           val registro = listFTIME[i]
                                           // Aquí puedes realizar alguna operación con el registro, por ejemplo:
                                           //   println("Registro $i: $registro  $currentTimeHexMILLNOWMINUSDIF")

                                           val fechaHora1 = Date(listFTIME[i]!!.substring(0, 8).toLong(16) * 1000)
                                           val fechaHora2 = Date(listFTIME[i - 1]!!.substring(0, 8).toLong(16) * 1000)
                                           val dif = (listFTIME[i]!!.substring(0, 8).toLong(16) * 1000)-(listFTIME[i - 1]!!.substring(0, 8).toLong(16) * 1000)
                                           val FINALDIF= (listaTemporalFINALTIME.last()!!.substring(0, 8).toLong(16) * 1000) - dif
                                           //   val dateFINALDIF= Date(FINALDIF)
                                           val date = Date(FINALDIF)
                                           val fechaHoraExadecimal =
                                               BigInteger.valueOf(date.time / 1000).toString(16).padStart(8, '0').uppercase()

                                           //  println("PPPPPPPPPPPPPPPPPPPPPPPPPPP  ${listaTemporalFINALTIME.last()} ${date}  $FINALDIF  $fechaHoraExadecimal  ")
                                           listaTemporalFINALTIME.add(fechaHoraExadecimal+listFTIME[i]!!.substring(8,listFTIME[i]!!.length))
                                           // println(" ${registrosTIMEPODUMMY[i]} $i  fechaHora1 $fechaHora1 fechaHora2 $fechaHora2 dif $dif   ------------- ${registrosTIMEPODUMMY[i]} ${registrosTIMEPODUMMY[i-1]} ")


                                       }

                                       for (i in closestIndex downTo 0) {
                                           listaTemporalFINALTIME.add(listFTIME[i])
                                       }

                                       println("------------------------------------")
                                       listaTemporalFINALTIME.map{
                                           val hexString = it!!.substring(0, 8)
                                           println(" $it ${convertirHexAFecha(hexString)}")
                                       }
                                       callback.getInfo(listaTemporalFINALTIME as MutableList<String>?)
                                   }*/
                                in 1..Int.MAX_VALUE -> {
                                    Log.d(
                                        "FFFFFFFFFFFFFFFFF",
                                        "Hay $count elemento con EVENT_TYPE == \"04\"    ${registros[iC!!]}  ${
                                            convertirHexAFecha(registros[iC!!].substring(8, 16))
                                        }"
                                    )

                                    val targetTimestamp =
                                        registros[iC!!].substring(
                                            8,
                                            16
                                        )//"6467B000" // Valor objetivo


                                    var closestTimestamp: String? = null
                                    var smallestDifference: Long = Long.MAX_VALUE
                                    var closestIndex = -1

                                    for ((index, registro) in listFTIME!!.withIndex()) {
                                        val timestampDifference = abs(
                                            targetTimestamp.toLong(16) - registro.substring(0, 8)
                                                .toLong(16)
                                        )
                                        if (timestampDifference < smallestDifference) {
                                            smallestDifference = timestampDifference
                                            closestTimestamp = registro
                                            closestIndex = index
                                        }
                                    }

                                    //   val hexString2 =it.substring(8, 16)
                                    //           println("->S $it   ${convertirHexAFecha(hexString)}   ${convertirHexAFecha(hexString2)}")


                                    Log.d(
                                        "FFFFFFFFFFFFFFFFF",
                                        "El valor más cercano al timestamp objetivo ($targetTimestamp) ${
                                            convertirHexAFecha(targetTimestamp)
                                        } es: $closestTimestamp   ${
                                            convertirHexAFecha(
                                                closestTimestamp!!.substring(
                                                    0,
                                                    8
                                                )
                                            )
                                        }"
                                    )
                                    Log.d(
                                        "FFFFFFFFFFFFFFFFF",
                                        "Índice correspondiente: $closestIndex  ${listFTIME[closestIndex]}"
                                    )

                                    /*for(item in registrosTIMEPODUMMY ){

                                    }*/
                                    for ((index, registro) in listFTIME.withIndex()) {
                                        val hexString = registro.substring(
                                            0,
                                            8
                                        )  // valor hexadecimal que se desea convertir
                                        //   val hexString2 =it.substring(8, 16)
                                        Log.d(
                                            "MyAsyncTaskGeTIME",
                                            "->S $registro   ${convertirHexAFecha(hexString)}  $index "
                                        )
                                    }
                                    registros.map {
                                        val hexString =
                                            it.substring(
                                                0,
                                                8
                                            ) // valor hexadecimal que se desea convertir
                                        val hexString2 = it.substring(8, 16)
                                        Log.d(
                                            "FFFFFFFFFFFFFFFFF",
                                            "->S $it   ${convertirHexAFecha(hexString)}   ${
                                                convertirHexAFecha(hexString2)
                                            }"
                                        )
                                    }
                                    //  Log.d("CONTEOFINAL ","registrosRECIVRE ${registros.size}")
                                    val hexStringFINALTIME = listFTIME.last().substring(0, 8)
                                    Log.d(
                                        "FFFFFFFFFFFFFFFFF",
                                        "FINAL listFTIME.last()  ${listFTIME.last()} ${
                                            listFTIME.last().substring(0, 8)
                                        }"
                                    )

                                    for (i in closestIndex downTo 0) {
                                        val item = listFTIME[i]
                                        Log.d(
                                            "FFFFFFFFFFFFFFFFF",
                                            "${listFTIME[i]}   ${
                                                convertirHexAFecha(
                                                    listFTIME[i].substring(
                                                        0,
                                                        8
                                                    )
                                                )
                                            } "
                                        )
                                    }
                                    val currentTimeMillis = System.currentTimeMillis()
                                    val currentTimeHexMILLNOWMINUSDIF =
                                        toHexString(currentTimeMillis / 1000).uppercase()
                                    listaTemporalFINALTIME.add(
                                        "$currentTimeHexMILLNOWMINUSDIF${
                                            listFTIME.last().substring(8, listFTIME.last().length)
                                        }"
                                    )
                                    for (i in listFTIME.size - 2 downTo closestIndex + 1) {
                                        val item = listFTIME[i]
                                        Log.d(
                                            "FFFFFFFFFFFFFFFFF",
                                            " complemento ${listFTIME[i]}   ${
                                                convertirHexAFecha(
                                                    listFTIME[i].substring(
                                                        0,
                                                        8
                                                    )
                                                )
                                            } "
                                        )

                                        val fechaHora1 =
                                            Date(
                                                listFTIME.get(i)!!.substring(0, 8).toLong(16) * 1000
                                            )
                                        val fechaHora2 =
                                            Date(
                                                listFTIME[i - 1]!!.substring(0, 8).toLong(16) * 1000
                                            )

                                        val dif = (listFTIME?.get(i)!!.substring(0, 8)
                                            .toLong(16) * 1000) - (listFTIME[i - 1].substring(0, 8)
                                            .toLong(16) * 1000)
                                        // val difBACK = (listFTIME?.get(i-1)!!.substring(0, 8).toLong(16) * 1000) - (listFTIME[i-2].substring(0, 8).toLong(16) * 1000)
                                        //(listFTIME?.get(i+1)!!.substring(0, 8).toLong(16) * 1000) - (listFTIME[i].substring(0, 8).toLong(16) * 1000)
                                        val difBACK = kotlin.math.abs(
                                            (listFTIME?.get(i - 1)!!.substring(0, 8)
                                                .toLong(16) * 1000) - (listFTIME[i].substring(0, 8)
                                                .toLong(16) * 1000)
                                        )

                                        //     val difBACK = (listFTIME?.get(i+1)!!.substring(0, 8).toLong(16) * 1000) - (listFTIME[i].substring(0, 8).toLong(16) * 1000)

                                        Log.d(
                                            "FFFFFFFFFFFFFFFFF",
                                            "------------------------------------ difBACK $difBACK $i"
                                        )
                                        var difFINAL: Long = 0
                                        if (dif == difBACK) {
                                            difFINAL = 60000 //dif
                                        } else {
                                            difFINAL = 60000
                                        }
                                        val FINALDIF =
                                            (listaTemporalFINALTIME.last().substring(0, 8)
                                                .toLong(16) * 1000) - difFINAL
                                        val date = Date(FINALDIF)
                                        val fechaHoraExadecimal =
                                            BigInteger.valueOf(date.time / 1000).toString(16)
                                                .padStart(8, '0')
                                                .uppercase()
                                        listaTemporalFINALTIME.add(
                                            fechaHoraExadecimal + listFTIME[i].substring(
                                                8, listFTIME[i]!!.length
                                            )
                                        )

                                    }


                                    Log.d(
                                        "FFFFFFFFFFFFFFFFF",
                                        "------------------------------------ listFTIME ${listFTIME.size}  listaTemporalFINALTIME ${listaTemporalFINALTIME.size}"
                                    )


                                    for (i in closestIndex downTo 0) {
                                        val item = listFTIME[i]
                                        Log.d(
                                            "datosResultTIME->>>",
                                            "LLLLLLL ${listFTIME[i]}   ${
                                                convertirHexAFecha(
                                                    listFTIME[i].substring(
                                                        0,
                                                        8
                                                    )
                                                )
                                            } "
                                        )
                                        listaTemporalFINALTIME.add(listFTIME[i])
                                    }
                                    /*    listaTemporalFINALTIME.map {
                                            val hexString = it!!.substring(0, 8)
                                            println(" $it ${convertirHexAFecha(hexString)}")
                                        }*/
                                    Log.d(
                                        "CONTEOFINAL",
                                        "listFTIME ${listFTIME.size} listaTem ${listaTemporalFINALTIME.size}"
                                    )
                                    isTIMEOK.set(true)
                                    runOnUiThread {
                                        callback.getTime(
                                            agregar00AlPenultimoCaracter(
                                                listaTemporalFINALTIME,
                                                2
                                            )
                                        )
                                    }

                                }

                                /*                              2 -> {
                                                                  FINALLISTA.map {
                                                                      Log.d("FINALLISTA", "FINALLISTA ->>>>>>>> $it")
                                                                  }

                                                                  var listD: MutableList<String>? = ArrayList()
                                                                  for (item in FINALLISTA) {
                                                                      if (item!!.length >= 10) {
                                                                          listD!!.add(item!!)
                                                                      }
                                                                  }
                                                                  Log.d(
                                                                      "FFFFFFFFFFFFFFFFF",
                                                                      "Hay $count elemento con EVENT_TYPE == \"04\""
                                                                  )
                                                                  isTIMEOK.set(true)
                                                                  runOnUiThread {
                                                                      callback.getTime(listD)
                                                                  }

                                                              }
                              */
                                else -> {
                                    Log.d(
                                        "FFFFFFFFFFFFFFFFF",
                                        "Hay $count elemento con EVENT_TYPE == \"04\""
                                    )
                                    var listD: MutableList<String>? = ArrayList()
                                    for (item in FINALLISTA) {
                                        if (item!!.length >= 10) {
                                            listD!!.add(item!!)
                                        }
                                    }
                                    Log.d(
                                        "FFFFFFFFFFFFFFFFF",
                                        "Hay $count elemento con EVENT_TYPE == \"04\""
                                    )
                                    isTIMEOK.set(true)
                                    runOnUiThread {
                                        callback.getTime(agregar00AlPenultimoCaracter(listD, 2))
                                    }
                                }
                            }


                            var listD: MutableList<String>? = ArrayList()
                            /*   for (item in FINALLISTA) {
                                   if (item!!.length >= 10) {
                                       listD!!.add(item!!)
                                   }
                               }*/
                            val iterator = FINALLISTA.iterator()
                            while (iterator.hasNext()) {
                                val item = iterator.next()
                                if (item!!.length >= 10) {
                                    listD!!.add(item)
                                }
                            }

                            runOnUiThread {
                                callback.onProgress("Finalizado Tiempo")
                            }
                            // callback.onSuccess(true)
                        }
                    } else {
                        var listFTIME: MutableList<String>? = ArrayList()
                        /* este esta ok pero se modifica por el de abajo para evitar concurrencia
                         for (item in FINALLISTA) {
                             Log.d("FINALLISTA", "$item ")
                             if (item!!.length >= 10) {
                                 listFTIME!!.add(item!!)
                             }
                         }*/
                        val iterator = FINALLISTA.iterator()
                        while (iterator.hasNext()) {
                            val item = iterator.next()
                            if (item!!.length >= 10) {
                                listFTIME!!.add(item)
                            }
                        }
                        if (listFTIME!!.isEmpty()) {
                            runOnUiThread {
                                callback.onProgress("Finalizado Tiempo")
                                callback.onError("Lista vacia")
                            }
                        } else {

                            isTIMEOK.set(true)
                            runOnUiThread {
                                callback.getTime(
                                    agregar00AlPenultimoCaracter(
                                        listFTIME as MutableList<String>,
                                        2
                                    )
                                )
                                callback.onProgress("Finalizado Tiempo")
                            }
                        }
                        // callback.onProgress("Finalizado Tiempo")
                        //callback.onError("Lista vacia")
                        //  callback.onSuccess(false)
                    }
                    //   }catch (Excep : Exception){}

                } else {
                    runOnUiThread {
                        callback.onProgress("Finalizado Tiempo")
                        callback.onError("desconectado")
                    }
                    // callback.onSuccess(false)
                }


                /***************************************************COMIENZA LA FUNCION DE EVENTO*******************************************/

                clearLogger()
                runOnUiThread {
                    callback.onProgress("Iniciando Evento")
                }
                BanderaEvento = true
                var FINALLISTA: MutableList<String?> = ArrayList()

                var listDataGEvent: MutableList<String>? = ArrayList()
                runOnUiThread {
                    callback.onProgress("Realizando Evento")
                }
                if (ValidaConexion()) {
                    bluetoothLeService = bluetoothServices.bluetoothLeService()
                    Thread.sleep(500)
                    bluetoothServices.sendCommand("handshake", "4021")
                    Thread.sleep(500)

                }

                try {


                    if (ValidaConexion()) {
                        try {
                            FinalListDataFinalE.clear()
                            // Log.d("getInfoListFinal", "${getInfoList().toString()}")
                            bluetoothLeService = bluetoothServices.bluetoothLeService()
                            Thread.sleep(500)
                            bluetoothServices.sendCommand("handshake", "4021")
                            Thread.sleep(500)
                            runOnUiThread {
                                clearLogger()
                                //  getInfoList()!!.clear()
                            }

                            bluetoothServices.sendCommand("event", "4061")
                            Thread.sleep(700)
                            do {
                                //  listDataGT!!.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                                /*  Log.d(
                                    "islistttt2",
                                    ": ${bluetoothLeService?.dataFromBroadcastUpdateString}  "
                                )
                                */
                                Thread.sleep(700)

                            } while (bluetoothLeService?.dataFromBroadcastUpdateString?.isNotEmpty() == true)

                            Thread.sleep(700)
                            runOnUiThread {

                                FINALLISTA = VEvent(
                                    getInfoList() as MutableList<String?>
                                )
                            }

                            delay(500)
                            var TIMEUNIX: String? = null
                            var listTIMEUNIX: MutableList<String>? = ArrayList()
                            val SALIDAEVENTOREGISTROS: MutableList<String?> = ArrayList()
                            for (item in FINALLISTA) {
                                if (item != null) {
                                    if (item.length > 18) {
                                        SALIDAEVENTOREGISTROS.add(item)
                                        Log.d(
                                            "FINALLISTA",
                                            "metodoquerecorre FINALLISTA ${FINALLISTA}"
                                        )
                                    }
                                }
                            }
                            Log.d(
                                "funcionUltime",
                                "SALIDAEVENTOREGISTROS size ${SALIDAEVENTOREGISTROS}"
                            )
                            if (SALIDAEVENTOREGISTROS.isNotEmpty()) {
                                Log.d("funcionUltime", "SALIDAEVENTOREGISTROS.isNotEmpty()")
                                /*******************************CONTEO DE EVENTO 04 ***********************************/
                                val indexCount: MutableList<Int> = ArrayList()
                                var count = 0
                                for ((index, item) in SALIDAEVENTOREGISTROS.withIndex()) {
                                    val EVENT_TYPE = item?.substring(16, 18)
                                    if (EVENT_TYPE == "04") {
                                        count++
                                        println("-> $item  $count index $index  ")
                                        indexCount.add(index)
                                    }
                                }


                                Log.d("funcionUltime", "count size ${count}")
                                when (count) {
                                    0 -> {
                                        isEVENTOK.set(true)
                                        SALIDAEVENTOREGISTROS?.let {
                                            runOnUiThread {
                                                callback.getEvent(
                                                    agregar00AlPenultimoCaracter(
                                                        it as MutableList<String>,
                                                        2
                                                    )
                                                )
                                                callback.onProgress("Finalizado Evento")
                                            }
                                            //   callback.onSuccess(true)
                                        }
                                        //  callback.getInfo(SALIDAEVENTOREGISTROS as MutableList<String>)
                                    }

                                    else -> {

                                        try {
                                            val iC = indexCount.maxOrNull()
                                            val HEXAStar =
                                                SALIDAEVENTOREGISTROS[iC!!]?.substring(0, 8)
                                            val HEXAEnd =
                                                SALIDAEVENTOREGISTROS[iC!!]?.substring(8, 16)
                                            val HEXAEndMIlisegundosEventCorte =
                                                HEXAEnd?.toLong(16)?.times(1000)
                                            Log.d(
                                                "funcionUltime",
                                                "iC $iC  ${SALIDAEVENTOREGISTROS[iC!!]}"
                                            )
                                            /******************************************************************/
                                            var SalidaEvent: MutableList<String?> = ArrayList()
                                            SALIDAEVENTOREGISTROS.map {
                                                val hexString =
                                                    it!!.substring(
                                                        0,
                                                        8
                                                    ) // valor hexadecimal que se desea convertir
                                                val hexString2 = it.substring(8, 16)
                                                val star = convertirHexAFecha(hexString)
                                                val end = convertirHexAFecha(hexString2)
                                                Log.d(
                                                    "funcionUltime",
                                                    " $it inicio  $star \n final $end"
                                                )
                                                println(" SALIDAEVENTOREGISTROS $it inicio  $star \n final $end")
                                            }
                                            bluetoothLeService =
                                                bluetoothServices.bluetoothLeService()
                                            bluetoothLeService?.sendFirstComando("405B")
                                            listData.clear()
                                            Thread.sleep(700)
                                            for (num in 0..1) {
                                                listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                                                Log.d("islistttt2", ":" + listData[num])
                                                Thread.sleep(700)
                                            }
                                            val s = listData[0]
                                            val sSinEspacios = s?.replace(" ", "")
                                            var TIMEUNIX: String? = null

                                            listData?.let {
                                                if (sSinEspacios?.length!! >= 24) TIMEUNIX =
                                                    sSinEspacios?.substring(16, 24) else TIMEUNIX =
                                                    "612F6B47"
                                                var listTIMEUNIX: MutableList<String>? = ArrayList()
                                                TIMEUNIX?.let {
                                                    listTIMEUNIX?.add(it)
                                                };"612F6B47"
                                            }

                                            val TimeStampControl = TIMEUNIX
                                            val TimeStampActual = GetNowDateExa()
                                            val TimeStampControlMIlisegundos =
                                                TimeStampControl?.toLong(16)?.times(1000)
                                            val currentTimeHexTimeStampControl =
                                                toHexString(
                                                    TimeStampControlMIlisegundos?.div(1000) ?: 0
                                                )
                                            val TimeStampActualMIlisegundos =
                                                TimeStampActual.toLong(16) * 1000
                                            val currentTimeHexTimeStampActual =
                                                toHexString(TimeStampActualMIlisegundos / 1000)
                                            val registrosLastMIlisegundos =
                                                SALIDAEVENTOREGISTROS.last()?.substring(8, 16)
                                                    ?.toLong(16)?.times(1000)
                                            val currentTimeHexregistrosLastMIlisegundos =
                                                toHexString(
                                                    registrosLastMIlisegundos?.div(1000) ?: 0
                                                )
                                            val diferenciaTIMESTAMPActualvsControl =
                                                TimeStampActualMIlisegundos - registrosLastMIlisegundos!!
                                            val currentTimeHexDiferenciaACTUALvsControl =
                                                toHexString((TimeStampActualMIlisegundos) / 1000)
                                            Log.d(
                                                "funcionUltime",
                                                "TIEMPOS s ${s?.get(0)} sSinEspacios $sSinEspacios  TIMEUNIX $TIMEUNIX TimeStampActual $TimeStampActual"
                                            )
                                            /*****************************************CICLO PARA ESTAMPAR DESPUES DEL ULTIMO CORTE DE LUZ************************************************/
                                            for (i in SALIDAEVENTOREGISTROS.size - 1 downTo iC) {

                                                if (SALIDAEVENTOREGISTROS[i]?.substring(16, 18)
                                                        .equals("04")
                                                ) {
                                                    val HEXAStarCiclo =
                                                        SALIDAEVENTOREGISTROS[i]!!.substring(0, 8)
                                                    val HEXAEndCiclo =
                                                        SALIDAEVENTOREGISTROS[i]!!.substring(8, 16)
                                                    val HEXAEndCicloMENOSHEXAStarCiclo =
                                                        (HEXAEndCiclo.toLong(16) * 1000) - (HEXAStarCiclo.toLong(
                                                            16
                                                        ) * 1000)
                                                    val HEXAEndMIlisegundos =
                                                        HEXAEndCiclo.toLong(16) * 1000
                                                    val Dif = TimeStampControlMIlisegundos?.minus(
                                                        HEXAEndMIlisegundos
                                                    )
                                                    val DiferenciaNowvsDif =
                                                        TimeStampActualMIlisegundos - Dif!!
                                                    val DiferenciaFINALstar =
                                                        DiferenciaNowvsDif - HEXAEndCicloMENOSHEXAStarCiclo
                                                    val exaFinalEnd =
                                                        toHexString(DiferenciaNowvsDif / 1000)
                                                    val exaFinalStar =
                                                        toHexString(DiferenciaFINALstar / 1000)
                                                    val s = "${
                                                        SALIDAEVENTOREGISTROS[i]?.substring(
                                                            0,
                                                            8
                                                        )
                                                    }$exaFinalEnd${
                                                        SALIDAEVENTOREGISTROS[i]!!.substring(16)
                                                    }"
                                                    SalidaEvent.add(s)
                                                    Log.d(
                                                        "Ciclo04",
                                                        "ciclo ${SALIDAEVENTOREGISTROS[i]} $Dif   $DiferenciaNowvsDif   star $${
                                                            convertirHexAFecha(
                                                                exaFinalStar
                                                            )
                                                        }   end ${convertirHexAFecha(exaFinalEnd)} $s ${s.length} , $i  "
                                                    )
                                                } else {
                                                    val HEXAStarCiclo =
                                                        SALIDAEVENTOREGISTROS[i]!!.substring(0, 8)
                                                    val HEXAEndCiclo =
                                                        SALIDAEVENTOREGISTROS[i]!!.substring(8, 16)
                                                    val HEXAEndCicloMENOSHEXAStarCiclo =
                                                        (HEXAEndCiclo.toLong(16) * 1000) - (HEXAStarCiclo.toLong(
                                                            16
                                                        ) * 1000)
                                                    val HEXAEndMIlisegundos =
                                                        HEXAEndCiclo.toLong(16) * 1000
                                                    val Dif = TimeStampControlMIlisegundos?.minus(
                                                        HEXAEndMIlisegundos
                                                    )
                                                    val DiferenciaNowvsDif =
                                                        TimeStampActualMIlisegundos - Dif!!
                                                    val DiferenciaFINALstar =
                                                        DiferenciaNowvsDif - HEXAEndCicloMENOSHEXAStarCiclo
                                                    val exaFinalEnd =
                                                        toHexString(DiferenciaNowvsDif / 1000)
                                                    val exaFinalStar =
                                                        toHexString(DiferenciaFINALstar / 1000)
                                                    val s = "$exaFinalStar$exaFinalEnd${
                                                        SALIDAEVENTOREGISTROS[i]!!.substring(16)
                                                    }"
                                                    SalidaEvent.add(s)
                                                    Log.d(
                                                        " ",
                                                        "ciclo ${SALIDAEVENTOREGISTROS[i]} $Dif   $DiferenciaNowvsDif   star $${
                                                            convertirHexAFecha(
                                                                exaFinalStar
                                                            )
                                                        }   end ${convertirHexAFecha(exaFinalEnd)} $s ${s.length} , $i  "
                                                    )
                                                }

                                            }

                                            /*     for (i in iC downTo iC) {
                                                     val HEXAStarCiclo =
                                                         SALIDAEVENTOREGISTROS[i]!!.substring(0, 8)
                                                     val HEXAEndCiclo =
                                                         SALIDAEVENTOREGISTROS[i]!!.substring(8, 16)
                                                     val HEXAEndCicloMENOSHEXAStarCiclo =
                                                         (HEXAEndCiclo.toLong(16) * 1000) - (HEXAStarCiclo.toLong(
                                                             16
                                                         ) * 1000)
                                                     val HEXAEndMIlisegundos =
                                                         HEXAEndCiclo.toLong(16) * 1000
                                                     val Dif = TimeStampControlMIlisegundos?.minus(
                                                         HEXAEndMIlisegundos
                                                     )
                                                     val DiferenciaNowvsDif =
                                                         TimeStampActualMIlisegundos - 0
                                                     val DiferenciaFINALstar =
                                                         DiferenciaNowvsDif - HEXAEndCicloMENOSHEXAStarCiclo
                                                     val exaFinalEnd =  //SALIDAEVENTOREGISTROS[i]?.substring(8,16)
                                                          toHexString(DiferenciaFINALstar / 1000)
                                                    //     toHexString(DiferenciaNowvsDif / 1000)
                                                     val exaFinalStar = SALIDAEVENTOREGISTROS[i]?.substring(8,16)  //SALIDAEVENTOREGISTROS[i]?.substring(0,8)
                                                     //   toHexString(DiferenciaFINALstar / 1000)

                                                     ////Dado que se modifico el evento 4 y se respet
                                                     val s = "$exaFinalStar$exaFinalEnd${
                                                         SALIDAEVENTOREGISTROS[i]!!.substring(16)
                                                     }"
                                                     SalidaEvent.add(s)
                                                     Log.d("Ciclo04",
                                                         "ciclo ${SALIDAEVENTOREGISTROS[i]} $Dif   $DiferenciaNowvsDif   star $${
                                                             //convertirHexAFecha(
                                                                 exaFinalStar
                                                        //     )
                                                         }   end ${exaFinalEnd} $s ${s.length} , $i  ${SALIDAEVENTOREGISTROS[i]?.substring(16,18)}  "
                                                     )

                                                     //  SalidaEvent.add(SALIDAEVENTOREGISTROS[i])
                                                 }*/
                                            for (i in iC - 1 downTo 0) {
                                                ////////////////Esta parte del codigo pasa los registros del logger del ultimo evento 04 al evento mas antiguo
                                                //    SalidaEvent.add(SALIDAEVENTOREGISTROS[i])
                                            }
                                            SalidaEvent.map {
                                                val hexString = it?.substring(
                                                    0,
                                                    8
                                                ) // valor hexadecimal que se desea convertir
                                                val hexString2 = it?.substring(8, 16)
                                                val star =
                                                    hexString?.let { it1 -> convertirHexAFecha(it1) }
                                                val end =
                                                    hexString2?.let { it1 -> convertirHexAFecha(it1) }
                                                println("$it   star $star end $end ")
                                            }
                                            //  callback.onSuccess(true)
                                            isEVENTOK.set(true)

                                            ////////////////Esta parte del codigo pasa los registros del logger del ultimo evento 04 al evento mas antiguo
                                            var tempEvent = SalidaEvent

                                            runOnUiThread {
                                                callback.getEvent(
                                                    agregar00AlPenultimoCaracter(
                                                        SalidaEvent as MutableList<String>?,
                                                        2
                                                    )
                                                )
                                            }
                                        } catch (exc: Exception) {
                                            runOnUiThread {
                                                callback.onError(exc.toString())
                                                // callback.onSuccess(false)
                                                callback.onProgress("Finalizado Evento")
                                            }
                                        }
                                    }
                                }

                                // callback.onSuccess(true)

                                //   callback.getInfo(SALIDAEVENTOREGISTROS as MutableList<String>?)
                            }
                            /*
                          if (FINALLISTA.isNotEmpty()) {
                              getInfoList()!!.clear()
                              bluetoothLeService!!.sendFirstComando("405B")
                              Thread.sleep(700)
                              listData.clear()
                              listData.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                              val s = listData[0]
                              val sSinEspacios = s?.replace(" ", "")
                              var TIMEUNIX: String? = null
                              if (sSinEspacios?.length!! >= 24) TIMEUNIX =
                                  sSinEspacios?.substring(16, 24) else TIMEUNIX = "612F6B47"
                              var listTIMEUNIX: MutableList<String>? = ArrayList()
                              TIMEUNIX?.let {
                                  listTIMEUNIX?.add(it)
                              };"612F6B47"
                              val TIMEUNIX2022 = "62840A56"//"75840A56"// "62840A56"
                              Log.d("FINALLISTA", "DATOS DEL TIME  $TIMEUNIX")

                              var registros = mutableListOf<String>()
                              FINALLISTA?.map {
                                  if (it!!.length > 18) {
                                      registros.add(it)
                                  }
                              }
                              registros?.map {
                                  Log.d("HHHHHHH", "$it")
                                  val hexString =
                                      it.substring(0, 8) // valor hexadecimal que se desea convertir
                                  val hexString2 = it.substring(8, 16)
                                  Log.d(
                                      "FFFFFFFG",
                                      "$it   ${convertirHexAFecha(hexString)}   ${
                                          convertirHexAFecha(hexString2)
                                      } "
                                  )
                              }
                             var  registrosx =
                                  LOGGEREVENTZ(registros as MutableList<String?>) /// LOGGEREVENT(registros as MutableList<String?>) //mutableListOf<String>()
                              var SalidaEvent: MutableList<String?> = ArrayList()
                              val TimeStampControl = TIMEUNIX
                              val TimeStampActual = GetNowDateExa()
                              val TimeStampControlMIlisegundos = TimeStampControl!!.toLong(16) * 1000
                              val currentTimeHexTimeStampControl =
                                  toHexString(TimeStampControlMIlisegundos / 1000)

                              val TimeStampActualMIlisegundos = TimeStampActual.toLong(16) * 1000
                              val currentTimeHexTimeStampActual =
                                  toHexString(TimeStampActualMIlisegundos / 1000)


                              val registrosLastMIlisegundos =
                                  registrosx.last().substring(8, 16).toLong(16) * 1000
                              val currentTimeHexregistrosLastMIlisegundos =
                                  toHexString(registrosLastMIlisegundos / 1000)

                              val diferenciaTIMESTAMPActualvsControl =
                                  TimeStampActualMIlisegundos - registrosLastMIlisegundos
                              val currentTimeHexDiferenciaACTUALvsControl =
                                  toHexString((TimeStampActualMIlisegundos) / 1000)


                              val indexCount: MutableList<Int> = ArrayList()
                              var count = 0
                              for ((index, item) in registrosx.withIndex()) {
                                  val EVENT_TYPE = item!!.substring(16, 18)
                                  if (EVENT_TYPE == "04") {
                                      count++
                                      println("-> $item  $count index $index  ")
                                      indexCount.add(index!!)
                                  }
                              }

                              val iC = indexCount.last()
                              val HEXAStar = registrosx[iC].substring(0, 8)
                              val HEXAEnd = registrosx[iC].substring(8, 16)
                              val HEXAEndMIlisegundosEventCorte = HEXAEnd.toLong(16) * 1000

                              for (i in registrosx.size - 1 downTo indexCount.max() + 1) {
                                  val HEXAStarCiclo = registrosx[i].substring(0, 8)
                                  val HEXAEndCiclo = registrosx[i].substring(8, 16)

                                  val HEXAEndCicloMENOSHEXAStarCiclo =
                                      (HEXAEndCiclo.toLong(16) * 1000) - (HEXAStarCiclo.toLong(16) * 1000)

                                  val HEXAEndMIlisegundos = HEXAEndCiclo.toLong(16) * 1000
                                  val Dif = TimeStampControlMIlisegundos - HEXAEndMIlisegundos
                                  val DiferenciaNowvsDif = TimeStampActualMIlisegundos - Dif
                                  val DiferenciaFINALstar =
                                      DiferenciaNowvsDif - HEXAEndCicloMENOSHEXAStarCiclo
                                  val exaFinalEnd = toHexString(DiferenciaNowvsDif / 1000)
                                  val exaFinalStar = toHexString(DiferenciaFINALstar / 1000)
                                  val s = "$exaFinalStar$exaFinalEnd${registrosx[i].substring(16)}"
                                  SalidaEvent.add(s)
                                  println(
                                      "ciclo ${registrosx[i]} $Dif   $DiferenciaNowvsDif   star $${
                                          convertirHexAFecha(
                                              exaFinalStar
                                          )
                                      }   end ${convertirHexAFecha(exaFinalEnd)} $s ${s.length} "
                                  )
                              }
                              for (i in indexCount.max() downTo 0) {
                                  SalidaEvent.add(registrosx[i])
                              }


                              SalidaEvent.map {
                                  val hexString =
                                      it!!.substring(0, 8) // valor hexadecimal que se desea convertir
                                  val hexString2 = it!!.substring(8, 16)
                                  val star = convertirHexAFecha(hexString)
                                  val end = convertirHexAFecha(hexString2)
                                  println("$it   star $star end $end ")
                                  Log.d("SalidaFInalEventoPrueba", "$it   star $star end $end ")
                              }
                              callback.getInfo(SalidaEvent as MutableList<String>?)
                          }*/
                            else {
                                // callback.onSuccess(false)
                                Log.d("funcionUltime", "Lista vacia Evento")
                                callback.onError("Lista vacia Evento")

                            }
                        } catch (ex: Exception) {
                            runOnUiThread {
                                callback.onError(ex.toString())
                            }
                        }
                        runOnUiThread { callback.onProgress("Finalizado Evento") }

                    } else {
                        runOnUiThread {
                            callback.onError("Desconectado")
                            callback.onProgress("Finalizado Evento")
                        }
                    }
                } catch (Exce: Exception) {
                    runOnUiThread {
                        callback.onError("error ${Exce.toString()}")
                        callback.onProgress("Finalizado Evento")
                    }
                }



                Thread.sleep(2000)

                /******************************************************* Iniciando Limpieza del logger *****************************************************/

                var listDataCleanLogger: MutableList<String>? = ArrayList()
                val s = sp?.getBoolean("isconnected", false)
                //  callback.onProgress("Realizando")
                Log.d(
                    "SALIDAfinalqqqaaaaaaaaaa",
                    "------------------------------------------------------------ Iniciando Limpieza del logger  S $s"
                )
                if (s == true) {

                    bluetoothLeService = bluetoothServices.bluetoothLeService()
                    Thread.sleep(500)
                    bluetoothServices.sendCommand("handshake", "4021")
                    Thread.sleep(500)
                    for (i in 1..2) {
                        Thread.sleep(500)
                        bluetoothLeService?.dataFromBroadcastUpdateString?.let {
                            listDataCleanLogger!!.add(
                                it
                            )
                        }
                    }
                }
                Thread.sleep(500)

                if (listDataCleanLogger.isNullOrEmpty()) {
                    val s = sp?.getBoolean("isconnected", false)
                    if (s == false) {
                        callback.onError("Desconectado")
                    } else
                        callback.onError("Desconectado")
                } else {
                    val s = sp?.getBoolean("isconnected", false)

                    if (s == true) {
                        Thread.sleep(1000)

                        callback.onProgress("Iniciando Actualizacion del TIMESTAMP")

                        var HourNow: String? = null
                        callback.onProgress("Realizando Actualizacion del TIMESTAMP")
                        var ListTIMEResult: MutableList<String?> = ArrayList()

                        val s = ValidaConexion()//sp?.getBoolean("isconnected", false)
                        if (s) {

                            try {
                                runOnUiThread {
                                    clearLogger()
                                    //  getInfoList()!!.clear()
                                }
                                //   getInfoList()?.clear()
                                listData.clear()
                                bluetoothLeService = bluetoothServices.bluetoothLeService()
                                bluetoothLeService?.sendFirstComando("4021")
                                Thread.sleep(500)
                                HourNow = GetNowDateExa()

                                var Command: String? = null
                                HourNow?.let {
                                    var CHECKSUMGEO = mutableListOf<String>()
                                    CHECKSUMGEO.add("4058")
                                    CHECKSUMGEO.add(HourNow.uppercase())
                                    CHECKSUMGEO.add(sumarParesHexadecimales(CHECKSUMGEO))
                                    val result = CHECKSUMGEO.joinToString("")



                                    result?.let {
                                        if (bluetoothLeService?.sendFirstComando(it) == true) {

                                            Thread.sleep(450)
                                            listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                                            Thread.sleep(450)

                                            if (getInfoList()?.last()?.equals("F13D") == true) {
                                                ListTIMEResult.add("F1 3D")
                                            } else {
                                                callback.onError("No se pudo Actualizar el TIMESTAMP")
                                                ListTIMEResult.add("F1 3E")
                                            }
                                        } else Log.d("", "dataChecksum total:8")
                                    }
                                    ListTIMEResult.add("Sin hora")

                                } ?: "sin hora"


                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                                ListTIMEResult.add("exception")
                            }
                        }

                        when (ListTIMEResult[0].toString()) {

                            "F1 3D" -> {
                                //62840A56 -> Your time zone: martes, 17 de mayo de 2022 15:49:26 GMT-05:00
                                bluetoothLeService!!.sendFirstComando("405B")
                                Thread.sleep(700)
                                listData.clear()
                                listData.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                                val s = listData[0]
                                val sSinEspacios = s?.replace(" ", "")
                                val TIMEUNIX = sSinEspacios?.substring(16, 24)
                                var listTIMEUNIX: MutableList<String>? = ArrayList()
                                TIMEUNIX?.let {
                                    listTIMEUNIX?.add(it)
                                }

                                // callback.getInfo(listTIMEUNIX)
                                //   callback.onSuccess(true)
                                callback.onProgress("Finalizando Actualizacion del TIMESTAMP")
                            }

                            "F1 3E", "not" -> {

                                //   callback.onSuccess(false)
                                callback.onError("No se pudo Actualizar el TIMESTAMP")
                            }

                            "DESCONECTADO" -> {
                                //  callback.onSuccess(false)
                                callback.onError("DESCONECTADO")
                            }

                        }
                        //  val handler = Handler()
                        // handler.postDelayed({


                        // callback.onSuccess(true)

                        callback.onProgress("Iniciando Limpieza del logger")
                        bluetoothLeService = bluetoothServices.bluetoothLeService()
                        callback.onProgress("Realizando Limpieza del logger")
                        Log.d("reset de memoria", "${isEVENTOK.get()}  ${isTIMEOK.get()}")
                        if (/*isEVENTOK.get() && isTIMEOK.get()*/true) {
                            Thread.sleep(1000)
                            if (bluetoothLeService!!.sendFirstComando("4054")) { //reset de memoria 0x4054
                                Thread.sleep(200)
                                Log.d("reset de memoria", "dataChecksum total:7")
                                callback.onProgress("Finalizando Limpieza del logger")
                                delay(1000)

                            } else {
                                Log.d("reset de memoria", "dataChecksum total:8")
                                callback.onError("NoResetMemory")
                            }

                            callback.onProgress("Finalizando Logger")
                            callback.onSuccess(true)
                        } else {
                            // Log,d("","")
                            callback.onProgress("Finalizando Logger!!")
                            //  callback.onProgress("Se ha limpiado el logger")
                            callback.onSuccess(true)
                        }

                        //     desconectar()
                        //  }, 5000)
                    }
                }


            }

        }
    }

    inner class ObtenerLoggerVersion2(
        callback2: CallbackLoggerVersionCrudo
    ) :
        AsyncTask<Int?, Int?, String>() {
        var callback = callback2

        var Event = mutableListOf<String?>()
        var Time = mutableListOf<String?>()
        var registrosTime = mutableListOf<String?>()
        var registrosEvent = mutableListOf<String?>()
        var ListaTimeFinal = mutableListOf<String>()
        var ListapruebaEvent = mutableListOf<String>()

        protected override fun doInBackground(vararg params: Int?): String? {

            ListapruebaTime.clear()
            ListapruebaEvent.clear()

            if (ValidaConexion()) {

                bluetoothLeService!!.sendComando("4021")
                Thread.sleep(800)
                Log.d("ObtenerLoggerPrueba", "getinfolist ${getInfoList()}")
                clearLogger()
                bluetoothLeService!!.sendComando("4060")

               for ( i in 1..45){
                    Thread.sleep(800)
                    bluetoothServices.bluetoothLeService?.dataFromBroadcastUpdateString?.let {
                        ListapruebaTime.add(
                            it
                        )
                    }
                }


            //    ListapruebaTime = getInfoList() as MutableList<String>


                if (!ListapruebaTime.isNullOrEmpty()) {

                    ListapruebaTime.map {
                        Log.d("datosCrudosTiempoSALidaFinal", "\n$it")
                    }


                    //LA FUNCION VTIME SEPARA LOS DATOS EN EL HEADER Y LOS DATOS EN BRUTO
                    if (ListapruebaTime.isNotEmpty()) {
                        Time =  VTIME(ListapruebaTime as MutableList<String?>)
                    }

                    Thread.sleep(100)
                    Time.map {
                        if (it!!.length > 15) {
                            Log.d(
                                "ObtenerLoggerPrueba",
                                "Traducido Time ${it}  ${convertirHexAFecha(it.substring(0, 8))}"
                            )
                        }

                    }
                } else {
                    Log.d("ObtenerLoggerPrueba", "lista vacia ")
                }

                clearLogger()
                bluetoothServices.sendCommand("4021")
                Thread.sleep(800)
                clearLogger()
                bluetoothServices.sendCommand("event", "4061")
                Thread.sleep(40000)
                ListapruebaEvent = getInfoList() as MutableList<String>
                if (!ListapruebaEvent.isNullOrEmpty()) {
                    ListapruebaEvent.map {
                        Log.d("datosdeEvento", " Event ${it}")
                    }
                    //LA FUNCION VEvent SEPARA LOS DATOS EN EL HEADER Y LOS DATOS EN BRUTO
                    Event = VEvent(ListapruebaEvent as MutableList<String?>)
                    Thread.sleep(100)
                    Event.map {
                        Log.d("DatosCRUDOSEVENT", "Traducido Event ${it}")
                    }
                } else {
                    Log.d("ObtenerLoggerPrueba", "lista Event vacia ")
                }

                /////////SOLO se agregan los registros del logger sin el header
                if (!Time.isNullOrEmpty()) {
                    for (item in Time) {
                        if (item!!.length > 15) {
                            registrosTime.add(item)
                        }
                    }
                }
                if (!Event.isNullOrEmpty()) {
                    for (item in Event) {
                        if (item!!.length > 15) {
                            registrosEvent.add(item)
                        }
                    }
                }

                val MapaControl = mutableListOf<Int>()
                for (index in 0 until registrosEvent.size) {
                    val HEXAEnd = registrosEvent[index]?.substring(8, 16)
                    // println("HEXAEnd $HEXAEnd  lista ${lista.get(index)}   HEXAEnd ${HEXAEnd}  index $index lista.size ${lista.size} ")
                    if (index < registrosEvent.size - 1) {
                        val HEXAEndplus = registrosEvent[index + 1]?.substring(8, 16)
                        var dif = (HEXAEndplus!!.toLong(16)?.times(1000) ?: 0).minus(
                            HEXAEnd!!.toLong(16)?.times(1000) ?: 0
                        )
                        //   println("HEXAEndplus $HEXAEndplus  index $index index+1 ${index+1}  dif $dif")
                        if (dif in 0..1999) {
                            //  println("entro en menos de 2000")
                            MapaControl.add(index + 1)
                        }
                    }
                }

                val MapaControlOrdenado = MapaControl.sortedDescending()

                // Eliminar elementos de la lista en los índices especificados
                for (indice in MapaControlOrdenado) {
                    if (indice >= 0 && indice < registrosEvent.size) {
                        registrosEvent.removeAt(indice)
                    }
                }
                Log.d(
                    "ProcesoEvent",
                    "-------------------------------------------------------------------\n"
                )
                registrosEvent.map {
                    Log.d(
                        "ProcesoEvent", "despues de quitar los 04  $it"
                    )
                }

                //    registrosEvent.add("65BA8E1265BA8E12C2FE34FE3485")
                //////////////////////// SE BUSCA LOS REGISTROS DE EVENTO 04
                val indexCount: MutableList<Int> = ArrayList()
                var count = 0
                for ((index, item) in registrosEvent.withIndex()) {
                    val EVENT_TYPE = item?.substring(16, 18)
                    if (EVENT_TYPE == "04") {
                        count++
                        Log.d(
                            "ProcesoEvent",
                            "obtener indice de event 04 -> $item  $count index $index  "
                        )
                        indexCount.add(index)
                    }
                }

                clearLogger()
                //////////////////// SE OBTIENE EL TIMESTAMP DEL CONTROL
                bluetoothLeService?.sendFirstComando("405B")
                listData.clear()
                Thread.sleep(1000)

                listData = getInfoList() as MutableList<String?>
                Log.d("islistttt2", "405B :" + listData)
                // Thread.sleep(900)
                var sdf = cleanSpace(listData)
                var TimeStampMilisegundos =
                    /*listData[0]?*/
                    sdf.toString().replace(" ", "")!!.substring(16, 24).toLong(16).times(1000)


                Log.d(
                    "depuracioinTime",
                    "TimeStamp $TimeStampMilisegundos   ${
                        listData[0]?.replace(" ", "")!!.substring(16, 24)
                    }"
                )


                /////// SE OBTIENE EL ULTIMO EVENTO 04
                val iC = indexCount.maxOrNull()

                ////////////////////////////////////////////////// COMIENZA EL PROCESO DE TIEMPO///////////////////////////////////////
                callback.onProgress("Realizando Tiempo")


                ////////////////////////SE BUSCARA LOS CASOS DE EXISTIR UN EVENTO  04

                if (!registrosTime.isNullOrEmpty()) {
                    when (count) {
                        /////// SI NO HAY EVENTO 04 SOLO SE SINCRONIZARA LOS TIEMPOS EN CASO DE EXISTIR UNA DIFERENCIA DEL TIMESTAMP CONTROL VS EL TIMESTAMP ACTUAL (REAL)
                        0 -> {

                            registrosTime.map {
                                var TIMECONTROLMIL =
                                    it?.replace(" ", "")!!.substring(0, 8).replace(" ", "")
                                        .toLong(16) * 1000
                                var GetNowLMIL = GetNowDateExa().toLong(16) * 1000
                                var dif = TIMECONTROLMIL + (GetNowLMIL - TimeStampMilisegundos)
                                var Exaprueba1 = toHexString(dif / 1000)
                                var RegistroTimeFinal = Exaprueba1 + it.substring(8, it.length)
                                ListaTimeFinal.add(RegistroTimeFinal)
                                Log.d(
                                    "depuracioinTime",
                                    " TIMECONTROLMIL $TIMECONTROLMIL  GetNowLMIL $GetNowLMIL  timecontrol ${
                                        it?.replace(
                                            " ",
                                            ""
                                        )!!.substring(0, 8)
                                    } GetNowLMIL ${GetNowDateExa()} "
                                )
                            }
                        }

                        else -> {

                            /////OBTENGO EL ULTIMO EVENTO 04
                            val targetTimestamp =
                                registrosEvent[iC!!]!!.substring(
                                    8,
                                    16
                                )
                            //////////////////// SE OBTIENE EL REGISTRO DE TIME MAS CERCANO AL EVENTO 04

                            var closestTimestamp: String? = null
                            var smallestDifference: Long = Long.MAX_VALUE
                            var closestIndex = -1

                            for ((index, registro) in registrosTime!!.withIndex()) {
                                val timestampDifference = abs(
                                    targetTimestamp.toLong(16) - registro!!.substring(0, 8)
                                        .toLong(16)
                                )
                                if (timestampDifference < smallestDifference) {
                                    smallestDifference = timestampDifference
                                    closestTimestamp = registro
                                    closestIndex = index
                                }
                            }

                            for (i in registrosTime.size - 1 downTo closestIndex + 1) {   // for (i in ListapruebaTime.size - 1 downTo closestIndex +1  ) {
                                val item = registrosTime[i]


                                var TIMESTAMP = listData[0]?.replace(" ", "")!!.substring(16, 24)
                                    .replace(" ", "")
                                var TIMECONTROLMIL =
                                    listData[0]?.replace(" ", "")!!.substring(16, 24)
                                        .replace(" ", "").toLong(16) * 1000
                                var GetNowLMIL = GetNowDateExa().toLong(16) * 1000
                                var regTimemil = item!!.substring(0, 8).toLong(16) * 1000

                                var s = convertHexToHumanDate(item.substring(0, 8))
                                var prueba1 =
                                    GetNowLMIL - (TIMECONTROLMIL - regTimemil) //regTimemil - (GetNowLMIL - TIMECONTROLMIL)
                                var Exaprueba1 = toHexString(prueba1 / 1000)
                                var RegistroTimeFinal = Exaprueba1 + item.substring(8, item.length)
                                ListaTimeFinal.add(RegistroTimeFinal)
                                println(" ${registrosTime[i]} ${convertirHexAFecha(Exaprueba1)} Exaprueba1 $Exaprueba1 RegistroTimeFinal $RegistroTimeFinal")

                                // println(" ${ListapruebaTime[i]}  ${convertirHexAFecha(ListapruebaTime[i].substring(0, 8))}")
                            }

                            println("/////////////////////////////////////////////////////////////")
                            for (i in closestIndex downTo 0) {
                                Log.d("ObtenerLoggerPrueba", "valor de indice i $i")
                                val item = registrosTime[i]
                                var TIMESTAMP =
                                    registrosTime[closestIndex]?.replace(" ", "")!!.substring(0, 8)
                                        .replace(" ", "")
                                var TIMECONTROLMIL =
                                    registrosTime[closestIndex]?.replace(" ", "")!!.substring(0, 8)
                                        .replace(" ", "").toLong(16) * 1000
                                var GetNowLMIL = GetNowDateExa().toLong(16) * 1000
                                var regTimemil = item!!.substring(0, 8).toLong(16) * 1000

                                var s = convertHexToHumanDate(item.substring(0, 8))
                                var prueba1 =
                                    regTimemil - (TIMECONTROLMIL - regTimemil) // regTimemil - ( TIMECONTROLMIL - regTimemil)  // regTimemil - (GetNowLMIL - TIMECONTROLMIL)
                                var Exaprueba1 = toHexString(prueba1 / 1000)

                                var RegistroTimeFinal = Exaprueba1 + item.substring(8, item.length)
                                ListaTimeFinal.add(RegistroTimeFinal)
                                Log.d("pruebaLOg", "Exaprueba1 $Exaprueba1 ${registrosTime[i]}")
                                /*   println(
                                       " ${registrosTime[i]} ${convertirHexAFecha(Exaprueba1)} Exaprueba1 $Exaprueba1   hora normal ${
                                           convertirHexAFecha(
                                               registrosTime[i]!!.substring(0, 8)
                                           )
                                       }"
                                   )*/
                            }
                            val listaVolteada = registrosTime.reversed()
                            println("mas cercano es closestTimestamp $closestTimestamp closestIndex $closestIndex ")


                        }

                    }
                    var salidaime = agregar00AlPenultimoCaracter(ListaTimeFinal, 2)
                    salidaime!!.add(0, "20")
                    callback.getTimeCrudo( agregar00AlPenultimoCaracter(registrosTime as MutableList<String>,2))
                    callback.getTime(salidaime)
                } else {
                    callback.onError("Lista vacia Tiempo")
                }




                ////////////////////////////////////////////////// COMIENZA EL PROCESO DE EVENTO///////////////////////////////////////
                clearLogger()
                runOnUiThread {
                    callback.onProgress("Iniciando Evento")
                }
                BanderaEvento = true
                var FINALLISTA: MutableList<String?> = ArrayList()

                var listDataGEvent: MutableList<String>? = ArrayList()
                runOnUiThread {
                    callback.onProgress("Realizando Evento")
                }
                if (ValidaConexion()) {
                    //  bluetoothLeService = getInfoList()// bluetoothServices.bluetoothLeService()
                    Thread.sleep(500)
                    bluetoothServices.sendCommand("handshake", "4021")
                    Thread.sleep(500)

                }

                try {


                    if (ValidaConexion()) {
                        try {
                            FinalListDataFinalE.clear()
                            // Log.d("getInfoListFinal", "${getInfoList().toString()}")
                            //   bluetoothLeService =  getInfoList()// bluetoothServices.bluetoothLeService()
                            Thread.sleep(500)
                            bluetoothServices.sendCommand("handshake", "4021")
                            Thread.sleep(500)
                            runOnUiThread {
                                clearLogger()
                                //  getInfoList()!!.clear()
                            }

                            listData.clear()
                            FinalListTest.clear()
                            FinalListData.clear()



                            FINALLISTA = registrosEvent

                            var SALIDAEVENTOREGISTROS: MutableList<String?> = ArrayList()
                            for (item in FINALLISTA) {
                                if (item != null) {
                                    if (item.length > 18) {
                                        SALIDAEVENTOREGISTROS.add(item)
                                        Log.d(
                                            "FINALLISTA",
                                            "metodoquerecorre FINALLISTA ${FINALLISTA}"
                                        )
                                    }
                                }
                            }

                            if (SALIDAEVENTOREGISTROS.isNotEmpty()) {

                                callback.getEventCrudo(SALIDAEVENTOREGISTROS as MutableList<String>)
                                /*******************************CONTEO DE EVENTO 04 ***********************************/
                                val indexCount: MutableList<Int> = ArrayList()
                                var count = 0
                                for ((index, item) in SALIDAEVENTOREGISTROS.withIndex()) {
                                    val EVENT_TYPE = item?.substring(16, 18)
                                    if (EVENT_TYPE == "04") {
                                        count++
                                        Log.d("debugConco", "-> $item  $count index $index  ")
                                        println("-> $item  $count index $index  ")
                                        indexCount.add(index)
                                    }
                                }
                                val maxIndex =
                                    indexCount.indexOf(iC) // Obtiene el índice del máximo en indexCount
                                val anteriorAlMaximo =
                                    if (maxIndex > 0) indexCount[maxIndex - 1] else null
                                Log.d(
                                    "debugConco",
                                    "el anterior al maximo es ->>>>>>>>>>>>>>>>>>>> anteriorAlMaximo   $anteriorAlMaximo"
                                )
                                Log.d("funcionUltime", "count size ${count}")
                                var SalidaEventF: MutableList<String> = ArrayList()
                                var tipoble = sp!!.getString("name", "")
                                Log.d("debugConco", "tipoble $tipoble  count $count")
                                if (count == 0) {
                                    /*
                                                                            SALIDAEVENTOREGISTROS?.let {
                                                                                runOnUiThread {
                                                                                    callback.getEvent(
                                                                                        agregar00AlPenultimoCaracter(
                                                                                            it as MutableList<String>,
                                                                                            2
                                                                                        )
                                                                                    )
                                                                                    callback.onProgress("Finalizado Evento")
                                                                                }
                                                                            }*/

                                    //     sdds

                                    Log.d("tipoble", "tipoble count $tipoble")
                                    if (tipoble.equals("CEO_CONCO")) {
                                        SALIDAEVENTOREGISTROS.map {
                                            if (it!!.substring(16, 18).uppercase()
                                                    .equals("C2")
                                            ) {
                                                var evento = it.substring(16, 18)
                                                var consumo = it.substring(
                                                    it.length - 2,
                                                    it.length
                                                )
                                                val hexadecimal =
                                                    eventCompresorConco(evento, consumo)

                                                var cadenaEvent = it.substring(
                                                    0,
                                                    16
                                                ) + "02" + it.substring(
                                                    18,
                                                    26
                                                ) + "00" + hexadecimal
                                                SalidaEventF.add(cadenaEvent)
                                                //      println("evento $evento   consumo $consumo hexadecimal $hexadecimal   cadenaEvent  $it $cadenaEvent   ${hexadecimal.toInt(16)}")

                                            } else {
                                                var cadenaEvent = it.substring(
                                                    0,
                                                    26
                                                ) + "0000" + it.substring(26, 28)
                                                //   println("cadenaEvent $cadenaEvent")
                                                SalidaEventF.add(cadenaEvent)
                                            }
                                        }
                                        SALIDAEVENTOREGISTROS.clear()
                                        SALIDAEVENTOREGISTROS =
                                            SalidaEventF as MutableList<String?>

                                        runOnUiThread {
                                            SALIDAEVENTOREGISTROS.add(0, "32")
                                            callback.getEvent(SALIDAEVENTOREGISTROS as MutableList<String>)
                                        }
                                    } else {
                                        Log.d("tipoble", "SALIDAEVENTOREGISTROS")
                                        Log.d(
                                            "tipoble",
                                            "SALIDAEVENTOREGISTROS $SALIDAEVENTOREGISTROS"
                                        )
                                        //    SalidaEventF = SALIDAEVENTOREGISTROS as MutableList<String>
                                        //       SALIDAEVENTOREGISTROS.clear()
                                        SalidaEventF = agregar00AlPenultimoCaracter(
                                            SALIDAEVENTOREGISTROS as MutableList<String>,
                                            2, "00"
                                        ) as MutableList<String>
                                        Log.d("tipoble", "after $SalidaEventF")

                                        runOnUiThread {
                                            SALIDAEVENTOREGISTROS.add(0, "30")
                                            callback.getEvent(SalidaEventF)
                                        }
                                    }


                                }
                                else {

                                    try {
                                        val iC = indexCount.maxOrNull()

                                        val HEXAEnd =
                                            SALIDAEVENTOREGISTROS[iC!!]?.substring(8, 16)

                                        /******************************************************************/
                                        var SalidaEvent: MutableList<String?> = ArrayList()

                                        bluetoothLeService?.sendFirstComando("405B")
                                        listData.clear()
                                        Thread.sleep(700)
                                        for (num in 0..1) {
                                            listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)

                                            Thread.sleep(700)
                                        }
                                        val s = listData[0]
                                        val sSinEspacios = s?.replace(" ", "")
                                        var TIMEUNIX: String? = null

                                        listData?.let {
                                            if (sSinEspacios?.length!! >= 24) TIMEUNIX =
                                                sSinEspacios?.substring(16, 24) else TIMEUNIX =
                                                "612F6B47"
                                            var listTIMEUNIX: MutableList<String>? = ArrayList()
                                            TIMEUNIX?.let {
                                                listTIMEUNIX?.add(it)
                                            };"612F6B47"
                                        }

                                        val TimeStampControl = TIMEUNIX
                                        val TimeStampActual = GetNowDateExa()
                                        /*val TimeStampControlMIlisegundos =
                                            (TimeStampControl?.toLong(16)?.times(1000))
                                        */
                                        var timestampMilliseconds =
                                            TimeStampControl?.toLong(16)?.times(1000)
                                                ?: 0 // Convertir a milisegundos
                                        val timestampWithOneSecondMore =
                                            timestampMilliseconds //+ 1000
                                        var TimeStampControlMIlisegundos =
                                            timestampWithOneSecondMore
                                        val TimeStampActualMIlisegundos =
                                            (TimeStampActual.toLong(16) * 1000) - 71000// 50000


                                        /*****************************************CICLO PARA ESTAMPAR DESPUES DEL ULTIMO CORTE DE LUZ************************************************/
                                        var getEventCrudos = mutableListOf<String>()
                                        getEventCrudos.add(TimeStampControl!!)
                                        getEventCrudos.add(TimeStampActual)
                                        SALIDAEVENTOREGISTROS.map { getEventCrudos.add(it!!) }
                                        //    callback.getEventCrudos(getEventCrudos)

                                        Log.d(
                                            "valoresdelICindex",
                                            "indexCount $indexCount cantidad de count ${indexCount.size}"
                                        )

                                        for (i in SALIDAEVENTOREGISTROS.size - 1 downTo iC) {

//                                            if (SALIDAEVENTOREGISTROS[i]?.substring(16, 18)
//                                                    .equals("04")
//                                            ) {
                                            val HEXAStarCiclo =
                                                SALIDAEVENTOREGISTROS[i]!!.substring(0, 8)
                                            val HEXAEndCiclo =
                                                SALIDAEVENTOREGISTROS[i]!!.substring(8, 16)
                                            val HEXAEndCicloMENOSHEXAStarCiclo =
                                                (HEXAEndCiclo.toLong(16) * 1000) - (HEXAStarCiclo.toLong(
                                                    16
                                                ) * 1000)
                                            val HEXAEndMIlisegundos =
                                                HEXAEndCiclo.toLong(16) * 1000
                                            val Dif = TimeStampControlMIlisegundos?.minus(
                                                HEXAEndMIlisegundos
                                            )
                                            val DiferenciaNowvsDif =
                                                TimeStampActualMIlisegundos - Dif!!
                                            val DiferenciaFINALstar =
                                                DiferenciaNowvsDif - HEXAEndCicloMENOSHEXAStarCiclo
                                            val exaFinalEnd =
                                                toHexString(DiferenciaNowvsDif / 1000)
                                            val exaFinalStar =
                                                toHexString(DiferenciaFINALstar / 1000)


                                            val sdf = if (SALIDAEVENTOREGISTROS[i]!!.substring(
                                                    16,
                                                    18
                                                ) == "04"
                                            ) {
                                                "${
                                                    SALIDAEVENTOREGISTROS[i]!!.substring(
                                                        0,
                                                        8
                                                    )
                                                }$exaFinalEnd${
                                                    SALIDAEVENTOREGISTROS[i]!!.substring(
                                                        16
                                                    )
                                                }"
                                            } else {
                                                "$exaFinalStar$exaFinalEnd${
                                                    SALIDAEVENTOREGISTROS[i]!!.substring(
                                                        16
                                                    )
                                                }"
                                            }
                                            SalidaEvent.add(sdf)


                                        }
                                        if (indexCount.size == 1) {
                                            Log.d(
                                                "valoresdelICindex",
                                                " solo hay un solo evento 04"
                                            )
                                            for (i in iC - 1 downTo 0) {
                                                SalidaEvent.add(SALIDAEVENTOREGISTROS[i])
                                            }
                                        } else Log.d("valoresdelICindex", "  hay varios eventos 04")

                                        if (indexCount.size == 2) {
                                            Log.d(
                                                "valoresdelICindex",
                                                " solo hay un solo evento 04"
                                            )
                                            for (i in iC - 1 downTo anteriorAlMaximo!! + 1) {
                                                SalidaEvent.add(SALIDAEVENTOREGISTROS[i])
                                            }
                                        } else Log.d("valoresdelICindex", "  hay varios eventos 04")

                                        /* for (i in    iC-1 downTo iC2){  //SALIDAEVENTOREGISTROS.size - 1 downTo iC){
                                             println("salida ${SALIDAEVENTOREGISTROS[i]}")
                                         }
                                         */
                                        /*
                                        val diferenciaTOP2 =
                                            result.first()!!.substring(8, 16).toLong(16) * 1000 - (result[1]!!.substring(8, 16)
                                                .toLong(16) * 1000)
                                        val diferencia = result.first()!!.substring(8, 16).toLong(16) * 1000 - (result.first()!!
                                            .substring(0, 8).toLong(16) * 1000)

                                        val diferenciaTOPMINUSlast = (listaTemporalFINALEVENTO.last()!!.substring(8, 16)
                                            .toLong(16) * 1000) - diferenciaTOP
                                        val currentTimeHex =
                                            toHexString(diferenciaTOPMINUSlast / 1000).uppercase()
                                        val LINEARDIFMILL = diferenciaTOPMINUSlast - diferencia
                                        val currentTimeHexLINEAL = toHexString(LINEARDIFMILL / 1000).uppercase()
                                        val dd = (currentTimeHexLINEAL + currentTimeHex + result[0]!!.substring(
                                            16,
                                            result.last()!!.length
                                        )).uppercase()
                                        * */
                                        for (i in iC - 1 downTo 0) {
                                            ////////////////Esta parte del codigo pasa los registros del logger del ultimo evento 04 al evento mas antiguo
                                              SalidaEvent.add(SALIDAEVENTOREGISTROS[i])
                                        }
                                        var tempEvent = SalidaEvent

                                        runOnUiThread {
                                            /*
                                               callback.getEvent(
                                                   agregar00AlPenultimoCaracter(
                                                       SalidaEvent as MutableList<String>?,
                                                       2
                                                   )
                                               )
                                               */


                                            if (tipoble.equals("CEO_CONCO")) {
                                                //   SALIDAEVENTOREGISTROS.add("65BA8E1265BA8E12C2FE34FE3425")
                                                Log.d(
                                                    "debugConco",
                                                    "dentro del if  SALIDAEVENTOREGISTROS $SalidaEvent"
                                                )
                                                SalidaEvent.map {
                                                    if (it!!.substring(16, 18).uppercase()
                                                            .equals("C2")
                                                    ) {
                                                        Log.d("debugConco", "dentro del if  C2 ")
                                                        var evento = it.substring(16, 18)
                                                        var consumo =
                                                            it.substring(it.length - 2, it.length)
                                                        val hexadecimal =
                                                            eventCompresorConco(evento, consumo)

                                                        var cadenaEvent = it.substring(
                                                            0,
                                                            16
                                                        ) + "02" + it.substring(
                                                            18,
                                                            26
                                                        ) + "00" + hexadecimal
                                                        SalidaEventF.add(cadenaEvent)
                                                        //      println("evento $evento   consumo $consumo hexadecimal $hexadecimal   cadenaEvent  $it $cadenaEvent   ${hexadecimal.toInt(16)}")

                                                    } else {
                                                        Log.d(
                                                            "debugConco",
                                                            "dentro del if  C2 else  "
                                                        )
                                                        Log.d(
                                                            "debugConco",
                                                            "debugConco    fatal $it"
                                                        )
                                                        var cadenaEvent = it.substring(
                                                            0,
                                                            26
                                                        ) + "0000" + it.substring(26, 28)
                                                        //   println("cadenaEvent $cadenaEvent")
                                                        SalidaEventF.add(cadenaEvent)
                                                    }
                                                }

                                                //  SALIDAEVENTOREGISTROS = SalidaEventF as MutableList<String?>
                                                //   SALIDAEVENTOREGISTROS.clear()
                                                SalidaEventF.add(0, "32")
                                                callback.getEvent(SalidaEventF)
                                            } else {
                                                //  SalidaEventF = SalidaEvent as MutableList<String>
                                                Log.d(
                                                    "debugConco",
                                                    "no es conco  SALIDAEVENTOREGISTROS $SALIDAEVENTOREGISTROS  SalidaEventF $SalidaEventF"
                                                )
                                                //  SALIDAEVENTOREGISTROS.clear()
                                                var SalidaEventF =
                                                    agregar00AlPenultimoCaracter(
                                                        SalidaEvent as MutableList<String>,
                                                        2, "00"
                                                    ) as MutableList<String?>
                                                Log.d(
                                                    "debugConco",
                                                    "no es conco  SALIDAEVENTOREGISTROS con 0000 "
                                                )
                                                SalidaEventF.add(0, "30")
                                                if (sp!!.getString("name", "")
                                                        .equals("IMBERA-HEALTH")
                                                ) {
                                                    var NewListtoHEALTH = mutableListOf<String>()
                                                    SalidaEventF.get(0)
                                                        ?.let { NewListtoHEALTH.add(it!!) }
                                                    SalidaEventF.map {
                                                        if (it!!.length > 25) {
                                                            var stampInicio = (it.substring(0, 8)
                                                                .toLong(16) * 1000) - (3600000)
                                                            var stampFinal = (it.substring(8, 16)
                                                                .toLong(16) * 1000) - (3600000)
                                                            var FinaltoExaInicio =
                                                                toHexString(stampInicio / 1000)
                                                            var FinaltoExaFinal =
                                                                toHexString(stampFinal / 1000)
                                                            NewListtoHEALTH.add(
                                                                "$FinaltoExaInicio$FinaltoExaFinal${
                                                                    it.substring(
                                                                        16
                                                                    )
                                                                }"
                                                            )
                                                        }

                                                    }
                                                    NewListtoHEALTH.map {
                                                        Log.d("SalidaHeaLT", it)
                                                    }

                                                }

                                                callback.getEvent(SalidaEventF as MutableList<String>)


                                            }
                                        }
                                    } catch (exc: Exception) {
                                        runOnUiThread {
                                            callback.onError(exc.toString())
                                            // callback.onSuccess(false)
                                            callback.onProgress("Finalizado Evento")
                                        }
                                    }
                                }
                                /*                              when (count) {
                                                                  0 -> {
                                                                      /*
                                                                                                              SALIDAEVENTOREGISTROS?.let {
                                                                                                                  runOnUiThread {
                                                                                                                      callback.getEvent(
                                                                                                                          agregar00AlPenultimoCaracter(
                                                                                                                              it as MutableList<String>,
                                                                                                                              2
                                                                                                                          )
                                                                                                                      )
                                                                                                                      callback.onProgress("Finalizado Evento")
                                                                                                                  }
                                                                                                              }*/

                                                                      //     sdds

                                                                          Log.d("tipoble","tipoble count $tipoble")
                                                                          if (tipoble.equals("CEO_CONCO")) {
                                                                              SALIDAEVENTOREGISTROS.map {
                                                                                  if (it!!.substring(16, 18).uppercase()
                                                                                          .equals("C2")
                                                                                  ) {
                                                                                      var evento = it.substring(16, 18)
                                                                                      var consumo = it.substring(
                                                                                          it.length - 2,
                                                                                          it.length
                                                                                      )
                                                                                      val hexadecimal =
                                                                                          eventCompresorConco(evento, consumo)

                                                                                      var cadenaEvent = it.substring(
                                                                                          0,
                                                                                          16
                                                                                      ) + "02" + it.substring(
                                                                                          18,
                                                                                          26
                                                                                      ) + "00" + hexadecimal
                                                                                      SalidaEventF.add(cadenaEvent)
                                                                                      //      println("evento $evento   consumo $consumo hexadecimal $hexadecimal   cadenaEvent  $it $cadenaEvent   ${hexadecimal.toInt(16)}")

                                                                                  } else {
                                                                                      var cadenaEvent = it.substring(
                                                                                          0,
                                                                                          26
                                                                                      ) + "0000" + it.substring(26, 28)
                                                                                      //   println("cadenaEvent $cadenaEvent")
                                                                                      SalidaEventF.add(cadenaEvent)
                                                                                  }
                                                                              }
                                                                              SALIDAEVENTOREGISTROS.clear()
                                                                              SALIDAEVENTOREGISTROS =
                                                                                  SalidaEventF as MutableList<String?>

                                                                              runOnUiThread {
                                                                                  callback.getEvent(SALIDAEVENTOREGISTROS as MutableList<String>)
                                                                              }
                                                                          }
                                                                          else {
                                                                              Log.d("tipoble","SALIDAEVENTOREGISTROS")
                                                                              Log.d("tipoble","SALIDAEVENTOREGISTROS $SALIDAEVENTOREGISTROS")
                                                                              SalidaEventF = SALIDAEVENTOREGISTROS as MutableList<String>
                                                                              SALIDAEVENTOREGISTROS.clear()
                                                                              SALIDAEVENTOREGISTROS =      agregar00AlPenultimoCaracter(
                                                                                  SalidaEventF as MutableList<String>,
                                                                                  2, "0000"
                                                                              ) as MutableList<String?>
                                                                              Log.d("tipoble","after $SALIDAEVENTOREGISTROS")

                                                                              runOnUiThread {
                                                                                  callback.getEvent(SALIDAEVENTOREGISTROS as MutableList<String>)
                                                                              }
                                                                          }




                                                                  }

                                                                  else -> {

                                                                      try {
                                                                          val iC = indexCount.maxOrNull()

                                                                          val HEXAEnd =
                                                                              SALIDAEVENTOREGISTROS[iC!!]?.substring(8, 16)

                                                                          /******************************************************************/
                                                                          var SalidaEvent: MutableList<String?> = ArrayList()

                                                                          bluetoothLeService?.sendFirstComando("405B")
                                                                          listData.clear()
                                                                          Thread.sleep(700)
                                                                          for (num in 0..1) {
                                                                              listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)

                                                                              Thread.sleep(700)
                                                                          }
                                                                          val s = listData[0]
                                                                          val sSinEspacios = s?.replace(" ", "")
                                                                          var TIMEUNIX: String? = null

                                                                          listData?.let {
                                                                              if (sSinEspacios?.length!! >= 24) TIMEUNIX =
                                                                                  sSinEspacios?.substring(16, 24) else TIMEUNIX =
                                                                                  "612F6B47"
                                                                              var listTIMEUNIX: MutableList<String>? = ArrayList()
                                                                              TIMEUNIX?.let {
                                                                                  listTIMEUNIX?.add(it)
                                                                              };"612F6B47"
                                                                          }

                                                                          val TimeStampControl = TIMEUNIX
                                                                          val TimeStampActual = GetNowDateExa()
                                                                          val TimeStampControlMIlisegundos =
                                                                              ( TimeStampControl?.toLong(16)?.times(1000))

                                                                          val TimeStampActualMIlisegundos =
                                                                              ( TimeStampActual.toLong(16) * 1000 )


                                                                          /*****************************************CICLO PARA ESTAMPAR DESPUES DEL ULTIMO CORTE DE LUZ************************************************/
                                                                          for (i in SALIDAEVENTOREGISTROS.size - 1 downTo iC) {

                                                                              if (SALIDAEVENTOREGISTROS[i]?.substring(16, 18)
                                                                                      .equals("04")
                                                                              ) {
                                                                                  val HEXAStarCiclo =
                                                                                      SALIDAEVENTOREGISTROS[i]!!.substring(0, 8)
                                                                                  val HEXAEndCiclo =
                                                                                      SALIDAEVENTOREGISTROS[i]!!.substring(8, 16)
                                                                                  val HEXAEndCicloMENOSHEXAStarCiclo =
                                                                                      (HEXAEndCiclo.toLong(16) * 1000) - (HEXAStarCiclo.toLong(
                                                                                          16
                                                                                      ) * 1000)
                                                                                  val HEXAEndMIlisegundos =
                                                                                      HEXAEndCiclo.toLong(16) * 1000
                                                                                  val Dif = TimeStampControlMIlisegundos?.minus(
                                                                                      HEXAEndMIlisegundos
                                                                                  )
                                                                                  val DiferenciaNowvsDif =
                                                                                      TimeStampActualMIlisegundos - Dif!!
                                                                                  val DiferenciaFINALstar =
                                                                                      DiferenciaNowvsDif - HEXAEndCicloMENOSHEXAStarCiclo
                                                                                  val exaFinalEnd =
                                                                                      toHexString(DiferenciaNowvsDif / 1000)
                                                                                  val exaFinalStar =
                                                                                      toHexString(DiferenciaFINALstar / 1000)
                                                                                  val s = "${
                                                                                      SALIDAEVENTOREGISTROS[i]?.substring(
                                                                                          0,
                                                                                          8
                                                                                      )
                                                                                  }$exaFinalEnd${
                                                                                      SALIDAEVENTOREGISTROS[i]!!.substring(16)
                                                                                  }"
                                                                                  SalidaEvent.add(s)

                                                                              } else {
                                                                                  val HEXAStarCiclo =
                                                                                      SALIDAEVENTOREGISTROS[i]!!.substring(0, 8)
                                                                                  val HEXAEndCiclo =
                                                                                      SALIDAEVENTOREGISTROS[i]!!.substring(8, 16)
                                                                                  val HEXAEndCicloMENOSHEXAStarCiclo =
                                                                                      (HEXAEndCiclo.toLong(16) * 1000) - (HEXAStarCiclo.toLong(
                                                                                          16
                                                                                      ) * 1000)
                                                                                  val HEXAEndMIlisegundos =
                                                                                      HEXAEndCiclo.toLong(16) * 1000
                                                                                  val Dif = TimeStampControlMIlisegundos?.minus(
                                                                                      HEXAEndMIlisegundos
                                                                                  )
                                                                                  val DiferenciaNowvsDif =
                                                                                      TimeStampActualMIlisegundos - Dif!!
                                                                                  val DiferenciaFINALstar =
                                                                                      DiferenciaNowvsDif - HEXAEndCicloMENOSHEXAStarCiclo
                                                                                  val exaFinalEnd =
                                                                                      toHexString(DiferenciaNowvsDif / 1000)
                                                                                  val exaFinalStar =
                                                                                      toHexString(DiferenciaFINALstar / 1000)
                                                                                  val s = "$exaFinalStar$exaFinalEnd${
                                                                                      SALIDAEVENTOREGISTROS[i]!!.substring(16)
                                                                                  }"
                                                                                  SalidaEvent.add(s)

                                                                              }

                                                                          }

                                                                          for (i in iC - 1 downTo 0) {
                                                                              ////////////////Esta parte del codigo pasa los registros del logger del ultimo evento 04 al evento mas antiguo
                                                                              //  SalidaEvent.add(SALIDAEVENTOREGISTROS[i])
                                                                          }
                                                                          var tempEvent = SalidaEvent

                                                                          runOnUiThread {
                                                                              /*
                                                                                 callback.getEvent(
                                                                                     agregar00AlPenultimoCaracter(
                                                                                         SalidaEvent as MutableList<String>?,
                                                                                         2
                                                                                     )
                                                                                 )
                                                                                 */


                                                                              if(tipoble.equals("CEO_CONCO"))
                                                                              {
                                                                                  //   SALIDAEVENTOREGISTROS.add("65BA8E1265BA8E12C2FE34FE3425")
                                                                                  Log.d("debugConco","dentro del if  SALIDAEVENTOREGISTROS $SALIDAEVENTOREGISTROS")
                                                                                  SALIDAEVENTOREGISTROS.map {
                                                                                      if (it!!.substring(16,18).uppercase().equals("C2")){
                                                                                          Log.d("debugConco","dentro del if  C2 ")
                                                                                          var evento = it.substring(16,18)
                                                                                          var consumo = it.substring(it.length-2,it.length)
                                                                                          val hexadecimal =    eventCompresorConco(evento,consumo)

                                                                                          var cadenaEvent = it.substring(0,16)+"02"+it.substring(18,26)+"00"+hexadecimal
                                                                                          SalidaEventF.add(cadenaEvent)
                                                                                          //      println("evento $evento   consumo $consumo hexadecimal $hexadecimal   cadenaEvent  $it $cadenaEvent   ${hexadecimal.toInt(16)}")

                                                                                      }else{
                                                                                          Log.d("debugConco","dentro del if  C2 else  ")
                                                                                          var cadenaEvent = it.substring(0,26)+"0000"+it.substring(26,28)
                                                                                          //   println("cadenaEvent $cadenaEvent")
                                                                                          SalidaEventF.add(cadenaEvent)
                                                                                      }
                                                                                  }

                                                                                  //  SALIDAEVENTOREGISTROS = SalidaEventF as MutableList<String?>
                                                                                  //   SALIDAEVENTOREGISTROS.clear()
                                                                                  callback.getEvent(SalidaEventF as MutableList<String>)
                                                                              }
                                                                              else
                                                                              {
                                                                                  SalidaEventF = SALIDAEVENTOREGISTROS as MutableList<String>
                                                                                  Log.d("debugConco", "no es conco  SALIDAEVENTOREGISTROS $SALIDAEVENTOREGISTROS  SalidaEventF $SalidaEventF")
                                                                                  //  SALIDAEVENTOREGISTROS.clear()
                                                                                  var SDEvent =
                                                                                      agregar00AlPenultimoCaracter(
                                                                                          SalidaEventF as MutableList<String>,
                                                                                          2, "0000"
                                                                                      ) as MutableList<String?>
                                                                                  Log.d("debugConco", "no es conco  SALIDAEVENTOREGISTROS con 0000 $SDEvent")
                                                                                  callback.getEvent(SDEvent as MutableList<String>)


                                                                              }
                                                                          }
                                                                      } catch (exc: Exception) {
                                                                          runOnUiThread {
                                                                              callback.onError(exc.toString())
                                                                              // callback.onSuccess(false)
                                                                              callback.onProgress("Finalizado Evento")
                                                                          }
                                                                      }
                                                                  }
                                                              }
                              */

                            } else {
                                // callback.onSuccess(false)
                                Log.d("funcionUltime", "Lista vacia Evento")
                                callback.onError("Lista vacia Evento")

                            }
                        } catch (ex: Exception) {
                            runOnUiThread {
                                callback.onError(ex.toString())
                            }
                        }
                        runOnUiThread { callback.onProgress("Finalizado Evento") }

                    } else {
                        runOnUiThread {
                            callback.onError("Desconectado")
                            callback.onProgress("Finalizado Evento")
                        }
                    }
                } catch (Exce: Exception) {
                    runOnUiThread {
                        callback.onError("error ${Exce.toString()}")
                        callback.onProgress("Finalizado Evento")
                    }
                }


                /*  ListaTimeFinal.map {
                      Log.d(
                          "salidaFINALTIME",
                          " $it hora  ${convertirHexAFecha(it.substring(0, 8))} "
                      )
                  }*/


            } else {
                callback.onProgress("Finalizado Tiempo")
                callback.onProgress("Finalizado Evento")
                callback.onError("desconectado")
                callback.onSuccess(false)
            }


            return "resp"

        }

        override fun onPostExecute(result: String) {

            callback.onSuccess(true)
            /******************************************************* Iniciando Actualizacion del TIMESTAMP *****************************************************/
//////////////////////// se comenta esta parte para las pruebas

            /*
                        if (ValidaConexion()) {
                            MyAsyncTaskSendDateHour(object : MyCallback {

                                override fun onSuccess(result: Boolean): Boolean {
                                    callback.onProgress(" $result onSuccess timestamp")
                                    return result
                                }

                                override fun onError(error: String) {
                                    callback.onError("timestamp: $error")
                                }

                                override fun getInfo(data: MutableList<String>?) {
                                    callback.onProgress(" $data getInfo timestamp")
                                }

                                override fun onProgress(progress: String): String {
                                    callback.onProgress(" $progress timestamp")
                                    return progress
                                }


                            }).execute()

                            Thread.sleep(500)
                            //   callback.onProgress("iniciando limpieza de logger")

                            Handler().postDelayed({
                                MyAsyncTaskResetMemory(object : MyCallback {

                                    override fun onSuccess(result: Boolean): Boolean {
                                        callback.onProgress(" $result onSuccess ResetMemory")
                                        return result
                                    }

                                    override fun onError(error: String) {
                                        callback.onError("ResetMemory: $error")

                                    }

                                    override fun getInfo(data: MutableList<String>?) {
                                        callback.onProgress(" $data getInfo ResetMemory")
                                    }

                                    override fun onProgress(progress: String): String {

                                        callback.onProgress(" $progress ResetMemory")
                                        if (progress.equals("Finalizado")) {
                                            callback.onProgress("Finalizado Logger")
                                            callback.onSuccess(true)
                                        }
                                        return progress
                                    }


                                }).execute()


                            }, 10000L)
                        }
            */

        }

        override fun onPreExecute() {
            runOnUiThread {
                // getInfoList()?.clear()
                clearLogger()
                callback.onProgress("Iniciando Logger")
                callback.onProgress("Iniciando Tiempo")
                BanderaTiempo = true
            }
        }


    }



    private val gattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    // Manejar datos disponibles
                    val data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA)
                    // Hacer algo con los datos
                }
            }
        }
    }

    suspend fun ObtenerLoggerTimeComand()  :  MutableList<String>{
        bluetoothServices.bluetoothLeService!!.clearListLogger()
        delay(800) // Suspende el hilo sin bloquearlo
        bluetoothServices.bluetoothLeService?.listData?.clear()
        bluetoothLeService?.sendComando("4060")
        var ListapruebaTime = mutableListOf<String>()
        // Recopila datos en segundo plano sin bloquear el hilo principal
        delay(30000)
        ListapruebaTime = bluetoothServices!!.bluetoothLeService!!.getLogeer() as MutableList<String>
        return ListapruebaTime
    }
    suspend fun ObtenerLoggerEventComand()  :  MutableList<String>{
        bluetoothServices.bluetoothLeService!!.clearListLogger()
        delay(800) // Suspende el hilo sin bloquearlo
        bluetoothServices.bluetoothLeService?.listData?.clear()
        bluetoothLeService?.sendComando("evento","4061")
        var ListapruebaTime = mutableListOf<String>()
        // Recopila datos en segundo plano sin bloquear el hilo principal
        delay(50000)
        ListapruebaTime = bluetoothServices!!.bluetoothLeService!!.getLogeer() as MutableList<String>
        return ListapruebaTime
    }


    suspend fun OBtenerTimeControl()  : Long? {
        bluetoothServices.bluetoothLeService!!.clearListLogger()
        delay(300) // Suspende el hilo sin bloquearlo
        bluetoothServices.bluetoothLeService?.listData?.clear()
        bluetoothLeService?.sendComando("4058")
        var ListapruebaTime = mutableListOf<String>()
        // Recopila datos en segundo plano sin bloquear el hilo principal
        delay(800)
        ListapruebaTime = bluetoothServices!!.bluetoothLeService!!.getLogeer() as MutableList<String>

        val s = ListapruebaTime[0]
        val sSinEspacios = s?.replace(" ", "")
        var horaControl = sSinEspacios?.substring(16, 24)!!.toLong(16)?.times(1000)
        return horaControl
    }

    suspend fun obtenerDatosBluetooth(bluetoothServices: BluetoothServices): String? {
        for (i in 1..50) {
            delay(500) // Suspende el hilo sin bloquearlo

            val data = bluetoothServices.bluetoothLeService?.dataFromBroadcastUpdateString
            Log.d("ListapruebaTime", "$data")
            if (data != null) {
                return data
            }
        }
        return null
    }


    fun GetLoggerFinal(callback2: CallbackLoggerVersionCrudo) {
        CoroutineScope(Dispatchers.Main).launch {
            var ValoresFiltrados =  mutableListOf<String>()
            var DatoREfechadoTIme = mutableListOf<String>()
            var ListapruebaEvent = mutableListOf<String>()
            var Event = mutableListOf<String>()

            var CL = 0
            try {
                callback2.onProgress("Iniciando Logger")
                callback2.onProgress("Iniciando Tiempo")
                BanderaTiempo = true

                withContext(Dispatchers.Default) {

                    Log.d("LogProcesoExtraccionDatos","Termino recolectado Evento")
                    delay(500)
                    //var CB133 = OBtenerTimeControl()
                    bluetoothServices.bluetoothLeService!!.clearListLogger()
                    Log.d("LogProcesoExtraccionDatos","Se esta recolectando la hora del control")
                    bluetoothLeService?.sendFirstComando("405B")
                    delay(500)
                    var T33CB1 = bluetoothServices!!.bluetoothLeService!!.getLogeer()!!.first().replace(" ","").substring(16,24).toLong(16).times(1000) /// as MutableList<String>
                    Log.d("LogProcesoExtraccionDatos","Se termino la recollecion del time control")

                    Log.d("LogProcesoExtraccionDatos","Se esta recolectando Evento")
                    bluetoothServices.bluetoothLeService!!.clearListLogger()
                    delay(800) // Suspende el hilo sin bloquearlo
                    bluetoothServices.bluetoothLeService!!.clearListLogger()
                    delay(800) // Suspende el hilo sin bloquearlo
                    bluetoothServices.bluetoothLeService?.listData?.clear()
                    bluetoothLeService?.sendComando("4061")
                    delay(50000)
                    ListapruebaEvent = bluetoothServices.bluetoothLeService!!.getLogeer() as MutableList<String>
                    ListapruebaEvent.map {
                        Log.d("DatosCRUDOSEVENT","$it")
                    }
                    var  EventPRE = VEvent(ListapruebaEvent as MutableList<String?>)
                    EventPRE.map {
                        if (it!!.length>20){
                            Event.add(it)
                        }
                    }
                    Event.map {
                        println("DatosCRUDOSEVENT: $it")
                        Log.d("DatosCRUDOSEVENT","$it")
                    }
                }

                BanderaTiempo = false
                callback2.onProgress("Finalizando")
            } catch (exc: Exception) {
                callback2.onProgress("Error: ${exc.message}")
            }
        }
    }



    inner class MyAsyncTaskGetLoggerFinal(

        var callback: CallbackLoggerVersionCrudo
    ) :
        AsyncTask<Int?, Int?, String>() {
        var ValoresFiltrados =  mutableListOf<String>()
        var DatoREfechadoTIme = mutableListOf<String>()
        var ListapruebaEvent = mutableListOf<String>()
        var ListapruebaTime = mutableListOf<String>()

        var ListaTImeDespuesDelRefechado = mutableListOf<String>()
        var ListaEventoDespuesDelRefechado = mutableListOf<String>()
         var listaInvertidaTIME = mutableListOf<String>()
        var ListaTimeCrudo = mutableListOf<String>()
        var Event = mutableListOf<String>()
        var EventAntes =mutableListOf<String>()

        var CL = 0
        protected override fun doInBackground(vararg params: Int?): String? {



            Log.d("LogProcesoExtraccionDatos","Se esta recolectando la plantilla")
            bluetoothServices.bluetoothLeService!!.clearListLogger()
            bluetoothServices.bluetoothLeService!!.sendFirstComando("4051")
            Thread.sleep(800)
            var Plantilla = bluetoothServices.bluetoothLeService!!.getLogeer()!!.joinToString("").replace(" ","")
            var preAT = "2"
                if (Plantilla.length > 190) {
                    preAT = getDecimal(Plantilla.substring(178, 180)).toString()
                }

            val ATMUestra = preAT.toLong(16).times(1000)

            Log.d("ProcesoPrincipal","PLantillaLimpia $Plantilla  ATMUestra $ATMUestra  preAT $preAT" )
            Log.d("LogProcesoExtraccionDatos","Se esta terminando la recoleccion  la plantilla")
            bluetoothServices.bluetoothLeService!!.clearListLogger()
            bluetoothServices.bluetoothLeService?.listData?.clear()
            Log.d("LogProcesoExtraccionDatos","Se esta recolectando la hora del control")
            bluetoothLeService?.sendFirstComando("405B")
            Thread.sleep (800)
            var T33CB1 = // GetNowDateExa().toLong(16).times(1000) //
             bluetoothServices.bluetoothLeService!!.getLogeer()!!.joinToString("").replace(" ","").substring(16,24).toLong(16).times(1000) /// as MutableList<String>
            Log.d("LogProcesoExtraccionDatos","Se termino la recollecion del time control")
            Thread.sleep(100)
            bluetoothServices.bluetoothLeService!!.clearListLogger()
            var CElTime1 = GetNowDateExa().toLong(16).times(1000)
            Log.d("procesoPrincipal", "CB133 $T33CB1 CElTime1 $CElTime1")
//                    Log.d("procesoPrincipal", "CB133 $CB133")


            ///////////////////////////////////////////////////////////////////////////

            Thread.sleep(1000)
            Log.d("LogProcesoExtraccionDatos","Se esta recolectando Evento")
            bluetoothServices.bluetoothLeService!!.clearListLogger()
            Thread.sleep(800)
            bluetoothServices.bluetoothLeService!!.clearListLogger()
            Thread.sleep(800) // Suspende el hilo sin bloquearlo
            bluetoothServices.bluetoothLeService?.listData?.clear()
            bluetoothLeService?.sendComando("4061")
            Thread.sleep(40000)//50000)
            ListapruebaEvent = bluetoothServices.bluetoothLeService!!.getLogeer() as MutableList<String>

            ListapruebaEvent.map {
               Log.d("DatosCRUDOSEVENT","$it")
            }

            var  EventPRE = VEvent(ListapruebaEvent as MutableList<String?>)

            EventPRE.map {
                if (it!!.length>20){
                    EventAntes.add(it)
                }
            }


            Event = EventAntes.reversed().toMutableList()
            Log.d("LogProcesoExtraccionDatos","Termino recolectado Evento")
            Thread.sleep(500)

            ////////////////////////////////////////////////////////////////// OBteniendo Los datos de Time

            Log.d("LogProcesoExtraccionDatos","Se esta recolectando Time")
            bluetoothServices.bluetoothLeService!!.clearListLogger()
            Thread.sleep(800)
            bluetoothServices.bluetoothLeService!!.clearListLogger()
            Thread.sleep(800) // Suspende el hilo sin bloquearlo
            bluetoothServices.bluetoothLeService?.listData?.clear()
            bluetoothLeService?.sendComando("4060")
            Thread.sleep(20000)//50000)
            ListapruebaEvent = bluetoothServices.bluetoothLeService!!.getLogeer() as MutableList<String>
            var ListapruebaTime =  bluetoothServices.bluetoothLeService!!.getLogeer() as MutableList<String>

            ListapruebaTime.map {
                Log.d("informacionListaBle",it)
            }
            Log.d("LogProcesoExtraccionDatos","Termino recolectado Time")


            var replaceCadena =  ListapruebaTime.joinToString("").replace(" ", "")
            Log.d("ListapruebaTime","replaceCadena ${replaceCadena.length} $replaceCadena   \n $ListapruebaTime")


            var isChecksumOk =  sumarParesHexadecimales(replaceCadena.substring(0,replaceCadena.length-8)).uppercase()
            var toChecksume =replaceCadena.substring(replaceCadena.length-8,replaceCadena.length).uppercase()

            Log.d("ProcesoDelLoggerTime","isChecksumOk $isChecksumOk  toChecksume $toChecksume")
            val maxIntentos = 1
            var intentos = 0
            var checksumCorrecto = false

            while (intentos < maxIntentos && !checksumCorrecto) {
                // Verificar el checksum
                if (!isChecksumOk.equals(toChecksume)) {
                    // Incrementar el contador de intentos
                    intentos++

                    Log.d("ProcesoDelLoggerTime", "Intento $intentos: isChecksumOk es incorrecto")

                    if (intentos < maxIntentos) {
                        // Intentar obtener los datos nuevamente si no es el último intento


                        bluetoothServices.bluetoothLeService!!.clearListLogger()
                        Thread.sleep(800)
                        bluetoothServices.bluetoothLeService!!.clearListLogger()
                        Thread.sleep(800) // Suspende el hilo sin bloquearlo
                        bluetoothServices.bluetoothLeService?.listData?.clear()
                        bluetoothLeService?.sendComando("4060")
                        Thread.sleep(20000) //50000)
                        ListapruebaEvent = bluetoothServices.bluetoothLeService!!.getLogeer() as MutableList<String>
                         ListapruebaTime =  bluetoothServices.bluetoothLeService!!.getLogeer() as MutableList<String>

                        replaceCadena =  ListapruebaTime.joinToString("").replace(" ", "")
                        isChecksumOk =  sumarParesHexadecimales(replaceCadena.substring(0,replaceCadena.length-8)).uppercase()
                        toChecksume =replaceCadena.substring(replaceCadena.length-8,replaceCadena.length).uppercase()

                    }
                } else {
                    // El checksum es correcto, salir del bucle
                    checksumCorrecto = true
                    replaceCadena =  ListapruebaTime.joinToString("").replace(" ", "")
                    Log.d("ProcesoDelLoggerTime", "isChecksumOk es Correcto")
                }
            }

            if (!checksumCorrecto) {
                // Si después de los tres intentos el checksum sigue siendo incorrecto, limpiar ListapruebaTime
                Log.d("ProcesoDelLoggerTime", "No se pudo verificar el checksum después de $maxIntentos intentos. Limpiando ListapruebaTime.")
                ListapruebaTime.clear()
            }

            else{

                bluetoothServices.bluetoothLeService!!.clearListLogger()
                bluetoothLeService?.sendFirstComando("405B")

                Thread.sleep(500)
                var T33CB2 = bluetoothServices!!.bluetoothLeService!!.getLogeer()!!.first().replace(" ","").substring(16,24).toLong(16).times(1000) /// as MutableList<String>

                var CElTime2 = GetNowDateExa().toLong(16).times(1000)
                Thread.sleep(100)
                bluetoothServices.bluetoothLeService!!.clearListLogger()

                Log.d("procesoPrincipal", "T33CB2 $T33CB2 CElTime2 $CElTime2")
                Thread.sleep(1000)


                ListapruebaTime.map {
                    Log.d("cadenaOriginaldeTime","$it")
                }

                var listtoEval = replaceCadena.substring(16,replaceCadena.length-8)
                Log.d("ProcesoDelLoggerTime","listtoEval ${listtoEval.length}")
                var listaFinal = dividirEnPaquetes(listtoEval, 256)
                listaFinal.forEachIndexed { index, paquete ->
                    println("Paquete ${index + 1}: $paquete")
                    Log.d("ProcesoDelLoggerTime"," $paquete")
                }





                /*
                val listaFinal = listOf(

                    "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                    "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                    "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                    "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                    "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                    "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                    "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                    "00000007FFE7FE34766657B841FFE3FE34786657BF52FFD9FE347B6657C662FFDFFE34796657CD73FFE3FE34796657D485FFD5FE347C6657DB97FFE3FE347A6657E2A9FFF0FE347D6657E9BAFFE7FE347D6657F0CBFFF7FE347A6657F7DBFFE8FE347E6657FEECFFF4FE347B665805FDFFECFE347F66580D0E0032FE347E0001",
                    "6658141FFFF7FE347B66581B30FFEAFE347E66582241FFEFFE347E66582952FFF7FE347E66583062FFE9FE347F66583773FFEFFE347C66583E83FFF8FE347E66584593FFE7FE347D66584CA5FFF2FE347F665853B6FFF4FE347F66585AC7FFE7FE347D665861D80037FE347E665868E9FFF7FE347F66586FFAFFE9FE347E0001",
                    "6658770BFFEDFE347C66587E1CFFF9FE347A6658852DFFEDFE347C66588C3EFFEAFE347766589350FFF7FE347766589A61FFF0FE347A6658A172FFE4FE34786658A883FFE8FE347A6658AF93FFEEFE34786658B6A50039FE34796658BDB6FFF7FE34776658C4C7FFEAFE34766658CBD8FFF5FE34766658D2E9FFEEFE34770001",
                    "6658D9FBFFF5FE34766658E10CFFE7FE34796658E81DFFFBFE34766658EF2FFFEDFE34786658F640FFEEFE34796658FD51FFDCFE347766590462FFD7FE347A66590B740035FE347866591285FFD9FE347866591997FFD8FE3479665920A9FFEBFE347C665927BAFFE7FE347B66592ECCFFECFE347A665935DEFFF8FE347C0001",
                    "66593CF0FFEBFE347D66594402FFF8FE347A66594B14FFEDFE347A66595226FFF1FE347D66595937FFF2FE347C665960490039FE347C6659675BFFFEFE347E66596E6DFFEEFE347E6659757FFFF0FE347D66597C91FFEEFE347E665983A3FFF7FE347E66598AB5FFEAFE347C665991C6FFF8FE347D665998D8FFE7FE347F0001",
                    "66599FEAFFF5FE347F6659A6FCFFECFE347E6659AE0DFFEFFE347F6659B51E0038FE347F6659BC2FFFFDFE347C6659C340FFE7FE347E6659CA52FFF6FE347C6659D163FFEFFE347A6659D874FFE8FE347A6659DF84FFF8FE34786659E696FFECFE347A6659EDA7FFEAFE34786659F4B7FFF2FE34796659FBC9FFF8FE34790001",
                    "665A02DAFFEFFE3477665A09EB0042FE3477665A10FC0000FE3478665A180DFFECFE3475665A1F1EFFF9FE3476665A262FFFE9FE3476665A2D40FFFBFE3478665A3451FFE7FE3478665A3B62FFFBFE3476665A4274FFEBFE3476665A4985FFF7FE3477665A5096FFF1FE3479665A57A7FFEEFE347A665A5EB70047FE34780001",
                    "665A65C8FFF1FE347B665A6CD9FFEFFE3479665A73EAFFEAFE3479665A7AFBFFE5FE347C665A820CFFE7FE347A665A891DFFECFE347A665A902FFFF5FE347D665A973FFFF8FE347A665A9E51FFEEFE347C665AA562FFE5FE347B665AAC73FFEAFE347E665AB385002FFE347C665ABA96FFE7FE347E665AC1A8FFEBFE347E0001",
                    "665AC8B9FFF6FE347C665ACFC9FFE9FE3480665AD6DAFFE9FE347F665ADDEAFFF6FE347C665AE4FBFFE6FE347F665AEC0CFFECFE347D665AF31DFFF5FE347E665AFA2FFFE3FE347D665B0140FFF2FE347F665B08510034FE347F665B0F62FFE8FE347D665B1672FFF5FE347E665B1D83FFECFE347D665B2495FFE7FE347B0001",
                    "665B2BA6FFF4FE347C665B32B7FFE3FE347A665B39C8FFF3FE347B665B40D9FFE7FE3479665B47EAFFECFE3479665B4EFBFFF1FE347A665B560CFFE4FE3479665B5D1D002CFE3479665B642EFFEBFE347B665B6B40FFF8FE347A665B7251FFE8FE347B665B7962FFE9FE3479665B8074FFF5FE347B665B8785FFE4FE347C0001",
                    "665B8E96FFEEFE347A665B95A8FFF2FE347B665B9CB9FFE4FE347C665BA3CCFFF0FE3479665BAADDFFF0FE347C665BB1EF002CFE347C665BB901FFEBFE347A665BC013FFF5FE3479665BC724FFEEFE347B665BCE36FFE6FE347D665BD547FFF5FE347A665BDC59FFEBFE347C665BE36AFFE7FE347D665BEA7CFFF5FE347B0001",
                    "665BF18DFFE8FE347D665BF89EFFEAFE347E665BFFAEFFF4FE347E665C06C0002DFE347D665C0DD1FFEAFE347E665C14E3FFF6FE347E665C1BF5FFE8FE347D665C2306FFEAFE347F665C2A17FFF4FE347E665C3127FFE3FE347D665C3839FFF3FE347E665C3F4AFFEAFE347E665C465BFFEAFE347D665C4D6DFFF1FE347D0001",
                    "665C547EFFE4FE347E665C5B8F002CFE347F665C62A1FFEDFE347D665C69B1FFF2FE347C665C70C2FFE3FE347F665C77D3FFF5FE347C665C7EE4FFE4FE347E665C85F4FFF2FE347D665C8D05FFE7FE347B665C9416FFF0FE347B665C9B28FFEAFE347A665CA238FFEAFE347D665CA949FFF0FE347A665CB05B0028FE347A0001",
                    "665CB76CFFF0FE347C665CBE7DFFF4FE3479665CC58FFFE3FE347A665CCCA0FFF2FE347B665CD3B1FFECFE347A665CDAC3FFE9FE347C665CE1D4FFF5FE347A665CE8E5FFE4FE347C665CEFF6FFF0FE347A665CF706FFEFFE3479665CFE16FFE4FE347C665D0525002EFE347A665D0C35FFEAFE347B665D1345FFF5FE347C0001",
                    "665D1A55FFEDFE3479665D2166FFE5FE347C665D2876FFF5FE347A665D2F86FFE7FE347C665D3696FFEDFE347D665D3DA6FFF1FE347A665D44B6FFE3FE347E665D4BC6FFF5FE347D665D52D7FFE7FE347C665D59E70026FE347F665D60F7FFEFFE347C665D6806FFF2FE347D665D6F16FFE3FE347C665D7626FFF5FE347E0001",
                    "665D7D36FFE3FE347F665D8447FFF1FE347D665D8B58FFE7FE3480665D9268FFEFFE347D665D9978FFE7FE347D665DA089FFEEFE3480665DA799FFE9FE347D665DAEAA0025FE347F665DB5BBFFF1FE347A665DBCCCFFE9FE347D665DC3DDFFEDFE347C665DCAEEFFEBFE3478665DD1FFFFEBFE347B665DD910FFEAFE34790001",
                    "665DE020FFEAFE3478665DE731FFE9FE3479665DEE42FFECFE3477665DF552FFEBFE3476665DFC62FFEAFE3477665E03730037FE3479665E0A83FFE7FE3479665E1194FFEBFE3476665E18A4FFF8FE3478665E1FB4FFF0FE3478665E26C4FFE4FE3479665E2DD5FFE8FE3478665E34E6FFF3FE3476665E3BF7FFF7FE34760001",
                    "665E4307FFEEFE3475665E4A18FFFBFE3478665E5128FFE7FE347A665E5838FFF8FE3479665E5F48FFE9FE347A665E6658FFDFFE3478665E6D68FFEBFE347B665E7478FFEAFE347C665E7B88FFE8FE347A665E8298FFE6FE347B665E89A8FFE2FE347D665E90B8FFE8FE347E665E97C8FFF8FE347B665E9ED80023FE347E0001",
                    "665EA5E8FFF3FE347C665EACF8FFE9FE347C665EB408FFFAFE347E665EBB19FFE7FE347C665EC22AFFF1FE347E665EC93BFFF2FE347E665ED04BFFE7FE347D665ED75CFFF5FE347E665EDE6DFFEEFE347C665EE57DFFEAFE347B665EEC8EFFF5FE347E665EF39E0022FE347C665EFAAFFFF3FE347D665F01C0FFE7FE347C0001",
                    "665F08D0FFF5FE347A665F0FE1FFF2FE347D665F16F2FFE6FE347A665F1E02FFEEFE347B665F2514FFF8FE3478665F2C24FFEBFE347B665F3335FFEAFE3477665F3A45FFF5FE3478665F4156FFDAFE3479665F4866001DFE3477665F4F76FFF2FE3478665F5686FFD8FE3479665F5D97FFDEFE3476665F64A8FFE3FE34760001",
                    "665F6BB9FFE3FE3477665F72CAFFE7FE3479665F79DBFFEEFE3479665F80EBFFF1FE3479665F87FCFFE6FE347A665F8F0DFFEFFE3477665F9619FFF8FE3478665F9D290024FE3478665FA43AFFF4FE3478665FAB4AFFE8FE3479665FB259FFE6FE347A665FB96AFFE7FE347D665FC07BFFEDFE3479665FC78CFFF4FE347C0001",
                    "665FCE9CFFF8FE347D665FD5ADFFEEFE347B665FDCBEFFEAFE347D665FE3CFFFF8FE347B665FEAE0FFEAFE347D665FF1F10022FE347C665FF902FFF5FE347B66600012FFEAFE347D66600723FFF9FE347B66600E35FFEBFE347E66601546FFEEFE347D66601C57FFF8FE347D66602369FFE9FE347C66602A7AFFEEFE347C0001",
                    "6660318BFFF8FE347E6660389BFFE8FE347C66603FADFFF3FE347E666046BE0024FE347E66604DCFFFF5FE347C666054E0FFEEFE347C66605BF1FFF2FE347A66606302FFEAFE347D66606A13FFF8FE347A66607124FFE9FE347A66607834FFF4FE347B66607F45FFEFFE347766608656FFEAFE347866608D670006FE34770001",
                    "6660947AFFECFE347966609B8B0032FE347A6660A29CFFF5FE34766660A9ADFFD7FE347A6660B0BEFFD9FE34776660B7D0FFDCFE34776660BEE1FFF4FE34796660B7D0FFEAFE34776660BEE0FFF8FE34796660C5F1FFE7FE34776660CD02FFFAFE34786660E235FFE7FE347A6660E946FFFBFE34766660F057002DFE347A0001",
                    "6660F769FFFCFE347D6660FE7AFFFEFE34786661058BFFE9FE347966610C9CFFF7FE3479666113AEFFE9FE347D66611AC0FFEEFE347C666121D2FFF1FE347D666128E3FFF7FE347A66612FF4FFE8FE347E66613705FFF0FE347F66613E16FFF8FE347A66614527002BFE347C66614C39FFFCFE347D6661534AFFFFFE347E0001",
                    "66615A5BFFF4FE347E6661616CFFE7FE347C6661687DFFFCFE347C66616F8FFFEEFE347C666176A0FFEEFE347F66617DB2FFF5FE347C666184C3FFE7FE347C66618BD5FFF8FE347E666192E7FFECFE347E666199F80024FE347B6661A10AFFF8FE347C6661A81CFFF5FE347D6661AF2EFFE8FE347E6661B63FFFF8FE347C0001",
                    "6661BD51FFE7FE347D6661C462FFF8FE34796661CB74FFE7FE347B6661D285FFF9FE347A6661D995FFE7FE347B6661E0A6FFF6FE34796661E7B7FFE7FE34796661EEC70023FE34796661F5D8FFF5FE34796661FCE9FFEAFE3478666203FBFFF9FE347666620B0CFFE7FE347A6662121EFFFAFE34796662192FFFEAFE34790001",
                    "6662203FFFF5FE347766622750FFEFFE347666622E61FFECFE347766623571FFF7FE347766623C81FFE8FE3477666243920022FE347B66624AA3FFF7FE3478666251B4FFEDFE347B666258C6FFF8FE347966625FD8FFEDFE347C666266EAFFE7FE347C66626DFBFFF3FE34796662750DFFF0FE347D66627C1DFFEAFE347D0001",
                    "6662832EFFF8FE347D66628A40FFE7FE347D66629150FFF5FE347A666298610024FE347C66629F72FFF7FE347D6662A684FFEFFE347D6662AD95FFF6FE347B6662B4A6FFE8FE347C6662BBB8FFF8FE347E6662C2C9FFE9FE347F6662C9DBFFEEFE347E6662D0ECFFF8FE347C6662D7FDFFE7FE347F6662DF0FFFF4FE347B0001",
                    "6662E621FFF1FE347C6662ED320025FE347E6662F444FFF5FE347D6662FB56FFEAFE347B66630267FFFAFE347C66630978FFE7FE347C66631089FFF0FE347B6663179AFFF7FE347966631EABFFE7FE347C666325BCFFEFFE347866632CCEFFF7FE347A666333DFFFF1FE347766633AF1FFE8FE3477666342030022FE34790001",
                    "66634915FFF3FE347966635026FFE7FE347A66635736FFF5FE347966635E47FFEDFE347A66636557FFEEFE347966636C67FFF8FE347766637378FFECFE347A66637A88FFF5FE347966638199FFEDFE3477666388ABFFF5FE347A66638FBCFFEDFE3478666396CD0022FE347B66639DDDFFF3FE34786663A4EFFFE7FE347A0001",
                    "6663AC00FFEEFE34796663B312FFF5FE34786663BA23FFECFE347C6663C134FFE5FE347D6663C845FFF1FE347D6663CF57FFF5FE347A6663D669FFE7FE347B6663DD7AFFEAFE347E6663E48BFFF7FE347B6663EB9D001AFE347C6663F2AEFFE7FE347E6663F9C0FFEAFE347C666400D2FFF7FE347E666407E3FFEBFE347E0001",
                    "66640EF5FFE9FE347C66641607FFF7FE347D66641D18FFEAFE347B66642429FFE7FE347F66642B3AFFF5FE347B6664324BFFE6FE347F6664395CFFEDFE347F6664406D001BFE347C6664477FFFE7FE347D66644E90FFEAFE347C666455A1FFF5FE347D66645CB3FFE3FE347D666463C4FFF0FE347D66646AD5FFECFE347C0001",
                    "666471E6FFE7FE347A666478F6FFF5FE347B66648007FFE4FE347B66648719FFF1FE347C66648E2AFFECFE34786664953B0018FE347966649C4CFFE7FE347B6664A35DFFEFFE347C6664AA6EFFF2FE347B6664B180FFE3FE347C6664B892FFF1FE347A6664BFA4FFEEFE34786664C6B6FFE7FE347C6664CDC8FFF6FE347B0001",
                    "6664D4DAFFE6FE347D6664DBECFFEBFE347A6664E2FEFFF4FE347A6664EA0F0017FE347A6664F121FFE8FE347D6664F833FFF0FE347A6664FF44FFF4FE347C66650656FFE4FE347A66650D67FFEEFE347A66651478FFF4FE347C66651B89FFE4FE347E66652299FFEEFE347E666529AAFFF4FE347A666530BBFFE3FE347C0001",
                    "666537CCFFF1FE347E66653EDE0019FE347C666545EFFFE7FE347E66654D00FFEAFE347E66655412FFF6FE347C66655B23FFE9FE347C66656234FFEAFE347F66656946FFF5FE347E66657057FFE4FE347F66657769FFF0FE347D66657E7BFFF0FE347E6665858DFFE3FE347C66658C9EFFF5FE347E666593AF0017FE347F0001",
                    "66659AC1FFEAFE347F6665A1D1FFF1FE347F6665A8E2FFEFFE347B6665AFF3FFE4FE347B6665B704FFF5FE347E6665BE15FFEAFE347D6665C526FFE9FE347E6665CC37FFF3FE347D6665D348FFE5FE347B6665DA59FFEEFE347D6665E16AFFF1FE347B6665E87B0015FE347A6665EF8CFFEAFE347D6665F69DFFF2FE347A0001",
                    "6665FDAEFFF1FE347D666604BFFFE3FE347D66660BCFFFF1FE347A666612E0FFF0FE347C666619F2FFE3FE347B66662103FFF1FE347B66662815FFEEFE347A66662F26FFE5FE347D66663637FFF2FE347D66663D490019FE347D6666445BFFE7FE347C66664B6CFFE9FE347D6666527DFFF5FE347B6666598FFFF0FE347D0001",
                    "666660A1FFE4FE347E666667B2FFEEFE347C66666EC3FFF3FE347C666675D5FFE4FE347F66667CE6FFEEFE347F666683F7FFF5FE347E66668B08FFE7FE347D666692190017FE347F6666992AFFEAFE347D6666A03BFFEEFE347F6666A74CFFF4FE347F6666AE5DFFE6FE34806666B56EFFECFE347E6666BC7FFFF5FE347D0001",
                    "6666C390FFE7FE347E6666CAA1FFEAFE34806666D1B3FFF4FE347F6666D8C4FFE6FE347F6666DFD5FFECFE347D6666E6E70018FE347D6666EDF8FFE8FE347F6666F509FFEEFE347E6666FC1AFFF4FE347C6667032BFFE3FE347D66670A3CFFF1FE347A6667114EFFEDFE347966671860FFE7FE347A66671F71FFF2FE347A0001",
                    "66672683FFE2FE347B66672D94FFF2FE3477666734A6FFE3FE347866673BB70018FE3477666742C8FFEAFE3479666749D9FFF4FE3479666750EAFFEFFE3479666757FCFFE3FE347966675F0CFFF0FE347A6667661DFFFAFE347766676D2EFFE8FE347A6667743FFFE8FE347A66677B4FFFE8FE34786667825FFFE7FE34780001",
                    "6667896FFFE5FE347966679080FFE7FE347966679791FFF0FE347C66679EA1FFF2FE347B6667A5B2FFE8FE347D6667ACC2FFE1FE34796667B3D2FFE8FE347A6667BAE30006FE347E6667C1F4000DFE347B6667C905FFDBFE347E6667D016FFDCFE347C6667D727FFE5FE347C6667DE38FFE7FE347C6667E549FFE9FE347F0001",
                    "6667EC5AFFEAFE347F6667F36CFFF1FE347C6667FA7DFFF5FE347E6668018FFFF1FE347C666808A0FFEEFE347E66680FB10004FE347F666816C2000DFE347C66681DD3FFEAFE347D666824E6FFE7FE347F66682BF7FFE7FE347F66683308FFEDFE347F66683A19FFF1FE347C6668412AFFF3FE347D6668483BFFEEFE347C0001",
                    "66684F4CFFE8FE347B6668565CFFE6FE347D66685D6DFFEAFE347D6668647D0013FE347A66686B8E0014FE34786668729EFFEAFE347A666879AFFFE4FE3479666880BFFFEFFE347A666887CFFFEFFE347766688EE0FFE7FE347B666895F0FFE7FE347766689D00FFE9FE347A6668A411FFECFE34786668AB21FFEFFE347A0001",
                    "6668B232FFEDFE347B6668B9420013FE347B6668C0530019FE34786668C763FFF7FE34786668CE73FFECFE34776668D584FFE9FE34776668DC92FFE8FE347A6668E3A3FFE9FE34786668EAB3FFEBFE347C6668F1C3FFF2FE347B6668F8D4FFF0FE347B6668FFE5FFE8FE347C666906F6FFE7FE347B66690E060009FE347B0001",
                    "666915170014FE347D66691C28FFF5FE347B66692339FFF5FE347D66692A4AFFF4FE347B6669315BFFF2FE347E6669386CFFEEFE347B66693F7DFFEBFE347E6669468EFFE7FE347D66694DA0FFE6FE347F666954B1FFE8FE347C66695BC2FFEBFE347F666962D40011FE347C666969E70016FE347E666970F9FFF3FE347C0001",
                    "6669780BFFF5FE347E66697F1CFFF6FE347E6669862DFFF7FE347D66698D3FFFF6FE347E66699451FFF8FE347D66699B62FFF4FE347A6669A273FFF4FE347C6669A985FFF4FE347B6669B097FFF5FE34786669B7A8000DFE34786669BEB90018FE347A6669C5CAFFF8FE34796669CCDAFFF4FE34796669D3EBFFF3FE34740001",
                    "6669DAFCFFF3FE34756669E20DFFF1FE34776669E91EFFF5FE34746669F02FFFE9FE34796669F740FFEEFE34786669FE51FFEEFE3479666A0562FFF0FE3477666A0C730014FE347A666A1384001BFE3477666A1A95FFF5FE3478666A21A6FFECFE3477666A28B7FFEAFE347A666A2FC8FFE7FE347B666A36D9FFE6FE34790001",
                    "666A3DE9FFE7FE347A666A44FAFFE5FE347C666A4C0BFFE6FE347C666A531CFFE5FE347D666A5A2DFFE6FE347E666A613E0009FE347B666A684F0018FE347D666A6F61FFF1FE347B666A7672FFF0FE347C666A7D83FFEEFE347E666A8494FFECFE347D666A8BA5FFEDFE347E666A92B6FFEDFE347F666A99C7FFEDFE347D0001",
                    "666AA0D7FFEEFE347F666AA7E8FFEEFE347F666AAEF8FFF1FE347C666AB6090010FE347F666ABD1B0017FE347D666AC42CFFF5FE347F666ACB3DFFF4FE347F666AD24EFFF0FE347C666AD95FFFECFE347E666AE070FFE7FE347D666AE781FFE6FE347E666AEE93FFEBFE347D666AF5A5FFF1FE347A666AFCB7FFF3FE347C0001",
                    "666B03C8FFECFE347C666B0ADA0005FE3479666B11EB0012FE347B666B18FCFFE5FE347B666B200DFFEFFE347B666B271EFFF4FE347A666B2E2FFFE5FE347B666B3540FFE8FE347D666B3C51FFECFE347A666B4363FFECFE347B666B4A74FFEAFE347D666B5186FFE7FE347B666B5897FFE6FE347B666B5FA90007FE347C0001",
                    "666B66BA0018FE3477666B6DCCFFF7FE3477666B74DDFFECFE347A666B7BEFFFE6FE3478666B8301FFE8FE347A666B8A12FFD8FE3479666B9123FFE8FE347A666B9834FFD7FE347D666B9F45FFDCFE347A666BA657FFE1FE347D666BAD68FFE9FE347B666BB4790005FE347C666BBB8B0015FE347C666BC29CFFF5FE347C0001",
                    "666BC9AEFFF6FE347C666BD0C0FFEFFE347E666BD7D1FFEFFE347E666BDEE2FFF0FE347F666BE5F3FFECFE347F666BED05FFE9FE347F666BF416FFE7FE347E666BFB28FFEAFE347C666C0239FFEFFE347D666C094A000DFE347F666C105C001DFE347F666C176EFFEDFE347F666C1E80FFE7FE347F666C2591FFEAFE347C0001",
                    "666C2CA3FFF1FE347E666C33B5FFF4FE347D666C3AC6FFF4FE347B666C41D7FFEEFE347B666C48E8FFECFE347B666C4FF9FFE7FE347C666C570AFFE6FE347B666C5E1C0007FE3478666C652E0018FE347A666C6C40FFEEFE3477666C7352FFECFE3478666C7A64FFECFE3478666C8175FFE5FE3478666C8887FFE4FE34780001",
                    "666C8F99FFE6FE3478666C96ABFFE8FE3478666C9DBDFFE7FE347A666CA4CFFFE7FE3478666CABE1FFE5FE347A666CB2F3000DFE347A666CBAE70024FE3479666CC348FFF4FE347A666CCA5BFFF1FE347B666CD16EFFE7FE347B666CD881FFE7FE347C666CDF95FFEAFE3479666CE6A7FFEDFE3479666CEDBAFFE3FE347C0001",
                    "666CF4CCFFF2FE347C666CFBDEFFE3FE347B666D02F1FFF2FE347D666D0A03FFE4FE347E666D1116FFF0FE347B666D18290014FE347E666D1F3CFFF1FE347C666D264EFFECFE347E666D2D61FFEAFE347D666D3473FFE7FE347F666D3B86FFF0FE347E666D4298FFE3FE347F666D49ABFFF1FE347E666D50BEFFE7FE347D0001",
                    "666D57D1FFEBFE347F666D5EE4FFEEFE347F666D65F6FFE4FE347D666D6D090017FE347F666D741CFFEDFE347F666D7B2FFFECFE347E666D8242FFEAFE3480666D8954FFE5FE347F666D9067FFEEFE347F666D977AFFE4FE347E666D9E8DFFF0FE347D666DA59FFFE3FE347D666DACB1FFF1FE347C666DB3C3FFE3FE347C0001",
                    "666DBAD6FFF1FE347B666DC1E90014FE347B666DC8FBFFF0FE347B666DD00DFFE7FE347C666DD71FFFF1FE347B666DDE31FFE3FE347B666DE543FFF1FE347B666DEC55FFE6FE347D666DF367FFEFFE347B666DFA79FFE7FE347D666E018AFFEBFE347A666E089CFFE9FE347C666E0FADFFEAFE347C666E16BE0015FE347D0001",
                    "666E1DCFFFEEFE347B666E24E1FFEAFE347D666E2BF2FFECFE347E666E3303FFE5FE347E666E3A14FFF1FE347D666E4125FFE4FE347B666E4837FFEEFE347C666E4F49FFE8FE347E666E565AFFEDFE347E666E5D6CFFE9FE347E666E647DFFE8FE347C666E6B8F0014FE347F666E72A0FFEFFE347D666E79B1FFEAFE347F0001",
                    "666E80C2FFEFFE347E666E87D3FFE3FE347F666E8EE4FFF1FE347C666E95F5FFE4FE347F666E9D06FFEFFE347D666EA417FFE7FE3480666EAB28FFEEFE347F666EB239FFE8FE3480666EB949FFEAFE347D666EC05A0014FE3480666EC76BFFF0FE347D666ECE7BFFE8FE3480666ED58CFFEEFE347E666EDC9CFFE3FE34800001",
                    "666EE3ADFFF2FE347F666EEABEFFE3FE347F666EF1CEFFEFFE347E666EF8DFFFE7FE347F666EFFEFFFEBFE347E666F06FFFFEBFE347C666F0E0FFFE7FE347B666F15200014FE347D666F1C30FFF1FE3479666F2340FFE7FE347B666F2A51FFEFFE347C666F3161FFE3FE347D666F3872FFF1FE347C666F3F83FFE5FE347D0001",
                    "666F4694FFEFFE347A666F4DA4FFE4FE347D666F54B5FFF1FE347C666F5BC6FFE6FE347D666F62D7FFEFFE347A666F69E70014FE347E666F70F8FFF2FE347A666F7808FFE6FE347D666F7F19FFF2FE347B666F862AFFE5FE347E666F8D39FFEDFE347D666F9449FFEBFE347D666F9B59FFE7FE347B666FA26AFFEEFE347C0001",
                    "666FA97AFFE3FE347E666FB08AFFF1FE347D666FB79AFFE3FE347F666FBEAA0017FE347F666FC5BBFFEDFE347F666FCCCAFFEDFE347E666FD3DBFFECFE3480666FDAEBFFE4FE3480666FE1FBFFF1FE347D666FE90BFFE3FE347D666FF01BFFF1FE347F666FF72AFFE3FE347E666FFE3AFFEFFE347F6670054AFFE3FE34800001",
                    "66700C5BFFF1FE347F6670136B0014FE348066701A7BFFEFFE347F6670218CFFE9FE347D6670289CFFEFFE347E66702FACFFE3FE347C667036BCFFF1FE347D66703DCCFFE3FE347D667044DCFFF1FE347C66704BEDFFE3FE3479667052FEFFF1FE347966705A0EFFE4FE347B6670611EFFE0FE34796670682E0014FE347A0001",
                    "66706F3EFFEFFE347966707834FFE6FE347A66707F46FFF0FE347966708658FFE7FE347866708D6AFFECFE34796670947CFFECFE347866709B8EFFE6FE347A6670A2A0FFF0FE34796670A9B1FFE3FE347A6670B0C3FFF1FE34776670B7D4FFE3FE347A6670BEE6FFF1FE347A6670C5F7FFE7FE347A6670CD090026FE347B0001",
                    "6670D41BFFE4FE347C6670DB2DFFF1FE34796670E23FFFE3FE347A6670E950FFF1FE347C6670F062FFE3FE347B6670F773FFF1FE347D6670FE84FFE4FE347B66710595FFF0FE347E66710CA7FFE8FE347E667113B8FFE7FE347F66711AC9FFEBFE347F667121DA0022FE347F667128EBFFE7FE347E66712FFDFFEFFE347E0001",
                    "6671370EFFE8FE347F66713E1FFFEAFE347E66714530FFEEFE347C66714C40FFE3FE347D66715350FFF1FE347C66715A61FFE3FE347F66716171FFF1FE347C66716882FFE4FE348066716F93FFF1FE347C667176A3001CFE347F66717DB4FFEAFE347E667184C4FFECFE347D66718BD5FFE9FE347D667192E5FFEAFE347C0001",
                    "667199F6FFEAFE347C6671A106FFEAFE347B6671A817FFE8FE34786671AF27FFF1FE347A6671B637FFE8FE34786671BD47FFEBFE347A6671C458FFE7FE34776671CB690022FE347A6671D279FFE8FE347A6671D98AFFF1FE34766671E09AFFE9FE34796671E7ABFFECFE34766671EEBBFFEAFE347A6671F5CBFFEAFE34780001",
                    "6671FCDCFFEDFE3477667203EDFFEAFE347966720AFDFFEAFE347A6672120EFFEAFE34776672191FFFEBFE347B667220300025FE347B66722741FFE6FE347C66722E52FFEFFE347B66723563FFE3FE347966723C74FFF3FE347C66724384FFE3FE347B66724A95FFF2FE347C667251A6FFE4FE347D667258B7FFF1FE347B0001",
                    "66725FC9FFE6FE347E667266DAFFEEFE347A66726DEBFFEAFE347F667274FD0022FE347E66727C0EFFE6FE347F6672831FFFF3FE347E66728A30FFE3FE347D66729141FFEFFE347F66729852FFE9FE347D66729F63FFE7FE347F6672A674FFEEFE347D6672AD86FFE3FE347F6672B498FFF2FE347E6672BBAAFFE3FE347F0001",
                    "6672C2BBFFEFFE347E6672C9CD001FFE347E6672D0DFFFEAFE347E6672D7F0FFEDFE347C6672DF02FFECFE347A6672E614FFE4FE347B6672ED25FFF1FE34796672F437FFE6FE347B6672FB48FFEBFE347866730259FFEAFE347B6673096BFFEDFE34796673107DFFE3FE34786673178EFFF1FE347966731E9F001FFE347A0001",
                    "667325B1FFE7FE347766732CC2FFEFFE3479667333D4FFE5FE347966733AE5FFF1FE3476667341F6FFE3FE347966734907FFEEFE347666735018FFE6FE347966735729FFF0FE347866735E3AFFE3FE347A6673654AFFF2FE347866736C5BFFE3FE347A6673736C0022FE347C66737A7DFFE7FE347C6673818EFFEFFE34790001",
                    "6673889FFFEBFE347C66738FB0FFE3FE347D667396C0FFEAFE347D66739DD1FFE5FE347E6673A4E1FFF1FE347D6673ABF2FFE6FE347B6673B303FFECFE347C6673BA13FFEAFE347E6673C124FFE7FE347E6673C8360020FE347F6673CF47FFE8FE347F6673D658FFF1FE347E6673DD69FFE8FE347F6673E47AFFE9FE347D0001",
                    "6673EB8BFFEFFE347C6673F29DFFE4FE347D6673F9AEFFF0FE347C667400C0FFE9FE3480667407D1FFEAFE347E66740EE2FFECFE347F667415F2FFE3FE347F66741D030021FE347E66742413FFE7FE347B66742B23FFEFFE347966743234FFEAFE347D66743944FFE7FE347C66744055FFEFFE347A66744765FFE3FE347B0001",
                    "66744E76FFF1FE347966745584FFE7FE347A66745C95FFE4FE3477667463A7FFEEFE347866746AB7FFEEFE3478667471C8001BFE347A667478D9FFEAFE347966747FEAFFF0FE3478667486FBFFE3FE347A66748E0CFFF1FE34766674951DFFE2FE347966749C2EFFF1FE34796674A33FFFE1FE34776674AA50FFEFFE34780001",
                    "6674B161FFE5FE347A6674B872FFECFE347A6674BF83FFEEFE347A6674C695001DFE347B6674CDA6FFEBFE347B6674D4B6FFEAFE347B6674DBC7FFE2FE347C6674E2D8FFECFE347A6674E9E9FFE7FE347D6674F0FAFFEDFE347B6674F80AFFE6FE347E6674FF1BFFF1FE347A6675062CFFE3FE347B66750D3DFFEFFE347C0001",
                    "6675144DFFE5FE347E66751B5E0024FE347E6675226FFFE6FE347C66752981FFF1FE347B66753092FFE7FE347F667537A3FFE7FE347F66753EB4FFEDFE347D667545C5FFE2FE347D66754CD6FFF1FE347F667553E7FFE5FE348066755AF7FFEEFE347F66756208FFE7FE347C66756918FFE9FE347F66757029001FFE347E0001",
                    "66757739FFEAFE347D66757E4AFFE8FE347D6675855AFFEEFE347A66758C6BFFE3FE347C6675937DFFF1FE347B66759A8EFFE4FE347A6675A19FFFEEFE347A6675A8B0FFF0FE34796675AFC2FFEEFE34786675B6D3FFE5FE34796675BDE4FFE2FE347B6675C4F5001CFE34786675CC07FFE8FE347A6675D318FFEEFE347A0001",
                    "6675DA29FFEFFE347A6675E13AFFEDFE34796675E84BFFE5FE34796675EF5CFFE3FE347B6675F66EFFE1FE347B6675FD7FFFDFFE347966760491FFDFFE347B66760BA2FFE1FE347C667612B4FFE0FE347D667619C4001BFE347A667620D5FFE5FE347D667627E7FFE3FE347D66762EF8FFE3FE347C6676360AFFE9FE347E0001",
                    "66763D1BFFF0FE347D6676442DFFE3FE347C66764B3EFFECFE347E6676524EFFECFE347D6676595FFFE7FE347B66766070FFECFE347D66766781FFE6FE347F66766E910024FE347C667675A2FFE7FE347F66767CB3FFEAFE347D667683C3FFEAFE347D66768AD4FFE9FE347F667691E5FFE9FE347F667698F5FFF0FE347D0001",
                    "6676A005FFE3FE347F6676A715FFEEFE347F6676AE25FFEBFE347D6676B535FFE6FE347F6676BC45FFF0FE347F6676C3560019FE347D6676CA66FFEEFE347F6676D177FFE7FE347E6676D887FFE9FE347D6676DF97FFEBFE347C6676E6A7FFE5FE347D6676EDB7FFF1FE347C6676F4C7FFE3FE347A6676FBD7FFF1FE347B0001",
                    "667702E8FFE7FE347A667709F8FFE7FE347C66771109FFF1FE347B667718190019FE347A66771F2AFFEEFE347C6677263AFFE5FE347C66772D4AFFEDFE347A6677345AFFE4FE347C66773B6BFFEEFE347B6677427BFFE7FE347C6677498CFFE9FE347D6677509DFFEBFE347A667757AEFFEAFE347D66775EBFFFEAFE347D0001",
                    "667765CFFFE6FE347D66776CDF0025FE347C667773EFFFE8FE347E66777B00FFEEFE347C66778211FFE8FE347B66778922FFECFE347A66779033FFE7FE347E66779744FFE7FE347C66779E54FFEAFE347F6677A565FFEEFE347E6677AC75FFE2FE347F6677B385FFF0FE347E6677BA95FFE4FE347F6677C1A5001CFE347D0001",
                    "6677C8B4FFECFE34806677CFC3FFE9FE34806677D6D3FFE8FE34806677DDE3FFEDFE34806677E4F3FFEAFE347E6677EC03FFF1FE34806677F312FFE3FE34806677FA22FFF0FE347E66780132FFE6FE348166780843FFE7FE347E66780F54FFF1FE3480667816640018FE347E66781D75FFEBFE348066782486FFE5FE347F0001",
                    "66782B97FFEBFE347B667832A7FFEAFE347E667839B8FFE8FE347E667840C9FFEFFE347C667847DAFFE4FE347D66784EEAFFF1FE347B667855FAFFE3FE347966785D0BFFEAFE347D6678641CFFEEFE347C66786B2D0018FE34796678723EFFEFFE347C66787950FFE3FE347D66788061FFEEFE347A66788771FFE7FE347E0001",
                    "66788E82FFEAFE347D66789593FFEDFE347C66789CA4FFE5FE347D6678A3B4FFF0FE347D6678AAC5FFE3FE347E6678B1D7FFF0FE347B6678B8E8FFE5FE347E6678BFF9001BFE347B6678C70BFFECFE347E6678CE1CFFEBFE347D6678D52EFFE7FE347B6678DC3EFFEEFE347D6678E34FFFE3FE347C6678EA61FFF1FE347D0001",
                    "6678F171FFE3FE347E6678F882FFE7FE347C6678FF93FFF1FE347E667906A4FFE4FE347C66790DB5FFE7FE347F667914C7001FFE347C66791BD8FFEAFE347F667922E9FFEAFE347F667929FAFFE8FE34806679310BFFF1FE347F6679381CFFE3FE348066793F2DFFEFFE347F6679463EFFE4FE347D66794D4FFFEBFE34800001",
                    "66795461FFF0FE347D66795B74FFE2FE347F66796285FFECFE347E66796997001BFE347B667970A9FFEAFE347D667977BBFFEEFE347C66797ECCFFE7FE347D667985DDFFF0FE347B66798CEFFFE1FE347C667993FFFFE5FE347C66799B10FFE6FE34796679A221FFE4FE347B6679A933FFE7FE347B6679B044FFEBFE347A0001",
                    "6679B755FFEEFE347A6679BE660017FE34786679C576FFEAFE34786679CC87FFF1FE347A6679D398FFF1FE34796679DAA8FFE5FE347B6679E1B9FFE8FE34786679E8CBFFEFFE347A6679EFDCFFE2FE34796679F6EDFFEFFE347A6679FDFFFFF0FE347B667A050DFFEDFE347B0000000000000000000000000000000000006C00"
                )
*/

                Log.d("ProcesoDelLoggerTime"," \n \n ----------------------- ${listaFinal.size}")

                val FinalLIstaTime = mutableListOf<String>()
                var FinalLIstaTimeFINAL = mutableListOf<String>()
                for (i in 0 until listaFinal.size - 1) {
                    FinalLIstaTime.add(listaFinal[i])
                }
                Log.d("ProcesoDelLoggerTime"," \n \n ----------------------- ${FinalLIstaTime.size}")
                FinalLIstaTime.map{ //   .forEachIndexed { index, paquete ->
                    //  println("Paquete ${index + 1}: $paquete")
                    Log.d("FinalLIstaTime"," $it")
                }
                var ultimopaquete  =  mutableListOf<String>()
                ultimopaquete.add(listaFinal.last())


                ValoresFiltrados = dividirpaquetes18TIme(FinalLIstaTime)

                ValoresFiltrados.map {
                    Log.d("RecorridoDELOsDatosSeparados","$it ${convertirHexAFecha(it.substring(0,8))}")
                }
//                ultimopaquete.map {
                var lastDato =  ultimopaquete.last()
                    val indiceFinal = getDecimal(lastDato.substring(lastDato.length - 4, lastDato.length - 2)) * 2
                    val BanderaListaCompleta = lastDato.substring(lastDato.length - 2, lastDato.length)
                Log.d("VAloresUltimoPAquete"," ------------------------------------indiceFinal $indiceFinal BanderaListaCompleta $BanderaListaCompleta ")
                    val valorNUevo = lastDato.substring(0, indiceFinal)
                    val tempListNUeva = mutableListOf<String>()
                    tempListNUeva.add(valorNUevo)
                    println(" \n valorNUevo $valorNUevo  BanderaListaCompleta $BanderaListaCompleta")

                    val ListaULtimoPAquete = dividirpaquetes18TIme(tempListNUeva)
                    ListaULtimoPAquete.map {
                       Log.d("FinalLIstaTime","ListaULtimoPAquete $ListaULtimoPAquete")
                        Log.d("RecorridoDELOsDatosSeparados"," ListaULtimoPAquete $it ${convertirHexAFecha(it.substring(0,8))}")
                    }



                    if (BanderaListaCompleta == "00") {
                        var indice = ValoresFiltrados.size
                        ValoresFiltrados.addAll(indice, ListaULtimoPAquete)

                    }
                    else if (BanderaListaCompleta != "00") {
                        // Lógica específica para BanderaListaCompleta "01"
                        val indiceFinal = getDecimal(lastDato.substring(lastDato.length - 4, lastDato.length - 2)) * 2
                        val BanderaListaCompleta = lastDato.substring(lastDato.length - 2, lastDato.length)

                        val valorNUevo = lastDato
                        val tempListNUeva = mutableListOf<String>()
                        tempListNUeva.add(valorNUevo)

                        var indice = ValoresFiltrados.size
                        ValoresFiltrados.addAll(indice, ListaULtimoPAquete)
                        val ListaULtimoPAquete = dividirpaquetes18TIme(tempListNUeva)

                    } else {
                        println("BanderaListaCompleta no es ni '00' ni '01', revisar datos.")
                    }



//                }
                var listaEntera = listaFinal.last().substring(listaFinal.last().length-2, listaFinal.last().length  )
                var indexLast = 0
                var ValoresToSeachListaFinalIncompletaDatosRecientes = mutableListOf<String>()
                var ValoresToSeachListaFinalIncompletaDatosViejos = mutableListOf<String>()

                Log.d("ValoresFiltrados","ValoresFiltrados ${ValoresFiltrados.size}  ${ValoresFiltrados.toString()}")

                ValoresFiltrados.map {
                    Log.d("RecorridoDELOsDatosSeparados"," reorganizados $it ${convertirHexAFecha(it.substring(0,8))}")
                }

                ///////////////////OBtener Los datos de evento
                if (ValoresFiltrados.isNotEmpty()){
                 //   if (!Event.isNullOrEmpty()) {

                        //LA FUNCION VEvent SEPARA LOS DATOS EN EL HEADER Y LOS DATOS EN BRUTO

                    val textEvento = listOf(
                        "666D9678666D973E02FFEFFE347C",
                        "666D986B666D993002FFEFFE347E",
                        "666D9A66666D9B3002FFEFFE347E",
                        "666D9C72666D9D3D02FFEFFE347E",
                        "666D9E6F666D9F3502FFEFFE347E",
                        "666DA070666DA13F02FFEFFE347D",
                        "666DA27A666DA34602FFEFFE347B",
                        "666DA487666DA55702FFEFFE347A",
                        "666DA69A666DA76302FFEFFE347B",
                        "666DA8A0666DA96602FFEFFE347D",
                        "666DAA94666DAB5902FFEFFE347D",
                        "666DAC92666DAD5C02FFEFFE347D",
                        "666DAE98666DAF5E02FFEFFE347C",
                        "666DB090666DB15A02FFEFFE347A",
                        "666DB28B666DB34902FFEFFE347A",
                        "666DB47A666DB53B02FFEFFE347C",
                        "666DB673666DB74102FFEFFE347C",
                        "666DB878666DB94202FFEFFE347C",
                        "666DBA89666DBB5202FFF0FE347A",
                        "666DBC93666DBD3B02FFEFFE347A",
                        "666DBD3B666DC44D03FFEDFE347B",
                        "666DC502666DC7CC020026FE3479",
                        "666DC8F9666DC9EF02FFF0FE347B",
                        "666DCB20666DCBF802FFEFFE347C",
                        "666DCD25666DCDF402FFEFFE347B",
                        "666DCF21666DCFF202FFEFFE347C",
                        "666DD121666DD1E602FFEFFE347B",
                        "666DD318666DD3E202FFEFFE3479",
                        "666DD512666DD5DA02FFEFFE347C",
                        "666DD711666DD7D402FFEFFE347B",
                        "666DD902666DD9CE02FFEFFE347C",
                        "666DDB06666DDBC802FFEFFE347C",
                        "666DDCFB666DDDBC02FFEFFE3479",
                        "666DDEEA666DDFB602FFEFFE347C",
                        "666DE0F0666DE1B402FFEFFE347C",
                        "666DE2EB666DE3B502FFEFFE347C",
                        "666DE4EA666DE5AE02FFF0FE347C",
                        "666DE6DB666DE7A002FFEFFE3479",
                        "666DE8D3666DE99802FFEFFE3479",
                        "666DEACB666DEB9302FFEFFE347C",
                        "666DECCB666DED9302FFEFFE347C",
                        "666DEEC4666DEF9102FFEFFE347C",
                        "666DF0D8666DF1A402FFEFFE347C",
                        "666DF2E1666DF3AD02FFEFFE347A",
                        "666DF4E6666DF5B402FFEFFE347B",
                        "666DF6F0666DF7B402FFEFFE347C",
                        "666DF8EF666DF9B502FFEFFE347C",
                        "666DFAE4666DFBA702FFEFFE347D",
                        "666DFCD7666DFD9D02FFEFFE347D",
                        "666DFED0666DFF9202FFEFFE347A",
                        "666E00D0666E01A102FFEFFE347C",
                        "666E02E0666E03AD02FFEFFE347D",
                        "666E04EB666E05B502FFEFFE347D",
                        "666E06F1666E07BA02FFEFFE347D",
                        "666E08F1666E09B902FFEFFE347D",
                        "666E0AEA666E0BB302FFEFFE347D",
                        "666E0CE4666E0DA502FFEFFE347D",
                        "666E0EDD666E0FAE02FFEFFE347B",
                        "666E10DF666E11A502FFEFFE347B",
                        "666E1211666E192203FFE3FE347D",
                        "666E19D7666E1CBC02002AFE347B",
                        "666E1DE9666E1EE202FFF0FE347A",
                        "666E200F666E20F202FFEFFE347B",
                        "666E221F666E22EF02FFEFFE347B",
                        "666E241C666E24EA02FFEFFE347B",
                        "666E261D666E26E602FFEFFE347E",
                        "666E2813666E28E302FFEFFE347E",
                        "666E2A18666E2AE402FFEFFE347E",
                        "666E2C17666E2CDD02FFEFFE347E",
                        "666E2E09666E2ED402FFEFFE347E",
                        "666E3001666E30D502FFF0FE347E",
                        "666E320B666E32D202FFEFFE347E",
                        "666E33FF666E34C802FFEFFE347B",
                        "666E35FB666E36B802FFEFFE347B",
                        "666E37E5666E38B202FFF0FE347B",
                        "666E39E9666E3AAF02FFEFFE347E",
                        "666E3BDC666E3C9E02FFEFFE347E",
                        "666E3DD0666E3E9102FFEFFE347D",
                        "666E3FBF666E408602FFEFFE347D",
                        "666E41BA666E427B02FFEFFE347D",
                        "666E43A8666E446F02FFEFFE347D",
                        "666E45A2666E466E02FFEFFE347B",
                        "666E47A2666E486C02FFEFFE347B",
                        "666E499F666E4A6702FFEFFE347E",
                        "666E4BA2666E4C6802FFEFFE347E",
                        "666E4DA0666E4E6C02FFF0FE347E",
                        "666E4F9E666E506302FFEFFE347E",
                        "666E51A0666E526902FFEFFE347E",
                        "666E53B1666E547B02FFEFFE347E",
                        "666E55B3666E568002FFEFFE347E",
                        "666E57BB666E588802FFEFFE347E",
                        "666E59C7666E5A9102FFEFFE347C",
                        "666E5BCA666E5C9402FFEFFE347C",
                        "666E5DC1666E5E8202FFEFFE347C",
                        "666E5FBA666E608202FFEFFE347F",
                        "666E61BA666E627E02FFEFFE347E",
                        "666E63AC666E647402FFEFFE347F",
                        "666E65AF666E667B02FFEFFE347E",
                        "666E66E1666E6DF303FFE3FE347F",
                        "666E6EA8666E7179020028FE347D",
                        "666E72A5666E73A102FFEFFE347D",
                        "666E74CF666E75AD02FFEFFE347D",
                        "666E76DA666E77B002FFEFFE347D",
                        "666E78E1666E79B202FFEFFE3480",
                        "666E7AE6666E7BAB02FFEFFE347F",
                        "666E7CD8666E7DA202FFEFFE347F",
                        "666E7ED0666E7F8F02FFEFFE347F",
                        "666E80C0666E818C02FFEFFE347F",
                        "666E82BB666E838002FFEFFE347C",
                        "666E84B3666E858002FFEFFE347C",
                        "666E86B1666E877402FFEFFE347D",
                        "666E88AE666E897B02FFEFFE347F",
                        "666E8AB5666E8B7F02FFEFFE3480",
                        "666E8CAE666E8D7202FFEFFE347D",
                        "666E8E9F666E8F6702FFEFFE347D",
                        "666E90A7666E917202FFEFFE347F",
                        "666E92A5666E936D02FFF0FE347F",
                        "666E949E666E956302FFEFFE347D",
                        "666E96A2666E976702FFEFFE3480",
                        "666E9896666E995F02FFEFFE347F",
                        "666E9A90666E9B5702FFEFFE347D",
                        "666E9C8B666E9D4E02FFEFFE347D",
                        "666E9E7E666E9F4002FFEFFE347F",
                        "666EA081666EA14A02FFF0FE3480",
                        "666EA281666EA34A02FFEFFE347E",
                        "666EA487666EA55002FFEFFE347D",
                        "666EA690666EA75702FFEFFE3480",
                        "666EA890666EA95902FFEFFE3480",
                        "666EAA93666EAB5C02FFEFFE347D",
                        "666EAC98666EAD5F02FFEFFE347D",
                        "666EAE99666EAF6702FFEFFE3480",
                        "666EB096666EB15A02FFEFFE3480",
                        "666EB28E666EB34F02FFEFFE3480",
                        "666EB482666EB54702FFEFFE3480",
                        "666EB682666EB74D02FFF0FE3480",
                        "666EB88A666EB95502FFEFFE3480",
                        "666EBA8D666EBB5002FFEFFE347E",
                        "666EBBAD666EC2BE03FFE3FE3480",
                        "666EC373666EC637020026FE347D",
                        "666EC764666EC85A02FFEFFE347E",
                        "666EC991666ECA7202FFEFFE3480",
                        "666ECBA2666ECC7302FFEFFE3480",
                        "666ECDA6666ECE6702FFEFFE3480",
                        "666ECF94666ED06902FFEFFE3480",
                        "666ED1A9666ED27302FFEFFE3480",
                        "666ED3AF666ED47002FFEFFE347E",
                        "666ED5A1666ED66A02FFEFFE347E",
                        "666ED7A3666ED86802FFEFFE347E",
                        "666ED994666EDA6102FFEFFE3480",
                        "666EDB92666EDC4B02FFEFFE3480",
                        "666EDD81666EDE4502FFEFFE3480",
                        "666EDF72666EE03902FFEFFE3480",
                        "666EE179666EE23902FFF0FE347F",
                        "666EE36F666EE43F02FFEFFE3480",
                        "666EE57F666EE64602FFEFFE347D",
                        "666EE77C666EE83C02FFEFFE347D",
                        "666EE96E666EEA3B02FFEFFE347F",
                        "666EEB80666EEC4202FFEFFE347F",
                        "666EED76666EEE3002FFEFFE347F",
                        "666EEF5D666EF01A02FFEFFE347F",
                        "666EF14B666EF20C02FFEFFE347E",
                        "666EF339666EF3FB02FFEFFE347D",
                        "666EF53C666EF60B02FFEFFE347D",
                        "666EF74D666EF80F02FFEFFE347D",
                        "666EF946666EFA0902FFEFFE347F",
                        "666EFB36666EFBF202FFEFFE347E",
                        "666EFD2E666EFDF802FFEFFE347F",
                        "666EFF39666F000802FFEFFE347E",
                        "666F0147666F020702FFEFFE347E",
                        "666F033B666F03FC02FFEFFE347E",
                        "666F0534666F05FA02FFEFFE347C",
                        "666F072B666F07EC02FFEFFE347C",
                        "666F0927666F09E902FFEFFE347C",
                        "666F0B26666F0BEF02FFEFFE347E",
                        "666F0D2E666F0DF302FFEFFE347E",
                        "666F0F36666F100202FFEFFE347D",
                        "666F1073666F178303FFE3FE347E",
                        "666F1838666F1AF8020026FE347B",
                        "666F1C25666F1D2402FFEFFE347A",
                        "666F1E56666F1F2D02FFEFFE347A",
                        "666F205A666F212A02FFEFFE347A",
                        "666F2259666F232902FFEFFE347D",
                        "666F2463666F252A02FFEFFE347D",
                        "666F2666666F272F02FFEFFE347D",
                        "666F2861666F292302FFEFFE347D",
                        "666F2A50666F2B1202FFEFFE347D",
                        "666F2C4A666F2D1002FFEFFE347A",
                        "666F2E41666F2F0302FFEFFE347B",
                        "666F3037666F30FD02FFEFFE347D",
                        "666F323E666F330302FFEFFE347C",
                        "666F3441666F34FE02FFEFFE347D",
                        "666F362D666F36E902FFEFFE347D",
                        "666F3818666F38DC02FFEFFE347D",
                        "666F3A13666F3ADF02FFEFFE347C",
                        "666F3C1E666F3CE402FFEFFE347D",
                        "666F3E22666F3EE002FFEFFE347E",
                        "666F4018666F40DB02FFEFFE347B",
                        "666F420E666F42D402FFEFFE347B",
                        "666F440C666F44D102FFEFFE347B",
                        "666F4614666F46E002FFEFFE347D",
                        "666F4825666F48EB02FFEFFE347D",
                        "666F4A28666F4AF602FFEFFE347D",
                        "666F4C3C666F4D0202FFEFFE347D",
                        "666F4E38666F4EFC02FFF0FE347D",
                        "666F5042666F511102FFEFFE347D",
                        "666F524F666F530A02FFEFFE347D",
                        "666F5443666F550B02FFEFFE347D",
                        "666F563F666F570602FFEFFE347D",
                        "666F5848666F591002FFEFFE347D",
                        "666F5A45666F5B0C02FFEFFE347A",
                        "666F5C4C666F5D1702FFEFFE347B",
                        "666F5E59666F5F2602FFEFFE347B",
                        "666F6064666F612502FFEFFE347B",
                        "666F625D666F632302FFEFFE347C",
                        "666F645C666F652302FFEFFE347D",
                        "666F653B666F6C4B03FFE7FE347E",
                        "666F6D00666F6FBA020026FE347B",
                        "666F70E7666F71E402FFF1FE347C",
                        "666F7311666F73F002FFF1FE347C",
                        "666F7520666F75EA02FFEFFE347B",
                        "666F7718666F77E802FFEFFE347E",
                        "666F7917666F79D902FFEFFE347E",
                        "666F7B06666F7BCE02FFEFFE347E",
                        "666F7D07666F7DC802FFEFFE347D",
                        "666F7EF5666F7FC102FFF0FE347B",
                        "666F80F3666F81AE02FFEFFE347B",
                        "666F82DB666F83A002FFEFFE347B",
                        "666F84CF666F858C02FFEFFE347E",
                        "666F86BC666F878302FFEFFE347E",
                        "666F88B6666F897302FFEFFE347E",
                        "666F8AA0666F8B6C02FFF0FE347F",
                        "666F8C9F666F8D6702FFEFFE347E",
                        "666F8EA0666F8F6602FFEFFE347E",
                        "666F909D666F915D02FFEFFE347B",
                        "666F928A666F934F02FFEFFE347B",
                        "666F947B666F953D02FFEFFE347D",
                        "666F967B666F973F02FFEFFE347D",
                        "666F987D666F994102FFEFFE347E",
                        "666F9A72666F9B3202FFEFFE347D",
                        "666F9C63666F9D2602FFEFFE347D",
                        "666F9E53666F9F1902FFEFFE347D",
                        "666FA05B666FA12702FFEFFE347B",
                        "666FA26B666FA33302FFEFFE347C",
                        "666FA460666FA52502FFEFFE347B",
                        "666FA65C666FA71E02FFEFFE347B",
                        "666FA852666FA91902FFEFFE347E",
                        "666FAA5D666FAB2002FFEFFE347E",
                        "666FAC4D666FAD1202FFEFFE347E",
                        "666FAE52666FAF1C02FFEFFE347E",
                        "666FB054666FB11A02FFEFFE347E",
                        "666FB249666FB30D02FFEFFE347F",
                        "666FB453666FB51B02FFEFFE347E",
                        "666FB64C666FB71502FFEFFE347C",
                        "666FB855666FB91902FFEFFE347D",
                        "666FB9FE666FC10E03FFE9FE3480",
                        "666FC1C3666FC4B402002CFE347D",
                        "666FC5E1666FC6E202FFF0FE347F",
                        "666FC813666FC8F302FFEFFE347F",
                        "666FCA27666FCAFC02FFEFFE347F",
                        "666FCC2A666FCCF502FFEFFE3480",
                        "666FCE22666FCEE702FFF0FE3480",
                        "666FD01A666FD0DE02FFEFFE347D",
                        "666FD20B666FD2D402FFEFFE347F",
                        "666FD405666FD4CC02FFEFFE3480",
                        "666FD604666FD6C802FFEFFE3480",
                        "666FD7F4666FD8B402FFEFFE347F",
                        "666FD9EA666FDAB002FFEFFE3480",
                        "666FDBEC666FDCB202FFEFFE347D",
                        "666FDDE8666FDEB002FFEFFE347E",
                        "666FDFE3666FE0A102FFEFFE347E",
                        "666FE1CF666FE29302FFEFFE347E",
                        "666FE3DB666FE4A502FFEFFE3480",
                        "666FE5DE666FE69F02FFEFFE347E",
                        "666FE7E2666FE8AA02FFEFFE3480",
                        "666FE9E3666FEAA902FFEFFE3480",
                        "666FEBEC666FECB102FFEFFE347E",
                        "666FEDE8666FEEAC02FFEFFE3480",
                        "666FEFF7666FF0C102FFF0FE3480",
                        "666FF20A666FF2D702FFEFFE3481",
                        "666FF41D666FF4E002FFF0FE347E",
                        "666FF619666FF6E302FFEFFE3480",
                        "666FF82B666FF8F102FFEFFE3480",
                        "666FFA2F666FFAF102FFEFFE3481",
                        "666FFC31666FFCF902FFEFFE347E",
                        "666FFE2F666FFEF102FFEFFE3480",
                        "66700035667000FA02FFEFFE3480",
                        "6670022C667002E702FFEFFE347E",
                        "66700424667004EA02FFEFFE347E",
                        "66700629667006EC02FFEFFE3480",
                        "66700830667008F902FFEFFE3480",
                        "66700A4266700B0602FFEFFE347E",
                        "66700C3D66700D0102FFEFFE3480",
                        "66700E4766700EBF02FFEFFE3480",
                        "66700EBF667015CF03FFF0FE3480",
                        "6670168466701951020026FE347E",
                        "66701A7E66701B6F02FFF0FE3480",
                        "66701CB366701D9102FFEFFE3480",
                        "66701EC266701F8B02FFEFFE3480",
                        "667020B86670218102FFEFFE3480",
                        "667022BA6670237C02FFEFFE347F",
                        "667024AC6670257502FFEFFE347D",
                        "667026AE6670276B02FFEFFE3480",
                        "667028986670295802FFEFFE347F",
                        "66702A9466702B4E02FFEFFE347E",
                        "66702C8966702D4902FFEFFE347F",
                        "66702E8366702F4802FFEFFE347F",
                        "6670308F6670315302FFEFFE347E",
                        "6670328F6670334F02FFEFFE347E",
                        "6670348A6670354902FFEFFE347E",
                        "6670368D6670374B02FFEFFE347D",
                        "667038806670394202FFEFFE347B",
                        "66703A8C66703B5602FFEFFE347B",
                        "66703C8C66703D4202FFEFFE347B",
                        "66703E6F66703F2702FFEFFE347D",
                        "667040686670412B02FFEFFE347D",
                        "667042636670432302FFEFFE347C",
                        "6670446E6670453402FFEFFE347C",
                        "667046766670474002FFEFFE347C",
                        "6670488A6670494F02FFEFFE347C",
                        "66704A8C66704B4F02FFEFFE347C",
                        "66704CA866704D7302FFEFFE347C",
                        "66704EB066704F7A02FFEFFE347C",
                        "667050CC6670518E02FFEFFE347C",
                        "667052D16670539402FFEFFE347A",
                        "667054C26670558002FFEFFE3479",
                        "667056F6667057C402FFEFFE347A",
                        "6670591A667059E102FFEFFE347C",
                        "66705B7066705C3902FFEFFE3479",
                        "66705DA666705E6702FFEFFE347B",
                        "66705FD6667060A102FFEFFE347B",
                        "66706201667062C402FFEFFE347B",
                        "6670638266706A9203FFE2FE347B",
                        "66706B4766706E06020026FE3478",
                        "66706F38667071AB02FFEFFE347A",
                        "66706FBF667071AC040001FE347A",
                        "667071C266707341020002FE3477",
                        "667074906670756302FFEFFE3479",
                        "667076A16670776D02FFEFFE3478",
                        "667078A96670797202FFEFFE347A",
                        "66707AB666707B7B02FFEFFE347A",
                        "66707CB666707D8202FFEFFE3478",
                        "66707EC566707F8F02FFEFFE347A",
                        "667080D9667081A102FFEFFE3478",
                        "667082CE6670839702FFEFFE347A",
                        "667084C96670858B02FFEFFE347A",
                        "667086C16670878402FFEFFE3478",
                        "667088C46670898B02FFEFFE347A",
                        "66708AB866708B7E02FFEFFE3478",
                        "66708CBA66708D8302FFEFFE347A",
                        "66708EB066708F7702FFF0FE3478",
                        "667090A76670916C02FFEFFE347A",
                        "667092A36670936C02FFEFFE347A",
                        "667094A56670957602FFEFFE3478",
                        "667096AE6670977602FFEFFE347A",
                        "667098A36670996C02FFEFFE3478",
                        "66709A9866709B6A02FFEFFE347A",
                        "66709C9E66709D6B02FFEFFE347A",
                        "66709E9B66709F6302FFEFFE3478",
                        "6670A0906670A15C02FFEFFE347A",
                        "6670A2906670A35702FFF0FE347A",
                        "6670A4836670A54F02FFEFFE3479",
                        "6670A6836670A74E02FFF0FE3477",
                        "6670A8826670A94F02FFEFFE347A",
                        "6670AA826670AB4B02FFEFFE3479",
                        "6670AC7A6670AD4002FFEFFE3477",
                        "6670AE6D6670AF3C02FFEFFE347A",
                        "6670B06D6670B13602FFEFFE3478",
                        "6670B2636670B32F02FFEFFE3479",
                        "6670B4716670B54402FFEFFE3479",
                        "6670B6816670B75102FFEFFE3479",
                        "6670B87E6670B94A02FFEFFE347A",
                        "6670BA7A6670BB4802FFEFFE3478",
                        "6670BC7D6670BD3B02FFEFFE347A",
                        "6670BE6A6670BF3F02FFEFFE347A",
                        "6670C0726670C13702FFEFFE3478",
                        "6670C2646670C33002FFEFFE347A",
                        "6670C4656670C53302FFEFFE347A",
                        "6670C6626670C68102FFEFFE3478",
                        "6670C6816670CD9303FFF1FE3479",
                        "6670CE486670D174020032FE3479",
                        "6670D2A16670D3AD02FFF1FE3479",
                        "6670D4DA6670D5CD02FFF1FE347C",
                        "6670D7056670D7D802FFEFFE347C",
                        "6670D9066670D9CF02FFEFFE347C",
                        "6670DAFD6670DBC602FFEFFE347A",
                        "6670DCF36670DDB402FFEFFE347C",
                        "6670DEEE6670DFB802FFEFFE347B",
                        "6670E0ED6670E1B502FFEFFE347D",
                        "6670E2F96670E3CA02FFEFFE347C",
                        "6670E50C6670E5D102FFEFFE347B",
                        "6670E71B6670E7E202FFEFFE347D",
                        "6670E9126670E9D402FFEFFE347D",
                        "6670EB136670EBE002FFEFFE347B",
                        "6670ED166670EDDD02FFEFFE347D",
                        "6670EF176670EFE002FFEFFE347D",
                        "6670F1126670F1DC02FFEFFE347E",
                        "6670F3096670F3D202FFEFFE347B",
                        "6670F5056670F5D702FFEFFE347E",
                        "6670F7076670F7CF02FFEFFE347E",
                        "6670F9086670F9DA02FFEFFE347C",
                        "6670FB126670FBDC02FFEFFE347E",
                        "6670FD0B6670FDDA02FFEFFE347E",
                        "6670FF0B6670FFD802FFEFFE347E",
                        "66710116667101E702FFEFFE347B",
                        "66710324667103EB02FFEFFE347E",
                        "6671051F667105E702FFEFFE347F",
                        "6671071B667107E502FFEFFE347F",
                        "66710912667109D902FFEFFE347E",
                        "66710B0666710BD202FFEFFE347D",
                        "66710CFF66710DC002FFEFFE347E",
                        "66710EED66710FB102FFEFFE347E",
                        "667110DE667111A802FFEFFE347C",
                        "667112DA667113AB02FFEFFE347C",
                        "667114E6667115AD02FFEFFE347C",
                        "667116E1667117AD02FFEFFE347F",
                        "667118ED667119C002FFEFFE347F",
                        "66711AFD66711B5202FFEFFE347F",
                        "66711B526671226403FFF1FE347E",
                        "667123196671260D02002CFE347B",
                        "6671273A6671284102FFF1FE347F",
                        "6671296E66712A5B02FFF0FE347E",
                        "66712B8866712C5D02FFEFFE347F",
                        "66712D8966712E5802FFF0FE347F",
                        "66712F856671305002FFEFFE347F",
                        "6671317D6671324802FFEFFE347D",
                        "667133786671343F02FFEFFE347D",
                        "6671356F6671363902FFEFFE347F",
                        "667137706671383A02FFEFFE347F",
                        "6671397866713A4202FFEFFE347F",
                        "66713B6F66713C3302FFEFFE347F",
                        "66713D6066713E2B02FFEFFE347C",
                        "66713F616671402502FFEFFE347F",
                        "667141596671421C02FFF0FE347F",
                        "667143496671440802FFEFFE347F",
                        "66714535667145FB02FFEFFE347D",
                        "66714737667147FD02FFEFFE347F",
                        "6671492A667149EE02FFEFFE347F",
                        "66714B1F66714BE502FFEFFE3480",
                        "66714D1566714DD902FFEFFE347D",
                        "66714F1766714FE202FFEFFE347F",
                        "6671511B667151E702FFEFFE347F",
                        "66715321667153E502FFEFFE347D",
                        "66715518667155E302FFF0FE3480",
                        "6671572F667157F502FFEFFE347D",
                        "66715925667159E802FFEFFE347F",
                        "66715B2466715BF502FFEFFE347F",
                        "66715D3966715DFD02FFEFFE347D",
                        "66715F3066715FF302FFEFFE347F",
                        "66716128667161F102FFEFFE347D",
                        "66716330667163F402FFEFFE3480",
                        "66716531667165F102FFF1FE347F",
                        "66716724667167EC02FFEFFE347F",
                        "66716929667169ED02FFEFFE347F",
                        "66716B1A66716BE302FFEFFE347D",
                        "66716D3066716DFE02FFEFFE347F",
                        "66716F4166716FFF02FFEFFE347E",
                        "6671701C6671772D03FFE7FE347F",
                        "667177E266717A9B020025FE347C",
                        "66717BC866717CC902FFF1FE347E",
                        "66717DF866717ED202FFEFFE347E",
                        "6671800B667180DA02FFEFFE347E",
                        "66718208667182D502FFEFFE347E",
                        "66718415667184E602FFEFFE347D",
                        "66718631667186F602FFEFFE347B",
                        "66718826667188F102FFEFFE347B",
                        "66718A2266718AE302FFEFFE347D",
                        "66718C2166718CE402FFEFFE347D",
                        "66718E1B66718EDD02FFEFFE347D",
                        "6671901E667190E402FFF0FE347C",
                        "66719220667192E602FFEFFE347A",
                        "66719425667194EB02FFEFFE347D",
                        "66719629667196F202FFEFFE347C",
                        "66719836667198FA02FFEFFE347C",
                        "66719A3866719B0002FFEFFE347B",
                        "66719C3866719CFD02FFEFFE347C",
                        "66719E3566719F0102FFEFFE347A",
                        "6671A04E6671A11502FFEFFE3479",
                        "6671A2566671A31C02FFEFFE347B",
                        "6671A45C6671A51902FFEFFE347B",
                        "6671A6536671A71902FFEFFE347B",
                        "6671A87C6671A94202FFEFFE347B",
                        "6671AA976671AB5C02FFEFFE3478",
                        "6671ACC36671AD8902FFEFFE347B",
                        "6671AEEB6671AFB502FFEFFE347B",
                        "6671B1136671B1DF02FFEFFE3478",
                        "6671B3526671B41C02FFEFFE347A",
                        "6671B56B6671B61802FFEFFE3478",
                        "6671B7536671B81002FFEFFE347A",
                        "6671B9716671BA3202FFEFFE347A",
                        "6671BB716671BC2702FFEFFE3478",
                        "6671BD626671BE2102FFEFFE347B",
                        "6671BF736671C03802FFEFFE347A",
                        "6671C1766671C23602FFEFFE347B",
                        "6671C37D6671C43B02FFEFFE347A",
                        "6671C4E26671CBF203FFE5FE347A",
                        "6671CCA76671CF9702002BFE3477",
                        "6671D0C36671D1C102FFF0FE3477",
                        "6671D2EE6671D3D802FFF0FE3477",
                        "6671D5046671D5E102FFEFFE3477",
                        "6671D70E6671D7DE02FFEFFE3479",
                        "6671D90A6671D9D902FFF1FE3477",
                        "6671DB066671DBDA02FFF1FE347A",
                        "6671DD076671DDDE02FFF0FE3477",
                        "6671DF0B6671DFD502FFF0FE347A",
                        "6671E1026671E1D102FFF0FE3479",
                        "6671E2FE6671E3D002FFF0FE3478",
                        "6671E4FD6671E5CD02FFEFFE3479",
                        "6671E6FC6671E7CF02FFF0FE347A",
                        "6671E8FC6671E9D102FFF0FE3477",
                        "6671EAFE6671EBD002FFEFFE347A",
                        "6671ECFD6671EDCF02FFF1FE3477",
                        "6671EEFB6671EFD402FFF1FE347A",
                        "6671F1046671F1D602FFEFFE347A",
                        "6671F3036671F3D102FFEFFE3477",
                        "6671F4FE6671F5D402FFF0FE3479",
                        "6671F7016671F7DF02FFF1FE3476",
                        "6671F90D6671F9E202FFEFFE3477",
                        "6671FB0F6671FBDF02FFEFFE347A",
                        "6671FD0C6671FDEA02FFF0FE3477",
                        "6671FF176671FFEB02FFEFFE347A",
                        "66720117667201F202FFF1FE347A",
                        "66720324667203F402FFEFFE3477",
                        "66720520667205FF02FFF0FE3479",
                        "6672072D6672080502FFEFFE3477",
                        "6672093266720A0702FFF0FE347A",
                        "66720B3566720C0C02FFEFFE347A",
                        "66720D3966720E1202FFF0FE3477",
                        "66720F3F6672101302FFEFFE347A",
                        "667211406672121702FFF0FE3477",
                        "667213436672141F02FFF1FE347A",
                        "6672154D6672162002FFEFFE347B",
                        "6672174D6672182302FFEFFE3478",
                        "66721950667219A902FFF1FE347B",
                        "667219A9667220BA03FFF2FE347A",
                        "6672216E667224A2020032FE3479",
                        "667225CF667226E702FFF1FE3479",
                        "66722814667228FF02FFF1FE347C",
                        "66722A2C66722B0802FFEFFE347C",
                        "66722C3566722D1702FFF1FE347C",
                        "66722E4866722F1502FFEFFE347C",
                        "667230426672311302FFF0FE347A",
                        "667232406672330E02FFF0FE347C",
                        "667234426672350C02FFEFFE347C",
                        "667236396672370602FFF0FE347C",
                        "667238326672390802FFEFFE347A",
                        "66723A3566723B0202FFEFFE347C",
                        "66723C2F66723D0802FFF0FE347C",
                        "66723E3566723F1202FFF1FE347B",
                        "6672403F6672411C02FFF0FE347D",
                        "667242496672431A02FFEFFE347D",
                        "667244466672451C02FFEFFE347B",
                        "667246496672472102FFF1FE347D",
                        "6672484E6672492602FFF1FE347A",
                        "66724A5366724B2B02FFEFFE347D",
                        "66724C5966724D2202FFEFFE347D",
                        "66724E4F66724F2702FFF1FE347B",
                        "667250556672512502FFEFFE347E",
                        "667252526672532002FFEFFE347D",
                        "6672544D6672552702FFF1FE347B",
                        "667256546672572002FFEFFE347E",
                        "6672584D6672591D02FFEFFE347B",
                        "66725A4966725B1F02FFF0FE347E",
                        "66725C4C66725D1B02FFF0FE347E",
                        "66725E4866725F1802FFF0FE347B",
                        "667260456672611802FFEFFE347E",
                        "667262456672631202FFEFFE347E",
                        "6672643F6672651102FFEFFE347E",
                        "6672663D6672670302FFEFFE347B",
                        "667268306672690802FFF0FE347E",
                        "66726A3766726B0002FFEFFE347E",
                        "66726C2D66726D0402FFF1FE347B",
                        "66726E3166726E7502FFEFFE347E",
                        "66726E756672758703FFF1FE347D",
                        "6672763B6672794702002FFE347C",
                        "66727A7466727B8002FFF1FE347E",
                        "66727CAD66727DA402FFF1FE347F",
                        "66727ED166727FAA02FFF0FE347C",
                        "667280D7667281AB02FFF0FE347F",
                        "667282D8667283B102FFF1FE347F",
                        "667284DD667285A502FFEFFE347D",
                        "667286D2667287A202FFF0FE347F",
                        "667288CE667289A502FFF0FE347D",
                        "66728AD366728B9802FFEFFE347F",
                        "66728CC566728D9102FFEFFE3480",
                        "66728EBE66728F8602FFEFFE347D",
                        "667290B46672918002FFF0FE347F",
                        "667292AC6672937D02FFEFFE347D",
                        "667294B46672957B02FFEFFE3480",
                        "667296A86672977202FFEFFE3480",
                        "6672989F6672996202FFEFFE347D",
                        "66729A8F66729B5F02FFEFFE3480",
                        "66729C8C66729D4E02FFEFFE347C",
                        "66729E7B66729F4402FFF0FE347F",
                        "6672A0716672A13902FFEFFE347F",
                        "6672A2686672A33A02FFF0FE347C",
                        "6672A4776672A54402FFEFFE3480",
                        "6672A6786672A73E02FFEFFE347D",
                        "6672A86F6672A93F02FFEFFE347F",
                        "6672AA786672AB3F02FFEFFE347F",
                        "6672AC6E6672AD4002FFF0FE347D",
                        "6672AE6D6672AF2F02FFEFFE347F",
                        "6672B05C6672B12402FFEFFE347D",
                        "6672B2646672B32E02FFEFFE347F",
                        "6672B45B6672B52302FFEFFE347F",
                        "6672B6506672B71002FFEFFE347D",
                        "6672B83F6672B90802FFEFFE347F",
                        "6672BA3A6672BB0E02FFEFFE347C",
                        "6672BC506672BD1A02FFEFFE347F",
                        "6672BE476672BF0C02FFF0FE347F",
                        "6672C0396672C0FF02FFF0FE347D",
                        "6672C2346672C30202FFEFFE347F",
                        "6672C3456672CA5703FFE3FE347F",
                        "6672CB0C6672CDD9020026FE347B",
                        "6672CF056672D00402FFF1FE347B",
                        "6672D1316672D21902FFF1FE347D",
                        "6672D3516672D41702FFF0FE347D",
                        "6672D5456672D61A02FFF0FE347B",
                        "6672D7486672D80B02FFEFFE347D",
                        "6672D9436672DA0B02FFEFFE347A",
                        "6672DB3C6672DBFC02FFEFFE347D",
                        "6672DD2A6672DDF402FFF0FE347C",
                        "6672DF2A6672DFEC02FFEFFE347C",
                        "6672E11E6672E1E802FFEFFE347C",
                        "6672E3176672E3D302FFEFFE3479",
                        "6672E50E6672E5D402FFEFFE347C",
                        "6672E7016672E7C402FFEFFE3478",
                        "6672E8FA6672E9C002FFEFFE347B",
                        "6672EAF16672EBB302FFEFFE347B",
                        "6672ECE06672EDA202FFEFFE3478",
                        "6672EEDE6672EFA402FFEFFE347C",
                        "6672F0E26672F1AA02FFEFFE3478",
                        "6672F2DB6672F39502FFEFFE347A",
                        "6672F4C26672F58302FFEFFE347A",
                        "6672F6BE6672F78002FFEFFE347B",
                        "6672F8AE6672F97002FFEFFE347B",
                        "6672FA9E6672FB6002FFEFFE3478",
                        "6672FC996672FD5C02FFF0FE347B",
                        "6672FE946672FF5902FFF0FE347B",
                        "6673009B6673016602FFEFFE347B",
                        "667302A56673036F02FFF1FE347B",
                        "667304AF6673057702FFF0FE347B",
                        "667306BC6673077502FFEFFE347A",
                        "667308C96673099F02FFEFFE347A",
                        "66730AFD66730BC702FFEFFE347A",
                        "66730D1966730DE902FFEFFE3477",
                        "66730F3E6673100102FFF0FE3479",
                        "6673113C6673120502FFEFFE3477",
                        "667313416673140C02FFEFFE3479",
                        "6673154C6673161802FFEFFE3477",
                        "667317586673181802FFEFFE347A",
                        "6673181866731F2903FFEAFE3478",
                        "66731FDE667322B6020027FE3477",
                        "667323E2667324EB02FFF1FE3479",
                        "6673261A667326FB02FFEFFE3479",
                        "6673282C667328F802FFEFFE3477",
                        "66732A2B66732AFD02FFEFFE3479",
                        "66732C2A66732CFB02FFEFFE347A",
                        "66732E3A66732F0402FFEFFE3476",
                        "667330446673311002FFEFFE347A",
                        "667332556673331C02FFEFFE3479",
                        "6673345E6673352A02FFEFFE3477",
                        "667336796673374402FFEFFE3479",
                        "667338786673393E02FFEFFE3477",
                        "66733A8066733B4602FFEFFE347A",
                        "66733C7E66733D4102FFEFFE3479",
                        "66733E8166733F4802FFEFFE3477",
                        "667340876673415802FFEFFE347A",
                        "667342906673435202FFEFFE347A",
                        "667344876673454C02FFEFFE3477",
                        "667346796673474602FFEFFE347A",
                        "667348736673493302FFEFFE347A",
                        "66734A6366734B2B02FFEFFE3479",
                        "66734C7366734D3C02FFEFFE347A",
                        "66734E8566734F4D02FFEFFE3477",
                        "6673509C6673516802FFEFFE3477",
                        "6673529D6673536B02FFEFFE3479",
                        "667354A76673556E02FFEFFE347A",
                        "667356B06673577502FFEFFE3479",
                        "667358B66673598202FFEFFE3477",
                        "66735ADA66735BA802FFEFFE3477",
                        "66735CED66735DB002FFEFFE347A",
                        "66735EF266735FC002FFEFFE347A",
                        "66736106667361CA02FFEFFE347A",
                        "6673630F667363D902FFEFFE3478",
                        "66736516667365D702FFEFFE347B",
                        "6673670E667367DC02FFEFFE347B",
                        "66736933667369FC02FFEFFE347B",
                        "66736B4566736C1502FFEFFE3478",
                        "66736CE5667373F603FFE5FE347B",
                        "667374AB6673777B02002DFE3479",
                        "667378A76673799502FFEFFE347C",
                        "66737AE666737BBF02FFEFFE3479",
                        "66737D1D66737DEA02FFEFFE347C",
                        "66737F426673800C02FFEFFE347C",
                        "667381796673824002FFEFFE347A",
                        "667383A96673846E02FFEFFE347C",
                        "667385D16673868A02FFEFFE347A",
                        "667387E2667388AD02FFEFFE347D",
                        "66738A3466738AFD02FFEFFE347A",
                        "66738C5D66738D1E02FFEFFE347D",
                        "66738E9F66738F6502FFEFFE347A",
                        "667390D06673919302FFEFFE347D",
                        "667392E3667393A802FFEFFE347D",
                        "667394E9667395AE02FFEFFE347B",
                        "667396F5667397BC02FFEFFE347D",
                        "667398EF667399B202FFEFFE347D",
                        "66739AE166739B9A02FFEFFE347B",
                        "66739CD066739D9502FFEFFE347D",
                        "66739EC966739F8D02FFEFFE347D",
                        "6673A0C06673A18302FFF0FE347B",
                        "6673A2B56673A37D02FFEFFE347E",
                        "6673A4AB6673A56C02FFEFFE347E",
                        "6673A6986673A75E02FFEFFE347C",
                        "6673A8916673A95402FFEFFE347E",
                        "6673AA826673AB4902FFEFFE347E",
                        "6673AC7B6673AD4002FFF0FE347B",
                        "6673AE6E6673AF3602FFEFFE347E",
                        "6673B0636673B13002FFEFFE347B",
                        "6673B2606673B32702FFEFFE347E",
                        "6673B4616673B52D02FFF0FE347D",
                        "6673B65A6673B72102FFEFFE347B",
                        "6673B8576673B92302FFF0FE347E",
                        "6673BA526673BB1902FFEFFE347E",
                        "6673BC4D6673BD1702FFF1FE347C",
                        "6673BE526673BF1902FFEFFE347E",
                        "6673C0466673C11002FFEFFE347E",
                        "6673C1AE6673C8BF03FFE5FE347E",
                        "6673C9746673CC6602002AFE347B",
                        "6673CD936673CE8D02FFEFFE347F",
                        "6673CFBA6673D0A702FFF0FE347F",
                        "6673D1D46673D2AB02FFEFFE347F",
                        "6673D3D86673D4B502FFF1FE347F",
                        "6673D5E36673D6AE02FFEFFE347F",
                        "6673D7DB6673D8AB02FFF0FE347F",
                        "6673D9DA6673DAA902FFEFFE347C",
                        "6673DBD76673DC9F02FFF0FE347C",
                        "6673DDCB6673DE9902FFF0FE347C",
                        "6673DFC66673E08B02FFF0FE347F",
                        "6673E1BC6673E28602FFEFFE347F",
                        "6673E3B36673E47702FFEFFE347F",
                        "6673E5A46673E67202FFEFFE347F",
                        "6673E79F6673E86602FFEFFE347F",
                        "6673E9936673EA5B02FFEFFE347D",
                        "6673EB876673EC4F02FFEFFE347D",
                        "6673ED7C6673EE4702FFF0FE347F",
                        "6673EF746673F03302FFEFFE3480",
                        "6673F1606673F22602FFF0FE347F",
                        "6673F3536673F41C02FFF1FE3480",
                        "6673F54B6673F60E02FFEFFE347D",
                        "6673F73B6673F80C02FFF0FE347D",
                        "6673F93D6673F9FF02FFEFFE3480",
                        "6673FB2E6673FBF502FFF0FE3480",
                        "6673FD2D6673FDF702FFEFFE3480",
                        "6673FF246673FFF002FFF0FE347F",
                        "6674011C667401E002FFF0FE347F",
                        "6674030F667403DE02FFF0FE347F",
                        "6674051C667405E002FFEFFE3480",
                        "6674070D667407DD02FFF0FE347D",
                        "6674090D667409CB02FFF0FE347F",
                        "66740AF866740BC802FFF0FE3480",
                        "66740CFC66740DC602FFEFFE347F",
                        "66740EF966740FBE02FFEFFE347F",
                        "667410EC667411B002FFEFFE347F",
                        "667412E2667413A802FFEFFE347C",
                        "667414D76674159D02FFEFFE347D",
                        "6674167C66741D8C03FFE9FE347F",
                        "66741E416674212002002AFE347B",
                        "6674224D6674235A02FFF1FE347B",
                        "667424896674256602FFEFFE347B",
                        "667426926674277602FFF0FE347B",
                        "667428A36674296D02FFEFFE347B",
                        "66742A9A66742B6902FFEFFE347C",
                        "66742C9666742D5502FFEFFE347C",
                        "66742E8266742F5402FFF0FE347C",
                        "667430846674314902FFEFFE347D",
                        "6674327A6674334102FFEFFE347D",
                        "667434796674354102FFEFFE347A",
                        "6674366E6674372F02FFEFFE347C",
                        "667438606674392A02FFEFFE347C",
                        "66743A5666743B1702FFEFFE3479",
                        "66743C4D66743D1C02FFEFFE347C",
                        "66743E5566743F1702FFEFFE3479",
                        "667440466674410C02FFEFFE347B",
                        "667442426674430402FFEFFE347B",
                        "667444406674450402FFF0FE3479",
                        "66744631667446F902FFEFFE347B",
                        "66744832667448FE02FFEFFE3479",
                        "66744A2F66744AEE02FFF0FE347A",
                        "66744C2A66744CEB02FFEFFE3478",
                        "66744E2B66744EED02FFEFFE347A",
                        "6674504D6674511402FFEFFE347A",
                        "667452856674534802FFEFFE3477",
                        "667454A26674556A02FFEFFE347A",
                        "667456AE6674576C02FFEFFE3477",
                        "667458B66674597C02FFEFFE347A",
                        "66745AD966745BAA02FFEFFE347A",
                        "66745D2766745DE902FFEFFE3477",
                        "66745F4E6674600B02FFEFFE347A",
                        "6674617A6674624602FFEFFE3477",
                        "6674639C6674645F02FFEFFE3479",
                        "667465CD6674669B02FFEFFE347A",
                        "667467FC667468B602FFEFFE3477",
                        "66746A2166746AE902FFEFFE3479",
                        "66746B416674725203FFE1FE3479",
                        "66747307667475AF020022FE3477",
                        "667476E8667477D602FFEFFE3477",
                        "6674792666747A0102FFEFFE3479",
                        "66747B4966747C1F02FFF0FE3479",
                        "66747D6066747E2702FFEFFE3477",
                        "66747F686674803502FFEFFE3478",
                        "667481786674824D02FFEFFE3478",
                        "6674839B6674846702FFEFFE3477",
                        "667485A16674866B02FFEFFE3479",
                        "667487AB6674886C02FFEFFE3477",
                        "6674899F66748A6A02FFEFFE347A",
                        "66748BA366748C6902FFEFFE3479",
                        "66748DAC66748E7702FFEFFE3477",
                        "66748FA46674906A02FFEFFE3479",
                        "667491A16674926202FFEFFE3479",
                        "667493A86674947602FFEFFE3477",
                        "667495BB6674967802FFEFFE3479",
                        "667497B56674988102FFEFFE3477",
                        "667499C466749A8E02FFF0FE347A",
                        "66749BDA66749CA102FFEFFE3479",
                        "66749DEC66749EB202FFEFFE3476",
                        "66749FE06674A0A902FFEFFE347A",
                        "6674A1F76674A2C402FFEFFE347A",
                        "6674A4196674A4E102FFEFFE3477",
                        "6674A6196674A6E202FFF0FE347A",
                        "6674A8316674A8F902FFF0FE3479",
                        "6674AA446674AB0D02FFEFFE3479",
                        "6674AC676674AD3102FFEFFE3479",
                        "6674AE6F6674AF2D02FFF0FE3477",
                        "6674B0646674B12D02FFEFFE347A",
                        "6674B26E6674B32F02FFEFFE347A",
                        "6674B4736674B53802FFEFFE3477",
                        "6674B6836674B74E02FFF0FE3479",
                        "6674B89D6674B96B02FFEFFE3479",
                        "6674BAB26674BB7502FFEFFE3478",
                        "6674BCBE6674BD8802FFEFFE347B",
                        "6674BEEC6674BFBD02FFEFFE3478",
                        "6674C00D6674C71E03FFE3FE347B",
                        "6674C7D36674CA8C020023FE3478",
                        "6674CBB86674CC9402FFEFFE347B",
                        "6674CDD56674CEA902FFEFFE347B",
                        "6674CFE56674D0B802FFEFFE347B",
                        "6674D1F66674D2B402FFEFFE3478",
                        "6674D3FB6674D4C202FFEFFE347C",
                        "6674D6166674D6DE02FFEFFE347C",
                        "6674D82E6674D8FA02FFF0FE347C",
                        "6674DA5E6674DB2202FFF0FE347C",
                        "6674DC816674DD4C02FFEFFE347A",
                        "6674DEA06674DF6802FFEFFE347C",
                        "6674E0D36674E19802FFF0FE347C",
                        "6674E2F86674E3B402FFEFFE347A",
                        "6674E50A6674E5CC02FFEFFE347D",
                        "6674E71C6674E7E102FFEFFE347A",
                        "6674E9156674E9DA02FFEFFE347C",
                        "6674EB1B6674EBE102FFEFFE347D",
                        "6674ED126674EDD702FFEFFE347B",
                        "6674EF136674EFD802FFEFFE347D",
                        "6674F1126674F1D302FFEFFE347B",
                        "6674F3096674F3CC02FFEFFE347B",
                        "6674F5066674F5D302FFEFFE347D",
                        "6674F7176674F7E102FFF1FE347D",
                        "6674F9106674F9CF02FFEFFE347B",
                        "6674FB106674FBD802FFEFFE347D",
                        "6674FD046674FDC302FFEFFE347D",
                        "6674FEF16674FFB202FFEFFE347B",
                        "667500E8667501AE02FFEFFE347D",
                        "667502E5667503AF02FFEFFE347D",
                        "667504E4667505AB02FFEFFE347B",
                        "667506DB6675079402FFEFFE347B",
                        "667508C06675098A02FFEFFE347E",
                        "66750ACC66750B8E02FFF0FE347E",
                        "66750CBB66750D8102FFEFFE347B",
                        "66750EC066750F8602FFEFFE347D",
                        "667510C46675118B02FFEFFE347D",
                        "667512CD6675139802FFEFFE347C",
                        "667514CC667514D702FFF0FE347E",
                        "667514D766751BE803FFEFFE347D",
                        "66751C9D66751FA002002FFE347B",
                        "667520CD667521E002FFF1FE347F",
                        "6675230C667523EC02FFF0FE347C",
                        "6675251D667525EB02FFEFFE347E",
                        "66752719667527EB02FFF0FE347F",
                        "66752925667529EA02FFEFFE347D",
                        "66752B1766752BDB02FFEFFE347F",
                        "66752D0766752DCF02FFEFFE347F",
                        "66752EFD66752FC002FFEFFE347D",
                        "667530F2667531AF02FFEFFE347F",
                        "667532DC667533A502FFEFFE347F",
                        "667534DC6675359402FFEFFE347D",
                        "667536C16675378B02FFEFFE347F",
                        "667538C86675398702FFEFFE347F",
                        "66753AC766753B9202FFF1FE347F",
                        "66753CC766753D8D02FFEFFE347F",
                        "66753EC066753F7902FFEFFE347D",
                        "667540A76675416902FFEFFE347F",
                        "667542A56675436D02FFEFFE3480",
                        "667544A56675456702FFEFFE347D",
                        "6675469F6675475E02FFEFFE347F",
                        "6675488B6675494A02FFEFFE347F",
                        "66754A8666754B4E02FFEFFE347D",
                        "66754C9666754D5702FFEFFE3480",
                        "66754E8466754F4402FFEFFE347F",
                        "667550836675514002FFF0FE347D",
                        "667552726675533802FFF0FE347F",
                        "667554756675553402FFEFFE3480",
                        "667556756675573E02FFEFFE347D",
                        "667558786675593E02FFEFFE3480",
                        "66755A8066755B3A02FFEFFE3480",
                        "66755C7A66755D3D02FFF0FE347D",
                        "66755E7366755F3102FFEFFE347F",
                        "6675606B6675613102FFEFFE347F",
                        "667562786675633902FFEFFE347C",
                        "6675646C6675652902FFEFFE347F",
                        "667566646675672002FFEFFE347F",
                        "667568506675690B02FFEFFE347D",
                        "667569A2667570B203FFE5FE347F",
                        "6675716766757426020026FE347B",
                        "667575536675764702FFEFFE347D",
                        "667577836675785802FFEFFE347E",
                        "6675798F66757A5E02FFEFFE347B",
                        "66757B8E66757C4F02FFF0FE347D",
                        "66757D7C66757E3702FFF0FE347D",
                        "66757F646675802A02FFEFFE347A",
                        "667581776675823F02FFEFFE347C",
                        "667583776675843402FFEFFE347C",
                        "667585656675861D02FFEFFE347B",
                        "667587536675881702FFEFFE347C",
                        "6675895166758A0F02FFF0FE347C",
                        "66758B4666758BF702FFEFFE347A",
                        "66758D2466758DDF02FFEFFE347C",
                        "66758F1E66758FDF02FFEFFE347A",
                        "6675911D667591DD02FFEFFE347D",
                        "6675931D667593E202FFEFFE347C",
                        "66759532667595F402FFEFFE3479",
                        "66759735667597FD02FFEFFE347C",
                        "6675993A667599F302FFEFFE347B",
                        "66759B2C66759BED02FFEFFE3479",
                        "66759D2B66759DEA02FFEFFE347B",
                        "66759F5E6675A02602FFEFFE3479",
                        "6675A1AA6675A26D02FFEFFE347A",
                        "6675A3F56675A4BA02FFEFFE347A",
                        "6675A63F6675A70802FFEFFE347A",
                        "6675A88C6675A95102FFEFFE347A",
                        "6675AADB6675ABA302FFEFFE347A",
                        "6675AD0D6675ADCF02FFEFFE347B",
                        "6675AF326675AFF702FFEFFE3478",
                        "6675B1696675B23B02FFF0FE347A",
                        "6675B3A76675B46C02FFEFFE3478",
                        "6675B5DD6675B6A402FFEFFE3479",
                        "6675B81C6675B8E802FFEFFE3478",
                        "6675BA6B6675BB3102FFEFFE347A",
                        "6675BCBF6675BD8302FFEFFE3477",
                        "6675BE6E6675C57F03FFE4FE347A",
                        "6675C6346675C8BF020022FE3478",
                        "6675CA296675CB0902FFEFFE347A",
                        "6675CC646675CD3802FFEFFE347A",
                        "6675CEA16675CF7502FFEFFE347A",
                        "6675D0E36675D1A102FFEFFE347A",
                        "6675D3186675D3D702FFEFFE347A",
                        "6675D5596675D61A02FFEFFE347A",
                        "6675D77A6675D83502FFEFFE347A",
                        "6675D9B96675DA7902FFEFFE347B",
                        "6675DBFE6675DCC102FFEFFE347A",
                        "6675DE4B6675DF1702FFEFFE3478",
                        "6675E0986675E15E02FFEFFE347A",
                        "6675E2E36675E3AD02FFF0FE347A",
                        "6675E5346675E5FA02FFEFFE347B",
                        "6675E7686675E82202FFEFFE3479",
                        "6675E9A36675EA7102FFEFFE347A",
                        "6675EC086675ECD002FFEFFE3478",
                        "6675EE506675EF1502FFEFFE347A",
                        "6675F08C6675F14602FFEFFE347A",
                        "6675F2B46675F37502FFEFFE347B",
                        "6675F4FE6675F5C602FFEFFE3479",
                        "6675F7576675F82102FFF0FE347B",
                        "6675F9C26675FA8B02FFEFFE3479",
                        "6675FC1D6675FCE702FFEFFE347A",
                        "6675FE876675FF4C02FFEFFE347B",
                        "667600DD667601A202FFEFFE347A",
                        "6676033D6676040802FFEFFE347C",
                        "667605AB6676066E02FFF0FE347A",
                        "66760802667608CA02FFEFFE347C",
                        "66760A5D66760B2602FFEFFE347C",
                        "66760CCE66760D9002FFEFFE347A",
                        "66760F0266760FCB02FFEFFE347C",
                        "667611676676122E02FFEFFE347A",
                        "6676133D66761A4E03FFE5FE347C",
                        "66761B0366761D79020021FE347A",
                        "66761EF266761FD202FFEFFE347D",
                        "667621636676223402FFEFFE347D",
                        "667623CF6676249302FFEFFE347D",
                        "667626496676270D02FFEFFE347B",
                        "667628B26676297202FFEFFE347D",
                        "66762B0B66762BBF02FFEFFE347B",
                        "66762D6A66762E2102FFEFFE347D",
                        "66762FA96676305A02FFEFFE347D",
                        "667631D86676329C02FFEFFE347E",
                        "6676341E667634E102FFEFFE347E",
                        "6676364F6676370F02FFEFFE347D",
                        "667638776676393402FFEFFE347D",
                        "66763A9566763B4D02FFEFFE347D",
                        "66763CAF66763D6902FFEFFE347B",
                        "66763EC566763F7F02FFF0FE347F",
                        "667640E0667641A702FFEFFE347C",
                        "6676430F667643D102FFEFFE347E",
                        "66764530667645EC02FFEFFE347E",
                        "667647446676480202FFF0FE347E",
                        "6676495966764A1802FFEFFE347F",
                        "66764B6F66764C2B02FFEFFE347E",
                        "66764D7C66764E3502FFEFFE347B",
                        "66764F866676504702FFF0FE347C",
                        "667651A06676526102FFEFFE347E",
                        "667653A96676545A02FFEFFE347E",
                        "667655A36676565B02FFEFFE347C",
                        "667657AC6676586A02FFEFFE347E",
                        "667659CA66765A8A02FFF0FE347B",
                        "66765BE166765CA002FFEFFE347E",
                        "66765DE666765E9702FFEFFE347D",
                        "66765FD06676608602FFEFFE347E",
                        "667661CF6676628902FFF0FE347E",
                        "667663E1667664A402FFEFFE347C",
                        "667665FA667666B602FFEFFE347E",
                        "667668096676680A02FFEFFE347F",
                        "6676680A66766F1B03FFEFFE347F",
                        "66766FD0667672B502002EFE347C",
                        "667673E2667674D002FFEFFE347F",
                        "66767608667676D502FFEFFE347F",
                        "6676780D667678D502FFF0FE347F",
                        "66767A1466767ACD02FFEFFE347F",
                        "66767C0466767CBA02FFEFFE347F",
                        "66767E0466767EC102FFEFFE347E",
                        "66767FFF667680B802FFEFFE347F",
                        "66768201667682BB02FFEFFE347F",
                        "66768400667684B802FFEFFE347F",
                        "6676860A667686CD02FFEFFE347F",
                        "66768819667688D202FFEFFE347D",
                        "66768A1D66768AD202FFEFFE347F",
                        "66768C1E66768CD402FFEFFE347D",
                        "66768E2966768EED02FFEFFE347F",
                        "6676903A667690EF02FFF0FE347D",
                        "66769239667692F802FFEFFE347F",
                        "66769440667694F902FFEFFE347D",
                        "667696516676971602FFEFFE347F",
                        "6676987B6676993E02FFEFFE347D",
                        "66769A8C66769B3D02FFEFFE3480",
                        "66769C8866769D4502FFEFFE347D",
                        "66769EA866769F6A02FFEFFE347F",
                        "6676A0C76676A18502FFEFFE347D",
                        "6676A2F16676A3BF02FFEFFE347F",
                        "6676A5166676A5D202FFEFFE347E",
                        "6676A7216676A7E002FFEFFE347F",
                        "6676A9436676AA0B02FFEFFE347E",
                        "6676AB626676AC2002FFEFFE347F",
                        "6676AD7A6676AE3402FFEFFE347D",
                        "6676AF8A6676B04D02FFF0FE347F",
                        "6676B1B66676B27A02FFEFFE347E",
                        "6676B3C56676B47D02FFEFFE3480",
                        "6676B5D16676B69202FFEFFE347E",
                        "6676B7ED6676B8A902FFEFFE347F",
                        "6676B9FA6676BAB602FFEFFE347D",
                        "6676BBF86676BCA802FFEFFE347F",
                        "6676BCCF6676C3E003FFE6FE3480",
                        "6676C4946676C718020020FE347D",
                        "6676C84B6676C92402FFEFFE347F",
                        "6676CA6C6676CB4002FFEFFE347F",
                        "6676CC846676CD4D02FFEFFE347E",
                        "6676CE9B6676CF5C02FFEFFE347D",
                        "6676D09D6676D15E02FFEFFE347E",
                        "6676D2B06676D37002FFEFFE347E",
                        "6676D4C06676D57E02FFEFFE347E",
                        "6676D6CB6676D78A02FFEFFE347D",
                        "6676D8D46676D98D02FFEFFE347D",
                        "6676DAE16676DB9A02FFEFFE347D",
                        "6676DCDD6676DD9502FFEFFE347D",
                        "6676DEE46676DFA802FFEFFE347D",
                        "6676E10B6676E1CB02FFEFFE347B",
                        "6676E3176676E3CB02FFEFFE347B",
                        "6676E51A6676E5DF02FFEFFE347B",
                        "6676E73B6676E7F802FFEFFE347B",
                        "6676E9506676EA0C02FFEFFE347A",
                        "6676EB5A6676EC1602FFEFFE347B",
                        "6676ED696676EE2802FFEFFE347B",
                        "6676EF7C6676F03302FFEFFE347D",
                        "6676F1856676F24002FFEFFE347D",
                        "6676F3946676F44C02FFEFFE347D",
                        "6676F59C6676F65C02FFF0FE347C",
                        "6676F7A46676F85802FFEFFE347C",
                        "6676F9A86676FA5F02FFEFFE347D",
                        "6676FBAA6676FC6A02FFEFFE347D",
                        "6676FDC76676FE8C02FFF0FE347C",
                        "6676FFEC667700AC02FFEFFE347C",
                        "66770206667702C802FFEFFE347C",
                        "66770421667704E102FFEFFE347C",
                        "667706486677070C02FFEFFE347C",
                        "6677085E6677091702FFEFFE347C",
                        "66770A6E66770B2B02FFEFFE347A",
                        "66770C7566770D3402FFEFFE347A",
                        "66770E9266770F4F02FFEFFE347A",
                        "667710A56677116102FFEFFE347C",
                        "66771192667718A303FFE4FE347C",
                        "6677195866771BE3020020FE3479",
                        "66771D0F66771DE902FFEFFE347C",
                        "66771F396677200F02FFF0FE347C",
                        "6677215B6677221F02FFF0FE347A",
                        "667723556677240C02FFEFFE347C",
                        "667725506677260802FFEFFE347A",
                        "6677274F6677280E02FFEFFE347C",
                        "6677294D66772A0402FFEFFE347A",
                        "66772B4F66772C1302FFEFFE347C",
                        "66772D6166772E1B02FFEFFE347A",
                        "66772F686677302102FFEFFE347D",
                        "667731626677322102FFEFFE347C",
                        "667733636677341702FFEFFE347C",
                        "667735636677362102FFEFFE347C",
                        "667737676677381C02FFEFFE347B",
                        "6677396C66773A3002FFF0FE347D",
                        "66773B7D66773C3902FFEFFE347B",
                        "66773D8866773E4202FFEFFE347D",
                        "66773F856677403F02FFEFFE347A",
                        "6677419A6677425D02FFEFFE347D",
                        "667743AD6677446B02FFEFFE347A",
                        "667745BC6677467A02FFEFFE347C",
                        "667747C76677488402FFEFFE347B",
                        "667749D966774A9B02FFEFFE347D",
                        "66774BEB66774CA702FFEFFE347B",
                        "66774DF266774EA802FFEFFE347D",
                        "66774FF0667750B702FFEFFE347B",
                        "66775205667752B702FFEFFE347D",
                        "667753F7667754AC02FFEFFE347D",
                        "667755F5667756B002FFEFFE347D",
                        "667757F7667758AF02FFEFFE347D",
                        "66775A0266775AC702FFEFFE347D",
                        "66775C0666775CB902FFEFFE347D",
                        "66775E0766775EC602FFF0FE347D",
                        "6677601B667760DB02FFEFFE347D",
                        "6677622F667762F402FFEFFE347D",
                        "6677643E667764FC02FFF0FE347B",
                        "667766576677665802FFEFFE347D",
                        "6677665866776D6903FFEFFE347C",
                        "66776E1E6677710402002FFE347C",
                        "667772316677732302FFEFFE347E",
                        "667774506677752D02FFEFFE347E",
                        "6677767B6677774302FFEFFE347E",
                        "667778736677792F02FFEFFE347E",
                        "66777A7066777B3602FFEFFE347E",
                        "66777C7366777D2902FFEFFE347D",
                        "66777E5866777F1102FFEFFE347D",
                        "6677805D6677812402FFF0FE347D",
                        "6677827F6677834702FFF1FE347D",
                        "667784876677854502FFEFFE347D",
                        "6677868B6677874402FFEFFE347D",
                        "667788876677893E02FFEFFE347E",
                        "66778A7E66778B3F02FFEFFE347D",
                        "66778C7F66778D3B02FFEFFE347E",
                        "66778E8666778F4602FFEFFE347E",
                        "6677908D6677914502FFEFFE347E",
                        "667792836677933C02FFF0FE347C",
                        "667794746677952702FFEFFE347E",
                        "667796716677973902FFEFFE347C",
                        "667798806677993202FFEFFE347E",
                        "66779A6B66779B2B02FFEFFE347C",
                        "66779C8466779D4B02FFEFFE347E",
                        "66779E9F66779F5E02FFEFFE3480",
                        "6677A0A76677A16802FFEFFE347F",
                        "6677A2B66677A37402FFEFFE347F",
                        "6677A4CF6677A59402FFEFFE347F",
                        "6677A6E56677A7A402FFEFFE347D",
                        "6677A8F76677A9BC02FFEFFE347E",
                        "6677AB236677ABED02FFF0FE347D",
                        "6677AD3F6677AE0102FFEFFE347F",
                        "6677AF586677B01602FFEFFE347F",
                        "6677B1646677B22302FFF0FE347D",
                        "6677B36A6677B42B02FFEFFE347F",
                        "6677B57A6677B63B02FFEFFE347D",
                        "6677B7956677B85402FFEFFE3480",
                        "6677B99B6677BA5902FFEFFE3480",
                        "6677BB1E6677C22E03FFE4FE347F",
                        "6677C2E36677C585020023FE347D",
                        "6677C6B56677C79F02FFEFFE347F",
                        "6677C8D96677C9AD02FFEFFE3480",
                        "6677CAF86677CBC602FFEFFE3480",
                        "6677CD046677CDC202FFEFFE347E",
                        "6677CEFF6677CFC002FFEFFE347E",
                        "6677D1106677D1D802FFEFFE347E",
                        "6677D3196677D3D302FFF0FE347E",
                        "6677D5216677D5E302FFEFFE3480",
                        "6677D72E6677D7EF02FFEFFE347E",
                        "6677D9396677D9F302FFEFFE347E",
                        "6677DB406677DBFD02FFEFFE347E",
                        "6677DD496677DE0602FFEFFE3481",
                        "6677DF496677DFFC02FFF0FE3480",
                        "6677E1436677E1FD02FFEFFE3481",
                        "6677E33B6677E3F102FFEFFE3481",
                        "6677E53F6677E60102FFEFFE3480",
                        "6677E7566677E81F02FFF0FE3480",
                        "6677E9816677EA4302FFEFFE3480",
                        "6677EB9F6677EC6002FFF0FE3480",
                        "6677EDB96677EE7902FFEFFE3480",
                        "6677EFD06677F08E02FFEFFE3480",
                        "6677F1DA6677F29A02FFF0FE3480",
                        "6677F3F86677F4B802FFEFFE347E",
                        "6677F6006677F6B402FFEFFE347E",
                        "6677F7F56677F8B702FFEFFE3480",
                        "6677FA166677FADB02FFEFFE347E",
                        "6677FC2A6677FCE602FFEFFE347E",
                        "6677FE426677FEFF02FFEFFE347E",
                        "667800546678011A02FFEFFE347E",
                        "6678026C6678032602FFEFFE347E",
                        "667804836678054D02FFEFFE347F",
                        "667806A06678075D02FFEFFE347F",
                        "667808BD6678098002FFEFFE347F",
                        "66780ADA66780B9702FFEFFE347E",
                        "66780CE766780DA202FFEFFE347E",
                        "66780EF466780FB302FFEFFE3481",
                        "66780FDD667816EE03FFE5FE3481",
                        "667817A366781A2902001FFE347E",
                        "66781B7966781C6002FFEFFE3480",
                        "66781D9C66781E6602FFEFFE3480",
                        "66781FA76678206602FFEFFE347E",
                        "6678219F6678225702FFEFFE347F",
                        "667823956678245602FFEFFE347D",
                        "667825AD6678266A02FFEFFE347F",
                        "667827B66678286E02FFEFFE347C",
                        "667829B366782A7402FFEFFE347F",
                        "66782BC766782C8902FFEFFE347C",
                        "66782DDF66782E9C02FFEFFE347E",
                        "66782FF3667830B302FFEFFE347C",
                        "667831F3667832A902FFEFFE347E",
                        "667833F9667834B902FFEFFE347C",
                        "66783615667836CD02FFEFFE347E",
                        "66783815667838D302FFEFFE347B",
                        "66783A1866783ACD02FFEFFE347E",
                        "66783C0E66783CCA02FFEFFE347E",
                        "66783E2966783EEA02FFEFFE347E",
                        "667840436678410202FFEFFE347D",
                        "667842546678431402FFEFFE347D",
                        "6678446A6678452202FFF0FE347D",
                        "6678465C6678471502FFEFFE347D",
                        "6678487B6678494302FFEFFE347D",
                        "66784A8D66784B4A02FFEFFE347D",
                        "66784CA066784D6102FFEFFE347D",
                        "66784EB966784F7302FFF0FE347D",
                        "667850C96678518802FFEFFE347C",
                        "667852DC667853A102FFF0FE347C",
                        "667854F7667855B802FFEFFE347C",
                        "6678571A667857DA02FFEFFE347D",
                        "6678592A667859E702FFEFFE347D",
                        "66785B3066785BED02FFEFFE347A",
                        "66785D4566785E0302FFEFFE347D",
                        "66785F5B6678601902FFEFFE347D",
                        "667861746678623B02FFF0FE347C",
                        "6678639A6678646002FFEFFE347D",
                        "667864A666786BB703FFE3FE347D",
                        "66786C6C66786EF002001FFE3479",
                        "66787021667870FB02FFEFFE347D",
                        "6678723D6678730A02FFEFFE347D",
                        "6678744D6678751702FFEFFE347A",
                        "667876506678770C02FFEFFE347D",
                        "667878466678790002FFEFFE347A",
                        "66787A4D66787B0A02FFEFFE347D",
                        "66787C5A66787D1A02FFEFFE347A",
                        "66787E6066787F1B02FFEFFE347D",
                        "6678806E6678813202FFEFFE347A",
                        "6678829B6678835D02FFEFFE347D",
                        "667884AF6678856902FFEFFE347B",
                        "667886A06678875302FFEFFE347D",
                        "6678889B6678895B02FFEFFE347B",
                        "66788AB966788B7702FFEFFE347D",
                        "66788CC366788D7D02FFEFFE347D",
                        "66788ECE66788F8402FFEFFE347D",
                        "667890D06678918F02FFEFFE347D",
                        "667892DC6678939E02FFEFFE347D",
                        "667894F9667895B402FFEFFE347D",
                        "66789702667897C302FFEFFE347E",
                        "66789921667899DA02FFEFFE347D",
                        "66789B2766789BE202FFEFFE347B",
                        "66789D3E66789E0402FFEFFE347E",
                        "66789F586678A01A02FFEFFE347E",
                        "6678A16A6678A22C02FFEFFE347E",
                        "6678A37F6678A43802FFEFFE347E",
                        "6678A58E6678A64E02FFEFFE347E",
                        "6678A79D6678A84D02FFEFFE347E",
                        "6678A98A6678AA3D02FFEFFE347C",
                        "6678AB926678AC5702FFEFFE347E",
                        "6678ADA26678AE5802FFEFFE347C",
                        "6678AFA66678B06802FFEFFE347E",
                        "6678B1C56678B28802FFEFFE347C",
                        "6678B3E56678B49B02FFEFFE347E",
                        "6678B5DA6678B69402FFEFFE347C",
                        "6678B7F86678B8BA02FFEFFE347E",
                        "6678B9726678C08303FFE4FE347E",
                        "6678C1386678C3D7020022FE347E",
                        "6678C5176678C5F502FFEFFE347C",
                        "6678C72D6678C7FF02FFEFFE347C",
                        "6678C9546678CA2102FFEFFE347B",
                        "6678CB656678CC1F02FFEFFE347B",
                        "6678CD676678CE2A02FFEFFE347D",
                        "6678CF7D6678D03D02FFEFFE347D",
                        "6678D1936678D25402FFEFFE347E",
                        "6678D3A76678D46302FFEFFE347E",
                        "6678D5AC6678D66602FFEFFE347D",
                        "6678D7AB6678D86002FFF0FE347E",
                        "6678D9AC6678DA6B02FFF0FE347E",
                        "6678DBC46678DC8002FFEFFE347D",
                        "6678DDD26678DE9402FFEFFE347E",
                        "6678DFEF6678E0A402FFEFFE347E",
                        "6678E1FA6678E2BA02FFEFFE347E",
                        "6678E40F6678E4D102FFEFFE347E",
                        "6678E6216678E6D502FFEFFE347E",
                        "6678E8326678E8F302FFEFFE347E",
                        "6678EA456678EAFA02FFEFFE347E",
                        "6678EC486678ECFE02FFEFFE347E",
                        "6678EE536678EF1702FFF0FE347E",
                        "6678F0616678F11802FFEFFE347C",
                        "6678F2726678F32C02FFEFFE347C",
                        "6678F48E6678F55402FFEFFE347C",
                        "6678F6BC6678F78702FFEFFE347F",
                        "6678F8F26678F9B802FFEFFE347F",
                        "6678FB1A6678FBE002FFEFFE347F",
                        "6678FD386678FDF402FFEFFE347F",
                        "6678FF526679001102FFEFFE347F",
                        "667901716679022E02FFF0FE347F",
                        "667903886679044D02FFEFFE347F",
                        "667905A66679066502FFEFFE347F",
                        "667907C46679088102FFF0FE347F",
                        "667909E466790AA602FFEFFE347D",
                        "66790C0266790CC602FFEFFE347D",
                        "66790E2566790E3F02FFEFFE347F",
                        "66790E3F6679155003FFEFFE347E",
                        "66791605667918B5020025FE347C",
                        "667919FC66791AD902FFEFFE347F",
                        "66791C0B66791CD202FFEFFE3480",
                        "66791E1D66791EE102FFEFFE3480",
                        "66792022667920E302FFEFFE3480",
                        "66792233667922F502FFEFFE3480",
                        "6679244E6679250902FFEFFE3480",
                        "667926596679271602FFEFFE347D",
                        "667928666679291D02FFEFFE347D",
                        "66792A6F66792B3002FFF1FE347E",
                        "66792C8466792D4002FFEFFE3480",
                        "66792E9866792F5502FFEFFE3480",
                        "667930B56679317402FFEFFE3480",
                        "667932CD6679338802FFEFFE3480",
                        "667934D26679358B02FFEFFE3480",
                        "667936ED667937AC02FFEFFE347E",
                        "667938FF667939B402FFEFFE347E",
                        "66793B0E66793BCB02FFEFFE347D",
                        "66793D1C66793DD202FFEFFE3480",
                        "66793F1F66793FDC02FFEFFE3480",
                        "6679413A667941F502FFF0FE3480",
                        "66794341667943F202FFEFFE3480",
                        "66794545667945FD02FFEFFE3480",
                        "667947506679480602FFEFFE3480",
                        "6679495966794A1C02FFEFFE3480",
                        "66794B7366794C2802FFEFFE347F",
                        "66794D7B66794E3302FFEFFE347F",
                        "66794F9D6679506202FFEFFE347F",
                        "667951CA6679528802FFEFFE3480",
                        "667953EB667954B102FFEFFE347F",
                        "6679561C667956DD02FFF0FE347D",
                        "667958446679590202FFEFFE347E",
                        "66795A4D66795AFE02FFEFFE347D",
                        "66795C5366795D0D02FFEFFE347D",
                        "66795E6E66795F2F02FFEFFE347D",
                        "667960956679615602FFEFFE347D",
                        "667962B46679630F02FFEFFE347E",
                        "6679630F66796A2003FFF0FE347D",
                        "66796AD566796D58020020FE347D",
                        "66796EA666796F8F02FFEFFE347B",
                        "667970F0667971C602FFEFFE347C",
                        "6679731C667973DF02FFEFFE347B",
                        "66797531667975EF02FFEFFE347B",
                        "66797735667977EE02FFEFFE347B",
                        "66797936667979E702FFEFFE347A",
                        "66797B2866797BDA02FFEFFE347A",
                        "66797D2E66797DE202FFF0FE347A",
                        "66797F2466797FDC02FFEFFE347B",
                        "6679812E667981E602FFEFFE347A",
                        "6679834D6679840A02FFEFFE347C",
                        "6679856C6679862A02FFF0FE347C",
                        "667987836679884102FFEFFE347C",
                        "667989A466798A5F02FFF0FE347C",
                        "66798BC066798C8302FFEFFE347C",
                        "66798DE966798EAA02FFF0FE347C",
                        "66799026667990E402FFEFFE347C",
                        "667992566679931002FFEFFE3478",
                        "667994856679953F02FFEFFE347B",
                        "667996D36679979C02FFEFFE347B",
                        "66799934667999F602FFF0FE347B",
                        "66799B8766799C4702FFEFFE347A",
                        "66799DFF66799EC702FFEFFE347A",
                        "6679A05A6679A11802FFEFFE347B",
                        "6679A2C36679A38002FFEFFE3478",
                        "6679A50E6679A5D402FFEFFE347B",
                        "6679A7706679A82D02FFF0FE3478",
                        "6679A9A86679AA6802FFEFFE347B",
                        "6679ABF26679ACB302FFEFFE347A",
                        "6679AE386679AEF002FFEFFE347A",
                        "6679B0746679B13C02FFEFFE347A",
                        "6679B2CF6679B38C02FFEFFE3478",
                        "6679B5096679B5C902FFEFFE347A",
                        "6679B75B6679B7DF02FFEFFE347A",
                        "6679B7DF6679BEEF03FFEEFE3479",
                        "6679BFA46679C1F402001DFE3479",
                        "6679C3616679C43902FFEFFE3478",
                        "6679C5AF6679C67B02FFEFFE3479",
                        "6679C7F56679C8B602FFEFFE347B",
                        "6679CA2B6679CAE802FFEFFE3478",
                        "6679CC6C6679CD3602FFEFFE347A",
                        "6679CEB06679CF7702FFF0FE347A",
                        "6679D1016679D1C102FFEFFE347B",
                        "6679D3446679D40D02FFF0FE347A",
                        "6679D58D6679D64E02FFEFFE3478",
                        "6679D7A96679D86302FFEFFE347B",
                        "6679D9C36679DA8002FFEFFE3478",
                        "6679DBD96679DC9402FFEFFE347B",
                        "6679DDEB6679DEA202FFEFFE347B",
                        "6679DFFE6679E0B902FFEFFE347B",
                        "6679E2176679E2D902FFEFFE3478",
                        "6679E4266679E4D802FFEFFE347A",
                        "6679E6236679E6E602FFEFFE3479",
                        "6679E83C6679E90602FFEFFE347A",
                        "6679EA566679EB0B02FFEFFE347B",
                        "6679EC646679ED2602FFEFFE3478",
                        "6679EE786679EF3802FFEFFE347B",
                        "6679F0996679F15C02FFEFFE3478",
                        "6679F2B26679F36302FFEFFE347B",
                        "6679F4C16679F58002FFEFFE3478",
                        "6679F6E36679F7A802FFF0FE347B",
                        "6679F9266679F9E902FFEFFE347B",
                        "666D8A60666D8B2A02FFEFFE347C",
                        "666D8C60666D8D2802FFEFFE347F",
                        "666D8E5D666D8F2902FFEFFE347E",
                        "666D9068666D913502FFEFFE347E",
                        "666D926B666D933702FFEFFE347E",
                        "666D9475666D954402FFEFFE347E"

                    )
                  //  Event = textEvento as MutableList<String>
                        Event.map {
                            Log.d("datosdeEvento","$it")
                        }

                    runOnUiThread {
                        callback.getEventCrudo(agregar00AlPenultimoCaracter(Event,2) )//   Event)
                    }

                         listaInvertidaTIME =    ValoresFiltrados.reversed().toMutableList() //ValoresFiltrados as MutableList<String>
                         ListaTimeCrudo = listaInvertidaTIME
                    var reverListaEvent = Event.reversed().toMutableList()

                    runOnUiThread {
                        callback.getTimeCrudo(agregar00AlPenultimoCaracter(ListaTimeCrudo,2))
                    }
                        Log.d("RecorridoDELOsDatosSeparados","--------------------------------------\n \n \n")
                    listaInvertidaTIME.map {
                        Log.d("RecorridoDELOsDatosSeparados"," listaInvertidaTIME $it ${convertirHexAFecha(it.substring(0,8))}")
                    }

                        var A = T33CB2 - T33CB1
                    Log.d("LogProcesoExtraccionDatos"," A $A  T33CB2 $T33CB2  T33CB1 $T33CB1   ")
                        var B = CElTime2 - CElTime1
                    Log.d("LogProcesoExtraccionDatos"," B $B  CElTime2 $CElTime2  T33CB1 $CElTime1   ")
                     //   var C  = ((A - B)/ B) * ATMUestra
                    val C: Long = (((A - B).toDouble() / B) * ATMUestra).toLong()
                    Log.d("LogProcesoExtraccionDatos"," C $C  ATMUestra $ATMUestra   ")
                    var AV =  1//0
                    var n=0
                    var E  : Long = 0L
                    var D : Long = T33CB1 - CElTime1
                    var J = 0L
                    Log.d("LogProcesoExtraccionDatos"," AV $AV  E $E  D $D ")
                    Log.d("LogProcesoExtraccionDatos","Eventos  $Event")
                    var count = 0
                    val CantidadEventos04: MutableList<Int> = ArrayList()
                    for(item in Event){
                        val EVENT_TYPE = item.substring(16, 18)
                        if (EVENT_TYPE == "04") {
                            count++ // Incrementamos el contador de eventos tipo "04"
                            println("item $item EVENT_TYPE $EVENT_TYPE ")

                            // Añadimos el índice a la lista CantidadEventos04
                          //  CantidadEventos04.add(count)
                        }
                    }
                    CantidadEventos04.add(count)
                    var L = CantidadEventos04
                    var HayEvento = false
                    Log.d("LogProcesoExtraccionDatos","Cantidad de eventos 04 $L ")
                    for (i in 0 until listaInvertidaTIME.size - 1) {




                        J = C * AV
                        AV = AV +1
                        var timeActual =   listaInvertidaTIME[i].substring(0,8).toLong(16)
                        var timeAnterior =   listaInvertidaTIME[i + 1 ].substring(0,8).toLong(16)

                        Log.d("LogProcesoExtraccionDatos","Dentro del for i $i  j $J  C $C AV $AV ")
                        var Evento = ""
                        Log.d("procesoPrincipal"," ${listaInvertidaTIME[i]}   ${convertirHexAFecha(
                            listaInvertidaTIME[i].substring( 0,8 ) )}")

                        Log.d("procesoPrincipal"," Event $Event")

                        for (eventRegistro in Event) {
                            val eventTime = eventRegistro.substring(8, 16).toLong(16)

                         Log.d("BusquedadeEventos" ,"eventTime $eventTime  timeActual $timeActual  timeAnterior $timeAnterior  listaInvertidaTIME[i] ${listaInvertidaTIME[i]}  i $i")
                            // Comparar si el tiempo del evento está entre timeActual y timeAnterior
//                            if (eventTime in (timeActual) until >timeAnterior) {
                                if(eventTime <= timeActual &&  eventTime > timeAnterior ) {
                                Log.d("BusquedadeEventos" ,"Registro encontrado en Event: $eventRegistro")
                                // Si cumple la condición, hacer algo con el registro encontrado
                                println("Registro encontrado en Event: $eventRegistro")
                                HayEvento = true
                                Evento = eventRegistro
                            } else {
                                HayEvento = false
                                //    println("No cumple con el rango: $eventRegistro (eventTime: $eventTime)")
                            }
                        }


                        var eventoTimeMil = listaInvertidaTIME[i].substring(0,8).toLong(16) .times(1000)
                        var TimeRefechado =   eventoTimeMil - D -E + J


                        var ConvertidotoExa = toHexString(TimeRefechado / 1000)
                       var  DatotoStamp = ConvertidotoExa.substring(0,8)+listaInvertidaTIME[i].substring(8)
                        ListaTImeDespuesDelRefechado.add(DatotoStamp)
                        Log.d("LogProcesoExtraccionDatos","timemilEventoActual $eventoTimeMil   D $D E $E  J $J  TimeRefechado $TimeRefechado  $ConvertidotoExa DatotoStamp $DatotoStamp")
                        if (!HayEvento){

//                                var TimeRefechado =   listaInvertidaTIME[i].substring(0,8).toLong(16)
//                                    .times(1000) - D -E + J
//                                var ConvertidotoExa = toHexString(TimeRefechado / 1000)
//
//                                ListaTImeDespuesDelRefechado.add(ConvertidotoExa+listaInvertidaTIME[i].substring(8))
                            //   DatoREfechadoTIme.add(ConvertidotoExa+listaInvertidaTIME[i].substring(8))
                        }

                        else{

                            var EventoFinalTimetoCheck= Evento.substring(8,16).toLong(16)!!.times(1000)
                            var EventoInicialTimetoCheck= Evento.substring(8,16).toLong(16)!!.times(1000)
                            var TlnEv =  Evento.substring(0,8).toLong(16)!!.times(1000)
                            var TempFin  =
                                ReturnValLoggerTraduccion(Evento.substring(22, 26)).toFloat()
                            var TempIn  =
                                ReturnValLoggerTraduccion(Evento.substring(18, 22)).toFloat()

                            var Even04 = Evento.substring(16, 18)
                            var EventoIs04 = false
                            if (Even04.equals("04")){
                                EventoIs04 = true
                            }
                            else
                            {
                                EventoIs04 = false
                            }


                            if (EventoIs04){

                                var inicio_FinconV = false
                                if(inicio_FinconV){

                                }
                                else{
                                    when(CL){
                                        0 ->{
                                            var dTempEvento: Long = (if ((TempFin - TempIn) < 0) 0L else (TempFin - TempIn).toLong()) * 1000L

                                            var  F : Long = 10 * 1000L
                                            E += D + dTempEvento * F
                                            var TlnEv : Long = TlnEv   - D - E - J

                                            E = E + D + dTempEvento * F
                                            var TFinEvento = EventoFinalTimetoCheck - D -E - J


                                            var ConvertidotoExaFinal = toHexString(TFinEvento / 1000)
                                            var ConvertidotoExaInicial = toHexString(TlnEv / 1000)

                                            ListaEventoDespuesDelRefechado.add(ConvertidotoExaInicial + ConvertidotoExaFinal +Evento.substring(16))


                                        }
                                        1->{

                                        }
                                        else ->{

                                        }
                                    }
                                }
                            }
                            else{


                                var TFinEvento = EventoFinalTimetoCheck - D -E - J
                                var TInEvento = EventoFinalTimetoCheck - D -E - J
                                var ConvertidotoExaFinal = toHexString(TFinEvento / 1000)
                                var ConvertidotoExaInicial = toHexString(TInEvento / 1000)

                                ListaEventoDespuesDelRefechado.add(ConvertidotoExaInicial + ConvertidotoExaFinal +Evento.substring(16)) //   ConvertidotoExa+listaInvertidaTIME[i].substring(8))

                            }
                        }




                    }


                    /*
                    Algoritmo del dia viernes
                        for (i in 0 until listaInvertidaTIME.size - 1) {




                            var J: Long = C * AV
                            AV = AV +1
                            var timeActual =   listaInvertidaTIME[i].substring(0,8).toLong(16)
                            var timeAnterior =   listaInvertidaTIME[i + 1 ].substring(0,8).toLong(16)

                            Log.d("LogProcesoExtraccionDatos","Dentro del for i $i  j $J  C $C AV $AV ")
                            var Evento = ""
                            Log.d("procesoPrincipal"," ${listaInvertidaTIME[i]}   ${convertirHexAFecha(
                                listaInvertidaTIME[i].substring( 0,8 ) )}")

                            Log.d("procesoPrincipal"," Event $Event")

                                for (eventRegistro in Event) {
                                    val eventTime = eventRegistro.substring(8, 16).toLong(16)

                                    // Comparar si el tiempo del evento está entre timeActual y timeAnterior
                                    if (eventTime in (timeActual + 1) until timeAnterior) {
                                        // Si cumple la condición, hacer algo con el registro encontrado
                                        println("Registro encontrado en Event: $eventRegistro")
                                        HayEvento = true
                                        Evento = eventRegistro
                                    } else {
                                        HayEvento = false
                                        println("No cumple con el rango: $eventRegistro (eventTime: $eventTime)")
                                    }
                                }


                            var TimeRefechado =   listaInvertidaTIME[i].substring(0,8).toLong(16)
                                .times(1000) - D -E + J
                            var ConvertidotoExa = toHexString(TimeRefechado / 1000)

                            ListaTImeDespuesDelRefechado.add(ConvertidotoExa+listaInvertidaTIME[i].substring(8))
                            if (!HayEvento){

//                                var TimeRefechado =   listaInvertidaTIME[i].substring(0,8).toLong(16)
//                                    .times(1000) - D -E + J
//                                var ConvertidotoExa = toHexString(TimeRefechado / 1000)
//
//                                ListaTImeDespuesDelRefechado.add(ConvertidotoExa+listaInvertidaTIME[i].substring(8))
                             //   DatoREfechadoTIme.add(ConvertidotoExa+listaInvertidaTIME[i].substring(8))
                            }

                            else{

                                var EventoFinalTimetoCheck= Evento.substring(8,16).toLong(16)!!.times(1000)
                                var EventoInicialTimetoCheck= Evento.substring(8,16).toLong(16)!!.times(1000)
                                var TlnEv =  Evento.substring(0,8).toLong(16)!!.times(1000)
                                var TempFin  =
                                    ReturnValLoggerTraduccion(Evento.substring(22, 26)).toFloat()
                                var TempIn  =
                                    ReturnValLoggerTraduccion(Evento.substring(18, 22)).toFloat()

                                var Even04 = Evento.substring(16, 18)
                                var EventoIs04 = false
                                if (Even04.equals("04")){
                                    EventoIs04 = true
                                }
                                else
                                {
                                     EventoIs04 = false
                                }


                                if (EventoIs04){

                                    var inicio_FinconV = false
                                    if(inicio_FinconV){

                                    }
                                    else{
                                        when(CL){
                                            0 ->{
                                                var dTempEvento: Long = (if ((TempFin - TempIn) < 0) 0L else (TempFin - TempIn).toLong()) * 1000L

                                                var  F : Long = 10 * 1000L
                                                E += D + dTempEvento * F
                                                var TlnEv : Long = TlnEv   - D - E - J

                                            E = E + D + dTempEvento * F
                                                var TFinEvento = EventoFinalTimetoCheck - D -E - J


                                                var ConvertidotoExaFinal = toHexString(TFinEvento / 1000)
                                                var ConvertidotoExaInicial = toHexString(TlnEv / 1000)

                                                ListaEventoDespuesDelRefechado.add(ConvertidotoExaInicial + ConvertidotoExaFinal +Evento.substring(16))


                                            }
                                            1->{

                                            }
                                            else ->{

                                            }
                                        }
                                    }
                                }
                                else{


                                    var TFinEvento = EventoFinalTimetoCheck - D -E - J
                                    var TInEvento = EventoFinalTimetoCheck - D -E - J
                                    var ConvertidotoExaFinal = toHexString(TFinEvento / 1000)
                                    var ConvertidotoExaInicial = toHexString(TInEvento / 1000)

                                    ListaEventoDespuesDelRefechado.add(ConvertidotoExaInicial + ConvertidotoExaFinal +Evento.substring(16)) //   ConvertidotoExa+listaInvertidaTIME[i].substring(8))

                                }
                            }




                        }

                    */
                        DatoREfechadoTIme.map {
                            Log.d("ProcesoPrincipal"," Datos Refechados $it ${convertirHexAFecha(it.substring(0,8))}")
                        }

                        Thread.sleep(100)
                        Event.map {
                            Log.d("ObtenerLoggerPrueba", "Traducido Event ${it}")
                        }
                   /* } else {
                        Log.d("ObtenerLoggerPrueba", "lista Event vacia ")
                    }
                    */



                }








            }

            return ""
        }

        override fun onPostExecute(result: String) {

          //  callback.getTime(ListaTImeDespuesDelRefechado)

            callback.getTime( agregar00AlPenultimoCaracter(ListaTImeDespuesDelRefechado,2))
            callback.getEvent(agregar00AlPenultimoCaracter(ListaEventoDespuesDelRefechado,2))

            callback.onProgress("Finalizando")
            callback.onSuccess(true)
            /******************************************************* Iniciando Actualizacion del TIMESTAMP *****************************************************/
//////////////////////// se comenta esta parte para las pruebas

            /*
                        if (ValidaConexion()) {
                            MyAsyncTaskSendDateHour(object : MyCallback {

                                override fun onSuccess(result: Boolean): Boolean {
                                    callback.onProgress(" $result onSuccess timestamp")
                                    return result
                                }

                                override fun onError(error: String) {
                                    callback.onError("timestamp: $error")
                                }

                                override fun getInfo(data: MutableList<String>?) {
                                    callback.onProgress(" $data getInfo timestamp")
                                }

                                override fun onProgress(progress: String): String {
                                    callback.onProgress(" $progress timestamp")
                                    return progress
                                }


                            }).execute()

                            Thread.sleep(500)
                            //   callback.onProgress("iniciando limpieza de logger")

                            Handler().postDelayed({
                                MyAsyncTaskResetMemory(object : MyCallback {

                                    override fun onSuccess(result: Boolean): Boolean {
                                        callback.onProgress(" $result onSuccess ResetMemory")
                                        return result
                                    }

                                    override fun onError(error: String) {
                                        callback.onError("ResetMemory: $error")

                                    }

                                    override fun getInfo(data: MutableList<String>?) {
                                        callback.onProgress(" $data getInfo ResetMemory")
                                    }

                                    override fun onProgress(progress: String): String {

                                        callback.onProgress(" $progress ResetMemory")
                                        if (progress.equals("Finalizado")) {
                                            callback.onProgress("Finalizado Logger")
                                            callback.onSuccess(true)
                                        }
                                        return progress
                                    }


                                }).execute()


                            }, 10000L)
                        }
            */

        }

        override fun onPreExecute() {
//            runOnUiThread {
                // getInfoList()?.clear()
                clearLogger()
                callback.onProgress("Iniciando Logger")
                callback.onProgress("Iniciando Tiempo")
                BanderaTiempo = true
//            }
        }


    }



    fun getLog(callback2: CallbackLoggerVersionCrudo) {
        var callback = callback2
        var ValoresFiltrados =  mutableListOf<String>()
        var DatoREfechadoTIme = mutableListOf<String>()
        var ListapruebaEvent = mutableListOf<String>()
        var Event = mutableListOf<String>()

        var CL = 0
        CoroutineScope(Dispatchers.Main).launch {
            try {
                callback.onProgress("Iniciando Logger")
                callback.onProgress("Iniciando Tiempo")
                BanderaTiempo = true

                withContext(Dispatchers.Default) {
                  //  bluetoothLeService?.sendComando("4021")
                  /*  bluetoothServices.bluetoothLeService!!.clearListLogger()
                    delay(800) // Suspende el hilo sin bloquearlo
                    bluetoothServices.bluetoothLeService?.listData?.clear()
                    bluetoothLeService?.sendComando("4060")
                    var ListapruebaTime = mutableListOf<String>()
                    // Recopila datos en segundo plano sin bloquear el hilo principal
                    delay(30000)
                    */
                    delay(1000)
                    Log.d("LogProcesoExtraccionDatos","Se esta recolectando Evento")
                    bluetoothServices.bluetoothLeService!!.clearListLogger()
                    delay(800) // Suspende el hilo sin bloquearlo
//                    bluetoothServices.bluetoothLeService?.listData?.clear()
//                    bluetoothServices.sendCommand("event", "4061")
//                    Thread.sleep(40000)

                    bluetoothServices.bluetoothLeService!!.clearListLogger()
                    delay(800) // Suspende el hilo sin bloquearlo
                    bluetoothServices.bluetoothLeService?.listData?.clear()
                    bluetoothLeService?.sendComando("4061")

                    // Recopila datos en segundo plano sin bloquear el hilo principal
                    delay(50000)
                    ListapruebaEvent = bluetoothServices.bluetoothLeService!!.getLogeer() as MutableList<String>

                  //  ListapruebaEvent = ObtenerLoggerEventComand()// bluetoothServices.bluetoothLeService!!.getLogeer() as MutableList<String> //getInfoList() as MutableList<String>

                    ListapruebaEvent.map {
                        Log.d("DatosCRUDOSEVENT","$it")
                    }
                    var  EventPRE = VEvent(ListapruebaEvent as MutableList<String?>)

                    EventPRE.map {
                        if (it!!.length>20){
                            Event.add(it)
                        }
                    }
                    Log.d("LogProcesoExtraccionDatos","Termino recolectado Evento")
                    delay(500)
                    //var CB133 = OBtenerTimeControl()
                    bluetoothServices.bluetoothLeService!!.clearListLogger()
                    Log.d("LogProcesoExtraccionDatos","Se esta recolectando la hora del control")
                    bluetoothLeService?.sendFirstComando("405B")
                    delay(500)
                    var T33CB1 = bluetoothServices!!.bluetoothLeService!!.getLogeer()!!.first().replace(" ","").substring(16,24).toLong(16).times(1000) /// as MutableList<String>
                    Log.d("LogProcesoExtraccionDatos","Se termino la recollecion del time control")
                    delay(100)
                    bluetoothServices.bluetoothLeService!!.clearListLogger()
                    var CElTime1 = GetNowDateExa().toLong(16).times(1000)
                    Log.d("procesoPrincipal", "CB133 $T33CB1 CElTime1 $CElTime1")
//                    Log.d("procesoPrincipal", "CB133 $CB133")
                    Log.d("LogProcesoExtraccionDatos","Se esta recolectando Time")
                    var ListapruebaTime =  ObtenerLoggerTimeComand ()//bluetoothServices!!.bluetoothLeService!!.getLogeer() as MutableList<String>

                    ListapruebaTime.map {
                        Log.d("informacionListaBle",it)
                    }

                    var replaceCadena =  ListapruebaTime.joinToString("").replace(" ", "")
                    Log.d("ListapruebaTime","replaceCadena ${replaceCadena.length} $replaceCadena   \n $ListapruebaTime")


                    var isChecksumOk =  sumarParesHexadecimales(replaceCadena.substring(0,replaceCadena.length-8)).uppercase()
                    var toChecksume =replaceCadena.substring(replaceCadena.length-8,replaceCadena.length).uppercase()

                    Log.d("ProcesoDelLoggerTime","isChecksumOk $isChecksumOk  toChecksume $toChecksume")
                    val maxIntentos = 3
                    var intentos = 0
                    var checksumCorrecto = false

                    while (intentos < maxIntentos && !checksumCorrecto) {
                        // Verificar el checksum
                        if (!isChecksumOk.equals(toChecksume)) {
                            // Incrementar el contador de intentos
                            intentos++

                            Log.d("ProcesoDelLoggerTime", "Intento $intentos: isChecksumOk es incorrecto")

                            if (intentos < maxIntentos) {
                                // Intentar obtener los datos nuevamente si no es el último intento
                                ListapruebaTime = ObtenerLoggerTimeComand()
                                replaceCadena =  ListapruebaTime.joinToString("").replace(" ", "")
                               isChecksumOk =  sumarParesHexadecimales(replaceCadena.substring(0,replaceCadena.length-8)).uppercase()
                               toChecksume =replaceCadena.substring(replaceCadena.length-8,replaceCadena.length).uppercase()

                            }
                        } else {
                            // El checksum es correcto, salir del bucle
                            checksumCorrecto = true
                             replaceCadena =  ListapruebaTime.joinToString("").replace(" ", "")
                            Log.d("ProcesoDelLoggerTime", "isChecksumOk es Correcto")
                        }
                    }

                    Log.d("LogProcesoExtraccionDatos","Se Termino la recollecion Time")
                    if (!checksumCorrecto) {
                        // Si después de los tres intentos el checksum sigue siendo incorrecto, limpiar ListapruebaTime
                        Log.d("ProcesoDelLoggerTime", "No se pudo verificar el checksum después de $maxIntentos intentos. Limpiando ListapruebaTime.")
                        ListapruebaTime.clear()
                    }

                    else{



                        var listtoEval = replaceCadena.substring(16,replaceCadena.length-8)
                        Log.d("ProcesoDelLoggerTime","listtoEval ${listtoEval.length}")
                            var listaFinal = dividirEnPaquetes(listtoEval, 256)
                        listaFinal.forEachIndexed { index, paquete ->
                            println("Paquete ${index + 1}: $paquete")
                            Log.d("ProcesoDelLoggerTime"," $paquete")
                        }
                        Log.d("ProcesoDelLoggerTime"," \n \n ----------------------- ${listaFinal.size}")

                       val FinalLIstaTime = mutableListOf<String>()
                        var FinalLIstaTimeFINAL = mutableListOf<String>()
                        for (i in 0 until listaFinal.size - 1) {
                            FinalLIstaTime.add(listaFinal[i])
                        }
                        Log.d("ProcesoDelLoggerTime"," \n \n ----------------------- ${FinalLIstaTime.size}")
                        FinalLIstaTime.map{ //   .forEachIndexed { index, paquete ->
                          //  println("Paquete ${index + 1}: $paquete")
                            Log.d("FinalLIstaTime"," $it")
                        }

                         ValoresFiltrados = dividirpaquetes18TIme(FinalLIstaTime)
                        var listaEntera = listaFinal.last().substring(listaFinal.last().length-2, listaFinal.last().length  )
                        var indexLast = 0
                        var ValoresToSeachListaFinalIncompletaDatosRecientes = mutableListOf<String>()
                        var ValoresToSeachListaFinalIncompletaDatosViejos = mutableListOf<String>()

                        Log.d("ValoresFiltrados","ValoresFiltrados ${ValoresFiltrados.size}  ${ValoresFiltrados.toString()}")

                        ValoresFiltrados.map {
                        //    Log.d("ValoresFiltrados","$it")
                        }

                        ///////////////////OBtener Los datos de evento
                        if (ValoresFiltrados.isNotEmpty()){
//                            var ListapruebaEvento =  ObtenerLoggerEventComand ()
//                            ListapruebaEvento.map {
//                                Log.d("ValoresEventoCrudo","$it")
//                            }
//                            var replaceCadenaEvento =  ListapruebaEvento.joinToString("").replace(" ", "")


                            if (!Event.isNullOrEmpty()) {

                                //LA FUNCION VEvent SEPARA LOS DATOS EN EL HEADER Y LOS DATOS EN BRUTO


                                    Event.map {
                                        Log.d("datosdeEvento","$it")
                                    }


                                val listaInvertidaTIME = ValoresFiltrados.reversed()

                                bluetoothServices.bluetoothLeService!!.clearListLogger()
                                bluetoothLeService?.sendFirstComando("405B")
                                delay(500)
                                var T33CB2 = bluetoothServices!!.bluetoothLeService!!.getLogeer()!!.first().replace(" ","").substring(16,24).toLong(16).times(1000) /// as MutableList<String>

                                delay(100)
                                bluetoothServices.bluetoothLeService!!.clearListLogger()
                                var CElTime2 = GetNowDateExa().toLong(16).times(1000)
                                Log.d("procesoPrincipal", "T33CB2 $T33CB2 CElTime2 $CElTime2")
                                delay(1000)
                                var A = T33CB2 - T33CB1
                                var B = CElTime2 - CElTime1
                                var ATMUestra = "2".toLong(16)!!.times(1000)
                                var C= ((A - B)/ B) * ATMUestra
                                var AV = 0L
                                var E  : Long = 0L
                                var D : Long = T33CB1 - CElTime1


                                for (i in listaInvertidaTIME.indices) {
                                    var J = C* AV
                                    AV = AV +1
                                Log.d("procesoPrincipal"," ${listaInvertidaTIME[i]}   ${convertirHexAFecha(
                                    listaInvertidaTIME[i].substring( 0,8 ) )}")

                                    var timeActual =   listaInvertidaTIME[i].substring(0,8).toLong(16)
                                    var timeAnterior =   listaInvertidaTIME[i -1 ].substring(0,8).toLong(16)

                                    var HayEvento = false
                                    if (!HayEvento){

                                      var TimeRefechado =   listaInvertidaTIME[i].substring(0,8).toLong(16)
                                            .times(1000) - D -E + J
                                        var ConvertidotoExa = toHexString(TimeRefechado / 1000)
                                        DatoREfechadoTIme.add(ConvertidotoExa+listaInvertidaTIME[i].substring(8))
                                    }

                                    else{
                                        var Evento = "6674E8626674E86204003B00067F"
                                        var EventoFinalTimetoCheck= Evento.substring(8,16).toLong(16)!!.times(1000)
                                        var TlnEv =  Evento.substring(0,8).toLong(16)!!.times(1000)
                                        var TempFin  =
                                             ReturnValLoggerTraduccion(Evento.substring(22, 26)).toFloat()
                                        var TempIn  =
                                             ReturnValLoggerTraduccion(Evento.substring(18, 22)).toFloat()

                                        var EventoIs04 = false
                                         
                                        if (EventoIs04){

                                            var inicio_FinconV = false
                                            if(inicio_FinconV){
                                                
                                            }
                                            else{
                                                when(CL){
                                                    0 ->{
                                                        var dTempEvento: Long = (if ((TempFin - TempIn) < 0) 0L else (TempFin - TempIn).toLong()) * 1000L

                                                        var  F : Long = 10 * 1000L
                                                        E += D + dTempEvento * F
                                                       var TlnEv : Long = TlnEv   - D - E - J


                                                    }
                                                    1->{

                                                    }
                                                    else ->{
                                                        
                                                    }
                                                }
                                            }
                                        }
                                        else{

                                            
                                            var TFinEvento = EventoFinalTimetoCheck - D -E - J


                                        }
                                    }

                                }

                                DatoREfechadoTIme.map {
                                    Log.d("ProcesoPrincipal"," Datos Refechados $it ${convertirHexAFecha(it.substring(0,8))}")
                                }

                                Thread.sleep(100)
                                Event.map {
                                    Log.d("ObtenerLoggerPrueba", "Traducido Event ${it}")
                                }
                            } else {
                                Log.d("ObtenerLoggerPrueba", "lista Event vacia ")
                            }



                        }



                        var  CL = 0
                        when(CL)
                        {
                            0 ->{
                               var  CElTime2 = GetNowDateExa()


                            }
                        }







//
//                        Log.d("ProcesoDelLoggerTime"," \n \n -----------------------")
//
//
//                        FinalLIstaTime.forEachIndexed { index, paquete ->
//                            println("Paquete ${index + 1}: $paquete")
//                            Log.d("ListapruebaTime"," $paquete")
//                        }
//
//                        val listaFiltrada = dividirpaquetes18TIme (FinalLIstaTime)
//
//                        FinalLIstaTime.map{
//
//                        }
//                        val listaFiltrada = dividirpaquetes18TIme (FinalLIstaTime)
//                        listaFiltrada.map{
//                            Log.d("DespuesDEFiltarlaLista","$listaFiltrada")
//                        }



                    }


                   /* Log.d("ListapruebaTime","isChecksumOk $isChecksumOk")
                    var listtoEval = replaceCadena.substring(16,replaceCadena.length-8)
                    var listaFinal = dividirEnPaquetes(listtoEval)
                    listaFinal.map {
                        Log.d("ListapruebaTime","$it")
                    }
                    */



//                        callback.getTimeCrudo(replaceCadena as MutableList<String>)
//                        callback.getEventCrudo(listaFinal as MutableList<String>)

                 //   var asdfg = getInfoList()
                  /*  ListapruebaTime.map {
                        Log.d("ListapruebaTime","$it")
                    }*/

                 //    DividirLoggerTime(ListapruebaTime)


                    // Llama a la función de callback en el hilo principal después de recopilar datos
                    withContext(Dispatchers.Main) {

                        runOnUiThread {
                            callback.onProgress("Finalizando")
                            callback.getTime( agregar00AlPenultimoCaracter(ValoresFiltrados as MutableList<String>,2))
                            callback.onSuccess(true)
                        }

                        // Aquí puedes realizar operaciones en el hilo principal, como actualizar la UI o notificar la finalización
//                        callback.onComplete()
                    }
                }
            } catch (e: Exception) {
                // Manejar cualquier excepción que pueda ocurrir durante la ejecución de las corrutinas
                Log.e("Error", "Error en getLog: ${e.message}")
            }
        }
    }





    fun quitarEspaciosYConvertirAStringBuilder(lista: MutableList<String>): StringBuilder {
        val sb = StringBuilder()
        for (item in lista) {
            sb.append(item.replace(" ", ""))
        }
        return sb
    }

    fun dividirpaquetes28TIme(listaFinal: MutableList<String>) : MutableList<String> {
        val FinalLIstaTime = mutableListOf<String>()
        for (registro in listaFinal) {
            var startIndex = 0
            var registrosParciales = mutableListOf<String>()

            // Dividir cada registro en segmentos de 18 caracteres
            while (startIndex < registro.length) {
                val endIndex = minOf(startIndex + 28, registro.length)
                registrosParciales.add(registro.substring(startIndex, endIndex))
                startIndex += 28
            }

            // Agregar los segmentos del registro actual al resultado final
            FinalLIstaTime.addAll(registrosParciales)
        }
        val listaFiltrada = FinalLIstaTime.filter { it != "0000000000000000000000000000" && it.length >= 28 }
        return listaFiltrada as MutableList<String>
    }
    fun dividirpaquetes18TIme(listaFinal: MutableList<String>) : MutableList<String> {
        val FinalLIstaTime = mutableListOf<String>()
        for (registro in listaFinal) {
            var startIndex = 0
            var registrosParciales = mutableListOf<String>()

            // Dividir cada registro en segmentos de 18 caracteres
            while (startIndex < registro.length) {
                val endIndex = minOf(startIndex + 18, registro.length)
                registrosParciales.add(registro.substring(startIndex, endIndex))
                startIndex += 18
            }

            // Agregar los segmentos del registro actual al resultado final
            FinalLIstaTime.addAll(registrosParciales)
        }
        val listaFiltrada = FinalLIstaTime.filter { it != "000000000000000000" && it.length >= 18 }
        return listaFiltrada as MutableList<String>
    }
    fun procesarLista(lista: List<String>): List<String> {
        return lista.map {
            val sinEspacios = it.replace(" ", "")
            if (sinEspacios.length > 24) {
                sinEspacios.substring(16, sinEspacios.length - 8)
            } else {
                "" // Si la cadena es demasiado corta para el procesamiento, devolvemos una cadena vacía
            }
        }
    }
    fun dividirEnPaquetes(cadena: String, tamanoPaquete: Int): List<String> {
        val longitudTotal = cadena.length
        val numPaquetes = (longitudTotal + tamanoPaquete - 1) / tamanoPaquete // Calcula el número de paquetes

        val paquetes = mutableListOf<String>()

        for (i in 0 until numPaquetes) {
            val inicio = i * tamanoPaquete
            val fin = minOf((i + 1) * tamanoPaquete, longitudTotal)
            val paquete = cadena.substring(inicio, fin)
            paquetes.add(paquete)
        }

        return paquetes
    }



    private fun DividirLoggerTime(listapruebaTime: MutableList<String>){
        var arrayListInfo: MutableList<String?> = ArrayList()


                if (!listapruebaTime.isEmpty()) {
                    listapruebaTime.map {
                        Log.d("ListapruebaTime","$it")
                    }
                    Log.d("ListapruebaTime","\n \n")

                    val s = quitarEspaciosYConvertirAStringBuilder(listapruebaTime)
                    val datos: MutableList<String> = ArrayList()
                    val datos2: MutableList<String> = ArrayList()
                    //header
                    Log.d("", "sssTiempo:$s")
                    arrayListInfo.add(s.substring(0, 4)) //head
                    arrayListInfo.add(s.substring(4, 12)) //
                    arrayListInfo.add(s.substring(12, 14)) //modelo trefpb
                    arrayListInfo.add(s.substring(14, 16)) //version
                    Log.d("datosCrudosTiempoSALidaFinal","------------> arrayListInfo $arrayListInfo")
                    val st = StringBuilder()
                    st.append(s.substring(16, s.length - 8))
                    var resultado = mutableListOf<String>()
                    /*
                    val resultado = mutableListOf<String>()
                    var inicio = 0

                    while (inicio < st.length) {
                        val fin = minOf(inicio + 256, st.length)
                        resultado.add(st.substring(inicio, fin))
                        inicio += 256
                    }
                    */

                    var resultadoFinal = mutableListOf<String>()
//                     resultado = dividirEnPaquetes(s)
//                    Log.d("RecorreLaLIsta","resultado ${resultado.size}")
//                    var toDividir = mutableListOf<String>()
//                    resultado.map {
//                        Log.d("ListapruebaTime","$it")
//                        toDividir.add(it)
//                    }



               //     toDividir.addAll(resultado.subList(0, resultado.size - 1))

//                    Log.d("RecorreLaLIsta","toDividir ${toDividir.size}")
//                    toDividir.map{
//                        Log.d("RecorreLaLIsta","item ${it.substring(0,20)}")
//                    }


                 //   toDividir.forEach { Log.d("resultadoFinal",it) }


                    /*do { //dividir los paquetes de 128 bytes según el protocolo
                        var i = 0
                        var j = 0
                        while (i < toDividir[j].length) {
                            if (i + 18 > toDividir[j].length) {
                                resultadoFinal.add(toDividir[j].substring(i)) //checksum
                                break
                            } else resultadoFinal.add(toDividir[j].substring(i, i + 18))
                            i += 18
                        }
                        j++
                    } while (j < toDividir.size)

                    */


                    // Recorrer cada elemento en la lista data
                  /*
                    for (registro in toDividir) {
                        var startIndex = 0
                        val registrosParciales = mutableListOf<String>()

                        // Dividir cada registro en segmentos de 18 caracteres
                        while (startIndex < registro.length) {
                            val endIndex = minOf(startIndex + 18, registro.length)
                            registrosParciales.add(registro.substring(startIndex, endIndex))
                            startIndex += 18
                        }

                        // Agregar los segmentos del registro actual al resultado final
                        resultadoFinal.addAll(registrosParciales)
                    }
                    resultadoFinal.forEachIndexed { index, segmento ->
                      Log.d("resultadoFinal","$segmento  ${resultadoFinal.size}")
                    }
                    */


                }


    }
    private fun DividirLoggerTime12(listapruebaTime: MutableList<String>) {

        var arrayListInfo: MutableList<String?> = ArrayList()

        if (!listapruebaTime.isEmpty()) {
            val s = quitarEspaciosYConvertirAStringBuilder(listapruebaTime)
            val datos: MutableList<String> = ArrayList()
            val datos2: MutableList<String> = ArrayList()
            //header
            Log.d("", "sssTiempo:$s")
            arrayListInfo.add(s.substring(0, 4)) //head
            arrayListInfo.add(s.substring(4, 12)) //
            arrayListInfo.add(s.substring(12, 14)) //modelo trefpb
            arrayListInfo.add(s.substring(14, 16)) //version
            Log.d("datosCrudosTiempoSALidaFinal","------------> arrayListInfo $arrayListInfo")
            val st = StringBuilder()
            st.append(s.substring(16, s.length - 8))
            val resultado = mutableListOf<String>()
            /*
            val resultado = mutableListOf<String>()
            var inicio = 0

            while (inicio < st.length) {
                val fin = minOf(inicio + 256, st.length)
                resultado.add(st.substring(inicio, fin))
                inicio += 256
            }
            */

            var resultadoFinal = mutableListOf<String>()
            var i = 0
            do { //dividir toda la información en paquetes de 128 bytes
             //   Log.d("datosCrudosTiempoSALidaFinal","i + 256 ${i + 256 } i $i ")
                if (i + 256 > st.length) {
                    resultado.add(st.substring(i)) //checksum
                    break
                } else resultado.add(st.substring(i, i + 256))
                i += 256
            } while (i < st.length)

            resultado.map {
                Log.d("ListapruebaTime","$it")
            }

            var toDividir = mutableListOf<String>()

            toDividir.addAll(resultado.subList(0, resultado.size - 1))


            println("Datos en toDividir:")
            do { //dividir los paquetes de 128 bytes según el protocolo
                var i = 0
                var j = 0
                while (i < toDividir[j].length) {
                    if (i + 18 > toDividir[j].length) {
                        resultadoFinal.add(toDividir[j].substring(i)) //checksum
                        break
                    } else resultadoFinal.add(toDividir[j].substring(i, i + 18))
                    i += 18
                }
                j++
            } while (j < toDividir.size)
      /*      Log.d("ListapruebaTime","st ${st.length} resultado  ${quitarEspaciosYConvertirAStringBuilder(resultado).length} ")
            var j = 0

            var asd = mutableListOf<String>()
             asd.add(resultado.get(5))
dsdsd
            do { //dividir los paquetes de 128 bytes según el protocolo
                i = 0
                while (i < asd[j].length) {
                    if (i + 18 > asd[j].length) {
                        resultadoFinal.add(asd[j].substring(i)) //checksum
                        break
                    } else resultadoFinal.add(asd[j].substring(i, i + 18))
                    i += 18
                }
                j++
            } while (j < asd.size)
*/
            /*
            for (registro in resultado) {
                // Dividimos el registro en fragmentos de 18 caracteres
                var index = 0
                while (index + 18 <= registro.length) {
                  //  Log.d("ListapruebaTime", "${registro.substring(index, index + 18)}")
                    resultadoFinal.add(registro.substring(index, index + 18))
                    println(registro.substring(index, index + 18))
                    index += 18
                }
                // Imprimimos los caracteres sobrantes
                if (index < registro.length) {
                    Log.d("ListapruebaTime", "${registro.substring(index)}")
                    println(registro.substring(index))
                }
            }
*/

            resultadoFinal.map {
                Log.d("resultadoFinal","$it")
            }
/*
            do { //dividir los paquetes de 128 bytes según el protocolo
                i = 0

                while (i < resultado[j].length) {
                    if (i + 18 > resultado[j].length) {
                        datos2.add(resultado[j].substring(i)) //checksum
                        break
                    } else datos2.add(resultado[j].substring(i, i + 18))
                    i += 18
                }
                j++
            } while (j < resultado.size)
*/
            /*
             var i = 0
                Log.d("datosCrudosTiempoSALidaFinal","st $st")
                do { //dividir toda la información en paquetes de 128 bytes
                    Log.d("datosCrudosTiempoSALidaFinal","i + 256 ${i + 256 } i $i ")
                    if (i + 256 > st.length) {
                        datos.add(st.substring(i)) //checksum
                        break
                    } else datos.add(st.substring(i, i + 256))
                    i += 256
                } while (i < st.length)
            * */

            /*  Log.d("LPOPOP", "crudotiempoTIEMPO:$fwversion")
              Log.d("LPOPOP", "crudotiempoTIEMPO:$modelo")

             */
            /*
            //if (sp?.getString("numversion", "")  /*fwversion*/ == "1.02" &&  sp?.getString("modelo", "")/*modelo */== "3.3" || sp?.getString("numversion", "") == "1.04" && sp?.getString("modelo", "") == "3.5") {
            if (/*sp?.getString("numversion", "") == "1.02" && sp?.getString("modelo", "") == "3.3"
                || sp?.getString("numversion", "") == "1.04" && sp?.getString("modelo", "") == "3.5"
                || sp?.getString("numversion", "") == "1.01" && sp?.getString("modelo", "") == "8.1"*/true
            ) { //nuevo logger, diferente división de información
                var i = 0
                Log.d("datosCrudosTiempoSALidaFinal","st $st")
                do { //dividir toda la información en paquetes de 128 bytes
                    Log.d("datosCrudosTiempoSALidaFinal","i + 256 ${i + 256 } i $i ")
                    if (i + 256 > st.length) {
                        datos.add(st.substring(i)) //checksum
                        break
                    } else datos.add(st.substring(i, i + 256))
                    i += 256
                } while (i < st.length)
                var j = 0
                do { //dividir los paquetes de 128 bytes según el protocolo
                    i = 0
                    while (i < datos[j].length) {
                        if (i + 18 > datos[j].length) {
                            datos2.add(datos[j].substring(i)) //checksum
                            break
                        } else datos2.add(datos[j].substring(i, i + 18))
                        i += 18
                    }
                    j++
                } while (j < datos.size)
                Log.d("crudotiempoTIEMPO", "crudotiempoTIEMPO:$datos2")
                //Log.d("","ultimonTIEMPO:"+datos2.get(datos2.size()-2));

                //organizar la información que realmente sirve (quitar 0s)
                var h = 4
                //String numeroRegistrosNuevos = datos2.get(datos.size()-2);
                var o = 0
                while (o < datos2.size) {
                    if (datos2[o].length != 4 && datos2[o] != "000000000000000000") {
                        arrayListInfo.add(datos2[o])
                        Log.d(
                            "crudotiempoTIEMPO",
                            "crudotiempoFOR:" + arrayListInfo[h]
                        )
                        h++
                    }
                    o++
                }
                Log.d("crudotiempoTIEMPO", "crudotiempo:" + arrayListInfo)
            }
            else {
                var i = 16
                do {
                    if (i + 18 > s.length) {
                        //arrayListInfo.add(s.substring(i));//checksum
                        break
                    } else arrayListInfo.add(
                        s.substring(
                            i,
                            i + 18
                        )
                    )
                    i = i + 18
                } while (i < s.length)
                Log.d("crudotiempoTIEMPO", "crudotiempo:" + arrayListInfo)
            }
            */
            //data
        }

    }


    inner class MyAsyncTaskUpdateFirmware(
        var FWWW: String,
        var fw: String,
        var callback: MyCallback
    ) :
        AsyncTask<Int?, Int?, String>() {
        protected override fun doInBackground(vararg params: Int?): String? {
            dividirFirmware102(FWWW)
            titulo = "Actualizar a Firmware Personalizado"
            command = "4046"
            Log.d("ProgressFirmware", "1756")
            callback.onProgress("Iniciando")
            //if ()//necesita limpiar memoria al actualizar el fw?
            Log.d("MODELO leìdo en equipo", ":" + sp!!.getString("modelo", ""))
            Log.d("MODELO extraido de fw", ":$newFimwareModelo")
            Log.d("MODELO extraido de fw", ":$newFirmwareVersion")
            Log.d("MODELO extraido de fw", "fw :$fw")
            val modeloActual: String = sp!!.getString("modelo", "")!!
            val modeloEnFw: String = newFimwareModelo

            //if (fw.equals("")){
            //if (modeloActual.equals(modeloEnFw)){
            if (bluetoothLeService == null) {
                callback.onSuccess(false)
                callback.onError("notConnected")
                return "notConnected"
            } else bluetoothLeService!!.sendComando(command)
            return "resp"

        }

        override fun onPostExecute(result: String) {
            try {
                Thread.sleep(800)


                Log.d(
                    "ProgressFirmware",
                    "numversion ${
                        sp!!.getString(
                            "numversion",
                            ""
                        )!!
                    } modelo ${sp!!.getString("modelo", "")!!}"
                )
                //Pedir permisos de ubicacion
                listData.clear()
                listData = bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                FinalListData.clear()
                //    Log.d("ProgressFirmware","1797 FinalListData $FinalListData  ${getInfoList()  as List<String>}")
                FinalListData = GetRealDataFromHexaImbera.convert(
                    listData as List<String>,
                    titulo,
                    sp!!.getString("numversion", "")!!,
                    sp!!.getString("modelo", "")!!
                ) as MutableList<String?>
                //if (result.equals("noCompatibleModelo")){
                //FinalList.add("noCompatibleModelo");
                //showInfoPopup("Actualización de firmware","La versión de Modelo mínima que debe tener este equipo para instalar la actualización es:"+newFimwareModelo);
                //}else{
                Log.d("ProgressFirmware", "1805 result $result")
                if (result == "notConnected") {
                    callback.onSuccess(false)
                    callback.onError("notConnected")
                    makeToast("Conéctate a un BLE")
                } else {
                    Log.d("ProgressFirmware", "1809 FinalListData $FinalListData  ")
                    if (!FinalListData.isEmpty()) {
                        if (FinalListData[0] == "F103") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                            callback.onProgress("Realizando")
                            Log.d("MODELO extraido de fw", fw)
                            Log.d(
                                "ProgressFirmware",
                                "\"F103\") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb"
                            )
                            MyAsyncTaskUpdateFirmware2(fw, callback).execute()
                        } else {
                            callback.onSuccess(false)
                            Log.d("ProgressFirmware", "1812 UpdateFirmwareFail")
                            FinalList.add("UpdateFirmwareFail")
                            callback.onError("UpdateFirmwareFail")
                            GlobalTools.showInfoPopup(
                                "Actualización de firmware",
                                "La actualización de Firmware falló inesperádamente por respuesta, intenta de nuevo.",
                                getContext!!
                            )
                        }
                    } else {
                        callback.onSuccess(false)
                        FinalList.add("UpdateFirmwareFail")
                        GlobalTools.showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware falló inesperádamente no hay respuesta, intenta de nuevo,",
                            getContext!!
                        )
                    }
                }

                //}
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        override fun onPreExecute() {
            progressDialog2.second("Realizando operaciones...")
            //createProgressDialog("Realizando operaciones...")
        }


    }

    internal inner class MyAsyncTaskUpdateFirmware2(var fw: String, callback1: MyCallback) :
        AsyncTask<Int?, Int?, String>() {
        //Log.d("TestFW",":"+Integer.toHexString(FinalFirmwareCommands.size()));
        var callback = callback1
        protected override fun doInBackground(vararg params: Int?): String? {

            if (bluetoothLeService == null) {
                callback.onSuccess(false)
                callback.onError("notConnected")
                return "notConnected"
            } else Log.d("sizeFWHE", ":" + Integer.toHexString(FinalFirmwareCommands.size))
            bluetoothLeService!!.sendComando(
                "4049" + Integer.toHexString(FinalFirmwareCommands.size),
                ""
            )
            return "resp"
        }

        override fun onPostExecute(result: String) {
            try {
                Thread.sleep(400)
                if (progressDialog2.returnISshowing()) {
                    progressDialog2.secondStop()
                }
                //Pedir permisos de ubicacion
                listData.clear()
                listData = bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                FinalListData.clear()
                FinalListData = GetRealDataFromHexaImbera.convert(
                    listData as List<String>,
                    titulo,
                    sp!!.getString("numversion", "")!!,
                    sp!!.getString("modelo", "")!!
                ) as MutableList<String?>
                //FinalList.clear();
                //FinalList = GetRealDataFromHexa.GetRealData(FinalListData,titulo);
                if (result == "notConnected") {
                    callback.onSuccess(false)
                    callback.onError("notConnected")
                    makeToast("Conéctate a un BLE")
                } else {
                    if (FinalListData.get(0) == "F107") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                        Log.d("MODELO extraido de fw", " fw $fw")
                        MyAsyncTaskUpdateFirmware3(fw, callback).execute()
                    } else {
                        callback.onSuccess(false)
                        FinalList.add("UpdateFirmwareFail")
                        GlobalTools.showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware falló inesperádamente, intenta de nuevo más tarde",
                            getContext!!
                        )
                    }
                }

                //showdataPopup(listData);
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        override fun onPreExecute() {
            // createProgressDialog("Realizando operaciones...")
            progressDialog2.second("Realizando operaciones...")
        }


    }

    internal inner class MyAsyncTaskUpdateFirmware3(var fw: String, callback1: MyCallback) :
        AsyncTask<Int?, Int?, String>() {
        var callback = callback1
        protected override fun doInBackground(vararg params: Int?): String? {

            if (bluetoothLeService == null) {
                callback.onSuccess(false)
                callback.onError("cancelNoConnect")
                return "cancelNoConnect"
            } else {
                //Log.d("Progress Firmware update:","size:"+FinalFirmwareCommands.size());
                var i = 0
                while (i < FinalFirmwareCommands.size) {
                    Log.d("FinalFirmwareCommands", ":" + FinalFirmwareCommands.get(i))
                    bluetoothLeService!!.sendComando(FinalFirmwareCommands.get(i), "")
                    try {
                        Thread.sleep(150)
                        listData.clear()
                        listData =
                            bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                        FinalListData.clear()
                        FinalListData = GetRealDataFromHexaImbera.convert(
                            listData as List<String>,
                            titulo,
                            sp!!.getString("numversion", "")!!,
                            sp!!.getString("modelo", "")!!
                        ) as MutableList<String?>
                        if (FinalListData.isEmpty()) {
                            Thread.sleep(500)
                            listData.clear()
                            listData =
                                bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                            FinalListData.clear()
                            FinalListData = GetRealDataFromHexaImbera.convert(
                                listData as List<String>,
                                titulo,
                                sp!!.getString("numversion", "")!!,
                                sp!!.getString("modelo", "")!!
                            ) as MutableList<String?>
                            if (FinalListData.isEmpty()) {
                                callback.onError("cancel")
                                return "cancel"
                            } else {
                                if (FinalListData.get(0) == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                                    if (i + 1 == FinalFirmwareCommands.size) {
                                        //AL finalizar los paquetes se envia comando de temrinacion + checksum total
                                        val c = Integer.toHexString(checksumTotal)
                                        var finalchecksum = ""
                                        finalchecksum = c.padStart(8, '0')
                                        //   if (c.length == 1) "0000000$c" else if (c.length == 2) "000000$c" else if (c.length == 3) "00000$c" else if (c.length == 4) "0000$c" else if (c.length == 5) "000$c" else if (c.length == 6) "00$c" else if (c.length == 7) "0$c" else c
                                        bluetoothLeService!!.sendComando("404A$finalchecksum")
                                        //revision de la ultima respuesta del trefp despues del checksum
                                        Thread.sleep(150)
                                        listData.clear()
                                        listData =
                                            bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                                        FinalListData.clear()
                                        FinalListData = GetRealDataFromHexaImbera.convert(
                                            listData as List<String>,
                                            titulo,
                                            sp!!.getString("numversion", "")!!,
                                            sp!!.getString("modelo", "")!!
                                        ) as MutableList<String?>
                                        return if (FinalListData.isEmpty()) {
                                            callback.onSuccess(false)
                                            callback.onError("cancel")
                                            "cancel"
                                        } else {
                                            if (FinalListData.get(0) == "F13D") {
                                                "correct"
                                            } else "cancelFialChecksumError"
                                        }
                                    }
                                } else {
                                    i = FinalFirmwareCommands.size
                                }
                            }
                        } else {
                            if (FinalListData.get(0) == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                                if (i + 1 == FinalFirmwareCommands.size) {
                                    //AL finalizar los paquetes se envia comando de temrinacion + checksum total
                                    val c = Integer.toHexString(checksumTotal)
                                    var finalchecksum = ""
                                    finalchecksum = c.padStart(8, '0')
                                    //  if (c.length == 1) "0000000$c" else if (c.length == 2) "000000$c" else if (c.length == 3) "00000$c" else if (c.length == 4) "0000$c" else if (c.length == 5) "000$c" else if (c.length == 6) "00$c" else if (c.length == 7) "0$c" else c
                                    Log.d("checksumFinal", ":$finalchecksum")
                                    bluetoothLeService!!.sendComando("404A$finalchecksum")
                                    //revision de la ultima respuesta del trefp despues del checksum
                                    Thread.sleep(150)
                                    listData.clear()
                                    listData =
                                        bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                                    FinalListData.clear()
                                    FinalListData = GetRealDataFromHexaImbera.convert(
                                        listData as List<String>,
                                        titulo,
                                        sp!!.getString("numversion", "")!!,
                                        sp!!.getString("modelo", "")!!
                                    ) as MutableList<String?>
                                    return if (FinalListData.isEmpty()) {
                                        callback.onSuccess(false)
                                        callback.onError("cancel")
                                        "cancel"
                                    } else {
                                        if (FinalListData.get(0) == "F13D") {
                                            "correct"
                                        } else "cancelFialChecksumError"
                                    }
                                }
                            } else {
                                i = FinalFirmwareCommands.size
                            }
                        }
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    i++
                }
            }
            return "resp"
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onPostExecute(result: String) {
            /*if (progressdialog != null) progressdialog.dismiss()
            progressdialog = null*/
            if (progressDialog2.returnISshowing()) {
                progressDialog2.secondStop()
            }
            Log.d("MODELO extraido de fw", " 2837 fw $fw result $result")
            if (fw == "newfwTrefpResetMemoryWF") {
                Log.d("BLR", "newfwTrefpResetMemoryWF")
                if (result == "cancelFialChecksumError") {
                    callback.onSuccess(false)
                    callback.onError("La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)")
                    GlobalTools.showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)",
                        getContext!!
                    )
                } else if (result == "cancel") {
                    callback.onSuccess(false)
                    callback.onError("La actualización de Firmware falló inesperádamente, intenta de nuevo.")
                    GlobalTools.showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo.",
                        getContext!!
                    )
                } else if (result == "") {
                    callback.onSuccess(false)
                    callback.onError("No estás conectado a ningún BLE, conéctate e intenta de nuevo")
                    GlobalTools.showInfoPopup(
                        "Actualización de firmware",
                        "No estás conectado a ningún BLE, conéctate e intenta de nuevo",
                        getContext!!
                    )
                } else {
                    if (FinalListData.get(0) == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                        /* com.example.imberap.BluetoothServices.BluetoothServices.MyAsyncTaskUpdateFirmwareResetMemoryWF()
                             .execute()
                         */
                    } else {
                        callback.onSuccess(false)
                        callback.onError("La actualización de Firmware tuvo problemas para completarse")
                        GlobalTools.showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware tuvo problemas para completarse",
                            getContext!!
                        )
                    }
                }
            } else if (fw == "newfwTrefpResetMemory") {
                Log.d("BLR", "newfwTrefpResetMemory")
                if (result == "cancelFialChecksumError") {
                    callback.onSuccess(false)
                    callback.onError("La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)")
                    GlobalTools.showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)",
                        getContext!!
                    )
                } else if (result == "cancel") {
                    callback.onSuccess(false)
                    callback.onError("La actualización de Firmware falló inesperádamente, intenta de nuevo.")
                    GlobalTools.showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo.",
                        getContext!!
                    )
                } else if (result == "") {
                    callback.onSuccess(false)
                    callback.onError("No estás conectado a ningún BLE, conéctate e intenta de nuevo")
                    GlobalTools.showInfoPopup(
                        "Actualización de firmware",
                        "No estás conectado a ningún BLE, conéctate e intenta de nuevo",
                        getContext!!
                    )
                } else {
                    if (FinalListData.get(0) == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                        MyAsyncTaskUpdateFirmwareResetMemory(callback)//.execute()
                    } else {
                        GlobalTools.showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware tuvo problemas para completarse",
                            getContext!!
                        )
                    }
                }
            } else if (fw == "fwPlantillaOperador") {
                Log.d("BLROperardor", "sendfwOpradorPlantilla")
                if (result == "cancelFialChecksumError") {
                    GlobalTools.showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)",
                        getContext!!
                    )
                } else if (result == "cancel") {
                    GlobalTools.showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo.",
                        getContext!!
                    )
                } else if (result == "") {
                    GlobalTools.showInfoPopup(
                        "Actualización de firmware",
                        "No estás conectado a ningún BLE, conéctate e intenta de nuevo",
                        getContext!!
                    )
                } else {
                    if (FinalListData.get(0) == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                        makeToast(
                            "Actualización de Firmware: Correcta"
                        )
                        MyAsyncTaskUpdatePlantillaPostFwOperador(callback)//.execute()
                    } else {
                        GlobalTools.showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware tuvo problemas para completarse",
                            getContext!!
                        )
                    }
                }
            } else if (fw == "fwPlantillaDEVCEO" || fw == "fwPlantillaDEVCEBIN") {
                Log.d("BLROperardor", "sendfwOpradorPlantilla")
                if (result == "cancelFialChecksumError") {
                    GlobalTools.showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)",
                        getContext!!
                    )
                } else if (result == "cancel") {
                    GlobalTools.showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo.",
                        getContext!!
                    )
                } else if (result == "") {
                    GlobalTools.showInfoPopup(
                        "Actualización de firmware",
                        "No estás conectado a ningún BLE, conéctate e intenta de nuevo",
                        getContext!!
                    )
                } else {
                    if (FinalListData.get(0) == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                        makeToast(
                            "Actualización de Firmware: Correcta"
                        )
                        if (fw == "fwPlantillaDEVCEO") {
                            /*       com.example.imberap.BluetoothServices.BluetoothServices.MyAsyncTaskUpdatePlantillaPostFwDEVCEO()
                                       .execute()*/
                        } else if (fw == "fwPlantillaDEVCEBIN") {
                            /*      com.example.imberap.BluetoothServices.BluetoothServices.MyAsyncTaskUpdatePlantillaPostFwDEVCEBIN()
                                      .execute()*/
                        }
                    } else {
                        GlobalTools.showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware tuvo problemas para completarse",
                            getContext!!
                        )
                    }
                }
            } else if (fw == "fwPlantillaTecnicoRepare") {
                Log.d("BLROperardor", "sendfwOpradorPlantilla")
                if (result == "cancelFialChecksumError") {
                    GlobalTools.showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)",
                        getContext!!
                    )
                } else if (result == "cancel") {
                    GlobalTools.showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo.",
                        getContext!!
                    )
                } else if (result == "") {
                    GlobalTools.showInfoPopup(
                        "Actualización de firmware",
                        "No estás conectado a ningún BLE, conéctate e intenta de nuevo",
                        getContext!!
                    )
                } else {
                    if (FinalListData.get(0) == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                        makeToast(
                            "Actualización de Firmware: Correcta"
                        )
                        /* com.example.imberap.BluetoothServices.BluetoothServices.MyAsyncTaskUpdatePlantillaPostFwTecnicovoltajeVFS24ConUpdatedeFW()
                             .execute()*/
                    } else {
                        GlobalTools.showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware tuvo problemas para completarse",
                            getContext!!
                        )
                    }
                }
            } else {
                if (result == "cancelFialChecksumError") {
                    GlobalTools.showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch).",
                        getContext!!
                    )
                } else if (result == "cancel") {
                    GlobalTools.showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo,",
                        getContext!!
                    )
                } else if (result == "") {
                    GlobalTools.showInfoPopup(
                        "Actualización de firmware",
                        "No estás conectado a ningún BLE, conéctate e intenta de nuevo",
                        getContext!!
                    )
                } else {
                    if (FinalListData.get(0) == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                        runOnUiThread {

                            callback.onSuccess(true)
                            callback.onProgress("Finalizando")

                        }

                        /*  showInfoPopup(
                              "Actualización de firmware",
                              "La actualización de Firmware se realizó de manera exitosa" , getContext!!
                          )*/
                    } else {
                        runOnUiThread {

                            callback.onSuccess(false)
                            callback.onProgress("La actualización de Firmware tuvo problemas para completarse")

                        }
                        GlobalTools.showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware tuvo problemas para completarse",
                            getContext!!
                        )
                    }
                }
            }

        }

        override fun onPreExecute() {
            progressDialog2.second(
                "Actualizando versión " + sp!!.getString(
                    "numversion",
                    ""
                ) + " \n a versión " + newFirmwareVersion
            )
        }


    }

    /*
    fun MyAsyncTaskUpdateFirmware1(firmwareString: String, callback: MyCallback) {

        fw = "newfwTrefpResetMemory"
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            // Operaciones dentro de la corrutina
            command = "4046"
            titulo = "Actualizar a Firmware Personalizado"
            divideCadenaFirmware256(firmwareString)
            dividirFirmware102(firmwareString)
            val pre = withContext(Dispatchers.Default) {
                callback.onProgress("Iniciando")


                //if ()//necesita limpiar memoria al actualizar el fw?
                Log.d("MODELO leìdo en equipo", ":" + sp!!.getString("modelo", ""))
                Log.d("MODELO extraido de fw", ":$newFimwareModelo")
                Log.d("MODELO extraido de fw", ":$newFirmwareVersion")
                // Log.d("MODELO extraido de fw", "fw :$fw")
                val modeloActual = sp!!.getString("modelo", "")
                val modeloEnFw: String = newFimwareModelo

                //if (fw.equals("")){
                //if (modeloActual.equals(modeloEnFw)){
                if (bluetoothLeService == null) {
                    return@withContext "notConnected"
                } else bluetoothLeService!!.sendComando(command)
                return@withContext "resp"

            }
            val result = withContext(Dispatchers.Default) {
                try {
                    Thread.sleep(400)
                    if (progressdialog != null) progressdialog!!.dismiss()
                    progressdialog = null
                    //Pedir permisos de ubicacion
                    listData.clear()
                    listData =
                        bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                    FinalListData.clear()
                    FinalListData = GetRealDataFromHexaImbera.convert(
                        listData as List<String>, titulo,
                        sp!!.getString("numversion", "")!!, sp!!.getString("modelo", "")!!
                    ) as MutableList<String?>
                    //if (result.equals("noCompatibleModelo")){
                    //FinalList.add("noCompatibleModelo");
                    //showInfoPopup("Actualización de firmware","La versión de Modelo mínima que debe tener este equipo para instalar la actualización es:"+newFimwareModelo);
                    //}else{
                    if (pre == "notConnected") {
                        makeToast("Conéctate a un BLE")//   Toast.makeText(context, "Conéctate a un BLE", Toast.LENGTH_SHORT).show()
                    } else {
                        if (!FinalListData.isEmpty()) {
                            if (FinalListData[0] == "F103") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                                //    Log.d("MODELO extraido de fw", fw)
                                // MyAsyncTaskUpdateFirmware2(fw).execute()
                                Log.d("ProgressFirmware", "MyAsyncTaskUpdateFirmware F103")

                                if (bluetoothLeService == null) {
                                    return@withContext "notConnected"
                                } else Log.d(
                                    "sizeFWHE",
                                    ":" + Integer.toHexString(FinalFirmwareCommands.size)
                                )
                                bluetoothLeService!!.sendComando(
                                    "4049" + Integer.toHexString(
                                        FinalFirmwareCommands.size
                                    ), ""
                                )
                                return@withContext "resp"

                            } else {
                                FinalList.add("UpdateFirmwareFail")
                                showInfoPopup(
                                    "Actualización de firmware",
                                    "La actualización de Firmware falló inesperádamente por respuesta, intenta de nuevo.",
                                    getContext!!
                                )
                            }
                        } else {
                            FinalList.add("UpdateFirmwareFail")
                            showInfoPopup(
                                "Actualización de firmware",
                                "La actualización de Firmware falló inesperádamente no hay respuesta, intenta de nuevo,",
                                getContext!!
                            )
                        }
                    }

                    //}
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }


            val onPostExecuteFW2 = withContext(Dispatchers.Default) {
                try {
                    Thread.sleep(400)
                    if (progressdialog != null) progressdialog!!.dismiss()
                    progressdialog = null
                    //Pedir permisos de ubicacion
                    listData.clear()
                    listData =
                        bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                    FinalListData.clear()
                    FinalListData = GetRealDataFromHexaImbera.convert(
                        listData as List<String>, titulo,
                        sp!!.getString("numversion", "")!!, sp!!.getString("modelo", "")!!
                    ) as MutableList<String?>
                    //FinalList.clear();
                    //FinalList = GetRealDataFromHexa.GetRealData(FinalListData,titulo);
                    if (result == "notConnected") {
                        makeToast("Conéctate a un BLE")
                    } else {
                        if (FinalListData[0] == "F107") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                            Log.d(
                                "ProgressFirmware",
                                "F107 comando de confirmaciòn de actualizacion de firmware desde el trefpb"
                            )
                            //   Log.d("MODELO extraido de fw", " fw $fw")
                              MyAsyncTaskUpdateFirmware3(fw, callback).execute()
                      /*      if (bluetoothLeService == null) {
                                FinalFirmwareCommands
                                return@withContext "cancelNoConnect"
                            } else {
                                Log.d("ProgressFirmware", "else 1866")
                                //Log.d("Progress Firmware update:","size:"+FinalFirmwareCommands.size());
                                   Log.d("FinalFirmwareCommands", "FinalFirmwareCommands ${FinalFirmwareCommands.size} $FinalFirmwareCommands ")
                                var i = 0
                                while (i < FinalFirmwareCommands.size) {
                                    Log.d("FinalFirmwareCommands", ":" + FinalFirmwareCommands[i])
                                    bluetoothLeService!!.sendComando(FinalFirmwareCommands[i], "")
                                    try {
                                        Thread.sleep(150)
                                        listData.clear()
                                        listData =
                                            bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                                        FinalListData.clear()
                                        FinalListData = GetRealDataFromHexaImbera.convert(
                                            listData as List<String>,
                                            titulo,
                                            sp!!.getString("numversion", "")!!,
                                            sp!!.getString("modelo", "")!!
                                        ) as MutableList<String?>
                                        if (FinalListData.isEmpty()) {
                                            Thread.sleep(500)
                                            listData.clear()
                                            listData =
                                                bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                                            FinalListData.clear()
                                            FinalListData = GetRealDataFromHexaImbera.convert(
                                                listData as List<String>,
                                                titulo,
                                                sp!!.getString("numversion", "")!!,
                                                sp!!.getString("modelo", "")!!
                                            ) as MutableList<String?>
                                            if (FinalListData.isEmpty()) {
                                                return@withContext "cancel"
                                            } else {
                                                if (FinalListData[0] == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                                                    if (i + 1 == FinalFirmwareCommands.size) {
                                                        //AL finalizar los paquetes se envia comando de temrinacion + checksum total
                                                        val c = Integer.toHexString(checksumTotal)
                                                        var finalchecksum = ""
                                                        finalchecksum = c.padStart(8, '0')
                                                        /* if (c.length == 1) "0000000$c"
                                                         else if (c.length == 2) "000000$c"
                                                         else if (c.length == 3) "00000$c"
                                                         else if (c.length == 4) "0000$c"
                                                         else if (c.length == 5) "000$c"
                                                         else if (c.length == 6) "00$c"
                                                         else if (c.length == 7) "0$c"
                                                         else c*/
                                                        bluetoothLeService!!.sendComando("404A$finalchecksum")
                                                        //revision de la ultima respuesta del trefp despues del checksum
                                                        Thread.sleep(150)
                                                        listData.clear()
                                                        listData =
                                                            bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                                                        FinalListData.clear()
                                                        FinalListData =
                                                            GetRealDataFromHexaImbera.convert(
                                                                listData as List<String>,
                                                                titulo,
                                                                sp!!.getString("numversion", "")!!,
                                                                sp!!.getString("modelo", "")!!
                                                            ) as MutableList<String?>
                                                        return@withContext if (FinalListData.isEmpty()) {
                                                            "cancel"
                                                        } else {
                                                            if (FinalListData[0] == "F13D") {
                                                                Log.d(
                                                                    "ProgressFirmware",
                                                                    "F13D correct"
                                                                )
                                                                "correct"
                                                            } else {
                                                                Log.d(
                                                                    "ProgressFirmware",
                                                                    "cancelFialChecksumError"
                                                                )
                                                                "cancelFialChecksumError"
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    i = FinalFirmwareCommands.size
                                                }
                                            }
                                        } else {
                                            if (FinalListData[0] == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                                                if (i + 1 == FinalFirmwareCommands.size) {
                                                    //AL finalizar los paquetes se envia comando de temrinacion + checksum total
                                                    val c = Integer.toHexString(checksumTotal)
                                                    var finalchecksum = ""
                                                    finalchecksum = c.padStart(8, '0')
                                                    //       if (c.length == 1) "0000000$c" else if (c.length == 2) "000000$c" else if (c.length == 3) "00000$c" else if (c.length == 4) "0000$c" else if (c.length == 5) "000$c" else if (c.length == 6) "00$c" else if (c.length == 7) "0$c" else c
                                                    Log.d("checksumFinal", ":$finalchecksum")
                                                    bluetoothLeService!!.sendComando("404A$finalchecksum")
                                                    //revision de la ultima respuesta del trefp despues del checksum
                                                    Thread.sleep(150)
                                                    listData.clear()
                                                    listData =
                                                        bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                                                    FinalListData.clear()
                                                    FinalListData =
                                                        GetRealDataFromHexaImbera.convert(
                                                            listData as List<String>,
                                                            titulo,
                                                            sp!!.getString("numversion", "")!!,
                                                            sp!!.getString("modelo", "")!!
                                                        ) as MutableList<String?>
                                                    return@withContext if (FinalListData.isEmpty()) {
                                                        "cancel"
                                                    } else {
                                                        if (FinalListData[0] == "F13D") {
                                                            "correct"
                                                        } else "cancelFialChecksumError"
                                                    }
                                                }
                                            } else {
                                                i = FinalFirmwareCommands.size
                                            }
                                        }
                                    } catch (e: InterruptedException) {
                                        e.printStackTrace()
                                    }
                                    i++
                                }
                            }
                            */
                            Log.d("ProgressFirmware", "MyAsyncTaskUpdateFirmware F107")
                          //  return@withContext "resp"

                        } else {
                            FinalList.add("UpdateFirmwareFail")
                            showInfoPopup(
                                "Actualización de firmware",
                                "La actualización de Firmware falló inesperádamente, intenta de nuevo más tarde",
                                getContext!!
                            )
                        }
                    }

                    //showdataPopup(listData);
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
            val post = withContext(Dispatchers.Default) {
                var fw = ""
                Log.d("MODELO extraido de fw", " 2837 fw $fw")
                if (fw == "newfwTrefpResetMemoryWF") {
                    Log.d("BLR", "newfwTrefpResetMemoryWF")
                    if (result == "cancelFialChecksumError") {
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)", getContext!!
                        )
                    } else if (result == "cancel") {
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware falló inesperádamente, intenta de nuevo.", getContext!!
                        )
                    } else if (result == "") {
                        showInfoPopup(
                            "Actualización de firmware",
                            "No estás conectado a ningún BLE, conéctate e intenta de nuevo", getContext!!
                        )
                    } else {
                        if (FinalListData[0] == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                            MyAsyncTaskUpdateFirmwareResetMemoryWF(callback) //  MyAsyncTaskUpdateFirmwareResetMemoryWF(callback)

                        } else {
                            showInfoPopup(
                                "Actualización de firmware",
                                "La actualización de Firmware tuvo problemas para completarse" , getContext!!
                            )
                        }
                    }
                } else if (fw == "newfwTrefpResetMemory") {
                    Log.d("BLR", "newfwTrefpResetMemory")
                    if (result == "cancelFialChecksumError") {
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)" , getContext!!
                        )
                    } else if (result == "cancel") {
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware falló inesperádamente, intenta de nuevo." , getContext!!
                        )
                    } else if (result == "") {
                        showInfoPopup(
                            "Actualización de firmware",
                            "No estás conectado a ningún BLE, conéctate e intenta de nuevo" , getContext!!
                        )
                    } else {
                        if (FinalListData[0] == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                       //     MyAsyncTaskUpdateFirmwareResetMemory().execute()
                            MyAsyncTaskUpdateFirmwareResetMemoryWF(callback)
                        } else {
                            showInfoPopup(
                                "Actualización de firmware",
                                "La actualización de Firmware tuvo problemas para completarse" , getContext!!
                            )
                        }
                    }
                } else if (fw == "fwPlantillaOperador") {
                    Log.d("BLROperardor", "sendfwOpradorPlantilla")
                    if (result == "cancelFialChecksumError") {
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)" , getContext!!
                        )
                    } else if (result == "cancel") {
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware falló inesperádamente, intenta de nuevo." , getContext!!
                        )
                    } else if (result == "") {
                        showInfoPopup(
                            "Actualización de firmware",
                            "No estás conectado a ningún BLE, conéctate e intenta de nuevo" , getContext!!
                        )
                    } else {
                        if (FinalListData[0] == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                            makeToast(
                                "Actualización de Firmware: Correcta")
                            //MyAsyncTaskUpdatePlantillaPostFwOperador().execute()
                              MyAsyncTaskUpdatePlantillaPostFwOperador(callback)
                        } else {
                            showInfoPopup(
                                "Actualización de firmware",
                                "La actualización de Firmware tuvo problemas para completarse" , getContext!!
                            )
                        }
                    }
                } else if (fw == "fwPlantillaDEVCEO" || fw == "fwPlantillaDEVCEBIN") {
                    Log.d("BLROperardor", "sendfwOpradorPlantilla")
                    if (result == "cancelFialChecksumError") {
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)" , getContext!!
                        )
                    } else if (result == "cancel") {
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware falló inesperádamente, intenta de nuevo." , getContext!!
                        )
                    } else if (result == "") {
                        showInfoPopup(
                            "Actualización de firmware",
                            "No estás conectado a ningún BLE, conéctate e intenta de nuevo" , getContext!!
                        )
                    } else {
                        if (FinalListData[0] == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                          makeToast("Actualización de Firmware: Correcta")
                            if (fw == "fwPlantillaDEVCEO") {
                               /* com.example.imberap.BluetoothServices.BluetoothServices.MyAsyncTaskUpdatePlantillaPostFwDEVCEO()
                                    .execute()*/
                                MyAsyncTaskUpdateFirmwareResetMemoryWF(callback)
                            } else if (fw == "fwPlantillaDEVCEBIN") {
                               /* com.example.imberap.BluetoothServices.BluetoothServices.MyAsyncTaskUpdatePlantillaPostFwDEVCEBIN()
                                    .execute()
                                */
                                MyAsyncTaskUpdateFirmwareResetMemoryWF(callback)
                            }
                        } else {
                            showInfoPopup(
                                "Actualización de firmware",
                                "La actualización de Firmware tuvo problemas para completarse" , getContext!!
                            )
                        }
                    }
                } else if (fw == "fwPlantillaTecnicoRepare") {
                    Log.d("BLROperardor", "sendfwOpradorPlantilla")
                    if (result == "cancelFialChecksumError") {
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)" , getContext!!
                        )
                    } else if (result == "cancel") {
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware falló inesperádamente, intenta de nuevo." , getContext!!
                        )
                    } else if (result == "") {
                        showInfoPopup(
                            "Actualización de firmware",
                            "No estás conectado a ningún BLE, conéctate e intenta de nuevo" , getContext!!
                        )
                    } else {
                        if (FinalListData[0] == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                           makeToast("Actualización de Firmware: Correcta")
                         /*   com.example.imberap.BluetoothServices.BluetoothServices.MyAsyncTaskUpdatePlantillaPostFwTecnicovoltajeVFS24ConUpdatedeFW()
                                .execute()
                            */
                            MyAsyncTaskUpdateFirmwareResetMemoryWF(callback)
                        } else {
                            showInfoPopup(
                                "Actualización de firmware",
                                "La actualización de Firmware tuvo problemas para completarse" , getContext!!
                            )
                        }
                    }
                } else {
                    Log.d("ProgressFirmware","FinalListData 2174 ${FinalListData.toString()}")
                    if (result == "cancelFialChecksumError") {
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)." , getContext!!
                        )
                    } else if (result == "cancel") {
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware falló inesperádamente, intenta de nuevo," , getContext!!
                        )
                    } else if (result == "") {
                        showInfoPopup(
                            "Actualización de firmware",
                            "No estás conectado a ningún BLE, conéctate e intenta de nuevo" , getContext!!
                        )
                    } else {
                        if (FinalListData[0] == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                            showInfoPopup(
                                "Actualización de firmware",
                                "La actualización de Firmware se realizó de manera exitosa" , getContext!!
                            )
                        } else {
                            runOnUiThread {
                            showInfoPopup(
                                "Actualización de firmware",
                                "La actualización de Firmware tuvo problemas para completarse",
                                getContext!!
                            )
                        }
                        }
                    }
                }


                Log.d("ProgressFirmware", " onPostExecuteFW2 $onPostExecuteFW2")
                callback.onProgress("Finalizando")
            }
        }
    }


        internal inner  class MyAsyncTaskUpdateFirmware3(var fw: String , callback1: MyCallback) :
        AsyncTask<Int?, Int?, String>() {
          var   callback = callback1
        protected override fun doInBackground(vararg params: Int?): String? {
            if (bluetoothLeService == null) {
                return "cancelNoConnect"
            } else {
                //Log.d("Progress Firmware update:","size:"+FinalFirmwareCommands.size());
                var i = 0
                while (i < FinalFirmwareCommands.size) {
                    Log.d("FinalFirmwareCommands", ":" + FinalFirmwareCommands.get(i))
                    bluetoothLeService!!.sendComando(FinalFirmwareCommands.get(i), "")
                    try {
                        Thread.sleep(150)
                        listData.clear()
                        listData = bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                        FinalListData.clear()
                        FinalListData = GetRealDataFromHexaImbera.convert(
                            listData as List<String>,
                            titulo,
                            sp!!.getString("numversion", "")!!,
                            sp!!.getString("modelo", "")!!
                        ) as MutableList<String?>
                        if (FinalListData.isEmpty()) {
                            Thread.sleep(500)
                            listData.clear()
                            listData = bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                            FinalListData.clear()
                            FinalListData = GetRealDataFromHexaImbera.convert(
                                listData as List<String>,
                                titulo,
                                sp!!.getString("numversion", "")!!,
                                sp!!.getString("modelo", "")!!
                            ) as MutableList<String?>
                            if (FinalListData.isEmpty()) {
                                return "cancel"
                            } else {
                                if (FinalListData.get(0) == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                                    if (i + 1 == FinalFirmwareCommands.size) {
                                        //AL finalizar los paquetes se envia comando de temrinacion + checksum total
                                        val c = Integer.toHexString(checksumTotal)
                                        var finalchecksum = ""
                                        finalchecksum =
                                            if (c.length == 1) "0000000$c" else if (c.length == 2) "000000$c" else if (c.length == 3) "00000$c" else if (c.length == 4) "0000$c" else if (c.length == 5) "000$c" else if (c.length == 6) "00$c" else if (c.length == 7) "0$c" else c
                                        bluetoothLeService!!.sendComando("404A$finalchecksum")
                                        //revision de la ultima respuesta del trefp despues del checksum
                                        Thread.sleep(150)
                                        listData.clear()
                                        listData = bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                                        FinalListData.clear()
                                        FinalListData = GetRealDataFromHexaImbera.convert(
                                            listData as List<String>,
                                            titulo,
                                            sp!!.getString("numversion", "")!!,
                                            sp!!.getString("modelo", "")!!
                                        ) as MutableList<String?>
                                        return if (FinalListData.isEmpty()) {
                                            "cancel"
                                        } else {
                                            if (FinalListData.get(0) == "F13D") {
                                                "correct"
                                            } else "cancelFialChecksumError"
                                        }
                                    }
                                } else {
                                    i = FinalFirmwareCommands.size
                                }
                            }
                        } else {
                            if (FinalListData.get(0) == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                                if (i + 1 == FinalFirmwareCommands.size) {
                                    //AL finalizar los paquetes se envia comando de temrinacion + checksum total
                                    val c = Integer.toHexString(checksumTotal)
                                    var finalchecksum = ""
                                    finalchecksum =
                                        if (c.length == 1) "0000000$c" else if (c.length == 2) "000000$c" else if (c.length == 3) "00000$c" else if (c.length == 4) "0000$c" else if (c.length == 5) "000$c" else if (c.length == 6) "00$c" else if (c.length == 7) "0$c" else c
                                    Log.d("checksumFinal", ":$finalchecksum")
                                    bluetoothLeService!!.sendComando("404A$finalchecksum")
                                    //revision de la ultima respuesta del trefp despues del checksum
                                    Thread.sleep(150)
                                    listData.clear()
                                    listData = bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                                    FinalListData.clear()
                                    FinalListData = GetRealDataFromHexaImbera.convert(
                                        listData as List<String>,
                                        titulo,
                                        sp!!.getString("numversion", "") !!,
                                        sp!!.getString("modelo", "") !!
                                    ) as MutableList<String?>
                                    return if (FinalListData.isEmpty()) {
                                        "cancel"
                                    } else {
                                        if (FinalListData.get(0) == "F13D") {
                                            "correct"
                                        } else "cancelFialChecksumError"
                                    }
                                }
                            } else {
                                i = FinalFirmwareCommands.size
                            }
                        }
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    i++
                }
            }
            return "resp"
        }

        override fun onPostExecute(result: String) {
          /*  if (progressdialog != null) progressdialog.dismiss()
            progressdialog = null*/
            Log.d("MODELO extraido de fw", " 2837 fw $fw")
            if (fw == "newfwTrefpResetMemoryWF") {
                Log.d("BLR", "newfwTrefpResetMemoryWF")
                if (result == "cancelFialChecksumError") {
                    showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)", getContext!!
                    )
                } else if (result == "cancel") {
                    showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo." ,getContext!!
                    )
                } else if (result == "") {
                    showInfoPopup(
                        "Actualización de firmware",
                        "No estás conectado a ningún BLE, conéctate e intenta de nuevo" ,getContext!!
                    )
                } else {
                    if (FinalListData.get(0) == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                      MyAsyncTaskUpdateFirmwareResetMemoryWF(callback)

                    } else {
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware tuvo problemas para completarse", getContext!!
                        )
                    }
                }
            } else if (fw == "newfwTrefpResetMemory") {
                Log.d("BLR", "newfwTrefpResetMemory")
                if (result == "cancelFialChecksumError") {
                    showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)" , getContext!!
                    )
                } else if (result == "cancel") {
                    showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo.", getContext!!
                    )
                } else if (result == "") {
                    showInfoPopup(
                        "Actualización de firmware",
                        "No estás conectado a ningún BLE, conéctate e intenta de nuevo", getContext!!
                    )
                } else {
                    if (FinalListData.get(0) == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                     MyAsyncTaskUpdateFirmwareResetMemory(callback)//  MyAsyncTaskUpdateFirmware("",callback)//  MyAsyncTaskUpdateFirmwareResetMemory()//.execute()
                    } else {
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware tuvo problemas para completarse" , getContext!!
                        )
                    }
                }
            } else if (fw == "fwPlantillaOperador") {
                Log.d("BLROperardor", "sendfwOpradorPlantilla")
                if (result == "cancelFialChecksumError") {
                    showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)" , getContext!!
                    )
                } else if (result == "cancel") {
                    showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo." , getContext!!
                    )
                } else if (result == "") {
                    showInfoPopup(
                        "Actualización de firmware",
                        "No estás conectado a ningún BLE, conéctate e intenta de nuevo" , getContext!!
                    )
                } else {
                    if (FinalListData.get(0) == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                        makeToast(
                            "Actualización de Firmware: Correcta")
                        MyAsyncTaskUpdatePlantillaPostFwOperador(callback)//.execute()
                    } else {
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware tuvo problemas para completarse" ,  getContext!!
                        )
                    }
                }
            } else if (fw == "fwPlantillaDEVCEO" || fw == "fwPlantillaDEVCEBIN") {
                Log.d("BLROperardor", "sendfwOpradorPlantilla")
                if (result == "cancelFialChecksumError") {
                    showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)" , getContext!!
                    )
                } else if (result == "cancel") {
                    showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo." , getContext!!
                    )
                } else if (result == "") {
                    showInfoPopup(
                        "Actualización de firmware",
                        "No estás conectado a ningún BLE, conéctate e intenta de nuevo" , getContext!!
                    )
                } else {
                    if (FinalListData.get(0) == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                      makeToast(    "Actualización de Firmware: Correcta")
                        if (fw == "fwPlantillaDEVCEO") {
                        //  MyAsyncTaskUpdatePlantillaPostFwDEVCEO()
                        } else if (fw == "fwPlantillaDEVCEBIN") {
                           /* com.example.imberap.BluetoothServices.BluetoothServices.MyAsyncTaskUpdatePlantillaPostFwDEVCEBIN()
                                .execute()*/
                        }
                    } else {
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware tuvo problemas para completarse" , getContext!!
                        )
                    }
                }
            } else if (fw == "fwPlantillaTecnicoRepare") {
                Log.d("BLROperardor", "sendfwOpradorPlantilla")
                if (result == "cancelFialChecksumError") {
                    showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)" , getContext!!
                    )
                } else if (result == "cancel") {
                    showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo." , getContext!!
                    )
                } else if (result == "") {
                    showInfoPopup(
                        "Actualización de firmware",
                        "No estás conectado a ningún BLE, conéctate e intenta de nuevo" , getContext!!
                    )
                } else {
                    if (FinalListData.get(0) == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                        makeToast(
                            "Actualización de Firmware: Correcta")
                       /* com.example.imberap.BluetoothServices.BluetoothServices.MyAsyncTaskUpdatePlantillaPostFwTecnicovoltajeVFS24ConUpdatedeFW()
                            .execute()
                        */
                    } else {
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware tuvo problemas para completarse" , getContext!!
                        )
                    }
                }
            } else {
                if (result == "cancelFialChecksumError") {
                    showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo (ChecksumNotMatch)." , getContext!!
                    )
                } else if (result == "cancel") {
                    showInfoPopup(
                        "Actualización de firmware",
                        "La actualización de Firmware falló inesperádamente, intenta de nuevo," , getContext!!
                    )
                } else if (result == "") {
                    showInfoPopup(
                        "Actualización de firmware",
                        "No estás conectado a ningún BLE, conéctate e intenta de nuevo" , getContext!!
                    )
                } else {
                    if (FinalListData.get(0) == "F13D") { //comando de confirmaciòn de actualizacion de firmware desde el trefpb
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware se realizó de manera exitosa", getContext!!
                        )
                    } else {
                        showInfoPopup(
                            "Actualización de firmware",
                            "La actualización de Firmware tuvo problemas para completarse" , getContext!!
                        )
                    }
                }
            }
        }

        override fun onPreExecute() {
         /*   createProgressDialog(
                "Actualizando versión " + sp.getString(
                    "numversion",
                    ""
                ) + " a versión " + newFirmwareVersion
            )*/
        }


    }
*/
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun MyAsyncTaskUpdateFirmwareResetMemory(callback: MyCallback) {

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {

            val result = withContext(Dispatchers.Default) {
                return@withContext try {
                    callback.onProgress("MyAsyncTaskUpdateFirmwareResetMemory")
                    callback.onProgress("Terminando la actualización espera un momento")

                    progressDialog2.second("Terminando la actualización espera un momento...")
                    /**
                     * implementar esta manera de hacer la espera de milisegundos
                     */
                    /*new Handler().postDelayed(new Runnable() {
                                                public void run () {

                                                }
                                            }, 5000L);*/
                    bluetoothServices.disconnect()
                    Thread.sleep(5000)
                    bluetoothServices.connect(
                        sp!!.getString("nameUpdate", ""),
                        sp!!.getString("macUpdate", "")
                    )
                    Thread.sleep(10000)
                    //bluetoothLeService = getBluetoothLeService();
                    bluetoothLeService!!.sendFirstComando("4021")
                    Thread.sleep(1000)
                    if (bluetoothLeService!!.sendFirstComando("4054")) { //reset de memoria 0x4054
                        Thread.sleep(200)
                        Log.d("", "dataChecksum total:7")
                        return@withContext "ok"
                    } else Log.d("", "dataChecksum total:8")
                    "not"
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    "exception"
                }
            }

            val post = withContext(Dispatchers.Default) {

                if (progressDialog2.returnISshowing()) {
                    progressDialog2.secondStop()
                }
                Log.d("ProgressFirmware", "result $result")
                if (result.equals("ok")) {
                    callback.onProgress("Finalizado")
                    callback.onSuccess(true)
                } else {
                    callback.onSuccess(false)
                    callback.onError(result)
                }


            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun MyAsyncTaskUpdatePlantillaPostFwOperador(callback: MyCallback) {

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {

            val result = withContext(Dispatchers.Default) {
                callback.onProgress("MyAsyncTaskUpdateFirmwareResetMemoryWF")

                return@withContext try {
                    bluetoothServices.disconnect()
                    Thread.sleep(5000)
                    bluetoothServices.connect(
                        sp!!.getString("nameUpdate", ""),
                        sp!!.getString("macUpdate", "")
                    )
                    Thread.sleep(5000)
                    //bluetoothLeService = getBluetoothLeService();
                    bluetoothLeService!!.sendFirstComando("4021")
                    Thread.sleep(1000)
                    //operador
                    //String plantilla = "4050AAFFE9000F0000000000000000FFE200320000001EFF6A000000000005000A0000012C012C000000000078000000B4FF9C000A001400000000FFE9000F00B4FF9C665A28280F04280505000A0F000000000000020600000100020F010000000000002A030B1EF00A0A0000000000000A1400DE490000000000400001070012CC";
                    //tecnico
                    //String s = "4050AA FF E9 00 0F 00 00 00 00 00 00 00 00 FF E2 00 32 00 00 00 1E FF 6A 00 00 00 00 00 05 00 0A 00 00 01 2C 01 2C 00 00 00 00 00 78 00 00 00 B4 FF 9C 00 0A 00 14 00 00 00 00 00 00 00 00 00 00 00 00 66 5A 28 28 0F 04 28 05 05 00 0A 0F 00 00 00 00 00 00 02 06 00 00 01 00 02 0F 01 00 00 00 00 00 00 2A 03 0B 1E F0 0A 0A 00 00 00 00 00 00 0A 14 00 64 49 00 00 00 00 00 40 03 01 07 00 12 CC".replace(" ", "");
                    //newtecnico 22/03/2023
                    val s =
                        "4050AAFFE9000F0000000000000000FFE200320000001EFF6A000000000005000A0000012C012C000000000078000000B4FF9C000A0014000000000000000000000000666332320F04280505000A0F000000000000020600000100020F010000000000002A030B1EF00A0A0000000000000A140064490000000000400301070012CC".replace(
                            " ",
                            ""
                        )
                    val plantilla = java.lang.StringBuilder(s)
                    bluetoothServices.sendCommand(
                        "writeRealParam",
                        "$plantilla ${GetHexFromRealDataImbera.calculateChacksumString(plantilla.toString())}"
                    )
                    listData.clear()
                    Thread.sleep(800)
                    if (bluetoothLeService != null) {
                        listData =
                            bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                        if (listData.isEmpty()) {
                            "empty"
                        } else {
                            if (listData[0] == "F1 3D ") {
                                bluetoothServices.disconnect()
                                Thread.sleep(5000)
                                bluetoothServices.connect(
                                    sp!!.getString("nameUpdate", ""),
                                    sp!!.getString("macUpdate", "")
                                )
                                Thread.sleep(5000)
                                //bluetoothLeService = getBluetoothLeService();
                                bluetoothLeService!!.sendFirstComando("4021")
                                "ok"
                            } else {
                                "notok"
                            }
                        }
                    } else {
                        "noconnected"
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    "exception"
                }
            }

            val post = withContext(Dispatchers.Default) {
                //showInfoPopup("Actualización de firmware","La actualización de Firmware se realizó de manera exitosa");
                //showInfoPopup("Actualización de firmware","La actualización de Firmware se realizó de manera exitosa");
                if (result == "empty")
                    makeToast("Acabas de enviar plantilla, intenta reconectarte a BLE")
                if (result == "ok") {
                    makeToast("Actualización de plantilla: Correcta")
                    //listenermain.printExcel(getListToExcel(),"oxxo");
                }
                if (result == "noconnected") {
                    makeToast("No te has conectados a un BLE")
                }
                if (result == "exception") {
                    makeToast("ha ocurrido un error inesperado")
                }
                if (result == "notok") makeToast("Actualización de plantilla: Incorrecta")

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun MyAsyncTaskUpdateFirmwareResetMemoryWF(callback: MyCallback) {

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {

            val pre = withContext(Dispatchers.Default) {
                callback.onProgress("MyAsyncTaskUpdateFirmwareResetMemoryWF")

                return@withContext try {
                    /**
                     * implementar esta manera de hacer la espera de milisegundos
                     */
                    /*new Handler().postDelayed(new Runnable() {
                                                public void run () {

                                                }
                                            }, 5000L);*/
                    bluetoothServices.disconnect()
                    Thread.sleep(30000)
                    bluetoothServices.connect(
                        sp!!.getString("nameUpdate", ""),
                        sp!!.getString("macUpdate", "")
                    )
                    Thread.sleep(3000)
                    //bluetoothLeService = getBluetoothLeService();
                    bluetoothLeService!!.sendFirstComando("4021")
                    Thread.sleep(1000)
                    if (bluetoothLeService!!.sendFirstComando("4054")) { //reset de memoria 0x4054
                        Thread.sleep(200)
                        Log.d("", "dataChecksum total:7")
                        return@withContext "ok"
                    } else Log.d("", "dataChecksum total:8")
                    "not"
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    "exception"
                }
            }
        }
    }

    fun ObtenerStatus(callback: MyCallback) {

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            val pre = withContext(Dispatchers.Default) {
                callback.onProgress("Iniciando")
                //    progressDialog2.second("Obteniendo estatus...")
                delay(100)
                bluetoothLeService = bluetoothServices.bluetoothLeService()
                delay(500)
                clearLogger()
                bluetoothServices.sendCommand("handshake", "4021")
                delay(1000)

                var lista_HNDS = getInfoList()
                callback.onProgress("Realizando")
                if (!lista_HNDS.isNullOrEmpty()) {
                    Log.d("ProcesoStatus", "lista_HNDS $lista_HNDS")
                    var CleanList = GetRealDataFromHexaImbera.cleanSpace(lista_HNDS as List<String>)
                    Log.d("ProcesoStatus", "CleanList $CleanList")
                    var isChecksumOk = GlobalTools.checkChecksumImberaTREFPB(
                        GetRealDataFromHexaImbera.cleanSpace(lista_HNDS as List<String>).toString()
                    )
                    Log.d("ProcesoStatus", "isChecksumOk $isChecksumOk")

                    delay(100)
                    runOnUiThread {
                        clearLogger()

                    }
                    bluetoothServices!!.sendCommand("realState", "4053")
                    delay(1000)
                    var realState = getInfoList()

                    if (!realState.isNullOrEmpty()) {
                        var FinalListData = GetRealDataFromHexaOxxoDisplay.convert(
                            realState as MutableList<String?>,
                            "Lectura de datos tipo Tiempo real"
                        ) as MutableList<String>

                        Log.d("ProcesoStatus", "FinalListData $FinalListData")
                        FinalListDataRealState = GetRealDataFromHexaOxxoDisplay.GetRealData(
                            FinalListData as List<String>,
                            "Lectura de datos tipo Tiempo real"
                        ) as MutableList<String>
                        FinalList.clear()
                        FinalList.add(realState.get(0))
                        callback.getInfo(realState)
                        FinalList.clear()

                    } else {
                        callback.onSuccess(false)
                        callback.onError("No se pudo obtener la informacion el estatus")
                    }
                } else {
                    callback.onSuccess(false)
                    callback.onError("No se pudo obtener la informacion el estatus")
                }
                callback.onProgress("Finalizando")

                /*             delay(500)
                             Log.d("ProcesoStatus:", ":$realState")
                             isChecksumOk = GlobalTools.checkChecksum(
                                 GetRealDataFromHexaImbera.cleanSpace(realState as List<String>).toString()
                             )
                             Log.d("ProcesoStatus", "realState $realState")
                             var FinalListData = GetRealDataFromHexaOxxoDisplay.convert(
                                 realState as MutableList<String?>,
                                 "Lectura de datos tipo Tiempo real"
                             ) as MutableList<String>

                             Log.d("ProcesoStatus", "FinalListData $FinalListData")
                             FinalListDataRealState = GetRealDataFromHexaOxxoDisplay.GetRealData(
                                 FinalListData as List<String>,
                                 "Lectura de datos tipo Tiempo real"
                             ) as MutableList<String>

                             Log.d("ProcesoStatus", "FinalListDataRealState $FinalListDataRealState")
             */

            }
        }

    }

    inner class ObtenerStatusVersion2(
        callback2: MyCallback
    ) :
        AsyncTask<Int?, Int?, String>() {
        var callback = callback2
        var ListapruebaEvent = mutableListOf<String>()
        var estatusList = mutableListOf<String>()

        protected override fun doInBackground(vararg params: Int?): String? {

            ListapruebaTime.clear()
            ListapruebaEvent!!.clear()

            if (ValidaConexion()) {

                bluetoothServices!!.sendCommand("realState", "4053")
                Thread.sleep(100)
                if (isBLEGattConnected()) {
                    estatusList = getInfoList() as MutableList<String>
                }

            } else {
                callback.onProgress("Finalizado")
                callback.onError("desconectado")
                callback.onSuccess(false)
            }


            return "resp"

        }

        override fun onPostExecute(result: String) {
            if (estatusList.isEmpty()) {
                callback.onSuccess(false)
                callback.onProgress("Finalizando")
            } else {
                callback.getInfo(estatusList)
                callback.onSuccess(true)
                callback.onProgress("Finalizando")
            }


        }

        override fun onPreExecute() {
            runOnUiThread {
                clearLogger()
                callback.onProgress("Iniciando")
            }
        }


    }


    fun divideCadenaFirmware256(cadena: String): MutableList<String> {
        val fragmentos = mutableListOf<String>()
        var indice = 0

        while (indice < cadena.length) {
            val subcadena = cadena.substring(indice, minOf(indice + 256, cadena.length))
            fragmentos.add(subcadena)
            indice += 256
        }
        val firmCommandCut = fragmentos // Initialize your firmCommandCut list here
        //FinalFirmwareCommands = mutableListOf<String>()
        var checksum = 0
        for (p in 0 until firmCommandCut.size) {
            val s = firmCommandCut[p]


            for (h in 0 until s.length step 2) {
                checksum += getDecimal(s.substring(h, h + 2))
            }
            checksumTotal += checksum
            var c = Integer.toHexString(checksum)
            var finalchecksum = ""

            finalchecksum = c.padStart(8, '0')

            FinalFirmwareCommands.add(s + finalchecksum)
        }
        return FinalFirmwareCommands

    }

    fun dividirFirmware102(command: String) {
        //divide el firmware en partes de 128 (16 letras) + 4 bytes de checksum
        //getNewFWVersion(command);
        val stringBuilder = java.lang.StringBuilder()
        stringBuilder.append(GetRealDataFromHexaImbera.getDecimal(command.substring(256, 258)))
        stringBuilder.append(".")
        Log.d("VERSION", ":" + command.substring(2))
        val version = GetRealDataFromHexaImbera.getDecimal(command.substring(258, 260))
        if (version < 10) {
            stringBuilder.append("0$version")
        } else {
            stringBuilder.append(GetRealDataFromHexaImbera.getDecimal(command.substring(258, 260)))
        }
        newFirmwareVersion = stringBuilder.toString()
        newFimwareModelo = java.lang.String.valueOf(
            GetRealDataFromHexaImbera.cleanSpace(
                command.substring(
                    260,
                    262
                ) as List<String>
            )
        )
        var i = 0
        var j = 256
        firmCommandCut!!.clear()
        FinalFirmwareCommands.clear()
        checksumTotal = 0
        do { //dividir en partes
            firmCommandCut!!.add(command.substring(i, j))
            i = j
            j += 256
            if (j == command.length) {
                firmCommandCut!!.add(command.substring(i))
            }
        } while (j < command.length)
        var checksum: Int
        for (p in firmCommandCut!!.indices) {
            val s: String = firmCommandCut!!.get(p)
            checksum = 0
            var h = 0
            while (h < s.length) {
                checksum = checksum + getDecimal(s.substring(h, h + 2))
                h += 2
            }
            checksumTotal = checksumTotal + checksum
            val c = Integer.toHexString(checksum)
            var finalchecksum = ""
            finalchecksum =
                if (c.length == 1) "0000000$c" else if (c.length == 2) "000000$c" else if (c.length == 3) "00000$c" else if (c.length == 4) "0000$c" else if (c.length == 5) "000$c" else if (c.length == 6) "00$c" else if (c.length == 7) "0$c" else c
            FinalFirmwareCommands.add(s + finalchecksum)
        }
    }


    inner class MyAsyncTaskSendNewPlantilla2(
        sb: java.lang.StringBuilder,
        callback: MyCallback
    ) :
        AsyncTask<Int?, Int?, String>() {
        var comando: String
        var mcallback: MyCallback

        init {
            comando = sb.toString()
            mcallback = callback
        }

        override fun doInBackground(vararg params: Int?): String? {
            mcallback.onProgress("Realizando")
            Log.d("DebugHex", "MyAsyncTaskSendNewPlantilla  $comando")
            bluetoothServices!!.sendCommand("writeRealParam", comando)
            dataListPlantilla.clear()
            return try {
                Thread.sleep(800)
                bluetoothLeService = bluetoothServices.bluetoothLeService
                if (bluetoothLeService != null) {
                    dataListPlantilla =
                        bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String>
                    if (dataListPlantilla.isEmpty()) {
                        "empty"
                    } else {
                        Log.d("RESP", ":" + dataListPlantilla.get(0))
                        if (dataListPlantilla.get(0) == "F1 3D ") {
                            "ok"
                        } else {
                            "notok"
                        }
                    }
                } else {
                    "noconnected"
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
                "exception"
            }
        }

        override fun onPostExecute(result: String) {
            try {
                Thread.sleep(300)
                //    progressDialog2.secondStop()
                if (result == "empty") {
                    Toast.makeText(
                        getContext,
                        "Acabas de enviar plantilla, intenta reconectarte a BLE",
                        Toast.LENGTH_SHORT
                    ).show()
                    mcallback.onError("Acabas de enviar plantilla, intenta reconectarte a BLE")
                    mcallback.onSuccess(false)
                }
                if (result == "ok") {
                    Toast.makeText(getContext, "Actualización correcta", Toast.LENGTH_SHORT)
                        .show()
                    mcallback.onError("Actualización correcta")
                    mcallback.onSuccess(true)
                    //   GlobalTools.changeScreenConnectionStatus(tvConnectionState, sp)
                    //     mx.eltec.oxxomonitor.Fragment.MONIFragment.MyAsyncTaskDesconnectBLE().execute()
                    //listenermain.printExcel(getOriginalList(),"imbera");
                }
                if (result == "noconnected") {
                    Toast.makeText(getContext, "No te has conectado", Toast.LENGTH_SHORT).show()
                    mcallback.onError("No te has conectado")
                    mcallback.onSuccess(false)
                }
                if (result == "exception") {
                    Toast.makeText(
                        getContext,
                        "Ha ocurrido un error inesperado",
                        Toast.LENGTH_SHORT
                    ).show()
                    mcallback.onError("Ha ocurrido un error inesperado")
                    mcallback.onSuccess(false)
                }
                if (result == "notok") {
                    Toast.makeText(
                        getContext,
                        "Actualización incorrecta",
                        Toast.LENGTH_SHORT
                    ).show()
                    mcallback.onError("Actualización incorrecta")
                    mcallback.onSuccess(false)
                }
                mcallback.onProgress("Finalizado")
            } catch (e: InterruptedException) {
                e.printStackTrace()
                mcallback.onError("$e")
                mcallback.onProgress("Finalizado")
                mcallback.onSuccess(false)

            }
        }

        override fun onPreExecute() {
            mcallback.onProgress("Iniciando")
            //progressDialog2.second("Actualizando dispositivo...")
            //   createProgressDialog("Actualizando dispositivo...")
        }


    }

    fun MyAsyncTaskSendNewPlantilla(plantillaString: String?, callback: MyCallback) {

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            val pre = withContext(Dispatchers.Default) {
                runOnUiThread {
                    // getInfoList()?.clear()
                    clearLogger()
                    callback.onProgress("Iniciando")
                }

            }
            val doIbackground = withContext(Dispatchers.Default) {


                callback.onProgress("Realizando")
                bluetoothServices.sendCommand("handshake", "4021")
                delay(500)
                Log.d(
                    "plantillaString",
                    plantillaString.toString() + " \n HNS ${bluetoothLeService!!.dataFromBroadcastUpdate}"
                )


                delay(800)
                bluetoothServices.sendCommand("writeRealParam", plantillaString.toString())
                dataListPlantilla.clear()
                return@withContext try {
                    delay(800)
                    if (bluetoothLeService != null) {
                        dataListPlantilla =
                            bluetoothLeService!!.dataFromBroadcastUpdate as MutableList<String>

                        Log.d("plantillaString", "dataListPlantilla $dataListPlantilla")

                        if (dataListPlantilla.isEmpty()) {
                            "empty"
                        } else {
                            if (dataListPlantilla[0] == "F1 3D ") {
                                "ok"
                            } else {
                                "notok"
                            }
                        }
                    } else {
                        "noconnected"
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    "exception"
                }
            }

            val Post = withContext(Dispatchers.Default) {

                when (doIbackground) {
                    "noconnected" -> {
                        callback.onSuccess(false)
                        callback.onError("noconnected")
                    }

                    "notok" -> {
                        callback.onSuccess(false)
                        callback.onError("no se pudo actualizar la plantilla")
                    }

                    "empty" -> {
                        callback.onSuccess(false)
                        callback.onError("empty")
                    }

                    "ok" -> {
                        callback.onSuccess(true)
                    }

                    else -> {

                    }
                }

            }
            callback.onProgress("Finalizando")

        }
    }


    inner class MyAsyncTaskStatusRealTimeVersion2(
        callback: MyCallback
    ) :
        AsyncTask<Int?, Int?, String>() {

        var mcallback: MyCallback

        init {
            mcallback = callback
        }

        override fun doInBackground(vararg params: Int?): String? {
            mcallback.onProgress("Realizando")
            clearLogger()
            if (isBLEGattConnected()) {
                bluetoothServices!!.sendCommand("4021")
                Thread.sleep(800)
            }
            return ""
        }

        override fun onPostExecute(result: String) {
            try {
                Thread.sleep(300)
                //    progressDialog2.secondStop()
                if (isBLEGattConnected()) {
                    bluetoothLeService!!.sendComando("4053")
                }
                Thread.sleep(1000)
                if (isBLEGattConnected()) {
                    mcallback.getInfo(getInfoList())
                }
                mcallback.onSuccess(true)
                mcallback.onProgress("Finalizado")
            } catch (e: InterruptedException) {
                e.printStackTrace()
                mcallback.onProgress("Finalizado")
                mcallback.onSuccess(false)

            }
        }

        override fun onPreExecute() {
            mcallback.onProgress("Iniciando")

        }


    }

    fun MyAsyncTaskStatusRealTime(callback: MyCallback) {

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            val pre = withContext(Dispatchers.Default) {
                runOnUiThread {
                    // getInfoList()?.clear()
                    clearLogger()
                    callback.onProgress("Iniciando")
                }

            }
            val doIbackground = withContext(Dispatchers.Default) {


                callback.onProgress("Realizando")
                bluetoothServices.sendCommand("handshake", "4021")
                delay(500)
                clearLogger()
                delay(800)
                bluetoothServices.sendCommand("4053")
                delay(1000)
                if (getInfoList()!!.isNotEmpty()) {
                    callback.getInfo(getInfoList())
                    callback.onSuccess(true)
                } else {
                    callback.onError("no se pudo obtener la informacion")
                    callback.onSuccess(false)
                }

            }

            callback.onProgress("Finalizando")

        }
    }


    fun GetStatusBle(): Boolean {
        return getStatusConnectBle()
    }


    fun getInfoList(): MutableList<String>? {

        val lista: MutableList<String>? = bluetoothServices.bluetoothLeService?.getLogeer()
        lista?.let {
            it.mapIndexed { index, s ->
                it[index] = s.toUpperCase()
                Log.d("getInfoList", "$index    $it")
            }

        }
        return lista

    }

    @SuppressLint("MissingPermission")
            /*   fun desconectar() {
                   GlobalTools.changeScreenConnectionStatus(tvconnectionState!!, sp!!)
                   bluetoothServices.disconnect()
                   val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                   if (bluetoothAdapter != null) {
                       //     bluetoothAdapter.cancelDiscovery()
                   }
                   if (bluetoothLeService?.mBluetoothGatt != null) {
                       // Desconecta el dispositivo Bluetooth
                       bluetoothLeService?.mBluetoothGatt!!.disconnect()

                       // Realiza otras acciones necesarias después de la desconexión
                       GlobalTools.changeScreenConnectionStatus(tvconnectionState!!, sp!!)

                       // Si deseas liberar completamente la instancia de BluetoothGatt, puedes cerrarla
                       bluetoothLeService?.mBluetoothGatt!!.close()
                       bluetoothLeService?.mBluetoothGatt = null
                   }
               }*/
    fun desconectar() {
        //    GlobalTools.changeScreenConnectionStatus(tvconnectionState, sp!!)
        bluetoothServices.disconnect()
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null) {
            //     bluetoothAdapter.cancelDiscovery()
        }
        if (bluetoothLeService?.mBluetoothGatt != null) {
            // Desconecta el dispositivo Bluetooth
            bluetoothLeService?.mBluetoothGatt!!.disconnect()

            // Cierra la instancia de BluetoothGatt
            bluetoothLeService?.mBluetoothGatt!!.close()
            bluetoothLeService?.mBluetoothGatt = null


            // Realiza otras acciones necesarias después de la desconexión
            //   GlobalTools.changeScreenConnectionStatus(tvconnectionState!!, sp!!)
        }
    }

    fun DesconectarBLE(tvconnectionstate: TextView, sp: SharedPreferences, tvfwversion: TextView) {
        // GlobalTools.changeScreenConnectionStatus(tvconnectionstate, sp)
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            // Operaciones dentro de la corrutina

            val result = withContext(Dispatchers.IO) {
                return@withContext try {
                    Thread.sleep(400)
                    if (bluetoothServices.bluetoothLeService() != null) {
                        bluetoothServices.disconnect()
                        // myconnectListener!!.disconnectBLE()
                        "resp"
                    } else "noconexion"
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    "resp"
                }
            }
            delay(100L)
            Log.d("DesconectarBLE", "$result")
            onPostExecuteDesconnectBLE(result, tvconnectionstate, tvfwversion)
        }
    }


    fun cMyAsyncTaskDesconnectBLE() {

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {

            // createProgressDialog("Desconectando del dispositivo...")
            val result = withContext(Dispatchers.IO) {
                return@withContext try {
                    Thread.sleep(400)
                    if (bluetoothServices.bluetoothLeService() != null) {
                        bluetoothServices.disconnect()
                        "resp"
                    } else "noconexion"
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    "resp"
                }
            }
            delay(100L)
            //    onPostExecuteDesconnectBLE(result)
        }
    }

    private fun onPostExecuteDesconnectBLE(
        result: String,
        tvconnectionstate: TextView,
        tvfwversion: TextView
    ) {

        if (result == "noconexion") {
            runOnUiThread {
                Toast.makeText(getContext, "No estás conectado a ningún BLE", Toast.LENGTH_SHORT)
                    .show()
                esp!!.putBoolean("isconnected", false)
                esp!!.apply()
            }
        } else {
            //Toast.makeText(getContext(), "Te has desconectado de BLE", Toast.LENGTH_SHORT).show();

            runOnUiThread {
                tvfwversion.text = ""
                tvconnectionstate.text = "Desconectado"
                tvconnectionstate.setTextColor(Color.BLACK)
                esp!!.putString("trefpVersionName", "")
                esp!!.apply()
            }
            esp!!.putBoolean("isconnected", false)
            esp!!.apply()

        }
    }

    private fun removeZeros8(list: MutableList<String>):
            List<String> {
        return list.filter {
            val chunks = it.chunked(8)
            chunks.none { chunk ->
                chunk.all { ch -> ch == '0' }
            }
        }
    }

    fun sumarParesHexadecimales(cadena: MutableList<String>): String {
        val numerosHex =
            cadena.flatMap { it.chunked(2) }.map { it.padStart(2, '0') }.map { it.toInt(16) }
        val sumaHex = numerosHex.sum()
        return sumaHex.toString(16).padStart(8, '0').uppercase()
    }

    fun sumarParesHexadecimales(Dato: String): String {
        var cadena = mutableListOf<String>()
        var calculateCheckSume = Dato//.substring(0,Dato.length-8)
        cadena.add(calculateCheckSume)
        val numerosHex =
            cadena.flatMap { it.chunked(2) }.map { it.padStart(2, '0') }.map { it.toInt(16) }
        val sumaHex = numerosHex.sum()
        return sumaHex.toString(16).padStart(8, '0').uppercase()
    }

    interface MyCallbackBD {
        fun onSuccess(result: Boolean): Boolean
        fun onError(error: String)
        fun getInfo(data: MutableList<Pair<String, String>>?)
        fun onProgress(progress: String): String
    }

    interface MyCallback {
        fun onSuccess(result: Boolean): Boolean
        fun onError(error: String)
        fun getInfo(data: MutableList<String>?)
        fun onProgress(progress: String): String
    }

    interface CallbackLogger {
        fun onSuccess(result: Boolean): Boolean
        fun onError(error: String)
        fun getTime(data: MutableList<String>?)
        fun getEvent(data: MutableList<String>?)
        fun onProgress(progress: String): String
    }

    interface CallbackLoggerVersionCrudo {
        fun onSuccess(result: Boolean): Boolean
        fun onError(error: String)
        fun getTime(data: MutableList<String>?)
        fun getEvent(data: MutableList<String>?)
        fun getTimeCrudo(data: MutableList<String>?)
        fun getEventCrudo(data: MutableList<String>?)
        fun onProgress(progress: String): String
    }


    private fun validateList(list: MutableList<String>?): Boolean {
        if (list != null) {
            return !list.all {
                it.equals(0)

            }
        } else return false
    }

    private fun validaCeros(
        arrayLists: MutableList<String?>,
        action: String?,
        fwversion: String,
        modelo: String
    ): MutableList<String?> {
        var arrayListInfo: MutableList<String?> = ArrayList()
        val s = cleanSpace(arrayLists as MutableList<String?>)
        val datos: MutableList<String> = ArrayList()
        val datos2: MutableList<String> = ArrayList()
        //header
        Log.d("", "sss}Evento:" + s.length)
        arrayListInfo.add(s.substring(0, 4)) //head
        arrayListInfo.add(s.substring(4, 12)) //
        arrayListInfo.add(s.substring(12, 14)) //modelo trefpb
        arrayListInfo.add(s.substring(14, 16)) //version
        val st = StringBuilder()
        st.append(s.substring(16, s.length - 8))
        /*  Log.d("LPOPOP", "crudotiempoTEvento:$fwversion")
          Log.d("LPOPOP", "crudotiempoTEvento:$modelo")*/
        if (fwversion == "1.02" && modelo == "3.3" || fwversion == "1.04" && modelo == "3.5") { //nuevo logger, diferente división de información
            var i = 0
            do { //dividir toda la información en paquetes de 128 bytes
                if (i + 256 > st.length) {
                    datos.add(st.substring(i)) //checksum
                    break
                } else datos.add(st.substring(i, i + 256))
                i = i + 256
            } while (i < st.length)
            Log.d("DataEvento", "DataEvento:$datos")
            var j = 0
            do { //dividir los paquetes de 128 bytes según el protocolo
                i = 0
                while (i < datos[j].length) {
                    if (i + 28 > datos[0].length) {
                        datos2.add(datos[j].substring(i)) //checksum
                        break
                    } else datos2.add(datos[j].substring(i, i + 28))
                    i += 28
                }
                j++
            } while (j < datos.size)
            Log.d("crudotiempoDAots2", "crudotiempoDAots2:$datos2")
            Log.d("ultimondatos2", "ultimondatos2:" + datos2[datos2.size - 1])

            //organizar la información que realmente sirve (quitar 0s)
            var h = 4
            //String numeroRegistrosNuevos = datos2.get(datos.size()-2);
            var o = 0
            while (o < datos2.size) {
                if (datos2[o].length != 4 && datos2[o] != "0000000000000000000000000000") {
                    arrayListInfo.add(datos2[o])
                    Log.d(
                        "",
                        "crudoEventoFOR:" + arrayListInfo[h]
                    )
                    h++
                }
                o++
            }
        } else {
            var i = 16
            while (i < s.length) {
                if (i + 28 > s.length) {
                    //arrayListInfo.add(s.substring(i));//checksum
                    break
                } else arrayListInfo.add(
                    s.substring(
                        i,
                        i + 28
                    )
                )
                i += 28
            }
            Log.d("", "crudoEvento:" + arrayListInfo)
        }
        return arrayListInfo
    }

    private fun removeZeros(list: MutableList<String>?): List<String> {
        // return list!!.filter { !it.matches(Regex("^0+$")) }
        //   return list!!.filter { !it.matches(Regex("^0{1,10000}$")) }
        /*     return list!!.filter {
                 var sum = 0
                 it.forEach { ch ->
                     sum += Integer.parseInt(ch.toString(), 16)
                 }
                 sum != 0
             }
     */
        return list!!.filter {
            var sum = 0
            it.forEach { ch ->
                sum += Integer.parseInt(ch.toString(), 16)
            }
            sum != 0
        }/*.filter {
            val chunks = it.chunked(8)
            chunks.none { chunk ->
                chunk.all { ch -> ch == '0' }
            }
        }
*/
    }

    private fun VTIME(arrayLists: MutableList<String?>): MutableList<String?> {
        var arrayListInfo: MutableList<String?> = ArrayList()
        var arrayListInfoFG: MutableList<String?> = ArrayList()
        if (!arrayLists.isEmpty()) {
            val s = cleanSpace(arrayLists)
            val datos: MutableList<String> = ArrayList()
            val datos2: MutableList<String> = ArrayList()
            //header
            Log.d("", "sssTiempo:$s")
            arrayListInfo.add(s.substring(0, 4)) //head
            arrayListInfo.add(s.substring(4, 12)) //
            arrayListInfo.add(s.substring(12, 14)) //modelo trefpb
            arrayListInfo.add(s.substring(14, 16)) //version
            Log.d("datosCrudosTiempoSALidaFinal","------------> arrayListInfo $arrayListInfo")
            val st = StringBuilder()
            st.append(s.substring(16, s.length - 8))
            /*  Log.d("LPOPOP", "crudotiempoTIEMPO:$fwversion")
              Log.d("LPOPOP", "crudotiempoTIEMPO:$modelo")

             */

            //if (sp?.getString("numversion", "")  /*fwversion*/ == "1.02" &&  sp?.getString("modelo", "")/*modelo */== "3.3" || sp?.getString("numversion", "") == "1.04" && sp?.getString("modelo", "") == "3.5") {
            if (/*sp?.getString("numversion", "") == "1.02" && sp?.getString("modelo", "") == "3.3"
                || sp?.getString("numversion", "") == "1.04" && sp?.getString("modelo", "") == "3.5"
                || sp?.getString("numversion", "") == "1.01" && sp?.getString("modelo", "") == "8.1"*/true
            ) { //nuevo logger, diferente división de información
                var i = 0
                Log.d("datosCrudosTiempoSALidaFinal","st $st")
                do { //dividir toda la información en paquetes de 128 bytes
                    Log.d("datosCrudosTiempoSALidaFinal","i + 256 ${i + 256 } i $i ")
                    if (i + 256 > st.length) {
                        datos.add(st.substring(i)) //checksum
                        break
                    } else datos.add(st.substring(i, i + 256))
                    i += 256
                } while (i < st.length)
                var j = 0
                do { //dividir los paquetes de 128 bytes según el protocolo
                    i = 0
                    while (i < datos[j].length) {
                        if (i + 18 > datos[j].length) {
                            datos2.add(datos[j].substring(i)) //checksum
                            break
                        } else datos2.add(datos[j].substring(i, i + 18))
                        i += 18
                    }
                    j++
                } while (j < datos.size)
                Log.d("crudotiempoTIEMPO", "crudotiempoTIEMPO:$datos2")
                //Log.d("","ultimonTIEMPO:"+datos2.get(datos2.size()-2));

                //organizar la información que realmente sirve (quitar 0s)
                var h = 4
                //String numeroRegistrosNuevos = datos2.get(datos.size()-2);
                var o = 0
                while (o < datos2.size) {
                    if (datos2[o].length != 4 && datos2[o] != "000000000000000000") {
                        arrayListInfo.add(datos2[o])
                        Log.d(
                            "crudotiempoTIEMPO",
                            "crudotiempoFOR:" + arrayListInfo[h]
                        )
                        h++
                    }
                    o++
                }
                Log.d("crudotiempoTIEMPO", "crudotiempo:" + arrayListInfo)
            } else {
                var i = 16
                do {
                    if (i + 18 > s.length) {
                        //arrayListInfo.add(s.substring(i));//checksum
                        break
                    } else arrayListInfo.add(
                        s.substring(
                            i,
                            i + 18
                        )
                    )
                    i = i + 18
                } while (i < s.length)
                Log.d("crudotiempoTIEMPO", "crudotiempo:" + arrayListInfo)
            }
            //data
        }
        return arrayListInfo
    }

    private fun VEvent(arrayLists: MutableList<String?>): MutableList<String?> {
        var arrayListInfo: MutableList<String?> = ArrayList()
        var arrayListInfoFG: MutableList<String?> = ArrayList()
        val s = cleanSpace(arrayLists)
        if (!arrayLists.isEmpty() && s.toString().length >= 16) {

            val datos: MutableList<String> = ArrayList()
            val datos2: MutableList<String> = ArrayList()
            //header
            Log.d("", "sss}Evento:" + s.length)
            arrayListInfo.add(s.substring(0, 4)) //head
            arrayListInfo.add(s.substring(4, 12)) //
            arrayListInfo.add(s.substring(12, 14)) //modelo trefpb
            arrayListInfo.add(s.substring(14, 16)) //version
            val st = StringBuilder()
            st.append(s.substring(16, s.length - 8))
            // Log.d("LPOPOP", "crudotiempoTEvento:$fwversion")
            //   Log.d("LPOPOP", "crudotiempoTEvento:$modelo")
            if (/*sp?.getString("numversion", "")  /*fwversion*/ == "1.02" && sp?.getString(
                    "modelo",
                    ""
                )/*modelo */ == "3.3" || sp?.getString("numversion", "") == "1.04" && sp?.getString(
                    "modelo",
                    ""
                ) == "3.5"
                || sp?.getString("numversion", "") == "1.01" && sp?.getString("modelo", "") == "8.1"*/true
            ) { //nuevo logger, diferente división de información
                var i = 0
                do { //dividir toda la información en paquetes de 128 bytes
                    if (i + 256 > st.length) {
                        datos.add(st.substring(i)) //checksum
                        break
                    } else datos.add(st.substring(i, i + 256))
                    i = i + 256
                } while (i < st.length)
                Log.d("", "DataEvento:$datos")
                var j = 0
                do { //dividir los paquetes de 128 bytes según el protocolo
                    i = 0
                    while (i < datos[j].length) {
                        if (i + 28 > datos[j].length) {
                            datos2.add(datos[j].substring(i)) //checksum
                            break
                        } else datos2.add(datos[j].substring(i, i + 28))
                        i += 28
                    }
                    j++
                } while (j < datos.size)
                Log.d("", "crudotiempoDAots2:$datos2")
                Log.d("", "ultimondatos2:" + datos2[datos2.size - 1])

                //organizar la información que realmente sirve (quitar 0s)
                var h = 4
                //String numeroRegistrosNuevos = datos2.get(datos.size()-2);
                var o = 0
                while (o < datos2.size) {
                    if (datos2[o].length != 4 && datos2[o] != "0000000000000000000000000000") {
                        arrayListInfo.add(datos2[o])
                        Log.d(
                            "crudoEventoFOR",
                            "crudoEventoFOR:" + arrayListInfo[h]
                        )
                        h++
                    }
                    o++
                }
            } else {
                var i = 16
                while (i < s.length) {
                    if (i + 28 > s.length) {
                        //arrayListInfo.add(s.substring(i));//checksum
                        break
                    } else arrayListInfo.add(
                        s.substring(
                            i,
                            i + 28
                        )
                    )
                    i += 28
                }
                Log.d("", "crudoEvento:" + arrayListInfo)

            }
        }
        return arrayListInfo
    }

    inner class OBtenerComando(
        callback2: ConexionTrefp.CallbackLogger
    ) :
        AsyncTask<Int?, Int?, String>() {
        var callback = callback2
        var ListapruebaEvent = mutableListOf<String>()
        var Event = mutableListOf<String?>()
        var registrosEvent = mutableListOf<String?>()
        protected override fun doInBackground(vararg params: Int?): String? {


            if (ValidaConexion()) {

                bluetoothLeService!!.sendComando("4021")
                Thread.sleep(800)
                Log.d("ObtenerLoggerPrueba", "getinfolist ${getInfoList()}")
                clearLogger()

                bluetoothServices.sendCommand("4021")
                Thread.sleep(800)
                clearLogger()
                bluetoothServices.sendCommand("event", "4061")
                Thread.sleep(40000)
                ListapruebaEvent = getInfoList() as MutableList<String>
                if (!ListapruebaEvent.isNullOrEmpty()) {
                    ListapruebaEvent.map {
                        Log.d("datosdeEvento", " Event ${it}")
                    }
                    //LA FUNCION VEvent SEPARA LOS DATOS EN EL HEADER Y LOS DATOS EN BRUTO
                    Event = VEvent(ListapruebaEvent as MutableList<String?>)
                    Thread.sleep(100)
                    Event.map {
                        Log.d("ObtenerLoggerPrueba", "Traducido Event ${it}")
                    }
                } else {
                    Log.d("ObtenerLoggerPrueba", "lista Event vacia ")
                }


                if (!Event.isNullOrEmpty()) {
                    for (item in Event) {
                        if (item!!.length > 15) {
                            registrosEvent.add(item)
                        }
                    }
                }
                Log.d(
                    "ObtenerLoggerPrueba",
                    "\n ----------------------------------------------------------"
                )
                registrosEvent.map {
                    Log.d("ObtenerLoggerPrueba", it.toString())
                }


            } else {
                callback.onProgress("Finalizado Tiempo")
                callback.onProgress("Finalizado Evento")
                callback.onError("desconectado")
                callback.onSuccess(false)
            }


            return "resp"

        }

        override fun onPostExecute(result: String) {

            callback.onSuccess(true)


        }

        override fun onPreExecute() {
            runOnUiThread {
                // getInfoList()?.clear()

                clearLogger()
                callback.onProgress("Iniciando Logger")
                callback.onProgress("Iniciando Tiempo")

            }
        }


    }

    fun ValidaConexion(): Boolean {
        bluetoothLeService = bluetoothServices.bluetoothLeService
        bluetoothLeService?.let { service ->
            val gatt = service.mBluetoothGatt
            val device = gatt?.device
            if (device != null) {
                //  Log.d("ValidaConexion", "gatt ${gatt}  $device")
                return true
            } else return false
        } ?: return false

    }


    interface LocationResultListener {
        fun onLocationResult(latitude: Double, longitude: Double)
        fun onLocationError(error: String)
    }

    interface ListenerTREFP {
        fun onError(error: String?)
        fun isConnectionAlive(resp: String?)
        fun getInfo(data: MutableList<String?>)
        fun getInfoBytes(data: ByteArray?)
    }


    //////////////////////////---------------------------->P R U E B A S<------------------------------------------------------------------/////////////////////////

    fun NewCHECKSUM(data: String):
            String {
        val checksumData: String = data.substring(data.length - 8)
        var checksumTotal = 0
        var c = ""
        {
            var p = 0
            while (p < data.length - 9) {
                checksumTotal = checksumTotal + GetRealDataFromHexaOxxoDisplay.getDecimal(
                    data.substring(
                        p,
                        p + 2
                    )
                )
                p += 2
            }
        }
        c = Integer.toHexString(checksumTotal)
        Log.d("isChecksumOk", c.toString())
        var finalchecksum = ""
        finalchecksum =
            when (c.length) {
                1 -> "0000000$c"
                2 -> "000000$c"
                3 -> "00000$c"
                4 -> "0000$c"
                5 -> "000$c"
                6 -> "00$c"
                7 -> "0$c"
                else -> c
            }
        return finalchecksum.uppercase()
    }


    @SuppressLint("SuspiciousIndentation")
    private fun LOGGEREVENTZ(result: MutableList<String?>): MutableList<String> {
        listaTemporalFINALEVENTO.clear()



        result.map {
            val hexString = it!!.substring(0, 8) // valor hexadecimal que se desea convertir
            val hexString2 = it!!.substring(8, 16)
            Log.d(
                "DatosCRUDOSEVENTZ",
                "$it   ${convertirHexAFecha(hexString)}   ${convertirHexAFecha(hexString2)}"
            )
            //  println("datosResult  $it   ${convertirHexAFecha(hexString)}   ${convertirHexAFecha(hexString2)} ")
        }
        val currentTimeMillis = System.currentTimeMillis()
        val diferenciaTOP =
            result.last()!!.substring(8, 16).toLong(16) * 1000 - (result.last()!!.substring(0, 8)
                .toLong(16) * 1000)

        val currentTimeHexNOW = toHexString(currentTimeMillis / 1000)
        val MILLNOWMINUSDIF = currentTimeMillis - diferenciaTOP
        /* val MILLNOWMINUSDIF = if(diferenciaTOP >= 0) {
             currentTimeMillis - diferenciaTOP
         } else {
             currentTimeMillis + Math.abs(diferenciaTOP)
         }
         */

        val currentTimeHexMILLNOWMINUSDIF = toHexString(MILLNOWMINUSDIF / 1000)


        val d =
            (currentTimeHexMILLNOWMINUSDIF + currentTimeHexNOW + result.last().toString().substring(
                16,
                result.last()!!.length
            )).uppercase()
        listaTemporalFINALEVENTO.add(d)
        println(
            "ultimo registro $currentTimeMillis ${currentTimeHexNOW.uppercase()} ${
                convertirHexAFecha(
                    currentTimeHexNOW
                )
            }  ${convertirHexAFecha(currentTimeHexMILLNOWMINUSDIF)}"
        )
        val hexString = listaTemporalFINALEVENTO.last()
            .substring(0, 8) // valor hexadecimal que se desea convertir
        val hexString2 = listaTemporalFINALEVENTO.last().substring(8, 16)
        println(
            "ultimo registro $listaTemporalFINALEVENTO} $hexString ${
                convertirHexAFecha(
                    hexString
                )
            }  $hexString2 ${convertirHexAFecha(hexString2)} "
        )

        if (result.size > 1) {
            for (i in result.size - 2 downTo 1) {
                val diferenciaTOP =
                    result[i + 1]!!.substring(8, 16).toLong(16) * 1000 - (result[i]!!.substring(
                        8,
                        16
                    ).toLong(16) * 1000)
                val diferencia =
                    result[i]!!.substring(8, 16).toLong(16) * 1000 - (result[i]!!.substring(0, 8)
                        .toLong(16) * 1000)

                val diferenciaTOPMINUSlast = ((listaTemporalFINALEVENTO.last()!!.substring(8, 16)
                    .toLong(16) * 1000) - diferenciaTOP)
                println(
                    "TOP->>>>>>>>>>>>>  ${diferenciaTOP}  ${listaTemporalFINALEVENTO.last()}  ${
                        toHexString(
                            diferenciaTOPMINUSlast / 1000
                        ).uppercase()
                    } "
                )

                val currentTimeHex =
                    toHexString(diferenciaTOPMINUSlast / 1000).uppercase()
                val LINEARDIFMILL = diferenciaTOPMINUSlast - diferencia
                val currentTimeHexLINEAL =
                    toHexString(LINEARDIFMILL / 1000).uppercase()
                val d = (currentTimeHexLINEAL + currentTimeHex + result[i]!!.substring(
                    16,
                    result.last()!!.length
                )).uppercase()
                listaTemporalFINALEVENTO.add(d)

                println(
                    "datosResult->>>>>>>>>>>>>  ${result[i]}  ${result[i - 1]}  diferenciaTOPMINUSlast $currentTimeHex ${
                        convertirHexAFecha(
                            currentTimeHex
                        )
                    }" +
                            "  $currentTimeHexLINEAL ${convertirHexAFecha(currentTimeHexLINEAL)}   $i ${i - 1}"
                )
            }
            val diferenciaTOP2 =
                result.first()!!.substring(8, 16).toLong(16) * 1000 - (result[1]!!.substring(8, 16)
                    .toLong(16) * 1000)
            val diferencia = result.first()!!.substring(8, 16).toLong(16) * 1000 - (result.first()!!
                .substring(0, 8).toLong(16) * 1000)

            val diferenciaTOPMINUSlast = (listaTemporalFINALEVENTO.last()!!.substring(8, 16)
                .toLong(16) * 1000) - diferenciaTOP
            val currentTimeHex =
                toHexString(diferenciaTOPMINUSlast / 1000).uppercase()
            val LINEARDIFMILL = diferenciaTOPMINUSlast - diferencia
            val currentTimeHexLINEAL = toHexString(LINEARDIFMILL / 1000).uppercase()
            val dd = (currentTimeHexLINEAL + currentTimeHex + result[0]!!.substring(
                16,
                result.last()!!.length
            )).uppercase()
            listaTemporalFINALEVENTO.add(dd)
        }

        /*
                listaTemporalEVENTFINAL.clear()
                val diferencia= result.last()!!.substring(8, 16).toLong(16) * 1000 - result.last()!!.substring(0, 8).toLong(16) * 1000    //(result[1]!!.substring(0, 8).toLong(16) * 1000) -  (result[0]!!.substring(0, 8).toLong(16) * 1000)
                val unixTime = (System.currentTimeMillis() / 1000) - diferencia

                val currentTimestampSeconds = System.currentTimeMillis() / 1000
                val currentHexTimestamp = java.lang.Long.toHexString(currentTimestampSeconds)

                val resultHexTimestamp = java.lang.Long.toHexString(currentTimestampSeconds / 1000)
                val anterior2 =  (currentHexTimestamp.toLong(16) * 1000) - diferencia
                val date2 = Date(anterior2)
                val fechaHoraExadecimal2 =
                    BigInteger.valueOf(date2.time / 1000).toString(16).padStart(8, '0').uppercase()
                //val primerDato = currentHexTimestamp+fechaHoraExadecimal2+result.first().toString().substring(16,result.first().toString().length).uppercase()
                val primerDato = fechaHoraExadecimal2+currentHexTimestamp+result.last().toString().substring(16,result.last().toString().length).uppercase()

                listaTemporalEVENTFINAL.add(primerDato)

                Log.d("SALIDASTNIGB","$primerDato" )
                */
        //  println("lista temporal  $listaTemporalEVENTFINAL  ${listaTemporalEVENTFINAL.first().toString().length} ${result.first().toString().length}   ")
        /*  for (i in result.size - 2 downTo 1) {

              val diferenciaTOP= result[i]!!.substring(8, 16).toLong(16) * 1000 -result[i-1]!!.substring(8, 16).toLong(16) * 1000    //(result[1]!!.substring(0, 8).toLong(16) * 1000) -  (result[0]!!.substring(0, 8).toLong(16) * 1000)

              val diferencia= result[i]!!.substring(8, 16).toLong(16) * 1000 -result[i]!!.substring(0, 8).toLong(16) * 1000    //(result[1]!!.substring(0, 8).toLong(16) * 1000) -  (result[0]!!.substring(0, 8).toLong(16) * 1000)
              val unixTime = (System.currentTimeMillis() / 1000) - diferencia

              val currentTimestampSeconds =  (listaTemporalEVENTFINAL.last().substring(/*8,16*/0,8).toLong(16) *1000) /1000   // System.currentTimeMillis() / 1000
              val currentHexTimestamp = java.lang.Long.toHexString(currentTimestampSeconds)

              val resultHexTimestamp = java.lang.Long.toHexString(currentTimestampSeconds / 1000)
              val anterior2 =  (currentHexTimestamp.toLong(16) * 1000) - diferencia
              val date2 = Date(anterior2)
              val fechaHoraExadecimal2 =
                  BigInteger.valueOf(date2.time / 1000).toString(16).padStart(8, '0').uppercase()
              //val primerDato = currentHexTimestamp+fechaHoraExadecimal2+result[i].toString().substring(16,result[i].toString().length).uppercase()

              val primerDato = fechaHoraExadecimal2+currentHexTimestamp+ result[i].toString().substring(16,result[i].toString().length).uppercase()
              listaTemporalEVENTFINAL.add(primerDato)

          }
  */
        return listaTemporalFINALEVENTO
    }


    private fun LOGGERTIME(registros: MutableList<String>?): MutableList<String> {
        listaTemporalFINAL.clear()
        /* val data = listOf(
             "612F6B4700C3FE3480",
             "612F6C7300C3FE3478",
             "612F6D9F00C3FE3480",
             "612F6ECB00C2FE347E",
             "612F6FF700C2FE347F",
             "612F6B4700C1FE347F",
             "612F6B4700C3FE3480",
             "612F6C7300C3FE3478",
             "612F6D9F00C3FE3480",
             "612F6ECB00C2FE347E",
             "612F6FF700C2FE347F",
             "612F6B4700C3FE3480",
             "612F6C7300C3FE3478",
             "612F6D9F00C3FE3480",


             )*/

        println(registros!!.size)
        Log.d("DATOSEXAFECHA", "${registros}")
        val result = dataFECHA2(
            registros as MutableList<String?>,
            "Lectura de datos tipo Tiempo"
        )/// as MutableList<List<String>?> , "PRUEBA")
        println(result)
        println(result.size)
        result.map {
            Log.d("DiferenciaResult--------------", "${it}")
            Log.d("DATOSEXAFECHA", "${convertHexToHumanDate(it!!.substring(0, 8))}")
        }


        var listaTemporal = mutableListOf<String>()
        var resultado = mutableListOf<List<String>>()


        //   result.last()?.let { listaTemporalFINAL.add(it!!) }
        for (i in result.size - 1 downTo 1) {

            val fechaHora1 = Date(result[i]!!.substring(0, 8).toLong(16) * 1000)
            val fechaHora2 = Date(result[i - 1]!!.substring(0, 8).toLong(16) * 1000)

            println(" ${result[i]} $i  fechaHora1 $fechaHora1 fechaHora2 $fechaHora2 ")
            if (fechaHora1 >= fechaHora2) {
                listaTemporalFINAL.add(result[i]!!)
            } else {
                listaTemporalFINAL.add(result[i]!!)
                val timestamp = (result[i]!!.substring(0, 8).toLong(16) * 1000) - 360000
                val date = Date(timestamp)

                /********/
                val fechaHoraExadecimal =
                    BigInteger.valueOf(date.time / 1000).toString(16).padStart(8, '0').uppercase()

                listaTemporalFINAL.add(
                    fechaHoraExadecimal + result[i - 1]!!.substring(
                        8,
                        result[i - 1]!!.length
                    )
                )


                /***************/

                println(
                    " ->>>>>>>>>>>>>>>>>  ${result[i]!!} ${i} ${timestamp}  $date    ${
                        timestamp.toString(
                            16
                        ).uppercase()
                    } --  ${fechaHoraExadecimal} "
                )



                IteraFUNRegistros(i - 1, result)

                break
            }

        }


        val w2 = (result[1]!!.substring(0, 8).toLong(16) * 1000) - (result[0]!!.substring(0, 8)
            .toLong(16) * 1000)
        val anterior2 = (listaTemporalFINAL.last().substring(0, 8).toLong(16) * 1000) - w2
        val date2 = Date(anterior2)
        val fechaHoraExadecimal2 =
            BigInteger.valueOf(date2.time / 1000).toString(16).padStart(8, '0').uppercase()
        listaTemporalFINAL.add(fechaHoraExadecimal2 + result[0]!!.substring(8, result[0]!!.length))

        Log.d("AJUSTETIEMPO--------------", "")
//        Log.d("AJUSTETIEMPO--------------","${listaTemporalFINAL.last()} ${listaTemporalFINAL[listaTemporalFINAL.size]}  ${listaTemporalFINAL[listaTemporalFINAL.size-1]} ")
        listaTemporalFINAL.forEachIndexed { index, s ->
            Log.d("---------SALIDA", "$index $s    ${convertHexToHumanDate(s.substring(0, 8))}")
        }

        return listaTemporalFINAL


    }








    fun getPlantillacommand(callback: MyCallback) {
        runOnUiThread {
            //   bluetoothServices.bluetoothLeService!!.CleanListLogger()
        }
        val job = CoroutineScope(Dispatchers.IO).launch {
            var data : MutableList<String>? = mutableListOf<String>()
            val corrutina1 = launch {
//                callback.onProgress("Iniciando")
                data = fetchData(callback)


            }
            corrutina1.join()
            val corrutina2 = launch {
                processData(data, callback)
            }
            corrutina2.join()
            val corrutina3 = launch {}
            corrutina3.join()



        }


        /*
              val job = CoroutineScope(Dispatchers.IO).launch {
                  val data = fetchData(callback)
      sdsds
                  // Una vez que se obtienen los datos, se pueden usar o pasar a otra función
                  processData(data, callback)
              }
      */
        // Esperar a que la corrutina termine sin bloquear el hilo principal
        runBlocking {
            job.join()
        }
        /*
                scope.launch {
                    var pre = withContext(Dispatchers.IO) {
                        runOnUiThread {


                            callback.onProgress("Iniciando")

                            bluetoothServices.sendCommand("4051")
                        }
                        delay(1000)

                        var listData: List<String?>? = java.util.ArrayList()
                        var FinalListData: List<String?> = java.util.ArrayList()
                        runOnUiThread {
                            callback.onProgress("Realizando")
                        }


                        if (getInfoList().isNullOrEmpty()){
                            callback.onError("no se pudo obtener la plantilla")

                            callback.onSuccess(false)
                            callback.onProgress("Finalizando")
                        }
                        else{
                            var nuevaPlantillaEnviar: java.lang.StringBuilder? = null
                            nuevaPlantillaEnviar = java.lang.StringBuilder()
                            listData?.map {
                                nuevaPlantillaEnviar!!.append(it)
                            }
                            val plantilla =
                                nuevaPlantillaEnviar // sp.getString("ValoresPlantillasApp", "").toString()
                                    .replace(
                                        "\\s".toRegex(),
                                        ""
                                    )

                            var lis: MutableList<String>? = ArrayList()
                            lis?.add(plantilla)

                            Log.d("DatosfromHomeFragment", " getInfoList() ${getInfoList()}")
                            callback.getInfo(getInfoList())
                            callback.onSuccess(true)
                            callback.onProgress("Finalizando")
                        }

                    }


                }
                */
    }

    fun processData(data: MutableList<String>?, callback: MyCallback) {
        if (data.isNullOrEmpty()) {
            callback.onError("no se pudo obtener la plantilla")

            callback.onSuccess(false)
            callback.onProgress("Finalizando")
        } else {
            callback.getInfo(data)
            callback.onSuccess(true)
            callback.onProgress("Finalizando")
        }
    }

    suspend fun fetchData(callback: MyCallback): MutableList<String>? {

        callback.onProgress("Iniciando")
        bluetoothServices.bluetoothLeService!!.CleanListLogger()
        bluetoothServices.bluetoothLeService!!.sendFirstComando("4051")
        println("Antes del delay ")
        delay(800)
        println("Despues del delay ")
        callback.onProgress("Realizando")
        return getInfoList()
    }

    fun ObtenerPlantillafromAA(input: String): String? {
        // Buscar la primera ocurrencia de "AA"
        val indiceAA = input.indexOf("AA")

        // Si no se encuentra "AA", retornar null
        if (indiceAA == -1) {
            return null
        }

        // Obtener el substring desde el primer "AA" hasta el final
        val resultado = input.substring(indiceAA)

        // Retornar el resultado
        return resultado
    }

    inner class MyAsyncTaskGeEventVALIDACION(private val callback: MyCallback) :
        AsyncTask<Int?, Int?, MutableList<String>>() {
        var FINALLISTA: MutableList<String?> = ArrayList()
        override fun doInBackground(vararg params: Int?): MutableList<String>? {
            var listDataGT: MutableList<String>? = ArrayList()
            callback.onProgress("Realizando Evento")
            if (ValidaConexion()) {
                bluetoothLeService = bluetoothServices.bluetoothLeService()
                Thread.sleep(500)
                bluetoothServices.sendCommand("handshake", "4021")
                Thread.sleep(500)

            }
            return listDataGT//getInfoList()

        }

        override fun onPostExecute(result: MutableList<String>?) {
            try {


                if (ValidaConexion()) {
                    try {
                        FinalListDataFinalE.clear()
                        Log.d("getInfoListFinal", "${getInfoList().toString()}")
                        bluetoothLeService = bluetoothServices.bluetoothLeService()
                        Thread.sleep(500)
                        bluetoothServices.sendCommand("handshake", "4021")
                        Thread.sleep(500)
                        getInfoList()?.clear()
                        bluetoothServices.sendCommand("event", "4061")
                        Thread.sleep(700)
                        do {
                            //  listDataGT!!.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                            Log.d(
                                "islistttt2",
                                ": ${bluetoothLeService?.dataFromBroadcastUpdateString}  "
                            )
                            Thread.sleep(700)

                        } while (bluetoothLeService?.dataFromBroadcastUpdateString?.isNotEmpty() == true)

                        Thread.sleep(700)
                        FINALLISTA = VEvent(
                            getInfoList() as MutableList<String?>
                        )


                        var TIMEUNIX: String? = null
                        var listTIMEUNIX: MutableList<String>? = ArrayList()
                        val SALIDAEVENTOREGISTROS: MutableList<String?> = ArrayList()
                        for (item in FINALLISTA) {
                            if (item != null) {
                                if (item.length > 18) {
                                    SALIDAEVENTOREGISTROS.add(item)
                                    Log.d("FINALLISTA", "metodoquerecorre FINALLISTA ${FINALLISTA}")
                                }
                            }
                        }
                        Log.d(
                            "funcionUltime",
                            "SALIDAEVENTOREGISTROS size ${SALIDAEVENTOREGISTROS}"
                        )
                        if (SALIDAEVENTOREGISTROS.isNotEmpty()) {
                            Log.d("funcionUltime", "SALIDAEVENTOREGISTROS.isNotEmpty()")
                            /*******************************CONTEO DE EVENTO 04 ***********************************/
                            val indexCount: MutableList<Int> = ArrayList()
                            var count = 0
                            for ((index, item) in SALIDAEVENTOREGISTROS.withIndex()) {
                                val EVENT_TYPE = item?.substring(16, 18)
                                if (EVENT_TYPE == "04") {
                                    count++
                                    println("-> $item  $count index $index  ")
                                    indexCount.add(index)
                                }
                            }


                            Log.d("funcionUltime", "count size ${count}")
                            when (count) {
                                0 -> {
                                    SALIDAEVENTOREGISTROS?.let {
                                        callback.getInfo(it as MutableList<String>)
                                        callback.onProgress("Finalizado Evento")
                                        callback.onSuccess(true)
                                    }
                                    //  callback.getInfo(SALIDAEVENTOREGISTROS as MutableList<String>)
                                }

                                else -> {

                                    try {
                                        val iC = indexCount.maxOrNull()
                                        val HEXAStar = SALIDAEVENTOREGISTROS[iC!!]?.substring(0, 8)
                                        val HEXAEnd = SALIDAEVENTOREGISTROS[iC!!]?.substring(8, 16)
                                        val HEXAEndMIlisegundosEventCorte =
                                            HEXAEnd?.toLong(16)?.times(1000)
                                        Log.d(
                                            "funcionUltime",
                                            "iC $iC  ${SALIDAEVENTOREGISTROS[iC!!]}"
                                        )
                                        /******************************************************************/
                                        var SalidaEvent: MutableList<String?> = ArrayList()
                                        SALIDAEVENTOREGISTROS.map {
                                            val hexString =
                                                it!!.substring(
                                                    0,
                                                    8
                                                ) // valor hexadecimal que se desea convertir
                                            val hexString2 = it.substring(8, 16)
                                            val star = convertirHexAFecha(hexString)
                                            val end = convertirHexAFecha(hexString2)
                                            Log.d(
                                                "funcionUltime",
                                                " $it inicio  $star \n final $end"
                                            )
                                            println(" SALIDAEVENTOREGISTROS $it inicio  $star \n final $end")
                                        }
                                        bluetoothLeService = bluetoothServices.bluetoothLeService()
                                        bluetoothLeService?.sendFirstComando("405B")
                                        listData.clear()
                                        Thread.sleep(700)
                                        for (num in 0..1) {
                                            listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                                            Log.d("islistttt2", ":" + listData[num])
                                            Thread.sleep(700)
                                        }
                                        val s = listData[0]
                                        val sSinEspacios = s?.replace(" ", "")
                                        var TIMEUNIX: String? = null

                                        listData?.let {
                                            if (sSinEspacios?.length!! >= 24) TIMEUNIX =
                                                sSinEspacios?.substring(16, 24) else TIMEUNIX =
                                                "612F6B47"
                                            var listTIMEUNIX: MutableList<String>? = ArrayList()
                                            TIMEUNIX?.let {
                                                listTIMEUNIX?.add(it)
                                            };"612F6B47"
                                        }

                                        val TimeStampControl = TIMEUNIX
                                        val TimeStampActual = GetNowDateExa()
                                        val TimeStampControlMIlisegundos =
                                            TimeStampControl?.toLong(16)?.times(1000)
                                        val currentTimeHexTimeStampControl =
                                            toHexString(
                                                TimeStampControlMIlisegundos?.div(1000) ?: 0
                                            )
                                        val TimeStampActualMIlisegundos =
                                            TimeStampActual.toLong(16) * 1000
                                        val currentTimeHexTimeStampActual =
                                            toHexString(TimeStampActualMIlisegundos / 1000)
                                        val registrosLastMIlisegundos =
                                            SALIDAEVENTOREGISTROS.last()?.substring(8, 16)
                                                ?.toLong(16)?.times(1000)
                                        val currentTimeHexregistrosLastMIlisegundos =
                                            toHexString(registrosLastMIlisegundos?.div(1000) ?: 0)
                                        val diferenciaTIMESTAMPActualvsControl =
                                            TimeStampActualMIlisegundos - registrosLastMIlisegundos!!
                                        val currentTimeHexDiferenciaACTUALvsControl =
                                            toHexString((TimeStampActualMIlisegundos) / 1000)
                                        Log.d(
                                            "funcionUltime",
                                            "TIEMPOS s ${s?.get(0)} sSinEspacios $sSinEspacios  TIMEUNIX $TIMEUNIX TimeStampActual $TimeStampActual"
                                        )
                                        /*****************************************CICLO PARA ESTAMPAR DESPUES DEL ULTIMO CORTE DE LUZ************************************************/
                                        for (i in SALIDAEVENTOREGISTROS.size - 1 downTo iC + 1) {
                                            val HEXAStarCiclo =
                                                SALIDAEVENTOREGISTROS[i]!!.substring(0, 8)
                                            val HEXAEndCiclo =
                                                SALIDAEVENTOREGISTROS[i]!!.substring(8, 16)
                                            val HEXAEndCicloMENOSHEXAStarCiclo =
                                                (HEXAEndCiclo.toLong(16) * 1000) - (HEXAStarCiclo.toLong(
                                                    16
                                                ) * 1000)
                                            val HEXAEndMIlisegundos = HEXAEndCiclo.toLong(16) * 1000
                                            val Dif = TimeStampControlMIlisegundos?.minus(
                                                HEXAEndMIlisegundos
                                            )
                                            val DiferenciaNowvsDif =
                                                TimeStampActualMIlisegundos - Dif!!
                                            val DiferenciaFINALstar =
                                                DiferenciaNowvsDif - HEXAEndCicloMENOSHEXAStarCiclo
                                            val exaFinalEnd = toHexString(DiferenciaNowvsDif / 1000)
                                            val exaFinalStar =
                                                toHexString(DiferenciaFINALstar / 1000)
                                            val s = "$exaFinalStar$exaFinalEnd${
                                                SALIDAEVENTOREGISTROS[i]!!.substring(16)
                                            }"
                                            SalidaEvent.add(s)
                                            println(
                                                "ciclo ${SALIDAEVENTOREGISTROS[i]} $Dif   $DiferenciaNowvsDif   star $${
                                                    convertirHexAFecha(
                                                        exaFinalStar
                                                    )
                                                }   end ${convertirHexAFecha(exaFinalEnd)} $s ${s.length} "
                                            )
                                        }
                                        for (i in iC downTo 0) {
                                            SalidaEvent.add(SALIDAEVENTOREGISTROS[i])
                                        }
                                        SalidaEvent.map {
                                            val hexString = it?.substring(
                                                0,
                                                8
                                            ) // valor hexadecimal que se desea convertir
                                            val hexString2 = it?.substring(8, 16)
                                            val star =
                                                hexString?.let { it1 -> convertirHexAFecha(it1) }
                                            val end =
                                                hexString2?.let { it1 -> convertirHexAFecha(it1) }
                                            println("$it   star $star end $end ")
                                        }
                                        callback.onSuccess(true)
                                        callback.getInfo(SalidaEvent as MutableList<String>?)
                                    } catch (exc: Exception) {
                                        callback.onError(exc.toString())
                                        callback.onSuccess(false)
                                        callback.onProgress("Finalizado Evento")
                                    }
                                }
                            }

                            callback.onSuccess(true)

                            //   callback.getInfo(SALIDAEVENTOREGISTROS as MutableList<String>?)
                        }
                        /*
                      if (FINALLISTA.isNotEmpty()) {
                          getInfoList()!!.clear()
                          bluetoothLeService!!.sendFirstComando("405B")
                          Thread.sleep(700)
                          listData.clear()
                          listData.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                          val s = listData[0]
                          val sSinEspacios = s?.replace(" ", "")
                          var TIMEUNIX: String? = null
                          if (sSinEspacios?.length!! >= 24) TIMEUNIX =
                              sSinEspacios?.substring(16, 24) else TIMEUNIX = "612F6B47"
                          var listTIMEUNIX: MutableList<String>? = ArrayList()
                          TIMEUNIX?.let {
                              listTIMEUNIX?.add(it)
                          };"612F6B47"
                          val TIMEUNIX2022 = "62840A56"//"75840A56"// "62840A56"
                          Log.d("FINALLISTA", "DATOS DEL TIME  $TIMEUNIX")

                          var registros = mutableListOf<String>()
                          FINALLISTA?.map {
                              if (it!!.length > 18) {
                                  registros.add(it)
                              }
                          }
                          registros?.map {
                              Log.d("HHHHHHH", "$it")
                              val hexString =
                                  it.substring(0, 8) // valor hexadecimal que se desea convertir
                              val hexString2 = it.substring(8, 16)
                              Log.d(
                                  "FFFFFFFG",
                                  "$it   ${convertirHexAFecha(hexString)}   ${
                                      convertirHexAFecha(hexString2)
                                  } "
                              )
                          }
                         var  registrosx =
                              LOGGEREVENTZ(registros as MutableList<String?>) /// LOGGEREVENT(registros as MutableList<String?>) //mutableListOf<String>()
                          var SalidaEvent: MutableList<String?> = ArrayList()
                          val TimeStampControl = TIMEUNIX
                          val TimeStampActual = GetNowDateExa()
                          val TimeStampControlMIlisegundos = TimeStampControl!!.toLong(16) * 1000
                          val currentTimeHexTimeStampControl =
                              toHexString(TimeStampControlMIlisegundos / 1000)

                          val TimeStampActualMIlisegundos = TimeStampActual.toLong(16) * 1000
                          val currentTimeHexTimeStampActual =
                              toHexString(TimeStampActualMIlisegundos / 1000)


                          val registrosLastMIlisegundos =
                              registrosx.last().substring(8, 16).toLong(16) * 1000
                          val currentTimeHexregistrosLastMIlisegundos =
                              toHexString(registrosLastMIlisegundos / 1000)

                          val diferenciaTIMESTAMPActualvsControl =
                              TimeStampActualMIlisegundos - registrosLastMIlisegundos
                          val currentTimeHexDiferenciaACTUALvsControl =
                              toHexString((TimeStampActualMIlisegundos) / 1000)


                          val indexCount: MutableList<Int> = ArrayList()
                          var count = 0
                          for ((index, item) in registrosx.withIndex()) {
                              val EVENT_TYPE = item!!.substring(16, 18)
                              if (EVENT_TYPE == "04") {
                                  count++
                                  println("-> $item  $count index $index  ")
                                  indexCount.add(index!!)
                              }
                          }

                          val iC = indexCount.last()
                          val HEXAStar = registrosx[iC].substring(0, 8)
                          val HEXAEnd = registrosx[iC].substring(8, 16)
                          val HEXAEndMIlisegundosEventCorte = HEXAEnd.toLong(16) * 1000

                          for (i in registrosx.size - 1 downTo indexCount.max() + 1) {
                              val HEXAStarCiclo = registrosx[i].substring(0, 8)
                              val HEXAEndCiclo = registrosx[i].substring(8, 16)

                              val HEXAEndCicloMENOSHEXAStarCiclo =
                                  (HEXAEndCiclo.toLong(16) * 1000) - (HEXAStarCiclo.toLong(16) * 1000)

                              val HEXAEndMIlisegundos = HEXAEndCiclo.toLong(16) * 1000
                              val Dif = TimeStampControlMIlisegundos - HEXAEndMIlisegundos
                              val DiferenciaNowvsDif = TimeStampActualMIlisegundos - Dif
                              val DiferenciaFINALstar =
                                  DiferenciaNowvsDif - HEXAEndCicloMENOSHEXAStarCiclo
                              val exaFinalEnd = toHexString(DiferenciaNowvsDif / 1000)
                              val exaFinalStar = toHexString(DiferenciaFINALstar / 1000)
                              val s = "$exaFinalStar$exaFinalEnd${registrosx[i].substring(16)}"
                              SalidaEvent.add(s)
                              println(
                                  "ciclo ${registrosx[i]} $Dif   $DiferenciaNowvsDif   star $${
                                      convertirHexAFecha(
                                          exaFinalStar
                                      )
                                  }   end ${convertirHexAFecha(exaFinalEnd)} $s ${s.length} "
                              )
                          }
                          for (i in indexCount.max() downTo 0) {
                              SalidaEvent.add(registrosx[i])
                          }


                          SalidaEvent.map {
                              val hexString =
                                  it!!.substring(0, 8) // valor hexadecimal que se desea convertir
                              val hexString2 = it!!.substring(8, 16)
                              val star = convertirHexAFecha(hexString)
                              val end = convertirHexAFecha(hexString2)
                              println("$it   star $star end $end ")
                              Log.d("SalidaFInalEventoPrueba", "$it   star $star end $end ")
                          }
                          callback.getInfo(SalidaEvent as MutableList<String>?)
                      }*/
                        else {
                            callback.onSuccess(false)
                            Log.d("funcionUltime", "Lista vacia Evento")
                            callback.onError("Lista vacia Evento")

                        }
                    } catch (ex: Exception) {
                        callback.onError(ex.toString())
                    }
                    callback.onProgress("Finalizado Evento")

                    //     if (TIMEUNIX/*"75840A56"*/!! <= TIMEUNIX2022!!) {
                    /*
                                        println("$TIMEUNIX2022  ${convertirHexAFecha(TIMEUNIX2022)}")
                                                     println("------------------------------------------------------ este es menor")
                                                  Log.d(
                                                      "getInfoListFinal",
                                                      "------------------------------------------------------ ${
                                                          convertirHexAFecha(TIMEUNIX2022)
                                                      }"
                                                  )
                                                  /*   val FINALLISTA: MutableList<String?> = VEvent(
                                                         getInfoList() as MutableList<String?>
                                                     )*/
                                                  /*    if (!FINALLISTA.isNullOrEmpty()) {
                                                          try {
                                                              val registros = mutableListOf<String>()
                                                              FINALLISTA.map {
                                                                  if (it!!.length > 18) {
                                                                      registros.add(it)
                                                                  }
                                                              }
                                                              registros.map {
                                                                  Log.d("HHHHHHH", "$it")
                                                                  val hexString =
                                                                      it.substring(0, 8) // valor hexadecimal que se desea convertir
                                                                  val hexString2 = it.substring(8, 16)
                                                                  Log.d(
                                                                      "FFFFFFFG",
                                                                      "$it   ${convertirHexAFecha(hexString)}   ${
                                                                          convertirHexAFecha(hexString2)
                                                                      } "
                                                                  )
                                                              }
                                                              val ListaTiempoUPdate =
                                                                  LOGGEREVENTZ(registros as MutableList<String?>)/// LOGGEREVENT(registros as MutableList<String?>) //mutableListOf<String>()
                                                              callback.getInfo(ListaTiempoUPdate)
                                                              val sttiempo = java.lang.StringBuilder()
                                                              Log.d("TAMAÑO", "E:$ListaTiempoUPdate")
                                                              Log.d("TAMAÑO", "ESize:" + ListaTiempoUPdate!!.size)

                                                              ListaTiempoUPdate?.let {
                                                                  it.map { ListaTiempoUPdate ->
                                                                      sttiempo.append(ListaTiempoUPdate)
                                                                      sttiempo.append(";")
                                                                  }
                                                              }
                                                              /* for (/*h in ListaTiempoUPdate/*lista*/!!.indices*/ item in ListaTiempoUPdate) {
                                                                   if (item.length > 0) {
                                                                       sttiempo.append(ListaTiempoUPdate)
                                                                       sttiempo.append(";")
                                                                   }

                                                               }*/
                                                              FinalListDataFinalE.clear()
                                                              FinalListDataFinalE.add(sttiempo.toString())

                                                              FinalListDataFinalE?.map { Log.d("TAMAÑO", "DATA :->$it") }
                                                              callback.onSuccess(GetStatusBle())
                                                              // sendInfoFirestoreNEW()
                                                              BanderaEvento = false
                                                          } catch (Ex: Exception) {
                                                              callback.onError("Evento $Ex")
                                                          }
                                                      } else {
                                                          callback.onError("Lista vacia Evento")
                                                      }
                                                      callback.onProgress("Finalizado Evento")*/

                                                  if (!FINALLISTA.isNullOrEmpty()) {
                                                      try {
                                                          val registros = mutableListOf<String>()
                                                          FINALLISTA.map {
                                                              if (it!!.length > 18) {
                                                                  registros.add(it)
                                                              }
                                                          }
                                                          registros.map {
                                                              Log.d("HHHHHHH", "$it")
                                                              val hexString =
                                                                  it.substring(0, 8) // valor hexadecimal que se desea convertir
                                                              val hexString2 = it.substring(8, 16)
                                                              Log.d(
                                                                  "FFFFFFFG",
                                                                  "$it   ${convertirHexAFecha(hexString)}   ${
                                                                      convertirHexAFecha(hexString2)
                                                                  } "
                                                              )
                                                          }
                                                          val ListaTiempoUPdate =
                                                              LOGGEREVENTZ(registros as MutableList<String?>)/// LOGGEREVENT(registros as MutableList<String?>) //mutableListOf<String>()
                                                          callback.getInfo(ListaTiempoUPdate)
                                                          val sttiempo = java.lang.StringBuilder()
                                                          Log.d("TAMAÑO", "E:$ListaTiempoUPdate")
                                                          Log.d("TAMAÑO", "ESize:" + ListaTiempoUPdate!!.size)

                                                          ListaTiempoUPdate?.let {
                                                              it.map { ListaTiempoUPdate ->
                                                                  sttiempo.append(ListaTiempoUPdate)
                                                                  sttiempo.append(";")
                                                              }
                                                          }
                                                          /* for (/*h in ListaTiempoUPdate/*lista*/!!.indices*/ item in ListaTiempoUPdate) {
                                                               if (item.length > 0) {
                                                                   sttiempo.append(ListaTiempoUPdate)
                                                                   sttiempo.append(";")
                                                               }

                                                           }*/
                                                          FinalListDataFinalE.clear()
                                                          FinalListDataFinalE.add(sttiempo.toString())

                                                          FinalListDataFinalE?.map { Log.d("TAMAÑO", "DATA :->$it") }
                                                          callback.onSuccess(GetStatusBle())
                                                          // sendInfoFirestoreNEW()
                                                          BanderaEvento = false
                                                      } catch (Ex: Exception) {
                                                          callback.onError("Evento $Ex")
                                                      }
                                                  } else {
                                                      callback.onError("Lista vacia Evento")
                                                  }
                                                  callback.onProgress("Finalizado Evento")
                    */
                    //    } else {
                    /*
                                        FINALLISTA.map {
                                            Log.d("FINALLISTA", "FINALLISTA ->>>>>>>> $it")
                                        }
                                        var listD: MutableList<String>? = ArrayList()
                                        for (item in FINALLISTA) {
                                            if (item!!.length >= 20) {
                                                listD!!.add(item!!)
                                            }
                                        }
                                        val indexCount: MutableList<Int> = ArrayList()
                                        var count = 0
                                        for ((index, item) in listD!!.withIndex()) {
                                            val EVENT_TYPE = item!!.substring(16, 18)
                                            if (EVENT_TYPE == "04") {
                                                count++
                                                println("-> $item  $count index $index")
                                                indexCount.add(index!!)
                                            }
                                        }

                                        for((index, item) in listD!!.withIndex()){
                                            Log.d("FINALLISTA","$index  $item")
                                        }
                                        val iC = indexCount.maxOrNull()
                                        when (count) {

                                            0 -> {
                                                callback.getInfo(listD as MutableList<String>?)
                                                callback.onProgress("Finalizado Evento")
                                            }
                                            1 -> {
                                               Log.d("SalidaEVENTO","Hay $count elemento con EVENT_TYPE == \"04\"")
                                                val currentTimeMillis = System.currentTimeMillis()
                                                val diferenciaTOP =
                                                    listD.last().substring(8, 16).toLong(16) * 1000 - (listD.last()!!
                                                        .substring(0, 8)
                                                        .toLong(16) * 1000)

                                                val currentTimeHexNOW = toHexString(currentTimeMillis / 1000)
                                                val MILLNOWMINUSDIF = currentTimeMillis - diferenciaTOP

                                                listD.map {
                                                    val hexString =
                                                        it!!.substring(0, 8) // valor hexadecimal que se desea convertir
                                                    val hexString2 = it!!.substring(8, 16)
                                                    println(
                                                        "$it   ${convertirHexAFecha(hexString)}   ${
                                                            convertirHexAFecha(
                                                                hexString2
                                                            )
                                                        }"
                                                    )
                                                }
                                                val currentTimeHexMILLNOWMINUSDIF = toHexString(MILLNOWMINUSDIF / 1000)


                                                val d =
                                                    (currentTimeHexMILLNOWMINUSDIF + currentTimeHexNOW + listD.last()
                                                        .toString().substring(16, listD.last().length)).uppercase()
                                                listaTemporalFINALEVENTO.add(d)
                                                val hexString = listaTemporalFINALEVENTO.last()
                                                    .substring(0, 8) // valor hexadecimal que se desea convertir
                                                val hexString2 = listaTemporalFINALEVENTO.last()!!.substring(8, 16)
                                                for (i in listD.size - 2 downTo indexCount.max()) {

                                                    val diferenciaTOP =
                                                        listD[i + 1]!!.substring(8, 16)
                                                            .toLong(16) * 1000 - (listD[i]!!.substring(
                                                            8, 16
                                                        ).toLong(16) * 1000)
                                                    val diferencia =
                                                        listD[i]!!.substring(8, 16)
                                                            .toLong(16) * 1000 - (listD[i]!!.substring(0, 8)
                                                            .toLong(16) * 1000)

                                                    val diferenciaTOPMINUSlast =
                                                        ((listaTemporalFINALEVENTO.last()!!.substring(8, 16)
                                                            .toLong(16) * 1000) - diferenciaTOP)
                                                    val currentTimeHex = toHexString(diferenciaTOPMINUSlast / 1000)
                                                            .uppercase()
                                                    val LINEARDIFMILL = diferenciaTOPMINUSlast - diferencia
                                                    val currentTimeHexLINEAL =toHexString(LINEARDIFMILL / 1000).uppercase()
                                                    val d =
                                                        (currentTimeHexLINEAL + currentTimeHex + listD[i]!!.substring(
                                                            16, listD.last().length )).uppercase()
                                                    listaTemporalFINALEVENTO.add(d)
                                                }
                                                for (i in indexCount.max() - 1 downTo 0) {
                                                    listaTemporalFINALEVENTO.add(listD[i])
                                                }
                                                callback.getInfo(listaTemporalFINALEVENTO as MutableList<String>?)
                                                callback.onProgress("Finalizado Evento")
                                            }
                                            2 -> {
                                                callback.getInfo(listD as MutableList<String>?)
                                                callback.onProgress("Finalizado Evento")
                                                //   println("Hay $count elemento con EVENT_TYPE == \"04\"")


                                            }
                                            else -> {
                                                callback.getInfo(listD as MutableList<String>?)
                                                callback.onProgress("Finalizado Evento")
                                            }
                                        }


                                        /*       callback.getInfo(listD as MutableList<String>?)
                                           callback.onProgress("Finalizado Evento")*/
                    */
                    //       }
                } else {
                    callback.onError("Desconectado")
                    callback.onProgress("Finalizado Evento")
                }
            } catch (Exce: Exception) {
                callback.onError("error 2792 ${Exce.toString()}")
                callback.onProgress("Finalizado Evento")
            }

        }

        override fun onPreExecute() {
            getInfoList()?.clear()
            bluetoothLeService = bluetoothServices.bluetoothLeService()
            callback.onProgress("Iniciando Evento")

            BanderaEvento = true
        }
    }

    inner class MyAsyncTaskGeTIMEVALIDACION(private val callback: MyCallback) :
        AsyncTask<Int?, Int?, MutableList<String>>() {
        var registros = mutableListOf<String>()
        var registrosPRE = mutableListOf<String>()
        override fun doInBackground(vararg params: Int?): MutableList<String>? {
            FinalListDataTiempo.clear()
            FinalListDataFinalT.clear()
            var listDataGT: MutableList<String>? = ArrayList()
            callback.onProgress("Realizando Tiempo")
            /*      if (ValidaConexion()) {
                      bluetoothLeService = bluetoothServices.bluetoothLeService()
                      Thread.sleep(500)
                      bluetoothServices!!.sendCommand("handshake", "4021")
                      Thread.sleep(500)


                      getInfoList()?.clear()
                      bluetoothServices!!.sendCommand("time", "4060")
                      /*  do {


                            listDataGT!!.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                            //  Log.d("islistttt2", ":" + listDataGT[num])
                            Thread.sleep(700)

                        } while (bluetoothLeService!!.dataFromBroadcastUpdateString.isNotEmpty())

                        */
                      do {


                          listDataGT!!.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                          //  Log.d("islistttt2", ":" + listDataGT[num])
                          Thread.sleep(700)

                      } while (bluetoothLeService?.dataFromBroadcastUpdateString?.isNotEmpty() == true)
                  }
      */
            return listDataGT// getInfoList()//

        }

        override fun onPostExecute(result: MutableList<String>?) {
            Thread.sleep(700)
            Log.d("getInfoListFinal", getInfoList().toString())
            Thread.sleep(700)
            FinalListDataTiempo.clear()
            FinalListDataFinalT.clear()
            var listDataGT: MutableList<String>? = ArrayList()
            var listDataEVENT: MutableList<String>? = ArrayList()
            // callback.onProgress("Realizando Tiempo")
            if (ValidaConexion()) {
                bluetoothLeService = bluetoothServices.bluetoothLeService()
                Thread.sleep(500)
                bluetoothServices.sendCommand("handshake", "4021")
                Thread.sleep(500)


                getInfoList()?.clear()
                bluetoothServices.sendCommand("time", "4060")
                Thread.sleep(3000)
                if (listDataGT != null) {
                    synchronized(listDataGT) {
                        do {
                            bluetoothLeService?.dataFromBroadcastUpdateString?.let {
                                listDataGT.add(
                                    it
                                )
                            }
                            Thread.sleep(700)
                        } while (bluetoothLeService?.dataFromBroadcastUpdateString?.isNotEmpty() == true)
                    }
                }
                /*  do {


                      listDataGT!!.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                      //  Log.d("islistttt2", ":" + listDataGT[num])
                      Thread.sleep(700)

                  } while (bluetoothLeService!!.dataFromBroadcastUpdateString.isNotEmpty())

                  */
                /*   do {


                       bluetoothLeService?.dataFromBroadcastUpdateString?.let { listDataGT?.add(it) }
                       //  Log.d("islistttt2", ":" + listDataGT[num])
                       Thread.sleep(700)

                   } while (bluetoothLeService?.dataFromBroadcastUpdateString?.isNotEmpty() == true)
                   */
                Thread.sleep(2000)
                val FINALLISTA: MutableList<String?> = VTIME(
                    getInfoList() as MutableList<String?>
                )

                getInfoList()!!.clear()
                Thread.sleep(1000)
                bluetoothServices.sendCommand("handshake", "4021")
                Thread.sleep(700)
                getInfoList()!!.clear()
                bluetoothServices.sendCommand("event", "4061")

                /* do {


                     bluetoothLeService?.dataFromBroadcastUpdateString?.let { listDataEVENT?.add(it) }
                     //  Log.d("islistttt2", ":" + listDataGT[num])
                     Thread.sleep(700)

                 } while (bluetoothLeService?.dataFromBroadcastUpdateString?.isNotEmpty() == true)*/
                if (listDataEVENT != null) {
                    synchronized(listDataEVENT) {
                        do {
                            bluetoothLeService?.dataFromBroadcastUpdateString?.let {
                                listDataEVENT.add(
                                    it
                                )
                            }
                            Thread.sleep(700)
                        } while (bluetoothLeService?.dataFromBroadcastUpdateString?.isNotEmpty() == true)
                    }
                }
                registrosPRE = VEvent(
                    getInfoList() as MutableList<String?>
                ) as MutableList<String>
                for (item in registrosPRE) {
                    if (item.length > 15) {
                        registros.add(item)
                    }
                }

                if (FINALLISTA.isNotEmpty()) {
                    var smallestDifference: Long = Long.MAX_VALUE
                    //   var closestIndex = -1

                    var listFTIME: MutableList<String>? = ArrayList()
                    for (item in FINALLISTA) {
                        Log.d("FINALLISTA", "$item ")
                        if (item!!.length >= 10) {
                            listFTIME!!.add(item!!)
                        }
                    }

                    if (listFTIME != null) {
                        for ((index, item) in listFTIME.withIndex()) {

                            Log.d(
                                "FINALLISTAFINALLISTA",
                                " $index $item ${convertirHexAFecha(item.substring(0, 8))}"
                            )
                        }
                    }

                    //   val hexString2 =it.substring(8, 16)
                    //           println("->S $it   ${convertirHexAFecha(hexString)}   ${convertirHexAFecha(hexString2)}")
                    /*  if (closestTimestamp != null) {
                          Log.d("FFFFFFFFFFFFFFFFF","El valor más cercano al timestamp objetivo ($targetTimestamp) ${targetTimestamp?.let {
                              convertirHexAFecha(
                                  it
                              )
                          }} es: $closestTimestamp   ${convertirHexAFecha(closestTimestamp.substring(0, 8))}")
                      }
          */

                    //  try{
                    getInfoList()?.clear()
                    bluetoothLeService!!.sendFirstComando("405B")
                    Thread.sleep(700)
                    listData.clear()
                    listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                    val s = listData[0]
                    val sSinEspacios = s?.replace(" ", "")
                    var TIMEUNIX: String? = null
                    if (sSinEspacios?.length!! >= 24) TIMEUNIX =
                        sSinEspacios?.substring(16, 24) else TIMEUNIX = "612F6B47"
                    var listTIMEUNIX: MutableList<String>? = ArrayList()
                    TIMEUNIX?.let {
                        listTIMEUNIX?.add(it)
                    };"612F6B47"
                    val TIMEUNIX2022 = "62840A56"//"75840A56"// "62840A56"
                    if (TIMEUNIX/*"75840A56"*/!! <= TIMEUNIX2022!!) {
                        //  if (/*TIMEUNIX/*/*"75840A56"*/!!<=TIMEUNIX2022!!){
                        println("------------------------------------------------------ este es menor")
                        Log.d(
                            "getInfoListFinal",
                            "------------------------------------------------------ este es menor"
                        )

                        if (!FINALLISTA.isNullOrEmpty()) {
                            try {
                                val registros = mutableListOf<String>()
                                FINALLISTA.map {
                                    if (it!!.length == 18) {
                                        registros.add(it)
                                    }
                                }
                                val ListaTiempoUPdate =
                                    LOGGERTIME(registros) //mutableListOf<String>()

                                val sttiempo = java.lang.StringBuilder()
                                Log.d("TAMAÑO", "T:$ListaTiempoUPdate")
                                Log.d("TAMAÑO", "TSize:" + ListaTiempoUPdate.size)
                                for (h in ListaTiempoUPdate/*lista*/.indices) {
                                    if (h > 0) {
                                        sttiempo.append(ListaTiempoUPdate/*lista*/[h])
                                        sttiempo.append(";")
                                    }

                                }
                                FinalListDataFinalT.add(sttiempo.toString())
                                callback.getInfo(FinalListDataFinalT as MutableList<String>?)
                                FinalListDataFinalT?.map { Log.d("TAMAÑO", "DATA :$it") }
                                callback.onSuccess(GetStatusBle())
                                // sendInfoFirestoreNEW()
                                BanderaTiempo = false
                            } catch (Ex: Exception) {
                                callback.onError("Tiempo $Ex")
                            }
                        } else {
                            callback.onSuccess(false)
                            callback.onError("Lista vacia Tiempo")
                        }
                        callback.onProgress("Finalizado Tiempo")
                        callback.onSuccess(true)

                    } else {

                        /* val registros = arrayListOf(
                             "6467B5AC6467BD1901004110097E",
                             "6467B5E06467BD7302004020097E",
                             "6467BD1C6467BDA001004030097E",
                             "6467B0006467B12504003F40097F",
                             "6467B1C56467B1F503003F50097F",
                             "6467B1E56467B2F503003660097F"
                         )*/
                        /* val registrosTIMEPODUMMY = arrayListOf(
                              "6467ABAC0040000971",
                              "6467ABFC0040000972",
                              "6467B0000040000973",
                              "6467BE350040000974",
                              "6467BE9D0040000975",
                              "6467BED90040000976",
                              "6467BF150040000977",
                              "6467BF510040000978",
                              "6467BF8D0040000979"
                          )*/
                        val indexCount: MutableList<Int> = ArrayList()
                        var count = 0
                        for ((index, item) in registros.withIndex()) {
                            val EVENT_TYPE = item!!.substring(16, 18)
                            if (EVENT_TYPE == "04") {
                                count++
                                println("-> $item  $count index $index")
                                indexCount.add(index!!)
                            }
                        }
                        val iC = indexCount.maxOrNull()
                        var listaTemporalFINALTIME = mutableListOf<String>()
                        when (count) {

                            0 -> {
                                Log.d(
                                    "FFFFFFFFFFFFFFFFF", "No hay elementos con EVENT_TYPE == \"04\""
                                )
                                var listD: MutableList<String>? = ArrayList()
                                for (item in FINALLISTA) {
                                    if (item!!.length >= 10) {
                                        listD!!.add(item!!)
                                    }
                                }
                                Log.d(
                                    "FFFFFFFFFFFFFFFFF",
                                    "Hay $count elemento con EVENT_TYPE == \"04\""
                                )
                                callback.getInfo(listD)
                            }

                            /*   1 -> {

                                   registros?.map {
                                       val hexString =it.substring(0, 8) // valor hexadecimal que se desea convertir
                                       val hexString2 =it.substring(8, 16)
                                       Log.d(
                                           "datosResult->>>",
                                           "->S $it   ${convertirHexAFecha(hexString)}   ${
                                               convertirHexAFecha(hexString2)
                                           } "
                                       )
                                   } ;Log.d(
                                       "datosResult->>>",
                                       "->S VACIO "
                                   )
           //                        Log.d("FFFFFFFFFFFFFFFFF","Índice correspondiente: $closestIndex  ${listFTIME?.get(closestIndex)}")
                                   var closestTimestamp: String? = null
                                   var closestIndex = -1
                                   var smallestDifference: Long = Long.MAX_VALUE
                                     val indexCount: MutableList<Int> = ArrayList()
                                   var count = 0
                                   for ((index, item) in registros.withIndex()) {
                                       val EVENT_TYPE = item!!.substring(16, 18)
                                       if (EVENT_TYPE == "04") {
                                           count++
                                           println("-> $item  $count index $index")
                                           indexCount.add(index!!)
                                       }
                                   }
                                   val iC = indexCount.maxOrNull()
                                   Log.d("FFFFFFFFFFFFFFFFF","Hay $count elemento con EVENT_TYPE == \"04\"")
                                   val targetTimestamp =  registros[iC!!].substring(8, 16)//"6467B000" // Valor objetivo
                                   for ((index, registro) in listFTIME!!.withIndex()) {
                                       val timestampDifference = Math.abs(targetTimestamp.toLong(16) - registro.substring(0, 8).toLong(16))
                                       if (timestampDifference < smallestDifference) {
                                           smallestDifference = timestampDifference
                                           closestTimestamp = registro
                                           closestIndex = index
                                       }
                                   }
                                   Log.d("FFFFFFFFFFFFFFFFF","El valor más cercano al timestamp objetivo ($targetTimestamp) ${convertirHexAFecha(targetTimestamp)} ")

                                   Log.d("FFFFFFFFFFFFFFFFF","El valor más cercano al timestamp objetivo  es: $closestTimestamp   ")
                                   Log.d("FFFFFFFFFFFFFFFFF","Índice correspondiente: $closestIndex  ${listFTIME?.get(closestIndex)}  ${convertirHexAFecha(listFTIME?.get(closestIndex)!!.substring(0,8))} ")


                                   for ((index, registro) in listFTIME.withIndex()) {
                                       val hexString =registro.substring(0, 8)  // valor hexadecimal que se desea convertir
                                       //   val hexString2 =it.substring(8, 16)
                                       println("->S $registro   ${convertirHexAFecha(hexString)}  $index ")
                                   }
                                   registros.map{
                                       val hexString =it.substring(0, 8) // valor hexadecimal que se desea convertir
                                       val hexString2 =it.substring(8, 16)
                                      Log.d("FFFFFFFFFFFFFFFFF","->S $it   ${convertirHexAFecha(hexString)}   ${convertirHexAFecha(hexString2)}")
                                   }
                                   val hexStringFINALTIME = listFTIME.last().substring(0, 8)
                                   val currentTimeMillis = System.currentTimeMillis()
                                   val currentTimeHexMILLNOWMINUSDIF =
                                       java.lang.Long.toHexString(currentTimeMillis / 1000).uppercase()

                                   listaTemporalFINALTIME.add("$currentTimeHexMILLNOWMINUSDIF${listFTIME.last().substring(8,listFTIME.last().length)}")
                                   //   println("------------------------------------ $listaTemporalFINALTIME")
                                   for (i in listFTIME.size - 2 downTo closestIndex +1) {
                                       val registro = listFTIME[i]
                                       // Aquí puedes realizar alguna operación con el registro, por ejemplo:
                                       //   println("Registro $i: $registro  $currentTimeHexMILLNOWMINUSDIF")

                                       val fechaHora1 = Date(listFTIME[i]!!.substring(0, 8).toLong(16) * 1000)
                                       val fechaHora2 = Date(listFTIME[i - 1]!!.substring(0, 8).toLong(16) * 1000)
                                       val dif = (listFTIME[i]!!.substring(0, 8).toLong(16) * 1000)-(listFTIME[i - 1]!!.substring(0, 8).toLong(16) * 1000)
                                       val FINALDIF= (listaTemporalFINALTIME.last()!!.substring(0, 8).toLong(16) * 1000) - dif
                                       //   val dateFINALDIF= Date(FINALDIF)
                                       val date = Date(FINALDIF)
                                       val fechaHoraExadecimal =
                                           BigInteger.valueOf(date.time / 1000).toString(16).padStart(8, '0').uppercase()

                                       //  println("PPPPPPPPPPPPPPPPPPPPPPPPPPP  ${listaTemporalFINALTIME.last()} ${date}  $FINALDIF  $fechaHoraExadecimal  ")
                                       listaTemporalFINALTIME.add(fechaHoraExadecimal+listFTIME[i]!!.substring(8,listFTIME[i]!!.length))
                                       // println(" ${registrosTIMEPODUMMY[i]} $i  fechaHora1 $fechaHora1 fechaHora2 $fechaHora2 dif $dif   ------------- ${registrosTIMEPODUMMY[i]} ${registrosTIMEPODUMMY[i-1]} ")


                                   }

                                   for (i in closestIndex downTo 0) {
                                       listaTemporalFINALTIME.add(listFTIME[i])
                                   }

                                   println("------------------------------------")
                                   listaTemporalFINALTIME.map{
                                       val hexString = it!!.substring(0, 8)
                                       println(" $it ${convertirHexAFecha(hexString)}")
                                   }
                                   callback.getInfo(listaTemporalFINALTIME as MutableList<String>?)
                               }*/
                            1 -> {
                                Log.d(
                                    "FFFFFFFFFFFFFFFFF",
                                    "Hay $count elemento con EVENT_TYPE == \"04\"    ${registros[iC!!]}  ${
                                        convertirHexAFecha(registros[iC!!].substring(8, 16))
                                    }"
                                )

                                val targetTimestamp =
                                    registros[iC!!].substring(8, 16)//"6467B000" // Valor objetivo


                                var closestTimestamp: String? = null
                                var smallestDifference: Long = Long.MAX_VALUE
                                var closestIndex = -1

                                for ((index, registro) in listFTIME!!.withIndex()) {
                                    val timestampDifference = abs(
                                        targetTimestamp.toLong(16) - registro.substring(0, 8)
                                            .toLong(16)
                                    )
                                    if (timestampDifference < smallestDifference) {
                                        smallestDifference = timestampDifference
                                        closestTimestamp = registro
                                        closestIndex = index
                                    }
                                }

                                //   val hexString2 =it.substring(8, 16)
                                //           println("->S $it   ${convertirHexAFecha(hexString)}   ${convertirHexAFecha(hexString2)}")


                                Log.d(
                                    "FFFFFFFFFFFFFFFFF",
                                    "El valor más cercano al timestamp objetivo ($targetTimestamp) ${
                                        convertirHexAFecha(targetTimestamp)
                                    } es: $closestTimestamp   ${
                                        convertirHexAFecha(
                                            closestTimestamp!!.substring(
                                                0,
                                                8
                                            )
                                        )
                                    }"
                                )
                                Log.d(
                                    "FFFFFFFFFFFFFFFFF",
                                    "Índice correspondiente: $closestIndex  ${listFTIME[closestIndex]}"
                                )

                                /*for(item in registrosTIMEPODUMMY ){

                                }*/
                                for ((index, registro) in listFTIME.withIndex()) {
                                    val hexString = registro.substring(
                                        0,
                                        8
                                    )  // valor hexadecimal que se desea convertir
                                    //   val hexString2 =it.substring(8, 16)
                                    Log.d(
                                        "MyAsyncTaskGeTIME",
                                        "->S $registro   ${convertirHexAFecha(hexString)}  $index "
                                    )
                                }
                                registros.map {
                                    val hexString =
                                        it.substring(
                                            0,
                                            8
                                        ) // valor hexadecimal que se desea convertir
                                    val hexString2 = it.substring(8, 16)
                                    Log.d(
                                        "FFFFFFFFFFFFFFFFF",
                                        "->S $it   ${convertirHexAFecha(hexString)}   ${
                                            convertirHexAFecha(hexString2)
                                        }"
                                    )
                                }
                                //  Log.d("CONTEOFINAL ","registrosRECIVRE ${registros.size}")
                                val hexStringFINALTIME = listFTIME.last().substring(0, 8)
                                Log.d(
                                    "FFFFFFFFFFFFFFFFF",
                                    "FINAL listFTIME.last()  ${listFTIME.last()} ${
                                        listFTIME.last().substring(0, 8)
                                    }"
                                )

                                for (i in closestIndex downTo 0) {
                                    val item = listFTIME[i]
                                    Log.d(
                                        "FFFFFFFFFFFFFFFFF",
                                        "${listFTIME[i]}   ${
                                            convertirHexAFecha(
                                                listFTIME[i].substring(
                                                    0,
                                                    8
                                                )
                                            )
                                        } "
                                    )
                                }
                                val currentTimeMillis = System.currentTimeMillis()
                                val currentTimeHexMILLNOWMINUSDIF =
                                    toHexString(currentTimeMillis / 1000).uppercase()
                                listaTemporalFINALTIME.add(
                                    "$currentTimeHexMILLNOWMINUSDIF${
                                        listFTIME.last().substring(8, listFTIME.last().length)
                                    }"
                                )
                                for (i in listFTIME.size - 2 downTo closestIndex + 1) {
                                    val item = listFTIME[i]
                                    Log.d(
                                        "FFFFFFFFFFFFFFFFF",
                                        " complemento ${listFTIME[i]}   ${
                                            convertirHexAFecha(
                                                listFTIME[i].substring(
                                                    0,
                                                    8
                                                )
                                            )
                                        } "
                                    )

                                    val fechaHora1 =
                                        Date(listFTIME.get(i)!!.substring(0, 8).toLong(16) * 1000)
                                    val fechaHora2 =
                                        Date(listFTIME[i - 1]!!.substring(0, 8).toLong(16) * 1000)

                                    val dif = (listFTIME?.get(i)!!.substring(0, 8)
                                        .toLong(16) * 1000) - (listFTIME[i - 1].substring(0, 8)
                                        .toLong(16) * 1000)
                                    // val difBACK = (listFTIME?.get(i-1)!!.substring(0, 8).toLong(16) * 1000) - (listFTIME[i-2].substring(0, 8).toLong(16) * 1000)
                                    //(listFTIME?.get(i+1)!!.substring(0, 8).toLong(16) * 1000) - (listFTIME[i].substring(0, 8).toLong(16) * 1000)
                                    val difBACK = kotlin.math.abs(
                                        (listFTIME?.get(i - 1)!!.substring(0, 8)
                                            .toLong(16) * 1000) - (listFTIME[i].substring(0, 8)
                                            .toLong(16) * 1000)
                                    )

                                    //     val difBACK = (listFTIME?.get(i+1)!!.substring(0, 8).toLong(16) * 1000) - (listFTIME[i].substring(0, 8).toLong(16) * 1000)

                                    Log.d(
                                        "FFFFFFFFFFFFFFFFF",
                                        "------------------------------------ difBACK $difBACK $i"
                                    )
                                    var difFINAL: Long = 0
                                    if (dif == difBACK) {
                                        difFINAL = 60000 //dif
                                    } else {
                                        difFINAL = 60000
                                    }
                                    /*   val difFINAL: Long = if (dif == difBACK) {
                                           dif
                                       } else {
                                           60000//difBACK

                                       }*/
                                    val FINALDIF = (listaTemporalFINALTIME.last().substring(0, 8)
                                        .toLong(16) * 1000) - difFINAL
                                    val date = Date(FINALDIF)
                                    val fechaHoraExadecimal =
                                        BigInteger.valueOf(date.time / 1000).toString(16)
                                            .padStart(8, '0')
                                            .uppercase()
                                    listaTemporalFINALTIME.add(
                                        fechaHoraExadecimal + listFTIME[i].substring(
                                            8, listFTIME[i]!!.length
                                        )
                                    )

                                }


                                //   println("------------------------------------ $listaTemporalFINALTIME")

                                /*  for (i in closestIndex downTo 0) {
                                       listaTemporalFINALTIME.add(listFTIME!!.get(i))
                                  }*/
                                /*                        for (i in listFTIME.size - 2 downTo closestIndex + 1) {
                                                             val registro = listFTIME[i]
                                                             // Aquí puedes realizar alguna operación con el registro, por ejemplo:
                                                             //   println("Registro $i: $registro  $currentTimeHexMILLNOWMINUSDIF")

                                                             val fechaHora1 =
                                                                 Date(listFTIME.get(i)!!.substring(0, 8).toLong(16) * 1000)
                                                             val fechaHora2 =
                                                                 Date(listFTIME[i - 1]!!.substring(0, 8).toLong(16) * 1000)
                                                             val dif = (listFTIME?.get(i)!!.substring(0, 8)
                                                                 .toLong(16) * 1000) - (listFTIME[i - 1]!!.substring(0, 8)
                                                                 .toLong(16) * 1000)
                                                             val FINALDIF = (listaTemporalFINALTIME.last()!!.substring(0, 8)
                                                                 .toLong(16) * 1000) - dif
                                                             //   val dateFINALDIF= Date(FINALDIF)
                                                             val date = Date(FINALDIF)
                                                             val fechaHoraExadecimal =
                                                                 BigInteger.valueOf(date.time / 1000).toString(16)
                                                                     .padStart(8, '0')
                                                                     .uppercase()

                                                             println("PPPPPPPPPPPPPPPPPPPPPPPPPPP  ${listaTemporalFINALTIME.last()} ${date}  $FINALDIF  $fechaHoraExadecimal  " +
                                                                     "listFTIME.size - 2 ${listFTIME.size - 2} downTo closestIndex + 1 ${closestIndex+1} ${i}  saqlida " +
                                                                     "${fechaHoraExadecimal + listFTIME[i]!!.substring(
                                                                         8,
                                                                         listFTIME[i]!!.length
                                                                     )}")
                                                            /* listaTemporalFINALTIME.add(
                                                                 fechaHoraExadecimal + listFTIME[i]!!.substring(
                                                                     8,
                                                                     listFTIME[i]!!.length
                                                                 )
                                                             )*/
                                                             // println(" ${registrosTIMEPODUMMY[i]} $i  fechaHora1 $fechaHora1 fechaHora2 $fechaHora2 dif $dif   ------------- ${registrosTIMEPODUMMY[i]} ${registrosTIMEPODUMMY[i-1]} ")


                                                         }
                             */


                                Log.d(
                                    "FFFFFFFFFFFFFFFFF",
                                    "------------------------------------ listFTIME ${listFTIME.size}  listaTemporalFINALTIME ${listaTemporalFINALTIME.size}"
                                )


                                for (i in closestIndex downTo 0) {
                                    val item = listFTIME[i]
                                    Log.d(
                                        "datosResultTIME->>>",
                                        "LLLLLLL ${listFTIME[i]}   ${
                                            convertirHexAFecha(
                                                listFTIME[i].substring(
                                                    0,
                                                    8
                                                )
                                            )
                                        } "
                                    )
                                    listaTemporalFINALTIME.add(listFTIME[i])
                                }
                                /*    listaTemporalFINALTIME.map {
                                        val hexString = it!!.substring(0, 8)
                                        println(" $it ${convertirHexAFecha(hexString)}")
                                    }*/
                                Log.d(
                                    "CONTEOFINAL",
                                    "listFTIME ${listFTIME.size} listaTem ${listaTemporalFINALTIME.size}"
                                )
                                callback.getInfo(listaTemporalFINALTIME)

                            }

                            2 -> {
                                FINALLISTA.map {
                                    Log.d("FINALLISTA", "FINALLISTA ->>>>>>>> $it")
                                }

                                var listD: MutableList<String>? = ArrayList()
                                for (item in FINALLISTA) {
                                    if (item!!.length >= 10) {
                                        listD!!.add(item!!)
                                    }
                                }
                                Log.d(
                                    "FFFFFFFFFFFFFFFFF",
                                    "Hay $count elemento con EVENT_TYPE == \"04\""
                                )
                                callback.getInfo(listD)

                            }

                            else -> {
                                Log.d(
                                    "FFFFFFFFFFFFFFFFF",
                                    "Hay $count elemento con EVENT_TYPE == \"04\""
                                )
                                var listD: MutableList<String>? = ArrayList()
                                for (item in FINALLISTA) {
                                    if (item!!.length >= 10) {
                                        listD!!.add(item!!)
                                    }
                                }
                                Log.d(
                                    "FFFFFFFFFFFFFFFFF",
                                    "Hay $count elemento con EVENT_TYPE == \"04\""
                                )
                                callback.getInfo(listD)
                            }
                        }


                        /* FINALLISTA.map {
                             Log.d("FINALLISTA", "FINALLISTA ->>>>>>>> $it")
                         }*/

                        var listD: MutableList<String>? = ArrayList()
                        for (item in FINALLISTA) {
                            if (item!!.length >= 10) {
                                listD!!.add(item!!)
                            }
                        }
                        //  callback.getInfo(listD as MutableList<String>?)
                        FINALLISTA?.let {
                            if (it.size >= 22) {

                            }

                        }

                        callback.onProgress("Finalizado Tiempo")
                        callback.onSuccess(true)
                    }
                } else {
                    Log.d(
                        "FFFFFFFFFFFFFFFFF",
                        "registros $registros"
                    )
                    callback.onProgress("Finalizado Tiempo")
                    callback.onError("Lista vacia")
                    callback.onSuccess(false)
                }
                //   }catch (Excep : Exception){}

            } else {
                callback.onProgress("Finalizado Tiempo")
                callback.onError("desconectado")
                callback.onSuccess(false)
            }

        }

        override fun onPreExecute() {

            getInfoList()?.clear()
            callback.onProgress("Iniciando Tiempo")
            BanderaTiempo = true
            //createProgressDialog("Obteniendo primera comunicación...");
        }

    }


    inner class MyAsyncTaskResetMemory(private val callback: MyCallback) :
        AsyncTask<Int?, Int?, MutableList<String>>() {
        override fun doInBackground(vararg params: Int?): MutableList<String>? {

            var listDataGT: MutableList<String>? = ArrayList()
            callback.onProgress("Realizando")
            bluetoothLeService = bluetoothServices.bluetoothLeService()
            Thread.sleep(500)
            bluetoothServices!!.sendCommand("handshake", "4021")
            Thread.sleep(500)
            for (i in 1..2) {
                Thread.sleep(500)
                listDataGT!!.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
            }
            return listDataGT


        }

        override fun onPostExecute(result: MutableList<String>?) {
            if (result.isNullOrEmpty()) {
                callback.onError("ErrorHandShake")
            } else {

                Thread.sleep(1000)

                if (bluetoothLeService!!.sendFirstComando("4054")) { //reset de memoria 0x4054
                    Thread.sleep(200)
                    Log.d("", "dataChecksum total:7")
                    callback.onSuccess(true)
                    //  return "ok"
                } else {
                    Log.d("", "dataChecksum total:8")
                    callback.onError("NoResetMemory")
                }
            }
            callback.onProgress("Finalizado")

        }

        override fun onPreExecute() {
            callback.onProgress("Iniciando")
        }
    }

    private inner class MyAsyncTaskResetMemoryPRUEBA(private val callback: MyCallback) :
        AsyncTask<Int?, Int?, MutableList<String>>() {
        val mac = sp!!.getString("mac", "")
        override fun doInBackground(vararg params: Int?): MutableList<String>? {

            var listDataGT: MutableList<String>? = ArrayList()
            val s = sp?.getBoolean("isconnected", false)
            callback.onProgress("Realizando")
            if (s == true) {

                bluetoothLeService = bluetoothServices.bluetoothLeService()
                Thread.sleep(500)
                bluetoothServices.sendCommand("handshake", "4021")
                Thread.sleep(500)
                for (i in 1..2) {
                    Thread.sleep(500)
                    bluetoothLeService?.dataFromBroadcastUpdateString?.let { listDataGT!!.add(it) }
                }
            }
            return listDataGT


        }

        override fun onPostExecute(result: MutableList<String>?) {
            Log.d("reset de memoria", "result $result")
            if (result.isNullOrEmpty()) {
                val s = sp?.getBoolean("isconnected", false)
                if (s == false) {
                    callback.onError("Desconectado")
                } else
                    callback.onError("Desconectado")
            } else {
                val s = sp?.getBoolean("isconnected", false)

                if (s == true) {
                    Thread.sleep(1000)
                    MyAsyncTaskSendDateHour(object : MyCallback {

                        override fun onSuccess(result: Boolean): Boolean {
                            Log.d("MyAsyncTaskSendDateHour", "resultado $result")

                            return result
                        }

                        override fun onError(error: String) {
                            Log.d("MyAsyncTaskSendDateHour", "error $error")
                        }

                        override fun getInfo(data: MutableList<String>?) {

                            //  GlobalTools.checkChecksum(data)
                            data?.map { Log.d("MyAsyncTaskSendDateHour", it + "  ") }
                        }


                        override fun onProgress(progress: String): String {
                            when (progress) {
                                "Iniciando" -> {

                                }

                                "Realizando" -> {


                                }

                                "Finalizado" -> {

                                }

                                else -> {

                                }
                            }

                            //    Log.d("MyAsyncTaskGeLogger", "progress ${progress}")
                            return progress
                        }

                    }).execute()

                    val handler = Handler()
                    handler.postDelayed({

                        bluetoothLeService = bluetoothServices.bluetoothLeService()
                        Thread.sleep(500)
                        bluetoothServices.sendCommand("handshake", "4021")
                        Thread.sleep(500)
                        if (bluetoothLeService!!.sendFirstComando("4054")) { //reset de memoria 0x4054
                            Thread.sleep(200)
                            Log.d("", "dataChecksum total:7")

                            Thread.sleep(10000)
                            /*   val handler = Handler()
                               handler.postDelayed({*/
                            /*                MyAsyncTaskSendDateHour(object : ConexionTrefp.MyCallback {

                                                override fun onSuccess(result: Boolean): Boolean {
                                                    Log.d("MyAsyncTaskSendDateHour", "resultado $result")

                                                    return result
                                                }

                                                override fun onError(error: String) {
                                                    Log.d("MyAsyncTaskSendDateHour", "error $error")
                                                }

                                                override fun getInfo(data: MutableList<String>?) {

                                                    //  GlobalTools.checkChecksum(data)
                                                    data?.map { Log.d("MyAsyncTaskSendDateHour", it + "  ") }
                                                }


                                                override fun onProgress(progress: String): String {
                                                    when (progress) {
                                                        "Iniciando" -> {

                                                        }
                                                        "Realizando" -> {


                                                        }
                                                        "Finalizado" -> {

                                                        }
                                                        else -> {

                                                        }
                                                    }

                                                //    Log.d("MyAsyncTaskGeLogger", "progress ${progress}")
                                                    return progress
                                                }

                                            }).execute()
                */

                            Thread.sleep(5000)
                            callback.onSuccess(true)
                            //  return "ok"
                            /*  Thread.sleep(10000)
                          val handler = Handler()
                          handler.postDelayed({
                              val task =

                                  MyAsyncTaskConnTest(mac!!,//"00:E4:4C:20:A2:23", //"00:E4:4C:21:7A:F3",//"00:E4:4C:21:7A:F3","",//"",
                                      //   "IMBERA_RUTA_FRIA",
                                      ///"00:E4:4C:00:92:E4",//"00:E4:4C:21:7A:F3","00:E4:4C:21:76:9C",//"00:E4:4C:21:76:9C",////"3C:A5:51:94:BF:A5",//"00:E4:4C:20:A5:7D",/*"00:E4:4C:21:76:9C",*///"00:E4:4C:21:27:BC", //
                                      object : ConexionTrefp.MyCallback {
                                          override fun onSuccess(result: Boolean): Boolean {

                                              Log.d("conexionTrefp2.MyAsyncTask", result.toString())
                                              return result
                                          }

                                          override fun onError(error: String) {
                                              // manejar error
                                              Log.d("conexionTrefp2.MyAsyncTask", error.toString())
                                          }

                                          override fun getInfo(data: MutableList<String>?) {

                                          }

                                          override fun onProgress(progress: String): String {
                                              Log.d("conexionTrefp2.MyAsyncTask", "progress ${progress}")
                                             when (progress)
                                              {
                                                  "Finalizado" ->{
                                                      /*MyAsyncTaskSendDateHour(object : ConexionTrefp.MyCallback {

                                                          override fun onSuccess(result: Boolean): Boolean {
                                                              Log.d("MyAsyncTaskSendDateHour", "resultado $result")

                                                              return result
                                                          }

                                                          override fun onError(error: String) {
                                                              Log.d("MyAsyncTaskSendDateHour", "error $error")
                                                          }

                                                          override fun getInfo(data: MutableList<String>?) {

                                                              //  GlobalTools.checkChecksum(data)
                                                              data?.map { Log.d("MyAsyncTaskSendDateHour", it + "  ") }
                                                          }


                                                          override fun onProgress(progress: String): String {
                                                              when (progress) {
                                                                  "Iniciando" -> {

                                                                  }
                                                                  "Realizando" -> {


                                                                  }
                                                                  "Finalizado" -> {

                                                                  }
                                                                  else -> {

                                                                  }
                                                              }

                                                              Log.d("MyAsyncTaskGeLogger", "progress ${progress}")
                                                              return progress
                                                          }

                                                      }).execute()
                                                      */
                                                      bluetoothLeService = bluetoothServices.bluetoothLeService()
                                                      bluetoothLeService!!.sendFirstComando("4021")

                                                      Thread.sleep(1000)

                                                      var HourNow = GetNowDateExa()

                                                      var Command: String? = null
                                                      HourNow?.let {
                                                          var CHECKSUMGEO = mutableListOf<String>()
                                                          CHECKSUMGEO.add("4058")
                                                          CHECKSUMGEO.add(HourNow.uppercase())
                                                          CHECKSUMGEO.add(sumarParesHexadecimales(CHECKSUMGEO))
                                                          val result = CHECKSUMGEO.joinToString("")

                                                          Log.d(
                                                              "MyAsyncTaskSendDateHour",
                                                              "resultado $CHECKSUMGEO sin espacios  $result"
                                                          )
                                                          if (bluetoothLeService!!.sendFirstComando(result)) {

                                                              Thread.sleep(450)
                                                              listData.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                                                              Thread.sleep(450)
                                                              Log.d(
                                                                  "MyAsyncTaskSendDateHour",
                                                                  "->>>>>>>>>>>>>>>>>>>>>>>>${listData[0]}"
                                                              )
                                                              if (listData[0]?.equals("F1 3D") == true) {
                                                                  return "F1 3D"
                                                              } else {
                                                                  callback.onError("F1 3E")
                                                                  return "F1 3E"
                                                              }
                                                          } else Log.d("", "dataChecksum total:8")

                                                      } ?: "sin hora"

                                                      Thread.sleep(1000)
                                                      desconectar()
                                                      callback.onProgress("Finalizado")
                                                  }

                                              }
                                              return progress
                                          }
                                      })

                              task.execute()
                          }, 5000)
                          */

                        } else {
                            Log.d("", "dataChecksum total:8")
                            callback.onError("NoResetMemory")
                        }
                    }, 5000)
                }
            }
            println("-----------------------------------------------------------------------------------------------------------------------------------------------")
            println("-----------------------------------------------------------------------------------------------------------------------------------------------")
            println("-----------------------------------------------------------------------------------------------------------------------------------------------")
            println("-----------------------------------------------------------------------------------------------------------------------------------------------")
            println("-----------------------------------------------------------------------------------------------------------------------------------------------")
            println("-----------------------------------------------------------------------------------------------------------------------------------------------")
            println("-----------------------------------------------------------------------------------------------------------------------------------------------")
            callback.onProgress("Finalizado")

        }

        override fun onPreExecute() {
            callback.onProgress("Iniciando")
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private fun dataFECHA2(
        data: MutableList<String?>, action: String
    ): MutableList<String?> {
        val newData: MutableList<String?> = ArrayList()
        return when (action) {

            "Lectura de datos tipo Tiempo" -> {

                val newData: MutableList<String?> = ArrayList()


                //buffer
                var date: Date
                var i = 0
                //Log.d("PAQUETE",":"+data.get())
                val timeStampOriginal = GetRealDataFromHexaImbera.getDecimal(
                    data[data.size - 1]!!.substring(
                        0,
                        8
                    )
                ).toLong() //getDecimal(data.get(data.size()-1).substring(0,8));
                val unixTime = System.currentTimeMillis() / 1000
                val diferencialTimeStamp = unixTime - timeStampOriginal
                do {
                    val instant = Instant.ofEpochSecond(
                        GetRealDataFromHexaImbera.getDecimal(
                            data[i]!!.substring(0, 8)
                        ) + diferencialTimeStamp
                    )
                    date = Date.from(instant)
                    val fechaHoraExadecimal =
                        BigInteger.valueOf(date.time / 1000).toString(16).padStart(8, '0')
                    val resul = replaceFirstEightChars(fechaHoraExadecimal, data[i]!!, 8)


                    newData.add(resul.toString().uppercase())


                    //  newData.add(GetRealDataFromHexaImbera.getDecimal(data[i]!!.substring(16)).toString()) //decimales sin punto
                    i++
                } while (i < data.size)

                newData

            }

            "Lectura de datos tipo Evento" -> {
                //buffer
                var date: Date
                var dateEnd: Date
                var i = 0
                //Log.d("PAQUETE",":"+data.get())
                val timeStampOriginal = GetRealDataFromHexaImbera.getDecimal(
                    data[data.size - 1]!!.substring(
                        0,
                        8
                    )
                ).toLong() //getDecimal(data.get(data.size()-1).substring(0,8));
                val timeStampOriginalEnd = GetRealDataFromHexaImbera.getDecimal(
                    data[data.size - 1]!!.substring(
                        8,
                        16
                    )
                ).toLong() //getDecimal(data.get(data.size()-1).substring(0,8));

                val unixTime = System.currentTimeMillis() / 1000
                val diferencialTimeStamp = unixTime - timeStampOriginal
                val diferencialTimeStampEnd = unixTime - timeStampOriginalEnd




                do {
                    val instant = Instant.ofEpochSecond(
                        GetRealDataFromHexaImbera.getDecimal(
                            data[i]!!.substring(0, 8)
                        ) + diferencialTimeStamp
                    )
                    val instantEnd = Instant.ofEpochSecond(
                        GetRealDataFromHexaImbera.getDecimal(
                            data[i]!!.substring(8, 16)
                        ) + diferencialTimeStamp
                    )
                    date = Date.from(instant)
                    dateEnd = Date.from(instant)
                    val fechaHoraExadecimal =
                        BigInteger.valueOf(date.time / 1000).toString(16).padStart(8, '0')
                    val fechaHoraExadecimalEnd =
                        BigInteger.valueOf(dateEnd.time / 1000).toString(16).padStart(8, '0')
                    val resul //= replaceFirstEightCharsEven(fechaHoraExadecimal, data[i]!!, 8)
                            = replaceFirstEightCharsEven(
                        diferencialTimeStamp.toString(16),
                        diferencialTimeStampEnd.toString(16),
                        data[i]!!
                    )
                    Log.d(
                        "datosResultEVENT",
                        " fechaHoraExadecimal ${fechaHoraExadecimal}  fechaHoraExadecimalEnd $fechaHoraExadecimalEnd   $date  $dateEnd  ${resul}  } "
                    )
                    // val resul = replaceFirstEightChars(fechaHoraExadecimal, data[i]!!, 8)
                    //val resulEnd = replaceFirstEightChars(fechaHoraExadecimalEnd, data[i]!!, 8)
                    val salida = fechaHoraExadecimal + fechaHoraExadecimalEnd + data[i]!!.substring(
                        16,
                        data[i]!!.length
                    )
                    newData.add(salida.toString().uppercase())


                    //  newData.add(GetRealDataFromHexaImbera.getDecimal(data[i]!!.substring(16)).toString()) //decimales sin punto
                    i++
                } while (i < data.size)


                return newData
            }

            else -> {
                ArrayList()
            }
        }
    }


    private fun dataFECHA(
        data: MutableList<String?>,
        action: String

    ): MutableList<String?> {
        return when (action) {
            "Lectura de datos tipo Tiempo" -> {
                val newData: MutableList<String?> = ArrayList()
                //header
                if (data.isEmpty()) {

                    newData.add("nullHandshake")
                } else {
                    if (true) {
                        var date: Date
                        var i = 4
                        //Log.d("PAQUETE",":"+data.get())
                        val timeStampOriginal = GetRealDataFromHexaImbera.getDecimal(
                            data[data.size - 1]!!.substring(
                                0,
                                8
                            )
                        ).toLong()
                        val unixTime = System.currentTimeMillis() / 1000
                        val diferencialTimeStamp = unixTime - timeStampOriginal
                        Log.d(
                            "algoritmoSalida",
                            " unixTime $unixTime -  timeStampOriginal $timeStampOriginal  = $diferencialTimeStamp"
                        )
                        do {
                            val instant = Instant.ofEpochSecond(
                                GetRealDataFromHexaImbera.getDecimal(
                                    data[i]!!.substring(0, 8)
                                ) + diferencialTimeStamp
                            )
                            date = Date.from(instant)
                            val fechaHoraExadecimal =
                                BigInteger.valueOf(date.time / 1000).toString(16).padStart(8, '0')
                            val resul = replaceFirstEightChars(fechaHoraExadecimal, data[i]!!, 8)
                            Log.d(
                                "datosEXA",
                                " dato ${data[i]!!} insta $instant   date ${date.toString()}  fechaHoraExadecimal ${fechaHoraExadecimal}"
                            )
                            newData.add(resul.toString().uppercase()) //decimales sin punto
                            i++
                        } while (i < data.size)
                    }
                }

                newData
            }

            "PRUEBA" -> {
                val newData: MutableList<String?> = ArrayList()

                data?.let { data ->
                    var date: Date
                    var i = 4
                    //Log.d("PAQUETE",":"+data.get())
                    val timeStampOriginal = GetRealDataFromHexaImbera.getDecimal(
                        data[data.size - 1]!!.substring(
                            0,
                            8
                        )
                    ).toLong()
                    val unixTime = System.currentTimeMillis() / 1000
                    val diferencialTimeStamp = unixTime - timeStampOriginal
                    Log.d(
                        "algoritmoSalida",
                        " unixTime $unixTime -  timeStampOriginal $timeStampOriginal  = $diferencialTimeStamp"
                    )
                    data.map {
                        val instant = Instant.ofEpochSecond(
                            GetRealDataFromHexaImbera.getDecimal(
                                it!!.substring(0, 8)
                            ) + diferencialTimeStamp
                        )
                        date = Date.from(instant)
                        val fechaHoraExadecimal =
                            BigInteger.valueOf(date.time / 1000).toString(16).padStart(8, '0')
                        val resul = replaceFirstEightChars(fechaHoraExadecimal, data[i]!!, 8)
                        Log.d(
                            "datosEXA",
                            " dato ${it!!} insta $instant   date ${date.toString()}  fechaHoraExadecimal ${fechaHoraExadecimal}"
                        )
                        Log.d("datosEXASALIDA", "$it")

                        newData.add(resul.toString().uppercase()) //decimales sin punto
                    }
                }
                newData

            }

            else -> {
                ArrayList()
            }
        }
    }

    private fun validarLista(lista: List<String>): List<String> {
        val result = mutableListOf<String>()
        var prev = lista[0].substring(0, 8)
            .toInt(16) // obtener el primer número de la lista y convertirlo a entero
        for (i in 1 until lista.size) {
            val current = lista[i].substring(0, 8)
                .toInt(16) // obtener el número actual de la lista y convertirlo a entero
            if (current < prev) { // verificar si el número actual es menor que el anterior
                break
            }
            prev = current // actualizar el número anterior con el número actual
            result.add(lista[i]) // agregar el número actual a la lista de resultados si es válido
        }
        return result
    }

    private fun GetRealDataPRueba(
        data: MutableList<String?>,
        action: String?

    ): List<String>? {
        //USO SOLO DE LOS DATOS BUFFER IMPORTANTES PARA MOSTRARLOS EN PANTALLA, LAS POSICIONES RESTANTES (HEADER) SON CORRECTAS
        return when (action) {
            "Handshake" -> {
                val newData: MutableList<String> = ArrayList()
                if (data.isEmpty()) {
                    //newData.add(getSameData(data.get(0),"trefpversion"));
                    newData.add("nullHandshake")
                } else {
                    //header
                    //newData.add(data.get(0));
                    newData.add(GetRealDataFromHexaImbera.hexToAscii(data[1]!!))
                    newData.add(GetRealDataFromHexaImbera.getSameData(data[2]!!, action))
                    //newData.add(getSameData(data.get(3), "trefpversion"));
                    Log.d("ASDASDASD", ":" + data[3])
                    Log.d("ASDASDASD", ":" + data[3]!!.substring(0, 2))
                    val stringBuilder = java.lang.StringBuilder()
                    stringBuilder.append(
                        GetRealDataFromHexaImbera.getDecimal(
                            data[3]!!.substring(
                                0,
                                2
                            )
                        )
                    )
                    stringBuilder.append(".")
                    Log.d("VERSION", ":" + data[3]!!.substring(2))
                    val version = GetRealDataFromHexaImbera.getDecimal(data[3]!!.substring(2))
                    if (version < 10) {
                        stringBuilder.append("0$version")
                    } else {
                        stringBuilder.append(
                            GetRealDataFromHexaImbera.getDecimal(
                                data[3]!!.substring(
                                    2
                                )
                            )
                        )
                    }
                    newData.add(stringBuilder.toString())
                    newData.add(
                        GetRealDataFromHexaImbera.getDecimalFloat(data[4]!!).toString()
                    ) // decimales con punto
                }
                newData
            }

            "Lectura de parámetros de operación" -> {
                val newData: MutableList<String> = ArrayList()
                if (data.isEmpty()) {
                    //newData.add(getSameData(data.get(0),"trefpversion"));
                    newData.add("nullHandshake")
                } else {
                    //header
                    newData.add(GetRealDataFromHexaImbera.getSameData(data[0]!!, "trefpversion"))
                    newData.add(GetRealDataFromHexaImbera.getSameData(data[1]!!, action))
                    newData.add(GetRealDataFromHexaImbera.getDecimal(data[2]!!).toString())
                    newData.add(GetRealDataFromHexaImbera.getDecimal(data[3]!!).toString())

                    //buffer
                    var i = 4
                    do {
                        if (i == 42 || i == 17 || i == 18 || i == 19 || i == 20 || i == 21 || i == 13 || i == 14 || i == 15 || i == 16 || i == 43 || i == 27) {
                            newData.add(
                                GetRealDataFromHexaImbera.getDecimal(data[i]!!).toString()
                            ) //decimales sin punto
                        } else {
                            if (i == 4 || i == 6 || i == 7 || i == 8 || i == 12 || i == 11) {
                                //comprobar si es popsitivo
                                val j =
                                    GetRealDataFromHexaImbera.getDecimalFloat(data[i]!!) // decimales con punto
                                if (j > 99.9) {
                                    //Extraccion de temperaturas en decimales
                                    newData.add(GetRealDataFromHexaImbera.getNegativeTemp("FFFF" + data[i]))
                                } else {
                                    newData.add(
                                        GetRealDataFromHexaImbera.getDecimalFloat(data[i]!!)
                                            .toString()
                                    ) // decimales con punto
                                }
                            } else if (i == 22) {
                                //Extraccion opciones segùn bit usado
                                newData.add(
                                    GetRealDataFromHexaImbera.getOptionSpinner(
                                        data[i],
                                        "mododeshielo"
                                    )
                                )
                            } else if (i == 23) {
                                //Extraccion opciones segùn bit usado
                                newData.add(
                                    GetRealDataFromHexaImbera.getOptionSpinner(
                                        data[i],
                                        "funcionesControl"
                                    )
                                )
                            } else if (i == 24) {
                                //Extraccion opciones segùn bit usado
                                newData.add(
                                    GetRealDataFromHexaImbera.getOptionSpinner(
                                        data[i],
                                        "funcionesdeshielo"
                                    )
                                )
                            } else if (i == 25) {
                                //Extraccion opciones segùn bit usado
                                newData.add(
                                    GetRealDataFromHexaImbera.getOptionSpinner(
                                        data[i],
                                        "funcionesventilador"
                                    )
                                )
                            } else if (i == 26) {
                                //Extraccion opciones segùn bit usado
                                newData.add(
                                    GetRealDataFromHexaImbera.getOptionSpinner(
                                        data[i],
                                        "funcionesvoltaje"
                                    )
                                )
                            } else {
                                newData.add(
                                    GetRealDataFromHexaImbera.getDecimalFloat(data[i]!!).toString()
                                ) // decimales con punto
                            }
                        }
                        i++
                    } while (i < data.size)
                }
                newData
            }

            "Lectura de datos tipo Tiempo" -> {
                val newData: MutableList<String> = ArrayList()
                val header: MutableList<String> = ArrayList()
                //header
                if (data.isEmpty()) {
                    //newData.add(getSameData(data.get(0),"trefpversion"));
                    newData.add("nullHandshake")
                } else {
                    if (true) {
                        //nuevo logger, diferente división de información
                        //header
                        header.add(GetRealDataFromHexaImbera.getSameData(data[0]!!, "trefpversion"))
                        header.add(GetRealDataFromHexaImbera.getDecimal(data[1]!!).toString())
                        header.add(GetRealDataFromHexaImbera.getSameData(data[2]!!, action))
                        header.add(GetRealDataFromHexaImbera.getSameData(data[3]!!, action))

                        //buffer
                        var date: Date
                        var i = 4
                        //Log.d("PAQUETE",":"+data.get())
                        val timeStampOriginal = GetRealDataFromHexaImbera.getDecimal(
                            data[data.size - 1]!!.substring(
                                0,
                                8
                            )
                        ).toLong() //getDecimal(data.get(data.size()-1).substring(0,8));
                        val unixTime = System.currentTimeMillis() / 1000
                        val diferencialTimeStamp = unixTime - timeStampOriginal
                        do {
                            val instant = Instant.ofEpochSecond(
                                GetRealDataFromHexaImbera.getDecimal(
                                    data[i]!!.substring(0, 8)
                                ) + diferencialTimeStamp
                            )


                            date = Date.from(instant)
                            val fechaHoraExadecimal =
                                BigInteger.valueOf(date.time / 1000).toString(16).padStart(8, '0')

                            Log.d(
                                "datosEXA",
                                " insta $instant   date ${date.toString()}  fechaHoraExadecimal ${fechaHoraExadecimal}"
                            )
                            newData.add(date.toString()) //decimales sin punto
                            //decision de temperaturas positivas y negativas
                            var numf =
                                GetRealDataFromHexaImbera.getDecimalFloat(
                                    data[i]!!.substring(
                                        8,
                                        12
                                    )
                                )
                            var num = numf.toInt()
                            if (num < 99.99) {
                                newData.add(
                                    GetRealDataFromHexaImbera.getDecimalFloat(
                                        data[i]!!.substring(
                                            8,
                                            12
                                        )
                                    ).toString()
                                ) //decimales con punto //get temp positivo
                            } else if (num > 99.99) {
                                newData.add(
                                    GetRealDataFromHexaImbera.getNegativeTemp(
                                        "FFFF" + data[i]!!.substring(
                                            8,
                                            12
                                        )
                                    )
                                ) //get negativos
                            } else { //Es 0 cero
                                newData.add("0000") //get negativos
                            }
                            numf =
                                GetRealDataFromHexaImbera.getDecimalFloat(
                                    data[i]!!.substring(
                                        12,
                                        16
                                    )
                                )
                            num = numf.toInt()
                            if (num < 99.99) {
                                newData.add(
                                    GetRealDataFromHexaImbera.getDecimalFloat(
                                        data[i]!!.substring(
                                            12,
                                            16
                                        )
                                    ).toString()
                                ) //decimales con punto //get temp positivo
                            } else if (num > 99.99) {
                                //newData.add(getNegativeTemp("FFFF"+data.get(i).substring(22,26))); //get negativos
                                newData.add(
                                    GetRealDataFromHexaImbera.getNegativeTemp(
                                        "FFFF" + data[i]!!.substring(
                                            12,
                                            16
                                        )
                                    )
                                ) //get negativos
                            } else { //Es 0 cero
                                newData.add("0000") //get negativos
                            }
                            newData.add(
                                GetRealDataFromHexaImbera.getDecimal(data[i]!!.substring(16))
                                    .toString()
                            ) //decimales sin punto
                            i++
                        } while (i < data.size)
                    } else {
                        //header
                        header.add(GetRealDataFromHexaImbera.getSameData(data[0]!!, "trefpversion"))
                        header.add(GetRealDataFromHexaImbera.getDecimal(data[1]!!).toString())
                        header.add(GetRealDataFromHexaImbera.getSameData(data[2]!!, action))
                        header.add(GetRealDataFromHexaImbera.getSameData(data[3]!!, action))

                        //buffer
                        var date: Date
                        var i = 4
                        val timeStampOriginal = GetRealDataFromHexaImbera.getDecimal(
                            data[data.size - 1]!!.substring(
                                0,
                                8
                            )
                        ).toLong() //getDecimal(data.get(data.size()-1).substring(0,8));
                        val unixTime = System.currentTimeMillis() / 1000
                        val diferencialTimeStamp = unixTime - timeStampOriginal
                        //612F6B42
                        //long f =
                        do {
                            if (i >= data.size) {
                                i = data.size //no interesa el checksum
                            } else {
                                val instant = Instant.ofEpochSecond(
                                    GetRealDataFromHexaImbera.getDecimal(
                                        data[i]!!.substring(0, 8)
                                    ) + diferencialTimeStamp
                                )
                                date = Date.from(instant)

                                Log.d("PruebaDatos", " date $date instant $instant ")

                                newData.add(date.toString()) //decimales sin punto
                                //decision de temperaturas positivas y negativas
                                var numf = GetRealDataFromHexaImbera.getDecimalFloat(
                                    data[i]!!.substring(
                                        8,
                                        12
                                    )
                                )
                                var num = numf.toInt()
                                if (num < 99.99) {
                                    newData.add(
                                        GetRealDataFromHexaImbera.getDecimalFloat(
                                            data[i]!!.substring(
                                                8,
                                                12
                                            )
                                        ).toString()
                                    ) //decimales con punto //get temp positivo
                                } else if (num > 99.99) {
                                    newData.add(
                                        GetRealDataFromHexaImbera.getNegativeTemp(
                                            "FFFF" + data[i]!!.substring(
                                                8,
                                                12
                                            )
                                        )
                                    ) //get negativos
                                } else { //Es 0 cero
                                    newData.add("0000") //get negativos
                                }
                                numf = GetRealDataFromHexaImbera.getDecimalFloat(
                                    data[i]!!.substring(
                                        12,
                                        16
                                    )
                                )
                                num = numf.toInt()
                                if (num < 99.99) {
                                    newData.add(
                                        GetRealDataFromHexaImbera.getDecimalFloat(
                                            data[i]!!.substring(
                                                12,
                                                16
                                            )
                                        ).toString()
                                    ) //decimales con punto //get temp positivo
                                } else if (num > 99.99) {
                                    //newData.add(getNegativeTemp("FFFF"+data.get(i).substring(22,26))); //get negativos
                                    newData.add(
                                        GetRealDataFromHexaImbera.getNegativeTemp(
                                            "FFFF" + data[i]!!.substring(
                                                12,
                                                16
                                            )
                                        )
                                    ) //get negativos
                                } else { //Es 0 cero
                                    newData.add("0000") //get negativos
                                }
                                //newData.add(String.valueOf(getDecimalFloat(data.get(i).substring(8,12)) ));
                                //newData.add(String.valueOf(getDecimalFloat(data.get(i).substring(12,16)) ));
                                newData.add(
                                    GetRealDataFromHexaImbera.getDecimal(
                                        data[i]!!.substring(
                                            16
                                        )
                                    ).toString()
                                ) //decimales sin punto
                                i++

                                //25,349,176
                            }
                        } while (i < data.size)
                    }
                }
                Log.d("realdata", "realdataHeadr:$header")
                Log.d("realdata", "realdata:$newData")
                newData
            }

            else -> {
                ArrayList()
            }
        }
    }


    private fun convertHexUnixTimestamp(hexTimestamp: String): String {
        // Convert the hexadecimal timestamp to a decimal timestamp
        val decimalTimestamp = hexTimestamp.toLong(16)

        // Convert the decimal timestamp to an Instant
        val instant = Instant.ofEpochSecond(decimalTimestamp)

        // Create a DateTimeFormatter to format the date string
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        // Convert the Instant to a ZonedDateTime in your local timezone
        val zonedDateTime = instant.atZone(ZoneId.of("America/Mexico_City"))

        // Format the ZonedDateTime to a human-readable date string
        return formatter.format(zonedDateTime)
    }

    companion object {
        var getHandShake: MutableList<String>? = null
        var resultadoAsyncTask: String? = null
        private const val SCAN_PERIOD: Long = 1000

        private var instance: ConexionTrefp? = null

        @JvmStatic
        fun newInstance(
            context: Context,
            tvconnectionState: TextView?,
            tvfwversion: TextView?
        ): ConexionTrefp {
            if (instance == null) {
                instance = ConexionTrefp(context, tvconnectionState, tvfwversion)
            }
            return instance!!
        }

    }

    override fun getList(): MutableList<String>? {
        TODO("Not yet implemented")
    }

    override fun getLogeer(): MutableList<String>? {
        TODO("Not yet implemented")
    }

    override fun getMacConnect(): MutableList<String>? {
        TODO("Not yet implemented")
        return listMcc ?: "desconectado" as MutableList<String>
    }

    @SuppressLint("MissingPermission")
    private fun obtenerIMEI(): String? {
        val telephonyManager =
            getContext!!.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Hacemos la validación de métodos, ya que el método getDeviceId() ya no se admite para android Oreo en adelante, debemos usar el método getImei()
            Settings.Secure.getString(getContext!!.getContentResolver(), Settings.Secure.ANDROID_ID)
            //return telephonyManager.getImei();
        } else {
            telephonyManager.deviceId
        }
    }


    private fun sortListBLE() {
        /*if (listaDevices.size()>1){
            for (int i=0; i<listaDevices.size(); i++){
                String mac = listaDevices.get(i).getMac();
                for (int j=i+1; j<listaDevices.size(); j++){
                    String mac2 = listaDevices.get(j).getMac();
                    if (mac.equals(mac2)){
                        listaDevices.remove(j);
                        j--;
                    }
                }
            }
        }*/

        // Eliminar elementos duplicados de listaDevices
        val uniqueDevices: MutableList<BLEDevices> = ArrayList<BLEDevices>()
        val uniqueMacs: MutableSet<String> = HashSet()
        for (device in listaDevices!!) {
            // Log.d("PRUEBASNEW",device.mac)
            if (uniqueMacs.add(device.mac)) {
                uniqueDevices.add(device)
            }
        }
        listaDevices!!.clear()
        listaDevices?.clear()
        listaDevices!!.addAll(uniqueDevices)
        listaDevices!!.addAll(uniqueDevices)

        /*     List<BLEDevices> Lista2 = listaDevices.stream().distinct().collect(Collectors.toList());
            listaDevices.clear();
            listaDevices.addAll(Lista2);

        */
    }


    private fun replaceFirstEightChars(str1: String, str2: String, indice: Int): String {
        val newStr1 = str1.padStart(8, '0')
        val newStr2 = newStr1 + str2.substring(indice)
        return newStr2
    }

    private fun replaceFirstEightCharsEven(valor1: String, valor2: String, valor3: String): String {
        val valor1Llenado = valor1.padStart(8, '0')
        val valor2Llenado = valor2.padStart(8, '0')
        val valor3Remplazado = valor1Llenado + valor2Llenado + valor3.substring(16)
        return valor1Llenado + valor2Llenado + valor3Remplazado.substring(16)
    }

    fun convertHexToHumanDate(hexTimestamp: String): String? {
        val unixTimestamp = hexTimestamp.toLong(16)
        val date = Date(unixTimestamp * 1000)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("es", "MX"))
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(date)
    }

    fun convertHexToHumanDateLogger(hexTimestamp: String): String? {
        val unixTimestamp = hexTimestamp.toLong(16)
        val date = Date(unixTimestamp * 1000)

        val dateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale("es", "MX"))
        dateFormat.timeZone = TimeZone.getTimeZone("America/Mexico_City")

        return dateFormat.format(date)
    }

    private fun IteraFUNRegistros(item: Int, result: MutableList<String?>) {
        var w: Long? = null
        // listaTemporalFINAL.add(result[item]!!)
        for (i in item - 1 downTo 1) {

            val fechaHora1 = Date(result[i]!!.substring(0, 8).toLong(16) * 1000)
            val fechaHora2 = Date(result[i - 1]!!.substring(0, 8).toLong(16) * 1000)

            println(" ${result[i]} $i  fechaHora1 $fechaHora1 fechaHora2 $fechaHora2 ")
            if (fechaHora1 >= fechaHora2) {
                w = (result[i]!!.substring(0, 8).toLong(16) * 1000) - (result[i - 1]!!.substring(
                    0,
                    8
                ).toLong(16) * 1000)

                val anterior = (listaTemporalFINAL.last().substring(0, 8).toLong(16) * 1000) - w
                val date = Date(anterior)
                val fechaHoraExadecimal =
                    BigInteger.valueOf(date.time / 1000).toString(16).padStart(8, '0').uppercase()

                listaTemporalFINAL.add(
                    fechaHoraExadecimal + result[i]!!.substring(
                        8,
                        result[i]!!.length
                    )
                )
            } else {
                /*           val w2=  (result[i]!!.substring(0, 8).toLong(16) * 1000) -  (result[i-1]!!.substring(0, 8).toLong(16) * 1000)
                             val anterior2 =  (listaTemporalFINAL.last().substring(0, 8).toLong(16) * 1000) - w2
                             val date2 = Date(anterior2)
                             val fechaHoraExadecimal2 =
                                 BigInteger.valueOf(date2.time / 1000).toString(16).padStart(8, '0').uppercase()
                             listaTemporalFINAL.add(fechaHoraExadecimal2+result[i]!!.substring(8,result[i]!!.length))

             */
                val w2 =
                    (result[i + 2]!!.substring(0, 8).toLong(16) * 1000) - (result[i]!!.substring(
                        0,
                        8
                    ).toLong(16) * 1000)


                val anterior2 = (listaTemporalFINAL.last().substring(0, 8).toLong(16) * 1000) - w2
                val date2 = Date(anterior2)
                val fechaHoraExadecimal2 =
                    BigInteger.valueOf(date2.time / 1000).toString(16).padStart(8, '0').uppercase()
                Log.d(
                    "---------SALIDA",
                    " w ${w} $anterior2   $date2  $fechaHoraExadecimal2  ${result[i]}  ${result[i - 1]} "
                )
                listaTemporalFINAL.add(
                    fechaHoraExadecimal2 + result[i]!!.substring(
                        8,
                        result[i]!!.length
                    )
                )

                //  listaTemporalFINAL.add(result[i]!!)
                // IteraFUNRegistros(i-1, result)

                //  listaTemporalFINAL.add(result[i]!!)
                Log.d("---------SALIDA", "${listaTemporalFINAL.last()} $i")
                val timestamp =
                    (listaTemporalFINAL.last()!!.substring(0, 8).toLong(16) * 1000) - 3600000
                val date = Date(timestamp)

                /********/
                val fechaHoraExadecimal =
                    BigInteger.valueOf(date.time / 1000).toString(16).padStart(8, '0').uppercase()

                listaTemporalFINAL.add(
                    fechaHoraExadecimal + result[i - 1]!!.substring(
                        8,
                        result[i - 1]!!.length
                    )
                )
                println(" ->>>>>>>>>>>>>>>>> valor de i   ${i - 1} ")
                IteraFUNRegistros(i - 1, result)
                break
            }


            ///   println(" ->>>>>>>>>>>>>>>>>  ${w.toString()} ")

        }


    }

    fun convertirHexAFecha(hex: String): String {
        val unixTime = hex.toLong(16)
        val fecha = java.util.Date(unixTime * 1000)
        val sdf = java.text.SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy HH:mm:ss z")
        return sdf.format(fecha)
    }

    ///////////////////////////////////

    private fun GetNowDateExa(): String {
        val currentTimeMillis = System.currentTimeMillis()
        val currentTimeHexNOW = toHexString(currentTimeMillis / 1000)
        println("currentTimeHexNOW $currentTimeHexNOW ")

        return currentTimeHexNOW

    }

    private fun getGEO(callback: (latitude: String, longitude: String) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                getContext!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                getContext!! as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            checkGooglePlayServicesGEO { latitude, longitude ->
                var listalatitude = mutableListOf<String>()
                var listalongitude = mutableListOf<String>()

                listalatitude.add(latitude.toString())
                listalongitude.add(longitude.toString())
                val exalongitude = getData(listalongitude).get(0).uppercase()
                val exalatitude = convertDecimalToHexa(latitude.toDouble())
                Log.d(
                    "MainActivity",
                    "checkGooglePlayServices->>>>> Latitude: ${latitude.toString()} " + " Longitude: $longitude" +
                            " -> exalatitude $exalatitude   ${getDecimalFloat(exalatitude)}   ${
                                convertHexaToDecimalWithTwoDecimals(
                                    exalatitude
                                )
                            } exalongitude $exalongitude  ${getNegativeTempfloat("FFFF${exalongitude}").toFloat()}  "
                )

                callback(exalatitude, exalongitude)
            }

        }
    }


    inner class MyAsyncTaskSendDateHour(private val callback:  ConexionTrefp.MyCallback) :
        AsyncTask<Int?, Int?, String?>() {
        var TIMEUNIX: String? = null

        /* override fun doInBackground(vararg params: Int?): String? {

             var listaNueva: MutableList<String>? = ArrayList()
             var HourNow: String? = null
             callback.onProgress("Realizando")
             val s = ValidaConexion()//sp?.getBoolean("isconnected", false)
             if (s) {

                 try {
                     clearLogger()
                     //  getInfoList()?.clear()
                     listData.clear()
                     //  bluetoothLeService = bluetoothServices.bluetoothLeService()
                     bluetoothLeService?.sendFirstComando("4021")



                     Thread.sleep(500)
 /*
                     bluetoothLeService?.sendFirstComando("405B")
                     Thread.sleep(500)

                     for (num in 0..1) {
                         listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                         Log.d("islistttt2", ":" + listData[num])
                         Thread.sleep(700)
                     }

                     HourNow = toHexString(
                         (((GetNowDateExa().toLong(16).times(1000))?.plus(1000))?.div(1000)) ?: 0
                     ) //   (( GetNowDateExa().toLong(16)?.times(1000) )?.plus(2000)).toString() // GetNowDateExa()
                     Log.d("DataTime", "${GetNowDateExa()}  HourNow $HourNow")

                     if(!listData.isNullOrEmpty()){

                     }

                     if (!listData.isNullOrEmpty()) {
                         listData.let {
                             val sSinEspacios = listData[0]?.replace(" ", "")
                             //   var TIMEUNIX: String? = null
                             TIMEUNIX = if (sSinEspacios?.length!! >= 24) sSinEspacios.substring(
                                 16,
                                 24
                             ) else "612F6B47"
                             Log.d("islistttt2", "TIMEUNIX $TIMEUNIX")
                         }
                     }
                     var TIMEUNIXMIlis = TIMEUNIX?.toLong(16)?.times(1000)
                     var DifTime = (HourNow.toLong(16)?.times(1000))?.minus((TIMEUNIXMIlis!!))


                     Log.d(
                         "DataTime",
                         "DifTime $DifTime   $TIMEUNIX HourNow  ${HourNow}  ${
                             convertirHexAFecha(
                                 HourNow
                             )
                         }  TIMEUNIX $TIMEUNIX ${
                             convertirHexAFecha(TIMEUNIX!!)
                         }"
                     )
 */

          //           Log.d("DataTime", "result $result")
                     if (true) {
                         var Command: String? = null
                         HourNow?.let {
                             var CHECKSUMGEO = mutableListOf<String>()
                             CHECKSUMGEO.add("4058")
                             CHECKSUMGEO.add(GetNowDateExa().uppercase())
                             CHECKSUMGEO.add(sumarParesHexadecimales(CHECKSUMGEO))
                             val result = CHECKSUMGEO.joinToString("")

                             Log.d("DataTime", "result $result")

                             result.let {
                                 if (bluetoothLeService?.sendFirstComando(it) == true) {

                                     Thread.sleep(450)
                                     listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                                     Thread.sleep(450)


                                     getInfoList()?.let {
                                         Log.d(
                                             "DataTime",
                                             "getInfoList 7054 ${getInfoList()?.last()}"
                                         )

                                         if (getInfoList()?.last()?.equals("F13D") == true) {
                                             return "F1 3D"
                                         } else {
                                             callback.onError("F1 3E")
                                             return "F1 3E"
                                         }
                                     } ?: return "F1 3E"

                                 } else Log.d("", "dataChecksum total:8")
                             }
                             return "Sin hora"

                         } ?: "sin hora"
                     } else {
                         return "F1 3D"

                     }


                 } catch (e: InterruptedException) {
                     e.printStackTrace()
                     return "exception"
                 }
             }
             return "DESCONECTADO"
         }
         */
        override fun doInBackground(vararg params: Int?): String? {
            var mensaje = ""
            try {
                callback.onProgress("Realizando")


                clearLogger()
                bluetoothLeService?.sendFirstComando("4021")
                Thread.sleep(700)
                listData.clear()
                listData.add(bluetoothLeService!!.dataFromBroadcastUpdateString)

                FinalListData =GetRealDataFromHexaImbera.convertVersion2(listData as List<String>, "Handshake", "", "") as MutableList<String?>
                listData = GetRealDataFromHexaImbera.GetRealDataVersion2(FinalListData as List<String>,"Handshake","","") as MutableList<String?>

                clearLogger()
                Thread.sleep(500)
                var CHECKSUMGEO = mutableListOf<String>()
                CHECKSUMGEO.add("4058")
                CHECKSUMGEO.add(GetNowDateExa().uppercase())
                CHECKSUMGEO.add(sumarParesHexadecimales(CHECKSUMGEO))
                val result = CHECKSUMGEO.joinToString("")

                Log.d("DataTime", "result $result")

                var name = sp!!.getString("name", "")?.trim()
                var FW = listData[2].toString().toDouble() ?: 0.0
                var HW = if (listData.size==4) 0.0 else listData.get(4).toString().toDouble()
                //    var HW = listData[4].toString().toDouble() ?: 0.0

                listData.map {
                    Log.d("DebugdelaHora"," $it")
                }
                Log.d("DebugdelaHora","FW $FW  HW $HW " )
                mensaje =   when(name)
                {
                    "IMBERA-HEALTH"->{
                        Log.d("DebugdelaHora","Control IMBERA-HEALTH ")
                        var statusBCD = actualizarHoraBCD()
                        Log.d("actualizarHoraBCD", "actualizarHoraBCD result  $statusBCD")
                        if (statusBCD) "F1 3D" else  "F1 3E"
                    }
                    "CEB_IN"->{

                        if (FW >= 2.0  && HW >= 1.1 ){
                            Log.d("DebugdelaHora","Control CEB_IN >= 2.0  && HW >= 1.1 " +
                                    " ")
                            clearLogger()
                            var statusBCD = actualizarHoraBCD()
                            Log.d("actualizarHoraBCD", "actualizarHoraBCD result  $statusBCD")
                            if (statusBCD) "F1 3D" else  "F1 3E"
                        }else{
                            Log.d("DebugdelaHora","Control CEB_IN diferente a =! 2.0  <1.1 ")
                            clearLogger()
                            bluetoothLeService!!.sendComando(result)
                            Thread.sleep(1000)
                            listData.clear()
                            var lista = getInfoList()
                            if (lista!!.last().replace(" ", "").toUpperCase() == "F13D") {
                                "F1 3D"
                            } else {
                                "F1 3E"
                            }
                        }
                    }
                    else -> {
                        Log.d("DebugdelaHora","Control else ")
                        clearLogger()
                        bluetoothLeService!!.sendComando(result)
                        Thread.sleep(1000)
                        listData.clear()
                        var lista = getInfoList()
                        if (lista!!.last().replace(" ", "").toUpperCase() == "F13D") {
                            "F1 3D"
                        } else {
                            "F1 3E"
                        }
                    }
                }
                /*
                result.let {

                    Log.d(
                        "DataTime",
                        sp!!.getString("name", "")!! + " bool " + sp!!.getString("name", "")!!
                            .contains("IMBERA-HEALTH")
                    )

                    if (name!!.contains("IMBERA-HEALTH")  ) {
                        var statusBCD = actualizarHoraBCD()
                        Log.d("actualizarHoraBCD", "actualizarHoraBCD result  $statusBCD")
                        if (statusBCD) mensaje = "F1 3D" else mensaje = "F1 3E"

                    } else {

                        clearLogger()
                        bluetoothLeService!!.sendComando(it)
                        Thread.sleep(1000)
                        listData.clear()
                        var lista = getInfoList()
                        mensaje = if (lista!!.last().replace(" ", "").toUpperCase() == "F13D") {
                            "F1 3D"
                        } else {
                            "F1 3E"
                        }
                    }
                }
                */

            } catch (exce: java.lang.Exception) {
                mensaje = "error"
            }
            return mensaje
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.d("DataTime", "result ----> $result")
            clearLogger()
            when (result) {

                "F1 3D" -> {
                    //62840A56 -> Your time zone: martes, 17 de mayo de 2022 15:49:26 GMT-05:00
                    bluetoothLeService!!.sendFirstComando("405B")
                    Thread.sleep(700)
                    listData.clear()
                    listData.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                    val s = listData[0]
                    val sSinEspacios = s?.replace(" ", "")
                    val TIMEUNIX = sSinEspacios?.substring(16, 24)
                    var listTIMEUNIX: MutableList<String>? = ArrayList()
                    TIMEUNIX?.let {
                        listTIMEUNIX?.add(it)
                    }

                    callback.getInfo(listTIMEUNIX)
                    callback.onSuccess(true)
                }

                "F1 3E" -> {

                    callback.onSuccess(false)
                    //callback.onError("ErrorCommand")
                }

                "not" -> {
                    callback.onSuccess(false)
                    callback.onError("ErrorCommand")
                }

                "DESCONECTADO" -> {
                    callback.onSuccess(false)
                    callback.onError("DESCONECTADO")
                }

                "Rango" -> {
                    Log.d("DataTime", "ok Rango")
                    callback.onSuccess(true)
                }
                /* else->{
                     callback.onSuccess(false)
                     if (result != null) {
                         callback.onError(result)
                     }
                 }*/
            }
            callback.onProgress("Finalizado")
        }


        override fun onPreExecute() {


            callback.onProgress("Iniciando")
        }


    }
    inner class MyAsyncTaskGetgeolocalización(private val callback: MyCallback) :
        AsyncTask<Int?, Int?, String?>() {


        override fun doInBackground(vararg params: Int?): String? {
            var listaNueva: MutableList<String>? = ArrayList()
            var HourNow: String? = null
            callback.onProgress("Realizando")
            listData.clear()
            if (sp?.getString("ACTION_GATT_CONNECTED", "").equals("CONNECTED") == true) {

                try {
                    bluetoothLeService = bluetoothServices.bluetoothLeService()
                    bluetoothLeService!!.sendFirstComando("4021")

                    Thread.sleep(1000)
                    /*   getGEO{exalatitude,exalongitude ->

                           Log.d("MainActivity","conexionTrefp2.getGEO -> exalatitude $exalatitude ,exalongitude $exalongitude")
                           var CHECKSUMGEO = mutableListOf<String>()
                           CHECKSUMGEO.add(exalatitude)
                           CHECKSUMGEO.add(exalongitude)

                           Log.d("MainActivity", "CHECKSUMGEO-> ${CHECKSUMGEO.toString()} CKS ${sumarParesHexadecimales(CHECKSUMGEO)}")
                       }
                       */
                    var Command: String? = null

                    bluetoothLeService!!.sendFirstComando("405A")
                    Thread.sleep(700)
                    for (num in 0..1) {
                        listData.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                        Log.d("islistttt2", ":" + listData[num])
                        Thread.sleep(700)
                    }


                    /*  HourNow?.let {
                          var CHECKSUMGEO = mutableListOf<String>()
                          CHECKSUMGEO.add("4058")
                          CHECKSUMGEO.add(HourNow.uppercase())
                          CHECKSUMGEO.add(sumarParesHexadecimales(CHECKSUMGEO))
                          val result = CHECKSUMGEO.joinToString("")

                          Log.d("MyAsyncTaskSendDateHour", "resultado $CHECKSUMGEO sin espacios  $result" )
                          /*  if (bluetoothLeService!!.sendFirstComando(result)) {
                                Thread.sleep(200)
                                Log.d("", "dataChecksum total:7")
                                Thread.sleep(450)
                                listData.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                                Thread.sleep(450)
                                if (listData[0]?.equals("F13D") ==true ){
                                    return "ok"
                                }else
                                {
                                    callback.onError("F13E")
                                    return "F13E"
                                }
                            } else Log.d("", "dataChecksum total:8")
                            */
                      } ?: "sin hora"
  */
                    return "sin hora"
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    return "exception"
                }
            }
            return "DESCONECTADO"
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            when (result) {

                "F13D" -> {
                    callback.onSuccess(true)
                }

                "F13E" -> {
                    callback.onSuccess(false)
                    //callback.onError("ErrorCommand")
                }

                "not" -> {
                    callback.onSuccess(false)
                    callback.onError("ErrorCommand")
                }

                "DESCONECTADO" -> {
                    callback.onSuccess(false)
                    callback.onError("DESCONECTADO")
                }

                else -> {
                    callback.onSuccess(false)
                    if (result != null) {
                        callback.onError(result)
                    }
                }
            }
            callback.onProgress("Finalizado")
        }


        override fun onPreExecute() {

            getInfoList()?.clear()
            callback.onProgress("Iniciando")
        }


    }


    inner class MyAsyncTaskGetHour(private val callback: MyCallback) :
        AsyncTask<Int?, Int?, String?>() {


        override fun doInBackground(vararg params: Int?): String? {
            var listaNueva: MutableList<String>? = ArrayList()
            var HourNow: String? = null
            callback.onProgress("Realizando")
            getInfoList()?.clear()
            listData.clear()
            val s = sp?.getBoolean("isconnected", false)
            if (s == true) {

                try {
                    bluetoothLeService = bluetoothServices.bluetoothLeService()
                    bluetoothLeService?.sendFirstComando("4021")

                    Thread.sleep(1000)
                    var Command: String? = null

                    clearLogger()
                    bluetoothLeService?.sendFirstComando("405B")
                    Thread.sleep(800)
                    listData = getInfoList() as MutableList<String?>

                    if (!listData.isNullOrEmpty()) {
                        listData?.let {

                            val s = listData[0]
                            val sSinEspacios = s?.replace(" ", "")
                            Log.d(
                                "MyAsyncTaskGetHourLog",
                                "sSinEspacios $sSinEspacios listData $listData"
                            )
                            if (sSinEspacios.toString().length >= 24) {
                                val Timestamp = listOf(sSinEspacios?.substring(16, 24))
                                Log.d("MyAsyncTaskGetHourLog", "Timestamp $Timestamp")
                                callback.getInfo(Timestamp as MutableList<String>)
                            }
                        }
                        return "F1 3D"
                    } else {
                        return "sin hora"
                    }


                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    return "exception"
                }
            } else {
                return "DESCONECTADO"
            }

        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            when (result!!.uppercase()) {

                "F1 3D" -> {
                    callback.onSuccess(true)
                }

                "F1 3E" -> {
                    callback.onSuccess(false)
                    //callback.onError("ErrorCommand")
                }

                "not" -> {
                    callback.onSuccess(false)
                    callback.onError("ErrorCommand")
                }

                "DESCONECTADO" -> {
                    callback.onSuccess(false)
                    callback.onError("DESCONECTADO")
                }
                /*  else->{
                      callback.onSuccess(false)
                      if (result != null) {
                          callback.onError(result)
                      }
                  }*/
            }
            callback.onProgress("Finalizado")
        }


        override fun onPreExecute() {

            getInfoList()?.clear()
            callback.onProgress("Iniciando")
        }


    }


    inner class MyAsyncTaskGetXY(private val callback: MyCallback) :
        AsyncTask<Int?, Int?, String?>() {


        override fun doInBackground(vararg params: Int?): String? {
            var listaNueva: MutableList<String>? = ArrayList()
            var HourNow: String? = null
            callback.onProgress("Realizando")
            getInfoList()?.clear()
            listData.clear()
            val s = ValidaConexion()//sp?.getBoolean("isconnected", false)
            if (s == true) {

                try {
                    bluetoothLeService = bluetoothServices.bluetoothLeService()
                    bluetoothLeService?.sendFirstComando("4021")

                    Thread.sleep(1000)
                    /*   getGEO{exalatitude,exalongitude ->

                           Log.d("MainActivity","conexionTrefp2.getGEO -> exalatitude $exalatitude ,exalongitude $exalongitude")
                           var CHECKSUMGEO = mutableListOf<String>()
                           CHECKSUMGEO.add(exalatitude)
                           CHECKSUMGEO.add(exalongitude)

                           Log.d("MainActivity", "CHECKSUMGEO-> ${CHECKSUMGEO.toString()} CKS ${sumarParesHexadecimales(CHECKSUMGEO)}")
                       }
                       */
                    var Command: String? = null
                    Log.d("MyAsyncTaskGetHour", ": $bluetoothLeService ")
                    getInfoList()?.clear()
                    bluetoothLeService?.sendFirstComando("405A")
                    Thread.sleep(400)
                    for (num in 0..2) {
                        listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                        Log.d("islistttt2", ":" + listData[num])
                        Thread.sleep(700)
                    }
                    if (!listData.isNullOrEmpty()) {
                        listData?.let {

                            val s = listData[0]
                            val sSinEspacios = s?.replace(" ", "")
                            Log.d(
                                "islistttt2",
                                ": Log.d(\"islistttt2\", \":\" + listData[num])" + getInfoList() + " listData[0] -> ${listData[0]}  sSinEspacios $sSinEspacios"
                            )
                            /*val CHSK = sSinEspacios!!.substring(
                                (sSinEspacios!!.length) - 8,
                                sSinEspacios!!.length
                            )
                            */
                            var LONGITUD = sSinEspacios!!.substring(
                                sSinEspacios.length - 12,
                                sSinEspacios.length - 8
                            )
                            var LATITUD = sSinEspacios!!.substring(
                                sSinEspacios.length - 16,
                                sSinEspacios.length - 12
                            )
                            //   val LONGITUD = sSinEspacios!!.substring(24, 32)
                            //    val LATITUD = sSinEspacios!!.substring(16, 24)
                            val TIMEUNIX = sSinEspacios
                            var listTIMEUNIX: MutableList<String>? = ArrayList()
                            /*TIMEUNIX?.let {
                                listTIMEUNIX?.add(it)
                            }*/
                            listTIMEUNIX?.add(LATITUD)
                            listTIMEUNIX?.add(LONGITUD)



                            callback.getInfo(listTIMEUNIX)
                        }

                        return "F1 3D"
                    } else {
                        return "sin hora"
                    }

                    //  callback.getInfo()

                    /*  HourNow?.let {
                          var CHECKSUMGEO = mutableListOf<String>()
                          CHECKSUMGEO.add("4058")
                          CHECKSUMGEO.add(HourNow.uppercase())
                          CHECKSUMGEO.add(sumarParesHexadecimales(CHECKSUMGEO))
                          val result = CHECKSUMGEO.joinToString("")

                          Log.d("MyAsyncTaskSendDateHour", "resultado $CHECKSUMGEO sin espacios  $result" )
                          /*  if (bluetoothLeService!!.sendFirstComando(result)) {
                                Thread.sleep(200)
                                Log.d("", "dataChecksum total:7")
                                Thread.sleep(450)
                                listData.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                                Thread.sleep(450)
                                if (listData[0]?.equals("F13D") ==true ){
                                    return "ok"
                                }else
                                {
                                    callback.onError("F13E")
                                    return "F13E"
                                }
                            } else Log.d("", "dataChecksum total:8")
                            */
                      } ?: "sin hora"
  */

                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    return "exception"
                }
            } else {
                return "DESCONECTADO"
            }

        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.d("MyAsyncTaskGetHour", "MyAsyncTaskGetHour ----> $result")
            when (result!!.uppercase()) {

                "F13D" -> {
                    callback.onSuccess(true)
                }

                "F13E" -> {
                    callback.onSuccess(false)
                    //callback.onError("ErrorCommand")
                }

                "not" -> {
                    callback.onSuccess(false)
                    callback.onError("ErrorCommand")
                }

                "DESCONECTADO" -> {
                    callback.onSuccess(false)
                    callback.onError("DESCONECTADO")
                }
                /*  else->{
                      callback.onSuccess(false)
                      if (result != null) {
                          callback.onError(result)
                      }
                  }*/
            }
            callback.onProgress("Finalizado")
        }


        override fun onPreExecute() {

            getInfoList()?.clear()
            callback.onProgress("Iniciando")
        }


    }

    inner class MyAsyncTaskGetXYNEW(private val callback: MyCallback) :
        AsyncTask<Int?, Int?, String?>() {


        override fun doInBackground(vararg params: Int?): String? {
            var listaNueva: MutableList<String>? = ArrayList()
            var HourNow: String? = null
            callback.onProgress("Realizando")
            getInfoList()?.clear()
            listData.clear()
            val s = ValidaConexion()//sp?.getBoolean("isconnected", false)
            if (s) {

                try {
                    bluetoothLeService = bluetoothServices.bluetoothLeService()
                    bluetoothLeService?.sendFirstComando("4021")

                    Thread.sleep(1000)

                    var Command: String? = null

                    getInfoList()?.clear()
                    bluetoothLeService?.sendFirstComando("405A")
                    Thread.sleep(400)
                    for (num in 0..2) {
                        listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)

                        Thread.sleep(700)
                    }
                    if (!listData.isNullOrEmpty()) {
                        listData?.let {

                            val s = listData[0]
                            val sSinEspacios = s?.replace(" ", "")

                            val LONGITUD = try {
                                sSinEspacios!!.substring(24, 32) ?: "0"
                            } catch (Excep: Exception) {
                                "0"
                            }
                            val LATITUD = try {
                                sSinEspacios!!.substring(16, 24) ?: "0"
                            } catch (Excep: Exception) {
                                "0"
                            }
                            val TIMEUNIX = sSinEspacios
                            var listTIMEUNIX: MutableList<String>? = ArrayList()
                            /*TIMEUNIX?.let {
                                listTIMEUNIX?.add(it)
                            }*/
                            listTIMEUNIX?.add(LATITUD)
                            listTIMEUNIX?.add(LONGITUD)



                            callback.getInfo(listTIMEUNIX)
                        }

                        return "F1 3D"
                    } else {
                        return "sin hora"
                    }


                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    return "exception"
                }
            } else {
                return "DESCONECTADO"
            }

        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.d("MyAsyncTaskGetXYNEW", "MyAsyncTaskGetHour ----> $result")
            var s = result!!.replace(" ", "")
            when (s!!.uppercase()) {

                "F13D" -> {
                    callback.onSuccess(true)
                }

                "F13E" -> {
                    callback.onSuccess(false)
                    //callback.onError("ErrorCommand")
                }

                "not" -> {
                    callback.onSuccess(false)
                    callback.onError("ErrorCommand")
                }

                "DESCONECTADO" -> {
                    callback.onSuccess(false)
                    callback.onError("DESCONECTADO")
                }
                /*  else->{
                      callback.onSuccess(false)
                      if (result != null) {
                          callback.onError(result)
                      }
                  }*/
            }
            callback.onProgress("Finalizado")
        }


        override fun onPreExecute() {

            getInfoList()?.clear()
            callback.onProgress("Iniciando")
        }


    }

    private inner class MyAsyncTaskSendXYVersionAnterior(
        Latitud: Double,
        Longitud: Double,
        private val callback: MyCallback
    ) :
        AsyncTask<Int?, Int?, String?>() {
        var latitude = Latitud
        var longitude = Longitud
        var ControlLatitud: String? = ""//ControlLatitud
        var ControlLongitud: String? = ""//ControlLongitud
        var resultReturn: String? = ""
        override fun doInBackground(vararg params: Int?): String? {
            var listaNueva: MutableList<String>? = ArrayList()
            var HourNow: String? = null
            callback.onProgress("Realizando")
            val s = ValidaConexion()// sp?.getBoolean("isconnected", false)
            if (s) {
                try {
                    bluetoothLeService = bluetoothServices.bluetoothLeService()
                    bluetoothLeService?.sendFirstComando("4021")
                    Thread.sleep(1000)
                    Log.d("islistttt2", ":bluetoothLeService $bluetoothLeService")
                    getInfoList()?.clear()
                    listData.clear()
                    bluetoothLeService?.sendFirstComando("405A")
                    Thread.sleep(400)
                    for (num in 0..2) {
                        listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                        Log.d("islistttt2", ":" + listData[num] + "  ")
                        Thread.sleep(700)
                    }
                    listData.let {
                        val s = listData[0]
                        val sSinEspacios = s?.replace(" ", "")
                        Log.d(
                            "islistttt2",
                            ": Log.d(\"islistttt2\", \":\" + listData[num])" + getInfoList() + " listData[0] -> ${listData[0]}  sSinEspacios $sSinEspacios"
                        )
                        //  ControlLatitud = sSinEspacios!!.substring(8, 12)//.toFloat()
                        //ControlLongitud = sSinEspacios!!.substring(4, 8)//.toString().toFloat()
                        ControlLatitud = sSinEspacios!!.substring(
                            sSinEspacios.length - 16,
                            sSinEspacios.length - 12
                        )
                        ControlLongitud = sSinEspacios!!.substring(
                            sSinEspacios.length - 12,
                            sSinEspacios.length - 8
                        )//.toString().toFloat()
                        val LONGITUD = sSinEspacios!!.substring(24, 32)
                        val LATITUD = sSinEspacios!!.substring(16, 24)
                    }
                    Thread.sleep(800)
                    var SalidaLATITUDZ = ""
                    ControlLatitud?.let { it1 ->
                        Log.d(
                            "checkGooglePlayServices", "LATITUDZ $it1"
                        )
                        if (it1.first().uppercase() == "F" || it1.first().uppercase().equals("D"))
                        // conexionTrefp2. getDecimalFloat(it1)
                        {
                            SalidaLATITUDZ = getNegativeTempfloat4(
                                "FFFF${it1}"
                            )

                        } else {
                            SalidaLATITUDZ = getDecimalFloat2(it1).toString()
                            Log.d(
                                "checkGooglePlayServices",
                                "S ${it1.substring(1).uppercase()}"
                            )
                        }

                    }
                    var SalidaLongitud = ""
                    ControlLongitud?.let { it1 ->
                        Log.d(
                            "checkGooglePlayServices", "LATITUDZ $it1"
                        )
                        if (it1.first().uppercase().equals("F") || it1.first().uppercase()
                                .equals("D")
                        )
                        // conexionTrefp2. getDecimalFloat(it1)
                        {
                            SalidaLongitud = getNegativeTempfloat4(
                                "FFFF${it1}"
                            ).toString()

                        } else {
                            SalidaLongitud = getDecimalFloat2(it1).toString()
                            Log.d(
                                "checkGooglePlayServices",
                                "S ${it1.substring(1).uppercase()}"
                            )
                        }

                    }
                    Thread.sleep(800)
                    isWithinRange(getContext!!,
                        latitude,
                        longitude,
                        SalidaLATITUDZ.toDouble(),
                        SalidaLongitud.toDouble(),
                        object : OnRangeCheckListener {
                            override fun onRangeChecked(isWithinRange: Boolean): Boolean {
                                Log.d(
                                    "checkGooglePlayServices",
                                    "checkGooglePlayServices->>>>> Latitude: $latitude " + " Longitude: $longitude  isWithinRange $isWithinRange"
                                )
                                if (!isWithinRange) {
                                    var listalatitude = mutableListOf<String>()
                                    var listalongitude = mutableListOf<String>()

                                    var La: String? = null
                                    var Lo: String? = null
                                    Log.d(
                                        "checkGooglePlayServices",
                                        "VALORES A ENVIAR latitude ${
                                            String.format(
                                                "%.4f",
                                                latitude
                                            )
                                        }  --- $latitude  ${latitude.toDouble()} ---- longitude ${
                                            String.format(
                                                "%.4f",
                                                longitude
                                            )
                                        } ---$longitude --  "
                                    )

                                    listalatitude.add(/*String.format("%.6f", latitude).toDouble().toString()*/
                                        formatDoubleWithDecimals(
                                            latitude,
                                            4
                                        ).toString()//String.format("%.4f",latitude)
                                    )
                                    listalongitude.add(/*tring.format("%.6f", longitude).toFloat().toString()*/
                                        formatDoubleWithDecimals(
                                            longitude,
                                            4
                                        ).toString() //String.format( "%.4f",longitude)
                                    )

                                    val r = String.format("%.6f", latitude).toDouble()
                                    val exalongitude =
                                        convertDecimalToHexa4(/*(listalongitude)[0].toDouble()*/
                                            formatDoubleWithDecimals(longitude, 4)
                                        )  //String.format("%.4f",longitude).toDouble())

                                    val exalatitude =
                                        convertDecimalToHexa4(/*(listalatitude)[0].toDouble()*/
                                            formatDoubleWithDecimals(latitude, 4)
                                        ) //String.format("%.4f",latitude).toDouble())

                                    Log.d(
                                        "checkGooglePlayServices",
                                        "checkGooglePlayServices->>>>> Latitude: $latitude " + " Longitude: $longitude" +
                                                " -> exalatitude $exalatitude   ${
                                                    getDecimalFloat2(
                                                        exalatitude
                                                    )
                                                } exalongitude $exalongitude  ${
                                                    getNegativeTempfloat4(
                                                        "FFFF${exalongitude}"
                                                    ).toFloat()
                                                }   "
                                    )


                                    bluetoothLeService = bluetoothServices.bluetoothLeService()
                                    bluetoothLeService?.sendFirstComando("4021")

                                    Thread.sleep(500)

                                    HourNow = GetNowDateExa()
                                    getInfoList()!!.clear()
                                    var Command: String? = null
                                    HourNow?.let {
                                        var CHECKSUMGEO = mutableListOf<String>()
                                        CHECKSUMGEO.add("4059")
                                        CHECKSUMGEO.add(
                                            exalatitude.toString().uppercase()
                                                .substring(
                                                    exalatitude.length - 4,
                                                    exalatitude.length
                                                )
                                        )
                                        CHECKSUMGEO.add(
                                            exalongitude.toString().uppercase()
                                                .substring(
                                                    exalongitude.length - 4,
                                                    exalongitude.length
                                                )
                                        )

                                        CHECKSUMGEO.add(sumarParesHexadecimales(CHECKSUMGEO))
                                        var result = CHECKSUMGEO.joinToString("")

                                        Log.d(
                                            "MyAsyncTaskSendXY",
                                            "resultado $CHECKSUMGEO sin espacios  $result"
                                        )
                                        getInfoList()?.clear()
                                        if (bluetoothLeService!!.sendFirstComando(result)) {

                                            Thread.sleep(450)
                                            listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                                            Thread.sleep(450)
                                            Log.d(
                                                "MyAsyncTaskSendXY",
                                                "->>>>>>>>>>>>>>>>>>>>>>>>${listData[0]}  ${getInfoList()}"
                                            )
                                            if (getInfoList()!!.joinToString("").equals("F13D")) {
                                                resultReturn = "F1 3D"
                                            } else {
                                                callback.onError("F1 3E")
                                                resultReturn = "F1 3E"
                                            }
                                        } else {
                                            Log.d("checkGooglePlayServices", "dataChecksum total:8")
                                            resultReturn = "error"
                                        }

                                    } ?: "Sin hora"

                                } else {
                                    val info = mutableListOf<String>()
                                    info.add("Ubicacion dentro del rango")
                                    runOnUiThread {
                                        callback.onSuccess(true)

                                        callback.getInfo(info)
                                    }
                                    resultReturn = "Ubicacion dentro del rango"
                                }
                                return isWithinRange
                            }

                        })

                    return resultReturn //"sin hora"
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    return "exception"
                }
            }
            return "DESCONECTADO"
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.d("checkGooglePlayServices", "----> $result")
            when (result) {

                "F1 3D" -> {
                    //62840A56 -> Your time zone: martes, 17 de mayo de 2022 15:49:26 GMT-05:00
                    /*   getInfoList()!!.clear()
                       bluetoothLeService!!.sendFirstComando("405A")
                       Thread.sleep(700)
                       listData.clear()
                       listData.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                       val s = listData[0]
                       val sSinEspacios = s?.replace(" ", "")
                       val TIMEUNIX = sSinEspacios//?.substring(16, 24)
                       var listTIMEUNIX: MutableList<String>? = ArrayList()
                       TIMEUNIX?.let {
                           listTIMEUNIX?.add(it)
                       }

                       println("------------------------------------------ ${getInfoList()}")
                       callback.getInfo(listTIMEUNIX)
                       callback.onSuccess(true)
                       */
                    val info = mutableListOf<String>()
                    info.add("Ubicacion actualizada")

                    callback.getInfo(info)
                    callback.onSuccess(true)

                }

                "F1 3E" -> {

                    callback.onSuccess(false)
                    //callback.onError("ErrorCommand")
                }

                "not" -> {
                    callback.onSuccess(false)
                    callback.onError("ErrorCommand")
                }

                "DESCONECTADO" -> {
                    callback.onSuccess(false)
                    callback.onError("DESCONECTADO")
                }
                /* else->{
                     callback.onSuccess(false)
                     if (result != null) {
                         callback.onError(result)
                     }
                 }*/
            }
            callback.onProgress("Finalizado")
        }


        override fun onPreExecute() {

            getInfoList()?.clear()
            callback.onProgress("Iniciando")
        }


    }


    inner class MyAsyncTaskSendXYNew(
        Latitud: Double,
        Longitud: Double,
        private val callback: MyCallback
    ) :
        AsyncTask<Int?, Int?, String?>() {
        /*  String.format("%.2f", latitude).toFloat(),
          String.format("%.2f", longitude).toFloat()
  */
        var latitude = Latitud
        var longitude = Longitud
        var ControlLatitud: String? = ""//ControlLatitud
        var ControlLongitud: String? = ""//ControlLongitud
        var resultReturn: String = "DESCONECTADO"

        /*      override fun doInBackground(vararg params: Int?): String {
                  var listaNueva: MutableList<String>? = ArrayList()
                  var HourNow: String? = null
                  callback.onProgress("Realizando")
                  val s = ValidaConexion()// sp?.getBoolean("isconnected", false)
                  /*if (s == true) {

                      try {

                          var listalatitude = mutableListOf<String>()
                          var listalongitude = mutableListOf<String>()

                          var La: String? = null
                          var Lo: String? = null


                          listalatitude.add(String.format("%.6f", latitude).toFloat().toString())
                          listalongitude.add(String.format("%.6f", longitude).toFloat().toString())
                          //listalatitude.add(String.format("%.6f", latitude).toString())
                          val exalongitude = if ((listalongitude).get(0).toFloat() > 0) {
                              Log.d(
                                  "checkGooglePlayServices",
                                  "exalongitude es mayor a 0  ${(listalongitude).get(0).toFloat()}"
                              )
                              convertDecimalToHexa((listalongitude)[0].toDouble())
                                  .uppercase()
                          } else {
                              Log.d(
                                  "checkGooglePlayServices",
                                  "exalongitude es menor a 0  ${(listalongitude).get(0).toFloat()}"
                              )
                              convertDecimalToHexa((listalongitude)[0].toDouble())
                                  .uppercase()
                          }
                          val exalatitude =
                              convertDecimalToHexa((listalatitude)[0].toDouble())
                                  .uppercase()
                          /*if ((listalatitude).get(0).toFloat()>0){
                          Log.d("checkGooglePlayServices","exalatitude es mayor a 0  ${(listalatitude).get(0).toFloat()}")
                        //  getData(listalatitude).get(0).uppercase()
                       //   convertDecimalToHexa(latitude)
                        /*   GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(latitude.toString())
                              .uppercase()*/

                      } else{
                          Log.d("checkGooglePlayServices","es menor a 0  ${(listalatitude).get(0).toFloat()}")
                          convertDecimalToHexa(latitude)
                              .uppercase()}
      */

                          /*  val exalatitude = if (latitude.toFloat()>0) getData(listalatitude).get(0).uppercase() else{
                                convertDecimalToHexa(latitude)
                                    .uppercase()
                                   }
                            */
                          /* val exalongitude = if (listalongitude[0].toFloat() > 0) {
                                getData(listalongitude).get(0).uppercase()
                            } else {
                                convertDecimalToHexa(latitude).uppercase()
                            }

                            val exalatitude = if (listalatitude[0].toFloat() > 0) {
                                getData(listalatitude).get(0).uppercase()
                            } else {
                                convertDecimalToHexa(latitude).uppercase()
                            }*/

                          Log.d(
                              "checkGooglePlayServices",
                              "checkGooglePlayServices->>>>> Latitude: $latitude " + " Longitude: $longitude" +
                                      " -> exalatitude $exalatitude   ${getDecimalFloat(exalatitude)} exalongitude $exalongitude  ${
                                          getNegativeTempfloat(
                                              "FFFF${exalongitude}"
                                          ).toFloat()
                                      }   "
                          )


                          bluetoothLeService = bluetoothServices.bluetoothLeService()
                          bluetoothLeService?.sendFirstComando("4021")

                          Thread.sleep(1000)

                          HourNow = GetNowDateExa()
                          //  getInfoList()!!.clear()
                          clearLogger()
                          var Command: String? = null
                          HourNow?.let {
                              var CHECKSUMGEO = mutableListOf<String>()
                              CHECKSUMGEO.add("4059")
                              CHECKSUMGEO.add(
                                  exalatitude.toString().uppercase().substring(exalatitude.length - 8, exalatitude.length)
                              )
                              CHECKSUMGEO.add(
                                  exalongitude.toString().uppercase().substring(exalongitude.length - 8, exalongitude.length)
                              )

                              CHECKSUMGEO.add(sumarParesHexadecimales(CHECKSUMGEO))
                              val result = CHECKSUMGEO.joinToString("")

                              Log.d(
                                  "MyAsyncTaskSendXY",
                                  "resultado $CHECKSUMGEO sin espacios  $result"
                              )
                              getInfoList()?.clear()
                              if (bluetoothLeService!!.sendFirstComando(result)) {

                                  Thread.sleep(450)
                                  listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                                  Thread.sleep(450)
                                  Log.d(
                                      "MyAsyncTaskSendXY",
                                      "->>>>>>>>>>>>>>>>>>>>>>>>${listData[0]}  ${getInfoList()}"
                                  )
                                  if (getInfoList()!!.joinToString("").equals("F13D")) {
                                      return "F1 3D"
                                  } else {
                                      callback.onError("F1 3E")
                                      return "F1 3E"
                                  }
                              } else Log.d("checkGooglePlayServices", "dataChecksum total:8")

                          } ?: "sin hora"

                          return "sin hora"
                      } catch (e: InterruptedException) {
                          e.printStackTrace()
                          return "exception"
                      }
                  }
                  */
                  if (s) {
                      try {
                          bluetoothLeService = bluetoothServices.bluetoothLeService()
                          bluetoothLeService?.sendFirstComando("4021")
                          Thread.sleep(1000)
                          Log.d("islistttt2", ":bluetoothLeService $bluetoothLeService")
                          getInfoList()?.clear()
                          listData.clear()
                          bluetoothLeService?.sendFirstComando("405A")
                          Thread.sleep(400)
                          for (num in 0..2) {
                              listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                              Log.d("islistttt2", ":" + listData[num] + "  ")
                              Thread.sleep(700)
                          }
                          listData.let {
                              val s = listData[0]
                              val sSinEspacios = s?.replace(" ", "")
                              Log.d(
                                  "islistttt2",
                                  ": Log.d(\"islistttt2\", \":\" + listData[num])" + getInfoList() + " listData[0] -> ${listData[0]}  sSinEspacios $sSinEspacios"
                              )
                              //  ControlLatitud = sSinEspacios!!.substring(8, 12)//.toFloat()
                              //ControlLongitud = sSinEspacios!!.substring(4, 8)//.toString().toFloat()
                              ControlLatitud = sSinEspacios!!.substring(sSinEspacios.length-16, sSinEspacios.length-12)
                              ControlLongitud = sSinEspacios!!.substring(sSinEspacios.length-12, sSinEspacios.length-8)//.toString().toFloat()
                              val LONGITUD = sSinEspacios!!.substring(24, 32)
                              val LATITUD = sSinEspacios!!.substring(16, 24)
                          }
                          Thread.sleep(800)
                          var SalidaLATITUDZ = ""
                          ControlLatitud?.let { it1 ->
                              Log.d(
                                  "checkGooglePlayServices", "LATITUDZ $it1"
                              )
                              if (it1.first().uppercase() == "F" || it1.first().uppercase().equals("D") )
                              // conexionTrefp2. getDecimalFloat(it1)
                              {
                                  SalidaLATITUDZ = getNegativeTempfloat4(
                                      "FFFF${it1}"
                                  )

                              } else {
                                  SalidaLATITUDZ = getDecimalFloat2(it1).toString()
                                  Log.d(
                                      "checkGooglePlayServices",
                                      "S ${it1.substring(1).uppercase()}"
                                  )
                              }

                          }
                          var SalidaLongitud = ""
                          ControlLongitud?.let { it1 ->
                              Log.d(
                                  "checkGooglePlayServices", "LATITUDZ $it1"
                              )
                              if (it1.first().uppercase().equals("F") || it1.first().uppercase().equals("D") )
                              // conexionTrefp2. getDecimalFloat(it1)
                              {
                                  SalidaLongitud = getNegativeTempfloat4(
                                      "FFFF${it1}"
                                  ).toString()

                              } else {
                                  SalidaLongitud = getDecimalFloat2(it1).toString()
                                  Log.d(
                                      "checkGooglePlayServices",
                                      "S ${it1.substring(1).uppercase()}"
                                  )
                              }

                          }
                          Thread.sleep(800)
                          isWithinRange(getContext!!,
                              latitude.toDouble(),
                              longitude.toDouble(),
                              SalidaLATITUDZ.toDouble(),
                              SalidaLongitud.toDouble(),
                              object : OnRangeCheckListener {
                                  override fun onRangeChecked(isWithinRange: Boolean): Boolean {
                                      Log.d(
                                          "checkGooglePlayServices",
                                          "checkGooglePlayServices->>>>> Latitude: $latitude " + " Longitude: $longitude  isWithinRange $isWithinRange"
                                      )
                                      if (!isWithinRange) {
                                          var listalatitude = mutableListOf<String>()
                                          var listalongitude = mutableListOf<String>()

                                          var La: String? = null
                                          var Lo: String? = null
                                          Log.d(
                                              "checkGooglePlayServices",
                                              "VALORES A ENVIAR latitude ${String.format("%.4f",latitude)}  --- $latitude  ${latitude.toDouble()} ---- longitude ${String.format("%.4f",longitude)} ---$longitude --  "
                                          )

                                          listalatitude.add(/*String.format("%.6f", latitude).toDouble().toString()*/ formatDoubleWithDecimals(latitude,6).toString()//String.format("%.4f",latitude)
                                          )
                                          listalongitude.add(/*tring.format("%.6f", longitude).toFloat().toString()*/formatDoubleWithDecimals(longitude,6).toString() //String.format( "%.4f",longitude)
                                          )

                                          val r = String.format("%.6f", latitude).toDouble()
                                          val exalongitude = convertDecimalToHexa(/*(listalongitude)[0].toDouble()*/ formatDoubleWithDecimals(longitude,6))  //String.format("%.4f",longitude).toDouble())

                                          val exalatitude = convertDecimalToHexa(/*(listalatitude)[0].toDouble()*/formatDoubleWithDecimals(latitude,6)) //String.format("%.4f",latitude).toDouble())

                                          Log.d(
                                              "checkGooglePlayServices",
                                              "checkGooglePlayServices->>>>> Latitude: $latitude " + " Longitude: $longitude" +
                                                      " -> exalatitude $exalatitude   ${getDecimalFloat(exalatitude)} exalongitude $exalongitude  ${
                                                          getNegativeTempfloat(
                                                              "FFFF${exalongitude}"
                                                          ).toFloat()
                                                      }   "
                                          )


                                          bluetoothLeService = bluetoothServices.bluetoothLeService()
                                          bluetoothLeService?.sendFirstComando("4021")

                                          Thread.sleep(500)

                                          HourNow = GetNowDateExa()
                                          getInfoList()!!.clear()
                                          var Command: String? = null
                                          HourNow?.let {
                                              var CHECKSUMGEO = mutableListOf<String>()
                                              CHECKSUMGEO.add("4059")
                                              CHECKSUMGEO.add(
                                                  exalatitude.toString().uppercase()
                                                      .substring(exalatitude.length - 8, exalatitude.length)
                                              )
                                              CHECKSUMGEO.add(
                                                  exalongitude.toString().uppercase()
                                                      .substring(exalongitude.length - 8, exalongitude.length)
                                              )

                                              CHECKSUMGEO.add(sumarParesHexadecimales(CHECKSUMGEO))
                                              var result = CHECKSUMGEO.joinToString("")

                                              Log.d(
                                                  "MyAsyncTaskSendXY",
                                                  "resultado $CHECKSUMGEO sin espacios  $result"
                                              )
                                              getInfoList()?.clear()
                                              if (bluetoothLeService!!.sendFirstComando(result)) {

                                                  Thread.sleep(450)
                                                  listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                                                  Thread.sleep(450)
                                                  Log.d(
                                                      "MyAsyncTaskSendXY",
                                                      "->>>>>>>>>>>>>>>>>>>>>>>>${listData[0]}  ${getInfoList()}"
                                                  )
                                                  if (getInfoList()!!.joinToString("").equals("F13D")) {
                                                      resultReturn = "F1 3D"
                                                  } else {
                                                      callback.onError("F1 3E")
                                                      resultReturn = "F1 3E"
                                                  }
                                              } else {
                                                  Log.d("checkGooglePlayServices", "dataChecksum total:8")
                                                  resultReturn = "error"
                                              }

                                          } ?: "Sin hora"

                                      } else {
                                          val info = mutableListOf<String>()
                                          info.add("Ubicacion dentro del rango")
                                          runOnUiThread {
                                              callback.onSuccess(true)

                                              callback.getInfo(info)
                                          }
                                          resultReturn = "Ubicacion dentro del rango"
                                      }
                                      return isWithinRange
                                  }

                              })


                      } catch (e: InterruptedException) {
                          e.printStackTrace()
                          return "exception"
                      }
                      resultReturn //
                  }
              return resultReturn
              }
      */
        override fun doInBackground(vararg params: Int?): String? {
            var listaNueva: MutableList<String>? = ArrayList()
            var HourNow: String? = null
            callback.onProgress("Realizando")
            val s = ValidaConexion()// sp?.getBoolean("isconnected", false)


            val exalongitude =
                convertDecimalToHexa(formatDoubleWithDecimals(longitude, 6))

            val exalatitude =
                convertDecimalToHexa(formatDoubleWithDecimals(latitude, 6))

            var CHECKSUMGEO = mutableListOf<String>()
            CHECKSUMGEO.add("4059")
            CHECKSUMGEO.add(
                exalatitude.toString().uppercase()
                    .substring(
                        exalatitude.length - 8,
                        exalatitude.length
                    )
            )
            CHECKSUMGEO.add(
                exalongitude.toString().uppercase()
                    .substring(
                        exalongitude.length - 8,
                        exalongitude.length
                    )
            )

            CHECKSUMGEO.add(sumarParesHexadecimales(CHECKSUMGEO))
            var result = CHECKSUMGEO.joinToString("")

            Log.d(
                "MyAsyncTaskSendXY",
                "resultado $CHECKSUMGEO sin espacios  $result"
            )
            clearLogger()

            bluetoothLeService!!.sendFirstComando(result)
            Thread.sleep(1000)
            Log.d(
                "MyAsyncTaskSendXY",
                "resultado $CHECKSUMGEO sin espacios  $result ${getInfoList()!!}"
            )

            if (getInfoList()!!.joinToString("") == "F13D") {
                clearLogger()
                bluetoothLeService?.sendFirstComando("405A")
                Thread.sleep(1000)
                var Respuesta = getInfoList()
                Log.d(
                    "MyAsyncTaskSendXY",
                    "Respuesta  4050A $Respuesta"
                )
                if (!Respuesta.isNullOrEmpty() && Respuesta.toString().length >= 32) {
                    var s = Respuesta.toString()
                    val sSinEspacios = s.replace(" ", "")
                    val LONGITUD = try {
                        sSinEspacios!!.substring(24, 32) ?: "0"
                    } catch (Excep: Exception) {
                        "0"
                    }
                    val LATITUD = try {
                        sSinEspacios!!.substring(16, 24) ?: "0"
                    } catch (Excep: Exception) {
                        "0"
                    }
                    var salida = mutableListOf<String>()
                    salida.add(LONGITUD)
                    salida.add(LATITUD)
                    callback.getInfo(salida)
                    //  Log.d("MyAsyncTaskSendXYPRueba"," LONGITUD $LONGITUD  LATITUD $LATITUD")

                } else {
                    callback.onError("Hubo un error al obtener XY.")
                    resultReturn = "F1 3E"
                }
                resultReturn = "F1 3D"
            } else {
                callback.onError("No se pudo enviar XY, intente de nuevo")
                resultReturn = "F1 3E"
            }


            return resultReturn
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.d("checkGooglePlayServices", "----> $result")
            when (result) {

                "F1 3D" -> {
                    callback.onSuccess(true)
                }

                "F1 3E" -> {
                    callback.onSuccess(false)
                }

                "not" -> {
                    callback.onSuccess(false)
                    callback.onError("ErrorCommand")
                }

                "DESCONECTADO" -> {
                    callback.onSuccess(false)
                    callback.onError("DESCONECTADO")
                }

                "Campos vacios" -> {
                    callback.onSuccess(false)
                    callback.onError("Campos vacios")
                }
                /* else->{
                     callback.onSuccess(false)
                     if (result != null) {
                         callback.onError(result)
                     }
                 }*/
            }
            callback.onProgress("Finalizado")
        }


        override fun onPreExecute() {

            clearLogger()
            callback.onProgress("Iniciando")
        }


    }


    private inner class MyAsyncTaskSendXYNewVersionnoEstable(
        Latitud: Double,
        Longitud: Double,
        private val callback: MyCallback
    ) :
        AsyncTask<Int?, Int?, String?>() {
        /*  String.format("%.2f", latitude).toFloat(),
          String.format("%.2f", longitude).toFloat()
  */
        var latitude = Latitud
        var longitude = Longitud
        var ControlLatitud: String? = ""//ControlLatitud
        var ControlLongitud: String? = ""//ControlLongitud
        var resultReturn: String = ""

        /*      override fun doInBackground(vararg params: Int?): String {
                  var listaNueva: MutableList<String>? = ArrayList()
                  var HourNow: String? = null
                  callback.onProgress("Realizando")
                  val s = ValidaConexion()// sp?.getBoolean("isconnected", false)
                  /*if (s == true) {

                      try {

                          var listalatitude = mutableListOf<String>()
                          var listalongitude = mutableListOf<String>()

                          var La: String? = null
                          var Lo: String? = null


                          listalatitude.add(String.format("%.6f", latitude).toFloat().toString())
                          listalongitude.add(String.format("%.6f", longitude).toFloat().toString())
                          //listalatitude.add(String.format("%.6f", latitude).toString())
                          val exalongitude = if ((listalongitude).get(0).toFloat() > 0) {
                              Log.d(
                                  "checkGooglePlayServices",
                                  "exalongitude es mayor a 0  ${(listalongitude).get(0).toFloat()}"
                              )
                              convertDecimalToHexa((listalongitude)[0].toDouble())
                                  .uppercase()
                          } else {
                              Log.d(
                                  "checkGooglePlayServices",
                                  "exalongitude es menor a 0  ${(listalongitude).get(0).toFloat()}"
                              )
                              convertDecimalToHexa((listalongitude)[0].toDouble())
                                  .uppercase()
                          }
                          val exalatitude =
                              convertDecimalToHexa((listalatitude)[0].toDouble())
                                  .uppercase()
                          /*if ((listalatitude).get(0).toFloat()>0){
                          Log.d("checkGooglePlayServices","exalatitude es mayor a 0  ${(listalatitude).get(0).toFloat()}")
                        //  getData(listalatitude).get(0).uppercase()
                       //   convertDecimalToHexa(latitude)
                        /*   GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(latitude.toString())
                              .uppercase()*/

                      } else{
                          Log.d("checkGooglePlayServices","es menor a 0  ${(listalatitude).get(0).toFloat()}")
                          convertDecimalToHexa(latitude)
                              .uppercase()}
      */

                          /*  val exalatitude = if (latitude.toFloat()>0) getData(listalatitude).get(0).uppercase() else{
                                convertDecimalToHexa(latitude)
                                    .uppercase()
                                   }
                            */
                          /* val exalongitude = if (listalongitude[0].toFloat() > 0) {
                                getData(listalongitude).get(0).uppercase()
                            } else {
                                convertDecimalToHexa(latitude).uppercase()
                            }

                            val exalatitude = if (listalatitude[0].toFloat() > 0) {
                                getData(listalatitude).get(0).uppercase()
                            } else {
                                convertDecimalToHexa(latitude).uppercase()
                            }*/

                          Log.d(
                              "checkGooglePlayServices",
                              "checkGooglePlayServices->>>>> Latitude: $latitude " + " Longitude: $longitude" +
                                      " -> exalatitude $exalatitude   ${getDecimalFloat(exalatitude)} exalongitude $exalongitude  ${
                                          getNegativeTempfloat(
                                              "FFFF${exalongitude}"
                                          ).toFloat()
                                      }   "
                          )


                          bluetoothLeService = bluetoothServices.bluetoothLeService()
                          bluetoothLeService?.sendFirstComando("4021")

                          Thread.sleep(1000)

                          HourNow = GetNowDateExa()
                          //  getInfoList()!!.clear()
                          clearLogger()
                          var Command: String? = null
                          HourNow?.let {
                              var CHECKSUMGEO = mutableListOf<String>()
                              CHECKSUMGEO.add("4059")
                              CHECKSUMGEO.add(
                                  exalatitude.toString().uppercase().substring(exalatitude.length - 8, exalatitude.length)
                              )
                              CHECKSUMGEO.add(
                                  exalongitude.toString().uppercase().substring(exalongitude.length - 8, exalongitude.length)
                              )

                              CHECKSUMGEO.add(sumarParesHexadecimales(CHECKSUMGEO))
                              val result = CHECKSUMGEO.joinToString("")

                              Log.d(
                                  "MyAsyncTaskSendXY",
                                  "resultado $CHECKSUMGEO sin espacios  $result"
                              )
                              getInfoList()?.clear()
                              if (bluetoothLeService!!.sendFirstComando(result)) {

                                  Thread.sleep(450)
                                  listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                                  Thread.sleep(450)
                                  Log.d(
                                      "MyAsyncTaskSendXY",
                                      "->>>>>>>>>>>>>>>>>>>>>>>>${listData[0]}  ${getInfoList()}"
                                  )
                                  if (getInfoList()!!.joinToString("").equals("F13D")) {
                                      return "F1 3D"
                                  } else {
                                      callback.onError("F1 3E")
                                      return "F1 3E"
                                  }
                              } else Log.d("checkGooglePlayServices", "dataChecksum total:8")

                          } ?: "sin hora"

                          return "sin hora"
                      } catch (e: InterruptedException) {
                          e.printStackTrace()
                          return "exception"
                      }
                  }
                  */
                  if (s) {
                      try {
                          bluetoothLeService = bluetoothServices.bluetoothLeService()
                          bluetoothLeService?.sendFirstComando("4021")
                          Thread.sleep(1000)
                          Log.d("islistttt2", ":bluetoothLeService $bluetoothLeService")
                          getInfoList()?.clear()
                          listData.clear()
                          bluetoothLeService?.sendFirstComando("405A")
                          Thread.sleep(400)
                          for (num in 0..2) {
                              listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                              Log.d("islistttt2", ":" + listData[num] + "  ")
                              Thread.sleep(700)
                          }
                          listData.let {
                              val s = listData[0]
                              val sSinEspacios = s?.replace(" ", "")
                              Log.d(
                                  "islistttt2",
                                  ": Log.d(\"islistttt2\", \":\" + listData[num])" + getInfoList() + " listData[0] -> ${listData[0]}  sSinEspacios $sSinEspacios"
                              )
                              //  ControlLatitud = sSinEspacios!!.substring(8, 12)//.toFloat()
                              //ControlLongitud = sSinEspacios!!.substring(4, 8)//.toString().toFloat()
                              ControlLatitud = sSinEspacios!!.substring(sSinEspacios.length-16, sSinEspacios.length-12)
                              ControlLongitud = sSinEspacios!!.substring(sSinEspacios.length-12, sSinEspacios.length-8)//.toString().toFloat()
                              val LONGITUD = sSinEspacios!!.substring(24, 32)
                              val LATITUD = sSinEspacios!!.substring(16, 24)
                          }
                          Thread.sleep(800)
                          var SalidaLATITUDZ = ""
                          ControlLatitud?.let { it1 ->
                              Log.d(
                                  "checkGooglePlayServices", "LATITUDZ $it1"
                              )
                              if (it1.first().uppercase() == "F" || it1.first().uppercase().equals("D") )
                              // conexionTrefp2. getDecimalFloat(it1)
                              {
                                  SalidaLATITUDZ = getNegativeTempfloat4(
                                      "FFFF${it1}"
                                  )

                              } else {
                                  SalidaLATITUDZ = getDecimalFloat2(it1).toString()
                                  Log.d(
                                      "checkGooglePlayServices",
                                      "S ${it1.substring(1).uppercase()}"
                                  )
                              }

                          }
                          var SalidaLongitud = ""
                          ControlLongitud?.let { it1 ->
                              Log.d(
                                  "checkGooglePlayServices", "LATITUDZ $it1"
                              )
                              if (it1.first().uppercase().equals("F") || it1.first().uppercase().equals("D") )
                              // conexionTrefp2. getDecimalFloat(it1)
                              {
                                  SalidaLongitud = getNegativeTempfloat4(
                                      "FFFF${it1}"
                                  ).toString()

                              } else {
                                  SalidaLongitud = getDecimalFloat2(it1).toString()
                                  Log.d(
                                      "checkGooglePlayServices",
                                      "S ${it1.substring(1).uppercase()}"
                                  )
                              }

                          }
                          Thread.sleep(800)
                          isWithinRange(getContext!!,
                              latitude.toDouble(),
                              longitude.toDouble(),
                              SalidaLATITUDZ.toDouble(),
                              SalidaLongitud.toDouble(),
                              object : OnRangeCheckListener {
                                  override fun onRangeChecked(isWithinRange: Boolean): Boolean {
                                      Log.d(
                                          "checkGooglePlayServices",
                                          "checkGooglePlayServices->>>>> Latitude: $latitude " + " Longitude: $longitude  isWithinRange $isWithinRange"
                                      )
                                      if (!isWithinRange) {
                                          var listalatitude = mutableListOf<String>()
                                          var listalongitude = mutableListOf<String>()

                                          var La: String? = null
                                          var Lo: String? = null
                                          Log.d(
                                              "checkGooglePlayServices",
                                              "VALORES A ENVIAR latitude ${String.format("%.4f",latitude)}  --- $latitude  ${latitude.toDouble()} ---- longitude ${String.format("%.4f",longitude)} ---$longitude --  "
                                          )

                                          listalatitude.add(/*String.format("%.6f", latitude).toDouble().toString()*/ formatDoubleWithDecimals(latitude,6).toString()//String.format("%.4f",latitude)
                                          )
                                          listalongitude.add(/*tring.format("%.6f", longitude).toFloat().toString()*/formatDoubleWithDecimals(longitude,6).toString() //String.format( "%.4f",longitude)
                                          )

                                          val r = String.format("%.6f", latitude).toDouble()
                                          val exalongitude = convertDecimalToHexa(/*(listalongitude)[0].toDouble()*/ formatDoubleWithDecimals(longitude,6))  //String.format("%.4f",longitude).toDouble())

                                          val exalatitude = convertDecimalToHexa(/*(listalatitude)[0].toDouble()*/formatDoubleWithDecimals(latitude,6)) //String.format("%.4f",latitude).toDouble())

                                          Log.d(
                                              "checkGooglePlayServices",
                                              "checkGooglePlayServices->>>>> Latitude: $latitude " + " Longitude: $longitude" +
                                                      " -> exalatitude $exalatitude   ${getDecimalFloat(exalatitude)} exalongitude $exalongitude  ${
                                                          getNegativeTempfloat(
                                                              "FFFF${exalongitude}"
                                                          ).toFloat()
                                                      }   "
                                          )


                                          bluetoothLeService = bluetoothServices.bluetoothLeService()
                                          bluetoothLeService?.sendFirstComando("4021")

                                          Thread.sleep(500)

                                          HourNow = GetNowDateExa()
                                          getInfoList()!!.clear()
                                          var Command: String? = null
                                          HourNow?.let {
                                              var CHECKSUMGEO = mutableListOf<String>()
                                              CHECKSUMGEO.add("4059")
                                              CHECKSUMGEO.add(
                                                  exalatitude.toString().uppercase()
                                                      .substring(exalatitude.length - 8, exalatitude.length)
                                              )
                                              CHECKSUMGEO.add(
                                                  exalongitude.toString().uppercase()
                                                      .substring(exalongitude.length - 8, exalongitude.length)
                                              )

                                              CHECKSUMGEO.add(sumarParesHexadecimales(CHECKSUMGEO))
                                              var result = CHECKSUMGEO.joinToString("")

                                              Log.d(
                                                  "MyAsyncTaskSendXY",
                                                  "resultado $CHECKSUMGEO sin espacios  $result"
                                              )
                                              getInfoList()?.clear()
                                              if (bluetoothLeService!!.sendFirstComando(result)) {

                                                  Thread.sleep(450)
                                                  listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                                                  Thread.sleep(450)
                                                  Log.d(
                                                      "MyAsyncTaskSendXY",
                                                      "->>>>>>>>>>>>>>>>>>>>>>>>${listData[0]}  ${getInfoList()}"
                                                  )
                                                  if (getInfoList()!!.joinToString("").equals("F13D")) {
                                                      resultReturn = "F1 3D"
                                                  } else {
                                                      callback.onError("F1 3E")
                                                      resultReturn = "F1 3E"
                                                  }
                                              } else {
                                                  Log.d("checkGooglePlayServices", "dataChecksum total:8")
                                                  resultReturn = "error"
                                              }

                                          } ?: "Sin hora"

                                      } else {
                                          val info = mutableListOf<String>()
                                          info.add("Ubicacion dentro del rango")
                                          runOnUiThread {
                                              callback.onSuccess(true)

                                              callback.getInfo(info)
                                          }
                                          resultReturn = "Ubicacion dentro del rango"
                                      }
                                      return isWithinRange
                                  }

                              })


                      } catch (e: InterruptedException) {
                          e.printStackTrace()
                          return "exception"
                      }
                      resultReturn //
                  }
              return resultReturn
              }
      */
        override fun doInBackground(vararg params: Int?): String? {
            var listaNueva: MutableList<String>? = ArrayList()
            var HourNow: String? = null
            callback.onProgress("Realizando")
            val s = ValidaConexion()// sp?.getBoolean("isconnected", false)
            if (s) {
                try {
                    bluetoothLeService = bluetoothServices.bluetoothLeService()
                    bluetoothLeService?.sendFirstComando("4021")
                    Thread.sleep(1000)
                    Log.d("islistttt2", ":bluetoothLeService $bluetoothLeService")
                    getInfoList()?.clear()
                    listData.clear()
                    bluetoothLeService?.sendFirstComando("405A")
                    Thread.sleep(400)
                    for (num in 0..2) {
                        listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                        Log.d("islistttt2", ":" + listData[num] + "  ")
                        Thread.sleep(700)
                    }
                    listData.let {
                        val s = listData[0]
                        val sSinEspacios = s?.replace(" ", "")
                        Log.d(
                            "islistttt2",
                            ": Log.d(\"islistttt2\", \":\" + listData[num])" + getInfoList() + " listData[0] -> ${listData[0]}  sSinEspacios $sSinEspacios"
                        )
                        //  ControlLatitud = sSinEspacios!!.substring(8, 12)//.toFloat()
                        //ControlLongitud = sSinEspacios!!.substring(4, 8)//.toString().toFloat()


                        /*   ControlLatitud = sSinEspacios!!.substring(sSinEspacios.length-16, sSinEspacios.length-12)
                           ControlLongitud = sSinEspacios!!.substring(sSinEspacios.length-12, sSinEspacios.length-8)//.toString().toFloat()
                         */
                        ControlLongitud = sSinEspacios!!.substring(24, 32)
                        ControlLatitud = sSinEspacios!!.substring(16, 24)
                    }
                    Thread.sleep(800)
                    var SalidaLATITUDZ = ""
                    ControlLatitud?.let { it1 ->
                        Log.d(
                            "checkGooglePlayServices", "LATITUDZ $it1"
                        )
                        if (it1.first().uppercase() == "F" || it1.first().uppercase().equals("D"))
                        // conexionTrefp2. getDecimalFloat(it1)
                        {
                            SalidaLATITUDZ = getNegativeTempfloat(
                                "FFFF${it1}"
                            )

                        } else {
                            SalidaLATITUDZ = getDecimalFloat(it1).toString()
                            Log.d(
                                "checkGooglePlayServices",
                                "S ${it1.substring(1).uppercase()}"
                            )
                        }

                    }
                    var SalidaLongitud = ""
                    ControlLongitud?.let { it1 ->
                        Log.d(
                            "checkGooglePlayServices", "LATITUDZ $it1"
                        )
                        if (it1.first().uppercase().equals("F") || it1.first().uppercase()
                                .equals("D")
                        )
                        // conexionTrefp2. getDecimalFloat(it1)
                        {
                            SalidaLongitud = getNegativeTempfloat(
                                "FFFF${it1}"
                            ).toString()

                        } else {
                            SalidaLongitud = getDecimalFloat(it1).toString()
                            Log.d(
                                "checkGooglePlayServices",
                                "S ${it1.substring(1).uppercase()}"
                            )
                        }

                    }
                    Thread.sleep(800)
                    isWithinRange6(getContext!!,
                        latitude,
                        longitude,
                        SalidaLATITUDZ.toDouble(),
                        SalidaLongitud.toDouble(),
                        object : OnRangeCheckListener {
                            override fun onRangeChecked(isWithinRange: Boolean): Boolean {
                                Log.d(
                                    "checkGooglePlayServices",
                                    "checkGooglePlayServices->>>>> Latitude: $latitude " + " Longitude: $longitude  isWithinRange $isWithinRange"
                                )
                                if (!isWithinRange) {
                                    var listalatitude = mutableListOf<String>()
                                    var listalongitude = mutableListOf<String>()

                                    var La: String? = null
                                    var Lo: String? = null
                                    Log.d(
                                        "checkGooglePlayServices",
                                        "VALORES A ENVIAR latitude ${
                                            String.format(
                                                "%.6f",
                                                latitude
                                            )
                                        }  --- $latitude  ${latitude.toDouble()} ---- longitude ${
                                            String.format(
                                                "%.6f",
                                                longitude
                                            )
                                        } ---$longitude --  "
                                    )

                                    listalatitude.add(/*String.format("%.6f", latitude).toDouble().toString()*/
                                        formatDoubleWithDecimals(
                                            latitude,
                                            6
                                        ).toString()//String.format("%.4f",latitude)
                                    )
                                    listalongitude.add(/*tring.format("%.6f", longitude).toFloat().toString()*/
                                        formatDoubleWithDecimals(
                                            longitude,
                                            6
                                        ).toString() //String.format( "%.4f",longitude)
                                    )

                                    val r = String.format("%.6f", latitude).toDouble()
                                    val exalongitude =
                                        convertDecimalToHexa(/*(listalongitude)[0].toDouble()*/
                                            formatDoubleWithDecimals(longitude, 6)
                                        )  //String.format("%.4f",longitude).toDouble())

                                    val exalatitude =
                                        convertDecimalToHexa(/*(listalatitude)[0].toDouble()*/
                                            formatDoubleWithDecimals(latitude, 6)
                                        ) //String.format("%.4f",latitude).toDouble())

                                    Log.d(
                                        "checkGooglePlayServices",
                                        "checkGooglePlayServices->>>>> Latitude: $latitude " + " Longitude: $longitude" +
                                                " -> exalatitude $exalatitude   ${
                                                    getDecimalFloat(
                                                        exalatitude
                                                    )
                                                } exalongitude $exalongitude  ${
                                                    getNegativeTempfloat(
                                                        "FFFF${exalongitude}"
                                                    ).toFloat()
                                                }   "
                                    )


                                    bluetoothLeService = bluetoothServices.bluetoothLeService()
                                    bluetoothLeService?.sendFirstComando("4021")

                                    Thread.sleep(500)

                                    HourNow = GetNowDateExa()
                                    clearLogger()
                                    var Command: String? = null
                                    HourNow?.let {
                                        var CHECKSUMGEO = mutableListOf<String>()
                                        CHECKSUMGEO.add("4059")
                                        CHECKSUMGEO.add(
                                            exalatitude.toString().uppercase()
                                                .substring(
                                                    exalatitude.length - 8,
                                                    exalatitude.length
                                                )
                                        )
                                        CHECKSUMGEO.add(
                                            exalongitude.toString().uppercase()
                                                .substring(
                                                    exalongitude.length - 8,
                                                    exalongitude.length
                                                )
                                        )

                                        CHECKSUMGEO.add(sumarParesHexadecimales(CHECKSUMGEO))
                                        var result = CHECKSUMGEO.joinToString("")

                                        Log.d(
                                            "MyAsyncTaskSendXY",
                                            "resultado $CHECKSUMGEO sin espacios  $result"
                                        )
                                        getInfoList()?.clear()
                                        if (bluetoothLeService!!.sendFirstComando(result)) {

                                            Thread.sleep(450)
                                            listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                                            Thread.sleep(450)
                                            Log.d(
                                                "MyAsyncTaskSendXY",
                                                "->>>>>>>>>>>>>>>>>>>>>>>>${listData[0]}  ${
                                                    getInfoList()!!.joinToString(
                                                        ""
                                                    )
                                                }"
                                            )
                                            if (getInfoList()!!.joinToString("").equals("F13D")) {
                                                resultReturn = "F1 3D"
                                            } else {
                                                callback.onError("F1 3E")
                                                resultReturn = "F1 3E"
                                            }
                                        } else {
                                            Log.d("checkGooglePlayServices", "dataChecksum total:8")
                                            resultReturn = "error"
                                        }

                                    } ?: "Sin hora"

                                } else {
                                    val info = mutableListOf<String>()
                                    info.add("Ubicacion dentro del rango")
                                    runOnUiThread {
                                        callback.onSuccess(true)

                                        callback.getInfo(info)
                                    }
                                    resultReturn = "Ubicacion dentro del rango"
                                }
                                return isWithinRange
                            }

                        })

                    return resultReturn //"sin hora"
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    return "exception"
                }
            }
            return "DESCONECTADO"
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.d("checkGooglePlayServices", "----> $result")
            when (result) {

                "F1 3D" -> {
                    //62840A56 -> Your time zone: martes, 17 de mayo de 2022 15:49:26 GMT-05:00
                    /*   getInfoList()!!.clear()
                       bluetoothLeService!!.sendFirstComando("405A")
                       Thread.sleep(700)
                       listData.clear()
                       listData.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                       val s = listData[0]
                       val sSinEspacios = s?.replace(" ", "")
                       val TIMEUNIX = sSinEspacios//?.substring(16, 24)
                       var listTIMEUNIX: MutableList<String>? = ArrayList()
                       TIMEUNIX?.let {
                           listTIMEUNIX?.add(it)
                       }

                       println("------------------------------------------ ${getInfoList()}")
                       callback.getInfo(listTIMEUNIX)
                       callback.onSuccess(true)
                       */
                    callback.onSuccess(true)

                }

                "F1 3E" -> {

                    callback.onSuccess(false)
                    //callback.onError("ErrorCommand")
                }

                "not" -> {
                    callback.onSuccess(false)
                    callback.onError("ErrorCommand")
                }

                "DESCONECTADO" -> {
                    callback.onSuccess(false)
                    callback.onError("DESCONECTADO")
                }
                /* else->{
                     callback.onSuccess(false)
                     if (result != null) {
                         callback.onError(result)
                     }
                 }*/
            }
            callback.onProgress("Finalizado")
        }


        override fun onPreExecute() {

            getInfoList()?.clear()
            callback.onProgress("Iniciando")
        }


    }

    fun convertDecimalToHexa(s: Double): String {

        val f = s * 100000
        val nm = f.toInt()
        val c = Integer.toHexString(nm)
        Log.d("checkGooglePlayServices", "convertDecimalToHexa nm $nm c $c")
        val paddedHex = c.padStart(8, '0')
        return paddedHex

    }

    fun getNegativeTempfloat(hexaTemp: String): String {

        val parsedResult: Double = hexaTemp.toLong(16).toInt().toDouble() // as  Int.toFloat()
        Log.d("checkGooglePlayServices", "parsedResult $parsedResult hexaTemp $hexaTemp")
        val result = parsedResult / 100000.0

        return String.format("%.6f", result)
    }

    private fun getNeg(`val`: Float): String {
        var `val` = `val`
        `val` *= 100
        val numfi = `val`.toInt()
        val hex = Integer.toHexString(numfi)
        println("---------------------------->" + hex.substring(4))
        return hex.substring(4)
        /* var `val` = `val`
         `val` *= 100
         val numfi = `val`.toInt()
         val hex = Integer.toHexString(numfi)
         return hex.substring(3).padStart(4, '0')
         */
    }

    fun convertHexaToDecimal(hex: String): Int {
        return hex.toInt(16)
    }

    fun convertHexaToDecimalWithTwoDecimals(hex: String): Double {
        val decimalValue = convertHexaToDecimal(hex)
        return decimalValue.toDouble() / 10000
    }

    fun getDecimalFloat(hex: String): String {
        val decimalPlaces = 6
        var hex = hex
        val digits = "0123456789ABCDEF"
        hex = hex.uppercase(Locale.getDefault())
        var `val` = 0.0
        for (element in hex) {
            val c = element
            val d = digits.indexOf(c)
            `val` = 16 * `val` + d
        }
        Log.d("checkGooglePlayServices", "`val` ${`val`}")
        val result = `val` / 100000
        return String.format("%.6f", result)
    }

    fun getDecimalFloatLogger(hex: String): Float {
        val digits = "0123456789ABCDEF"
        val upperHex = hex.toUpperCase()
        var valDecimal = 0f

        for (i in 0 until upperHex.length) {
            val c = upperHex[i]
            val d = digits.indexOf(c)
            valDecimal = 16 * valDecimal + d
        }

        return valDecimal / 10
    }

    private fun getData(dataListPlantilla: List<String>): List<String> {
        Log.d("tag", ";:$dataListPlantilla")
        GetHexFromRealDataOxxoDisplay.arrayListInfo.clear()

        //revision para hacer estos datos dinamicos
        //   GetHexFromRealDataOxxoDisplay.arrayListInfo.add("4050") //comando de inicio de modificación de parámetros
        // GetHexFromRealDataOxxoDisplay.arrayListInfo.add("AA") //buffer_size
        var i = 0
        do {
            when (i) {
                0 -> {
                    val numf = dataListPlantilla[i].toFloat()
                    val numfinal = dataListPlantilla[i].toFloat()
                    var num: Int
                    num = if (numfinal < 0 && numfinal > -1.0) {
                        -1
                    } else numf.toInt()

                    //Log.d("tag",":"+numfi);
                    Log.d("tag", "2:$numfinal")
                    if (Integer.signum(num) == 1 || numfinal > 0) {
                        Log.d("TAGGGSD", "122")
                        GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                            GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                dataListPlantilla[i]
                            )
                        ) //decimales con punto //get temp positivo
                        GetHexFromRealDataOxxoDisplay.T0 =
                            GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                dataListPlantilla[i]
                            )
                    } else if (Integer.signum(num) == -1) {
                        GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                            getNeg(
                                numfinal
                            )
                        ) //get negativos
                        GetHexFromRealDataOxxoDisplay.T0 = getNeg(
                            numfinal
                        )
                        Log.d("TAGGGSD", "11")
                    } else { //Es 0 cero
                        GetHexFromRealDataOxxoDisplay.T0 = "0000"
                        Log.d("TAGGGSD", "00")
                        GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000") //get negativos
                    }
                }

                1 -> {
                    val numf = dataListPlantilla[i].toFloat()
                    val numfinal = dataListPlantilla[i].toFloat()
                    var num: Int
                    num = if (numfinal < 0 && numfinal > -1.0) {
                        -1
                    } else numf.toInt()
                    //Log.d("tag",":"+numfi);
                    Log.d("tag", "2:$numfinal")
                    if (Integer.signum(num) == 1 || numfinal > 0) {
                        GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                            GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                dataListPlantilla[i]
                            )
                        ) //decimales con punto //get temp positivo
                    } else if (Integer.signum(num) == -1) {
                        GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                            GetHexFromRealDataOxxoDisplay.getNeg(
                                numfinal
                            )
                        ) //get negativos
                    } else { //Es 0 cero
                        GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000") //get negativos
                    }
                    GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000") //get negativos
                }

                /*     1 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                 dataListPlantilla[i]
                             )
                         ) //decimales con punto
                         GetHexFromRealDataOxxoDisplay.T1 =
                             GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                 dataListPlantilla[i]
                             )
                     }

                     2 -> {
                         val numf = dataListPlantilla[i].toFloat()
                         val numfinal = dataListPlantilla[i].toFloat()
                         var num: Int
                         num = if (numfinal < 0 && numfinal > -1.0) {
                             -1
                         } else numf.toInt()
                         //Log.d("tag",":"+numfi);
                         Log.d("tag", "2:$numfinal")
                         if (Integer.signum(num) == 1 || numfinal > 0) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                     dataListPlantilla[i]
                                 )
                             ) //decimales con punto //get temp positivo
                         } else if (Integer.signum(num) == -1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.getNeg(
                                     numfinal
                                 )
                             ) //get negativos
                         } else { //Es 0 cero
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000") //get negativos
                         }
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("000000000000") //get negativos
                     }

                     3 -> {
                         val numf = dataListPlantilla[i].toFloat()
                         val numfinal = dataListPlantilla[i].toFloat()
                         var num: Int
                         num = if (numfinal < 0 && numfinal > -1.0) {
                             -1
                         } else numf.toInt()
                         //Log.d("tag",":"+numfi);
                         Log.d("tag", "2:$numfinal")
                         if (Integer.signum(num) == 1 || numfinal > 0) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                     dataListPlantilla[i]
                                 )
                             ) //decimales con punto //get temp positivo
                         } else if (Integer.signum(num) == -1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.getNeg(
                                     numfinal
                                 )
                             ) //get negativos
                         } else { //Es 0 cero
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000") //get negativos
                         }
                     }

                     4 -> {
                         val numf = dataListPlantilla[i].toFloat()
                         val numfinal = dataListPlantilla[i].toFloat()
                         var num: Int
                         num = if (numfinal < 0 && numfinal > -1.0) {
                             -1
                         } else numf.toInt()
                         //Log.d("tag",":"+numfi);
                         Log.d("tag", "2:$numfinal")
                         if (Integer.signum(num) == 1 || numfinal > 0) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                     dataListPlantilla[i]
                                 )
                             ) //decimales con punto //get temp positivo
                         } else if (Integer.signum(num) == -1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.getNeg(
                                     numfinal
                                 )
                             ) //get negativos
                         } else { //Es 0 cero
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000") //get negativos
                         }
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000") //get negativos
                     }

                     5 -> {
                         val numf = dataListPlantilla[i].toFloat()
                         val numfinal = dataListPlantilla[i].toFloat()
                         var num: Int = if (numfinal < 0 && numfinal > -1.0) {
                             -1
                         } else numf.toInt()
                         //Log.d("tag",":"+numfi);
                         Log.d("tag", "2:$numfinal")
                         if (Integer.signum(num) == 1 || numfinal > 0) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                     dataListPlantilla[i]
                                 )
                             ) //decimales con punto //get temp positivo
                         } else if (Integer.signum(num) == -1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.getNeg(
                                     numfinal
                                 )
                             ) //get negativos
                         } else { //Es 0 cero
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000") //get negativos
                         }
                     }

                     6 -> {
                         val numf = dataListPlantilla[i].toFloat()
                         val numfinal = dataListPlantilla[i].toFloat()
                         var num: Int
                         num = if (numfinal < 0 && numfinal > -1.0) {
                             -1
                         } else numf.toInt()
                         //Log.d("tag",":"+numfi);
                         Log.d("tag", "2:$numfinal")
                         if (Integer.signum(num) == 1 || numfinal > 0) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                     dataListPlantilla[i]
                                 )
                             ) //decimales con punto //get temp positivo
                         } else if (Integer.signum(num) == -1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.getNeg(
                                     numfinal
                                 )
                             ) //get negativos
                         } else { //Es 0 cero
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000") //get negativos
                         }
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("00000000") //get negativos
                     }

                     7 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                 dataListPlantilla[i]
                             )
                         ) //decimales con punto
                     }

                     8 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                 dataListPlantilla[i]
                             )
                         ) //decimales con punto
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000")
                     }

                     9 -> {
                         val numf = dataListPlantilla[i].toFloat()
                         val numfinal = dataListPlantilla[i].toFloat()
                         var num: Int = if (numfinal < 0 && numfinal > -1.0) {
                             -1
                         } else numf.toInt()
                         //Log.d("tag",":"+numfi);
                         Log.d("tag", "2:$numfinal")
                         if (Integer.signum(num) == 1 || numfinal > 0) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                     dataListPlantilla[i]
                                 )
                             ) //decimales con punto //get temp positivo
                         } else if (Integer.signum(num) == -1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.getNeg(
                                     numfinal
                                 )
                             ) //get negativos
                         } else { //Es 0 cero
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000") //get negativos
                         }
                     }

                     10 -> {
                         val numf = dataListPlantilla[i].toFloat()
                         val numfinal = dataListPlantilla[i].toFloat()
                         var num: Int = if (numfinal < 0 && numfinal > -1.0) {
                             -1
                         } else numf.toInt()
                         //Log.d("tag",":"+numfi);
                         Log.d("tag", "2:$numfinal")
                         if (Integer.signum(num) == 1 || numfinal > 0) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                     dataListPlantilla[i]
                                 )
                             ) //decimales con punto //get temp positivo
                         } else if (Integer.signum(num) == -1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.getNeg(
                                     numfinal
                                 )
                             ) //get negativos
                         } else { //Es 0 cero
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000") //get negativos
                         }
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("00000000")
                     }

                     11 -> {
                         val numf = dataListPlantilla[i].toFloat()
                         val numfinal = dataListPlantilla[i].toFloat()
                         var num: Int = if (numfinal < 0 && numfinal > -1.0) {
                             -1
                         } else numf.toInt()
                         //Log.d("tag",":"+numfi);
                         Log.d("tag", "2:$numfinal")
                         if (Integer.signum(num) == 1 || numfinal > 0) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                     dataListPlantilla[i]
                                 )
                             ) //decimales con punto //get temp positivo
                         } else if (Integer.signum(num) == -1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.getNeg(
                                     numfinal
                                 )
                             ) //get negativos
                         } else { //Es 0 cero
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000") //get negativos
                         }
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000")
                     }

                     12 -> {
                         val numf = dataListPlantilla[i].toFloat()
                         val numfinal = dataListPlantilla[i].toFloat()
                         var num: Int = if (numfinal < 0 && numfinal > -1.0) {
                             -1
                         } else numf.toInt()
                         //Log.d("tag",":"+numfi);
                         Log.d("tag", "2:$numfinal")
                         if (Integer.signum(num) == 1 || numfinal > 0) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                     dataListPlantilla[i]
                                 )
                             ) //decimales con punto //get temp positivo
                             GetHexFromRealDataOxxoDisplay.A6 =
                                 GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                     dataListPlantilla[i]
                                 )
                         } else if (Integer.signum(num) == -1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.getNeg(
                                     numfinal
                                 )
                             ) //get negativos
                             GetHexFromRealDataOxxoDisplay.A6 = GetHexFromRealDataOxxoDisplay.getNeg(
                                 numfinal
                             )
                         } else { //Es 0 cero
                             GetHexFromRealDataOxxoDisplay.A6 = "0000"
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000") //get negativos
                         }
                     }

                     13 -> {
                         val numf = dataListPlantilla[i].toFloat()
                         val numfinal = dataListPlantilla[i].toFloat()
                         var num: Int = if (numfinal < 0 && numfinal > -1.0) {
                             -1
                         } else numf.toInt()
                         //Log.d("tag",":"+numfi);
                         Log.d("tag", "2:$numfinal")
                         if (Integer.signum(num) == 1 || numfinal > 0) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                     dataListPlantilla[i]
                                 )
                             ) //decimales con punto //get temp positivo
                             GetHexFromRealDataOxxoDisplay.A7 =
                                 GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                     dataListPlantilla[i]
                                 )
                         } else if (Integer.signum(num) == -1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.getNeg(
                                     numfinal
                                 )
                             ) //get negativos
                             GetHexFromRealDataOxxoDisplay.A7 = GetHexFromRealDataOxxoDisplay.getNeg(
                                 numfinal
                             )
                         } else { //Es 0 cero
                             GetHexFromRealDataOxxoDisplay.A7 = "0000"
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000") //get negativos
                         }
                     }

                     14 -> {
                         val numf = dataListPlantilla[i].toFloat()
                         val numfinal = dataListPlantilla[i].toFloat()
                         var num: Int = if (numfinal < 0 && numfinal > -1.0) {
                             -1
                         } else numf.toInt()
                         //Log.d("tag",":"+numfi);
                         Log.d("tag", "2:$numfinal")
                         if (Integer.signum(num) == 1 || numfinal > 0) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                     dataListPlantilla[i]
                                 )
                             ) //decimales con punto //get temp positivo
                         } else if (Integer.signum(num) == -1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.getNeg(
                                     numfinal
                                 )
                             ) //get negativos
                         } else { //Es 0 cero
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000") //get negativos
                         }
                     }

                     15 -> {
                         val numf = dataListPlantilla[i].toFloat()
                         val numfinal = dataListPlantilla[i].toFloat()
                         var num: Int = if (numfinal < 0 && numfinal > -1.0) {
                             -1
                         } else numf.toInt()
                         //Log.d("tag",":"+numfi);
                         Log.d("tag", "2:$numfinal")
                         if (Integer.signum(num) == 1 || numfinal > 0) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.convertDecimalToHexa(
                                     dataListPlantilla[i]
                                 )
                             ) //decimales con punto //get temp positivo
                         } else if (Integer.signum(num) == -1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.getNeg(
                                     numfinal
                                 )
                             ) //get negativos
                         } else { //Es 0 cero
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000") //get negativos
                         }
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("00000000")
                         /**AQUI VAN LOS DATOS DE RESPALDO */
                         val stringBuilder = GetHexFromRealDataOxxoDisplay.T0 +
                                 GetHexFromRealDataOxxoDisplay.T1 +
                                 GetHexFromRealDataOxxoDisplay.A6 +
                                 GetHexFromRealDataOxxoDisplay.A7
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(stringBuilder)
                     }

                     16 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("66") //dato de seguridad
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalIntToHexa(
                                 dataListPlantilla[i]
                             )
                         ) //decimales sin punto
                     }

                     17 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalIntToHexa(
                                 dataListPlantilla[i]
                             )
                         ) //decimales sin punto
                     }

                     18 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalIntToHexa(
                                 dataListPlantilla[i]
                             )
                         ) //decimales sin punto
                     }

                     19 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalIntToHexa(
                                 dataListPlantilla[i]
                             )
                         ) //decimales sin punto
                     }

                     20 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalIntToHexa(
                                 dataListPlantilla[i]
                             )
                         ) //decimales sin punto
                     }

                     21 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalIntToHexa(
                                 dataListPlantilla[i]
                             )
                         ) //decimales sin punto
                     }

                     22 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalIntToHexa(
                                 dataListPlantilla[i]
                             )
                         ) //decimales sin punto
                     }

                     23 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalIntToHexa(
                                 dataListPlantilla[i]
                             )
                         ) //decimales sin punto
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("00")
                     }

                     24 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalIntToHexa(
                                 dataListPlantilla[i]
                             )
                         ) //decimales sin punto
                     }

                     25 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalIntToHexa(
                                 dataListPlantilla[i]
                             )
                         ) //decimales sin punto
                     }

                     26 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalIntToHexa(
                                 dataListPlantilla[i]
                             )
                         ) //decimales sin punto
                     }

                     27 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalIntToHexa(
                                 dataListPlantilla[i]
                             )
                         ) //decimales sin punto
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("000000")
                     }

                     28 -> {
                         //C0
                         if (GetHexFromRealDataOxxoDisplay.decimalToHex(dataListPlantilla[i].toLong()).length == 1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 "0" + GetHexFromRealDataOxxoDisplay.decimalToHex(
                                     dataListPlantilla[i].toLong()
                                 )
                             ) //swith
                         } else GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.decimalToHex(
                                 dataListPlantilla[i].toLong()
                             )
                         ) //swith
                     }

                     29 -> {
                         //C1 banderas de configuracion
                         if (GetHexFromRealDataOxxoDisplay.decimalToHex(dataListPlantilla[i].toLong()).length == 1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 "0" + GetHexFromRealDataOxxoDisplay.decimalToHex(
                                     dataListPlantilla[i].toLong()
                                 )
                             ) //switch C1
                         } else GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.decimalToHex(
                                 dataListPlantilla[i].toLong()
                             )
                         ) //switch C1
                     }

                     30 -> {
                         //c2
                         if (GetHexFromRealDataOxxoDisplay.decimalToHex(dataListPlantilla[i].toLong()).length == 1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 "0" + GetHexFromRealDataOxxoDisplay.decimalToHex(
                                     dataListPlantilla[i].toLong()
                                 )
                             ) //swith
                         } else GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.decimalToHex(
                                 dataListPlantilla[i].toLong()
                             )
                         ) //swith
                     }

                     31 -> {
                         //c3
                         if (GetHexFromRealDataOxxoDisplay.decimalToHex(dataListPlantilla[i].toLong()).length == 1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 "0" + GetHexFromRealDataOxxoDisplay.decimalToHex(
                                     dataListPlantilla[i].toLong()
                                 )
                             ) //swith
                         } else GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.decimalToHex(
                                 dataListPlantilla[i].toLong()
                             )
                         ) //swith
                     }

                     32 -> {
                         //c4
                         if (GetHexFromRealDataOxxoDisplay.decimalToHex(dataListPlantilla[i].toLong()).length == 1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 "0" + GetHexFromRealDataOxxoDisplay.decimalToHex(
                                     dataListPlantilla[i].toLong()
                                 )
                             ) //swith
                         } else GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.decimalToHex(
                                 dataListPlantilla[i].toLong()
                             )
                         ) //swith
                     }

                     33 -> {
                         //C5
                         if (GetHexFromRealDataOxxoDisplay.decimalToHex(dataListPlantilla[i].toLong()).length == 1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 "0" + GetHexFromRealDataOxxoDisplay.decimalToHex(
                                     dataListPlantilla[i].toLong()
                                 )
                             ) //swith
                         } else GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.decimalToHex(
                                 dataListPlantilla[i].toLong()
                             )
                         ) //swith
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("00")
                     }

                     34 -> {
                         //c7
                         if (GetHexFromRealDataOxxoDisplay.decimalToHex(dataListPlantilla[i].toLong()).length == 1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 "0" + GetHexFromRealDataOxxoDisplay.decimalToHex(
                                     dataListPlantilla[i].toLong()
                                 )
                             ) //swith
                         } else GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.decimalToHex(
                                 dataListPlantilla[i].toLong()
                             )
                         ) //swith
                     }

                     35 -> {
                         //c8
                         if (GetHexFromRealDataOxxoDisplay.decimalToHex(dataListPlantilla[i].toLong()).length == 1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 "0" + GetHexFromRealDataOxxoDisplay.decimalToHex(
                                     dataListPlantilla[i].toLong()
                                 )
                             ) //swith
                         } else GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.decimalToHex(
                                 dataListPlantilla[i].toLong()
                             )
                         ) //swith
                     }

                     36 -> {
                         //c9
                         if (GetHexFromRealDataOxxoDisplay.decimalToHex(dataListPlantilla[i].toLong()).length == 1) {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 "0" + GetHexFromRealDataOxxoDisplay.decimalToHex(
                                     dataListPlantilla[i].toLong()
                                 )
                             ) //swith
                         } else GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.decimalToHex(
                                 dataListPlantilla[i].toLong()
                             )
                         ) //swith
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000000000")
                         /**
                          * Se agrega el parámetro CF que solo se edita mediante Firmware, se manda en 0x00
                          */
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("00")
                     }

                     37 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalToHexa8(
                                 dataListPlantilla[i]
                             )
                         ) //decimales con punto
                     }

                     38 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalToHexa8(
                                 dataListPlantilla[i]
                             )
                         ) //decimales con punto
                     }

                     39 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalToHexa8(
                                 dataListPlantilla[i]
                             )
                         ) //decimales con punto
                     }

                     40 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalToHexa8(
                                 dataListPlantilla[i]
                             )
                         ) //decimales con punto
                     }

                     41 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalToHexa8(
                                 dataListPlantilla[i]
                             )
                         ) //decimales con punto
                     }

                     42 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalToHexa8(
                                 dataListPlantilla[i]
                             )
                         ) //decimales con punto
                     }

                     43 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalToHexa8(
                                 dataListPlantilla[i]
                             )
                         ) //decimales con punto
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("000000000000")
                     }

                     44 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalToHexa8(
                                 dataListPlantilla[i]
                             )
                         ) //decimales con punto
                     }

                     45 -> {
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalToHexa8(
                                 dataListPlantilla[i]
                             )
                         ) //decimales con punto
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("00")
                     }

                     46 -> {
                         //modbus
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalIntToHexa(
                                 dataListPlantilla[i]
                             )
                         ) //decimales sin punto
                     }

                     47 -> {
                         //password
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalIntToHexa(
                                 dataListPlantilla[i]
                             )
                         ) //decimales sin punto
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("0000000000")
                     }

                     48 -> {
                         //modelo
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.convertDecimalToHexa8(
                                 dataListPlantilla[i]
                             )
                         ) //decimales con punto
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("00")
                     }

                     49 -> {
                         /**
                          * Pensar en esto màs adelante cuando se empiece a dividir la app en permisos o en dos apps, pues este valor es candado para no poder agregar plantillas a versiones viejas excepto que sea superusuario
                          * Por ahora:Permitir al usuario que edita la plantilla que edite la version
                          */
                         val s = dataListPlantilla[i].replace(".", "")
                         var s2: String
                         var s3: String
                         var s4: String
                         if (s.length != 4) {
                             val index = dataListPlantilla[i].indexOf(".")
                             s2 = if (index == 1) {
                                 "0" + dataListPlantilla[i]
                             } else {
                                 dataListPlantilla[i]
                             }
                             s4 = dataListPlantilla[i].substring(index + 1)
                             s3 = if (s4.length == 1) { //el numero es menor a dos dígitos
                                 dataListPlantilla[i] + "0"
                             } else {
                                 dataListPlantilla[i]
                             }
                             /*Log.d("TEST FIRM","s:"+s);
                             Log.d("TEST FIRM","s2:"+s2);
                             Log.d("TEST FIRM","s3:"+s3);

                             Log.d("TEST FIRM","s22;"+s2.substring(0,2));
                             Log.d("TEST FIRM","s32:"+s3.substring(2));

                             Log.d("TEST FIRM","FINAL:"+s2.substring(0,2)+s3.substring(2));*/GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.convertDecimalToHexaFirmwareVersion8(
                                     s2.substring(
                                         0,
                                         2
                                     )
                                 )
                             )
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.convertDecimalToHexaFirmwareVersion8(
                                     s3.substring(2)
                                 )
                             )
                         } else {
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.convertDecimalToHexaFirmwareVersion8(
                                     s.substring(
                                         0,
                                         2
                                     )
                                 )
                             )
                             GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                                 GetHexFromRealDataOxxoDisplay.convertDecimalToHexaFirmwareVersion8(
                                     s.substring(2)
                                 )
                             )
                         }
                     }

                     50 -> {

                         //String s = dataListPlantilla.get(i).replace(".","");
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("00" + GetHexFromRealDataOxxoDisplay.convertDecimalToHexa8(
                             dataListPlantilla[i]
                         )
                         ) //decimales sin punto
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add("CC")
                         GetHexFromRealDataOxxoDisplay.arrayListInfo.add(
                             GetHexFromRealDataOxxoDisplay.calculateChacksum(
                                 GetHexFromRealDataOxxoDisplay.arrayListInfo
                             )
                         ) //CHECKSUM
                     }

                 */
            }
            i++
        } while (i < dataListPlantilla.size)
        Log.d(
            GetHexFromRealDataOxxoDisplay.TAG,
            "DATOS A ENVIAR:" + GetHexFromRealDataOxxoDisplay.arrayListInfo
        )
        return GetHexFromRealDataOxxoDisplay.arrayListInfo
    }

    private fun decimalToHex(decimal: Double): String {
        val absoluteValue = abs(decimal)
        val integerPart = absoluteValue.toInt()
        val fractionalPart = absoluteValue - integerPart

        val hexInteger = Integer.toHexString(integerPart)
        val hexFractional = convertFractionalToHex(fractionalPart)

        return "0x$hexInteger$hexFractional"
    }

    private fun convertFractionalToHex(fraction: Double): String {
        val result = StringBuilder()
        var remaining = fraction

        repeat(4) {
            remaining *= 16
            val digit = remaining.toInt()
            val hexDigit = Integer.toHexString(digit)
            result.append(hexDigit)
            remaining -= digit
        }

        return result.toString()
    }


    fun checkGooglePlayServicesGEO(callback: (latitude: Float, longitude: Float) -> Unit) {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(getContext!!)

        if (resultCode == ConnectionResult.SUCCESS) {
            // getDeviceLocation()
            getDeviceLocation { latitude, longitude ->
                //  callback( latitude.toFloat(), longitude.toFloat())//latitude, longitude)
                callback(
                    String.format("%.2f", latitude).toFloat(),
                    String.format("%.2f", longitude).toFloat()
                ) //String.format("%.2f", longitude).toFloat())//latitude, longitude)
                //  Log.d("MainActivity", "->>>>> Latitude: $latitude  ${String.format("%.2f", latitude)}, Longitude: $longitude")
            }

        } else {
            Log.e("MainActivity", "Servicios de Google Play no disponibles")
        }
    }


    @SuppressLint("MissingPermission")
    fun getDeviceLocation(callback: (latitude: Double, longitude: Double) -> Unit) {
        val fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(getContext as Activity)

        try {
            val locationResult = fusedLocationProviderClient.lastLocation

            locationResult.addOnCompleteListener(getContext as Activity) { task ->
                if (task.isSuccessful && task.result != null) {
                    val location = task.result
                    val latitude = location.latitude.toFloat()
                    val longitude = location.longitude.toFloat()

                    String.format("%.4f", latitude)
                    callback(
                        String.format("%.6f", latitude).toDouble(),
                        String.format("%.6f", longitude).toDouble()
                    )
                } else {
                    Log.e("MainActivity", "No se pudo obtener la ubicación del dispositivo")
                }
            }
        } catch (e: SecurityException) {
            Log.e("MainActivity", e.message ?: "Excepcion de seguridad")
        }
    }


    interface OnRangeCheckListener {
        fun onRangeChecked(isWithinRange: Boolean): Boolean
    }

    fun isWithinRange(
        context: Context,
        newLatitude: Double,
        newLongitude: Double,
        ControlLatitude: Double,
        Controllongitude: Double,
        listener: OnRangeCheckListener
    ) {
        val referenceLocation = Location("reference")
        //   formatted_latitude = "{:.2f}".format(ControlLatitude)
        referenceLocation.latitude = formatDoubleWithDecimals(
            ControlLatitude,
            4
        ) // "{:.2f}".format(ControlLatitude).toDouble() //ControlLatitude // 19.3910
        referenceLocation.longitude = formatDoubleWithDecimals(
            Controllongitude,
            4
        )// "{:.2f}".format(Controllongitude).toDouble()  //Controllongitude// -99.1809

        val newLocation = Location("new")
        newLocation.latitude = formatDoubleWithDecimals(
            newLatitude,
            4
        )// "{:.2f}".format(newLatitude).toDouble()  //newLatitude
        newLocation.longitude = formatDoubleWithDecimals(
            newLongitude,
            4
        ) //"{:.2f}".format(newLongitude).toDouble()  //newLongitude

        val distance = newLocation.distanceTo(referenceLocation)

        val isWithinRange = distance <= 100
        Log.d(
            "checkGooglePlayServices",
            "distance $distance new location $newLocation referenceLocation $referenceLocation isWithinRange $isWithinRange"
        )

        listener.onRangeChecked(isWithinRange)
        /*  val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

          fusedLocationClient.lastLocation.addOnSuccessListener { location ->
              if (location != null) {
                  val currentLocation = Location("current")
                  currentLocation.latitude = location.latitude
                  currentLocation.longitude = location.longitude

                  val distance = currentLocation.distanceTo(newLocation)
                  Log.d("checkGooglePlayServices", "distance $distance new location $newLocation referenceLocation $referenceLocation")

                  val isWithinRange = distance <= 0.1
                  listener.onRangeChecked(isWithinRange)
              }
          }
          */
    }

    fun isWithinRange6(
        context: Context,
        newLatitude: Double,
        newLongitude: Double,
        ControlLatitude: Double,
        Controllongitude: Double,
        listener: OnRangeCheckListener
    ) {
        val referenceLocation = Location("reference")
        //   formatted_latitude = "{:.2f}".format(ControlLatitude)
        referenceLocation.latitude = formatDoubleWithDecimals(
            ControlLatitude,
            6
        ) // "{:.2f}".format(ControlLatitude).toDouble() //ControlLatitude // 19.3910
        referenceLocation.longitude = formatDoubleWithDecimals(
            Controllongitude,
            6
        )// "{:.2f}".format(Controllongitude).toDouble()  //Controllongitude// -99.1809

        val newLocation = Location("new")
        newLocation.latitude = formatDoubleWithDecimals(
            newLatitude,
            6
        )// "{:.2f}".format(newLatitude).toDouble()  //newLatitude
        newLocation.longitude = formatDoubleWithDecimals(
            newLongitude,
            6
        ) //"{:.2f}".format(newLongitude).toDouble()  //newLongitude

        val distance = newLocation.distanceTo(referenceLocation)

        val isWithinRange = distance <= 100
        Log.d(
            "checkGooglePlayServices",
            "distance $distance new location $newLocation referenceLocation $referenceLocation isWithinRange $isWithinRange"
        )

        listener.onRangeChecked(isWithinRange)
        /*  val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

          fusedLocationClient.lastLocation.addOnSuccessListener { location ->
              if (location != null) {
                  val currentLocation = Location("current")
                  currentLocation.latitude = location.latitude
                  currentLocation.longitude = location.longitude

                  val distance = currentLocation.distanceTo(newLocation)
                  Log.d("checkGooglePlayServices", "distance $distance new location $newLocation referenceLocation $referenceLocation")

                  val isWithinRange = distance <= 0.1
                  listener.onRangeChecked(isWithinRange)
              }
          }
          */
    }

    fun formatDoubleWithTwoDecimals(number: Double): Double {

        val df = DecimalFormat("#.##")
        val formattedString = df.format(number)
        return formattedString.toDouble()
    }

    fun formatDoubleWithDecimals(number: Double, decimals: Int): Double {
        val factor = 10.0.pow(decimals)
        return floor(number * factor) / factor
    }

    fun isWithinRange1(newLatitude: Double, newLongitude: Double): Boolean {
        val referenceLocation = Location("reference")
        referenceLocation.latitude = 19.3910
        referenceLocation.longitude = -99.1809

        val newLocation = Location("new")
        newLocation.latitude = newLatitude
        newLocation.longitude = newLongitude

        val distance = referenceLocation.distanceTo(newLocation)

        Log.d(
            "checkGooglePlayServices",
            "distance $distance new location $newLocation  referenceLocation $referenceLocation"
        )
        return distance <= 100
    }

    /*  fun MyHNSD(callback: MyCallback){
          val scope = CoroutineScope(Dispatchers.Default)
          scope.launch {
              val pre = withContext(Dispatchers.Default) {
                  runOnUiThread {
                      callback.onProgress("Iniciando primera comunicacion")
                  }
                  delay(100)
              }
              val result = withContext(Dispatchers.Default) {
                  runOnUiThread {
                      callback.onProgress("Realizando primera comunicacion")
                  }
                      if (bluetoothLeService!!.sendFirstComando("4021")) {
                          listData.clear()
                          delay(500)
                          listData =
                              bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                          //  Log.d("MyAsyncTaskGetHandshakeFUN", "dataChecksum total:7 ok ")
                          return@withContext "ok"
                      } else //Log.d("MyAsyncTaskGetHandshakeFUN", "dataChecksum total:8 not ok ")
                          return@withContext "not"
                  }

              val post = withContext(Dispatchers.Default) {
                  runOnUiThread {
                      callback.onProgress("Realizando primera comunicacion")
                  }
                  delay(500)
                  Log.d("MyAsyncTaskGetHandshakeFUN","--------------------->>>>>>>>>>> $result")
                  if (result == "noconnected") {
                      callback.onError("Problemas de conexión, reconecta a tu BLE")
                  }else{
                      if (!listData!!.isEmpty()) {

                          callback.getInfo(listData as MutableList<String>)
                          val isChecksumOk = GlobalTools.checkChecksum(
                              GetRealDataFromHexaImbera.cleanSpace(listData as List<String>)
                                  .toString()
                          )
                          when (isChecksumOk) {
                              "ok" -> {
                                  FinalListData =
                                      (GetRealDataFromHexaImbera.convert(
                                          listData as List<String>,
                                          "Handshake",
                                          "",
                                          ""
                                      ) as MutableList<String>).toMutableList()
                                  listData = GetRealData(
                                      FinalListData as List<String>,
                                      "Handshake",
                                      "",
                                      ""
                                  ) as MutableList<String?>
                                  callback.onSuccess(true)
                                  esp!!.putString("modelo", listData[1])
                                  esp!!.putString("numversion", listData[2])
                                  esp!!.putString("plantillaVersion", listData[3])
                                  esp!!.putString("trefpVersionName", name)
                                  esp!!.apply()
                                  Log.d(
                                      "trefpVersionName",
                                      "${sp!!.getString("trefpVersionName", "")}"
                                  )
                              }

                              "notFirmware" -> {
                                  //Toast.makeText(getContext(), "", Toast.LENGTH_SHORT).show();
                                  callback.onError("notFirmware")
                                  callback.onSuccess(false)
                                  GlobalTools.showInfoPopup(
                                      "Información del equipo",
                                      "Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (NFW)",
                                      getContext!!
                                  )
                                  esp!!.putString("trefpVersionName", "")
                                  esp!!.putString("numversion", "")
                                  esp!!.putString("plantillaVersion", "")
                                  esp!!.apply()
                                  //    desconectar()//  desconectarBLE()
                              }

                              "notok" -> {
                                  runOnUiThread {
                                      GlobalTools.showInfoPopup(
                                          "Información del equipo",
                                          "Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (CHKSM)",
                                          getContext!!
                                      )
                                  }
                                  callback.onError("intenta de nuevo o contacta a personal autorizado. (CHKSM)")

                                  esp!!.putString("trefpVersionName", "")
                                  esp!!.putString("numversion", "")
                                  esp!!.putString("plantillaVersion", "")
                                  esp!!.apply()
                                  //    desconectar()
                                  callback.onSuccess(false)
                              }

                              else -> {}
                          }
                          //}
                      } else {
                          /*  Toast.makeText(
                                getContext!!,
                                "No se pudo obtener primera comunicación",
                                Toast.LENGTH_SHORT
                            ).show()*/
                          esp!!.putString("trefpVersionName", "")
                          esp!!.putString("numversion", "")
                          esp!!.putString("plantillaVersion", "")
                          esp!!.apply()
                          // desconectar()
                          callback.onSuccess(false)
                      }
                  }
              }
                  delay(100)
              }
          }
  */


    fun MyHNSD(callback: MyCallback) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                showProgress("Iniciando primera comunicacion", callback)
                delay(100)

                val result = withContext(Dispatchers.Default) {
                    showProgress("Realizando primera comunicacion", callback)
                    if (bluetoothLeService?.sendFirstComando("4021") == true) {
                        listData.clear()
                        delay(500)
                        listData.addAll(
                            bluetoothLeService?.getDataFromBroadcastUpdate() ?: emptyList()
                        )
                        return@withContext "ok"
                    } else {
                        return@withContext "not"
                    }
                }

                //  showProgress("Realizando primera comunicacion",callback)
                delay(500)

                if (result == "noconnected") {
                    callback.onError("Problemas de conexión, reconecta a tu BLE")
                } else {
                    if (listData.isNotEmpty()) {
                        handleSuccess(callback)
                    } else {
                        handleFailure(callback)
                    }
                }
            } catch (e: Exception) {
                callback.onError("Error inesperado: ${e.message}")
            }
        }
    }

    private suspend fun showProgress(message: String, callback: MyCallback) {
        withContext(Dispatchers.Main) {
            callback.onProgress(message)
        }
    }

    private suspend fun handleSuccess(callback: MyCallback) {
        callback.getInfo(listData as MutableList<String>)
        val isChecksumOk = GlobalTools.checkChecksum(
            GetRealDataFromHexaImbera.cleanSpace(listData as List<String>).toString()
        )

        when (isChecksumOk) {
            "ok" -> {
                handleChecksumOk(callback)
            }

            "notFirmware" -> {
                handleNotFirmware(callback)
            }

            "notok" -> {
                handleNotOk(callback)
            }
        }
    }

    private fun handleChecksumOk(callback: MyCallback) {
        FinalListData = (GetRealDataFromHexaImbera.convert(
            listData as List<String>,
            "Handshake",
            "",
            ""
        ) as MutableList<String>).toMutableList()
        listData =
            GetRealData(FinalListData as List<String>, "Handshake", "", "") as MutableList<String?>

        callback.onSuccess(true)

        esp?.apply {
            putString("modelo", listData[1])
            putString("numversion", listData[2])
            putString("plantillaVersion", listData[3])
            putString("trefpVersionName", name)
            apply()
        }

        Log.d("trefpVersionName", "${sp?.getString("trefpVersionName", "")}")
    }

    private suspend fun handleNotFirmware(callback: MyCallback) {
        callback.onError("notFirmware")
        callback.onSuccess(false)
        showInfoPopup(
            "Información del equipo",
            "Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (NFW)"
        )
        esp?.apply {
            putString("trefpVersionName", "")
            putString("numversion", "")
            putString("plantillaVersion", "")
            apply()
        }
    }

    private suspend fun handleNotOk(callback: MyCallback) {
        showInfoPopup(
            "Información del equipo",
            "Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (CHKSM)"
        )
        callback.onError("intenta de nuevo o contacta a personal autorizado. (CHKSM)")
        esp?.apply {
            putString("trefpVersionName", "")
            putString("numversion", "")
            putString("plantillaVersion", "")
            apply()
        }
        callback.onSuccess(false)
    }

    private suspend fun handleFailure(callback: MyCallback) {
        esp?.apply {
            putString("trefpVersionName", "")
            putString("numversion", "")
            putString("plantillaVersion", "")
            apply()
        }
        callback.onSuccess(false)
    }

    private suspend fun showInfoPopup(title: String, message: String) {
        withContext(Dispatchers.Main) {
            GlobalTools.showInfoPopup(title, message, getContext!!)
        }
    }


    fun cMyAsyncTaskGetHandshake(callback: MyCallback) {
        val isEVENTOK = AtomicBoolean(false)
        val isTIMEOK = AtomicBoolean(false)

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            // Operaciones dentro de la corrutina

            val pre = withContext(Dispatchers.Default) {
                runOnUiThread {
                    // getInfoList()?.clear()
                    callback.onProgress("Iniciando primera comunicacion")
                }

            }
            val result = withContext(Dispatchers.Default) {

                callback.onProgress("Realizando primera comunicacion")

                return@withContext if (/*sp!!.getBoolean("isconnected", false)*/ ValidaConexion()) {
                    //  getBluetoothLeService()
                    delay(850)


                    if (bluetoothLeService!!.sendFirstComando("4021")) {
                        //  Log.d("MyAsyncTaskGetHandshakeFUN", "dataChecksum total:7 ok ")
                        return@withContext "ok"
                    } else //Log.d("MyAsyncTaskGetHandshakeFUN", "dataChecksum total:8 not ok ")
                        return@withContext "not"
                } else return@withContext "noconnected"

            }

            val post = withContext(Dispatchers.Default) {
                delay(500)
                Log.d("MyAsyncTaskGetHandshakeFUN", "--------------------->>>>>>>>>>> $result")
                if (result == "noconnected") {

                    callback.onError("Problemas de conexión, reconecta a tu BLE")
                } else {

                    if (result == "ok") {
                        var listData: MutableList<String?> = java.util.ArrayList()
                        var FinalListData: MutableList<String> = java.util.ArrayList()
                        //   bluetoothLeService!!.sendFirstComando("4021")
                        //delay(500)

                        listData =
                            bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String?>
                        Log.d(
                            "MyAsyncTaskGetHandshakeFUN",
                            "-------$listData "
                        )
                        if (!listData!!.isEmpty()) {

                            callback.getInfo(listData as MutableList<String>)
                            val isChecksumOk = GlobalTools.checkChecksum(
                                GetRealDataFromHexaImbera.cleanSpace(listData as List<String>)
                                    .toString()
                            )
                            when (isChecksumOk) {
                                "ok" -> {
                                    FinalListData =
                                        GetRealDataFromHexaImbera.convert(
                                            listData,
                                            "Handshake",
                                            "",
                                            ""
                                        ) as MutableList<String>
                                    listData = GetRealData(
                                        FinalListData,
                                        "Handshake",
                                        "",
                                        ""
                                    ) as MutableList<String?>
                                    callback.onSuccess(true)
                                    esp!!.putString("modelo", listData[1])
                                    esp!!.putString("numversion", listData[2])
                                    esp!!.putString("plantillaVersion", listData[3])
                                    esp!!.putString("trefpVersionName", name)
                                    esp!!.apply()
                                    Log.d(
                                        "trefpVersionName",
                                        "${sp!!.getString("trefpVersionName", "")}"
                                    )
                                }

                                "notFirmware" -> {
                                    //Toast.makeText(getContext(), "", Toast.LENGTH_SHORT).show();
                                    callback.onError("notFirmware")
                                    callback.onSuccess(false)
                                    GlobalTools.showInfoPopup(
                                        "Información del equipo",
                                        "Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (NFW)",
                                        getContext!!
                                    )
                                    esp!!.putString("trefpVersionName", "")
                                    esp!!.putString("numversion", "")
                                    esp!!.putString("plantillaVersion", "")
                                    esp!!.apply()
                                    //    desconectar()//  desconectarBLE()
                                }

                                "notok" -> {
                                    runOnUiThread {
                                        GlobalTools.showInfoPopup(
                                            "Información del equipo",
                                            "Tu control BLE no está respondiendo como se esperaba, intenta de nuevo o contacta a personal autorizado. (CHKSM)",
                                            getContext!!
                                        )
                                    }
                                    callback.onError("intenta de nuevo o contacta a personal autorizado. (CHKSM)")

                                    esp!!.putString("trefpVersionName", "")
                                    esp!!.putString("numversion", "")
                                    esp!!.putString("plantillaVersion", "")
                                    esp!!.apply()
                                    //    desconectar()
                                    callback.onSuccess(false)
                                }

                                else -> {}
                            }
                            //}
                        } else {
                            /*  Toast.makeText(
                                  getContext!!,
                                  "No se pudo obtener primera comunicación",
                                  Toast.LENGTH_SHORT
                              ).show()*/
                            esp!!.putString("trefpVersionName", "")
                            esp!!.putString("numversion", "")
                            esp!!.putString("plantillaVersion", "")
                            esp!!.apply()
                            // desconectar()
                            callback.onSuccess(false)
                        }
                    } else {
                        callback.onError("Fallo al conectar a un BLE")

                        //   desconectar()
                        callback.onSuccess(false)
                    }
                }
                callback.onProgress("Finalizando primera comunicacion")
            }

        }
    }

    fun GetRealData(
        data: List<String>,
        action: String?,
        fwversion: String,
        modelo: String
    ): List<String?>? {
        //USO SOLO DE LOS DATOS BUFFER IMPORTANTES PARA MOSTRARLOS EN PANTALLA, LAS POSICIONES RESTANTES (HEADER) SON CORRECTAS
        return when (action) {
            "Handshake" -> {
                val newData: MutableList<String?> = java.util.ArrayList()
                if (data.isEmpty()) {
                    //newData.add(getSameData(data.get(0),"trefpversion"));
                    newData.add("nullHandshake")
                } else {
                    //header
                    //newData.add(data.get(0));
                    newData.add(GetRealDataFromHexaImbera.hexToAscii(data[1]))
                    Log.d("dedos", ":" + data[2])
                    newData.add(GetRealDataFromHexaImbera.getSameData(data[2], action))
                    //newData.add(getSameData(data.get(3), "trefpversion"));
                    newData.add(
                        GetRealDataFromHexaImbera.getFwVersionFromHex(
                            data[3].substring(
                                0,
                                2
                            ), data[3].substring(2)
                        )
                    )


                    /*StringBuilder stringBuilder = new StringBuilder();
                           stringBuilder.append(GetRealDataFromHexaImbera.getDecimal(data.get(3).substring(0,2)));
                           stringBuilder.append(".");
                           Log.d("VERSION",":"+data.get(3).substring(2));
                           int version = GetRealDataFromHexaImbera.getDecimal(data.get(3).substring(2));
                           if (version<10){
                               stringBuilder.append("0"+String.valueOf(version));
                           }else{
                               stringBuilder.append(GetRealDataFromHexaImbera.getDecimal(data.get(3).substring(2)));
                           }
                           newData.add(stringBuilder.toString());*/newData.add(
                        GetRealDataFromHexaImbera.getDecimalFloat(
                            data[4]
                        ).toString()
                    ) // decimales con punto
                }
                newData
            }

            "Lectura de parámetros de operación" -> {
                val newData: MutableList<String?> = java.util.ArrayList()
                if (data.isEmpty()) {
                    //newData.add(getSameData(data.get(0),"trefpversion"));
                    newData.add("nullHandshake")
                } else {
                    //header
                    newData.add(GetRealDataFromHexaImbera.getSameData(data[0], "trefpversion"))
                    newData.add(GetRealDataFromHexaImbera.getSameData(data[1], action))
                    newData.add(GetRealDataFromHexaImbera.getDecimal(data[2]).toString())
                    newData.add(GetRealDataFromHexaImbera.getDecimal(data[3]).toString())

                    //buffer
                    var i = 4
                    do {
                        if (i == 42 || i == 17 || i == 18 || i == 19 || i == 20 || i == 21 || i == 13 || i == 14 || i == 15 || i == 16 || i == 43 || i == 27) {
                            newData.add(
                                GetRealDataFromHexaImbera.getDecimal(data[i]).toString()
                            ) //decimales sin punto
                        } else {
                            if (i == 4 || i == 6 || i == 7 || i == 8 || i == 12 || i == 11) {
                                //comprobar si es popsitivo
                                val j =
                                    GetRealDataFromHexaImbera.getDecimalFloat(data[i]) // decimales con punto
                                if (j > 99.9) {
                                    //Extraccion de temperaturas en decimales
                                    newData.add(GetRealDataFromHexaImbera.getNegativeTemp("FFFF" + data[i]))
                                } else {
                                    newData.add(
                                        GetRealDataFromHexaImbera.getDecimalFloat(data[i])
                                            .toString()
                                    ) // decimales con punto
                                }
                            } else if (i == 22) {
                                //Extraccion opciones segùn bit usado
                                newData.add(
                                    GetRealDataFromHexaImbera.getOptionSpinner(
                                        data[i],
                                        "mododeshielo"
                                    )
                                )
                            } else if (i == 23) {
                                //Extraccion opciones segùn bit usado
                                newData.add(
                                    GetRealDataFromHexaImbera.getOptionSpinner(
                                        data[i],
                                        "funcionesControl"
                                    )
                                )
                            } else if (i == 24) {
                                //Extraccion opciones segùn bit usado
                                newData.add(
                                    GetRealDataFromHexaImbera.getOptionSpinner(
                                        data[i],
                                        "funcionesdeshielo"
                                    )
                                )
                            } else if (i == 25) {
                                //Extraccion opciones segùn bit usado
                                newData.add(
                                    GetRealDataFromHexaImbera.getOptionSpinner(
                                        data[i],
                                        "funcionesventilador"
                                    )
                                )
                            } else if (i == 26) {
                                //Extraccion opciones segùn bit usado
                                newData.add(
                                    GetRealDataFromHexaImbera.getOptionSpinner(
                                        data[i],
                                        "funcionesvoltaje"
                                    )
                                )
                            } else {
                                newData.add(
                                    GetRealDataFromHexaImbera.getDecimalFloat(data[i]).toString()
                                ) // decimales con punto
                            }
                        }
                        i++
                    } while (i < data.size)
                }
                newData
            }

            "Lectura de datos tipo Tiempo real" -> {
                val newData: MutableList<String?> = java.util.ArrayList()
                if (data.isEmpty()) {
                    //newData.add(getSameData(data.get(0),"trefpversion"));
                    newData.add("nullHandshake")
                } else {
                    //header
                    /*newData.add(getSameData(data.get(0), "trefpversion"));
                           newData.add(getSameData(data.get(1), action));
                           newData.add(getSameData(data.get(2), action));
                           newData.add(getSameData(data.get(3), action));

                            */
                    //buffer
                    if (fwversion == "1.04") {
                        var numf = GetRealDataFromHexaImbera.getDecimalFloat(data[4])
                        //numf = getDecimalFloat("FFCE");
                        var num = numf.toInt()
                        if (num < 99.99) {
                            newData.add(
                                GetRealDataFromHexaImbera.getDecimalFloat(data[4]).toString()
                            ) //decimales con punto //get temp positivo
                        } else if (num > 99.99) {
                            //newData.add(getNegativeTemp("FFFF"+data.get(i).substring(22,26))); //get negativos
                            newData.add(GetRealDataFromHexaImbera.getNegativeTemp("FFFF" + data[4])) //get negativos
                        } else { //Es 0 cero
                            newData.add("0000") //get negativos
                        }
                        numf = GetRealDataFromHexaImbera.getDecimalFloat(data[5])
                        num = numf.toInt()
                        if (num < 99.99) {
                            newData.add(
                                GetRealDataFromHexaImbera.getDecimalFloat(data[5]).toString()
                            ) //decimales con punto //get temp positivo
                        } else if (num > 99.99) {
                            newData.add(GetRealDataFromHexaImbera.getNegativeTemp("FFFF" + data[5])) //get negativos
                        } else { //Es 0 cero
                            newData.add("0000") //get negativos
                        }
                        numf = GetRealDataFromHexaImbera.getDecimalFloat(data[6])
                        num = numf.toInt()
                        if (num < 99.99) {
                            newData.add(
                                GetRealDataFromHexaImbera.getDecimalFloat(data[6]).toString()
                            ) //decimales con punto //get temp positivo
                        } else if (num > 99.99) {
                            newData.add(GetRealDataFromHexaImbera.getNegativeTemp("FFFF" + data[6])) //get negativos
                        } else { //Es 0 cero
                            newData.add("0000") //get negativos
                        }
                        numf = GetRealDataFromHexaImbera.getDecimalFloat(data[7])
                        num = numf.toInt()
                        if (num < 99.99) {
                            newData.add(
                                GetRealDataFromHexaImbera.getDecimalFloat(data[7]).toString()
                            ) //decimales con punto //get temp positivo
                        } else if (num > 99.99) {
                            newData.add(GetRealDataFromHexaImbera.getNegativeTemp("FFFF" + data[7])) //get negativos
                        } else { //Es 0 cero
                            newData.add("0000") //get negativos
                        }
                        newData.add(
                            GetRealDataFromHexaImbera.getDecimal(data[8]).toString()
                        ) //voltage
                        newData.add(getActuador104(data[9]))
                        newData.add(getAlarma(data[10]))
                    } else {
                        var numf = GetRealDataFromHexaImbera.getDecimalFloat(data[4])
                        //numf = getDecimalFloat("FFCE");
                        var num = numf.toInt()
                        if (num < 99.99) {
                            newData.add(
                                GetRealDataFromHexaImbera.getDecimalFloat(data[4]).toString()
                            ) //decimales con punto //get temp positivo
                        } else if (num > 99.99) {
                            //newData.add(getNegativeTemp("FFFF"+data.get(i).substring(22,26))); //get negativos
                            newData.add(GetRealDataFromHexaImbera.getNegativeTemp("FFFF" + data[4])) //get negativos
                        } else { //Es 0 cero
                            newData.add("0000") //get negativos
                        }
                        //32*10 bits=320,000/9600

                        //115,000
                        //send rate
                        //33sec  115,000
                        //45sec
                        numf = GetRealDataFromHexaImbera.getDecimalFloat(data[5])
                        num = numf.toInt()
                        if (num < 99.99) {
                            newData.add(
                                GetRealDataFromHexaImbera.getDecimalFloat(data[5]).toString()
                            ) //decimales con punto //get temp positivo
                        } else if (num > 99.99) {
                            newData.add(GetRealDataFromHexaImbera.getNegativeTemp("FFFF" + data[5])) //get negativos
                        } else { //Es 0 cero
                            newData.add("0000") //get negativos
                        }

                        //newData.add(String.valueOf(getDecimalFloat(data.get(4)) ));//temp2
                        //newData.add(String.valueOf(getDecimalFloat(data.get(5)) ));//temp1
                        newData.add(
                            GetRealDataFromHexaImbera.getDecimal(data[6]).toString()
                        ) //voltage
                        newData.add(getActuador(data[7]))
                        newData.add(getAlarma(data[8]))
                        //newData.add(getSameData(data.get(9), "trefpversion")); // decimales con punto
                        //newData.add(hexToAscii(data.get(9)));
                    }
                }
                newData
            }

            "Lectura de datos tipo Tiempo" -> {
                val newData: MutableList<String?> = java.util.ArrayList()
                val header: MutableList<String> = java.util.ArrayList()
                //header
                if (data.isEmpty()) {
                    //newData.add(getSameData(data.get(0),"trefpversion"));
                    newData.add("nullHandshake")
                } else {
                    if (fwversion == "1.02" && modelo == "3.3" || fwversion == "1.04" && modelo == "3.5") {
                        //nuevo logger, diferente división de información
                        //header
                        header.add(GetRealDataFromHexaImbera.getSameData(data[0], "trefpversion"))
                        header.add(GetRealDataFromHexaImbera.getDecimal(data[1]).toString())
                        header.add(GetRealDataFromHexaImbera.getSameData(data[2], action))
                        header.add(GetRealDataFromHexaImbera.getSameData(data[3], action))

                        //buffer
                        var date: Date
                        var i = 4
                        //Log.d("PAQUETE",":"+data.get())
                        val timeStampOriginal = GetRealDataFromHexaImbera.getDecimal(
                            data[data.size - 1].substring(
                                0,
                                8
                            )
                        ).toLong() //getDecimal(data.get(data.size()-1).substring(0,8));
                        val unixTime = System.currentTimeMillis() / 1000
                        val diferencialTimeStamp = unixTime - timeStampOriginal
                        do {
                            val instant = Instant.ofEpochSecond(
                                GetRealDataFromHexaImbera.getDecimal(
                                    data[i].substring(0, 8)
                                ) + diferencialTimeStamp
                            )
                            date = Date.from(instant)
                            newData.add(date.toString()) //decimales sin punto
                            //decision de temperaturas positivas y negativas
                            var numf =
                                GetRealDataFromHexaImbera.getDecimalFloat(data[i].substring(8, 12))
                            var num = numf.toInt()
                            if (num < 99.99) {
                                newData.add(
                                    GetRealDataFromHexaImbera.getDecimalFloat(
                                        data[i].substring(
                                            8,
                                            12
                                        )
                                    ).toString()
                                ) //decimales con punto //get temp positivo
                            } else if (num > 99.99) {
                                newData.add(
                                    GetRealDataFromHexaImbera.getNegativeTemp(
                                        "FFFF" + data[i].substring(
                                            8,
                                            12
                                        )
                                    )
                                ) //get negativos
                            } else { //Es 0 cero
                                newData.add("0000") //get negativos
                            }
                            numf =
                                GetRealDataFromHexaImbera.getDecimalFloat(data[i].substring(12, 16))
                            num = numf.toInt()
                            if (num < 99.99) {
                                newData.add(
                                    GetRealDataFromHexaImbera.getDecimalFloat(
                                        data[i].substring(
                                            12,
                                            16
                                        )
                                    ).toString()
                                ) //decimales con punto //get temp positivo
                            } else if (num > 99.99) {
                                //newData.add(getNegativeTemp("FFFF"+data.get(i).substring(22,26))); //get negativos
                                newData.add(
                                    GetRealDataFromHexaImbera.getNegativeTemp(
                                        "FFFF" + data[i].substring(
                                            12,
                                            16
                                        )
                                    )
                                ) //get negativos
                            } else { //Es 0 cero
                                newData.add("0000") //get negativos
                            }
                            newData.add(
                                GetRealDataFromHexaImbera.getDecimal(data[i].substring(16))
                                    .toString()
                            ) //decimales sin punto
                            i++
                        } while (i < data.size)
                    } else {
                        //header
                        Log.d("GHDD", ":$data")
                        header.add(GetRealDataFromHexaImbera.getSameData(data[0], "trefpversion"))
                        header.add(GetRealDataFromHexaImbera.getDecimal(data[1]).toString())
                        header.add(GetRealDataFromHexaImbera.getSameData(data[2], action))
                        header.add(GetRealDataFromHexaImbera.getSameData(data[3], action))

                        //buffer
                        var date: Date
                        var i = 4
                        val timeStampOriginal = GetRealDataFromHexaImbera.getDecimal(
                            data[data.size - 1].substring(
                                0,
                                8
                            )
                        ).toLong() //getDecimal(data.get(data.size()-1).substring(0,8));
                        val unixTime = System.currentTimeMillis() / 1000
                        val diferencialTimeStamp = unixTime - timeStampOriginal
                        //612F6B42
                        //long f =
                        do {
                            if (i >= data.size) {
                                i = data.size //no interesa el checksum
                            } else {
                                val instant = Instant.ofEpochSecond(
                                    GetRealDataFromHexaImbera.getDecimal(
                                        data[i].substring(0, 8)
                                    ) + diferencialTimeStamp
                                )
                                date = Date.from(instant)
                                newData.add(date.toString()) //decimales sin punto
                                //decision de temperaturas positivas y negativas
                                var numf = GetRealDataFromHexaImbera.getDecimalFloat(
                                    data[i].substring(
                                        8,
                                        12
                                    )
                                )
                                var num = numf.toInt()
                                if (num < 99.99) {
                                    newData.add(
                                        GetRealDataFromHexaImbera.getDecimalFloat(
                                            data[i].substring(
                                                8,
                                                12
                                            )
                                        ).toString()
                                    ) //decimales con punto //get temp positivo
                                } else if (num > 99.99) {
                                    newData.add(
                                        GetRealDataFromHexaImbera.getNegativeTemp(
                                            "FFFF" + data[i].substring(
                                                8,
                                                12
                                            )
                                        )
                                    ) //get negativos
                                } else { //Es 0 cero
                                    newData.add("0000") //get negativos
                                }
                                numf = GetRealDataFromHexaImbera.getDecimalFloat(
                                    data[i].substring(
                                        12,
                                        16
                                    )
                                )
                                num = numf.toInt()
                                if (num < 99.99) {
                                    newData.add(
                                        GetRealDataFromHexaImbera.getDecimalFloat(
                                            data[i].substring(
                                                12,
                                                16
                                            )
                                        ).toString()
                                    ) //decimales con punto //get temp positivo
                                } else if (num > 99.99) {
                                    //newData.add(getNegativeTemp("FFFF"+data.get(i).substring(22,26))); //get negativos
                                    newData.add(
                                        GetRealDataFromHexaImbera.getNegativeTemp(
                                            "FFFF" + data[i].substring(
                                                12,
                                                16
                                            )
                                        )
                                    ) //get negativos
                                } else { //Es 0 cero
                                    newData.add("0000") //get negativos
                                }
                                //newData.add(String.valueOf(getDecimalFloat(data.get(i).substring(8,12)) ));
                                //newData.add(String.valueOf(getDecimalFloat(data.get(i).substring(12,16)) ));
                                newData.add(
                                    GetRealDataFromHexaImbera.getDecimal(
                                        data[i].substring(
                                            16
                                        )
                                    ).toString()
                                ) //decimales sin punto
                                i++

                                //25,349,176
                            }
                        } while (i < data.size)
                    }
                }
                Log.d("", "realdataHeadr:$header")
                Log.d("", "realdata:$newData")
                newData
            }

            "Lectura de datos tipo Evento" -> {
                val newData: MutableList<String?> = java.util.ArrayList()
                //header
                if (data.isEmpty()) {
                    //newData.add(getSameData(data.get(0),"trefpversion"));
                    newData.add("nullHandshake")
                } else {
                    if (fwversion == "1.02" && modelo == "3.3" || fwversion == "1.04" && modelo == "3.5") { //nuevo logger, diferente división de información
                        /*
                               newData.add(getSameData(data.get(0),"trefpversion"));
                               newData.add(getSameData(data.get(1),action));
                               newData.add(getSameData(data.get(2),action));
                               newData.add(getSameData(data.get(3),action));
                               */
                        //buffer
                        var date: Date
                        var date2: Date
                        var i = 4
                        val timeStampOriginal = GetRealDataFromHexaImbera.getDecimal(
                            data[data.size - 1].substring(
                                0,
                                8
                            )
                        ).toLong()
                        val timeStampOriginal2 = GetRealDataFromHexaImbera.getDecimal(
                            data[data.size - 1].substring(
                                8,
                                16
                            )
                        ).toLong() //getDecimal(data.get(data.size()-2).substring(8,16));
                        val unixTime = System.currentTimeMillis() / 1000
                        val diferencialTimeStamp = unixTime - timeStampOriginal
                        val diferencialTimeStamp2 = unixTime - timeStampOriginal2
                        do {
                            //Date
                            val instant = Instant.ofEpochSecond(
                                GetRealDataFromHexaImbera.getDecimal(
                                    data[i].substring(0, 8)
                                ) + diferencialTimeStamp
                            )
                            date = Date.from(instant)
                            newData.add(date.toString())
                            val instant2 = Instant.ofEpochSecond(
                                GetRealDataFromHexaImbera.getDecimal(
                                    data[i].substring(8, 16)
                                ) + diferencialTimeStamp2
                            )
                            date2 = Date.from(instant2)
                            newData.add(date2.toString())
                            newData.add(
                                getEventType(
                                    data[i].substring(
                                        16,
                                        18
                                    )
                                )
                            ) //evento type
                            var numf =
                                GetRealDataFromHexaImbera.getDecimalFloat(data[i].substring(18, 22))
                            var num = numf.toInt()
                            if (num < 99.99) {
                                newData.add(
                                    GetRealDataFromHexaImbera.getDecimalFloat(
                                        data[i].substring(
                                            18,
                                            22
                                        )
                                    ).toString()
                                ) //decimales con punto //get temp positivo
                            } else if (num > 99.99) {
                                newData.add(
                                    GetRealDataFromHexaImbera.getNegativeTemp(
                                        "FFFF" + data[i].substring(
                                            18,
                                            22
                                        )
                                    )
                                ) //get negativos
                            } else { //Es 0 cero
                                newData.add("0000") //get negativos
                            }
                            numf =
                                GetRealDataFromHexaImbera.getDecimalFloat(data[i].substring(22, 26))
                            //numf = getDecimalFloat("FFCE");
                            num = numf.toInt()
                            if (num < 99.99) {
                                newData.add(
                                    GetRealDataFromHexaImbera.getDecimalFloat(
                                        data[i].substring(
                                            22,
                                            26
                                        )
                                    ).toString()
                                ) //decimales con punto //get temp positivo
                            } else if (num > 99.99) {
                                //newData.add(getNegativeTemp("FFFF"+data.get(i).substring(22,26))); //get negativos
                                newData.add(
                                    GetRealDataFromHexaImbera.getNegativeTemp(
                                        "FFFF" + data[i].substring(
                                            22,
                                            26
                                        )
                                    )
                                ) //get negativos
                            } else { //Es 0 cero
                                newData.add("0000") //get negativos
                            }
                            newData.add(
                                GetRealDataFromHexaImbera.getDecimal(data[i].substring(26))
                                    .toString()
                            ) //decimales sin punto,voltaje
                            i++
                        } while (i < data.size)
                    } else {
                        /*newData.add(getSameData(data.get(0),"trefpversion"));
                               newData.add(getSameData(data.get(1),action));
                               newData.add(getSameData(data.get(2),action));
                               newData.add(getSameData(data.get(3),action));
                                */
                        //buffer
                        var date: Date
                        var date2: Date
                        var i = 4
                        val timeStampOriginal = GetRealDataFromHexaImbera.getDecimal(
                            data[data.size - 1].substring(
                                8,
                                16
                            )
                        ).toLong()
                        //long timeStampOriginal2 = getDecimal(data.get(data.size()-2).substring(8,16));
                        val unixTime = System.currentTimeMillis() / 1000
                        val diferencialTimeStamp = unixTime - timeStampOriginal
                        //long diferencialTimeStamp2 =  unixTime - timeStampOriginal2  ;
                        do {
                            //Date
                            val instant = Instant.ofEpochSecond(
                                GetRealDataFromHexaImbera.getDecimal(
                                    data[i].substring(0, 8)
                                ) + diferencialTimeStamp
                            )
                            date = Date.from(instant)
                            newData.add(date.toString())

                            //Log.d("ASDASDASD",":"+data.get(i));
                            val instant2 = Instant.ofEpochSecond(
                                GetRealDataFromHexaImbera.getDecimal(
                                    data[i].substring(8, 16)
                                ) + diferencialTimeStamp
                            )
                            date2 = Date.from(instant2)
                            newData.add(date2.toString())
                            newData.add(
                                getEventType(
                                    data[i].substring(
                                        16,
                                        18
                                    )
                                )
                            ) //evento type
                            var numf =
                                GetRealDataFromHexaImbera.getDecimalFloat(data[i].substring(18, 22))
                            var num = numf.toInt()
                            if (num < 99.99) {
                                newData.add(
                                    GetRealDataFromHexaImbera.getDecimalFloat(
                                        data[i].substring(
                                            18,
                                            22
                                        )
                                    ).toString()
                                ) //decimales con punto //get temp positivo
                            } else if (num > 99.99) {
                                newData.add(
                                    GetRealDataFromHexaImbera.getNegativeTemp(
                                        "FFFF" + data[i].substring(
                                            18,
                                            22
                                        )
                                    )
                                ) //get negativos
                            } else { //Es 0 cero
                                newData.add("0000") //get negativos
                            }
                            numf =
                                GetRealDataFromHexaImbera.getDecimalFloat(data[i].substring(22, 26))
                            //numf = getDecimalFloat("FFCE");
                            num = numf.toInt()
                            if (num < 99.99) {
                                newData.add(
                                    GetRealDataFromHexaImbera.getDecimalFloat(
                                        data[i].substring(
                                            22,
                                            26
                                        )
                                    ).toString()
                                ) //decimales con punto //get temp positivo
                            } else if (num > 99.99) {
                                //newData.add(getNegativeTemp("FFFF"+data.get(i).substring(22,26))); //get negativos
                                newData.add(
                                    GetRealDataFromHexaImbera.getNegativeTemp(
                                        "FFFF" + data[i].substring(
                                            22,
                                            26
                                        )
                                    )
                                ) //get negativos
                            } else { //Es 0 cero
                                newData.add("0000") //get negativos
                            }
                            newData.add(
                                GetRealDataFromHexaImbera.getDecimal(data[i].substring(26))
                                    .toString()
                            ) //decimales sin punto,voltaje
                            i++
                        } while (i < data.size)
                    }
                }
                newData
            }

            else -> {
                java.util.ArrayList()
            }
        }
    }

    fun getDecimal(hex: String): Int {
        var hex = hex
        val digits = "0123456789ABCDEF"
        hex = hex.uppercase(Locale.getDefault())
        var `val` = 0
        for (i in 0 until hex.length) {
            val c = hex[i]
            val d = digits.indexOf(c)
            `val` = 16 * `val` + d
        }
        return `val`
    }

    fun getDecimalFloatLoggerLayout(hex: String): String {

        val parsedResult = hex.toLong(16).toInt()
        return (parsedResult / 10).toString()

    }


    fun getNegativeTempfloatLogger(hexaTemp: String): String {

        val parsedResult: Double = hexaTemp.toLong(16).toInt().toDouble() // as  Int.toFloat()
        val result = parsedResult / 10.0

        return String.format("%.2f", result)
    }

    fun ReturnValLoggerTraduccion(valor: String): String {
        var S: String = ""
        val numf = getDecimalFloatLogger(valor)
        val num = numf.toInt()

        if (num < 99.99) {
            S =
                getDecimalFloatLogger(valor).toString() // Decimales con punto // Obtener temperatura positiva
        } else if (num > 99.99) {
            S = getNegativeTempfloatLogger("FFFF" + valor) // Obtener temperaturas negativas
        } else {
            S = "0000" // Es 0 (cero)
        }

        return S
    }

    private fun getEventType(s: String): String? {
        //String ss = He
        // xToBinary(s);
        //StringBuilder stringBuilder = new StringBuilder();
        var evento = ""
        //Log.d(TAG,"1:"+ss);
        //Log.d(TAG,"2:"+s);
        var c: String
        when (s) {
            "04" -> evento = "Falla de energía"
            "03" -> evento = "Ciclo de deshielo"
            "02" -> evento = "Ciclo de compresor"
            "01" -> evento = "Apertura de puerta"
        }
        //}
        //}
        //return stringBuilder.toString();
        return evento
    }

    private fun getAlarma(s: String): String? {
        val ss = GetRealDataFromHexaImbera.HexToBinary(s)
        val stb = java.lang.StringBuilder()
        var c: String
        for (i in 0..7) {
            c = ss.substring(i, i + 1)
            if (c == "1") {
                when (i) {
                    7 -> stb.append("Falla sensor ambiente en corto\n")
                    6 -> stb.append("Falla sensor ambiente en abierto\n")
                    5 -> stb.append("Falla sensor evaporador en corto\n")
                    4 -> stb.append("Falla sensor evaporador en abierto\n")
                    3 -> stb.append("Falla de puerta\n")
                    2 -> stb.append("Reservada\n")
                    1 -> stb.append("Falla de voltaje bajo\n")
                    0 -> stb.append("\nFalla de voltaje alto\n")
                }
            }
        }
        stb.append(".")
        return stb.toString()
    }

    private fun getActuador104(s: String): String? {
        Log.d("LOaL", ":$s")
        val ss3 = GetRealDataFromHexaImbera.HexToBinary(s.substring(0, 2))
        val ss2 = GetRealDataFromHexaImbera.HexToBinary(s.substring(2))
        val ss = ss3 + ss2
        Log.d("LOL", ":$ss")
        val stringBuilder = StringBuilder()
        var c: String
        for (i in ss.length downTo 0) {
            c = if (i - 1 < 0) {
                ss.substring(0, 1)
            } else {
                ss.substring(i - 1, i)
            }
            Log.d("C", ":$c")
            Log.d("i", ":$i")
            if (c == "1") {
                when (i - 1) {
                    0 -> stringBuilder.append("Estado iluminación: Activo\n")
                    1 -> stringBuilder.append("Estado vendilador: Encendido\n")
                    2 -> stringBuilder.append("Modo nocturno: Encendido\n")
                    3 -> stringBuilder.append("Modo ahorro 2: Activo\n")
                    4 -> stringBuilder.append("Modo ahorro 1: Activo\n")
                    5 -> stringBuilder.append("Estado de puerta: Abierta\n")
                    6 -> stringBuilder.append("Estado de deshielo: Activo\n")
                    7 -> stringBuilder.append("\nEstado de compresor: Activo\n")
                }
            } else {
                when (i - 1) {
                    0 -> stringBuilder.append("Estado iluminación: Inactivo\n")
                    1 -> stringBuilder.append("Estado vendilador: Apagado\n")
                    2 -> stringBuilder.append("Modo nocturno: Apagado\n")
                    3 -> stringBuilder.append("Modo ahorro 2: Inactivo\n")
                    4 -> stringBuilder.append("Modo ahorro 1: Inactivo\n")
                    5 -> stringBuilder.append("Estado de puerta: Cerrada\n")
                    6 -> stringBuilder.append("Estado de deshielo: Inactivo\n")
                    7 -> stringBuilder.append("\nEstado de compresor: Inactivo\n")
                }
            }
        }

        /*for (int i=0; i<=15 ; i++){
            c = ss.substring(i,i+1);
            if (c.equals("1")){
                switch (i)  {
                    case 0:
                        stringBuilder.append("\nEquipo en deshielo: Si\n");
                        break;
                    case 1:
                        stringBuilder.append("Equipo enfriando encendido: Si\n");
                        break;
                    case 2:
                        stringBuilder.append("Equipo enfriando apagadp: Si\n");
                        break;
                    case 3:
                        stringBuilder.append("Estado general: Encendido\n");
                        break;
                    case 15:
                        stringBuilder.append("\nEstado iluminación: Activo\n");
                        break;
                    case 14:
                        stringBuilder.append("Estado vendilador: Encendido\n");
                        break;
                    case 13:
                        stringBuilder.append("Modo nocturno: Encendido\n");
                        break;
                    case 12:
                        stringBuilder.append("\nModo ahorro 2: Activo\n");
                        break;
                    case 11:
                        stringBuilder.append("Modo ahorro 1: Activo\n");
                        break;
                    case 10:
                        stringBuilder.append("Estado de puerta: Abierta\n");
                        break;
                    case 9:
                        stringBuilder.append("Estado de deshielo: Activo\n");
                        break;
                    case 8:
                        stringBuilder.append("Estado de compresor: Activo");
                        break;
                }
            }else{
                switch (i)  {//00101
                    case 0:
                        stringBuilder.append("\nEquipo en deshielo: No\n");
                        break;
                    case 1:
                        stringBuilder.append("Equipo enfriando encendido: No\n");
                        break;
                    case 2:
                        stringBuilder.append("Equipo enfriando apagadp: No\n");
                        break;
                    case 3:
                        stringBuilder.append("Estado general: Apagado\n");
                        break;
                    case 15:
                        stringBuilder.append("\nEstado iluminación: Inactivo\n");
                        break;
                    case 14:
                        stringBuilder.append("Estado vendilador: Apagado\n");
                        break;
                    case 13:
                        stringBuilder.append("Modo nocturno: Apagado\n");
                        break;
                    case 12:
                        stringBuilder.append("\nModo ahorro 2: Inactivo\n");
                        break;
                    case 11:
                        stringBuilder.append("Modo ahorro 1: Inactivo\n");
                        break;
                    case 10:
                        stringBuilder.append("Estado de puerta: Cerrada\n");
                        break;
                    case 9:
                        stringBuilder.append("Estado de deshielo: Inactivo\n");
                        break;
                    case 8:
                        stringBuilder.append("Estado de compresor: Inactivo");
                        break;
                }
            }
        }*/return stringBuilder.toString()
    }

    private fun getActuador(s: String): String? {
        val ss = GetRealDataFromHexaImbera.HexToBinary(s)
        val stringBuilder = java.lang.StringBuilder()
        var c: String
        for (i in 3..7) {
            c = ss.substring(i, i + 1)
            if (c == "1") {
                when (i) {
                    3 -> stringBuilder.append("\nModo ahorro 2: ON\n")
                    4 -> stringBuilder.append("Modo ahorro 1: ON\n")
                    5 -> stringBuilder.append("Estado de puerta: Abierta\n")
                    6 -> stringBuilder.append("Estado de deshielo: ON\n")
                    7 -> stringBuilder.append("Estado de compresor: ON")
                }
            } else {
                when (i) {
                    3 -> stringBuilder.append("\nModo ahorro 2: OFF\n")
                    4 -> stringBuilder.append("Modo ahorro 1: OFF\n")
                    5 -> stringBuilder.append("Estado de puerta: Cerrada\n")
                    6 -> stringBuilder.append("Estado de deshielo: OFF\n")
                    7 -> stringBuilder.append("Estado de compresor: OFF")
                }
            }
        }
        return stringBuilder.toString()
    }

    fun getTipoEvent(tipoEvent: String): String {
        return when (getDecimal(tipoEvent)) {
            1 -> "Apertura de Puerta"
            2 -> "Ciclo de compresor"
            3 -> "Ciclo de Deshielo"
            4 -> "Falla de energía"
            5 -> "Alarma + futuros"
            else -> {
                "no se pudo identificar el evento"
            }
        }
    }

    override fun onBluetoothDeviceConnected(): Boolean {
        return true
        Log.d(
            "conexionTrefp2.cMyAsyncTaskGetHandshake",
            "salidadeconsolainterface  onBluetoothDeviceConnected"
        )
    }

    override fun onBluetoothDeviceDisconnected(): Boolean {
        return false
        Log.d(
            "conexionTrefp2.cMyAsyncTaskGetHandshake",
            "salidadeconsolainterface  onBluetoothDeviceDisconnected "
        )
    }

    fun getFwVersionFromHex(byte1: String?, byte2: String?): String? {
        val stringBuilder = java.lang.StringBuilder()
        stringBuilder.append(GetRealDataFromHexaImbera.getDecimal(byte1!!))
        stringBuilder.append(".")
        //Log.d("VERSION",":"+data.get(3).substring(2));
        val version = GetRealDataFromHexaImbera.getDecimal(byte2!!)
        if (version < 10) {
            stringBuilder.append("0$version")
        } else {
            stringBuilder.append(GetRealDataFromHexaImbera.getDecimal(byte2))
        }
        return stringBuilder.toString()
    }

    fun makeToast(texto: String) {
        // Toast.makeText(context, texto, Toast.LENGTH_SHORT).show()
        // Cancelar cualquier Toast anterior

        // Mostrar el nuevo Toast
        Toast.makeText(getContext, texto, Toast.LENGTH_SHORT).show()

    }

    fun sendCommandWifi(ssid: String, pass: String, ssid2: String, pass2: String) {
        if (bluetoothLeService == null) {
            GlobalTools.showInfoPopup(
                "Información",
                "La respuesta obtenida por el control presentó un detalle, intenta de nuevo o vuelve a conectarte al equipo.",
                getContext!!
            )
        } else {
            val toSendb: ByteArray
            val toSendb2: ByteArray
            var bytesTemp: Array<Byte?>?

            command = "BLE_WIFI_NEW_SSID=" + ssid + ";" + 0x08.toChar() + pass + ";" + 0x08.toChar()
            command2 =
                "BLE_WIFI_NEW_SSID_2=" + ssid2 + ";" + 0x08.toChar() + pass2 + ";" + 0x08.toChar()

            bytesTemp = arrayOfNulls(command.toByteArray().size)
            for (i in command.toByteArray().indices) {
                bytesTemp[i] = command.toByteArray()[i]
            }

            toSendb = ByteArray(bytesTemp.size)
            var j = 0
            for (b in bytesTemp) {
                if (b != null) {
                    toSendb[j++] = b
                }
            }

            bytesTemp = null
            bytesTemp = arrayOfNulls(command2.toByteArray().size)
            for (i in command2.toByteArray().indices) {
                bytesTemp[i] = command2.toByteArray()[i]
            }

            toSendb2 = ByteArray(bytesTemp.size)
            j = 0
            for (b in bytesTemp) {
                if (b != null) {
                    toSendb2[j++] = b
                }
            }
            Log.d(
                "MyAsyncTaskGetFirmwareBd",
                " command $command command2 $command2 toSendb $toSendb toSendb2 $toSendb2 "
            )
            MyAsyncTaskSendCommandWifi(toSendb, toSendb2)
        }
    }

    fun MyAsyncTaskSendCommandWifi(toSendb: ByteArray, toSendb2: ByteArray) {
        var toSendb = toSendb
        var toSendb2 = toSendb2

        Log.d(
            "MyAsyncTaskGetFirmwareBd",
            " second command $command command2 $command2 toSendb $toSendb toSendb2 $toSendb2 "
        )
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            val pre = withContext(Dispatchers.Default) {


                if (bluetoothLeService == null) {
                    return@withContext "notConnected"
                } else {
                    Log.d("wifiCommand", ":$command")
                    try {
                        bluetoothLeService!!.sendComandoW(command, toSendb)
                        Thread.sleep(350)
                        bluetoothLeService!!.sendComandoW(command2, toSendb2)
                    } catch (e: InterruptedException) {
                        throw RuntimeException(e)
                    }
                }
            }
            val result = withContext(Dispatchers.Default) {
                try {
                    Thread.sleep(300)

                    /*listData.clear();
                listData = bluetoothLeService.getDataFromBroadcastUpdate();
                FinalListData.clear();*/if (pre == "notConnected") {
                        runOnUiThread {
                            makeToast("Conéctate a un BLE")
                        }
                    } else {
                        runOnUiThread {
                            makeToast("Escritura correcta")
                        }
                    }

                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }

        }
    }


    ////////////////////////////////////////FIRMWARE//////////////////////////////////////////////

    fun MyAsyncTaskGetFirmwareBd(callback: MyCallbackBD) {
        var exc: String? = null
        callback.onProgress("Iniciando")
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            //     createProgressDialog("Buscando usuario...")
            //    progressDialog2.second("Cargando plantilla")

            data class EdithPrueba(var Nombre: String, var Hint: String)
            //   var edtihTextNombre = mutableListOf<String, String>()
            val pre = withContext(Dispatchers.IO) {
                return@withContext try {
                    callback.onProgress("Realizando")
                    Thread.sleep(200)
                    Class.forName("com.mysql.jdbc.Driver").newInstance()
                    val connection: Connection = DriverManager.getConnection(
                        "jdbc:mysql://electronicaeltec.com:3306/empresas",
                        "moises",
                        "9873216543"
                    )
                    var jerarquia = ""
                    val modelo = sp?.getString(
                        "modeloHEXDecimal",
                        ""
                    ) //"6885"// sp.getString("modeloHEX","")


                    val sql = "SELECT f3.version, f3.Valor   From BD_Imbera.formularios f\n" +
                            "INNER JOIN empresas.formularios f2 ON f.id_formulario = f2.id \n" +
                            "INNER JOIN empresas.firmware f3 ON f2.id_vf  = f3.id\n" +
                            "INNER JOIN empresas.modelos m ON f3.modelo = m.id\n" +
                            "WHERE  f.vigente  = 1\n" +
                            "and m.clave_modelo  = '" + modelo + "'" +
                            "ORDER BY f3.version DESC;"

                    Log.d("MyAsyncTaskGetFirmwareBd", "modelo $modelo") //dmodeloHEXdecimal

                    Log.d(
                        "MyAsyncTaskGetFirmwareBd",
                        "modeloHEXDecimal Ope ${sp?.getString("modeloHEX", "")}"
                    )
                    val statement = connection.createStatement()

                    val result = statement.executeQuery(sql)

                    var count = 0
                    while (result.next()) {
                        //      json = result.getString("Plantilla")
                        Log.d("MyAsyncTaskGetFirmwareBd", "version ${result.getString("version")}")

                        MapaFirmware.add(
                            Pair(
                                result.getString("version"),
                                result.getString("Valor")
                            )
                        )
                        count++
                    }



                    return@withContext if (count > 0) {
                        "exito"
                    } else {
                        "falla"
                    }
                } catch (ex: ClassNotFoundException) {
                    Log.d("ClassNotFoundException", ":$ex")
                    val exc = ex.toString()
                    return@withContext "ClassNotFoundException"
                } catch (ex: IllegalAccessException) {
                    Log.d("IllegalAccessException", ":$ex")
                    val exc = ex.toString()
                    return@withContext "IllegalAccessException"
                } catch (ex: Fragment.InstantiationException) {
                    Log.d("InstantiationException", ":$ex")
                    val exc = ex.toString()
                    return@withContext "InstantiationException"
                } catch (ex: InterruptedException) {
                    Log.d("InterruptedException", ":$ex")
                    val exc = ex.toString()
                    return@withContext "InterruptedException"
                }
            }
            var post = withContext(Dispatchers.Main) {
                progressDialog2.secondStop()
                Log.d("MyAsyncTaskGetFirmwareBd", "pre $pre")
                when (pre) {
                    "exito" -> {


                        callback.onSuccess(true)
                        runOnUiThread {
                            callback.getInfo(MapaFirmware)
                        }
                        callback.onProgress("Finalizando")
                        delay(200)
                        //     Log.d("FirmwareLog","MapaFirmware ${MapaFirmware.toString()}")

                        /*                      MapaFirmware.map {
                                var firmware = it.first
                                var firmwareString = it.second
                                val firm =
                                    controlGenerator.createExampleButton(
                                        it.first,
                                        "Actualizar a Firmware ${firmware}"
                                    )
                                firm.setOnClickListener {
                                    //     clearData()
                                    makeToast(
                                        "Actualizar a Firmware ${firmware}"
                                    )
                                    /*Log.d(
                                        "ProgressFirmware","firmware  $firmwareString"
                                    )*/

                                    MyAsyncTaskUpdateFirmware(
                                        firmwareString, "",
                                        object : ConexionTrefp.MyCallback {
                                            override fun onSuccess(result: Boolean): Boolean {
                                                Log.d(
                                                    "MyAsyncTaskUpdateFirmware",
                                                    result.toString()
                                                )
                                                return result
                                            }

                                            override fun onError(error: String) {
                                                // manejar error

                                                Log.d(
                                                    "MyAsyncTaskUpdateFirmware",
                                                    error.toString()
                                                )
                                            }

                                            override fun getInfo(data: MutableList<String>?) {

                                                Log.d(
                                                    "MyAsyncTaskUpdateFirmware",
                                                    "getInfo" + data.toString()
                                                )

                                            }

                                            override fun onProgress(progress: String): String {
                                                when (progress) {
                                                    "Iniciando" -> {
                                                        requireActivity().runOnUiThread {
                                                            progressDialog2.second("Actualizando equipo espera un momento...")
                                                            //    progressDialog2.second("Actualizando equipo espera un momento...")
                                                        }
                                                    }

                                                    "Realizando" -> {

                                                        if (progressDialog2.returnprogressDialog2() != null && progressDialog2.returnISshowing()) {
                                                            // progressDialog.dismiss()
                                                            progressDialog2.secondStop()
                                                        }
                                                    }

                                                    "Finalizando" -> {
                                                        requireActivity().runOnUiThread {
                                                            progressDialog2.secondStop()
                                                        }
                                                    }
                                                }
                                                Log.d(
                                                    "MyAsyncTaskUpdateFirmware",
                                                    "progress : $progress"
                                                )

                                                return progress
                                            }
                                        }).execute()

                                }

                                linearLayout.addView(firm)
                                val layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                layoutParams.gravity = android.view.Gravity.CENTER
                                layoutParams.setMargins(0, 0, 0, 30)
                                firm.layoutParams = layoutParams



                            }
    */
                        /*                      var vacio: MutableList<String>? = arrayListOf()
                                              vacio?.add("No hay plantillas descargadas");
                                              val adaptervacio =
                                                  ArrayAdapter(
                                                      requireContext(),
                                                      android.R.layout.simple_spinner_item,
                                                      vacio!!
                                                  )
                                              adaptervacio.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                              spinnerPlantillas?.setAdapter(adaptervacio)

                                              val adapter1 = ArrayAdapter(
                                                  requireContext(),
                                                  android.R.layout.simple_spinner_item,
                                                  ListPlantillasNombres
                                              )
                                              adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                              spinnerPlantillas!!.adapter = adapter1
                                              val i = ListPlantillas.size - 1
                                              GlobalTools.showInfoPopup(
                                                  "Éxito",
                                                  "Se encontraron $i plantillas",
                                                  requireContext()
                                              )
                      */
                    }

                    "falla" -> {
                        callback.onSuccess(false)

                        /*      var vacio: MutableList<String>? = arrayListOf()
                              vacio!!.add("No hay plantillas descargadas")
                              val adaptervacio =
                                  ArrayAdapter(
                                      requireContext(),
                                      android.R.layout.simple_spinner_item,
                                      vacio
                                  )
                              adaptervacio.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                              spinnerPlantillas!!.adapter = adaptervacio
      */
                    }

                    "SQLException" -> {
                        callback.onSuccess(false)
                        makeToast("Error al conectar con BD")
                    }

                    "IllegalAccessException" -> {
                        callback.onSuccess(false)
                        makeToast("Error al conectar con BD")
                    }

                    "InterruptedException" -> {
                        callback.onSuccess(false)
                        makeToast("Error al conectar con BD")
                    }

                    else -> {}
                }
            }


            //    wifi()

        }

    }

    private fun VTIMEVERSION2(arrayLists: MutableList<String?>): MutableList<String?> {
        val arrayListInfo: MutableList<String?> = ArrayList()
        val s = cleanSpace(arrayLists)
        if (arrayLists.isNotEmpty() && s.toString().length > 16) {


            // Header
            arrayListInfo.addAll(
                listOf(
                    s.substring(0, 4),
                    s.substring(4, 12),
                    s.substring(12, 14),
                    s.substring(14, 16)
                )
            )
            val st = StringBuilder(s.substring(16, s.length - 8))

            // Nuevo logger, diferente división de información
            if (true) {
                // Dividir toda la información en paquetes de 128 bytes
                val datos = st.chunked(256)
                val datos2 = datos.flatMap { it.chunked(18) }

                // Organizar la información que realmente sirve (quitar 0s)
                arrayListInfo.addAll(datos2.filter { it.length != 4 && it != "000000000000000000" })
            } else {
                // Antiguo logger
                arrayListInfo.addAll(s.substring(16).chunked(18))
            }
        }

        return arrayListInfo
    }

    ///////////////////////////////WIFI////////////////////////////////////////////
    fun createProgressDialogwifi() {
        //   if (progressdialog == null) {
        //primero pedir la información para cargarla en el menu
        try {
            // bluetoothLeService = bluetoothServices.getBluetoothLeService()
            var listData: List<String>? = arrayListOf()
            var listDatafinal: List<String>? = arrayListOf()
            val toSendb: ByteArray
            var bytesTemp: Array<Byte?>?
            val command: String

            //para leer
            bytesTemp = null
            command = "BLE_WIFI_STATE=?"
            bytesTemp = arrayOfNulls(command.toByteArray().size)
            for (i in command.toByteArray().indices) {
                bytesTemp[i] = command.toByteArray()[i]
            }
            toSendb = ByteArray(bytesTemp.size)
            var j = 0
            for (b in bytesTemp) {
                if (b != null) {
                    toSendb[j++] = b
                }
            }
            bluetoothLeService!!.sendComandoW(command, toSendb)
            Log.d("wifiCommandStatus", ":$command")
            Thread.sleep(200)

            listData = bluetoothLeService!!.getDataFromBroadcastUpdate() //as MutableList<String>
            listDatafinal = listData?.let { hexToAsciiWifi(it) as MutableList<String> }!!


            //Crear dialogos de "pantalla de carga" y "popups if"
            val inflater = LayoutInflater.from(getContext)
            val dialogView = inflater.inflate(R.layout.popup_wifi, null, false)
            val adb = AlertDialog.Builder(
                getContext!!, R.style.Theme_AppCompat_Light_Dialog_Alert_eltc
            )
            adb.setView(dialogView)
            if (listDatafinal.isNotEmpty()) {
                Log.d("wifiCommandStat", ":$listDatafinal")
                val tvssid1 = dialogView.findViewById<View>(R.id.etSSID1) as EditText
                tvssid1.setText(listDatafinal[0].substring(6))
                val tvssid2 = dialogView.findViewById<View>(R.id.etSSID2) as EditText
                tvssid2.setText(listDatafinal[4].substring(7))
                val tvinfossid1 = dialogView.findViewById<View>(R.id.tvinfossid1) as TextView
                val s3 = listDatafinal[3]
                val seF = """
            
            SOLKOS_PLATAFORM${s3.substring(9)}
            """.trimIndent()
                val g = java.lang.StringBuilder()
                g.append("Información completa de la conexión Wi-Fi:")
                for (i in listDatafinal.indices) {
                    g.append(listDatafinal[i])
                    if (i == 3) {
                        g.append(seF)
                    }
                }
                tvinfossid1.text = g
                progressdialog = adb.create()
                progressdialog!!.setCanceledOnTouchOutside(false)
                progressdialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                progressdialog!!.show()
                dialogView.findViewById<View>(R.id.btnSendWifi)
                    .setOnClickListener { //para escribir
                        progressdialog!!.dismiss()
                        progressdialog = null
                        val etSsid1 = dialogView.findViewById<EditText>(R.id.etSSID1)
                        val etSsid2 = dialogView.findViewById<EditText>(R.id.etSSID2)
                        val etpass1 = dialogView.findViewById<EditText>(R.id.etWifiPass1)
                        val etpass2 = dialogView.findViewById<EditText>(R.id.etWifiPass2)
                        val ssid1 = etSsid1.text.toString()
                        val ssid2 = etSsid2.text.toString()
                        val pass1 = etpass1.text.toString()
                        val pass2 = etpass2.text.toString()


                        sendCommandWifi(ssid1, pass1, ssid2, pass2)
                    }
                dialogView.findViewById<View>(R.id.btndontSendwifi).setOnClickListener {
                    progressdialog!!.dismiss()
                    //   progressdialog = null
                }
            }

        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
        // }
    }

    /*
    fun getInfoWifi(callback: ConexionTrefp.MyCallback){
        val job = CoroutineScope(Dispatchers.IO).launch {

            var  FinalPlantillaValorestoExa = mutableListOf<Pair<String, String>>()
            val corrutina1 = launch {
                println("")
                callback.onProgress("Iniciando")
                /*    try {
                        // bluetoothLeService = bluetoothServices.getBluetoothLeService()


                        //para leer
                        var listData: List<String?>? = ArrayList()
                        var listDatafinal: List<String?> = ArrayList()
                        val toSendb: ByteArray
                        var bytesTemp: Array<Byte?>?
                        val command: String
                        //para leer

                        //para leer
                        bytesTemp = null
                        command = "BLE_WIFI_STATE=?"
                        bytesTemp = arrayOfNulls(command.toByteArray().size)
                        for (i in command.toByteArray().indices) {
                            bytesTemp[i] = command.toByteArray()[i]
                        }

                        toSendb = ByteArray(bytesTemp.size)
                        var j = 0
                        for (b in bytesTemp) {
                            if (b != null) {
                                toSendb[j++] = b
                            }
                        }
                        callback.onProgress("Realizando")

                        if (bluetoothLeService == null)
                        {
                            callback.onError("Error de conexión")
                            callback.onSuccess(false)
                        }
                        else{
                            bluetoothLeService!!.sendComandoW(command, toSendb)
                            Log.d("wifiCommandStatus", ":$command")
                            Thread.sleep(800)

                            listData = bluetoothLeService!!.getDataFromBroadcastUpdate() //as MutableList<String>

                            if (listData?.isNotEmpty() == true){
                                listDatafinal = listData?.let { hexToAsciiWifi(it) as MutableList<String> }!!
                                Log.d("wifiCommandStat", "listData :$listData  \n ${hexToAsciiWifi(listData!!)}" +
                                        "\n  $listDatafinal")
                                callback.getInfo(listDatafinal as MutableList<String>)
                                callback.onSuccess(true)
                            }
                            else{
                                callback.onError("No se obtuvo respuesta del control")
                                callback.onSuccess(false)
                            }
                        }
                    } catch (e: InterruptedException)
                    {
                        throw RuntimeException(e)
                        callback.onError(e.toString())
                        callback.onSuccess(false)
                    }
                    */
                var listData: List<String?>? = ArrayList()
                var listDatafinal: List<String?> = ArrayList()
                val toSendb: ByteArray
                var bytesTemp: Array<Byte?>?
                val command: String

                //para leer

                //para leer
                bytesTemp = null
                command = "BLE_WIFI_STATE=?"
                bytesTemp = arrayOfNulls(command.toByteArray().size)
                for (i in command.toByteArray().indices) {
                    bytesTemp[i] = command.toByteArray()[i]
                }

                toSendb = ByteArray(bytesTemp.size)
                var j = 0
                for (b in bytesTemp) {
                    if (b != null) {
                        toSendb[j++] = b
                    }
                }


                Log.d("wifiCommandStatus", ":$command toSendb $toSendb ${ getStatusConnectBle()}")
                //    callback.onProgress("Finalizando")

                callback.onProgress("Realizando")
                if (  getStatusConnectBle())
                {
                    bluetoothLeService!!.sendComandoW(command, toSendb)

                    Thread.sleep(500)
                    listData = bluetoothLeService!!.dataFromBroadcastUpdate





                    Log.d("wifiCommandStat", ":$listDatafinal    listData $listData")
                    if (listData.isNullOrEmpty())
                    {
                        callback.onError("Lista vacia")
                    }
                    else {
                        listDatafinal = hexToAsciiWifi(listData)
                        val g = java.lang.StringBuilder()
                        g.append("Información completa de la conexión Wi-Fi:")
                        for (i in listDatafinal.indices) {
                            //el espacio numero 3 es sustituido por otro texto
                            //if (i==3){
                            //   g.append(seF);
                            // }else{
                            //if (i!=6){//el espacio 6 no aparece en la interfaz
                            g.append(listDatafinal.get(i))
                            //}
                            //}
                        }
                        callback.getInfo(listDatafinal as MutableList<String>)
                        //        Log.d("getInfoWifi","getInfoWifi $g")
                    }
                }
                else{
                    callback.onError("Desconectado")
                }


            }
            corrutina1.join()
            val corrutina2 = launch{
                callback.onProgress("Finalizando")
            }
            corrutina2.join()

            println("Finalizado")

        }


        runBlocking {
            job.join()
        }
    }
*/


    fun getInfoWifi(callback: ConexionTrefp.MyCallback){
        val job = CoroutineScope(Dispatchers.IO).launch {

            var  FinalPlantillaValorestoExa = mutableListOf<Pair<String, String>>()
            val corrutina1 = launch {
                println("")
                callback.onProgress("Iniciando")
                /*    try {
                        // bluetoothLeService = bluetoothServices.getBluetoothLeService()


                        //para leer
                        var listData: List<String?>? = ArrayList()
                        var listDatafinal: List<String?> = ArrayList()
                        val toSendb: ByteArray
                        var bytesTemp: Array<Byte?>?
                        val command: String
                        //para leer

                        //para leer
                        bytesTemp = null
                        command = "BLE_WIFI_STATE=?"
                        bytesTemp = arrayOfNulls(command.toByteArray().size)
                        for (i in command.toByteArray().indices) {
                            bytesTemp[i] = command.toByteArray()[i]
                        }

                        toSendb = ByteArray(bytesTemp.size)
                        var j = 0
                        for (b in bytesTemp) {
                            if (b != null) {
                                toSendb[j++] = b
                            }
                        }
                        callback.onProgress("Realizando")

                        if (bluetoothLeService == null)
                        {
                            callback.onError("Error de conexión")
                            callback.onSuccess(false)
                        }
                        else{
                            bluetoothLeService!!.sendComandoW(command, toSendb)
                            Log.d("wifiCommandStatus", ":$command")
                            Thread.sleep(800)

                            listData = bluetoothLeService!!.getDataFromBroadcastUpdate() //as MutableList<String>

                            if (listData?.isNotEmpty() == true){
                                listDatafinal = listData?.let { hexToAsciiWifi(it) as MutableList<String> }!!
                                Log.d("wifiCommandStat", "listData :$listData  \n ${hexToAsciiWifi(listData!!)}" +
                                        "\n  $listDatafinal")
                                callback.getInfo(listDatafinal as MutableList<String>)
                                callback.onSuccess(true)
                            }
                            else{
                                callback.onError("No se obtuvo respuesta del control")
                                callback.onSuccess(false)
                            }
                        }
                    } catch (e: InterruptedException)
                    {
                        throw RuntimeException(e)
                        callback.onError(e.toString())
                        callback.onSuccess(false)
                    }
                    */
                var listData: List<String?>? = java.util.ArrayList()
                var listDatafinal: List<String?> = java.util.ArrayList()
                val toSendb: ByteArray
                var bytesTemp: Array<Byte?>?
                val command: String

                //para leer

                //para leer
                bytesTemp = null
                command = "BLE_WIFI_STATE=?"
                bytesTemp = arrayOfNulls(command.toByteArray().size)
                for (i in command.toByteArray().indices) {
                    bytesTemp[i] = command.toByteArray()[i]
                }

                toSendb = ByteArray(bytesTemp.size)
                var j = 0
                for (b in bytesTemp) {
                    if (b != null) {
                        toSendb[j++] = b
                    }
                }


                //   Log.d("wifiCommandStatus", ":$command toSendb $toSendb ${ getStatusConnectBle()}")
                //    callback.onProgress("Finalizando")

                callback.onProgress("Realizando")
                if ( getStatusConnectBle())
                {
                    bluetoothServices.bluetoothLeService!!.sendComandoW(command, toSendb)

                    Thread.sleep(500)
                    listData =  bluetoothServices.bluetoothLeService!!.dataFromBroadcastUpdate





                    Log.d("wifiCommandStat", ":$listDatafinal    listData $listData")
                    if (listData.isNullOrEmpty())
                    {
                        callback.onError("Lista vacia")
                    }
                    else {
                        listDatafinal = hexToAsciiWifi(listData)
                        val g = java.lang.StringBuilder()
                        g.append("Información completa de la conexión Wi-Fi:")
                        for (i in listDatafinal.indices) {
                            //el espacio numero 3 es sustituido por otro texto
                            //if (i==3){
                            //   g.append(seF);
                            // }else{
                            //if (i!=6){//el espacio 6 no aparece en la interfaz
                            g.append(listDatafinal.get(i))
                            //}
                            //}
                        }
                        callback.getInfo(listDatafinal as MutableList<String>)
                        //        Log.d("getInfoWifi","getInfoWifi $g")
                    }
                }
                else{
                    callback.onError("Desconectado")
                }


            }
            corrutina1.join()
            val corrutina2 = launch{
                callback.onProgress("Finalizando")
            }
            corrutina2.join()

            println("Finalizado")

        }


        runBlocking {
            job.join()
        }
    }


    fun hexToAsciiWifi(hex: List<String?>): List<String> {
        val arrayListInfo = mutableListOf<String>()
        val hexStrr = StringBuilder()
        for (i in hex.indices) {
            hexStrr.append(hex[i])
        }
        val hexStr = hexStrr.toString().replace(" ", "")
        val output = StringBuilder()
        Log.d("YYY", ":$hexStr")
        var j = 0 // identificar el dato, SSID 0, IP=1, MAC_STA 2, IOT_CORE 3, SSID2 4, VER 5, MQTT
        // estado completo
        var i = 0
        while (i < hexStr.length) {
            val strSeparacion = hexStr.substring(i, i + 4)
            Log.d("opo", ":" + output.length)
            Log.d("opo", ":$output")
            if (strSeparacion == "3B08") { // punto y coma ;
                arrayListInfo.add("\n${output.toString()}")
                j++
                output.delete(0, output.length)
                i += 2
            } else {
                val str = hexStr.substring(i, i + 2)
                output.append(str.toInt(16).toChar())
            }
            i += 2
        }
        return arrayListInfo
    }

    fun actualizarHoraBCD(): Boolean {
        try {
            Thread.sleep(500)

            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val horas = String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY))
            val minutos = String.format("%02d", calendar.get(Calendar.MINUTE))
            val segundos = String.format("%02d", calendar.get(Calendar.SECOND))

            Log.d("actualizarHoraBCD", " horas $horas minutos $minutos  segundos $segundos")
            var mestime = calendar.get(Calendar.MONTH) + 1
            val anotime: String = calendar.toString().substring(calendar.toString().length - 4)
            val diatime = calendar.get(Calendar.DAY_OF_MONTH)
            var mes = ""
            var ano1 = ""
            var ano2 = ""
            var dia = ""
            mes = String.format("%02d", mestime)
            dia = String.format("%02d", diatime)
            val diaNum = dia
            val año = calendar.get(Calendar.YEAR)
            val añoFormateado = String.format("%04d", año)

            ano1 = añoFormateado.substring(0, 2)
            ano2 = añoFormateado.substring(2)
            Log.d("actualizarHoraBCD", "currentTimeHexNOW_BCD $horas horas")
            Log.d("actualizarHoraBCD", "currentTimeHexNOW_BCD $minutos minutos")
            Log.d("actualizarHoraBCD", "currentTimeHexNOW_BCD $segundos segundos")
            Log.d("actualizarHoraBCD", "currentTimeHexNOW_BCD diasemana:$dia dia")
            Log.d("actualizarHoraBCD", "currentTimeHexNOW_BCD diaNum:$diaNum diaNum")
            Log.d("actualizarHoraBCD", "currentTimeHexNOW_BCD mes:$mes mes")
            Log.d("actualizarHoraBCD", "currentTimeHexNOW_BCD ano:$ano1 ano1")
            Log.d("actualizarHoraBCD", "currentTimeHexNOW_BCD ano:$ano2 ano2")
            Log.d("actualizarHoraBCD", "currentTimeHexNOW_BCD time:$calendar time")


            var listData: List<String> = ArrayList()

            Log.d("actualizarHoraBCD", "bluetoothLeService $bluetoothLeService")
            if (bluetoothLeService == null) {

                return false
            } else {
                val stringBuilder = java.lang.StringBuilder()
                stringBuilder.append("405C")
                stringBuilder.append(ano2.toUpperCase())
                stringBuilder.append(mes.toUpperCase())
                stringBuilder.append(diaNum.toUpperCase())
                stringBuilder.append(horas.toUpperCase())
                stringBuilder.append(minutos.toUpperCase())
                stringBuilder.append(segundos.toUpperCase())
                stringBuilder.append(dia.toUpperCase())
                stringBuilder.append("00")
                stringBuilder.append(ano1.toUpperCase())
                stringBuilder.append(
                    calculateChacksumString(stringBuilder.toString())
                        .toUpperCase()
                )
                clearLogger()
                Log.d("actualizarHoraBCD", " stringBuilder.toString() ${stringBuilder.toString()} ")
                bluetoothLeService!!.sendFirstComando(stringBuilder.toString())

                /*  stringBuilder.append(ano2.uppercase(Locale.getDefault()))
                  stringBuilder.append(mes.uppercase(Locale.getDefault()))
                  stringBuilder.append(diaNum.uppercase(Locale.getDefault()))
                  stringBuilder.append(horas.uppercase(Locale.getDefault()))
                  stringBuilder.append(minutos.uppercase(Locale.getDefault()))
                  stringBuilder.append(segundos.uppercase(Locale.getDefault()))
                  stringBuilder.append(dia.uppercase(Locale.getDefault()))
                  stringBuilder.append("00")
                  stringBuilder.append(ano1.uppercase(Locale.getDefault()))
                  */

                var cadena = stringBuilder.toString()
                val codigo = cadena.substring(0, 4)
                val anio = cadena.substring(4, 6)
                val mes = cadena.substring(6, 8)
                val dia = cadena.substring(8, 10)
                val hora = cadena.substring(10, 12)
                val min = cadena.substring(12, 14)
                val segundo = cadena.substring(14, 16)
                val diaSemana = cadena.substring(16, 18)
                val formatoHora = cadena.substring(18, 20)
                val anio2 = cadena.substring(20, 22)
                val checksum = cadena.substring(22)
                // Log.d("COMANDO enviado", ":$cadena")
                Log.d("COMANDO enviado", "Código: $codigo")
                Log.d("COMANDO enviado", "Año: $anio")
                Log.d("COMANDO enviado", "Mes: $mes")
                Log.d("COMANDO enviado", "Día: $dia")
                Log.d("COMANDO enviado", "Hora: $hora")
                Log.d("COMANDO enviado", "Minuto: $min")
                Log.d("COMANDO enviado", "Segundo: $segundo")
                Log.d("COMANDO enviado", "Día de la semana: $diaSemana")
                Log.d("COMANDO enviado", "Formato de hora: $formatoHora")
                Log.d("COMANDO enviado", "Año 2: $anio2")
                Log.d("COMANDO enviado", "Checksum: $checksum")

                Thread.sleep(800)

                listData = getInfoList() as List<String>
                Log.d("COMANDO enviado", "listData $listData")
                Thread.sleep(500)
                if (!listData.isEmpty()) {
                    return if (listData[0].toString().uppercase().contains("F13D")) {
                        Log.d("COMANDO EXITOSO BCD", ":" + listData[0])
                        true
                    } else {
                        Log.d("COMANDO fallido BCD", ":" + listData[0])
                        false
                    }
                }
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
            return false
        }
        return false
    }

    fun eventCompresorConco(event: String, consumo: String): String {

        val eventBinary = convertirHexadecimalABinario(event)
        val consumoBinary = convertirHexadecimalABinario(consumo)
        return convertirBinarioAHexadecimal(eventBinary.substring(0, 3) + consumoBinary).padStart(
            4,
            '0'
        )

    }

    fun convertirBinarioAHexadecimal(binario: String): String {
        val numeroDecimal = binario.toInt(2)
        return String.format("%X", numeroDecimal)
    }

    fun convertirHexadecimalABinario(hexadecimal: String): String {

        val binarioSinCeros = Integer.toBinaryString(hexadecimal.toInt(16))
        val longitudDeseada = hexadecimal.length * 4
        return binarioSinCeros.padStart(longitudDeseada, '0')
    }

    fun agregar00AlPenultimoCaracter(
        lista: MutableList<String>,
        posicion: Int, replace: String
    ): MutableList<String>? {
        lista?.let { // Verificar si la lista no es nula
            for (i in 0 until lista.size) {
                val registro = lista[i]
                if (registro.length >= posicion) {
                    val nuevoRegistro = registro.substring(
                        0,
                        registro.length - posicion
                    ) + replace + registro.substring(registro.length - posicion)
                    lista[i] = nuevoRegistro
                }
            }
            return lista // Devolver la lista modificada
        }
        return null // Devolver null si la lista es nula
    }

    private fun InsertLoggerBD(timeStampControl: String, timeStampNow: String, datos: String) {
        val URL: String = "jdbc:mysql://electronicaeltec.com:3306/BD_Imbera"
        val USER: String = "moises"
        val PASS: String = "9873216543"

        // Datos para la inserción
        val timeStampControl = timeStampControl  //Timestamp(System.currentTimeMillis())
        val timeStampNow = timeStampNow //Timestamp(System.currentTimeMillis())
        val datosCrudos = datos //"Datos de prueba"

        // Intentar establecer la conexión y realizar la inserción
        try {
            // Cargar el controlador de MySQL
            Class.forName("com.mysql.jdbc.Driver")

            // Establecer la conexión
            val connection: Connection = DriverManager.getConnection(URL, USER, PASS)

            // Consulta de inserción
            val query =
                "INSERT INTO nombre_de_la_tabla (timeStampControl, timeStampNow, datosCrudos) VALUES (?, ?, ?)"

            // Preparar la declaración SQL
            val preparedStatement: PreparedStatement = connection.prepareStatement(query)

            // Establecer los valores de los parámetros
            preparedStatement.setString(1, timeStampControl)
            preparedStatement.setString(2, timeStampNow)
            preparedStatement.setString(3, datosCrudos)

            // Ejecutar la inserción
            val rowsAffected = preparedStatement.executeUpdate()

            if (rowsAffected > 0) {
                println("Inserción exitosa.")
            } else {
                println("Error al insertar datos.")
            }

            // Cerrar recursos
            preparedStatement.close()
            connection.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    fun MyAsyncTaskSendWifiPrincipal(ssid : String, pass : String,callback: ConexionTrefp.MyCallback){
        SendWifi(ssid,pass, "BLE_WIFI_NEW_SSID=", callback)
    }
    fun MyAsyncTaskSendWifiSecundario(ssid : String, pass : String,callback: ConexionTrefp.MyCallback){
        SendWifi(ssid,pass, "BLE_WIFI_NEW_SSID_2=", callback)
    }
    fun SendWifi(ssid : String, pass : String, comando : String, callback: ConexionTrefp.MyCallback){
        val job = CoroutineScope(Dispatchers.IO).launch {
            bluetoothLeService = bluetoothServices.bluetoothLeService
//            var bluetoothLeService = conexionTrefp2.bluetoothLeService
            Log.d("BtnSendWifi","bluetoothLeService  $bluetoothLeService")
            var listData: List<String>? = arrayListOf()
            val toSendb: ByteArray
            var bytesTemp: Array<Byte?>? = null
            var   command = ""
            command = comando + ssid + ";" + 0x08.toChar() + pass + ";" + 0x08.toChar()
            bytesTemp = arrayOfNulls(command.toByteArray().size)
            for (i in command.toByteArray().indices) {
                bytesTemp!![i] = command.toByteArray()[i]
            }
            toSendb = ByteArray(bytesTemp!!.size)
            var j = 0
            for (b in bytesTemp!!) {
                if (b != null) {
                    toSendb[j++] = b
                }
            }
            var  FinalPlantillaValorestoExa = mutableListOf<Pair<String, String>>()
            val corrutina1 = launch {

                callback.onProgress("Iniciando")
                try {
                    callback.onProgress("Realizando")
                    if (bluetoothLeService == null)
                    {
                        callback.onError("Error de conexión")
                        callback.onSuccess(false)
                    }
                    else{
                        runOnUiThread {
//                            conexionTrefp2.bluetoothServices.sendCommandWifiPrincipal(ssid, pass)
                            bluetoothLeService!!.sendComandoW(command, toSendb)
                        }
                        callback.onSuccess(true)
                        /*  Thread.sleep(500)
                          listData = bluetoothLeService!!.getDataFromBroadcastUpdate() //as MutableList<String>


                          if (listData?.isNotEmpty() == true){
                              // CON RESPUESTA
                              callback.onSuccess(true)
                          }
                          else{
                              // SIN RESPUESTA
                              callback.onError("No se obtuvo respuesta del control")
                              callback.onSuccess(false)
                          }
                          */
                    }
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                    callback.onError(e.toString())
                    callback.onSuccess(false)
                }
            }
            corrutina1.join()
            val corrutina2 = launch{
                delay(1000)
                callback.onProgress("Finalizando")
            }
            corrutina2.join()

        }


        runBlocking {
            job.join()
        }
    }

    fun getVersionName(): String {
        try {

            val pInfo: PackageInfo = this.packageManager.getPackageInfo(this.packageName, 0)
            return pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return ""
    }


    fun Updatefirmware(
        FWWW: String,
        callback:  MyCallback
    ) {
        var error = false
        var textError = ""
        var divFirmware =   dividirNewFirmware(FWWW)

        val job = CoroutineScope(Dispatchers.IO).launch {

            val corrutina1 = launch {
                callback.onProgress("Iniciando")
                Log.d("firmwarePASOS", "Iniciando")

                listData!!.clear()


                Log.d(
                    "firmwarePASOS",
                    "firmware corrutina1 handshake listData $listData  infolist ${ getInfoList()} "
                )

                if ( GetStatusBle()) {
                    if (divFirmware) {
                        bluetoothServices.bluetoothLeService!!.CleanListLogger()
                        delay(2000)
                        bluetoothServices.bluetoothLeService!!.sendComando("4046")
                        delay(800)
                        listData = getInfoList() as MutableList<String?>
                        Log.d(
                            "firmwarePASOS",
                            "firmware corrutina1 handshake listData $listData  infolist ${getInfoList()} "
                        )
                    }
                    else    {
                        error = true
                        textError = "Firmware incorrecto"
                    }

                } else {
                    error = true
                    textError = "Dispositivo desconectado"

                }





            }
            corrutina1.join()
            val corrutina2 = launch {
                callback.onProgress("Realizando")
                Log.d("firmwarePASOS", "Realizando GetStatusBle  ${GetStatusBle()} divFirmware $divFirmware")
                if (GetStatusBle()  ) {
                    if (divFirmware)
                    {
                        if (containsPair(listData!!, "F1", "03")) {
                            Log.d("firmwarePASOS", "2251")

                            bluetoothServices.bluetoothLeService!!.CleanListLogger()

                            delay(200)
                            Log.d("firmwarePASOS", "Despues de limpiar")
                            bluetoothServices.bluetoothLeService!!.sendFirstComando(
                                "4049" + Integer.toHexString(
                                    FinalFirmwareCommands.size
                                )
                            )
                            Log.d(
                                "firmwarePASOS",
                                "se mando el comando 4049  ${Integer.toHexString(FinalFirmwareCommands.size)}"
                            )
                            delay(1000)
                            Log.d(
                                "firmwarePASOS", "se espera 800"
                            )
                            listData =  getInfoList() as MutableList<String?>
                            Log.d(
                                "firmwarePASOS",
                                "listData ${ getInfoList()} listData $listData"
                            )
                            val result = executeFirmwareUpdate()
                            when (result) {
                                "correct" -> {
                                    callback.onSuccess(true)
                                }
                                "cancel", "cancelFialChecksumError" -> {
                                    callback.onSuccess(false)
                                    callback.onError(result)
                                }
                            }
                        }
                        else {
                            Log.d(
                                "firmwarePASOS",
                                "No se pudo preparar el dispositivo para la actualizacion"
                            )
                            callback.onError("No se pudo preparar el modo actualizacion de flash")
                        }
                    }
                }
            }
            corrutina2.join()
            val corrutina3 = launch {

                callback.onProgress("Finalizando")
                Log.d("firmwarePASOS", "Finalizando error $error")
                if (error) {
                    callback.onError(textError)
                    callback.onSuccess(false)
                }
            }
            corrutina3.join()


            /*  val corrutina1 = launch {

                  listData?.clear()

                  listData = conexionTrefp.getInfoList()

                  bluetoothLeService!!.sendComando("4046")
                  delay(500)
                  listData = conexionTrefp.getInfoList()
                  Log.d(
                      "firmwarePASOS",
                      "firmware corrutina1 handshake listData $listData  infolist ${conexionTrefp.getInfoList()} "
                  )

                  if (!listData!!.isNotEmpty()) {
                      Log.d(
                          "firmwarePASOS",
                          "listData!!.isNotEmpty()")
                  } else if (listData!!.first().trim().uppercase().replace(" ", "") == "F103") {
                      Log.d(
                          "firmwarePASOS", " valor  F103")
                      dividirFirmware102(FWWW)
                      delay(200)
                      bluetoothLeService?.CleanListLogger()
                      bluetoothLeService!!.sendComando(
                          "4049" + Integer.toHexString(FinalFirmwareCommands.size),
                          ""
                      )
                      delay(800)
                      listData = conexionTrefp.getInfoList()
                      delay(800)
                      Log.d(
                          "firmwarePASOS",
                          "4049 listData $listData ")

                      val updatedData = executeFirmwareUpdate()
                      if (updatedData != "correct") callback.onError(updatedData) else callback.onSuccess(
                          true
                      )
                  }
                  callback.onProgress("Finalizando")
                  Log.d(
                      "firmwarePASOS",
                      "Finalizando ")

              }
              */


        }
    }

    fun dividirNewFirmware(command: String): Boolean {
        // Verificar que la longitud del comando sea suficiente para evitar IndexOutOfBoundsException
        if (command.length < 262) {
            Log.e("dividirFirmware102", "El comando es demasiado corto: ${command.length}")
            return false
        } else {
            // Obtener la versión del firmware
            val majorVersion = GetRealDataFromHexaImbera.getDecimal(command.substring(256, 258))
            val minorVersion = GetRealDataFromHexaImbera.getDecimal(command.substring(258, 260))
            newFirmwareVersion = String.format("%d.%02d", majorVersion, minorVersion)
            newFimwareModelo =
                GetRealDataFromHexaImbera.getDecimalFloat(command.substring(260, 262)).toString()

            // Inicializar variables para dividir el firmware
            firmCommandCut?.clear()
            FinalFirmwareCommands.clear()
            checksumTotal = 0

            // Dividir el firmware en partes de 256 caracteres (128 bytes)
            var i = 0
            var j = 256
            while (j < command.length) {
                firmCommandCut?.add(command.substring(i, j))
                i = j
                j += 256
            }
            if (i < command.length) {
                firmCommandCut?.add(command.substring(i))
            }

            // Calcular y agregar checksums
            firmCommandCut?.forEach { part ->
                val checksum = part.chunked(2).sumOf { GetRealDataFromHexaImbera.getDecimal(it) }
                checksumTotal += checksum
                val checksumHex = checksum.toString(16).padStart(8, '0')
                FinalFirmwareCommands.add(part + checksumHex)
            }
            return true
        }


    }
    fun containsPair(input: Any, primer: String, segundo: String): Boolean {
        // Patrón para coincidir con "primer segundo" con un espacio opcional en medio
        val pattern = Regex("$primer\\s?$segundo")

        return when (input) {
            is String -> pattern.containsMatchIn(input)
            is List<*> -> input.any { it is String && pattern.containsMatchIn(it) }
            else -> false
        }
    }

    fun removeSpecificElements(listData: List<String?>?): List<String?>? {
        // Verifica si la lista no es nula
        return listData?.filter {
            // Verifica si el elemento no es nulo y luego lo limpia de espacios y caracteres de corchetes
            it?.replace(" ", "")?.replace("[", "")?.replace("]", "")?.trim() != "40F8"
        }
    }
    suspend fun executeFirmwareUpdate(): String {
        var i = 0
        while (i < FinalFirmwareCommands.size) {
            bluetoothServices.bluetoothLeService!!.CleanListLogger()
            bluetoothServices.bluetoothLeService!!.sendComando(
                FinalFirmwareCommands[i],
                ""
            )
            delay(200)

            //  val listData = conexionTrefp.getInfoList()!!.trim().uppercase().replace(" ","") //     bluetoothLeService!!.listData.first().trim().uppercase().replace(" ","")//  conexionTrefp.getInfoList() //bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String>

            listData = getInfoList() as MutableList<String?>

            Log.d(
                "executeFirmwareUpdate",
                "newListData $i ${listData.toString().trim().uppercase()}"
            )
            if (!containsPair(listData!!, "F1", "3D")) {
                return "cancel"
            }

            if (i + 1 == FinalFirmwareCommands.size) {
                bluetoothServices.bluetoothLeService!!.CleanListLogger()
                val finalChecksum = Integer.toHexString(checksumTotal).padStart(8, '0')
                bluetoothServices.bluetoothLeService!!.sendComando("404A$finalChecksum")
                delay(150)

                val finalResponse =
                    bluetoothServices.bluetoothLeService!!.listData // conexionTrefp.getInfoList()
                //  bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String>
                val finalListResponse = GetRealDataFromHexaImbera.convert(
                    finalResponse as List<String>,
                    "Actualizar a Firmware Personalizado",
                    sp!!.getString("numversion", "")!!,
                    sp!!.getString("modelo", "")!!
                ).toMutableList()

                return if (finalListResponse.isEmpty() || finalListResponse[0].trim()
                        .replace(" ", "")
                        .uppercase() != "F13D"
                ) {
                    "cancelFialChecksumError"
                } else {
                    "correct"
                }
            }
            i++
        }
        return "correct"
    }
}



