package mx.eltec.oxxomonitor.BleRoom

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update


@Dao
interface DeviceBleDao {

    @get:Query("SELECT * FROM Ble_BD") // Cambio del nombre de la tabla
    val DeviceBle: MutableList<DeviceBle?>?

    @Query("SELECT * FROM Ble_BD WHERE id LIKE :uuid")
    fun getDeviceByid(uuid: String?): DeviceBle?

    @Query("SELECT * FROM Ble_BD WHERE n_ble LIKE :Ble")
    fun getDeviceByBle(Ble: String?): DeviceBle?


    @Query("SELECT * FROM Ble_BD")
    fun getAllDataDevice(): MutableList<DeviceBle>

    @Insert
    fun addBlePrincipal(ble: DeviceBle?)

    @Delete
    fun deleteBle(ble: DeviceBle?)

    @Query("DELETE FROM Ble_BD")
    fun deleteAllBle()
    @Update
    fun updateBle(ble: DeviceBle?)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateDatable(dataList: MutableList<DeviceBle>)

}