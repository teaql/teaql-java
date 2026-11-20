
package com.teaql.ordermanagementservice.commerceplatform;

import com.teaql.ordermanagementservice.Q;
import com.teaql.ordermanagementservice.customer.Customer;
import com.teaql.ordermanagementservice.customer.CustomerRequest;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.customerorder.CustomerOrderRequest;
import com.teaql.ordermanagementservice.orderline.OrderLine;
import com.teaql.ordermanagementservice.orderline.OrderLineRequest;
import com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset;
import com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPresetRequest;
import com.teaql.ordermanagementservice.orderstatus.OrderStatus;
import com.teaql.ordermanagementservice.orderstatus.OrderStatusRequest;
import com.teaql.ordermanagementservice.product.Product;
import com.teaql.ordermanagementservice.product.ProductRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.time.LocalDateTime;
import java.util.Date;
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
        return selectId().selectName().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public CommercePlatformRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public CommercePlatformRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(CommercePlatform.ID_PROPERTY);
        selectProperty(CommercePlatform.NAME_PROPERTY);
        selectProperty(CommercePlatform.CREATE_TIME_PROPERTY);
        selectProperty(CommercePlatform.UPDATE_TIME_PROPERTY);
        selectProperty(CommercePlatform.VERSION_PROPERTY);
        return this;
    }

    public CommercePlatformRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectName().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public CommercePlatformRequest<T> selectChildren(){
        super.selectAny();
        selectCustomerList().selectOrderStatusList().selectCustomerOrderList().selectProductList().selectOrderLineList().selectOrderSearchPresetList();
        return selectId().selectName().selectCreateTime().selectUpdateTime().selectVersion();
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
    public CommercePlatformRequest<T> selectCreateTime(){
       selectProperty(CommercePlatform.CREATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the createTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  createTime) to fetch createTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CommercePlatformRequest<T> unselectCreateTime(){
       unselectProperty(CommercePlatform.CREATE_TIME_PROPERTY);
       return this;
    }
    public CommercePlatformRequest<T> selectUpdateTime(){
       selectProperty(CommercePlatform.UPDATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the updateTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  updateTime) to fetch updateTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CommercePlatformRequest<T> unselectUpdateTime(){
       unselectProperty(CommercePlatform.UPDATE_TIME_PROPERTY);
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
    public CommercePlatformRequest<T> selectCustomerList(){
       return selectCustomerListWith(Q.customers().selectSelf());
    }

    public CommercePlatformRequest<T> selectCustomerListWith(CustomerRequest customerList){
       enhanceRelation(CommercePlatform.CUSTOMER_LIST_PROPERTY, customerList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectCustomerListWith", null, CommercePlatform.CUSTOMER_LIST_PROPERTY));
       return this;
    }
    public CommercePlatformRequest<T> selectOrderStatusList(){
       return selectOrderStatusListWith(Q.orderStatuses().selectSelf());
    }

    public CommercePlatformRequest<T> selectOrderStatusListWith(OrderStatusRequest orderStatusList){
       enhanceRelation(CommercePlatform.ORDER_STATUS_LIST_PROPERTY, orderStatusList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectOrderStatusListWith", null, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
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
    public CommercePlatformRequest<T> selectProductList(){
       return selectProductListWith(Q.products().selectSelf());
    }

    public CommercePlatformRequest<T> selectProductListWith(ProductRequest productList){
       enhanceRelation(CommercePlatform.PRODUCT_LIST_PROPERTY, productList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectProductListWith", null, CommercePlatform.PRODUCT_LIST_PROPERTY));
       return this;
    }
    public CommercePlatformRequest<T> selectOrderLineList(){
       return selectOrderLineListWith(Q.orderLines().selectSelf());
    }

    public CommercePlatformRequest<T> selectOrderLineListWith(OrderLineRequest orderLineList){
       enhanceRelation(CommercePlatform.ORDER_LINE_LIST_PROPERTY, orderLineList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectOrderLineListWith", null, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
       return this;
    }
    public CommercePlatformRequest<T> selectOrderSearchPresetList(){
       return selectOrderSearchPresetListWith(Q.orderSearchPresets().selectSelf());
    }

    public CommercePlatformRequest<T> selectOrderSearchPresetListWith(OrderSearchPresetRequest orderSearchPresetList){
       enhanceRelation(CommercePlatform.ORDER_SEARCH_PRESET_LIST_PROPERTY, orderSearchPresetList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectOrderSearchPresetListWith", null, CommercePlatform.ORDER_SEARCH_PRESET_LIST_PROPERTY));
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



    public CommercePlatformRequest<T> filterByCreateTime(LocalDateTime... createTime){
      if (createTime == null || createTime.length == 0) {
        throw new IllegalArgumentException("filterByCreateTime parameter createTime cannot be empty");
      }
      return appendSearchCriteria(createCreateTimeCriteria(Operator.EQUAL, (Object[])createTime));
    }

    public CommercePlatformRequest<T> withCreateTime(Operator operator, Object... values){
       return appendSearchCriteria(createCreateTimeCriteria(operator, values));
    }

    public CommercePlatformRequest<T> withCreateTimeIsUnknown(){
       return withCreateTime(Operator.IS_NULL);
    }

    public CommercePlatformRequest<T> withCreateTimeIsKnown(){
       return withCreateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCreateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CommercePlatform.CREATE_TIME_PROPERTY, operator, values);
    }

    public CommercePlatformRequest<T> withCreateTimeIs(LocalDateTime createTime){
       return withCreateTime(Operator.EQUAL, createTime);
    }

    public CommercePlatformRequest<T> withCreateTimeIsNot(LocalDateTime createTime){
       return withCreateTime(Operator.NOT_EQUAL, createTime);
    }

    public CommercePlatformRequest<T> withCreateTimeIn(LocalDateTime... createTime){
       return withCreateTime(Operator.IN, (Object[])createTime);
    }

    public CommercePlatformRequest<T> withCreateTimeNotIn(LocalDateTime... createTime){
       return withCreateTime(Operator.NOT_IN, (Object[])createTime);
    }
    public CommercePlatformRequest<T> withCreateTimeGreaterThan(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public CommercePlatformRequest<T> withCreateTimeGreaterThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN_OR_EQUAL, createTime);
    }

    public CommercePlatformRequest<T> withCreateTimeLessThan(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public CommercePlatformRequest<T> withCreateTimeLessThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN_OR_EQUAL, createTime);
    }

    public CommercePlatformRequest<T> withCreateTimeBetween(LocalDateTime startOfCreateTime, LocalDateTime endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }
    public CommercePlatformRequest<T> withCreateTimeBefore(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public CommercePlatformRequest<T> withCreateTimeBefore(Date createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public CommercePlatformRequest<T> withCreateTimeAfter(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public CommercePlatformRequest<T> withCreateTimeAfter(Date createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public CommercePlatformRequest<T> withCreateTimeBetween(Date startOfCreateTime, Date endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }




    public CommercePlatformRequest<T> filterByUpdateTime(LocalDateTime... updateTime){
      if (updateTime == null || updateTime.length == 0) {
        throw new IllegalArgumentException("filterByUpdateTime parameter updateTime cannot be empty");
      }
      return appendSearchCriteria(createUpdateTimeCriteria(Operator.EQUAL, (Object[])updateTime));
    }

    public CommercePlatformRequest<T> withUpdateTime(Operator operator, Object... values){
       return appendSearchCriteria(createUpdateTimeCriteria(operator, values));
    }

    public CommercePlatformRequest<T> withUpdateTimeIsUnknown(){
       return withUpdateTime(Operator.IS_NULL);
    }

    public CommercePlatformRequest<T> withUpdateTimeIsKnown(){
       return withUpdateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createUpdateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CommercePlatform.UPDATE_TIME_PROPERTY, operator, values);
    }

    public CommercePlatformRequest<T> withUpdateTimeIs(LocalDateTime updateTime){
       return withUpdateTime(Operator.EQUAL, updateTime);
    }

    public CommercePlatformRequest<T> withUpdateTimeIsNot(LocalDateTime updateTime){
       return withUpdateTime(Operator.NOT_EQUAL, updateTime);
    }

    public CommercePlatformRequest<T> withUpdateTimeIn(LocalDateTime... updateTime){
       return withUpdateTime(Operator.IN, (Object[])updateTime);
    }

    public CommercePlatformRequest<T> withUpdateTimeNotIn(LocalDateTime... updateTime){
       return withUpdateTime(Operator.NOT_IN, (Object[])updateTime);
    }
    public CommercePlatformRequest<T> withUpdateTimeGreaterThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public CommercePlatformRequest<T> withUpdateTimeGreaterThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN_OR_EQUAL, updateTime);
    }

    public CommercePlatformRequest<T> withUpdateTimeLessThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public CommercePlatformRequest<T> withUpdateTimeLessThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN_OR_EQUAL, updateTime);
    }

    public CommercePlatformRequest<T> withUpdateTimeBetween(LocalDateTime startOfUpdateTime, LocalDateTime endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
    }
    public CommercePlatformRequest<T> withUpdateTimeBefore(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public CommercePlatformRequest<T> withUpdateTimeBefore(Date updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public CommercePlatformRequest<T> withUpdateTimeAfter(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public CommercePlatformRequest<T> withUpdateTimeAfter(Date updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public CommercePlatformRequest<T> withUpdateTimeBetween(Date startOfUpdateTime, Date endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
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


    public CommercePlatformRequest<T> withCustomerListMatching(CustomerRequest customerRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(CommercePlatform.ID_PROPERTY, customerRequest, Customer.COMMERCE_PLATFORM_PROPERTY));
    }

    public CommercePlatformRequest<T> withoutCustomerListMatching(CustomerRequest customerRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(CommercePlatform.ID_PROPERTY, customerRequest, Customer.COMMERCE_PLATFORM_PROPERTY)));
    }

    public CommercePlatformRequest<T> haveCustomers(){
        return withCustomerListMatching(Q.customers().unlimited());
    }

    public CommercePlatformRequest<T> haveNoCustomers(){
        return withoutCustomerListMatching(Q.customers().unlimited());
    }
    public CommercePlatformRequest<T> withOrderStatusListMatching(OrderStatusRequest orderStatusRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(CommercePlatform.ID_PROPERTY, orderStatusRequest, OrderStatus.COMMERCE_PLATFORM_PROPERTY));
    }

    public CommercePlatformRequest<T> withoutOrderStatusListMatching(OrderStatusRequest orderStatusRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(CommercePlatform.ID_PROPERTY, orderStatusRequest, OrderStatus.COMMERCE_PLATFORM_PROPERTY)));
    }

    public CommercePlatformRequest<T> haveOrderStatuses(){
        return withOrderStatusListMatching(Q.orderStatuses().unlimited());
    }

    public CommercePlatformRequest<T> haveNoOrderStatuses(){
        return withoutOrderStatusListMatching(Q.orderStatuses().unlimited());
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
    public CommercePlatformRequest<T> withProductListMatching(ProductRequest productRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(CommercePlatform.ID_PROPERTY, productRequest, Product.COMMERCE_PLATFORM_PROPERTY));
    }

    public CommercePlatformRequest<T> withoutProductListMatching(ProductRequest productRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(CommercePlatform.ID_PROPERTY, productRequest, Product.COMMERCE_PLATFORM_PROPERTY)));
    }

    public CommercePlatformRequest<T> haveProducts(){
        return withProductListMatching(Q.products().unlimited());
    }

    public CommercePlatformRequest<T> haveNoProducts(){
        return withoutProductListMatching(Q.products().unlimited());
    }
    public CommercePlatformRequest<T> withOrderLineListMatching(OrderLineRequest orderLineRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(CommercePlatform.ID_PROPERTY, orderLineRequest, OrderLine.COMMERCE_PLATFORM_PROPERTY));
    }

    public CommercePlatformRequest<T> withoutOrderLineListMatching(OrderLineRequest orderLineRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(CommercePlatform.ID_PROPERTY, orderLineRequest, OrderLine.COMMERCE_PLATFORM_PROPERTY)));
    }

    public CommercePlatformRequest<T> haveOrderLines(){
        return withOrderLineListMatching(Q.orderLines().unlimited());
    }

    public CommercePlatformRequest<T> haveNoOrderLines(){
        return withoutOrderLineListMatching(Q.orderLines().unlimited());
    }
    public CommercePlatformRequest<T> withOrderSearchPresetListMatching(OrderSearchPresetRequest orderSearchPresetRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(CommercePlatform.ID_PROPERTY, orderSearchPresetRequest, OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY));
    }

    public CommercePlatformRequest<T> withoutOrderSearchPresetListMatching(OrderSearchPresetRequest orderSearchPresetRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(CommercePlatform.ID_PROPERTY, orderSearchPresetRequest, OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY)));
    }

    public CommercePlatformRequest<T> haveOrderSearchPresets(){
        return withOrderSearchPresetListMatching(Q.orderSearchPresets().unlimited());
    }

    public CommercePlatformRequest<T> haveNoOrderSearchPresets(){
        return withoutOrderSearchPresetListMatching(Q.orderSearchPresets().unlimited());
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
    public CommercePlatformRequest<T> countDistinctCreateTime(){
        return countDistinctCreateTimeAs("create_time_distinct_count");
    }

    public CommercePlatformRequest<T> countDistinctCreateTimeAs(String alias){
        super.countDistinctProperty(alias, CommercePlatform.CREATE_TIME_PROPERTY);
        return this;
    }
    public CommercePlatformRequest<T> countDistinctUpdateTime(){
        return countDistinctUpdateTimeAs("update_time_distinct_count");
    }

    public CommercePlatformRequest<T> countDistinctUpdateTimeAs(String alias){
        super.countDistinctProperty(alias, CommercePlatform.UPDATE_TIME_PROPERTY);
        return this;
    }
    public CommercePlatformRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public CommercePlatformRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, CommercePlatform.VERSION_PROPERTY);
        return this;
    }
    public CommercePlatformRequest<T> groupByCustomersWithDetails(CustomerRequest subRequest){
       aggregate(CommercePlatform.CUSTOMER_LIST_PROPERTY, subRequest);
       return this;
    }
    public CommercePlatformRequest<T> groupByOrderStatusesWithDetails(OrderStatusRequest subRequest){
       aggregate(CommercePlatform.ORDER_STATUS_LIST_PROPERTY, subRequest);
       return this;
    }
    public CommercePlatformRequest<T> groupByCustomerOrdersWithDetails(CustomerOrderRequest subRequest){
       aggregate(CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY, subRequest);
       return this;
    }
    public CommercePlatformRequest<T> groupByProductsWithDetails(ProductRequest subRequest){
       aggregate(CommercePlatform.PRODUCT_LIST_PROPERTY, subRequest);
       return this;
    }
    public CommercePlatformRequest<T> groupByOrderLinesWithDetails(OrderLineRequest subRequest){
       aggregate(CommercePlatform.ORDER_LINE_LIST_PROPERTY, subRequest);
       return this;
    }
    public CommercePlatformRequest<T> groupByOrderSearchPresetsWithDetails(OrderSearchPresetRequest subRequest){
       aggregate(CommercePlatform.ORDER_SEARCH_PRESET_LIST_PROPERTY, subRequest);
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

    public CommercePlatformRequest<T> groupByCreateTime(){
       groupBy(CommercePlatform.CREATE_TIME_PROPERTY);
       return this;
    }

    public CommercePlatformRequest<T> groupByCreateTimeAs(String retName){
       groupBy(retName, CommercePlatform.CREATE_TIME_PROPERTY);
       return this;
    }

    public CommercePlatformRequest<T> groupByCreateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, CommercePlatform.CREATE_TIME_PROPERTY, function);
       return this;
    }

    public CommercePlatformRequest<T> groupByUpdateTime(){
       groupBy(CommercePlatform.UPDATE_TIME_PROPERTY);
       return this;
    }

    public CommercePlatformRequest<T> groupByUpdateTimeAs(String retName){
       groupBy(retName, CommercePlatform.UPDATE_TIME_PROPERTY);
       return this;
    }

    public CommercePlatformRequest<T> groupByUpdateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, CommercePlatform.UPDATE_TIME_PROPERTY, function);
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
    public CommercePlatformRequest<T> orderByCreateTimeAscending(){
       addOrderByAscending(CommercePlatform.CREATE_TIME_PROPERTY);
       return this;
    }

    public CommercePlatformRequest<T> orderByCreateTimeDescending(){
       addOrderByDescending(CommercePlatform.CREATE_TIME_PROPERTY);
       return this;
    }

    public CommercePlatformRequest<T> orderByUpdateTimeAscending(){
       addOrderByAscending(CommercePlatform.UPDATE_TIME_PROPERTY);
       return this;
    }

    public CommercePlatformRequest<T> orderByUpdateTimeDescending(){
       addOrderByDescending(CommercePlatform.UPDATE_TIME_PROPERTY);
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


    public CommercePlatformRequest<T> statsFromCustomersAs(String name, CustomerRequest subRequest){
       return statsFromCustomersAs(name, subRequest, false);
    }

    public CommercePlatformRequest<T> statsFromCustomersAs(String name, CustomerRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, Customer.COMMERCE_PLATFORM_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromCustomersAs", name, CommercePlatform.CUSTOMER_LIST_PROPERTY));
       return this;
    }

    public CommercePlatformRequest<T> statsFromCustomers(CustomerRequest subRequest){
       return statsFromCustomersAs(REFINEMENTS, subRequest);
    }
    public CommercePlatformRequest<T> statsFromOrderStatusesAs(String name, OrderStatusRequest subRequest){
       return statsFromOrderStatusesAs(name, subRequest, false);
    }

    public CommercePlatformRequest<T> statsFromOrderStatusesAs(String name, OrderStatusRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, OrderStatus.COMMERCE_PLATFORM_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromOrderStatusesAs", name, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
       return this;
    }

    public CommercePlatformRequest<T> statsFromOrderStatuses(OrderStatusRequest subRequest){
       return statsFromOrderStatusesAs(REFINEMENTS, subRequest);
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
    public CommercePlatformRequest<T> statsFromProductsAs(String name, ProductRequest subRequest){
       return statsFromProductsAs(name, subRequest, false);
    }

    public CommercePlatformRequest<T> statsFromProductsAs(String name, ProductRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, Product.COMMERCE_PLATFORM_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromProductsAs", name, CommercePlatform.PRODUCT_LIST_PROPERTY));
       return this;
    }

    public CommercePlatformRequest<T> statsFromProducts(ProductRequest subRequest){
       return statsFromProductsAs(REFINEMENTS, subRequest);
    }
    public CommercePlatformRequest<T> statsFromOrderLinesAs(String name, OrderLineRequest subRequest){
       return statsFromOrderLinesAs(name, subRequest, false);
    }

    public CommercePlatformRequest<T> statsFromOrderLinesAs(String name, OrderLineRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, OrderLine.COMMERCE_PLATFORM_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromOrderLinesAs", name, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
       return this;
    }

    public CommercePlatformRequest<T> statsFromOrderLines(OrderLineRequest subRequest){
       return statsFromOrderLinesAs(REFINEMENTS, subRequest);
    }
    public CommercePlatformRequest<T> statsFromOrderSearchPresetsAs(String name, OrderSearchPresetRequest subRequest){
       return statsFromOrderSearchPresetsAs(name, subRequest, false);
    }

    public CommercePlatformRequest<T> statsFromOrderSearchPresetsAs(String name, OrderSearchPresetRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromOrderSearchPresetsAs", name, CommercePlatform.ORDER_SEARCH_PRESET_LIST_PROPERTY));
       return this;
    }

    public CommercePlatformRequest<T> statsFromOrderSearchPresets(OrderSearchPresetRequest subRequest){
       return statsFromOrderSearchPresetsAs(REFINEMENTS, subRequest);
    }
    public CommercePlatformRequest<T> countCustomers(){
        addRelationCount("Count", Q.customers().unlimited(), Customer.COMMERCE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomers", null, CommercePlatform.CUSTOMER_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> countCustomersAs(String name){
        addRelationCount(name, Q.customers().unlimited(), Customer.COMMERCE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomersAs", name, CommercePlatform.CUSTOMER_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> countCustomersWith(String name, CustomerRequest subRequest){
        addRelationCount(name, subRequest, Customer.COMMERCE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomersWith", name, CommercePlatform.CUSTOMER_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> countOrderStatuses(){
        addRelationCount("Count", Q.orderStatuses().unlimited(), OrderStatus.COMMERCE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderStatuses", null, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> countOrderStatusesAs(String name){
        addRelationCount(name, Q.orderStatuses().unlimited(), OrderStatus.COMMERCE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderStatusesAs", name, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> countOrderStatusesWith(String name, OrderStatusRequest subRequest){
        addRelationCount(name, subRequest, OrderStatus.COMMERCE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderStatusesWith", name, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
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
    public CommercePlatformRequest<T> countProducts(){
        addRelationCount("Count", Q.products().unlimited(), Product.COMMERCE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countProducts", null, CommercePlatform.PRODUCT_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> countProductsAs(String name){
        addRelationCount(name, Q.products().unlimited(), Product.COMMERCE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countProductsAs", name, CommercePlatform.PRODUCT_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> countProductsWith(String name, ProductRequest subRequest){
        addRelationCount(name, subRequest, Product.COMMERCE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countProductsWith", name, CommercePlatform.PRODUCT_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> countOrderLines(){
        addRelationCount("Count", Q.orderLines().unlimited(), OrderLine.COMMERCE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderLines", null, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> countOrderLinesAs(String name){
        addRelationCount(name, Q.orderLines().unlimited(), OrderLine.COMMERCE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderLinesAs", name, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> countOrderLinesWith(String name, OrderLineRequest subRequest){
        addRelationCount(name, subRequest, OrderLine.COMMERCE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderLinesWith", name, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> countOrderSearchPresets(){
        addRelationCount("Count", Q.orderSearchPresets().unlimited(), OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderSearchPresets", null, CommercePlatform.ORDER_SEARCH_PRESET_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> countOrderSearchPresetsAs(String name){
        addRelationCount(name, Q.orderSearchPresets().unlimited(), OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderSearchPresetsAs", name, CommercePlatform.ORDER_SEARCH_PRESET_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> countOrderSearchPresetsWith(String name, OrderSearchPresetRequest subRequest){
        addRelationCount(name, subRequest, OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderSearchPresetsWith", name, CommercePlatform.ORDER_SEARCH_PRESET_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> minDisplayOrderOfOrderStatuses(){
        addRelationScalarAggregate("minDisplayOrderOfOrderStatuses", Q.orderStatuses().unlimited(),
            OrderStatus.COMMERCE_PLATFORM_PROPERTY, OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minDisplayOrderOfOrderStatuses", null, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> minDisplayOrderOfOrderStatusesAs(String name){
        return minDisplayOrderOfOrderStatusesAs(name, Q.orderStatuses().unlimited());
    }

    public CommercePlatformRequest<T> minDisplayOrderOfOrderStatusesAs(String name, OrderStatusRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderStatus.COMMERCE_PLATFORM_PROPERTY,
            OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minDisplayOrderOfOrderStatusesAs", name, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> maxDisplayOrderOfOrderStatuses(){
        addRelationScalarAggregate("maxDisplayOrderOfOrderStatuses", Q.orderStatuses().unlimited(),
            OrderStatus.COMMERCE_PLATFORM_PROPERTY, OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxDisplayOrderOfOrderStatuses", null, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> maxDisplayOrderOfOrderStatusesAs(String name){
        return maxDisplayOrderOfOrderStatusesAs(name, Q.orderStatuses().unlimited());
    }

    public CommercePlatformRequest<T> maxDisplayOrderOfOrderStatusesAs(String name, OrderStatusRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderStatus.COMMERCE_PLATFORM_PROPERTY,
            OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxDisplayOrderOfOrderStatusesAs", name, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> sumDisplayOrderOfOrderStatuses(){
        addRelationScalarAggregate("sumDisplayOrderOfOrderStatuses", Q.orderStatuses().unlimited(),
            OrderStatus.COMMERCE_PLATFORM_PROPERTY, OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumDisplayOrderOfOrderStatuses", null, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> sumDisplayOrderOfOrderStatusesAs(String name){
        return sumDisplayOrderOfOrderStatusesAs(name, Q.orderStatuses().unlimited());
    }

    public CommercePlatformRequest<T> sumDisplayOrderOfOrderStatusesAs(String name, OrderStatusRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderStatus.COMMERCE_PLATFORM_PROPERTY,
            OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumDisplayOrderOfOrderStatusesAs", name, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> avgDisplayOrderOfOrderStatuses(){
        addRelationScalarAggregate("avgDisplayOrderOfOrderStatuses", Q.orderStatuses().unlimited(),
            OrderStatus.COMMERCE_PLATFORM_PROPERTY, OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgDisplayOrderOfOrderStatuses", null, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> avgDisplayOrderOfOrderStatusesAs(String name){
        return avgDisplayOrderOfOrderStatusesAs(name, Q.orderStatuses().unlimited());
    }

    public CommercePlatformRequest<T> avgDisplayOrderOfOrderStatusesAs(String name, OrderStatusRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderStatus.COMMERCE_PLATFORM_PROPERTY,
            OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgDisplayOrderOfOrderStatusesAs", name, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> standardDeviationDisplayOrderOfOrderStatuses(){
        addRelationScalarAggregate("stdDevDisplayOrderOfOrderStatuses", Q.orderStatuses().unlimited(),
            OrderStatus.COMMERCE_PLATFORM_PROPERTY, OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationDisplayOrderOfOrderStatuses", null, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> standardDeviationDisplayOrderOfOrderStatusesAs(String name){
        return standardDeviationDisplayOrderOfOrderStatusesAs(name, Q.orderStatuses().unlimited());
    }

    public CommercePlatformRequest<T> standardDeviationDisplayOrderOfOrderStatusesAs(String name, OrderStatusRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderStatus.COMMERCE_PLATFORM_PROPERTY,
            OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationDisplayOrderOfOrderStatusesAs", name, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> squareRootOfPopulationStandardDeviationDisplayOrderOfOrderStatuses(){
        addRelationScalarAggregate("stdDevPopDisplayOrderOfOrderStatuses", Q.orderStatuses().unlimited(),
            OrderStatus.COMMERCE_PLATFORM_PROPERTY, OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationDisplayOrderOfOrderStatuses", null, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> squareRootOfPopulationStandardDeviationDisplayOrderOfOrderStatusesAs(String name){
        return squareRootOfPopulationStandardDeviationDisplayOrderOfOrderStatusesAs(name, Q.orderStatuses().unlimited());
    }

    public CommercePlatformRequest<T> squareRootOfPopulationStandardDeviationDisplayOrderOfOrderStatusesAs(String name, OrderStatusRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderStatus.COMMERCE_PLATFORM_PROPERTY,
            OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationDisplayOrderOfOrderStatusesAs", name, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> sampleVarianceDisplayOrderOfOrderStatuses(){
        addRelationScalarAggregate("varSampDisplayOrderOfOrderStatuses", Q.orderStatuses().unlimited(),
            OrderStatus.COMMERCE_PLATFORM_PROPERTY, OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceDisplayOrderOfOrderStatuses", null, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> sampleVarianceDisplayOrderOfOrderStatusesAs(String name){
        return sampleVarianceDisplayOrderOfOrderStatusesAs(name, Q.orderStatuses().unlimited());
    }

    public CommercePlatformRequest<T> sampleVarianceDisplayOrderOfOrderStatusesAs(String name, OrderStatusRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderStatus.COMMERCE_PLATFORM_PROPERTY,
            OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceDisplayOrderOfOrderStatusesAs", name, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> samplePopulationVarianceDisplayOrderOfOrderStatuses(){
        addRelationScalarAggregate("varPopDisplayOrderOfOrderStatuses", Q.orderStatuses().unlimited(),
            OrderStatus.COMMERCE_PLATFORM_PROPERTY, OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceDisplayOrderOfOrderStatuses", null, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> samplePopulationVarianceDisplayOrderOfOrderStatusesAs(String name){
        return samplePopulationVarianceDisplayOrderOfOrderStatusesAs(name, Q.orderStatuses().unlimited());
    }

    public CommercePlatformRequest<T> samplePopulationVarianceDisplayOrderOfOrderStatusesAs(String name, OrderStatusRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderStatus.COMMERCE_PLATFORM_PROPERTY,
            OrderStatus.DISPLAY_ORDER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceDisplayOrderOfOrderStatusesAs", name, CommercePlatform.ORDER_STATUS_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> minTotalAmountOfCustomerOrders(){
        addRelationScalarAggregate("minTotalAmountOfCustomerOrders", Q.customerOrders().unlimited(),
            CustomerOrder.COMMERCE_PLATFORM_PROPERTY, CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minTotalAmountOfCustomerOrders", null, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> minTotalAmountOfCustomerOrdersAs(String name){
        return minTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CommercePlatformRequest<T> minTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        addRelationScalarAggregate(name, subRequest, CustomerOrder.COMMERCE_PLATFORM_PROPERTY,
            CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minTotalAmountOfCustomerOrdersAs", name, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> maxTotalAmountOfCustomerOrders(){
        addRelationScalarAggregate("maxTotalAmountOfCustomerOrders", Q.customerOrders().unlimited(),
            CustomerOrder.COMMERCE_PLATFORM_PROPERTY, CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxTotalAmountOfCustomerOrders", null, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> maxTotalAmountOfCustomerOrdersAs(String name){
        return maxTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CommercePlatformRequest<T> maxTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        addRelationScalarAggregate(name, subRequest, CustomerOrder.COMMERCE_PLATFORM_PROPERTY,
            CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxTotalAmountOfCustomerOrdersAs", name, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> sumTotalAmountOfCustomerOrders(){
        addRelationScalarAggregate("sumTotalAmountOfCustomerOrders", Q.customerOrders().unlimited(),
            CustomerOrder.COMMERCE_PLATFORM_PROPERTY, CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumTotalAmountOfCustomerOrders", null, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> sumTotalAmountOfCustomerOrdersAs(String name){
        return sumTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CommercePlatformRequest<T> sumTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        addRelationScalarAggregate(name, subRequest, CustomerOrder.COMMERCE_PLATFORM_PROPERTY,
            CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumTotalAmountOfCustomerOrdersAs", name, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> avgTotalAmountOfCustomerOrders(){
        addRelationScalarAggregate("avgTotalAmountOfCustomerOrders", Q.customerOrders().unlimited(),
            CustomerOrder.COMMERCE_PLATFORM_PROPERTY, CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgTotalAmountOfCustomerOrders", null, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> avgTotalAmountOfCustomerOrdersAs(String name){
        return avgTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CommercePlatformRequest<T> avgTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        addRelationScalarAggregate(name, subRequest, CustomerOrder.COMMERCE_PLATFORM_PROPERTY,
            CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgTotalAmountOfCustomerOrdersAs", name, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> standardDeviationTotalAmountOfCustomerOrders(){
        addRelationScalarAggregate("stdDevTotalAmountOfCustomerOrders", Q.customerOrders().unlimited(),
            CustomerOrder.COMMERCE_PLATFORM_PROPERTY, CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationTotalAmountOfCustomerOrders", null, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> standardDeviationTotalAmountOfCustomerOrdersAs(String name){
        return standardDeviationTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CommercePlatformRequest<T> standardDeviationTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        addRelationScalarAggregate(name, subRequest, CustomerOrder.COMMERCE_PLATFORM_PROPERTY,
            CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationTotalAmountOfCustomerOrdersAs", name, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrders(){
        addRelationScalarAggregate("stdDevPopTotalAmountOfCustomerOrders", Q.customerOrders().unlimited(),
            CustomerOrder.COMMERCE_PLATFORM_PROPERTY, CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrders", null, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrdersAs(String name){
        return squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CommercePlatformRequest<T> squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        addRelationScalarAggregate(name, subRequest, CustomerOrder.COMMERCE_PLATFORM_PROPERTY,
            CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrdersAs", name, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> sampleVarianceTotalAmountOfCustomerOrders(){
        addRelationScalarAggregate("varSampTotalAmountOfCustomerOrders", Q.customerOrders().unlimited(),
            CustomerOrder.COMMERCE_PLATFORM_PROPERTY, CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceTotalAmountOfCustomerOrders", null, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> sampleVarianceTotalAmountOfCustomerOrdersAs(String name){
        return sampleVarianceTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CommercePlatformRequest<T> sampleVarianceTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        addRelationScalarAggregate(name, subRequest, CustomerOrder.COMMERCE_PLATFORM_PROPERTY,
            CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceTotalAmountOfCustomerOrdersAs", name, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> samplePopulationVarianceTotalAmountOfCustomerOrders(){
        addRelationScalarAggregate("varPopTotalAmountOfCustomerOrders", Q.customerOrders().unlimited(),
            CustomerOrder.COMMERCE_PLATFORM_PROPERTY, CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceTotalAmountOfCustomerOrders", null, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> samplePopulationVarianceTotalAmountOfCustomerOrdersAs(String name){
        return samplePopulationVarianceTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CommercePlatformRequest<T> samplePopulationVarianceTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        addRelationScalarAggregate(name, subRequest, CustomerOrder.COMMERCE_PLATFORM_PROPERTY,
            CustomerOrder.TOTAL_AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceTotalAmountOfCustomerOrdersAs", name, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> minQuantityOfOrderLines(){
        addRelationScalarAggregate("minQuantityOfOrderLines", Q.orderLines().unlimited(),
            OrderLine.COMMERCE_PLATFORM_PROPERTY, OrderLine.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minQuantityOfOrderLines", null, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> minQuantityOfOrderLinesAs(String name){
        return minQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public CommercePlatformRequest<T> minQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderLine.COMMERCE_PLATFORM_PROPERTY,
            OrderLine.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minQuantityOfOrderLinesAs", name, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> maxQuantityOfOrderLines(){
        addRelationScalarAggregate("maxQuantityOfOrderLines", Q.orderLines().unlimited(),
            OrderLine.COMMERCE_PLATFORM_PROPERTY, OrderLine.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxQuantityOfOrderLines", null, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> maxQuantityOfOrderLinesAs(String name){
        return maxQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public CommercePlatformRequest<T> maxQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderLine.COMMERCE_PLATFORM_PROPERTY,
            OrderLine.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxQuantityOfOrderLinesAs", name, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> sumQuantityOfOrderLines(){
        addRelationScalarAggregate("sumQuantityOfOrderLines", Q.orderLines().unlimited(),
            OrderLine.COMMERCE_PLATFORM_PROPERTY, OrderLine.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumQuantityOfOrderLines", null, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> sumQuantityOfOrderLinesAs(String name){
        return sumQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public CommercePlatformRequest<T> sumQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderLine.COMMERCE_PLATFORM_PROPERTY,
            OrderLine.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumQuantityOfOrderLinesAs", name, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> avgQuantityOfOrderLines(){
        addRelationScalarAggregate("avgQuantityOfOrderLines", Q.orderLines().unlimited(),
            OrderLine.COMMERCE_PLATFORM_PROPERTY, OrderLine.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgQuantityOfOrderLines", null, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> avgQuantityOfOrderLinesAs(String name){
        return avgQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public CommercePlatformRequest<T> avgQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderLine.COMMERCE_PLATFORM_PROPERTY,
            OrderLine.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgQuantityOfOrderLinesAs", name, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> standardDeviationQuantityOfOrderLines(){
        addRelationScalarAggregate("stdDevQuantityOfOrderLines", Q.orderLines().unlimited(),
            OrderLine.COMMERCE_PLATFORM_PROPERTY, OrderLine.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationQuantityOfOrderLines", null, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> standardDeviationQuantityOfOrderLinesAs(String name){
        return standardDeviationQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public CommercePlatformRequest<T> standardDeviationQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderLine.COMMERCE_PLATFORM_PROPERTY,
            OrderLine.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationQuantityOfOrderLinesAs", name, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> squareRootOfPopulationStandardDeviationQuantityOfOrderLines(){
        addRelationScalarAggregate("stdDevPopQuantityOfOrderLines", Q.orderLines().unlimited(),
            OrderLine.COMMERCE_PLATFORM_PROPERTY, OrderLine.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationQuantityOfOrderLines", null, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> squareRootOfPopulationStandardDeviationQuantityOfOrderLinesAs(String name){
        return squareRootOfPopulationStandardDeviationQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public CommercePlatformRequest<T> squareRootOfPopulationStandardDeviationQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderLine.COMMERCE_PLATFORM_PROPERTY,
            OrderLine.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationQuantityOfOrderLinesAs", name, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> sampleVarianceQuantityOfOrderLines(){
        addRelationScalarAggregate("varSampQuantityOfOrderLines", Q.orderLines().unlimited(),
            OrderLine.COMMERCE_PLATFORM_PROPERTY, OrderLine.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceQuantityOfOrderLines", null, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> sampleVarianceQuantityOfOrderLinesAs(String name){
        return sampleVarianceQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public CommercePlatformRequest<T> sampleVarianceQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderLine.COMMERCE_PLATFORM_PROPERTY,
            OrderLine.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceQuantityOfOrderLinesAs", name, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
        return this;
    }
    public CommercePlatformRequest<T> samplePopulationVarianceQuantityOfOrderLines(){
        addRelationScalarAggregate("varPopQuantityOfOrderLines", Q.orderLines().unlimited(),
            OrderLine.COMMERCE_PLATFORM_PROPERTY, OrderLine.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceQuantityOfOrderLines", null, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
        return this;
    }

    public CommercePlatformRequest<T> samplePopulationVarianceQuantityOfOrderLinesAs(String name){
        return samplePopulationVarianceQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public CommercePlatformRequest<T> samplePopulationVarianceQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderLine.COMMERCE_PLATFORM_PROPERTY,
            OrderLine.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceQuantityOfOrderLinesAs", name, CommercePlatform.ORDER_LINE_LIST_PROPERTY));
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