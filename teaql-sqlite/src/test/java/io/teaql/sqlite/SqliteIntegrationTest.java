package io.teaql.sqlite;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.dataservice.sql.SqlDataServiceExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.DefaultQueryRequest;
import io.teaql.runtime.RuntimeLogSink;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.runtime.RawAuditEvent;
import io.teaql.runtime.SafeAuditEvent;
import io.teaql.runtime.AppAuditEventSink;
import io.teaql.data.dynamic.*;
import io.teaql.data.dynamic.jdbc.JdbcDynamicFieldsProvider;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class SqliteIntegrationTest {

    private static final AtomicLong dynamicDefinitionIds = new AtomicLong(100_000);
    private static final DynamicFieldContext definitionContext = new DynamicFieldContext() {
        public String scopeType() { return "GLOBAL"; }
        public String scopeId() { return "default"; }
        public String userId() { return "sqlite-regression"; }
        public String purpose() { return "register isolated regression definitions"; }
        public String comment() { return "what: prepare dynamic field metadata"; }
        public boolean strictIntent() { return true; }
        public long nextId(String type) { return dynamicDefinitionIds.getAndIncrement(); }
    };

    private record DynamicFixture(DefaultUserContext context, List<RawAuditEvent> audits,
                                  List<SafeAuditEvent> appAudits, List<ExecutionMetadata> logs) {}

    private DynamicFixture dynamicFixture(JdbcDynamicFieldsProvider provider) {
        provider.ensureSchema();
        var audits = new ArrayList<RawAuditEvent>();
        var appAudits = new ArrayList<SafeAuditEvent>();
        var logs = new ArrayList<ExecutionMetadata>();
        var local = new DefaultUserContext(TeaQLRuntime.builder().metadata(runtime.getMetadata())
                .dataService("sqlite", runtime.getRegistry().resolve("sqlite"))
                .idGenerationService(runtime.getIdGenerationService()).logSink(new RuntimeLogSink() {
                    @Override public void writeExecutionLog(UserContext caller, ExecutionMetadata value) { logs.add(value); }
                    @Override public void writeAuditEvent(UserContext caller, RawAuditEvent value) { audits.add(value); }
                }).build());
        local.putAttribute(DynamicFieldsFacade.class.getName(), new DefaultDynamicFieldsFacade(provider));
        local.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, value) -> appAudits.add(value));
        return new DynamicFixture(local, audits, appAudits, logs);
    }

    private DynamicFieldDef define(JdbcDynamicFieldsProvider provider, String code, DynamicDataType type) {
        return define(provider, code, type, definitionContext);
    }

    private DynamicFieldDef define(JdbcDynamicFieldsProvider provider, String code, DynamicDataType type, DynamicFieldContext context) {
        var field = new DynamicFieldDef();
        field.setScope(DynamicFieldScope.of(context.scopeType(), context.scopeId()));
        field.setOwnerType("Task");
        field.setCode(code);
        field.setName(code);
        field.setDataType(type);
        return provider.registerFieldDef(context, field);
    }

    private DynamicFieldContext alternateScopeContext() {
        return new DynamicFieldContext() {
            public String scopeType() { return "PROFILE"; }
            public String scopeId() { return "other"; }
            public String userId() { return definitionContext.userId(); }
            public String purpose() { return definitionContext.purpose(); }
            public String comment() { return definitionContext.comment(); }
            public boolean strictIntent() { return true; }
            public long nextId(String type) { return definitionContext.nextId(type); }
        };
    }

    @Test
    public void loadedDynamicViewCannotFollowAChangedStorageScope() {
        var provider = new JdbcDynamicFieldsProvider(jdbcSqlExecutor);
        var fixture = dynamicFixture(provider);
        define(provider, "profile_note", DynamicDataType.STRING);
        Task task = dynamicTask(fixture, "PROFILE-ORIGIN", new DynamicFieldSelection().selectString("profile_note"));
        var alternate = alternateScopeContext();
        define(provider, "profile_note", DynamicDataType.STRING, alternate);
        provider.saveValue(alternate, DynamicSetCommand.of(DynamicOwnerRef.of("Task", task.getId()),
                "profile_note", DynamicDataType.STRING, "other profile value", alternate.purpose(), alternate.comment()));
        task.updateTitle("must-not-cross-provider");
        task.updateDynamicField("profile_note", "private held value");
        fixture.context().putAttribute(DynamicFieldsFacade.class.getName(),
                new DefaultDynamicFieldsFacade(provider, DynamicFieldScope.of("PROFILE", "other")));
        try {
            task.auditAs("reject changed provider provenance").save(fixture.context());
            fail("held metadata was accepted by a different storage scope");
        } catch (DynamicFieldException expected) {
            assertEquals("DYNAMIC_FIELD_STORAGE_PROVENANCE_MISMATCH", expected.errorCode());
        }
        assertEquals("initial", nativeColumn(task, "title"));
        assertEquals(Long.valueOf(1), task.getVersion());
        assertEquals(1, dynamicRowCount(task, "profile_note"));
        assertEquals("other profile value", provider.loadValues(alternate, DynamicOwnerRef.of("Task", task.getId()),
                new DynamicFieldSelection().selectString("profile_note")).getString("profile_note"));
        assertFalse(task.__internalDynamicMutations().isEmpty());
        assertEquals(1, fixture.audits().size());
        fixture.context().putAttribute(DynamicFieldsFacade.class.getName(), new DefaultDynamicFieldsFacade(provider));
        task.auditAs("retry with original provider").save(fixture.context());
        assertEquals("must-not-cross-provider", nativeColumn(task, "title"));
        assertEquals("private held value", task.dynamicFields().getString("profile_note"));
        assertEquals("other profile value", provider.loadValues(alternate, DynamicOwnerRef.of("Task", task.getId()),
                new DynamicFieldSelection().selectString("profile_note")).getString("profile_note"));
        assertTrue(task.__internalDynamicMutations().isEmpty());
    }

    @Test
    public void replacingProviderOverTheSameExecutorPreservesStorageProvenance() {
        var provider = new JdbcDynamicFieldsProvider(jdbcSqlExecutor);
        var fixture = dynamicFixture(provider);
        define(provider, "same_source_note", DynamicDataType.STRING);
        Task task = dynamicTask(fixture, "PROFILE-SAME", new DynamicFieldSelection().selectString("same_source_note"));
        task.updateDynamicField("same_source_note", "same storage");
        fixture.context().putAttribute(DynamicFieldsFacade.class.getName(),
                new DefaultDynamicFieldsFacade(new JdbcDynamicFieldsProvider(jdbcSqlExecutor)));
        task.auditAs("keep same executor provenance").save(fixture.context());
        assertEquals("same storage", task.dynamicFields().getString("same_source_note"));
        assertEquals(Long.valueOf(2), task.getVersion());
        assertEquals(2, fixture.audits().size());
    }

    private Task dynamicTask(DynamicFixture fixture, String status, DynamicFieldSelection selection) {
        var task = new Task().updateTitle("initial").updateStatus(status);
        task.auditAs("seed atomic dynamic regression").save(fixture.context());
        return loadDynamicTask(fixture, status, selection);
    }

    private Task loadDynamicTask(DynamicFixture fixture, String status, DynamicFieldSelection selection) {
        var request = new TaskRequest().filterByStatus(status);
        request.setSize(1);
        request.selectDynamicFieldsWith(selection);
        return request.comment("load fixed and dynamic regression fields").purpose("verify graph mutation isolation")
                .executeForList(fixture.context()).get(0);
    }

    private Object nativeColumn(Task task, String column) {
        return jdbcSqlExecutor.queryForList("SELECT " + column + " FROM task_data WHERE id=?", new Object[]{task.getId()})
                .get(0).get(column);
    }

    private long dynamicRowCount(Task task, String code) {
        return ((Number) jdbcSqlExecutor.queryForList("SELECT COUNT(*) AS n FROM teaql_dynamic_field_value v "
                + "JOIN teaql_dynamic_field_def d ON d.id=v.field_id WHERE v.owner_type='Task' AND v.owner_id=? AND d.code=?",
                new Object[]{task.getId(), code}).get(0).get("n")).longValue();
    }

    @Test
    public void dynamicGraphSavePersistsNullAndDeleteDistinctlyAndAuditsAfterCommit() {
        var provider = new JdbcDynamicFieldsProvider(jdbcSqlExecutor);
        var fixture = dynamicFixture(provider);
        define(provider, "atomic_note", DynamicDataType.STRING);
        define(provider, "atomic_untouched", DynamicDataType.STRING);
        var selection = new DynamicFieldSelection().selectString("atomic_note");
        Task task = dynamicTask(fixture, "DYNAMIC-ATOMIC", selection);
        assertEquals(DynamicFieldValue.State.NOT_LOADED, task.dynamicFields().field("atomic_note").state());
        fixture.context().dynamicFields().comment("seed unselected extension").purpose("verify save does not clear it")
                .owner("Task", task.getId()).string("atomic_untouched").set("preserved");
        LoadState before = task.__internalLoadState();
        task.updateTitle("updated");
        task.updateDynamicField("atomic_note", "PRIVATE-DYNAMIC-CANARY");
        assertNotSame(before, task.__internalLoadState());
        task.addDynamicProperty("atomic_note", "readonly-derived");
        task.auditAs("save extension PRIVATE-DYNAMIC-CANARY").save(fixture.context());
        assertEquals("updated", nativeColumn(task, "title"));
        assertEquals(Long.valueOf(2), task.getVersion());
        assertEquals("PRIVATE-DYNAMIC-CANARY", task.dynamicFields().getString("atomic_note"));
        assertTrue(task.__internalDynamicMutations().isEmpty());
        assertTrue(task.getEntityMutationLedger().currentChangeSet().isEmpty());
        assertEquals("readonly-derived", task.getProperty("_atomic_note"));
        assertEquals("preserved", fixture.context().dynamicFields().comment("read untouched extension").purpose("verify isolation")
                .owner("Task", task.getId()).string("atomic_untouched").get());
        assertEquals(2, fixture.audits().size());
        var audit = fixture.audits().get(1);
        assertTrue(audit.changes().stream().anyMatch(change -> change.field().equals("#atomic_note")));
        assertFalse(audit.toString().contains("PRIVATE-DYNAMIC-CANARY"));
        assertFalse(fixture.appAudits().toString().contains("PRIVATE-DYNAMIC-CANARY"));
        assertTrue(fixture.appAudits().get(1).fields().stream().anyMatch(field -> field.name().equals("#atomic_note") && field.masked()));
        assertTrue(fixture.logs().stream().noneMatch(log -> String.valueOf(log.getDebugQuery()).contains("PRIVATE-DYNAMIC-CANARY")));

        LoadState loaded = task.__internalLoadState();
        task.updateDynamicField("atomic_note", null);
        assertSame("value to NULL preserves loaded geometry", loaded, task.__internalLoadState());
        task.auditAs("persist explicit dynamic null after PRIVATE-DYNAMIC-CANARY").save(fixture.context());
        assertEquals(Long.valueOf(3), task.getVersion());
        assertEquals(1, dynamicRowCount(task, "atomic_note"));
        assertEquals(DynamicFieldValue.State.NULL, task.dynamicFields().field("atomic_note").state());
        assertEquals(DynamicFieldValue.State.NULL, loadDynamicTask(fixture, "DYNAMIC-ATOMIC", selection).dynamicFields().field("atomic_note").state());
        assertFalse("overwritten extension values must also be scrubbed from reasons", fixture.audits().toString().contains("PRIVATE-DYNAMIC-CANARY"));
        assertFalse(fixture.appAudits().toString().contains("PRIVATE-DYNAMIC-CANARY"));
        assertTrue(task.__internalDynamicOriginalValues().isEmpty());
        task.deleteDynamicField("atomic_note");
        task.auditAs("delete dynamic field without deleting entity").save(fixture.context());
        assertEquals(Long.valueOf(4), task.getVersion());
        assertEquals(0, dynamicRowCount(task, "atomic_note"));
        assertEquals(DynamicFieldValue.State.NOT_LOADED, task.dynamicFields().field("atomic_note").state());
        assertFalse(task.isPropertyLoaded("#atomic_note"));
    }

    @Test
    public void dynamicFailureAfterWritingRollsBackNativeAndExtensionAndRetainsRetryIntent() {
        class FailingProvider extends JdbcDynamicFieldsProvider {
            boolean fail;
            FailingProvider() { super(jdbcSqlExecutor); }
            @Override public void saveValue(DynamicFieldContext caller, DynamicSetCommand command) {
                super.saveValue(caller, command);
                if (fail) throw new IllegalStateException("injected failure after extension DML");
            }
        }
        var provider = new FailingProvider();
        var fixture = dynamicFixture(provider);
        define(provider, "rollback_note", DynamicDataType.STRING);
        Task task = dynamicTask(fixture, "DYNAMIC-ROLLBACK", new DynamicFieldSelection().selectString("rollback_note"));
        task.updateTitle("retry-title");
        task.updateDynamicField("rollback_note", "retry-value");
        provider.fail = true;
        assertThrows(IllegalStateException.class, () -> task.auditAs("rollback both writes").save(fixture.context()));
        assertEquals("initial", nativeColumn(task, "title"));
        assertEquals(1L, ((Number) nativeColumn(task, "version")).longValue());
        assertEquals(0, dynamicRowCount(task, "rollback_note"));
        assertEquals(Long.valueOf(1), task.getVersion());
        assertEquals(EntityStatus.UPDATED, task.get$status());
        assertEquals("retry-value", task.dynamicFields().getString("rollback_note"));
        assertFalse(task.__internalDynamicMutations().isEmpty());
        assertFalse(task.getEntityMutationLedger().currentChangeSet().isEmpty());
        assertEquals("failed transaction must not emit committed audit", 1, fixture.audits().size());
        provider.fail = false;
        task.auditAs("retry original graph intent").save(fixture.context());
        assertEquals("retry-title", nativeColumn(task, "title"));
        assertEquals(Long.valueOf(2), task.getVersion());
        assertEquals(1, dynamicRowCount(task, "rollback_note"));
        assertEquals(2, fixture.audits().size());
    }

    @Test
    public void dynamicProviderMustShareExecutorNotMerelyDataSource() {
        var provider = new JdbcDynamicFieldsProvider(nativeDataSource);
        var fixture = dynamicFixture(provider);
        define(provider, "wrong_executor", DynamicDataType.STRING);
        Task task = dynamicTask(fixture, "DYNAMIC-WRONG-EXECUTOR", new DynamicFieldSelection().selectString("wrong_executor"));
        task.updateTitle("must-not-commit");
        task.updateDynamicField("wrong_executor", "must-not-commit");
        var error = assertThrows(DynamicFieldException.class, () -> task.auditAs("reject split transaction").save(fixture.context()));
        assertTrue(error.getMessage().contains("exact active graph executor"));
        assertEquals("initial", nativeColumn(task, "title"));
        assertEquals(1L, ((Number) nativeColumn(task, "version")).longValue());
        assertEquals(0, dynamicRowCount(task, "wrong_executor"));
        assertEquals(1, fixture.audits().size());
    }

    @Test
    public void staleNativeVersionRejectsExtensionOnlyUpdate() {
        var provider = new JdbcDynamicFieldsProvider(jdbcSqlExecutor);
        var fixture = dynamicFixture(provider);
        define(provider, "stale_note", DynamicDataType.STRING);
        var selection = new DynamicFieldSelection().selectString("stale_note");
        Task current = dynamicTask(fixture, "DYNAMIC-STALE", selection);
        Task stale = loadDynamicTask(fixture, "DYNAMIC-STALE", selection);
        current.updateDynamicField("stale_note", "current");
        current.auditAs("advance optimistic version").save(fixture.context());
        stale.updateDynamicField("stale_note", "stale");
        assertThrows(RuntimeException.class, () -> stale.auditAs("reject stale extension-only write").save(fixture.context()));
        assertEquals("current", loadDynamicTask(fixture, "DYNAMIC-STALE", selection).dynamicFields().getString("stale_note"));
        assertEquals(Long.valueOf(1), stale.getVersion());
        assertFalse(stale.__internalDynamicMutations().isEmpty());
        assertEquals(2, fixture.audits().size());
    }

    @Test
    public void dynamicReadbackUsesDatabaseAuthoritativeValueInSameTransaction() {
        var provider = new JdbcDynamicFieldsProvider(jdbcSqlExecutor);
        var fixture = dynamicFixture(provider);
        DynamicFieldDef field = define(provider, "trigger_note", DynamicDataType.STRING);
        jdbcSqlExecutor.execute("CREATE TRIGGER normalize_dynamic_note AFTER INSERT ON teaql_dynamic_field_value "
                + "WHEN NEW.field_id=" + field.getId() + " BEGIN UPDATE teaql_dynamic_field_value SET string_value=UPPER(NEW.string_value) "
                + "WHERE field_id=NEW.field_id AND owner_id=NEW.owner_id AND owner_type=NEW.owner_type; END");
        Task task = dynamicTask(fixture, "DYNAMIC-READBACK", new DynamicFieldSelection().selectString("trigger_note"));
        task.updateDynamicField("trigger_note", "canonicalize me");
        task.auditAs("read authoritative extension payload").save(fixture.context());
        assertEquals("CANONICALIZE ME", task.dynamicFields().getString("trigger_note"));
        assertTrue(task.__internalDynamicMutations().isEmpty());
    }

    @Test
    public void unboundNewEntityDefinitionsCannotBeSilentlyAssignedToCurrentStorage() {
        var provider = new JdbcDynamicFieldsProvider(jdbcSqlExecutor);
        var fixture = dynamicFixture(provider);
        define(provider, "unbound_note", DynamicDataType.STRING);
        var task = new Task().updateTitle("must-not-insert").updateStatus("DYNAMIC-UNBOUND");
        task.setDynamicFieldValues(new DynamicFieldValues(
                DynamicFieldMetadata.fromDefinitions(provider.listFieldDefs(definitionContext, "Task")), java.util.Map.of()));
        task.updateDynamicField("unbound_note", "unbound value");
        var rejected = assertThrows(DynamicFieldException.class,
                () -> task.auditAs("reject unbound definition source").save(fixture.context()));
        assertEquals("DYNAMIC_FIELD_STORAGE_PROVENANCE_MISMATCH", rejected.errorCode());
        assertTrue(jdbcSqlExecutor.queryForList("SELECT id FROM task_data WHERE status=?",
                new Object[]{"DYNAMIC-UNBOUND"}).isEmpty());
        assertFalse(task.__internalDynamicMutations().isEmpty());
        assertTrue(fixture.audits().isEmpty());
    }

    @Test
    public void newEntityDynamicIntentSurvivesIdAssignment() {
        var provider = new JdbcDynamicFieldsProvider(jdbcSqlExecutor);
        var fixture = dynamicFixture(provider);
        define(provider, "created_note", DynamicDataType.STRING);
        var task = new Task().updateTitle("new graph").updateStatus("DYNAMIC-CREATE");
        task.setDynamicFieldValues(new DynamicFieldValues(fixture.context().dynamicFields().metadata("Task"), java.util.Map.of()));
        task.updateDynamicField("created_note", "before-id");
        assertNull(task.getId());
        task.auditAs("create entity and extension together").save(fixture.context());
        assertNotNull(task.getId());
        assertEquals(Long.valueOf(1), task.getVersion());
        assertEquals(1, dynamicRowCount(task, "created_note"));
        assertEquals("before-id", task.dynamicFields().getString("created_note"));
        assertEquals(1, fixture.audits().size());
    }

    @Test
    public void unsupportedDynamicNumberPrecisionIsRejectedBeforeAnyGraphDml() {
        var provider = new JdbcDynamicFieldsProvider(jdbcSqlExecutor);
        var fixture = dynamicFixture(provider);
        define(provider, "integer_storage", DynamicDataType.NUMBER);
        Task task = dynamicTask(fixture, "DYNAMIC-PRECISION", new DynamicFieldSelection().selectNumber("integer_storage"));
        task.updateTitle("must-not-commit");
        task.updateDynamicField("integer_storage", new java.math.BigDecimal("1.25"));
        assertThrows(DynamicFieldException.class, () -> task.auditAs("reject lossy numeric persistence").save(fixture.context()));
        assertEquals("initial", nativeColumn(task, "title"));
        assertEquals(1L, ((Number) nativeColumn(task, "version")).longValue());
        assertEquals(0, dynamicRowCount(task, "integer_storage"));
        assertEquals(1, fixture.audits().size());
    }

    @Test
    public void dynamicPermissionChangeIsRevalidatedBeforeNativeWrite() {
        var provider = new JdbcDynamicFieldsProvider(jdbcSqlExecutor);
        var fixture = dynamicFixture(provider);
        var field = define(provider, "revoked_edit", DynamicDataType.STRING);
        Task task = dynamicTask(fixture, "DYNAMIC-PERMISSION", new DynamicFieldSelection().selectString("revoked_edit"));
        task.updateTitle("must-not-commit");
        task.updateDynamicField("revoked_edit", "pending");
        jdbcSqlExecutor.update("UPDATE teaql_dynamic_field_def SET editable=0 WHERE id=?", new Object[]{field.getId()});
        assertThrows(DynamicFieldException.class, () -> task.auditAs("respect updated write permission").save(fixture.context()));
        assertEquals("initial", nativeColumn(task, "title"));
        assertEquals(0, dynamicRowCount(task, "revoked_edit"));
        assertEquals(1, fixture.audits().size());
    }

    @Test
    public void explicitFixedNullAndDynamicValuePersistTogether() {
        var provider = new JdbcDynamicFieldsProvider(jdbcSqlExecutor);
        var fixture = dynamicFixture(provider);
        define(provider, "fixed_null_note", DynamicDataType.STRING);
        Task task = dynamicTask(fixture, "DYNAMIC-FIXED-NULL", new DynamicFieldSelection().selectString("fixed_null_note"));
        task.updateTitle(null);
        task.updateDynamicField("fixed_null_note", "stored");
        task.auditAs("persist explicit fixed null alongside dynamic edit").save(fixture.context());
        assertNull(nativeColumn(task, "title"));
        assertNull(task.getTitle());
        assertEquals("stored", task.dynamicFields().getString("fixed_null_note"));
    }

    @Test
    public void multiEntityDynamicGraphRollsBackAsOneUnitAndDoesNotAdoptUnrelatedContextEntity() {
        class FailingProvider extends JdbcDynamicFieldsProvider {
            int calls;
            boolean fail;
            FailingProvider() { super(jdbcSqlExecutor); }
            @Override public void saveValue(DynamicFieldContext caller, DynamicSetCommand command) {
                super.saveValue(caller, command);
                if (++calls == 2 && fail) throw new IllegalStateException("second extension write failed");
            }
        }
        var provider = new FailingProvider();
        var fixture = dynamicFixture(provider);
        define(provider, "graph_note", DynamicDataType.STRING);
        var selection = new DynamicFieldSelection().selectString("graph_note");
        Task parent = dynamicTask(fixture, "DYNAMIC-PARENT", selection);
        Task child = dynamicTask(fixture, "DYNAMIC-CHILD", selection);
        Task unrelated = dynamicTask(fixture, "DYNAMIC-UNRELATED", selection);
        LoadState shared = LoadState.projection(parent.__internalLoadState().layout(), List.of("id", "version", "title", "status"));
        parent.__internalUseLoadState(shared);
        child.__internalUseLoadState(shared);
        assertSame(parent.__internalLoadState(), child.__internalLoadState());
        parent.children = new SmartList<>();
        parent.children.add(child);
        parent.updateTitle("parent-pending");
        child.updateTitle("child-pending");
        parent.updateDynamicField("graph_note", "parent-extension");
        child.updateDynamicField("graph_note", "child-extension");
        unrelated.updateDynamicField("graph_note", "unrelated-pending");
        provider.calls = 0;
        provider.fail = true;
        assertThrows(IllegalStateException.class, () -> parent.auditAs("rollback complete related graph").save(fixture.context()));
        for (Task task : List.of(parent, child)) {
            assertEquals("initial", nativeColumn(task, "title"));
            assertEquals(1L, ((Number) nativeColumn(task, "version")).longValue());
            assertEquals(0, dynamicRowCount(task, "graph_note"));
            assertEquals(Long.valueOf(1), task.getVersion());
            assertFalse(task.__internalDynamicMutations().isEmpty());
        }
        assertEquals(3, fixture.audits().size());
        provider.fail = false;
        provider.calls = 0;
        parent.auditAs("retry related graph only").save(fixture.context());
        assertEquals(Long.valueOf(2), parent.getVersion());
        assertEquals(Long.valueOf(2), child.getVersion());
        assertEquals("parent-extension", parent.dynamicFields().getString("graph_note"));
        assertEquals("child-extension", child.dynamicFields().getString("graph_note"));
        assertEquals(5, fixture.audits().size());
        assertEquals(0, dynamicRowCount(unrelated, "graph_note"));
        assertFalse(unrelated.getEntityMutationLedger().currentChangeSet().isEmpty());
    }

    @Test
    public void dynamicFieldQueryBatchesThroughContextAndSharesOnlyActualLoadedShapes() {
        class ObservedProvider extends io.teaql.data.dynamic.InMemoryDynamicFieldsProvider {
            int batches;
            io.teaql.data.dynamic.DynamicFieldContext lastContext;
            List<io.teaql.data.dynamic.DynamicOwnerRef> lastOwners;
            @Override public java.util.Map<io.teaql.data.dynamic.DynamicOwnerRef, io.teaql.data.dynamic.DynamicFieldValues> loadValues(
                    io.teaql.data.dynamic.DynamicFieldContext caller, List<io.teaql.data.dynamic.DynamicOwnerRef> owners,
                    io.teaql.data.dynamic.DynamicFieldSelection selection) {
                batches++;
                lastContext = caller;
                lastOwners = List.copyOf(owners);
                return super.loadValues(caller, owners, selection);
            }
        }
        var provider = new ObservedProvider();
        for (String code : List.of("title", "extra")) {
            var def = new io.teaql.data.dynamic.DynamicFieldDef();
            def.setScope(io.teaql.data.dynamic.DynamicFieldScope.global());
            def.setOwnerType("Task");
            def.setCode(code);
            def.setDataType(io.teaql.data.dynamic.DynamicDataType.STRING);
            provider.registerFieldDef(def);
        }
        var local = new DefaultUserContext(runtime);
        local.putAttribute(io.teaql.data.dynamic.DynamicFieldsFacade.class.getName(),
                new io.teaql.data.dynamic.DefaultDynamicFieldsFacade(provider));
        var tasks = new ArrayList<Task>();
        for (int i = 0; i < 3; i++) {
            var task = new Task().updateTitle("fixed-" + i).updateStatus("DYNAMIC-LOAD-SHAPE");
            task.auditAs("seed dynamic query shape counterexamples").save(local);
            tasks.add(task);
        }
        var writer = local.dynamicFields().purpose("prepare stored extensions").comment("write value and explicit null");
        writer.owner("Task", tasks.get(0).getId()).string("title").set("extension");
        writer.owner("Task", tasks.get(1).getId()).string("title").set(null);

        TaskRequest request = new TaskRequest().filterByStatus("DYNAMIC-LOAD-SHAPE");
        request.setSize(3);
        request.addOrderBy("id", true);
        request.selectDynamicFieldsWith(new io.teaql.data.dynamic.DynamicFieldSelection().selectString("title"));
        var rows = request.comment("load dynamic availability counterexamples")
                .purpose("verify batch state isolation").executeForList(local);
        assertEquals(3, rows.size());
        assertEquals(1, provider.batches);
        assertEquals(3, provider.lastOwners.size());
        assertEquals("load dynamic availability counterexamples", provider.lastContext.comment());
        assertEquals("verify batch state isolation", provider.lastContext.purpose());
        Task value = rows.get(0), nil = rows.get(1), missing = rows.get(2);
        assertSame(value.__internalLoadState(), nil.__internalLoadState());
        assertNotSame(nil.__internalLoadState(), missing.__internalLoadState());
        assertSame(value.dynamicFields().metadata(), missing.dynamicFields().metadata());
        assertEquals("fixed-0", value.getTitle());
        assertEquals("extension", value.getProperty("#title"));
        assertTrue(nil.isPropertyLoaded("#title"));
        assertNull(nil.getProperty("#title"));
        assertEquals(io.teaql.data.dynamic.DynamicFieldValue.State.NULL, nil.dynamicFields().field("title").state());
        assertEquals(io.teaql.data.dynamic.DynamicFieldValue.State.NOT_LOADED, missing.dynamicFields().field("title").state());
        assertFalse(missing.isPropertyLoaded("#title"));
        assertEquals(io.teaql.data.dynamic.DynamicFieldValue.State.NOT_LOADED, value.dynamicFields().field("extra").state());
        assertTrue(rows.stream().allMatch(row -> row.getUpdatedProperties().isEmpty()));
        assertNotSame(value.getEntityMutationLedger(), nil.getEntityMutationLedger());
        LoadState shared = nil.__internalLoadState();
        value.putAdditional("#extra", "private");
        assertSame(shared, nil.__internalLoadState());
        assertNotSame(shared, value.__internalLoadState());
        assertFalse(nil.isPropertyLoaded("#extra"));
        value.addDynamicProperty("title", "derived");
        assertEquals("derived", value.getProperty("_title"));
        assertEquals("extension", value.getProperty("#title"));
        assertEquals("fixed-0", value.getProperty("title"));

        TaskRequest wrongType = new TaskRequest().filterByStatus("DYNAMIC-LOAD-SHAPE");
        wrongType.setSize(3);
        wrongType.selectDynamicFieldsWith(new io.teaql.data.dynamic.DynamicFieldSelection().selectNumber("title"));
        assertThrows(io.teaql.data.dynamic.DynamicFieldException.class, () -> wrongType
                .comment("reject incompatible extension projection").purpose("verify type boundary").executeForList(local));
        assertEquals("provider must not receive a rejected projection", 1, provider.batches);

        TaskRequest streamed = new TaskRequest().filterByStatus("DYNAMIC-LOAD-SHAPE");
        streamed.setSize(3);
        streamed.addOrderBy("id", true);
        streamed.selectDynamicFieldsWith(new io.teaql.data.dynamic.DynamicFieldSelection().selectString("title"));
        streamed.comment("stream persistent extension availability").purpose("preserve sparse wrappers after cursor cleanup");
        var streaming = (StreamingQueryExecutor) runtime.getRegistry().resolve("sqlite");
        List<Task> streamedRows;
        try (var cursor = streaming.<Task>queryForCursor(local, new DefaultQueryRequest(streamed))) {
            assertEquals("cursor creation must not hydrate extensions", 1, provider.batches);
            streamedRows = cursor.stream().toList();
        }
        assertEquals(3, streamedRows.size());
        assertEquals(2, provider.batches);
        assertEquals("stream persistent extension availability", provider.lastContext.comment());
        assertEquals("preserve sparse wrappers after cursor cleanup", provider.lastContext.purpose());
        assertSame(streamedRows.get(0).__internalLoadState(), streamedRows.get(1).__internalLoadState());
        assertNotSame(streamedRows.get(1).__internalLoadState(), streamedRows.get(2).__internalLoadState());
        assertEquals("extension", streamedRows.get(0).dynamicFields().field("title").value());
        assertEquals(DynamicFieldValue.State.NULL, streamedRows.get(1).dynamicFields().field("title").state());
        assertEquals(DynamicFieldValue.State.NOT_LOADED, streamedRows.get(2).dynamicFields().field("title").state());
        for (Task row : streamedRows) {
            assertFalse(row.__internalHasMutationLedger());
            assertEquals(DynamicFieldValue.State.NOT_LOADED, row.dynamicFields().field("extra").state());
            assertTrue(row.getUpdatedProperties().isEmpty());
        }
        // Invalid type projections still fail before invoking the provider, even on streams.
        try (var cursor = streaming.<Task>queryForCursor(local, new DefaultQueryRequest(wrongType))) {
            assertThrows(DynamicFieldException.class, () -> cursor.stream().toList());
        }
        assertEquals("rejected stream projection must not reach provider", 2, provider.batches);
    }

    @Test
    public void realSqliteFailureRetainsSafeIntentAndRecoversAfterSchemaInitialization() throws Exception {
        var logs = new ArrayList<ExecutionMetadata>();
        var ds = new SimpleDataSource("jdbc:sqlite:" + java.nio.file.Files.createTempFile("teaql-failed-query-", ".db"), "", "");
        var provider = new SqliteDataServiceExecutor("sqlite", new JdbcSqlExecutor(ds), ds);
        var local = new DefaultUserContext(TeaQLRuntime.builder().metadata(runtime.getMetadata())
                .dataService("sqlite", provider).idGenerationService(runtime.getIdGenerationService())
                .logSink((caller, metadata) -> logs.add(metadata)).build());
        assertThrows(RuntimeException.class, () -> new TaskRequest().filterByTitle("PRIVATE-QUERY-CANARY")
                .comment("read absent table").purpose("verify driver failure evidence").executeForList(local));
        var failures = logs.stream().filter(log -> "failure".equals(log.getExecutionOutcome())).toList();
        assertEquals(1, failures.size());
        var failure = failures.get(0);
        assertNull(failure.getResultCount());
        assertNull(failure.getAffectedRows());
        assertEquals("read absent table", failure.getComment());
        assertEquals("verify driver failure evidence", failure.getPurpose());
        assertTrue(failure.getDebugQuery().contains("NOT REPLAYABLE"));
        assertFalse(failure.getDebugQuery().contains("PRIVATE-QUERY-CANARY"));
        local.ensureSchema();
        var task = new Task();
        task.updateTitle("PRIVATE-QUERY-CANARY");
        task.updateStatus("RECOVERED");
        task.auditAs("verify same runtime can recover").save(local);
        var rows = new TaskRequest().filterByTitle("PRIVATE-QUERY-CANARY")
                .comment("read initialized table").purpose("verify original data retained").executeForList(local);
        assertEquals(1, rows.size());
        assertEquals("PRIVATE-QUERY-CANARY", rows.get(0).getTitle());
        assertTrue(logs.stream().anyMatch(log -> "success".equals(log.getExecutionOutcome())));
        assertTrue(logs.stream().noneMatch(log -> String.valueOf(log.getDebugQuery()).contains("PRIVATE-QUERY-CANARY")));
    }

    @Test
    public void ordinarySqlLogsExpandSafeBindingsWithoutEarlyPlaintextSql() {
        List<ExecutionMetadata> safeLogs = new ArrayList<>();
        RuntimeLogSink safeSink = (context, metadata) -> safeLogs.add(metadata);
        executeLoggedQueryAndMutation(safeSink);
        assertTrue(safeLogs.stream().anyMatch(log -> log.getOperation() == DataServiceOperation.QUERY));
        assertTrue(safeLogs.stream().anyMatch(log -> log.getOperation() == DataServiceOperation.MUTATION));
        assertTrue(safeLogs.stream().allMatch(log -> log.getParameterizedQuery() != null));
        assertTrue(safeLogs.stream().allMatch(log -> !log.getParameters().isEmpty()));
        assertTrue(safeLogs.stream().allMatch(log -> log.getDebugQuery() != null && log.getSqlOmissionReason() == null));
        assertTrue(safeLogs.stream().allMatch(log -> !log.getDebugQuery().contains("diagnostic-payload-check")));
        assertTrue(safeLogs.stream().anyMatch(log -> log.getDebugQuery().contains("'LOG-CHECK'")));

        List<ExecutionMetadata> diagnosticLogs = new ArrayList<>();
        executeLoggedQueryAndMutation(new RuntimeLogSink() {
            @Override
            public void writeExecutionLog(UserContext context, ExecutionMetadata metadata) {
                diagnosticLogs.add(metadata);
            }

            @Override
            public boolean requiresSensitiveSqlData() {
                return true;
            }
        });
        // Requesting sensitive data is not authorization: the exact environment
        // acknowledgement is also required (covered by LogPrivacyTest).
        assertFalse(diagnosticLogs.isEmpty());
        assertTrue(diagnosticLogs.stream().allMatch(log -> log.getDebugQuery() != null));
        assertTrue(diagnosticLogs.stream().allMatch(log -> !log.getDebugQuery().contains("diagnostic-payload-check")));
    }

    @Test
    public void realCrudPreservesValuesWithoutLoggingThem() throws Exception {
        var path = java.nio.file.Files.createTempFile("teaql-privacy-crud-", ".log");
        List<ExecutionMetadata> captured = new ArrayList<>();
        try (var output = new java.io.PrintStream(path.toFile())) {
            var text = new io.teaql.runtime.DefaultTextRuntimeLogSink(output);
            RuntimeLogSink sink = new RuntimeLogSink() {
                @Override public boolean requiresSensitiveSqlData() { return true; }
                @Override public void writeExecutionLog(UserContext caller, ExecutionMetadata metadata) {
                    captured.add(metadata);
                    text.writeExecutionLog(caller, metadata);
                }
            };
            var local = new DefaultUserContext(TeaQLRuntime.builder()
                    .metadata(runtime.getMetadata())
                    .dataService("sqlite", runtime.getRegistry().resolve("sqlite"))
                    .idGenerationService(runtime.getIdGenerationService()).logSink(sink).build());
            Task task = new Task();
            task.updateTitle("PRIVATE-CREATE-CANARY");
            task.updateStatus("PRIVACY-FIXTURE");
            task.auditAs("create privacy fixture").save(local);
            Task stale = new TaskRequest().filterByTitle("PRIVATE-CREATE-CANARY")
                    .comment("read fixture").purpose("verify original values").executeForList(local).get(0);
            assertEquals("PRIVATE-CREATE-CANARY", stale.getTitle());
            task.updateTitle("PRIVATE-UPDATE-CANARY");
            task.auditAs("update privacy fixture").save(local);
            stale.updateTitle("PRIVATE-FAILURE-CANARY");
            assertThrows(RuntimeException.class, () -> stale.auditAs("reject stale update").save(local));
            var rows = new TaskRequest().filterByTitle("PRIVATE-UPDATE-CANARY")
                    .comment("read updated fixture").purpose("verify failed write did not change data").executeForList(local);
            assertEquals(1, rows.size());
            assertEquals("PRIVATE-UPDATE-CANARY", rows.get(0).getTitle());
            task.markForDeletion().auditAs("delete privacy fixture").save(local);
            assertTrue(new TaskRequest().filterByTitle("PRIVATE-UPDATE-CANARY")
                    .comment("read deleted fixture").purpose("verify deletion").executeForList(local).isEmpty());
        }
        assertFalse(captured.isEmpty());
        String logs = java.nio.file.Files.readString(path);
        assertFalse(logs.isEmpty());
        for (var entry : captured) {
            logs += entry.getParameterizedQuery() + " " + entry.getParameters() + " "
                    + entry.getDebugQuery() + " " + entry.getTraceChain() + " "
                    + entry.getComment() + " " + entry.getPurpose() + " " + entry.getAuditReason();
        }
        for (String marker : List.of("PRIVATE-CREATE-CANARY", "PRIVATE-UPDATE-CANARY", "PRIVATE-FAILURE-CANARY"))
            assertFalse("sensitive marker reached log destination", logs.contains(marker));
    }

    private void executeLoggedQueryAndMutation(RuntimeLogSink sink) {
        TeaQLRuntime loggedRuntime = TeaQLRuntime.builder()
                .metadata(runtime.getMetadata())
                .dataService("sqlite", runtime.getRegistry().resolve("sqlite"))
                .idGenerationService(runtime.getIdGenerationService())
                .logSink(sink)
                .build();
        UserContext loggedContext = new DefaultUserContext(loggedRuntime);
        TaskRequest request = new TaskRequest().filterByTitle("diagnostic-payload-check");
        request.comment("what: verify SQL payload construction")
                .purpose("why: check logging sink contract")
                .executeForList(loggedContext);

        Task task = new Task();
        task.updateTitle("diagnostic-payload-check");
        task.updateStatus("LOG-CHECK");
        task.auditAs("verify logging sink contract").save(loggedContext);
    }

    @Test
    public void localDynamicSearchPreservesTrustedScopeInSqlite() {
        for (String scope : new String[] {"SEARCH-SCOPE-A", "SEARCH-SCOPE-B"}) {
            for (int i = 0; i < 3; i++) {
                Task task = new Task();
                task.updateTitle("dynamic-search-match");
                task.updateStatus(scope);
                task.auditAs("seed scoped dynamic search counterexamples").save(context);
            }
        }
        TaskRequest request = new TaskRequest().filterByStatus("SEARCH-SCOPE-A");
        request.setSize(2);
        request.addOrderBy("id", false);
        int originalHardLimit = request.hardLimit();
        var warnings = new ArrayList<io.teaql.query.json.LocalDynamicSearch.Warning>();
        var models = java.util.Map.of("Task", new io.teaql.query.json.LocalDynamicSearch.Model(
                java.util.Map.of("id", "integer", "title", "string", "status", "string"), java.util.Map.of()));
        io.teaql.query.json.LocalDynamicSearch.merge(request,
                "{\"filter\":{\"title\":\"dynamic-search-match\",\"removed\":\"SECRET\"},"
                        + "\"orderBy\":[{\"field\":\"removed\",\"direction\":\"asc\"}]}",
                models, filter -> request.createBasicSearchCriteria(filter.fieldPath(), Operator.EQUAL, filter.value().textValue()),
                order -> new OrderBy(order.fieldPath(), order.direction().toUpperCase(java.util.Locale.ROOT)), warnings::add);
        SmartList<Task> rows = request.comment("what: scoped dynamic search")
                .purpose("why: verify unknown clauses preserve server scope").executeForList(context);
        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(task -> "SEARCH-SCOPE-A".equals(task.getStatus())));
        assertTrue(rows.get(0).getId() > rows.get(1).getId());
        assertEquals(originalHardLimit, request.hardLimit());
        assertEquals(2, warnings.size());
    }

    private static UserContext context;
    private static TeaQLRuntime runtime;
    private static JdbcSqlExecutor jdbcSqlExecutor;
    private static DataSource nativeDataSource;

    public static class Task extends BaseEntity {
        public static final String __TEAQL_FIELD_LAYOUT_REVISION = "sqlite-task-fixture-v1";
        public static final java.util.Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES =
                java.util.Map.of("id", 0, "version", 1, "title", 2, "status", 3);
        public static final java.util.Map<String, List<String>> __TEAQL_FIXED_FIELD_MAPPINGS =
                java.util.Map.of("id", List.of("id", "id"), "version", List.of("version", "version"),
                        "title", List.of("title", "title"), "status", List.of("status", "status"));
        public static final java.util.Set<String> __TEAQL_FIXED_RELATION_NAMES = java.util.Set.of("children");
        public String title;
        public String status;
        public SmartList<Task> children;
        public SmartList<Task> getChildren() { return children; }

        public String getTitle() { return title; }
        public Task updateTitle(String title) {
            handleUpdate("title", this.title, title);
            this.title = title;
            return this;
        }

        public String getStatus() { return status; }
        public Task updateStatus(String status) {
            handleUpdate("status", this.status, status);
            this.status = status;
            return this;
        }

        @Override
        public String typeName() { return "Task"; }

        @Override
        public void __internalSet(String property, Object value) {
            switch (property) {
                case "title": this.title = (String) value; break;
                case "status": this.status = (String) value; break;
                case "children": this.children = (SmartList<Task>) value; break;
                default: super.__internalSet(property, value);
            }
        }

        @Override
        public Object __internalGet(String property) {
            switch (property) {
                case "title": return this.title;
                case "status": return this.status;
                case "children": return this.children;
                default: return super.__internalGet(property);
            }
        }
    }

    public static class TaskRequest extends BaseRequest<Task> {
        public TaskRequest() {
            super(Task.class);
            for (String field : List.of("id", "version", "title", "status")) selectProperty(field);
        }

        @Override
        public String getTypeName() { return "Task"; }

        public TaskRequest filterByTitle(String title) {
            appendSearchCriteria(createBasicSearchCriteria("title", Operator.EQUAL, title));
            return this;
        }

        public TaskRequest filterByStatus(String status) {
            appendSearchCriteria(createBasicSearchCriteria("status", Operator.EQUAL, status));
            return this;
        }

        public TaskRequest comment(String comment) {
            internalComment(comment);
            return this;
        }
    }

    private static class SimpleDataSource implements DataSource {
        private final String url;
        private final String user;
        private final String password;

        public SimpleDataSource(String url, String user, String password) {
            this.url = url;
            this.user = user;
            this.password = password;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, user, password);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }

        @Override public PrintWriter getLogWriter() throws SQLException { return null; }
        @Override public void setLogWriter(PrintWriter out) throws SQLException {}
        @Override public void setLoginTimeout(int seconds) throws SQLException {}
        @Override public int getLoginTimeout() throws SQLException { return 0; }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { throw new SQLFeatureNotSupportedException(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
    }

    @BeforeClass
    public static void setup() throws Exception {
        // Use embedded sqlite
        String url = "jdbc:sqlite:" + java.nio.file.Files.createTempFile("teaql-sqlite-regression-", ".db");
        String user = "";
        String password = "";

        SimpleEntityMetaFactory metaFactory = new SimpleEntityMetaFactory();

        SQLEntityDescriptor taskDescriptor = new SQLEntityDescriptor();
        taskDescriptor.setType("Task");
        taskDescriptor.setTargetType(Task.class);
        taskDescriptor.setEntitySupplier(Task::new);
        taskDescriptor.setDataService("sqlite");
        taskDescriptor.setAuditMaskFields(List.of("title"));

        io.teaql.core.sql.GenericSQLProperty idProp = (io.teaql.core.sql.GenericSQLProperty) taskDescriptor.addSimpleProperty("id", Long.class);
        idProp.setColumnType("BIGINT");
        io.teaql.core.sql.GenericSQLProperty versionProp = (io.teaql.core.sql.GenericSQLProperty) taskDescriptor.addSimpleProperty("version", Long.class);
        versionProp.setColumnType("BIGINT");
        io.teaql.core.sql.GenericSQLProperty titleProp = (io.teaql.core.sql.GenericSQLProperty) taskDescriptor.addSimpleProperty("title", String.class);
        titleProp.setColumnType("VARCHAR(200)");
        io.teaql.core.sql.GenericSQLProperty statusProp = (io.teaql.core.sql.GenericSQLProperty) taskDescriptor.addSimpleProperty("status", String.class);
        statusProp.setColumnType("VARCHAR(50)");
        
        taskDescriptor.with("table_name", "task_data");
        var children = new io.teaql.core.meta.Relation();
        children.setName("children");
        children.setOwner(taskDescriptor);
        children.setType(new io.teaql.core.meta.SimplePropertyType(SmartList.class));
        var properties = new ArrayList<>(taskDescriptor.getProperties());
        properties.add(children);
        taskDescriptor.setProperties(properties);
        metaFactory.register(taskDescriptor);
        EntityMetaFactory.registerGlobal(metaFactory);

        DataSource ds = new SimpleDataSource(url, user, password);
        nativeDataSource = ds;
        jdbcSqlExecutor = new JdbcSqlExecutor(ds);
        io.teaql.core.sqlite.SqliteDataServiceExecutor sqliteExecutor = new io.teaql.core.sqlite.SqliteDataServiceExecutor("sqlite", jdbcSqlExecutor, ds);

        AtomicLong idGen = new AtomicLong(2);
        InternalIdGenerationService idService = (c, entity) -> idGen.getAndIncrement();

        runtime = TeaQLRuntime.builder()
                .metadata(metaFactory)
                .dataService("sqlite", sqliteExecutor)
                .idGenerationService(idService)
                .build();
        
        context = new DefaultUserContext(runtime);

        // Drop existing tables for clean test state
        try {
            jdbcSqlExecutor.execute("DROP TABLE IF EXISTS task_data");
            jdbcSqlExecutor.execute("DROP TABLE IF EXISTS teaql_id_space");
        } catch (Exception e) {
            // ignore
        }

        // Ensure Schema
        context.ensureSchema();
    }

    @Test
    public void testEnsureSchemaRegistersSoundexIdempotently() {
        context.ensureSchema();
        List<java.util.Map<String, Object>> rows = jdbcSqlExecutor.queryForList(
                "SELECT soundex('Robert') AS robert, soundex('Rupert') AS rupert, soundex(NULL) AS empty",
                new Object[0]);
        assertEquals("R163", rows.get(0).get("robert"));
        assertEquals(rows.get(0).get("robert"), rows.get(0).get("rupert"));
        assertEquals("?000", rows.get(0).get("empty"));
    }

    @Test
    public void sqliteMetadataFailureDoesNotBecomeAnEmptyTable() {
        io.teaql.dataservice.sql.SqlExecutionAdapter failingAdapter =
                (io.teaql.dataservice.sql.SqlExecutionAdapter) java.lang.reflect.Proxy.newProxyInstance(
                        io.teaql.dataservice.sql.SqlExecutionAdapter.class.getClassLoader(),
                        new Class<?>[] {io.teaql.dataservice.sql.SqlExecutionAdapter.class},
                        (proxy, method, args) -> {
                            if ("queryForList".equals(method.getName())
                                    && args[0] instanceof String sql
                                    && sql.startsWith("PRAGMA table_info(")) {
                                throw new IllegalStateException("simulated SQLite metadata failure");
                            }
                            try {
                                return method.invoke(jdbcSqlExecutor, args);
                            } catch (java.lang.reflect.InvocationTargetException failure) {
                                throw failure.getCause();
                            }
                        });
        SqliteDataServiceExecutor failingExecutor =
                new SqliteDataServiceExecutor("sqlite", failingAdapter, null);
        UserContext failingContext = new DefaultUserContext(TeaQLRuntime.builder()
                .metadata(runtime.getMetadata())
                .dataService("sqlite", failingExecutor)
                .build());

        IllegalStateException failure = assertThrows(
                IllegalStateException.class, failingContext::ensureSchema);
        assertTrue(failure.getMessage().contains("task_data"));
        assertTrue(failure.getCause().getMessage().contains("simulated SQLite metadata failure"));
    }

    @Test
    public void ensureSchemaUsesContextMetadataWithoutGlobalRegistry() {
        EntityMetaFactory previous = EntityMetaFactory.get();
        try {
            EntityMetaFactory.registerGlobal(null);
            assertSame(runtime.getMetadata(), context.capability(EntityMetaFactory.class));
            context.ensureSchema();
            assertFalse(jdbcSqlExecutor.queryForList(
                    "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'task_data'",
                    new Object[0]).isEmpty());
        } finally {
            EntityMetaFactory.registerGlobal(previous);
        }
    }

    @Test
    public void queryAndMutationUseContextMetadataWithoutGlobalRegistry() {
        EntityMetaFactory previous = EntityMetaFactory.get();
        try {
            EntityMetaFactory.registerGlobal(null);
            Task task = new Task();
            task.updateTitle("context-owned-query-mutation");
            task.updateStatus("READY");
            task.auditAs("verify context-owned SQL metadata").save(context);

            SmartList<Task> rows = new TaskRequest()
                    .filterByTitle("context-owned-query-mutation")
                    .comment("what: load context-owned SQL metadata fixture")
                    .purpose("why: prove query and mutation do not use global metadata")
                    .executeForList(context);
            assertEquals(1, rows.size());
            assertEquals(task.getId(), rows.get(0).getId());
            assertEquals("READY", rows.get(0).getStatus());
        } finally {
            EntityMetaFactory.registerGlobal(previous);
        }
    }

    @AfterClass
    public static void teardown() throws Exception {
        Thread.sleep(500); // Allow asynchronous provider work to settle.
    }

    @Test
    public void testSqliteCrud() {
        context.pushTrace("SqliteIntegrationTest.testSqliteCrud");
        
        // 1. Create and Save Tasks
        Task task1 = new Task();
        task1.updateTitle("Assemble Assembly Line");
        task1.updateStatus("TODO");
        Task created = task1.auditAs("save").save(context);

        assertSame(task1, created);
        assertNotNull(task1.getId());
        assertEquals(Long.valueOf(1L), task1.getVersion());
        assertEquals("Status should transition to PERSISTED", EntityStatus.PERSISTED, task1.get$status());

        Task task2 = new Task();
        task2.updateTitle("Write Integration Tests");
        task2.updateStatus("TODO");
        task2.auditAs("save").save(context);

        // 2. Query Tasks by criteria
        TaskRequest req = new TaskRequest().filterByTitle("Assemble Assembly Line");
        SmartList<Task> resultList = req.comment("test").purpose("test").executeForList(context);
        assertEquals(1, resultList.size());
        assertEquals("Assemble Assembly Line", resultList.get(0).getTitle());

        // Test filter no results
        TaskRequest reqEmpty = new TaskRequest().filterByTitle("Clean up workspace");
        assertTrue(reqEmpty.comment("test").purpose("test").executeForList(context).isEmpty());

        // 3. Update task
        task1.updateStatus("DONE");
        Task updated = task1.auditAs("save").save(context);
        assertSame(task1, updated);
        assertEquals(Long.valueOf(2L), updated.getVersion());

        TaskRequest reqDone = new TaskRequest().filterByStatus("DONE");
        SmartList<Task> resultDone = reqDone.comment("test").purpose("test").executeForList(context);
        assertEquals(1, resultDone.size());
        assertEquals("Assemble Assembly Line", resultDone.get(0).getTitle());

        // 4. Delete task
        Task deleted = task1.markForDeletion().auditAs("delete").save(context);
        assertSame(task1, deleted);
        assertEquals(Long.valueOf(-3L), deleted.getVersion());
        assertEquals(EntityStatus.PERSISTED_DELETED, deleted.get$status());

        SmartList<Task> resultAfterDelete = new TaskRequest().filterByStatus("DONE").comment("test").purpose("test").executeForList(context);
        assertTrue(resultAfterDelete.isEmpty());
    }

    @Test
    public void temporalDebugSqlMatchesPreparedSqliteStorage() throws Exception {
        String sql = "INSERT INTO temporal_fixture VALUES (?, ?, ?) /* ignored ? */";
        Object[] args = {
            1L,
            LocalDate.of(2024, 2, 29),
            LocalDateTime.of(2026, 8, 19, 2, 3, 4, 123_000_000)
        };
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            connection.createStatement().execute(
                    "CREATE TABLE temporal_fixture(id INTEGER, d TEXT, local_time TEXT)");
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, (Long) args[0]);
                statement.setString(2, args[1].toString());
                statement.setString(3, args[2].toString().replace('T', ' '));
                statement.executeUpdate();
            }
            String literal = SqlDataServiceExecutor.debugSql(sql, args)
                    .replaceFirst("VALUES \\(1,", "VALUES (2,");
            connection.createStatement().executeUpdate(literal);
            try (ResultSet rows = connection.createStatement().executeQuery(
                    "SELECT d, local_time, typeof(d), typeof(local_time) "
                            + "FROM temporal_fixture ORDER BY id")) {
                for (int i = 0; i < 2; i++) {
                    assertTrue(rows.next());
                    assertEquals("2024-02-29", rows.getString(1));
                    assertEquals("2026-08-19 02:03:04.123", rows.getString(2));
                    assertEquals("text", rows.getString(3));
                    assertEquals("text", rows.getString(4));
                }
                assertFalse(rows.next());
            }
        }
    }
}
