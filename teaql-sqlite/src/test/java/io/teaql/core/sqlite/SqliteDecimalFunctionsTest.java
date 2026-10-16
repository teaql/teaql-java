package io.teaql.core.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import io.teaql.core.BaseEntity;
import io.teaql.core.BaseRequest;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

public class SqliteDecimalFunctionsTest {

    public static class DecimalRow extends BaseEntity {
        private BigDecimal amount;

        @Override public String typeName() { return "DecimalRow"; }

        @Override public void __internalSet(String property, Object value) {
            if ("amount".equals(property)) amount = (BigDecimal) value;
            else super.__internalSet(property, value);
        }

        @Override public Object __internalGet(String property) {
            return "amount".equals(property) ? amount : super.__internalGet(property);
        }
    }

    public static class DecimalRequest extends BaseRequest<DecimalRow> {
        public DecimalRequest() { super(DecimalRow.class); }
        @Override public String getTypeName() { return "DecimalRow"; }
        public DecimalRequest comment(String comment) {
            super.internalComment(comment);
            return this;
        }

        public DecimalRequest amountIs(BigDecimal amount) {
            appendSearchCriteria(createBasicSearchCriteria("amount", Operator.EQUAL, amount));
            return this;
        }
    }

    @Test
    public void exactStorageComparisonAndSumSurviveJdbcDecimalBinding() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SqliteDecimalFunctions.register(connection);
            SqliteDecimalFunctions.register(connection);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE amounts (value DECIMAL_TEXT COLLATE TEAQL_DECIMAL "
                        + "CHECK (value IS NULL OR teaql_decimal_valid(value)))");
            }
            for (String value : new String[] {"3.50", "2", "10", "123456789012.3456789", "-0.1"}) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO amounts (value) VALUES (?)")) {
                    statement.setBigDecimal(1, new BigDecimal(value));
                    statement.executeUpdate();
                }
            }
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(
                         "SELECT typeof(value), value FROM amounts ORDER BY value LIMIT 1")) {
                rows.next();
                assertEquals("text", rows.getString(1));
                assertEquals("-0.1", rows.getString(2));
            }
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(
                         "SELECT value FROM amounts WHERE value = '3.5'")) {
                rows.next();
                assertEquals("3.50", rows.getString(1));
            }
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(
                         "SELECT teaql_decimal_sum(value), typeof(sum(value)) FROM amounts")) {
                rows.next();
                assertEquals("123456789027.7456789", rows.getString(1));
                assertEquals("real", rows.getString(2));
            }
            try (Statement statement = connection.createStatement()) {
                assertThrows(java.sql.SQLException.class,
                        () -> statement.executeUpdate("INSERT INTO amounts (value) VALUES ('not-a-decimal')"));
                assertThrows(java.sql.SQLException.class,
                        () -> statement.executeUpdate("INSERT INTO amounts (value) VALUES (X'31')"));
            }
        }
    }

    @Test
    public void contextEnsureSchemaCreatesExactDecimalStorageAndRepeats() throws Exception {
        Path file = Files.createTempFile("teaql-java-exact-decimal-", ".db");
        try {
            SQLiteDataSource dataSource = new SQLiteDataSource();
            dataSource.setUrl("jdbc:sqlite:" + file);
            JdbcSqlExecutor jdbc = new JdbcSqlExecutor(dataSource);
            SqliteDataServiceExecutor provider = new SqliteDataServiceExecutor(
                    "sqlite", jdbc, dataSource);
            SQLEntityDescriptor entity = new SQLEntityDescriptor();
            entity.setType("DecimalRow");
            entity.setTargetType(DecimalRow.class);
            entity.setEntitySupplier(DecimalRow::new);
            entity.setDataService("sqlite");
            ((GenericSQLProperty) entity.addSimpleProperty("id", Long.class))
                    .setColumnType("BIGINT");
            ((GenericSQLProperty) entity.addSimpleProperty("version", Long.class))
                    .setColumnType("BIGINT");
            ((GenericSQLProperty) entity.addSimpleProperty("bucket", String.class))
                    .setColumnType("VARCHAR(20)");
            ((GenericSQLProperty) entity.addSimpleProperty("amount", BigDecimal.class))
                    .setColumnType("NUMERIC(19,7)");
            SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
            metadata.register(entity);
            DefaultUserContext context = new DefaultUserContext(TeaQLRuntime.builder()
                    .metadata(metadata).dataService("sqlite", provider).build());

            context.ensureSchema();
            context.ensureSchema();
            String ddl = String.valueOf(jdbc.queryForList(
                    "SELECT sql FROM sqlite_master WHERE type='table' AND name='decimal_row_data'",
                    new Object[0]).get(0).get("sql"));
            assertTrue(ddl, ddl.contains("DECIMAL_TEXT(19,7) COLLATE TEAQL_DECIMAL"));
            assertTrue(ddl, ddl.contains("CHECK (amount IS NULL OR teaql_decimal_valid(amount))"));
            jdbc.update("INSERT INTO decimal_row_data (id, version, bucket, amount) VALUES (?, ?, ?, ?)",
                    new Object[] {1L, 1L, "A", new BigDecimal("3.50")});
            assertEquals("3.50", jdbc.queryForList(
                    "SELECT amount FROM decimal_row_data WHERE amount = ?", new Object[] {
                            new BigDecimal("3.5")}).get(0).get("amount"));
            DecimalRequest query = new DecimalRequest();
            query.sum("total", "amount");
            Object total = query.comment("calculate exact order amount")
                    .purpose("verify modeled Q aggregation")
                    .aggregation(context).getData().get(0).getValues().values().iterator().next();
            assertEquals(new BigDecimal("3.50"), total);

            jdbc.update("INSERT INTO decimal_row_data (id, version, bucket, amount) VALUES (?, ?, ?, ?)",
                    new Object[] {2L, 1L, "A", new BigDecimal("10.25")});
            jdbc.update("INSERT INTO decimal_row_data (id, version, bucket, amount) VALUES (?, ?, ?, ?)",
                    new Object[] {3L, 1L, "B", new BigDecimal("123456789012.3456789")});
            DecimalRequest selected = new DecimalRequest();
            for (String property : new String[] {"id", "version", "amount"}) {
                selected.selectProperty(property);
            }
            var loaded = selected.amountIs(new BigDecimal("3.5"))
                    .comment("load exact Decimal row")
                    .purpose("verify typed Q and E Decimal hydration")
                    .executeForList(context);
            assertEquals(1, loaded.size());
            assertEquals(new BigDecimal("3.50"), loaded.get(0).getProperty("amount"));

            DecimalRequest grouped = new DecimalRequest();
            grouped.groupBy("bucket");
            grouped.sum("total", "amount");
            grouped.min("smallest", "amount");
            grouped.max("largest", "amount");
            grouped.countProperty("items", "amount");
            var rows = grouped.comment("aggregate exact Decimal buckets")
                    .purpose("verify grouped monetary totals").aggregation(context).toList();
            assertEquals(2, rows.size());
            var first = rows.stream().filter(row -> "A".equals(row.get("bucket"))).findFirst().orElseThrow();
            assertEquals(new BigDecimal("13.75"), first.get("total"));
            assertEquals(new BigDecimal("3.50"), first.get("smallest"));
            assertEquals(new BigDecimal("10.25"), first.get("largest"));
            assertEquals(2L, ((Number) first.get("items")).longValue());
            var second = rows.stream().filter(row -> "B".equals(row.get("bucket"))).findFirst().orElseThrow();
            assertEquals(new BigDecimal("123456789012.3456789"), second.get("total"));

            DecimalRequest unsupported = new DecimalRequest();
            unsupported.avg("average", "amount");
            assertThrows(IllegalArgumentException.class, () -> unsupported
                    .comment("reject approximate Decimal average")
                    .purpose("prevent silent financial rounding").aggregation(context));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void existingDecimalTablesWithoutTheFullContractFailClosed() throws Exception {
        String[] invalidDefinitions = {
                "amount NUMERIC(19,7)",
                "amount DECIMAL_TEXT COLLATE TEAQL_DECIMAL",
                "amount DECIMAL_TEXT CHECK (amount IS NULL OR teaql_decimal_valid(amount))",
                "amount DECIMAL_TEXT COLLATE TEAQL_DECIMAL "
                        + "DEFAULT 'CHECK (amount IS NULL OR teaql_decimal_valid(amount))'",
                "amount DECIMAL_TEXT COLLATE TEAQL_DECIMAL "
                        + "/* CHECK (amount IS NULL OR teaql_decimal_valid(amount)) */"
        };
        for (String definition : invalidDefinitions) {
            Path file = Files.createTempFile("teaql-java-decimal-legacy-", ".db");
            try {
                SQLiteDataSource dataSource = new SQLiteDataSource();
                dataSource.setUrl("jdbc:sqlite:" + file);
                JdbcSqlExecutor jdbc = new JdbcSqlExecutor(dataSource);
                jdbc.addConnectionInitializer(connection -> {
                    try {
                        SqliteDecimalFunctions.register(connection);
                    } catch (java.sql.SQLException failure) {
                        throw new IllegalStateException(failure);
                    }
                });
                jdbc.execute("CREATE TABLE amounts (" + definition + ")");
                IllegalStateException failure = assertThrows(IllegalStateException.class,
                        () -> SqliteDecimalSchemaVerifier.verify(jdbc, "amounts", "amount"));
                assertTrue(failure.getMessage(), failure.getMessage().contains("incompatible exact Decimal"));
            } finally {
                Files.deleteIfExists(file);
            }
        }
    }

    @Test
    public void contextEnsureSchemaDoesNotAdoptLegacyNumericStorage() throws Exception {
        Path file = Files.createTempFile("teaql-java-decimal-context-legacy-", ".db");
        try {
            SQLiteDataSource dataSource = new SQLiteDataSource();
            dataSource.setUrl("jdbc:sqlite:" + file);
            JdbcSqlExecutor jdbc = new JdbcSqlExecutor(dataSource);
            String ddl = "CREATE TABLE decimal_row_data (id BIGINT NOT NULL PRIMARY KEY, "
                    + "version BIGINT, bucket VARCHAR(20), amount NUMERIC(19,7))";
            jdbc.execute(ddl);

            SQLEntityDescriptor entity = new SQLEntityDescriptor();
            entity.setType("DecimalRow");
            entity.setTargetType(DecimalRow.class);
            entity.setEntitySupplier(DecimalRow::new);
            entity.setDataService("sqlite");
            ((GenericSQLProperty) entity.addSimpleProperty("id", Long.class)).setColumnType("BIGINT");
            ((GenericSQLProperty) entity.addSimpleProperty("version", Long.class)).setColumnType("BIGINT");
            ((GenericSQLProperty) entity.addSimpleProperty("bucket", String.class))
                    .setColumnType("VARCHAR(20)");
            ((GenericSQLProperty) entity.addSimpleProperty("amount", BigDecimal.class))
                    .setColumnType("NUMERIC(19,7)");
            SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
            metadata.register(entity);
            DefaultUserContext context = new DefaultUserContext(TeaQLRuntime.builder()
                    .metadata(metadata)
                    .dataService("sqlite", new SqliteDataServiceExecutor("sqlite", jdbc, dataSource))
                    .build());

            assertThrows(RuntimeException.class, context::ensureSchema);
            assertEquals(ddl, jdbc.queryForList(
                    "SELECT sql FROM sqlite_master WHERE type='table' AND name='decimal_row_data'",
                    new Object[0]).get(0).get("sql"));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
