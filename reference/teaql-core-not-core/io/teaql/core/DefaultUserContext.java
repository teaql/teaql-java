package io.teaql.core;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;

import io.teaql.core.utils.Cache;
import io.teaql.core.utils.CacheUtil;
import io.teaql.core.utils.ListUtil;
import io.teaql.core.utils.OptNullBasicTypeFromObjectGetter;
import io.teaql.core.utils.CallerUtil;
import io.teaql.core.utils.ArrayUtil;
import io.teaql.core.utils.BooleanUtil;
import io.teaql.core.utils.ClassUtil;
import io.teaql.core.utils.ObjectUtil;
import io.teaql.utils.reflect.ReflectUtil;
import io.teaql.core.utils.StrUtil;
import io.teaql.utils.json.JSONUtil;

import io.teaql.core.internal.GLobalResolver;
import io.teaql.core.internal.RepositoryAdaptor;
import io.teaql.core.internal.SimpleChineseViewTranslator;
import io.teaql.core.internal.TempRequest;
import io.teaql.core.checker.CheckException;
import io.teaql.core.checker.CheckResult;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import io.teaql.core.criteria.Operator;
import io.teaql.core.language.BaseLanguageTranslator;
import io.teaql.core.lock.LockService;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.Relation;
import io.teaql.core.translation.TranslationRequest;
import io.teaql.core.translation.TranslationResponse;
import io.teaql.core.translation.Translator;

