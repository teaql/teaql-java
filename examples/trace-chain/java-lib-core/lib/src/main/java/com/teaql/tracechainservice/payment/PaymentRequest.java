
package com.teaql.tracechainservice.payment;

import com.teaql.tracechainservice.Q;
import com.teaql.tracechainservice.customerorder.CustomerOrder;
import com.teaql.tracechainservice.customerorder.CustomerOrderRequest;
import com.teaql.tracechainservice.paymentattempt.PaymentAttempt;
import com.teaql.tracechainservice.paymentattempt.PaymentAttemptRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.util.Objects;

public class PaymentRequest<T extends Payment> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public PaymentRequest(Class<T> returnType){
        this(returnType, () -> (T) new Payment());
    }

    protected PaymentRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public PaymentRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public PaymentRequest<T> copy(){
        return (PaymentRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new PaymentRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public PaymentRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public PaymentRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public PaymentRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public PaymentRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public PaymentRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public PaymentRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public PaymentRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public PaymentRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public PaymentRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public PaymentRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (PaymentRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public PaymentRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public PaymentRequest<T> matchingAnyOf(PaymentRequest payment){
        super.internalMatchAny(payment);
        return this;
    }

    public PaymentRequest<T> matchingIdsOf(PaymentRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                Payment.ID_PROPERTY, request, Payment.ID_PROPERTY));
    }

    public PaymentRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public PaymentRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public PaymentRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public PaymentRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectCustomerOrderIdOnly().selectReferenceCode().selectVersion();
    }

    public PaymentRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public PaymentRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(Payment.ID_PROPERTY);
        selectProperty(Payment.CUSTOMER_ORDER_PROPERTY);
        selectProperty(Payment.REFERENCE_CODE_PROPERTY);
        selectProperty(Payment.VERSION_PROPERTY);
        return this;
    }

    public PaymentRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectCustomerOrder().selectReferenceCode().selectVersion();
    }

    public PaymentRequest<T> selectChildren(){
        super.selectAny();
        selectPaymentAttemptList();
        return selectId().selectCustomerOrder().selectReferenceCode().selectVersion();
    }


    public PaymentRequest<T> selectId(){
       selectProperty(Payment.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PaymentRequest<T> unselectId(){
       unselectProperty(Payment.ID_PROPERTY);
       return this;
    }
    public PaymentRequest<T> selectCustomerOrderIdOnly(){
       selectProperty(Payment.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public PaymentRequest<T> selectCustomerOrder(){
        return selectCustomerOrderWith(Q.customerOrders().unlimited().selectSelf());
    }

    public PaymentRequest<T> selectCustomerOrderWith(CustomerOrderRequest customerOrder){
       selectProperty(Payment.CUSTOMER_ORDER_PROPERTY);
       enhanceRelation(Payment.CUSTOMER_ORDER_PROPERTY, customerOrder,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectCustomerOrderWith", null, Payment.CUSTOMER_ORDER_PROPERTY));
       return this;
    }

    public PaymentRequest<T> unselectCustomerOrder(){
       unselectProperty(Payment.CUSTOMER_ORDER_PROPERTY);
       return this;
    }
    public PaymentRequest<T> selectReferenceCode(){
       selectProperty(Payment.REFERENCE_CODE_PROPERTY);
       return this;
    }

    /**
     * fill the referenceCode with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  referenceCode) to fetch referenceCode property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PaymentRequest<T> unselectReferenceCode(){
       unselectProperty(Payment.REFERENCE_CODE_PROPERTY);
       return this;
    }
    public PaymentRequest<T> selectVersion(){
       selectProperty(Payment.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PaymentRequest<T> unselectVersion(){
       unselectProperty(Payment.VERSION_PROPERTY);
       return this;
    }
    public PaymentRequest<T> selectPaymentAttemptList(){
       return selectPaymentAttemptListWith(Q.paymentAttempts().selectSelf());
    }

    public PaymentRequest<T> selectPaymentAttemptListWith(PaymentAttemptRequest paymentAttemptList){
       enhanceRelation(Payment.PAYMENT_ATTEMPT_LIST_PROPERTY, paymentAttemptList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectPaymentAttemptListWith", null, Payment.PAYMENT_ATTEMPT_LIST_PROPERTY));
       return this;
    }

    public PaymentRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Payment.ID_PROPERTY, operator, values);
    }

    public PaymentRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public PaymentRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public PaymentRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public PaymentRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public PaymentRequest<T> filterByCustomerOrder(CustomerOrder... customerOrder){
      if (customerOrder == null || customerOrder.length == 0) {
        throw new IllegalArgumentException("filterByCustomerOrder parameter customerOrder cannot be empty");
      }
      return appendSearchCriteria(createCustomerOrderCriteria(Operator.EQUAL, (Object[])customerOrder));
    }

    public PaymentRequest<T> withCustomerOrder(Operator operator, Object... values){
       return appendSearchCriteria(createCustomerOrderCriteria(operator, values));
    }

    public PaymentRequest<T> withCustomerOrderIsUnknown(){
       return withCustomerOrder(Operator.IS_NULL);
    }

    public PaymentRequest<T> withCustomerOrderIsKnown(){
       return withCustomerOrder(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCustomerOrderCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Payment.CUSTOMER_ORDER_PROPERTY, operator, values);
    }

    public PaymentRequest<T> filterByCustomerOrder(Long customerOrder){
      if(customerOrder == null){
         return this;
      }
      return withCustomerOrder(Operator.EQUAL, customerOrder);
    }
    public PaymentRequest<T> withCustomerOrderMatching(CustomerOrderRequest customerOrder){
       return appendSearchCriteria(new SubQuerySearchCriteria(Payment.CUSTOMER_ORDER_PROPERTY, customerOrder, CustomerOrder.ID_PROPERTY));
    }

    public PaymentRequest<T> withoutCustomerOrderMatching(CustomerOrderRequest customerOrder){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(Payment.CUSTOMER_ORDER_PROPERTY, customerOrder, CustomerOrder.ID_PROPERTY)));
    }

    public PaymentRequest<T> filterByReferenceCode(String... referenceCode){
      if (referenceCode == null || referenceCode.length == 0) {
        throw new IllegalArgumentException("filterByReferenceCode parameter referenceCode cannot be empty");
      }
      return appendSearchCriteria(createReferenceCodeCriteria(Operator.EQUAL, (Object[])referenceCode));
    }

    public PaymentRequest<T> withReferenceCode(Operator operator, Object... values){
       return appendSearchCriteria(createReferenceCodeCriteria(operator, values));
    }

    public PaymentRequest<T> withReferenceCodeIsUnknown(){
       return withReferenceCode(Operator.IS_NULL);
    }

    public PaymentRequest<T> withReferenceCodeIsKnown(){
       return withReferenceCode(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createReferenceCodeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Payment.REFERENCE_CODE_PROPERTY, operator, values);
    }

    public PaymentRequest<T> withReferenceCodeIsNot(String referenceCode){
       return withReferenceCode(Operator.NOT_EQUAL, referenceCode);
    }

    public PaymentRequest<T> withReferenceCodeIn(String... referenceCode){
       return withReferenceCode(Operator.IN, (Object[])referenceCode);
    }

    public PaymentRequest<T> withReferenceCodeNotIn(String... referenceCode){
       return withReferenceCode(Operator.NOT_IN, (Object[])referenceCode);
    }
    public PaymentRequest<T> withReferenceCodeGreaterThan(String referenceCode){
       return withReferenceCode(Operator.GREATER_THAN, referenceCode);
    }

    public PaymentRequest<T> withReferenceCodeGreaterThanOrEqualTo(String referenceCode){
       return withReferenceCode(Operator.GREATER_THAN_OR_EQUAL, referenceCode);
    }

    public PaymentRequest<T> withReferenceCodeLessThan(String referenceCode){
       return withReferenceCode(Operator.LESS_THAN, referenceCode);
    }

    public PaymentRequest<T> withReferenceCodeLessThanOrEqualTo(String referenceCode){
       return withReferenceCode(Operator.LESS_THAN_OR_EQUAL, referenceCode);
    }

    public PaymentRequest<T> withReferenceCodeBetween(String startOfReferenceCode, String endOfReferenceCode){
       return withReferenceCode(Operator.BETWEEN, startOfReferenceCode, endOfReferenceCode);
    }
    public PaymentRequest<T> withReferenceCodeStartingWith(String referenceCode){
       return withReferenceCode(Operator.BEGIN_WITH, referenceCode);
    }
    public PaymentRequest<T> withReferenceCodeContaining(String referenceCode){
       return withReferenceCode(Operator.CONTAIN, referenceCode);
    }

    public PaymentRequest<T> withReferenceCodeNotContaining(String referenceCode){
       return withReferenceCode(Operator.NOT_CONTAIN, referenceCode);
    }

    public PaymentRequest<T> withReferenceCodeNotStartingWith(String referenceCode){
       return withReferenceCode(Operator.NOT_BEGIN_WITH, referenceCode);
    }

    public PaymentRequest<T> withReferenceCodeEndingWith(String referenceCode){
       return withReferenceCode(Operator.END_WITH, referenceCode);
    }

    public PaymentRequest<T> withReferenceCodeNotEndingWith(String referenceCode){
       return withReferenceCode(Operator.NOT_END_WITH, referenceCode);
    }

    public PaymentRequest<T> withReferenceCodeIs(String referenceCode){
       return withReferenceCode(Operator.EQUAL, referenceCode);
    }

    public PaymentRequest<T> withReferenceCodeSoundingLike(String referenceCode){
       return withReferenceCode(Operator.SOUNDS_LIKE, referenceCode);
    }



    public PaymentRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public PaymentRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public PaymentRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public PaymentRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Payment.VERSION_PROPERTY, operator, values);
    }

    public PaymentRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public PaymentRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public PaymentRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public PaymentRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public PaymentRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public PaymentRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public PaymentRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public PaymentRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public PaymentRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public PaymentRequest<T> withPaymentAttemptListMatching(PaymentAttemptRequest paymentAttemptRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(Payment.ID_PROPERTY, paymentAttemptRequest, PaymentAttempt.PAYMENT_PROPERTY));
    }

    public PaymentRequest<T> withoutPaymentAttemptListMatching(PaymentAttemptRequest paymentAttemptRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(Payment.ID_PROPERTY, paymentAttemptRequest, PaymentAttempt.PAYMENT_PROPERTY)));
    }

    public PaymentRequest<T> havePaymentAttempts(){
        return withPaymentAttemptListMatching(Q.paymentAttempts().unlimited());
    }

    public PaymentRequest<T> haveNoPaymentAttempts(){
        return withoutPaymentAttemptListMatching(Q.paymentAttempts().unlimited());
    }

    public PaymentRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public PaymentRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public PaymentRequest<T> groupByCustomerOrderWithDetails(){
       return groupByCustomerOrderWithDetails(Q.customerOrders().unlimited());
    }

    public PaymentRequest<T> groupByCustomerOrderWithDetails(CustomerOrderRequest subRequest){
       aggregate(Payment.CUSTOMER_ORDER_PROPERTY, subRequest);
       return this;
    }



    public PaymentRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public PaymentRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, Payment.ID_PROPERTY);
        return this;
    }
    public PaymentRequest<T> countDistinctCustomerOrder(){
        return countDistinctCustomerOrderAs("customer_order_distinct_count");
    }

    public PaymentRequest<T> countDistinctCustomerOrderAs(String alias){
        super.countDistinctProperty(alias, Payment.CUSTOMER_ORDER_PROPERTY);
        return this;
    }
    public PaymentRequest<T> countDistinctReferenceCode(){
        return countDistinctReferenceCodeAs("reference_code_distinct_count");
    }

    public PaymentRequest<T> countDistinctReferenceCodeAs(String alias){
        super.countDistinctProperty(alias, Payment.REFERENCE_CODE_PROPERTY);
        return this;
    }
    public PaymentRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public PaymentRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, Payment.VERSION_PROPERTY);
        return this;
    }
    public PaymentRequest<T> groupByPaymentAttemptsWithDetails(PaymentAttemptRequest subRequest){
       aggregate(Payment.PAYMENT_ATTEMPT_LIST_PROPERTY, subRequest);
       return this;
    }

    public PaymentRequest<T> groupById(){
       groupBy(Payment.ID_PROPERTY);
       return this;
    }

    public PaymentRequest<T> groupByIdAs(String retName){
       groupBy(retName, Payment.ID_PROPERTY);
       return this;
    }

    public PaymentRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, Payment.ID_PROPERTY, function);
       return this;
    }
    public PaymentRequest<T> groupByCustomerOrderWith(CustomerOrderRequest subRequest){
       groupBy(Payment.CUSTOMER_ORDER_PROPERTY, subRequest,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "groupByCustomerOrderWith", null, "customer_order"));
       return this;
    }
    public PaymentRequest<T> groupByCustomerOrder(){
       groupBy(Payment.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public PaymentRequest<T> groupByCustomerOrderAs(String retName){
       groupBy(retName, Payment.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public PaymentRequest<T> groupByCustomerOrderWithFunction(String retName, AggrFunction function){
       groupBy(retName, Payment.CUSTOMER_ORDER_PROPERTY, function);
       return this;
    }

    public PaymentRequest<T> groupByReferenceCode(){
       groupBy(Payment.REFERENCE_CODE_PROPERTY);
       return this;
    }

    public PaymentRequest<T> groupByReferenceCodeAs(String retName){
       groupBy(retName, Payment.REFERENCE_CODE_PROPERTY);
       return this;
    }

    public PaymentRequest<T> groupByReferenceCodeWithFunction(String retName, AggrFunction function){
       groupBy(retName, Payment.REFERENCE_CODE_PROPERTY, function);
       return this;
    }

    public PaymentRequest<T> groupByVersion(){
       groupBy(Payment.VERSION_PROPERTY);
       return this;
    }

    public PaymentRequest<T> groupByVersionAs(String retName){
       groupBy(retName, Payment.VERSION_PROPERTY);
       return this;
    }

    public PaymentRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, Payment.VERSION_PROPERTY, function);
       return this;
    }



    public PaymentRequest<T> orderByIdAscending(){
       addOrderByAscending(Payment.ID_PROPERTY);
       return this;
    }

    public PaymentRequest<T> orderByIdDescending(){
       addOrderByDescending(Payment.ID_PROPERTY);
       return this;
    }

    public PaymentRequest<T> orderByCustomerOrderAscending(){
       addOrderByAscending(Payment.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public PaymentRequest<T> orderByCustomerOrderDescending(){
       addOrderByDescending(Payment.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public PaymentRequest<T> orderByReferenceCodeAscending(){
       addOrderByAscending(Payment.REFERENCE_CODE_PROPERTY);
       return this;
    }

    public PaymentRequest<T> orderByReferenceCodeDescending(){
       addOrderByDescending(Payment.REFERENCE_CODE_PROPERTY);
       return this;
    }
    public PaymentRequest<T> orderByReferenceCodeAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Payment.REFERENCE_CODE_PROPERTY);
       return this;
    }

    public PaymentRequest<T> orderByReferenceCodeDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Payment.REFERENCE_CODE_PROPERTY);
       return this;
    }
    public PaymentRequest<T> orderByVersionAscending(){
       addOrderByAscending(Payment.VERSION_PROPERTY);
       return this;
    }

    public PaymentRequest<T> orderByVersionDescending(){
       addOrderByDescending(Payment.VERSION_PROPERTY);
       return this;
    }


    public PaymentRequest<T> statsFromPaymentAttemptsAs(String name, PaymentAttemptRequest subRequest){
       return statsFromPaymentAttemptsAs(name, subRequest, false);
    }

    public PaymentRequest<T> statsFromPaymentAttemptsAs(String name, PaymentAttemptRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, PaymentAttempt.PAYMENT_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromPaymentAttemptsAs", name, Payment.PAYMENT_ATTEMPT_LIST_PROPERTY));
       return this;
    }

    public PaymentRequest<T> statsFromPaymentAttempts(PaymentAttemptRequest subRequest){
       return statsFromPaymentAttemptsAs(REFINEMENTS, subRequest);
    }
    public CustomerOrderRequest rollUpToCustomerOrder(){
       CustomerOrderRequest customerOrder = Q.customerOrders().unlimited();
       this.internalRollUpTo("customer_order", customerOrder,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "rollUpToCustomerOrder", null, "customer_order"));
       return customerOrder;
    }



    public PaymentRequest<T> countPaymentAttempts(){
        addRelationCount("Count", Q.paymentAttempts().unlimited(), PaymentAttempt.PAYMENT_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countPaymentAttempts", null, Payment.PAYMENT_ATTEMPT_LIST_PROPERTY));
        return this;
    }

    public PaymentRequest<T> countPaymentAttemptsAs(String name){
        addRelationCount(name, Q.paymentAttempts().unlimited(), PaymentAttempt.PAYMENT_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countPaymentAttemptsAs", name, Payment.PAYMENT_ATTEMPT_LIST_PROPERTY));
        return this;
    }

    public PaymentRequest<T> countPaymentAttemptsWith(String name, PaymentAttemptRequest subRequest){
        addRelationCount(name, subRequest, PaymentAttempt.PAYMENT_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countPaymentAttemptsWith", name, Payment.PAYMENT_ATTEMPT_LIST_PROPERTY));
        return this;
    }

   public PaymentRequest<T> facetByCustomerOrderAs(String facetName, CustomerOrderRequest customerOrder){
       return facetByCustomerOrderAs(facetName, customerOrder, true);
   }

   public PaymentRequest<T> facetByCustomerOrderAs(String facetName, CustomerOrderRequest customerOrder, boolean includeAllFacets){
       addFacet(facetName, Payment.CUSTOMER_ORDER_PROPERTY, customerOrder, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByCustomerOrderAs", facetName, Payment.CUSTOMER_ORDER_PROPERTY));
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public PaymentRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public PaymentRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public PaymentRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public PaymentRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public PaymentRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public PaymentRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}