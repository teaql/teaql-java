package io.teaql.examples.inheritance;

import com.example.inheritancematchingservice.*;
import com.example.inheritancematchingservice.academy.Academy;
import com.example.inheritancematchingservice.school.School;
import io.teaql.core.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sqlite.SingleConnectionDataSource;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.core.postgres.PostgresDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Same natural-save lifecycle as the Rust example; never inserts a parent row. */
public class InheritanceLifecycleExampleTest {
    private static final class FixtureIds implements InternalIdGenerationService {
        long next = 10000;
        @Override public Long generateId(UserContext context, Entity entity) { return next++; }
        @Override public long nextId(String type) { return next++; }
        @Override public void ensureFloor(String type, long floor) { next = Math.max(next, floor + 1); }
    }

    private static <T extends Entity> T save(T entity, UserContext context, String comment) {
        return entity.<T>auditAs(comment).save(context);
    }

    private static Academy child(UserContext context, long id) {
        return Q.academies().withIdIs(id).limit(1).comment("Load one complete inherited object")
                .purpose("Verify the shared lifecycle through the typed child view").executeForOne(context);
    }

    private static School parent(UserContext context, long id) {
        return Q.schools().withIdIs(id).limit(1).comment("Load one complete parent view")
                .purpose("Verify inherited values and the authoritative root version").executeForOne(context);
    }

