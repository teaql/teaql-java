package io.teaql.core.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GraphMutationBatch {
    private String entity;
    private GraphMutationKind kind;
    private List<Item> items = new ArrayList<>();
    private List<String> updateFields = new ArrayList<>();

    public GraphMutationBatch(String entity, GraphMutationKind kind) {
        this.entity = entity;
        this.kind = kind;
    }

    public String getEntity() {
        return entity;
    }

    public GraphMutationKind getKind() {
        return kind;
    }

    public List<Item> getItems() {
        return items;
    }

    public List<String> getUpdateFields() {
        return updateFields;
    }

    public void setUpdateFields(List<String> updateFields) {
        this.updateFields = updateFields;
    }

    public void addItem(Map<String, Object> values, TraceScopeToken scopeToken, Map<String, Object> oldValues) {
        this.items.add(new Item(values, scopeToken, oldValues));
    }

    public static class Item {
        private final Map<String, Object> values;
        private final TraceScopeToken scopeToken;
        private final Map<String, Object> oldValues;

        public Item(Map<String, Object> values, TraceScopeToken scopeToken, Map<String, Object> oldValues) {
            this.values = values;
            this.scopeToken = scopeToken;
            this.oldValues = oldValues;
        }

        public Map<String, Object> getValues() {
            return values;
        }

        public TraceScopeToken getScopeToken() {
            return scopeToken;
        }

        public Map<String, Object> getOldValues() {
            return oldValues;
        }
    }
}
