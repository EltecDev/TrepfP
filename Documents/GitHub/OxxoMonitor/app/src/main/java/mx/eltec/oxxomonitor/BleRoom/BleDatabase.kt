package mx.eltec.oxxomonitor.BleRoom

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [DeviceBle::class], version = 3, exportSchema = false)

abstract class BleDatabase : RoomDatabase() {
    abstract val deviceDao: DeviceBleDao// NotaDao

    companion object {
        @Volatile
        private var INSTANCE: BleDatabase? = null

        fun getInstance(context: Context): BleDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        BleDatabase::class.java,
                        "Device_database"
                    ).build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}