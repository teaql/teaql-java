package com.example.schoolmanagementservice;

import com.example.schoolmanagementservice.school.School;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.teaql.jackson.TeaQLModule;
import io.teaql.core.BaseEntity;
import io.teaql.core.FieldLayout;
import io.teaql.core.LoadState;
import io.teaql.core.UserContext;
import io.teaql.core.checker.CheckException;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sql.portable.TeaQLDatabase;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.dataservice.sql.SqlExecutionAdapter;
import io.teaql.data.dynamic.DefaultDynamicFieldsFacade;
import io.teaql.data.dynamic.DynamicDataType;
import io.teaql.data.dynamic.DynamicFieldContext;
import io.teaql.data.dynamic.DynamicFieldException;
import io.teaql.data.dynamic.DynamicFieldDef;
import io.teaql.data.dynamic.DynamicFieldScope;
import io.teaql.data.dynamic.DynamicFieldSelection;
import io.teaql.data.dynamic.DynamicFieldValue;
import io.teaql.data.dynamic.DynamicFieldsFacade;
import io.teaql.data.dynamic.jdbc.JdbcDynamicFieldsProvider;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.jupiter.api.Assertions.*;

/** Runtime-owned acceptance code; generated APIs come from current model-aware Assist. */
class GeneratedSchoolLoadStateTest {
    static final class Context extends DefaultUserContext implements SchoolManagementServiceUserContext {
        Context(TeaQLRuntime runtime) { super(runtime); }
    }

