package io.teaql.core.graph;

import java.util.*;
import java.util.stream.Collectors;

import io.teaql.core.Entity;
import io.teaql.core.Repository;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.Relation;

public class GraphMutationEngine {
    
    public static GraphMutationPlan planGraph(UserContext userContext, Entity entity) {
        GraphNode root = graphNodeFromEntity(userContext, entity);
        GraphMutationPlan plan = new GraphMutationPlan();
        collectGraphPlan(userContext, root, plan, null, null, false, new java.util.IdentityHashMap<>());
        plan.setPlannedRoot(root);
        plan.rebuildBatches();
        return plan;
    }
    
    public static GraphNode graphNodeFromEntity(UserContext userContext, Entity entity) {
        return graphNodeFromEntity(userContext, entity, new java.util.IdentityHashMap<>());
    }

    private static GraphNode graphNodeFromEntity(UserContext userContext, Entity entity, java.util.Map<Entity, GraphNode> visited) {
        if (entity == null) return null;
        if (visited.containsKey(entity)) return visited.get(entity);
        
        String typeName = entity.typeName();
        EntityDescriptor descriptor = userContext.resolveEntityDescriptor(typeName);
        GraphNode node = new GraphNode();
        visited.put(entity, node);
        node.setEntity(typeName);
        
        if (entity.getComment() != null) {
            node.setComment(entity.getComment());
        }
        
        if (entity instanceof io.teaql.core.BaseEntity && io.teaql.core.EntityStatus.REFER.equals(((io.teaql.core.BaseEntity)entity).get$status())) {
            node.setOperation(GraphOperation.REFERENCE);
        } else if (entity.deleteItem()) {
            node.setOperation(GraphOperation.REMOVE);
        } else if (entity.newItem()) {
            node.setOperation(GraphOperation.CREATE);
        } else {
            node.setOperation(GraphOperation.UPSERT);
        }
        
        Set<String> dirty = new HashSet<>();
        List<String> updated = entity.getUpdatedProperties();
        if (updated != null) {
            dirty.addAll(updated);
        }
        node.setDirtyFields(dirty);
        
        // Extract values and relations
        EntityDescriptor currDesc = descriptor;
        while (currDesc != null) {
            for (PropertyDescriptor prop : currDesc.getProperties()) {
                String propName = prop.getName();
                Object val = entity.getProperty(propName);
                if (prop instanceof Relation) {
                    Relation rel = (Relation) prop;
                    if (val instanceof Entity) {
                        GraphNode childNode = graphNodeFromEntity(userContext, (Entity) val, visited);
                        if (childNode != null) {
                            node.addRelation(propName, childNode);
                        }
                    } else if (val instanceof Iterable) {
                        for (Object child : (Iterable<?>) val) {
                            if (child instanceof Entity) {
                                GraphNode childNode = graphNodeFromEntity(userContext, (Entity) child, visited);
                                if (childNode != null) {
                                    node.addRelation(propName, childNode);
                                }
                            }
                        }
                    }
                    // Keep the relation value so the reconstructed entity has it
                    node.getValues().put(propName, val);
                } else {
                    node.getValues().put(propName, val);
                }
            }
            currDesc = currDesc.getParent();
        }
        
        return node;
    }
    
    private static void collectGraphPlan(UserContext userContext, GraphNode node, GraphMutationPlan plan, 
                                         TraceNode parentScope, TraceScopeToken parentToken, boolean parentIsCreate, java.util.Map<GraphNode, Boolean> visited) {
        if (node == null) return;
        if (visited.containsKey(node)) return;
        visited.put(node, true);
        
        if (node.getOperation() == GraphOperation.REFERENCE) {
            plan.push(node.getEntity(), GraphMutationKind.REFERENCE, node.getValues(), new ArrayList<>(), parentToken, node.getOriginalValues());
            return;
        } else if (node.getOperation() == GraphOperation.REMOVE) {
            plan.push(node.getEntity(), GraphMutationKind.DELETE, node.getValues(), new ArrayList<>(), parentToken, node.getOriginalValues());
            return;
        }
        
        EntityDescriptor descriptor = userContext.resolveEntityDescriptor(node.getEntity());
        
        TraceScopeToken currentToken = parentToken;
        Long entityId = null;
        Object idVal = node.getId();
        if (idVal instanceof Long) {
            entityId = (Long) idVal;
        } else if (idVal instanceof Integer) {
            entityId = ((Integer) idVal).longValue();
        }
        String comment = node.getComment() != null ? node.getComment() : node.getEntity() + " mutation";
        TraceNode track = new TraceNode(node.getEntity(), entityId, comment);
        currentToken = new TraceScopeToken(parentToken, track, plan.getNextItemIndex());
        
        boolean isCreate = node.getOperation() == GraphOperation.CREATE || (parentIsCreate && node.getOperation() == GraphOperation.UPSERT);
        
        List<String> updateFields = new ArrayList<>();
        if (!isCreate) {
            updateFields = new ArrayList<>(node.getDirtyFields());
            if (updateFields.isEmpty()) {
                // if nothing dirty and it's UPSERT, maybe we can skip? 
                // for now we'll allow empty update
            }
        }
        
        plan.push(node.getEntity(), isCreate ? GraphMutationKind.CREATE : GraphMutationKind.UPDATE, 
                  node.getValues(), updateFields, currentToken, node.getOriginalValues());
                  
        // Traverse relations
        EntityDescriptor currDesc = descriptor;
        while (currDesc != null) {
            for (Relation relation : currDesc.getOwnRelations()) {
                List<GraphNode> children = node.getRelations().get(relation.getName());
                if (children != null) {
                    for (GraphNode child : children) {
                        collectGraphPlan(userContext, child, plan, null, currentToken, isCreate, visited);
                    }
                }
            }
            for (Relation relation : currDesc.getForeignRelations()) {
                List<GraphNode> children = node.getRelations().get(relation.getName());
                if (children != null) {
                    for (GraphNode child : children) {
                        collectGraphPlan(userContext, child, plan, null, currentToken, isCreate, visited);
                    }
                }
            }
            currDesc = currDesc.getParent();
        }
    }
    
