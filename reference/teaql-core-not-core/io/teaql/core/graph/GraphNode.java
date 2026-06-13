package io.teaql.core.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphNode {
    private String entity;
    private Map<String, Object> values = new HashMap<>();
    private Map<String, List<GraphNode>> relations = new HashMap<>();
    private GraphOperation operation = GraphOperation.UPSERT;
    private String comment;
    private Set<String> dirtyFields;
    private Map<String, Object> originalValues;

    public GraphNode() {}

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    public Map<String, List<GraphNode>> getRelations() {
        return relations;
    }

    public void setRelations(Map<String, List<GraphNode>> relations) {
        this.relations = relations;
    }

    public void addRelation(String name, GraphNode child) {
        this.relations.computeIfAbsent(name, k -> new ArrayList<>()).add(child);
    }

    public GraphOperation getOperation() {
        return operation;
    }

    public void setOperation(GraphOperation operation) {
        this.operation = operation;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Set<String> getDirtyFields() {
        return dirtyFields;
    }

    public void setDirtyFields(Set<String> dirtyFields) {
        this.dirtyFields = dirtyFields;
    }

    public Map<String, Object> getOriginalValues() {
        return originalValues;
    }

    public void setOriginalValues(Map<String, Object> originalValues) {
        this.originalValues = originalValues;
    }

    public Object getId() {
        return values.get("id"); // assuming "id" is the standard pk name
    }
}