    @Test
    void generatedSchoolUsesLocalRuntimeAndSharedState() throws Exception {
        String dependencyMode = System.getenv().getOrDefault("TEAQL_RUNTIME_DEPENDENCY_MODE", "workspace");
        assertTrue(List.of("workspace", "registry").contains(dependencyMode), "Unknown dependency verification mode");
        Path root = Path.of(System.getenv(dependencyMode.equals("registry")
                ? "TEAQL_ARTIFACT_CACHE" : "TEAQL_LOCAL_RUNTIME_ROOT")).toRealPath();
        for (Class<?> type : List.of(BaseEntity.class, LoadState.class, TeaQLRuntime.class,
                JdbcSqlExecutor.class, SqliteDataServiceExecutor.class, TeaQLModule.class, School.class)) {
            Path location = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
            assertTrue(location.startsWith(root), "Non-local dependency " + type + " from " + location);
            if (dependencyMode.equals("registry")) {
                assertTrue(location.toString().endsWith(".jar"), "Expected isolated downloaded JAR: " + location);
                if (type.getName().startsWith("io.teaql.")) {
                    String version = System.getenv("TEAQL_CANDIDATE_VERSION");
                    assertNotNull(version); assertTrue(location.toString().contains("/" + version + "/"));
                }
                System.out.println("ARTIFACT_CLASS " + type.getName() + " " + location);
            } else {
                assertTrue(location.endsWith(Path.of("target/classes")), "Expected reactor classes: " + location);
                System.out.println("LOCAL_CLASS " + type.getName() + " " + location);
            }
        }
        String round = System.getenv("TEAQL_LOAD_STATE_ROUND");
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + System.getenv("TEAQL_LOAD_STATE_DATABASE"));
        ObservedJdbcExecutor sql = new ObservedJdbcExecutor(dataSource);
        IdSpaceIdGenerator ids = new IdSpaceIdGenerator(new DatabaseBridge(sql));
        ids.ensureIdSpaceTable();
        var service = new SqliteDataServiceExecutor("sqlite", sql, dataSource);
        TeaQLRuntime runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                .dataService("default", service).dataService("sqlite", service)
                .idGenerationService(ids).build().install(GeneratedRuntimeModule.module());
        UserContext context = new Context(runtime);
        context.ensureSchema();
        context.ensureSchema();
        var constants = Q.schoolTypes().orderByIdAscending().limit(10)
                .comment("what: read model-owned constants").purpose("why: verify repeat bootstrap")
                .executeForList(context);
        assertEquals(2, constants.size());
        assertEquals(1001L, constants.get(0).getId());
        assertEquals(1002L, constants.get(1).getId());
        var platform = Q.platforms().withIdIs(1L).limit(1)
                .comment("what: reuse the model-owned root").purpose("why: attach the test Schools")
                .executeForOne(context);
        assertNotNull(platform);
        String first = round + " First School";
        String second = round + " Second School";
        for (String name : List.of(first, second)) {
            var school = Q.schools().comment("what: allocate a controlled School")
                    .purpose("why: verify the generated mutation contract").newEntity(context);
            school.updatePlatform(platform);
            school.updateSchoolTypeToPrimary();
            school.updateName(name);
            school.updateAddress("Private row address");
            school.updateEstablishedDate(LocalDate.of(1995, 9, 1));
            school.updateStudentCapacity(0);
            school.updateActive(false);
            school.updateCreateTime(LocalDateTime.of(2023, 11, 14, 22, 13));
            school.updateUpdateTime(LocalDateTime.of(2023, 11, 14, 22, 13));
            school.auditAs("Create controlled shared-state probe").save(context);
        }
        var full = Q.schools().withNameIn(first, second).selectSelfFields()
                .orderByIdAscending().limit(2).comment("what: read compatible full projections")
                .purpose("why: inspect exact immutable state references").executeForList(context);
        assertEquals(2, full.size());
        LoadState snapshot = full.get(0).__internalLoadState();
        assertSame(snapshot, full.get(1).__internalLoadState());
        LoadStateAllocationTest.verifyGeneratedSharing(full);
        FieldLayout layout = FieldLayout.forType(School.class);
        assertSame(layout, snapshot.layout());
        assertEquals(School.__TEAQL_FIELD_LAYOUT_REVISION, layout.revision());
        School.__TEAQL_FIXED_FIELD_INDEXES.forEach((name, index) -> assertEquals(index, layout.findIndex(name)));
        boolean wide = Boolean.parseBoolean(System.getenv("TEAQL_LOAD_STATE_WIDE"));
        if (wide) {
            assertTrue(School.__TEAQL_FIXED_FIELD_INDEXES.size() > 130);
            for (int index : new int[]{0, 31, 32, 63, 64, 65, 129}) {
                String name = School.__TEAQL_FIXED_FIELD_INDEXES.entrySet().stream()
                        .filter(entry -> entry.getValue() == index).findFirst().orElseThrow().getKey();
                assertTrue(full.get(0).isPropertyLoaded(name), "missing generated slot " + index);
                if (index < 64) assertNotEquals(0L, snapshot.bits() & (1L << index));
                else assertTrue(snapshot.overflow().contains(index));
                if (index >= 31) {
                    assertTrue(name.startsWith("probe_"), "expected nullable boundary probe");
                    assertNull(full.get(0).__internalGet(School.__TEAQL_FIXED_FIELD_MAPPINGS.get(name).get(0)));
                }
            }
            String name = School.__TEAQL_FIXED_FIELD_INDEXES.entrySet().stream()
                    .filter(entry -> entry.getValue() == 64).findFirst().orElseThrow().getKey();
            LoadState detached = snapshot.withLoaded(name, false);
            assertNotSame(snapshot, detached);
            assertFalse(detached.isLoaded(name));
            assertTrue(full.get(1).isPropertyLoaded(name));
            System.out.println("WIDE generated Java fields=" + School.__TEAQL_FIXED_FIELD_INDEXES.size()
                    + " bit63/overflow64/65/129 loaded NULL and COW");
        }
        assertEquals(0, E.school(full.get(0)).getStudentCapacity().eval());
        assertEquals(false, E.school(full.get(0)).isActive().eval());
        assertEquals(1001L, full.get(0).getSchoolType().getId());
        GeneratedFieldOrderAcceptance.verify(context, dataSource, full.get(0).getId());
        GeneratedPageStreamAcceptance.verify(context, first, second);
        GeneratedPropertyMetadataAcceptance.verify(context, first, second);

