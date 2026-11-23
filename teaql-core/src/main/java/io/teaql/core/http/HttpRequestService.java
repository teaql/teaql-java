package io.teaql.core.http;

import io.teaql.core.UserContext;
import io.teaql.core.TraceKind;
import io.teaql.core.TraceNode;
import io.teaql.core.QueryRequest;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Context-owned, provider-neutral HTTP boundary for configured external services. */
public interface HttpRequestService {
    /** Sensitive adapters fail closed unless the installed transport attests
     *  to the trusted profile it will actually use. */
    default boolean profileIsEncryptedOrLoopback(String profile) { return false; }

    enum Method { GET, POST, PUT, DELETE }
    enum IntentKind { PURPOSE, AUDIT }
    enum RetryMode { NONE, CONFIRMED_IDEMPOTENT }

    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = Call.BlockedSerializer.class)
    final class Call {
        private static final com.fasterxml.jackson.databind.ObjectMapper TRACE_JSON =
                new com.fasterxml.jackson.databind.ObjectMapper();

        // HTTP requests can contain credentials in headers, paths and bodies.
        // Transient fields also prevent Gson's default reflective serializer
        // from copying them into diagnostic JSON.
        private final transient String profile;
        private final transient Method method;
        private final transient String relativePath;
        private final transient Map<String, String> headers;
        private final transient byte[] body;
        private final transient String contentType;
        private final transient String comment;
        private final transient IntentKind intentKind;
        private final transient String intent;
        private final transient RetryMode retryMode;
        private final transient List<TraceNode> parentTraceChain;

        public Call(String profile, Method method, String relativePath, Map<String, String> headers,
                    byte[] body, String contentType, String comment, IntentKind intentKind,
                    String intent, RetryMode retryMode, List<TraceNode> parentTraceChain) {
            if (profile == null || !profile.matches("[A-Za-z][A-Za-z0-9_-]{0,63}")
                    || method == null || relativePath == null || relativePath.isBlank()
                    || comment == null || comment.isBlank() || intentKind == null
                    || intent == null || intent.isBlank() || retryMode == null) {
                throw new HttpRequestFailure(HttpRequestFailure.Code.INVALID_INPUT);
            }
            if (headers != null && headers.entrySet().stream()
                    .anyMatch(entry -> entry.getKey() == null || entry.getValue() == null)) {
                throw new HttpRequestFailure(HttpRequestFailure.Code.INVALID_INPUT);
            }
            Map<String, String> safeHeaders = headers == null ? Map.of() : Map.copyOf(headers);
            byte[] safeBody = body == null ? new byte[0] : body.clone();
            if (contentType != null && contentType.chars().anyMatch(c -> c < 32 || c == 127)) {
                throw new HttpRequestFailure(HttpRequestFailure.Code.INVALID_INPUT);
            }
            if ((method == Method.GET || method == Method.DELETE) && safeBody.length != 0) {
                throw new HttpRequestFailure(HttpRequestFailure.Code.INVALID_INPUT);
            }
            if (parentTraceChain == null || !validParentTraceChain(parentTraceChain)) {
                throw new HttpRequestFailure(HttpRequestFailure.Code.INVALID_INPUT);
            }
            this.profile = profile;
            this.method = method;
            this.relativePath = relativePath;
            this.headers = safeHeaders;
            this.body = safeBody;
            this.contentType = contentType;
            this.comment = comment;
            this.intentKind = intentKind;
            this.intent = intent;
            this.retryMode = retryMode;
            this.parentTraceChain = List.copyOf(parentTraceChain);
        }

        /** Preserve the original ten-argument API; lineage is opt-in and request-owned. */
        public Call(String profile, Method method, String relativePath, Map<String, String> headers,
                    byte[] body, String contentType, String comment, IntentKind intentKind,
                    String intent, RetryMode retryMode) {
            this(profile, method, relativePath, headers, body, contentType,
                    comment, intentKind, intent, retryMode, List.of());
        }

        public String profile() { return profile; }
        public Method method() { return method; }
        public String relativePath() { return relativePath; }
        public Map<String, String> headers() { return headers; }
        public byte[] body() { return body.clone(); }
        public String contentType() { return contentType; }
        public String comment() { return comment; }
        public IntentKind intentKind() { return intentKind; }
        public String intent() { return intent; }
        public RetryMode retryMode() { return retryMode; }
        public List<TraceNode> parentTraceChain() { return parentTraceChain; }
        @Override public String toString() { return "HttpCall[profile=" + profile + ", method=" + method + ", details=masked]"; }

        @Override public boolean equals(Object other) {
            if (!(other instanceof Call call)) return false;
            return java.util.Objects.equals(profile, call.profile)
                    && method == call.method
                    && java.util.Objects.equals(relativePath, call.relativePath)
                    && java.util.Objects.equals(headers, call.headers)
                    // A record's byte[] component used reference equality.
                    && body == call.body
                    && java.util.Objects.equals(contentType, call.contentType)
                    && java.util.Objects.equals(comment, call.comment)
                    && intentKind == call.intentKind
                    && java.util.Objects.equals(intent, call.intent)
                    && retryMode == call.retryMode
                    && java.util.Objects.equals(parentTraceChain, call.parentTraceChain);
        }

        @Override public int hashCode() {
            return java.util.Objects.hash(profile, method, relativePath, headers, body,
                    contentType, comment, intentKind, intent, retryMode, parentTraceChain);
        }

        public static final class BlockedSerializer
                extends com.fasterxml.jackson.databind.JsonSerializer<Call> {
            @Override public void serialize(Call value,
                                            com.fasterxml.jackson.core.JsonGenerator generator,
                                            com.fasterxml.jackson.databind.SerializerProvider serializers)
                    throws java.io.IOException {
                throw new java.io.IOException("HTTP request serialization denied");
            }
        }

        /** Attach an immutable snapshot from the initiating request, never the Context stack. */
        public Call withParentTraceChain(List<TraceNode> parent) {
            return new Call(profile, method, relativePath, headers, body, contentType,
                    comment, intentKind, intent, retryMode, parent);
        }

        public Call withParentRequest(QueryRequest request) {
            if (request == null) throw new HttpRequestFailure(HttpRequestFailure.Code.INVALID_INPUT);
            return withParentTraceChain(request.businessTraceChain());
        }

        public List<TraceNode> businessTraceChain() {
            var chain = new ArrayList<TraceNode>();
            if (parentTraceChain.isEmpty()) {
                chain.add(new TraceNode(intentKind == IntentKind.PURPOSE
                        ? TraceKind.PURPOSE : TraceKind.AUDIT_REASON, profile, intent));
            } else {
                chain.addAll(parentTraceChain);
            }
            chain.add(new TraceNode(TraceKind.COMMENT, profile, comment));
            return List.copyOf(chain);
        }

        /** Structured safe projection; never includes IDs, path, headers, or body. */
        public String businessTraceProjection() {
            var nodes = businessTraceChain().stream().map(node -> {
                var fields = new LinkedHashMap<String, String>();
                fields.put("comment", node.getComment().replaceAll("[\\p{Cntrl}]", " "));
                fields.put("kind", node.getKind().name());
                return fields;
            }).toList();
            return TRACE_JSON.valueToTree(nodes).toString();
        }

        public HttpObservation.Operation observation() {
            return new HttpObservation.Operation(
                    profile, method,
                    comment.replaceAll("[\\p{Cntrl}]", " "),
                    intent.replaceAll("[\\p{Cntrl}]", " "),
                    businessTraceProjection());
        }

        private static boolean validParentTraceChain(List<TraceNode> parent) {
            if (parent.size() > 63) return false;
            for (int index = 0; index < parent.size(); index++) {
                TraceNode node = parent.get(index);
                if (node == null || node.getEntityId() != null || node.getComment() == null
                        || node.getComment().isBlank()
                        || (index == 0 && node.getKind() != TraceKind.PURPOSE
                                && node.getKind() != TraceKind.AUDIT_REASON)
                        || (index > 0 && node.getKind() != TraceKind.COMMENT)) return false;
            }
            return true;
        }

        public static Call get(String profile, String path, String comment, String purpose) {
            return new Call(profile, Method.GET, path, Map.of(), null, null,
                    comment, IntentKind.PURPOSE, purpose, RetryMode.NONE);
        }

        public static Call postText(String profile, String path, String text, String comment, String purpose) {
            return new Call(profile, Method.POST, path, Map.of(),
                    text == null ? null : text.getBytes(StandardCharsets.UTF_8),
                    "text/plain; charset=utf-8", comment, IntentKind.PURPOSE, purpose, RetryMode.NONE);
        }
    }

    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = Response.BlockedSerializer.class)
    final class Response {
        // A response can contain a Vault secret or another credential.
        private final transient int status;
        private final transient byte[] body;
        private final transient int attempts;

        public Response(int status, byte[] body, int attempts) {
            if (status < 100 || status > 599 || body == null || attempts < 1) {
                throw new IllegalArgumentException("invalid HTTP response");
            }
            this.status = status;
            this.body = body.clone();
            this.attempts = attempts;
        }

        public int status() { return status; }
        public byte[] body() { return body.clone(); }
        public int attempts() { return attempts; }
        public String text() {
            try {
                return StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(body)).toString();
            } catch (CharacterCodingException invalid) {
                throw new HttpRequestFailure(HttpRequestFailure.Code.MALFORMED_RESPONSE);
            }
        }
        public Object jsonValue() {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(text(), Object.class);
            } catch (com.fasterxml.jackson.core.JsonProcessingException invalid) {
                throw new HttpRequestFailure(HttpRequestFailure.Code.MALFORMED_RESPONSE);
            }
        }
        @Override public String toString() { return "HttpResponse[status=" + status + ", attempts=" + attempts + ", body=masked]"; }

        @Override public boolean equals(Object other) {
            return other instanceof Response response && status == response.status
                    && body == response.body && attempts == response.attempts;
        }

        @Override public int hashCode() {
            return java.util.Objects.hash(status, body, attempts);
        }

        public static final class BlockedSerializer
                extends com.fasterxml.jackson.databind.JsonSerializer<Response> {
            @Override public void serialize(Response value,
                                            com.fasterxml.jackson.core.JsonGenerator generator,
                                            com.fasterxml.jackson.databind.SerializerProvider serializers)
                    throws java.io.IOException {
                throw new java.io.IOException("HTTP response serialization denied");
            }
        }
    }

    Response execute(UserContext context, Call call);
}
