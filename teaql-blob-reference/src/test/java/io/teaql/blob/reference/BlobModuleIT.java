package io.teaql.blob.reference;

import io.teaql.blob.content.tika.TikaBlobContentVerifier;
import io.teaql.blob.store.file.*;
import io.teaql.core.*;
import io.teaql.core.blob.*;
import io.teaql.core.blob.BlobRuntime.Reads.*;
import io.teaql.dataservice.sql.SqlDataServiceExecutor;
import io.teaql.runtime.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static io.teaql.blob.reference.PostgresBlobCatalogIT.*;
import static io.teaql.core.blob.BlobException.Code.*;
import static org.junit.Assert.*;

/** Public module/capability consumer with real PostgreSQL, actual verification and file HTTP transport. */
public class BlobModuleIT {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();

    @Test public void optionalCommandCapabilityInstallsPassivelyAndRequiresItsOwnExplicitMigration() throws Exception {
        try (ModuleFixture m=new ModuleFixture(true,true,BlobPolicies.defaults(),false,true)) {
            m.runtime.install(RuntimeModule.of().withRequiredCapabilities(BlobCommandService.CAPABILITY)); m.runtime.validateCapabilities();
            var actor=m.context(); actor.putAttribute("principal","alice");
            BlobCommandService service=actor.capability(BlobCommandService.class);
            assertSame(m.module.runtime().commands().orElseThrow(),service);
            assertEquals(0,m.f.scalar("SELECT count(*) FROM pg_tables WHERE schemaname=current_schema() AND tablename='teaql_blob_command'"));
            assertEquals(BLOB_SCHEMA_MISSING,assertThrows(BlobException.class,m.runtime::validateReadiness).code());
            new PostgresBlobCommandJournal(m.f.sql).migrate(); m.runtime.validateReadiness();
            BlobRef key=m.upload(); var asset=m.f.newAsset(42,key);
            assertFalse(service.execute(actor,BlobCommandIT.request("typed"),() -> { m.runtime.saveGraph(actor,asset); return BlobCommandIT.revisions(asset); }).replayed());
            m.f.assertOracle(42,key); assertTrue(service.execute(actor,BlobCommandIT.request("typed"),BlobCommandIT.never()).replayed());
        }
        try (ModuleFixture m=new ModuleFixture(true,true,BlobPolicies.defaults())) {
            assertTrue(m.module.runtime().commands().isEmpty()); assertNull(m.context().capability(BlobCommandService.class)); m.runtime.validateReadiness();
            m.runtime.install(RuntimeModule.of().withRequiredCapabilities(BlobCommandService.CAPABILITY));
            assertTrue(assertThrows(TeaQLRuntimeException.class,m.runtime::validateCapabilities).getMessage().contains("BLOB_CAPABILITY_MISSING"));
        }
    }

    @Test public void assemblyIsPassiveAndReadinessNeverRepairsMissingInfrastructure() throws Exception {
        try (ModuleFixture m=new ModuleFixture(false,false,BlobPolicies.defaults())) {
            assertFalse(Files.exists(m.root.resolve("files"))); assertEquals(0,m.f.scalar("SELECT count(*) FROM information_schema.tables WHERE table_schema=current_schema() AND table_name='teaql_blob_schema'"));
            m.runtime.validateCapabilities(); assertEquals(0,m.store.signCalls.get());
            assertEquals(BLOB_SCHEMA_MISSING,assertThrows(BlobException.class,m.runtime::validateReadiness).code());
            assertFalse(Files.exists(m.root.resolve("files"))); assertEquals(0,m.f.scalar("SELECT count(*) FROM information_schema.tables WHERE table_schema=current_schema() AND table_name='teaql_blob_schema'"));
            m.f.catalog.migrate(); assertEquals(BLOB_CAPABILITY_MISSING,assertThrows(BlobException.class,m.runtime::validateReadiness).code());
            m.files.initialize(); assertEquals(BLOB_CAPABILITY_MISSING,assertThrows(BlobException.class,m.runtime::validateReadiness).code());
            m.gateway.start(); m.runtime.validateReadiness();
            m.f.sql.update("UPDATE teaql_blob_schema SET schema_version=2",new Object[0]);
            assertEquals(BLOB_SCHEMA_MISSING,assertThrows(BlobException.class,m.runtime::validateReadiness).code()); assertEquals(2,m.f.scalar("SELECT schema_version FROM teaql_blob_schema"));
        }
    }

