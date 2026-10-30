package io.teaql.blob.reference;

import io.teaql.core.blob.BlobRef;
import static io.teaql.blob.reference.PostgresBlobCatalogIT.*;

/** Owned child JVM halts at physical commit. Secrets are inherited only through the test environment. */
public final class BlobCommandCrashProbe {
    public static void main(String[] arguments) {
        try (var harness=new BlobCommandFaultHarness(System.getenv("TEAQL_BLOB_COMMAND_CRASH_SCHEMA"))) {
            var asset=new Asset(); asset.__internalInitializeNewEntityId(42L); asset.updateProperty("title","asset");
            asset.updateProperty("document",BlobRef.of(arguments[0])); asset.setComment("owned command crash boundary");
            harness.fault.set(BlobCommandFaultHarness.Fault.valueOf(arguments[1]));
            harness.commands.execute(harness.actor,BlobCommandIT.request("crash"),() -> {
                harness.runtime.saveGraph(harness.actor,asset); return BlobCommandIT.revisions(asset);
            });
        }
        throw new IllegalStateException("Owned commit crash injection was not reached");
    }
}
