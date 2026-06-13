package io.teaql.core.web;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.teaql.core.utils.CollStreamUtil;
import io.teaql.core.utils.MapUtil;
import io.teaql.core.utils.ClassUtil;
import io.teaql.core.utils.ObjectUtil;
import io.teaql.core.utils.ReflectUtil;
import io.teaql.core.utils.StrUtil;

import io.teaql.core.UserContext;
import io.teaql.core.DefaultUserContext;
import io.teaql.core.BaseRequest;
import io.teaql.core.BaseEntity;
import io.teaql.core.Entity;
import io.teaql.core.EntityStatus;
import io.teaql.core.SmartList;
import io.teaql.core.TQLException;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.Relation;
import io.teaql.core.translation.TranslationRecord;
import io.teaql.core.translation.TranslationRequest;
import io.teaql.core.translation.TranslationResponse;

/**
 * a generic service implementation for entity CRUD operations
 *
 * @author jackytian
 */
public abstract class BaseService {

    public static final int MAX_VERSION = Integer.MAX_VALUE - 10000;

    /**
     * the main service entrance
     *
     * @param ctx       user context
     * @param action    action name
     * @param parameter parameter, json format
     * @return webResponse
     */
    public final WebResponse execute(
            UserContext ctx, String beanName, String action, String parameter) {
        if (ObjectUtil.isEmpty(beanName)) {
            return WebResponse.fail("missing beanName");
        }
        if (ObjectUtil.isEmpty(action)) {
            return WebResponse.fail(String.format("missing action from bean: %s", beanName));
        }
        Method method =
                ReflectUtil.getPublicMethod(this.getClass(), action, UserContext.class, String.class);
        if (method != null) {
            return ReflectUtil.invoke(this, method, ctx, parameter);
        }
        if (action.startsWith("search")) {
            return doDynamicSearch(ctx, beanName, action, parameter);
        }
        if (action.startsWith("save")) {
            return doSave(ctx, action, parameter);
        }
        if (action.startsWith("delete")) {
            return doDelete(ctx, action, parameter);
        }
        if (action.startsWith("list") && action.endsWith("ForCandidate")) {
            return doCandidate(ctx, action, parameter);
        }
        return WebResponse.fail(
                StrUtil.format(
                        "unknown action: %s from bean: %s, reference: %s/%s, method shuld declare like method(UserContext ctx, String params)",
                        action, beanName, beanName, action));
    }

    public WebResponse doCandidate(UserContext ctx, String action, String parameter) {
        String type = StrUtil.removePrefix(action, "list");
        type = StrUtil.removeSuffix(type, "ForCandidate");
        Class<? extends BaseRequest> requestClass = requestClass(type);
        Class<? extends BaseEntity> entityClass = getEntityClass(type);
        BaseRequest baseRequest = ((DefaultUserContext) ctx).initRequest(entityClass);
        baseRequest.selectAll();
        if (ObjectUtil.isNotEmpty(parameter)) {
            baseRequest.internalFindWithJsonExpr(parameter);
        }
        return WebResponse.of(baseRequest.executeForList(ctx));
    }

    public WebResponse doDelete(UserContext ctx, String action, String parameter) {
        String type = StrUtil.removePrefix(action, "delete");
        Entity entity = reloadEntity(ctx, type, parameter);
        validateEntityForDelete(ctx, entity);
        if (entity == null) {
            return WebResponse.success();
        }
        entity.markAsDeleted();
        beforeDelete(ctx, entity);
        entity.save(ctx);
        return WebResponse.of((BaseEntity) entity);
    }

    private void beforeDelete(UserContext ctx, Entity entity) {
    }

    public void validateEntityForDelete(UserContext ctx, Entity entity) {
    }

