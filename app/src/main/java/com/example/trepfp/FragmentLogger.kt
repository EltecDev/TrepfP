package com.example123.trepfp

import BluetoothServices.BluetoothServices
import Utility.GetRealDataFromHexaImbera.cleanSpace
import Utility.GetRealDataFromHexaOxxoDisplay
import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import mx.eltec.BluetoothServices.BluetoothLeService
import java.lang.Long.toHexString
import java.text.DecimalFormat


class FragmentLogger (BluetoothService: BluetoothServices?,context: Context,conexiontrefp: ConexionTrefp): Fragment(){
    var bluetoothService: BluetoothServices? = null
    var bluetoothLeService: BluetoothLeService? = null
    val TAG = "FragmentLogger"

    var listaDatosObtenidos: MutableList<String> = ArrayList()
    var listaDatosObtenidosAjustadosTiempo: MutableList<String> = ArrayList()
    var listaDatosObtenidosAjustadosEvento: MutableList<String> = ArrayList()

    //inicialización de variables
    var listaInicialDatosTiempoOriginal: List<String> = ArrayList()
    var listaInicialDatosEventoOriginal: List<String> = ArrayList()
    var listaInicialDatosTiempoFiltrada: List<String> = ArrayList()
    var listaInicialDatosEventoFiltrada: List<String> = ArrayList()
    var listaFinalDatosTiempoAjustado: List<String> = ArrayList()
    var listaFinalDatosEventoAjustado: List<String> = ArrayList()
    var listaInicialDatosTiempoFiltradaErrores: List<String> = ArrayList()
    var listaInicialDatosEventosFiltradaErrores: List<String> = ArrayList()

    //Variables de algoritmo
    //dt
    var UD = 0
    var UE = 0
    var T33CB2 = 0L
    var CElTime2 = 0L
    var dTm = 0L
    var dS = 0.0 //diferencial de setpoint T1
    var SP = 0.0 //
    var E = 0L
    var F = 0L // F es long y se divide 600000 entre ds, diferencial de setpoint
    var ToffF = 200000.0


    //Pantalla de peticion inicial de permisos
    var v: View? = null
    var progressdialog: AlertDialog? = null
    var dialogViewProgressBar: View? = null

    var sp: SharedPreferences? = null
    var esp: SharedPreferences.Editor? = null

    init {
        this@FragmentLogger.bluetoothService = BluetoothService
        sp = context.getSharedPreferences("connection_preferences", Context.MODE_PRIVATE)
        esp = sp?.edit()
    }

    // private conexiontrefp conexiontrefp ;
    private var conexiontrefp: ConexionTrefp? = null

    /*override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.show_progress_bar, container, false)
        init(view)
        view.findViewById<View>(R.id.btnGetCurrentLogger).setOnClickListener {
            MyAsyncTaskPedirPLantilla().execute()
        }
        view.findViewById<View>(R.id.btnGrabarHora).setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                actualizarHora()
            }
        }
        view.findViewById<View>(R.id.btnLimpiarLogger).setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                borrarLogger()
                Toast.makeText(requireContext(), "Logger eliminado", Toast.LENGTH_SHORT).show()
            }
        }
        return view
    }
*/
    private fun init(v: View) {
        this.v = v
    }

    fun tiempoDiferencia(tiempoMeses: Int):Long {
        val diasMes = tiempoMeses * 30.44 //dias promedio por mes

        return (diasMes*86400).toLong()
    }

