package io.teaql.core.sqlite;

import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.sql.portable.PortableSQLRepository;
import io.teaql.core.sql.portable.TeaQLDatabase;
import io.teaql.dataservice.sql.SqlDataServiceExecutor;
import io.teaql.dataservice.sql.SqlExecutionAdapter;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import org.sqlite.Function;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SqliteDataServiceExecutor extends SqlDataServiceExecutor {

    private final DataSource dataSource;
    private boolean soundexInstalled;

    public SqliteDataServiceExecutor(String name, SqlExecutionAdapter executionAdapter, DataSource dataSource) {
        super(name, executionAdapter);
        this.debugDatabaseKind = "sqlite";
        this.dataSource = dataSource;
    }

    @Override
    public void ensureSchema(UserContext context, io.teaql.core.SchemaExecutor.Invocation invocation) {
        io.teaql.core.SchemaExecutor.Invocation.requireContextOwned(invocation);
        ensureSoundexOnEveryConnection();
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
            public void execute(io.teaql.core.UserContext context, String sql) {
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
            repository.ensureSchema(context);
        }
    }

    private synchronized void ensureSoundexOnEveryConnection() {
        if (soundexInstalled) return;
        if (!(getExecutionAdapter() instanceof JdbcSqlExecutor jdbc)) {
            throw new IllegalStateException("SQLite soundex registration requires JdbcSqlExecutor");
        }
        jdbc.addConnectionInitializer(connection -> {
            try {
                java.sql.Connection sqliteConnection = connection;
                if (!(sqliteConnection instanceof org.sqlite.SQLiteConnection)) {
                    sqliteConnection = connection.unwrap(org.sqlite.SQLiteConnection.class);
                }
                Function.create(sqliteConnection, "soundex", new Function() {
                    @Override protected void xFunc() throws java.sql.SQLException {
                        result(sqliteCompatibleSoundex(value_text(0)));
                    }
                }, 1, Function.FLAG_DETERMINISTIC);
            } catch (java.sql.SQLException exception) {
                throw new IllegalStateException("Unable to register SQLite soundex function", exception);
            }
        });
        soundexInstalled = true;
    }

    static String sqliteCompatibleSoundex(String value) {
        if (value == null) return "?000";
        String letters = value.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z]", "");
        if (letters.isEmpty()) return "?000";
        StringBuilder result = new StringBuilder().append(letters.charAt(0));
        char previous = soundexCode(letters.charAt(0));
        for (int index = 1; index < letters.length() && result.length() < 4; index++) {
            char current = soundexCode(letters.charAt(index));
            if (current != '0' && current != previous) result.append(current);
            previous = current;
        }
        while (result.length() < 4) result.append('0');
        return result.toString();
    }

    private static char soundexCode(char value) {
        return switch (value) {
            case 'B', 'F', 'P', 'V' -> '1';
            case 'C', 'G', 'J', 'K', 'Q', 'S', 'X', 'Z' -> '2';
            case 'D', 'T' -> '3'; case 'L' -> '4'; case 'M', 'N' -> '5'; case 'R' -> '6';
            default -> '0';
        };
    }
}
