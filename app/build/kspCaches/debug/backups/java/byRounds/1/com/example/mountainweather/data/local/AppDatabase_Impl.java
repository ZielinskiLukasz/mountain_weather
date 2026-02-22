package com.example.mountainweather.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile WeatherDao _weatherDao;

  private volatile SavedLocationDao _savedLocationDao;

  private volatile HourlyForecastDao _hourlyForecastDao;

  private volatile DailyForecastDao _dailyForecastDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(4) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `weather_cache` (`locationKey` TEXT NOT NULL, `locationName` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `temperature` REAL NOT NULL, `apparentTemperature` REAL NOT NULL, `weatherCode` INTEGER NOT NULL, `windSpeed` REAL NOT NULL, `windDirection` INTEGER NOT NULL, `humidity` INTEGER NOT NULL, `precipitation` REAL NOT NULL, `pressure` REAL NOT NULL, `time` TEXT NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`locationKey`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `saved_locations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `country` TEXT, `region` TEXT, `isFavorite` INTEGER NOT NULL, `lastUsedAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_saved_locations_latitude_longitude` ON `saved_locations` (`latitude`, `longitude`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `hourly_forecast` (`locationKey` TEXT NOT NULL, `time` TEXT NOT NULL, `temperature` REAL NOT NULL, `weatherCode` INTEGER NOT NULL, `precipitation` REAL NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`locationKey`, `time`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_forecast` (`locationKey` TEXT NOT NULL, `date` TEXT NOT NULL, `weatherCode` INTEGER NOT NULL, `temperatureMax` REAL NOT NULL, `temperatureMin` REAL NOT NULL, `precipitationSum` REAL NOT NULL, `windSpeedMax` REAL NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`locationKey`, `date`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'b80fdee2c86c9ed38b63959d03bcf95a')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `weather_cache`");
        db.execSQL("DROP TABLE IF EXISTS `saved_locations`");
        db.execSQL("DROP TABLE IF EXISTS `hourly_forecast`");
        db.execSQL("DROP TABLE IF EXISTS `daily_forecast`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsWeatherCache = new HashMap<String, TableInfo.Column>(14);
        _columnsWeatherCache.put("locationKey", new TableInfo.Column("locationKey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherCache.put("locationName", new TableInfo.Column("locationName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherCache.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherCache.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherCache.put("temperature", new TableInfo.Column("temperature", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherCache.put("apparentTemperature", new TableInfo.Column("apparentTemperature", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherCache.put("weatherCode", new TableInfo.Column("weatherCode", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherCache.put("windSpeed", new TableInfo.Column("windSpeed", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherCache.put("windDirection", new TableInfo.Column("windDirection", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherCache.put("humidity", new TableInfo.Column("humidity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherCache.put("precipitation", new TableInfo.Column("precipitation", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherCache.put("pressure", new TableInfo.Column("pressure", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherCache.put("time", new TableInfo.Column("time", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherCache.put("cachedAt", new TableInfo.Column("cachedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWeatherCache = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWeatherCache = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWeatherCache = new TableInfo("weather_cache", _columnsWeatherCache, _foreignKeysWeatherCache, _indicesWeatherCache);
        final TableInfo _existingWeatherCache = TableInfo.read(db, "weather_cache");
        if (!_infoWeatherCache.equals(_existingWeatherCache)) {
          return new RoomOpenHelper.ValidationResult(false, "weather_cache(com.example.mountainweather.data.local.WeatherEntity).\n"
                  + " Expected:\n" + _infoWeatherCache + "\n"
                  + " Found:\n" + _existingWeatherCache);
        }
        final HashMap<String, TableInfo.Column> _columnsSavedLocations = new HashMap<String, TableInfo.Column>(9);
        _columnsSavedLocations.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedLocations.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedLocations.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedLocations.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedLocations.put("country", new TableInfo.Column("country", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedLocations.put("region", new TableInfo.Column("region", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedLocations.put("isFavorite", new TableInfo.Column("isFavorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedLocations.put("lastUsedAt", new TableInfo.Column("lastUsedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedLocations.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSavedLocations = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSavedLocations = new HashSet<TableInfo.Index>(1);
        _indicesSavedLocations.add(new TableInfo.Index("index_saved_locations_latitude_longitude", true, Arrays.asList("latitude", "longitude"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoSavedLocations = new TableInfo("saved_locations", _columnsSavedLocations, _foreignKeysSavedLocations, _indicesSavedLocations);
        final TableInfo _existingSavedLocations = TableInfo.read(db, "saved_locations");
        if (!_infoSavedLocations.equals(_existingSavedLocations)) {
          return new RoomOpenHelper.ValidationResult(false, "saved_locations(com.example.mountainweather.data.local.SavedLocationEntity).\n"
                  + " Expected:\n" + _infoSavedLocations + "\n"
                  + " Found:\n" + _existingSavedLocations);
        }
        final HashMap<String, TableInfo.Column> _columnsHourlyForecast = new HashMap<String, TableInfo.Column>(6);
        _columnsHourlyForecast.put("locationKey", new TableInfo.Column("locationKey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHourlyForecast.put("time", new TableInfo.Column("time", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHourlyForecast.put("temperature", new TableInfo.Column("temperature", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHourlyForecast.put("weatherCode", new TableInfo.Column("weatherCode", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHourlyForecast.put("precipitation", new TableInfo.Column("precipitation", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHourlyForecast.put("cachedAt", new TableInfo.Column("cachedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHourlyForecast = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHourlyForecast = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoHourlyForecast = new TableInfo("hourly_forecast", _columnsHourlyForecast, _foreignKeysHourlyForecast, _indicesHourlyForecast);
        final TableInfo _existingHourlyForecast = TableInfo.read(db, "hourly_forecast");
        if (!_infoHourlyForecast.equals(_existingHourlyForecast)) {
          return new RoomOpenHelper.ValidationResult(false, "hourly_forecast(com.example.mountainweather.data.local.HourlyForecastEntity).\n"
                  + " Expected:\n" + _infoHourlyForecast + "\n"
                  + " Found:\n" + _existingHourlyForecast);
        }
        final HashMap<String, TableInfo.Column> _columnsDailyForecast = new HashMap<String, TableInfo.Column>(8);
        _columnsDailyForecast.put("locationKey", new TableInfo.Column("locationKey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyForecast.put("date", new TableInfo.Column("date", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyForecast.put("weatherCode", new TableInfo.Column("weatherCode", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyForecast.put("temperatureMax", new TableInfo.Column("temperatureMax", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyForecast.put("temperatureMin", new TableInfo.Column("temperatureMin", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyForecast.put("precipitationSum", new TableInfo.Column("precipitationSum", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyForecast.put("windSpeedMax", new TableInfo.Column("windSpeedMax", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyForecast.put("cachedAt", new TableInfo.Column("cachedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDailyForecast = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDailyForecast = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDailyForecast = new TableInfo("daily_forecast", _columnsDailyForecast, _foreignKeysDailyForecast, _indicesDailyForecast);
        final TableInfo _existingDailyForecast = TableInfo.read(db, "daily_forecast");
        if (!_infoDailyForecast.equals(_existingDailyForecast)) {
          return new RoomOpenHelper.ValidationResult(false, "daily_forecast(com.example.mountainweather.data.local.DailyForecastEntity).\n"
                  + " Expected:\n" + _infoDailyForecast + "\n"
                  + " Found:\n" + _existingDailyForecast);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "b80fdee2c86c9ed38b63959d03bcf95a", "8ee9876eeaf61b8c41153eeecde60132");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "weather_cache","saved_locations","hourly_forecast","daily_forecast");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `weather_cache`");
      _db.execSQL("DELETE FROM `saved_locations`");
      _db.execSQL("DELETE FROM `hourly_forecast`");
      _db.execSQL("DELETE FROM `daily_forecast`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(WeatherDao.class, WeatherDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SavedLocationDao.class, SavedLocationDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HourlyForecastDao.class, HourlyForecastDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DailyForecastDao.class, DailyForecastDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public WeatherDao weatherDao() {
    if (_weatherDao != null) {
      return _weatherDao;
    } else {
      synchronized(this) {
        if(_weatherDao == null) {
          _weatherDao = new WeatherDao_Impl(this);
        }
        return _weatherDao;
      }
    }
  }

  @Override
  public SavedLocationDao savedLocationDao() {
    if (_savedLocationDao != null) {
      return _savedLocationDao;
    } else {
      synchronized(this) {
        if(_savedLocationDao == null) {
          _savedLocationDao = new SavedLocationDao_Impl(this);
        }
        return _savedLocationDao;
      }
    }
  }

  @Override
  public HourlyForecastDao hourlyForecastDao() {
    if (_hourlyForecastDao != null) {
      return _hourlyForecastDao;
    } else {
      synchronized(this) {
        if(_hourlyForecastDao == null) {
          _hourlyForecastDao = new HourlyForecastDao_Impl(this);
        }
        return _hourlyForecastDao;
      }
    }
  }

  @Override
  public DailyForecastDao dailyForecastDao() {
    if (_dailyForecastDao != null) {
      return _dailyForecastDao;
    } else {
      synchronized(this) {
        if(_dailyForecastDao == null) {
          _dailyForecastDao = new DailyForecastDao_Impl(this);
        }
        return _dailyForecastDao;
      }
    }
  }
}
