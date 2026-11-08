package io.teaql.core.duck;

import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.sql.portable.PortableSQLRepository;
import io.teaql.core.sql.portable.TeaQLDatabase;
import io.teaql.dataservice.sql.SqlDataServiceExecutor;
import io.teaql.dataservice.sql.SqlExecutionAdapter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DuckDataServiceExecutor extends SqlDataServiceExecutor {

    public DuckDataServiceExecutor(String name, SqlExecutionAdapter executionAdapter) {
        super(name, executionAdapter);
        this.debugDatabaseKind = "duckdb";
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
                getExecutionAdapter().execute(sql.replace("<max>", "255"));
            }

            @Override
            public void executeInTransaction(Runnable action) {
                action.run();
            }

            @Override
            public List<Map<String, Object>> getTableColumns(String tableName) {
                String sql = "SELECT column_name, data_type, data_type AS type_name, "
                        + "character_maximum_length AS column_size, "
                        + "numeric_precision, numeric_scale AS decimal_digits, "
                        + "CASE WHEN is_nullable = 'YES' THEN 1 ELSE 0 END AS nullable "
                        + "FROM information_schema.columns WHERE table_name = ?";
                return getExecutionAdapter().queryForList(sql, new Object[] {tableName});
            }

            @Override
            public Optional<Boolean> canonicalRelationIndexMatches(UserContext caller,
                    String tableName, String indexName, List<String> orderedColumns) {
                List<Map<String, Object>> matches = getExecutionAdapter().queryForList(
                        "SELECT table_name, is_unique, sql FROM duckdb_indexes() "
                                + "WHERE index_name = ?", new Object[] {indexName});
                if (matches.isEmpty()) return Optional.of(false);
                if (matches.size() != 1) {
                    throw new IllegalStateException("DuckDB canonical relation index name is ambiguous");
                }
                Map<String, Object> index = matches.get(0);
                if (!tableName.equals(index.get("table_name"))
                        || !Boolean.FALSE.equals(index.get("is_unique"))
                        || !sameSimpleOrderedColumns(index.get("sql"), orderedColumns)) {
                    throw new IllegalStateException("DuckDB canonical relation index has incompatible shape");
                }
                return Optional.of(true);
            }
        };

        for (EntityDescriptor descriptor : descriptors) {
            PortableSQLRepository repository = new PortableSQLRepository(descriptor, dbAdapter, null);
            repository.ensurePhysicalSchema(context);
        }
    }

    private static boolean sameSimpleOrderedColumns(Object definition, List<String> expected) {
        if (!(definition instanceof String ddl)) return false;
        int open = ddl.indexOf('(');
        int close = ddl.lastIndexOf(')');
        if (open < 0 || close <= open || !ddl.regionMatches(true, 0, "CREATE INDEX ", 0, 13)) {
            return false;
        }
        String[] actual = ddl.substring(open + 1, close).split(",", -1);
        if (actual.length != expected.size()) return false;
        for (int i = 0; i < actual.length; i++) {
            String column = actual[i].trim().replaceFirst("(?i)\\s+(ASC|DESC)$", "");
            if (column.startsWith("\"") && column.endsWith("\"") && column.length() >= 2) {
                column = column.substring(1, column.length() - 1).replace("\"\"", "\"");
            }
            if (!column.equals(expected.get(i))) return false;
        }
        return true;
    }
}
