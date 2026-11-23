
package com.teaql.orderattachmentservice.customerorder;

import com.teaql.orderattachmentservice.Q;
import com.teaql.orderattachmentservice.commerceplatform.CommercePlatform;
import com.teaql.orderattachmentservice.commerceplatform.CommercePlatformRequest;
import com.teaql.orderattachmentservice.orderattachment.OrderAttachment;
import com.teaql.orderattachmentservice.orderattachment.OrderAttachmentRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.util.Objects;

public class CustomerOrderRequest<T extends CustomerOrder> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public CustomerOrderRequest(Class<T> returnType){
        this(returnType, () -> (T) new CustomerOrder());
    }

    protected CustomerOrderRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public CustomerOrderRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public CustomerOrderRequest<T> copy(){
        return (CustomerOrderRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new CustomerOrderRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public CustomerOrderRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public CustomerOrderRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public CustomerOrderRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public CustomerOrderRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public CustomerOrderRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public CustomerOrderRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public CustomerOrderRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public CustomerOrderRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public CustomerOrderRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public CustomerOrderRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (CustomerOrderRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public CustomerOrderRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public CustomerOrderRequest<T> matchingAnyOf(CustomerOrderRequest customerOrder){
        super.internalMatchAny(customerOrder);
        return this;
    }

    public CustomerOrderRequest<T> matchingIdsOf(CustomerOrderRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                CustomerOrder.ID_PROPERTY, request, CustomerOrder.ID_PROPERTY));
    }

    public CustomerOrderRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public CustomerOrderRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public CustomerOrderRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public CustomerOrderRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectOrderNumber().selectCommercePlatformIdOnly().selectVersion();
    }

    public CustomerOrderRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public CustomerOrderRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(CustomerOrder.ID_PROPERTY);
        selectProperty(CustomerOrder.ORDER_NUMBER_PROPERTY);
        selectProperty(CustomerOrder.COMMERCE_PLATFORM_PROPERTY);
        selectProperty(CustomerOrder.VERSION_PROPERTY);
        return this;
    }

    public CustomerOrderRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectOrderNumber().selectCommercePlatform().selectVersion();
    }

    public CustomerOrderRequest<T> selectChildren(){
        super.selectAny();
        selectOrderAttachmentList();
        return selectId().selectOrderNumber().selectCommercePlatform().selectVersion();
    }


    public CustomerOrderRequest<T> selectId(){
       selectProperty(CustomerOrder.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerOrderRequest<T> unselectId(){
       unselectProperty(CustomerOrder.ID_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> selectOrderNumber(){
       selectProperty(CustomerOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    /**
     * fill the orderNumber with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  orderNumber) to fetch orderNumber property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerOrderRequest<T> unselectOrderNumber(){
       unselectProperty(CustomerOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> selectCommercePlatformIdOnly(){
       selectProperty(CustomerOrder.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> selectCommercePlatform(){
        return selectCommercePlatformWith(Q.commercePlatforms().unlimited().selectSelf());
    }

    public CustomerOrderRequest<T> selectCommercePlatformWith(CommercePlatformRequest commercePlatform){
       selectProperty(CustomerOrder.COMMERCE_PLATFORM_PROPERTY);
       enhanceRelation(CustomerOrder.COMMERCE_PLATFORM_PROPERTY, commercePlatform,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectCommercePlatformWith", null, CustomerOrder.COMMERCE_PLATFORM_PROPERTY));
       return this;
    }

    public CustomerOrderRequest<T> unselectCommercePlatform(){
       unselectProperty(CustomerOrder.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> selectVersion(){
       selectProperty(CustomerOrder.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerOrderRequest<T> unselectVersion(){
       unselectProperty(CustomerOrder.VERSION_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> selectOrderAttachmentList(){
       return selectOrderAttachmentListWith(Q.orderAttachments().selectSelf());
    }

    public CustomerOrderRequest<T> selectOrderAttachmentListWith(OrderAttachmentRequest orderAttachmentList){
       enhanceRelation(CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY, orderAttachmentList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectOrderAttachmentListWith", null, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
       return this;
    }

    public CustomerOrderRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.ID_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public CustomerOrderRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public CustomerOrderRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public CustomerOrderRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public CustomerOrderRequest<T> filterByOrderNumber(String... orderNumber){
      if (orderNumber == null || orderNumber.length == 0) {
        throw new IllegalArgumentException("filterByOrderNumber parameter orderNumber cannot be empty");
      }
      return appendSearchCriteria(createOrderNumberCriteria(Operator.EQUAL, (Object[])orderNumber));
    }

    public CustomerOrderRequest<T> withOrderNumber(Operator operator, Object... values){
       return appendSearchCriteria(createOrderNumberCriteria(operator, values));
    }

    public CustomerOrderRequest<T> withOrderNumberIsUnknown(){
       return withOrderNumber(Operator.IS_NULL);
    }

    public CustomerOrderRequest<T> withOrderNumberIsKnown(){
       return withOrderNumber(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createOrderNumberCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.ORDER_NUMBER_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> withOrderNumberIsNot(String orderNumber){
       return withOrderNumber(Operator.NOT_EQUAL, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberIn(String... orderNumber){
       return withOrderNumber(Operator.IN, (Object[])orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberNotIn(String... orderNumber){
       return withOrderNumber(Operator.NOT_IN, (Object[])orderNumber);
    }
    public CustomerOrderRequest<T> withOrderNumberGreaterThan(String orderNumber){
       return withOrderNumber(Operator.GREATER_THAN, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberGreaterThanOrEqualTo(String orderNumber){
       return withOrderNumber(Operator.GREATER_THAN_OR_EQUAL, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberLessThan(String orderNumber){
       return withOrderNumber(Operator.LESS_THAN, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberLessThanOrEqualTo(String orderNumber){
       return withOrderNumber(Operator.LESS_THAN_OR_EQUAL, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberBetween(String startOfOrderNumber, String endOfOrderNumber){
       return withOrderNumber(Operator.BETWEEN, startOfOrderNumber, endOfOrderNumber);
    }
    public CustomerOrderRequest<T> withOrderNumberStartingWith(String orderNumber){
       return withOrderNumber(Operator.BEGIN_WITH, orderNumber);
    }
    public CustomerOrderRequest<T> withOrderNumberContaining(String orderNumber){
       return withOrderNumber(Operator.CONTAIN, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberNotContaining(String orderNumber){
       return withOrderNumber(Operator.NOT_CONTAIN, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberNotStartingWith(String orderNumber){
       return withOrderNumber(Operator.NOT_BEGIN_WITH, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberEndingWith(String orderNumber){
       return withOrderNumber(Operator.END_WITH, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberNotEndingWith(String orderNumber){
       return withOrderNumber(Operator.NOT_END_WITH, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberIs(String orderNumber){
       return withOrderNumber(Operator.EQUAL, orderNumber);
    }

    public CustomerOrderRequest<T> withOrderNumberSoundingLike(String orderNumber){
       return withOrderNumber(Operator.SOUNDS_LIKE, orderNumber);
    }



    public CustomerOrderRequest<T> filterByCommercePlatform(CommercePlatform... commercePlatform){
      if (commercePlatform == null || commercePlatform.length == 0) {
        throw new IllegalArgumentException("filterByCommercePlatform parameter commercePlatform cannot be empty");
      }
      return appendSearchCriteria(createCommercePlatformCriteria(Operator.EQUAL, (Object[])commercePlatform));
    }

    public CustomerOrderRequest<T> withCommercePlatform(Operator operator, Object... values){
       return appendSearchCriteria(createCommercePlatformCriteria(operator, values));
    }

    public CustomerOrderRequest<T> withCommercePlatformIsUnknown(){
       return withCommercePlatform(Operator.IS_NULL);
    }

    public CustomerOrderRequest<T> withCommercePlatformIsKnown(){
       return withCommercePlatform(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCommercePlatformCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.COMMERCE_PLATFORM_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> filterByCommercePlatform(Long commercePlatform){
      if(commercePlatform == null){
         return this;
      }
      return withCommercePlatform(Operator.EQUAL, commercePlatform);
    }
    public CustomerOrderRequest<T> withCommercePlatformMatching(CommercePlatformRequest commercePlatform){
       return appendSearchCriteria(new SubQuerySearchCriteria(CustomerOrder.COMMERCE_PLATFORM_PROPERTY, commercePlatform, CommercePlatform.ID_PROPERTY));
    }

    public CustomerOrderRequest<T> withoutCommercePlatformMatching(CommercePlatformRequest commercePlatform){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(CustomerOrder.COMMERCE_PLATFORM_PROPERTY, commercePlatform, CommercePlatform.ID_PROPERTY)));
    }

    public CustomerOrderRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public CustomerOrderRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public CustomerOrderRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public CustomerOrderRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(CustomerOrder.VERSION_PROPERTY, operator, values);
    }

    public CustomerOrderRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public CustomerOrderRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public CustomerOrderRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public CustomerOrderRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public CustomerOrderRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public CustomerOrderRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public CustomerOrderRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public CustomerOrderRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public CustomerOrderRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public CustomerOrderRequest<T> withOrderAttachmentListMatching(OrderAttachmentRequest orderAttachmentRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(CustomerOrder.ID_PROPERTY, orderAttachmentRequest, OrderAttachment.CUSTOMER_ORDER_PROPERTY));
    }

    public CustomerOrderRequest<T> withoutOrderAttachmentListMatching(OrderAttachmentRequest orderAttachmentRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(CustomerOrder.ID_PROPERTY, orderAttachmentRequest, OrderAttachment.CUSTOMER_ORDER_PROPERTY)));
    }

    public CustomerOrderRequest<T> haveOrderAttachments(){
        return withOrderAttachmentListMatching(Q.orderAttachments().unlimited());
    }

    public CustomerOrderRequest<T> haveNoOrderAttachments(){
        return withoutOrderAttachmentListMatching(Q.orderAttachments().unlimited());
    }

    public CustomerOrderRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public CustomerOrderRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public CustomerOrderRequest<T> groupByCommercePlatformWithDetails(){
       return groupByCommercePlatformWithDetails(Q.commercePlatforms().unlimited());
    }

    public CustomerOrderRequest<T> groupByCommercePlatformWithDetails(CommercePlatformRequest subRequest){
       aggregate(CustomerOrder.COMMERCE_PLATFORM_PROPERTY, subRequest);
       return this;
    }


    public CustomerOrderRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public CustomerOrderRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, CustomerOrder.ID_PROPERTY);
        return this;
    }
    public CustomerOrderRequest<T> countDistinctOrderNumber(){
        return countDistinctOrderNumberAs("order_number_distinct_count");
    }

    public CustomerOrderRequest<T> countDistinctOrderNumberAs(String alias){
        super.countDistinctProperty(alias, CustomerOrder.ORDER_NUMBER_PROPERTY);
        return this;
    }
    public CustomerOrderRequest<T> countDistinctCommercePlatform(){
        return countDistinctCommercePlatformAs("commerce_platform_distinct_count");
    }

    public CustomerOrderRequest<T> countDistinctCommercePlatformAs(String alias){
        super.countDistinctProperty(alias, CustomerOrder.COMMERCE_PLATFORM_PROPERTY);
        return this;
    }
    public CustomerOrderRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public CustomerOrderRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, CustomerOrder.VERSION_PROPERTY);
        return this;
    }
    public CustomerOrderRequest<T> groupByOrderAttachmentsWithDetails(OrderAttachmentRequest subRequest){
       aggregate(CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY, subRequest);
       return this;
    }

    public CustomerOrderRequest<T> groupById(){
       groupBy(CustomerOrder.ID_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByIdAs(String retName){
       groupBy(retName, CustomerOrder.ID_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.ID_PROPERTY, function);
       return this;
    }

    public CustomerOrderRequest<T> groupByOrderNumber(){
       groupBy(CustomerOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByOrderNumberAs(String retName){
       groupBy(retName, CustomerOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByOrderNumberWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.ORDER_NUMBER_PROPERTY, function);
       return this;
    }
    public CustomerOrderRequest<T> groupByCommercePlatformWith(CommercePlatformRequest subRequest){
       groupBy(CustomerOrder.COMMERCE_PLATFORM_PROPERTY, subRequest,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "groupByCommercePlatformWith", null, "commerce_platform"));
       return this;
    }
    public CustomerOrderRequest<T> groupByCommercePlatform(){
       groupBy(CustomerOrder.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByCommercePlatformAs(String retName){
       groupBy(retName, CustomerOrder.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByCommercePlatformWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.COMMERCE_PLATFORM_PROPERTY, function);
       return this;
    }

    public CustomerOrderRequest<T> groupByVersion(){
       groupBy(CustomerOrder.VERSION_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByVersionAs(String retName){
       groupBy(retName, CustomerOrder.VERSION_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, CustomerOrder.VERSION_PROPERTY, function);
       return this;
    }



    public CustomerOrderRequest<T> orderByIdAscending(){
       addOrderByAscending(CustomerOrder.ID_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByIdDescending(){
       addOrderByDescending(CustomerOrder.ID_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByOrderNumberAscending(){
       addOrderByAscending(CustomerOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByOrderNumberDescending(){
       addOrderByDescending(CustomerOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> orderByOrderNumberAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(CustomerOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByOrderNumberDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(CustomerOrder.ORDER_NUMBER_PROPERTY);
       return this;
    }
    public CustomerOrderRequest<T> orderByCommercePlatformAscending(){
       addOrderByAscending(CustomerOrder.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByCommercePlatformDescending(){
       addOrderByDescending(CustomerOrder.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByVersionAscending(){
       addOrderByAscending(CustomerOrder.VERSION_PROPERTY);
       return this;
    }

    public CustomerOrderRequest<T> orderByVersionDescending(){
       addOrderByDescending(CustomerOrder.VERSION_PROPERTY);
       return this;
    }


    public CustomerOrderRequest<T> statsFromOrderAttachmentsAs(String name, OrderAttachmentRequest subRequest){
       return statsFromOrderAttachmentsAs(name, subRequest, false);
    }

    public CustomerOrderRequest<T> statsFromOrderAttachmentsAs(String name, OrderAttachmentRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, OrderAttachment.CUSTOMER_ORDER_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromOrderAttachmentsAs", name, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
       return this;
    }

    public CustomerOrderRequest<T> statsFromOrderAttachments(OrderAttachmentRequest subRequest){
       return statsFromOrderAttachmentsAs(REFINEMENTS, subRequest);
    }
    public CommercePlatformRequest rollUpToCommercePlatform(){
       CommercePlatformRequest commercePlatform = Q.commercePlatforms().unlimited();
       this.internalRollUpTo("commerce_platform", commercePlatform,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "rollUpToCommercePlatform", null, "commerce_platform"));
       return commercePlatform;
    }


    public CustomerOrderRequest<T> countOrderAttachments(){
        addRelationCount("Count", Q.orderAttachments().unlimited(), OrderAttachment.CUSTOMER_ORDER_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderAttachments", null, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> countOrderAttachmentsAs(String name){
        addRelationCount(name, Q.orderAttachments().unlimited(), OrderAttachment.CUSTOMER_ORDER_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderAttachmentsAs", name, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> countOrderAttachmentsWith(String name, OrderAttachmentRequest subRequest){
        addRelationCount(name, subRequest, OrderAttachment.CUSTOMER_ORDER_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderAttachmentsWith", name, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> minContentLengthOfOrderAttachments(){
        addRelationScalarAggregate("minContentLengthOfOrderAttachments", Q.orderAttachments().unlimited(),
            OrderAttachment.CUSTOMER_ORDER_PROPERTY, OrderAttachment.CONTENT_LENGTH_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minContentLengthOfOrderAttachments", null, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> minContentLengthOfOrderAttachmentsAs(String name){
        return minContentLengthOfOrderAttachmentsAs(name, Q.orderAttachments().unlimited());
    }

    public CustomerOrderRequest<T> minContentLengthOfOrderAttachmentsAs(String name, OrderAttachmentRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderAttachment.CUSTOMER_ORDER_PROPERTY,
            OrderAttachment.CONTENT_LENGTH_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minContentLengthOfOrderAttachmentsAs", name, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> maxContentLengthOfOrderAttachments(){
        addRelationScalarAggregate("maxContentLengthOfOrderAttachments", Q.orderAttachments().unlimited(),
            OrderAttachment.CUSTOMER_ORDER_PROPERTY, OrderAttachment.CONTENT_LENGTH_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxContentLengthOfOrderAttachments", null, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> maxContentLengthOfOrderAttachmentsAs(String name){
        return maxContentLengthOfOrderAttachmentsAs(name, Q.orderAttachments().unlimited());
    }

    public CustomerOrderRequest<T> maxContentLengthOfOrderAttachmentsAs(String name, OrderAttachmentRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderAttachment.CUSTOMER_ORDER_PROPERTY,
            OrderAttachment.CONTENT_LENGTH_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxContentLengthOfOrderAttachmentsAs", name, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> sumContentLengthOfOrderAttachments(){
        addRelationScalarAggregate("sumContentLengthOfOrderAttachments", Q.orderAttachments().unlimited(),
            OrderAttachment.CUSTOMER_ORDER_PROPERTY, OrderAttachment.CONTENT_LENGTH_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumContentLengthOfOrderAttachments", null, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> sumContentLengthOfOrderAttachmentsAs(String name){
        return sumContentLengthOfOrderAttachmentsAs(name, Q.orderAttachments().unlimited());
    }

    public CustomerOrderRequest<T> sumContentLengthOfOrderAttachmentsAs(String name, OrderAttachmentRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderAttachment.CUSTOMER_ORDER_PROPERTY,
            OrderAttachment.CONTENT_LENGTH_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumContentLengthOfOrderAttachmentsAs", name, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> avgContentLengthOfOrderAttachments(){
        addRelationScalarAggregate("avgContentLengthOfOrderAttachments", Q.orderAttachments().unlimited(),
            OrderAttachment.CUSTOMER_ORDER_PROPERTY, OrderAttachment.CONTENT_LENGTH_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgContentLengthOfOrderAttachments", null, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> avgContentLengthOfOrderAttachmentsAs(String name){
        return avgContentLengthOfOrderAttachmentsAs(name, Q.orderAttachments().unlimited());
    }

    public CustomerOrderRequest<T> avgContentLengthOfOrderAttachmentsAs(String name, OrderAttachmentRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderAttachment.CUSTOMER_ORDER_PROPERTY,
            OrderAttachment.CONTENT_LENGTH_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgContentLengthOfOrderAttachmentsAs", name, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> standardDeviationContentLengthOfOrderAttachments(){
        addRelationScalarAggregate("stdDevContentLengthOfOrderAttachments", Q.orderAttachments().unlimited(),
            OrderAttachment.CUSTOMER_ORDER_PROPERTY, OrderAttachment.CONTENT_LENGTH_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationContentLengthOfOrderAttachments", null, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> standardDeviationContentLengthOfOrderAttachmentsAs(String name){
        return standardDeviationContentLengthOfOrderAttachmentsAs(name, Q.orderAttachments().unlimited());
    }

    public CustomerOrderRequest<T> standardDeviationContentLengthOfOrderAttachmentsAs(String name, OrderAttachmentRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderAttachment.CUSTOMER_ORDER_PROPERTY,
            OrderAttachment.CONTENT_LENGTH_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationContentLengthOfOrderAttachmentsAs", name, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> squareRootOfPopulationStandardDeviationContentLengthOfOrderAttachments(){
        addRelationScalarAggregate("stdDevPopContentLengthOfOrderAttachments", Q.orderAttachments().unlimited(),
            OrderAttachment.CUSTOMER_ORDER_PROPERTY, OrderAttachment.CONTENT_LENGTH_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationContentLengthOfOrderAttachments", null, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> squareRootOfPopulationStandardDeviationContentLengthOfOrderAttachmentsAs(String name){
        return squareRootOfPopulationStandardDeviationContentLengthOfOrderAttachmentsAs(name, Q.orderAttachments().unlimited());
    }

    public CustomerOrderRequest<T> squareRootOfPopulationStandardDeviationContentLengthOfOrderAttachmentsAs(String name, OrderAttachmentRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderAttachment.CUSTOMER_ORDER_PROPERTY,
            OrderAttachment.CONTENT_LENGTH_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationContentLengthOfOrderAttachmentsAs", name, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> sampleVarianceContentLengthOfOrderAttachments(){
        addRelationScalarAggregate("varSampContentLengthOfOrderAttachments", Q.orderAttachments().unlimited(),
            OrderAttachment.CUSTOMER_ORDER_PROPERTY, OrderAttachment.CONTENT_LENGTH_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceContentLengthOfOrderAttachments", null, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> sampleVarianceContentLengthOfOrderAttachmentsAs(String name){
        return sampleVarianceContentLengthOfOrderAttachmentsAs(name, Q.orderAttachments().unlimited());
    }

    public CustomerOrderRequest<T> sampleVarianceContentLengthOfOrderAttachmentsAs(String name, OrderAttachmentRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderAttachment.CUSTOMER_ORDER_PROPERTY,
            OrderAttachment.CONTENT_LENGTH_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceContentLengthOfOrderAttachmentsAs", name, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }
    public CustomerOrderRequest<T> samplePopulationVarianceContentLengthOfOrderAttachments(){
        addRelationScalarAggregate("varPopContentLengthOfOrderAttachments", Q.orderAttachments().unlimited(),
            OrderAttachment.CUSTOMER_ORDER_PROPERTY, OrderAttachment.CONTENT_LENGTH_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceContentLengthOfOrderAttachments", null, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }

    public CustomerOrderRequest<T> samplePopulationVarianceContentLengthOfOrderAttachmentsAs(String name){
        return samplePopulationVarianceContentLengthOfOrderAttachmentsAs(name, Q.orderAttachments().unlimited());
    }

    public CustomerOrderRequest<T> samplePopulationVarianceContentLengthOfOrderAttachmentsAs(String name, OrderAttachmentRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderAttachment.CUSTOMER_ORDER_PROPERTY,
            OrderAttachment.CONTENT_LENGTH_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceContentLengthOfOrderAttachmentsAs", name, CustomerOrder.ORDER_ATTACHMENT_LIST_PROPERTY));
        return this;
    }

   public CustomerOrderRequest<T> facetByCommercePlatformAs(String facetName, CommercePlatformRequest commercePlatform){
       return facetByCommercePlatformAs(facetName, commercePlatform, true);
   }

   public CustomerOrderRequest<T> facetByCommercePlatformAs(String facetName, CommercePlatformRequest commercePlatform, boolean includeAllFacets){
       addFacet(facetName, CustomerOrder.COMMERCE_PLATFORM_PROPERTY, commercePlatform, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByCommercePlatformAs", facetName, CustomerOrder.COMMERCE_PLATFORM_PROPERTY));
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public CustomerOrderRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public CustomerOrderRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public CustomerOrderRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public CustomerOrderRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public CustomerOrderRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public CustomerOrderRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}