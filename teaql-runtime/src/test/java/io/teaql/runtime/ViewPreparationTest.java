package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.blob.*;
import io.teaql.core.meta.*;
import io.teaql.core.reference.*;
import io.teaql.core.view.*;
import io.teaql.core.view.AuthorizedResponseGraph.*;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static io.teaql.core.view.ViewPreparationException.Code.*;
import static org.junit.Assert.*;

public class ViewPreparationTest {
    static final Instant NOW=Instant.parse("2026-10-25T00:00:00Z");
    static final TrustedReferencePrincipal PRINCIPAL=new TrustedReferencePrincipal("test","alice","Tenant",1);
    static final ReferenceDocumentScope SCOPE=new ReferenceDocumentScope("doc1","edit","ViewAsset",42,1);
    static final ViewAuthorization POLICY=new ViewAuthorization() {
        @Override public Identity identity(UserContext context) { return new Identity("test",Objects.toString(context.getAttribute("subject"),"alice"),Objects.toString(context.getAttribute("scope"),"tenant-a")); }
        @Override public boolean canReadEntity(UserContext context,EntityTarget target,String path) { return !Boolean.FALSE.equals(context.getAttribute("entity")); }
        @Override public FieldDecision field(UserContext context,EntityTarget target,String field,String path,ViewOptions.Mode mode) {
            if (Set.of("hidden").contains(field) || field.equals(context.getAttribute("hide"))) return FieldDecision.hide();
            return field.equals(context.getAttribute("mask"))?FieldDecision.mask("***"):FieldDecision.show();
        }
    };
    static SimpleEntityMetaFactory metadata() {
        var metadata=new SimpleEntityMetaFactory(); var descriptor=new EntityDescriptor(); descriptor.setType("ViewAsset"); descriptor.setTargetType(Asset.class); descriptor.setEntitySupplier(Asset::new);
        descriptor.addSimpleProperty("id",Long.class); descriptor.addSimpleProperty("version",Long.class);
        for (String field:List.of("title","hidden","unloaded")) descriptor.addSimpleProperty(field,String.class);
        descriptor.addSimpleProperty("document",BlobRef.class).withBlobFieldSpec(BlobFieldSpec.of("ViewAsset","document",List.of("pdf"),true,"default"));
        descriptor.addSimpleProperty("child",Asset.class); descriptor.addSimpleProperty("json",Object.class); metadata.register(descriptor); return metadata;
    }
    static TeaQLRuntime.Builder builder() { return TeaQLRuntime.builder().metadata(metadata()).viewAuthorization(POLICY).viewClock(Clock.fixed(NOW,ZoneOffset.UTC)).telemetry(RuntimeTelemetry.NOOP).executionLogging(false); }
    static Asset asset() { Asset asset=new Asset(); asset.__internalSet("id",42L); asset.__internalSet("version",1L); asset.__internalSet("title","original"); asset.__internalSet("hidden","secret"); return asset; }
    static Map<?,?> object(Object value) { return (Map<?,?>)value; }
    static ViewBlobContributor.Projection reads(AuthorizedResponseGraph graph,Instant expiry) {
        return projection(graph.blobs().stream().map(request -> new BlobRuntime.Reads.Read(request.reference(),URI.create("https://files.example/complete-url"),expiry,"sample.pdf","application/pdf",100,"private, max-age=300",BlobPolicyProfile.ResponseShape.URL)).toList());
    }
    static ViewBlobContributor.Projection projection(List<BlobRuntime.Reads.Read> reads) {
        return new ViewBlobContributor.Projection(reads,(context,options,document) -> {
            Object count=context.getAttribute("blobValidations"); if (count instanceof AtomicInteger calls) calls.incrementAndGet();
            if (Boolean.FALSE.equals(context.getAttribute("blobAllowed"))) throw new ViewPreparationException(VIEW_ACCESS_DENIED,options.businessCompletion());
        });
    }

