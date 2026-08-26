
package com.example.schoolmanagementservice.schooltype;

import com.example.schoolmanagementservice.Q;
import com.example.schoolmanagementservice.platform.Platform;
import com.example.schoolmanagementservice.platform.PlatformRequest;
import com.example.schoolmanagementservice.school.School;
import com.example.schoolmanagementservice.school.SchoolRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.math.BigDecimal;

public class SchoolTypeRequest<T extends SchoolType> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public SchoolTypeRequest(Class<T> returnType){
        super(returnType, () -> (T) new SchoolType());
        selectId();
        selectVersion();
    }

    public SchoolTypeRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public SchoolTypeRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public SchoolTypeRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public SchoolTypeRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public SchoolTypeRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public SchoolTypeRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public SchoolTypeRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public SchoolTypeRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (SchoolTypeRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public SchoolTypeRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public SchoolTypeRequest<T> matchingAnyOf(SchoolTypeRequest schoolType){
        super.internalMatchAny(schoolType);
        return this;
    }

    public SchoolTypeRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public SchoolTypeRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public SchoolTypeRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public SchoolTypeRequest<T> selectSelf(){
        super.selectSelf();
        return selectPlatformIdOnly().selectId().selectName().selectCode().selectDisplayOrder().selectVersion();
    }

    public SchoolTypeRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public SchoolTypeRequest<T> selectAll(){
        super.selectAll();
        return selectPlatform().selectId().selectName().selectCode().selectDisplayOrder().selectVersion();
    }

    public SchoolTypeRequest<T> selectChildren(){
        super.selectAny();
        selectSchoolList();
        return selectPlatform().selectId().selectName().selectCode().selectDisplayOrder().selectVersion();
    }


    public SchoolTypeRequest<T> selectPlatformIdOnly(){
       selectProperty(SchoolType.PLATFORM_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> selectPlatform(){
        return selectPlatformWith(Q.platforms().unlimited().selectSelf());
    }

    public SchoolTypeRequest<T> selectPlatformWith(PlatformRequest platform){
       selectProperty(SchoolType.PLATFORM_PROPERTY);
       enhanceRelation(SchoolType.PLATFORM_PROPERTY, platform);
       return this;
    }

    public SchoolTypeRequest<T> unselectPlatform(){
       unselectProperty(SchoolType.PLATFORM_PROPERTY);
       return this;
    }
    public SchoolTypeRequest<T> selectId(){
       selectProperty(SchoolType.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SchoolTypeRequest<T> unselectId(){
       unselectProperty(SchoolType.ID_PROPERTY);
       return this;
    }
    public SchoolTypeRequest<T> selectName(){
       selectProperty(SchoolType.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SchoolTypeRequest<T> unselectName(){
       unselectProperty(SchoolType.NAME_PROPERTY);
       return this;
    }
    public SchoolTypeRequest<T> selectCode(){
       selectProperty(SchoolType.CODE_PROPERTY);
       return this;
    }

    /**
     * fill the code with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  code) to fetch code property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SchoolTypeRequest<T> unselectCode(){
       unselectProperty(SchoolType.CODE_PROPERTY);
       return this;
    }
    public SchoolTypeRequest<T> selectDisplayOrder(){
       selectProperty(SchoolType.DISPLAY_ORDER_PROPERTY);
       return this;
    }

    /**
     * fill the displayOrder with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  displayOrder) to fetch displayOrder property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the displayOrder with customized aggrFunction, TEAQL uses ({aggrFunction}(displayOrder) AS displayOrder to fetch displayOrder property.
     * @param aggrFunction  aggrFunction
     */
    public SchoolTypeRequest<T> selectDisplayOrder(AggrFunction aggrFunction){
       selectProperty(SchoolType.DISPLAY_ORDER_PROPERTY, aggrFunction);
       return this;
    }


    public SchoolTypeRequest<T> unselectDisplayOrder(){
       unselectProperty(SchoolType.DISPLAY_ORDER_PROPERTY);
       return this;
    }
    public SchoolTypeRequest<T> selectVersion(){
       selectProperty(SchoolType.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SchoolTypeRequest<T> unselectVersion(){
       unselectProperty(SchoolType.VERSION_PROPERTY);
       return this;
    }
    public SchoolTypeRequest<T> selectSchoolList(){
       return selectSchoolListWith(Q.schools().selectSelf());
    }

    public SchoolTypeRequest<T> selectSchoolListWith(SchoolRequest schoolList){
       enhanceRelation(SchoolType.SCHOOL_LIST_PROPERTY, schoolList);
       return this;
    }

    public SchoolTypeRequest<T> filterByPlatform(Platform... platform){
      if (platform == null || platform.length == 0) {
        throw new IllegalArgumentException("filterByPlatform parameter platform cannot be empty");
      }
      return appendSearchCriteria(createPlatformCriteria(Operator.EQUAL, (Object[])platform));
    }

    public SchoolTypeRequest<T> withPlatform(Operator operator, Object... values){
       return appendSearchCriteria(createPlatformCriteria(operator, values));
    }

    public SchoolTypeRequest<T> withPlatformIsUnknown(){
       return withPlatform(Operator.IS_NULL);
    }

    public SchoolTypeRequest<T> withPlatformIsKnown(){
       return withPlatform(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createPlatformCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SchoolType.PLATFORM_PROPERTY, operator, values);
    }

    public SchoolTypeRequest<T> filterByPlatform(Long platform){
      if(platform == null){
         return this;
      }
      return withPlatform(Operator.EQUAL, platform);
    }
    public SchoolTypeRequest<T> withPlatformMatching(PlatformRequest platform){
       return appendSearchCriteria(new SubQuerySearchCriteria(SchoolType.PLATFORM_PROPERTY, platform, Platform.ID_PROPERTY));
    }

    public SchoolTypeRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SchoolType.ID_PROPERTY, operator, values);
    }

    public SchoolTypeRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }
    public SchoolTypeRequest<T> withIdIn(Long... id){
       return withId(Operator.EQUAL, (Object[])id);
    }



    public SchoolTypeRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public SchoolTypeRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public SchoolTypeRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public SchoolTypeRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SchoolType.NAME_PROPERTY, operator, values);
    }

    public SchoolTypeRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public SchoolTypeRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public SchoolTypeRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public SchoolTypeRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public SchoolTypeRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public SchoolTypeRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public SchoolTypeRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public SchoolTypeRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public SchoolTypeRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public SchoolTypeRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public SchoolTypeRequest<T> filterByCode(String... code){
      if (code == null || code.length == 0) {
        throw new IllegalArgumentException("filterByCode parameter code cannot be empty");
      }
      return appendSearchCriteria(createCodeCriteria(Operator.EQUAL, (Object[])code));
    }

    public SchoolTypeRequest<T> withCode(Operator operator, Object... values){
       return appendSearchCriteria(createCodeCriteria(operator, values));
    }

    public SchoolTypeRequest<T> withCodeIsUnknown(){
       return withCode(Operator.IS_NULL);
    }

    public SchoolTypeRequest<T> withCodeIsKnown(){
       return withCode(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCodeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SchoolType.CODE_PROPERTY, operator, values);
    }

    public SchoolTypeRequest<T> withCodeGreaterThan(String code){
       return withCode(Operator.GREATER_THAN, code);
    }

    public SchoolTypeRequest<T> withCodeGreaterThanOrEqualTo(String code){
       return withCode(Operator.GREATER_THAN_OR_EQUAL, code);
    }

    public SchoolTypeRequest<T> withCodeLessThan(String code){
       return withCode(Operator.LESS_THAN, code);
    }

    public SchoolTypeRequest<T> withCodeLessThanOrEqualTo(String code){
       return withCode(Operator.LESS_THAN_OR_EQUAL, code);
    }

    public SchoolTypeRequest<T> withCodeBetween(String startOfCode, String endOfCode){
       return withCode(Operator.BETWEEN, startOfCode, endOfCode);
    }
    public SchoolTypeRequest<T> withCodeStartingWith(String code){
       return withCode(Operator.BEGIN_WITH, code);
    }
    public SchoolTypeRequest<T> withCodeContaining(String code){
       return withCode(Operator.CONTAIN, code);
    }

    public SchoolTypeRequest<T> withCodeEndingWith(String code){
       return withCode(Operator.END_WITH, code);
    }

    public SchoolTypeRequest<T> withCodeIs(String code){
       return withCode(Operator.EQUAL, code);
    }

    public SchoolTypeRequest<T> withCodeSoundingLike(String code){
       return withCode(Operator.SOUNDS_LIKE, code);
    }



    public SchoolTypeRequest<T> filterByDisplayOrder(BigDecimal... displayOrder){
      if (displayOrder == null || displayOrder.length == 0) {
        throw new IllegalArgumentException("filterByDisplayOrder parameter displayOrder cannot be empty");
      }
      return appendSearchCriteria(createDisplayOrderCriteria(Operator.EQUAL, (Object[])displayOrder));
    }

    public SchoolTypeRequest<T> withDisplayOrder(Operator operator, Object... values){
       return appendSearchCriteria(createDisplayOrderCriteria(operator, values));
    }

    public SchoolTypeRequest<T> withDisplayOrderIsUnknown(){
       return withDisplayOrder(Operator.IS_NULL);
    }

    public SchoolTypeRequest<T> withDisplayOrderIsKnown(){
       return withDisplayOrder(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createDisplayOrderCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SchoolType.DISPLAY_ORDER_PROPERTY, operator, values);
    }

    public SchoolTypeRequest<T> withDisplayOrderGreaterThan(BigDecimal displayOrder){
       return withDisplayOrder(Operator.GREATER_THAN, displayOrder);
    }

    public SchoolTypeRequest<T> withDisplayOrderGreaterThanOrEqualTo(BigDecimal displayOrder){
       return withDisplayOrder(Operator.GREATER_THAN_OR_EQUAL, displayOrder);
    }

    public SchoolTypeRequest<T> withDisplayOrderLessThan(BigDecimal displayOrder){
       return withDisplayOrder(Operator.LESS_THAN, displayOrder);
    }

    public SchoolTypeRequest<T> withDisplayOrderLessThanOrEqualTo(BigDecimal displayOrder){
       return withDisplayOrder(Operator.LESS_THAN_OR_EQUAL, displayOrder);
    }

    public SchoolTypeRequest<T> withDisplayOrderBetween(BigDecimal startOfDisplayOrder, BigDecimal endOfDisplayOrder){
       return withDisplayOrder(Operator.BETWEEN, startOfDisplayOrder, endOfDisplayOrder);
    }



    public SchoolTypeRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public SchoolTypeRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public SchoolTypeRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public SchoolTypeRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SchoolType.VERSION_PROPERTY, operator, values);
    }

    public SchoolTypeRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public SchoolTypeRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public SchoolTypeRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public SchoolTypeRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public SchoolTypeRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }

    public SchoolTypeRequest<T> withSchoolListMatching(SchoolRequest schoolRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(SchoolType.ID_PROPERTY, schoolRequest, School.SCHOOL_TYPE_PROPERTY));
    }

    public SchoolTypeRequest<T> withoutSchoolListMatching(SchoolRequest schoolRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(SchoolType.ID_PROPERTY, schoolRequest, School.SCHOOL_TYPE_PROPERTY)));
    }

    public SchoolTypeRequest<T> haveSchools(){
        return withSchoolListMatching(Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> haveNoSchools(){
        return withoutSchoolListMatching(Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> count(){
        super.count();
        return this;
    }
    public SchoolTypeRequest<T> countAs(String retName){
        super.count(retName);
        return this;
    }
    public SchoolTypeRequest minDisplayOrder(){
        return minDisplayOrderAs(prefix("minOf",SchoolType.DISPLAY_ORDER_PROPERTY));
    }

    public SchoolTypeRequest minDisplayOrderAs(String retName){
        super.min(retName, SchoolType.DISPLAY_ORDER_PROPERTY);
        return this;
    }
    public SchoolTypeRequest maxDisplayOrder(){
        return maxDisplayOrderAs(prefix("maxOf",SchoolType.DISPLAY_ORDER_PROPERTY));
    }

    public SchoolTypeRequest maxDisplayOrderAs(String retName){
        super.max(retName, SchoolType.DISPLAY_ORDER_PROPERTY);
        return this;
    }
    public SchoolTypeRequest sumDisplayOrder(){
        return sumDisplayOrderAs(prefix("sumOf",SchoolType.DISPLAY_ORDER_PROPERTY));
    }

    public SchoolTypeRequest sumDisplayOrderAs(String retName){
        super.sum(retName, SchoolType.DISPLAY_ORDER_PROPERTY);
        return this;
    }
    public SchoolTypeRequest avgDisplayOrder(){
        return avgDisplayOrderAs(prefix("avgOf",SchoolType.DISPLAY_ORDER_PROPERTY));
    }

    public SchoolTypeRequest avgDisplayOrderAs(String retName){
        super.avg(retName, SchoolType.DISPLAY_ORDER_PROPERTY);
        return this;
    }
    public SchoolTypeRequest standardDeviationDisplayOrder(){
        return standardDeviationDisplayOrderAs(prefix("standardDeviationOf",SchoolType.DISPLAY_ORDER_PROPERTY));
    }

    public SchoolTypeRequest standardDeviationDisplayOrderAs(String retName){
        super.standardDeviation(retName, SchoolType.DISPLAY_ORDER_PROPERTY);
        return this;
    }
    public SchoolTypeRequest squareRootOfPopulationStandardDeviationDisplayOrder(){
        return squareRootOfPopulationStandardDeviationDisplayOrderAs(prefix("squareRootOfPopulationStandardDeviationOf",SchoolType.DISPLAY_ORDER_PROPERTY));
    }

    public SchoolTypeRequest squareRootOfPopulationStandardDeviationDisplayOrderAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, SchoolType.DISPLAY_ORDER_PROPERTY);
        return this;
    }
    public SchoolTypeRequest sampleVarianceDisplayOrder(){
        return sampleVarianceDisplayOrderAs(prefix("sampleVarianceOf",SchoolType.DISPLAY_ORDER_PROPERTY));
    }

    public SchoolTypeRequest sampleVarianceDisplayOrderAs(String retName){
        super.sampleVariance(retName, SchoolType.DISPLAY_ORDER_PROPERTY);
        return this;
    }
    public SchoolTypeRequest samplePopulationVarianceDisplayOrder(){
        return samplePopulationVarianceDisplayOrderAs(prefix("samplePopulationVarianceOf",SchoolType.DISPLAY_ORDER_PROPERTY));
    }

    public SchoolTypeRequest samplePopulationVarianceDisplayOrderAs(String retName){
        super.samplePopulationVariance(retName, SchoolType.DISPLAY_ORDER_PROPERTY);
        return this;
    }
    public SchoolTypeRequest<T> groupByPlatformWithDetails(){
       return groupByPlatformWithDetails(Q.platforms().unlimited());
    }

    public SchoolTypeRequest<T> groupByPlatformWithDetails(PlatformRequest subRequest){
       aggregate(SchoolType.PLATFORM_PROPERTY, subRequest);
       return this;
    }






    public SchoolTypeRequest<T> groupBySchoolsWithDetails(SchoolRequest subRequest){
       aggregate(SchoolType.SCHOOL_LIST_PROPERTY, subRequest);
       return this;
    }

    public SchoolTypeRequest<T> groupByPlatformWith(PlatformRequest subRequest){
       groupBy(SchoolType.PLATFORM_PROPERTY, subRequest);
       return this;
    }
    public SchoolTypeRequest<T> groupByPlatform(){
       groupBy(SchoolType.PLATFORM_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> groupByPlatformAs(String retName){
       groupBy(retName, SchoolType.PLATFORM_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> groupByPlatformWithFunction(String retName, AggrFunction function){
       groupBy(retName, SchoolType.PLATFORM_PROPERTY, function);
       return this;
    }

    public SchoolTypeRequest<T> groupById(){
       groupBy(SchoolType.ID_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> groupByIdAs(String retName){
       groupBy(retName, SchoolType.ID_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, SchoolType.ID_PROPERTY, function);
       return this;
    }

    public SchoolTypeRequest<T> groupByName(){
       groupBy(SchoolType.NAME_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> groupByNameAs(String retName){
       groupBy(retName, SchoolType.NAME_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, SchoolType.NAME_PROPERTY, function);
       return this;
    }

    public SchoolTypeRequest<T> groupByCode(){
       groupBy(SchoolType.CODE_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> groupByCodeAs(String retName){
       groupBy(retName, SchoolType.CODE_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> groupByCodeWithFunction(String retName, AggrFunction function){
       groupBy(retName, SchoolType.CODE_PROPERTY, function);
       return this;
    }

    public SchoolTypeRequest<T> groupByDisplayOrder(){
       groupBy(SchoolType.DISPLAY_ORDER_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> groupByDisplayOrderAs(String retName){
       groupBy(retName, SchoolType.DISPLAY_ORDER_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> groupByDisplayOrderWithFunction(String retName, AggrFunction function){
       groupBy(retName, SchoolType.DISPLAY_ORDER_PROPERTY, function);
       return this;
    }

    public SchoolTypeRequest<T> groupByVersion(){
       groupBy(SchoolType.VERSION_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> groupByVersionAs(String retName){
       groupBy(retName, SchoolType.VERSION_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, SchoolType.VERSION_PROPERTY, function);
       return this;
    }



    public SchoolTypeRequest<T> orderByPlatformAscending(){
       addOrderByAscending(SchoolType.PLATFORM_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> orderByPlatformDescending(){
       addOrderByDescending(SchoolType.PLATFORM_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> orderByIdAscending(){
       addOrderByAscending(SchoolType.ID_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> orderByIdDescending(){
       addOrderByDescending(SchoolType.ID_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> orderByNameAscending(){
       addOrderByAscending(SchoolType.NAME_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> orderByNameDescending(){
       addOrderByDescending(SchoolType.NAME_PROPERTY);
       return this;
    }
    public SchoolTypeRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(SchoolType.NAME_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(SchoolType.NAME_PROPERTY);
       return this;
    }
    public SchoolTypeRequest<T> orderByCodeAscending(){
       addOrderByAscending(SchoolType.CODE_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> orderByCodeDescending(){
       addOrderByDescending(SchoolType.CODE_PROPERTY);
       return this;
    }
    public SchoolTypeRequest<T> orderByCodeAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(SchoolType.CODE_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> orderByCodeDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(SchoolType.CODE_PROPERTY);
       return this;
    }
    public SchoolTypeRequest<T> orderByDisplayOrderAscending(){
       addOrderByAscending(SchoolType.DISPLAY_ORDER_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> orderByDisplayOrderDescending(){
       addOrderByDescending(SchoolType.DISPLAY_ORDER_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> orderByVersionAscending(){
       addOrderByAscending(SchoolType.VERSION_PROPERTY);
       return this;
    }

    public SchoolTypeRequest<T> orderByVersionDescending(){
       addOrderByDescending(SchoolType.VERSION_PROPERTY);
       return this;
    }


    public SchoolTypeRequest<T> statsFromSchoolsAs(String name, SchoolRequest subRequest){
       return statsFromSchoolsAs(name, subRequest, false);
    }

    public SchoolTypeRequest<T> statsFromSchoolsAs(String name, SchoolRequest subRequest, boolean singleResult){
       subRequest.setPartitionProperty(School.SCHOOL_TYPE_PROPERTY);
       addAggregateDynamicProperty(name, subRequest, singleResult);
       return this;
    }

    public SchoolTypeRequest<T> statsFromSchools(SchoolRequest subRequest){
       return statsFromSchoolsAs(REFINEMENTS, subRequest);
    }
    public PlatformRequest rollUpToPlatform(){
       PlatformRequest platform = Q.platforms().unlimited();
       this.withPlatformMatching(platform)
           .groupByPlatformWith(platform);
       return platform;
    }






    public SchoolTypeRequest<T> countSchools(){
        return countSchoolsAs("Count");
    }

    public SchoolTypeRequest<T> countSchoolsAs(String name){
        return countSchoolsWith(name, Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> countSchoolsWith(String name, SchoolRequest subRequest){
        return statsFromSchoolsAs(name, subRequest.count(), true);
    }
    public SchoolTypeRequest<T> minStudentCapacityOfSchools(){
        return minStudentCapacityOfSchoolsAs("minStudentCapacityOfSchools");
    }

    public SchoolTypeRequest<T> minStudentCapacityOfSchoolsAs(String name){
        return minStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> minStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        return statsFromSchoolsAs(name, subRequest.minStudentCapacity(), true);
    }
    public SchoolTypeRequest<T> maxStudentCapacityOfSchools(){
        return maxStudentCapacityOfSchoolsAs("maxStudentCapacityOfSchools");
    }

    public SchoolTypeRequest<T> maxStudentCapacityOfSchoolsAs(String name){
        return maxStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> maxStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        return statsFromSchoolsAs(name, subRequest.maxStudentCapacity(), true);
    }
    public SchoolTypeRequest<T> sumStudentCapacityOfSchools(){
        return sumStudentCapacityOfSchoolsAs("sumStudentCapacityOfSchools");
    }

    public SchoolTypeRequest<T> sumStudentCapacityOfSchoolsAs(String name){
        return sumStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> sumStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        return statsFromSchoolsAs(name, subRequest.sumStudentCapacity(), true);
    }
    public SchoolTypeRequest<T> avgStudentCapacityOfSchools(){
        return avgStudentCapacityOfSchoolsAs("avgStudentCapacityOfSchools");
    }

    public SchoolTypeRequest<T> avgStudentCapacityOfSchoolsAs(String name){
        return avgStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> avgStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        return statsFromSchoolsAs(name, subRequest.avgStudentCapacity(), true);
    }
    public SchoolTypeRequest<T> standardDeviationStudentCapacityOfSchools(){
        return standardDeviationStudentCapacityOfSchoolsAs("stdDevStudentCapacityOfSchools");
    }

    public SchoolTypeRequest<T> standardDeviationStudentCapacityOfSchoolsAs(String name){
        return standardDeviationStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> standardDeviationStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        return statsFromSchoolsAs(name, subRequest.standardDeviationStudentCapacity(), true);
    }
    public SchoolTypeRequest<T> squareRootOfPopulationStandardDeviationStudentCapacityOfSchools(){
        return squareRootOfPopulationStandardDeviationStudentCapacityOfSchoolsAs("stdDevPopStudentCapacityOfSchools");
    }

    public SchoolTypeRequest<T> squareRootOfPopulationStandardDeviationStudentCapacityOfSchoolsAs(String name){
        return squareRootOfPopulationStandardDeviationStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> squareRootOfPopulationStandardDeviationStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        return statsFromSchoolsAs(name, subRequest.squareRootOfPopulationStandardDeviationStudentCapacity(), true);
    }
    public SchoolTypeRequest<T> sampleVarianceStudentCapacityOfSchools(){
        return sampleVarianceStudentCapacityOfSchoolsAs("varSampStudentCapacityOfSchools");
    }

    public SchoolTypeRequest<T> sampleVarianceStudentCapacityOfSchoolsAs(String name){
        return sampleVarianceStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> sampleVarianceStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        return statsFromSchoolsAs(name, subRequest.sampleVarianceStudentCapacity(), true);
    }
    public SchoolTypeRequest<T> samplePopulationVarianceStudentCapacityOfSchools(){
        return samplePopulationVarianceStudentCapacityOfSchoolsAs("varPopStudentCapacityOfSchools");
    }

    public SchoolTypeRequest<T> samplePopulationVarianceStudentCapacityOfSchoolsAs(String name){
        return samplePopulationVarianceStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> samplePopulationVarianceStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        return statsFromSchoolsAs(name, subRequest.samplePopulationVarianceStudentCapacity(), true);
    }

   public SchoolTypeRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform){
       return facetByPlatformAs(facetName, platform, true);
   }

   public SchoolTypeRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform, boolean includeAllFacets){
       addFacet(facetName, SchoolType.PLATFORM_PROPERTY, platform, includeAllFacets);
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public SchoolTypeRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public SchoolTypeRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public SchoolTypeRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public SchoolTypeRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public SchoolTypeRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public SchoolTypeRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}