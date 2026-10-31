package com.example.scalartypeconformanceservice;

import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.*;
import io.teaql.core.value.TeaQLNotLoadedException;
import io.teaql.data.dynamic.*;
import io.teaql.data.dynamic.jdbc.JdbcDynamicFieldsProvider;
import java.nio.file.Path;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real generated Q/E/audited saves, including a transaction owned by the caller. */
public class GeneratedRestorationViewTest {
    private static GeneratedCanonicalNameTest.Fixture fixture(String name) throws Exception {
        var expected = Path.of(System.getProperty("teaql.expected.runtime.root")).toRealPath();
        for (var type : List.of(BaseEntity.class, io.teaql.runtime.TeaQLRuntime.class,
                io.teaql.provider.jdbc.JdbcSqlExecutor.class, io.teaql.core.sqlite.SqliteDataServiceExecutor.class)) {
            var origin = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
            assertTrue("local reactor origin: " + type.getName(), origin.startsWith(expected));
        }
        int round = Integer.parseInt(System.getenv("TEAQL_NAME_ROUND"));
        return new GeneratedCanonicalNameTest.Fixture("jdbc:sqlite:"
                + Path.of(System.getenv("TEAQL_NAME_DB") + ".views-" + name + ".sqlite").toAbsolutePath(), round);
    }

    private static ScalarTypeProbe sparse(GeneratedCanonicalNameTest.Fixture f, long id) {
        var query = Q.scalarTypeProbesWithMinimalFields().withIdIs(id).selectRequiredString()
                .selectOptionalString().limit(1)
                .selectTypePlatformWith(Q.typePlatformsWithMinimalFields().selectName());
        query.setDynamicPropertyMetadata(new DynamicPropertyMetadata(Map.of(
                "_zero", Number.class, "_null", String.class, "_missing", Long.class)));
        query.addSimpleDynamicProperty("_zero", new PropertyReference("optional_integer"));
        query.addSimpleDynamicProperty("_null", new PropertyReference("optional_string"));
        return query.comment("Load a sparse view with typed readonly results")
                .purpose("Retain projection and null presence through an unchanged save").executeForOne(f.context);
    }

    private static void assertView(ScalarTypeProbe view, String name) {
        assertEquals(name, E.scalarTypeProbe(view).getRequiredString().eval());
        assertNull(E.scalarTypeProbe(view).getOptionalString().eval());
        assertThrows(TeaQLNotLoadedException.class, () -> E.scalarTypeProbe(view).getRequiredLong().eval());
        assertEquals("View root", E.scalarTypeProbe(view).getTypePlatform().getName().eval());
        assertSame(Long.class, view.getDynamicPropertyType("_missing"));
        assertNull(view.getDynamicProperty("_missing")); assertFalse(view.hasDynamicProperty("_missing"));
        assertTrue(view.hasDynamicProperty("_null")); assertNull(view.getDynamicProperty("_null"));
        assertEquals(0, ((Number) view.getDynamicProperty("_zero")).intValue());
        assertTrue(view.getUpdatedProperties().isEmpty());
    }

    @Test public void unchangedSparseViewRetainsReadonlyPresenceAndRelations() throws Exception {
        var f = fixture("readonly"); var root = f.root("View root");
        var created = f.probe(root, "View row").updateOptionalInteger(0);
        created.auditAs("Seed the sparse unchanged view").save(f.context);
        var view = sparse(f, created.getId()); var state = view.__internalLoadState();
        var relation = view.getTypePlatform();
        long calls = f.data.calls; int policies = f.policies.size(), audits = f.audits.size();
        view.updateRequiredString("temporary"); view.updateRequiredString("View row");
        view.auditAs("Save a restored sparse view without refreshing it").save(f.context);
        assertEquals(calls, f.data.calls); assertEquals(policies, f.policies.size()); assertEquals(audits, f.audits.size());
        assertSame(state, view.__internalLoadState()); assertSame(relation, view.getTypePlatform());
        assertView(view, "View row"); assertEquals(Long.valueOf(1), view.getVersion());
        var foreign = root.__internalLoadState();
        assertThrows(IllegalArgumentException.class, () -> view.__internalUseLoadState(foreign));
        assertSame(state, view.__internalLoadState()); assertView(view, "View row");
        f.oracle(created.getId(), "View row", 1, root.getId());
        System.out.println("JAVA_RESTORATION_VIEW=" + new ObjectMapper().writeValueAsString(Map.of(
                "passed", true, "case", "readonly-sparse-noop", "round", Integer.parseInt(System.getenv("TEAQL_NAME_ROUND")),
                "id", created.getId(), "version", view.getVersion(), "providerDelta", f.data.calls - calls)));
    }