    @Test public void freezesLoadedAuthorizedFieldsAndRelationsWithoutTouchingEntitiesOrLedgers() {
        try (var runtime=builder().build()) {
            var actor=new DefaultUserContext(runtime); var asset=asset(); asset.__internalSet("child",asset); asset.addDynamicProperty("label","result");
            assertFalse(asset.__internalHasMutationLedger()); var state=asset.__internalLoadState();
            var prepared=runtime.prepareView(actor,Map.of("data",List.of(asset),"prepared",true),ViewOptions.readOnly());
            Map<?,?> value=object(((List<?>)object(prepared.value()).get("data")).get(0));
            assertEquals("original",value.get("title")); assertFalse(value.containsKey("hidden")); assertFalse(value.containsKey("unloaded"));
            assertEquals(Map.of("id",42L,"version",1L),value.get("child")); assertEquals("result",value.get("_label"));
            assertEquals("secret",asset.values.get("hidden")); assertSame(state,asset.__internalLoadState()); assertFalse(asset.__internalHasMutationLedger());
            assertThrows(UnsupportedOperationException.class,() -> ((Map<Object,Object>)prepared.value()).clear());
            asset.__internalSet("title","later"); assertEquals("original",value.get("title"));
        }
    }

    @Test public void hiddenAndMaskedBlobFieldsAreNeverReadOrSigned() {
        AtomicInteger signed=new AtomicInteger();
        try (var runtime=builder().viewBlobs((context,graph,options,document) -> { signed.incrementAndGet(); return reads(graph,NOW.plusSeconds(120)); }).build()) {
            var actor=new DefaultUserContext(runtime); var asset=asset(); asset.__internalSet("document",BlobRef.of("blob_fixture"));
            actor.putAttribute("hide","document"); assertFalse(object(runtime.prepareView(actor,asset,ViewOptions.readOnly()).value()).containsKey("document"));
            actor.putAttribute("hide","none"); actor.putAttribute("mask","document"); assertEquals("***",object(runtime.prepareView(actor,asset,ViewOptions.readOnly()).value()).get("document"));
            assertEquals(0,signed.get()); assertEquals(0,asset.blobReads);
        }
    }

    @Test public void outputIsFrozenBeforeSignerAndPlainUrlsOnlyAppearInTheCopy() {
        var asset=asset(); asset.__internalSet("document",BlobRef.of("blob_fixture"));
        try (var runtime=builder().viewBlobs((context,graph,options,document) -> { asset.__internalSet("title","late signer edit"); return reads(graph,NOW.plusSeconds(120)); }).build()) {
            var prepared=runtime.prepareView(new DefaultUserContext(runtime),asset,ViewOptions.readOnly());
            assertEquals("original",object(prepared.value()).get("title")); assertEquals("https://files.example/complete-url",object(prepared.value()).get("document"));
            assertEquals(BlobRef.of("blob_fixture"),asset.values.get("document")); assertFalse(asset.__internalHasMutationLedger());
        }
    }

    @Test public void currentScopeActorEntityAndFieldRevocationDuringSigningDenyWholeResult() {
        for (String change:List.of("scope","subject","entity","hide")) try (var runtime=builder().viewBlobs((context,graph,options,document) -> {
            context.putAttribute(change,change.equals("entity")?Boolean.FALSE:change.equals("hide")?"title":"different"); return reads(graph,NOW.plusSeconds(120));
        }).build()) {
            var asset=asset(); asset.__internalSet("document",BlobRef.of("blob_fixture"));
            assertEquals(VIEW_ACCESS_DENIED,assertThrows(ViewPreparationException.class,() -> runtime.prepareView(new DefaultUserContext(runtime),asset,ViewOptions.readOnly())).code());
        }
    }

    @Test public void onlySamePresentationProvenanceCanReuseAndEveryReuseRechecksPolicy() {
        AtomicInteger signed=new AtomicInteger();
        try (var runtime=builder().viewBlobs((context,graph,options,document) -> { signed.incrementAndGet(); return reads(graph,NOW.plusSeconds(120)); }).build()) {
            var actor=new DefaultUserContext(runtime); var asset=asset(); asset.__internalSet("document",BlobRef.of("blob_fixture")); var options=ViewOptions.readOnly();
            AtomicInteger validations=new AtomicInteger(); actor.putAttribute("blobValidations",validations);
            var prepared=runtime.prepareView(actor,asset,options); assertSame(prepared,actor.capability(ViewPreparationService.class).prepare(actor,prepared,options)); assertEquals(1,signed.get());
            assertEquals(VIEW_PREPARED_MISMATCH,assertThrows(ViewPreparationException.class,() -> runtime.prepareView(actor,prepared,ViewOptions.readOnly())).code());
            actor.putAttribute("blobAllowed",Boolean.FALSE); assertEquals(VIEW_ACCESS_DENIED,assertThrows(ViewPreparationException.class,() -> runtime.prepareView(actor,prepared,options)).code()); actor.putAttribute("blobAllowed",Boolean.TRUE);
            int before=validations.get(); actor.putAttribute("hide","title"); assertEquals(VIEW_ACCESS_DENIED,assertThrows(ViewPreparationException.class,() -> runtime.prepareView(actor,prepared,options)).code()); assertEquals(before,validations.get());
        }
    }

