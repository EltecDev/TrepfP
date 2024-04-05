package mx.eltec.oxxomonitor.FirmwareRoom

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update


@Dao
interface FirmwareDao {

    @get:Query("SELECT * FROM Firmware_BD") // Cambio del nombre de la tabla
    val firmware: MutableList<Firmware?>?


    @Query("SELECT * FROM Firmware_BD  WHERE version LIKE :version") // Cambio del nombre de la tabla
    fun firmwareByVersion(version : String): MutableList<Firmware?>?

    @Query("SELECT * FROM Firmware_BD  WHERE clave_modelo LIKE :version") // Cambio del nombre de la tabla
    fun firmwareByclave_modelo(version : String): MutableList<Firmware?>?

    @Query("SELECT * FROM Firmware_BD WHERE id LIKE :uuid")
    fun getfirmwarePrincipal(uuid: String?): Firmware?

    @Query("SELECT * FROM firmware_bd WHERE version LIKE :uuid")
    fun getfirmwareTRPF(uuid: String?): Firmware?

    @Query("SELECT * FROM firmware_bd WHERE Valor = :valorABuscar")
    fun getfirmwareByValor(valorABuscar: String): MutableList<Firmware>

    @Query("SELECT * FROM Firmware_BD")
    fun getAllDatafirmware(): MutableList<Firmware>
    @Insert
    fun addfirmwarePrincipal(firmware: Firmware?)

    @Delete
    fun deletefirmwarePrincipal(firmware: Firmware?)


    @Query("DELETE FROM Firmware_BD")
    fun deleteAllfirmware()
    @Update
    fun updatefirmware(template: Firmware?)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateDatafirmware(dataList: MutableList<Firmware>)

}