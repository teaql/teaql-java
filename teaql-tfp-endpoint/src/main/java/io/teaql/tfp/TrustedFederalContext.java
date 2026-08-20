package io.teaql.tfp;

import java.util.Map;
import java.util.Set;

/** Server-owned TFP policy. Never deserialize this type from a TFP payload. */
public final class TrustedFederalContext {
    private final String tenantField;
    private final Object tenantId;
    private final String authenticatedUser;
    private final String approvedPurpose;
    private final Set<String> allowedEntities;
    private final Map<String, Map<String, String>> readableFields;
    private final Map<String, Map<String, String>> writableFields;
    private final Map<String, Set<String>> allowedActions;
    private final int maxPageSize;

    public TrustedFederalContext(String tenantField, Object tenantId,
            String authenticatedUser, String approvedPurpose,
            Set<String> allowedEntities,
            Map<String, Map<String, String>> readableFields,
            Map<String, Map<String, String>> writableFields,
            Map<String, Set<String>> allowedActions, int maxPageSize) {
        this.tenantField = tenantField;
        this.tenantId = tenantId;
        this.authenticatedUser = authenticatedUser;
        this.approvedPurpose = approvedPurpose;
        this.allowedEntities = Set.copyOf(allowedEntities);
        this.readableFields = Map.copyOf(readableFields);
        this.writableFields = Map.copyOf(writableFields);
        this.allowedActions = Map.copyOf(allowedActions);
        this.maxPageSize = maxPageSize;
    }

    public String tenantField() { return tenantField; }
    public Object tenantId() { return tenantId; }
    public String authenticatedUser() { return authenticatedUser; }
    public String approvedPurpose() { return approvedPurpose; }
    public Set<String> allowedEntities() { return allowedEntities; }
    public Map<String, String> readableFields(String entity) { return readableFields.get(entity); }
    public Map<String, String> writableFields(String entity) { return writableFields.get(entity); }
    public Set<String> allowedActions(String entity) { return allowedActions.get(entity); }
    public int maxPageSize() { return maxPageSize; }
}
