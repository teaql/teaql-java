
package com.example.inheritancematchingservice.platform;

import com.example.inheritancematchingservice.Q;
import com.example.inheritancematchingservice.school.School;
import com.example.inheritancematchingservice.school.SchoolRequest;
import com.example.inheritancematchingservice.schooltype.SchoolType;
import com.example.inheritancematchingservice.schooltype.SchoolTypeRequest;
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
        return selectId().selectName().selectBaseUrl().selectVersion();
    }

    public PlatformRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public PlatformRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(Platform.ID_PROPERTY);
        selectProperty(Platform.NAME_PROPERTY);
        selectProperty(Platform.BASE_URL_PROPERTY);
        selectProperty(Platform.VERSION_PROPERTY);
        return this;
    }

    public PlatformRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectName().selectBaseUrl().selectVersion();
    }

    public PlatformRequest<T> selectChildren(){
        super.selectAny();
        selectSchoolTypeList().selectSchoolList();
        return selectId().selectName().selectBaseUrl().selectVersion();
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
       enhanceRelation(Platform.SCHOOL_TYPE_LIST_PROPERTY, schoolTypeList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectSchoolTypeListWith", null, Platform.SCHOOL_TYPE_LIST_PROPERTY));
       return this;
    }
    public PlatformRequest<T> selectSchoolList(){
       return selectSchoolListWith(Q.schools().selectSelf());
    }

    public PlatformRequest<T> selectSchoolListWith(SchoolRequest schoolList){
       enhanceRelation(Platform.SCHOOL_LIST_PROPERTY, schoolList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectSchoolListWith", null, Platform.SCHOOL_LIST_PROPERTY));
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
    public PlatformRequest<T> countDistinctBaseUrl(){
        return countDistinctBaseUrlAs("base_url_distinct_count");
    }

    public PlatformRequest<T> countDistinctBaseUrlAs(String alias){
        super.countDistinctProperty(alias, Platform.BASE_URL_PROPERTY);
        return this;
    }
    public PlatformRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public PlatformRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, Platform.VERSION_PROPERTY);
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
       addRelationAggregate(name, subRequest, SchoolType.PLATFORM_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromSchoolTypesAs", name, Platform.SCHOOL_TYPE_LIST_PROPERTY));
       return this;
    }

    public PlatformRequest<T> statsFromSchoolTypes(SchoolTypeRequest subRequest){
       return statsFromSchoolTypesAs(REFINEMENTS, subRequest);
    }
    public PlatformRequest<T> statsFromSchoolsAs(String name, SchoolRequest subRequest){
       return statsFromSchoolsAs(name, subRequest, false);
    }

    public PlatformRequest<T> statsFromSchoolsAs(String name, SchoolRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, School.PLATFORM_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromSchoolsAs", name, Platform.SCHOOL_LIST_PROPERTY));
       return this;
    }

    public PlatformRequest<T> statsFromSchools(SchoolRequest subRequest){
       return statsFromSchoolsAs(REFINEMENTS, subRequest);
    }
    public PlatformRequest<T> countSchoolTypes(){
        addRelationCount("Count", Q.schoolTypes().unlimited(), SchoolType.PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countSchoolTypes", null, Platform.SCHOOL_TYPE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> countSchoolTypesAs(String name){
        addRelationCount(name, Q.schoolTypes().unlimited(), SchoolType.PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countSchoolTypesAs", name, Platform.SCHOOL_TYPE_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> countSchoolTypesWith(String name, SchoolTypeRequest subRequest){
        addRelationCount(name, subRequest, SchoolType.PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countSchoolTypesWith", name, Platform.SCHOOL_TYPE_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> countSchools(){
        addRelationCount("Count", Q.schools().unlimited(), School.PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countSchools", null, Platform.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> countSchoolsAs(String name){
        addRelationCount(name, Q.schools().unlimited(), School.PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countSchoolsAs", name, Platform.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> countSchoolsWith(String name, SchoolRequest subRequest){
        addRelationCount(name, subRequest, School.PLATFORM_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countSchoolsWith", name, Platform.SCHOOL_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> minStudentCapacityOfSchools(){
        addRelationScalarAggregate("minStudentCapacityOfSchools", Q.schools().unlimited(),
            School.PLATFORM_PROPERTY, School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minStudentCapacityOfSchools", null, Platform.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> minStudentCapacityOfSchoolsAs(String name){
        return minStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public PlatformRequest<T> minStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        addRelationScalarAggregate(name, subRequest, School.PLATFORM_PROPERTY,
            School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minStudentCapacityOfSchoolsAs", name, Platform.SCHOOL_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> maxStudentCapacityOfSchools(){
        addRelationScalarAggregate("maxStudentCapacityOfSchools", Q.schools().unlimited(),
            School.PLATFORM_PROPERTY, School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxStudentCapacityOfSchools", null, Platform.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> maxStudentCapacityOfSchoolsAs(String name){
        return maxStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public PlatformRequest<T> maxStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        addRelationScalarAggregate(name, subRequest, School.PLATFORM_PROPERTY,
            School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxStudentCapacityOfSchoolsAs", name, Platform.SCHOOL_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> sumStudentCapacityOfSchools(){
        addRelationScalarAggregate("sumStudentCapacityOfSchools", Q.schools().unlimited(),
            School.PLATFORM_PROPERTY, School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumStudentCapacityOfSchools", null, Platform.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> sumStudentCapacityOfSchoolsAs(String name){
        return sumStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public PlatformRequest<T> sumStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        addRelationScalarAggregate(name, subRequest, School.PLATFORM_PROPERTY,
            School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumStudentCapacityOfSchoolsAs", name, Platform.SCHOOL_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> avgStudentCapacityOfSchools(){
        addRelationScalarAggregate("avgStudentCapacityOfSchools", Q.schools().unlimited(),
            School.PLATFORM_PROPERTY, School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgStudentCapacityOfSchools", null, Platform.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> avgStudentCapacityOfSchoolsAs(String name){
        return avgStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public PlatformRequest<T> avgStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        addRelationScalarAggregate(name, subRequest, School.PLATFORM_PROPERTY,
            School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgStudentCapacityOfSchoolsAs", name, Platform.SCHOOL_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> standardDeviationStudentCapacityOfSchools(){
        addRelationScalarAggregate("stdDevStudentCapacityOfSchools", Q.schools().unlimited(),
            School.PLATFORM_PROPERTY, School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationStudentCapacityOfSchools", null, Platform.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> standardDeviationStudentCapacityOfSchoolsAs(String name){
        return standardDeviationStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public PlatformRequest<T> standardDeviationStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        addRelationScalarAggregate(name, subRequest, School.PLATFORM_PROPERTY,
            School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationStudentCapacityOfSchoolsAs", name, Platform.SCHOOL_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> squareRootOfPopulationStandardDeviationStudentCapacityOfSchools(){
        addRelationScalarAggregate("stdDevPopStudentCapacityOfSchools", Q.schools().unlimited(),
            School.PLATFORM_PROPERTY, School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationStudentCapacityOfSchools", null, Platform.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> squareRootOfPopulationStandardDeviationStudentCapacityOfSchoolsAs(String name){
        return squareRootOfPopulationStandardDeviationStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public PlatformRequest<T> squareRootOfPopulationStandardDeviationStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        addRelationScalarAggregate(name, subRequest, School.PLATFORM_PROPERTY,
            School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationStudentCapacityOfSchoolsAs", name, Platform.SCHOOL_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> sampleVarianceStudentCapacityOfSchools(){
        addRelationScalarAggregate("varSampStudentCapacityOfSchools", Q.schools().unlimited(),
            School.PLATFORM_PROPERTY, School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceStudentCapacityOfSchools", null, Platform.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> sampleVarianceStudentCapacityOfSchoolsAs(String name){
        return sampleVarianceStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public PlatformRequest<T> sampleVarianceStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        addRelationScalarAggregate(name, subRequest, School.PLATFORM_PROPERTY,
            School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceStudentCapacityOfSchoolsAs", name, Platform.SCHOOL_LIST_PROPERTY));
        return this;
    }
    public PlatformRequest<T> samplePopulationVarianceStudentCapacityOfSchools(){
        addRelationScalarAggregate("varPopStudentCapacityOfSchools", Q.schools().unlimited(),
            School.PLATFORM_PROPERTY, School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceStudentCapacityOfSchools", null, Platform.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public PlatformRequest<T> samplePopulationVarianceStudentCapacityOfSchoolsAs(String name){
        return samplePopulationVarianceStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public PlatformRequest<T> samplePopulationVarianceStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        addRelationScalarAggregate(name, subRequest, School.PLATFORM_PROPERTY,
            School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceStudentCapacityOfSchoolsAs", name, Platform.SCHOOL_LIST_PROPERTY));
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