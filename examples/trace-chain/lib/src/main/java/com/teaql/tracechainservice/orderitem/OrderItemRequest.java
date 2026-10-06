
package com.teaql.tracechainservice.orderitem;

import com.teaql.tracechainservice.Q;
import com.teaql.tracechainservice.customerorder.CustomerOrder;
import com.teaql.tracechainservice.customerorder.CustomerOrderRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;

public class OrderItemRequest<T extends OrderItem> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public OrderItemRequest(Class<T> returnType){
        super(returnType, () -> (T) new OrderItem());
        selectId();
        selectVersion();
    }

    public OrderItemRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public OrderItemRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public OrderItemRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public OrderItemRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public OrderItemRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public OrderItemRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public OrderItemRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public OrderItemRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public OrderItemRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public OrderItemRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public OrderItemRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (OrderItemRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public OrderItemRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public OrderItemRequest<T> matchingAnyOf(OrderItemRequest orderItem){
        super.internalMatchAny(orderItem);
        return this;
    }

    public OrderItemRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public OrderItemRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public OrderItemRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public OrderItemRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectCustomerOrderIdOnly().selectName().selectVersion();
    }

    public OrderItemRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public OrderItemRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectCustomerOrder().selectName().selectVersion();
    }

    public OrderItemRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectCustomerOrder().selectName().selectVersion();
    }


    public OrderItemRequest<T> selectId(){
       selectProperty(OrderItem.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderItemRequest<T> unselectId(){
       unselectProperty(OrderItem.ID_PROPERTY);
       return this;
    }
    public OrderItemRequest<T> selectCustomerOrderIdOnly(){
       selectProperty(OrderItem.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> selectCustomerOrder(){
        return selectCustomerOrderWith(Q.customerOrders().unlimited().selectSelf());
    }

    public OrderItemRequest<T> selectCustomerOrderWith(CustomerOrderRequest customerOrder){
       selectProperty(OrderItem.CUSTOMER_ORDER_PROPERTY);
       enhanceRelation(OrderItem.CUSTOMER_ORDER_PROPERTY, customerOrder);
       return this;
    }

    public OrderItemRequest<T> unselectCustomerOrder(){
       unselectProperty(OrderItem.CUSTOMER_ORDER_PROPERTY);
       return this;
    }
    public OrderItemRequest<T> selectName(){
       selectProperty(OrderItem.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderItemRequest<T> unselectName(){
       unselectProperty(OrderItem.NAME_PROPERTY);
       return this;
    }
    public OrderItemRequest<T> selectVersion(){
       selectProperty(OrderItem.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderItemRequest<T> unselectVersion(){
       unselectProperty(OrderItem.VERSION_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderItem.ID_PROPERTY, operator, values);
    }

    public OrderItemRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public OrderItemRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public OrderItemRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public OrderItemRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public OrderItemRequest<T> filterByCustomerOrder(CustomerOrder... customerOrder){
      if (customerOrder == null || customerOrder.length == 0) {
        throw new IllegalArgumentException("filterByCustomerOrder parameter customerOrder cannot be empty");
      }
      return appendSearchCriteria(createCustomerOrderCriteria(Operator.EQUAL, (Object[])customerOrder));
    }

    public OrderItemRequest<T> withCustomerOrder(Operator operator, Object... values){
       return appendSearchCriteria(createCustomerOrderCriteria(operator, values));
    }

    public OrderItemRequest<T> withCustomerOrderIsUnknown(){
       return withCustomerOrder(Operator.IS_NULL);
    }

    public OrderItemRequest<T> withCustomerOrderIsKnown(){
       return withCustomerOrder(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCustomerOrderCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderItem.CUSTOMER_ORDER_PROPERTY, operator, values);
    }

    public OrderItemRequest<T> filterByCustomerOrder(Long customerOrder){
      if(customerOrder == null){
         return this;
      }
      return withCustomerOrder(Operator.EQUAL, customerOrder);
    }
    public OrderItemRequest<T> withCustomerOrderMatching(CustomerOrderRequest customerOrder){
       return appendSearchCriteria(new SubQuerySearchCriteria(OrderItem.CUSTOMER_ORDER_PROPERTY, customerOrder, CustomerOrder.ID_PROPERTY));
    }

    public OrderItemRequest<T> withoutCustomerOrderMatching(CustomerOrderRequest customerOrder){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(OrderItem.CUSTOMER_ORDER_PROPERTY, customerOrder, CustomerOrder.ID_PROPERTY)));
    }

    public OrderItemRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public OrderItemRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public OrderItemRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public OrderItemRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderItem.NAME_PROPERTY, operator, values);
    }

    public OrderItemRequest<T> withNameIsNot(String name){
       return withName(Operator.NOT_EQUAL, name);
    }

    public OrderItemRequest<T> withNameIn(String... name){
       return withName(Operator.IN, (Object[])name);
    }

    public OrderItemRequest<T> withNameNotIn(String... name){
       return withName(Operator.NOT_IN, (Object[])name);
    }
    public OrderItemRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public OrderItemRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public OrderItemRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public OrderItemRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public OrderItemRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public OrderItemRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public OrderItemRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public OrderItemRequest<T> withNameNotContaining(String name){
       return withName(Operator.NOT_CONTAIN, name);
    }

    public OrderItemRequest<T> withNameNotStartingWith(String name){
       return withName(Operator.NOT_BEGIN_WITH, name);
    }

    public OrderItemRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public OrderItemRequest<T> withNameNotEndingWith(String name){
       return withName(Operator.NOT_END_WITH, name);
    }

    public OrderItemRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public OrderItemRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public OrderItemRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public OrderItemRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public OrderItemRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public OrderItemRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderItem.VERSION_PROPERTY, operator, values);
    }

    public OrderItemRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public OrderItemRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public OrderItemRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public OrderItemRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public OrderItemRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public OrderItemRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public OrderItemRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public OrderItemRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public OrderItemRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }



    public OrderItemRequest<T> count(){
        super.count();
        return this;
    }
    public OrderItemRequest<T> countAs(String retName){
        super.count(retName);
        return this;
    }
    public OrderItemRequest<T> groupByCustomerOrderWithDetails(){
       return groupByCustomerOrderWithDetails(Q.customerOrders().unlimited());
    }

    public OrderItemRequest<T> groupByCustomerOrderWithDetails(CustomerOrderRequest subRequest){
       aggregate(OrderItem.CUSTOMER_ORDER_PROPERTY, subRequest);
       return this;
    }




    public OrderItemRequest<T> groupById(){
       groupBy(OrderItem.ID_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByIdAs(String retName){
       groupBy(retName, OrderItem.ID_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderItem.ID_PROPERTY, function);
       return this;
    }
    public OrderItemRequest<T> groupByCustomerOrderWith(CustomerOrderRequest subRequest){
       groupBy(OrderItem.CUSTOMER_ORDER_PROPERTY, subRequest);
       return this;
    }
    public OrderItemRequest<T> groupByCustomerOrder(){
       groupBy(OrderItem.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByCustomerOrderAs(String retName){
       groupBy(retName, OrderItem.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByCustomerOrderWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderItem.CUSTOMER_ORDER_PROPERTY, function);
       return this;
    }

    public OrderItemRequest<T> groupByName(){
       groupBy(OrderItem.NAME_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByNameAs(String retName){
       groupBy(retName, OrderItem.NAME_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderItem.NAME_PROPERTY, function);
       return this;
    }

    public OrderItemRequest<T> groupByVersion(){
       groupBy(OrderItem.VERSION_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByVersionAs(String retName){
       groupBy(retName, OrderItem.VERSION_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderItem.VERSION_PROPERTY, function);
       return this;
    }



    public OrderItemRequest<T> orderByIdAscending(){
       addOrderByAscending(OrderItem.ID_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByIdDescending(){
       addOrderByDescending(OrderItem.ID_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByCustomerOrderAscending(){
       addOrderByAscending(OrderItem.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByCustomerOrderDescending(){
       addOrderByDescending(OrderItem.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByNameAscending(){
       addOrderByAscending(OrderItem.NAME_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByNameDescending(){
       addOrderByDescending(OrderItem.NAME_PROPERTY);
       return this;
    }
    public OrderItemRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(OrderItem.NAME_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(OrderItem.NAME_PROPERTY);
       return this;
    }
    public OrderItemRequest<T> orderByVersionAscending(){
       addOrderByAscending(OrderItem.VERSION_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByVersionDescending(){
       addOrderByDescending(OrderItem.VERSION_PROPERTY);
       return this;
    }


    public CustomerOrderRequest rollUpToCustomerOrder(){
       CustomerOrderRequest customerOrder = Q.customerOrders().unlimited();
       this.withCustomerOrderMatching(customerOrder)
           .groupByCustomerOrderWith(customerOrder);
       return customerOrder;
    }




   public OrderItemRequest<T> facetByCustomerOrderAs(String facetName, CustomerOrderRequest customerOrder){
       return facetByCustomerOrderAs(facetName, customerOrder, true);
   }

   public OrderItemRequest<T> facetByCustomerOrderAs(String facetName, CustomerOrderRequest customerOrder, boolean includeAllFacets){
       addFacet(facetName, OrderItem.CUSTOMER_ORDER_PROPERTY, customerOrder, includeAllFacets);
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public OrderItemRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public OrderItemRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public OrderItemRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public OrderItemRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public OrderItemRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public OrderItemRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}