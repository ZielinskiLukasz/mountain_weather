package com.ergonomic.mountainweather.util

import com.ergonomic.mountainweather.data.local.SavedLocationEntity
import kotlin.math.abs
import kotlin.math.round

/** ~500 m — same pin as Room `findByCoordinates`. */
private const val SAME_POINT_DEG = 0.005

/** ~5 km — GPS / city-center vs a saved favourite of the same name. */
private const val SAME_PLACE_DEG = 0.05

fun isSamePoint(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Boolean =
    abs(lat1 - lat2) < SAME_POINT_DEG && abs(lon1 - lon2) < SAME_POINT_DEG

/**
 * True when two pins are the same place: identical point, same 0.01° weather bucket,
 * or same name (and country, if both set) within a few kilometres.
 */
fun isSamePlace(
    lat1: Double,
    lon1: Double,
    name1: String,
    lat2: Double,
    lon2: Double,
    name2: String,
    country1: String? = null,
    country2: String? = null
): Boolean {
    if (isSamePoint(lat1, lon1, lat2, lon2)) return true
    if (sameBucket(lat1, lat2) && sameBucket(lon1, lon2)) return true
    if (!name1.trim().equals(name2.trim(), ignoreCase = true)) return false
    if (!country1.isNullOrBlank() && !country2.isNullOrBlank() &&
        !country1.equals(country2, ignoreCase = true)
    ) {
        return false
    }
    return abs(lat1 - lat2) < SAME_PLACE_DEG && abs(lon1 - lon2) < SAME_PLACE_DEG
}

fun List<SavedLocationEntity>.findMatching(
    latitude: Double,
    longitude: Double,
    name: String,
    country: String? = null
): SavedLocationEntity? = firstOrNull { saved ->
    isSamePlace(
        latitude, longitude, name,
        saved.latitude, saved.longitude, saved.name,
        country, saved.country
    )
}

private fun sameBucket(a: Double, b: Double): Boolean =
    round(a * 100.0) == round(b * 100.0)
