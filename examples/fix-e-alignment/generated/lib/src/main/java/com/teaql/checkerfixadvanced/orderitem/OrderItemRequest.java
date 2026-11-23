
package com.teaql.checkerfixadvanced.orderitem;

import com.teaql.checkerfixadvanced.Q;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrderRequest;
import com.teaql.checkerfixadvanced.tenant.Tenant;
import com.teaql.checkerfixadvanced.tenant.TenantRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

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
        return selectId().selectPurchaseOrderIdOnly().selectTenantIdOnly().selectProductName().selectUnitPrice().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public OrderItemRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public OrderItemRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(OrderItem.ID_PROPERTY);
        selectProperty(OrderItem.PURCHASE_ORDER_PROPERTY);
        selectProperty(OrderItem.TENANT_PROPERTY);
        selectProperty(OrderItem.PRODUCT_NAME_PROPERTY);
        selectProperty(OrderItem.UNIT_PRICE_PROPERTY);
        selectProperty(OrderItem.CREATE_TIME_PROPERTY);
        selectProperty(OrderItem.UPDATE_TIME_PROPERTY);
        selectProperty(OrderItem.VERSION_PROPERTY);
        return this;
    }

    public OrderItemRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectPurchaseOrder().selectTenant().selectProductName().selectUnitPrice().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public OrderItemRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectPurchaseOrder().selectTenant().selectProductName().selectUnitPrice().selectCreateTime().selectUpdateTime().selectVersion();
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
    public OrderItemRequest<T> selectPurchaseOrderIdOnly(){
       selectProperty(OrderItem.PURCHASE_ORDER_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> selectPurchaseOrder(){
        return selectPurchaseOrderWith(Q.purchaseOrders().unlimited().selectSelf());
    }

    public OrderItemRequest<T> selectPurchaseOrderWith(PurchaseOrderRequest purchaseOrder){
       selectProperty(OrderItem.PURCHASE_ORDER_PROPERTY);
       enhanceRelation(OrderItem.PURCHASE_ORDER_PROPERTY, purchaseOrder,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectPurchaseOrderWith", null, OrderItem.PURCHASE_ORDER_PROPERTY));
       return this;
    }

    public OrderItemRequest<T> unselectPurchaseOrder(){
       unselectProperty(OrderItem.PURCHASE_ORDER_PROPERTY);
       return this;
    }
    public OrderItemRequest<T> selectTenantIdOnly(){
       selectProperty(OrderItem.TENANT_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> selectTenant(){
        return selectTenantWith(Q.tenants().unlimited().selectSelf());
    }

    public OrderItemRequest<T> selectTenantWith(TenantRequest tenant){
       selectProperty(OrderItem.TENANT_PROPERTY);
       enhanceRelation(OrderItem.TENANT_PROPERTY, tenant,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectTenantWith", null, OrderItem.TENANT_PROPERTY));
       return this;
    }

    public OrderItemRequest<T> unselectTenant(){
       unselectProperty(OrderItem.TENANT_PROPERTY);
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
    public OrderItemRequest<T> selectCreateTime(){
       selectProperty(OrderItem.CREATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the createTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  createTime) to fetch createTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderItemRequest<T> unselectCreateTime(){
       unselectProperty(OrderItem.CREATE_TIME_PROPERTY);
       return this;
    }
    public OrderItemRequest<T> selectUpdateTime(){
       selectProperty(OrderItem.UPDATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the updateTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  updateTime) to fetch updateTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderItemRequest<T> unselectUpdateTime(){
       unselectProperty(OrderItem.UPDATE_TIME_PROPERTY);
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



    public OrderItemRequest<T> filterByPurchaseOrder(PurchaseOrder... purchaseOrder){
      if (purchaseOrder == null || purchaseOrder.length == 0) {
        throw new IllegalArgumentException("filterByPurchaseOrder parameter purchaseOrder cannot be empty");
      }
      return appendSearchCriteria(createPurchaseOrderCriteria(Operator.EQUAL, (Object[])purchaseOrder));
    }

    public OrderItemRequest<T> withPurchaseOrder(Operator operator, Object... values){
       return appendSearchCriteria(createPurchaseOrderCriteria(operator, values));
    }

    public OrderItemRequest<T> withPurchaseOrderIsUnknown(){
       return withPurchaseOrder(Operator.IS_NULL);
    }

    public OrderItemRequest<T> withPurchaseOrderIsKnown(){
       return withPurchaseOrder(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createPurchaseOrderCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderItem.PURCHASE_ORDER_PROPERTY, operator, values);
    }

    public OrderItemRequest<T> filterByPurchaseOrder(Long purchaseOrder){
      if(purchaseOrder == null){
         return this;
      }
      return withPurchaseOrder(Operator.EQUAL, purchaseOrder);
    }
    public OrderItemRequest<T> withPurchaseOrderMatching(PurchaseOrderRequest purchaseOrder){
       return appendSearchCriteria(new SubQuerySearchCriteria(OrderItem.PURCHASE_ORDER_PROPERTY, purchaseOrder, PurchaseOrder.ID_PROPERTY));
    }

    public OrderItemRequest<T> withoutPurchaseOrderMatching(PurchaseOrderRequest purchaseOrder){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(OrderItem.PURCHASE_ORDER_PROPERTY, purchaseOrder, PurchaseOrder.ID_PROPERTY)));
    }

    public OrderItemRequest<T> filterByTenant(Tenant... tenant){
      if (tenant == null || tenant.length == 0) {
        throw new IllegalArgumentException("filterByTenant parameter tenant cannot be empty");
      }
      return appendSearchCriteria(createTenantCriteria(Operator.EQUAL, (Object[])tenant));
    }

    public OrderItemRequest<T> withTenant(Operator operator, Object... values){
       return appendSearchCriteria(createTenantCriteria(operator, values));
    }

    public OrderItemRequest<T> withTenantIsUnknown(){
       return withTenant(Operator.IS_NULL);
    }

    public OrderItemRequest<T> withTenantIsKnown(){
       return withTenant(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createTenantCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderItem.TENANT_PROPERTY, operator, values);
    }

    public OrderItemRequest<T> filterByTenant(Long tenant){
      if(tenant == null){
         return this;
      }
      return withTenant(Operator.EQUAL, tenant);
    }
    public OrderItemRequest<T> withTenantMatching(TenantRequest tenant){
       return appendSearchCriteria(new SubQuerySearchCriteria(OrderItem.TENANT_PROPERTY, tenant, Tenant.ID_PROPERTY));
    }

    public OrderItemRequest<T> withoutTenantMatching(TenantRequest tenant){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(OrderItem.TENANT_PROPERTY, tenant, Tenant.ID_PROPERTY)));
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



    public OrderItemRequest<T> filterByCreateTime(LocalDateTime... createTime){
      if (createTime == null || createTime.length == 0) {
        throw new IllegalArgumentException("filterByCreateTime parameter createTime cannot be empty");
      }
      return appendSearchCriteria(createCreateTimeCriteria(Operator.EQUAL, (Object[])createTime));
    }

    public OrderItemRequest<T> withCreateTime(Operator operator, Object... values){
       return appendSearchCriteria(createCreateTimeCriteria(operator, values));
    }

    public OrderItemRequest<T> withCreateTimeIsUnknown(){
       return withCreateTime(Operator.IS_NULL);
    }

    public OrderItemRequest<T> withCreateTimeIsKnown(){
       return withCreateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCreateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderItem.CREATE_TIME_PROPERTY, operator, values);
    }

    public OrderItemRequest<T> withCreateTimeIs(LocalDateTime createTime){
       return withCreateTime(Operator.EQUAL, createTime);
    }

    public OrderItemRequest<T> withCreateTimeIsNot(LocalDateTime createTime){
       return withCreateTime(Operator.NOT_EQUAL, createTime);
    }

    public OrderItemRequest<T> withCreateTimeIn(LocalDateTime... createTime){
       return withCreateTime(Operator.IN, (Object[])createTime);
    }

    public OrderItemRequest<T> withCreateTimeNotIn(LocalDateTime... createTime){
       return withCreateTime(Operator.NOT_IN, (Object[])createTime);
    }
    public OrderItemRequest<T> withCreateTimeGreaterThan(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public OrderItemRequest<T> withCreateTimeGreaterThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN_OR_EQUAL, createTime);
    }

    public OrderItemRequest<T> withCreateTimeLessThan(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public OrderItemRequest<T> withCreateTimeLessThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN_OR_EQUAL, createTime);
    }

    public OrderItemRequest<T> withCreateTimeBetween(LocalDateTime startOfCreateTime, LocalDateTime endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }
    public OrderItemRequest<T> withCreateTimeBefore(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public OrderItemRequest<T> withCreateTimeBefore(Date createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public OrderItemRequest<T> withCreateTimeAfter(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public OrderItemRequest<T> withCreateTimeAfter(Date createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public OrderItemRequest<T> withCreateTimeBetween(Date startOfCreateTime, Date endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }




    public OrderItemRequest<T> filterByUpdateTime(LocalDateTime... updateTime){
      if (updateTime == null || updateTime.length == 0) {
        throw new IllegalArgumentException("filterByUpdateTime parameter updateTime cannot be empty");
      }
      return appendSearchCriteria(createUpdateTimeCriteria(Operator.EQUAL, (Object[])updateTime));
    }

    public OrderItemRequest<T> withUpdateTime(Operator operator, Object... values){
       return appendSearchCriteria(createUpdateTimeCriteria(operator, values));
    }

    public OrderItemRequest<T> withUpdateTimeIsUnknown(){
       return withUpdateTime(Operator.IS_NULL);
    }

    public OrderItemRequest<T> withUpdateTimeIsKnown(){
       return withUpdateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createUpdateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderItem.UPDATE_TIME_PROPERTY, operator, values);
    }

    public OrderItemRequest<T> withUpdateTimeIs(LocalDateTime updateTime){
       return withUpdateTime(Operator.EQUAL, updateTime);
    }

    public OrderItemRequest<T> withUpdateTimeIsNot(LocalDateTime updateTime){
       return withUpdateTime(Operator.NOT_EQUAL, updateTime);
    }

    public OrderItemRequest<T> withUpdateTimeIn(LocalDateTime... updateTime){
       return withUpdateTime(Operator.IN, (Object[])updateTime);
    }

    public OrderItemRequest<T> withUpdateTimeNotIn(LocalDateTime... updateTime){
       return withUpdateTime(Operator.NOT_IN, (Object[])updateTime);
    }
    public OrderItemRequest<T> withUpdateTimeGreaterThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public OrderItemRequest<T> withUpdateTimeGreaterThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN_OR_EQUAL, updateTime);
    }

    public OrderItemRequest<T> withUpdateTimeLessThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public OrderItemRequest<T> withUpdateTimeLessThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN_OR_EQUAL, updateTime);
    }

    public OrderItemRequest<T> withUpdateTimeBetween(LocalDateTime startOfUpdateTime, LocalDateTime endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
    }
    public OrderItemRequest<T> withUpdateTimeBefore(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public OrderItemRequest<T> withUpdateTimeBefore(Date updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public OrderItemRequest<T> withUpdateTimeAfter(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public OrderItemRequest<T> withUpdateTimeAfter(Date updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public OrderItemRequest<T> withUpdateTimeBetween(Date startOfUpdateTime, Date endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
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
    public OrderItemRequest<T> groupByPurchaseOrderWithDetails(){
       return groupByPurchaseOrderWithDetails(Q.purchaseOrders().unlimited());
    }

    public OrderItemRequest<T> groupByPurchaseOrderWithDetails(PurchaseOrderRequest subRequest){
       aggregate(OrderItem.PURCHASE_ORDER_PROPERTY, subRequest);
       return this;
    }

    public OrderItemRequest<T> groupByTenantWithDetails(){
       return groupByTenantWithDetails(Q.tenants().unlimited());
    }

    public OrderItemRequest<T> groupByTenantWithDetails(TenantRequest subRequest){
       aggregate(OrderItem.TENANT_PROPERTY, subRequest);
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
    public OrderItemRequest<T> groupByPurchaseOrderWith(PurchaseOrderRequest subRequest){
       groupBy(OrderItem.PURCHASE_ORDER_PROPERTY, subRequest);
       return this;
    }
    public OrderItemRequest<T> groupByPurchaseOrder(){
       groupBy(OrderItem.PURCHASE_ORDER_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByPurchaseOrderAs(String retName){
       groupBy(retName, OrderItem.PURCHASE_ORDER_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByPurchaseOrderWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderItem.PURCHASE_ORDER_PROPERTY, function);
       return this;
    }
    public OrderItemRequest<T> groupByTenantWith(TenantRequest subRequest){
       groupBy(OrderItem.TENANT_PROPERTY, subRequest);
       return this;
    }
    public OrderItemRequest<T> groupByTenant(){
       groupBy(OrderItem.TENANT_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByTenantAs(String retName){
       groupBy(retName, OrderItem.TENANT_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByTenantWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderItem.TENANT_PROPERTY, function);
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

    public OrderItemRequest<T> groupByCreateTime(){
       groupBy(OrderItem.CREATE_TIME_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByCreateTimeAs(String retName){
       groupBy(retName, OrderItem.CREATE_TIME_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByCreateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderItem.CREATE_TIME_PROPERTY, function);
       return this;
    }

    public OrderItemRequest<T> groupByUpdateTime(){
       groupBy(OrderItem.UPDATE_TIME_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByUpdateTimeAs(String retName){
       groupBy(retName, OrderItem.UPDATE_TIME_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> groupByUpdateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderItem.UPDATE_TIME_PROPERTY, function);
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

    public OrderItemRequest<T> orderByPurchaseOrderAscending(){
       addOrderByAscending(OrderItem.PURCHASE_ORDER_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByPurchaseOrderDescending(){
       addOrderByDescending(OrderItem.PURCHASE_ORDER_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByTenantAscending(){
       addOrderByAscending(OrderItem.TENANT_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByTenantDescending(){
       addOrderByDescending(OrderItem.TENANT_PROPERTY);
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
    public OrderItemRequest<T> orderByUnitPriceAscending(){
       addOrderByAscending(OrderItem.UNIT_PRICE_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByUnitPriceDescending(){
       addOrderByDescending(OrderItem.UNIT_PRICE_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByCreateTimeAscending(){
       addOrderByAscending(OrderItem.CREATE_TIME_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByCreateTimeDescending(){
       addOrderByDescending(OrderItem.CREATE_TIME_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByUpdateTimeAscending(){
       addOrderByAscending(OrderItem.UPDATE_TIME_PROPERTY);
       return this;
    }

    public OrderItemRequest<T> orderByUpdateTimeDescending(){
       addOrderByDescending(OrderItem.UPDATE_TIME_PROPERTY);
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


    public PurchaseOrderRequest rollUpToPurchaseOrder(){
       PurchaseOrderRequest purchaseOrder = Q.purchaseOrders().unlimited();
       this.withPurchaseOrderMatching(purchaseOrder)
           .groupByPurchaseOrderWith(purchaseOrder);
       return purchaseOrder;
    }

    public TenantRequest rollUpToTenant(){
       TenantRequest tenant = Q.tenants().unlimited();
       this.withTenantMatching(tenant)
           .groupByTenantWith(tenant);
       return tenant;
    }







   public OrderItemRequest<T> facetByPurchaseOrderAs(String facetName, PurchaseOrderRequest purchaseOrder){
       return facetByPurchaseOrderAs(facetName, purchaseOrder, true);
   }

   public OrderItemRequest<T> facetByPurchaseOrderAs(String facetName, PurchaseOrderRequest purchaseOrder, boolean includeAllFacets){
       addFacet(facetName, OrderItem.PURCHASE_ORDER_PROPERTY, purchaseOrder, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByPurchaseOrderAs", facetName, OrderItem.PURCHASE_ORDER_PROPERTY));
       return this;
   }
   public OrderItemRequest<T> facetByTenantAs(String facetName, TenantRequest tenant){
       return facetByTenantAs(facetName, tenant, true);
   }

   public OrderItemRequest<T> facetByTenantAs(String facetName, TenantRequest tenant, boolean includeAllFacets){
       addFacet(facetName, OrderItem.TENANT_PROPERTY, tenant, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByTenantAs", facetName, OrderItem.TENANT_PROPERTY));
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