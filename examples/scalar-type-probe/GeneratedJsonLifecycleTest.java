package com.example.scalartypeconformanceservice;

import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.EntityStatus;
import io.teaql.core.MutationOperationKind;
import io.teaql.core.checker.CheckException;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.jackson.TeaQLModule;
import io.teaql.runtime.TeaQLRuntime;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real Q -> JSON -> mutation -> SQLite, with no edits to the generated library. */
public class GeneratedJsonLifecycleTest {
    @Test public void decodedExistingGraphsUpdateConflictDeleteAndRecover() throws Exception {
        var profile = JsonFieldNamingProfile.fromModelValue(System.getenv("TEAQL_JSON_PROFILE"));
        int round = Integer.parseInt(System.getenv("TEAQL_NAME_ROUND"));
        var expected = Path.of(System.getProperty("teaql.expected.runtime.root")).toRealPath();
        for (var type : List.of(TeaQLRuntime.class, TeaQLModule.class))
            assertTrue(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().startsWith(expected));
        var f = new GeneratedCanonicalNameTest.Fixture("jdbc:sqlite:" + Path.of(System.getenv("TEAQL_NAME_DB")).toAbsolutePath(), round);
        var root = f.root("JSON lifecycle " + profile + " round " + round);
        var created = f.probe(root, "lifecycle " + round);
        created.auditAs("Persist the JSON lifecycle probe").save(f.context);
        var loaded = Q.scalarTypeProbes().withIdIs(created.getId()).limit(1)
                .selectTypePlatformWith(Q.typePlatformsWithMinimalFields())
                .comment("Load the complete probe and a partial reference")
                .purpose("Verify decoded lifecycle without loading untouched reference fields").executeForOne(f.context);
        var mapper = new ObjectMapper().findAndRegisterModules().registerModule(new TeaQLModule(f.context));
        long codecCalls = f.data.calls;
        String json = mapper.writeValueAsString(loaded);
        var first = mapper.readValue(json, ScalarTypeProbe.class);
        var stale = mapper.readValue(json, ScalarTypeProbe.class);
        var unrelated = mapper.readValue(json, ScalarTypeProbe.class);
        for (var value : List.of(first, stale, unrelated)) {
            assertEquals(EntityStatus.PERSISTED, value.get$status());
            assertFalse(value.needPersist()); assertFalse(value.__internalHasMutationLedger());
            assertEquals(Long.valueOf(1), value.getOriginalVersion());
            assertEquals(root.getId(), E.scalarTypeProbe(value).getTypePlatform().getId().eval());
            var reference = E.scalarTypeProbe(value).getTypePlatform().eval();
            assertFalse(reference.isPropertyLoaded("name"));
            assertFalse(reference.needPersist());
            assertFalse(reference.__internalHasMutationLedger());
        }
        assertSame(first.__internalLoadState(), stale.__internalLoadState());
        assertEquals(codecCalls, f.data.calls);
        first.updateRequiredString("lifecycle " + round + " updated");
        unrelated.updateRequiredString("must remain unpersisted");
        assertNotSame(first.getEntityMutationLedger(), unrelated.getEntityMutationLedger());
        int policies = f.policies.size(), audits = f.audits.size();
        first.auditAs("Update one independently decoded existing object").save(f.context);
        assertEquals(Long.valueOf(2), first.getVersion()); assertEquals(EntityStatus.PERSISTED, first.get$status());
        assertEquals(1, f.policies.size() - policies);
        var plan = f.policies.get(policies);
        assertEquals(1, plan.operations().size());
        assertEquals(MutationOperationKind.UPDATE, plan.operations().get(0).kind());
        assertEquals("ScalarTypeProbe", plan.operations().get(0).entity().entity());
        assertEquals(1, f.audits.size() - audits);
        assertTrue(unrelated.needPersist()); assertEquals(Long.valueOf(1), unrelated.getVersion());
        f.oracle(created.getId(), "lifecycle " + round + " updated", 2, root.getId());
        var reloaded = f.load(created.getId());
        assertEquals("lifecycle " + round + " updated", E.scalarTypeProbe(reloaded).getRequiredString().eval());

        stale.updateRequiredString("stale decoded overwrite");
        int beforeConflictAudits = f.audits.size();
        var conflict = assertThrows(RuntimeException.class,
                () -> stale.auditAs("Reject a stale independently decoded version").save(f.context));
        Throwable cause = conflict;
        while (cause.getCause() != null) cause = cause.getCause();
        assertEquals("ConcurrentModifyException", cause.getClass().getSimpleName());
        assertEquals(Long.valueOf(1), stale.getVersion()); assertTrue(stale.needPersist());
        assertEquals(beforeConflictAudits, f.audits.size());
        f.oracle(created.getId(), "lifecycle " + round + " updated", 2, root.getId());

        var partial = Q.scalarTypeProbesWithMinimalFields().withIdIs(created.getId()).selectRequiredString().limit(1)
                .comment("Load a partial existing JSON object").purpose("Reject incomplete modified objects before database access")
                .executeForOne(f.context);
        var partialDecoded = mapper.readValue(mapper.writeValueAsString(partial), ScalarTypeProbe.class);
        assertFalse(partialDecoded.needPersist()); partialDecoded.updateRequiredString("incomplete overwrite");
        long beforePartialSave = f.data.calls;
        assertThrows(CheckException.class, () -> partialDecoded.auditAs("Reject a modified partial decoded object").save(f.context));
        assertEquals(beforePartialSave, f.data.calls);
        assertEquals(Long.valueOf(2), partialDecoded.getVersion()); assertTrue(partialDecoded.needPersist());

        var deleted = mapper.readValue(mapper.writeValueAsString(reloaded), ScalarTypeProbe.class);
        deleted.markForDeletion(); deleted.auditAs("Delete the decoded existing object").save(f.context);
        assertEquals(Long.valueOf(-3), deleted.getVersion()); assertEquals(EntityStatus.PERSISTED_DELETED, deleted.get$status());
        f.oracle(created.getId(), "lifecycle " + round + " updated", -3, root.getId());
        long beforeDeletedCodec = f.data.calls;
        var recovered = mapper.readValue(mapper.writeValueAsString(deleted), ScalarTypeProbe.class);
        assertEquals(EntityStatus.PERSISTED_DELETED, recovered.get$status());
        assertFalse(recovered.needPersist()); assertFalse(recovered.__internalHasMutationLedger());
        assertEquals(Long.valueOf(-3), recovered.getOriginalVersion()); assertEquals(beforeDeletedCodec, f.data.calls);
        recovered.markAsRecover(); recovered.auditAs("Recover the decoded deleted object").save(f.context);
        assertEquals(Long.valueOf(4), recovered.getVersion()); assertEquals(EntityStatus.PERSISTED, recovered.get$status());
        f.oracle(created.getId(), "lifecycle " + round + " updated", 4, root.getId());
        assertEquals("lifecycle " + round + " updated", E.scalarTypeProbe(f.load(created.getId())).getRequiredString().eval());
        try (var connection = DriverManager.getConnection(f.data.url);
             var statement = connection.prepareStatement("SELECT version FROM type_platform_data WHERE id=?")) {
            statement.setLong(1, root.getId());
            try (var rows = statement.executeQuery()) { assertTrue(rows.next()); assertEquals(1L, rows.getLong(1)); assertFalse(rows.next()); }
        }
        var report = new LinkedHashMap<String, Object>();
        report.put("passed", true); report.put("profile", profile.modelValue()); report.put("round", round);
        report.put("id", created.getId()); report.put("rootId", root.getId()); report.put("finalVersion", 4);
        for (var name : List.of("cleanDecode", "updateNotInsert", "independentLedgers", "staleConflict", "failedIntentRetained",
                "partialRejectedBeforeProvider", "untouchedPartialReference", "deleteRecover", "zeroProviderCodec")) report.put(name, true);
        System.out.println("JAVA_JSON_LIFECYCLE=" + new ObjectMapper().writeValueAsString(report));
    }
}
