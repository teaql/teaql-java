
package com.example.inheritancematchingservice.academy;

import cn.hutool.core.util.ObjectUtil;
import com.example.inheritancematchingservice.platform.Platform;
import com.example.inheritancematchingservice.platform.PlatformRequest;
import com.example.inheritancematchingservice.school.SchoolRequest;
import com.example.inheritancematchingservice.schooltype.SchoolType;
import com.example.inheritancematchingservice.schooltype.SchoolTypeRequest;
import com.example.inheritancematchingservice.trainingacademy.TrainingAcademy;
import io.teaql.core.AggrFunction;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AcademyRequest<T extends Academy> extends SchoolRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public AcademyRequest(Class<T> returnType){
        this(returnType, () -> (T) new Academy());
    }

    protected AcademyRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public AcademyRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public AcademyRequest<T> copy(){
        return (AcademyRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new AcademyRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public AcademyRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public AcademyRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public AcademyRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public AcademyRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public AcademyRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public AcademyRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public AcademyRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public AcademyRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public AcademyRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public AcademyRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (AcademyRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public AcademyRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public AcademyRequest<T> matchingAnyOf(AcademyRequest... academies){
        if(ObjectUtil.isEmpty(academies)){
            return this;
        }
        List<SearchCriteria> l = new ArrayList();
        for(AcademyRequest academy:academies){
            l.add(new SubQuerySearchCriteria(Academy.ID_PROPERTY, academy, Academy.ID_PROPERTY));
        }
        return appendSearchCriteria(SearchCriteria.or(l.toArray(new SearchCriteria[l.size()])));
    }

    public AcademyRequest<T> enhanceChild(AcademyRequest academy){
        super.enhanceSelf(academy);
        return this;
    }
    public AcademyRequest<T> matchingIdsOf(AcademyRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                Academy.ID_PROPERTY, request, Academy.ID_PROPERTY));
    }

    public AcademyRequest<T> enhanceChildrenIfNeeded(){
        this.enhanceChild(
new com.example.inheritancematchingservice.trainingacademy.TrainingAcademyRequest(com.example.inheritancematchingservice.trainingacademy.TrainingAcademy.class).unlimited().selectSelfWithoutParent()
);
        return this;
    }

    public AcademyRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public AcademyRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public AcademyRequest<T> selectSelf(){
        super.selectSelf();
        return selectCampusCode();
    }

    public AcademyRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public AcademyRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(Academy.CAMPUS_CODE_PROPERTY);
        return this;
    }

    public AcademyRequest<T> selectAll(){
        super.selectAll();
        return selectCampusCode();
    }

    public AcademyRequest<T> selectChildren(){
        super.selectAny();
        return selectCampusCode();
    }


    public AcademyRequest<T> selectCampusCode(){
       selectProperty(Academy.CAMPUS_CODE_PROPERTY);
       return this;
    }

    /**
     * fill the campusCode with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  campusCode) to fetch campusCode property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public AcademyRequest<T> unselectCampusCode(){
       unselectProperty(Academy.CAMPUS_CODE_PROPERTY);
       return this;
    }
    public AcademyRequest<T> selectPlatformIdOnly(){
       super.selectPlatformIdOnly();
       return this;
    }

    public AcademyRequest<T> selectPlatform(){
        super.selectPlatform();
        return this;
    }

    public AcademyRequest<T> selectPlatformWith(PlatformRequest platform){
       super.selectPlatformWith(platform);
       return this;
    }

    public AcademyRequest<T> unselectPlatform(){
       super.unselectPlatform();
       return this;
    }
    public AcademyRequest<T> selectSchoolTypeIdOnly(){
       super.selectSchoolTypeIdOnly();
       return this;
    }

    public AcademyRequest<T> selectSchoolType(){
        super.selectSchoolType();
        return this;
    }

    public AcademyRequest<T> selectSchoolTypeWith(SchoolTypeRequest schoolType){
       super.selectSchoolTypeWith(schoolType);
       return this;
    }

    public AcademyRequest<T> unselectSchoolType(){
       super.unselectSchoolType();
       return this;
    }
    public AcademyRequest<T> selectName(){
       super.selectName();
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public AcademyRequest<T> unselectName(){
       super.unselectName();
       return this;
    }
    public AcademyRequest<T> selectStudentCapacity(){
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
    public AcademyRequest<T> selectStudentCapacity(AggrFunction aggrFunction){
       super.selectStudentCapacity(aggrFunction);
       return this;
    }


    public AcademyRequest<T> unselectStudentCapacity(){
       super.unselectStudentCapacity();
       return this;
    }
    public AcademyRequest<T> selectVersion(){
       super.selectVersion();
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public AcademyRequest<T> unselectVersion(){
       super.unselectVersion();
       return this;
    }
    public AcademyRequest<T> selectId(){
       super.selectId();
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public AcademyRequest<T> unselectId(){
       super.unselectId();
       return this;
    }

    public AcademyRequest<T> isTrainingAcademy(){
        return matchTypes(TrainingAcademy.INTERNAL_TYPE);
    }

    public AcademyRequest<T> matchTypes(String... types){
        super.matchType(types);
        return this;
    }

    public AcademyRequest<T> withTypeGroup(){
        super.withTypeGroup();
        return this;
    }

    public AcademyRequest<T> filterByCampusCode(String... campusCode){
      if (campusCode == null || campusCode.length == 0) {
        throw new IllegalArgumentException("filterByCampusCode parameter campusCode cannot be empty");
      }
      return appendSearchCriteria(createCampusCodeCriteria(Operator.EQUAL, (Object[])campusCode));
    }

    public AcademyRequest<T> withCampusCode(Operator operator, Object... values){
       return appendSearchCriteria(createCampusCodeCriteria(operator, values));
    }

    public AcademyRequest<T> withCampusCodeIsUnknown(){
       return withCampusCode(Operator.IS_NULL);
    }

    public AcademyRequest<T> withCampusCodeIsKnown(){
       return withCampusCode(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCampusCodeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Academy.CAMPUS_CODE_PROPERTY, operator, values);
    }

    public AcademyRequest<T> withCampusCodeIsNot(String campusCode){
       return withCampusCode(Operator.NOT_EQUAL, campusCode);
    }

    public AcademyRequest<T> withCampusCodeIn(String... campusCode){
       return withCampusCode(Operator.IN, (Object[])campusCode);
    }

    public AcademyRequest<T> withCampusCodeNotIn(String... campusCode){
       return withCampusCode(Operator.NOT_IN, (Object[])campusCode);
    }
    public AcademyRequest<T> withCampusCodeGreaterThan(String campusCode){
       return withCampusCode(Operator.GREATER_THAN, campusCode);
    }

    public AcademyRequest<T> withCampusCodeGreaterThanOrEqualTo(String campusCode){
       return withCampusCode(Operator.GREATER_THAN_OR_EQUAL, campusCode);
    }

    public AcademyRequest<T> withCampusCodeLessThan(String campusCode){
       return withCampusCode(Operator.LESS_THAN, campusCode);
    }

    public AcademyRequest<T> withCampusCodeLessThanOrEqualTo(String campusCode){
       return withCampusCode(Operator.LESS_THAN_OR_EQUAL, campusCode);
    }

    public AcademyRequest<T> withCampusCodeBetween(String startOfCampusCode, String endOfCampusCode){
       return withCampusCode(Operator.BETWEEN, startOfCampusCode, endOfCampusCode);
    }
    public AcademyRequest<T> withCampusCodeStartingWith(String campusCode){
       return withCampusCode(Operator.BEGIN_WITH, campusCode);
    }
    public AcademyRequest<T> withCampusCodeContaining(String campusCode){
       return withCampusCode(Operator.CONTAIN, campusCode);
    }

    public AcademyRequest<T> withCampusCodeNotContaining(String campusCode){
       return withCampusCode(Operator.NOT_CONTAIN, campusCode);
    }

    public AcademyRequest<T> withCampusCodeNotStartingWith(String campusCode){
       return withCampusCode(Operator.NOT_BEGIN_WITH, campusCode);
    }

    public AcademyRequest<T> withCampusCodeEndingWith(String campusCode){
       return withCampusCode(Operator.END_WITH, campusCode);
    }

    public AcademyRequest<T> withCampusCodeNotEndingWith(String campusCode){
       return withCampusCode(Operator.NOT_END_WITH, campusCode);
    }

    public AcademyRequest<T> withCampusCodeIs(String campusCode){
       return withCampusCode(Operator.EQUAL, campusCode);
    }

    public AcademyRequest<T> withCampusCodeSoundingLike(String campusCode){
       return withCampusCode(Operator.SOUNDS_LIKE, campusCode);
    }



    public AcademyRequest<T> filterByPlatform(Platform platform){
      return (AcademyRequest<T>)super.filterByPlatform(platform);
    }

    public AcademyRequest<T> withPlatform(Operator operator, Object... values){
       return (AcademyRequest<T>)super.withPlatform(operator, values);
    }

    public AcademyRequest<T> withPlatformIsUnknown(){
       return (AcademyRequest<T>)super.withPlatformIsUnknown();
    }

    public AcademyRequest<T> withPlatformIsKnown(){
       return (AcademyRequest<T>)super.withPlatformIsKnown();
    }

    public AcademyRequest<T> filterByPlatform(Long platform){
      return (AcademyRequest<T>)super.filterByPlatform(platform);
    }

    public AcademyRequest<T> withPlatformMatching(PlatformRequest platform){
       return (AcademyRequest<T>)super.withPlatformMatching(platform);
    }

    public AcademyRequest<T> filterBySchoolType(SchoolType schoolType){
      return (AcademyRequest<T>)super.filterBySchoolType(schoolType);
    }

    public AcademyRequest<T> withSchoolType(Operator operator, Object... values){
       return (AcademyRequest<T>)super.withSchoolType(operator, values);
    }

    public AcademyRequest<T> withSchoolTypeIsUnknown(){
       return (AcademyRequest<T>)super.withSchoolTypeIsUnknown();
    }

    public AcademyRequest<T> withSchoolTypeIsKnown(){
       return (AcademyRequest<T>)super.withSchoolTypeIsKnown();
    }

    public AcademyRequest<T> filterBySchoolType(Long schoolType){
      return (AcademyRequest<T>)super.filterBySchoolType(schoolType);
    }

    public AcademyRequest<T> withSchoolTypeMatching(SchoolTypeRequest schoolType){
       return (AcademyRequest<T>)super.withSchoolTypeMatching(schoolType);
    }

    public AcademyRequest<T> filterByName(String name){
      return (AcademyRequest<T>)super.filterByName(name);
    }

    public AcademyRequest<T> withName(Operator operator, Object... values){
       return (AcademyRequest<T>)super.withName(operator, values);
    }

    public AcademyRequest<T> withNameIsUnknown(){
       return (AcademyRequest<T>)super.withNameIsUnknown();
    }

    public AcademyRequest<T> withNameIsKnown(){
       return (AcademyRequest<T>)super.withNameIsKnown();
    }

    public AcademyRequest<T> withNameStartingWith(String Name){
       return (AcademyRequest<T>)super.withNameStartingWith(Name);
    }
    public AcademyRequest<T> withNameContaining(String Name){
       return (AcademyRequest<T>)super.withNameContaining(Name);
    }

    public AcademyRequest<T> withNameEndingWith(String Name){
       return (AcademyRequest<T>)super.withNameEndingWith(Name);
    }


    public AcademyRequest<T> filterByStudentCapacity(Integer studentCapacity){
      return (AcademyRequest<T>)super.filterByStudentCapacity(studentCapacity);
    }

    public AcademyRequest<T> withStudentCapacity(Operator operator, Object... values){
       return (AcademyRequest<T>)super.withStudentCapacity(operator, values);
    }

    public AcademyRequest<T> withStudentCapacityIsUnknown(){
       return (AcademyRequest<T>)super.withStudentCapacityIsUnknown();
    }

    public AcademyRequest<T> withStudentCapacityIsKnown(){
       return (AcademyRequest<T>)super.withStudentCapacityIsKnown();
    }

    public AcademyRequest<T> withStudentCapacityGreaterThan(Integer studentCapacity){
       return (AcademyRequest<T>)super.withStudentCapacityGreaterThan(studentCapacity);
    }

    public AcademyRequest<T> withStudentCapacityGreaterThanOrEqualTo(Integer studentCapacity){
       return (AcademyRequest<T>)super.withStudentCapacityGreaterThanOrEqualTo(studentCapacity);
    }

    public AcademyRequest<T> withStudentCapacityLessThan(Integer studentCapacity){
       return (AcademyRequest<T>)super.withStudentCapacityLessThan(studentCapacity);
    }

    public AcademyRequest<T> withStudentCapacityLessThanOrEqualTo(Integer studentCapacity){
       return (AcademyRequest<T>)super.withStudentCapacityLessThanOrEqualTo(studentCapacity);
    }

    public AcademyRequest<T> withStudentCapacityBetween(Integer startOfStudentCapacity, Integer endOfStudentCapacity){
       return (AcademyRequest<T>)super.withStudentCapacityBetween(startOfStudentCapacity, endOfStudentCapacity);
    }


    public AcademyRequest<T> filterByVersion(Long version){
      return (AcademyRequest<T>)super.filterByVersion(version);
    }

    public AcademyRequest<T> withVersion(Operator operator, Object... values){
       return (AcademyRequest<T>)super.withVersion(operator, values);
    }

    public AcademyRequest<T> withVersionIsUnknown(){
       return (AcademyRequest<T>)super.withVersionIsUnknown();
    }

    public AcademyRequest<T> withVersionIsKnown(){
       return (AcademyRequest<T>)super.withVersionIsKnown();
    }


    public AcademyRequest<T> withIdIs(Long value){
       return (AcademyRequest<T>)super.withIdIs(value);
    }

    public AcademyRequest<T> withId(Operator operator, Object... values){
       return (AcademyRequest<T>)super.withId(operator, values);
    }


    public AcademyRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public AcademyRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public AcademyRequest minStudentCapacity(){
        super.minStudentCapacity();
        return this;
    }

    public AcademyRequest minStudentCapacityAs(String retName){
        super.minStudentCapacityAs(retName);
        return this;
    }
    public AcademyRequest maxStudentCapacity(){
        super.maxStudentCapacity();
        return this;
    }

    public AcademyRequest maxStudentCapacityAs(String retName){
        super.maxStudentCapacityAs(retName);
        return this;
    }
    public AcademyRequest sumStudentCapacity(){
        super.sumStudentCapacity();
        return this;
    }

    public AcademyRequest sumStudentCapacityAs(String retName){
        super.sumStudentCapacityAs(retName);
        return this;
    }
    public AcademyRequest avgStudentCapacity(){
        super.avgStudentCapacity();
        return this;
    }

    public AcademyRequest avgStudentCapacityAs(String retName){
        super.avgStudentCapacityAs(retName);
        return this;
    }
    public AcademyRequest stddevStudentCapacity(){
        super.standardDeviationStudentCapacity();
        return this;
    }

    public AcademyRequest stddevStudentCapacityAs(String retName){
        super.standardDeviationStudentCapacityAs(retName);
        return this;
    }
    public AcademyRequest stddev_popStudentCapacity(){
        super.squareRootOfPopulationStandardDeviationStudentCapacity();
        return this;
    }

    public AcademyRequest stddev_popStudentCapacityAs(String retName){
        super.squareRootOfPopulationStandardDeviationStudentCapacityAs(retName);
        return this;
    }
    public AcademyRequest var_sampStudentCapacity(){
        super.sampleVarianceStudentCapacity();
        return this;
    }

    public AcademyRequest var_sampStudentCapacityAs(String retName){
        super.sampleVarianceStudentCapacityAs(retName);
        return this;
    }
    public AcademyRequest var_popStudentCapacity(){
        super.samplePopulationVarianceStudentCapacity();
        return this;
    }

    public AcademyRequest var_popStudentCapacityAs(String retName){
        super.samplePopulationVarianceStudentCapacityAs(retName);
        return this;
    }
    public AcademyRequest<T> groupByPlatformWithDetails(){
       super.groupByPlatformWithDetails();
       return this;
    }

    public AcademyRequest<T> groupByPlatformWithDetails(PlatformRequest subRequest){
       super.groupByPlatformWithDetails(subRequest);
       return this;
    }

    public AcademyRequest<T> groupBySchoolTypeWithDetails(){
       super.groupBySchoolTypeWithDetails();
       return this;
    }

    public AcademyRequest<T> groupBySchoolTypeWithDetails(SchoolTypeRequest subRequest){
       super.groupBySchoolTypeWithDetails(subRequest);
       return this;
    }





    public AcademyRequest<T> countDistinctCampusCode(){
        return countDistinctCampusCodeAs("campus_code_distinct_count");
    }

    public AcademyRequest<T> countDistinctCampusCodeAs(String alias){
        super.countDistinctProperty(alias, Academy.CAMPUS_CODE_PROPERTY);
        return this;
    }
    public AcademyRequest<T> countDistinctPlatform(){
        return countDistinctPlatformAs("platform_distinct_count");
    }

    public AcademyRequest<T> countDistinctPlatformAs(String alias){
        super.countDistinctProperty(alias, Academy.PLATFORM_PROPERTY);
        return this;
    }
    public AcademyRequest<T> countDistinctSchoolType(){
        return countDistinctSchoolTypeAs("school_type_distinct_count");
    }

    public AcademyRequest<T> countDistinctSchoolTypeAs(String alias){
        super.countDistinctProperty(alias, Academy.SCHOOL_TYPE_PROPERTY);
        return this;
    }
    public AcademyRequest<T> countDistinctName(){
        return countDistinctNameAs("name_distinct_count");
    }

    public AcademyRequest<T> countDistinctNameAs(String alias){
        super.countDistinctProperty(alias, Academy.NAME_PROPERTY);
        return this;
    }
    public AcademyRequest<T> countDistinctStudentCapacity(){
        return countDistinctStudentCapacityAs("student_capacity_distinct_count");
    }

    public AcademyRequest<T> countDistinctStudentCapacityAs(String alias){
        super.countDistinctProperty(alias, Academy.STUDENT_CAPACITY_PROPERTY);
        return this;
    }
    public AcademyRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public AcademyRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, Academy.VERSION_PROPERTY);
        return this;
    }
    public AcademyRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public AcademyRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, Academy.ID_PROPERTY);
        return this;
    }

    public AcademyRequest<T> groupByCampusCode(){
       groupBy(Academy.CAMPUS_CODE_PROPERTY);
       return this;
    }

    public AcademyRequest<T> groupByCampusCodeAs(String retName){
       groupBy(retName, Academy.CAMPUS_CODE_PROPERTY);
       return this;
    }

    public AcademyRequest<T> groupByCampusCodeWithFunction(String retName, AggrFunction function){
       groupBy(retName, Academy.CAMPUS_CODE_PROPERTY, function);
       return this;
    }


    public AcademyRequest<T> groupByPlatformWith(PlatformRequest subRequest){
       super.groupByPlatformWith(subRequest);
       return this;
    }
    public AcademyRequest<T> groupByPlatform(){
       super.groupByPlatform();
       return this;
    }

    public AcademyRequest<T> groupByPlatformAs(String retName){
       super.groupByPlatformAs(retName);
       return this;
    }

    public AcademyRequest<T> groupByPlatformWithFunction(String retName, AggrFunction function){
       super.groupByPlatformWithFunction(retName, function);
       return this;
    }
    public AcademyRequest<T> groupBySchoolTypeWith(SchoolTypeRequest subRequest){
       super.groupBySchoolTypeWith(subRequest);
       return this;
    }
    public AcademyRequest<T> groupBySchoolType(){
       super.groupBySchoolType();
       return this;
    }

    public AcademyRequest<T> groupBySchoolTypeAs(String retName){
       super.groupBySchoolTypeAs(retName);
       return this;
    }

    public AcademyRequest<T> groupBySchoolTypeWithFunction(String retName, AggrFunction function){
       super.groupBySchoolTypeWithFunction(retName, function);
       return this;
    }

    public AcademyRequest<T> groupByName(){
       super.groupByName();
       return this;
    }

    public AcademyRequest<T> groupByNameAs(String retName){
       super.groupByNameAs(retName);
       return this;
    }

    public AcademyRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       super.groupByNameWithFunction(retName, function);
       return this;
    }

    public AcademyRequest<T> groupByStudentCapacity(){
       super.groupByStudentCapacity();
       return this;
    }

    public AcademyRequest<T> groupByStudentCapacityAs(String retName){
       super.groupByStudentCapacityAs(retName);
       return this;
    }

    public AcademyRequest<T> groupByStudentCapacityWithFunction(String retName, AggrFunction function){
       super.groupByStudentCapacityWithFunction(retName, function);
       return this;
    }

    public AcademyRequest<T> groupByVersion(){
       super.groupByVersion();
       return this;
    }

    public AcademyRequest<T> groupByVersionAs(String retName){
       super.groupByVersionAs(retName);
       return this;
    }

    public AcademyRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       super.groupByVersionWithFunction(retName, function);
       return this;
    }

    public AcademyRequest<T> groupById(){
       super.groupById();
       return this;
    }

    public AcademyRequest<T> groupByIdAs(String retName){
       super.groupByIdAs(retName);
       return this;
    }

    public AcademyRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       super.groupByIdWithFunction(retName, function);
       return this;
    }

    public AcademyRequest<T> orderByCampusCodeAscending(){
       addOrderByAscending(Academy.CAMPUS_CODE_PROPERTY);
       return this;
    }

    public AcademyRequest<T> orderByCampusCodeDescending(){
       addOrderByDescending(Academy.CAMPUS_CODE_PROPERTY);
       return this;
    }
    public AcademyRequest<T> orderByCampusCodeAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Academy.CAMPUS_CODE_PROPERTY);
       return this;
    }

    public AcademyRequest<T> orderByCampusCodeDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Academy.CAMPUS_CODE_PROPERTY);
       return this;
    }
    public AcademyRequest<T> orderByPlatformAscending(){
       super.orderByPlatformAscending();
       return this;
    }

    public AcademyRequest<T> orderByPlatformDescending(){
       super.orderByPlatformDescending();
       return this;
    }

    public AcademyRequest<T> orderBySchoolTypeAscending(){
       super.orderBySchoolTypeAscending();
       return this;
    }

    public AcademyRequest<T> orderBySchoolTypeDescending(){
       super.orderBySchoolTypeDescending();
       return this;
    }

    public AcademyRequest<T> orderByNameAscending(){
       super.orderByNameAscending();
       return this;
    }

    public AcademyRequest<T> orderByNameDescending(){
       super.orderByNameDescending();
       return this;
    }
    public AcademyRequest<T> orderByNameAscendingUsingGBK(){
       super.orderByNameAscendingUsingGBK();
       return this;
    }

    public AcademyRequest<T> orderByNameDescendingUsingGBK(){
       super.orderByNameDescendingUsingGBK();
       return this;
    }
    public AcademyRequest<T> orderByStudentCapacityAscending(){
       super.orderByStudentCapacityAscending();
       return this;
    }

    public AcademyRequest<T> orderByStudentCapacityDescending(){
       super.orderByStudentCapacityDescending();
       return this;
    }

    public AcademyRequest<T> orderByVersionAscending(){
       super.orderByVersionAscending();
       return this;
    }

    public AcademyRequest<T> orderByVersionDescending(){
       super.orderByVersionDescending();
       return this;
    }

    public AcademyRequest<T> orderByIdAscending(){
       super.orderByIdAscending();
       return this;
    }

    public AcademyRequest<T> orderByIdDescending(){
       super.orderByIdDescending();
       return this;
    }





    /**
     * get topN records
     * @param topN  records number
     */
    public AcademyRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public AcademyRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public AcademyRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public AcademyRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public AcademyRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public AcademyRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}