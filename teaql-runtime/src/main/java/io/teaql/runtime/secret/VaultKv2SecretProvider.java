package io.teaql.runtime.secret;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.UserContext;
import io.teaql.core.http.HttpRequestService;
import io.teaql.core.secret.SecretFailure;
import io.teaql.core.secret.SecretProvider;
import io.teaql.core.secret.SecretReference;
import io.teaql.core.secret.SecretValue;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Explicit Vault KV v2 adapter. Profiles and secret paths are bound at trusted startup. */
public final class VaultKv2SecretProvider implements SecretProvider {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_RESPONSE_BYTES = 64 * 1024;

    private record Binding(String path, String field) {}

    private final String httpProfile;
    private final String mount;
    private final SecretProvider tokenProvider;
    private final SecretReference tokenReference;
    private final Map<SecretReference, Binding> bindings;

    private VaultKv2SecretProvider(Builder builder) {
        httpProfile = builder.httpProfile;
        mount = builder.mount;
        tokenProvider = builder.tokenProvider;
        tokenReference = builder.tokenReference;
        bindings = Map.copyOf(builder.bindings);
    }

    public static Builder builder(String httpProfile, String mount,
                                  SecretProvider tokenProvider, SecretReference tokenReference) {
        return new Builder(httpProfile, mount, tokenProvider, tokenReference);
    }

    @Override public SecretValue resolve(UserContext context, SecretReference reference) {
        if (context == null || reference == null) {
            throw new SecretFailure(SecretFailure.Code.INVALID_INPUT);
        }
        Binding binding = bindings.get(reference);
        if (binding == null) throw new SecretFailure(SecretFailure.Code.UNKNOWN_BINDING);
        try {
            HttpRequestService http = context.httpRequests();
            if (!http.profileIsEncryptedOrLoopback(httpProfile)) {
                throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
            }
            SecretValue credential = tokenProvider.resolve(context, tokenReference);
            if (credential == null) throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
            String token = decodeToken(credential.exposeBytes());
            HttpRequestService.Call call = new HttpRequestService.Call(
                    httpProfile, HttpRequestService.Method.GET,
                    "v1/" + mount + "/data/" + binding.path(),
                    Map.of("X-Vault-Token", token), null, null,
                    "read configured Vault secret", HttpRequestService.IntentKind.PURPOSE,
                    "resolve runtime credential", HttpRequestService.RetryMode.NONE);
            HttpRequestService.Response response = http.execute(context, call);
            if (response == null) throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
            return switch (response.status()) {
                case 200 -> parse(response.body(), binding.field());
                case 401, 403 -> throw new SecretFailure(SecretFailure.Code.DENIED);
                case 404 -> throw new SecretFailure(SecretFailure.Code.MISSING);
                default -> throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
            };
        } catch (SecretFailure safe) {
            throw safe;
        } catch (RuntimeException unsafe) {
            throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
        }
    }

    private static String decodeToken(byte[] bytes) {
        try {
            String token = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            if (token.isEmpty() || token.chars().anyMatch(c -> Character.isISOControl(c))) {
                throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
            }
            return token;
        } catch (CharacterCodingException invalid) {
            throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
        }
    }

    private static SecretValue parse(byte[] body, String field) {
        if (body.length > MAX_RESPONSE_BYTES) throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
        try {
            JsonNode document = JSON.readTree(body);
            JsonNode data = document.path("data");
            JsonNode metadata = data.path("metadata");
            JsonNode version = metadata.path("version");
            JsonNode destroyed = metadata.path("destroyed");
            JsonNode deletionTime = metadata.path("deletion_time");
            if (!version.isIntegralNumber() || !version.canConvertToLong()
                    || version.longValue() <= 0 || !destroyed.isBoolean()
                    || !deletionTime.isTextual()) {
                throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
            }
            if (destroyed.booleanValue()) throw new SecretFailure(SecretFailure.Code.MISSING);
            Instant expiresAt = deletionTime.textValue().isEmpty()
                    ? null : Instant.parse(deletionTime.textValue());
            JsonNode lease = document.path("lease_duration");
            if (!lease.isMissingNode() && (!lease.isIntegralNumber() || !lease.canConvertToLong()
                    || lease.longValue() < 0)) {
                throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
            }
            if (!lease.isMissingNode() && lease.longValue() > 0) {
                Instant leaseExpiry = Instant.now().plusSeconds(lease.longValue());
                expiresAt = expiresAt == null || leaseExpiry.isBefore(expiresAt)
                        ? leaseExpiry : expiresAt;
            }
            JsonNode value = data.path("data").path(field);
            if (!value.isTextual()) throw new SecretFailure(SecretFailure.Code.MISSING);
            byte[] bytes = value.textValue().getBytes(StandardCharsets.UTF_8);
            if (bytes.length == 0 || bytes.length > SecretValue.MAX_BYTES) {
                throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
            }
            return new SecretValue(bytes, "vault-kv2:" + version.longValue(), expiresAt);
        } catch (SecretFailure safe) {
            throw safe;
        } catch (java.io.IOException | DateTimeException | ArithmeticException invalid) {
            throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
        }
    }

    public static final class Builder {
        private final String httpProfile;
        private final String mount;
        private final SecretProvider tokenProvider;
        private final SecretReference tokenReference;
        private final Map<SecretReference, Binding> bindings = new HashMap<>();

        private Builder(String httpProfile, String mount,
                        SecretProvider tokenProvider, SecretReference tokenReference) {
            if (!safeName(httpProfile) || !safeName(mount)) {
                throw new SecretFailure(SecretFailure.Code.INVALID_INPUT);
            }
            this.httpProfile = httpProfile;
            this.mount = mount;
            this.tokenProvider = Objects.requireNonNull(tokenProvider);
            this.tokenReference = Objects.requireNonNull(tokenReference);
        }

        public Builder bind(SecretReference reference, String path, String field) {
            if (reference == null || !validPath(path) || !safeName(field)) {
                throw new SecretFailure(SecretFailure.Code.INVALID_INPUT);
            }
            bindings.put(reference, new Binding(path, field));
            return this;
        }

        public VaultKv2SecretProvider build() {
            return new VaultKv2SecretProvider(this);
        }
    }

    private static boolean safeName(String value) {
        return value != null && value.matches("[A-Za-z][A-Za-z0-9_-]{0,63}");
    }

    private static boolean validPath(String path) {
        if (path == null || path.isEmpty() || path.length() > 256) return false;
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")
                    || !segment.matches("[A-Za-z0-9_.-]+")) return false;
        }
        return true;
    }
}
