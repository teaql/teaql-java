package io.teaql.blob.reference;

import io.teaql.core.*;
import io.teaql.core.blob.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.*;
import io.teaql.dataservice.sql.SqlDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.io.PrintWriter;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.Test;
import static org.junit.Assert.*;

/** Explicit opt-in integration suite. Requires the task-owned PostgreSQL and real fixture paths. */
public class PostgresBlobCatalogIT {
    static final BlobScope SCOPE=new BlobScope("test","tenant-a");
    static final Instant NOW=Instant.parse("2026-10-25T00:00:00Z");

    @Test public void explicitMigrationAndUploadCatalogIdempotency() throws Exception {
        try (Fixture f=new Fixture(false,BlobPolicies.defaults())) {
            assertEquals(BlobException.Code.BLOB_SCHEMA_MISSING,assertThrows(BlobException.class,f.catalog::validateSchema).code());
            assertEquals(0L,f.scalar("SELECT count(*) FROM pg_tables WHERE schemaname=current_schema() AND tablename LIKE 'teaql_blob_%'"));
            f.sql.update("INSERT INTO asset_data(id,version,document) VALUES (1,1,?),(2,1,?)",
                    new Object[]{"legacy-invoice.pdf","https://legacy.example.test/files/2.pdf"});
            f.catalog.migrate(); f.catalog.migrate(); f.catalog.validateSchema();
            assertEquals("legacy-invoice.pdf",f.row(1).get("document"));
            assertEquals("https://legacy.example.test/files/2.pdf",f.row(2).get("document"));
            assertEquals(0L,f.scalar("SELECT count(*) FROM teaql_blob_binding"));
            var draft=f.draft("same-request","a".repeat(64));
            var first=f.catalog.begin(draft,NOW);
            var retry=f.draft("same-request","a".repeat(64));
            assertEquals(first.reference(),f.catalog.begin(retry,NOW.plusSeconds(50)).reference());
            assertEquals(1L,f.scalar("SELECT count(*) FROM teaql_blob_object"));
            assertEquals(BlobException.Code.BLOB_IDEMPOTENCY_CONFLICT,assertThrows(BlobException.class,
                    () -> f.catalog.begin(f.draft("same-request","b".repeat(64)),NOW)).code());
            var complete=f.catalog.complete(SCOPE,first.reference(),f.content(),NOW.plusSeconds(1));
            assertEquals(BlobState.UPLOADED_UNBOUND,complete.state());
            assertEquals(complete.completedAt(),f.catalog.complete(SCOPE,first.reference(),f.content(),NOW.plusSeconds(2)).completedAt());
            assertEquals(BlobException.Code.BLOB_IDEMPOTENCY_CONFLICT,assertThrows(BlobException.class,
                    () -> f.catalog.complete(SCOPE,first.reference(),new PostgresBlobCatalog.VerifiedContent("application/pdf",12,"c".repeat(64),"changed"),NOW)).code());
            assertEquals(f.policies.resolve(f.spec),complete.policy());
        }
    }

