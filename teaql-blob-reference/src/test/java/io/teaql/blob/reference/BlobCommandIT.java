package io.teaql.blob.reference;

import io.teaql.core.*;
import io.teaql.core.blob.*;
import io.teaql.core.blob.BlobCommandService.*;
import io.teaql.dataservice.sql.SqlDataServiceExecutor;
import io.teaql.runtime.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static io.teaql.blob.reference.PostgresBlobCatalogIT.*;
import static io.teaql.core.blob.BlobException.Code.*;
import static io.teaql.core.TransactionCompletion.Outcome.*;
import static org.junit.Assert.*;

/** Durable command tests use native graph saves and a separate connection for committed-state oracles. */
public class BlobCommandIT {
    static BlobCommandAuthorization authorization() {
        return new BlobCommandAuthorization(context -> context.getAttribute("scope",BlobScope.class),
                context -> context.getAttribute("principal",String.class),
                (context,request) -> !Boolean.FALSE.equals(context.getAttribute("execute")),
                (context,receipt) -> !Boolean.FALSE.equals(context.getAttribute("replay")));
    }
    static DefaultUserContext actor(Fixture f) { var actor=f.context(); actor.putAttribute("principal","alice"); return actor; }
    static Request request(String key) { return request("Asset.bind/v1",key,"bind Asset 42"); }
    static Request request(String operation,String key,String input) { return new Request(operation,key,input.getBytes(StandardCharsets.UTF_8)); }
    static List<EntityRevision> revisions(Asset asset) { return List.of(new EntityRevision(asset.typeName(),asset.getId(),asset.getVersion())); }
    static PostgresBlobCommandExecutor commands(Fixture f) {
        var journal=new PostgresBlobCommandJournal(f.sql); journal.migrate(); return new PostgresBlobCommandExecutor(journal,authorization(),f.clock);
    }
    static long count(Fixture f,String table) { return ((Number)f.otherSql.queryForList("SELECT count(*) FROM "+table,new Object[0]).get(0).values().iterator().next()).longValue(); }
    static Action never() { return () -> { throw new AssertionError("Committed replay must not run the action"); }; }

    @Test public void migrationIsExplicitIdempotentAndChecksMarkerColumnsAndConstraints() throws Exception {
        try (Fixture f=new Fixture()) {
            var journal=new PostgresBlobCommandJournal(f.sql);
            var unsupported=new PostgresBlobCommandJournal(new io.teaql.provider.jdbc.JdbcSqlExecutor(f.dataSource) {
                @Override public boolean supportsTransactionCompletion() { return false; }
            });
            var service=new PostgresBlobCommandExecutor(unsupported,authorization(),f.clock);
            assertEquals(BLOB_TRANSACTION_REQUIRED,assertThrows(BlobException.class,unsupported::validateReadiness).code());
            assertEquals(BLOB_TRANSACTION_REQUIRED,assertThrows(BlobException.class,() -> service.resolve(actor(f),request("unsupported"))).code());
            assertEquals(BLOB_TRANSACTION_REQUIRED,assertThrows(BlobException.class,() -> service.execute(actor(f),request("unsupported"),never())).code());
            assertEquals(0,count(f,"pg_tables WHERE schemaname=current_schema() AND tablename LIKE 'teaql_blob_command%'"));
            assertEquals(BLOB_SCHEMA_MISSING,assertThrows(BlobException.class,journal::validateReadiness).code());
            assertEquals(0,count(f,"pg_tables WHERE schemaname=current_schema() AND tablename LIKE 'teaql_blob_command%'"));
            journal.migrate(); journal.migrate(); journal.validateReadiness();
            f.sql.update("UPDATE teaql_blob_command_schema SET schema_digest=?",new Object[]{"0".repeat(64)});
            assertEquals(BLOB_SCHEMA_MISSING,assertThrows(BlobException.class,journal::validateSchema).code());
        }
        for (String alteration:List.of("ALTER TABLE teaql_blob_command DROP CONSTRAINT teaql_blob_command_completion_ck",
                "ALTER TABLE teaql_blob_command ALTER COLUMN request_digest DROP NOT NULL",
                "ALTER TABLE teaql_blob_command ADD COLUMN unexpected TEXT")) try (Fixture f=new Fixture()) {
            var journal=new PostgresBlobCommandJournal(f.sql); journal.migrate(); f.sql.execute(alteration);
            assertEquals(BLOB_SCHEMA_MISSING,assertThrows(BlobException.class,journal::validateReadiness).code());
        }
    }

