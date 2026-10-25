package io.teaql.core.view;

import io.teaql.core.blob.BlobRuntime;
import java.math.*;
import java.util.*;

/** Immutable, value-only response tree. It never retains mutable entities, context or lazy loaders. */
public final class AuthorizedResponseGraph {
    public static final int MAX_NODES=10000,MAX_DEPTH=64,MAX_TEXT_BYTES=8388608;
    public sealed interface Node permits Scalar,ObjectNode,ArrayNode,EntityNode,BlobNode {}
    public record Scalar(Object value) implements Node {
        public Scalar {
            if (value instanceof BigInteger integer) value=new BigInteger(integer.toByteArray());
            else if (value instanceof BigDecimal decimal) value=new BigDecimal(new BigInteger(decimal.unscaledValue().toByteArray()),decimal.scale());
            if (value!=null && !(value instanceof String || value instanceof Boolean || value instanceof Byte || value instanceof Short || value instanceof Integer
                    || value instanceof Long || value instanceof BigInteger || value instanceof BigDecimal || value instanceof Float || value instanceof Double)
                    || value instanceof Float f && !Float.isFinite(f) || value instanceof Double d && !Double.isFinite(d)) throw new IllegalArgumentException("Unsupported view scalar");
        }
        @Override public String toString() { return "ViewScalar[redacted]"; }
    }
    public record ObjectNode(Map<String,Node> fields) implements Node {
        public ObjectNode { fields=AuthorizedResponseGraph.fields(fields); }
        @Override public String toString() { return "ViewObject[redacted]"; }
    }
    public record ArrayNode(List<Node> items) implements Node {
        public ArrayNode { items=List.copyOf(items); }
        @Override public String toString() { return "ViewArray[redacted]"; }
    }
    public record Field(String canonicalName,ViewAuthorization.FieldDecision decision) {
        public Field { Objects.requireNonNull(canonicalName); Objects.requireNonNull(decision); }
    }
    public record EntityNode(ViewAuthorization.EntityTarget target,Map<String,Node> fields,Map<String,Field> policy) implements Node {
        public EntityNode {
            Objects.requireNonNull(target); fields=AuthorizedResponseGraph.fields(fields); policy=Map.copyOf(policy);
            if (!fields.keySet().equals(policy.keySet())) throw new IllegalArgumentException("Incomplete view policy surface");
        }
        @Override public String toString() { return "ViewEntity[redacted]"; }
    }
    public record BlobNode(BlobRuntime.Reads.Request request) implements Node {
        public BlobNode { Objects.requireNonNull(request); }
        @Override public String toString() { return "ViewBlob[redacted]"; }
    }
    private final Node root;
    public AuthorizedResponseGraph(Node root) { this.root=Objects.requireNonNull(root); }
    public Node root() { return root; }
    public List<BlobRuntime.Reads.Request> blobs() { List<BlobRuntime.Reads.Request> result=new ArrayList<>(); collect(root,result); return List.copyOf(result); }
    private static void collect(Node node,List<BlobRuntime.Reads.Request> result) {
        if (node instanceof BlobNode blob) result.add(blob.request());
        else if (node instanceof EntityNode entity) entity.fields().values().forEach(value -> collect(value,result));
        else if (node instanceof ObjectNode object) object.fields().values().forEach(value -> collect(value,result));
        else if (node instanceof ArrayNode array) array.items().forEach(value -> collect(value,result));
    }
    private static Map<String,Node> fields(Map<String,Node> source) {
        LinkedHashMap<String,Node> result=new LinkedHashMap<>();
        source.forEach((key,value) -> result.put(Objects.requireNonNull(key),Objects.requireNonNull(value)));
        return Collections.unmodifiableMap(result);
    }
    @Override public String toString() { return "AuthorizedResponseGraph[redacted]"; }
}