    public WebResponse doSave(UserContext ctx, String action, String parameter) {
        String type = StrUtil.removePrefix(action, "save");
        BaseEntity baseEntity = parseEntity(ctx, type, parameter);
        validateEntityForSave(ctx, baseEntity);
        if (baseEntity == null) {
            return WebResponse.success();
        }
        Long id = baseEntity.getId();
        cleanupEntity(ctx, baseEntity);
        if (!baseEntity.needPersist()) {
            return WebResponse.success();
        }

        if (id == null) {
            clearRemovedItemsBeforeCreate(ctx, baseEntity);
            maintainRelationship(ctx, baseEntity);
            baseEntity.save(ctx);
            return WebResponse.of(baseEntity);
        }
        else {
            // load the entity before update
            BaseEntity dbItem = reloadEntity(ctx, type, parameter);
            if (dbItem == null) {
                throw new TQLException(StrUtil.format("item [{}] with id [{}] does not exist", type, id));
            }
            EntityDescriptor entityDescriptor = ctx.resolveEntityDescriptor(type);
            mergeEntity(ctx, entityDescriptor, baseEntity, dbItem);
            beforeSave(ctx, dbItem);
            // save item
            dbItem.save(ctx);
            return WebResponse.of(dbItem);
        }
    }

    public void beforeSave(UserContext ctx, BaseEntity item) {
    }

    public void validateEntityForSave(UserContext ctx, BaseEntity entity) {
    }

    private void maintainRelationship(UserContext ctx, BaseEntity baseEntity) {
        EntityDescriptor entityDescriptor = ctx.resolveEntityDescriptor(baseEntity.typeName());
        List<Relation> foreignRelations = entityDescriptor.getForeignRelations();
        for (Relation foreignRelation : foreignRelations) {
            String name = foreignRelation.getName();
            PropertyDescriptor reverseProperty = foreignRelation.getReverseProperty();
            Boolean attach = MapUtil.getBool(reverseProperty.getAdditionalInfo(), "attach");
            if (attach != null && attach) {
                Object property = baseEntity.getProperty(name);
                if (property instanceof BaseEntity e) {
                    e.cacheRelation(reverseProperty.getName(), baseEntity);
                    maintainRelationship(ctx, e);
                }
                else if (property instanceof SmartList l) {
                    for (Object o : l) {
                        if (o instanceof BaseEntity i) {
                            i.cacheRelation(reverseProperty.getName(), baseEntity);
                            maintainRelationship(ctx, i);
                        }
                    }
                }
            }
        }
    }

    private void clearRemovedItemsBeforeCreate(UserContext ctx, BaseEntity baseEntity) {
        if (baseEntity == null || baseEntity.deleteItem()) {
            return;
        }

        EntityDescriptor entityDescriptor = ctx.resolveEntityDescriptor(baseEntity.typeName());
        List<Relation> ownRelations = entityDescriptor.getOwnRelations();
        for (Relation ownRelation : ownRelations) {
            String name = ownRelation.getName();
            BaseEntity ref = baseEntity.getProperty(name);
            if (ref != null && ref.deleteItem()) {
                baseEntity.setProperty(name, null);
            }
            else {
                clearRemovedItemsBeforeCreate(ctx, ref);
            }
        }

        List<Relation> foreignRelations = entityDescriptor.getForeignRelations();
        for (Relation foreignRelation : foreignRelations) {
            String name = foreignRelation.getName();
            Object property = baseEntity.getProperty(name);
            if (property instanceof BaseEntity) {
                if (((BaseEntity) property).deleteItem()) {
                    baseEntity.setProperty(name, null);
                }
                else {
                    clearRemovedItemsBeforeCreate(ctx, (BaseEntity) property);
                }
            }
            else if (property instanceof SmartList l) {
                Iterator iterator = l.iterator();
                while (iterator.hasNext()) {
                    Object next = iterator.next();
                    if (next instanceof BaseEntity e && e.deleteItem()) {
                        iterator.remove();
                    }
                    else {
                        clearRemovedItemsBeforeCreate(ctx, (BaseEntity) next);
                    }
                }
            }
        }
    }

