# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# Gson
-keep class com.google.gson.** { *; }
-keepattributes EnclosingMethod
-keep class com.example.mountainweather.data.WeatherResponse { *; }
-keep class com.example.mountainweather.data.CurrentWeather { *; }
-keep class com.example.mountainweather.data.HourlyForecastResponse { *; }
-keep class com.example.mountainweather.data.HourlyData { *; }
-keep class com.example.mountainweather.data.DailyForecastResponse { *; }
-keep class com.example.mountainweather.data.DailyData { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlin serialization / metadata
-keepattributes RuntimeVisibleAnnotations
-keep class kotlin.Metadata { *; }

# WorkManager
-keep class androidx.work.** { *; }
-keep class com.example.mountainweather.data.sync.WeatherSyncWorker { *; }

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# DataStore
-keep class androidx.datastore.** { *; }
