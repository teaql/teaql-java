
package com.teaql.supplieronboardingservice.warehouse;

import com.teaql.supplieronboardingservice.Q;
import com.teaql.supplieronboardingservice.supplier.Supplier;
import com.teaql.supplieronboardingservice.supplier.SupplierRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.util.Objects;

public class WarehouseRequest<T extends Warehouse> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public WarehouseRequest(Class<T> returnType){
        this(returnType, () -> (T) new Warehouse());
    }

    protected WarehouseRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public WarehouseRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public WarehouseRequest<T> copy(){
        return (WarehouseRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new WarehouseRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public WarehouseRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public WarehouseRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public WarehouseRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public WarehouseRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public WarehouseRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public WarehouseRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public WarehouseRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public WarehouseRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public WarehouseRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public WarehouseRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (WarehouseRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public WarehouseRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public WarehouseRequest<T> matchingAnyOf(WarehouseRequest warehouse){
        super.internalMatchAny(warehouse);
        return this;
    }

    public WarehouseRequest<T> matchingIdsOf(WarehouseRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                Warehouse.ID_PROPERTY, request, Warehouse.ID_PROPERTY));
    }

    public WarehouseRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public WarehouseRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public WarehouseRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public WarehouseRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectName().selectVersion();
    }

    public WarehouseRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public WarehouseRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(Warehouse.ID_PROPERTY);
        selectProperty(Warehouse.NAME_PROPERTY);
        selectProperty(Warehouse.VERSION_PROPERTY);
        return this;
    }

    public WarehouseRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectName().selectVersion();
    }

    public WarehouseRequest<T> selectChildren(){
        super.selectAny();
        selectSupplierList();
        return selectId().selectName().selectVersion();
    }


    public WarehouseRequest<T> selectId(){
       selectProperty(Warehouse.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public WarehouseRequest<T> unselectId(){
       unselectProperty(Warehouse.ID_PROPERTY);
       return this;
    }
    public WarehouseRequest<T> selectName(){
       selectProperty(Warehouse.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public WarehouseRequest<T> unselectName(){
       unselectProperty(Warehouse.NAME_PROPERTY);
       return this;
    }
    public WarehouseRequest<T> selectVersion(){
       selectProperty(Warehouse.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public WarehouseRequest<T> unselectVersion(){
       unselectProperty(Warehouse.VERSION_PROPERTY);
       return this;
    }
    public WarehouseRequest<T> selectSupplierList(){
       return selectSupplierListWith(Q.suppliers().selectSelf());
    }

    public WarehouseRequest<T> selectSupplierListWith(SupplierRequest supplierList){
       enhanceRelation(Warehouse.SUPPLIER_LIST_PROPERTY, supplierList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectSupplierListWith", null, Warehouse.SUPPLIER_LIST_PROPERTY));
       return this;
    }

    public WarehouseRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Warehouse.ID_PROPERTY, operator, values);
    }

    public WarehouseRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public WarehouseRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public WarehouseRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public WarehouseRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public WarehouseRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public WarehouseRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public WarehouseRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public WarehouseRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Warehouse.NAME_PROPERTY, operator, values);
    }

    public WarehouseRequest<T> withNameIsNot(String name){
       return withName(Operator.NOT_EQUAL, name);
    }

    public WarehouseRequest<T> withNameIn(String... name){
       return withName(Operator.IN, (Object[])name);
    }

    public WarehouseRequest<T> withNameNotIn(String... name){
       return withName(Operator.NOT_IN, (Object[])name);
    }
    public WarehouseRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public WarehouseRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public WarehouseRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public WarehouseRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public WarehouseRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public WarehouseRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public WarehouseRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public WarehouseRequest<T> withNameNotContaining(String name){
       return withName(Operator.NOT_CONTAIN, name);
    }

    public WarehouseRequest<T> withNameNotStartingWith(String name){
       return withName(Operator.NOT_BEGIN_WITH, name);
    }

    public WarehouseRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public WarehouseRequest<T> withNameNotEndingWith(String name){
       return withName(Operator.NOT_END_WITH, name);
    }

    public WarehouseRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public WarehouseRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public WarehouseRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public WarehouseRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public WarehouseRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public WarehouseRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Warehouse.VERSION_PROPERTY, operator, values);
    }

    public WarehouseRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public WarehouseRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public WarehouseRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public WarehouseRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public WarehouseRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public WarehouseRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public WarehouseRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public WarehouseRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public WarehouseRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public WarehouseRequest<T> withSupplierListMatching(SupplierRequest supplierRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(Warehouse.ID_PROPERTY, supplierRequest, Supplier.WAREHOUSE_PROPERTY));
    }

    public WarehouseRequest<T> withoutSupplierListMatching(SupplierRequest supplierRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(Warehouse.ID_PROPERTY, supplierRequest, Supplier.WAREHOUSE_PROPERTY)));
    }

    public WarehouseRequest<T> haveSuppliers(){
        return withSupplierListMatching(Q.suppliers().unlimited());
    }

    public WarehouseRequest<T> haveNoSuppliers(){
        return withoutSupplierListMatching(Q.suppliers().unlimited());
    }

    public WarehouseRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public WarehouseRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public WarehouseRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public WarehouseRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, Warehouse.ID_PROPERTY);
        return this;
    }
    public WarehouseRequest<T> countDistinctName(){
        return countDistinctNameAs("name_distinct_count");
    }

    public WarehouseRequest<T> countDistinctNameAs(String alias){
        super.countDistinctProperty(alias, Warehouse.NAME_PROPERTY);
        return this;
    }
    public WarehouseRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public WarehouseRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, Warehouse.VERSION_PROPERTY);
        return this;
    }
    public WarehouseRequest<T> groupBySuppliersWithDetails(SupplierRequest subRequest){
       aggregate(Warehouse.SUPPLIER_LIST_PROPERTY, subRequest);
       return this;
    }

    public WarehouseRequest<T> groupById(){
       groupBy(Warehouse.ID_PROPERTY);
       return this;
    }

    public WarehouseRequest<T> groupByIdAs(String retName){
       groupBy(retName, Warehouse.ID_PROPERTY);
       return this;
    }

    public WarehouseRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, Warehouse.ID_PROPERTY, function);
       return this;
    }

    public WarehouseRequest<T> groupByName(){
       groupBy(Warehouse.NAME_PROPERTY);
       return this;
    }

    public WarehouseRequest<T> groupByNameAs(String retName){
       groupBy(retName, Warehouse.NAME_PROPERTY);
       return this;
    }

    public WarehouseRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, Warehouse.NAME_PROPERTY, function);
       return this;
    }

    public WarehouseRequest<T> groupByVersion(){
       groupBy(Warehouse.VERSION_PROPERTY);
       return this;
    }

    public WarehouseRequest<T> groupByVersionAs(String retName){
       groupBy(retName, Warehouse.VERSION_PROPERTY);
       return this;
    }

    public WarehouseRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, Warehouse.VERSION_PROPERTY, function);
       return this;
    }



    public WarehouseRequest<T> orderByIdAscending(){
       addOrderByAscending(Warehouse.ID_PROPERTY);
       return this;
    }

    public WarehouseRequest<T> orderByIdDescending(){
       addOrderByDescending(Warehouse.ID_PROPERTY);
       return this;
    }

    public WarehouseRequest<T> orderByNameAscending(){
       addOrderByAscending(Warehouse.NAME_PROPERTY);
       return this;
    }

    public WarehouseRequest<T> orderByNameDescending(){
       addOrderByDescending(Warehouse.NAME_PROPERTY);
       return this;
    }
    public WarehouseRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Warehouse.NAME_PROPERTY);
       return this;
    }

    public WarehouseRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Warehouse.NAME_PROPERTY);
       return this;
    }
    public WarehouseRequest<T> orderByVersionAscending(){
       addOrderByAscending(Warehouse.VERSION_PROPERTY);
       return this;
    }

    public WarehouseRequest<T> orderByVersionDescending(){
       addOrderByDescending(Warehouse.VERSION_PROPERTY);
       return this;
    }


    public WarehouseRequest<T> statsFromSuppliersAs(String name, SupplierRequest subRequest){
       return statsFromSuppliersAs(name, subRequest, false);
    }

    public WarehouseRequest<T> statsFromSuppliersAs(String name, SupplierRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, Supplier.WAREHOUSE_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromSuppliersAs", name, Warehouse.SUPPLIER_LIST_PROPERTY));
       return this;
    }

    public WarehouseRequest<T> statsFromSuppliers(SupplierRequest subRequest){
       return statsFromSuppliersAs(REFINEMENTS, subRequest);
    }
    public WarehouseRequest<T> countSuppliers(){
        addRelationCount("Count", Q.suppliers().unlimited(), Supplier.WAREHOUSE_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countSuppliers", null, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }

    public WarehouseRequest<T> countSuppliersAs(String name){
        addRelationCount(name, Q.suppliers().unlimited(), Supplier.WAREHOUSE_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countSuppliersAs", name, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }

    public WarehouseRequest<T> countSuppliersWith(String name, SupplierRequest subRequest){
        addRelationCount(name, subRequest, Supplier.WAREHOUSE_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countSuppliersWith", name, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }
    public WarehouseRequest<T> minMinimumOrderQuantityOfSuppliers(){
        addRelationScalarAggregate("minMinimumOrderQuantityOfSuppliers", Q.suppliers().unlimited(),
            Supplier.WAREHOUSE_PROPERTY, Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minMinimumOrderQuantityOfSuppliers", null, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }

    public WarehouseRequest<T> minMinimumOrderQuantityOfSuppliersAs(String name){
        return minMinimumOrderQuantityOfSuppliersAs(name, Q.suppliers().unlimited());
    }

    public WarehouseRequest<T> minMinimumOrderQuantityOfSuppliersAs(String name, SupplierRequest subRequest){
        addRelationScalarAggregate(name, subRequest, Supplier.WAREHOUSE_PROPERTY,
            Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minMinimumOrderQuantityOfSuppliersAs", name, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }
    public WarehouseRequest<T> maxMinimumOrderQuantityOfSuppliers(){
        addRelationScalarAggregate("maxMinimumOrderQuantityOfSuppliers", Q.suppliers().unlimited(),
            Supplier.WAREHOUSE_PROPERTY, Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxMinimumOrderQuantityOfSuppliers", null, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }

    public WarehouseRequest<T> maxMinimumOrderQuantityOfSuppliersAs(String name){
        return maxMinimumOrderQuantityOfSuppliersAs(name, Q.suppliers().unlimited());
    }

    public WarehouseRequest<T> maxMinimumOrderQuantityOfSuppliersAs(String name, SupplierRequest subRequest){
        addRelationScalarAggregate(name, subRequest, Supplier.WAREHOUSE_PROPERTY,
            Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxMinimumOrderQuantityOfSuppliersAs", name, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }
    public WarehouseRequest<T> sumMinimumOrderQuantityOfSuppliers(){
        addRelationScalarAggregate("sumMinimumOrderQuantityOfSuppliers", Q.suppliers().unlimited(),
            Supplier.WAREHOUSE_PROPERTY, Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumMinimumOrderQuantityOfSuppliers", null, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }

    public WarehouseRequest<T> sumMinimumOrderQuantityOfSuppliersAs(String name){
        return sumMinimumOrderQuantityOfSuppliersAs(name, Q.suppliers().unlimited());
    }

    public WarehouseRequest<T> sumMinimumOrderQuantityOfSuppliersAs(String name, SupplierRequest subRequest){
        addRelationScalarAggregate(name, subRequest, Supplier.WAREHOUSE_PROPERTY,
            Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumMinimumOrderQuantityOfSuppliersAs", name, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }
    public WarehouseRequest<T> avgMinimumOrderQuantityOfSuppliers(){
        addRelationScalarAggregate("avgMinimumOrderQuantityOfSuppliers", Q.suppliers().unlimited(),
            Supplier.WAREHOUSE_PROPERTY, Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgMinimumOrderQuantityOfSuppliers", null, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }

    public WarehouseRequest<T> avgMinimumOrderQuantityOfSuppliersAs(String name){
        return avgMinimumOrderQuantityOfSuppliersAs(name, Q.suppliers().unlimited());
    }

    public WarehouseRequest<T> avgMinimumOrderQuantityOfSuppliersAs(String name, SupplierRequest subRequest){
        addRelationScalarAggregate(name, subRequest, Supplier.WAREHOUSE_PROPERTY,
            Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgMinimumOrderQuantityOfSuppliersAs", name, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }
    public WarehouseRequest<T> standardDeviationMinimumOrderQuantityOfSuppliers(){
        addRelationScalarAggregate("stdDevMinimumOrderQuantityOfSuppliers", Q.suppliers().unlimited(),
            Supplier.WAREHOUSE_PROPERTY, Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationMinimumOrderQuantityOfSuppliers", null, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }

    public WarehouseRequest<T> standardDeviationMinimumOrderQuantityOfSuppliersAs(String name){
        return standardDeviationMinimumOrderQuantityOfSuppliersAs(name, Q.suppliers().unlimited());
    }

    public WarehouseRequest<T> standardDeviationMinimumOrderQuantityOfSuppliersAs(String name, SupplierRequest subRequest){
        addRelationScalarAggregate(name, subRequest, Supplier.WAREHOUSE_PROPERTY,
            Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationMinimumOrderQuantityOfSuppliersAs", name, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }
    public WarehouseRequest<T> squareRootOfPopulationStandardDeviationMinimumOrderQuantityOfSuppliers(){
        addRelationScalarAggregate("stdDevPopMinimumOrderQuantityOfSuppliers", Q.suppliers().unlimited(),
            Supplier.WAREHOUSE_PROPERTY, Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationMinimumOrderQuantityOfSuppliers", null, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }

    public WarehouseRequest<T> squareRootOfPopulationStandardDeviationMinimumOrderQuantityOfSuppliersAs(String name){
        return squareRootOfPopulationStandardDeviationMinimumOrderQuantityOfSuppliersAs(name, Q.suppliers().unlimited());
    }

    public WarehouseRequest<T> squareRootOfPopulationStandardDeviationMinimumOrderQuantityOfSuppliersAs(String name, SupplierRequest subRequest){
        addRelationScalarAggregate(name, subRequest, Supplier.WAREHOUSE_PROPERTY,
            Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationMinimumOrderQuantityOfSuppliersAs", name, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }
    public WarehouseRequest<T> sampleVarianceMinimumOrderQuantityOfSuppliers(){
        addRelationScalarAggregate("varSampMinimumOrderQuantityOfSuppliers", Q.suppliers().unlimited(),
            Supplier.WAREHOUSE_PROPERTY, Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceMinimumOrderQuantityOfSuppliers", null, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }

    public WarehouseRequest<T> sampleVarianceMinimumOrderQuantityOfSuppliersAs(String name){
        return sampleVarianceMinimumOrderQuantityOfSuppliersAs(name, Q.suppliers().unlimited());
    }

    public WarehouseRequest<T> sampleVarianceMinimumOrderQuantityOfSuppliersAs(String name, SupplierRequest subRequest){
        addRelationScalarAggregate(name, subRequest, Supplier.WAREHOUSE_PROPERTY,
            Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceMinimumOrderQuantityOfSuppliersAs", name, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }
    public WarehouseRequest<T> samplePopulationVarianceMinimumOrderQuantityOfSuppliers(){
        addRelationScalarAggregate("varPopMinimumOrderQuantityOfSuppliers", Q.suppliers().unlimited(),
            Supplier.WAREHOUSE_PROPERTY, Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceMinimumOrderQuantityOfSuppliers", null, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }

    public WarehouseRequest<T> samplePopulationVarianceMinimumOrderQuantityOfSuppliersAs(String name){
        return samplePopulationVarianceMinimumOrderQuantityOfSuppliersAs(name, Q.suppliers().unlimited());
    }

    public WarehouseRequest<T> samplePopulationVarianceMinimumOrderQuantityOfSuppliersAs(String name, SupplierRequest subRequest){
        addRelationScalarAggregate(name, subRequest, Supplier.WAREHOUSE_PROPERTY,
            Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceMinimumOrderQuantityOfSuppliersAs", name, Warehouse.SUPPLIER_LIST_PROPERTY));
        return this;
    }



    /**
     * get topN records
     * @param topN  records number
     */
    public WarehouseRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public WarehouseRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public WarehouseRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public WarehouseRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public WarehouseRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public WarehouseRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}