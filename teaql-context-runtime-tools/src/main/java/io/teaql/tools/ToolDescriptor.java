package io.teaql.tools;

public final class ToolDescriptor {
    private final String id;
    private final Class<?> toolType;
    private final ToolRisk risk;
    private final String acknowledgementEnvironmentVariable;
    private final String acknowledgementValue;

    private ToolDescriptor(Builder builder) {
        this.id = builder.id;
        this.toolType = builder.toolType;
        this.risk = builder.risk;
        this.acknowledgementEnvironmentVariable = builder.acknowledgementEnvironmentVariable;
        this.acknowledgementValue = builder.acknowledgementValue;
    }

    public static Builder builder(String id, Class<?> toolType) {
        return new Builder(id, toolType);
    }

    public String getId() {
        return id;
    }

    public Class<?> getToolType() {
        return toolType;
    }

    public ToolRisk getRisk() {
        return risk;
    }

    public String getAcknowledgementEnvironmentVariable() {
        return acknowledgementEnvironmentVariable;
    }

    public String getAcknowledgementValue() {
        return acknowledgementValue;
    }

    public boolean requiresAcknowledgement() {
        return acknowledgementEnvironmentVariable != null && acknowledgementValue != null;
    }

    public static final class Builder {
        private final String id;
        private final Class<?> toolType;
        private ToolRisk risk = ToolRisk.MEMORY_ONLY;
        private String acknowledgementEnvironmentVariable;
        private String acknowledgementValue;

        private Builder(String id, Class<?> toolType) {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Tool id must not be blank.");
            }
            if (toolType == null) {
                throw new IllegalArgumentException("Tool type must not be null.");
            }
            this.id = id;
            this.toolType = toolType;
        }

        public Builder risk(ToolRisk risk) {
            if (risk == null) {
                throw new IllegalArgumentException("Tool risk must not be null.");
            }
            this.risk = risk;
            return this;
        }

        public Builder acknowledgement(String environmentVariable, String value) {
            if (environmentVariable == null || environmentVariable.trim().isEmpty()) {
                throw new IllegalArgumentException("Acknowledgement environment variable must not be blank.");
            }
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("Acknowledgement value must not be blank.");
            }
            this.acknowledgementEnvironmentVariable = environmentVariable;
            this.acknowledgementValue = value;
            return this;
        }

        public ToolDescriptor build() {
            return new ToolDescriptor(this);
        }
    }
}