    @Test public void fullTypedConsumerUploadsBindsReadsClearsAndCollectsWithoutFramework() throws Exception {
        for (boolean manifestFirst:List.of(false,true)) try (ModuleFixture m=new ModuleFixture(true,true,BlobPolicies.defaults(),manifestFirst)) {
            m.runtime.validateReadiness(); DefaultUserContext actor=m.context();
            assertSame(m.module.runtime(),actor.capability(BlobRuntime.class)); assertSame(m.module.runtime().mutation(),actor.capability(BlobMutationParticipant.class));
            assertSame(m.module.runtime(),m.context().capability(BlobRuntime.class));
            BlobRef key=m.upload(); var asset=m.f.newAsset(42,key); m.runtime.saveGraph(actor,asset); m.f.assertOracle(42,key);
            Read read=m.read(key); assertArrayEquals(file(),download(read.url())); assertEquals("application/pdf",read.contentType());
            assertEquals(NOW.plusSeconds(600),read.expiresAt()); assertEquals("private, no-store",read.cachePolicy()); assertEquals(key,asset.document);
            assertTrue(asset.getEntityMutationLedger().currentChangeSet().isEmpty()); assertFalse(read.toString().contains("token="));
            asset.updateProperty("document",null); m.runtime.saveGraph(actor,asset); m.f.assertOracle(42,null);
            assertEquals(BLOB_READ_DENIED,assertThrows(BlobException.class,() -> m.read(key)).code());
            m.f.clock.now=NOW.plus(Duration.ofDays(7)).plusSeconds(1); actor.putAttribute("maintenance",Boolean.TRUE);
            assertEquals(BlobRuntime.Maintenance.Status.DELETED,m.module.runtime().maintenance().runBatch(actor).outcomes().get(0).status());
            assertEquals(BlobState.DELETED,m.f.find(key).state());
        }
    }

    @Test public void duplicateInstallAndUnsupportedOrDifferentPhysicalResourceRejectBeforeIo() throws Exception {
        try (ModuleFixture m=new ModuleFixture(true,true,BlobPolicies.defaults())) {
            var builder=TeaQLRuntime.builder().metadata(m.f.runtime.getMetadata()); m.module.installInto(builder);
            assertThrows(IllegalStateException.class,() -> m.module.installInto(builder)); assertThrows(IllegalStateException.class,() -> builder.blobMutationParticipant(m.f.participant));
            var partial=TeaQLRuntime.builder().metadata(m.f.runtime.getMetadata()).blobMutationParticipant(m.f.participant);
            assertThrows(IllegalStateException.class,() -> m.module.installInto(partial));
            assertEquals(BLOB_CAPABILITY_MISSING,assertThrows(BlobException.class,m.f.runtime::validateReadiness).code());
            for (boolean sameResource:List.of(false,true)) {
                var executor=new SqlDataServiceExecutor("default",sameResource?m.f.sql:m.f.otherSql) {
                    @Override public boolean supportsTransactionCompletion() { return !sameResource; }
                };
                try (TeaQLRuntime wrong=m.module.installInto(TeaQLRuntime.builder().metadata(m.f.runtime.getMetadata()).dataService("default",executor).telemetry(RuntimeTelemetry.NOOP)).build()) {
                    wrong.install(RuntimeModule.of().withRequiredCapabilities("blob.v1"));
                    assertEquals(BLOB_TRANSACTION_REQUIRED,assertThrows(BlobException.class,wrong::validateCapabilities).code());
                }
            }
            assertEquals(0,m.store.signCalls.get());
        }
    }

    @Test public void currentScopeHostFieldAndAdditionalPolicyAreAllRequiredAndEmptyGraphDoesNoIo() throws Exception {
        try (ModuleFixture m=new ModuleFixture(true,true,BlobPolicies.defaults())) {
            BlobRef key=m.bound(); var request=request(key); long before=m.f.blobReadQueries.get();
            assertTrue(m.module.runtime().reads().readBatch(m.context(),List.of(),Options.defaults()).isEmpty()); assertEquals(before,m.f.blobReadQueries.get());
            for (String denial:List.of("field","policy","scope")) {
                var actor=m.context(); actor.putAttribute(denial,denial.equals("scope")?new BlobScope("test","tenant-b"):Boolean.FALSE);
                assertEquals(BLOB_READ_DENIED,assertThrows(BlobException.class,() -> m.module.runtime().reads().readBatch(actor,List.of(request),Options.defaults())).code());
            }
            assertEquals(0,m.store.signCalls.get());
            var hidden=m.context(); hidden.putAttribute("field",Boolean.FALSE); before=m.f.blobReadQueries.get();
            assertThrows(BlobException.class,() -> m.module.runtime().reads().readBatch(hidden,List.of(request),Options.defaults())); assertEquals(before,m.f.blobReadQueries.get());
        }
    }

