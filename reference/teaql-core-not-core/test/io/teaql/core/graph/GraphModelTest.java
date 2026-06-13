package io.teaql.core.graph;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;

public class GraphModelTest {

    @Test
    public void testTraceScopeTokenRecovery() {
        TraceNode rootNode = new TraceNode("Task", 1L, "Create task");
        TraceScopeToken rootToken = new TraceScopeToken(null, rootNode, 0);

        TraceNode childNode = new TraceNode("TaskExecutionLog", null, "Generate log for task");
        TraceScopeToken childToken = new TraceScopeToken(rootToken, childNode, 1);

        List<TraceNode> chain = childToken.recoverTraceChain();

        assertEquals(2, chain.size());
        assertEquals("Task", chain.get(0).getEntityType());
        assertEquals(Long.valueOf(1L), chain.get(0).getEntityId());
        assertEquals("Create task", chain.get(0).getComment());

        assertEquals("TaskExecutionLog", chain.get(1).getEntityType());
        assertNull(chain.get(1).getEntityId());
        assertEquals("Generate log for task", chain.get(1).getComment());
    }

    @Test
    public void testGraphMutationPlanMerging() {
        GraphMutationPlan plan = new GraphMutationPlan();

        // Push first item
        plan.push("Task", GraphMutationKind.CREATE, new HashMap<>(), new java.util.ArrayList<>(), null, null);
        assertEquals(1, plan.getBatches().size());
        assertEquals(1, plan.getBatches().get(0).getItems().size());
        assertEquals(1, plan.getNextItemIndex());

        // Push second item of same kind
        plan.push("Task", GraphMutationKind.CREATE, new HashMap<>(), new java.util.ArrayList<>(), null, null);
        assertEquals(1, plan.getBatches().size()); // Merged into the same batch
        assertEquals(2, plan.getBatches().get(0).getItems().size());
        assertEquals(2, plan.getNextItemIndex());

        // Push item of different kind
        plan.push("Task", GraphMutationKind.UPDATE, new HashMap<>(), new java.util.ArrayList<>(), null, null);
        assertEquals(2, plan.getBatches().size());
        assertEquals(1, plan.getBatches().get(1).getItems().size());
        assertEquals(3, plan.getNextItemIndex());

        // Rebuild test
        plan.push("Task", GraphMutationKind.UPDATE, new HashMap<>(), new java.util.ArrayList<>(), null, null);
        assertEquals(2, plan.getBatches().size()); // The push automatically merged it
        
        // Let's create an artificial unmerged scenario by forcing a new batch manually
        GraphMutationBatch extraBatch = new GraphMutationBatch("Task", GraphMutationKind.UPDATE);
        plan.getBatches().add(extraBatch);
        assertEquals(3, plan.getBatches().size());
        
        plan.rebuildBatches();
        assertEquals(2, plan.getBatches().size()); // Should merge the last two UPDATE batches
    }
}
