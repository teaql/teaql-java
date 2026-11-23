
package com.example.inheritancematchingservice.schooltype;

import com.example.inheritancematchingservice.Q;
import com.example.inheritancematchingservice.platform.Platform;
import com.example.inheritancematchingservice.platform.PlatformRequest;
import com.example.inheritancematchingservice.school.School;
import com.example.inheritancematchingservice.school.SchoolRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.util.Objects;

public class SchoolTypeRequest<T extends SchoolType> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public SchoolTypeRequest(Class<T> returnType){
        this(returnType, () -> (T) new SchoolType());
    }

    protected SchoolTypeRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public SchoolTypeRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public SchoolTypeRequest<T> copy(){
        return (SchoolTypeRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new SchoolTypeRequest<>((Class<T>) returnType(), this::internalNewEntity);
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

    public SchoolTypeRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public SchoolTypeRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public SchoolTypeRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
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

    public SchoolTypeRequest<T> matchingIdsOf(SchoolTypeRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                SchoolType.ID_PROPERTY, request, SchoolType.ID_PROPERTY));
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
        return selectPlatformIdOnly().selectId().selectName().selectCode().selectVersion();
    }

    public SchoolTypeRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public SchoolTypeRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(SchoolType.PLATFORM_PROPERTY);
        selectProperty(SchoolType.ID_PROPERTY);
        selectProperty(SchoolType.NAME_PROPERTY);
        selectProperty(SchoolType.CODE_PROPERTY);
        selectProperty(SchoolType.VERSION_PROPERTY);
        return this;
    }

    public SchoolTypeRequest<T> selectAll(){
        super.selectAll();
        return selectPlatform().selectId().selectName().selectCode().selectVersion();
    }

    public SchoolTypeRequest<T> selectChildren(){
        super.selectAny();
        selectSchoolList();
        return selectPlatform().selectId().selectName().selectCode().selectVersion();
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
       enhanceRelation(SchoolType.PLATFORM_PROPERTY, platform,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectPlatformWith", null, SchoolType.PLATFORM_PROPERTY));
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
       enhanceRelation(SchoolType.SCHOOL_LIST_PROPERTY, schoolList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectSchoolListWith", null, SchoolType.SCHOOL_LIST_PROPERTY));
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

    public SchoolTypeRequest<T> withoutPlatformMatching(PlatformRequest platform){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(SchoolType.PLATFORM_PROPERTY, platform, Platform.ID_PROPERTY)));
    }

    public SchoolTypeRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SchoolType.ID_PROPERTY, operator, values);
    }

    public SchoolTypeRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public SchoolTypeRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public SchoolTypeRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public SchoolTypeRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
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

    public SchoolTypeRequest<T> withNameIsNot(String name){
       return withName(Operator.NOT_EQUAL, name);
    }

    public SchoolTypeRequest<T> withNameIn(String... name){
       return withName(Operator.IN, (Object[])name);
    }

    public SchoolTypeRequest<T> withNameNotIn(String... name){
       return withName(Operator.NOT_IN, (Object[])name);
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

    public SchoolTypeRequest<T> withNameNotContaining(String name){
       return withName(Operator.NOT_CONTAIN, name);
    }

    public SchoolTypeRequest<T> withNameNotStartingWith(String name){
       return withName(Operator.NOT_BEGIN_WITH, name);
    }

    public SchoolTypeRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public SchoolTypeRequest<T> withNameNotEndingWith(String name){
       return withName(Operator.NOT_END_WITH, name);
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

    public SchoolTypeRequest<T> withCodeIsNot(String code){
       return withCode(Operator.NOT_EQUAL, code);
    }

    public SchoolTypeRequest<T> withCodeIn(String... code){
       return withCode(Operator.IN, (Object[])code);
    }

    public SchoolTypeRequest<T> withCodeNotIn(String... code){
       return withCode(Operator.NOT_IN, (Object[])code);
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

    public SchoolTypeRequest<T> withCodeNotContaining(String code){
       return withCode(Operator.NOT_CONTAIN, code);
    }

    public SchoolTypeRequest<T> withCodeNotStartingWith(String code){
       return withCode(Operator.NOT_BEGIN_WITH, code);
    }

    public SchoolTypeRequest<T> withCodeEndingWith(String code){
       return withCode(Operator.END_WITH, code);
    }

    public SchoolTypeRequest<T> withCodeNotEndingWith(String code){
       return withCode(Operator.NOT_END_WITH, code);
    }

    public SchoolTypeRequest<T> withCodeIs(String code){
       return withCode(Operator.EQUAL, code);
    }

    public SchoolTypeRequest<T> withCodeSoundingLike(String code){
       return withCode(Operator.SOUNDS_LIKE, code);
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

    public SchoolTypeRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public SchoolTypeRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public SchoolTypeRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public SchoolTypeRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
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
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public SchoolTypeRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public SchoolTypeRequest<T> groupByPlatformWithDetails(){
       return groupByPlatformWithDetails(Q.platforms().unlimited());
    }

    public SchoolTypeRequest<T> groupByPlatformWithDetails(PlatformRequest subRequest){
       aggregate(SchoolType.PLATFORM_PROPERTY, subRequest);
       return this;
    }





    public SchoolTypeRequest<T> countDistinctPlatform(){
        return countDistinctPlatformAs("platform_distinct_count");
    }

    public SchoolTypeRequest<T> countDistinctPlatformAs(String alias){
        super.countDistinctProperty(alias, SchoolType.PLATFORM_PROPERTY);
        return this;
    }
    public SchoolTypeRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public SchoolTypeRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, SchoolType.ID_PROPERTY);
        return this;
    }
    public SchoolTypeRequest<T> countDistinctName(){
        return countDistinctNameAs("name_distinct_count");
    }

    public SchoolTypeRequest<T> countDistinctNameAs(String alias){
        super.countDistinctProperty(alias, SchoolType.NAME_PROPERTY);
        return this;
    }
    public SchoolTypeRequest<T> countDistinctCode(){
        return countDistinctCodeAs("code_distinct_count");
    }

    public SchoolTypeRequest<T> countDistinctCodeAs(String alias){
        super.countDistinctProperty(alias, SchoolType.CODE_PROPERTY);
        return this;
    }
    public SchoolTypeRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public SchoolTypeRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, SchoolType.VERSION_PROPERTY);
        return this;
    }
    public SchoolTypeRequest<T> groupBySchoolsWithDetails(SchoolRequest subRequest){
       aggregate(SchoolType.SCHOOL_LIST_PROPERTY, subRequest);
       return this;
    }

    public SchoolTypeRequest<T> groupByPlatformWith(PlatformRequest subRequest){
       groupBy(SchoolType.PLATFORM_PROPERTY, subRequest,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "groupByPlatformWith", null, "platform"));
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
       addRelationAggregate(name, subRequest, School.SCHOOL_TYPE_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromSchoolsAs", name, SchoolType.SCHOOL_LIST_PROPERTY));
       return this;
    }

    public SchoolTypeRequest<T> statsFromSchools(SchoolRequest subRequest){
       return statsFromSchoolsAs(REFINEMENTS, subRequest);
    }
    public PlatformRequest rollUpToPlatform(){
       PlatformRequest platform = Q.platforms().unlimited();
       this.internalRollUpTo("platform", platform,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "rollUpToPlatform", null, "platform"));
       return platform;
    }





    public SchoolTypeRequest<T> countSchools(){
        addRelationCount("Count", Q.schools().unlimited(), School.SCHOOL_TYPE_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countSchools", null, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public SchoolTypeRequest<T> countSchoolsAs(String name){
        addRelationCount(name, Q.schools().unlimited(), School.SCHOOL_TYPE_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countSchoolsAs", name, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public SchoolTypeRequest<T> countSchoolsWith(String name, SchoolRequest subRequest){
        addRelationCount(name, subRequest, School.SCHOOL_TYPE_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countSchoolsWith", name, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }
    public SchoolTypeRequest<T> minStudentCapacityOfSchools(){
        addRelationScalarAggregate("minStudentCapacityOfSchools", Q.schools().unlimited(),
            School.SCHOOL_TYPE_PROPERTY, School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minStudentCapacityOfSchools", null, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public SchoolTypeRequest<T> minStudentCapacityOfSchoolsAs(String name){
        return minStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> minStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        addRelationScalarAggregate(name, subRequest, School.SCHOOL_TYPE_PROPERTY,
            School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minStudentCapacityOfSchoolsAs", name, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }
    public SchoolTypeRequest<T> maxStudentCapacityOfSchools(){
        addRelationScalarAggregate("maxStudentCapacityOfSchools", Q.schools().unlimited(),
            School.SCHOOL_TYPE_PROPERTY, School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxStudentCapacityOfSchools", null, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public SchoolTypeRequest<T> maxStudentCapacityOfSchoolsAs(String name){
        return maxStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> maxStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        addRelationScalarAggregate(name, subRequest, School.SCHOOL_TYPE_PROPERTY,
            School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxStudentCapacityOfSchoolsAs", name, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }
    public SchoolTypeRequest<T> sumStudentCapacityOfSchools(){
        addRelationScalarAggregate("sumStudentCapacityOfSchools", Q.schools().unlimited(),
            School.SCHOOL_TYPE_PROPERTY, School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumStudentCapacityOfSchools", null, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public SchoolTypeRequest<T> sumStudentCapacityOfSchoolsAs(String name){
        return sumStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> sumStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        addRelationScalarAggregate(name, subRequest, School.SCHOOL_TYPE_PROPERTY,
            School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumStudentCapacityOfSchoolsAs", name, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }
    public SchoolTypeRequest<T> avgStudentCapacityOfSchools(){
        addRelationScalarAggregate("avgStudentCapacityOfSchools", Q.schools().unlimited(),
            School.SCHOOL_TYPE_PROPERTY, School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgStudentCapacityOfSchools", null, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public SchoolTypeRequest<T> avgStudentCapacityOfSchoolsAs(String name){
        return avgStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> avgStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        addRelationScalarAggregate(name, subRequest, School.SCHOOL_TYPE_PROPERTY,
            School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgStudentCapacityOfSchoolsAs", name, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }
    public SchoolTypeRequest<T> standardDeviationStudentCapacityOfSchools(){
        addRelationScalarAggregate("stdDevStudentCapacityOfSchools", Q.schools().unlimited(),
            School.SCHOOL_TYPE_PROPERTY, School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationStudentCapacityOfSchools", null, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public SchoolTypeRequest<T> standardDeviationStudentCapacityOfSchoolsAs(String name){
        return standardDeviationStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> standardDeviationStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        addRelationScalarAggregate(name, subRequest, School.SCHOOL_TYPE_PROPERTY,
            School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationStudentCapacityOfSchoolsAs", name, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }
    public SchoolTypeRequest<T> squareRootOfPopulationStandardDeviationStudentCapacityOfSchools(){
        addRelationScalarAggregate("stdDevPopStudentCapacityOfSchools", Q.schools().unlimited(),
            School.SCHOOL_TYPE_PROPERTY, School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationStudentCapacityOfSchools", null, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public SchoolTypeRequest<T> squareRootOfPopulationStandardDeviationStudentCapacityOfSchoolsAs(String name){
        return squareRootOfPopulationStandardDeviationStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> squareRootOfPopulationStandardDeviationStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        addRelationScalarAggregate(name, subRequest, School.SCHOOL_TYPE_PROPERTY,
            School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationStudentCapacityOfSchoolsAs", name, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }
    public SchoolTypeRequest<T> sampleVarianceStudentCapacityOfSchools(){
        addRelationScalarAggregate("varSampStudentCapacityOfSchools", Q.schools().unlimited(),
            School.SCHOOL_TYPE_PROPERTY, School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceStudentCapacityOfSchools", null, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public SchoolTypeRequest<T> sampleVarianceStudentCapacityOfSchoolsAs(String name){
        return sampleVarianceStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> sampleVarianceStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        addRelationScalarAggregate(name, subRequest, School.SCHOOL_TYPE_PROPERTY,
            School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceStudentCapacityOfSchoolsAs", name, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }
    public SchoolTypeRequest<T> samplePopulationVarianceStudentCapacityOfSchools(){
        addRelationScalarAggregate("varPopStudentCapacityOfSchools", Q.schools().unlimited(),
            School.SCHOOL_TYPE_PROPERTY, School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceStudentCapacityOfSchools", null, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }

    public SchoolTypeRequest<T> samplePopulationVarianceStudentCapacityOfSchoolsAs(String name){
        return samplePopulationVarianceStudentCapacityOfSchoolsAs(name, Q.schools().unlimited());
    }

    public SchoolTypeRequest<T> samplePopulationVarianceStudentCapacityOfSchoolsAs(String name, SchoolRequest subRequest){
        addRelationScalarAggregate(name, subRequest, School.SCHOOL_TYPE_PROPERTY,
            School.STUDENT_CAPACITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceStudentCapacityOfSchoolsAs", name, SchoolType.SCHOOL_LIST_PROPERTY));
        return this;
    }

   public SchoolTypeRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform){
       return facetByPlatformAs(facetName, platform, true);
   }

   public SchoolTypeRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform, boolean includeAllFacets){
       addFacet(facetName, SchoolType.PLATFORM_PROPERTY, platform, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByPlatformAs", facetName, SchoolType.PLATFORM_PROPERTY));
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