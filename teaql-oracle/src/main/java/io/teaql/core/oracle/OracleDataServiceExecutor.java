package io.teaql.core.oracle;

import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.sql.portable.PortableSQLRepository;
import io.teaql.core.sql.portable.TeaQLDatabase;
import io.teaql.dataservice.sql.SqlDataServiceExecutor;
import io.teaql.dataservice.sql.SqlExecutionAdapter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OracleDataServiceExecutor extends SqlDataServiceExecutor {

    public OracleDataServiceExecutor(String name, SqlExecutionAdapter executionAdapter) {
        super(name, executionAdapter);
        this.debugDatabaseKind = "oracle";
        this.dialect = new io.teaql.core.sql.dialect.OracleDialect();
    }

    @Override
    public void ensureSchema(UserContext context, io.teaql.core.SchemaExecutor.Invocation invocation) {
        io.teaql.core.SchemaExecutor.Invocation.requireContextOwned(invocation);
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
                // Translate types for Oracle
                String translated = sql.replace("<max>", "255")
                        .replaceAll("(?i)\\bBIGINT\\b", "NUMBER(19)")
                        .replaceAll("(?i)\\bDATETIME\\b", "TIMESTAMP")
                        .replaceAll("(?i)\\bBOOLEAN\\b", "NUMBER(1)")
                        .replaceAll("(?i)\\bDOUBLE\\b", "BINARY_DOUBLE");
                getExecutionAdapter().execute(translated);
            }

            @Override
            public void executeInTransaction(Runnable action) {
                action.run();
            }

            @Override
            public List<Map<String, Object>> getTableColumns(String tableName) {
                String sql = "SELECT column_name, data_type FROM all_tab_columns WHERE table_name = UPPER(:tableName)";
                return getExecutionAdapter().queryForList(sql, Collections.singletonMap("tableName", tableName));
            }
        };

        for (EntityDescriptor descriptor : descriptors) {
            PortableSQLRepository repository = new PortableSQLRepository(descriptor, dbAdapter, null);
            repository.setDialect(this.dialect);
            repository.ensureSchema(context);
        }
    }
}
