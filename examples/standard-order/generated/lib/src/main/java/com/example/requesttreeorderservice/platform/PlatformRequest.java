
package com.example.requesttreeorderservice.platform;

import com.example.requesttreeorderservice.Q;
import com.example.requesttreeorderservice.customer.Customer;
import com.example.requesttreeorderservice.customer.CustomerRequest;
import com.example.requesttreeorderservice.orderstatus.OrderStatus;
import com.example.requesttreeorderservice.orderstatus.OrderStatusRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;

public class PlatformRequest<T extends Platform> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public PlatformRequest(Class<T> returnType){
        this(returnType, () -> (T) new Platform());
    }

    protected PlatformRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public PlatformRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public PlatformRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public PlatformRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public PlatformRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public PlatformRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public PlatformRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public PlatformRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public PlatformRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public PlatformRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public PlatformRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public PlatformRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (PlatformRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public PlatformRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public PlatformRequest<T> matchingAnyOf(PlatformRequest platform){
        super.internalMatchAny(platform);
        return this;
    }

    public PlatformRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public PlatformRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public PlatformRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public PlatformRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectName().selectVersion();
    }

    public PlatformRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public PlatformRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(Platform.ID_PROPERTY);
        selectProperty(Platform.NAME_PROPERTY);
        selectProperty(Platform.VERSION_PROPERTY);
        return this;
    }

    public PlatformRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectName().selectVersion();
    }

    public PlatformRequest<T> selectChildren(){
        super.selectAny();
        selectOrderStatusList().selectCustomerList();
        return selectId().selectName().selectVersion();
    }


    public PlatformRequest<T> selectId(){
       selectProperty(Platform.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PlatformRequest<T> unselectId(){
       unselectProperty(Platform.ID_PROPERTY);
       return this;
    }
    public PlatformRequest<T> selectName(){
       selectProperty(Platform.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PlatformRequest<T> unselectName(){
       unselectProperty(Platform.NAME_PROPERTY);
       return this;
    }
    public PlatformRequest<T> selectVersion(){
       selectProperty(Platform.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PlatformRequest<T> unselectVersion(){
       unselectProperty(Platform.VERSION_PROPERTY);
       return this;
    }
    public PlatformRequest<T> selectOrderStatusList(){
       return selectOrderStatusListWith(Q.orderStatuses().selectSelf());
    }

    public PlatformRequest<T> selectOrderStatusListWith(OrderStatusRequest orderStatusList){
       enhanceRelation(Platform.ORDER_STATUS_LIST_PROPERTY, orderStatusList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectOrderStatusListWith", null, Platform.ORDER_STATUS_LIST_PROPERTY));
       return this;
    }
    public PlatformRequest<T> selectCustomerList(){
       return selectCustomerListWith(Q.customers().selectSelf());
    }

    public PlatformRequest<T> selectCustomerListWith(CustomerRequest customerList){
       enhanceRelation(Platform.CUSTOMER_LIST_PROPERTY, customerList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectCustomerListWith", null, Platform.CUSTOMER_LIST_PROPERTY));
       return this;
    }

    public PlatformRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Platform.ID_PROPERTY, operator, values);
    }

    public PlatformRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public PlatformRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public PlatformRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public PlatformRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public PlatformRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public PlatformRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public PlatformRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public PlatformRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Platform.NAME_PROPERTY, operator, values);
    }

    public PlatformRequest<T> withNameIsNot(String name){
       return withName(Operator.NOT_EQUAL, name);
    }

    public PlatformRequest<T> withNameIn(String... name){
       return withName(Operator.IN, (Object[])name);
    }

    public PlatformRequest<T> withNameNotIn(String... name){
       return withName(Operator.NOT_IN, (Object[])name);
    }
    public PlatformRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public PlatformRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public PlatformRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public PlatformRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public PlatformRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public PlatformRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public PlatformRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public PlatformRequest<T> withNameNotContaining(String name){
       return withName(Operator.NOT_CONTAIN, name);
    }

    public PlatformRequest<T> withNameNotStartingWith(String name){
       return withName(Operator.NOT_BEGIN_WITH, name);
    }

    public PlatformRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public PlatformRequest<T> withNameNotEndingWith(String name){
       return withName(Operator.NOT_END_WITH, name);
    }

    public PlatformRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public PlatformRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public PlatformRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public PlatformRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public PlatformRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public PlatformRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Platform.VERSION_PROPERTY, operator, values);
    }

    public PlatformRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public PlatformRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public PlatformRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public PlatformRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public PlatformRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public PlatformRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public PlatformRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public PlatformRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public PlatformRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public PlatformRequest<T> withOrderStatusListMatching(OrderStatusRequest orderStatusRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(Platform.ID_PROPERTY, orderStatusRequest, OrderStatus.PLATFORM_PROPERTY));
    }

    public PlatformRequest<T> withoutOrderStatusListMatching(OrderStatusRequest orderStatusRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(Platform.ID_PROPERTY, orderStatusRequest, OrderStatus.PLATFORM_PROPERTY)));
    }

    public PlatformRequest<T> haveOrderStatuses(){
        return withOrderStatusListMatching(Q.orderStatuses().unlimited());
    }

    public PlatformRequest<T> haveNoOrderStatuses(){
        return withoutOrderStatusListMatching(Q.orderStatuses().unlimited());
    }
    public PlatformRequest<T> withCustomerListMatching(CustomerRequest customerRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(Platform.ID_PROPERTY, customerRequest, Customer.PLATFORM_PROPERTY));
    }

    public PlatformRequest<T> withoutCustomerListMatching(CustomerRequest customerRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(Platform.ID_PROPERTY, customerRequest, Customer.PLATFORM_PROPERTY)));
    }

    public PlatformRequest<T> haveCustomers(){
        return withCustomerListMatching(Q.customers().unlimited());
    }

    public PlatformRequest<T> haveNoCustomers(){
        return withoutCustomerListMatching(Q.customers().unlimited());
    }

    public PlatformRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public PlatformRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public PlatformRequest<T> groupByOrderStatusesWithDetails(OrderStatusRequest subRequest){
       aggregate(Platform.ORDER_STATUS_LIST_PROPERTY, subRequest);
       return this;
    }
    public PlatformRequest<T> groupByCustomersWithDetails(CustomerRequest subRequest){
       aggregate(Platform.CUSTOMER_LIST_PROPERTY, subRequest);
       return this;
    }

    public PlatformRequest<T> groupById(){
       groupBy(Platform.ID_PROPERTY);
       return this;
    }

    public PlatformRequest<T> groupByIdAs(String retName){
       groupBy(retName, Platform.ID_PROPERTY);
       return this;
    }

    public PlatformRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, Platform.ID_PROPERTY, function);
       return this;
    }

    public PlatformRequest<T> groupByName(){
       groupBy(Platform.NAME_PROPERTY);
       return this;
    }

    public PlatformRequest<T> groupByNameAs(String retName){
       groupBy(retName, Platform.NAME_PROPERTY);
       return this;
    }

    public PlatformRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, Platform.NAME_PROPERTY, function);
       return this;
    }

    public PlatformRequest<T> groupByVersion(){
       groupBy(Platform.VERSION_PROPERTY);
       return this;
    }

    public PlatformRequest<T> groupByVersionAs(String retName){
       groupBy(retName, Platform.VERSION_PROPERTY);
       return this;
    }

    public PlatformRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, Platform.VERSION_PROPERTY, function);
       return this;
    }



    public PlatformRequest<T> orderByIdAscending(){
       addOrderByAscending(Platform.ID_PROPERTY);
       return this;
    }

    public PlatformRequest<T> orderByIdDescending(){
       addOrderByDescending(Platform.ID_PROPERTY);
       return this;
    }

    public PlatformRequest<T> orderByNameAscending(){
       addOrderByAscending(Platform.NAME_PROPERTY);
       return this;
    }

    public PlatformRequest<T> orderByNameDescending(){
       addOrderByDescending(Platform.NAME_PROPERTY);
       return this;
    }
    public PlatformRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Platform.NAME_PROPERTY);
       return this;
    }

    public PlatformRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Platform.NAME_PROPERTY);
       return this;
    }
    public PlatformRequest<T> orderByVersionAscending(){
       addOrderByAscending(Platform.VERSION_PROPERTY);
       return this;
    }

    public PlatformRequest<T> orderByVersionDescending(){
       addOrderByDescending(Platform.VERSION_PROPERTY);
       return this;
    }


    public PlatformRequest<T> statsFromOrderStatusesAs(String name, OrderStatusRequest subRequest){
       return statsFromOrderStatusesAs(name, subRequest, false);
    }

    public PlatformRequest<T> statsFromOrderStatusesAs(String name, OrderStatusRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, OrderStatus.PLATFORM_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromOrderStatusesAs", name, Platform.ORDER_STATUS_LIST_PROPERTY));
       return this;
    }

    public PlatformRequest<T> statsFromOrderStatuses(OrderStatusRequest subRequest){
       return statsFromOrderStatusesAs(REFINEMENTS, subRequest);
    }
    public PlatformRequest<T> statsFromCustomersAs(String name, CustomerRequest subRequest){
       return statsFromCustomersAs(name, subRequest, false);
    }

    public PlatformRequest<T> statsFromCustomersAs(String name, CustomerRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, Customer.PLATFORM_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromCustomersAs", name, Platform.CUSTOMER_LIST_PROPERTY));
       return this;
    }

    public PlatformRequest<T> statsFromCustomers(CustomerRequest subRequest){
       return statsFromCustomersAs(REFINEMENTS, subRequest);
    }
    public PlatformRequest<T> countOrderStatuses(){
        addRelationCount("Count", Q.orderStatuses().unlimited(), OrderStatus.PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderStatuses", null, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> countOrderStatusesAs(String name){
        addRelationCount(name, Q.orderStatuses().unlimited(), OrderStatus.PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderStatusesAs", name, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> countOrderStatusesWith(String name, OrderStatusRequest subRequest){
        addRelationCount(name, subRequest, OrderStatus.PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderStatusesWith", name, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> countCustomers(){
        addRelationCount("Count", Q.customers().unlimited(), Customer.PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomers", null, Platform.CUSTOMER_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> countCustomersAs(String name){
        addRelationCount(name, Q.customers().unlimited(), Customer.PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomersAs", name, Platform.CUSTOMER_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> countCustomersWith(String name, CustomerRequest subRequest){
        addRelationCount(name, subRequest, Customer.PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomersWith", name, Platform.CUSTOMER_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> minDisplayOrderOfOrderStatuses(){
        addRelationScalarAggregate("minDisplayOrderOfOrderStatuses", Q.orderStatuses().unlimited(),
            OrderStatus.PLATFORM_PROPERTY, OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minDisplayOrderOfOrderStatuses", null, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> minDisplayOrderOfOrderStatusesAs(String name){
        return minDisplayOrderOfOrderStatusesAs(name, Q.orderStatuses().unlimited());
    }

    public PlatformRequest<T> minDisplayOrderOfOrderStatusesAs(String name, OrderStatusRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderStatus.PLATFORM_PROPERTY,
            OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minDisplayOrderOfOrderStatusesAs", name, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> maxDisplayOrderOfOrderStatuses(){
        addRelationScalarAggregate("maxDisplayOrderOfOrderStatuses", Q.orderStatuses().unlimited(),
            OrderStatus.PLATFORM_PROPERTY, OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxDisplayOrderOfOrderStatuses", null, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> maxDisplayOrderOfOrderStatusesAs(String name){
        return maxDisplayOrderOfOrderStatusesAs(name, Q.orderStatuses().unlimited());
    }

    public PlatformRequest<T> maxDisplayOrderOfOrderStatusesAs(String name, OrderStatusRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderStatus.PLATFORM_PROPERTY,
            OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxDisplayOrderOfOrderStatusesAs", name, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> sumDisplayOrderOfOrderStatuses(){
        addRelationScalarAggregate("sumDisplayOrderOfOrderStatuses", Q.orderStatuses().unlimited(),
            OrderStatus.PLATFORM_PROPERTY, OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumDisplayOrderOfOrderStatuses", null, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> sumDisplayOrderOfOrderStatusesAs(String name){
        return sumDisplayOrderOfOrderStatusesAs(name, Q.orderStatuses().unlimited());
    }

    public PlatformRequest<T> sumDisplayOrderOfOrderStatusesAs(String name, OrderStatusRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderStatus.PLATFORM_PROPERTY,
            OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumDisplayOrderOfOrderStatusesAs", name, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> avgDisplayOrderOfOrderStatuses(){
        addRelationScalarAggregate("avgDisplayOrderOfOrderStatuses", Q.orderStatuses().unlimited(),
            OrderStatus.PLATFORM_PROPERTY, OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgDisplayOrderOfOrderStatuses", null, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> avgDisplayOrderOfOrderStatusesAs(String name){
        return avgDisplayOrderOfOrderStatusesAs(name, Q.orderStatuses().unlimited());
    }

    public PlatformRequest<T> avgDisplayOrderOfOrderStatusesAs(String name, OrderStatusRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderStatus.PLATFORM_PROPERTY,
            OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgDisplayOrderOfOrderStatusesAs", name, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> standardDeviationDisplayOrderOfOrderStatuses(){
        addRelationScalarAggregate("stdDevDisplayOrderOfOrderStatuses", Q.orderStatuses().unlimited(),
            OrderStatus.PLATFORM_PROPERTY, OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationDisplayOrderOfOrderStatuses", null, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> standardDeviationDisplayOrderOfOrderStatusesAs(String name){
        return standardDeviationDisplayOrderOfOrderStatusesAs(name, Q.orderStatuses().unlimited());
    }

    public PlatformRequest<T> standardDeviationDisplayOrderOfOrderStatusesAs(String name, OrderStatusRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderStatus.PLATFORM_PROPERTY,
            OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationDisplayOrderOfOrderStatusesAs", name, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> squareRootOfPopulationStandardDeviationDisplayOrderOfOrderStatuses(){
        addRelationScalarAggregate("stdDevPopDisplayOrderOfOrderStatuses", Q.orderStatuses().unlimited(),
            OrderStatus.PLATFORM_PROPERTY, OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationDisplayOrderOfOrderStatuses", null, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> squareRootOfPopulationStandardDeviationDisplayOrderOfOrderStatusesAs(String name){
        return squareRootOfPopulationStandardDeviationDisplayOrderOfOrderStatusesAs(name, Q.orderStatuses().unlimited());
    }

    public PlatformRequest<T> squareRootOfPopulationStandardDeviationDisplayOrderOfOrderStatusesAs(String name, OrderStatusRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderStatus.PLATFORM_PROPERTY,
            OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationDisplayOrderOfOrderStatusesAs", name, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> sampleVarianceDisplayOrderOfOrderStatuses(){
        addRelationScalarAggregate("varSampDisplayOrderOfOrderStatuses", Q.orderStatuses().unlimited(),
            OrderStatus.PLATFORM_PROPERTY, OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceDisplayOrderOfOrderStatuses", null, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> sampleVarianceDisplayOrderOfOrderStatusesAs(String name){
        return sampleVarianceDisplayOrderOfOrderStatusesAs(name, Q.orderStatuses().unlimited());
    }

    public PlatformRequest<T> sampleVarianceDisplayOrderOfOrderStatusesAs(String name, OrderStatusRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderStatus.PLATFORM_PROPERTY,
            OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceDisplayOrderOfOrderStatusesAs", name, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> samplePopulationVarianceDisplayOrderOfOrderStatuses(){
        addRelationScalarAggregate("varPopDisplayOrderOfOrderStatuses", Q.orderStatuses().unlimited(),
            OrderStatus.PLATFORM_PROPERTY, OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceDisplayOrderOfOrderStatuses", null, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> samplePopulationVarianceDisplayOrderOfOrderStatusesAs(String name){
        return samplePopulationVarianceDisplayOrderOfOrderStatusesAs(name, Q.orderStatuses().unlimited());
    }

    public PlatformRequest<T> samplePopulationVarianceDisplayOrderOfOrderStatusesAs(String name, OrderStatusRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderStatus.PLATFORM_PROPERTY,
            OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceDisplayOrderOfOrderStatusesAs", name, Platform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }



    /**
     * get topN records
     * @param topN  records number
     */
    public PlatformRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public PlatformRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public PlatformRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public PlatformRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public PlatformRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public PlatformRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}