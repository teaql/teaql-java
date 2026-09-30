package io.teaql.core.sql.portable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/** Exercises the portable optimistic allocator through independent SQLite connections. */
public class SqliteIdSpaceConcurrencyTest {

    @Test
    public void independentInstancesAllocateUniqueIdsAndRespectBootstrapFloor() throws Exception {
        Path databaseFile = Files.createTempFile("teaql-id-space-", ".db");
        try {
            String url = "jdbc:sqlite:" + databaseFile;
            SqliteDatabase database = new SqliteDatabase(url);
            IdSpaceIdGenerator initializer = new IdSpaceIdGenerator(database);
            initializer.ensureIdSpaceTable();
            initializer.ensureFloor("SchoolType", 1002L);
            assertEquals(
                    1003L,
                    new IdSpaceIdGenerator(new SqliteDatabase(url)).nextId("SchoolType"));

            int workers = 4;
            int allocationsPerWorker = 20;
            CountDownLatch start = new CountDownLatch(1);
            Set<Long> ids = ConcurrentHashMap.newKeySet();
            var pool = Executors.newFixedThreadPool(workers);
            try {
                List<Future<Void>> futures = new ArrayList<>();
                for (int worker = 0; worker < workers; worker++) {
                    futures.add(pool.submit(new Callable<>() {
                        @Override
                        public Void call() throws Exception {
                            IdSpaceIdGenerator generator = new IdSpaceIdGenerator(
                                    new SqliteDatabase(url));
                            start.await();
                            for (int index = 0; index < allocationsPerWorker; index++) {
                                assertTrue("duplicate ID allocated", ids.add(generator.nextId("Order")));
                            }
                            return null;
                        }
                    }));
                }
                start.countDown();
                for (Future<Void> future : futures) {
                    future.get(30, TimeUnit.SECONDS);
                }
            } finally {
                pool.shutdownNow();
            }

            assertEquals(workers * allocationsPerWorker, ids.size());
            for (long id = 1; id <= workers * allocationsPerWorker; id++) {
                assertTrue("missing ID " + id, ids.contains(id));
            }
            assertEquals(
                    81L,
                    new IdSpaceIdGenerator(new SqliteDatabase(url)).nextId("Order"));
        } finally {
            Files.deleteIfExists(databaseFile);
        }
    }

    private static final class SqliteDatabase implements TeaQLDatabase {
        private final String url;

        private SqliteDatabase(String url) {
            this.url = url;
        }

        private Connection connect() throws Exception {
            Connection connection = DriverManager.getConnection(url);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA busy_timeout = 5000");
            }
            return connection;
        }

        @Override
        public List<Map<String, Object>> query(String sql, Object[] args) {
            try (Connection connection = connect();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, args);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next()
                            ? List.of(Map.of("current_level", result.getLong("current_level")))
                            : List.of();
                }
            } catch (Exception error) {
                throw new IllegalStateException(error);
            }
        }

        @Override
        public int executeUpdate(String sql, Object[] args) {
            try (Connection connection = connect();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, args);
                return statement.executeUpdate();
            } catch (Exception error) {
                throw new IllegalStateException(error);
            }
        }

        @Override
        public void execute(String sql) {
            try (Connection connection = connect(); Statement statement = connection.createStatement()) {
                statement.execute(sql);
            } catch (Exception error) {
                throw new IllegalStateException(error);
            }
        }

        private static void bind(PreparedStatement statement, Object[] args) throws Exception {
            for (int index = 0; index < args.length; index++) {
                statement.setObject(index + 1, args[index]);
            }
        }

        @Override
        public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
            throw new UnsupportedOperationException("not used by ID allocation");
        }

        @Override
        public void executeInTransaction(Runnable action) {
            throw new UnsupportedOperationException("not used by ID allocation");
        }

        @Override
        public List<Map<String, Object>> getTableColumns(String tableName) {
            throw new UnsupportedOperationException("not used by ID allocation");
        }
    }
}
