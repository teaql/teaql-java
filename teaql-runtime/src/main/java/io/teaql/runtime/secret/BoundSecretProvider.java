package io.teaql.runtime.secret;

import io.teaql.core.UserContext;
import io.teaql.core.secret.SecretFailure;
import io.teaql.core.secret.SecretProvider;
import io.teaql.core.secret.SecretReference;
import io.teaql.core.secret.SecretValue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.function.Function;

/** No-cache baseline with startup-bound aliases and owner-only single-file bindings. */
public final class BoundSecretProvider implements SecretProvider {
    private record Binding(String variable, Path file, String version, Instant expiresAt) {}

    private final Path root;
    private final Object rootFileKey;
    private final Map<SecretReference, Binding> bindings;
    private final Function<String, String> environment;

    private BoundSecretProvider(Path root, Object rootFileKey, Map<SecretReference, Binding> bindings,
                                Function<String, String> environment) {
        this.root = root;
        this.rootFileKey = rootFileKey;
        this.bindings = Map.copyOf(bindings);
        this.environment = environment;
    }

    public static Builder builder(Path fileRoot) {
        return new Builder(fileRoot, System::getenv);
    }

    static Builder testBuilder(Path fileRoot, Function<String, String> environment) {
        return new Builder(fileRoot, environment);
    }

    @Override public SecretValue resolve(UserContext context, SecretReference reference) {
        if (context == null || reference == null) throw new SecretFailure(SecretFailure.Code.INVALID_INPUT);
        Binding binding = bindings.get(reference);
        if (binding == null) throw new SecretFailure(SecretFailure.Code.UNKNOWN_BINDING);
        if (binding.expiresAt() != null && !binding.expiresAt().isAfter(Instant.now())) {
            throw new SecretFailure(SecretFailure.Code.EXPIRED);
        }
        byte[] bytes;
        if (binding.variable() != null) {
            String value = environment.apply(binding.variable());
            if (value == null) throw new SecretFailure(SecretFailure.Code.MISSING);
            bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } else {
            bytes = readFile(root, rootFileKey, binding.file());
        }
        return new SecretValue(bytes, binding.version(), binding.expiresAt());
    }

    private static byte[] readFile(Path root, Object rootFileKey, Path relative) {
        if (Files.isSymbolicLink(root)) throw new SecretFailure(SecretFailure.Code.DENIED);
        try (DirectoryStream<Path> directory = Files.newDirectoryStream(root)) {
            if (!(directory instanceof SecureDirectoryStream<Path> secure)) {
                throw new SecretFailure(SecretFailure.Code.DENIED);
            }
            PosixFileAttributeView rootView = secure.getFileAttributeView(PosixFileAttributeView.class);
            if (rootView == null) throw new SecretFailure(SecretFailure.Code.DENIED);
            PosixFileAttributes rootAttributes = rootView.readAttributes();
            if (!rootAttributes.isDirectory()
                    || !java.util.Objects.equals(rootFileKey, rootAttributes.fileKey())
                    || rootAttributes.permissions().contains(PosixFilePermission.GROUP_WRITE)
                    || rootAttributes.permissions().contains(PosixFilePermission.OTHERS_WRITE)) {
                throw new SecretFailure(SecretFailure.Code.DENIED);
            }
            PosixFileAttributeView fileView = secure.getFileAttributeView(relative,
                    PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            if (fileView == null) throw new SecretFailure(SecretFailure.Code.DENIED);
            PosixFileAttributes before = fileView.readAttributes();
            if (!privateRegularFile(before)) throw new SecretFailure(SecretFailure.Code.DENIED);
            try (SeekableByteChannel channel = secure.newByteChannel(relative,
                    Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS));
                 InputStream stream = Channels.newInputStream(channel)) {
                if (channel.size() > SecretValue.MAX_BYTES) throw new SecretFailure(SecretFailure.Code.DENIED);
                byte[] bytes = stream.readNBytes(SecretValue.MAX_BYTES + 1);
                PosixFileAttributes after = fileView.readAttributes();
                if (bytes.length > SecretValue.MAX_BYTES || !privateRegularFile(after)
                        || !java.util.Objects.equals(before.fileKey(), after.fileKey())) {
                    throw new SecretFailure(SecretFailure.Code.DENIED);
                }
                return bytes;
            }
        } catch (SecretFailure failure) {
            throw failure;
        } catch (java.nio.file.NoSuchFileException failure) {
            throw new SecretFailure(SecretFailure.Code.MISSING);
        } catch (IOException failure) {
            throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
        }
    }