    private void cleanupEntity(UserContext ctx, BaseEntity baseEntity) {
        if (baseEntity == null) {
            return;
        }

        // to be removed item, mark the status as deleted
        String manipulationOperation = baseEntity.getDynamicProperty(".manipulationOperation");
        if ("REMOVE".equals(manipulationOperation)) {
            baseEntity.set$status(EntityStatus.UPDATED_DELETED);
            return;
        }

        // reference item only
        EntityDescriptor entityDescriptor = ctx.resolveEntityDescriptor(baseEntity.typeName());
        Long version = baseEntity.getVersion();
        if (version != null && version > MAX_VERSION) {
            // reference, keep id only
            baseEntity.set$status(EntityStatus.REFER);
            List<PropertyDescriptor> properties = entityDescriptor.getProperties();
            for (PropertyDescriptor property : properties) {
                if (!property.isId()) {
                    baseEntity.setProperty(property.getName(), null);
                }
            }
            return;
        }

        setContextRelationBeforeSave(ctx, baseEntity.typeName(), baseEntity);

        // for new or update request
        List<Relation> ownRelations = entityDescriptor.getOwnRelations();
        for (Relation ownRelation : ownRelations) {
            String name = ownRelation.getName();
            BaseEntity r = baseEntity.getProperty(name);
            cleanupEntity(ctx, r);
        }

        List<Relation> foreignRelations = entityDescriptor.getForeignRelations();
        for (Relation foreignRelation : foreignRelations) {
            String name = foreignRelation.getName();
            Object v = baseEntity.getProperty(name);
            EntityDescriptor owner = foreignRelation.getReverseProperty().getOwner();
            if (!owner.hasRepository()) {
                continue;
            }
            if (v instanceof BaseEntity) {
                cleanupEntity(ctx, (BaseEntity) v);
            }
            else if (v instanceof SmartList l) {
                for (Object o : l) {
                    cleanupEntity(ctx, (BaseEntity) o);
                }
            }
        }
    }

    private void mergeEntity(
            UserContext ctx, EntityDescriptor entityDescriptor, Entity baseEntity, Entity dbItem) {
        if (baseEntity.deleteItem()) {
            dbItem.markAsDeleted();
            return;
        }

        // try update Simple properties
        List<PropertyDescriptor> ownProperties = entityDescriptor.getOwnProperties();
        for (PropertyDescriptor ownProperty : ownProperties) {
            // id,version is set in the repository
            // createFunction, updateFunction will not be ignored, and called in the checker
            if (ownProperty.isId()
                    || ownProperty.isVersion()
                    || ownProperty.getAdditionalInfo().get("createFunction") != null
                    || ownProperty.getAdditionalInfo().get("updateFunction") != null) {
                continue;
            }
            String name = ownProperty.getName();
            Object property = baseEntity.getProperty(name);
            String updateMethodName = StrUtil.upperFirstAndAddPre(name, "update");
            Method method = ReflectUtil.getMethodByName(dbItem.getClass(), updateMethodName);
            if (method != null) {
                ReflectUtil.invoke(dbItem, method, property);
            }
        }

        // try update attached relationships
        List<Relation> foreignRelations = entityDescriptor.getForeignRelations();
        for (Relation foreignRelation : foreignRelations) {
            String name = foreignRelation.getName();
            PropertyDescriptor reverseProperty = foreignRelation.getReverseProperty();
            Boolean attach = MapUtil.getBool(reverseProperty.getAdditionalInfo(), "attach");
            if (attach != null && attach) {
                SmartList<? extends Entity> children = baseEntity.getProperty(name);
                SmartList<? extends Entity> currentChildren = dbItem.getProperty(name);
                if (ObjectUtil.isEmpty(children)) {
                    if (ObjectUtil.isEmpty(currentChildren)) {
                        continue;
                    }
                    else {
                        for (Entity currentChild : currentChildren) {
                            currentChild.markAsDeleted();
                        }
                    }
                }
                Map<Long, ? extends Entity> identityMap = new HashMap<>();
                if (currentChildren != null) {
                    identityMap = currentChildren.toIdentityMap(Entity::getId);
                }
                for (Entity child : children) {
                    Long childId = child.getId();
                    // for new child
                    if (childId == null) {
                        dbItem.addRelation(name, child);
                        ((BaseEntity) child).cacheRelation(reverseProperty.getName(), dbItem);
                    }
                    else {
                        Entity entity = identityMap.get(childId);
                        if (entity == null && child.newItem()) {
                            dbItem.addRelation(name, child);
                            ((BaseEntity) child).cacheRelation(reverseProperty.getName(), dbItem);
                        }
                        else {
                            mergeEntity(ctx, ctx.resolveEntityDescriptor(entity.typeName()), child, entity);
                        }
                    }
                }
            }
        }
    }

