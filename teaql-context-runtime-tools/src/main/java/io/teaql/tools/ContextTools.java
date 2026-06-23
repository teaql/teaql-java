package io.teaql.tools;

import io.teaql.core.UserContext;
import io.teaql.tools.impl.DefaultTools;
import io.teaql.tools.spi.ToolProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public final class ContextTools {

    private ContextTools() {
    }

    public static Tools of(UserContext ctx) {
        return builder(ctx).build();
    }

    public static Builder builder(UserContext ctx) {
        return new Builder(ctx);
    }

    public static AgentHttpTool http(UserContext ctx) {
        return of(ctx).get(AgentHttpTool.class);
    }

    public static final class Builder {
        private final UserContext ctx;
        private ToolPolicy policy = ToolPolicy.allowStandardTools();
        private ToolAcknowledgements acknowledgements = ToolAcknowledgements.system();
        private final List<ToolProvider> providers = new ArrayList<>();

        private Builder(UserContext ctx) {
            this.ctx = ctx;
        }

        public Builder policy(ToolPolicy policy) {
            if (policy == null) {
                throw new IllegalArgumentException("Tool policy must not be null.");
            }
            this.policy = policy;
            return this;
        }

        public Builder acknowledgements(ToolAcknowledgements acknowledgements) {
            if (acknowledgements == null) {
                throw new IllegalArgumentException("Tool acknowledgements must not be null.");
            }
            this.acknowledgements = acknowledgements;
            return this;
        }

        public Builder provider(ToolProvider provider) {
            if (provider == null) {
                throw new IllegalArgumentException("Tool provider must not be null.");
            }
            this.providers.add(provider);
            return this;
        }

        public Tools build() {
            List<ToolProvider> allProviders = new ArrayList<>(providers);
            ServiceLoader.load(ToolProvider.class).forEach(allProviders::add);
            return new DefaultTools(ctx, policy, acknowledgements, allProviders);
        }
    }
}