    internal inner class MyAsyncTaskPedirPLantilla(callback2: com.example123.trepfp.ConexionTrefp.CallbackLogger) : AsyncTask<Int?, Int?, String>() {
        var callback = callback2
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        protected override fun doInBackground(vararg params: Int?): String? {
            try {

//
                //bluetoothLeService = bluetoothService!!.bluetoothLeService//inicializar objeto que manda comandos
                bluetoothLeService = conexiontrefp!!.bluetoothLeService
                bluetoothLeService!!.sendFirstComando("4051")
                Thread.sleep(400)

                var Plantilla =
                    bluetoothLeService!!.listData.joinToString("").replace(" ", "").substring(18)

                // valores desde la plantilla
                var preAT = "2"
                var preT1 = "0"
                var preT0 = "0"

                preAT = Plantilla.substring(176, 178)
                preT0 = Plantilla.substring(0, 4)
                preT1 = Plantilla.substring(4, 8)

                Log.d(TAG, "PLANTILLA:$Plantilla")
                Log.d(TAG, "preAT:$preAT")
                Log.d(TAG, "preT1:$preT1")
                Log.d(TAG, "preT0:$preT0")

                val df = DecimalFormat("#.##")
                dTm = preAT.toLong(16)*60//.times(1000)   // AQUI SE PASA EL VALOR A segundos
                SP = convertirHexATemp(preT0).toDouble()
                dS = df.format(GetRealDataFromHexaOxxoDisplay.getDecimalFloat(preT1)).toDouble()

                Log.d(TAG, "preT0 dTm:$dTm")
                Log.d(TAG, "preT1 SP:$SP")
                Log.d(TAG, "preAT dS:$dS")

                //verificar si el tiempo que pasó desde el CElTime1 supera los 100 segundos a este momento
                var CElTime1 = sp!!.getString("CElTime1","")!!.toLong()
                val tiempoActual = GetNowDateExa().toLong(16)
                Log.d(TAG, "Comparación de tiempos")
                Log.d(TAG, "CElTime1:$CElTime1")
                Log.d(TAG, "tiempoActual:$tiempoActual")
                Log.d(TAG, "tiempoActual diferencia:${tiempoActual - CElTime1})")
                if ((tiempoActual - CElTime1) < 100){
                    Thread.sleep(100000)
                }

                return "not"
            } catch (e: InterruptedException) {
                e.printStackTrace()
                return "exception"
            }
        }

        override fun onPostExecute(result: String) {
            MyAsyncTaskLoggerTiempo(callback).execute()
        }

        override fun onPreExecute() {
            callback.onProgress("Iniciando proceso de obtención de información Logger")
            /*createProgressDialog("Pedir plantilla: En proceso")*/
        }

        protected override fun onProgressUpdate(vararg values: Int?) {}
    }

    internal inner class MyAsyncTaskLoggerTiempo(callback2: com.example123.trepfp.ConexionTrefp.CallbackLogger) : AsyncTask<Int?, Int?, String>() {
        var callback = callback2
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        protected override fun doInBackground(vararg params: Int?): String? {
            try {

                bluetoothLeService = bluetoothService!!.bluetoothLeService


                //Se pide el logger al control DATOS DE TIEMPO
                //Limpiar las listas antes de empezar la recolección
                bluetoothLeService!!.clearListLogger()
                listaDatosObtenidos.clear()

                bluetoothLeService!!.sendFirstComando("4060") //Mandar comando a control de extracción de datos de tiempo
                Thread.sleep(14000)//Esperar la extracción del logger
                listaDatosObtenidos =
                    bluetoothLeService!!.getLogeer()!! //Obtener el logger en una lista
                listaInicialDatosTiempoOriginal = listaDatosObtenidos.toList()
                Thread.sleep(1000)

                return "not"
            } catch (e: InterruptedException) {
                e.printStackTrace()
                return "exception"
            }
        }

        override fun onPostExecute(result: String) {
            MyAsyncTaskLoggerEvento(callback).execute()
        }

        override fun onPreExecute() {
            callback.onProgress("Obteniendo datos de Tiempo")
            /*editarTextoMensajeProgressDialogAsync("Pedir plantilla: Listo\nPedir Logger Tiempo: En proceso")*/
        }

        protected override fun onProgressUpdate(vararg values: Int?) {}
    }

    internal inner class MyAsyncTaskLoggerEvento(callback2: ConexionTrefp.CallbackLogger) : AsyncTask<Int?, Int?, String>() {
        var callback = callback2
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        protected override fun doInBackground(vararg params: Int?): String? {
            try {
                bluetoothLeService = bluetoothService!!.bluetoothLeService

                //createProgressDialog("Obteniendo datos de Evento")
                //Se pide el logger al control DATOS DE Evento
                //Limpiar las listas antes de empezar la recolección
                bluetoothLeService!!.clearListLogger()
                listaDatosObtenidos.clear()

                bluetoothLeService!!.sendFirstComando("4061") //Mandar comando a control de extracción de datos de evento
                Thread.sleep(22000)
                listaDatosObtenidos =
                    bluetoothLeService!!.getLogeer()!! //Obtener el logger en una lista
                listaInicialDatosEventoOriginal = listaDatosObtenidos.toList()
                Thread.sleep(1000)

                return "not"
            } catch (e: InterruptedException) {
                e.printStackTrace()
                return "exception"
            }
        }

        override fun onPostExecute(result: String) {
            listaInicialDatosTiempoOriginal.map {
                Log.d(TAG, it)
            }
            /*Log.d(TAG, "onPostExecute evento:${listaInicialDatosEventoOriginal.size}")
            listaInicialDatosEventoOriginal.map {
                Log.d(TAG, "onPostExecute Evento:$it")
            }*/
            MyAsyncTaskPedirHora2(callback).execute()
        }

        override fun onPreExecute() {
            callback.onProgress("Obteniendo datos de Evento")
            /*editarTextoMensajeProgressDialogAsync("Pedir plantilla: Listo\nPedir Logger Tiempo: Listo\nPedir Logger Evento: En proceso")*/
        }

        protected override fun onProgressUpdate(vararg values: Int?) {}
    }

