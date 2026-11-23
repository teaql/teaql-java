
package com.example.inheritancematchingservice.college;

import com.example.inheritancematchingservice.platform.Platform;
import com.example.inheritancematchingservice.platform.PlatformRequest;
import com.example.inheritancematchingservice.school.SchoolRequest;
import com.example.inheritancematchingservice.schooltype.SchoolType;
import com.example.inheritancematchingservice.schooltype.SchoolTypeRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.util.Objects;

public class CollegeRequest<T extends College> extends SchoolRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public CollegeRequest(Class<T> returnType){
        this(returnType, () -> (T) new College());
    }

    protected CollegeRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public CollegeRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public CollegeRequest<T> copy(){
        return (CollegeRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new CollegeRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public CollegeRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public CollegeRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public CollegeRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public CollegeRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public CollegeRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public CollegeRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public CollegeRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public CollegeRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public CollegeRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public CollegeRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (CollegeRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public CollegeRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public CollegeRequest<T> matchingAnyOf(CollegeRequest college){
        super.internalMatchAny(college);
        return this;
    }

    public CollegeRequest<T> matchingIdsOf(CollegeRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                College.ID_PROPERTY, request, College.ID_PROPERTY));
    }

    public CollegeRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public CollegeRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public CollegeRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public CollegeRequest<T> selectSelf(){
        super.selectSelf();
        return selectDegreeLevel();
    }

    public CollegeRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public CollegeRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(College.DEGREE_LEVEL_PROPERTY);
        return this;
    }

    public CollegeRequest<T> selectAll(){
        super.selectAll();
        return selectDegreeLevel();
    }

    public CollegeRequest<T> selectChildren(){
        super.selectAny();
        return selectDegreeLevel();
    }


    public CollegeRequest<T> selectDegreeLevel(){
       selectProperty(College.DEGREE_LEVEL_PROPERTY);
       return this;
    }

    /**
     * fill the degreeLevel with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  degreeLevel) to fetch degreeLevel property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CollegeRequest<T> unselectDegreeLevel(){
       unselectProperty(College.DEGREE_LEVEL_PROPERTY);
       return this;
    }
    public CollegeRequest<T> selectPlatformIdOnly(){
       super.selectPlatformIdOnly();
       return this;
    }

    public CollegeRequest<T> selectPlatform(){
        super.selectPlatform();
        return this;
    }

    public CollegeRequest<T> selectPlatformWith(PlatformRequest platform){
       super.selectPlatformWith(platform);
       return this;
    }

    public CollegeRequest<T> unselectPlatform(){
       super.unselectPlatform();
       return this;
    }
    public CollegeRequest<T> selectSchoolTypeIdOnly(){
       super.selectSchoolTypeIdOnly();
       return this;
    }

    public CollegeRequest<T> selectSchoolType(){
        super.selectSchoolType();
        return this;
    }

    public CollegeRequest<T> selectSchoolTypeWith(SchoolTypeRequest schoolType){
       super.selectSchoolTypeWith(schoolType);
       return this;
    }

    public CollegeRequest<T> unselectSchoolType(){
       super.unselectSchoolType();
       return this;
    }
    public CollegeRequest<T> selectName(){
       super.selectName();
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CollegeRequest<T> unselectName(){
       super.unselectName();
       return this;
    }
    public CollegeRequest<T> selectStudentCapacity(){
       super.selectStudentCapacity();
       return this;
    }

    /**
     * fill the studentCapacity with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  studentCapacity) to fetch studentCapacity property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the studentCapacity with customized aggrFunction, TEAQL uses ({aggrFunction}(studentCapacity) AS studentCapacity to fetch studentCapacity property.
     * @param aggrFunction  aggrFunction
     */
    public CollegeRequest<T> selectStudentCapacity(AggrFunction aggrFunction){
       super.selectStudentCapacity(aggrFunction);
       return this;
    }


    public CollegeRequest<T> unselectStudentCapacity(){
       super.unselectStudentCapacity();
       return this;
    }
    public CollegeRequest<T> selectVersion(){
       super.selectVersion();
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CollegeRequest<T> unselectVersion(){
       super.unselectVersion();
       return this;
    }
    public CollegeRequest<T> selectId(){
       super.selectId();
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CollegeRequest<T> unselectId(){
       super.unselectId();
       return this;
    }

    public CollegeRequest<T> filterByDegreeLevel(String... degreeLevel){
      if (degreeLevel == null || degreeLevel.length == 0) {
        throw new IllegalArgumentException("filterByDegreeLevel parameter degreeLevel cannot be empty");
      }
      return appendSearchCriteria(createDegreeLevelCriteria(Operator.EQUAL, (Object[])degreeLevel));
    }

    public CollegeRequest<T> withDegreeLevel(Operator operator, Object... values){
       return appendSearchCriteria(createDegreeLevelCriteria(operator, values));
    }

    public CollegeRequest<T> withDegreeLevelIsUnknown(){
       return withDegreeLevel(Operator.IS_NULL);
    }

    public CollegeRequest<T> withDegreeLevelIsKnown(){
       return withDegreeLevel(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createDegreeLevelCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(College.DEGREE_LEVEL_PROPERTY, operator, values);
    }

    public CollegeRequest<T> withDegreeLevelIsNot(String degreeLevel){
       return withDegreeLevel(Operator.NOT_EQUAL, degreeLevel);
    }

    public CollegeRequest<T> withDegreeLevelIn(String... degreeLevel){
       return withDegreeLevel(Operator.IN, (Object[])degreeLevel);
    }

    public CollegeRequest<T> withDegreeLevelNotIn(String... degreeLevel){
       return withDegreeLevel(Operator.NOT_IN, (Object[])degreeLevel);
    }
    public CollegeRequest<T> withDegreeLevelGreaterThan(String degreeLevel){
       return withDegreeLevel(Operator.GREATER_THAN, degreeLevel);
    }

    public CollegeRequest<T> withDegreeLevelGreaterThanOrEqualTo(String degreeLevel){
       return withDegreeLevel(Operator.GREATER_THAN_OR_EQUAL, degreeLevel);
    }

    public CollegeRequest<T> withDegreeLevelLessThan(String degreeLevel){
       return withDegreeLevel(Operator.LESS_THAN, degreeLevel);
    }

    public CollegeRequest<T> withDegreeLevelLessThanOrEqualTo(String degreeLevel){
       return withDegreeLevel(Operator.LESS_THAN_OR_EQUAL, degreeLevel);
    }

    public CollegeRequest<T> withDegreeLevelBetween(String startOfDegreeLevel, String endOfDegreeLevel){
       return withDegreeLevel(Operator.BETWEEN, startOfDegreeLevel, endOfDegreeLevel);
    }
    public CollegeRequest<T> withDegreeLevelStartingWith(String degreeLevel){
       return withDegreeLevel(Operator.BEGIN_WITH, degreeLevel);
    }
    public CollegeRequest<T> withDegreeLevelContaining(String degreeLevel){
       return withDegreeLevel(Operator.CONTAIN, degreeLevel);
    }

    public CollegeRequest<T> withDegreeLevelNotContaining(String degreeLevel){
       return withDegreeLevel(Operator.NOT_CONTAIN, degreeLevel);
    }

    public CollegeRequest<T> withDegreeLevelNotStartingWith(String degreeLevel){
       return withDegreeLevel(Operator.NOT_BEGIN_WITH, degreeLevel);
    }

    public CollegeRequest<T> withDegreeLevelEndingWith(String degreeLevel){
       return withDegreeLevel(Operator.END_WITH, degreeLevel);
    }

    public CollegeRequest<T> withDegreeLevelNotEndingWith(String degreeLevel){
       return withDegreeLevel(Operator.NOT_END_WITH, degreeLevel);
    }

    public CollegeRequest<T> withDegreeLevelIs(String degreeLevel){
       return withDegreeLevel(Operator.EQUAL, degreeLevel);
    }

    public CollegeRequest<T> withDegreeLevelSoundingLike(String degreeLevel){
       return withDegreeLevel(Operator.SOUNDS_LIKE, degreeLevel);
    }



    public CollegeRequest<T> filterByPlatform(Platform platform){
      return (CollegeRequest<T>)super.filterByPlatform(platform);
    }

    public CollegeRequest<T> withPlatform(Operator operator, Object... values){
       return (CollegeRequest<T>)super.withPlatform(operator, values);
    }

    public CollegeRequest<T> withPlatformIsUnknown(){
       return (CollegeRequest<T>)super.withPlatformIsUnknown();
    }

    public CollegeRequest<T> withPlatformIsKnown(){
       return (CollegeRequest<T>)super.withPlatformIsKnown();
    }

    public CollegeRequest<T> filterByPlatform(Long platform){
      return (CollegeRequest<T>)super.filterByPlatform(platform);
    }

    public CollegeRequest<T> withPlatformMatching(PlatformRequest platform){
       return (CollegeRequest<T>)super.withPlatformMatching(platform);
    }

    public CollegeRequest<T> filterBySchoolType(SchoolType schoolType){
      return (CollegeRequest<T>)super.filterBySchoolType(schoolType);
    }

    public CollegeRequest<T> withSchoolType(Operator operator, Object... values){
       return (CollegeRequest<T>)super.withSchoolType(operator, values);
    }

    public CollegeRequest<T> withSchoolTypeIsUnknown(){
       return (CollegeRequest<T>)super.withSchoolTypeIsUnknown();
    }

    public CollegeRequest<T> withSchoolTypeIsKnown(){
       return (CollegeRequest<T>)super.withSchoolTypeIsKnown();
    }

    public CollegeRequest<T> filterBySchoolType(Long schoolType){
      return (CollegeRequest<T>)super.filterBySchoolType(schoolType);
    }

    public CollegeRequest<T> withSchoolTypeMatching(SchoolTypeRequest schoolType){
       return (CollegeRequest<T>)super.withSchoolTypeMatching(schoolType);
    }

    public CollegeRequest<T> filterByName(String name){
      return (CollegeRequest<T>)super.filterByName(name);
    }

    public CollegeRequest<T> withName(Operator operator, Object... values){
       return (CollegeRequest<T>)super.withName(operator, values);
    }

    public CollegeRequest<T> withNameIsUnknown(){
       return (CollegeRequest<T>)super.withNameIsUnknown();
    }

    public CollegeRequest<T> withNameIsKnown(){
       return (CollegeRequest<T>)super.withNameIsKnown();
    }

    public CollegeRequest<T> withNameStartingWith(String Name){
       return (CollegeRequest<T>)super.withNameStartingWith(Name);
    }
    public CollegeRequest<T> withNameContaining(String Name){
       return (CollegeRequest<T>)super.withNameContaining(Name);
    }

    public CollegeRequest<T> withNameEndingWith(String Name){
       return (CollegeRequest<T>)super.withNameEndingWith(Name);
    }


    public CollegeRequest<T> filterByStudentCapacity(Integer studentCapacity){
      return (CollegeRequest<T>)super.filterByStudentCapacity(studentCapacity);
    }

    public CollegeRequest<T> withStudentCapacity(Operator operator, Object... values){
       return (CollegeRequest<T>)super.withStudentCapacity(operator, values);
    }

    public CollegeRequest<T> withStudentCapacityIsUnknown(){
       return (CollegeRequest<T>)super.withStudentCapacityIsUnknown();
    }

    public CollegeRequest<T> withStudentCapacityIsKnown(){
       return (CollegeRequest<T>)super.withStudentCapacityIsKnown();
    }

    public CollegeRequest<T> withStudentCapacityGreaterThan(Integer studentCapacity){
       return (CollegeRequest<T>)super.withStudentCapacityGreaterThan(studentCapacity);
    }

    public CollegeRequest<T> withStudentCapacityGreaterThanOrEqualTo(Integer studentCapacity){
       return (CollegeRequest<T>)super.withStudentCapacityGreaterThanOrEqualTo(studentCapacity);
    }

    public CollegeRequest<T> withStudentCapacityLessThan(Integer studentCapacity){
       return (CollegeRequest<T>)super.withStudentCapacityLessThan(studentCapacity);
    }

    public CollegeRequest<T> withStudentCapacityLessThanOrEqualTo(Integer studentCapacity){
       return (CollegeRequest<T>)super.withStudentCapacityLessThanOrEqualTo(studentCapacity);
    }

    public CollegeRequest<T> withStudentCapacityBetween(Integer startOfStudentCapacity, Integer endOfStudentCapacity){
       return (CollegeRequest<T>)super.withStudentCapacityBetween(startOfStudentCapacity, endOfStudentCapacity);
    }


    public CollegeRequest<T> filterByVersion(Long version){
      return (CollegeRequest<T>)super.filterByVersion(version);
    }

    public CollegeRequest<T> withVersion(Operator operator, Object... values){
       return (CollegeRequest<T>)super.withVersion(operator, values);
    }

    public CollegeRequest<T> withVersionIsUnknown(){
       return (CollegeRequest<T>)super.withVersionIsUnknown();
    }

    public CollegeRequest<T> withVersionIsKnown(){
       return (CollegeRequest<T>)super.withVersionIsKnown();
    }


    public CollegeRequest<T> withIdIs(Long value){
       return (CollegeRequest<T>)super.withIdIs(value);
    }

    public CollegeRequest<T> withId(Operator operator, Object... values){
       return (CollegeRequest<T>)super.withId(operator, values);
    }


    public CollegeRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public CollegeRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public CollegeRequest minStudentCapacity(){
        super.minStudentCapacity();
        return this;
    }

    public CollegeRequest minStudentCapacityAs(String retName){
        super.minStudentCapacityAs(retName);
        return this;
    }
    public CollegeRequest maxStudentCapacity(){
        super.maxStudentCapacity();
        return this;
    }

    public CollegeRequest maxStudentCapacityAs(String retName){
        super.maxStudentCapacityAs(retName);
        return this;
    }
    public CollegeRequest sumStudentCapacity(){
        super.sumStudentCapacity();
        return this;
    }

    public CollegeRequest sumStudentCapacityAs(String retName){
        super.sumStudentCapacityAs(retName);
        return this;
    }
    public CollegeRequest avgStudentCapacity(){
        super.avgStudentCapacity();
        return this;
    }

    public CollegeRequest avgStudentCapacityAs(String retName){
        super.avgStudentCapacityAs(retName);
        return this;
    }
    public CollegeRequest stddevStudentCapacity(){
        super.standardDeviationStudentCapacity();
        return this;
    }

    public CollegeRequest stddevStudentCapacityAs(String retName){
        super.standardDeviationStudentCapacityAs(retName);
        return this;
    }
    public CollegeRequest stddev_popStudentCapacity(){
        super.squareRootOfPopulationStandardDeviationStudentCapacity();
        return this;
    }

    public CollegeRequest stddev_popStudentCapacityAs(String retName){
        super.squareRootOfPopulationStandardDeviationStudentCapacityAs(retName);
        return this;
    }
    public CollegeRequest var_sampStudentCapacity(){
        super.sampleVarianceStudentCapacity();
        return this;
    }

    public CollegeRequest var_sampStudentCapacityAs(String retName){
        super.sampleVarianceStudentCapacityAs(retName);
        return this;
    }
    public CollegeRequest var_popStudentCapacity(){
        super.samplePopulationVarianceStudentCapacity();
        return this;
    }

    public CollegeRequest var_popStudentCapacityAs(String retName){
        super.samplePopulationVarianceStudentCapacityAs(retName);
        return this;
    }
    public CollegeRequest<T> groupByPlatformWithDetails(){
       super.groupByPlatformWithDetails();
       return this;
    }

    public CollegeRequest<T> groupByPlatformWithDetails(PlatformRequest subRequest){
       super.groupByPlatformWithDetails(subRequest);
       return this;
    }

    public CollegeRequest<T> groupBySchoolTypeWithDetails(){
       super.groupBySchoolTypeWithDetails();
       return this;
    }

    public CollegeRequest<T> groupBySchoolTypeWithDetails(SchoolTypeRequest subRequest){
       super.groupBySchoolTypeWithDetails(subRequest);
       return this;
    }





    public CollegeRequest<T> countDistinctDegreeLevel(){
        return countDistinctDegreeLevelAs("degree_level_distinct_count");
    }

    public CollegeRequest<T> countDistinctDegreeLevelAs(String alias){
        super.countDistinctProperty(alias, College.DEGREE_LEVEL_PROPERTY);
        return this;
    }
    public CollegeRequest<T> countDistinctPlatform(){
        return countDistinctPlatformAs("platform_distinct_count");
    }

    public CollegeRequest<T> countDistinctPlatformAs(String alias){
        super.countDistinctProperty(alias, College.PLATFORM_PROPERTY);
        return this;
    }
    public CollegeRequest<T> countDistinctSchoolType(){
        return countDistinctSchoolTypeAs("school_type_distinct_count");
    }

    public CollegeRequest<T> countDistinctSchoolTypeAs(String alias){
        super.countDistinctProperty(alias, College.SCHOOL_TYPE_PROPERTY);
        return this;
    }
    public CollegeRequest<T> countDistinctName(){
        return countDistinctNameAs("name_distinct_count");
    }

    public CollegeRequest<T> countDistinctNameAs(String alias){
        super.countDistinctProperty(alias, College.NAME_PROPERTY);
        return this;
    }
    public CollegeRequest<T> countDistinctStudentCapacity(){
        return countDistinctStudentCapacityAs("student_capacity_distinct_count");
    }

    public CollegeRequest<T> countDistinctStudentCapacityAs(String alias){
        super.countDistinctProperty(alias, College.STUDENT_CAPACITY_PROPERTY);
        return this;
    }
    public CollegeRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public CollegeRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, College.VERSION_PROPERTY);
        return this;
    }
    public CollegeRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public CollegeRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, College.ID_PROPERTY);
        return this;
    }

    public CollegeRequest<T> groupByDegreeLevel(){
       groupBy(College.DEGREE_LEVEL_PROPERTY);
       return this;
    }

    public CollegeRequest<T> groupByDegreeLevelAs(String retName){
       groupBy(retName, College.DEGREE_LEVEL_PROPERTY);
       return this;
    }

    public CollegeRequest<T> groupByDegreeLevelWithFunction(String retName, AggrFunction function){
       groupBy(retName, College.DEGREE_LEVEL_PROPERTY, function);
       return this;
    }


    public CollegeRequest<T> groupByPlatformWith(PlatformRequest subRequest){
       super.groupByPlatformWith(subRequest);
       return this;
    }
    public CollegeRequest<T> groupByPlatform(){
       super.groupByPlatform();
       return this;
    }

    public CollegeRequest<T> groupByPlatformAs(String retName){
       super.groupByPlatformAs(retName);
       return this;
    }

    public CollegeRequest<T> groupByPlatformWithFunction(String retName, AggrFunction function){
       super.groupByPlatformWithFunction(retName, function);
       return this;
    }
    public CollegeRequest<T> groupBySchoolTypeWith(SchoolTypeRequest subRequest){
       super.groupBySchoolTypeWith(subRequest);
       return this;
    }
    public CollegeRequest<T> groupBySchoolType(){
       super.groupBySchoolType();
       return this;
    }

    public CollegeRequest<T> groupBySchoolTypeAs(String retName){
       super.groupBySchoolTypeAs(retName);
       return this;
    }

    public CollegeRequest<T> groupBySchoolTypeWithFunction(String retName, AggrFunction function){
       super.groupBySchoolTypeWithFunction(retName, function);
       return this;
    }

    public CollegeRequest<T> groupByName(){
       super.groupByName();
       return this;
    }

    public CollegeRequest<T> groupByNameAs(String retName){
       super.groupByNameAs(retName);
       return this;
    }

    public CollegeRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       super.groupByNameWithFunction(retName, function);
       return this;
    }

    public CollegeRequest<T> groupByStudentCapacity(){
       super.groupByStudentCapacity();
       return this;
    }

    public CollegeRequest<T> groupByStudentCapacityAs(String retName){
       super.groupByStudentCapacityAs(retName);
       return this;
    }

    public CollegeRequest<T> groupByStudentCapacityWithFunction(String retName, AggrFunction function){
       super.groupByStudentCapacityWithFunction(retName, function);
       return this;
    }

    public CollegeRequest<T> groupByVersion(){
       super.groupByVersion();
       return this;
    }

    public CollegeRequest<T> groupByVersionAs(String retName){
       super.groupByVersionAs(retName);
       return this;
    }

    public CollegeRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       super.groupByVersionWithFunction(retName, function);
       return this;
    }

    public CollegeRequest<T> groupById(){
       super.groupById();
       return this;
    }

    public CollegeRequest<T> groupByIdAs(String retName){
       super.groupByIdAs(retName);
       return this;
    }

    public CollegeRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       super.groupByIdWithFunction(retName, function);
       return this;
    }

    public CollegeRequest<T> orderByDegreeLevelAscending(){
       addOrderByAscending(College.DEGREE_LEVEL_PROPERTY);
       return this;
    }

    public CollegeRequest<T> orderByDegreeLevelDescending(){
       addOrderByDescending(College.DEGREE_LEVEL_PROPERTY);
       return this;
    }
    public CollegeRequest<T> orderByDegreeLevelAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(College.DEGREE_LEVEL_PROPERTY);
       return this;
    }

    public CollegeRequest<T> orderByDegreeLevelDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(College.DEGREE_LEVEL_PROPERTY);
       return this;
    }
    public CollegeRequest<T> orderByPlatformAscending(){
       super.orderByPlatformAscending();
       return this;
    }

    public CollegeRequest<T> orderByPlatformDescending(){
       super.orderByPlatformDescending();
       return this;
    }

    public CollegeRequest<T> orderBySchoolTypeAscending(){
       super.orderBySchoolTypeAscending();
       return this;
    }

    public CollegeRequest<T> orderBySchoolTypeDescending(){
       super.orderBySchoolTypeDescending();
       return this;
    }

    public CollegeRequest<T> orderByNameAscending(){
       super.orderByNameAscending();
       return this;
    }

    public CollegeRequest<T> orderByNameDescending(){
       super.orderByNameDescending();
       return this;
    }
    public CollegeRequest<T> orderByNameAscendingUsingGBK(){
       super.orderByNameAscendingUsingGBK();
       return this;
    }

    public CollegeRequest<T> orderByNameDescendingUsingGBK(){
       super.orderByNameDescendingUsingGBK();
       return this;
    }
    public CollegeRequest<T> orderByStudentCapacityAscending(){
       super.orderByStudentCapacityAscending();
       return this;
    }

    public CollegeRequest<T> orderByStudentCapacityDescending(){
       super.orderByStudentCapacityDescending();
       return this;
    }

    public CollegeRequest<T> orderByVersionAscending(){
       super.orderByVersionAscending();
       return this;
    }

    public CollegeRequest<T> orderByVersionDescending(){
       super.orderByVersionDescending();
       return this;
    }

    public CollegeRequest<T> orderByIdAscending(){
       super.orderByIdAscending();
       return this;
    }

    public CollegeRequest<T> orderByIdDescending(){
       super.orderByIdDescending();
       return this;
    }





    /**
     * get topN records
     * @param topN  records number
     */
    public CollegeRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public CollegeRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public CollegeRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public CollegeRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public CollegeRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public CollegeRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}