    @Test public void documentStagePrecedesSigningUsesCurrentPrincipalAndNeverExpandsTheFrozenGraph() {
        List<String> stages=new ArrayList<>();
        try (var runtime=builder().viewDocuments(currentDocument((context,graph,options) -> {
            stages.add("document"); return new ViewDocumentContributor.Preparation(graph,new DocumentContext(context.trustedReferencePrincipal(),SCOPE,NOW.plusSeconds(30)),Map.of("documentToken","server-issued"));
        })).viewBlobs((context,graph,options,document) -> { stages.add("blob"); assertEquals(NOW.plusSeconds(30),document.orElseThrow().expiresAt()); return reads(graph,NOW.plusSeconds(30)); }).build()) {
            var actor=new DefaultUserContext(runtime); actor.withTrustedReferencePrincipal(PRINCIPAL); var asset=asset(); asset.__internalSet("document",BlobRef.of("blob_fixture"));
            var options=new ViewOptions(ViewOptions.Mode.GOVERNED,ViewOptions.BusinessCompletion.QUERY,null,Duration.ofSeconds(300),false);
            var prepared=runtime.prepareView(actor,asset,options); assertEquals(List.of("document","blob"),stages); assertEquals(NOW.plusSeconds(30),prepared.expiresAt().orElseThrow());
            assertEquals("private, max-age=30",prepared.cachePolicy()); assertEquals("server-issued",prepared.documentCredentials().get("documentToken"));
            assertEquals("private, max-age=25",prepared.cachePolicyAt(NOW.plusSeconds(5))); assertEquals("private, no-store",prepared.cachePolicyAt(NOW.plusSeconds(30)));
            assertSame(prepared,runtime.prepareView(actor,prepared,options)); assertEquals(2,stages.size());
            actor.putAttribute("documentAllowed",Boolean.FALSE);
            assertEquals(VIEW_ACCESS_DENIED,assertThrows(ViewPreparationException.class,() -> runtime.prepareView(actor,prepared,options)).code()); assertEquals(2,stages.size());
        }
        try (var runtime=builder().viewDocuments((context,graph,options) -> new ViewDocumentContributor.Preparation(new AuthorizedResponseGraph(new ObjectNode(Map.of("injected",new Scalar("secret")))),new DocumentContext(PRINCIPAL,SCOPE,NOW.plusSeconds(30)),Map.of())).build()) {
            var actor=new DefaultUserContext(runtime); actor.withTrustedReferencePrincipal(PRINCIPAL);
            assertEquals(VIEW_ACCESS_DENIED,assertThrows(ViewPreparationException.class,() -> runtime.prepareView(actor,asset(),new ViewOptions(ViewOptions.Mode.GOVERNED,ViewOptions.BusinessCompletion.QUERY,null,Duration.ZERO,false))).code());
        }
    }

    @Test public void readOnlySkipsConfiguredDocumentStageAndGovernedRechecksDocumentAuthorityAfterSigning() {
        try (var runtime=builder().viewDocuments((context,graph,options) -> { throw new AssertionError("Read-only presentation must not issue documents"); }).build()) {
            assertNotNull(runtime.prepareView(new DefaultUserContext(runtime),asset(),ViewOptions.readOnly()));
        }
        try (var runtime=builder().viewDocuments(currentDocument((context,graph,options) -> new ViewDocumentContributor.Preparation(graph,new DocumentContext(PRINCIPAL,SCOPE,NOW.plusSeconds(30)),Map.of("documentToken","server-issued"))))
                .viewBlobs((context,graph,options,document) -> { context.putAttribute("documentAllowed",Boolean.FALSE); return reads(graph,NOW.plusSeconds(30)); }).build()) {
            var actor=new DefaultUserContext(runtime); actor.withTrustedReferencePrincipal(PRINCIPAL); var asset=asset(); asset.__internalSet("document",BlobRef.of("blob_fixture"));
            assertEquals(VIEW_ACCESS_DENIED,assertThrows(ViewPreparationException.class,() -> runtime.prepareView(actor,asset,new ViewOptions(ViewOptions.Mode.GOVERNED,ViewOptions.BusinessCompletion.QUERY,null,Duration.ZERO,false))).code());
        }
    }

