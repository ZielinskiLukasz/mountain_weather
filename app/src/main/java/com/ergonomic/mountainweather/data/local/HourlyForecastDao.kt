package com.ergonomic.mountainweather.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HourlyForecastDao {

    @Query("SELECT * FROM hourly_forecast WHERE locationKey = :key ORDER BY time ASC")
    fun observe(key: String): Flow<List<HourlyForecastEntity>>

    @Query("SELECT * FROM hourly_forecast WHERE locationKey = :key ORDER BY time ASC")
    suspend fun getAll(key: String): List<HourlyForecastEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HourlyForecastEntity>)

    @Query("DELETE FROM hourly_forecast WHERE locationKey = :key")
    suspend fun deleteForLocation(key: String)

    @Transaction
    suspend fun replaceForLocation(key: String, items: List<HourlyForecastEntity>) {
        deleteForLocation(key)
        insertAll(items)
    }
}
