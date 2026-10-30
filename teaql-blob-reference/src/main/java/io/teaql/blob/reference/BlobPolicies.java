package io.teaql.blob.reference;

import io.teaql.core.blob.*;
import io.teaql.core.blob.BlobPolicyProfile.*;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import static io.teaql.core.blob.BlobException.Code.*;

/** Trusted startup configuration. No tenant or principal is retained by this shared service. */
public final class BlobPolicies {
    public static final long DEFAULT_MAX_SIZE_BYTES = 104857600;
    public static final Set<String> FORMATS = Set.of("jpg", "jpeg", "png", "mp4", "mp3", "doc", "docx", "xls", "xlsx", "pdf");
    public record Limits(long maxUploadSizeBytes, Duration readUrlMaximum,
                         Set<String> allowedExtensions, Set<String> inlineContentTypes) {
        public Limits {
            if (maxUploadSizeBytes <= 0 || readUrlMaximum == null || readUrlMaximum.isZero()
                    || readUrlMaximum.isNegative() || readUrlMaximum.getNano() != 0
                    || allowedExtensions == null || inlineContentTypes == null
                    || !FORMATS.containsAll(allowedExtensions)) throw new BlobException(BLOB_POLICY_INVALID);
            allowedExtensions = Set.copyOf(allowedExtensions);
            inlineContentTypes = Set.copyOf(inlineContentTypes);
        }
        public static Limits defaults() {
            return new Limits(DEFAULT_MAX_SIZE_BYTES, Duration.ofHours(1), FORMATS,
                    Set.of("image/jpeg", "image/png", "audio/mpeg", "video/mp4"));
        }
    }
    private final Limits limits;
    private final BlobPolicyProfile defaults;
    private final Map<String, BlobPolicyProfile> profiles;
    private final Map<String, String> fields;

    public BlobPolicies(Limits limits, BlobPolicyProfile defaults,
                        Map<String, BlobPolicyProfile> profiles, Map<String, String> fields) {
        this.limits = java.util.Objects.requireNonNull(limits);
        this.defaults = java.util.Objects.requireNonNull(defaults);
        this.profiles = Map.copyOf(profiles); this.fields = Map.copyOf(fields);
        this.profiles.forEach((name, profile) -> {
            if (!name.equals(profile.name()) || name.equals("default")) throw new BlobException(BLOB_POLICY_INVALID);
        });
        this.fields.values().forEach(name -> {
            if (!name.equals("default") && !this.profiles.containsKey(name)) throw new BlobException(BLOB_POLICY_INVALID);
        });
    }
    public static BlobPolicies defaults() {
        return new BlobPolicies(Limits.defaults(), BlobPolicyProfile.named("default").build(), Map.of(), Map.of());
    }
    public long maximumUploadSizeBytes() { return limits.maxUploadSizeBytes(); }
    public Disposition readDisposition(BlobPolicySnapshot policy,String contentType) {
        if (policy.disposition()!=Disposition.AUTO) return policy.disposition();
        return limits.inlineContentTypes().contains(contentType)?Disposition.INLINE:Disposition.ATTACHMENT;
    }
    public void requireUploadFormat(String extension) {
        if (!limits.allowedExtensions().contains(extension)) throw new BlobException(BLOB_FORMAT_NOT_ALLOWED);
    }

    public void validate(Iterable<BlobFieldSpec> model) {
        Set<String> found = new TreeSet<>();
        for (BlobFieldSpec field : model) { found.add(field.entity() + "." + field.field()); resolve(field); }
        if (!found.containsAll(fields.keySet())) throw new BlobException(BLOB_POLICY_INVALID);
    }

