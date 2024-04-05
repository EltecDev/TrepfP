package mx.eltec.oxxomonitor.adapters

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import com.example.trepfp.ConexionTrefp
import mx.eltec.Utility.CustomProgressDialog

class PlantillaImplement (
    private val context: Context,
    private val tvconstate: TextView,
    private val tvfw: TextView
) : ConexionTrefp.MyCallback {
    private lateinit var trepfConnRequester: TrepfConnRequester
    private val customProgressDialog by lazy { CustomProgressDialog(context) }
    val handler = Handler(Looper.getMainLooper())
    var typeExecute = ""

    private val conexionTrefp: ConexionTrefp = ConexionTrefp.newInstance(context, tvconstate, tvfw)
    var returnInfo = mutableListOf<String>()
    fun executePlantilla(finalCommand: String) {
        customProgressDialog.show()
        typeExecute = "MyAsyncTaskSendNewPlantilla"
        conexionTrefp.MyAsyncTaskSendNewPlantilla(finalCommand, this)//.execute()
    }


    fun executeStatus() {
       // customProgressDialog.show()
        typeExecute = "MyAsyncTaskStatusRealTime"
        conexionTrefp.MyAsyncTaskStatusRealTimeVersion2( this).execute()

    }
    override fun getInfo(data: MutableList<String>?) {
        if (data != null) {
            Log.d("PlantillaImplement","data $data")
            returnInfo = data

        }
    }

    override fun onError(error: String) {
   //     callback.onError(error)
    }

    override fun onProgress(progress: String): String {
    //    callback.onProgress(progress)
        handler.post {
            if (!typeExecute.equals("MyAsyncTaskStatusRealTime"))
            { customProgressDialog.updateText("$progress")}
        }
        return progress
    }

    override fun onSuccess(result: Boolean): Boolean {

        Log.d("PlantillaImplement","result $result")

        handler.post {
            customProgressDialog.dismiss()
            if (typeExecute.equals("MyAsyncTaskSendNewPlantilla"))
            {
                conexionTrefp.makeToast(if (result) {
                    "Se actualizo la plantilla"

                }else "No se pudo actualizar la plantilla" )
                if (result )conexionTrefp.desconectar()
            }
        }
        return result
    }
    interface TrepfConnRequester {
        fun requestTrepfConn(mac: String, name: String)
        fun Desconectar()
        fun checkEnableWIfiBlue(): Boolean
        fun isConnectedtoNetwork(): Boolean
        fun checkEnableBluetooth(): Boolean
        fun checkEnableWIfi() : Boolean
        fun checkAndRequestPermissions() : Boolean
        fun LimpiarLayout()
        fun islocalEnable() : Boolean
    }
}