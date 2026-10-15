package io.teaql.runtime.secret;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.secret.SecretFailure;
import io.teaql.core.secret.SecretProvider;
import io.teaql.core.secret.SecretReference;
import io.teaql.core.secret.SecretValue;
import io.teaql.runtime.DefaultUserContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import io.teaql.core.businessid.BusinessClock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class BoundSecretProviderTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void contextBindingAndSafeSerialization() throws Exception {
        SecretReference reference = new SecretReference("signing", "current");
        DefaultUserContext context = new DefaultUserContext(null);
        assertCode(SecretFailure.Code.PROVIDER_NOT_CONFIGURED,
                () -> context.resolveSecret(reference, "read signing key"));
        BoundSecretProvider provider = BoundSecretProvider.testBuilder(null,
                variable -> variable.equals("TEST_KEY") ? "canary-do-not-log" : null)
                .environment(reference, "TEST_KEY", "v1", null).build();
        context.withSecretProvider(provider);
        assertCode(SecretFailure.Code.INVALID_INPUT,
                () -> context.resolveSecret(reference, " "));
        SecretValue value = context.resolveSecret(reference, "read signing key");
        assertEquals("canary-do-not-log", new String(value.exposeBytes()));
        assertEquals("v1", value.version());
        assertFalse(value.toString().contains("canary"));
        Exception error = assertThrows(Exception.class,
                () -> new ObjectMapper().writeValueAsString(value));
        assertFalse(error.toString().contains("canary"));
        assertCode(SecretFailure.Code.UNKNOWN_BINDING,
                () -> context.resolveSecret(new SecretReference("signing", "absent"), "read"));
        assertCode(SecretFailure.Code.INVALID_INPUT,
                () -> new SecretReference("signing", "../key"));
        assertCode(SecretFailure.Code.INVALID_INPUT,
                () -> new SecretReference("signing", "/tmp/key"));

        CountDownLatch entered = new CountDownLatch(2);
        SecretProvider custom = (caller, requested) -> {
            entered.countDown();
            boolean bothEntered;
            try {
                bothEntered = entered.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
            }
            if (!bothEntered) {
                throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
            }
            return new SecretValue("custom".getBytes(), null, null);
        };
        DefaultUserContext second = new DefaultUserContext(null).withSecretProvider(custom);
        DefaultUserContext third = new DefaultUserContext(null).withSecretProvider(custom);
        second.putAttribute("test.marker", "second");
        third.putAttribute("test.marker", "third");
        var executor = Executors.newFixedThreadPool(2);
        try {
            var twoCalls = executor.submit(
                    () -> new String(second.resolveSecret(reference, "second context").exposeBytes()));
            var threeCalls = executor.submit(
                    () -> new String(third.resolveSecret(reference, "third context").exposeBytes()));
            assertEquals("custom", twoCalls.get(3, TimeUnit.SECONDS));
            assertEquals("custom", threeCalls.get(3, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
        assertEquals("second", second.getAttribute("test.marker"));
        assertEquals("third", third.getAttribute("test.marker"));
        assertEquals("canary-do-not-log", new String(context.resolveSecret(reference, "first provider").exposeBytes()));

        DefaultUserContext unsafe = new DefaultUserContext(null).withSecretProvider(
                (caller, requested) -> { throw new IllegalStateException("canary-do-not-log"); });
        SecretFailure sanitized = assertThrows(SecretFailure.class,
                () -> unsafe.resolveSecret(reference, "read"));
        assertEquals(SecretFailure.Code.UNAVAILABLE, sanitized.code());
        assertNull(sanitized.getCause());
        assertFalse(sanitized.toString().contains("canary"));
        DefaultUserContext absent = new DefaultUserContext(null).withSecretProvider(
                (caller, requested) -> null);
        assertCode(SecretFailure.Code.UNAVAILABLE,
                () -> absent.resolveSecret(reference, "read"));
    }

    @Test public void fileRotationPermissionsAndSymlink() throws Exception {
        Path root = temporary.newFolder("keys").toPath();
        org.junit.Assume.assumeTrue(Files.getFileStore(root).supportsFileAttributeView("posix"));
        Files.setPosixFilePermissions(root, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        Path key = root.resolve("key");
        Files.writeString(key, "first");
        Files.setPosixFilePermissions(key, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE));
        SecretReference reference = new SecretReference("signing", "key");
        BoundSecretProvider.Builder builder = BoundSecretProvider.builder(root);
        assertCode(SecretFailure.Code.INVALID_INPUT,
                () -> builder.file(reference, Path.of("../key"), null, null));
        assertCode(SecretFailure.Code.INVALID_INPUT,
                () -> builder.file(reference, Path.of("nested/key"), null, null));
        BoundSecretProvider provider = builder.file(reference, Path.of("key"), "v1", null).build();
        DefaultUserContext context = new DefaultUserContext(null).withSecretProvider(provider);
        assertEquals("first", new String(context.resolveSecret(reference, "read").exposeBytes()));
        Files.writeString(key, "second");
        context.withSecretProvider(BoundSecretProvider.builder(root)
                .file(reference, Path.of("key"), "v2", null).build());
        SecretValue rotated = context.resolveSecret(reference, "refresh");
        assertEquals("second", new String(rotated.exposeBytes()));
        assertEquals("v2", rotated.version());
        Files.setPosixFilePermissions(key, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_READ));
        assertCode(SecretFailure.Code.DENIED,
                () -> context.resolveSecret(reference, "read"));
        Files.delete(key);
        Files.createSymbolicLink(key, root.resolve("missing"));
        assertCode(SecretFailure.Code.DENIED,
                () -> context.resolveSecret(reference, "read"));
        Files.delete(key);
        Files.write(key, new byte[SecretValue.MAX_BYTES + 1]);
        Files.setPosixFilePermissions(key, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE));
        assertCode(SecretFailure.Code.DENIED,
                () -> context.resolveSecret(reference, "oversized file"));
        Files.setPosixFilePermissions(root, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_WRITE));
        assertCode(SecretFailure.Code.DENIED,
                () -> BoundSecretProvider.builder(root));
    }

    @Test public void expiryUsesOperationalTime() {
        SecretReference reference = new SecretReference("signing", "key");
        BoundSecretProvider provider = BoundSecretProvider.testBuilder(null,
                ignored -> "canary")
                .environment(reference, "TEST_KEY", null, Instant.EPOCH).build();
        DefaultUserContext context = new DefaultUserContext(null).withSecretProvider(provider);
        context.putAttribute(BusinessClock.class.getName(),
                (BusinessClock) caller -> LocalDate.of(1900, 1, 1));
        assertCode(SecretFailure.Code.EXPIRED,
                () -> context.resolveSecret(reference, "read"));
    }

    private static void assertCode(SecretFailure.Code expected, Runnable action) {
        assertEquals(expected, assertThrows(SecretFailure.class, action::run).code());
    }
}
