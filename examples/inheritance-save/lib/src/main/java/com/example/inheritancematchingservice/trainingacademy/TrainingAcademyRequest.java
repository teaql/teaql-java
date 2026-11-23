
package com.example.inheritancematchingservice.trainingacademy;

import com.example.inheritancematchingservice.academy.AcademyRequest;
import com.example.inheritancematchingservice.platform.Platform;
import com.example.inheritancematchingservice.platform.PlatformRequest;
import com.example.inheritancematchingservice.schooltype.SchoolType;
import com.example.inheritancematchingservice.schooltype.SchoolTypeRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.util.Objects;

public class TrainingAcademyRequest<T extends TrainingAcademy> extends AcademyRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public TrainingAcademyRequest(Class<T> returnType){
        this(returnType, () -> (T) new TrainingAcademy());
    }

    protected TrainingAcademyRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public TrainingAcademyRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public TrainingAcademyRequest<T> copy(){
        return (TrainingAcademyRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new TrainingAcademyRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public TrainingAcademyRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public TrainingAcademyRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public TrainingAcademyRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public TrainingAcademyRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public TrainingAcademyRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public TrainingAcademyRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public TrainingAcademyRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public TrainingAcademyRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public TrainingAcademyRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public TrainingAcademyRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (TrainingAcademyRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public TrainingAcademyRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public TrainingAcademyRequest<T> matchingAnyOf(TrainingAcademyRequest trainingAcademy){
        super.internalMatchAny(trainingAcademy);
        return this;
    }

    public TrainingAcademyRequest<T> matchingIdsOf(TrainingAcademyRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                TrainingAcademy.ID_PROPERTY, request, TrainingAcademy.ID_PROPERTY));
    }

    public TrainingAcademyRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public TrainingAcademyRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public TrainingAcademyRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public TrainingAcademyRequest<T> selectSelf(){
        super.selectSelf();
        return selectSpecialty();
    }

    public TrainingAcademyRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public TrainingAcademyRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(TrainingAcademy.SPECIALTY_PROPERTY);
        return this;
    }

    public TrainingAcademyRequest<T> selectAll(){
        super.selectAll();
        return selectSpecialty();
    }

    public TrainingAcademyRequest<T> selectChildren(){
        super.selectAny();
        return selectSpecialty();
    }


    public TrainingAcademyRequest<T> selectSpecialty(){
       selectProperty(TrainingAcademy.SPECIALTY_PROPERTY);
       return this;
    }

    /**
     * fill the specialty with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  specialty) to fetch specialty property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public TrainingAcademyRequest<T> unselectSpecialty(){
       unselectProperty(TrainingAcademy.SPECIALTY_PROPERTY);
       return this;
    }
    public TrainingAcademyRequest<T> selectCampusCode(){
       super.selectCampusCode();
       return this;
    }

    /**
     * fill the campusCode with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  campusCode) to fetch campusCode property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public TrainingAcademyRequest<T> unselectCampusCode(){
       super.unselectCampusCode();
       return this;
    }
    public TrainingAcademyRequest<T> selectPlatformIdOnly(){
       super.selectPlatformIdOnly();
       return this;
    }

    public TrainingAcademyRequest<T> selectPlatform(){
        super.selectPlatform();
        return this;
    }

    public TrainingAcademyRequest<T> selectPlatformWith(PlatformRequest platform){
       super.selectPlatformWith(platform);
       return this;
    }

    public TrainingAcademyRequest<T> unselectPlatform(){
       super.unselectPlatform();
       return this;
    }
    public TrainingAcademyRequest<T> selectSchoolTypeIdOnly(){
       super.selectSchoolTypeIdOnly();
       return this;
    }

    public TrainingAcademyRequest<T> selectSchoolType(){
        super.selectSchoolType();
        return this;
    }

    public TrainingAcademyRequest<T> selectSchoolTypeWith(SchoolTypeRequest schoolType){
       super.selectSchoolTypeWith(schoolType);
       return this;
    }

    public TrainingAcademyRequest<T> unselectSchoolType(){
       super.unselectSchoolType();
       return this;
    }
    public TrainingAcademyRequest<T> selectName(){
       super.selectName();
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public TrainingAcademyRequest<T> unselectName(){
       super.unselectName();
       return this;
    }
    public TrainingAcademyRequest<T> selectStudentCapacity(){
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
    public TrainingAcademyRequest<T> selectStudentCapacity(AggrFunction aggrFunction){
       super.selectStudentCapacity(aggrFunction);
       return this;
    }


    public TrainingAcademyRequest<T> unselectStudentCapacity(){
       super.unselectStudentCapacity();
       return this;
    }
    public TrainingAcademyRequest<T> selectVersion(){
       super.selectVersion();
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public TrainingAcademyRequest<T> unselectVersion(){
       super.unselectVersion();
       return this;
    }
    public TrainingAcademyRequest<T> selectId(){
       super.selectId();
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public TrainingAcademyRequest<T> unselectId(){
       super.unselectId();
       return this;
    }

    public TrainingAcademyRequest<T> filterBySpecialty(String... specialty){
      if (specialty == null || specialty.length == 0) {
        throw new IllegalArgumentException("filterBySpecialty parameter specialty cannot be empty");
      }
      return appendSearchCriteria(createSpecialtyCriteria(Operator.EQUAL, (Object[])specialty));
    }

    public TrainingAcademyRequest<T> withSpecialty(Operator operator, Object... values){
       return appendSearchCriteria(createSpecialtyCriteria(operator, values));
    }

    public TrainingAcademyRequest<T> withSpecialtyIsUnknown(){
       return withSpecialty(Operator.IS_NULL);
    }

    public TrainingAcademyRequest<T> withSpecialtyIsKnown(){
       return withSpecialty(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createSpecialtyCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(TrainingAcademy.SPECIALTY_PROPERTY, operator, values);
    }

    public TrainingAcademyRequest<T> withSpecialtyIsNot(String specialty){
       return withSpecialty(Operator.NOT_EQUAL, specialty);
    }

    public TrainingAcademyRequest<T> withSpecialtyIn(String... specialty){
       return withSpecialty(Operator.IN, (Object[])specialty);
    }

    public TrainingAcademyRequest<T> withSpecialtyNotIn(String... specialty){
       return withSpecialty(Operator.NOT_IN, (Object[])specialty);
    }
    public TrainingAcademyRequest<T> withSpecialtyGreaterThan(String specialty){
       return withSpecialty(Operator.GREATER_THAN, specialty);
    }

    public TrainingAcademyRequest<T> withSpecialtyGreaterThanOrEqualTo(String specialty){
       return withSpecialty(Operator.GREATER_THAN_OR_EQUAL, specialty);
    }

    public TrainingAcademyRequest<T> withSpecialtyLessThan(String specialty){
       return withSpecialty(Operator.LESS_THAN, specialty);
    }

    public TrainingAcademyRequest<T> withSpecialtyLessThanOrEqualTo(String specialty){
       return withSpecialty(Operator.LESS_THAN_OR_EQUAL, specialty);
    }

    public TrainingAcademyRequest<T> withSpecialtyBetween(String startOfSpecialty, String endOfSpecialty){
       return withSpecialty(Operator.BETWEEN, startOfSpecialty, endOfSpecialty);
    }
    public TrainingAcademyRequest<T> withSpecialtyStartingWith(String specialty){
       return withSpecialty(Operator.BEGIN_WITH, specialty);
    }
    public TrainingAcademyRequest<T> withSpecialtyContaining(String specialty){
       return withSpecialty(Operator.CONTAIN, specialty);
    }

    public TrainingAcademyRequest<T> withSpecialtyNotContaining(String specialty){
       return withSpecialty(Operator.NOT_CONTAIN, specialty);
    }

    public TrainingAcademyRequest<T> withSpecialtyNotStartingWith(String specialty){
       return withSpecialty(Operator.NOT_BEGIN_WITH, specialty);
    }

    public TrainingAcademyRequest<T> withSpecialtyEndingWith(String specialty){
       return withSpecialty(Operator.END_WITH, specialty);
    }

    public TrainingAcademyRequest<T> withSpecialtyNotEndingWith(String specialty){
       return withSpecialty(Operator.NOT_END_WITH, specialty);
    }

    public TrainingAcademyRequest<T> withSpecialtyIs(String specialty){
       return withSpecialty(Operator.EQUAL, specialty);
    }

    public TrainingAcademyRequest<T> withSpecialtySoundingLike(String specialty){
       return withSpecialty(Operator.SOUNDS_LIKE, specialty);
    }



    public TrainingAcademyRequest<T> filterByCampusCode(String campusCode){
      return (TrainingAcademyRequest<T>)super.filterByCampusCode(campusCode);
    }

    public TrainingAcademyRequest<T> withCampusCode(Operator operator, Object... values){
       return (TrainingAcademyRequest<T>)super.withCampusCode(operator, values);
    }

    public TrainingAcademyRequest<T> withCampusCodeIsUnknown(){
       return (TrainingAcademyRequest<T>)super.withCampusCodeIsUnknown();
    }

    public TrainingAcademyRequest<T> withCampusCodeIsKnown(){
       return (TrainingAcademyRequest<T>)super.withCampusCodeIsKnown();
    }

    public TrainingAcademyRequest<T> withCampusCodeStartingWith(String CampusCode){
       return (TrainingAcademyRequest<T>)super.withCampusCodeStartingWith(CampusCode);
    }
    public TrainingAcademyRequest<T> withCampusCodeContaining(String CampusCode){
       return (TrainingAcademyRequest<T>)super.withCampusCodeContaining(CampusCode);
    }

    public TrainingAcademyRequest<T> withCampusCodeEndingWith(String CampusCode){
       return (TrainingAcademyRequest<T>)super.withCampusCodeEndingWith(CampusCode);
    }


    public TrainingAcademyRequest<T> filterByPlatform(Platform platform){
      return (TrainingAcademyRequest<T>)super.filterByPlatform(platform);
    }

    public TrainingAcademyRequest<T> withPlatform(Operator operator, Object... values){
       return (TrainingAcademyRequest<T>)super.withPlatform(operator, values);
    }

    public TrainingAcademyRequest<T> withPlatformIsUnknown(){
       return (TrainingAcademyRequest<T>)super.withPlatformIsUnknown();
    }

    public TrainingAcademyRequest<T> withPlatformIsKnown(){
       return (TrainingAcademyRequest<T>)super.withPlatformIsKnown();
    }

    public TrainingAcademyRequest<T> filterByPlatform(Long platform){
      return (TrainingAcademyRequest<T>)super.filterByPlatform(platform);
    }

    public TrainingAcademyRequest<T> withPlatformMatching(PlatformRequest platform){
       return (TrainingAcademyRequest<T>)super.withPlatformMatching(platform);
    }

    public TrainingAcademyRequest<T> filterBySchoolType(SchoolType schoolType){
      return (TrainingAcademyRequest<T>)super.filterBySchoolType(schoolType);
    }

    public TrainingAcademyRequest<T> withSchoolType(Operator operator, Object... values){
       return (TrainingAcademyRequest<T>)super.withSchoolType(operator, values);
    }

    public TrainingAcademyRequest<T> withSchoolTypeIsUnknown(){
       return (TrainingAcademyRequest<T>)super.withSchoolTypeIsUnknown();
    }

    public TrainingAcademyRequest<T> withSchoolTypeIsKnown(){
       return (TrainingAcademyRequest<T>)super.withSchoolTypeIsKnown();
    }

    public TrainingAcademyRequest<T> filterBySchoolType(Long schoolType){
      return (TrainingAcademyRequest<T>)super.filterBySchoolType(schoolType);
    }

    public TrainingAcademyRequest<T> withSchoolTypeMatching(SchoolTypeRequest schoolType){
       return (TrainingAcademyRequest<T>)super.withSchoolTypeMatching(schoolType);
    }

    public TrainingAcademyRequest<T> filterByName(String name){
      return (TrainingAcademyRequest<T>)super.filterByName(name);
    }

    public TrainingAcademyRequest<T> withName(Operator operator, Object... values){
       return (TrainingAcademyRequest<T>)super.withName(operator, values);
    }

    public TrainingAcademyRequest<T> withNameIsUnknown(){
       return (TrainingAcademyRequest<T>)super.withNameIsUnknown();
    }

    public TrainingAcademyRequest<T> withNameIsKnown(){
       return (TrainingAcademyRequest<T>)super.withNameIsKnown();
    }

    public TrainingAcademyRequest<T> withNameStartingWith(String Name){
       return (TrainingAcademyRequest<T>)super.withNameStartingWith(Name);
    }
    public TrainingAcademyRequest<T> withNameContaining(String Name){
       return (TrainingAcademyRequest<T>)super.withNameContaining(Name);
    }

    public TrainingAcademyRequest<T> withNameEndingWith(String Name){
       return (TrainingAcademyRequest<T>)super.withNameEndingWith(Name);
    }


    public TrainingAcademyRequest<T> filterByStudentCapacity(Integer studentCapacity){
      return (TrainingAcademyRequest<T>)super.filterByStudentCapacity(studentCapacity);
    }

    public TrainingAcademyRequest<T> withStudentCapacity(Operator operator, Object... values){
       return (TrainingAcademyRequest<T>)super.withStudentCapacity(operator, values);
    }

    public TrainingAcademyRequest<T> withStudentCapacityIsUnknown(){
       return (TrainingAcademyRequest<T>)super.withStudentCapacityIsUnknown();
    }

    public TrainingAcademyRequest<T> withStudentCapacityIsKnown(){
       return (TrainingAcademyRequest<T>)super.withStudentCapacityIsKnown();
    }

    public TrainingAcademyRequest<T> withStudentCapacityGreaterThan(Integer studentCapacity){
       return (TrainingAcademyRequest<T>)super.withStudentCapacityGreaterThan(studentCapacity);
    }

    public TrainingAcademyRequest<T> withStudentCapacityGreaterThanOrEqualTo(Integer studentCapacity){
       return (TrainingAcademyRequest<T>)super.withStudentCapacityGreaterThanOrEqualTo(studentCapacity);
    }

    public TrainingAcademyRequest<T> withStudentCapacityLessThan(Integer studentCapacity){
       return (TrainingAcademyRequest<T>)super.withStudentCapacityLessThan(studentCapacity);
    }

    public TrainingAcademyRequest<T> withStudentCapacityLessThanOrEqualTo(Integer studentCapacity){
       return (TrainingAcademyRequest<T>)super.withStudentCapacityLessThanOrEqualTo(studentCapacity);
    }

    public TrainingAcademyRequest<T> withStudentCapacityBetween(Integer startOfStudentCapacity, Integer endOfStudentCapacity){
       return (TrainingAcademyRequest<T>)super.withStudentCapacityBetween(startOfStudentCapacity, endOfStudentCapacity);
    }


    public TrainingAcademyRequest<T> filterByVersion(Long version){
      return (TrainingAcademyRequest<T>)super.filterByVersion(version);
    }

    public TrainingAcademyRequest<T> withVersion(Operator operator, Object... values){
       return (TrainingAcademyRequest<T>)super.withVersion(operator, values);
    }

    public TrainingAcademyRequest<T> withVersionIsUnknown(){
       return (TrainingAcademyRequest<T>)super.withVersionIsUnknown();
    }

    public TrainingAcademyRequest<T> withVersionIsKnown(){
       return (TrainingAcademyRequest<T>)super.withVersionIsKnown();
    }


    public TrainingAcademyRequest<T> withIdIs(Long value){
       return (TrainingAcademyRequest<T>)super.withIdIs(value);
    }

    public TrainingAcademyRequest<T> withId(Operator operator, Object... values){
       return (TrainingAcademyRequest<T>)super.withId(operator, values);
    }


    public TrainingAcademyRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public TrainingAcademyRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public TrainingAcademyRequest minStudentCapacity(){
        super.minStudentCapacity();
        return this;
    }

    public TrainingAcademyRequest minStudentCapacityAs(String retName){
        super.minStudentCapacityAs(retName);
        return this;
    }
    public TrainingAcademyRequest maxStudentCapacity(){
        super.maxStudentCapacity();
        return this;
    }

    public TrainingAcademyRequest maxStudentCapacityAs(String retName){
        super.maxStudentCapacityAs(retName);
        return this;
    }
    public TrainingAcademyRequest sumStudentCapacity(){
        super.sumStudentCapacity();
        return this;
    }

    public TrainingAcademyRequest sumStudentCapacityAs(String retName){
        super.sumStudentCapacityAs(retName);
        return this;
    }
    public TrainingAcademyRequest avgStudentCapacity(){
        super.avgStudentCapacity();
        return this;
    }

    public TrainingAcademyRequest avgStudentCapacityAs(String retName){
        super.avgStudentCapacityAs(retName);
        return this;
    }
    public TrainingAcademyRequest stddevStudentCapacity(){
        super.standardDeviationStudentCapacity();
        return this;
    }

    public TrainingAcademyRequest stddevStudentCapacityAs(String retName){
        super.standardDeviationStudentCapacityAs(retName);
        return this;
    }
    public TrainingAcademyRequest stddev_popStudentCapacity(){
        super.squareRootOfPopulationStandardDeviationStudentCapacity();
        return this;
    }

    public TrainingAcademyRequest stddev_popStudentCapacityAs(String retName){
        super.squareRootOfPopulationStandardDeviationStudentCapacityAs(retName);
        return this;
    }
    public TrainingAcademyRequest var_sampStudentCapacity(){
        super.sampleVarianceStudentCapacity();
        return this;
    }

    public TrainingAcademyRequest var_sampStudentCapacityAs(String retName){
        super.sampleVarianceStudentCapacityAs(retName);
        return this;
    }
    public TrainingAcademyRequest var_popStudentCapacity(){
        super.samplePopulationVarianceStudentCapacity();
        return this;
    }

    public TrainingAcademyRequest var_popStudentCapacityAs(String retName){
        super.samplePopulationVarianceStudentCapacityAs(retName);
        return this;
    }
    public TrainingAcademyRequest<T> groupByPlatformWithDetails(){
       super.groupByPlatformWithDetails();
       return this;
    }

    public TrainingAcademyRequest<T> groupByPlatformWithDetails(PlatformRequest subRequest){
       super.groupByPlatformWithDetails(subRequest);
       return this;
    }

    public TrainingAcademyRequest<T> groupBySchoolTypeWithDetails(){
       super.groupBySchoolTypeWithDetails();
       return this;
    }

    public TrainingAcademyRequest<T> groupBySchoolTypeWithDetails(SchoolTypeRequest subRequest){
       super.groupBySchoolTypeWithDetails(subRequest);
       return this;
    }





    public TrainingAcademyRequest<T> countDistinctSpecialty(){
        return countDistinctSpecialtyAs("specialty_distinct_count");
    }

    public TrainingAcademyRequest<T> countDistinctSpecialtyAs(String alias){
        super.countDistinctProperty(alias, TrainingAcademy.SPECIALTY_PROPERTY);
        return this;
    }
    public TrainingAcademyRequest<T> countDistinctCampusCode(){
        return countDistinctCampusCodeAs("campus_code_distinct_count");
    }

    public TrainingAcademyRequest<T> countDistinctCampusCodeAs(String alias){
        super.countDistinctProperty(alias, TrainingAcademy.CAMPUS_CODE_PROPERTY);
        return this;
    }
    public TrainingAcademyRequest<T> countDistinctPlatform(){
        return countDistinctPlatformAs("platform_distinct_count");
    }

    public TrainingAcademyRequest<T> countDistinctPlatformAs(String alias){
        super.countDistinctProperty(alias, TrainingAcademy.PLATFORM_PROPERTY);
        return this;
    }
    public TrainingAcademyRequest<T> countDistinctSchoolType(){
        return countDistinctSchoolTypeAs("school_type_distinct_count");
    }

    public TrainingAcademyRequest<T> countDistinctSchoolTypeAs(String alias){
        super.countDistinctProperty(alias, TrainingAcademy.SCHOOL_TYPE_PROPERTY);
        return this;
    }
    public TrainingAcademyRequest<T> countDistinctName(){
        return countDistinctNameAs("name_distinct_count");
    }

    public TrainingAcademyRequest<T> countDistinctNameAs(String alias){
        super.countDistinctProperty(alias, TrainingAcademy.NAME_PROPERTY);
        return this;
    }
    public TrainingAcademyRequest<T> countDistinctStudentCapacity(){
        return countDistinctStudentCapacityAs("student_capacity_distinct_count");
    }

    public TrainingAcademyRequest<T> countDistinctStudentCapacityAs(String alias){
        super.countDistinctProperty(alias, TrainingAcademy.STUDENT_CAPACITY_PROPERTY);
        return this;
    }
    public TrainingAcademyRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public TrainingAcademyRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, TrainingAcademy.VERSION_PROPERTY);
        return this;
    }
    public TrainingAcademyRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public TrainingAcademyRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, TrainingAcademy.ID_PROPERTY);
        return this;
    }

    public TrainingAcademyRequest<T> groupBySpecialty(){
       groupBy(TrainingAcademy.SPECIALTY_PROPERTY);
       return this;
    }

    public TrainingAcademyRequest<T> groupBySpecialtyAs(String retName){
       groupBy(retName, TrainingAcademy.SPECIALTY_PROPERTY);
       return this;
    }

    public TrainingAcademyRequest<T> groupBySpecialtyWithFunction(String retName, AggrFunction function){
       groupBy(retName, TrainingAcademy.SPECIALTY_PROPERTY, function);
       return this;
    }


    public TrainingAcademyRequest<T> groupByCampusCode(){
       super.groupByCampusCode();
       return this;
    }

    public TrainingAcademyRequest<T> groupByCampusCodeAs(String retName){
       super.groupByCampusCodeAs(retName);
       return this;
    }

    public TrainingAcademyRequest<T> groupByCampusCodeWithFunction(String retName, AggrFunction function){
       super.groupByCampusCodeWithFunction(retName, function);
       return this;
    }
    public TrainingAcademyRequest<T> groupByPlatformWith(PlatformRequest subRequest){
       super.groupByPlatformWith(subRequest);
       return this;
    }
    public TrainingAcademyRequest<T> groupByPlatform(){
       super.groupByPlatform();
       return this;
    }

    public TrainingAcademyRequest<T> groupByPlatformAs(String retName){
       super.groupByPlatformAs(retName);
       return this;
    }

    public TrainingAcademyRequest<T> groupByPlatformWithFunction(String retName, AggrFunction function){
       super.groupByPlatformWithFunction(retName, function);
       return this;
    }
    public TrainingAcademyRequest<T> groupBySchoolTypeWith(SchoolTypeRequest subRequest){
       super.groupBySchoolTypeWith(subRequest);
       return this;
    }
    public TrainingAcademyRequest<T> groupBySchoolType(){
       super.groupBySchoolType();
       return this;
    }

    public TrainingAcademyRequest<T> groupBySchoolTypeAs(String retName){
       super.groupBySchoolTypeAs(retName);
       return this;
    }

    public TrainingAcademyRequest<T> groupBySchoolTypeWithFunction(String retName, AggrFunction function){
       super.groupBySchoolTypeWithFunction(retName, function);
       return this;
    }

    public TrainingAcademyRequest<T> groupByName(){
       super.groupByName();
       return this;
    }

    public TrainingAcademyRequest<T> groupByNameAs(String retName){
       super.groupByNameAs(retName);
       return this;
    }

    public TrainingAcademyRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       super.groupByNameWithFunction(retName, function);
       return this;
    }

    public TrainingAcademyRequest<T> groupByStudentCapacity(){
       super.groupByStudentCapacity();
       return this;
    }

    public TrainingAcademyRequest<T> groupByStudentCapacityAs(String retName){
       super.groupByStudentCapacityAs(retName);
       return this;
    }

    public TrainingAcademyRequest<T> groupByStudentCapacityWithFunction(String retName, AggrFunction function){
       super.groupByStudentCapacityWithFunction(retName, function);
       return this;
    }

    public TrainingAcademyRequest<T> groupByVersion(){
       super.groupByVersion();
       return this;
    }

    public TrainingAcademyRequest<T> groupByVersionAs(String retName){
       super.groupByVersionAs(retName);
       return this;
    }

    public TrainingAcademyRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       super.groupByVersionWithFunction(retName, function);
       return this;
    }

    public TrainingAcademyRequest<T> groupById(){
       super.groupById();
       return this;
    }

    public TrainingAcademyRequest<T> groupByIdAs(String retName){
       super.groupByIdAs(retName);
       return this;
    }

    public TrainingAcademyRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       super.groupByIdWithFunction(retName, function);
       return this;
    }

    public TrainingAcademyRequest<T> orderBySpecialtyAscending(){
       addOrderByAscending(TrainingAcademy.SPECIALTY_PROPERTY);
       return this;
    }

    public TrainingAcademyRequest<T> orderBySpecialtyDescending(){
       addOrderByDescending(TrainingAcademy.SPECIALTY_PROPERTY);
       return this;
    }
    public TrainingAcademyRequest<T> orderBySpecialtyAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(TrainingAcademy.SPECIALTY_PROPERTY);
       return this;
    }

    public TrainingAcademyRequest<T> orderBySpecialtyDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(TrainingAcademy.SPECIALTY_PROPERTY);
       return this;
    }
    public TrainingAcademyRequest<T> orderByCampusCodeAscending(){
       super.orderByCampusCodeAscending();
       return this;
    }

    public TrainingAcademyRequest<T> orderByCampusCodeDescending(){
       super.orderByCampusCodeDescending();
       return this;
    }
    public TrainingAcademyRequest<T> orderByCampusCodeAscendingUsingGBK(){
       super.orderByCampusCodeAscendingUsingGBK();
       return this;
    }

    public TrainingAcademyRequest<T> orderByCampusCodeDescendingUsingGBK(){
       super.orderByCampusCodeDescendingUsingGBK();
       return this;
    }
    public TrainingAcademyRequest<T> orderByPlatformAscending(){
       super.orderByPlatformAscending();
       return this;
    }

    public TrainingAcademyRequest<T> orderByPlatformDescending(){
       super.orderByPlatformDescending();
       return this;
    }

    public TrainingAcademyRequest<T> orderBySchoolTypeAscending(){
       super.orderBySchoolTypeAscending();
       return this;
    }

    public TrainingAcademyRequest<T> orderBySchoolTypeDescending(){
       super.orderBySchoolTypeDescending();
       return this;
    }

    public TrainingAcademyRequest<T> orderByNameAscending(){
       super.orderByNameAscending();
       return this;
    }

    public TrainingAcademyRequest<T> orderByNameDescending(){
       super.orderByNameDescending();
       return this;
    }
    public TrainingAcademyRequest<T> orderByNameAscendingUsingGBK(){
       super.orderByNameAscendingUsingGBK();
       return this;
    }

    public TrainingAcademyRequest<T> orderByNameDescendingUsingGBK(){
       super.orderByNameDescendingUsingGBK();
       return this;
    }
    public TrainingAcademyRequest<T> orderByStudentCapacityAscending(){
       super.orderByStudentCapacityAscending();
       return this;
    }

    public TrainingAcademyRequest<T> orderByStudentCapacityDescending(){
       super.orderByStudentCapacityDescending();
       return this;
    }

    public TrainingAcademyRequest<T> orderByVersionAscending(){
       super.orderByVersionAscending();
       return this;
    }

    public TrainingAcademyRequest<T> orderByVersionDescending(){
       super.orderByVersionDescending();
       return this;
    }

    public TrainingAcademyRequest<T> orderByIdAscending(){
       super.orderByIdAscending();
       return this;
    }

    public TrainingAcademyRequest<T> orderByIdDescending(){
       super.orderByIdDescending();
       return this;
    }





    /**
     * get topN records
     * @param topN  records number
     */
    public TrainingAcademyRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public TrainingAcademyRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public TrainingAcademyRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public TrainingAcademyRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public TrainingAcademyRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public TrainingAcademyRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}