    protected void setContextRelationBeforeSave(UserContext ctx, String type, BaseEntity baseEntity) {
        Relation contextRelation = getContextRelation(ctx, type);
        if (contextRelation != null) {
            Object merchant = ReflectUtil.invoke(ctx, "getMerchant");
            ReflectUtil.invoke(
                    baseEntity, StrUtil.upperFirstAndAddPre(contextRelation.getName(), "update"), merchant);
        }
    }

    /**
     * reload the entity before update, now load all, and select attached lists
     *
     * @param ctx
     * @param type
     * @param parameter
     * @return
     */
    private BaseEntity reloadEntity(UserContext ctx, String type, String parameter) {
        BaseEntity baseEntity = parseEntity(ctx, type, parameter);
        Long id = baseEntity.getId();
        if (id == null) {
            return null;
        }
        Class<BaseRequest> baseRequestClass = requestClass(type);
        BaseRequest baseRequest = ReflectUtil.newInstance(baseRequestClass, getEntityClass(type));
        baseRequest.appendSearchCriteria(
                baseRequest.createBasicSearchCriteria(BaseEntity.ID_PROPERTY, Operator.EQUAL, id));
        baseRequest.appendSearchCriteria(
                baseRequest.createBasicSearchCriteria(
                        BaseEntity.VERSION_PROPERTY, Operator.GREATER_THAN, 0));
        baseRequest.selectAll();
        EntityDescriptor entityDescriptor = ctx.resolveEntityDescriptor(type);
        List<Relation> foreignRelations = entityDescriptor.getForeignRelations();
        for (Relation foreignRelation : foreignRelations) {
            String name = foreignRelation.getName();
            PropertyDescriptor reverseProperty = foreignRelation.getReverseProperty();
            Boolean attach = MapUtil.getBool(reverseProperty.getAdditionalInfo(), "attach");
            if (attach != null && attach) {
                ReflectUtil.invoke(baseRequest, StrUtil.upperFirstAndAddPre(name, "select"));
            }
        }
        return (BaseEntity) baseRequest.executeForOne(ctx);
    }

    public BaseEntity parseEntity(UserContext ctx, String type, String parameter) {
        if (ObjectUtil.isEmpty(parameter)) {
            throw new IllegalArgumentException("missing parameter");
        }
        ctx.resolveEntityDescriptor(type);
        Class<? extends BaseEntity> entityClass = getEntityClass(type);
        ObjectMapper objectMapper = ctx.getBean(ObjectMapper.class);
        try {
            return objectMapper.readValue(parameter, entityClass);
        }
        catch (JsonProcessingException pE) {
            throw new TQLException(pE);
        }
    }

    public WebResponse doDynamicSearch(
            UserContext ctx, String beanName, String action, String parameter) {
        String type = StrUtil.removePrefix(action, "search");
        Class<? extends BaseRequest> requestClass = requestClass(type);
        Class<? extends BaseEntity> entityClass = getEntityClass(type);
        BaseRequest baseRequest = ReflectUtil.newInstance(requestClass, entityClass);
        baseRequest.selectAll();
        baseRequest.count();
        baseRequest.addOrderByDescending(BaseEntity.ID_PROPERTY);
        if (ObjectUtil.isNotEmpty(parameter)) {
            baseRequest.internalFindWithJsonExpr(parameter);
        }
        baseRequest.appendSearchCriteria(
                baseRequest.createBasicSearchCriteria(
                        BaseEntity.VERSION_PROPERTY, Operator.GREATER_THAN, 0));
        addContextRelationFilter(ctx, type, baseRequest);

        EntityDescriptor entityDescriptor = ctx.resolveEntityDescriptor(type);
        List<Relation> foreignRelations = entityDescriptor.getForeignRelations();
        List<String> dynamicProperties = new ArrayList<>();
        for (Relation foreignRelation : foreignRelations) {
            String name = foreignRelation.getName();
            String retName = StrUtil.upperFirstAndAddPre(name, "sizeOf");
            dynamicProperties.add(retName);
            PropertyDescriptor reverseProperty = foreignRelation.getReverseProperty();
            EntityDescriptor owner = reverseProperty.getOwner();
            if (!owner.hasRepository()) {
                continue;
            }

            String subType = owner.getType();
            Class<? extends BaseRequest> subRequestClass = requestClass(subType);
            Class<? extends BaseEntity> subEntityClass = getEntityClass(subType);
            BaseRequest subRequest = ReflectUtil.newInstance(subRequestClass, subEntityClass);
            subRequest.unlimited().count();
            subRequest.setPartitionProperty(reverseProperty.getName());
            baseRequest.addAggregateDynamicProperty(retName, subRequest, true);
            // load attached list
            Boolean attach = MapUtil.getBool(reverseProperty.getAdditionalInfo(), "attach");
            if (attach != null && attach) {
                ReflectUtil.invoke(baseRequest, StrUtil.upperFirstAndAddPre(name, "select"));
            }
        }

        SmartList<? extends BaseEntity> ret = baseRequest.executeForList(ctx);

        WebAction webAction = WebAction.modifyWebAction(saveURL(beanName, type));
        WebAction deleteWebAction = WebAction.deleteWebAction(deleteURL(beanName, type), null);
        for (BaseEntity entity : ret) {
            entity.addAction(webAction);
            Long subCounts = entity.sumDynaPropOfNumberAsLong(dynamicProperties);
            if (subCounts == 0) {
                entity.addAction(deleteWebAction);
            }
        }
        translateResult(ctx, ret);
        return WebResponse.of(ret);
    }

