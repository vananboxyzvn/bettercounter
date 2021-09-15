package com.luteapp.bettercounter.boilerplate

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.luteapp.bettercounter.persistence.Entry
import com.luteapp.bettercounter.persistence.EntryDao

@Database(entities = [Entry::class], version = 3)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context) : AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java, "appdb"
                    ).build().also { INSTANCE = it }
            }
    }
}