    internal inner class MyAsyncTaskOrganizarDatosLogger(callback2: ConexionTrefp.CallbackLogger) : AsyncTask<Int?, Int?, String>() {
        var callback = callback2
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        protected override fun doInBackground(vararg params: Int?): String? {
            try {
                VTIME()
                Thread.sleep(2000)
                VEvent()
                Thread.sleep(2000)
                return "not"
            } catch (e: InterruptedException) {
                e.printStackTrace()
                return "exception"
            }
        }

        override fun onPostExecute(result: String) {
            MyAsyncTaskAlgoritmoDeAjuste(callback).execute()
        }

        override fun onPreExecute() {
            callback.onProgress("Organizando datos")
            /*editarTextoMensajeProgressDialogAsync("Pedir plantilla: Listo\nPedir Logger Tiempo: Listo\nPedir Logger Evento: En proceso\nPedir Hora 2: Listo\nOrganizar datos: En proceso")*/
        }

        protected override fun onProgressUpdate(vararg values: Int?) {}
    }

    internal inner class MyAsyncTaskPedirHora2(callback2: ConexionTrefp.CallbackLogger) : AsyncTask<Int?, Int?, String>() {
        var callback = callback2
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        protected override fun doInBackground(vararg params: Int?): String? {
            try {
                bluetoothLeService = bluetoothService!!.bluetoothLeService

                // SE OBTIENE EL VALOR DE LA HORA DEL CONTROL
                bluetoothLeService!!.clearListLogger()
                bluetoothLeService!!.clearData()

                bluetoothLeService?.sendFirstComando("405B")
                Thread.sleep(400)
                // se obtiene el valor del buffer se le quita los espacios y se obtiene el valor del control y se pasa a milisegundos
                T33CB2 = bluetoothLeService!!.listData.joinToString("").replace(" ", "")
                    .substring(16, 24).toLong(16)//.times(1000)

                // SE OBTIENE LA HORA DEL CELULAR Y SE PASA A MILISEGUNDOS
                CElTime2 = GetNowDateExa().toLong(16)//.times(1000)

                Log.d(TAG, "Hora CB133 2:$T33CB2 CElTime2:$CElTime2")

                return "not"
            } catch (e: InterruptedException) {
                e.printStackTrace()
                return "exception"
            }
        }

        override fun onPostExecute(result: String) {
            MyAsyncTaskOrganizarDatosLogger(callback).execute()
        }

        override fun onPreExecute() {
            /*editarTextoMensajeProgressDialogAsync("Pedir plantilla: Listo\nPedir Logger Tiempo: Listo\nPedir Logger Evento: En proceso\nPedir Hora 2:En proceso")*/
        }

        protected override fun onProgressUpdate(vararg values: Int?) {}
    }

    internal inner class MyAsyncTaskAlgoritmoDeAjuste(callback2: ConexionTrefp.CallbackLogger) : AsyncTask<Int?, Int?, String>() {
        var callback = callback2
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        protected override fun doInBackground(vararg params: Int?): String? {
            try {
                algoritmoAjusteDeDatos()
                Thread.sleep(2000)
                return "not"
            } catch (e: InterruptedException) {
                e.printStackTrace()
                return "exception"
            }
        }

        override fun onPostExecute(result: String) {
            limpiarVariables()
            callback.onSuccess(true)
        }

        override fun onPreExecute() {
            /*editarTextoMensajeProgressDialogAsync("Pedir plantilla: Listo\nPedir Logger Tiempo: Listo\nPedir Logger Evento: En proceso\nPedir Hora 2: Listo\nOrganizar datos: Listo\nAlgoritmo de ajuste: En proceso")*/
        }

        protected override fun onProgressUpdate(vararg values: Int?) {}
    }

    fun limpiarVariables(){
        listaDatosObtenidos.clear()
        listaDatosObtenidosAjustadosTiempo.clear()
        listaDatosObtenidosAjustadosEvento.clear()

        /*listaInicialDatosTiempoOriginal = emptyList()
        listaInicialDatosEventoOriginal = emptyList()
        listaInicialDatosTiempoFiltrada = emptyList()
        listaInicialDatosEventoFiltrada = emptyList()
        listaFinalDatosTiempoAjustado = emptyList()
        listaFinalDatosEventoAjustado = emptyList()*/

        UD = 0
        UE = 0
        T33CB2 = 0L
        CElTime2 = 0L
        dTm = 0L
        dS = 0.0 //diferencial de setpoint T1
        SP = 0.0 //
        E = 0L
        F = 0L // F es long y se divide 600000 entre ds, diferencial de setpoint
        ToffF = 600000.0
    }

