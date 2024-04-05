package mx.eltec.oxxomonitor.packageInterface

interface TrepfConnRequester {
        fun requestTrepfConn(mac: String, name: String)
        fun Desconectar()
        fun checkEnableWIfiBlue(): Boolean
        fun isConnectedtoNetwork(): Boolean
        fun checkEnableBluetooth(): Boolean
        fun checkEnableWIfi() : Boolean
        fun checkAndRequestPermissions() : Boolean

        fun LimpiarLayout()


    }