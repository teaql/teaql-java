package io.teaql.data.dynamic;

public class DynamicFieldDef {

    private long id;
    private DynamicFieldScope scope;
    private String ownerType;
    private String code;
    private String name;
    private String description;
    private DynamicDataType dataType;
    private DynamicLogicalType logicalType;
    private boolean required;
    private boolean visible = true;
    private boolean editable = true;
    private boolean filterable;
    private boolean sortable;
    private boolean searchable;
    private boolean exportable;
    private boolean importable;
    private boolean auditable = true;
    private String privacyLevel;
    private String maskRule;
    private String defaultValue;
    private DynamicFieldStatus status;
    private int displayOrder;
    private long version;
    private String createdBy;
    private String updatedBy;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public DynamicFieldScope getScope() { return scope; }
    public void setScope(DynamicFieldScope scope) { this.scope = scope; }

    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public DynamicDataType getDataType() { return dataType; }
    public void setDataType(DynamicDataType dataType) { this.dataType = dataType; }

    public DynamicLogicalType getLogicalType() { return logicalType; }
    public void setLogicalType(DynamicLogicalType logicalType) { this.logicalType = logicalType; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }

    public boolean isFilterable() { return filterable; }
    public void setFilterable(boolean filterable) { this.filterable = filterable; }

    public boolean isSortable() { return sortable; }
    public void setSortable(boolean sortable) { this.sortable = sortable; }

    public boolean isSearchable() { return searchable; }
    public void setSearchable(boolean searchable) { this.searchable = searchable; }

    public boolean isExportable() { return exportable; }
    public void setExportable(boolean exportable) { this.exportable = exportable; }

    public boolean isImportable() { return importable; }
    public void setImportable(boolean importable) { this.importable = importable; }

    public boolean isAuditable() { return auditable; }
    public void setAuditable(boolean auditable) { this.auditable = auditable; }

    public String getPrivacyLevel() { return privacyLevel; }
    public void setPrivacyLevel(String privacyLevel) { this.privacyLevel = privacyLevel; }

    public String getMaskRule() { return maskRule; }
    public void setMaskRule(String maskRule) { this.maskRule = maskRule; }

    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    public DynamicFieldStatus getStatus() { return status; }
    public void setStatus(DynamicFieldStatus status) { this.status = status; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public boolean isActive() {
        return status == DynamicFieldStatus.ACTIVE;
    }
}