    fun VTIME() {
        var arrayListInfo = mutableListOf<String>()

        if (listaInicialDatosTiempoOriginal.isNotEmpty()) {
            val s = cleanSpace(listaInicialDatosTiempoOriginal)
            val datos128B: MutableList<String> = java.util.ArrayList()
            var nuevaLista128: List<String> = ArrayList()
            val datos9B: MutableList<String> = java.util.ArrayList()


            //header
            arrayListInfo.add(s.substring(0, 4))
            arrayListInfo.add(s.substring(4, 12))
            arrayListInfo.add(s.substring(12, 14))
            arrayListInfo.add(s.substring(14, 16))

            val st = StringBuilder()
            st.append(s.substring(16, s.length - 8))


            var i = 0
            do { //dividir toda la información en paquetes de 128 bytes
                if (i + 256 > st.length) {
                    datos128B.add(st.substring(i)) //checksum
                    break
                } else
                    datos128B.add(st.substring(i, i + 256))
                i += 256
            } while (i < st.length)


            /*Log.d("crudotiempoTIEMPO","Paquetes de 128 byte viejos T:")
            datos128B.map {
                Log.d(TAG,it)
            }*/


            //SE OBTIENE LOS DATOS MAS VIEJOS Y MAS NUEVOS DE LA ULTIMA FILA


            listaDatosObtenidos.clear()
            val datoSeparador = datos128B[datos128B.size - 1].substring(
                datos128B[datos128B.size - 1].length - 4,
                datos128B[datos128B.size - 1].length - 2
            )
            if (datoSeparador == "00" || datoSeparador == "7D") {//no hay datos o est[an llenos o sea toda la ultima fila es dato nuevo

            } else {
                val datoSeparadorDecimal =
                    GetRealDataFromHexaOxxoDisplay.getDecimal(datoSeparador) * 2//*2 porque viene en bytes
                listaDatosObtenidos.add(
                    datos128B[datos128B.size - 1].substring(
                        0,
                        datoSeparadorDecimal
                    )
                )

                listaDatosObtenidos.add(datos128B[datos128B.size - 1].substring(datoSeparadorDecimal))
                //se agregan los nuevos datos, mas nuevos al final y mas viejos al incio, se voltea la lista al final despues de tenerlos en 9 bytes
                datos128B.removeAt(datos128B.size - 1)//quitar ultima fila
                datos128B.add(
                    datos128B.size,
                    listaDatosObtenidos[0]
                )//la nueva ultima fila son las datos mas nuevos
                datos128B.add(
                    0,
                    listaDatosObtenidos[1]
                )//la nueva primera fila son los datos mas viejos
            }

            Log.d("crudotiempoTIEMPO","Paquetes de 128 byte nuevos T size:${datos128B.size}")
            datos128B.map {
                Log.d("crudotiempoTIEMPO","Paquetes de 128 byte nuevos T:$it")
            }


            var j = 0//j en 1 porque ya use la primea fila para sacar los nuevos y viejos
            do { //dividir los paquetes de 9 bytes
                i = 0
                while (i < datos128B[j].length) {
                    if (i + 18 > datos128B[j].length) {
//                        Log.d("crudotiempoTIEMPO","Paquetes de 2 byte:${datos128B[j].substring(i)}")
                        datos9B.add(datos128B[j].substring(i)) //checksum
                        break
                    } else {
                        datos9B.add(datos128B[j].substring(i, i + 18))
                        //Log.d("crudotiempoTIEMPO","Paquetes de 9 byte:${datos128B[j].substring(i, i + 18)}")
                    }


                    i += 18
                }
                j++
            } while (j < datos128B.size)







            listaDatosObtenidos.clear()
            var o = 0
            var datos0a = 0
            var datos00 = 0
            while (o < datos9B.size) {
                if (datos9B[o].length != 4 && datos9B[o] != "000000000000000000" && datos9B[o] != "a0a0a0a0a0a0a0a0a0") {
                    //Log.d("crudotiempoTIEMPO","crudotiempoFOR:" + arrayListInfo[h])
                    //se valen las fracciones mientras se esta operando el numero pero todo va en enteros
                    var datomili = datos9B[o].substring(0, 8).toLong(16)/*.times(1000)*/
                    val tiempoDiferencia = tiempoDiferencia(18)
                    if (datomili > T33CB2 - tiempoDiferencia && datomili < T33CB2)//verificar que no supera los 3 meses (7776000) segundos
                        listaDatosObtenidos.add(datos9B[o])
                    //Log.d(TAG, "Dato en miliseconds:$datomili")
                }else{
                    if (datos9B[o] == "000000000000000000") {
                        datos00++
                    }
                    if (datos9B[o] == "0a0a0a0a0a0a0a0a0a") {
                        datos0a++
                    }
                    //listaDatosObtenidos.add(datos2[o])

                }
                o++
            }
            Log.d("crudotiempoTIEMPO", "Errores 00:$datos00")
            Log.d("crudotiempoTIEMPO", "Errores 0a:$datos0a")

        }

        UD = listaDatosObtenidos.size
        listaInicialDatosTiempoFiltrada = listaDatosObtenidos.reversed().toList()
        Log.d("crudotiempoTIEMPO", "Paquetes de 9byte Filtrada size T:${listaInicialDatosTiempoFiltrada.size}")
        /*listaInicialDatosTiempoFiltrada.map {
            Log.d("crudotiempoTIEMPO", "Paquetes de 9byte Filtrada:$it")
        }*/
    }

