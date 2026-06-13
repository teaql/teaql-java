package io.teaql.core.postgres;

import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.sql.portable.PortableSQLRepository;
import io.teaql.core.sql.portable.TeaQLDatabase;
import io.teaql.dataservice.sql.SqlDataServiceExecutor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PostgresDataServiceExecutor extends SqlDataServiceExecutor {

    public PostgresDataServiceExecutor(String name, io.teaql.dataservice.sql.SqlExecutionAdapter executionAdapter) {
        super(name, executionAdapter);
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
                String sql = "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = :tableName AND table_schema = 'public'";
                return getExecutionAdapter().queryForList(sql, Collections.singletonMap("tableName", tableName.toLowerCase()));
            }
        };

        for (EntityDescriptor descriptor : descriptors) {
            PortableSQLRepository repository = new PortableSQLRepository(descriptor, dbAdapter, null);
            repository.ensureSchema(ctx);
        }
    }
}
