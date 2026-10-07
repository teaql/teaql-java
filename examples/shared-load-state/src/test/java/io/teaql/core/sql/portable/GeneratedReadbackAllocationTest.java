package io.teaql.core.sql.portable;

import com.example.schoolmanagementservice.GeneratedRuntimeModule;
import io.teaql.core.*;
import io.teaql.core.meta.*;
import io.teaql.runtime.*;
import java.lang.management.ManagementFactory;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Generated School, prepared driver rows, no SQL I/O, intent logging or setup in counters. */
class GeneratedReadbackAllocationTest {
    static volatile Object consumed;
    record Binding(PropertyDescriptor property, String column, EntityDescriptor target) {}
    record Sample(List<BaseEntity> rows, long bytes) {}
    static class Database implements TeaQLDatabase {
        List<Map<String,Object>> rows;
        public List<Map<String,Object>> query(String sql,Object[] args) {
            return List.of(rows.get(((Number)args[0]).intValue()-1));
        }
        public int executeUpdate(String sql,Object[] args) { throw new AssertionError("read only"); }
        public int[] batchUpdate(String sql,List<Object[]> args) { throw new AssertionError("read only"); }
        public void execute(String sql) { throw new AssertionError("no schema I/O"); }
        public void executeInTransaction(Runnable action) { throw new AssertionError("no transaction I/O"); }
        public List<Map<String,Object>> getTableColumns(String table) { throw new AssertionError("generated metadata"); }
    }
    static Object value(PropertyDescriptor property) {
        Class<?> type=property.getType().javaType();
        if(property.getName().startsWith("probe"))return null;
        if(Entity.class.isAssignableFrom(type))return property.getName().equals("schoolType")?1001L:1L;
        if(type==String.class)return "readback name";
        if(type==Long.class||type==long.class)return 1L;
        if(type==Integer.class||type==int.class)return 0;
        if(type==Boolean.class||type==boolean.class)return false;
        if(type==LocalDate.class)return LocalDate.of(1995,9,1);
        if(type==LocalDateTime.class)return LocalDateTime.of(2023,11,14,22,13);
        throw new AssertionError(type);
    }
    static Sample sample(com.sun.management.ThreadMXBean bean,int count,
            java.util.function.IntFunction<BaseEntity> loader) {
        long thread=Thread.currentThread().threadId(),before=bean.getThreadAllocatedBytes(thread);
        List<BaseEntity> result=new ArrayList<>(count);
        for(int id=1;id<=count;id++)result.add(loader.apply(id));
        consumed=result;
        return new Sample(result,bean.getThreadAllocatedBytes(thread)-before);
    }
    @Test void generatedReadbackSharesGeometryWithoutPerFieldOverflowCopies() {
        var metadata=new SimpleEntityMetaFactory();
        var context=new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).queryExecutionLogging(false)
                .build().install(GeneratedRuntimeModule.module()));
        var descriptor=metadata.allEntityDescriptors().stream().filter(d->d.getType().equals("School")).findFirst().orElseThrow();
        var layout=FieldLayout.forType(descriptor.getTargetType());assertTrue(layout.indexes().size()>130);
        List<Binding> bindings=new ArrayList<>();
        for(var property:descriptor.getProperties()) {
            EntityDescriptor target=metadata.allEntityDescriptors().stream()
                    .filter(d->d.getTargetType()==property.getType().javaType()).findFirst().orElse(null);
            bindings.add(new Binding(property,SQLPropertyUtil.getColumns(property).get(0).getColumnName(),target));
        }
        var database=new Database();database.rows=new ArrayList<>(10000);
        for(int id=1;id<=10000;id++) {
            Map<String,Object> row=new LinkedHashMap<>();
            for(var binding:bindings)row.put(binding.column(),binding.property().isId()?(long)id:value(binding.property()));
            database.rows.add(row);
        }
        var repository=new PortableSQLRepository<BaseEntity>(descriptor,database,type->null,metadata);
        java.util.function.IntFunction<BaseEntity> cached=id->repository.loadPersistedById(context,(long)id);
        // Correct typed values, but reproduce the previous incremental availability algorithm.
        java.util.function.IntFunction<BaseEntity> incremental=id->{
            BaseEntity entity=(BaseEntity)descriptor.createEntity();Map<String,Object> row=database.rows.get(id-1);
            for(var binding:bindings) {
                Object cell=row.get(binding.column());
                if(binding.target()!=null&&cell!=null) {
                    BaseEntity reference=(BaseEntity)binding.target().createEntity();
                    reference.__internalSet("id",cell);reference.set$status(EntityStatus.REFER);cell=reference;
                }
                entity.setProperty(binding.property().getName(),cell);
            }
            entity.set$status(EntityStatus.PERSISTED);entity.clearUpdatedProperties();return entity;
        };
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        assertTrue(bean.isThreadAllocatedMemorySupported());bean.setThreadAllocatedMemoryEnabled(true);
        for(int i=0;i<2000;i++){consumed=cached.apply(1);consumed=incremental.apply(1);}
        for(int count:new int[]{1,100,10000}) {
            var old=sample(bean,count,incremental);var current=sample(bean,count,cached);
            System.out.printf("NATIVE_READBACK_ALLOC,%d,%d,%d%n",count,old.bytes(),current.bytes());
            assertTrue(current.bytes()<old.bytes(),"whole generated row must avoid repeated overflow copies");
            var state=current.rows().get(0).__internalLoadState();
            for(int index=0;index<count;index++) {
                var row=current.rows().get(index);var reference=old.rows().get(index);
                assertEquals((long)index+1,row.getId());assertEquals(1L,row.getVersion());
                assertSame(state,row.__internalLoadState());assertSame(layout,state.layout());
                assertEquals(layout.indexes().size(),layout.indexes().keySet().stream().filter(row::isPropertyLoaded).count());
                assertEquals(reference.<String>getProperty("name"),row.<String>getProperty("name"));
                assertEquals(LocalDate.of(1995,9,1),row.getProperty("establishedDate"));
                for(var binding:bindings) {
                    Object expected=reference.__internalGet(binding.property().getName());
                    Object actual=row.__internalGet(binding.property().getName());
                    if(expected instanceof Entity entity) {
                        assertTrue(actual instanceof Entity);
                        assertEquals(entity.getId(),((Entity)actual).getId());
                        assertEquals(entity.getClass(),actual.getClass());
                    } else assertEquals(expected,actual,binding.property().getName());
                }
                assertTrue(row.getUpdatedProperties().isEmpty());assertFalse(row.__internalHasMutationLedger());
            }
        }
        System.out.println("PASS generated Java authoritative readback reuses one actual shape and avoids per-field overflow copies");
    }
}