    fun VEvent() {
        var arrayListInfo = mutableListOf<String>()
        var listaFinal = mutableListOf<String>()
        val s = cleanSpace(listaInicialDatosEventoOriginal)
        if (!listaInicialDatosEventoOriginal.isEmpty() && s.toString().length >= 16) {

            val datos128B: MutableList<String> = java.util.ArrayList()
            val datos9B: MutableList<String> = java.util.ArrayList()
            //header
            arrayListInfo.add(s.substring(0, 4))
            arrayListInfo.add(s.substring(4, 12))
            arrayListInfo.add(s.substring(12, 14))
            arrayListInfo.add(s.substring(14, 16))
            val st = StringBuilder()
            st.append(s.substring(16, s.length - 8))


            var i = 0
            do { //dividir toda la información en paquetes de 128 bytes
                if (i + 256 > st.length) {
                    datos128B.add(st.substring(i)) //checksum
                    break
                } else datos128B.add(st.substring(i, i + 256))
                i = i + 256
            } while (i < st.length)

            /*Log.d("crudotiempoTIEMPO","Paquetes de 128 byte viejos E:")
            datos128B.map {
                Log.d(TAG,it)
            }*/


            //SE OBTIENE LOS DATOS MAS VIEJOS Y MAS NUEVOS DE LA ULTIMA FILA

            listaDatosObtenidos.clear()
            val datoSeparador = datos128B[datos128B.size - 1].substring(
                datos128B[datos128B.size - 1].length - 4,
                datos128B[datos128B.size - 1].length - 2
            )
            if (datoSeparador == "00" || datoSeparador == "7D") {//no hay datos o est[an llenos o sea toda la ultima fila es dato nuevo

            } else {
                val datoSeparadorDecimal =
                    GetRealDataFromHexaOxxoDisplay.getDecimal(datoSeparador) * 2//*2 porque viene en bytes
                listaDatosObtenidos.add(
                    datos128B[datos128B.size - 1].substring(
                        0,
                        datoSeparadorDecimal
                    )
                )


                listaDatosObtenidos.add(datos128B[datos128B.size - 1].substring(datoSeparadorDecimal))
                //se agregan los nuevos datos, mas nuevos al final y mas viejos al incio, se voltea la lista al final despues de tenerlos en 9 bytes
                datos128B.removeAt(datos128B.size - 1)//quitar ultima fila
                datos128B.add(
                    datos128B.size,
                    listaDatosObtenidos[0]
                )//la nueva ultima fila son las datos mas nuevos
                datos128B.add(
                    0,
                    listaDatosObtenidos[1]
                )//la nueva primera fila son los datos mas viejos
            }

            Log.d("crudotiempoTIEMPO","Paquetes de 128 byte nuevos E:${datos128B.size}")
            datos128B.map {
                Log.d("crudotiempoTIEMPO","Paquetes de 128 byte nuevos E:$it")
            }


            var j = 0
            do { //dividir los paquetes de 14 bytes según el protocolo
                i = 0
                while (i < datos128B[j].length) {
                    if (i + 28 > datos128B[j].length) {
                        datos9B.add(datos128B[j].substring(i)) //checksum
                        break
                    } else datos9B.add(datos128B[j].substring(i, i + 28))
                    i += 28
                }
                j++
            } while (j < datos128B.size)


            /*datos9B.map {
                Log.d("crudotiempoTIEMPO","Paquetes de 14 byte nuevos E:$it")
            }*/

            //organizar la información que realmente sirve (quitar 0s)
            listaDatosObtenidos.clear()
            var o = 0
            var h=0
            var datos0a = 0
            var datos00 = 0
            while (o < datos9B.size) {
                if (datos9B[o].length != 4 && datos9B[o] != "0000000000000000000000000000" && datos9B[o] != "0a0a0a0a0a0a0a0a0a0a0a0a0a0a") {
                    var datomili = datos9B[o].substring(0, 8).toLong(16)//.times(1000)
                    val tiempoDiferencia = tiempoDiferencia(8)
                    //verificar que no supera los 3 meses (7776000) segundos
                    if (datomili > T33CB2 - tiempoDiferencia && datomili < T33CB2){
                        //test de temperaturas cambiadas en eventos 04
                        /*if (datos9B[o].substring(16,18) == "04"){
                            var nuevo = StringBuilder()
                            nuevo.append(datos9B[o].substring(0,18))
                            //10 = 100
                            //15 = 150
                            if (h==0){
                                nuevo.append("0064")
                                h++
                            }else{
                                nuevo.append("0096")
                            }

                            nuevo.append(datos9B[o].substring(22))
                            listaDatosObtenidos.add(nuevo.toString())
                        }else{
                            listaDatosObtenidos.add(datos9B[o])
                        }*/
                        listaDatosObtenidos.add(datos9B[o])

                    }
                }else {
                    if (datos9B[o] == "0000000000000000000000000000" ) {
                        datos00++
                    }
                    if (datos9B[o] == "0a0a0a0a0a0a0a0a0a0a0a0a0a0a") {
                        datos0a++
                    }
                    //listaInicialDatosEventosFiltradaErrores.add(datos2[o])
                }
                o++
            }
            Log.d("crudotiempoTIEMPO", "Errores 00:$datos00")
            Log.d("crudotiempoTIEMPO", "Errores 0a:$datos0a")

        }
        UE = listaDatosObtenidos.size
        listaInicialDatosEventoFiltrada = listaDatosObtenidos.reversed().toList()
        Log.d(TAG, "Datos Filtrados  Evento size:${listaInicialDatosEventoFiltrada.size}")
        /*listaInicialDatosEventoFiltrada.map {
            Log.d("crudotiempoTIEMPO", "Paquetes de 14 byte Filtrada E:$it")
        }*/

    }