    private static boolean privateRegularFile(PosixFileAttributes attributes) {
        Set<PosixFilePermission> mode = attributes.permissions();
        return attributes.isRegularFile() && attributes.fileKey() != null
                && attributes.size() <= SecretValue.MAX_BYTES
                && !mode.contains(PosixFilePermission.GROUP_READ)
                && !mode.contains(PosixFilePermission.GROUP_WRITE)
                && !mode.contains(PosixFilePermission.GROUP_EXECUTE)
                && !mode.contains(PosixFilePermission.OTHERS_READ)
                && !mode.contains(PosixFilePermission.OTHERS_WRITE)
                && !mode.contains(PosixFilePermission.OTHERS_EXECUTE);
    }

    public static final class Builder {
        private final Path root;
        private final Object rootFileKey;
        private final Function<String, String> environment;
        private final Map<SecretReference, Binding> bindings = new HashMap<>();

        private Builder(Path fileRoot, Function<String, String> environment) {
            this.environment = java.util.Objects.requireNonNull(environment);
            if (fileRoot == null) {
                this.root = null;
                this.rootFileKey = null;
            } else {
                try {
                    if (Files.isSymbolicLink(fileRoot) || !Files.isDirectory(fileRoot,
                            LinkOption.NOFOLLOW_LINKS)) {
                        throw new SecretFailure(SecretFailure.Code.DENIED);
                    }
                    Set<PosixFilePermission> rootMode = Files.getPosixFilePermissions(fileRoot,
                            LinkOption.NOFOLLOW_LINKS);
                    if (rootMode.contains(PosixFilePermission.GROUP_WRITE)
                            || rootMode.contains(PosixFilePermission.OTHERS_WRITE)) {
                        throw new SecretFailure(SecretFailure.Code.DENIED);
                    }
                    this.root = fileRoot.toRealPath(LinkOption.NOFOLLOW_LINKS);
                    PosixFileAttributes resolved = Files.readAttributes(this.root,
                            PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    if (!resolved.isDirectory() || resolved.fileKey() == null
                            || resolved.permissions().contains(PosixFilePermission.GROUP_WRITE)
                            || resolved.permissions().contains(PosixFilePermission.OTHERS_WRITE)) {
                        throw new SecretFailure(SecretFailure.Code.DENIED);
                    }
                    this.rootFileKey = resolved.fileKey();
                } catch (IOException failure) {
                    throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
                } catch (UnsupportedOperationException failure) {
                    throw new SecretFailure(SecretFailure.Code.DENIED);
                }
            }
        }

        public Builder environment(SecretReference reference, String variable,
                                   String version, Instant expiresAt) {
            if (reference == null || variable == null || !variable.matches("[A-Za-z0-9_]+")) {
                throw new SecretFailure(SecretFailure.Code.INVALID_INPUT);
            }
            bindings.put(reference, new Binding(variable, null, version, expiresAt));
            return this;
        }

        public Builder file(SecretReference reference, Path relative,
                            String version, Instant expiresAt) {
            if (root == null) throw new SecretFailure(SecretFailure.Code.PROVIDER_NOT_CONFIGURED);
            if (reference == null || relative == null || relative.isAbsolute()
                    || relative.getNameCount() != 1 || relative.toString().isBlank()) {
                throw new SecretFailure(SecretFailure.Code.INVALID_INPUT);
            }
            for (Path component : relative) {
                String name = component.toString();
                if (name.isBlank() || name.equals(".") || name.equals("..")) {
                    throw new SecretFailure(SecretFailure.Code.INVALID_INPUT);
                }
            }
            bindings.put(reference, new Binding(null, relative, version, expiresAt));
            return this;
        }

        public BoundSecretProvider build() {
            return new BoundSecretProvider(root, rootFileKey, bindings, environment);
        }
    }
}
