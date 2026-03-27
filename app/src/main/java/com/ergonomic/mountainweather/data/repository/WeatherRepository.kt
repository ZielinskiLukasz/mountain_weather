package com.ergonomic.mountainweather.data.repository

import com.ergonomic.mountainweather.data.AirQualityApi
import com.ergonomic.mountainweather.data.OpenMeteoApi
import com.ergonomic.mountainweather.data.local.DailyForecastDao
import com.ergonomic.mountainweather.data.local.DailyForecastEntity
import com.ergonomic.mountainweather.data.local.HourlyForecastDao
import com.ergonomic.mountainweather.data.local.HourlyForecastEntity
import com.ergonomic.mountainweather.data.local.WeatherDao
import com.ergonomic.mountainweather.data.local.WeatherEntity
import com.ergonomic.mountainweather.util.WeatherParams
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.Locale

class WeatherRepository(
    private val api: OpenMeteoApi,
    private val dao: WeatherDao,
    private val hourlyDao: HourlyForecastDao,
    private val dailyDao: DailyForecastDao,
    private val airQualityApi: AirQualityApi = AirQualityApi.create()
) {
    fun observeCachedWeather(locationKey: String): Flow<WeatherEntity?> =
        dao.observeWeather(locationKey)

    fun observeHourlyForecast(locationKey: String): Flow<List<HourlyForecastEntity>> =
        hourlyDao.observe(locationKey)

    fun observeDailyForecast(locationKey: String): Flow<List<DailyForecastEntity>> =
        dailyDao.observe(locationKey)

    suspend fun refreshWeather(
        latitude: Double,
        longitude: Double,
        locationName: String
    ): Result<WeatherEntity> {
        val key = locationKey(latitude, longitude)
        return try {
            val response = api.getCurrentWeather(latitude, longitude)
            val entity = WeatherEntity(
                locationKey = key,
                locationName = locationName,
                latitude = latitude,
                longitude = longitude,
                temperature = response.current.temperature,
                apparentTemperature = response.current.apparentTemperature,
                weatherCode = response.current.weatherCode,
                windSpeed = response.current.windSpeed,
                windDirection = response.current.windDirection,
                humidity = response.current.humidity,
                precipitation = response.current.precipitation,
                pressure = response.current.pressure,
                time = response.current.time,
                cachedAt = System.currentTimeMillis()
            )
            dao.insertWeather(entity)
            Result.success(entity)
        } catch (e: Exception) {
            val cached = dao.getWeather(key)
            if (cached != null) {
                Result.failure(CachedDataException(e, cached))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun refreshEnrichedWeather(
        latitude: Double,
        longitude: Double,
        locationName: String,
        enabledParams: Set<String>
    ): Result<WeatherEntity> {
        val key = locationKey(latitude, longitude)
        return try {
            val currentFields = buildCurrentQuery(enabledParams)
            val dailyFields = buildDailyQuery(enabledParams)
            val hourlyFields = buildHourlyQuery(enabledParams)

            val response = api.getEnrichedWeather(
                latitude = latitude,
                longitude = longitude,
                current = currentFields,
                daily = dailyFields,
                hourly = hourlyFields,
                forecastDays = if (dailyFields != null) 1 else null,
                forecastHours = if (hourlyFields != null) 24 else null
            )

            val todayDaily = response.daily
            val currentHourIndex = response.hourly?.let { hourly ->
                val targetHour = response.current.time.take(13)
                hourly.time.indexOfFirst { it.startsWith(targetHour) }.takeIf { it >= 0 }
            }

            val aqData = if (enabledParams.any { it in WeatherParams.AIR_QUALITY_KEYS }) {
                try {
                    val aqQuery = buildAirQualityQuery(enabledParams)
                    if (aqQuery != null) airQualityApi.getCurrent(latitude, longitude, aqQuery).current
                    else null
                } catch (_: Exception) { null }
            } else null

            val entity = WeatherEntity(
                locationKey = key,
                locationName = locationName,
                latitude = latitude,
                longitude = longitude,
                temperature = response.current.temperature,
                apparentTemperature = response.current.apparentTemperature,
                weatherCode = response.current.weatherCode,
                windSpeed = response.current.windSpeed,
                windDirection = response.current.windDirection,
                humidity = response.current.humidity,
                precipitation = response.current.precipitation,
                pressure = response.current.pressure,
                time = response.current.time,
                cachedAt = System.currentTimeMillis(),
                cloudCover = response.current.cloudCover,
                windGusts = response.current.windGusts,
                snowfall = response.current.snowfall,
                rain = response.current.rain,
                temperatureMax = todayDaily?.temperatureMax?.firstOrNull(),
                temperatureMin = todayDaily?.temperatureMin?.firstOrNull(),
                sunrise = todayDaily?.sunrise?.firstOrNull(),
                sunset = todayDaily?.sunset?.firstOrNull(),
                uvIndexMax = todayDaily?.uvIndexMax?.firstOrNull(),
                rainSum = todayDaily?.rainSum?.firstOrNull(),
                showersSum = todayDaily?.showersSum?.firstOrNull(),
                snowfallSum = todayDaily?.snowfallSum?.firstOrNull(),
                precipitationHours = todayDaily?.precipitationHours?.firstOrNull(),
                precipitationProbabilityMax = todayDaily?.precipitationProbabilityMax?.firstOrNull(),
                sunshineDuration = todayDaily?.sunshineDuration?.firstOrNull(),
                windGustsMax = todayDaily?.windGustsMax?.firstOrNull(),
                dominantWindDirection = todayDaily?.windDirectionDominant?.firstOrNull(),
                dewPoint = currentHourIndex?.let { response.hourly?.dewPoint?.getOrNull(it) },
                visibility = currentHourIndex?.let { response.hourly?.visibility?.getOrNull(it) },
                freezingLevelHeight = currentHourIndex?.let { response.hourly?.freezingLevelHeight?.getOrNull(it) },
                aqiEu = aqData?.europeanAqi,
                aqiUs = aqData?.usAqi,
                pm25 = aqData?.pm25,
                pm10 = aqData?.pm10,
                ozone = aqData?.ozone
            )
            dao.insertWeather(entity)
            Result.success(entity)
        } catch (e: Exception) {
            val cached = dao.getWeather(key)
            if (cached != null) {
                Result.failure(CachedDataException(e, cached))
            } else {
                Result.failure(e)
            }
        }
    }

    private fun buildCurrentQuery(enabledParams: Set<String>): String {
        val fields = mutableListOf(
            "temperature_2m", "apparent_temperature", "weather_code",
            "wind_speed_10m", "wind_direction_10m", "relative_humidity_2m",
            "precipitation", "pressure_msl", "is_day"
        )
        if (WeatherParams.CLOUD_COVER in enabledParams) fields.add("cloud_cover")
        if (WeatherParams.WIND_GUSTS in enabledParams) fields.add("wind_gusts_10m")
        if (WeatherParams.SNOWFALL in enabledParams) fields.add("snowfall")
        if (WeatherParams.RAIN in enabledParams) fields.add("rain")
        return fields.joinToString(",")
    }

    private fun buildDailyQuery(enabledParams: Set<String>): String? {
        val fields = mutableListOf<String>()
        if (WeatherParams.TEMPERATURE in enabledParams) {
            fields.addAll(listOf("temperature_2m_max", "temperature_2m_min"))
        }
        if (WeatherParams.SUNRISE_SUNSET in enabledParams) fields.addAll(listOf("sunrise", "sunset"))
        if (WeatherParams.UV_INDEX in enabledParams) fields.add("uv_index_max")
        if (WeatherParams.RAIN_SUM in enabledParams) fields.add("rain_sum")
        if (WeatherParams.SHOWERS_SUM in enabledParams) fields.add("showers_sum")
        if (WeatherParams.SNOWFALL_SUM in enabledParams) fields.add("snowfall_sum")
        if (WeatherParams.PRECIP_HOURS in enabledParams) fields.add("precipitation_hours")
        if (WeatherParams.PRECIP_PROBABILITY in enabledParams) fields.add("precipitation_probability_max")
        if (WeatherParams.SUNSHINE_DURATION in enabledParams) fields.add("sunshine_duration")
        if (WeatherParams.WIND_GUSTS_MAX in enabledParams) fields.add("wind_gusts_10m_max")
        if (WeatherParams.DOMINANT_WIND_DIR in enabledParams) fields.add("wind_direction_10m_dominant")
        return if (fields.isEmpty()) null else fields.joinToString(",")
    }

    private fun buildHourlyQuery(enabledParams: Set<String>): String? {
        val fields = mutableListOf<String>()
        if (WeatherParams.DEW_POINT in enabledParams) fields.add("dew_point_2m")
        if (WeatherParams.VISIBILITY in enabledParams) fields.add("visibility")
        if (WeatherParams.FREEZING_LEVEL in enabledParams) fields.add("freezing_level_height")
        return if (fields.isEmpty()) null else fields.joinToString(",")
    }

    private fun buildAirQualityQuery(enabledParams: Set<String>): String? {
        val fields = mutableListOf<String>()
        if (WeatherParams.AQI_EU in enabledParams) fields.add("european_aqi")
        if (WeatherParams.AQI_US in enabledParams) fields.add("us_aqi")
        if (WeatherParams.PM25 in enabledParams) fields.add("pm2_5")
        if (WeatherParams.PM10 in enabledParams) fields.add("pm10")
        if (WeatherParams.OZONE in enabledParams) fields.add("ozone")
        return if (fields.isEmpty()) null else fields.joinToString(",")
    }

    suspend fun refreshHourlyForecast(
        latitude: Double,
        longitude: Double,
        forecastDays: Int = 1
    ): Result<List<HourlyForecastEntity>> {
        val key = locationKey(latitude, longitude)
        return try {
            val today = LocalDate.now()
            val endDate = today.plusDays((forecastDays - 1).toLong().coerceAtLeast(0))
            val response = api.getHourlyForecast(
                latitude, longitude,
                startDate = today.toString(),
                endDate = endDate.toString()
            )
            val now = System.currentTimeMillis()
            val entities = response.hourly.time.indices.map { i ->
                HourlyForecastEntity(
                    locationKey = key,
                    time = response.hourly.time[i],
                    temperature = response.hourly.temperature?.get(i) ?: 0.0,
                    weatherCode = response.hourly.weatherCode?.get(i) ?: 0,
                    precipitation = response.hourly.precipitation?.get(i) ?: 0.0,
                    cachedAt = now
                )
            }
            hourlyDao.replaceForLocation(key, entities)
            Result.success(entities)
        } catch (e: Exception) {
            val cached = hourlyDao.getAll(key)
            if (cached.isNotEmpty()) {
                Result.failure(CachedHourlyException(e, cached))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun refreshDailyForecast(
        latitude: Double,
        longitude: Double,
        days: Int
    ): Result<List<DailyForecastEntity>> {
        val key = locationKey(latitude, longitude)
        return try {
            val response = api.getDailyForecast(latitude, longitude, forecastDays = days)
            val now = System.currentTimeMillis()
            val entities = response.daily.time.indices.map { i ->
                DailyForecastEntity(
                    locationKey = key,
                    date = response.daily.time[i],
                    weatherCode = response.daily.weatherCode?.get(i) ?: 0,
                    temperatureMax = response.daily.temperatureMax?.get(i) ?: 0.0,
                    temperatureMin = response.daily.temperatureMin?.get(i) ?: 0.0,
                    precipitationSum = response.daily.precipitationSum?.get(i) ?: 0.0,
                    windSpeedMax = response.daily.windSpeedMax?.get(i) ?: 0.0,
                    cachedAt = now
                )
            }
            dailyDao.replaceForLocation(key, entities)
            Result.success(entities)
        } catch (e: Exception) {
            val cached = dailyDao.getAll(key)
            if (cached.isNotEmpty()) {
                Result.failure(CachedDailyException(e, cached))
            } else {
                Result.failure(e)
            }
        }
    }

    companion object {
        fun locationKey(lat: Double, lon: Double): String =
            String.format(Locale.US, "%.2f_%.2f", lat, lon)
    }
}

class CachedDataException(
    cause: Exception,
    val cachedData: WeatherEntity
) : Exception("Network error, using cached data", cause)

class CachedHourlyException(
    cause: Exception,
    val cachedData: List<HourlyForecastEntity>
) : Exception("Network error, using cached hourly data", cause)

class CachedDailyException(
    cause: Exception,
    val cachedData: List<DailyForecastEntity>
) : Exception("Network error, using cached daily data", cause)
