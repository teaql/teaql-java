package io.teaql.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import io.teaql.core.utils.CollStreamUtil;
import io.teaql.core.utils.ArrayUtil;
import io.teaql.core.utils.ObjectUtil;

import io.teaql.core.AggregationResult;
import io.teaql.core.Entity;
import io.teaql.core.Repository;
import io.teaql.core.SearchRequest;
import io.teaql.core.SmartList;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.Relation;

public class RepositoryAdaptor {

    public static <T extends Entity> void saveGraph(UserContext userContext, Object items) {
        if (ObjectUtil.isEmpty(items)) {
            return;
        }
        
        List<Entity> topLevelEntities = new ArrayList<>();
        extractTopLevelEntities(items, topLevelEntities);
        
        for (Entity entity : topLevelEntities) {
            io.teaql.core.graph.GraphMutationPlan plan = io.teaql.core.graph.GraphMutationEngine.planGraph(userContext, entity);
            io.teaql.core.graph.GraphMutationEngine.executeGraphPlan(userContext, plan);
        }
    }

    private static void extractTopLevelEntities(Object item, List<Entity> topLevelEntities) {
        if (item == null) return;
        if (item instanceof Entity) {
            topLevelEntities.add((Entity) item);
        } else if (item instanceof Iterable) {
            for (Object obj : (Iterable<?>) item) {
                extractTopLevelEntities(obj, topLevelEntities);
            }
        } else if (ArrayUtil.isArray(item)) {
            int length = ArrayUtil.length(item);
            for (int i = 0; i < length; i++) {
                extractTopLevelEntities(ArrayUtil.get(item, i), topLevelEntities);
            }
        }
    }

    private static void saveType(
            UserContext userContext, String type, Map<String, List<Entity>> entities) {
        // save referenced types first
        EntityDescriptor entityDescriptor = userContext.resolveEntityDescriptor(type);

        while (entityDescriptor != null) {
            List<Relation> ownRelations = entityDescriptor.getOwnRelations();
            for (Relation ownRelation : ownRelations) {
                EntityDescriptor owner = ownRelation.getReverseProperty().getOwner();
                String referType = owner.getType();
                saveType(userContext, referType, entities);
            }
            entityDescriptor = entityDescriptor.getParent();
        }

        // save this type-self
        List<Entity> list = entities.remove(type);
        if (ObjectUtil.isEmpty(list)) {
            return;
        }
        Repository<Entity> repository = userContext.resolveRepository(type);
        Collection<Entity> saveResult = repository.save(userContext, list);
        Map<Long, Entity> entityMap = CollStreamUtil.toIdentityMap(list, Entity::getId);
        for (Entity entity : saveResult) {
            Entity input = entityMap.get(entity.getId());
            if (input == entity) {
                continue;
            }
            copyProperties(entity, input);
        }
    }

    private static void copyProperties(Entity src, Entity dest) {
    }

    private static void collect(
            UserContext userContext, Map<String, List<Entity>> entities, Object item, List handled) {
        if (item == null) {
            return;
        }
        for (Object o : handled) {
            if (item == o) {
                return;
            }
        }
        handled.add(item);
        if (item instanceof Entity) {
            Entity entity = (Entity) item;
            appendEntity(userContext, entities, entity, handled);
        }
        else if (item instanceof Iterable) {
            for (Object entity : (Iterable) item) {
                collect(userContext, entities, entity, handled);
            }
        }
        else if (ArrayUtil.isArray(item)) {
            int length = ArrayUtil.length(item);
            for (int i = 0; i < length; i++) {
                Object o = ArrayUtil.get(item, i);
                collect(userContext, entities, o, handled);
            }
        }
        else if (item instanceof Iterator) {
            while (((Iterator) item).hasNext()) {
                collect(userContext, entities, ((Iterator<?>) item).next(), handled);
            }
        }
        else if (item instanceof Map) {
            Map m = (Map) item;
            m.forEach(
                    (k, v) -> {
                        collect(userContext, entities, k, handled);
                        collect(userContext, entities, v, handled);
                    });
        }
    }

    private static void appendEntity(
            UserContext userContext, Map<String, List<Entity>> entities, Entity entity, List pHandled) {
        userContext.info("RepositoryAdaptor.appendEntity: entity hash=" + System.identityHashCode(entity));
        if (entity == null) {
            return;
        }

        String typeName = entity.typeName();
        List<Entity> list = entities.get(typeName);
        if (list == null) {
            list = new ArrayList<>();
            entities.put(typeName, list);
        }
        if (entity.needPersist()) {
            list.add(entity);
        }

        EntityDescriptor entityDescriptor = userContext.resolveEntityDescriptor(typeName);
        while (entityDescriptor != null) {
            List<PropertyDescriptor> properties = entityDescriptor.getProperties();
            for (PropertyDescriptor property : properties) {
                if (property instanceof Relation r) {
                    EntityDescriptor relationKeeper = r.getRelationKeeper();
                    if (!relationKeeper.hasRepository()) {
                        continue;
                    }
                }
                String name = property.getName();
                Object propertyValue = entity.getProperty(name);
                collect(userContext, entities, propertyValue, pHandled);
            }
            entityDescriptor = entityDescriptor.getParent();
        }
    }

    public static <T extends Entity> void delete(UserContext userContext, T entity) {
        Repository repository = userContext.resolveRepository(entity.typeName());
        repository.delete(userContext, entity);
    }

    public static <T extends Entity> T executeForOne(UserContext userContext, SearchRequest<T> request) {
        Repository<T> repository = userContext.resolveRepository(request.getTypeName());
        return repository.executeForOne(userContext, request);
    }

    public static <T extends Entity> SmartList<T> executeForList(
            UserContext userContext, SearchRequest<T> request) {
        Repository<T> repository = userContext.resolveRepository(request.getTypeName());
        return repository.executeForList(userContext, request);
    }

    public static <T extends Entity> AggregationResult aggregation(
            UserContext userContext, SearchRequest<T> request) {
        Repository<T> repository = userContext.resolveRepository(request.getTypeName());
        return repository.aggregation(userContext, request);
    }

    public static <T extends Entity> Stream<T> executeForStream(
            UserContext userContext, SearchRequest request) {
        Repository<T> repository = userContext.resolveRepository(request.getTypeName());
        return repository.executeForStream(userContext, request);
    }

    public static <T extends Entity> Stream<T> executeForStream(
            UserContext userContext, SearchRequest request, int enhanceBatch) {
        Repository<T> repository = userContext.resolveRepository(request.getTypeName());
        return repository.executeForStream(userContext, request, enhanceBatch);
    }
}
