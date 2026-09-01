package com.archimede.w2full.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VehicleEntity::class, RifornimentoEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class W2FullDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao

    abstract fun rifornimentoDao(): RifornimentoDao

    companion object {
        private const val DATABASE_NAME = "w2full.db"

        @Volatile
        private var instance: W2FullDatabase? = null

        fun getInstance(context: Context): W2FullDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    W2FullDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }
    }
}
