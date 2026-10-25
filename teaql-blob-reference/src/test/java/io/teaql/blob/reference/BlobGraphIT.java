package io.teaql.blob.reference;

import io.teaql.blob.content.tika.TikaBlobContentVerifier;
import io.teaql.blob.store.file.FileBlobStore;
import io.teaql.core.*;
import io.teaql.core.blob.*;
import io.teaql.core.meta.*;
import io.teaql.dataservice.sql.SqlDataServiceExecutor;
import io.teaql.runtime.DefaultDataServiceRegistry;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static io.teaql.blob.reference.PostgresBlobCatalogIT.*;
import static io.teaql.core.blob.BlobException.Code.*;
import static org.junit.Assert.*;

/** Actual native SQL graph plans, two Blob fields, explicit cascade intent and immutable-file GC. */
public class BlobGraphIT {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();

    @Test public void cyclicThreeLevelGraphBindsEveryFieldOnceAndSharesKeysWithoutPrematureUnreference() throws Exception {
        try (Fixture f=fixture()) {
            BlobRef a=f.upload(),b=f.upload(); Asset root=asset(f,42,a,b),child=asset(f,43,b,a),leaf=asset(f,44,a,null);
            root.children=List.of(child); child.children=List.of(leaf); leaf.children=List.of(root); f.save(root);
            pair(f,42,a,b); pair(f,43,b,a); pair(f,44,a,null); assertEquals(3,count(f,a)); assertEquals(2,count(f,b));
            assertEquals(3,f.audits.size()); assertEquals(3,f.scalar("SELECT count(*) FROM asset_data"));
            for (Asset entity:List.of(root,child,leaf)) { assertEquals(Long.valueOf(1),entity.getVersion()); assertFalse(entity.getEntityMutationLedger().hasUncommittedChanges()); }
            assertNull(f.find(a).unreferencedAt()); assertNull(f.find(b).unreferencedAt()); f.save(root); assertEquals(3,f.audits.size());
        }
    }

    @Test public void singleBindingKeysCanSwapFieldsOrTransferAcrossGraphEntitiesAsOneNetPlan() throws Exception {
        var policies=new BlobPolicies(BlobPolicies.Limits.defaults(),BlobPolicyProfile.named("default").sharing(BlobPolicyProfile.Sharing.SINGLE_BINDING).build(),Map.of(),Map.of());
        try (Fixture f=fixture(policies)) {
            BlobRef a=f.upload(),b=f.upload(); Asset root=asset(f,42,a,b); f.save(root);
            root.updateProperty("document",b); root.updateProperty("preview",a); f.save(root); pair(f,42,b,a);
            assertEquals(1,count(f,a)); assertEquals(1,count(f,b)); assertTrue(f.find(a).singleBinding());
            Asset child=asset(f,43,a,null);
            assertEquals(BLOB_BIND_DENIED,assertThrows(BlobException.class,() -> f.save(child)).code()); assertEquals(1,f.scalar("SELECT count(*) FROM asset_data"));
            root.updateProperty("preview",null); root.children=List.of(child); f.save(root);
            pair(f,42,b,null); pair(f,43,a,null); assertEquals(1,count(f,a)); assertNull(f.find(a).unreferencedAt());
            assertEquals(Long.valueOf(3),root.getVersion()); assertEquals(Long.valueOf(1),child.getVersion());
        }
    }

    @Test public void explicitPartialCascadeDeletesAndRecoversAllFieldsFromAuthoritativeHistory() throws Exception {
        try (Fixture f=fixture()) {
            BlobRef a=f.upload(),b=f.upload(); Asset root=asset(f,42,a,b),child=asset(f,43,b,a),leaf=asset(f,44,a,b); root.children=List.of(child); child.children=List.of(leaf); f.save(root);
            Asset outsider=asset(f,45,a,null); f.save(outsider);
            root=partial(42,1); child=partial(43,1); leaf=partial(44,1); root.children=List.of(child); child.children=List.of(leaf);
            for (Asset entity:List.of(root,child,leaf)) entity.markForDeletion(); f.save(root);
            for (long id:List.of(42L,43L,44L)) {
                assertEquals(-2L,((Number)f.row(id).get("version")).longValue());
                assertNull(binding(f,id,"document").reference()); assertNull(binding(f,id,"preview").reference());
                assertNotNull(binding(f,id,"document").retiredReference()); assertNotNull(binding(f,id,"preview").retiredReference());
            }
            pair(f,45,a,null); assertEquals(1,count(f,a)); assertEquals(0,count(f,b)); assertNull(f.find(a).unreferencedAt()); assertEquals(NOW,f.find(b).unreferencedAt());
            for (Asset entity:List.of(root,child,leaf)) entity.markAsRecover(); f.save(root);
            pair(f,42,a,b); pair(f,43,b,a); pair(f,44,a,b); assertEquals(4,count(f,a)); assertEquals(3,count(f,b));
            for (Asset entity:List.of(root,child,leaf)) assertEquals(Long.valueOf(3),entity.getVersion());
            assertNull(f.find(b).unreferencedAt());
        }
    }

