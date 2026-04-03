# Retrofit: keep HTTP method annotations and annotated interface methods
-keepattributes Signature, *Annotation*
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson: keep SerializedName annotations and API response/request model classes
-keepattributes EnclosingMethod, RuntimeVisibleAnnotations
-keep class com.ergonomic.mountainweather.data.WeatherResponse { *; }
-keep class com.ergonomic.mountainweather.data.WeatherResponse$* { *; }
-keep class com.ergonomic.mountainweather.data.CurrentWeather { *; }
-keep class com.ergonomic.mountainweather.data.HourlyForecastResponse { *; }
-keep class com.ergonomic.mountainweather.data.HourlyData { *; }
-keep class com.ergonomic.mountainweather.data.DailyForecastResponse { *; }
-keep class com.ergonomic.mountainweather.data.DailyData { *; }
-keep class com.ergonomic.mountainweather.data.AirQualityApi$* { *; }
-keep class com.ergonomic.mountainweather.data.GeocodingApi$* { *; }

# Room: keep database, entities, and DAOs
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# WorkManager: keep only our worker class (instantiated by reflection)
-keep class com.ergonomic.mountainweather.data.sync.WeatherSyncWorker { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlin metadata (needed for reflection-based libraries)
-keep class kotlin.Metadata { *; }

# Google Play Services Location: keep only what's accessed via reflection
-keep class com.google.android.gms.location.FusedLocationProviderClient { *; }
-keep class com.google.android.gms.location.LocationServices { *; }
-dontwarn com.google.android.gms.**
