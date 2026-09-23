package io.teaql.core.postgres;

import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.sql.portable.PortableSQLRepository;
import io.teaql.core.sql.portable.TeaQLDatabase;
import io.teaql.dataservice.sql.SqlDataServiceExecutor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PostgresDataServiceExecutor extends SqlDataServiceExecutor {

    public PostgresDataServiceExecutor(String name, io.teaql.dataservice.sql.SqlExecutionAdapter executionAdapter) {
        super(name, executionAdapter);
    }

    @Override
    public void ensureSchema(UserContext context, io.teaql.core.SchemaExecutor.Invocation invocation) {
        io.teaql.core.SchemaExecutor.Invocation.requireContextOwned(invocation);
        List<EntityDescriptor> descriptors = EntityMetaFactory.requireFrom(context).allEntityDescriptors();

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
                getExecutionAdapter().execute(sql);
            }

            @Override
            public void executeInTransaction(Runnable action) {
                action.run();
            }

            @Override
            public List<Map<String, Object>> getTableColumns(String tableName) {
                String sql = "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = ? AND table_schema = 'public'";
                return getExecutionAdapter().queryForList(sql, new Object[] {tableName.toLowerCase()});
            }

            @Override
            public Optional<Boolean> indexExists(
                    UserContext context, String tableName, String indexName) {
                List<Map<String, Object>> rows = getExecutionAdapter().queryForList(
                        "SELECT 1 AS present FROM pg_indexes "
                                + "WHERE schemaname = current_schema() AND tablename = ? "
                                + "AND indexname = ? LIMIT 1",
                        new Object[] {tableName.toLowerCase(), indexName.toLowerCase()});
                return Optional.of(!rows.isEmpty());
            }
        };

        for (EntityDescriptor descriptor : descriptors) {
            PortableSQLRepository repository = new PortableSQLRepository(descriptor, dbAdapter, null);
            repository.ensurePhysicalSchema(context);
        }
    }
}
