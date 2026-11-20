
package com.example.exactdecimalservice.platform;

import com.example.exactdecimalservice.Q;
import com.example.exactdecimalservice.decimalentry.DecimalEntry;
import com.example.exactdecimalservice.decimalentry.DecimalEntryRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.util.Objects;

public class PlatformRequest<T extends Platform> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public PlatformRequest(Class<T> returnType){
        this(returnType, () -> (T) new Platform());
    }

    protected PlatformRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public PlatformRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public PlatformRequest<T> copy(){
        return (PlatformRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new PlatformRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public PlatformRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public PlatformRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public PlatformRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public PlatformRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public PlatformRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public PlatformRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public PlatformRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public PlatformRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public PlatformRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public PlatformRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (PlatformRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public PlatformRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public PlatformRequest<T> matchingAnyOf(PlatformRequest platform){
        super.internalMatchAny(platform);
        return this;
    }

    public PlatformRequest<T> matchingIdsOf(PlatformRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                Platform.ID_PROPERTY, request, Platform.ID_PROPERTY));
    }

    public PlatformRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public PlatformRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public PlatformRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public PlatformRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectName().selectVersion();
    }

    public PlatformRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public PlatformRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(Platform.ID_PROPERTY);
        selectProperty(Platform.NAME_PROPERTY);
        selectProperty(Platform.VERSION_PROPERTY);
        return this;
    }

    public PlatformRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectName().selectVersion();
    }

    public PlatformRequest<T> selectChildren(){
        super.selectAny();
        selectDecimalEntryList();
        return selectId().selectName().selectVersion();
    }


    public PlatformRequest<T> selectId(){
       selectProperty(Platform.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PlatformRequest<T> unselectId(){
       unselectProperty(Platform.ID_PROPERTY);
       return this;
    }
    public PlatformRequest<T> selectName(){
       selectProperty(Platform.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PlatformRequest<T> unselectName(){
       unselectProperty(Platform.NAME_PROPERTY);
       return this;
    }
    public PlatformRequest<T> selectVersion(){
       selectProperty(Platform.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PlatformRequest<T> unselectVersion(){
       unselectProperty(Platform.VERSION_PROPERTY);
       return this;
    }
    public PlatformRequest<T> selectDecimalEntryList(){
       return selectDecimalEntryListWith(Q.decimalEntries().selectSelf());
    }

    public PlatformRequest<T> selectDecimalEntryListWith(DecimalEntryRequest decimalEntryList){
       enhanceRelation(Platform.DECIMAL_ENTRY_LIST_PROPERTY, decimalEntryList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectDecimalEntryListWith", null, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
       return this;
    }

    public PlatformRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Platform.ID_PROPERTY, operator, values);
    }

    public PlatformRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public PlatformRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public PlatformRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public PlatformRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public PlatformRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public PlatformRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public PlatformRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public PlatformRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Platform.NAME_PROPERTY, operator, values);
    }

    public PlatformRequest<T> withNameIsNot(String name){
       return withName(Operator.NOT_EQUAL, name);
    }

    public PlatformRequest<T> withNameIn(String... name){
       return withName(Operator.IN, (Object[])name);
    }

    public PlatformRequest<T> withNameNotIn(String... name){
       return withName(Operator.NOT_IN, (Object[])name);
    }
    public PlatformRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public PlatformRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public PlatformRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public PlatformRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public PlatformRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public PlatformRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public PlatformRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public PlatformRequest<T> withNameNotContaining(String name){
       return withName(Operator.NOT_CONTAIN, name);
    }

    public PlatformRequest<T> withNameNotStartingWith(String name){
       return withName(Operator.NOT_BEGIN_WITH, name);
    }

    public PlatformRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public PlatformRequest<T> withNameNotEndingWith(String name){
       return withName(Operator.NOT_END_WITH, name);
    }

    public PlatformRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public PlatformRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public PlatformRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public PlatformRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public PlatformRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public PlatformRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Platform.VERSION_PROPERTY, operator, values);
    }

    public PlatformRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public PlatformRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public PlatformRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public PlatformRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public PlatformRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public PlatformRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public PlatformRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public PlatformRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public PlatformRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public PlatformRequest<T> withDecimalEntryListMatching(DecimalEntryRequest decimalEntryRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(Platform.ID_PROPERTY, decimalEntryRequest, DecimalEntry.PLATFORM_PROPERTY));
    }

    public PlatformRequest<T> withoutDecimalEntryListMatching(DecimalEntryRequest decimalEntryRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(Platform.ID_PROPERTY, decimalEntryRequest, DecimalEntry.PLATFORM_PROPERTY)));
    }

    public PlatformRequest<T> haveDecimalEntries(){
        return withDecimalEntryListMatching(Q.decimalEntries().unlimited());
    }

    public PlatformRequest<T> haveNoDecimalEntries(){
        return withoutDecimalEntryListMatching(Q.decimalEntries().unlimited());
    }

    public PlatformRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public PlatformRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public PlatformRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public PlatformRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, Platform.ID_PROPERTY);
        return this;
    }
    public PlatformRequest<T> countDistinctName(){
        return countDistinctNameAs("name_distinct_count");
    }

    public PlatformRequest<T> countDistinctNameAs(String alias){
        super.countDistinctProperty(alias, Platform.NAME_PROPERTY);
        return this;
    }
    public PlatformRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public PlatformRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, Platform.VERSION_PROPERTY);
        return this;
    }
    public PlatformRequest<T> groupByDecimalEntriesWithDetails(DecimalEntryRequest subRequest){
       aggregate(Platform.DECIMAL_ENTRY_LIST_PROPERTY, subRequest);
       return this;
    }

    public PlatformRequest<T> groupById(){
       groupBy(Platform.ID_PROPERTY);
       return this;
    }

    public PlatformRequest<T> groupByIdAs(String retName){
       groupBy(retName, Platform.ID_PROPERTY);
       return this;
    }

    public PlatformRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, Platform.ID_PROPERTY, function);
       return this;
    }

    public PlatformRequest<T> groupByName(){
       groupBy(Platform.NAME_PROPERTY);
       return this;
    }

    public PlatformRequest<T> groupByNameAs(String retName){
       groupBy(retName, Platform.NAME_PROPERTY);
       return this;
    }

    public PlatformRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, Platform.NAME_PROPERTY, function);
       return this;
    }

    public PlatformRequest<T> groupByVersion(){
       groupBy(Platform.VERSION_PROPERTY);
       return this;
    }

    public PlatformRequest<T> groupByVersionAs(String retName){
       groupBy(retName, Platform.VERSION_PROPERTY);
       return this;
    }

    public PlatformRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, Platform.VERSION_PROPERTY, function);
       return this;
    }



    public PlatformRequest<T> orderByIdAscending(){
       addOrderByAscending(Platform.ID_PROPERTY);
       return this;
    }

    public PlatformRequest<T> orderByIdDescending(){
       addOrderByDescending(Platform.ID_PROPERTY);
       return this;
    }

    public PlatformRequest<T> orderByNameAscending(){
       addOrderByAscending(Platform.NAME_PROPERTY);
       return this;
    }

    public PlatformRequest<T> orderByNameDescending(){
       addOrderByDescending(Platform.NAME_PROPERTY);
       return this;
    }
    public PlatformRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Platform.NAME_PROPERTY);
       return this;
    }

    public PlatformRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Platform.NAME_PROPERTY);
       return this;
    }
    public PlatformRequest<T> orderByVersionAscending(){
       addOrderByAscending(Platform.VERSION_PROPERTY);
       return this;
    }

    public PlatformRequest<T> orderByVersionDescending(){
       addOrderByDescending(Platform.VERSION_PROPERTY);
       return this;
    }


    public PlatformRequest<T> statsFromDecimalEntriesAs(String name, DecimalEntryRequest subRequest){
       return statsFromDecimalEntriesAs(name, subRequest, false);
    }

    public PlatformRequest<T> statsFromDecimalEntriesAs(String name, DecimalEntryRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, DecimalEntry.PLATFORM_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromDecimalEntriesAs", name, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
       return this;
    }

    public PlatformRequest<T> statsFromDecimalEntries(DecimalEntryRequest subRequest){
       return statsFromDecimalEntriesAs(REFINEMENTS, subRequest);
    }
    public PlatformRequest<T> countDecimalEntries(){
        addRelationCount("Count", Q.decimalEntries().unlimited(), DecimalEntry.PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countDecimalEntries", null, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> countDecimalEntriesAs(String name){
        addRelationCount(name, Q.decimalEntries().unlimited(), DecimalEntry.PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countDecimalEntriesAs", name, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> countDecimalEntriesWith(String name, DecimalEntryRequest subRequest){
        addRelationCount(name, subRequest, DecimalEntry.PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countDecimalEntriesWith", name, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> minAmountOfDecimalEntries(){
        addRelationScalarAggregate("minAmountOfDecimalEntries", Q.decimalEntries().unlimited(),
            DecimalEntry.PLATFORM_PROPERTY, DecimalEntry.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minAmountOfDecimalEntries", null, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> minAmountOfDecimalEntriesAs(String name){
        return minAmountOfDecimalEntriesAs(name, Q.decimalEntries().unlimited());
    }

    public PlatformRequest<T> minAmountOfDecimalEntriesAs(String name, DecimalEntryRequest subRequest){
        addRelationScalarAggregate(name, subRequest, DecimalEntry.PLATFORM_PROPERTY,
            DecimalEntry.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minAmountOfDecimalEntriesAs", name, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> maxAmountOfDecimalEntries(){
        addRelationScalarAggregate("maxAmountOfDecimalEntries", Q.decimalEntries().unlimited(),
            DecimalEntry.PLATFORM_PROPERTY, DecimalEntry.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxAmountOfDecimalEntries", null, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> maxAmountOfDecimalEntriesAs(String name){
        return maxAmountOfDecimalEntriesAs(name, Q.decimalEntries().unlimited());
    }

    public PlatformRequest<T> maxAmountOfDecimalEntriesAs(String name, DecimalEntryRequest subRequest){
        addRelationScalarAggregate(name, subRequest, DecimalEntry.PLATFORM_PROPERTY,
            DecimalEntry.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxAmountOfDecimalEntriesAs", name, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> sumAmountOfDecimalEntries(){
        addRelationScalarAggregate("sumAmountOfDecimalEntries", Q.decimalEntries().unlimited(),
            DecimalEntry.PLATFORM_PROPERTY, DecimalEntry.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumAmountOfDecimalEntries", null, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> sumAmountOfDecimalEntriesAs(String name){
        return sumAmountOfDecimalEntriesAs(name, Q.decimalEntries().unlimited());
    }

    public PlatformRequest<T> sumAmountOfDecimalEntriesAs(String name, DecimalEntryRequest subRequest){
        addRelationScalarAggregate(name, subRequest, DecimalEntry.PLATFORM_PROPERTY,
            DecimalEntry.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumAmountOfDecimalEntriesAs", name, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> avgAmountOfDecimalEntries(){
        addRelationScalarAggregate("avgAmountOfDecimalEntries", Q.decimalEntries().unlimited(),
            DecimalEntry.PLATFORM_PROPERTY, DecimalEntry.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgAmountOfDecimalEntries", null, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> avgAmountOfDecimalEntriesAs(String name){
        return avgAmountOfDecimalEntriesAs(name, Q.decimalEntries().unlimited());
    }

    public PlatformRequest<T> avgAmountOfDecimalEntriesAs(String name, DecimalEntryRequest subRequest){
        addRelationScalarAggregate(name, subRequest, DecimalEntry.PLATFORM_PROPERTY,
            DecimalEntry.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgAmountOfDecimalEntriesAs", name, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> standardDeviationAmountOfDecimalEntries(){
        addRelationScalarAggregate("stdDevAmountOfDecimalEntries", Q.decimalEntries().unlimited(),
            DecimalEntry.PLATFORM_PROPERTY, DecimalEntry.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationAmountOfDecimalEntries", null, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> standardDeviationAmountOfDecimalEntriesAs(String name){
        return standardDeviationAmountOfDecimalEntriesAs(name, Q.decimalEntries().unlimited());
    }

    public PlatformRequest<T> standardDeviationAmountOfDecimalEntriesAs(String name, DecimalEntryRequest subRequest){
        addRelationScalarAggregate(name, subRequest, DecimalEntry.PLATFORM_PROPERTY,
            DecimalEntry.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationAmountOfDecimalEntriesAs", name, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> squareRootOfPopulationStandardDeviationAmountOfDecimalEntries(){
        addRelationScalarAggregate("stdDevPopAmountOfDecimalEntries", Q.decimalEntries().unlimited(),
            DecimalEntry.PLATFORM_PROPERTY, DecimalEntry.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationAmountOfDecimalEntries", null, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> squareRootOfPopulationStandardDeviationAmountOfDecimalEntriesAs(String name){
        return squareRootOfPopulationStandardDeviationAmountOfDecimalEntriesAs(name, Q.decimalEntries().unlimited());
    }

    public PlatformRequest<T> squareRootOfPopulationStandardDeviationAmountOfDecimalEntriesAs(String name, DecimalEntryRequest subRequest){
        addRelationScalarAggregate(name, subRequest, DecimalEntry.PLATFORM_PROPERTY,
            DecimalEntry.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationAmountOfDecimalEntriesAs", name, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> sampleVarianceAmountOfDecimalEntries(){
        addRelationScalarAggregate("varSampAmountOfDecimalEntries", Q.decimalEntries().unlimited(),
            DecimalEntry.PLATFORM_PROPERTY, DecimalEntry.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceAmountOfDecimalEntries", null, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> sampleVarianceAmountOfDecimalEntriesAs(String name){
        return sampleVarianceAmountOfDecimalEntriesAs(name, Q.decimalEntries().unlimited());
    }

    public PlatformRequest<T> sampleVarianceAmountOfDecimalEntriesAs(String name, DecimalEntryRequest subRequest){
        addRelationScalarAggregate(name, subRequest, DecimalEntry.PLATFORM_PROPERTY,
            DecimalEntry.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceAmountOfDecimalEntriesAs", name, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> samplePopulationVarianceAmountOfDecimalEntries(){
        addRelationScalarAggregate("varPopAmountOfDecimalEntries", Q.decimalEntries().unlimited(),
            DecimalEntry.PLATFORM_PROPERTY, DecimalEntry.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceAmountOfDecimalEntries", null, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> samplePopulationVarianceAmountOfDecimalEntriesAs(String name){
        return samplePopulationVarianceAmountOfDecimalEntriesAs(name, Q.decimalEntries().unlimited());
    }

    public PlatformRequest<T> samplePopulationVarianceAmountOfDecimalEntriesAs(String name, DecimalEntryRequest subRequest){
        addRelationScalarAggregate(name, subRequest, DecimalEntry.PLATFORM_PROPERTY,
            DecimalEntry.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceAmountOfDecimalEntriesAs", name, Platform.DECIMAL_ENTRY_LIST_PROPERTY));
        return this;
    }



    /**
     * get topN records
     * @param topN  records number
     */
    public PlatformRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public PlatformRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public PlatformRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public PlatformRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public PlatformRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public PlatformRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}