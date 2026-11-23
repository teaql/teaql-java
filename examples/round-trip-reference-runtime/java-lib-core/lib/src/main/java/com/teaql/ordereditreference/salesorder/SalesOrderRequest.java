
package com.teaql.ordereditreference.salesorder;

import com.teaql.ordereditreference.Q;
import com.teaql.ordereditreference.merchant.Merchant;
import com.teaql.ordereditreference.merchant.MerchantRequest;
import com.teaql.ordereditreference.orderitem.OrderItem;
import com.teaql.ordereditreference.orderitem.OrderItemRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.util.Objects;

public class SalesOrderRequest<T extends SalesOrder> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public SalesOrderRequest(Class<T> returnType){
        this(returnType, () -> (T) new SalesOrder());
    }

    protected SalesOrderRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public SalesOrderRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public SalesOrderRequest<T> copy(){
        return (SalesOrderRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new SalesOrderRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public SalesOrderRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public SalesOrderRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public SalesOrderRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public SalesOrderRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public SalesOrderRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public SalesOrderRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public SalesOrderRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public SalesOrderRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public SalesOrderRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public SalesOrderRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (SalesOrderRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public SalesOrderRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public SalesOrderRequest<T> matchingAnyOf(SalesOrderRequest salesOrder){
        super.internalMatchAny(salesOrder);
        return this;
    }

    public SalesOrderRequest<T> matchingIdsOf(SalesOrderRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                SalesOrder.ID_PROPERTY, request, SalesOrder.ID_PROPERTY));
    }

    public SalesOrderRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public SalesOrderRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public SalesOrderRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public SalesOrderRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectMerchantIdOnly().selectOrderNumber().selectVersion();
    }

    public SalesOrderRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public SalesOrderRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(SalesOrder.ID_PROPERTY);
        selectProperty(SalesOrder.MERCHANT_PROPERTY);
        selectProperty(SalesOrder.ORDER_NUMBER_PROPERTY);
        selectProperty(SalesOrder.VERSION_PROPERTY);
        return this;
    }

    public SalesOrderRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectMerchant().selectOrderNumber().selectVersion();
    }

    public SalesOrderRequest<T> selectChildren(){
        super.selectAny();
        selectOrderItemList();
        return selectId().selectMerchant().selectOrderNumber().selectVersion();
    }


    public SalesOrderRequest<T> selectId(){
       selectProperty(SalesOrder.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SalesOrderRequest<T> unselectId(){
       unselectProperty(SalesOrder.ID_PROPERTY);
       return this;
    }
    public SalesOrderRequest<T> selectMerchantIdOnly(){
       selectProperty(SalesOrder.MERCHANT_PROPERTY);
       return this;
    }

    public SalesOrderRequest<T> selectMerchant(){
        return selectMerchantWith(Q.merchants().unlimited().selectSelf());
    }

    public SalesOrderRequest<T> selectMerchantWith(MerchantRequest merchant){
       selectProperty(SalesOrder.MERCHANT_PROPERTY);
       enhanceRelation(SalesOrder.MERCHANT_PROPERTY, merchant,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectMerchantWith", null, SalesOrder.MERCHANT_PROPERTY));
       return this;
    }

    public SalesOrderRequest<T> unselectMerchant(){
       unselectProperty(SalesOrder.MERCHANT_PROPERTY);
       return this;
    }
    public SalesOrderRequest<T> selectOrderNumber(){
       selectProperty(SalesOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    /**
     * fill the orderNumber with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  orderNumber) to fetch orderNumber property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SalesOrderRequest<T> unselectOrderNumber(){
       unselectProperty(SalesOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }
    public SalesOrderRequest<T> selectVersion(){
       selectProperty(SalesOrder.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SalesOrderRequest<T> unselectVersion(){
       unselectProperty(SalesOrder.VERSION_PROPERTY);
       return this;
    }
    public SalesOrderRequest<T> selectOrderItemList(){
       return selectOrderItemListWith(Q.orderItems().selectSelf());
    }

    public SalesOrderRequest<T> selectOrderItemListWith(OrderItemRequest orderItemList){
       enhanceRelation(SalesOrder.ORDER_ITEM_LIST_PROPERTY, orderItemList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectOrderItemListWith", null, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
       return this;
    }

    public SalesOrderRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SalesOrder.ID_PROPERTY, operator, values);
    }

    public SalesOrderRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public SalesOrderRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public SalesOrderRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public SalesOrderRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public SalesOrderRequest<T> filterByMerchant(Merchant... merchant){
      if (merchant == null || merchant.length == 0) {
        throw new IllegalArgumentException("filterByMerchant parameter merchant cannot be empty");
      }
      return appendSearchCriteria(createMerchantCriteria(Operator.EQUAL, (Object[])merchant));
    }

    public SalesOrderRequest<T> withMerchant(Operator operator, Object... values){
       return appendSearchCriteria(createMerchantCriteria(operator, values));
    }

    public SalesOrderRequest<T> withMerchantIsUnknown(){
       return withMerchant(Operator.IS_NULL);
    }

    public SalesOrderRequest<T> withMerchantIsKnown(){
       return withMerchant(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createMerchantCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SalesOrder.MERCHANT_PROPERTY, operator, values);
    }

    public SalesOrderRequest<T> filterByMerchant(Long merchant){
      if(merchant == null){
         return this;
      }
      return withMerchant(Operator.EQUAL, merchant);
    }
    public SalesOrderRequest<T> withMerchantMatching(MerchantRequest merchant){
       return appendSearchCriteria(new SubQuerySearchCriteria(SalesOrder.MERCHANT_PROPERTY, merchant, Merchant.ID_PROPERTY));
    }

    public SalesOrderRequest<T> withoutMerchantMatching(MerchantRequest merchant){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(SalesOrder.MERCHANT_PROPERTY, merchant, Merchant.ID_PROPERTY)));
    }

    public SalesOrderRequest<T> filterByOrderNumber(String... orderNumber){
      if (orderNumber == null || orderNumber.length == 0) {
        throw new IllegalArgumentException("filterByOrderNumber parameter orderNumber cannot be empty");
      }
      return appendSearchCriteria(createOrderNumberCriteria(Operator.EQUAL, (Object[])orderNumber));
    }

    public SalesOrderRequest<T> withOrderNumber(Operator operator, Object... values){
       return appendSearchCriteria(createOrderNumberCriteria(operator, values));
    }

    public SalesOrderRequest<T> withOrderNumberIsUnknown(){
       return withOrderNumber(Operator.IS_NULL);
    }

    public SalesOrderRequest<T> withOrderNumberIsKnown(){
       return withOrderNumber(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createOrderNumberCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SalesOrder.ORDER_NUMBER_PROPERTY, operator, values);
    }

    public SalesOrderRequest<T> withOrderNumberIsNot(String orderNumber){
       return withOrderNumber(Operator.NOT_EQUAL, orderNumber);
    }

    public SalesOrderRequest<T> withOrderNumberIn(String... orderNumber){
       return withOrderNumber(Operator.IN, (Object[])orderNumber);
    }

    public SalesOrderRequest<T> withOrderNumberNotIn(String... orderNumber){
       return withOrderNumber(Operator.NOT_IN, (Object[])orderNumber);
    }
    public SalesOrderRequest<T> withOrderNumberGreaterThan(String orderNumber){
       return withOrderNumber(Operator.GREATER_THAN, orderNumber);
    }

    public SalesOrderRequest<T> withOrderNumberGreaterThanOrEqualTo(String orderNumber){
       return withOrderNumber(Operator.GREATER_THAN_OR_EQUAL, orderNumber);
    }

    public SalesOrderRequest<T> withOrderNumberLessThan(String orderNumber){
       return withOrderNumber(Operator.LESS_THAN, orderNumber);
    }

    public SalesOrderRequest<T> withOrderNumberLessThanOrEqualTo(String orderNumber){
       return withOrderNumber(Operator.LESS_THAN_OR_EQUAL, orderNumber);
    }

    public SalesOrderRequest<T> withOrderNumberBetween(String startOfOrderNumber, String endOfOrderNumber){
       return withOrderNumber(Operator.BETWEEN, startOfOrderNumber, endOfOrderNumber);
    }
    public SalesOrderRequest<T> withOrderNumberStartingWith(String orderNumber){
       return withOrderNumber(Operator.BEGIN_WITH, orderNumber);
    }
    public SalesOrderRequest<T> withOrderNumberContaining(String orderNumber){
       return withOrderNumber(Operator.CONTAIN, orderNumber);
    }

    public SalesOrderRequest<T> withOrderNumberNotContaining(String orderNumber){
       return withOrderNumber(Operator.NOT_CONTAIN, orderNumber);
    }

    public SalesOrderRequest<T> withOrderNumberNotStartingWith(String orderNumber){
       return withOrderNumber(Operator.NOT_BEGIN_WITH, orderNumber);
    }

    public SalesOrderRequest<T> withOrderNumberEndingWith(String orderNumber){
       return withOrderNumber(Operator.END_WITH, orderNumber);
    }

    public SalesOrderRequest<T> withOrderNumberNotEndingWith(String orderNumber){
       return withOrderNumber(Operator.NOT_END_WITH, orderNumber);
    }

    public SalesOrderRequest<T> withOrderNumberIs(String orderNumber){
       return withOrderNumber(Operator.EQUAL, orderNumber);
    }

    public SalesOrderRequest<T> withOrderNumberSoundingLike(String orderNumber){
       return withOrderNumber(Operator.SOUNDS_LIKE, orderNumber);
    }



    public SalesOrderRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public SalesOrderRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public SalesOrderRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public SalesOrderRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SalesOrder.VERSION_PROPERTY, operator, values);
    }

    public SalesOrderRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public SalesOrderRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public SalesOrderRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public SalesOrderRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public SalesOrderRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public SalesOrderRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public SalesOrderRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public SalesOrderRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public SalesOrderRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public SalesOrderRequest<T> withOrderItemListMatching(OrderItemRequest orderItemRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(SalesOrder.ID_PROPERTY, orderItemRequest, OrderItem.SALES_ORDER_PROPERTY));
    }

    public SalesOrderRequest<T> withoutOrderItemListMatching(OrderItemRequest orderItemRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(SalesOrder.ID_PROPERTY, orderItemRequest, OrderItem.SALES_ORDER_PROPERTY)));
    }

    public SalesOrderRequest<T> haveOrderItems(){
        return withOrderItemListMatching(Q.orderItems().unlimited());
    }

    public SalesOrderRequest<T> haveNoOrderItems(){
        return withoutOrderItemListMatching(Q.orderItems().unlimited());
    }

    public SalesOrderRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public SalesOrderRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public SalesOrderRequest<T> groupByMerchantWithDetails(){
       return groupByMerchantWithDetails(Q.merchants().unlimited());
    }

    public SalesOrderRequest<T> groupByMerchantWithDetails(MerchantRequest subRequest){
       aggregate(SalesOrder.MERCHANT_PROPERTY, subRequest);
       return this;
    }



    public SalesOrderRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public SalesOrderRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, SalesOrder.ID_PROPERTY);
        return this;
    }
    public SalesOrderRequest<T> countDistinctMerchant(){
        return countDistinctMerchantAs("merchant_distinct_count");
    }

    public SalesOrderRequest<T> countDistinctMerchantAs(String alias){
        super.countDistinctProperty(alias, SalesOrder.MERCHANT_PROPERTY);
        return this;
    }
    public SalesOrderRequest<T> countDistinctOrderNumber(){
        return countDistinctOrderNumberAs("order_number_distinct_count");
    }

    public SalesOrderRequest<T> countDistinctOrderNumberAs(String alias){
        super.countDistinctProperty(alias, SalesOrder.ORDER_NUMBER_PROPERTY);
        return this;
    }
    public SalesOrderRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public SalesOrderRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, SalesOrder.VERSION_PROPERTY);
        return this;
    }
    public SalesOrderRequest<T> groupByOrderItemsWithDetails(OrderItemRequest subRequest){
       aggregate(SalesOrder.ORDER_ITEM_LIST_PROPERTY, subRequest);
       return this;
    }

    public SalesOrderRequest<T> groupById(){
       groupBy(SalesOrder.ID_PROPERTY);
       return this;
    }

    public SalesOrderRequest<T> groupByIdAs(String retName){
       groupBy(retName, SalesOrder.ID_PROPERTY);
       return this;
    }

    public SalesOrderRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, SalesOrder.ID_PROPERTY, function);
       return this;
    }
    public SalesOrderRequest<T> groupByMerchantWith(MerchantRequest subRequest){
       groupBy(SalesOrder.MERCHANT_PROPERTY, subRequest,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "groupByMerchantWith", null, "merchant"));
       return this;
    }
    public SalesOrderRequest<T> groupByMerchant(){
       groupBy(SalesOrder.MERCHANT_PROPERTY);
       return this;
    }

    public SalesOrderRequest<T> groupByMerchantAs(String retName){
       groupBy(retName, SalesOrder.MERCHANT_PROPERTY);
       return this;
    }

    public SalesOrderRequest<T> groupByMerchantWithFunction(String retName, AggrFunction function){
       groupBy(retName, SalesOrder.MERCHANT_PROPERTY, function);
       return this;
    }

    public SalesOrderRequest<T> groupByOrderNumber(){
       groupBy(SalesOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    public SalesOrderRequest<T> groupByOrderNumberAs(String retName){
       groupBy(retName, SalesOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    public SalesOrderRequest<T> groupByOrderNumberWithFunction(String retName, AggrFunction function){
       groupBy(retName, SalesOrder.ORDER_NUMBER_PROPERTY, function);
       return this;
    }

    public SalesOrderRequest<T> groupByVersion(){
       groupBy(SalesOrder.VERSION_PROPERTY);
       return this;
    }

    public SalesOrderRequest<T> groupByVersionAs(String retName){
       groupBy(retName, SalesOrder.VERSION_PROPERTY);
       return this;
    }

    public SalesOrderRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, SalesOrder.VERSION_PROPERTY, function);
       return this;
    }



    public SalesOrderRequest<T> orderByIdAscending(){
       addOrderByAscending(SalesOrder.ID_PROPERTY);
       return this;
    }

    public SalesOrderRequest<T> orderByIdDescending(){
       addOrderByDescending(SalesOrder.ID_PROPERTY);
       return this;
    }

    public SalesOrderRequest<T> orderByMerchantAscending(){
       addOrderByAscending(SalesOrder.MERCHANT_PROPERTY);
       return this;
    }

    public SalesOrderRequest<T> orderByMerchantDescending(){
       addOrderByDescending(SalesOrder.MERCHANT_PROPERTY);
       return this;
    }

    public SalesOrderRequest<T> orderByOrderNumberAscending(){
       addOrderByAscending(SalesOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    public SalesOrderRequest<T> orderByOrderNumberDescending(){
       addOrderByDescending(SalesOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }
    public SalesOrderRequest<T> orderByOrderNumberAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(SalesOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    public SalesOrderRequest<T> orderByOrderNumberDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(SalesOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }
    public SalesOrderRequest<T> orderByVersionAscending(){
       addOrderByAscending(SalesOrder.VERSION_PROPERTY);
       return this;
    }

    public SalesOrderRequest<T> orderByVersionDescending(){
       addOrderByDescending(SalesOrder.VERSION_PROPERTY);
       return this;
    }


    public SalesOrderRequest<T> statsFromOrderItemsAs(String name, OrderItemRequest subRequest){
       return statsFromOrderItemsAs(name, subRequest, false);
    }

    public SalesOrderRequest<T> statsFromOrderItemsAs(String name, OrderItemRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, OrderItem.SALES_ORDER_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromOrderItemsAs", name, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
       return this;
    }

    public SalesOrderRequest<T> statsFromOrderItems(OrderItemRequest subRequest){
       return statsFromOrderItemsAs(REFINEMENTS, subRequest);
    }
    public MerchantRequest rollUpToMerchant(){
       MerchantRequest merchant = Q.merchants().unlimited();
       this.internalRollUpTo("merchant", merchant,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "rollUpToMerchant", null, "merchant"));
       return merchant;
    }



    public SalesOrderRequest<T> countOrderItems(){
        addRelationCount("Count", Q.orderItems().unlimited(), OrderItem.SALES_ORDER_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderItems", null, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public SalesOrderRequest<T> countOrderItemsAs(String name){
        addRelationCount(name, Q.orderItems().unlimited(), OrderItem.SALES_ORDER_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderItemsAs", name, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public SalesOrderRequest<T> countOrderItemsWith(String name, OrderItemRequest subRequest){
        addRelationCount(name, subRequest, OrderItem.SALES_ORDER_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderItemsWith", name, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public SalesOrderRequest<T> minQuantityOfOrderItems(){
        addRelationScalarAggregate("minQuantityOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.SALES_ORDER_PROPERTY, OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minQuantityOfOrderItems", null, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public SalesOrderRequest<T> minQuantityOfOrderItemsAs(String name){
        return minQuantityOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public SalesOrderRequest<T> minQuantityOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.SALES_ORDER_PROPERTY,
            OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minQuantityOfOrderItemsAs", name, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public SalesOrderRequest<T> maxQuantityOfOrderItems(){
        addRelationScalarAggregate("maxQuantityOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.SALES_ORDER_PROPERTY, OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxQuantityOfOrderItems", null, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public SalesOrderRequest<T> maxQuantityOfOrderItemsAs(String name){
        return maxQuantityOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public SalesOrderRequest<T> maxQuantityOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.SALES_ORDER_PROPERTY,
            OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxQuantityOfOrderItemsAs", name, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public SalesOrderRequest<T> sumQuantityOfOrderItems(){
        addRelationScalarAggregate("sumQuantityOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.SALES_ORDER_PROPERTY, OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumQuantityOfOrderItems", null, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public SalesOrderRequest<T> sumQuantityOfOrderItemsAs(String name){
        return sumQuantityOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public SalesOrderRequest<T> sumQuantityOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.SALES_ORDER_PROPERTY,
            OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumQuantityOfOrderItemsAs", name, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public SalesOrderRequest<T> avgQuantityOfOrderItems(){
        addRelationScalarAggregate("avgQuantityOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.SALES_ORDER_PROPERTY, OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgQuantityOfOrderItems", null, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public SalesOrderRequest<T> avgQuantityOfOrderItemsAs(String name){
        return avgQuantityOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public SalesOrderRequest<T> avgQuantityOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.SALES_ORDER_PROPERTY,
            OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgQuantityOfOrderItemsAs", name, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public SalesOrderRequest<T> standardDeviationQuantityOfOrderItems(){
        addRelationScalarAggregate("stdDevQuantityOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.SALES_ORDER_PROPERTY, OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationQuantityOfOrderItems", null, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public SalesOrderRequest<T> standardDeviationQuantityOfOrderItemsAs(String name){
        return standardDeviationQuantityOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public SalesOrderRequest<T> standardDeviationQuantityOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.SALES_ORDER_PROPERTY,
            OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationQuantityOfOrderItemsAs", name, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public SalesOrderRequest<T> squareRootOfPopulationStandardDeviationQuantityOfOrderItems(){
        addRelationScalarAggregate("stdDevPopQuantityOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.SALES_ORDER_PROPERTY, OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationQuantityOfOrderItems", null, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public SalesOrderRequest<T> squareRootOfPopulationStandardDeviationQuantityOfOrderItemsAs(String name){
        return squareRootOfPopulationStandardDeviationQuantityOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public SalesOrderRequest<T> squareRootOfPopulationStandardDeviationQuantityOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.SALES_ORDER_PROPERTY,
            OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationQuantityOfOrderItemsAs", name, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public SalesOrderRequest<T> sampleVarianceQuantityOfOrderItems(){
        addRelationScalarAggregate("varSampQuantityOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.SALES_ORDER_PROPERTY, OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceQuantityOfOrderItems", null, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public SalesOrderRequest<T> sampleVarianceQuantityOfOrderItemsAs(String name){
        return sampleVarianceQuantityOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public SalesOrderRequest<T> sampleVarianceQuantityOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.SALES_ORDER_PROPERTY,
            OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceQuantityOfOrderItemsAs", name, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public SalesOrderRequest<T> samplePopulationVarianceQuantityOfOrderItems(){
        addRelationScalarAggregate("varPopQuantityOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.SALES_ORDER_PROPERTY, OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceQuantityOfOrderItems", null, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public SalesOrderRequest<T> samplePopulationVarianceQuantityOfOrderItemsAs(String name){
        return samplePopulationVarianceQuantityOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public SalesOrderRequest<T> samplePopulationVarianceQuantityOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.SALES_ORDER_PROPERTY,
            OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceQuantityOfOrderItemsAs", name, SalesOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

   public SalesOrderRequest<T> facetByMerchantAs(String facetName, MerchantRequest merchant){
       return facetByMerchantAs(facetName, merchant, true);
   }

   public SalesOrderRequest<T> facetByMerchantAs(String facetName, MerchantRequest merchant, boolean includeAllFacets){
       addFacet(facetName, SalesOrder.MERCHANT_PROPERTY, merchant, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByMerchantAs", facetName, SalesOrder.MERCHANT_PROPERTY));
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public SalesOrderRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public SalesOrderRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public SalesOrderRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public SalesOrderRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public SalesOrderRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public SalesOrderRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}