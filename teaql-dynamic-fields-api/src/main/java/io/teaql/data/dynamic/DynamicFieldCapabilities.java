package io.teaql.data.dynamic;

public final class DynamicFieldCapabilities {

    private final boolean sourceOfTruth;
    private final boolean supportsTransaction;
    private final boolean supportsBatchLoad;
    private final boolean supportsTypedValue;
    private final boolean supportsBasicPermission;
    private final boolean supportsBasicAudit;

    private DynamicFieldCapabilities(Builder builder) {
        this.sourceOfTruth = builder.sourceOfTruth;
        this.supportsTransaction = builder.supportsTransaction;
        this.supportsBatchLoad = builder.supportsBatchLoad;
        this.supportsTypedValue = builder.supportsTypedValue;
        this.supportsBasicPermission = builder.supportsBasicPermission;
        this.supportsBasicAudit = builder.supportsBasicAudit;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean sourceOfTruth() { return sourceOfTruth; }
    public boolean supportsTransaction() { return supportsTransaction; }
    public boolean supportsBatchLoad() { return supportsBatchLoad; }
    public boolean supportsTypedValue() { return supportsTypedValue; }
    public boolean supportsBasicPermission() { return supportsBasicPermission; }
    public boolean supportsBasicAudit() { return supportsBasicAudit; }

    public static final class Builder {
        private boolean sourceOfTruth;
        private boolean supportsTransaction;
        private boolean supportsBatchLoad;
        private boolean supportsTypedValue;
        private boolean supportsBasicPermission;
        private boolean supportsBasicAudit;

        private Builder() {}

        public Builder sourceOfTruth(boolean value) { this.sourceOfTruth = value; return this; }
        public Builder supportsTransaction(boolean value) { this.supportsTransaction = value; return this; }
        public Builder supportsBatchLoad(boolean value) { this.supportsBatchLoad = value; return this; }
        public Builder supportsTypedValue(boolean value) { this.supportsTypedValue = value; return this; }
        public Builder supportsBasicPermission(boolean value) { this.supportsBasicPermission = value; return this; }
        public Builder supportsBasicAudit(boolean value) { this.supportsBasicAudit = value; return this; }

        public DynamicFieldCapabilities build() {
            return new DynamicFieldCapabilities(this);
        }
    }
}
