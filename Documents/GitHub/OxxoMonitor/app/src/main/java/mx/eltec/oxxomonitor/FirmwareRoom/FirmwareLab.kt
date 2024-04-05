package mx.eltec.oxxomonitor.FirmwareRoom

import android.content.Context


class FirmwareLab private constructor(context: Context) {

    private val mfirmwareDao: FirmwareDao// Dao
    private val firmwareDatabase: FirmwareDatabase = FirmwareDatabase.getInstance(context)

    init {
        mfirmwareDao = firmwareDatabase.firmwareDao
    }

    val firmware: List<Firmware?>?
        get() = mfirmwareDao!!.firmware// templetePrincipal

    fun firmwareByVers(id: String): MutableList<Firmware?>? {
        return mfirmwareDao.firmwareByVersion(id)
    }
    fun firmwareByclave_modelo(id: String): MutableList<Firmware?>? {
        return mfirmwareDao.firmwareByclave_modelo(id)
    }

    fun getmfirmwareDao(id: String?): Firmware? {
        return mfirmwareDao.getfirmwareTRPF(id)
    }





    fun addTemplete(template: Firmware?){
        mfirmwareDao!!.addfirmwarePrincipal(template)
    }
    fun truncate(){
        mfirmwareDao.deleteAllfirmware()
    }

    fun deleteTemplete(template: Firmware?) {
        mfirmwareDao!!.deletefirmwarePrincipal(template)
    }

    companion object {
        private var sFirmwareLab: FirmwareLab? = null
        operator fun get(context: Context): FirmwareLab? {
            if (sFirmwareLab == null) {
                sFirmwareLab = FirmwareLab(context)

            }
            return sFirmwareLab
        }
    }
}