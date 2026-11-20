
package com.example.exactdecimalservice.decimalentry;

import com.example.exactdecimalservice.Q;
import com.example.exactdecimalservice.platform.Platform;
import com.example.exactdecimalservice.platform.PlatformRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.math.BigDecimal;
import java.util.Objects;

public class DecimalEntryRequest<T extends DecimalEntry> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public DecimalEntryRequest(Class<T> returnType){
        this(returnType, () -> (T) new DecimalEntry());
    }

    protected DecimalEntryRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public DecimalEntryRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public DecimalEntryRequest<T> copy(){
        return (DecimalEntryRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new DecimalEntryRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public DecimalEntryRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public DecimalEntryRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public DecimalEntryRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public DecimalEntryRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public DecimalEntryRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public DecimalEntryRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public DecimalEntryRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public DecimalEntryRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public DecimalEntryRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public DecimalEntryRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (DecimalEntryRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public DecimalEntryRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public DecimalEntryRequest<T> matchingAnyOf(DecimalEntryRequest decimalEntry){
        super.internalMatchAny(decimalEntry);
        return this;
    }

    public DecimalEntryRequest<T> matchingIdsOf(DecimalEntryRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                DecimalEntry.ID_PROPERTY, request, DecimalEntry.ID_PROPERTY));
    }

    public DecimalEntryRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public DecimalEntryRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public DecimalEntryRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public DecimalEntryRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectPlatformIdOnly().selectLabel().selectAmount().selectVersion();
    }

    public DecimalEntryRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public DecimalEntryRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(DecimalEntry.ID_PROPERTY);
        selectProperty(DecimalEntry.PLATFORM_PROPERTY);
        selectProperty(DecimalEntry.LABEL_PROPERTY);
        selectProperty(DecimalEntry.AMOUNT_PROPERTY);
        selectProperty(DecimalEntry.VERSION_PROPERTY);
        return this;
    }

    public DecimalEntryRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectPlatform().selectLabel().selectAmount().selectVersion();
    }

    public DecimalEntryRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectPlatform().selectLabel().selectAmount().selectVersion();
    }


    public DecimalEntryRequest<T> selectId(){
       selectProperty(DecimalEntry.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public DecimalEntryRequest<T> unselectId(){
       unselectProperty(DecimalEntry.ID_PROPERTY);
       return this;
    }
    public DecimalEntryRequest<T> selectPlatformIdOnly(){
       selectProperty(DecimalEntry.PLATFORM_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> selectPlatform(){
        return selectPlatformWith(Q.platforms().unlimited().selectSelf());
    }

    public DecimalEntryRequest<T> selectPlatformWith(PlatformRequest platform){
       selectProperty(DecimalEntry.PLATFORM_PROPERTY);
       enhanceRelation(DecimalEntry.PLATFORM_PROPERTY, platform,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectPlatformWith", null, DecimalEntry.PLATFORM_PROPERTY));
       return this;
    }

    public DecimalEntryRequest<T> unselectPlatform(){
       unselectProperty(DecimalEntry.PLATFORM_PROPERTY);
       return this;
    }
    public DecimalEntryRequest<T> selectLabel(){
       selectProperty(DecimalEntry.LABEL_PROPERTY);
       return this;
    }

    /**
     * fill the label with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  label) to fetch label property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public DecimalEntryRequest<T> unselectLabel(){
       unselectProperty(DecimalEntry.LABEL_PROPERTY);
       return this;
    }
    public DecimalEntryRequest<T> selectAmount(){
       selectProperty(DecimalEntry.AMOUNT_PROPERTY);
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
    public DecimalEntryRequest<T> selectAmount(AggrFunction aggrFunction){
       selectProperty(DecimalEntry.AMOUNT_PROPERTY, aggrFunction);
       return this;
    }


    public DecimalEntryRequest<T> unselectAmount(){
       unselectProperty(DecimalEntry.AMOUNT_PROPERTY);
       return this;
    }
    public DecimalEntryRequest<T> selectVersion(){
       selectProperty(DecimalEntry.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public DecimalEntryRequest<T> unselectVersion(){
       unselectProperty(DecimalEntry.VERSION_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(DecimalEntry.ID_PROPERTY, operator, values);
    }

    public DecimalEntryRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public DecimalEntryRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public DecimalEntryRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public DecimalEntryRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public DecimalEntryRequest<T> filterByPlatform(Platform... platform){
      if (platform == null || platform.length == 0) {
        throw new IllegalArgumentException("filterByPlatform parameter platform cannot be empty");
      }
      return appendSearchCriteria(createPlatformCriteria(Operator.EQUAL, (Object[])platform));
    }

    public DecimalEntryRequest<T> withPlatform(Operator operator, Object... values){
       return appendSearchCriteria(createPlatformCriteria(operator, values));
    }

    public DecimalEntryRequest<T> withPlatformIsUnknown(){
       return withPlatform(Operator.IS_NULL);
    }

    public DecimalEntryRequest<T> withPlatformIsKnown(){
       return withPlatform(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createPlatformCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(DecimalEntry.PLATFORM_PROPERTY, operator, values);
    }

    public DecimalEntryRequest<T> filterByPlatform(Long platform){
      if(platform == null){
         return this;
      }
      return withPlatform(Operator.EQUAL, platform);
    }
    public DecimalEntryRequest<T> withPlatformMatching(PlatformRequest platform){
       return appendSearchCriteria(new SubQuerySearchCriteria(DecimalEntry.PLATFORM_PROPERTY, platform, Platform.ID_PROPERTY));
    }

    public DecimalEntryRequest<T> withoutPlatformMatching(PlatformRequest platform){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(DecimalEntry.PLATFORM_PROPERTY, platform, Platform.ID_PROPERTY)));
    }

    public DecimalEntryRequest<T> filterByLabel(String... label){
      if (label == null || label.length == 0) {
        throw new IllegalArgumentException("filterByLabel parameter label cannot be empty");
      }
      return appendSearchCriteria(createLabelCriteria(Operator.EQUAL, (Object[])label));
    }

    public DecimalEntryRequest<T> withLabel(Operator operator, Object... values){
       return appendSearchCriteria(createLabelCriteria(operator, values));
    }

    public DecimalEntryRequest<T> withLabelIsUnknown(){
       return withLabel(Operator.IS_NULL);
    }

    public DecimalEntryRequest<T> withLabelIsKnown(){
       return withLabel(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createLabelCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(DecimalEntry.LABEL_PROPERTY, operator, values);
    }

    public DecimalEntryRequest<T> withLabelIsNot(String label){
       return withLabel(Operator.NOT_EQUAL, label);
    }

    public DecimalEntryRequest<T> withLabelIn(String... label){
       return withLabel(Operator.IN, (Object[])label);
    }

    public DecimalEntryRequest<T> withLabelNotIn(String... label){
       return withLabel(Operator.NOT_IN, (Object[])label);
    }
    public DecimalEntryRequest<T> withLabelGreaterThan(String label){
       return withLabel(Operator.GREATER_THAN, label);
    }

    public DecimalEntryRequest<T> withLabelGreaterThanOrEqualTo(String label){
       return withLabel(Operator.GREATER_THAN_OR_EQUAL, label);
    }

    public DecimalEntryRequest<T> withLabelLessThan(String label){
       return withLabel(Operator.LESS_THAN, label);
    }

    public DecimalEntryRequest<T> withLabelLessThanOrEqualTo(String label){
       return withLabel(Operator.LESS_THAN_OR_EQUAL, label);
    }

    public DecimalEntryRequest<T> withLabelBetween(String startOfLabel, String endOfLabel){
       return withLabel(Operator.BETWEEN, startOfLabel, endOfLabel);
    }
    public DecimalEntryRequest<T> withLabelStartingWith(String label){
       return withLabel(Operator.BEGIN_WITH, label);
    }
    public DecimalEntryRequest<T> withLabelContaining(String label){
       return withLabel(Operator.CONTAIN, label);
    }

    public DecimalEntryRequest<T> withLabelNotContaining(String label){
       return withLabel(Operator.NOT_CONTAIN, label);
    }

    public DecimalEntryRequest<T> withLabelNotStartingWith(String label){
       return withLabel(Operator.NOT_BEGIN_WITH, label);
    }

    public DecimalEntryRequest<T> withLabelEndingWith(String label){
       return withLabel(Operator.END_WITH, label);
    }

    public DecimalEntryRequest<T> withLabelNotEndingWith(String label){
       return withLabel(Operator.NOT_END_WITH, label);
    }

    public DecimalEntryRequest<T> withLabelIs(String label){
       return withLabel(Operator.EQUAL, label);
    }

    public DecimalEntryRequest<T> withLabelSoundingLike(String label){
       return withLabel(Operator.SOUNDS_LIKE, label);
    }



    public DecimalEntryRequest<T> filterByAmount(BigDecimal... amount){
      if (amount == null || amount.length == 0) {
        throw new IllegalArgumentException("filterByAmount parameter amount cannot be empty");
      }
      return appendSearchCriteria(createAmountCriteria(Operator.EQUAL, (Object[])amount));
    }

    public DecimalEntryRequest<T> withAmount(Operator operator, Object... values){
       return appendSearchCriteria(createAmountCriteria(operator, values));
    }

    public DecimalEntryRequest<T> withAmountIsUnknown(){
       return withAmount(Operator.IS_NULL);
    }

    public DecimalEntryRequest<T> withAmountIsKnown(){
       return withAmount(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createAmountCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(DecimalEntry.AMOUNT_PROPERTY, operator, values);
    }

    public DecimalEntryRequest<T> withAmountIs(BigDecimal amount){
       return withAmount(Operator.EQUAL, amount);
    }

    public DecimalEntryRequest<T> withAmountIsNot(BigDecimal amount){
       return withAmount(Operator.NOT_EQUAL, amount);
    }

    public DecimalEntryRequest<T> withAmountIn(BigDecimal... amount){
       return withAmount(Operator.IN, (Object[])amount);
    }

    public DecimalEntryRequest<T> withAmountNotIn(BigDecimal... amount){
       return withAmount(Operator.NOT_IN, (Object[])amount);
    }
    public DecimalEntryRequest<T> withAmountGreaterThan(BigDecimal amount){
       return withAmount(Operator.GREATER_THAN, amount);
    }

    public DecimalEntryRequest<T> withAmountGreaterThanOrEqualTo(BigDecimal amount){
       return withAmount(Operator.GREATER_THAN_OR_EQUAL, amount);
    }

    public DecimalEntryRequest<T> withAmountLessThan(BigDecimal amount){
       return withAmount(Operator.LESS_THAN, amount);
    }

    public DecimalEntryRequest<T> withAmountLessThanOrEqualTo(BigDecimal amount){
       return withAmount(Operator.LESS_THAN_OR_EQUAL, amount);
    }

    public DecimalEntryRequest<T> withAmountBetween(BigDecimal startOfAmount, BigDecimal endOfAmount){
       return withAmount(Operator.BETWEEN, startOfAmount, endOfAmount);
    }



    public DecimalEntryRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public DecimalEntryRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public DecimalEntryRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public DecimalEntryRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(DecimalEntry.VERSION_PROPERTY, operator, values);
    }

    public DecimalEntryRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public DecimalEntryRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public DecimalEntryRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public DecimalEntryRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public DecimalEntryRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public DecimalEntryRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public DecimalEntryRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public DecimalEntryRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public DecimalEntryRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }



    public DecimalEntryRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public DecimalEntryRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public DecimalEntryRequest minAmount(){
        return minAmountAs(prefix("minOf",DecimalEntry.AMOUNT_PROPERTY));
    }

    public DecimalEntryRequest minAmountAs(String retName){
        super.min(retName, DecimalEntry.AMOUNT_PROPERTY);
        return this;
    }
    public DecimalEntryRequest maxAmount(){
        return maxAmountAs(prefix("maxOf",DecimalEntry.AMOUNT_PROPERTY));
    }

    public DecimalEntryRequest maxAmountAs(String retName){
        super.max(retName, DecimalEntry.AMOUNT_PROPERTY);
        return this;
    }
    public DecimalEntryRequest sumAmount(){
        return sumAmountAs(prefix("sumOf",DecimalEntry.AMOUNT_PROPERTY));
    }

    public DecimalEntryRequest sumAmountAs(String retName){
        super.sum(retName, DecimalEntry.AMOUNT_PROPERTY);
        return this;
    }
    public DecimalEntryRequest avgAmount(){
        return avgAmountAs(prefix("avgOf",DecimalEntry.AMOUNT_PROPERTY));
    }

    public DecimalEntryRequest avgAmountAs(String retName){
        super.avg(retName, DecimalEntry.AMOUNT_PROPERTY);
        return this;
    }
    public DecimalEntryRequest standardDeviationAmount(){
        return standardDeviationAmountAs(prefix("standardDeviationOf",DecimalEntry.AMOUNT_PROPERTY));
    }

    public DecimalEntryRequest standardDeviationAmountAs(String retName){
        super.standardDeviation(retName, DecimalEntry.AMOUNT_PROPERTY);
        return this;
    }
    public DecimalEntryRequest squareRootOfPopulationStandardDeviationAmount(){
        return squareRootOfPopulationStandardDeviationAmountAs(prefix("squareRootOfPopulationStandardDeviationOf",DecimalEntry.AMOUNT_PROPERTY));
    }

    public DecimalEntryRequest squareRootOfPopulationStandardDeviationAmountAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, DecimalEntry.AMOUNT_PROPERTY);
        return this;
    }
    public DecimalEntryRequest sampleVarianceAmount(){
        return sampleVarianceAmountAs(prefix("sampleVarianceOf",DecimalEntry.AMOUNT_PROPERTY));
    }

    public DecimalEntryRequest sampleVarianceAmountAs(String retName){
        super.sampleVariance(retName, DecimalEntry.AMOUNT_PROPERTY);
        return this;
    }
    public DecimalEntryRequest samplePopulationVarianceAmount(){
        return samplePopulationVarianceAmountAs(prefix("samplePopulationVarianceOf",DecimalEntry.AMOUNT_PROPERTY));
    }

    public DecimalEntryRequest samplePopulationVarianceAmountAs(String retName){
        super.samplePopulationVariance(retName, DecimalEntry.AMOUNT_PROPERTY);
        return this;
    }
    public DecimalEntryRequest<T> groupByPlatformWithDetails(){
       return groupByPlatformWithDetails(Q.platforms().unlimited());
    }

    public DecimalEntryRequest<T> groupByPlatformWithDetails(PlatformRequest subRequest){
       aggregate(DecimalEntry.PLATFORM_PROPERTY, subRequest);
       return this;
    }




    public DecimalEntryRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public DecimalEntryRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, DecimalEntry.ID_PROPERTY);
        return this;
    }
    public DecimalEntryRequest<T> countDistinctPlatform(){
        return countDistinctPlatformAs("platform_distinct_count");
    }

    public DecimalEntryRequest<T> countDistinctPlatformAs(String alias){
        super.countDistinctProperty(alias, DecimalEntry.PLATFORM_PROPERTY);
        return this;
    }
    public DecimalEntryRequest<T> countDistinctLabel(){
        return countDistinctLabelAs("label_distinct_count");
    }

    public DecimalEntryRequest<T> countDistinctLabelAs(String alias){
        super.countDistinctProperty(alias, DecimalEntry.LABEL_PROPERTY);
        return this;
    }
    public DecimalEntryRequest<T> countDistinctAmount(){
        return countDistinctAmountAs("amount_distinct_count");
    }

    public DecimalEntryRequest<T> countDistinctAmountAs(String alias){
        super.countDistinctProperty(alias, DecimalEntry.AMOUNT_PROPERTY);
        return this;
    }
    public DecimalEntryRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public DecimalEntryRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, DecimalEntry.VERSION_PROPERTY);
        return this;
    }

    public DecimalEntryRequest<T> groupById(){
       groupBy(DecimalEntry.ID_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> groupByIdAs(String retName){
       groupBy(retName, DecimalEntry.ID_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, DecimalEntry.ID_PROPERTY, function);
       return this;
    }
    public DecimalEntryRequest<T> groupByPlatformWith(PlatformRequest subRequest){
       groupBy(DecimalEntry.PLATFORM_PROPERTY, subRequest,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "groupByPlatformWith", null, "platform"));
       return this;
    }
    public DecimalEntryRequest<T> groupByPlatform(){
       groupBy(DecimalEntry.PLATFORM_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> groupByPlatformAs(String retName){
       groupBy(retName, DecimalEntry.PLATFORM_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> groupByPlatformWithFunction(String retName, AggrFunction function){
       groupBy(retName, DecimalEntry.PLATFORM_PROPERTY, function);
       return this;
    }

    public DecimalEntryRequest<T> groupByLabel(){
       groupBy(DecimalEntry.LABEL_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> groupByLabelAs(String retName){
       groupBy(retName, DecimalEntry.LABEL_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> groupByLabelWithFunction(String retName, AggrFunction function){
       groupBy(retName, DecimalEntry.LABEL_PROPERTY, function);
       return this;
    }

    public DecimalEntryRequest<T> groupByAmount(){
       groupBy(DecimalEntry.AMOUNT_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> groupByAmountAs(String retName){
       groupBy(retName, DecimalEntry.AMOUNT_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> groupByAmountWithFunction(String retName, AggrFunction function){
       groupBy(retName, DecimalEntry.AMOUNT_PROPERTY, function);
       return this;
    }

    public DecimalEntryRequest<T> groupByVersion(){
       groupBy(DecimalEntry.VERSION_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> groupByVersionAs(String retName){
       groupBy(retName, DecimalEntry.VERSION_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, DecimalEntry.VERSION_PROPERTY, function);
       return this;
    }



    public DecimalEntryRequest<T> orderByIdAscending(){
       addOrderByAscending(DecimalEntry.ID_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> orderByIdDescending(){
       addOrderByDescending(DecimalEntry.ID_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> orderByPlatformAscending(){
       addOrderByAscending(DecimalEntry.PLATFORM_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> orderByPlatformDescending(){
       addOrderByDescending(DecimalEntry.PLATFORM_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> orderByLabelAscending(){
       addOrderByAscending(DecimalEntry.LABEL_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> orderByLabelDescending(){
       addOrderByDescending(DecimalEntry.LABEL_PROPERTY);
       return this;
    }
    public DecimalEntryRequest<T> orderByLabelAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(DecimalEntry.LABEL_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> orderByLabelDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(DecimalEntry.LABEL_PROPERTY);
       return this;
    }
    public DecimalEntryRequest<T> orderByAmountAscending(){
       addOrderByAscending(DecimalEntry.AMOUNT_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> orderByAmountDescending(){
       addOrderByDescending(DecimalEntry.AMOUNT_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> orderByVersionAscending(){
       addOrderByAscending(DecimalEntry.VERSION_PROPERTY);
       return this;
    }

    public DecimalEntryRequest<T> orderByVersionDescending(){
       addOrderByDescending(DecimalEntry.VERSION_PROPERTY);
       return this;
    }


    public PlatformRequest rollUpToPlatform(){
       PlatformRequest platform = Q.platforms().unlimited();
       this.internalRollUpTo("platform", platform,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "rollUpToPlatform", null, "platform"));
       return platform;
    }





   public DecimalEntryRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform){
       return facetByPlatformAs(facetName, platform, true);
   }

   public DecimalEntryRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform, boolean includeAllFacets){
       addFacet(facetName, DecimalEntry.PLATFORM_PROPERTY, platform, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByPlatformAs", facetName, DecimalEntry.PLATFORM_PROPERTY));
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public DecimalEntryRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public DecimalEntryRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public DecimalEntryRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public DecimalEntryRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public DecimalEntryRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public DecimalEntryRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}