package io.teaql.tools;

import io.teaql.core.UserContext;

import java.util.HashSet;
import java.util.Set;

public interface ToolPolicy {

    boolean isAllowed(ToolDescriptor descriptor, UserContext ctx);

    static ToolPolicy allowStandardTools() {
        return (descriptor, ctx) -> descriptor.getRisk() == ToolRisk.MEMORY_ONLY;
    }

    static ToolPolicy denyAll() {
        return (descriptor, ctx) -> false;
    }

    static Builder builder() {
        return new Builder();
    }

    final class Builder {
        private final Set<Class<?>> allowedTypes = new HashSet<>();
        private final Set<Class<?>> deniedTypes = new HashSet<>();
        private boolean allowMemoryOnly = true;
        private boolean allowExternalResource = false;
        private boolean allowPrivileged = false;

        private Builder() {
        }

        public Builder allow(Class<?> toolType) {
            allowedTypes.add(toolType);
            deniedTypes.remove(toolType);
            return this;
        }

        public Builder deny(Class<?> toolType) {
            deniedTypes.add(toolType);
            allowedTypes.remove(toolType);
            return this;
        }

        public Builder allowExternalResources() {
            this.allowExternalResource = true;
            return this;
        }

        public Builder allowPrivileged() {
            this.allowPrivileged = true;
            return this;
        }

        public Builder denyMemoryOnlyByDefault() {
            this.allowMemoryOnly = false;
            return this;
        }

        public ToolPolicy build() {
            return (descriptor, ctx) -> {
                Class<?> toolType = descriptor.getToolType();
                if (deniedTypes.contains(toolType)) {
                    return false;
                }
                if (allowedTypes.contains(toolType)) {
                    return true;
                }
                if (descriptor.getRisk() == ToolRisk.MEMORY_ONLY) {
                    return allowMemoryOnly;
                }
                if (descriptor.getRisk() == ToolRisk.EXTERNAL_RESOURCE) {
                    return allowExternalResource;
                }
                return allowPrivileged;
            };
        }
    }
}
