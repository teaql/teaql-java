package io.teaql.core.sql.portable;

import io.teaql.core.*;
import io.teaql.core.meta.*;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.runtime.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Actual SQLite and direct provider admission, not a compiler-only probe. */
public class VersionLifecyclePortableSQLTest {
    public static final class Row extends BaseEntity {
        String name;
        void updateName(String value) { handleUpdate("name", name, value); name = value; }
        @Override public Object __internalGet(String field) { return field.equals("name") ? name : super.__internalGet(field); }
        @Override public void __internalSet(String field, Object value) {
            if (field.equals("name")) { markPropertyLoaded(field); name = (String) value; }
            else super.__internalSet(field, value);
        }
    }
    static final class Database extends PortableSQLDatabaseTest.SQLiteTeaQLDatabase {
        int writes, transactions;
        Database() throws Exception { super(); }
        @Override public int executeUpdate(String sql, Object[] args) { writes++; return super.executeUpdate(sql, args); }
        @Override public int[] batchUpdate(String sql, List<Object[]> args) { writes++; return super.batchUpdate(sql, args); }
        @Override public void executeInTransaction(Runnable action) { transactions++; super.executeInTransaction(action); }
    }
    static final class Fixture {
        final Database database = new Database();
        final PortableSQLDataService service;
        final PortableSQLRepository<Row> repository;
        final DefaultUserContext context;
        Fixture() throws Exception {
            var descriptor = new EntityDescriptor();
            descriptor.setType("Row"); descriptor.setTargetType(Row.class); descriptor.setEntitySupplier(Row::new);
            var properties = new ArrayList<PropertyDescriptor>();
            for (String name : List.of("id", "version", "name")) {
                var p = new GenericSQLProperty("version_lifecycle_data", name, name.equals("name") ? "VARCHAR(100)" : "BIGINT");
                p.setName(name); p.setOwner(descriptor);
                p.setType(new SimplePropertyType(name.equals("name") ? String.class : Long.class)); properties.add(p);
            }
            descriptor.setProperties(properties);
            var metadata = new SimpleEntityMetaFactory(); metadata.register(descriptor);
            context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).executionLogging(false).build());
            service = new PortableSQLDataService("fixture", database, metadata);
            repository = service.getRepository("Row"); repository.ensurePhysicalSchema(context);
        }
        Row row(long id, long version) {
            database.executeUpdate("INSERT INTO version_lifecycle_data(id,version,name) VALUES(?,?,?)", new Object[]{id, version, "original"});
            return repository.loadPersistedById(context, id);
        }
        void reset() { database.writes = 0; database.transactions = 0; }
        long version(long id) { return ((Number) database.query("SELECT version FROM version_lifecycle_data WHERE id=?", new Object[]{id}).get(0).get("version")).longValue(); }
        void rejected(Runnable action, Row... rows) {
            long[] before = Arrays.stream(rows).mapToLong(row -> row.getVersion()).toArray();
            var names = Arrays.stream(rows).map(row -> database.query("SELECT name FROM version_lifecycle_data WHERE id=?", new Object[]{row.getId()}).get(0).get("name")).toList();
            var pendingNames = Arrays.stream(rows).map(row -> row.name).toList();
            reset();
            Throwable failure = null;
            try { action.run(); } catch (RuntimeException error) { failure = error; }
            System.out.println("VERSION_SQL originals=" + Arrays.toString(before) + " writes=" + database.writes
                    + " transactions=" + database.transactions + " failure=" + (failure == null ? "none" : failure.getClass().getSimpleName()));
            assertEquals("invalid full batch must not enter storage", 0, database.writes);
            assertEquals("invalid provider batch must not begin transaction", 0, database.transactions);
            assertNotNull("invalid operation requires explicit rejection", failure);
            for (int i = 0; i < rows.length; i++) {
                assertEquals(before[i], version(rows[i].getId())); assertEquals(Long.valueOf(before[i]), rows[i].getVersion());
                assertEquals(names.get(i), database.query("SELECT name FROM version_lifecycle_data WHERE id=?", new Object[]{rows[i].getId()}).get(0).get("name"));
                assertEquals(pendingNames.get(i), rows[i].name);
            }
        }
    }
    static void rejectUpdate(long version) throws Exception {
        var f = new Fixture(); var row = f.row(1, version); row.set$status(EntityStatus.PERSISTED); row.updateName("pending");
        f.rejected(() -> f.repository.updateInternal(f.context, List.of(row)), row);
    }
    @Test public void exhaustedUpdateCannotWrapToTombstone() throws Exception { rejectUpdate(Long.MAX_VALUE); }
    @Test public void zeroUpdateCannotBecomeLive() throws Exception { rejectUpdate(0); }
    @Test public void tombstoneCannotBeUpdatedAsLive() throws Exception { rejectUpdate(-1); }
    @Test public void minimumRecoveryCannotWrap() throws Exception {
        var f = new Fixture(); var row = f.row(1, Long.MIN_VALUE); f.rejected(() -> f.repository.recoverInternal(f.context, List.of(row)), row);
    }
    @Test public void minimumPlusOneRecoveryCannotWrap() throws Exception {
        var f = new Fixture(); var row = f.row(1, Long.MIN_VALUE + 1); f.rejected(() -> f.repository.recoverInternal(f.context, List.of(row)), row);
    }
    @Test public void nonLiveDeleteCannotSilentlySucceed() throws Exception {
        var f = new Fixture(); var row = f.row(1, -2); f.rejected(() -> f.repository.deleteInternal(f.context, List.of(row)), row);
    }
    @Test public void wholeUpdateCollectionRejectedBeforeFirstNativeWrite() throws Exception {
        var f = new Fixture(); var first = f.row(1, 1); var second = f.row(2, Long.MAX_VALUE);
        first.updateName("first pending"); second.updateName("second pending");
        f.rejected(() -> f.repository.updateInternal(f.context, List.of(first, second)), first, second);
    }
    @Test public void wholePreparedBatchRejectedBeforeFirstNativeWrite() throws Exception {
        var f = new Fixture(); var first = f.row(1, 1); var second = f.row(2, Long.MAX_VALUE);
        first.updateName("first pending"); second.updateName("second pending");
        var traces = List.of(SqlExecutionTrace.mutation(first, List.of(), "update", MutationIntent.of("inspect version batch")),
                SqlExecutionTrace.mutation(second, List.of(), "update", MutationIntent.of("inspect version batch")));
        f.rejected(() -> f.repository.updateBatchInternal(f.context, List.of(first, second), null, traces), first, second);
    }
    private static EntityPersistenceMutation mutation(Row row, EntityPersistenceMutation.Action action) {
        return new EntityPersistenceMutation(row, action, MutationIntent.of("validate actual provider version lifecycle"));
    }
    @Test public void providerBatchRejectsEveryOriginalBeforeTransaction() throws Exception {
        var f = new Fixture(); var first = f.row(1, 1); var second = f.row(2, Long.MAX_VALUE);
        first.updateName("first pending"); second.updateName("second pending");
        var request = new MutationBatchRequest("validate actual provider version lifecycle", List.of(
                mutation(first, EntityPersistenceMutation.Action.SAVE), mutation(second, EntityPersistenceMutation.Action.SAVE)));
        f.rejected(() -> f.service.mutateBatch(f.context, request), first, second);
    }
    @Test public void providerSingleRejectsBeforeNativeWriteOrReadback() throws Exception {
        var f = new Fixture(); var row = f.row(1, Long.MAX_VALUE); row.updateName("pending");
        f.rejected(() -> f.service.mutate(f.context, mutation(row, EntityPersistenceMutation.Action.SAVE)), row);
    }
    @Test public void lastUpdateThenDeleteUsesExactLongEndpointsInDatabaseAndReadback() throws Exception {
        var f = new Fixture(); var row = f.row(1, Long.MAX_VALUE - 1); row.updateName("final live value");
        var updated = f.service.mutate(f.context, mutation(row, EntityPersistenceMutation.Action.SAVE));
        assertEquals(Long.valueOf(Long.MAX_VALUE), row.getVersion()); assertEquals(Long.MAX_VALUE, f.version(1));
        assertEquals(Long.valueOf(Long.MAX_VALUE), updated.persistedEntity().getVersion());
        row.markForDeletion(); var deleted = f.service.mutate(f.context, mutation(row, EntityPersistenceMutation.Action.DELETE));
        assertEquals(Long.valueOf(Long.MIN_VALUE), row.getVersion()); assertEquals(Long.MIN_VALUE, f.version(1));
        assertEquals(EntityStatus.PERSISTED_DELETED, row.get$status());
        assertEquals(Long.valueOf(Long.MIN_VALUE), deleted.persistedEntity().getVersion());
        row.markAsRecover(); f.rejected(() -> f.service.mutate(f.context, mutation(row, EntityPersistenceMutation.Action.SAVE)), row);
    }
    @Test public void lastRepresentableRecoveryUsesExactLongReadback() throws Exception {
        var f = new Fixture(); var row = f.row(1, Long.MIN_VALUE + 2); row.markAsRecover();
        var recovered = f.service.mutate(f.context, mutation(row, EntityPersistenceMutation.Action.SAVE));
        assertEquals(Long.valueOf(Long.MAX_VALUE), row.getVersion()); assertEquals(Long.MAX_VALUE, f.version(1));
        assertEquals(EntityStatus.PERSISTED, row.get$status()); assertEquals(Long.valueOf(Long.MAX_VALUE), recovered.persistedEntity().getVersion());
    }
    @Test public void preparedDeleteAtMaxAndRecoveryToMaxMatchScalarTransitions() throws Exception {
        var f = new Fixture(); var first = f.row(1, Long.MAX_VALUE); var second = f.row(2, 1);
        first.markForDeletion(); second.markForDeletion();
        var deleted = f.service.mutateBatch(f.context, new MutationBatchRequest("validate actual provider version lifecycle", List.of(
                mutation(first, EntityPersistenceMutation.Action.DELETE), mutation(second, EntityPersistenceMutation.Action.DELETE))));
        assertEquals(Long.MIN_VALUE, f.version(1)); assertEquals(-2, f.version(2));
        assertEquals(Long.valueOf(Long.MIN_VALUE), deleted.get(0).persistedEntity().getVersion());
        var recoverable = f.row(3, Long.MIN_VALUE + 2); recoverable.markAsRecover(); second.markAsRecover();
        var recovered = f.service.mutateBatch(f.context, new MutationBatchRequest("validate actual provider version lifecycle", List.of(
                mutation(recoverable, EntityPersistenceMutation.Action.SAVE), mutation(second, EntityPersistenceMutation.Action.SAVE))));
        assertEquals(Long.MAX_VALUE, f.version(3)); assertEquals(3, f.version(2));
        assertEquals(Long.valueOf(Long.MAX_VALUE), recovered.get(0).persistedEntity().getVersion());
    }
}
