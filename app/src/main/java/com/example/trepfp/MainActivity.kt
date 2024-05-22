package com.example.trepfp


import RecyclerResultAdapter
import RecyclerResultItem
import Utility.BLEDevices
import Utility.GetHexFromRealDataCEOWF.getDecimal
import Utility.GetHexFromRealDataImbera
import Utility.GetRealDataFromHexaImbera
import Utility.GetRealDataFromHexaOxxoDisplay
import Utility.MakeProgressBar
import android.Manifest
import android.R
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
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trepfp.databinding.ActivityMainBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import java.util.*

//import com.example123.trepfp.ConexionTrefp
class MainActivity : AppCompatActivity(), ConexionTrefp.MyCallback {
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var binding: ActivityMainBinding
    var myContext: Context? = null
    var listaTemporalFINAL = mutableListOf<String>()
    var datalogger = mutableListOf<String>()
    var locationResultListener: ConexionTrefp.LocationResultListener? = null
    val data = listOf("00:E4:4C:20:A2:23", "00:E4:4C:00:92:E4", "00:E4:4C:20:A7:E4")
    val conexionTrefp2 by lazy { ConexionTrefp(this, null, null) }
    var sp: SharedPreferences? = null
    var esp: SharedPreferences.Editor? = null
    var myconnectListener: connectListener? = null
    private val progressDialog2 by lazy { this@MainActivity?.let { MakeProgressBar(it) } }
    val workbook = XSSFWorkbook()
    private lateinit var mHandler: Handler
    private val customProgressDialog by lazy { this@MainActivity?.let { CustomProgressDialog(this) } }
    val scope = CoroutineScope(Dispatchers.Main)

    data class Datos(
        val timestamp: String,
        val temperature1p1: String,
        val temperature2p1: String,
        val voltage: String,
        val Original: String
        /*,
        val checksum: String*/

    )


    private lateinit var saveFileLauncher: ActivityResultLauncher<String>


    private lateinit var recyclerViewBLEList: RecyclerViewBLEList
    private lateinit var listaDevices: ArrayList<BLEDevices>
    private lateinit var mBluetoothAdapter: BluetoothAdapter

    private var mScanning: Boolean = false
//    private var mBluetoothAdapter: BluetoothAdapter? = null
//    var recyclerViewBLEList: RecyclerViewBLEList? = null


    private lateinit var recyclerResultAdapter: RecyclerResultAdapter
    private lateinit var recyclerResultList: ArrayList<RecyclerResultItem>
    var bluetoothLeService  : BluetoothLeService? = null // by lazy { conexionTrefp2.bluetoothLeService }
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //  setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        this.myContext = this@MainActivity
        askPermission()
        init()
        mHandler = Handler()
        recyclerResultList = ArrayList()
        recyclerResultAdapter = RecyclerResultAdapter(recyclerResultList)
        binding.ReciclerResult.layoutManager = LinearLayoutManager(this)
        binding.ReciclerResult.adapter = recyclerResultAdapter

        val adapter = ArrayAdapter(this, R.layout.simple_spinner_item, data)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        listaDevices = ArrayList()
        recyclerViewBLEList = RecyclerViewBLEList(listaDevices)
        binding.rvbleDevices.layoutManager = LinearLayoutManager(this)
        binding.rvbleDevices.adapter = recyclerViewBLEList


        binding.TextAltitud.setText("ELTEC_apps")
        binding.Textlatitud.setText("975318642a")


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mHandler = Handler()
        // Establece el adaptador y carga los datos en el Spinner
        binding.spinnerMAC.adapter = adapter
        // binding.spinner.adapter = adapter
        binding.spinnerMAC.selectedItem.toString()
        //conexionTrefp(applicationContext)

        bluetoothLeService  =   conexionTrefp2.bluetoothLeService
                //   bluetoothServices = BluetoothServices(this)

        binding.textViewVersion.text = "Version de prueba: ${BuildConfig.VERSION_NAME}"
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


