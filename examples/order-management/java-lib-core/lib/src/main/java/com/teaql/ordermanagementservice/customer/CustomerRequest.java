
package com.teaql.ordermanagementservice.customer;

import com.teaql.ordermanagementservice.Q;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformRequest;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.customerorder.CustomerOrderRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.time.LocalDateTime;
import java.util.Date;

public class CustomerRequest<T extends Customer> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    public CustomerRequest(Class<T> returnType){
        super(returnType);
        selectId();
        selectVersion();
    }

    public CustomerRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public CustomerRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public CustomerRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public CustomerRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public CustomerRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    public CustomerRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (CustomerRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public CustomerRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public CustomerRequest<T> matchingAnyOf(CustomerRequest customer){
        super.internalMatchAny(customer);
        return this;
    }

    public CustomerRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public CustomerRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public CustomerRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public CustomerRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectName().selectEmail().selectCommercePlatformIdOnly().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public CustomerRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public CustomerRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectName().selectEmail().selectCommercePlatform().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public CustomerRequest<T> selectChildren(){
        super.selectAny();
        selectCustomerOrderList();
        return selectId().selectName().selectEmail().selectCommercePlatform().selectCreateTime().selectUpdateTime().selectVersion();
    }


    public CustomerRequest<T> selectId(){
       selectProperty(Customer.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerRequest<T> unselectId(){
       unselectProperty(Customer.ID_PROPERTY);
       return this;
    }
    public CustomerRequest<T> selectName(){
       selectProperty(Customer.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerRequest<T> unselectName(){
       unselectProperty(Customer.NAME_PROPERTY);
       return this;
    }
    public CustomerRequest<T> selectEmail(){
       selectProperty(Customer.EMAIL_PROPERTY);
       return this;
    }

    /**
     * fill the email with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  email) to fetch email property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerRequest<T> unselectEmail(){
       unselectProperty(Customer.EMAIL_PROPERTY);
       return this;
    }
    public CustomerRequest<T> selectCommercePlatformIdOnly(){
       selectProperty(Customer.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public CustomerRequest<T> selectCommercePlatform(){
        return selectCommercePlatformWith(Q.commercePlatforms().unlimited().selectSelf());
    }

    public CustomerRequest<T> selectCommercePlatformWith(CommercePlatformRequest commercePlatform){
       selectProperty(Customer.COMMERCE_PLATFORM_PROPERTY);
       enhanceRelation(Customer.COMMERCE_PLATFORM_PROPERTY, commercePlatform);
       return this;
    }

    public CustomerRequest<T> unselectCommercePlatform(){
       unselectProperty(Customer.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }
    public CustomerRequest<T> selectCreateTime(){
       selectProperty(Customer.CREATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the createTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  createTime) to fetch createTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerRequest<T> unselectCreateTime(){
       unselectProperty(Customer.CREATE_TIME_PROPERTY);
       return this;
    }
    public CustomerRequest<T> selectUpdateTime(){
       selectProperty(Customer.UPDATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the updateTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  updateTime) to fetch updateTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerRequest<T> unselectUpdateTime(){
       unselectProperty(Customer.UPDATE_TIME_PROPERTY);
       return this;
    }
    public CustomerRequest<T> selectVersion(){
       selectProperty(Customer.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public CustomerRequest<T> unselectVersion(){
       unselectProperty(Customer.VERSION_PROPERTY);
       return this;
    }
    public CustomerRequest<T> selectCustomerOrderList(){
       return selectCustomerOrderListWith(Q.customerOrders().selectSelf());
    }

    public CustomerRequest<T> selectCustomerOrderListWith(CustomerOrderRequest customerOrderList){
       enhanceRelation(Customer.CUSTOMER_ORDER_LIST_PROPERTY, customerOrderList);
       return this;
    }

    public CustomerRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Customer.ID_PROPERTY, operator, values);
    }

    public CustomerRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }
    public CustomerRequest<T> withIdIn(Long... id){
       return withId(Operator.EQUAL, (Object[])id);
    }



    public CustomerRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public CustomerRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public CustomerRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public CustomerRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Customer.NAME_PROPERTY, operator, values);
    }

    public CustomerRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public CustomerRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public CustomerRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public CustomerRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public CustomerRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public CustomerRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public CustomerRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public CustomerRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public CustomerRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public CustomerRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public CustomerRequest<T> filterByEmail(String... email){
      if (email == null || email.length == 0) {
        throw new IllegalArgumentException("filterByEmail parameter email cannot be empty");
      }
      return appendSearchCriteria(createEmailCriteria(Operator.EQUAL, (Object[])email));
    }

    public CustomerRequest<T> withEmail(Operator operator, Object... values){
       return appendSearchCriteria(createEmailCriteria(operator, values));
    }

    public CustomerRequest<T> withEmailIsUnknown(){
       return withEmail(Operator.IS_NULL);
    }

    public CustomerRequest<T> withEmailIsKnown(){
       return withEmail(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createEmailCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Customer.EMAIL_PROPERTY, operator, values);
    }

    public CustomerRequest<T> withEmailGreaterThan(String email){
       return withEmail(Operator.GREATER_THAN, email);
    }

    public CustomerRequest<T> withEmailGreaterThanOrEqualTo(String email){
       return withEmail(Operator.GREATER_THAN_OR_EQUAL, email);
    }

    public CustomerRequest<T> withEmailLessThan(String email){
       return withEmail(Operator.LESS_THAN, email);
    }

    public CustomerRequest<T> withEmailLessThanOrEqualTo(String email){
       return withEmail(Operator.LESS_THAN_OR_EQUAL, email);
    }

    public CustomerRequest<T> withEmailBetween(String startOfEmail, String endOfEmail){
       return withEmail(Operator.BETWEEN, startOfEmail, endOfEmail);
    }
    public CustomerRequest<T> withEmailStartingWith(String email){
       return withEmail(Operator.BEGIN_WITH, email);
    }
    public CustomerRequest<T> withEmailContaining(String email){
       return withEmail(Operator.CONTAIN, email);
    }

    public CustomerRequest<T> withEmailEndingWith(String email){
       return withEmail(Operator.END_WITH, email);
    }

    public CustomerRequest<T> withEmailIs(String email){
       return withEmail(Operator.EQUAL, email);
    }

    public CustomerRequest<T> withEmailSoundingLike(String email){
       return withEmail(Operator.SOUNDS_LIKE, email);
    }



    public CustomerRequest<T> filterByCommercePlatform(CommercePlatform... commercePlatform){
      if (commercePlatform == null || commercePlatform.length == 0) {
        throw new IllegalArgumentException("filterByCommercePlatform parameter commercePlatform cannot be empty");
      }
      return appendSearchCriteria(createCommercePlatformCriteria(Operator.EQUAL, (Object[])commercePlatform));
    }

    public CustomerRequest<T> withCommercePlatform(Operator operator, Object... values){
       return appendSearchCriteria(createCommercePlatformCriteria(operator, values));
    }

    public CustomerRequest<T> withCommercePlatformIsUnknown(){
       return withCommercePlatform(Operator.IS_NULL);
    }

    public CustomerRequest<T> withCommercePlatformIsKnown(){
       return withCommercePlatform(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCommercePlatformCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Customer.COMMERCE_PLATFORM_PROPERTY, operator, values);
    }

    public CustomerRequest<T> filterByCommercePlatform(Long commercePlatform){
      if(commercePlatform == null){
         return this;
      }
      return withCommercePlatform(Operator.EQUAL, commercePlatform);
    }
    public CustomerRequest<T> withCommercePlatformMatching(CommercePlatformRequest commercePlatform){
       return appendSearchCriteria(new SubQuerySearchCriteria(Customer.COMMERCE_PLATFORM_PROPERTY, commercePlatform, CommercePlatform.ID_PROPERTY));
    }

    public CustomerRequest<T> filterByCreateTime(LocalDateTime... createTime){
      if (createTime == null || createTime.length == 0) {
        throw new IllegalArgumentException("filterByCreateTime parameter createTime cannot be empty");
      }
      return appendSearchCriteria(createCreateTimeCriteria(Operator.EQUAL, (Object[])createTime));
    }

    public CustomerRequest<T> withCreateTime(Operator operator, Object... values){
       return appendSearchCriteria(createCreateTimeCriteria(operator, values));
    }

    public CustomerRequest<T> withCreateTimeIsUnknown(){
       return withCreateTime(Operator.IS_NULL);
    }

    public CustomerRequest<T> withCreateTimeIsKnown(){
       return withCreateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCreateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Customer.CREATE_TIME_PROPERTY, operator, values);
    }

    public CustomerRequest<T> withCreateTimeGreaterThan(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public CustomerRequest<T> withCreateTimeGreaterThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN_OR_EQUAL, createTime);
    }

    public CustomerRequest<T> withCreateTimeLessThan(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public CustomerRequest<T> withCreateTimeLessThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN_OR_EQUAL, createTime);
    }

    public CustomerRequest<T> withCreateTimeBetween(LocalDateTime startOfCreateTime, LocalDateTime endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }
    public CustomerRequest<T> withCreateTimeBefore(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public CustomerRequest<T> withCreateTimeBefore(Date createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public CustomerRequest<T> withCreateTimeAfter(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public CustomerRequest<T> withCreateTimeAfter(Date createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public CustomerRequest<T> withCreateTimeBetween(Date startOfCreateTime, Date endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }




    public CustomerRequest<T> filterByUpdateTime(LocalDateTime... updateTime){
      if (updateTime == null || updateTime.length == 0) {
        throw new IllegalArgumentException("filterByUpdateTime parameter updateTime cannot be empty");
      }
      return appendSearchCriteria(createUpdateTimeCriteria(Operator.EQUAL, (Object[])updateTime));
    }

    public CustomerRequest<T> withUpdateTime(Operator operator, Object... values){
       return appendSearchCriteria(createUpdateTimeCriteria(operator, values));
    }

    public CustomerRequest<T> withUpdateTimeIsUnknown(){
       return withUpdateTime(Operator.IS_NULL);
    }

    public CustomerRequest<T> withUpdateTimeIsKnown(){
       return withUpdateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createUpdateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Customer.UPDATE_TIME_PROPERTY, operator, values);
    }

    public CustomerRequest<T> withUpdateTimeGreaterThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public CustomerRequest<T> withUpdateTimeGreaterThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN_OR_EQUAL, updateTime);
    }

    public CustomerRequest<T> withUpdateTimeLessThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public CustomerRequest<T> withUpdateTimeLessThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN_OR_EQUAL, updateTime);
    }

    public CustomerRequest<T> withUpdateTimeBetween(LocalDateTime startOfUpdateTime, LocalDateTime endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
    }
    public CustomerRequest<T> withUpdateTimeBefore(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public CustomerRequest<T> withUpdateTimeBefore(Date updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public CustomerRequest<T> withUpdateTimeAfter(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public CustomerRequest<T> withUpdateTimeAfter(Date updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public CustomerRequest<T> withUpdateTimeBetween(Date startOfUpdateTime, Date endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
    }




    public CustomerRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public CustomerRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public CustomerRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public CustomerRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(Customer.VERSION_PROPERTY, operator, values);
    }

    public CustomerRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public CustomerRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public CustomerRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public CustomerRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public CustomerRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }

    public CustomerRequest<T> withCustomerOrderListMatching(CustomerOrderRequest customerOrderRequest){
        return appendSearchCriteria(new SubQuerySearchCriteria(Customer.ID_PROPERTY, customerOrderRequest, CustomerOrder.CUSTOMER_PROPERTY));
    }

    public CustomerRequest<T> withoutCustomerOrderListMatching(CustomerOrderRequest customerOrderRequest){
        return appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria(Customer.ID_PROPERTY, customerOrderRequest, CustomerOrder.CUSTOMER_PROPERTY)));
    }

    public CustomerRequest<T> haveCustomerOrders(){
        return withCustomerOrderListMatching(Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> haveNoCustomerOrders(){
        return withoutCustomerOrderListMatching(Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> count(){
        super.count();
        return this;
    }
    public CustomerRequest<T> countAs(String retName){
        super.count(retName);
        return this;
    }
    public CustomerRequest<T> groupByCommercePlatformWithDetails(){
       return groupByCommercePlatformWithDetails(Q.commercePlatforms().unlimited());
    }

    public CustomerRequest<T> groupByCommercePlatformWithDetails(CommercePlatformRequest subRequest){
       aggregate(Customer.COMMERCE_PLATFORM_PROPERTY, subRequest);
       return this;
    }




    public CustomerRequest<T> groupByCustomerOrdersWithDetails(CustomerOrderRequest subRequest){
       aggregate(Customer.CUSTOMER_ORDER_LIST_PROPERTY, subRequest);
       return this;
    }

    public CustomerRequest<T> groupById(){
       groupBy(Customer.ID_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByIdAs(String retName){
       groupBy(retName, Customer.ID_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, Customer.ID_PROPERTY, function);
       return this;
    }

    public CustomerRequest<T> groupByName(){
       groupBy(Customer.NAME_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByNameAs(String retName){
       groupBy(retName, Customer.NAME_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, Customer.NAME_PROPERTY, function);
       return this;
    }

    public CustomerRequest<T> groupByEmail(){
       groupBy(Customer.EMAIL_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByEmailAs(String retName){
       groupBy(retName, Customer.EMAIL_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByEmailWithFunction(String retName, AggrFunction function){
       groupBy(retName, Customer.EMAIL_PROPERTY, function);
       return this;
    }
    public CustomerRequest<T> groupByCommercePlatformWith(CommercePlatformRequest subRequest){
       groupBy(Customer.COMMERCE_PLATFORM_PROPERTY, subRequest);
       return this;
    }
    public CustomerRequest<T> groupByCommercePlatform(){
       groupBy(Customer.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByCommercePlatformAs(String retName){
       groupBy(retName, Customer.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByCommercePlatformWithFunction(String retName, AggrFunction function){
       groupBy(retName, Customer.COMMERCE_PLATFORM_PROPERTY, function);
       return this;
    }

    public CustomerRequest<T> groupByCreateTime(){
       groupBy(Customer.CREATE_TIME_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByCreateTimeAs(String retName){
       groupBy(retName, Customer.CREATE_TIME_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByCreateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, Customer.CREATE_TIME_PROPERTY, function);
       return this;
    }

    public CustomerRequest<T> groupByUpdateTime(){
       groupBy(Customer.UPDATE_TIME_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByUpdateTimeAs(String retName){
       groupBy(retName, Customer.UPDATE_TIME_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByUpdateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, Customer.UPDATE_TIME_PROPERTY, function);
       return this;
    }

    public CustomerRequest<T> groupByVersion(){
       groupBy(Customer.VERSION_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByVersionAs(String retName){
       groupBy(retName, Customer.VERSION_PROPERTY);
       return this;
    }

    public CustomerRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, Customer.VERSION_PROPERTY, function);
       return this;
    }



    public CustomerRequest<T> orderByIdAscending(){
       addOrderByAscending(Customer.ID_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByIdDescending(){
       addOrderByDescending(Customer.ID_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByNameAscending(){
       addOrderByAscending(Customer.NAME_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByNameDescending(){
       addOrderByDescending(Customer.NAME_PROPERTY);
       return this;
    }
    public CustomerRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Customer.NAME_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Customer.NAME_PROPERTY);
       return this;
    }
    public CustomerRequest<T> orderByEmailAscending(){
       addOrderByAscending(Customer.EMAIL_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByEmailDescending(){
       addOrderByDescending(Customer.EMAIL_PROPERTY);
       return this;
    }
    public CustomerRequest<T> orderByEmailAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(Customer.EMAIL_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByEmailDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(Customer.EMAIL_PROPERTY);
       return this;
    }
    public CustomerRequest<T> orderByCommercePlatformAscending(){
       addOrderByAscending(Customer.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByCommercePlatformDescending(){
       addOrderByDescending(Customer.COMMERCE_PLATFORM_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByCreateTimeAscending(){
       addOrderByAscending(Customer.CREATE_TIME_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByCreateTimeDescending(){
       addOrderByDescending(Customer.CREATE_TIME_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByUpdateTimeAscending(){
       addOrderByAscending(Customer.UPDATE_TIME_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByUpdateTimeDescending(){
       addOrderByDescending(Customer.UPDATE_TIME_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByVersionAscending(){
       addOrderByAscending(Customer.VERSION_PROPERTY);
       return this;
    }

    public CustomerRequest<T> orderByVersionDescending(){
       addOrderByDescending(Customer.VERSION_PROPERTY);
       return this;
    }


    public CustomerRequest<T> statsFromCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
       return statsFromCustomerOrdersAs(name, subRequest, false);
    }

    public CustomerRequest<T> statsFromCustomerOrdersAs(String name, CustomerOrderRequest subRequest, boolean singleResult){
       subRequest.setPartitionProperty(CustomerOrder.CUSTOMER_PROPERTY);
       addAggregateDynamicProperty(name, subRequest, singleResult);
       return this;
    }

    public CustomerRequest<T> statsFromCustomerOrders(CustomerOrderRequest subRequest){
       return statsFromCustomerOrdersAs(REFINEMENTS, subRequest);
    }
    public CommercePlatformRequest rollUpToCommercePlatform(){
       CommercePlatformRequest commercePlatform = Q.commercePlatforms().unlimited();
       this.withCommercePlatformMatching(commercePlatform)
           .groupByCommercePlatformWith(commercePlatform);
       return commercePlatform;
    }




    public CustomerRequest<T> countCustomerOrders(){
        return countCustomerOrdersAs("Count");
    }

    public CustomerRequest<T> countCustomerOrdersAs(String name){
        return countCustomerOrdersWith(name, Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> countCustomerOrdersWith(String name, CustomerOrderRequest subRequest){
        return statsFromCustomerOrdersAs(name, subRequest.count(), true);
    }
    public CustomerRequest<T> minTotalAmountOfCustomerOrders(){
        return minTotalAmountOfCustomerOrdersAs("minTotalAmountOfCustomerOrders");
    }

    public CustomerRequest<T> minTotalAmountOfCustomerOrdersAs(String name){
        return minTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> minTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        return statsFromCustomerOrdersAs(name, subRequest.minTotalAmount(), true);
    }
    public CustomerRequest<T> maxTotalAmountOfCustomerOrders(){
        return maxTotalAmountOfCustomerOrdersAs("maxTotalAmountOfCustomerOrders");
    }

    public CustomerRequest<T> maxTotalAmountOfCustomerOrdersAs(String name){
        return maxTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> maxTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        return statsFromCustomerOrdersAs(name, subRequest.maxTotalAmount(), true);
    }
    public CustomerRequest<T> sumTotalAmountOfCustomerOrders(){
        return sumTotalAmountOfCustomerOrdersAs("sumTotalAmountOfCustomerOrders");
    }

    public CustomerRequest<T> sumTotalAmountOfCustomerOrdersAs(String name){
        return sumTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> sumTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        return statsFromCustomerOrdersAs(name, subRequest.sumTotalAmount(), true);
    }
    public CustomerRequest<T> avgTotalAmountOfCustomerOrders(){
        return avgTotalAmountOfCustomerOrdersAs("avgTotalAmountOfCustomerOrders");
    }

    public CustomerRequest<T> avgTotalAmountOfCustomerOrdersAs(String name){
        return avgTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> avgTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        return statsFromCustomerOrdersAs(name, subRequest.avgTotalAmount(), true);
    }
    public CustomerRequest<T> standardDeviationTotalAmountOfCustomerOrders(){
        return standardDeviationTotalAmountOfCustomerOrdersAs("stdDevTotalAmountOfCustomerOrders");
    }

    public CustomerRequest<T> standardDeviationTotalAmountOfCustomerOrdersAs(String name){
        return standardDeviationTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> standardDeviationTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        return statsFromCustomerOrdersAs(name, subRequest.standardDeviationTotalAmount(), true);
    }
    public CustomerRequest<T> squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrders(){
        return squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrdersAs("stdDevPopTotalAmountOfCustomerOrders");
    }

    public CustomerRequest<T> squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrdersAs(String name){
        return squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> squareRootOfPopulationStandardDeviationTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        return statsFromCustomerOrdersAs(name, subRequest.squareRootOfPopulationStandardDeviationTotalAmount(), true);
    }
    public CustomerRequest<T> sampleVarianceTotalAmountOfCustomerOrders(){
        return sampleVarianceTotalAmountOfCustomerOrdersAs("varSampTotalAmountOfCustomerOrders");
    }

    public CustomerRequest<T> sampleVarianceTotalAmountOfCustomerOrdersAs(String name){
        return sampleVarianceTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> sampleVarianceTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        return statsFromCustomerOrdersAs(name, subRequest.sampleVarianceTotalAmount(), true);
    }
    public CustomerRequest<T> samplePopulationVarianceTotalAmountOfCustomerOrders(){
        return samplePopulationVarianceTotalAmountOfCustomerOrdersAs("varPopTotalAmountOfCustomerOrders");
    }

    public CustomerRequest<T> samplePopulationVarianceTotalAmountOfCustomerOrdersAs(String name){
        return samplePopulationVarianceTotalAmountOfCustomerOrdersAs(name, Q.customerOrders().unlimited());
    }

    public CustomerRequest<T> samplePopulationVarianceTotalAmountOfCustomerOrdersAs(String name, CustomerOrderRequest subRequest){
        return statsFromCustomerOrdersAs(name, subRequest.samplePopulationVarianceTotalAmount(), true);
    }

   public CustomerRequest<T> facetByCommercePlatformAs(String facetName, CommercePlatformRequest commercePlatform){
       return facetByCommercePlatformAs(facetName, commercePlatform, true);
   }

   public CustomerRequest<T> facetByCommercePlatformAs(String facetName, CommercePlatformRequest commercePlatform, boolean includeAllFacets){
       addFacet(facetName, Customer.COMMERCE_PLATFORM_PROPERTY, commercePlatform, includeAllFacets);
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public CustomerRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public CustomerRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public CustomerRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public CustomerRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public CustomerRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}