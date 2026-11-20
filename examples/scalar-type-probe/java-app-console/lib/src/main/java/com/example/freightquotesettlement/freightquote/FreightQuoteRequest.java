
package com.example.freightquotesettlement.freightquote;

import com.example.freightquotesettlement.Q;
import com.example.freightquotesettlement.platform.Platform;
import com.example.freightquotesettlement.platform.PlatformRequest;
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
import java.time.LocalTime;
import java.util.Date;
import java.util.Objects;

public class FreightQuoteRequest<T extends FreightQuote> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public FreightQuoteRequest(Class<T> returnType){
        this(returnType, () -> (T) new FreightQuote());
    }

    protected FreightQuoteRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public FreightQuoteRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public FreightQuoteRequest<T> copy(){
        return (FreightQuoteRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new FreightQuoteRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public FreightQuoteRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public FreightQuoteRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public FreightQuoteRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public FreightQuoteRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public FreightQuoteRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public FreightQuoteRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public FreightQuoteRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public FreightQuoteRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public FreightQuoteRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public FreightQuoteRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (FreightQuoteRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public FreightQuoteRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public FreightQuoteRequest<T> matchingAnyOf(FreightQuoteRequest freightQuote){
        super.internalMatchAny(freightQuote);
        return this;
    }

    public FreightQuoteRequest<T> matchingIdsOf(FreightQuoteRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                FreightQuote.ID_PROPERTY, request, FreightQuote.ID_PROPERTY));
    }

    public FreightQuoteRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public FreightQuoteRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public FreightQuoteRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public FreightQuoteRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectPlatformIdOnly().selectReference().selectAmount().selectShipmentNumber().selectPackageCount().selectDeliveryDate().selectPickupTime().selectQuotedAt().selectInstructions().selectVersion();
    }

    public FreightQuoteRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public FreightQuoteRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(FreightQuote.ID_PROPERTY);
        selectProperty(FreightQuote.PLATFORM_PROPERTY);
        selectProperty(FreightQuote.REFERENCE_PROPERTY);
        selectProperty(FreightQuote.AMOUNT_PROPERTY);
        selectProperty(FreightQuote.SHIPMENT_NUMBER_PROPERTY);
        selectProperty(FreightQuote.PACKAGE_COUNT_PROPERTY);
        selectProperty(FreightQuote.DELIVERY_DATE_PROPERTY);
        selectProperty(FreightQuote.PICKUP_TIME_PROPERTY);
        selectProperty(FreightQuote.QUOTED_AT_PROPERTY);
        selectProperty(FreightQuote.INSTRUCTIONS_PROPERTY);
        selectProperty(FreightQuote.VERSION_PROPERTY);
        return this;
    }

    public FreightQuoteRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectPlatform().selectReference().selectAmount().selectShipmentNumber().selectPackageCount().selectDeliveryDate().selectPickupTime().selectQuotedAt().selectInstructions().selectVersion();
    }

    public FreightQuoteRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectPlatform().selectReference().selectAmount().selectShipmentNumber().selectPackageCount().selectDeliveryDate().selectPickupTime().selectQuotedAt().selectInstructions().selectVersion();
    }


    public FreightQuoteRequest<T> selectId(){
       selectProperty(FreightQuote.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public FreightQuoteRequest<T> unselectId(){
       unselectProperty(FreightQuote.ID_PROPERTY);
       return this;
    }
    public FreightQuoteRequest<T> selectPlatformIdOnly(){
       selectProperty(FreightQuote.PLATFORM_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> selectPlatform(){
        return selectPlatformWith(Q.platforms().unlimited().selectSelf());
    }

    public FreightQuoteRequest<T> selectPlatformWith(PlatformRequest platform){
       selectProperty(FreightQuote.PLATFORM_PROPERTY);
       enhanceRelation(FreightQuote.PLATFORM_PROPERTY, platform,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectPlatformWith", null, FreightQuote.PLATFORM_PROPERTY));
       return this;
    }

    public FreightQuoteRequest<T> unselectPlatform(){
       unselectProperty(FreightQuote.PLATFORM_PROPERTY);
       return this;
    }
    public FreightQuoteRequest<T> selectReference(){
       selectProperty(FreightQuote.REFERENCE_PROPERTY);
       return this;
    }

    /**
     * fill the reference with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  reference) to fetch reference property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public FreightQuoteRequest<T> unselectReference(){
       unselectProperty(FreightQuote.REFERENCE_PROPERTY);
       return this;
    }
    public FreightQuoteRequest<T> selectAmount(){
       selectProperty(FreightQuote.AMOUNT_PROPERTY);
       return this;
    }

    /**
     * fill the amount with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  amount) to fetch amount property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the amount with customized aggrFunction, TEAQL uses ({aggrFunction}(amount) AS amount to fetch amount property.
     * @param aggrFunction  aggrFunction
     */
    public FreightQuoteRequest<T> selectAmount(AggrFunction aggrFunction){
       selectProperty(FreightQuote.AMOUNT_PROPERTY, aggrFunction);
       return this;
    }


    public FreightQuoteRequest<T> unselectAmount(){
       unselectProperty(FreightQuote.AMOUNT_PROPERTY);
       return this;
    }
    public FreightQuoteRequest<T> selectShipmentNumber(){
       selectProperty(FreightQuote.SHIPMENT_NUMBER_PROPERTY);
       return this;
    }

    /**
     * fill the shipmentNumber with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  shipmentNumber) to fetch shipmentNumber property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the shipmentNumber with customized aggrFunction, TEAQL uses ({aggrFunction}(shipmentNumber) AS shipmentNumber to fetch shipmentNumber property.
     * @param aggrFunction  aggrFunction
     */
    public FreightQuoteRequest<T> selectShipmentNumber(AggrFunction aggrFunction){
       selectProperty(FreightQuote.SHIPMENT_NUMBER_PROPERTY, aggrFunction);
       return this;
    }


    public FreightQuoteRequest<T> unselectShipmentNumber(){
       unselectProperty(FreightQuote.SHIPMENT_NUMBER_PROPERTY);
       return this;
    }
    public FreightQuoteRequest<T> selectPackageCount(){
       selectProperty(FreightQuote.PACKAGE_COUNT_PROPERTY);
       return this;
    }

    /**
     * fill the packageCount with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  packageCount) to fetch packageCount property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the packageCount with customized aggrFunction, TEAQL uses ({aggrFunction}(packageCount) AS packageCount to fetch packageCount property.
     * @param aggrFunction  aggrFunction
     */
    public FreightQuoteRequest<T> selectPackageCount(AggrFunction aggrFunction){
       selectProperty(FreightQuote.PACKAGE_COUNT_PROPERTY, aggrFunction);
       return this;
    }


    public FreightQuoteRequest<T> unselectPackageCount(){
       unselectProperty(FreightQuote.PACKAGE_COUNT_PROPERTY);
       return this;
    }
    public FreightQuoteRequest<T> selectDeliveryDate(){
       selectProperty(FreightQuote.DELIVERY_DATE_PROPERTY);
       return this;
    }

    /**
     * fill the deliveryDate with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  deliveryDate) to fetch deliveryDate property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public FreightQuoteRequest<T> unselectDeliveryDate(){
       unselectProperty(FreightQuote.DELIVERY_DATE_PROPERTY);
       return this;
    }
    public FreightQuoteRequest<T> selectPickupTime(){
       selectProperty(FreightQuote.PICKUP_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the pickupTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  pickupTime) to fetch pickupTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public FreightQuoteRequest<T> unselectPickupTime(){
       unselectProperty(FreightQuote.PICKUP_TIME_PROPERTY);
       return this;
    }
    public FreightQuoteRequest<T> selectQuotedAt(){
       selectProperty(FreightQuote.QUOTED_AT_PROPERTY);
       return this;
    }

    /**
     * fill the quotedAt with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  quotedAt) to fetch quotedAt property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public FreightQuoteRequest<T> unselectQuotedAt(){
       unselectProperty(FreightQuote.QUOTED_AT_PROPERTY);
       return this;
    }
    public FreightQuoteRequest<T> selectInstructions(){
       selectProperty(FreightQuote.INSTRUCTIONS_PROPERTY);
       return this;
    }

    /**
     * fill the instructions with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  instructions) to fetch instructions property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public FreightQuoteRequest<T> unselectInstructions(){
       unselectProperty(FreightQuote.INSTRUCTIONS_PROPERTY);
       return this;
    }
    public FreightQuoteRequest<T> selectVersion(){
       selectProperty(FreightQuote.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public FreightQuoteRequest<T> unselectVersion(){
       unselectProperty(FreightQuote.VERSION_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(FreightQuote.ID_PROPERTY, operator, values);
    }

    public FreightQuoteRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public FreightQuoteRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public FreightQuoteRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public FreightQuoteRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public FreightQuoteRequest<T> filterByPlatform(Platform... platform){
      if (platform == null || platform.length == 0) {
        throw new IllegalArgumentException("filterByPlatform parameter platform cannot be empty");
      }
      return appendSearchCriteria(createPlatformCriteria(Operator.EQUAL, (Object[])platform));
    }

    public FreightQuoteRequest<T> withPlatform(Operator operator, Object... values){
       return appendSearchCriteria(createPlatformCriteria(operator, values));
    }

    public FreightQuoteRequest<T> withPlatformIsUnknown(){
       return withPlatform(Operator.IS_NULL);
    }

    public FreightQuoteRequest<T> withPlatformIsKnown(){
       return withPlatform(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createPlatformCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(FreightQuote.PLATFORM_PROPERTY, operator, values);
    }

    public FreightQuoteRequest<T> filterByPlatform(Long platform){
      if(platform == null){
         return this;
      }
      return withPlatform(Operator.EQUAL, platform);
    }
    public FreightQuoteRequest<T> withPlatformMatching(PlatformRequest platform){
       return appendSearchCriteria(new SubQuerySearchCriteria(FreightQuote.PLATFORM_PROPERTY, platform, Platform.ID_PROPERTY));
    }

    public FreightQuoteRequest<T> withoutPlatformMatching(PlatformRequest platform){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(FreightQuote.PLATFORM_PROPERTY, platform, Platform.ID_PROPERTY)));
    }

    public FreightQuoteRequest<T> filterByReference(String... reference){
      if (reference == null || reference.length == 0) {
        throw new IllegalArgumentException("filterByReference parameter reference cannot be empty");
      }
      return appendSearchCriteria(createReferenceCriteria(Operator.EQUAL, (Object[])reference));
    }

    public FreightQuoteRequest<T> withReference(Operator operator, Object... values){
       return appendSearchCriteria(createReferenceCriteria(operator, values));
    }

    public FreightQuoteRequest<T> withReferenceIsUnknown(){
       return withReference(Operator.IS_NULL);
    }

    public FreightQuoteRequest<T> withReferenceIsKnown(){
       return withReference(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createReferenceCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(FreightQuote.REFERENCE_PROPERTY, operator, values);
    }

    public FreightQuoteRequest<T> withReferenceIsNot(String reference){
       return withReference(Operator.NOT_EQUAL, reference);
    }

    public FreightQuoteRequest<T> withReferenceIn(String... reference){
       return withReference(Operator.IN, (Object[])reference);
    }

    public FreightQuoteRequest<T> withReferenceNotIn(String... reference){
       return withReference(Operator.NOT_IN, (Object[])reference);
    }
    public FreightQuoteRequest<T> withReferenceGreaterThan(String reference){
       return withReference(Operator.GREATER_THAN, reference);
    }

    public FreightQuoteRequest<T> withReferenceGreaterThanOrEqualTo(String reference){
       return withReference(Operator.GREATER_THAN_OR_EQUAL, reference);
    }

    public FreightQuoteRequest<T> withReferenceLessThan(String reference){
       return withReference(Operator.LESS_THAN, reference);
    }

    public FreightQuoteRequest<T> withReferenceLessThanOrEqualTo(String reference){
       return withReference(Operator.LESS_THAN_OR_EQUAL, reference);
    }

    public FreightQuoteRequest<T> withReferenceBetween(String startOfReference, String endOfReference){
       return withReference(Operator.BETWEEN, startOfReference, endOfReference);
    }
    public FreightQuoteRequest<T> withReferenceStartingWith(String reference){
       return withReference(Operator.BEGIN_WITH, reference);
    }
    public FreightQuoteRequest<T> withReferenceContaining(String reference){
       return withReference(Operator.CONTAIN, reference);
    }

    public FreightQuoteRequest<T> withReferenceNotContaining(String reference){
       return withReference(Operator.NOT_CONTAIN, reference);
    }

    public FreightQuoteRequest<T> withReferenceNotStartingWith(String reference){
       return withReference(Operator.NOT_BEGIN_WITH, reference);
    }

    public FreightQuoteRequest<T> withReferenceEndingWith(String reference){
       return withReference(Operator.END_WITH, reference);
    }

    public FreightQuoteRequest<T> withReferenceNotEndingWith(String reference){
       return withReference(Operator.NOT_END_WITH, reference);
    }

    public FreightQuoteRequest<T> withReferenceIs(String reference){
       return withReference(Operator.EQUAL, reference);
    }

    public FreightQuoteRequest<T> withReferenceSoundingLike(String reference){
       return withReference(Operator.SOUNDS_LIKE, reference);
    }



    public FreightQuoteRequest<T> filterByAmount(BigDecimal... amount){
      if (amount == null || amount.length == 0) {
        throw new IllegalArgumentException("filterByAmount parameter amount cannot be empty");
      }
      return appendSearchCriteria(createAmountCriteria(Operator.EQUAL, (Object[])amount));
    }

    public FreightQuoteRequest<T> withAmount(Operator operator, Object... values){
       return appendSearchCriteria(createAmountCriteria(operator, values));
    }

    public FreightQuoteRequest<T> withAmountIsUnknown(){
       return withAmount(Operator.IS_NULL);
    }

    public FreightQuoteRequest<T> withAmountIsKnown(){
       return withAmount(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createAmountCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(FreightQuote.AMOUNT_PROPERTY, operator, values);
    }

    public FreightQuoteRequest<T> withAmountIs(BigDecimal amount){
       return withAmount(Operator.EQUAL, amount);
    }

    public FreightQuoteRequest<T> withAmountIsNot(BigDecimal amount){
       return withAmount(Operator.NOT_EQUAL, amount);
    }

    public FreightQuoteRequest<T> withAmountIn(BigDecimal... amount){
       return withAmount(Operator.IN, (Object[])amount);
    }

    public FreightQuoteRequest<T> withAmountNotIn(BigDecimal... amount){
       return withAmount(Operator.NOT_IN, (Object[])amount);
    }
    public FreightQuoteRequest<T> withAmountGreaterThan(BigDecimal amount){
       return withAmount(Operator.GREATER_THAN, amount);
    }

    public FreightQuoteRequest<T> withAmountGreaterThanOrEqualTo(BigDecimal amount){
       return withAmount(Operator.GREATER_THAN_OR_EQUAL, amount);
    }

    public FreightQuoteRequest<T> withAmountLessThan(BigDecimal amount){
       return withAmount(Operator.LESS_THAN, amount);
    }

    public FreightQuoteRequest<T> withAmountLessThanOrEqualTo(BigDecimal amount){
       return withAmount(Operator.LESS_THAN_OR_EQUAL, amount);
    }

    public FreightQuoteRequest<T> withAmountBetween(BigDecimal startOfAmount, BigDecimal endOfAmount){
       return withAmount(Operator.BETWEEN, startOfAmount, endOfAmount);
    }



    public FreightQuoteRequest<T> filterByShipmentNumber(Long... shipmentNumber){
      if (shipmentNumber == null || shipmentNumber.length == 0) {
        throw new IllegalArgumentException("filterByShipmentNumber parameter shipmentNumber cannot be empty");
      }
      return appendSearchCriteria(createShipmentNumberCriteria(Operator.EQUAL, (Object[])shipmentNumber));
    }

    public FreightQuoteRequest<T> withShipmentNumber(Operator operator, Object... values){
       return appendSearchCriteria(createShipmentNumberCriteria(operator, values));
    }

    public FreightQuoteRequest<T> withShipmentNumberIsUnknown(){
       return withShipmentNumber(Operator.IS_NULL);
    }

    public FreightQuoteRequest<T> withShipmentNumberIsKnown(){
       return withShipmentNumber(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createShipmentNumberCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(FreightQuote.SHIPMENT_NUMBER_PROPERTY, operator, values);
    }

    public FreightQuoteRequest<T> withShipmentNumberIs(Long shipmentNumber){
       return withShipmentNumber(Operator.EQUAL, shipmentNumber);
    }

    public FreightQuoteRequest<T> withShipmentNumberIsNot(Long shipmentNumber){
       return withShipmentNumber(Operator.NOT_EQUAL, shipmentNumber);
    }

    public FreightQuoteRequest<T> withShipmentNumberIn(Long... shipmentNumber){
       return withShipmentNumber(Operator.IN, (Object[])shipmentNumber);
    }

    public FreightQuoteRequest<T> withShipmentNumberNotIn(Long... shipmentNumber){
       return withShipmentNumber(Operator.NOT_IN, (Object[])shipmentNumber);
    }
    public FreightQuoteRequest<T> withShipmentNumberGreaterThan(Long shipmentNumber){
       return withShipmentNumber(Operator.GREATER_THAN, shipmentNumber);
    }

    public FreightQuoteRequest<T> withShipmentNumberGreaterThanOrEqualTo(Long shipmentNumber){
       return withShipmentNumber(Operator.GREATER_THAN_OR_EQUAL, shipmentNumber);
    }

    public FreightQuoteRequest<T> withShipmentNumberLessThan(Long shipmentNumber){
       return withShipmentNumber(Operator.LESS_THAN, shipmentNumber);
    }

    public FreightQuoteRequest<T> withShipmentNumberLessThanOrEqualTo(Long shipmentNumber){
       return withShipmentNumber(Operator.LESS_THAN_OR_EQUAL, shipmentNumber);
    }

    public FreightQuoteRequest<T> withShipmentNumberBetween(Long startOfShipmentNumber, Long endOfShipmentNumber){
       return withShipmentNumber(Operator.BETWEEN, startOfShipmentNumber, endOfShipmentNumber);
    }



    public FreightQuoteRequest<T> filterByPackageCount(Integer... packageCount){
      if (packageCount == null || packageCount.length == 0) {
        throw new IllegalArgumentException("filterByPackageCount parameter packageCount cannot be empty");
      }
      return appendSearchCriteria(createPackageCountCriteria(Operator.EQUAL, (Object[])packageCount));
    }

    public FreightQuoteRequest<T> withPackageCount(Operator operator, Object... values){
       return appendSearchCriteria(createPackageCountCriteria(operator, values));
    }

    public FreightQuoteRequest<T> withPackageCountIsUnknown(){
       return withPackageCount(Operator.IS_NULL);
    }

    public FreightQuoteRequest<T> withPackageCountIsKnown(){
       return withPackageCount(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createPackageCountCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(FreightQuote.PACKAGE_COUNT_PROPERTY, operator, values);
    }

    public FreightQuoteRequest<T> withPackageCountIs(Integer packageCount){
       return withPackageCount(Operator.EQUAL, packageCount);
    }

    public FreightQuoteRequest<T> withPackageCountIsNot(Integer packageCount){
       return withPackageCount(Operator.NOT_EQUAL, packageCount);
    }

    public FreightQuoteRequest<T> withPackageCountIn(Integer... packageCount){
       return withPackageCount(Operator.IN, (Object[])packageCount);
    }

    public FreightQuoteRequest<T> withPackageCountNotIn(Integer... packageCount){
       return withPackageCount(Operator.NOT_IN, (Object[])packageCount);
    }
    public FreightQuoteRequest<T> withPackageCountGreaterThan(Integer packageCount){
       return withPackageCount(Operator.GREATER_THAN, packageCount);
    }

    public FreightQuoteRequest<T> withPackageCountGreaterThanOrEqualTo(Integer packageCount){
       return withPackageCount(Operator.GREATER_THAN_OR_EQUAL, packageCount);
    }

    public FreightQuoteRequest<T> withPackageCountLessThan(Integer packageCount){
       return withPackageCount(Operator.LESS_THAN, packageCount);
    }

    public FreightQuoteRequest<T> withPackageCountLessThanOrEqualTo(Integer packageCount){
       return withPackageCount(Operator.LESS_THAN_OR_EQUAL, packageCount);
    }

    public FreightQuoteRequest<T> withPackageCountBetween(Integer startOfPackageCount, Integer endOfPackageCount){
       return withPackageCount(Operator.BETWEEN, startOfPackageCount, endOfPackageCount);
    }



    public FreightQuoteRequest<T> filterByDeliveryDate(LocalDate... deliveryDate){
      if (deliveryDate == null || deliveryDate.length == 0) {
        throw new IllegalArgumentException("filterByDeliveryDate parameter deliveryDate cannot be empty");
      }
      return appendSearchCriteria(createDeliveryDateCriteria(Operator.EQUAL, (Object[])deliveryDate));
    }

    public FreightQuoteRequest<T> withDeliveryDate(Operator operator, Object... values){
       return appendSearchCriteria(createDeliveryDateCriteria(operator, values));
    }

    public FreightQuoteRequest<T> withDeliveryDateIsUnknown(){
       return withDeliveryDate(Operator.IS_NULL);
    }

    public FreightQuoteRequest<T> withDeliveryDateIsKnown(){
       return withDeliveryDate(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createDeliveryDateCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(FreightQuote.DELIVERY_DATE_PROPERTY, operator, values);
    }

    public FreightQuoteRequest<T> withDeliveryDateIs(LocalDate deliveryDate){
       return withDeliveryDate(Operator.EQUAL, deliveryDate);
    }

    public FreightQuoteRequest<T> withDeliveryDateIsNot(LocalDate deliveryDate){
       return withDeliveryDate(Operator.NOT_EQUAL, deliveryDate);
    }

    public FreightQuoteRequest<T> withDeliveryDateIn(LocalDate... deliveryDate){
       return withDeliveryDate(Operator.IN, (Object[])deliveryDate);
    }

    public FreightQuoteRequest<T> withDeliveryDateNotIn(LocalDate... deliveryDate){
       return withDeliveryDate(Operator.NOT_IN, (Object[])deliveryDate);
    }
    public FreightQuoteRequest<T> withDeliveryDateGreaterThan(LocalDate deliveryDate){
       return withDeliveryDate(Operator.GREATER_THAN, deliveryDate);
    }

    public FreightQuoteRequest<T> withDeliveryDateGreaterThanOrEqualTo(LocalDate deliveryDate){
       return withDeliveryDate(Operator.GREATER_THAN_OR_EQUAL, deliveryDate);
    }

    public FreightQuoteRequest<T> withDeliveryDateLessThan(LocalDate deliveryDate){
       return withDeliveryDate(Operator.LESS_THAN, deliveryDate);
    }

    public FreightQuoteRequest<T> withDeliveryDateLessThanOrEqualTo(LocalDate deliveryDate){
       return withDeliveryDate(Operator.LESS_THAN_OR_EQUAL, deliveryDate);
    }

    public FreightQuoteRequest<T> withDeliveryDateBetween(LocalDate startOfDeliveryDate, LocalDate endOfDeliveryDate){
       return withDeliveryDate(Operator.BETWEEN, startOfDeliveryDate, endOfDeliveryDate);
    }
    public FreightQuoteRequest<T> withDeliveryDateBefore(LocalDate deliveryDate){
       return withDeliveryDate(Operator.LESS_THAN, deliveryDate);
    }

    public FreightQuoteRequest<T> withDeliveryDateBefore(Date deliveryDate){
       return withDeliveryDate(Operator.LESS_THAN, deliveryDate);
    }

    public FreightQuoteRequest<T> withDeliveryDateAfter(LocalDate deliveryDate){
       return withDeliveryDate(Operator.GREATER_THAN, deliveryDate);
    }

    public FreightQuoteRequest<T> withDeliveryDateAfter(Date deliveryDate){
       return withDeliveryDate(Operator.GREATER_THAN, deliveryDate);
    }

    public FreightQuoteRequest<T> withDeliveryDateBetween(Date startOfDeliveryDate, Date endOfDeliveryDate){
       return withDeliveryDate(Operator.BETWEEN, startOfDeliveryDate, endOfDeliveryDate);
    }




    public FreightQuoteRequest<T> filterByPickupTime(LocalTime... pickupTime){
      if (pickupTime == null || pickupTime.length == 0) {
        throw new IllegalArgumentException("filterByPickupTime parameter pickupTime cannot be empty");
      }
      return appendSearchCriteria(createPickupTimeCriteria(Operator.EQUAL, (Object[])pickupTime));
    }

    public FreightQuoteRequest<T> withPickupTime(Operator operator, Object... values){
       return appendSearchCriteria(createPickupTimeCriteria(operator, values));
    }

    public FreightQuoteRequest<T> withPickupTimeIsUnknown(){
       return withPickupTime(Operator.IS_NULL);
    }

    public FreightQuoteRequest<T> withPickupTimeIsKnown(){
       return withPickupTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createPickupTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(FreightQuote.PICKUP_TIME_PROPERTY, operator, values);
    }

    public FreightQuoteRequest<T> withPickupTimeIs(LocalTime pickupTime){
       return withPickupTime(Operator.EQUAL, pickupTime);
    }

    public FreightQuoteRequest<T> withPickupTimeIsNot(LocalTime pickupTime){
       return withPickupTime(Operator.NOT_EQUAL, pickupTime);
    }

    public FreightQuoteRequest<T> withPickupTimeIn(LocalTime... pickupTime){
       return withPickupTime(Operator.IN, (Object[])pickupTime);
    }

    public FreightQuoteRequest<T> withPickupTimeNotIn(LocalTime... pickupTime){
       return withPickupTime(Operator.NOT_IN, (Object[])pickupTime);
    }
    public FreightQuoteRequest<T> withPickupTimeGreaterThan(LocalTime pickupTime){
       return withPickupTime(Operator.GREATER_THAN, pickupTime);
    }

    public FreightQuoteRequest<T> withPickupTimeGreaterThanOrEqualTo(LocalTime pickupTime){
       return withPickupTime(Operator.GREATER_THAN_OR_EQUAL, pickupTime);
    }

    public FreightQuoteRequest<T> withPickupTimeLessThan(LocalTime pickupTime){
       return withPickupTime(Operator.LESS_THAN, pickupTime);
    }

    public FreightQuoteRequest<T> withPickupTimeLessThanOrEqualTo(LocalTime pickupTime){
       return withPickupTime(Operator.LESS_THAN_OR_EQUAL, pickupTime);
    }

    public FreightQuoteRequest<T> withPickupTimeBetween(LocalTime startOfPickupTime, LocalTime endOfPickupTime){
       return withPickupTime(Operator.BETWEEN, startOfPickupTime, endOfPickupTime);
    }


    public FreightQuoteRequest<T> filterByQuotedAt(LocalDateTime... quotedAt){
      if (quotedAt == null || quotedAt.length == 0) {
        throw new IllegalArgumentException("filterByQuotedAt parameter quotedAt cannot be empty");
      }
      return appendSearchCriteria(createQuotedAtCriteria(Operator.EQUAL, (Object[])quotedAt));
    }

    public FreightQuoteRequest<T> withQuotedAt(Operator operator, Object... values){
       return appendSearchCriteria(createQuotedAtCriteria(operator, values));
    }

    public FreightQuoteRequest<T> withQuotedAtIsUnknown(){
       return withQuotedAt(Operator.IS_NULL);
    }

    public FreightQuoteRequest<T> withQuotedAtIsKnown(){
       return withQuotedAt(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createQuotedAtCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(FreightQuote.QUOTED_AT_PROPERTY, operator, values);
    }

    public FreightQuoteRequest<T> withQuotedAtIs(LocalDateTime quotedAt){
       return withQuotedAt(Operator.EQUAL, quotedAt);
    }

    public FreightQuoteRequest<T> withQuotedAtIsNot(LocalDateTime quotedAt){
       return withQuotedAt(Operator.NOT_EQUAL, quotedAt);
    }

    public FreightQuoteRequest<T> withQuotedAtIn(LocalDateTime... quotedAt){
       return withQuotedAt(Operator.IN, (Object[])quotedAt);
    }

    public FreightQuoteRequest<T> withQuotedAtNotIn(LocalDateTime... quotedAt){
       return withQuotedAt(Operator.NOT_IN, (Object[])quotedAt);
    }
    public FreightQuoteRequest<T> withQuotedAtGreaterThan(LocalDateTime quotedAt){
       return withQuotedAt(Operator.GREATER_THAN, quotedAt);
    }

    public FreightQuoteRequest<T> withQuotedAtGreaterThanOrEqualTo(LocalDateTime quotedAt){
       return withQuotedAt(Operator.GREATER_THAN_OR_EQUAL, quotedAt);
    }

    public FreightQuoteRequest<T> withQuotedAtLessThan(LocalDateTime quotedAt){
       return withQuotedAt(Operator.LESS_THAN, quotedAt);
    }

    public FreightQuoteRequest<T> withQuotedAtLessThanOrEqualTo(LocalDateTime quotedAt){
       return withQuotedAt(Operator.LESS_THAN_OR_EQUAL, quotedAt);
    }

    public FreightQuoteRequest<T> withQuotedAtBetween(LocalDateTime startOfQuotedAt, LocalDateTime endOfQuotedAt){
       return withQuotedAt(Operator.BETWEEN, startOfQuotedAt, endOfQuotedAt);
    }
    public FreightQuoteRequest<T> withQuotedAtBefore(LocalDateTime quotedAt){
       return withQuotedAt(Operator.LESS_THAN, quotedAt);
    }

    public FreightQuoteRequest<T> withQuotedAtBefore(Date quotedAt){
       return withQuotedAt(Operator.LESS_THAN, quotedAt);
    }

    public FreightQuoteRequest<T> withQuotedAtAfter(LocalDateTime quotedAt){
       return withQuotedAt(Operator.GREATER_THAN, quotedAt);
    }

    public FreightQuoteRequest<T> withQuotedAtAfter(Date quotedAt){
       return withQuotedAt(Operator.GREATER_THAN, quotedAt);
    }

    public FreightQuoteRequest<T> withQuotedAtBetween(Date startOfQuotedAt, Date endOfQuotedAt){
       return withQuotedAt(Operator.BETWEEN, startOfQuotedAt, endOfQuotedAt);
    }




    public FreightQuoteRequest<T> filterByInstructions(String... instructions){
      if (instructions == null || instructions.length == 0) {
        throw new IllegalArgumentException("filterByInstructions parameter instructions cannot be empty");
      }
      return appendSearchCriteria(createInstructionsCriteria(Operator.EQUAL, (Object[])instructions));
    }

    public FreightQuoteRequest<T> withInstructions(Operator operator, Object... values){
       return appendSearchCriteria(createInstructionsCriteria(operator, values));
    }

    public FreightQuoteRequest<T> withInstructionsIsUnknown(){
       return withInstructions(Operator.IS_NULL);
    }

    public FreightQuoteRequest<T> withInstructionsIsKnown(){
       return withInstructions(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createInstructionsCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(FreightQuote.INSTRUCTIONS_PROPERTY, operator, values);
    }

    public FreightQuoteRequest<T> withInstructionsIsNot(String instructions){
       return withInstructions(Operator.NOT_EQUAL, instructions);
    }

    public FreightQuoteRequest<T> withInstructionsIn(String... instructions){
       return withInstructions(Operator.IN, (Object[])instructions);
    }

    public FreightQuoteRequest<T> withInstructionsNotIn(String... instructions){
       return withInstructions(Operator.NOT_IN, (Object[])instructions);
    }
    public FreightQuoteRequest<T> withInstructionsGreaterThan(String instructions){
       return withInstructions(Operator.GREATER_THAN, instructions);
    }

    public FreightQuoteRequest<T> withInstructionsGreaterThanOrEqualTo(String instructions){
       return withInstructions(Operator.GREATER_THAN_OR_EQUAL, instructions);
    }

    public FreightQuoteRequest<T> withInstructionsLessThan(String instructions){
       return withInstructions(Operator.LESS_THAN, instructions);
    }

    public FreightQuoteRequest<T> withInstructionsLessThanOrEqualTo(String instructions){
       return withInstructions(Operator.LESS_THAN_OR_EQUAL, instructions);
    }

    public FreightQuoteRequest<T> withInstructionsBetween(String startOfInstructions, String endOfInstructions){
       return withInstructions(Operator.BETWEEN, startOfInstructions, endOfInstructions);
    }
    public FreightQuoteRequest<T> withInstructionsStartingWith(String instructions){
       return withInstructions(Operator.BEGIN_WITH, instructions);
    }
    public FreightQuoteRequest<T> withInstructionsContaining(String instructions){
       return withInstructions(Operator.CONTAIN, instructions);
    }

    public FreightQuoteRequest<T> withInstructionsNotContaining(String instructions){
       return withInstructions(Operator.NOT_CONTAIN, instructions);
    }

    public FreightQuoteRequest<T> withInstructionsNotStartingWith(String instructions){
       return withInstructions(Operator.NOT_BEGIN_WITH, instructions);
    }

    public FreightQuoteRequest<T> withInstructionsEndingWith(String instructions){
       return withInstructions(Operator.END_WITH, instructions);
    }

    public FreightQuoteRequest<T> withInstructionsNotEndingWith(String instructions){
       return withInstructions(Operator.NOT_END_WITH, instructions);
    }

    public FreightQuoteRequest<T> withInstructionsIs(String instructions){
       return withInstructions(Operator.EQUAL, instructions);
    }

    public FreightQuoteRequest<T> withInstructionsSoundingLike(String instructions){
       return withInstructions(Operator.SOUNDS_LIKE, instructions);
    }



    public FreightQuoteRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public FreightQuoteRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public FreightQuoteRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public FreightQuoteRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(FreightQuote.VERSION_PROPERTY, operator, values);
    }

    public FreightQuoteRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public FreightQuoteRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public FreightQuoteRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public FreightQuoteRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public FreightQuoteRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public FreightQuoteRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public FreightQuoteRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public FreightQuoteRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public FreightQuoteRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }



    public FreightQuoteRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public FreightQuoteRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public FreightQuoteRequest minAmount(){
        return minAmountAs(prefix("minOf",FreightQuote.AMOUNT_PROPERTY));
    }

    public FreightQuoteRequest minAmountAs(String retName){
        super.min(retName, FreightQuote.AMOUNT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest maxAmount(){
        return maxAmountAs(prefix("maxOf",FreightQuote.AMOUNT_PROPERTY));
    }

    public FreightQuoteRequest maxAmountAs(String retName){
        super.max(retName, FreightQuote.AMOUNT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest sumAmount(){
        return sumAmountAs(prefix("sumOf",FreightQuote.AMOUNT_PROPERTY));
    }

    public FreightQuoteRequest sumAmountAs(String retName){
        super.sum(retName, FreightQuote.AMOUNT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest avgAmount(){
        return avgAmountAs(prefix("avgOf",FreightQuote.AMOUNT_PROPERTY));
    }

    public FreightQuoteRequest avgAmountAs(String retName){
        super.avg(retName, FreightQuote.AMOUNT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest standardDeviationAmount(){
        return standardDeviationAmountAs(prefix("standardDeviationOf",FreightQuote.AMOUNT_PROPERTY));
    }

    public FreightQuoteRequest standardDeviationAmountAs(String retName){
        super.standardDeviation(retName, FreightQuote.AMOUNT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest squareRootOfPopulationStandardDeviationAmount(){
        return squareRootOfPopulationStandardDeviationAmountAs(prefix("squareRootOfPopulationStandardDeviationOf",FreightQuote.AMOUNT_PROPERTY));
    }

    public FreightQuoteRequest squareRootOfPopulationStandardDeviationAmountAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, FreightQuote.AMOUNT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest sampleVarianceAmount(){
        return sampleVarianceAmountAs(prefix("sampleVarianceOf",FreightQuote.AMOUNT_PROPERTY));
    }

    public FreightQuoteRequest sampleVarianceAmountAs(String retName){
        super.sampleVariance(retName, FreightQuote.AMOUNT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest samplePopulationVarianceAmount(){
        return samplePopulationVarianceAmountAs(prefix("samplePopulationVarianceOf",FreightQuote.AMOUNT_PROPERTY));
    }

    public FreightQuoteRequest samplePopulationVarianceAmountAs(String retName){
        super.samplePopulationVariance(retName, FreightQuote.AMOUNT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest minShipmentNumber(){
        return minShipmentNumberAs(prefix("minOf",FreightQuote.SHIPMENT_NUMBER_PROPERTY));
    }

    public FreightQuoteRequest minShipmentNumberAs(String retName){
        super.min(retName, FreightQuote.SHIPMENT_NUMBER_PROPERTY);
        return this;
    }
    public FreightQuoteRequest maxShipmentNumber(){
        return maxShipmentNumberAs(prefix("maxOf",FreightQuote.SHIPMENT_NUMBER_PROPERTY));
    }

    public FreightQuoteRequest maxShipmentNumberAs(String retName){
        super.max(retName, FreightQuote.SHIPMENT_NUMBER_PROPERTY);
        return this;
    }
    public FreightQuoteRequest sumShipmentNumber(){
        return sumShipmentNumberAs(prefix("sumOf",FreightQuote.SHIPMENT_NUMBER_PROPERTY));
    }

    public FreightQuoteRequest sumShipmentNumberAs(String retName){
        super.sum(retName, FreightQuote.SHIPMENT_NUMBER_PROPERTY);
        return this;
    }
    public FreightQuoteRequest avgShipmentNumber(){
        return avgShipmentNumberAs(prefix("avgOf",FreightQuote.SHIPMENT_NUMBER_PROPERTY));
    }

    public FreightQuoteRequest avgShipmentNumberAs(String retName){
        super.avg(retName, FreightQuote.SHIPMENT_NUMBER_PROPERTY);
        return this;
    }
    public FreightQuoteRequest standardDeviationShipmentNumber(){
        return standardDeviationShipmentNumberAs(prefix("standardDeviationOf",FreightQuote.SHIPMENT_NUMBER_PROPERTY));
    }

    public FreightQuoteRequest standardDeviationShipmentNumberAs(String retName){
        super.standardDeviation(retName, FreightQuote.SHIPMENT_NUMBER_PROPERTY);
        return this;
    }
    public FreightQuoteRequest squareRootOfPopulationStandardDeviationShipmentNumber(){
        return squareRootOfPopulationStandardDeviationShipmentNumberAs(prefix("squareRootOfPopulationStandardDeviationOf",FreightQuote.SHIPMENT_NUMBER_PROPERTY));
    }

    public FreightQuoteRequest squareRootOfPopulationStandardDeviationShipmentNumberAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, FreightQuote.SHIPMENT_NUMBER_PROPERTY);
        return this;
    }
    public FreightQuoteRequest sampleVarianceShipmentNumber(){
        return sampleVarianceShipmentNumberAs(prefix("sampleVarianceOf",FreightQuote.SHIPMENT_NUMBER_PROPERTY));
    }

    public FreightQuoteRequest sampleVarianceShipmentNumberAs(String retName){
        super.sampleVariance(retName, FreightQuote.SHIPMENT_NUMBER_PROPERTY);
        return this;
    }
    public FreightQuoteRequest samplePopulationVarianceShipmentNumber(){
        return samplePopulationVarianceShipmentNumberAs(prefix("samplePopulationVarianceOf",FreightQuote.SHIPMENT_NUMBER_PROPERTY));
    }

    public FreightQuoteRequest samplePopulationVarianceShipmentNumberAs(String retName){
        super.samplePopulationVariance(retName, FreightQuote.SHIPMENT_NUMBER_PROPERTY);
        return this;
    }
    public FreightQuoteRequest minPackageCount(){
        return minPackageCountAs(prefix("minOf",FreightQuote.PACKAGE_COUNT_PROPERTY));
    }

    public FreightQuoteRequest minPackageCountAs(String retName){
        super.min(retName, FreightQuote.PACKAGE_COUNT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest maxPackageCount(){
        return maxPackageCountAs(prefix("maxOf",FreightQuote.PACKAGE_COUNT_PROPERTY));
    }

    public FreightQuoteRequest maxPackageCountAs(String retName){
        super.max(retName, FreightQuote.PACKAGE_COUNT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest sumPackageCount(){
        return sumPackageCountAs(prefix("sumOf",FreightQuote.PACKAGE_COUNT_PROPERTY));
    }

    public FreightQuoteRequest sumPackageCountAs(String retName){
        super.sum(retName, FreightQuote.PACKAGE_COUNT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest avgPackageCount(){
        return avgPackageCountAs(prefix("avgOf",FreightQuote.PACKAGE_COUNT_PROPERTY));
    }

    public FreightQuoteRequest avgPackageCountAs(String retName){
        super.avg(retName, FreightQuote.PACKAGE_COUNT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest standardDeviationPackageCount(){
        return standardDeviationPackageCountAs(prefix("standardDeviationOf",FreightQuote.PACKAGE_COUNT_PROPERTY));
    }

    public FreightQuoteRequest standardDeviationPackageCountAs(String retName){
        super.standardDeviation(retName, FreightQuote.PACKAGE_COUNT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest squareRootOfPopulationStandardDeviationPackageCount(){
        return squareRootOfPopulationStandardDeviationPackageCountAs(prefix("squareRootOfPopulationStandardDeviationOf",FreightQuote.PACKAGE_COUNT_PROPERTY));
    }

    public FreightQuoteRequest squareRootOfPopulationStandardDeviationPackageCountAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, FreightQuote.PACKAGE_COUNT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest sampleVariancePackageCount(){
        return sampleVariancePackageCountAs(prefix("sampleVarianceOf",FreightQuote.PACKAGE_COUNT_PROPERTY));
    }

    public FreightQuoteRequest sampleVariancePackageCountAs(String retName){
        super.sampleVariance(retName, FreightQuote.PACKAGE_COUNT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest samplePopulationVariancePackageCount(){
        return samplePopulationVariancePackageCountAs(prefix("samplePopulationVarianceOf",FreightQuote.PACKAGE_COUNT_PROPERTY));
    }

    public FreightQuoteRequest samplePopulationVariancePackageCountAs(String retName){
        super.samplePopulationVariance(retName, FreightQuote.PACKAGE_COUNT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest<T> groupByPlatformWithDetails(){
       return groupByPlatformWithDetails(Q.platforms().unlimited());
    }

    public FreightQuoteRequest<T> groupByPlatformWithDetails(PlatformRequest subRequest){
       aggregate(FreightQuote.PLATFORM_PROPERTY, subRequest);
       return this;
    }










    public FreightQuoteRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public FreightQuoteRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, FreightQuote.ID_PROPERTY);
        return this;
    }
    public FreightQuoteRequest<T> countDistinctPlatform(){
        return countDistinctPlatformAs("platform_distinct_count");
    }

    public FreightQuoteRequest<T> countDistinctPlatformAs(String alias){
        super.countDistinctProperty(alias, FreightQuote.PLATFORM_PROPERTY);
        return this;
    }
    public FreightQuoteRequest<T> countDistinctReference(){
        return countDistinctReferenceAs("reference_distinct_count");
    }

    public FreightQuoteRequest<T> countDistinctReferenceAs(String alias){
        super.countDistinctProperty(alias, FreightQuote.REFERENCE_PROPERTY);
        return this;
    }
    public FreightQuoteRequest<T> countDistinctAmount(){
        return countDistinctAmountAs("amount_distinct_count");
    }

    public FreightQuoteRequest<T> countDistinctAmountAs(String alias){
        super.countDistinctProperty(alias, FreightQuote.AMOUNT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest<T> countDistinctShipmentNumber(){
        return countDistinctShipmentNumberAs("shipment_number_distinct_count");
    }

    public FreightQuoteRequest<T> countDistinctShipmentNumberAs(String alias){
        super.countDistinctProperty(alias, FreightQuote.SHIPMENT_NUMBER_PROPERTY);
        return this;
    }
    public FreightQuoteRequest<T> countDistinctPackageCount(){
        return countDistinctPackageCountAs("package_count_distinct_count");
    }

    public FreightQuoteRequest<T> countDistinctPackageCountAs(String alias){
        super.countDistinctProperty(alias, FreightQuote.PACKAGE_COUNT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest<T> countDistinctDeliveryDate(){
        return countDistinctDeliveryDateAs("delivery_date_distinct_count");
    }

    public FreightQuoteRequest<T> countDistinctDeliveryDateAs(String alias){
        super.countDistinctProperty(alias, FreightQuote.DELIVERY_DATE_PROPERTY);
        return this;
    }
    public FreightQuoteRequest<T> countDistinctPickupTime(){
        return countDistinctPickupTimeAs("pickup_time_distinct_count");
    }

    public FreightQuoteRequest<T> countDistinctPickupTimeAs(String alias){
        super.countDistinctProperty(alias, FreightQuote.PICKUP_TIME_PROPERTY);
        return this;
    }
    public FreightQuoteRequest<T> countDistinctQuotedAt(){
        return countDistinctQuotedAtAs("quoted_at_distinct_count");
    }

    public FreightQuoteRequest<T> countDistinctQuotedAtAs(String alias){
        super.countDistinctProperty(alias, FreightQuote.QUOTED_AT_PROPERTY);
        return this;
    }
    public FreightQuoteRequest<T> countDistinctInstructions(){
        return countDistinctInstructionsAs("instructions_distinct_count");
    }

    public FreightQuoteRequest<T> countDistinctInstructionsAs(String alias){
        super.countDistinctProperty(alias, FreightQuote.INSTRUCTIONS_PROPERTY);
        return this;
    }
    public FreightQuoteRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public FreightQuoteRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, FreightQuote.VERSION_PROPERTY);
        return this;
    }

    public FreightQuoteRequest<T> groupById(){
       groupBy(FreightQuote.ID_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByIdAs(String retName){
       groupBy(retName, FreightQuote.ID_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, FreightQuote.ID_PROPERTY, function);
       return this;
    }
    public FreightQuoteRequest<T> groupByPlatformWith(PlatformRequest subRequest){
       groupBy(FreightQuote.PLATFORM_PROPERTY, subRequest,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "groupByPlatformWith", null, "platform"));
       return this;
    }
    public FreightQuoteRequest<T> groupByPlatform(){
       groupBy(FreightQuote.PLATFORM_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByPlatformAs(String retName){
       groupBy(retName, FreightQuote.PLATFORM_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByPlatformWithFunction(String retName, AggrFunction function){
       groupBy(retName, FreightQuote.PLATFORM_PROPERTY, function);
       return this;
    }

    public FreightQuoteRequest<T> groupByReference(){
       groupBy(FreightQuote.REFERENCE_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByReferenceAs(String retName){
       groupBy(retName, FreightQuote.REFERENCE_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByReferenceWithFunction(String retName, AggrFunction function){
       groupBy(retName, FreightQuote.REFERENCE_PROPERTY, function);
       return this;
    }

    public FreightQuoteRequest<T> groupByAmount(){
       groupBy(FreightQuote.AMOUNT_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByAmountAs(String retName){
       groupBy(retName, FreightQuote.AMOUNT_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByAmountWithFunction(String retName, AggrFunction function){
       groupBy(retName, FreightQuote.AMOUNT_PROPERTY, function);
       return this;
    }

    public FreightQuoteRequest<T> groupByShipmentNumber(){
       groupBy(FreightQuote.SHIPMENT_NUMBER_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByShipmentNumberAs(String retName){
       groupBy(retName, FreightQuote.SHIPMENT_NUMBER_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByShipmentNumberWithFunction(String retName, AggrFunction function){
       groupBy(retName, FreightQuote.SHIPMENT_NUMBER_PROPERTY, function);
       return this;
    }

    public FreightQuoteRequest<T> groupByPackageCount(){
       groupBy(FreightQuote.PACKAGE_COUNT_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByPackageCountAs(String retName){
       groupBy(retName, FreightQuote.PACKAGE_COUNT_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByPackageCountWithFunction(String retName, AggrFunction function){
       groupBy(retName, FreightQuote.PACKAGE_COUNT_PROPERTY, function);
       return this;
    }

    public FreightQuoteRequest<T> groupByDeliveryDate(){
       groupBy(FreightQuote.DELIVERY_DATE_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByDeliveryDateAs(String retName){
       groupBy(retName, FreightQuote.DELIVERY_DATE_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByDeliveryDateWithFunction(String retName, AggrFunction function){
       groupBy(retName, FreightQuote.DELIVERY_DATE_PROPERTY, function);
       return this;
    }

    public FreightQuoteRequest<T> groupByPickupTime(){
       groupBy(FreightQuote.PICKUP_TIME_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByPickupTimeAs(String retName){
       groupBy(retName, FreightQuote.PICKUP_TIME_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByPickupTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, FreightQuote.PICKUP_TIME_PROPERTY, function);
       return this;
    }

    public FreightQuoteRequest<T> groupByQuotedAt(){
       groupBy(FreightQuote.QUOTED_AT_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByQuotedAtAs(String retName){
       groupBy(retName, FreightQuote.QUOTED_AT_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByQuotedAtWithFunction(String retName, AggrFunction function){
       groupBy(retName, FreightQuote.QUOTED_AT_PROPERTY, function);
       return this;
    }

    public FreightQuoteRequest<T> groupByInstructions(){
       groupBy(FreightQuote.INSTRUCTIONS_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByInstructionsAs(String retName){
       groupBy(retName, FreightQuote.INSTRUCTIONS_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByInstructionsWithFunction(String retName, AggrFunction function){
       groupBy(retName, FreightQuote.INSTRUCTIONS_PROPERTY, function);
       return this;
    }

    public FreightQuoteRequest<T> groupByVersion(){
       groupBy(FreightQuote.VERSION_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByVersionAs(String retName){
       groupBy(retName, FreightQuote.VERSION_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, FreightQuote.VERSION_PROPERTY, function);
       return this;
    }



    public FreightQuoteRequest<T> orderByIdAscending(){
       addOrderByAscending(FreightQuote.ID_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByIdDescending(){
       addOrderByDescending(FreightQuote.ID_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByPlatformAscending(){
       addOrderByAscending(FreightQuote.PLATFORM_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByPlatformDescending(){
       addOrderByDescending(FreightQuote.PLATFORM_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByReferenceAscending(){
       addOrderByAscending(FreightQuote.REFERENCE_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByReferenceDescending(){
       addOrderByDescending(FreightQuote.REFERENCE_PROPERTY);
       return this;
    }
    public FreightQuoteRequest<T> orderByReferenceAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(FreightQuote.REFERENCE_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByReferenceDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(FreightQuote.REFERENCE_PROPERTY);
       return this;
    }
    public FreightQuoteRequest<T> orderByAmountAscending(){
       addOrderByAscending(FreightQuote.AMOUNT_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByAmountDescending(){
       addOrderByDescending(FreightQuote.AMOUNT_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByShipmentNumberAscending(){
       addOrderByAscending(FreightQuote.SHIPMENT_NUMBER_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByShipmentNumberDescending(){
       addOrderByDescending(FreightQuote.SHIPMENT_NUMBER_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByPackageCountAscending(){
       addOrderByAscending(FreightQuote.PACKAGE_COUNT_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByPackageCountDescending(){
       addOrderByDescending(FreightQuote.PACKAGE_COUNT_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByDeliveryDateAscending(){
       addOrderByAscending(FreightQuote.DELIVERY_DATE_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByDeliveryDateDescending(){
       addOrderByDescending(FreightQuote.DELIVERY_DATE_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByPickupTimeAscending(){
       addOrderByAscending(FreightQuote.PICKUP_TIME_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByPickupTimeDescending(){
       addOrderByDescending(FreightQuote.PICKUP_TIME_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByQuotedAtAscending(){
       addOrderByAscending(FreightQuote.QUOTED_AT_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByQuotedAtDescending(){
       addOrderByDescending(FreightQuote.QUOTED_AT_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByInstructionsAscending(){
       addOrderByAscending(FreightQuote.INSTRUCTIONS_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByInstructionsDescending(){
       addOrderByDescending(FreightQuote.INSTRUCTIONS_PROPERTY);
       return this;
    }
    public FreightQuoteRequest<T> orderByInstructionsAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(FreightQuote.INSTRUCTIONS_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByInstructionsDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(FreightQuote.INSTRUCTIONS_PROPERTY);
       return this;
    }
    public FreightQuoteRequest<T> orderByVersionAscending(){
       addOrderByAscending(FreightQuote.VERSION_PROPERTY);
       return this;
    }

    public FreightQuoteRequest<T> orderByVersionDescending(){
       addOrderByDescending(FreightQuote.VERSION_PROPERTY);
       return this;
    }


    public PlatformRequest rollUpToPlatform(){
       PlatformRequest platform = Q.platforms().unlimited();
       this.internalRollUpTo("platform", platform,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "rollUpToPlatform", null, "platform"));
       return platform;
    }











   public FreightQuoteRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform){
       return facetByPlatformAs(facetName, platform, true);
   }

   public FreightQuoteRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform, boolean includeAllFacets){
       addFacet(facetName, FreightQuote.PLATFORM_PROPERTY, platform, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByPlatformAs", facetName, FreightQuote.PLATFORM_PROPERTY));
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public FreightQuoteRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public FreightQuoteRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public FreightQuoteRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public FreightQuoteRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public FreightQuoteRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public FreightQuoteRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}