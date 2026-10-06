package com.aiguidecamera.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** 앱 Room 데이터베이스. 프로세스당 하나만 만든다. */
@Database(entities = [PhotoRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun photoRecordDao(): PhotoRecordDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_guide_camera.db",
                ).build().also { instance = it }
            }
    }
}
