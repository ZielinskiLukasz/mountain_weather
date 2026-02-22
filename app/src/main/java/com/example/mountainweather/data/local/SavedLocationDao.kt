package com.example.mountainweather.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLocationDao {

    @Query("SELECT * FROM saved_locations WHERE isFavorite = 1 ORDER BY name ASC LIMIT :limit")
    fun observeFavorites(limit: Int = 10): Flow<List<SavedLocationEntity>>

    @Query("SELECT * FROM saved_locations WHERE isFavorite = 1 ORDER BY name ASC")
    suspend fun getFavorites(): List<SavedLocationEntity>

    @Query(
        "SELECT * FROM saved_locations WHERE isFavorite = 0 ORDER BY lastUsedAt DESC LIMIT :limit"
    )
    fun observeRecent(limit: Int = 10): Flow<List<SavedLocationEntity>>

    @Query(
        "SELECT * FROM saved_locations " +
                "WHERE ABS(latitude - :lat) < 0.005 AND ABS(longitude - :lon) < 0.005 LIMIT 1"
    )
    suspend fun findByCoordinates(lat: Double, lon: Double): SavedLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: SavedLocationEntity): Long

    @Query("UPDATE saved_locations SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE saved_locations SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        "SELECT isFavorite FROM saved_locations " +
                "WHERE ABS(latitude - :lat) < 0.005 AND ABS(longitude - :lon) < 0.005 LIMIT 1"
    )
    fun observeIsFavorite(lat: Double, lon: Double): Flow<Boolean?>
}
