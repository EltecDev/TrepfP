package mx.eltec.oxxomonitor.FirmwareRoom

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [Firmware::class], version = 3, exportSchema = false)

abstract class FirmwareDatabase : RoomDatabase() {
    abstract val firmwareDao: FirmwareDao// NotaDao

    companion object {
        @Volatile
        private var INSTANCE: FirmwareDatabase? = null

        fun getInstance(context: Context): FirmwareDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        FirmwareDatabase::class.java,
                        "Firmware_database"
                    ).build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}