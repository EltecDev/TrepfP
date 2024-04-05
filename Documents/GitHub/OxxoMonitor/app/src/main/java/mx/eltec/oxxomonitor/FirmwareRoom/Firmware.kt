package mx.eltec.oxxomonitor.FirmwareRoom

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "Firmware_BD")
class Firmware {
    @PrimaryKey
    var id: String = UUID.randomUUID().toString()


    @ColumnInfo(name = "version")
    var version: String? = "version"
    @ColumnInfo(name = "Valor")
    var Valor: String? = "Valor"
    @ColumnInfo(name = "clave_modelo")
    var clave_modelo: String? = "clave_modelo"




}