    @Test public void callerOwnsTransactionAuditAndRetryAfterRollback() throws Exception {
        var f = fixture("transaction"); var root = f.root("View root");
        var clean = f.probe(root, "Outer unchanged").updateOptionalInteger(0);
        clean.auditAs("Seed an unchanged outer transaction participant").save(f.context);
        var dirty = f.probe(root, "Outer original");
        dirty.auditAs("Seed an independently edited transaction participant").save(f.context);
        var view = sparse(f, clean.getId()); var state = view.__internalLoadState();
        dirty = f.load(dirty.getId()); dirty.updateRequiredString("Outer retry");
        final var edited = dirty; int audits = f.audits.size(), policies = f.policies.size();
        var error = assertThrows(IllegalStateException.class, () -> f.sql.executeInTransaction(() -> {
            long calls = f.data.calls;
            view.auditAs("A no-op must not finish the caller transaction").save(f.context);
            assertEquals(calls, f.data.calls); assertTrue(f.sql.hasActiveTransaction());
            assertSame(state, view.__internalLoadState()); assertView(view, "Outer unchanged");
            edited.auditAs("Roll back an independent real edit").save(f.context);
            assertEquals("audit must wait for outer commit", audits, f.audits.size());
            throw new IllegalStateException("INTENTIONAL_OUTER_ROLLBACK");
        }));
        assertEquals("INTENTIONAL_OUTER_ROLLBACK", error.getMessage());
        assertFalse(f.sql.hasActiveTransaction()); assertEquals(audits, f.audits.size());
        f.oracle(edited.getId(), "Outer original", 1, root.getId());
        assertEquals(Long.valueOf(1), edited.getVersion());
        assertTrue(edited.getUpdatedProperties().contains("required_string"));
        assertEquals(Long.valueOf(1), edited.getEntityMutationLedger().getOriginalVersion(
                new EntityKey("ScalarTypeProbe", edited.getId())));
        edited.auditAs("Retry original optimistic intent after outer rollback").save(f.context);
        assertEquals(audits + 1, f.audits.size()); assertEquals(Long.valueOf(2), edited.getVersion());
        f.oracle(edited.getId(), "Outer retry", 2, root.getId());
        edited.updateOptionalString("Outer committed");
        f.sql.executeInTransaction(() -> {
            long calls = f.data.calls;
            view.auditAs("Keep the unchanged sparse view inside the commit scope").save(f.context);
            assertEquals(calls, f.data.calls);
            edited.auditAs("Commit only the genuinely changed object").save(f.context);
            assertEquals(audits + 1, f.audits.size()); assertTrue(f.sql.hasActiveTransaction());
        });
        assertEquals(audits + 2, f.audits.size()); assertEquals(policies + 3, f.policies.size());
        assertSame(state, view.__internalLoadState()); assertView(view, "Outer unchanged");
        assertEquals(Long.valueOf(1), view.getVersion()); assertEquals(Long.valueOf(3), edited.getVersion());
        assertTrue(edited.getUpdatedProperties().isEmpty());
        f.oracle(clean.getId(), "Outer unchanged", 1, root.getId());
        f.oracle(edited.getId(), "Outer retry", 3, root.getId());
        System.out.println("JAVA_RESTORATION_VIEW=" + new ObjectMapper().writeValueAsString(Map.of(
                "passed", true, "case", "caller-transaction-rollback-retry-commit", "round", Integer.parseInt(System.getenv("TEAQL_NAME_ROUND")),
                "unchangedId", clean.getId(), "editedId", edited.getId(), "auditDelta", f.audits.size() - audits,
                "policyDelta", f.policies.size() - policies, "finalVersion", edited.getVersion())));
    }

