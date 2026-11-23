
package com.teaql.orderattachmentservice.orderattachment;

import com.teaql.orderattachmentservice.Q;
import com.teaql.orderattachmentservice.customerorder.CustomerOrder;
import com.teaql.orderattachmentservice.customerorder.CustomerOrderRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.util.Objects;

public class OrderAttachmentRequest<T extends OrderAttachment> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public OrderAttachmentRequest(Class<T> returnType){
        this(returnType, () -> (T) new OrderAttachment());
    }

    protected OrderAttachmentRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public OrderAttachmentRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    /** Independent query graph; ordinary roll-up handles remain live. */
    @Override
    @SuppressWarnings("unchecked")
    public OrderAttachmentRequest<T> copy(){
        return (OrderAttachmentRequest<T>) super.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected io.teaql.core.BaseRequest<T> internalCopyTarget(){
        return new OrderAttachmentRequest<>((Class<T>) returnType(), this::internalNewEntity);
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public OrderAttachmentRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public OrderAttachmentRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public OrderAttachmentRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public OrderAttachmentRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public OrderAttachmentRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public OrderAttachmentRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public OrderAttachmentRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public OrderAttachmentRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public OrderAttachmentRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public OrderAttachmentRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (OrderAttachmentRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public OrderAttachmentRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public OrderAttachmentRequest<T> matchingAnyOf(OrderAttachmentRequest orderAttachment){
        super.internalMatchAny(orderAttachment);
        return this;
    }

    public OrderAttachmentRequest<T> matchingIdsOf(OrderAttachmentRequest<?> request){
        Objects.requireNonNull(request, "matching request");
        return appendSearchCriteria(new SubQuerySearchCriteria(
                OrderAttachment.ID_PROPERTY, request, OrderAttachment.ID_PROPERTY));
    }

    public OrderAttachmentRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public OrderAttachmentRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public OrderAttachmentRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public OrderAttachmentRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectCustomerOrderIdOnly().selectFileName().selectContentType().selectContentLength().selectContentSha256().selectObjectKey().selectVersion();
    }

    public OrderAttachmentRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public OrderAttachmentRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(OrderAttachment.ID_PROPERTY);
        selectProperty(OrderAttachment.CUSTOMER_ORDER_PROPERTY);
        selectProperty(OrderAttachment.FILE_NAME_PROPERTY);
        selectProperty(OrderAttachment.CONTENT_TYPE_PROPERTY);
        selectProperty(OrderAttachment.CONTENT_LENGTH_PROPERTY);
        selectProperty(OrderAttachment.CONTENT_SHA256_PROPERTY);
        selectProperty(OrderAttachment.OBJECT_KEY_PROPERTY);
        selectProperty(OrderAttachment.VERSION_PROPERTY);
        return this;
    }

    public OrderAttachmentRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectCustomerOrder().selectFileName().selectContentType().selectContentLength().selectContentSha256().selectObjectKey().selectVersion();
    }

    public OrderAttachmentRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectCustomerOrder().selectFileName().selectContentType().selectContentLength().selectContentSha256().selectObjectKey().selectVersion();
    }


    public OrderAttachmentRequest<T> selectId(){
       selectProperty(OrderAttachment.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderAttachmentRequest<T> unselectId(){
       unselectProperty(OrderAttachment.ID_PROPERTY);
       return this;
    }
    public OrderAttachmentRequest<T> selectCustomerOrderIdOnly(){
       selectProperty(OrderAttachment.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> selectCustomerOrder(){
        return selectCustomerOrderWith(Q.customerOrders().unlimited().selectSelf());
    }

    public OrderAttachmentRequest<T> selectCustomerOrderWith(CustomerOrderRequest customerOrder){
       selectProperty(OrderAttachment.CUSTOMER_ORDER_PROPERTY);
       enhanceRelation(OrderAttachment.CUSTOMER_ORDER_PROPERTY, customerOrder,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectCustomerOrderWith", null, OrderAttachment.CUSTOMER_ORDER_PROPERTY));
       return this;
    }

    public OrderAttachmentRequest<T> unselectCustomerOrder(){
       unselectProperty(OrderAttachment.CUSTOMER_ORDER_PROPERTY);
       return this;
    }
    public OrderAttachmentRequest<T> selectFileName(){
       selectProperty(OrderAttachment.FILE_NAME_PROPERTY);
       return this;
    }

    /**
     * fill the fileName with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  fileName) to fetch fileName property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderAttachmentRequest<T> unselectFileName(){
       unselectProperty(OrderAttachment.FILE_NAME_PROPERTY);
       return this;
    }
    public OrderAttachmentRequest<T> selectContentType(){
       selectProperty(OrderAttachment.CONTENT_TYPE_PROPERTY);
       return this;
    }

    /**
     * fill the contentType with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  contentType) to fetch contentType property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderAttachmentRequest<T> unselectContentType(){
       unselectProperty(OrderAttachment.CONTENT_TYPE_PROPERTY);
       return this;
    }
    public OrderAttachmentRequest<T> selectContentLength(){
       selectProperty(OrderAttachment.CONTENT_LENGTH_PROPERTY);
       return this;
    }

    /**
     * fill the contentLength with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  contentLength) to fetch contentLength property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the contentLength with customized aggrFunction, TEAQL uses ({aggrFunction}(contentLength) AS contentLength to fetch contentLength property.
     * @param aggrFunction  aggrFunction
     */
    public OrderAttachmentRequest<T> selectContentLength(AggrFunction aggrFunction){
       selectProperty(OrderAttachment.CONTENT_LENGTH_PROPERTY, aggrFunction);
       return this;
    }


    public OrderAttachmentRequest<T> unselectContentLength(){
       unselectProperty(OrderAttachment.CONTENT_LENGTH_PROPERTY);
       return this;
    }
    public OrderAttachmentRequest<T> selectContentSha256(){
       selectProperty(OrderAttachment.CONTENT_SHA256_PROPERTY);
       return this;
    }

    /**
     * fill the contentSha256 with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  contentSha256) to fetch contentSha256 property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderAttachmentRequest<T> unselectContentSha256(){
       unselectProperty(OrderAttachment.CONTENT_SHA256_PROPERTY);
       return this;
    }
    public OrderAttachmentRequest<T> selectObjectKey(){
       selectProperty(OrderAttachment.OBJECT_KEY_PROPERTY);
       return this;
    }

    /**
     * fill the objectKey with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  objectKey) to fetch objectKey property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderAttachmentRequest<T> unselectObjectKey(){
       unselectProperty(OrderAttachment.OBJECT_KEY_PROPERTY);
       return this;
    }
    public OrderAttachmentRequest<T> selectVersion(){
       selectProperty(OrderAttachment.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderAttachmentRequest<T> unselectVersion(){
       unselectProperty(OrderAttachment.VERSION_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderAttachment.ID_PROPERTY, operator, values);
    }

    public OrderAttachmentRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public OrderAttachmentRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public OrderAttachmentRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public OrderAttachmentRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public OrderAttachmentRequest<T> filterByCustomerOrder(CustomerOrder... customerOrder){
      if (customerOrder == null || customerOrder.length == 0) {
        throw new IllegalArgumentException("filterByCustomerOrder parameter customerOrder cannot be empty");
      }
      return appendSearchCriteria(createCustomerOrderCriteria(Operator.EQUAL, (Object[])customerOrder));
    }

    public OrderAttachmentRequest<T> withCustomerOrder(Operator operator, Object... values){
       return appendSearchCriteria(createCustomerOrderCriteria(operator, values));
    }

    public OrderAttachmentRequest<T> withCustomerOrderIsUnknown(){
       return withCustomerOrder(Operator.IS_NULL);
    }

    public OrderAttachmentRequest<T> withCustomerOrderIsKnown(){
       return withCustomerOrder(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCustomerOrderCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderAttachment.CUSTOMER_ORDER_PROPERTY, operator, values);
    }

    public OrderAttachmentRequest<T> filterByCustomerOrder(Long customerOrder){
      if(customerOrder == null){
         return this;
      }
      return withCustomerOrder(Operator.EQUAL, customerOrder);
    }
    public OrderAttachmentRequest<T> withCustomerOrderMatching(CustomerOrderRequest customerOrder){
       return appendSearchCriteria(new SubQuerySearchCriteria(OrderAttachment.CUSTOMER_ORDER_PROPERTY, customerOrder, CustomerOrder.ID_PROPERTY));
    }

    public OrderAttachmentRequest<T> withoutCustomerOrderMatching(CustomerOrderRequest customerOrder){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(OrderAttachment.CUSTOMER_ORDER_PROPERTY, customerOrder, CustomerOrder.ID_PROPERTY)));
    }

    public OrderAttachmentRequest<T> filterByFileName(String... fileName){
      if (fileName == null || fileName.length == 0) {
        throw new IllegalArgumentException("filterByFileName parameter fileName cannot be empty");
      }
      return appendSearchCriteria(createFileNameCriteria(Operator.EQUAL, (Object[])fileName));
    }

    public OrderAttachmentRequest<T> withFileName(Operator operator, Object... values){
       return appendSearchCriteria(createFileNameCriteria(operator, values));
    }

    public OrderAttachmentRequest<T> withFileNameIsUnknown(){
       return withFileName(Operator.IS_NULL);
    }

    public OrderAttachmentRequest<T> withFileNameIsKnown(){
       return withFileName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createFileNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderAttachment.FILE_NAME_PROPERTY, operator, values);
    }

    public OrderAttachmentRequest<T> withFileNameIsNot(String fileName){
       return withFileName(Operator.NOT_EQUAL, fileName);
    }

    public OrderAttachmentRequest<T> withFileNameIn(String... fileName){
       return withFileName(Operator.IN, (Object[])fileName);
    }

    public OrderAttachmentRequest<T> withFileNameNotIn(String... fileName){
       return withFileName(Operator.NOT_IN, (Object[])fileName);
    }
    public OrderAttachmentRequest<T> withFileNameGreaterThan(String fileName){
       return withFileName(Operator.GREATER_THAN, fileName);
    }

    public OrderAttachmentRequest<T> withFileNameGreaterThanOrEqualTo(String fileName){
       return withFileName(Operator.GREATER_THAN_OR_EQUAL, fileName);
    }

    public OrderAttachmentRequest<T> withFileNameLessThan(String fileName){
       return withFileName(Operator.LESS_THAN, fileName);
    }

    public OrderAttachmentRequest<T> withFileNameLessThanOrEqualTo(String fileName){
       return withFileName(Operator.LESS_THAN_OR_EQUAL, fileName);
    }

    public OrderAttachmentRequest<T> withFileNameBetween(String startOfFileName, String endOfFileName){
       return withFileName(Operator.BETWEEN, startOfFileName, endOfFileName);
    }
    public OrderAttachmentRequest<T> withFileNameStartingWith(String fileName){
       return withFileName(Operator.BEGIN_WITH, fileName);
    }
    public OrderAttachmentRequest<T> withFileNameContaining(String fileName){
       return withFileName(Operator.CONTAIN, fileName);
    }

    public OrderAttachmentRequest<T> withFileNameNotContaining(String fileName){
       return withFileName(Operator.NOT_CONTAIN, fileName);
    }

    public OrderAttachmentRequest<T> withFileNameNotStartingWith(String fileName){
       return withFileName(Operator.NOT_BEGIN_WITH, fileName);
    }

    public OrderAttachmentRequest<T> withFileNameEndingWith(String fileName){
       return withFileName(Operator.END_WITH, fileName);
    }

    public OrderAttachmentRequest<T> withFileNameNotEndingWith(String fileName){
       return withFileName(Operator.NOT_END_WITH, fileName);
    }

    public OrderAttachmentRequest<T> withFileNameIs(String fileName){
       return withFileName(Operator.EQUAL, fileName);
    }

    public OrderAttachmentRequest<T> withFileNameSoundingLike(String fileName){
       return withFileName(Operator.SOUNDS_LIKE, fileName);
    }



    public OrderAttachmentRequest<T> filterByContentType(String... contentType){
      if (contentType == null || contentType.length == 0) {
        throw new IllegalArgumentException("filterByContentType parameter contentType cannot be empty");
      }
      return appendSearchCriteria(createContentTypeCriteria(Operator.EQUAL, (Object[])contentType));
    }

    public OrderAttachmentRequest<T> withContentType(Operator operator, Object... values){
       return appendSearchCriteria(createContentTypeCriteria(operator, values));
    }

    public OrderAttachmentRequest<T> withContentTypeIsUnknown(){
       return withContentType(Operator.IS_NULL);
    }

    public OrderAttachmentRequest<T> withContentTypeIsKnown(){
       return withContentType(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createContentTypeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderAttachment.CONTENT_TYPE_PROPERTY, operator, values);
    }

    public OrderAttachmentRequest<T> withContentTypeIsNot(String contentType){
       return withContentType(Operator.NOT_EQUAL, contentType);
    }

    public OrderAttachmentRequest<T> withContentTypeIn(String... contentType){
       return withContentType(Operator.IN, (Object[])contentType);
    }

    public OrderAttachmentRequest<T> withContentTypeNotIn(String... contentType){
       return withContentType(Operator.NOT_IN, (Object[])contentType);
    }
    public OrderAttachmentRequest<T> withContentTypeGreaterThan(String contentType){
       return withContentType(Operator.GREATER_THAN, contentType);
    }

    public OrderAttachmentRequest<T> withContentTypeGreaterThanOrEqualTo(String contentType){
       return withContentType(Operator.GREATER_THAN_OR_EQUAL, contentType);
    }

    public OrderAttachmentRequest<T> withContentTypeLessThan(String contentType){
       return withContentType(Operator.LESS_THAN, contentType);
    }

    public OrderAttachmentRequest<T> withContentTypeLessThanOrEqualTo(String contentType){
       return withContentType(Operator.LESS_THAN_OR_EQUAL, contentType);
    }

    public OrderAttachmentRequest<T> withContentTypeBetween(String startOfContentType, String endOfContentType){
       return withContentType(Operator.BETWEEN, startOfContentType, endOfContentType);
    }
    public OrderAttachmentRequest<T> withContentTypeStartingWith(String contentType){
       return withContentType(Operator.BEGIN_WITH, contentType);
    }
    public OrderAttachmentRequest<T> withContentTypeContaining(String contentType){
       return withContentType(Operator.CONTAIN, contentType);
    }

    public OrderAttachmentRequest<T> withContentTypeNotContaining(String contentType){
       return withContentType(Operator.NOT_CONTAIN, contentType);
    }

    public OrderAttachmentRequest<T> withContentTypeNotStartingWith(String contentType){
       return withContentType(Operator.NOT_BEGIN_WITH, contentType);
    }

    public OrderAttachmentRequest<T> withContentTypeEndingWith(String contentType){
       return withContentType(Operator.END_WITH, contentType);
    }

    public OrderAttachmentRequest<T> withContentTypeNotEndingWith(String contentType){
       return withContentType(Operator.NOT_END_WITH, contentType);
    }

    public OrderAttachmentRequest<T> withContentTypeIs(String contentType){
       return withContentType(Operator.EQUAL, contentType);
    }

    public OrderAttachmentRequest<T> withContentTypeSoundingLike(String contentType){
       return withContentType(Operator.SOUNDS_LIKE, contentType);
    }



    public OrderAttachmentRequest<T> filterByContentLength(Integer... contentLength){
      if (contentLength == null || contentLength.length == 0) {
        throw new IllegalArgumentException("filterByContentLength parameter contentLength cannot be empty");
      }
      return appendSearchCriteria(createContentLengthCriteria(Operator.EQUAL, (Object[])contentLength));
    }

    public OrderAttachmentRequest<T> withContentLength(Operator operator, Object... values){
       return appendSearchCriteria(createContentLengthCriteria(operator, values));
    }

    public OrderAttachmentRequest<T> withContentLengthIsUnknown(){
       return withContentLength(Operator.IS_NULL);
    }

    public OrderAttachmentRequest<T> withContentLengthIsKnown(){
       return withContentLength(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createContentLengthCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderAttachment.CONTENT_LENGTH_PROPERTY, operator, values);
    }

    public OrderAttachmentRequest<T> withContentLengthIs(Integer contentLength){
       return withContentLength(Operator.EQUAL, contentLength);
    }

    public OrderAttachmentRequest<T> withContentLengthIsNot(Integer contentLength){
       return withContentLength(Operator.NOT_EQUAL, contentLength);
    }

    public OrderAttachmentRequest<T> withContentLengthIn(Integer... contentLength){
       return withContentLength(Operator.IN, (Object[])contentLength);
    }

    public OrderAttachmentRequest<T> withContentLengthNotIn(Integer... contentLength){
       return withContentLength(Operator.NOT_IN, (Object[])contentLength);
    }
    public OrderAttachmentRequest<T> withContentLengthGreaterThan(Integer contentLength){
       return withContentLength(Operator.GREATER_THAN, contentLength);
    }

    public OrderAttachmentRequest<T> withContentLengthGreaterThanOrEqualTo(Integer contentLength){
       return withContentLength(Operator.GREATER_THAN_OR_EQUAL, contentLength);
    }

    public OrderAttachmentRequest<T> withContentLengthLessThan(Integer contentLength){
       return withContentLength(Operator.LESS_THAN, contentLength);
    }

    public OrderAttachmentRequest<T> withContentLengthLessThanOrEqualTo(Integer contentLength){
       return withContentLength(Operator.LESS_THAN_OR_EQUAL, contentLength);
    }

    public OrderAttachmentRequest<T> withContentLengthBetween(Integer startOfContentLength, Integer endOfContentLength){
       return withContentLength(Operator.BETWEEN, startOfContentLength, endOfContentLength);
    }



    public OrderAttachmentRequest<T> filterByContentSha256(String... contentSha256){
      if (contentSha256 == null || contentSha256.length == 0) {
        throw new IllegalArgumentException("filterByContentSha256 parameter contentSha256 cannot be empty");
      }
      return appendSearchCriteria(createContentSha256Criteria(Operator.EQUAL, (Object[])contentSha256));
    }

    public OrderAttachmentRequest<T> withContentSha256(Operator operator, Object... values){
       return appendSearchCriteria(createContentSha256Criteria(operator, values));
    }

    public OrderAttachmentRequest<T> withContentSha256IsUnknown(){
       return withContentSha256(Operator.IS_NULL);
    }

    public OrderAttachmentRequest<T> withContentSha256IsKnown(){
       return withContentSha256(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createContentSha256Criteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderAttachment.CONTENT_SHA256_PROPERTY, operator, values);
    }

    public OrderAttachmentRequest<T> withContentSha256IsNot(String contentSha256){
       return withContentSha256(Operator.NOT_EQUAL, contentSha256);
    }

    public OrderAttachmentRequest<T> withContentSha256In(String... contentSha256){
       return withContentSha256(Operator.IN, (Object[])contentSha256);
    }

    public OrderAttachmentRequest<T> withContentSha256NotIn(String... contentSha256){
       return withContentSha256(Operator.NOT_IN, (Object[])contentSha256);
    }
    public OrderAttachmentRequest<T> withContentSha256GreaterThan(String contentSha256){
       return withContentSha256(Operator.GREATER_THAN, contentSha256);
    }

    public OrderAttachmentRequest<T> withContentSha256GreaterThanOrEqualTo(String contentSha256){
       return withContentSha256(Operator.GREATER_THAN_OR_EQUAL, contentSha256);
    }

    public OrderAttachmentRequest<T> withContentSha256LessThan(String contentSha256){
       return withContentSha256(Operator.LESS_THAN, contentSha256);
    }

    public OrderAttachmentRequest<T> withContentSha256LessThanOrEqualTo(String contentSha256){
       return withContentSha256(Operator.LESS_THAN_OR_EQUAL, contentSha256);
    }

    public OrderAttachmentRequest<T> withContentSha256Between(String startOfContentSha256, String endOfContentSha256){
       return withContentSha256(Operator.BETWEEN, startOfContentSha256, endOfContentSha256);
    }
    public OrderAttachmentRequest<T> withContentSha256StartingWith(String contentSha256){
       return withContentSha256(Operator.BEGIN_WITH, contentSha256);
    }
    public OrderAttachmentRequest<T> withContentSha256Containing(String contentSha256){
       return withContentSha256(Operator.CONTAIN, contentSha256);
    }

    public OrderAttachmentRequest<T> withContentSha256NotContaining(String contentSha256){
       return withContentSha256(Operator.NOT_CONTAIN, contentSha256);
    }

    public OrderAttachmentRequest<T> withContentSha256NotStartingWith(String contentSha256){
       return withContentSha256(Operator.NOT_BEGIN_WITH, contentSha256);
    }

    public OrderAttachmentRequest<T> withContentSha256EndingWith(String contentSha256){
       return withContentSha256(Operator.END_WITH, contentSha256);
    }

    public OrderAttachmentRequest<T> withContentSha256NotEndingWith(String contentSha256){
       return withContentSha256(Operator.NOT_END_WITH, contentSha256);
    }

    public OrderAttachmentRequest<T> withContentSha256Is(String contentSha256){
       return withContentSha256(Operator.EQUAL, contentSha256);
    }

    public OrderAttachmentRequest<T> withContentSha256SoundingLike(String contentSha256){
       return withContentSha256(Operator.SOUNDS_LIKE, contentSha256);
    }



    public OrderAttachmentRequest<T> filterByObjectKey(String... objectKey){
      if (objectKey == null || objectKey.length == 0) {
        throw new IllegalArgumentException("filterByObjectKey parameter objectKey cannot be empty");
      }
      return appendSearchCriteria(createObjectKeyCriteria(Operator.EQUAL, (Object[])objectKey));
    }

    public OrderAttachmentRequest<T> withObjectKey(Operator operator, Object... values){
       return appendSearchCriteria(createObjectKeyCriteria(operator, values));
    }

    public OrderAttachmentRequest<T> withObjectKeyIsUnknown(){
       return withObjectKey(Operator.IS_NULL);
    }

    public OrderAttachmentRequest<T> withObjectKeyIsKnown(){
       return withObjectKey(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createObjectKeyCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderAttachment.OBJECT_KEY_PROPERTY, operator, values);
    }

    public OrderAttachmentRequest<T> withObjectKeyIsNot(String objectKey){
       return withObjectKey(Operator.NOT_EQUAL, objectKey);
    }

    public OrderAttachmentRequest<T> withObjectKeyIn(String... objectKey){
       return withObjectKey(Operator.IN, (Object[])objectKey);
    }

    public OrderAttachmentRequest<T> withObjectKeyNotIn(String... objectKey){
       return withObjectKey(Operator.NOT_IN, (Object[])objectKey);
    }
    public OrderAttachmentRequest<T> withObjectKeyGreaterThan(String objectKey){
       return withObjectKey(Operator.GREATER_THAN, objectKey);
    }

    public OrderAttachmentRequest<T> withObjectKeyGreaterThanOrEqualTo(String objectKey){
       return withObjectKey(Operator.GREATER_THAN_OR_EQUAL, objectKey);
    }

    public OrderAttachmentRequest<T> withObjectKeyLessThan(String objectKey){
       return withObjectKey(Operator.LESS_THAN, objectKey);
    }

    public OrderAttachmentRequest<T> withObjectKeyLessThanOrEqualTo(String objectKey){
       return withObjectKey(Operator.LESS_THAN_OR_EQUAL, objectKey);
    }

    public OrderAttachmentRequest<T> withObjectKeyBetween(String startOfObjectKey, String endOfObjectKey){
       return withObjectKey(Operator.BETWEEN, startOfObjectKey, endOfObjectKey);
    }
    public OrderAttachmentRequest<T> withObjectKeyStartingWith(String objectKey){
       return withObjectKey(Operator.BEGIN_WITH, objectKey);
    }
    public OrderAttachmentRequest<T> withObjectKeyContaining(String objectKey){
       return withObjectKey(Operator.CONTAIN, objectKey);
    }

    public OrderAttachmentRequest<T> withObjectKeyNotContaining(String objectKey){
       return withObjectKey(Operator.NOT_CONTAIN, objectKey);
    }

    public OrderAttachmentRequest<T> withObjectKeyNotStartingWith(String objectKey){
       return withObjectKey(Operator.NOT_BEGIN_WITH, objectKey);
    }

    public OrderAttachmentRequest<T> withObjectKeyEndingWith(String objectKey){
       return withObjectKey(Operator.END_WITH, objectKey);
    }

    public OrderAttachmentRequest<T> withObjectKeyNotEndingWith(String objectKey){
       return withObjectKey(Operator.NOT_END_WITH, objectKey);
    }

    public OrderAttachmentRequest<T> withObjectKeyIs(String objectKey){
       return withObjectKey(Operator.EQUAL, objectKey);
    }

    public OrderAttachmentRequest<T> withObjectKeySoundingLike(String objectKey){
       return withObjectKey(Operator.SOUNDS_LIKE, objectKey);
    }



    public OrderAttachmentRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public OrderAttachmentRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public OrderAttachmentRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public OrderAttachmentRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderAttachment.VERSION_PROPERTY, operator, values);
    }

    public OrderAttachmentRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public OrderAttachmentRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public OrderAttachmentRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public OrderAttachmentRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public OrderAttachmentRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public OrderAttachmentRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public OrderAttachmentRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public OrderAttachmentRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public OrderAttachmentRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }



    public OrderAttachmentRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public OrderAttachmentRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public OrderAttachmentRequest minContentLength(){
        return minContentLengthAs(prefix("minOf",OrderAttachment.CONTENT_LENGTH_PROPERTY));
    }

    public OrderAttachmentRequest minContentLengthAs(String retName){
        super.min(retName, OrderAttachment.CONTENT_LENGTH_PROPERTY);
        return this;
    }
    public OrderAttachmentRequest maxContentLength(){
        return maxContentLengthAs(prefix("maxOf",OrderAttachment.CONTENT_LENGTH_PROPERTY));
    }

    public OrderAttachmentRequest maxContentLengthAs(String retName){
        super.max(retName, OrderAttachment.CONTENT_LENGTH_PROPERTY);
        return this;
    }
    public OrderAttachmentRequest sumContentLength(){
        return sumContentLengthAs(prefix("sumOf",OrderAttachment.CONTENT_LENGTH_PROPERTY));
    }

    public OrderAttachmentRequest sumContentLengthAs(String retName){
        super.sum(retName, OrderAttachment.CONTENT_LENGTH_PROPERTY);
        return this;
    }
    public OrderAttachmentRequest avgContentLength(){
        return avgContentLengthAs(prefix("avgOf",OrderAttachment.CONTENT_LENGTH_PROPERTY));
    }

    public OrderAttachmentRequest avgContentLengthAs(String retName){
        super.avg(retName, OrderAttachment.CONTENT_LENGTH_PROPERTY);
        return this;
    }
    public OrderAttachmentRequest standardDeviationContentLength(){
        return standardDeviationContentLengthAs(prefix("standardDeviationOf",OrderAttachment.CONTENT_LENGTH_PROPERTY));
    }

    public OrderAttachmentRequest standardDeviationContentLengthAs(String retName){
        super.standardDeviation(retName, OrderAttachment.CONTENT_LENGTH_PROPERTY);
        return this;
    }
    public OrderAttachmentRequest squareRootOfPopulationStandardDeviationContentLength(){
        return squareRootOfPopulationStandardDeviationContentLengthAs(prefix("squareRootOfPopulationStandardDeviationOf",OrderAttachment.CONTENT_LENGTH_PROPERTY));
    }

    public OrderAttachmentRequest squareRootOfPopulationStandardDeviationContentLengthAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, OrderAttachment.CONTENT_LENGTH_PROPERTY);
        return this;
    }
    public OrderAttachmentRequest sampleVarianceContentLength(){
        return sampleVarianceContentLengthAs(prefix("sampleVarianceOf",OrderAttachment.CONTENT_LENGTH_PROPERTY));
    }

    public OrderAttachmentRequest sampleVarianceContentLengthAs(String retName){
        super.sampleVariance(retName, OrderAttachment.CONTENT_LENGTH_PROPERTY);
        return this;
    }
    public OrderAttachmentRequest samplePopulationVarianceContentLength(){
        return samplePopulationVarianceContentLengthAs(prefix("samplePopulationVarianceOf",OrderAttachment.CONTENT_LENGTH_PROPERTY));
    }

    public OrderAttachmentRequest samplePopulationVarianceContentLengthAs(String retName){
        super.samplePopulationVariance(retName, OrderAttachment.CONTENT_LENGTH_PROPERTY);
        return this;
    }
    public OrderAttachmentRequest<T> groupByCustomerOrderWithDetails(){
       return groupByCustomerOrderWithDetails(Q.customerOrders().unlimited());
    }

    public OrderAttachmentRequest<T> groupByCustomerOrderWithDetails(CustomerOrderRequest subRequest){
       aggregate(OrderAttachment.CUSTOMER_ORDER_PROPERTY, subRequest);
       return this;
    }







    public OrderAttachmentRequest<T> countDistinctId(){
        return countDistinctIdAs("id_distinct_count");
    }

    public OrderAttachmentRequest<T> countDistinctIdAs(String alias){
        super.countDistinctProperty(alias, OrderAttachment.ID_PROPERTY);
        return this;
    }
    public OrderAttachmentRequest<T> countDistinctCustomerOrder(){
        return countDistinctCustomerOrderAs("customer_order_distinct_count");
    }

    public OrderAttachmentRequest<T> countDistinctCustomerOrderAs(String alias){
        super.countDistinctProperty(alias, OrderAttachment.CUSTOMER_ORDER_PROPERTY);
        return this;
    }
    public OrderAttachmentRequest<T> countDistinctFileName(){
        return countDistinctFileNameAs("file_name_distinct_count");
    }

    public OrderAttachmentRequest<T> countDistinctFileNameAs(String alias){
        super.countDistinctProperty(alias, OrderAttachment.FILE_NAME_PROPERTY);
        return this;
    }
    public OrderAttachmentRequest<T> countDistinctContentType(){
        return countDistinctContentTypeAs("content_type_distinct_count");
    }

    public OrderAttachmentRequest<T> countDistinctContentTypeAs(String alias){
        super.countDistinctProperty(alias, OrderAttachment.CONTENT_TYPE_PROPERTY);
        return this;
    }
    public OrderAttachmentRequest<T> countDistinctContentLength(){
        return countDistinctContentLengthAs("content_length_distinct_count");
    }

    public OrderAttachmentRequest<T> countDistinctContentLengthAs(String alias){
        super.countDistinctProperty(alias, OrderAttachment.CONTENT_LENGTH_PROPERTY);
        return this;
    }
    public OrderAttachmentRequest<T> countDistinctContentSha256(){
        return countDistinctContentSha256As("content_sha256_distinct_count");
    }

    public OrderAttachmentRequest<T> countDistinctContentSha256As(String alias){
        super.countDistinctProperty(alias, OrderAttachment.CONTENT_SHA256_PROPERTY);
        return this;
    }
    public OrderAttachmentRequest<T> countDistinctObjectKey(){
        return countDistinctObjectKeyAs("object_key_distinct_count");
    }

    public OrderAttachmentRequest<T> countDistinctObjectKeyAs(String alias){
        super.countDistinctProperty(alias, OrderAttachment.OBJECT_KEY_PROPERTY);
        return this;
    }
    public OrderAttachmentRequest<T> countDistinctVersion(){
        return countDistinctVersionAs("version_distinct_count");
    }

    public OrderAttachmentRequest<T> countDistinctVersionAs(String alias){
        super.countDistinctProperty(alias, OrderAttachment.VERSION_PROPERTY);
        return this;
    }

    public OrderAttachmentRequest<T> groupById(){
       groupBy(OrderAttachment.ID_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> groupByIdAs(String retName){
       groupBy(retName, OrderAttachment.ID_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderAttachment.ID_PROPERTY, function);
       return this;
    }
    public OrderAttachmentRequest<T> groupByCustomerOrderWith(CustomerOrderRequest subRequest){
       groupBy(OrderAttachment.CUSTOMER_ORDER_PROPERTY, subRequest,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "groupByCustomerOrderWith", null, "customer_order"));
       return this;
    }
    public OrderAttachmentRequest<T> groupByCustomerOrder(){
       groupBy(OrderAttachment.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> groupByCustomerOrderAs(String retName){
       groupBy(retName, OrderAttachment.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> groupByCustomerOrderWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderAttachment.CUSTOMER_ORDER_PROPERTY, function);
       return this;
    }

    public OrderAttachmentRequest<T> groupByFileName(){
       groupBy(OrderAttachment.FILE_NAME_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> groupByFileNameAs(String retName){
       groupBy(retName, OrderAttachment.FILE_NAME_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> groupByFileNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderAttachment.FILE_NAME_PROPERTY, function);
       return this;
    }

    public OrderAttachmentRequest<T> groupByContentType(){
       groupBy(OrderAttachment.CONTENT_TYPE_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> groupByContentTypeAs(String retName){
       groupBy(retName, OrderAttachment.CONTENT_TYPE_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> groupByContentTypeWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderAttachment.CONTENT_TYPE_PROPERTY, function);
       return this;
    }

    public OrderAttachmentRequest<T> groupByContentLength(){
       groupBy(OrderAttachment.CONTENT_LENGTH_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> groupByContentLengthAs(String retName){
       groupBy(retName, OrderAttachment.CONTENT_LENGTH_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> groupByContentLengthWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderAttachment.CONTENT_LENGTH_PROPERTY, function);
       return this;
    }

    public OrderAttachmentRequest<T> groupByContentSha256(){
       groupBy(OrderAttachment.CONTENT_SHA256_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> groupByContentSha256As(String retName){
       groupBy(retName, OrderAttachment.CONTENT_SHA256_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> groupByContentSha256WithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderAttachment.CONTENT_SHA256_PROPERTY, function);
       return this;
    }

    public OrderAttachmentRequest<T> groupByObjectKey(){
       groupBy(OrderAttachment.OBJECT_KEY_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> groupByObjectKeyAs(String retName){
       groupBy(retName, OrderAttachment.OBJECT_KEY_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> groupByObjectKeyWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderAttachment.OBJECT_KEY_PROPERTY, function);
       return this;
    }

    public OrderAttachmentRequest<T> groupByVersion(){
       groupBy(OrderAttachment.VERSION_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> groupByVersionAs(String retName){
       groupBy(retName, OrderAttachment.VERSION_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderAttachment.VERSION_PROPERTY, function);
       return this;
    }



    public OrderAttachmentRequest<T> orderByIdAscending(){
       addOrderByAscending(OrderAttachment.ID_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> orderByIdDescending(){
       addOrderByDescending(OrderAttachment.ID_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> orderByCustomerOrderAscending(){
       addOrderByAscending(OrderAttachment.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> orderByCustomerOrderDescending(){
       addOrderByDescending(OrderAttachment.CUSTOMER_ORDER_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> orderByFileNameAscending(){
       addOrderByAscending(OrderAttachment.FILE_NAME_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> orderByFileNameDescending(){
       addOrderByDescending(OrderAttachment.FILE_NAME_PROPERTY);
       return this;
    }
    public OrderAttachmentRequest<T> orderByFileNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(OrderAttachment.FILE_NAME_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> orderByFileNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(OrderAttachment.FILE_NAME_PROPERTY);
       return this;
    }
    public OrderAttachmentRequest<T> orderByContentTypeAscending(){
       addOrderByAscending(OrderAttachment.CONTENT_TYPE_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> orderByContentTypeDescending(){
       addOrderByDescending(OrderAttachment.CONTENT_TYPE_PROPERTY);
       return this;
    }
    public OrderAttachmentRequest<T> orderByContentTypeAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(OrderAttachment.CONTENT_TYPE_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> orderByContentTypeDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(OrderAttachment.CONTENT_TYPE_PROPERTY);
       return this;
    }
    public OrderAttachmentRequest<T> orderByContentLengthAscending(){
       addOrderByAscending(OrderAttachment.CONTENT_LENGTH_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> orderByContentLengthDescending(){
       addOrderByDescending(OrderAttachment.CONTENT_LENGTH_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> orderByContentSha256Ascending(){
       addOrderByAscending(OrderAttachment.CONTENT_SHA256_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> orderByContentSha256Descending(){
       addOrderByDescending(OrderAttachment.CONTENT_SHA256_PROPERTY);
       return this;
    }
    public OrderAttachmentRequest<T> orderByContentSha256AscendingUsingGBK(){
       addOrderByAscendingUsingGBK(OrderAttachment.CONTENT_SHA256_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> orderByContentSha256DescendingUsingGBK(){
       addOrderByDescendingUsingGBK(OrderAttachment.CONTENT_SHA256_PROPERTY);
       return this;
    }
    public OrderAttachmentRequest<T> orderByObjectKeyAscending(){
       addOrderByAscending(OrderAttachment.OBJECT_KEY_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> orderByObjectKeyDescending(){
       addOrderByDescending(OrderAttachment.OBJECT_KEY_PROPERTY);
       return this;
    }
    public OrderAttachmentRequest<T> orderByObjectKeyAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(OrderAttachment.OBJECT_KEY_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> orderByObjectKeyDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(OrderAttachment.OBJECT_KEY_PROPERTY);
       return this;
    }
    public OrderAttachmentRequest<T> orderByVersionAscending(){
       addOrderByAscending(OrderAttachment.VERSION_PROPERTY);
       return this;
    }

    public OrderAttachmentRequest<T> orderByVersionDescending(){
       addOrderByDescending(OrderAttachment.VERSION_PROPERTY);
       return this;
    }


    public CustomerOrderRequest rollUpToCustomerOrder(){
       CustomerOrderRequest customerOrder = Q.customerOrders().unlimited();
       this.internalRollUpTo("customer_order", customerOrder,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "rollUpToCustomerOrder", null, "customer_order"));
       return customerOrder;
    }








   public OrderAttachmentRequest<T> facetByCustomerOrderAs(String facetName, CustomerOrderRequest customerOrder){
       return facetByCustomerOrderAs(facetName, customerOrder, true);
   }

   public OrderAttachmentRequest<T> facetByCustomerOrderAs(String facetName, CustomerOrderRequest customerOrder, boolean includeAllFacets){
       addFacet(facetName, OrderAttachment.CUSTOMER_ORDER_PROPERTY, customerOrder, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByCustomerOrderAs", facetName, OrderAttachment.CUSTOMER_ORDER_PROPERTY));
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public OrderAttachmentRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public OrderAttachmentRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public OrderAttachmentRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public OrderAttachmentRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public OrderAttachmentRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public OrderAttachmentRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}