
package com.teaql.supplierordersyncservice.supplier;

import com.teaql.supplierordersyncservice.Q;
import com.teaql.supplierordersyncservice.supplierorder.SupplierOrder;
import com.teaql.supplierordersyncservice.supplierorder.SupplierOrderRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.util.Objects;

public class SupplierRequest<T extends Supplier> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public SupplierRequest(Class<T> returnType){
        this(returnType, () -> (T) new Supplier());
    }

    protected SupplierRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public SupplierRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public SupplierRequest<T> copy(){
        return (SupplierRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new SupplierRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public SupplierRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public SupplierRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public SupplierRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public SupplierRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public SupplierRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public SupplierRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public SupplierRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public SupplierRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public SupplierRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public SupplierRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (SupplierRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public SupplierRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public SupplierRequest<T> matchingAnyOf(SupplierRequest supplier){
        super.internalMatchAny(supplier);
        return this;
    }

    public SupplierRequest<T> matchingIdsOf(SupplierRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                Supplier.ID_PROPERTY, request, Supplier.ID_PROPERTY));
    }

    public SupplierRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public SupplierRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public SupplierRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public SupplierRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectName().selectVersion();
    }

    public SupplierRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public SupplierRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(Supplier.ID_PROPERTY);
        selectProperty(Supplier.NAME_PROPERTY);
        selectProperty(Supplier.VERSION_PROPERTY);
        return this;
    }

    public SupplierRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectName().selectVersion();
    }

    public SupplierRequest<T> selectChildren(){
        super.selectAny();
        selectSupplierOrderList();
        return selectId().selectName().selectVersion();
    }


    public SupplierRequest<T> selectId(){
       selectProperty(Supplier.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SupplierRequest<T> unselectId(){
       unselectProperty(Supplier.ID_PROPERTY);
       return this;
    }
    public SupplierRequest<T> selectName(){
       selectProperty(Supplier.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SupplierRequest<T> unselectName(){
       unselectProperty(Supplier.NAME_PROPERTY);
       return this;
    }
    public SupplierRequest<T> selectVersion(){
       selectProperty(Supplier.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SupplierRequest<T> unselectVersion(){
       unselectProperty(Supplier.VERSION_PROPERTY);
       return this;
    }
    public SupplierRequest<T> selectSupplierOrderList(){
       return selectSupplierOrderListWith(Q.supplierOrders().selectSelf());
    }

    public SupplierRequest<T> selectSupplierOrderListWith(SupplierOrderRequest supplierOrderList){
       enhanceRelation(Supplier.SUPPLIER_ORDER_LIST_PROPERTY, supplierOrderList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectSupplierOrderListWith", null, Supplier.SUPPLIER_ORDER_LIST_PROPERTY));
       return this;
    }

    public SupplierRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Supplier.ID_PROPERTY, operator, values);
    }

    public SupplierRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public SupplierRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public SupplierRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public SupplierRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public SupplierRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public SupplierRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public SupplierRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public SupplierRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Supplier.NAME_PROPERTY, operator, values);
    }

    public SupplierRequest<T> withNameIsNot(String name){
       return withName(Operator.NOT_EQUAL, name);
    }

    public SupplierRequest<T> withNameIn(String... name){
       return withName(Operator.IN, (Object[])name);
    }

    public SupplierRequest<T> withNameNotIn(String... name){
       return withName(Operator.NOT_IN, (Object[])name);
    }
    public SupplierRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public SupplierRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public SupplierRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public SupplierRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public SupplierRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public SupplierRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public SupplierRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public SupplierRequest<T> withNameNotContaining(String name){
       return withName(Operator.NOT_CONTAIN, name);
    }

    public SupplierRequest<T> withNameNotStartingWith(String name){
       return withName(Operator.NOT_BEGIN_WITH, name);
    }

    public SupplierRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public SupplierRequest<T> withNameNotEndingWith(String name){
       return withName(Operator.NOT_END_WITH, name);
    }

    public SupplierRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public SupplierRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public SupplierRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public SupplierRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public SupplierRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public SupplierRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Supplier.VERSION_PROPERTY, operator, values);
    }

    public SupplierRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public SupplierRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public SupplierRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public SupplierRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public SupplierRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public SupplierRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public SupplierRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public SupplierRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public SupplierRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public SupplierRequest<T> withSupplierOrderListMatching(SupplierOrderRequest supplierOrderRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(Supplier.ID_PROPERTY, supplierOrderRequest, SupplierOrder.SUPPLIER_PROPERTY));
    }

    public SupplierRequest<T> withoutSupplierOrderListMatching(SupplierOrderRequest supplierOrderRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(Supplier.ID_PROPERTY, supplierOrderRequest, SupplierOrder.SUPPLIER_PROPERTY)));
    }

    public SupplierRequest<T> haveSupplierOrders(){
        return withSupplierOrderListMatching(Q.supplierOrders().unlimited());
    }

    public SupplierRequest<T> haveNoSupplierOrders(){
        return withoutSupplierOrderListMatching(Q.supplierOrders().unlimited());
    }

    public SupplierRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public SupplierRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public SupplierRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public SupplierRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, Supplier.ID_PROPERTY);
        return this;
    }
    public SupplierRequest<T> countDistinctName(){
        return countDistinctNameAs("name_distinct_count");
    }

    public SupplierRequest<T> countDistinctNameAs(String alias){
        super.countDistinctProperty(alias, Supplier.NAME_PROPERTY);
        return this;
    }
    public SupplierRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public SupplierRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, Supplier.VERSION_PROPERTY);
        return this;
    }
    public SupplierRequest<T> groupBySupplierOrdersWithDetails(SupplierOrderRequest subRequest){
       aggregate(Supplier.SUPPLIER_ORDER_LIST_PROPERTY, subRequest);
       return this;
    }

    public SupplierRequest<T> groupById(){
       groupBy(Supplier.ID_PROPERTY);
       return this;
    }

    public SupplierRequest<T> groupByIdAs(String retName){
       groupBy(retName, Supplier.ID_PROPERTY);
       return this;
    }

    public SupplierRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, Supplier.ID_PROPERTY, function);
       return this;
    }

    public SupplierRequest<T> groupByName(){
       groupBy(Supplier.NAME_PROPERTY);
       return this;
    }

    public SupplierRequest<T> groupByNameAs(String retName){
       groupBy(retName, Supplier.NAME_PROPERTY);
       return this;
    }

    public SupplierRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, Supplier.NAME_PROPERTY, function);
       return this;
    }

    public SupplierRequest<T> groupByVersion(){
       groupBy(Supplier.VERSION_PROPERTY);
       return this;
    }

    public SupplierRequest<T> groupByVersionAs(String retName){
       groupBy(retName, Supplier.VERSION_PROPERTY);
       return this;
    }

    public SupplierRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, Supplier.VERSION_PROPERTY, function);
       return this;
    }



    public SupplierRequest<T> orderByIdAscending(){
       addOrderByAscending(Supplier.ID_PROPERTY);
       return this;
    }

    public SupplierRequest<T> orderByIdDescending(){
       addOrderByDescending(Supplier.ID_PROPERTY);
       return this;
    }

    public SupplierRequest<T> orderByNameAscending(){
       addOrderByAscending(Supplier.NAME_PROPERTY);
       return this;
    }

    public SupplierRequest<T> orderByNameDescending(){
       addOrderByDescending(Supplier.NAME_PROPERTY);
       return this;
    }
    public SupplierRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Supplier.NAME_PROPERTY);
       return this;
    }

    public SupplierRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Supplier.NAME_PROPERTY);
       return this;
    }
    public SupplierRequest<T> orderByVersionAscending(){
       addOrderByAscending(Supplier.VERSION_PROPERTY);
       return this;
    }

    public SupplierRequest<T> orderByVersionDescending(){
       addOrderByDescending(Supplier.VERSION_PROPERTY);
       return this;
    }


    public SupplierRequest<T> statsFromSupplierOrdersAs(String name, SupplierOrderRequest subRequest){
       return statsFromSupplierOrdersAs(name, subRequest, false);
    }

    public SupplierRequest<T> statsFromSupplierOrdersAs(String name, SupplierOrderRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, SupplierOrder.SUPPLIER_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromSupplierOrdersAs", name, Supplier.SUPPLIER_ORDER_LIST_PROPERTY));
       return this;
    }

    public SupplierRequest<T> statsFromSupplierOrders(SupplierOrderRequest subRequest){
       return statsFromSupplierOrdersAs(REFINEMENTS, subRequest);
    }
    public SupplierRequest<T> countSupplierOrders(){
        addRelationCount("Count", Q.supplierOrders().unlimited(), SupplierOrder.SUPPLIER_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countSupplierOrders", null, Supplier.SUPPLIER_ORDER_LIST_PROPERTY));
        return this;
    }

    public SupplierRequest<T> countSupplierOrdersAs(String name){
        addRelationCount(name, Q.supplierOrders().unlimited(), SupplierOrder.SUPPLIER_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countSupplierOrdersAs", name, Supplier.SUPPLIER_ORDER_LIST_PROPERTY));
        return this;
    }

    public SupplierRequest<T> countSupplierOrdersWith(String name, SupplierOrderRequest subRequest){
        addRelationCount(name, subRequest, SupplierOrder.SUPPLIER_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countSupplierOrdersWith", name, Supplier.SUPPLIER_ORDER_LIST_PROPERTY));
        return this;
    }



    /**
     * get topN records
     * @param topN  records number
     */
    public SupplierRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public SupplierRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public SupplierRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public SupplierRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public SupplierRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public SupplierRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}