    @Test public void nativeBusinessBindingAndReceiptCommitOnceAndReplayNeverResurrectsHistoricalBinding() throws Exception {
        try (Fixture f=new Fixture()) {
            var service=commands(f); var actor=actor(f); BlobRef key=f.upload(); var asset=f.newAsset(42,key); AtomicInteger calls=new AtomicInteger();
            var result=service.execute(actor,request("once"),() -> {
                calls.incrementAndGet(); f.runtime.saveGraph(actor,asset);
                assertEquals(0,count(f,"asset_data")); assertEquals(0,count(f,"teaql_blob_binding")); assertEquals(0,count(f,"teaql_blob_command"));
                assertTrue(f.audits.isEmpty()); return revisions(asset);
            });
            assertFalse(result.replayed()); assertEquals(1,calls.get()); assertEquals(NOW,result.receipt().completedAt());
            assertEquals(List.of(new EntityRevision("Asset",42,1)),result.receipt().entities()); f.assertOracle(42,key);
            assertEquals(1,count(f,"teaql_blob_command")); assertEquals(1,f.audits.size());
            assertEquals(result.receipt(),service.resolve(actor,request("once")).orElseThrow());
            asset.updateProperty("document",null); f.runtime.saveGraph(actor,asset); f.assertOracle(42,null);
            var replay=service.execute(actor,request("once"),never()); assertTrue(replay.replayed()); assertEquals(result.receipt(),replay.receipt());
            f.assertOracle(42,null); assertEquals(2L,((Number)f.row(42).get("version")).longValue()); assertEquals(2,f.audits.size());
        }
    }

    @Test public void changedPayloadConflictsAndOperationAndScopeHaveIndependentIdentity() throws Exception {
        try (Fixture f=new Fixture()) {
            var service=commands(f); var actor=actor(f); var initial=service.execute(actor,request("key"),List::of);
            assertEquals(BLOB_IDEMPOTENCY_CONFLICT,assertThrows(BlobException.class,() -> service.execute(actor,request("Asset.bind/v1","key","different"),never())).code());
            assertEquals(BLOB_IDEMPOTENCY_CONFLICT,assertThrows(BlobException.class,() -> service.resolve(actor,request("Asset.bind/v1","key","different"))).code());
            assertFalse(service.execute(actor,request("Asset.clear/v1","key","bind Asset 42"),List::of).replayed());
            actor.putAttribute("scope",new BlobScope("test","tenant-b")); assertTrue(service.resolve(actor,request("key")).isEmpty());
            assertFalse(service.execute(actor,request("key"),List::of).replayed()); assertEquals(3,count(f,"teaql_blob_command"));
            assertEquals(SCOPE,initial.receipt().scope());
        }
    }

    @Test public void principalExecuteAndReplayPoliciesAreCurrentAndFailClosed() throws Exception {
        try (Fixture f=new Fixture()) {
            var service=commands(f); var actor=actor(f); service.execute(actor,request("private"),List::of);
            actor.putAttribute("principal","bob");
            assertEquals(BLOB_COMMAND_DENIED,assertThrows(BlobException.class,() -> service.execute(actor,request("private"),never())).code());
            assertEquals(BLOB_COMMAND_DENIED,assertThrows(BlobException.class,() -> service.resolve(actor,request("private"))).code());
            actor.putAttribute("principal","alice"); actor.putAttribute("execute",Boolean.FALSE);
            assertEquals(BLOB_COMMAND_DENIED,assertThrows(BlobException.class,() -> service.execute(actor,request("private"),never())).code());
            actor.putAttribute("execute",Boolean.TRUE); actor.putAttribute("replay",Boolean.FALSE);
            assertEquals(BLOB_COMMAND_DENIED,assertThrows(BlobException.class,() -> service.resolve(actor,request("private"))).code());
            assertEquals(BLOB_COMMAND_DENIED,assertThrows(BlobException.class,() -> service.execute(actor,request("private"),never())).code());
            assertEquals(1,count(f,"teaql_blob_command"));
            for (String invalid:List.of("","\n","a".repeat(257),"\ud800","\udfff")) {
                actor.putAttribute("principal",invalid);
                assertEquals(BLOB_COMMAND_DENIED,assertThrows(BlobException.class,() -> service.execute(actor,request("new"),never())).code());
            }
        }
    }

