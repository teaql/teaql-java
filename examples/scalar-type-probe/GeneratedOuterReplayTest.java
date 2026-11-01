package com.example.scalartypeconformanceservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.*;
import io.teaql.core.checker.CheckException;
import io.teaql.core.checker.CheckResult;
import io.teaql.data.dynamic.*;
import io.teaql.data.dynamic.jdbc.JdbcDynamicFieldsProvider;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/** A physical rollback must retain the latest intent, not the first save's intent alone. */
public class GeneratedOuterReplayTest {
    private GeneratedCanonicalNameTest.Fixture fixture(String kind) throws Exception {
        var expected = Path.of(System.getProperty("teaql.expected.runtime.root")).toRealPath();
        for (var type : List.of(BaseEntity.class, io.teaql.runtime.TeaQLRuntime.class,
                io.teaql.provider.jdbc.JdbcSqlExecutor.class, io.teaql.core.sqlite.SqliteDataServiceExecutor.class)) {
            var origin = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
            assertTrue("local reactor origin: " + type.getName(), origin.startsWith(expected));
        }
        int round = Integer.parseInt(System.getenv("TEAQL_NAME_ROUND"));
        var file = Path.of(System.getenv("TEAQL_NAME_DB") + ".outer-replay-" + kind + ".sqlite").toAbsolutePath();
        return new GeneratedCanonicalNameTest.Fixture("jdbc:sqlite:" + file, round);
    }

    private void verify(String kind) throws Exception {
        int round = Integer.parseInt(System.getenv("TEAQL_NAME_ROUND"));
        var f = fixture(kind);
        var root = f.root("Outer replay root"); var prepared = f.probe(root, "Outer replay original");
        boolean create = kind.equals("new-then-update");
        if (!create) prepared.auditAs("Seed the outer replay owner").save(f.context);
        var row = create ? prepared : f.load(prepared.getId());
        Long idBeforeFirstSave = row.getId();
        int audits = f.audits.size(); var assigned = new long[]{-1};
        var error = assertThrows(IllegalStateException.class, () -> f.sql.executeInTransaction(() -> {
            row.updateRequiredString("Outer replay first");
            row.auditAs("Stage the first version inside the caller transaction").save(f.context);
            assigned[0] = row.getId();
            assertEquals(Long.valueOf(create ? 1 : 2), row.getVersion());
            if (kind.equals("same-native-field")) row.updateRequiredString("Outer replay latest");
            else row.updateOptionalString("Outer replay latest note");
            if (!kind.equals("unsaved-native-field")) {
                row.auditAs("Stage another edit before the caller decides to roll back").save(f.context);
                assertEquals(Long.valueOf(create ? 2 : 3), row.getVersion());
            }
            assertEquals(audits, f.audits.size());
            throw new IllegalStateException("INTENTIONAL_OUTER_REPLAY_ROLLBACK");
        }));
        assertEquals("INTENTIONAL_OUTER_REPLAY_ROLLBACK", error.getMessage());
        assertFalse(f.sql.hasActiveTransaction()); assertEquals(audits, f.audits.size());
        long id = assigned[0]; assertTrue(id > 0); var key = new EntityKey("ScalarTypeProbe", id);
        assertEquals(Long.valueOf(id), row.getId());
        assertEquals(create ? null : Long.valueOf(1), row.getVersion());
        if (create) {
            assertEquals(EntityStatus.NEW, row.get$status()); assertTrue(row.getEntityMutationLedger().isNew(key));
            try (var connection = DriverManager.getConnection(f.data.url);
                    var query = connection.prepareStatement("SELECT count(*) FROM scalar_type_probe_data WHERE id=?")) {
                query.setLong(1, id); try (var result = query.executeQuery()) { assertTrue(result.next()); assertEquals(0, result.getInt(1)); }
            }
        } else {
            f.oracle(id, "Outer replay original", 1, root.getId());
            assertEquals(Long.valueOf(1), row.getEntityMutationLedger().getOriginalVersion(key));
        }
        String expectedName = kind.equals("same-native-field") ? "Outer replay latest" : "Outer replay first";
        assertEquals("later saved intent must survive rollback", expectedName, E.scalarTypeProbe(row).getRequiredString().eval());
        if (!kind.equals("same-native-field")) {
            assertEquals("later saved or unsaved note must survive rollback", "Outer replay latest note", E.scalarTypeProbe(row).getOptionalString().eval());
            assertTrue(row.getUpdatedProperties().contains("optional_string"));
        }
        assertTrue(row.getUpdatedProperties().contains("required_string"));
        row.auditAs("Retry all final intent against the original database version").save(f.context);
        long version = create ? 1 : 2;
        assertEquals(Long.valueOf(version), row.getVersion()); assertEquals(audits + 1, f.audits.size());
        f.oracle(id, expectedName, version, root.getId());
        try (var connection = DriverManager.getConnection(f.data.url);
                var query = connection.prepareStatement("SELECT optional_string FROM scalar_type_probe_data WHERE id=?")) {
            query.setLong(1, id); try (var result = query.executeQuery()) {
                assertTrue(result.next()); assertEquals(kind.equals("same-native-field") ? null : "Outer replay latest note", result.getString(1)); assertFalse(result.next());
            }
        }
        System.out.println("JAVA_OUTER_REPLAY=" + new ObjectMapper().writeValueAsString(Map.of(
                "passed", true, "case", kind, "round", round, "id", id, "version", version, "auditDelta", f.audits.size()-audits,
                "idAssignedBeforeFirstSave", idBeforeFirstSave != null)));
    }

