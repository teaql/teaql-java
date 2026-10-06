
package com.teaql.tracechainservice.shipment;

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

public class ShipmentRequest<T extends Shipment> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public ShipmentRequest(Class<T> returnType){
        super(returnType, () -> (T) new Shipment());
        selectId();
        selectVersion();
    }

    public ShipmentRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public ShipmentRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public ShipmentRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public ShipmentRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public ShipmentRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public ShipmentRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public ShipmentRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public ShipmentRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public ShipmentRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public ShipmentRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public ShipmentRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (ShipmentRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public ShipmentRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public ShipmentRequest<T> matchingAnyOf(ShipmentRequest shipment){
        super.internalMatchAny(shipment);
        return this;
    }

    public ShipmentRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public ShipmentRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public ShipmentRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public ShipmentRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectCustomerOrderIdOnly().selectReferenceCode().selectVersion();
    }

    public ShipmentRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public ShipmentRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectCustomerOrder().selectReferenceCode().selectVersion();
    }

    public ShipmentRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectCustomerOrder().selectReferenceCode().selectVersion();
    }


    public ShipmentRequest<T> selectId(){
       selectProperty(Shipment.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ShipmentRequest<T> unselectId(){
       unselectProperty(Shipment.ID_PROPERTY);
       return this;
    }
    public ShipmentRequest<T> selectCustomerOrderIdOnly(){
       selectProperty(Shipment.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public ShipmentRequest<T> selectCustomerOrder(){
        return selectCustomerOrderWith(Q.customerOrders().unlimited().selectSelf());
    }

    public ShipmentRequest<T> selectCustomerOrderWith(CustomerOrderRequest customerOrder){
       selectProperty(Shipment.CUSTOMER_ORDER_PROPERTY);
       enhanceRelation(Shipment.CUSTOMER_ORDER_PROPERTY, customerOrder);
       return this;
    }

    public ShipmentRequest<T> unselectCustomerOrder(){
       unselectProperty(Shipment.CUSTOMER_ORDER_PROPERTY);
       return this;
    }
    public ShipmentRequest<T> selectReferenceCode(){
       selectProperty(Shipment.REFERENCE_CODE_PROPERTY);
       return this;
    }

    /**
     * fill the referenceCode with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  referenceCode) to fetch referenceCode property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ShipmentRequest<T> unselectReferenceCode(){
       unselectProperty(Shipment.REFERENCE_CODE_PROPERTY);
       return this;
    }
    public ShipmentRequest<T> selectVersion(){
       selectProperty(Shipment.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ShipmentRequest<T> unselectVersion(){
       unselectProperty(Shipment.VERSION_PROPERTY);
       return this;
    }

    public ShipmentRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Shipment.ID_PROPERTY, operator, values);
    }

    public ShipmentRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public ShipmentRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public ShipmentRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public ShipmentRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public ShipmentRequest<T> filterByCustomerOrder(CustomerOrder... customerOrder){
      if (customerOrder == null || customerOrder.length == 0) {
        throw new IllegalArgumentException("filterByCustomerOrder parameter customerOrder cannot be empty");
      }
      return appendSearchCriteria(createCustomerOrderCriteria(Operator.EQUAL, (Object[])customerOrder));
    }

    public ShipmentRequest<T> withCustomerOrder(Operator operator, Object... values){
       return appendSearchCriteria(createCustomerOrderCriteria(operator, values));
    }

    public ShipmentRequest<T> withCustomerOrderIsUnknown(){
       return withCustomerOrder(Operator.IS_NULL);
    }

    public ShipmentRequest<T> withCustomerOrderIsKnown(){
       return withCustomerOrder(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCustomerOrderCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Shipment.CUSTOMER_ORDER_PROPERTY, operator, values);
    }

    public ShipmentRequest<T> filterByCustomerOrder(Long customerOrder){
      if(customerOrder == null){
         return this;
      }
      return withCustomerOrder(Operator.EQUAL, customerOrder);
    }
    public ShipmentRequest<T> withCustomerOrderMatching(CustomerOrderRequest customerOrder){
       return appendSearchCriteria(new SubQuerySearchCriteria(Shipment.CUSTOMER_ORDER_PROPERTY, customerOrder, CustomerOrder.ID_PROPERTY));
    }

    public ShipmentRequest<T> withoutCustomerOrderMatching(CustomerOrderRequest customerOrder){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(Shipment.CUSTOMER_ORDER_PROPERTY, customerOrder, CustomerOrder.ID_PROPERTY)));
    }

    public ShipmentRequest<T> filterByReferenceCode(String... referenceCode){
      if (referenceCode == null || referenceCode.length == 0) {
        throw new IllegalArgumentException("filterByReferenceCode parameter referenceCode cannot be empty");
      }
      return appendSearchCriteria(createReferenceCodeCriteria(Operator.EQUAL, (Object[])referenceCode));
    }

    public ShipmentRequest<T> withReferenceCode(Operator operator, Object... values){
       return appendSearchCriteria(createReferenceCodeCriteria(operator, values));
    }

    public ShipmentRequest<T> withReferenceCodeIsUnknown(){
       return withReferenceCode(Operator.IS_NULL);
    }

    public ShipmentRequest<T> withReferenceCodeIsKnown(){
       return withReferenceCode(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createReferenceCodeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Shipment.REFERENCE_CODE_PROPERTY, operator, values);
    }

    public ShipmentRequest<T> withReferenceCodeIsNot(String referenceCode){
       return withReferenceCode(Operator.NOT_EQUAL, referenceCode);
    }

    public ShipmentRequest<T> withReferenceCodeIn(String... referenceCode){
       return withReferenceCode(Operator.IN, (Object[])referenceCode);
    }

    public ShipmentRequest<T> withReferenceCodeNotIn(String... referenceCode){
       return withReferenceCode(Operator.NOT_IN, (Object[])referenceCode);
    }
    public ShipmentRequest<T> withReferenceCodeGreaterThan(String referenceCode){
       return withReferenceCode(Operator.GREATER_THAN, referenceCode);
    }

    public ShipmentRequest<T> withReferenceCodeGreaterThanOrEqualTo(String referenceCode){
       return withReferenceCode(Operator.GREATER_THAN_OR_EQUAL, referenceCode);
    }

    public ShipmentRequest<T> withReferenceCodeLessThan(String referenceCode){
       return withReferenceCode(Operator.LESS_THAN, referenceCode);
    }

    public ShipmentRequest<T> withReferenceCodeLessThanOrEqualTo(String referenceCode){
       return withReferenceCode(Operator.LESS_THAN_OR_EQUAL, referenceCode);
    }

    public ShipmentRequest<T> withReferenceCodeBetween(String startOfReferenceCode, String endOfReferenceCode){
       return withReferenceCode(Operator.BETWEEN, startOfReferenceCode, endOfReferenceCode);
    }
    public ShipmentRequest<T> withReferenceCodeStartingWith(String referenceCode){
       return withReferenceCode(Operator.BEGIN_WITH, referenceCode);
    }
    public ShipmentRequest<T> withReferenceCodeContaining(String referenceCode){
       return withReferenceCode(Operator.CONTAIN, referenceCode);
    }

    public ShipmentRequest<T> withReferenceCodeNotContaining(String referenceCode){
       return withReferenceCode(Operator.NOT_CONTAIN, referenceCode);
    }

    public ShipmentRequest<T> withReferenceCodeNotStartingWith(String referenceCode){
       return withReferenceCode(Operator.NOT_BEGIN_WITH, referenceCode);
    }

    public ShipmentRequest<T> withReferenceCodeEndingWith(String referenceCode){
       return withReferenceCode(Operator.END_WITH, referenceCode);
    }

    public ShipmentRequest<T> withReferenceCodeNotEndingWith(String referenceCode){
       return withReferenceCode(Operator.NOT_END_WITH, referenceCode);
    }

    public ShipmentRequest<T> withReferenceCodeIs(String referenceCode){
       return withReferenceCode(Operator.EQUAL, referenceCode);
    }

    public ShipmentRequest<T> withReferenceCodeSoundingLike(String referenceCode){
       return withReferenceCode(Operator.SOUNDS_LIKE, referenceCode);
    }



    public ShipmentRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public ShipmentRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public ShipmentRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public ShipmentRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Shipment.VERSION_PROPERTY, operator, values);
    }

    public ShipmentRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public ShipmentRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public ShipmentRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public ShipmentRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public ShipmentRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public ShipmentRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public ShipmentRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public ShipmentRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public ShipmentRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }



    public ShipmentRequest<T> count(){
        super.count();
        return this;
    }
    public ShipmentRequest<T> countAs(String retName){
        super.count(retName);
        return this;
    }
    public ShipmentRequest<T> groupByCustomerOrderWithDetails(){
       return groupByCustomerOrderWithDetails(Q.customerOrders().unlimited());
    }

    public ShipmentRequest<T> groupByCustomerOrderWithDetails(CustomerOrderRequest subRequest){
       aggregate(Shipment.CUSTOMER_ORDER_PROPERTY, subRequest);
       return this;
    }




    public ShipmentRequest<T> groupById(){
       groupBy(Shipment.ID_PROPERTY);
       return this;
    }

    public ShipmentRequest<T> groupByIdAs(String retName){
       groupBy(retName, Shipment.ID_PROPERTY);
       return this;
    }

    public ShipmentRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, Shipment.ID_PROPERTY, function);
       return this;
    }
    public ShipmentRequest<T> groupByCustomerOrderWith(CustomerOrderRequest subRequest){
       groupBy(Shipment.CUSTOMER_ORDER_PROPERTY, subRequest);
       return this;
    }
    public ShipmentRequest<T> groupByCustomerOrder(){
       groupBy(Shipment.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public ShipmentRequest<T> groupByCustomerOrderAs(String retName){
       groupBy(retName, Shipment.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public ShipmentRequest<T> groupByCustomerOrderWithFunction(String retName, AggrFunction function){
       groupBy(retName, Shipment.CUSTOMER_ORDER_PROPERTY, function);
       return this;
    }

    public ShipmentRequest<T> groupByReferenceCode(){
       groupBy(Shipment.REFERENCE_CODE_PROPERTY);
       return this;
    }

    public ShipmentRequest<T> groupByReferenceCodeAs(String retName){
       groupBy(retName, Shipment.REFERENCE_CODE_PROPERTY);
       return this;
    }

    public ShipmentRequest<T> groupByReferenceCodeWithFunction(String retName, AggrFunction function){
       groupBy(retName, Shipment.REFERENCE_CODE_PROPERTY, function);
       return this;
    }

    public ShipmentRequest<T> groupByVersion(){
       groupBy(Shipment.VERSION_PROPERTY);
       return this;
    }

    public ShipmentRequest<T> groupByVersionAs(String retName){
       groupBy(retName, Shipment.VERSION_PROPERTY);
       return this;
    }

    public ShipmentRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, Shipment.VERSION_PROPERTY, function);
       return this;
    }



    public ShipmentRequest<T> orderByIdAscending(){
       addOrderByAscending(Shipment.ID_PROPERTY);
       return this;
    }

    public ShipmentRequest<T> orderByIdDescending(){
       addOrderByDescending(Shipment.ID_PROPERTY);
       return this;
    }

    public ShipmentRequest<T> orderByCustomerOrderAscending(){
       addOrderByAscending(Shipment.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public ShipmentRequest<T> orderByCustomerOrderDescending(){
       addOrderByDescending(Shipment.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public ShipmentRequest<T> orderByReferenceCodeAscending(){
       addOrderByAscending(Shipment.REFERENCE_CODE_PROPERTY);
       return this;
    }

    public ShipmentRequest<T> orderByReferenceCodeDescending(){
       addOrderByDescending(Shipment.REFERENCE_CODE_PROPERTY);
       return this;
    }
    public ShipmentRequest<T> orderByReferenceCodeAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Shipment.REFERENCE_CODE_PROPERTY);
       return this;
    }

    public ShipmentRequest<T> orderByReferenceCodeDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Shipment.REFERENCE_CODE_PROPERTY);
       return this;
    }
    public ShipmentRequest<T> orderByVersionAscending(){
       addOrderByAscending(Shipment.VERSION_PROPERTY);
       return this;
    }

    public ShipmentRequest<T> orderByVersionDescending(){
       addOrderByDescending(Shipment.VERSION_PROPERTY);
       return this;
    }


    public CustomerOrderRequest rollUpToCustomerOrder(){
       CustomerOrderRequest customerOrder = Q.customerOrders().unlimited();
       this.withCustomerOrderMatching(customerOrder)
           .groupByCustomerOrderWith(customerOrder);
       return customerOrder;
    }




   public ShipmentRequest<T> facetByCustomerOrderAs(String facetName, CustomerOrderRequest customerOrder){
       return facetByCustomerOrderAs(facetName, customerOrder, true);
   }

   public ShipmentRequest<T> facetByCustomerOrderAs(String facetName, CustomerOrderRequest customerOrder, boolean includeAllFacets){
       addFacet(facetName, Shipment.CUSTOMER_ORDER_PROPERTY, customerOrder, includeAllFacets);
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public ShipmentRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public ShipmentRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public ShipmentRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public ShipmentRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public ShipmentRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public ShipmentRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}