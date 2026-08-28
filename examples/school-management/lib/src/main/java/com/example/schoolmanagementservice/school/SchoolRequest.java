
package com.example.schoolmanagementservice.school;

import com.example.schoolmanagementservice.Q;
import com.example.schoolmanagementservice.platform.Platform;
import com.example.schoolmanagementservice.platform.PlatformRequest;
import com.example.schoolmanagementservice.schooltype.SchoolType;
import com.example.schoolmanagementservice.schooltype.SchoolTypeRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

public class SchoolRequest<T extends School> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public SchoolRequest(Class<T> returnType){
        super(returnType, () -> (T) new School());
        selectId();
        selectVersion();
    }

    public SchoolRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public SchoolRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public SchoolRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public SchoolRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public SchoolRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public SchoolRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public SchoolRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public SchoolRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (SchoolRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public SchoolRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public SchoolRequest<T> matchingAnyOf(SchoolRequest school){
        super.internalMatchAny(school);
        return this;
    }

    public SchoolRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public SchoolRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public SchoolRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public SchoolRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectPlatformIdOnly().selectSchoolTypeIdOnly().selectName().selectAddress().selectEstablishedDate().selectStudentCapacity().selectActive().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public SchoolRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public SchoolRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectPlatform().selectSchoolType().selectName().selectAddress().selectEstablishedDate().selectStudentCapacity().selectActive().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public SchoolRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectPlatform().selectSchoolType().selectName().selectAddress().selectEstablishedDate().selectStudentCapacity().selectActive().selectCreateTime().selectUpdateTime().selectVersion();
    }


    public SchoolRequest<T> selectId(){
       selectProperty(School.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SchoolRequest<T> unselectId(){
       unselectProperty(School.ID_PROPERTY);
       return this;
    }
    public SchoolRequest<T> selectPlatformIdOnly(){
       selectProperty(School.PLATFORM_PROPERTY);
       return this;
    }

    public SchoolRequest<T> selectPlatform(){
        return selectPlatformWith(Q.platforms().unlimited().selectSelf());
    }

    public SchoolRequest<T> selectPlatformWith(PlatformRequest platform){
       selectProperty(School.PLATFORM_PROPERTY);
       enhanceRelation(School.PLATFORM_PROPERTY, platform);
       return this;
    }

    public SchoolRequest<T> unselectPlatform(){
       unselectProperty(School.PLATFORM_PROPERTY);
       return this;
    }
    public SchoolRequest<T> selectSchoolTypeIdOnly(){
       selectProperty(School.SCHOOL_TYPE_PROPERTY);
       return this;
    }

    public SchoolRequest<T> selectSchoolType(){
        return selectSchoolTypeWith(Q.schoolTypes().unlimited().selectSelf());
    }

    public SchoolRequest<T> selectSchoolTypeWith(SchoolTypeRequest schoolType){
       selectProperty(School.SCHOOL_TYPE_PROPERTY);
       enhanceRelation(School.SCHOOL_TYPE_PROPERTY, schoolType);
       return this;
    }

    public SchoolRequest<T> unselectSchoolType(){
       unselectProperty(School.SCHOOL_TYPE_PROPERTY);
       return this;
    }
    public SchoolRequest<T> selectName(){
       selectProperty(School.NAME_PROPERTY);
       return this;
    }

    /**
     * fill the name with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  name) to fetch name property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SchoolRequest<T> unselectName(){
       unselectProperty(School.NAME_PROPERTY);
       return this;
    }
    public SchoolRequest<T> selectAddress(){
       selectProperty(School.ADDRESS_PROPERTY);
       return this;
    }

    /**
     * fill the address with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  address) to fetch address property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SchoolRequest<T> unselectAddress(){
       unselectProperty(School.ADDRESS_PROPERTY);
       return this;
    }
    public SchoolRequest<T> selectEstablishedDate(){
       selectProperty(School.ESTABLISHED_DATE_PROPERTY);
       return this;
    }

    /**
     * fill the establishedDate with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  establishedDate) to fetch establishedDate property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SchoolRequest<T> unselectEstablishedDate(){
       unselectProperty(School.ESTABLISHED_DATE_PROPERTY);
       return this;
    }
    public SchoolRequest<T> selectStudentCapacity(){
       selectProperty(School.STUDENT_CAPACITY_PROPERTY);
       return this;
    }

    /**
     * fill the studentCapacity with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  studentCapacity) to fetch studentCapacity property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the studentCapacity with customized aggrFunction, TEAQL uses ({aggrFunction}(studentCapacity) AS studentCapacity to fetch studentCapacity property.
     * @param aggrFunction  aggrFunction
     */
    public SchoolRequest<T> selectStudentCapacity(AggrFunction aggrFunction){
       selectProperty(School.STUDENT_CAPACITY_PROPERTY, aggrFunction);
       return this;
    }


    public SchoolRequest<T> unselectStudentCapacity(){
       unselectProperty(School.STUDENT_CAPACITY_PROPERTY);
       return this;
    }
    public SchoolRequest<T> selectActive(){
       selectProperty(School.ACTIVE_PROPERTY);
       return this;
    }

    /**
     * fill the active with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  active) to fetch active property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SchoolRequest<T> unselectActive(){
       unselectProperty(School.ACTIVE_PROPERTY);
       return this;
    }
    public SchoolRequest<T> selectCreateTime(){
       selectProperty(School.CREATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the createTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  createTime) to fetch createTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SchoolRequest<T> unselectCreateTime(){
       unselectProperty(School.CREATE_TIME_PROPERTY);
       return this;
    }
    public SchoolRequest<T> selectUpdateTime(){
       selectProperty(School.UPDATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the updateTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  updateTime) to fetch updateTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SchoolRequest<T> unselectUpdateTime(){
       unselectProperty(School.UPDATE_TIME_PROPERTY);
       return this;
    }
    public SchoolRequest<T> selectVersion(){
       selectProperty(School.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public SchoolRequest<T> unselectVersion(){
       unselectProperty(School.VERSION_PROPERTY);
       return this;
    }

    public SchoolRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(School.ID_PROPERTY, operator, values);
    }

    public SchoolRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public SchoolRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public SchoolRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public SchoolRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public SchoolRequest<T> filterByPlatform(Platform... platform){
      if (platform == null || platform.length == 0) {
        throw new IllegalArgumentException("filterByPlatform parameter platform cannot be empty");
      }
      return appendSearchCriteria(createPlatformCriteria(Operator.EQUAL, (Object[])platform));
    }

    public SchoolRequest<T> withPlatform(Operator operator, Object... values){
       return appendSearchCriteria(createPlatformCriteria(operator, values));
    }

    public SchoolRequest<T> withPlatformIsUnknown(){
       return withPlatform(Operator.IS_NULL);
    }

    public SchoolRequest<T> withPlatformIsKnown(){
       return withPlatform(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createPlatformCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(School.PLATFORM_PROPERTY, operator, values);
    }

    public SchoolRequest<T> filterByPlatform(Long platform){
      if(platform == null){
         return this;
      }
      return withPlatform(Operator.EQUAL, platform);
    }
    public SchoolRequest<T> withPlatformMatching(PlatformRequest platform){
       return appendSearchCriteria(new SubQuerySearchCriteria(School.PLATFORM_PROPERTY, platform, Platform.ID_PROPERTY));
    }

    public SchoolRequest<T> filterBySchoolType(SchoolType... schoolType){
      if (schoolType == null || schoolType.length == 0) {
        throw new IllegalArgumentException("filterBySchoolType parameter schoolType cannot be empty");
      }
      return appendSearchCriteria(createSchoolTypeCriteria(Operator.EQUAL, (Object[])schoolType));
    }

    public SchoolRequest<T> withSchoolType(Operator operator, Object... values){
       return appendSearchCriteria(createSchoolTypeCriteria(operator, values));
    }

    public SchoolRequest<T> withSchoolTypeIsUnknown(){
       return withSchoolType(Operator.IS_NULL);
    }

    public SchoolRequest<T> withSchoolTypeIsKnown(){
       return withSchoolType(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createSchoolTypeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(School.SCHOOL_TYPE_PROPERTY, operator, values);
    }

    public SchoolRequest<T> filterBySchoolType(Long schoolType){
      if(schoolType == null){
         return this;
      }
      return withSchoolType(Operator.EQUAL, schoolType);
    }
    public SchoolRequest<T> withSchoolTypeMatching(SchoolTypeRequest schoolType){
       return appendSearchCriteria(new SubQuerySearchCriteria(School.SCHOOL_TYPE_PROPERTY, schoolType, SchoolType.ID_PROPERTY));
    }

    public SchoolRequest<T> filterByName(String... name){
      if (name == null || name.length == 0) {
        throw new IllegalArgumentException("filterByName parameter name cannot be empty");
      }
      return appendSearchCriteria(createNameCriteria(Operator.EQUAL, (Object[])name));
    }

    public SchoolRequest<T> withName(Operator operator, Object... values){
       return appendSearchCriteria(createNameCriteria(operator, values));
    }

    public SchoolRequest<T> withNameIsUnknown(){
       return withName(Operator.IS_NULL);
    }

    public SchoolRequest<T> withNameIsKnown(){
       return withName(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createNameCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(School.NAME_PROPERTY, operator, values);
    }

    public SchoolRequest<T> withNameIsNot(String name){
       return withName(Operator.NOT_EQUAL, name);
    }

    public SchoolRequest<T> withNameIn(String... name){
       return withName(Operator.IN, (Object[])name);
    }

    public SchoolRequest<T> withNameNotIn(String... name){
       return withName(Operator.NOT_IN, (Object[])name);
    }
    public SchoolRequest<T> withNameGreaterThan(String name){
       return withName(Operator.GREATER_THAN, name);
    }

    public SchoolRequest<T> withNameGreaterThanOrEqualTo(String name){
       return withName(Operator.GREATER_THAN_OR_EQUAL, name);
    }

    public SchoolRequest<T> withNameLessThan(String name){
       return withName(Operator.LESS_THAN, name);
    }

    public SchoolRequest<T> withNameLessThanOrEqualTo(String name){
       return withName(Operator.LESS_THAN_OR_EQUAL, name);
    }

    public SchoolRequest<T> withNameBetween(String startOfName, String endOfName){
       return withName(Operator.BETWEEN, startOfName, endOfName);
    }
    public SchoolRequest<T> withNameStartingWith(String name){
       return withName(Operator.BEGIN_WITH, name);
    }
    public SchoolRequest<T> withNameContaining(String name){
       return withName(Operator.CONTAIN, name);
    }

    public SchoolRequest<T> withNameNotContaining(String name){
       return withName(Operator.NOT_CONTAIN, name);
    }

    public SchoolRequest<T> withNameNotStartingWith(String name){
       return withName(Operator.NOT_BEGIN_WITH, name);
    }

    public SchoolRequest<T> withNameEndingWith(String name){
       return withName(Operator.END_WITH, name);
    }

    public SchoolRequest<T> withNameNotEndingWith(String name){
       return withName(Operator.NOT_END_WITH, name);
    }

    public SchoolRequest<T> withNameIs(String name){
       return withName(Operator.EQUAL, name);
    }

    public SchoolRequest<T> withNameSoundingLike(String name){
       return withName(Operator.SOUNDS_LIKE, name);
    }



    public SchoolRequest<T> filterByAddress(String... address){
      if (address == null || address.length == 0) {
        throw new IllegalArgumentException("filterByAddress parameter address cannot be empty");
      }
      return appendSearchCriteria(createAddressCriteria(Operator.EQUAL, (Object[])address));
    }

    public SchoolRequest<T> withAddress(Operator operator, Object... values){
       return appendSearchCriteria(createAddressCriteria(operator, values));
    }

    public SchoolRequest<T> withAddressIsUnknown(){
       return withAddress(Operator.IS_NULL);
    }

    public SchoolRequest<T> withAddressIsKnown(){
       return withAddress(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createAddressCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(School.ADDRESS_PROPERTY, operator, values);
    }

    public SchoolRequest<T> withAddressIsNot(String address){
       return withAddress(Operator.NOT_EQUAL, address);
    }

    public SchoolRequest<T> withAddressIn(String... address){
       return withAddress(Operator.IN, (Object[])address);
    }

    public SchoolRequest<T> withAddressNotIn(String... address){
       return withAddress(Operator.NOT_IN, (Object[])address);
    }
    public SchoolRequest<T> withAddressGreaterThan(String address){
       return withAddress(Operator.GREATER_THAN, address);
    }

    public SchoolRequest<T> withAddressGreaterThanOrEqualTo(String address){
       return withAddress(Operator.GREATER_THAN_OR_EQUAL, address);
    }

    public SchoolRequest<T> withAddressLessThan(String address){
       return withAddress(Operator.LESS_THAN, address);
    }

    public SchoolRequest<T> withAddressLessThanOrEqualTo(String address){
       return withAddress(Operator.LESS_THAN_OR_EQUAL, address);
    }

    public SchoolRequest<T> withAddressBetween(String startOfAddress, String endOfAddress){
       return withAddress(Operator.BETWEEN, startOfAddress, endOfAddress);
    }
    public SchoolRequest<T> withAddressStartingWith(String address){
       return withAddress(Operator.BEGIN_WITH, address);
    }
    public SchoolRequest<T> withAddressContaining(String address){
       return withAddress(Operator.CONTAIN, address);
    }

    public SchoolRequest<T> withAddressNotContaining(String address){
       return withAddress(Operator.NOT_CONTAIN, address);
    }

    public SchoolRequest<T> withAddressNotStartingWith(String address){
       return withAddress(Operator.NOT_BEGIN_WITH, address);
    }

    public SchoolRequest<T> withAddressEndingWith(String address){
       return withAddress(Operator.END_WITH, address);
    }

    public SchoolRequest<T> withAddressNotEndingWith(String address){
       return withAddress(Operator.NOT_END_WITH, address);
    }

    public SchoolRequest<T> withAddressIs(String address){
       return withAddress(Operator.EQUAL, address);
    }

    public SchoolRequest<T> withAddressSoundingLike(String address){
       return withAddress(Operator.SOUNDS_LIKE, address);
    }



    public SchoolRequest<T> filterByEstablishedDate(LocalDate... establishedDate){
      if (establishedDate == null || establishedDate.length == 0) {
        throw new IllegalArgumentException("filterByEstablishedDate parameter establishedDate cannot be empty");
      }
      return appendSearchCriteria(createEstablishedDateCriteria(Operator.EQUAL, (Object[])establishedDate));
    }

    public SchoolRequest<T> withEstablishedDate(Operator operator, Object... values){
       return appendSearchCriteria(createEstablishedDateCriteria(operator, values));
    }

    public SchoolRequest<T> withEstablishedDateIsUnknown(){
       return withEstablishedDate(Operator.IS_NULL);
    }

    public SchoolRequest<T> withEstablishedDateIsKnown(){
       return withEstablishedDate(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createEstablishedDateCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(School.ESTABLISHED_DATE_PROPERTY, operator, values);
    }

    public SchoolRequest<T> withEstablishedDateIs(LocalDate establishedDate){
       return withEstablishedDate(Operator.EQUAL, establishedDate);
    }

    public SchoolRequest<T> withEstablishedDateIsNot(LocalDate establishedDate){
       return withEstablishedDate(Operator.NOT_EQUAL, establishedDate);
    }

    public SchoolRequest<T> withEstablishedDateIn(LocalDate... establishedDate){
       return withEstablishedDate(Operator.IN, (Object[])establishedDate);
    }

    public SchoolRequest<T> withEstablishedDateNotIn(LocalDate... establishedDate){
       return withEstablishedDate(Operator.NOT_IN, (Object[])establishedDate);
    }
    public SchoolRequest<T> withEstablishedDateGreaterThan(LocalDate establishedDate){
       return withEstablishedDate(Operator.GREATER_THAN, establishedDate);
    }

    public SchoolRequest<T> withEstablishedDateGreaterThanOrEqualTo(LocalDate establishedDate){
       return withEstablishedDate(Operator.GREATER_THAN_OR_EQUAL, establishedDate);
    }

    public SchoolRequest<T> withEstablishedDateLessThan(LocalDate establishedDate){
       return withEstablishedDate(Operator.LESS_THAN, establishedDate);
    }

    public SchoolRequest<T> withEstablishedDateLessThanOrEqualTo(LocalDate establishedDate){
       return withEstablishedDate(Operator.LESS_THAN_OR_EQUAL, establishedDate);
    }

    public SchoolRequest<T> withEstablishedDateBetween(LocalDate startOfEstablishedDate, LocalDate endOfEstablishedDate){
       return withEstablishedDate(Operator.BETWEEN, startOfEstablishedDate, endOfEstablishedDate);
    }
    public SchoolRequest<T> withEstablishedDateBefore(LocalDate establishedDate){
       return withEstablishedDate(Operator.LESS_THAN, establishedDate);
    }

    public SchoolRequest<T> withEstablishedDateBefore(Date establishedDate){
       return withEstablishedDate(Operator.LESS_THAN, establishedDate);
    }

    public SchoolRequest<T> withEstablishedDateAfter(LocalDate establishedDate){
       return withEstablishedDate(Operator.GREATER_THAN, establishedDate);
    }

    public SchoolRequest<T> withEstablishedDateAfter(Date establishedDate){
       return withEstablishedDate(Operator.GREATER_THAN, establishedDate);
    }

    public SchoolRequest<T> withEstablishedDateBetween(Date startOfEstablishedDate, Date endOfEstablishedDate){
       return withEstablishedDate(Operator.BETWEEN, startOfEstablishedDate, endOfEstablishedDate);
    }




    public SchoolRequest<T> filterByStudentCapacity(Integer... studentCapacity){
      if (studentCapacity == null || studentCapacity.length == 0) {
        throw new IllegalArgumentException("filterByStudentCapacity parameter studentCapacity cannot be empty");
      }
      return appendSearchCriteria(createStudentCapacityCriteria(Operator.EQUAL, (Object[])studentCapacity));
    }

    public SchoolRequest<T> withStudentCapacity(Operator operator, Object... values){
       return appendSearchCriteria(createStudentCapacityCriteria(operator, values));
    }

    public SchoolRequest<T> withStudentCapacityIsUnknown(){
       return withStudentCapacity(Operator.IS_NULL);
    }

    public SchoolRequest<T> withStudentCapacityIsKnown(){
       return withStudentCapacity(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createStudentCapacityCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(School.STUDENT_CAPACITY_PROPERTY, operator, values);
    }

    public SchoolRequest<T> withStudentCapacityIs(Integer studentCapacity){
       return withStudentCapacity(Operator.EQUAL, studentCapacity);
    }

    public SchoolRequest<T> withStudentCapacityIsNot(Integer studentCapacity){
       return withStudentCapacity(Operator.NOT_EQUAL, studentCapacity);
    }

    public SchoolRequest<T> withStudentCapacityIn(Integer... studentCapacity){
       return withStudentCapacity(Operator.IN, (Object[])studentCapacity);
    }

    public SchoolRequest<T> withStudentCapacityNotIn(Integer... studentCapacity){
       return withStudentCapacity(Operator.NOT_IN, (Object[])studentCapacity);
    }
    public SchoolRequest<T> withStudentCapacityGreaterThan(Integer studentCapacity){
       return withStudentCapacity(Operator.GREATER_THAN, studentCapacity);
    }

    public SchoolRequest<T> withStudentCapacityGreaterThanOrEqualTo(Integer studentCapacity){
       return withStudentCapacity(Operator.GREATER_THAN_OR_EQUAL, studentCapacity);
    }

    public SchoolRequest<T> withStudentCapacityLessThan(Integer studentCapacity){
       return withStudentCapacity(Operator.LESS_THAN, studentCapacity);
    }

    public SchoolRequest<T> withStudentCapacityLessThanOrEqualTo(Integer studentCapacity){
       return withStudentCapacity(Operator.LESS_THAN_OR_EQUAL, studentCapacity);
    }

    public SchoolRequest<T> withStudentCapacityBetween(Integer startOfStudentCapacity, Integer endOfStudentCapacity){
       return withStudentCapacity(Operator.BETWEEN, startOfStudentCapacity, endOfStudentCapacity);
    }



    public SchoolRequest<T> filterByActive(Boolean... active){
      if (active == null || active.length == 0) {
        throw new IllegalArgumentException("filterByActive parameter active cannot be empty");
      }
      return appendSearchCriteria(createActiveCriteria(Operator.EQUAL, (Object[])active));
    }

    public SchoolRequest<T> withActive(Operator operator, Object... values){
       return appendSearchCriteria(createActiveCriteria(operator, values));
    }

    public SchoolRequest<T> withActiveIsUnknown(){
       return withActive(Operator.IS_NULL);
    }

    public SchoolRequest<T> withActiveIsKnown(){
       return withActive(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createActiveCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(School.ACTIVE_PROPERTY, operator, values);
    }

    public SchoolRequest<T> whichAreActive(){
       return withActive(Operator.EQUAL, true);
    }

    public SchoolRequest<T> whichAreNotActive(){
       return withActive(Operator.EQUAL, false);
    }


    public SchoolRequest<T> filterByCreateTime(LocalDateTime... createTime){
      if (createTime == null || createTime.length == 0) {
        throw new IllegalArgumentException("filterByCreateTime parameter createTime cannot be empty");
      }
      return appendSearchCriteria(createCreateTimeCriteria(Operator.EQUAL, (Object[])createTime));
    }

    public SchoolRequest<T> withCreateTime(Operator operator, Object... values){
       return appendSearchCriteria(createCreateTimeCriteria(operator, values));
    }

    public SchoolRequest<T> withCreateTimeIsUnknown(){
       return withCreateTime(Operator.IS_NULL);
    }

    public SchoolRequest<T> withCreateTimeIsKnown(){
       return withCreateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCreateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(School.CREATE_TIME_PROPERTY, operator, values);
    }

    public SchoolRequest<T> withCreateTimeIs(LocalDateTime createTime){
       return withCreateTime(Operator.EQUAL, createTime);
    }

    public SchoolRequest<T> withCreateTimeIsNot(LocalDateTime createTime){
       return withCreateTime(Operator.NOT_EQUAL, createTime);
    }

    public SchoolRequest<T> withCreateTimeIn(LocalDateTime... createTime){
       return withCreateTime(Operator.IN, (Object[])createTime);
    }

    public SchoolRequest<T> withCreateTimeNotIn(LocalDateTime... createTime){
       return withCreateTime(Operator.NOT_IN, (Object[])createTime);
    }
    public SchoolRequest<T> withCreateTimeGreaterThan(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public SchoolRequest<T> withCreateTimeGreaterThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN_OR_EQUAL, createTime);
    }

    public SchoolRequest<T> withCreateTimeLessThan(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public SchoolRequest<T> withCreateTimeLessThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN_OR_EQUAL, createTime);
    }

    public SchoolRequest<T> withCreateTimeBetween(LocalDateTime startOfCreateTime, LocalDateTime endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }
    public SchoolRequest<T> withCreateTimeBefore(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public SchoolRequest<T> withCreateTimeBefore(Date createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public SchoolRequest<T> withCreateTimeAfter(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public SchoolRequest<T> withCreateTimeAfter(Date createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public SchoolRequest<T> withCreateTimeBetween(Date startOfCreateTime, Date endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }




    public SchoolRequest<T> filterByUpdateTime(LocalDateTime... updateTime){
      if (updateTime == null || updateTime.length == 0) {
        throw new IllegalArgumentException("filterByUpdateTime parameter updateTime cannot be empty");
      }
      return appendSearchCriteria(createUpdateTimeCriteria(Operator.EQUAL, (Object[])updateTime));
    }

    public SchoolRequest<T> withUpdateTime(Operator operator, Object... values){
       return appendSearchCriteria(createUpdateTimeCriteria(operator, values));
    }

    public SchoolRequest<T> withUpdateTimeIsUnknown(){
       return withUpdateTime(Operator.IS_NULL);
    }

    public SchoolRequest<T> withUpdateTimeIsKnown(){
       return withUpdateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createUpdateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(School.UPDATE_TIME_PROPERTY, operator, values);
    }

    public SchoolRequest<T> withUpdateTimeIs(LocalDateTime updateTime){
       return withUpdateTime(Operator.EQUAL, updateTime);
    }

    public SchoolRequest<T> withUpdateTimeIsNot(LocalDateTime updateTime){
       return withUpdateTime(Operator.NOT_EQUAL, updateTime);
    }

    public SchoolRequest<T> withUpdateTimeIn(LocalDateTime... updateTime){
       return withUpdateTime(Operator.IN, (Object[])updateTime);
    }

    public SchoolRequest<T> withUpdateTimeNotIn(LocalDateTime... updateTime){
       return withUpdateTime(Operator.NOT_IN, (Object[])updateTime);
    }
    public SchoolRequest<T> withUpdateTimeGreaterThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public SchoolRequest<T> withUpdateTimeGreaterThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN_OR_EQUAL, updateTime);
    }

    public SchoolRequest<T> withUpdateTimeLessThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public SchoolRequest<T> withUpdateTimeLessThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN_OR_EQUAL, updateTime);
    }

    public SchoolRequest<T> withUpdateTimeBetween(LocalDateTime startOfUpdateTime, LocalDateTime endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
    }
    public SchoolRequest<T> withUpdateTimeBefore(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public SchoolRequest<T> withUpdateTimeBefore(Date updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public SchoolRequest<T> withUpdateTimeAfter(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public SchoolRequest<T> withUpdateTimeAfter(Date updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public SchoolRequest<T> withUpdateTimeBetween(Date startOfUpdateTime, Date endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
    }




    public SchoolRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public SchoolRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public SchoolRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public SchoolRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(School.VERSION_PROPERTY, operator, values);
    }

    public SchoolRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public SchoolRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public SchoolRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public SchoolRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public SchoolRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public SchoolRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public SchoolRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public SchoolRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public SchoolRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }


    public SchoolRequest<T> count(){
        super.count();
        return this;
    }
    public SchoolRequest<T> countAs(String retName){
        super.count(retName);
        return this;
    }
    public SchoolRequest minStudentCapacity(){
        return minStudentCapacityAs(prefix("minOf",School.STUDENT_CAPACITY_PROPERTY));
    }

    public SchoolRequest minStudentCapacityAs(String retName){
        super.min(retName, School.STUDENT_CAPACITY_PROPERTY);
        return this;
    }
    public SchoolRequest maxStudentCapacity(){
        return maxStudentCapacityAs(prefix("maxOf",School.STUDENT_CAPACITY_PROPERTY));
    }

    public SchoolRequest maxStudentCapacityAs(String retName){
        super.max(retName, School.STUDENT_CAPACITY_PROPERTY);
        return this;
    }
    public SchoolRequest sumStudentCapacity(){
        return sumStudentCapacityAs(prefix("sumOf",School.STUDENT_CAPACITY_PROPERTY));
    }

    public SchoolRequest sumStudentCapacityAs(String retName){
        super.sum(retName, School.STUDENT_CAPACITY_PROPERTY);
        return this;
    }
    public SchoolRequest avgStudentCapacity(){
        return avgStudentCapacityAs(prefix("avgOf",School.STUDENT_CAPACITY_PROPERTY));
    }

    public SchoolRequest avgStudentCapacityAs(String retName){
        super.avg(retName, School.STUDENT_CAPACITY_PROPERTY);
        return this;
    }
    public SchoolRequest standardDeviationStudentCapacity(){
        return standardDeviationStudentCapacityAs(prefix("standardDeviationOf",School.STUDENT_CAPACITY_PROPERTY));
    }

    public SchoolRequest standardDeviationStudentCapacityAs(String retName){
        super.standardDeviation(retName, School.STUDENT_CAPACITY_PROPERTY);
        return this;
    }
    public SchoolRequest squareRootOfPopulationStandardDeviationStudentCapacity(){
        return squareRootOfPopulationStandardDeviationStudentCapacityAs(prefix("squareRootOfPopulationStandardDeviationOf",School.STUDENT_CAPACITY_PROPERTY));
    }

    public SchoolRequest squareRootOfPopulationStandardDeviationStudentCapacityAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, School.STUDENT_CAPACITY_PROPERTY);
        return this;
    }
    public SchoolRequest sampleVarianceStudentCapacity(){
        return sampleVarianceStudentCapacityAs(prefix("sampleVarianceOf",School.STUDENT_CAPACITY_PROPERTY));
    }

    public SchoolRequest sampleVarianceStudentCapacityAs(String retName){
        super.sampleVariance(retName, School.STUDENT_CAPACITY_PROPERTY);
        return this;
    }
    public SchoolRequest samplePopulationVarianceStudentCapacity(){
        return samplePopulationVarianceStudentCapacityAs(prefix("samplePopulationVarianceOf",School.STUDENT_CAPACITY_PROPERTY));
    }

    public SchoolRequest samplePopulationVarianceStudentCapacityAs(String retName){
        super.samplePopulationVariance(retName, School.STUDENT_CAPACITY_PROPERTY);
        return this;
    }
    public SchoolRequest<T> groupByPlatformWithDetails(){
       return groupByPlatformWithDetails(Q.platforms().unlimited());
    }

    public SchoolRequest<T> groupByPlatformWithDetails(PlatformRequest subRequest){
       aggregate(School.PLATFORM_PROPERTY, subRequest);
       return this;
    }

    public SchoolRequest<T> groupBySchoolTypeWithDetails(){
       return groupBySchoolTypeWithDetails(Q.schoolTypes().unlimited());
    }

    public SchoolRequest<T> groupBySchoolTypeWithDetails(SchoolTypeRequest subRequest){
       aggregate(School.SCHOOL_TYPE_PROPERTY, subRequest);
       return this;
    }










    public SchoolRequest<T> groupById(){
       groupBy(School.ID_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByIdAs(String retName){
       groupBy(retName, School.ID_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, School.ID_PROPERTY, function);
       return this;
    }
    public SchoolRequest<T> groupByPlatformWith(PlatformRequest subRequest){
       groupBy(School.PLATFORM_PROPERTY, subRequest);
       return this;
    }
    public SchoolRequest<T> groupByPlatform(){
       groupBy(School.PLATFORM_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByPlatformAs(String retName){
       groupBy(retName, School.PLATFORM_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByPlatformWithFunction(String retName, AggrFunction function){
       groupBy(retName, School.PLATFORM_PROPERTY, function);
       return this;
    }
    public SchoolRequest<T> groupBySchoolTypeWith(SchoolTypeRequest subRequest){
       groupBy(School.SCHOOL_TYPE_PROPERTY, subRequest);
       return this;
    }
    public SchoolRequest<T> groupBySchoolType(){
       groupBy(School.SCHOOL_TYPE_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupBySchoolTypeAs(String retName){
       groupBy(retName, School.SCHOOL_TYPE_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupBySchoolTypeWithFunction(String retName, AggrFunction function){
       groupBy(retName, School.SCHOOL_TYPE_PROPERTY, function);
       return this;
    }

    public SchoolRequest<T> groupByName(){
       groupBy(School.NAME_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByNameAs(String retName){
       groupBy(retName, School.NAME_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByNameWithFunction(String retName, AggrFunction function){
       groupBy(retName, School.NAME_PROPERTY, function);
       return this;
    }

    public SchoolRequest<T> groupByAddress(){
       groupBy(School.ADDRESS_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByAddressAs(String retName){
       groupBy(retName, School.ADDRESS_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByAddressWithFunction(String retName, AggrFunction function){
       groupBy(retName, School.ADDRESS_PROPERTY, function);
       return this;
    }

    public SchoolRequest<T> groupByEstablishedDate(){
       groupBy(School.ESTABLISHED_DATE_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByEstablishedDateAs(String retName){
       groupBy(retName, School.ESTABLISHED_DATE_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByEstablishedDateWithFunction(String retName, AggrFunction function){
       groupBy(retName, School.ESTABLISHED_DATE_PROPERTY, function);
       return this;
    }

    public SchoolRequest<T> groupByStudentCapacity(){
       groupBy(School.STUDENT_CAPACITY_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByStudentCapacityAs(String retName){
       groupBy(retName, School.STUDENT_CAPACITY_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByStudentCapacityWithFunction(String retName, AggrFunction function){
       groupBy(retName, School.STUDENT_CAPACITY_PROPERTY, function);
       return this;
    }

    public SchoolRequest<T> groupByActive(){
       groupBy(School.ACTIVE_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByActiveAs(String retName){
       groupBy(retName, School.ACTIVE_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByActiveWithFunction(String retName, AggrFunction function){
       groupBy(retName, School.ACTIVE_PROPERTY, function);
       return this;
    }

    public SchoolRequest<T> groupByCreateTime(){
       groupBy(School.CREATE_TIME_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByCreateTimeAs(String retName){
       groupBy(retName, School.CREATE_TIME_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByCreateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, School.CREATE_TIME_PROPERTY, function);
       return this;
    }

    public SchoolRequest<T> groupByUpdateTime(){
       groupBy(School.UPDATE_TIME_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByUpdateTimeAs(String retName){
       groupBy(retName, School.UPDATE_TIME_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByUpdateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, School.UPDATE_TIME_PROPERTY, function);
       return this;
    }

    public SchoolRequest<T> groupByVersion(){
       groupBy(School.VERSION_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByVersionAs(String retName){
       groupBy(retName, School.VERSION_PROPERTY);
       return this;
    }

    public SchoolRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, School.VERSION_PROPERTY, function);
       return this;
    }

    public SchoolRequest<T> withSchoolTypeIsPrimary(){
       filterBySchoolType(com.example.schoolmanagementservice.Constants.SCHOOL_TYPE_PRIMARY);
       return this;
    }


    public SchoolRequest<T> withSchoolTypeIsSecondary(){
       filterBySchoolType(com.example.schoolmanagementservice.Constants.SCHOOL_TYPE_SECONDARY);
       return this;
    }




    public SchoolRequest<T> orderByIdAscending(){
       addOrderByAscending(School.ID_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByIdDescending(){
       addOrderByDescending(School.ID_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByPlatformAscending(){
       addOrderByAscending(School.PLATFORM_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByPlatformDescending(){
       addOrderByDescending(School.PLATFORM_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderBySchoolTypeAscending(){
       addOrderByAscending(School.SCHOOL_TYPE_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderBySchoolTypeDescending(){
       addOrderByDescending(School.SCHOOL_TYPE_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByNameAscending(){
       addOrderByAscending(School.NAME_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByNameDescending(){
       addOrderByDescending(School.NAME_PROPERTY);
       return this;
    }
    public SchoolRequest<T> orderByNameAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(School.NAME_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByNameDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(School.NAME_PROPERTY);
       return this;
    }
    public SchoolRequest<T> orderByAddressAscending(){
       addOrderByAscending(School.ADDRESS_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByAddressDescending(){
       addOrderByDescending(School.ADDRESS_PROPERTY);
       return this;
    }
    public SchoolRequest<T> orderByAddressAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(School.ADDRESS_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByAddressDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(School.ADDRESS_PROPERTY);
       return this;
    }
    public SchoolRequest<T> orderByEstablishedDateAscending(){
       addOrderByAscending(School.ESTABLISHED_DATE_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByEstablishedDateDescending(){
       addOrderByDescending(School.ESTABLISHED_DATE_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByStudentCapacityAscending(){
       addOrderByAscending(School.STUDENT_CAPACITY_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByStudentCapacityDescending(){
       addOrderByDescending(School.STUDENT_CAPACITY_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByActiveAscending(){
       addOrderByAscending(School.ACTIVE_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByActiveDescending(){
       addOrderByDescending(School.ACTIVE_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByCreateTimeAscending(){
       addOrderByAscending(School.CREATE_TIME_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByCreateTimeDescending(){
       addOrderByDescending(School.CREATE_TIME_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByUpdateTimeAscending(){
       addOrderByAscending(School.UPDATE_TIME_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByUpdateTimeDescending(){
       addOrderByDescending(School.UPDATE_TIME_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByVersionAscending(){
       addOrderByAscending(School.VERSION_PROPERTY);
       return this;
    }

    public SchoolRequest<T> orderByVersionDescending(){
       addOrderByDescending(School.VERSION_PROPERTY);
       return this;
    }


    public PlatformRequest rollUpToPlatform(){
       PlatformRequest platform = Q.platforms().unlimited();
       this.withPlatformMatching(platform)
           .groupByPlatformWith(platform);
       return platform;
    }

    public SchoolTypeRequest rollUpToSchoolType(){
       SchoolTypeRequest schoolType = Q.schoolTypes().unlimited();
       this.withSchoolTypeMatching(schoolType)
           .groupBySchoolTypeWith(schoolType);
       return schoolType;
    }










   public SchoolRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform){
       return facetByPlatformAs(facetName, platform, true);
   }

   public SchoolRequest<T> facetByPlatformAs(String facetName, PlatformRequest platform, boolean includeAllFacets){
       addFacet(facetName, School.PLATFORM_PROPERTY, platform, includeAllFacets);
       return this;
   }
   public SchoolRequest<T> facetBySchoolTypeAs(String facetName, SchoolTypeRequest schoolType){
       return facetBySchoolTypeAs(facetName, schoolType, true);
   }

   public SchoolRequest<T> facetBySchoolTypeAs(String facetName, SchoolTypeRequest schoolType, boolean includeAllFacets){
       addFacet(facetName, School.SCHOOL_TYPE_PROPERTY, schoolType, includeAllFacets);
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public SchoolRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public SchoolRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public SchoolRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public SchoolRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public SchoolRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public SchoolRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}