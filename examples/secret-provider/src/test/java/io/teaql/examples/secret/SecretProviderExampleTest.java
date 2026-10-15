package io.teaql.examples.secret;

import io.teaql.core.secret.SecretFailure;
import io.teaql.core.secret.SecretReference;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.secret.BoundSecretProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class SecretProviderExampleTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

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

        assertEquals("private-example-key-v1", new String(
                context.resolveSecret(reference, "sign document").exposeBytes(), StandardCharsets.UTF_8));
        Files.writeString(file, "private-example-key-v2");
        context.withSecretProvider(BoundSecretProvider.builder(root)
                .file(reference, Path.of("signing-key"), "v2", null).build());
        var rotated = context.resolveSecret(reference, "sign next document");
        assertEquals("private-example-key-v2", new String(rotated.exposeBytes(), StandardCharsets.UTF_8));
        assertEquals("v2", rotated.version());
        assertFalse(rotated.toString().contains("private-example-key"));
        assertEquals(SecretFailure.Code.INVALID_INPUT,
                assertThrows(SecretFailure.class,
                        () -> context.resolveSecret(reference, " ")).code());
    }
}
