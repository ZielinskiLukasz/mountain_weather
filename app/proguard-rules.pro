# Retrofit: keep HTTP method annotations and annotated interface methods
-keepattributes Signature, *Annotation*
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation interface com.ergonomic.mountainweather.data.OpenMeteoApi
-keep,allowobfuscation interface com.ergonomic.mountainweather.data.AirQualityApi
-keep,allowobfuscation interface com.ergonomic.mountainweather.data.GeocodingApi
-keep,allowobfuscation interface com.ergonomic.mountainweather.data.PhotonApi
-keep class com.ergonomic.mountainweather.data.PhotonApi$Companion { *; }
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
-keep class com.ergonomic.mountainweather.data.AirQualityResponse { *; }
-keep class com.ergonomic.mountainweather.data.AirQualityCurrent { *; }
-keep class com.ergonomic.mountainweather.data.GeocodingResponse { *; }
-keep class com.ergonomic.mountainweather.data.GeocodingResult { *; }
-keep class com.ergonomic.mountainweather.data.PhotonResponse { *; }
-keep class com.ergonomic.mountainweather.data.PhotonFeature { *; }
-keep class com.ergonomic.mountainweather.data.PhotonGeometry { *; }
-keep class com.ergonomic.mountainweather.data.PhotonProperties { *; }

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

# Google Play Services Location
-keep class com.google.android.gms.location.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn com.google.android.gms.**

# Google Play In-App Updates
-keep class com.google.android.play.core.appupdate.** { *; }
-keep class com.google.android.play.core.install.** { *; }
-keep class com.google.android.play.core.tasks.** { *; }
-dontwarn com.google.android.play.**

# Home screen widgets (Jetpack Glance)
-keep class com.ergonomic.mountainweather.widget.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
-dontwarn androidx.glance.**
