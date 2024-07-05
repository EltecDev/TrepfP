package com.example.trepfp


import AppDatabase
import BluetoothServices.BluetoothServices
import Device_info
import RecyclerResultAdapter
import RecyclerResultItem
import Utility.BLEDevices
import Utility.GetHexFromRealDataCEOWF.getDecimal
import Utility.GetHexFromRealDataImbera
import Utility.GetRealDataFromHexaImbera
import Utility.GetRealDataFromHexaOxxoDisplay
import Utility.GlobalTools
import Utility.MakeProgressBar
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
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.trepfp.databinding.ActivityMainBinding
import com.example.trepfp.ui.ConexionNewBle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mx.eltec.BluetoothServices.BluetoothLeService
import mx.eltec.Utility.CustomProgressDialog
import mx.eltec.imberatrefp.BuildConfig
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
import java.util.*
import com.example123.trepfp.ConexionTrefp
import com.example123.trepfp.ConexionTrefp.CallbackLoggerVersionCrudo
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.BufferedReader

class MainActivity : AppCompatActivity(), ConexionTrefp.MyCallback,


   BluetoothLeService.BluetoothLeServiceListener {
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private val PICK_FILE_REQUEST_CODE  = 999
    private val PICK_OPEN_FILE  = 888

    private lateinit var binding: ActivityMainBinding
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
    private val customProgressDialog by lazy { this@MainActivity?.let { CustomProgressDialog(this) } }
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
    private lateinit var conexionNewBle: ConexionNewBle
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        FirebaseApp.initializeApp(this)
        this.myContext = this@MainActivity
        askPermission()
        init()
        mHandler = Handler()
        recyclerResultList = ArrayList()
        recyclerResultAdapter = RecyclerResultAdapter(recyclerResultList)
//        binding.ReciclerResult.layoutManager = LinearLayoutManager(this)
//        binding.ReciclerResult.adapter = recyclerResultAdapter

        val adapter = ArrayAdapter(this, R.layout.simple_spinner_item, data)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        listaDevices = ArrayList()
        recyclerViewBLEList = RecyclerViewBLEList(listaDevices)
        binding.rvbleDevices.layoutManager = LinearLayoutManager(this)
        binding.rvbleDevices.adapter = recyclerViewBLEList


        binding.TextUsuario.setText("ELTEC_apps")
        binding.TextPassword.setText("975318642a")//  textPass.setText("975318642a")


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mHandler = Handler()
        // Establece el adaptador y carga los datos en el Spinner
//        binding.spinnerMAC.adapter = adapter
//        // binding.spinner.adapter = adapter
//        binding.spinnerMAC.selectedItem.toString()
        //conexionTrefp(applicationContext)

//        bluetoothLeService  =   conexionTrefp2.bluetoothLeService
        //   bluetoothServices = BluetoothServices(this)

        //   binding.textViewVersion.text = "Version de prueba: ${BuildConfig.VERSION_NAME}"
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

                //  Log.d("PruebaFuncionConexion"," onSuccess ${device.mac} ${device.nombre} ")

                scope.launch {
                    var contador = 1
                    while (contador <= 1) {

                        customProgressDialog!!.show()
                        //       conexionTrefp2.bluetoothLeService!!.connect(device.mac)

                      //  conexionNewBle = ConexionNewBle(this@MainActivity, device.mac)

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
                                    Log.d("ConexionCLASBLE", "data $data")
                                    mHandler.postDelayed({
                                        if (data != null) {
                                            if (data.isNotEmpty()) {
                                                valorHnds = data
                                                var listData = data
                                                var listDataFW = data
                                                var FinalListData = mutableListOf<String>()
                                                if (!listData.isEmpty()) {

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
                                                    } \n${sp!!.getString("name", "")}"
                                                    FinalListData =
                                                        GetRealDataFromHexaImbera.convertVersion2(
                                                            listData as MutableList<String>,
                                                            "Handshake",
                                                            "",
                                                            ""
                                                        ) as MutableList<String>
                                                    listData =
                                                        GetRealDataFromHexaImbera.GetRealDataVersion2(
                                                            FinalListData as List<String>,
                                                            "Handshake",
                                                            "",
                                                            ""
                                                        ) as MutableList<String>

                                                    Log.d(
                                                        "Lista BLE Fragment",
                                                        "onPostExecute:$FinalListData"
                                                    )
                                                    Log.d(
                                                        "Lista BLE Fragment",
                                                        "onPostExecute:$listData"
                                                    )
                                                    Log.d(
                                                        "Lista BLE Fragment",
                                                        "onPostExecute:" + listData.size
                                                    )
                                                    if (listData.size == 4) {
                                                        //viejo handshake
                                                        esp!!.putString("modelo", listData[1])
                                                        esp!!.putString("numversion", listData[2])
                                                        esp!!.putString(
                                                            "plantillaVersion",
                                                            listData[3]
                                                        )
                                                        esp!!.putString("HWVersion", "")
                                                        esp!!.putString(
                                                            "trefpVersionName",
                                                            device.nombre
                                                        )
                                                        esp!!.apply()
                                                    } else {
                                                        //nuevo handshake
                                                        esp!!.putString("modelo", listData[1])
                                                        esp!!.putString("numversion", listData[2])
                                                        esp!!.putString(
                                                            "plantillaVersion",
                                                            listData[3]
                                                        )
                                                        esp!!.putString("HWVersion", listData[4])
                                                        esp!!.putString(
                                                            "trefpVersionName",
                                                            device.nombre
                                                        )
                                                        esp!!.apply()
                                                    }
                                                    if (true /*sp.getString("name","").equals("OXXO-CMO") && listData.get(2).equals("1.01")*/) {

                                                        Log.d(
                                                            "listdatalolo",
                                                            "GetRealDataFromHexaImbera :$listData"
                                                        )


                                                        binding!!.tvfwversion.text =
                                                            ("Modelo:" + sp?.getString("modelo", "")
                                                                    + "\nVersión HW:" + sp!!.getString(
                                                                "HWVersion",
                                                                ""
                                                            )
                                                                    + "\nVersión FW:" + sp!!.getString(
                                                                "numversion",
                                                                ""
                                                            )
                                                                    + "\nPlantilla:" + sp!!.getString(
                                                                "plantillaVersion",
                                                                ""
                                                            ))
                                                        /* if (listData!!.size >= 5) {
                                                             runOnUiThread {
                                                                 binding!!.tvfwversion.text =
                                                                     ("Modelo:" + listData.get(4)
                                                                             + "\nVersión HW:" + listData.get(5)
                                                                             + "\nVersión FW:" + listData.get(2)
                                                                             + "\nPlantilla:" + listData.get(3))
                                                             }

                                                         }*/
                                                        /*
                                                        */
                                                    } else {
                                                        Log.d(
                                                            "listdatalolo",
                                                            "GetRealDataFromHexaImbera :$listData"
                                                        )
                                                        runOnUiThread {
                                                            binding!!.tvfwversion.setText(
                                                                ("Modelo:" + listData?.get(1)
                                                                        + "\nVersión:" + listData?.get(
                                                                    2
                                                                )
                                                                        + "\nPlantilla:" + listData?.get(
                                                                    3
                                                                ))
                                                            )
                                                        }
                                                    }

                                                    mHandler.postDelayed({
                                                        //ObtenerPlantilla()
                                                        Log.d(
                                                            "claseconexion",
                                                            "llave ${conexionTrefp2.returnLlaveConecct()}"
                                                        )
                                                        // Llamar a NopintarLockIcon(true) si returnLlaveConecct() devuelve true, de lo contrario llamar a NopintarLockIcon(false)
                                                        if (conexionTrefp2.returnLlaveConecct()) NopintarLockIcon(
                                                            true
                                                        ) else NopintarLockIcon(
                                                            false
                                                        )

                                                    }, 2000)


                                                    /*                         if (true /*sp.getString("name","").equals("OXXO-CMO") && listData.get(2).equals("1.01")*/) {

                                                                                 Log.d("listdatalolo", "GetRealDataFromHexaImbera :$listData")


                                                                                 if (listData!!.size >= 5) {
                                                                                     runOnUiThread {
                                                                                         binding!!.tvfwversion.text =
                                                                                             ("Modelo:" + listData.get(4)
                                                                                                     + "\nVersión HW:" + listData.get(5)
                                                                                                     + "\nVersión FW:" + listData.get(2)
                                                                                                     + "\nPlantilla:" + listData.get(3))
                                                                                     }

                                                                                 }
                                                                                 /*
                                                                                 */
                                                                             }
                                                                             else {
                                                                                 Log.d("listdatalolo", "GetRealDataFromHexaImbera :$listData")
                                                                                 runOnUiThread {
                                                                                     binding!!.tvfwversion.setText(
                                                                                         ("Modelo:" + listData?.get(1)
                                                                                                 + "\nVersión:" + listData?.get(2)
                                                                                                 + "\nPlantilla:" + listData?.get(3))
                                                                                     )
                                                                                 }
                                                                             }

                                                                             mHandler.postDelayed({
                                                                                 ObtenerPlantilla()
                                                                                 Log.d(
                                                                                     "claseconexion",
                                                                                     "llave ${conexionTrefp.returnLlaveConecct()}"
                                                                                 )
                                                                                 // Llamar a NopintarLockIcon(true) si returnLlaveConecct() devuelve true, de lo contrario llamar a NopintarLockIcon(false)
                                                                                 if (conexionTrefp.returnLlaveConecct()) NopintarLockIcon(true) else NopintarLockIcon(
                                                                                     false
                                                                                 )

                                                                             }, 1000)
                                                 */

                                                }

                                            }
                                        }

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
//                ConexionTrefp.MyCallback{}
//                try {
//                    Handler(Looper.getMainLooper()).post {
//                        Log.i("PruebaFuncionConexion", "Connection parameters: mac- ${device.mac} name- ${device.nombre}")
//                        conexionTrefp2.MyAsyncTaskConnectBLE(device.mac, device.nombre, this).execute()
//
//                    }
//                } catch (ex: Exception) {
////                    Log.wtf(TAG, "Error while trying to connect: ${ex.message ?: ""}")
////                    resultType = Constants.SCAN_ERROR
////                    state = STATE_FINISHED
//                }

                //    Toast.makeText(this@MainActivity, "Dispositivo seleccionado: ${device.nombre}", Toast.LENGTH_SHORT).show()
            }
            // exportarDatosAExcelPRUEBA()

            val fileName = "OtraPruebaExcel.xls"
            val data = arrayOf("a", "b", "c", "d", "e")
            // CreateExcel(fileName,data)
        }
        binding.Desconectar.setOnClickListener({
//            conexionTrefp2.desconectar()
//            Desconectar()
            conexionTrefp2.desconectar()

/*
            val db = FirebaseFirestore.getInstance()
            val unixTime = System.currentTimeMillis() / 1000
            val instant = Instant.ofEpochSecond(unixTime)
            val dateNormal = Date.from(instant)

            val DatosMOni: Map<String, Any> = mapOf(

                "createdAt" to dateNormal,

                "source" to "oxxomonitor",
                "technology" to "ELTEC",
                // "type" to "MEASUREMENTS",
                // "userUid" to ""
            )
            DatosMOni.map {
                Log.d("sendInfoFirestore", " ${it.key} , ${it.value}")
            }

            db.collection("moni_readings").document()
                .set(DatosMOni)
                .addOnSuccessListener {
                    Toast.makeText(this@MainActivity, "Sincronización de datos completa", Toast.LENGTH_SHORT).show()
                    Log.d("sendInfoFirestore", "DocumentSnapshot successfully written!")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this@MainActivity, "Sincronización de datos errónea", Toast.LENGTH_SHORT).show()
                    Log.d("sendInfoFirestore", "BAD: $e")
                }
            */

        })
        /*  binding.conectar.setOnClickListener {


              val task =

                  conexionTrefp2.MyAsyncTaskConnTest("00:E4:4C:21:7A:F3",//"00:E4:4C:20:A2:23", //"00:E4:4C:21:7A:F3",//"00:E4:4C:21:7A:F3","",//"",
                      //   "IMBERA_RUTA_FRIA",
                      ///"00:E4:4C:00:92:E4",//"00:E4:4C:21:7A:F3","00:E4:4C:21:76:9C",//"00:E4:4C:21:76:9C",////"3C:A5:51:94:BF:A5",//"00:E4:4C:20:A5:7D",/*"00:E4:4C:21:76:9C",*///"00:E4:4C:21:27:BC", //
                      object : ConexionTrefp.MyCallback {
                          override fun onSuccess(result: Boolean): Boolean {
                              // manejar resultado exitoso
                              Toast.makeText(
                                  applicationContext,
                                  " Resultado  = ${result}",
                                  Toast.LENGTH_LONG
                              ).show()
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
                              return progress
                          }
                      })

              task.execute()
           //   conexionTrefp2.g()
          }
      */
        /*      binding.HandShake.setOnClickListener {

                  /*   if (conexionTrefp2.getStatusConnectBle()) {
                         try {
                             val res = conexionTrefp2.GetHandShake().toString()
                             Log.d(
                                 "GetActualStatus",
                                 " res $res  getlist ${res}"
                             )
                             binding.TextResultado.text =
                                 conexionTrefp2.getInfoList().toString()
                             conexionTrefp2.getInfoList()?.clear()
                         } catch (e: Exception) {
                         }
                     } else {
                         Toast.makeText(applicationContext, "Conectate a un BLE", Toast.LENGTH_LONG)
                             .show()
                     }*/
                  val registros = listOf(
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
                      "612F6D9F00C3FE3480"

                  )
                  /*    val registrosMayores = mutableListOf<String>()
                      for (i in 0 until registros.size - 1) {
                          val registroActual = registros[i]
                          val registroSiguiente = registros[i + 1]
                          val fechaActual = convertirUnixHexAFecha(registroActual.substring(0, 8))
                          val fechaSiguiente = convertirUnixHexAFecha(registroSiguiente.substring(0, 8))

                          if (fechaActual <= fechaSiguiente) {
                              registrosMayores.add(registroActual)
                          } else {
                              break
                          }
                      }



                      val w = dataFECHA(registrosMayores as MutableList<String?>, "PRUEBA")//"Lectura de datos tipo Tiempo")

                      println("Registros mayores encontrados: $registrosMayores")*/


                  var listaTemporal = mutableListOf<String>()
                  var resultado = mutableListOf<List<String>>()

                  for (i in 0 until registros.size - 1) {
                      val fechaHora1 = Date(registros[i].substring(0, 8).toLong(16) * 1000)
                      val fechaHora2 = Date(registros[i + 1].substring(0, 8).toLong(16) * 1000)

                      listaTemporal.add(registros[i])

                      if (fechaHora1 > fechaHora2) {
                          resultado.add(listaTemporal.toList())
                          listaTemporal.clear()
                      }
                  }

                  listaTemporal.add(registros.last())
                  resultado.add(listaTemporal.toList())


                  //  val resultadoTransformado: MutableList<String?> = dataFECHA(resultado as MutableList<String?>, "PRUEBA")
                  val result = dataFECHA(
                      registros as MutableList<String?>,
                      "Lectura de datos tipo Tiempo"
                  )/// as MutableList<List<String>?> , "PRUEBA")


                  /****************************************************/


                  /************************************************************************/


                  //   val res = dataFECHAPRUEBA(result as MutableList<List<String>?> , "PRUEBA")

                  // println(result)
                  result.map {


                      Log.d(
                          "DiferenciaResult--------------",
                          "$it   "
                      )
                      Log.d("SIMPLE--------------", "$it     ")
                      //  Log.d("algoritmoSalida-------","  timeStampOriginal $timeStampOriginal  " )

                  }


                  val r: MutableList<List<String>> = divideLista(result)

                  r.map {
                      Log.d("SALIDAANTES", "$it")
                  }

                  for (i in r.indices.reversed()) { // Iteramos en orden inverso para evitar problemas al remover elementos de la lista


                      val sublista: MutableList<String> =
                          r[i].toMutableList() // Convertimos a una sublista mutable para poder modificarla

                      val registro0 = sublista[0] // Tomamos el primer elemento de la sublista
                      val timestamp = Integer.parseInt(
                          registro0.substring(0, 8),
                          16
                      ) - 3600 // Convertimos los primeros 8 caracteres a timestamp y restamos una hora
                      val nuevoRegistro0 = Integer.toHexString(timestamp).toUpperCase().padStart(
                          8,
                          '0'
                      ) + registro0.substring(8) // Convertimos el timestamp a hexadecimal y lo concatenamos con el resto del registro
                      println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> $timestamp   $nuevoRegistro0")

                      while (sublista.isNotEmpty()) { // Iteramos hasta que no haya más elementos en la sublista


                      }
                  }


                  val rr = calculateTimeDifferencesPREFINAL(r as MutableList<List<String>?>)


                  r.map {
                      val timeStampOriginal = GetRealDataFromHexaImbera.getDecimal(
                          data[data.size - 1]!!.substring(0, 8)
                      ).toLong()


                      Log.d("SALIDANUEVOMETODO", "$it")
                  }
                  r.forEachIndexed { index, strings ->

                      strings.map {

                      }
                  }
                  //  println("SALIDA NUEVO METODO " +subListas)


                  /*   res.map {
                         Log.d("calculateTimeDifferences2","$it")
                     */

                  //    val e=      calculateTimeDifferencesPREFINAL(result)

                  //  e.toString()
                  /*       result.map {
                              Log.d("DATOSFLATTER", it.toString())
                          }
              */

                  //  val wer =    calculateTimeDifferences(result)


                  /*     val lastList = result[result.lastIndex]!!.toMutableList()

                        val lastRecord = lastList?.get(lastList.lastIndex)
                        val sdf = SimpleDateFormat("yyyyMMddHHmmss")
                        val calendar = Calendar.getInstance()
                        calendar.time = sdf.parse(lastRecord!!.substring(0, 8))
                        calendar.add(Calendar.HOUR_OF_DAY, -1)
                        var lastTimestamp = sdf.format(calendar.time)

                        var diff: Long = 0
                        if (lastList != null) {
                            for (i in lastList.lastIndex - 1 downTo 0) {
                                val prevRecord = lastList[i]
                                val prevTimestamp = prevRecord.substring(0, 8)
                                diff = sdf.parse(lastTimestamp).time - sdf.parse(prevTimestamp).time
                                lastTimestamp = prevTimestamp
                            }
                        }

                        for (i in lastList.lastIndex - 1 downTo 0) {
                            val record = lastList[i]
                            val timestamp = record.substring(0, 8) + "000000"
                            calendar.time = sdf.parse(timestamp)
                            calendar.add(Calendar.MILLISECOND, -diff.toInt())
                            val newTimestamp = sdf.format(calendar.time)
                            lastList[i] = newTimestamp + record.substring(8)
                            lastTimestamp = timestamp
                        }

                        for (j in result.lastIndex - 1 downTo 0) {
                            val list = result[j]
                            for (i in list!!.lastIndex downTo 0) {
                                Log.d("forEachIndexedQQ","$i")
                                // Restar una hora al último registro
                                // ...

                                // Restar la diferencia de tiempo entre el último registro y el registro anterior
                                // ...

                                // Restar la diferencia de tiempo a cada registro anterior
                                // ...
                            }
                        }

            */

              }
      */
        Log.d(
            "salidaCaract",
            "characteristicToHexString ${characteristicToHexString("[B@d429884")}"
        )
        /*
                binding.TIME.setOnClickListener {
                    //   binding.TextResultado.text = data.toString()
                    /*  val recyclerResultAdapter2 = binding.ReciclerResult.adapter as RecyclerResultAdapter
                      recyclerResultList.clear() // Si recyclerResultList es mutable, puedes usar clear() directamente
                      recyclerResultList.clear()
                      recyclerResultAdapter2.notifyDataSetChanged()
                      //conexionTrefp2.getTime()
                      if (conexionTrefp2.getStatusConnectBle()) {


                          //   conexionTrefp2.getTime()

                          conexionTrefp2.MyAsyncTaskGeTIMEVALIDACION(object : ConexionTrefp.MyCallback {

                              override fun onSuccess(result: Boolean): Boolean {
                                  Log.d("MyAsyncTaskGeTIME", "resultado ${result}")
                                  //     binding.TextResultado.text = result.toString()
                                  return result
                              }

                              override fun onError(error: String) {
                                  binding.TextResultado.text = error
                                  Log.d("MyAsyncTaskGeTIME", "error ${error}")
                              }

                              override fun getInfo(data: MutableList<String>?) {
                                  /*      Log.d(
                                            "CONTEOFINAL",
                                            "data ${data!!.size}"
                                        )
                                        binding.TextResultado.text = data.toString()
                                     //   data?.map {
                                        for ((index, registro) in data!!.reversed()!!.withIndex()) {
                                            val hexString =
                                                registro.substring(0, 8) // valor hexadecimal que se desea convertir

                                            Log.d("datosResultTIMESSSSSSSSSSSSSSSSS", "$registro $index  ${convertirHexAFecha(hexString)} ")
                                        }
                */

                                  data?.reversed()?.map {
                                      val hexString =
                                          it.substring(0, 8) // valor hexadecimal que se desea convertir

                                      recyclerResultList.add(
                                          RecyclerResultItem(
                                              it,
                                              convertirHexAFecha(hexString)
                                          )
                                      )

                                  }

                                  if (recyclerResultList.isNotEmpty()) {
                                      recyclerResultAdapter = RecyclerResultAdapter(recyclerResultList)
                                      binding.ReciclerResult.adapter = recyclerResultAdapter
                                      binding.ReciclerResult.layoutManager =
                                          LinearLayoutManager(this@MainActivity)
                                  } else {
                                      // Aquí puedes mostrar un Toast u otro mensaje indicando que no se encontraron resultados
                                      Toast.makeText(
                                          this@MainActivity,
                                          "No se encontraron resultados",
                                          Toast.LENGTH_SHORT
                                      ).show()
                                  }
                                  try {
                                      CreateExcelTIME("", data)
                                  } catch (exce: java.lang.Exception) {

                                      binding.TextResultado.text = exce.toString()
                                      MakeToast("$exce")
                                  }

                              }

                              override fun onProgress(progress: String): String {
                                  when (progress) {
                                      "Iniciando" -> {
                                          binding.TextResultado.text = progress
                                      }

                                      "Realizando" -> {
                                          binding.TextResultado.text = progress

                                      }

                                      "Finalizado" -> {
                                          binding.TextResultado.text = progress
                                      }

                                      else -> {
                                          binding.TextResultado.text = progress
                                      }
                                  }
                                  binding.TextResultado.text = progress
                                  Log.d("MyAsyncTaskGeTIME", "progress ${progress}")
                                  return progress
                              }

                          }).execute()


                          /*   val R = conexionTrefp2.getTime() //conexionTrefp2.getevent()


                             R?.forEachIndexed { index, item ->
                                 Log.d(
                                     "getTimeReturn", "Listgetevent getLogeer for ${index} : " + item
                                     //   " res getTREFPBLERealTimeStatus  getlist ${conexionTrefp2.bluetoothServices.bluetoothLeService!!.getList()}"
                                 )



                                 binding.TextResultado.text =
                                     conexionTrefp2.getInfoList().toString()

                             }


                             var FinalListData: MutableList<String?> = java.util.ArrayList()
                             val FinalListTest: MutableList<String?> = java.util.ArrayList()
                             var isChecksumOk: String
                             listData.clear()
                             FinalListDataEvento.map { Log.d("Lectura de datos tipo Evento",it) }
                             */

                          /* FinalListData = GetRealDataFromHexaImbera.convert(
                           R    as
                          MutableList<String?>, "Lectura de datos tipo Tiempo", "1.04"
                                  .toDouble().toString(), "3.5".toDouble().toString()
                          )
                          FinalListDataEvento = GetRealDataFromHexaImbera.GetRealData(
                              FinalListData as
                                      MutableList<String>,
                              "Lectura de datos tipo Evento",
                              "1.04"
                                  .toDouble().toString(),
                              "3.5".toDouble().toString()
                          ).toMutableList()
          */


                          /*
                           conexionTrefp2.getInfoList()?.forEachIndexed { index, item ->
                               Log.d(
                                   "getevent", "Listgetevent getLogeer for ${index} : " + item
                                   //   " res getTREFPBLERealTimeStatus  getlist ${conexionTrefp2.bluetoothServices.bluetoothLeService!!.getList()}"
                               )
                               binding.TextResultado.text =
                                   bluetoothServices!!.bluetoothLeService()?.getList().toString()

                           }
                           */
                      } else {
                          Toast.makeText(applicationContext, "Conectate a un BLE", Toast.LENGTH_LONG)
                              .show()
                      }
                  */
           /*         conexionTrefp2.ObtenerStatus(object : ConexionTrefp.MyCallback {
                        override fun onSuccess(result: Boolean): Boolean {
                            customProgressDialog!!.dismiss()
                            Log.d("ObtenerStatus", "--------- onSuccess  $result")
                            return result
                        }

                        override fun onError(error: String) {
                            Log.d("ObtenerStatus", "--------- error  $error")
                        }

                        override fun getInfo(data: MutableList<String>?) {
                            Log.d("ObtenerStatus", "--------- getInfo  $data")
                        }


                        override fun onProgress(progress: String): String {

                            binding.TextResultado.text = progress
                            Log.d("ObtenerStatus", "---------progress $progress")
                            return progress
                        }
                    }
        */

                    val currentTimeMillis = System.currentTimeMillis()
                    val currentTimeHexNOW = java.lang.Long.toHexString(currentTimeMillis / 1000)



                    Log.d("asdfghj","currentTimeMillis  $currentTimeMillis currentTimeHexNOW $currentTimeHexNOW")
                    println("currentTimeHexNOW $currentTimeHexNOW ")


                }
                binding.Event.setOnClickListener {
                    //   binding.TextResultado.text = data.toString()
                  /*  val recyclerResultAdapter2 = binding.ReciclerResult.adapter as RecyclerResultAdapter
                    recyclerResultList.clear() // Si recyclerResultList es mutable, puedes usar clear() directamente
                    recyclerResultList.clear()
                    recyclerResultAdapter2.notifyDataSetChanged()

                    Log.d("PruebaFuncionConexion","${ conexionTrefp2.isBLEGattConnected()}")

                    conexionTrefp2.makeToast("mac ${conexionTrefp2.GetMacBle()}")*/
                   conexionTrefp2. OBtenerComando(object : ConexionTrefp.CallbackLogger {
                        override fun getEvent(data: MutableList<String>?) {

                        }

                        override fun getTime(data: MutableList<String>?) {

                        }

                        override fun onError(error: String) {

                        }

                        override fun onProgress(progress: String): String {
                           return progress
                        }

                        override fun onSuccess(result: Boolean): Boolean {
                            return  result
                        }

                    }).execute()




                }
                binding.EstatusConectado.setOnClickListener {
                    //   Toast.makeText(applicationContext,"${returnStatus()}",Toast.LENGTH_LONG).show()

                    /* Toast.makeText(
                         applicationContext,
                         "result : ${conexionTrefp2.GetMacBle()}   ${conexionTrefp2.GetStatusBle()}",
                         Toast.LENGTH_LONG
                     ).show()*/
                    binding.TextResultado.text = ""
                    /*         conexionTrefp2.getDeviceLocation { latitude, longitude ->

                                 var la = latitude
                                 var lo = longitude


                                 /*  MakeToast("latitud ${String.format("%.4f", latitude).toFloat().toString()}" +
                                           " longitud ${String.format("%.4f", longitude).toFloat().toString()}")
                                 */
                              /*   MakeToast(
                                     "la $latitude ${conexionTrefp2.convertDecimalToHexa((latitude))}   lo $longitude ${
                                         conexionTrefp2.convertDecimalToHexa(
                                             (longitude)
                                         )
                                     }"
                                 )
                                 */
                                /* binding.TextResultado.text =
                                     "la $latitude ${conexionTrefp2.convertDecimalToHexa((latitude))}   lo $longitude ${
                                         conexionTrefp2.convertDecimalToHexa((longitude))
                                     }"*/
                             }
                 */
                    //conexionTrefp2.GETPLANTILLA()

                }
                */
        binding.BTNERnviar.setOnClickListener {
/*
          conexionTrefp2.MyAsyncTaskGeTIMETREFPV2(object : ConexionTrefp.MyCallback {
                override fun onSuccess(result: Boolean): Boolean {
                    Log.d("MyAsyncTaskGeEventTREFPPRUEBA", "resultado TIME ${result}")
                    return result
                }
                override fun onError(error: String) {
                    Log.d("MyAsyncTaskGeEventTREFPPRUEBA", "error TIME ${error}")
                }
                override fun getInfo(data: MutableList<String>?) {
                    data?.let {
                        Log.d("MyAsyncTaskGeEventTREFPPRUEBA", "información de tiempo: $it")
                    }
                }
                override fun onProgress(progress: String): String {
                    Log.d("MyAsyncTaskGeEventTREFPPRUEBA", "progreso TIME: $progress")
                    return progress
                } }).execute()

            conexionTrefp2.MyAsyncTaskGeEventTREFPV2(object : ConexionTrefp.MyCallback {
                override fun onSuccess(result: Boolean): Boolean {
                    Log.d("MyAsyncTaskGeEventTREFPPRUEBA", "resultado Evento ${result}")
                    return result
                }
                override fun onError(error: String) {
                    Log.d("MyAsyncTaskGeEventTREFPPRUEBA", "error Evento ${error}")
                }
                override fun getInfo(data: MutableList<String>?) {
                    data?.let {
                        Log.d("MyAsyncTaskGeEventTREFPPRUEBA", "información de eventos: $it")
                    }
                }
                override fun onProgress(progress: String): String {
                    Log.d("MyAsyncTaskGeEventTREFPPRUEBA", "progreso Evento: $progress")
                    return progress
                }  }).execute()

            */
            var FinalEvento: MutableList<String>? = mutableListOf()
            var FinalTiempo: MutableList<String>? = mutableListOf()
            var FinalEventoCrudo: MutableList<String>? = mutableListOf()
            var FinalEventoCrudoVersion2: MutableList<String>? = mutableListOf()
            var FinalTiempoCrudoVersion2: MutableList<String>? = mutableListOf()

            /*
            conexionTrefp2.MyAsyncTaskGetLoggerFinal(object : ConexionTrefp.CallbackLoggerVersionCrudo {
                override fun getTimeCrudo(data: MutableList<String>?) {
                   FinalTiempoCrudoVersion2 = data
                }

                override fun getEventCrudo(data: MutableList<String>?) {
                   FinalEventoCrudoVersion2 = data
                    Log.d("LoggerFUN", "--------- FinalEventoCrudoVersion2  $FinalEventoCrudoVersion2 ")
                }

                override fun onSuccess(result: Boolean): Boolean {
                    customProgressDialog!!.dismiss()
                    Log.d("LoggerFUN", "---------  $result ")

                    if (result) {

                    }
                    return result
                }

                override fun onError(error: String) {
                    Log.d("LoggerFUN", "--------- tiempo  $error")
                }

                override fun getTime(data: MutableList<String>?) {
                    FinalTiempo = data
                  /*  data?.map {
                        Log.d("LoggerFUN", "getTime $it")
                        if (it.length > 10) {
                            Log.d(
                                "ObtenerLoggerPruebaFUNCION",
                                "--------- tiempo  $it  ${
                                    conexionTrefp2.convertirHexAFecha(
                                        it.substring(
                                            0,
                                            8
                                        )
                                    )
                                }"
                            )
                        }
                    }
                    */
                }

                override fun getEvent(data: MutableList<String>?) {

                    FinalEvento = data
                    Log.d("LoggerFUN","FinalEvento $FinalEvento")
                 /*   data?.map {
                        Log.d("LoggerFUN", "getEvent $it")
                        if (it.length > 16) {
                        }
                    }

                    data?.map {
                        //  Log.d("ObtenerLoggerPruebaFUNCION", "getEvent $it")
                        if (it.length > 16) {
                            Log.d(
                                "ObtenerLoggerPruebaFUNCION",
                                "inicio ${
                                    convertHexToHumanDateNew(
                                        it.substring(
                                            0,
                                            8
                                        )
                                    )
                                }  final ${convertHexToHumanDateNew(it.substring(8, 16))}"
                            )

                        }
                    }
                    */
                }



                /*              override fun getEventCrudos(data: MutableList<String>?) {
                                  FinalEventoCrudo = data
                              }
              */
                override fun onProgress(progress: String): String {
                    /*  runOnUiThread {
                          customProgressDialog!!.show()
                       */   when (progress) {
                        "Iniciando Logger" -> {

//                            runOnUiThread {
                                customProgressDialog!!.show()
                                customProgressDialog!!.updateText("Iniciando Logger")
//                            }
                        }
                        "Finalizando"->{



                            runOnUiThread {
                                customProgressDialog!!.show()
                                customProgressDialog!!.updateText("creando excel")
                            }
                            val handler = Handler()
                            handler.postDelayed({
                                //LoggerExcel(FinalEvento,FinalTiempo)
                                Log.d("LoggerFUN","enviando para el excel  FinalTiempo $FinalTiempo")
                                Log.d("LoggerFUN","enviando para el excel  FinalEvento $FinalEvento")
                                Log.d("LoggerFUN","enviando para el excel  FinalTiempoCrudoVersion2 $FinalTiempoCrudoVersion2")

                                Log.d("LoggerFUN","enviando para el excel  FinalEventoCrudoVersion2 $FinalEventoCrudoVersion2")
                                createCombinedExcelFile(FinalTiempo, FinalEvento,FinalTiempoCrudoVersion2 ,  FinalEventoCrudoVersion2 )//, FinalTiempoCrudoVersion2 , FinalEventoCrudoVersion2)
                            }, 1000)

                        }

                    }

                    binding.TextResultado.text = progress
                    Log.d("LoggerFUN", "---------progress $progress")
                    return progress
                }

            }).execute()
            */
            /*conexionTrefp2.LoggerLuis(object  : CallbackLoggerVersionCrudo{
                override fun onSuccess(result: Boolean): Boolean {
                   return result
                }

                override fun onError(error: String) {

                }

                override fun getTime(data: MutableList<String>?) {

                }

                override fun getEvent(data: MutableList<String>?) {

                }

                override fun getTimeCrudo(data: MutableList<String>?) {

                }

                override fun getEventCrudo(data: MutableList<String>?) {

                }

                override fun onProgress(progress: String): String {
                  return progress
                }

            })
            */

            openFilePicker()
  /*
            lifecycleScope.launch {
                pedirLoger(view)
            }
*/
            /*          conexionTrefp2./*MyAsyncTaskGetLogger*/CLogger(object : ConexionTrefp.CallbackLogger {
                          override fun onSuccess(result: Boolean): Boolean {
                             return result
                          }

                          override fun onError(error: String) {

                          }

                          override fun getTime(data: MutableList<String>?) {

                          }

                          override fun getEvent(data: MutableList<String>?) {

                          }

                          override fun onProgress(progress: String): String {
                            return progress
                          }

                      })
          */


        }

        binding.BTNReset.setOnClickListener {
            /*  conexionTrefp2.MyAsyncTaskResetMemory(object :ConexionTrefp.MyCallback{
                  override fun getInfo(data: MutableList<String>?) {
                      Log.d("MyAsyncTaskResetMemory", "getInfo ${data}")
                  }

                  override fun onError(error: String) {
                      Log.d("MyAsyncTaskResetMemory", "onError $error")
                  }

                  override fun onProgress(progress: String): String {
                      Log.d("MyAsyncTaskResetMemory", "progress $progress")
                      return  progress
                  }

                  override fun onSuccess(result: Boolean): Boolean {
                      Log.d("MyAsyncTaskResetMemory", "onSuccess ${result}")
                      return  result
                  }

              })*/
            conexionTrefp2.MyAsyncTaskResetMemory(object : ConexionTrefp.MyCallback {

                override fun onSuccess(result: Boolean): Boolean {
                    Log.d("MyAsyncTaskResetMe", "resultado ${result}")

                    if (result) MakeToast("exito") else MakeToast("Fallido")
                    return result
                }

                override fun onError(error: String) {
                    Log.d("MyAsyncTaskResetMe", "error ${error}")
                }

                override fun getInfo(data: MutableList<String>?) {
                    data?.map { Log.d("MyAsyncTaskResetMe", it + "  ") }
                }

                override fun onProgress(progress: String): String {
                    binding.TextResultado.text = progress
                    Log.d("MyAsyncTaskResetMe", "progress ${progress}")
                    return progress
                }
            }).execute()
            //  var FinalEvento: MutableList<String>? =listOf(

        }
        binding.BTNGETTIME.setOnClickListener {
            /*  conexionTrefp2.MyAsyncTaskResetMemory(object :ConexionTrefp.MyCallback{
                  override fun getInfo(data: MutableList<String>?) {
                      Log.d("MyAsyncTaskResetMemory", "getInfo ${data}")
                  }

                  override fun onError(error: String) {
                      Log.d("MyAsyncTaskResetMemory", "onError $error")
                  }

                  override fun onProgress(progress: String): String {
                      Log.d("MyAsyncTaskResetMemory", "progress $progress")
                      return  progress
                  }

                  override fun onSuccess(result: Boolean): Boolean {
                      Log.d("MyAsyncTaskResetMemory", "onSuccess ${result}")
                      return  result
                  }

              })*/
            conexionTrefp2.MyAsyncTaskGetHour(object : ConexionTrefp.MyCallback {

                override fun onSuccess(result: Boolean): Boolean {
                    Log.d("MyAsyncTaskGetHour", "resultado ${result}")

                    return result
                }

                override fun onError(error: String) {
                    Log.d("MyAsyncTaskGetHour", "error ${error}")
                }

                override fun getInfo(data: MutableList<String>?) {

                    //  GlobalTools.checkChecksum(data)
                    data?.let {
                        var fe = it.first()
                        var sinespacio = fe?.replace(" ", "")
                        binding.TextResultado.text = convertirHexAFecha(sinespacio!!)
                    }
                    var fe = data?.first()
                    fe
                    data?.map {
                        //    binding.TextResultado.text = convertirHexAFecha(it)
                        // Log.d("MyAsyncTaskGetHour", it + " FECHA   ${convertirHexAFecha(it)} cadena $it")
                        Log.d(
                            "MyAsyncTaskGetHour",
                            it + " FECHA  del control   ${convertirHexAFecha(it)}"
                        )
                    }
                }


                override fun onProgress(progress: String): String {
                    when (progress) {
                        "Iniciando" -> {
                            //    progressDialog2?.second(progress+ " la conexion")
                            //progressDialog2?.secondStop()
                            binding.TextResultado.text = progress

                            //   progressDialog2?.secondStop()
                        }

                        "Realizando" -> {
                            binding.TextResultado.text = progress
                            // progressDialog2?.second(progress+ " la conexion")
                        }

                        "Finalizado" -> {
                            // binding.TextResultado.text = progress
                            //  progressDialog2?.secondStop()
                            //    progressDialog2?.second(progress+ " la conexion")


                            var Vhandler = Handler()

                            mHandler.postDelayed({

                                /*  binding.textView.text =
                                      "Resultado ${sp?.getBoolean("isconnected", false)}"
                                  */
                                sp?.getString("mac", "").let {
                                    //      binding.T.text = " conectado a \n $it "
                                }
                                progressDialog2?.dismiss()//secondStop()
                            }, HandlerTIME)
                        }
                    }

                    Log.d("MyAsyncTaskGetHour", "progress ${progress}")
                    return progress
                }

            }).execute()
        }
        binding.BTNOSENDHOUR.setOnClickListener {
            /*  conexionTrefp2.MyAsyncTaskSendDateHour(object : ConexionTrefp.MyCallback {

                  override fun onSuccess(result: Boolean): Boolean {
                      Log.d("MyAsyncTaskSendDateHour", "resultado ${result}")

                      return result
                  }

                  override fun onError(error: String) {
                      Log.d("MyAsyncTaskSendDateHour", "error ${error}")
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

                      Log.d("MyAsyncTaskSendDateHour", "progress ${progress}")
                      return progress
                  }

              }).execute()
          */
            /*
            conexionTrefp2.MyAsyncTaskSendDateHour(object : ConexionTrefp.MyCallback {

                          override fun onSuccess(result: Boolean): Boolean {
                              Log.d("MyAsyncTaskSendDateHour", "resultado $result")
                              MakeToast(result.toString())
                              return result
                          }

                          override fun onError(error: String) {
                              Log.d("MyAsyncTaskSendDateHour", "error $error")
                              runOnUiThread {
                                  MakeToast(error)
                              }
                          }

                          override fun getInfo(data: MutableList<String>?) {
                              data?.map {
                                  Log.d("MyAsyncTaskSendDateHour", "$it ")
                              }
                          }

                          override fun onProgress(progress: String): String {
                              when (progress) {
                                  "Iniciando" -> {
                                      binding.TextResultado.text = progress
                                  }

                                  "Realizando" -> {
                                      binding.TextResultado.text = progress

                                  }

                                  "Finalizado" -> {
                                      binding.TextResultado.text = progress
                                  }

                                  else -> {

                                  }
                              }
                              Log.d("MyAsyncTaskSendDateHour", "progress ${progress}")
                              return progress
                          }
                      }).execute()
          */
            //  conexionTrefp2.actualizarHoraBCD()

            conexionTrefp2.MyAsyncTaskSendDateHour(object : ConexionTrefp.MyCallback {

                override fun onSuccess(result: Boolean): Boolean {
                    Log.d("MyAsyncTaskSendDateHour", "resultado $result")
                    MakeToast(result.toString())
                    return result
                }

                override fun onError(error: String) {
                    Log.d("MyAsyncTaskSendDateHour", "error $error")
                    runOnUiThread {
                        MakeToast(error)
                    }
                }

                override fun getInfo(data: MutableList<String>?) {
                    data?.map {
                        Log.d("MyAsyncTaskSendDateHour", "$it ")
                    }
                }

                override fun onProgress(progress: String): String {
                    when (progress) {
                        "Iniciando" -> {
                            binding.TextResultado.text = progress
                        }

                        "Realizando" -> {
                            binding.TextResultado.text = progress

                        }

                        "Finalizado" -> {
                            binding.TextResultado.text = progress
                        }

                        else -> {

                        }
                    }
                    Log.d("MyAsyncTaskSendDateHour", "progress ${progress}")
                    return progress
                }
            }).execute()
        }
        binding.BTNOGETDGEO.setOnClickListener {
            conexionTrefp2.MyAsyncTaskGetXYNEW(object : ConexionTrefp.MyCallback {

                override fun onSuccess(result: Boolean): Boolean {
                    Log.d("MyAsyncTaskGetXY", "resultado ${result}")
                    return result
                }

                override fun onError(error: String) {
                    Log.d("MyAsyncTaskGetXY", "error ${error}")
                }

                override fun getInfo(data: MutableList<String>?) {

                    Log.d("MyAsyncTaskGetXY", data.toString())
                    //  GlobalTools.checkChecksum(data)
                    var LATITUDZ: String? = ""
                    data?.get(0).let {
                        LATITUDZ = it
                    }
                    var LONGITUDZ: String? = ""
                    data?.get(1).let {
                        LONGITUDZ = it
                    }

                    var SalidaLATITUDZ = ""
                    var SalidaLONGITUDZ = ""

                    LATITUDZ?.let { it1 ->
                        Log.d(
                            "checkGooglePlayServices", "LATITUDZ $it1"
                        )
                        if (it1.first().uppercase().equals("F") || it1.first().uppercase()
                                .equals("D")
                        )
                        // conexionTrefp2. getDecimalFloat(it1)
                        {
                            SalidaLATITUDZ = conexionTrefp2.getNegativeTempfloat(
                                "FFFF${it1}"
                            ).toString()
                            Log.d(
                                "checkGooglePlayServices",
                                " F ${it1.substring(1).uppercase()}" +
                                        "${
                                            conexionTrefp2.getNegativeTempfloat(
                                                "FFFF${it1}"
                                            ).toFloat()
                                        }                   "
                            )
                        } else {
                            SalidaLATITUDZ =
                                conexionTrefp2.getDecimalFloat(it1)
                            Log.d(
                                "checkGooglePlayServices",
                                "S ${it1.substring(1).uppercase()}"
                            )
                        }

                    }


                    LONGITUDZ?.let { it1 ->
                        Log.d(
                            "checkGooglePlayServices", "LONGITUDZ $it1"
                        )
                        //    conexionTrefp2. getDecimalFloat(it1)
                        if (it1.first().uppercase().equals("F") || it1.first().uppercase()
                                .equals("D")
                        )
                        // conexionTrefp2. getDecimalFloat(it1)
                            SalidaLONGITUDZ = conexionTrefp2.getNegativeTempfloat(
                                "FFFF${it1}"
                            )//.toFloat()
                        else SalidaLONGITUDZ = conexionTrefp2.getDecimalFloat(it1).toString()

                    }


                    binding.TextResultado.text = "LATITUD = $SalidaLATITUDZ   " +
                            "LONGITUD = $SalidaLONGITUDZ  "


                }


                override fun onProgress(progress: String): String {
                    when (progress) {
                        "Iniciando" -> {
                            //    progressDialog2?.second(progress+ " la conexion")
                            //progressDialog2?.secondStop()
                            binding.TextResultado.text = progress

                            //   progressDialog2?.secondStop()
                        }

                        "Realizando" -> {
                            binding.TextResultado.text = progress
                            // progressDialog2?.second(progress+ " la conexion")
                        }

                        "Finalizado" -> {
                            //  binding.TextResultado.text = progress
                            //  progressDialog2?.secondStop()
                            //    progressDialog2?.second(progress+ " la conexion")


                        }
                    }

                    Log.d("MyAsyncTaskGetXY", "progress ${progress}")
                    return progress
                }

            }).execute()


        }
        binding.BTNOSENDGEO.setOnClickListener {


            var Latitud = binding.Textlatitud.text.toString()//.toDouble() //19.39198f //
            var Longitud =
                binding.TextAltitud.text.toString()//.toDouble()// -99.17931f//binding.TextAltitud.text.toString().toFloat()
            /*   conexionTrefp2.getDeviceLocation { latitude, longitude ->

                   var la = latitude
                   var lo = longitude
   */


            // Función que ejecuta MyAsyncTaskSendXYNewPRueba cada 15 segundos

            /*
            scope.launch {
                var contador = 1
                while (contador <= 1) {
                    if (Latitud.isNotEmpty() && Longitud.isNotEmpty()) {
                        conexionTrefp2.MyAsyncTaskSendXYNew(
                            Latitud.toDouble(),
                            Longitud.toDouble(),
                            object : ConexionTrefp.MyCallback {

                                override fun onSuccess(result: Boolean): Boolean {
                                    var name = sp?.getString("name", "")
                                    var mac = sp?.getString("mac", "")
                                    Log.d(
                                        "MyAsyncTaskSendXYPRueba",
                                        "resultado $result   intento $contador  ble $name mac $mac   "
                                    )
                                    MakeToast("resultado ${result.toString()}")
                                    return result
                                }

                                override fun onError(error: String) {
                                    Log.d("MyAsyncTaskSendXYPRueba", "error ${error}")
                                    //  MakeToast("resultado $error")
                                }

                                override fun getInfo(data: MutableList<String>?) {
                                    Log.d("MyAsyncTaskSendXYPRueba", "getInfo $data")
                                    binding.TextResultado.text = data.toString()
                                }

                                override fun onProgress(progress: String): String {
                                    when (progress) {
                                        "Iniciando" -> {
                                            binding.TextResultado.text = progress
                                        }

                                        "Realizando" -> {
                                            binding.TextResultado.text = progress
                                        }

                                        "Finalizado" -> {
                                            binding.TextResultado.text = progress
                                        }

                                        else -> {
                                        }
                                    }

                                    Log.d("MyAsyncTaskSendXYPRueba", "progress $progress")
                                    return progress
                                }

                            }).execute()
                    } else

                        MakeToast("Debes de llenar los campos")

                    // Esperar 15 segundos antes de continuar con la siguiente iteración
                    delay(15000)
                    contador++
                }
            }
*/

            var firmwareString =
                "820091B68200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA01090A44510000000000000000000000010204081000000008405C00405B00405A00405900405800405700405600405500405400404A004049004046004051004050004053004061004060004021002B4D41433D0041542B4D41430D0A0041542B484F5354454E330D0A0041542B454E4C4F47300D0A0041542B4E414D454345425F494E0D0A0041542B42415544380D0A002B424155443D380041542B424155440D0A00004F4B0D0A0041540D0A0000015180894A620012894B61005089F96101008CC864040091B6000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010001010001C2000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000011111222133314441555166617770000000000000000000000000000211112221333000031113222333334443555366637773888000012345678999901000000000100000028000A000A000000030000000100280003000500F00032005A00DE0005FFBA00960091005F006400000000000000000000000000003C00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000078787878787878787878787800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006600000000000000000000000000000000000000000000000000000000001F0000003B0000005A0000007800000097000000B6000000D5000000F300000111000001300000014E0066AE0FFF9490CE8933AE8935F62725A5602717BF0CEE03BF0FBE0CEE0190F6F75C905C90B30F26F5BE0C909390EE031C000520D8AE00002002F75CA3001226F9AE08EE2002F75CA308F226F9CDA51920FE351150C2725F50C6725F50C1725F50CB350150C8725F50C9350350C0725F50C3358050C4725F50D0725F50CA725F50C5725F50CD725F50CC725F50CE35B950CF814D2706721050C22004721150C28135AC50CE353550CEC750CD814D2706721450C22004721550C281721150C6721950C6CA50C6C750C681721550C6721B50C6CA50C6C750C681721050CA814D2706721850CA2004721950CA81899F1A01C750C58581C750C881C650C7815209AE00001F07AE00001F05C650C76B097B09A101260CAE24001F07AE00F41F0520227B09A102260CAE94701F07AE00001F0520107B09A104260AAE24001F07AE00F41F05C650C0A4076B097B095F97D688906B097B09B70B3F0A3F093F08961C0001CDE251961C0005CDE109961C0001CDE11B961C0005CDE251961C0005CDE1095B0981C750C0814D2706721250C92004721350C981899F1A01C750C18581C750CB8189889EA4F06B010D0126350D0327197B02A40F5F97A6015D2704485A26FCCA50C3C750C3AC0093FF7B02A40F5F97A6015D2704485A26FC43C450C3C750C3206A7B01A11026330D0327177B02A40F5F97A6015D2704485A26FCCA50C4C750C420497B02A40F5F97A6015D2704485A26FC43C450C4C750C420310D0327177B02A40F5F97A6015D2704485A26FCCA50D0C750D020167B02A40F5F97A6015D2704485A26FC43C450D0C750D05B038172105190817212519081899F4D27099ECA50C2C750C220097B0143C450C2C750C285814D2706721350CF2004721250CF81899F4D271D9EA11C2606721450C9202E7B01A12C2606721451902022721450CA201C7B01A11C2606721550C920107B01A12C2606721551902004721550CA85818889A4F06B027B03A40F6B010D022607C650C16B0220607B02A1102607C650C26B0220537B02A1202607C650C56B0220467B02A1302607C650C66B0220397B02A1402607C650C96B02202C7B02A1502607C650CA6B02201F7B02A1702607C650CF6B0220127B02A1802607C651906B022005C650CB6B027B015F97A6015D2704485A26FC14022706A6016B0220020F027B025B038172175190818888A11C2611C450C9A10C2606A6016B01202E0F01202A7B02A12C2613C651901402A10C2606A6016B0120150F012011C650CA1402A10C2606A6016B0120020F017B018581A4F0A1202606721751902004721750C9817211507035FC50708189F6A4FEF77F6F02A350932608A640E7036F0520061E01A652E7031E016F041E016F061E016F071E016F01858189F6A4FEF77F7B0C1A0D1A0EFAF7E601A4C7E7017B0F1A10EA01E7017B0B1E01E7027B091E01E7037B0A1E01E7041E01A3509326067B061E01E7057B071E01E7067B081E01E70785814D27067210507020047211507081890D052706F6AA01F720061E01F6A4FEF78581725F50704848C7507081897B051E01E7028581E60281890D062706F61A05F720071E017B0543F4F78581895204C650706B04C650716B01019FA40F974F025D27367B05A5012707AE50751F02201F7B05A5022707AE507F1F0220127B05A5042707AE50891F022005AE50931F021E02E60114066B0420147B05A51027087B0614016B0420067B04A4026B047B045B06818989019FA401974F025D2707AE50751F01201F7B03A5022707AE507F1F0120127B03A5042707AE50891F012005AE50931F011E017B04A40643E401E7015B0481885204A5102707AE50751F03201F7B05A5202707AE507F1F0320127B05A5402707AE50891F032005AE50931F031E03E6016B011E037B01F46B017B05A4066B027B0114026B027B025B05818889A5102707AE50751F01201F7B03A5202707AE507F1F0120127B03A5402707AE50891F012005AE50931F011E017B03A40643E401E7015B03816F047F6F026F0381897B0543E404E7047B06A580271D7B06A5102706F61A05F720071E017B0543F4F71E01E6021A05E70220091E017B0543E402E7027B06A540270A1E01E6031A05E70320091E017B0543E403E7037B06A520270A1E01E6041A05E70420091E017B0543E404E7048581890D062708E6031A05E70320091E017B0543E403E7038581897B051E01F78581890D062706F61A05F720071E017B0543F4F7858189F61A05F78581897B0543F4F7858189F61805F78581E60181F68189E6011405858189F614058581C750E081C750E181C750E28135AA50E08135CC50E081725F52E0725F52E1725F52E2725F52E4725F52E7725F52E835FF52E9725F52E5819FC752E99EC752E8350152E6819EC752E89FA1012606721052E62004721152E681C752E781C752E98188C652E76B017B015B0181C652E8814D2706721252E02004721352E081A1012606721452E02004721552E0814D2706721E52E02004721F52E081A1012606721652E02004721752E0814D2706721052E02004721152E081899F4D27099ECA52E4C752E420097B0143C452E4C752E48581CA52E6C752E68188C452E52706A6016B0120020F017B015B018143C752E5818889C452E56B01C652E414036B020D01270A0D022706A6016B0220020F027B025B038143C752E581899F4D27099ECA52E3C752E320097B0143C452E3C752E38581C652E2A4F8C752E2818888C652E26B017B01A48F6B017B011A026B017B01C752E285818888C652E16B017B01A48F6B017B011A026B017B01C752E185818888C652E26B017B01A4F86B017B011A026B017B01C752E285814D2706721E52E22004721F52E281F6E6016F036F026F046F056F066F0781895204E604A4E9E7047B0D1A0FEA04E704E606A4CFE706E6061A0EE7066F02E603A40FE703E603A4F0E703CD92B1961C0009CDE11B961C0001CDE2517B03A4F01E05E7031E057B04A40FEA03E703961C0001CDE109A604CDE242B60B1E05E7021E05E605A4F3E7051E05E6051A10E7055B068189E606A4F8E7067B061A071A08EA06E7060D052708E606AA08E70620081E01E606A4F7E7068581890D052708E604A4DFE70420081E01E604AA20E7048581897B051E01E70A8581E605AA01E70581E601818989E6045FA4805F02581F011E03E6015F97011A02011A010101A4FF01A401015B0481897B051E01E701858189E604A4BFE704160590549054909FA440EA04E7047B061E01E7018581890D052708E605AA02E70520081E01E605A4FDE705858189E607A4F0E707E6071A05E707858189E604A4F7E704E6041A05E7048581890D052708E608AA08E70820081E01E608A4F7E7088581890D052708E608AA20E70820081E01E608A4DFE7088581890D052708E608AA10E70820081E01E608A4EFE7088581897B051E01E7098581890D052708E608AA04E70820081E01E608A4FBE7088581890D052708E608AA02E70820081E01E608A4FDE7088581890D062708E6081A05E70820091E017B0543E408E708858189897B076B017B08A40F5F97A6015D2704485A26FC6B020D09272A7B01A101260A1E03E6041A02E70420457B01A105260A1E03E6081A02E70820351E03E6051A02E705202B7B01A101260B1E037B0243E404E704201A7B01A105260B1E037B0243E408E70820091E037B0243E405E7055B048189881E06A3010126121E02E60515072706A6016B0120130F01200F1E02F615072706A6016B0120020F017B015B0381897B06431E01F785818952037B09A40F5F97A6015D2704485A26FC6B027B094EA40F6B037B035F97A6015D2704485A26FC6B031E08A30100261D1E04E60414036B031E04F61502270A0D032706A6016B03204F0F03204B1E08A3023526291E04E60514036B031E04E608A4016B011E04F61502270E0D0326040D012706A6016B03201F0F03201B1E04E60514036B031E04F61502270A0D032706A6016B0320020F037B035B0581F6A4BFF781725D10612604AD242021720A001200241A350204084B004B08AE5014CD97E8854B014B20AE5014CD97E885814B004B20AE5014CD97E885720A00120024164B004B08AE5014CD97E8854B004B20AE5014CD97E885CE04045A27285A2603CC9E795A2603CC9EBB5A2603CC9EFD5A2603CC9F515A2603CC9F7E5A2603CC9FABAC009FE59DAE03E889AE0000895F89CE040489AE892589AE892ACDA39B5B0AC30404260E72110012AE0002CF0404AC009FE572000012002403CC9FE5CE04061C0001CF0406CE04065A27265A27345A27425A27505A275E5A276C5A277A5A2603CC9E535A2603CC9E645FCF0406AC009FE5AE04B089AE000089CD9FE65B04AC009FE5AE096089AE000089CD9FE65B04AC009FE5AE12C089AE000089CD9FE65B04AC009FE5AE258089AE000089CD9FE65B04AC009FE5AE4B0089AE000089CD9FE65B04AC009FE5AE960089AE000089CD9FE65B04AC009FE5AEE10089AE000089CD9FE65B04AC009FE5AEC20089AE000189CD9FE65B04AC009FE5AE840089AE000389CD9FE65B04AC009FE5AC009FE59DAE07D089AE0000895F89CE040489AE892489AE8924CDA39B5B0AC30404260E72110012AE0003CF0404AC009FE572000012002403CC9FE5AE0003CF0404AC009FE59DAE0FA089AE0000895F89CE040489AE891289AE891ACDA39B5B0AC30404260E72110012AE0005CF0404AC009FE572000012002403CC9FE5AE0004CF0404AC009FE5AE0BB889AE0000895F89CE040489AE891289AE8907CDA39B5B0AC30404262172110012AE0001CF0404AE0008CF0406AEC20089AE000189CD9FE65B04AC009FE572000012002403CC9FE5AE0001CF0404AC009FE5AE07D089AE0000895F89CE040489AE892589AE88F7CDA39B5B0AC304042702207372110012AE0006CF04042067AE07D089AE0000895F89CE040489AE892589AE88EBCDA39B5B0AC304042702204672110012AE0007CF0404203AAE07D089AE0000895F89CE040489AE892589AE88DECDA39B5B0AC30404270220197211001235020408AE0001CF04049D3566005FAE1061CDA893814B00AE5230CD9A63844B0C4B004B004B001E09891E0989AE5230CD99C95B084B01AE5230CD9A63848152034B004B20AE5014CD97E8854B004B08AE5014CD97E885CE04045A27055A2752205F9DAE07D089AE0000895F89CE040489AE88CF89AE88D5CDA39B5B0AC30404263F4B3DAE04F3CDDFA1841F010F037B035F977B03905F909772F90190E601D7080D0C037B03A10C25E5AE0002CF0404200F350304084B014B20AE5014CD97E8855B03814B014B20AE5014CD97E8854B004B08AE5014CD97E885CDA3E272020012002503CCA28672130012720308620472140862AE000289AE88CC89AE04F3CDDF715B04A30000260435210835AE000289AE88C989AE04F3CDDF715B04A30000260435600835AE000289AE88C689AE04F3CDDF715B04A30000260435610835AE000289AE88C389AE04F3CDDF715B04A30000260435530835AE000289AE88C089AE04F3CDDF715B04A30000260435500835AE000289AE88BD89AE04F3CDDF715B04A30000260435510835AE000289AE88BA89AE04F3CDDF715B04A30000260435460835AE000289AE88B789AE04F3CDDF715B04A30000260435490835AE000289AE88B489AE04F3CDDF715B04A300002604354A0835AE000289AE88B189AE04F3CDDF715B04A30000260435540835AE000289AE88AE89AE04F3CDDF715B04A30000260435550835AE000289AE88AB89AE04F3CDDF715B04A30000260435560835AE000289AE88A889AE04F3CDDF715B04A30000260435570835AE000289AE88A589AE04F3CDDF715B04A30000260435580835AE000289AE88A289AE04F3CDDF715B04A30000260435590835AE000289AE889F89AE04F3CDDF715B04A300002604355A0835AE000289AE889C89AE04F3CDDF715B04A300002604355B0835AE000289AE889989AE04F3CDDF715B04A300002604355C0835CDA512AE08EECDE0F1AE8895CDE01F2403CCA39A720C0012002503CCA39ACDA512AE08EECDE251C60832A1552703CCA3859D7204083402202190AE0803AE084190F6F75C905CCDB72690A3080A23F13508083C72150834CCA38572010834057203083449CE082FC3082D2541720608342690AE083DAE0841725F083190F6F75C905C725C0831C60831A10425EF3504083C721608342068725F083272130834721108347219082A721B082A205290CE082FAE0841725F083190F6720008620F7208082A05720B082A0590AF010000F75C905C725C0831CDB726720108340E90C30838260890CE083672120834C60831C1083A25C490CF082F55083A083C2000C60832A155260E9DC6083C5F9789AE0841CDA4E2858189897200001200251272100012CDA512AE0400CDE2511E03CDA484AD2A1E07891E0D891E0DCDA45B5B041F01CDA512AE0400CDE0F1961C000DCDE01F2504721100121E015B04815206A6906B03AE5089CD96166B047B0411032462C65230A510275BC652305F97C652315F97AE5089CD96166B045F1F05200F1E05D60437D704F31E051C00011F05A60097A690100424015A02130522E21E05724F04F34B00AE5089CD95F0847B0388AE5089CD960D844B01AE5089CD95F084721200125B068189895F1F0172020012002418721300121E0989AE04F3CDDFB95B02A3000027041E031F011E015B04818989CDDFAD1F01AE04171603CDE2634B00AE507FCD95F0847B0288AE507FCD960D844B01AE507FCD95F0845B048189BF0C1E055D270A5A92D60CD704175D26F64B00AE507FCD95F0847B0688AE507FCD960D844B01AE507FCD95F084858189BF0C1E055D270A5A92D60CD704175D26F64B00AE507FCD95F0847B0688AE507FCD960D844B01AE507FCD95F0848581AE040DCDE109819D550000005F555000005F558000005F558880005F558881005F3501086CCDA7CECDA71635000061350000073500006235000063350000063581006435020054358100604560A6357800A7350003BD721003A8721003A9721403A8721703A9350A03A6350003AA350003AC350003AD350203AF350503B0350803A635FF03A7721903A9721B03A9721D03A93FFB721903A83504000435D3000335A40005350F00023F0135010000AE1057CDA8D955005F0157356E00ABC60157A102260435D200AB721000B0721300B0721700B0359600C6359600C7359600C8359600C935AA01003566014135CC017F55104800D7CDB6EDCDB700CE1283CF07FACE1285CF07FC725F07E9725F07EA725F07EB725F07EC725F07ED7216085FAE50001C007FAF010000B7603F5FA37FFF26043F5F200D3C5F1C0080AF010000B16027EA55005F07F7C607F7AE0080421C50001C007EAF010000C707F6AE06E2CF07F2AE5000CF07F05507F707EBCDB773AE00001C007FAF010000B7603F5FA34FFF26043F5F200D3C5F1C0080AF010000B16027EA55005F07F9C607F9AE0080421C00001C007EAF010000C707F8AE0762CF07F2AE0000CF07F05507F907EBCDB773CCB88281BD9298ACC8C09D808884E0C3B0C2C6F0E3E2A4F780AFF5DAF4AEDADAF486FFE284E3C3C686FEA1F68EFFFFFFFFFFF1E284E3C3C6FFFFFFFFFFFFFFFFFFFFFF350150C2350050C6350050C8350050C5350050C0350050C935FF50C335FF50C435FF50D0352053403583534135605342350053433580534AA600C7534BC7534CC7534DC7534EC7534FC753513538535035FF500235FF500335005004359F5007359F50083500500935F8500C35F8500D3500500E7216500A35EF501135EF50123500501335EE501635EE50173500501835FF501B35FF501C3500501D350452E835FA52E9350152E0A655C750E0A602C750E1A6FFC750E281351052123590521B3501521C3511521D3500521A3500521335005214350052173500521835005219350052163501521035045211819093FE5D27035A90FF81F6B7604D27053A60B660F7813F5FB679BA7A26047210005FBE6FC301232F047212005FC60159A101270BBE71C301212F047218005F7201006404721E005F455F5B8190AE0100909FBBFD9097AE10009FBBFD97B6FCA1AA261A90F6B75F90AE031E909FBBFD909790F6B15F270CB75FAD152006AD57B65F90F73CFDB6FDA18025043FFC3FFD817206505410A6AEC75053A656C750539D72075054FAB65FF7720052E503CCB591721152E53C5F720300060B7201005F0672125014200472135014720450540220D77217505481F6B75F813D682603CCA9F1B668A1012704721B00673D6626063D65260220E9720000AD3B721A00673513005F7201006602200B720200660220283513005F455F58CDAA45CDB6EDCDB70045585F721100677213006735010060455F5A456059CCA9F17208006505720B006508351300603503005F72040066057207006608351300603502005F7205006508351F00603520005F7207006508351F0060350F005F7209006608351F0060351E005F720D006508351F0060350A005F7203006508351F0060350C005F720F006608351F00603527005F720D006608352600603527005F7201006508350E0060352F005FB668A1012709B655C4015A2702201B200D720C005608351F005F351F006045605A455F59B667A4FCB767819D72015343FAC65344C6534535235340725F53439D72015343FA555344008555534500868138609FBB6097B686F75CB685F7813508005F905FF6B7515CF6B7505C72B900503A5F26F090549054905490BF508181A63C97C60149429FB78A9EB7898190BF50B6509097B660904290BF5CB6519097B6609042909FB751909EBB5DB75081550146006090AE0E10ADD445517645507590BE50ADB0550156006090BE50ADBF4551824550818145556045565F385F3960385F396081BE6BBF57CDB3C581BE6DBF57BE6BBF5FCDB442CDB3D1815501650060CDB44C45518445508381457060456F5F81720403A803CCAB6F721503A8720903A821AE00F490AE00FA35000060350A005F350103B3350003B4350A03B5350003B6204D720103A821AE03C390AE03C5350900603507005F350103B3350103B4350703B5350903B62027721F00B0721303A9AE100090AE107F35800060358C005F350C03B3350A03B4350F03B5350F03B6CF039E90CF03A055005F03B9725C03B955006003BACDABFAC603B3A101271D720703A918350103B3350A03B4350403B5350F03B6350003B7721203A9725C03B7C603B7C103B3262C721203A9725C03B8C603B8C103B42604721403A95503B803BC725A03BCC603BC48484848CB03B6C703BC2004350003BC5503B703BB725A03BBC603BB48484848CB03B5C703BB5503B503A25503B603A3C603B6A003C703A4AD328190CE039EAE018090C303A0220890F6F75C905C20F2720003A816350C005F90AE080D90F6F73A5F3D5F27055C905C20F281725F03B2725F03C15503BB0060CDADB6B660A003C703C297B65F42909372B903C1905ACF03C1AE018072BB03C1CF039E90CF03C190AE018072B903C190CF03A0725F03C1720003A806358C03C22004350703C2AE018072BB03C15A90CF03C1C303C12403CF03A090CE039EAE025E45605F3A5FC603BBAD30C603BCAD2B720903A906A637AD22201690C303A0220A90F6905CAD1426F22006A600AD0C26FA5503B203BDC603B2F781F7CB03B2C703B25C3A5F81AE025E9FCB03AC97F6C75216725C03AC81AE026E9FCB03AD97C65216F7B760C603A34A4AC103AD24022010B660A1002704721303A8CB03B2C703B2725C03AD81725F03C15503BC0060CDADB6B660A001C703C297B65F42909372B903C1905ACF03C1AE027E72BB03C1CF039E90CF03C190AE027E72B903C190CF03A0725F03C1720003A806358C03C22004350703C2AE027E72BB03C15A90CF03C1C303C12403CF03A090CE039EAE026E45605F3A5F90C303A0220CF690F75C905C3A5F26F020019D81CE039E90AE027EC303A0220890F6F75C905C20F381B660A4F044444444B75FB660A40FB76081A600AE03C7F75CA303CE220220F781350203BF35F003C035F003C0725A03BF2708725A03C027F020F8813508005F3F513F50905FF6B7513F5072B900505C3A5F26F2909FB751909EB75034503651345036513450365181720300CD25721200B0351400953FBF3FB93FE8AE100090AE031ECDA8D9B65F90F75C905C90A3039D23F0720200B003CCB35A3DB12704351400953D952603CCB346720500B00B720800B003CCB1CBCCB1A8720700B003CCAF8C721300B1721B00B1B6B9350900C2350000C4CDAFC74560B9A60FB4B9AE000242DCAE9A201E2026202E2036203E2046204E2056205E2066206E206C206A2068206620643505005A352500592062350D005A350F005920583520005A350A0059204E3520005A350B005920443500005A350F0059203A3525005A350500592030350F005A352700592026350F005A350A0059201C3505005A350E00592012351F005A350E005920083526005A352600597211006772130067720500B156721600B0B6B9A1092603CCB346A1082645720100E804721000E9720300E82C721300E8550357031F5503580320550359032155035A032255035B034B55035C034C55035D034D55035E034E350000FD35AA00FCCCB346CCB35AA60FB4B9AE000342DCAF97CCB049CCB062CCB07BCCB09ECCB0BDCCB0EDCCB112CCB122CCB151CCB15BCCB165CCB165CCB165CCB165CCB165CCB165721C00B05F973FC13FC3720000B125720800B1413DD1261A351900D13D962604350A00D1720200B10B720A00B127350500962044B3C12F04BEC32038720C00B00AA300642E0AA3FF9C2F051C000120241C000A201FB3C32C04BEC12017720C00B00AA300642C0AA3FF9C2D051D000120031D000A351E0095BF57721D00B045586081CE032DBFC1CE032BBFC3CE031FCDAFD1CF031FCDB3C5CCB16DAE0063BFC1AE0000BFC3CE0321CDAFD1CF0321CDB3C5CCB16DCE032D1C0096BFC1CE031F72BB03211C000ABFC3CE034BCDAFD1CF034BCDB3C5CCB16DCE031F1D000ABFC1CE032B1D0096BFC3CE034DCDAFD1CF034DCDB3C5CCB16D720000B107720800B1022004901000E83528005A35010059720100E8083500005A351D00597211006772130067CCB16D3525005A35050059356300C2350000C4B6BFCDAFC74560BFCDB41E7211006772130067205B55107B005ACE107CA60A629FB759204B720000B107720800B1022004901200E83528005A35010059720300E8083500005A351D00597211006772130067201C3505005A350E00592012351F005A350E005920083526005A35260059720500B133721700B0B6B9A1052629B6BFC101732622721400B03FB8721700E8720100B004721600E8721900E8C60152A1202604721800E8CCB35AA607B4B8AE000342DCB1B3CCB292CCB2C1CCB2F0CCB31FCCB329CCB333CCB333CCB333721300B1721B00B1B6B8350400C2350000C4CDAFC74560B8A607B4B8AE000242DCB1EE200E2016201E2026202E2036203420323500005A350F005920303511005A3527005920263527005A351D0059201C3505005A350E00592012351F005A350E005920083526005A352600597211006772130067720500B14A721800B0B6B8A1042603CCB346A1032639721100B0720700E804721000B0720500E804357800EA35000370720900E80435200370C60370C11052270B550370005FAE1052CDA893CCB346CCB35A720000B107720800B1022004901400E83528005A35010059720500E8083500005A351D00597211006772130067207A720000B107720800B1022004901600E83500005A350F0059720700E8083500005A351D00597211006772130067204B720000B107720800B1022004901800E83529005A350C0059720900E8083529005A350F00597211006772130067201C3505005A350E00592012351F005A350E005920083526005A35260059720500B104721900B02014721300B0721700B0721B00B0721500B0721900B081720000AE022022720200AE02201B720600AE0E350000FD35AA00FC721600AE20083DFC2604721400AE81BE73BF57CE0105BF5FCDB439456058455F57C60152A1202627BE572A1550A60262A61242A60562BF57AE014072B00057200CA60262A61242A605621C0140BF57CDA9DDBE572A055072120067A60A62B7589FB757B657720300671335230060720300B00435640060B1602437201135630060720300B00435640060B1602424B657A10A241172100067BE572604721300675EBF59201D455760AD2C455F5A4560592010720A005603CCA9DD3510005A3511005981BE5F72BB0057BF5F81BE5F5072BB0057BF57813F5FBE5FA60A62B7609FB75FAE0006B66042BF50AE003CB65F4272BB0050BF50813F583F5790BE5090BF7BBE7BA30338240EBE7BA302B1241AA300B7242820393534006090BE7BCDAA54BE501D93CCBF572039351E006090BE7BCDAA54BE501D4D00BF5720263514006090BE7BCDAA54BE501D3219BF5720133522006090BE7BCDAA54BE501D3C19BF572000BE5790932A01509EB75FA60A429E5F97BF57B65F97A60A4272BB0057905D2A0150BF57815F905FCE07E390CE07E5725F07E772105051C607E2A1AA262E720250540FA656C75052A6AEC7505272035054F135AA50E090F6A70100005C905C725C07E7C607E7A17F23E82029720650540FA6AEC75053A656C7505372075054EC35AA50E090F6F75C905C725C07E7C607E7A17F23EB3F5F35AA50E0720152E51A721152E53C5F720300060B7201005F067212501420047213501472055054D87213505472175054CCB772B6BBA10224124E484C45BABE38BEBBBEB7BE350300BD200B4EAB22BBBAB7BE350300BD81B6B9A1052336A1092732A140275DA1422759A145277AA1462751A147274DA1302750A1312752A1322754A1332756A1342758A135274EA1362744205AA100271FA103271BAE0063BFC1A102270BA1052707AE0000BFC3203EAEFF9DBFC32037CE032DBFC1CE032BBFC3202BAE0063BFC12024350100C2201E353F00C22018350900C22012350200C2200C350800C22006350100C42000817208015309355000B9721600B081B6B9720000B107720800B11C2032A1012602A602A1042602A605A1062602A627A1282602A6502018A14F2602A627A1262602A605A1042602A603A1012602A600B7B9819D20FDB677A4E04D26F6B683A4F84D262CB68BA4F84D2625B6B4A4FC4D261EB6B6A4FC4D2617BE6DB36B2F11C652E8A0044D2609C652E9A0FA4D2601819D20FD55106F006090AE0168CDAA544551E24550E181551070006090AE0168CDAA544551E44550E38155104B006090AE003CCDAA544551E74550E681BF5090BF5790CE083DCE083F3F5CB75D72BB005C2402905C90CF083DCF083FBE5090BE5781725F07E85F905FA6AAC750E0AEB4FC90AE063BF690F7725C07E85C905CC607E8A1A523EFCC063B8190CE07F2C607EBAE00804272BB07F03F60AF01000090F75C905C3C60B660A18025EF81CE07FABF5045505FAE1283CDA89335AA50E045515FAE1284CDA89335AA50E0CE07FCBF5045505FAE1285CDA89335AA50E045515FAE1286CDA89335AA50E081F6CDB7265C3C60B660B15F25F3F6B7505CF6B7515CF6B7575CF6B758813F5FB660A40FBB5FB75FB6604EA40FAE000A429FBB5FB75F815F5508A70060ADDFB65F971C07D0CF08ADC608A8A41FB760ADCD55005F08AF5508A90060ADC155005F08B05508AA0060ADB555005F08B15508AB0060ADA955005F08B25508AC0060AD9D55005F08B381AE06E27F5CA3076223F9813F607F5C3C603D6026F8813F60F690F75C905C3C60B660A18025F28135060416CDDEC9CDDEEBCDDF704B01AE5230CD9A638435030408725C041620019DA601CD98FB4D27F7A601CD990E9D720500070672145005200472155005720F006206721E500A2004721F500A20007205000606721450142004721550143C52720300060B7201005206721250142004721350143C99B699A1282B023F999D725C0638C60638A1C82307721008A6CCBB7272150600720006000672130600200472120600352353403580534A3500534B3510534C3500534DCDA9F245855F45866090BE5F90A3000A240672110600200472100600C60600A403A1012702200C721406009010500072100001720406000AC6063AA1012711CCBB7EC60601A1002703CCBB723501063AC60601A120260A725F06013500063A2036352353403580534A3500534B3510534C3500534DCDA9F245855F45866090BE5F90549054AE06189FCB060197909FF7725C0601CCBB7E725F0605725F0604725F0603725F06023F5FAE06189FCB060197F6B760BE5FA66442A63C62BF5FBF50B66097B65142CF0609B65F97B65142CF060BB66097B65042CF060DB65F97B65042CF060F55060A0614C60609CB060C3F6024023C60CB060EC7061324023C60B660CB060B3F6024023C60CB060D24023C60CB0610C7061224023C60B660CB060FC70611CE061372BB0604CF06043F5F3F6024023C60BE5F72BB061172BB0602CF0602725C0601C60601A1202703CCB9E1725F0601C60603A11F23022012C60603A11F27022012C60604A180250B2000AEFE01CF0606CCBB7EA605725406027256060372560604725606054A4D26ECCE0604CF06068952041F035F1F01200F1E031F011E0516036572FB03541F031E01130327055C130326E61E035B069FC70639725F0638721108A6721308A6C61282A13C260220615FC606399790AE003C90BF5FB35F2202204D90AE005A90BF5FB35F244235640060550639005F720650540D35AE50533556505372075054FBB660AE1281F772045054FBB65FAE1280F772045054FBA63CAE1282F772045054FB35005054200220FE200C725F0601725F06383500063A7201006A167202000109720100011072120001AE0005CDA80D2004721300017203006A167204000109720100011072140001AE0004CDA80D200472150001720100AF167206000109720100011072160001AE0003CDA80D2004721700017205006A167208000109720100011072180001AE0002CDA80D200472190001721100019DCDDDDF725D086C27254B014B08AE5014CD97E8854B004B20AE5014CD97E8854B00AE5230CD9A63843501040820254B01AE5230CD9A63844B004B08AE5014CD97E885C604084A27084A27224A27242003CD9CEDCDDE34720C001200241C9D720E08340435F0086A721E08342025CDA00F20E1CDA09420DC20DA9D721F0834725F0832725F0834725F08627219082A721B082A720A001200240A9DCE05C31C0001CF05C34B01AE500FCD980B84CDB80BAE0000CF08BAAE0000CF08B8C608AEA5032617C608AFA1032510AE5180BF0AAE0001BF08AE08B8CDE04F725D08AF271CC608AF97A604421D00041C08BCCDE109AE892FCDE066AE08B8CDE04FCE08AD90AE016DCDDFED1DF8CACDE287AE892FCDE066AE08B8CDE04FCE08AD1D07B1545401C708EC02C608ECB70B3F0A3F093F08AE892FCDE066AE08B8CDE04FC608B05F975ACDE011AE892FCDE066AE08B8CDE04FC608B15F9790AE0E10CDE28EAE08B8CDE04FC608B297A63C42CDE011AE08B8CDE04FC608B3AE08B8CDE03FCE08B8CF08B4CE08BACF08B69DC604164D272D4A2603CCC29E4A2603CCC3B14A2603CCC6654A2603CCC68C4A2603CCCC694A2603CCCD764A2603CCDBCC9D20FE9D720300AE1C720400AE0735FF03A7CCC091721100AE721300AE721500AE721700AE725A03AB2703CCC0915503A603ABA607C403AAC703AAA10026022020A1012602207BA1022603CCBE9FA1032603CCBF16CCC299CCC299CCC299CCC2997203521903CCC034720103A925350303A6720903A804A6AA200B720103A804A6552002A65FC703B172145211721103B1200C725F03B2350103A6721003B172105211CDADD67200521703CCC0915503B15216350103AA7203521703CCC041CCC0917202521703CCC0457202521903CCC04135FF03A7720003A906350303AA207E350203AACDAAD8C603ACC103A2270E720E521703CCC041CDACDECCC091720E521703CCC0417204521703CCC04172125211725F03AC350503AB350103A6350003AA35FF03A7720303A931721603A9721303A9721103A9720903A820721903A8350003AA350303A6350A03AB350003B7350003B8721403A8721003A9CCC091C603ADC103A42412720C521703CCC028350703B0CDACEFCCC091C603ADC103A4261A7204521703CCC028350703B072155211CDACEF72125211CDACEF720C521703CCC028350703B0CDACEF350003AA350303A6350503AB35FF03A7725F03AD720303A80220163F5F5503A300603A60AE026E72BB005FF6C103B22711C603AEA1042603CCC041725C03AECCC091725F03AECDAD1E720103A80EAE03C7CF039EAE03CECF03A0200CAE031ECF039EAE039DCF03A0720503A93ACDADA1720003A826721603A8C60398A480A1802619C60398A47FC70398720A03A90C721803A9721A03A9721000AE721503A9350003B7350003B8720103A804721403A8721F00B0721003A9350003B7720100AE04721200AE2069725A03B02663350703B0200D725C03A5C603A5A10A27022050721E00B072125211725F03A5725F03AE350003AC350003AD350003AA721003A9721703A9350003B7350003B8C6080DA1782704901003A8721903A8720703A804721C03A9721903A9721B03A9721403A82000B668A1012603CCC288720601530220097200501502204C200772015015022043AE009CCDA80D3D602703CCC15F721100E555014800D772190066CDAA467202006403CCC15F7213006472070064042033207ACDB7133F773F787216006472180067203AB69CA18024023C9CB69CA180252E721000E53505008D725D01482604356300D7CDAA4572060064022012721000B072170064721900673FE73FE62014B68ABA89260E3DD7270A725D0149270472180066720300E50435F0009D721300E5721500E5CDB6EDCDB7002048720200E50ACDAAD1BE5FC301292ED83D00270220D2B6E2BAE1270DCDB700721300E5721500E52020720200E50435F0009D721200E5B6E4BAE3260D720400E50435F0009D721400E53F693FCD3FB1720200B015720103CD10720503CD0B3DCC2604721200CDCCC288B6CCA1962404AB03B7CC720103CD2F720000C50472100069720300B00DB6C6A16E222972120069CCC288720000AC1D3DC6261972120069721000ACCCC288721100ACB6C6A1962404AB03B7C6720303CD13720400AC2B3DC7262772160069721400AC205D721500AC720300C50AB6C7A17D250472140069B6C7A1962404AB03B7C7720503CD2D720400C50472180069720300B00CB6C8A16E2227721A00692021720800AC1C3DC82618721A0069721800AC200E721900ACB6C8A1962404AB03B7C85503CD00C5720300B0054569B13F6920009DAC00DDC89D720C006A03CCC39E3CA8B6A8A1192402206F3FA85506390060B660A1FF260AB6DCA10227163CDC20583FDCB660A132222BB6DBA10227043CDB204672110065B668A1012604721000653FA9357300ABC60157A102260435D200AB20253FDB72110065AE023E9FBBA997B660F73CA9B6A9A1082402200B3FA9AE023ECDADF14551AB720E006604B6D22002B6D4B1AB2212B694A10027022067721E0066721D0066202E5501450094B668A101260435020094720C006604B6D32002B6D5B1AB2520B693A10027022037721F0066721C00665501630060CDB44C45518C45508B201F5501450093B668A101260435020093B68CBA8B27022008721D0066721F0066720C006A08721D0066721F00662000AC00DDC89D3C9AB69AA1322403CCC63AAE009FCDA80DAE00A0CDA80DAE00EBCDA80D3F9A352353403580534A3540534B3500534C3500534DCDA9F290BE85B668A101270890A303AE2512200690A301C4250A72180065721B0065201CB668A101270890A3002B2416200690A301AE240E721A006572190065350F00EB200EB6EBA100260872190065721B0065AE024E459B60CDAA17352353403580534A3500534B3520534C3500534DCDA9F290BE85B668A101270890A303AE2512200690A301C4250A7210006672130066201CB668A101270890A3002B2418200690A301AE24107212006672110066350F009F3FA2200EB69FA10026087211006672130066AE021E459B60CDAA17352353403580534A3500534B3504534C3500534DCDA9F290BE85B668A101270890A303842512200690A301C4250A7214006672170066201CB668A101270890A3002B2416200690A301AE240E7216006672150066351400A0200EB6A0A1002608721500667217006672040066022008350A008635000085AE020E459B60CDAA173C9BB69BA1082403CCC63AAE024ECDAA25CDB46DBE57BFECAE020ECDAA25A678B7A7721B0066CDB46D455872455771AE021ECDAA2590BE7D72B20050274E2A1272A2FFFF271E90BE5072A2000190BF50202D72A20001270C90BE5072A9000190BF50201B90BE7F72B2005027043FA120023CA190BE5090BF7FB6A1A10F2515BE50BF7DCDB46D45587045576F457E80457D7F3FA1B69FA100270BA1092541AEFE34BF6F20527201006402203390BE7372B2006F27422C28C60154A1002721483C9EB19E2234905090A3000B2509BE6F1D000ABF5F200EBE731C0001BF5F2005CDAAD13FA2B6A2A100270990BE6D72B2005F2F04BE5FBF733F9E3F9B7207006605AEFE34BF71B668A101271FC60159A1032718A102270CA1012610721500667217006672190065721B00652000AC00DDC89D9DB668A1022518720F00B0022011720500690472120064725D01582602200020009DAC00DDC89D3F6A3FAF550153006072010060047216006A72030060047218006A5501620060CDB44CC60157A1022618C60143AB64B7D3BB5FB7D5C60144ABC8B7D2B060B7D42018A1012618C60142B7D3BB5FB7D5C60143AB64B7D2B060B7D4721C006A721200AFCE010190CE0103BF6BBF5790BF5FCDB439BE5FBF6D720300E525BE6B72BB011BBF6BBE6D72BB011BBF6D720500E510BE6B72BB011DBF6BBE6D72BB011DBF6D72030068022014CDA9DDB667A480B7677200006803CCC85CCCC90BCDB3857215006772170067720A015305720400E50E7206006409720100B0047214006A7200006803CCCA51CCCBFE72100064A602B75435000068200EA611B78FA608B7D8350100683F543F563F5520655501640060CDB44CB65F97A63C42BFB4B66097A63C42BFB6350200682043CDAAC235020068203A351600843500008335020068202C350208A53F90CDAA755501470060C60155A1012605CDB44C2008AE003CB66042BF50455178455077350300683504008E720E006605720D0066077211006ACDAAC27200006A0472150067B6E6BAE7271E720F500A03CDAAC27211006A7213006A721100AF72150067721F0067201D7207006418721000B07217006472190067721200E53FE23FE13FE43FE3CCCC65721700ADCDAA45CDAA46721D0066721F00663502009135050060720D00AD043501006045608D456076CDAA9CB660A1032716A105272EA107272FA101262E5501460057CDB41B20245501470057C60155A1012609CDB41B72100067200F3F58CDB4042008CDAAAB2003CDAAB3CDA819720F005B022028C60159A10126077208006A162017720600660F720400660A7208006A057209005B03CDAA75CCC7C03D5E260A72000066022003CCC785CCC7FE458F60456079CDAA9CB660A1002607721E00ADCCCA43720200AD4F35050060CDB41E72100067721000ADB68FA110230E7201501506721800662025CCCA43B68FA10D231872130067720150150C721400AD72120067721300AFCCCA43721100AD3D6626063D652602204E350A005A350A0059720C005608351F005A351F0059721200AD721E00AD720E006605720D006624721700AD720000D804721600ADB6ABA1642508A064A1642502A064B760CDB41E72110067CCCA43720400AD0220AB7210006A72140067B68FA10B230220697211006A721500677212006A72160067A109230220537213006A72170067721000AF721E0067A1072302203D721100AF721F00677214006A72180067A105230220277215006A72190067B6ABA1642508A064A1642502A064B760CDB41E72110067721C00AD721F00ADCDA8197201005B03CCC777CCC7FE721F006745B760B6B6BA60270220523DD727063D8D27022048720F500A063D97263F20355501680060CDB44C3D5F2602202F3D60260220217205501402200F3DB32702201C3DB22610455FB2200B3DB2270220054560B32008721000AF721E006745767945757A72000066057203006602204C7202006549CDAAD1BE5FB36D2F11B684BA83263A45B560B6B4BA6026312023BE6BB35F2F16721D0067AE0000CDA80D720E500A022018CDAAC22013720E500A02200C7210006A72140067200220E1725D014D2603CCCBA8720E500A2755014D0060AE003CB66042B3EE2709C60159A10327532033BF504551EF4550EE1D0001BFF22061C60159A103272A7208006551720A00654CBEEEB3F22606BE6FBFF02044BEEEA30001263DBEF072B0006FC301332E2E201C72080065D1720A0065CCBEEEA30001261FBE6F72B000ECC301332E10721C00653F753F7645767945757A2004721D0065CDAAD1BE5FC301132F05CDAA75202CC60159A101271A45726045715FBE5F720600660D7204006608C301152E03CCC7CECDA8197201005B03CCC7CE3D8E26147207006903CCC7CE720100E907721100E9CCC7CECCC7FE72160067721F0067725D08A52710350D005A350F00597211006772130067C60159A101270FC60155A10126047210006AA100260E3D8D260E721000AF721E006720047212006A3F5E55014C00A245787945777ACDA819A613B45B4D2703CCC7993D8E2600CCC7FEAC00DDC89D720F083412350B005A35110059721100677213006720157207006410351F005A351D00597211006772130067CDAE1EB668A101271C720F00AD17350A005A350A0059720C005608351F005A351F0059203F7201006402202BB69DA100271EA1C8241CA1962505CDAAAB2025A164240FA132250BCDAAB37213006720142005CDA9DD200D720600AD08720200B003CDA8DD5FB65AA43F97AF00A6D6C703C35FB659A43F97AF00A6D6C703C435FF03C57200006704721303C57202006704721103C57204006704721903C57206006704721B03C5720E006704721D03C57208006704721F03C5721503C57206006710720100E514720A00560F721403C52009720E005604721403C52000AC00DDC89DB699A1082503CCCEEFBE555CBF55AE009DCDA80DAE00A4CDA80DAE00CFCDA80DAE00D0CDA80DAE00D1CDA80DAE00D6CDA80D3C53720100532AAE00C6CDA80DAE00C7CDA80DAE00C8CDA80DAE00C9CDA80DAE00CACDA80DAE00CBCDA80DAE00CCCDA80DB653A1642403CCCEEF3F5390CE07FACE07FC5C2602905C90CF07FACF07FCAE0860CDA803AE08A5CDA80DAE008DCDA80D5CA3009825F7AE00EECDA803AE086ACDA80DAE086BCDA80DAE086CCDA80DAE07E9CDA803AE0833CDA80DAE00E1CDA803AE00E3CDA803AE00E6CDA8037200008F06AE00D8CDA80DAE008BCDA803AE0083CDA803AE0089CDA803AE00B6CDA803AE00B4CDA803BE75B3812407720E0062022006AE0075CDA803AE0077CDA8033C5EB65EA13C25083F5EAE00A2CDA80D3C54B654A1092504721100647209005404721F0064B654A13C253C3F54721F00ADAE00D7CDA80DAE00B2CDA80DAE00B3CDA80DAE00A3CDA80DAE00A7CDA80DAE0087CDA803AE00DDCDA803AE00DFCDA803AE00EACDA80D2000720E083403CCD82BC6086AA10126043505086C720108621735F0086A725D086B260A725F0862725F08352003CCD31FC60835A121260735F0086ACCD00B7208083403CCD82BC60835A160260735F0086ACCD065C60835A161260735F0086ACCD0DCC60835A153260735F0086ACCD153C60835A151260735F0086ACCD1FEC60835A150260735F0086ACCD249C60835A146260735F0086ACCD2EDC60835A154260735F0086ACCD4BFC60835A155260735F0086ACCD55EC60835A156260735F0086ACCD561C60835A157260735F0086ACCD564C60835A158260735F0086ACCD567C60835A159260735F0086ACCD5DFC60835A15A260735F0086ACCD6BDC60835A15B260735F0086ACCD709C60835A15C260735F0086ACCD755CCD82B35F1080B353F080C5501790819558880081A558881081B55017D081C55017E081D35FF081E35FF081FAE080BCF082FCF082BAE081FCF082D3515083A72180834721508345FCF083DCF083F7217083435550832725F0835CCD82B5507F707EB5507F607EC3503083BAE06E2CF07F2AE5000CF07F07203082A04721008343508083A721A082AAE5000CF0836AE7FFF5CCF0838356007EECDD7CD55017B080355017C0804AE0000CF0805AE0540CF0807350108093509080A721408345FCF083DCF083F7217083435550832725F0835CCD82B5507F907EB5507F807EC350A083BAE0762CF07F2AE0000CF07F07205082A04721008343508083A7218082AAE0000CF0836AE4FFF5CCF083835A007EECDD7CD55017B080355017C0804AE0000CF0805AE05A0CF080735020809350E080A721408345FCF083DCF083F7217083435550832725F0835CCD82BBE6FCF0820BE71CF0822BEECCF08245500AB0826B66AA403C70827720100E50472140827720300E50472160827720500E504721808277207006404721A0827720100AF04721C08277205006A04721E08275500650828721108285500660829AE0820CF082FCF082BAE0829CF082D350A083A55017B080355017C0804AE0000CF0805AE0001CF08073503080955083A080A721408345FCF083DCF083F7217083435550832725F0835CCD82BAE1000CF082FCF082BAE107FCF082D3508083A55017B080355017C0804AE0000CF0805AE0001CF0807350408093580080A721408345FCF083DCF083F7217083435550832725F0835CCD82BAE04371C0002F6A1AA266FAE04371C0043F6A1662664AE04371C0081F6A1CC26595FCF083DCF083FAE04373F60F6CDB7265C3C60B660A18225F3F6B7505CF6B7515CF6B7575CF6B758BE57C3083F262ABE50C3083D2623355507E2AE04375C5CCF07E5AE1000CF07E3CDB74B35F1080B353D080C721A0834200835F1080B353E080CAE080BCF082FCF082BAE080CCF082D3502083A7216083435550832725F0835CCD82B35F1080B3503080C72100862AE080BCF082FCF082BAE080CCF082D3502083A7216083435550832725F08353505086BCCD82B7218082A721A082A7207086203CCD44A7202086250C60835A1492703CCD4BCAE04375C5CF6C70863725F086435F1080B3507080C721208625FCF0865CF0867AE080BCF082FCF082BAE080CCF082D3502083A7216083435550832725F08353505086BCCD82B7204086203CCD82B721508625FCF083DCF083FAE04373F60F6CDB7265C3C60B660A18025F3F6B7505CF6B7515CF6B7575CF6B758BE57C3083F2659BE50C3083D265290CE0865CE086772BB083F2402905C72B9083D90CF0865CF086735AA07E2AE0437CF07E5C60864AE0080421C0000CF07E3CDB74B725C0864C60864C1086326047216086235F1080B353D080C3505086B201035F1080B353E080C725F0862725F0835AE080BCF082FCF082BAE080CCF082D3502083A7216083435550832725F0835CCD82B720808626DC60835A14A2666AE04375C5CF6B7505CF6B7515CF6B7575CF6B758BE57C308672619BE50C30865261235F1080B353D080C721808623505086B201035F1080B353E080C725F0862725F0835AE080BCF082FCF082BAE080CCF082D3502083A7216083435550832725F0835CCD82BCCD82BCDB85B725F086435AA07E2AE06E2CF07E5C60864AE0080421C0000CF07E3CDB74B725C0864C60864A10026DB35F1080B353D080C725F07F6725F07F7725F07F8725F07F9AE06E2CF07F2AE5000CF07F05507F707EBCDB773AE0762CF07F2AE0000CF07F05507F907EBCDB773725F085F200835F1080B353E080CAE080BCF082FCF082BAE080CCF082D3502083A7216083435550832725F0835CCD82BCCD82BCCD82BCCD82BCCD82B5FCF083DCF083FAE04373F603506005FCDB7D5BE57C3083F2634BE50C3083D262DAE04371C0002F6B7505CF6B75190BE5090CF07FA5CF6B7505CF6B75190BE5090CF07FC35F1080B353D080C200835F1080B353E080CAE080BCF082FCF082BAE080CCF082D3502083A7216083435550832725F0835CCD82B5FCF083DCF083FAE04373F60350A005FCDB7D5BE57C3083F2703CCD693BE50C3083D26F690AE043772A9000290F6B75FAE1287CDA89335AA50E0905C90F6B75FAE1288CDA89335AA50E0905C90F6B75FAE1289CDA89335AA50E0905C90F6B75FAE128ACDA89335AA50E0905C90F6B75FAE128BCDA89335AA50E0905C90F6B75FAE128CCDA89335AA50E0905C90F6B75FAE128DCDA89335AA50E0905C90F6B75FAE128ECDA89335AA50E035F1080B353D080C200835F1080B353E080CAE080BCF082FCF082BAE080CCF082D3502083A7216083435550832725F0835CCD82BAE1287CF082FCF082BAE128FCF082D3508083A55017B080355017C0804AE0000CF0805AE0001CF08073506080955083A080A721408345FCF083DCF083F7217083435550832725F0835CCD82BAE07FACF082FCF082BAE07FECF082D3504083A55017B080355017C0804AE0000CF0805AE0001CF08073507080955083A080A721408345FCF083DCF083F7217083435550832725F0835CCD82B5FCF083DCF083FAE04373F603508005FCDB7D5BE57C3083F2635BE50C3083D262EAE04371C0002F6C708A75CF6C708A85CF6C708A95CF6C708AA5CF6C708AB5CF6C708AC5C35F1080B353D080C200835F1080B353E080CAE080BCF082FCF082BAE080CCF082D3502083A7216083435550832725F0835205ECE07F23F50357E005172BB0050C607ECF735AA07E2C607EBAE00804272BB07F0CF07E3CE07F2CF07E5CDB74BCDB796C607EB725D07EC27014CAE00804272BB07F0CF082BCF082F72100834C308362609CE08387211083420015ACF082D81725D08322611720908620435AA0869720B0834039D20F89DB668A102240FCE1283CF07FACE1285CF07FCCCDBB0720008A603CCD8F0720208A6F85507F707EB5507F607ECAE06E2CF07F2AE5000CF07F0CE07F23F50357E005172BB0050C607ECF735AA07E2C607EBAE00804272BB07F0CF07E3CE07F2CF07E5CDB74B5507F907EB5507F807ECAE0762CF07F2AE0000CF07F0CE07F23F50357E005172BB0050C607ECF735AA07E2C607EBAE00804272BB07F0CF07E3CE07F2CF07E5CDB74BCDB796721208A6720E082A1EC60157A1022708B6ABA146240A200CB6ABA1AA24022004721E082ACCDBB07209082A03CCDBB07200085F31720000E502205BCE07FACF086DCE07FCCF086F35010875BE6FCF08765500AB087A7210085FA60095C6015097CF08602031720000E52CCE08605D27067211085F2020CE07FACF0871CE07FCCF0873BE71CF0878AE086DCF07F4CDDA657211085F20007202085F277200006A022045CE07FACF087BCE07FCCF087D35020883BE6FCF08845500AB08887212085F20257200006A20CE07FACF087FCE07FCCF0881BE71CF0886AE087BCF07F4CDDA657213085F20007204085F26B668A1032645CE07FACF0889CE07FCCF088B35030891BE6FCF08925500AB08967214085F2025B668A103271FCE07FACF088DCE07FCCF088FBE71CF0894AE0889CF07F4AD487215085F20007206085F022039CE07FACF0897CE07FCCF08993504089FBE6FCF08A05500AB08A4CE07FACF089BCE07FCCF089DBE71CF08A2AE0897CF07F4AD087217085F20002047AE0762CF07F2AE0000CF07F035A007EE350E07EF5507F907EB5507F807ECC607EFAE008062C607EF429FB760CDDB2F5507EB07F95507EC07F87201082A087211082A7214082A81720A082A08C607E9CA07EA2703CCDBB0550151006090AE003CCDAA5455005107EA55005007E9BE6FCF07FEBE71CF08005500AB0802AE06E2CF07F2AE07FACF07F4AE5000CF07F0356007EE350907EF5507F707EB5507F607ECC607EFAE008062C607EF429FB760AD1A5507EB07F75507EC07F67201082A087211082A7212082ACCDBB090CE07F4CE07F23F505507EC005172BB0050725F07ED90F6F75C905C725C07ED725C07ECC607ECB160254DCE07F21C007FF64CF75A7F35AA07E2C607EBAE00804272BB07F0CF07E3CE07F2CF07E590BF57CDB74BCDB796725C07EBC607EBC107EE2508725F07EB7210082ACDB77390BE57CE07F2725F07ECC607EDC107EF259681C60869A1AA2703CCDDC89D725D08322703CCDDC89DCD8080AC00DDC89D7200006605720300660A7217006572150065202ECE012DB36F2C02200C72B00131B36F2F0472150065CE012FB36F2F0672160065200C72BB0131B36F2C0472170065CDB6B0CDA7169BCDB35BCDA84F720F00B00E3DCF2612721500073DD02702200C7214000735C800CF356400D0725D0166260672130065204C720E500A0E5501660060CDB44C4551DE4550DD7202006524B6DDBADE260672120065202455016700603D60260435010060CDB44C4551E04550DF2010B6DFBAE026067213006520047211006A721F0062721300067215000672170061720300AF04721600613DEA265A7201006A0A3D05260A721E0062200435A400057200006A0D720000AF06350F009720023F977203006A063D0426062004350400047205006A0A3D02260A721200062004350F0002720100AF0A3D03260A72140006200435D30003720E500A1855014D0060AE003CB66042BF504551EF4550EE1D0001BFF2725A03A72702204D721903A9721B03A9721E00B0CDADC7350A03A635FF03A7721E521172115210350003AC350003AD350003AA35FF00603A6026FCC65217C65219721F5211CDA7CE720D03A908721D03A99D9D20FCC61000A1AA2702203BC61041A16627022032C6107FA1CC27022029C60100A1AA27022020C60141A16627022017C6017FA1CC2702200EC64800A1002607C64808A10127049D9D20FC35FF0416CD9830721500127217001272190012721B0012AC00B89C725C0411C60411A1042514725F041172140012725C0412AE040DA601CDE03FC60412A10A250C725F041272160012725C0413C60413A10A250C725F041372180012725C0414C60414A10A2508725F0414721A001281720400120024314B10AE5014CD98175B014D271CC60415A1332404725C0415C60415A1322612721C0012353304152008725F0415721D001281814FCD9336AE0201CD9355AE0501CD9355AE1401CD9355AE0301CD93558189355550E0350650E135FF50E2725F50E035AA50E05F1F011E011C00011F019C1E01A303E82FF135AA50E08581AE03FACD985BA601CD990EA601CD98CD814B0C4B004B004B00CE040B89CE040989AE5230CD99C95B084B00AE5230CD9A638481CD9563AE507FCD956CAE5089CD956C4B004B304B204B004B004B90AE523189AE0437CDE287BE0A89BE0889AE5089CD95995B0C4B004B004B204B004B084B00AE523189AE0417CDE287BE0A89BE0889AE507FCD95995B0C4B014B80AE5230CD9B87854B014B40AE5230CD9B8785A601CD95E24B01AE507FCD95F0844B01AE5089CD95F08481818920251E01F61E05F1270E1E01F616055F90F024015A0220141E015C1F011E055C1F051E075A1F071E0726D75F5B0281F62707110327045C20F65F81BF0C5A5C7D26FC72B0000C8189520420261F011E091F03F626041E05201F1E01F61E03F127071E055C1F05200A1E015C1F011E035C20DE1E05F626D55F5B06819001BF0D5E42BF0C90014D2709BE0D4272BB000CBF0C9002BE0D3F0E4272BB000D900281BF0A2A06AEFFFFBF08815FBF0881B608F1261AB609E101260CB60AE1022606B60BE10327089C2403A6FF81A60181EB03E70324096C0226056C0126017C81E603BB0BE703E602B90AE702E601B909E701F6B908F781E60388E60288FE891E03B60842BF0CBE087B034272BB000CBF0CBE097B024272BB000CBF0CBE0A7B014272BB000C9FB708BE087B043F094272BB0008BF08BE097B034272BB0008BF08BE0A7B024272BB0008BF08BE097B043F0A4272BB0009BF0924023C08BE0A7B034272BB0009BF0924023C08BE0A7B043F0B4272BB000ABF0A2405BE085CBF085B0481B60BE003B70BB60AE202B70AB609E201B709B608F2B7088188F6B708E601B709E602B70AE603B70B84815204E60388E60288E60188F69688CDE1465B08813F0C4D2A07CDE21A7210000CB6082A07CDE22D7212000C9089F62667E601266390BE08271BE602272EB10822592606E603B109225190AE00204FE706E707205789EE0290BE0A5165BF0A85EF06905FEF0490BF08908581E704E705E706E603906290BF089095B60A9097E60390629001B70AB60B9097E6039062E707909FB70B90858190BE08EF0690BE0A90BF0890AE00104FB70AB70BE705380B390A3909390869076906690549F12537261CE704E605E10125102614E606E1022508260CE607E003240AE6042019E704E607E003E707E606E202E706E605E201E705E604F23C0B905A26B39085E704817363016302600326096C0226056C0126017C8133083309330A300B260A3C0A26063C0926023C08814D270B34083609360A360B4A26F58188B608F7B609E701B60AE702B60BE703848190F6F7271E90E601E701271790E602E702271090E603E70327091C000472A9000420DD81BF0A3F093F088188BF0C90BF0F909F42BF0AB60C97B60F42BF084D271097B60D270B4272BB0009BF0924023C08B60C271097B610270B4272BB0009BF0924023C0884818000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"

//            conexionTrefp2.Updatefirmware(
//                firmwareString,
//                object : ConexionTrefp.MyCallback {
//                    override fun onSuccess(result: Boolean): Boolean {
//                        Log.d(
//                            "MyAsyncTaskUpdateFirmware", "onSuccess " +
//                                    result.toString()
//                        )
//                        if (result) {
//
//                        }
//                        return result
//                    }
//
//                    override fun onError(error: String) {
//                        Log.d(
//                            "MyAsyncTaskUpdateFirmware", "onError " +
//                                    error
//                        )
//                    }
//
//                    override fun getInfo(data: MutableList<String>?) {
//                        Log.d(
//                            "MyAsyncTaskUpdateFirmware",
//                            "getInfo" + data.toString()
//                        )
//                    }
//
//                    override fun onProgress(progress: String): String {
//                        when (progress) {
//                            "Iniciando" -> {
//
//
//                                runOnUiThread {
//                                    progressDialog2.show()
//                                    progressDialog2.updateText(
//                                        "Actualizando versión " + sp?.getString(
//                                            "numversion",
//                                            ""
//                                        ) + " \n a versión "
//                                    )
//                                }
//                            }
//
//                            "Realizando" -> {
//
////                                                if (progressDialog2.returnprogressDialog2() != null && progressDialog2.returnISshowing()) {
////                                                    // progressDialog.dismiss()
////                                                    progressDialog2.dismiss()
////                                                }
//                            }
//
//                            "Finalizando" -> {
//                                runOnUiThread {
//                                    progressDialog2.dismiss()
//                                }
//
//                            }
//                        }
//
//                        Log.d(
//                            "MyAsyncTaskUpdateFirmware",
//                            "progress : $progress"
//                        )
//
//                        return progress
//                    }
//                })

        }
        binding.imageView2.setOnClickListener {
            getDeviceLocation { latitude, longitude ->
                //  String.format("%.4f", latitude).toFloat().toString()
                var la = latitude
                var lo = longitude

                binding.Textlatitud.setText(latitude.toString())
                binding.TextAltitud.setText(longitude.toString())
            }

            /*          getDeviceLocation { latitude, longitude ->
                          //  String.format("%.4f", latitude).toFloat().toString()
                          var la = latitude
                          var lo = longitude

                          binding.Textlatitud.setText(latitude.toString())
                          binding.TextAltitud.setText(longitude.toString())

                          Log.d("geo ", "latitude $latitude longitude $longitude")
                          /*   conexionTrefp2.LoGe( latitude.toDouble()/*-21.51f*/,
                                 longitude.toDouble(),object : ConexionTrefp.MyCallback{
                                     override fun onSuccess(result: Boolean): Boolean {
                                         Log.d("MyAsyncTaskSendXYPRueba", "resultado ${result}")
                                         //   MakeToast("resultado $result")
                                         return result
                                     }

                                     override fun onError(error: String) {
                                         Log.d("MyAsyncTaskSendXYPRueba", "error ${error}")
                                         //  MakeToast("resultado $error")
                                     }

                                     override fun getInfo(data: MutableList<String>?) {
                                         binding.TextResultado.text = data.toString()
                                         //  GlobalTools.checkChecksum(data)
                                         //  data?.map { Log.d("MyAsyncTaskGeEventTREFPPRUEBA", it + "  ") }
                                         data?.let {
                                             Log.d(
                                                 "MyAsyncTaskSendXYPRueba",
                                                 "$it"
                                             )
                                         } ?:{
                                             Log.d(
                                                 "MyAsyncTaskSendXYPRueba",
                                                 "sin datos"
                                             )
                                         }



                                     }


                                     override fun onProgress(progress: String): String {
                                         when (progress) {
                                             "Iniciando" -> {
                                                 binding.TextResultado.text = progress
                                             }

                                             "Realizando" -> {
                                                 binding.TextResultado.text = progress

                                             }

                                             "Finalizado" -> {
                                                 binding.TextResultado.text = progress
                                             }

                                             else -> {

                                             }
                                         }

                                         Log.d("MyAsyncTaskSendXYPRueba", "progress ${progress}")
                                         return progress
                                     }

                                 })
                             */

                          /*                val Latitud = latitude.toString().let {
                                              if (it.first().uppercase().equals("F")) {conexionTrefp2. getNegativeTempfloat(
                                                  "FFFF${it}"
                                              ).toString()} else {
                                                  conexionTrefp2.getDecimalFloat(it).toString()

                                              }
                                          } ?: "00.000000f"
                                          val Longitud = longitude.toString().let {
                                              if (it.first().uppercase().equals("F")) {
                                                  conexionTrefp2. getNegativeTempfloat(
                                                      "FFFF${it}"
                                                  )
                                              } else {
                                                  conexionTrefp2.getDecimalFloat(it).toString()

                                              }
                                          }
                          */
                          //  MakeToast("Latitud $latitude Longitud $longitude   ${latitude.toFloat()}")
                          /*  MakeToast("latitud ${String.format("%.4f", latitude).toFloat().toString()}" +
                                    " longitud ${String.format("%.4f", longitude).toFloat().toString()}")
                          */
                          /*   conexionTrefp2.MyAsyncTaskSendXY(
                                 latitude/*-21.51f*/,
                                 longitude,
                                 object : ConexionTrefp.MyCallback {

                                     override fun onSuccess(result: Boolean): Boolean {
                                         Log.d("MyAsyncTaskSendXYPRueba", "resultado ${result}")

                                         return result
                                     }

                                     override fun onError(error: String) {
                                         Log.d("MyAsyncTaskSendXYPRueba", "error ${error}")
                                     }

                                     override fun getInfo(data: MutableList<String>?) {

                                         binding.TextResultado.text = data.toString()
                                         //  GlobalTools.checkChecksum(data)
                                         //  data?.map { Log.d("MyAsyncTaskGeEventTREFPPRUEBA", it + "  ") }
                                         data?.map {
                                             /*val hexString =
                                                 it.substring(0, 8) // valor hexadecimal que se desea convertir
                                             val hexString2 = it.substring(8, 16)
                                             */
                                             Log.d(
                                                 "MyAsyncTaskSendXYPRueba",
                                                 "$it"
                                             )
                                         }


                                     }


                                     override fun onProgress(progress: String): String {
                                         when (progress) {
                                             "Iniciando" -> {
                                                 binding.TextResultado.text = progress
                                             }

                                             "Realizando" -> {
                                                 binding.TextResultado.text = progress

                                             }

                                             "Finalizado" -> {
                                                 binding.TextResultado.text = progress
                                             }

                                             else -> {

                                             }
                                         }

                                         Log.d("MyAsyncTaskSendXYPRueba", "progress ${progress}")
                                         return progress
                                     }

                                 }).execute()
                             */

                      }
          */
            /*conexionTrefp2.getPlantillacommand(object : ConexionTrefp.MyCallback {

                override fun onSuccess(result: Boolean): Boolean {
                    Log.d("getPlantillacommand", "resultado ${result}")
                    return result
                }

                override fun onError(error: String) {
                    Log.d("getPlantillacommand", "error ${error}")
                }

                override fun getInfo(data: MutableList<String>?) {

                    Log.d("getPlantillacommand", data.toString())





                }


                override fun onProgress(progress: String): String {
                    when (progress) {
                        "Iniciando" -> {
                            //    progressDialog2?.second(progress+ " la conexion")
                            //progressDialog2?.secondStop()
                            binding.TextResultado.text = progress

                            //   progressDialog2?.secondStop()
                        }

                        "Realizando" -> {
                            binding.TextResultado.text = progress
                            // progressDialog2?.second(progress+ " la conexion")
                        }

                        "Finalizado" -> {
                            //  binding.TextResultado.text = progress
                            //  progressDialog2?.secondStop()
                            //    progressDialog2?.second(progress+ " la conexion")


                        }
                    }

                    Log.d("getPlantillacommand", "progress ${progress}")
                    return progress
                }

            })
            */
            /*          conexionTrefp2.LoGe( Latitud.toDouble()/*-21.51f*/,
                          Longitud.toDouble(),object : ConexionTrefp.MyCallback{
                          override fun onSuccess(result: Boolean): Boolean {
                              Log.d("MyAsyncTaskSendXYPRueba", "resultado ${result}")
                              //   MakeToast("resultado $result")
                              return result
                          }

                          override fun onError(error: String) {
                              Log.d("MyAsyncTaskSendXYPRueba", "error ${error}")
                              //  MakeToast("resultado $error")
                          }

                          override fun getInfo(data: MutableList<String>?) {
                              binding.TextResultado.text = data.toString()
                              //  GlobalTools.checkChecksum(data)
                              //  data?.map { Log.d("MyAsyncTaskGeEventTREFPPRUEBA", it + "  ") }
                              data?.let {
                                  Log.d(
                                      "MyAsyncTaskSendXYPRueba",
                                      "$it"
                                  )
                              } ?:{
                                  Log.d(
                                      "MyAsyncTaskSendXYPRueba",
                                      "sin datos"
                                  )
                              }



                          }


                          override fun onProgress(progress: String): String {
                              when (progress) {
                                  "Iniciando" -> {
                                      binding.TextResultado.text = progress
                                  }

                                  "Realizando" -> {
                                      binding.TextResultado.text = progress

                                  }

                                  "Finalizado" -> {
                                      binding.TextResultado.text = progress
                                  }

                                  else -> {

                                  }
                              }

                              Log.d("MyAsyncTaskSendXYPRueba", "progress ${progress}")
                              return progress
                          }

                      })
          */
        }

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
        binding.BtnWifi.setOnClickListener {
//            conexionTrefp2.createProgressDialogwifi()
            conexionTrefp2.getInfoWifi(object : ConexionTrefp.MyCallback {
                override fun getInfo(data: MutableList<String>?) {
                    Log.d("getInfoWifi", "data $data")
                    data?.map {

                    }
                }

                override fun onError(error: String) {
                    Log.d("getInfoWifi", "onError ${error.toString()}")
                }

                override fun onProgress(progress: String): String {
                    Log.d("getInfoWifi", "progress $progress")
                    return progress
                }

                override fun onSuccess(result: Boolean): Boolean {
                    Log.d("getInfoWifi", "result $result")
                    return result
                }
            })
        }

        binding.BtnSendWifi.setOnClickListener {

//            binding.TextAltitud.text  setText("ELTEC_apps")
//            binding.Textlatitud.text.toString(),  setText("975318642a")
            conexionTrefp2.MyAsyncTaskSendWifiPrincipal(
                binding.TextUsuario.text.toString(),
                binding.TextPassword.text.toString(),
                object : ConexionTrefp.MyCallback {
                    override fun getInfo(data: MutableList<String>?) {
                        data?.map {
                            Log.d("BtnSendWifi", "data $it")
                        }
                    }

                    override fun onError(error: String) {
                        Log.d("BtnSendWifi", "onError ${error.toString()}")
                    }

                    override fun onProgress(progress: String): String {
                        Log.d("BtnSendWifi", "progress $progress")
                        return progress
                    }

                    override fun onSuccess(result: Boolean): Boolean {
                        Log.d("BtnSendWifi", "result $result")
                        return result
                    }
                })
        }
        binding.BtnSendWifiSecundario.setOnClickListener {
            conexionTrefp2.MyAsyncTaskSendWifiSecundario(
                binding.TextUsuario.text.toString(),
                binding.TextPassword.text.toString(),
                object : ConexionTrefp.MyCallback {
                    override fun getInfo(data: MutableList<String>?) {
                        data?.map {
                            Log.d("BtnSendWifi", "data $it")
                        }
                    }

                    override fun onError(error: String) {
                        Log.d("BtnSendWifi", "onError ${error.toString()}")
                    }

                    override fun onProgress(progress: String): String {
                        Log.d("BtnSendWifi", "progress $progress")
                        return progress
                    }

                    override fun onSuccess(result: Boolean): Boolean {
                        Log.d("BtnSendWifi", "result $result")
                        return result
                    }
                })
        }
        validaCONEXION()
        val BLE =
            conexionTrefp2.bluetoothLeService //  BluetoothService().bluetoothLeService // bluetoothService!!.bluetoothLeService
        conexionTrefp2.bluetoothServices.registerBluetoothServiceListener(this)
        // bluetoothService!!.registerBluetoothServiceListener(this)
    }



    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/plain"
        }
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE)
    }

    private fun readTextFile(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val text = inputStream?.bufferedReader().use { it?.readText() }
            Log.d("FileContent", text ?: "Archivo vacío")

            if (text!!.isNotEmpty()){

                // conexionTrefp2.getInfoWifi(object : ConexionTrefp.MyCallback {
                conexionTrefp2.Updatefirmware(
                  //  text,
                    "820091B68200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA8200E2CA01090A44510000000000000000000000010204081000000008405C00405B00405A00405900405800405700405600405500405400404A004049004046004051004050004053004061004060004021002B4D41433D0041542B4D41430D0A0041542B484F5354454E330D0A0041542B454E4C4F47300D0A0041542B4E414D454345425F494E0D0A0041542B42415544380D0A002B424155443D380041542B424155440D0A00004F4B0D0A0041540D0A0000015180894A620012894B61005089F96101008CC864040091B6000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010001010001C2000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000011111222133314441555166617770000000000000000000000000000211112221333000031113222333334443555366637773888000012345678999901000000000100000028000A000A000000030000000100280003000500F00032005A00DE0005FFBA00960091005F006400000000000000000000000000003C00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000078787878787878787878787800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006600000000000000000000000000000000000000000000000000000000001F0000003B0000005A0000007800000097000000B6000000D5000000F300000111000001300000014E0066AE0FFF9490CE8933AE8935F62725A5602717BF0CEE03BF0FBE0CEE0190F6F75C905C90B30F26F5BE0C909390EE031C000520D8AE00002002F75CA3001226F9AE08EE2002F75CA308F226F9CDA51920FE351150C2725F50C6725F50C1725F50CB350150C8725F50C9350350C0725F50C3358050C4725F50D0725F50CA725F50C5725F50CD725F50CC725F50CE35B950CF814D2706721050C22004721150C28135AC50CE353550CEC750CD814D2706721450C22004721550C281721150C6721950C6CA50C6C750C681721550C6721B50C6CA50C6C750C681721050CA814D2706721850CA2004721950CA81899F1A01C750C58581C750C881C650C7815209AE00001F07AE00001F05C650C76B097B09A101260CAE24001F07AE00F41F0520227B09A102260CAE94701F07AE00001F0520107B09A104260AAE24001F07AE00F41F05C650C0A4076B097B095F97D688906B097B09B70B3F0A3F093F08961C0001CDE251961C0005CDE109961C0001CDE11B961C0005CDE251961C0005CDE1095B0981C750C0814D2706721250C92004721350C981899F1A01C750C18581C750CB8189889EA4F06B010D0126350D0327197B02A40F5F97A6015D2704485A26FCCA50C3C750C3AC0093FF7B02A40F5F97A6015D2704485A26FC43C450C3C750C3206A7B01A11026330D0327177B02A40F5F97A6015D2704485A26FCCA50C4C750C420497B02A40F5F97A6015D2704485A26FC43C450C4C750C420310D0327177B02A40F5F97A6015D2704485A26FCCA50D0C750D020167B02A40F5F97A6015D2704485A26FC43C450D0C750D05B038172105190817212519081899F4D27099ECA50C2C750C220097B0143C450C2C750C285814D2706721350CF2004721250CF81899F4D271D9EA11C2606721450C9202E7B01A12C2606721451902022721450CA201C7B01A11C2606721550C920107B01A12C2606721551902004721550CA85818889A4F06B027B03A40F6B010D022607C650C16B0220607B02A1102607C650C26B0220537B02A1202607C650C56B0220467B02A1302607C650C66B0220397B02A1402607C650C96B02202C7B02A1502607C650CA6B02201F7B02A1702607C650CF6B0220127B02A1802607C651906B022005C650CB6B027B015F97A6015D2704485A26FC14022706A6016B0220020F027B025B038172175190818888A11C2611C450C9A10C2606A6016B01202E0F01202A7B02A12C2613C651901402A10C2606A6016B0120150F012011C650CA1402A10C2606A6016B0120020F017B018581A4F0A1202606721751902004721750C9817211507035FC50708189F6A4FEF77F6F02A350932608A640E7036F0520061E01A652E7031E016F041E016F061E016F071E016F01858189F6A4FEF77F7B0C1A0D1A0EFAF7E601A4C7E7017B0F1A10EA01E7017B0B1E01E7027B091E01E7037B0A1E01E7041E01A3509326067B061E01E7057B071E01E7067B081E01E70785814D27067210507020047211507081890D052706F6AA01F720061E01F6A4FEF78581725F50704848C7507081897B051E01E7028581E60281890D062706F61A05F720071E017B0543F4F78581895204C650706B04C650716B01019FA40F974F025D27367B05A5012707AE50751F02201F7B05A5022707AE507F1F0220127B05A5042707AE50891F022005AE50931F021E02E60114066B0420147B05A51027087B0614016B0420067B04A4026B047B045B06818989019FA401974F025D2707AE50751F01201F7B03A5022707AE507F1F0120127B03A5042707AE50891F012005AE50931F011E017B04A40643E401E7015B0481885204A5102707AE50751F03201F7B05A5202707AE507F1F0320127B05A5402707AE50891F032005AE50931F031E03E6016B011E037B01F46B017B05A4066B027B0114026B027B025B05818889A5102707AE50751F01201F7B03A5202707AE507F1F0120127B03A5402707AE50891F012005AE50931F011E017B03A40643E401E7015B03816F047F6F026F0381897B0543E404E7047B06A580271D7B06A5102706F61A05F720071E017B0543F4F71E01E6021A05E70220091E017B0543E402E7027B06A540270A1E01E6031A05E70320091E017B0543E403E7037B06A520270A1E01E6041A05E70420091E017B0543E404E7048581890D062708E6031A05E70320091E017B0543E403E7038581897B051E01F78581890D062706F61A05F720071E017B0543F4F7858189F61A05F78581897B0543F4F7858189F61805F78581E60181F68189E6011405858189F614058581C750E081C750E181C750E28135AA50E08135CC50E081725F52E0725F52E1725F52E2725F52E4725F52E7725F52E835FF52E9725F52E5819FC752E99EC752E8350152E6819EC752E89FA1012606721052E62004721152E681C752E781C752E98188C652E76B017B015B0181C652E8814D2706721252E02004721352E081A1012606721452E02004721552E0814D2706721E52E02004721F52E081A1012606721652E02004721752E0814D2706721052E02004721152E081899F4D27099ECA52E4C752E420097B0143C452E4C752E48581CA52E6C752E68188C452E52706A6016B0120020F017B015B018143C752E5818889C452E56B01C652E414036B020D01270A0D022706A6016B0220020F027B025B038143C752E581899F4D27099ECA52E3C752E320097B0143C452E3C752E38581C652E2A4F8C752E2818888C652E26B017B01A48F6B017B011A026B017B01C752E285818888C652E16B017B01A48F6B017B011A026B017B01C752E185818888C652E26B017B01A4F86B017B011A026B017B01C752E285814D2706721E52E22004721F52E281F6E6016F036F026F046F056F066F0781895204E604A4E9E7047B0D1A0FEA04E704E606A4CFE706E6061A0EE7066F02E603A40FE703E603A4F0E703CD92B1961C0009CDE11B961C0001CDE2517B03A4F01E05E7031E057B04A40FEA03E703961C0001CDE109A604CDE242B60B1E05E7021E05E605A4F3E7051E05E6051A10E7055B068189E606A4F8E7067B061A071A08EA06E7060D052708E606AA08E70620081E01E606A4F7E7068581890D052708E604A4DFE70420081E01E604AA20E7048581897B051E01E70A8581E605AA01E70581E601818989E6045FA4805F02581F011E03E6015F97011A02011A010101A4FF01A401015B0481897B051E01E701858189E604A4BFE704160590549054909FA440EA04E7047B061E01E7018581890D052708E605AA02E70520081E01E605A4FDE705858189E607A4F0E707E6071A05E707858189E604A4F7E704E6041A05E7048581890D052708E608AA08E70820081E01E608A4F7E7088581890D052708E608AA20E70820081E01E608A4DFE7088581890D052708E608AA10E70820081E01E608A4EFE7088581897B051E01E7098581890D052708E608AA04E70820081E01E608A4FBE7088581890D052708E608AA02E70820081E01E608A4FDE7088581890D062708E6081A05E70820091E017B0543E408E708858189897B076B017B08A40F5F97A6015D2704485A26FC6B020D09272A7B01A101260A1E03E6041A02E70420457B01A105260A1E03E6081A02E70820351E03E6051A02E705202B7B01A101260B1E037B0243E404E704201A7B01A105260B1E037B0243E408E70820091E037B0243E405E7055B048189881E06A3010126121E02E60515072706A6016B0120130F01200F1E02F615072706A6016B0120020F017B015B0381897B06431E01F785818952037B09A40F5F97A6015D2704485A26FC6B027B094EA40F6B037B035F97A6015D2704485A26FC6B031E08A30100261D1E04E60414036B031E04F61502270A0D032706A6016B03204F0F03204B1E08A3023526291E04E60514036B031E04E608A4016B011E04F61502270E0D0326040D012706A6016B03201F0F03201B1E04E60514036B031E04F61502270A0D032706A6016B0320020F037B035B0581F6A4BFF781725D10612604AD242021720A001200241A350204084B004B08AE5014CD97E8854B014B20AE5014CD97E885814B004B20AE5014CD97E885720A00120024164B004B08AE5014CD97E8854B004B20AE5014CD97E885CE04045A27285A2603CC9E795A2603CC9EBB5A2603CC9EFD5A2603CC9F515A2603CC9F7E5A2603CC9FABAC009FE59DAE03E889AE0000895F89CE040489AE892589AE892ACDA39B5B0AC30404260E72110012AE0002CF0404AC009FE572000012002403CC9FE5CE04061C0001CF0406CE04065A27265A27345A27425A27505A275E5A276C5A277A5A2603CC9E535A2603CC9E645FCF0406AC009FE5AE04B089AE000089CD9FE65B04AC009FE5AE096089AE000089CD9FE65B04AC009FE5AE12C089AE000089CD9FE65B04AC009FE5AE258089AE000089CD9FE65B04AC009FE5AE4B0089AE000089CD9FE65B04AC009FE5AE960089AE000089CD9FE65B04AC009FE5AEE10089AE000089CD9FE65B04AC009FE5AEC20089AE000189CD9FE65B04AC009FE5AE840089AE000389CD9FE65B04AC009FE5AC009FE59DAE07D089AE0000895F89CE040489AE892489AE8924CDA39B5B0AC30404260E72110012AE0003CF0404AC009FE572000012002403CC9FE5AE0003CF0404AC009FE59DAE0FA089AE0000895F89CE040489AE891289AE891ACDA39B5B0AC30404260E72110012AE0005CF0404AC009FE572000012002403CC9FE5AE0004CF0404AC009FE5AE0BB889AE0000895F89CE040489AE891289AE8907CDA39B5B0AC30404262172110012AE0001CF0404AE0008CF0406AEC20089AE000189CD9FE65B04AC009FE572000012002403CC9FE5AE0001CF0404AC009FE5AE07D089AE0000895F89CE040489AE892589AE88F7CDA39B5B0AC304042702207372110012AE0006CF04042067AE07D089AE0000895F89CE040489AE892589AE88EBCDA39B5B0AC304042702204672110012AE0007CF0404203AAE07D089AE0000895F89CE040489AE892589AE88DECDA39B5B0AC30404270220197211001235020408AE0001CF04049D3566005FAE1061CDA893814B00AE5230CD9A63844B0C4B004B004B001E09891E0989AE5230CD99C95B084B01AE5230CD9A63848152034B004B20AE5014CD97E8854B004B08AE5014CD97E885CE04045A27055A2752205F9DAE07D089AE0000895F89CE040489AE88CF89AE88D5CDA39B5B0AC30404263F4B3DAE04F3CDDFA1841F010F037B035F977B03905F909772F90190E601D7080D0C037B03A10C25E5AE0002CF0404200F350304084B014B20AE5014CD97E8855B03814B014B20AE5014CD97E8854B004B08AE5014CD97E885CDA3E272020012002503CCA28672130012720308620472140862AE000289AE88CC89AE04F3CDDF715B04A30000260435210835AE000289AE88C989AE04F3CDDF715B04A30000260435600835AE000289AE88C689AE04F3CDDF715B04A30000260435610835AE000289AE88C389AE04F3CDDF715B04A30000260435530835AE000289AE88C089AE04F3CDDF715B04A30000260435500835AE000289AE88BD89AE04F3CDDF715B04A30000260435510835AE000289AE88BA89AE04F3CDDF715B04A30000260435460835AE000289AE88B789AE04F3CDDF715B04A30000260435490835AE000289AE88B489AE04F3CDDF715B04A300002604354A0835AE000289AE88B189AE04F3CDDF715B04A30000260435540835AE000289AE88AE89AE04F3CDDF715B04A30000260435550835AE000289AE88AB89AE04F3CDDF715B04A30000260435560835AE000289AE88A889AE04F3CDDF715B04A30000260435570835AE000289AE88A589AE04F3CDDF715B04A30000260435580835AE000289AE88A289AE04F3CDDF715B04A30000260435590835AE000289AE889F89AE04F3CDDF715B04A300002604355A0835AE000289AE889C89AE04F3CDDF715B04A300002604355B0835AE000289AE889989AE04F3CDDF715B04A300002604355C0835CDA512AE08EECDE0F1AE8895CDE01F2403CCA39A720C0012002503CCA39ACDA512AE08EECDE251C60832A1552703CCA3859D7204083402202190AE0803AE084190F6F75C905CCDB72690A3080A23F13508083C72150834CCA38572010834057203083449CE082FC3082D2541720608342690AE083DAE0841725F083190F6F75C905C725C0831C60831A10425EF3504083C721608342068725F083272130834721108347219082A721B082A205290CE082FAE0841725F083190F6720008620F7208082A05720B082A0590AF010000F75C905C725C0831CDB726720108340E90C30838260890CE083672120834C60831C1083A25C490CF082F55083A083C2000C60832A155260E9DC6083C5F9789AE0841CDA4E2858189897200001200251272100012CDA512AE0400CDE2511E03CDA484AD2A1E07891E0D891E0DCDA45B5B041F01CDA512AE0400CDE0F1961C000DCDE01F2504721100121E015B04815206A6906B03AE5089CD96166B047B0411032462C65230A510275BC652305F97C652315F97AE5089CD96166B045F1F05200F1E05D60437D704F31E051C00011F05A60097A690100424015A02130522E21E05724F04F34B00AE5089CD95F0847B0388AE5089CD960D844B01AE5089CD95F084721200125B068189895F1F0172020012002418721300121E0989AE04F3CDDFB95B02A3000027041E031F011E015B04818989CDDFAD1F01AE04171603CDE2634B00AE507FCD95F0847B0288AE507FCD960D844B01AE507FCD95F0845B048189BF0C1E055D270A5A92D60CD704175D26F64B00AE507FCD95F0847B0688AE507FCD960D844B01AE507FCD95F084858189BF0C1E055D270A5A92D60CD704175D26F64B00AE507FCD95F0847B0688AE507FCD960D844B01AE507FCD95F0848581AE040DCDE109819D550000005F555000005F558000005F558880005F558881005F3501086CCDA7CECDA71635000061350000073500006235000063350000063581006435020054358100604560A6357800A7350003BD721003A8721003A9721403A8721703A9350A03A6350003AA350003AC350003AD350203AF350503B0350803A635FF03A7721903A9721B03A9721D03A93FFB721903A83504000435D3000335A40005350F00023F0135010000AE1057CDA8D955005F0157356E00ABC60157A102260435D200AB721000B0721300B0721700B0359600C6359600C7359600C8359600C935AA01003566014135CC017F55104800D7CDB6EDCDB700CE1283CF07FACE1285CF07FC725F07E9725F07EA725F07EB725F07EC725F07ED7216085FAE50001C007FAF010000B7603F5FA37FFF26043F5F200D3C5F1C0080AF010000B16027EA55005F07F7C607F7AE0080421C50001C007EAF010000C707F6AE06E2CF07F2AE5000CF07F05507F707EBCDB773AE00001C007FAF010000B7603F5FA34FFF26043F5F200D3C5F1C0080AF010000B16027EA55005F07F9C607F9AE0080421C00001C007EAF010000C707F8AE0762CF07F2AE0000CF07F05507F907EBCDB773CCB88281BD9298ACC8C09D808884E0C3B0C2C6F0E3E2A4F780AFF5DAF4AEDADAF486FFE284E3C3C686FEA1F68EFFFFFFFFFFF1E284E3C3C6FFFFFFFFFFFFFFFFFFFFFF350150C2350050C6350050C8350050C5350050C0350050C935FF50C335FF50C435FF50D0352053403583534135605342350053433580534AA600C7534BC7534CC7534DC7534EC7534FC753513538535035FF500235FF500335005004359F5007359F50083500500935F8500C35F8500D3500500E7216500A35EF501135EF50123500501335EE501635EE50173500501835FF501B35FF501C3500501D350452E835FA52E9350152E0A655C750E0A602C750E1A6FFC750E281351052123590521B3501521C3511521D3500521A3500521335005214350052173500521835005219350052163501521035045211819093FE5D27035A90FF81F6B7604D27053A60B660F7813F5FB679BA7A26047210005FBE6FC301232F047212005FC60159A101270BBE71C301212F047218005F7201006404721E005F455F5B8190AE0100909FBBFD9097AE10009FBBFD97B6FCA1AA261A90F6B75F90AE031E909FBBFD909790F6B15F270CB75FAD152006AD57B65F90F73CFDB6FDA18025043FFC3FFD817206505410A6AEC75053A656C750539D72075054FAB65FF7720052E503CCB591721152E53C5F720300060B7201005F0672125014200472135014720450540220D77217505481F6B75F813D682603CCA9F1B668A1012704721B00673D6626063D65260220E9720000AD3B721A00673513005F7201006602200B720200660220283513005F455F58CDAA45CDB6EDCDB70045585F721100677213006735010060455F5A456059CCA9F17208006505720B006508351300603503005F72040066057207006608351300603502005F7205006508351F00603520005F7207006508351F0060350F005F7209006608351F0060351E005F720D006508351F0060350A005F7203006508351F0060350C005F720F006608351F00603527005F720D006608352600603527005F7201006508350E0060352F005FB668A1012709B655C4015A2702201B200D720C005608351F005F351F006045605A455F59B667A4FCB767819D72015343FAC65344C6534535235340725F53439D72015343FA555344008555534500868138609FBB6097B686F75CB685F7813508005F905FF6B7515CF6B7505C72B900503A5F26F090549054905490BF508181A63C97C60149429FB78A9EB7898190BF50B6509097B660904290BF5CB6519097B6609042909FB751909EBB5DB75081550146006090AE0E10ADD445517645507590BE50ADB0550156006090BE50ADBF4551824550818145556045565F385F3960385F396081BE6BBF57CDB3C581BE6DBF57BE6BBF5FCDB442CDB3D1815501650060CDB44C45518445508381457060456F5F81720403A803CCAB6F721503A8720903A821AE00F490AE00FA35000060350A005F350103B3350003B4350A03B5350003B6204D720103A821AE03C390AE03C5350900603507005F350103B3350103B4350703B5350903B62027721F00B0721303A9AE100090AE107F35800060358C005F350C03B3350A03B4350F03B5350F03B6CF039E90CF03A055005F03B9725C03B955006003BACDABFAC603B3A101271D720703A918350103B3350A03B4350403B5350F03B6350003B7721203A9725C03B7C603B7C103B3262C721203A9725C03B8C603B8C103B42604721403A95503B803BC725A03BCC603BC48484848CB03B6C703BC2004350003BC5503B703BB725A03BBC603BB48484848CB03B5C703BB5503B503A25503B603A3C603B6A003C703A4AD328190CE039EAE018090C303A0220890F6F75C905C20F2720003A816350C005F90AE080D90F6F73A5F3D5F27055C905C20F281725F03B2725F03C15503BB0060CDADB6B660A003C703C297B65F42909372B903C1905ACF03C1AE018072BB03C1CF039E90CF03C190AE018072B903C190CF03A0725F03C1720003A806358C03C22004350703C2AE018072BB03C15A90CF03C1C303C12403CF03A090CE039EAE025E45605F3A5FC603BBAD30C603BCAD2B720903A906A637AD22201690C303A0220A90F6905CAD1426F22006A600AD0C26FA5503B203BDC603B2F781F7CB03B2C703B25C3A5F81AE025E9FCB03AC97F6C75216725C03AC81AE026E9FCB03AD97C65216F7B760C603A34A4AC103AD24022010B660A1002704721303A8CB03B2C703B2725C03AD81725F03C15503BC0060CDADB6B660A001C703C297B65F42909372B903C1905ACF03C1AE027E72BB03C1CF039E90CF03C190AE027E72B903C190CF03A0725F03C1720003A806358C03C22004350703C2AE027E72BB03C15A90CF03C1C303C12403CF03A090CE039EAE026E45605F3A5F90C303A0220CF690F75C905C3A5F26F020019D81CE039E90AE027EC303A0220890F6F75C905C20F381B660A4F044444444B75FB660A40FB76081A600AE03C7F75CA303CE220220F781350203BF35F003C035F003C0725A03BF2708725A03C027F020F8813508005F3F513F50905FF6B7513F5072B900505C3A5F26F2909FB751909EB75034503651345036513450365181720300CD25721200B0351400953FBF3FB93FE8AE100090AE031ECDA8D9B65F90F75C905C90A3039D23F0720200B003CCB35A3DB12704351400953D952603CCB346720500B00B720800B003CCB1CBCCB1A8720700B003CCAF8C721300B1721B00B1B6B9350900C2350000C4CDAFC74560B9A60FB4B9AE000242DCAE9A201E2026202E2036203E2046204E2056205E2066206E206C206A2068206620643505005A352500592062350D005A350F005920583520005A350A0059204E3520005A350B005920443500005A350F0059203A3525005A350500592030350F005A352700592026350F005A350A0059201C3505005A350E00592012351F005A350E005920083526005A352600597211006772130067720500B156721600B0B6B9A1092603CCB346A1082645720100E804721000E9720300E82C721300E8550357031F5503580320550359032155035A032255035B034B55035C034C55035D034D55035E034E350000FD35AA00FCCCB346CCB35AA60FB4B9AE000342DCAF97CCB049CCB062CCB07BCCB09ECCB0BDCCB0EDCCB112CCB122CCB151CCB15BCCB165CCB165CCB165CCB165CCB165CCB165721C00B05F973FC13FC3720000B125720800B1413DD1261A351900D13D962604350A00D1720200B10B720A00B127350500962044B3C12F04BEC32038720C00B00AA300642E0AA3FF9C2F051C000120241C000A201FB3C32C04BEC12017720C00B00AA300642C0AA3FF9C2D051D000120031D000A351E0095BF57721D00B045586081CE032DBFC1CE032BBFC3CE031FCDAFD1CF031FCDB3C5CCB16DAE0063BFC1AE0000BFC3CE0321CDAFD1CF0321CDB3C5CCB16DCE032D1C0096BFC1CE031F72BB03211C000ABFC3CE034BCDAFD1CF034BCDB3C5CCB16DCE031F1D000ABFC1CE032B1D0096BFC3CE034DCDAFD1CF034DCDB3C5CCB16D720000B107720800B1022004901000E83528005A35010059720100E8083500005A351D00597211006772130067CCB16D3525005A35050059356300C2350000C4B6BFCDAFC74560BFCDB41E7211006772130067205B55107B005ACE107CA60A629FB759204B720000B107720800B1022004901200E83528005A35010059720300E8083500005A351D00597211006772130067201C3505005A350E00592012351F005A350E005920083526005A35260059720500B133721700B0B6B9A1052629B6BFC101732622721400B03FB8721700E8720100B004721600E8721900E8C60152A1202604721800E8CCB35AA607B4B8AE000342DCB1B3CCB292CCB2C1CCB2F0CCB31FCCB329CCB333CCB333CCB333721300B1721B00B1B6B8350400C2350000C4CDAFC74560B8A607B4B8AE000242DCB1EE200E2016201E2026202E2036203420323500005A350F005920303511005A3527005920263527005A351D0059201C3505005A350E00592012351F005A350E005920083526005A352600597211006772130067720500B14A721800B0B6B8A1042603CCB346A1032639721100B0720700E804721000B0720500E804357800EA35000370720900E80435200370C60370C11052270B550370005FAE1052CDA893CCB346CCB35A720000B107720800B1022004901400E83528005A35010059720500E8083500005A351D00597211006772130067207A720000B107720800B1022004901600E83500005A350F0059720700E8083500005A351D00597211006772130067204B720000B107720800B1022004901800E83529005A350C0059720900E8083529005A350F00597211006772130067201C3505005A350E00592012351F005A350E005920083526005A35260059720500B104721900B02014721300B0721700B0721B00B0721500B0721900B081720000AE022022720200AE02201B720600AE0E350000FD35AA00FC721600AE20083DFC2604721400AE81BE73BF57CE0105BF5FCDB439456058455F57C60152A1202627BE572A1550A60262A61242A60562BF57AE014072B00057200CA60262A61242A605621C0140BF57CDA9DDBE572A055072120067A60A62B7589FB757B657720300671335230060720300B00435640060B1602437201135630060720300B00435640060B1602424B657A10A241172100067BE572604721300675EBF59201D455760AD2C455F5A4560592010720A005603CCA9DD3510005A3511005981BE5F72BB0057BF5F81BE5F5072BB0057BF57813F5FBE5FA60A62B7609FB75FAE0006B66042BF50AE003CB65F4272BB0050BF50813F583F5790BE5090BF7BBE7BA30338240EBE7BA302B1241AA300B7242820393534006090BE7BCDAA54BE501D93CCBF572039351E006090BE7BCDAA54BE501D4D00BF5720263514006090BE7BCDAA54BE501D3219BF5720133522006090BE7BCDAA54BE501D3C19BF572000BE5790932A01509EB75FA60A429E5F97BF57B65F97A60A4272BB0057905D2A0150BF57815F905FCE07E390CE07E5725F07E772105051C607E2A1AA262E720250540FA656C75052A6AEC7505272035054F135AA50E090F6A70100005C905C725C07E7C607E7A17F23E82029720650540FA6AEC75053A656C7505372075054EC35AA50E090F6F75C905C725C07E7C607E7A17F23EB3F5F35AA50E0720152E51A721152E53C5F720300060B7201005F067212501420047213501472055054D87213505472175054CCB772B6BBA10224124E484C45BABE38BEBBBEB7BE350300BD200B4EAB22BBBAB7BE350300BD81B6B9A1052336A1092732A140275DA1422759A145277AA1462751A147274DA1302750A1312752A1322754A1332756A1342758A135274EA1362744205AA100271FA103271BAE0063BFC1A102270BA1052707AE0000BFC3203EAEFF9DBFC32037CE032DBFC1CE032BBFC3202BAE0063BFC12024350100C2201E353F00C22018350900C22012350200C2200C350800C22006350100C42000817208015309355000B9721600B081B6B9720000B107720800B11C2032A1012602A602A1042602A605A1062602A627A1282602A6502018A14F2602A627A1262602A605A1042602A603A1012602A600B7B9819D20FDB677A4E04D26F6B683A4F84D262CB68BA4F84D2625B6B4A4FC4D261EB6B6A4FC4D2617BE6DB36B2F11C652E8A0044D2609C652E9A0FA4D2601819D20FD55106F006090AE0168CDAA544551E24550E181551070006090AE0168CDAA544551E44550E38155104B006090AE003CCDAA544551E74550E681BF5090BF5790CE083DCE083F3F5CB75D72BB005C2402905C90CF083DCF083FBE5090BE5781725F07E85F905FA6AAC750E0AEB4FC90AE063BF690F7725C07E85C905CC607E8A1A523EFCC063B8190CE07F2C607EBAE00804272BB07F03F60AF01000090F75C905C3C60B660A18025EF81CE07FABF5045505FAE1283CDA89335AA50E045515FAE1284CDA89335AA50E0CE07FCBF5045505FAE1285CDA89335AA50E045515FAE1286CDA89335AA50E081F6CDB7265C3C60B660B15F25F3F6B7505CF6B7515CF6B7575CF6B758813F5FB660A40FBB5FB75FB6604EA40FAE000A429FBB5FB75F815F5508A70060ADDFB65F971C07D0CF08ADC608A8A41FB760ADCD55005F08AF5508A90060ADC155005F08B05508AA0060ADB555005F08B15508AB0060ADA955005F08B25508AC0060AD9D55005F08B381AE06E27F5CA3076223F9813F607F5C3C603D6026F8813F60F690F75C905C3C60B660A18025F28135060416CDDEC9CDDEEBCDDF704B01AE5230CD9A638435030408725C041620019DA601CD98FB4D27F7A601CD990E9D720500070672145005200472155005720F006206721E500A2004721F500A20007205000606721450142004721550143C52720300060B7201005206721250142004721350143C99B699A1282B023F999D725C0638C60638A1C82307721008A6CCBB7272150600720006000672130600200472120600352353403580534A3500534B3510534C3500534DCDA9F245855F45866090BE5F90A3000A240672110600200472100600C60600A403A1012702200C721406009010500072100001720406000AC6063AA1012711CCBB7EC60601A1002703CCBB723501063AC60601A120260A725F06013500063A2036352353403580534A3500534B3510534C3500534DCDA9F245855F45866090BE5F90549054AE06189FCB060197909FF7725C0601CCBB7E725F0605725F0604725F0603725F06023F5FAE06189FCB060197F6B760BE5FA66442A63C62BF5FBF50B66097B65142CF0609B65F97B65142CF060BB66097B65042CF060DB65F97B65042CF060F55060A0614C60609CB060C3F6024023C60CB060EC7061324023C60B660CB060B3F6024023C60CB060D24023C60CB0610C7061224023C60B660CB060FC70611CE061372BB0604CF06043F5F3F6024023C60BE5F72BB061172BB0602CF0602725C0601C60601A1202703CCB9E1725F0601C60603A11F23022012C60603A11F27022012C60604A180250B2000AEFE01CF0606CCBB7EA605725406027256060372560604725606054A4D26ECCE0604CF06068952041F035F1F01200F1E031F011E0516036572FB03541F031E01130327055C130326E61E035B069FC70639725F0638721108A6721308A6C61282A13C260220615FC606399790AE003C90BF5FB35F2202204D90AE005A90BF5FB35F244235640060550639005F720650540D35AE50533556505372075054FBB660AE1281F772045054FBB65FAE1280F772045054FBA63CAE1282F772045054FB35005054200220FE200C725F0601725F06383500063A7201006A167202000109720100011072120001AE0005CDA80D2004721300017203006A167204000109720100011072140001AE0004CDA80D200472150001720100AF167206000109720100011072160001AE0003CDA80D2004721700017205006A167208000109720100011072180001AE0002CDA80D200472190001721100019DCDDDDF725D086C27254B014B08AE5014CD97E8854B004B20AE5014CD97E8854B00AE5230CD9A63843501040820254B01AE5230CD9A63844B004B08AE5014CD97E885C604084A27084A27224A27242003CD9CEDCDDE34720C001200241C9D720E08340435F0086A721E08342025CDA00F20E1CDA09420DC20DA9D721F0834725F0832725F0834725F08627219082A721B082A720A001200240A9DCE05C31C0001CF05C34B01AE500FCD980B84CDB80BAE0000CF08BAAE0000CF08B8C608AEA5032617C608AFA1032510AE5180BF0AAE0001BF08AE08B8CDE04F725D08AF271CC608AF97A604421D00041C08BCCDE109AE892FCDE066AE08B8CDE04FCE08AD90AE016DCDDFED1DF8CACDE287AE892FCDE066AE08B8CDE04FCE08AD1D07B1545401C708EC02C608ECB70B3F0A3F093F08AE892FCDE066AE08B8CDE04FC608B05F975ACDE011AE892FCDE066AE08B8CDE04FC608B15F9790AE0E10CDE28EAE08B8CDE04FC608B297A63C42CDE011AE08B8CDE04FC608B3AE08B8CDE03FCE08B8CF08B4CE08BACF08B69DC604164D272D4A2603CCC29E4A2603CCC3B14A2603CCC6654A2603CCC68C4A2603CCCC694A2603CCCD764A2603CCDBCC9D20FE9D720300AE1C720400AE0735FF03A7CCC091721100AE721300AE721500AE721700AE725A03AB2703CCC0915503A603ABA607C403AAC703AAA10026022020A1012602207BA1022603CCBE9FA1032603CCBF16CCC299CCC299CCC299CCC2997203521903CCC034720103A925350303A6720903A804A6AA200B720103A804A6552002A65FC703B172145211721103B1200C725F03B2350103A6721003B172105211CDADD67200521703CCC0915503B15216350103AA7203521703CCC041CCC0917202521703CCC0457202521903CCC04135FF03A7720003A906350303AA207E350203AACDAAD8C603ACC103A2270E720E521703CCC041CDACDECCC091720E521703CCC0417204521703CCC04172125211725F03AC350503AB350103A6350003AA35FF03A7720303A931721603A9721303A9721103A9720903A820721903A8350003AA350303A6350A03AB350003B7350003B8721403A8721003A9CCC091C603ADC103A42412720C521703CCC028350703B0CDACEFCCC091C603ADC103A4261A7204521703CCC028350703B072155211CDACEF72125211CDACEF720C521703CCC028350703B0CDACEF350003AA350303A6350503AB35FF03A7725F03AD720303A80220163F5F5503A300603A60AE026E72BB005FF6C103B22711C603AEA1042603CCC041725C03AECCC091725F03AECDAD1E720103A80EAE03C7CF039EAE03CECF03A0200CAE031ECF039EAE039DCF03A0720503A93ACDADA1720003A826721603A8C60398A480A1802619C60398A47FC70398720A03A90C721803A9721A03A9721000AE721503A9350003B7350003B8720103A804721403A8721F00B0721003A9350003B7720100AE04721200AE2069725A03B02663350703B0200D725C03A5C603A5A10A27022050721E00B072125211725F03A5725F03AE350003AC350003AD350003AA721003A9721703A9350003B7350003B8C6080DA1782704901003A8721903A8720703A804721C03A9721903A9721B03A9721403A82000B668A1012603CCC288720601530220097200501502204C200772015015022043AE009CCDA80D3D602703CCC15F721100E555014800D772190066CDAA467202006403CCC15F7213006472070064042033207ACDB7133F773F787216006472180067203AB69CA18024023C9CB69CA180252E721000E53505008D725D01482604356300D7CDAA4572060064022012721000B072170064721900673FE73FE62014B68ABA89260E3DD7270A725D0149270472180066720300E50435F0009D721300E5721500E5CDB6EDCDB7002048720200E50ACDAAD1BE5FC301292ED83D00270220D2B6E2BAE1270DCDB700721300E5721500E52020720200E50435F0009D721200E5B6E4BAE3260D720400E50435F0009D721400E53F693FCD3FB1720200B015720103CD10720503CD0B3DCC2604721200CDCCC288B6CCA1962404AB03B7CC720103CD2F720000C50472100069720300B00DB6C6A16E222972120069CCC288720000AC1D3DC6261972120069721000ACCCC288721100ACB6C6A1962404AB03B7C6720303CD13720400AC2B3DC7262772160069721400AC205D721500AC720300C50AB6C7A17D250472140069B6C7A1962404AB03B7C7720503CD2D720400C50472180069720300B00CB6C8A16E2227721A00692021720800AC1C3DC82618721A0069721800AC200E721900ACB6C8A1962404AB03B7C85503CD00C5720300B0054569B13F6920009DAC00DDC89D720C006A03CCC39E3CA8B6A8A1192402206F3FA85506390060B660A1FF260AB6DCA10227163CDC20583FDCB660A132222BB6DBA10227043CDB204672110065B668A1012604721000653FA9357300ABC60157A102260435D200AB20253FDB72110065AE023E9FBBA997B660F73CA9B6A9A1082402200B3FA9AE023ECDADF14551AB720E006604B6D22002B6D4B1AB2212B694A10027022067721E0066721D0066202E5501450094B668A101260435020094720C006604B6D32002B6D5B1AB2520B693A10027022037721F0066721C00665501630060CDB44C45518C45508B201F5501450093B668A101260435020093B68CBA8B27022008721D0066721F0066720C006A08721D0066721F00662000AC00DDC89D3C9AB69AA1322403CCC63AAE009FCDA80DAE00A0CDA80DAE00EBCDA80D3F9A352353403580534A3540534B3500534C3500534DCDA9F290BE85B668A101270890A303AE2512200690A301C4250A72180065721B0065201CB668A101270890A3002B2416200690A301AE240E721A006572190065350F00EB200EB6EBA100260872190065721B0065AE024E459B60CDAA17352353403580534A3500534B3520534C3500534DCDA9F290BE85B668A101270890A303AE2512200690A301C4250A7210006672130066201CB668A101270890A3002B2418200690A301AE24107212006672110066350F009F3FA2200EB69FA10026087211006672130066AE021E459B60CDAA17352353403580534A3500534B3504534C3500534DCDA9F290BE85B668A101270890A303842512200690A301C4250A7214006672170066201CB668A101270890A3002B2416200690A301AE240E7216006672150066351400A0200EB6A0A1002608721500667217006672040066022008350A008635000085AE020E459B60CDAA173C9BB69BA1082403CCC63AAE024ECDAA25CDB46DBE57BFECAE020ECDAA25A678B7A7721B0066CDB46D455872455771AE021ECDAA2590BE7D72B20050274E2A1272A2FFFF271E90BE5072A2000190BF50202D72A20001270C90BE5072A9000190BF50201B90BE7F72B2005027043FA120023CA190BE5090BF7FB6A1A10F2515BE50BF7DCDB46D45587045576F457E80457D7F3FA1B69FA100270BA1092541AEFE34BF6F20527201006402203390BE7372B2006F27422C28C60154A1002721483C9EB19E2234905090A3000B2509BE6F1D000ABF5F200EBE731C0001BF5F2005CDAAD13FA2B6A2A100270990BE6D72B2005F2F04BE5FBF733F9E3F9B7207006605AEFE34BF71B668A101271FC60159A1032718A102270CA1012610721500667217006672190065721B00652000AC00DDC89D9DB668A1022518720F00B0022011720500690472120064725D01582602200020009DAC00DDC89D3F6A3FAF550153006072010060047216006A72030060047218006A5501620060CDB44CC60157A1022618C60143AB64B7D3BB5FB7D5C60144ABC8B7D2B060B7D42018A1012618C60142B7D3BB5FB7D5C60143AB64B7D2B060B7D4721C006A721200AFCE010190CE0103BF6BBF5790BF5FCDB439BE5FBF6D720300E525BE6B72BB011BBF6BBE6D72BB011BBF6D720500E510BE6B72BB011DBF6BBE6D72BB011DBF6D72030068022014CDA9DDB667A480B7677200006803CCC85CCCC90BCDB3857215006772170067720A015305720400E50E7206006409720100B0047214006A7200006803CCCA51CCCBFE72100064A602B75435000068200EA611B78FA608B7D8350100683F543F563F5520655501640060CDB44CB65F97A63C42BFB4B66097A63C42BFB6350200682043CDAAC235020068203A351600843500008335020068202C350208A53F90CDAA755501470060C60155A1012605CDB44C2008AE003CB66042BF50455178455077350300683504008E720E006605720D0066077211006ACDAAC27200006A0472150067B6E6BAE7271E720F500A03CDAAC27211006A7213006A721100AF72150067721F0067201D7207006418721000B07217006472190067721200E53FE23FE13FE43FE3CCCC65721700ADCDAA45CDAA46721D0066721F00663502009135050060720D00AD043501006045608D456076CDAA9CB660A1032716A105272EA107272FA101262E5501460057CDB41B20245501470057C60155A1012609CDB41B72100067200F3F58CDB4042008CDAAAB2003CDAAB3CDA819720F005B022028C60159A10126077208006A162017720600660F720400660A7208006A057209005B03CDAA75CCC7C03D5E260A72000066022003CCC785CCC7FE458F60456079CDAA9CB660A1002607721E00ADCCCA43720200AD4F35050060CDB41E72100067721000ADB68FA110230E7201501506721800662025CCCA43B68FA10D231872130067720150150C721400AD72120067721300AFCCCA43721100AD3D6626063D652602204E350A005A350A0059720C005608351F005A351F0059721200AD721E00AD720E006605720D006624721700AD720000D804721600ADB6ABA1642508A064A1642502A064B760CDB41E72110067CCCA43720400AD0220AB7210006A72140067B68FA10B230220697211006A721500677212006A72160067A109230220537213006A72170067721000AF721E0067A1072302203D721100AF721F00677214006A72180067A105230220277215006A72190067B6ABA1642508A064A1642502A064B760CDB41E72110067721C00AD721F00ADCDA8197201005B03CCC777CCC7FE721F006745B760B6B6BA60270220523DD727063D8D27022048720F500A063D97263F20355501680060CDB44C3D5F2602202F3D60260220217205501402200F3DB32702201C3DB22610455FB2200B3DB2270220054560B32008721000AF721E006745767945757A72000066057203006602204C7202006549CDAAD1BE5FB36D2F11B684BA83263A45B560B6B4BA6026312023BE6BB35F2F16721D0067AE0000CDA80D720E500A022018CDAAC22013720E500A02200C7210006A72140067200220E1725D014D2603CCCBA8720E500A2755014D0060AE003CB66042B3EE2709C60159A10327532033BF504551EF4550EE1D0001BFF22061C60159A103272A7208006551720A00654CBEEEB3F22606BE6FBFF02044BEEEA30001263DBEF072B0006FC301332E2E201C72080065D1720A0065CCBEEEA30001261FBE6F72B000ECC301332E10721C00653F753F7645767945757A2004721D0065CDAAD1BE5FC301132F05CDAA75202CC60159A101271A45726045715FBE5F720600660D7204006608C301152E03CCC7CECDA8197201005B03CCC7CE3D8E26147207006903CCC7CE720100E907721100E9CCC7CECCC7FE72160067721F0067725D08A52710350D005A350F00597211006772130067C60159A101270FC60155A10126047210006AA100260E3D8D260E721000AF721E006720047212006A3F5E55014C00A245787945777ACDA819A613B45B4D2703CCC7993D8E2600CCC7FEAC00DDC89D720F083412350B005A35110059721100677213006720157207006410351F005A351D00597211006772130067CDAE1EB668A101271C720F00AD17350A005A350A0059720C005608351F005A351F0059203F7201006402202BB69DA100271EA1C8241CA1962505CDAAAB2025A164240FA132250BCDAAB37213006720142005CDA9DD200D720600AD08720200B003CDA8DD5FB65AA43F97AF00A6D6C703C35FB659A43F97AF00A6D6C703C435FF03C57200006704721303C57202006704721103C57204006704721903C57206006704721B03C5720E006704721D03C57208006704721F03C5721503C57206006710720100E514720A00560F721403C52009720E005604721403C52000AC00DDC89DB699A1082503CCCEEFBE555CBF55AE009DCDA80DAE00A4CDA80DAE00CFCDA80DAE00D0CDA80DAE00D1CDA80DAE00D6CDA80D3C53720100532AAE00C6CDA80DAE00C7CDA80DAE00C8CDA80DAE00C9CDA80DAE00CACDA80DAE00CBCDA80DAE00CCCDA80DB653A1642403CCCEEF3F5390CE07FACE07FC5C2602905C90CF07FACF07FCAE0860CDA803AE08A5CDA80DAE008DCDA80D5CA3009825F7AE00EECDA803AE086ACDA80DAE086BCDA80DAE086CCDA80DAE07E9CDA803AE0833CDA80DAE00E1CDA803AE00E3CDA803AE00E6CDA8037200008F06AE00D8CDA80DAE008BCDA803AE0083CDA803AE0089CDA803AE00B6CDA803AE00B4CDA803BE75B3812407720E0062022006AE0075CDA803AE0077CDA8033C5EB65EA13C25083F5EAE00A2CDA80D3C54B654A1092504721100647209005404721F0064B654A13C253C3F54721F00ADAE00D7CDA80DAE00B2CDA80DAE00B3CDA80DAE00A3CDA80DAE00A7CDA80DAE0087CDA803AE00DDCDA803AE00DFCDA803AE00EACDA80D2000720E083403CCD82BC6086AA10126043505086C720108621735F0086A725D086B260A725F0862725F08352003CCD31FC60835A121260735F0086ACCD00B7208083403CCD82BC60835A160260735F0086ACCD065C60835A161260735F0086ACCD0DCC60835A153260735F0086ACCD153C60835A151260735F0086ACCD1FEC60835A150260735F0086ACCD249C60835A146260735F0086ACCD2EDC60835A154260735F0086ACCD4BFC60835A155260735F0086ACCD55EC60835A156260735F0086ACCD561C60835A157260735F0086ACCD564C60835A158260735F0086ACCD567C60835A159260735F0086ACCD5DFC60835A15A260735F0086ACCD6BDC60835A15B260735F0086ACCD709C60835A15C260735F0086ACCD755CCD82B35F1080B353F080C5501790819558880081A558881081B55017D081C55017E081D35FF081E35FF081FAE080BCF082FCF082BAE081FCF082D3515083A72180834721508345FCF083DCF083F7217083435550832725F0835CCD82B5507F707EB5507F607EC3503083BAE06E2CF07F2AE5000CF07F07203082A04721008343508083A721A082AAE5000CF0836AE7FFF5CCF0838356007EECDD7CD55017B080355017C0804AE0000CF0805AE0540CF0807350108093509080A721408345FCF083DCF083F7217083435550832725F0835CCD82B5507F907EB5507F807EC350A083BAE0762CF07F2AE0000CF07F07205082A04721008343508083A7218082AAE0000CF0836AE4FFF5CCF083835A007EECDD7CD55017B080355017C0804AE0000CF0805AE05A0CF080735020809350E080A721408345FCF083DCF083F7217083435550832725F0835CCD82BBE6FCF0820BE71CF0822BEECCF08245500AB0826B66AA403C70827720100E50472140827720300E50472160827720500E504721808277207006404721A0827720100AF04721C08277205006A04721E08275500650828721108285500660829AE0820CF082FCF082BAE0829CF082D350A083A55017B080355017C0804AE0000CF0805AE0001CF08073503080955083A080A721408345FCF083DCF083F7217083435550832725F0835CCD82BAE1000CF082FCF082BAE107FCF082D3508083A55017B080355017C0804AE0000CF0805AE0001CF0807350408093580080A721408345FCF083DCF083F7217083435550832725F0835CCD82BAE04371C0002F6A1AA266FAE04371C0043F6A1662664AE04371C0081F6A1CC26595FCF083DCF083FAE04373F60F6CDB7265C3C60B660A18225F3F6B7505CF6B7515CF6B7575CF6B758BE57C3083F262ABE50C3083D2623355507E2AE04375C5CCF07E5AE1000CF07E3CDB74B35F1080B353D080C721A0834200835F1080B353E080CAE080BCF082FCF082BAE080CCF082D3502083A7216083435550832725F0835CCD82B35F1080B3503080C72100862AE080BCF082FCF082BAE080CCF082D3502083A7216083435550832725F08353505086BCCD82B7218082A721A082A7207086203CCD44A7202086250C60835A1492703CCD4BCAE04375C5CF6C70863725F086435F1080B3507080C721208625FCF0865CF0867AE080BCF082FCF082BAE080CCF082D3502083A7216083435550832725F08353505086BCCD82B7204086203CCD82B721508625FCF083DCF083FAE04373F60F6CDB7265C3C60B660A18025F3F6B7505CF6B7515CF6B7575CF6B758BE57C3083F2659BE50C3083D265290CE0865CE086772BB083F2402905C72B9083D90CF0865CF086735AA07E2AE0437CF07E5C60864AE0080421C0000CF07E3CDB74B725C0864C60864C1086326047216086235F1080B353D080C3505086B201035F1080B353E080C725F0862725F0835AE080BCF082FCF082BAE080CCF082D3502083A7216083435550832725F0835CCD82B720808626DC60835A14A2666AE04375C5CF6B7505CF6B7515CF6B7575CF6B758BE57C308672619BE50C30865261235F1080B353D080C721808623505086B201035F1080B353E080C725F0862725F0835AE080BCF082FCF082BAE080CCF082D3502083A7216083435550832725F0835CCD82BCCD82BCDB85B725F086435AA07E2AE06E2CF07E5C60864AE0080421C0000CF07E3CDB74B725C0864C60864A10026DB35F1080B353D080C725F07F6725F07F7725F07F8725F07F9AE06E2CF07F2AE5000CF07F05507F707EBCDB773AE0762CF07F2AE0000CF07F05507F907EBCDB773725F085F200835F1080B353E080CAE080BCF082FCF082BAE080CCF082D3502083A7216083435550832725F0835CCD82BCCD82BCCD82BCCD82BCCD82B5FCF083DCF083FAE04373F603506005FCDB7D5BE57C3083F2634BE50C3083D262DAE04371C0002F6B7505CF6B75190BE5090CF07FA5CF6B7505CF6B75190BE5090CF07FC35F1080B353D080C200835F1080B353E080CAE080BCF082FCF082BAE080CCF082D3502083A7216083435550832725F0835CCD82B5FCF083DCF083FAE04373F60350A005FCDB7D5BE57C3083F2703CCD693BE50C3083D26F690AE043772A9000290F6B75FAE1287CDA89335AA50E0905C90F6B75FAE1288CDA89335AA50E0905C90F6B75FAE1289CDA89335AA50E0905C90F6B75FAE128ACDA89335AA50E0905C90F6B75FAE128BCDA89335AA50E0905C90F6B75FAE128CCDA89335AA50E0905C90F6B75FAE128DCDA89335AA50E0905C90F6B75FAE128ECDA89335AA50E035F1080B353D080C200835F1080B353E080CAE080BCF082FCF082BAE080CCF082D3502083A7216083435550832725F0835CCD82BAE1287CF082FCF082BAE128FCF082D3508083A55017B080355017C0804AE0000CF0805AE0001CF08073506080955083A080A721408345FCF083DCF083F7217083435550832725F0835CCD82BAE07FACF082FCF082BAE07FECF082D3504083A55017B080355017C0804AE0000CF0805AE0001CF08073507080955083A080A721408345FCF083DCF083F7217083435550832725F0835CCD82B5FCF083DCF083FAE04373F603508005FCDB7D5BE57C3083F2635BE50C3083D262EAE04371C0002F6C708A75CF6C708A85CF6C708A95CF6C708AA5CF6C708AB5CF6C708AC5C35F1080B353D080C200835F1080B353E080CAE080BCF082FCF082BAE080CCF082D3502083A7216083435550832725F0835205ECE07F23F50357E005172BB0050C607ECF735AA07E2C607EBAE00804272BB07F0CF07E3CE07F2CF07E5CDB74BCDB796C607EB725D07EC27014CAE00804272BB07F0CF082BCF082F72100834C308362609CE08387211083420015ACF082D81725D08322611720908620435AA0869720B0834039D20F89DB668A102240FCE1283CF07FACE1285CF07FCCCDBB0720008A603CCD8F0720208A6F85507F707EB5507F607ECAE06E2CF07F2AE5000CF07F0CE07F23F50357E005172BB0050C607ECF735AA07E2C607EBAE00804272BB07F0CF07E3CE07F2CF07E5CDB74B5507F907EB5507F807ECAE0762CF07F2AE0000CF07F0CE07F23F50357E005172BB0050C607ECF735AA07E2C607EBAE00804272BB07F0CF07E3CE07F2CF07E5CDB74BCDB796721208A6720E082A1EC60157A1022708B6ABA146240A200CB6ABA1AA24022004721E082ACCDBB07209082A03CCDBB07200085F31720000E502205BCE07FACF086DCE07FCCF086F35010875BE6FCF08765500AB087A7210085FA60095C6015097CF08602031720000E52CCE08605D27067211085F2020CE07FACF0871CE07FCCF0873BE71CF0878AE086DCF07F4CDDA657211085F20007202085F277200006A022045CE07FACF087BCE07FCCF087D35020883BE6FCF08845500AB08887212085F20257200006A20CE07FACF087FCE07FCCF0881BE71CF0886AE087BCF07F4CDDA657213085F20007204085F26B668A1032645CE07FACF0889CE07FCCF088B35030891BE6FCF08925500AB08967214085F2025B668A103271FCE07FACF088DCE07FCCF088FBE71CF0894AE0889CF07F4AD487215085F20007206085F022039CE07FACF0897CE07FCCF08993504089FBE6FCF08A05500AB08A4CE07FACF089BCE07FCCF089DBE71CF08A2AE0897CF07F4AD087217085F20002047AE0762CF07F2AE0000CF07F035A007EE350E07EF5507F907EB5507F807ECC607EFAE008062C607EF429FB760CDDB2F5507EB07F95507EC07F87201082A087211082A7214082A81720A082A08C607E9CA07EA2703CCDBB0550151006090AE003CCDAA5455005107EA55005007E9BE6FCF07FEBE71CF08005500AB0802AE06E2CF07F2AE07FACF07F4AE5000CF07F0356007EE350907EF5507F707EB5507F607ECC607EFAE008062C607EF429FB760AD1A5507EB07F75507EC07F67201082A087211082A7212082ACCDBB090CE07F4CE07F23F505507EC005172BB0050725F07ED90F6F75C905C725C07ED725C07ECC607ECB160254DCE07F21C007FF64CF75A7F35AA07E2C607EBAE00804272BB07F0CF07E3CE07F2CF07E590BF57CDB74BCDB796725C07EBC607EBC107EE2508725F07EB7210082ACDB77390BE57CE07F2725F07ECC607EDC107EF259681C60869A1AA2703CCDDC89D725D08322703CCDDC89DCD8080AC00DDC89D7200006605720300660A7217006572150065202ECE012DB36F2C02200C72B00131B36F2F0472150065CE012FB36F2F0672160065200C72BB0131B36F2C0472170065CDB6B0CDA7169BCDB35BCDA84F720F00B00E3DCF2612721500073DD02702200C7214000735C800CF356400D0725D0166260672130065204C720E500A0E5501660060CDB44C4551DE4550DD7202006524B6DDBADE260672120065202455016700603D60260435010060CDB44C4551E04550DF2010B6DFBAE026067213006520047211006A721F0062721300067215000672170061720300AF04721600613DEA265A7201006A0A3D05260A721E0062200435A400057200006A0D720000AF06350F009720023F977203006A063D0426062004350400047205006A0A3D02260A721200062004350F0002720100AF0A3D03260A72140006200435D30003720E500A1855014D0060AE003CB66042BF504551EF4550EE1D0001BFF2725A03A72702204D721903A9721B03A9721E00B0CDADC7350A03A635FF03A7721E521172115210350003AC350003AD350003AA35FF00603A6026FCC65217C65219721F5211CDA7CE720D03A908721D03A99D9D20FCC61000A1AA2702203BC61041A16627022032C6107FA1CC27022029C60100A1AA27022020C60141A16627022017C6017FA1CC2702200EC64800A1002607C64808A10127049D9D20FC35FF0416CD9830721500127217001272190012721B0012AC00B89C725C0411C60411A1042514725F041172140012725C0412AE040DA601CDE03FC60412A10A250C725F041272160012725C0413C60413A10A250C725F041372180012725C0414C60414A10A2508725F0414721A001281720400120024314B10AE5014CD98175B014D271CC60415A1332404725C0415C60415A1322612721C0012353304152008725F0415721D001281814FCD9336AE0201CD9355AE0501CD9355AE1401CD9355AE0301CD93558189355550E0350650E135FF50E2725F50E035AA50E05F1F011E011C00011F019C1E01A303E82FF135AA50E08581AE03FACD985BA601CD990EA601CD98CD814B0C4B004B004B00CE040B89CE040989AE5230CD99C95B084B00AE5230CD9A638481CD9563AE507FCD956CAE5089CD956C4B004B304B204B004B004B90AE523189AE0437CDE287BE0A89BE0889AE5089CD95995B0C4B004B004B204B004B084B00AE523189AE0417CDE287BE0A89BE0889AE507FCD95995B0C4B014B80AE5230CD9B87854B014B40AE5230CD9B8785A601CD95E24B01AE507FCD95F0844B01AE5089CD95F08481818920251E01F61E05F1270E1E01F616055F90F024015A0220141E015C1F011E055C1F051E075A1F071E0726D75F5B0281F62707110327045C20F65F81BF0C5A5C7D26FC72B0000C8189520420261F011E091F03F626041E05201F1E01F61E03F127071E055C1F05200A1E015C1F011E035C20DE1E05F626D55F5B06819001BF0D5E42BF0C90014D2709BE0D4272BB000CBF0C9002BE0D3F0E4272BB000D900281BF0A2A06AEFFFFBF08815FBF0881B608F1261AB609E101260CB60AE1022606B60BE10327089C2403A6FF81A60181EB03E70324096C0226056C0126017C81E603BB0BE703E602B90AE702E601B909E701F6B908F781E60388E60288FE891E03B60842BF0CBE087B034272BB000CBF0CBE097B024272BB000CBF0CBE0A7B014272BB000C9FB708BE087B043F094272BB0008BF08BE097B034272BB0008BF08BE0A7B024272BB0008BF08BE097B043F0A4272BB0009BF0924023C08BE0A7B034272BB0009BF0924023C08BE0A7B043F0B4272BB000ABF0A2405BE085CBF085B0481B60BE003B70BB60AE202B70AB609E201B709B608F2B7088188F6B708E601B709E602B70AE603B70B84815204E60388E60288E60188F69688CDE1465B08813F0C4D2A07CDE21A7210000CB6082A07CDE22D7212000C9089F62667E601266390BE08271BE602272EB10822592606E603B109225190AE00204FE706E707205789EE0290BE0A5165BF0A85EF06905FEF0490BF08908581E704E705E706E603906290BF089095B60A9097E60390629001B70AB60B9097E6039062E707909FB70B90858190BE08EF0690BE0A90BF0890AE00104FB70AB70BE705380B390A3909390869076906690549F12537261CE704E605E10125102614E606E1022508260CE607E003240AE6042019E704E607E003E707E606E202E706E605E201E705E604F23C0B905A26B39085E704817363016302600326096C0226056C0126017C8133083309330A300B260A3C0A26063C0926023C08814D270B34083609360A360B4A26F58188B608F7B609E701B60AE702B60BE703848190F6F7271E90E601E701271790E602E702271090E603E70327091C000472A9000420DD81BF0A3F093F088188BF0C90BF0F909F42BF0AB60C97B60F42BF084D271097B60D270B4272BB0009BF0924023C08B60C271097B610270B4272BB0009BF0924023C0884818000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
                    ,
                    object : ConexionTrefp.MyCallback {
                        override fun onSuccess(result: Boolean): Boolean {
                            Log.d(
                                "MyAsyncTaskUpdateFirmware", "onSuccess " +
                                        result.toString()
                            )
                            runOnUiThread {

                                conexionTrefp2.makeToast("Proceso finalizado : ${if (result) "correcto" else "Error"}")
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
                    })//.execute()
            }
            // Aquí puedes manejar el contenido del archivo como desees, por ejemplo, mostrarlo en un TextView
            ///   findViewById<TextView>(R.id.fileContentTextView).text = text ?: "Archivo vacío"
            //    binding.TextResultado.text = text ?: "Archivo vacío"
        } catch (e: Exception) {
            Log.e("FileError", "Error al leer el archivo", e)
        }
    }

    suspend fun pedirLoger(view: View){
       // bluetoothLeService = bluetoothService!!.bluetoothLeService()//inicializar objeto que manda comandos
        //inicialización de variables
        var listaInicialDatosTiempoOriginal: MutableList<String> = ArrayList()
        var listaInicialDatosEventoOriginal: MutableList<String> = ArrayList()
        var listaInicialDatosTiempoFiltrada: MutableList<String> = ArrayList()
        var listaInicialDatosEventoFiltrada: MutableList<String> = ArrayList()
        var listaFinalDatosTiempoAjustado: MutableList<String> = ArrayList()
        var listaFinalDatosEventoAjustado: MutableList<String> = ArrayList()
        var listaInicialDatosTiempoFiltradaErrores: MutableList<String> = ArrayList()
        var listaInicialDatosEventosFiltradaErrores: MutableList<String> = ArrayList()
        val resp = lifecycleScope.async(Dispatchers.Main) { //Petición de proceso en segundo plano
         //   editarTextoMensajeProgressDialog("Pedir plantilla: Listo\nPedir hora: Listo\nPedir Logger Tiempo: En proceso")

            //Se pide el logger al control DATOS DE TIEMPO
            //Limpiar las listas antes de empezar la recolección
            bluetoothLeService!!.clearListLogger()
            listaInicialDatosTiempoOriginal.clear()

            bluetoothLeService!!.sendFirstComando("4060") //Mandar comando a control de extracción de datos de tiempo
            delay(40000)//Esperar la extracción del logger
            listaInicialDatosTiempoOriginal = bluetoothLeService!!.getLogeer()!! //Obtener el logger en una lista

        }.await()

        Log.d("TAG", "extraerLogger Datos Tiempo:${listaInicialDatosTiempoOriginal.size}")
        listaInicialDatosTiempoOriginal.map {
            Log.d("", it)//Revisión de los datos extraidos
        }

        val resp2 = lifecycleScope.async(Dispatchers.Main) { //Petición de proceso en segundo plano
            //createProgressDialog("Obteniendo datos de Evento")
          //  editarTextoMensajeProgressDialog("Pedir plantilla: Listo\nPedir hora: Listo\nPedir Logger Tiempo: Listo\nPedir Logger Evento: En proceso")
            //Se pide el logger al control DATOS DE Evento
            //Limpiar las listas antes de empezar la recolección
            bluetoothLeService!!.clearListLogger()
            listaInicialDatosEventoOriginal.clear()

            bluetoothLeService!!.sendFirstComando("4061") //Mandar comando a control de extracción de datos de tiempo
            delay(40000)//Esperar la extracción del logger
            listaInicialDatosEventoOriginal = bluetoothLeService!!.getLogeer()!! //Obtener el logger en una lista

        }.await()

        Log.d("TAG", "extraerLogger Datos Evento:${listaInicialDatosEventoOriginal.size}")
        listaInicialDatosEventoOriginal.map {
            Log.d("","listaInicialDatosEventoOriginal " + it)//Revisión de los datos extraidos
        }
        listaInicialDatosTiempoOriginal.map {
            Log.d("","listaInicialDatosTiempoOriginal " + it)//Revisión de los datos extraidos
        }

    }//fin de m[etodo de meticion de datos
    private fun openFile(mineType: String = "*/*") {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mineType
        }
        startActivityForResult(intent, PICK_OPEN_FILE)
    }



    private fun ObtenerPlantilla() {
        customProgressDialog!!.show()
        conexionTrefp2.getPlantillacommand(object : ConexionTrefp.MyCallback {
            override fun getInfo(data: MutableList<String>?) {
                Log.d("getPlantillacommand", "data $data")
                if (data != null) {
                    if (data.isNotEmpty()) {
                        var chs = data[0].replace("\\s".toRegex(), "")
                        var calculateCheckSume = chs.substring(0, chs.length - 8)
                        var CHStoValid = conexionTrefp2.sumarParesHexadecimales(calculateCheckSume)
                        Log.d(
                            "getPlantillacommand", "\n  ${calculateCheckSume}  \n" +
                                    "valor deCHStoValid $CHStoValid ${chs.substring(chs.length - 8)} \n chs $chs "
                        )
                        esp!!.putString("currentPlantilla", chs.toString())
                        esp!!.apply()
                        if (CHStoValid.equals(chs.substring(chs.length - 8))) {
                            var Plantilla = conexionTrefp2.ObtenerPlantillafromAA(chs)
                            Log.d("getPlantillacommand", "valor del checksume ok ")
                            obtenerDatosStatus(chs)

                        }
                        var Plantilla: String? =
                            if (!chs.isNullOrEmpty()) conexionTrefp2.ObtenerPlantillafromAA(
                                chs.replace(
                                    "\\s".toRegex(), ""
                                )
                            ) else "vacia"
                        Log.d("getPlantillacommand", "dato de la plantilla $Plantilla")

                        esp!!.putString(
                            "plantillaDefaultAA",
                            Plantilla!!.substring(0, Plantilla!!.length - 8)
                        )
                        esp!!.apply()
                        when (sp!!.getString("name", "")) {
                            "OXXO-CMO" -> {
                                Handler(Looper.getMainLooper()).postDelayed({


                                }, 100)
                            }

                            else -> {

                            }
                        }
                    }

                }
            }

            override fun onError(error: String) {
                Log.d("getPlantillacommand", "error $error")
            }

            override fun onProgress(progress: String): String {
                Log.d("getPlantillacommand", "progress $progress")
                return progress
            }

            override fun onSuccess(result: Boolean): Boolean {
                Log.d("getPlantillacommand", "result $result")
                customProgressDialog!!.dismiss()

                return result
            }
        })
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


        if (deci) binding!!.ivlockIconConnection.visibility =
            View.VISIBLE else binding!!.ivlockIconConnection.visibility =
            View.GONE
        // ivLockIcon.setVisibility(View.GONE);
    }


    private fun GetNowDateExa(): String {
        val currentTimeMillis = System.currentTimeMillis()
        val currentTimeHexNOW = java.lang.Long.toHexString(currentTimeMillis / 1000)
        println("currentTimeHexNOW $currentTimeHexNOW ")

        return currentTimeHexNOW

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


    private fun createCombinedExcelFile(
        dataTime: MutableList<String>?,
        dataEvent: MutableList<String>?,
        dataTimeCrudos: MutableList<String>?,
        finalEventoCrudoVersion2: MutableList<String>?
    ) {
        // Lanzar una coroutine en un hilo secundario
        GlobalScope.launch(Dispatchers.IO) {
            val workbook = XSSFWorkbook()

            // Crear hojas y agregar datos
            createExcelSheet(
                dataTime, "Time",
                arrayOf("Iteración", "TimeStamp", "Temperatura 1", "Temperatura 1", "Voltaje", "Original"),
                2, workbook
            )

            createExcelSheet(
                dataEvent, "Event",
                arrayOf("Iteración", "TimeStampStart", "TimeStampEnd", "Temperatura 1", "Temperatura 1", "Consumo", "TipoEvent", "Original"),
                2, workbook
            )

            createExcelSheet(
                dataTimeCrudos, "Datos Crudos Time",
                arrayOf("Iteración", "TimeStampStart", "TimeStampEnd", "Temperatura 1", "Temperatura 1", "Consumo", "TipoEvent", "Original"),
                2, workbook
            )

            createExcelSheet(
                finalEventoCrudoVersion2, "Datos Crudos Event",
                arrayOf("Iteración", "TimeStampStart", "TimeStampEnd", "Temperatura 1", "Temperatura 1", "Consumo", "TipoEvent", "Original"),
                2, workbook
            )

            val fileName = "LoggerImbera ${sp!!.getString("mac", "")} ${Calendar.getInstance().time}.xlsx"
            val file = File(this@MainActivity.getExternalFilesDir(null), fileName)

            try {
                FileOutputStream(file).use { outputStream ->
                    workbook.write(outputStream)
                }

                withContext(Dispatchers.Main) {
                    customProgressDialog?.dismiss()
                    Toast.makeText(this@MainActivity, "Reporte generado correctamente", Toast.LENGTH_LONG).show()
                }

                try {
                    setupEmailEvent(
                        "Datos tipo Tiempo",
                        "Archivo generado en la fecha: ${Calendar.getInstance().time}",
                        fileName
                    )
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Reporte no generado", Toast.LENGTH_LONG).show()
                }
                e.printStackTrace()
            }
        }
    }

    /*
    private fun createCombinedExcelFile(
        dataTime: MutableList<String>?,
        dataEvent: MutableList<String>?,
        dataTimeCrudos: MutableList<String>?,
        FinalEventoCrudoVersion2: MutableList<String>?

    ) {
        val workbook = XSSFWorkbook()

        // Crear la hoja "Time" y agregar títulos y datos
        createExcelSheet(
            dataTime,
            "Time",
            arrayOf(
                "Iteración",
                "TimeStamp",
                "Temperatura 1",
                "Temperatura 1",
                "Voltaje",
                "Original"
            ),
            2,
            workbook
        )

        // Crear la hoja "Event" y agregar títulos y datos

        createExcelSheet(
            dataEvent,
            "Event",
            arrayOf(
                "Iteración",
                "TimeStampStart",
                "TimeStampEnd",
                "Temperatura 1",
                "Temperatura 1",
                "Consumo",
                "TipoEvent",
                "Original"
            ),
            2,
            workbook
        )
        createExcelSheet(dataTimeCrudos,"Datos Crudos Time", arrayOf(
            "Iteración",
            "TimeStampStart",
            "TimeStampEnd",
            "Temperatura 1",
            "Temperatura 1",
            "Consumo",
            "TipoEvent",
            "Original"
        ),
            2,
            workbook)
        createExcelSheet(FinalEventoCrudoVersion2,"Datos Crudos Event", arrayOf(
            "Iteración",
            "TimeStampStart",
            "TimeStampEnd",
            "Temperatura 1",
            "Temperatura 1",
            "Consumo",
            "TipoEvent",
            "Original"
        ),
            2,
            workbook)

        val nombreFile =
            "LoggerImbera ${sp!!.getString("mac", "")} ${Calendar.getInstance().time}.xlsx"
        val file: File = File(this.getExternalFilesDir(null), nombreFile)
        val outputStream = FileOutputStream(file)
        workbook.write(outputStream)
        outputStream.close()
        runOnUiThread {
            customProgressDialog!!.dismiss()
        }
        try {

            this.runOnUiThread {
                Toast.makeText(
                    this,
                    "Reporte generado correctamente",
                    Toast.LENGTH_LONG
                ).show()
            }
            try {

                setupEmailEvent(
                    "Datos tipo TIempo",
                    "Archivo generado en la fecha:" + Calendar.getInstance().time,
                    nombreFile
                )
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        } catch (e: IOException) {
            this.runOnUiThread {
                Toast.makeText(
                    this,
                    "Reporte no generado",
                    Toast.LENGTH_LONG
                ).show()
            }
            e.printStackTrace()
        }
        // Tu código para mostrar un mensaje de éxito o error
    }
*/
    private fun createExcelSheet(
        data: MutableList<String>?,
        sheetName: String,
        reportHeaders: Array<String>,
        positionToInsert00: Int,
        workbook: XSSFWorkbook
    ) {
        val sheet = workbook.createSheet(sheetName) // Crear una nueva hoja

        // Agregar los encabezados a la hoja
        val encabezadosFila = sheet.createRow(0)
        val style: CellStyle = workbook.createCellStyle()
        val styleceldaOscura: CellStyle = workbook.createCellStyle()
        val styleceldaClara: CellStyle = workbook.createCellStyle()
        val styleceldainfo: CellStyle = workbook.createCellStyle()

        style.fillForegroundColor =
            IndexedColors.DARK_TEAL.getIndex() //encabezado

        styleceldaOscura.fillForegroundColor =
            IndexedColors.PALE_BLUE.getIndex() //celda oscura

        styleceldaClara.fillForegroundColor =
            IndexedColors.LIGHT_TURQUOISE1.getIndex() //celda clara

        styleceldainfo.fillForegroundColor =
            IndexedColors.LIGHT_TURQUOISE1.getIndex() //celda clara


        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        val font = workbook.createFont()
        font.color = IndexedColors.WHITE.index // Establece el color de la fuente a negro
        style.setFont(font)
        styleceldaOscura.fillPattern = FillPatternType.SOLID_FOREGROUND
        styleceldaClara.fillPattern = FillPatternType.SOLID_FOREGROUND
        styleceldainfo.fillPattern = FillPatternType.SOLID_FOREGROUND

        /*   var estiloActual =
               true //styleceldaOscura // Inicia con el estilo oscuro
        */   for ((index, encabezado) in reportHeaders.withIndex()) {
            val celda = encabezadosFila.createCell(index)
            celda.setCellValue(encabezado)
            celda.setCellStyle(style);
        }


        var colNumTIME = 0
        var filaNumTIME = 1
        when (sheetName) {
            "Time" -> {
                var estiloActual =
                    true //styleceldaOscura // Inicia con el estilo oscuro
                var index = 1
                val filaTIME = sheet.createRow(filaNumTIME++)
                if (data != null ) {

                    for (dato in data)

                    {
                        if (dato.length>18)  {

                            val indiceReal = index  //+ 1
                            var TimeStampR = dato.substring(0, 8)
                            var voltajeR = dato.substring(18, 20)
                            var Temp2R =
                                conexionTrefp2.ReturnValLoggerTraduccion(dato.substring(12, 16))
                            var Temp1R =
                                conexionTrefp2.ReturnValLoggerTraduccion(dato.substring(8, 12))
                            val filaTIME = sheet.createRow(filaNumTIME++)
                            // colNumTIME = 0
                            val Iteración = filaTIME.createCell(colNumTIME++)
                            Iteración.setCellValue(index.toString())
                            if (estiloActual/*.equals(styleceldaOscura)*/) Iteración.setCellStyle(
                                styleceldaOscura
                            ) else Iteración.setCellStyle(styleceldaClara)
                            val TimeStamp = filaTIME.createCell(colNumTIME++)
                            TimeStamp.setCellValue(
                                conexionTrefp2.convertirHexAFecha(
                                    TimeStampR
                                )
                            )
                            if (estiloActual/*.equals(styleceldaOscura)*/) TimeStamp.setCellStyle(
                                styleceldaOscura
                            ) else TimeStamp.setCellStyle(styleceldaClara)


                            val Temp1 = filaTIME.createCell(colNumTIME++)
                            Temp1.setCellValue(Temp1R)
                            if (estiloActual/*.equals(styleceldaOscura)*/) Temp1.setCellStyle(
                                styleceldaOscura
                            ) else Temp1.setCellStyle(styleceldaClara)

                            val Temp2 = filaTIME.createCell(colNumTIME++)
                            Temp2.setCellValue(Temp2R)
                            if (estiloActual/*.equals(styleceldaOscura)*/) Temp2.setCellStyle(
                                styleceldaOscura
                            ) else Temp2.setCellStyle(styleceldaClara)

                            val voltaje = filaTIME.createCell(colNumTIME++)
                            voltaje.setCellValue("${conexionTrefp2.getDecimal(voltajeR)}  Vca")  //" + conexionTrefp.getDecimal(voltaje) + " Vca") //voltajeR)
                            if (estiloActual/*.equals(styleceldaOscura)*/) voltaje.setCellStyle(
                                styleceldaOscura
                            ) else voltaje.setCellStyle(styleceldaClara)

                            val Original = filaTIME.createCell(colNumTIME++)
                            Original.setCellValue(dato)
                            if (estiloActual/*.equals(styleceldaOscura)*/) Original.setCellStyle(
                                styleceldaOscura
                            ) else Original.setCellStyle(styleceldaClara)

                            //     estiloActual = false
                            index++
                            // Alternar entre estilos
                            if (estiloActual) {
                                Iteración.setCellStyle(styleceldaOscura)
                            } else {
                                Iteración.setCellStyle(styleceldaClara)
                            }
                            estiloActual = !estiloActual
                            colNumTIME = 0
                        }
                    }
                }
                index = index + 5

                val currentTime = Calendar.getInstance().time
                var fila = sheet.createRow(index++)
                var Creador = fila.createCell(1)
                //temperature1p1Celda.setCellValue(temperature1p1.toString())
                // Creador.setCellValue(dato.second)

                when (sp!!.getString("userjerarquia", "")) {
                    "1" -> {
                        Creador.setCellValue(
                            "Creador:" + sp!!.getString(
                                "userId",
                                ""
                            ) + " - Jerarquía: Administrador"
                        )
                        Creador.setCellStyle(styleceldainfo)
                    }

                    "4" -> {
                        Creador.setCellValue(
                            "Creador:" + sp!!.getString(
                                "userId",
                                ""
                            ) + " - Jerarquía: Producción"
                        )
                        Creador.setCellStyle(styleceldainfo)
                    }
                }
                fila = sheet.createRow(index++)
                Creador = fila.createCell(1)

                Creador.setCellValue("Fecha:$currentTime")
                Creador.setCellStyle(styleceldainfo)

                fila = sheet.createRow(index++)
                Creador = fila.createCell(1)
                Creador.setCellValue(
                    "Nombre: " + sp!!.getString(
                        "name",
                        ""
                    ) + "    MAC:  " + sp!!.getString("mac", "")
                )
                Creador.setCellStyle(styleceldainfo)

                fila = sheet.createRow(index++)
                Creador = fila.createCell(1)
                Creador.setCellValue("Versión ImberaP:" )
                Creador.setCellStyle(styleceldainfo)
                data?.clear()
            }

            "Event" -> {
                var colNumEVENT = 1
                var filaNumEVENT = 1
                var estiloActual =
                    true //styleceldaOscura // Inicia con el estilo oscuro
                var index = 1
                var fila = sheet.createRow(filaNumEVENT++)
                if (data != null) {
                    for (dato in data) {

                        Log.d("debugConco", "dato $dato")
                        if (dato.length >= 30) {
                            val indiceReal = index

                            var TimeStampStartR = dato.substring(0, 8)
                            var TimeStampEndR = dato.substring(8, 16)
                            var voltajeR =
                                if (sp!!.getString("name","").equals("CEO_CONCO")){
                                    dato.substring(28, 32)
                                }else{
                                    dato.substring(28, 30)
                                }

                            var TipoEventR = dato.substring(16, 18)

                            var Ev = when (conexionTrefp2.getDecimal(dato.substring(16, 18))) {
                                1 -> "Evento 1: Apertura de Puerta"
                                2 -> "Evento 2: Ciclo de Compresor"
                                3 -> "Evento 3: Ciclo de Deshielo"
                                4 -> "Evento 4: Falla de Energía"
                                5 -> "Evento 5: Alarma"
                                else -> "Evento desconocido"
                            }

                            var Temp2R =
                                conexionTrefp2.ReturnValLoggerTraduccion(dato.substring(22, 26))
                            var Temp1R =
                                conexionTrefp2.ReturnValLoggerTraduccion(dato.substring(18, 22))

                            val fila = sheet.createRow(filaNumEVENT++)
                            colNumEVENT = 0
                            val Iteración = fila.createCell(colNumEVENT++)
                            Iteración.setCellValue(index.toString())
                            if (estiloActual/*.equals(styleceldaOscura)*/) Iteración.setCellStyle(
                                styleceldaOscura
                            ) else Iteración.setCellStyle(styleceldaClara)


                            val TimeStamp = fila.createCell(colNumEVENT++)
                            TimeStamp.setCellValue(
                                conexionTrefp2.convertHexToHumanDateLogger(
                                    TimeStampStartR
                                )
                            )
                            if (estiloActual/*.equals(styleceldaOscura)*/) TimeStamp.setCellStyle(
                                styleceldaOscura
                            ) else TimeStamp.setCellStyle(styleceldaClara)


                            val TimeStampEnd = fila.createCell(colNumEVENT++)
                            TimeStampEnd.setCellValue(
                                conexionTrefp2.convertHexToHumanDateLogger(
                                    TimeStampEndR
                                )
                            )
                            if (estiloActual/*.equals(styleceldaOscura)*/) TimeStampEnd.setCellStyle(
                                styleceldaOscura
                            ) else TimeStampEnd.setCellStyle(styleceldaClara)

                            val Temp1 = fila.createCell(colNumEVENT++)
                            Temp1.setCellValue(Temp1R)
                            if (estiloActual/*.equals(styleceldaOscura)*/) Temp1.setCellStyle(
                                styleceldaOscura
                            ) else Temp1.setCellStyle(styleceldaClara)

                            val Temp2 = fila.createCell(colNumEVENT++)
                            Temp2.setCellValue(Temp2R)
                            if (estiloActual/*.equals(styleceldaOscura)*/) Temp2.setCellStyle(
                                styleceldaOscura
                            ) else Temp2.setCellStyle(styleceldaClara)


                            val voltaje = fila.createCell(colNumEVENT++)
                            if (dato.substring(16, 18).equals("02")) {
                                val vol = conexionTrefp2.getDecimal(voltajeR)
                                // Suponiendo que "val" es tu valor decimal
                                val valorDecimal = BigDecimal(vol)

                                // Convertir a float
                                val valorFloat = (valorDecimal).toFloat() / 100
                                voltaje.setCellValue("${valorFloat}  KVA")
                            } else voltaje.setCellValue("${conexionTrefp2.getDecimal(voltajeR)}  Vca")  //" + conexionTrefp.getDecimal(voltaje) + " Vca") //voltajeR)

                            if (estiloActual/*.equals(styleceldaOscura)*/) voltaje.setCellStyle(
                                styleceldaOscura
                            ) else voltaje.setCellStyle(styleceldaClara)

                            val TipoEvent = fila.createCell(colNumEVENT++)

                            TipoEvent.setCellValue(Ev)  //" + conexionTrefp.getDecimal(voltaje) + " Vca") //voltajeR)
                            if (estiloActual/*.equals(styleceldaOscura)*/) TipoEvent.setCellStyle(
                                styleceldaOscura
                            ) else TipoEvent.setCellStyle(styleceldaClara)

                            val Original = fila.createCell(colNumEVENT++)
                            Original.setCellValue(dato)
                            if (estiloActual/*.equals(styleceldaOscura)*/) Original.setCellStyle(
                                styleceldaOscura
                            ) else Original.setCellStyle(styleceldaClara)

                            // estiloActual = false
                            index++
                            // Alternar entre estilos
                            if (estiloActual) {
                                Iteración.setCellStyle(styleceldaOscura)
                            } else {
                                Iteración.setCellStyle(styleceldaClara)
                            }
                            estiloActual = !estiloActual

                        }
                    }


                }
                index = index + 5

                val currentTime = Calendar.getInstance().time
               // var fila = sheet.createRow(index++)
                var Creador = fila.createCell(1)
                //temperature1p1Celda.setCellValue(temperature1p1.toString())
                // Creador.setCellValue(dato.second)

                when (sp!!.getString("userjerarquia", "")) {
                    "1" -> {
                        Creador.setCellValue(
                            "Creador:" + sp!!.getString(
                                "userId",
                                ""
                            ) + " - Jerarquía: Administrador"
                        )
                        Creador.setCellStyle(styleceldainfo)
                    }

                    "4" -> {
                        Creador.setCellValue(
                            "Creador:" + sp!!.getString(
                                "userId",
                                ""
                            ) + " - Jerarquía: Producción"
                        )
                        Creador.setCellStyle(styleceldainfo)
                    }
                }
                fila = sheet.createRow(index++)
                Creador = fila.createCell(1)

                Creador.setCellValue("Fecha:$currentTime")
                Creador.setCellStyle(styleceldainfo)

                fila = sheet.createRow(index++)
                Creador = fila.createCell(1)
                Creador.setCellValue(
                    "Nombre: " + sp!!.getString(
                        "name",
                        ""
                    ) + "    MAC:  " + sp!!.getString("mac", "")
                )
                Creador.setCellStyle(styleceldainfo)

                fila = sheet.createRow(index++)
                Creador = fila.createCell(1)
                Creador.setCellValue("Versión ImberaP:")
                Creador.setCellStyle(styleceldainfo)
            }
            "Datos Crudos Event" -> {
                var colNumEVENT = 1
                var filaNumEVENT = 1
                var estiloActual =
                    true //styleceldaOscura // Inicia con el estilo oscuro
                var index = 1
                val fila = sheet.createRow(filaNumEVENT++)
                if (data != null) {
                    for (dato in data) {

                        Log.d("debugConco", "dato $dato")
                        if (dato.length >= 30) {
                            val indiceReal = index

                            var TimeStampStartR = dato.substring(0, 8)
                            var TimeStampEndR = dato.substring(8, 16)
                            var voltajeR =
                                if (sp!!.getString("name","").equals("CEO_CONCO")){
                                    dato.substring(28, 32)
                                }else{
                                    dato.substring(28, 30)
                                }

                            var TipoEventR = dato.substring(16, 18)

                            var Ev = when (conexionTrefp2.getDecimal(dato.substring(16, 18))) {
                                1 -> "Evento 1: Apertura de Puerta"
                                2 -> "Evento 2: Ciclo de Compresor"
                                3 -> "Evento 3: Ciclo de Deshielo"
                                4 -> "Evento 4: Falla de Energía"
                                5 -> "Evento 5: Alarma"
                                else -> "Evento desconocido"
                            }

                            var Temp2R =
                                conexionTrefp2.ReturnValLoggerTraduccion(dato.substring(22, 26))
                            var Temp1R =
                                conexionTrefp2.ReturnValLoggerTraduccion(dato.substring(18, 22))

                            val fila = sheet.createRow(filaNumEVENT++)
                            colNumEVENT = 0
                            val Iteración = fila.createCell(colNumEVENT++)
                            Iteración.setCellValue(index.toString())
                            if (estiloActual/*.equals(styleceldaOscura)*/) Iteración.setCellStyle(
                                styleceldaOscura
                            ) else Iteración.setCellStyle(styleceldaClara)


                            val TimeStamp = fila.createCell(colNumEVENT++)
                            TimeStamp.setCellValue(
                                conexionTrefp2.convertHexToHumanDateLogger(
                                    TimeStampStartR
                                )
                            )
                            if (estiloActual/*.equals(styleceldaOscura)*/) TimeStamp.setCellStyle(
                                styleceldaOscura
                            ) else TimeStamp.setCellStyle(styleceldaClara)


                            val TimeStampEnd = fila.createCell(colNumEVENT++)
                            TimeStampEnd.setCellValue(
                                conexionTrefp2.convertHexToHumanDateLogger(
                                    TimeStampEndR
                                )
                            )
                            if (estiloActual/*.equals(styleceldaOscura)*/) TimeStampEnd.setCellStyle(
                                styleceldaOscura
                            ) else TimeStampEnd.setCellStyle(styleceldaClara)

                            val Temp1 = fila.createCell(colNumEVENT++)
                            Temp1.setCellValue(Temp1R)
                            if (estiloActual/*.equals(styleceldaOscura)*/) Temp1.setCellStyle(
                                styleceldaOscura
                            ) else Temp1.setCellStyle(styleceldaClara)

                            val Temp2 = fila.createCell(colNumEVENT++)
                            Temp2.setCellValue(Temp2R)
                            if (estiloActual/*.equals(styleceldaOscura)*/) Temp2.setCellStyle(
                                styleceldaOscura
                            ) else Temp2.setCellStyle(styleceldaClara)


                            val voltaje = fila.createCell(colNumEVENT++)
                            if (dato.substring(16, 18).equals("02")) {
                                val vol = conexionTrefp2.getDecimal(voltajeR)
                                // Suponiendo que "val" es tu valor decimal
                                val valorDecimal = BigDecimal(vol)

                                // Convertir a float
                                val valorFloat = (valorDecimal).toFloat() / 100
                                voltaje.setCellValue("${valorFloat}  KVA")
                            } else voltaje.setCellValue("${conexionTrefp2.getDecimal(voltajeR)}  Vca")  //" + conexionTrefp.getDecimal(voltaje) + " Vca") //voltajeR)

                            if (estiloActual/*.equals(styleceldaOscura)*/) voltaje.setCellStyle(
                                styleceldaOscura
                            ) else voltaje.setCellStyle(styleceldaClara)

                            val TipoEvent = fila.createCell(colNumEVENT++)

                            TipoEvent.setCellValue(Ev)  //" + conexionTrefp.getDecimal(voltaje) + " Vca") //voltajeR)
                            if (estiloActual/*.equals(styleceldaOscura)*/) TipoEvent.setCellStyle(
                                styleceldaOscura
                            ) else TipoEvent.setCellStyle(styleceldaClara)

                            val Original = fila.createCell(colNumEVENT++)
                            Original.setCellValue(dato)
                            if (estiloActual/*.equals(styleceldaOscura)*/) Original.setCellStyle(
                                styleceldaOscura
                            ) else Original.setCellStyle(styleceldaClara)

                            // estiloActual = false
                            index++
                            // Alternar entre estilos
                            if (estiloActual) {
                                Iteración.setCellStyle(styleceldaOscura)
                            } else {
                                Iteración.setCellStyle(styleceldaClara)
                            }
                            estiloActual = !estiloActual

                        }
                    }

                    index = index + 5

                    val currentTime = Calendar.getInstance().time
                    var fila = sheet.createRow(index++)
                    var Creador = fila.createCell(1)
                    //temperature1p1Celda.setCellValue(temperature1p1.toString())
                    // Creador.setCellValue(dato.second)

                    when (sp!!.getString("userjerarquia", "")) {
                        "1" -> {
                            Creador.setCellValue(
                                "Creador:" + sp!!.getString(
                                    "userId",
                                    ""
                                ) + " - Jerarquía: Administrador"
                            )
                            Creador.setCellStyle(styleceldainfo)
                        }

                        "4" -> {
                            Creador.setCellValue(
                                "Creador:" + sp!!.getString(
                                    "userId",
                                    ""
                                ) + " - Jerarquía: Producción"
                            )
                            Creador.setCellStyle(styleceldainfo)
                        }
                    }
                    fila = sheet.createRow(index++)
                    Creador = fila.createCell(1)

                    Creador.setCellValue("Fecha:$currentTime")
                    Creador.setCellStyle(styleceldainfo)

                    fila = sheet.createRow(index++)
                    Creador = fila.createCell(1)
                    Creador.setCellValue(
                        "Nombre: " + sp!!.getString(
                            "name",
                            ""
                        ) + "    MAC:  " + sp!!.getString("mac", "")
                    )
                    Creador.setCellStyle(styleceldainfo)

                    fila = sheet.createRow(index++)
                    Creador = fila.createCell(1)
                    Creador.setCellValue("Versión ImberaP:")
                    Creador.setCellStyle(styleceldainfo)
                }
            }


            "Datos Crudos Time"->
            {
                var estiloActual =
                    true //styleceldaOscura // Inicia con el estilo oscuro
                var index = 1
                val filaTIME = sheet.createRow(filaNumTIME++)
                if (data != null ) {

                    for (dato in data)

                    {
                        if (dato.length>18)  {

                            val indiceReal = index  //+ 1
                            var TimeStampR = dato.substring(0, 8)
                            var voltajeR = dato.substring(18, 20)
                            var Temp2R =
                                conexionTrefp2.ReturnValLoggerTraduccion(dato.substring(12, 16))
                            var Temp1R =
                                conexionTrefp2.ReturnValLoggerTraduccion(dato.substring(8, 12))
                            val filaTIME = sheet.createRow(filaNumTIME++)
                            // colNumTIME = 0
                            val Iteración = filaTIME.createCell(colNumTIME++)
                            Iteración.setCellValue(index.toString())
                            if (estiloActual/*.equals(styleceldaOscura)*/) Iteración.setCellStyle(
                                styleceldaOscura
                            ) else Iteración.setCellStyle(styleceldaClara)
                            val TimeStamp = filaTIME.createCell(colNumTIME++)
                            TimeStamp.setCellValue(
                                conexionTrefp2.convertirHexAFecha(
                                    TimeStampR
                                )
                            )
                            if (estiloActual/*.equals(styleceldaOscura)*/) TimeStamp.setCellStyle(
                                styleceldaOscura
                            ) else TimeStamp.setCellStyle(styleceldaClara)


                            val Temp1 = filaTIME.createCell(colNumTIME++)
                            Temp1.setCellValue(Temp1R)
                            if (estiloActual/*.equals(styleceldaOscura)*/) Temp1.setCellStyle(
                                styleceldaOscura
                            ) else Temp1.setCellStyle(styleceldaClara)

                            val Temp2 = filaTIME.createCell(colNumTIME++)
                            Temp2.setCellValue(Temp2R)
                            if (estiloActual/*.equals(styleceldaOscura)*/) Temp2.setCellStyle(
                                styleceldaOscura
                            ) else Temp2.setCellStyle(styleceldaClara)

                            val voltaje = filaTIME.createCell(colNumTIME++)
                            voltaje.setCellValue("${conexionTrefp2.getDecimal(voltajeR)}  Vca")  //" + conexionTrefp.getDecimal(voltaje) + " Vca") //voltajeR)
                            if (estiloActual/*.equals(styleceldaOscura)*/) voltaje.setCellStyle(
                                styleceldaOscura
                            ) else voltaje.setCellStyle(styleceldaClara)

                            val Original = filaTIME.createCell(colNumTIME++)
                            Original.setCellValue(dato)
                            if (estiloActual/*.equals(styleceldaOscura)*/) Original.setCellStyle(
                                styleceldaOscura
                            ) else Original.setCellStyle(styleceldaClara)

                            //     estiloActual = false
                            index++
                            // Alternar entre estilos
                            if (estiloActual) {
                                Iteración.setCellStyle(styleceldaOscura)
                            } else {
                                Iteración.setCellStyle(styleceldaClara)
                            }
                            estiloActual = !estiloActual
                            colNumTIME = 0
                        }
                    }
                }
                index = index + 5

                val currentTime = Calendar.getInstance().time
                var fila = sheet.createRow(index++)
                var Creador = fila.createCell(1)
                //temperature1p1Celda.setCellValue(temperature1p1.toString())
                // Creador.setCellValue(dato.second)

                when (sp!!.getString("userjerarquia", "")) {
                    "1" -> {
                        Creador.setCellValue(
                            "Creador:" + sp!!.getString(
                                "userId",
                                ""
                            ) + " - Jerarquía: Administrador"
                        )
                        Creador.setCellStyle(styleceldainfo)
                    }

                    "4" -> {
                        Creador.setCellValue(
                            "Creador:" + sp!!.getString(
                                "userId",
                                ""
                            ) + " - Jerarquía: Producción"
                        )
                        Creador.setCellStyle(styleceldainfo)
                    }
                }
                fila = sheet.createRow(index++)
                Creador = fila.createCell(1)

                Creador.setCellValue("Fecha:$currentTime")
                Creador.setCellStyle(styleceldainfo)

                fila = sheet.createRow(index++)
                Creador = fila.createCell(1)
                Creador.setCellValue(
                    "Nombre: " + sp!!.getString(
                        "name",
                        ""
                    ) + "    MAC:  " + sp!!.getString("mac", "")
                )
                Creador.setCellStyle(styleceldainfo)

                fila = sheet.createRow(index++)
                Creador = fila.createCell(1)
                Creador.setCellValue("Versión ImberaP:" )
                Creador.setCellStyle(styleceldainfo)
                data?.clear()
            }

        }

    }




    private fun ExcelEvent(data: MutableList<String>?) {
        var name = "LoggerEvent ${sp!!.getString("mac", "")} ${Calendar.getInstance().time}  "
        val nombreFile = "$name.xlsx"
        //  val file = File(getExternalFilesDir(null), nombreFile)
        val file: File =
            File(this.getExternalFilesDir(null), nombreFile)

        var outputStream: FileOutputStream? = null

        // Crear un nuevo libro de Excel
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Time")

        val style: CellStyle = workbook.createCellStyle()
        val styleceldaOscura: CellStyle = workbook.createCellStyle()
        val styleceldaClara: CellStyle = workbook.createCellStyle()
        val styleceldainfo: CellStyle = workbook.createCellStyle()

        style.fillForegroundColor =
            IndexedColors.DARK_TEAL.getIndex() //encabezado

        styleceldaOscura.fillForegroundColor =
            IndexedColors.PALE_BLUE.getIndex() //celda oscura

        styleceldaClara.fillForegroundColor =
            IndexedColors.LIGHT_TURQUOISE1.getIndex() //celda clara

        styleceldainfo.fillForegroundColor =
            IndexedColors.YELLOW.getIndex() //celda clara


        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        val font = workbook.createFont()
        font.color = IndexedColors.WHITE.index // Establece el color de la fuente a negro
        style.setFont(font)
        styleceldaOscura.fillPattern = FillPatternType.SOLID_FOREGROUND
        styleceldaClara.fillPattern = FillPatternType.SOLID_FOREGROUND
        styleceldainfo.fillPattern = FillPatternType.SOLID_FOREGROUND


        var filaNum = 0


        val encabezados =
            arrayOf(
                "Iteración",
                "TimeStampStart",
                "TimeStampEnd",
                "Temperatura 1",
                "Temperatura 1",
                "Voltaje",
                "TipoEvent",
                "Original"
            )
        val encabezadosFila = sheet.createRow(filaNum++)
        var colNum = 0

        for (encabezado in encabezados) {
            val celda = encabezadosFila.createCell(colNum++)
            celda.setCellValue(encabezado)
            celda.setCellStyle(style);
        }
        var estiloActual =
            true //styleceldaOscura // Inicia con el estilo oscuro
        var index = 1

        if (data != null) {
            for (dato in data) {


                val indiceReal = index
                var TimeStampStartR = dato.substring(0, 8)
                var TimeStampEndR = dato.substring(8, 16)
                var voltajeR = dato.substring(26, 28)
                var TipoEventR = dato.substring(16, 18)

                var Ev = when (getDecimal(dato.substring(16, 18))) {
                    1 -> "Evento 1: Apertura de Puerta"
                    2 -> "Evento 2: Ciclo de Compresor"
                    3 -> "Evento 3: Ciclo de Deshielo"
                    4 -> "Evento 4: Falla de Energía"
                    5 -> "Evento 5: Alarma"
                    else -> "Evento desconocido"
                }

                var Temp2R = conexionTrefp2.ReturnValLoggerTraduccion(dato.substring(22, 26))
                var Temp1R = conexionTrefp2.ReturnValLoggerTraduccion(dato.substring(18, 22))

                val fila = sheet.createRow(filaNum++)
                colNum = 0
                val Iteración = fila.createCell(colNum++)
                Iteración.setCellValue(index.toString())
                if (estiloActual/*.equals(styleceldaOscura)*/) Iteración.setCellStyle(
                    styleceldaOscura
                ) else Iteración.setCellStyle(styleceldaClara)


                val TimeStamp = fila.createCell(colNum++)
                TimeStamp.setCellValue(conexionTrefp2.convertHexToHumanDateLogger(TimeStampStartR))
                if (estiloActual/*.equals(styleceldaOscura)*/) TimeStamp.setCellStyle(
                    styleceldaOscura
                ) else TimeStamp.setCellStyle(styleceldaClara)


                val TimeStampEnd = fila.createCell(colNum++)
                TimeStampEnd.setCellValue(conexionTrefp2.convertHexToHumanDateLogger(TimeStampEndR))
                if (estiloActual/*.equals(styleceldaOscura)*/) TimeStampEnd.setCellStyle(
                    styleceldaOscura
                ) else TimeStampEnd.setCellStyle(styleceldaClara)

                val Temp1 = fila.createCell(colNum++)
                Temp1.setCellValue(Temp1R)
                if (estiloActual/*.equals(styleceldaOscura)*/) Temp1.setCellStyle(
                    styleceldaOscura
                ) else Temp1.setCellStyle(styleceldaClara)

                val Temp2 = fila.createCell(colNum++)
                Temp2.setCellValue(Temp2R)
                if (estiloActual/*.equals(styleceldaOscura)*/) Temp2.setCellStyle(
                    styleceldaOscura
                ) else Temp2.setCellStyle(styleceldaClara)


                val voltaje = fila.createCell(colNum++)
                voltaje.setCellValue("${conexionTrefp2.getDecimal(voltajeR)}  Vca")  //" + conexionTrefp.getDecimal(voltaje) + " Vca") //voltajeR)
                if (estiloActual/*.equals(styleceldaOscura)*/) voltaje.setCellStyle(
                    styleceldaOscura
                ) else voltaje.setCellStyle(styleceldaClara)

                val TipoEvent = fila.createCell(colNum++)
                TipoEvent.setCellValue(Ev)  //" + conexionTrefp.getDecimal(voltaje) + " Vca") //voltajeR)
                if (estiloActual/*.equals(styleceldaOscura)*/) TipoEvent.setCellStyle(
                    styleceldaOscura
                ) else TipoEvent.setCellStyle(styleceldaClara)

                val Original = fila.createCell(colNum++)
                Original.setCellValue(dato)
                if (estiloActual/*.equals(styleceldaOscura)*/) Original.setCellStyle(
                    styleceldaOscura
                ) else Original.setCellStyle(styleceldaClara)


                index++
            }
        }


        //se agregan los datos de quien lo genera y la fecha etc

        index = index + 5

        val currentTime = Calendar.getInstance().time
        var fila = sheet.createRow(index++)
        var Creador = fila.createCell(1)
        //temperature1p1Celda.setCellValue(temperature1p1.toString())
        // Creador.setCellValue(dato.second)

        when (sp!!.getString("userjerarquia", "")) {
            "1" -> {
                Creador.setCellValue(
                    "Creador:" + sp!!.getString(
                        "userId",
                        ""
                    ) + " - Jerarquía: Administrador"
                )
                Creador.setCellStyle(styleceldainfo)
            }

            "4" -> {
                Creador.setCellValue(
                    "Creador:" + sp!!.getString(
                        "userId",
                        ""
                    ) + " - Jerarquía: Producción"
                )
                Creador.setCellStyle(styleceldainfo)
            }
        }
        fila = sheet.createRow(index++)
        Creador = fila.createCell(1)

        Creador.setCellValue("Fecha:$currentTime")
        Creador.setCellStyle(styleceldainfo)

        fila = sheet.createRow(index++)
        Creador = fila.createCell(1)
        Creador.setCellValue(
            "Nombre: " + sp!!.getString(
                "name",
                ""
            ) + "    MAC:  " + sp!!.getString("mac", "")
        )
        Creador.setCellStyle(styleceldainfo)

        fila = sheet.createRow(index++)
        Creador = fila.createCell(1)
        // Creador.setCellValue("Versión ImberaP:" + BuildConfig.VERSION_NAME)
        Creador.setCellStyle(styleceldainfo)

        outputStream = FileOutputStream(file)
        workbook.write(outputStream)
        outputStream.close()

        try {

            this.runOnUiThread {
                Toast.makeText(
                    this,
                    "Reporte generado correctamente",
                    Toast.LENGTH_LONG
                ).show()
            }
            try {

                setupEmailEvent(
                    "Datos tipo TIempo",
                    "Archivo generado en la fecha:" + Calendar.getInstance().time,
                    nombreFile
                )
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        } catch (e: IOException) {
            this.runOnUiThread {
                Toast.makeText(
                    this,
                    "Reporte no generado",
                    Toast.LENGTH_LONG
                ).show()
            }
            e.printStackTrace()
        }
    }


    fun pruebaXY() {

    }

    fun createExcelTimeData(name: String?, data: List<String?>?) {
        val nombreFile = "DatosTipoTiempo.xls"
        val file = File(getExternalFilesDir(null), nombreFile)
        var outputStream: FileOutputStream? = null
        val wb = HSSFWorkbook()
        val hssfSheet = wb.createSheet("Datos tipo Tiempo")
        var hssfRow = hssfSheet.createRow(0)
        var hssfCell = hssfRow.createCell(0)

        // Se agregan los datos de quien lo genera y la fecha, etc.
        val currentTime = Calendar.getInstance().time
        hssfCell.setCellValue("Creador: " + sp!!.getString("userId", ""))
        hssfRow = hssfSheet.createRow(1)
        hssfCell = hssfRow.createCell(0)
        when (sp!!.getString("userjerarquia", "")) {
            "1" -> {
                hssfCell.setCellValue("Jerarquía: Administrador")
            }

            "4" -> {
                hssfCell.setCellValue("Jerarquía: Producción")
            }

            else -> {
                hssfCell.setCellValue("Jerarquía: Pruebas")
            }
        }
        hssfRow = hssfSheet.createRow(2)
        hssfCell = hssfRow.createCell(0)
        hssfCell.setCellValue("Fecha: $currentTime")

        // Títulos
        hssfRow = hssfSheet.createRow(4)
        hssfCell = hssfRow.createCell(0)
        hssfCell.setCellValue("TimeStamp1")
        hssfCell = hssfRow.createCell(1)
        hssfCell.setCellValue("Temperatura 1")
        hssfCell = hssfRow.createCell(2)
        hssfCell.setCellValue("Temperatura 2")
        hssfCell = hssfRow.createCell(3)
        hssfCell.setCellValue("Voltaje")

        val numeroReg = data!!.size / 4
        var numcons = 0

        for (i in 0 until numeroReg) {
            hssfRow = hssfSheet.createRow(i + 5)
            for (j in 0..3) {
                hssfCell = hssfRow.createCell(j)
                hssfCell.setCellValue(data[numcons])
                numcons++
            }
        }

        try {
            outputStream = FileOutputStream(file)
            wb.write(outputStream)

            /* try {
                 outputStream.close()
                 setupEmailEvent(
                     "Datos tipo Tiempo",
                     "Archivo generado en la fecha: " + Calendar.getInstance().time,
                     nombreFile
                 )
             } catch (ex: IOException) {
                 ex.printStackTrace()
             }*/
        } catch (e: IOException) {
            Toast.makeText(this.applicationContext, "Reporte no generado", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun CreateExcelTIME(fileName: String, datos: MutableList<String>?) {
        val newData: MutableList<String> = ArrayList()
        val data = listOf(
            "612F6B4700C3FE3480",
            "612F6C7300C3FE3478",
            "612F6D9F00C3FE3480",
            "612F6ECB00C2FE347E",
            "612F6FF700C2FE347F"
        )

        val nombreFile = "DatosTipoTiempo.xls"
        val file = File(getExternalFilesDir(null), nombreFile)
        var outputStream: FileOutputStream? = null


        if (datos.isNullOrEmpty()) {
            MakeToast("La lista de datos está vacía.")
            return
        }

        // Crear un nuevo libro de Excel
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("ExcelTIME")

        var filaNum = 0

        // Escribir los encabezados
        val encabezados =
            arrayOf("Timestamp", "Temperature 1P1", "Temperature 2P1", "Voltage", "Original")
        val encabezadosFila = sheet.createRow(filaNum++)
        var colNum = 0
        for (encabezado in encabezados) {
            val celda = encabezadosFila.createCell(colNum++)
            celda.setCellValue(encabezado)
        }

        // Escribir los datos en la hoja de Excel
        for (dato in datos) {
            val fila = sheet.createRow(filaNum++)
            colNum = 0

            //////////////////////////////////////////////////////
            var newDatatemperature1p1: String? = null
            var numf = GetRealDataFromHexaImbera.getDecimalFloat(dato.substring(8, 12))
            var num = numf.toInt()
            if (num < 99.99) {
                //   newData.add(
                newDatatemperature1p1 = GetRealDataFromHexaImbera.getDecimalFloat(
                    dato.substring(
                        8,
                        12
                    )
                ).toString()
                //  ) //decimales con punto //get temp positivo
            } else if (num > 99.99) {
                // newData.add(
                newDatatemperature1p1 = GetRealDataFromHexaImbera.getNegativeTemp(
                    "FFFF" + dato.substring(
                        8,
                        12
                    )
                )
                //  ) //get negativos
            } else { //Es 0 cero
                newDatatemperature1p1 = "0000"
                //  newData.add("0000") //get negativos
            }

            //////////.///////////////////////////////////////////
            var newDatatemperature2p2: String? = null

            val numf2 = GetRealDataFromHexaImbera.getDecimalFloat(dato.substring(12, 16))
            var num2 = numf2.toInt()
            num2 = numf2.toInt()
            if (num2 < 99.99) {
                //  newData.add(
                newDatatemperature2p2 = GetRealDataFromHexaImbera.getDecimalFloat(
                    dato.substring(
                        12,
                        16
                    )
                ).toString()
                //  ) //decimales con punto //get temp positivo
            } else if (num2 > 99.99) {
                //newData.add(getNegativeTemp("FFFF"+data.get(i).substring(22,26))); //get negativos

                //  newData.add(
                newDatatemperature2p2 = GetRealDataFromHexaImbera.getNegativeTemp(
                    "FFFF" + dato.substring(
                        12,
                        16
                    )
                )
                //  ) //get negativos
            } else { //Es 0 cero
                //  newData.add("0000") //get negativos
                newDatatemperature2p2 = "0000"
            }
            ///////////////////////////////////////////////////////

            // Extraer los valores de la cadena de entrada
            val timestamp = dato.substring(0, 8)
            val temperature1p1 = GetRealDataFromHexaImbera.getDecimalFloat(
                dato.substring(
                    8,
                    12
                )
            )//dato.substring(8, 12)
            val temperature2p1 = dato.substring(12, 16)
            val voltage = GetRealDataFromHexaImbera.getDecimal(dato.substring(16))
                .toString()//dato.substring(16, 18)
            //    val checksum = dato.substring(20)

            val timestampCelda = fila.createCell(colNum++)
            timestampCelda.setCellValue(convertirHexAFechaCORTA(timestamp))

            val temperature1p1Celda = fila.createCell(colNum++)
            //temperature1p1Celda.setCellValue(temperature1p1.toString())
            temperature1p1Celda.setCellValue(newDatatemperature1p1)
            val temperature2p1Celda = fila.createCell(colNum++)
            //  temperature2p1Celda.setCellValue(temperature2p1.toString())
            temperature2p1Celda.setCellValue(newDatatemperature2p2)
            val voltageCelda = fila.createCell(colNum++)
            voltageCelda.setCellValue(voltage.toString())
            val OriginalCelda = fila.createCell(colNum++)
            OriginalCelda.setCellValue(dato)

            /* val checksumCelda = fila.createCell(colNum)
             checksumCelda.setCellValue(checksum)*/
        }

        // Ajustar automáticamente el ancho de las columnas
        /*  for (colNum in 0 until encabezados.size) {
              sheet.autoSizeColumn(colNum)
          }
  */

        // Guardar el libro de Excel en un archivo
        /*  val nombreArchivo =
              Environment.getExternalStorageDirectory().absolutePath + File.separator + "datos.xlsx"

          val archivoSalida = FileOutputStream(nombreArchivo)
          workbook.write(archivoSalida)
          archivoSalida.close()*/

        try {
            outputStream = FileOutputStream(file)
            workbook.write(outputStream)
            Toast.makeText(
                this.applicationContext,
                "Reporte generado correctamente",
                Toast.LENGTH_LONG
            ).show()
            try {
                outputStream.close()
                setupEmailEvent(
                    "Datos tipo TIempo",
                    "Archivo generado en la fecha:" + Calendar.getInstance().time,
                    nombreFile
                )
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        } catch (e: IOException) {
            Toast.makeText(this.applicationContext, "Reporte no generado", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }

    }

    private fun CreateExcelEVENT(fileName: String, result: List<String>?) {
        val workbook = XSSFWorkbook()
        if (result.isNullOrEmpty()) {
            MakeToast("La lista de datos está vacía.")
            return
        }
        val nombreFile = "DatosTipoEvento${LocalDateTime.now()}.xls"
        val file = File(getExternalFilesDir(null), nombreFile)
        var outputStream: FileOutputStream? = null
        val newData: MutableList<String> = ArrayList()

        // Crear un nuevo libro de Excel
        val fechaHoraActual = LocalDateTime.now()
        val formato = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss")
        val fechaHoraFormateada = fechaHoraActual.format(formato)
        val sheet = workbook.createSheet("$fechaHoraFormateada")

        var filaNum = 0

        // Escribir los encabezados
        val encabezados = arrayOf(
            "TIMESTAMP_START",
            "TIMESTAMP_END",
            "EVENT_TYPE",
            "TEMPERATURE_1I",
            "TEMPERATURE_2F",
            "VOLTAGE", "Original"
        )
        val encabezadosFila = sheet.createRow(filaNum++)
        var colNum = 0
        for (encabezado in encabezados) {
            val celda = encabezadosFila.createCell(colNum++)
            celda.setCellValue(encabezado)
        }

        // Escribir los datos en la hoja de Excel
        for (dato in result) {
            val fila = sheet.createRow(filaNum++)
            colNum = 0

            //////////////////////////////////////////////////////
            var newDatatemperature1p1: String? = null
            var numf = GetRealDataFromHexaImbera.getDecimalFloat(dato.substring(18, 22))
            var num = numf.toInt()
            if (num < 99.99) {
                //   newData.add(
                newDatatemperature1p1 = GetRealDataFromHexaImbera.getDecimalFloat(
                    dato.substring(
                        18, 22
                    )
                ).toString()
                //  ) //decimales con punto //get temp positivo
            } else if (num > 99.99) {
                // newData.add(
                newDatatemperature1p1 = GetRealDataFromHexaImbera.getNegativeTemp(
                    "FFFF" + dato.substring(
                        18, 22
                    )
                )
                //  ) //get negativos
            } else { //Es 0 cero
                newDatatemperature1p1 = "0000"
                //  newData.add("0000") //get negativos
            }

            /////////////////////////////////////////////////////
            var newDatatemperature2p2: String? = null

            val numf2 = GetRealDataFromHexaImbera.getDecimalFloat(dato.substring(22, 26))
            var num2 = numf2.toInt()
            num2 = numf2.toInt()
            if (num2 < 99.99) {
                //  newData.add(
                newDatatemperature2p2 = GetRealDataFromHexaImbera.getDecimalFloat(
                    dato.substring(
                        22, 26
                    )
                ).toString()
                //  ) //decimales con punto //get temp positivo
            } else if (num2 > 99.99) {
                //newData.add(getNegativeTemp("FFFF"+data.get(i).substring(22,26))); //get negativos

                //  newData.add(
                newDatatemperature2p2 = GetRealDataFromHexaImbera.getNegativeTemp(
                    "FFFF" + dato.substring(
                        22, 26
                    )
                )
                //  ) //get negativos
            } else { //Es 0 cero
                //  newData.add("0000") //get negativos
                newDatatemperature2p2 = "0000"
            }
            ///////////////////////////////////////////////////////

            val timestampStart = convertirHexAFechaCORTA(dato.substring(0, 8))
            val timestampEnd = convertirHexAFechaCORTA(dato.substring(8, 16))
            val eventType = dato.substring(16, 18)
            val temperature1i = dato.substring(18, 22)
            val temperature2f = dato.substring(22, 26)
            //val voltage = dato.substring(26, 28)
            val voltage = GetRealDataFromHexaImbera.getDecimal(dato.substring(26, 28))
                .toString()//dato.substring(16, 18)
            val timestampStartCelda = fila.createCell(colNum++)
            timestampStartCelda.setCellValue(timestampStart)

            val timestampEndCelda = fila.createCell(colNum++)
            timestampEndCelda.setCellValue(timestampEnd)

            val eventTypeCelda = fila.createCell(colNum++)
            eventTypeCelda.setCellValue(eventType)

            /*   val temperature1iCelda = fila.createCell(colNum++)
               temperature1iCelda.setCellValue(temperature1i)

               val temperature2fCelda = fila.createCell(colNum++)
               temperature2fCelda.setCellValue(temperature2f)
               */
            val temperature1p1Celda = fila.createCell(colNum++)
            //temperature1p1Celda.setCellValue(temperature1p1.toString())
            temperature1p1Celda.setCellValue(newDatatemperature1p1)
            val temperature2p1Celda = fila.createCell(colNum++)
            //  temperature2p1Celda.setCellValue(temperature2p1.toString())
            temperature2p1Celda.setCellValue(newDatatemperature2p2)

            val voltageCelda = fila.createCell(colNum++)
            voltageCelda.setCellValue(voltage)

            val OriginalCelda = fila.createCell(colNum++)
            OriginalCelda.setCellValue(dato)


        }

        // Ajustar automáticamente el ancho de las columnas
        /*   for (colNum in 0 until encabezados.size) {
               sheet.autoSizeColumn(colNum)
           }*/

        try {
            outputStream = FileOutputStream(file)
            workbook.write(outputStream)
            Toast.makeText(
                this.applicationContext,
                "Reporte generado correctamente",
                Toast.LENGTH_LONG
            ).show()
            try {
                outputStream.close()
                setupEmailEvent(
                    "Datos tipo Evento",
                    "Archivo generado en la fecha:" + Calendar.getInstance().time,
                    nombreFile
                )
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        } catch (e: IOException) {
            Toast.makeText(this.applicationContext, "Reporte no generado", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }


        // Guardar el libro de Excel en un archivo
        /*   val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
               addCategory(Intent.CATEGORY_OPENABLE)
               type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
               putExtra(Intent.EXTRA_TITLE, "$fileName.xlsx")
           }

           startActivityForResult(intent, REQUEST_SAVE_EXCEL_FILE)
           */
        /*    try {
                outputStream = FileOutputStream(file)
                workbook.write(outputStream)

                try {
                    outputStream.close()
                    setupEmailEvent(
                        "Datos tipo Tiempo",
                        "Archivo generado en la fecha:" + Calendar.getInstance().time,
                        nombreFile
                    )
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            } catch (e: IOException) {
                Toast.makeText(this@MainActivity, "Reporte no generado", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
    */
        /*           saveFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
               uri?.let { saveExcelFile(it) }
           }

           // Lanzar el diálogo de guardar archivo cuando sea necesario
           showSaveFileDialog()
           */
    }

    fun setupEmailEvent(titulo: String, descripcion: String, filestring: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/excel"
        //intent.putExtra(Intent.EXTRA_EMAIL, email);
        intent.putExtra(Intent.EXTRA_SUBJECT, titulo)
        intent.putExtra(Intent.EXTRA_TEXT, descripcion)
        val file = File(getExternalFilesDir(null), filestring)
        if (file.exists()) {
            Log.v("TEST EMAIL", "Email file_exists!")
        } else {
            Log.v("TEST EMAIL", "Email file does not exist!")
        }
        val uri = FileProvider.getUriForFile(
            this,
            "com.example.trepfp.MainActivity.provider",
            file
        )
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        this.startActivity(Intent.createChooser(intent, "Send mail..."))
    }

    private fun showSaveFileDialog() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_TITLE, "archivo.xlsx")
        }

        saveFileLauncher.launch("archivo.xlsx")
    }

    private fun saveExcelFile(uri: android.net.Uri) {
        try {
            val outputStream: OutputStream? = contentResolver.openOutputStream(uri)
            outputStream?.use { stream ->
                // Crear el archivo de Excel y escribir los datos
                val workbook = XSSFWorkbook()
                // ... Código para escribir los datos en el libro de Excel ...

                // Guardar el libro de Excel en el archivo
                workbook.write(stream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)



        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                readTextFile(uri)
            }
        }

    }


    fun exportarDatosAExcelPRUEBA() {
        val data = listOf(
            "612F6B4700C3FE3480",
            "612F6C7300C3FE3478",
            "612F6D9F00C3FE3480",
            "612F6ECB00C2FE347E",
            "612F6FF700C2FE347F"
        )

        if (data.isNullOrEmpty()) {
            // La lista de datos está vacía.
            return
        }

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Datos")

        var filaNum = 0

        val encabezados = arrayOf("Timestamp", "Temperature 1P1", "Temperature 2P1", "Voltage")
        val encabezadosFila = sheet.createRow(filaNum++)
        var colNum = 0
        for (encabezado in encabezados) {
            val celda = encabezadosFila.createCell(colNum++)
            celda.setCellValue(encabezado)
        }

        for (dato in data) {
            val fila = sheet.createRow(filaNum++)
            colNum = 0

            val timestamp = dato.substring(0, 8)
            val temperature1p1 = dato.substring(8, 12)
            val temperature2p1 = dato.substring(12, 16)
            val voltage = dato.substring(16, 18)

            val timestampCelda = fila.createCell(colNum++)
            timestampCelda.setCellValue(timestamp)

            val temperature1p1Celda = fila.createCell(colNum++)
            temperature1p1Celda.setCellValue(temperature1p1)

            val temperature2p1Celda = fila.createCell(colNum++)
            temperature2p1Celda.setCellValue(temperature2p1)

            val voltageCelda = fila.createCell(colNum++)
            voltageCelda.setCellValue(voltage)
        }

        for (colNum in 0 until encabezados.size) {
            sheet.autoSizeColumn(colNum)
        }

        println("Ingrese la dirección de guardado del archivo (incluyendo el nombre y extensión):")
        val direccionGuardado = readLine()

        if (direccionGuardado.isNullOrEmpty()) {
            // No se proporcionó una dirección de guardado válida. Los datos no se exportaron.
            return
        }

        val archivoSalida = FileOutputStream(direccionGuardado)
        workbook.write(archivoSalida)
        archivoSalida.close()

        println("Los datos se han exportado correctamente al archivo '$direccionGuardado'.")
    }

    fun exportarDatosAExcel2(datos: MutableList<String>?) {
        if (datos.isNullOrEmpty()) {
            //  MakeToast("La lista de datos está vacía.")
            return
        }

        // Crear un nuevo libro de Excel
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Datos")

        var filaNum = 0

        // Escribir los encabezados
        val encabezados = arrayOf("Timestamp", "Temperature 1P1", "Temperature 2P1", "Voltage")
        val encabezadosFila = sheet.createRow(filaNum++)
        var colNum = 0
        for (encabezado in encabezados) {
            val celda = encabezadosFila.createCell(colNum++)
            celda.setCellValue(encabezado)
        }
        val datoX = listOf(
            "612F6B4700C3FE3480",
            "612F6C7300C3FE3478",
            "612F6D9F00C3FE3480",
            "612F6ECB00C2FE347E",
            "612F6FF700C2FE347F"

        )

        // Escribir los datos en la hoja de Excel
        for (dato in datos) {

            val fila = sheet.createRow(filaNum++)
            colNum = 0

            // Extraer los valores de la cadena de entrada
            val timestamp = dato.substring(0, 8)
            val temperature1p1 = dato.substring(8, 12)
            val temperature2p1 = dato.substring(12, 16)
            val voltage = dato.substring(16, 18)

            val timestampCelda = fila.createCell(colNum++)
            timestampCelda.setCellValue(timestamp)

            val temperature1p1Celda = fila.createCell(colNum++)
            temperature1p1Celda.setCellValue(temperature1p1)

            val temperature2p1Celda = fila.createCell(colNum++)
            temperature2p1Celda.setCellValue(temperature2p1)

            val voltageCelda = fila.createCell(colNum++)
            voltageCelda.setCellValue(voltage)
        }

        // Ajustar automáticamente el ancho de las columnas
        for (colNum in 0 until encabezados.size) {
            sheet.autoSizeColumn(colNum)
        }

        // Pedir al usuario la dirección de guardado
        //    MakeToast("Ingrese la dirección de guardado del archivo (incluyendo el nombre y extensión):")
        val direccionGuardado = readLine()

        if (direccionGuardado.isNullOrEmpty()) {
            //     MakeToast("No se proporcionó una dirección de guardado válida. Los datos no se exportaron.")
            return
        }

        // Guardar el libro de Excel en un archivo
        val archivoSalida = FileOutputStream(direccionGuardado)
        workbook.write(archivoSalida)
        archivoSalida.close()
        Log.d("DatosPrint", "direccionGuardado $direccionGuardado")
        println("Los datos se han exportado correctamente al archivo '$direccionGuardado'.")
    }

    fun exportarDatosAExcel(datos: MutableList<String>?) {
        if (datos.isNullOrEmpty()) {
            MakeToast("La lista de datos está vacía.")
            return
        }

        // Crear un nuevo libro de Excel
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Datos")

        var filaNum = 0

        // Escribir los encabezados
        val encabezados =
            arrayOf("Timestamp", "Temperature 1P1", "Temperature 2P1", "Voltage", "Checksum")
        val encabezadosFila = sheet.createRow(filaNum++)
        var colNum = 0
        for (encabezado in encabezados) {
            val celda = encabezadosFila.createCell(colNum++)
            celda.setCellValue(encabezado)
        }

        // Escribir los datos en la hoja de Excel
        for (dato in datos) {
            val fila = sheet.createRow(filaNum++)
            colNum = 0

            // Extraer los valores de la cadena de entrada
            val timestamp = dato.substring(0, 8)
            val temperature1p1 = dato.substring(8, 12)
            val temperature2p1 = dato.substring(12, 16)
            val voltage = dato.substring(16, 18)
            //    val checksum = dato.substring(20)

            val timestampCelda = fila.createCell(colNum++)
            timestampCelda.setCellValue(timestamp)

            val temperature1p1Celda = fila.createCell(colNum++)
            temperature1p1Celda.setCellValue(temperature1p1.toDouble())

            val temperature2p1Celda = fila.createCell(colNum++)
            temperature2p1Celda.setCellValue(temperature2p1.toDouble())

            val voltageCelda = fila.createCell(colNum++)
            voltageCelda.setCellValue(voltage.toDouble())

            /* val checksumCelda = fila.createCell(colNum)
             checksumCelda.setCellValue(checksum)*/
        }

        // Ajustar automáticamente el ancho de las columnas
        for (colNum in 0 until encabezados.size) {
            sheet.autoSizeColumn(colNum)
        }

        // Guardar el libro de Excel en un archivo
        val nombreArchivo = "datos.xlsx"
        val archivoSalida = FileOutputStream(nombreArchivo)
        workbook.write(archivoSalida)
        archivoSalida.close()

        MakeToast("Los datos se han exportado correctamente al archivo '$nombreArchivo'.")
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
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this@MainActivity)

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


    class LocationHelper(private val context: Context) {
        private val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        fun getLocation(listener: ConexionTrefp.LocationResultListener) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                listener.onLocationError("No se han concedido permisos de ubicación.")
                return
            }

            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastLocation != null) {
                listener.onLocationResult(lastLocation.latitude, lastLocation.longitude)
            } else {
                val locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        listener.onLocationResult(latitude, longitude)
                        locationManager.removeUpdates(this)
                    }

                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                }
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0L,
                    0f,
                    locationListener
                )
            }
        }
    }

    fun MakeToast(Text: String) {
        Toast.makeText(
            this@MainActivity,
            Text,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun LOGGER() {
        listaTemporalFINAL.clear()
        val registros = listOf(
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


            )


        /*   val subListas = mutableListOf<List<String>>()
           var subListaActual = mutableListOf<String>()
           var fechaAnterior = ""

           for (registro in result) {
               val fechaActual = convertirHexAFecha(registro!!.substring(0, 8))

               if (fechaAnterior.isNotEmpty() && fechaActual > fechaAnterior) {
                   subListas.add(subListaActual)
                   subListaActual = mutableListOf()
               }

               subListaActual.add(registro!!)
               fechaAnterior = fechaActual
           }

           subListas.add(subListaActual)

          println(subListas)*/


        //  var sublists = divideLista(registros as MutableList<String?>)
        println(registros.size)
        val result = dataFECHA(
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
                val timestamp = (result[i]!!.substring(0, 8).toLong(16) * 1000) - 3600000
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

        /*
                for (i in 0 until result.size -1 ) {


                    val fechaHora1 = Date(result[i]!!.substring(0, 8).toLong(16) * 1000)
                    val fechaHora2 = Date(result[i + 1]!!.substring(0, 8).toLong(16) * 1000)

                    listaTemporal.add(result[i]!!)
                   // println(" ${result[i]} $i  fechaHora1 $fechaHora1 fechaHora2 $fechaHora2 ")
                    if (fechaHora1.compareTo(fechaHora2) > 0) {
                        resultado.add(listaTemporal.toList())
                        listaTemporal.clear()


                    }
                }

                listaTemporal.add(registros.last()!!)
                resultado.add(listaTemporal.toList())

               println(resultado)
                println("listaTemporalFINAL   $listaTemporalFINAL")

         */
        /* NuevaLista.map {
             Log.d("AJUSTETIEMPO--------------","$it ")
         }
           */


        /*    for (i in result.size -1  downTo 0) {


                val fechaActual = Date(result[i]!!.substring(0, 8).toLong(16) * 1000)
                val fechaMenor = Date(result[i + 1]!!.substring(0, 8).toLong(16) * 1000)


                if (fechaActual.compareTo(fechaMenor) > 0) {
                    Log.d("DATOSEXAFECHA","fechaActual ${fechaActual} fechaMEnor $fechaMenor  ")
                }



            }*/

        /*  val q = convertAndAdjustDates(result as List<String>)

          q.map {
              Log.d("AJUSTETIEMPO--------------","${it}")
          }
          */

        /* val r = divideLista(result)
         println(r)

       /*  subListas.map {
         */
             println(it)
         }*/

    }

    private fun LOGGERTIME(registros: MutableList<String>?) {
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


        /*   val subListas = mutableListOf<List<String>>()
           var subListaActual = mutableListOf<String>()
           var fechaAnterior = ""

           for (registro in result) {
               val fechaActual = convertirHexAFecha(registro!!.substring(0, 8))

               if (fechaAnterior.isNotEmpty() && fechaActual > fechaAnterior) {
                   subListas.add(subListaActual)
                   subListaActual = mutableListOf()
               }

               subListaActual.add(registro!!)
               fechaAnterior = fechaActual
           }

           subListas.add(subListaActual)

          println(subListas)*/


        //  var sublists = divideLista(registros as MutableList<String?>)
        println(registros!!.size)
        Log.d("DATOSEXAFECHA", "${registros}")
        val result = dataFECHA(
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
                val timestamp = (result[i]!!.substring(0, 8).toLong(16) * 1000) - 3600000
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

        /*
                for (i in 0 until result.size -1 ) {


                    val fechaHora1 = Date(result[i]!!.substring(0, 8).toLong(16) * 1000)
                    val fechaHora2 = Date(result[i + 1]!!.substring(0, 8).toLong(16) * 1000)

                    listaTemporal.add(result[i]!!)
                   // println(" ${result[i]} $i  fechaHora1 $fechaHora1 fechaHora2 $fechaHora2 ")
                    if (fechaHora1.compareTo(fechaHora2) > 0) {
                        resultado.add(listaTemporal.toList())
                        listaTemporal.clear()


                    }
                }

                listaTemporal.add(registros.last()!!)
                resultado.add(listaTemporal.toList())

               println(resultado)
                println("listaTemporalFINAL   $listaTemporalFINAL")

         */
        /* NuevaLista.map {
             Log.d("AJUSTETIEMPO--------------","$it ")
         }
           */


        /*    for (i in result.size -1  downTo 0) {


                val fechaActual = Date(result[i]!!.substring(0, 8).toLong(16) * 1000)
                val fechaMenor = Date(result[i + 1]!!.substring(0, 8).toLong(16) * 1000)


                if (fechaActual.compareTo(fechaMenor) > 0) {
                    Log.d("DATOSEXAFECHA","fechaActual ${fechaActual} fechaMEnor $fechaMenor  ")
                }



            }*/

        /*  val q = convertAndAdjustDates(result as List<String>)

          q.map {
              Log.d("AJUSTETIEMPO--------------","${it}")
          }
          */

        /* val r = divideLista(result)
         println(r)

       /*  subListas.map {
         */
             println(it)
         }*/

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


        /*  val w2=  (result[0]!!.substring(0, 8).toLong(16) * 1000) -  (result[1]!!.substring(0, 8).toLong(16) * 1000)
          val anterior2 =  (listaTemporalFINAL.last().substring(0, 8).toLong(16) * 1000) - w2
          val date2 = Date(anterior2)
          val fechaHoraExadecimal2 =
              BigInteger.valueOf(date2.time / 1000).toString(16).padStart(8, '0').uppercase()
          listaTemporalFINAL.add(fechaHoraExadecimal2+result[0]!!.substring(8,result[0]!!.length))
          */
    }

    fun procesarRegistros(registros: List<String>): List<String> {


        println("registros antes del proceso ->  $registros")

        // Lista actualizada de registros
        val nuevosRegistros = mutableListOf<String>()

        // Variables auxiliares para procesar los registros
        var ultimoRegistro = ""
        var fechaUltimoRegistro = Date()

        // Procesar cada registro en la lista
        for (registro in registros) {

            // Obtener el timestamp del registro actual
            val timestampActual = try {
                registro.substring(0, 8).toLong(16)
            } catch (e: Exception) {
                // Si no se puede obtener el timestamp, pasar al siguiente registro
                continue
            }

            // Si es el primer registro, agregarlo a la lista actualizada y continuar
            if (ultimoRegistro.isEmpty()) {
                nuevosRegistros.add(registro)
                ultimoRegistro = registro
                fechaUltimoRegistro = Date(timestampActual * 1000)
                continue
            }

            // Obtener la fecha del último registro procesado
            val fechaUltimoRegistroActualizada = Date(fechaUltimoRegistro.time - 3600 * 1000)

            // Si el timestamp del registro actual es menor o igual al del último registro procesado,
            // agregarlo a la lista actualizada y continuar
            if (timestampActual <= ultimoRegistro.substring(0, 8).toLong(16)) {
                nuevosRegistros.add(registro)
                ultimoRegistro = registro
                fechaUltimoRegistro = fechaUltimoRegistroActualizada
                continue
            }

            // Si el timestamp del registro actual es mayor que el del último registro procesado,
            // calcular la diferencia de tiempo entre los dos registros y actualizar la fecha del último registro
            val diferencia = timestampActual - ultimoRegistro.substring(0, 8).toLong(16)
            fechaUltimoRegistro = Date(fechaUltimoRegistro.time - diferencia * 1000)

            // Crear el nuevo registro con el timestamp actualizado
            val nuevoRegistro = String.format(
                "%08X%s",
                fechaUltimoRegistro.time / 1000,
                ultimoRegistro.substring(8)
            )
            nuevosRegistros.add(nuevoRegistro)
            ultimoRegistro = nuevoRegistro
        }

        return nuevosRegistros
    }

    private fun divideLista(registros: MutableList<String?>): MutableList<List<String>> {

        var listaTemporal = mutableListOf<String>()
        var resultado = mutableListOf<List<String>>()

        for (i in 0 until registros.size - 1) {
            val fechaHora1 = Date(registros[i]!!.substring(0, 8).toLong(16) * 1000)
            val fechaHora2 = Date(registros[i + 1]!!.substring(0, 8).toLong(16) * 1000)

            listaTemporal.add(registros[i]!!)

            if (fechaHora1.compareTo(fechaHora2) > 0) {
                resultado.add(listaTemporal.toList())
                listaTemporal.clear()
            }
        }

        listaTemporal.add(registros.last()!!)
        resultado.add(listaTemporal.toList())
        return resultado
        // println(subListas)
    }

    private fun SALIDADATOSMENOSHORA(registros: MutableList<List<String>?>): MutableList<List<String>?> {
        var listaTemporal = mutableListOf<String>()
        var resultado = mutableListOf<List<String>>()

        val newData: MutableList<List<String>?> = ArrayList()

        newData.add(registros.last())
        for (i in registros.size - 1 downTo 0) {
            Log.d("REcorridoMAP", "${registros[0]!![0]!!}")


        }
        return newData
    }

    fun convertAndAdjustDates(list: List<String>): List<String> {
        val adjustedList = mutableListOf<String>()
        var previousDate: Long? = null
        var difference: Long? = null

        for (i in list.size - 1 downTo 0) {
            val timestamp = list[i].substring(0, 8).toLong(16) * 1000
            val date = Date(timestamp)

            if (previousDate != null && previousDate < timestamp) {
                val adjustedTimestamp = (timestamp - difference!!) / 1000 - 3600
                val adjustedHex = adjustedTimestamp.toString(16).toUpperCase().padStart(8, '0')
                adjustedList.add(list[i].replace(list[i].substring(0, 8), adjustedHex))
                difference = previousDate - timestamp
            } else if (previousDate != null && previousDate >= timestamp) {
                val timestampDifference = previousDate - timestamp
                if (difference != null && difference != timestampDifference) {
                    throw Exception("Inconsistent timestamp differences.")
                } else {
                    difference = timestampDifference
                }
                adjustedList.add(list[i])
            } else {
                adjustedList.add(list[i])
            }

            previousDate = timestamp
        }

        return adjustedList.reversed()
    }

    fun GETContext(): Context {
        return applicationContext
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

    /*    private fun askPermission() {
            if (Build.VERSION.SDK_INT >= 31) {
                val perms = arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                if (ContextCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                        applicationContext, Manifest.permission.BLUETOOTH_SCAN
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
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
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
                    "CORREWCTO"
                ) else Log.d("PERMISSION LOCATION ENABLED", "IJNININCORREWCTO")
            }
        }
    */

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
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.MANAGE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

                && ContextCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE
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

    /*
    fun obtenerUbicacionEnHexa(): Pair<String, String>? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext!!)
        val locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(1000)
            .setFastestInterval(500)
        var ubicacionHexa: Pair<String, String>? = null
        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    val latitudHexa = Integer.toHexString((location.latitude * 1E7).toInt())
                    val longitudHexa = Integer.toHexString((location.longitude * 1E7).toInt())
                    ubicacionHexa = Pair(latitudHexa, longitudHexa)
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
        }, null)
        return ubicacionHexa
    }*/


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

    override fun getInfo(data: MutableList<String>?) {
        TODO("Not yet implemented")
    }

    override fun onError(error: String) {
        TODO("Not yet implemented")
    }

    override fun onProgress(progress: String): String {
        TODO("Not yet implemented")
    }

    override fun onSuccess(result: Boolean): Boolean {
        TODO("Not yet implemented")
    }


    /*
        override fun onError(error: String?) {

        }

        override fun isConnectionAlive(resp: String?) {

            Log.d("isConnectionAlive", "$resp")
        }

        override fun getInfo(data: MutableList<String?>) {

            Log.d("getInfo", "$data")
        }


        override fun getInfoBytes(data: ByteArray?) {
            Log.d("getInfoBytes", "$data")
        }
    */

    /*
    override fun getList(): MutableList<String>? {
        return list
    }

    override fun getMacConnect(): MutableList<String>? {
        return listMcc
    }
*/


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

    private fun t(): String {
        val timeStampOriginal = GetHexFromRealDataImbera.getDecimal(
            "612F6D9F".substring(0, 8)
        ).toLong()
        val unixTime = System.currentTimeMillis() / 1000
        val diferencialTimeStamp = unixTime - timeStampOriginal
        return diferencialTimeStamp.toString(16).padStart(8, '0')
    }

    private fun dataFECHAPRUEBA(
        data: MutableList<List<String>?>,
        action: String
    ): MutableList<List<String>?> {
        return when (action) {
            "PRUEBA" -> {
                val newData: MutableList<List<String>?> = ArrayList()
                data?.let { data ->
                    var date: Date
                    var i = 4
                    val timeStampOriginal = GetRealDataFromHexaImbera.getDecimal(
                        data[data.size - 1]!![0].substring(0, 8)
                    ).toLong()
                    val unixTime = System.currentTimeMillis() / 1000
                    val diferencialTimeStamp = unixTime - timeStampOriginal
                    Log.d(
                        "algoritmoSalida",
                        " unixTime $unixTime -  timeStampOriginal $timeStampOriginal  = $diferencialTimeStamp"
                    )
                    data.map { listaInterna ->
                        val nuevaListaInterna: MutableList<String> = ArrayList()
                        listaInterna!!.map {
                            val instant = Instant.ofEpochSecond(
                                GetRealDataFromHexaImbera.getDecimal(
                                    it.substring(0, 8)
                                ) + diferencialTimeStamp
                            )
                            date = Date.from(instant)
                            val fechaHoraExadecimal =
                                BigInteger.valueOf(date.time / 1000).toString(16).padStart(8, '0')
                            val resul = replaceFirstEightChars(fechaHoraExadecimal, it, 8)
                            Log.d(
                                "datosEXA",
                                " dato $it insta $instant   date $date  fechaHoraExadecimal ${fechaHoraExadecimal}"
                            )
                            Log.d("datosEXASALIDA", "$it")
                            nuevaListaInterna.add(
                                resul.toString().uppercase()
                            ) //decimales sin punto
                        }
                        newData.add(nuevaListaInterna)
                    }
                }
                newData
            }

            else -> {
                ArrayList()
            }
        }
    }

    private fun dataFECHAPRUEBA2(
        data: MutableList<List<String>?>,
        action: String
    ): MutableList<List<String>?> {
        return when (action) {
            "PRUEBA" -> {
                val newData: MutableList<List<String>?> = ArrayList()

                data?.let { data ->
                    var date: Date
                    var i = 4
                    //Log.d("PAQUETE",":"+data.get())
                    val timeStampOriginal = GetRealDataFromHexaImbera.getDecimal(
                        data[data.size - 1]!![0].substring(
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
                    data.map { listaInterna ->
                        val nuevaListaInterna: MutableList<String> = ArrayList()

                        listaInterna!!.map {
                            val instant = Instant.ofEpochSecond(
                                GetRealDataFromHexaImbera.getDecimal(
                                    it.substring(0, 8)
                                ) + diferencialTimeStamp
                            )
                            date = Date.from(instant)
                            val fechaHoraExadecimal =
                                BigInteger.valueOf(date.time / 1000).toString(16).padStart(8, '0')
                            val resul = replaceFirstEightChars(fechaHoraExadecimal, it, 8)
                            Log.d(
                                "datosEXA",
                                " dato $it insta $instant   date ${date.toString()}  fechaHoraExadecimal ${fechaHoraExadecimal}"
                            )
                            Log.d("datosEXASALIDA", "$it")
                            nuevaListaInterna.add(
                                resul.toString().uppercase()
                            ) //decimales sin punto
                        }


                        newData.add(nuevaListaInterna)
                    }
                }
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
        return true
    }

    override fun onBluetoothDeviceDisconnected(): Boolean {
        if (!isDisconnecting) {
//            esp!!.putString("trefpVersionName", "")
//            esp!!.apply()
            runOnUiThread {
                binding!!.tvconnectionstate.setText("Desconectado")
                //binding!!.ivLockIcon.setVisibility(View.GONE)
                binding!!.tvconnectionstate.setTextColor(Color.BLACK)
                binding!!.tvfwversion.text = ""
                //conexionTrefp2.desconectar()
                NopintarLockIcon(false)
            }

        }
        return true
    }


}