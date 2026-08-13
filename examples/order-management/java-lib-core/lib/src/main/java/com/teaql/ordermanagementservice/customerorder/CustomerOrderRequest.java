
package com.teaql.ordermanagementservice.customerorder;

import com.teaql.ordermanagementservice.Q;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformRequest;
import com.teaql.ordermanagementservice.customer.Customer;
import com.teaql.ordermanagementservice.customer.CustomerRequest;
import com.teaql.ordermanagementservice.orderline.OrderLine;
import com.teaql.ordermanagementservice.orderline.OrderLineRequest;
import com.teaql.ordermanagementservice.orderstatus.OrderStatus;
import com.teaql.ordermanagementservice.orderstatus.OrderStatusRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

public class CustomerOrderRequest<T extends CustomerOrder> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    public CustomerOrderRequest(Class<T> returnType){
        super(returnType);
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
        return selectId().selectOrderNumber().selectOrderDate().selectTotalAmount().selectStatusIdOnly().selectCustomerIdOnly().selectCommercePlatformIdOnly().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public CustomerOrderRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public CustomerOrderRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectOrderNumber().selectOrderDate().selectTotalAmount().selectStatus().selectCustomer().selectCommercePlatform().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public CustomerOrderRequest<T> selectChildren(){
        super.selectAny();
        selectOrderLineList();
        return selectId().selectOrderNumber().selectOrderDate().selectTotalAmount().selectStatus().selectCustomer().selectCommercePlatform().selectCreateTime().selectUpdateTime().selectVersion();
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
    public CustomerOrderRequest<T> selectStatusIdOnly(){
       selectProperty(CustomerOrder.STATUS_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> selectStatus(){
        return selectStatusWith(Q.orderStatuses().unlimited().selectSelf());
    }

    public CustomerOrderRequest<T> selectStatusWith(OrderStatusRequest status){
       selectProperty(CustomerOrder.STATUS_PROPERTY);
       enhanceRelation(CustomerOrder.STATUS_PROPERTY, status);
       return this;
    }

    public CustomerOrderRequest<T> unselectStatus(){
       unselectProperty(CustomerOrder.STATUS_PROPERTY);
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
       enhanceRelation(CustomerOrder.CUSTOMER_PROPERTY, customer);
       return this;
    }

    public CustomerOrderRequest<T> unselectCustomer(){
       unselectProperty(CustomerOrder.CUSTOMER_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> selectCommercePlatformIdOnly(){
       selectProperty(CustomerOrder.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> selectCommercePlatform(){
        return selectCommercePlatformWith(Q.commercePlatforms().unlimited().selectSelf());
    }

    public CustomerOrderRequest<T> selectCommercePlatformWith(CommercePlatformRequest commercePlatform){
       selectProperty(CustomerOrder.COMMERCE_PLATFORM_PROPERTY);
       enhanceRelation(CustomerOrder.COMMERCE_PLATFORM_PROPERTY, commercePlatform);
       return this;
    }

    public CustomerOrderRequest<T> unselectCommercePlatform(){
       unselectProperty(CustomerOrder.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> selectCreateTime(){
       selectProperty(CustomerOrder.CREATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the createTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  createTime) to fetch createTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerOrderRequest<T> unselectCreateTime(){
       unselectProperty(CustomerOrder.CREATE_TIME_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> selectUpdateTime(){
       selectProperty(CustomerOrder.UPDATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the updateTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  updateTime) to fetch updateTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerOrderRequest<T> unselectUpdateTime(){
       unselectProperty(CustomerOrder.UPDATE_TIME_PROPERTY);
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
    public CustomerOrderRequest<T> selectOrderLineList(){
       return selectOrderLineListWith(Q.orderLines().selectSelf());
    }

    public CustomerOrderRequest<T> selectOrderLineListWith(OrderLineRequest orderLineList){
       enhanceRelation(CustomerOrder.ORDER_LINE_LIST_PROPERTY, orderLineList);
       return this;
    }

    public CustomerOrderRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.ID_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }
    public CustomerOrderRequest<T> withIdIn(Long... id){
       return withId(Operator.EQUAL, (Object[])id);
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

    public CustomerOrderRequest<T> withOrderNumberEndingWith(String orderNumber){
       return withOrderNumber(Operator.END_WITH, orderNumber);
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

    public CustomerOrderRequest<T> filterByCommercePlatform(CommercePlatform... commercePlatform){
      if (commercePlatform == null || commercePlatform.length == 0) {
        throw new IllegalArgumentException("filterByCommercePlatform parameter commercePlatform cannot be empty");
      }
      return appendSearchCriteria(createCommercePlatformCriteria(Operator.EQUAL, (Object[])commercePlatform));
    }

    public CustomerOrderRequest<T> withCommercePlatform(Operator operator, Object... values){
       return appendSearchCriteria(createCommercePlatformCriteria(operator, values));
    }

    public CustomerOrderRequest<T> withCommercePlatformIsUnknown(){
       return withCommercePlatform(Operator.IS_NULL);
    }

    public CustomerOrderRequest<T> withCommercePlatformIsKnown(){
       return withCommercePlatform(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCommercePlatformCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.COMMERCE_PLATFORM_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> filterByCommercePlatform(Long commercePlatform){
      if(commercePlatform == null){
         return this;
      }
      return withCommercePlatform(Operator.EQUAL, commercePlatform);
    }
    public CustomerOrderRequest<T> withCommercePlatformMatching(CommercePlatformRequest commercePlatform){
       return appendSearchCriteria(new SubQuerySearchCriteria(CustomerOrder.COMMERCE_PLATFORM_PROPERTY, commercePlatform, CommercePlatform.ID_PROPERTY));
    }

    public CustomerOrderRequest<T> filterByCreateTime(LocalDateTime... createTime){
      if (createTime == null || createTime.length == 0) {
        throw new IllegalArgumentException("filterByCreateTime parameter createTime cannot be empty");
      }
      return appendSearchCriteria(createCreateTimeCriteria(Operator.EQUAL, (Object[])createTime));
    }

    public CustomerOrderRequest<T> withCreateTime(Operator operator, Object... values){
       return appendSearchCriteria(createCreateTimeCriteria(operator, values));
    }

    public CustomerOrderRequest<T> withCreateTimeIsUnknown(){
       return withCreateTime(Operator.IS_NULL);
    }

    public CustomerOrderRequest<T> withCreateTimeIsKnown(){
       return withCreateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCreateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.CREATE_TIME_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> withCreateTimeGreaterThan(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public CustomerOrderRequest<T> withCreateTimeGreaterThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN_OR_EQUAL, createTime);
    }

    public CustomerOrderRequest<T> withCreateTimeLessThan(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public CustomerOrderRequest<T> withCreateTimeLessThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN_OR_EQUAL, createTime);
    }

    public CustomerOrderRequest<T> withCreateTimeBetween(LocalDateTime startOfCreateTime, LocalDateTime endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }
    public CustomerOrderRequest<T> withCreateTimeBefore(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public CustomerOrderRequest<T> withCreateTimeBefore(Date createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public CustomerOrderRequest<T> withCreateTimeAfter(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public CustomerOrderRequest<T> withCreateTimeAfter(Date createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public CustomerOrderRequest<T> withCreateTimeBetween(Date startOfCreateTime, Date endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }




    public CustomerOrderRequest<T> filterByUpdateTime(LocalDateTime... updateTime){
      if (updateTime == null || updateTime.length == 0) {
        throw new IllegalArgumentException("filterByUpdateTime parameter updateTime cannot be empty");
      }
      return appendSearchCriteria(createUpdateTimeCriteria(Operator.EQUAL, (Object[])updateTime));
    }

    public CustomerOrderRequest<T> withUpdateTime(Operator operator, Object... values){
       return appendSearchCriteria(createUpdateTimeCriteria(operator, values));
    }

    public CustomerOrderRequest<T> withUpdateTimeIsUnknown(){
       return withUpdateTime(Operator.IS_NULL);
    }

    public CustomerOrderRequest<T> withUpdateTimeIsKnown(){
       return withUpdateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createUpdateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.UPDATE_TIME_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> withUpdateTimeGreaterThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public CustomerOrderRequest<T> withUpdateTimeGreaterThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN_OR_EQUAL, updateTime);
    }

    public CustomerOrderRequest<T> withUpdateTimeLessThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public CustomerOrderRequest<T> withUpdateTimeLessThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN_OR_EQUAL, updateTime);
    }

    public CustomerOrderRequest<T> withUpdateTimeBetween(LocalDateTime startOfUpdateTime, LocalDateTime endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
    }
    public CustomerOrderRequest<T> withUpdateTimeBefore(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public CustomerOrderRequest<T> withUpdateTimeBefore(Date updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public CustomerOrderRequest<T> withUpdateTimeAfter(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public CustomerOrderRequest<T> withUpdateTimeAfter(Date updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public CustomerOrderRequest<T> withUpdateTimeBetween(Date startOfUpdateTime, Date endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
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

    public CustomerOrderRequest<T> withOrderLineListMatching(OrderLineRequest orderLineRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(CustomerOrder.ID_PROPERTY, orderLineRequest, OrderLine.CUSTOMER_ORDER_PROPERTY));
    }

    public CustomerOrderRequest<T> withoutOrderLineListMatching(OrderLineRequest orderLineRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(CustomerOrder.ID_PROPERTY, orderLineRequest, OrderLine.CUSTOMER_ORDER_PROPERTY)));
    }

    public CustomerOrderRequest<T> haveOrderLines(){
        return withOrderLineListMatching(Q.orderLines().unlimited());
    }

    public CustomerOrderRequest<T> haveNoOrderLines(){
        return withoutOrderLineListMatching(Q.orderLines().unlimited());
    }

    public CustomerOrderRequest<T> count(){
        super.count();
        return this;
    }
    public CustomerOrderRequest<T> countAs(String retName){
        super.count(retName);
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
    public CustomerOrderRequest<T> groupByStatusWithDetails(){
       return groupByStatusWithDetails(Q.orderStatuses().unlimited());
    }

    public CustomerOrderRequest<T> groupByStatusWithDetails(OrderStatusRequest subRequest){
       aggregate(CustomerOrder.STATUS_PROPERTY, subRequest);
       return this;
    }

    public CustomerOrderRequest<T> groupByCustomerWithDetails(){
       return groupByCustomerWithDetails(Q.customers().unlimited());
    }

    public CustomerOrderRequest<T> groupByCustomerWithDetails(CustomerRequest subRequest){
       aggregate(CustomerOrder.CUSTOMER_PROPERTY, subRequest);
       return this;
    }

    public CustomerOrderRequest<T> groupByCommercePlatformWithDetails(){
       return groupByCommercePlatformWithDetails(Q.commercePlatforms().unlimited());
    }

    public CustomerOrderRequest<T> groupByCommercePlatformWithDetails(CommercePlatformRequest subRequest){
       aggregate(CustomerOrder.COMMERCE_PLATFORM_PROPERTY, subRequest);
       return this;
    }




    public CustomerOrderRequest<T> groupByOrderLinesWithDetails(OrderLineRequest subRequest){
       aggregate(CustomerOrder.ORDER_LINE_LIST_PROPERTY, subRequest);
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
    public CustomerOrderRequest<T> groupByCommercePlatformWith(CommercePlatformRequest subRequest){
       groupBy(CustomerOrder.COMMERCE_PLATFORM_PROPERTY, subRequest);
       return this;
    }
    public CustomerOrderRequest<T> groupByCommercePlatform(){
       groupBy(CustomerOrder.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByCommercePlatformAs(String retName){
       groupBy(retName, CustomerOrder.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByCommercePlatformWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.COMMERCE_PLATFORM_PROPERTY, function);
       return this;
    }

    public CustomerOrderRequest<T> groupByCreateTime(){
       groupBy(CustomerOrder.CREATE_TIME_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByCreateTimeAs(String retName){
       groupBy(retName, CustomerOrder.CREATE_TIME_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByCreateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.CREATE_TIME_PROPERTY, function);
       return this;
    }

    public CustomerOrderRequest<T> groupByUpdateTime(){
       groupBy(CustomerOrder.UPDATE_TIME_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByUpdateTimeAs(String retName){
       groupBy(retName, CustomerOrder.UPDATE_TIME_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByUpdateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.UPDATE_TIME_PROPERTY, function);
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

    public CustomerOrderRequest<T> withStatusIsPending(){
       filterByStatus(com.teaql.ordermanagementservice.Constants.ORDER_STATUS_PENDING);
       return this;
    }


    public CustomerOrderRequest<T> withStatusIsProcessing(){
       filterByStatus(com.teaql.ordermanagementservice.Constants.ORDER_STATUS_PROCESSING);
       return this;
    }


    public CustomerOrderRequest<T> withStatusIsShipped(){
       filterByStatus(com.teaql.ordermanagementservice.Constants.ORDER_STATUS_SHIPPED);
       return this;
    }


    public CustomerOrderRequest<T> withStatusIsCompleted(){
       filterByStatus(com.teaql.ordermanagementservice.Constants.ORDER_STATUS_COMPLETED);
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

    public CustomerOrderRequest<T> orderByStatusAscending(){
       addOrderByAscending(CustomerOrder.STATUS_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByStatusDescending(){
       addOrderByDescending(CustomerOrder.STATUS_PROPERTY);
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

    public CustomerOrderRequest<T> orderByCommercePlatformAscending(){
       addOrderByAscending(CustomerOrder.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByCommercePlatformDescending(){
       addOrderByDescending(CustomerOrder.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByCreateTimeAscending(){
       addOrderByAscending(CustomerOrder.CREATE_TIME_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByCreateTimeDescending(){
       addOrderByDescending(CustomerOrder.CREATE_TIME_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByUpdateTimeAscending(){
       addOrderByAscending(CustomerOrder.UPDATE_TIME_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByUpdateTimeDescending(){
       addOrderByDescending(CustomerOrder.UPDATE_TIME_PROPERTY);
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


    public CustomerOrderRequest<T> statsFromOrderLinesAs(String name, OrderLineRequest subRequest){
       return statsFromOrderLinesAs(name, subRequest, false);
    }

    public CustomerOrderRequest<T> statsFromOrderLinesAs(String name, OrderLineRequest subRequest, boolean singleResult){
       subRequest.setPartitionProperty(OrderLine.CUSTOMER_ORDER_PROPERTY);
       addAggregateDynamicProperty(name, subRequest, singleResult);
       return this;
    }

    public CustomerOrderRequest<T> statsFromOrderLines(OrderLineRequest subRequest){
       return statsFromOrderLinesAs(REFINEMENTS, subRequest);
    }
    public OrderStatusRequest rollUpToStatus(){
       OrderStatusRequest status = Q.orderStatuses().unlimited();
       this.withStatusMatching(status)
           .groupByStatusWith(status);
       return status;
    }

    public CustomerRequest rollUpToCustomer(){
       CustomerRequest customer = Q.customers().unlimited();
       this.withCustomerMatching(customer)
           .groupByCustomerWith(customer);
       return customer;
    }

    public CommercePlatformRequest rollUpToCommercePlatform(){
       CommercePlatformRequest commercePlatform = Q.commercePlatforms().unlimited();
       this.withCommercePlatformMatching(commercePlatform)
           .groupByCommercePlatformWith(commercePlatform);
       return commercePlatform;
    }




    public CustomerOrderRequest<T> countOrderLines(){
        return countOrderLinesAs("Count");
    }

    public CustomerOrderRequest<T> countOrderLinesAs(String name){
        return countOrderLinesWith(name, Q.orderLines().unlimited());
    }

    public CustomerOrderRequest<T> countOrderLinesWith(String name, OrderLineRequest subRequest){
        return statsFromOrderLinesAs(name, subRequest.count(), true);
    }
    public CustomerOrderRequest<T> minQuantityOfOrderLines(){
        return minQuantityOfOrderLinesAs("minQuantityOfOrderLines");
    }

    public CustomerOrderRequest<T> minQuantityOfOrderLinesAs(String name){
        return minQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public CustomerOrderRequest<T> minQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        return statsFromOrderLinesAs(name, subRequest.minQuantity(), true);
    }
    public CustomerOrderRequest<T> maxQuantityOfOrderLines(){
        return maxQuantityOfOrderLinesAs("maxQuantityOfOrderLines");
    }

    public CustomerOrderRequest<T> maxQuantityOfOrderLinesAs(String name){
        return maxQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public CustomerOrderRequest<T> maxQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        return statsFromOrderLinesAs(name, subRequest.maxQuantity(), true);
    }
    public CustomerOrderRequest<T> sumQuantityOfOrderLines(){
        return sumQuantityOfOrderLinesAs("sumQuantityOfOrderLines");
    }

    public CustomerOrderRequest<T> sumQuantityOfOrderLinesAs(String name){
        return sumQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public CustomerOrderRequest<T> sumQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        return statsFromOrderLinesAs(name, subRequest.sumQuantity(), true);
    }
    public CustomerOrderRequest<T> avgQuantityOfOrderLines(){
        return avgQuantityOfOrderLinesAs("avgQuantityOfOrderLines");
    }

    public CustomerOrderRequest<T> avgQuantityOfOrderLinesAs(String name){
        return avgQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public CustomerOrderRequest<T> avgQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        return statsFromOrderLinesAs(name, subRequest.avgQuantity(), true);
    }
    public CustomerOrderRequest<T> standardDeviationQuantityOfOrderLines(){
        return standardDeviationQuantityOfOrderLinesAs("stdDevQuantityOfOrderLines");
    }

    public CustomerOrderRequest<T> standardDeviationQuantityOfOrderLinesAs(String name){
        return standardDeviationQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public CustomerOrderRequest<T> standardDeviationQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        return statsFromOrderLinesAs(name, subRequest.standardDeviationQuantity(), true);
    }
    public CustomerOrderRequest<T> squareRootOfPopulationStandardDeviationQuantityOfOrderLines(){
        return squareRootOfPopulationStandardDeviationQuantityOfOrderLinesAs("stdDevPopQuantityOfOrderLines");
    }

    public CustomerOrderRequest<T> squareRootOfPopulationStandardDeviationQuantityOfOrderLinesAs(String name){
        return squareRootOfPopulationStandardDeviationQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public CustomerOrderRequest<T> squareRootOfPopulationStandardDeviationQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        return statsFromOrderLinesAs(name, subRequest.squareRootOfPopulationStandardDeviationQuantity(), true);
    }
    public CustomerOrderRequest<T> sampleVarianceQuantityOfOrderLines(){
        return sampleVarianceQuantityOfOrderLinesAs("varSampQuantityOfOrderLines");
    }

    public CustomerOrderRequest<T> sampleVarianceQuantityOfOrderLinesAs(String name){
        return sampleVarianceQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public CustomerOrderRequest<T> sampleVarianceQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        return statsFromOrderLinesAs(name, subRequest.sampleVarianceQuantity(), true);
    }
    public CustomerOrderRequest<T> samplePopulationVarianceQuantityOfOrderLines(){
        return samplePopulationVarianceQuantityOfOrderLinesAs("varPopQuantityOfOrderLines");
    }

    public CustomerOrderRequest<T> samplePopulationVarianceQuantityOfOrderLinesAs(String name){
        return samplePopulationVarianceQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public CustomerOrderRequest<T> samplePopulationVarianceQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        return statsFromOrderLinesAs(name, subRequest.samplePopulationVarianceQuantity(), true);
    }

   public CustomerOrderRequest<T> facetByStatusAs(String facetName, OrderStatusRequest status){
       return facetByStatusAs(facetName, status, true);
   }

   public CustomerOrderRequest<T> facetByStatusAs(String facetName, OrderStatusRequest status, boolean includeAllFacets){
       addFacet(facetName, CustomerOrder.STATUS_PROPERTY, status, includeAllFacets);
       return this;
   }
   public CustomerOrderRequest<T> facetByCustomerAs(String facetName, CustomerRequest customer){
       return facetByCustomerAs(facetName, customer, true);
   }

   public CustomerOrderRequest<T> facetByCustomerAs(String facetName, CustomerRequest customer, boolean includeAllFacets){
       addFacet(facetName, CustomerOrder.CUSTOMER_PROPERTY, customer, includeAllFacets);
       return this;
   }
   public CustomerOrderRequest<T> facetByCommercePlatformAs(String facetName, CommercePlatformRequest commercePlatform){
       return facetByCommercePlatformAs(facetName, commercePlatform, true);
   }

   public CustomerOrderRequest<T> facetByCommercePlatformAs(String facetName, CommercePlatformRequest commercePlatform, boolean includeAllFacets){
       addFacet(facetName, CustomerOrder.COMMERCE_PLATFORM_PROPERTY, commercePlatform, includeAllFacets);
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