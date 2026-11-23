
package com.teaql.checkerfixadvanced.tenant;

import com.teaql.checkerfixadvanced.Q;
import com.teaql.checkerfixadvanced.orderitem.OrderItem;
import com.teaql.checkerfixadvanced.orderitem.OrderItemRequest;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrderRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;

public class TenantRequest<T extends Tenant> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public TenantRequest(Class<T> returnType){
        this(returnType, () -> (T) new Tenant());
    }

    protected TenantRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public TenantRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public TenantRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public TenantRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public TenantRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public TenantRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public TenantRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public TenantRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public TenantRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public TenantRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public TenantRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public TenantRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (TenantRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public TenantRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public TenantRequest<T> matchingAnyOf(TenantRequest tenant){
        super.internalMatchAny(tenant);
        return this;
    }

    public TenantRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public TenantRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public TenantRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public TenantRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectName().selectVersion();
    }

    public TenantRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public TenantRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(Tenant.ID_PROPERTY);
        selectProperty(Tenant.NAME_PROPERTY);
        selectProperty(Tenant.VERSION_PROPERTY);
        return this;
    }

    public TenantRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectName().selectVersion();
    }

    public TenantRequest<T> selectChildren(){
        super.selectAny();
        selectPurchaseOrderList().selectOrderItemList();
        return selectId().selectName().selectVersion();
    }


    public TenantRequest<T> selectId(){
       selectProperty(Tenant.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public TenantRequest<T> unselectId(){
       unselectProperty(Tenant.ID_PROPERTY);
       return this;
    }
    public TenantRequest<T> selectName(){
       selectProperty(Tenant.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public TenantRequest<T> unselectName(){
       unselectProperty(Tenant.NAME_PROPERTY);
       return this;
    }
    public TenantRequest<T> selectVersion(){
       selectProperty(Tenant.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public TenantRequest<T> unselectVersion(){
       unselectProperty(Tenant.VERSION_PROPERTY);
       return this;
    }
    public TenantRequest<T> selectPurchaseOrderList(){
       return selectPurchaseOrderListWith(Q.purchaseOrders().selectSelf());
    }

    public TenantRequest<T> selectPurchaseOrderListWith(PurchaseOrderRequest purchaseOrderList){
       enhanceRelation(Tenant.PURCHASE_ORDER_LIST_PROPERTY, purchaseOrderList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectPurchaseOrderListWith", null, Tenant.PURCHASE_ORDER_LIST_PROPERTY));
       return this;
    }
    public TenantRequest<T> selectOrderItemList(){
       return selectOrderItemListWith(Q.orderItems().selectSelf());
    }

    public TenantRequest<T> selectOrderItemListWith(OrderItemRequest orderItemList){
       enhanceRelation(Tenant.ORDER_ITEM_LIST_PROPERTY, orderItemList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectOrderItemListWith", null, Tenant.ORDER_ITEM_LIST_PROPERTY));
       return this;
    }

    public TenantRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Tenant.ID_PROPERTY, operator, values);
    }

    public TenantRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public TenantRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public TenantRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public TenantRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public TenantRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public TenantRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public TenantRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public TenantRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Tenant.NAME_PROPERTY, operator, values);
    }

    public TenantRequest<T> withNameIsNot(String name){
       return withName(Operator.NOT_EQUAL, name);
    }

    public TenantRequest<T> withNameIn(String... name){
       return withName(Operator.IN, (Object[])name);
    }

    public TenantRequest<T> withNameNotIn(String... name){
       return withName(Operator.NOT_IN, (Object[])name);
    }
    public TenantRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public TenantRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public TenantRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public TenantRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public TenantRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public TenantRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public TenantRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public TenantRequest<T> withNameNotContaining(String name){
       return withName(Operator.NOT_CONTAIN, name);
    }

    public TenantRequest<T> withNameNotStartingWith(String name){
       return withName(Operator.NOT_BEGIN_WITH, name);
    }

    public TenantRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public TenantRequest<T> withNameNotEndingWith(String name){
       return withName(Operator.NOT_END_WITH, name);
    }

    public TenantRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public TenantRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public TenantRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public TenantRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public TenantRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public TenantRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Tenant.VERSION_PROPERTY, operator, values);
    }

    public TenantRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public TenantRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public TenantRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public TenantRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public TenantRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public TenantRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public TenantRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public TenantRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public TenantRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public TenantRequest<T> withPurchaseOrderListMatching(PurchaseOrderRequest purchaseOrderRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(Tenant.ID_PROPERTY, purchaseOrderRequest, PurchaseOrder.TENANT_PROPERTY));
    }

    public TenantRequest<T> withoutPurchaseOrderListMatching(PurchaseOrderRequest purchaseOrderRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(Tenant.ID_PROPERTY, purchaseOrderRequest, PurchaseOrder.TENANT_PROPERTY)));
    }

    public TenantRequest<T> havePurchaseOrders(){
        return withPurchaseOrderListMatching(Q.purchaseOrders().unlimited());
    }

    public TenantRequest<T> haveNoPurchaseOrders(){
        return withoutPurchaseOrderListMatching(Q.purchaseOrders().unlimited());
    }
    public TenantRequest<T> withOrderItemListMatching(OrderItemRequest orderItemRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(Tenant.ID_PROPERTY, orderItemRequest, OrderItem.TENANT_PROPERTY));
    }

    public TenantRequest<T> withoutOrderItemListMatching(OrderItemRequest orderItemRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(Tenant.ID_PROPERTY, orderItemRequest, OrderItem.TENANT_PROPERTY)));
    }

    public TenantRequest<T> haveOrderItems(){
        return withOrderItemListMatching(Q.orderItems().unlimited());
    }

    public TenantRequest<T> haveNoOrderItems(){
        return withoutOrderItemListMatching(Q.orderItems().unlimited());
    }

    public TenantRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public TenantRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public TenantRequest<T> groupByPurchaseOrdersWithDetails(PurchaseOrderRequest subRequest){
       aggregate(Tenant.PURCHASE_ORDER_LIST_PROPERTY, subRequest);
       return this;
    }
    public TenantRequest<T> groupByOrderItemsWithDetails(OrderItemRequest subRequest){
       aggregate(Tenant.ORDER_ITEM_LIST_PROPERTY, subRequest);
       return this;
    }

    public TenantRequest<T> groupById(){
       groupBy(Tenant.ID_PROPERTY);
       return this;
    }

    public TenantRequest<T> groupByIdAs(String retName){
       groupBy(retName, Tenant.ID_PROPERTY);
       return this;
    }

    public TenantRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, Tenant.ID_PROPERTY, function);
       return this;
    }

    public TenantRequest<T> groupByName(){
       groupBy(Tenant.NAME_PROPERTY);
       return this;
    }

    public TenantRequest<T> groupByNameAs(String retName){
       groupBy(retName, Tenant.NAME_PROPERTY);
       return this;
    }

    public TenantRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, Tenant.NAME_PROPERTY, function);
       return this;
    }

    public TenantRequest<T> groupByVersion(){
       groupBy(Tenant.VERSION_PROPERTY);
       return this;
    }

    public TenantRequest<T> groupByVersionAs(String retName){
       groupBy(retName, Tenant.VERSION_PROPERTY);
       return this;
    }

    public TenantRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, Tenant.VERSION_PROPERTY, function);
       return this;
    }



    public TenantRequest<T> orderByIdAscending(){
       addOrderByAscending(Tenant.ID_PROPERTY);
       return this;
    }

    public TenantRequest<T> orderByIdDescending(){
       addOrderByDescending(Tenant.ID_PROPERTY);
       return this;
    }

    public TenantRequest<T> orderByNameAscending(){
       addOrderByAscending(Tenant.NAME_PROPERTY);
       return this;
    }

    public TenantRequest<T> orderByNameDescending(){
       addOrderByDescending(Tenant.NAME_PROPERTY);
       return this;
    }
    public TenantRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Tenant.NAME_PROPERTY);
       return this;
    }

    public TenantRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Tenant.NAME_PROPERTY);
       return this;
    }
    public TenantRequest<T> orderByVersionAscending(){
       addOrderByAscending(Tenant.VERSION_PROPERTY);
       return this;
    }

    public TenantRequest<T> orderByVersionDescending(){
       addOrderByDescending(Tenant.VERSION_PROPERTY);
       return this;
    }


    public TenantRequest<T> statsFromPurchaseOrdersAs(String name, PurchaseOrderRequest subRequest){
       return statsFromPurchaseOrdersAs(name, subRequest, false);
    }

    public TenantRequest<T> statsFromPurchaseOrdersAs(String name, PurchaseOrderRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, PurchaseOrder.TENANT_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromPurchaseOrdersAs", name, Tenant.PURCHASE_ORDER_LIST_PROPERTY));
       return this;
    }

    public TenantRequest<T> statsFromPurchaseOrders(PurchaseOrderRequest subRequest){
       return statsFromPurchaseOrdersAs(REFINEMENTS, subRequest);
    }
    public TenantRequest<T> statsFromOrderItemsAs(String name, OrderItemRequest subRequest){
       return statsFromOrderItemsAs(name, subRequest, false);
    }

    public TenantRequest<T> statsFromOrderItemsAs(String name, OrderItemRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, OrderItem.TENANT_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromOrderItemsAs", name, Tenant.ORDER_ITEM_LIST_PROPERTY));
       return this;
    }

    public TenantRequest<T> statsFromOrderItems(OrderItemRequest subRequest){
       return statsFromOrderItemsAs(REFINEMENTS, subRequest);
    }
    public TenantRequest<T> countPurchaseOrders(){
        addRelationCount("Count", Q.purchaseOrders().unlimited(), PurchaseOrder.TENANT_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countPurchaseOrders", null, Tenant.PURCHASE_ORDER_LIST_PROPERTY));
        return this;
    }

    public TenantRequest<T> countPurchaseOrdersAs(String name){
        addRelationCount(name, Q.purchaseOrders().unlimited(), PurchaseOrder.TENANT_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countPurchaseOrdersAs", name, Tenant.PURCHASE_ORDER_LIST_PROPERTY));
        return this;
    }

    public TenantRequest<T> countPurchaseOrdersWith(String name, PurchaseOrderRequest subRequest){
        addRelationCount(name, subRequest, PurchaseOrder.TENANT_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countPurchaseOrdersWith", name, Tenant.PURCHASE_ORDER_LIST_PROPERTY));
        return this;
    }
    public TenantRequest<T> countOrderItems(){
        addRelationCount("Count", Q.orderItems().unlimited(), OrderItem.TENANT_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderItems", null, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public TenantRequest<T> countOrderItemsAs(String name){
        addRelationCount(name, Q.orderItems().unlimited(), OrderItem.TENANT_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderItemsAs", name, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public TenantRequest<T> countOrderItemsWith(String name, OrderItemRequest subRequest){
        addRelationCount(name, subRequest, OrderItem.TENANT_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderItemsWith", name, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public TenantRequest<T> minUnitPriceOfOrderItems(){
        addRelationScalarAggregate("minUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.TENANT_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minUnitPriceOfOrderItems", null, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public TenantRequest<T> minUnitPriceOfOrderItemsAs(String name){
        return minUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public TenantRequest<T> minUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.TENANT_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minUnitPriceOfOrderItemsAs", name, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public TenantRequest<T> maxUnitPriceOfOrderItems(){
        addRelationScalarAggregate("maxUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.TENANT_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxUnitPriceOfOrderItems", null, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public TenantRequest<T> maxUnitPriceOfOrderItemsAs(String name){
        return maxUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public TenantRequest<T> maxUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.TENANT_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxUnitPriceOfOrderItemsAs", name, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public TenantRequest<T> sumUnitPriceOfOrderItems(){
        addRelationScalarAggregate("sumUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.TENANT_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumUnitPriceOfOrderItems", null, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public TenantRequest<T> sumUnitPriceOfOrderItemsAs(String name){
        return sumUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public TenantRequest<T> sumUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.TENANT_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumUnitPriceOfOrderItemsAs", name, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public TenantRequest<T> avgUnitPriceOfOrderItems(){
        addRelationScalarAggregate("avgUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.TENANT_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgUnitPriceOfOrderItems", null, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public TenantRequest<T> avgUnitPriceOfOrderItemsAs(String name){
        return avgUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public TenantRequest<T> avgUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.TENANT_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgUnitPriceOfOrderItemsAs", name, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public TenantRequest<T> standardDeviationUnitPriceOfOrderItems(){
        addRelationScalarAggregate("stdDevUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.TENANT_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationUnitPriceOfOrderItems", null, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public TenantRequest<T> standardDeviationUnitPriceOfOrderItemsAs(String name){
        return standardDeviationUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public TenantRequest<T> standardDeviationUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.TENANT_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationUnitPriceOfOrderItemsAs", name, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public TenantRequest<T> squareRootOfPopulationStandardDeviationUnitPriceOfOrderItems(){
        addRelationScalarAggregate("stdDevPopUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.TENANT_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationUnitPriceOfOrderItems", null, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public TenantRequest<T> squareRootOfPopulationStandardDeviationUnitPriceOfOrderItemsAs(String name){
        return squareRootOfPopulationStandardDeviationUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public TenantRequest<T> squareRootOfPopulationStandardDeviationUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.TENANT_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationUnitPriceOfOrderItemsAs", name, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public TenantRequest<T> sampleVarianceUnitPriceOfOrderItems(){
        addRelationScalarAggregate("varSampUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.TENANT_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceUnitPriceOfOrderItems", null, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public TenantRequest<T> sampleVarianceUnitPriceOfOrderItemsAs(String name){
        return sampleVarianceUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public TenantRequest<T> sampleVarianceUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.TENANT_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceUnitPriceOfOrderItemsAs", name, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public TenantRequest<T> samplePopulationVarianceUnitPriceOfOrderItems(){
        addRelationScalarAggregate("varPopUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.TENANT_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceUnitPriceOfOrderItems", null, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public TenantRequest<T> samplePopulationVarianceUnitPriceOfOrderItemsAs(String name){
        return samplePopulationVarianceUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public TenantRequest<T> samplePopulationVarianceUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.TENANT_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceUnitPriceOfOrderItemsAs", name, Tenant.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }



    /**
     * get topN records
     * @param topN  records number
     */
    public TenantRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public TenantRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public TenantRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public TenantRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public TenantRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public TenantRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}