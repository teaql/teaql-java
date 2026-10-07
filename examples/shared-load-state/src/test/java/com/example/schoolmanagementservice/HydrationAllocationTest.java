package com.example.schoolmanagementservice;

import io.teaql.core.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.core.sql.portable.PortableSQLRepository;
import io.teaql.core.sql.portable.TeaQLDatabase;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.lang.management.ManagementFactory;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real repository row hydration with prepared inputs; no JDBC, logging or seeding. */
class HydrationAllocationTest {
    static volatile Object consumed;
    static final class Probe extends BaseEntity {
        static final FieldLayout LAYOUT = FieldLayout.installGenerated(FieldLayout.generated(
                Probe.class, "hydration-probe-v1", Map.of("id",0,"version",1,"name",2,"note",3),
                Map.of("id",List.of("id","id"),"version",List.of("version","version"),
                        "name",List.of("name","name"),"note",List.of("note","note")),Set.of()));
        String name;
        String note;
        @Override public Object __internalGet(String field) {
            return switch (field) { case "name" -> name; case "note" -> note; default -> super.__internalGet(field); };
        }
        @Override public void __internalSet(String field, Object value) {
            switch (field) {
                case "name" -> { markPropertyLoaded(field); name = (String) value; }
                case "note" -> { markPropertyLoaded(field); note = (String) value; }
                default -> super.__internalSet(field,value);
            }
        }
    }
    static final class Request extends BaseRequest<Probe> {
        Request(SimpleEntityMetaFactory metadata, boolean full, int size) {
            super(Probe.class, Probe::new);
            bindMetadata(metadata);
            for (String field : full ? List.of("id","version","name","note") : List.of("id","version","name")) selectProperty(field);
            setSize(size);
            internalComment("hydrate controlled allocation rows");
            internalPurpose("measure row payload versus shared availability allocation");
        }
        @Override public String getTypeName() { return "Probe"; }
    }
    record Plain(Long id, Long version, String name, String note) {}
    record Input(Long id, Long version, String name, String note) implements DataRow {
        public Object get(int index) { return switch (index) { case 1 -> id; case 2 -> version; case 3 -> name; case 4 -> note; default -> throw new AssertionError(index); }; }
        public <V> V get(int index, Class<V> type) { return type.cast(get(index)); }
    }
    static final class PreparedDatabase implements TeaQLDatabase {
        boolean compiled;
        List<Input> rows;
        List<Map<String,Object>> maps;
        int compiledCalls;
        int genericCalls;
        int typedStreamCalls;
        int mapStreamCalls;
        boolean allocateStreamMaps;
        void prepare(int size, boolean full) {
            rows = new ArrayList<>(size); maps = new ArrayList<>(size);
            for (int i=1; i<=size; i++) {
                Input row = new Input((long)i,1L,"sample name",null); rows.add(row);
                Map<String,Object> map = new LinkedHashMap<>();
                map.put("id",row.id()); map.put("version",row.version()); map.put("name",row.name());
                if (full) map.put("note",null);
                maps.add(map);
            }
        }
        public boolean supportsCompiledRowMapping() { return compiled; }
        public <T extends Entity> List<T> query(UserContext context, String sql, Object[] args, CompiledRowMapper<T> mapper) {
            compiledCalls++;
            List<T> result = new ArrayList<>(rows.size());
            for (Input row : rows) result.add(mapper.map(row));
            return result;
        }
        public List<Map<String,Object>> query(String sql, Object[] args) { genericCalls++; return maps; }
        public boolean supportsCompiledStreamMapping() { return compiled; }
        public <T extends Entity> java.util.stream.Stream<T> queryForStream(UserContext context, String sql, Object[] args,
                CompiledRowMapper<T> mapper, io.teaql.core.sql.portable.SqlLogBindings bindings) {
            typedStreamCalls++; return rows.stream().map(mapper::map);
        }
        public java.util.stream.Stream<Map<String,Object>> queryForStream(UserContext context, String sql, Object[] args) {
            mapStreamCalls++;
            // Explicit control for provider-side map creation; fixed input values
            // remain prepared outside measurement in both lanes.
            return allocateStreamMaps ? maps.stream().map(LinkedHashMap::new) : maps.stream();
        }
        public int executeUpdate(String sql, Object[] args) { throw new AssertionError("read-only probe"); }
        public int[] batchUpdate(String sql, List<Object[]> args) { throw new AssertionError("read-only probe"); }
        public void execute(String sql) { throw new AssertionError("no schema I/O in measured probe"); }
        public void executeInTransaction(Runnable action) { throw new AssertionError("read-only probe"); }
        public List<Map<String,Object>> getTableColumns(String table) { throw new AssertionError("metadata prepared outside measurement"); }
    }
    record Sample(Object result, long bytes, long nanos) {}

