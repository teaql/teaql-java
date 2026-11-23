
package com.teaql.ordereditreference.orderitem;

import com.teaql.ordereditreference.Q;
import com.teaql.ordereditreference.salesorder.SalesOrder;
import com.teaql.ordereditreference.salesorder.SalesOrderRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.util.Objects;

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

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public OrderItemRequest<T> copy(){
        return (OrderItemRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new OrderItemRequest<>((Class<T>) returnType(), this::internalNewEntity);
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

    public OrderItemRequest<T> matchingIdsOf(OrderItemRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                OrderItem.ID_PROPERTY, request, OrderItem.ID_PROPERTY));
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
        return selectId().selectSalesOrderIdOnly().selectSku().selectQuantity().selectVersion();
    }

    public OrderItemRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public OrderItemRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(OrderItem.ID_PROPERTY);
        selectProperty(OrderItem.SALES_ORDER_PROPERTY);
        selectProperty(OrderItem.SKU_PROPERTY);
        selectProperty(OrderItem.QUANTITY_PROPERTY);
        selectProperty(OrderItem.VERSION_PROPERTY);
        return this;
    }

    public OrderItemRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectSalesOrder().selectSku().selectQuantity().selectVersion();
    }

    public OrderItemRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectSalesOrder().selectSku().selectQuantity().selectVersion();
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
    public OrderItemRequest<T> selectSalesOrderIdOnly(){
       selectProperty(OrderItem.SALES_ORDER_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> selectSalesOrder(){
        return selectSalesOrderWith(Q.salesOrders().unlimited().selectSelf());
    }

    public OrderItemRequest<T> selectSalesOrderWith(SalesOrderRequest salesOrder){
       selectProperty(OrderItem.SALES_ORDER_PROPERTY);
       enhanceRelation(OrderItem.SALES_ORDER_PROPERTY, salesOrder,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectSalesOrderWith", null, OrderItem.SALES_ORDER_PROPERTY));
       return this;
    }

    public OrderItemRequest<T> unselectSalesOrder(){
       unselectProperty(OrderItem.SALES_ORDER_PROPERTY);
       return this;
    }
    public OrderItemRequest<T> selectSku(){
       selectProperty(OrderItem.SKU_PROPERTY);
       return this;
    }

    /**
     * fill the sku with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  sku) to fetch sku property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderItemRequest<T> unselectSku(){
       unselectProperty(OrderItem.SKU_PROPERTY);
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



    public OrderItemRequest<T> filterBySalesOrder(SalesOrder... salesOrder){
      if (salesOrder == null || salesOrder.length == 0) {
        throw new IllegalArgumentException("filterBySalesOrder parameter salesOrder cannot be empty");
      }
      return appendSearchCriteria(createSalesOrderCriteria(Operator.EQUAL, (Object[])salesOrder));
    }

    public OrderItemRequest<T> withSalesOrder(Operator operator, Object... values){
       return appendSearchCriteria(createSalesOrderCriteria(operator, values));
    }

    public OrderItemRequest<T> withSalesOrderIsUnknown(){
       return withSalesOrder(Operator.IS_NULL);
    }

    public OrderItemRequest<T> withSalesOrderIsKnown(){
       return withSalesOrder(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createSalesOrderCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderItem.SALES_ORDER_PROPERTY, operator, values);
    }

    public OrderItemRequest<T> filterBySalesOrder(Long salesOrder){
      if(salesOrder == null){
         return this;
      }
      return withSalesOrder(Operator.EQUAL, salesOrder);
    }
    public OrderItemRequest<T> withSalesOrderMatching(SalesOrderRequest salesOrder){
       return appendSearchCriteria(new SubQuerySearchCriteria(OrderItem.SALES_ORDER_PROPERTY, salesOrder, SalesOrder.ID_PROPERTY));
    }

    public OrderItemRequest<T> withoutSalesOrderMatching(SalesOrderRequest salesOrder){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(OrderItem.SALES_ORDER_PROPERTY, salesOrder, SalesOrder.ID_PROPERTY)));
    }

    public OrderItemRequest<T> filterBySku(String... sku){
      if (sku == null || sku.length == 0) {
        throw new IllegalArgumentException("filterBySku parameter sku cannot be empty");
      }
      return appendSearchCriteria(createSkuCriteria(Operator.EQUAL, (Object[])sku));
    }

    public OrderItemRequest<T> withSku(Operator operator, Object... values){
       return appendSearchCriteria(createSkuCriteria(operator, values));
    }

    public OrderItemRequest<T> withSkuIsUnknown(){
       return withSku(Operator.IS_NULL);
    }

    public OrderItemRequest<T> withSkuIsKnown(){
       return withSku(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createSkuCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderItem.SKU_PROPERTY, operator, values);
    }

    public OrderItemRequest<T> withSkuIsNot(String sku){
       return withSku(Operator.NOT_EQUAL, sku);
    }

    public OrderItemRequest<T> withSkuIn(String... sku){
       return withSku(Operator.IN, (Object[])sku);
    }

    public OrderItemRequest<T> withSkuNotIn(String... sku){
       return withSku(Operator.NOT_IN, (Object[])sku);
    }
    public OrderItemRequest<T> withSkuGreaterThan(String sku){
       return withSku(Operator.GREATER_THAN, sku);
    }

    public OrderItemRequest<T> withSkuGreaterThanOrEqualTo(String sku){
       return withSku(Operator.GREATER_THAN_OR_EQUAL, sku);
    }

    public OrderItemRequest<T> withSkuLessThan(String sku){
       return withSku(Operator.LESS_THAN, sku);
    }

    public OrderItemRequest<T> withSkuLessThanOrEqualTo(String sku){
       return withSku(Operator.LESS_THAN_OR_EQUAL, sku);
    }

    public OrderItemRequest<T> withSkuBetween(String startOfSku, String endOfSku){
       return withSku(Operator.BETWEEN, startOfSku, endOfSku);
    }
    public OrderItemRequest<T> withSkuStartingWith(String sku){
       return withSku(Operator.BEGIN_WITH, sku);
    }
    public OrderItemRequest<T> withSkuContaining(String sku){
       return withSku(Operator.CONTAIN, sku);
    }

    public OrderItemRequest<T> withSkuNotContaining(String sku){
       return withSku(Operator.NOT_CONTAIN, sku);
    }

    public OrderItemRequest<T> withSkuNotStartingWith(String sku){
       return withSku(Operator.NOT_BEGIN_WITH, sku);
    }

    public OrderItemRequest<T> withSkuEndingWith(String sku){
       return withSku(Operator.END_WITH, sku);
    }

    public OrderItemRequest<T> withSkuNotEndingWith(String sku){
       return withSku(Operator.NOT_END_WITH, sku);
    }

    public OrderItemRequest<T> withSkuIs(String sku){
       return withSku(Operator.EQUAL, sku);
    }

    public OrderItemRequest<T> withSkuSoundingLike(String sku){
       return withSku(Operator.SOUNDS_LIKE, sku);
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
    public OrderItemRequest<T> groupBySalesOrderWithDetails(){
       return groupBySalesOrderWithDetails(Q.salesOrders().unlimited());
    }

    public OrderItemRequest<T> groupBySalesOrderWithDetails(SalesOrderRequest subRequest){
       aggregate(OrderItem.SALES_ORDER_PROPERTY, subRequest);
       return this;
    }




    public OrderItemRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public OrderItemRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, OrderItem.ID_PROPERTY);
        return this;
    }
    public OrderItemRequest<T> countDistinctSalesOrder(){
        return countDistinctSalesOrderAs("sales_order_distinct_count");
    }

    public OrderItemRequest<T> countDistinctSalesOrderAs(String alias){
        super.countDistinctProperty(alias, OrderItem.SALES_ORDER_PROPERTY);
        return this;
    }
    public OrderItemRequest<T> countDistinctSku(){
        return countDistinctSkuAs("sku_distinct_count");
    }

    public OrderItemRequest<T> countDistinctSkuAs(String alias){
        super.countDistinctProperty(alias, OrderItem.SKU_PROPERTY);
        return this;
    }
    public OrderItemRequest<T> countDistinctQuantity(){
        return countDistinctQuantityAs("quantity_distinct_count");
    }

    public OrderItemRequest<T> countDistinctQuantityAs(String alias){
        super.countDistinctProperty(alias, OrderItem.QUANTITY_PROPERTY);
        return this;
    }
    public OrderItemRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public OrderItemRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, OrderItem.VERSION_PROPERTY);
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
    public OrderItemRequest<T> groupBySalesOrderWith(SalesOrderRequest subRequest){
       groupBy(OrderItem.SALES_ORDER_PROPERTY, subRequest,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "groupBySalesOrderWith", null, "sales_order"));
       return this;
    }
    public OrderItemRequest<T> groupBySalesOrder(){
       groupBy(OrderItem.SALES_ORDER_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupBySalesOrderAs(String retName){
       groupBy(retName, OrderItem.SALES_ORDER_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupBySalesOrderWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderItem.SALES_ORDER_PROPERTY, function);
       return this;
    }

    public OrderItemRequest<T> groupBySku(){
       groupBy(OrderItem.SKU_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupBySkuAs(String retName){
       groupBy(retName, OrderItem.SKU_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupBySkuWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderItem.SKU_PROPERTY, function);
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

    public OrderItemRequest<T> orderBySalesOrderAscending(){
       addOrderByAscending(OrderItem.SALES_ORDER_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderBySalesOrderDescending(){
       addOrderByDescending(OrderItem.SALES_ORDER_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderBySkuAscending(){
       addOrderByAscending(OrderItem.SKU_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderBySkuDescending(){
       addOrderByDescending(OrderItem.SKU_PROPERTY);
       return this;
    }
    public OrderItemRequest<T> orderBySkuAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(OrderItem.SKU_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderBySkuDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(OrderItem.SKU_PROPERTY);
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

    public OrderItemRequest<T> orderByVersionAscending(){
       addOrderByAscending(OrderItem.VERSION_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByVersionDescending(){
       addOrderByDescending(OrderItem.VERSION_PROPERTY);
       return this;
    }


    public SalesOrderRequest rollUpToSalesOrder(){
       SalesOrderRequest salesOrder = Q.salesOrders().unlimited();
       this.internalRollUpTo("sales_order", salesOrder,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "rollUpToSalesOrder", null, "sales_order"));
       return salesOrder;
    }





   public OrderItemRequest<T> facetBySalesOrderAs(String facetName, SalesOrderRequest salesOrder){
       return facetBySalesOrderAs(facetName, salesOrder, true);
   }

   public OrderItemRequest<T> facetBySalesOrderAs(String facetName, SalesOrderRequest salesOrder, boolean includeAllFacets){
       addFacet(facetName, OrderItem.SALES_ORDER_PROPERTY, salesOrder, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetBySalesOrderAs", facetName, OrderItem.SALES_ORDER_PROPERTY));
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