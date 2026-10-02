
package com.teaql.tracechainservice.customerorder;

import com.teaql.tracechainservice.Q;
import com.teaql.tracechainservice.orderitem.OrderItem;
import com.teaql.tracechainservice.orderitem.OrderItemRequest;
import com.teaql.tracechainservice.payment.Payment;
import com.teaql.tracechainservice.payment.PaymentRequest;
import com.teaql.tracechainservice.platform.Platform;
import com.teaql.tracechainservice.platform.PlatformRequest;
import com.teaql.tracechainservice.shipment.Shipment;
import com.teaql.tracechainservice.shipment.ShipmentRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;

public class CustomerOrderRequest<T extends CustomerOrder> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public CustomerOrderRequest(Class<T> returnType){
        super(returnType, () -> (T) new CustomerOrder());
        selectId();
        selectVersion();
    }

    public CustomerOrderRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public CustomerOrderRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public CustomerOrderRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public CustomerOrderRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public CustomerOrderRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public CustomerOrderRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public CustomerOrderRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public CustomerOrderRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public CustomerOrderRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public CustomerOrderRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public CustomerOrderRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (CustomerOrderRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public CustomerOrderRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public CustomerOrderRequest<T> matchingAnyOf(CustomerOrderRequest customerOrder){
        super.internalMatchAny(customerOrder);
        return this;
    }

    public CustomerOrderRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public CustomerOrderRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public CustomerOrderRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public CustomerOrderRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectPlatformIdOnly().selectOrderNumber().selectDescription().selectVersion();
    }

    public CustomerOrderRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public CustomerOrderRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectPlatform().selectOrderNumber().selectDescription().selectVersion();
    }

    public CustomerOrderRequest<T> selectChildren(){
        super.selectAny();
        selectOrderItemList().selectPaymentList().selectShipmentList();
        return selectId().selectPlatform().selectOrderNumber().selectDescription().selectVersion();
    }


    public CustomerOrderRequest<T> selectId(){
       selectProperty(CustomerOrder.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerOrderRequest<T> unselectId(){
       unselectProperty(CustomerOrder.ID_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> selectPlatformIdOnly(){
       selectProperty(CustomerOrder.PLATFORM_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> selectPlatform(){
        return selectPlatformWith(Q.platforms().unlimited().selectSelf());
    }

    public CustomerOrderRequest<T> selectPlatformWith(PlatformRequest platform){
       selectProperty(CustomerOrder.PLATFORM_PROPERTY);
       enhanceRelation(CustomerOrder.PLATFORM_PROPERTY, platform);
       return this;
    }

    public CustomerOrderRequest<T> unselectPlatform(){
       unselectProperty(CustomerOrder.PLATFORM_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> selectOrderNumber(){
       selectProperty(CustomerOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    /**
     * fill the orderNumber with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  orderNumber) to fetch orderNumber property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerOrderRequest<T> unselectOrderNumber(){
       unselectProperty(CustomerOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> selectDescription(){
       selectProperty(CustomerOrder.DESCRIPTION_PROPERTY);
       return this;
    }

    /**
     * fill the description with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  description) to fetch description property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerOrderRequest<T> unselectDescription(){
       unselectProperty(CustomerOrder.DESCRIPTION_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> selectVersion(){
       selectProperty(CustomerOrder.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerOrderRequest<T> unselectVersion(){
       unselectProperty(CustomerOrder.VERSION_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> selectOrderItemList(){
       return selectOrderItemListWith(Q.orderItems().selectSelf());
    }

    public CustomerOrderRequest<T> selectOrderItemListWith(OrderItemRequest orderItemList){
       enhanceRelation(CustomerOrder.ORDER_ITEM_LIST_PROPERTY, orderItemList);
       return this;
    }
    public CustomerOrderRequest<T> selectPaymentList(){
       return selectPaymentListWith(Q.payments().selectSelf());
    }

    public CustomerOrderRequest<T> selectPaymentListWith(PaymentRequest paymentList){
       enhanceRelation(CustomerOrder.PAYMENT_LIST_PROPERTY, paymentList);
       return this;
    }
    public CustomerOrderRequest<T> selectShipmentList(){
       return selectShipmentListWith(Q.shipments().selectSelf());
    }

    public CustomerOrderRequest<T> selectShipmentListWith(ShipmentRequest shipmentList){
       enhanceRelation(CustomerOrder.SHIPMENT_LIST_PROPERTY, shipmentList);
       return this;
    }

    public CustomerOrderRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.ID_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public CustomerOrderRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public CustomerOrderRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public CustomerOrderRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public CustomerOrderRequest<T> filterByPlatform(Platform... platform){
      if (platform == null || platform.length == 0) {
        throw new IllegalArgumentException("filterByPlatform parameter platform cannot be empty");
      }
      return appendSearchCriteria(createPlatformCriteria(Operator.EQUAL, (Object[])platform));
    }

    public CustomerOrderRequest<T> withPlatform(Operator operator, Object... values){
       return appendSearchCriteria(createPlatformCriteria(operator, values));
    }

    public CustomerOrderRequest<T> withPlatformIsUnknown(){
       return withPlatform(Operator.IS_NULL);
    }

    public CustomerOrderRequest<T> withPlatformIsKnown(){
       return withPlatform(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createPlatformCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.PLATFORM_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> filterByPlatform(Long platform){
      if(platform == null){
         return this;
      }
      return withPlatform(Operator.EQUAL, platform);
    }
    public CustomerOrderRequest<T> withPlatformMatching(PlatformRequest platform){
       return appendSearchCriteria(new SubQuerySearchCriteria(CustomerOrder.PLATFORM_PROPERTY, platform, Platform.ID_PROPERTY));
    }

    public CustomerOrderRequest<T> withoutPlatformMatching(PlatformRequest platform){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(CustomerOrder.PLATFORM_PROPERTY, platform, Platform.ID_PROPERTY)));
    }

    public CustomerOrderRequest<T> filterByOrderNumber(String... orderNumber){
      if (orderNumber == null || orderNumber.length == 0) {
        throw new IllegalArgumentException("filterByOrderNumber parameter orderNumber cannot be empty");
      }
      return appendSearchCriteria(createOrderNumberCriteria(Operator.EQUAL, (Object[])orderNumber));
    }

    public CustomerOrderRequest<T> withOrderNumber(Operator operator, Object... values){
       return appendSearchCriteria(createOrderNumberCriteria(operator, values));
    }

    public CustomerOrderRequest<T> withOrderNumberIsUnknown(){
       return withOrderNumber(Operator.IS_NULL);
    }

    public CustomerOrderRequest<T> withOrderNumberIsKnown(){
       return withOrderNumber(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createOrderNumberCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.ORDER_NUMBER_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> withOrderNumberIsNot(String orderNumber){
       return withOrderNumber(Operator.NOT_EQUAL, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberIn(String... orderNumber){
       return withOrderNumber(Operator.IN, (Object[])orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberNotIn(String... orderNumber){
       return withOrderNumber(Operator.NOT_IN, (Object[])orderNumber);
    }
    public CustomerOrderRequest<T> withOrderNumberGreaterThan(String orderNumber){
       return withOrderNumber(Operator.GREATER_THAN, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberGreaterThanOrEqualTo(String orderNumber){
       return withOrderNumber(Operator.GREATER_THAN_OR_EQUAL, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberLessThan(String orderNumber){
       return withOrderNumber(Operator.LESS_THAN, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberLessThanOrEqualTo(String orderNumber){
       return withOrderNumber(Operator.LESS_THAN_OR_EQUAL, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberBetween(String startOfOrderNumber, String endOfOrderNumber){
       return withOrderNumber(Operator.BETWEEN, startOfOrderNumber, endOfOrderNumber);
    }
    public CustomerOrderRequest<T> withOrderNumberStartingWith(String orderNumber){
       return withOrderNumber(Operator.BEGIN_WITH, orderNumber);
    }
    public CustomerOrderRequest<T> withOrderNumberContaining(String orderNumber){
       return withOrderNumber(Operator.CONTAIN, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberNotContaining(String orderNumber){
       return withOrderNumber(Operator.NOT_CONTAIN, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberNotStartingWith(String orderNumber){
       return withOrderNumber(Operator.NOT_BEGIN_WITH, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberEndingWith(String orderNumber){
       return withOrderNumber(Operator.END_WITH, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberNotEndingWith(String orderNumber){
       return withOrderNumber(Operator.NOT_END_WITH, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberIs(String orderNumber){
       return withOrderNumber(Operator.EQUAL, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberSoundingLike(String orderNumber){
       return withOrderNumber(Operator.SOUNDS_LIKE, orderNumber);
    }



    public CustomerOrderRequest<T> filterByDescription(String... description){
      if (description == null || description.length == 0) {
        throw new IllegalArgumentException("filterByDescription parameter description cannot be empty");
      }
      return appendSearchCriteria(createDescriptionCriteria(Operator.EQUAL, (Object[])description));
    }

    public CustomerOrderRequest<T> withDescription(Operator operator, Object... values){
       return appendSearchCriteria(createDescriptionCriteria(operator, values));
    }

    public CustomerOrderRequest<T> withDescriptionIsUnknown(){
       return withDescription(Operator.IS_NULL);
    }

    public CustomerOrderRequest<T> withDescriptionIsKnown(){
       return withDescription(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createDescriptionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.DESCRIPTION_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> withDescriptionIsNot(String description){
       return withDescription(Operator.NOT_EQUAL, description);
    }

    public CustomerOrderRequest<T> withDescriptionIn(String... description){
       return withDescription(Operator.IN, (Object[])description);
    }

    public CustomerOrderRequest<T> withDescriptionNotIn(String... description){
       return withDescription(Operator.NOT_IN, (Object[])description);
    }
    public CustomerOrderRequest<T> withDescriptionGreaterThan(String description){
       return withDescription(Operator.GREATER_THAN, description);
    }

    public CustomerOrderRequest<T> withDescriptionGreaterThanOrEqualTo(String description){
       return withDescription(Operator.GREATER_THAN_OR_EQUAL, description);
    }

    public CustomerOrderRequest<T> withDescriptionLessThan(String description){
       return withDescription(Operator.LESS_THAN, description);
    }

    public CustomerOrderRequest<T> withDescriptionLessThanOrEqualTo(String description){
       return withDescription(Operator.LESS_THAN_OR_EQUAL, description);
    }

    public CustomerOrderRequest<T> withDescriptionBetween(String startOfDescription, String endOfDescription){
       return withDescription(Operator.BETWEEN, startOfDescription, endOfDescription);
    }
    public CustomerOrderRequest<T> withDescriptionStartingWith(String description){
       return withDescription(Operator.BEGIN_WITH, description);
    }
    public CustomerOrderRequest<T> withDescriptionContaining(String description){
       return withDescription(Operator.CONTAIN, description);
    }

    public CustomerOrderRequest<T> withDescriptionNotContaining(String description){
       return withDescription(Operator.NOT_CONTAIN, description);
    }

    public CustomerOrderRequest<T> withDescriptionNotStartingWith(String description){
       return withDescription(Operator.NOT_BEGIN_WITH, description);
    }

    public CustomerOrderRequest<T> withDescriptionEndingWith(String description){
       return withDescription(Operator.END_WITH, description);
    }

    public CustomerOrderRequest<T> withDescriptionNotEndingWith(String description){
       return withDescription(Operator.NOT_END_WITH, description);
    }

    public CustomerOrderRequest<T> withDescriptionIs(String description){
       return withDescription(Operator.EQUAL, description);
    }

    public CustomerOrderRequest<T> withDescriptionSoundingLike(String description){
       return withDescription(Operator.SOUNDS_LIKE, description);
    }



    public CustomerOrderRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public CustomerOrderRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public CustomerOrderRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public CustomerOrderRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.VERSION_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public CustomerOrderRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public CustomerOrderRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public CustomerOrderRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public CustomerOrderRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public CustomerOrderRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public CustomerOrderRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public CustomerOrderRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public CustomerOrderRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public CustomerOrderRequest<T> withOrderItemListMatching(OrderItemRequest orderItemRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(CustomerOrder.ID_PROPERTY, orderItemRequest, OrderItem.CUSTOMER_ORDER_PROPERTY));
    }

    public CustomerOrderRequest<T> withoutOrderItemListMatching(OrderItemRequest orderItemRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(CustomerOrder.ID_PROPERTY, orderItemRequest, OrderItem.CUSTOMER_ORDER_PROPERTY)));
    }

    public CustomerOrderRequest<T> haveOrderItems(){
        return withOrderItemListMatching(Q.orderItems().unlimited());
    }

    public CustomerOrderRequest<T> haveNoOrderItems(){
        return withoutOrderItemListMatching(Q.orderItems().unlimited());
    }
    public CustomerOrderRequest<T> withPaymentListMatching(PaymentRequest paymentRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(CustomerOrder.ID_PROPERTY, paymentRequest, Payment.CUSTOMER_ORDER_PROPERTY));
    }

    public CustomerOrderRequest<T> withoutPaymentListMatching(PaymentRequest paymentRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(CustomerOrder.ID_PROPERTY, paymentRequest, Payment.CUSTOMER_ORDER_PROPERTY)));
    }

    public CustomerOrderRequest<T> havePayments(){
        return withPaymentListMatching(Q.payments().unlimited());
    }

    public CustomerOrderRequest<T> haveNoPayments(){
        return withoutPaymentListMatching(Q.payments().unlimited());
    }
    public CustomerOrderRequest<T> withShipmentListMatching(ShipmentRequest shipmentRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(CustomerOrder.ID_PROPERTY, shipmentRequest, Shipment.CUSTOMER_ORDER_PROPERTY));
    }

    public CustomerOrderRequest<T> withoutShipmentListMatching(ShipmentRequest shipmentRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(CustomerOrder.ID_PROPERTY, shipmentRequest, Shipment.CUSTOMER_ORDER_PROPERTY)));
    }

    public CustomerOrderRequest<T> haveShipments(){
        return withShipmentListMatching(Q.shipments().unlimited());
    }

    public CustomerOrderRequest<T> haveNoShipments(){
        return withoutShipmentListMatching(Q.shipments().unlimited());
    }

    public CustomerOrderRequest<T> count(){
        super.count();
        return this;
    }
    public CustomerOrderRequest<T> countAs(String retName){
        super.count(retName);
        return this;
    }
    public CustomerOrderRequest<T> groupByPlatformWithDetails(){
       return groupByPlatformWithDetails(Q.platforms().unlimited());
    }

    public CustomerOrderRequest<T> groupByPlatformWithDetails(PlatformRequest subRequest){
       aggregate(CustomerOrder.PLATFORM_PROPERTY, subRequest);
       return this;
    }




    public CustomerOrderRequest<T> groupByOrderItemsWithDetails(OrderItemRequest subRequest){
       aggregate(CustomerOrder.ORDER_ITEM_LIST_PROPERTY, subRequest);
       return this;
    }
    public CustomerOrderRequest<T> groupByPaymentsWithDetails(PaymentRequest subRequest){
       aggregate(CustomerOrder.PAYMENT_LIST_PROPERTY, subRequest);
       return this;
    }
    public CustomerOrderRequest<T> groupByShipmentsWithDetails(ShipmentRequest subRequest){
       aggregate(CustomerOrder.SHIPMENT_LIST_PROPERTY, subRequest);
       return this;
    }

    public CustomerOrderRequest<T> groupById(){
       groupBy(CustomerOrder.ID_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByIdAs(String retName){
       groupBy(retName, CustomerOrder.ID_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.ID_PROPERTY, function);
       return this;
    }
    public CustomerOrderRequest<T> groupByPlatformWith(PlatformRequest subRequest){
       groupBy(CustomerOrder.PLATFORM_PROPERTY, subRequest);
       return this;
    }
    public CustomerOrderRequest<T> groupByPlatform(){
       groupBy(CustomerOrder.PLATFORM_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByPlatformAs(String retName){
       groupBy(retName, CustomerOrder.PLATFORM_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByPlatformWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.PLATFORM_PROPERTY, function);
       return this;
    }

    public CustomerOrderRequest<T> groupByOrderNumber(){
       groupBy(CustomerOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByOrderNumberAs(String retName){
       groupBy(retName, CustomerOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByOrderNumberWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.ORDER_NUMBER_PROPERTY, function);
       return this;
    }

    public CustomerOrderRequest<T> groupByDescription(){
       groupBy(CustomerOrder.DESCRIPTION_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByDescriptionAs(String retName){
       groupBy(retName, CustomerOrder.DESCRIPTION_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByDescriptionWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.DESCRIPTION_PROPERTY, function);
       return this;
    }

    public CustomerOrderRequest<T> groupByVersion(){
       groupBy(CustomerOrder.VERSION_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByVersionAs(String retName){
       groupBy(retName, CustomerOrder.VERSION_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.VERSION_PROPERTY, function);
       return this;
    }



    public CustomerOrderRequest<T> orderByIdAscending(){
       addOrderByAscending(CustomerOrder.ID_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByIdDescending(){
       addOrderByDescending(CustomerOrder.ID_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByPlatformAscending(){
       addOrderByAscending(CustomerOrder.PLATFORM_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByPlatformDescending(){
       addOrderByDescending(CustomerOrder.PLATFORM_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByOrderNumberAscending(){
       addOrderByAscending(CustomerOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByOrderNumberDescending(){
       addOrderByDescending(CustomerOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> orderByOrderNumberAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(CustomerOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByOrderNumberDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(CustomerOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> orderByDescriptionAscending(){
       addOrderByAscending(CustomerOrder.DESCRIPTION_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByDescriptionDescending(){
       addOrderByDescending(CustomerOrder.DESCRIPTION_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> orderByDescriptionAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(CustomerOrder.DESCRIPTION_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByDescriptionDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(CustomerOrder.DESCRIPTION_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> orderByVersionAscending(){
       addOrderByAscending(CustomerOrder.VERSION_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByVersionDescending(){
       addOrderByDescending(CustomerOrder.VERSION_PROPERTY);
       return this;
    }


    public CustomerOrderRequest<T> statsFromOrderItemsAs(String name, OrderItemRequest subRequest){
       return statsFromOrderItemsAs(name, subRequest, false);
    }

    public CustomerOrderRequest<T> statsFromOrderItemsAs(String name, OrderItemRequest subRequest, boolean singleResult){
       subRequest.setPartitionProperty(OrderItem.CUSTOMER_ORDER_PROPERTY);
       addAggregateDynamicProperty(name, subRequest, singleResult);
       return this;
    }

    public CustomerOrderRequest<T> statsFromOrderItems(OrderItemRequest subRequest){
       return statsFromOrderItemsAs(REFINEMENTS, subRequest);
    }
    public CustomerOrderRequest<T> statsFromPaymentsAs(String name, PaymentRequest subRequest){
       return statsFromPaymentsAs(name, subRequest, false);
    }

    public CustomerOrderRequest<T> statsFromPaymentsAs(String name, PaymentRequest subRequest, boolean singleResult){
       subRequest.setPartitionProperty(Payment.CUSTOMER_ORDER_PROPERTY);
       addAggregateDynamicProperty(name, subRequest, singleResult);
       return this;
    }

    public CustomerOrderRequest<T> statsFromPayments(PaymentRequest subRequest){
       return statsFromPaymentsAs(REFINEMENTS, subRequest);
    }
    public CustomerOrderRequest<T> statsFromShipmentsAs(String name, ShipmentRequest subRequest){
       return statsFromShipmentsAs(name, subRequest, false);
    }

    public CustomerOrderRequest<T> statsFromShipmentsAs(String name, ShipmentRequest subRequest, boolean singleResult){
       subRequest.setPartitionProperty(Shipment.CUSTOMER_ORDER_PROPERTY);
       addAggregateDynamicProperty(name, subRequest, singleResult);
       return this;
    }

    public CustomerOrderRequest<T> statsFromShipments(ShipmentRequest subRequest){
       return statsFromShipmentsAs(REFINEMENTS, subRequest);
    }
    public PlatformRequest rollUpToPlatform(){
       PlatformRequest platform = Q.platforms().unlimited();
       this.withPlatformMatching(platform)
           .groupByPlatformWith(platform);
       return platform;
    }




    public CustomerOrderRequest<T> countOrderItems(){
        return countOrderItemsAs("Count");
    }

    public CustomerOrderRequest<T> countOrderItemsAs(String name){
        return countOrderItemsWith(name, Q.orderItems().unlimited());
    }

    public CustomerOrderRequest<T> countOrderItemsWith(String name, OrderItemRequest subRequest){
        return statsFromOrderItemsAs(name, subRequest.count(), true);
    }
    public CustomerOrderRequest<T> countPayments(){
        return countPaymentsAs("Count");
    }

    public CustomerOrderRequest<T> countPaymentsAs(String name){
        return countPaymentsWith(name, Q.payments().unlimited());
    }

    public CustomerOrderRequest<T> countPaymentsWith(String name, PaymentRequest subRequest){
        return statsFromPaymentsAs(name, subRequest.count(), true);
    }
    public CustomerOrderRequest<T> countShipments(){
        return countShipmentsAs("Count");
    }

    public CustomerOrderRequest<T> countShipmentsAs(String name){
        return countShipmentsWith(name, Q.shipments().unlimited());
    }

    public CustomerOrderRequest<T> countShipmentsWith(String name, ShipmentRequest subRequest){
        return statsFromShipmentsAs(name, subRequest.count(), true);
    }

   public CustomerOrderRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform){
       return facetByPlatformAs(facetName, platform, true);
   }

   public CustomerOrderRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform, boolean includeAllFacets){
       addFacet(facetName, CustomerOrder.PLATFORM_PROPERTY, platform, includeAllFacets);
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public CustomerOrderRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public CustomerOrderRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public CustomerOrderRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public CustomerOrderRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public CustomerOrderRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public CustomerOrderRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}