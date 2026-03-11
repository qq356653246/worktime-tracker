package com.example.worktime

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WorkRecord::class], version = 1, exportSchema = false)
abstract class WorkTimeDatabase : RoomDatabase() {
    
    abstract fun workTimeDao(): WorkTimeDao
    
    companion object {
        @Volatile
        private var INSTANCE: WorkTimeDatabase? = null
        
        fun getDatabase(context: Context): WorkTimeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkTimeDatabase::class.java,
                    "worktime_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