                 //       conexionTrefp2.bluetoothLeService!!.connect(device.mac)
                        conexionTrefp2.MyAsyncTaskConnectBLE(device.mac,
                            device.nombre,
                            object : ConexionTrefp.MyCallback {
                                override fun onSuccess(result: Boolean): Boolean {

                                    runOnUiThread {
                                        customProgressDialog!!.dismiss()
                                        Log.d("ConexionCLASBLE", "onSuccess intento $contador resultado ${result.toString()} ")
                                        if (result) binding.T.text = "${sp!!.getString("mac", "")}"
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
                                    Log.d("ConexionCLASBLE", "getInfo ${data.toString()}")
                                }

                                override fun onProgress(progress: String): String {

                                    when (progress) {
                                        "Iniciando" -> {

                                            runOnUiThread {
                                                customProgressDialog!!.updateText("Conectando")
                                                binding.TextResultado.text = progress
                                            }
                                        }

                                        "Realizando" -> {
                                            runOnUiThread {
                                                binding.TextResultado.text = progress
                                            }
                                        }

                                        "Finalizando" -> {
                                            runOnUiThread {
                                                binding.TextResultado.text = progress
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
            conexionTrefp2.desconectar()


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

            conexionTrefp2.ObtenerLoggerVersion2(object : ConexionTrefp.CallbackLogger {
                override fun onSuccess(result: Boolean): Boolean {
                    customProgressDialog!!.dismiss()
                    Log.d("LoggerFUN", "---------  $result ")

                    if (result)

                    {
                        runOnUiThread {
                            customProgressDialog!!.show()
                        customProgressDialog!!.updateText("creando excel")
                        }
                        val handler = Handler()
                        handler.postDelayed({
                            //LoggerExcel(FinalEvento,FinalTiempo)
                            createCombinedExcelFile(FinalTiempo, FinalEvento, FinalEventoCrudo)
                        }, 2000)
                    }
                    return result
                }

                override fun onError(error: String) {
                    Log.d("LoggerFUN", "--------- tiempo  $error")
                }

                override fun getTime(data: MutableList<String>?) {
                    FinalTiempo = data
                    data?.map {
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
                }

                override fun getEvent(data: MutableList<String>?) {

                    FinalEvento = data
                    data?.map {
                        Log.d("LoggerFUN", "getEvent $it")
                        if (it.length > 16) {
                        }
                    }

                    data?.map {
                      //  Log.d("ObtenerLoggerPruebaFUNCION", "getEvent $it")
                        if (it.length > 16) {
                            Log.d("ObtenerLoggerPruebaFUNCION","inicio ${convertHexToHumanDateNew(it.substring(0,8))}  final ${convertHexToHumanDateNew(it.substring(8,16))}")

                        }
                    }
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

                                  runOnUiThread {
                                      customProgressDialog!!.show()
                                      customProgressDialog!!.updateText("Iniciando Logger")
                                  }}

//                                  customProgressDialog!!.updateText("Iniciando Logger")
                              }

                    binding.TextResultado.text = progress
                    Log.d("LoggerFUN", "---------progress $progress")
                    return progress
                }

            }).execute()
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
                        Log.d("MyAsyncTaskGetHour", it + " FECHA  del control   ${convertirHexAFecha(it)}")
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

                                binding.textView.text =
                                    "Resultado ${sp?.getBoolean("isconnected", false)}"
                                sp?.getString("mac", "").let {
                                    binding.T.text = " conectado a \n $it "
                                }
                                progressDialog2?.secondStop()
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


            //     }


            /*          conexionTrefp2.MyAsyncTaskSendXY(Latitud/*-21.51f*/, Longitud, object : ConexionTrefp.MyCallback {

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
        binding.imageView2.setOnClickListener {
            getDeviceLocation { latitude, longitude ->
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
        binding.BtnWifi.setOnClickListener {
//            conexionTrefp2.createProgressDialogwifi()
           conexionTrefp2.getInfoWifi(object : ConexionTrefp.MyCallback{
                override fun getInfo(data: MutableList<String>?) {
                    Log.d("getInfoWifi","data $data")
                   data?.map {

                   }
                }

                override fun onError(error: String) {
                    Log.d("getInfoWifi","onError ${error.toString()}")
                }

                override fun onProgress(progress: String): String {
                    Log.d("getInfoWifi","progress $progress")
                    return progress
                }

                override fun onSuccess(result: Boolean): Boolean {
                    Log.d("getInfoWifi","result $result")
                    return  result
                }
            })
        }

        binding.BtnSendWifi.setOnClickListener {

//            binding.TextAltitud.text  setText("ELTEC_apps")
//            binding.Textlatitud.text.toString(),  setText("975318642a")
            conexionTrefp2.MyAsyncTaskSendWifiPrincipal(binding.TextAltitud.text.toString(),binding.Textlatitud.text.toString(), object : ConexionTrefp.MyCallback{
                override fun getInfo(data: MutableList<String>?) {
                    data?.map {
                        Log.d("BtnSendWifi","data $it")
                    }
                }

                override fun onError(error: String) {
                    Log.d("BtnSendWifi","onError ${error.toString()}")
                }

                override fun onProgress(progress: String): String {
                    Log.d("BtnSendWifi","progress $progress")
                    return progress
                }

                override fun onSuccess(result: Boolean): Boolean {
                    Log.d("BtnSendWifi","result $result")
                    return  result
                }
            })
        }
        binding.BtnSendWifiSecundario.setOnClickListener {
            conexionTrefp2.MyAsyncTaskSendWifiSecundario(binding.TextAltitud.text.toString(),binding.Textlatitud.text.toString(), object : ConexionTrefp.MyCallback{
                override fun getInfo(data: MutableList<String>?) {
                    data?.map {
                        Log.d("BtnSendWifi","data $it")
                    }
                }

                override fun onError(error: String) {
                    Log.d("BtnSendWifi","onError ${error.toString()}")
                }

                override fun onProgress(progress: String): String {
                    Log.d("BtnSendWifi","progress $progress")
                    return progress
                }

                override fun onSuccess(result: Boolean): Boolean {
                    Log.d("BtnSendWifi","result $result")
                    return  result
                }
            })
        }
        validaCONEXION()

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
        dateFormat.timeZone = TimeZone.getTimeZone("America/Mexico_City") // Utiliza la zona horaria de México
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
        dataEventCrudos: MutableList<String>?
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
        createExcelSheet(dataEventCrudos,"Datos Crudos", arrayOf("datos"),2,workbook)

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
                                conexionTrefp2.convertHexToHumanDateLogger(
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
                Creador.setCellValue("Versión ImberaP:" + BuildConfig.VERSION_NAME)
                Creador.setCellStyle(styleceldainfo)
                data?.clear()
            }

            "Event" -> {
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
                    Creador.setCellValue("Versión ImberaP:" + BuildConfig.VERSION_NAME)
                    Creador.setCellStyle(styleceldainfo)
                }
            }
            "Datos Crudos"->{
                var filaNumEVENT = 1
                var estiloActual = true // Inicia con el estilo oscuro
                var colNumEVENT = 0

                if (data != null) {
                    for (dato in data) {
                        val fila = sheet.createRow(filaNumEVENT)


                        val Iteración = fila.createCell(colNumEVENT)
                        if (estiloActual/*.equals(styleceldaOscura)*/) Iteración.setCellStyle(
                            styleceldaOscura
                        ) else Iteración.setCellStyle(styleceldaClara)
                        val voltaje = fila.createCell(colNumEVENT)
                        voltaje.setCellValue(dato)
                        colNumEVENT
                        filaNumEVENT++
                    }
                }
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
        Creador.setCellValue("Versión ImberaP:" + BuildConfig.VERSION_NAME)
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

    // En el método onActivityResult, manejar la respuesta del usuario al seleccionar la carpeta de destino
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SAVE_EXCEL_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val outputStream = contentResolver.openOutputStream(uri)
                    outputStream?.use { stream ->
                        workbook.write(stream)
                    }
                    MakeToast("Los datos se han exportado correctamente al archivo '${uri.path}'.")
                } catch (e: IOException) {
                    e.printStackTrace()
                    MakeToast("Error al guardar el archivo.")
                }
            }
        } else {
            MakeToast("No se ha seleccionado ninguna carpeta de destino.")
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
                    binding.T.setTextColor(Color.parseColor("#308446"))

                } else {
                    binding.T.setTextColor(Color.RED)
                    binding.T.text = "desconectado"
                    binding.textView.text = "Resultado"
                }
                delay(5000L) // Espera 5 segundos antes de la próxima validación
            }
        }
    }

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
                Log.d("MainActivity", "->>>>> Latitude: $latitude, Longitude: $longitude")
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

                        Log.d("MainActivity", "->>>>> Latitude: $latitude, Longitude: $longitude")
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
                Manifest.permission.CAMERA
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


}