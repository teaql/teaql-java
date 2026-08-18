package io.teaql.android;

import android.database.sqlite.SQLiteDatabase;
import io.teaql.core.UserContext;
import io.teaql.core.TransactionCallback;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.sql.portable.PortableSQLRepository;
import io.teaql.dataservice.sql.SqlDataServiceExecutor;
import io.teaql.dataservice.sql.SqlExecutionAdapter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AndroidSqliteDataServiceExecutor extends SqlDataServiceExecutor {

    private final SQLiteDatabase db;

    public AndroidSqliteDataServiceExecutor(String name, SQLiteDatabase db) {
        super(name, new AndroidSQLiteExecutionAdapter(db));
        this.db = db;
    }

    @Override
    public <T> T executeInTransaction(UserContext context, TransactionCallback<T> action) {
        db.beginTransaction();
        try {
            T result = action.doInTransaction();
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void ensureSchema(UserContext context) {
        List<EntityDescriptor> descriptors = EntityMetaFactory.get().allEntityDescriptors();
        SqlExecutionAdapter adapter = getExecutionAdapter();
        
        io.teaql.core.sql.portable.TeaQLDatabase dbAdapter = new io.teaql.core.sql.portable.TeaQLDatabase() {
            @Override
            public List<Map<String, Object>> query(String sql, Object[] args) {
                return adapter.queryForList(sql, args);
            }

            @Override
            public int executeUpdate(String sql, Object[] args) {
                return adapter.update(sql, args);
            }

            @Override
            public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
                return adapter.batchUpdate(sql, batchArgs);
            }

            @Override
            public void execute(String sql) {
                adapter.execute(sql.replace("<max>", "255"));
            }

            @Override
            public void execute(UserContext context, String sql) {
                this.execute(sql);
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
                try {
                    List<Map<String, Object>> columns = adapter.queryForList("PRAGMA table_info(" + tableName + ")", new Object[0]);
                    for (Map<String, Object> col : columns) {
                        col.put("column_name", col.get("name"));
                    }
                    return columns;
                } catch (Exception e) {
                    return Collections.emptyList();
                }
            }
        };

        for (EntityDescriptor descriptor : descriptors) {
            PortableSQLRepository repository = new PortableSQLRepository(descriptor, dbAdapter, null);
            repository.ensureSchema(context);
        }
    }
}
