package com.example123.trepfp

import BluetoothServices.BluetoothServices
import BluetoothServices.ListenerInfoBle
import Utility.*
import Utility.GetHexFromRealDataImbera.calculateChacksumString
import Utility.GetRealDataFromHexaOxxoDisplay.cleanSpace
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
import androidx.lifecycle.lifecycleScope

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


        fun LoggerLuis2(callback2: CallbackLoggerVersionCrudo) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.Default)  {
                var listaInicialDatosTiempoOriginal: MutableList<String> = ArrayList()
                var listaInicialDatosEventoOriginal: MutableList<String> = ArrayList()
                var listaInicialDatosTiempoFiltrada: MutableList<String> = ArrayList()
                var listaInicialDatosEventoFiltrada: MutableList<String> = ArrayList()
                var listaFinalDatosTiempoAjustado: MutableList<String> = ArrayList()
                var listaFinalDatosEventoAjustado: MutableList<String> = ArrayList()
                var listaInicialDatosTiempoFiltradaErrores: MutableList<String> = ArrayList()
                var listaInicialDatosEventosFiltradaErrores: MutableList<String> = ArrayList()
                bluetoothLeService!!.clearListLogger()
                listaInicialDatosTiempoOriginal.clear()

                bluetoothLeService!!.sendFirstComando("4060") // Mandar comando a control de extracción de datos de tiempo
                delay(13000) // Esperar la extracción del logger
                bluetoothLeService!!.getLogeer()!!.toList()

            }
        }
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
         inner class ConnectSimple(
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

            val maxAttempts = 3
            do {
                val isConnected = nuewIsConnected()
                if (isConnected) {
                 /*   var llave = pedirLlaveComunicacion()
                    if (!llave.equals("empty")) {
                        var result = mandarLlaveComunicacion(llave!!.trim())

                        BanderaLLave = true
                        Log.d("MyAsyncTaskConnectBLE", "resultado $result  BanderaLLave = true")
                    } else {
                        BanderaLLave = false
                        Log.d("MyAsyncTaskConnectBLE", "BanderaLLave = false")
                    }
                    callback.onSuccess(true)

                    cancelExecution()*/
                    callback.onSuccess(true)
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
            delay(5000)
            for (attempt in 1..maxAttempts) {
                delay(3000)
                bluetoothLeService = bluetoothServices.bluetoothLeService
                isConnected =
                  true//  isBLEGattConnected() // sp?.getString("ACTION_GATT_CONNECTED", "") == "CONNECTED"

                Log.d(
                    "pruebaConexionLOg",
                    "nuewIsConnected isConnected $isConnected attempt $attempt"
                )


            }
            delay(1000)
            return isConnected
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

                for (i in 1..45) {
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
                        Time = VTIME(ListapruebaTime as MutableList<String?>)
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
                    callback.getTimeCrudo(
                        agregar00AlPenultimoCaracter(
                            registrosTime as MutableList<String>,
                            2
                        )
                    )
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
                                    }
                                    else {
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


                                } else {

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

    suspend fun ObtenerLoggerTimeComand(): MutableList<String> {
        bluetoothServices.bluetoothLeService!!.clearListLogger()
        delay(800) // Suspende el hilo sin bloquearlo
        bluetoothServices.bluetoothLeService?.listData?.clear()
        bluetoothLeService?.sendComando("4060")
        var ListapruebaTime = mutableListOf<String>()
        // Recopila datos en segundo plano sin bloquear el hilo principal
        delay(30000)
        ListapruebaTime =
            bluetoothServices!!.bluetoothLeService!!.getLogeer() as MutableList<String>
        return ListapruebaTime
    }

    suspend fun ObtenerLoggerEventComand(): MutableList<String> {
        bluetoothServices.bluetoothLeService!!.clearListLogger()
        delay(800) // Suspende el hilo sin bloquearlo
        bluetoothServices.bluetoothLeService?.listData?.clear()
        bluetoothLeService?.sendComando("evento", "4061")
        var ListapruebaTime = mutableListOf<String>()
        // Recopila datos en segundo plano sin bloquear el hilo principal
        delay(50000)
        ListapruebaTime =
            bluetoothServices!!.bluetoothLeService!!.getLogeer() as MutableList<String>
        return ListapruebaTime
    }


    suspend fun OBtenerTimeControl(): Long? {
        bluetoothServices.bluetoothLeService!!.clearListLogger()
        delay(300) // Suspende el hilo sin bloquearlo
        bluetoothServices.bluetoothLeService?.listData?.clear()
        bluetoothLeService?.sendComando("4058")
        var ListapruebaTime = mutableListOf<String>()
        // Recopila datos en segundo plano sin bloquear el hilo principal
        delay(800)
        ListapruebaTime =
            bluetoothServices!!.bluetoothLeService!!.getLogeer() as MutableList<String>

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


    fun LoggerLuis(callback2: CallbackLoggerVersionCrudo) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.Default) {
                bluetoothServices.bluetoothLeService!!.clearListLogger()
                delay(800) // Suspende el hilo sin bloquearlo
                bluetoothServices.bluetoothLeService?.listData?.clear()
                bluetoothLeService?.sendComando("4061")
                Thread.sleep(40000)
                val ListaInicialEvento = bluetoothServices.bluetoothLeService!!.getLogeer()!!.toList() as MutableList<String>
                ListaInicialEvento.map {
                    Log.d("LogListasINiciales", "Evento $it")
                }

                bluetoothServices.bluetoothLeService!!.clearListLogger()
                delay(800) // Suspende el hilo sin bloquearlo
                bluetoothServices.bluetoothLeService?.listData?.clear()
                bluetoothLeService?.sendComando("4060")
                Thread.sleep(18000)
                val ListaInicialTiempo = bluetoothServices.bluetoothLeService!!.getLogeer()!!.toList() as MutableList<String>
                ListaInicialTiempo.map {
                    Log.d("LogListasINiciales", "Tiempo $it")
                }

                Log.d(
                    "LogListasINiciales",
                    "///////////////////////////////////////////////////////////////////////\n///////////////////////////////////////////////////////////////////////"
                )
                ListaInicialEvento.map {
                    Log.d("LogListasINiciales", "Evento $it")
                }
                Log.d(
                    "LogListasINiciales",
                    "///////////////////////////////////////////////////////////////////////\n///////////////////////////////////////////////////////////////////////"
                )
                ListaInicialTiempo.map {
                    Log.d("LogListasINiciales", "Tiempo $it")
                }
            }
        }
    }


    fun GetLoggerFinal(callback2: CallbackLoggerVersionCrudo) {
        CoroutineScope(Dispatchers.Main).launch {
            var ValoresFiltrados = mutableListOf<String>()
            var DatoREfechadoTIme = mutableListOf<String>()
            var ListapruebaEvent = mutableListOf<String>()
            var Event = mutableListOf<String>()

            var CL = 0
            try {
                callback2.onProgress("Iniciando Logger")
                callback2.onProgress("Iniciando Tiempo")
                BanderaTiempo = true

                withContext(Dispatchers.Default) {

                    Log.d("LogProcesoExtraccionDatos", "Termino recolectado Evento")
                    delay(500)
                    //var CB133 = OBtenerTimeControl()
                    bluetoothServices.bluetoothLeService!!.clearListLogger()
                    Log.d("LogProcesoExtraccionDatos", "Se esta recolectando la hora del control")
                    bluetoothLeService?.sendFirstComando("405B")
                    delay(500)
                    var T33CB1 = bluetoothServices!!.bluetoothLeService!!.getLogeer()!!.first()
                        .replace(" ", "").substring(16, 24).toLong(16)
                        .times(1000) /// as MutableList<String>
                    Log.d("LogProcesoExtraccionDatos", "Se termino la recollecion del time control")

                    Log.d("LogProcesoExtraccionDatos", "Se esta recolectando Evento")
                    bluetoothServices.bluetoothLeService!!.clearListLogger()
                    delay(800) // Suspende el hilo sin bloquearlo
                    bluetoothServices.bluetoothLeService!!.clearListLogger()
                    delay(800) // Suspende el hilo sin bloquearlo
                    bluetoothServices.bluetoothLeService?.listData?.clear()
                    bluetoothLeService?.sendComando("4061")
                    delay(50000)
                    ListapruebaEvent =
                        bluetoothServices.bluetoothLeService!!.getLogeer() as MutableList<String>
                    ListapruebaEvent.map {
                        Log.d("DatosCRUDOSEVENT", "$it")
                    }
                    var EventPRE = VEvent(ListapruebaEvent as MutableList<String?>)
                    EventPRE.map {
                        if (it!!.length > 20) {
                            Event.add(it)
                        }
                    }
                    Event.map {
                        println("DatosCRUDOSEVENT: $it")
                        Log.d("DatosCRUDOSEVENT", "$it")
                    }
                }

                BanderaTiempo = false
                callback2.onProgress("Finalizando")
            } catch (exc: Exception) {
                callback2.onProgress("Error: ${exc.message}")
            }
        }
    }


    fun dividirpaquetesFInales(listaFinal: MutableList<String>, tamLis: Int): MutableList<String> {
        val FinalLIstaTime = mutableListOf<String>()
        for (registro in listaFinal) {
            var startIndex = 0
            var registrosParciales = mutableListOf<String>()

            // Dividir cada registro en segmentos de tamLis caracteres
            while (startIndex < registro.length) {
                val endIndex = minOf(startIndex + tamLis, registro.length)
                registrosParciales.add(registro.substring(startIndex, endIndex))
                startIndex += tamLis
            }

            // Agregar los segmentos del registro actual al resultado final
            FinalLIstaTime.addAll(registrosParciales)
        }

        // Filtrar segmentos que no sean tamLis de puros 0 y que tengan una longitud de al menos tamLis
        val listaFiltrada =
            FinalLIstaTime.filter { it != "0".repeat(tamLis) && it.length >= tamLis }
        return listaFiltrada as MutableList<String>
    }

    inner class MyAsyncTaskGetLoggerFinal(

        var callback: CallbackLoggerVersionCrudo
    ) :
        AsyncTask<Int?, Int?, String>() {
        var ValoresFiltrados = mutableListOf<String>()
        var DatoREfechadoTIme = mutableListOf<String>()
        var ListapruebaEvent = mutableListOf<String>()
        var ListapruebaTime = mutableListOf<String>()

        var ListaTImeDespuesDelRefechado = mutableListOf<String>()
        var ListaEventoDespuesDelRefechado = mutableListOf<String>()
        var listaInvertidaTIME = mutableListOf<String>()
        var ListaTimeCrudo = mutableListOf<String>()
        var Event = mutableListOf<String>()
        var EventAntes = mutableListOf<String>()

        var CL = 0

        override fun onPreExecute() {

            clearLogger()
            callback.onProgress("Iniciando Logger")
            callback.onProgress("Iniciando Tiempo")
            BanderaTiempo = true

        }

        protected override fun doInBackground(vararg params: Int?): String? {


            ////////////////// SE ENVIA EL COMANDO PARA OBTENER LA PLANTILLA
            Log.d("LogProcesoExtraccionDatos", "Se esta recolectando la plantilla")
            bluetoothServices.bluetoothLeService!!.clearListLogger()
            bluetoothServices.bluetoothLeService!!.sendFirstComando("4051")
            Thread.sleep(800)  /////////////// SE ESPERA 800 MILISEGUNDOS
            ////////////////// SE GUARDA LA PLANTILLA EN UNA STRING
            var Plantilla = bluetoothServices.bluetoothLeService!!.getLogeer()!!.joinToString("")
                .replace(" ", "")

            ////////////////// SE VALIDA EL LARGO DE LA PLANTILLA SE TIENE COMO EJEMPLO 190 DAOD QUE EL VALOR QUE BUSCAMOS ES EL 179 a 180
            ////////////////// SI SE ENCUENTRA EL VALOR SE LE ASIGNA A preAT SINO SE TIENE COMO DEFAULT 2
            var preAT = "2" ////////////////// VALOR DEFAULT de ATMUestra
            if (Plantilla.length > 190) {
                preAT = getDecimal(Plantilla.substring(178, 180)).toString()
            }
            ////////////////// SE GUARDA LO QUE SE RECIVIO A ATMUestra
            val ATMUestra = preAT.toLong(16)
                .times(1000)   ////////////////// AQUI SE PASA EL VALOR A MILISEGUNDOS
            Log.d("LogProcesoExtraccionDatos", "Se esta terminando la recoleccion  la plantilla")
            bluetoothServices.bluetoothLeService!!.clearListLogger()
            bluetoothServices.bluetoothLeService?.listData?.clear()
            ////////////////// SE OBTIENE EL VALOR DE LA HORA DEL CONTROL
            Log.d("LogProcesoExtraccionDatos", "Se esta recolectando la hora del control")
            bluetoothLeService?.sendFirstComando("405B")
            Thread.sleep(800)
            ////////////////// se obtiene el valor del buffer se le quita los espacios y se obtiene el valor del control y se pasa a milisegundos
            var T33CB1 =
                bluetoothServices.bluetoothLeService!!.getLogeer()!!.joinToString("")
                    .replace(" ", "").substring(16, 24).toLong(16).times(1000)

            Thread.sleep(100)
            bluetoothServices.bluetoothLeService!!.clearListLogger()
            ////////////////// SE OBTIENE LA HORA DEL CONTROL Y SE PASA A MILISEGUNDOS
            var CElTime1 = GetNowDateExa().toLong(16).times(1000)
            Log.d("procesoPrincipal", "CB133 $T33CB1 CElTime1 $CElTime1")
            ////////////////// SE LIMPIA EL BUFFER DONDE SE ALMACENARA LOS DATOS A PEDIR
            bluetoothServices.bluetoothLeService!!.clearListLogger()
            //////////////////
            Thread.sleep(800)
            bluetoothServices.bluetoothLeService?.listData?.clear()
            //////////////////  SE ENVIA EL COMANDO DE LOGGER DE EVENTO
            bluetoothLeService?.sendComando("4061")
            ////////////////// Se espera CIERTO TIEMPO DADDO PARA QUE EL BUFFER SE LLENE Y PODER CONSULTARLO
            Thread.sleep(21000)//50000)
            ListapruebaEvent =
                bluetoothServices.bluetoothLeService!!.getLogeer() as MutableList<String>
            ////////////////// SE LE ASIGNA EL BUFFER A LA LISTA
            ////////////////// LA FUNCION  VEvent RECIBE LA LISTA Y SEPARA LOS DATOS QQUITANDO EL HEADER; EL CHECKSUME, DIVIDE LO RESTANTE EN PARTES DE 256 CARACTERES
            ////////////////// Y LUEGO LOS DIVIDE EN PAQUETES DE  28 CARACTERES
            var EventPRE = VEvent(ListapruebaEvent as MutableList<String?>)
            ////////////////// SE RECORRE LA LISTA DE EventPRE Y SE AGREGA LOS DATOS A UNA NUEVA LISTA CON LOS VALORES  CON LARGO IGUALES  A 28
            EventPRE.map {
                if (it!!.length == 28) {
                    EventAntes.add(it)
                }
            }
            ////////////////// TERMINA EL RECORRIDO DE LA LISTA
            //////////////////  SE VOLTEA LA LISTA PARA QUE LOS DATOS MAS RECIENTES QUEDEN HAASTA ARRIBA
            Event = EventAntes.reversed().toMutableList()
            Log.d("LogProcesoExtraccionDatos", "Termino recolectado Evento")
            ////////////////////////////////////////////////////////////////// OBteniendo Los datos de Time
            Log.d("LogProcesoExtraccionDatos", "Se esta recolectando Time")
            bluetoothServices.bluetoothLeService!!.clearListLogger()
            Thread.sleep(800) // Suspende el hilo sin bloquearlo
            bluetoothServices.bluetoothLeService?.listData?.clear()
            //////////////////  SE ENVIA EL COMANDO DE LOGGER DE TIEMPO
            bluetoothLeService?.sendComando("4060")
            ////////////////// Se espera CIERTO TIEMPO DADDO PARA QUE EL BUFFER SE LLENE Y PODER CONSULTARLO
            Thread.sleep(15000)//50000)
            ////////////////// SE LE ASIGNA EL BUFFER A LA LISTA
            var ListapruebaTime =
                bluetoothServices.bluetoothLeService!!.getLogeer() as MutableList<String>
            Log.d("LogProcesoExtraccionDatos", "Termino recolectado Time")
            //////////////////  SE ASIGNA LA LISTA A UN STRING SIN ESPACIOS PARA POSTERIORMENTE COMPROBAR EL CHECKSUME COINCIDA
            var replaceCadena = ListapruebaTime.joinToString("").replace(" ", "")
            ////////////////// SE SUMAN EXADECIMALES SIN EL CHECKSUME  PARA CALCULAR EL CHECKSUME DE LOS DATOS RECIBIDOS
            var isChecksumOk = sumarParesHexadecimales(
                replaceCadena.substring(
                    0,
                    replaceCadena.length - 8
                )
            ).uppercase()
            ////////////////// SE TERMINA DE CALCULAR EL CHECKSUME
            ////////////////// SE OBTIENE EL CHECKSUME QUE MANDA EL CONTROL
            var toChecksume =
                replaceCadena.substring(replaceCadena.length - 8, replaceCadena.length).uppercase()
            ////////////////// SE INTENTARA HACER INTENTOS POR SI EL CHECKSUME CALCULADO NO COINCIDE CON EL RECIBIDO
            val maxIntentos = 1
            var intentos = 0
            ////////////////// BANDERA DE SI COINCIDE EL CHECKSUME POR SI NO COINCIDE
            var checksumCorrecto = false

            ////////////////// INICIA UN CICLO WHILE
            while (intentos < maxIntentos && !checksumCorrecto) {
                // Verificar el checksum
                if (!isChecksumOk.equals(toChecksume)) {

                    // Incrementar el contador de intentos DAOD QUE FALLO Y NO COINCIDE EL CHECKSUME
                    intentos++

                    if (intentos < maxIntentos) {
                        // Intentar obtener los datos nuevamente si no es el último intento
                        bluetoothServices.bluetoothLeService!!.clearListLogger()
                        Thread.sleep(200)
                        bluetoothServices.bluetoothLeService?.listData?.clear()
                        bluetoothLeService?.sendComando("4060")
                        Thread.sleep(15000)
                        ////////////////// SE VUELVE A CALCULAR EL CHECKSUME PARA VALIDARLO
                        ListapruebaTime =
                            bluetoothServices.bluetoothLeService!!.getLogeer() as MutableList<String>
                        replaceCadena = ListapruebaTime.joinToString("").replace(" ", "")
                        isChecksumOk = sumarParesHexadecimales(
                            replaceCadena.substring(
                                0,
                                replaceCadena.length - 8
                            )
                        ).uppercase()
                        toChecksume =
                            replaceCadena.substring(replaceCadena.length - 8, replaceCadena.length)
                                .uppercase()

                    }
                } else {
                    // El checksum es correcto, salir del bucle
                    checksumCorrecto = true
                    replaceCadena = ListapruebaTime.joinToString("").replace(" ", "")
                }
            }
            ////////////////// TERMINA EL CICLO WHILE
            ////////////////// INICIA LA NEGACION DE LA VALIDACION DE SI EL CHECKSUME
            if (!checksumCorrecto) {

                // Si después de los intentos el checksum sigue siendo incorrecto, limpiar ListapruebaTime Y NO SE SIGUE CON LA OBTENCION DE LOS DATOS DE EVENTO
                ListapruebaTime.clear()
            } ////////////////// TERMINA LA VALIDACION DE SI EL CHECKSUME
            ////////////////// INICIA EL ELSE DE LA VALIDACION DE SI EL CHECKSUME
            else {
                ////////////////// SE OBTUVO CORRECTAMENTE EL LOGGER DE TIEMPO
                ////////////////// SE OBTIENE EL TIMESTAMP DEL CONTROL POR SEGUNDA VEZ
                bluetoothServices.bluetoothLeService!!.clearListLogger()
                bluetoothLeService?.sendFirstComando("405B")
                Thread.sleep(500)
                var T33CB2 =
                    bluetoothServices!!.bluetoothLeService!!.getLogeer()!!.first().replace(" ", "")
                        .substring(16, 24).toLong(16).times(1000) /// as MutableList<String>
                ////////////////// SE OBTIENE LA HORA DEL DISPOSITIVO POR SEGUNDA VEZ
                var CElTime2 = GetNowDateExa().toLong(16).times(1000)
                Thread.sleep(100)
                bluetoothServices.bluetoothLeService!!.clearListLogger()
                Log.d("procesoPrincipal", "T33CB2 $T33CB2 CElTime2 $CElTime2")

                ////////////////// SE QUITA EL HEADER DE 16 CARACTERES AL PRINCPIO DE LOS DATOS Y LOS 8 ULTIMOS QUE PERTENECE AL CHECKSUME
                var listtoEval = replaceCadena.substring(16, replaceCadena.length - 8)
                ////////////////// LA FUNCION dividirEnPaquetes DIVIDE UN STRING EN UNA LISTA CON REGISTROS DE 256
                var listaFinal = dividirEnPaquetes(listtoEval, 256)
                val FinalLIstaTime = mutableListOf<String>()
                ////////////////// SE VA A GUARDAR LOS REGISTROS DE 256 A EXCEPCION DEL ULTIMO REGISTRO QUE ES DONDE SE ENCUENTRA LA CONVERGENCIA ENTRE LOS DATOS MAS
                ////////////////// RECIENTES Y LOS VIEJOS
                ////////////////// INICIA CICLO FOR
                for (i in 0 until listaFinal.size - 1) {
                    FinalLIstaTime.add(listaFinal[i])
                }
                ////////////////// TERMINA EL CICLO FOR
                ////////////////// SE CREA LA LISTA DE ULTIMOPAQUETE
                var ultimopaquete = mutableListOf<String>()
                ////////////////// SE AGREGA EL ULTIMO REGISTRO A LA LISTA DE ultimopaquee
                ultimopaquete.add(listaFinal.last())

                //////////////////  la funcion dividirpaquetes18TIme va a crear paquetes de 18 caracteres y va a excluir los ultimos 4 caracteres que sobran y los que son puros 0
                ValoresFiltrados = dividirpaquetes18TIme(FinalLIstaTime)

                ////////////////// OBTENEMOS EN UN STRING LA INFORMACION DE ULTIMO PAQUETE
                var lastDato = ultimopaquete.last()
                ////////////////// OBTENEMOS EL INDICE QUE NOS DICE DONDE SE QUEDO EL DATO MAS RECIENTE, SE PASA A DECIMAL Y SE MULTIPLICA POR DOS
                val indiceFinal =
                    getDecimal(lastDato.substring(lastDato.length - 4, lastDato.length - 2)) * 2
                ////////////////// CHECAMOS EL ULTIMO PAR PARA SABER SI SE TIENE DATOS NUEVOS Y VIEJOS ENTONCES SERA UN 00
                val BanderaListaCompleta = lastDato.substring(lastDato.length - 2, lastDato.length)
                ////////////////// OBTENEMOS EL STRING DE LOS DATOS NUEVOS SE COMENZARA DESDE EL DATO  0  HASTA EL INDICE FINAL QUE OBTUVIMOS PREVIAMENTE
                val valorNUevo = lastDato.substring(0, indiceFinal)
                ////////////////// CREAMOS UNA LISTA TEMPORAL DONDE LA GUARDAREMOS Y AGREGAREMOS EL STRING QUE GUARDAMOS
                val tempListNUeva = mutableListOf<String>()
                tempListNUeva.add(valorNUevo)
                ////////////////// DIVIDIREMOS EL STRING EN PAQUETES DE 18
                val ListaULtimoPAquete = dividirpaquetes18TIme(tempListNUeva)

                ////////////////// VALIDAREMOS SI ES 00 (SE TIENEN DATOS NUEVOS y VIEJOS) INICIA EL IF
                if (BanderaListaCompleta == "00") {
                    ////////////////// LOS DATOS LOS INGRESAREMOS AL FINAL DE LA LISTA
                    var indice =
                        ValoresFiltrados.size ////////////////// INDICE A DONDE VAN A SER INGRESADOS
                    ValoresFiltrados.addAll(indice, ListaULtimoPAquete)

                }  ////////////////// VALIDAREMOS SI ES 00 (SE TIENEN DATOS NUEVOS y VIEJOS) TERMINA EL IF

                ////////////////// COMIENZA EL  else if
                else if (BanderaListaCompleta != "00") {
                    ////////////////// SI ES DIFERENTE A 00 SIGNIFICA QUE TENEMOS SOLAMENTE DATOS NUEVOS POR ENDE LOS DIVIDIREMOS EN PAQUETE DE 18
                    val valorNUevo = lastDato.substring(lastDato.length - 4, lastDato.length)
                    val tempListNUeva = mutableListOf<String>()
                    tempListNUeva.add(valorNUevo)
                    var indice =
                        ValoresFiltrados.size ////////////////// INDICE A DONDE VAN A SER INGRESADOS
                    ////////////////// LOS DATOS LOS INGRESAREMOS AL FINAL DE LA LISTA
                    ValoresFiltrados.addAll(indice, ListaULtimoPAquete)
                    val ListaULtimoPAquete = dividirpaquetes18TIme(tempListNUeva)

                }
                ////////////////// TERMINA  else if

                ////////////////// EMPIEZA EL IF DE VALIDAR SI LA LISTA DE VALORES FILTRADOS ESTA VALICA (SI TENEMOS REGISTRO DE TIME)

                if (ValoresFiltrados.isNotEmpty()) {

                    ////////////////// SE MANDA AL CALLBACK LOS DATOS CRUDOS DE EVENTO
                    runOnUiThread {
                        callback.getEventCrudo(agregar00AlPenultimoCaracter(Event, 2))
                    }
                    ////////////////// SE VOLTEA LOS VALORES DE TIEMPO PARA QUE LOS DATOS QUEDEN DE MAS RECIENTE A MAS ANTIGUO
                    listaInvertidaTIME = ValoresFiltrados.reversed()
                        .toMutableList()
                    ListaTimeCrudo = listaInvertidaTIME

                    ////////////////// SE ENVIA LOS DATOS DE TIME CRUDOS SIN PROCESAR AL CALLBACK
                    runOnUiThread {
                        callback.getTimeCrudo(agregar00AlPenultimoCaracter(ListaTimeCrudo, 2))
                    }
                    ////////////////// SE TERMINA de ENVIAR LOS DATOS DE TIME CRUDOS SIN PROCESAR AL CALLBACK


                    var A = T33CB2 - T33CB1
                    var B = CElTime2 - CElTime1
                    val C: Long = (((A - B).toDouble() / B) * ATMUestra).toLong()
                    var AV = 1
                    var n = 0
                    var E: Long = 0L
                    var D: Long = T33CB1 - CElTime1
                    var J = 0L
                    var count = 0
                    val CantidadEventos04: MutableList<Int> = ArrayList()
                    ////////////////// SE ITERA LA LISTA DE EVENTOS PARA VER Y CONTAR LOS QUE SON EVENTO 04
                    for (item in Event) {
                        val EVENT_TYPE = item.substring(16, 18)
                        if (EVENT_TYPE == "04") {
                            count++ // Incrementamos el contador de eventos tipo "04"
                            println("item $item EVENT_TYPE $EVENT_TYPE ")
                        }
                    }
                    ////////////////// SE TERMINA DE ITERA LA LISTA DE EVENTOS PARA VER Y CONTAR LOS QUE SON EVENTO 04

                    var L = count
                    /////////////// BANDERA PARA SABER SI HAY EVENTOS
                    var HayEvento = false
                    Log.d("LogProcesoExtraccionDatos", "Cantidad de eventos 04 $L ")
                    /////////////// SE VA A ITERAR LA LISTA DE TIEMPO PARA EL RE ESTAMPADO DEL TIME
                    for (i in 0 until listaInvertidaTIME.size - 1) {
                        J = C * AV
                        AV = AV + 1
                        var timeActual = listaInvertidaTIME[i].substring(0, 8).toLong(16)
                        var timeAnterior = listaInvertidaTIME[i + 1].substring(0, 8).toLong(16)
                        var Evento = ""
                        /////////////// SE VA A ITERAR LA LISTA DE EVENTO PARA BUSCAR LOS EVENTOS QUE OCURREN ENTRE LOS EVENTOS DE TIME
                        for (eventRegistro in Event) {
                            val eventTime = eventRegistro.substring(8, 16).toLong(16)
                            /////////////// INICIO DEL IF
                            if (eventTime in (timeAnterior + 1)..timeActual) {
                                /////////////// SI SE ENCUENTRA UN REGISTRO SE CAMBIA LA VANDERA Y SE GUARDA EL REGISTRO DE ESE EVENTO
                                HayEvento = true
                                Evento = eventRegistro
                            }  /////////////// FIN DEL IF
                            /////////////// INICIO DEL ELSE
                            else {
                                HayEvento = false
                            } /////////////// FIN DEL ELSE
                        }
                        /////////////// SE TERMINA DE ITERAR LA LISTA DE EVENTO PARA BUSCAR LOS EVENTOS QUE OCURREN ENTRE LOS EVENTOS DE TIME

                        /////////////// SE HACE EL REFECHADO DE LOS EVENTOS DE TIME
                        var eventoTimeMil =
                            listaInvertidaTIME[i].substring(0, 8).toLong(16).times(1000)
                        var TimeRefechado = eventoTimeMil - D - E + J
                        var ConvertidotoExa = toHexString(TimeRefechado / 1000)
                        var DatotoStamp =
                            ConvertidotoExa.substring(0, 8) + listaInvertidaTIME[i].substring(8)
                        ListaTImeDespuesDelRefechado.add(DatotoStamp)

                        /////////////// INICIO DE LA VALIDACION DE SI HAY EVENTOS
                        if (HayEvento) {
                            /////////////// SE OBTIENE LOS DATOS
                            var EventoFinalTimetoCheck =
                                Evento.substring(8, 16).toLong(16).times(1000)
                            var TlnEv = Evento.substring(0, 8).toLong(16).times(1000)
                            var TempFin =
                                ReturnValLoggerTraduccion(Evento.substring(22, 26)).toFloat()
                            var TempIn =
                                ReturnValLoggerTraduccion(Evento.substring(18, 22)).toFloat()

                            var Even04 =
                                Evento.substring(16, 18) /////////////// SE OBTIENE EL EVENTO 04

                            /////////////// INICIO DE LA VALIDACION DE SI ES EVENTO 04
                            if (Even04.equals("04")) {

                                var inicio_FinconV = false
                                /////////////// INICIO DEL IF inicio_FinconV
                                if (inicio_FinconV) {

                                }
                                /////////////// FIN DEL IF inicio_FinconV
                                /////////////// INICIO DEL ELSE  inicio_FinconV
                                else {
                                    when (CL) {
                                        0 -> {
                                            var dTempEvento: Long =
                                                (if ((TempFin - TempIn) < 0) 0L else (TempFin - TempIn).toLong()) * 1000L
                                            var F: Long = 10 * 1000L
                                            E += D + dTempEvento * F
                                            var TlnEv: Long = TlnEv - D - E - J
                                            E = E + D + dTempEvento * F
                                            var TFinEvento = EventoFinalTimetoCheck - D - E - J
                                            var ConvertidotoExaFinal =
                                                toHexString(TFinEvento / 1000)
                                            var ConvertidotoExaInicial = toHexString(TlnEv / 1000)

                                            ListaEventoDespuesDelRefechado.add(
                                                ConvertidotoExaInicial + ConvertidotoExaFinal + Evento.substring(
                                                    16
                                                )
                                            )

                                        }
                                    }
                                }
                                /////////////// FINAL DEL ELSE  inicio_FinconV
                            }
                            /////////////// FINAL DE LA VALIDACION DE SI ES EVENTO 04


                            /////////////// INICIO DEL ELSE DE LA  VALIDACION DE SI ES EVENTO 04
                            else {


                                var TFinEvento = EventoFinalTimetoCheck - D - E - J
                                var TInEvento = EventoFinalTimetoCheck - D - E - J
                                var ConvertidotoExaFinal = toHexString(TFinEvento / 1000)
                                var ConvertidotoExaInicial = toHexString(TInEvento / 1000)

                                /////////////// SE AGREGA EL REGISTRO A LA LISTA FINAL DE EVENTOS
                                ListaEventoDespuesDelRefechado.add(
                                    ConvertidotoExaInicial + ConvertidotoExaFinal + Evento.substring(
                                        16
                                    )
                                )

                            }
                            /////////////// FINAL DEL ELSE DE LA  VALIDACION DE SI ES EVENTO 04
                        }
                        /////////////// FIN DE LA VALIDACION DE SI HAY EVENTOS
                    }
                    /////////////// SE TERMINA DE ITERAR LA LISTA DE TIEMPO PARA EL RE ESTAMPADO DEL TIME
                }
                ////////////////// TERMINA EL IF DE VALIDAR SI LA LISTA DE VALORES FILTRADOS ESTA VALICA (SI TENEMOS REGISTRO DE TIME)
            }
            ////////////////// TERMINA EL ELSE DE LA VALIDACION DE SI EL CHECKSUME
            return ""
        }

        override fun onPostExecute(result: String) {

            callback.getTime(agregar00AlPenultimoCaracter(ListaTImeDespuesDelRefechado, 2))
            callback.getEvent(agregar00AlPenultimoCaracter(ListaEventoDespuesDelRefechado, 2))
            callback.onProgress("Finalizando")
            callback.onSuccess(true)
        }
    }


    fun getLog(callback2: CallbackLoggerVersionCrudo) {
        var callback = callback2
        var ValoresFiltrados = mutableListOf<String>()
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
                    Log.d("LogProcesoExtraccionDatos", "Se esta recolectando Evento")
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
                    ListapruebaEvent =
                        bluetoothServices.bluetoothLeService!!.getLogeer() as MutableList<String>

                    //  ListapruebaEvent = ObtenerLoggerEventComand()// bluetoothServices.bluetoothLeService!!.getLogeer() as MutableList<String> //getInfoList() as MutableList<String>

                    ListapruebaEvent.map {
                        Log.d("DatosCRUDOSEVENT", "$it")
                    }
                    var EventPRE = VEvent(ListapruebaEvent as MutableList<String?>)

                    EventPRE.map {
                        if (it!!.length > 20) {
                            Event.add(it)
                        }
                    }
                    Log.d("LogProcesoExtraccionDatos", "Termino recolectado Evento")
                    delay(500)
                    //var CB133 = OBtenerTimeControl()
                    bluetoothServices.bluetoothLeService!!.clearListLogger()
                    Log.d("LogProcesoExtraccionDatos", "Se esta recolectando la hora del control")
                    bluetoothLeService?.sendFirstComando("405B")
                    delay(500)
                    var T33CB1 = bluetoothServices!!.bluetoothLeService!!.getLogeer()!!.first()
                        .replace(" ", "").substring(16, 24).toLong(16)
                        .times(1000) /// as MutableList<String>
                    Log.d("LogProcesoExtraccionDatos", "Se termino la recollecion del time control")
                    delay(100)
                    bluetoothServices.bluetoothLeService!!.clearListLogger()
                    var CElTime1 = GetNowDateExa().toLong(16).times(1000)
                    Log.d("procesoPrincipal", "CB133 $T33CB1 CElTime1 $CElTime1")
//                    Log.d("procesoPrincipal", "CB133 $CB133")
                    Log.d("LogProcesoExtraccionDatos", "Se esta recolectando Time")
                    var ListapruebaTime =
                        ObtenerLoggerTimeComand()//bluetoothServices!!.bluetoothLeService!!.getLogeer() as MutableList<String>

                    ListapruebaTime.map {
                        Log.d("informacionListaBle", it)
                    }

                    var replaceCadena = ListapruebaTime.joinToString("").replace(" ", "")
                    Log.d(
                        "ListapruebaTime",
                        "replaceCadena ${replaceCadena.length} $replaceCadena   \n $ListapruebaTime"
                    )


                    var isChecksumOk = sumarParesHexadecimales(
                        replaceCadena.substring(
                            0,
                            replaceCadena.length - 8
                        )
                    ).uppercase()
                    var toChecksume =
                        replaceCadena.substring(replaceCadena.length - 8, replaceCadena.length)
                            .uppercase()

                    Log.d(
                        "ProcesoDelLoggerTime",
                        "isChecksumOk $isChecksumOk  toChecksume $toChecksume"
                    )
                    val maxIntentos = 3
                    var intentos = 0
                    var checksumCorrecto = false

                    while (intentos < maxIntentos && !checksumCorrecto) {
                        // Verificar el checksum
                        if (!isChecksumOk.equals(toChecksume)) {
                            // Incrementar el contador de intentos
                            intentos++

                            Log.d(
                                "ProcesoDelLoggerTime",
                                "Intento $intentos: isChecksumOk es incorrecto"
                            )

                            if (intentos < maxIntentos) {
                                // Intentar obtener los datos nuevamente si no es el último intento
                                ListapruebaTime = ObtenerLoggerTimeComand()
                                replaceCadena = ListapruebaTime.joinToString("").replace(" ", "")
                                isChecksumOk = sumarParesHexadecimales(
                                    replaceCadena.substring(
                                        0,
                                        replaceCadena.length - 8
                                    )
                                ).uppercase()
                                toChecksume = replaceCadena.substring(
                                    replaceCadena.length - 8,
                                    replaceCadena.length
                                ).uppercase()

                            }
                        } else {
                            // El checksum es correcto, salir del bucle
                            checksumCorrecto = true
                            replaceCadena = ListapruebaTime.joinToString("").replace(" ", "")
                            Log.d("ProcesoDelLoggerTime", "isChecksumOk es Correcto")
                        }
                    }

                    Log.d("LogProcesoExtraccionDatos", "Se Termino la recollecion Time")
                    if (!checksumCorrecto) {
                        // Si después de los tres intentos el checksum sigue siendo incorrecto, limpiar ListapruebaTime
                        Log.d(
                            "ProcesoDelLoggerTime",
                            "No se pudo verificar el checksum después de $maxIntentos intentos. Limpiando ListapruebaTime."
                        )
                        ListapruebaTime.clear()
                    } else {


                        var listtoEval = replaceCadena.substring(16, replaceCadena.length - 8)
                        Log.d("ProcesoDelLoggerTime", "listtoEval ${listtoEval.length}")
                        var listaFinal = dividirEnPaquetes(listtoEval, 256)
                        listaFinal.forEachIndexed { index, paquete ->
                            println("Paquete ${index + 1}: $paquete")
                            Log.d("ProcesoDelLoggerTime", " $paquete")
                        }
                        Log.d(
                            "ProcesoDelLoggerTime",
                            " \n \n ----------------------- ${listaFinal.size}"
                        )

                        val FinalLIstaTime = mutableListOf<String>()
                        var FinalLIstaTimeFINAL = mutableListOf<String>()
                        for (i in 0 until listaFinal.size - 1) {
                            FinalLIstaTime.add(listaFinal[i])
                        }
                        Log.d(
                            "ProcesoDelLoggerTime",
                            " \n \n ----------------------- ${FinalLIstaTime.size}"
                        )
                        FinalLIstaTime.map { //   .forEachIndexed { index, paquete ->
                            //  println("Paquete ${index + 1}: $paquete")
                            Log.d("FinalLIstaTime", " $it")
                        }

                        ValoresFiltrados = dividirpaquetes18TIme(FinalLIstaTime)
                        var listaEntera = listaFinal.last()
                            .substring(listaFinal.last().length - 2, listaFinal.last().length)
                        var indexLast = 0
                        var ValoresToSeachListaFinalIncompletaDatosRecientes =
                            mutableListOf<String>()
                        var ValoresToSeachListaFinalIncompletaDatosViejos = mutableListOf<String>()

                        Log.d(
                            "ValoresFiltrados",
                            "ValoresFiltrados ${ValoresFiltrados.size}  ${ValoresFiltrados.toString()}"
                        )

                        ValoresFiltrados.map {
                            //    Log.d("ValoresFiltrados","$it")
                        }

                        ///////////////////OBtener Los datos de evento
                        if (ValoresFiltrados.isNotEmpty()) {
//                            var ListapruebaEvento =  ObtenerLoggerEventComand ()
//                            ListapruebaEvento.map {
//                                Log.d("ValoresEventoCrudo","$it")
//                            }
//                            var replaceCadenaEvento =  ListapruebaEvento.joinToString("").replace(" ", "")


                            if (!Event.isNullOrEmpty()) {

                                //LA FUNCION VEvent SEPARA LOS DATOS EN EL HEADER Y LOS DATOS EN BRUTO


                                Event.map {
                                    Log.d("datosdeEvento", "$it")
                                }


                                val listaInvertidaTIME = ValoresFiltrados.reversed()

                                bluetoothServices.bluetoothLeService!!.clearListLogger()
                                bluetoothLeService?.sendFirstComando("405B")
                                delay(500)
                                var T33CB2 =
                                    bluetoothServices!!.bluetoothLeService!!.getLogeer()!!.first()
                                        .replace(" ", "").substring(16, 24).toLong(16)
                                        .times(1000) /// as MutableList<String>

                                delay(100)
                                bluetoothServices.bluetoothLeService!!.clearListLogger()
                                var CElTime2 = GetNowDateExa().toLong(16).times(1000)
                                Log.d("procesoPrincipal", "T33CB2 $T33CB2 CElTime2 $CElTime2")
                                delay(1000)
                                var A = T33CB2 - T33CB1
                                var B = CElTime2 - CElTime1
                                var ATMUestra = "2".toLong(16)!!.times(1000)
                                var C = ((A - B) / B) * ATMUestra
                                var AV = 0L
                                var E: Long = 0L
                                var D: Long = T33CB1 - CElTime1


                                for (i in listaInvertidaTIME.indices) {
                                    var J = C * AV
                                    AV = AV + 1
                                    Log.d(
                                        "procesoPrincipal", " ${listaInvertidaTIME[i]}   ${
                                            convertirHexAFecha(
                                                listaInvertidaTIME[i].substring(0, 8)
                                            )
                                        }"
                                    )

                                    var timeActual =
                                        listaInvertidaTIME[i].substring(0, 8).toLong(16)
                                    var timeAnterior =
                                        listaInvertidaTIME[i - 1].substring(0, 8).toLong(16)

                                    var HayEvento = false
                                    if (!HayEvento) {

                                        var TimeRefechado =
                                            listaInvertidaTIME[i].substring(0, 8).toLong(16)
                                                .times(1000) - D - E + J
                                        var ConvertidotoExa = toHexString(TimeRefechado / 1000)
                                        DatoREfechadoTIme.add(
                                            ConvertidotoExa + listaInvertidaTIME[i].substring(
                                                8
                                            )
                                        )
                                    } else {
                                        var Evento = "6674E8626674E86204003B00067F"
                                        var EventoFinalTimetoCheck =
                                            Evento.substring(8, 16).toLong(16)!!.times(1000)
                                        var TlnEv = Evento.substring(0, 8).toLong(16)!!.times(1000)
                                        var TempFin =
                                            ReturnValLoggerTraduccion(
                                                Evento.substring(
                                                    22,
                                                    26
                                                )
                                            ).toFloat()
                                        var TempIn =
                                            ReturnValLoggerTraduccion(
                                                Evento.substring(
                                                    18,
                                                    22
                                                )
                                            ).toFloat()

                                        var EventoIs04 = false

                                        if (EventoIs04) {

                                            var inicio_FinconV = false
                                            if (inicio_FinconV) {

                                            } else {
                                                when (CL) {
                                                    0 -> {
                                                        var dTempEvento: Long =
                                                            (if ((TempFin - TempIn) < 0) 0L else (TempFin - TempIn).toLong()) * 1000L

                                                        var F: Long = 10 * 1000L
                                                        E += D + dTempEvento * F
                                                        var TlnEv: Long = TlnEv - D - E - J


                                                    }

                                                    1 -> {

                                                    }

                                                    else -> {

                                                    }
                                                }
                                            }
                                        } else {


                                            var TFinEvento = EventoFinalTimetoCheck - D - E - J


                                        }
                                    }

                                }

                                DatoREfechadoTIme.map {
                                    Log.d(
                                        "ProcesoPrincipal",
                                        " Datos Refechados $it ${
                                            convertirHexAFecha(
                                                it.substring(
                                                    0,
                                                    8
                                                )
                                            )
                                        }"
                                    )
                                }

                                Thread.sleep(100)
                                Event.map {
                                    Log.d("ObtenerLoggerPrueba", "Traducido Event ${it}")
                                }
                            } else {
                                Log.d("ObtenerLoggerPrueba", "lista Event vacia ")
                            }


                        }


                        var CL = 0
                        when (CL) {
                            0 -> {
                                var CElTime2 = GetNowDateExa()


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
                            callback.getTime(
                                agregar00AlPenultimoCaracter(
                                    ValoresFiltrados as MutableList<String>,
                                    2
                                )
                            )
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

    fun dividirpaquetes28TIme(listaFinal: MutableList<String>): MutableList<String> {
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
        val listaFiltrada =
            FinalLIstaTime.filter { it != "0000000000000000000000000000" && it.length >= 28 }
        return listaFiltrada as MutableList<String>
    }

    fun dividirpaquetes18TIme(listaFinal: MutableList<String>): MutableList<String> {
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
        val numPaquetes =
            (longitudTotal + tamanoPaquete - 1) / tamanoPaquete // Calcula el número de paquetes

        val paquetes = mutableListOf<String>()

        for (i in 0 until numPaquetes) {
            val inicio = i * tamanoPaquete
            val fin = minOf((i + 1) * tamanoPaquete, longitudTotal)
            val paquete = cadena.substring(inicio, fin)
            paquetes.add(paquete)
        }

        return paquetes
    }


    private fun DividirLoggerTime(listapruebaTime: MutableList<String>) {
        var arrayListInfo: MutableList<String?> = ArrayList()


        if (!listapruebaTime.isEmpty()) {
            listapruebaTime.map {
                Log.d("ListapruebaTime", "$it")
            }
            Log.d("ListapruebaTime", "\n \n")

            val s = quitarEspaciosYConvertirAStringBuilder(listapruebaTime)
            val datos: MutableList<String> = ArrayList()
            val datos2: MutableList<String> = ArrayList()
            //header
            Log.d("", "sssTiempo:$s")
            arrayListInfo.add(s.substring(0, 4)) //head
            arrayListInfo.add(s.substring(4, 12)) //
            arrayListInfo.add(s.substring(12, 14)) //modelo trefpb
            arrayListInfo.add(s.substring(14, 16)) //version
            Log.d("datosCrudosTiempoSALidaFinal", "------------> arrayListInfo $arrayListInfo")
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
            Log.d("datosCrudosTiempoSALidaFinal", "------------> arrayListInfo $arrayListInfo")
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
                Log.d("ListapruebaTime", "$it")
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
                Log.d("resultadoFinal", "$it")
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
            Log.d("datosCrudosTiempoSALidaFinal", "------------> arrayListInfo $arrayListInfo")
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
                Log.d("datosCrudosTiempoSALidaFinal", "st $st")
                do { //dividir toda la información en paquetes de 128 bytes
                    Log.d("datosCrudosTiempoSALidaFinal", "i + 256 ${i + 256} i $i ")
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
            var data: MutableList<String>? = mutableListOf<String>()
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


    inner class MyAsyncTaskSendDateHour(private val callback: ConexionTrefp.MyCallback) :
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

                FinalListData = GetRealDataFromHexaImbera.convertVersion2(
                    listData as List<String>,
                    "Handshake",
                    "",
                    ""
                ) as MutableList<String?>
                listData = GetRealDataFromHexaImbera.GetRealDataVersion2(
                    FinalListData as List<String>,
                    "Handshake",
                    "",
                    ""
                ) as MutableList<String?>

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
                var HW = if (listData.size == 4) 0.0 else listData.get(4).toString().toDouble()
                //    var HW = listData[4].toString().toDouble() ?: 0.0

                listData.map {
                    Log.d("DebugdelaHora", " $it")
                }
                Log.d("DebugdelaHora", "FW $FW  HW $HW ")
                mensaje = when (name) {
                    "IMBERA-HEALTH" -> {
                        Log.d("DebugdelaHora", "Control IMBERA-HEALTH ")
                        var statusBCD = actualizarHoraBCD()
                        Log.d("actualizarHoraBCD", "actualizarHoraBCD result  $statusBCD")
                        if (statusBCD) "F1 3D" else "F1 3E"
                    }

                    "CEB_IN" -> {

                        if (FW >= 2.0 && HW >= 1.1) {
                            Log.d(
                                "DebugdelaHora", "Control CEB_IN >= 2.0  && HW >= 1.1 " +
                                        " "
                            )
                            clearLogger()
                            var statusBCD = actualizarHoraBCD()
                            Log.d("actualizarHoraBCD", "actualizarHoraBCD result  $statusBCD")
                            if (statusBCD) "F1 3D" else "F1 3E"
                        } else {
                            Log.d("DebugdelaHora", "Control CEB_IN diferente a =! 2.0  <1.1 ")
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
                        Log.d("DebugdelaHora", "Control else ")
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


    fun getInfoWifi(callback: ConexionTrefp.MyCallback) {
        val job = CoroutineScope(Dispatchers.IO).launch {

            var FinalPlantillaValorestoExa = mutableListOf<Pair<String, String>>()
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
                if (getStatusConnectBle()) {
                    bluetoothServices.bluetoothLeService!!.sendComandoW(command, toSendb)

                    Thread.sleep(500)
                    listData = bluetoothServices.bluetoothLeService!!.dataFromBroadcastUpdate





                    Log.d("wifiCommandStat", ":$listDatafinal    listData $listData")
                    if (listData.isNullOrEmpty()) {
                        callback.onError("Lista vacia")
                    } else {
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
                } else {
                    callback.onError("Desconectado")
                }


            }
            corrutina1.join()
            val corrutina2 = launch {
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


    fun MyAsyncTaskSendWifiPrincipal(
        ssid: String,
        pass: String,
        callback: ConexionTrefp.MyCallback
    ) {
        SendWifi(ssid, pass, "BLE_WIFI_NEW_SSID=", callback)
    }

    fun MyAsyncTaskSendWifiSecundario(
        ssid: String,
        pass: String,
        callback: ConexionTrefp.MyCallback
    ) {
        SendWifi(ssid, pass, "BLE_WIFI_NEW_SSID_2=", callback)
    }

    fun SendWifi(ssid: String, pass: String, comando: String, callback: ConexionTrefp.MyCallback) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            bluetoothLeService = bluetoothServices.bluetoothLeService
//            var bluetoothLeService = conexionTrefp2.bluetoothLeService
            Log.d("BtnSendWifi", "bluetoothLeService  $bluetoothLeService")
            var listData: List<String>? = arrayListOf()
            val toSendb: ByteArray
            var bytesTemp: Array<Byte?>? = null
            var command = ""
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
            var FinalPlantillaValorestoExa = mutableListOf<Pair<String, String>>()
            val corrutina1 = launch {

                callback.onProgress("Iniciando")
                try {
                    callback.onProgress("Realizando")
                    if (bluetoothLeService == null) {
                        callback.onError("Error de conexión")
                        callback.onSuccess(false)
                    } else {
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
            val corrutina2 = launch {
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
        callback: MyCallback
    ) {
        var error = false
        var textError = ""
        var divFirmware = dividirNewFirmware(FWWW.replace(" ",""))

        val job = CoroutineScope(Dispatchers.IO).launch {

            val corrutina1 = launch {
                callback.onProgress("Iniciando")
                Log.d("firmwarePASOS", "Iniciando")

                listData!!.clear()


                Log.d(
                    "firmwarePASOS",
                    "firmware corrutina1 handshake listData $listData  infolist ${getInfoList()} "
                )

                if (GetStatusBle()) {
                    if (divFirmware) {
                        bluetoothServices.bluetoothLeService!!.clearListLogger()
                        delay(10) // Suspende el hilo sin bloquearlo
                        bluetoothServices.bluetoothLeService?.listData?.clear()
                        delay(2000)
                        bluetoothServices.bluetoothLeService!!.sendComando("4046")
                        delay(800)
                        listData = getInfoList() as MutableList<String?>
                        Log.d(
                            "firmwarePASOS",
                            "firmware corrutina1 handshake listData $listData  infolist ${getInfoList()} "
                        )
                    } else {
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
                Log.d(
                    "firmwarePASOS",
                    "Realizando GetStatusBle  ${GetStatusBle()} divFirmware $divFirmware  listData $listData"
                )
                if (GetStatusBle()) {
                    if (divFirmware) {
                        if (containsPair(listData!!, "F1", "03")) {
                            Log.d("firmwarePASOS", "2251")

                            bluetoothServices.bluetoothLeService!!.clearListLogger()
                            delay(800) // Suspende el hilo sin bloquearlo
                            bluetoothServices.bluetoothLeService?.listData?.clear()

                            delay(200)
                            Log.d("firmwarePASOS", "Despues de limpiar")
                            bluetoothServices.bluetoothLeService!!.sendFirstComando(
                                "4049" + Integer.toHexString(
                                    FinalFirmwareCommands.size
                                )
                            )
                            Log.d(
                                "firmwarePASOS",
                                "se mando el comando 4049  ${
                                    Integer.toHexString(
                                        FinalFirmwareCommands.size
                                    )
                                }"
                            )
                            delay(1000)
                            Log.d(
                                "firmwarePASOS", "se espera 800"
                            )
                            listData = getInfoList() as MutableList<String?>
                            Log.d(
                                "firmwarePASOS",
                                "listData ${getInfoList()} listData $listData"
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
                        } else {
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

    fun NewUpdatefirmware(
        FWWW: String,
        callback: MyCallback
    ) {
        var error = false
        var textError = ""
        var divFirmware = dividirNewFirmware(FWWW.replace(" ",""))

        val job = CoroutineScope(Dispatchers.IO).launch {

            val corrutina1 = launch {
                callback.onProgress("Iniciando")
                Log.d("firmwarePASOS", "Iniciando")

                listData!!.clear()


                Log.d(
                    "firmwarePASOS",
                    "firmware corrutina1 handshake listData $listData  infolist ${getInfoList()} "
                )

                if (GetStatusBle()) {
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
                    } else {
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
                Log.d(
                    "firmwarePASOS",
                    "Realizando GetStatusBle  ${GetStatusBle()} divFirmware $divFirmware"
                )
                if (GetStatusBle()) {
                    if (divFirmware) {
                        if (containsPair(listData!!, "F1", "03")) {
                            Log.d("firmwarePASOS", "2251")

                            bluetoothServices.bluetoothLeService!!.CleanListLogger()

                            delay(200)
                            Log.d("firmwarePASOS", "Despues de limpiar")
                            bluetoothServices.bluetoothLeService!!.sendFirstComando(
                                "4049" + Integer.toHexString(
                                    FinalFirmwareCommands.size
                                ).padStart(2,'0')
                            )
                            Log.d(
                                "firmwarePASOS",
                                "se mando el comando 4049  ${
                                    Integer.toHexString(
                                        FinalFirmwareCommands.size
                                    )
                                }"
                            )
                            delay(1000)
                            Log.d(
                                "firmwarePASOS", "se espera 800"
                            )
                            listData = getInfoList() as MutableList<String?>
                            Log.d(
                                "firmwarePASOS",
                                "listData ${getInfoList()} listData $listData"
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
                        } else {
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

    inner class MyAsyncTaskUpdateFIrmware123(private val  FWWW: String, private val CantidadPaq : Int,  private val callback: MyCallback) :
        AsyncTask<Int?, Int?, String?>() {
        var error = false
        var textError = ""
        var divFirmware = dividirNewFirmware(FWWW.replace(" ",""))
        var resultadoPaquetes = ""
        var tamPaquete = CantidadPaq // 240
        override fun doInBackground(vararg params: Int?): String? {

            FinalFirmwareCommands.map {
             //   Log.d("VALORESFINALCOMMANDS","$it")
            }

            if (GetStatusBle()) {
                if (!divFirmware) {
                    textError = "No se pudo dividir el firmware"
                    error = true
                }
                else{
                    clearLogger()
                    bluetoothLeService?.sendFirstComando("4021")
                    Thread.sleep(700)
                    listData.clear()
                    listData.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                    Log.d(
                        "firmwarePASOS",
                        "firmware Valor del hanshake  listData $listData "
                    )
                    /////////////////////////////////////////////////////////////////////////////////

                     resultadoPaquetes = EnviarPaquetesDatos(callback, tamPaquete)
                    when (resultadoPaquetes) {
                        "correct" -> {
                            callback.onSuccess(true)
                        }

                        "cancel", "cancelFialChecksumError" -> {
////                            callback.onSuccess(false)
//                            callback.onError(resultadoPaquetes)
                        }
                    }
                    ////////////////////////////////////////////////////////////////////////////////

                }
            } else {
                textError = "Dispositivo Desconectado"
            }

            return ""
        }

        override fun onPostExecute(result: String?) {

            if (!resultadoPaquetes.equals("correct"))
            {
                 callback.onSuccess(false)
                callback.onError(textError)
            }
            callback.onProgress("Finalizando")

        }


        override fun onPreExecute() {


            callback.onProgress("Iniciando")
        }

    }

    fun EnviarPaquetesDatos(callback: MyCallback, tamPaquete: Int) : String{
        clearLogger()
        bluetoothLeService?.sendFirstComando("4046")
        Thread.sleep(700)
        listData.clear()
        listData.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
        Log.d(
            "firmwarePASOS",
            "firmware Valor del 4046  listData $listData "
        )

        callback.onProgress("Realizando")

        Log.d(
            "firmwarePASOS",
            "Valida el resultado de 4046 listData $listData"
        )
        if(listData.isNotEmpty() ){

            val finalListResponse: MutableList<String> = GetRealDataFromHexaImbera.convert(
                listData as List<String>,
                "Actualizar a Firmware Personalizado",
                sp!!.getString("numversion", "")!!,
                sp!!.getString("modelo", "")!!
            ).toMutableList()

            if (finalListResponse[0].uppercase().equals("F103"))
            {
                val PaddedFirmwareCommands = adjustFirmwareCommands(FinalFirmwareCommands ,tamPaquete)
                Thread.sleep(500)
                FinalFirmwareCommands.clear()
                Thread.sleep(100)
                FinalFirmwareCommands = PaddedFirmwareCommands
                Log.d(
                    "firmwarePASOS",
                    "Valida el resultado de 4046 el resultado es F103 ${FinalFirmwareCommands.size}"
                )
                clearLogger()
                var tamBytestoSend = when(tamPaquete){
                    240 -> 2
                    384 -> 4
                    else -> 4
                }
                bluetoothServices.bluetoothLeService!!.sendFirstComando(
                    "4049" + Integer.toHexString(
                        FinalFirmwareCommands.size
                    ).padStart(tamBytestoSend,'0')
                )
                Log.d(
                    "firmwarePASOS",
                    "comando 4049 ${"4049" + Integer.toHexString(
                        FinalFirmwareCommands.size
                    ).padStart(tamBytestoSend,'0')}"
                )

                Thread.sleep((1000))
                listData.clear()
                listData.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                Log.d(
                    "firmwarePASOS",
                    "Valida el resultado de 4049:  el resultado es listData $listData"
                )
                val finalListResponse4049: MutableList<String> = GetRealDataFromHexaImbera.convert(
                    listData as List<String>,
                    "Actualizar a Firmware Personalizado",
                    sp!!.getString("numversion", "")!!,
                    sp!!.getString("modelo", "")!!
                ).toMutableList()


                var i = 0




                while (i < FinalFirmwareCommands.size) {
                  /*  Log.d(
                        "firmwarePASOS",
                        "Tama;o de paquete a enviar  FinalFirmwareCommands.size ${i +1} ${FinalFirmwareCommands.size} "
                    )*/

                    clearLogger()
                    var textotoSend: String =
                    if (FinalFirmwareCommands[i].length<256){
                        FinalFirmwareCommands[i].substring(0,FinalFirmwareCommands[i].length-8).padEnd(256,'0')+FinalFirmwareCommands[i].substring(FinalFirmwareCommands[i].length-8,FinalFirmwareCommands[i].length)
                    } else FinalFirmwareCommands[i]
                    bluetoothServices.bluetoothLeService!!.sendComando(
                        textotoSend,
                        ""
                    )
                    Log.d("VALORESFINALCOMMANDS"," $i ${textotoSend}")

                    callback.onProgress("Enviando el paquete ${ i +1}")
                    Thread.sleep((200))
                    listData.clear()
                    listData.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                    val EnvioDatos: MutableList<String> = GetRealDataFromHexaImbera.convert(
                        listData as List<String>,
                        "Actualizar a Firmware Personalizado",
                        sp!!.getString("numversion", "")!!,
                        sp!!.getString("modelo", "")!!
                    ).toMutableList()
                    Log.d(
                        "firmwarePASOS",
                        "Valida el resultado de EnvioDatos ${i+1}:  el resultado es EnvioDatos $EnvioDatos"
                    )
                    if (EnvioDatos.isNotEmpty()){
                        if (!EnvioDatos.first().trim().uppercase().equals("F13D"))
                        {
                            Thread.sleep(800)
                            listData.clear()
                            listData.add(bluetoothLeService!!.dataFromBroadcastUpdateString)
                            val EnvioDatos: MutableList<String> = GetRealDataFromHexaImbera.convert(
                                listData as List<String>,
                                "Actualizar a Firmware Personalizado",
                                sp!!.getString("numversion", "")!!,
                                sp!!.getString("modelo", "")!!
                            ).toMutableList()
                            Log.d(
                                "firmwarePASOS",
                                "Valida el resultado de EnvioDatos ${i+1}:  el resultado es EnvioDatos $EnvioDatos"
                            )
                            if (EnvioDatos.isNotEmpty()){
                                if (!EnvioDatos.first().trim().uppercase().equals("F13D"))
                                {
                                    return "cancel"
                                }
                            }

                        }
                    }

                    if (i + 1 == FinalFirmwareCommands.size) {

                        clearLogger()
                        listData.clear()
                        val finalChecksum = Integer.toHexString(checksumTotal).padStart(8, '0')
                        bluetoothServices.bluetoothLeService!!.sendComando("404A$finalChecksum")
                        Log.d("firmwarePASOS","Final de la lista ${i+ 1} ${FinalFirmwareCommands.size}  toSend -> $404A$finalChecksum ")
                        Thread.sleep((200))
                        listData.add(bluetoothLeService!!.dataFromBroadcastUpdateString)

                        val EnvioDatos: MutableList<String> = GetRealDataFromHexaImbera.convert(
                            listData as List<String>,
                            "Actualizar a Firmware Personalizado",
                            sp!!.getString("numversion", "")!!,
                            sp!!.getString("modelo", "")!!
                        ).toMutableList()
                        Log.d(
                            "firmwarePASOS",
                            "Valida el ultimo paquete ${i+1}:  el resultado es $EnvioDatos"
                        )
                        Thread.sleep(800)
                        callback.onProgress("404A$finalChecksum EnvioDatos $EnvioDatos")
                        if (EnvioDatos.isNotEmpty()){
                            if (!EnvioDatos.first().trim().uppercase().equals("F13D"))
                            {
                                return "cancelFialChecksumError"
                            }
                            else{
                                return  "correct"
                            }
                        }
                    }


                    i++
                }




            }

        }
        else{
            return  "No se pudo preparar el modo actualizacion de flash"
            Log.d(
                "firmwarePASOS",
                "No se pudo preparar el dispositivo para la actualizacion"
            )


        }
        return "error"
    }

        fun adjustFirmwareCommands(FinalFirmwareCommands:  MutableList<String>, tamPaquetes : Int):  MutableList<String> {
        val PaddedFirmwareCommands = mutableListOf<String>()
        val targetLength =  tamPaquetes // 240 //384
        val finalPacketLength = 264

        // Copiar los comandos originales
        for (i in FinalFirmwareCommands.indices) {
            var command = FinalFirmwareCommands[i]

            // Ajustar el último paquete si no tiene 264 caracteres
            if (i == FinalFirmwareCommands.lastIndex && command.length != finalPacketLength) {
                val prefix = command.substring(0, command.length - 8).padEnd(256, '0')
                val suffix = command.substring(command.length - 8, command.length)
                command = prefix + suffix
            }

            PaddedFirmwareCommands.add(command)
        }

        // Añadir paquetes de '0' si la longitud es menor a 240
        while (PaddedFirmwareCommands.size < targetLength) {
            PaddedFirmwareCommands.add("0".repeat(finalPacketLength))
        }

        return PaddedFirmwareCommands
    }
      fun UpdatefirmwareSimple(
        FWWW: String,
        callback: MyCallback
    ) {
        var error = false
        var textError = ""
        var divFirmware = dividirNewFirmware(FWWW.replace(" ",""))

        val job = CoroutineScope(Dispatchers.IO).launch {

            val corrutina1 = launch {
                callback.onProgress("Iniciando")
                Log.d("firmwarePASOS", "Iniciando")

                listData!!.clear()


                Log.d(
                    "firmwarePASOS",
                    "firmware corrutina1 handshake listData $listData  infolist ${getInfoList()} "
                )




                if (GetStatusBle()) {
                    if (divFirmware) {
                        bluetoothServices.bluetoothLeService!!.CleanListLogger()
                        delay(2000)
                        bluetoothServices.bluetoothLeService!!.sendComando("4047")
                        delay(800)

                    } else {
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
                Log.d(
                    "firmwarePASOS",
                    "Realizando GetStatusBle  ${GetStatusBle()} divFirmware $divFirmware"
                )
                if (GetStatusBle()) {
                    if (divFirmware) {
                        FinalFirmwareCommands.map {
                            Log.d("FinalFirmwareCommands","$it")
                        }
                        val result = executeFirmwareUpdateSimple(callback)
                        callback.onProgress("Se enviaron todos los paquetes con un tama;o de ${FinalFirmwareCommands.size}")
                        delay(500)
                        callback.onProgress("Finalizado")
                        callback.onSuccess(true)
                       /*
                        when (result) {
                            "correct" -> {
                                callback.onSuccess(true)
                            }

                            "cancel", "cancelFialChecksumError" -> {
                                callback.onSuccess(false)
                                callback.onError(result)
                            }
                        }
                        */
                        /*
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
                                "se mando el comando 4049  ${
                                    Integer.toHexString(
                                        FinalFirmwareCommands.size
                                    )
                                }"
                            )
                            delay(1000)
                            Log.d(
                                "firmwarePASOS", "se espera 800"
                            )
                            listData = getInfoList() as MutableList<String?>
                            Log.d(
                                "firmwarePASOS",
                                "listData ${getInfoList()} listData $listData"
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
                        } else {
                            Log.d(
                                "firmwarePASOS",
                                "No se pudo preparar el dispositivo para la actualizacion"
                            )
                            callback.onError("No se pudo preparar el modo actualizacion de flash")
                        }

                        */
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
                Log.d("ddddddddddddddddddddddddddddddddddddddddddddddddd","")
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
            bluetoothServices.bluetoothLeService!!.clearListLogger()

            bluetoothServices.bluetoothLeService?.listData?.clear()
            bluetoothServices.bluetoothLeService!!.sendComando(
                FinalFirmwareCommands[i],
                ""
            )
            delay(150)

            //  val listData = conexionTrefp.getInfoList()!!.trim().uppercase().replace(" ","") //     bluetoothLeService!!.listData.first().trim().uppercase().replace(" ","")//  conexionTrefp.getInfoList() //bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String>

            listData.clear()
            listData = bluetoothServices.bluetoothLeService!!.dataFromBroadcastUpdate!! as MutableList<String?>  // bluetoothServices.bluetoothLeService!!.getLogeer()!! as MutableList<String?>

                //getInfoList() as MutableList<String?>

            Log.d(
                "firmwarePASOS",
                "newListData $i ${listData.toString().trim().uppercase()}"
            )
            val Respuesta3D = GetRealDataFromHexaImbera.convert(
                listData as List<String>,
                "Actualizar a Firmware Personalizado",
                sp!!.getString("numversion", "")!!,
                sp!!.getString("modelo", "")!!
            ).toMutableList()

            if (listData.toString().trim().uppercase().equals("F13D"))
            {
                Log.d(
                    "firmwarePASOS",
                    "cancel 17635 listData $listData Respuesta3D $Respuesta3D")
                return "cancel"
            }
//            if (!containsPair(listData!!, "F1", "3D") || !containsPair(listData!!, "f1", "3d") ) {
//                Log.d(
//                    "firmwarePASOS",
//                    "cancel 17635 listData $listData")
//                return "cancel"
//            }

            if (i + 1 == FinalFirmwareCommands.size) {
                bluetoothServices.bluetoothLeService!!.clearListLogger()

                bluetoothServices.bluetoothLeService?.listData?.clear()
                val finalChecksum = Integer.toHexString(checksumTotal).padStart(8, '0')
                bluetoothServices.bluetoothLeService!!.sendComando("404A$finalChecksum")
                delay(150)

                val finalResponse =  bluetoothServices.bluetoothLeService!!.dataFromBroadcastUpdate //   .getLogeer()!!.toList() as MutableList<String>
                  //  bluetoothServices.bluetoothLeService!!.listData // conexionTrefp.getInfoList()


                //  bluetoothLeService!!.getDataFromBroadcastUpdate() as MutableList<String>
                val finalListResponse = GetRealDataFromHexaImbera.convert(
                    finalResponse as List<String>,
                    "Actualizar a Firmware Personalizado",
                    sp!!.getString("numversion", "")!!,
                    sp!!.getString("modelo", "")!!
                ).toMutableList()

                Log.d("firmwarePASOS","finalListResponse $finalListResponse")
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

    suspend fun executeFirmwareUpdateSimple(callback: MyCallback): String {
        var i = 0
        while (i < FinalFirmwareCommands.size) {

            callback.onProgress("enviando paquete $i")
            bluetoothServices.bluetoothLeService!!.clearListLogger()

            bluetoothServices.bluetoothLeService?.listData?.clear()
            bluetoothServices.bluetoothLeService!!.sendComando("4046${FinalFirmwareCommands[i]}"


            )
            delay(200)


            i++
        }

        return "correct"
    }
}