    fun algoritmoAjusteDeDatos(){

        /*Log.d(TAG, "Datos Filtrados  Tiempo:")
        listaInicialDatosTiempoFiltrada.map {
            Log.d("", it)//Revisión de los datos extraidos
        }

        Log.d(TAG, "Datos Filtrados  Evento:${listaInicialDatosEventoFiltrada.size}")
        listaInicialDatosEventoFiltrada.map {
            Log.d("", it)//Revisión de los datos extraidos
        }*/
        var T33CB1 = sp!!.getString("T33CB1","")!!.toLong()
        var CElTime1 = sp!!.getString("CElTime1","")!!.toLong()

        var A = T33CB2 - T33CB1
        var B = CElTime2 - CElTime1
        var C = -10.0f//((A-B)/B)*dTm // esto calcula el error del tiempo de atraso entre grabaciones causadas por el reloj del control
        var D = T33CB1 - CElTime1
        Log.d(TAG, "INICIO DE AJUSTE")
        Log.d(TAG, "algoritmoAjusteDeDatos A:$A ")
        Log.d(TAG, "algoritmoAjusteDeDatos B:$B ")
        Log.d(TAG, "algoritmoAjusteDeDatos C:$C ")
        Log.d(TAG, "algoritmoAjusteDeDatos Operacion C Operacion:${((A-B)/B)*dTm}")
        var AV = 0
        var AVN = 0

//        d debe ser fijo
//        j y e en 0 solo con D
//        luego la e
        //d es la resta de 170millones,debe ser to do en long
        var n = 0
        var J = 0
        E = 0
        ToffF = 600.0
        F = (ToffF/dS).toLong()

        Log.d(TAG, "algoritmoAjusteDeDatos T33CB1:$T33CB1 ")
        Log.d(TAG, "algoritmoAjusteDeDatos T33CB2:$T33CB2 ")
        Log.d(TAG, "algoritmoAjusteDeDatos CElTime1:$CElTime1 ")
        Log.d(TAG, "algoritmoAjusteDeDatos CElTime2:$CElTime2 ")

        while (AV<listaInicialDatosTiempoFiltrada.size){
            Log.d(TAG, "---------NUEVO CICLO DE AJUSTE---------")
            Log.d(TAG, "algoritmoAjusteDeDatos n:$n")
            Log.d(TAG, "algoritmoAjusteDeDatos AV:$AV ")
            Log.d(TAG, "algoritmoAjusteDeDatos F:$F ")
            Log.d(TAG, "algoritmoAjusteDeDatos E:$E ")
            Log.d(TAG, "algoritmoAjusteDeDatos D:$D ")
            Log.d(TAG, "algoritmoAjusteDeDatos J:$J ")

            if (n<listaInicialDatosEventoFiltrada.size  && F1(AV, n)){
                AE(n,D,J) //Ajuste de eventos
                n++//paso al siguiente evento
            }else{
                //esto va afuera porque el while no est[a recorriendo los datos de tiempo

                var Tm = listaInicialDatosTiempoFiltrada[AV].substring(0,8).toInt(16)//.times(1000)
                Log.d(TAG, "algoritmoAjusteDeDatos datoViejoTiempo:${listaInicialDatosTiempoFiltrada[AV]}")
                Log.d(TAG, "algoritmoAjusteDeDatos datoViejoTiempo TmOri:${Tm}")
                Log.d(TAG, "algoritmoAjusteDeDatos datoViejoTiempo Tmcuenta:${Tm - D - E + J}")
                Tm = ((Tm - D - E + J)).toInt()
                Log.d(TAG, "algoritmoAjusteDeDatos datoViejoTiempo:${Tm}")
                val datoTiempoAjustado = StringBuilder()
                datoTiempoAjustado.append(Tm.toString(16))
                datoTiempoAjustado.append(listaInicialDatosTiempoFiltrada[AV].substring(8))
                listaDatosObtenidosAjustadosTiempo.add(datoTiempoAjustado.toString())
                Log.d(TAG, "algoritmoAjusteDeDatos datoNueviTiempo:$datoTiempoAjustado ")
                J = (C * AV).toInt()
                AV++
            }
        }

        while (n<listaInicialDatosEventoFiltrada.size){//Se termina de ajustar los eventos cuando ya no hay registros de tiempo    TAMBIEN LE QUITE EL =
            Log.d(TAG, "algoritmoAjusteDeDatos ENTRA A SEGUNDO CICLO DE AJUSTE ")
            Log.d(TAG, "algoritmoAjusteDeDatos n:$n ")
            Log.d(TAG, "algoritmoAjusteDeDatos AV:$AV ")
            Log.d(TAG, "algoritmoAjusteDeDatos F:$F ")
            Log.d(TAG, "algoritmoAjusteDeDatos E:$E ")
            Log.d(TAG, "algoritmoAjusteDeDatos D:$D ")
            Log.d(TAG, "algoritmoAjusteDeDatos J:$J ")
            AE(n,D,J) //Ajuste de eventos
            n++//paso al siguiente evento
        }

        listaFinalDatosTiempoAjustado = listaDatosObtenidosAjustadosTiempo.toList()
        listaFinalDatosEventoAjustado = listaDatosObtenidosAjustadosEvento.toList()
        Log.d(TAG, "Datos  ajustados Tiempo size:${listaFinalDatosTiempoAjustado.size}")
        /*listaFinalDatosTiempoAjustado.map {
            Log.d("", it)//Revisión de los datos extraidos
        }*/

        Log.d(TAG, "Datos  ajustados Evento size:${listaFinalDatosEventoAjustado.size}")
        /*listaFinalDatosEventoAjustado.map {
            Log.d("", it)//Revisión de los datos extraidos
        }*/
    }

