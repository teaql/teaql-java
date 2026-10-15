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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
    private final Map<SecretReference, Binding> bindings;
    private final Function<String, String> environment;

    private BoundSecretProvider(Path root, Map<SecretReference, Binding> bindings,
                                Function<String, String> environment) {
        this.root = root;
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
            bytes = readFile(root, binding.file());
        }
        return new SecretValue(bytes, binding.version(), binding.expiresAt());
    }

    private static byte[] readFile(Path root, Path relative) {
        Path candidate = root;
        try {
            for (Path component : relative) {
                candidate = candidate.resolve(component);
                if (Files.isSymbolicLink(candidate)) throw new SecretFailure(SecretFailure.Code.DENIED);
                if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
                    throw new SecretFailure(SecretFailure.Code.MISSING);
                }
            }
            Path actual = candidate.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!actual.startsWith(root) || !Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)) {
                throw new SecretFailure(SecretFailure.Code.DENIED);
            }
            if (Files.size(candidate) > SecretValue.MAX_BYTES) throw new SecretFailure(SecretFailure.Code.DENIED);
            try {
                Set<PosixFilePermission> mode = Files.getPosixFilePermissions(candidate,
                        LinkOption.NOFOLLOW_LINKS);
                if (mode.contains(PosixFilePermission.GROUP_READ)
                        || mode.contains(PosixFilePermission.GROUP_WRITE)
                        || mode.contains(PosixFilePermission.GROUP_EXECUTE)
                        || mode.contains(PosixFilePermission.OTHERS_READ)
                        || mode.contains(PosixFilePermission.OTHERS_WRITE)
                        || mode.contains(PosixFilePermission.OTHERS_EXECUTE)) {
                    throw new SecretFailure(SecretFailure.Code.DENIED);
                }
            } catch (UnsupportedOperationException ignored) {
                // No portable owner-only permission proof: fail closed.
                throw new SecretFailure(SecretFailure.Code.DENIED);
            }
            try (SeekableByteChannel channel = Files.newByteChannel(candidate,
                    Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS));
                 InputStream stream = Channels.newInputStream(channel)) {
                if (channel.size() > SecretValue.MAX_BYTES) {
                    throw new SecretFailure(SecretFailure.Code.DENIED);
                }
                byte[] bytes = stream.readNBytes(SecretValue.MAX_BYTES + 1);
                if (bytes.length > SecretValue.MAX_BYTES) throw new SecretFailure(SecretFailure.Code.DENIED);
                return bytes;
            }
        } catch (SecretFailure failure) {
            throw failure;
        } catch (IOException failure) {
            throw new SecretFailure(SecretFailure.Code.UNAVAILABLE);
        }
    }

    public static final class Builder {
        private final Path root;
        private final Function<String, String> environment;
        private final Map<SecretReference, Binding> bindings = new HashMap<>();

        private Builder(Path fileRoot, Function<String, String> environment) {
            this.environment = java.util.Objects.requireNonNull(environment);
            if (fileRoot == null) {
                this.root = null;
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
            return new BoundSecretProvider(root, bindings, environment);
        }
    }
}
