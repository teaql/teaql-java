
package com.example.freightquotesettlement.platform;

import com.example.freightquotesettlement.Q;
import com.example.freightquotesettlement.freightquote.FreightQuote;
import com.example.freightquotesettlement.freightquote.FreightQuoteRequest;
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
        selectFreightQuoteList();
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
    public PlatformRequest<T> selectFreightQuoteList(){
       return selectFreightQuoteListWith(Q.freightQuotes().selectSelf());
    }

    public PlatformRequest<T> selectFreightQuoteListWith(FreightQuoteRequest freightQuoteList){
       enhanceRelation(Platform.FREIGHT_QUOTE_LIST_PROPERTY, freightQuoteList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectFreightQuoteListWith", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
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


    public PlatformRequest<T> withFreightQuoteListMatching(FreightQuoteRequest freightQuoteRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(Platform.ID_PROPERTY, freightQuoteRequest, FreightQuote.PLATFORM_PROPERTY));
    }

    public PlatformRequest<T> withoutFreightQuoteListMatching(FreightQuoteRequest freightQuoteRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(Platform.ID_PROPERTY, freightQuoteRequest, FreightQuote.PLATFORM_PROPERTY)));
    }

    public PlatformRequest<T> haveFreightQuotes(){
        return withFreightQuoteListMatching(Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> haveNoFreightQuotes(){
        return withoutFreightQuoteListMatching(Q.freightQuotes().unlimited());
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
    public PlatformRequest<T> groupByFreightQuotesWithDetails(FreightQuoteRequest subRequest){
       aggregate(Platform.FREIGHT_QUOTE_LIST_PROPERTY, subRequest);
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


    public PlatformRequest<T> statsFromFreightQuotesAs(String name, FreightQuoteRequest subRequest){
       return statsFromFreightQuotesAs(name, subRequest, false);
    }

    public PlatformRequest<T> statsFromFreightQuotesAs(String name, FreightQuoteRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
       return this;
    }

    public PlatformRequest<T> statsFromFreightQuotes(FreightQuoteRequest subRequest){
       return statsFromFreightQuotesAs(REFINEMENTS, subRequest);
    }
    public PlatformRequest<T> countFreightQuotes(){
        addRelationCount("Count", Q.freightQuotes().unlimited(), FreightQuote.PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> countFreightQuotesAs(String name){
        addRelationCount(name, Q.freightQuotes().unlimited(), FreightQuote.PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> countFreightQuotesWith(String name, FreightQuoteRequest subRequest){
        addRelationCount(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countFreightQuotesWith", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> minAmountOfFreightQuotes(){
        addRelationScalarAggregate("minAmountOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minAmountOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> minAmountOfFreightQuotesAs(String name){
        return minAmountOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> minAmountOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minAmountOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> maxAmountOfFreightQuotes(){
        addRelationScalarAggregate("maxAmountOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxAmountOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> maxAmountOfFreightQuotesAs(String name){
        return maxAmountOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> maxAmountOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxAmountOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> sumAmountOfFreightQuotes(){
        addRelationScalarAggregate("sumAmountOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumAmountOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> sumAmountOfFreightQuotesAs(String name){
        return sumAmountOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> sumAmountOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumAmountOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> avgAmountOfFreightQuotes(){
        addRelationScalarAggregate("avgAmountOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgAmountOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> avgAmountOfFreightQuotesAs(String name){
        return avgAmountOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> avgAmountOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgAmountOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> standardDeviationAmountOfFreightQuotes(){
        addRelationScalarAggregate("stdDevAmountOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationAmountOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> standardDeviationAmountOfFreightQuotesAs(String name){
        return standardDeviationAmountOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> standardDeviationAmountOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationAmountOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> squareRootOfPopulationStandardDeviationAmountOfFreightQuotes(){
        addRelationScalarAggregate("stdDevPopAmountOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationAmountOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> squareRootOfPopulationStandardDeviationAmountOfFreightQuotesAs(String name){
        return squareRootOfPopulationStandardDeviationAmountOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> squareRootOfPopulationStandardDeviationAmountOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationAmountOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> sampleVarianceAmountOfFreightQuotes(){
        addRelationScalarAggregate("varSampAmountOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceAmountOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> sampleVarianceAmountOfFreightQuotesAs(String name){
        return sampleVarianceAmountOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> sampleVarianceAmountOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceAmountOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> samplePopulationVarianceAmountOfFreightQuotes(){
        addRelationScalarAggregate("varPopAmountOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceAmountOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> samplePopulationVarianceAmountOfFreightQuotesAs(String name){
        return samplePopulationVarianceAmountOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> samplePopulationVarianceAmountOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.AMOUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceAmountOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> minShipmentNumberOfFreightQuotes(){
        addRelationScalarAggregate("minShipmentNumberOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.SHIPMENT_NUMBER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minShipmentNumberOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> minShipmentNumberOfFreightQuotesAs(String name){
        return minShipmentNumberOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> minShipmentNumberOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.SHIPMENT_NUMBER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minShipmentNumberOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> maxShipmentNumberOfFreightQuotes(){
        addRelationScalarAggregate("maxShipmentNumberOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.SHIPMENT_NUMBER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxShipmentNumberOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> maxShipmentNumberOfFreightQuotesAs(String name){
        return maxShipmentNumberOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> maxShipmentNumberOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.SHIPMENT_NUMBER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxShipmentNumberOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> sumShipmentNumberOfFreightQuotes(){
        addRelationScalarAggregate("sumShipmentNumberOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.SHIPMENT_NUMBER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumShipmentNumberOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> sumShipmentNumberOfFreightQuotesAs(String name){
        return sumShipmentNumberOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> sumShipmentNumberOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.SHIPMENT_NUMBER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumShipmentNumberOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> avgShipmentNumberOfFreightQuotes(){
        addRelationScalarAggregate("avgShipmentNumberOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.SHIPMENT_NUMBER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgShipmentNumberOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> avgShipmentNumberOfFreightQuotesAs(String name){
        return avgShipmentNumberOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> avgShipmentNumberOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.SHIPMENT_NUMBER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgShipmentNumberOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> standardDeviationShipmentNumberOfFreightQuotes(){
        addRelationScalarAggregate("stdDevShipmentNumberOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.SHIPMENT_NUMBER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationShipmentNumberOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> standardDeviationShipmentNumberOfFreightQuotesAs(String name){
        return standardDeviationShipmentNumberOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> standardDeviationShipmentNumberOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.SHIPMENT_NUMBER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationShipmentNumberOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> squareRootOfPopulationStandardDeviationShipmentNumberOfFreightQuotes(){
        addRelationScalarAggregate("stdDevPopShipmentNumberOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.SHIPMENT_NUMBER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationShipmentNumberOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> squareRootOfPopulationStandardDeviationShipmentNumberOfFreightQuotesAs(String name){
        return squareRootOfPopulationStandardDeviationShipmentNumberOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> squareRootOfPopulationStandardDeviationShipmentNumberOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.SHIPMENT_NUMBER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationShipmentNumberOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> sampleVarianceShipmentNumberOfFreightQuotes(){
        addRelationScalarAggregate("varSampShipmentNumberOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.SHIPMENT_NUMBER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceShipmentNumberOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> sampleVarianceShipmentNumberOfFreightQuotesAs(String name){
        return sampleVarianceShipmentNumberOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> sampleVarianceShipmentNumberOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.SHIPMENT_NUMBER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceShipmentNumberOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> samplePopulationVarianceShipmentNumberOfFreightQuotes(){
        addRelationScalarAggregate("varPopShipmentNumberOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.SHIPMENT_NUMBER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceShipmentNumberOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> samplePopulationVarianceShipmentNumberOfFreightQuotesAs(String name){
        return samplePopulationVarianceShipmentNumberOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> samplePopulationVarianceShipmentNumberOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.SHIPMENT_NUMBER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceShipmentNumberOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> minPackageCountOfFreightQuotes(){
        addRelationScalarAggregate("minPackageCountOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.PACKAGE_COUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minPackageCountOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> minPackageCountOfFreightQuotesAs(String name){
        return minPackageCountOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> minPackageCountOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.PACKAGE_COUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minPackageCountOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> maxPackageCountOfFreightQuotes(){
        addRelationScalarAggregate("maxPackageCountOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.PACKAGE_COUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxPackageCountOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> maxPackageCountOfFreightQuotesAs(String name){
        return maxPackageCountOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> maxPackageCountOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.PACKAGE_COUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxPackageCountOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> sumPackageCountOfFreightQuotes(){
        addRelationScalarAggregate("sumPackageCountOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.PACKAGE_COUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumPackageCountOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> sumPackageCountOfFreightQuotesAs(String name){
        return sumPackageCountOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> sumPackageCountOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.PACKAGE_COUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumPackageCountOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> avgPackageCountOfFreightQuotes(){
        addRelationScalarAggregate("avgPackageCountOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.PACKAGE_COUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgPackageCountOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> avgPackageCountOfFreightQuotesAs(String name){
        return avgPackageCountOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> avgPackageCountOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.PACKAGE_COUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgPackageCountOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> standardDeviationPackageCountOfFreightQuotes(){
        addRelationScalarAggregate("stdDevPackageCountOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.PACKAGE_COUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationPackageCountOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> standardDeviationPackageCountOfFreightQuotesAs(String name){
        return standardDeviationPackageCountOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> standardDeviationPackageCountOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.PACKAGE_COUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationPackageCountOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> squareRootOfPopulationStandardDeviationPackageCountOfFreightQuotes(){
        addRelationScalarAggregate("stdDevPopPackageCountOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.PACKAGE_COUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationPackageCountOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> squareRootOfPopulationStandardDeviationPackageCountOfFreightQuotesAs(String name){
        return squareRootOfPopulationStandardDeviationPackageCountOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> squareRootOfPopulationStandardDeviationPackageCountOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.PACKAGE_COUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationPackageCountOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> sampleVariancePackageCountOfFreightQuotes(){
        addRelationScalarAggregate("varSampPackageCountOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.PACKAGE_COUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVariancePackageCountOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> sampleVariancePackageCountOfFreightQuotesAs(String name){
        return sampleVariancePackageCountOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> sampleVariancePackageCountOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.PACKAGE_COUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVariancePackageCountOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> samplePopulationVariancePackageCountOfFreightQuotes(){
        addRelationScalarAggregate("varPopPackageCountOfFreightQuotes", Q.freightQuotes().unlimited(),
            FreightQuote.PLATFORM_PROPERTY, FreightQuote.PACKAGE_COUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVariancePackageCountOfFreightQuotes", null, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> samplePopulationVariancePackageCountOfFreightQuotesAs(String name){
        return samplePopulationVariancePackageCountOfFreightQuotesAs(name, Q.freightQuotes().unlimited());
    }

    public PlatformRequest<T> samplePopulationVariancePackageCountOfFreightQuotesAs(String name, FreightQuoteRequest subRequest){
        addRelationScalarAggregate(name, subRequest, FreightQuote.PLATFORM_PROPERTY,
            FreightQuote.PACKAGE_COUNT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVariancePackageCountOfFreightQuotesAs", name, Platform.FREIGHT_QUOTE_LIST_PROPERTY));
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