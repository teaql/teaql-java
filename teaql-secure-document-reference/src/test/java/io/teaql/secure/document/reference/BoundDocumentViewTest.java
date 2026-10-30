package io.teaql.secure.document.reference;

import io.teaql.core.UserContext;
import io.teaql.core.blob.BlobRef;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.reference.BoundDocumentSnapshot;
import io.teaql.core.reference.ContextBoundDocumentService;
import io.teaql.core.reference.DocumentBlobBindingSnapshot;
import io.teaql.core.reference.DocumentSnapshot;
import io.teaql.core.reference.InternalEntityIdentity;
import io.teaql.core.reference.ReferenceDocumentScope;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import io.teaql.core.reference.TrustedReferencePrincipal;
import io.teaql.core.reference.VerifiedBoundDocumentSnapshot;
import io.teaql.core.view.ViewAuthorization;
import io.teaql.core.view.ViewOptions;
import io.teaql.core.view.ViewPreparationException;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.RuntimeTelemetry;
import io.teaql.runtime.TeaQLRuntime;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

/** A previously issued tqd2 keeps its binding authority during governed view reuse. */
public class BoundDocumentViewTest {
    private static final BlobRef OLD=BlobRef.of("blob_old");
    private static final ViewOptions OPTIONS=new ViewOptions(ViewOptions.Mode.GOVERNED,
            ViewOptions.BusinessCompletion.QUERY,null,Duration.ZERO,false);

    private static BoundDocumentSnapshot document() {
        var scope=new ReferenceDocumentScope("order-10","edit-order","Order",10,1);
        var item=new DocumentSnapshot.Entity("item:0",new InternalEntityIdentity("OrderItem",20,1),
                Set.of("name","attachment"),Set.of("attachment"),
                Set.of(DocumentSnapshot.MutationKind.UPDATE));
        var snapshot=new DocumentSnapshot("order-model-v1","ORD-10",scope,
                new InternalEntityIdentity("Order",10,1),List.of(item),
                List.of(new DocumentSnapshot.Relation("items",List.of("item:0"),
                        DocumentSnapshot.Completeness.COMPLETE,true,
                        Set.of(DocumentSnapshot.MutationKind.REMOVE_CHILD))));
        return new BoundDocumentSnapshot(snapshot,List.of(new DocumentBlobBindingSnapshot(
                "item:0","attachment","OrderItem",OLD,1,true,true)));
    }

    private static final class Service implements ContextBoundDocumentService {
        final AtomicBoolean allowed=new AtomicBoolean(true);
        final AtomicReference<BlobRef> current=new AtomicReference<>(OLD);
        final AtomicLong version=new AtomicLong(1);
        final AtomicBoolean revokeAfterAccept=new AtomicBoolean(false);
        final BoundDocumentSnapshot issued=document();
        @Override public ContextualDocument openDocument(UserContext context,
                TrustedReferencePrincipal principal,OpenRequest request) {
            return new ContextualDocument("ORD-10",
                    context.issueBoundDocumentSnapshot(issued,request.lifetime()),
                    Map.of("items",List.of(Map.of("ref","item:0","name","sample"))));
        }
        @Override public AcceptedDocument acceptDocument(UserContext context,
                TrustedReferencePrincipal principal,AcceptRequest request) {
            var returned=openDocument(context,principal,new OpenRequest(
                    "Order","ORD-10","edit-order",Duration.ofMinutes(5)));
            if (revokeAfterAccept.get()) allowed.set(false);
            return new AcceptedDocument(returned,1,1,List.of());
        }
        @Override public boolean supportsViewPreparation() { return true; }
        @Override public void validatePreparedBoundDocument(UserContext context,
                TrustedReferencePrincipal principal,VerifiedBoundDocumentSnapshot verified) {
            var binding=verified.document().bindings().get(0);
            if (!allowed.get() || !principal.equals(context.trustedReferencePrincipal())
                    || !issued.equals(verified.document())
                    || !current.get().equals(binding.reference())
                    || version.get()!=binding.bindingVersion()) {
                throw new RoundTripReferenceException(
                        RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,"AUTHORIZATION_REQUIRED");
            }
        }
    }

    private static TeaQLRuntime runtime() {
        byte[] key=new byte[32]; Arrays.fill(key,(byte)0x41);
        var provider=AeadRoundTripReferenceProvider.governed(DeploymentProfile.TEST,"bound-view","test",
                new StaticRoundTripReferenceKeyProvider(new RoundTripReferenceKey("k1",key)),
                (context,principal,scope,identity) -> {});
        ViewAuthorization authorization=new ViewAuthorization() {
            @Override public Identity identity(UserContext context) {
                var principal=context.trustedReferencePrincipal();
                return new Identity(principal.authenticationRealm(),principal.subject(),
                        principal.domainRootType()+":"+principal.domainRootId());
            }
            @Override public boolean canReadEntity(UserContext context,EntityTarget target,String path) {
                return true;
            }
            @Override public FieldDecision field(UserContext context,EntityTarget target,
                    String canonicalField,String path,ViewOptions.Mode mode) {
                return FieldDecision.show();
            }
        };
        return SecureDocumentReferenceModule.of(provider).installInto(TeaQLRuntime.builder()
                .metadata(new SimpleEntityMetaFactory()).viewAuthorization(authorization)
                .telemetry(RuntimeTelemetry.NOOP).executionLogging(false)).build();
    }