    @Test public void differentNativeFields() throws Exception { verify("different-native-fields"); }
    @Test public void sameNativeField() throws Exception { verify("same-native-field"); }
    @Test public void unsavedNativeField() throws Exception { verify("unsaved-native-field"); }
    @Test public void newThenUpdate() throws Exception { verify("new-then-update"); }

    private void verifyDynamic(boolean unsaved) throws Exception {
        String kind = unsaved ? "dynamic-saved-unsaved" : "dynamic-then-native";
        int round = Integer.parseInt(System.getenv("TEAQL_NAME_ROUND"));
        var f = fixture(kind); var root = f.root("Outer replay root");
        var seed = f.probe(root, "Outer dynamic original"); seed.auditAs("Seed the outer dynamic owner").save(f.context);
        var provider = new JdbcDynamicFieldsProvider(f.sql); provider.ensureSchema();
        var ids = new java.util.concurrent.atomic.AtomicLong(10000L * round);
        DynamicFieldContext definitions = new DynamicFieldContext() {
            public String scopeType() { return "GLOBAL"; }
            public String scopeId() { return "default"; }
            public String userId() { return "outer-replay"; }
            public String purpose() { return "Verify dynamic retry across physical rollback"; }
            public String comment() { return "Register the controlled replay extensions"; }
            public boolean strictIntent() { return true; }
            public long nextId(String type) { return ids.incrementAndGet(); }
        };
        String edited = "edited_" + round, hidden = "hidden_" + round;
        for (String code : List.of(edited, hidden)) {
            var def = new DynamicFieldDef(); def.setScope(DynamicFieldScope.global()); def.setOwnerType("ScalarTypeProbe");
            def.setCode(code); def.setName(code); def.setDataType(DynamicDataType.STRING); provider.registerFieldDef(definitions, def);
        }
        f.context.putAttribute(DynamicFieldsFacade.class.getName(), new DefaultDynamicFieldsFacade(provider).withIdGenerator(type -> ids.incrementAndGet()));
        var all = new DynamicFieldSelection().selectString(edited).selectString(hidden);
        var load = Q.scalarTypeProbes().withIdIs(seed.getId()).limit(1); load.selectDynamicFieldsWith(all);
        var initial = load.comment("Load both extension definitions").purpose("Seed all extensions through audited save").executeForOne(f.context);
        initial.updateDynamicField(edited, "original extension"); initial.updateDynamicField(hidden, "retain hidden extension");
        initial.auditAs("Seed the selected and unselected extension controls").save(f.context);
        var selected = Q.scalarTypeProbes().withIdIs(seed.getId()).limit(1);
        selected.selectDynamicFieldsWith(new DynamicFieldSelection().selectString(edited));
        var row = selected.comment("Load only the extension that will be modified")
                .purpose("Preserve the unselected stored value across retry").executeForOne(f.context);
        int audits = f.audits.size();
        var error = assertThrows(IllegalStateException.class, () -> f.sql.executeInTransaction(() -> {
            row.updateDynamicField(edited, "saved extension"); row.auditAs("Stage a dynamic edit").save(f.context);
            row.updateRequiredString("Outer dynamic latest"); row.auditAs("Stage a later native edit").save(f.context);
            if (unsaved) row.updateDynamicField(edited, "unsaved extension");
            assertEquals(audits, f.audits.size()); throw new IllegalStateException("INTENTIONAL_DYNAMIC_REPLAY_ROLLBACK");
        }));
        assertEquals("INTENTIONAL_DYNAMIC_REPLAY_ROLLBACK", error.getMessage());
        f.oracle(row.getId(), "Outer dynamic original", 2, root.getId());
        assertEquals(Long.valueOf(2), row.getVersion()); assertEquals(audits, f.audits.size());
        assertEquals("Outer dynamic latest", E.scalarTypeProbe(row).getRequiredString().eval());
        String expected = unsaved ? "unsaved extension" : "saved extension";
        assertEquals(expected, row.dynamicFields().field(edited).value());
        assertEquals(DynamicFieldValue.State.NOT_LOADED, row.dynamicFields().field(hidden).state());
        assertTrue(row.dynamicFields().metadata().matchesStorage(f.sql.storageIdentity(), "GLOBAL", "default", "ScalarTypeProbe"));
        assertEquals("original extension", row.__internalDynamicOriginalValues().get(edited).value());
        row.auditAs("Retry native and dynamic final intent together").save(f.context);
        f.oracle(row.getId(), "Outer dynamic latest", 3, root.getId()); assertEquals(audits + 1, f.audits.size());
        var reload = Q.scalarTypeProbes().withIdIs(row.getId()).limit(1); reload.selectDynamicFieldsWith(all);
        var stored = reload.comment("Reload both stored extensions after retry")
                .purpose("Verify persistence without exposing the hidden field to mutation").executeForOne(f.context);
        assertEquals(expected, stored.dynamicFields().field(edited).value());
        assertEquals("retain hidden extension", stored.dynamicFields().field(hidden).value());
        System.out.println("JAVA_OUTER_REPLAY=" + new ObjectMapper().writeValueAsString(Map.of(
                "passed", true, "case", kind, "round", round, "id", row.getId(), "version", row.getVersion(), "auditDelta", f.audits.size()-audits)));
    }

