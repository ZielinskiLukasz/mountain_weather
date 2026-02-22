package com.example.mountainweather.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyForecastDao {

    @Query("SELECT * FROM daily_forecast WHERE locationKey = :key ORDER BY date ASC")
    fun observe(key: String): Flow<List<DailyForecastEntity>>

    @Query("SELECT * FROM daily_forecast WHERE locationKey = :key ORDER BY date ASC")
    suspend fun getAll(key: String): List<DailyForecastEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DailyForecastEntity>)

    @Query("DELETE FROM daily_forecast WHERE locationKey = :key")
    suspend fun deleteForLocation(key: String)

    @Transaction
    suspend fun replaceForLocation(key: String, items: List<DailyForecastEntity>) {
        deleteForLocation(key)
        insertAll(items)
    }
}
