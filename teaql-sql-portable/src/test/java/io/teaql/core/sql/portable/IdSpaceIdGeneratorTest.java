package io.teaql.core.sql.portable;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class IdSpaceIdGeneratorTest {
    @Test
    public void parentChildrenAndSiblingShareTheContextOwnedRootIdSpace() {
        var database = new RecordingDatabase();
        database.levels.put("Parent", 5L);
        var generator = new IdSpaceIdGenerator(database);
        var factory = new io.teaql.core.meta.SimpleEntityMetaFactory();
        var parent = new io.teaql.core.meta.EntityDescriptor(); parent.setType("Parent");
        var child = new io.teaql.core.meta.EntityDescriptor(); child.setType("Child"); child.setParent(parent);
        var sibling = new io.teaql.core.meta.EntityDescriptor(); sibling.setType("Sibling"); sibling.setParent(parent);
        factory.register(parent); factory.register(child); factory.register(sibling);
        var context = new io.teaql.runtime.DefaultUserContext(io.teaql.runtime.TeaQLRuntime.builder().metadata(factory).build());
        assertEquals(Long.valueOf(6), generator.generateId(context, new NamedEntity("Child")));
        assertEquals(Long.valueOf(7), generator.generateId(context, new NamedEntity("Parent")));
        assertEquals(Long.valueOf(8), generator.generateId(context, new NamedEntity("Sibling")));
        assertEquals(Map.of("Parent", 8L), database.levels);
    }

    @Test
    public void cyclicAllocationMetadataRejectsBeforeDatabaseAccess() {
        var database = new RecordingDatabase();
        var parent = new io.teaql.core.meta.EntityDescriptor(); parent.setType("Parent");
        var child = new io.teaql.core.meta.EntityDescriptor(); child.setType("Child");
        parent.setParent(child); child.setParent(parent);
        var factory = new io.teaql.core.meta.SimpleEntityMetaFactory(); factory.register(parent); factory.register(child);
        var context = new io.teaql.runtime.DefaultUserContext(io.teaql.runtime.TeaQLRuntime.builder().metadata(factory).build());
        assertThrows(IllegalArgumentException.class, () -> new IdSpaceIdGenerator(database).generateId(context, new NamedEntity("Child")));
        assertTrue(database.queries.isEmpty());
    }

    private static final class NamedEntity extends io.teaql.core.BaseEntity {
        private final String type;
        NamedEntity(String type) { this.type = type; }
        @Override public String typeName() { return type; }
    }


    @Test
    public void incrementsExistingRowWithOptimisticCompareAndSet() {
        RecordingDatabase database = new RecordingDatabase();
        database.levels.put("Order", 41L);

        long allocated = new IdSpaceIdGenerator(database).nextId("Order");

        assertEquals(42L, allocated);
        assertEquals(Long.valueOf(42L), database.levels.get("Order"));
        assertEquals(
                "UPDATE teaql_id_space SET current_level = ? WHERE type_name = ? AND current_level = ?",
                database.updates.get(0));
    }

    @Test
    public void retriesWhenAnotherInstanceWinsFirstRowInsert() {
        RecordingDatabase database = new RecordingDatabase();
        database.failFirstInsertAsRace = true;

        long allocated = new IdSpaceIdGenerator(database).nextId("Invoice");

        assertEquals(2L, allocated);
        assertEquals(Long.valueOf(2L), database.levels.get("Invoice"));
        assertTrue(database.queries.size() >= 3);
    }

    @Test
    public void retriesWhenAnotherInstanceWinsCompareAndSet() {
        RecordingDatabase database = new RecordingDatabase();
        database.levels.put("Payment", 10L);
        database.failFirstCompareAndSetAsRace = true;

        long allocated = new IdSpaceIdGenerator(database).nextId("Payment");

        assertEquals(12L, allocated);
        assertEquals(Long.valueOf(12L), database.levels.get("Payment"));
        assertEquals(2, database.updates.size());
    }

    @Test
    public void failsClosedWhenPersistentIdSpaceTableCannotBeEnsured() {
        RecordingDatabase database = new RecordingDatabase();
        RuntimeException providerFailure = new RuntimeException("permission denied");
        database.executeFailure = providerFailure;

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> new IdSpaceIdGenerator(database, "tenant_id_space").ensureIdSpaceTable());

        assertTrue(failure.getMessage().contains("tenant_id_space"));
        assertTrue(failure.getMessage().contains("connectivity and schema permissions"));
        assertSame(providerFailure, failure.getCause());
    }

    @Test
    public void reportsBoundedAllocationContentionWithTypeAndAttemptCount() {
        RecordingDatabase database = new RecordingDatabase();
        database.levels.put("Order", 7L);
        database.alwaysCompareAndSetConflict = true;

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> new IdSpaceIdGenerator(database).nextId("Order"));

        assertTrue(failure.getMessage().contains("Order"));
        assertTrue(failure.getMessage().contains("100 optimistic-lock attempts"));
        assertEquals(100, database.updates.size());
    }

    @Test
    public void reportsBoundedFloorContentionWithoutMovingFloorBackwards() {
        RecordingDatabase database = new RecordingDatabase();
        database.levels.put("SchoolType", 1L);
        database.alwaysCompareAndSetConflict = true;

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> new IdSpaceIdGenerator(database).ensureFloor("SchoolType", 1002L));

        assertTrue(failure.getMessage().contains("SchoolType"));
        assertTrue(failure.getMessage().contains("100 optimistic-lock attempts"));
        assertEquals(Long.valueOf(1L), database.levels.get("SchoolType"));
        assertEquals(100, database.updates.size());
    }

    private static final class RecordingDatabase implements TeaQLDatabase {
        private final Map<String, Long> levels = new HashMap<>();
        private final List<String> queries = new ArrayList<>();
        private final List<String> updates = new ArrayList<>();
        private boolean failFirstInsertAsRace;
        private boolean failFirstCompareAndSetAsRace;
        private boolean alwaysCompareAndSetConflict;
        private RuntimeException executeFailure;

        @Override
        public List<Map<String, Object>> query(String sql, Object[] args) {
            queries.add(sql);
            Long level = levels.get(String.valueOf(args[0]));
            return level == null
                    ? List.of()
                    : List.of(Map.of("current_level", level));
        }

        @Override
        public int executeUpdate(String sql, Object[] args) {
            if (sql.startsWith("INSERT")) {
                String typeName = String.valueOf(args[0]);
                if (failFirstInsertAsRace) {
                    failFirstInsertAsRace = false;
                    levels.put(typeName, 1L); // Simulates the row committed by the winning instance.
                    throw new RuntimeException("duplicate key");
                }
                levels.put(typeName, ((Number) args[1]).longValue());
                return 1;
            }
            updates.add(sql);
            String typeName = String.valueOf(args[1]);
            long expected = ((Number) args[2]).longValue();
            if (alwaysCompareAndSetConflict) {
                return 0;
            }
            if (failFirstCompareAndSetAsRace) {
                failFirstCompareAndSetAsRace = false;
                levels.put(typeName, expected + 1); // Simulates a competing successful CAS.
                return 0;
            }
            if (!Long.valueOf(expected).equals(levels.get(typeName))) {
                return 0;
            }
            levels.put(typeName, ((Number) args[0]).longValue());
            return 1;
        }

        @Override
        public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void execute(String sql) {
            if (executeFailure != null) throw executeFailure;
        }

        @Override
        public void executeInTransaction(Runnable action) {
            action.run();
        }

        @Override
        public List<Map<String, Object>> getTableColumns(String tableName) {
            return List.of();
        }
    }
}
