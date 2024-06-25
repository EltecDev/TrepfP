import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

@Entity(tableName = "Device_BD")
data class Device_info(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "Device")
    var nombre: String? = "",
    @ColumnInfo(name = "Handshake")
    var Handshake: MutableList<String>? = arrayListOf(),
    @ColumnInfo(name = "modelo")
    var modelo_device: String? = "",
    @ColumnInfo(name = "firmware_device")
    var firmware_device: String? = ""
)

class Converters {
    @TypeConverter
    fun fromString(value: String?): MutableList<String>? {
        val listType = object : TypeToken<MutableList<String>>() {}.type
        return if (value == null) null else Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromMutableList(list: MutableList<String>?): String? {
        return Gson().toJson(list)
    }
}

@Database(entities = [Device_info::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceInfoDao(): DeviceInfoDao
}

@Dao
interface DeviceInfoDao {
    @Query("SELECT * FROM Device_BD")
    fun getAll(): List<Device_info>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(deviceInfo: Device_info)

    @Update
    fun update(deviceInfo: Device_info)

    @Delete
    fun delete(deviceInfo: Device_info)
}