        var sparse = Q.schoolsWithMinimalFields().withNameIn(first, second).selectName()
                .orderByIdAscending().limit(2).comment("what: read matching sparse projections")
                .purpose("why: preserve separate field availability boundaries").executeForList(context);
        assertEquals(2, sparse.size());
        LoadState sparseState = sparse.get(0).__internalLoadState();
        assertSame(sparseState, sparse.get(1).__internalLoadState());
        assertNotSame(snapshot, sparseState);
        assertTrue(sparse.get(0).isPropertyLoaded("id"));
        assertTrue(sparse.get(0).isPropertyLoaded("version"));
        assertFalse(sparse.get(0).isPropertyLoaded("address"));
        var mapper = new ObjectMapper().registerModule(new TeaQLModule(context))
                .registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var sparseJson = mapper.readTree(mapper.writeValueAsString(sparse));
        assertEquals(first, sparseJson.get(0).get("name").asText());
        assertFalse(sparseJson.get(0).has("address"), "NotLoaded must not become serialized NULL");
        assertFalse(sparseJson.get(0).has("establishedDate"));
        assertSame(sparseState, sparse.get(0).__internalLoadState());
        var sparseRestored = mapper.treeToValue(sparseJson.get(0), School.class);
        assertEquals(first, E.school(sparseRestored).getName().eval());
        assertFalse(sparseRestored.isPropertyLoaded("address"));
        assertFalse(sparseRestored.__internalHasMutationLedger());
        assertTrue(sparseRestored.getUpdatedProperties().isEmpty());
        var sparseSecondRestored = mapper.treeToValue(sparseJson.get(1), School.class);
        assertSame(sparseRestored.__internalLoadState(), sparseSecondRestored.__internalLoadState());
        assertEquals(sparseJson.get(0), mapper.readTree(mapper.writeValueAsString(sparseRestored)));
        var fullJson = mapper.readTree(mapper.writeValueAsString(full));
        assertEquals("1995-09-01", fullJson.get(0).get("establishedDate").asText());
        assertEquals(0, fullJson.get(0).get("studentCapacity").asInt());
        assertFalse(fullJson.get(0).get("active").asBoolean());
        assertSame(snapshot, full.get(0).__internalLoadState());
        var fullRestored = mapper.treeToValue(fullJson.get(0), School.class);
        assertEquals(0, E.school(fullRestored).getStudentCapacity().eval());
        assertEquals(false, E.school(fullRestored).isActive().eval());
        assertEquals(LocalDate.of(1995, 9, 1), fullRestored.getEstablishedDate());
        assertEquals(1001L, fullRestored.getSchoolType().getId());
        assertFalse(fullRestored.__internalHasMutationLedger());
        assertTrue(fullRestored.getUpdatedProperties().isEmpty());
        assertEquals(fullJson.get(0), mapper.readTree(mapper.writeValueAsString(fullRestored)));
        System.out.println("PASS generated Java typed native JSON roundtrip and snapshot sharing " + round);
        if (wide) {
            for (int index : new int[]{31, 32, 63, 64, 65, 129}) {
                String name = School.__TEAQL_FIXED_FIELD_INDEXES.entrySet().stream()
                        .filter(entry -> entry.getValue() == index).findFirst().orElseThrow().getKey();
                assertFalse(sparse.get(0).isPropertyLoaded(name), "sparse slot " + index + " must remain NotLoaded");
            }
        }
        assertEquals(first, E.school(sparse.get(0)).getName().eval());
        sparse.get(0).updateAddress("New private address");
        assertNotSame(sparseState, sparse.get(0).__internalLoadState());
        assertSame(sparseState, sparse.get(1).__internalLoadState());
        assertFalse(sparse.get(1).isPropertyLoaded("address"));
        long[] beforeRejectedSave = sql.counts();
        assertThrows(CheckException.class, () -> sparse.get(0)
                .auditAs("Reject sparse whole-object update").save(context));
        assertArrayEquals(beforeRejectedSave, sql.counts(),
                "Checker must reject before read, write or transaction provider entry");
        assertFalse(sparse.get(0).isPropertyLoaded("active"),
                "checker cannot promote an absent false default into loaded state");
        assertFalse(sparse.get(0).isPropertyLoaded("establishedDate"));
        assertTrue(sparse.get(0).isPropertyLoaded("address"), "the explicit caller mutation must remain loaded");
        assertSame(sparseState, sparse.get(1).__internalLoadState(), "validation cannot widen a sibling projection");
        System.out.println("PASS generated Java Checker preserves NotLoaded and sibling load boundaries");

