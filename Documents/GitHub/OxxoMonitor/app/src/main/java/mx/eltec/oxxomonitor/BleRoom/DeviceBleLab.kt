package mx.eltec.oxxomonitor.BleRoom

import android.content.Context


class DeviceBleLab private constructor(context: Context) {

    private val mDeviceDao: DeviceBleDao// Dao
    private val bleDatabase: BleDatabase = BleDatabase.getInstance(context)

    init {
        mDeviceDao = bleDatabase.deviceDao
    }

    val device: List<DeviceBle?>?
        get() = mDeviceDao!!.DeviceBle// templetePrincipal


    fun getmgetDeviceByBle(ble: String?): DeviceBle? {
        return mDeviceDao.getDeviceByBle(ble)
    }





    fun addDevice(device: DeviceBle?){
        mDeviceDao!!.addBlePrincipal(device)
    }
    fun truncate(){
        mDeviceDao.deleteAllBle()
    }

    fun deleteByDevice(template: DeviceBle?) {
        mDeviceDao!!.deleteBle (template)
    }

    companion object {
        private var sDeviceLab: DeviceBleLab? = null
        operator fun get(context: Context): DeviceBleLab? {
            if (sDeviceLab == null) {
                sDeviceLab = DeviceBleLab(context)

            }
            return sDeviceLab
        }
    }
}