package io.teaql.android;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import io.teaql.dataservice.sql.SqlExecutionAdapter;
import io.teaql.dataservice.sql.SqlRowMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class AndroidSQLiteExecutionAdapter implements SqlExecutionAdapter {

    private final SQLiteDatabase db;

    public AndroidSQLiteExecutionAdapter(SQLiteDatabase db) {
        this.db = db;
    }

    private String[] convertArgsToStrings(Object[] args) {
        if (args == null) return new String[0];
        String[] strArgs = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            strArgs[i] = args[i] == null ? null : String.valueOf(args[i]);
        }
        return strArgs;
    }

    @Override
    public <T> List<T> query(String sql, Map<String, Object> params, SqlRowMapper<T> rowMapper) {
        throw new UnsupportedOperationException("Named parameters not supported yet");
    }

    @Override
    public <T> Stream<T> queryForStream(String sql, Map<String, Object> params, SqlRowMapper<T> rowMapper) {
        throw new UnsupportedOperationException("Streaming not supported yet");
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Map<String, Object> params) {
        throw new UnsupportedOperationException("Named parameters not supported yet");
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Object[] params) {
        List<Map<String, Object>> result = new ArrayList<>();
        // Note: rawQuery only supports String bindings. For true multi-type support on Android, 
        // a more complex binding using SQLiteQuery is needed, but this suffices for standard TEAQL.
        try (Cursor cursor = db.rawQuery(sql, convertArgsToStrings(params))) {
            String[] columns = cursor.getColumnNames();
            while (cursor.moveToNext()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 0; i < columns.length; i++) {
                    switch (cursor.getType(i)) {
                        case Cursor.FIELD_TYPE_INTEGER:
                            row.put(columns[i], cursor.getLong(i));
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            row.put(columns[i], cursor.getDouble(i));
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            row.put(columns[i], cursor.getString(i));
                            break;
                        case Cursor.FIELD_TYPE_BLOB:
                            row.put(columns[i], cursor.getBlob(i));
                            break;
                        default:
                            row.put(columns[i], null);
                    }
                }
                result.add(row);
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> queryForMap(String sql, Map<String, Object> params) {
        throw new UnsupportedOperationException("Named parameters not supported yet");
    }

    @Override
    public <T> T queryForObject(String sql, Map<String, Object> params, Class<T> requiredType) {
        throw new UnsupportedOperationException("Named parameters not supported yet");
    }

    @Override
    public void execute(String sql) {
        db.execSQL(sql);
    }

    @Override
    public int update(String sql, Map<String, Object> params) {
        throw new UnsupportedOperationException("Named parameters not supported yet");
    }

    private void bindArgs(SQLiteStatement statement, Object[] params) {
        if (params == null) return;
        for (int i = 0; i < params.length; i++) {
            int index = i + 1;
            Object arg = params[i];
            if (arg == null) {
                statement.bindNull(index);
            } else if (arg instanceof String) {
                statement.bindString(index, (String) arg);
            } else if (arg instanceof Double || arg instanceof Float) {
                statement.bindDouble(index, ((Number) arg).doubleValue());
            } else if (arg instanceof Number) {
                statement.bindLong(index, ((Number) arg).longValue());
            } else if (arg instanceof byte[]) {
                statement.bindBlob(index, (byte[]) arg);
            } else {
                statement.bindString(index, String.valueOf(arg));
            }
        }
    }

    @Override
    public int update(String sql, Object[] params) {
        SQLiteStatement statement = db.compileStatement(sql);
        try {
            bindArgs(statement, params);
            return statement.executeUpdateDelete();
        } finally {
            statement.close();
        }
    }

    @Override
    public int[] batchUpdate(String sql, List<Object[]> paramsList) {
        SQLiteStatement statement = db.compileStatement(sql);
        int[] results = new int[paramsList.size()];
        db.beginTransaction();
        try {
            for (int i = 0; i < paramsList.size(); i++) {
                statement.clearBindings();
                bindArgs(statement, paramsList.get(i));
                results[i] = statement.executeUpdateDelete();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            statement.close();
        }
        return results;
    }
}