    @Test public void unmarkedRelatedEntityKeepsItsRowsAndBindingsWhenOnlyTheRootIsDeleted() throws Exception {
        try (Fixture f=fixture()) {
            BlobRef a=f.upload(),b=f.upload(); Asset root=asset(f,42,a,b),child=asset(f,43,b,a); root.children=List.of(child); f.save(root);
            root.markForDeletion(); f.save(root); pair(f,43,b,a);
            assertEquals(-2L,((Number)f.row(42).get("version")).longValue()); assertEquals(1L,((Number)f.row(43).get("version")).longValue());
            assertEquals(1,count(f,a)); assertEquals(1,count(f,b)); assertNull(f.find(a).unreferencedAt()); assertNull(f.find(b).unreferencedAt());
        }
    }

    @Test public void partialTitleOnlyGraphUpdatesPreserveBothUnloadedKeysAndTheirBindingClocks() throws Exception {
        try (Fixture f=fixture()) {
            BlobRef a=f.upload(),b=f.upload(); Asset root=asset(f,42,a,b),child=asset(f,43,b,a); root.children=List.of(child); f.save(root);
            var before=List.of(binding(f,42,"document"),binding(f,42,"preview"),binding(f,43,"document"),binding(f,43,"preview"));
            root=partial(42,1); child=partial(43,1); root.children=List.of(child); child.updateProperty("title","partial child"); f.clock.now=NOW.plusSeconds(15); f.save(root);
            pair(f,42,a,b); pair(f,43,b,a); assertEquals(before,List.of(binding(f,42,"document"),binding(f,42,"preview"),binding(f,43,"document"),binding(f,43,"preview")));
            assertEquals("partial child",f.row(43).get("title")); assertEquals(1L,((Number)f.row(42).get("version")).longValue());
            assertEquals(2L,((Number)f.row(43).get("version")).longValue());
        }
    }

    @Test public void invalidFormatInOneFieldRejectsTheWholeNewGraphBeforeBusinessWrites() throws Exception {
        try (Fixture f=fixture()) {
            BlobRef key=f.upload(); Asset root=asset(f,42,key,null),child=asset(f,43,null,key); root.children=List.of(child);
            f.runtime.getMetadata().resolveEntityDescriptor("Asset").getProperties().stream().filter(value -> value.getName().equals("preview")).findFirst().orElseThrow()
                    .withBlobFieldSpec(BlobFieldSpec.of("Asset","preview",List.of("png"),true,"default"));
            assertEquals(BLOB_FORMAT_NOT_ALLOWED,assertThrows(BlobException.class,() -> f.save(root)).code());
            assertEquals(0,f.scalar("SELECT count(*) FROM asset_data")); assertEquals(0,f.scalar("SELECT count(*) FROM teaql_blob_binding")); assertTrue(f.audits.isEmpty());
            assertEquals(BlobState.UPLOADED_UNBOUND,f.find(key).state()); assertNull(root.getVersion()); assertNull(child.getVersion());
        }
    }

