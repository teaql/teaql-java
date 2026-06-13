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
                throw new UnsupportedOperationException();
            }

            @Override
            public int executeUpdate(String sql, Object[] args) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void execute(String sql) {
                getExecutionAdapter().execute(sql);
            }

            @Override
            public void executeInTransaction(Runnable action) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<Map<String, Object>> getTableColumns(String tableName) {
                // Return empty so that PortableSQLRepository thinks the table doesn't exist and creates it
                // We could implement PRAGMA table_info here if needed
                return Collections.emptyList();
            }
        };

        for (EntityDescriptor descriptor : descriptors) {
            PortableSQLRepository repository = new PortableSQLRepository(descriptor, dbAdapter, null);
            repository.ensureSchema(ctx);
        }
    }
}
