# Android Developer Guide: TeaQL Portable SQL

This guide explains how to use the `teaql-sql-portable` module to integrate TeaQL into an Android application using native Android SQLite APIs, without any dependency on Spring or Java Database Connectivity (JDBC).

---

## 1. Overview

`teaql-sql-portable` provides a dependency-free, lightweight SQL database provider for TeaQL. In Spring-based environments, TeaQL uses JDBC and Spring JDBC. For Android, `teaql-sql-portable` decouples the database operations by introducing the `TeaQLDatabase` abstraction layer.

Your Android application is responsible for:
1. Creating/managing the native SQLite database via `SQLiteOpenHelper`.
2. Implementing the `TeaQLDatabase` interface to route execution to the native SQLite database.
3. Registering the repository with the TeaQL `UserContext` runtime.

---

## 2. Implementing `TeaQLDatabase` for Android

Since the `teaql-sql-portable` library does not compile against the Android SDK, you must write a simple wrapper implementation of `TeaQLDatabase` inside your Android codebase.

Copy the following class into your Android codebase (e.g. `database/AndroidTeaQLDatabase.java`):

```java
package io.teaql.core.sql.portable; // Adjust package name to your project structure

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import io.teaql.core.sql.portable.TeaQLDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Native Android implementation of TeaQLDatabase wrapping SQLiteDatabase.
 */
public class AndroidTeaQLDatabase implements TeaQLDatabase {
    private final SQLiteDatabase db;

    public AndroidTeaQLDatabase(SQLiteDatabase db) {
        this.db = db;
    }

    @Override
    public List<Map<String, Object>> query(String sql, Object[] args) {
        String[] selectionArgs = null;
        if (args != null && args.length > 0) {
            selectionArgs = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg == null) {
                    selectionArgs[i] = null;
                } else if (arg instanceof Boolean) {
                    selectionArgs[i] = ((Boolean) arg) ? "1" : "0";
                } else {
                    selectionArgs[i] = arg.toString();
                }
            }
        }

        List<Map<String, Object>> results = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(sql, selectionArgs)) {
            if (cursor != null && cursor.moveToFirst()) {
                String[] columnNames = cursor.getColumnNames();
                do {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 0; i < columnNames.length; i++) {
                        String colName = columnNames[i];
                        if (cursor.isNull(i)) {
                            row.put(colName, null);
                        } else {
                            int type = cursor.getType(i);
                            switch (type) {
                                case Cursor.FIELD_TYPE_INTEGER:
                                    row.put(colName, cursor.getLong(i));
                                    break;
                                case Cursor.FIELD_TYPE_FLOAT:
                                    row.put(colName, cursor.getDouble(i));
                                    break;
                                case Cursor.FIELD_TYPE_STRING:
                                    row.put(colName, cursor.getString(i));
                                    break;
                                case Cursor.FIELD_TYPE_BLOB:
                                    row.put(colName, cursor.getBlob(i));
                                    break;
                                default:
                                    row.put(colName, cursor.getString(i));
                            }
                        }
                    }
                    results.add(row);
                } while (cursor.moveToNext());
            }
        }
        return results;
    }

    @Override
    public int executeUpdate(String sql, Object[] args) {
        try (SQLiteStatement stmt = db.compileStatement(sql)) {
            bindArgs(stmt, args);
            return stmt.executeUpdateDelete();
        }
    }

    @Override
    public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
        int[] results = new int[batchArgs.size()];
        try (SQLiteStatement stmt = db.compileStatement(sql)) {
            for (int i = 0; i < batchArgs.size(); i++) {
                stmt.clearBindings();
                bindArgs(stmt, batchArgs.get(i));
                results[i] = stmt.executeUpdateDelete();
            }
        }
        return results;
    }

    @Override
    public void execute(String sql) {
        db.execSQL(sql);
    }

    @Override
    public void executeInTransaction(Runnable action) {
        db.beginTransaction();
        try {
            action.run();
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public List<Map<String, Object>> getTableColumns(String tableName) {
        List<Map<String, Object>> columns = new ArrayList<>();
        // Sanitize input to prevent SQL injection (table names cannot be bound as parameters)
        String sanitizedTable = tableName.replaceAll("[^a-zA-Z0-9_]", "");
        String sql = "PRAGMA table_info(" + sanitizedTable + ")";
        try (Cursor cursor = db.rawQuery(sql, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex("name");
                if (nameIndex != -1) {
                    do {
                        Map<String, Object> col = new HashMap<>();
                        col.put("column_name", cursor.getString(nameIndex));
                        columns.add(col);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            // Table might not exist yet; return empty list
        }
        return columns;
    }

    private void bindArgs(SQLiteStatement stmt, Object[] args) {
        if (args == null) return;
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            int index = i + 1;
            if (arg == null) {
                stmt.bindNull(index);
            } else if (arg instanceof Boolean) {
                stmt.bindLong(index, ((Boolean) arg) ? 1 : 0);
            } else if (arg instanceof Long || arg instanceof Integer || arg instanceof Short || arg instanceof Byte) {
                stmt.bindLong(index, ((Number) arg).longValue());
            } else if (arg instanceof Double || arg instanceof Float) {
                stmt.bindDouble(index, ((Number) arg).doubleValue());
            } else if (arg instanceof byte[]) {
                stmt.bindBlob(index, (byte[]) arg);
            } else {
                stmt.bindString(index, arg.toString());
            }
        }
    }
}
```

