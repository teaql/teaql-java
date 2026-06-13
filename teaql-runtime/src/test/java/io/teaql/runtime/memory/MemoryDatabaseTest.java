package io.teaql.runtime.memory;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.runtime.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MemoryDatabaseTest {

    // ── Stub Entity and Request ──────────────────────────

    public static class Task extends BaseEntity {
        private String title;
        private String status;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            handleUpdate("title", this.title, title);
            this.title = title;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            handleUpdate("status", this.status, status);
            this.status = status;
        }

        @Override
        public String typeName() {
            return "Task";
        }
    }

    public static class TaskRequest extends BaseRequest<Task> {
        public TaskRequest() {
            super(Task.class);
        }

        @Override
        public String getTypeName() {
            return "Task";
        }

        public TaskRequest filterByTitle(String title) {
            appendSearchCriteria(createBasicSearchCriteria("title", Operator.EQUAL, title));
            return this;
        }

        public TaskRequest filterByStatus(String status) {
            appendSearchCriteria(createBasicSearchCriteria("status", Operator.EQUAL, status));
            return this;
        }
    }

    // ── Setup metadata and runtime ───────────────────────

    private static SimpleEntityMetaFactory metaFactory;
    private static MemoryDataService memoryDb;
    private static UserContext ctx;

    @BeforeClass
    public static void setup() {
        metaFactory = new SimpleEntityMetaFactory();

        // Describe Task Entity
        EntityDescriptor taskDescriptor = new EntityDescriptor();
        taskDescriptor.setType("Task");
        taskDescriptor.setTargetType(Task.class);
        taskDescriptor.setDataService("memory"); // route to memory provider

        List<PropertyDescriptor> props = new ArrayList<>();

        PropertyDescriptor idProp = new PropertyDescriptor();
        idProp.setName("id");
        idProp.setOwner(taskDescriptor);
        props.add(idProp);

        PropertyDescriptor versionProp = new PropertyDescriptor();
        versionProp.setName("version");
        versionProp.setOwner(taskDescriptor);
        props.add(versionProp);

        PropertyDescriptor titleProp = new PropertyDescriptor();
        titleProp.setName("title");
        titleProp.setOwner(taskDescriptor);
        props.add(titleProp);

        PropertyDescriptor statusProp = new PropertyDescriptor();
        statusProp.setName("status");
        statusProp.setOwner(taskDescriptor);
        props.add(statusProp);

        taskDescriptor.setProperties(props);

        metaFactory.register(taskDescriptor);
        EntityMetaFactory.registerGlobal(metaFactory);

        // Build runtime
        memoryDb = new MemoryDataService("memory");
        AtomicLong idGen = new AtomicLong(100);
        InternalIdGenerationService idService = (c, entity) -> idGen.getAndIncrement();

        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(metaFactory)
                .dataService("memory", memoryDb)
                .idGenerationService(idService)
                .build();

        ctx = new DefaultUserContext(runtime);
    }

    @Test
    public void testFullMemoryWorkflow() {
        // 1. Create and Save Tasks
        Task task1 = new Task();
        task1.setTitle("Assemble Assembly Line");
        task1.setStatus("TODO");
        task1.save(ctx);

        assertNotNull("ID should be generated automatically", task1.getId());
        assertEquals(Long.valueOf(100), task1.getId());
        assertEquals("Status should transition to PERSISTED", EntityStatus.PERSISTED, task1.get$status());

        Task task2 = new Task();
        task2.setTitle("Write Integration Tests");
        task2.setStatus("TODO");
        task2.save(ctx);
        assertEquals(Long.valueOf(101), task2.getId());

        // 2. Query Tasks by criteria
        TaskRequest req = new TaskRequest().filterByTitle("Assemble Assembly Line");
        SmartList<Task> resultList = req.executeForList(ctx);
        assertEquals(1, resultList.size());
        assertEquals("Assemble Assembly Line", resultList.get(0).getTitle());

        // Test filter no results
        TaskRequest reqEmpty = new TaskRequest().filterByTitle("Clean up workspace");
        assertTrue(reqEmpty.executeForList(ctx).isEmpty());

        // 3. Update task
        task1.setStatus("DONE");
        task1.save(ctx);

        TaskRequest reqDone = new TaskRequest().filterByStatus("DONE");
        SmartList<Task> resultDone = reqDone.executeForList(ctx);
        assertEquals(1, resultDone.size());
        assertEquals("Assemble Assembly Line", resultDone.get(0).getTitle());

        // 4. Delete task
        task1.delete(ctx);

        SmartList<Task> resultAfterDelete = new TaskRequest().filterByStatus("DONE").executeForList(ctx);
        assertTrue("Task should be removed from DB", resultAfterDelete.isEmpty());
    }
}
