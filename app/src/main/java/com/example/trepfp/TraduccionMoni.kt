package com.example.trepfp

import Utility.GetRealDataFromHexaImbera
import android.util.Log
import java.time.Instant
import java.util.Date
import java.util.Locale

class TraduccionMoni {



    companion object {


        fun hexToAscii(hexStr: String): String {
            val output = java.lang.StringBuilder()
            var i = 0
            while (i < hexStr.length) {
                val str = hexStr.substring(i, i + 2)
                output.append(str.toInt(16).toChar())
                i += 2
            }

            return output.toString()
        }

        fun asciiToDecimal(text: String): Int {
            return text.map { it.toInt() }
                .joinToString("")
                .toInt()
        }

        fun convert(
            arrayLists: List<String?>,
            action: String?,
            fwversion: String,
            modelo: String
        ): List<String> {
            GetRealDataFromHexaImbera.arrayListInfo.clear()
            when (action) {
                "Handshake" -> {
                    if (!arrayLists.isEmpty()) {
                        val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists  as List<String>)
                        //header
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(0, 4)) //head
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(4, 28)) //Mac

                        //data
                        GetRealDataFromHexaImbera.arrayListInfo.add(
                            s.substring(
                                28,
                                30
                            )
                        ) //modelo trefpb
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(30, 34)) //version
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(34, 38)) //plantilla

                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(38, 42)) //checklist

                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(42)) //checksum


                        //checksum
                        //arrayListInfo.add(s.substring(46,54));
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
                        GetRealDataFromHexaImbera.arrayListInfo.add(
                            s.substring(
                                4,
                                12
                            )
                        ) //buffer_size
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(12, 14)) //data_type
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(14, 16)) //data_size
                        do {
                            if (i == 22) { //saltar posiciones por parámetros que no se están usando
                                i = i + 4
                            } else if (i == 30) { //saltar posiciones por parámetros que no se están usando
                                i = i + 20
                            } else if (i == 62) {
                                i = i + 8
                            } else {
                                GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(i, i + 4))
                                i = i + 4
                            }
                        } while (i < 86)

                        //+2 para saltar 66
                        i = 148
                        do {
                            if (i == 154) { //saltar posiciones por parámetros que no se están usando
                                i = i + 2
                            } else if (i == 158) { //saltar posiciones por parámetros que no se están usando
                                i = i + 6
                            } else if (i == 172) {
                                i = i + 6
                            } else if (i == 184) {
                                i = i + 2
                            } else if (i == 190) {
                                i = i + 2
                            } else if (i == 196) {
                                i = i + 16
                            } else if (i == 228) {
                                i = i + 2
                            } else if (i == 242) {
                                i = i + 6
                            } else if (i == 252) {
                                i = i + 8
                            } else {
                                GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(i, i + 2))
                                i = i + 2
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
                        Log.d("PPP", ":$s")
                        if (fwversion == "1.04") {
                            //head
                            GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(0, 4)) //head
                            GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(4, 12)) //
                            GetRealDataFromHexaImbera.arrayListInfo.add(
                                s.substring(
                                    12,
                                    14
                                )
                            ) //modelo trefpb
                            GetRealDataFromHexaImbera.arrayListInfo.add(
                                s.substring(
                                    14,
                                    16
                                )
                            ) //version

                            GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(16, 20)) //temp1
                            GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(20, 24)) //temp2
                            GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(24, 28)) //temp3
                            GetRealDataFromHexaImbera.arrayListInfo.add(
                                s.substring(
                                    28,
                                    32
                                )
                            ) //tempDisplay
                            GetRealDataFromHexaImbera.arrayListInfo.add(
                                s.substring(
                                    32,
                                    34
                                )
                            ) //voltaje
                            GetRealDataFromHexaImbera.arrayListInfo.add(
                                s.substring(
                                    34,
                                    38
                                )
                            ) //actuadores
                            GetRealDataFromHexaImbera.arrayListInfo.add(
                                s.substring(
                                    38,
                                    42
                                )
                            ) //alarmas
                            Log.d("", "crudoTR104:" + GetRealDataFromHexaImbera.arrayListInfo)
                        } else {
                            //head
                            GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(0, 4)) //head
                            GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(4, 12)) //
                            GetRealDataFromHexaImbera.arrayListInfo.add(
                                s.substring(
                                    12,
                                    14
                                )
                            ) //modelo trefpb
                            GetRealDataFromHexaImbera.arrayListInfo.add(
                                s.substring(
                                    14,
                                    16
                                )
                            ) //version

                            GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(16, 20)) //temp1
                            GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(20, 24)) //temp2
                            GetRealDataFromHexaImbera.arrayListInfo.add(
                                s.substring(
                                    24,
                                    26
                                )
                            ) //voltaje
                            GetRealDataFromHexaImbera.arrayListInfo.add(
                                s.substring(
                                    26,
                                    28
                                )
                            ) //actuadores
                            GetRealDataFromHexaImbera.arrayListInfo.add(
                                s.substring(
                                    28,
                                    32
                                )
                            ) //alarmas
                            Log.d("", "crudoTR102:" + GetRealDataFromHexaImbera.arrayListInfo)
                        }

                        //arrayListInfo.add(s.substring(28,32));//plantilla
                        //arrayListInfo.add(s.substring(34));//checksum
                    }
                }

                "Lectura de datos tipo Tiempo" -> {
                    if (!arrayLists.isEmpty()) {
                        val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists as List<String>)
                        val datos: MutableList<String> = ArrayList()
                        val datos2: MutableList<String> = ArrayList()
                        //header
                        Log.d("", "sssTiempo:$s")
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(0, 4)) //head
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(4, 12)) //
                        GetRealDataFromHexaImbera.arrayListInfo.add(
                            s.substring(
                                12,
                                14
                            )
                        ) //modelo trefpb
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(14, 16)) //version

                        val st = java.lang.StringBuilder()
                        st.append(s.substring(16, s.length - 8))
                        Log.d("LPOPOP", "crudotiempoTIEMPO:$fwversion")
                        Log.d("LPOPOP", "crudotiempoTIEMPO:$modelo")
                        if ((fwversion == "1.02" && modelo == "3.3") || (fwversion == "1.04" && modelo == "3.5")) { //nuevo logger, diferente división de información
                            var i = 0
                            do { //dividir toda la información en paquetes de 128 bytes
                                if (i + 256 > st.length) {
                                    datos.add(st.substring(i)) //checksum
                                    break
                                } else datos.add(st.substring(i, i + 256))
                                i = i + 256
                            } while (i < st.length)

                            var j = 0
                            do { //dividir los paquetes de 128 bytes según el protocolo
                                i = 0
                                while (i < datos[j].length) {
                                    if (i + 18 > datos[0].length) {
                                        datos2.add(datos[j].substring(i)) //checksum
                                        break
                                    } else datos2.add(datos[j].substring(i, i + 18))
                                    i += 18
                                }
                                j++
                            } while (j < datos.size)

                            Log.d("", "crudotiempoTIEMPO:$datos2")

                            //Log.d("","ultimonTIEMPO:"+datos2.get(datos2.size()-2));

                            //organizar la información que realmente sirve (quitar 0s)
                            var h = 4
                            //String numeroRegistrosNuevos = datos2.get(datos.size()-2);
                            var o = 0
                            while (o < datos2.size) {
                                if (datos2[o].length != 4 && datos2[o] != "000000000000000000") {
                                    GetRealDataFromHexaImbera.arrayListInfo.add(datos2[o])
                                    Log.d(
                                        "",
                                        "crudotiempoFOR:" + GetRealDataFromHexaImbera.arrayListInfo[h]
                                    )
                                    h++
                                }
                                o++
                            }
                            Log.d("", "crudotiempo:" + GetRealDataFromHexaImbera.arrayListInfo)
                        } else {
                            var i = 16
                            do {
                                if (i + 18 > s.length) {
                                    //arrayListInfo.add(s.substring(i));//checksum
                                    break
                                } else GetRealDataFromHexaImbera.arrayListInfo.add(
                                    s.substring(
                                        i,
                                        i + 18
                                    )
                                )

                                i = i + 18
                            } while (i < s.length)

                            Log.d("", "crudotiempo:" + GetRealDataFromHexaImbera.arrayListInfo)
                        }


                        //data
                    }
                }

                "Lectura de datos tipo Evento" -> {
                    if (!arrayLists.isEmpty()) {
                        val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists as List<String>)
                        val datos: MutableList<String> = ArrayList()
                        val datos2: MutableList<String> = ArrayList()
                        //header
                        Log.d("", "sss}Evento:" + s.length)

                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(0, 4)) //head
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(4, 12)) //
                        GetRealDataFromHexaImbera.arrayListInfo.add(
                            s.substring(
                                12,
                                14
                            )
                        ) //modelo trefpb
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.substring(14, 16)) //version

                        val st = java.lang.StringBuilder()
                        st.append(s.substring(16, s.length - 8))
                        Log.d("LPOPOP", "crudotiempoTEvento:$fwversion")
                        Log.d("LPOPOP", "crudotiempoTEvento:$modelo")

                        if ((fwversion == "1.02" && modelo == "3.3") || (fwversion == "1.04" && modelo == "3.5")) { //nuevo logger, diferente división de información
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
                                    if (i + 28 > datos[0].length) {
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
                                    GetRealDataFromHexaImbera.arrayListInfo.add(datos2[o])
                                    Log.d(
                                        "",
                                        "crudoEventoFOR:" + GetRealDataFromHexaImbera.arrayListInfo[h]
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
                                } else GetRealDataFromHexaImbera.arrayListInfo.add(
                                    s.substring(
                                        i,
                                        i + 28
                                    )
                                )
                                i += 28
                            }
                            Log.d("", "crudoEvento:" + GetRealDataFromHexaImbera.arrayListInfo)
                        }
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

        fun GetRealData(
            data: List<String>,
            action: String?,
            fwversion: String,
            modelo: String
        ): List<String?> {
            //USO SOLO DE LOS DATOS BUFFER IMPORTANTES PARA MOSTRARLOS EN PANTALLA, LAS POSICIONES RESTANTES (HEADER) SON CORRECTAS
            when (action) {
                "Handshake" -> {
                    val newData: MutableList<String?> = java.util.ArrayList()
                    if (data.isEmpty()) {
                        //newData.add(getSameData(data.get(0),"trefpversion"));
                        newData.add("nullHandshake")
                    } else {
                        //header
                        //newData.add(data.get(0));

                        //el handshake nuevo tiene el modelo en dos bytes y la versión de HardWare

                        if (data[5].contains("FFFF")) { //verifiar que el handshake sea viejo o nuevo, FFFF es viejo
                            newData.add(GetRealDataFromHexaImbera.hexToAscii(data[1]))
                            Log.d("dedos", ":" + data[2])
                            newData.add(GetRealDataFromHexaImbera.getSameData(data[2], action))
                            newData.add(
                                GetRealDataFromHexaImbera.getFwVersionFromHex(
                                    data[3].substring(
                                        0,
                                        2
                                    ), data[3].substring(2)
                                )
                            )
                            newData.add(
                                GetRealDataFromHexaImbera.getDecimalFloat(data[4]).toString()
                            ) // decimales con punto
                        } else {
                            newData.add(GetRealDataFromHexaImbera.hexToAscii(data[1]))
                            //modelo
                            newData.add(
                                GetRealDataFromHexaImbera.hexToAscii(
                                    data[5].substring(
                                        0,
                                        2
                                    )
                                ) + GetRealDataFromHexaImbera.hexToAscii(
                                    data[2]
                                )
                            )
                            newData.add(
                                GetRealDataFromHexaImbera.getFwVersionFromHex(
                                    data[3].substring(
                                        0,
                                        2
                                    ), data[3].substring(2)
                                )
                            )
                            newData.add(
                                GetRealDataFromHexaImbera.getDecimalFloat(data[4]).toString()
                            ) // decimales con punto
                            //HW
                            newData.add(
                                GetRealDataFromHexaImbera.getDecimalFloat(
                                    data[5].substring(
                                        2
                                    )
                                ).toString()
                            )
                        }
                    }
                    return newData
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
                                        GetRealDataFromHexaImbera.getDecimalFloat(data[i])
                                            .toString()
                                    ) // decimales con punto
                                }
                            }

                            i++
                        } while (i < data.size)
                    }
                    return newData
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
                            newData.add( getActuadorMONI(data[9]))
                            newData.add( getAlarmaMONI(data[10]))
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
                            newData.add( getActuadorMONI (data[7]))
                            newData.add( getAlarmaMONI (data[8]))
                            //newData.add(getSameData(data.get(9), "trefpversion")); // decimales con punto
                            //newData.add(hexToAscii(data.get(9)));
                        }
                    }
                    return newData
                }

                "Lectura de datos tipo Tiempo" -> {
                    val newData: MutableList<String?> = java.util.ArrayList()
                    val header: MutableList<String> = java.util.ArrayList()
                    //header
                    if (data.isEmpty()) {
                        //newData.add(getSameData(data.get(0),"trefpversion"));
                        newData.add("nullHandshake")
                    } else {
                        if ((fwversion == "1.02" && modelo == "3.3") || (fwversion == "1.04" && modelo == "3.5")) {
                            //nuevo logger, diferente división de información
                            //header
                            header.add(
                                GetRealDataFromHexaImbera.getSameData(
                                    data[0],
                                    "trefpversion"
                                )
                            )
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

                                newData.add(
                                    GetRealDataFromHexaImbera.getDecimal(
                                        data[i].substring(
                                            16
                                        )
                                    ).toString()
                                ) //decimales sin punto
                                i++
                            } while (i < data.size)
                        } else {
                            //header
                            Log.d("GHDD", ":$data")
                            header.add(
                                GetRealDataFromHexaImbera.getSameData(
                                    data[0],
                                    "trefpversion"
                                )
                            )
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
                    return newData
                }

                "Lectura de datos tipo Evento" -> {
                    val newData: MutableList<String?> = java.util.ArrayList()
                    //header
                    if (data.isEmpty()) {
                        //newData.add(getSameData(data.get(0),"trefpversion"));
                        newData.add("nullHandshake")
                    } else {
                        if ((fwversion == "1.02" && modelo == "3.3") || (fwversion == "1.04" && modelo == "3.5")) { //nuevo logger, diferente división de información
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
                                        (data[i].substring(0, 8))
                                    ) + diferencialTimeStamp
                                )
                                date = Date.from(instant)
                                newData.add(date.toString())


                                val instant2 = Instant.ofEpochSecond(
                                    GetRealDataFromHexaImbera.getDecimal(
                                        (data[i].substring(8, 16))
                                    ) + diferencialTimeStamp2
                                )
                                date2 = Date.from(instant2)
                                newData.add(date2.toString())

                                newData.add(
                                     getEventTypeMONI (
                                        (data[i].substring(
                                            16,
                                            18
                                        ))
                                    )
                                ) //evento type

                                var numf = GetRealDataFromHexaImbera.getDecimalFloat(
                                    data[i].substring(
                                        18,
                                        22
                                    )
                                )
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

                                numf = GetRealDataFromHexaImbera.getDecimalFloat(
                                    data[i].substring(
                                        22,
                                        26
                                    )
                                )
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
                                    GetRealDataFromHexaImbera.getDecimal(
                                        data[i].substring(
                                            26
                                        )
                                    ).toString()
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
                                        (data[i].substring(0, 8))
                                    ) + diferencialTimeStamp
                                )
                                date = Date.from(instant)
                                newData.add(date.toString())

                                //Log.d("ASDASDASD",":"+data.get(i));
                                val instant2 = Instant.ofEpochSecond(
                                    GetRealDataFromHexaImbera.getDecimal(
                                        (data[i].substring(8, 16))
                                    ) + diferencialTimeStamp
                                )
                                date2 = Date.from(instant2)
                                newData.add(date2.toString())

                                newData.add(
                                     getEventTypeMONI (
                                        (data[i].substring(
                                            16,
                                            18
                                        ))
                                    )
                                ) //evento type

                                var numf = GetRealDataFromHexaImbera.getDecimalFloat(
                                    data[i].substring(
                                        18,
                                        22
                                    )
                                )
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

                                numf = GetRealDataFromHexaImbera.getDecimalFloat(
                                    data[i].substring(
                                        22,
                                        26
                                    )
                                )
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
                                    GetRealDataFromHexaImbera.getDecimal(
                                        data[i].substring(
                                            26
                                        )
                                    ).toString()
                                ) //decimales sin punto,voltaje
                                i++
                            } while (i < data.size)
                        }
                    }
                    return newData
                }

                else -> {
                    val newData: List<String?> = java.util.ArrayList()
                    return newData
                }
            }
        }
        fun GetRealDataMONI(data: List<kotlin.String>, action: kotlin.String?): MutableList<kotlin.String>? {
            //USO SOLO DE LOS DATOS BUFFER IMPORTANTES PARA MOSTRARLOS EN PANTALLA, LAS POSICIONES RESTANTES (HEADER) SON CORRECTAS
            return when (action) {
                "Handshake" -> {
                    val newData: MutableList<kotlin.String> = java.util.ArrayList()
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
                    val newData: MutableList<kotlin.String> = java.util.ArrayList()
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
                    val newData: MutableList<kotlin.String> = java.util.ArrayList()
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
                    val newData: MutableList<kotlin.String> = java.util.ArrayList()
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
                    val newData: MutableList<kotlin.String> = java.util.ArrayList()
                    val header: MutableList<kotlin.String> = java.util.ArrayList()
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
                    val newData: MutableList<kotlin.String> = java.util.ArrayList()
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

        fun convertMONI(arrayLists: List<kotlin.String?>, action: kotlin.String?): List<kotlin.String?>? {
            GetRealDataFromHexaImbera.arrayListInfo.clear()
            when (action) {
                "Handshake" -> {
                    if (!arrayLists.isEmpty()) {
                        val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists as List<kotlin.String>)
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
                        val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists as List<kotlin.String>)
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
                    val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists as List<kotlin.String>)

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
                        val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists as List<kotlin.String>)
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
                        val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists as List<kotlin.String>)
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
                        val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists as List<kotlin.String>)
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
                        val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists as List<kotlin.String>)
                        //header
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.toString()) //head
                    }
                }

                "Actualizar a Firmware Personalizado" -> {
                    if (!arrayLists.isEmpty()) {
                        val s = GetRealDataFromHexaImbera.cleanSpace(arrayLists as List<kotlin.String>)
                        //header
                        GetRealDataFromHexaImbera.arrayListInfo.add(s.toString()) //head
                    }
                }
            }
            return GetRealDataFromHexaImbera.arrayListInfo
        }

        private fun getActuadorMONI(s: kotlin.String): kotlin.String? {
            val ss = GetRealDataFromHexaImbera.HexToBinary(s)
            val stringBuilder = StringBuilder()
            var c: kotlin.String
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

        private fun getAlarmaMONI(s: kotlin.String): kotlin.String? {
            val ss = GetRealDataFromHexaImbera.HexToBinary(s)
            var isSensorCrash = false
            val stb = java.lang.StringBuilder()
            var c: kotlin.String
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

        private fun getEventTypeMONI(s: kotlin.String): kotlin.String? {
            var evento = ""
            var c: kotlin.String
            when (s) {
                "04" -> evento = "Falla de energía"
                "03" -> evento = "Ciclo de deshielo"
                "02" -> evento = "Ciclo de compresor"
                "01" -> evento = "Apertura de puerta"
            }
            return evento
        }




        fun traducirValores(valor: kotlin.String): kotlin.String? {
            val j =  getDecimalFloat(
                valor.toString()
            ) // decimales con punto

            if (j > 99.9) {
                //Extraccion de temperaturas en decimales
                //newData.add(getNegativeTemp("FFFF"+data.get(i)));

                return getNegativeTemp(
                    "FFFF" + valor.toString()
                )
            } else {
                //newData.add(String.valueOf(getDecimalFloat(data.get(i)) )); // decimales con punto
                return java.lang.String.valueOf(
                    getDecimalFloat(
                        valor.toString()
                    )
                )
            }
        }

        fun getNegativeTemp(hexaTemp: String): String? {
            val parsedResult = hexaTemp.toLong(16).toInt()
            val result = parsedResult.toDouble() / 10.0
            return result.toString()
        }
        fun getDecimalFloat(hex: String): Float {
            var hex = hex

            val digits = "0123456789ABCDEF"
            hex = hex.uppercase(Locale.getDefault())
            var `val` = 0f
            for (i in 0 until hex.length) {
                val c = hex[i]
                val d = digits.indexOf(c)
                `val` = 16 * `val` + d
            }
            return `val` / 10
        }
        fun getNegativeEntero(hexaTemp: String): String {
            val parsedResult = hexaTemp.toLong(16).toInt()
            return (parsedResult).toString()
        }
        fun getNegativeTempfloat(hexaTemp: String): String {
            val parsedResult: Float = hexaTemp.toLong(16).toInt().toFloat()
            return (parsedResult / 10).toString()
        }

        fun ajusteCampoLarge(dato: String, rangoToPadStart: Int): String {
            return if (dato.length > rangoToPadStart) {
                dato.substring(dato.length - rangoToPadStart, dato.length)
            } else {
                dato.padStart(rangoToPadStart, '0')
            }
        }

        fun convertDecimalToHexa(s: String): String {
            val f = s.toFloat()
            val nm = f.toInt()
            val c = Integer.toHexString(nm)
            return when (c.length) {
                1 -> "000$c"
                2 -> "00$c"
                3 -> "0$c"
                else -> c
            }
        }

        fun ConvierteExa(valor: String): String {
            val numf = valor.toFloat()
            val num = numf.toInt()
            return when {
                Integer.signum(num) == 1 -> convertDecimalToHexa(valor).toUpperCase() // positive numbers
                Integer.signum(num) == -1 -> getNeg(numf) // negative numbers
                else -> convertDecimalToHexa(valor) // zero
            }
        }

        fun getNeg(`val`: Float): String {
            val numfi = (`val` * 10).toInt()
            val hex = Integer.toHexString(numfi)
            return hex.substring(hex.length - 4)
        }


    }
}