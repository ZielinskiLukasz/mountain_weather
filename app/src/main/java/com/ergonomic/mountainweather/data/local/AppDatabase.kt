package com.ergonomic.mountainweather.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WeatherEntity::class,
        SavedLocationEntity::class,
        HourlyForecastEntity::class,
        DailyForecastEntity::class
    ],
    version = 6,
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

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = listOf(
                    "cloudCover INTEGER",
                    "windGusts REAL",
                    "snowfall REAL",
                    "rain REAL",
                    "sunrise TEXT",
                    "sunset TEXT",
                    "uvIndexMax REAL",
                    "rainSum REAL",
                    "showersSum REAL",
                    "snowfallSum REAL",
                    "precipitationHours REAL",
                    "precipitationProbabilityMax INTEGER",
                    "sunshineDuration REAL",
                    "windGustsMax REAL",
                    "dominantWindDirection INTEGER",
                    "dewPoint REAL",
                    "visibility REAL",
                    "freezingLevelHeight REAL",
                    "temperatureMax REAL",
                    "temperatureMin REAL"
                )
                columns.forEach { col ->
                    try { db.execSQL("ALTER TABLE weather_cache ADD COLUMN $col") }
                    catch (_: Exception) { }
                }
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = listOf(
                    "aqiEu INTEGER",
                    "aqiUs INTEGER",
                    "pm25 REAL",
                    "pm10 REAL",
                    "ozone REAL"
                )
                columns.forEach { col ->
                    try { db.execSQL("ALTER TABLE weather_cache ADD COLUMN $col") }
                    catch (_: Exception) { }
                }
            }
        }

        private val MIGRATION_1_6 = object : Migration(1, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addAllWeatherColumns(db)
                createTablesIfMissing(db)
            }
        }
        private val MIGRATION_2_6 = object : Migration(2, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addAllWeatherColumns(db)
                createTablesIfMissing(db)
            }
        }
        private val MIGRATION_3_6 = object : Migration(3, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addAllWeatherColumns(db)
                createTablesIfMissing(db)
            }
        }
        private val MIGRATION_4_6 = object : Migration(4, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addAllWeatherColumns(db)
            }
        }

        private fun addAllWeatherColumns(db: SupportSQLiteDatabase) {
            val columns = listOf(
                "cloudCover INTEGER", "windGusts REAL", "snowfall REAL", "rain REAL",
                "sunrise TEXT", "sunset TEXT", "uvIndexMax REAL", "rainSum REAL",
                "showersSum REAL", "snowfallSum REAL", "precipitationHours REAL",
                "precipitationProbabilityMax INTEGER", "sunshineDuration REAL",
                "windGustsMax REAL", "dominantWindDirection INTEGER", "dewPoint REAL",
                "visibility REAL", "freezingLevelHeight REAL",
                "temperatureMax REAL", "temperatureMin REAL",
                "aqiEu INTEGER", "aqiUs INTEGER", "pm25 REAL", "pm10 REAL", "ozone REAL"
            )
            columns.forEach { col ->
                try { db.execSQL("ALTER TABLE weather_cache ADD COLUMN $col") }
                catch (_: Exception) { }
            }
        }

        private fun createTablesIfMissing(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS saved_locations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    country TEXT,
                    region TEXT,
                    isFavorite INTEGER NOT NULL DEFAULT 0,
                    lastUsedAt INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_saved_locations_latitude_longitude ON saved_locations (latitude, longitude)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS hourly_forecast (
                    locationKey TEXT NOT NULL,
                    time TEXT NOT NULL,
                    temperature REAL NOT NULL,
                    weatherCode INTEGER NOT NULL,
                    precipitation REAL NOT NULL,
                    cachedAt INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(locationKey, time)
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS daily_forecast (
                    locationKey TEXT NOT NULL,
                    date TEXT NOT NULL,
                    weatherCode INTEGER NOT NULL,
                    temperatureMax REAL NOT NULL,
                    temperatureMin REAL NOT NULL,
                    precipitationSum REAL NOT NULL,
                    windSpeedMax REAL NOT NULL,
                    cachedAt INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(locationKey, date)
                )
            """.trimIndent())
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mountain_weather.db"
                )
                    .addMigrations(
                        MIGRATION_1_6, MIGRATION_2_6, MIGRATION_3_6,
                        MIGRATION_4_6, MIGRATION_4_5, MIGRATION_5_6
                    )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
