package io.teaql.blob.reference;

import io.teaql.core.*;
import io.teaql.core.blob.*;
import io.teaql.core.meta.*;
import io.teaql.runtime.TeaQLRuntime;
import java.time.Clock;
import java.util.*;
import static io.teaql.core.blob.BlobException.Code.*;

/** Passive assembly of the optional Blob services. The host owns all resources and scheduling. */
public final class BlobReferenceModule {
    public static final String CAPABILITY=BlobFieldSpec.CAPABILITY;
    private final BlobRuntime services;
    private BlobReferenceModule(Builder builder) {
        PostgresBlobCatalog catalog=Objects.requireNonNull(builder.catalog,"catalog");
        BlobStore store=Objects.requireNonNull(builder.store,"store");
        BlobPolicies policies=builder.policies; Clock clock=builder.clock;
        BlobContentVerifier verifier=Objects.requireNonNull(builder.verifier,"verifier");
        var binding=Objects.requireNonNull(builder.binding,"bindingAuthorization");
        services=new Services(catalog,store,policies,verifier,
                new BlobUploadService(catalog,store,verifier,policies,Objects.requireNonNull(builder.upload,"uploadAuthorization"),clock),
                new PostgresBlobMutationParticipant(catalog,policies,binding,clock),
                new BlobReadService(catalog,store,policies,Objects.requireNonNull(builder.read,"readAuthorization"),clock),
                new BlobMaintenanceService(catalog,store,binding::scope,Objects.requireNonNull(builder.maintenance,"maintenancePermission"),clock,builder.batchSize));
    }
    public static Builder builder() { return new Builder(); }
    public BlobRuntime runtime() { return services; }
    public TeaQLRuntime.Builder installInto(TeaQLRuntime.Builder builder) { return Objects.requireNonNull(builder).blobRuntime(services); }

    private record Services(PostgresBlobCatalog catalog,BlobStore store,BlobPolicies policies,BlobContentVerifier verifier,
                            BlobUploadService upload,PostgresBlobMutationParticipant mutation,
                            BlobReadService reads,BlobMaintenanceService maintenance) implements BlobRuntime {
        @Override public void validateConfiguration(EntityMetaFactory metadata,DataServiceRegistry registry) {
            Objects.requireNonNull(metadata); Objects.requireNonNull(registry);
            BlobStore.Capabilities capabilities=store.capabilities();
            if (!capabilities.signedReads() || !capabilities.range() || !capabilities.versionConditionalDelete()) throw new BlobException(BLOB_CAPABILITY_MISSING);
            List<BlobFieldSpec> model=new ArrayList<>();
            for (EntityDescriptor descriptor:metadata.allEntityDescriptors()) {
                Map<String,BlobFieldSpec> fields=BlobModel.fields(descriptor);
                if (fields.isEmpty()) continue;
                String route=descriptor.getDataService(); if (route==null || route.isEmpty()) route="default";
                MutationExecutor executor=registry.resolveMutationExecutor(route);
                if (!(executor instanceof TransactionExecutor transaction) || !transaction.supportsTransactionCompletion()) throw new BlobException(BLOB_TRANSACTION_REQUIRED);
                catalog.requireResource(transaction.transactionResource());
                for (BlobFieldSpec field:fields.values()) {
                    model.add(field); BlobPolicySnapshot policy=policies.resolve(field);
                    // Direct uploads need a proven remote publisher fencing/maintenance protocol first.
                    if (policy.uploadMode()!=BlobPolicyProfile.UploadMode.SERVER_STREAM || policy.maxSizeBytes()>store.maximumWriteBytes()
                            || !verifier.supportedExtensions().containsAll(policy.field().allowedExtensions())) throw new BlobException(BLOB_CAPABILITY_MISSING);
                }
            }
            policies.validate(model);
        }
        @Override public void validateReadiness() {
            catalog.requireNoActiveTransaction(); catalog.validateSchema(); store.validateReady();
        }
    }

    public static final class Builder {
        private PostgresBlobCatalog catalog;
        private BlobStore store;
        private BlobContentVerifier verifier;
        private BlobPolicies policies=BlobPolicies.defaults();
        private BlobUploadAuthorization upload;
        private BlobBindingAuthorization binding;
        private BlobReadAuthorization read;
        private BlobMaintenanceService.Permission maintenance;
        private Clock clock=Clock.systemUTC();
        private int batchSize=100;
        private Builder() {}
        public Builder catalog(PostgresBlobCatalog value) { catalog=Objects.requireNonNull(value); return this; }
        public Builder store(BlobStore value) { store=Objects.requireNonNull(value); return this; }
        public Builder verifier(BlobContentVerifier value) { verifier=Objects.requireNonNull(value); return this; }
        public Builder policies(BlobPolicies value) { policies=Objects.requireNonNull(value); return this; }
        public Builder uploadAuthorization(BlobUploadAuthorization value) { upload=Objects.requireNonNull(value); return this; }
        public Builder bindingAuthorization(BlobBindingAuthorization value) { binding=Objects.requireNonNull(value); return this; }
        public Builder readAuthorization(BlobReadAuthorization value) { read=Objects.requireNonNull(value); return this; }
        public Builder maintenancePermission(BlobMaintenanceService.Permission value) { maintenance=Objects.requireNonNull(value); return this; }
        public Builder clock(Clock value) { clock=Objects.requireNonNull(value); return this; }
        public Builder maintenanceBatchSize(int value) { if (value<1 || value>1000) throw new BlobException(BLOB_POLICY_INVALID); batchSize=value; return this; }
        public BlobReferenceModule build() { return new BlobReferenceModule(this); }
    }
}
