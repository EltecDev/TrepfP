package mx.eltec.oxxomonitor.RegistroUpdate

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "RegistroUpdate_BD")
class RegistroUpdateBle {
    @PrimaryKey
    var id: String = UUID.randomUUID().toString()


    @ColumnInfo(name = "updated_table")
    var updated_table: String? = "updated_table"
    @ColumnInfo(name = "fecha")
    var fecha: Date? = Date()


    // Constructor para pasar una tabla específica
    constructor(updatedTable: String) {
        this.updated_table = updatedTable
    }
    constructor()

}