    fun F1(av: Int,n : Int): Boolean{
        val Tm0 = listaInicialDatosTiempoFiltrada[0].substring(0,8).toLong(16)//.times(1000)
        val TmAV = listaInicialDatosTiempoFiltrada[av].substring(0,8).toLong(16)//.times(1000)
        var TmAVMas1 = 0L
        if (av+1==listaInicialDatosTiempoFiltrada.size){
            TmAVMas1 = listaInicialDatosTiempoFiltrada[av].substring(0,8).toLong(16)//.times(1000)
        }else{
            TmAVMas1= listaInicialDatosTiempoFiltrada[av+1].substring(0,8).toLong(16)//.times(1000)
        }

        val TFiEv = listaInicialDatosEventoFiltrada[n].substring(0,8).toLong(16)//.times(1000)

        Log.d(TAG, "F1 Tm:$Tm0 ")
        Log.d(TAG, "F1 Tm2:$TmAVMas1 ")
        Log.d(TAG, "F1 TFiev:$TFiEv ")

        val TFiEvMenorqueTm = TFiEv > Tm0

        if (av ==0 && TFiEvMenorqueTm){
            return true
        }else{
            if (TFiEv <= TmAV){
                if (TFiEv>TmAVMas1){
                    return true
                }else
                    return false
            }else
                return false
        }

    }