    @Test public void batchChecksBindingsAndDeduplicatesOnlyEquivalentTransportRequests() throws Exception {
        try (ModuleFixture m=new ModuleFixture(true,true,BlobPolicies.defaults())) {
            BlobRef key=m.bound(); m.runtime.saveGraph(m.context(),m.f.newAsset(43,key));
            var requests=List.of(request(key),new Request(new BlobFieldTarget("Asset",43L,"document"),key),request(key));
            long before=m.f.blobReadQueries.get(); var reads=m.module.runtime().reads().readBatch(m.context(),requests,Options.defaults());
            assertEquals(3,reads.size()); assertEquals(1,m.store.signedRequests); assertEquals(2,m.f.blobReadQueries.get()-before); assertEquals(reads.get(0).url(),reads.get(1).url());
            assertThrows(UnsupportedOperationException.class,reads::clear);
            int calls=m.store.signCalls.get();
            for (Request wrong:List.of(request(m.f.upload()),new Request(new BlobFieldTarget("Asset",99L,"document"),key))) {
                assertEquals(BLOB_READ_DENIED,assertThrows(BlobException.class,() -> m.module.runtime().reads().readBatch(m.context(),List.of(wrong),Options.defaults())).code());
            }
            assertEquals(calls,m.store.signCalls.get());
        }
    }

    @Test public void revocationUnbindingAndScopeChangesDuringSigningNeverReturnCredentials() throws Exception {
        for (String change:List.of("field","policy","scope","binding")) try (ModuleFixture m=new ModuleFixture(true,true,BlobPolicies.defaults())) {
            BlobRef key=m.bound(); var actor=m.context();
            m.store.afterSign=() -> {
                if (change.equals("binding")) { var asset=m.f.load(42); asset.updateProperty("document",null); m.runtime.saveGraph(m.context(),asset); }
                else actor.putAttribute(change,change.equals("scope")?new BlobScope("test","tenant-b"):Boolean.FALSE);
            };
            assertEquals(BLOB_READ_DENIED,assertThrows(BlobException.class,() -> m.module.runtime().reads().readBatch(actor,List.of(request(key)),Options.defaults())).code());
            assertEquals(1,m.store.signCalls.get());
        }
    }

    @Test public void policyDocumentAndProviderExpiryCapRealUrlAndPrivateCache() throws Exception {
        BlobPolicies policies=new BlobPolicies(BlobPolicies.Limits.defaults(),BlobPolicyProfile.named("default").readUrlTtl(Duration.ofSeconds(60)).cachePolicy("private, max-age=60").build(),Map.of(),Map.of());
        try (ModuleFixture m=new ModuleFixture(true,true,policies)) {
            BlobRef key=m.bound(); Read read=m.module.runtime().reads().readBatch(m.context(),List.of(request(key)),new Options(Duration.ofSeconds(40),NOW.plusSeconds(20),false)).get(0);
            assertEquals(NOW.plusSeconds(20),read.expiresAt()); assertEquals("private, max-age=20",read.cachePolicy()); assertArrayEquals(file(),download(read.url()));
            m.f.clock.now=NOW.plusSeconds(20); assertThrows(IOException.class,() -> download(read.url()));
            assertEquals(BLOB_REFERENCE_EXPIRED,assertThrows(BlobException.class,() -> m.module.runtime().reads().readBatch(m.context(),List.of(request(key)),new Options(null,NOW.plusSeconds(20),false))).code());
            assertEquals(BLOB_CAPABILITY_MISSING,assertThrows(BlobException.class,() -> m.module.runtime().reads().readBatch(m.context(),List.of(request(key)),new Options(null,null,true))).code());
        }
    }

