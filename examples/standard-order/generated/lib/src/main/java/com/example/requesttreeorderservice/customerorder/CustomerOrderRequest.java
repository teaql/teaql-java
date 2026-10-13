
package com.example.requesttreeorderservice.customerorder;

import com.example.requesttreeorderservice.Q;
import com.example.requesttreeorderservice.customer.Customer;
import com.example.requesttreeorderservice.customer.CustomerRequest;
import com.example.requesttreeorderservice.orderitem.OrderItem;
import com.example.requesttreeorderservice.orderitem.OrderItemRequest;
import com.example.requesttreeorderservice.orderstatus.OrderStatus;
import com.example.requesttreeorderservice.orderstatus.OrderStatusRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

public class CustomerOrderRequest<T extends CustomerOrder> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public CustomerOrderRequest(Class<T> returnType){
        this(returnType, () -> (T) new CustomerOrder());
    }

    protected CustomerOrderRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
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
        return selectId().selectCustomerIdOnly().selectStatusIdOnly().selectOrderNumber().selectOrderDate().selectTotalAmount().selectVersion();
    }

    public CustomerOrderRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public CustomerOrderRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(CustomerOrder.ID_PROPERTY);
        selectProperty(CustomerOrder.CUSTOMER_PROPERTY);
        selectProperty(CustomerOrder.STATUS_PROPERTY);
        selectProperty(CustomerOrder.ORDER_NUMBER_PROPERTY);
        selectProperty(CustomerOrder.ORDER_DATE_PROPERTY);
        selectProperty(CustomerOrder.TOTAL_AMOUNT_PROPERTY);
        selectProperty(CustomerOrder.VERSION_PROPERTY);
        return this;
    }

    public CustomerOrderRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectCustomer().selectStatus().selectOrderNumber().selectOrderDate().selectTotalAmount().selectVersion();
    }

    public CustomerOrderRequest<T> selectChildren(){
        super.selectAny();
        selectOrderItemList();
        return selectId().selectCustomer().selectStatus().selectOrderNumber().selectOrderDate().selectTotalAmount().selectVersion();
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
    public CustomerOrderRequest<T> selectCustomerIdOnly(){
       selectProperty(CustomerOrder.CUSTOMER_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> selectCustomer(){
        return selectCustomerWith(Q.customers().unlimited().selectSelf());
    }

    public CustomerOrderRequest<T> selectCustomerWith(CustomerRequest customer){
       selectProperty(CustomerOrder.CUSTOMER_PROPERTY);
       enhanceRelation(CustomerOrder.CUSTOMER_PROPERTY, customer,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectCustomerWith", null, CustomerOrder.CUSTOMER_PROPERTY));
       return this;
    }

    public CustomerOrderRequest<T> unselectCustomer(){
       unselectProperty(CustomerOrder.CUSTOMER_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> selectStatusIdOnly(){
       selectProperty(CustomerOrder.STATUS_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> selectStatus(){
        return selectStatusWith(Q.orderStatuses().unlimited().selectSelf());
    }

    public CustomerOrderRequest<T> selectStatusWith(OrderStatusRequest status){
       selectProperty(CustomerOrder.STATUS_PROPERTY);
       enhanceRelation(CustomerOrder.STATUS_PROPERTY, status,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectStatusWith", null, CustomerOrder.STATUS_PROPERTY));
       return this;
    }

    public CustomerOrderRequest<T> unselectStatus(){
       unselectProperty(CustomerOrder.STATUS_PROPERTY);
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
    public CustomerOrderRequest<T> selectOrderDate(){
       selectProperty(CustomerOrder.ORDER_DATE_PROPERTY);
       return this;
    }

    /**
     * fill the orderDate with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  orderDate) to fetch orderDate property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerOrderRequest<T> unselectOrderDate(){
       unselectProperty(CustomerOrder.ORDER_DATE_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> selectTotalAmount(){
       selectProperty(CustomerOrder.TOTAL_AMOUNT_PROPERTY);
       return this;
    }

    /**
     * fill the totalAmount with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  totalAmount) to fetch totalAmount property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the totalAmount with customized aggrFunction, TEAQL uses ({aggrFunction}(totalAmount) AS totalAmount to fetch totalAmount property.
     * @param aggrFunction  aggrFunction
     */
    public CustomerOrderRequest<T> selectTotalAmount(AggrFunction aggrFunction){
       selectProperty(CustomerOrder.TOTAL_AMOUNT_PROPERTY, aggrFunction);
       return this;
    }


    public CustomerOrderRequest<T> unselectTotalAmount(){
       unselectProperty(CustomerOrder.TOTAL_AMOUNT_PROPERTY);
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
       enhanceRelation(CustomerOrder.ORDER_ITEM_LIST_PROPERTY, orderItemList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectOrderItemListWith", null, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
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



    public CustomerOrderRequest<T> filterByCustomer(Customer... customer){
      if (customer == null || customer.length == 0) {
        throw new IllegalArgumentException("filterByCustomer parameter customer cannot be empty");
      }
      return appendSearchCriteria(createCustomerCriteria(Operator.EQUAL, (Object[])customer));
    }

    public CustomerOrderRequest<T> withCustomer(Operator operator, Object... values){
       return appendSearchCriteria(createCustomerCriteria(operator, values));
    }

    public CustomerOrderRequest<T> withCustomerIsUnknown(){
       return withCustomer(Operator.IS_NULL);
    }

    public CustomerOrderRequest<T> withCustomerIsKnown(){
       return withCustomer(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCustomerCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.CUSTOMER_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> filterByCustomer(Long customer){
      if(customer == null){
         return this;
      }
      return withCustomer(Operator.EQUAL, customer);
    }
    public CustomerOrderRequest<T> withCustomerMatching(CustomerRequest customer){
       return appendSearchCriteria(new SubQuerySearchCriteria(CustomerOrder.CUSTOMER_PROPERTY, customer, Customer.ID_PROPERTY));
    }

    public CustomerOrderRequest<T> withoutCustomerMatching(CustomerRequest customer){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(CustomerOrder.CUSTOMER_PROPERTY, customer, Customer.ID_PROPERTY)));
    }

    public CustomerOrderRequest<T> filterByStatus(OrderStatus... status){
      if (status == null || status.length == 0) {
        throw new IllegalArgumentException("filterByStatus parameter status cannot be empty");
      }
      return appendSearchCriteria(createStatusCriteria(Operator.EQUAL, (Object[])status));
    }

    public CustomerOrderRequest<T> withStatus(Operator operator, Object... values){
       return appendSearchCriteria(createStatusCriteria(operator, values));
    }

    public CustomerOrderRequest<T> withStatusIsUnknown(){
       return withStatus(Operator.IS_NULL);
    }

    public CustomerOrderRequest<T> withStatusIsKnown(){
       return withStatus(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createStatusCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.STATUS_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> filterByStatus(Long status){
      if(status == null){
         return this;
      }
      return withStatus(Operator.EQUAL, status);
    }
    public CustomerOrderRequest<T> withStatusMatching(OrderStatusRequest status){
       return appendSearchCriteria(new SubQuerySearchCriteria(CustomerOrder.STATUS_PROPERTY, status, OrderStatus.ID_PROPERTY));
    }

    public CustomerOrderRequest<T> withoutStatusMatching(OrderStatusRequest status){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(CustomerOrder.STATUS_PROPERTY, status, OrderStatus.ID_PROPERTY)));
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



    public CustomerOrderRequest<T> filterByOrderDate(LocalDate... orderDate){
      if (orderDate == null || orderDate.length == 0) {
        throw new IllegalArgumentException("filterByOrderDate parameter orderDate cannot be empty");
      }
      return appendSearchCriteria(createOrderDateCriteria(Operator.EQUAL, (Object[])orderDate));
    }

    public CustomerOrderRequest<T> withOrderDate(Operator operator, Object... values){
       return appendSearchCriteria(createOrderDateCriteria(operator, values));
    }

    public CustomerOrderRequest<T> withOrderDateIsUnknown(){
       return withOrderDate(Operator.IS_NULL);
    }

    public CustomerOrderRequest<T> withOrderDateIsKnown(){
       return withOrderDate(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createOrderDateCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.ORDER_DATE_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> withOrderDateIs(LocalDate orderDate){
       return withOrderDate(Operator.EQUAL, orderDate);
    }

    public CustomerOrderRequest<T> withOrderDateIsNot(LocalDate orderDate){
       return withOrderDate(Operator.NOT_EQUAL, orderDate);
    }

    public CustomerOrderRequest<T> withOrderDateIn(LocalDate... orderDate){
       return withOrderDate(Operator.IN, (Object[])orderDate);
    }

    public CustomerOrderRequest<T> withOrderDateNotIn(LocalDate... orderDate){
       return withOrderDate(Operator.NOT_IN, (Object[])orderDate);
    }
    public CustomerOrderRequest<T> withOrderDateGreaterThan(LocalDate orderDate){
       return withOrderDate(Operator.GREATER_THAN, orderDate);
    }

    public CustomerOrderRequest<T> withOrderDateGreaterThanOrEqualTo(LocalDate orderDate){
       return withOrderDate(Operator.GREATER_THAN_OR_EQUAL, orderDate);
    }

    public CustomerOrderRequest<T> withOrderDateLessThan(LocalDate orderDate){
       return withOrderDate(Operator.LESS_THAN, orderDate);
    }

    public CustomerOrderRequest<T> withOrderDateLessThanOrEqualTo(LocalDate orderDate){
       return withOrderDate(Operator.LESS_THAN_OR_EQUAL, orderDate);
    }

    public CustomerOrderRequest<T> withOrderDateBetween(LocalDate startOfOrderDate, LocalDate endOfOrderDate){
       return withOrderDate(Operator.BETWEEN, startOfOrderDate, endOfOrderDate);
    }
    public CustomerOrderRequest<T> withOrderDateBefore(LocalDate orderDate){
       return withOrderDate(Operator.LESS_THAN, orderDate);
    }

    public CustomerOrderRequest<T> withOrderDateBefore(Date orderDate){
       return withOrderDate(Operator.LESS_THAN, orderDate);
    }

    public CustomerOrderRequest<T> withOrderDateAfter(LocalDate orderDate){
       return withOrderDate(Operator.GREATER_THAN, orderDate);
    }

    public CustomerOrderRequest<T> withOrderDateAfter(Date orderDate){
       return withOrderDate(Operator.GREATER_THAN, orderDate);
    }

    public CustomerOrderRequest<T> withOrderDateBetween(Date startOfOrderDate, Date endOfOrderDate){
       return withOrderDate(Operator.BETWEEN, startOfOrderDate, endOfOrderDate);
    }




    public CustomerOrderRequest<T> filterByTotalAmount(BigDecimal... totalAmount){
      if (totalAmount == null || totalAmount.length == 0) {
        throw new IllegalArgumentException("filterByTotalAmount parameter totalAmount cannot be empty");
      }
      return appendSearchCriteria(createTotalAmountCriteria(Operator.EQUAL, (Object[])totalAmount));
    }

    public CustomerOrderRequest<T> withTotalAmount(Operator operator, Object... values){
       return appendSearchCriteria(createTotalAmountCriteria(operator, values));
    }

    public CustomerOrderRequest<T> withTotalAmountIsUnknown(){
       return withTotalAmount(Operator.IS_NULL);
    }

    public CustomerOrderRequest<T> withTotalAmountIsKnown(){
       return withTotalAmount(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createTotalAmountCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.TOTAL_AMOUNT_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> withTotalAmountIs(BigDecimal totalAmount){
       return withTotalAmount(Operator.EQUAL, totalAmount);
    }

    public CustomerOrderRequest<T> withTotalAmountIsNot(BigDecimal totalAmount){
       return withTotalAmount(Operator.NOT_EQUAL, totalAmount);
    }

    public CustomerOrderRequest<T> withTotalAmountIn(BigDecimal... totalAmount){
       return withTotalAmount(Operator.IN, (Object[])totalAmount);
    }

    public CustomerOrderRequest<T> withTotalAmountNotIn(BigDecimal... totalAmount){
       return withTotalAmount(Operator.NOT_IN, (Object[])totalAmount);
    }
    public CustomerOrderRequest<T> withTotalAmountGreaterThan(BigDecimal totalAmount){
       return withTotalAmount(Operator.GREATER_THAN, totalAmount);
    }

    public CustomerOrderRequest<T> withTotalAmountGreaterThanOrEqualTo(BigDecimal totalAmount){
       return withTotalAmount(Operator.GREATER_THAN_OR_EQUAL, totalAmount);
    }

    public CustomerOrderRequest<T> withTotalAmountLessThan(BigDecimal totalAmount){
       return withTotalAmount(Operator.LESS_THAN, totalAmount);
    }

    public CustomerOrderRequest<T> withTotalAmountLessThanOrEqualTo(BigDecimal totalAmount){
       return withTotalAmount(Operator.LESS_THAN_OR_EQUAL, totalAmount);
    }

    public CustomerOrderRequest<T> withTotalAmountBetween(BigDecimal startOfTotalAmount, BigDecimal endOfTotalAmount){
       return withTotalAmount(Operator.BETWEEN, startOfTotalAmount, endOfTotalAmount);
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

    public CustomerOrderRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public CustomerOrderRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public CustomerOrderRequest minTotalAmount(){
        return minTotalAmountAs(prefix("minOf",CustomerOrder.TOTAL_AMOUNT_PROPERTY));
    }

    public CustomerOrderRequest minTotalAmountAs(String retName){
        super.min(retName, CustomerOrder.TOTAL_AMOUNT_PROPERTY);
        return this;
    }
    public CustomerOrderRequest maxTotalAmount(){
        return maxTotalAmountAs(prefix("maxOf",CustomerOrder.TOTAL_AMOUNT_PROPERTY));
    }

    public CustomerOrderRequest maxTotalAmountAs(String retName){
        super.max(retName, CustomerOrder.TOTAL_AMOUNT_PROPERTY);
        return this;
    }
    public CustomerOrderRequest sumTotalAmount(){
        return sumTotalAmountAs(prefix("sumOf",CustomerOrder.TOTAL_AMOUNT_PROPERTY));
    }

    public CustomerOrderRequest sumTotalAmountAs(String retName){
        super.sum(retName, CustomerOrder.TOTAL_AMOUNT_PROPERTY);
        return this;
    }
    public CustomerOrderRequest avgTotalAmount(){
        return avgTotalAmountAs(prefix("avgOf",CustomerOrder.TOTAL_AMOUNT_PROPERTY));
    }

    public CustomerOrderRequest avgTotalAmountAs(String retName){
        super.avg(retName, CustomerOrder.TOTAL_AMOUNT_PROPERTY);
        return this;
    }
    public CustomerOrderRequest standardDeviationTotalAmount(){
        return standardDeviationTotalAmountAs(prefix("standardDeviationOf",CustomerOrder.TOTAL_AMOUNT_PROPERTY));
    }

    public CustomerOrderRequest standardDeviationTotalAmountAs(String retName){
        super.standardDeviation(retName, CustomerOrder.TOTAL_AMOUNT_PROPERTY);
        return this;
    }
    public CustomerOrderRequest squareRootOfPopulationStandardDeviationTotalAmount(){
        return squareRootOfPopulationStandardDeviationTotalAmountAs(prefix("squareRootOfPopulationStandardDeviationOf",CustomerOrder.TOTAL_AMOUNT_PROPERTY));
    }

    public CustomerOrderRequest squareRootOfPopulationStandardDeviationTotalAmountAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, CustomerOrder.TOTAL_AMOUNT_PROPERTY);
        return this;
    }
    public CustomerOrderRequest sampleVarianceTotalAmount(){
        return sampleVarianceTotalAmountAs(prefix("sampleVarianceOf",CustomerOrder.TOTAL_AMOUNT_PROPERTY));
    }

    public CustomerOrderRequest sampleVarianceTotalAmountAs(String retName){
        super.sampleVariance(retName, CustomerOrder.TOTAL_AMOUNT_PROPERTY);
        return this;
    }
    public CustomerOrderRequest samplePopulationVarianceTotalAmount(){
        return samplePopulationVarianceTotalAmountAs(prefix("samplePopulationVarianceOf",CustomerOrder.TOTAL_AMOUNT_PROPERTY));
    }

    public CustomerOrderRequest samplePopulationVarianceTotalAmountAs(String retName){
        super.samplePopulationVariance(retName, CustomerOrder.TOTAL_AMOUNT_PROPERTY);
        return this;
    }
    public CustomerOrderRequest<T> groupByCustomerWithDetails(){
       return groupByCustomerWithDetails(Q.customers().unlimited());
    }

    public CustomerOrderRequest<T> groupByCustomerWithDetails(CustomerRequest subRequest){
       aggregate(CustomerOrder.CUSTOMER_PROPERTY, subRequest);
       return this;
    }

    public CustomerOrderRequest<T> groupByStatusWithDetails(){
       return groupByStatusWithDetails(Q.orderStatuses().unlimited());
    }

    public CustomerOrderRequest<T> groupByStatusWithDetails(OrderStatusRequest subRequest){
       aggregate(CustomerOrder.STATUS_PROPERTY, subRequest);
       return this;
    }





    public CustomerOrderRequest<T> groupByOrderItemsWithDetails(OrderItemRequest subRequest){
       aggregate(CustomerOrder.ORDER_ITEM_LIST_PROPERTY, subRequest);
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
    public CustomerOrderRequest<T> groupByCustomerWith(CustomerRequest subRequest){
       groupBy(CustomerOrder.CUSTOMER_PROPERTY, subRequest);
       return this;
    }
    public CustomerOrderRequest<T> groupByCustomer(){
       groupBy(CustomerOrder.CUSTOMER_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByCustomerAs(String retName){
       groupBy(retName, CustomerOrder.CUSTOMER_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByCustomerWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.CUSTOMER_PROPERTY, function);
       return this;
    }
    public CustomerOrderRequest<T> groupByStatusWith(OrderStatusRequest subRequest){
       groupBy(CustomerOrder.STATUS_PROPERTY, subRequest);
       return this;
    }
    public CustomerOrderRequest<T> groupByStatus(){
       groupBy(CustomerOrder.STATUS_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByStatusAs(String retName){
       groupBy(retName, CustomerOrder.STATUS_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByStatusWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.STATUS_PROPERTY, function);
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

    public CustomerOrderRequest<T> groupByOrderDate(){
       groupBy(CustomerOrder.ORDER_DATE_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByOrderDateAs(String retName){
       groupBy(retName, CustomerOrder.ORDER_DATE_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByOrderDateWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.ORDER_DATE_PROPERTY, function);
       return this;
    }

    public CustomerOrderRequest<T> groupByTotalAmount(){
       groupBy(CustomerOrder.TOTAL_AMOUNT_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByTotalAmountAs(String retName){
       groupBy(retName, CustomerOrder.TOTAL_AMOUNT_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByTotalAmountWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.TOTAL_AMOUNT_PROPERTY, function);
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

    public CustomerOrderRequest<T> withStatusIsNew(){
       filterByStatus(com.example.requesttreeorderservice.Constants.ORDER_STATUS_NEW);
       return this;
    }


    public CustomerOrderRequest<T> withStatusIsPaid(){
       filterByStatus(com.example.requesttreeorderservice.Constants.ORDER_STATUS_PAID);
       return this;
    }


    public CustomerOrderRequest<T> withStatusIsCancelled(){
       filterByStatus(com.example.requesttreeorderservice.Constants.ORDER_STATUS_CANCELLED);
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

    public CustomerOrderRequest<T> orderByCustomerAscending(){
       addOrderByAscending(CustomerOrder.CUSTOMER_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByCustomerDescending(){
       addOrderByDescending(CustomerOrder.CUSTOMER_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByStatusAscending(){
       addOrderByAscending(CustomerOrder.STATUS_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByStatusDescending(){
       addOrderByDescending(CustomerOrder.STATUS_PROPERTY);
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
    public CustomerOrderRequest<T> orderByOrderDateAscending(){
       addOrderByAscending(CustomerOrder.ORDER_DATE_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByOrderDateDescending(){
       addOrderByDescending(CustomerOrder.ORDER_DATE_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByTotalAmountAscending(){
       addOrderByAscending(CustomerOrder.TOTAL_AMOUNT_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByTotalAmountDescending(){
       addOrderByDescending(CustomerOrder.TOTAL_AMOUNT_PROPERTY);
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
       addRelationAggregate(name, subRequest, OrderItem.CUSTOMER_ORDER_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromOrderItemsAs", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
       return this;
    }

    public CustomerOrderRequest<T> statsFromOrderItems(OrderItemRequest subRequest){
       return statsFromOrderItemsAs(REFINEMENTS, subRequest);
    }
    public CustomerRequest rollUpToCustomer(){
       CustomerRequest customer = Q.customers().unlimited();
       this.withCustomerMatching(customer)
           .groupByCustomerWith(customer);
       return customer;
    }

    public OrderStatusRequest rollUpToStatus(){
       OrderStatusRequest status = Q.orderStatuses().unlimited();
       this.withStatusMatching(status)
           .groupByStatusWith(status);
       return status;
    }





    public CustomerOrderRequest<T> countOrderItems(){
        addRelationCount("Count", Q.orderItems().unlimited(), OrderItem.CUSTOMER_ORDER_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderItems", null, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> countOrderItemsAs(String name){
        addRelationCount(name, Q.orderItems().unlimited(), OrderItem.CUSTOMER_ORDER_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderItemsAs", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> countOrderItemsWith(String name, OrderItemRequest subRequest){
        addRelationCount(name, subRequest, OrderItem.CUSTOMER_ORDER_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderItemsWith", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> minQuantityOfOrderItems(){
        addRelationScalarAggregate("minQuantityOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.CUSTOMER_ORDER_PROPERTY, OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minQuantityOfOrderItems", null, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> minQuantityOfOrderItemsAs(String name){
        return minQuantityOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public CustomerOrderRequest<T> minQuantityOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.CUSTOMER_ORDER_PROPERTY,
            OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minQuantityOfOrderItemsAs", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> maxQuantityOfOrderItems(){
        addRelationScalarAggregate("maxQuantityOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.CUSTOMER_ORDER_PROPERTY, OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxQuantityOfOrderItems", null, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> maxQuantityOfOrderItemsAs(String name){
        return maxQuantityOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public CustomerOrderRequest<T> maxQuantityOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.CUSTOMER_ORDER_PROPERTY,
            OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxQuantityOfOrderItemsAs", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> sumQuantityOfOrderItems(){
        addRelationScalarAggregate("sumQuantityOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.CUSTOMER_ORDER_PROPERTY, OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumQuantityOfOrderItems", null, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> sumQuantityOfOrderItemsAs(String name){
        return sumQuantityOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public CustomerOrderRequest<T> sumQuantityOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.CUSTOMER_ORDER_PROPERTY,
            OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumQuantityOfOrderItemsAs", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> avgQuantityOfOrderItems(){
        addRelationScalarAggregate("avgQuantityOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.CUSTOMER_ORDER_PROPERTY, OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgQuantityOfOrderItems", null, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> avgQuantityOfOrderItemsAs(String name){
        return avgQuantityOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public CustomerOrderRequest<T> avgQuantityOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.CUSTOMER_ORDER_PROPERTY,
            OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgQuantityOfOrderItemsAs", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> standardDeviationQuantityOfOrderItems(){
        addRelationScalarAggregate("stdDevQuantityOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.CUSTOMER_ORDER_PROPERTY, OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationQuantityOfOrderItems", null, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> standardDeviationQuantityOfOrderItemsAs(String name){
        return standardDeviationQuantityOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public CustomerOrderRequest<T> standardDeviationQuantityOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.CUSTOMER_ORDER_PROPERTY,
            OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationQuantityOfOrderItemsAs", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> squareRootOfPopulationStandardDeviationQuantityOfOrderItems(){
        addRelationScalarAggregate("stdDevPopQuantityOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.CUSTOMER_ORDER_PROPERTY, OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationQuantityOfOrderItems", null, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> squareRootOfPopulationStandardDeviationQuantityOfOrderItemsAs(String name){
        return squareRootOfPopulationStandardDeviationQuantityOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public CustomerOrderRequest<T> squareRootOfPopulationStandardDeviationQuantityOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.CUSTOMER_ORDER_PROPERTY,
            OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationQuantityOfOrderItemsAs", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> sampleVarianceQuantityOfOrderItems(){
        addRelationScalarAggregate("varSampQuantityOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.CUSTOMER_ORDER_PROPERTY, OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceQuantityOfOrderItems", null, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> sampleVarianceQuantityOfOrderItemsAs(String name){
        return sampleVarianceQuantityOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public CustomerOrderRequest<T> sampleVarianceQuantityOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.CUSTOMER_ORDER_PROPERTY,
            OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceQuantityOfOrderItemsAs", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> samplePopulationVarianceQuantityOfOrderItems(){
        addRelationScalarAggregate("varPopQuantityOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.CUSTOMER_ORDER_PROPERTY, OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceQuantityOfOrderItems", null, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> samplePopulationVarianceQuantityOfOrderItemsAs(String name){
        return samplePopulationVarianceQuantityOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public CustomerOrderRequest<T> samplePopulationVarianceQuantityOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.CUSTOMER_ORDER_PROPERTY,
            OrderItem.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceQuantityOfOrderItemsAs", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> minUnitPriceOfOrderItems(){
        addRelationScalarAggregate("minUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.CUSTOMER_ORDER_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minUnitPriceOfOrderItems", null, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> minUnitPriceOfOrderItemsAs(String name){
        return minUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public CustomerOrderRequest<T> minUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.CUSTOMER_ORDER_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minUnitPriceOfOrderItemsAs", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> maxUnitPriceOfOrderItems(){
        addRelationScalarAggregate("maxUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.CUSTOMER_ORDER_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxUnitPriceOfOrderItems", null, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> maxUnitPriceOfOrderItemsAs(String name){
        return maxUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public CustomerOrderRequest<T> maxUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.CUSTOMER_ORDER_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxUnitPriceOfOrderItemsAs", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> sumUnitPriceOfOrderItems(){
        addRelationScalarAggregate("sumUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.CUSTOMER_ORDER_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumUnitPriceOfOrderItems", null, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> sumUnitPriceOfOrderItemsAs(String name){
        return sumUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public CustomerOrderRequest<T> sumUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.CUSTOMER_ORDER_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumUnitPriceOfOrderItemsAs", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> avgUnitPriceOfOrderItems(){
        addRelationScalarAggregate("avgUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.CUSTOMER_ORDER_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgUnitPriceOfOrderItems", null, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> avgUnitPriceOfOrderItemsAs(String name){
        return avgUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public CustomerOrderRequest<T> avgUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.CUSTOMER_ORDER_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgUnitPriceOfOrderItemsAs", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> standardDeviationUnitPriceOfOrderItems(){
        addRelationScalarAggregate("stdDevUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.CUSTOMER_ORDER_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationUnitPriceOfOrderItems", null, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> standardDeviationUnitPriceOfOrderItemsAs(String name){
        return standardDeviationUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public CustomerOrderRequest<T> standardDeviationUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.CUSTOMER_ORDER_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationUnitPriceOfOrderItemsAs", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> squareRootOfPopulationStandardDeviationUnitPriceOfOrderItems(){
        addRelationScalarAggregate("stdDevPopUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.CUSTOMER_ORDER_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationUnitPriceOfOrderItems", null, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> squareRootOfPopulationStandardDeviationUnitPriceOfOrderItemsAs(String name){
        return squareRootOfPopulationStandardDeviationUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public CustomerOrderRequest<T> squareRootOfPopulationStandardDeviationUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.CUSTOMER_ORDER_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationUnitPriceOfOrderItemsAs", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> sampleVarianceUnitPriceOfOrderItems(){
        addRelationScalarAggregate("varSampUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.CUSTOMER_ORDER_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceUnitPriceOfOrderItems", null, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> sampleVarianceUnitPriceOfOrderItemsAs(String name){
        return sampleVarianceUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public CustomerOrderRequest<T> sampleVarianceUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.CUSTOMER_ORDER_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceUnitPriceOfOrderItemsAs", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> samplePopulationVarianceUnitPriceOfOrderItems(){
        addRelationScalarAggregate("varPopUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.CUSTOMER_ORDER_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceUnitPriceOfOrderItems", null, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> samplePopulationVarianceUnitPriceOfOrderItemsAs(String name){
        return samplePopulationVarianceUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public CustomerOrderRequest<T> samplePopulationVarianceUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.CUSTOMER_ORDER_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceUnitPriceOfOrderItemsAs", name, CustomerOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

   public CustomerOrderRequest<T> facetByCustomerAs(String facetName, CustomerRequest customer){
       return facetByCustomerAs(facetName, customer, true);
   }

   public CustomerOrderRequest<T> facetByCustomerAs(String facetName, CustomerRequest customer, boolean includeAllFacets){
       addFacet(facetName, CustomerOrder.CUSTOMER_PROPERTY, customer, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByCustomerAs", facetName, CustomerOrder.CUSTOMER_PROPERTY));
       return this;
   }
   public CustomerOrderRequest<T> facetByStatusAs(String facetName, OrderStatusRequest status){
       return facetByStatusAs(facetName, status, true);
   }

   public CustomerOrderRequest<T> facetByStatusAs(String facetName, OrderStatusRequest status, boolean includeAllFacets){
       addFacet(facetName, CustomerOrder.STATUS_PROPERTY, status, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByStatusAs", facetName, CustomerOrder.STATUS_PROPERTY));
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