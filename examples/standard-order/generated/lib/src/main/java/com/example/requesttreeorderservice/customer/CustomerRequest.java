
package com.example.requesttreeorderservice.customer;

import com.example.requesttreeorderservice.Q;
import com.example.requesttreeorderservice.customerorder.CustomerOrder;
import com.example.requesttreeorderservice.customerorder.CustomerOrderRequest;
import com.example.requesttreeorderservice.platform.Platform;
import com.example.requesttreeorderservice.platform.PlatformRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;

public class CustomerRequest<T extends Customer> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public CustomerRequest(Class<T> returnType){
        this(returnType, () -> (T) new Customer());
    }

    protected CustomerRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public CustomerRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public CustomerRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public CustomerRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public CustomerRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public CustomerRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public CustomerRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public CustomerRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public CustomerRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public CustomerRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public CustomerRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public CustomerRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (CustomerRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public CustomerRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public CustomerRequest<T> matchingAnyOf(CustomerRequest customer){
        super.internalMatchAny(customer);
        return this;
    }

    public CustomerRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public CustomerRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public CustomerRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public CustomerRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectPlatformIdOnly().selectName().selectVersion();
    }

    public CustomerRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public CustomerRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(Customer.ID_PROPERTY);
        selectProperty(Customer.PLATFORM_PROPERTY);
        selectProperty(Customer.NAME_PROPERTY);
        selectProperty(Customer.VERSION_PROPERTY);
        return this;
    }

    public CustomerRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectPlatform().selectName().selectVersion();
    }

    public CustomerRequest<T> selectChildren(){
        super.selectAny();
        selectCustomerOrderList();
        return selectId().selectPlatform().selectName().selectVersion();
    }


    public CustomerRequest<T> selectId(){
       selectProperty(Customer.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerRequest<T> unselectId(){
       unselectProperty(Customer.ID_PROPERTY);
       return this;
    }
    public CustomerRequest<T> selectPlatformIdOnly(){
       selectProperty(Customer.PLATFORM_PROPERTY);
       return this;
    }

    public CustomerRequest<T> selectPlatform(){
        return selectPlatformWith(Q.platforms().unlimited().selectSelf());
    }

    public CustomerRequest<T> selectPlatformWith(PlatformRequest platform){
       selectProperty(Customer.PLATFORM_PROPERTY);
       enhanceRelation(Customer.PLATFORM_PROPERTY, platform,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectPlatformWith", null, Customer.PLATFORM_PROPERTY));
       return this;
    }

    public CustomerRequest<T> unselectPlatform(){
       unselectProperty(Customer.PLATFORM_PROPERTY);
       return this;
    }
    public CustomerRequest<T> selectName(){
       selectProperty(Customer.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerRequest<T> unselectName(){
       unselectProperty(Customer.NAME_PROPERTY);
       return this;
    }
    public CustomerRequest<T> selectVersion(){
       selectProperty(Customer.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerRequest<T> unselectVersion(){
       unselectProperty(Customer.VERSION_PROPERTY);
       return this;
    }
    public CustomerRequest<T> selectCustomerOrderList(){
       return selectCustomerOrderListWith(Q.customerOrders().selectSelf());
    }

    public CustomerRequest<T> selectCustomerOrderListWith(CustomerOrderRequest customerOrderList){
       enhanceRelation(Customer.CUSTOMER_ORDER_LIST_PROPERTY, customerOrderList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectCustomerOrderListWith", null, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
       return this;
    }

    public CustomerRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Customer.ID_PROPERTY, operator, values);
    }

    public CustomerRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public CustomerRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public CustomerRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public CustomerRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public CustomerRequest<T> filterByPlatform(Platform... platform){
      if (platform == null || platform.length == 0) {
        throw new IllegalArgumentException("filterByPlatform parameter platform cannot be empty");
      }
      return appendSearchCriteria(createPlatformCriteria(Operator.EQUAL, (Object[])platform));
    }

    public CustomerRequest<T> withPlatform(Operator operator, Object... values){
       return appendSearchCriteria(createPlatformCriteria(operator, values));
    }

    public CustomerRequest<T> withPlatformIsUnknown(){
       return withPlatform(Operator.IS_NULL);
    }

    public CustomerRequest<T> withPlatformIsKnown(){
       return withPlatform(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createPlatformCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Customer.PLATFORM_PROPERTY, operator, values);
    }

    public CustomerRequest<T> filterByPlatform(Long platform){
      if(platform == null){
         return this;
      }
      return withPlatform(Operator.EQUAL, platform);
    }
    public CustomerRequest<T> withPlatformMatching(PlatformRequest platform){
       return appendSearchCriteria(new SubQuerySearchCriteria(Customer.PLATFORM_PROPERTY, platform, Platform.ID_PROPERTY));
    }

    public CustomerRequest<T> withoutPlatformMatching(PlatformRequest platform){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(Customer.PLATFORM_PROPERTY, platform, Platform.ID_PROPERTY)));
    }

    public CustomerRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public CustomerRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public CustomerRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public CustomerRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Customer.NAME_PROPERTY, operator, values);
    }

    public CustomerRequest<T> withNameIsNot(String name){
       return withName(Operator.NOT_EQUAL, name);
    }

    public CustomerRequest<T> withNameIn(String... name){
       return withName(Operator.IN, (Object[])name);
    }

    public CustomerRequest<T> withNameNotIn(String... name){
       return withName(Operator.NOT_IN, (Object[])name);
    }
    public CustomerRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public CustomerRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public CustomerRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public CustomerRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public CustomerRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public CustomerRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public CustomerRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public CustomerRequest<T> withNameNotContaining(String name){
       return withName(Operator.NOT_CONTAIN, name);
    }

    public CustomerRequest<T> withNameNotStartingWith(String name){
       return withName(Operator.NOT_BEGIN_WITH, name);
    }

    public CustomerRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public CustomerRequest<T> withNameNotEndingWith(String name){
       return withName(Operator.NOT_END_WITH, name);
    }

    public CustomerRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public CustomerRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public CustomerRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public CustomerRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public CustomerRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public CustomerRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Customer.VERSION_PROPERTY, operator, values);
    }

    public CustomerRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public CustomerRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public CustomerRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public CustomerRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public CustomerRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public CustomerRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public CustomerRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public CustomerRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public CustomerRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public CustomerRequest<T> withCustomerOrderListMatching(CustomerOrderRequest customerOrderRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(Customer.ID_PROPERTY, customerOrderRequest, CustomerOrder.CUSTOMER_PROPERTY));
    }

    public CustomerRequest<T> withoutCustomerOrderListMatching(CustomerOrderRequest customerOrderRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(Customer.ID_PROPERTY, customerOrderRequest, CustomerOrder.CUSTOMER_PROPERTY)));
    }

    public CustomerRequest<T> haveCustomerOrders(){
        return withCustomerOrderListMatching(Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> haveNoCustomerOrders(){
        return withoutCustomerOrderListMatching(Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public CustomerRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public CustomerRequest<T> groupByPlatformWithDetails(){
       return groupByPlatformWithDetails(Q.platforms().unlimited());
    }

    public CustomerRequest<T> groupByPlatformWithDetails(PlatformRequest subRequest){
       aggregate(Customer.PLATFORM_PROPERTY, subRequest);
       return this;
    }



    public CustomerRequest<T> groupByCustomerOrdersWithDetails(CustomerOrderRequest subRequest){
       aggregate(Customer.CUSTOMER_ORDER_LIST_PROPERTY, subRequest);
       return this;
    }

    public CustomerRequest<T> groupById(){
       groupBy(Customer.ID_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByIdAs(String retName){
       groupBy(retName, Customer.ID_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, Customer.ID_PROPERTY, function);
       return this;
    }
    public CustomerRequest<T> groupByPlatformWith(PlatformRequest subRequest){
       groupBy(Customer.PLATFORM_PROPERTY, subRequest);
       return this;
    }
    public CustomerRequest<T> groupByPlatform(){
       groupBy(Customer.PLATFORM_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByPlatformAs(String retName){
       groupBy(retName, Customer.PLATFORM_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByPlatformWithFunction(String retName, AggrFunction function){
       groupBy(retName, Customer.PLATFORM_PROPERTY, function);
       return this;
    }

    public CustomerRequest<T> groupByName(){
       groupBy(Customer.NAME_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByNameAs(String retName){
       groupBy(retName, Customer.NAME_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, Customer.NAME_PROPERTY, function);
       return this;
    }

    public CustomerRequest<T> groupByVersion(){
       groupBy(Customer.VERSION_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByVersionAs(String retName){
       groupBy(retName, Customer.VERSION_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, Customer.VERSION_PROPERTY, function);
       return this;
    }



    public CustomerRequest<T> orderByIdAscending(){
       addOrderByAscending(Customer.ID_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByIdDescending(){
       addOrderByDescending(Customer.ID_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByPlatformAscending(){
       addOrderByAscending(Customer.PLATFORM_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByPlatformDescending(){
       addOrderByDescending(Customer.PLATFORM_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByNameAscending(){
       addOrderByAscending(Customer.NAME_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByNameDescending(){
       addOrderByDescending(Customer.NAME_PROPERTY);
       return this;
    }
    public CustomerRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Customer.NAME_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Customer.NAME_PROPERTY);
       return this;
    }
    public CustomerRequest<T> orderByVersionAscending(){
       addOrderByAscending(Customer.VERSION_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByVersionDescending(){
       addOrderByDescending(Customer.VERSION_PROPERTY);
       return this;
    }


    public CustomerRequest<T> statsFromCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
       return statsFromCustomerOrdersAs(name, subRequest, false);
    }

    public CustomerRequest<T> statsFromCustomerOrdersAs(String name, CustomerOrderRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, CustomerOrder.CUSTOMER_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromCustomerOrdersAs", name, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
       return this;
    }

    public CustomerRequest<T> statsFromCustomerOrders(CustomerOrderRequest subRequest){
       return statsFromCustomerOrdersAs(REFINEMENTS, subRequest);
    }
    public PlatformRequest rollUpToPlatform(){
       PlatformRequest platform = Q.platforms().unlimited();
       this.withPlatformMatching(platform)
           .groupByPlatformWith(platform);
       return platform;
    }



    public CustomerRequest<T> countCustomerOrders(){
        addRelationCount("Count", Q.customerOrders().unlimited(), CustomerOrder.CUSTOMER_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomerOrders", null, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CustomerRequest<T> countCustomerOrdersAs(String name){
        addRelationCount(name, Q.customerOrders().unlimited(), CustomerOrder.CUSTOMER_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomerOrdersAs", name, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CustomerRequest<T> countCustomerOrdersWith(String name, CustomerOrderRequest subRequest){
        addRelationCount(name, subRequest, CustomerOrder.CUSTOMER_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomerOrdersWith", name, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }
    public CustomerRequest<T> minTotalAmountOfCustomerOrders(){
        addRelationScalarAggregate("minTotalAmountOfCustomerOrders", Q.customerOrders().unlimited(),
            CustomerOrder.CUSTOMER_PROPERTY, CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minTotalAmountOfCustomerOrders", null, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CustomerRequest<T> minTotalAmountOfCustomerOrdersAs(String name){
        return minTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> minTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        addRelationScalarAggregate(name, subRequest, CustomerOrder.CUSTOMER_PROPERTY,
            CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minTotalAmountOfCustomerOrdersAs", name, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }
    public CustomerRequest<T> maxTotalAmountOfCustomerOrders(){
        addRelationScalarAggregate("maxTotalAmountOfCustomerOrders", Q.customerOrders().unlimited(),
            CustomerOrder.CUSTOMER_PROPERTY, CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxTotalAmountOfCustomerOrders", null, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CustomerRequest<T> maxTotalAmountOfCustomerOrdersAs(String name){
        return maxTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> maxTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        addRelationScalarAggregate(name, subRequest, CustomerOrder.CUSTOMER_PROPERTY,
            CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxTotalAmountOfCustomerOrdersAs", name, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }
    public CustomerRequest<T> sumTotalAmountOfCustomerOrders(){
        addRelationScalarAggregate("sumTotalAmountOfCustomerOrders", Q.customerOrders().unlimited(),
            CustomerOrder.CUSTOMER_PROPERTY, CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumTotalAmountOfCustomerOrders", null, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CustomerRequest<T> sumTotalAmountOfCustomerOrdersAs(String name){
        return sumTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> sumTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        addRelationScalarAggregate(name, subRequest, CustomerOrder.CUSTOMER_PROPERTY,
            CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumTotalAmountOfCustomerOrdersAs", name, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }
    public CustomerRequest<T> avgTotalAmountOfCustomerOrders(){
        addRelationScalarAggregate("avgTotalAmountOfCustomerOrders", Q.customerOrders().unlimited(),
            CustomerOrder.CUSTOMER_PROPERTY, CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgTotalAmountOfCustomerOrders", null, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CustomerRequest<T> avgTotalAmountOfCustomerOrdersAs(String name){
        return avgTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> avgTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        addRelationScalarAggregate(name, subRequest, CustomerOrder.CUSTOMER_PROPERTY,
            CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgTotalAmountOfCustomerOrdersAs", name, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }
    public CustomerRequest<T> standardDeviationTotalAmountOfCustomerOrders(){
        addRelationScalarAggregate("stdDevTotalAmountOfCustomerOrders", Q.customerOrders().unlimited(),
            CustomerOrder.CUSTOMER_PROPERTY, CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationTotalAmountOfCustomerOrders", null, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CustomerRequest<T> standardDeviationTotalAmountOfCustomerOrdersAs(String name){
        return standardDeviationTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> standardDeviationTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        addRelationScalarAggregate(name, subRequest, CustomerOrder.CUSTOMER_PROPERTY,
            CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationTotalAmountOfCustomerOrdersAs", name, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }
    public CustomerRequest<T> squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrders(){
        addRelationScalarAggregate("stdDevPopTotalAmountOfCustomerOrders", Q.customerOrders().unlimited(),
            CustomerOrder.CUSTOMER_PROPERTY, CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrders", null, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CustomerRequest<T> squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrdersAs(String name){
        return squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        addRelationScalarAggregate(name, subRequest, CustomerOrder.CUSTOMER_PROPERTY,
            CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrdersAs", name, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }
    public CustomerRequest<T> sampleVarianceTotalAmountOfCustomerOrders(){
        addRelationScalarAggregate("varSampTotalAmountOfCustomerOrders", Q.customerOrders().unlimited(),
            CustomerOrder.CUSTOMER_PROPERTY, CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceTotalAmountOfCustomerOrders", null, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CustomerRequest<T> sampleVarianceTotalAmountOfCustomerOrdersAs(String name){
        return sampleVarianceTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> sampleVarianceTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        addRelationScalarAggregate(name, subRequest, CustomerOrder.CUSTOMER_PROPERTY,
            CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceTotalAmountOfCustomerOrdersAs", name, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }
    public CustomerRequest<T> samplePopulationVarianceTotalAmountOfCustomerOrders(){
        addRelationScalarAggregate("varPopTotalAmountOfCustomerOrders", Q.customerOrders().unlimited(),
            CustomerOrder.CUSTOMER_PROPERTY, CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceTotalAmountOfCustomerOrders", null, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CustomerRequest<T> samplePopulationVarianceTotalAmountOfCustomerOrdersAs(String name){
        return samplePopulationVarianceTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> samplePopulationVarianceTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        addRelationScalarAggregate(name, subRequest, CustomerOrder.CUSTOMER_PROPERTY,
            CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceTotalAmountOfCustomerOrdersAs", name, Customer.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

   public CustomerRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform){
       return facetByPlatformAs(facetName, platform, true);
   }

   public CustomerRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform, boolean includeAllFacets){
       addFacet(facetName, Customer.PLATFORM_PROPERTY, platform, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByPlatformAs", facetName, Customer.PLATFORM_PROPERTY));
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public CustomerRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public CustomerRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public CustomerRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public CustomerRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public CustomerRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public CustomerRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}