public class DefaultUserContext
        implements UserContext,
        NaturalLanguageTranslator,
        RequestHolder,
        OptNullBasicTypeFromObjectGetter<String>,
        Translator {



    private static final IntentEnforcementMode INTENT_MODE = resolveIntentMode();

    private static IntentEnforcementMode resolveIntentMode() {
        String env = System.getenv("TEAQL_ENFORCE_INTENT");
        if (env == null) {
            env = System.getProperty("teaql.enforce.intent", "off");
        }
        try {
            return IntentEnforcementMode.valueOf(env.toUpperCase());
        } catch (IllegalArgumentException e) {
            return IntentEnforcementMode.OFF;
        }
    }

    public static final String FQCN = UserContext.class.getName();
    public static final String X_CLASS = "X-Class";
    public static final String REQUEST_HOLDER = "$request:requestHolder";
    public static final String RESPONSE_HOLDER = "$response:responseHolder";
    InternalIdGenerator internalIdGenerator;
    private TQLResolver resolver = GLobalResolver.getGlobalResolver();
    private RequestPolicy requestPolicy;
    private io.teaql.core.log.LogManager logManager;
    private final Cache<String, Object> localStorage = CacheUtil.newTimedCache(0);

    /**
     * Framework support API. Subject to change or internalization.
     */
    public Repository resolveRepository(String type) {
        if (getResolver() != null) {
            Repository repository = getResolver().resolveRepository(type);
            if (repository != null) {
                return repository;
            }
        }
        throw new RepositoryException("Repository for '" + type + "' is not defined.");
    }

    public DataConfigProperties config() {
        if (getResolver() != null) {
            DataConfigProperties bean = getResolver().getBean(DataConfigProperties.class);
            if (bean != null) {
                return bean;
            }
        }
        return new DataConfigProperties();
    }

    /**
     * Framework support API. Subject to change or internalization.
     */
    public EntityDescriptor resolveEntityDescriptor(String type) {
        if (getResolver() != null) {
            EntityDescriptor entityDescriptor = getResolver().resolveEntityDescriptor(type);
            if (entityDescriptor != null) {
                return entityDescriptor;
            }
        }
        throw new RepositoryException("ItemDescriptor for:" + type + " not defined.");
    }

    public void saveGraph(Object items) {
        if (items != null) {
            this.info("UserContext.saveGraph: items hash=" + System.identityHashCode(items));
        }
        RepositoryAdaptor.saveGraph(this, items);
    }

    public <T extends Entity> T executeForOne(SearchRequest<T> searchRequest) {
        return RepositoryAdaptor.executeForOne(this, submitRequest(searchRequest));
    }

    private <T extends Entity> SearchRequest<T> submitRequest(SearchRequest<T> request) {
        normalizeRequest(request);
        if (requestPolicy != null) {
            requestPolicy.enforceSelect(this, request);
        }
        return enforceRequestPolicy(request);
    }

    private <T extends Entity> void normalizeRequest(SearchRequest<T> request) {
        normalizeFacetRequest(request);
    }

    protected <T extends Entity> SearchRequest<T> enforceRequestPolicy(SearchRequest<T> request) {
        if (INTENT_MODE == IntentEnforcementMode.OFF) {
            return request;
        }
        String typeName = request.getTypeName();
        String comment = request.comment();
        String purpose = request.purpose();
        boolean missingComment = ObjectUtil.isEmpty(comment);
        boolean missingPurpose = ObjectUtil.isEmpty(purpose);

        if (!missingComment && !missingPurpose) {
            return request;
        }

        StringBuilder msg = new StringBuilder();
        msg.append("[TRIPLE-INTENT VIOLATION] Query on ").append(typeName).append(" rejected.\n");
        msg.append("Missing: ");
        if (missingComment) msg.append(".comment() ");
        if (missingPurpose) msg.append(".purpose() ");
        msg.append("\n\n");
        msg.append("FIX: Every query must declare both .comment() and .purpose() before execution.\n");
        msg.append("Correct pattern:\n");
        msg.append("  Q.").append(uncapFirst(typeName)).append("s()\n");
        msg.append("      .filterByXxx(...)\n");
        msg.append("      .comment(\"Describe what this query loads\")\n");
        msg.append("      .purpose(\"Describe why this data is needed\")\n");
        msg.append("      .executeForList(ctx);\n");
        msg.append("\n");
        msg.append("Refer to AGENTS.md section 'MANDATORY TRIPLE-INTENT' for full documentation.");

        if (INTENT_MODE == IntentEnforcementMode.STRICT) {
            throw new RepositoryException(msg.toString());
        }
        // WARN mode
        this.warn(msg.toString());
        return request;
    }

    private static String uncapFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private <T extends Entity> void normalizeFacetRequest(SearchRequest<T> request) {
        // check if facet requests are valid
        List<FacetRequest> facetRequests = request.getFacetRequests();
        if (ObjectUtil.isEmpty(facetRequests)) {
            return;
        }

        // find the repository and entity descriptor
        Repository repository = resolveRepository(request.getTypeName());
        EntityDescriptor entityDescriptor = repository.getEntityDescriptor();
        for (FacetRequest facetRequest : facetRequests) {
            String relationName = facetRequest.getRelationName();
            PropertyDescriptor propertyDescriptor = entityDescriptor.findProperty(relationName);
            SearchRequest facetSearchRequest = facetRequest.getRequest();
            // facet requests are relations
            if (propertyDescriptor instanceof Relation) {
                // copy facet request, as we wil edit the request
                // 1. add the current request criteria in the dynamic attribute aggregation request
                // 2. if mergeCriteria = true, we also copy the  request criteria to the facet request
                TempRequest tempRequest = new TempRequest(facetSearchRequest);
                facetRequest.setRequest(tempRequest);
                boolean mergeCriteria = facetRequest.isMergeCriteria();
                if (mergeCriteria) {
                    // copy current request criteria to the facet request
                    tempRequest.appendSearchCriteria(new SubQuerySearchCriteria(BaseEntity.ID_PROPERTY, copyCriteriaOnly(request), relationName));
                }
                // real aggregations, we will append criteria
                List<SimpleAggregation> dynamicAggregateAttributes = tempRequest.getDynamicAggregateAttributes();
                dynamicAggregateAttributes.forEach(
                        (dynamicAggregate) -> {
                            SearchRequest aggregateRequest = dynamicAggregate.getAggregateRequest();
                            // only append criteria if the sub request is for the same type
                            // TODO: now it works, but needs it remove duplicate version criteria?
                            if (aggregateRequest.getTypeName().equals(request.getTypeName())) {
                                aggregateRequest.appendSearchCriteria(request.getSearchCriteria());
                            }
                        }
                );
            }
        }
    }

    private <T extends Entity> SearchRequest copyCriteriaOnly(SearchRequest request) {
        SearchRequest requestCopy = new TempRequest(request.returnType(), request.getTypeName());
        requestCopy.appendSearchCriteria(request.getSearchCriteria());
        return requestCopy;
    }

    public <T extends Entity> SmartList<T> executeForList(SearchRequest searchRequest) {
        return RepositoryAdaptor.executeForList(this, submitRequest(searchRequest));
    }

    public <T extends Entity> Stream<T> executeForStream(SearchRequest searchRequest) {
        return RepositoryAdaptor.executeForStream(this, submitRequest(searchRequest));
    }

    public <T extends Entity> Stream<T> executeForStream(
            SearchRequest searchRequest, int enhanceBatch) {
        return RepositoryAdaptor.executeForStream(this, submitRequest(searchRequest), enhanceBatch);
    }

    public void delete(Entity pEntity) {
        RepositoryAdaptor.delete(this, pEntity);
    }

    public void info(String messageTemplate, Object... args) {
        LocationAwareLogger logger = getLogger();
        logger.log(null, FQCN, LocationAwareLogger.INFO_INT, messageTemplate, args, null);
    }

    public void info(Marker marker, String messageTemplate, Object... args) {
        LocationAwareLogger logger = getLogger();
        logger.log(marker, FQCN, LocationAwareLogger.INFO_INT, messageTemplate, args, null);
    }

    public void debug(String messageTemplate, Object... args) {
        LocationAwareLogger logger = getLogger();
        logger.log(null, FQCN, LocationAwareLogger.DEBUG_INT, messageTemplate, args, null);
    }

    public void debug(Marker marker, String messageTemplate, Object... args) {
        LocationAwareLogger logger = getLogger();
        logger.log(marker, FQCN, LocationAwareLogger.DEBUG_INT, messageTemplate, args, null);
    }

    public void warn(String messageTemplate, Object... args) {
        LocationAwareLogger logger = getLogger();
        logger.log(null, FQCN, LocationAwareLogger.WARN_INT, messageTemplate, args, null);
    }

    public void warn(Marker marker, String messageTemplate, Object... args) {
        LocationAwareLogger logger = getLogger();
        logger.log(marker, FQCN, LocationAwareLogger.WARN_INT, messageTemplate, args, null);
    }

    public void warn(Exception e, String messageTemplate, Object... args) {
        LocationAwareLogger logger = getLogger();
        logger.log(null, FQCN, LocationAwareLogger.WARN_INT, messageTemplate, args, e);
    }

    public void warn(Marker marker, Exception e, String messageTemplate, Object... args) {
        LocationAwareLogger logger = getLogger();
        logger.log(marker, FQCN, LocationAwareLogger.WARN_INT, messageTemplate, args, e);
    }

    public void error(String messageTemplate, Object... args) {
        LocationAwareLogger logger = getLogger();
        logger.log(null, FQCN, LocationAwareLogger.ERROR_INT, messageTemplate, args, null);
    }

    public void error(Marker marker, String messageTemplate, Object... args) {
        LocationAwareLogger logger = getLogger();
        logger.log(marker, FQCN, LocationAwareLogger.ERROR_INT, messageTemplate, args, null);
    }

    public void error(Exception e, String messageTemplate, Object... args) {
        LocationAwareLogger logger = getLogger();
        logger.log(null, FQCN, LocationAwareLogger.ERROR_INT, messageTemplate, args, e);
    }

    public void error(Marker marker, Exception e, String messageTemplate, Object... args) {
        LocationAwareLogger logger = getLogger();
        logger.log(marker, FQCN, LocationAwareLogger.ERROR_INT, messageTemplate, args, e);
    }

    private LocationAwareLogger getLogger() {
        Class<?> caller = CallerUtil.getCaller(3);
        return (LocationAwareLogger) LoggerFactory.getLogger(caller);
    }

    public <T extends Entity> AggregationResult aggregation(SearchRequest request) {
        return RepositoryAdaptor.aggregation(this, submitRequest(request));
    }

    public void put(String key, Object value) {
        if (ObjectUtil.isEmpty(key)) {
            throw new IllegalArgumentException("key cannot be null");
        }
        localStorage.put(key, value);
    }

    public void del(String key) {
        localStorage.remove(key);
    }

    public boolean containsKey(String key) {
        return localStorage.containsKey(key);
    }

    public void append(String key, Object value) {
        if (ObjectUtil.isEmpty(key)) {
            throw new IllegalArgumentException("key cannot be null");
        }
        if (ObjectUtil.isEmpty(value)) {
            return;
        }
        Object existing = localStorage.get(key);
        if (existing == null) {
            existing = new ArrayList();
        }
        else if (!(existing instanceof Collection<?>)) {
            ArrayList newCollection = new ArrayList();
            newCollection.add(existing);
            existing = newCollection;
        }
        ((Collection) existing).add(value);
        localStorage.put(key, existing);
    }

    public List getList(String key) {
        Object value = localStorage.get(key);
        if (value == null) {
            return ListUtil.empty();
        }
        if (value instanceof List) {
            return (List) value;
        }
        List ret = new ArrayList();
        ret.add(value);
        return ret;
    }

    public boolean hasObject(String key, Object o) {
        List list = getList(key);
        for (Object o1 : list) {
            if (o1 == o) {
                return true;
            }
        }
        return false;
    }

    /**
     * Framework support API. Subject to change or internalization.
     */
    public <T> T getBean(Class<T> clazz) {
        if (getResolver() != null) {
            T bean = getResolver().getBean(clazz);
            if (bean != null) {
                return bean;
            }
        }
        throw new TQLException("No bean defined for type:" + clazz);
    }

    /**
     * Framework support API. Subject to change or internalization.
     */
    public <T> T getBean(String name) {
        if (getResolver() != null) {
            T bean = getResolver().getBean(name);
            if (bean != null) {
                return bean;
            }
        }
        throw new TQLException("No bean defined for name:" + name);
    }

    public LocalDateTime now() {
        return LocalDateTime.now();
    }

    public void saveGraph(Entity entity) {
        if (entity == null) {
            return;
        }
        if (requestPolicy != null) {
            if (entity.newItem()) {
                requestPolicy.enforceInsert(this, entity);
            } else if (entity.deleteItem()) {
                requestPolicy.enforceDelete(this, entity);
            } else if (entity.recoverItem()) {
                requestPolicy.enforceRecover(this, entity);
            } else if (entity.needPersist()) {
                requestPolicy.enforceUpdate(this, entity);
            }
        }
        enforceAuditPolicy(entity);
        this.info("UserContext.saveGraph: entity hash=" + System.identityHashCode(entity));

        // Record old values before change (for audit)
        java.util.Map<String, Object> oldValues = null;
        if (entity instanceof BaseEntity && entity.getUpdatedProperties() != null) {
            oldValues = new java.util.HashMap<>();
            for (String prop : entity.getUpdatedProperties()) {
                Object oldVal = ((BaseEntity) entity).getOldValue(prop);
                if (oldVal != null) oldValues.put(prop, oldVal);
            }
        }

        RepositoryAdaptor.saveGraph(this, entity);

        // Emit audit event
        if (logManager != null) {
            emitAuditEvent(entity, oldValues);
        }
    }

    private void emitAuditEvent(Entity entity, java.util.Map<String, Object> oldValues) {
        java.util.Map<String, Object> values = new java.util.HashMap<>();
        if (entity instanceof BaseEntity) {
            values.put("id", entity.getId());
            values.put("version", entity.getVersion());
        }
        for (String prop : entity.getUpdatedProperties()) {
            values.put(prop, entity.getProperty(prop));
        }

        String comment = entity.getComment();
        io.teaql.core.log.AuditEvent event;
        if (entity.newItem()) {
            event = io.teaql.core.log.AuditEvent.created(entity.typeName(), values, comment);
        } else if (entity.deleteItem()) {
            event = io.teaql.core.log.AuditEvent.deleted(entity.typeName(), entity.getId(), entity.getVersion(), comment);
        } else if (entity.recoverItem()) {
            event = io.teaql.core.log.AuditEvent.recovered(entity.typeName(), entity.getId(), entity.getVersion(), comment);
        } else {
            event = io.teaql.core.log.AuditEvent.updated(
                entity.typeName(), values, entity.getUpdatedProperties(), oldValues, values, comment);
        }

        // Get masking config from EntityDescriptor (aligned with Rust audit_mask_fields, audit_value_max_len)
        java.util.List<String> maskFields = java.util.List.of();
        Integer maxValueLen = null;
        try {
            io.teaql.core.meta.EntityDescriptor desc = resolveEntityDescriptor(entity.typeName());
            String maskFieldsStr = desc.getStr("audit_mask_fields", "");
            if (!maskFieldsStr.isEmpty()) {
                maskFields = java.util.Arrays.asList(maskFieldsStr.split(","));
            }
            String maxLenStr = desc.getStr("audit_value_max_len", "");
            if (!maxLenStr.isEmpty()) {
                maxValueLen = Integer.parseInt(maxLenStr);
            }
        } catch (Exception ignored) {
        }

        logManager.emitAuditEvent(this, event, maskFields, maxValueLen);
    }

    /**
     * Enforces that entities have an audit comment set via auditAs() before saving.
     */
    protected void enforceAuditPolicy(Entity entity) {
        if (INTENT_MODE == IntentEnforcementMode.OFF) {
            return;
        }
        String comment = entity.getComment();
        if (ObjectUtil.isNotEmpty(comment)) {
            return;
        }

        String typeName = entity.typeName();
        StringBuilder msg = new StringBuilder();
        msg.append("[TRIPLE-INTENT VIOLATION] Save on ").append(typeName);
        msg.append("(id=").append(entity.getId()).append(") rejected.\n");
        msg.append("Missing: .auditAs()\n\n");
        msg.append("FIX: Every entity mutation must call .auditAs() before .save().\n");
        msg.append("Correct pattern:\n");
        msg.append("  ").append(uncapFirst(typeName)).append(".updateXxx(newValue)\n");
        msg.append("      .auditAs(\"Describe the business action being performed\")\n");
        msg.append("      .save(ctx);\n");
        msg.append("\n");
        msg.append("Do NOT use .save(ctx) alone. Do NOT use .setComment() directly.\n");
        msg.append("Refer to AGENTS.md section 'MANDATORY TRIPLE-INTENT' for full documentation.");

        if (INTENT_MODE == IntentEnforcementMode.STRICT) {
            throw new RepositoryException(msg.toString());
        }
        // WARN mode
        this.warn(msg.toString());
    }

    public void checkAndFix(Entity entity) {
        if (!(entity instanceof BaseEntity)) {
            return;
        }
        Checker checker = getChecker(entity);
        if (ObjectUtil.isEmpty(checker)) {
            throw new TQLException("No checker defined for entity:" + entity);
        }
        checker.checkAndFix(this, (BaseEntity) entity);
        List errors = getList(Checker.TEAQL_DATA_CHECK_RESULT);
        if (ObjectUtil.isEmpty(errors)) {
            return;
        }
        localStorage.remove(Checker.TEAQL_DATA_CHECK_RESULT);
        errors = translateError(entity, errors);
        throw new CheckException(errors);
    }

    public void checkAndFix(Iterable<? extends Entity> entities) {
        if (ObjectUtil.isEmpty(entities)) {
            return;
        }

        int i = 0;
        for (Entity entity : entities) {
            if (!(entity instanceof BaseEntity)) {
                i++;
                continue;
            }
            Checker checker = getChecker(entity);
            if (ObjectUtil.isEmpty(checker)) {
                throw new TQLException("No checker defined for entity:" + entity);
            }
            checker.checkAndFix(this, (BaseEntity) entity, ObjectLocation.arrayRoot(i));
            i++;
        }

        List errors = getList(Checker.TEAQL_DATA_CHECK_RESULT);
        if (ObjectUtil.isEmpty(errors)) {
            return;
        }
        localStorage.remove(Checker.TEAQL_DATA_CHECK_RESULT);
        errors = translateError(null, errors);
        throw new CheckException(errors);
    }

    public Checker getChecker(Entity entity) {
        String type = entity.typeName();
        Map<String, Checker> checkers = getBean("checkers");
        if (checkers == null) {
            return null;
        }
        return checkers.get(type);
    }

    public List<CheckResult> translateError(Entity pEntity, List<CheckResult> errors) {
        return getNaturalLanguageTranslator(pEntity).translateError(pEntity, errors);
    }

    public NaturalLanguageTranslator getNaturalLanguageTranslator(Entity entity) {
        if (entity != null) {
            EntityDescriptor entityDescriptor = resolveEntityDescriptor(entity.typeName());
            if (BooleanUtil.toBoolean(entityDescriptor.getStr("viewObject", "false"))) {
                return new SimpleChineseViewTranslator(getBean(EntityMetaFactory.class));
            }
        }
        return new BaseLanguageTranslator("en");
    }

    /**
     * Framework support API. Subject to change or internalization.
     */
    public void init(Object request) {
        if (getResolver() == null) {
            return;
        }
        List<UserContextInitializer> initializers =
                getResolver().getBeans(UserContextInitializer.class);
        if (initializers != null) {
            for (UserContextInitializer initializer : initializers) {
                if (initializer.support(request)) {
                    initializer.init(this, request);
                }
            }
        }
    }

    public InternalIdGenerator getInternalIdGenerator() {
        return internalIdGenerator;
    }

    public void setInternalIdGenerator(InternalIdGenerator internalIdGenerator) {
        this.internalIdGenerator = internalIdGenerator;
    }

    public Long generateId(Entity pEntity) {
        if (this.internalIdGenerator == null) {
            return null;
        }
        return internalIdGenerator.generateId(pEntity);
    }

    public void sendEvent(Object event) {
    }

    public void afterPersist(BaseEntity item) {
        item.clearUpdatedProperties();
    }

    public RequestPolicy getRequestPolicy() {
        return requestPolicy;
    }

    public void setRequestPolicy(RequestPolicy requestPolicy) {
        this.requestPolicy = requestPolicy;
    }

    public io.teaql.core.log.LogManager getLogManager() {
        return logManager;
    }

    public void setLogManager(io.teaql.core.log.LogManager logManager) {
        this.logManager = logManager;
    }

    /**
     * Framework support API. Subject to change or internalization.
     */
    public TQLResolver getResolver() {

        if (resolver == null) {
            resolver = GLobalResolver.getGlobalResolver();
        }

        return resolver;
    }

    /**
     * Framework support API. Subject to change or internalization.
     */
    public void setResolver(TQLResolver pResolver) {
        resolver = pResolver;
    }

    public RequestHolder getRequestHolder() {
        RequestHolder requestHolder = (RequestHolder) getObj(REQUEST_HOLDER);
        if (requestHolder == null) {
            throw new IllegalStateException("user context missing request holder");
        }
        return requestHolder;
    }

    public ResponseHolder getResponseHolder() {
        ResponseHolder responseHolder = (ResponseHolder) getObj(RESPONSE_HOLDER);
        if (responseHolder == null) {
            throw new IllegalStateException("user context missing response holder");
        }
        return responseHolder;
    }

    public List<String> getHeaderNames() {
        return getRequestHolder().getHeaderNames();
    }

    @Override
    public String method() {
        return getRequestHolder().method();
    }

    @Override
    public String getHeader(String name) {
        return getRequestHolder().getHeader(name);
    }

    @Override
    public byte[] getPart(String name) {
        return getRequestHolder().getPart(name);
    }

    @Override
    public List<String> getParameterNames() {
        return getRequestHolder().getParameterNames();
    }

    @Override
    public String getParameter(String name) {
        return getRequestHolder().getParameter(name);
    }

    @Override
    public byte[] getBodyBytes() {
        return getRequestHolder().getBodyBytes();
    }

    @Override
    public String requestUri() {
        return getRequestHolder().requestUri();
    }

    @Override
    public String getRemoteAddress() {
        return getRequestHolder().getRemoteAddress();
    }

    public String getClientIp() {
        String header = getHeader("X-Forwarded-For");
        if (header == null) {
            return getRemoteAddress();
        }
        String[] parts = header.split(",");
        return parts[0];
    }

    public boolean isFromLocalhost() {
        String clientIp = getClientIp();
        try {
            return InetAddress.getByName(clientIp).isLoopbackAddress();
        }
        catch (UnknownHostException pE) {
            throw new RuntimeException(pE);
        }
    }

    public List<String> getProxyChain() {
        String header = getHeader("X-Forwarded-For");
        if (header == null) {
            return Collections.emptyList();
        }
        String[] parts = header.split(",");
        return ListUtil.of(ArrayUtil.sub(parts, 1, -1));
    }

    public void setResponseHeader(String headerName, String headerValue) {
        getResponseHolder().setHeader(headerName, headerValue);
    }

    public Object graphql(String query) {
        GraphQLService service = getBean(GraphQLService.class);
        if (service == null) {
            throw new TQLException("graphql service not found");
        }
        return service.execute(this, query);
    }

    public Object getObj(String key, Object defaultValue) {
        return get(key, () -> defaultValue);
    }

    public <T> T get(String key, Supplier<T> supplier) {
        return (T) localStorage.get(key, supplier);
    }

    public <T> T advancedGet(String key, Supplier<T> supplier) {
        return get(key, () -> getInStore(key, supplier));
    }

    @Override
    public TranslationResponse translate(TranslationRequest req) {
        Translator translator = getBean(Translator.class);
        if (translator != null) {
            return translator.translate(req);
        }
        return null;
    }

    public void beforeCreate(EntityDescriptor descriptor, Entity toBeCreate) {
    }

    public void beforeUpdate(EntityDescriptor descriptor, Entity toBeUpdated) {
    }

    public void beforeDelete(EntityDescriptor descriptor, Entity toBeDeleted) {
    }

    public void beforeRecover(EntityDescriptor descriptor, Entity pToBeRecoverItem) {
    }

    public void afterLoad(EntityDescriptor descriptor, Entity loadedItem) {
    }

    public <T> T getInStore(String key) {
        return getBean(DataStore.class).get(key);
    }

    public <T> T getInStore(String key, Supplier<T> supplier) {
        return getBean(DataStore.class).get(key, supplier);
    }

    public <T> T getAndRemoveInStore(String key) {
        return getBean(DataStore.class).getAndRemove(key);
    }

    public void clearInStore(String key) {
        getBean(DataStore.class).remove(key);
    }

    public void putInStore(String key, Object value, int timeout) {
        if (timeout <= 0) {
            getBean(DataStore.class).put(key, value);
        }
        else {
            getBean(DataStore.class).put(key, value, timeout);
        }
    }

    public void duplicateFormException() {
        throw new DuplicatedFormException(
                "Your form is submitted and processing, please don't resubmit.");
    }

    public void errorMessage(String message, Object... args) {
        String req = getStr("_req");
        if (req != null) {
            clearInStore(req);
        }
        throw new ErrorMessageException(StrUtil.format(message, args));
    }

    /**
     * reload the entity if id exists
     *
     * @param entity
     * @param <T>
     * @return
     */
    public <T extends BaseEntity> T reload(T entity) {
        if (entity == null) {
            return null;
        }
        Long id = entity.getId();
        if (id == null) {
            return entity;
        }

        if (entity.get$status().equals(EntityStatus.PERSISTED)
                || entity.get$status().equals(EntityStatus.PERSISTED_DELETED)) {
            return entity;
        }

        BaseRequest<T> tempRequest = initRequest(entity.getClass());
        tempRequest.appendSearchCriteria(
                tempRequest.createBasicSearchCriteria(BaseEntity.ID_PROPERTY, Operator.EQUAL, id));
        T item = tempRequest.executeForOne(this);
        EntityDescriptor entityDescriptor = resolveEntityDescriptor(entity.typeName());
        while (entityDescriptor != null) {
            List<PropertyDescriptor> properties = entityDescriptor.getProperties();
            for (PropertyDescriptor property : properties) {
                entity.setProperty(property.getName(), item.getProperty(property.getName()));
            }
            entityDescriptor = entityDescriptor.getParent();
        }
        entity.set$status(item.get$status());
        return entity;
    }

    public <T> BaseRequest initRequest(Class<T> type) {
        if (type == null) {
            return null;
        }
        String name = type.getName();
        BaseRequest request = ReflectUtil.newInstance(ClassUtil.loadClass(name + "Request"), type);
        request.selectSelf();
        request.appendSearchCriteria(
                request.createBasicSearchCriteria(BaseEntity.VERSION_PROPERTY, Operator.GREATER_THAN, 0l));
        return request;
    }

    /**
     * execute task directly(in the same thread)
     *
     * @param lock the lock/task name
     * @param task the task to execute
     */
    public void execLocalTask(String lock, Runnable task) {
        if (ObjectUtil.isEmpty(lock)) {
            throw new TQLException("lock cannot be empty to execute local task");
        }

        LockService lockService = getBean(LockService.class);
        Lock localLock = lockService.getLocalLock(this, lock);
        runTask(task, localLock);
    }

    /**
     * execute task in one thread of the pool
     *
     * @param lock the lock/task name
     * @param task the task to execute
     */
    public void execLocalTaskAsync(String lock, Runnable task) {
        if (ObjectUtil.isEmpty(lock)) {
            throw new TQLException("lock cannot be empty to execute local task");
        }

        LockService lockService = getBean(LockService.class);
        Lock localLock = lockService.getLocalLock(this, lock);
        runTaskAsync(task, localLock);
    }

    /**
     * execute task directly(in the same thread), if this task is running, then do nothing
     *
     * @param lock the lock/task name
     * @param task the task to execute
     */
    public void execSingleLocalTask(String lock, Runnable task) {
        if (ObjectUtil.isEmpty(lock)) {
            throw new TQLException("lock cannot be empty to execute local task");
        }
        LockService lockService = getBean(LockService.class);
        Lock localLock = lockService.getLocalLock(this, lock);
        runSingleTask(task, localLock);
    }

    /**
     * execute task in one thread of the pool
     *
     * @param lock the lock/task name
     * @param task the task to execute
     */
    public void execSingleLocalTaskAsync(String lock, Runnable task) {
        if (ObjectUtil.isEmpty(lock)) {
            throw new TQLException("lock cannot be empty to execute local task");
        }

        LockService lockService = getBean(LockService.class);
        Lock localLock = lockService.getLocalLock(this, lock);
        runSingleTaskAsync(task, localLock);
    }

    /**
     * execute global task directly(in the same thread)
     *
     * @param lock the lock/task name
     * @param task the task to execute
     */
    public void execGlobalTask(String lock, Runnable task) {
        if (ObjectUtil.isEmpty(lock)) {
            throw new TQLException("lock cannot be empty to execute global task");
        }

        LockService lockService = getBean(LockService.class);
        Lock distributeLock = lockService.getDistributeLock(this, lock);
        runTask(task, distributeLock);
    }

    /**
     * execute global task in one thread of the pool
     *
     * @param lock the lock/task name
     * @param task the task to execute
     */
    public void execGlobalTaskAsync(String lock, Runnable task) {
        if (ObjectUtil.isEmpty(lock)) {
            throw new TQLException("lock cannot be empty to execute global task");
        }

        LockService lockService = getBean(LockService.class);
        Lock distributeLock = lockService.getDistributeLock(this, lock);
        runTaskAsync(task, distributeLock);
    }

    /**
     * execute global task directly(in the same thread), if this task is running, then do nothing
     *
     * @param lock the lock/task name
     * @param task the task to execute
     */
    public void execSingleGlobalTask(String lock, Runnable task) {
        if (ObjectUtil.isEmpty(lock)) {
            throw new TQLException("lock cannot be empty to execute local task");
        }
        LockService lockService = getBean(LockService.class);
        Lock distributeLock = lockService.getDistributeLock(this, lock);
        runSingleTask(task, distributeLock);
    }

    /**
     * execute task in one thread of the pool, if this task is running, then do nothing
     *
     * @param lock the lock/task name
     * @param task the task to execute
     */
    public void execSingleGlobalTaskAsync(String lock, Runnable task) {
        if (ObjectUtil.isEmpty(lock)) {
            throw new TQLException("lock cannot be empty to execute local task");
        }

        LockService lockService = getBean(LockService.class);
        Lock distributeLock = lockService.getDistributeLock(this, lock);
        runSingleTaskAsync(task, distributeLock);
    }

    private void runTask(Runnable task, Lock lock) {
        if (task == null) {
            return;
        }
        try {
            if (lock != null) {
                lock.lock();
            }
            task.run();
        }
        finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    private void runTaskAsync(Runnable task, Lock lock) {
        LockService.taskExecutor.execute(() -> runTask(task, lock));
    }

    private void runSingleTask(Runnable task, Lock lock) {
        if (task == null) {
            return;
        }
        boolean ready = false;
        try {
            if (lock != null) {
                ready = lock.tryLock();
            }
            else {
                ready = true;
            }
            if (ready) {
                task.run();
            }
        }
        finally {
            if (lock != null && ready) {
                lock.unlock();
            }
        }
    }

    private void runSingleTaskAsync(Runnable task, Lock lock) {
        LockService.taskExecutor.execute(() -> runSingleTask(task, lock));
    }


}