    private void translateResult(UserContext ctx, SmartList<? extends BaseEntity> ret) {
        Set<WebAction> actions = new HashSet<>();
        for (BaseEntity baseEntity : ret) {
            List<Object> actionList = baseEntity.getActionList();
            if (actionList != null) {
                for (Object act : actionList) {
                    if (act instanceof WebAction) {
                        actions.add((WebAction) act);
                    }
                }
            }
        }
        Map<String, WebAction> identityMap = CollStreamUtil.toIdentityMap(actions, WebAction::getKey);
        Set<String> keys = identityMap.keySet();
        Set<TranslationRecord> records = CollStreamUtil.toSet(keys, key -> new TranslationRecord(key));
        TranslationRequest request = new TranslationRequest(records);
        TranslationResponse response = null;
        if (ctx instanceof DefaultUserContext dctx) {
            response = dctx.translate(request);
        }
        if (response == null) {
            return;
        }
        Map<String, String> results = response.getResults();
        results.forEach(
                (k, v) -> {
                    identityMap.get(k).setName(v);
                });
    }

    private void addContextRelationFilter(UserContext ctx, String type, BaseRequest baseRequest) {
        Relation contextRelation = getContextRelation(ctx, type);
        if (contextRelation != null) {
            baseRequest.appendSearchCriteria(
                    baseRequest.createBasicSearchCriteria(
                            contextRelation.getName(),
                            Operator.EQUAL,
                            (Object) ReflectUtil.invoke(ctx, "getMerchant")));
        }
    }

    public Relation getContextRelation(UserContext ctx, String type) {
        EntityDescriptor entityDescriptor = ctx.resolveEntityDescriptor(type);
        while (entityDescriptor != null) {
            List<Relation> ownRelations = entityDescriptor.getOwnRelations();
            for (Relation ownRelation : ownRelations) {
                Boolean context = MapUtil.getBool(ownRelation.getAdditionalInfo(), "context");
                if (context != null && context) {
                    return ownRelation;
                }
            }
            entityDescriptor = entityDescriptor.getParent();
        }
        return null;
    }

    private Class<BaseEntity> getEntityClass(String type) {
        return ClassUtil.loadClass(StrUtil.format("{}.{}.{}", rootPackage(), type.toLowerCase(), type));
    }

    private Class<BaseRequest> requestClass(String type) {
        return ClassUtil.loadClass(
                StrUtil.format("{}.{}.{}Request", rootPackage(), type.toLowerCase(), type));
    }

    private String rootPackage() {
        Class clazz = this.getClass();
        while (clazz != null) {
            if (BaseService.class.equals(clazz.getSuperclass())) {
                return clazz.getPackage().getName();
            }
            clazz = clazz.getSuperclass();
        }
        throw new TQLException("cannot guess the domain package");
    }

    protected String saveURL(String beanName, String objectType) {
        return StrUtil.format("{}/save{}/", beanName, objectType);
    }

    protected String deleteURL(String beanName, String objectType) {
        return StrUtil.format("{}/delete{}/", beanName, objectType);
    }
}