    private static ViewDocumentContributor currentDocument(ViewDocumentContributor delegate) {
        return new ViewDocumentContributor() {
            @Override public Preparation prepare(UserContext context,AuthorizedResponseGraph graph,ViewOptions options) { return delegate.prepare(context,graph,options); }
            @Override public void validateCurrent(UserContext context,AuthorizedResponseGraph graph,DocumentContext document,ViewOptions options) {
                if (Boolean.FALSE.equals(context.getAttribute("documentAllowed"))) throw new ViewPreparationException(VIEW_ACCESS_DENIED,options.businessCompletion());
            }
        };
    }

    @Test public void committedPresentationFailurePreservesOutcomeAndRedactsProviderDetails() {
        try (var runtime=builder().viewBlobs((context,graph,options,document) -> { throw new IllegalStateException("CANARY_SECRET_AND_URL"); }).build()) {
            var asset=asset(); asset.__internalSet("document",BlobRef.of("blob_fixture"));
            var failed=assertThrows(ViewPreparationException.class,() -> runtime.prepareView(new DefaultUserContext(runtime),asset,ViewOptions.afterCommit()));
            assertTrue(failed.businessCommitted()); assertEquals(VIEW_PREPARATION_FAILED,failed.code()); assertNull(failed.getCause()); assertFalse(failed.toString().contains("CANARY"));
        }
    }

    @Test public void rejectsMissingCapabilitiesForeignContextsRawKeysAndClaimedPreparedJson() {
        try (var runtime=builder().build(); var other=builder().build()) {
            var actor=new DefaultUserContext(runtime); var asset=asset(); asset.__internalSet("document",BlobRef.of("blob_fixture"));
            assertEquals(VIEW_CAPABILITY_MISSING,assertThrows(ViewPreparationException.class,() -> runtime.prepareView(actor,asset,ViewOptions.readOnly())).code());
            assertEquals(VIEW_ACCESS_DENIED,assertThrows(ViewPreparationException.class,() -> runtime.prepareView(new DefaultUserContext(other),asset(),ViewOptions.readOnly())).code());
            assertEquals(VIEW_RESULT_UNSUPPORTED,assertThrows(ViewPreparationException.class,() -> runtime.prepareView(actor,Map.of("raw",BlobRef.of("blob_fixture")),ViewOptions.readOnly())).code());
            actor.withTrustedReferencePrincipal(PRINCIPAL);
            assertEquals(VIEW_CAPABILITY_MISSING,assertThrows(ViewPreparationException.class,() -> runtime.prepareView(actor,Map.of("prepared",true),new ViewOptions(ViewOptions.Mode.GOVERNED,ViewOptions.BusinessCompletion.QUERY,null,Duration.ZERO,false))).code());
        }
    }

    @Test public void rejectsUnfinishedCommandBeforeAuthorizationOrSigning() {
        try (var runtime=builder().build(); var scope=BlobCommandScope.open(new Object(),new Object())) {
            assertEquals(VIEW_TRANSACTION_UNFINISHED,assertThrows(ViewPreparationException.class,() -> runtime.prepareView(new DefaultUserContext(runtime),asset(),ViewOptions.readOnly())).code());
        }
    }