    @Test public void currentHostPermissionDenialOnOneCascadeFieldRollsBackEveryEntityAndBinding() throws Exception {
        try (Fixture f=fixture()) {
            BlobRef key=f.upload(); Asset root=asset(f,42,key,key),child=asset(f,43,key,key); root.children=List.of(child); f.save(root); int audits=f.audits.size();
            root.markForDeletion(); child.markForDeletion(); var actor=f.context(); actor.putAttribute("deniedTarget",new BlobFieldTarget("Asset",43L,"preview"));
            assertEquals(BLOB_BIND_DENIED,assertThrows(BlobException.class,() -> f.runtime.saveGraph(actor,root)).code());
            pair(f,42,key,key); pair(f,43,key,key); assertEquals(4,count(f,key)); assertEquals(audits,f.audits.size());
            assertEquals(Long.valueOf(1),root.getVersion()); assertEquals(Long.valueOf(1),child.getVersion()); f.save(root);
            assertEquals(0,count(f,key)); assertEquals(-2L,((Number)f.row(43).get("version")).longValue());
        }
    }

    @Test public void recoveryRechecksAdditionalPolicyForEveryRetiredFieldAndRetriesAfterDenial() throws Exception {
        try (Fixture f=fixture()) {
            BlobRef key=f.upload(); Asset root=asset(f,42,key,key),child=asset(f,43,key,key); root.children=List.of(child); f.save(root);
            root.markForDeletion(); child.markForDeletion(); f.save(root); int audits=f.audits.size(); root.markAsRecover(); child.markAsRecover();
            var actor=f.context(); actor.putAttribute("deniedPolicyTarget",new BlobFieldTarget("Asset",43L,"preview"));
            assertEquals(BLOB_BIND_DENIED,assertThrows(BlobException.class,() -> f.runtime.saveGraph(actor,root)).code());
            assertEquals(0,count(f,key)); assertEquals(audits,f.audits.size()); assertEquals(-2L,((Number)f.row(42).get("version")).longValue());
            assertEquals(key,binding(f,43,"preview").retiredReference()); f.save(root); pair(f,42,key,key); pair(f,43,key,key); assertEquals(4,count(f,key));
        }
    }

    @Test public void failureAfterWholeBusinessBatchRollsBackAndRetainsEveryFieldForOneRetry() throws Exception {
        try (Fixture f=fixture()) {
            BlobRef a=f.upload(),b=f.upload(); Asset root=asset(f,42,a,b),child=asset(f,43,b,a),leaf=asset(f,44,a,b); root.children=List.of(child); child.children=List.of(leaf);
            f.failure="before-apply"; assertThrows(IllegalStateException.class,() -> f.save(root));
            assertNotNull(f.applyTransaction); assertEquals(0,f.scalar("SELECT count(*) FROM asset_data")); assertEquals(0,f.scalar("SELECT count(*) FROM teaql_blob_binding")); assertTrue(f.audits.isEmpty());
            for (Asset entity:List.of(root,child,leaf)) assertNull(entity.getVersion()); f.failure=""; f.save(root);
            pair(f,42,a,b); pair(f,43,b,a); pair(f,44,a,b); assertEquals(3,f.audits.size()); assertEquals(3,count(f,a)); assertEquals(3,count(f,b));
        }
    }

    @Test public void staleDescendantRollsBackOtherGraphWritesAndFreshGraphCanApplyTheIntendedChange() throws Exception {
        try (Fixture f=fixture()) {
            BlobRef a=f.upload(),b=f.upload(),next=f.upload(); Asset root=asset(f,42,a,b),child=asset(f,43,b,a); root.children=List.of(child); f.save(root); int audits=f.audits.size();
            f.otherSql.update("UPDATE asset_data SET version=2,title='external child' WHERE id=43",new Object[0]);
            root.updateProperty("title","new root"); child.updateProperty("preview",next);
            assertThrows(RuntimeException.class,() -> f.save(root)); pair(f,42,a,b); pair(f,43,b,a); assertEquals("asset",f.row(42).get("title"));
            assertEquals("external child",f.row(43).get("title")); assertEquals(audits,f.audits.size()); assertEquals(BlobState.UPLOADED_UNBOUND,f.find(next).state());
            Asset freshRoot=f.load(42),freshChild=f.load(43); freshRoot.children=List.of(freshChild); freshRoot.updateProperty("title","new root"); freshChild.updateProperty("preview",next); f.save(freshRoot);
            pair(f,43,b,next); assertEquals(2L,((Number)f.row(42).get("version")).longValue()); assertEquals(3L,((Number)f.row(43).get("version")).longValue());
        }
    }