    @Test public void persistentFieldsKeepNullMissingProvenanceAndIndependentIntent() throws Exception {
        var f = fixture("dynamic"); var root = f.root("View root");
        int round = Integer.parseInt(System.getenv("TEAQL_NAME_ROUND"));
        var ids = new java.util.concurrent.atomic.AtomicLong(round * 10000L);
        var provider = new JdbcDynamicFieldsProvider(f.sql); provider.ensureSchema();
        DynamicFieldContext definitionContext = new DynamicFieldContext() {
            public String scopeType() { return "GLOBAL"; }
            public String scopeId() { return "default"; }
            public String userId() { return "restoration-example"; }
            public String purpose() { return "Verify persistent extension view geometry"; }
            public String comment() { return "Register controlled extension definitions"; }
            public boolean strictIntent() { return true; }
            public long nextId(String type) { return ids.incrementAndGet(); }
        };
        String selected = "selected_" + round, hidden = "hidden_" + round;
        for (String code : List.of(selected, hidden)) {
            var def = new DynamicFieldDef(); def.setScope(DynamicFieldScope.global());
            def.setOwnerType("ScalarTypeProbe"); def.setCode(code); def.setName(code); def.setDataType(DynamicDataType.STRING);
            provider.registerFieldDef(definitionContext, def);
        }
        f.context.putAttribute(DynamicFieldsFacade.class.getName(),
                new DefaultDynamicFieldsFacade(provider).withIdGenerator(type -> ids.incrementAndGet()));
        var first = f.probe(root, "Dynamic value"); var second = f.probe(root, "Dynamic null");
        first.auditAs("Seed the extension value owner").save(f.context);
        second.auditAs("Seed the extension null owner").save(f.context);
        var all = new DynamicFieldSelection().selectString(selected).selectString(hidden);
        for (var owner : List.of(first, second)) {
            var request = Q.scalarTypeProbes().withIdIs(owner.getId()).limit(1);
            request.selectDynamicFieldsWith(all);
            var loaded = request.comment("Load extension definitions before setting values")
                    .purpose("Seed extensions through normal audited mutation").executeForOne(f.context);
            loaded.updateDynamicField(selected, owner == first ? "baseline" : null);
            loaded.updateDynamicField(hidden, "retain hidden");
            loaded.auditAs("Persist controlled extension values").save(f.context);
        }
        var children = Q.scalarTypeProbes().orderByIdAscending().limit(10);
        children.selectDynamicFieldsWith(new DynamicFieldSelection().selectString(selected));
        var graph = Q.typePlatforms().withIdIs(root.getId()).limit(1).selectScalarTypeProbeListWith(children)
                .comment("Load both extension presence states in one graph")
                .purpose("Share immutable geometry while retaining private row values").executeForOne(f.context);
        var rows = graph.getScalarTypeProbeList(); assertEquals(2, rows.size());
        var value = rows.get(0); var nullRow = rows.get(1); var state = value.__internalLoadState();
        assertSame(state, nullRow.__internalLoadState());
        assertEquals("baseline", value.dynamicFields().field(selected).value());
        assertEquals(DynamicFieldValue.State.NULL, nullRow.dynamicFields().field(selected).state());
        for (var row : rows.getData()) {
            assertEquals(DynamicFieldValue.State.NOT_LOADED, row.dynamicFields().field(hidden).state());
            assertTrue(row.dynamicFields().metadata().matchesStorage(f.sql.storageIdentity(), "GLOBAL", "default", "ScalarTypeProbe"));
        }
        var metadata = value.dynamicFields().metadata(); var selection = value.dynamicFields().selectedCodes();
        long calls = f.data.calls; int policies = f.policies.size(), audits = f.audits.size();
        value.updateRequiredString("temporary"); value.updateRequiredString("Dynamic value");
        value.auditAs("Retain persistent metadata through a no-op").save(f.context);
        assertEquals(calls, f.data.calls); assertEquals(policies, f.policies.size()); assertEquals(audits, f.audits.size());
        assertSame(state, value.__internalLoadState()); assertSame(metadata, value.dynamicFields().metadata());
        assertEquals(selection, value.dynamicFields().selectedCodes());
        value.updateDynamicField(selected, "real extension edit");
        value.updateRequiredString("temporary again"); value.updateRequiredString("Dynamic value");
        value.auditAs("Keep a real extension edit after native restoration").save(f.context);
        assertEquals(policies + 1, f.policies.size()); assertEquals(audits + 1, f.audits.size());
        assertEquals(Long.valueOf(3), value.getVersion());
        assertEquals("real extension edit", value.dynamicFields().field(selected).value());
        assertEquals(DynamicFieldValue.State.NOT_LOADED, value.dynamicFields().field(hidden).state());
        assertEquals(DynamicFieldValue.State.NULL, nullRow.dynamicFields().field(selected).state());
        assertSame(state, nullRow.__internalLoadState()); assertEquals(Long.valueOf(2), nullRow.getVersion());
        var check = Q.scalarTypeProbes().withIdIs(first.getId()).limit(1); check.selectDynamicFieldsWith(all);
        var stored = check.comment("Reload stored extensions independently")
                .purpose("Prove hidden values and explicit null survive a separate native restoration").executeForOne(f.context);
        assertEquals("retain hidden", stored.dynamicFields().field(hidden).value());
        assertEquals("real extension edit", stored.dynamicFields().field(selected).value());
        f.oracle(first.getId(), "Dynamic value", 3, root.getId());
        System.out.println("JAVA_RESTORATION_VIEW=" + new ObjectMapper().writeValueAsString(Map.of(
                "passed", true, "case", "persistent-dynamic-noop-and-real-edit", "round", round,
                "id", value.getId(), "nullId", nullRow.getId(), "finalVersion", value.getVersion())));
    }
}
