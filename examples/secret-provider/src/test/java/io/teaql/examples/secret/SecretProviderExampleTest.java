package io.teaql.examples.secret;

import io.teaql.core.secret.SecretFailure;
import io.teaql.core.secret.SecretProvider;
import io.teaql.core.secret.SecretReference;
import io.teaql.core.secret.SecretValue;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityKey;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.secret.BoundSecretProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class SecretProviderExampleTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private static final class LedgerProbe extends BaseEntity {
        private String name = "original";

        private LedgerProbe(long id) {
            updateId(id);
        }

        private void updateName(String replacement) {
            handleUpdate("name", name, replacement);
            name = replacement;
        }
    }

    @Test public void startupBindingAndContextResolutionObserveRotation() throws Exception {
        Path root = temporary.newFolder("keys").toPath();
        org.junit.Assume.assumeTrue(Files.getFileStore(root).supportsFileAttributeView("posix"));
        Files.setPosixFilePermissions(root, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        Path file = root.resolve("signing-key");
        Files.writeString(file, "private-example-key-v1");
        Files.setPosixFilePermissions(file, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE));
        SecretReference reference = new SecretReference("document", "signing");
        BoundSecretProvider provider = BoundSecretProvider.builder(root)
                .file(reference, Path.of("signing-key"), "v1", null).build();
        DefaultUserContext context = new DefaultUserContext(null).withSecretProvider(provider);
        Object marker = new Object();
        BusinessClock clock = ignored -> LocalDate.of(2000, 1, 1);
        context.putAttribute("example.rotation-marker", marker);
        context.putAttribute(BusinessClock.class.getName(), clock);
        LedgerProbe pending = new LedgerProbe(42);
        pending.updateName("pending before rotation");
        var ledger = pending.getEntityMutationLedger();
        EntityKey pendingKey = new EntityKey(pending.typeName(), 42L);

        assertEquals("private-example-key-v1", new String(
                context.resolveSecret(reference, "sign document").exposeBytes(), StandardCharsets.UTF_8));
        Files.writeString(file, "private-example-key-v2");
        context.withSecretProvider(BoundSecretProvider.builder(root)
                .file(reference, Path.of("signing-key"), "v2", null).build());
        var rotated = context.resolveSecret(reference, "sign next document");
        assertEquals("private-example-key-v2", new String(rotated.exposeBytes(), StandardCharsets.UTF_8));
        assertEquals("v2", rotated.version());
        assertSame(marker, context.getAttribute("example.rotation-marker"));
        assertSame(clock, context.getAttribute(BusinessClock.class.getName()));
        assertSame(ledger, pending.getEntityMutationLedger());
        assertEquals("pending before rotation", ledger.get(pendingKey, "name"));
        assertTrue(pending.dirtyFields().contains("name"));
        assertFalse(rotated.toString().contains("private-example-key"));
        assertEquals(SecretFailure.Code.INVALID_INPUT,
                assertThrows(SecretFailure.class,
                        () -> context.resolveSecret(reference, " ")).code());
        System.out.println("SECRET_ROTATION_LEDGER_UNCHANGED_PASS language=java context=1 clock=1 pending=1");
    }

    @Test public void operationalExpiryAndSharedCustomProviderRemainContextIsolated() throws Exception {
        SecretReference reference = new SecretReference("document", "signing");
        Path root = temporary.newFolder("expired-keys").toPath();
        org.junit.Assume.assumeTrue(Files.getFileStore(root).supportsFileAttributeView("posix"));
        Files.setPosixFilePermissions(root, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        Path key = root.resolve("signing-key");
        Files.writeString(key, "private-example-key");
        Files.setPosixFilePermissions(key, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE));
        BoundSecretProvider expired = BoundSecretProvider.builder(root)
                .file(reference, Path.of("signing-key"), "expired", Instant.EPOCH).build();
        DefaultUserContext frozen = new DefaultUserContext(null).withSecretProvider(expired);
        frozen.putAttribute(BusinessClock.class.getName(),
                (BusinessClock) ignored -> LocalDate.of(1900, 1, 1));
        assertEquals(SecretFailure.Code.EXPIRED,
                assertThrows(SecretFailure.class,
                        () -> frozen.resolveSecret(reference, "check operational expiry")).code());

        CountDownLatch entered = new CountDownLatch(2);
        SecretProvider custom = (caller, requested) -> {
            entered.countDown();
            try {
                if (!entered.await(3, TimeUnit.SECONDS)) {
                    throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
            }
            return new SecretValue(caller.getAttribute("example.business-date").toString()
                    .getBytes(StandardCharsets.UTF_8), null, null);
        };
        DefaultUserContext first = new DefaultUserContext(null).withSecretProvider(custom);
        DefaultUserContext second = new DefaultUserContext(null).withSecretProvider(custom);
        first.putAttribute("example.business-date", "2000-01-01");
        second.putAttribute("example.business-date", "2100-01-01");
        // Same type and ID make accidental process- or provider-owned ledger
        // sharing observable even when the two calls run concurrently.
        LedgerProbe firstEntity = new LedgerProbe(42);
        LedgerProbe secondEntity = new LedgerProbe(42);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var firstCall = executor.submit(() -> {
                String resolved = new String(first.resolveSecret(reference, "first custom context")
                        .exposeBytes(), StandardCharsets.UTF_8);
                firstEntity.updateName("first pending");
                return resolved;
            });
            var secondCall = executor.submit(() -> {
                String resolved = new String(second.resolveSecret(reference, "second custom context")
                        .exposeBytes(), StandardCharsets.UTF_8);
                secondEntity.updateName("second pending");
                return resolved;
            });
            assertEquals("2000-01-01", firstCall.get(4, TimeUnit.SECONDS));
            assertEquals("2100-01-01", secondCall.get(4, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
        assertEquals("2000-01-01", first.getAttribute("example.business-date"));
        assertEquals("2100-01-01", second.getAttribute("example.business-date"));
        EntityKey ledgerKey = new EntityKey(firstEntity.typeName(), 42L);
        assertNotSame(firstEntity.getEntityMutationLedger(), secondEntity.getEntityMutationLedger());
        assertEquals("first pending", firstEntity.getEntityMutationLedger().get(ledgerKey, "name"));
        assertEquals("second pending", secondEntity.getEntityMutationLedger().get(ledgerKey, "name"));
        firstEntity.getEntityMutationLedger().clearCurrentChangeSet();
        assertNull(firstEntity.getEntityMutationLedger().get(ledgerKey, "name"));
        assertEquals("second pending", secondEntity.getEntityMutationLedger().get(ledgerKey, "name"));
        assertTrue(secondEntity.dirtyFields().contains("name"));
        System.out.println("SECRET_CONTEXT_LEDGER_ISOLATION_PASS language=java overlap=1 same_id=1 clear_isolated=1");
    }
}
