package com.example.trepfp

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.StringBuilder
import java.math.BigInteger
import java.util.ArrayList
import java.util.Collections
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class prueba {

}

/*
    fun ObtenerLogger(callback: ConexionTrefp.CallbackLogger) {
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

                /***************************************************COMIENZA LA FUNCION DE TIEMPO*******************************************/

                delay(700)
                //   Log.d("getInfoListFinal", getInfoList().toString())
                delay(700)
                FinalListDataTiempo.clear()
                FinalListDataFinalT.clear()
                //    var listDataGT: MutableList<String>? = ArrayList()
                var listDataEVENT: MutableList<String>? = ArrayList()
                // callback.onProgress("Realizando Tiempo")
                if (ValidaConexion()) {
                    //    bluetoothLeService = getInfoList()// bluetoothServices.bluetoothLeService()
                    delay(500)
                    bluetoothServices.sendCommand("handshake", "4021")
                    delay(500)

                    runOnUiThread {
                        // getInfoList()?.clear()
                        clearLogger()
                    }
                    delay(1000)
                    bluetoothServices.sendCommand("time", "4060")
                    /* for (i in 0..31) {
                         Thread.sleep(700)
                         listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                      //   Log.d("ListatiempoLog", ":" + listData[i])
                     }*/
                    /*  ------- val tempListTime = ArrayList<String>()

                     for (i in 0..31) {
                         Thread.sleep(500)
                         tempListTime.add(bluetoothLeService?.dataFromBroadcastUpdateString!!)
                     }*/

                    val listDataTime = Collections.synchronizedList(mutableListOf<String>())

                    for (i in 0 until 32) {
                        Thread.sleep(700)


                        // Bloque sincronizado para thread-safety
                        synchronized(listDataTime) {
                            val data = bluetoothLeService?.dataFromBroadcastUpdateString
                            listDataTime.add(data)
                        }
                    }
                    listData = listDataTime

                    Log.d("qqqqqqqqqqqqqqqqqqqq", listData.toString())
// Hacer copia de la lista
                    /*    val listCopy = ArrayList(tempListTime)

    // Iterar sobre la copia en lugar de la original
                        for (data in listCopy) {
                            // hacer algo con cada data
                        }

                        listData.addAll(tempListTime)*/
                    for (i in listData.indices) { //comprobación de la obtención de datos sin lista vacía
                        if (listData[i]!!.length != 0) {
                            FinalListTest.add(listData[i]!!)
                        }
                    }
                    if (FinalListTest[0]!!.length == 0) {

                    } else {
                        var df = getInfoList()
                        df?.map {
                            Log.d("SalidagetInfoList","datos $it")
                        }
                        var RemplazoTime = VTIME(df as MutableList<String?>)
                        RemplazoTime.map { Log.d("SalidagetInfoList","\n --------------------------------------------- $it") }
                        FinalListData = VTIME(df as MutableList<String?>) //VTIME(FinalListTest)
                        //FinalListData = GetRealDataFromHexaCEB.convert(FinalListTest, "Lectura de datos tipo Tiempo",sp.getString("numversion",""), sp.getString("modelo",""));

                    }

                    FinalListData?.map {

                        Log.d("SALIDAAAAAAAAAAAAAAAAA","time $it")
                    }


                    Thread.sleep(2000)
                    var FINALLISTA = mutableListOf<String?>()
                    runOnUiThread {
                        var df = getInfoList()
                        df?.map {
                            Log.d("SalidagetInfoList","datos $it")
                        }

                        FINALLISTA = VTIME(
                            df as MutableList<String?>//  getInfoList() as MutableList<String?>
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
                    listData.clear()
                    FinalListTest.clear()
                    FinalListData.clear()
                    //   FINALLISTA.clear()
                    Thread.sleep(1000)
                    runOnUiThread {
                        clearLogger()
                        //  getInfoList()!!.clear()
                    }
                    bluetoothServices.sendCommand("event", "4061")

                    /*   for (i in 0..31) {
                           Thread.sleep(700)

                           listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)
                         //  Log.d("ListatiempoLog", ":" + listData[i])
                       }*/
                    /* -------  val tempList = ArrayList<String>()

                      for (i in 0..31) {
                          Thread.sleep(700)
                          tempList.add(bluetoothLeService?.dataFromBroadcastUpdateString!!)
                      }

                      listData.addAll(tempList)
                      */


                    val listDataevent = Collections.synchronizedList(mutableListOf<String>())

                    for (i in 0 until 32) {
                        Thread.sleep(700)
                        val data = bluetoothLeService?.dataFromBroadcastUpdateString

                        // Bloque sincronizado para thread-safety
                        synchronized(listDataevent) {
                            listDataevent.add(data)
                        }
                    }
                    listData = listDataevent

                    for (i in listData.indices) { //comprobación de la obtención de datos sin lista vacía
                        if (listData[i]!!.length != 0) {
                            FinalListTest.add(listData[i]!!)
                        }
                    }
                    if (FinalListTest[0]!!.isEmpty()) {

                    } else {
                        FinalListData = VEvent(FinalListTest)
                        //FinalListData = GetRealDataFromHexaCEB.convert(FinalListTest, "Lectura de datos tipo Tiempo",sp.getString("numversion",""), sp.getString("modelo",""));

                    }

                    FinalListData?.map {

                        Log.d("SALIDAAAAAAAAAAAAAAAAA","Event $it")
                    }
                    runOnUiThread {
                        var ListaPRe = mutableListOf<String>()
                        FINALLISTA?.map {

                            if (it!!.length>18) ListaPRe.add(it)
                        }
                        if (ListaPRe.size>0) {
                            registrosPRE = VEvent(
                                ListaPRe as MutableList<String?>//  getInfoList() as MutableList<String?>
                            ) as MutableList<String>
                        }
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

                                    val sttiempo = StringBuilder()
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

                                        callback.getTime(  agregar00AlPenultimoCaracter(FinalListDataFinalT as MutableList<String>?,2)) }
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
                                        callback.getTime(  agregar00AlPenultimoCaracter(listD,2))
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
                                        java.lang.Long.toHexString(currentTimeMillis / 1000).uppercase()
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
                                        val difBACK = abs(
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
                                        callback.getTime(agregar00AlPenultimoCaracter(listaTemporalFINALTIME,2))
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
                                        callback.getTime(agregar00AlPenultimoCaracter(listD,2))
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
                                callback.getTime(agregar00AlPenultimoCaracter(listFTIME as MutableList<String>,2))
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
                            bluetoothServices.sendCommand("event", "4061")

                            /*  for (i in 0..31) {
                                  Thread.sleep(700)
                                  listData.add(bluetoothLeService?.dataFromBroadcastUpdateString)// getDataFromBroadcastUpdateString())
                                  //Log.d("ListatiempoLog", ":" + listData[i])
                              }*/
                            val tempListEvent = ArrayList<String>()

                            for (i in 0..31) {
                                Thread.sleep(700)
                                tempListEvent.add(bluetoothLeService?.dataFromBroadcastUpdateString!!)
                            }

                            listData.addAll(tempListEvent)
                            for (i in listData.indices) { //comprobación de la obtención de datos sin lista vacía
                                if (listData[i]!!.length != 0) {
                                    FinalListTest.add(listData[i]!!)
                                }
                            }
                            if (FinalListTest[0]!!.length == 0) {

                            } else {
                                FinalListData = VEvent(FinalListTest)
                                //FinalListData = GetRealDataFromHexaCEB.convert(FinalListTest, "Lectura de datos tipo Tiempo",sp.getString("numversion",""), sp.getString("modelo",""));

                            }
                            Thread.sleep(700)
                            runOnUiThread {

                                var DefaultEvent = getInfoList()
                                FINALLISTA = VEvent(
                                    DefaultEvent as MutableList<String?> //  FinalListTest //     getInfoList() as MutableList<String?>
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
                                                callback.getEvent(agregar00AlPenultimoCaracter(it as MutableList<String>,2))
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
                                            //       bluetoothLeService =   getInfoList()//   bluetoothServices.bluetoothLeService()
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
                                                java.lang.Long.toHexString(
                                                    TimeStampControlMIlisegundos?.div(1000) ?: 0
                                                )
                                            val TimeStampActualMIlisegundos =
                                                TimeStampActual.toLong(16) * 1000
                                            val currentTimeHexTimeStampActual =
                                                java.lang.Long.toHexString(TimeStampActualMIlisegundos / 1000)
                                            val registrosLastMIlisegundos =
                                                SALIDAEVENTOREGISTROS.last()?.substring(8, 16)
                                                    ?.toLong(16)?.times(1000)
                                            val currentTimeHexregistrosLastMIlisegundos =
                                                java.lang.Long.toHexString(
                                                    registrosLastMIlisegundos?.div(1000) ?: 0
                                                )
                                            val diferenciaTIMESTAMPActualvsControl =
                                                TimeStampActualMIlisegundos - registrosLastMIlisegundos!!
                                            val currentTimeHexDiferenciaACTUALvsControl =
                                                java.lang.Long.toHexString((TimeStampActualMIlisegundos) / 1000)
                                            Log.d(
                                                "funcionUltime",
                                                "TIEMPOS s ${s?.get(0)} sSinEspacios $sSinEspacios  TIMEUNIX $TIMEUNIX TimeStampActual $TimeStampActual"
                                            )
                                            /*****************************************CICLO PARA ESTAMPAR DESPUES DEL ULTIMO CORTE DE LUZ************************************************/
                                            /*****************************************CICLO PARA ESTAMPAR DESPUES DEL ULTIMO CORTE DE LUZ************************************************/
                                            for (i in SALIDAEVENTOREGISTROS.size - 1 downTo iC ) {

                                                if (SALIDAEVENTOREGISTROS[i]?.substring(16,18).equals("04")){
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
                                                        java.lang.Long.toHexString(DiferenciaNowvsDif / 1000)
                                                    val exaFinalStar =
                                                        java.lang.Long.toHexString(DiferenciaFINALstar / 1000)
                                                    val s = "${SALIDAEVENTOREGISTROS[i]?.substring(0,8)}$exaFinalEnd${
                                                        SALIDAEVENTOREGISTROS[i]!!.substring(16)
                                                    }"
                                                    SalidaEvent.add(s)
                                                    Log.d("Ciclo04",
                                                        "ciclo ${SALIDAEVENTOREGISTROS[i]} $Dif   $DiferenciaNowvsDif   star $${
                                                            convertirHexAFecha(
                                                                exaFinalStar
                                                            )
                                                        }   end ${convertirHexAFecha(exaFinalEnd)} $s ${s.length} , $i  "
                                                    )
                                                }
                                                else {
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
                                                        java.lang.Long.toHexString(DiferenciaNowvsDif / 1000)
                                                    val exaFinalStar =
                                                        java.lang.Long.toHexString(DiferenciaFINALstar / 1000)
                                                    val s = "$exaFinalStar$exaFinalEnd${
                                                        SALIDAEVENTOREGISTROS[i]!!.substring(16)
                                                    }"
                                                    SalidaEvent.add(s)
                                                    Log.d("Ciclo04",
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
                                            for (i in iC-1 downTo 0) {
                                                ////////////////Esta parte del codigo pasa los registros del logger del ultimo evento 04 al evento mas antiguo
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
                                                Log.d("SALIDAAAAAAAAAAAAAAAAA","$it   star $star end $end ")
                                                println("$it   star $star end $end ")
                                            }
                                            //  callback.onSuccess(true)
                                            isEVENTOK.set(true)

                                            ////////////////Esta parte del codigo pasa los registros del logger del ultimo evento 04 al evento mas antiguo
                                            var tempEvent = SalidaEvent

                                            runOnUiThread {
                                                callback.getEvent(agregar00AlPenultimoCaracter(SalidaEvent as MutableList<String>?,2))
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
                        }
                        catch (ex: Exception) {
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




                //   callback.onSuccess(true)
                /******************************************************* Iniciando Limpieza del logger *****************************************************/



                /******************************************************* Iniciando Limpieza del logger *****************************************************/


                var listDataCleanLogger: MutableList<String>? = ArrayList()
                val s = sp?.getBoolean("isconnected", false)
                //  callback.onProgress("Realizando")
                Log.d("SALIDAfinalqqqaaaaaaaaaa","------------------------------------------------------------ Iniciando Limpieza del logger  S $s")
                if (s == true) {

                    //  bluetoothLeService = getInfoList()// bluetoothServices.bluetoothLeService()
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
                                // bluetoothLeService =  getInfoList()// bluetoothServices.bluetoothLeService()
                                bluetoothLeService?.sendFirstComando("4021")
                                Thread.sleep(500)
                                HourNow = GetNowDateExa()

                                var Command: String? = null
                                HourNow?.let {
                                    if (sp!!.getString("name", "")!!.contains("IMBERA-HEALTH")) {
                                        var statusBCD = actualizarHoraBCD()
                                        Log.d("actualizarHoraBCD", "actualizarHoraBCD result  $statusBCD")
                                        if (statusBCD)  ListTIMEResult.add("F1 3D") else  ListTIMEResult.add("F1 3E")

                                    }
                                    else {
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

                                                Log.d("Salidarespuesta3472","valor linea 3472 $listData")
                                                if (getInfoList()?.last()?.equals("F13D") == true) {
                                                    ListTIMEResult.add("F1 3D")
                                                } else {
                                                    callback.onError("No se pudo Actualizar el TIMESTAMP")
                                                    ListTIMEResult.add("F1 3E")
                                                }
                                            } else Log.d("", "dataChecksum total:8")
                                        }
                                        ListTIMEResult.add("Sin hora")
                                    }
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
                                callback.onSuccess(true)
                                callback.onProgress("Finalizando Actualizacion del TIMESTAMP")
                            }

                            "F1 3E", "not" -> {

                                callback.onSuccess(false)
                                callback.onError("No se pudo Actualizar el TIMESTAMP")
                            }

                            "DESCONECTADO" -> {
                                callback.onSuccess(false)
                                callback.onError("DESCONECTADO")
                            }

                        }
                        //  val handler = Handler()
                        // handler.postDelayed({

                        ////////////////////////////AQUI SE HACE LA LIMPIEZA DEL LOGGER
                        /*      callback.onProgress("Iniciando Limpieza del logger")
                          //    bluetoothLeService = getInfoList()// bluetoothServices.bluetoothLeService()
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
                         */

                        //     desconectar()
                        //  }, 5000)
                    }
                }


            }

        }
    }


 */