
package com.teaql.purchaseordernumbering.purchaseorder;

import com.teaql.purchaseordernumbering.Q;
import com.teaql.purchaseordernumbering.platform.Platform;
import com.teaql.purchaseordernumbering.platform.PlatformRequest;
import com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.OrderNumber;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.util.Objects;

public class PurchaseOrderRequest<T extends PurchaseOrder> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public PurchaseOrderRequest(Class<T> returnType){
        this(returnType, () -> (T) new PurchaseOrder());
    }

    protected PurchaseOrderRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public PurchaseOrderRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public PurchaseOrderRequest<T> copy(){
        return (PurchaseOrderRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new PurchaseOrderRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public PurchaseOrderRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public PurchaseOrderRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public PurchaseOrderRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public PurchaseOrderRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public PurchaseOrderRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public PurchaseOrderRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public PurchaseOrderRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public PurchaseOrderRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public PurchaseOrderRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public PurchaseOrderRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (PurchaseOrderRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public PurchaseOrderRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public PurchaseOrderRequest<T> matchingAnyOf(PurchaseOrderRequest purchaseOrder){
        super.internalMatchAny(purchaseOrder);
        return this;
    }

    public PurchaseOrderRequest<T> matchingIdsOf(PurchaseOrderRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                PurchaseOrder.ID_PROPERTY, request, PurchaseOrder.ID_PROPERTY));
    }

    public PurchaseOrderRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public PurchaseOrderRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public PurchaseOrderRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public PurchaseOrderRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectPlatformIdOnly().selectSupplier().selectOrderNumber().selectVersion();
    }

    public PurchaseOrderRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public PurchaseOrderRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(PurchaseOrder.ID_PROPERTY);
        selectProperty(PurchaseOrder.PLATFORM_PROPERTY);
        selectProperty(PurchaseOrder.SUPPLIER_PROPERTY);
        selectProperty(PurchaseOrder.ORDER_NUMBER_PROPERTY);
        selectProperty(PurchaseOrder.VERSION_PROPERTY);
        return this;
    }

    public PurchaseOrderRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectPlatform().selectSupplier().selectOrderNumber().selectVersion();
    }

    public PurchaseOrderRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectPlatform().selectSupplier().selectOrderNumber().selectVersion();
    }


    public PurchaseOrderRequest<T> selectId(){
       selectProperty(PurchaseOrder.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PurchaseOrderRequest<T> unselectId(){
       unselectProperty(PurchaseOrder.ID_PROPERTY);
       return this;
    }
    public PurchaseOrderRequest<T> selectPlatformIdOnly(){
       selectProperty(PurchaseOrder.PLATFORM_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> selectPlatform(){
        return selectPlatformWith(Q.platforms().unlimited().selectSelf());
    }

    public PurchaseOrderRequest<T> selectPlatformWith(PlatformRequest platform){
       selectProperty(PurchaseOrder.PLATFORM_PROPERTY);
       enhanceRelation(PurchaseOrder.PLATFORM_PROPERTY, platform,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectPlatformWith", null, PurchaseOrder.PLATFORM_PROPERTY));
       return this;
    }

    public PurchaseOrderRequest<T> unselectPlatform(){
       unselectProperty(PurchaseOrder.PLATFORM_PROPERTY);
       return this;
    }
    public PurchaseOrderRequest<T> selectSupplier(){
       selectProperty(PurchaseOrder.SUPPLIER_PROPERTY);
       return this;
    }

    /**
     * fill the supplier with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  supplier) to fetch supplier property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PurchaseOrderRequest<T> unselectSupplier(){
       unselectProperty(PurchaseOrder.SUPPLIER_PROPERTY);
       return this;
    }
    public PurchaseOrderRequest<T> selectOrderNumber(){
       selectProperty(PurchaseOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    /**
     * fill the orderNumber with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  orderNumber) to fetch orderNumber property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PurchaseOrderRequest<T> unselectOrderNumber(){
       unselectProperty(PurchaseOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }
    public PurchaseOrderRequest<T> selectVersion(){
       selectProperty(PurchaseOrder.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PurchaseOrderRequest<T> unselectVersion(){
       unselectProperty(PurchaseOrder.VERSION_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(PurchaseOrder.ID_PROPERTY, operator, values);
    }

    public PurchaseOrderRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public PurchaseOrderRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public PurchaseOrderRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public PurchaseOrderRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public PurchaseOrderRequest<T> filterByPlatform(Platform... platform){
      if (platform == null || platform.length == 0) {
        throw new IllegalArgumentException("filterByPlatform parameter platform cannot be empty");
      }
      return appendSearchCriteria(createPlatformCriteria(Operator.EQUAL, (Object[])platform));
    }

    public PurchaseOrderRequest<T> withPlatform(Operator operator, Object... values){
       return appendSearchCriteria(createPlatformCriteria(operator, values));
    }

    public PurchaseOrderRequest<T> withPlatformIsUnknown(){
       return withPlatform(Operator.IS_NULL);
    }

    public PurchaseOrderRequest<T> withPlatformIsKnown(){
       return withPlatform(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createPlatformCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(PurchaseOrder.PLATFORM_PROPERTY, operator, values);
    }

    public PurchaseOrderRequest<T> filterByPlatform(Long platform){
      if(platform == null){
         return this;
      }
      return withPlatform(Operator.EQUAL, platform);
    }
    public PurchaseOrderRequest<T> withPlatformMatching(PlatformRequest platform){
       return appendSearchCriteria(new SubQuerySearchCriteria(PurchaseOrder.PLATFORM_PROPERTY, platform, Platform.ID_PROPERTY));
    }

    public PurchaseOrderRequest<T> withoutPlatformMatching(PlatformRequest platform){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(PurchaseOrder.PLATFORM_PROPERTY, platform, Platform.ID_PROPERTY)));
    }

    public PurchaseOrderRequest<T> filterBySupplier(String... supplier){
      if (supplier == null || supplier.length == 0) {
        throw new IllegalArgumentException("filterBySupplier parameter supplier cannot be empty");
      }
      return appendSearchCriteria(createSupplierCriteria(Operator.EQUAL, (Object[])supplier));
    }

    public PurchaseOrderRequest<T> withSupplier(Operator operator, Object... values){
       return appendSearchCriteria(createSupplierCriteria(operator, values));
    }

    public PurchaseOrderRequest<T> withSupplierIsUnknown(){
       return withSupplier(Operator.IS_NULL);
    }

    public PurchaseOrderRequest<T> withSupplierIsKnown(){
       return withSupplier(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createSupplierCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(PurchaseOrder.SUPPLIER_PROPERTY, operator, values);
    }

    public PurchaseOrderRequest<T> withSupplierIsNot(String supplier){
       return withSupplier(Operator.NOT_EQUAL, supplier);
    }

    public PurchaseOrderRequest<T> withSupplierIn(String... supplier){
       return withSupplier(Operator.IN, (Object[])supplier);
    }

    public PurchaseOrderRequest<T> withSupplierNotIn(String... supplier){
       return withSupplier(Operator.NOT_IN, (Object[])supplier);
    }
    public PurchaseOrderRequest<T> withSupplierGreaterThan(String supplier){
       return withSupplier(Operator.GREATER_THAN, supplier);
    }

    public PurchaseOrderRequest<T> withSupplierGreaterThanOrEqualTo(String supplier){
       return withSupplier(Operator.GREATER_THAN_OR_EQUAL, supplier);
    }

    public PurchaseOrderRequest<T> withSupplierLessThan(String supplier){
       return withSupplier(Operator.LESS_THAN, supplier);
    }

    public PurchaseOrderRequest<T> withSupplierLessThanOrEqualTo(String supplier){
       return withSupplier(Operator.LESS_THAN_OR_EQUAL, supplier);
    }

    public PurchaseOrderRequest<T> withSupplierBetween(String startOfSupplier, String endOfSupplier){
       return withSupplier(Operator.BETWEEN, startOfSupplier, endOfSupplier);
    }
    public PurchaseOrderRequest<T> withSupplierStartingWith(String supplier){
       return withSupplier(Operator.BEGIN_WITH, supplier);
    }
    public PurchaseOrderRequest<T> withSupplierContaining(String supplier){
       return withSupplier(Operator.CONTAIN, supplier);
    }

    public PurchaseOrderRequest<T> withSupplierNotContaining(String supplier){
       return withSupplier(Operator.NOT_CONTAIN, supplier);
    }

    public PurchaseOrderRequest<T> withSupplierNotStartingWith(String supplier){
       return withSupplier(Operator.NOT_BEGIN_WITH, supplier);
    }

    public PurchaseOrderRequest<T> withSupplierEndingWith(String supplier){
       return withSupplier(Operator.END_WITH, supplier);
    }

    public PurchaseOrderRequest<T> withSupplierNotEndingWith(String supplier){
       return withSupplier(Operator.NOT_END_WITH, supplier);
    }

    public PurchaseOrderRequest<T> withSupplierIs(String supplier){
       return withSupplier(Operator.EQUAL, supplier);
    }

    public PurchaseOrderRequest<T> withSupplierSoundingLike(String supplier){
       return withSupplier(Operator.SOUNDS_LIKE, supplier);
    }



    public PurchaseOrderRequest<T> withOrderNumberIs(OrderNumber value){
       return appendSearchCriteria(createBasicSearchCriteria(PurchaseOrder.ORDER_NUMBER_PROPERTY, Operator.EQUAL, value.value()));
    }

    public PurchaseOrderRequest<T> withOrderNumberIsNot(OrderNumber value){
       return appendSearchCriteria(createBasicSearchCriteria(PurchaseOrder.ORDER_NUMBER_PROPERTY, Operator.NOT_EQUAL, value.value()));
    }

    public PurchaseOrderRequest<T> withOrderNumberIsUnknown(){
       return appendSearchCriteria(createBasicSearchCriteria(PurchaseOrder.ORDER_NUMBER_PROPERTY, Operator.IS_NULL));
    }

    public PurchaseOrderRequest<T> withOrderNumberIsKnown(){
       return appendSearchCriteria(createBasicSearchCriteria(PurchaseOrder.ORDER_NUMBER_PROPERTY, Operator.IS_NOT_NULL));
    }

    public PurchaseOrderRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public PurchaseOrderRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public PurchaseOrderRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public PurchaseOrderRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(PurchaseOrder.VERSION_PROPERTY, operator, values);
    }

    public PurchaseOrderRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public PurchaseOrderRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public PurchaseOrderRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public PurchaseOrderRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public PurchaseOrderRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public PurchaseOrderRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public PurchaseOrderRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public PurchaseOrderRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public PurchaseOrderRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }



    public PurchaseOrderRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public PurchaseOrderRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public PurchaseOrderRequest<T> groupByPlatformWithDetails(){
       return groupByPlatformWithDetails(Q.platforms().unlimited());
    }

    public PurchaseOrderRequest<T> groupByPlatformWithDetails(PlatformRequest subRequest){
       aggregate(PurchaseOrder.PLATFORM_PROPERTY, subRequest);
       return this;
    }




    public PurchaseOrderRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public PurchaseOrderRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, PurchaseOrder.ID_PROPERTY);
        return this;
    }
    public PurchaseOrderRequest<T> countDistinctPlatform(){
        return countDistinctPlatformAs("platform_distinct_count");
    }

    public PurchaseOrderRequest<T> countDistinctPlatformAs(String alias){
        super.countDistinctProperty(alias, PurchaseOrder.PLATFORM_PROPERTY);
        return this;
    }
    public PurchaseOrderRequest<T> countDistinctSupplier(){
        return countDistinctSupplierAs("supplier_distinct_count");
    }

    public PurchaseOrderRequest<T> countDistinctSupplierAs(String alias){
        super.countDistinctProperty(alias, PurchaseOrder.SUPPLIER_PROPERTY);
        return this;
    }
    public PurchaseOrderRequest<T> countDistinctOrderNumber(){
        return countDistinctOrderNumberAs("order_number_distinct_count");
    }

    public PurchaseOrderRequest<T> countDistinctOrderNumberAs(String alias){
        super.countDistinctProperty(alias, PurchaseOrder.ORDER_NUMBER_PROPERTY);
        return this;
    }
    public PurchaseOrderRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public PurchaseOrderRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, PurchaseOrder.VERSION_PROPERTY);
        return this;
    }

    public PurchaseOrderRequest<T> groupById(){
       groupBy(PurchaseOrder.ID_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupByIdAs(String retName){
       groupBy(retName, PurchaseOrder.ID_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, PurchaseOrder.ID_PROPERTY, function);
       return this;
    }
    public PurchaseOrderRequest<T> groupByPlatformWith(PlatformRequest subRequest){
       groupBy(PurchaseOrder.PLATFORM_PROPERTY, subRequest,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "groupByPlatformWith", null, "platform"));
       return this;
    }
    public PurchaseOrderRequest<T> groupByPlatform(){
       groupBy(PurchaseOrder.PLATFORM_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupByPlatformAs(String retName){
       groupBy(retName, PurchaseOrder.PLATFORM_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupByPlatformWithFunction(String retName, AggrFunction function){
       groupBy(retName, PurchaseOrder.PLATFORM_PROPERTY, function);
       return this;
    }

    public PurchaseOrderRequest<T> groupBySupplier(){
       groupBy(PurchaseOrder.SUPPLIER_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupBySupplierAs(String retName){
       groupBy(retName, PurchaseOrder.SUPPLIER_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupBySupplierWithFunction(String retName, AggrFunction function){
       groupBy(retName, PurchaseOrder.SUPPLIER_PROPERTY, function);
       return this;
    }

    public PurchaseOrderRequest<T> groupByOrderNumber(){
       groupBy(PurchaseOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupByOrderNumberAs(String retName){
       groupBy(retName, PurchaseOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupByOrderNumberWithFunction(String retName, AggrFunction function){
       groupBy(retName, PurchaseOrder.ORDER_NUMBER_PROPERTY, function);
       return this;
    }

    public PurchaseOrderRequest<T> groupByVersion(){
       groupBy(PurchaseOrder.VERSION_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupByVersionAs(String retName){
       groupBy(retName, PurchaseOrder.VERSION_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, PurchaseOrder.VERSION_PROPERTY, function);
       return this;
    }



    public PurchaseOrderRequest<T> orderByIdAscending(){
       addOrderByAscending(PurchaseOrder.ID_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> orderByIdDescending(){
       addOrderByDescending(PurchaseOrder.ID_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> orderByPlatformAscending(){
       addOrderByAscending(PurchaseOrder.PLATFORM_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> orderByPlatformDescending(){
       addOrderByDescending(PurchaseOrder.PLATFORM_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> orderBySupplierAscending(){
       addOrderByAscending(PurchaseOrder.SUPPLIER_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> orderBySupplierDescending(){
       addOrderByDescending(PurchaseOrder.SUPPLIER_PROPERTY);
       return this;
    }
    public PurchaseOrderRequest<T> orderBySupplierAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(PurchaseOrder.SUPPLIER_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> orderBySupplierDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(PurchaseOrder.SUPPLIER_PROPERTY);
       return this;
    }
    public PurchaseOrderRequest<T> orderByOrderNumberAscending(){
       addOrderByAscending(PurchaseOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> orderByOrderNumberDescending(){
       addOrderByDescending(PurchaseOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }
    public PurchaseOrderRequest<T> orderByOrderNumberAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(PurchaseOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> orderByOrderNumberDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(PurchaseOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }
    public PurchaseOrderRequest<T> orderByVersionAscending(){
       addOrderByAscending(PurchaseOrder.VERSION_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> orderByVersionDescending(){
       addOrderByDescending(PurchaseOrder.VERSION_PROPERTY);
       return this;
    }


    public PlatformRequest rollUpToPlatform(){
       PlatformRequest platform = Q.platforms().unlimited();
       this.internalRollUpTo("platform", platform,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "rollUpToPlatform", null, "platform"));
       return platform;
    }





   public PurchaseOrderRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform){
       return facetByPlatformAs(facetName, platform, true);
   }

   public PurchaseOrderRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform, boolean includeAllFacets){
       addFacet(facetName, PurchaseOrder.PLATFORM_PROPERTY, platform, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByPlatformAs", facetName, PurchaseOrder.PLATFORM_PROPERTY));
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public PurchaseOrderRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public PurchaseOrderRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public PurchaseOrderRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public PurchaseOrderRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public PurchaseOrderRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public PurchaseOrderRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}