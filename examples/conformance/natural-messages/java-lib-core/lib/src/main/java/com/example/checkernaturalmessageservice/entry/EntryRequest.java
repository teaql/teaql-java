
package com.example.checkernaturalmessageservice.entry;

import com.example.checkernaturalmessageservice.Q;
import com.example.checkernaturalmessageservice.catalog.Catalog;
import com.example.checkernaturalmessageservice.catalog.CatalogRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;

public class EntryRequest<T extends Entry> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public EntryRequest(Class<T> returnType){
        this(returnType, () -> (T) new Entry());
    }

    protected EntryRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public EntryRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public EntryRequest<T> copy(){
        return (EntryRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new EntryRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public EntryRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public EntryRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public EntryRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public EntryRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public EntryRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public EntryRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public EntryRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public EntryRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public EntryRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public EntryRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (EntryRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public EntryRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public EntryRequest<T> matchingAnyOf(EntryRequest entry){
        super.internalMatchAny(entry);
        return this;
    }

    public EntryRequest<T> matchingIdsOf(EntryRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                Entry.ID_PROPERTY, request, Entry.ID_PROPERTY));
    }

    public EntryRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public EntryRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public EntryRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public EntryRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectCatalogIdOnly().selectName().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public EntryRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public EntryRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(Entry.ID_PROPERTY);
        selectProperty(Entry.CATALOG_PROPERTY);
        selectProperty(Entry.NAME_PROPERTY);
        selectProperty(Entry.CREATE_TIME_PROPERTY);
        selectProperty(Entry.UPDATE_TIME_PROPERTY);
        selectProperty(Entry.VERSION_PROPERTY);
        return this;
    }

    public EntryRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectCatalog().selectName().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public EntryRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectCatalog().selectName().selectCreateTime().selectUpdateTime().selectVersion();
    }


    public EntryRequest<T> selectId(){
       selectProperty(Entry.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public EntryRequest<T> unselectId(){
       unselectProperty(Entry.ID_PROPERTY);
       return this;
    }
    public EntryRequest<T> selectCatalogIdOnly(){
       selectProperty(Entry.CATALOG_PROPERTY);
       return this;
    }

    public EntryRequest<T> selectCatalog(){
        return selectCatalogWith(Q.catalogs().unlimited().selectSelf());
    }

    public EntryRequest<T> selectCatalogWith(CatalogRequest catalog){
       selectProperty(Entry.CATALOG_PROPERTY);
       enhanceRelation(Entry.CATALOG_PROPERTY, catalog,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectCatalogWith", null, Entry.CATALOG_PROPERTY));
       return this;
    }

    public EntryRequest<T> unselectCatalog(){
       unselectProperty(Entry.CATALOG_PROPERTY);
       return this;
    }
    public EntryRequest<T> selectName(){
       selectProperty(Entry.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public EntryRequest<T> unselectName(){
       unselectProperty(Entry.NAME_PROPERTY);
       return this;
    }
    public EntryRequest<T> selectCreateTime(){
       selectProperty(Entry.CREATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the createTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  createTime) to fetch createTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public EntryRequest<T> unselectCreateTime(){
       unselectProperty(Entry.CREATE_TIME_PROPERTY);
       return this;
    }
    public EntryRequest<T> selectUpdateTime(){
       selectProperty(Entry.UPDATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the updateTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  updateTime) to fetch updateTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public EntryRequest<T> unselectUpdateTime(){
       unselectProperty(Entry.UPDATE_TIME_PROPERTY);
       return this;
    }
    public EntryRequest<T> selectVersion(){
       selectProperty(Entry.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public EntryRequest<T> unselectVersion(){
       unselectProperty(Entry.VERSION_PROPERTY);
       return this;
    }

    public EntryRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Entry.ID_PROPERTY, operator, values);
    }

    public EntryRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public EntryRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public EntryRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public EntryRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public EntryRequest<T> filterByCatalog(Catalog... catalog){
      if (catalog == null || catalog.length == 0) {
        throw new IllegalArgumentException("filterByCatalog parameter catalog cannot be empty");
      }
      return appendSearchCriteria(createCatalogCriteria(Operator.EQUAL, (Object[])catalog));
    }

    public EntryRequest<T> withCatalog(Operator operator, Object... values){
       return appendSearchCriteria(createCatalogCriteria(operator, values));
    }

    public EntryRequest<T> withCatalogIsUnknown(){
       return withCatalog(Operator.IS_NULL);
    }

    public EntryRequest<T> withCatalogIsKnown(){
       return withCatalog(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCatalogCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Entry.CATALOG_PROPERTY, operator, values);
    }

    public EntryRequest<T> filterByCatalog(Long catalog){
      if(catalog == null){
         return this;
      }
      return withCatalog(Operator.EQUAL, catalog);
    }
    public EntryRequest<T> withCatalogMatching(CatalogRequest catalog){
       return appendSearchCriteria(new SubQuerySearchCriteria(Entry.CATALOG_PROPERTY, catalog, Catalog.ID_PROPERTY));
    }

    public EntryRequest<T> withoutCatalogMatching(CatalogRequest catalog){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(Entry.CATALOG_PROPERTY, catalog, Catalog.ID_PROPERTY)));
    }

    public EntryRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public EntryRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public EntryRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public EntryRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Entry.NAME_PROPERTY, operator, values);
    }

    public EntryRequest<T> withNameIsNot(String name){
       return withName(Operator.NOT_EQUAL, name);
    }

    public EntryRequest<T> withNameIn(String... name){
       return withName(Operator.IN, (Object[])name);
    }

    public EntryRequest<T> withNameNotIn(String... name){
       return withName(Operator.NOT_IN, (Object[])name);
    }
    public EntryRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public EntryRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public EntryRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public EntryRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public EntryRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public EntryRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public EntryRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public EntryRequest<T> withNameNotContaining(String name){
       return withName(Operator.NOT_CONTAIN, name);
    }

    public EntryRequest<T> withNameNotStartingWith(String name){
       return withName(Operator.NOT_BEGIN_WITH, name);
    }

    public EntryRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public EntryRequest<T> withNameNotEndingWith(String name){
       return withName(Operator.NOT_END_WITH, name);
    }

    public EntryRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public EntryRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public EntryRequest<T> filterByCreateTime(LocalDateTime... createTime){
      if (createTime == null || createTime.length == 0) {
        throw new IllegalArgumentException("filterByCreateTime parameter createTime cannot be empty");
      }
      return appendSearchCriteria(createCreateTimeCriteria(Operator.EQUAL, (Object[])createTime));
    }

    public EntryRequest<T> withCreateTime(Operator operator, Object... values){
       return appendSearchCriteria(createCreateTimeCriteria(operator, values));
    }

    public EntryRequest<T> withCreateTimeIsUnknown(){
       return withCreateTime(Operator.IS_NULL);
    }

    public EntryRequest<T> withCreateTimeIsKnown(){
       return withCreateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCreateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Entry.CREATE_TIME_PROPERTY, operator, values);
    }

    public EntryRequest<T> withCreateTimeIs(LocalDateTime createTime){
       return withCreateTime(Operator.EQUAL, createTime);
    }

    public EntryRequest<T> withCreateTimeIsNot(LocalDateTime createTime){
       return withCreateTime(Operator.NOT_EQUAL, createTime);
    }

    public EntryRequest<T> withCreateTimeIn(LocalDateTime... createTime){
       return withCreateTime(Operator.IN, (Object[])createTime);
    }

    public EntryRequest<T> withCreateTimeNotIn(LocalDateTime... createTime){
       return withCreateTime(Operator.NOT_IN, (Object[])createTime);
    }
    public EntryRequest<T> withCreateTimeGreaterThan(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public EntryRequest<T> withCreateTimeGreaterThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN_OR_EQUAL, createTime);
    }

    public EntryRequest<T> withCreateTimeLessThan(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public EntryRequest<T> withCreateTimeLessThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN_OR_EQUAL, createTime);
    }

    public EntryRequest<T> withCreateTimeBetween(LocalDateTime startOfCreateTime, LocalDateTime endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }
    public EntryRequest<T> withCreateTimeBefore(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public EntryRequest<T> withCreateTimeBefore(Date createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public EntryRequest<T> withCreateTimeAfter(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public EntryRequest<T> withCreateTimeAfter(Date createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public EntryRequest<T> withCreateTimeBetween(Date startOfCreateTime, Date endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }




    public EntryRequest<T> filterByUpdateTime(LocalDateTime... updateTime){
      if (updateTime == null || updateTime.length == 0) {
        throw new IllegalArgumentException("filterByUpdateTime parameter updateTime cannot be empty");
      }
      return appendSearchCriteria(createUpdateTimeCriteria(Operator.EQUAL, (Object[])updateTime));
    }

    public EntryRequest<T> withUpdateTime(Operator operator, Object... values){
       return appendSearchCriteria(createUpdateTimeCriteria(operator, values));
    }

    public EntryRequest<T> withUpdateTimeIsUnknown(){
       return withUpdateTime(Operator.IS_NULL);
    }

    public EntryRequest<T> withUpdateTimeIsKnown(){
       return withUpdateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createUpdateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Entry.UPDATE_TIME_PROPERTY, operator, values);
    }

    public EntryRequest<T> withUpdateTimeIs(LocalDateTime updateTime){
       return withUpdateTime(Operator.EQUAL, updateTime);
    }

    public EntryRequest<T> withUpdateTimeIsNot(LocalDateTime updateTime){
       return withUpdateTime(Operator.NOT_EQUAL, updateTime);
    }

    public EntryRequest<T> withUpdateTimeIn(LocalDateTime... updateTime){
       return withUpdateTime(Operator.IN, (Object[])updateTime);
    }

    public EntryRequest<T> withUpdateTimeNotIn(LocalDateTime... updateTime){
       return withUpdateTime(Operator.NOT_IN, (Object[])updateTime);
    }
    public EntryRequest<T> withUpdateTimeGreaterThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public EntryRequest<T> withUpdateTimeGreaterThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN_OR_EQUAL, updateTime);
    }

    public EntryRequest<T> withUpdateTimeLessThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public EntryRequest<T> withUpdateTimeLessThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN_OR_EQUAL, updateTime);
    }

    public EntryRequest<T> withUpdateTimeBetween(LocalDateTime startOfUpdateTime, LocalDateTime endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
    }
    public EntryRequest<T> withUpdateTimeBefore(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public EntryRequest<T> withUpdateTimeBefore(Date updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public EntryRequest<T> withUpdateTimeAfter(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public EntryRequest<T> withUpdateTimeAfter(Date updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public EntryRequest<T> withUpdateTimeBetween(Date startOfUpdateTime, Date endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
    }




    public EntryRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public EntryRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public EntryRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public EntryRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Entry.VERSION_PROPERTY, operator, values);
    }

    public EntryRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public EntryRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public EntryRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public EntryRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public EntryRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public EntryRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public EntryRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public EntryRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public EntryRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }



    public EntryRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public EntryRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public EntryRequest<T> groupByCatalogWithDetails(){
       return groupByCatalogWithDetails(Q.catalogs().unlimited());
    }

    public EntryRequest<T> groupByCatalogWithDetails(CatalogRequest subRequest){
       aggregate(Entry.CATALOG_PROPERTY, subRequest);
       return this;
    }





    public EntryRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public EntryRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, Entry.ID_PROPERTY);
        return this;
    }
    public EntryRequest<T> countDistinctCatalog(){
        return countDistinctCatalogAs("catalog_distinct_count");
    }

    public EntryRequest<T> countDistinctCatalogAs(String alias){
        super.countDistinctProperty(alias, Entry.CATALOG_PROPERTY);
        return this;
    }
    public EntryRequest<T> countDistinctName(){
        return countDistinctNameAs("name_distinct_count");
    }

    public EntryRequest<T> countDistinctNameAs(String alias){
        super.countDistinctProperty(alias, Entry.NAME_PROPERTY);
        return this;
    }
    public EntryRequest<T> countDistinctCreateTime(){
        return countDistinctCreateTimeAs("create_time_distinct_count");
    }

    public EntryRequest<T> countDistinctCreateTimeAs(String alias){
        super.countDistinctProperty(alias, Entry.CREATE_TIME_PROPERTY);
        return this;
    }
    public EntryRequest<T> countDistinctUpdateTime(){
        return countDistinctUpdateTimeAs("update_time_distinct_count");
    }

    public EntryRequest<T> countDistinctUpdateTimeAs(String alias){
        super.countDistinctProperty(alias, Entry.UPDATE_TIME_PROPERTY);
        return this;
    }
    public EntryRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public EntryRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, Entry.VERSION_PROPERTY);
        return this;
    }

    public EntryRequest<T> groupById(){
       groupBy(Entry.ID_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByIdAs(String retName){
       groupBy(retName, Entry.ID_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, Entry.ID_PROPERTY, function);
       return this;
    }
    public EntryRequest<T> groupByCatalogWith(CatalogRequest subRequest){
       groupBy(Entry.CATALOG_PROPERTY, subRequest,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "groupByCatalogWith", null, "catalog"));
       return this;
    }
    public EntryRequest<T> groupByCatalog(){
       groupBy(Entry.CATALOG_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByCatalogAs(String retName){
       groupBy(retName, Entry.CATALOG_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByCatalogWithFunction(String retName, AggrFunction function){
       groupBy(retName, Entry.CATALOG_PROPERTY, function);
       return this;
    }

    public EntryRequest<T> groupByName(){
       groupBy(Entry.NAME_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByNameAs(String retName){
       groupBy(retName, Entry.NAME_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, Entry.NAME_PROPERTY, function);
       return this;
    }

    public EntryRequest<T> groupByCreateTime(){
       groupBy(Entry.CREATE_TIME_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByCreateTimeAs(String retName){
       groupBy(retName, Entry.CREATE_TIME_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByCreateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, Entry.CREATE_TIME_PROPERTY, function);
       return this;
    }

    public EntryRequest<T> groupByUpdateTime(){
       groupBy(Entry.UPDATE_TIME_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByUpdateTimeAs(String retName){
       groupBy(retName, Entry.UPDATE_TIME_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByUpdateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, Entry.UPDATE_TIME_PROPERTY, function);
       return this;
    }

    public EntryRequest<T> groupByVersion(){
       groupBy(Entry.VERSION_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByVersionAs(String retName){
       groupBy(retName, Entry.VERSION_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, Entry.VERSION_PROPERTY, function);
       return this;
    }



    public EntryRequest<T> orderByIdAscending(){
       addOrderByAscending(Entry.ID_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByIdDescending(){
       addOrderByDescending(Entry.ID_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByCatalogAscending(){
       addOrderByAscending(Entry.CATALOG_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByCatalogDescending(){
       addOrderByDescending(Entry.CATALOG_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByNameAscending(){
       addOrderByAscending(Entry.NAME_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByNameDescending(){
       addOrderByDescending(Entry.NAME_PROPERTY);
       return this;
    }
    public EntryRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Entry.NAME_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Entry.NAME_PROPERTY);
       return this;
    }
    public EntryRequest<T> orderByCreateTimeAscending(){
       addOrderByAscending(Entry.CREATE_TIME_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByCreateTimeDescending(){
       addOrderByDescending(Entry.CREATE_TIME_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByUpdateTimeAscending(){
       addOrderByAscending(Entry.UPDATE_TIME_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByUpdateTimeDescending(){
       addOrderByDescending(Entry.UPDATE_TIME_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByVersionAscending(){
       addOrderByAscending(Entry.VERSION_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByVersionDescending(){
       addOrderByDescending(Entry.VERSION_PROPERTY);
       return this;
    }


    public CatalogRequest rollUpToCatalog(){
       CatalogRequest catalog = Q.catalogs().unlimited();
       this.internalRollUpTo("catalog", catalog,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "rollUpToCatalog", null, "catalog"));
       return catalog;
    }






   public EntryRequest<T> facetByCatalogAs(String facetName, CatalogRequest catalog){
       return facetByCatalogAs(facetName, catalog, true);
   }

   public EntryRequest<T> facetByCatalogAs(String facetName, CatalogRequest catalog, boolean includeAllFacets){
       addFacet(facetName, Entry.CATALOG_PROPERTY, catalog, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByCatalogAs", facetName, Entry.CATALOG_PROPERTY));
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public EntryRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public EntryRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public EntryRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public EntryRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public EntryRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public EntryRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}