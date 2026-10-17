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

    record Call(String profile, Method method, String relativePath, Map<String, String> headers,
                byte[] body, String contentType, String comment, IntentKind intentKind,
                String intent, RetryMode retryMode, List<TraceNode> parentTraceChain) {
        private static final com.fasterxml.jackson.databind.ObjectMapper TRACE_JSON =
                new com.fasterxml.jackson.databind.ObjectMapper();

        public Call {
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
            headers = headers == null ? Map.of() : Map.copyOf(headers);
            body = body == null ? new byte[0] : body.clone();
            if (contentType != null && contentType.chars().anyMatch(c -> c < 32 || c == 127)) {
                throw new HttpRequestFailure(HttpRequestFailure.Code.INVALID_INPUT);
            }
            if ((method == Method.GET || method == Method.DELETE) && body.length != 0) {
                throw new HttpRequestFailure(HttpRequestFailure.Code.INVALID_INPUT);
            }
            if (parentTraceChain == null || !validParentTraceChain(parentTraceChain)) {
                throw new HttpRequestFailure(HttpRequestFailure.Code.INVALID_INPUT);
            }
            parentTraceChain = List.copyOf(parentTraceChain);
        }

        /** Preserve the original ten-argument API; lineage is opt-in and request-owned. */
        public Call(String profile, Method method, String relativePath, Map<String, String> headers,
                    byte[] body, String contentType, String comment, IntentKind intentKind,
                    String intent, RetryMode retryMode) {
            this(profile, method, relativePath, headers, body, contentType,
                    comment, intentKind, intent, retryMode, List.of());
        }

        @Override public byte[] body() { return body.clone(); }
        @Override public String toString() { return "HttpCall[profile=" + profile + ", method=" + method + ", details=masked]"; }

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

    record Response(int status, byte[] body, int attempts) {
        public Response {
            if (status < 100 || status > 599 || body == null || attempts < 1) {
                throw new IllegalArgumentException("invalid HTTP response");
            }
            body = body.clone();
        }

        @Override public byte[] body() { return body.clone(); }
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
    }

    Response execute(UserContext context, Call call);
}
