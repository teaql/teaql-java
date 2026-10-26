package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.blob.*;
import io.teaql.core.meta.*;
import io.teaql.core.reference.*;
import io.teaql.core.view.*;
import io.teaql.core.view.AuthorizedResponseGraph.*;
import io.teaql.core.view.ViewAuthorization.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static io.teaql.core.view.ViewPreparationException.Code.*;

/** Framework-independent, value-only presentation. No serializer or business-save callback is invoked. */
public final class DefaultViewPreparationService implements ViewPreparationService {
    public static final int MAX_NODES=AuthorizedResponseGraph.MAX_NODES,MAX_DEPTH=AuthorizedResponseGraph.MAX_DEPTH,MAX_TEXT_BYTES=AuthorizedResponseGraph.MAX_TEXT_BYTES;
    private final TeaQLRuntime runtime;
    private final ViewAuthorization authorization;
    private final ViewDocumentContributor documents;
    private final ViewBlobContributor blobs;
    private final Clock clock;
    private final Object issuer=new Object();
    public DefaultViewPreparationService(TeaQLRuntime runtime,ViewAuthorization authorization,ViewDocumentContributor documents,ViewBlobContributor blobs,Clock clock) {
        this.runtime=Objects.requireNonNull(runtime); this.authorization=authorization; this.documents=documents; this.blobs=blobs; this.clock=Objects.requireNonNull(clock);
    }
    @Override public DocumentView openDocumentForView(UserContext context,ContextBoundDocumentService.OpenRequest request) {
        documentReady(context);
        var principal=context.trustedReferencePrincipal();
        var identity=authorization.identity(context);
        var document=context.openDocument(request);
        return captureDocument(context,document,request.aggregateType(),request.purpose(),
                principal,identity,ViewOptions.BusinessCompletion.QUERY);
    }
    @Override public AcceptedDocumentView acceptDocumentForView(UserContext context,ContextBoundDocumentService.AcceptRequest request) {
        documentReady(context);
        var principal=context.trustedReferencePrincipal();
        var identity=authorization.identity(context);
        var accepted=context.acceptDocument(request);
        DocumentView document=captureDocument(context,accepted.document(),request.aggregateType(),request.purpose(),
                principal,identity,ViewOptions.BusinessCompletion.COMMITTED);
        return new AcceptedDocumentView(document,accepted.previousRevision(),accepted.newRevision(),accepted.changedEntities());
    }
    private void documentReady(UserContext context) {
        ViewOptions options=documentOptions(ViewOptions.BusinessCompletion.QUERY);
        try {
            if (authorization==null || context==null || context.capability(ViewPreparationService.class)!=this
                    || context.capability(EntityMetaFactory.class)!=runtime.getMetadata()
                    || !context.contextBoundDocumentService().supportsViewPreparation()
                    || context.roundTripReferences().mode()!=ReferenceMode.GOVERNED) throw error(VIEW_CAPABILITY_MISSING,options);
            requireCompleted(options);
            context.trustedReferencePrincipal();
            Objects.requireNonNull(authorization.identity(context));
        } catch (ViewPreparationException known) { throw known; }
        catch (RoundTripReferenceException unavailable) { throw documentError(unavailable,options); }
        catch (RuntimeException failed) { throw error(VIEW_PREPARATION_FAILED,options); }
    }
    private DocumentView captureDocument(UserContext context,ContextBoundDocumentService.ContextualDocument document,
            String aggregate,String purpose,TrustedReferencePrincipal principal,Identity identity,
            ViewOptions.BusinessCompletion completion) {
        ViewOptions options=documentOptions(completion);
        try {
            requireCompleted(options);
            requireSame(context,identity,principal,options);
            DocumentView source;
            if (document.documentToken().startsWith("tqd2.")) {
                VerifiedBoundDocumentSnapshot verified=context.consumeBoundDocumentSnapshot(
                        document.documentToken(),aggregate,purpose);
                if (!verified.document().snapshot().businessId().equals(document.businessId()))
                    throw error(VIEW_ACCESS_DENIED,options);
                source=DocumentView.issueBound(issuer,document,verified,principal,
                        context.contextBoundDocumentService(),context.roundTripReferences(),completion);
            } else {
                VerifiedDocumentSnapshot verified=context.consumeDocumentSnapshot(
                        document.documentToken(),aggregate,purpose);
                if (!verified.snapshot().businessId().equals(document.businessId()))
                    throw error(VIEW_ACCESS_DENIED,options);
                source=DocumentView.issue(issuer,document,verified,principal,
                        context.contextBoundDocumentService(),context.roundTripReferences(),completion);
            }
            source.validateCurrent(issuer,context);
            requireSame(context,identity,principal,options);
            return source;
        } catch (ViewPreparationException known) { throw known; }
        catch (RoundTripReferenceException invalid) { throw documentError(invalid,options); }
        catch (RuntimeException failed) { throw error(VIEW_PREPARATION_FAILED,options); }
    }
    private static ViewOptions documentOptions(ViewOptions.BusinessCompletion completion) {
        return new ViewOptions(ViewOptions.Mode.GOVERNED,completion,null,Duration.ZERO,false);
    }
    private static ViewPreparationException documentError(RoundTripReferenceException failed,ViewOptions options) {
        return switch (failed.getCode()) {
            case EXPIRED -> error(VIEW_EXPIRED,options);
            case PROVIDER_NOT_CONFIGURED, CONFIGURATION_INVALID -> error(VIEW_CAPABILITY_MISSING,options);
            case AUTHORIZATION_REQUIRED, CONTEXT_MISMATCH, CONTEXT_BINDING_REQUIRED, SCOPE_MISMATCH -> error(VIEW_ACCESS_DENIED,options);
            default -> error(VIEW_PREPARATION_FAILED,options);
        };
    }
    @Override public PreparedView prepare(UserContext context,Object result,ViewOptions options) {
        Objects.requireNonNull(options);
        try {
            if (authorization==null || context==null) throw error(VIEW_CAPABILITY_MISSING,options);
            if (context.capability(EntityMetaFactory.class)!=runtime.getMetadata()) throw error(VIEW_ACCESS_DENIED,options);
            requireCompleted(options); Identity identity=Objects.requireNonNull(authorization.identity(context));
            DocumentView source=result instanceof DocumentView view?view:null;
            TrustedReferencePrincipal principal=options.mode()==ViewOptions.Mode.GOVERNED?context.trustedReferencePrincipal():null;
            if (result instanceof PreparedView prepared) {
                if (!prepared.matches(issuer,options,identity)) throw error(VIEW_PREPARED_MISMATCH,options);
                if (options.mode()==ViewOptions.Mode.GOVERNED && documents==null
                        && prepared.documentSource(issuer).isEmpty()) throw error(VIEW_CAPABILITY_MISSING,options);
                if (prepared.expiresAt().isPresent() && !clock.instant().isBefore(prepared.expiresAt().orElseThrow())) throw error(VIEW_EXPIRED,options);
                if (prepared.documentContext().isPresent() && !prepared.documentContext().orElseThrow().principal().equals(principal)) throw error(VIEW_ACCESS_DENIED,options);
                recheck(context,prepared.graph(issuer).root(),"",options); requireSame(context,identity,principal,options);
                if (prepared.documentContext().isPresent()) validateDocumentCurrent(context,prepared,options);
                prepared.validateBlobs(issuer,context,options);
                recheck(context,prepared.graph(issuer).root(),"",options); requireSame(context,identity,principal,options); requireCompleted(options);
                if (prepared.expiresAt().isPresent() && !clock.instant().isBefore(prepared.expiresAt().orElseThrow())) throw error(VIEW_EXPIRED,options);
                return prepared;
            }
            if (options.mode()==ViewOptions.Mode.GOVERNED && documents==null && source==null) throw error(VIEW_CAPABILITY_MISSING,options);
            if (source!=null) {
                if (options.mode()!=ViewOptions.Mode.GOVERNED || source.businessCompletion()!=options.businessCompletion()) throw error(VIEW_PREPARED_MISMATCH,options);
                if (!source.matches(issuer)) throw error(VIEW_PREPARED_MISMATCH,options);
                source.validateCurrent(issuer,context);
                result=source.document(issuer).body();
            }
            var frozen=new Freeze(context,options); Node root=frozen.node(result,"",0);
            if (root==null) throw error(VIEW_ACCESS_DENIED,options);
            AuthorizedResponseGraph graph=new AuthorizedResponseGraph(root);
            requireSame(context,identity,principal,options); recheck(context,root,"",options);
            DocumentContext document=null; Map<String,String> credentials=Map.of();
            if (options.mode()==ViewOptions.Mode.GOVERNED) {
                if (source!=null) {
                    document=source.context(issuer); credentials=source.credentials(issuer);
                } else {
                    var prepared=Objects.requireNonNull(documents.prepare(context,graph,options));
                    if (!surface(root,prepared.graph().root()) || !principal.equals(prepared.context().principal())) throw error(VIEW_ACCESS_DENIED,options);
                    document=prepared.context(); credentials=prepared.credentials(); graph=prepared.graph();
                }
                if (credentials.size()>32) throw error(VIEW_LIMIT_EXCEEDED,options);
                for (var entry:credentials.entrySet()) { frozen.text(entry.getKey()); frozen.text(entry.getValue()); }
                if (!clock.instant().isBefore(document.expiresAt())) throw error(VIEW_EXPIRED,options);
                requireSame(context,identity,principal,options); recheck(context,root,"",options); requireCompleted(options);
            }
            List<BlobRuntime.Reads.Request> requests=graph.blobs(); List<BlobRuntime.Reads.Read> reads=List.of(); ViewBlobContributor.Projection projection=null;
            if (!requests.isEmpty()) {
                if (blobs==null) throw error(VIEW_CAPABILITY_MISSING,options);
                projection=Objects.requireNonNull(blobs.projectBatch(context,graph,options,Optional.ofNullable(document))); reads=projection.reads();
                if (reads.size()!=requests.size()) throw error(VIEW_PREPARATION_FAILED,options);
            }
            Instant expiresAt=document==null?null:document.expiresAt();
            long cacheSeconds=options.cacheLifetime().getSeconds(); Instant now=clock.instant();
            for (int index=0;index<reads.size();index++) {
                var read=reads.get(index);
                if (!read.reference().equals(requests.get(index).reference()) || !now.isBefore(read.expiresAt())
                        || document!=null && read.expiresAt().isAfter(document.expiresAt())) throw error(VIEW_PREPARATION_FAILED,options);
                expiresAt=min(expiresAt,read.expiresAt()); cacheSeconds=Math.min(cacheSeconds,cacheSeconds(read.cachePolicy(),options));
            }
            if (expiresAt!=null) cacheSeconds=Math.min(cacheSeconds,Math.max(0,Duration.between(now,expiresAt).getSeconds()));
            String cache=cacheSeconds==0?"private, no-store":"private, max-age="+cacheSeconds;
            if (cacheSeconds>0) expiresAt=min(expiresAt,now.plusSeconds(cacheSeconds));
            Budget rendered=new Budget(options); for (var entry:credentials.entrySet()) { rendered.text(entry.getKey()); rendered.text(entry.getValue()); }
            Object value=render(graph.root(),reads.iterator(),rendered,0);
            if (document!=null) {
                if (source!=null) source.validateCurrent(issuer,context);
                else documents.validateCurrent(context,new AuthorizedResponseGraph(root),document,options);
            }
            recheck(context,root,"",options); requireSame(context,identity,principal,options); requireCompleted(options);
            if (expiresAt!=null && !clock.instant().isBefore(expiresAt)) throw error(VIEW_EXPIRED,options);
            return PreparedView.issue(issuer,value,options,identity,new AuthorizedResponseGraph(root),document,credentials,expiresAt,cache,projection,source);
        } catch (ViewPreparationException known) { throw error(known.code(),options); }
        catch (RoundTripReferenceException invalid) { throw documentError(invalid,options); }
        catch (RuntimeException failed) { throw error(VIEW_PREPARATION_FAILED,options); }
    }
    private void validateDocumentCurrent(UserContext context,PreparedView prepared,ViewOptions options) {
        var source=prepared.documentSource(issuer);
        if (source.isPresent()) source.orElseThrow().validateCurrent(issuer,context);
        else documents.validateCurrent(context,prepared.graph(issuer),prepared.documentContext().orElseThrow(),options);
    }
    private void requireSame(UserContext context,Identity identity,TrustedReferencePrincipal principal,ViewOptions options) {
        if (!identity.equals(authorization.identity(context)) || principal!=null && !principal.equals(context.trustedReferencePrincipal())) throw error(VIEW_ACCESS_DENIED,options);
    }
    private void requireCompleted(ViewOptions options) {
        try { BlobCommandScope.requireAbsent(); } catch (BlobException pending) { throw error(VIEW_TRANSACTION_UNFINISHED,options); }
        Set<String> routes=new HashSet<>(); routes.add("default");
        for (var descriptor:runtime.getMetadata().allEntityDescriptors()) {
            String route=descriptor.getDataService(); routes.add(route==null || route.isEmpty()?"default":route);
        }
        for (String route:routes) if (runtime.getRegistry().resolveTransactionExecutor(route).map(value -> value.activeTransactionIdentity()!=null).orElse(false)) throw error(VIEW_TRANSACTION_UNFINISHED,options);
    }
    private void recheck(UserContext context,Node node,String path,ViewOptions options) {
        if (node instanceof EntityNode entity) {
            if (!authorization.canReadEntity(context,entity.target(),path)) throw error(VIEW_ACCESS_DENIED,options);
            for (var entry:entity.fields().entrySet()) {
                Field field=entity.policy().get(entry.getKey()); String child=path(path,entry.getKey());
                if (!field.decision().equals(authorization.field(context,entity.target(),field.canonicalName(),child,options.mode()))) throw error(VIEW_ACCESS_DENIED,options);
                recheck(context,entry.getValue(),child,options);
            }
        } else if (node instanceof ObjectNode object) object.fields().forEach((key,value) -> recheck(context,value,path(path,key),options));
        else if (node instanceof ArrayNode array) for (int index=0;index<array.items().size();index++) recheck(context,array.items().get(index),path(path,Integer.toString(index)),options);
    }
    private final class Freeze extends Budget {
        final UserContext context;
        final IdentityHashMap<Object,Boolean> active=new IdentityHashMap<>();
        Freeze(UserContext context,ViewOptions options) { super(options); this.context=context; }
        Node node(Object value,String path,int depth) {
            visit(depth);
            if (value instanceof JsonNode json) return json(json,path,depth);
            if (value instanceof BaseEntity entity) return entity(entity,path,depth);
            if (value instanceof Entity || value instanceof BlobRef || value instanceof PreparedView) throw error(VIEW_RESULT_UNSUPPORTED,options);
            if (value instanceof Map<?,?> map) {
                enter(value); try {
                    Map<String,Node> result=new LinkedHashMap<>();
                    for (var entry:map.entrySet()) {
                        if (!(entry.getKey() instanceof String key)) throw error(VIEW_RESULT_UNSUPPORTED,options);
                        text(key); Node child=node(entry.getValue(),path(path,key),depth+1); if (child!=null) result.put(key,child);
                    }
                    return new ObjectNode(result);
                } finally { active.remove(value); }
            }
            if (value instanceof List<?> || value instanceof SmartList<?> || value instanceof Object[]) {
                enter(value); try {
                    List<Node> result=new ArrayList<>();
                    Iterable<?> items=value instanceof Object[] array?Arrays.asList(array):(Iterable<?>)value;
                    for (Object item:items) {
                        Node child=node(item,path(path,Integer.toString(result.size())),depth+1); if (child!=null) result.add(child);
                    }
                    return new ArrayNode(result);
                } finally { active.remove(value); }
            }
            if (value instanceof String s) text(s);
            if (value instanceof BigInteger || value instanceof BigDecimal) text(value.toString());
            if (value instanceof UUID || value instanceof Instant || value instanceof LocalDate || value instanceof LocalDateTime || value instanceof LocalTime
                    || value instanceof OffsetDateTime || value instanceof OffsetTime || value instanceof ZonedDateTime || value instanceof Duration || value instanceof Year || value instanceof YearMonth || value instanceof MonthDay) {
                value=value.toString(); text((String)value);
            } else if (value instanceof Enum<?> enumeration) { value=enumeration.name(); text((String)value); }
            try { return new Scalar(value); } catch (IllegalArgumentException unsupported) { throw error(VIEW_RESULT_UNSUPPORTED,options); }
        }
        Node json(JsonNode json,String path,int depth) {
            if (json.isObject()) {
                enter(json); try {
                    Map<String,Node> result=new LinkedHashMap<>(); var fields=json.fields();
                    while (fields.hasNext()) { var entry=fields.next(); text(entry.getKey()); result.put(entry.getKey(),node(entry.getValue(),path(path,entry.getKey()),depth+1)); }
                    return new ObjectNode(result);
                } finally { active.remove(json); }
            }
            if (json.isArray()) {
                enter(json); try {
                    List<Node> result=new ArrayList<>(); for (var item:json) result.add(node(item,path(path,Integer.toString(result.size())),depth+1)); return new ArrayNode(result);
                } finally { active.remove(json); }
            }
            if (json.isNull()) return new Scalar(null);
            if (json.isTextual()) { text(json.textValue()); return new Scalar(json.textValue()); }
            if (json.isBoolean()) return new Scalar(json.booleanValue());
            if (json.isIntegralNumber()) { Object number=json.canConvertToLong()?json.longValue():json.bigIntegerValue(); text(number.toString()); return new Scalar(number); }
            if (json.isFloatingPointNumber()) {
                if ((json.isDouble() || json.isFloat()) && !Double.isFinite(json.doubleValue())) throw error(VIEW_RESULT_UNSUPPORTED,options);
                var number=json.decimalValue(); text(number.toString()); return new Scalar(number);
            }
            throw error(VIEW_RESULT_UNSUPPORTED,options);
        }
        void enter(Object value) { if (active.put(value,Boolean.TRUE)!=null) throw error(VIEW_RESULT_UNSUPPORTED,options); }
        Node entity(BaseEntity entity,String path,int depth) {
            if (entity.__internalHasMutationLedger() && (entity.getEntityMutationLedger().hasUncommittedChanges() || entity.getEntityMutationLedger().hasUnknownTransactionOutcome())) throw error(VIEW_TRANSACTION_UNFINISHED,options);
            EntityTarget target=new EntityTarget(entity.typeName(),entity.getId(),entity.getVersion());
            if (!authorization.canReadEntity(context,target,path)) throw error(VIEW_ACCESS_DENIED,options);
            var descriptor=runtime.getMetadata().resolveEntityDescriptor(entity.typeName());
            if (descriptor==null) throw error(VIEW_RESULT_UNSUPPORTED,options);
            boolean cycle=active.containsKey(entity); if (!cycle) active.put(entity,Boolean.TRUE);
            try {
                Map<String,PropertyDescriptor> properties=new LinkedHashMap<>();
                for (var current=descriptor;current!=null;current=current.getParent()) for (var property:current.getProperties()) properties.putIfAbsent(property.getName(),property);
                Map<String,Node> values=new LinkedHashMap<>(); Map<String,Field> policy=new LinkedHashMap<>(); Set<String> declared=new HashSet<>();
                var layout=entity.__internalLoadState().layout();
                for (var property:properties.values()) {
                    String canonical=property.getName(),member=layout.memberName(canonical); if (member==null) member=canonical;
                    declared.add(canonical); declared.add(member);
                    if (cycle && !property.isId() && !property.isVersion() || !entity.isPropertyLoaded(canonical)) continue;
                    String child=path(path,member); FieldDecision decision=Objects.requireNonNull(authorization.field(context,target,canonical,child,options.mode()));
                    if (decision.projection()==Projection.HIDDEN) continue;
                    Node projected;
                    if (decision.projection()==Projection.MASK) projected=node(decision.mask(),child,depth+1);
                    else {
                        Object raw=entity.__internalGet(member);
                        if (raw!=null && property.blobFieldSpec()!=null) {
                            if (!(raw instanceof BlobRef ref) || target.id()==null) throw error(VIEW_RESULT_UNSUPPORTED,options);
                            projected=new BlobNode(new BlobRuntime.Reads.Request(new BlobFieldTarget(target.entity(),target.id(),canonical),ref)); visit(depth+1);
                        } else projected=node(raw,child,depth+1);
                    }
                    if (projected!=null) add(values,policy,member,canonical,decision,projected);
                }
                if (!cycle) for (var entry:entity.__internalAdditionalInfoSnapshot().entrySet()) {
                    String name=entry.getKey(); if (declared.contains(name)) continue;
                    String child=path(path,name); FieldDecision decision=Objects.requireNonNull(authorization.field(context,target,name,child,options.mode()));
                    if (decision.projection()==Projection.HIDDEN) continue;
                    Node projected=node(decision.projection()==Projection.MASK?decision.mask():entry.getValue(),child,depth+1);
                    if (projected!=null) add(values,policy,name,name,decision,projected);
                }
                return new EntityNode(target,values,policy);
            } finally { if (!cycle) active.remove(entity); }
        }
        void add(Map<String,Node> values,Map<String,Field> policy,String member,String canonical,FieldDecision decision,Node value) {
            text(member); if (values.putIfAbsent(member,value)!=null) throw error(VIEW_RESULT_UNSUPPORTED,options); policy.put(member,new Field(canonical,decision));
        }
    }
    private static class Budget {
        final ViewOptions options; int nodes; long bytes;
        Budget(ViewOptions options) { this.options=options; }
        void visit(int depth) { if (++nodes>MAX_NODES || depth>MAX_DEPTH) throw error(VIEW_LIMIT_EXCEEDED,options); }
        void text(String value) { bytes+=value.getBytes(StandardCharsets.UTF_8).length; if (bytes>MAX_TEXT_BYTES) throw error(VIEW_LIMIT_EXCEEDED,options); }
    }
    private static Object render(Node node,Iterator<BlobRuntime.Reads.Read> reads,Budget budget,int depth) {
        budget.visit(depth);
        if (node instanceof Scalar scalar) {
            if (scalar.value() instanceof String || scalar.value() instanceof BigInteger || scalar.value() instanceof BigDecimal) budget.text(scalar.value().toString()); return scalar.value();
        }
        if (node instanceof BlobNode) {
            var read=reads.next();
            if (read.responseShape()==BlobPolicyProfile.ResponseShape.URL) { budget.text(read.url().toString()); return read.url().toString(); }
            Map<String,Object> result=new LinkedHashMap<>(); result.put("url",read.url().toString()); result.put("expiresAt",read.expiresAt().toString());
            result.put("fileName",read.originalName()); result.put("contentType",read.contentType()); result.put("sizeBytes",read.sizeBytes());
            for (var entry:result.entrySet()) { budget.visit(depth+1); budget.text(entry.getKey()); if (entry.getValue() instanceof String text) budget.text(text); }
            return Collections.unmodifiableMap(result);
        }
        if (node instanceof ArrayNode array) { List<Object> result=new ArrayList<>(); for (var child:array.items()) result.add(render(child,reads,budget,depth+1)); return Collections.unmodifiableList(result); }
        Map<String,Node> fields=node instanceof EntityNode entity?entity.fields():((ObjectNode)node).fields(); Map<String,Object> result=new LinkedHashMap<>();
        fields.forEach((key,value) -> { budget.text(key); result.put(key,render(value,reads,budget,depth+1)); }); return Collections.unmodifiableMap(result);
    }
    private static boolean surface(Node before,Node after) {
        if (before instanceof Scalar) return after instanceof Scalar;
        if (before instanceof BlobNode blob) return blob.equals(after);
        if (before instanceof ArrayNode array) {
            if (!(after instanceof ArrayNode other) || array.items().size()!=other.items().size()) return false;
            for (int index=0;index<array.items().size();index++) if (!surface(array.items().get(index),other.items().get(index))) return false; return true;
        }
        Map<String,Node> left,right;
        if (before instanceof EntityNode entity) {
            if (!(after instanceof EntityNode other) || !entity.target().equals(other.target()) || !entity.policy().equals(other.policy())) return false;
            left=entity.fields(); right=other.fields();
        } else { if (!(after instanceof ObjectNode other)) return false; left=((ObjectNode)before).fields(); right=other.fields(); }
        if (!left.keySet().equals(right.keySet())) return false;
        for (String key:left.keySet()) if (!surface(left.get(key),right.get(key))) return false; return true;
    }
    private static long cacheSeconds(String policy,ViewOptions options) {
        if ("private, no-store".equals(policy)) return 0;
        if (!policy.matches("private, max-age=[0-9]+")) throw error(VIEW_PREPARATION_FAILED,options);
        try { return Long.parseLong(policy.substring("private, max-age=".length())); } catch (NumberFormatException invalid) { throw error(VIEW_PREPARATION_FAILED,options); }
    }
    private static Instant min(Instant left,Instant right) { return left==null || right.isBefore(left)?right:left; }
    private static String path(String parent,String member) { return parent+"/"+member.replace("~","~0").replace("/","~1"); }
    private static ViewPreparationException error(ViewPreparationException.Code code,ViewOptions options) { return new ViewPreparationException(code,options.businessCompletion()); }
}