        var changed = full.get(0);
        Long originalVersion = changed.getVersion();
        String renamed = round + " Renamed School";
        changed.updateName(renamed);
        assertSame(snapshot, changed.__internalLoadState());
        assertEquals(second, full.get(1).getName());
        changed.auditAs("Rename only one independently loaded row").save(context);
        long[] afterAcceptedSave = sql.counts();
        assertTrue(afterAcceptedSave[0] > beforeRejectedSave[0], "accepted save must exercise observed reads");
        assertTrue(afterAcceptedSave[1] > beforeRejectedSave[1], "accepted save must exercise observed writes");
        assertTrue(afterAcceptedSave[2] > beforeRejectedSave[2], "accepted save must exercise observed transactions");
        System.out.println("PASS generated Java sparse Checker rejects before provider entry; positive save observed");
        assertEquals(originalVersion + 1, changed.getVersion());
        GeneratedIndependentMutationAcceptance.verify(context, changed.getId(), renamed, second);
        var related = Q.schools().withIdIs(changed.getId())
                .selectPlatformWith(Q.platformsWithMinimalFields().selectName())
                .selectSchoolTypeWith(Q.schoolTypesWithMinimalFields().selectCode())
                .limit(1).comment("what: read the renamed School graph")
                .purpose("why: verify generated Q and typed E relation traversal").executeForOne(context);
        assertEquals("Campus Learning Platform", E.school(related).getPlatform().eval().getName());
        assertEquals("PRIMARY", E.school(related).getSchoolType().eval().getCode());
        GeneratedNestedGraphAssertions.verify(context, changed.getId(), renamed, second, mapper);
        var provider = new JdbcDynamicFieldsProvider(sql);
        provider.ensureSchema();
        String extension = "note_" + round;
        DynamicFieldContext definitionContext = new DynamicFieldContext() {
            public String scopeType() { return "GLOBAL"; }
            public String scopeId() { return "default"; }
            public String userId() { return "generated-example"; }
            public String purpose() { return "register controlled example definitions"; }
            public String comment() { return "what: register one persistent extension"; }
            public boolean strictIntent() { return true; }
            public long nextId(String type) { return ids.nextId(type); }
        };
        DynamicFieldDef definition = new DynamicFieldDef();
        definition.setScope(DynamicFieldScope.global());
        definition.setOwnerType("School");
        definition.setCode(extension);
        definition.setName(extension);
        definition.setDataType(DynamicDataType.STRING);
        provider.registerFieldDef(definitionContext, definition);
        context.putAttribute(DynamicFieldsFacade.class.getName(), new DefaultDynamicFieldsFacade(provider));
        var extensionRequest = Q.schools().withIdIs(changed.getId());
        extensionRequest.selectDynamicFieldsWith(new DynamicFieldSelection().selectString(extension));
        var extended = extensionRequest.limit(1).comment("what: load persistent extension definitions")
                .purpose("why: qualify the generated carrier and local provider").executeForOne(context);
        assertEquals(DynamicFieldValue.State.NOT_LOADED, extended.dynamicFields().field(extension).state());
        assertNull(layout.findIndex("#" + extension));
        extended.addDynamicProperty("name", "readonly-derived-name");
        assertNull(layout.findIndex("_name"));
        assertEquals(renamed, E.school(extended).getName().eval());
        assertEquals("readonly-derived-name", extended.getProperty("_name"));
        assertNull(extended.getProperty("_missing_property"));
        var beforeExtensionSave = mapper.readTree(mapper.writeValueAsString(extended));
        assertEquals(renamed, beforeExtensionSave.get("name").asText());
        assertEquals("readonly-derived-name", beforeExtensionSave.get("_name").asText());
        assertFalse(beforeExtensionSave.has("#" + extension));
        extended.updateDynamicField(extension, "Private extension");
        extended.auditAs("Persist the controlled extension through governed save").save(context);
        assertEquals("Private extension", extended.dynamicFields().field(extension).value());
        var afterExtensionSave = mapper.readTree(mapper.writeValueAsString(extended));
        assertEquals("Private extension", afterExtensionSave.get("#" + extension).asText());
        assertEquals(renamed, afterExtensionSave.get("name").asText());
        assertFalse(afterExtensionSave.has("traceChain"));
        assertFalse(afterExtensionSave.has("loadState"));
        System.out.println("PASS generated Java namespace serialization and NotLoaded boundary");
        GeneratedNestedGraphAssertions.verifyDynamic(context, changed.getId(), renamed, second, extension, mapper);
        var heldVersion = extended.getVersion();
        extended.updateDynamicField(extension, "must not cross storage profile");
        context.putAttribute(DynamicFieldsFacade.class.getName(),
                new DefaultDynamicFieldsFacade(provider, DynamicFieldScope.of("PROFILE", "other")));
        var rejected = assertThrows(DynamicFieldException.class,
                () -> extended.auditAs("Reject held dynamic view in another profile").save(context));
        assertEquals("DYNAMIC_FIELD_STORAGE_PROVENANCE_MISMATCH", rejected.errorCode());
        assertEquals(heldVersion, extended.getVersion());
        assertFalse(extended.__internalDynamicMutations().isEmpty());
        context.putAttribute(DynamicFieldsFacade.class.getName(),
                new DefaultDynamicFieldsFacade(new JdbcDynamicFieldsProvider(sql)));
        var stored = context.dynamicFields().comment("what: inspect original extension after rejected save")
                .purpose("why: prove a changed profile cannot redirect held data")
                .owner(extended.typeName(), extended.getId())
                .readAll(new DynamicFieldSelection().selectString(extension));
        assertEquals("Private extension", stored.getString(extension));
        extended.updateDynamicField(extension, null);
        extended.auditAs("Persist explicit extension NULL").save(context);
        assertEquals(DynamicFieldValue.State.NULL, extended.dynamicFields().field(extension).state());
        extended.deleteDynamicField(extension);
        extended.auditAs("Delete the extension without confusing it with NULL").save(context);
        assertEquals(DynamicFieldValue.State.NOT_LOADED, extended.dynamicFields().field(extension).state());
        assertEquals("readonly-derived-name", extended.getProperty("_name"));
        System.out.println("PASS generated Java dynamic storage provenance and retry");
        var combined = GeneratedDynamicRollbackAcceptance.verify(context, sql, extended, renamed, second, extension, provider, definitionContext);
        String deletionName = combined.getName();
        GeneratedNamespaceCowAcceptance.verify(context,combined.getId(),deletionName,second,extension,provider,definitionContext,mapper);
        GeneratedNativeRollbackAcceptance.verify(context, dataSource, sql, deletionName, second);
        GeneratedMaterializationAcceptance.verify(context, combined.getId(), deletionName, second, round);
        // Partial related objects are valid for E reads, not for whole-graph Checker validation.
        var forDeletion = Q.schools().withIdIs(changed.getId()).limit(1)
                .comment("what: reload the full native School before deletion")
                .purpose("why: preserve whole-object Checker validation without partial child projections")
                .executeForOne(context);
        assertEquals(deletionName, E.school(forDeletion).getName().eval());
        assertNull(forDeletion.getProperty("_name"), "readonly dynamic properties must not be persisted by save");
        forDeletion.markForDeletion().auditAs("Soft-delete the selected probe").save(context);
        var remaining = Q.schools().withNameIn(deletionName, second).orderByIdAscending().limit(2)
                .comment("what: inspect the surviving sibling").purpose("why: verify mutation isolation")
                .executeForList(context);
        assertEquals(1, remaining.size());
        assertEquals(second, remaining.get(0).getName());
        // The combined control explicitly seeds the companion's NULL extension once.
        assertEquals(full.get(1).getVersion() + 1, remaining.get(0).getVersion());
        System.out.println("PASS generated Java indexed Q/E/Checker/create/update/delete and snapshot sharing " + round);
    }

    /** Framework ID-allocation bridge, not an application-owned persistence alternative. */
    record DatabaseBridge(SqlExecutionAdapter sql) implements TeaQLDatabase {
        public List<Map<String, Object>> query(String query, Object[] args) { return sql.queryForList(query, args); }
        public int executeUpdate(String query, Object[] args) { return sql.update(query, args); }
        public int[] batchUpdate(String query, List<Object[]> args) { return sql.batchUpdate(query, args); }
        public void execute(String query) { sql.execute(query); }
        public void executeInTransaction(Runnable action) { sql.executeInTransaction(action); }
        public List<Map<String, Object>> getTableColumns(String table) {
            throw new UnsupportedOperationException("Schema inspection belongs to the dialect");
        }
    }
}
