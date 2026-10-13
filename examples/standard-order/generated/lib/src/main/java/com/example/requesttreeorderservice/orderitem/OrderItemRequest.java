
package com.example.requesttreeorderservice.orderitem;

import com.example.requesttreeorderservice.Q;
import com.example.requesttreeorderservice.customerorder.CustomerOrder;
import com.example.requesttreeorderservice.customerorder.CustomerOrderRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.math.BigDecimal;

public class OrderItemRequest<T extends OrderItem> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public OrderItemRequest(Class<T> returnType){
        this(returnType, () -> (T) new OrderItem());
    }

    protected OrderItemRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
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
        return selectId().selectCustomerOrderIdOnly().selectProductName().selectQuantity().selectUnitPrice().selectVersion();
    }

    public OrderItemRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public OrderItemRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(OrderItem.ID_PROPERTY);
        selectProperty(OrderItem.CUSTOMER_ORDER_PROPERTY);
        selectProperty(OrderItem.PRODUCT_NAME_PROPERTY);
        selectProperty(OrderItem.QUANTITY_PROPERTY);
        selectProperty(OrderItem.UNIT_PRICE_PROPERTY);
        selectProperty(OrderItem.VERSION_PROPERTY);
        return this;
    }

    public OrderItemRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectCustomerOrder().selectProductName().selectQuantity().selectUnitPrice().selectVersion();
    }

    public OrderItemRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectCustomerOrder().selectProductName().selectQuantity().selectUnitPrice().selectVersion();
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
       enhanceRelation(OrderItem.CUSTOMER_ORDER_PROPERTY, customerOrder,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectCustomerOrderWith", null, OrderItem.CUSTOMER_ORDER_PROPERTY));
       return this;
    }

    public OrderItemRequest<T> unselectCustomerOrder(){
       unselectProperty(OrderItem.CUSTOMER_ORDER_PROPERTY);
       return this;
    }
    public OrderItemRequest<T> selectProductName(){
       selectProperty(OrderItem.PRODUCT_NAME_PROPERTY);
       return this;
    }

    /**
     * fill the productName with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  productName) to fetch productName property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderItemRequest<T> unselectProductName(){
       unselectProperty(OrderItem.PRODUCT_NAME_PROPERTY);
       return this;
    }
    public OrderItemRequest<T> selectQuantity(){
       selectProperty(OrderItem.QUANTITY_PROPERTY);
       return this;
    }

    /**
     * fill the quantity with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  quantity) to fetch quantity property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the quantity with customized aggrFunction, TEAQL uses ({aggrFunction}(quantity) AS quantity to fetch quantity property.
     * @param aggrFunction  aggrFunction
     */
    public OrderItemRequest<T> selectQuantity(AggrFunction aggrFunction){
       selectProperty(OrderItem.QUANTITY_PROPERTY, aggrFunction);
       return this;
    }


    public OrderItemRequest<T> unselectQuantity(){
       unselectProperty(OrderItem.QUANTITY_PROPERTY);
       return this;
    }
    public OrderItemRequest<T> selectUnitPrice(){
       selectProperty(OrderItem.UNIT_PRICE_PROPERTY);
       return this;
    }

    /**
     * fill the unitPrice with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  unitPrice) to fetch unitPrice property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the unitPrice with customized aggrFunction, TEAQL uses ({aggrFunction}(unitPrice) AS unitPrice to fetch unitPrice property.
     * @param aggrFunction  aggrFunction
     */
    public OrderItemRequest<T> selectUnitPrice(AggrFunction aggrFunction){
       selectProperty(OrderItem.UNIT_PRICE_PROPERTY, aggrFunction);
       return this;
    }


    public OrderItemRequest<T> unselectUnitPrice(){
       unselectProperty(OrderItem.UNIT_PRICE_PROPERTY);
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

    public OrderItemRequest<T> filterByProductName(String... productName){
      if (productName == null || productName.length == 0) {
        throw new IllegalArgumentException("filterByProductName parameter productName cannot be empty");
      }
      return appendSearchCriteria(createProductNameCriteria(Operator.EQUAL, (Object[])productName));
    }

    public OrderItemRequest<T> withProductName(Operator operator, Object... values){
       return appendSearchCriteria(createProductNameCriteria(operator, values));
    }

    public OrderItemRequest<T> withProductNameIsUnknown(){
       return withProductName(Operator.IS_NULL);
    }

    public OrderItemRequest<T> withProductNameIsKnown(){
       return withProductName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createProductNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderItem.PRODUCT_NAME_PROPERTY, operator, values);
    }

    public OrderItemRequest<T> withProductNameIsNot(String productName){
       return withProductName(Operator.NOT_EQUAL, productName);
    }

    public OrderItemRequest<T> withProductNameIn(String... productName){
       return withProductName(Operator.IN, (Object[])productName);
    }

    public OrderItemRequest<T> withProductNameNotIn(String... productName){
       return withProductName(Operator.NOT_IN, (Object[])productName);
    }
    public OrderItemRequest<T> withProductNameGreaterThan(String productName){
       return withProductName(Operator.GREATER_THAN, productName);
    }

    public OrderItemRequest<T> withProductNameGreaterThanOrEqualTo(String productName){
       return withProductName(Operator.GREATER_THAN_OR_EQUAL, productName);
    }

    public OrderItemRequest<T> withProductNameLessThan(String productName){
       return withProductName(Operator.LESS_THAN, productName);
    }

    public OrderItemRequest<T> withProductNameLessThanOrEqualTo(String productName){
       return withProductName(Operator.LESS_THAN_OR_EQUAL, productName);
    }

    public OrderItemRequest<T> withProductNameBetween(String startOfProductName, String endOfProductName){
       return withProductName(Operator.BETWEEN, startOfProductName, endOfProductName);
    }
    public OrderItemRequest<T> withProductNameStartingWith(String productName){
       return withProductName(Operator.BEGIN_WITH, productName);
    }
    public OrderItemRequest<T> withProductNameContaining(String productName){
       return withProductName(Operator.CONTAIN, productName);
    }

    public OrderItemRequest<T> withProductNameNotContaining(String productName){
       return withProductName(Operator.NOT_CONTAIN, productName);
    }

    public OrderItemRequest<T> withProductNameNotStartingWith(String productName){
       return withProductName(Operator.NOT_BEGIN_WITH, productName);
    }

    public OrderItemRequest<T> withProductNameEndingWith(String productName){
       return withProductName(Operator.END_WITH, productName);
    }

    public OrderItemRequest<T> withProductNameNotEndingWith(String productName){
       return withProductName(Operator.NOT_END_WITH, productName);
    }

    public OrderItemRequest<T> withProductNameIs(String productName){
       return withProductName(Operator.EQUAL, productName);
    }

    public OrderItemRequest<T> withProductNameSoundingLike(String productName){
       return withProductName(Operator.SOUNDS_LIKE, productName);
    }



    public OrderItemRequest<T> filterByQuantity(Integer... quantity){
      if (quantity == null || quantity.length == 0) {
        throw new IllegalArgumentException("filterByQuantity parameter quantity cannot be empty");
      }
      return appendSearchCriteria(createQuantityCriteria(Operator.EQUAL, (Object[])quantity));
    }

    public OrderItemRequest<T> withQuantity(Operator operator, Object... values){
       return appendSearchCriteria(createQuantityCriteria(operator, values));
    }

    public OrderItemRequest<T> withQuantityIsUnknown(){
       return withQuantity(Operator.IS_NULL);
    }

    public OrderItemRequest<T> withQuantityIsKnown(){
       return withQuantity(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createQuantityCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderItem.QUANTITY_PROPERTY, operator, values);
    }

    public OrderItemRequest<T> withQuantityIs(Integer quantity){
       return withQuantity(Operator.EQUAL, quantity);
    }

    public OrderItemRequest<T> withQuantityIsNot(Integer quantity){
       return withQuantity(Operator.NOT_EQUAL, quantity);
    }

    public OrderItemRequest<T> withQuantityIn(Integer... quantity){
       return withQuantity(Operator.IN, (Object[])quantity);
    }

    public OrderItemRequest<T> withQuantityNotIn(Integer... quantity){
       return withQuantity(Operator.NOT_IN, (Object[])quantity);
    }
    public OrderItemRequest<T> withQuantityGreaterThan(Integer quantity){
       return withQuantity(Operator.GREATER_THAN, quantity);
    }

    public OrderItemRequest<T> withQuantityGreaterThanOrEqualTo(Integer quantity){
       return withQuantity(Operator.GREATER_THAN_OR_EQUAL, quantity);
    }

    public OrderItemRequest<T> withQuantityLessThan(Integer quantity){
       return withQuantity(Operator.LESS_THAN, quantity);
    }

    public OrderItemRequest<T> withQuantityLessThanOrEqualTo(Integer quantity){
       return withQuantity(Operator.LESS_THAN_OR_EQUAL, quantity);
    }

    public OrderItemRequest<T> withQuantityBetween(Integer startOfQuantity, Integer endOfQuantity){
       return withQuantity(Operator.BETWEEN, startOfQuantity, endOfQuantity);
    }



    public OrderItemRequest<T> filterByUnitPrice(BigDecimal... unitPrice){
      if (unitPrice == null || unitPrice.length == 0) {
        throw new IllegalArgumentException("filterByUnitPrice parameter unitPrice cannot be empty");
      }
      return appendSearchCriteria(createUnitPriceCriteria(Operator.EQUAL, (Object[])unitPrice));
    }

    public OrderItemRequest<T> withUnitPrice(Operator operator, Object... values){
       return appendSearchCriteria(createUnitPriceCriteria(operator, values));
    }

    public OrderItemRequest<T> withUnitPriceIsUnknown(){
       return withUnitPrice(Operator.IS_NULL);
    }

    public OrderItemRequest<T> withUnitPriceIsKnown(){
       return withUnitPrice(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createUnitPriceCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderItem.UNIT_PRICE_PROPERTY, operator, values);
    }

    public OrderItemRequest<T> withUnitPriceIs(BigDecimal unitPrice){
       return withUnitPrice(Operator.EQUAL, unitPrice);
    }

    public OrderItemRequest<T> withUnitPriceIsNot(BigDecimal unitPrice){
       return withUnitPrice(Operator.NOT_EQUAL, unitPrice);
    }

    public OrderItemRequest<T> withUnitPriceIn(BigDecimal... unitPrice){
       return withUnitPrice(Operator.IN, (Object[])unitPrice);
    }

    public OrderItemRequest<T> withUnitPriceNotIn(BigDecimal... unitPrice){
       return withUnitPrice(Operator.NOT_IN, (Object[])unitPrice);
    }
    public OrderItemRequest<T> withUnitPriceGreaterThan(BigDecimal unitPrice){
       return withUnitPrice(Operator.GREATER_THAN, unitPrice);
    }

    public OrderItemRequest<T> withUnitPriceGreaterThanOrEqualTo(BigDecimal unitPrice){
       return withUnitPrice(Operator.GREATER_THAN_OR_EQUAL, unitPrice);
    }

    public OrderItemRequest<T> withUnitPriceLessThan(BigDecimal unitPrice){
       return withUnitPrice(Operator.LESS_THAN, unitPrice);
    }

    public OrderItemRequest<T> withUnitPriceLessThanOrEqualTo(BigDecimal unitPrice){
       return withUnitPrice(Operator.LESS_THAN_OR_EQUAL, unitPrice);
    }

    public OrderItemRequest<T> withUnitPriceBetween(BigDecimal startOfUnitPrice, BigDecimal endOfUnitPrice){
       return withUnitPrice(Operator.BETWEEN, startOfUnitPrice, endOfUnitPrice);
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
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public OrderItemRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public OrderItemRequest minQuantity(){
        return minQuantityAs(prefix("minOf",OrderItem.QUANTITY_PROPERTY));
    }

    public OrderItemRequest minQuantityAs(String retName){
        super.min(retName, OrderItem.QUANTITY_PROPERTY);
        return this;
    }
    public OrderItemRequest maxQuantity(){
        return maxQuantityAs(prefix("maxOf",OrderItem.QUANTITY_PROPERTY));
    }

    public OrderItemRequest maxQuantityAs(String retName){
        super.max(retName, OrderItem.QUANTITY_PROPERTY);
        return this;
    }
    public OrderItemRequest sumQuantity(){
        return sumQuantityAs(prefix("sumOf",OrderItem.QUANTITY_PROPERTY));
    }

    public OrderItemRequest sumQuantityAs(String retName){
        super.sum(retName, OrderItem.QUANTITY_PROPERTY);
        return this;
    }
    public OrderItemRequest avgQuantity(){
        return avgQuantityAs(prefix("avgOf",OrderItem.QUANTITY_PROPERTY));
    }

    public OrderItemRequest avgQuantityAs(String retName){
        super.avg(retName, OrderItem.QUANTITY_PROPERTY);
        return this;
    }
    public OrderItemRequest standardDeviationQuantity(){
        return standardDeviationQuantityAs(prefix("standardDeviationOf",OrderItem.QUANTITY_PROPERTY));
    }

    public OrderItemRequest standardDeviationQuantityAs(String retName){
        super.standardDeviation(retName, OrderItem.QUANTITY_PROPERTY);
        return this;
    }
    public OrderItemRequest squareRootOfPopulationStandardDeviationQuantity(){
        return squareRootOfPopulationStandardDeviationQuantityAs(prefix("squareRootOfPopulationStandardDeviationOf",OrderItem.QUANTITY_PROPERTY));
    }

    public OrderItemRequest squareRootOfPopulationStandardDeviationQuantityAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, OrderItem.QUANTITY_PROPERTY);
        return this;
    }
    public OrderItemRequest sampleVarianceQuantity(){
        return sampleVarianceQuantityAs(prefix("sampleVarianceOf",OrderItem.QUANTITY_PROPERTY));
    }

    public OrderItemRequest sampleVarianceQuantityAs(String retName){
        super.sampleVariance(retName, OrderItem.QUANTITY_PROPERTY);
        return this;
    }
    public OrderItemRequest samplePopulationVarianceQuantity(){
        return samplePopulationVarianceQuantityAs(prefix("samplePopulationVarianceOf",OrderItem.QUANTITY_PROPERTY));
    }

    public OrderItemRequest samplePopulationVarianceQuantityAs(String retName){
        super.samplePopulationVariance(retName, OrderItem.QUANTITY_PROPERTY);
        return this;
    }
    public OrderItemRequest minUnitPrice(){
        return minUnitPriceAs(prefix("minOf",OrderItem.UNIT_PRICE_PROPERTY));
    }

    public OrderItemRequest minUnitPriceAs(String retName){
        super.min(retName, OrderItem.UNIT_PRICE_PROPERTY);
        return this;
    }
    public OrderItemRequest maxUnitPrice(){
        return maxUnitPriceAs(prefix("maxOf",OrderItem.UNIT_PRICE_PROPERTY));
    }

    public OrderItemRequest maxUnitPriceAs(String retName){
        super.max(retName, OrderItem.UNIT_PRICE_PROPERTY);
        return this;
    }
    public OrderItemRequest sumUnitPrice(){
        return sumUnitPriceAs(prefix("sumOf",OrderItem.UNIT_PRICE_PROPERTY));
    }

    public OrderItemRequest sumUnitPriceAs(String retName){
        super.sum(retName, OrderItem.UNIT_PRICE_PROPERTY);
        return this;
    }
    public OrderItemRequest avgUnitPrice(){
        return avgUnitPriceAs(prefix("avgOf",OrderItem.UNIT_PRICE_PROPERTY));
    }

    public OrderItemRequest avgUnitPriceAs(String retName){
        super.avg(retName, OrderItem.UNIT_PRICE_PROPERTY);
        return this;
    }
    public OrderItemRequest standardDeviationUnitPrice(){
        return standardDeviationUnitPriceAs(prefix("standardDeviationOf",OrderItem.UNIT_PRICE_PROPERTY));
    }

    public OrderItemRequest standardDeviationUnitPriceAs(String retName){
        super.standardDeviation(retName, OrderItem.UNIT_PRICE_PROPERTY);
        return this;
    }
    public OrderItemRequest squareRootOfPopulationStandardDeviationUnitPrice(){
        return squareRootOfPopulationStandardDeviationUnitPriceAs(prefix("squareRootOfPopulationStandardDeviationOf",OrderItem.UNIT_PRICE_PROPERTY));
    }

    public OrderItemRequest squareRootOfPopulationStandardDeviationUnitPriceAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, OrderItem.UNIT_PRICE_PROPERTY);
        return this;
    }
    public OrderItemRequest sampleVarianceUnitPrice(){
        return sampleVarianceUnitPriceAs(prefix("sampleVarianceOf",OrderItem.UNIT_PRICE_PROPERTY));
    }

    public OrderItemRequest sampleVarianceUnitPriceAs(String retName){
        super.sampleVariance(retName, OrderItem.UNIT_PRICE_PROPERTY);
        return this;
    }
    public OrderItemRequest samplePopulationVarianceUnitPrice(){
        return samplePopulationVarianceUnitPriceAs(prefix("samplePopulationVarianceOf",OrderItem.UNIT_PRICE_PROPERTY));
    }

    public OrderItemRequest samplePopulationVarianceUnitPriceAs(String retName){
        super.samplePopulationVariance(retName, OrderItem.UNIT_PRICE_PROPERTY);
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

    public OrderItemRequest<T> groupByProductName(){
       groupBy(OrderItem.PRODUCT_NAME_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByProductNameAs(String retName){
       groupBy(retName, OrderItem.PRODUCT_NAME_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByProductNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderItem.PRODUCT_NAME_PROPERTY, function);
       return this;
    }

    public OrderItemRequest<T> groupByQuantity(){
       groupBy(OrderItem.QUANTITY_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByQuantityAs(String retName){
       groupBy(retName, OrderItem.QUANTITY_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByQuantityWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderItem.QUANTITY_PROPERTY, function);
       return this;
    }

    public OrderItemRequest<T> groupByUnitPrice(){
       groupBy(OrderItem.UNIT_PRICE_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByUnitPriceAs(String retName){
       groupBy(retName, OrderItem.UNIT_PRICE_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByUnitPriceWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderItem.UNIT_PRICE_PROPERTY, function);
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

    public OrderItemRequest<T> orderByProductNameAscending(){
       addOrderByAscending(OrderItem.PRODUCT_NAME_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByProductNameDescending(){
       addOrderByDescending(OrderItem.PRODUCT_NAME_PROPERTY);
       return this;
    }
    public OrderItemRequest<T> orderByProductNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(OrderItem.PRODUCT_NAME_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByProductNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(OrderItem.PRODUCT_NAME_PROPERTY);
       return this;
    }
    public OrderItemRequest<T> orderByQuantityAscending(){
       addOrderByAscending(OrderItem.QUANTITY_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByQuantityDescending(){
       addOrderByDescending(OrderItem.QUANTITY_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByUnitPriceAscending(){
       addOrderByAscending(OrderItem.UNIT_PRICE_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByUnitPriceDescending(){
       addOrderByDescending(OrderItem.UNIT_PRICE_PROPERTY);
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
       addFacet(facetName, OrderItem.CUSTOMER_ORDER_PROPERTY, customerOrder, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByCustomerOrderAs", facetName, OrderItem.CUSTOMER_ORDER_PROPERTY));
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