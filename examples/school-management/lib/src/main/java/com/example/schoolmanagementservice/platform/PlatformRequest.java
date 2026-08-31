
package com.example.schoolmanagementservice.platform;

import com.example.schoolmanagementservice.Q;
import com.example.schoolmanagementservice.school.School;
import com.example.schoolmanagementservice.school.SchoolRequest;
import com.example.schoolmanagementservice.schooltype.SchoolType;
import com.example.schoolmanagementservice.schooltype.SchoolTypeRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.time.LocalDateTime;
import java.util.Date;

public class PlatformRequest<T extends Platform> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public PlatformRequest(Class<T> returnType){
        super(returnType, () -> (T) new Platform());
        selectId();
        selectVersion();
    }

    public PlatformRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
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
        return selectId().selectName().selectBaseUrl().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public PlatformRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public PlatformRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectName().selectBaseUrl().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public PlatformRequest<T> selectChildren(){
        super.selectAny();
        selectSchoolTypeList().selectSchoolList();
        return selectId().selectName().selectBaseUrl().selectCreateTime().selectUpdateTime().selectVersion();
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
    public PlatformRequest<T> selectBaseUrl(){
       selectProperty(Platform.BASE_URL_PROPERTY);
       return this;
    }

    /**
     * fill the baseUrl with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  baseUrl) to fetch baseUrl property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PlatformRequest<T> unselectBaseUrl(){
       unselectProperty(Platform.BASE_URL_PROPERTY);
       return this;
    }
    public PlatformRequest<T> selectCreateTime(){
       selectProperty(Platform.CREATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the createTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  createTime) to fetch createTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PlatformRequest<T> unselectCreateTime(){
       unselectProperty(Platform.CREATE_TIME_PROPERTY);
       return this;
    }
    public PlatformRequest<T> selectUpdateTime(){
       selectProperty(Platform.UPDATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the updateTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  updateTime) to fetch updateTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PlatformRequest<T> unselectUpdateTime(){
       unselectProperty(Platform.UPDATE_TIME_PROPERTY);
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
    public PlatformRequest<T> selectSchoolTypeList(){
       return selectSchoolTypeListWith(Q.schoolTypes().selectSelf());
    }

    public PlatformRequest<T> selectSchoolTypeListWith(SchoolTypeRequest schoolTypeList){
       enhanceRelation(Platform.SCHOOL_TYPE_LIST_PROPERTY, schoolTypeList);
       return this;
    }
    public PlatformRequest<T> selectSchoolList(){
       return selectSchoolListWith(Q.schools().selectSelf());
    }

    public PlatformRequest<T> selectSchoolListWith(SchoolRequest schoolList){
       enhanceRelation(Platform.SCHOOL_LIST_PROPERTY, schoolList);
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



    public PlatformRequest<T> filterByBaseUrl(String... baseUrl){
      if (baseUrl == null || baseUrl.length == 0) {
        throw new IllegalArgumentException("filterByBaseUrl parameter baseUrl cannot be empty");
      }
      return appendSearchCriteria(createBaseUrlCriteria(Operator.EQUAL, (Object[])baseUrl));
    }

    public PlatformRequest<T> withBaseUrl(Operator operator, Object... values){
       return appendSearchCriteria(createBaseUrlCriteria(operator, values));
    }

    public PlatformRequest<T> withBaseUrlIsUnknown(){
       return withBaseUrl(Operator.IS_NULL);
    }

    public PlatformRequest<T> withBaseUrlIsKnown(){
       return withBaseUrl(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createBaseUrlCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Platform.BASE_URL_PROPERTY, operator, values);
    }

    public PlatformRequest<T> withBaseUrlIsNot(String baseUrl){
       return withBaseUrl(Operator.NOT_EQUAL, baseUrl);
    }

    public PlatformRequest<T> withBaseUrlIn(String... baseUrl){
       return withBaseUrl(Operator.IN, (Object[])baseUrl);
    }

    public PlatformRequest<T> withBaseUrlNotIn(String... baseUrl){
       return withBaseUrl(Operator.NOT_IN, (Object[])baseUrl);
    }
    public PlatformRequest<T> withBaseUrlGreaterThan(String baseUrl){
       return withBaseUrl(Operator.GREATER_THAN, baseUrl);
    }

    public PlatformRequest<T> withBaseUrlGreaterThanOrEqualTo(String baseUrl){
       return withBaseUrl(Operator.GREATER_THAN_OR_EQUAL, baseUrl);
    }

    public PlatformRequest<T> withBaseUrlLessThan(String baseUrl){
       return withBaseUrl(Operator.LESS_THAN, baseUrl);
    }

    public PlatformRequest<T> withBaseUrlLessThanOrEqualTo(String baseUrl){
       return withBaseUrl(Operator.LESS_THAN_OR_EQUAL, baseUrl);
    }

    public PlatformRequest<T> withBaseUrlBetween(String startOfBaseUrl, String endOfBaseUrl){
       return withBaseUrl(Operator.BETWEEN, startOfBaseUrl, endOfBaseUrl);
    }
    public PlatformRequest<T> withBaseUrlStartingWith(String baseUrl){
       return withBaseUrl(Operator.BEGIN_WITH, baseUrl);
    }
    public PlatformRequest<T> withBaseUrlContaining(String baseUrl){
       return withBaseUrl(Operator.CONTAIN, baseUrl);
    }

    public PlatformRequest<T> withBaseUrlNotContaining(String baseUrl){
       return withBaseUrl(Operator.NOT_CONTAIN, baseUrl);
    }

    public PlatformRequest<T> withBaseUrlNotStartingWith(String baseUrl){
       return withBaseUrl(Operator.NOT_BEGIN_WITH, baseUrl);
    }

    public PlatformRequest<T> withBaseUrlEndingWith(String baseUrl){
       return withBaseUrl(Operator.END_WITH, baseUrl);
    }

    public PlatformRequest<T> withBaseUrlNotEndingWith(String baseUrl){
       return withBaseUrl(Operator.NOT_END_WITH, baseUrl);
    }

    public PlatformRequest<T> withBaseUrlIs(String baseUrl){
       return withBaseUrl(Operator.EQUAL, baseUrl);
    }

    public PlatformRequest<T> withBaseUrlSoundingLike(String baseUrl){
       return withBaseUrl(Operator.SOUNDS_LIKE, baseUrl);
    }



    public PlatformRequest<T> filterByCreateTime(LocalDateTime... createTime){
      if (createTime == null || createTime.length == 0) {
        throw new IllegalArgumentException("filterByCreateTime parameter createTime cannot be empty");
      }
      return appendSearchCriteria(createCreateTimeCriteria(Operator.EQUAL, (Object[])createTime));
    }

    public PlatformRequest<T> withCreateTime(Operator operator, Object... values){
       return appendSearchCriteria(createCreateTimeCriteria(operator, values));
    }

    public PlatformRequest<T> withCreateTimeIsUnknown(){
       return withCreateTime(Operator.IS_NULL);
    }

    public PlatformRequest<T> withCreateTimeIsKnown(){
       return withCreateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCreateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Platform.CREATE_TIME_PROPERTY, operator, values);
    }

    public PlatformRequest<T> withCreateTimeIs(LocalDateTime createTime){
       return withCreateTime(Operator.EQUAL, createTime);
    }

    public PlatformRequest<T> withCreateTimeIsNot(LocalDateTime createTime){
       return withCreateTime(Operator.NOT_EQUAL, createTime);
    }

    public PlatformRequest<T> withCreateTimeIn(LocalDateTime... createTime){
       return withCreateTime(Operator.IN, (Object[])createTime);
    }

    public PlatformRequest<T> withCreateTimeNotIn(LocalDateTime... createTime){
       return withCreateTime(Operator.NOT_IN, (Object[])createTime);
    }
    public PlatformRequest<T> withCreateTimeGreaterThan(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public PlatformRequest<T> withCreateTimeGreaterThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN_OR_EQUAL, createTime);
    }

    public PlatformRequest<T> withCreateTimeLessThan(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public PlatformRequest<T> withCreateTimeLessThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN_OR_EQUAL, createTime);
    }

    public PlatformRequest<T> withCreateTimeBetween(LocalDateTime startOfCreateTime, LocalDateTime endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }
    public PlatformRequest<T> withCreateTimeBefore(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public PlatformRequest<T> withCreateTimeBefore(Date createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public PlatformRequest<T> withCreateTimeAfter(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public PlatformRequest<T> withCreateTimeAfter(Date createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public PlatformRequest<T> withCreateTimeBetween(Date startOfCreateTime, Date endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }




    public PlatformRequest<T> filterByUpdateTime(LocalDateTime... updateTime){
      if (updateTime == null || updateTime.length == 0) {
        throw new IllegalArgumentException("filterByUpdateTime parameter updateTime cannot be empty");
      }
      return appendSearchCriteria(createUpdateTimeCriteria(Operator.EQUAL, (Object[])updateTime));
    }

    public PlatformRequest<T> withUpdateTime(Operator operator, Object... values){
       return appendSearchCriteria(createUpdateTimeCriteria(operator, values));
    }

    public PlatformRequest<T> withUpdateTimeIsUnknown(){
       return withUpdateTime(Operator.IS_NULL);
    }

    public PlatformRequest<T> withUpdateTimeIsKnown(){
       return withUpdateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createUpdateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Platform.UPDATE_TIME_PROPERTY, operator, values);
    }

    public PlatformRequest<T> withUpdateTimeIs(LocalDateTime updateTime){
       return withUpdateTime(Operator.EQUAL, updateTime);
    }

    public PlatformRequest<T> withUpdateTimeIsNot(LocalDateTime updateTime){
       return withUpdateTime(Operator.NOT_EQUAL, updateTime);
    }

    public PlatformRequest<T> withUpdateTimeIn(LocalDateTime... updateTime){
       return withUpdateTime(Operator.IN, (Object[])updateTime);
    }

    public PlatformRequest<T> withUpdateTimeNotIn(LocalDateTime... updateTime){
       return withUpdateTime(Operator.NOT_IN, (Object[])updateTime);
    }
    public PlatformRequest<T> withUpdateTimeGreaterThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public PlatformRequest<T> withUpdateTimeGreaterThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN_OR_EQUAL, updateTime);
    }

    public PlatformRequest<T> withUpdateTimeLessThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public PlatformRequest<T> withUpdateTimeLessThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN_OR_EQUAL, updateTime);
    }

    public PlatformRequest<T> withUpdateTimeBetween(LocalDateTime startOfUpdateTime, LocalDateTime endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
    }
    public PlatformRequest<T> withUpdateTimeBefore(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public PlatformRequest<T> withUpdateTimeBefore(Date updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public PlatformRequest<T> withUpdateTimeAfter(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public PlatformRequest<T> withUpdateTimeAfter(Date updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public PlatformRequest<T> withUpdateTimeBetween(Date startOfUpdateTime, Date endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
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

    public PlatformRequest<T> withSchoolTypeListMatching(SchoolTypeRequest schoolTypeRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(Platform.ID_PROPERTY, schoolTypeRequest, SchoolType.PLATFORM_PROPERTY));
    }

    public PlatformRequest<T> withoutSchoolTypeListMatching(SchoolTypeRequest schoolTypeRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(Platform.ID_PROPERTY, schoolTypeRequest, SchoolType.PLATFORM_PROPERTY)));
    }

    public PlatformRequest<T> haveSchoolTypes(){
        return withSchoolTypeListMatching(Q.schoolTypes().unlimited());
    }

    public PlatformRequest<T> haveNoSchoolTypes(){
        return withoutSchoolTypeListMatching(Q.schoolTypes().unlimited());
    }
    public PlatformRequest<T> withSchoolListMatching(SchoolRequest schoolRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(Platform.ID_PROPERTY, schoolRequest, School.PLATFORM_PROPERTY));
    }

    public PlatformRequest<T> withoutSchoolListMatching(SchoolRequest schoolRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(Platform.ID_PROPERTY, schoolRequest, School.PLATFORM_PROPERTY)));
    }

    public PlatformRequest<T> haveSchools(){
        return withSchoolListMatching(Q.schools().unlimited());
    }

    public PlatformRequest<T> haveNoSchools(){
        return withoutSchoolListMatching(Q.schools().unlimited());
    }

    public PlatformRequest<T> count(){
        super.count();
        return this;
    }
    public PlatformRequest<T> countAs(String retName){
        super.count(retName);
        return this;
    }
    public PlatformRequest<T> groupBySchoolTypesWithDetails(SchoolTypeRequest subRequest){
       aggregate(Platform.SCHOOL_TYPE_LIST_PROPERTY, subRequest);
       return this;
    }
    public PlatformRequest<T> groupBySchoolsWithDetails(SchoolRequest subRequest){
       aggregate(Platform.SCHOOL_LIST_PROPERTY, subRequest);
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

    public PlatformRequest<T> groupByBaseUrl(){
       groupBy(Platform.BASE_URL_PROPERTY);
       return this;
    }

    public PlatformRequest<T> groupByBaseUrlAs(String retName){
       groupBy(retName, Platform.BASE_URL_PROPERTY);
       return this;
    }

    public PlatformRequest<T> groupByBaseUrlWithFunction(String retName, AggrFunction function){
       groupBy(retName, Platform.BASE_URL_PROPERTY, function);
       return this;
    }

    public PlatformRequest<T> groupByCreateTime(){
       groupBy(Platform.CREATE_TIME_PROPERTY);
       return this;
    }

    public PlatformRequest<T> groupByCreateTimeAs(String retName){
       groupBy(retName, Platform.CREATE_TIME_PROPERTY);
       return this;
    }

    public PlatformRequest<T> groupByCreateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, Platform.CREATE_TIME_PROPERTY, function);
       return this;
    }

    public PlatformRequest<T> groupByUpdateTime(){
       groupBy(Platform.UPDATE_TIME_PROPERTY);
       return this;
    }

    public PlatformRequest<T> groupByUpdateTimeAs(String retName){
       groupBy(retName, Platform.UPDATE_TIME_PROPERTY);
       return this;
    }

    public PlatformRequest<T> groupByUpdateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, Platform.UPDATE_TIME_PROPERTY, function);
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
    public PlatformRequest<T> orderByBaseUrlAscending(){
       addOrderByAscending(Platform.BASE_URL_PROPERTY);
       return this;
    }

    public PlatformRequest<T> orderByBaseUrlDescending(){
       addOrderByDescending(Platform.BASE_URL_PROPERTY);
       return this;
    }
    public PlatformRequest<T> orderByBaseUrlAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Platform.BASE_URL_PROPERTY);
       return this;
    }

    public PlatformRequest<T> orderByBaseUrlDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Platform.BASE_URL_PROPERTY);
       return this;
    }
    public PlatformRequest<T> orderByCreateTimeAscending(){
       addOrderByAscending(Platform.CREATE_TIME_PROPERTY);
       return this;
    }

    public PlatformRequest<T> orderByCreateTimeDescending(){
       addOrderByDescending(Platform.CREATE_TIME_PROPERTY);
       return this;
    }

    public PlatformRequest<T> orderByUpdateTimeAscending(){
       addOrderByAscending(Platform.UPDATE_TIME_PROPERTY);
       return this;
    }

    public PlatformRequest<T> orderByUpdateTimeDescending(){
       addOrderByDescending(Platform.UPDATE_TIME_PROPERTY);
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


    public PlatformRequest<T> statsFromSchoolTypesAs(String name, SchoolTypeRequest subRequest){
       return statsFromSchoolTypesAs(name, subRequest, false);
    }

    public PlatformRequest<T> statsFromSchoolTypesAs(String name, SchoolTypeRequest subRequest, boolean singleResult){
       subRequest.setPartitionProperty(SchoolType.PLATFORM_PROPERTY);
       addAggregateDynamicProperty(name, subRequest, singleResult);
       return this;
    }

    public PlatformRequest<T> statsFromSchoolTypes(SchoolTypeRequest subRequest){
       return statsFromSchoolTypesAs(REFINEMENTS, subRequest);
    }
    public PlatformRequest<T> statsFromSchoolsAs(String name, SchoolRequest subRequest){
       return statsFromSchoolsAs(name, subRequest, false);
    }

    public PlatformRequest<T> statsFromSchoolsAs(String name, SchoolRequest subRequest, boolean singleResult){
       subRequest.setPartitionProperty(School.PLATFORM_PROPERTY);
       addAggregateDynamicProperty(name, subRequest, singleResult);
       return this;
    }

    public PlatformRequest<T> statsFromSchools(SchoolRequest subRequest){
       return statsFromSchoolsAs(REFINEMENTS, subRequest);
    }
    public PlatformRequest<T> countSchoolTypes(){
        return countSchoolTypesAs("Count");
    }

    public PlatformRequest<T> countSchoolTypesAs(String name){
        return countSchoolTypesWith(name, Q.schoolTypes().unlimited());
    }

    public PlatformRequest<T> countSchoolTypesWith(String name, SchoolTypeRequest subRequest){
        return statsFromSchoolTypesAs(name, subRequest.count(), true);
    }
    public PlatformRequest<T> countSchools(){
        return countSchoolsAs("Count");
    }

    public PlatformRequest<T> countSchoolsAs(String name){
        return countSchoolsWith(name, Q.schools().unlimited());
    }

    public PlatformRequest<T> countSchoolsWith(String name, SchoolRequest subRequest){
        return statsFromSchoolsAs(name, subRequest.count(), true);
    }
    public PlatformRequest<T> minDisplayOrderOfSchoolTypes(){
        return minDisplayOrderOfSchoolTypesAs("minDisplayOrderOfSchoolTypes");
    }

    public PlatformRequest<T> minDisplayOrderOfSchoolTypesAs(String name){
        return minDisplayOrderOfSchoolTypesAs(name, Q.schoolTypes().unlimited());
    }

    public PlatformRequest<T> minDisplayOrderOfSchoolTypesAs(String name, SchoolTypeRequest subRequest){
        return statsFromSchoolTypesAs(name, subRequest.minDisplayOrder(), true);
    }
    public PlatformRequest<T> maxDisplayOrderOfSchoolTypes(){
        return maxDisplayOrderOfSchoolTypesAs("maxDisplayOrderOfSchoolTypes");
    }

    public PlatformRequest<T> maxDisplayOrderOfSchoolTypesAs(String name){
        return maxDisplayOrderOfSchoolTypesAs(name, Q.schoolTypes().unlimited());
    }

    public PlatformRequest<T> maxDisplayOrderOfSchoolTypesAs(String name, SchoolTypeRequest subRequest){
        return statsFromSchoolTypesAs(name, subRequest.maxDisplayOrder(), true);
    }
    public PlatformRequest<T> sumDisplayOrderOfSchoolTypes(){
        return sumDisplayOrderOfSchoolTypesAs("sumDisplayOrderOfSchoolTypes");
    }

    public PlatformRequest<T> sumDisplayOrderOfSchoolTypesAs(String name){
        return sumDisplayOrderOfSchoolTypesAs(name, Q.schoolTypes().unlimited());
    }

    public PlatformRequest<T> sumDisplayOrderOfSchoolTypesAs(String name, SchoolTypeRequest subRequest){
        return statsFromSchoolTypesAs(name, subRequest.sumDisplayOrder(), true);
    }
    public PlatformRequest<T> avgDisplayOrderOfSchoolTypes(){
        return avgDisplayOrderOfSchoolTypesAs("avgDisplayOrderOfSchoolTypes");
    }

    public PlatformRequest<T> avgDisplayOrderOfSchoolTypesAs(String name){
        return avgDisplayOrderOfSchoolTypesAs(name, Q.schoolTypes().unlimited());
    }

    public PlatformRequest<T> avgDisplayOrderOfSchoolTypesAs(String name, SchoolTypeRequest subRequest){
        return statsFromSchoolTypesAs(name, subRequest.avgDisplayOrder(), true);
    }
    public PlatformRequest<T> standardDeviationDisplayOrderOfSchoolTypes(){
        return standardDeviationDisplayOrderOfSchoolTypesAs("stdDevDisplayOrderOfSchoolTypes");
    }

    public PlatformRequest<T> standardDeviationDisplayOrderOfSchoolTypesAs(String name){
        return standardDeviationDisplayOrderOfSchoolTypesAs(name, Q.schoolTypes().unlimited());
    }

    public PlatformRequest<T> standardDeviationDisplayOrderOfSchoolTypesAs(String name, SchoolTypeRequest subRequest){
        return statsFromSchoolTypesAs(name, subRequest.standardDeviationDisplayOrder(), true);
    }
    public PlatformRequest<T> squareRootOfPopulationStandardDeviationDisplayOrderOfSchoolTypes(){
        return squareRootOfPopulationStandardDeviationDisplayOrderOfSchoolTypesAs("stdDevPopDisplayOrderOfSchoolTypes");
    }

    public PlatformRequest<T> squareRootOfPopulationStandardDeviationDisplayOrderOfSchoolTypesAs(String name){
        return squareRootOfPopulationStandardDeviationDisplayOrderOfSchoolTypesAs(name, Q.schoolTypes().unlimited());
    }

    public PlatformRequest<T> squareRootOfPopulationStandardDeviationDisplayOrderOfSchoolTypesAs(String name, SchoolTypeRequest subRequest){
        return statsFromSchoolTypesAs(name, subRequest.squareRootOfPopulationStandardDeviationDisplayOrder(), true);
    }
    public PlatformRequest<T> sampleVarianceDisplayOrderOfSchoolTypes(){
        return sampleVarianceDisplayOrderOfSchoolTypesAs("varSampDisplayOrderOfSchoolTypes");
    }

    public PlatformRequest<T> sampleVarianceDisplayOrderOfSchoolTypesAs(String name){
        return sampleVarianceDisplayOrderOfSchoolTypesAs(name, Q.schoolTypes().unlimited());
    }

    public PlatformRequest<T> sampleVarianceDisplayOrderOfSchoolTypesAs(String name, SchoolTypeRequest subRequest){
        return statsFromSchoolTypesAs(name, subRequest.sampleVarianceDisplayOrder(), true);
    }
    public PlatformRequest<T> samplePopulationVarianceDisplayOrderOfSchoolTypes(){
        return samplePopulationVarianceDisplayOrderOfSchoolTypesAs("varPopDisplayOrderOfSchoolTypes");
    }

    public PlatformRequest<T> samplePopulationVarianceDisplayOrderOfSchoolTypesAs(String name){
        return samplePopulationVarianceDisplayOrderOfSchoolTypesAs(name, Q.schoolTypes().unlimited());
    }

    public PlatformRequest<T> samplePopulationVarianceDisplayOrderOfSchoolTypesAs(String name, SchoolTypeRequest subRequest){
        return statsFromSchoolTypesAs(name, subRequest.samplePopulationVarianceDisplayOrder(), true);
    }
    public PlatformRequest<T> minStudentCapacityOfSchools(){
        return minStudentCapacityOfSchoolsAs("minStudentCapacityOfSchools");
    }

    public PlatformRequest<T> minStudentCapacityOfSchoolsAs(String name){
        return minStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public PlatformRequest<T> minStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        return statsFromSchoolsAs(name, subRequest.minStudentCapacity(), true);
    }
    public PlatformRequest<T> maxStudentCapacityOfSchools(){
        return maxStudentCapacityOfSchoolsAs("maxStudentCapacityOfSchools");
    }

    public PlatformRequest<T> maxStudentCapacityOfSchoolsAs(String name){
        return maxStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public PlatformRequest<T> maxStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        return statsFromSchoolsAs(name, subRequest.maxStudentCapacity(), true);
    }
    public PlatformRequest<T> sumStudentCapacityOfSchools(){
        return sumStudentCapacityOfSchoolsAs("sumStudentCapacityOfSchools");
    }

    public PlatformRequest<T> sumStudentCapacityOfSchoolsAs(String name){
        return sumStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public PlatformRequest<T> sumStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        return statsFromSchoolsAs(name, subRequest.sumStudentCapacity(), true);
    }
    public PlatformRequest<T> avgStudentCapacityOfSchools(){
        return avgStudentCapacityOfSchoolsAs("avgStudentCapacityOfSchools");
    }

    public PlatformRequest<T> avgStudentCapacityOfSchoolsAs(String name){
        return avgStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public PlatformRequest<T> avgStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        return statsFromSchoolsAs(name, subRequest.avgStudentCapacity(), true);
    }
    public PlatformRequest<T> standardDeviationStudentCapacityOfSchools(){
        return standardDeviationStudentCapacityOfSchoolsAs("stdDevStudentCapacityOfSchools");
    }

    public PlatformRequest<T> standardDeviationStudentCapacityOfSchoolsAs(String name){
        return standardDeviationStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public PlatformRequest<T> standardDeviationStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        return statsFromSchoolsAs(name, subRequest.standardDeviationStudentCapacity(), true);
    }
    public PlatformRequest<T> squareRootOfPopulationStandardDeviationStudentCapacityOfSchools(){
        return squareRootOfPopulationStandardDeviationStudentCapacityOfSchoolsAs("stdDevPopStudentCapacityOfSchools");
    }

    public PlatformRequest<T> squareRootOfPopulationStandardDeviationStudentCapacityOfSchoolsAs(String name){
        return squareRootOfPopulationStandardDeviationStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public PlatformRequest<T> squareRootOfPopulationStandardDeviationStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        return statsFromSchoolsAs(name, subRequest.squareRootOfPopulationStandardDeviationStudentCapacity(), true);
    }
    public PlatformRequest<T> sampleVarianceStudentCapacityOfSchools(){
        return sampleVarianceStudentCapacityOfSchoolsAs("varSampStudentCapacityOfSchools");
    }

    public PlatformRequest<T> sampleVarianceStudentCapacityOfSchoolsAs(String name){
        return sampleVarianceStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public PlatformRequest<T> sampleVarianceStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        return statsFromSchoolsAs(name, subRequest.sampleVarianceStudentCapacity(), true);
    }
    public PlatformRequest<T> samplePopulationVarianceStudentCapacityOfSchools(){
        return samplePopulationVarianceStudentCapacityOfSchoolsAs("varPopStudentCapacityOfSchools");
    }

    public PlatformRequest<T> samplePopulationVarianceStudentCapacityOfSchoolsAs(String name){
        return samplePopulationVarianceStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public PlatformRequest<T> samplePopulationVarianceStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        return statsFromSchoolsAs(name, subRequest.samplePopulationVarianceStudentCapacity(), true);
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