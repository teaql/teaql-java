
package com.teaql.merchantorderprivacyservice.customerprofile;

import com.teaql.merchantorderprivacyservice.Q;
import com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder;
import com.teaql.merchantorderprivacyservice.customerorder.CustomerOrderRequest;
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

public class CustomerProfileRequest<T extends CustomerProfile> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public CustomerProfileRequest(Class<T> returnType){
        this(returnType, () -> (T) new CustomerProfile());
    }

    protected CustomerProfileRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public CustomerProfileRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public CustomerProfileRequest<T> copy(){
        return (CustomerProfileRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new CustomerProfileRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public CustomerProfileRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public CustomerProfileRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public CustomerProfileRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public CustomerProfileRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public CustomerProfileRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public CustomerProfileRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public CustomerProfileRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public CustomerProfileRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public CustomerProfileRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public CustomerProfileRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (CustomerProfileRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public CustomerProfileRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public CustomerProfileRequest<T> matchingAnyOf(CustomerProfileRequest customerProfile){
        super.internalMatchAny(customerProfile);
        return this;
    }

    public CustomerProfileRequest<T> matchingIdsOf(CustomerProfileRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                CustomerProfile.ID_PROPERTY, request, CustomerProfile.ID_PROPERTY));
    }

    public CustomerProfileRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public CustomerProfileRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public CustomerProfileRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public CustomerProfileRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectMerchantIdOnly().selectEmail().selectVersion();
    }

    public CustomerProfileRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public CustomerProfileRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(CustomerProfile.ID_PROPERTY);
        selectProperty(CustomerProfile.MERCHANT_PROPERTY);
        selectProperty(CustomerProfile.EMAIL_PROPERTY);
        selectProperty(CustomerProfile.VERSION_PROPERTY);
        return this;
    }

    public CustomerProfileRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectMerchant().selectEmail().selectVersion();
    }

    public CustomerProfileRequest<T> selectChildren(){
        super.selectAny();
        selectCustomerOrderList();
        return selectId().selectMerchant().selectEmail().selectVersion();
    }


    public CustomerProfileRequest<T> selectId(){
       selectProperty(CustomerProfile.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerProfileRequest<T> unselectId(){
       unselectProperty(CustomerProfile.ID_PROPERTY);
       return this;
    }
    public CustomerProfileRequest<T> selectMerchantIdOnly(){
       selectProperty(CustomerProfile.MERCHANT_PROPERTY);
       return this;
    }

    public CustomerProfileRequest<T> selectMerchant(){
        return selectMerchantWith(Q.merchants().unlimited().selectSelf());
    }

    public CustomerProfileRequest<T> selectMerchantWith(MerchantRequest merchant){
       selectProperty(CustomerProfile.MERCHANT_PROPERTY);
       enhanceRelation(CustomerProfile.MERCHANT_PROPERTY, merchant,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectMerchantWith", null, CustomerProfile.MERCHANT_PROPERTY));
       return this;
    }

    public CustomerProfileRequest<T> unselectMerchant(){
       unselectProperty(CustomerProfile.MERCHANT_PROPERTY);
       return this;
    }
    public CustomerProfileRequest<T> selectEmail(){
       selectProperty(CustomerProfile.EMAIL_PROPERTY);
       return this;
    }

    /**
     * fill the email with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  email) to fetch email property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerProfileRequest<T> unselectEmail(){
       unselectProperty(CustomerProfile.EMAIL_PROPERTY);
       return this;
    }
    public CustomerProfileRequest<T> selectVersion(){
       selectProperty(CustomerProfile.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerProfileRequest<T> unselectVersion(){
       unselectProperty(CustomerProfile.VERSION_PROPERTY);
       return this;
    }
    public CustomerProfileRequest<T> selectCustomerOrderList(){
       return selectCustomerOrderListWith(Q.customerOrders().selectSelf());
    }

    public CustomerProfileRequest<T> selectCustomerOrderListWith(CustomerOrderRequest customerOrderList){
       enhanceRelation(CustomerProfile.CUSTOMER_ORDER_LIST_PROPERTY, customerOrderList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectCustomerOrderListWith", null, CustomerProfile.CUSTOMER_ORDER_LIST_PROPERTY));
       return this;
    }

    public CustomerProfileRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerProfile.ID_PROPERTY, operator, values);
    }

    public CustomerProfileRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public CustomerProfileRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public CustomerProfileRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public CustomerProfileRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public CustomerProfileRequest<T> filterByMerchant(Merchant... merchant){
      if (merchant == null || merchant.length == 0) {
        throw new IllegalArgumentException("filterByMerchant parameter merchant cannot be empty");
      }
      return appendSearchCriteria(createMerchantCriteria(Operator.EQUAL, (Object[])merchant));
    }

    public CustomerProfileRequest<T> withMerchant(Operator operator, Object... values){
       return appendSearchCriteria(createMerchantCriteria(operator, values));
    }

    public CustomerProfileRequest<T> withMerchantIsUnknown(){
       return withMerchant(Operator.IS_NULL);
    }

    public CustomerProfileRequest<T> withMerchantIsKnown(){
       return withMerchant(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createMerchantCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerProfile.MERCHANT_PROPERTY, operator, values);
    }

    public CustomerProfileRequest<T> filterByMerchant(Long merchant){
      if(merchant == null){
         return this;
      }
      return withMerchant(Operator.EQUAL, merchant);
    }
    public CustomerProfileRequest<T> withMerchantMatching(MerchantRequest merchant){
       return appendSearchCriteria(new SubQuerySearchCriteria(CustomerProfile.MERCHANT_PROPERTY, merchant, Merchant.ID_PROPERTY));
    }

    public CustomerProfileRequest<T> withoutMerchantMatching(MerchantRequest merchant){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(CustomerProfile.MERCHANT_PROPERTY, merchant, Merchant.ID_PROPERTY)));
    }

    public CustomerProfileRequest<T> filterByEmail(String... email){
      if (email == null || email.length == 0) {
        throw new IllegalArgumentException("filterByEmail parameter email cannot be empty");
      }
      return appendSearchCriteria(createEmailCriteria(Operator.EQUAL, (Object[])email));
    }

    public CustomerProfileRequest<T> withEmail(Operator operator, Object... values){
       return appendSearchCriteria(createEmailCriteria(operator, values));
    }

    public CustomerProfileRequest<T> withEmailIsUnknown(){
       return withEmail(Operator.IS_NULL);
    }

    public CustomerProfileRequest<T> withEmailIsKnown(){
       return withEmail(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createEmailCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerProfile.EMAIL_PROPERTY, operator, values);
    }

    public CustomerProfileRequest<T> withEmailIsNot(String email){
       return withEmail(Operator.NOT_EQUAL, email);
    }

    public CustomerProfileRequest<T> withEmailIn(String... email){
       return withEmail(Operator.IN, (Object[])email);
    }

    public CustomerProfileRequest<T> withEmailNotIn(String... email){
       return withEmail(Operator.NOT_IN, (Object[])email);
    }
    public CustomerProfileRequest<T> withEmailGreaterThan(String email){
       return withEmail(Operator.GREATER_THAN, email);
    }

    public CustomerProfileRequest<T> withEmailGreaterThanOrEqualTo(String email){
       return withEmail(Operator.GREATER_THAN_OR_EQUAL, email);
    }

    public CustomerProfileRequest<T> withEmailLessThan(String email){
       return withEmail(Operator.LESS_THAN, email);
    }

    public CustomerProfileRequest<T> withEmailLessThanOrEqualTo(String email){
       return withEmail(Operator.LESS_THAN_OR_EQUAL, email);
    }

    public CustomerProfileRequest<T> withEmailBetween(String startOfEmail, String endOfEmail){
       return withEmail(Operator.BETWEEN, startOfEmail, endOfEmail);
    }
    public CustomerProfileRequest<T> withEmailStartingWith(String email){
       return withEmail(Operator.BEGIN_WITH, email);
    }
    public CustomerProfileRequest<T> withEmailContaining(String email){
       return withEmail(Operator.CONTAIN, email);
    }

    public CustomerProfileRequest<T> withEmailNotContaining(String email){
       return withEmail(Operator.NOT_CONTAIN, email);
    }

    public CustomerProfileRequest<T> withEmailNotStartingWith(String email){
       return withEmail(Operator.NOT_BEGIN_WITH, email);
    }

    public CustomerProfileRequest<T> withEmailEndingWith(String email){
       return withEmail(Operator.END_WITH, email);
    }

    public CustomerProfileRequest<T> withEmailNotEndingWith(String email){
       return withEmail(Operator.NOT_END_WITH, email);
    }

    public CustomerProfileRequest<T> withEmailIs(String email){
       return withEmail(Operator.EQUAL, email);
    }

    public CustomerProfileRequest<T> withEmailSoundingLike(String email){
       return withEmail(Operator.SOUNDS_LIKE, email);
    }



    public CustomerProfileRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public CustomerProfileRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public CustomerProfileRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public CustomerProfileRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerProfile.VERSION_PROPERTY, operator, values);
    }

    public CustomerProfileRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public CustomerProfileRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public CustomerProfileRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public CustomerProfileRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public CustomerProfileRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public CustomerProfileRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public CustomerProfileRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public CustomerProfileRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public CustomerProfileRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public CustomerProfileRequest<T> withCustomerOrderListMatching(CustomerOrderRequest customerOrderRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(CustomerProfile.ID_PROPERTY, customerOrderRequest, CustomerOrder.CUSTOMER_PROFILE_PROPERTY));
    }

    public CustomerProfileRequest<T> withoutCustomerOrderListMatching(CustomerOrderRequest customerOrderRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(CustomerProfile.ID_PROPERTY, customerOrderRequest, CustomerOrder.CUSTOMER_PROFILE_PROPERTY)));
    }

    public CustomerProfileRequest<T> haveCustomerOrders(){
        return withCustomerOrderListMatching(Q.customerOrders().unlimited());
    }

    public CustomerProfileRequest<T> haveNoCustomerOrders(){
        return withoutCustomerOrderListMatching(Q.customerOrders().unlimited());
    }

    public CustomerProfileRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public CustomerProfileRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public CustomerProfileRequest<T> groupByMerchantWithDetails(){
       return groupByMerchantWithDetails(Q.merchants().unlimited());
    }

    public CustomerProfileRequest<T> groupByMerchantWithDetails(MerchantRequest subRequest){
       aggregate(CustomerProfile.MERCHANT_PROPERTY, subRequest);
       return this;
    }



    public CustomerProfileRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public CustomerProfileRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, CustomerProfile.ID_PROPERTY);
        return this;
    }
    public CustomerProfileRequest<T> countDistinctMerchant(){
        return countDistinctMerchantAs("merchant_distinct_count");
    }

    public CustomerProfileRequest<T> countDistinctMerchantAs(String alias){
        super.countDistinctProperty(alias, CustomerProfile.MERCHANT_PROPERTY);
        return this;
    }
    public CustomerProfileRequest<T> countDistinctEmail(){
        return countDistinctEmailAs("email_distinct_count");
    }

    public CustomerProfileRequest<T> countDistinctEmailAs(String alias){
        super.countDistinctProperty(alias, CustomerProfile.EMAIL_PROPERTY);
        return this;
    }
    public CustomerProfileRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public CustomerProfileRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, CustomerProfile.VERSION_PROPERTY);
        return this;
    }
    public CustomerProfileRequest<T> groupByCustomerOrdersWithDetails(CustomerOrderRequest subRequest){
       aggregate(CustomerProfile.CUSTOMER_ORDER_LIST_PROPERTY, subRequest);
       return this;
    }

    public CustomerProfileRequest<T> groupById(){
       groupBy(CustomerProfile.ID_PROPERTY);
       return this;
    }

    public CustomerProfileRequest<T> groupByIdAs(String retName){
       groupBy(retName, CustomerProfile.ID_PROPERTY);
       return this;
    }

    public CustomerProfileRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerProfile.ID_PROPERTY, function);
       return this;
    }
    public CustomerProfileRequest<T> groupByMerchantWith(MerchantRequest subRequest){
       groupBy(CustomerProfile.MERCHANT_PROPERTY, subRequest,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "groupByMerchantWith", null, "merchant"));
       return this;
    }
    public CustomerProfileRequest<T> groupByMerchant(){
       groupBy(CustomerProfile.MERCHANT_PROPERTY);
       return this;
    }

    public CustomerProfileRequest<T> groupByMerchantAs(String retName){
       groupBy(retName, CustomerProfile.MERCHANT_PROPERTY);
       return this;
    }

    public CustomerProfileRequest<T> groupByMerchantWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerProfile.MERCHANT_PROPERTY, function);
       return this;
    }

    public CustomerProfileRequest<T> groupByEmail(){
       groupBy(CustomerProfile.EMAIL_PROPERTY);
       return this;
    }

    public CustomerProfileRequest<T> groupByEmailAs(String retName){
       groupBy(retName, CustomerProfile.EMAIL_PROPERTY);
       return this;
    }

    public CustomerProfileRequest<T> groupByEmailWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerProfile.EMAIL_PROPERTY, function);
       return this;
    }

    public CustomerProfileRequest<T> groupByVersion(){
       groupBy(CustomerProfile.VERSION_PROPERTY);
       return this;
    }

    public CustomerProfileRequest<T> groupByVersionAs(String retName){
       groupBy(retName, CustomerProfile.VERSION_PROPERTY);
       return this;
    }

    public CustomerProfileRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerProfile.VERSION_PROPERTY, function);
       return this;
    }



    public CustomerProfileRequest<T> orderByIdAscending(){
       addOrderByAscending(CustomerProfile.ID_PROPERTY);
       return this;
    }

    public CustomerProfileRequest<T> orderByIdDescending(){
       addOrderByDescending(CustomerProfile.ID_PROPERTY);
       return this;
    }

    public CustomerProfileRequest<T> orderByMerchantAscending(){
       addOrderByAscending(CustomerProfile.MERCHANT_PROPERTY);
       return this;
    }

    public CustomerProfileRequest<T> orderByMerchantDescending(){
       addOrderByDescending(CustomerProfile.MERCHANT_PROPERTY);
       return this;
    }

    public CustomerProfileRequest<T> orderByEmailAscending(){
       addOrderByAscending(CustomerProfile.EMAIL_PROPERTY);
       return this;
    }

    public CustomerProfileRequest<T> orderByEmailDescending(){
       addOrderByDescending(CustomerProfile.EMAIL_PROPERTY);
       return this;
    }
    public CustomerProfileRequest<T> orderByEmailAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(CustomerProfile.EMAIL_PROPERTY);
       return this;
    }

    public CustomerProfileRequest<T> orderByEmailDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(CustomerProfile.EMAIL_PROPERTY);
       return this;
    }
    public CustomerProfileRequest<T> orderByVersionAscending(){
       addOrderByAscending(CustomerProfile.VERSION_PROPERTY);
       return this;
    }

    public CustomerProfileRequest<T> orderByVersionDescending(){
       addOrderByDescending(CustomerProfile.VERSION_PROPERTY);
       return this;
    }


    public CustomerProfileRequest<T> statsFromCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
       return statsFromCustomerOrdersAs(name, subRequest, false);
    }

    public CustomerProfileRequest<T> statsFromCustomerOrdersAs(String name, CustomerOrderRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, CustomerOrder.CUSTOMER_PROFILE_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromCustomerOrdersAs", name, CustomerProfile.CUSTOMER_ORDER_LIST_PROPERTY));
       return this;
    }

    public CustomerProfileRequest<T> statsFromCustomerOrders(CustomerOrderRequest subRequest){
       return statsFromCustomerOrdersAs(REFINEMENTS, subRequest);
    }
    public MerchantRequest rollUpToMerchant(){
       MerchantRequest merchant = Q.merchants().unlimited();
       this.internalRollUpTo("merchant", merchant,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "rollUpToMerchant", null, "merchant"));
       return merchant;
    }



    public CustomerProfileRequest<T> countCustomerOrders(){
        addRelationCount("Count", Q.customerOrders().unlimited(), CustomerOrder.CUSTOMER_PROFILE_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomerOrders", null, CustomerProfile.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CustomerProfileRequest<T> countCustomerOrdersAs(String name){
        addRelationCount(name, Q.customerOrders().unlimited(), CustomerOrder.CUSTOMER_PROFILE_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomerOrdersAs", name, CustomerProfile.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public CustomerProfileRequest<T> countCustomerOrdersWith(String name, CustomerOrderRequest subRequest){
        addRelationCount(name, subRequest, CustomerOrder.CUSTOMER_PROFILE_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomerOrdersWith", name, CustomerProfile.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

   public CustomerProfileRequest<T> facetByMerchantAs(String facetName, MerchantRequest merchant){
       return facetByMerchantAs(facetName, merchant, true);
   }

   public CustomerProfileRequest<T> facetByMerchantAs(String facetName, MerchantRequest merchant, boolean includeAllFacets){
       addFacet(facetName, CustomerProfile.MERCHANT_PROPERTY, merchant, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByMerchantAs", facetName, CustomerProfile.MERCHANT_PROPERTY));
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public CustomerProfileRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public CustomerProfileRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public CustomerProfileRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public CustomerProfileRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public CustomerProfileRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public CustomerProfileRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}