    @Test public void naturalLifecycleThroughGenericAuditedSave() throws Exception {
        String provider = System.getProperty("teaql.inheritance.provider", "sqlite");
        int round = Integer.parseInt(System.getProperty("teaql.inheritance.round"));
        assertTrue(Set.of("sqlite", "postgres").contains(provider));
        for (var component : List.of(BaseRequest.class, TeaQLRuntime.class, JdbcSqlExecutor.class))
            assertTrue(component.getName(), Path.of(component.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath()
                    .startsWith(Path.of(System.getProperty("teaql.expected.runtime.root")).toRealPath()));
        assertTrue(Path.of(Q.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath()
                .startsWith(Path.of(System.getProperty("teaql.expected.inheritance.root")).toRealPath()));
        SingleConnectionDataSource sqlite = provider.equals("sqlite") ? new SingleConnectionDataSource(
                "jdbc:sqlite:" + System.getProperty("teaql.inheritance.database")) : null;
        try {
            javax.sql.DataSource source = sqlite;
            if (provider.equals("postgres")) {
                var pg = new org.postgresql.ds.PGSimpleDataSource();
                pg.setURL(System.getenv("TEAQL_TEST_INHERITANCE_JDBC_URL"));
                pg.setUser(System.getenv("TEAQL_TEST_POSTGRES_USER"));
                pg.setPassword(System.getenv("TEAQL_TEST_POSTGRES_PASSWORD")); source = pg;
            }
            assertNotNull(source);
            var driver = new JdbcSqlExecutor(source); var ids = new FixtureIds();
            MutationPolicy observer = new MutationPolicy() {
                public MutationPolicyIdentity identity() { return new MutationPolicyIdentity("inheritance-example","1","source-fixture"); }
                public MutationDecision review(UserContext ignored, MutationPlan plan) {
                    if ("Recover and correct both declaring tables atomically".equals(plan.auditReason())) {
                        assertEquals(1,plan.operations().size());
                        assertEquals(MutationOperationKind.RECOVER,plan.operations().get(0).kind());
                        assertEquals("Corrected recovered academy",plan.operations().get(0).changedValues().get("name"));
                        assertEquals("WEST",plan.operations().get(0).changedValues().get("campus_code"));
                    }
                    return MutationDecision.allow();
                }
            };
            var metadata = new SimpleEntityMetaFactory();
            var runtime = TeaQLRuntime.builder().metadata(metadata)
                    .dataService("default", provider.equals("sqlite")
                            ? new SqliteDataServiceExecutor("default", driver, source)
                            : new PostgresDataServiceExecutor("default", driver))
                    .idGenerationService(ids).mutationPolicyRegistry(key -> Optional.of(observer)).build().install(GeneratedRuntimeModule.module());
            // Keep the deliberately quoted legacy storage mapping in application
            // assembly so the generated library remains exact producer output.
            metadata.resolveEntityDescriptor("Academy").findProperty("campus_code")
                    .with("column", "Legacy Campus Code").with("columnName", "Legacy Campus Code");
            UserContext context = new DefaultUserContext(runtime);
            context.ensureSchema(); context.ensureSchema();
            var auditEvents = new ArrayList<SafeAuditEvent>();
            context.putAttribute(AppAuditEventSink.class.getName(),(AppAuditEventSink)(ignored,event)->auditEvents.add(event));
            // Deliberately collide only the numeric ID with a constant entity.
            ids.next = 1000 + round;
            var platform = Q.platformsWithMinimalFields().withIdIs(1L).limit(1)
                    .comment("Reuse the provisioned root").purpose("Never seed a second Platform").executeForOne(context);
            var academy = Q.academies().comment("Create a subtype without manually materializing its parent")
                    .purpose("Compare Java and Rust inherited audited persistence").newEntity(context);
            academy.updatePlatform(platform); academy.updateSchoolTypeToPrimary();
            academy.updateName("Natural inheritance academy"); academy.updateStudentCapacity(20);
            academy.updateCampusCode("NORTH");
            School genericView = academy;
            var saved = save(genericView, context, "Create one inherited object through a generic save");
            assertSame(academy, saved); assertEquals(Academy.class, saved.getClass());
            long id = saved.getId(); assertEquals(Long.valueOf(1), saved.getVersion());
            assertNotNull(parent(context, id));
            assertEquals("Natural inheritance academy", E.academy(child(context,id)).getName().eval());
            assertEquals(Long.valueOf(1), save(academy,context,"Save unchanged inherited state").getVersion());
            var partial = Q.academiesWithMinimalFields().withIdIs(id).selectName().limit(1)
                    .comment("Load only the name for a negative mutation case")
                    .purpose("Reject changed incomplete mutation targets").executeForOne(context);
            partial.updateName("Incomplete write must be rejected");
            assertThrows(RuntimeException.class, () -> save(partial,context,"Reject a partial inherited mutation"));
            var current = child(context,id); var stale = child(context,id);
            current.updateName("Updated inherited academy"); current.updateCampusCode("SOUTH");
            assertEquals(Long.valueOf(2),save(current,context,"Update ancestor and local fields together").getVersion());
            stale.updateCampusCode("STALE");
            assertThrows(RuntimeException.class, () -> save(stale,context,"Reject an independent stale view"));
            var root = parent(context,id); assertEquals(Long.valueOf(2),root.getVersion());
            assertEquals("Updated inherited academy",E.school(root).getName().eval());
            assertEquals(School.class,root.getClass());
            assertEquals("Academy",root.runtimeType());
            assertSame(root,save(root,context,"Keep an unchanged parent view without losing its subtype"));
            assertEquals(Long.valueOf(2),root.getVersion());
            assertEquals("Academy",root.runtimeType());
            root.updateName("Parent edited academy");
            assertEquals(Long.valueOf(3),save(root,context,"Edit inherited state through its parent").getVersion());
            assertEquals("Academy", root.runtimeType());
            var reloaded = child(context,id); assertEquals(Long.valueOf(3),reloaded.getVersion());
            assertEquals("Parent edited academy",E.academy(reloaded).getName().eval());
            assertEquals("SOUTH",reloaded.getCampusCode());
            reloaded.markForDeletion();
            assertEquals(Long.valueOf(-4),save(reloaded,context,"Mark the subtype for deletion and save").getVersion());
            assertNull(child(context,id)); assertNull(parent(context,id));
            var deleted = Q.academies().deletedRowsOnly().withIdIs(id).limit(1)
                    .comment("Read the retained inherited tombstone")
                    .purpose("Verify deletion is root-owned and not physical row removal").executeForOne(context);
            assertNotNull(deleted); assertEquals(Long.valueOf(-4),deleted.getVersion());
            assertEquals("Parent edited academy",E.academy(deleted).getName().eval());
            assertEquals("Academy", deleted.runtimeType());
            var staleDeleted = Q.academies().deletedRowsOnly().withIdIs(id).limit(1)
                    .comment("Read an independent tombstone before recovery")
                    .purpose("Reject obsolete negative versions without shared ledgers").executeForOne(context);
            var partialRecovery=Q.academiesWithMinimalFields().deletedRowsOnly().withIdIs(id).selectName().limit(1)
                    .comment("Load an intentionally incomplete recovery target")
                    .purpose("Reject recovery without loading the complete object").executeForOne(context);
            partialRecovery.markToRecover();
            int beforeRejected=auditEvents.size();
            assertThrows(RuntimeException.class,()->save(partialRecovery,context,"Reject incomplete recovery"));
            assertEquals(beforeRejected,auditEvents.size());
            deleted.markToRecover();
            var recovered = save(deleted,context,"Recover without inventing a scalar edit");
            assertEquals(Long.valueOf(5),recovered.getVersion());
            assertEquals("Academy",recovered.runtimeType());
            assertEquals(Long.valueOf(5),child(context,id).getVersion());
            assertEquals(Long.valueOf(5),parent(context,id).getVersion());
            staleDeleted.markToRecover();
            assertThrows(RuntimeException.class,()->save(staleDeleted,context,"Reject obsolete recovery version"));
            recovered.markForDeletion();
            assertEquals(Long.valueOf(-6),save(recovered,context,"Delete before recovery with edits").getVersion());
            var editedRecovery = Q.academies().deletedRowsOnly().withIdIs(id).limit(1)
                    .comment("Load a complete tombstone for correction and recovery")
                    .purpose("Commit ancestor and child edits with one recovery version").executeForOne(context);
            editedRecovery.markToRecover();
            editedRecovery.updateName("Corrected recovered academy");
            editedRecovery.updateCampusCode("WEST");
            assertTrue(editedRecovery.getUpdatedProperties().containsAll(List.of("name","campus_code")));
            var corrected = save(editedRecovery,context,"Recover and correct both declaring tables atomically");
            assertEquals(Long.valueOf(7),corrected.getVersion());
            assertEquals("Corrected recovered academy",E.academy(child(context,id)).getName().eval());
            assertEquals("WEST",child(context,id).getCampusCode());
            assertEquals("Corrected recovered academy",E.school(parent(context,id)).getName().eval());
            assertEquals(Long.valueOf(7),parent(context,id).getVersion());
            var live=child(context,id);
            int beforeLiveRejection=auditEvents.size();
            assertThrows(RuntimeException.class,()->{live.markToRecover();save(live,context,"Reject recovery of a live object");});
            assertEquals(beforeLiveRejection,auditEvents.size());
            var recoveryAudits=auditEvents.stream().filter(event->event.kind()==MutationAuditKind.RECOVERED && event.entityType().equals("Academy") && Objects.equals(event.entityId(),id)).toList();
            assertEquals(2,recoveryAudits.size());
            for(var event:recoveryAudits)assertTrue(event.traceChain().stream().anyMatch(node->node.getKind()==TraceKind.AUDIT_REASON && !node.getComment().isBlank()));
            assertTrue(recoveryAudits.get(1).fields().stream().anyMatch(field->field.name().equals("campus_code")));
            assertTrue(recoveryAudits.get(1).fields().stream().anyMatch(field->field.name().equals("name")));
            var rollback=GraphRollbackProbe.verify(context,id,auditEvents,source,provider,round);
            var constant = Q.schoolTypes().withIdIs(id).limit(1).comment("Read an unrelated type at the same numeric ID")
                    .purpose("Reject optimistic-version reuse across distinct entity families").executeForOne(context);
            assertNotNull(constant); assertEquals(Long.valueOf(1),constant.getVersion());
            var subtypeViews = new ArrayList<List<String>>();
            subtypeViews.add(List.of(root.getClass().getSimpleName(), "Academy", root.runtimeType()));
            ids.next = 2000 + round;
            var training = Q.trainingAcademies().comment("Create a grandchild for typed-view identity checks")
                    .purpose("Compare the same three-level inheritance semantics").newEntity(context);
            training.updatePlatform(platform); training.updateSchoolTypeToPrimary(); training.updateName("Training Robotics");
            training.updateStudentCapacity(80); training.updateCampusCode("NORTH"); training.updateSpecialty("robotics");
            save(training,context,"Create one most-derived training academy");
            var trainingRoot = parent(context,training.getId()); var trainingMiddle = child(context,training.getId());
            assertEquals(School.class,trainingRoot.getClass()); assertEquals("Academy",trainingRoot.runtimeType());
            assertEquals(Academy.class,trainingMiddle.getClass()); assertEquals("TrainingAcademy",trainingMiddle.runtimeType());
            subtypeViews.add(List.of(trainingRoot.getClass().getSimpleName(), "TrainingAcademy", trainingRoot.runtimeType()));
            subtypeViews.add(List.of(trainingMiddle.getClass().getSimpleName(), "TrainingAcademy", trainingMiddle.runtimeType()));
            var leaf = Q.trainingAcademiesWithMinimalFields().withIdIs(training.getId()).selectName().limit(1)
                    .comment("Read a minimal leaf view").purpose("Keep the leaf type without an implicit cast").executeForOne(context);
            assertEquals("TrainingAcademy",leaf.runtimeType());
            subtypeViews.add(List.of(leaf.getClass().getSimpleName(), "TrainingAcademy", leaf.runtimeType()));
            var minimal = Q.schoolsWithMinimalFields().withIdIs(training.getId()).selectName().limit(1)
                    .comment("Read a minimal parent view").purpose("Retain subtype information without loading child fields").executeForOne(context);
            assertEquals("Academy",minimal.runtimeType()); assertEquals(School.class,minimal.getClass());
            try (var stream = Q.schoolsWithMinimalFields().withIdIs(training.getId()).selectName().limit(1)
                    .comment("Stream one typed parent view").purpose("Match list and stream subtype metadata").executeForStream(context)) {
                var streamed = stream.toList(); assertEquals(1,streamed.size());
                assertEquals(School.class,streamed.get(0).getClass()); assertEquals("Academy",streamed.get(0).runtimeType());
            }
            ids.next = 3000 + round;
            var college = Q.colleges().comment("Create a sibling subtype").purpose("Keep sibling branch identities distinct").newEntity(context);
            college.updatePlatform(platform); college.updateSchoolTypeToPrimary(); college.updateName("College Arts");
            college.updateStudentCapacity(60); college.updateDegreeLevel("arts");save(college,context,"Create the sibling branch");
            var sibling = parent(context,college.getId()); assertEquals("College",sibling.runtimeType());
            subtypeViews.add(List.of(sibling.getClass().getSimpleName(), "College", sibling.runtimeType()));
            ids.next = 4000 + round;
            var base = Q.schools().comment("Create a base object without a child branch").purpose("Keep base identity when no subtype exists").newEntity(context);
            base.updatePlatform(platform); base.updateSchoolTypeToPrimary(); base.updateName("Base School");base.updateStudentCapacity(100);
            save(base,context,"Create one plain parent object");var baseView = parent(context,base.getId());
            assertEquals("School",baseView.runtimeType());subtypeViews.add(List.of(baseView.getClass().getSimpleName(),"School",baseView.runtimeType()));
            var proof = new LinkedHashMap<String,Object>();
            proof.put("passed",true); proof.put("provider",provider); proof.put("round",round); proof.put("id",id);
            proof.put("caseCount",17); proof.put("versions",List.of(1,2,3,-4,5,-6,7,8)); proof.put("manualParentSeed",false);
            proof.put("genericSavePreservesType",true); proof.put("sameIdOtherTypeUnchanged",true);
            proof.put("typedViewSubtypeMetadata",true); proof.put("subtypeViews",subtypeViews);
            proof.put("parentNoopSubtypeRetained",true);
            proof.put("recoveryWithoutScalarEdit",true);proof.put("recoveryWithEdits",true);proof.put("staleRecoveryRejected",true);
            proof.put("partialRecoveryRejected",true);proof.put("liveRecoveryRejected",true);proof.put("recoveryAuditLineage",true);
            proof.put("mixedGraphRollback",true);proof.put("retrySameGraph",true);proof.put("rollback",rollback);
            System.out.println("INHERITED_SAVE_PARITY="+new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(proof));
        } finally { if(sqlite!=null) sqlite.close(); }
    }
}
