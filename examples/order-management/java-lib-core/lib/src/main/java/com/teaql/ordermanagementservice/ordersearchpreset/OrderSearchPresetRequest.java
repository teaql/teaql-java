
package com.teaql.ordermanagementservice.ordersearchpreset;

import com.teaql.ordermanagementservice.Q;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.time.LocalDateTime;
import java.util.Date;

public class OrderSearchPresetRequest<T extends OrderSearchPreset> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    public OrderSearchPresetRequest(Class<T> returnType){
        super(returnType);
        selectId();
        selectVersion();
    }

    public OrderSearchPresetRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public OrderSearchPresetRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public OrderSearchPresetRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public OrderSearchPresetRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public OrderSearchPresetRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    public OrderSearchPresetRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (OrderSearchPresetRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public OrderSearchPresetRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public OrderSearchPresetRequest<T> matchingAnyOf(OrderSearchPresetRequest orderSearchPreset){
        super.internalMatchAny(orderSearchPreset);
        return this;
    }

    public OrderSearchPresetRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public OrderSearchPresetRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public OrderSearchPresetRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public OrderSearchPresetRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectName().selectFilterJson().selectRequestId().selectOwnerUserId().selectCommercePlatformIdOnly().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public OrderSearchPresetRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public OrderSearchPresetRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectName().selectFilterJson().selectRequestId().selectOwnerUserId().selectCommercePlatform().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public OrderSearchPresetRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectName().selectFilterJson().selectRequestId().selectOwnerUserId().selectCommercePlatform().selectCreateTime().selectUpdateTime().selectVersion();
    }


    public OrderSearchPresetRequest<T> selectId(){
       selectProperty(OrderSearchPreset.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderSearchPresetRequest<T> unselectId(){
       unselectProperty(OrderSearchPreset.ID_PROPERTY);
       return this;
    }
    public OrderSearchPresetRequest<T> selectName(){
       selectProperty(OrderSearchPreset.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderSearchPresetRequest<T> unselectName(){
       unselectProperty(OrderSearchPreset.NAME_PROPERTY);
       return this;
    }
    public OrderSearchPresetRequest<T> selectFilterJson(){
       selectProperty(OrderSearchPreset.FILTER_JSON_PROPERTY);
       return this;
    }

    /**
     * fill the filterJson with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  filterJson) to fetch filterJson property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderSearchPresetRequest<T> unselectFilterJson(){
       unselectProperty(OrderSearchPreset.FILTER_JSON_PROPERTY);
       return this;
    }
    public OrderSearchPresetRequest<T> selectRequestId(){
       selectProperty(OrderSearchPreset.REQUEST_ID_PROPERTY);
       return this;
    }

    /**
     * fill the requestId with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  requestId) to fetch requestId property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderSearchPresetRequest<T> unselectRequestId(){
       unselectProperty(OrderSearchPreset.REQUEST_ID_PROPERTY);
       return this;
    }
    public OrderSearchPresetRequest<T> selectOwnerUserId(){
       selectProperty(OrderSearchPreset.OWNER_USER_ID_PROPERTY);
       return this;
    }

    /**
     * fill the ownerUserId with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  ownerUserId) to fetch ownerUserId property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderSearchPresetRequest<T> unselectOwnerUserId(){
       unselectProperty(OrderSearchPreset.OWNER_USER_ID_PROPERTY);
       return this;
    }
    public OrderSearchPresetRequest<T> selectCommercePlatformIdOnly(){
       selectProperty(OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> selectCommercePlatform(){
        return selectCommercePlatformWith(Q.commercePlatforms().unlimited().selectSelf());
    }

    public OrderSearchPresetRequest<T> selectCommercePlatformWith(CommercePlatformRequest commercePlatform){
       selectProperty(OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY);
       enhanceRelation(OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY, commercePlatform);
       return this;
    }

    public OrderSearchPresetRequest<T> unselectCommercePlatform(){
       unselectProperty(OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }
    public OrderSearchPresetRequest<T> selectCreateTime(){
       selectProperty(OrderSearchPreset.CREATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the createTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  createTime) to fetch createTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderSearchPresetRequest<T> unselectCreateTime(){
       unselectProperty(OrderSearchPreset.CREATE_TIME_PROPERTY);
       return this;
    }
    public OrderSearchPresetRequest<T> selectUpdateTime(){
       selectProperty(OrderSearchPreset.UPDATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the updateTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  updateTime) to fetch updateTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderSearchPresetRequest<T> unselectUpdateTime(){
       unselectProperty(OrderSearchPreset.UPDATE_TIME_PROPERTY);
       return this;
    }
    public OrderSearchPresetRequest<T> selectVersion(){
       selectProperty(OrderSearchPreset.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public OrderSearchPresetRequest<T> unselectVersion(){
       unselectProperty(OrderSearchPreset.VERSION_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderSearchPreset.ID_PROPERTY, operator, values);
    }

    public OrderSearchPresetRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }
    public OrderSearchPresetRequest<T> withIdIn(Long... id){
       return withId(Operator.EQUAL, (Object[])id);
    }



    public OrderSearchPresetRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public OrderSearchPresetRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public OrderSearchPresetRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public OrderSearchPresetRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderSearchPreset.NAME_PROPERTY, operator, values);
    }

    public OrderSearchPresetRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public OrderSearchPresetRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public OrderSearchPresetRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public OrderSearchPresetRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public OrderSearchPresetRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public OrderSearchPresetRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public OrderSearchPresetRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public OrderSearchPresetRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public OrderSearchPresetRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public OrderSearchPresetRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public OrderSearchPresetRequest<T> filterByFilterJson(String... filterJson){
      if (filterJson == null || filterJson.length == 0) {
        throw new IllegalArgumentException("filterByFilterJson parameter filterJson cannot be empty");
      }
      return appendSearchCriteria(createFilterJsonCriteria(Operator.EQUAL, (Object[])filterJson));
    }

    public OrderSearchPresetRequest<T> withFilterJson(Operator operator, Object... values){
       return appendSearchCriteria(createFilterJsonCriteria(operator, values));
    }

    public OrderSearchPresetRequest<T> withFilterJsonIsUnknown(){
       return withFilterJson(Operator.IS_NULL);
    }

    public OrderSearchPresetRequest<T> withFilterJsonIsKnown(){
       return withFilterJson(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createFilterJsonCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderSearchPreset.FILTER_JSON_PROPERTY, operator, values);
    }

    public OrderSearchPresetRequest<T> withFilterJsonGreaterThan(String filterJson){
       return withFilterJson(Operator.GREATER_THAN, filterJson);
    }

    public OrderSearchPresetRequest<T> withFilterJsonGreaterThanOrEqualTo(String filterJson){
       return withFilterJson(Operator.GREATER_THAN_OR_EQUAL, filterJson);
    }

    public OrderSearchPresetRequest<T> withFilterJsonLessThan(String filterJson){
       return withFilterJson(Operator.LESS_THAN, filterJson);
    }

    public OrderSearchPresetRequest<T> withFilterJsonLessThanOrEqualTo(String filterJson){
       return withFilterJson(Operator.LESS_THAN_OR_EQUAL, filterJson);
    }

    public OrderSearchPresetRequest<T> withFilterJsonBetween(String startOfFilterJson, String endOfFilterJson){
       return withFilterJson(Operator.BETWEEN, startOfFilterJson, endOfFilterJson);
    }
    public OrderSearchPresetRequest<T> withFilterJsonStartingWith(String filterJson){
       return withFilterJson(Operator.BEGIN_WITH, filterJson);
    }
    public OrderSearchPresetRequest<T> withFilterJsonContaining(String filterJson){
       return withFilterJson(Operator.CONTAIN, filterJson);
    }

    public OrderSearchPresetRequest<T> withFilterJsonEndingWith(String filterJson){
       return withFilterJson(Operator.END_WITH, filterJson);
    }

    public OrderSearchPresetRequest<T> withFilterJsonIs(String filterJson){
       return withFilterJson(Operator.EQUAL, filterJson);
    }

    public OrderSearchPresetRequest<T> withFilterJsonSoundingLike(String filterJson){
       return withFilterJson(Operator.SOUNDS_LIKE, filterJson);
    }



    public OrderSearchPresetRequest<T> filterByRequestId(String... requestId){
      if (requestId == null || requestId.length == 0) {
        throw new IllegalArgumentException("filterByRequestId parameter requestId cannot be empty");
      }
      return appendSearchCriteria(createRequestIdCriteria(Operator.EQUAL, (Object[])requestId));
    }

    public OrderSearchPresetRequest<T> withRequestId(Operator operator, Object... values){
       return appendSearchCriteria(createRequestIdCriteria(operator, values));
    }

    public OrderSearchPresetRequest<T> withRequestIdIsUnknown(){
       return withRequestId(Operator.IS_NULL);
    }

    public OrderSearchPresetRequest<T> withRequestIdIsKnown(){
       return withRequestId(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createRequestIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderSearchPreset.REQUEST_ID_PROPERTY, operator, values);
    }

    public OrderSearchPresetRequest<T> withRequestIdGreaterThan(String requestId){
       return withRequestId(Operator.GREATER_THAN, requestId);
    }

    public OrderSearchPresetRequest<T> withRequestIdGreaterThanOrEqualTo(String requestId){
       return withRequestId(Operator.GREATER_THAN_OR_EQUAL, requestId);
    }

    public OrderSearchPresetRequest<T> withRequestIdLessThan(String requestId){
       return withRequestId(Operator.LESS_THAN, requestId);
    }

    public OrderSearchPresetRequest<T> withRequestIdLessThanOrEqualTo(String requestId){
       return withRequestId(Operator.LESS_THAN_OR_EQUAL, requestId);
    }

    public OrderSearchPresetRequest<T> withRequestIdBetween(String startOfRequestId, String endOfRequestId){
       return withRequestId(Operator.BETWEEN, startOfRequestId, endOfRequestId);
    }
    public OrderSearchPresetRequest<T> withRequestIdStartingWith(String requestId){
       return withRequestId(Operator.BEGIN_WITH, requestId);
    }
    public OrderSearchPresetRequest<T> withRequestIdContaining(String requestId){
       return withRequestId(Operator.CONTAIN, requestId);
    }

    public OrderSearchPresetRequest<T> withRequestIdEndingWith(String requestId){
       return withRequestId(Operator.END_WITH, requestId);
    }

    public OrderSearchPresetRequest<T> withRequestIdIs(String requestId){
       return withRequestId(Operator.EQUAL, requestId);
    }

    public OrderSearchPresetRequest<T> withRequestIdSoundingLike(String requestId){
       return withRequestId(Operator.SOUNDS_LIKE, requestId);
    }



    public OrderSearchPresetRequest<T> filterByOwnerUserId(String... ownerUserId){
      if (ownerUserId == null || ownerUserId.length == 0) {
        throw new IllegalArgumentException("filterByOwnerUserId parameter ownerUserId cannot be empty");
      }
      return appendSearchCriteria(createOwnerUserIdCriteria(Operator.EQUAL, (Object[])ownerUserId));
    }

    public OrderSearchPresetRequest<T> withOwnerUserId(Operator operator, Object... values){
       return appendSearchCriteria(createOwnerUserIdCriteria(operator, values));
    }

    public OrderSearchPresetRequest<T> withOwnerUserIdIsUnknown(){
       return withOwnerUserId(Operator.IS_NULL);
    }

    public OrderSearchPresetRequest<T> withOwnerUserIdIsKnown(){
       return withOwnerUserId(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createOwnerUserIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderSearchPreset.OWNER_USER_ID_PROPERTY, operator, values);
    }

    public OrderSearchPresetRequest<T> withOwnerUserIdGreaterThan(String ownerUserId){
       return withOwnerUserId(Operator.GREATER_THAN, ownerUserId);
    }

    public OrderSearchPresetRequest<T> withOwnerUserIdGreaterThanOrEqualTo(String ownerUserId){
       return withOwnerUserId(Operator.GREATER_THAN_OR_EQUAL, ownerUserId);
    }

    public OrderSearchPresetRequest<T> withOwnerUserIdLessThan(String ownerUserId){
       return withOwnerUserId(Operator.LESS_THAN, ownerUserId);
    }

    public OrderSearchPresetRequest<T> withOwnerUserIdLessThanOrEqualTo(String ownerUserId){
       return withOwnerUserId(Operator.LESS_THAN_OR_EQUAL, ownerUserId);
    }

    public OrderSearchPresetRequest<T> withOwnerUserIdBetween(String startOfOwnerUserId, String endOfOwnerUserId){
       return withOwnerUserId(Operator.BETWEEN, startOfOwnerUserId, endOfOwnerUserId);
    }
    public OrderSearchPresetRequest<T> withOwnerUserIdStartingWith(String ownerUserId){
       return withOwnerUserId(Operator.BEGIN_WITH, ownerUserId);
    }
    public OrderSearchPresetRequest<T> withOwnerUserIdContaining(String ownerUserId){
       return withOwnerUserId(Operator.CONTAIN, ownerUserId);
    }

    public OrderSearchPresetRequest<T> withOwnerUserIdEndingWith(String ownerUserId){
       return withOwnerUserId(Operator.END_WITH, ownerUserId);
    }

    public OrderSearchPresetRequest<T> withOwnerUserIdIs(String ownerUserId){
       return withOwnerUserId(Operator.EQUAL, ownerUserId);
    }

    public OrderSearchPresetRequest<T> withOwnerUserIdSoundingLike(String ownerUserId){
       return withOwnerUserId(Operator.SOUNDS_LIKE, ownerUserId);
    }



    public OrderSearchPresetRequest<T> filterByCommercePlatform(CommercePlatform... commercePlatform){
      if (commercePlatform == null || commercePlatform.length == 0) {
        throw new IllegalArgumentException("filterByCommercePlatform parameter commercePlatform cannot be empty");
      }
      return appendSearchCriteria(createCommercePlatformCriteria(Operator.EQUAL, (Object[])commercePlatform));
    }

    public OrderSearchPresetRequest<T> withCommercePlatform(Operator operator, Object... values){
       return appendSearchCriteria(createCommercePlatformCriteria(operator, values));
    }

    public OrderSearchPresetRequest<T> withCommercePlatformIsUnknown(){
       return withCommercePlatform(Operator.IS_NULL);
    }

    public OrderSearchPresetRequest<T> withCommercePlatformIsKnown(){
       return withCommercePlatform(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCommercePlatformCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY, operator, values);
    }

    public OrderSearchPresetRequest<T> filterByCommercePlatform(Long commercePlatform){
      if(commercePlatform == null){
         return this;
      }
      return withCommercePlatform(Operator.EQUAL, commercePlatform);
    }
    public OrderSearchPresetRequest<T> withCommercePlatformMatching(CommercePlatformRequest commercePlatform){
       return appendSearchCriteria(new SubQuerySearchCriteria(OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY, commercePlatform, CommercePlatform.ID_PROPERTY));
    }

    public OrderSearchPresetRequest<T> filterByCreateTime(LocalDateTime... createTime){
      if (createTime == null || createTime.length == 0) {
        throw new IllegalArgumentException("filterByCreateTime parameter createTime cannot be empty");
      }
      return appendSearchCriteria(createCreateTimeCriteria(Operator.EQUAL, (Object[])createTime));
    }

    public OrderSearchPresetRequest<T> withCreateTime(Operator operator, Object... values){
       return appendSearchCriteria(createCreateTimeCriteria(operator, values));
    }

    public OrderSearchPresetRequest<T> withCreateTimeIsUnknown(){
       return withCreateTime(Operator.IS_NULL);
    }

    public OrderSearchPresetRequest<T> withCreateTimeIsKnown(){
       return withCreateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCreateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderSearchPreset.CREATE_TIME_PROPERTY, operator, values);
    }

    public OrderSearchPresetRequest<T> withCreateTimeGreaterThan(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public OrderSearchPresetRequest<T> withCreateTimeGreaterThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN_OR_EQUAL, createTime);
    }

    public OrderSearchPresetRequest<T> withCreateTimeLessThan(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public OrderSearchPresetRequest<T> withCreateTimeLessThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN_OR_EQUAL, createTime);
    }

    public OrderSearchPresetRequest<T> withCreateTimeBetween(LocalDateTime startOfCreateTime, LocalDateTime endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }
    public OrderSearchPresetRequest<T> withCreateTimeBefore(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public OrderSearchPresetRequest<T> withCreateTimeBefore(Date createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public OrderSearchPresetRequest<T> withCreateTimeAfter(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public OrderSearchPresetRequest<T> withCreateTimeAfter(Date createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public OrderSearchPresetRequest<T> withCreateTimeBetween(Date startOfCreateTime, Date endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }




    public OrderSearchPresetRequest<T> filterByUpdateTime(LocalDateTime... updateTime){
      if (updateTime == null || updateTime.length == 0) {
        throw new IllegalArgumentException("filterByUpdateTime parameter updateTime cannot be empty");
      }
      return appendSearchCriteria(createUpdateTimeCriteria(Operator.EQUAL, (Object[])updateTime));
    }

    public OrderSearchPresetRequest<T> withUpdateTime(Operator operator, Object... values){
       return appendSearchCriteria(createUpdateTimeCriteria(operator, values));
    }

    public OrderSearchPresetRequest<T> withUpdateTimeIsUnknown(){
       return withUpdateTime(Operator.IS_NULL);
    }

    public OrderSearchPresetRequest<T> withUpdateTimeIsKnown(){
       return withUpdateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createUpdateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderSearchPreset.UPDATE_TIME_PROPERTY, operator, values);
    }

    public OrderSearchPresetRequest<T> withUpdateTimeGreaterThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public OrderSearchPresetRequest<T> withUpdateTimeGreaterThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN_OR_EQUAL, updateTime);
    }

    public OrderSearchPresetRequest<T> withUpdateTimeLessThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public OrderSearchPresetRequest<T> withUpdateTimeLessThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN_OR_EQUAL, updateTime);
    }

    public OrderSearchPresetRequest<T> withUpdateTimeBetween(LocalDateTime startOfUpdateTime, LocalDateTime endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
    }
    public OrderSearchPresetRequest<T> withUpdateTimeBefore(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public OrderSearchPresetRequest<T> withUpdateTimeBefore(Date updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public OrderSearchPresetRequest<T> withUpdateTimeAfter(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public OrderSearchPresetRequest<T> withUpdateTimeAfter(Date updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public OrderSearchPresetRequest<T> withUpdateTimeBetween(Date startOfUpdateTime, Date endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
    }




    public OrderSearchPresetRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public OrderSearchPresetRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public OrderSearchPresetRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public OrderSearchPresetRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(OrderSearchPreset.VERSION_PROPERTY, operator, values);
    }

    public OrderSearchPresetRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public OrderSearchPresetRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public OrderSearchPresetRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public OrderSearchPresetRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public OrderSearchPresetRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public OrderSearchPresetRequest<T> count(){
        super.count();
        return this;
    }
    public OrderSearchPresetRequest<T> countAs(String retName){
        super.count(retName);
        return this;
    }
    public OrderSearchPresetRequest<T> groupByCommercePlatformWithDetails(){
       return groupByCommercePlatformWithDetails(Q.commercePlatforms().unlimited());
    }

    public OrderSearchPresetRequest<T> groupByCommercePlatformWithDetails(CommercePlatformRequest subRequest){
       aggregate(OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY, subRequest);
       return this;
    }





    public OrderSearchPresetRequest<T> groupById(){
       groupBy(OrderSearchPreset.ID_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByIdAs(String retName){
       groupBy(retName, OrderSearchPreset.ID_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderSearchPreset.ID_PROPERTY, function);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByName(){
       groupBy(OrderSearchPreset.NAME_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByNameAs(String retName){
       groupBy(retName, OrderSearchPreset.NAME_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderSearchPreset.NAME_PROPERTY, function);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByFilterJson(){
       groupBy(OrderSearchPreset.FILTER_JSON_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByFilterJsonAs(String retName){
       groupBy(retName, OrderSearchPreset.FILTER_JSON_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByFilterJsonWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderSearchPreset.FILTER_JSON_PROPERTY, function);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByRequestId(){
       groupBy(OrderSearchPreset.REQUEST_ID_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByRequestIdAs(String retName){
       groupBy(retName, OrderSearchPreset.REQUEST_ID_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByRequestIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderSearchPreset.REQUEST_ID_PROPERTY, function);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByOwnerUserId(){
       groupBy(OrderSearchPreset.OWNER_USER_ID_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByOwnerUserIdAs(String retName){
       groupBy(retName, OrderSearchPreset.OWNER_USER_ID_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByOwnerUserIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderSearchPreset.OWNER_USER_ID_PROPERTY, function);
       return this;
    }
    public OrderSearchPresetRequest<T> groupByCommercePlatformWith(CommercePlatformRequest subRequest){
       groupBy(OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY, subRequest);
       return this;
    }
    public OrderSearchPresetRequest<T> groupByCommercePlatform(){
       groupBy(OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByCommercePlatformAs(String retName){
       groupBy(retName, OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByCommercePlatformWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY, function);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByCreateTime(){
       groupBy(OrderSearchPreset.CREATE_TIME_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByCreateTimeAs(String retName){
       groupBy(retName, OrderSearchPreset.CREATE_TIME_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByCreateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderSearchPreset.CREATE_TIME_PROPERTY, function);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByUpdateTime(){
       groupBy(OrderSearchPreset.UPDATE_TIME_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByUpdateTimeAs(String retName){
       groupBy(retName, OrderSearchPreset.UPDATE_TIME_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByUpdateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderSearchPreset.UPDATE_TIME_PROPERTY, function);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByVersion(){
       groupBy(OrderSearchPreset.VERSION_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByVersionAs(String retName){
       groupBy(retName, OrderSearchPreset.VERSION_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, OrderSearchPreset.VERSION_PROPERTY, function);
       return this;
    }



    public OrderSearchPresetRequest<T> orderByIdAscending(){
       addOrderByAscending(OrderSearchPreset.ID_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> orderByIdDescending(){
       addOrderByDescending(OrderSearchPreset.ID_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> orderByNameAscending(){
       addOrderByAscending(OrderSearchPreset.NAME_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> orderByNameDescending(){
       addOrderByDescending(OrderSearchPreset.NAME_PROPERTY);
       return this;
    }
    public OrderSearchPresetRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(OrderSearchPreset.NAME_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(OrderSearchPreset.NAME_PROPERTY);
       return this;
    }
    public OrderSearchPresetRequest<T> orderByFilterJsonAscending(){
       addOrderByAscending(OrderSearchPreset.FILTER_JSON_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> orderByFilterJsonDescending(){
       addOrderByDescending(OrderSearchPreset.FILTER_JSON_PROPERTY);
       return this;
    }
    public OrderSearchPresetRequest<T> orderByFilterJsonAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(OrderSearchPreset.FILTER_JSON_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> orderByFilterJsonDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(OrderSearchPreset.FILTER_JSON_PROPERTY);
       return this;
    }
    public OrderSearchPresetRequest<T> orderByRequestIdAscending(){
       addOrderByAscending(OrderSearchPreset.REQUEST_ID_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> orderByRequestIdDescending(){
       addOrderByDescending(OrderSearchPreset.REQUEST_ID_PROPERTY);
       return this;
    }
    public OrderSearchPresetRequest<T> orderByRequestIdAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(OrderSearchPreset.REQUEST_ID_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> orderByRequestIdDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(OrderSearchPreset.REQUEST_ID_PROPERTY);
       return this;
    }
    public OrderSearchPresetRequest<T> orderByOwnerUserIdAscending(){
       addOrderByAscending(OrderSearchPreset.OWNER_USER_ID_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> orderByOwnerUserIdDescending(){
       addOrderByDescending(OrderSearchPreset.OWNER_USER_ID_PROPERTY);
       return this;
    }
    public OrderSearchPresetRequest<T> orderByOwnerUserIdAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(OrderSearchPreset.OWNER_USER_ID_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> orderByOwnerUserIdDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(OrderSearchPreset.OWNER_USER_ID_PROPERTY);
       return this;
    }
    public OrderSearchPresetRequest<T> orderByCommercePlatformAscending(){
       addOrderByAscending(OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> orderByCommercePlatformDescending(){
       addOrderByDescending(OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> orderByCreateTimeAscending(){
       addOrderByAscending(OrderSearchPreset.CREATE_TIME_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> orderByCreateTimeDescending(){
       addOrderByDescending(OrderSearchPreset.CREATE_TIME_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> orderByUpdateTimeAscending(){
       addOrderByAscending(OrderSearchPreset.UPDATE_TIME_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> orderByUpdateTimeDescending(){
       addOrderByDescending(OrderSearchPreset.UPDATE_TIME_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> orderByVersionAscending(){
       addOrderByAscending(OrderSearchPreset.VERSION_PROPERTY);
       return this;
    }

    public OrderSearchPresetRequest<T> orderByVersionDescending(){
       addOrderByDescending(OrderSearchPreset.VERSION_PROPERTY);
       return this;
    }


    public CommercePlatformRequest rollUpToCommercePlatform(){
       CommercePlatformRequest commercePlatform = Q.commercePlatforms().unlimited();
       this.withCommercePlatformMatching(commercePlatform)
           .groupByCommercePlatformWith(commercePlatform);
       return commercePlatform;
    }





   public OrderSearchPresetRequest<T> facetByCommercePlatformAs(String facetName, CommercePlatformRequest commercePlatform){
       return facetByCommercePlatformAs(facetName, commercePlatform, true);
   }

   public OrderSearchPresetRequest<T> facetByCommercePlatformAs(String facetName, CommercePlatformRequest commercePlatform, boolean includeAllFacets){
       addFacet(facetName, OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY, commercePlatform, includeAllFacets);
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public OrderSearchPresetRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public OrderSearchPresetRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public OrderSearchPresetRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public OrderSearchPresetRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public OrderSearchPresetRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}