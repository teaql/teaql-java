
package com.example.schoolmanagementservice.schoolcapacitysummary;

import com.example.schoolmanagementservice.Q;
import com.example.schoolmanagementservice.platform.Platform;
import com.example.schoolmanagementservice.platform.PlatformRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.util.Objects;

public class SchoolCapacitySummaryRequest<T extends SchoolCapacitySummary> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public SchoolCapacitySummaryRequest(Class<T> returnType){
        this(returnType, () -> (T) new SchoolCapacitySummary());
    }

    protected SchoolCapacitySummaryRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public SchoolCapacitySummaryRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public SchoolCapacitySummaryRequest<T> copy(){
        return (SchoolCapacitySummaryRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new SchoolCapacitySummaryRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public SchoolCapacitySummaryRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public SchoolCapacitySummaryRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public SchoolCapacitySummaryRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public SchoolCapacitySummaryRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public SchoolCapacitySummaryRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public SchoolCapacitySummaryRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public SchoolCapacitySummaryRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public SchoolCapacitySummaryRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public SchoolCapacitySummaryRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public SchoolCapacitySummaryRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (SchoolCapacitySummaryRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public SchoolCapacitySummaryRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public SchoolCapacitySummaryRequest<T> matchingAnyOf(SchoolCapacitySummaryRequest schoolCapacitySummary){
        super.internalMatchAny(schoolCapacitySummary);
        return this;
    }

    public SchoolCapacitySummaryRequest<T> matchingIdsOf(SchoolCapacitySummaryRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                SchoolCapacitySummary.ID_PROPERTY, request, SchoolCapacitySummary.ID_PROPERTY));
    }

    public SchoolCapacitySummaryRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public SchoolCapacitySummaryRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public SchoolCapacitySummaryRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public SchoolCapacitySummaryRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectPlatformIdOnly().selectName().selectTotalCapacity().selectSchoolCount().selectVersion();
    }

    public SchoolCapacitySummaryRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public SchoolCapacitySummaryRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(SchoolCapacitySummary.ID_PROPERTY);
        selectProperty(SchoolCapacitySummary.PLATFORM_PROPERTY);
        selectProperty(SchoolCapacitySummary.NAME_PROPERTY);
        selectProperty(SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY);
        selectProperty(SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY);
        selectProperty(SchoolCapacitySummary.VERSION_PROPERTY);
        return this;
    }

    public SchoolCapacitySummaryRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectPlatform().selectName().selectTotalCapacity().selectSchoolCount().selectVersion();
    }

    public SchoolCapacitySummaryRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectPlatform().selectName().selectTotalCapacity().selectSchoolCount().selectVersion();
    }


    public SchoolCapacitySummaryRequest<T> selectId(){
       selectProperty(SchoolCapacitySummary.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SchoolCapacitySummaryRequest<T> unselectId(){
       unselectProperty(SchoolCapacitySummary.ID_PROPERTY);
       return this;
    }
    public SchoolCapacitySummaryRequest<T> selectPlatformIdOnly(){
       selectProperty(SchoolCapacitySummary.PLATFORM_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> selectPlatform(){
        return selectPlatformWith(Q.platforms().unlimited().selectSelf());
    }

    public SchoolCapacitySummaryRequest<T> selectPlatformWith(PlatformRequest platform){
       selectProperty(SchoolCapacitySummary.PLATFORM_PROPERTY);
       enhanceRelation(SchoolCapacitySummary.PLATFORM_PROPERTY, platform,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectPlatformWith", null, SchoolCapacitySummary.PLATFORM_PROPERTY));
       return this;
    }

    public SchoolCapacitySummaryRequest<T> unselectPlatform(){
       unselectProperty(SchoolCapacitySummary.PLATFORM_PROPERTY);
       return this;
    }
    public SchoolCapacitySummaryRequest<T> selectName(){
       selectProperty(SchoolCapacitySummary.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SchoolCapacitySummaryRequest<T> unselectName(){
       unselectProperty(SchoolCapacitySummary.NAME_PROPERTY);
       return this;
    }
    public SchoolCapacitySummaryRequest<T> selectTotalCapacity(){
       selectProperty(SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY);
       return this;
    }

    /**
     * fill the totalCapacity with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  totalCapacity) to fetch totalCapacity property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the totalCapacity with customized aggrFunction, TEAQL uses ({aggrFunction}(totalCapacity) AS totalCapacity to fetch totalCapacity property.
     * @param aggrFunction  aggrFunction
     */
    public SchoolCapacitySummaryRequest<T> selectTotalCapacity(AggrFunction aggrFunction){
       selectProperty(SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY, aggrFunction);
       return this;
    }


    public SchoolCapacitySummaryRequest<T> unselectTotalCapacity(){
       unselectProperty(SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY);
       return this;
    }
    public SchoolCapacitySummaryRequest<T> selectSchoolCount(){
       selectProperty(SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY);
       return this;
    }

    /**
     * fill the schoolCount with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  schoolCount) to fetch schoolCount property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the schoolCount with customized aggrFunction, TEAQL uses ({aggrFunction}(schoolCount) AS schoolCount to fetch schoolCount property.
     * @param aggrFunction  aggrFunction
     */
    public SchoolCapacitySummaryRequest<T> selectSchoolCount(AggrFunction aggrFunction){
       selectProperty(SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY, aggrFunction);
       return this;
    }


    public SchoolCapacitySummaryRequest<T> unselectSchoolCount(){
       unselectProperty(SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY);
       return this;
    }
    public SchoolCapacitySummaryRequest<T> selectVersion(){
       selectProperty(SchoolCapacitySummary.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SchoolCapacitySummaryRequest<T> unselectVersion(){
       unselectProperty(SchoolCapacitySummary.VERSION_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SchoolCapacitySummary.ID_PROPERTY, operator, values);
    }

    public SchoolCapacitySummaryRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public SchoolCapacitySummaryRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public SchoolCapacitySummaryRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public SchoolCapacitySummaryRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public SchoolCapacitySummaryRequest<T> filterByPlatform(Platform... platform){
      if (platform == null || platform.length == 0) {
        throw new IllegalArgumentException("filterByPlatform parameter platform cannot be empty");
      }
      return appendSearchCriteria(createPlatformCriteria(Operator.EQUAL, (Object[])platform));
    }

    public SchoolCapacitySummaryRequest<T> withPlatform(Operator operator, Object... values){
       return appendSearchCriteria(createPlatformCriteria(operator, values));
    }

    public SchoolCapacitySummaryRequest<T> withPlatformIsUnknown(){
       return withPlatform(Operator.IS_NULL);
    }

    public SchoolCapacitySummaryRequest<T> withPlatformIsKnown(){
       return withPlatform(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createPlatformCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SchoolCapacitySummary.PLATFORM_PROPERTY, operator, values);
    }

    public SchoolCapacitySummaryRequest<T> filterByPlatform(Long platform){
      if(platform == null){
         return this;
      }
      return withPlatform(Operator.EQUAL, platform);
    }
    public SchoolCapacitySummaryRequest<T> withPlatformMatching(PlatformRequest platform){
       return appendSearchCriteria(new SubQuerySearchCriteria(SchoolCapacitySummary.PLATFORM_PROPERTY, platform, Platform.ID_PROPERTY));
    }

    public SchoolCapacitySummaryRequest<T> withoutPlatformMatching(PlatformRequest platform){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(SchoolCapacitySummary.PLATFORM_PROPERTY, platform, Platform.ID_PROPERTY)));
    }

    public SchoolCapacitySummaryRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public SchoolCapacitySummaryRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public SchoolCapacitySummaryRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public SchoolCapacitySummaryRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SchoolCapacitySummary.NAME_PROPERTY, operator, values);
    }

    public SchoolCapacitySummaryRequest<T> withNameIsNot(String name){
       return withName(Operator.NOT_EQUAL, name);
    }

    public SchoolCapacitySummaryRequest<T> withNameIn(String... name){
       return withName(Operator.IN, (Object[])name);
    }

    public SchoolCapacitySummaryRequest<T> withNameNotIn(String... name){
       return withName(Operator.NOT_IN, (Object[])name);
    }
    public SchoolCapacitySummaryRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public SchoolCapacitySummaryRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public SchoolCapacitySummaryRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public SchoolCapacitySummaryRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public SchoolCapacitySummaryRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public SchoolCapacitySummaryRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public SchoolCapacitySummaryRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public SchoolCapacitySummaryRequest<T> withNameNotContaining(String name){
       return withName(Operator.NOT_CONTAIN, name);
    }

    public SchoolCapacitySummaryRequest<T> withNameNotStartingWith(String name){
       return withName(Operator.NOT_BEGIN_WITH, name);
    }

    public SchoolCapacitySummaryRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public SchoolCapacitySummaryRequest<T> withNameNotEndingWith(String name){
       return withName(Operator.NOT_END_WITH, name);
    }

    public SchoolCapacitySummaryRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public SchoolCapacitySummaryRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public SchoolCapacitySummaryRequest<T> filterByTotalCapacity(Integer... totalCapacity){
      if (totalCapacity == null || totalCapacity.length == 0) {
        throw new IllegalArgumentException("filterByTotalCapacity parameter totalCapacity cannot be empty");
      }
      return appendSearchCriteria(createTotalCapacityCriteria(Operator.EQUAL, (Object[])totalCapacity));
    }

    public SchoolCapacitySummaryRequest<T> withTotalCapacity(Operator operator, Object... values){
       return appendSearchCriteria(createTotalCapacityCriteria(operator, values));
    }

    public SchoolCapacitySummaryRequest<T> withTotalCapacityIsUnknown(){
       return withTotalCapacity(Operator.IS_NULL);
    }

    public SchoolCapacitySummaryRequest<T> withTotalCapacityIsKnown(){
       return withTotalCapacity(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createTotalCapacityCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY, operator, values);
    }

    public SchoolCapacitySummaryRequest<T> withTotalCapacityIs(Integer totalCapacity){
       return withTotalCapacity(Operator.EQUAL, totalCapacity);
    }

    public SchoolCapacitySummaryRequest<T> withTotalCapacityIsNot(Integer totalCapacity){
       return withTotalCapacity(Operator.NOT_EQUAL, totalCapacity);
    }

    public SchoolCapacitySummaryRequest<T> withTotalCapacityIn(Integer... totalCapacity){
       return withTotalCapacity(Operator.IN, (Object[])totalCapacity);
    }

    public SchoolCapacitySummaryRequest<T> withTotalCapacityNotIn(Integer... totalCapacity){
       return withTotalCapacity(Operator.NOT_IN, (Object[])totalCapacity);
    }
    public SchoolCapacitySummaryRequest<T> withTotalCapacityGreaterThan(Integer totalCapacity){
       return withTotalCapacity(Operator.GREATER_THAN, totalCapacity);
    }

    public SchoolCapacitySummaryRequest<T> withTotalCapacityGreaterThanOrEqualTo(Integer totalCapacity){
       return withTotalCapacity(Operator.GREATER_THAN_OR_EQUAL, totalCapacity);
    }

    public SchoolCapacitySummaryRequest<T> withTotalCapacityLessThan(Integer totalCapacity){
       return withTotalCapacity(Operator.LESS_THAN, totalCapacity);
    }

    public SchoolCapacitySummaryRequest<T> withTotalCapacityLessThanOrEqualTo(Integer totalCapacity){
       return withTotalCapacity(Operator.LESS_THAN_OR_EQUAL, totalCapacity);
    }

    public SchoolCapacitySummaryRequest<T> withTotalCapacityBetween(Integer startOfTotalCapacity, Integer endOfTotalCapacity){
       return withTotalCapacity(Operator.BETWEEN, startOfTotalCapacity, endOfTotalCapacity);
    }



    public SchoolCapacitySummaryRequest<T> filterBySchoolCount(Integer... schoolCount){
      if (schoolCount == null || schoolCount.length == 0) {
        throw new IllegalArgumentException("filterBySchoolCount parameter schoolCount cannot be empty");
      }
      return appendSearchCriteria(createSchoolCountCriteria(Operator.EQUAL, (Object[])schoolCount));
    }

    public SchoolCapacitySummaryRequest<T> withSchoolCount(Operator operator, Object... values){
       return appendSearchCriteria(createSchoolCountCriteria(operator, values));
    }

    public SchoolCapacitySummaryRequest<T> withSchoolCountIsUnknown(){
       return withSchoolCount(Operator.IS_NULL);
    }

    public SchoolCapacitySummaryRequest<T> withSchoolCountIsKnown(){
       return withSchoolCount(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createSchoolCountCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY, operator, values);
    }

    public SchoolCapacitySummaryRequest<T> withSchoolCountIs(Integer schoolCount){
       return withSchoolCount(Operator.EQUAL, schoolCount);
    }

    public SchoolCapacitySummaryRequest<T> withSchoolCountIsNot(Integer schoolCount){
       return withSchoolCount(Operator.NOT_EQUAL, schoolCount);
    }

    public SchoolCapacitySummaryRequest<T> withSchoolCountIn(Integer... schoolCount){
       return withSchoolCount(Operator.IN, (Object[])schoolCount);
    }

    public SchoolCapacitySummaryRequest<T> withSchoolCountNotIn(Integer... schoolCount){
       return withSchoolCount(Operator.NOT_IN, (Object[])schoolCount);
    }
    public SchoolCapacitySummaryRequest<T> withSchoolCountGreaterThan(Integer schoolCount){
       return withSchoolCount(Operator.GREATER_THAN, schoolCount);
    }

    public SchoolCapacitySummaryRequest<T> withSchoolCountGreaterThanOrEqualTo(Integer schoolCount){
       return withSchoolCount(Operator.GREATER_THAN_OR_EQUAL, schoolCount);
    }

    public SchoolCapacitySummaryRequest<T> withSchoolCountLessThan(Integer schoolCount){
       return withSchoolCount(Operator.LESS_THAN, schoolCount);
    }

    public SchoolCapacitySummaryRequest<T> withSchoolCountLessThanOrEqualTo(Integer schoolCount){
       return withSchoolCount(Operator.LESS_THAN_OR_EQUAL, schoolCount);
    }

    public SchoolCapacitySummaryRequest<T> withSchoolCountBetween(Integer startOfSchoolCount, Integer endOfSchoolCount){
       return withSchoolCount(Operator.BETWEEN, startOfSchoolCount, endOfSchoolCount);
    }



    public SchoolCapacitySummaryRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public SchoolCapacitySummaryRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public SchoolCapacitySummaryRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public SchoolCapacitySummaryRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SchoolCapacitySummary.VERSION_PROPERTY, operator, values);
    }

    public SchoolCapacitySummaryRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public SchoolCapacitySummaryRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public SchoolCapacitySummaryRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public SchoolCapacitySummaryRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public SchoolCapacitySummaryRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public SchoolCapacitySummaryRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public SchoolCapacitySummaryRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public SchoolCapacitySummaryRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public SchoolCapacitySummaryRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }



    public SchoolCapacitySummaryRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public SchoolCapacitySummaryRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public SchoolCapacitySummaryRequest minTotalCapacity(){
        return minTotalCapacityAs(prefix("minOf",SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY));
    }

    public SchoolCapacitySummaryRequest minTotalCapacityAs(String retName){
        super.min(retName, SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest maxTotalCapacity(){
        return maxTotalCapacityAs(prefix("maxOf",SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY));
    }

    public SchoolCapacitySummaryRequest maxTotalCapacityAs(String retName){
        super.max(retName, SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest sumTotalCapacity(){
        return sumTotalCapacityAs(prefix("sumOf",SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY));
    }

    public SchoolCapacitySummaryRequest sumTotalCapacityAs(String retName){
        super.sum(retName, SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest avgTotalCapacity(){
        return avgTotalCapacityAs(prefix("avgOf",SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY));
    }

    public SchoolCapacitySummaryRequest avgTotalCapacityAs(String retName){
        super.avg(retName, SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest standardDeviationTotalCapacity(){
        return standardDeviationTotalCapacityAs(prefix("standardDeviationOf",SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY));
    }

    public SchoolCapacitySummaryRequest standardDeviationTotalCapacityAs(String retName){
        super.standardDeviation(retName, SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest squareRootOfPopulationStandardDeviationTotalCapacity(){
        return squareRootOfPopulationStandardDeviationTotalCapacityAs(prefix("squareRootOfPopulationStandardDeviationOf",SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY));
    }

    public SchoolCapacitySummaryRequest squareRootOfPopulationStandardDeviationTotalCapacityAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest sampleVarianceTotalCapacity(){
        return sampleVarianceTotalCapacityAs(prefix("sampleVarianceOf",SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY));
    }

    public SchoolCapacitySummaryRequest sampleVarianceTotalCapacityAs(String retName){
        super.sampleVariance(retName, SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest samplePopulationVarianceTotalCapacity(){
        return samplePopulationVarianceTotalCapacityAs(prefix("samplePopulationVarianceOf",SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY));
    }

    public SchoolCapacitySummaryRequest samplePopulationVarianceTotalCapacityAs(String retName){
        super.samplePopulationVariance(retName, SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest minSchoolCount(){
        return minSchoolCountAs(prefix("minOf",SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY));
    }

    public SchoolCapacitySummaryRequest minSchoolCountAs(String retName){
        super.min(retName, SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest maxSchoolCount(){
        return maxSchoolCountAs(prefix("maxOf",SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY));
    }

    public SchoolCapacitySummaryRequest maxSchoolCountAs(String retName){
        super.max(retName, SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest sumSchoolCount(){
        return sumSchoolCountAs(prefix("sumOf",SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY));
    }

    public SchoolCapacitySummaryRequest sumSchoolCountAs(String retName){
        super.sum(retName, SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest avgSchoolCount(){
        return avgSchoolCountAs(prefix("avgOf",SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY));
    }

    public SchoolCapacitySummaryRequest avgSchoolCountAs(String retName){
        super.avg(retName, SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest standardDeviationSchoolCount(){
        return standardDeviationSchoolCountAs(prefix("standardDeviationOf",SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY));
    }

    public SchoolCapacitySummaryRequest standardDeviationSchoolCountAs(String retName){
        super.standardDeviation(retName, SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest squareRootOfPopulationStandardDeviationSchoolCount(){
        return squareRootOfPopulationStandardDeviationSchoolCountAs(prefix("squareRootOfPopulationStandardDeviationOf",SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY));
    }

    public SchoolCapacitySummaryRequest squareRootOfPopulationStandardDeviationSchoolCountAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest sampleVarianceSchoolCount(){
        return sampleVarianceSchoolCountAs(prefix("sampleVarianceOf",SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY));
    }

    public SchoolCapacitySummaryRequest sampleVarianceSchoolCountAs(String retName){
        super.sampleVariance(retName, SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest samplePopulationVarianceSchoolCount(){
        return samplePopulationVarianceSchoolCountAs(prefix("samplePopulationVarianceOf",SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY));
    }

    public SchoolCapacitySummaryRequest samplePopulationVarianceSchoolCountAs(String retName){
        super.samplePopulationVariance(retName, SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest<T> groupByPlatformWithDetails(){
       return groupByPlatformWithDetails(Q.platforms().unlimited());
    }

    public SchoolCapacitySummaryRequest<T> groupByPlatformWithDetails(PlatformRequest subRequest){
       aggregate(SchoolCapacitySummary.PLATFORM_PROPERTY, subRequest);
       return this;
    }





    public SchoolCapacitySummaryRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public SchoolCapacitySummaryRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, SchoolCapacitySummary.ID_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest<T> countDistinctPlatform(){
        return countDistinctPlatformAs("platform_distinct_count");
    }

    public SchoolCapacitySummaryRequest<T> countDistinctPlatformAs(String alias){
        super.countDistinctProperty(alias, SchoolCapacitySummary.PLATFORM_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest<T> countDistinctName(){
        return countDistinctNameAs("name_distinct_count");
    }

    public SchoolCapacitySummaryRequest<T> countDistinctNameAs(String alias){
        super.countDistinctProperty(alias, SchoolCapacitySummary.NAME_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest<T> countDistinctTotalCapacity(){
        return countDistinctTotalCapacityAs("total_capacity_distinct_count");
    }

    public SchoolCapacitySummaryRequest<T> countDistinctTotalCapacityAs(String alias){
        super.countDistinctProperty(alias, SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest<T> countDistinctSchoolCount(){
        return countDistinctSchoolCountAs("school_count_distinct_count");
    }

    public SchoolCapacitySummaryRequest<T> countDistinctSchoolCountAs(String alias){
        super.countDistinctProperty(alias, SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY);
        return this;
    }
    public SchoolCapacitySummaryRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public SchoolCapacitySummaryRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, SchoolCapacitySummary.VERSION_PROPERTY);
        return this;
    }

    public SchoolCapacitySummaryRequest<T> groupById(){
       groupBy(SchoolCapacitySummary.ID_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> groupByIdAs(String retName){
       groupBy(retName, SchoolCapacitySummary.ID_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, SchoolCapacitySummary.ID_PROPERTY, function);
       return this;
    }
    public SchoolCapacitySummaryRequest<T> groupByPlatformWith(PlatformRequest subRequest){
       groupBy(SchoolCapacitySummary.PLATFORM_PROPERTY, subRequest,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "groupByPlatformWith", null, "platform"));
       return this;
    }
    public SchoolCapacitySummaryRequest<T> groupByPlatform(){
       groupBy(SchoolCapacitySummary.PLATFORM_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> groupByPlatformAs(String retName){
       groupBy(retName, SchoolCapacitySummary.PLATFORM_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> groupByPlatformWithFunction(String retName, AggrFunction function){
       groupBy(retName, SchoolCapacitySummary.PLATFORM_PROPERTY, function);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> groupByName(){
       groupBy(SchoolCapacitySummary.NAME_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> groupByNameAs(String retName){
       groupBy(retName, SchoolCapacitySummary.NAME_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, SchoolCapacitySummary.NAME_PROPERTY, function);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> groupByTotalCapacity(){
       groupBy(SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> groupByTotalCapacityAs(String retName){
       groupBy(retName, SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> groupByTotalCapacityWithFunction(String retName, AggrFunction function){
       groupBy(retName, SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY, function);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> groupBySchoolCount(){
       groupBy(SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> groupBySchoolCountAs(String retName){
       groupBy(retName, SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> groupBySchoolCountWithFunction(String retName, AggrFunction function){
       groupBy(retName, SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY, function);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> groupByVersion(){
       groupBy(SchoolCapacitySummary.VERSION_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> groupByVersionAs(String retName){
       groupBy(retName, SchoolCapacitySummary.VERSION_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, SchoolCapacitySummary.VERSION_PROPERTY, function);
       return this;
    }



    public SchoolCapacitySummaryRequest<T> orderByIdAscending(){
       addOrderByAscending(SchoolCapacitySummary.ID_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> orderByIdDescending(){
       addOrderByDescending(SchoolCapacitySummary.ID_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> orderByPlatformAscending(){
       addOrderByAscending(SchoolCapacitySummary.PLATFORM_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> orderByPlatformDescending(){
       addOrderByDescending(SchoolCapacitySummary.PLATFORM_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> orderByNameAscending(){
       addOrderByAscending(SchoolCapacitySummary.NAME_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> orderByNameDescending(){
       addOrderByDescending(SchoolCapacitySummary.NAME_PROPERTY);
       return this;
    }
    public SchoolCapacitySummaryRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(SchoolCapacitySummary.NAME_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(SchoolCapacitySummary.NAME_PROPERTY);
       return this;
    }
    public SchoolCapacitySummaryRequest<T> orderByTotalCapacityAscending(){
       addOrderByAscending(SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> orderByTotalCapacityDescending(){
       addOrderByDescending(SchoolCapacitySummary.TOTAL_CAPACITY_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> orderBySchoolCountAscending(){
       addOrderByAscending(SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> orderBySchoolCountDescending(){
       addOrderByDescending(SchoolCapacitySummary.SCHOOL_COUNT_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> orderByVersionAscending(){
       addOrderByAscending(SchoolCapacitySummary.VERSION_PROPERTY);
       return this;
    }

    public SchoolCapacitySummaryRequest<T> orderByVersionDescending(){
       addOrderByDescending(SchoolCapacitySummary.VERSION_PROPERTY);
       return this;
    }


    public PlatformRequest rollUpToPlatform(){
       PlatformRequest platform = Q.platforms().unlimited();
       this.internalRollUpTo("platform", platform,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "rollUpToPlatform", null, "platform"));
       return platform;
    }






   public SchoolCapacitySummaryRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform){
       return facetByPlatformAs(facetName, platform, true);
   }

   public SchoolCapacitySummaryRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform, boolean includeAllFacets){
       addFacet(facetName, SchoolCapacitySummary.PLATFORM_PROPERTY, platform, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByPlatformAs", facetName, SchoolCapacitySummary.PLATFORM_PROPERTY));
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public SchoolCapacitySummaryRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public SchoolCapacitySummaryRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public SchoolCapacitySummaryRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public SchoolCapacitySummaryRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public SchoolCapacitySummaryRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public SchoolCapacitySummaryRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}