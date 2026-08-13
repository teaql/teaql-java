
package com.teaql.ordermanagementservice.product;

import com.teaql.ordermanagementservice.Q;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformRequest;
import com.teaql.ordermanagementservice.orderline.OrderLine;
import com.teaql.ordermanagementservice.orderline.OrderLineRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.time.LocalDateTime;
import java.util.Date;

public class ProductRequest<T extends Product> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    public ProductRequest(Class<T> returnType){
        super(returnType);
        selectId();
        selectVersion();
    }

    public ProductRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public ProductRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public ProductRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public ProductRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public ProductRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    public ProductRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (ProductRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public ProductRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public ProductRequest<T> matchingAnyOf(ProductRequest product){
        super.internalMatchAny(product);
        return this;
    }

    public ProductRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public ProductRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public ProductRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public ProductRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectName().selectSku().selectImageUrl().selectCommercePlatformIdOnly().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public ProductRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public ProductRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectName().selectSku().selectImageUrl().selectCommercePlatform().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public ProductRequest<T> selectChildren(){
        super.selectAny();
        selectOrderLineList();
        return selectId().selectName().selectSku().selectImageUrl().selectCommercePlatform().selectCreateTime().selectUpdateTime().selectVersion();
    }


    public ProductRequest<T> selectId(){
       selectProperty(Product.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ProductRequest<T> unselectId(){
       unselectProperty(Product.ID_PROPERTY);
       return this;
    }
    public ProductRequest<T> selectName(){
       selectProperty(Product.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ProductRequest<T> unselectName(){
       unselectProperty(Product.NAME_PROPERTY);
       return this;
    }
    public ProductRequest<T> selectSku(){
       selectProperty(Product.SKU_PROPERTY);
       return this;
    }

    /**
     * fill the sku with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  sku) to fetch sku property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ProductRequest<T> unselectSku(){
       unselectProperty(Product.SKU_PROPERTY);
       return this;
    }
    public ProductRequest<T> selectImageUrl(){
       selectProperty(Product.IMAGE_URL_PROPERTY);
       return this;
    }

    /**
     * fill the imageUrl with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  imageUrl) to fetch imageUrl property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ProductRequest<T> unselectImageUrl(){
       unselectProperty(Product.IMAGE_URL_PROPERTY);
       return this;
    }
    public ProductRequest<T> selectCommercePlatformIdOnly(){
       selectProperty(Product.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public ProductRequest<T> selectCommercePlatform(){
        return selectCommercePlatformWith(Q.commercePlatforms().unlimited().selectSelf());
    }

    public ProductRequest<T> selectCommercePlatformWith(CommercePlatformRequest commercePlatform){
       selectProperty(Product.COMMERCE_PLATFORM_PROPERTY);
       enhanceRelation(Product.COMMERCE_PLATFORM_PROPERTY, commercePlatform);
       return this;
    }

    public ProductRequest<T> unselectCommercePlatform(){
       unselectProperty(Product.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }
    public ProductRequest<T> selectCreateTime(){
       selectProperty(Product.CREATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the createTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  createTime) to fetch createTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ProductRequest<T> unselectCreateTime(){
       unselectProperty(Product.CREATE_TIME_PROPERTY);
       return this;
    }
    public ProductRequest<T> selectUpdateTime(){
       selectProperty(Product.UPDATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the updateTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  updateTime) to fetch updateTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ProductRequest<T> unselectUpdateTime(){
       unselectProperty(Product.UPDATE_TIME_PROPERTY);
       return this;
    }
    public ProductRequest<T> selectVersion(){
       selectProperty(Product.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ProductRequest<T> unselectVersion(){
       unselectProperty(Product.VERSION_PROPERTY);
       return this;
    }
    public ProductRequest<T> selectOrderLineList(){
       return selectOrderLineListWith(Q.orderLines().selectSelf());
    }

    public ProductRequest<T> selectOrderLineListWith(OrderLineRequest orderLineList){
       enhanceRelation(Product.ORDER_LINE_LIST_PROPERTY, orderLineList);
       return this;
    }

    public ProductRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Product.ID_PROPERTY, operator, values);
    }

    public ProductRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }
    public ProductRequest<T> withIdIn(Long... id){
       return withId(Operator.EQUAL, (Object[])id);
    }



    public ProductRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public ProductRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public ProductRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public ProductRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Product.NAME_PROPERTY, operator, values);
    }

    public ProductRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public ProductRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public ProductRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public ProductRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public ProductRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public ProductRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public ProductRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public ProductRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public ProductRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public ProductRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public ProductRequest<T> filterBySku(String... sku){
      if (sku == null || sku.length == 0) {
        throw new IllegalArgumentException("filterBySku parameter sku cannot be empty");
      }
      return appendSearchCriteria(createSkuCriteria(Operator.EQUAL, (Object[])sku));
    }

    public ProductRequest<T> withSku(Operator operator, Object... values){
       return appendSearchCriteria(createSkuCriteria(operator, values));
    }

    public ProductRequest<T> withSkuIsUnknown(){
       return withSku(Operator.IS_NULL);
    }

    public ProductRequest<T> withSkuIsKnown(){
       return withSku(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createSkuCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Product.SKU_PROPERTY, operator, values);
    }

    public ProductRequest<T> withSkuGreaterThan(String sku){
       return withSku(Operator.GREATER_THAN, sku);
    }

    public ProductRequest<T> withSkuGreaterThanOrEqualTo(String sku){
       return withSku(Operator.GREATER_THAN_OR_EQUAL, sku);
    }

    public ProductRequest<T> withSkuLessThan(String sku){
       return withSku(Operator.LESS_THAN, sku);
    }

    public ProductRequest<T> withSkuLessThanOrEqualTo(String sku){
       return withSku(Operator.LESS_THAN_OR_EQUAL, sku);
    }

    public ProductRequest<T> withSkuBetween(String startOfSku, String endOfSku){
       return withSku(Operator.BETWEEN, startOfSku, endOfSku);
    }
    public ProductRequest<T> withSkuStartingWith(String sku){
       return withSku(Operator.BEGIN_WITH, sku);
    }
    public ProductRequest<T> withSkuContaining(String sku){
       return withSku(Operator.CONTAIN, sku);
    }

    public ProductRequest<T> withSkuEndingWith(String sku){
       return withSku(Operator.END_WITH, sku);
    }

    public ProductRequest<T> withSkuIs(String sku){
       return withSku(Operator.EQUAL, sku);
    }

    public ProductRequest<T> withSkuSoundingLike(String sku){
       return withSku(Operator.SOUNDS_LIKE, sku);
    }



    public ProductRequest<T> filterByImageUrl(String... imageUrl){
      if (imageUrl == null || imageUrl.length == 0) {
        throw new IllegalArgumentException("filterByImageUrl parameter imageUrl cannot be empty");
      }
      return appendSearchCriteria(createImageUrlCriteria(Operator.EQUAL, (Object[])imageUrl));
    }

    public ProductRequest<T> withImageUrl(Operator operator, Object... values){
       return appendSearchCriteria(createImageUrlCriteria(operator, values));
    }

    public ProductRequest<T> withImageUrlIsUnknown(){
       return withImageUrl(Operator.IS_NULL);
    }

    public ProductRequest<T> withImageUrlIsKnown(){
       return withImageUrl(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createImageUrlCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Product.IMAGE_URL_PROPERTY, operator, values);
    }

    public ProductRequest<T> withImageUrlGreaterThan(String imageUrl){
       return withImageUrl(Operator.GREATER_THAN, imageUrl);
    }

    public ProductRequest<T> withImageUrlGreaterThanOrEqualTo(String imageUrl){
       return withImageUrl(Operator.GREATER_THAN_OR_EQUAL, imageUrl);
    }

    public ProductRequest<T> withImageUrlLessThan(String imageUrl){
       return withImageUrl(Operator.LESS_THAN, imageUrl);
    }

    public ProductRequest<T> withImageUrlLessThanOrEqualTo(String imageUrl){
       return withImageUrl(Operator.LESS_THAN_OR_EQUAL, imageUrl);
    }

    public ProductRequest<T> withImageUrlBetween(String startOfImageUrl, String endOfImageUrl){
       return withImageUrl(Operator.BETWEEN, startOfImageUrl, endOfImageUrl);
    }
    public ProductRequest<T> withImageUrlStartingWith(String imageUrl){
       return withImageUrl(Operator.BEGIN_WITH, imageUrl);
    }
    public ProductRequest<T> withImageUrlContaining(String imageUrl){
       return withImageUrl(Operator.CONTAIN, imageUrl);
    }

    public ProductRequest<T> withImageUrlEndingWith(String imageUrl){
       return withImageUrl(Operator.END_WITH, imageUrl);
    }

    public ProductRequest<T> withImageUrlIs(String imageUrl){
       return withImageUrl(Operator.EQUAL, imageUrl);
    }

    public ProductRequest<T> withImageUrlSoundingLike(String imageUrl){
       return withImageUrl(Operator.SOUNDS_LIKE, imageUrl);
    }



    public ProductRequest<T> filterByCommercePlatform(CommercePlatform... commercePlatform){
      if (commercePlatform == null || commercePlatform.length == 0) {
        throw new IllegalArgumentException("filterByCommercePlatform parameter commercePlatform cannot be empty");
      }
      return appendSearchCriteria(createCommercePlatformCriteria(Operator.EQUAL, (Object[])commercePlatform));
    }

    public ProductRequest<T> withCommercePlatform(Operator operator, Object... values){
       return appendSearchCriteria(createCommercePlatformCriteria(operator, values));
    }

    public ProductRequest<T> withCommercePlatformIsUnknown(){
       return withCommercePlatform(Operator.IS_NULL);
    }

    public ProductRequest<T> withCommercePlatformIsKnown(){
       return withCommercePlatform(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCommercePlatformCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Product.COMMERCE_PLATFORM_PROPERTY, operator, values);
    }

    public ProductRequest<T> filterByCommercePlatform(Long commercePlatform){
      if(commercePlatform == null){
         return this;
      }
      return withCommercePlatform(Operator.EQUAL, commercePlatform);
    }
    public ProductRequest<T> withCommercePlatformMatching(CommercePlatformRequest commercePlatform){
       return appendSearchCriteria(new SubQuerySearchCriteria(Product.COMMERCE_PLATFORM_PROPERTY, commercePlatform, CommercePlatform.ID_PROPERTY));
    }

    public ProductRequest<T> filterByCreateTime(LocalDateTime... createTime){
      if (createTime == null || createTime.length == 0) {
        throw new IllegalArgumentException("filterByCreateTime parameter createTime cannot be empty");
      }
      return appendSearchCriteria(createCreateTimeCriteria(Operator.EQUAL, (Object[])createTime));
    }

    public ProductRequest<T> withCreateTime(Operator operator, Object... values){
       return appendSearchCriteria(createCreateTimeCriteria(operator, values));
    }

    public ProductRequest<T> withCreateTimeIsUnknown(){
       return withCreateTime(Operator.IS_NULL);
    }

    public ProductRequest<T> withCreateTimeIsKnown(){
       return withCreateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCreateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Product.CREATE_TIME_PROPERTY, operator, values);
    }

    public ProductRequest<T> withCreateTimeGreaterThan(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public ProductRequest<T> withCreateTimeGreaterThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN_OR_EQUAL, createTime);
    }

    public ProductRequest<T> withCreateTimeLessThan(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public ProductRequest<T> withCreateTimeLessThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN_OR_EQUAL, createTime);
    }

    public ProductRequest<T> withCreateTimeBetween(LocalDateTime startOfCreateTime, LocalDateTime endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }
    public ProductRequest<T> withCreateTimeBefore(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public ProductRequest<T> withCreateTimeBefore(Date createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public ProductRequest<T> withCreateTimeAfter(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public ProductRequest<T> withCreateTimeAfter(Date createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public ProductRequest<T> withCreateTimeBetween(Date startOfCreateTime, Date endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }




    public ProductRequest<T> filterByUpdateTime(LocalDateTime... updateTime){
      if (updateTime == null || updateTime.length == 0) {
        throw new IllegalArgumentException("filterByUpdateTime parameter updateTime cannot be empty");
      }
      return appendSearchCriteria(createUpdateTimeCriteria(Operator.EQUAL, (Object[])updateTime));
    }

    public ProductRequest<T> withUpdateTime(Operator operator, Object... values){
       return appendSearchCriteria(createUpdateTimeCriteria(operator, values));
    }

    public ProductRequest<T> withUpdateTimeIsUnknown(){
       return withUpdateTime(Operator.IS_NULL);
    }

    public ProductRequest<T> withUpdateTimeIsKnown(){
       return withUpdateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createUpdateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Product.UPDATE_TIME_PROPERTY, operator, values);
    }

    public ProductRequest<T> withUpdateTimeGreaterThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public ProductRequest<T> withUpdateTimeGreaterThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN_OR_EQUAL, updateTime);
    }

    public ProductRequest<T> withUpdateTimeLessThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public ProductRequest<T> withUpdateTimeLessThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN_OR_EQUAL, updateTime);
    }

    public ProductRequest<T> withUpdateTimeBetween(LocalDateTime startOfUpdateTime, LocalDateTime endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
    }
    public ProductRequest<T> withUpdateTimeBefore(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public ProductRequest<T> withUpdateTimeBefore(Date updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public ProductRequest<T> withUpdateTimeAfter(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public ProductRequest<T> withUpdateTimeAfter(Date updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public ProductRequest<T> withUpdateTimeBetween(Date startOfUpdateTime, Date endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
    }




    public ProductRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public ProductRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public ProductRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public ProductRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Product.VERSION_PROPERTY, operator, values);
    }

    public ProductRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public ProductRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public ProductRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public ProductRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public ProductRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }

    public ProductRequest<T> withOrderLineListMatching(OrderLineRequest orderLineRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(Product.ID_PROPERTY, orderLineRequest, OrderLine.PRODUCT_PROPERTY));
    }

    public ProductRequest<T> withoutOrderLineListMatching(OrderLineRequest orderLineRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(Product.ID_PROPERTY, orderLineRequest, OrderLine.PRODUCT_PROPERTY)));
    }

    public ProductRequest<T> haveOrderLines(){
        return withOrderLineListMatching(Q.orderLines().unlimited());
    }

    public ProductRequest<T> haveNoOrderLines(){
        return withoutOrderLineListMatching(Q.orderLines().unlimited());
    }

    public ProductRequest<T> count(){
        super.count();
        return this;
    }
    public ProductRequest<T> countAs(String retName){
        super.count(retName);
        return this;
    }
    public ProductRequest<T> groupByCommercePlatformWithDetails(){
       return groupByCommercePlatformWithDetails(Q.commercePlatforms().unlimited());
    }

    public ProductRequest<T> groupByCommercePlatformWithDetails(CommercePlatformRequest subRequest){
       aggregate(Product.COMMERCE_PLATFORM_PROPERTY, subRequest);
       return this;
    }




    public ProductRequest<T> groupByOrderLinesWithDetails(OrderLineRequest subRequest){
       aggregate(Product.ORDER_LINE_LIST_PROPERTY, subRequest);
       return this;
    }

    public ProductRequest<T> groupById(){
       groupBy(Product.ID_PROPERTY);
       return this;
    }

    public ProductRequest<T> groupByIdAs(String retName){
       groupBy(retName, Product.ID_PROPERTY);
       return this;
    }

    public ProductRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, Product.ID_PROPERTY, function);
       return this;
    }

    public ProductRequest<T> groupByName(){
       groupBy(Product.NAME_PROPERTY);
       return this;
    }

    public ProductRequest<T> groupByNameAs(String retName){
       groupBy(retName, Product.NAME_PROPERTY);
       return this;
    }

    public ProductRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, Product.NAME_PROPERTY, function);
       return this;
    }

    public ProductRequest<T> groupBySku(){
       groupBy(Product.SKU_PROPERTY);
       return this;
    }

    public ProductRequest<T> groupBySkuAs(String retName){
       groupBy(retName, Product.SKU_PROPERTY);
       return this;
    }

    public ProductRequest<T> groupBySkuWithFunction(String retName, AggrFunction function){
       groupBy(retName, Product.SKU_PROPERTY, function);
       return this;
    }

    public ProductRequest<T> groupByImageUrl(){
       groupBy(Product.IMAGE_URL_PROPERTY);
       return this;
    }

    public ProductRequest<T> groupByImageUrlAs(String retName){
       groupBy(retName, Product.IMAGE_URL_PROPERTY);
       return this;
    }

    public ProductRequest<T> groupByImageUrlWithFunction(String retName, AggrFunction function){
       groupBy(retName, Product.IMAGE_URL_PROPERTY, function);
       return this;
    }
    public ProductRequest<T> groupByCommercePlatformWith(CommercePlatformRequest subRequest){
       groupBy(Product.COMMERCE_PLATFORM_PROPERTY, subRequest);
       return this;
    }
    public ProductRequest<T> groupByCommercePlatform(){
       groupBy(Product.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public ProductRequest<T> groupByCommercePlatformAs(String retName){
       groupBy(retName, Product.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public ProductRequest<T> groupByCommercePlatformWithFunction(String retName, AggrFunction function){
       groupBy(retName, Product.COMMERCE_PLATFORM_PROPERTY, function);
       return this;
    }

    public ProductRequest<T> groupByCreateTime(){
       groupBy(Product.CREATE_TIME_PROPERTY);
       return this;
    }

    public ProductRequest<T> groupByCreateTimeAs(String retName){
       groupBy(retName, Product.CREATE_TIME_PROPERTY);
       return this;
    }

    public ProductRequest<T> groupByCreateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, Product.CREATE_TIME_PROPERTY, function);
       return this;
    }

    public ProductRequest<T> groupByUpdateTime(){
       groupBy(Product.UPDATE_TIME_PROPERTY);
       return this;
    }

    public ProductRequest<T> groupByUpdateTimeAs(String retName){
       groupBy(retName, Product.UPDATE_TIME_PROPERTY);
       return this;
    }

    public ProductRequest<T> groupByUpdateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, Product.UPDATE_TIME_PROPERTY, function);
       return this;
    }

    public ProductRequest<T> groupByVersion(){
       groupBy(Product.VERSION_PROPERTY);
       return this;
    }

    public ProductRequest<T> groupByVersionAs(String retName){
       groupBy(retName, Product.VERSION_PROPERTY);
       return this;
    }

    public ProductRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, Product.VERSION_PROPERTY, function);
       return this;
    }



    public ProductRequest<T> orderByIdAscending(){
       addOrderByAscending(Product.ID_PROPERTY);
       return this;
    }

    public ProductRequest<T> orderByIdDescending(){
       addOrderByDescending(Product.ID_PROPERTY);
       return this;
    }

    public ProductRequest<T> orderByNameAscending(){
       addOrderByAscending(Product.NAME_PROPERTY);
       return this;
    }

    public ProductRequest<T> orderByNameDescending(){
       addOrderByDescending(Product.NAME_PROPERTY);
       return this;
    }
    public ProductRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Product.NAME_PROPERTY);
       return this;
    }

    public ProductRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Product.NAME_PROPERTY);
       return this;
    }
    public ProductRequest<T> orderBySkuAscending(){
       addOrderByAscending(Product.SKU_PROPERTY);
       return this;
    }

    public ProductRequest<T> orderBySkuDescending(){
       addOrderByDescending(Product.SKU_PROPERTY);
       return this;
    }
    public ProductRequest<T> orderBySkuAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Product.SKU_PROPERTY);
       return this;
    }

    public ProductRequest<T> orderBySkuDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Product.SKU_PROPERTY);
       return this;
    }
    public ProductRequest<T> orderByImageUrlAscending(){
       addOrderByAscending(Product.IMAGE_URL_PROPERTY);
       return this;
    }

    public ProductRequest<T> orderByImageUrlDescending(){
       addOrderByDescending(Product.IMAGE_URL_PROPERTY);
       return this;
    }
    public ProductRequest<T> orderByImageUrlAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Product.IMAGE_URL_PROPERTY);
       return this;
    }

    public ProductRequest<T> orderByImageUrlDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Product.IMAGE_URL_PROPERTY);
       return this;
    }
    public ProductRequest<T> orderByCommercePlatformAscending(){
       addOrderByAscending(Product.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public ProductRequest<T> orderByCommercePlatformDescending(){
       addOrderByDescending(Product.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public ProductRequest<T> orderByCreateTimeAscending(){
       addOrderByAscending(Product.CREATE_TIME_PROPERTY);
       return this;
    }

    public ProductRequest<T> orderByCreateTimeDescending(){
       addOrderByDescending(Product.CREATE_TIME_PROPERTY);
       return this;
    }

    public ProductRequest<T> orderByUpdateTimeAscending(){
       addOrderByAscending(Product.UPDATE_TIME_PROPERTY);
       return this;
    }

    public ProductRequest<T> orderByUpdateTimeDescending(){
       addOrderByDescending(Product.UPDATE_TIME_PROPERTY);
       return this;
    }

    public ProductRequest<T> orderByVersionAscending(){
       addOrderByAscending(Product.VERSION_PROPERTY);
       return this;
    }

    public ProductRequest<T> orderByVersionDescending(){
       addOrderByDescending(Product.VERSION_PROPERTY);
       return this;
    }


    public ProductRequest<T> statsFromOrderLinesAs(String name, OrderLineRequest subRequest){
       return statsFromOrderLinesAs(name, subRequest, false);
    }

    public ProductRequest<T> statsFromOrderLinesAs(String name, OrderLineRequest subRequest, boolean singleResult){
       subRequest.setPartitionProperty(OrderLine.PRODUCT_PROPERTY);
       addAggregateDynamicProperty(name, subRequest, singleResult);
       return this;
    }

    public ProductRequest<T> statsFromOrderLines(OrderLineRequest subRequest){
       return statsFromOrderLinesAs(REFINEMENTS, subRequest);
    }
    public CommercePlatformRequest rollUpToCommercePlatform(){
       CommercePlatformRequest commercePlatform = Q.commercePlatforms().unlimited();
       this.withCommercePlatformMatching(commercePlatform)
           .groupByCommercePlatformWith(commercePlatform);
       return commercePlatform;
    }




    public ProductRequest<T> countOrderLines(){
        return countOrderLinesAs("Count");
    }

    public ProductRequest<T> countOrderLinesAs(String name){
        return countOrderLinesWith(name, Q.orderLines().unlimited());
    }

    public ProductRequest<T> countOrderLinesWith(String name, OrderLineRequest subRequest){
        return statsFromOrderLinesAs(name, subRequest.count(), true);
    }
    public ProductRequest<T> minQuantityOfOrderLines(){
        return minQuantityOfOrderLinesAs("minQuantityOfOrderLines");
    }

    public ProductRequest<T> minQuantityOfOrderLinesAs(String name){
        return minQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public ProductRequest<T> minQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        return statsFromOrderLinesAs(name, subRequest.minQuantity(), true);
    }
    public ProductRequest<T> maxQuantityOfOrderLines(){
        return maxQuantityOfOrderLinesAs("maxQuantityOfOrderLines");
    }

    public ProductRequest<T> maxQuantityOfOrderLinesAs(String name){
        return maxQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public ProductRequest<T> maxQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        return statsFromOrderLinesAs(name, subRequest.maxQuantity(), true);
    }
    public ProductRequest<T> sumQuantityOfOrderLines(){
        return sumQuantityOfOrderLinesAs("sumQuantityOfOrderLines");
    }

    public ProductRequest<T> sumQuantityOfOrderLinesAs(String name){
        return sumQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public ProductRequest<T> sumQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        return statsFromOrderLinesAs(name, subRequest.sumQuantity(), true);
    }
    public ProductRequest<T> avgQuantityOfOrderLines(){
        return avgQuantityOfOrderLinesAs("avgQuantityOfOrderLines");
    }

    public ProductRequest<T> avgQuantityOfOrderLinesAs(String name){
        return avgQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public ProductRequest<T> avgQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        return statsFromOrderLinesAs(name, subRequest.avgQuantity(), true);
    }
    public ProductRequest<T> standardDeviationQuantityOfOrderLines(){
        return standardDeviationQuantityOfOrderLinesAs("stdDevQuantityOfOrderLines");
    }

    public ProductRequest<T> standardDeviationQuantityOfOrderLinesAs(String name){
        return standardDeviationQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public ProductRequest<T> standardDeviationQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        return statsFromOrderLinesAs(name, subRequest.standardDeviationQuantity(), true);
    }
    public ProductRequest<T> squareRootOfPopulationStandardDeviationQuantityOfOrderLines(){
        return squareRootOfPopulationStandardDeviationQuantityOfOrderLinesAs("stdDevPopQuantityOfOrderLines");
    }

    public ProductRequest<T> squareRootOfPopulationStandardDeviationQuantityOfOrderLinesAs(String name){
        return squareRootOfPopulationStandardDeviationQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public ProductRequest<T> squareRootOfPopulationStandardDeviationQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        return statsFromOrderLinesAs(name, subRequest.squareRootOfPopulationStandardDeviationQuantity(), true);
    }
    public ProductRequest<T> sampleVarianceQuantityOfOrderLines(){
        return sampleVarianceQuantityOfOrderLinesAs("varSampQuantityOfOrderLines");
    }

    public ProductRequest<T> sampleVarianceQuantityOfOrderLinesAs(String name){
        return sampleVarianceQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public ProductRequest<T> sampleVarianceQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        return statsFromOrderLinesAs(name, subRequest.sampleVarianceQuantity(), true);
    }
    public ProductRequest<T> samplePopulationVarianceQuantityOfOrderLines(){
        return samplePopulationVarianceQuantityOfOrderLinesAs("varPopQuantityOfOrderLines");
    }

    public ProductRequest<T> samplePopulationVarianceQuantityOfOrderLinesAs(String name){
        return samplePopulationVarianceQuantityOfOrderLinesAs(name, Q.orderLines().unlimited());
    }

    public ProductRequest<T> samplePopulationVarianceQuantityOfOrderLinesAs(String name, OrderLineRequest subRequest){
        return statsFromOrderLinesAs(name, subRequest.samplePopulationVarianceQuantity(), true);
    }

   public ProductRequest<T> facetByCommercePlatformAs(String facetName, CommercePlatformRequest commercePlatform){
       return facetByCommercePlatformAs(facetName, commercePlatform, true);
   }

   public ProductRequest<T> facetByCommercePlatformAs(String facetName, CommercePlatformRequest commercePlatform, boolean includeAllFacets){
       addFacet(facetName, Product.COMMERCE_PLATFORM_PROPERTY, commercePlatform, includeAllFacets);
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public ProductRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public ProductRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public ProductRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public ProductRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public ProductRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}