    private static DefaultUserContext context(TeaQLRuntime runtime,String actor,
            ContextBoundDocumentService service) {
        var context=new DefaultUserContext(runtime);
        context.withTrustedReferencePrincipal(new TrustedReferencePrincipal("oidc",actor,"Merchant",7));
        context.withContextBoundDocumentService(service);
        return context;
    }

    @Test public void issuedBoundDocumentCanBePreparedAndReusedUntilAuthorityChanges() {
        try (TeaQLRuntime runtime=runtime()) {
            var service=new Service(); var alice=context(runtime,"alice",service);
            var source=alice.openDocumentForView(new ContextBoundDocumentService.OpenRequest(
                    "Order","ORD-10","edit-order",Duration.ofMinutes(5)));
            var prepared=runtime.prepareView(alice,source,OPTIONS);
            assertEquals(Map.of("items",List.of(Map.of("ref","item:0","name","sample"))),prepared.value());
            assertEquals(document().snapshot().scope(),prepared.documentContext().orElseThrow().scope());
            assertSame(prepared,runtime.prepareView(alice,prepared,OPTIONS));
            assertEquals(ViewPreparationException.Code.VIEW_PREPARED_MISMATCH,
                    assertThrows(ViewPreparationException.class,() -> runtime.prepareView(
                            context(runtime,"bob",service),prepared,OPTIONS)).code());
            assertEquals(ViewPreparationException.Code.VIEW_ACCESS_DENIED,
                    assertThrows(ViewPreparationException.class,() -> runtime.prepareView(
                            context(runtime,"bob",service),source,OPTIONS)).code());
            service.current.set(BlobRef.of("blob_replaced")); service.version.incrementAndGet();
            assertEquals(ViewPreparationException.Code.VIEW_ACCESS_DENIED,
                    assertThrows(ViewPreparationException.class,
                            () -> runtime.prepareView(alice,prepared,OPTIONS)).code());
            service.current.set(OLD); service.version.set(1); service.allowed.set(false);
            assertEquals(ViewPreparationException.Code.VIEW_ACCESS_DENIED,
                    assertThrows(ViewPreparationException.class,
                            () -> runtime.prepareView(alice,prepared,OPTIONS)).code());
            service.allowed.set(true); alice.withContextBoundDocumentService(new Service());
            assertEquals(ViewPreparationException.Code.VIEW_ACCESS_DENIED,
                    assertThrows(ViewPreparationException.class,
                            () -> runtime.prepareView(alice,prepared,OPTIONS)).code());
        }
    }

    @Test public void boundDocumentRequiresExplicitServiceValidation() {
        try (TeaQLRuntime runtime=runtime()) {
            var delegate=new Service();
            ContextBoundDocumentService service=new ContextBoundDocumentService() {
                @Override public ContextualDocument openDocument(UserContext context,
                        TrustedReferencePrincipal principal,OpenRequest request) {
                    return delegate.openDocument(context,principal,request);
                }
                @Override public AcceptedDocument acceptDocument(UserContext context,
                        TrustedReferencePrincipal principal,AcceptRequest request) {
                    return delegate.acceptDocument(context,principal,request);
                }
                @Override public boolean supportsViewPreparation() { return true; }
            };
            var alice=context(runtime,"alice",service);
            assertEquals(ViewPreparationException.Code.VIEW_CAPABILITY_MISSING,
                    assertThrows(ViewPreparationException.class,() -> alice.openDocumentForView(
                            new ContextBoundDocumentService.OpenRequest("Order","ORD-10","edit-order",
                                    Duration.ofMinutes(5)))).code());
        }
    }

    @Test public void acceptedBoundDocumentKeepsCommittedPresentationSemantics() {
        try (TeaQLRuntime runtime=runtime()) {
            var service=new Service(); var alice=context(runtime,"alice",service);
            var submitted=new ContextBoundDocumentService.SubmittedDocument("ORD-10",
                    "submitted-token","command-1",Map.of("items",List.of()));
            var request=new ContextBoundDocumentService.AcceptRequest("Order","edit-order",submitted);
            var accepted=alice.acceptDocumentForView(request);
            var options=new ViewOptions(ViewOptions.Mode.GOVERNED,
                    ViewOptions.BusinessCompletion.COMMITTED,null,Duration.ZERO,false);
            var prepared=runtime.prepareView(alice,accepted.document(),options);
            assertSame(prepared,runtime.prepareView(alice,prepared,options));
            service.revokeAfterAccept.set(true);
            var failed=assertThrows(ViewPreparationException.class,
                    () -> alice.acceptDocumentForView(request));
            assertEquals(ViewPreparationException.Code.VIEW_ACCESS_DENIED,failed.code());
            assertTrue(failed.businessCommitted());
        }
    }
}