    public static void executeGraphPlan(UserContext userContext, GraphMutationPlan plan) {
        for (GraphMutationBatch batch : plan.getBatches()) {
            if (batch.getItems().isEmpty()) continue;
            
            String entityType = batch.getEntity();
            Repository<Entity> repository = userContext.resolveRepository(entityType);
            
            switch (batch.getKind()) {
                case CREATE:
                case UPDATE:
                    List<Entity> toSave = new ArrayList<>();
                    for (GraphMutationBatch.Item item : batch.getItems()) {
                        // Reconstruct a lightweight entity just to save
                        Entity e = (Entity) io.teaql.utils.reflect.ReflectUtil.newInstance(
                            userContext.resolveEntityDescriptor(entityType).getTargetType());
                        for (Map.Entry<String, Object> entry : item.getValues().entrySet()) {
                            e.setProperty(entry.getKey(), entry.getValue());
                        }

                        // Output trace chain log
                        String traceStr = "";
                        if (item.getScopeToken() != null) {
                            List<String> chainStr = new ArrayList<>();
                            for (TraceNode tn : item.getScopeToken().recoverTraceChain()) {
                                chainStr.add(tn.getEntityType() + "[" + tn.getEntityId() + "](" + tn.getComment() + ")");
                            }
                            traceStr = String.join(" -> ", chainStr);
                        }
                        e.setTraceChain(traceStr);
                        String actionName = batch.getKind() == GraphMutationKind.CREATE ? "CREATED" : "UPDATED";
                        String entityIdStr = item.getValues().get("id") != null ? item.getValues().get("id").toString() : "null";
                        String entityIdentity = entityType + "(" + entityIdStr + ")";

                        List<String> fieldChanges = new ArrayList<>();
                        for (String field : batch.getUpdateFields()) {
                            Object oldVal = item.getOldValues() != null ? item.getOldValues().get(field) : null;
                            Object newVal = item.getValues().get(field);
                            fieldChanges.add(field + ": [" + oldVal + " ➔ " + newVal + "]");
                        }
                        String fieldsPart = fieldChanges.isEmpty() ? "" : " {" + String.join(",  ", fieldChanges) + "}";

                        String ts = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
                        String user = "system";
                        System.out.println(String.format("[%s]-[%s]-[AUDIT]-Entity [%s] was %s. [%s]%s", 
                            ts, user, entityIdentity, actionName, traceStr, fieldsPart));

                        // In a real execution, we would also attach the trace scope logic to the repository call.
                        // For now we use standard save()
                        if (e instanceof io.teaql.core.BaseEntity) {
                            if (batch.getKind() == GraphMutationKind.CREATE) {
                                ((io.teaql.core.BaseEntity) e).set$status(io.teaql.core.EntityStatus.NEW);
                            } else if (batch.getKind() == GraphMutationKind.UPDATE) {
                                ((io.teaql.core.BaseEntity) e).set$status(io.teaql.core.EntityStatus.UPDATED);
                            }
                        }
                        toSave.add(e);
                    }
                    repository.save(userContext, toSave);
                    break;
                case DELETE:
                    for (GraphMutationBatch.Item item : batch.getItems()) {
                        Entity e = (Entity) io.teaql.utils.reflect.ReflectUtil.newInstance(
                            userContext.resolveEntityDescriptor(entityType).getTargetType());
                        Object id = item.getValues().get("id");
                        if (id instanceof Number) {
                            e.setId(((Number)id).longValue());
                        }
                        repository.delete(userContext, e);
                    }
                    break;
                case REFERENCE:
                    break;
            }
        }
    }
}