---

## 3. Initialization & Wiring

To use TeaQL in your Android application, wire up the repository and register the data services in your custom `UserContext` initialization path.

### Step 3.1: Open native SQLite Database
Usually, you open the database via a custom `SQLiteOpenHelper`:

```java
public class MyDbHelper extends SQLiteOpenHelper {
    public MyDbHelper(Context context) {
        super(context, "my_app.db", null, 1);
    }
    // ... onCreate, onUpgrade
}
```

### Step 3.2: Initialize TeaQL Components
Once you have the database instance, wrap it and configure the TeaQL data service:

```java
// Open or get database helper
MyDbHelper helper = new MyDbHelper(context);
SQLiteDatabase sqliteDb = helper.getWritableDatabase();

// 1. Wrap with TeaQLDatabase
TeaQLDatabase teaqlDb = new AndroidTeaQLDatabase(sqliteDb);

// 2. Initialize the PortableSQLDataService
PortableSQLDataService dataService = new PortableSQLDataService(teaqlDb);

// 3. Register entity repositories with the data service
// Note: These descriptors and entity classes are generated by the TeaQL Compiler
EntityDescriptor taskDescriptor = Task.DESCRIPTOR; // example generated descriptor
PortableSQLRepository taskRepo = new PortableSQLRepository(taskDescriptor, teaqlDb);

dataService.registerRepository(taskRepo);
```

### Step 3.3: Set Up UserContext
To make the database operations accessible through the generated API, wire `PortableSQLDataService` as the data store on the user's transaction/execution context:

```java
UserContext userContext = new UserContext();
userContext.setDataStore(dataService);
```

---

## 4. Querying and Mutations

Now, you can execute standard TeaQL generated request models natively:

### Example Query
```java
// Execute a query to find all active tasks
List<Task> tasks = Q.tasks()
    .filterByStatus("ACTIVE")
    .purpose("Show user's dashboard")
    .executeForList(userContext);
```

### Example Mutation
```java
// Mutate (Save/Update/Delete) entities
Task task = new Task();
task.setName("Integrate TeaQL");
task.setStatus("ACTIVE");

// This updates state, versions, and persists via the PortableSQLDataService
task.save(userContext);
```

---

## 5. Best Practices for Android

### Thread Safety & Threading Model
Android's `SQLiteDatabase` handles multi-threaded accesses internally using locks, but executing heavy queries on the main thread will cause application UI freeze/ANR (Application Not Responding).
* Always dispatch TeaQL DB executions (queries/mutations) using a background thread pool, `AsyncTask`, or Kotlin Coroutines (`Dispatchers.IO`).
* Ensure you reuse a single, shared `SQLiteOpenHelper` instance across the entire application to avoid thread conflict and database locking exceptions.

### Schema Generation and Upgrades
* Use `dataService.ensureSchema(userContext, "Task")` during application startup or within `SQLiteOpenHelper.onCreate()` to automatically compile and create tables for all registered repositories if they do not exist.
* For schema migrations, either leverage `dataService.ensureSchema(userContext, "Task")` (which automatically detects missing tables/columns) or manage migrations using Android's native `onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)` override.