    @Test public void dynamicThenNative() throws Exception { verifyDynamic(false); }
    @Test public void dynamicSavedAndUnsaved() throws Exception { verifyDynamic(true); }

    @Test public void rawAdmissionIsNotBypassedByRollback() throws Exception {
        int round = Integer.parseInt(System.getenv("TEAQL_NAME_ROUND")); var f = fixture("raw-admission");
        var root = f.root("Outer replay root"); var seed = f.probe(root, "Outer raw original");
        seed.auditAs("Seed the raw admission control").save(f.context); var row = f.load(seed.getId());
        var key = new EntityKey("ScalarTypeProbe", row.getId()); int audits = f.audits.size();
        var error = assertThrows(IllegalStateException.class, () -> f.sql.executeInTransaction(() -> {
            row.updateRequiredString("Outer raw latest"); row.auditAs("Stage a valid native edit").save(f.context);
            row.getEntityMutationLedger().set(key, "unknown_field", "PRIVATE_ROLLBACK_CANARY");
            row.getEntityMutationLedger().set(key, "optional_string", null);
            throw new IllegalStateException("INTENTIONAL_RAW_REPLAY_ROLLBACK");
        }));
        assertEquals("INTENTIONAL_RAW_REPLAY_ROLLBACK", error.getMessage());
        assertTrue(row.getEntityMutationLedger().containsField(key, "unknown_field"));
        assertTrue(row.getEntityMutationLedger().containsField(key, "optional_string"));
        assertNull(row.getEntityMutationLedger().get(key, "optional_string"));
        long calls = f.data.calls; int policies = f.policies.size();
        var rejection = assertThrows(CheckException.class, () -> row.auditAs("Retain invalid raw input for the normal model admission").save(f.context));
        assertTrue(rejection.getViolates().stream().anyMatch(value -> value.getRuleId() == CheckResult.RuleId.UNKNOWN_FIELD));
        assertFalse(rejection.getMessage().contains("PRIVATE_ROLLBACK_CANARY"));
        assertEquals(calls, f.data.calls); assertEquals(policies, f.policies.size()); assertEquals(audits, f.audits.size());
        assertEquals(Long.valueOf(1), row.getVersion()); f.oracle(row.getId(), "Outer raw original", 1, root.getId());
        System.out.println("JAVA_OUTER_REPLAY=" + new ObjectMapper().writeValueAsString(Map.of(
                "passed", true, "case", "raw-admission", "round", round, "id", row.getId(), "version", row.getVersion(), "auditDelta", 0)));
    }
}
