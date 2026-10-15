package io.teaql.core.http;

import io.teaql.core.UserContext;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Context-owned, provider-neutral HTTP boundary for configured external services. */
public interface HttpRequestService {
    enum Method { GET, POST, PUT, DELETE }
    enum IntentKind { PURPOSE, AUDIT }
    enum RetryMode { NONE, CONFIRMED_IDEMPOTENT }

    record Call(String profile, Method method, String relativePath, Map<String, String> headers,
                byte[] body, String contentType, String comment, IntentKind intentKind,
                String intent, RetryMode retryMode) {
        public Call {
            if (profile == null || !profile.matches("[A-Za-z][A-Za-z0-9_-]{0,63}")
                    || method == null || relativePath == null || relativePath.isBlank()
                    || comment == null || comment.isBlank() || intentKind == null
                    || intent == null || intent.isBlank() || retryMode == null) {
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
        }

        @Override public byte[] body() { return body.clone(); }
        @Override public String toString() { return "HttpCall[profile=" + profile + ", method=" + method + ", details=masked]"; }

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
