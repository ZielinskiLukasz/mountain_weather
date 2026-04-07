package com.ergonomic.mountainweather.data

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface PhotonApi {

    @GET("api/")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10,
        @Query("lang") lang: String = "en"
    ): PhotonResponse

    companion object {
        private const val BASE_URL = "https://photon.komoot.io/"

        fun create(): PhotonApi {
            val client = OkHttpClient.Builder().build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PhotonApi::class.java)
        }
    }
}

data class PhotonResponse(
    val features: List<PhotonFeature>? = null
)

data class PhotonFeature(
    val geometry: PhotonGeometry? = null,
    val properties: PhotonProperties? = null
)

data class PhotonGeometry(
    val coordinates: List<Double>? = null
)

data class PhotonProperties(
    val name: String? = null,
    @SerializedName("osm_key")
    val osmKey: String? = null,
    @SerializedName("osm_value")
    val osmValue: String? = null,
    val country: String? = null,
    val state: String? = null,
    val city: String? = null,
    val type: String? = null
)
