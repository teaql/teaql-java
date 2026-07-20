package io.teaql.tools;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface ToolAcknowledgements {

    boolean isAcknowledged(ToolDescriptor descriptor);

    static ToolAcknowledgements system() {
        return new EnvironmentToolAcknowledgements();
    }

    static ToolAcknowledgements none() {
        return descriptor -> !descriptor.requiresAcknowledgement();
    }

    static ToolAcknowledgements from(Map<String, String> values) {
        Map<String, String> copy = values == null ? Collections.emptyMap() : new HashMap<>(values);
        return descriptor -> {
            if (!descriptor.requiresAcknowledgement()) {
                return true;
            }
            String value = copy.get(descriptor.getAcknowledgementEnvironmentVariable());
            return descriptor.getAcknowledgementValue().equals(value);
        };
    }

    final class EnvironmentToolAcknowledgements implements ToolAcknowledgements {
        private final Map<String, String> values;

        private EnvironmentToolAcknowledgements() {
            Map<String, String> collected = new HashMap<>();
            for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (key.startsWith("TEAQL_")) {
                    collected.put(key, String.valueOf(entry.getValue()));
                }
            }
            for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
                if (entry.getKey().startsWith("TEAQL_") && !collected.containsKey(entry.getKey())) {
                    collected.put(entry.getKey(), entry.getValue());
                }
            }
            this.values = Collections.unmodifiableMap(collected);
        }

        @Override
        public boolean isAcknowledged(ToolDescriptor descriptor) {
            if (!descriptor.requiresAcknowledgement()) {
                return true;
            }
            String value = values.get(descriptor.getAcknowledgementEnvironmentVariable());
            return descriptor.getAcknowledgementValue().equals(value);
        }
    }
}
