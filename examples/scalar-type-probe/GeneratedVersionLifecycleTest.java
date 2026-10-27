package com.example.scalartypeconformanceservice;

import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.*;
import io.teaql.core.checker.CheckException;
import io.teaql.jackson.TeaQLModule;
import io.teaql.runtime.TeaQLRuntime;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Generated Q/E/audited Save with exact real SQLite version endpoints. */
public class GeneratedVersionLifecycleTest {
    static void injectStoredVersion(GeneratedCanonicalNameTest.Fixture f, long id, long version) throws Exception {
        // Deliberate boundary fault injection after the normal generated create.
        try (var connection = DriverManager.getConnection(f.data.url);
             var statement = connection.prepareStatement("UPDATE scalar_type_probe_data SET version=? WHERE id=? AND version=1")) {
            statement.setLong(1, version); statement.setLong(2, id); assertEquals(1, statement.executeUpdate());
        }
    }
    static void reject(GeneratedCanonicalNameTest.Fixture f, ScalarTypeProbe entity, String pending) {
        long calls = f.data.calls; int checks = f.checked.size(), policies = f.policies.size(), audits = f.audits.size();
        long version = entity.getVersion(); var status = entity.get$status(); Long baseline = entity.getOriginalVersion();
        var error = assertThrows(CheckException.class, () -> entity.auditAs("Reject exhausted optimistic version before customer code").save(f.context));
        assertEquals(calls, f.data.calls); assertEquals(checks, f.checked.size());
        assertEquals(policies, f.policies.size()); assertEquals(audits, f.audits.size());
        assertEquals("VERSION_EXHAUSTED", error.getViolates().get(0).getRuleId().name());
        assertEquals("/version", error.getViolates().get(0).getLocation().instancePath());
        assertTrue(error.getCause() instanceof MutationVersionException);
        assertEquals(Long.valueOf(version), entity.getVersion()); assertEquals(status, entity.get$status());
        // A completed save clears ledger originals; in that case the current
        // authoritative version is the baseline. Rejection must preserve both.
        assertEquals(baseline, entity.getOriginalVersion());
        if (pending != null) {
            assertEquals(Long.valueOf(version), baseline); // fresh Q hydration
            assertEquals(pending, E.scalarTypeProbe(entity).getRequiredString().eval());
            assertEquals(pending, entity.getEntityMutationLedger().get(new EntityKey("ScalarTypeProbe", entity.getId()), "required_string"));
        }
    }
    @Test public void exactVersionsThroughGeneratedQueryExpressionJsonAndAuditedSave() throws Exception {
        int round = Integer.parseInt(System.getenv("TEAQL_NAME_ROUND"));
        var expected = Path.of(System.getProperty("teaql.expected.runtime.root")).toRealPath();
        for (var type : List.of(MutationVersions.class, TeaQLRuntime.class, TeaQLModule.class))
            assertTrue(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().startsWith(expected));
        var f = new GeneratedCanonicalNameTest.Fixture("jdbc:sqlite:" + Path.of(System.getenv("TEAQL_NAME_DB")).toAbsolutePath(), round);
        var root = f.root("Version boundary root " + round);
        var mapper = new ObjectMapper().findAndRegisterModules().registerModule(new TeaQLModule(f.context));
        var liveIds = new ArrayList<Long>(); var deletedIds = new ArrayList<Long>(); var recoveredIds = new ArrayList<Long>();
        for (String variant : List.of("null", "value")) {
            String name = "Version boundary " + round + " " + variant;
            var seed = f.probe(root, name).updateOptionalString(variant.equals("null") ? null : "retained note");
            seed.auditAs("Create a complete version-boundary fixture").save(f.context);
            injectStoredVersion(f, seed.getId(), Long.MAX_VALUE - 1);
            var live = f.load(seed.getId()); live.updateRequiredString(name + " final");
            live.auditAs("Commit the last representable live update").save(f.context);
            assertEquals(Long.valueOf(Long.MAX_VALUE), live.getVersion());
            f.oracle(live.getId(), name + " final", Long.MAX_VALUE, root.getId());
            var loaded = f.load(live.getId());
            assertEquals(name + " final", E.scalarTypeProbe(loaded).getRequiredString().eval());
            assertEquals(variant.equals("null") ? null : "retained note", E.scalarTypeProbe(loaded).getOptionalString().eval());
            var json = mapper.valueToTree(loaded);
            assertTrue(json.get("version").isTextual()); assertEquals(Long.toString(Long.MAX_VALUE), json.get("version").asText());
            long calls = f.data.calls; int checks = f.checked.size(), policies = f.policies.size(), audits = f.audits.size();
            loaded.auditAs("Leave an unchanged exhausted object alone").save(f.context);
            assertEquals(calls, f.data.calls); assertEquals(checks, f.checked.size());
            assertEquals(policies, f.policies.size()); assertEquals(audits, f.audits.size());
            loaded.updateRequiredString(name + " pending"); reject(f, loaded, name + " pending");
            f.oracle(live.getId(), name + " final", Long.MAX_VALUE, root.getId()); liveIds.add(live.getId());

            var deleteSeed = f.probe(root, name + " delete");
            deleteSeed.auditAs("Create independent deletion fixture after a rejected save").save(f.context);
            injectStoredVersion(f, deleteSeed.getId(), Long.MAX_VALUE);
            var deleted = f.load(deleteSeed.getId()); deleted.markForDeletion();
            deleted.auditAs("Delete the largest live version without intermediate overflow").save(f.context);
            assertEquals(Long.valueOf(Long.MIN_VALUE), deleted.getVersion());
            assertEquals(EntityStatus.PERSISTED_DELETED, deleted.get$status());
            f.oracle(deleted.getId(), name + " delete", Long.MIN_VALUE, root.getId());
            deleted.markAsRecover(); reject(f, deleted, null); deletedIds.add(deleted.getId());

            var recoverSeed = f.probe(root, name + " recover");
            recoverSeed.auditAs("Create an independent recoverable boundary fixture").save(f.context);
            var tree = mapper.valueToTree(recoverSeed);
            ((com.fasterxml.jackson.databind.node.ObjectNode) tree).put("version", Long.toString(Long.MIN_VALUE + 2));
            injectStoredVersion(f, recoverSeed.getId(), Long.MIN_VALUE + 2);
            var recovered = mapper.treeToValue(tree, ScalarTypeProbe.class);
            assertEquals(EntityStatus.PERSISTED_DELETED, recovered.get$status());
            recovered.markAsRecover(); recovered.auditAs("Recover the last representable tombstone").save(f.context);
            assertEquals(Long.valueOf(Long.MAX_VALUE), recovered.getVersion());
            assertEquals(name + " recover", E.scalarTypeProbe(f.load(recovered.getId())).getRequiredString().eval());
            f.oracle(recovered.getId(), name + " recover", Long.MAX_VALUE, root.getId()); recoveredIds.add(recovered.getId());
            assertEquals(name + " pending", E.scalarTypeProbe(loaded).getRequiredString().eval()); assertTrue(loaded.needPersist());
        }
        try (var connection = DriverManager.getConnection(f.data.url);
             var statement = connection.prepareStatement("SELECT name,version FROM type_platform_data WHERE id=?")) {
            statement.setLong(1, root.getId()); try (var rows = statement.executeQuery()) {
                assertTrue(rows.next()); assertEquals("Version boundary root " + round, rows.getString(1)); assertEquals(1L, rows.getLong(2));
            }
        }
        var report = new LinkedHashMap<String, Object>();
        report.put("passed", true); report.put("profile", "camelCase"); report.put("round", round); report.put("rootId", root.getId());
        report.put("liveIds", liveIds); report.put("deletedIds", deletedIds); report.put("recoveredIds", recoveredIds);
        for (String flag : List.of("lastUpdateExact", "maxUpdateRejectedBeforeChecker", "zeroProviderPolicyAudit", "pendingIntentRetained",
                "typedQAndE", "exactJsonVersion", "unchangedMaxNoop", "untouchedParent", "legalDeleteAtMax", "exhaustedRecoveryRejected", "lastRecoveryExact")) report.put(flag, true);
        report.put("fullExampleInventoryAccepted", false); report.put("internalArtifactAccepted", false);
        System.out.println("JAVA_VERSION_LIFECYCLE=" + mapper.writeValueAsString(report));
    }
}
