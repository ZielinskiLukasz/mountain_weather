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
public final class HourlyForecastDao_Impl implements HourlyForecastDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<HourlyForecastEntity> __insertionAdapterOfHourlyForecastEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteForLocation;

  public HourlyForecastDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHourlyForecastEntity = new EntityInsertionAdapter<HourlyForecastEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `hourly_forecast` (`locationKey`,`time`,`temperature`,`weatherCode`,`precipitation`,`cachedAt`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HourlyForecastEntity entity) {
        statement.bindString(1, entity.getLocationKey());
        statement.bindString(2, entity.getTime());
        statement.bindDouble(3, entity.getTemperature());
        statement.bindLong(4, entity.getWeatherCode());
        statement.bindDouble(5, entity.getPrecipitation());
        statement.bindLong(6, entity.getCachedAt());
      }
    };
    this.__preparedStmtOfDeleteForLocation = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM hourly_forecast WHERE locationKey = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<HourlyForecastEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfHourlyForecastEntity.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object replaceForLocation(final String key, final List<HourlyForecastEntity> items,
      final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> HourlyForecastDao.DefaultImpls.replaceForLocation(HourlyForecastDao_Impl.this, key, items, __cont), $completion);
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
  public Flow<List<HourlyForecastEntity>> observe(final String key) {
    final String _sql = "SELECT * FROM hourly_forecast WHERE locationKey = ? ORDER BY time ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"hourly_forecast"}, new Callable<List<HourlyForecastEntity>>() {
      @Override
      @NonNull
      public List<HourlyForecastEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLocationKey = CursorUtil.getColumnIndexOrThrow(_cursor, "locationKey");
          final int _cursorIndexOfTime = CursorUtil.getColumnIndexOrThrow(_cursor, "time");
          final int _cursorIndexOfTemperature = CursorUtil.getColumnIndexOrThrow(_cursor, "temperature");
          final int _cursorIndexOfWeatherCode = CursorUtil.getColumnIndexOrThrow(_cursor, "weatherCode");
          final int _cursorIndexOfPrecipitation = CursorUtil.getColumnIndexOrThrow(_cursor, "precipitation");
          final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
          final List<HourlyForecastEntity> _result = new ArrayList<HourlyForecastEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HourlyForecastEntity _item;
            final String _tmpLocationKey;
            _tmpLocationKey = _cursor.getString(_cursorIndexOfLocationKey);
            final String _tmpTime;
            _tmpTime = _cursor.getString(_cursorIndexOfTime);
            final double _tmpTemperature;
            _tmpTemperature = _cursor.getDouble(_cursorIndexOfTemperature);
            final int _tmpWeatherCode;
            _tmpWeatherCode = _cursor.getInt(_cursorIndexOfWeatherCode);
            final double _tmpPrecipitation;
            _tmpPrecipitation = _cursor.getDouble(_cursorIndexOfPrecipitation);
            final long _tmpCachedAt;
            _tmpCachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
            _item = new HourlyForecastEntity(_tmpLocationKey,_tmpTime,_tmpTemperature,_tmpWeatherCode,_tmpPrecipitation,_tmpCachedAt);
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
      final Continuation<? super List<HourlyForecastEntity>> $completion) {
    final String _sql = "SELECT * FROM hourly_forecast WHERE locationKey = ? ORDER BY time ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<HourlyForecastEntity>>() {
      @Override
      @NonNull
      public List<HourlyForecastEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLocationKey = CursorUtil.getColumnIndexOrThrow(_cursor, "locationKey");
          final int _cursorIndexOfTime = CursorUtil.getColumnIndexOrThrow(_cursor, "time");
          final int _cursorIndexOfTemperature = CursorUtil.getColumnIndexOrThrow(_cursor, "temperature");
          final int _cursorIndexOfWeatherCode = CursorUtil.getColumnIndexOrThrow(_cursor, "weatherCode");
          final int _cursorIndexOfPrecipitation = CursorUtil.getColumnIndexOrThrow(_cursor, "precipitation");
          final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
          final List<HourlyForecastEntity> _result = new ArrayList<HourlyForecastEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HourlyForecastEntity _item;
            final String _tmpLocationKey;
            _tmpLocationKey = _cursor.getString(_cursorIndexOfLocationKey);
            final String _tmpTime;
            _tmpTime = _cursor.getString(_cursorIndexOfTime);
            final double _tmpTemperature;
            _tmpTemperature = _cursor.getDouble(_cursorIndexOfTemperature);
            final int _tmpWeatherCode;
            _tmpWeatherCode = _cursor.getInt(_cursorIndexOfWeatherCode);
            final double _tmpPrecipitation;
            _tmpPrecipitation = _cursor.getDouble(_cursorIndexOfPrecipitation);
            final long _tmpCachedAt;
            _tmpCachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
            _item = new HourlyForecastEntity(_tmpLocationKey,_tmpTime,_tmpTemperature,_tmpWeatherCode,_tmpPrecipitation,_tmpCachedAt);
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