    fun AE(n: Int, D: Long, J: Int){
        F = CF(n)

        val TIniEv = listaInicialDatosEventoFiltrada[n].substring(0,8).toLong(16)//.times(1000)
        val TFinEv = listaInicialDatosEventoFiltrada[n].substring(8,16).toLong(16)//.times(1000)
        Log.d(TAG, "AE TIniEv:$TIniEv ")
        Log.d(TAG, "AE TFinEv:$TFinEv ")

        var TFinEvAjustado = 0L

        if (listaInicialDatosEventoFiltrada[n].substring(16,18) == "04"){
            if (TFinEv==TIniEv){
                val CFinEv = convertirHexATemp(listaInicialDatosEventoFiltrada[n].substring(18,22)) //temperatura final del evento n
                var dCE = CFinEv - (SP + dS)
                if (dCE<0){
                   dCE = 0.1
                }
                Log.d(TAG, "algoritmoAjusteDeDatos dCE:$dCE ")
                TFinEvAjustado = (TFinEv - D - E - J).toLong()
                E = (E + (dCE * F)).toLong()

            }else{
                if (TFinEv>TIniEv){
                    TFinEvAjustado = (TFinEv - D - E - J).toLong()
                }else{
                    //borrar el resto de datos de evento hacia atras incluyendo los eventos
                    var indice = n
                    while (indice<listaInicialDatosEventoFiltrada.size){
                        //listaInicialDatosEventoFiltrada.removeAt(n)
                        //REVISAR NO ESTA COMPLETADO, como remuevo?
                        indice++
                    }
                }
            }
        }else{
            TFinEvAjustado = (TFinEv - D - E - J).toLong()
        }
        Log.d(TAG, "AE TFinEvAjustado:$TFinEvAjustado ")
        //Ajuste de evento del inicio del evento
        val TIniEvAjustado = (TIniEv - D - E - J).toLong()
        Log.d(TAG, "AE TIniEvAjustado:$TIniEvAjustado ")
        val nuevoEvntoAjustado = StringBuilder()
        nuevoEvntoAjustado.append(TIniEvAjustado.toString(16))
        nuevoEvntoAjustado.append(TFinEvAjustado.toString(16))
        nuevoEvntoAjustado.append(listaInicialDatosEventoFiltrada[n].substring(16))

        Log.d(TAG, "AE nuevoEvntoAjustado:$nuevoEvntoAjustado")
        listaDatosObtenidosAjustadosEvento.add(nuevoEvntoAjustado.toString())



    }

    fun CF(n: Int): Long {
        if (listaInicialDatosEventoFiltrada[n].substring(16,18) == "02"){
            if (listaInicialDatosEventoFiltrada[n+1].substring(16,18) == "02"){
                val TInEv = listaInicialDatosEventoFiltrada[n].substring(0,8).toLong(16)//.times(1000)
                val TFinEv = listaInicialDatosEventoFiltrada[n+1].substring(8,16).toLong(16)//.times(1000)
                var Toff = TInEv - TFinEv
                //si es el Toff calculado es menor que el que ya se tenia quedarse con F
                val ToffFViejo = F
                ToffF = (Toff+Toff*0.2)/1.2
                Log.d(TAG, "algoritmoAjusteDeDatos Toff:$Toff ")
                Log.d(TAG, "algoritmoAjusteDeDatos ToffF:$ToffF ")
                if(F > ToffF){
                    return F
                }else{
                    return (ToffF/dS).toLong()
                }


            }
        }
        return F
    }

    fun convertirHexATemp(data: String): Float {
        val numf =GetRealDataFromHexaOxxoDisplay.getDecimalFloat(data)
        val num = numf.toInt()
        if (num < 99.99) {
            return GetRealDataFromHexaOxxoDisplay.getDecimalFloat(data) //decimales con punto //get temp positivo
        } else if (num > 99.99) {
            val ndata = StringBuilder()
            ndata.append("FFFF")
            ndata.append(data)
            return (ndata.toString().toLong(16)).toFloat()/ 10;
        } else { //Es 0 cero
            return 0.0f //get negativos
        }
    }

    private fun GetNowDateExa(): String {
        val currentTimeMillis = System.currentTimeMillis()
        val currentTimeHexNOW = toHexString(currentTimeMillis / 1000)
        println("currentTimeHexNOW $currentTimeHexNOW ")

        return currentTimeHexNOW

    }

    interface CallbackLogger {
        fun onSuccess(result: Boolean): Boolean
        fun onError(error: String)
        fun getTime(data: MutableList<String>?)
        fun getEvent(data: MutableList<String>?)
        fun onProgress(progress: String): String
    }
}