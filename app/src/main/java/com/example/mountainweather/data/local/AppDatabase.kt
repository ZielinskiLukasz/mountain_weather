package com.example.mountainweather.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WeatherEntity::class,
        SavedLocationEntity::class,
        HourlyForecastEntity::class,
        DailyForecastEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun weatherDao(): WeatherDao
    abstract fun savedLocationDao(): SavedLocationDao
    abstract fun hourlyForecastDao(): HourlyForecastDao
    abstract fun dailyForecastDao(): DailyForecastDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mountain_weather.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