    @Test public void signingFailureExpiryAndOversizeBatchNeverMutateBusinessState() throws Exception {
        for (String mode:List.of("failure","missing","expired","beyond-document")) try (ModuleFixture m=new ModuleFixture(true,true,BlobPolicies.defaults())) {
            BlobRef key=m.bound(); m.store.mode=mode;
            assertEquals(BLOB_SIGN_FAILED,assertThrows(BlobException.class,() -> m.module.runtime().reads().readBatch(m.context(),List.of(request(key)),new Options(null,NOW.plusSeconds(30),false))).code());
            m.f.assertOracle(42,key); assertEquals(1,m.f.scalar("SELECT count(*) FROM asset_data"));
            assertEquals(BLOB_POLICY_INVALID,assertThrows(BlobException.class,() -> m.module.runtime().reads().readBatch(m.context(),Collections.nCopies(257,request(key)),Options.defaults())).code());
        }
    }

    @Test public void readsAndInfrastructureProbesRejectAnUnfinishedPhysicalOwner() throws Exception {
        try (ModuleFixture m=new ModuleFixture(true,true,BlobPolicies.defaults())) {
            BlobRef key=m.bound(); m.f.sql.executeInTransaction(() -> {
                m.runtime.validateCapabilities();
                assertEquals(BLOB_TRANSACTION_REQUIRED,assertThrows(BlobException.class,m.runtime::validateReadiness).code());
                assertEquals(BLOB_TRANSACTION_REQUIRED,assertThrows(BlobException.class,() -> m.read(key)).code());
            }); assertEquals(0,m.store.signCalls.get());
        }
    }

    @Test public void readinessRejectsMalformedMetadataUnknownProfilesAndUnsupportedDeploymentModes() throws Exception {
        try (ModuleFixture m=new ModuleFixture(true,true,BlobPolicies.defaults())) {
            var property=m.runtime.getMetadata().resolveEntityDescriptor("Asset").getProperties().stream().filter(value -> value.getName().equals("document")).findFirst().orElseThrow();
            for (BlobFieldSpec invalid:List.of(BlobFieldSpec.of("Different","document",List.of("pdf"),true,"default"),BlobFieldSpec.of("Asset","document",List.of("pdf"),true,"missing"))) {
                property.withBlobFieldSpec(invalid); assertEquals(BLOB_POLICY_INVALID,assertThrows(BlobException.class,m.runtime::validateCapabilities).code());
            }
            property.withBlobFieldSpec(m.f.spec); m.store.maximumBytes=1;
            assertEquals(BLOB_CAPABILITY_MISSING,assertThrows(BlobException.class,m.runtime::validateCapabilities).code()); assertEquals(0,m.store.signCalls.get());
        }
        var direct=new BlobPolicies(BlobPolicies.Limits.defaults(),BlobPolicyProfile.named("default").uploadMode(BlobPolicyProfile.UploadMode.DIRECT).build(),Map.of(),Map.of());
        try (ModuleFixture m=new ModuleFixture(true,true,direct)) {
            assertEquals(BLOB_CAPABILITY_MISSING,assertThrows(BlobException.class,m.runtime::validateCapabilities).code()); assertEquals(0,m.store.signCalls.get());
        }
    }

