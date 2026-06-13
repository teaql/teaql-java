package io.teaql.core.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GraphMutationPlan {
    private List<GraphMutationBatch> batches = new ArrayList<>();
    private GraphNode plannedRoot;
    private int nextItemIndex = 0;

    public GraphMutationPlan() {}

    public List<GraphMutationBatch> getBatches() {
        return batches;
    }

    public GraphNode getPlannedRoot() {
        return plannedRoot;
    }

    public void setPlannedRoot(GraphNode plannedRoot) {
        this.plannedRoot = plannedRoot;
    }

    public int getNextItemIndex() {
        return nextItemIndex;
    }

    public void push(String entity, GraphMutationKind kind, Map<String, Object> values, 
                     List<String> updateFields, TraceScopeToken scopeToken, Map<String, Object> oldValues) {
        if (!batches.isEmpty()) {
            GraphMutationBatch lastBatch = batches.get(batches.size() - 1);
            if (lastBatch.getEntity().equals(entity) && 
                lastBatch.getKind() == kind && 
                Objects.equals(lastBatch.getUpdateFields(), updateFields)) {
                
                lastBatch.addItem(values, scopeToken, oldValues);
                nextItemIndex++;
                return;
            }
        }
        
        GraphMutationBatch newBatch = new GraphMutationBatch(entity, kind);
        newBatch.setUpdateFields(updateFields);
        newBatch.addItem(values, scopeToken, oldValues);
        batches.add(newBatch);
        nextItemIndex++;
    }

    public void rebuildBatches() {
        // Optional optimization: merge adjacent batches of the same entity/kind/fields
        List<GraphMutationBatch> merged = new ArrayList<>();
        for (GraphMutationBatch batch : batches) {
            if (!merged.isEmpty()) {
                GraphMutationBatch last = merged.get(merged.size() - 1);
                if (last.getEntity().equals(batch.getEntity()) && 
                    last.getKind() == batch.getKind() && 
                    Objects.equals(last.getUpdateFields(), batch.getUpdateFields())) {
                    
                    last.getItems().addAll(batch.getItems());
                    continue;
                }
            }
            merged.add(batch);
        }
        this.batches = merged;
    }
}
