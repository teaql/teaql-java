
package com.example.checkernaturalmessageservice.catalog;

import com.example.checkernaturalmessageservice.Q;
import com.example.checkernaturalmessageservice.entry.Entry;
import com.example.checkernaturalmessageservice.entry.EntryRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;

public class CatalogRequest<T extends Catalog> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public CatalogRequest(Class<T> returnType){
        this(returnType, () -> (T) new Catalog());
    }

    protected CatalogRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public CatalogRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public CatalogRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public CatalogRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public CatalogRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public CatalogRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public CatalogRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public CatalogRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public CatalogRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public CatalogRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public CatalogRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public CatalogRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (CatalogRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public CatalogRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public CatalogRequest<T> matchingAnyOf(CatalogRequest catalog){
        super.internalMatchAny(catalog);
        return this;
    }

    public CatalogRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public CatalogRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public CatalogRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public CatalogRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectName().selectVersion();
    }

    public CatalogRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public CatalogRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(Catalog.ID_PROPERTY);
        selectProperty(Catalog.NAME_PROPERTY);
        selectProperty(Catalog.VERSION_PROPERTY);
        return this;
    }

    public CatalogRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectName().selectVersion();
    }

    public CatalogRequest<T> selectChildren(){
        super.selectAny();
        selectEntryList();
        return selectId().selectName().selectVersion();
    }


    public CatalogRequest<T> selectId(){
       selectProperty(Catalog.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CatalogRequest<T> unselectId(){
       unselectProperty(Catalog.ID_PROPERTY);
       return this;
    }
    public CatalogRequest<T> selectName(){
       selectProperty(Catalog.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CatalogRequest<T> unselectName(){
       unselectProperty(Catalog.NAME_PROPERTY);
       return this;
    }
    public CatalogRequest<T> selectVersion(){
       selectProperty(Catalog.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CatalogRequest<T> unselectVersion(){
       unselectProperty(Catalog.VERSION_PROPERTY);
       return this;
    }
    public CatalogRequest<T> selectEntryList(){
       return selectEntryListWith(Q.entries().selectSelf());
    }

    public CatalogRequest<T> selectEntryListWith(EntryRequest entryList){
       enhanceRelation(Catalog.ENTRY_LIST_PROPERTY, entryList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectEntryListWith", null, Catalog.ENTRY_LIST_PROPERTY));
       return this;
    }

    public CatalogRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Catalog.ID_PROPERTY, operator, values);
    }

    public CatalogRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public CatalogRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public CatalogRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public CatalogRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public CatalogRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public CatalogRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public CatalogRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public CatalogRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Catalog.NAME_PROPERTY, operator, values);
    }

    public CatalogRequest<T> withNameIsNot(String name){
       return withName(Operator.NOT_EQUAL, name);
    }

    public CatalogRequest<T> withNameIn(String... name){
       return withName(Operator.IN, (Object[])name);
    }

    public CatalogRequest<T> withNameNotIn(String... name){
       return withName(Operator.NOT_IN, (Object[])name);
    }
    public CatalogRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public CatalogRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public CatalogRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public CatalogRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public CatalogRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public CatalogRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public CatalogRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public CatalogRequest<T> withNameNotContaining(String name){
       return withName(Operator.NOT_CONTAIN, name);
    }

    public CatalogRequest<T> withNameNotStartingWith(String name){
       return withName(Operator.NOT_BEGIN_WITH, name);
    }

    public CatalogRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public CatalogRequest<T> withNameNotEndingWith(String name){
       return withName(Operator.NOT_END_WITH, name);
    }

    public CatalogRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public CatalogRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public CatalogRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public CatalogRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public CatalogRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public CatalogRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Catalog.VERSION_PROPERTY, operator, values);
    }

    public CatalogRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public CatalogRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public CatalogRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public CatalogRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public CatalogRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public CatalogRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public CatalogRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public CatalogRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public CatalogRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public CatalogRequest<T> withEntryListMatching(EntryRequest entryRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(Catalog.ID_PROPERTY, entryRequest, Entry.CATALOG_PROPERTY));
    }

    public CatalogRequest<T> withoutEntryListMatching(EntryRequest entryRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(Catalog.ID_PROPERTY, entryRequest, Entry.CATALOG_PROPERTY)));
    }

    public CatalogRequest<T> haveEntries(){
        return withEntryListMatching(Q.entries().unlimited());
    }

    public CatalogRequest<T> haveNoEntries(){
        return withoutEntryListMatching(Q.entries().unlimited());
    }

    public CatalogRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public CatalogRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public CatalogRequest<T> groupByEntriesWithDetails(EntryRequest subRequest){
       aggregate(Catalog.ENTRY_LIST_PROPERTY, subRequest);
       return this;
    }

    public CatalogRequest<T> groupById(){
       groupBy(Catalog.ID_PROPERTY);
       return this;
    }

    public CatalogRequest<T> groupByIdAs(String retName){
       groupBy(retName, Catalog.ID_PROPERTY);
       return this;
    }

    public CatalogRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, Catalog.ID_PROPERTY, function);
       return this;
    }

    public CatalogRequest<T> groupByName(){
       groupBy(Catalog.NAME_PROPERTY);
       return this;
    }

    public CatalogRequest<T> groupByNameAs(String retName){
       groupBy(retName, Catalog.NAME_PROPERTY);
       return this;
    }

    public CatalogRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, Catalog.NAME_PROPERTY, function);
       return this;
    }

    public CatalogRequest<T> groupByVersion(){
       groupBy(Catalog.VERSION_PROPERTY);
       return this;
    }

    public CatalogRequest<T> groupByVersionAs(String retName){
       groupBy(retName, Catalog.VERSION_PROPERTY);
       return this;
    }

    public CatalogRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, Catalog.VERSION_PROPERTY, function);
       return this;
    }



    public CatalogRequest<T> orderByIdAscending(){
       addOrderByAscending(Catalog.ID_PROPERTY);
       return this;
    }

    public CatalogRequest<T> orderByIdDescending(){
       addOrderByDescending(Catalog.ID_PROPERTY);
       return this;
    }

    public CatalogRequest<T> orderByNameAscending(){
       addOrderByAscending(Catalog.NAME_PROPERTY);
       return this;
    }

    public CatalogRequest<T> orderByNameDescending(){
       addOrderByDescending(Catalog.NAME_PROPERTY);
       return this;
    }
    public CatalogRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Catalog.NAME_PROPERTY);
       return this;
    }

    public CatalogRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Catalog.NAME_PROPERTY);
       return this;
    }
    public CatalogRequest<T> orderByVersionAscending(){
       addOrderByAscending(Catalog.VERSION_PROPERTY);
       return this;
    }

    public CatalogRequest<T> orderByVersionDescending(){
       addOrderByDescending(Catalog.VERSION_PROPERTY);
       return this;
    }


    public CatalogRequest<T> statsFromEntriesAs(String name, EntryRequest subRequest){
       return statsFromEntriesAs(name, subRequest, false);
    }

    public CatalogRequest<T> statsFromEntriesAs(String name, EntryRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, Entry.CATALOG_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromEntriesAs", name, Catalog.ENTRY_LIST_PROPERTY));
       return this;
    }

    public CatalogRequest<T> statsFromEntries(EntryRequest subRequest){
       return statsFromEntriesAs(REFINEMENTS, subRequest);
    }
    public CatalogRequest<T> countEntries(){
        addRelationCount("Count", Q.entries().unlimited(), Entry.CATALOG_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countEntries", null, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }

    public CatalogRequest<T> countEntriesAs(String name){
        addRelationCount(name, Q.entries().unlimited(), Entry.CATALOG_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countEntriesAs", name, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }

    public CatalogRequest<T> countEntriesWith(String name, EntryRequest subRequest){
        addRelationCount(name, subRequest, Entry.CATALOG_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countEntriesWith", name, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }
    public CatalogRequest<T> minQuantityOfEntries(){
        addRelationScalarAggregate("minQuantityOfEntries", Q.entries().unlimited(),
            Entry.CATALOG_PROPERTY, Entry.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minQuantityOfEntries", null, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }

    public CatalogRequest<T> minQuantityOfEntriesAs(String name){
        return minQuantityOfEntriesAs(name, Q.entries().unlimited());
    }

    public CatalogRequest<T> minQuantityOfEntriesAs(String name, EntryRequest subRequest){
        addRelationScalarAggregate(name, subRequest, Entry.CATALOG_PROPERTY,
            Entry.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minQuantityOfEntriesAs", name, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }
    public CatalogRequest<T> maxQuantityOfEntries(){
        addRelationScalarAggregate("maxQuantityOfEntries", Q.entries().unlimited(),
            Entry.CATALOG_PROPERTY, Entry.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxQuantityOfEntries", null, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }

    public CatalogRequest<T> maxQuantityOfEntriesAs(String name){
        return maxQuantityOfEntriesAs(name, Q.entries().unlimited());
    }

    public CatalogRequest<T> maxQuantityOfEntriesAs(String name, EntryRequest subRequest){
        addRelationScalarAggregate(name, subRequest, Entry.CATALOG_PROPERTY,
            Entry.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxQuantityOfEntriesAs", name, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }
    public CatalogRequest<T> sumQuantityOfEntries(){
        addRelationScalarAggregate("sumQuantityOfEntries", Q.entries().unlimited(),
            Entry.CATALOG_PROPERTY, Entry.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumQuantityOfEntries", null, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }

    public CatalogRequest<T> sumQuantityOfEntriesAs(String name){
        return sumQuantityOfEntriesAs(name, Q.entries().unlimited());
    }

    public CatalogRequest<T> sumQuantityOfEntriesAs(String name, EntryRequest subRequest){
        addRelationScalarAggregate(name, subRequest, Entry.CATALOG_PROPERTY,
            Entry.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumQuantityOfEntriesAs", name, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }
    public CatalogRequest<T> avgQuantityOfEntries(){
        addRelationScalarAggregate("avgQuantityOfEntries", Q.entries().unlimited(),
            Entry.CATALOG_PROPERTY, Entry.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgQuantityOfEntries", null, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }

    public CatalogRequest<T> avgQuantityOfEntriesAs(String name){
        return avgQuantityOfEntriesAs(name, Q.entries().unlimited());
    }

    public CatalogRequest<T> avgQuantityOfEntriesAs(String name, EntryRequest subRequest){
        addRelationScalarAggregate(name, subRequest, Entry.CATALOG_PROPERTY,
            Entry.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgQuantityOfEntriesAs", name, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }
    public CatalogRequest<T> standardDeviationQuantityOfEntries(){
        addRelationScalarAggregate("stdDevQuantityOfEntries", Q.entries().unlimited(),
            Entry.CATALOG_PROPERTY, Entry.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationQuantityOfEntries", null, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }

    public CatalogRequest<T> standardDeviationQuantityOfEntriesAs(String name){
        return standardDeviationQuantityOfEntriesAs(name, Q.entries().unlimited());
    }

    public CatalogRequest<T> standardDeviationQuantityOfEntriesAs(String name, EntryRequest subRequest){
        addRelationScalarAggregate(name, subRequest, Entry.CATALOG_PROPERTY,
            Entry.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationQuantityOfEntriesAs", name, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }
    public CatalogRequest<T> squareRootOfPopulationStandardDeviationQuantityOfEntries(){
        addRelationScalarAggregate("stdDevPopQuantityOfEntries", Q.entries().unlimited(),
            Entry.CATALOG_PROPERTY, Entry.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationQuantityOfEntries", null, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }

    public CatalogRequest<T> squareRootOfPopulationStandardDeviationQuantityOfEntriesAs(String name){
        return squareRootOfPopulationStandardDeviationQuantityOfEntriesAs(name, Q.entries().unlimited());
    }

    public CatalogRequest<T> squareRootOfPopulationStandardDeviationQuantityOfEntriesAs(String name, EntryRequest subRequest){
        addRelationScalarAggregate(name, subRequest, Entry.CATALOG_PROPERTY,
            Entry.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationQuantityOfEntriesAs", name, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }
    public CatalogRequest<T> sampleVarianceQuantityOfEntries(){
        addRelationScalarAggregate("varSampQuantityOfEntries", Q.entries().unlimited(),
            Entry.CATALOG_PROPERTY, Entry.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceQuantityOfEntries", null, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }

    public CatalogRequest<T> sampleVarianceQuantityOfEntriesAs(String name){
        return sampleVarianceQuantityOfEntriesAs(name, Q.entries().unlimited());
    }

    public CatalogRequest<T> sampleVarianceQuantityOfEntriesAs(String name, EntryRequest subRequest){
        addRelationScalarAggregate(name, subRequest, Entry.CATALOG_PROPERTY,
            Entry.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceQuantityOfEntriesAs", name, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }
    public CatalogRequest<T> samplePopulationVarianceQuantityOfEntries(){
        addRelationScalarAggregate("varPopQuantityOfEntries", Q.entries().unlimited(),
            Entry.CATALOG_PROPERTY, Entry.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceQuantityOfEntries", null, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }

    public CatalogRequest<T> samplePopulationVarianceQuantityOfEntriesAs(String name){
        return samplePopulationVarianceQuantityOfEntriesAs(name, Q.entries().unlimited());
    }

    public CatalogRequest<T> samplePopulationVarianceQuantityOfEntriesAs(String name, EntryRequest subRequest){
        addRelationScalarAggregate(name, subRequest, Entry.CATALOG_PROPERTY,
            Entry.QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceQuantityOfEntriesAs", name, Catalog.ENTRY_LIST_PROPERTY));
        return this;
    }



    /**
     * get topN records
     * @param topN  records number
     */
    public CatalogRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public CatalogRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public CatalogRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public CatalogRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public CatalogRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public CatalogRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}