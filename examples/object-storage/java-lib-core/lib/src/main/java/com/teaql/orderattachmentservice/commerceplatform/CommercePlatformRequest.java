
package com.teaql.orderattachmentservice.commerceplatform;

import com.teaql.orderattachmentservice.Q;
import com.teaql.orderattachmentservice.customerorder.CustomerOrder;
import com.teaql.orderattachmentservice.customerorder.CustomerOrderRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.util.Objects;

public class CommercePlatformRequest<T extends CommercePlatform> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public CommercePlatformRequest(Class<T> returnType){
        this(returnType, () -> (T) new CommercePlatform());
    }

    protected CommercePlatformRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public CommercePlatformRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public CommercePlatformRequest<T> copy(){
        return (CommercePlatformRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new CommercePlatformRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public CommercePlatformRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public CommercePlatformRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public CommercePlatformRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public CommercePlatformRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public CommercePlatformRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public CommercePlatformRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public CommercePlatformRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public CommercePlatformRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public CommercePlatformRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public CommercePlatformRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (CommercePlatformRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public CommercePlatformRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public CommercePlatformRequest<T> matchingAnyOf(CommercePlatformRequest commercePlatform){
        super.internalMatchAny(commercePlatform);
        return this;
    }

    public CommercePlatformRequest<T> matchingIdsOf(CommercePlatformRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                CommercePlatform.ID_PROPERTY, request, CommercePlatform.ID_PROPERTY));
    }

    public CommercePlatformRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public CommercePlatformRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public CommercePlatformRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public CommercePlatformRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectName().selectVersion();
    }

    public CommercePlatformRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public CommercePlatformRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(CommercePlatform.ID_PROPERTY);
        selectProperty(CommercePlatform.NAME_PROPERTY);
        selectProperty(CommercePlatform.VERSION_PROPERTY);
        return this;
    }

    public CommercePlatformRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectName().selectVersion();
    }

    public CommercePlatformRequest<T> selectChildren(){
        super.selectAny();
        selectCustomerOrderList();
        return selectId().selectName().selectVersion();
    }


    public CommercePlatformRequest<T> selectId(){
       selectProperty(CommercePlatform.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CommercePlatformRequest<T> unselectId(){
       unselectProperty(CommercePlatform.ID_PROPERTY);
       return this;
    }
    public CommercePlatformRequest<T> selectName(){
       selectProperty(CommercePlatform.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CommercePlatformRequest<T> unselectName(){
       unselectProperty(CommercePlatform.NAME_PROPERTY);
       return this;
    }
    public CommercePlatformRequest<T> selectVersion(){
       selectProperty(CommercePlatform.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CommercePlatformRequest<T> unselectVersion(){
       unselectProperty(CommercePlatform.VERSION_PROPERTY);
       return this;
    }
    public CommercePlatformRequest<T> selectCustomerOrderList(){
       return selectCustomerOrderListWith(Q.customerOrders().selectSelf());
    }

    public CommercePlatformRequest<T> selectCustomerOrderListWith(CustomerOrderRequest customerOrderList){
       enhanceRelation(CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY, customerOrderList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectCustomerOrderListWith", null, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
       return this;
    }

    public CommercePlatformRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CommercePlatform.ID_PROPERTY, operator, values);
    }

    public CommercePlatformRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public CommercePlatformRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public CommercePlatformRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public CommercePlatformRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public CommercePlatformRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public CommercePlatformRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public CommercePlatformRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public CommercePlatformRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CommercePlatform.NAME_PROPERTY, operator, values);
    }

    public CommercePlatformRequest<T> withNameIsNot(String name){
       return withName(Operator.NOT_EQUAL, name);
    }

    public CommercePlatformRequest<T> withNameIn(String... name){
       return withName(Operator.IN, (Object[])name);
    }

    public CommercePlatformRequest<T> withNameNotIn(String... name){
       return withName(Operator.NOT_IN, (Object[])name);
    }
    public CommercePlatformRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public CommercePlatformRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public CommercePlatformRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public CommercePlatformRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public CommercePlatformRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public CommercePlatformRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public CommercePlatformRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public CommercePlatformRequest<T> withNameNotContaining(String name){
       return withName(Operator.NOT_CONTAIN, name);
    }

    public CommercePlatformRequest<T> withNameNotStartingWith(String name){
       return withName(Operator.NOT_BEGIN_WITH, name);
    }

    public CommercePlatformRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public CommercePlatformRequest<T> withNameNotEndingWith(String name){
       return withName(Operator.NOT_END_WITH, name);
    }

    public CommercePlatformRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public CommercePlatformRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public CommercePlatformRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public CommercePlatformRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public CommercePlatformRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public CommercePlatformRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CommercePlatform.VERSION_PROPERTY, operator, values);
    }

    public CommercePlatformRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public CommercePlatformRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public CommercePlatformRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public CommercePlatformRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public CommercePlatformRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public CommercePlatformRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public CommercePlatformRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public CommercePlatformRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public CommercePlatformRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public CommercePlatformRequest<T> withCustomerOrderListMatching(CustomerOrderRequest customerOrderRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(CommercePlatform.ID_PROPERTY, customerOrderRequest, CustomerOrder.COMMERCE_PLATFORM_PROPERTY));
    }

    public CommercePlatformRequest<T> withoutCustomerOrderListMatching(CustomerOrderRequest customerOrderRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(CommercePlatform.ID_PROPERTY, customerOrderRequest, CustomerOrder.COMMERCE_PLATFORM_PROPERTY)));
    }

    public CommercePlatformRequest<T> haveCustomerOrders(){
        return withCustomerOrderListMatching(Q.customerOrders().unlimited());
    }

    public CommercePlatformRequest<T> haveNoCustomerOrders(){
        return withoutCustomerOrderListMatching(Q.customerOrders().unlimited());
    }

    public CommercePlatformRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public CommercePlatformRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public CommercePlatformRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public CommercePlatformRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, CommercePlatform.ID_PROPERTY);
        return this;
    }
    public CommercePlatformRequest<T> countDistinctName(){
        return countDistinctNameAs("name_distinct_count");
    }

    public CommercePlatformRequest<T> countDistinctNameAs(String alias){
        super.countDistinctProperty(alias, CommercePlatform.NAME_PROPERTY);
        return this;
    }
    public CommercePlatformRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public CommercePlatformRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, CommercePlatform.VERSION_PROPERTY);
        return this;
    }
    public CommercePlatformRequest<T> groupByCustomerOrdersWithDetails(CustomerOrderRequest subRequest){
       aggregate(CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY, subRequest);
       return this;
    }

    public CommercePlatformRequest<T> groupById(){
       groupBy(CommercePlatform.ID_PROPERTY);
       return this;
    }

    public CommercePlatformRequest<T> groupByIdAs(String retName){
       groupBy(retName, CommercePlatform.ID_PROPERTY);
       return this;
    }

    public CommercePlatformRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, CommercePlatform.ID_PROPERTY, function);
       return this;
    }

    public CommercePlatformRequest<T> groupByName(){
       groupBy(CommercePlatform.NAME_PROPERTY);
       return this;
    }

    public CommercePlatformRequest<T> groupByNameAs(String retName){
       groupBy(retName, CommercePlatform.NAME_PROPERTY);
       return this;
    }

    public CommercePlatformRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, CommercePlatform.NAME_PROPERTY, function);
       return this;
    }

    public CommercePlatformRequest<T> groupByVersion(){
       groupBy(CommercePlatform.VERSION_PROPERTY);
       return this;
    }

    public CommercePlatformRequest<T> groupByVersionAs(String retName){
       groupBy(retName, CommercePlatform.VERSION_PROPERTY);
       return this;
    }

    public CommercePlatformRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, CommercePlatform.VERSION_PROPERTY, function);
       return this;
    }



    public CommercePlatformRequest<T> orderByIdAscending(){
       addOrderByAscending(CommercePlatform.ID_PROPERTY);
       return this;
    }

    public CommercePlatformRequest<T> orderByIdDescending(){
       addOrderByDescending(CommercePlatform.ID_PROPERTY);
       return this;
    }

    public CommercePlatformRequest<T> orderByNameAscending(){
       addOrderByAscending(CommercePlatform.NAME_PROPERTY);
       return this;
    }

    public CommercePlatformRequest<T> orderByNameDescending(){
       addOrderByDescending(CommercePlatform.NAME_PROPERTY);
       return this;
    }
    public CommercePlatformRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(CommercePlatform.NAME_PROPERTY);
       return this;
    }

    public CommercePlatformRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(CommercePlatform.NAME_PROPERTY);
       return this;
    }
    public CommercePlatformRequest<T> orderByVersionAscending(){
       addOrderByAscending(CommercePlatform.VERSION_PROPERTY);
       return this;
    }

    public CommercePlatformRequest<T> orderByVersionDescending(){
       addOrderByDescending(CommercePlatform.VERSION_PROPERTY);
       return this;
    }


    public CommercePlatformRequest<T> statsFromCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
       return statsFromCustomerOrdersAs(name, subRequest, false);
    }

    public CommercePlatformRequest<T> statsFromCustomerOrdersAs(String name, CustomerOrderRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, CustomerOrder.COMMERCE_PLATFORM_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromCustomerOrdersAs", name, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
       return this;
    }

    public CommercePlatformRequest<T> statsFromCustomerOrders(CustomerOrderRequest subRequest){
       return statsFromCustomerOrdersAs(REFINEMENTS, subRequest);
    }
    public CommercePlatformRequest<T> countCustomerOrders(){
        addRelationCount("Count", Q.customerOrders().unlimited(), CustomerOrder.COMMERCE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomerOrders", null, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> countCustomerOrdersAs(String name){
        addRelationCount(name, Q.customerOrders().unlimited(), CustomerOrder.COMMERCE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomerOrdersAs", name, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> countCustomerOrdersWith(String name, CustomerOrderRequest subRequest){
        addRelationCount(name, subRequest, CustomerOrder.COMMERCE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomerOrdersWith", name, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }



    /**
     * get topN records
     * @param topN  records number
     */
    public CommercePlatformRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public CommercePlatformRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public CommercePlatformRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public CommercePlatformRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public CommercePlatformRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public CommercePlatformRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}