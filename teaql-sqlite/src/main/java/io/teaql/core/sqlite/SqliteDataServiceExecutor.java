package io.teaql.core.sqlite;

import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.sql.portable.PortableSQLRepository;
import io.teaql.core.sql.portable.TeaQLDatabase;
import io.teaql.dataservice.sql.SqlDataServiceExecutor;
import io.teaql.dataservice.sql.SqlExecutionAdapter;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SqliteDataServiceExecutor extends SqlDataServiceExecutor {

    private final DataSource dataSource;

    public SqliteDataServiceExecutor(String name, SqlExecutionAdapter executionAdapter, DataSource dataSource) {
        super(name, executionAdapter);
        this.dataSource = dataSource;
    }

    @Override
    public void ensureSchema(UserContext ctx) {
        List<EntityDescriptor> descriptors = EntityMetaFactory.get().allEntityDescriptors();

        TeaQLDatabase dbAdapter = new TeaQLDatabase() {
            @Override
            public List<Map<String, Object>> query(String sql, Object[] args) {
                return getExecutionAdapter().queryForList(sql, args);
            }

            @Override
            public int executeUpdate(String sql, Object[] args) {
                return getExecutionAdapter().update(sql, args);
            }

            @Override
            public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
                return getExecutionAdapter().batchUpdate(sql, batchArgs);
            }

            @Override
            public void execute(String sql) {
                getExecutionAdapter().execute(sql.replace("<max>", "255"));
            }

            @Override
            public void execute(io.teaql.core.UserContext ctx, String sql) {
                this.execute(sql);
            }

            @Override
            public void executeInTransaction(Runnable action) {
                action.run(); // For SQLite simplicity in this CLI
            }

            @Override
            public List<Map<String, Object>> getTableColumns(String tableName) {
                try {
                    List<Map<String, Object>> columns = getExecutionAdapter().queryForList("PRAGMA table_info(" + tableName + ")", new Object[0]);
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
            repository.ensureSchema(ctx);
        }
    }
}
