
package com.example.contextfixservice.entry;

import com.example.contextfixservice.Q;
import com.example.contextfixservice.catalog.Catalog;
import com.example.contextfixservice.catalog.CatalogRequest;
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
        return selectId().selectCatalogIdOnly().selectName().selectBornAt().selectTouchedAt().selectSourceIp().selectLastIp().selectOpenedBy().selectChangedBy().selectGuardedToken().selectVersion();
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
        selectProperty(Entry.BORN_AT_PROPERTY);
        selectProperty(Entry.TOUCHED_AT_PROPERTY);
        selectProperty(Entry.SOURCE_IP_PROPERTY);
        selectProperty(Entry.LAST_IP_PROPERTY);
        selectProperty(Entry.OPENED_BY_PROPERTY);
        selectProperty(Entry.CHANGED_BY_PROPERTY);
        selectProperty(Entry.GUARDED_TOKEN_PROPERTY);
        selectProperty(Entry.VERSION_PROPERTY);
        return this;
    }

    public EntryRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectCatalog().selectName().selectBornAt().selectTouchedAt().selectSourceIp().selectLastIp().selectOpenedBy().selectChangedBy().selectGuardedToken().selectVersion();
    }

    public EntryRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectCatalog().selectName().selectBornAt().selectTouchedAt().selectSourceIp().selectLastIp().selectOpenedBy().selectChangedBy().selectGuardedToken().selectVersion();
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
    public EntryRequest<T> selectBornAt(){
       selectProperty(Entry.BORN_AT_PROPERTY);
       return this;
    }

    /**
     * fill the bornAt with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  bornAt) to fetch bornAt property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public EntryRequest<T> unselectBornAt(){
       unselectProperty(Entry.BORN_AT_PROPERTY);
       return this;
    }
    public EntryRequest<T> selectTouchedAt(){
       selectProperty(Entry.TOUCHED_AT_PROPERTY);
       return this;
    }

    /**
     * fill the touchedAt with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  touchedAt) to fetch touchedAt property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public EntryRequest<T> unselectTouchedAt(){
       unselectProperty(Entry.TOUCHED_AT_PROPERTY);
       return this;
    }
    public EntryRequest<T> selectSourceIp(){
       selectProperty(Entry.SOURCE_IP_PROPERTY);
       return this;
    }

    /**
     * fill the sourceIp with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  sourceIp) to fetch sourceIp property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public EntryRequest<T> unselectSourceIp(){
       unselectProperty(Entry.SOURCE_IP_PROPERTY);
       return this;
    }
    public EntryRequest<T> selectLastIp(){
       selectProperty(Entry.LAST_IP_PROPERTY);
       return this;
    }

    /**
     * fill the lastIp with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  lastIp) to fetch lastIp property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public EntryRequest<T> unselectLastIp(){
       unselectProperty(Entry.LAST_IP_PROPERTY);
       return this;
    }
    public EntryRequest<T> selectOpenedBy(){
       selectProperty(Entry.OPENED_BY_PROPERTY);
       return this;
    }

    /**
     * fill the openedBy with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  openedBy) to fetch openedBy property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public EntryRequest<T> unselectOpenedBy(){
       unselectProperty(Entry.OPENED_BY_PROPERTY);
       return this;
    }
    public EntryRequest<T> selectChangedBy(){
       selectProperty(Entry.CHANGED_BY_PROPERTY);
       return this;
    }

    /**
     * fill the changedBy with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  changedBy) to fetch changedBy property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public EntryRequest<T> unselectChangedBy(){
       unselectProperty(Entry.CHANGED_BY_PROPERTY);
       return this;
    }
    public EntryRequest<T> selectGuardedToken(){
       selectProperty(Entry.GUARDED_TOKEN_PROPERTY);
       return this;
    }

    /**
     * fill the guardedToken with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  guardedToken) to fetch guardedToken property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public EntryRequest<T> unselectGuardedToken(){
       unselectProperty(Entry.GUARDED_TOKEN_PROPERTY);
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



    public EntryRequest<T> filterByBornAt(LocalDateTime... bornAt){
      if (bornAt == null || bornAt.length == 0) {
        throw new IllegalArgumentException("filterByBornAt parameter bornAt cannot be empty");
      }
      return appendSearchCriteria(createBornAtCriteria(Operator.EQUAL, (Object[])bornAt));
    }

    public EntryRequest<T> withBornAt(Operator operator, Object... values){
       return appendSearchCriteria(createBornAtCriteria(operator, values));
    }

    public EntryRequest<T> withBornAtIsUnknown(){
       return withBornAt(Operator.IS_NULL);
    }

    public EntryRequest<T> withBornAtIsKnown(){
       return withBornAt(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createBornAtCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Entry.BORN_AT_PROPERTY, operator, values);
    }

    public EntryRequest<T> withBornAtIs(LocalDateTime bornAt){
       return withBornAt(Operator.EQUAL, bornAt);
    }

    public EntryRequest<T> withBornAtIsNot(LocalDateTime bornAt){
       return withBornAt(Operator.NOT_EQUAL, bornAt);
    }

    public EntryRequest<T> withBornAtIn(LocalDateTime... bornAt){
       return withBornAt(Operator.IN, (Object[])bornAt);
    }

    public EntryRequest<T> withBornAtNotIn(LocalDateTime... bornAt){
       return withBornAt(Operator.NOT_IN, (Object[])bornAt);
    }
    public EntryRequest<T> withBornAtGreaterThan(LocalDateTime bornAt){
       return withBornAt(Operator.GREATER_THAN, bornAt);
    }

    public EntryRequest<T> withBornAtGreaterThanOrEqualTo(LocalDateTime bornAt){
       return withBornAt(Operator.GREATER_THAN_OR_EQUAL, bornAt);
    }

    public EntryRequest<T> withBornAtLessThan(LocalDateTime bornAt){
       return withBornAt(Operator.LESS_THAN, bornAt);
    }

    public EntryRequest<T> withBornAtLessThanOrEqualTo(LocalDateTime bornAt){
       return withBornAt(Operator.LESS_THAN_OR_EQUAL, bornAt);
    }

    public EntryRequest<T> withBornAtBetween(LocalDateTime startOfBornAt, LocalDateTime endOfBornAt){
       return withBornAt(Operator.BETWEEN, startOfBornAt, endOfBornAt);
    }
    public EntryRequest<T> withBornAtBefore(LocalDateTime bornAt){
       return withBornAt(Operator.LESS_THAN, bornAt);
    }

    public EntryRequest<T> withBornAtBefore(Date bornAt){
       return withBornAt(Operator.LESS_THAN, bornAt);
    }

    public EntryRequest<T> withBornAtAfter(LocalDateTime bornAt){
       return withBornAt(Operator.GREATER_THAN, bornAt);
    }

    public EntryRequest<T> withBornAtAfter(Date bornAt){
       return withBornAt(Operator.GREATER_THAN, bornAt);
    }

    public EntryRequest<T> withBornAtBetween(Date startOfBornAt, Date endOfBornAt){
       return withBornAt(Operator.BETWEEN, startOfBornAt, endOfBornAt);
    }




    public EntryRequest<T> filterByTouchedAt(LocalDateTime... touchedAt){
      if (touchedAt == null || touchedAt.length == 0) {
        throw new IllegalArgumentException("filterByTouchedAt parameter touchedAt cannot be empty");
      }
      return appendSearchCriteria(createTouchedAtCriteria(Operator.EQUAL, (Object[])touchedAt));
    }

    public EntryRequest<T> withTouchedAt(Operator operator, Object... values){
       return appendSearchCriteria(createTouchedAtCriteria(operator, values));
    }

    public EntryRequest<T> withTouchedAtIsUnknown(){
       return withTouchedAt(Operator.IS_NULL);
    }

    public EntryRequest<T> withTouchedAtIsKnown(){
       return withTouchedAt(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createTouchedAtCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Entry.TOUCHED_AT_PROPERTY, operator, values);
    }

    public EntryRequest<T> withTouchedAtIs(LocalDateTime touchedAt){
       return withTouchedAt(Operator.EQUAL, touchedAt);
    }

    public EntryRequest<T> withTouchedAtIsNot(LocalDateTime touchedAt){
       return withTouchedAt(Operator.NOT_EQUAL, touchedAt);
    }

    public EntryRequest<T> withTouchedAtIn(LocalDateTime... touchedAt){
       return withTouchedAt(Operator.IN, (Object[])touchedAt);
    }

    public EntryRequest<T> withTouchedAtNotIn(LocalDateTime... touchedAt){
       return withTouchedAt(Operator.NOT_IN, (Object[])touchedAt);
    }
    public EntryRequest<T> withTouchedAtGreaterThan(LocalDateTime touchedAt){
       return withTouchedAt(Operator.GREATER_THAN, touchedAt);
    }

    public EntryRequest<T> withTouchedAtGreaterThanOrEqualTo(LocalDateTime touchedAt){
       return withTouchedAt(Operator.GREATER_THAN_OR_EQUAL, touchedAt);
    }

    public EntryRequest<T> withTouchedAtLessThan(LocalDateTime touchedAt){
       return withTouchedAt(Operator.LESS_THAN, touchedAt);
    }

    public EntryRequest<T> withTouchedAtLessThanOrEqualTo(LocalDateTime touchedAt){
       return withTouchedAt(Operator.LESS_THAN_OR_EQUAL, touchedAt);
    }

    public EntryRequest<T> withTouchedAtBetween(LocalDateTime startOfTouchedAt, LocalDateTime endOfTouchedAt){
       return withTouchedAt(Operator.BETWEEN, startOfTouchedAt, endOfTouchedAt);
    }
    public EntryRequest<T> withTouchedAtBefore(LocalDateTime touchedAt){
       return withTouchedAt(Operator.LESS_THAN, touchedAt);
    }

    public EntryRequest<T> withTouchedAtBefore(Date touchedAt){
       return withTouchedAt(Operator.LESS_THAN, touchedAt);
    }

    public EntryRequest<T> withTouchedAtAfter(LocalDateTime touchedAt){
       return withTouchedAt(Operator.GREATER_THAN, touchedAt);
    }

    public EntryRequest<T> withTouchedAtAfter(Date touchedAt){
       return withTouchedAt(Operator.GREATER_THAN, touchedAt);
    }

    public EntryRequest<T> withTouchedAtBetween(Date startOfTouchedAt, Date endOfTouchedAt){
       return withTouchedAt(Operator.BETWEEN, startOfTouchedAt, endOfTouchedAt);
    }




    public EntryRequest<T> filterBySourceIp(String... sourceIp){
      if (sourceIp == null || sourceIp.length == 0) {
        throw new IllegalArgumentException("filterBySourceIp parameter sourceIp cannot be empty");
      }
      return appendSearchCriteria(createSourceIpCriteria(Operator.EQUAL, (Object[])sourceIp));
    }

    public EntryRequest<T> withSourceIp(Operator operator, Object... values){
       return appendSearchCriteria(createSourceIpCriteria(operator, values));
    }

    public EntryRequest<T> withSourceIpIsUnknown(){
       return withSourceIp(Operator.IS_NULL);
    }

    public EntryRequest<T> withSourceIpIsKnown(){
       return withSourceIp(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createSourceIpCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Entry.SOURCE_IP_PROPERTY, operator, values);
    }

    public EntryRequest<T> withSourceIpIsNot(String sourceIp){
       return withSourceIp(Operator.NOT_EQUAL, sourceIp);
    }

    public EntryRequest<T> withSourceIpIn(String... sourceIp){
       return withSourceIp(Operator.IN, (Object[])sourceIp);
    }

    public EntryRequest<T> withSourceIpNotIn(String... sourceIp){
       return withSourceIp(Operator.NOT_IN, (Object[])sourceIp);
    }
    public EntryRequest<T> withSourceIpGreaterThan(String sourceIp){
       return withSourceIp(Operator.GREATER_THAN, sourceIp);
    }

    public EntryRequest<T> withSourceIpGreaterThanOrEqualTo(String sourceIp){
       return withSourceIp(Operator.GREATER_THAN_OR_EQUAL, sourceIp);
    }

    public EntryRequest<T> withSourceIpLessThan(String sourceIp){
       return withSourceIp(Operator.LESS_THAN, sourceIp);
    }

    public EntryRequest<T> withSourceIpLessThanOrEqualTo(String sourceIp){
       return withSourceIp(Operator.LESS_THAN_OR_EQUAL, sourceIp);
    }

    public EntryRequest<T> withSourceIpBetween(String startOfSourceIp, String endOfSourceIp){
       return withSourceIp(Operator.BETWEEN, startOfSourceIp, endOfSourceIp);
    }
    public EntryRequest<T> withSourceIpStartingWith(String sourceIp){
       return withSourceIp(Operator.BEGIN_WITH, sourceIp);
    }
    public EntryRequest<T> withSourceIpContaining(String sourceIp){
       return withSourceIp(Operator.CONTAIN, sourceIp);
    }

    public EntryRequest<T> withSourceIpNotContaining(String sourceIp){
       return withSourceIp(Operator.NOT_CONTAIN, sourceIp);
    }

    public EntryRequest<T> withSourceIpNotStartingWith(String sourceIp){
       return withSourceIp(Operator.NOT_BEGIN_WITH, sourceIp);
    }

    public EntryRequest<T> withSourceIpEndingWith(String sourceIp){
       return withSourceIp(Operator.END_WITH, sourceIp);
    }

    public EntryRequest<T> withSourceIpNotEndingWith(String sourceIp){
       return withSourceIp(Operator.NOT_END_WITH, sourceIp);
    }

    public EntryRequest<T> withSourceIpIs(String sourceIp){
       return withSourceIp(Operator.EQUAL, sourceIp);
    }

    public EntryRequest<T> withSourceIpSoundingLike(String sourceIp){
       return withSourceIp(Operator.SOUNDS_LIKE, sourceIp);
    }



    public EntryRequest<T> filterByLastIp(String... lastIp){
      if (lastIp == null || lastIp.length == 0) {
        throw new IllegalArgumentException("filterByLastIp parameter lastIp cannot be empty");
      }
      return appendSearchCriteria(createLastIpCriteria(Operator.EQUAL, (Object[])lastIp));
    }

    public EntryRequest<T> withLastIp(Operator operator, Object... values){
       return appendSearchCriteria(createLastIpCriteria(operator, values));
    }

    public EntryRequest<T> withLastIpIsUnknown(){
       return withLastIp(Operator.IS_NULL);
    }

    public EntryRequest<T> withLastIpIsKnown(){
       return withLastIp(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createLastIpCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Entry.LAST_IP_PROPERTY, operator, values);
    }

    public EntryRequest<T> withLastIpIsNot(String lastIp){
       return withLastIp(Operator.NOT_EQUAL, lastIp);
    }

    public EntryRequest<T> withLastIpIn(String... lastIp){
       return withLastIp(Operator.IN, (Object[])lastIp);
    }

    public EntryRequest<T> withLastIpNotIn(String... lastIp){
       return withLastIp(Operator.NOT_IN, (Object[])lastIp);
    }
    public EntryRequest<T> withLastIpGreaterThan(String lastIp){
       return withLastIp(Operator.GREATER_THAN, lastIp);
    }

    public EntryRequest<T> withLastIpGreaterThanOrEqualTo(String lastIp){
       return withLastIp(Operator.GREATER_THAN_OR_EQUAL, lastIp);
    }

    public EntryRequest<T> withLastIpLessThan(String lastIp){
       return withLastIp(Operator.LESS_THAN, lastIp);
    }

    public EntryRequest<T> withLastIpLessThanOrEqualTo(String lastIp){
       return withLastIp(Operator.LESS_THAN_OR_EQUAL, lastIp);
    }

    public EntryRequest<T> withLastIpBetween(String startOfLastIp, String endOfLastIp){
       return withLastIp(Operator.BETWEEN, startOfLastIp, endOfLastIp);
    }
    public EntryRequest<T> withLastIpStartingWith(String lastIp){
       return withLastIp(Operator.BEGIN_WITH, lastIp);
    }
    public EntryRequest<T> withLastIpContaining(String lastIp){
       return withLastIp(Operator.CONTAIN, lastIp);
    }

    public EntryRequest<T> withLastIpNotContaining(String lastIp){
       return withLastIp(Operator.NOT_CONTAIN, lastIp);
    }

    public EntryRequest<T> withLastIpNotStartingWith(String lastIp){
       return withLastIp(Operator.NOT_BEGIN_WITH, lastIp);
    }

    public EntryRequest<T> withLastIpEndingWith(String lastIp){
       return withLastIp(Operator.END_WITH, lastIp);
    }

    public EntryRequest<T> withLastIpNotEndingWith(String lastIp){
       return withLastIp(Operator.NOT_END_WITH, lastIp);
    }

    public EntryRequest<T> withLastIpIs(String lastIp){
       return withLastIp(Operator.EQUAL, lastIp);
    }

    public EntryRequest<T> withLastIpSoundingLike(String lastIp){
       return withLastIp(Operator.SOUNDS_LIKE, lastIp);
    }



    public EntryRequest<T> filterByOpenedBy(String... openedBy){
      if (openedBy == null || openedBy.length == 0) {
        throw new IllegalArgumentException("filterByOpenedBy parameter openedBy cannot be empty");
      }
      return appendSearchCriteria(createOpenedByCriteria(Operator.EQUAL, (Object[])openedBy));
    }

    public EntryRequest<T> withOpenedBy(Operator operator, Object... values){
       return appendSearchCriteria(createOpenedByCriteria(operator, values));
    }

    public EntryRequest<T> withOpenedByIsUnknown(){
       return withOpenedBy(Operator.IS_NULL);
    }

    public EntryRequest<T> withOpenedByIsKnown(){
       return withOpenedBy(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createOpenedByCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Entry.OPENED_BY_PROPERTY, operator, values);
    }

    public EntryRequest<T> withOpenedByIsNot(String openedBy){
       return withOpenedBy(Operator.NOT_EQUAL, openedBy);
    }

    public EntryRequest<T> withOpenedByIn(String... openedBy){
       return withOpenedBy(Operator.IN, (Object[])openedBy);
    }

    public EntryRequest<T> withOpenedByNotIn(String... openedBy){
       return withOpenedBy(Operator.NOT_IN, (Object[])openedBy);
    }
    public EntryRequest<T> withOpenedByGreaterThan(String openedBy){
       return withOpenedBy(Operator.GREATER_THAN, openedBy);
    }

    public EntryRequest<T> withOpenedByGreaterThanOrEqualTo(String openedBy){
       return withOpenedBy(Operator.GREATER_THAN_OR_EQUAL, openedBy);
    }

    public EntryRequest<T> withOpenedByLessThan(String openedBy){
       return withOpenedBy(Operator.LESS_THAN, openedBy);
    }

    public EntryRequest<T> withOpenedByLessThanOrEqualTo(String openedBy){
       return withOpenedBy(Operator.LESS_THAN_OR_EQUAL, openedBy);
    }

    public EntryRequest<T> withOpenedByBetween(String startOfOpenedBy, String endOfOpenedBy){
       return withOpenedBy(Operator.BETWEEN, startOfOpenedBy, endOfOpenedBy);
    }
    public EntryRequest<T> withOpenedByStartingWith(String openedBy){
       return withOpenedBy(Operator.BEGIN_WITH, openedBy);
    }
    public EntryRequest<T> withOpenedByContaining(String openedBy){
       return withOpenedBy(Operator.CONTAIN, openedBy);
    }

    public EntryRequest<T> withOpenedByNotContaining(String openedBy){
       return withOpenedBy(Operator.NOT_CONTAIN, openedBy);
    }

    public EntryRequest<T> withOpenedByNotStartingWith(String openedBy){
       return withOpenedBy(Operator.NOT_BEGIN_WITH, openedBy);
    }

    public EntryRequest<T> withOpenedByEndingWith(String openedBy){
       return withOpenedBy(Operator.END_WITH, openedBy);
    }

    public EntryRequest<T> withOpenedByNotEndingWith(String openedBy){
       return withOpenedBy(Operator.NOT_END_WITH, openedBy);
    }

    public EntryRequest<T> withOpenedByIs(String openedBy){
       return withOpenedBy(Operator.EQUAL, openedBy);
    }

    public EntryRequest<T> withOpenedBySoundingLike(String openedBy){
       return withOpenedBy(Operator.SOUNDS_LIKE, openedBy);
    }



    public EntryRequest<T> filterByChangedBy(String... changedBy){
      if (changedBy == null || changedBy.length == 0) {
        throw new IllegalArgumentException("filterByChangedBy parameter changedBy cannot be empty");
      }
      return appendSearchCriteria(createChangedByCriteria(Operator.EQUAL, (Object[])changedBy));
    }

    public EntryRequest<T> withChangedBy(Operator operator, Object... values){
       return appendSearchCriteria(createChangedByCriteria(operator, values));
    }

    public EntryRequest<T> withChangedByIsUnknown(){
       return withChangedBy(Operator.IS_NULL);
    }

    public EntryRequest<T> withChangedByIsKnown(){
       return withChangedBy(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createChangedByCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Entry.CHANGED_BY_PROPERTY, operator, values);
    }

    public EntryRequest<T> withChangedByIsNot(String changedBy){
       return withChangedBy(Operator.NOT_EQUAL, changedBy);
    }

    public EntryRequest<T> withChangedByIn(String... changedBy){
       return withChangedBy(Operator.IN, (Object[])changedBy);
    }

    public EntryRequest<T> withChangedByNotIn(String... changedBy){
       return withChangedBy(Operator.NOT_IN, (Object[])changedBy);
    }
    public EntryRequest<T> withChangedByGreaterThan(String changedBy){
       return withChangedBy(Operator.GREATER_THAN, changedBy);
    }

    public EntryRequest<T> withChangedByGreaterThanOrEqualTo(String changedBy){
       return withChangedBy(Operator.GREATER_THAN_OR_EQUAL, changedBy);
    }

    public EntryRequest<T> withChangedByLessThan(String changedBy){
       return withChangedBy(Operator.LESS_THAN, changedBy);
    }

    public EntryRequest<T> withChangedByLessThanOrEqualTo(String changedBy){
       return withChangedBy(Operator.LESS_THAN_OR_EQUAL, changedBy);
    }

    public EntryRequest<T> withChangedByBetween(String startOfChangedBy, String endOfChangedBy){
       return withChangedBy(Operator.BETWEEN, startOfChangedBy, endOfChangedBy);
    }
    public EntryRequest<T> withChangedByStartingWith(String changedBy){
       return withChangedBy(Operator.BEGIN_WITH, changedBy);
    }
    public EntryRequest<T> withChangedByContaining(String changedBy){
       return withChangedBy(Operator.CONTAIN, changedBy);
    }

    public EntryRequest<T> withChangedByNotContaining(String changedBy){
       return withChangedBy(Operator.NOT_CONTAIN, changedBy);
    }

    public EntryRequest<T> withChangedByNotStartingWith(String changedBy){
       return withChangedBy(Operator.NOT_BEGIN_WITH, changedBy);
    }

    public EntryRequest<T> withChangedByEndingWith(String changedBy){
       return withChangedBy(Operator.END_WITH, changedBy);
    }

    public EntryRequest<T> withChangedByNotEndingWith(String changedBy){
       return withChangedBy(Operator.NOT_END_WITH, changedBy);
    }

    public EntryRequest<T> withChangedByIs(String changedBy){
       return withChangedBy(Operator.EQUAL, changedBy);
    }

    public EntryRequest<T> withChangedBySoundingLike(String changedBy){
       return withChangedBy(Operator.SOUNDS_LIKE, changedBy);
    }



    public EntryRequest<T> filterByGuardedToken(String... guardedToken){
      if (guardedToken == null || guardedToken.length == 0) {
        throw new IllegalArgumentException("filterByGuardedToken parameter guardedToken cannot be empty");
      }
      return appendSearchCriteria(createGuardedTokenCriteria(Operator.EQUAL, (Object[])guardedToken));
    }

    public EntryRequest<T> withGuardedToken(Operator operator, Object... values){
       return appendSearchCriteria(createGuardedTokenCriteria(operator, values));
    }

    public EntryRequest<T> withGuardedTokenIsUnknown(){
       return withGuardedToken(Operator.IS_NULL);
    }

    public EntryRequest<T> withGuardedTokenIsKnown(){
       return withGuardedToken(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createGuardedTokenCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Entry.GUARDED_TOKEN_PROPERTY, operator, values);
    }

    public EntryRequest<T> withGuardedTokenIsNot(String guardedToken){
       return withGuardedToken(Operator.NOT_EQUAL, guardedToken);
    }

    public EntryRequest<T> withGuardedTokenIn(String... guardedToken){
       return withGuardedToken(Operator.IN, (Object[])guardedToken);
    }

    public EntryRequest<T> withGuardedTokenNotIn(String... guardedToken){
       return withGuardedToken(Operator.NOT_IN, (Object[])guardedToken);
    }
    public EntryRequest<T> withGuardedTokenGreaterThan(String guardedToken){
       return withGuardedToken(Operator.GREATER_THAN, guardedToken);
    }

    public EntryRequest<T> withGuardedTokenGreaterThanOrEqualTo(String guardedToken){
       return withGuardedToken(Operator.GREATER_THAN_OR_EQUAL, guardedToken);
    }

    public EntryRequest<T> withGuardedTokenLessThan(String guardedToken){
       return withGuardedToken(Operator.LESS_THAN, guardedToken);
    }

    public EntryRequest<T> withGuardedTokenLessThanOrEqualTo(String guardedToken){
       return withGuardedToken(Operator.LESS_THAN_OR_EQUAL, guardedToken);
    }

    public EntryRequest<T> withGuardedTokenBetween(String startOfGuardedToken, String endOfGuardedToken){
       return withGuardedToken(Operator.BETWEEN, startOfGuardedToken, endOfGuardedToken);
    }
    public EntryRequest<T> withGuardedTokenStartingWith(String guardedToken){
       return withGuardedToken(Operator.BEGIN_WITH, guardedToken);
    }
    public EntryRequest<T> withGuardedTokenContaining(String guardedToken){
       return withGuardedToken(Operator.CONTAIN, guardedToken);
    }

    public EntryRequest<T> withGuardedTokenNotContaining(String guardedToken){
       return withGuardedToken(Operator.NOT_CONTAIN, guardedToken);
    }

    public EntryRequest<T> withGuardedTokenNotStartingWith(String guardedToken){
       return withGuardedToken(Operator.NOT_BEGIN_WITH, guardedToken);
    }

    public EntryRequest<T> withGuardedTokenEndingWith(String guardedToken){
       return withGuardedToken(Operator.END_WITH, guardedToken);
    }

    public EntryRequest<T> withGuardedTokenNotEndingWith(String guardedToken){
       return withGuardedToken(Operator.NOT_END_WITH, guardedToken);
    }

    public EntryRequest<T> withGuardedTokenIs(String guardedToken){
       return withGuardedToken(Operator.EQUAL, guardedToken);
    }

    public EntryRequest<T> withGuardedTokenSoundingLike(String guardedToken){
       return withGuardedToken(Operator.SOUNDS_LIKE, guardedToken);
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
       groupBy(Entry.CATALOG_PROPERTY, subRequest);
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

    public EntryRequest<T> groupByBornAt(){
       groupBy(Entry.BORN_AT_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByBornAtAs(String retName){
       groupBy(retName, Entry.BORN_AT_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByBornAtWithFunction(String retName, AggrFunction function){
       groupBy(retName, Entry.BORN_AT_PROPERTY, function);
       return this;
    }

    public EntryRequest<T> groupByTouchedAt(){
       groupBy(Entry.TOUCHED_AT_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByTouchedAtAs(String retName){
       groupBy(retName, Entry.TOUCHED_AT_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByTouchedAtWithFunction(String retName, AggrFunction function){
       groupBy(retName, Entry.TOUCHED_AT_PROPERTY, function);
       return this;
    }

    public EntryRequest<T> groupBySourceIp(){
       groupBy(Entry.SOURCE_IP_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupBySourceIpAs(String retName){
       groupBy(retName, Entry.SOURCE_IP_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupBySourceIpWithFunction(String retName, AggrFunction function){
       groupBy(retName, Entry.SOURCE_IP_PROPERTY, function);
       return this;
    }

    public EntryRequest<T> groupByLastIp(){
       groupBy(Entry.LAST_IP_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByLastIpAs(String retName){
       groupBy(retName, Entry.LAST_IP_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByLastIpWithFunction(String retName, AggrFunction function){
       groupBy(retName, Entry.LAST_IP_PROPERTY, function);
       return this;
    }

    public EntryRequest<T> groupByOpenedBy(){
       groupBy(Entry.OPENED_BY_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByOpenedByAs(String retName){
       groupBy(retName, Entry.OPENED_BY_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByOpenedByWithFunction(String retName, AggrFunction function){
       groupBy(retName, Entry.OPENED_BY_PROPERTY, function);
       return this;
    }

    public EntryRequest<T> groupByChangedBy(){
       groupBy(Entry.CHANGED_BY_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByChangedByAs(String retName){
       groupBy(retName, Entry.CHANGED_BY_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByChangedByWithFunction(String retName, AggrFunction function){
       groupBy(retName, Entry.CHANGED_BY_PROPERTY, function);
       return this;
    }

    public EntryRequest<T> groupByGuardedToken(){
       groupBy(Entry.GUARDED_TOKEN_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByGuardedTokenAs(String retName){
       groupBy(retName, Entry.GUARDED_TOKEN_PROPERTY);
       return this;
    }

    public EntryRequest<T> groupByGuardedTokenWithFunction(String retName, AggrFunction function){
       groupBy(retName, Entry.GUARDED_TOKEN_PROPERTY, function);
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
    public EntryRequest<T> orderByBornAtAscending(){
       addOrderByAscending(Entry.BORN_AT_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByBornAtDescending(){
       addOrderByDescending(Entry.BORN_AT_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByTouchedAtAscending(){
       addOrderByAscending(Entry.TOUCHED_AT_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByTouchedAtDescending(){
       addOrderByDescending(Entry.TOUCHED_AT_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderBySourceIpAscending(){
       addOrderByAscending(Entry.SOURCE_IP_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderBySourceIpDescending(){
       addOrderByDescending(Entry.SOURCE_IP_PROPERTY);
       return this;
    }
    public EntryRequest<T> orderBySourceIpAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Entry.SOURCE_IP_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderBySourceIpDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Entry.SOURCE_IP_PROPERTY);
       return this;
    }
    public EntryRequest<T> orderByLastIpAscending(){
       addOrderByAscending(Entry.LAST_IP_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByLastIpDescending(){
       addOrderByDescending(Entry.LAST_IP_PROPERTY);
       return this;
    }
    public EntryRequest<T> orderByLastIpAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Entry.LAST_IP_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByLastIpDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Entry.LAST_IP_PROPERTY);
       return this;
    }
    public EntryRequest<T> orderByOpenedByAscending(){
       addOrderByAscending(Entry.OPENED_BY_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByOpenedByDescending(){
       addOrderByDescending(Entry.OPENED_BY_PROPERTY);
       return this;
    }
    public EntryRequest<T> orderByOpenedByAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Entry.OPENED_BY_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByOpenedByDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Entry.OPENED_BY_PROPERTY);
       return this;
    }
    public EntryRequest<T> orderByChangedByAscending(){
       addOrderByAscending(Entry.CHANGED_BY_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByChangedByDescending(){
       addOrderByDescending(Entry.CHANGED_BY_PROPERTY);
       return this;
    }
    public EntryRequest<T> orderByChangedByAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Entry.CHANGED_BY_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByChangedByDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Entry.CHANGED_BY_PROPERTY);
       return this;
    }
    public EntryRequest<T> orderByGuardedTokenAscending(){
       addOrderByAscending(Entry.GUARDED_TOKEN_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByGuardedTokenDescending(){
       addOrderByDescending(Entry.GUARDED_TOKEN_PROPERTY);
       return this;
    }
    public EntryRequest<T> orderByGuardedTokenAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Entry.GUARDED_TOKEN_PROPERTY);
       return this;
    }

    public EntryRequest<T> orderByGuardedTokenDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Entry.GUARDED_TOKEN_PROPERTY);
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
       this.withCatalogMatching(catalog)
           .groupByCatalogWith(catalog);
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