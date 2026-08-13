
package com.teaql.ordermanagementservice.orderline;

import com.teaql.ordermanagementservice.Q;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformRequest;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.customerorder.CustomerOrderRequest;
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

public class OrderLineRequest<T extends OrderLine> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    public OrderLineRequest(Class<T> returnType){
        super(returnType);
        selectId();
        selectVersion();
    }

    public OrderLineRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public OrderLineRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public OrderLineRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public OrderLineRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public OrderLineRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    public OrderLineRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (OrderLineRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public OrderLineRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public OrderLineRequest<T> matchingAnyOf(OrderLineRequest orderLine){
        super.internalMatchAny(orderLine);
        return this;
    }

    public OrderLineRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public OrderLineRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public OrderLineRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public OrderLineRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectCustomerOrderIdOnly().selectProductIdOnly().selectProductName().selectSku().selectQuantity().selectCommercePlatformIdOnly().selectCreateTime().selectVersion();
    }

    public OrderLineRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public OrderLineRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectCustomerOrder().selectProduct().selectProductName().selectSku().selectQuantity().selectCommercePlatform().selectCreateTime().selectVersion();
    }

    public OrderLineRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectCustomerOrder().selectProduct().selectProductName().selectSku().selectQuantity().selectCommercePlatform().selectCreateTime().selectVersion();
    }


    public OrderLineRequest<T> selectId(){
       selectProperty(OrderLine.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderLineRequest<T> unselectId(){
       unselectProperty(OrderLine.ID_PROPERTY);
       return this;
    }
    public OrderLineRequest<T> selectCustomerOrderIdOnly(){
       selectProperty(OrderLine.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> selectCustomerOrder(){
        return selectCustomerOrderWith(Q.customerOrders().unlimited().selectSelf());
    }

    public OrderLineRequest<T> selectCustomerOrderWith(CustomerOrderRequest customerOrder){
       selectProperty(OrderLine.CUSTOMER_ORDER_PROPERTY);
       enhanceRelation(OrderLine.CUSTOMER_ORDER_PROPERTY, customerOrder);
       return this;
    }

    public OrderLineRequest<T> unselectCustomerOrder(){
       unselectProperty(OrderLine.CUSTOMER_ORDER_PROPERTY);
       return this;
    }
    public OrderLineRequest<T> selectProductIdOnly(){
       selectProperty(OrderLine.PRODUCT_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> selectProduct(){
        return selectProductWith(Q.products().unlimited().selectSelf());
    }

    public OrderLineRequest<T> selectProductWith(ProductRequest product){
       selectProperty(OrderLine.PRODUCT_PROPERTY);
       enhanceRelation(OrderLine.PRODUCT_PROPERTY, product);
       return this;
    }

    public OrderLineRequest<T> unselectProduct(){
       unselectProperty(OrderLine.PRODUCT_PROPERTY);
       return this;
    }
    public OrderLineRequest<T> selectProductName(){
       selectProperty(OrderLine.PRODUCT_NAME_PROPERTY);
       return this;
    }

    /**
     * fill the productName with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  productName) to fetch productName property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderLineRequest<T> unselectProductName(){
       unselectProperty(OrderLine.PRODUCT_NAME_PROPERTY);
       return this;
    }
    public OrderLineRequest<T> selectSku(){
       selectProperty(OrderLine.SKU_PROPERTY);
       return this;
    }

    /**
     * fill the sku with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  sku) to fetch sku property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderLineRequest<T> unselectSku(){
       unselectProperty(OrderLine.SKU_PROPERTY);
       return this;
    }
    public OrderLineRequest<T> selectQuantity(){
       selectProperty(OrderLine.QUANTITY_PROPERTY);
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
    public OrderLineRequest<T> selectQuantity(AggrFunction aggrFunction){
       selectProperty(OrderLine.QUANTITY_PROPERTY, aggrFunction);
       return this;
    }


    public OrderLineRequest<T> unselectQuantity(){
       unselectProperty(OrderLine.QUANTITY_PROPERTY);
       return this;
    }
    public OrderLineRequest<T> selectCommercePlatformIdOnly(){
       selectProperty(OrderLine.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> selectCommercePlatform(){
        return selectCommercePlatformWith(Q.commercePlatforms().unlimited().selectSelf());
    }

    public OrderLineRequest<T> selectCommercePlatformWith(CommercePlatformRequest commercePlatform){
       selectProperty(OrderLine.COMMERCE_PLATFORM_PROPERTY);
       enhanceRelation(OrderLine.COMMERCE_PLATFORM_PROPERTY, commercePlatform);
       return this;
    }

    public OrderLineRequest<T> unselectCommercePlatform(){
       unselectProperty(OrderLine.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }
    public OrderLineRequest<T> selectCreateTime(){
       selectProperty(OrderLine.CREATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the createTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  createTime) to fetch createTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderLineRequest<T> unselectCreateTime(){
       unselectProperty(OrderLine.CREATE_TIME_PROPERTY);
       return this;
    }
    public OrderLineRequest<T> selectVersion(){
       selectProperty(OrderLine.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderLineRequest<T> unselectVersion(){
       unselectProperty(OrderLine.VERSION_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderLine.ID_PROPERTY, operator, values);
    }

    public OrderLineRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }
    public OrderLineRequest<T> withIdIn(Long... id){
       return withId(Operator.EQUAL, (Object[])id);
    }



    public OrderLineRequest<T> filterByCustomerOrder(CustomerOrder... customerOrder){
      if (customerOrder == null || customerOrder.length == 0) {
        throw new IllegalArgumentException("filterByCustomerOrder parameter customerOrder cannot be empty");
      }
      return appendSearchCriteria(createCustomerOrderCriteria(Operator.EQUAL, (Object[])customerOrder));
    }

    public OrderLineRequest<T> withCustomerOrder(Operator operator, Object... values){
       return appendSearchCriteria(createCustomerOrderCriteria(operator, values));
    }

    public OrderLineRequest<T> withCustomerOrderIsUnknown(){
       return withCustomerOrder(Operator.IS_NULL);
    }

    public OrderLineRequest<T> withCustomerOrderIsKnown(){
       return withCustomerOrder(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCustomerOrderCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderLine.CUSTOMER_ORDER_PROPERTY, operator, values);
    }

    public OrderLineRequest<T> filterByCustomerOrder(Long customerOrder){
      if(customerOrder == null){
         return this;
      }
      return withCustomerOrder(Operator.EQUAL, customerOrder);
    }
    public OrderLineRequest<T> withCustomerOrderMatching(CustomerOrderRequest customerOrder){
       return appendSearchCriteria(new SubQuerySearchCriteria(OrderLine.CUSTOMER_ORDER_PROPERTY, customerOrder, CustomerOrder.ID_PROPERTY));
    }

    public OrderLineRequest<T> filterByProduct(Product... product){
      if (product == null || product.length == 0) {
        throw new IllegalArgumentException("filterByProduct parameter product cannot be empty");
      }
      return appendSearchCriteria(createProductCriteria(Operator.EQUAL, (Object[])product));
    }

    public OrderLineRequest<T> withProduct(Operator operator, Object... values){
       return appendSearchCriteria(createProductCriteria(operator, values));
    }

    public OrderLineRequest<T> withProductIsUnknown(){
       return withProduct(Operator.IS_NULL);
    }

    public OrderLineRequest<T> withProductIsKnown(){
       return withProduct(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createProductCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderLine.PRODUCT_PROPERTY, operator, values);
    }

    public OrderLineRequest<T> filterByProduct(Long product){
      if(product == null){
         return this;
      }
      return withProduct(Operator.EQUAL, product);
    }
    public OrderLineRequest<T> withProductMatching(ProductRequest product){
       return appendSearchCriteria(new SubQuerySearchCriteria(OrderLine.PRODUCT_PROPERTY, product, Product.ID_PROPERTY));
    }

    public OrderLineRequest<T> filterByProductName(String... productName){
      if (productName == null || productName.length == 0) {
        throw new IllegalArgumentException("filterByProductName parameter productName cannot be empty");
      }
      return appendSearchCriteria(createProductNameCriteria(Operator.EQUAL, (Object[])productName));
    }

    public OrderLineRequest<T> withProductName(Operator operator, Object... values){
       return appendSearchCriteria(createProductNameCriteria(operator, values));
    }

    public OrderLineRequest<T> withProductNameIsUnknown(){
       return withProductName(Operator.IS_NULL);
    }

    public OrderLineRequest<T> withProductNameIsKnown(){
       return withProductName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createProductNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderLine.PRODUCT_NAME_PROPERTY, operator, values);
    }

    public OrderLineRequest<T> withProductNameGreaterThan(String productName){
       return withProductName(Operator.GREATER_THAN, productName);
    }

    public OrderLineRequest<T> withProductNameGreaterThanOrEqualTo(String productName){
       return withProductName(Operator.GREATER_THAN_OR_EQUAL, productName);
    }

    public OrderLineRequest<T> withProductNameLessThan(String productName){
       return withProductName(Operator.LESS_THAN, productName);
    }

    public OrderLineRequest<T> withProductNameLessThanOrEqualTo(String productName){
       return withProductName(Operator.LESS_THAN_OR_EQUAL, productName);
    }

    public OrderLineRequest<T> withProductNameBetween(String startOfProductName, String endOfProductName){
       return withProductName(Operator.BETWEEN, startOfProductName, endOfProductName);
    }
    public OrderLineRequest<T> withProductNameStartingWith(String productName){
       return withProductName(Operator.BEGIN_WITH, productName);
    }
    public OrderLineRequest<T> withProductNameContaining(String productName){
       return withProductName(Operator.CONTAIN, productName);
    }

    public OrderLineRequest<T> withProductNameEndingWith(String productName){
       return withProductName(Operator.END_WITH, productName);
    }

    public OrderLineRequest<T> withProductNameIs(String productName){
       return withProductName(Operator.EQUAL, productName);
    }

    public OrderLineRequest<T> withProductNameSoundingLike(String productName){
       return withProductName(Operator.SOUNDS_LIKE, productName);
    }



    public OrderLineRequest<T> filterBySku(String... sku){
      if (sku == null || sku.length == 0) {
        throw new IllegalArgumentException("filterBySku parameter sku cannot be empty");
      }
      return appendSearchCriteria(createSkuCriteria(Operator.EQUAL, (Object[])sku));
    }

    public OrderLineRequest<T> withSku(Operator operator, Object... values){
       return appendSearchCriteria(createSkuCriteria(operator, values));
    }

    public OrderLineRequest<T> withSkuIsUnknown(){
       return withSku(Operator.IS_NULL);
    }

    public OrderLineRequest<T> withSkuIsKnown(){
       return withSku(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createSkuCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderLine.SKU_PROPERTY, operator, values);
    }

    public OrderLineRequest<T> withSkuGreaterThan(String sku){
       return withSku(Operator.GREATER_THAN, sku);
    }

    public OrderLineRequest<T> withSkuGreaterThanOrEqualTo(String sku){
       return withSku(Operator.GREATER_THAN_OR_EQUAL, sku);
    }

    public OrderLineRequest<T> withSkuLessThan(String sku){
       return withSku(Operator.LESS_THAN, sku);
    }

    public OrderLineRequest<T> withSkuLessThanOrEqualTo(String sku){
       return withSku(Operator.LESS_THAN_OR_EQUAL, sku);
    }

    public OrderLineRequest<T> withSkuBetween(String startOfSku, String endOfSku){
       return withSku(Operator.BETWEEN, startOfSku, endOfSku);
    }
    public OrderLineRequest<T> withSkuStartingWith(String sku){
       return withSku(Operator.BEGIN_WITH, sku);
    }
    public OrderLineRequest<T> withSkuContaining(String sku){
       return withSku(Operator.CONTAIN, sku);
    }

    public OrderLineRequest<T> withSkuEndingWith(String sku){
       return withSku(Operator.END_WITH, sku);
    }

    public OrderLineRequest<T> withSkuIs(String sku){
       return withSku(Operator.EQUAL, sku);
    }

    public OrderLineRequest<T> withSkuSoundingLike(String sku){
       return withSku(Operator.SOUNDS_LIKE, sku);
    }



    public OrderLineRequest<T> filterByQuantity(Integer... quantity){
      if (quantity == null || quantity.length == 0) {
        throw new IllegalArgumentException("filterByQuantity parameter quantity cannot be empty");
      }
      return appendSearchCriteria(createQuantityCriteria(Operator.EQUAL, (Object[])quantity));
    }

    public OrderLineRequest<T> withQuantity(Operator operator, Object... values){
       return appendSearchCriteria(createQuantityCriteria(operator, values));
    }

    public OrderLineRequest<T> withQuantityIsUnknown(){
       return withQuantity(Operator.IS_NULL);
    }

    public OrderLineRequest<T> withQuantityIsKnown(){
       return withQuantity(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createQuantityCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderLine.QUANTITY_PROPERTY, operator, values);
    }

    public OrderLineRequest<T> withQuantityGreaterThan(Integer quantity){
       return withQuantity(Operator.GREATER_THAN, quantity);
    }

    public OrderLineRequest<T> withQuantityGreaterThanOrEqualTo(Integer quantity){
       return withQuantity(Operator.GREATER_THAN_OR_EQUAL, quantity);
    }

    public OrderLineRequest<T> withQuantityLessThan(Integer quantity){
       return withQuantity(Operator.LESS_THAN, quantity);
    }

    public OrderLineRequest<T> withQuantityLessThanOrEqualTo(Integer quantity){
       return withQuantity(Operator.LESS_THAN_OR_EQUAL, quantity);
    }

    public OrderLineRequest<T> withQuantityBetween(Integer startOfQuantity, Integer endOfQuantity){
       return withQuantity(Operator.BETWEEN, startOfQuantity, endOfQuantity);
    }



    public OrderLineRequest<T> filterByCommercePlatform(CommercePlatform... commercePlatform){
      if (commercePlatform == null || commercePlatform.length == 0) {
        throw new IllegalArgumentException("filterByCommercePlatform parameter commercePlatform cannot be empty");
      }
      return appendSearchCriteria(createCommercePlatformCriteria(Operator.EQUAL, (Object[])commercePlatform));
    }

    public OrderLineRequest<T> withCommercePlatform(Operator operator, Object... values){
       return appendSearchCriteria(createCommercePlatformCriteria(operator, values));
    }

    public OrderLineRequest<T> withCommercePlatformIsUnknown(){
       return withCommercePlatform(Operator.IS_NULL);
    }

    public OrderLineRequest<T> withCommercePlatformIsKnown(){
       return withCommercePlatform(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCommercePlatformCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderLine.COMMERCE_PLATFORM_PROPERTY, operator, values);
    }

    public OrderLineRequest<T> filterByCommercePlatform(Long commercePlatform){
      if(commercePlatform == null){
         return this;
      }
      return withCommercePlatform(Operator.EQUAL, commercePlatform);
    }
    public OrderLineRequest<T> withCommercePlatformMatching(CommercePlatformRequest commercePlatform){
       return appendSearchCriteria(new SubQuerySearchCriteria(OrderLine.COMMERCE_PLATFORM_PROPERTY, commercePlatform, CommercePlatform.ID_PROPERTY));
    }

    public OrderLineRequest<T> filterByCreateTime(LocalDateTime... createTime){
      if (createTime == null || createTime.length == 0) {
        throw new IllegalArgumentException("filterByCreateTime parameter createTime cannot be empty");
      }
      return appendSearchCriteria(createCreateTimeCriteria(Operator.EQUAL, (Object[])createTime));
    }

    public OrderLineRequest<T> withCreateTime(Operator operator, Object... values){
       return appendSearchCriteria(createCreateTimeCriteria(operator, values));
    }

    public OrderLineRequest<T> withCreateTimeIsUnknown(){
       return withCreateTime(Operator.IS_NULL);
    }

    public OrderLineRequest<T> withCreateTimeIsKnown(){
       return withCreateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCreateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderLine.CREATE_TIME_PROPERTY, operator, values);
    }

    public OrderLineRequest<T> withCreateTimeGreaterThan(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public OrderLineRequest<T> withCreateTimeGreaterThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN_OR_EQUAL, createTime);
    }

    public OrderLineRequest<T> withCreateTimeLessThan(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public OrderLineRequest<T> withCreateTimeLessThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN_OR_EQUAL, createTime);
    }

    public OrderLineRequest<T> withCreateTimeBetween(LocalDateTime startOfCreateTime, LocalDateTime endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }
    public OrderLineRequest<T> withCreateTimeBefore(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public OrderLineRequest<T> withCreateTimeBefore(Date createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public OrderLineRequest<T> withCreateTimeAfter(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public OrderLineRequest<T> withCreateTimeAfter(Date createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public OrderLineRequest<T> withCreateTimeBetween(Date startOfCreateTime, Date endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }




    public OrderLineRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public OrderLineRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public OrderLineRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public OrderLineRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderLine.VERSION_PROPERTY, operator, values);
    }

    public OrderLineRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public OrderLineRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public OrderLineRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public OrderLineRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public OrderLineRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public OrderLineRequest<T> count(){
        super.count();
        return this;
    }
    public OrderLineRequest<T> countAs(String retName){
        super.count(retName);
        return this;
    }
    public OrderLineRequest minQuantity(){
        return minQuantityAs(prefix("minOf",OrderLine.QUANTITY_PROPERTY));
    }

    public OrderLineRequest minQuantityAs(String retName){
        super.min(retName, OrderLine.QUANTITY_PROPERTY);
        return this;
    }
    public OrderLineRequest maxQuantity(){
        return maxQuantityAs(prefix("maxOf",OrderLine.QUANTITY_PROPERTY));
    }

    public OrderLineRequest maxQuantityAs(String retName){
        super.max(retName, OrderLine.QUANTITY_PROPERTY);
        return this;
    }
    public OrderLineRequest sumQuantity(){
        return sumQuantityAs(prefix("sumOf",OrderLine.QUANTITY_PROPERTY));
    }

    public OrderLineRequest sumQuantityAs(String retName){
        super.sum(retName, OrderLine.QUANTITY_PROPERTY);
        return this;
    }
    public OrderLineRequest avgQuantity(){
        return avgQuantityAs(prefix("avgOf",OrderLine.QUANTITY_PROPERTY));
    }

    public OrderLineRequest avgQuantityAs(String retName){
        super.avg(retName, OrderLine.QUANTITY_PROPERTY);
        return this;
    }
    public OrderLineRequest standardDeviationQuantity(){
        return standardDeviationQuantityAs(prefix("standardDeviationOf",OrderLine.QUANTITY_PROPERTY));
    }

    public OrderLineRequest standardDeviationQuantityAs(String retName){
        super.standardDeviation(retName, OrderLine.QUANTITY_PROPERTY);
        return this;
    }
    public OrderLineRequest squareRootOfPopulationStandardDeviationQuantity(){
        return squareRootOfPopulationStandardDeviationQuantityAs(prefix("squareRootOfPopulationStandardDeviationOf",OrderLine.QUANTITY_PROPERTY));
    }

    public OrderLineRequest squareRootOfPopulationStandardDeviationQuantityAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, OrderLine.QUANTITY_PROPERTY);
        return this;
    }
    public OrderLineRequest sampleVarianceQuantity(){
        return sampleVarianceQuantityAs(prefix("sampleVarianceOf",OrderLine.QUANTITY_PROPERTY));
    }

    public OrderLineRequest sampleVarianceQuantityAs(String retName){
        super.sampleVariance(retName, OrderLine.QUANTITY_PROPERTY);
        return this;
    }
    public OrderLineRequest samplePopulationVarianceQuantity(){
        return samplePopulationVarianceQuantityAs(prefix("samplePopulationVarianceOf",OrderLine.QUANTITY_PROPERTY));
    }

    public OrderLineRequest samplePopulationVarianceQuantityAs(String retName){
        super.samplePopulationVariance(retName, OrderLine.QUANTITY_PROPERTY);
        return this;
    }
    public OrderLineRequest<T> groupByCustomerOrderWithDetails(){
       return groupByCustomerOrderWithDetails(Q.customerOrders().unlimited());
    }

    public OrderLineRequest<T> groupByCustomerOrderWithDetails(CustomerOrderRequest subRequest){
       aggregate(OrderLine.CUSTOMER_ORDER_PROPERTY, subRequest);
       return this;
    }

    public OrderLineRequest<T> groupByProductWithDetails(){
       return groupByProductWithDetails(Q.products().unlimited());
    }

    public OrderLineRequest<T> groupByProductWithDetails(ProductRequest subRequest){
       aggregate(OrderLine.PRODUCT_PROPERTY, subRequest);
       return this;
    }




    public OrderLineRequest<T> groupByCommercePlatformWithDetails(){
       return groupByCommercePlatformWithDetails(Q.commercePlatforms().unlimited());
    }

    public OrderLineRequest<T> groupByCommercePlatformWithDetails(CommercePlatformRequest subRequest){
       aggregate(OrderLine.COMMERCE_PLATFORM_PROPERTY, subRequest);
       return this;
    }




    public OrderLineRequest<T> groupById(){
       groupBy(OrderLine.ID_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> groupByIdAs(String retName){
       groupBy(retName, OrderLine.ID_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderLine.ID_PROPERTY, function);
       return this;
    }
    public OrderLineRequest<T> groupByCustomerOrderWith(CustomerOrderRequest subRequest){
       groupBy(OrderLine.CUSTOMER_ORDER_PROPERTY, subRequest);
       return this;
    }
    public OrderLineRequest<T> groupByCustomerOrder(){
       groupBy(OrderLine.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> groupByCustomerOrderAs(String retName){
       groupBy(retName, OrderLine.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> groupByCustomerOrderWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderLine.CUSTOMER_ORDER_PROPERTY, function);
       return this;
    }
    public OrderLineRequest<T> groupByProductWith(ProductRequest subRequest){
       groupBy(OrderLine.PRODUCT_PROPERTY, subRequest);
       return this;
    }
    public OrderLineRequest<T> groupByProduct(){
       groupBy(OrderLine.PRODUCT_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> groupByProductAs(String retName){
       groupBy(retName, OrderLine.PRODUCT_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> groupByProductWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderLine.PRODUCT_PROPERTY, function);
       return this;
    }

    public OrderLineRequest<T> groupByProductName(){
       groupBy(OrderLine.PRODUCT_NAME_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> groupByProductNameAs(String retName){
       groupBy(retName, OrderLine.PRODUCT_NAME_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> groupByProductNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderLine.PRODUCT_NAME_PROPERTY, function);
       return this;
    }

    public OrderLineRequest<T> groupBySku(){
       groupBy(OrderLine.SKU_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> groupBySkuAs(String retName){
       groupBy(retName, OrderLine.SKU_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> groupBySkuWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderLine.SKU_PROPERTY, function);
       return this;
    }

    public OrderLineRequest<T> groupByQuantity(){
       groupBy(OrderLine.QUANTITY_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> groupByQuantityAs(String retName){
       groupBy(retName, OrderLine.QUANTITY_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> groupByQuantityWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderLine.QUANTITY_PROPERTY, function);
       return this;
    }
    public OrderLineRequest<T> groupByCommercePlatformWith(CommercePlatformRequest subRequest){
       groupBy(OrderLine.COMMERCE_PLATFORM_PROPERTY, subRequest);
       return this;
    }
    public OrderLineRequest<T> groupByCommercePlatform(){
       groupBy(OrderLine.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> groupByCommercePlatformAs(String retName){
       groupBy(retName, OrderLine.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> groupByCommercePlatformWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderLine.COMMERCE_PLATFORM_PROPERTY, function);
       return this;
    }

    public OrderLineRequest<T> groupByCreateTime(){
       groupBy(OrderLine.CREATE_TIME_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> groupByCreateTimeAs(String retName){
       groupBy(retName, OrderLine.CREATE_TIME_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> groupByCreateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderLine.CREATE_TIME_PROPERTY, function);
       return this;
    }

    public OrderLineRequest<T> groupByVersion(){
       groupBy(OrderLine.VERSION_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> groupByVersionAs(String retName){
       groupBy(retName, OrderLine.VERSION_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderLine.VERSION_PROPERTY, function);
       return this;
    }



    public OrderLineRequest<T> orderByIdAscending(){
       addOrderByAscending(OrderLine.ID_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> orderByIdDescending(){
       addOrderByDescending(OrderLine.ID_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> orderByCustomerOrderAscending(){
       addOrderByAscending(OrderLine.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> orderByCustomerOrderDescending(){
       addOrderByDescending(OrderLine.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> orderByProductAscending(){
       addOrderByAscending(OrderLine.PRODUCT_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> orderByProductDescending(){
       addOrderByDescending(OrderLine.PRODUCT_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> orderByProductNameAscending(){
       addOrderByAscending(OrderLine.PRODUCT_NAME_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> orderByProductNameDescending(){
       addOrderByDescending(OrderLine.PRODUCT_NAME_PROPERTY);
       return this;
    }
    public OrderLineRequest<T> orderByProductNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(OrderLine.PRODUCT_NAME_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> orderByProductNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(OrderLine.PRODUCT_NAME_PROPERTY);
       return this;
    }
    public OrderLineRequest<T> orderBySkuAscending(){
       addOrderByAscending(OrderLine.SKU_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> orderBySkuDescending(){
       addOrderByDescending(OrderLine.SKU_PROPERTY);
       return this;
    }
    public OrderLineRequest<T> orderBySkuAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(OrderLine.SKU_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> orderBySkuDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(OrderLine.SKU_PROPERTY);
       return this;
    }
    public OrderLineRequest<T> orderByQuantityAscending(){
       addOrderByAscending(OrderLine.QUANTITY_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> orderByQuantityDescending(){
       addOrderByDescending(OrderLine.QUANTITY_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> orderByCommercePlatformAscending(){
       addOrderByAscending(OrderLine.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> orderByCommercePlatformDescending(){
       addOrderByDescending(OrderLine.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> orderByCreateTimeAscending(){
       addOrderByAscending(OrderLine.CREATE_TIME_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> orderByCreateTimeDescending(){
       addOrderByDescending(OrderLine.CREATE_TIME_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> orderByVersionAscending(){
       addOrderByAscending(OrderLine.VERSION_PROPERTY);
       return this;
    }

    public OrderLineRequest<T> orderByVersionDescending(){
       addOrderByDescending(OrderLine.VERSION_PROPERTY);
       return this;
    }


    public CustomerOrderRequest rollUpToCustomerOrder(){
       CustomerOrderRequest customerOrder = Q.customerOrders().unlimited();
       this.withCustomerOrderMatching(customerOrder)
           .groupByCustomerOrderWith(customerOrder);
       return customerOrder;
    }

    public ProductRequest rollUpToProduct(){
       ProductRequest product = Q.products().unlimited();
       this.withProductMatching(product)
           .groupByProductWith(product);
       return product;
    }




    public CommercePlatformRequest rollUpToCommercePlatform(){
       CommercePlatformRequest commercePlatform = Q.commercePlatforms().unlimited();
       this.withCommercePlatformMatching(commercePlatform)
           .groupByCommercePlatformWith(commercePlatform);
       return commercePlatform;
    }




   public OrderLineRequest<T> facetByCustomerOrderAs(String facetName, CustomerOrderRequest customerOrder){
       return facetByCustomerOrderAs(facetName, customerOrder, true);
   }

   public OrderLineRequest<T> facetByCustomerOrderAs(String facetName, CustomerOrderRequest customerOrder, boolean includeAllFacets){
       addFacet(facetName, OrderLine.CUSTOMER_ORDER_PROPERTY, customerOrder, includeAllFacets);
       return this;
   }
   public OrderLineRequest<T> facetByProductAs(String facetName, ProductRequest product){
       return facetByProductAs(facetName, product, true);
   }

   public OrderLineRequest<T> facetByProductAs(String facetName, ProductRequest product, boolean includeAllFacets){
       addFacet(facetName, OrderLine.PRODUCT_PROPERTY, product, includeAllFacets);
       return this;
   }
   public OrderLineRequest<T> facetByCommercePlatformAs(String facetName, CommercePlatformRequest commercePlatform){
       return facetByCommercePlatformAs(facetName, commercePlatform, true);
   }

   public OrderLineRequest<T> facetByCommercePlatformAs(String facetName, CommercePlatformRequest commercePlatform, boolean includeAllFacets){
       addFacet(facetName, OrderLine.COMMERCE_PLATFORM_PROPERTY, commercePlatform, includeAllFacets);
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public OrderLineRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public OrderLineRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public OrderLineRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public OrderLineRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public OrderLineRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}