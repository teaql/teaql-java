
package com.teaql.checkerfixadvanced.purchaseorder;

import com.teaql.checkerfixadvanced.Q;
import com.teaql.checkerfixadvanced.orderitem.OrderItem;
import com.teaql.checkerfixadvanced.orderitem.OrderItemRequest;
import com.teaql.checkerfixadvanced.tenant.Tenant;
import com.teaql.checkerfixadvanced.tenant.TenantRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.time.LocalDateTime;
import java.util.Date;

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
        return selectId().selectTenantIdOnly().selectContactPhone().selectInternalNote().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public PurchaseOrderRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public PurchaseOrderRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(PurchaseOrder.ID_PROPERTY);
        selectProperty(PurchaseOrder.TENANT_PROPERTY);
        selectProperty(PurchaseOrder.CONTACT_PHONE_PROPERTY);
        selectProperty(PurchaseOrder.INTERNAL_NOTE_PROPERTY);
        selectProperty(PurchaseOrder.CREATE_TIME_PROPERTY);
        selectProperty(PurchaseOrder.UPDATE_TIME_PROPERTY);
        selectProperty(PurchaseOrder.VERSION_PROPERTY);
        return this;
    }

    public PurchaseOrderRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectTenant().selectContactPhone().selectInternalNote().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public PurchaseOrderRequest<T> selectChildren(){
        super.selectAny();
        selectOrderItemList();
        return selectId().selectTenant().selectContactPhone().selectInternalNote().selectCreateTime().selectUpdateTime().selectVersion();
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
    public PurchaseOrderRequest<T> selectTenantIdOnly(){
       selectProperty(PurchaseOrder.TENANT_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> selectTenant(){
        return selectTenantWith(Q.tenants().unlimited().selectSelf());
    }

    public PurchaseOrderRequest<T> selectTenantWith(TenantRequest tenant){
       selectProperty(PurchaseOrder.TENANT_PROPERTY);
       enhanceRelation(PurchaseOrder.TENANT_PROPERTY, tenant,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectTenantWith", null, PurchaseOrder.TENANT_PROPERTY));
       return this;
    }

    public PurchaseOrderRequest<T> unselectTenant(){
       unselectProperty(PurchaseOrder.TENANT_PROPERTY);
       return this;
    }
    public PurchaseOrderRequest<T> selectContactPhone(){
       selectProperty(PurchaseOrder.CONTACT_PHONE_PROPERTY);
       return this;
    }

    /**
     * fill the contactPhone with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  contactPhone) to fetch contactPhone property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PurchaseOrderRequest<T> unselectContactPhone(){
       unselectProperty(PurchaseOrder.CONTACT_PHONE_PROPERTY);
       return this;
    }
    public PurchaseOrderRequest<T> selectInternalNote(){
       selectProperty(PurchaseOrder.INTERNAL_NOTE_PROPERTY);
       return this;
    }

    /**
     * fill the internalNote with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  internalNote) to fetch internalNote property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PurchaseOrderRequest<T> unselectInternalNote(){
       unselectProperty(PurchaseOrder.INTERNAL_NOTE_PROPERTY);
       return this;
    }
    public PurchaseOrderRequest<T> selectCreateTime(){
       selectProperty(PurchaseOrder.CREATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the createTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  createTime) to fetch createTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PurchaseOrderRequest<T> unselectCreateTime(){
       unselectProperty(PurchaseOrder.CREATE_TIME_PROPERTY);
       return this;
    }
    public PurchaseOrderRequest<T> selectUpdateTime(){
       selectProperty(PurchaseOrder.UPDATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the updateTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  updateTime) to fetch updateTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public PurchaseOrderRequest<T> unselectUpdateTime(){
       unselectProperty(PurchaseOrder.UPDATE_TIME_PROPERTY);
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
    public PurchaseOrderRequest<T> selectOrderItemList(){
       return selectOrderItemListWith(Q.orderItems().selectSelf());
    }

    public PurchaseOrderRequest<T> selectOrderItemListWith(OrderItemRequest orderItemList){
       enhanceRelation(PurchaseOrder.ORDER_ITEM_LIST_PROPERTY, orderItemList,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectOrderItemListWith", null, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
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



    public PurchaseOrderRequest<T> filterByTenant(Tenant... tenant){
      if (tenant == null || tenant.length == 0) {
        throw new IllegalArgumentException("filterByTenant parameter tenant cannot be empty");
      }
      return appendSearchCriteria(createTenantCriteria(Operator.EQUAL, (Object[])tenant));
    }

    public PurchaseOrderRequest<T> withTenant(Operator operator, Object... values){
       return appendSearchCriteria(createTenantCriteria(operator, values));
    }

    public PurchaseOrderRequest<T> withTenantIsUnknown(){
       return withTenant(Operator.IS_NULL);
    }

    public PurchaseOrderRequest<T> withTenantIsKnown(){
       return withTenant(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createTenantCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(PurchaseOrder.TENANT_PROPERTY, operator, values);
    }

    public PurchaseOrderRequest<T> filterByTenant(Long tenant){
      if(tenant == null){
         return this;
      }
      return withTenant(Operator.EQUAL, tenant);
    }
    public PurchaseOrderRequest<T> withTenantMatching(TenantRequest tenant){
       return appendSearchCriteria(new SubQuerySearchCriteria(PurchaseOrder.TENANT_PROPERTY, tenant, Tenant.ID_PROPERTY));
    }

    public PurchaseOrderRequest<T> withoutTenantMatching(TenantRequest tenant){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(PurchaseOrder.TENANT_PROPERTY, tenant, Tenant.ID_PROPERTY)));
    }

    public PurchaseOrderRequest<T> filterByContactPhone(String... contactPhone){
      if (contactPhone == null || contactPhone.length == 0) {
        throw new IllegalArgumentException("filterByContactPhone parameter contactPhone cannot be empty");
      }
      return appendSearchCriteria(createContactPhoneCriteria(Operator.EQUAL, (Object[])contactPhone));
    }

    public PurchaseOrderRequest<T> withContactPhone(Operator operator, Object... values){
       return appendSearchCriteria(createContactPhoneCriteria(operator, values));
    }

    public PurchaseOrderRequest<T> withContactPhoneIsUnknown(){
       return withContactPhone(Operator.IS_NULL);
    }

    public PurchaseOrderRequest<T> withContactPhoneIsKnown(){
       return withContactPhone(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createContactPhoneCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(PurchaseOrder.CONTACT_PHONE_PROPERTY, operator, values);
    }

    public PurchaseOrderRequest<T> withContactPhoneIsNot(String contactPhone){
       return withContactPhone(Operator.NOT_EQUAL, contactPhone);
    }

    public PurchaseOrderRequest<T> withContactPhoneIn(String... contactPhone){
       return withContactPhone(Operator.IN, (Object[])contactPhone);
    }

    public PurchaseOrderRequest<T> withContactPhoneNotIn(String... contactPhone){
       return withContactPhone(Operator.NOT_IN, (Object[])contactPhone);
    }
    public PurchaseOrderRequest<T> withContactPhoneGreaterThan(String contactPhone){
       return withContactPhone(Operator.GREATER_THAN, contactPhone);
    }

    public PurchaseOrderRequest<T> withContactPhoneGreaterThanOrEqualTo(String contactPhone){
       return withContactPhone(Operator.GREATER_THAN_OR_EQUAL, contactPhone);
    }

    public PurchaseOrderRequest<T> withContactPhoneLessThan(String contactPhone){
       return withContactPhone(Operator.LESS_THAN, contactPhone);
    }

    public PurchaseOrderRequest<T> withContactPhoneLessThanOrEqualTo(String contactPhone){
       return withContactPhone(Operator.LESS_THAN_OR_EQUAL, contactPhone);
    }

    public PurchaseOrderRequest<T> withContactPhoneBetween(String startOfContactPhone, String endOfContactPhone){
       return withContactPhone(Operator.BETWEEN, startOfContactPhone, endOfContactPhone);
    }
    public PurchaseOrderRequest<T> withContactPhoneStartingWith(String contactPhone){
       return withContactPhone(Operator.BEGIN_WITH, contactPhone);
    }
    public PurchaseOrderRequest<T> withContactPhoneContaining(String contactPhone){
       return withContactPhone(Operator.CONTAIN, contactPhone);
    }

    public PurchaseOrderRequest<T> withContactPhoneNotContaining(String contactPhone){
       return withContactPhone(Operator.NOT_CONTAIN, contactPhone);
    }

    public PurchaseOrderRequest<T> withContactPhoneNotStartingWith(String contactPhone){
       return withContactPhone(Operator.NOT_BEGIN_WITH, contactPhone);
    }

    public PurchaseOrderRequest<T> withContactPhoneEndingWith(String contactPhone){
       return withContactPhone(Operator.END_WITH, contactPhone);
    }

    public PurchaseOrderRequest<T> withContactPhoneNotEndingWith(String contactPhone){
       return withContactPhone(Operator.NOT_END_WITH, contactPhone);
    }

    public PurchaseOrderRequest<T> withContactPhoneIs(String contactPhone){
       return withContactPhone(Operator.EQUAL, contactPhone);
    }

    public PurchaseOrderRequest<T> withContactPhoneSoundingLike(String contactPhone){
       return withContactPhone(Operator.SOUNDS_LIKE, contactPhone);
    }



    public PurchaseOrderRequest<T> filterByInternalNote(String... internalNote){
      if (internalNote == null || internalNote.length == 0) {
        throw new IllegalArgumentException("filterByInternalNote parameter internalNote cannot be empty");
      }
      return appendSearchCriteria(createInternalNoteCriteria(Operator.EQUAL, (Object[])internalNote));
    }

    public PurchaseOrderRequest<T> withInternalNote(Operator operator, Object... values){
       return appendSearchCriteria(createInternalNoteCriteria(operator, values));
    }

    public PurchaseOrderRequest<T> withInternalNoteIsUnknown(){
       return withInternalNote(Operator.IS_NULL);
    }

    public PurchaseOrderRequest<T> withInternalNoteIsKnown(){
       return withInternalNote(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createInternalNoteCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(PurchaseOrder.INTERNAL_NOTE_PROPERTY, operator, values);
    }

    public PurchaseOrderRequest<T> withInternalNoteIsNot(String internalNote){
       return withInternalNote(Operator.NOT_EQUAL, internalNote);
    }

    public PurchaseOrderRequest<T> withInternalNoteIn(String... internalNote){
       return withInternalNote(Operator.IN, (Object[])internalNote);
    }

    public PurchaseOrderRequest<T> withInternalNoteNotIn(String... internalNote){
       return withInternalNote(Operator.NOT_IN, (Object[])internalNote);
    }
    public PurchaseOrderRequest<T> withInternalNoteGreaterThan(String internalNote){
       return withInternalNote(Operator.GREATER_THAN, internalNote);
    }

    public PurchaseOrderRequest<T> withInternalNoteGreaterThanOrEqualTo(String internalNote){
       return withInternalNote(Operator.GREATER_THAN_OR_EQUAL, internalNote);
    }

    public PurchaseOrderRequest<T> withInternalNoteLessThan(String internalNote){
       return withInternalNote(Operator.LESS_THAN, internalNote);
    }

    public PurchaseOrderRequest<T> withInternalNoteLessThanOrEqualTo(String internalNote){
       return withInternalNote(Operator.LESS_THAN_OR_EQUAL, internalNote);
    }

    public PurchaseOrderRequest<T> withInternalNoteBetween(String startOfInternalNote, String endOfInternalNote){
       return withInternalNote(Operator.BETWEEN, startOfInternalNote, endOfInternalNote);
    }
    public PurchaseOrderRequest<T> withInternalNoteStartingWith(String internalNote){
       return withInternalNote(Operator.BEGIN_WITH, internalNote);
    }
    public PurchaseOrderRequest<T> withInternalNoteContaining(String internalNote){
       return withInternalNote(Operator.CONTAIN, internalNote);
    }

    public PurchaseOrderRequest<T> withInternalNoteNotContaining(String internalNote){
       return withInternalNote(Operator.NOT_CONTAIN, internalNote);
    }

    public PurchaseOrderRequest<T> withInternalNoteNotStartingWith(String internalNote){
       return withInternalNote(Operator.NOT_BEGIN_WITH, internalNote);
    }

    public PurchaseOrderRequest<T> withInternalNoteEndingWith(String internalNote){
       return withInternalNote(Operator.END_WITH, internalNote);
    }

    public PurchaseOrderRequest<T> withInternalNoteNotEndingWith(String internalNote){
       return withInternalNote(Operator.NOT_END_WITH, internalNote);
    }

    public PurchaseOrderRequest<T> withInternalNoteIs(String internalNote){
       return withInternalNote(Operator.EQUAL, internalNote);
    }

    public PurchaseOrderRequest<T> withInternalNoteSoundingLike(String internalNote){
       return withInternalNote(Operator.SOUNDS_LIKE, internalNote);
    }



    public PurchaseOrderRequest<T> filterByCreateTime(LocalDateTime... createTime){
      if (createTime == null || createTime.length == 0) {
        throw new IllegalArgumentException("filterByCreateTime parameter createTime cannot be empty");
      }
      return appendSearchCriteria(createCreateTimeCriteria(Operator.EQUAL, (Object[])createTime));
    }

    public PurchaseOrderRequest<T> withCreateTime(Operator operator, Object... values){
       return appendSearchCriteria(createCreateTimeCriteria(operator, values));
    }

    public PurchaseOrderRequest<T> withCreateTimeIsUnknown(){
       return withCreateTime(Operator.IS_NULL);
    }

    public PurchaseOrderRequest<T> withCreateTimeIsKnown(){
       return withCreateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCreateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(PurchaseOrder.CREATE_TIME_PROPERTY, operator, values);
    }

    public PurchaseOrderRequest<T> withCreateTimeIs(LocalDateTime createTime){
       return withCreateTime(Operator.EQUAL, createTime);
    }

    public PurchaseOrderRequest<T> withCreateTimeIsNot(LocalDateTime createTime){
       return withCreateTime(Operator.NOT_EQUAL, createTime);
    }

    public PurchaseOrderRequest<T> withCreateTimeIn(LocalDateTime... createTime){
       return withCreateTime(Operator.IN, (Object[])createTime);
    }

    public PurchaseOrderRequest<T> withCreateTimeNotIn(LocalDateTime... createTime){
       return withCreateTime(Operator.NOT_IN, (Object[])createTime);
    }
    public PurchaseOrderRequest<T> withCreateTimeGreaterThan(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public PurchaseOrderRequest<T> withCreateTimeGreaterThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN_OR_EQUAL, createTime);
    }

    public PurchaseOrderRequest<T> withCreateTimeLessThan(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public PurchaseOrderRequest<T> withCreateTimeLessThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN_OR_EQUAL, createTime);
    }

    public PurchaseOrderRequest<T> withCreateTimeBetween(LocalDateTime startOfCreateTime, LocalDateTime endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }
    public PurchaseOrderRequest<T> withCreateTimeBefore(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public PurchaseOrderRequest<T> withCreateTimeBefore(Date createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public PurchaseOrderRequest<T> withCreateTimeAfter(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public PurchaseOrderRequest<T> withCreateTimeAfter(Date createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public PurchaseOrderRequest<T> withCreateTimeBetween(Date startOfCreateTime, Date endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }




    public PurchaseOrderRequest<T> filterByUpdateTime(LocalDateTime... updateTime){
      if (updateTime == null || updateTime.length == 0) {
        throw new IllegalArgumentException("filterByUpdateTime parameter updateTime cannot be empty");
      }
      return appendSearchCriteria(createUpdateTimeCriteria(Operator.EQUAL, (Object[])updateTime));
    }

    public PurchaseOrderRequest<T> withUpdateTime(Operator operator, Object... values){
       return appendSearchCriteria(createUpdateTimeCriteria(operator, values));
    }

    public PurchaseOrderRequest<T> withUpdateTimeIsUnknown(){
       return withUpdateTime(Operator.IS_NULL);
    }

    public PurchaseOrderRequest<T> withUpdateTimeIsKnown(){
       return withUpdateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createUpdateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(PurchaseOrder.UPDATE_TIME_PROPERTY, operator, values);
    }

    public PurchaseOrderRequest<T> withUpdateTimeIs(LocalDateTime updateTime){
       return withUpdateTime(Operator.EQUAL, updateTime);
    }

    public PurchaseOrderRequest<T> withUpdateTimeIsNot(LocalDateTime updateTime){
       return withUpdateTime(Operator.NOT_EQUAL, updateTime);
    }

    public PurchaseOrderRequest<T> withUpdateTimeIn(LocalDateTime... updateTime){
       return withUpdateTime(Operator.IN, (Object[])updateTime);
    }

    public PurchaseOrderRequest<T> withUpdateTimeNotIn(LocalDateTime... updateTime){
       return withUpdateTime(Operator.NOT_IN, (Object[])updateTime);
    }
    public PurchaseOrderRequest<T> withUpdateTimeGreaterThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public PurchaseOrderRequest<T> withUpdateTimeGreaterThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN_OR_EQUAL, updateTime);
    }

    public PurchaseOrderRequest<T> withUpdateTimeLessThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public PurchaseOrderRequest<T> withUpdateTimeLessThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN_OR_EQUAL, updateTime);
    }

    public PurchaseOrderRequest<T> withUpdateTimeBetween(LocalDateTime startOfUpdateTime, LocalDateTime endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
    }
    public PurchaseOrderRequest<T> withUpdateTimeBefore(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public PurchaseOrderRequest<T> withUpdateTimeBefore(Date updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public PurchaseOrderRequest<T> withUpdateTimeAfter(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public PurchaseOrderRequest<T> withUpdateTimeAfter(Date updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public PurchaseOrderRequest<T> withUpdateTimeBetween(Date startOfUpdateTime, Date endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
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


    public PurchaseOrderRequest<T> withOrderItemListMatching(OrderItemRequest orderItemRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(PurchaseOrder.ID_PROPERTY, orderItemRequest, OrderItem.PURCHASE_ORDER_PROPERTY));
    }

    public PurchaseOrderRequest<T> withoutOrderItemListMatching(OrderItemRequest orderItemRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(PurchaseOrder.ID_PROPERTY, orderItemRequest, OrderItem.PURCHASE_ORDER_PROPERTY)));
    }

    public PurchaseOrderRequest<T> haveOrderItems(){
        return withOrderItemListMatching(Q.orderItems().unlimited());
    }

    public PurchaseOrderRequest<T> haveNoOrderItems(){
        return withoutOrderItemListMatching(Q.orderItems().unlimited());
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
    public PurchaseOrderRequest<T> groupByTenantWithDetails(){
       return groupByTenantWithDetails(Q.tenants().unlimited());
    }

    public PurchaseOrderRequest<T> groupByTenantWithDetails(TenantRequest subRequest){
       aggregate(PurchaseOrder.TENANT_PROPERTY, subRequest);
       return this;
    }






    public PurchaseOrderRequest<T> groupByOrderItemsWithDetails(OrderItemRequest subRequest){
       aggregate(PurchaseOrder.ORDER_ITEM_LIST_PROPERTY, subRequest);
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
    public PurchaseOrderRequest<T> groupByTenantWith(TenantRequest subRequest){
       groupBy(PurchaseOrder.TENANT_PROPERTY, subRequest);
       return this;
    }
    public PurchaseOrderRequest<T> groupByTenant(){
       groupBy(PurchaseOrder.TENANT_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupByTenantAs(String retName){
       groupBy(retName, PurchaseOrder.TENANT_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupByTenantWithFunction(String retName, AggrFunction function){
       groupBy(retName, PurchaseOrder.TENANT_PROPERTY, function);
       return this;
    }

    public PurchaseOrderRequest<T> groupByContactPhone(){
       groupBy(PurchaseOrder.CONTACT_PHONE_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupByContactPhoneAs(String retName){
       groupBy(retName, PurchaseOrder.CONTACT_PHONE_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupByContactPhoneWithFunction(String retName, AggrFunction function){
       groupBy(retName, PurchaseOrder.CONTACT_PHONE_PROPERTY, function);
       return this;
    }

    public PurchaseOrderRequest<T> groupByInternalNote(){
       groupBy(PurchaseOrder.INTERNAL_NOTE_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupByInternalNoteAs(String retName){
       groupBy(retName, PurchaseOrder.INTERNAL_NOTE_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupByInternalNoteWithFunction(String retName, AggrFunction function){
       groupBy(retName, PurchaseOrder.INTERNAL_NOTE_PROPERTY, function);
       return this;
    }

    public PurchaseOrderRequest<T> groupByCreateTime(){
       groupBy(PurchaseOrder.CREATE_TIME_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupByCreateTimeAs(String retName){
       groupBy(retName, PurchaseOrder.CREATE_TIME_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupByCreateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, PurchaseOrder.CREATE_TIME_PROPERTY, function);
       return this;
    }

    public PurchaseOrderRequest<T> groupByUpdateTime(){
       groupBy(PurchaseOrder.UPDATE_TIME_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupByUpdateTimeAs(String retName){
       groupBy(retName, PurchaseOrder.UPDATE_TIME_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> groupByUpdateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, PurchaseOrder.UPDATE_TIME_PROPERTY, function);
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

    public PurchaseOrderRequest<T> orderByTenantAscending(){
       addOrderByAscending(PurchaseOrder.TENANT_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> orderByTenantDescending(){
       addOrderByDescending(PurchaseOrder.TENANT_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> orderByContactPhoneAscending(){
       addOrderByAscending(PurchaseOrder.CONTACT_PHONE_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> orderByContactPhoneDescending(){
       addOrderByDescending(PurchaseOrder.CONTACT_PHONE_PROPERTY);
       return this;
    }
    public PurchaseOrderRequest<T> orderByContactPhoneAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(PurchaseOrder.CONTACT_PHONE_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> orderByContactPhoneDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(PurchaseOrder.CONTACT_PHONE_PROPERTY);
       return this;
    }
    public PurchaseOrderRequest<T> orderByInternalNoteAscending(){
       addOrderByAscending(PurchaseOrder.INTERNAL_NOTE_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> orderByInternalNoteDescending(){
       addOrderByDescending(PurchaseOrder.INTERNAL_NOTE_PROPERTY);
       return this;
    }
    public PurchaseOrderRequest<T> orderByInternalNoteAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(PurchaseOrder.INTERNAL_NOTE_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> orderByInternalNoteDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(PurchaseOrder.INTERNAL_NOTE_PROPERTY);
       return this;
    }
    public PurchaseOrderRequest<T> orderByCreateTimeAscending(){
       addOrderByAscending(PurchaseOrder.CREATE_TIME_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> orderByCreateTimeDescending(){
       addOrderByDescending(PurchaseOrder.CREATE_TIME_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> orderByUpdateTimeAscending(){
       addOrderByAscending(PurchaseOrder.UPDATE_TIME_PROPERTY);
       return this;
    }

    public PurchaseOrderRequest<T> orderByUpdateTimeDescending(){
       addOrderByDescending(PurchaseOrder.UPDATE_TIME_PROPERTY);
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


    public PurchaseOrderRequest<T> statsFromOrderItemsAs(String name, OrderItemRequest subRequest){
       return statsFromOrderItemsAs(name, subRequest, false);
    }

    public PurchaseOrderRequest<T> statsFromOrderItemsAs(String name, OrderItemRequest subRequest, boolean singleResult){
       addRelationAggregate(name, subRequest, OrderItem.PURCHASE_ORDER_PROPERTY, singleResult,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "statsFromOrderItemsAs", name, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
       return this;
    }

    public PurchaseOrderRequest<T> statsFromOrderItems(OrderItemRequest subRequest){
       return statsFromOrderItemsAs(REFINEMENTS, subRequest);
    }
    public TenantRequest rollUpToTenant(){
       TenantRequest tenant = Q.tenants().unlimited();
       this.withTenantMatching(tenant)
           .groupByTenantWith(tenant);
       return tenant;
    }






    public PurchaseOrderRequest<T> countOrderItems(){
        addRelationCount("Count", Q.orderItems().unlimited(), OrderItem.PURCHASE_ORDER_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderItems", null, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public PurchaseOrderRequest<T> countOrderItemsAs(String name){
        addRelationCount(name, Q.orderItems().unlimited(), OrderItem.PURCHASE_ORDER_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderItemsAs", name, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public PurchaseOrderRequest<T> countOrderItemsWith(String name, OrderItemRequest subRequest){
        addRelationCount(name, subRequest, OrderItem.PURCHASE_ORDER_PROPERTY,
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_COUNT, "countOrderItemsWith", name, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public PurchaseOrderRequest<T> minUnitPriceOfOrderItems(){
        addRelationScalarAggregate("minUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.PURCHASE_ORDER_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minUnitPriceOfOrderItems", null, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public PurchaseOrderRequest<T> minUnitPriceOfOrderItemsAs(String name){
        return minUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public PurchaseOrderRequest<T> minUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.PURCHASE_ORDER_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("min".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "minUnitPriceOfOrderItemsAs", name, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public PurchaseOrderRequest<T> maxUnitPriceOfOrderItems(){
        addRelationScalarAggregate("maxUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.PURCHASE_ORDER_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxUnitPriceOfOrderItems", null, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public PurchaseOrderRequest<T> maxUnitPriceOfOrderItemsAs(String name){
        return maxUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public PurchaseOrderRequest<T> maxUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.PURCHASE_ORDER_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("max".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "maxUnitPriceOfOrderItemsAs", name, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public PurchaseOrderRequest<T> sumUnitPriceOfOrderItems(){
        addRelationScalarAggregate("sumUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.PURCHASE_ORDER_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumUnitPriceOfOrderItems", null, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public PurchaseOrderRequest<T> sumUnitPriceOfOrderItemsAs(String name){
        return sumUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public PurchaseOrderRequest<T> sumUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.PURCHASE_ORDER_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("sum".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sumUnitPriceOfOrderItemsAs", name, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public PurchaseOrderRequest<T> avgUnitPriceOfOrderItems(){
        addRelationScalarAggregate("avgUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.PURCHASE_ORDER_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgUnitPriceOfOrderItems", null, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public PurchaseOrderRequest<T> avgUnitPriceOfOrderItemsAs(String name){
        return avgUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public PurchaseOrderRequest<T> avgUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.PURCHASE_ORDER_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("avg".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "avgUnitPriceOfOrderItemsAs", name, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public PurchaseOrderRequest<T> standardDeviationUnitPriceOfOrderItems(){
        addRelationScalarAggregate("stdDevUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.PURCHASE_ORDER_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationUnitPriceOfOrderItems", null, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public PurchaseOrderRequest<T> standardDeviationUnitPriceOfOrderItemsAs(String name){
        return standardDeviationUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public PurchaseOrderRequest<T> standardDeviationUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.PURCHASE_ORDER_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "standardDeviationUnitPriceOfOrderItemsAs", name, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public PurchaseOrderRequest<T> squareRootOfPopulationStandardDeviationUnitPriceOfOrderItems(){
        addRelationScalarAggregate("stdDevPopUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.PURCHASE_ORDER_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationUnitPriceOfOrderItems", null, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public PurchaseOrderRequest<T> squareRootOfPopulationStandardDeviationUnitPriceOfOrderItemsAs(String name){
        return squareRootOfPopulationStandardDeviationUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public PurchaseOrderRequest<T> squareRootOfPopulationStandardDeviationUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.PURCHASE_ORDER_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("stddev_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "squareRootOfPopulationStandardDeviationUnitPriceOfOrderItemsAs", name, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public PurchaseOrderRequest<T> sampleVarianceUnitPriceOfOrderItems(){
        addRelationScalarAggregate("varSampUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.PURCHASE_ORDER_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceUnitPriceOfOrderItems", null, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public PurchaseOrderRequest<T> sampleVarianceUnitPriceOfOrderItemsAs(String name){
        return sampleVarianceUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public PurchaseOrderRequest<T> sampleVarianceUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.PURCHASE_ORDER_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_samp".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "sampleVarianceUnitPriceOfOrderItemsAs", name, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }
    public PurchaseOrderRequest<T> samplePopulationVarianceUnitPriceOfOrderItems(){
        addRelationScalarAggregate("varPopUnitPriceOfOrderItems", Q.orderItems().unlimited(),
            OrderItem.PURCHASE_ORDER_PROPERTY, OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceUnitPriceOfOrderItems", null, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

    public PurchaseOrderRequest<T> samplePopulationVarianceUnitPriceOfOrderItemsAs(String name){
        return samplePopulationVarianceUnitPriceOfOrderItemsAs(name, Q.orderItems().unlimited());
    }

    public PurchaseOrderRequest<T> samplePopulationVarianceUnitPriceOfOrderItemsAs(String name, OrderItemRequest subRequest){
        addRelationScalarAggregate(name, subRequest, OrderItem.PURCHASE_ORDER_PROPERTY,
            OrderItem.UNIT_PRICE_PROPERTY,
            io.teaql.core.AggrFunction.valueOf("var_pop".toUpperCase(java.util.Locale.ROOT)),
            io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.AGGREGATE, "samplePopulationVarianceUnitPriceOfOrderItemsAs", name, PurchaseOrder.ORDER_ITEM_LIST_PROPERTY));
        return this;
    }

   public PurchaseOrderRequest<T> facetByTenantAs(String facetName, TenantRequest tenant){
       return facetByTenantAs(facetName, tenant, true);
   }

   public PurchaseOrderRequest<T> facetByTenantAs(String facetName, TenantRequest tenant, boolean includeAllFacets){
       addFacet(facetName, PurchaseOrder.TENANT_PROPERTY, tenant, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByTenantAs", facetName, PurchaseOrder.TENANT_PROPERTY));
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