    @Test public void longestCrossFieldPolicySurvivesDetachmentAndControlsActualFileCollection() throws Exception {
        BlobPolicies policies=new BlobPolicies(BlobPolicies.Limits.defaults(),BlobPolicyProfile.named("default").unreferencedRetention(Duration.ofSeconds(10)).minimumRetention(Duration.ofSeconds(5)).build(),
                Map.of("long",BlobPolicyProfile.named("long").unreferencedRetention(Duration.ofSeconds(40)).minimumRetention(Duration.ofSeconds(100)).build()),Map.of("Asset.preview","long"));
        try (Fixture f=fixture(policies)) {
            Path root=temporary.newFolder().toPath(); FileBlobStore files=new FileBlobStore(root.resolve("files")); files.initialize();
            var upload=new BlobUploadService(f.catalog,files,new TikaBlobContentVerifier(root.resolve("verify"),TikaBlobContentVerifier.Limits.defaults()),policies,
                    new BlobUploadAuthorization(actor -> actor.getAttribute("scope",BlobScope.class),(actor,target) -> true,(actor,target,action,facts) -> true),f.clock);
            byte[] bytes=Files.readAllBytes(Path.of(Objects.requireNonNull(System.getenv("TEAQL_BLOB_TEST_FIXTURES"))).resolve("sample.pdf"));
            var session=upload.begin(f.context(),new BlobFieldTarget("Asset",null,"document"),new BlobRuntime.Uploads.FileInfo("sample.pdf",null,null),"cross-policy");
            upload.write(f.context(),session.uploadId(),new ByteArrayInputStream(bytes)); BlobRef key=upload.complete(f.context(),session.uploadId());
            Asset entity=asset(f,42,key,key),child=asset(f,43,key,null); entity.children=List.of(child); f.save(entity); assertEquals(3,count(f,key));
            assertEquals(NOW.plusSeconds(100),f.find(key).retentionFloorUntil()); assertEquals(40,f.find(key).effectiveUnreferencedSeconds());
            f.clock.now=NOW.plusSeconds(1); entity.updateProperty("preview",null); f.save(entity); assertEquals(2,count(f,key)); assertNull(f.find(key).unreferencedAt());
            f.clock.now=NOW.plusSeconds(3); entity.updateProperty("document",null); child.updateProperty("document",null); f.save(entity);
            assertEquals(0,count(f,key)); assertEquals(NOW.plusSeconds(3),f.find(key).unreferencedAt()); assertEquals(NOW.plusSeconds(100),f.find(key).retentionFloorUntil());
            String location=f.find(key).storageLocation(); var maintenance=new BlobMaintenanceService(f.catalog,files,actor -> actor.getAttribute("scope",BlobScope.class),(actor,scope) -> true,f.clock);
            f.clock.now=NOW.plusSeconds(99); assertTrue(maintenance.runBatch(f.context()).outcomes().isEmpty()); assertTrue(files.stat(f.context(),location).isPresent());
            f.clock.now=NOW.plusSeconds(100); assertEquals(BlobRuntime.Maintenance.Status.DELETED,maintenance.runBatch(f.context()).outcomes().get(0).status());
            assertEquals(BlobState.DELETED,f.find(key).state()); assertTrue(files.stat(f.context(),location).isEmpty());
        }
    }

    @Test public void materializedDescendantWinsOverReferAliasesInEitherTraversalOrder() throws Exception {
        for (boolean referenceFirst:List.of(false,true)) try (Fixture f=fixture()) {
            BlobRef a=f.upload(),b=f.upload(),next=f.upload(); Asset root=asset(f,42,a,b),child=asset(f,43,b,a); root.children=List.of(child); f.save(root);
            Asset reference=new Asset(); reference.__internalSet("id",43L); reference.set$status(EntityStatus.REFER);
            root.children=referenceFirst?List.of(reference,child):List.of(child,reference); child.updateProperty("preview",next); f.save(root);
            pair(f,42,a,b); pair(f,43,b,next); assertEquals(Long.valueOf(2),child.getVersion()); assertNull(reference.getVersion()); assertEquals(2,f.scalar("SELECT count(*) FROM asset_data"));
        }
    }

