package com.example.schoolmanagementservice;

import io.teaql.core.*;
import io.teaql.core.meta.*;
import io.teaql.core.sql.portable.*;
import io.teaql.runtime.*;
import java.lang.management.ManagementFactory;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Generated metadata and real repository mapping; input preparation is uncounted. */
class GeneratedWideHydrationAllocationTest {
    static volatile Object consumed;
    static final class Request extends BaseRequest<BaseEntity> {
        @SuppressWarnings("unchecked")
        Request(EntityDescriptor descriptor, SimpleEntityMetaFactory metadata, List<String> names, int size) {
            super((Class<BaseEntity>)descriptor.getTargetType(), () -> (BaseEntity)descriptor.createEntity());
            bindMetadata(metadata); names.forEach(this::selectProperty); setSize(size);
            internalComment("hydrate generated wide allocation rows");
            internalPurpose("verify shared overflow state without database or logging costs");
        }
        @Override public String getTypeName() { return returnType().getSimpleName(); }
    }
    record Row(Object[] cells) implements DataRow {
        public Object get(int index) { return cells[index-1]; }
        @SuppressWarnings("unchecked")
        public <V> V get(int index, Class<V> type) { return (V)get(index); }
    }
    static final class Database implements TeaQLDatabase {
        List<Row> rows;
        List<Map<String,Object>> maps;
        int compiledCalls;
        int mapCalls;
        public boolean supportsCompiledRowMapping() { return true; }
        public <T extends Entity> List<T> query(UserContext context,String sql,Object[] args,CompiledRowMapper<T> mapper) {
            compiledCalls++; var result = new ArrayList<T>(rows.size());
            for (Row row : rows) result.add(mapper.map(row)); return result;
        }
        public List<Map<String,Object>> query(String sql,Object[] args) { mapCalls++; return maps; }
        public int executeUpdate(String sql,Object[] args) { throw new AssertionError("read-only fixture"); }
        public int[] batchUpdate(String sql,List<Object[]> args) { throw new AssertionError("read-only fixture"); }
        public void execute(String sql) { throw new AssertionError("no schema I/O in allocation fixture"); }
        public void executeInTransaction(Runnable action) { throw new AssertionError("read-only fixture"); }
        public List<Map<String,Object>> getTableColumns(String table) { throw new AssertionError("metadata comes from generated module"); }
        void prepare(List<String> names,Object[] defaults,int count) {
            rows = new ArrayList<>(count); maps = new ArrayList<>(count);
            for (int i=1; i<=count; i++) {
                Object[] cells = defaults.clone(); cells[0] = (long)i;
                rows.add(new Row(cells)); Map<String,Object> map = new LinkedHashMap<>();
                for (int column=0; column<names.size(); column++) map.put(names.get(column),cells[column]);
                maps.add(map);
            }
        }
    }
    static PropertyDescriptor property(EntityDescriptor descriptor,String member) {
        for (EntityDescriptor current=descriptor; current!=null; current=current.getParent()) {
            PropertyDescriptor found = current.findProperty(member); if (found!=null) return found;
        }
        throw new AssertionError("missing generated property " + member);
    }
    static Object cell(EntityDescriptor descriptor,String member) {
        Class<?> type = property(descriptor,member).getType().javaType();
        if (member.startsWith("probe")) return null;
        if (Entity.class.isAssignableFrom(type)) return 1L;
        if (type == String.class) return "sample name";
        if (type == Long.class || type == long.class) return 1L;
        if (type == Integer.class || type == int.class) return 0;
        if (type == Boolean.class || type == boolean.class) return false;
        if (type == LocalDate.class) return LocalDate.of(2000,1,1);
        if (type == LocalDateTime.class) return LocalDateTime.of(2023,11,14,22,13);
        throw new AssertionError("unexpected controlled fixture type " + type);
    }
    @Test void generatedWideHydrationKeepsOverflowShared() {
        var metadata = new SimpleEntityMetaFactory();
        var runtime = TeaQLRuntime.builder().metadata(metadata).queryExecutionLogging(false).build()
                .install(GeneratedRuntimeModule.module());
        var context = new DefaultUserContext(runtime);
        var bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        assertTrue(bean.isThreadAllocatedMemorySupported()); bean.setThreadAllocatedMemoryEnabled(true);
        System.out.println("case,entity,selected_fields,rows,allocated_bytes,elapsed_ns");
        for (EntityDescriptor descriptor : metadata.allEntityDescriptors()) {
            if (!Set.of("School","Academy").contains(descriptor.getType())) continue;
            var layout = FieldLayout.forType(descriptor.getTargetType());
            assertTrue(layout.indexes().size()>130, "requires generated wide fixture");
            var database = new Database();
            var repository = new PortableSQLRepository<BaseEntity>(descriptor,database,type -> null,metadata);
            List<String> ordered = layout.indexes().entrySet().stream().sorted(Map.Entry.comparingByValue())
                    .map(entry -> layout.memberName(entry.getKey())).toList();
            for (int width : new int[]{3,64,layout.indexes().size()}) {
                List<String> names = new ArrayList<>(List.of("id","version","name"));
                for (String name : ordered) { if (names.size()>=width) break; if (!names.contains(name)) names.add(name); }
                Object[] defaults = names.stream().map(name -> cell(descriptor,name)).toArray();
                database.prepare(names,defaults,1); var warm = new Request(descriptor,metadata,names,1);
                for (int i=0; i<1000; i++) consumed = repository.loadInternal(context,warm);
                for (int count : new int[]{1,100,10_000}) {
                    database.prepare(names,defaults,count); var request = new Request(descriptor,metadata,names,count);
                    long thread = Thread.currentThread().getId(); long before = bean.getThreadAllocatedBytes(thread);
                    long start = System.nanoTime(); var entities = repository.loadInternal(context,request); consumed = entities;
                    long nanos = System.nanoTime()-start; long bytes = bean.getThreadAllocatedBytes(thread)-before;
                    System.out.printf("generated_wide_hydration,%s,%d,%d,%d,%d%n",descriptor.getType(),width,count,bytes,nanos);
                    assertEquals(count,entities.size()); LoadState state = entities.get(0).__internalLoadState();
                    for (int i=0; i<count; i++) {
                        BaseEntity entity = entities.get(i);
                        assertEquals((long)i+1,entity.getId()); assertEquals("sample name",entity.getProperty("name"));
                        assertSame(state,entity.__internalLoadState()); assertSame(layout,state.layout());
                        assertFalse(entity.__internalHasMutationLedger());
                        for (String name : names) assertTrue(entity.isPropertyLoaded(name));
                    }
                    if (count>1) {
                        String probe = layout.indexes().entrySet().stream().filter(entry -> entry.getValue()==64)
                                .map(Map.Entry::getKey).findFirst().orElseThrow();
                        LoadState changed = state.withLoaded(probe,!state.isLoaded(probe));
                        assertNotSame(state,changed); assertSame(state,entities.get(1).__internalLoadState());
                    }
                }
            }
            assertEquals(descriptor.hasChildren(),database.compiledCalls==0);
            assertEquals(!descriptor.hasChildren(),database.mapCalls==0);
        }
        System.out.println("PASS generated Java wide hydration shares one overflow snapshot per shape");
    }
}
