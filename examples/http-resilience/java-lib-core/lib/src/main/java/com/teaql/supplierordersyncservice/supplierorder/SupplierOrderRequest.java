
package com.teaql.supplierordersyncservice.supplierorder;

import com.teaql.supplierordersyncservice.Q;
import com.teaql.supplierordersyncservice.supplier.Supplier;
import com.teaql.supplierordersyncservice.supplier.SupplierRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.util.Objects;

public class SupplierOrderRequest<T extends SupplierOrder> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public SupplierOrderRequest(Class<T> returnType){
        this(returnType, () -> (T) new SupplierOrder());
    }

    protected SupplierOrderRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public SupplierOrderRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public SupplierOrderRequest<T> copy(){
        return (SupplierOrderRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new SupplierOrderRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public SupplierOrderRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public SupplierOrderRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public SupplierOrderRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public SupplierOrderRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public SupplierOrderRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public SupplierOrderRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public SupplierOrderRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public SupplierOrderRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public SupplierOrderRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public SupplierOrderRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (SupplierOrderRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public SupplierOrderRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public SupplierOrderRequest<T> matchingAnyOf(SupplierOrderRequest supplierOrder){
        super.internalMatchAny(supplierOrder);
        return this;
    }

    public SupplierOrderRequest<T> matchingIdsOf(SupplierOrderRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                SupplierOrder.ID_PROPERTY, request, SupplierOrder.ID_PROPERTY));
    }

    public SupplierOrderRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public SupplierOrderRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public SupplierOrderRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public SupplierOrderRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectSupplierIdOnly().selectExternalReference().selectSyncStatus().selectVersion();
    }

    public SupplierOrderRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public SupplierOrderRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(SupplierOrder.ID_PROPERTY);
        selectProperty(SupplierOrder.SUPPLIER_PROPERTY);
        selectProperty(SupplierOrder.EXTERNAL_REFERENCE_PROPERTY);
        selectProperty(SupplierOrder.SYNC_STATUS_PROPERTY);
        selectProperty(SupplierOrder.VERSION_PROPERTY);
        return this;
    }

    public SupplierOrderRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectSupplier().selectExternalReference().selectSyncStatus().selectVersion();
    }

    public SupplierOrderRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectSupplier().selectExternalReference().selectSyncStatus().selectVersion();
    }


    public SupplierOrderRequest<T> selectId(){
       selectProperty(SupplierOrder.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SupplierOrderRequest<T> unselectId(){
       unselectProperty(SupplierOrder.ID_PROPERTY);
       return this;
    }
    public SupplierOrderRequest<T> selectSupplierIdOnly(){
       selectProperty(SupplierOrder.SUPPLIER_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> selectSupplier(){
        return selectSupplierWith(Q.suppliers().unlimited().selectSelf());
    }

    public SupplierOrderRequest<T> selectSupplierWith(SupplierRequest supplier){
       selectProperty(SupplierOrder.SUPPLIER_PROPERTY);
       enhanceRelation(SupplierOrder.SUPPLIER_PROPERTY, supplier,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectSupplierWith", null, SupplierOrder.SUPPLIER_PROPERTY));
       return this;
    }

    public SupplierOrderRequest<T> unselectSupplier(){
       unselectProperty(SupplierOrder.SUPPLIER_PROPERTY);
       return this;
    }
    public SupplierOrderRequest<T> selectExternalReference(){
       selectProperty(SupplierOrder.EXTERNAL_REFERENCE_PROPERTY);
       return this;
    }

    /**
     * fill the externalReference with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  externalReference) to fetch externalReference property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SupplierOrderRequest<T> unselectExternalReference(){
       unselectProperty(SupplierOrder.EXTERNAL_REFERENCE_PROPERTY);
       return this;
    }
    public SupplierOrderRequest<T> selectSyncStatus(){
       selectProperty(SupplierOrder.SYNC_STATUS_PROPERTY);
       return this;
    }

    /**
     * fill the syncStatus with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  syncStatus) to fetch syncStatus property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SupplierOrderRequest<T> unselectSyncStatus(){
       unselectProperty(SupplierOrder.SYNC_STATUS_PROPERTY);
       return this;
    }
    public SupplierOrderRequest<T> selectVersion(){
       selectProperty(SupplierOrder.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SupplierOrderRequest<T> unselectVersion(){
       unselectProperty(SupplierOrder.VERSION_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SupplierOrder.ID_PROPERTY, operator, values);
    }

    public SupplierOrderRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public SupplierOrderRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public SupplierOrderRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public SupplierOrderRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public SupplierOrderRequest<T> filterBySupplier(Supplier... supplier){
      if (supplier == null || supplier.length == 0) {
        throw new IllegalArgumentException("filterBySupplier parameter supplier cannot be empty");
      }
      return appendSearchCriteria(createSupplierCriteria(Operator.EQUAL, (Object[])supplier));
    }

    public SupplierOrderRequest<T> withSupplier(Operator operator, Object... values){
       return appendSearchCriteria(createSupplierCriteria(operator, values));
    }

    public SupplierOrderRequest<T> withSupplierIsUnknown(){
       return withSupplier(Operator.IS_NULL);
    }

    public SupplierOrderRequest<T> withSupplierIsKnown(){
       return withSupplier(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createSupplierCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SupplierOrder.SUPPLIER_PROPERTY, operator, values);
    }

    public SupplierOrderRequest<T> filterBySupplier(Long supplier){
      if(supplier == null){
         return this;
      }
      return withSupplier(Operator.EQUAL, supplier);
    }
    public SupplierOrderRequest<T> withSupplierMatching(SupplierRequest supplier){
       return appendSearchCriteria(new SubQuerySearchCriteria(SupplierOrder.SUPPLIER_PROPERTY, supplier, Supplier.ID_PROPERTY));
    }

    public SupplierOrderRequest<T> withoutSupplierMatching(SupplierRequest supplier){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(SupplierOrder.SUPPLIER_PROPERTY, supplier, Supplier.ID_PROPERTY)));
    }

    public SupplierOrderRequest<T> filterByExternalReference(String... externalReference){
      if (externalReference == null || externalReference.length == 0) {
        throw new IllegalArgumentException("filterByExternalReference parameter externalReference cannot be empty");
      }
      return appendSearchCriteria(createExternalReferenceCriteria(Operator.EQUAL, (Object[])externalReference));
    }

    public SupplierOrderRequest<T> withExternalReference(Operator operator, Object... values){
       return appendSearchCriteria(createExternalReferenceCriteria(operator, values));
    }

    public SupplierOrderRequest<T> withExternalReferenceIsUnknown(){
       return withExternalReference(Operator.IS_NULL);
    }

    public SupplierOrderRequest<T> withExternalReferenceIsKnown(){
       return withExternalReference(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createExternalReferenceCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SupplierOrder.EXTERNAL_REFERENCE_PROPERTY, operator, values);
    }

    public SupplierOrderRequest<T> withExternalReferenceIsNot(String externalReference){
       return withExternalReference(Operator.NOT_EQUAL, externalReference);
    }

    public SupplierOrderRequest<T> withExternalReferenceIn(String... externalReference){
       return withExternalReference(Operator.IN, (Object[])externalReference);
    }

    public SupplierOrderRequest<T> withExternalReferenceNotIn(String... externalReference){
       return withExternalReference(Operator.NOT_IN, (Object[])externalReference);
    }
    public SupplierOrderRequest<T> withExternalReferenceGreaterThan(String externalReference){
       return withExternalReference(Operator.GREATER_THAN, externalReference);
    }

    public SupplierOrderRequest<T> withExternalReferenceGreaterThanOrEqualTo(String externalReference){
       return withExternalReference(Operator.GREATER_THAN_OR_EQUAL, externalReference);
    }

    public SupplierOrderRequest<T> withExternalReferenceLessThan(String externalReference){
       return withExternalReference(Operator.LESS_THAN, externalReference);
    }

    public SupplierOrderRequest<T> withExternalReferenceLessThanOrEqualTo(String externalReference){
       return withExternalReference(Operator.LESS_THAN_OR_EQUAL, externalReference);
    }

    public SupplierOrderRequest<T> withExternalReferenceBetween(String startOfExternalReference, String endOfExternalReference){
       return withExternalReference(Operator.BETWEEN, startOfExternalReference, endOfExternalReference);
    }
    public SupplierOrderRequest<T> withExternalReferenceStartingWith(String externalReference){
       return withExternalReference(Operator.BEGIN_WITH, externalReference);
    }
    public SupplierOrderRequest<T> withExternalReferenceContaining(String externalReference){
       return withExternalReference(Operator.CONTAIN, externalReference);
    }

    public SupplierOrderRequest<T> withExternalReferenceNotContaining(String externalReference){
       return withExternalReference(Operator.NOT_CONTAIN, externalReference);
    }

    public SupplierOrderRequest<T> withExternalReferenceNotStartingWith(String externalReference){
       return withExternalReference(Operator.NOT_BEGIN_WITH, externalReference);
    }

    public SupplierOrderRequest<T> withExternalReferenceEndingWith(String externalReference){
       return withExternalReference(Operator.END_WITH, externalReference);
    }

    public SupplierOrderRequest<T> withExternalReferenceNotEndingWith(String externalReference){
       return withExternalReference(Operator.NOT_END_WITH, externalReference);
    }

    public SupplierOrderRequest<T> withExternalReferenceIs(String externalReference){
       return withExternalReference(Operator.EQUAL, externalReference);
    }

    public SupplierOrderRequest<T> withExternalReferenceSoundingLike(String externalReference){
       return withExternalReference(Operator.SOUNDS_LIKE, externalReference);
    }



    public SupplierOrderRequest<T> filterBySyncStatus(String... syncStatus){
      if (syncStatus == null || syncStatus.length == 0) {
        throw new IllegalArgumentException("filterBySyncStatus parameter syncStatus cannot be empty");
      }
      return appendSearchCriteria(createSyncStatusCriteria(Operator.EQUAL, (Object[])syncStatus));
    }

    public SupplierOrderRequest<T> withSyncStatus(Operator operator, Object... values){
       return appendSearchCriteria(createSyncStatusCriteria(operator, values));
    }

    public SupplierOrderRequest<T> withSyncStatusIsUnknown(){
       return withSyncStatus(Operator.IS_NULL);
    }

    public SupplierOrderRequest<T> withSyncStatusIsKnown(){
       return withSyncStatus(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createSyncStatusCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SupplierOrder.SYNC_STATUS_PROPERTY, operator, values);
    }

    public SupplierOrderRequest<T> withSyncStatusIsNot(String syncStatus){
       return withSyncStatus(Operator.NOT_EQUAL, syncStatus);
    }

    public SupplierOrderRequest<T> withSyncStatusIn(String... syncStatus){
       return withSyncStatus(Operator.IN, (Object[])syncStatus);
    }

    public SupplierOrderRequest<T> withSyncStatusNotIn(String... syncStatus){
       return withSyncStatus(Operator.NOT_IN, (Object[])syncStatus);
    }
    public SupplierOrderRequest<T> withSyncStatusGreaterThan(String syncStatus){
       return withSyncStatus(Operator.GREATER_THAN, syncStatus);
    }

    public SupplierOrderRequest<T> withSyncStatusGreaterThanOrEqualTo(String syncStatus){
       return withSyncStatus(Operator.GREATER_THAN_OR_EQUAL, syncStatus);
    }

    public SupplierOrderRequest<T> withSyncStatusLessThan(String syncStatus){
       return withSyncStatus(Operator.LESS_THAN, syncStatus);
    }

    public SupplierOrderRequest<T> withSyncStatusLessThanOrEqualTo(String syncStatus){
       return withSyncStatus(Operator.LESS_THAN_OR_EQUAL, syncStatus);
    }

    public SupplierOrderRequest<T> withSyncStatusBetween(String startOfSyncStatus, String endOfSyncStatus){
       return withSyncStatus(Operator.BETWEEN, startOfSyncStatus, endOfSyncStatus);
    }
    public SupplierOrderRequest<T> withSyncStatusStartingWith(String syncStatus){
       return withSyncStatus(Operator.BEGIN_WITH, syncStatus);
    }
    public SupplierOrderRequest<T> withSyncStatusContaining(String syncStatus){
       return withSyncStatus(Operator.CONTAIN, syncStatus);
    }

    public SupplierOrderRequest<T> withSyncStatusNotContaining(String syncStatus){
       return withSyncStatus(Operator.NOT_CONTAIN, syncStatus);
    }

    public SupplierOrderRequest<T> withSyncStatusNotStartingWith(String syncStatus){
       return withSyncStatus(Operator.NOT_BEGIN_WITH, syncStatus);
    }

    public SupplierOrderRequest<T> withSyncStatusEndingWith(String syncStatus){
       return withSyncStatus(Operator.END_WITH, syncStatus);
    }

    public SupplierOrderRequest<T> withSyncStatusNotEndingWith(String syncStatus){
       return withSyncStatus(Operator.NOT_END_WITH, syncStatus);
    }

    public SupplierOrderRequest<T> withSyncStatusIs(String syncStatus){
       return withSyncStatus(Operator.EQUAL, syncStatus);
    }

    public SupplierOrderRequest<T> withSyncStatusSoundingLike(String syncStatus){
       return withSyncStatus(Operator.SOUNDS_LIKE, syncStatus);
    }



    public SupplierOrderRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public SupplierOrderRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public SupplierOrderRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public SupplierOrderRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(SupplierOrder.VERSION_PROPERTY, operator, values);
    }

    public SupplierOrderRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public SupplierOrderRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public SupplierOrderRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public SupplierOrderRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public SupplierOrderRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public SupplierOrderRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public SupplierOrderRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public SupplierOrderRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public SupplierOrderRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }



    public SupplierOrderRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public SupplierOrderRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public SupplierOrderRequest<T> groupBySupplierWithDetails(){
       return groupBySupplierWithDetails(Q.suppliers().unlimited());
    }

    public SupplierOrderRequest<T> groupBySupplierWithDetails(SupplierRequest subRequest){
       aggregate(SupplierOrder.SUPPLIER_PROPERTY, subRequest);
       return this;
    }




    public SupplierOrderRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public SupplierOrderRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, SupplierOrder.ID_PROPERTY);
        return this;
    }
    public SupplierOrderRequest<T> countDistinctSupplier(){
        return countDistinctSupplierAs("supplier_distinct_count");
    }

    public SupplierOrderRequest<T> countDistinctSupplierAs(String alias){
        super.countDistinctProperty(alias, SupplierOrder.SUPPLIER_PROPERTY);
        return this;
    }
    public SupplierOrderRequest<T> countDistinctExternalReference(){
        return countDistinctExternalReferenceAs("external_reference_distinct_count");
    }

    public SupplierOrderRequest<T> countDistinctExternalReferenceAs(String alias){
        super.countDistinctProperty(alias, SupplierOrder.EXTERNAL_REFERENCE_PROPERTY);
        return this;
    }
    public SupplierOrderRequest<T> countDistinctSyncStatus(){
        return countDistinctSyncStatusAs("sync_status_distinct_count");
    }

    public SupplierOrderRequest<T> countDistinctSyncStatusAs(String alias){
        super.countDistinctProperty(alias, SupplierOrder.SYNC_STATUS_PROPERTY);
        return this;
    }
    public SupplierOrderRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public SupplierOrderRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, SupplierOrder.VERSION_PROPERTY);
        return this;
    }

    public SupplierOrderRequest<T> groupById(){
       groupBy(SupplierOrder.ID_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> groupByIdAs(String retName){
       groupBy(retName, SupplierOrder.ID_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, SupplierOrder.ID_PROPERTY, function);
       return this;
    }
    public SupplierOrderRequest<T> groupBySupplierWith(SupplierRequest subRequest){
       groupBy(SupplierOrder.SUPPLIER_PROPERTY, subRequest,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "groupBySupplierWith", null, "supplier"));
       return this;
    }
    public SupplierOrderRequest<T> groupBySupplier(){
       groupBy(SupplierOrder.SUPPLIER_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> groupBySupplierAs(String retName){
       groupBy(retName, SupplierOrder.SUPPLIER_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> groupBySupplierWithFunction(String retName, AggrFunction function){
       groupBy(retName, SupplierOrder.SUPPLIER_PROPERTY, function);
       return this;
    }

    public SupplierOrderRequest<T> groupByExternalReference(){
       groupBy(SupplierOrder.EXTERNAL_REFERENCE_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> groupByExternalReferenceAs(String retName){
       groupBy(retName, SupplierOrder.EXTERNAL_REFERENCE_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> groupByExternalReferenceWithFunction(String retName, AggrFunction function){
       groupBy(retName, SupplierOrder.EXTERNAL_REFERENCE_PROPERTY, function);
       return this;
    }

    public SupplierOrderRequest<T> groupBySyncStatus(){
       groupBy(SupplierOrder.SYNC_STATUS_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> groupBySyncStatusAs(String retName){
       groupBy(retName, SupplierOrder.SYNC_STATUS_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> groupBySyncStatusWithFunction(String retName, AggrFunction function){
       groupBy(retName, SupplierOrder.SYNC_STATUS_PROPERTY, function);
       return this;
    }

    public SupplierOrderRequest<T> groupByVersion(){
       groupBy(SupplierOrder.VERSION_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> groupByVersionAs(String retName){
       groupBy(retName, SupplierOrder.VERSION_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, SupplierOrder.VERSION_PROPERTY, function);
       return this;
    }



    public SupplierOrderRequest<T> orderByIdAscending(){
       addOrderByAscending(SupplierOrder.ID_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> orderByIdDescending(){
       addOrderByDescending(SupplierOrder.ID_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> orderBySupplierAscending(){
       addOrderByAscending(SupplierOrder.SUPPLIER_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> orderBySupplierDescending(){
       addOrderByDescending(SupplierOrder.SUPPLIER_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> orderByExternalReferenceAscending(){
       addOrderByAscending(SupplierOrder.EXTERNAL_REFERENCE_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> orderByExternalReferenceDescending(){
       addOrderByDescending(SupplierOrder.EXTERNAL_REFERENCE_PROPERTY);
       return this;
    }
    public SupplierOrderRequest<T> orderByExternalReferenceAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(SupplierOrder.EXTERNAL_REFERENCE_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> orderByExternalReferenceDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(SupplierOrder.EXTERNAL_REFERENCE_PROPERTY);
       return this;
    }
    public SupplierOrderRequest<T> orderBySyncStatusAscending(){
       addOrderByAscending(SupplierOrder.SYNC_STATUS_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> orderBySyncStatusDescending(){
       addOrderByDescending(SupplierOrder.SYNC_STATUS_PROPERTY);
       return this;
    }
    public SupplierOrderRequest<T> orderBySyncStatusAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(SupplierOrder.SYNC_STATUS_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> orderBySyncStatusDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(SupplierOrder.SYNC_STATUS_PROPERTY);
       return this;
    }
    public SupplierOrderRequest<T> orderByVersionAscending(){
       addOrderByAscending(SupplierOrder.VERSION_PROPERTY);
       return this;
    }

    public SupplierOrderRequest<T> orderByVersionDescending(){
       addOrderByDescending(SupplierOrder.VERSION_PROPERTY);
       return this;
    }


    public SupplierRequest rollUpToSupplier(){
       SupplierRequest supplier = Q.suppliers().unlimited();
       this.internalRollUpTo("supplier", supplier,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "rollUpToSupplier", null, "supplier"));
       return supplier;
    }





   public SupplierOrderRequest<T> facetBySupplierAs(String facetName, SupplierRequest supplier){
       return facetBySupplierAs(facetName, supplier, true);
   }

   public SupplierOrderRequest<T> facetBySupplierAs(String facetName, SupplierRequest supplier, boolean includeAllFacets){
       addFacet(facetName, SupplierOrder.SUPPLIER_PROPERTY, supplier, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetBySupplierAs", facetName, SupplierOrder.SUPPLIER_PROPERTY));
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public SupplierOrderRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public SupplierOrderRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public SupplierOrderRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public SupplierOrderRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public SupplierOrderRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public SupplierOrderRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}