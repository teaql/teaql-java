package io.teaql.blob.reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.blob.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Test;
import static org.junit.Assert.*;

/** Shared policy/deadline components; SQL/HTTP/read authorization still need integration gates. */
public class BlobPolicyTest {
    private JsonNode test(String id) throws Exception {
        try (var input = getClass().getResourceAsStream("/policy-cases.json")) {
            for (JsonNode test : new ObjectMapper().readTree(input).get("cases")) {
                if (test.get("id").asText().equals(id)) return test;
            }
        }
        throw new AssertionError(id);
    }
    private Set<String> strings(JsonNode array) {
        Set<String> values = new TreeSet<>(); array.forEach(value -> values.add(value.asText())); return values;
    }
    private BlobFieldSpec field(Set<String> extensions, String profile) {
        return BlobFieldSpec.of("Asset", "document", extensions, false, profile);
    }
    private BlobPolicySnapshot defaults() { return BlobPolicies.defaults().resolve(field(Set.of("pdf"), "default")); }
    private Instant at(JsonNode input, String name) { return Instant.ofEpochSecond(input.get(name).asLong()); }
    private void error(String expected, Runnable action) { assertEquals(expected, assertThrows(BlobException.class, action::run).code().name()); }

    @Test public void sharedDefaultsAndFormatIntersections() throws Exception {
        var expected = test("defaults").get("expected"); var policy = defaults();
        assertEquals(expected.get("maxSizeBytes").asLong(), policy.maxSizeBytes());
        assertEquals(expected.get("uploadSessionTtlSeconds").asLong(), policy.uploadSessionTtl().getSeconds());
        assertEquals(expected.get("unboundRetentionSeconds").asLong(), policy.unboundRetention().getSeconds());
        assertEquals(expected.get("unreferencedRetentionSeconds").asLong(), policy.unreferencedRetention().getSeconds());
        assertEquals(expected.get("readUrlTtlSeconds").asLong(), policy.readUrlTtl().getSeconds());
        for (String id : List.of("intersections", "empty-intersection")) {
            var test = test(id); var input = test.get("input");
            var limits = new BlobPolicies.Limits(104857600, Duration.ofHours(1), input.has("deploymentExtensions") ? strings(input.get("deploymentExtensions")) : BlobPolicies.FORMATS, Set.of());
            var profiles = Map.of("documents", BlobPolicyProfile.named("documents").allowedExtensions(strings(input.get("profileExtensions"))).build());
            var resolver = new BlobPolicies(limits, BlobPolicyProfile.named("default").build(), profiles, Map.of());
            var field = field(strings(input.get("modelExtensions")), "documents");
            if (test.has("error")) error(test.get("error").asText(), () -> resolver.resolve(field));
            else {
                var result = resolver.resolve(field);
                assertEquals(strings(test.get("expected").get("allowedExtensions")), result.field().allowedExtensions());
                assertEquals(strings(test.get("expected").get("allowedContentTypes")), result.field().allowedContentTypes());
            }
        }
    }
    @Test public void hardSizeAndDocumentReadCredentialCapsAreIndependent() throws Exception {
        var test = test("hard-size-limit"); var input = test.get("input");
        var limits = new BlobPolicies.Limits(input.get("maxUploadSizeBytes").asLong(), Duration.ofHours(1), BlobPolicies.FORMATS, Set.of());
        var resolver = new BlobPolicies(limits, BlobPolicyProfile.named("default").maxSizeBytes(input.get("maxSizeBytes").asLong()).build(), Map.of(), Map.of());
        error(test.get("error").asText(), () -> resolver.resolve(field(Set.of("pdf"), "default")));
        test = test("url-capped-by-document"); input = test.get("input");
        Duration ttl = BlobPolicies.defaults().readTtl(defaults(), Duration.ofSeconds(input.get("requestedTtlSeconds").asLong()),
                Duration.ofSeconds(input.get("storeMaximumSeconds").asLong()), Duration.ofSeconds(input.get("documentRemainingSeconds").asLong()));
        assertEquals(test.get("expected").get("readUrlTtlSeconds").asLong(), ttl.getSeconds());
        assertEquals(Duration.ofDays(7), defaults().unreferencedRetention());
        assertEquals(Duration.ofSeconds(1), BlobPolicies.defaults().readTtl(defaults(), null, Duration.ofMinutes(10), Duration.ofMillis(1999)));
    }
    @Test public void uploadAndUnboundClocksUseTheirOwnOriginsAndExpiryIsInclusive() throws Exception {
        var test = test("upload-clock-independent"); var input = test.get("input"); var policy = defaults();
        assertEquals(test.get("expected").get("uploadSessionExpired").asBoolean(), !at(input, "nowSeconds").isBefore(at(input, "beginAtSeconds").plus(policy.uploadSessionTtl())));
        assertEquals(test.get("expected").get("unboundEligibleAtSeconds").asLong(), at(input, "completedAtSeconds").plus(policy.unboundRetention()).getEpochSecond());
        test = test("unbound-deadline-rejects-bind-before-gc"); input = test.get("input");
        Instant now = at(input, "nowSeconds"), completed = at(input, "completedAtSeconds"); Duration retention = Duration.ofSeconds(input.get("unboundRetentionSeconds").asLong());
        error(test.get("error").asText(), () -> BlobRetention.requireBindable(BlobState.UPLOADED_UNBOUND, now, completed, retention, null));
        BlobRetention.requireBindable(BlobState.UPLOADED_UNBOUND, now.minusNanos(1), completed, retention, null);
        error(test("deleting-rejects-binding").get("error").asText(), () -> BlobRetention.requireBindable(BlobState.DELETING, now, completed, retention, null));
    }
    @Test public void liveBindingsAndHistoricalPromisesOutliveUrlOrProfileChanges() throws Exception {
        var test = test("active-reference-never-collected"); var input = test.get("input");
        assertFalse(BlobRetention.collectable(BlobState.AVAILABLE, input.get("bindingCount").asLong(), at(input, "nowSeconds"), Instant.EPOCH, Instant.EPOCH, Duration.ZERO, Instant.EPOCH));
        // This shared case's additional current-read-authorization assertion belongs to the real read service gate.
        test = test("read-expiry-does-not-delete"); assertTrue(test.get("input").get("readUrlExpired").asBoolean());
        assertFalse(BlobRetention.collectable(BlobState.AVAILABLE, test.get("input").get("bindingCount").asLong(), Instant.ofEpochSecond(315360000), Instant.EPOCH, Instant.EPOCH, Duration.ZERO, Instant.EPOCH));
        test = test("maximum-retention-across-fields"); input = test.get("input");
        var accepted = new java.util.ArrayList<Duration>(); input.get("acceptedRetentionSeconds").forEach(value -> accepted.add(Duration.ofSeconds(value.asLong())));
        Duration effective = BlobRetention.effectiveRetention(accepted, Duration.ZERO);
        assertEquals(test.get("expected").get("effectiveUnreferencedRetentionSeconds").asLong(), effective.getSeconds());
        assertEquals(test.get("expected").get("unreferencedEligibleAtSeconds").asLong(), BlobRetention.unreferencedEligibleAt(at(input, "lastUnboundAtSeconds"), effective, at(input, "retentionFloorUntilSeconds")).getEpochSecond());
        test = test("profile-update-does-not-shorten"); input = test.get("input");
        assertEquals(test.get("expected").get("effectiveUnreferencedRetentionSeconds").asLong(), BlobRetention.effectiveRetention(List.of(Duration.ofSeconds(input.get("acceptedUnreferencedRetentionSeconds").asLong())), Duration.ofSeconds(input.get("currentProfileRetentionSeconds").asLong())).getSeconds());
    }
    @Test public void explicitCapabilitiesAndPolicySnapshotsFailClosed() throws Exception {
        var test = test("context-checked-read-is-explicit");
        error(test.get("error").asText(), () -> BlobPolicies.requireContextCheckedRead(test.get("input").get("requireContextCheckedRead").asBoolean(), strings(test.get("input").get("storeCapabilities"))));
        var moduleDefaults = BlobPolicyProfile.named("default").maxSizeBytes(1000).unreferencedRetention(Duration.ofDays(30)).build();
        var named = BlobPolicyProfile.named("documents").maxSizeBytes(500).build();
        var policies = new BlobPolicies(BlobPolicies.Limits.defaults(), moduleDefaults, Map.of("documents", named), Map.of("Asset.document", "documents"));
        var snapshot = policies.resolve(field(Set.of("pdf"), "default"));
        assertEquals(500, snapshot.maxSizeBytes()); assertEquals(Duration.ofDays(30), snapshot.unreferencedRetention());
        assertEquals("documents", snapshot.profile());
        assertThrows(UnsupportedOperationException.class, () -> named.overrides().clear());
        error("BLOB_POLICY_INVALID", () -> policies.resolve(BlobFieldSpec.of("Asset", "other", Set.of("pdf"), false, "unknown")));
        error("BLOB_POLICY_INVALID", () -> policies.validate(List.of()));
        for (Duration invalid : List.of(Duration.ZERO, Duration.ofSeconds(-1), Duration.ofNanos(1))) {
            error("BLOB_POLICY_INVALID", () -> new BlobPolicies(BlobPolicies.Limits.defaults(), BlobPolicyProfile.named("default").readUrlTtl(invalid).build(), Map.of(), Map.of()).resolve(field(Set.of("pdf"), "default")));
        }
    }
}
