package com.ergonomic.mountainweather.data.repository

import com.ergonomic.mountainweather.data.local.SavedLocationDao
import com.ergonomic.mountainweather.data.local.SavedLocationEntity
import com.ergonomic.mountainweather.util.findMatching
import com.ergonomic.mountainweather.util.isSamePlace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class SavedLocationRepository(private val dao: SavedLocationDao) {

    fun observeFavorites(): Flow<List<SavedLocationEntity>> = dao.observeFavorites()

    fun observeRecent(limit: Int = 10): Flow<List<SavedLocationEntity>> =
        combine(dao.observeFavorites(), dao.observeRecent(limit)) { favorites, recent ->
            recent.filter { row ->
                favorites.none { fav ->
                    isSamePlace(
                        row.latitude, row.longitude, row.name,
                        fav.latitude, fav.longitude, fav.name,
                        row.country, fav.country
                    )
                }
            }
        }

    suspend fun clearAllRecent() = dao.deleteAllRecent()

    suspend fun saveAsRecent(
        name: String,
        latitude: Double,
        longitude: Double,
        country: String? = null,
        region: String? = null
    ) {
        val existing = findExisting(latitude, longitude, name, country)
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

    suspend fun findExisting(
        latitude: Double,
        longitude: Double,
        name: String,
        country: String? = null
    ): SavedLocationEntity? {
        dao.findByCoordinates(latitude, longitude)?.let { return it }
        return dao.getAll().findMatching(latitude, longitude, name, country)
    }

    suspend fun toggleFavorite(id: Long) = dao.toggleFavorite(id)

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun reorderFavorites(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            dao.updateSortOrder(id, index)
        }
    }

    fun observeFavoriteByCoordinates(lat: Double, lon: Double): Flow<Boolean> =
        dao.observeIsFavorite(lat, lon).map { it == true }
}