    @Test public void crossRouteGraphRejectsEvenWhenBothRoutesExposeTheSamePhysicalAdapter() throws Exception {
        try (Fixture f=fixture()) {
            var descriptor=new EntityDescriptor(); descriptor.setType("ForeignRoot"); descriptor.setDataService("other"); descriptor.addSimpleProperty("id",Long.class); descriptor.addSimpleProperty("version",Long.class); descriptor.addSimpleProperty("title",String.class);
            Relation children=new Relation(); children.setName("children"); children.setOwner(descriptor); descriptor.getProperties().add(children); f.runtime.getMetadata().register(descriptor);
            ((DefaultDataServiceRegistry)f.runtime.getRegistry()).register("other",new SqlDataServiceExecutor("other",f.sql));
            ForeignRoot root=new ForeignRoot(); root.__internalInitializeNewEntityId(99L); root.updateProperty("title","foreign"); root.setComment("reject distributed graph"); root.children=List.of(asset(f,42,f.upload(),null));
            var failure=assertThrows(TeaQLRuntimeException.class,() -> f.runtime.saveGraph(f.context(),root)); assertTrue(failure.getMessage().contains("route"));
            assertEquals(0,f.scalar("SELECT count(*) FROM asset_data")); assertEquals(0,f.scalar("SELECT count(*) FROM teaql_blob_binding")); assertTrue(f.approvedPlans.isEmpty());
        }
    }

