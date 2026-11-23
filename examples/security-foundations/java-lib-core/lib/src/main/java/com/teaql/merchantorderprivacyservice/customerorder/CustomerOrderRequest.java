
package com.teaql.merchantorderprivacyservice.customerorder;

import com.teaql.merchantorderprivacyservice.Q;
import com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile;
import com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfileRequest;
import com.teaql.merchantorderprivacyservice.merchant.Merchant;
import com.teaql.merchantorderprivacyservice.merchant.MerchantRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.util.Objects;

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

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public CustomerOrderRequest<T> copy(){
        return (CustomerOrderRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new CustomerOrderRequest<>((Class<T>) returnType(), this::internalNewEntity);
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

    public CustomerOrderRequest<T> matchingIdsOf(CustomerOrderRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                CustomerOrder.ID_PROPERTY, request, CustomerOrder.ID_PROPERTY));
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
        return selectId().selectMerchantIdOnly().selectCustomerProfileIdOnly().selectOrderNumber().selectFulfillmentStatus().selectVersion();
    }

    public CustomerOrderRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public CustomerOrderRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(CustomerOrder.ID_PROPERTY);
        selectProperty(CustomerOrder.MERCHANT_PROPERTY);
        selectProperty(CustomerOrder.CUSTOMER_PROFILE_PROPERTY);
        selectProperty(CustomerOrder.ORDER_NUMBER_PROPERTY);
        selectProperty(CustomerOrder.FULFILLMENT_STATUS_PROPERTY);
        selectProperty(CustomerOrder.VERSION_PROPERTY);
        return this;
    }

    public CustomerOrderRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectMerchant().selectCustomerProfile().selectOrderNumber().selectFulfillmentStatus().selectVersion();
    }

    public CustomerOrderRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectMerchant().selectCustomerProfile().selectOrderNumber().selectFulfillmentStatus().selectVersion();
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
    public CustomerOrderRequest<T> selectMerchantIdOnly(){
       selectProperty(CustomerOrder.MERCHANT_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> selectMerchant(){
        return selectMerchantWith(Q.merchants().unlimited().selectSelf());
    }

    public CustomerOrderRequest<T> selectMerchantWith(MerchantRequest merchant){
       selectProperty(CustomerOrder.MERCHANT_PROPERTY);
       enhanceRelation(CustomerOrder.MERCHANT_PROPERTY, merchant,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectMerchantWith", null, CustomerOrder.MERCHANT_PROPERTY));
       return this;
    }

    public CustomerOrderRequest<T> unselectMerchant(){
       unselectProperty(CustomerOrder.MERCHANT_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> selectCustomerProfileIdOnly(){
       selectProperty(CustomerOrder.CUSTOMER_PROFILE_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> selectCustomerProfile(){
        return selectCustomerProfileWith(Q.customerProfiles().unlimited().selectSelf());
    }

    public CustomerOrderRequest<T> selectCustomerProfileWith(CustomerProfileRequest customerProfile){
       selectProperty(CustomerOrder.CUSTOMER_PROFILE_PROPERTY);
       enhanceRelation(CustomerOrder.CUSTOMER_PROFILE_PROPERTY, customerProfile,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectCustomerProfileWith", null, CustomerOrder.CUSTOMER_PROFILE_PROPERTY));
       return this;
    }

    public CustomerOrderRequest<T> unselectCustomerProfile(){
       unselectProperty(CustomerOrder.CUSTOMER_PROFILE_PROPERTY);
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
    public CustomerOrderRequest<T> selectFulfillmentStatus(){
       selectProperty(CustomerOrder.FULFILLMENT_STATUS_PROPERTY);
       return this;
    }

    /**
     * fill the fulfillmentStatus with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  fulfillmentStatus) to fetch fulfillmentStatus property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerOrderRequest<T> unselectFulfillmentStatus(){
       unselectProperty(CustomerOrder.FULFILLMENT_STATUS_PROPERTY);
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



    public CustomerOrderRequest<T> filterByMerchant(Merchant... merchant){
      if (merchant == null || merchant.length == 0) {
        throw new IllegalArgumentException("filterByMerchant parameter merchant cannot be empty");
      }
      return appendSearchCriteria(createMerchantCriteria(Operator.EQUAL, (Object[])merchant));
    }

    public CustomerOrderRequest<T> withMerchant(Operator operator, Object... values){
       return appendSearchCriteria(createMerchantCriteria(operator, values));
    }

    public CustomerOrderRequest<T> withMerchantIsUnknown(){
       return withMerchant(Operator.IS_NULL);
    }

    public CustomerOrderRequest<T> withMerchantIsKnown(){
       return withMerchant(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createMerchantCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.MERCHANT_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> filterByMerchant(Long merchant){
      if(merchant == null){
         return this;
      }
      return withMerchant(Operator.EQUAL, merchant);
    }
    public CustomerOrderRequest<T> withMerchantMatching(MerchantRequest merchant){
       return appendSearchCriteria(new SubQuerySearchCriteria(CustomerOrder.MERCHANT_PROPERTY, merchant, Merchant.ID_PROPERTY));
    }

    public CustomerOrderRequest<T> withoutMerchantMatching(MerchantRequest merchant){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(CustomerOrder.MERCHANT_PROPERTY, merchant, Merchant.ID_PROPERTY)));
    }

    public CustomerOrderRequest<T> filterByCustomerProfile(CustomerProfile... customerProfile){
      if (customerProfile == null || customerProfile.length == 0) {
        throw new IllegalArgumentException("filterByCustomerProfile parameter customerProfile cannot be empty");
      }
      return appendSearchCriteria(createCustomerProfileCriteria(Operator.EQUAL, (Object[])customerProfile));
    }

    public CustomerOrderRequest<T> withCustomerProfile(Operator operator, Object... values){
       return appendSearchCriteria(createCustomerProfileCriteria(operator, values));
    }

    public CustomerOrderRequest<T> withCustomerProfileIsUnknown(){
       return withCustomerProfile(Operator.IS_NULL);
    }

    public CustomerOrderRequest<T> withCustomerProfileIsKnown(){
       return withCustomerProfile(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCustomerProfileCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.CUSTOMER_PROFILE_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> filterByCustomerProfile(Long customerProfile){
      if(customerProfile == null){
         return this;
      }
      return withCustomerProfile(Operator.EQUAL, customerProfile);
    }
    public CustomerOrderRequest<T> withCustomerProfileMatching(CustomerProfileRequest customerProfile){
       return appendSearchCriteria(new SubQuerySearchCriteria(CustomerOrder.CUSTOMER_PROFILE_PROPERTY, customerProfile, CustomerProfile.ID_PROPERTY));
    }

    public CustomerOrderRequest<T> withoutCustomerProfileMatching(CustomerProfileRequest customerProfile){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(CustomerOrder.CUSTOMER_PROFILE_PROPERTY, customerProfile, CustomerProfile.ID_PROPERTY)));
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



    public CustomerOrderRequest<T> filterByFulfillmentStatus(String... fulfillmentStatus){
      if (fulfillmentStatus == null || fulfillmentStatus.length == 0) {
        throw new IllegalArgumentException("filterByFulfillmentStatus parameter fulfillmentStatus cannot be empty");
      }
      return appendSearchCriteria(createFulfillmentStatusCriteria(Operator.EQUAL, (Object[])fulfillmentStatus));
    }

    public CustomerOrderRequest<T> withFulfillmentStatus(Operator operator, Object... values){
       return appendSearchCriteria(createFulfillmentStatusCriteria(operator, values));
    }

    public CustomerOrderRequest<T> withFulfillmentStatusIsUnknown(){
       return withFulfillmentStatus(Operator.IS_NULL);
    }

    public CustomerOrderRequest<T> withFulfillmentStatusIsKnown(){
       return withFulfillmentStatus(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createFulfillmentStatusCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.FULFILLMENT_STATUS_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> withFulfillmentStatusIsNot(String fulfillmentStatus){
       return withFulfillmentStatus(Operator.NOT_EQUAL, fulfillmentStatus);
    }

    public CustomerOrderRequest<T> withFulfillmentStatusIn(String... fulfillmentStatus){
       return withFulfillmentStatus(Operator.IN, (Object[])fulfillmentStatus);
    }

    public CustomerOrderRequest<T> withFulfillmentStatusNotIn(String... fulfillmentStatus){
       return withFulfillmentStatus(Operator.NOT_IN, (Object[])fulfillmentStatus);
    }
    public CustomerOrderRequest<T> withFulfillmentStatusGreaterThan(String fulfillmentStatus){
       return withFulfillmentStatus(Operator.GREATER_THAN, fulfillmentStatus);
    }

    public CustomerOrderRequest<T> withFulfillmentStatusGreaterThanOrEqualTo(String fulfillmentStatus){
       return withFulfillmentStatus(Operator.GREATER_THAN_OR_EQUAL, fulfillmentStatus);
    }

    public CustomerOrderRequest<T> withFulfillmentStatusLessThan(String fulfillmentStatus){
       return withFulfillmentStatus(Operator.LESS_THAN, fulfillmentStatus);
    }

    public CustomerOrderRequest<T> withFulfillmentStatusLessThanOrEqualTo(String fulfillmentStatus){
       return withFulfillmentStatus(Operator.LESS_THAN_OR_EQUAL, fulfillmentStatus);
    }

    public CustomerOrderRequest<T> withFulfillmentStatusBetween(String startOfFulfillmentStatus, String endOfFulfillmentStatus){
       return withFulfillmentStatus(Operator.BETWEEN, startOfFulfillmentStatus, endOfFulfillmentStatus);
    }
    public CustomerOrderRequest<T> withFulfillmentStatusStartingWith(String fulfillmentStatus){
       return withFulfillmentStatus(Operator.BEGIN_WITH, fulfillmentStatus);
    }
    public CustomerOrderRequest<T> withFulfillmentStatusContaining(String fulfillmentStatus){
       return withFulfillmentStatus(Operator.CONTAIN, fulfillmentStatus);
    }

    public CustomerOrderRequest<T> withFulfillmentStatusNotContaining(String fulfillmentStatus){
       return withFulfillmentStatus(Operator.NOT_CONTAIN, fulfillmentStatus);
    }

    public CustomerOrderRequest<T> withFulfillmentStatusNotStartingWith(String fulfillmentStatus){
       return withFulfillmentStatus(Operator.NOT_BEGIN_WITH, fulfillmentStatus);
    }

    public CustomerOrderRequest<T> withFulfillmentStatusEndingWith(String fulfillmentStatus){
       return withFulfillmentStatus(Operator.END_WITH, fulfillmentStatus);
    }

    public CustomerOrderRequest<T> withFulfillmentStatusNotEndingWith(String fulfillmentStatus){
       return withFulfillmentStatus(Operator.NOT_END_WITH, fulfillmentStatus);
    }

    public CustomerOrderRequest<T> withFulfillmentStatusIs(String fulfillmentStatus){
       return withFulfillmentStatus(Operator.EQUAL, fulfillmentStatus);
    }

    public CustomerOrderRequest<T> withFulfillmentStatusSoundingLike(String fulfillmentStatus){
       return withFulfillmentStatus(Operator.SOUNDS_LIKE, fulfillmentStatus);
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
    public CustomerOrderRequest<T> groupByMerchantWithDetails(){
       return groupByMerchantWithDetails(Q.merchants().unlimited());
    }

    public CustomerOrderRequest<T> groupByMerchantWithDetails(MerchantRequest subRequest){
       aggregate(CustomerOrder.MERCHANT_PROPERTY, subRequest);
       return this;
    }

    public CustomerOrderRequest<T> groupByCustomerProfileWithDetails(){
       return groupByCustomerProfileWithDetails(Q.customerProfiles().unlimited());
    }

    public CustomerOrderRequest<T> groupByCustomerProfileWithDetails(CustomerProfileRequest subRequest){
       aggregate(CustomerOrder.CUSTOMER_PROFILE_PROPERTY, subRequest);
       return this;
    }




    public CustomerOrderRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public CustomerOrderRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, CustomerOrder.ID_PROPERTY);
        return this;
    }
    public CustomerOrderRequest<T> countDistinctMerchant(){
        return countDistinctMerchantAs("merchant_distinct_count");
    }

    public CustomerOrderRequest<T> countDistinctMerchantAs(String alias){
        super.countDistinctProperty(alias, CustomerOrder.MERCHANT_PROPERTY);
        return this;
    }
    public CustomerOrderRequest<T> countDistinctCustomerProfile(){
        return countDistinctCustomerProfileAs("customer_profile_distinct_count");
    }

    public CustomerOrderRequest<T> countDistinctCustomerProfileAs(String alias){
        super.countDistinctProperty(alias, CustomerOrder.CUSTOMER_PROFILE_PROPERTY);
        return this;
    }
    public CustomerOrderRequest<T> countDistinctOrderNumber(){
        return countDistinctOrderNumberAs("order_number_distinct_count");
    }

    public CustomerOrderRequest<T> countDistinctOrderNumberAs(String alias){
        super.countDistinctProperty(alias, CustomerOrder.ORDER_NUMBER_PROPERTY);
        return this;
    }
    public CustomerOrderRequest<T> countDistinctFulfillmentStatus(){
        return countDistinctFulfillmentStatusAs("fulfillment_status_distinct_count");
    }

    public CustomerOrderRequest<T> countDistinctFulfillmentStatusAs(String alias){
        super.countDistinctProperty(alias, CustomerOrder.FULFILLMENT_STATUS_PROPERTY);
        return this;
    }
    public CustomerOrderRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public CustomerOrderRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, CustomerOrder.VERSION_PROPERTY);
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
    public CustomerOrderRequest<T> groupByMerchantWith(MerchantRequest subRequest){
       groupBy(CustomerOrder.MERCHANT_PROPERTY, subRequest,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "groupByMerchantWith", null, "merchant"));
       return this;
    }
    public CustomerOrderRequest<T> groupByMerchant(){
       groupBy(CustomerOrder.MERCHANT_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByMerchantAs(String retName){
       groupBy(retName, CustomerOrder.MERCHANT_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByMerchantWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.MERCHANT_PROPERTY, function);
       return this;
    }
    public CustomerOrderRequest<T> groupByCustomerProfileWith(CustomerProfileRequest subRequest){
       groupBy(CustomerOrder.CUSTOMER_PROFILE_PROPERTY, subRequest,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "groupByCustomerProfileWith", null, "customer_profile"));
       return this;
    }
    public CustomerOrderRequest<T> groupByCustomerProfile(){
       groupBy(CustomerOrder.CUSTOMER_PROFILE_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByCustomerProfileAs(String retName){
       groupBy(retName, CustomerOrder.CUSTOMER_PROFILE_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByCustomerProfileWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.CUSTOMER_PROFILE_PROPERTY, function);
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

    public CustomerOrderRequest<T> groupByFulfillmentStatus(){
       groupBy(CustomerOrder.FULFILLMENT_STATUS_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByFulfillmentStatusAs(String retName){
       groupBy(retName, CustomerOrder.FULFILLMENT_STATUS_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByFulfillmentStatusWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.FULFILLMENT_STATUS_PROPERTY, function);
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

    public CustomerOrderRequest<T> orderByMerchantAscending(){
       addOrderByAscending(CustomerOrder.MERCHANT_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByMerchantDescending(){
       addOrderByDescending(CustomerOrder.MERCHANT_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByCustomerProfileAscending(){
       addOrderByAscending(CustomerOrder.CUSTOMER_PROFILE_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByCustomerProfileDescending(){
       addOrderByDescending(CustomerOrder.CUSTOMER_PROFILE_PROPERTY);
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
    public CustomerOrderRequest<T> orderByFulfillmentStatusAscending(){
       addOrderByAscending(CustomerOrder.FULFILLMENT_STATUS_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByFulfillmentStatusDescending(){
       addOrderByDescending(CustomerOrder.FULFILLMENT_STATUS_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> orderByFulfillmentStatusAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(CustomerOrder.FULFILLMENT_STATUS_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByFulfillmentStatusDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(CustomerOrder.FULFILLMENT_STATUS_PROPERTY);
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


    public MerchantRequest rollUpToMerchant(){
       MerchantRequest merchant = Q.merchants().unlimited();
       this.internalRollUpTo("merchant", merchant,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "rollUpToMerchant", null, "merchant"));
       return merchant;
    }

    public CustomerProfileRequest rollUpToCustomerProfile(){
       CustomerProfileRequest customerProfile = Q.customerProfiles().unlimited();
       this.internalRollUpTo("customer_profile", customerProfile,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "rollUpToCustomerProfile", null, "customer_profile"));
       return customerProfile;
    }





   public CustomerOrderRequest<T> facetByMerchantAs(String facetName, MerchantRequest merchant){
       return facetByMerchantAs(facetName, merchant, true);
   }

   public CustomerOrderRequest<T> facetByMerchantAs(String facetName, MerchantRequest merchant, boolean includeAllFacets){
       addFacet(facetName, CustomerOrder.MERCHANT_PROPERTY, merchant, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByMerchantAs", facetName, CustomerOrder.MERCHANT_PROPERTY));
       return this;
   }
   public CustomerOrderRequest<T> facetByCustomerProfileAs(String facetName, CustomerProfileRequest customerProfile){
       return facetByCustomerProfileAs(facetName, customerProfile, true);
   }

   public CustomerOrderRequest<T> facetByCustomerProfileAs(String facetName, CustomerProfileRequest customerProfile, boolean includeAllFacets){
       addFacet(facetName, CustomerOrder.CUSTOMER_PROFILE_PROPERTY, customerProfile, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByCustomerProfileAs", facetName, CustomerOrder.CUSTOMER_PROFILE_PROPERTY));
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