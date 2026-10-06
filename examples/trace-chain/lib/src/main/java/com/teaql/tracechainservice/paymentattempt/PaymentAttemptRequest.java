
package com.teaql.tracechainservice.paymentattempt;

import com.teaql.tracechainservice.Q;
import com.teaql.tracechainservice.payment.Payment;
import com.teaql.tracechainservice.payment.PaymentRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;

public class PaymentAttemptRequest<T extends PaymentAttempt> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public PaymentAttemptRequest(Class<T> returnType){
        super(returnType, () -> (T) new PaymentAttempt());
        selectId();
        selectVersion();
    }

    public PaymentAttemptRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public PaymentAttemptRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public PaymentAttemptRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public PaymentAttemptRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public PaymentAttemptRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public PaymentAttemptRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public PaymentAttemptRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public PaymentAttemptRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public PaymentAttemptRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public PaymentAttemptRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public PaymentAttemptRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (PaymentAttemptRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public PaymentAttemptRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public PaymentAttemptRequest<T> matchingAnyOf(PaymentAttemptRequest paymentAttempt){
        super.internalMatchAny(paymentAttempt);
        return this;
    }

    public PaymentAttemptRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public PaymentAttemptRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public PaymentAttemptRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public PaymentAttemptRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectPaymentIdOnly().selectReferenceCode().selectVersion();
    }

    public PaymentAttemptRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public PaymentAttemptRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectPayment().selectReferenceCode().selectVersion();
    }

    public PaymentAttemptRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectPayment().selectReferenceCode().selectVersion();
    }


    public PaymentAttemptRequest<T> selectId(){
       selectProperty(PaymentAttempt.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PaymentAttemptRequest<T> unselectId(){
       unselectProperty(PaymentAttempt.ID_PROPERTY);
       return this;
    }
    public PaymentAttemptRequest<T> selectPaymentIdOnly(){
       selectProperty(PaymentAttempt.PAYMENT_PROPERTY);
       return this;
    }

    public PaymentAttemptRequest<T> selectPayment(){
        return selectPaymentWith(Q.payments().unlimited().selectSelf());
    }

    public PaymentAttemptRequest<T> selectPaymentWith(PaymentRequest payment){
       selectProperty(PaymentAttempt.PAYMENT_PROPERTY);
       enhanceRelation(PaymentAttempt.PAYMENT_PROPERTY, payment);
       return this;
    }

    public PaymentAttemptRequest<T> unselectPayment(){
       unselectProperty(PaymentAttempt.PAYMENT_PROPERTY);
       return this;
    }
    public PaymentAttemptRequest<T> selectReferenceCode(){
       selectProperty(PaymentAttempt.REFERENCE_CODE_PROPERTY);
       return this;
    }

    /**
     * fill the referenceCode with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  referenceCode) to fetch referenceCode property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PaymentAttemptRequest<T> unselectReferenceCode(){
       unselectProperty(PaymentAttempt.REFERENCE_CODE_PROPERTY);
       return this;
    }
    public PaymentAttemptRequest<T> selectVersion(){
       selectProperty(PaymentAttempt.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PaymentAttemptRequest<T> unselectVersion(){
       unselectProperty(PaymentAttempt.VERSION_PROPERTY);
       return this;
    }

    public PaymentAttemptRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(PaymentAttempt.ID_PROPERTY, operator, values);
    }

    public PaymentAttemptRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public PaymentAttemptRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public PaymentAttemptRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public PaymentAttemptRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public PaymentAttemptRequest<T> filterByPayment(Payment... payment){
      if (payment == null || payment.length == 0) {
        throw new IllegalArgumentException("filterByPayment parameter payment cannot be empty");
      }
      return appendSearchCriteria(createPaymentCriteria(Operator.EQUAL, (Object[])payment));
    }

    public PaymentAttemptRequest<T> withPayment(Operator operator, Object... values){
       return appendSearchCriteria(createPaymentCriteria(operator, values));
    }

    public PaymentAttemptRequest<T> withPaymentIsUnknown(){
       return withPayment(Operator.IS_NULL);
    }

    public PaymentAttemptRequest<T> withPaymentIsKnown(){
       return withPayment(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createPaymentCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(PaymentAttempt.PAYMENT_PROPERTY, operator, values);
    }

    public PaymentAttemptRequest<T> filterByPayment(Long payment){
      if(payment == null){
         return this;
      }
      return withPayment(Operator.EQUAL, payment);
    }
    public PaymentAttemptRequest<T> withPaymentMatching(PaymentRequest payment){
       return appendSearchCriteria(new SubQuerySearchCriteria(PaymentAttempt.PAYMENT_PROPERTY, payment, Payment.ID_PROPERTY));
    }

    public PaymentAttemptRequest<T> withoutPaymentMatching(PaymentRequest payment){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(PaymentAttempt.PAYMENT_PROPERTY, payment, Payment.ID_PROPERTY)));
    }

    public PaymentAttemptRequest<T> filterByReferenceCode(String... referenceCode){
      if (referenceCode == null || referenceCode.length == 0) {
        throw new IllegalArgumentException("filterByReferenceCode parameter referenceCode cannot be empty");
      }
      return appendSearchCriteria(createReferenceCodeCriteria(Operator.EQUAL, (Object[])referenceCode));
    }

    public PaymentAttemptRequest<T> withReferenceCode(Operator operator, Object... values){
       return appendSearchCriteria(createReferenceCodeCriteria(operator, values));
    }

    public PaymentAttemptRequest<T> withReferenceCodeIsUnknown(){
       return withReferenceCode(Operator.IS_NULL);
    }

    public PaymentAttemptRequest<T> withReferenceCodeIsKnown(){
       return withReferenceCode(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createReferenceCodeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(PaymentAttempt.REFERENCE_CODE_PROPERTY, operator, values);
    }

    public PaymentAttemptRequest<T> withReferenceCodeIsNot(String referenceCode){
       return withReferenceCode(Operator.NOT_EQUAL, referenceCode);
    }

    public PaymentAttemptRequest<T> withReferenceCodeIn(String... referenceCode){
       return withReferenceCode(Operator.IN, (Object[])referenceCode);
    }

    public PaymentAttemptRequest<T> withReferenceCodeNotIn(String... referenceCode){
       return withReferenceCode(Operator.NOT_IN, (Object[])referenceCode);
    }
    public PaymentAttemptRequest<T> withReferenceCodeGreaterThan(String referenceCode){
       return withReferenceCode(Operator.GREATER_THAN, referenceCode);
    }

    public PaymentAttemptRequest<T> withReferenceCodeGreaterThanOrEqualTo(String referenceCode){
       return withReferenceCode(Operator.GREATER_THAN_OR_EQUAL, referenceCode);
    }

    public PaymentAttemptRequest<T> withReferenceCodeLessThan(String referenceCode){
       return withReferenceCode(Operator.LESS_THAN, referenceCode);
    }

    public PaymentAttemptRequest<T> withReferenceCodeLessThanOrEqualTo(String referenceCode){
       return withReferenceCode(Operator.LESS_THAN_OR_EQUAL, referenceCode);
    }

    public PaymentAttemptRequest<T> withReferenceCodeBetween(String startOfReferenceCode, String endOfReferenceCode){
       return withReferenceCode(Operator.BETWEEN, startOfReferenceCode, endOfReferenceCode);
    }
    public PaymentAttemptRequest<T> withReferenceCodeStartingWith(String referenceCode){
       return withReferenceCode(Operator.BEGIN_WITH, referenceCode);
    }
    public PaymentAttemptRequest<T> withReferenceCodeContaining(String referenceCode){
       return withReferenceCode(Operator.CONTAIN, referenceCode);
    }

    public PaymentAttemptRequest<T> withReferenceCodeNotContaining(String referenceCode){
       return withReferenceCode(Operator.NOT_CONTAIN, referenceCode);
    }

    public PaymentAttemptRequest<T> withReferenceCodeNotStartingWith(String referenceCode){
       return withReferenceCode(Operator.NOT_BEGIN_WITH, referenceCode);
    }

    public PaymentAttemptRequest<T> withReferenceCodeEndingWith(String referenceCode){
       return withReferenceCode(Operator.END_WITH, referenceCode);
    }

    public PaymentAttemptRequest<T> withReferenceCodeNotEndingWith(String referenceCode){
       return withReferenceCode(Operator.NOT_END_WITH, referenceCode);
    }

    public PaymentAttemptRequest<T> withReferenceCodeIs(String referenceCode){
       return withReferenceCode(Operator.EQUAL, referenceCode);
    }

    public PaymentAttemptRequest<T> withReferenceCodeSoundingLike(String referenceCode){
       return withReferenceCode(Operator.SOUNDS_LIKE, referenceCode);
    }



    public PaymentAttemptRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public PaymentAttemptRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public PaymentAttemptRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public PaymentAttemptRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(PaymentAttempt.VERSION_PROPERTY, operator, values);
    }

    public PaymentAttemptRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public PaymentAttemptRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public PaymentAttemptRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public PaymentAttemptRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public PaymentAttemptRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public PaymentAttemptRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public PaymentAttemptRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public PaymentAttemptRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public PaymentAttemptRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }



    public PaymentAttemptRequest<T> count(){
        super.count();
        return this;
    }
    public PaymentAttemptRequest<T> countAs(String retName){
        super.count(retName);
        return this;
    }
    public PaymentAttemptRequest<T> groupByPaymentWithDetails(){
       return groupByPaymentWithDetails(Q.payments().unlimited());
    }

    public PaymentAttemptRequest<T> groupByPaymentWithDetails(PaymentRequest subRequest){
       aggregate(PaymentAttempt.PAYMENT_PROPERTY, subRequest);
       return this;
    }




    public PaymentAttemptRequest<T> groupById(){
       groupBy(PaymentAttempt.ID_PROPERTY);
       return this;
    }

    public PaymentAttemptRequest<T> groupByIdAs(String retName){
       groupBy(retName, PaymentAttempt.ID_PROPERTY);
       return this;
    }

    public PaymentAttemptRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, PaymentAttempt.ID_PROPERTY, function);
       return this;
    }
    public PaymentAttemptRequest<T> groupByPaymentWith(PaymentRequest subRequest){
       groupBy(PaymentAttempt.PAYMENT_PROPERTY, subRequest);
       return this;
    }
    public PaymentAttemptRequest<T> groupByPayment(){
       groupBy(PaymentAttempt.PAYMENT_PROPERTY);
       return this;
    }

    public PaymentAttemptRequest<T> groupByPaymentAs(String retName){
       groupBy(retName, PaymentAttempt.PAYMENT_PROPERTY);
       return this;
    }

    public PaymentAttemptRequest<T> groupByPaymentWithFunction(String retName, AggrFunction function){
       groupBy(retName, PaymentAttempt.PAYMENT_PROPERTY, function);
       return this;
    }

    public PaymentAttemptRequest<T> groupByReferenceCode(){
       groupBy(PaymentAttempt.REFERENCE_CODE_PROPERTY);
       return this;
    }

    public PaymentAttemptRequest<T> groupByReferenceCodeAs(String retName){
       groupBy(retName, PaymentAttempt.REFERENCE_CODE_PROPERTY);
       return this;
    }

    public PaymentAttemptRequest<T> groupByReferenceCodeWithFunction(String retName, AggrFunction function){
       groupBy(retName, PaymentAttempt.REFERENCE_CODE_PROPERTY, function);
       return this;
    }

    public PaymentAttemptRequest<T> groupByVersion(){
       groupBy(PaymentAttempt.VERSION_PROPERTY);
       return this;
    }

    public PaymentAttemptRequest<T> groupByVersionAs(String retName){
       groupBy(retName, PaymentAttempt.VERSION_PROPERTY);
       return this;
    }

    public PaymentAttemptRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, PaymentAttempt.VERSION_PROPERTY, function);
       return this;
    }



    public PaymentAttemptRequest<T> orderByIdAscending(){
       addOrderByAscending(PaymentAttempt.ID_PROPERTY);
       return this;
    }

    public PaymentAttemptRequest<T> orderByIdDescending(){
       addOrderByDescending(PaymentAttempt.ID_PROPERTY);
       return this;
    }

    public PaymentAttemptRequest<T> orderByPaymentAscending(){
       addOrderByAscending(PaymentAttempt.PAYMENT_PROPERTY);
       return this;
    }

    public PaymentAttemptRequest<T> orderByPaymentDescending(){
       addOrderByDescending(PaymentAttempt.PAYMENT_PROPERTY);
       return this;
    }

    public PaymentAttemptRequest<T> orderByReferenceCodeAscending(){
       addOrderByAscending(PaymentAttempt.REFERENCE_CODE_PROPERTY);
       return this;
    }

    public PaymentAttemptRequest<T> orderByReferenceCodeDescending(){
       addOrderByDescending(PaymentAttempt.REFERENCE_CODE_PROPERTY);
       return this;
    }
    public PaymentAttemptRequest<T> orderByReferenceCodeAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(PaymentAttempt.REFERENCE_CODE_PROPERTY);
       return this;
    }

    public PaymentAttemptRequest<T> orderByReferenceCodeDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(PaymentAttempt.REFERENCE_CODE_PROPERTY);
       return this;
    }
    public PaymentAttemptRequest<T> orderByVersionAscending(){
       addOrderByAscending(PaymentAttempt.VERSION_PROPERTY);
       return this;
    }

    public PaymentAttemptRequest<T> orderByVersionDescending(){
       addOrderByDescending(PaymentAttempt.VERSION_PROPERTY);
       return this;
    }


    public PaymentRequest rollUpToPayment(){
       PaymentRequest payment = Q.payments().unlimited();
       this.withPaymentMatching(payment)
           .groupByPaymentWith(payment);
       return payment;
    }




   public PaymentAttemptRequest<T> facetByPaymentAs(String facetName, PaymentRequest payment){
       return facetByPaymentAs(facetName, payment, true);
   }

   public PaymentAttemptRequest<T> facetByPaymentAs(String facetName, PaymentRequest payment, boolean includeAllFacets){
       addFacet(facetName, PaymentAttempt.PAYMENT_PROPERTY, payment, includeAllFacets);
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public PaymentAttemptRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public PaymentAttemptRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public PaymentAttemptRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public PaymentAttemptRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public PaymentAttemptRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public PaymentAttemptRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}