package io.teaql.core.blob;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/** Immutable, explicit server overrides. Resolution is field profile → module defaults → built-ins. */
public final class BlobPolicyProfile {
    public enum Parameter {
        MAX_SIZE_BYTES, ALLOW_EMPTY, ALLOWED_EXTENSIONS, UPLOAD_SESSION_TTL, UPLOAD_URL_TTL,
        UNBOUND_RETENTION, UNREFERENCED_RETENTION, MINIMUM_RETENTION, READ_URL_TTL,
        UPLOAD_MODE, SHARING, DISPOSITION, RESPONSE_SHAPE, CACHE_POLICY
    }
    public enum UploadMode { SERVER_STREAM, DIRECT }
    public enum Sharing { AUTHORIZED_SAME_SCOPE, SINGLE_BINDING }
    public enum Disposition { AUTO, INLINE, ATTACHMENT }
    public enum ResponseShape { URL, DESCRIPTOR }
    private final String name;
    private final long version;
    private final Map<Parameter, Object> overrides;

    private BlobPolicyProfile(Builder builder) {
        if (builder.name == null || !builder.name.matches("[A-Za-z0-9_.-]{1,128}") || builder.version <= 0) {
            throw new BlobException(BlobException.Code.BLOB_POLICY_INVALID);
        }
        name = builder.name; version = builder.version;
        overrides = Collections.unmodifiableMap(new EnumMap<>(builder.values));
    }
    public String name() { return name; }
    public long version() { return version; }
    public Map<Parameter, Object> overrides() { return overrides; }
    public static Builder named(String name) { return new Builder(name); }

    public static final class Builder {
        private final String name;
        private long version = 1;
        private final EnumMap<Parameter, Object> values = new EnumMap<>(Parameter.class);
        private Builder(String name) { this.name = name; }
        public Builder version(long version) { this.version = version; return this; }
        public Builder maxSizeBytes(long value) { values.put(Parameter.MAX_SIZE_BYTES, value); return this; }
        public Builder allowEmpty(boolean value) { values.put(Parameter.ALLOW_EMPTY, value); return this; }
        public Builder allowedExtensions(Set<String> value) { values.put(Parameter.ALLOWED_EXTENSIONS, Set.copyOf(value)); return this; }
        private Builder duration(Parameter key, Duration value) { values.put(key, java.util.Objects.requireNonNull(value)); return this; }
        public Builder uploadSessionTtl(Duration value) { return duration(Parameter.UPLOAD_SESSION_TTL, value); }
        public Builder uploadUrlTtl(Duration value) { return duration(Parameter.UPLOAD_URL_TTL, value); }
        public Builder unboundRetention(Duration value) { return duration(Parameter.UNBOUND_RETENTION, value); }
        public Builder unreferencedRetention(Duration value) { return duration(Parameter.UNREFERENCED_RETENTION, value); }
        public Builder minimumRetention(Duration value) { return duration(Parameter.MINIMUM_RETENTION, value); }
        public Builder readUrlTtl(Duration value) { return duration(Parameter.READ_URL_TTL, value); }
        public Builder uploadMode(UploadMode value) { values.put(Parameter.UPLOAD_MODE, java.util.Objects.requireNonNull(value)); return this; }
        public Builder sharing(Sharing value) { values.put(Parameter.SHARING, java.util.Objects.requireNonNull(value)); return this; }
        public Builder disposition(Disposition value) { values.put(Parameter.DISPOSITION, java.util.Objects.requireNonNull(value)); return this; }
        public Builder responseShape(ResponseShape value) { values.put(Parameter.RESPONSE_SHAPE, java.util.Objects.requireNonNull(value)); return this; }
        public Builder cachePolicy(String value) { values.put(Parameter.CACHE_POLICY, java.util.Objects.requireNonNull(value)); return this; }
        public BlobPolicyProfile build() { return new BlobPolicyProfile(this); }
    }
}
