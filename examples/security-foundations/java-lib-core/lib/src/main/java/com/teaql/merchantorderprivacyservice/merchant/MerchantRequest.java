
package com.teaql.merchantorderprivacyservice.merchant;

import com.teaql.merchantorderprivacyservice.Q;
import com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder;
import com.teaql.merchantorderprivacyservice.customerorder.CustomerOrderRequest;
import com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile;
import com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfileRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.util.Objects;

public class MerchantRequest<T extends Merchant> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public MerchantRequest(Class<T> returnType){
        this(returnType, () -> (T) new Merchant());
    }

    protected MerchantRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public MerchantRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public MerchantRequest<T> copy(){
        return (MerchantRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new MerchantRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public MerchantRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public MerchantRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public MerchantRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public MerchantRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public MerchantRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public MerchantRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public MerchantRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public MerchantRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public MerchantRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public MerchantRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (MerchantRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public MerchantRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public MerchantRequest<T> matchingAnyOf(MerchantRequest merchant){
        super.internalMatchAny(merchant);
        return this;
    }

    public MerchantRequest<T> matchingIdsOf(MerchantRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                Merchant.ID_PROPERTY, request, Merchant.ID_PROPERTY));
    }

    public MerchantRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public MerchantRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public MerchantRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public MerchantRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectName().selectVersion();
    }

    public MerchantRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public MerchantRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(Merchant.ID_PROPERTY);
        selectProperty(Merchant.NAME_PROPERTY);
        selectProperty(Merchant.VERSION_PROPERTY);
        return this;
    }

    public MerchantRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectName().selectVersion();
    }

    public MerchantRequest<T> selectChildren(){
        super.selectAny();
        selectCustomerProfileList().selectCustomerOrderList();
        return selectId().selectName().selectVersion();
    }


    public MerchantRequest<T> selectId(){
       selectProperty(Merchant.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public MerchantRequest<T> unselectId(){
       unselectProperty(Merchant.ID_PROPERTY);
       return this;
    }
    public MerchantRequest<T> selectName(){
       selectProperty(Merchant.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public MerchantRequest<T> unselectName(){
       unselectProperty(Merchant.NAME_PROPERTY);
       return this;
    }
    public MerchantRequest<T> selectVersion(){
       selectProperty(Merchant.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public MerchantRequest<T> unselectVersion(){
       unselectProperty(Merchant.VERSION_PROPERTY);
       return this;
    }
    public MerchantRequest<T> selectCustomerProfileList(){
       return selectCustomerProfileListWith(Q.customerProfiles().selectSelf());
    }

    public MerchantRequest<T> selectCustomerProfileListWith(CustomerProfileRequest customerProfileList){
       enhanceRelation(Merchant.CUSTOMER_PROFILE_LIST_PROPERTY, customerProfileList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectCustomerProfileListWith", null, Merchant.CUSTOMER_PROFILE_LIST_PROPERTY));
       return this;
    }
    public MerchantRequest<T> selectCustomerOrderList(){
       return selectCustomerOrderListWith(Q.customerOrders().selectSelf());
    }

    public MerchantRequest<T> selectCustomerOrderListWith(CustomerOrderRequest customerOrderList){
       enhanceRelation(Merchant.CUSTOMER_ORDER_LIST_PROPERTY, customerOrderList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectCustomerOrderListWith", null, Merchant.CUSTOMER_ORDER_LIST_PROPERTY));
       return this;
    }

    public MerchantRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Merchant.ID_PROPERTY, operator, values);
    }

    public MerchantRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public MerchantRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public MerchantRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public MerchantRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public MerchantRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public MerchantRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public MerchantRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public MerchantRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Merchant.NAME_PROPERTY, operator, values);
    }

    public MerchantRequest<T> withNameIsNot(String name){
       return withName(Operator.NOT_EQUAL, name);
    }

    public MerchantRequest<T> withNameIn(String... name){
       return withName(Operator.IN, (Object[])name);
    }

    public MerchantRequest<T> withNameNotIn(String... name){
       return withName(Operator.NOT_IN, (Object[])name);
    }
    public MerchantRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public MerchantRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public MerchantRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public MerchantRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public MerchantRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public MerchantRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public MerchantRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public MerchantRequest<T> withNameNotContaining(String name){
       return withName(Operator.NOT_CONTAIN, name);
    }

    public MerchantRequest<T> withNameNotStartingWith(String name){
       return withName(Operator.NOT_BEGIN_WITH, name);
    }

    public MerchantRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public MerchantRequest<T> withNameNotEndingWith(String name){
       return withName(Operator.NOT_END_WITH, name);
    }

    public MerchantRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public MerchantRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public MerchantRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public MerchantRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public MerchantRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public MerchantRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Merchant.VERSION_PROPERTY, operator, values);
    }

    public MerchantRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public MerchantRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public MerchantRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public MerchantRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public MerchantRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public MerchantRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public MerchantRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public MerchantRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public MerchantRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public MerchantRequest<T> withCustomerProfileListMatching(CustomerProfileRequest customerProfileRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(Merchant.ID_PROPERTY, customerProfileRequest, CustomerProfile.MERCHANT_PROPERTY));
    }

    public MerchantRequest<T> withoutCustomerProfileListMatching(CustomerProfileRequest customerProfileRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(Merchant.ID_PROPERTY, customerProfileRequest, CustomerProfile.MERCHANT_PROPERTY)));
    }

    public MerchantRequest<T> haveCustomerProfiles(){
        return withCustomerProfileListMatching(Q.customerProfiles().unlimited());
    }

    public MerchantRequest<T> haveNoCustomerProfiles(){
        return withoutCustomerProfileListMatching(Q.customerProfiles().unlimited());
    }
    public MerchantRequest<T> withCustomerOrderListMatching(CustomerOrderRequest customerOrderRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(Merchant.ID_PROPERTY, customerOrderRequest, CustomerOrder.MERCHANT_PROPERTY));
    }

    public MerchantRequest<T> withoutCustomerOrderListMatching(CustomerOrderRequest customerOrderRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(Merchant.ID_PROPERTY, customerOrderRequest, CustomerOrder.MERCHANT_PROPERTY)));
    }

    public MerchantRequest<T> haveCustomerOrders(){
        return withCustomerOrderListMatching(Q.customerOrders().unlimited());
    }

    public MerchantRequest<T> haveNoCustomerOrders(){
        return withoutCustomerOrderListMatching(Q.customerOrders().unlimited());
    }

    public MerchantRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public MerchantRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public MerchantRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public MerchantRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, Merchant.ID_PROPERTY);
        return this;
    }
    public MerchantRequest<T> countDistinctName(){
        return countDistinctNameAs("name_distinct_count");
    }

    public MerchantRequest<T> countDistinctNameAs(String alias){
        super.countDistinctProperty(alias, Merchant.NAME_PROPERTY);
        return this;
    }
    public MerchantRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public MerchantRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, Merchant.VERSION_PROPERTY);
        return this;
    }
    public MerchantRequest<T> groupByCustomerProfilesWithDetails(CustomerProfileRequest subRequest){
       aggregate(Merchant.CUSTOMER_PROFILE_LIST_PROPERTY, subRequest);
       return this;
    }
    public MerchantRequest<T> groupByCustomerOrdersWithDetails(CustomerOrderRequest subRequest){
       aggregate(Merchant.CUSTOMER_ORDER_LIST_PROPERTY, subRequest);
       return this;
    }

    public MerchantRequest<T> groupById(){
       groupBy(Merchant.ID_PROPERTY);
       return this;
    }

    public MerchantRequest<T> groupByIdAs(String retName){
       groupBy(retName, Merchant.ID_PROPERTY);
       return this;
    }

    public MerchantRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, Merchant.ID_PROPERTY, function);
       return this;
    }

    public MerchantRequest<T> groupByName(){
       groupBy(Merchant.NAME_PROPERTY);
       return this;
    }

    public MerchantRequest<T> groupByNameAs(String retName){
       groupBy(retName, Merchant.NAME_PROPERTY);
       return this;
    }

    public MerchantRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, Merchant.NAME_PROPERTY, function);
       return this;
    }

    public MerchantRequest<T> groupByVersion(){
       groupBy(Merchant.VERSION_PROPERTY);
       return this;
    }

    public MerchantRequest<T> groupByVersionAs(String retName){
       groupBy(retName, Merchant.VERSION_PROPERTY);
       return this;
    }

    public MerchantRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, Merchant.VERSION_PROPERTY, function);
       return this;
    }



    public MerchantRequest<T> orderByIdAscending(){
       addOrderByAscending(Merchant.ID_PROPERTY);
       return this;
    }

    public MerchantRequest<T> orderByIdDescending(){
       addOrderByDescending(Merchant.ID_PROPERTY);
       return this;
    }

    public MerchantRequest<T> orderByNameAscending(){
       addOrderByAscending(Merchant.NAME_PROPERTY);
       return this;
    }

    public MerchantRequest<T> orderByNameDescending(){
       addOrderByDescending(Merchant.NAME_PROPERTY);
       return this;
    }
    public MerchantRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Merchant.NAME_PROPERTY);
       return this;
    }

    public MerchantRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Merchant.NAME_PROPERTY);
       return this;
    }
    public MerchantRequest<T> orderByVersionAscending(){
       addOrderByAscending(Merchant.VERSION_PROPERTY);
       return this;
    }

    public MerchantRequest<T> orderByVersionDescending(){
       addOrderByDescending(Merchant.VERSION_PROPERTY);
       return this;
    }


    public MerchantRequest<T> statsFromCustomerProfilesAs(String name, CustomerProfileRequest subRequest){
       return statsFromCustomerProfilesAs(name, subRequest, false);
    }

    public MerchantRequest<T> statsFromCustomerProfilesAs(String name, CustomerProfileRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, CustomerProfile.MERCHANT_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromCustomerProfilesAs", name, Merchant.CUSTOMER_PROFILE_LIST_PROPERTY));
       return this;
    }

    public MerchantRequest<T> statsFromCustomerProfiles(CustomerProfileRequest subRequest){
       return statsFromCustomerProfilesAs(REFINEMENTS, subRequest);
    }
    public MerchantRequest<T> statsFromCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
       return statsFromCustomerOrdersAs(name, subRequest, false);
    }

    public MerchantRequest<T> statsFromCustomerOrdersAs(String name, CustomerOrderRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, CustomerOrder.MERCHANT_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromCustomerOrdersAs", name, Merchant.CUSTOMER_ORDER_LIST_PROPERTY));
       return this;
    }

    public MerchantRequest<T> statsFromCustomerOrders(CustomerOrderRequest subRequest){
       return statsFromCustomerOrdersAs(REFINEMENTS, subRequest);
    }
    public MerchantRequest<T> countCustomerProfiles(){
        addRelationCount("Count", Q.customerProfiles().unlimited(), CustomerProfile.MERCHANT_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomerProfiles", null, Merchant.CUSTOMER_PROFILE_LIST_PROPERTY));
        return this;
    }

    public MerchantRequest<T> countCustomerProfilesAs(String name){
        addRelationCount(name, Q.customerProfiles().unlimited(), CustomerProfile.MERCHANT_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomerProfilesAs", name, Merchant.CUSTOMER_PROFILE_LIST_PROPERTY));
        return this;
    }

    public MerchantRequest<T> countCustomerProfilesWith(String name, CustomerProfileRequest subRequest){
        addRelationCount(name, subRequest, CustomerProfile.MERCHANT_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomerProfilesWith", name, Merchant.CUSTOMER_PROFILE_LIST_PROPERTY));
        return this;
    }
    public MerchantRequest<T> countCustomerOrders(){
        addRelationCount("Count", Q.customerOrders().unlimited(), CustomerOrder.MERCHANT_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomerOrders", null, Merchant.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public MerchantRequest<T> countCustomerOrdersAs(String name){
        addRelationCount(name, Q.customerOrders().unlimited(), CustomerOrder.MERCHANT_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomerOrdersAs", name, Merchant.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }

    public MerchantRequest<T> countCustomerOrdersWith(String name, CustomerOrderRequest subRequest){
        addRelationCount(name, subRequest, CustomerOrder.MERCHANT_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countCustomerOrdersWith", name, Merchant.CUSTOMER_ORDER_LIST_PROPERTY));
        return this;
    }



    /**
     * get topN records
     * @param topN  records number
     */
    public MerchantRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public MerchantRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public MerchantRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public MerchantRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public MerchantRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public MerchantRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}