    @Test void compiledAndFallbackStreamsKeepSharedNullAndNotLoadedWithLessAllocation() {
        assertNotNull(Probe.LAYOUT);
        var metadata = new SimpleEntityMetaFactory();
        var descriptor = new SQLEntityDescriptor(); descriptor.setType("Probe");
        descriptor.setTargetType(Probe.class); descriptor.setEntitySupplier(Probe::new);
        for (String field : List.of("id","version","name","note")) {
            var property = (GenericSQLProperty) descriptor.addSimpleProperty(field,
                    field.equals("id") || field.equals("version") ? Long.class : String.class);
            property.setColumnType(field.equals("id") || field.equals("version") ? "BIGINT" : "VARCHAR(255)");
        }
        metadata.register(descriptor);
        var database = new PreparedDatabase();
        database.allocateStreamMaps = true;
        var repository = new PortableSQLRepository<Probe>(descriptor,database,type -> null,metadata);
        var context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).queryExecutionLogging(false).build());
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assertTrue(bean.isThreadAllocatedMemorySupported()); bean.setThreadAllocatedMemoryEnabled(true);
        for (boolean full : List.of(false,true)) {
            database.prepare(1,full);
            var warm = new Request(metadata,full,1);
            for (int i=0; i<2_000; i++) for (boolean compiled : List.of(false,true)) {
                database.compiled=compiled;
                try (var stream=repository.streamInternal(context,warm)) { consumed=stream.toList(); }
            }
            for (int count : new int[]{1,100,10_000}) {
                database.prepare(count,full);
                var request = new Request(metadata,full,count);
                long compiledBytes=0, mapBytes=0;
                for (boolean compiled : List.of(false,true)) {
                    database.compiled=compiled;
                    int typedBefore=database.typedStreamCalls, mapsBefore=database.mapStreamCalls;
                    Sample sample=measure(bean,() -> { try(var stream=repository.streamInternal(context,request)) {return stream.toList();} });
                    @SuppressWarnings("unchecked") var rows=(List<Probe>)sample.result();
                    assertEquals(count,rows.size()); LoadState shape=rows.get(0).__internalLoadState();
                    for (Probe row : rows) {
                        assertSame(shape,row.__internalLoadState()); assertEquals(full,row.isPropertyLoaded("note"));
                        assertNull(row.note); assertEquals("sample name",row.name);
                        assertFalse(row.__internalHasMutationLedger()); assertTrue(row.getUpdatedProperties().isEmpty());
                    }
                    assertEquals(typedBefore+(compiled?1:0),database.typedStreamCalls);
                    assertEquals(mapsBefore+(compiled?0:1),database.mapStreamCalls);
                    if(compiled) compiledBytes=sample.bytes(); else mapBytes=sample.bytes();
                    System.out.printf("STREAM_HYDRATION,%s,%s,%d,%d%n",compiled?"compiled":"map",full?"full":"sparse",count,sample.bytes());
                }
                if(count==10_000) assertTrue(compiledBytes<mapBytes,"typed stream must remove fallback hydration allocations");
            }
        }
    }
    static Sample measure(com.sun.management.ThreadMXBean bean, java.util.function.Supplier<?> action) {
        long thread = Thread.currentThread().getId();
        long before = bean.getThreadAllocatedBytes(thread);
        long start = System.nanoTime();
        Object result = action.get(); consumed = result;
        long nanos = System.nanoTime()-start;
        long bytes = bean.getThreadAllocatedBytes(thread)-before;
        return new Sample(result,bytes,nanos);
    }
    @Test void fullRepositoryHydrationAllocationProbe() {
        assertNotNull(Probe.LAYOUT); // Generated modules install layouts before query planning.
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assertTrue(bean.isThreadAllocatedMemorySupported()); bean.setThreadAllocatedMemoryEnabled(true);
        var metadata = new SimpleEntityMetaFactory();
        var descriptor = new SQLEntityDescriptor(); descriptor.setType("Probe");
        descriptor.setTargetType(Probe.class); descriptor.setEntitySupplier(Probe::new);
        for (String field : List.of("id","version","name","note")) {
            var property = (GenericSQLProperty) descriptor.addSimpleProperty(field,
                    field.equals("id") || field.equals("version") ? Long.class : String.class);
            property.setColumnType(field.equals("id") || field.equals("version") ? "BIGINT" : "VARCHAR(255)");
        }
        metadata.register(descriptor);
        var database = new PreparedDatabase();
        var repository = new PortableSQLRepository<Probe>(descriptor,database,type -> null,metadata);
        var context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).queryExecutionLogging(false).build());
        System.out.println("case,projection,rows,allocated_bytes,elapsed_ns");
        for (boolean full : List.of(false,true)) {
            String shape = full ? "full" : "sparse";
            for (boolean compiled : List.of(true,false)) {
                database.compiled = compiled; database.prepare(1,full);
                var warm = new Request(metadata,full,1);
                for (int i=0; i<4_000; i++) consumed = repository.loadInternal(context,warm);
                for (int count : new int[]{1,100,10_000}) {
                    database.prepare(count,full);
                    var request = new Request(metadata,full,count);
                    int before = compiled ? database.compiledCalls : database.genericCalls;
                    var sample = measure(bean,() -> repository.loadInternal(context,request));
                    System.out.printf("%s,%s,%d,%d,%d%n",compiled ? "compiled_hydration" : "map_hydration",shape,count,sample.bytes(),sample.nanos());
                    assertEquals(before+1,compiled ? database.compiledCalls : database.genericCalls);
                    @SuppressWarnings("unchecked") var result = (SmartList<Probe>) sample.result();
                    assertEquals(count,result.size());
                    LoadState state = result.get(0).__internalLoadState();
                    for (int i=0; i<count; i++) {
                        Probe row = result.get(i);
                        assertEquals((long)i+1,row.getId()); assertEquals(1L,row.getVersion());
                        assertEquals("sample name",row.name); assertNull(row.note);
                        assertEquals(full,row.isPropertyLoaded("note"));
                        assertSame(state,row.__internalLoadState()); assertSame(Probe.LAYOUT,state.layout());
                        assertTrue(row.getUpdatedProperties().isEmpty());
                        assertFalse(row.__internalHasMutationLedger(), "readonly hydration must not allocate a mutation ledger");
                    }
                    var plain = measure(bean,() -> {
                        List<Plain> resultRows = new ArrayList<>(count);
                        for (Input row : database.rows) resultRows.add(new Plain(row.id(),row.version(),row.name(),row.note()));
                        return resultRows;
                    });
                    System.out.printf("plain_hydration,%s,%d,%d,%d%n",shape,count,plain.bytes(),plain.nanos());
                    assertEquals(count,((List<?>)plain.result()).size());
                }
            }
        }
        // Mixed row shapes must not turn absence into loaded NULL, including
        // a parallel stream consumer racing only the value-free shape hint.
        database.compiled = false; database.prepare(100, true);
        for (int i=0; i<database.maps.size(); i++) {
            if (i % 2 == 1) database.maps.get(i).remove("note");
            else database.maps.get(i).put("hint", null);
        }
        var mixedRequest = new Request(metadata,true,100);
        mixedRequest.addSimpleDynamicProperty("hint",new PropertyReference("name"));
        var mixed = repository.loadInternal(context,mixedRequest);
        var streamed = repository.streamInternal(context,mixedRequest).parallel().toList();
        for (int i=0; i<100; i++) {
            boolean present = i % 2 == 0;
            for (Probe row : List.of(mixed.get(i),streamed.get(i))) {
                assertEquals(present,row.isPropertyLoaded("note"));
                assertEquals(present,row.getAdditionalInfo().containsKey("_hint"));
                assertNull(row.getDynamicProperty("hint"));
                assertFalse(row.__internalHasMutationLedger());
            }
        }
        assertSame(mixed.get(0).__internalLoadState(),mixed.get(2).__internalLoadState());
        assertNotSame(mixed.get(0).__internalLoadState(),mixed.get(1).__internalLoadState());
        System.out.println("PASS mixed map/parallel-stream projection and dynamic-property NULL presence");
    }
}
