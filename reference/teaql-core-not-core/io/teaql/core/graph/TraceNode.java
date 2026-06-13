package io.teaql.core.graph;

public class TraceNode {
    private String entityType;
    private Long entityId;
    private String comment;

    public TraceNode() {}

    public TraceNode(String entityType, Long entityId, String comment) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.comment = comment;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
