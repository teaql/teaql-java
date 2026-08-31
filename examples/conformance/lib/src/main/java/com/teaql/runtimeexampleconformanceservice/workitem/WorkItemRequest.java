
package com.teaql.runtimeexampleconformanceservice.workitem;

import com.teaql.runtimeexampleconformanceservice.Q;
import com.teaql.runtimeexampleconformanceservice.platform.Platform;
import com.teaql.runtimeexampleconformanceservice.platform.PlatformRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;

public class WorkItemRequest<T extends WorkItem> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public WorkItemRequest(Class<T> returnType){
        super(returnType, () -> (T) new WorkItem());
        selectId();
        selectVersion();
    }

    public WorkItemRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public WorkItemRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public WorkItemRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public WorkItemRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public WorkItemRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public WorkItemRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public WorkItemRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public WorkItemRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public WorkItemRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public WorkItemRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public WorkItemRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (WorkItemRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public WorkItemRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public WorkItemRequest<T> matchingAnyOf(WorkItemRequest workItem){
        super.internalMatchAny(workItem);
        return this;
    }

    public WorkItemRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public WorkItemRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public WorkItemRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public WorkItemRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectTitle().selectDescription().selectPlatformIdOnly().selectVersion();
    }

    public WorkItemRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public WorkItemRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectTitle().selectDescription().selectPlatform().selectVersion();
    }

    public WorkItemRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectTitle().selectDescription().selectPlatform().selectVersion();
    }


    public WorkItemRequest<T> selectId(){
       selectProperty(WorkItem.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public WorkItemRequest<T> unselectId(){
       unselectProperty(WorkItem.ID_PROPERTY);
       return this;
    }
    public WorkItemRequest<T> selectTitle(){
       selectProperty(WorkItem.TITLE_PROPERTY);
       return this;
    }

    /**
     * fill the title with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  title) to fetch title property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public WorkItemRequest<T> unselectTitle(){
       unselectProperty(WorkItem.TITLE_PROPERTY);
       return this;
    }
    public WorkItemRequest<T> selectDescription(){
       selectProperty(WorkItem.DESCRIPTION_PROPERTY);
       return this;
    }

    /**
     * fill the description with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  description) to fetch description property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public WorkItemRequest<T> unselectDescription(){
       unselectProperty(WorkItem.DESCRIPTION_PROPERTY);
       return this;
    }
    public WorkItemRequest<T> selectPlatformIdOnly(){
       selectProperty(WorkItem.PLATFORM_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> selectPlatform(){
        return selectPlatformWith(Q.platforms().unlimited().selectSelf());
    }

    public WorkItemRequest<T> selectPlatformWith(PlatformRequest platform){
       selectProperty(WorkItem.PLATFORM_PROPERTY);
       enhanceRelation(WorkItem.PLATFORM_PROPERTY, platform);
       return this;
    }

    public WorkItemRequest<T> unselectPlatform(){
       unselectProperty(WorkItem.PLATFORM_PROPERTY);
       return this;
    }
    public WorkItemRequest<T> selectVersion(){
       selectProperty(WorkItem.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public WorkItemRequest<T> unselectVersion(){
       unselectProperty(WorkItem.VERSION_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(WorkItem.ID_PROPERTY, operator, values);
    }

    public WorkItemRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public WorkItemRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public WorkItemRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public WorkItemRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public WorkItemRequest<T> filterByTitle(String... title){
      if (title == null || title.length == 0) {
        throw new IllegalArgumentException("filterByTitle parameter title cannot be empty");
      }
      return appendSearchCriteria(createTitleCriteria(Operator.EQUAL, (Object[])title));
    }

    public WorkItemRequest<T> withTitle(Operator operator, Object... values){
       return appendSearchCriteria(createTitleCriteria(operator, values));
    }

    public WorkItemRequest<T> withTitleIsUnknown(){
       return withTitle(Operator.IS_NULL);
    }

    public WorkItemRequest<T> withTitleIsKnown(){
       return withTitle(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createTitleCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(WorkItem.TITLE_PROPERTY, operator, values);
    }

    public WorkItemRequest<T> withTitleIsNot(String title){
       return withTitle(Operator.NOT_EQUAL, title);
    }

    public WorkItemRequest<T> withTitleIn(String... title){
       return withTitle(Operator.IN, (Object[])title);
    }

    public WorkItemRequest<T> withTitleNotIn(String... title){
       return withTitle(Operator.NOT_IN, (Object[])title);
    }
    public WorkItemRequest<T> withTitleGreaterThan(String title){
       return withTitle(Operator.GREATER_THAN, title);
    }

    public WorkItemRequest<T> withTitleGreaterThanOrEqualTo(String title){
       return withTitle(Operator.GREATER_THAN_OR_EQUAL, title);
    }

    public WorkItemRequest<T> withTitleLessThan(String title){
       return withTitle(Operator.LESS_THAN, title);
    }

    public WorkItemRequest<T> withTitleLessThanOrEqualTo(String title){
       return withTitle(Operator.LESS_THAN_OR_EQUAL, title);
    }

    public WorkItemRequest<T> withTitleBetween(String startOfTitle, String endOfTitle){
       return withTitle(Operator.BETWEEN, startOfTitle, endOfTitle);
    }
    public WorkItemRequest<T> withTitleStartingWith(String title){
       return withTitle(Operator.BEGIN_WITH, title);
    }
    public WorkItemRequest<T> withTitleContaining(String title){
       return withTitle(Operator.CONTAIN, title);
    }

    public WorkItemRequest<T> withTitleNotContaining(String title){
       return withTitle(Operator.NOT_CONTAIN, title);
    }

    public WorkItemRequest<T> withTitleNotStartingWith(String title){
       return withTitle(Operator.NOT_BEGIN_WITH, title);
    }

    public WorkItemRequest<T> withTitleEndingWith(String title){
       return withTitle(Operator.END_WITH, title);
    }

    public WorkItemRequest<T> withTitleNotEndingWith(String title){
       return withTitle(Operator.NOT_END_WITH, title);
    }

    public WorkItemRequest<T> withTitleIs(String title){
       return withTitle(Operator.EQUAL, title);
    }

    public WorkItemRequest<T> withTitleSoundingLike(String title){
       return withTitle(Operator.SOUNDS_LIKE, title);
    }



    public WorkItemRequest<T> filterByDescription(String... description){
      if (description == null || description.length == 0) {
        throw new IllegalArgumentException("filterByDescription parameter description cannot be empty");
      }
      return appendSearchCriteria(createDescriptionCriteria(Operator.EQUAL, (Object[])description));
    }

    public WorkItemRequest<T> withDescription(Operator operator, Object... values){
       return appendSearchCriteria(createDescriptionCriteria(operator, values));
    }

    public WorkItemRequest<T> withDescriptionIsUnknown(){
       return withDescription(Operator.IS_NULL);
    }

    public WorkItemRequest<T> withDescriptionIsKnown(){
       return withDescription(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createDescriptionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(WorkItem.DESCRIPTION_PROPERTY, operator, values);
    }

    public WorkItemRequest<T> withDescriptionIsNot(String description){
       return withDescription(Operator.NOT_EQUAL, description);
    }

    public WorkItemRequest<T> withDescriptionIn(String... description){
       return withDescription(Operator.IN, (Object[])description);
    }

    public WorkItemRequest<T> withDescriptionNotIn(String... description){
       return withDescription(Operator.NOT_IN, (Object[])description);
    }
    public WorkItemRequest<T> withDescriptionGreaterThan(String description){
       return withDescription(Operator.GREATER_THAN, description);
    }

    public WorkItemRequest<T> withDescriptionGreaterThanOrEqualTo(String description){
       return withDescription(Operator.GREATER_THAN_OR_EQUAL, description);
    }

    public WorkItemRequest<T> withDescriptionLessThan(String description){
       return withDescription(Operator.LESS_THAN, description);
    }

    public WorkItemRequest<T> withDescriptionLessThanOrEqualTo(String description){
       return withDescription(Operator.LESS_THAN_OR_EQUAL, description);
    }

    public WorkItemRequest<T> withDescriptionBetween(String startOfDescription, String endOfDescription){
       return withDescription(Operator.BETWEEN, startOfDescription, endOfDescription);
    }
    public WorkItemRequest<T> withDescriptionStartingWith(String description){
       return withDescription(Operator.BEGIN_WITH, description);
    }
    public WorkItemRequest<T> withDescriptionContaining(String description){
       return withDescription(Operator.CONTAIN, description);
    }

    public WorkItemRequest<T> withDescriptionNotContaining(String description){
       return withDescription(Operator.NOT_CONTAIN, description);
    }

    public WorkItemRequest<T> withDescriptionNotStartingWith(String description){
       return withDescription(Operator.NOT_BEGIN_WITH, description);
    }

    public WorkItemRequest<T> withDescriptionEndingWith(String description){
       return withDescription(Operator.END_WITH, description);
    }

    public WorkItemRequest<T> withDescriptionNotEndingWith(String description){
       return withDescription(Operator.NOT_END_WITH, description);
    }

    public WorkItemRequest<T> withDescriptionIs(String description){
       return withDescription(Operator.EQUAL, description);
    }

    public WorkItemRequest<T> withDescriptionSoundingLike(String description){
       return withDescription(Operator.SOUNDS_LIKE, description);
    }



    public WorkItemRequest<T> filterByPlatform(Platform... platform){
      if (platform == null || platform.length == 0) {
        throw new IllegalArgumentException("filterByPlatform parameter platform cannot be empty");
      }
      return appendSearchCriteria(createPlatformCriteria(Operator.EQUAL, (Object[])platform));
    }

    public WorkItemRequest<T> withPlatform(Operator operator, Object... values){
       return appendSearchCriteria(createPlatformCriteria(operator, values));
    }

    public WorkItemRequest<T> withPlatformIsUnknown(){
       return withPlatform(Operator.IS_NULL);
    }

    public WorkItemRequest<T> withPlatformIsKnown(){
       return withPlatform(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createPlatformCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(WorkItem.PLATFORM_PROPERTY, operator, values);
    }

    public WorkItemRequest<T> filterByPlatform(Long platform){
      if(platform == null){
         return this;
      }
      return withPlatform(Operator.EQUAL, platform);
    }
    public WorkItemRequest<T> withPlatformMatching(PlatformRequest platform){
       return appendSearchCriteria(new SubQuerySearchCriteria(WorkItem.PLATFORM_PROPERTY, platform, Platform.ID_PROPERTY));
    }

    public WorkItemRequest<T> withoutPlatformMatching(PlatformRequest platform){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(WorkItem.PLATFORM_PROPERTY, platform, Platform.ID_PROPERTY)));
    }

    public WorkItemRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public WorkItemRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public WorkItemRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public WorkItemRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(WorkItem.VERSION_PROPERTY, operator, values);
    }

    public WorkItemRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public WorkItemRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public WorkItemRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public WorkItemRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public WorkItemRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public WorkItemRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public WorkItemRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public WorkItemRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public WorkItemRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public WorkItemRequest<T> count(){
        super.count();
        return this;
    }
    public WorkItemRequest<T> countAs(String retName){
        super.count(retName);
        return this;
    }
    public WorkItemRequest<T> groupByPlatformWithDetails(){
       return groupByPlatformWithDetails(Q.platforms().unlimited());
    }

    public WorkItemRequest<T> groupByPlatformWithDetails(PlatformRequest subRequest){
       aggregate(WorkItem.PLATFORM_PROPERTY, subRequest);
       return this;
    }



    public WorkItemRequest<T> groupById(){
       groupBy(WorkItem.ID_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> groupByIdAs(String retName){
       groupBy(retName, WorkItem.ID_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, WorkItem.ID_PROPERTY, function);
       return this;
    }

    public WorkItemRequest<T> groupByTitle(){
       groupBy(WorkItem.TITLE_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> groupByTitleAs(String retName){
       groupBy(retName, WorkItem.TITLE_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> groupByTitleWithFunction(String retName, AggrFunction function){
       groupBy(retName, WorkItem.TITLE_PROPERTY, function);
       return this;
    }

    public WorkItemRequest<T> groupByDescription(){
       groupBy(WorkItem.DESCRIPTION_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> groupByDescriptionAs(String retName){
       groupBy(retName, WorkItem.DESCRIPTION_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> groupByDescriptionWithFunction(String retName, AggrFunction function){
       groupBy(retName, WorkItem.DESCRIPTION_PROPERTY, function);
       return this;
    }
    public WorkItemRequest<T> groupByPlatformWith(PlatformRequest subRequest){
       groupBy(WorkItem.PLATFORM_PROPERTY, subRequest);
       return this;
    }
    public WorkItemRequest<T> groupByPlatform(){
       groupBy(WorkItem.PLATFORM_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> groupByPlatformAs(String retName){
       groupBy(retName, WorkItem.PLATFORM_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> groupByPlatformWithFunction(String retName, AggrFunction function){
       groupBy(retName, WorkItem.PLATFORM_PROPERTY, function);
       return this;
    }

    public WorkItemRequest<T> groupByVersion(){
       groupBy(WorkItem.VERSION_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> groupByVersionAs(String retName){
       groupBy(retName, WorkItem.VERSION_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, WorkItem.VERSION_PROPERTY, function);
       return this;
    }



    public WorkItemRequest<T> orderByIdAscending(){
       addOrderByAscending(WorkItem.ID_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> orderByIdDescending(){
       addOrderByDescending(WorkItem.ID_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> orderByTitleAscending(){
       addOrderByAscending(WorkItem.TITLE_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> orderByTitleDescending(){
       addOrderByDescending(WorkItem.TITLE_PROPERTY);
       return this;
    }
    public WorkItemRequest<T> orderByTitleAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(WorkItem.TITLE_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> orderByTitleDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(WorkItem.TITLE_PROPERTY);
       return this;
    }
    public WorkItemRequest<T> orderByDescriptionAscending(){
       addOrderByAscending(WorkItem.DESCRIPTION_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> orderByDescriptionDescending(){
       addOrderByDescending(WorkItem.DESCRIPTION_PROPERTY);
       return this;
    }
    public WorkItemRequest<T> orderByDescriptionAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(WorkItem.DESCRIPTION_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> orderByDescriptionDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(WorkItem.DESCRIPTION_PROPERTY);
       return this;
    }
    public WorkItemRequest<T> orderByPlatformAscending(){
       addOrderByAscending(WorkItem.PLATFORM_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> orderByPlatformDescending(){
       addOrderByDescending(WorkItem.PLATFORM_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> orderByVersionAscending(){
       addOrderByAscending(WorkItem.VERSION_PROPERTY);
       return this;
    }

    public WorkItemRequest<T> orderByVersionDescending(){
       addOrderByDescending(WorkItem.VERSION_PROPERTY);
       return this;
    }


    public PlatformRequest rollUpToPlatform(){
       PlatformRequest platform = Q.platforms().unlimited();
       this.withPlatformMatching(platform)
           .groupByPlatformWith(platform);
       return platform;
    }



   public WorkItemRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform){
       return facetByPlatformAs(facetName, platform, true);
   }

   public WorkItemRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform, boolean includeAllFacets){
       addFacet(facetName, WorkItem.PLATFORM_PROPERTY, platform, includeAllFacets);
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public WorkItemRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public WorkItemRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public WorkItemRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public WorkItemRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public WorkItemRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public WorkItemRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}