package mx.eltec.oxxomonitor.RegistroUpdate

import android.content.Context


class RegistroUpdateLab private constructor(context: Context) {

    private val mregistroUpdateDao: RegistroUpdateBleDao // Dao
    private val   registroUpdateDatabase : RegistroUpdateDatabase = RegistroUpdateDatabase.getInstance(context)

    init {
        mregistroUpdateDao = registroUpdateDatabase.RegistroUpdateBleDao
    }

    val registroUpdateBle: List<RegistroUpdateBle?>?
        get() = mregistroUpdateDao!!.RegistroUpdate


    fun getmgetDeviceById(ble: String?): RegistroUpdateBle? {
        return mregistroUpdateDao.getRegistroUpdateById(ble)
    }

    fun addRegistroUpdate(device: RegistroUpdateBle?){
        mregistroUpdateDao!!.addRegistroUpdate(device)
    }
    fun truncate(){
        mregistroUpdateDao.deleteAllRegistroUpdate()
    }

    fun deleteByDevice(template: RegistroUpdateBle?) {
        mregistroUpdateDao!!.deleteRegistroUpdate(template)
    }

    companion object {
        private var sRegistroUpdateLab:   RegistroUpdateLab? = null
        operator fun get(context: Context): RegistroUpdateLab? {
            if (sRegistroUpdateLab == null) {
                sRegistroUpdateLab = RegistroUpdateLab(context)

            }
            return sRegistroUpdateLab
        }
    }
}