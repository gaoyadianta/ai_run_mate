package com.runmate.ai.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.runmate.ai.data.dao.GpsPointDao
import com.runmate.ai.data.dao.RunningSessionDao
import com.runmate.ai.data.model.GpsPoint
import com.runmate.ai.data.model.RunningSession

/**
 * RunMate应用数据库
 */
@Database(
    entities = [RunningSession::class, GpsPoint::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RunMateDatabase : RoomDatabase() {
    
    abstract fun runningSessionDao(): RunningSessionDao
    abstract fun gpsPointDao(): GpsPointDao
    
    companion object {
        @Volatile
        private var INSTANCE: RunMateDatabase? = null
        
        fun getDatabase(context: Context): RunMateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RunMateDatabase::class.java,
                    "runmate_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}