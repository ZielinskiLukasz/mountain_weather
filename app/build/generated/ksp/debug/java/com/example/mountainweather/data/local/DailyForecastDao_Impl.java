package com.example.mountainweather.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DailyForecastDao_Impl implements DailyForecastDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DailyForecastEntity> __insertionAdapterOfDailyForecastEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteForLocation;

  public DailyForecastDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDailyForecastEntity = new EntityInsertionAdapter<DailyForecastEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `daily_forecast` (`locationKey`,`date`,`weatherCode`,`temperatureMax`,`temperatureMin`,`precipitationSum`,`windSpeedMax`,`cachedAt`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DailyForecastEntity entity) {
        statement.bindString(1, entity.getLocationKey());
        statement.bindString(2, entity.getDate());
        statement.bindLong(3, entity.getWeatherCode());
        statement.bindDouble(4, entity.getTemperatureMax());
        statement.bindDouble(5, entity.getTemperatureMin());
        statement.bindDouble(6, entity.getPrecipitationSum());
        statement.bindDouble(7, entity.getWindSpeedMax());
        statement.bindLong(8, entity.getCachedAt());
      }
    };
    this.__preparedStmtOfDeleteForLocation = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM daily_forecast WHERE locationKey = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<DailyForecastEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDailyForecastEntity.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object replaceForLocation(final String key, final List<DailyForecastEntity> items,
      final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> DailyForecastDao.DefaultImpls.replaceForLocation(DailyForecastDao_Impl.this, key, items, __cont), $completion);
  }

  @Override
  public Object deleteForLocation(final String key, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteForLocation.acquire();
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
          __preparedStmtOfDeleteForLocation.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<DailyForecastEntity>> observe(final String key) {
    final String _sql = "SELECT * FROM daily_forecast WHERE locationKey = ? ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"daily_forecast"}, new Callable<List<DailyForecastEntity>>() {
      @Override
      @NonNull
      public List<DailyForecastEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLocationKey = CursorUtil.getColumnIndexOrThrow(_cursor, "locationKey");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfWeatherCode = CursorUtil.getColumnIndexOrThrow(_cursor, "weatherCode");
          final int _cursorIndexOfTemperatureMax = CursorUtil.getColumnIndexOrThrow(_cursor, "temperatureMax");
          final int _cursorIndexOfTemperatureMin = CursorUtil.getColumnIndexOrThrow(_cursor, "temperatureMin");
          final int _cursorIndexOfPrecipitationSum = CursorUtil.getColumnIndexOrThrow(_cursor, "precipitationSum");
          final int _cursorIndexOfWindSpeedMax = CursorUtil.getColumnIndexOrThrow(_cursor, "windSpeedMax");
          final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
          final List<DailyForecastEntity> _result = new ArrayList<DailyForecastEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyForecastEntity _item;
            final String _tmpLocationKey;
            _tmpLocationKey = _cursor.getString(_cursorIndexOfLocationKey);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final int _tmpWeatherCode;
            _tmpWeatherCode = _cursor.getInt(_cursorIndexOfWeatherCode);
            final double _tmpTemperatureMax;
            _tmpTemperatureMax = _cursor.getDouble(_cursorIndexOfTemperatureMax);
            final double _tmpTemperatureMin;
            _tmpTemperatureMin = _cursor.getDouble(_cursorIndexOfTemperatureMin);
            final double _tmpPrecipitationSum;
            _tmpPrecipitationSum = _cursor.getDouble(_cursorIndexOfPrecipitationSum);
            final double _tmpWindSpeedMax;
            _tmpWindSpeedMax = _cursor.getDouble(_cursorIndexOfWindSpeedMax);
            final long _tmpCachedAt;
            _tmpCachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
            _item = new DailyForecastEntity(_tmpLocationKey,_tmpDate,_tmpWeatherCode,_tmpTemperatureMax,_tmpTemperatureMin,_tmpPrecipitationSum,_tmpWindSpeedMax,_tmpCachedAt);
            _result.add(_item);
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
  public Object getAll(final String key,
      final Continuation<? super List<DailyForecastEntity>> $completion) {
    final String _sql = "SELECT * FROM daily_forecast WHERE locationKey = ? ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DailyForecastEntity>>() {
      @Override
      @NonNull
      public List<DailyForecastEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLocationKey = CursorUtil.getColumnIndexOrThrow(_cursor, "locationKey");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfWeatherCode = CursorUtil.getColumnIndexOrThrow(_cursor, "weatherCode");
          final int _cursorIndexOfTemperatureMax = CursorUtil.getColumnIndexOrThrow(_cursor, "temperatureMax");
          final int _cursorIndexOfTemperatureMin = CursorUtil.getColumnIndexOrThrow(_cursor, "temperatureMin");
          final int _cursorIndexOfPrecipitationSum = CursorUtil.getColumnIndexOrThrow(_cursor, "precipitationSum");
          final int _cursorIndexOfWindSpeedMax = CursorUtil.getColumnIndexOrThrow(_cursor, "windSpeedMax");
          final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
          final List<DailyForecastEntity> _result = new ArrayList<DailyForecastEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyForecastEntity _item;
            final String _tmpLocationKey;
            _tmpLocationKey = _cursor.getString(_cursorIndexOfLocationKey);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final int _tmpWeatherCode;
            _tmpWeatherCode = _cursor.getInt(_cursorIndexOfWeatherCode);
            final double _tmpTemperatureMax;
            _tmpTemperatureMax = _cursor.getDouble(_cursorIndexOfTemperatureMax);
            final double _tmpTemperatureMin;
            _tmpTemperatureMin = _cursor.getDouble(_cursorIndexOfTemperatureMin);
            final double _tmpPrecipitationSum;
            _tmpPrecipitationSum = _cursor.getDouble(_cursorIndexOfPrecipitationSum);
            final double _tmpWindSpeedMax;
            _tmpWindSpeedMax = _cursor.getDouble(_cursorIndexOfWindSpeedMax);
            final long _tmpCachedAt;
            _tmpCachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
            _item = new DailyForecastEntity(_tmpLocationKey,_tmpDate,_tmpWeatherCode,_tmpTemperatureMax,_tmpTemperatureMin,_tmpPrecipitationSum,_tmpWindSpeedMax,_tmpCachedAt);
            _result.add(_item);
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
