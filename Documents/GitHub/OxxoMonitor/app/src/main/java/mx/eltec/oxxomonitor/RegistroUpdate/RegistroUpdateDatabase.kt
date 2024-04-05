package mx.eltec.oxxomonitor.RegistroUpdate

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@TypeConverters(DateTypeConverter::class) // Registra el TypeConverter aquí

@Database(entities = [RegistroUpdateBle::class], version = 3, exportSchema = false)

abstract class RegistroUpdateDatabase : RoomDatabase() {
    abstract val RegistroUpdateBleDao: RegistroUpdateBleDao// NotaDao

    companion object {
        @Volatile
        private var INSTANCE: RegistroUpdateDatabase? = null

        fun getInstance(context: Context): RegistroUpdateDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        RegistroUpdateDatabase::class.java,
                        "RegistroUpdate_database"
                    ).build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}