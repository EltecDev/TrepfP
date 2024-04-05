package mx.eltec.oxxomonitor.BleRoom

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "Ble_BD")
class DeviceBle {
    @PrimaryKey
    var id: String = UUID.randomUUID().toString()


    @ColumnInfo(name = "vigente")
    var vigente: String? = "vigente"
    @ColumnInfo(name = "n_ble")
    var n_ble: String? = "n_ble"


}