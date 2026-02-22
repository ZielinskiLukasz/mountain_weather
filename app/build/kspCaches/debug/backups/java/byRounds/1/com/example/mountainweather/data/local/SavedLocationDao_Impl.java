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
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
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
public final class SavedLocationDao_Impl implements SavedLocationDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SavedLocationEntity> __insertionAdapterOfSavedLocationEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLastUsed;

  private final SharedSQLiteStatement __preparedStmtOfToggleFavorite;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  public SavedLocationDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSavedLocationEntity = new EntityInsertionAdapter<SavedLocationEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `saved_locations` (`id`,`name`,`latitude`,`longitude`,`country`,`region`,`isFavorite`,`lastUsedAt`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SavedLocationEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindDouble(3, entity.getLatitude());
        statement.bindDouble(4, entity.getLongitude());
        if (entity.getCountry() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getCountry());
        }
        if (entity.getRegion() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getRegion());
        }
        final int _tmp = entity.isFavorite() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.getLastUsedAt());
        statement.bindLong(9, entity.getCreatedAt());
      }
    };
    this.__preparedStmtOfUpdateLastUsed = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE saved_locations SET lastUsedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfToggleFavorite = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE saved_locations SET isFavorite = NOT isFavorite WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM saved_locations WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final SavedLocationEntity location,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSavedLocationEntity.insertAndReturnId(location);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateLastUsed(final long id, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateLastUsed.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfUpdateLastUsed.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object toggleFavorite(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfToggleFavorite.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfToggleFavorite.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDelete.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfDelete.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SavedLocationEntity>> observeFavorites(final int limit) {
    final String _sql = "SELECT * FROM saved_locations WHERE isFavorite = 1 ORDER BY name ASC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"saved_locations"}, new Callable<List<SavedLocationEntity>>() {
      @Override
      @NonNull
      public List<SavedLocationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfCountry = CursorUtil.getColumnIndexOrThrow(_cursor, "country");
          final int _cursorIndexOfRegion = CursorUtil.getColumnIndexOrThrow(_cursor, "region");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfLastUsedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUsedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<SavedLocationEntity> _result = new ArrayList<SavedLocationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SavedLocationEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final String _tmpCountry;
            if (_cursor.isNull(_cursorIndexOfCountry)) {
              _tmpCountry = null;
            } else {
              _tmpCountry = _cursor.getString(_cursorIndexOfCountry);
            }
            final String _tmpRegion;
            if (_cursor.isNull(_cursorIndexOfRegion)) {
              _tmpRegion = null;
            } else {
              _tmpRegion = _cursor.getString(_cursorIndexOfRegion);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final long _tmpLastUsedAt;
            _tmpLastUsedAt = _cursor.getLong(_cursorIndexOfLastUsedAt);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new SavedLocationEntity(_tmpId,_tmpName,_tmpLatitude,_tmpLongitude,_tmpCountry,_tmpRegion,_tmpIsFavorite,_tmpLastUsedAt,_tmpCreatedAt);
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
  public Object getFavorites(final Continuation<? super List<SavedLocationEntity>> $completion) {
    final String _sql = "SELECT * FROM saved_locations WHERE isFavorite = 1 ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SavedLocationEntity>>() {
      @Override
      @NonNull
      public List<SavedLocationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfCountry = CursorUtil.getColumnIndexOrThrow(_cursor, "country");
          final int _cursorIndexOfRegion = CursorUtil.getColumnIndexOrThrow(_cursor, "region");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfLastUsedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUsedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<SavedLocationEntity> _result = new ArrayList<SavedLocationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SavedLocationEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final String _tmpCountry;
            if (_cursor.isNull(_cursorIndexOfCountry)) {
              _tmpCountry = null;
            } else {
              _tmpCountry = _cursor.getString(_cursorIndexOfCountry);
            }
            final String _tmpRegion;
            if (_cursor.isNull(_cursorIndexOfRegion)) {
              _tmpRegion = null;
            } else {
              _tmpRegion = _cursor.getString(_cursorIndexOfRegion);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final long _tmpLastUsedAt;
            _tmpLastUsedAt = _cursor.getLong(_cursorIndexOfLastUsedAt);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new SavedLocationEntity(_tmpId,_tmpName,_tmpLatitude,_tmpLongitude,_tmpCountry,_tmpRegion,_tmpIsFavorite,_tmpLastUsedAt,_tmpCreatedAt);
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

  @Override
  public Flow<List<SavedLocationEntity>> observeRecent(final int limit) {
    final String _sql = "SELECT * FROM saved_locations WHERE isFavorite = 0 ORDER BY lastUsedAt DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"saved_locations"}, new Callable<List<SavedLocationEntity>>() {
      @Override
      @NonNull
      public List<SavedLocationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfCountry = CursorUtil.getColumnIndexOrThrow(_cursor, "country");
          final int _cursorIndexOfRegion = CursorUtil.getColumnIndexOrThrow(_cursor, "region");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfLastUsedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUsedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<SavedLocationEntity> _result = new ArrayList<SavedLocationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SavedLocationEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final String _tmpCountry;
            if (_cursor.isNull(_cursorIndexOfCountry)) {
              _tmpCountry = null;
            } else {
              _tmpCountry = _cursor.getString(_cursorIndexOfCountry);
            }
            final String _tmpRegion;
            if (_cursor.isNull(_cursorIndexOfRegion)) {
              _tmpRegion = null;
            } else {
              _tmpRegion = _cursor.getString(_cursorIndexOfRegion);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final long _tmpLastUsedAt;
            _tmpLastUsedAt = _cursor.getLong(_cursorIndexOfLastUsedAt);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new SavedLocationEntity(_tmpId,_tmpName,_tmpLatitude,_tmpLongitude,_tmpCountry,_tmpRegion,_tmpIsFavorite,_tmpLastUsedAt,_tmpCreatedAt);
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
  public Object findByCoordinates(final double lat, final double lon,
      final Continuation<? super SavedLocationEntity> $completion) {
    final String _sql = "SELECT * FROM saved_locations WHERE ABS(latitude - ?) < 0.005 AND ABS(longitude - ?) < 0.005 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindDouble(_argIndex, lat);
    _argIndex = 2;
    _statement.bindDouble(_argIndex, lon);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SavedLocationEntity>() {
      @Override
      @Nullable
      public SavedLocationEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfCountry = CursorUtil.getColumnIndexOrThrow(_cursor, "country");
          final int _cursorIndexOfRegion = CursorUtil.getColumnIndexOrThrow(_cursor, "region");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfLastUsedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUsedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final SavedLocationEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final String _tmpCountry;
            if (_cursor.isNull(_cursorIndexOfCountry)) {
              _tmpCountry = null;
            } else {
              _tmpCountry = _cursor.getString(_cursorIndexOfCountry);
            }
            final String _tmpRegion;
            if (_cursor.isNull(_cursorIndexOfRegion)) {
              _tmpRegion = null;
            } else {
              _tmpRegion = _cursor.getString(_cursorIndexOfRegion);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final long _tmpLastUsedAt;
            _tmpLastUsedAt = _cursor.getLong(_cursorIndexOfLastUsedAt);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new SavedLocationEntity(_tmpId,_tmpName,_tmpLatitude,_tmpLongitude,_tmpCountry,_tmpRegion,_tmpIsFavorite,_tmpLastUsedAt,_tmpCreatedAt);
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

  @Override
  public Flow<Boolean> observeIsFavorite(final double lat, final double lon) {
    final String _sql = "SELECT isFavorite FROM saved_locations WHERE ABS(latitude - ?) < 0.005 AND ABS(longitude - ?) < 0.005 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindDouble(_argIndex, lat);
    _argIndex = 2;
    _statement.bindDouble(_argIndex, lon);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"saved_locations"}, new Callable<Boolean>() {
      @Override
      @Nullable
      public Boolean call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Boolean _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp == null ? null : _tmp != 0;
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