    @Test public void copiesJsonNodesAndEnvelopesWithoutReflectivePojoSerialization() throws Exception {
        try (var runtime=builder().build()) {
            var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"nested\":[null,12345678901234567890123,\"text\"]}");
            var asset=asset(); asset.__internalSet("json",json); var prepared=runtime.prepareView(new DefaultUserContext(runtime),asset,ViewOptions.readOnly());
            ((com.fasterxml.jackson.databind.node.ObjectNode)json).removeAll(); assertTrue(object(object(prepared.value()).get("json")).containsKey("nested"));
            assertEquals(VIEW_RESULT_UNSUPPORTED,assertThrows(ViewPreparationException.class,() -> runtime.prepareView(new DefaultUserContext(runtime),new Object(),ViewOptions.readOnly())).code());
        }
    }

    @Test public void rejectsContainerCyclesAndExcessDepthBeforeSigning() {
        try (var runtime=builder().build()) {
            Map<String,Object> cyclic=new HashMap<>(); cyclic.put("self",cyclic);
            assertEquals(VIEW_RESULT_UNSUPPORTED,assertThrows(ViewPreparationException.class,() -> runtime.prepareView(new DefaultUserContext(runtime),cyclic,ViewOptions.readOnly())).code());
            Object nested="end"; for (int i=0;i<66;i++) nested=List.of(nested); Object finalValue=nested;
            assertEquals(VIEW_LIMIT_EXCEEDED,assertThrows(ViewPreparationException.class,() -> runtime.prepareView(new DefaultUserContext(runtime),finalValue,ViewOptions.readOnly())).code());
        }
    }

    @Test public void sharedReadOnlyGoldenCasesMatchLoadedNullMaskAndJsonSemantics() throws Exception {
        var json=new com.fasterxml.jackson.databind.ObjectMapper();
        try (var stream=getClass().getResourceAsStream("/view-preparation-cases.json"); var runtime=builder().build()) {
            var cases=json.readTree(stream).path("cases"); assertEquals(6,cases.size());
            for (var test:cases) {
                var actor=new DefaultUserContext(runtime); if (test.has("mask")) actor.putAttribute("mask",test.path("mask").asText()); var entity=new Asset();
                var fields=test.path("fields").fields();
                while (fields.hasNext()) {
                    var entry=fields.next(); var value=entry.getValue(); Object input;
                    if (value.isNull()) input=null; else if (entry.getKey().equals("id") || entry.getKey().equals("version")) input=value.longValue();
                    else if (entry.getKey().equals("json")) input=value; else input=value.asText(); entity.__internalSet(entry.getKey(),input);
                }
                assertEquals(test.path("id").asText(),test.path("expected"),json.readTree(json.writeValueAsBytes(runtime.prepareView(actor,entity,ViewOptions.readOnly()).value())));
            }
        }
    }

    @Test public void descriptorRenderingOmitsKeysAndExpiredPresentationsRequireFreshPreparation() {
        var time=new Clock() {
            Instant now=NOW;
            @Override public Instant instant() { return now; }
            @Override public ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(ZoneId zone) { return this; }
        };
        try (var runtime=builder().viewClock(time).viewBlobs((context,graph,options,document) -> projection(graph.blobs().stream().map(request -> new BlobRuntime.Reads.Read(
                request.reference(),URI.create("https://files.example/complete-url"),NOW.plusSeconds(1),"sample.pdf","application/pdf",100,"private, max-age=300",BlobPolicyProfile.ResponseShape.DESCRIPTOR)).toList())).build()) {
            var actor=new DefaultUserContext(runtime); var asset=asset(); asset.__internalSet("document",BlobRef.of("blob_fixture")); var options=ViewOptions.readOnly();
            var prepared=runtime.prepareView(actor,asset,options); var descriptor=object(object(prepared.value()).get("document"));
            assertEquals(Set.of("url","expiresAt","fileName","contentType","sizeBytes"),descriptor.keySet()); assertFalse(descriptor.containsValue("blob_fixture"));
            time.now=NOW.plusSeconds(1); assertEquals(VIEW_EXPIRED,assertThrows(ViewPreparationException.class,() -> runtime.prepareView(actor,prepared,options)).code());
        }
    }

    @Test public void invocationLimitsAndTypedArraysKeepTheGraphBounded() {
        try (var runtime=builder().build()) {
            var actor=new DefaultUserContext(runtime);
            assertEquals(List.of("one","two"),runtime.prepareView(actor,new String[]{"one","two"},ViewOptions.readOnly()).value());
            assertEquals(VIEW_LIMIT_EXCEEDED,assertThrows(ViewPreparationException.class,() -> runtime.prepareView(actor,Collections.nCopies(10000,1),ViewOptions.readOnly())).code());
            assertEquals(VIEW_LIMIT_EXCEEDED,assertThrows(ViewPreparationException.class,() -> runtime.prepareView(actor,"x".repeat(8388609),ViewOptions.readOnly())).code());
        }
    }

    public static final class Asset extends BaseEntity {
        final Map<String,Object> values=new HashMap<>(); int blobReads;
        @Override public String typeName() { return "ViewAsset"; }
        @Override public void __internalSet(String field,Object value) { if (field.equals("id") || field.equals("version")) super.__internalSet(field,value); else { values.put(field,value); markPropertyLoaded(field); } }
        @Override public Object __internalGet(String field) {
            if (field.equals("hidden")) throw new AssertionError("Hidden getter must never be read");
            if (field.equals("document")) blobReads++;
            if (field.equals("id") || field.equals("version")) return super.__internalGet(field); return values.get(field);
        }
    }
}