    @Test public void simultaneousMultiFieldSwapsHaveOneOptimisticWinnerAndNoBrokenBindings() throws Exception {
        try (Fixture f=fixture()) {
            BlobRef a=f.upload(),b=f.upload(); f.save(asset(f,42,a,b)); Asset first=f.load(42),second=f.load(42);
            for (Asset entity:List.of(first,second)) { entity.updateProperty("document",b); entity.updateProperty("preview",a); }
            ExecutorService workers=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1);
            try {
                List<Future<Boolean>> futures=new ArrayList<>();
                for (Asset entity:List.of(first,second)) futures.add(workers.submit(() -> {
                    assertTrue(start.await(5,TimeUnit.SECONDS));
                    try { f.save(entity); return true; } catch (RuntimeException failed) { assertFalse(failed instanceof BlobException); return false; }
                }));
                start.countDown(); int winners=0; for (var future:futures) if (future.get(10,TimeUnit.SECONDS)) winners++;
                assertEquals(1,winners); pair(f,42,b,a); assertEquals(2L,((Number)f.row(42).get("version")).longValue()); assertEquals(1,count(f,a)); assertEquals(1,count(f,b));
            } finally { workers.shutdownNow(); assertTrue(workers.awaitTermination(5,TimeUnit.SECONDS)); }
        }
    }

    @Test public void conflictingMaterializedAliasesRejectBeforeIntentIsMergedOrSilentlyOverwritten() throws Exception {
        try (Fixture f=fixture()) {
            BlobRef a=f.upload(),b=f.upload(),nextDocument=f.upload(),nextPreview=f.upload();
            Asset root=asset(f,42,a,b),child=asset(f,43,b,a); root.children=List.of(child); f.save(root); int audits=f.audits.size();
            Asset first=f.load(43),second=f.load(43); first.updateProperty("document",nextDocument); second.updateProperty("preview",nextPreview); root.children=List.of(first,second);
            var failure=assertThrows(TeaQLRuntimeException.class,() -> {
                f.save(root); var persisted=f.row(43);
                throw new AssertionError("Ambiguous save reached SQL; document intent retained="+nextDocument.key().equals(persisted.get("document"))
                        +", preview intent retained="+nextPreview.key().equals(persisted.get("preview")));
            }); assertTrue(failure.getMessage().contains("ambiguous"));
            pair(f,43,b,a); assertEquals(1L,((Number)f.row(43).get("version")).longValue()); assertEquals(audits,f.audits.size());
            assertNotSame(first.getEntityMutationLedger(),second.getEntityMutationLedger());
            assertEquals(nextDocument,first.getEntityMutationLedger().get(new EntityKey("Asset",43L),"document"));
            assertEquals(nextPreview,second.getEntityMutationLedger().get(new EntityKey("Asset",43L),"preview"));
            first.updateProperty("preview",nextPreview); root.children=List.of(first); f.save(root); pair(f,43,nextDocument,nextPreview);
            Asset readOnlyCopy=f.load(43); root.children=List.of(first,readOnlyCopy); f.save(root); assertEquals(2L,((Number)f.row(43).get("version")).longValue());
        }
    }

    @Test public void generatedIdCollisionRejectsAfterAllocationBeforeAnyGraphIntentIsMerged() throws Exception {
        try (Fixture f=fixture(); var runtime=io.teaql.runtime.TeaQLRuntime.builder().metadata(f.runtime.getMetadata()).registry(f.runtime.getRegistry())
                .idGenerationService((actor,entity) -> 42L).blobMutationParticipant(f.participant).telemetry(io.teaql.runtime.RuntimeTelemetry.NOOP).build()) {
            BlobRef key=f.upload(); Asset root=asset(f,42,key,null),child=new Asset(); child.updateProperty("title","child"); child.updateProperty("document",key); root.children=List.of(child);
            var failure=assertThrows(TeaQLRuntimeException.class,() -> runtime.saveGraph(f.context(),root)); assertTrue(failure.getMessage().contains("ambiguous"));
            assertEquals(Long.valueOf(42),child.getId()); assertNotSame(root.getEntityMutationLedger(),child.getEntityMutationLedger());
            assertEquals(0,f.scalar("SELECT count(*) FROM asset_data")); assertEquals(0,f.scalar("SELECT count(*) FROM teaql_blob_binding"));
        }
    }

    @Test public void readonlyAliasesOfAnUntouchedEntityRemainValidWhileAnotherCohortEntityHasPendingWrites() throws Exception {
        for (boolean copyFirst:List.of(false,true)) try (Fixture f=fixture()) {
            BlobRef a=f.upload(),b=f.upload(); Asset root=asset(f,42,a,b),child=asset(f,43,b,a); root.children=List.of(child); f.save(root);
            Asset copy=f.load(43); root.children=copyFirst?List.of(copy,child):List.of(child,copy);
            f.sql.executeInTransaction(() -> {
                root.updateProperty("title","pending root"); f.save(root); assertTrue(root.getEntityMutationLedger().hasUncommittedChanges());
                root.updateProperty("title","later root"); f.save(root);
            });
            pair(f,42,a,b); pair(f,43,b,a); assertEquals("later root",f.row(42).get("title"));
            assertEquals(3L,((Number)f.row(42).get("version")).longValue()); assertEquals(1L,((Number)f.row(43).get("version")).longValue());
            assertEquals(Long.valueOf(1),child.getVersion()); assertEquals(Long.valueOf(1),copy.getVersion());
        }
    }

    private static Fixture fixture() throws Exception { return fixture(BlobPolicies.defaults()); }
    private static Fixture fixture(BlobPolicies policies) throws Exception { return new Fixture(true,policies,List.of("pdf"),true,true); }
    private static Asset asset(Fixture f,long id,BlobRef document,BlobRef preview) { Asset result=f.newAsset(id,document); result.updateProperty("preview",preview); return result; }
    private static Asset partial(long id,long version) { Asset result=new Asset(); result.__internalSet("id",id); result.__internalSet("version",version); result.set$status(EntityStatus.PERSISTED); result.setComment("change partial graph"); return result; }
    private static PostgresBlobCatalog.Binding binding(Fixture f,long id,String field) { return f.catalog.binding(SCOPE,new BlobFieldTarget("Asset",id,field)).orElseThrow(); }
    private static long count(Fixture f,BlobRef key) { return ((Number)f.otherSql.queryForList("SELECT count(*) AS n FROM teaql_blob_binding WHERE blob_key=?",new Object[]{key.key()}).get(0).get("n")).longValue(); }
    private static void pair(Fixture f,long id,BlobRef document,BlobRef preview) {
        var row=f.row(id); assertEquals(document==null?null:document.key(),row.get("document")); assertEquals(preview==null?null:preview.key(),row.get("preview"));
        assertEquals(document,binding(f,id,"document").reference()); assertEquals(preview,binding(f,id,"preview").reference());
    }
    private static final class ForeignRoot extends BaseEntity {
        String title; List<Asset> children=List.of();
        @Override public String typeName() { return "ForeignRoot"; }
        @Override public Object __internalGet(String field) { return switch (field) { case "title" -> title; case "children" -> children; default -> super.__internalGet(field); }; }
        @Override public void __internalSet(String field,Object value) { if (field.equals("title")) { title=(String)value; markPropertyLoaded(field); } else super.__internalSet(field,value); }
    }
}
