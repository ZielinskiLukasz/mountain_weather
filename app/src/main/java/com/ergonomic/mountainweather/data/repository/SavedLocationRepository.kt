package com.ergonomic.mountainweather.data.repository

import com.ergonomic.mountainweather.data.local.SavedLocationDao
import com.ergonomic.mountainweather.data.local.SavedLocationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavedLocationRepository(private val dao: SavedLocationDao) {

    fun observeFavorites(): Flow<List<SavedLocationEntity>> = dao.observeFavorites()

    fun observeRecent(limit: Int = 5): Flow<List<SavedLocationEntity>> = dao.observeRecent(limit)

    suspend fun saveAsRecent(
        name: String,
        latitude: Double,
        longitude: Double,
        country: String? = null,
        region: String? = null
    ) {
        val existing = dao.findByCoordinates(latitude, longitude)
        if (existing != null) {
            dao.updateLastUsed(existing.id)
        } else {
            dao.insert(
                SavedLocationEntity(
                    name = name,
                    latitude = latitude,
                    longitude = longitude,
                    country = country,
                    region = region
                )
            )
        }
    }

    suspend fun toggleFavorite(id: Long) = dao.toggleFavorite(id)

    suspend fun delete(id: Long) = dao.delete(id)

    fun observeFavoriteByCoordinates(lat: Double, lon: Double): Flow<Boolean> =
        dao.observeIsFavorite(lat, lon).map { it == true }
}
