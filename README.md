# Pogoda Górska / Mountain Weather

Aplikacja pogodowa na Androida, nastawiona na użycie w górach i przy słabym zasięgu: mały payload z API, cache offline, odświeżanie w tle i widgety na ekran główny.

**Identyfikator:** `com.ergonomic.mountainweather`  
**Wymagania:** Android 8.0+ (`minSdk` 26), `targetSdk` / `compileSdk` 36

## Funkcje

- Pogoda bieżąca z konfigurowalnymi parametrami (wiatr, opady, UV, AQI, izoterma 0 °C i inne)
- Prognoza godzinowa i dzienna (3 / 5 / 7 / 14 dni) — pobierane są tylko włączone typy
- Ulubione lokalizacje (max. 10) i lista ostatnich; przełączanie miast gestem lewo–prawo
- Wyszukiwanie miejsc i GPS
- Tryb jasny / ciemny / systemowy
- Odporny sync (retry, circuit breaker) i okresowe odświeżanie ulubionych w tle
- Widgety Glance: karuzela ulubionych, bieżąca pogoda, parametry, godzinówka, wielodniowa, przypięte miasto, pasek opadów 24h, słońce i UV

Języki UI: angielski, polski, niemiecki, hiszpański.

## Dane

Prognoza, geokodowanie i jakość powietrza pochodzą z [Open-Meteo](https://open-meteo.com/) (bez klucza API).

## Stack

- Kotlin, Jetpack Compose (Material 3), Navigation
- Room, DataStore
- Retrofit / OkHttp
- WorkManager, Glance widgets
- Play Services Location, in-app updates
- AGP 9.0.1, Gradle 9.1.0, Kotlin 2.2.10, JDK 17

## Budowanie

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Release (podpisany APK i AAB):

```bash
./bin/release.sh publish   # podbija versionName/versionCode, buduje APK+AAB
./bin/release.sh apk       # tylko APK, bez podbijania wersji
./bin/release.sh bundle    # tylko AAB
```

Wersję podbija wyłącznie `publish`. Debug APK nie zmienia numeru wersji.

Podpis release wymaga lokalnego `keystore.properties` i pliku keystore (katalog `keystore/`). Oba są w `.gitignore` i nie trafiają do repozytorium.

## Struktura

```
app/src/main/java/com/ergonomic/mountainweather/
  data/          API, Room, repozytoria, sync
  ui/            ekrany (lokalizacje, ustawienia)
  widget/        widgety Glance
  util/          kody WMO, formatowanie
bin/release.sh   wydania APK/AAB
```
