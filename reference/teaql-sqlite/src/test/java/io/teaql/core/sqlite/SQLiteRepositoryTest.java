package io.teaql.core.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class SQLiteRepositoryTest {

    @Test
    void wrapsJdbcUrlDataSourceWithSingleConnectionDataSource() throws Exception {
        DataSource wrapped = wrapDataSource(new JdbcUrlDataSource("jdbc:sqlite:/tmp/teaql.db"));

        assertTrue(wrapped instanceof SingleConnectionDataSource);
        assertUrl(wrapped, "jdbc:sqlite:/tmp/teaql.db");
    }

    @Test
    void wrapsUrlDataSourceWithSingleConnectionDataSource() throws Exception {
        DataSource wrapped = wrapDataSource(new UrlDataSource("jdbc:sqlite:/tmp/teaql-url.db"));

        assertTrue(wrapped instanceof SingleConnectionDataSource);
        assertUrl(wrapped, "jdbc:sqlite:/tmp/teaql-url.db");
    }

    @Test
    void keepsExistingSingleConnectionDataSource() throws Exception {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:sqlite:/tmp/teaql.db");

        assertSame(dataSource, wrapDataSource(dataSource));
    }

    @Test
    void keepsNonSqliteDataSource() throws Exception {
        DataSource dataSource = new JdbcUrlDataSource("jdbc:mysql://localhost/teaql");

        assertSame(dataSource, wrapDataSource(dataSource));
    }

    @Test
    void updatesIdSpaceAndBusinessTableInTransaction() throws Exception {
        Path db = Files.createTempFile("teaql-sqlite-transaction", ".db");
        DataSource dataSource =
                wrapDataSource(new DriverManagerDataSource("jdbc:sqlite:" + db));
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute(
                "CREATE TABLE teaql_id_space (type_name varchar(100) PRIMARY KEY, current_level bigint)");
        jdbcTemplate.execute("CREATE TABLE sample_entity (id bigint PRIMARY KEY, name varchar(100))");

        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        TransactionTemplate outerTransaction = new TransactionTemplate(transactionManager);
        TransactionTemplate prepareIdTransaction = new TransactionTemplate(transactionManager);
        prepareIdTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        outerTransaction.executeWithoutResult(
                outerStatus -> {
                    prepareIdTransaction.executeWithoutResult(
                            prepareIdStatus ->
                                    jdbcTemplate.update(
                                            "INSERT INTO teaql_id_space(type_name, current_level) VALUES (?, ?)",
                                            "sample_entity",
                                            1L));

                    jdbcTemplate.update(
                            "INSERT INTO sample_entity(id, name) VALUES (?, ?)",
                            1L,
                            "created in transaction");
                });

        assertEquals(
                1L,
                jdbcTemplate.queryForObject(
                        "SELECT current_level FROM teaql_id_space WHERE type_name = ?",
                        Long.class,
                        "sample_entity"));
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM sample_entity WHERE id = ?", Integer.class, 1L));
    }

    private static DataSource wrapDataSource(DataSource dataSource) throws Exception {
        Method method = SQLiteRepository.class.getDeclaredMethod("wrapDataSource", DataSource.class);
        method.setAccessible(true);
        return (DataSource) method.invoke(null, dataSource);
    }

    private static void assertUrl(DataSource dataSource, String expectedUrl) throws Exception {
        Field field = SingleConnectionDataSource.class.getDeclaredField("url");
        field.setAccessible(true);
        assertEquals(expectedUrl, field.get(dataSource));
    }

    public static class JdbcUrlDataSource extends UnsupportedDataSource {
        private final String jdbcUrl;

        JdbcUrlDataSource(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }
    }

    public static class UrlDataSource extends UnsupportedDataSource {
        private final String url;

        UrlDataSource(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

    public abstract static class UnsupportedDataSource implements DataSource {
        @Override
        public Connection getConnection() throws SQLException {
            throw new SQLFeatureNotSupportedException("Not implemented");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            throw new SQLFeatureNotSupportedException("Not implemented");
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            throw new SQLFeatureNotSupportedException("Not implemented");
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
            throw new SQLFeatureNotSupportedException("Not implemented");
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            throw new SQLFeatureNotSupportedException("Not implemented");
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            throw new SQLFeatureNotSupportedException("Not implemented");
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException("Not implemented");
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("Cannot unwrap");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }
}