    @Test public void actionRollbackLeavesNoMarkerAndRetainedGraphIntentCanRetryOnce() throws Exception {
        try (Fixture f=new Fixture()) {
            var service=commands(f); var actor=actor(f); BlobRef key=f.upload(); var asset=f.newAsset(42,key);
            assertThrows(IllegalArgumentException.class,() -> service.execute(actor,request("retry"),() -> {
                f.runtime.saveGraph(actor,asset); throw new IllegalArgumentException("owned rollback");
            }));
            assertEquals(0,count(f,"asset_data")); assertEquals(0,count(f,"teaql_blob_binding")); assertEquals(0,count(f,"teaql_blob_command"));
            assertNull(asset.getVersion()); assertTrue(service.resolve(actor,request("retry")).isEmpty());
            assertFalse(service.execute(actor,request("retry"),() -> { f.runtime.saveGraph(actor,asset); return revisions(asset); }).replayed());
            f.assertOracle(42,key); assertEquals(1,f.audits.size());
        }
    }

    @Test public void concurrentIdenticalRequestsWaitForPhysicalOwnerAndExecuteOneAction() throws Exception {
        try (Fixture f=new Fixture()) {
            var service=commands(f); BlobRef key=f.upload(); var pool=Executors.newFixedThreadPool(2);
            CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),secondStarted=new CountDownLatch(1); AtomicInteger calls=new AtomicInteger();
            try {
                Future<Execution> first=pool.submit(() -> service.execute(actor(f),request("concurrent"),() -> {
                    calls.incrementAndGet(); entered.countDown(); await(release); var asset=f.newAsset(42,key); f.save(asset); return revisions(asset);
                }));
                assertTrue(entered.await(5,TimeUnit.SECONDS));
                Future<Execution> second=pool.submit(() -> { secondStarted.countDown(); return service.execute(actor(f),request("concurrent"),never()); });
                assertTrue(secondStarted.await(5,TimeUnit.SECONDS)); assertEquals(0,count(f,"teaql_blob_command"));
                assertTrue(service.resolve(actor(f),request("concurrent")).isEmpty()); release.countDown();
                assertFalse(first.get(10,TimeUnit.SECONDS).replayed()); assertTrue(second.get(10,TimeUnit.SECONDS).replayed());
                assertEquals(1,calls.get()); assertEquals(1,count(f,"teaql_blob_command")); f.assertOracle(42,key);
            } finally { release.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS)); }
        }
    }

    @Test public void scopePrincipalAndPermissionChangesInsideActionRollBackEveryWrite() throws Exception {
        for (String change:List.of("scope","principal","execute","replay")) try (Fixture f=new Fixture()) {
            var service=commands(f); var actor=actor(f); BlobRef key=f.upload(); var asset=f.newAsset(42,key);
            assertEquals(BLOB_COMMAND_DENIED,assertThrows(BlobException.class,() -> service.execute(actor,request("changed"),() -> {
                f.runtime.saveGraph(actor,asset);
                actor.putAttribute(change,change.equals("scope")?new BlobScope("test","tenant-b"):change.equals("principal")?"bob":Boolean.FALSE);
                return revisions(asset);
            })).code());
            assertEquals(0,count(f,"asset_data")); assertEquals(0,count(f,"teaql_blob_binding")); assertEquals(0,count(f,"teaql_blob_command")); assertTrue(f.audits.isEmpty());
        }
    }

    @Test public void outerOwnerNestedCommandAndDifferentResourceRejectBeforeBusinessWrites() throws Exception {
        try (Fixture f=new Fixture()) {
            var service=commands(f); var actor=actor(f);
            f.sql.executeInTransaction(() -> {
                assertEquals(BLOB_TRANSACTION_REQUIRED,assertThrows(BlobException.class,() -> service.execute(actor,request("outer"),never())).code());
                assertEquals(BLOB_TRANSACTION_REQUIRED,assertThrows(BlobException.class,() -> service.resolve(actor,request("outer"))).code());
            });
            assertEquals(BLOB_TRANSACTION_REQUIRED,assertThrows(BlobException.class,() -> service.execute(actor,request("nested"),() -> {
                service.execute(actor,request("inner"),never()); return List.of();
            })).code());
            var otherParticipant=new PostgresBlobMutationParticipant(new PostgresBlobCatalog(f.otherSql),f.policies,
                    new BlobBindingAuthorization(context -> SCOPE,(context,target,kind) -> true,(context,target,blob) -> true),f.clock);
            try (var other=TeaQLRuntime.builder().metadata(f.runtime.getMetadata()).dataService("default",new SqlDataServiceExecutor("default",f.otherSql))
                    .blobMutationParticipant(otherParticipant).telemetry(RuntimeTelemetry.NOOP).executionLogging(false).build()) {
                var asset=f.newAsset(42,f.upload());
                assertEquals(BLOB_TRANSACTION_REQUIRED,assertThrows(BlobException.class,() -> service.execute(actor,request("other"),() -> {
                    other.saveGraph(new DefaultUserContext(other),asset); return revisions(asset);
                })).code());
            }
            assertEquals(0,count(f,"asset_data")); assertEquals(0,count(f,"teaql_blob_command"));
        }
    }

    @Test public void incompleteOrCorruptCommittedMarkersNeverRepeatBusinessAction() throws Exception {
        for (String corruption:List.of("incomplete","json","duplicate-entity")) try (Fixture f=new Fixture()) {
            var service=commands(f); var actor=actor(f); service.execute(actor,request("corrupt"),List::of);
            if (corruption.equals("incomplete")) f.sql.execute("UPDATE teaql_blob_command SET receipt_json=NULL,completed_at=NULL");
            else f.sql.update("UPDATE teaql_blob_command SET receipt_json=?",new Object[]{corruption.equals("json")?"{}":"{\"protocol\":\"blob.command.receipt.v1\",\"entities\":[{\"entity\":\"Asset\",\"id\":42,\"revision\":1},{\"entity\":\"Asset\",\"id\":42,\"revision\":2}]}"});
            assertEquals(BLOB_CATALOG_FAILED,assertThrows(BlobException.class,() -> service.execute(actor,request("corrupt"),never())).code());
            assertEquals(BLOB_CATALOG_FAILED,assertThrows(BlobException.class,() -> service.resolve(actor,request("corrupt"))).code());
            assertEquals(1,count(f,"teaql_blob_command"));
        }
    }

    @Test public void postCommitAuditFailurePreservesCommittedReceiptAndReplaySkipsAction() throws Exception {
        try (Fixture f=new Fixture()) {
            var service=commands(f); var actor=actor(f); BlobRef key=f.upload(); var asset=f.newAsset(42,key); f.failure="audit";
            var failure=assertThrows(TransactionCompletionException.class,() -> service.execute(actor,request("audit"),() -> { f.runtime.saveGraph(actor,asset); return revisions(asset); }));
            assertEquals(COMMITTED,failure.outcome()); f.assertOracle(42,key); assertEquals(1,count(f,"teaql_blob_command"));
            assertFalse(asset.getEntityMutationLedger().hasUncommittedChanges()); f.failure="";
            assertTrue(service.execute(actor,request("audit"),never()).replayed()); assertTrue(service.resolve(actor,request("audit")).isPresent());
        }
    }

    @Test public void postCommitRevocationDeniesReceiptDeliveryWithoutHidingCommittedOutcome() throws Exception {
        try (Fixture f=new Fixture()) {
            var service=commands(f); var actor=actor(f); var asset=f.newAsset(42,f.upload()); f.auditHook=event -> actor.putAttribute("replay",Boolean.FALSE);
            var failure=assertThrows(TransactionCompletionException.class,() -> service.execute(actor,request("revoke"),() -> { f.runtime.saveGraph(actor,asset); return revisions(asset); }));
            assertEquals(COMMITTED,failure.outcome()); assertTrue(failure.getCause() instanceof BlobException);
            assertEquals(BLOB_COMMAND_DENIED,((BlobException)failure.getCause()).code()); assertEquals(1,count(f,"teaql_blob_command"));
            assertEquals(BLOB_COMMAND_DENIED,assertThrows(BlobException.class,() -> service.resolve(actor,request("revoke"))).code());
            actor.putAttribute("replay",Boolean.TRUE); assertTrue(service.execute(actor,request("revoke"),never()).replayed());
        }
    }

    @Test public void commitDeliveryCanStartAnIndependentCommandAfterScopeAndOwnerAreReleased() throws Exception {
        try (Fixture f=new Fixture()) {
            var service=commands(f); var actor=actor(f); var asset=f.newAsset(42,f.upload()); AtomicInteger notifications=new AtomicInteger();
            f.auditHook=event -> {
                assertFalse(f.sql.hasActiveTransaction()); notifications.incrementAndGet();
                assertFalse(service.execute(actor,request("followup"),List::of).replayed());
            };
            assertFalse(service.execute(actor,request("outer-audit"),() -> { f.runtime.saveGraph(actor,asset); return revisions(asset); }).replayed());
            assertEquals(1,notifications.get()); assertEquals(2,count(f,"teaql_blob_command"));
            assertTrue(service.execute(actor,request("outer-audit"),never()).replayed()); assertTrue(service.execute(actor,request("followup"),never()).replayed());
        }
    }

    @Test public void invalidReceiptMetadataRollsBackProvisionalBusinessAndBindingWrites() throws Exception {
        try (Fixture f=new Fixture()) {
            var service=commands(f); var actor=actor(f); var asset=f.newAsset(42,f.upload());
            assertEquals(BLOB_COMMAND_INVALID,assertThrows(BlobException.class,() -> service.execute(actor,request("invalid-receipt"),() -> {
                f.runtime.saveGraph(actor,asset); return List.of(new EntityRevision("Asset",42,1),new EntityRevision("Asset",42,2));
            })).code());
            assertEquals(0,count(f,"asset_data")); assertEquals(0,count(f,"teaql_blob_binding")); assertEquals(0,count(f,"teaql_blob_command"));
            assertNull(asset.getVersion()); assertTrue(f.audits.isEmpty());
        }
    }

    @Test public void terminatedOwnerLeavesUnknownGraphPoisonedAndFreshCommandCanRetry() throws Exception {
        try (Fixture f=new Fixture()) {
            var service=commands(f); var actor=actor(f); BlobRef key=f.upload(); var asset=f.newAsset(42,key); f.failure="terminate-before-apply";
            var failure=assertThrows(TransactionCompletionException.class,() -> service.execute(actor,request("terminated"),() -> { f.runtime.saveGraph(actor,asset); return revisions(asset); }));
            assertEquals(UNKNOWN,failure.outcome()); assertEquals(0,count(f,"asset_data")); assertEquals(0,count(f,"teaql_blob_command"));
            assertTrue(service.resolve(actor,request("terminated")).isEmpty()); f.failure="";
            assertThrows(RuntimeException.class,() -> f.save(asset));
            var fresh=f.newAsset(42,key); assertFalse(service.execute(actor,request("terminated"),() -> { f.runtime.saveGraph(actor,fresh); return revisions(fresh); }).replayed()); f.assertOracle(42,key);
        }
    }

    @Test public void lostAcknowledgementResolvesDurableCommitWithoutClearingUnknownGraphProtection() throws Exception {
        try (Fixture f=new Fixture()) {
            commands(f); BlobRef key=f.upload(); var asset=f.newAsset(42,key);
            try (var harness=new BlobCommandFaultHarness(f.schema)) {
                harness.fault.set(BlobCommandFaultHarness.Fault.ACK_LOST);
                var failure=assertThrows(TransactionCompletionException.class,() -> harness.commands.execute(harness.actor,request("ack-lost"),() -> {
                    harness.runtime.saveGraph(harness.actor,asset); return revisions(asset);
                }));
                assertEquals(UNKNOWN,failure.outcome()); f.assertOracle(42,key); assertEquals(1,count(f,"teaql_blob_command"));
                assertTrue(asset.getEntityMutationLedger().hasUncommittedChanges());
                var receipt=harness.commands.resolve(harness.actor,request("ack-lost")).orElseThrow();
                assertEquals(List.of(new EntityRevision("Asset",42,1)),receipt.entities());
                assertThrows(RuntimeException.class,() -> harness.runtime.saveGraph(harness.actor,asset));
                var replay=harness.commands.execute(harness.actor,request("ack-lost"),never()); assertTrue(replay.replayed()); assertEquals(receipt,replay.receipt());
                assertThrows(RuntimeException.class,() -> harness.runtime.saveGraph(harness.actor,asset));
                var fresh=f.load(42); fresh.updateProperty("title","authorized reload"); harness.runtime.saveGraph(harness.actor,fresh);
                assertEquals(2L,((Number)f.row(42).get("version")).longValue()); assertEquals(1,count(f,"teaql_blob_command"));
            }
        }
    }

    @Test public void independentJvmDeathBeforeAndAfterCommitHasOneDurableOutcome() throws Exception {
        for (var phase:List.of(BlobCommandFaultHarness.Fault.CRASH_BEFORE_COMMIT,BlobCommandFaultHarness.Fault.CRASH_AFTER_COMMIT)) try (Fixture f=new Fixture()) {
            var service=commands(f); BlobRef key=f.upload(); Path log=Files.createTempFile("r3-owned-command-crash-",".log");
            String classpath=String.join(File.pathSeparator,System.getProperty("java.class.path",""),System.getProperty("jdk.module.path",""),Path.of("target/test-classes").toAbsolutePath().toString());
            var builder=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin/java").toString(),"-cp",classpath,
                    BlobCommandCrashProbe.class.getName(),key.key(),phase.name());
            builder.environment().put("TEAQL_BLOB_COMMAND_CRASH_SCHEMA",f.schema); builder.redirectErrorStream(true).redirectOutput(log.toFile());
            Process child=builder.start();
            try {
                assertTrue("Owned command crash probe did not terminate",child.waitFor(20,TimeUnit.SECONDS));
                boolean committed=phase==BlobCommandFaultHarness.Fault.CRASH_AFTER_COMMIT;
                if (child.exitValue()!=(committed?76:75)) {
                    String diagnostic=Files.readString(log); String password=System.getenv("TEAQL_BLOB_TEST_PG_PASSWORD");
                    if (password!=null && !password.isEmpty()) diagnostic=diagnostic.replace(password,"[redacted]");
                    fail("Owned command probe failed before injection: "+diagnostic.substring(0,Math.min(2048,diagnostic.length())));
                }
                var actor=actor(f);
                assertEquals(committed?1:0,count(f,"asset_data")); assertEquals(committed?1:0,count(f,"teaql_blob_binding")); assertEquals(committed?1:0,count(f,"teaql_blob_command"));
                assertEquals(committed,service.resolve(actor,request("crash")).isPresent());
                if (committed) { assertTrue(service.execute(actor,request("crash"),never()).replayed()); f.assertOracle(42,key); }
                else {
                    var fresh=f.newAsset(42,key); assertFalse(service.execute(actor,request("crash"),() -> { f.runtime.saveGraph(actor,fresh); return revisions(fresh); }).replayed()); f.assertOracle(42,key);
                }
                assertEquals(1,count(f,"asset_data")); assertEquals(1,count(f,"teaql_blob_command"));
            } finally { if (child.isAlive()) child.destroyForcibly(); assertTrue(child.waitFor(5,TimeUnit.SECONDS)); Files.deleteIfExists(log); }
        }
    }
    private static void await(CountDownLatch latch) {
        try { if (!latch.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("Owned command barrier timed out"); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
    }
}
