package mx.eltec.oxxomonitor.RegistroUpdate

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update


@Dao
interface RegistroUpdateBleDao {

    @get:Query("SELECT * FROM RegistroUpdate_BD") // Cambio del nombre de la tabla
    val RegistroUpdate: MutableList<RegistroUpdateBle?>?

    @Query("SELECT * FROM RegistroUpdate_BD WHERE id LIKE :uuid")
    fun getRegistroUpdateById(uuid: String?): RegistroUpdateBle?
/*
    @Query("SELECT * FROM Ble_BD WHERE n_ble LIKE :Ble")
    fun getDeviceByBle(Ble: String?): ClienteBle?
*/

    @Query("SELECT * FROM RegistroUpdate_BD")
    fun getAllRegistroUpdate(): MutableList<RegistroUpdateBle>

    @Insert
    fun addRegistroUpdate(ble: RegistroUpdateBle?)

    @Delete
    fun deleteRegistroUpdate(ble: RegistroUpdateBle?)

    @Query("DELETE FROM RegistroUpdate_BD")
    fun deleteAllRegistroUpdate()
    @Update
    fun updateRegistroUpdate(ble: RegistroUpdateBle?)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateDatable(dataList: MutableList<RegistroUpdateBle>)

}