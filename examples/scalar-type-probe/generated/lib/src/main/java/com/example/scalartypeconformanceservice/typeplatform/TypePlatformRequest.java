
package com.example.scalartypeconformanceservice.typeplatform;

import com.example.scalartypeconformanceservice.Q;
import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe;
import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbeRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;

public class TypePlatformRequest<T extends TypePlatform> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public TypePlatformRequest(Class<T> returnType){
        this(returnType, () -> (T) new TypePlatform());
    }

    protected TypePlatformRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public TypePlatformRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public TypePlatformRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public TypePlatformRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public TypePlatformRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public TypePlatformRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public TypePlatformRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public TypePlatformRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public TypePlatformRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public TypePlatformRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public TypePlatformRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public TypePlatformRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (TypePlatformRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public TypePlatformRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public TypePlatformRequest<T> matchingAnyOf(TypePlatformRequest typePlatform){
        super.internalMatchAny(typePlatform);
        return this;
    }

    public TypePlatformRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public TypePlatformRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public TypePlatformRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public TypePlatformRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectName().selectVersion();
    }

    public TypePlatformRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public TypePlatformRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(TypePlatform.ID_PROPERTY);
        selectProperty(TypePlatform.NAME_PROPERTY);
        selectProperty(TypePlatform.VERSION_PROPERTY);
        return this;
    }

    public TypePlatformRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectName().selectVersion();
    }

    public TypePlatformRequest<T> selectChildren(){
        super.selectAny();
        selectScalarTypeProbeList();
        return selectId().selectName().selectVersion();
    }


    public TypePlatformRequest<T> selectId(){
       selectProperty(TypePlatform.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public TypePlatformRequest<T> unselectId(){
       unselectProperty(TypePlatform.ID_PROPERTY);
       return this;
    }
    public TypePlatformRequest<T> selectName(){
       selectProperty(TypePlatform.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public TypePlatformRequest<T> unselectName(){
       unselectProperty(TypePlatform.NAME_PROPERTY);
       return this;
    }
    public TypePlatformRequest<T> selectVersion(){
       selectProperty(TypePlatform.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public TypePlatformRequest<T> unselectVersion(){
       unselectProperty(TypePlatform.VERSION_PROPERTY);
       return this;
    }
    public TypePlatformRequest<T> selectScalarTypeProbeList(){
       return selectScalarTypeProbeListWith(Q.scalarTypeProbes().selectSelf());
    }

    public TypePlatformRequest<T> selectScalarTypeProbeListWith(ScalarTypeProbeRequest scalarTypeProbeList){
       enhanceRelation(TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY, scalarTypeProbeList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectScalarTypeProbeListWith", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
       return this;
    }

    public TypePlatformRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(TypePlatform.ID_PROPERTY, operator, values);
    }

    public TypePlatformRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public TypePlatformRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public TypePlatformRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public TypePlatformRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public TypePlatformRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public TypePlatformRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public TypePlatformRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public TypePlatformRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(TypePlatform.NAME_PROPERTY, operator, values);
    }

    public TypePlatformRequest<T> withNameIsNot(String name){
       return withName(Operator.NOT_EQUAL, name);
    }

    public TypePlatformRequest<T> withNameIn(String... name){
       return withName(Operator.IN, (Object[])name);
    }

    public TypePlatformRequest<T> withNameNotIn(String... name){
       return withName(Operator.NOT_IN, (Object[])name);
    }
    public TypePlatformRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public TypePlatformRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public TypePlatformRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public TypePlatformRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public TypePlatformRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public TypePlatformRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public TypePlatformRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public TypePlatformRequest<T> withNameNotContaining(String name){
       return withName(Operator.NOT_CONTAIN, name);
    }

    public TypePlatformRequest<T> withNameNotStartingWith(String name){
       return withName(Operator.NOT_BEGIN_WITH, name);
    }

    public TypePlatformRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public TypePlatformRequest<T> withNameNotEndingWith(String name){
       return withName(Operator.NOT_END_WITH, name);
    }

    public TypePlatformRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public TypePlatformRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public TypePlatformRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public TypePlatformRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public TypePlatformRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public TypePlatformRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(TypePlatform.VERSION_PROPERTY, operator, values);
    }

    public TypePlatformRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public TypePlatformRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public TypePlatformRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public TypePlatformRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public TypePlatformRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public TypePlatformRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public TypePlatformRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public TypePlatformRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public TypePlatformRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public TypePlatformRequest<T> withScalarTypeProbeListMatching(ScalarTypeProbeRequest scalarTypeProbeRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(TypePlatform.ID_PROPERTY, scalarTypeProbeRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY));
    }

    public TypePlatformRequest<T> withoutScalarTypeProbeListMatching(ScalarTypeProbeRequest scalarTypeProbeRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(TypePlatform.ID_PROPERTY, scalarTypeProbeRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY)));
    }

    public TypePlatformRequest<T> haveScalarTypeProbes(){
        return withScalarTypeProbeListMatching(Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> haveNoScalarTypeProbes(){
        return withoutScalarTypeProbeListMatching(Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public TypePlatformRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public TypePlatformRequest<T> groupByScalarTypeProbesWithDetails(ScalarTypeProbeRequest subRequest){
       aggregate(TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY, subRequest);
       return this;
    }

    public TypePlatformRequest<T> groupById(){
       groupBy(TypePlatform.ID_PROPERTY);
       return this;
    }

    public TypePlatformRequest<T> groupByIdAs(String retName){
       groupBy(retName, TypePlatform.ID_PROPERTY);
       return this;
    }

    public TypePlatformRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, TypePlatform.ID_PROPERTY, function);
       return this;
    }

    public TypePlatformRequest<T> groupByName(){
       groupBy(TypePlatform.NAME_PROPERTY);
       return this;
    }

    public TypePlatformRequest<T> groupByNameAs(String retName){
       groupBy(retName, TypePlatform.NAME_PROPERTY);
       return this;
    }

    public TypePlatformRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, TypePlatform.NAME_PROPERTY, function);
       return this;
    }

    public TypePlatformRequest<T> groupByVersion(){
       groupBy(TypePlatform.VERSION_PROPERTY);
       return this;
    }

    public TypePlatformRequest<T> groupByVersionAs(String retName){
       groupBy(retName, TypePlatform.VERSION_PROPERTY);
       return this;
    }

    public TypePlatformRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, TypePlatform.VERSION_PROPERTY, function);
       return this;
    }



    public TypePlatformRequest<T> orderByIdAscending(){
       addOrderByAscending(TypePlatform.ID_PROPERTY);
       return this;
    }

    public TypePlatformRequest<T> orderByIdDescending(){
       addOrderByDescending(TypePlatform.ID_PROPERTY);
       return this;
    }

    public TypePlatformRequest<T> orderByNameAscending(){
       addOrderByAscending(TypePlatform.NAME_PROPERTY);
       return this;
    }

    public TypePlatformRequest<T> orderByNameDescending(){
       addOrderByDescending(TypePlatform.NAME_PROPERTY);
       return this;
    }
    public TypePlatformRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(TypePlatform.NAME_PROPERTY);
       return this;
    }

    public TypePlatformRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(TypePlatform.NAME_PROPERTY);
       return this;
    }
    public TypePlatformRequest<T> orderByVersionAscending(){
       addOrderByAscending(TypePlatform.VERSION_PROPERTY);
       return this;
    }

    public TypePlatformRequest<T> orderByVersionDescending(){
       addOrderByDescending(TypePlatform.VERSION_PROPERTY);
       return this;
    }


    public TypePlatformRequest<T> statsFromScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
       return statsFromScalarTypeProbesAs(name, subRequest, false);
    }

    public TypePlatformRequest<T> statsFromScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
       return this;
    }

    public TypePlatformRequest<T> statsFromScalarTypeProbes(ScalarTypeProbeRequest subRequest){
       return statsFromScalarTypeProbesAs(REFINEMENTS, subRequest);
    }
    public TypePlatformRequest<T> countScalarTypeProbes(){
        addRelationCount("Count", Q.scalarTypeProbes().unlimited(), ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> countScalarTypeProbesAs(String name){
        addRelationCount(name, Q.scalarTypeProbes().unlimited(), ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> countScalarTypeProbesWith(String name, ScalarTypeProbeRequest subRequest){
        addRelationCount(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countScalarTypeProbesWith", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> minRequiredIntegerOfScalarTypeProbes(){
        addRelationScalarAggregate("minRequiredIntegerOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minRequiredIntegerOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> minRequiredIntegerOfScalarTypeProbesAs(String name){
        return minRequiredIntegerOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> minRequiredIntegerOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minRequiredIntegerOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> maxRequiredIntegerOfScalarTypeProbes(){
        addRelationScalarAggregate("maxRequiredIntegerOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxRequiredIntegerOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> maxRequiredIntegerOfScalarTypeProbesAs(String name){
        return maxRequiredIntegerOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> maxRequiredIntegerOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxRequiredIntegerOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sumRequiredIntegerOfScalarTypeProbes(){
        addRelationScalarAggregate("sumRequiredIntegerOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumRequiredIntegerOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sumRequiredIntegerOfScalarTypeProbesAs(String name){
        return sumRequiredIntegerOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sumRequiredIntegerOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumRequiredIntegerOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> avgRequiredIntegerOfScalarTypeProbes(){
        addRelationScalarAggregate("avgRequiredIntegerOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgRequiredIntegerOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> avgRequiredIntegerOfScalarTypeProbesAs(String name){
        return avgRequiredIntegerOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> avgRequiredIntegerOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgRequiredIntegerOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> standardDeviationRequiredIntegerOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevRequiredIntegerOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationRequiredIntegerOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> standardDeviationRequiredIntegerOfScalarTypeProbesAs(String name){
        return standardDeviationRequiredIntegerOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> standardDeviationRequiredIntegerOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationRequiredIntegerOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationRequiredIntegerOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevPopRequiredIntegerOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationRequiredIntegerOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationRequiredIntegerOfScalarTypeProbesAs(String name){
        return squareRootOfPopulationStandardDeviationRequiredIntegerOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationRequiredIntegerOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationRequiredIntegerOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sampleVarianceRequiredIntegerOfScalarTypeProbes(){
        addRelationScalarAggregate("varSampRequiredIntegerOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceRequiredIntegerOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sampleVarianceRequiredIntegerOfScalarTypeProbesAs(String name){
        return sampleVarianceRequiredIntegerOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sampleVarianceRequiredIntegerOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceRequiredIntegerOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> samplePopulationVarianceRequiredIntegerOfScalarTypeProbes(){
        addRelationScalarAggregate("varPopRequiredIntegerOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceRequiredIntegerOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> samplePopulationVarianceRequiredIntegerOfScalarTypeProbesAs(String name){
        return samplePopulationVarianceRequiredIntegerOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> samplePopulationVarianceRequiredIntegerOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceRequiredIntegerOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> minOptionalIntegerOfScalarTypeProbes(){
        addRelationScalarAggregate("minOptionalIntegerOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minOptionalIntegerOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> minOptionalIntegerOfScalarTypeProbesAs(String name){
        return minOptionalIntegerOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> minOptionalIntegerOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minOptionalIntegerOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> maxOptionalIntegerOfScalarTypeProbes(){
        addRelationScalarAggregate("maxOptionalIntegerOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxOptionalIntegerOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> maxOptionalIntegerOfScalarTypeProbesAs(String name){
        return maxOptionalIntegerOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> maxOptionalIntegerOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxOptionalIntegerOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sumOptionalIntegerOfScalarTypeProbes(){
        addRelationScalarAggregate("sumOptionalIntegerOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumOptionalIntegerOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sumOptionalIntegerOfScalarTypeProbesAs(String name){
        return sumOptionalIntegerOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sumOptionalIntegerOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumOptionalIntegerOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> avgOptionalIntegerOfScalarTypeProbes(){
        addRelationScalarAggregate("avgOptionalIntegerOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgOptionalIntegerOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> avgOptionalIntegerOfScalarTypeProbesAs(String name){
        return avgOptionalIntegerOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> avgOptionalIntegerOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgOptionalIntegerOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> standardDeviationOptionalIntegerOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevOptionalIntegerOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationOptionalIntegerOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> standardDeviationOptionalIntegerOfScalarTypeProbesAs(String name){
        return standardDeviationOptionalIntegerOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> standardDeviationOptionalIntegerOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationOptionalIntegerOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationOptionalIntegerOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevPopOptionalIntegerOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationOptionalIntegerOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationOptionalIntegerOfScalarTypeProbesAs(String name){
        return squareRootOfPopulationStandardDeviationOptionalIntegerOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationOptionalIntegerOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationOptionalIntegerOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sampleVarianceOptionalIntegerOfScalarTypeProbes(){
        addRelationScalarAggregate("varSampOptionalIntegerOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceOptionalIntegerOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sampleVarianceOptionalIntegerOfScalarTypeProbesAs(String name){
        return sampleVarianceOptionalIntegerOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sampleVarianceOptionalIntegerOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceOptionalIntegerOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> samplePopulationVarianceOptionalIntegerOfScalarTypeProbes(){
        addRelationScalarAggregate("varPopOptionalIntegerOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceOptionalIntegerOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> samplePopulationVarianceOptionalIntegerOfScalarTypeProbesAs(String name){
        return samplePopulationVarianceOptionalIntegerOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> samplePopulationVarianceOptionalIntegerOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceOptionalIntegerOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> minRequiredLongOfScalarTypeProbes(){
        addRelationScalarAggregate("minRequiredLongOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minRequiredLongOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> minRequiredLongOfScalarTypeProbesAs(String name){
        return minRequiredLongOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> minRequiredLongOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minRequiredLongOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> maxRequiredLongOfScalarTypeProbes(){
        addRelationScalarAggregate("maxRequiredLongOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxRequiredLongOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> maxRequiredLongOfScalarTypeProbesAs(String name){
        return maxRequiredLongOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> maxRequiredLongOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxRequiredLongOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sumRequiredLongOfScalarTypeProbes(){
        addRelationScalarAggregate("sumRequiredLongOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumRequiredLongOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sumRequiredLongOfScalarTypeProbesAs(String name){
        return sumRequiredLongOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sumRequiredLongOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumRequiredLongOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> avgRequiredLongOfScalarTypeProbes(){
        addRelationScalarAggregate("avgRequiredLongOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgRequiredLongOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> avgRequiredLongOfScalarTypeProbesAs(String name){
        return avgRequiredLongOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> avgRequiredLongOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgRequiredLongOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> standardDeviationRequiredLongOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevRequiredLongOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationRequiredLongOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> standardDeviationRequiredLongOfScalarTypeProbesAs(String name){
        return standardDeviationRequiredLongOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> standardDeviationRequiredLongOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationRequiredLongOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationRequiredLongOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevPopRequiredLongOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationRequiredLongOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationRequiredLongOfScalarTypeProbesAs(String name){
        return squareRootOfPopulationStandardDeviationRequiredLongOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationRequiredLongOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationRequiredLongOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sampleVarianceRequiredLongOfScalarTypeProbes(){
        addRelationScalarAggregate("varSampRequiredLongOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceRequiredLongOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sampleVarianceRequiredLongOfScalarTypeProbesAs(String name){
        return sampleVarianceRequiredLongOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sampleVarianceRequiredLongOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceRequiredLongOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> samplePopulationVarianceRequiredLongOfScalarTypeProbes(){
        addRelationScalarAggregate("varPopRequiredLongOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceRequiredLongOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> samplePopulationVarianceRequiredLongOfScalarTypeProbesAs(String name){
        return samplePopulationVarianceRequiredLongOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> samplePopulationVarianceRequiredLongOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceRequiredLongOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> minOptionalLongOfScalarTypeProbes(){
        addRelationScalarAggregate("minOptionalLongOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minOptionalLongOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> minOptionalLongOfScalarTypeProbesAs(String name){
        return minOptionalLongOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> minOptionalLongOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minOptionalLongOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> maxOptionalLongOfScalarTypeProbes(){
        addRelationScalarAggregate("maxOptionalLongOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxOptionalLongOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> maxOptionalLongOfScalarTypeProbesAs(String name){
        return maxOptionalLongOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> maxOptionalLongOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxOptionalLongOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sumOptionalLongOfScalarTypeProbes(){
        addRelationScalarAggregate("sumOptionalLongOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumOptionalLongOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sumOptionalLongOfScalarTypeProbesAs(String name){
        return sumOptionalLongOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sumOptionalLongOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumOptionalLongOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> avgOptionalLongOfScalarTypeProbes(){
        addRelationScalarAggregate("avgOptionalLongOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgOptionalLongOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> avgOptionalLongOfScalarTypeProbesAs(String name){
        return avgOptionalLongOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> avgOptionalLongOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgOptionalLongOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> standardDeviationOptionalLongOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevOptionalLongOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationOptionalLongOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> standardDeviationOptionalLongOfScalarTypeProbesAs(String name){
        return standardDeviationOptionalLongOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> standardDeviationOptionalLongOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationOptionalLongOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationOptionalLongOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevPopOptionalLongOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationOptionalLongOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationOptionalLongOfScalarTypeProbesAs(String name){
        return squareRootOfPopulationStandardDeviationOptionalLongOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationOptionalLongOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationOptionalLongOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sampleVarianceOptionalLongOfScalarTypeProbes(){
        addRelationScalarAggregate("varSampOptionalLongOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceOptionalLongOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sampleVarianceOptionalLongOfScalarTypeProbesAs(String name){
        return sampleVarianceOptionalLongOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sampleVarianceOptionalLongOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceOptionalLongOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> samplePopulationVarianceOptionalLongOfScalarTypeProbes(){
        addRelationScalarAggregate("varPopOptionalLongOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceOptionalLongOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> samplePopulationVarianceOptionalLongOfScalarTypeProbesAs(String name){
        return samplePopulationVarianceOptionalLongOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> samplePopulationVarianceOptionalLongOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_LONG_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceOptionalLongOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> minRequiredDecimalOfScalarTypeProbes(){
        addRelationScalarAggregate("minRequiredDecimalOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minRequiredDecimalOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> minRequiredDecimalOfScalarTypeProbesAs(String name){
        return minRequiredDecimalOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> minRequiredDecimalOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minRequiredDecimalOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> maxRequiredDecimalOfScalarTypeProbes(){
        addRelationScalarAggregate("maxRequiredDecimalOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxRequiredDecimalOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> maxRequiredDecimalOfScalarTypeProbesAs(String name){
        return maxRequiredDecimalOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> maxRequiredDecimalOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxRequiredDecimalOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sumRequiredDecimalOfScalarTypeProbes(){
        addRelationScalarAggregate("sumRequiredDecimalOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumRequiredDecimalOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sumRequiredDecimalOfScalarTypeProbesAs(String name){
        return sumRequiredDecimalOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sumRequiredDecimalOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumRequiredDecimalOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> avgRequiredDecimalOfScalarTypeProbes(){
        addRelationScalarAggregate("avgRequiredDecimalOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgRequiredDecimalOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> avgRequiredDecimalOfScalarTypeProbesAs(String name){
        return avgRequiredDecimalOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> avgRequiredDecimalOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgRequiredDecimalOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> standardDeviationRequiredDecimalOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevRequiredDecimalOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationRequiredDecimalOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> standardDeviationRequiredDecimalOfScalarTypeProbesAs(String name){
        return standardDeviationRequiredDecimalOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> standardDeviationRequiredDecimalOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationRequiredDecimalOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationRequiredDecimalOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevPopRequiredDecimalOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationRequiredDecimalOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationRequiredDecimalOfScalarTypeProbesAs(String name){
        return squareRootOfPopulationStandardDeviationRequiredDecimalOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationRequiredDecimalOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationRequiredDecimalOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sampleVarianceRequiredDecimalOfScalarTypeProbes(){
        addRelationScalarAggregate("varSampRequiredDecimalOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceRequiredDecimalOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sampleVarianceRequiredDecimalOfScalarTypeProbesAs(String name){
        return sampleVarianceRequiredDecimalOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sampleVarianceRequiredDecimalOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceRequiredDecimalOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> samplePopulationVarianceRequiredDecimalOfScalarTypeProbes(){
        addRelationScalarAggregate("varPopRequiredDecimalOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceRequiredDecimalOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> samplePopulationVarianceRequiredDecimalOfScalarTypeProbesAs(String name){
        return samplePopulationVarianceRequiredDecimalOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> samplePopulationVarianceRequiredDecimalOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceRequiredDecimalOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> minOptionalDecimalOfScalarTypeProbes(){
        addRelationScalarAggregate("minOptionalDecimalOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minOptionalDecimalOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> minOptionalDecimalOfScalarTypeProbesAs(String name){
        return minOptionalDecimalOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> minOptionalDecimalOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minOptionalDecimalOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> maxOptionalDecimalOfScalarTypeProbes(){
        addRelationScalarAggregate("maxOptionalDecimalOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxOptionalDecimalOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> maxOptionalDecimalOfScalarTypeProbesAs(String name){
        return maxOptionalDecimalOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> maxOptionalDecimalOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxOptionalDecimalOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sumOptionalDecimalOfScalarTypeProbes(){
        addRelationScalarAggregate("sumOptionalDecimalOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumOptionalDecimalOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sumOptionalDecimalOfScalarTypeProbesAs(String name){
        return sumOptionalDecimalOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sumOptionalDecimalOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumOptionalDecimalOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> avgOptionalDecimalOfScalarTypeProbes(){
        addRelationScalarAggregate("avgOptionalDecimalOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgOptionalDecimalOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> avgOptionalDecimalOfScalarTypeProbesAs(String name){
        return avgOptionalDecimalOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> avgOptionalDecimalOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgOptionalDecimalOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> standardDeviationOptionalDecimalOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevOptionalDecimalOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationOptionalDecimalOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> standardDeviationOptionalDecimalOfScalarTypeProbesAs(String name){
        return standardDeviationOptionalDecimalOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> standardDeviationOptionalDecimalOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationOptionalDecimalOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationOptionalDecimalOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevPopOptionalDecimalOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationOptionalDecimalOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationOptionalDecimalOfScalarTypeProbesAs(String name){
        return squareRootOfPopulationStandardDeviationOptionalDecimalOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationOptionalDecimalOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationOptionalDecimalOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sampleVarianceOptionalDecimalOfScalarTypeProbes(){
        addRelationScalarAggregate("varSampOptionalDecimalOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceOptionalDecimalOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sampleVarianceOptionalDecimalOfScalarTypeProbesAs(String name){
        return sampleVarianceOptionalDecimalOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sampleVarianceOptionalDecimalOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceOptionalDecimalOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> samplePopulationVarianceOptionalDecimalOfScalarTypeProbes(){
        addRelationScalarAggregate("varPopOptionalDecimalOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceOptionalDecimalOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> samplePopulationVarianceOptionalDecimalOfScalarTypeProbesAs(String name){
        return samplePopulationVarianceOptionalDecimalOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> samplePopulationVarianceOptionalDecimalOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceOptionalDecimalOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> minRequiredFloatOfScalarTypeProbes(){
        addRelationScalarAggregate("minRequiredFloatOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minRequiredFloatOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> minRequiredFloatOfScalarTypeProbesAs(String name){
        return minRequiredFloatOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> minRequiredFloatOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minRequiredFloatOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> maxRequiredFloatOfScalarTypeProbes(){
        addRelationScalarAggregate("maxRequiredFloatOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxRequiredFloatOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> maxRequiredFloatOfScalarTypeProbesAs(String name){
        return maxRequiredFloatOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> maxRequiredFloatOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxRequiredFloatOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sumRequiredFloatOfScalarTypeProbes(){
        addRelationScalarAggregate("sumRequiredFloatOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumRequiredFloatOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sumRequiredFloatOfScalarTypeProbesAs(String name){
        return sumRequiredFloatOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sumRequiredFloatOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumRequiredFloatOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> avgRequiredFloatOfScalarTypeProbes(){
        addRelationScalarAggregate("avgRequiredFloatOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgRequiredFloatOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> avgRequiredFloatOfScalarTypeProbesAs(String name){
        return avgRequiredFloatOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> avgRequiredFloatOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgRequiredFloatOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> standardDeviationRequiredFloatOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevRequiredFloatOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationRequiredFloatOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> standardDeviationRequiredFloatOfScalarTypeProbesAs(String name){
        return standardDeviationRequiredFloatOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> standardDeviationRequiredFloatOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationRequiredFloatOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationRequiredFloatOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevPopRequiredFloatOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationRequiredFloatOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationRequiredFloatOfScalarTypeProbesAs(String name){
        return squareRootOfPopulationStandardDeviationRequiredFloatOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationRequiredFloatOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationRequiredFloatOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sampleVarianceRequiredFloatOfScalarTypeProbes(){
        addRelationScalarAggregate("varSampRequiredFloatOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceRequiredFloatOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sampleVarianceRequiredFloatOfScalarTypeProbesAs(String name){
        return sampleVarianceRequiredFloatOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sampleVarianceRequiredFloatOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceRequiredFloatOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> samplePopulationVarianceRequiredFloatOfScalarTypeProbes(){
        addRelationScalarAggregate("varPopRequiredFloatOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceRequiredFloatOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> samplePopulationVarianceRequiredFloatOfScalarTypeProbesAs(String name){
        return samplePopulationVarianceRequiredFloatOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> samplePopulationVarianceRequiredFloatOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceRequiredFloatOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> minOptionalFloatOfScalarTypeProbes(){
        addRelationScalarAggregate("minOptionalFloatOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minOptionalFloatOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> minOptionalFloatOfScalarTypeProbesAs(String name){
        return minOptionalFloatOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> minOptionalFloatOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minOptionalFloatOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> maxOptionalFloatOfScalarTypeProbes(){
        addRelationScalarAggregate("maxOptionalFloatOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxOptionalFloatOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> maxOptionalFloatOfScalarTypeProbesAs(String name){
        return maxOptionalFloatOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> maxOptionalFloatOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxOptionalFloatOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sumOptionalFloatOfScalarTypeProbes(){
        addRelationScalarAggregate("sumOptionalFloatOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumOptionalFloatOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sumOptionalFloatOfScalarTypeProbesAs(String name){
        return sumOptionalFloatOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sumOptionalFloatOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumOptionalFloatOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> avgOptionalFloatOfScalarTypeProbes(){
        addRelationScalarAggregate("avgOptionalFloatOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgOptionalFloatOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> avgOptionalFloatOfScalarTypeProbesAs(String name){
        return avgOptionalFloatOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> avgOptionalFloatOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgOptionalFloatOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> standardDeviationOptionalFloatOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevOptionalFloatOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationOptionalFloatOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> standardDeviationOptionalFloatOfScalarTypeProbesAs(String name){
        return standardDeviationOptionalFloatOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> standardDeviationOptionalFloatOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationOptionalFloatOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationOptionalFloatOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevPopOptionalFloatOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationOptionalFloatOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationOptionalFloatOfScalarTypeProbesAs(String name){
        return squareRootOfPopulationStandardDeviationOptionalFloatOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationOptionalFloatOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationOptionalFloatOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sampleVarianceOptionalFloatOfScalarTypeProbes(){
        addRelationScalarAggregate("varSampOptionalFloatOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceOptionalFloatOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sampleVarianceOptionalFloatOfScalarTypeProbesAs(String name){
        return sampleVarianceOptionalFloatOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sampleVarianceOptionalFloatOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceOptionalFloatOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> samplePopulationVarianceOptionalFloatOfScalarTypeProbes(){
        addRelationScalarAggregate("varPopOptionalFloatOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceOptionalFloatOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> samplePopulationVarianceOptionalFloatOfScalarTypeProbesAs(String name){
        return samplePopulationVarianceOptionalFloatOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> samplePopulationVarianceOptionalFloatOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceOptionalFloatOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> minRequiredDoubleOfScalarTypeProbes(){
        addRelationScalarAggregate("minRequiredDoubleOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minRequiredDoubleOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> minRequiredDoubleOfScalarTypeProbesAs(String name){
        return minRequiredDoubleOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> minRequiredDoubleOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minRequiredDoubleOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> maxRequiredDoubleOfScalarTypeProbes(){
        addRelationScalarAggregate("maxRequiredDoubleOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxRequiredDoubleOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> maxRequiredDoubleOfScalarTypeProbesAs(String name){
        return maxRequiredDoubleOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> maxRequiredDoubleOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxRequiredDoubleOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sumRequiredDoubleOfScalarTypeProbes(){
        addRelationScalarAggregate("sumRequiredDoubleOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumRequiredDoubleOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sumRequiredDoubleOfScalarTypeProbesAs(String name){
        return sumRequiredDoubleOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sumRequiredDoubleOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumRequiredDoubleOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> avgRequiredDoubleOfScalarTypeProbes(){
        addRelationScalarAggregate("avgRequiredDoubleOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgRequiredDoubleOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> avgRequiredDoubleOfScalarTypeProbesAs(String name){
        return avgRequiredDoubleOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> avgRequiredDoubleOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgRequiredDoubleOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> standardDeviationRequiredDoubleOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevRequiredDoubleOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationRequiredDoubleOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> standardDeviationRequiredDoubleOfScalarTypeProbesAs(String name){
        return standardDeviationRequiredDoubleOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> standardDeviationRequiredDoubleOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationRequiredDoubleOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationRequiredDoubleOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevPopRequiredDoubleOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationRequiredDoubleOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationRequiredDoubleOfScalarTypeProbesAs(String name){
        return squareRootOfPopulationStandardDeviationRequiredDoubleOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationRequiredDoubleOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationRequiredDoubleOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sampleVarianceRequiredDoubleOfScalarTypeProbes(){
        addRelationScalarAggregate("varSampRequiredDoubleOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceRequiredDoubleOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sampleVarianceRequiredDoubleOfScalarTypeProbesAs(String name){
        return sampleVarianceRequiredDoubleOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sampleVarianceRequiredDoubleOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceRequiredDoubleOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> samplePopulationVarianceRequiredDoubleOfScalarTypeProbes(){
        addRelationScalarAggregate("varPopRequiredDoubleOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceRequiredDoubleOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> samplePopulationVarianceRequiredDoubleOfScalarTypeProbesAs(String name){
        return samplePopulationVarianceRequiredDoubleOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> samplePopulationVarianceRequiredDoubleOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceRequiredDoubleOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> minOptionalDoubleOfScalarTypeProbes(){
        addRelationScalarAggregate("minOptionalDoubleOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minOptionalDoubleOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> minOptionalDoubleOfScalarTypeProbesAs(String name){
        return minOptionalDoubleOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> minOptionalDoubleOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minOptionalDoubleOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> maxOptionalDoubleOfScalarTypeProbes(){
        addRelationScalarAggregate("maxOptionalDoubleOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxOptionalDoubleOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> maxOptionalDoubleOfScalarTypeProbesAs(String name){
        return maxOptionalDoubleOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> maxOptionalDoubleOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxOptionalDoubleOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sumOptionalDoubleOfScalarTypeProbes(){
        addRelationScalarAggregate("sumOptionalDoubleOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumOptionalDoubleOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sumOptionalDoubleOfScalarTypeProbesAs(String name){
        return sumOptionalDoubleOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sumOptionalDoubleOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumOptionalDoubleOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> avgOptionalDoubleOfScalarTypeProbes(){
        addRelationScalarAggregate("avgOptionalDoubleOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgOptionalDoubleOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> avgOptionalDoubleOfScalarTypeProbesAs(String name){
        return avgOptionalDoubleOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> avgOptionalDoubleOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgOptionalDoubleOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> standardDeviationOptionalDoubleOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevOptionalDoubleOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationOptionalDoubleOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> standardDeviationOptionalDoubleOfScalarTypeProbesAs(String name){
        return standardDeviationOptionalDoubleOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> standardDeviationOptionalDoubleOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationOptionalDoubleOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationOptionalDoubleOfScalarTypeProbes(){
        addRelationScalarAggregate("stdDevPopOptionalDoubleOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationOptionalDoubleOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationOptionalDoubleOfScalarTypeProbesAs(String name){
        return squareRootOfPopulationStandardDeviationOptionalDoubleOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> squareRootOfPopulationStandardDeviationOptionalDoubleOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationOptionalDoubleOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> sampleVarianceOptionalDoubleOfScalarTypeProbes(){
        addRelationScalarAggregate("varSampOptionalDoubleOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceOptionalDoubleOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> sampleVarianceOptionalDoubleOfScalarTypeProbesAs(String name){
        return sampleVarianceOptionalDoubleOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> sampleVarianceOptionalDoubleOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceOptionalDoubleOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }
    public TypePlatformRequest<T> samplePopulationVarianceOptionalDoubleOfScalarTypeProbes(){
        addRelationScalarAggregate("varPopOptionalDoubleOfScalarTypeProbes", Q.scalarTypeProbes().unlimited(),
            ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceOptionalDoubleOfScalarTypeProbes", null, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }

    public TypePlatformRequest<T> samplePopulationVarianceOptionalDoubleOfScalarTypeProbesAs(String name){
        return samplePopulationVarianceOptionalDoubleOfScalarTypeProbesAs(name, Q.scalarTypeProbes().unlimited());
    }

    public TypePlatformRequest<T> samplePopulationVarianceOptionalDoubleOfScalarTypeProbesAs(String name, ScalarTypeProbeRequest subRequest){
        addRelationScalarAggregate(name, subRequest, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY,
            ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceOptionalDoubleOfScalarTypeProbesAs", name, TypePlatform.SCALAR_TYPE_PROBE_LIST_PROPERTY));
        return this;
    }



    /**
     * get topN records
     * @param topN  records number
     */
    public TypePlatformRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public TypePlatformRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public TypePlatformRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public TypePlatformRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public TypePlatformRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public TypePlatformRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}