    private static Request request(BlobRef key) { return new Request(new BlobFieldTarget("Asset",42L,"document"),key); }
    private static byte[] file() throws IOException { return Files.readAllBytes(Path.of(Objects.requireNonNull(System.getenv("TEAQL_BLOB_TEST_FIXTURES"))).resolve("sample.pdf")); }
    private static byte[] download(URI url) throws IOException {
        var connection=(HttpURLConnection)url.toURL().openConnection(); connection.setConnectTimeout(5000); connection.setReadTimeout(5000);
        try (var bytes=connection.getInputStream()) { return bytes.readAllBytes(); } finally { connection.disconnect(); }
    }
    private final class ModuleFixture implements AutoCloseable {
        final Fixture f; final Path root; final FileBlobStore files; final FileBlobHttpGateway gateway;
        final CountingStore store; final BlobReferenceModule module; final TeaQLRuntime runtime;
        ModuleFixture(boolean schema,boolean storage,BlobPolicies policies) throws Exception { this(schema,storage,policies,false); }
        ModuleFixture(boolean schema,boolean storage,BlobPolicies policies,boolean manifestFirst) throws Exception {
            this(schema,storage,policies,manifestFirst,false);
        }
        ModuleFixture(boolean schema,boolean storage,BlobPolicies policies,boolean manifestFirst,boolean commands) throws Exception {
            f=new Fixture(schema,policies); root=temporary.newFolder().toPath(); files=new FileBlobStore(root.resolve("files"));
            gateway=new FileBlobHttpGateway(files,new InetSocketAddress("127.0.0.1",0),URI.create("http://127.0.0.1:0/blob-read"),"test",Map.of("test",new byte[32]),f.clock,Duration.ofHours(1));
            try {
                store=new CountingStore(gateway);
                var scopes=(BlobBindingAuthorization.ScopeProvider)context -> context.getAttribute("scope",BlobScope.class);
                var moduleBuilder=BlobReferenceModule.builder().catalog(f.catalog).store(store).policies(policies).clock(f.clock)
                        .verifier(new TikaBlobContentVerifier(root.resolve("verify"),TikaBlobContentVerifier.Limits.defaults()))
                        .uploadAuthorization(new BlobUploadAuthorization(scopes,(context,target) -> !Boolean.FALSE.equals(context.getAttribute("field")),(context,target,action,info) -> !Boolean.FALSE.equals(context.getAttribute("policy"))))
                        .bindingAuthorization(new BlobBindingAuthorization(scopes,(context,target,kind) -> !Boolean.FALSE.equals(context.getAttribute("field")),(context,target,blob) -> !Boolean.FALSE.equals(context.getAttribute("policy"))))
                        .readAuthorization(new BlobReadAuthorization(scopes,(context,target) -> !Boolean.FALSE.equals(context.getAttribute("field")),(context,target,blob) -> !Boolean.FALSE.equals(context.getAttribute("policy"))))
                        .maintenancePermission((context,scope) -> Boolean.TRUE.equals(context.getAttribute("maintenance")));
                if (commands) moduleBuilder.commands(new PostgresBlobCommandJournal(f.sql),BlobCommandIT.authorization());
                module=moduleBuilder.build();
                var metadata=new io.teaql.core.meta.SimpleEntityMetaFactory();
                RuntimeModule business=RuntimeModule.of(target -> f.runtime.getMetadata().allEntityDescriptors().forEach(target::register)).withRequiredCapabilities("blob.v1");
                if (manifestFirst) business.install(metadata);
                var builder=TeaQLRuntime.builder().metadata(metadata).registry(f.runtime.getRegistry()).telemetry(RuntimeTelemetry.NOOP).executionLogging(false);
                runtime=module.installInto(builder).build();
                runtime.install(manifestFirst?RuntimeModule.of().withRequiredCapabilities("blob.v1"):business);
                if (storage) { files.initialize(); gateway.start(); }
            } catch (Exception | Error failure) { gateway.close(); f.close(); throw failure; }
        }
        DefaultUserContext context() { var actor=new DefaultUserContext(runtime); actor.putAttribute("scope",SCOPE); return actor; }
        BlobRef upload() throws IOException {
            var service=context().capability(BlobRuntime.class).upload(); var session=service.begin(context(),new BlobFieldTarget("Asset",null,"document"),new BlobRuntime.Uploads.FileInfo("application.pdf",null,null),UUID.randomUUID().toString());
            service.write(context(),session.uploadId(),new ByteArrayInputStream(file())); return service.complete(context(),session.uploadId());
        }
        BlobRef bound() throws IOException { BlobRef key=upload(); runtime.saveGraph(context(),f.newAsset(42,key)); return key; }
        Read read(BlobRef key) { return module.runtime().reads().readBatch(context(),List.of(request(key)),Options.defaults()).get(0); }
        @Override public void close() throws Exception { try { gateway.close(); runtime.close(); } finally { f.close(); } }
    }
    private static final class CountingStore extends BlobStoreDelegate {
        final AtomicInteger signCalls=new AtomicInteger(); int signedRequests; long maximumBytes=Long.MAX_VALUE; String mode=""; Runnable afterSign=() -> {};
        CountingStore(BlobStore delegate) { super(delegate); }
        @Override public long maximumWriteBytes() { return Math.min(maximumBytes,super.maximumWriteBytes()); }
        @Override public List<SignedRead> signReadBatch(UserContext context,List<ReadRequest> requests) {
            signCalls.incrementAndGet(); signedRequests=requests.size();
            if (mode.equals("failure")) throw new IllegalStateException("test transport unavailable");
            var result=super.signReadBatch(context,requests); afterSign.run();
            if (mode.equals("missing")) return List.of();
            if (mode.equals("expired")) return List.of(new SignedRead(result.get(0).url(),NOW));
            if (mode.equals("beyond-document")) return List.of(new SignedRead(result.get(0).url(),NOW.plusSeconds(31)));
            return result;
        }
    }
}