    @Test public void nativeBusinessSaveAndBindingUseOnePhysicalTransaction() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef first=f.upload(); Asset asset=f.create(42,first);
            assertEquals(first.key(),f.row(42).get("document"));
            assertEquals(first,f.binding(42).reference());
            assertEquals(BlobState.AVAILABLE,f.find(first).state());
            assertNotNull(f.prepareTransaction); assertEquals(f.prepareTransaction,f.businessTransaction); assertEquals(f.prepareTransaction,f.applyTransaction);
            assertEquals(f.find(first).firstBoundAt(),f.binding(42).boundAt());
            assertEquals(Long.valueOf(1),asset.getVersion());
            f.assertOracle(42,first);
            assertFalse(f.sql.hasActiveTransaction()); assertNull(f.sql.activeTransactionIdentity());
            BlobRef second=f.upload(); asset=f.load(42); asset.updateProperty("document",second); f.save(asset);
            f.assertOracle(42,second);
            assertNotNull(f.find(first).unreferencedAt()); assertNull(f.find(second).unreferencedAt());
            long version=f.binding(42).version();
            asset=f.load(42); asset.updateProperty("document",null); f.save(asset);
            f.assertOracle(42,null); assertTrue(f.binding(42).version()>version);
            assertNotNull(f.find(second).unreferencedAt());
            assertTrue(asset.getEntityMutationLedger().currentChangeSet().changes().isEmpty());
        }
    }

    @Test public void businessAndCatalogBothRollbackBeforeAndAfterApplyAndRetry() throws Exception {
        for (String stage:List.of("before-apply","after-apply","business")) {
            try (Fixture f=new Fixture()) {
                BlobRef old=f.upload(), next=f.upload(); f.create(42,old);
                long version=f.binding(42).version(); Asset asset=f.load(42); asset.updateProperty("document",next);
                if (stage.equals("business")) asset.updateProperty("title","fail"); else f.failure=stage;
                assertThrows(RuntimeException.class,() -> f.save(asset));
                f.assertOracle(42,old); assertEquals(version,f.binding(42).version());
                assertEquals(BlobState.UPLOADED_UNBOUND,f.find(next).state());
                assertNull(f.find(old).unreferencedAt()); assertEquals(Long.valueOf(1),asset.getVersion());
                assertTrue(asset.getEntityMutationLedger().changedFieldNames(new EntityKey("Asset",42L)).contains("document"));
                f.failure=""; if (stage.equals("business")) asset.updateProperty("title","retry");
                f.save(asset); f.assertOracle(42,next); assertEquals(Long.valueOf(2),asset.getVersion());
            }
        }
    }

    @Test public void authoritativeDeleteAndRecoveryPreserveMonotonicEmptyBindingVersion() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef key=f.upload(); f.create(42,key); Asset asset=f.load(42);
            asset.document=null; asset.__internalUseLoadState(asset.__internalLoadState().withLoaded("document",false));
            asset.markForDeletion(); f.save(asset);
            assertNull(f.binding(42).reference()); assertEquals(key,f.binding(42).retiredReference());
            long deleted=f.binding(42).version(); assertNotNull(f.find(key).unreferencedAt());
            assertEquals(key.key(),f.row(42).get("document")); assertTrue(((Number)f.row(42).get("version")).longValue()<0);
            asset=f.load(42); asset.document=null; asset.__internalUseLoadState(asset.__internalLoadState().withLoaded("document",false));
            asset.markAsRecover(); f.save(asset);
            f.assertOracle(42,key); assertTrue(f.binding(42).version()>deleted); assertNull(f.find(key).unreferencedAt());
            asset=f.load(42); asset.updateProperty("document",null); f.save(asset); long cleared=f.binding(42).version();
            asset=f.load(42); asset.updateProperty("document",key); f.save(asset);
            assertTrue(f.binding(42).version()>cleared); f.assertOracle(42,key);
        }
    }

    @Test public void partialUnrelatedUpdatesDoNotTouchBindings() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef key=f.upload(); f.create(42,key); long version=f.binding(42).version();
            Asset partial=f.load(42); partial.document=null;
            partial.__internalUseLoadState(partial.__internalLoadState().withLoaded("document",false));
            partial.updateProperty("title","partial"); f.save(partial);
            f.assertOracle(42,key); assertEquals(version,f.binding(42).version());
        }
    }

    @Test public void currentScopeHostPermissionAndAdditionalPolicyAllApply() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef key=f.upload(); f.create(42,key); BlobRef next=f.upload();
            for (String denial:List.of("scope","field","policy")) {
                DefaultUserContext context=f.context();
                if (denial.equals("scope")) context.putAttribute("scope",new BlobScope("test","tenant-b"));
                else context.putAttribute(denial,Boolean.FALSE);
                Asset asset=f.load(42); asset.updateProperty("document",next);
                BlobException failure=assertThrows(BlobException.class,() -> f.runtime.saveGraph(context,asset));
                assertEquals(BlobException.Code.BLOB_BIND_DENIED,failure.code()); f.assertOracle(42,key);
            }
            DefaultUserContext independent=f.context(); Asset allowed=f.load(42); allowed.updateProperty("document",next);
            f.runtime.saveGraph(independent,allowed); f.assertOracle(42,next);
        }
    }

    @Test public void liveReferencesRetentionFloorAndExpiryConstrainDeletionAndRebinding() throws Exception {
        BlobPolicies policies=new BlobPolicies(BlobPolicies.Limits.defaults(),BlobPolicyProfile.named("default")
                .unboundRetention(Duration.ofSeconds(10)).unreferencedRetention(Duration.ofSeconds(20)).minimumRetention(Duration.ofSeconds(50)).build(),Map.of(),Map.of());
        try (Fixture f=new Fixture(true,policies)) {
            BlobRef key=f.upload(); f.create(42,key);
            assertTrue(f.catalog.claimDeletion(SCOPE,key,NOW.plusSeconds(1000)).isEmpty());
            Asset asset=f.load(42); asset.updateProperty("document",null); f.save(asset);
            assertTrue(f.catalog.claimDeletion(SCOPE,key,NOW.plusSeconds(30)).isEmpty());
            assertEquals(NOW.plusSeconds(50),f.find(key).retentionFloorUntil());
            assertEquals(BlobState.DELETING,f.catalog.claimDeletion(SCOPE,key,NOW.plusSeconds(50)).orElseThrow().state());
            Asset rebind=f.load(42); rebind.updateProperty("document",key);
            assertEquals(BlobException.Code.BLOB_REFERENCE_EXPIRED,assertThrows(BlobException.class,() -> f.save(rebind)).code());
            f.assertOracle(42,null);
            BlobRef expired=f.upload(); f.clock.now=NOW.plusSeconds(10);
            Asset create=f.newAsset(43,expired);
            assertEquals(BlobException.Code.BLOB_REFERENCE_EXPIRED,assertThrows(BlobException.class,() -> f.save(create)).code());
            assertEquals(0L,f.scalar("SELECT count(*) FROM asset_data WHERE id=43"));
        }
    }

    @Test public void sharedObjectsStayProtectedUntilLastUnbindAndSingleBindingRejectsReuse() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef key=f.upload(); f.create(42,key); f.create(43,key);
            Asset first=f.load(42); first.updateProperty("document",null); f.save(first);
            assertNull(f.find(key).unreferencedAt()); assertTrue(f.catalog.claimDeletion(SCOPE,key,NOW.plusSeconds(1000000)).isEmpty());
            Asset second=f.load(43); second.updateProperty("document",null); f.save(second);
            assertNotNull(f.find(key).unreferencedAt());
        }
        BlobPolicies single=new BlobPolicies(BlobPolicies.Limits.defaults(),BlobPolicyProfile.named("default").sharing(BlobPolicyProfile.Sharing.SINGLE_BINDING).build(),Map.of(),Map.of());
        try (Fixture f=new Fixture(true,single)) {
            BlobRef key=f.upload(); f.create(42,key); Asset denied=f.newAsset(43,key);
            assertEquals(BlobException.Code.BLOB_BIND_DENIED,assertThrows(BlobException.class,() -> f.save(denied)).code());
            assertEquals(0L,f.scalar("SELECT count(*) FROM asset_data WHERE id=43")); f.assertOracle(42,key);
        }
    }

    @Test public void preparedBindingsCannotBeReusedInAnotherPhysicalTransaction() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef key=f.upload(); DefaultUserContext context=f.context();
            MutationPlan mutation=new MutationPlan("test","Asset.saveGraph","Asset","create asset",
                    List.of(new MutationOperation(MutationOperationKind.CREATE,new EntityKey("Asset",42L),null,Map.of("document",key))));
            BlobMutationParticipant.Plan plan=f.participant.plan(context,mutation);
            assertEquals(BlobException.Code.BLOB_TRANSACTION_REQUIRED,assertThrows(BlobException.class,
                    () -> plan.requireTransaction(f.otherSql)).code());
            assertEquals(BlobException.Code.BLOB_TRANSACTION_REQUIRED,assertThrows(BlobException.class,
                    () -> plan.prepare(context,f.sql)).code());
            BlobMutationParticipant.Prepared[] prepared=new BlobMutationParticipant.Prepared[1];
            Object[] token=new Object[1];
            f.sql.executeInTransaction(() -> { token[0]=f.sql.activeTransactionIdentity(); prepared[0]=plan.prepare(context,f.sql); });
            f.sql.executeInTransaction(() -> {
                assertNotSame(token[0],f.sql.activeTransactionIdentity());
                assertEquals(BlobException.Code.BLOB_TRANSACTION_REQUIRED,assertThrows(BlobException.class,
                        () -> prepared[0].apply(context,f.sql,List.of(new BlobMutationParticipant.PersistedEntity(new EntityKey("Asset",42L),new EntityKey("Asset",42L),1L,MutationOperationKind.CREATE)))).code());
            });
            assertNull(f.binding(42).reference());
        }
    }

    @Test public void simultaneousSingleBindingAttemptsHaveExactlyOneCommittedWinner() throws Exception {
        BlobPolicies single=new BlobPolicies(BlobPolicies.Limits.defaults(),BlobPolicyProfile.named("default").sharing(BlobPolicyProfile.Sharing.SINGLE_BINDING).build(),Map.of(),Map.of());
        try (Fixture f=new Fixture(true,single)) {
            BlobRef key=f.upload(); ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1);
            try {
                List<Future<Boolean>> results=new ArrayList<>();
                for (long id:List.of(42L,43L)) results.add(pool.submit(() -> {
                    assertTrue(start.await(5,TimeUnit.SECONDS));
                    try { f.save(f.newAsset(id,key)); return true; }
                    catch (BlobException denied) { assertEquals(BlobException.Code.BLOB_BIND_DENIED,denied.code()); return false; }
                }));
                start.countDown(); int winners=0; for (var result:results) if (result.get(10,TimeUnit.SECONDS)) winners++;
                assertEquals(1,winners); assertEquals(1L,f.scalar("SELECT count(*) FROM asset_data"));
                assertEquals(1L,f.scalar("SELECT count(*) FROM teaql_blob_binding WHERE blob_key IS NOT NULL"));
            } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS)); }
        }
    }

    @Test public void garbageClaimWaitsForBindingLockAndRechecksTheCommittedReference() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef key=f.upload(); ExecutorService pool=Executors.newFixedThreadPool(2);
            f.preparedLatch=new CountDownLatch(1); f.releasePrepared=new CountDownLatch(1);
            try {
                Future<?> save=pool.submit(() -> f.save(f.newAsset(42,key)));
                assertTrue(f.preparedLatch.await(5,TimeUnit.SECONDS));
                CountDownLatch claimStarted=new CountDownLatch(1);
                Future<Optional<BlobCatalogRecord>> claim=pool.submit(() -> { claimStarted.countDown(); return f.catalog.claimDeletion(SCOPE,key,NOW.plusSeconds(1000000)); });
                assertTrue(claimStarted.await(5,TimeUnit.SECONDS));
                assertThrows(TimeoutException.class,() -> claim.get(150,TimeUnit.MILLISECONDS));
                f.releasePrepared.countDown(); save.get(10,TimeUnit.SECONDS);
                assertTrue(claim.get(10,TimeUnit.SECONDS).isEmpty()); f.assertOracle(42,key);
                assertEquals(BlobState.AVAILABLE,f.find(key).state());
            } finally { f.releasePrepared.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS)); }
        }
    }

    @Test public void incompatibleSchemaAndInvalidCompletionNeverBecomeAvailable() throws Exception {
        try (Fixture f=new Fixture()) {
            f.sql.update("UPDATE teaql_blob_schema SET schema_version=2",new Object[0]);
            assertEquals(BlobException.Code.BLOB_SCHEMA_MISSING,assertThrows(BlobException.class,f.catalog::validateSchema).code());
            assertEquals(BlobException.Code.BLOB_SCHEMA_MISSING,assertThrows(BlobException.class,f.catalog::migrate).code());
            assertEquals(2L,f.scalar("SELECT schema_version FROM teaql_blob_schema"));
            f.sql.update("UPDATE teaql_blob_schema SET schema_version=1",new Object[0]);
            BlobRef key=f.catalog.begin(f.draft("invalid-content","a".repeat(64)),NOW).reference();
            assertEquals(BlobException.Code.BLOB_UPLOAD_INCOMPLETE,assertThrows(BlobException.class,() -> f.save(f.newAsset(42,key))).code());
            assertEquals(BlobException.Code.BLOB_FORMAT_NOT_ALLOWED,assertThrows(BlobException.class,
                    () -> f.catalog.complete(SCOPE,key,new PostgresBlobCatalog.VerifiedContent("image/png",12,"a".repeat(64),"version"),NOW)).code());
            assertEquals(BlobException.Code.BLOB_SIZE_EXCEEDED,assertThrows(BlobException.class,
                    () -> f.catalog.complete(SCOPE,key,new PostgresBlobCatalog.VerifiedContent("application/pdf",104857601,"a".repeat(64),"version"),NOW)).code());
            assertEquals(BlobException.Code.BLOB_UPLOAD_EXPIRED,assertThrows(BlobException.class,
                    () -> f.catalog.complete(SCOPE,key,f.content(),NOW.plusSeconds(3600))).code());
            assertEquals(BlobState.INITIATED,f.find(key).state()); assertEquals(0L,f.scalar("SELECT count(*) FROM asset_data"));
            f.sql.execute("ALTER TABLE teaql_blob_binding DROP CONSTRAINT teaql_blob_retired_fk");
            assertEquals(BlobException.Code.BLOB_SCHEMA_MISSING,assertThrows(BlobException.class,f.catalog::validateSchema).code());
        }
    }

    @Test public void postCommitAuditFailureLeavesBothAuthoritativeWritesCommitted() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef key=f.upload(); f.failure="audit";
            assertEquals(TransactionCompletion.Outcome.COMMITTED,assertThrows(TransactionCompletionException.class,() -> f.save(f.newAsset(42,key))).outcome());
            f.assertOracle(42,key); assertEquals(BlobState.AVAILABLE,f.find(key).state());
            assertEquals(1L,f.scalar("SELECT count(*) FROM asset_data"));
            assertFalse(f.sql.hasActiveTransaction());
        }
    }

    @Test public void nativeCompiledAndGenericQueriesHydrateOnlyTypedStableKeys() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef key=f.upload(); f.create(42,key);
            for (boolean generic:List.of(false,true)) {
                long compiled=f.compiledQueries.get(), mapped=f.mappedBusinessQueries.get();
                AssetRequest request=new AssetRequest();
                if (generic) request.addSimpleDynamicProperty("_title",new PropertyReference("title"));
                Asset result=request.comment("read asset").purpose("verify persisted Blob typing").executeForList(f.context()).getData().get(0);
                assertEquals(key,result.document); assertTrue(result.isPropertyLoaded("document"));
                assertTrue(result.getEntityMutationLedger().currentChangeSet().changes().isEmpty());
                if (generic) assertTrue(f.mappedBusinessQueries.get()>mapped); else assertTrue(f.compiledQueries.get()>compiled);
            }
            f.sql.update("UPDATE asset_data SET document=? WHERE id=42",new Object[]{"https://example.invalid/file.pdf"});
            for (boolean generic:List.of(false,true)) {
                AssetRequest request=new AssetRequest(); if (generic) request.addSimpleDynamicProperty("_title",new PropertyReference("title"));
                assertThrows(RuntimeException.class,() -> request.comment("reject corrupt Blob key").purpose("verify SQL codec boundary").executeForList(f.context()));
            }
        }
    }

    public static final class AssetRequest extends BaseRequest<Asset> {
        AssetRequest() { super(Asset.class,Asset::new); List.of("id","version","title","document").forEach(this::selectProperty); }
        @Override public String getTypeName() { return "Asset"; }
        AssetRequest comment(String comment) { internalComment(comment); return this; }
    }

    public static final class Asset extends BaseEntity {
        BlobRef document,preview; String title;
        List<Asset> children=List.of();
        @Override public String typeName() { return "Asset"; }
        @Override public void __internalSet(String field,Object value) {
            switch (field) {
                case "document" -> { document=(BlobRef)value; markPropertyLoaded(field); }
                case "preview" -> { preview=(BlobRef)value; markPropertyLoaded(field); }
                case "title" -> { title=(String)value; markPropertyLoaded(field); }
                case "children" -> { children=(List<Asset>)value; markPropertyLoaded(field); }
                default -> super.__internalSet(field,value);
            }
        }
        @Override public Object __internalGet(String field) {
            return switch (field) { case "document" -> document; case "preview" -> preview; case "title" -> title; case "children" -> children; default -> super.__internalGet(field); };
        }
    }
    static final class MutableClock extends Clock {
        volatile Instant now=NOW;
        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { if (!zone.equals(ZoneOffset.UTC)) throw new IllegalArgumentException(); return this; }
    }
    static final class Fixture implements AutoCloseable {
        final String schema="r3_blob_it_"+UUID.randomUUID().toString().replace("-","");
        final DataSource dataSource;
        final JdbcSqlExecutor sql,otherSql;
        final PostgresBlobCatalog catalog;
        final BlobPolicies policies;
        final BlobFieldSpec spec;
        final MutableClock clock=new MutableClock();
        final PostgresBlobMutationParticipant participant;
        final TeaQLRuntime runtime;
        final AtomicLong uploadSequence=new AtomicLong();
        final AtomicLong compiledQueries=new AtomicLong(),mappedBusinessQueries=new AtomicLong(),blobReadQueries=new AtomicLong();
        final List<RawAuditEvent> audits=new CopyOnWriteArrayList<>();
        final List<MutationPlan> approvedPlans=new CopyOnWriteArrayList<>();
        volatile java.util.function.Consumer<List<Map<String,Object>>> candidateReadHook=rows -> {};
        volatile java.util.function.Consumer<RawAuditEvent> auditHook=event -> {};
        volatile String failure="";
        volatile Long prepareTransaction,businessTransaction,applyTransaction;
        volatile CountDownLatch preparedLatch,releasePrepared;
        Fixture() throws Exception { this(true,BlobPolicies.defaults()); }
        Fixture(boolean migrate,BlobPolicies policies) throws Exception { this(migrate,policies,List.of("pdf")); }
        Fixture(boolean migrate,BlobPolicies policies,List<String> extensions) throws Exception {
            this(migrate,policies,extensions,false);
        }
        Fixture(boolean migrate,BlobPolicies policies,List<String> extensions,boolean graph) throws Exception {
            this(migrate,policies,extensions,graph,false);
        }
        Fixture(boolean migrate,BlobPolicies policies,List<String> extensions,boolean graph,boolean multiFields) throws Exception {
            this.policies=policies;
            spec=BlobFieldSpec.of("Asset","document",extensions,true,"default");
            String url=required("TEAQL_BLOB_TEST_PG_URL"),user=required("TEAQL_BLOB_TEST_PG_USER"),password=required("TEAQL_BLOB_TEST_PG_PASSWORD");
            dataSource=new TestDataSource(url,user,password,schema);
            try (Connection c=DriverManager.getConnection(url,user,password); Statement s=c.createStatement()) { s.execute("CREATE SCHEMA "+schema); }
            sql=new JdbcSqlExecutor(dataSource) {
                @Override public <T extends Entity> List<T> query(String statement,Object[] parameters,CompiledRowMapper<T> mapper) {
                    compiledQueries.incrementAndGet(); return super.query(statement,parameters,mapper);
                }
                @Override public List<Map<String,Object>> queryForList(String statement,Object[] parameters) {
                    if (statement.contains(" AS read_entity")) blobReadQueries.incrementAndGet();
                    if (statement.startsWith("SELECT ") && statement.contains("asset_data")) mappedBusinessQueries.incrementAndGet();
                    var rows=super.queryForList(statement,parameters);
                    if (statement.startsWith("SELECT o.* FROM teaql_blob_object o WHERE")) candidateReadHook.accept(rows);
                    return rows;
                }
            };
            otherSql=new JdbcSqlExecutor(dataSource); catalog=new PostgresBlobCatalog(sql);
            sql.execute("CREATE TABLE asset_data(id BIGINT PRIMARY KEY, version BIGINT NOT NULL, document VARCHAR(128), title VARCHAR(100) CHECK (title <> 'fail'))");
            if (multiFields) sql.execute("ALTER TABLE asset_data ADD COLUMN preview VARCHAR(128)");
            if (migrate) catalog.migrate();
            SimpleEntityMetaFactory metadata=new SimpleEntityMetaFactory(); SQLEntityDescriptor descriptor=new SQLEntityDescriptor();
            descriptor.setType("Asset"); descriptor.setEntitySupplier(Asset::new); descriptor.setTargetType(Asset.class);
            ((GenericSQLProperty)descriptor.addSimpleProperty("id",Long.class)).setColumnType("BIGINT");
            ((GenericSQLProperty)descriptor.addSimpleProperty("version",Long.class)).setColumnType("BIGINT");
            ((GenericSQLProperty)descriptor.addSimpleProperty("title",String.class)).setColumnType("VARCHAR(100)");
            BlobSQLProperty document=(BlobSQLProperty)descriptor.addSimpleProperty("document",BlobRef.class,BlobSQLProperty::new);
            document.setTableName("asset_data"); document.setColumnType("VARCHAR(128)"); document.withBlobFieldSpec(spec); metadata.register(descriptor);
            if (multiFields) {
                BlobSQLProperty preview=(BlobSQLProperty)descriptor.addSimpleProperty("preview",BlobRef.class,BlobSQLProperty::new);
                preview.setTableName("asset_data"); preview.setColumnType("VARCHAR(128)");
                preview.withBlobFieldSpec(BlobFieldSpec.of("Asset","preview",extensions,true,"default"));
            }
            if (graph) {
                var relation=new io.teaql.core.meta.Relation(); relation.setName("children"); relation.setOwner(descriptor);
                var properties=new ArrayList<>(descriptor.getProperties()); properties.add(relation); descriptor.setProperties(properties);
            }
            var authorization=new BlobBindingAuthorization(context -> context.getAttribute("scope",BlobScope.class),
                    (context,target,kind) -> !Boolean.FALSE.equals(context.getAttribute("field")) && !target.equals(context.getAttribute("deniedTarget")),
                    (context,target,blob) -> !Boolean.FALSE.equals(context.getAttribute("policy")) && !target.equals(context.getAttribute("deniedPolicyTarget")));
            participant=new PostgresBlobMutationParticipant(catalog,policies,authorization,clock);
            BlobMutationParticipant observed=(context,approved) -> {
                approvedPlans.add(approved);
                BlobMutationParticipant.Plan plan=participant.plan(context,approved);
                return new BlobMutationParticipant.Plan() {
                    @Override public void requireTransaction(Object resource) { plan.requireTransaction(resource); }
                    @Override public BlobMutationParticipant.Prepared prepare(UserContext actor,Object resource) {
                        prepareTransaction=scalar("SELECT txid_current()"); var prepared=plan.prepare(actor,resource);
                        if (preparedLatch!=null) {
                            preparedLatch.countDown();
                            try { if (!releasePrepared.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("prepare gate timed out"); }
                            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("prepare gate interrupted"); }
                        }
                        return (current,transaction,entities) -> {
                            applyTransaction=scalar("SELECT txid_current()");
                            if (failure.equals("terminate-before-apply")) {
                                assertTrue(scalar("SELECT count(*) FROM asset_data")>0);
                                long pid=scalar("SELECT pg_backend_pid()");
                                sql.queryForList("SELECT set_config('application_name',?,true)",new Object[]{schema});
                                var terminated=otherSql.queryForList("SELECT pg_terminate_backend(pid) AS terminated FROM pg_catalog.pg_stat_activity "
                                        +"WHERE pid=? AND application_name=? AND usename=current_user AND datname=current_database()",new Object[]{Math.toIntExact(pid),schema});
                                assertEquals(1,terminated.size()); assertEquals(Boolean.TRUE,terminated.get(0).get("terminated"));
                                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
                                while (!otherSql.queryForList("SELECT pid FROM pg_catalog.pg_stat_activity WHERE pid=? AND application_name=?",new Object[]{Math.toIntExact(pid),schema}).isEmpty()) {
                                    if (System.nanoTime()>deadline) throw new IllegalStateException("Owned backend termination deadline");
                                    try { Thread.sleep(10); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
                                }
                                throw new IllegalStateException("Owned connection terminated before binding apply");
                            }
                            if (failure.equals("before-apply")) throw new IllegalStateException("before apply");
                            prepared.apply(current,transaction,entities);
                            if (failure.equals("after-apply")) throw new IllegalStateException("after apply");
                        };
                    }
                };
            };
            var executor=new SqlDataServiceExecutor("default",sql) {
                @Override public MutationResult mutate(UserContext context,PersistenceMutation request) {
                    businessTransaction=scalar("SELECT txid_current()"); return super.mutate(context,request);
                }
                @Override public List<MutationResult> mutateBatch(UserContext context,MutationBatchRequest request) {
                    businessTransaction=scalar("SELECT txid_current()"); return super.mutateBatch(context,request);
                }
            };
            runtime=TeaQLRuntime.builder().metadata(metadata).dataService("default",executor).blobMutationParticipant(observed)
                    .telemetry(RuntimeTelemetry.NOOP).executionLogging(false).logSink(new RuntimeLogSink() {
                        @Override public void writeExecutionLog(UserContext context,ExecutionMetadata execution) {}
                        @Override public void writeAuditEvent(UserContext context,RawAuditEvent event) {
                            assertFalse(sql.hasActiveTransaction()); audits.add(event); auditHook.accept(event);
                            if (failure.equals("audit")) throw new IllegalStateException("audit delivery failed");
                        }
                    }).build();
        }
        DefaultUserContext context() { var context=new DefaultUserContext(runtime); context.putAttribute("scope",SCOPE); return context; }
        void save(Asset asset) { runtime.saveGraph(context(),asset); }
        Asset newAsset(long id,BlobRef key) {
            Asset asset=new Asset(); asset.__internalInitializeNewEntityId(id); asset.updateProperty("title","asset");
            asset.updateProperty("document",key); asset.setComment("bind asset document"); return asset;
        }
        Asset create(long id,BlobRef key) { Asset asset=newAsset(id,key); save(asset); return asset; }
        Asset load(long id) {
            var row=row(id); Asset asset=new Asset(); asset.__internalSet("id",id); asset.__internalSet("version",((Number)row.get("version")).longValue());
            asset.__internalSet("title",row.get("title")); asset.__internalSet("document",row.get("document")==null?null:BlobRef.of((String)row.get("document")));
            if (row.containsKey("preview")) asset.__internalSet("preview",row.get("preview")==null?null:BlobRef.of((String)row.get("preview")));
            asset.set$status(asset.getVersion()<0?EntityStatus.PERSISTED_DELETED:EntityStatus.PERSISTED); asset.setComment("change asset document"); return asset;
        }
        Map<String,Object> row(long id) { return otherSql.queryForList("SELECT * FROM asset_data WHERE id=?",new Object[]{id}).get(0); }
        PostgresBlobCatalog.Binding binding(long id) { return catalog.binding(SCOPE,new BlobFieldTarget("Asset",id,"document")).orElseThrow(); }
        BlobCatalogRecord find(BlobRef key) { return catalog.find(SCOPE,key).orElseThrow(); }
        long scalar(String query) { return ((Number)sql.queryForList(query,new Object[0]).get(0).values().iterator().next()).longValue(); }
        void assertOracle(long id,BlobRef expected) {
            var result=otherSql.queryForList("SELECT a.document,b.blob_key FROM asset_data a LEFT JOIN teaql_blob_binding b ON b.entity_type='Asset' "
                    + "AND b.entity_id=a.id AND b.field_name='document' AND b.scope_realm=? AND b.scope_boundary=? WHERE a.id=?",new Object[]{SCOPE.realm(),SCOPE.boundary(),id}).get(0);
            assertEquals(expected==null?null:expected.key(),result.get("document")); assertEquals(expected==null?null:expected.key(),result.get("blob_key"));
        }
        PostgresBlobCatalog.UploadDraft draft(String request,String digest) {
            long id=uploadSequence.incrementAndGet(); return new PostgresBlobCatalog.UploadDraft(SCOPE,BlobRef.of("blob_it_"+id),"upload_"+id,
                    request,digest,"sample.pdf","pdf","test-location/"+id,new BlobFieldTarget("Asset",null,"document"),policies.resolve(spec));
        }
        PostgresBlobCatalog.VerifiedContent content() throws Exception {
            byte[] bytes=Files.readAllBytes(Path.of(required("TEAQL_BLOB_TEST_FIXTURES"),"sample.pdf"));
            return new PostgresBlobCatalog.VerifiedContent("application/pdf",bytes.length,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),"fixture-v1");
        }
        BlobRef upload() throws Exception {
            long next=uploadSequence.get()+1; var draft=draft("request_"+next,"a".repeat(64));
            BlobRef key=catalog.begin(draft,NOW).reference(); catalog.complete(SCOPE,key,content(),NOW); return key;
        }
        @Override public void close() { runtime.close(); sql.execute("DROP SCHEMA "+schema+" CASCADE"); }
    }
    private static String required(String name) {
        String value=System.getenv(name); if (value==null || value.isBlank()) throw new IllegalStateException("Integration environment missing: "+name); return value;
    }
    static final class TestDataSource implements DataSource {
        private final String url,user,password,schema;
        TestDataSource(String url,String user,String password,String schema) { this.url=url; this.user=user; this.password=password; this.schema=schema; }
        @Override public Connection getConnection() throws SQLException {
            Connection c=DriverManager.getConnection(url,user,password);
            try (Statement s=c.createStatement()) { s.execute("SET search_path TO "+schema); s.execute("SET lock_timeout='5s'"); s.execute("SET statement_timeout='10s'"); }
            catch (SQLException failure) { c.close(); throw failure; } return c;
        }
        @Override public Connection getConnection(String user,String password) throws SQLException { throw new SQLFeatureNotSupportedException(); }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter writer) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() { return Logger.getLogger("teaql.blob.test"); }
        @Override public <T> T unwrap(Class<T> type) throws SQLException { throw new SQLFeatureNotSupportedException(); }
        @Override public boolean isWrapperFor(Class<?> type) { return false; }
    }
}
