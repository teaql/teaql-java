package io.teaql.core.view;

import io.teaql.core.FrameworkInternal;
import java.time.Instant;
import java.util.*;

/** One immutable presentation. Render value(), never the internal graph or mutable business result. */
public final class PreparedView {
    private final Object issuer,value;
    private final ViewOptions options;
    private final ViewAuthorization.Identity identity;
    private final AuthorizedResponseGraph graph;
    private final DocumentContext document;
    private final Map<String,String> credentials;
    private final Instant expiresAt;
    private final String cachePolicy;
    private final ViewBlobContributor.Projection blobs;
    private PreparedView(Object issuer,Object value,ViewOptions options,ViewAuthorization.Identity identity,AuthorizedResponseGraph graph,
                         DocumentContext document,Map<String,String> credentials,Instant expiresAt,String cachePolicy,ViewBlobContributor.Projection blobs) {
        this.issuer=Objects.requireNonNull(issuer); this.value=value; this.options=Objects.requireNonNull(options); this.identity=Objects.requireNonNull(identity);
        this.graph=Objects.requireNonNull(graph); this.document=document; this.credentials=Map.copyOf(credentials); this.expiresAt=expiresAt; this.cachePolicy=Objects.requireNonNull(cachePolicy);
        this.blobs=blobs;
    }
    @FrameworkInternal("Only an installed service can supply its private presentation issuer")
    public static PreparedView issue(Object issuer,Object value,ViewOptions options,ViewAuthorization.Identity identity,AuthorizedResponseGraph graph,
                                     DocumentContext document,Map<String,String> credentials,Instant expiresAt,String cachePolicy,ViewBlobContributor.Projection blobs) {
        return new PreparedView(issuer,value,options,identity,graph,document,credentials,expiresAt,cachePolicy,blobs);
    }
    @FrameworkInternal("Presentation-local reuse; JSON flags never establish this provenance")
    public boolean matches(Object issuer,ViewOptions options,ViewAuthorization.Identity identity) { return this.issuer==issuer && this.options==options && this.identity.equals(identity); }
    @FrameworkInternal("Recheck only this installed service's frozen authorization surface")
    public AuthorizedResponseGraph graph(Object issuer) { if (this.issuer!=issuer) throw new IllegalArgumentException("Foreign presentation issuer"); return graph; }
    @FrameworkInternal("Recheck only this installed service's private Blob presentation state")
    public void validateBlobs(Object issuer,io.teaql.core.UserContext context,ViewOptions options) {
        if (this.issuer!=issuer) throw new IllegalArgumentException("Foreign presentation issuer");
        if (blobs!=null) blobs.validation().requireCurrent(context,options,Optional.ofNullable(document));
    }
    public Object value() { return value; }
    public Optional<DocumentContext> documentContext() { return Optional.ofNullable(document); }
    public Map<String,String> documentCredentials() { return credentials; }
    public Optional<Instant> expiresAt() { return Optional.ofNullable(expiresAt); }
    public String cachePolicy() { return cachePolicy; }
    /** Apply the absolute validity bound at actual delivery time, including nested-renderer delays. */
    public String cachePolicyAt(Instant observedAt) {
        Objects.requireNonNull(observedAt);
        if (cachePolicy.equals("private, no-store") || expiresAt==null) return cachePolicy;
        long seconds=Math.min(Long.parseLong(cachePolicy.substring("private, max-age=".length())),Math.max(0,java.time.Duration.between(observedAt,expiresAt).getSeconds()));
        return seconds==0?"private, no-store":"private, max-age="+seconds;
    }
    public ViewOptions.BusinessCompletion businessCompletion() { return options.businessCompletion(); }
    @Override public String toString() { return "PreparedView[redacted]"; }
}
