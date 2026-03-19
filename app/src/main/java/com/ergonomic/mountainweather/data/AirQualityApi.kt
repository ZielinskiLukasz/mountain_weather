package com.ergonomic.mountainweather.data

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface AirQualityApi {

    @GET("v1/air-quality")
    suspend fun getCurrent(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String,
        @Query("timezone") timezone: String = "auto"
    ): AirQualityResponse

    companion object {
        private const val BASE_URL = "https://air-quality-api.open-meteo.com/"

        fun create(): AirQualityApi =
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AirQualityApi::class.java)
    }
}

data class AirQualityResponse(
    val current: AirQualityCurrent
)

data class AirQualityCurrent(
    @SerializedName("european_aqi") val europeanAqi: Int? = null,
    @SerializedName("us_aqi") val usAqi: Int? = null,
    @SerializedName("pm2_5") val pm25: Double? = null,
    val pm10: Double? = null,
    val ozone: Double? = null
)
