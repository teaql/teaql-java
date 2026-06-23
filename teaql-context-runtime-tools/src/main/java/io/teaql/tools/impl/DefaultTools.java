package io.teaql.tools.impl;

import io.teaql.core.UserContext;
import io.teaql.tools.ToolAcknowledgements;
import io.teaql.tools.ToolDescriptor;
import io.teaql.tools.ToolPolicy;
import io.teaql.tools.Tools;
import io.teaql.tools.spi.ToolProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultTools implements Tools {
    private final UserContext ctx;
    private final ToolPolicy policy;
    private final ToolAcknowledgements acknowledgements;
    private final Map<Class<?>, ToolProvider> providers;
    private final Set<ToolDescriptor> descriptors;

    public DefaultTools(
            UserContext ctx,
            ToolPolicy policy,
            ToolAcknowledgements acknowledgements,
            List<ToolProvider> providers) {
        this.ctx = ctx;
        this.policy = policy;
        this.acknowledgements = acknowledgements;
        Map<Class<?>, ToolProvider> providerMap = new LinkedHashMap<>();
        List<ToolDescriptor> descriptorList = new ArrayList<>();
        for (ToolProvider provider : providers) {
            ToolDescriptor descriptor = provider.descriptor();
            providerMap.putIfAbsent(descriptor.getToolType(), provider);
            descriptorList.add(descriptor);
        }
        this.providers = Collections.unmodifiableMap(providerMap);
        this.descriptors = Set.copyOf(descriptorList);
    }

    @Override
    public <T> T get(Class<T> toolType) {
        ToolProvider provider = providers.get(toolType);
        if (provider == null) {
            throw new IllegalArgumentException("Tool not available: " + toolType.getName());
        }
        ToolDescriptor descriptor = provider.descriptor();
        if (!policy.isAllowed(descriptor, ctx)) {
            throw new SecurityException("Tool denied by policy: " + descriptor.getId());
        }
        if (!acknowledgements.isAcknowledged(descriptor)) {
            throw new SecurityException(
                    "Tool requires acknowledgement: set "
                            + descriptor.getAcknowledgementEnvironmentVariable()
                            + " to "
                            + descriptor.getAcknowledgementValue());
        }
        return provider.create(toolType, ctx);
    }

    @Override
    public boolean has(Class<?> toolType) {
        return providers.containsKey(toolType);
    }

    @Override
    public Set<ToolDescriptor> descriptors() {
        return descriptors;
    }
}
