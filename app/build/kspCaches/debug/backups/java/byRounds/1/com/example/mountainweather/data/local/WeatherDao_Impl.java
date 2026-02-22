package com.example.mountainweather.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class WeatherDao_Impl implements WeatherDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WeatherEntity> __insertionAdapterOfWeatherEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteWeather;

  public WeatherDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWeatherEntity = new EntityInsertionAdapter<WeatherEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `weather_cache` (`locationKey`,`locationName`,`latitude`,`longitude`,`temperature`,`apparentTemperature`,`weatherCode`,`windSpeed`,`windDirection`,`humidity`,`precipitation`,`pressure`,`time`,`cachedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WeatherEntity entity) {
        statement.bindString(1, entity.getLocationKey());
        statement.bindString(2, entity.getLocationName());
        statement.bindDouble(3, entity.getLatitude());
        statement.bindDouble(4, entity.getLongitude());
        statement.bindDouble(5, entity.getTemperature());
        statement.bindDouble(6, entity.getApparentTemperature());
        statement.bindLong(7, entity.getWeatherCode());
        statement.bindDouble(8, entity.getWindSpeed());
        statement.bindLong(9, entity.getWindDirection());
        statement.bindLong(10, entity.getHumidity());
        statement.bindDouble(11, entity.getPrecipitation());
        statement.bindDouble(12, entity.getPressure());
        statement.bindString(13, entity.getTime());
        statement.bindLong(14, entity.getCachedAt());
      }
    };
    this.__preparedStmtOfDeleteWeather = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM weather_cache WHERE locationKey = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertWeather(final WeatherEntity weather,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWeatherEntity.insert(weather);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteWeather(final String key, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteWeather.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, key);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteWeather.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<WeatherEntity> observeWeather(final String key) {
    final String _sql = "SELECT * FROM weather_cache WHERE locationKey = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"weather_cache"}, new Callable<WeatherEntity>() {
      @Override
      @Nullable
      public WeatherEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLocationKey = CursorUtil.getColumnIndexOrThrow(_cursor, "locationKey");
          final int _cursorIndexOfLocationName = CursorUtil.getColumnIndexOrThrow(_cursor, "locationName");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfTemperature = CursorUtil.getColumnIndexOrThrow(_cursor, "temperature");
          final int _cursorIndexOfApparentTemperature = CursorUtil.getColumnIndexOrThrow(_cursor, "apparentTemperature");
          final int _cursorIndexOfWeatherCode = CursorUtil.getColumnIndexOrThrow(_cursor, "weatherCode");
          final int _cursorIndexOfWindSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "windSpeed");
          final int _cursorIndexOfWindDirection = CursorUtil.getColumnIndexOrThrow(_cursor, "windDirection");
          final int _cursorIndexOfHumidity = CursorUtil.getColumnIndexOrThrow(_cursor, "humidity");
          final int _cursorIndexOfPrecipitation = CursorUtil.getColumnIndexOrThrow(_cursor, "precipitation");
          final int _cursorIndexOfPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "pressure");
          final int _cursorIndexOfTime = CursorUtil.getColumnIndexOrThrow(_cursor, "time");
          final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
          final WeatherEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpLocationKey;
            _tmpLocationKey = _cursor.getString(_cursorIndexOfLocationKey);
            final String _tmpLocationName;
            _tmpLocationName = _cursor.getString(_cursorIndexOfLocationName);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final double _tmpTemperature;
            _tmpTemperature = _cursor.getDouble(_cursorIndexOfTemperature);
            final double _tmpApparentTemperature;
            _tmpApparentTemperature = _cursor.getDouble(_cursorIndexOfApparentTemperature);
            final int _tmpWeatherCode;
            _tmpWeatherCode = _cursor.getInt(_cursorIndexOfWeatherCode);
            final double _tmpWindSpeed;
            _tmpWindSpeed = _cursor.getDouble(_cursorIndexOfWindSpeed);
            final int _tmpWindDirection;
            _tmpWindDirection = _cursor.getInt(_cursorIndexOfWindDirection);
            final int _tmpHumidity;
            _tmpHumidity = _cursor.getInt(_cursorIndexOfHumidity);
            final double _tmpPrecipitation;
            _tmpPrecipitation = _cursor.getDouble(_cursorIndexOfPrecipitation);
            final double _tmpPressure;
            _tmpPressure = _cursor.getDouble(_cursorIndexOfPressure);
            final String _tmpTime;
            _tmpTime = _cursor.getString(_cursorIndexOfTime);
            final long _tmpCachedAt;
            _tmpCachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
            _result = new WeatherEntity(_tmpLocationKey,_tmpLocationName,_tmpLatitude,_tmpLongitude,_tmpTemperature,_tmpApparentTemperature,_tmpWeatherCode,_tmpWindSpeed,_tmpWindDirection,_tmpHumidity,_tmpPrecipitation,_tmpPressure,_tmpTime,_tmpCachedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getWeather(final String key,
      final Continuation<? super WeatherEntity> $completion) {
    final String _sql = "SELECT * FROM weather_cache WHERE locationKey = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<WeatherEntity>() {
      @Override
      @Nullable
      public WeatherEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLocationKey = CursorUtil.getColumnIndexOrThrow(_cursor, "locationKey");
          final int _cursorIndexOfLocationName = CursorUtil.getColumnIndexOrThrow(_cursor, "locationName");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfTemperature = CursorUtil.getColumnIndexOrThrow(_cursor, "temperature");
          final int _cursorIndexOfApparentTemperature = CursorUtil.getColumnIndexOrThrow(_cursor, "apparentTemperature");
          final int _cursorIndexOfWeatherCode = CursorUtil.getColumnIndexOrThrow(_cursor, "weatherCode");
          final int _cursorIndexOfWindSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "windSpeed");
          final int _cursorIndexOfWindDirection = CursorUtil.getColumnIndexOrThrow(_cursor, "windDirection");
          final int _cursorIndexOfHumidity = CursorUtil.getColumnIndexOrThrow(_cursor, "humidity");
          final int _cursorIndexOfPrecipitation = CursorUtil.getColumnIndexOrThrow(_cursor, "precipitation");
          final int _cursorIndexOfPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "pressure");
          final int _cursorIndexOfTime = CursorUtil.getColumnIndexOrThrow(_cursor, "time");
          final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
          final WeatherEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpLocationKey;
            _tmpLocationKey = _cursor.getString(_cursorIndexOfLocationKey);
            final String _tmpLocationName;
            _tmpLocationName = _cursor.getString(_cursorIndexOfLocationName);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final double _tmpTemperature;
            _tmpTemperature = _cursor.getDouble(_cursorIndexOfTemperature);
            final double _tmpApparentTemperature;
            _tmpApparentTemperature = _cursor.getDouble(_cursorIndexOfApparentTemperature);
            final int _tmpWeatherCode;
            _tmpWeatherCode = _cursor.getInt(_cursorIndexOfWeatherCode);
            final double _tmpWindSpeed;
            _tmpWindSpeed = _cursor.getDouble(_cursorIndexOfWindSpeed);
            final int _tmpWindDirection;
            _tmpWindDirection = _cursor.getInt(_cursorIndexOfWindDirection);
            final int _tmpHumidity;
            _tmpHumidity = _cursor.getInt(_cursorIndexOfHumidity);
            final double _tmpPrecipitation;
            _tmpPrecipitation = _cursor.getDouble(_cursorIndexOfPrecipitation);
            final double _tmpPressure;
            _tmpPressure = _cursor.getDouble(_cursorIndexOfPressure);
            final String _tmpTime;
            _tmpTime = _cursor.getString(_cursorIndexOfTime);
            final long _tmpCachedAt;
            _tmpCachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
            _result = new WeatherEntity(_tmpLocationKey,_tmpLocationName,_tmpLatitude,_tmpLongitude,_tmpTemperature,_tmpApparentTemperature,_tmpWeatherCode,_tmpWindSpeed,_tmpWindDirection,_tmpHumidity,_tmpPrecipitation,_tmpPressure,_tmpTime,_tmpCachedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
