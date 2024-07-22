package com.example.trepfp

import BluetoothServices.BluetoothServices
import RecyclerResultAdapter
import RecyclerResultItem
import Utility.BLEDevices
import Utility.GetHexFromRealDataCEOWF.getDecimal
import Utility.GetHexFromRealDataImbera
import Utility.GetRealDataFromHexaImbera
import Utility.GetRealDataFromHexaOxxoDisplay
import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.RestrictionsManager
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trepfp.databinding.TerminaleltecBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.eltec.BluetoothServices.BluetoothLeService
import mx.eltec.Utility.CustomProgressDialog
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Formatter
import java.util.Locale
import java.util.TimeZone

class TerminaleltecActivity : AppCompatActivity(),



    BluetoothLeService.BluetoothLeServiceListener {
        private val LOCATION_PERMISSION_REQUEST_CODE = 1

        private val PICK_FILE_REQUEST_CODE  = 999
        private val PICK_OPEN_FILE  = 888

        private lateinit var binding: TerminaleltecBinding
        var myContext: Context? = null
        var listaTemporalFINAL = mutableListOf<String>()
        var datalogger = mutableListOf<String>()
        var locationResultListener: ConexionTrefp.LocationResultListener? = null
        val data = listOf("00:E4:4C:20:A2:23", "00:E4:4C:00:92:E4", "00:E4:4C:20:A7:E4")
        val conexionTrefp2 by lazy { ConexionTrefp(this, null, null) }

        val bluetoothLeService by lazy { conexionTrefp2.bluetoothLeService }
        var sp: SharedPreferences? = null
        var esp: SharedPreferences.Editor? = null
        var myconnectListener: connectListener? = null
        private val progressDialog2 by lazy { CustomProgressDialog(this) }
        val workbook = XSSFWorkbook()
        private lateinit var mHandler: Handler
        private val customProgressDialog by lazy { this@TerminaleltecActivity?.let { CustomProgressDialog(this) } }
        val scope = CoroutineScope(Dispatchers.Main)
        var listData: MutableList<String?> = ArrayList()
        var FinalListData: MutableList<String?> = ArrayList()

        data class Datos(
            val timestamp: String,
            val temperature1p1: String,
            val temperature2p1: String,
            val voltage: String,
            val Original: String
            /*,
        val checksum: String*/

        )

        var bluetoothService: BluetoothServices? = null
        var valorHnds = mutableListOf<String>()

        private lateinit var saveFileLauncher: ActivityResultLauncher<String>

        private var isDisconnecting = false
        private lateinit var recyclerViewBLEList: RecyclerViewBLEList
        private lateinit var listaDevices: ArrayList<BLEDevices>
        private lateinit var mBluetoothAdapter: BluetoothAdapter

        private var mScanning: Boolean = false
//    private var mBluetoothAdapter: BluetoothAdapter? = null
//    var recyclerViewBLEList: RecyclerViewBLEList? = null


        private lateinit var recyclerResultAdapter: RecyclerResultAdapter
        private lateinit var recyclerResultList: ArrayList<RecyclerResultItem>

        // var bluetoothLeService  : BluetoothLeService? = null // by lazy { conexionTrefp2.bluetoothLeService }
        class RangeCheckListener : ConexionTrefp.OnRangeCheckListener {
            override fun onRangeChecked(isWithinRange: Boolean): Boolean {
                if (isWithinRange) {
                    // La ubicación está dentro del rango
                    Log.d("checkGooglePlayServices", "La ubicación está dentro del rango.")
                } else {
                    // La ubicación está fuera del rango
                    Log.d("checkGooglePlayServices", "La ubicación está fuera del rango.")
                }
                return isWithinRange
            }
        }

        private fun getVersionName(): String {
            try {
                val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
                return pInfo.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            return ""
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            //  setContentView(R.layout.activity_main)
            binding = TerminaleltecBinding.inflate(layoutInflater)
            val view = binding.root
            setContentView(view)
            FirebaseApp.initializeApp(this)
            this.myContext = this@TerminaleltecActivity
            askPermission()
            init()
            mHandler = Handler()
            recyclerResultList = ArrayList()
            recyclerResultAdapter = RecyclerResultAdapter(recyclerResultList)
            val adapter = ArrayAdapter(this, R.layout.simple_spinner_item, data)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            listaDevices = ArrayList()
            recyclerViewBLEList = RecyclerViewBLEList(listaDevices)
            binding.rvbleDevices.layoutManager = LinearLayoutManager(this)
            binding.rvbleDevices.adapter = recyclerViewBLEList





            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            mHandler = Handler()

            binding.Buscar.setOnClickListener {
                customProgressDialog!!.show()
                scanLeDevice(true)
                customProgressDialog!!.updateText("Escaneando")
                mHandler.postDelayed({
                    customProgressDialog!!.dismiss()
                    scanLeDevice(false)
                }, SCAN_PERIOD)

                recyclerViewBLEList.setOnClickListener { v ->
                    val position = binding.rvbleDevices.getChildAdapterPosition(v)
                    val device = listaDevices[position]


                    scope.launch {
                        var contador = 1
                        while (contador <= 1) {

                            customProgressDialog!!.show()

                            conexionTrefp2.MyAsyncTaskConnectBLE(device.mac,
                                device.nombre,
                                object : ConexionTrefp.MyCallback {
                                    override fun onSuccess(result: Boolean): Boolean {

                                        runOnUiThread {
                                            customProgressDialog!!.dismiss()
                                            Log.d(
                                                "ConexionCLASBLE",
                                                "onSuccess intento $contador resultado ${result.toString()} "
                                            )
                                            //       if (result) binding.T.text = "${sp!!.getString("mac", "")}"
                                            Thread.sleep(500)
                                            Log.d("ConexionCLASBLE", "data $ conexionTrefp2.isBLEGattConnected() ${ conexionTrefp2.isBLEGattConnected()}")
                                            mHandler.postDelayed({
                                                conexionTrefp2.isBLEGattConnected()
                                                Log.d("ConexionCLASBLE", "data $ conexionTrefp2.isBLEGattConnected() ${ conexionTrefp2.isBLEGattConnected()}")

                                                if (conexionTrefp2.isBLEGattConnected()){
                                                    runOnUiThread {
                                                        binding.Status.text = "Conectado"
                                                        binding!!.tvconnectionstate?.setTextColor(
                                                            Color.parseColor(
                                                                "#00a135"
                                                            )
                                                        )
                                                        binding!!.tvconnectionstate.text = "${
                                                            sp!!.getString(
                                                                "mac",
                                                                ""
                                                            )
                                                        }"
                                                    }
                                                }
                                                else{
                                                    runOnUiThread {
                                                        binding!!.Status.setText("Desconectado")
                                                        //binding!!.ivLockIcon.setVisibility(View.GONE)
                                                        binding!!.Status.setTextColor(Color.BLACK)
//                    binding!!.tvfwversion.text = ""
                                                        //conexionTrefp2.desconectar()
                                                        NopintarLockIcon(false)
                                                    }
                                                }

                                            }, 1000)
                                            //     conexionTrefp2.desconectar()
                                        }
                                        return result
                                    }

                                    override fun onError(error: String) {
                                        // manejar error
                                        runOnUiThread {
                                            binding.TextResultado.text = error
                                        }
                                        Log.d("ConexionCLASBLE", error.toString())
                                    }

                                    override fun getInfo(data: MutableList<String>?) {
                                        Log.d("ConexionCLASBLE", "data $ conexionTrefp2.isBLEGattConnected() ${ conexionTrefp2.isBLEGattConnected()}")
                                        mHandler.postDelayed({
                                        conexionTrefp2.isBLEGattConnected()


                                        }, 1000)
                                    }
                                    /* override fun getInfo(data: MutableList<String>?) {

                                     Log.d("ConexionCLASBLE", "getInfo ${data.toString()}")
                                     if (data != null) {
                                         postHandshake(data, device.nombre)
                                     }
                                 }
                                 */

                                    override fun onProgress(progress: String): String {

                                        when (progress) {
                                            "Iniciando" -> {

                                                runOnUiThread {
                                                    customProgressDialog!!.updateText("Conectando")
                                                    //binding.TextResultado.text = progress
                                                }
                                            }

                                            "Realizando" -> {
                                                runOnUiThread {
                                                    // binding.TextResultado.text = progress
                                                }
                                            }

                                            "Finalizando" -> {
                                                runOnUiThread {
                                                    //  binding.TextResultado.text = progress
                                                    customProgressDialog!!.dismiss()
                                                }
                                            }
                                        }
                                        Log.d("ConexionCLASBLE", "progress ${progress}")
                                        return progress
                                    }
                                }).execute()


                            delay(60000)
                            contador++
                        }
                    }

                }
            }
            binding.Desconectar.setOnClickListener({

                conexionTrefp2.desconectar()
                runOnUiThread {
                    binding!!.Status.setText("Desconectado")
                    binding.tvconnectionstate.text = ""
                    //binding!!.ivLockIcon.setVisibility(View.GONE)
                    binding!!.Status.setTextColor(Color.BLACK)
//                    binding!!.tvfwversion.text = ""
                    //conexionTrefp2.desconectar()
                    NopintarLockIcon(false)
                }


            })

            Log.d(
                "salidaCaract",
                "characteristicToHexString ${characteristicToHexString("[B@d429884")}"
            )



            binding.Room.setOnClickListener {
                // Crear una instancia de Device_info

                openFilePicker()


                /*  val deviceInfo = Device_info(
                id = UUID.randomUUID().toString(),
                nombre = "Mi Dispositivo",
                Handshake = mutableListOf("handshake1", "handshake2"),
                modelo_device = "Modelo X",
                firmware_device = "v1.0.0"
            )

            // Obtener la base de datos y el DAO
            val db = Room.databaseBuilder(
                this@MainActivity,
                AppDatabase::class.java, "Device_BD"
            ).build()

            val deviceInfoDao = db.deviceInfoDao()

            // Insertar en la base de datos usando coroutines
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    deviceInfoDao.insert(deviceInfo)
                    delay(5000)
                    deviceInfoDao.getAll().map { devices ->
                        Log.d("DatosRoom", "datos $devices")
                    }
                }
            }
            */
            }

            validaCONEXION()
            val BLE =
                conexionTrefp2.bluetoothLeService //  BluetoothService().bluetoothLeService // bluetoothService!!.bluetoothLeService
            conexionTrefp2.bluetoothServices.registerBluetoothServiceListener(this)
            // bluetoothService!!.registerBluetoothServiceListener(this)
        }

        private fun openFile(mineType: String = "*/*") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mineType
            }
            startActivityForResult(intent, PICK_OPEN_FILE)
        }

        private fun openFilePicker() {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "text/plain"
            }
            startActivityForResult(intent, PICK_FILE_REQUEST_CODE)
        }


        private fun obtenerDatosStatus(chs: String) {
            conexionTrefp2.ObtenerStatusVersion2(object : ConexionTrefp.MyCallback {
                override fun getInfo(data: MutableList<String>?) {
                    var FinalListData = convertMONI(
                        data as List<String>,
                        "Lectura de datos tipo Tiempo real"
                    )
                    Log.d(
                        "ObtenerStatus",
                        "FinalListData ---------------> $FinalListData \n data $data"
                    )
                    var dataListPlantilla = GetRealDataMONI(
                        FinalListData as List<String>,
                        "Lectura de datos tipo Tiempo real"
                    )
                    Log.d("ObtenerStatus", "GetRealDataMONI  ---------------> $dataListPlantilla ")

                    /*
                val s2 = GetRealDataFromHexaImbera.cleanSpace(dataListPlantilla!!)
                esp.putString("CurrentStatusCompleto", s2.toString())
*/
                    esp!!.putString("Temp1Estatus", dataListPlantilla?.get(0))
                    esp!!.putString("Temp2Estatus", dataListPlantilla?.get(1))
                    esp!!.putString("VoltajeEstatus", dataListPlantilla?.get(2))
                    esp!!.putString("ActuadoresEstatus", dataListPlantilla?.get(3))
                    if (dataListPlantilla!!.size == 5) esp!!.putString(
                        "AlarmasEstatus",
                        dataListPlantilla?.get(4)
                    )
                    esp!!.apply()
                }

                override fun onError(error: String) {
                    Log.d("obtenerDatosStatus", "error $error")
                }

                override fun onProgress(progress: String): String {
                    Log.d("obtenerDatosStatus", "progress $progress")
                    return progress
                }

                override fun onSuccess(result: Boolean): Boolean {
                    Log.d("obtenerDatosStatus", "result $result")
                    return result
                }

            }).execute()
        }

        fun GetRealDataMONI(data: List<String>, action: String?): MutableList<String>? {
            //USO SOLO DE LOS DATOS BUFFER IMPORTANTES PARA MOSTRARLOS EN PANTALLA, LAS POSICIONES RESTANTES (HEADER) SON CORRECTAS
            return when (action) {
                "Handshake" -> {
                    val newData: MutableList<String> = java.util.ArrayList()
                    if (data.isEmpty()) {
                        newData.add("nullHandshake")
                    } else {
                        newData.add(GetRealDataFromHexaImbera.hexToAscii(data[1]))
                        newData.add(GetRealDataFromHexaImbera.getSameData(data[2], action))
                        newData.add(GetRealDataFromHexaImbera.getSameData(data[3], "trefpversion"))
                        newData.add(
                            GetRealDataFromHexaImbera.getDecimalFloat(data[4]).toString()
                        ) // decimales con punto
                    }
                    newData
                }

                "newHandshake" -> {
                    val newData: MutableList<String> = java.util.ArrayList()
                    if (data.isEmpty()) {
                        newData.add("nullHandshake")
                    } else {
                        Log.d("listdatalolo", "modelo " + data[5] + " " + data[2])
                        newData.add(GetRealDataFromHexaImbera.hexToAscii(data[1]))
                        newData.add(GetRealDataFromHexaImbera.getSameData(data[2], action))
                        newData.add(GetRealDataFromHexaImbera.getSameData(data[3], "trefpversion"))
                        newData.add(
                            GetRealDataFromHexaImbera.getDecimalFloat(data[4]).toString()
                        ) // decimales con punto
                        newData.add(GetRealDataFromHexaImbera.hexToAscii(data[5] + data[2])) // modelo
                        newData.add(
                            GetRealDataFromHexaImbera.getDecimalFloat(data[6]).toString()
                        ) // hardware
                    }
                    newData
                }

                "Lectura de parámetros de operación" -> {
                    val newData: MutableList<String> = java.util.ArrayList()
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
                    val newData: MutableList<String> = java.util.ArrayList()
                    if (data.isEmpty()) {
                        //newData.add(getSameData(data.get(0),"trefpversion"));
                        newData.add("nullHandshake")
                    } else {
                        var numf = GetRealDataFromHexaImbera.getDecimalFloat(data[4])
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

                        //newData.add(String.valueOf(getDecimalFloat(data.get(4)) ));//temp2
                        //newData.add(String.valueOf(getDecimalFloat(data.get(5)) ));//temp1
                        newData.add(GetRealDataFromHexaImbera.getDecimal(data[6]).toString()) //voltage
                        getActuadorMONI(data[7])?.let { newData.add(it) }
                        getAlarmaMONI(data[8])?.let { newData.add(it) }
                        //newData.add(getSameData(data.get(9), "trefpversion")); // decimales con punto
                        //newData.add(hexToAscii(data.get(9)));
                    }
                    newData
                }

                "Lectura de datos tipo Tiempo" -> {
                    val newData: MutableList<String> = java.util.ArrayList()
                    val header: MutableList<String> = java.util.ArrayList()
                    //header
                    if (data.isEmpty()) {
                        //newData.add(getSameData(data.get(0),"trefpversion"));
                        newData.add("nullHandshake")
                    } else {
                        //header
                        header.add(GetRealDataFromHexaImbera.getSameData(data[0], "trefpversion"))
                        header.add(GetRealDataFromHexaImbera.getDecimal(data[1]).toString())
                        header.add(GetRealDataFromHexaImbera.getSameData(data[2], action))
                        header.add(GetRealDataFromHexaImbera.getSameData(data[3], action))

                        //buffer
                        var date: Date
                        var i = 4
                        val timeStampOriginal =
                            GetRealDataFromHexaImbera.getDecimal(data[data.size - 2].substring(0, 8))
                                .toLong() //getDecimal(data.get(data.size()-1).substring(0,8));
                        val unixTime = System.currentTimeMillis() / 1000
                        val diferencialTimeStamp = unixTime - timeStampOriginal
                        //612F6B42
                        //long f =
                        do {
                            if (i + 1 >= data.size) {
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
                                //newData.add(String.valueOf(getDecimalFloat(data.get(i).substring(8,12)) ));
                                //newData.add(String.valueOf(getDecimalFloat(data.get(i).substring(12,16)) ));
                                newData.add(
                                    GetRealDataFromHexaImbera.getDecimal(data[i].substring(16))
                                        .toString()
                                ) //decimales sin punto
                                i++

                                //25,349,176
                            }
                        } while (i < data.size)
                    }
                    Log.d("", "realdata:$header")
                    newData
                }

                "Lectura de datos tipo Evento" -> {
                    val newData: MutableList<String> = java.util.ArrayList()
                    //header
                    if (data.isEmpty()) {
                        //newData.add(getSameData(data.get(0),"trefpversion"));
                        newData.add("nullHandshake")
                    } else {
                        var date: Date
                        var date2: Date
                        var i = 4
                        val timeStampOriginal =
                            GetRealDataFromHexaImbera.getDecimal(data[data.size - 2].substring(8, 16))
                                .toLong()
                        //long timeStampOriginal2 = getDecimal(data.get(data.size()-2).substring(8,16));
                        val unixTime = System.currentTimeMillis() / 1000
                        val diferencialTimeStamp = unixTime - timeStampOriginal
                        //long diferencialTimeStamp2 =  unixTime - timeStampOriginal2  ;
                        do {
                            if (i + 1 >= data.size) {
                                break //i=data.size();//no interesa el checksum
                            } else {
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
                                    ) + diferencialTimeStamp
                                )
                                date2 = Date.from(instant2)
                                newData.add(date2.toString())
                                getEventTypeMONI(data[i].substring(16, 18))?.let {
                                    newData.add(it)
                                } //evento type
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

                                //newData.add(String.valueOf(getDecimalFloat(data.get(i).substring(18,22)) ));
                                //newData.add(String.valueOf(getDecimalFloat(data.get(i).substring(22,26)) ));
                                newData.add(
                                    GetRealDataFromHexaImbera.getDecimal(data[i].substring(26))
                                        .toString()
                                ) //decimales sin punto,voltaje
                                i++
                            }
                        } while (i < data.size)
                    }
                    newData
                }

                else -> {
                    java.util.ArrayList()
                }
            }
        }

        fun convertMONI(arrayLists: List<String?>, action: String?): List<String?>? {
            GetRealDataFromHexaImbera.arrayListInfo.clear()
            when (action) {
                "Handshake" -> {
                    if (!arrayLists.isEmpty()) {
                        val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists as List<String>)
                        //header
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(0, 4)) //head
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(4, 28)) //Mac
                        //data
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(28, 30)) //modelo trefpb
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(30, 34)) //version
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(34, 38)) //plantilla
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(38, 42)) //checklist
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(42)) //checksum
                    }
                }

                "newHandshake" -> {
                    if (!arrayLists.isEmpty()) {
                        val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists as List<String>)
                        //header
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(0, 4)) //head
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(4, 28)) //Mac
                        //data
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(28, 30)) //modelo trefpb
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(30, 34)) //version
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(34, 38)) //plantilla
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(38, 40)) //modelo
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(40, 42)) //hardware

                        //arrayListInfo.add(s.substring(38, 42));//checklist
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(42)) //checksum
                    }
                }

                "Lectura de parámetros de operación" -> {
                    var i = 18 //+2 para salta el AA
                    val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists as List<String>)

                    //header
                    if (!arrayLists.isEmpty()) {
                        GetRealDataFromHexaImbera.arrayListInfo.add(
                            s.substring(
                                0,
                                4
                            )
                        ) //software version
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(4, 12)) //buffer_size
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(12, 14)) //data_type
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(14, 16)) //data_size
                        do {
                            i = if (i == 22) { //saltar posiciones por parámetros que no se están usando
                                i + 4
                            } else if (i == 30) { //saltar posiciones por parámetros que no se están usando
                                i + 20
                            } else if (i == 62) {
                                i + 8
                            } else {
                                GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(i, i + 4))
                                i + 4
                            }
                        } while (i < 86)
                        //+2 para saltar 66
                        i = 148
                        do {
                            i =
                                when (i) {
                                    154 -> {
                                        i + 2
                                    }

                                    158 -> {
                                        i + 6
                                    }

                                    172 -> {
                                        i + 6
                                    }

                                    184 -> {
                                        i + 2
                                    }

                                    190 -> {
                                        i + 2
                                    }

                                    196 -> {
                                        i + 16
                                    }

                                    228 -> {
                                        i + 2
                                    }

                                    242 -> {
                                        i + 6
                                    }

                                    252 -> {
                                        i + 8
                                    }

                                    else -> {
                                        GetRealDataFromHexaImbera.arrayListInfo.add(
                                            s.substring(
                                                i,
                                                i + 2
                                            )
                                        )
                                        i + 2
                                    }
                                }
                        } while (i < s.length - 14)
                        GetRealDataFromHexaImbera.arrayListInfo.add(
                            s.substring(
                                s.length - 14,
                                s.length - 10
                            )
                        ) //dato final "Plantilla"
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(s.length - 8)) //checksum
                    }
                    return GetRealDataFromHexaImbera.arrayListInfo
                }

                "Lectura de datos tipo Tiempo real" -> {
                    if (!arrayLists.isEmpty()) {
                        val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists as List<String>)
                        //head
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(0, 4)) //head
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(4, 12)) //
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(12, 14)) //modelo trefpb
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(14, 16)) //version
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(16, 20)) //temp1
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(20, 24)) //temp2
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(24, 26)) //voltaje
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(26, 28)) //actuadores
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(28, 32)) //alarmas
                        //arrayListInfo.add(s.substring(28,32));//plantilla
                        //arrayListInfo.add(s.substring(34));//checksum
                    }
                }

                "Lectura de datos tipo Tiempo" -> {
                    if (!arrayLists.isEmpty()) {
                        val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists as List<String>)
                        //header
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(0, 4)) //head
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(4, 12)) //
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(12, 14)) //modelo trefpb
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(14, 16)) //version
                        //data
                        var i = 16
                        do {
                            if (i + 18 > s.length) {
                                GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(i)) //checksum
                                break
                            } else GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(i, i + 18))
                            i = i + 18
                        } while (i < s.length)
                        Log.d("", "crudo:" + GetRealDataFromHexaImbera.arrayListInfo)
                    }
                }

                "Lectura de datos tipo Evento" -> {
                    if (!arrayLists.isEmpty()) {
                        val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists as List<String>)
                        //header
                        Log.d("", "s:$s")
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(0, 4)) //head
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(4, 12)) //
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(12, 14)) //modelo trefpb
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(14, 16)) //version

                        //data
                        //data
                        var i = 16
                        while (i < s.length) {
                            if (i + 28 > s.length) {
                                GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(i)) //checksum
                                break
                            } else GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(i, i + 28))
                            i += 28
                        }
                        Log.d("", "LOGGdatatime:" + GetRealDataFromHexaImbera.arrayListInfo.size)
                    }
                }

                "Actualizar a Firmware Original" -> {
                    if (!arrayLists.isEmpty()) {
                        val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists as List<String>)
                        //header
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.toString()) //head
                    }
                }

                "Actualizar a Firmware Personalizado" -> {
                    if (!arrayLists.isEmpty()) {
                        val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists as List<String>)
                        //header
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.toString()) //head
                    }
                }
            }
            return GetRealDataFromHexaImbera.arrayListInfo
        }

        private fun getActuadorMONI(s: String): String? {
            val ss = GetRealDataFromHexaImbera.HexToBinary(s)
            val stringBuilder = StringBuilder()
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

        private fun getAlarmaMONI(s: String): String? {
            val ss = GetRealDataFromHexaImbera.HexToBinary(s)
            var isSensorCrash = false
            val stb = java.lang.StringBuilder()
            var c: String
            for (i in 0..7) {
                c = ss.substring(i, i + 1)
                if (c == "1") {
                    when (i) {
                        7 ->                         //stb.append("Falla sensor ambiente en corto\n");
                            if (isSensorCrash) {
                                stb.append("Falla sensor\n")
                                //stb.append("Falla sensor\n");
                            } else {
                            }

                        6 ->                         //stb.append("Falla sensor ambiente en abierto\n");
                            if (isSensorCrash) {
                                //stb.append("Falla sensor externo\n");
                                stb.append("Falla sensor\n")
                            } else {
                            }

                        5 ->                         //stb.append("Falla sensor evaporador en corto\n");
                            //stb.append("Falla sensor interno en corto\n");
                            isSensorCrash = true

                        4 ->                         //stb.append("Falla sensor evaporador en abierto\n");
                            isSensorCrash = true

                        3 -> stb.append("Falla de puerta\n")
                        2 -> stb.append("Reservada\n")
                        1 -> {}
                        0 -> {}
                    }
                }
            }
            stb.append(".")
            return stb.toString()
        }

        private fun getEventTypeMONI(s: String): String? {
            var evento = ""
            var c: String
            when (s) {
                "04" -> evento = "Falla de energía"
                "03" -> evento = "Ciclo de deshielo"
                "02" -> evento = "Ciclo de compresor"
                "01" -> evento = "Apertura de puerta"
            }
            return evento
        }

        private fun NopintarLockIcon(deci: Boolean) {

/*
            if (deci) binding!!.ivlockIconConnection.visibility =
                View.VISIBLE else binding!!.ivlockIconConnection.visibility =
                View.GONE
            */
            // ivLockIcon.setVisibility(View.GONE);
        }


        /*
        fun MyAsyncTaskSendWifiPrincipal(ssid : String, pass : String,callback: ConexionTrefp.MyCallback){
            SendWifi(ssid,pass, "BLE_WIFI_NEW_SSID=", callback)
        }
        fun MyAsyncTaskSendWifiSecundario(ssid : String, pass : String,callback: ConexionTrefp.MyCallback){
            SendWifi(ssid,pass, "BLE_WIFI_NEW_SSID_2=", callback)
        }
        fun SendWifi(ssid : String, pass : String, comando : String, callback: ConexionTrefp.MyCallback){
            val job = CoroutineScope(Dispatchers.IO).launch {
                var bluetoothLeService = conexionTrefp2.bluetoothLeService
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
                            Thread.sleep(500)
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
                        }
                    } catch (e: InterruptedException) {
                        throw RuntimeException(e)
                        callback.onError(e.toString())
                        callback.onSuccess(false)
                    }
                }
                corrutina1.join()
                val corrutina2 = launch{
                    callback.onProgress("Finalizando")
                }
                corrutina2.join()

            }


            runBlocking {
                job.join()
            }
        }

        fun SendCommandWifi(toSendb: ByteArray, command : String) {
            var toSendb = toSendb


            Log.d(
                "MyAsyncTaskGetFirmwareBd",
                " second command $command   toSendb $toSendb   "
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
                              //  makeToast("Conéctate a un BLE")
                            }
                        } else {
                            runOnUiThread {
                             //   makeToast("Escritura correcta")
                            }
                        }

                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }

            }
        }



        fun getInfoWifi(callback: ConexionTrefp.MyCallback){
            val job = CoroutineScope(Dispatchers.IO).launch {
                var bluetoothLeService = conexionTrefp2.bluetoothLeService
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


                    Log.d("wifiCommandStatus", ":$command toSendb $toSendb ${conexionTrefp2.getStatusConnectBle()}")
                //    callback.onProgress("Finalizando")


                    if ( conexionTrefp2.getStatusConnectBle())
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
    */
        private fun VEvent(arrayLists: MutableList<String?>): MutableList<String?> {
            var arrayListInfo: MutableList<String?> = ArrayList()
            var arrayListInfoFG: MutableList<String?> = ArrayList()
            val s = GetRealDataFromHexaOxxoDisplay.cleanSpace(arrayLists)
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

        fun convertHexToHumanDateNew(hexTimestamp: String): String? {
            val unixTimestamp = hexTimestamp.toLong(16)
            val date = Date(unixTimestamp * 1000)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("es", "MX"))
            dateFormat.timeZone =
                TimeZone.getTimeZone("America/Mexico_City") // Utiliza la zona horaria de México
            return dateFormat.format(date)
        }

        fun characteristicToHexString(characteristic: String): String {

            val inputString = String
            val byteArray = inputString.toString().toByteArray(Charsets.UTF_32LE)

            Log.d("salidaCaract", characteristic + "   " + byteArray.toString())
            val formatter = Formatter()
            for (b in byteArray) {
                formatter.format("%02x", b)
            }
            return formatter.toString()
        }









        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)



            if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                data?.data?.let { uri ->
                    readTextFile(uri)
                }
            }

        }
        private fun readTextFile(uri: Uri) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val text = inputStream?.bufferedReader().use { it?.readText() }
                Log.d("FileContent", text ?: "Archivo vacío")

                if (text!!.isNotEmpty()){

                    // conexionTrefp2.getInfoWifi(object : ConexionTrefp.MyCallback {
                    /*
                    conexionTrefp2.UpdatefirmwareSimple(
                        text,
                        object : ConexionTrefp.MyCallback {
                            override fun onSuccess(result: Boolean): Boolean {
                                Log.d(
                                    "MyAsyncTaskUpdateFirmware", "onSuccess " +
                                            result.toString()
                                )
                                runOnUiThread {
                                    conexionTrefp2.makeToast("Proceso finalizado")
                                }
                                if (result) {

                                }
                                return result
                            }

                            override fun onError(error: String) {
                                Log.d(
                                    "MyAsyncTaskUpdateFirmware", "onError " +
                                            error
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


                                        runOnUiThread {
                                            progressDialog2.show()
                                            progressDialog2.updateText(
                                                "Actualizando versión " + sp?.getString(
                                                    "numversion",
                                                    ""
                                                ) + " \n a versión "
                                            )
                                        }
                                    }

                                    "Realizando" -> {

//                                                if (progressDialog2.returnprogressDialog2() != null && progressDialog2.returnISshowing()) {
//                                                    // progressDialog.dismiss()
//                                                    progressDialog2.dismiss()
//                                                }
                                    }

                                    "Finalizando" -> {
                                        runOnUiThread {
                                            progressDialog2.dismiss()
                                        }

                                    }
                                }



                                runOnUiThread {

                                    progressDialog2.updateText(
                                        "$progress"
                                    )
                                }
                                return progress
                            }
                        })
                    */
                }
                // Aquí puedes manejar el contenido del archivo como desees, por ejemplo, mostrarlo en un TextView
                ///   findViewById<TextView>(R.id.fileContentTextView).text = text ?: "Archivo vacío"
            //    binding.TextResultado.text = text ?: "Archivo vacío"
            } catch (e: Exception) {
                Log.e("FileError", "Error al leer el archivo", e)
            }
        }



        fun validaCONEXION() {
            lifecycleScope.launch {
                while (isActive) {
                    val connectionValid = conexionTrefp2?.ValidaConexion()
                    if (connectionValid == true) {
                        // Realiza alguna operación cuando la conexión es válida
                        println("La conexión es válida.")
                        //     binding.T.setTextColor(Color.parseColor("#308446"))

                    } else {
//                    binding.T.setTextColor(Color.RED)
//                    binding.T.text = "desconectado"
                        //  binding.textView.text = "Resultado"
                    }
                    delay(5000L) // Espera 5 segundos antes de la próxima validación
                }
            }
        }

        @SuppressLint("MissingPermission")
        private fun scanLeDevice(enable: Boolean) {
            if (enable) {
                listaDevices.clear()
                mScanning = true
                mBluetoothAdapter.startLeScan(mLeScanCallback)
            } else {
                mScanning = false
                mBluetoothAdapter.stopLeScan(mLeScanCallback)
            }
        }

        @SuppressLint("MissingPermission")
        private val mLeScanCallback = BluetoothAdapter.LeScanCallback { device, _, _ ->
            runOnUiThread {
                if (device.name != null) {
                    Log.d("ConexionBLE", "dispositivo -> ${device.name} ${device.address}")
                }
                // Filtro de dispositivos TREFP
                if (device.name != null && (
                            device.name.contains("IMBERA-TREFP") ||
                                    device.name.contains("CEB_IN") ||
                                    device.name.contains("IMBERA-WF") ||
                                    device.name.contains("IMBERA_RUTA_FRIA")
                                    ||
                                    device.name.contains("CEB_BA")
                                    ||
                                    device.name.contains("CEO_CONCO")
                                    ||
                                    device.name.contains("IMBERA-HEALTH")
                                    ||
                                    device.name.contains("OXXO-CMO")
                                    ||
                                    device.name.contains("OXXO-MONI")
                                    ||
                                    device.name.contains("TRFP-FULL")
                                    ||  device.name.isNotEmpty()

                            ) ///&& device.address != "00:E4:4C:00:92:5F" && device.address != "00:E4:4C:21:76:9C"
                ) {
                    // Agregar dispositivo a la lista
                    if (!listaDevices.any { it.mac == device.address }) {
                        listaDevices.add(BLEDevices(device.name, device.address, null))
                        recyclerViewBLEList.notifyDataSetChanged()
                    }
                }
            }
        }

        private fun checkGooglePlayServicesGEO(callback: (latitude: Double, longitude: Double) -> Unit) {
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this@TerminaleltecActivity)

            if (resultCode == ConnectionResult.SUCCESS) {
                // getDeviceLocation()
                getDeviceLocation { latitude, longitude ->
                    callback(latitude, longitude)
                    Log.d("obtenerGeo", "->>>>> Latitude: $latitude, Longitude: $longitude")
                }

            } else {
                Log.e("MainActivity", "Google Play Services not available")
            }
        }


        fun getDeviceLocation(callback: (latitude: Double, longitude: Double) -> Unit) {
            val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

            try {
                val locationResult = fusedLocationProviderClient.lastLocation

                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful && task.result != null) {
                        val location = task.result
                        val latitude = location.latitude
                        val longitude = location.longitude

                        callback(latitude, longitude)
                    } else {
                        Log.e("MainActivity", "Could not get device location")
                    }
                }
            } catch (e: SecurityException) {
                Log.e("MainActivity", e.message ?: "Security Exception")
            }
        }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)

            when (requestCode) {
                LOCATION_PERMISSION_REQUEST_CODE -> {
                    if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                        checkGooglePlayServicesGEO { latitude, longitude ->

                            Log.d("obtenerGeo", "->>>>> Latitude: $latitude, Longitude: $longitude")
                        }

                    } else {
                        Log.e("MainActivity", "Location permission denied")
                    }
                    return
                }

                else -> {
                    // Ignore all other requests.
                }
            }
        }



        fun MakeToast(Text: String) {
            Toast.makeText(
                this,
                Text,
                Toast.LENGTH_LONG
            ).show()
        }






        fun convertHexToHumanDate(hexTimestamp: String): String? {
            val unixTimestamp = hexTimestamp.toLong(16)
            val date = Date(unixTimestamp * 1000)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("es", "MX"))
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            return dateFormat.format(date)
        }

        fun calculateTimeDifferences2(lists: MutableList<List<String>?>) {

            val listFINAL = lists.lastOrNull()
            var previousTimestamp: Long = 0
            for (i in lists.size - 1 downTo 0) {
                val list = lists[i]?.toMutableList()

                // Obtener el último registro de la lista
                val lastRecord = list?.lastOrNull() ?: continue

                // Obtener los primeros 8 caracteres del último registro
                val timestampString = lastRecord.substring(0, 8)
                val e = convertirUnixHexAFecha(timestampString)
                val fechaHora1 = Date(timestampString.toLong(16) * 1000)
                Log.d("calculateTimeDifferences2", "$timestampString  $i    e $fechaHora1  $listFINAL")


            }
        }


        fun calculateTimeDifferencesPREFINAL(lists: MutableList<List<String>?>) {
            var previousTimestamp: Long = 0
            val listFINAL = lists.lastOrNull()
            // Iterar sobre cada lista en orden inverso


            for (i in lists.size - 1 downTo 0) {
                val list = lists[i]?.toMutableList()


                // Iterar sobre los registros de la lista en orden inverso
                for (j in list!!.size - 1 downTo 1) {
                    val currentRecord = list[j]
                    val previousRecord = list[j - 1]


                    // Obtener los primeros 8 caracteres de cada registro
                    val currentTimestampString = currentRecord.substring(0, 8)
                    val previousTimestampString = previousRecord.substring(0, 8)

                    // Convertir los timestamps hexadecimales a Unix timestamps en milisegundos
                    val currentTimestamp = java.lang.Long.parseLong(currentTimestampString, 16) * 1000
                    val previousTimestamp = java.lang.Long.parseLong(previousTimestampString, 16) * 1000

                    // Calcular la diferencia entre los timestamps
                    val timestampDifference = (previousTimestamp - currentTimestamp) / 1000

                    // Restar la diferencia al registro anterior
                    val previousTimestampMinusDifference =
                        previousTimestamp - timestampDifference * 1000

                    // Reemplazar los primeros 8 caracteres del registro anterior con el timestamp convertido
                    val previousTimestampMinusDifferenceString =
                        java.lang.Long.toHexString(previousTimestampMinusDifference / 1000)
                            .toUpperCase()
                    val newPreviousRecord =
                        previousTimestampMinusDifferenceString + previousRecord.substring(8)

                    list[j - 1] = newPreviousRecord
                }
            }

        }

        fun calculateTimeDifferencesPREFINAL2(lists: MutableList<List<String>?>) {
            var previousTimestamp: Long = 0
            val listFINAL = lists.lastOrNull()

            for (i in lists.size - 1 downTo 0) {
                val list = lists[i]?.toMutableList()

                for (j in list!!.size - 1 downTo 1) {
                    val currentRecord = list[j]
                    val previousRecord = list[j - 1]

                    // Obtener el timestamp del registro actual
                    val currentTimestampString = currentRecord.substring(0, 14)
                    val currentDateFormat = SimpleDateFormat("yyyyMMddHHmmss")
                    val currentDate = currentDateFormat.parse(currentTimestampString)
                    val currentCalendar = Calendar.getInstance()
                    currentCalendar.time = currentDate

                    // Obtener el timestamp del registro anterior
                    val previousTimestampString = previousRecord.substring(0, 14)
                    val previousDateFormat = SimpleDateFormat("yyyyMMddHHmmss")
                    val previousDate = previousDateFormat.parse(previousTimestampString)
                    val previousCalendar = Calendar.getInstance()
                    previousCalendar.time = previousDate

                    // Restar una hora al registro anterior
                    previousCalendar.add(Calendar.HOUR_OF_DAY, -1)

                    // Obtener el nuevo timestamp del registro anterior
                    val previousTimestampMinusDifference = previousCalendar.timeInMillis

                    // Reemplazar el timestamp del registro anterior
                    val previousTimestampMinusDifferenceString =
                        previousDateFormat.format(previousTimestampMinusDifference)
                    val newPreviousRecord =
                        previousTimestampMinusDifferenceString + previousRecord.substring(14)

                    list[j - 1] = newPreviousRecord
                }
            }
        }

        fun convertirUnixHexAFecha(hexTimestamp: String): String {
            val unixTimestamp = hexTimestamp.toLong(16)
            val fecha = Date(unixTimestamp * 1000L)
            val sdf = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy HH:mm:ss z", Locale.getDefault())
            sdf.timeZone = TimeZone.getDefault()
            return sdf.format(fecha)
        }


        fun dataFECHAFINAL(data: MutableList<List<String>?>, action: String): MutableList<String?> {
            val newData: MutableList<String?> = ArrayList()
            return when (action) {

                "Lectura de datos tipo Tiempo" -> {

                    //buffer
                    var date: Date
                    var i = 4
                    val timeStampOriginal = GetRealDataFromHexaImbera.getDecimal(
                        data[data.size - 1]!![0].substring(
                            0,
                            8
                        )
                    ).toLong()
                    val unixTime = System.currentTimeMillis() / 1000
                    val diferencialTimeStamp = unixTime - timeStampOriginal
                    do {
                        val instant = Instant.ofEpochSecond(
                            GetRealDataFromHexaImbera.getDecimal(
                                data[i]!![0].substring(0, 8)
                            ) + diferencialTimeStamp
                        )
                        date = Date.from(instant)
                        val fechaHoraExadecimal =
                            BigInteger.valueOf(date.time / 1000).toString(16).padStart(8, '0')
                        val resul =
                            replaceFirstEightChars(fechaHoraExadecimal, data[i]!![0], 8).uppercase()

                        newData.add(resul)

                        i++
                    } while (i < data.size)

                    newData
                }

                else -> {
                    ArrayList()
                }
            }
        }

        fun replaceFirstEightChars(str1: String, str2: String, indice: Int): String {
            val newStr1 = str1.padStart(8, '0')
            return newStr1 + str2.substring(indice)
        }


        private fun init() {

            mHandler = Handler()
            val myRestrictionsMgr =
                this.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
            val appRestrictions = myRestrictionsMgr.applicationRestrictions
            sp = getSharedPreferences("connection_preferences", Context.MODE_PRIVATE)
            esp = sp!!.edit()

            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val decorView = window.decorView
            decorView.systemUiVisibility =
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.navigationBarColor = Color.parseColor("#f4f4f4")
        }
        if (!sp!!.getBoolean("permissionGiven", false)) */


            //fragmentManager = supportFragmentManager

        }


        private fun askPermission() {
            // Grant read permission to the URI
            // Grant read permission to the URI
            val uriToAccess =
                "content://com.example.trepfp.MainActivity.provider/external_files/..." // Replace with the actual URI

            this.grantUriPermission(
                "com.example.otherapp",
                Uri.parse(uriToAccess),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            if (Build.VERSION.SDK_INT >= 31) {
                val perms = arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                if (ContextCompat.checkSelfPermission(
                        this@TerminaleltecActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                        this@TerminaleltecActivity, Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                        this@TerminaleltecActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(
                        this@TerminaleltecActivity, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(
                        this@TerminaleltecActivity, Manifest.permission.MANAGE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED

                    && ContextCompat.checkSelfPermission(
                        this@TerminaleltecActivity, Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    esp!!.putBoolean("permissionGiven", true)
                    esp!!.apply()
                } else {
                    requestPermissions(perms, 100)
                }
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    1666
                )
                val locationManager =
                    this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                var locationEnabled = false
                try {
                    locationEnabled =
                        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                } catch (ignored: Exception) {
                }
                try {
                    locationEnabled =
                        locationEnabled or locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                } catch (ignored: Exception) {
                }
                if (!locationEnabled) Log.d(
                    "PERMISSION LOCATION ENABLED",
                    "CORRECTO"
                ) else Log.d("PERMISSION LOCATION ENABLED", "INCORRECTO")
            }
        }


        interface connectListener {
            fun connectBLE(name: String?, mac: String?)
            fun disconnectBLE()
            val isPermissionEnabled: Boolean

            fun requestPemission()
        }

        fun setconnectListenerListener(callback: connectListener?) {
            myconnectListener = callback
        }

        override fun getApplicationContext(): Context {
            return super.getApplicationContext()
        }




        fun dataFECHA(
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

                else -> {
                    ArrayList()
                }
            }
        }





        fun convertirHexAFecha(hex: String): String {
            val unixTime = hex.toLong(16)
            val fecha = java.util.Date(unixTime * 1000)
            val sdf = java.text.SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy HH:mm:ss z")
            return sdf.format(fecha)
        }

        fun convertirHexAFechaCORTA(hex: String): String {
            val unixTime = hex.toLong(16)
            val fecha = Date(unixTime * 1000)
            val sdf = SimpleDateFormat("d 'de' MMMM 'de' yyyy HH:mm:ss", Locale.getDefault())
            return sdf.format(fecha)
        }

        companion object {
            // Se escanea 1 segundo en total
            private const val SCAN_PERIOD: Long = 1000
            private const val HandlerTIME: Long = 3000
            private const val HandlerDialogs: Long = 200
            private const val REQUEST_SAVE_EXCEL_FILE = 1
            private const val REQUEST_SAVE_EXCEL_FILE2 = 1

        }

        override fun onBluetoothDeviceConnected(): Boolean {
            runOnUiThread {
                binding!!.tvconnectionstate?.setTextColor(
                    Color.parseColor(
                        "#00a135"
                    )
                )
                binding.Status.text = "Conectado"
                binding!!.tvconnectionstate.text = "${
                    sp!!.getString(
                        "mac",
                        ""
                    )
                }"
            }
            return true
        }

        override fun onBluetoothDeviceDisconnected(): Boolean {
            if (!isDisconnecting) {
//            esp!!.putString("trefpVersionName", "")
//            esp!!.apply()
                runOnUiThread {
                    binding!!.Status.setText("Desconectado")
                    //binding!!.ivLockIcon.setVisibility(View.GONE)
                    binding.tvconnectionstate.text = ""
                    binding!!.Status.setTextColor(Color.BLACK)
//                    binding!!.tvfwversion.text = ""
                    //conexionTrefp2.desconectar()
                    NopintarLockIcon(false)
                }

            }
            return true
        }


    }


