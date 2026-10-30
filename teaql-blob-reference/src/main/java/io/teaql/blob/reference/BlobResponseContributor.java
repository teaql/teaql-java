package io.teaql.blob.reference;

import io.teaql.core.UserContext;
import io.teaql.core.blob.BlobRuntime;
import io.teaql.core.view.*;
import java.util.*;

/** Projects only Blob fields in the frozen, authorized graph through current target-bound reads. */
public final class BlobResponseContributor implements ViewBlobContributor {
    private final BlobReadService reads;
    public BlobResponseContributor(BlobReadService reads) { this.reads=Objects.requireNonNull(reads); }
    @Override public Projection projectBatch(UserContext context,AuthorizedResponseGraph graph,ViewOptions options,Optional<DocumentContext> document) {
        var requests=graph.blobs(); if (requests.isEmpty()) return new Projection(List.of(),(current,mode,scope) -> {});
        var readOptions=new BlobRuntime.Reads.Options(options.readLifetime(),document.map(DocumentContext::expiresAt).orElse(null),options.requireContextCheckedRead());
        return reads.readGraph(context,graph,readOptions);
    }
}