    public BlobPolicySnapshot resolve(BlobFieldSpec model) {
        String name = fields.getOrDefault(model.entity() + "." + model.field(), model.policyProfile());
        BlobPolicyProfile profile = name.equals("default") ? defaults : profiles.get(name);
        if (profile == null) throw new BlobException(BLOB_POLICY_INVALID);
        EnumMap<Parameter, Object> values = builtIns();
        values.putAll(defaults.overrides()); values.putAll(profile.overrides());
        @SuppressWarnings("unchecked") Set<String> profileFormats = (Set<String>) values.get(Parameter.ALLOWED_EXTENSIONS);
        Set<String> formats = new TreeSet<>(model.allowedExtensions());
        formats.retainAll(limits.allowedExtensions()); formats.retainAll(profileFormats);
        if (formats.isEmpty() || !FORMATS.containsAll(profileFormats)) throw new BlobException(BLOB_POLICY_INVALID);
        long size = (Long) values.get(Parameter.MAX_SIZE_BYTES);
        if (size > limits.maxUploadSizeBytes()) throw new BlobException(BLOB_POLICY_INVALID);
        BlobFieldSpec effective = BlobFieldSpec.of(model.entity(), model.field(), formats, model.nullable(), name);
        Disposition disposition = (Disposition) values.get(Parameter.DISPOSITION);
        if (disposition == Disposition.INLINE && !limits.inlineContentTypes().containsAll(effective.allowedContentTypes())) {
            throw new BlobException(BLOB_POLICY_INVALID);
        }
        return new BlobPolicySnapshot(name, profile.version(), effective, size,
                (Boolean) values.get(Parameter.ALLOW_EMPTY), duration(values, Parameter.UPLOAD_SESSION_TTL),
                duration(values, Parameter.UPLOAD_URL_TTL), duration(values, Parameter.UNBOUND_RETENTION),
                duration(values, Parameter.UNREFERENCED_RETENTION), duration(values, Parameter.MINIMUM_RETENTION),
                duration(values, Parameter.READ_URL_TTL), (UploadMode) values.get(Parameter.UPLOAD_MODE),
                (Sharing) values.get(Parameter.SHARING), disposition,
                (ResponseShape) values.get(Parameter.RESPONSE_SHAPE), (String) values.get(Parameter.CACHE_POLICY));
    }

    private static Duration duration(Map<Parameter, Object> values, Parameter key) { return (Duration) values.get(key); }
    private static EnumMap<Parameter, Object> builtIns() {
        EnumMap<Parameter, Object> values = new EnumMap<>(Parameter.class);
        values.put(Parameter.MAX_SIZE_BYTES, DEFAULT_MAX_SIZE_BYTES); values.put(Parameter.ALLOW_EMPTY, false);
        values.put(Parameter.ALLOWED_EXTENSIONS, FORMATS); values.put(Parameter.UPLOAD_SESSION_TTL, Duration.ofHours(1));
        values.put(Parameter.UPLOAD_URL_TTL, Duration.ofMinutes(15)); values.put(Parameter.UNBOUND_RETENTION, Duration.ofDays(1));
        values.put(Parameter.UNREFERENCED_RETENTION, Duration.ofDays(7)); values.put(Parameter.MINIMUM_RETENTION, Duration.ZERO);
        values.put(Parameter.READ_URL_TTL, Duration.ofMinutes(10)); values.put(Parameter.UPLOAD_MODE, UploadMode.SERVER_STREAM);
        values.put(Parameter.SHARING, Sharing.AUTHORIZED_SAME_SCOPE); values.put(Parameter.DISPOSITION, Disposition.AUTO);
        values.put(Parameter.RESPONSE_SHAPE, ResponseShape.URL); values.put(Parameter.CACHE_POLICY, "private, no-store");
        return values;
    }

    /** Current read policy caps credentials, without changing object retention. */
    public Duration readTtl(BlobPolicySnapshot current, Duration requested, Duration storeMaximum, Duration documentRemaining) {
        Duration result = current.readUrlTtl();
        if (requested != null) { positive(requested, BLOB_POLICY_INVALID); result = min(result, requested); }
        positive(storeMaximum, BLOB_CAPABILITY_MISSING); result = min(result, storeMaximum);
        result = min(result, limits.readUrlMaximum());
        if (documentRemaining != null) {
            if (documentRemaining.isZero() || documentRemaining.isNegative()) throw new BlobException(BLOB_REFERENCE_EXPIRED);
            result = min(result, documentRemaining);
        }
        // Providers with whole-second credentials must never round beyond document expiry.
        result = Duration.ofSeconds(result.getSeconds());
        if (result.isZero()) throw new BlobException(BLOB_REFERENCE_EXPIRED);
        return result;
    }
    private static Duration min(Duration left, Duration right) { return left.compareTo(right) <= 0 ? left : right; }
    private static void positive(Duration value, BlobException.Code code) {
        if (value == null || value.isZero() || value.isNegative() || value.getNano() != 0) throw new BlobException(code);
    }
    public static void requireContextCheckedRead(boolean required, Set<String> capabilities) {
        if (required && !capabilities.contains("context_checked_read")) throw new BlobException(BLOB_CAPABILITY_MISSING);
    }
}
