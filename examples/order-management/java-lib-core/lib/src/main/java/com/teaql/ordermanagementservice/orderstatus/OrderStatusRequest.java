
package com.teaql.ordermanagementservice.orderstatus;

import com.teaql.ordermanagementservice.Q;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformRequest;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.customerorder.CustomerOrderRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.math.BigDecimal;

public class OrderStatusRequest<T extends OrderStatus> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    public OrderStatusRequest(Class<T> returnType){
        super(returnType);
        selectId();
        selectVersion();
    }

    public OrderStatusRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public OrderStatusRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public OrderStatusRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public OrderStatusRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public OrderStatusRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    public OrderStatusRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (OrderStatusRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public OrderStatusRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public OrderStatusRequest<T> matchingAnyOf(OrderStatusRequest orderStatus){
        super.internalMatchAny(orderStatus);
        return this;
    }

    public OrderStatusRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public OrderStatusRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public OrderStatusRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public OrderStatusRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectName().selectCode().selectColor().selectDisplayOrder().selectCommercePlatformIdOnly().selectVersion();
    }

    public OrderStatusRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public OrderStatusRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectName().selectCode().selectColor().selectDisplayOrder().selectCommercePlatform().selectVersion();
    }

    public OrderStatusRequest<T> selectChildren(){
        super.selectAny();
        selectCustomerOrderList();
        return selectId().selectName().selectCode().selectColor().selectDisplayOrder().selectCommercePlatform().selectVersion();
    }


    public OrderStatusRequest<T> selectId(){
       selectProperty(OrderStatus.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderStatusRequest<T> unselectId(){
       unselectProperty(OrderStatus.ID_PROPERTY);
       return this;
    }
    public OrderStatusRequest<T> selectName(){
       selectProperty(OrderStatus.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderStatusRequest<T> unselectName(){
       unselectProperty(OrderStatus.NAME_PROPERTY);
       return this;
    }
    public OrderStatusRequest<T> selectCode(){
       selectProperty(OrderStatus.CODE_PROPERTY);
       return this;
    }

    /**
     * fill the code with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  code) to fetch code property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderStatusRequest<T> unselectCode(){
       unselectProperty(OrderStatus.CODE_PROPERTY);
       return this;
    }
    public OrderStatusRequest<T> selectColor(){
       selectProperty(OrderStatus.COLOR_PROPERTY);
       return this;
    }

    /**
     * fill the color with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  color) to fetch color property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderStatusRequest<T> unselectColor(){
       unselectProperty(OrderStatus.COLOR_PROPERTY);
       return this;
    }
    public OrderStatusRequest<T> selectDisplayOrder(){
       selectProperty(OrderStatus.DISPLAY_ORDER_PROPERTY);
       return this;
    }

    /**
     * fill the displayOrder with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  displayOrder) to fetch displayOrder property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the displayOrder with customized aggrFunction, TEAQL uses ({aggrFunction}(displayOrder) AS displayOrder to fetch displayOrder property.
     * @param aggrFunction  aggrFunction
     */
    public OrderStatusRequest<T> selectDisplayOrder(AggrFunction aggrFunction){
       selectProperty(OrderStatus.DISPLAY_ORDER_PROPERTY, aggrFunction);
       return this;
    }


    public OrderStatusRequest<T> unselectDisplayOrder(){
       unselectProperty(OrderStatus.DISPLAY_ORDER_PROPERTY);
       return this;
    }
    public OrderStatusRequest<T> selectCommercePlatformIdOnly(){
       selectProperty(OrderStatus.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> selectCommercePlatform(){
        return selectCommercePlatformWith(Q.commercePlatforms().unlimited().selectSelf());
    }

    public OrderStatusRequest<T> selectCommercePlatformWith(CommercePlatformRequest commercePlatform){
       selectProperty(OrderStatus.COMMERCE_PLATFORM_PROPERTY);
       enhanceRelation(OrderStatus.COMMERCE_PLATFORM_PROPERTY, commercePlatform);
       return this;
    }

    public OrderStatusRequest<T> unselectCommercePlatform(){
       unselectProperty(OrderStatus.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }
    public OrderStatusRequest<T> selectVersion(){
       selectProperty(OrderStatus.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderStatusRequest<T> unselectVersion(){
       unselectProperty(OrderStatus.VERSION_PROPERTY);
       return this;
    }
    public OrderStatusRequest<T> selectCustomerOrderList(){
       return selectCustomerOrderListWith(Q.customerOrders().selectSelf());
    }

    public OrderStatusRequest<T> selectCustomerOrderListWith(CustomerOrderRequest customerOrderList){
       enhanceRelation(OrderStatus.CUSTOMER_ORDER_LIST_PROPERTY, customerOrderList);
       return this;
    }

    public OrderStatusRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderStatus.ID_PROPERTY, operator, values);
    }

    public OrderStatusRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }
    public OrderStatusRequest<T> withIdIn(Long... id){
       return withId(Operator.EQUAL, (Object[])id);
    }



    public OrderStatusRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public OrderStatusRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public OrderStatusRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public OrderStatusRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderStatus.NAME_PROPERTY, operator, values);
    }

    public OrderStatusRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public OrderStatusRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public OrderStatusRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public OrderStatusRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public OrderStatusRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public OrderStatusRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public OrderStatusRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public OrderStatusRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public OrderStatusRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public OrderStatusRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public OrderStatusRequest<T> filterByCode(String... code){
      if (code == null || code.length == 0) {
        throw new IllegalArgumentException("filterByCode parameter code cannot be empty");
      }
      return appendSearchCriteria(createCodeCriteria(Operator.EQUAL, (Object[])code));
    }

    public OrderStatusRequest<T> withCode(Operator operator, Object... values){
       return appendSearchCriteria(createCodeCriteria(operator, values));
    }

    public OrderStatusRequest<T> withCodeIsUnknown(){
       return withCode(Operator.IS_NULL);
    }

    public OrderStatusRequest<T> withCodeIsKnown(){
       return withCode(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCodeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderStatus.CODE_PROPERTY, operator, values);
    }

    public OrderStatusRequest<T> withCodeGreaterThan(String code){
       return withCode(Operator.GREATER_THAN, code);
    }

    public OrderStatusRequest<T> withCodeGreaterThanOrEqualTo(String code){
       return withCode(Operator.GREATER_THAN_OR_EQUAL, code);
    }

    public OrderStatusRequest<T> withCodeLessThan(String code){
       return withCode(Operator.LESS_THAN, code);
    }

    public OrderStatusRequest<T> withCodeLessThanOrEqualTo(String code){
       return withCode(Operator.LESS_THAN_OR_EQUAL, code);
    }

    public OrderStatusRequest<T> withCodeBetween(String startOfCode, String endOfCode){
       return withCode(Operator.BETWEEN, startOfCode, endOfCode);
    }
    public OrderStatusRequest<T> withCodeStartingWith(String code){
       return withCode(Operator.BEGIN_WITH, code);
    }
    public OrderStatusRequest<T> withCodeContaining(String code){
       return withCode(Operator.CONTAIN, code);
    }

    public OrderStatusRequest<T> withCodeEndingWith(String code){
       return withCode(Operator.END_WITH, code);
    }

    public OrderStatusRequest<T> withCodeIs(String code){
       return withCode(Operator.EQUAL, code);
    }

    public OrderStatusRequest<T> withCodeSoundingLike(String code){
       return withCode(Operator.SOUNDS_LIKE, code);
    }



    public OrderStatusRequest<T> filterByColor(String... color){
      if (color == null || color.length == 0) {
        throw new IllegalArgumentException("filterByColor parameter color cannot be empty");
      }
      return appendSearchCriteria(createColorCriteria(Operator.EQUAL, (Object[])color));
    }

    public OrderStatusRequest<T> withColor(Operator operator, Object... values){
       return appendSearchCriteria(createColorCriteria(operator, values));
    }

    public OrderStatusRequest<T> withColorIsUnknown(){
       return withColor(Operator.IS_NULL);
    }

    public OrderStatusRequest<T> withColorIsKnown(){
       return withColor(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createColorCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderStatus.COLOR_PROPERTY, operator, values);
    }

    public OrderStatusRequest<T> withColorGreaterThan(String color){
       return withColor(Operator.GREATER_THAN, color);
    }

    public OrderStatusRequest<T> withColorGreaterThanOrEqualTo(String color){
       return withColor(Operator.GREATER_THAN_OR_EQUAL, color);
    }

    public OrderStatusRequest<T> withColorLessThan(String color){
       return withColor(Operator.LESS_THAN, color);
    }

    public OrderStatusRequest<T> withColorLessThanOrEqualTo(String color){
       return withColor(Operator.LESS_THAN_OR_EQUAL, color);
    }

    public OrderStatusRequest<T> withColorBetween(String startOfColor, String endOfColor){
       return withColor(Operator.BETWEEN, startOfColor, endOfColor);
    }
    public OrderStatusRequest<T> withColorStartingWith(String color){
       return withColor(Operator.BEGIN_WITH, color);
    }
    public OrderStatusRequest<T> withColorContaining(String color){
       return withColor(Operator.CONTAIN, color);
    }

    public OrderStatusRequest<T> withColorEndingWith(String color){
       return withColor(Operator.END_WITH, color);
    }

    public OrderStatusRequest<T> withColorIs(String color){
       return withColor(Operator.EQUAL, color);
    }

    public OrderStatusRequest<T> withColorSoundingLike(String color){
       return withColor(Operator.SOUNDS_LIKE, color);
    }



    public OrderStatusRequest<T> filterByDisplayOrder(BigDecimal... displayOrder){
      if (displayOrder == null || displayOrder.length == 0) {
        throw new IllegalArgumentException("filterByDisplayOrder parameter displayOrder cannot be empty");
      }
      return appendSearchCriteria(createDisplayOrderCriteria(Operator.EQUAL, (Object[])displayOrder));
    }

    public OrderStatusRequest<T> withDisplayOrder(Operator operator, Object... values){
       return appendSearchCriteria(createDisplayOrderCriteria(operator, values));
    }

    public OrderStatusRequest<T> withDisplayOrderIsUnknown(){
       return withDisplayOrder(Operator.IS_NULL);
    }

    public OrderStatusRequest<T> withDisplayOrderIsKnown(){
       return withDisplayOrder(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createDisplayOrderCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderStatus.DISPLAY_ORDER_PROPERTY, operator, values);
    }

    public OrderStatusRequest<T> withDisplayOrderGreaterThan(BigDecimal displayOrder){
       return withDisplayOrder(Operator.GREATER_THAN, displayOrder);
    }

    public OrderStatusRequest<T> withDisplayOrderGreaterThanOrEqualTo(BigDecimal displayOrder){
       return withDisplayOrder(Operator.GREATER_THAN_OR_EQUAL, displayOrder);
    }

    public OrderStatusRequest<T> withDisplayOrderLessThan(BigDecimal displayOrder){
       return withDisplayOrder(Operator.LESS_THAN, displayOrder);
    }

    public OrderStatusRequest<T> withDisplayOrderLessThanOrEqualTo(BigDecimal displayOrder){
       return withDisplayOrder(Operator.LESS_THAN_OR_EQUAL, displayOrder);
    }

    public OrderStatusRequest<T> withDisplayOrderBetween(BigDecimal startOfDisplayOrder, BigDecimal endOfDisplayOrder){
       return withDisplayOrder(Operator.BETWEEN, startOfDisplayOrder, endOfDisplayOrder);
    }



    public OrderStatusRequest<T> filterByCommercePlatform(CommercePlatform... commercePlatform){
      if (commercePlatform == null || commercePlatform.length == 0) {
        throw new IllegalArgumentException("filterByCommercePlatform parameter commercePlatform cannot be empty");
      }
      return appendSearchCriteria(createCommercePlatformCriteria(Operator.EQUAL, (Object[])commercePlatform));
    }

    public OrderStatusRequest<T> withCommercePlatform(Operator operator, Object... values){
       return appendSearchCriteria(createCommercePlatformCriteria(operator, values));
    }

    public OrderStatusRequest<T> withCommercePlatformIsUnknown(){
       return withCommercePlatform(Operator.IS_NULL);
    }

    public OrderStatusRequest<T> withCommercePlatformIsKnown(){
       return withCommercePlatform(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCommercePlatformCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderStatus.COMMERCE_PLATFORM_PROPERTY, operator, values);
    }

    public OrderStatusRequest<T> filterByCommercePlatform(Long commercePlatform){
      if(commercePlatform == null){
         return this;
      }
      return withCommercePlatform(Operator.EQUAL, commercePlatform);
    }
    public OrderStatusRequest<T> withCommercePlatformMatching(CommercePlatformRequest commercePlatform){
       return appendSearchCriteria(new SubQuerySearchCriteria(OrderStatus.COMMERCE_PLATFORM_PROPERTY, commercePlatform, CommercePlatform.ID_PROPERTY));
    }

    public OrderStatusRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public OrderStatusRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public OrderStatusRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public OrderStatusRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderStatus.VERSION_PROPERTY, operator, values);
    }

    public OrderStatusRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public OrderStatusRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public OrderStatusRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public OrderStatusRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public OrderStatusRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }

    public OrderStatusRequest<T> withCustomerOrderListMatching(CustomerOrderRequest customerOrderRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(OrderStatus.ID_PROPERTY, customerOrderRequest, CustomerOrder.STATUS_PROPERTY));
    }

    public OrderStatusRequest<T> withoutCustomerOrderListMatching(CustomerOrderRequest customerOrderRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(OrderStatus.ID_PROPERTY, customerOrderRequest, CustomerOrder.STATUS_PROPERTY)));
    }

    public OrderStatusRequest<T> haveCustomerOrders(){
        return withCustomerOrderListMatching(Q.customerOrders().unlimited());
    }

    public OrderStatusRequest<T> haveNoCustomerOrders(){
        return withoutCustomerOrderListMatching(Q.customerOrders().unlimited());
    }

    public OrderStatusRequest<T> count(){
        super.count();
        return this;
    }
    public OrderStatusRequest<T> countAs(String retName){
        super.count(retName);
        return this;
    }
    public OrderStatusRequest minDisplayOrder(){
        return minDisplayOrderAs(prefix("minOf",OrderStatus.DISPLAY_ORDER_PROPERTY));
    }

    public OrderStatusRequest minDisplayOrderAs(String retName){
        super.min(retName, OrderStatus.DISPLAY_ORDER_PROPERTY);
        return this;
    }
    public OrderStatusRequest maxDisplayOrder(){
        return maxDisplayOrderAs(prefix("maxOf",OrderStatus.DISPLAY_ORDER_PROPERTY));
    }

    public OrderStatusRequest maxDisplayOrderAs(String retName){
        super.max(retName, OrderStatus.DISPLAY_ORDER_PROPERTY);
        return this;
    }
    public OrderStatusRequest sumDisplayOrder(){
        return sumDisplayOrderAs(prefix("sumOf",OrderStatus.DISPLAY_ORDER_PROPERTY));
    }

    public OrderStatusRequest sumDisplayOrderAs(String retName){
        super.sum(retName, OrderStatus.DISPLAY_ORDER_PROPERTY);
        return this;
    }
    public OrderStatusRequest avgDisplayOrder(){
        return avgDisplayOrderAs(prefix("avgOf",OrderStatus.DISPLAY_ORDER_PROPERTY));
    }

    public OrderStatusRequest avgDisplayOrderAs(String retName){
        super.avg(retName, OrderStatus.DISPLAY_ORDER_PROPERTY);
        return this;
    }
    public OrderStatusRequest standardDeviationDisplayOrder(){
        return standardDeviationDisplayOrderAs(prefix("standardDeviationOf",OrderStatus.DISPLAY_ORDER_PROPERTY));
    }

    public OrderStatusRequest standardDeviationDisplayOrderAs(String retName){
        super.standardDeviation(retName, OrderStatus.DISPLAY_ORDER_PROPERTY);
        return this;
    }
    public OrderStatusRequest squareRootOfPopulationStandardDeviationDisplayOrder(){
        return squareRootOfPopulationStandardDeviationDisplayOrderAs(prefix("squareRootOfPopulationStandardDeviationOf",OrderStatus.DISPLAY_ORDER_PROPERTY));
    }

    public OrderStatusRequest squareRootOfPopulationStandardDeviationDisplayOrderAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, OrderStatus.DISPLAY_ORDER_PROPERTY);
        return this;
    }
    public OrderStatusRequest sampleVarianceDisplayOrder(){
        return sampleVarianceDisplayOrderAs(prefix("sampleVarianceOf",OrderStatus.DISPLAY_ORDER_PROPERTY));
    }

    public OrderStatusRequest sampleVarianceDisplayOrderAs(String retName){
        super.sampleVariance(retName, OrderStatus.DISPLAY_ORDER_PROPERTY);
        return this;
    }
    public OrderStatusRequest samplePopulationVarianceDisplayOrder(){
        return samplePopulationVarianceDisplayOrderAs(prefix("samplePopulationVarianceOf",OrderStatus.DISPLAY_ORDER_PROPERTY));
    }

    public OrderStatusRequest samplePopulationVarianceDisplayOrderAs(String retName){
        super.samplePopulationVariance(retName, OrderStatus.DISPLAY_ORDER_PROPERTY);
        return this;
    }
    public OrderStatusRequest<T> groupByCommercePlatformWithDetails(){
       return groupByCommercePlatformWithDetails(Q.commercePlatforms().unlimited());
    }

    public OrderStatusRequest<T> groupByCommercePlatformWithDetails(CommercePlatformRequest subRequest){
       aggregate(OrderStatus.COMMERCE_PLATFORM_PROPERTY, subRequest);
       return this;
    }


    public OrderStatusRequest<T> groupByCustomerOrdersWithDetails(CustomerOrderRequest subRequest){
       aggregate(OrderStatus.CUSTOMER_ORDER_LIST_PROPERTY, subRequest);
       return this;
    }

    public OrderStatusRequest<T> groupById(){
       groupBy(OrderStatus.ID_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> groupByIdAs(String retName){
       groupBy(retName, OrderStatus.ID_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderStatus.ID_PROPERTY, function);
       return this;
    }

    public OrderStatusRequest<T> groupByName(){
       groupBy(OrderStatus.NAME_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> groupByNameAs(String retName){
       groupBy(retName, OrderStatus.NAME_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderStatus.NAME_PROPERTY, function);
       return this;
    }

    public OrderStatusRequest<T> groupByCode(){
       groupBy(OrderStatus.CODE_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> groupByCodeAs(String retName){
       groupBy(retName, OrderStatus.CODE_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> groupByCodeWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderStatus.CODE_PROPERTY, function);
       return this;
    }

    public OrderStatusRequest<T> groupByColor(){
       groupBy(OrderStatus.COLOR_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> groupByColorAs(String retName){
       groupBy(retName, OrderStatus.COLOR_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> groupByColorWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderStatus.COLOR_PROPERTY, function);
       return this;
    }

    public OrderStatusRequest<T> groupByDisplayOrder(){
       groupBy(OrderStatus.DISPLAY_ORDER_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> groupByDisplayOrderAs(String retName){
       groupBy(retName, OrderStatus.DISPLAY_ORDER_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> groupByDisplayOrderWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderStatus.DISPLAY_ORDER_PROPERTY, function);
       return this;
    }
    public OrderStatusRequest<T> groupByCommercePlatformWith(CommercePlatformRequest subRequest){
       groupBy(OrderStatus.COMMERCE_PLATFORM_PROPERTY, subRequest);
       return this;
    }
    public OrderStatusRequest<T> groupByCommercePlatform(){
       groupBy(OrderStatus.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> groupByCommercePlatformAs(String retName){
       groupBy(retName, OrderStatus.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> groupByCommercePlatformWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderStatus.COMMERCE_PLATFORM_PROPERTY, function);
       return this;
    }

    public OrderStatusRequest<T> groupByVersion(){
       groupBy(OrderStatus.VERSION_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> groupByVersionAs(String retName){
       groupBy(retName, OrderStatus.VERSION_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderStatus.VERSION_PROPERTY, function);
       return this;
    }



    public OrderStatusRequest<T> orderByIdAscending(){
       addOrderByAscending(OrderStatus.ID_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> orderByIdDescending(){
       addOrderByDescending(OrderStatus.ID_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> orderByNameAscending(){
       addOrderByAscending(OrderStatus.NAME_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> orderByNameDescending(){
       addOrderByDescending(OrderStatus.NAME_PROPERTY);
       return this;
    }
    public OrderStatusRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(OrderStatus.NAME_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(OrderStatus.NAME_PROPERTY);
       return this;
    }
    public OrderStatusRequest<T> orderByCodeAscending(){
       addOrderByAscending(OrderStatus.CODE_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> orderByCodeDescending(){
       addOrderByDescending(OrderStatus.CODE_PROPERTY);
       return this;
    }
    public OrderStatusRequest<T> orderByCodeAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(OrderStatus.CODE_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> orderByCodeDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(OrderStatus.CODE_PROPERTY);
       return this;
    }
    public OrderStatusRequest<T> orderByColorAscending(){
       addOrderByAscending(OrderStatus.COLOR_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> orderByColorDescending(){
       addOrderByDescending(OrderStatus.COLOR_PROPERTY);
       return this;
    }
    public OrderStatusRequest<T> orderByColorAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(OrderStatus.COLOR_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> orderByColorDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(OrderStatus.COLOR_PROPERTY);
       return this;
    }
    public OrderStatusRequest<T> orderByDisplayOrderAscending(){
       addOrderByAscending(OrderStatus.DISPLAY_ORDER_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> orderByDisplayOrderDescending(){
       addOrderByDescending(OrderStatus.DISPLAY_ORDER_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> orderByCommercePlatformAscending(){
       addOrderByAscending(OrderStatus.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> orderByCommercePlatformDescending(){
       addOrderByDescending(OrderStatus.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> orderByVersionAscending(){
       addOrderByAscending(OrderStatus.VERSION_PROPERTY);
       return this;
    }

    public OrderStatusRequest<T> orderByVersionDescending(){
       addOrderByDescending(OrderStatus.VERSION_PROPERTY);
       return this;
    }


    public OrderStatusRequest<T> statsFromCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
       return statsFromCustomerOrdersAs(name, subRequest, false);
    }

    public OrderStatusRequest<T> statsFromCustomerOrdersAs(String name, CustomerOrderRequest subRequest, boolean singleResult){
       subRequest.setPartitionProperty(CustomerOrder.STATUS_PROPERTY);
       addAggregateDynamicProperty(name, subRequest, singleResult);
       return this;
    }

    public OrderStatusRequest<T> statsFromCustomerOrders(CustomerOrderRequest subRequest){
       return statsFromCustomerOrdersAs(REFINEMENTS, subRequest);
    }
    public CommercePlatformRequest rollUpToCommercePlatform(){
       CommercePlatformRequest commercePlatform = Q.commercePlatforms().unlimited();
       this.withCommercePlatformMatching(commercePlatform)
           .groupByCommercePlatformWith(commercePlatform);
       return commercePlatform;
    }


    public OrderStatusRequest<T> countCustomerOrders(){
        return countCustomerOrdersAs("Count");
    }

    public OrderStatusRequest<T> countCustomerOrdersAs(String name){
        return countCustomerOrdersWith(name, Q.customerOrders().unlimited());
    }

    public OrderStatusRequest<T> countCustomerOrdersWith(String name, CustomerOrderRequest subRequest){
        return statsFromCustomerOrdersAs(name, subRequest.count(), true);
    }
    public OrderStatusRequest<T> minTotalAmountOfCustomerOrders(){
        return minTotalAmountOfCustomerOrdersAs("minTotalAmountOfCustomerOrders");
    }

    public OrderStatusRequest<T> minTotalAmountOfCustomerOrdersAs(String name){
        return minTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public OrderStatusRequest<T> minTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        return statsFromCustomerOrdersAs(name, subRequest.minTotalAmount(), true);
    }
    public OrderStatusRequest<T> maxTotalAmountOfCustomerOrders(){
        return maxTotalAmountOfCustomerOrdersAs("maxTotalAmountOfCustomerOrders");
    }

    public OrderStatusRequest<T> maxTotalAmountOfCustomerOrdersAs(String name){
        return maxTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public OrderStatusRequest<T> maxTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        return statsFromCustomerOrdersAs(name, subRequest.maxTotalAmount(), true);
    }
    public OrderStatusRequest<T> sumTotalAmountOfCustomerOrders(){
        return sumTotalAmountOfCustomerOrdersAs("sumTotalAmountOfCustomerOrders");
    }

    public OrderStatusRequest<T> sumTotalAmountOfCustomerOrdersAs(String name){
        return sumTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public OrderStatusRequest<T> sumTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        return statsFromCustomerOrdersAs(name, subRequest.sumTotalAmount(), true);
    }
    public OrderStatusRequest<T> avgTotalAmountOfCustomerOrders(){
        return avgTotalAmountOfCustomerOrdersAs("avgTotalAmountOfCustomerOrders");
    }

    public OrderStatusRequest<T> avgTotalAmountOfCustomerOrdersAs(String name){
        return avgTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public OrderStatusRequest<T> avgTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        return statsFromCustomerOrdersAs(name, subRequest.avgTotalAmount(), true);
    }
    public OrderStatusRequest<T> standardDeviationTotalAmountOfCustomerOrders(){
        return standardDeviationTotalAmountOfCustomerOrdersAs("stdDevTotalAmountOfCustomerOrders");
    }

    public OrderStatusRequest<T> standardDeviationTotalAmountOfCustomerOrdersAs(String name){
        return standardDeviationTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public OrderStatusRequest<T> standardDeviationTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        return statsFromCustomerOrdersAs(name, subRequest.standardDeviationTotalAmount(), true);
    }
    public OrderStatusRequest<T> squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrders(){
        return squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrdersAs("stdDevPopTotalAmountOfCustomerOrders");
    }

    public OrderStatusRequest<T> squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrdersAs(String name){
        return squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public OrderStatusRequest<T> squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        return statsFromCustomerOrdersAs(name, subRequest.squareRootOfPopulationStandardDeviationTotalAmount(), true);
    }
    public OrderStatusRequest<T> sampleVarianceTotalAmountOfCustomerOrders(){
        return sampleVarianceTotalAmountOfCustomerOrdersAs("varSampTotalAmountOfCustomerOrders");
    }

    public OrderStatusRequest<T> sampleVarianceTotalAmountOfCustomerOrdersAs(String name){
        return sampleVarianceTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public OrderStatusRequest<T> sampleVarianceTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        return statsFromCustomerOrdersAs(name, subRequest.sampleVarianceTotalAmount(), true);
    }
    public OrderStatusRequest<T> samplePopulationVarianceTotalAmountOfCustomerOrders(){
        return samplePopulationVarianceTotalAmountOfCustomerOrdersAs("varPopTotalAmountOfCustomerOrders");
    }

    public OrderStatusRequest<T> samplePopulationVarianceTotalAmountOfCustomerOrdersAs(String name){
        return samplePopulationVarianceTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public OrderStatusRequest<T> samplePopulationVarianceTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        return statsFromCustomerOrdersAs(name, subRequest.samplePopulationVarianceTotalAmount(), true);
    }

   public OrderStatusRequest<T> facetByCommercePlatformAs(String facetName, CommercePlatformRequest commercePlatform){
       return facetByCommercePlatformAs(facetName, commercePlatform, true);
   }

   public OrderStatusRequest<T> facetByCommercePlatformAs(String facetName, CommercePlatformRequest commercePlatform, boolean includeAllFacets){
       addFacet(facetName, OrderStatus.COMMERCE_PLATFORM_PROPERTY, commercePlatform, includeAllFacets);
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public OrderStatusRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public OrderStatusRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public OrderStatusRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public OrderStatusRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public OrderStatusRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}