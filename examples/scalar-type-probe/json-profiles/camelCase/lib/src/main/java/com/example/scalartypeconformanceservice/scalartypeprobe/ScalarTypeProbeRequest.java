
package com.example.scalartypeconformanceservice.scalartypeprobe;

import com.example.scalartypeconformanceservice.Q;
import com.example.scalartypeconformanceservice.typeplatform.TypePlatform;
import com.example.scalartypeconformanceservice.typeplatform.TypePlatformRequest;
import io.teaql.core.AggrFunction;
import io.teaql.core.BaseRequest;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

public class ScalarTypeProbeRequest<T extends ScalarTypeProbe> extends BaseRequest<T> {

    /**
     * @deprecated AI agents and business code must use the generated Q facade
     *             instead of constructing request builders directly.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public ScalarTypeProbeRequest(Class<T> returnType){
        this(returnType, () -> (T) new ScalarTypeProbe());
    }

    protected ScalarTypeProbeRequest(Class<T> returnType, java.util.function.Supplier<? extends T> factory){
        super(returnType, factory);
        selectId();
        selectVersion();
    }

    public ScalarTypeProbeRequest<T> comment(String comment){
         super.internalComment(comment);
         return this;
    }

    // purpose() 继承自 BaseRequest，返回 ExecutableRequest（终结方法）

    public ScalarTypeProbeRequest<T> returnType(Class<? extends T> returnType){
        super.setReturnType(returnType);
        return this;
    }

    public ScalarTypeProbeRequest<T> enableAggregationCache(long cacheExpiredMillis){
        super.enableAggregationCache();
        super.aggregateCacheTime(cacheExpiredMillis);
        return this;
    }

    public ScalarTypeProbeRequest<T> enableAggregationCache(){
        return enableAggregationCache(0l);
    }


    public ScalarTypeProbeRequest<T> propagateAggregationCache(long cacheExpiredMillis){
        super.propagateAggregationCache(cacheExpiredMillis);
        return this;
    }

    /**
     * Accept best-effort stateful seek optimization for browsing consecutive pages.
     * Do not use this for business processing that must visit every row exactly once.
     */
    public ScalarTypeProbeRequest<T> optimizeForContinuousPageFetch(){
        super.optimizeForContinuousPageFetch();
        return this;
    }

    public ScalarTypeProbeRequest<T> optimizeForContinuousPageFetch(String namespace, int ttlSeconds){
        super.optimizeForContinuousPageFetch(namespace, ttlSeconds);
        return this;
    }

    public ScalarTypeProbeRequest<T> optimizePaginationWithIdSet(){
        super.optimizePaginationWithIdSet();
        return this;
    }

    public ScalarTypeProbeRequest<T> optimizePaginationWithIdSet(
            String namespace, int ttlSeconds, int maxIds){
        super.optimizePaginationWithIdSet(namespace, ttlSeconds, maxIds);
        return this;
    }

    public ScalarTypeProbeRequest<T> topNProbeParentThreshold(int threshold){
        super.topNProbeParentThreshold(threshold);
        return this;
    }

    public ScalarTypeProbeRequest<T> appendSearchCriteria(SearchCriteria searchCriteria){
        return (ScalarTypeProbeRequest<T>)super.appendSearchCriteria(searchCriteria);
    }

    public ScalarTypeProbeRequest<T> filter(String property1, Operator operator, String property2){
        return appendSearchCriteria(new TwoOperatorCriteria(operator, new PropertyReference(property1), new PropertyReference(property2)));
    }


    public ScalarTypeProbeRequest<T> matchingAnyOf(ScalarTypeProbeRequest scalarTypeProbe){
        super.internalMatchAny(scalarTypeProbe);
        return this;
    }

    public ScalarTypeProbeRequest<T> enhanceChildrenIfNeeded(){
        return this;
    }

    public ScalarTypeProbeRequest<T> withDeletedRows(){
        super.withDeletedRows();
        return this;
    }

    public ScalarTypeProbeRequest<T> deletedRowsOnly(){
        super.deletedRowsOnly();
        return this;
    }

    public ScalarTypeProbeRequest<T> selectSelf(){
        super.selectSelf();
        return selectId().selectTypePlatformIdOnly().selectRequiredString().selectOptionalString().selectLargeText().selectRequiredBoolean().selectOptionalBoolean().selectRequiredInteger().selectOptionalInteger().selectRequiredLong().selectOptionalLong().selectRequiredDecimal().selectOptionalDecimal().selectRequiredFloat().selectOptionalFloat().selectRequiredDouble().selectOptionalDouble().selectRequiredDate().selectOptionalDate().selectRequiredTime().selectOptionalTime().selectRequiredDatetime().selectOptionalDatetime().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public ScalarTypeProbeRequest<T> selectSelfFields(){
        return selectSelf();
    }

    public ScalarTypeProbeRequest<T> selectSelfWithoutParent(){
        selectId();
        selectVersion();
        selectProperty(ScalarTypeProbe.ID_PROPERTY);
        selectProperty(ScalarTypeProbe.TYPE_PLATFORM_PROPERTY);
        selectProperty(ScalarTypeProbe.REQUIRED_STRING_PROPERTY);
        selectProperty(ScalarTypeProbe.OPTIONAL_STRING_PROPERTY);
        selectProperty(ScalarTypeProbe.LARGE_TEXT_PROPERTY);
        selectProperty(ScalarTypeProbe.REQUIRED_BOOLEAN_PROPERTY);
        selectProperty(ScalarTypeProbe.OPTIONAL_BOOLEAN_PROPERTY);
        selectProperty(ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY);
        selectProperty(ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY);
        selectProperty(ScalarTypeProbe.REQUIRED_LONG_PROPERTY);
        selectProperty(ScalarTypeProbe.OPTIONAL_LONG_PROPERTY);
        selectProperty(ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY);
        selectProperty(ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY);
        selectProperty(ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY);
        selectProperty(ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY);
        selectProperty(ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY);
        selectProperty(ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY);
        selectProperty(ScalarTypeProbe.REQUIRED_DATE_PROPERTY);
        selectProperty(ScalarTypeProbe.OPTIONAL_DATE_PROPERTY);
        selectProperty(ScalarTypeProbe.REQUIRED_TIME_PROPERTY);
        selectProperty(ScalarTypeProbe.OPTIONAL_TIME_PROPERTY);
        selectProperty(ScalarTypeProbe.REQUIRED_DATETIME_PROPERTY);
        selectProperty(ScalarTypeProbe.OPTIONAL_DATETIME_PROPERTY);
        selectProperty(ScalarTypeProbe.CREATE_TIME_PROPERTY);
        selectProperty(ScalarTypeProbe.UPDATE_TIME_PROPERTY);
        selectProperty(ScalarTypeProbe.VERSION_PROPERTY);
        return this;
    }

    public ScalarTypeProbeRequest<T> selectAll(){
        super.selectAll();
        return selectId().selectTypePlatform().selectRequiredString().selectOptionalString().selectLargeText().selectRequiredBoolean().selectOptionalBoolean().selectRequiredInteger().selectOptionalInteger().selectRequiredLong().selectOptionalLong().selectRequiredDecimal().selectOptionalDecimal().selectRequiredFloat().selectOptionalFloat().selectRequiredDouble().selectOptionalDouble().selectRequiredDate().selectOptionalDate().selectRequiredTime().selectOptionalTime().selectRequiredDatetime().selectOptionalDatetime().selectCreateTime().selectUpdateTime().selectVersion();
    }

    public ScalarTypeProbeRequest<T> selectChildren(){
        super.selectAny();
        return selectId().selectTypePlatform().selectRequiredString().selectOptionalString().selectLargeText().selectRequiredBoolean().selectOptionalBoolean().selectRequiredInteger().selectOptionalInteger().selectRequiredLong().selectOptionalLong().selectRequiredDecimal().selectOptionalDecimal().selectRequiredFloat().selectOptionalFloat().selectRequiredDouble().selectOptionalDouble().selectRequiredDate().selectOptionalDate().selectRequiredTime().selectOptionalTime().selectRequiredDatetime().selectOptionalDatetime().selectCreateTime().selectUpdateTime().selectVersion();
    }


    public ScalarTypeProbeRequest<T> selectId(){
       selectProperty(ScalarTypeProbe.ID_PROPERTY);
       return this;
    }

    /**
     * fill the id with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  id) to fetch id property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ScalarTypeProbeRequest<T> unselectId(){
       unselectProperty(ScalarTypeProbe.ID_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectTypePlatformIdOnly(){
       selectProperty(ScalarTypeProbe.TYPE_PLATFORM_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> selectTypePlatform(){
        return selectTypePlatformWith(Q.typePlatforms().unlimited().selectSelf());
    }

    public ScalarTypeProbeRequest<T> selectTypePlatformWith(TypePlatformRequest typePlatform){
       selectProperty(ScalarTypeProbe.TYPE_PLATFORM_PROPERTY);
       enhanceRelation(ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, typePlatform,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, "selectTypePlatformWith", null, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY));
       return this;
    }

    public ScalarTypeProbeRequest<T> unselectTypePlatform(){
       unselectProperty(ScalarTypeProbe.TYPE_PLATFORM_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectRequiredString(){
       selectProperty(ScalarTypeProbe.REQUIRED_STRING_PROPERTY);
       return this;
    }

    /**
     * fill the requiredString with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  requiredString) to fetch requiredString property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ScalarTypeProbeRequest<T> unselectRequiredString(){
       unselectProperty(ScalarTypeProbe.REQUIRED_STRING_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectOptionalString(){
       selectProperty(ScalarTypeProbe.OPTIONAL_STRING_PROPERTY);
       return this;
    }

    /**
     * fill the optionalString with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  optionalString) to fetch optionalString property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ScalarTypeProbeRequest<T> unselectOptionalString(){
       unselectProperty(ScalarTypeProbe.OPTIONAL_STRING_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectLargeText(){
       selectProperty(ScalarTypeProbe.LARGE_TEXT_PROPERTY);
       return this;
    }

    /**
     * fill the largeText with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  largeText) to fetch largeText property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ScalarTypeProbeRequest<T> unselectLargeText(){
       unselectProperty(ScalarTypeProbe.LARGE_TEXT_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectRequiredBoolean(){
       selectProperty(ScalarTypeProbe.REQUIRED_BOOLEAN_PROPERTY);
       return this;
    }

    /**
     * fill the requiredBoolean with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  requiredBoolean) to fetch requiredBoolean property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ScalarTypeProbeRequest<T> unselectRequiredBoolean(){
       unselectProperty(ScalarTypeProbe.REQUIRED_BOOLEAN_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectOptionalBoolean(){
       selectProperty(ScalarTypeProbe.OPTIONAL_BOOLEAN_PROPERTY);
       return this;
    }

    /**
     * fill the optionalBoolean with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  optionalBoolean) to fetch optionalBoolean property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ScalarTypeProbeRequest<T> unselectOptionalBoolean(){
       unselectProperty(ScalarTypeProbe.OPTIONAL_BOOLEAN_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectRequiredInteger(){
       selectProperty(ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY);
       return this;
    }

    /**
     * fill the requiredInteger with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  requiredInteger) to fetch requiredInteger property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the requiredInteger with customized aggrFunction, TEAQL uses ({aggrFunction}(requiredInteger) AS requiredInteger to fetch requiredInteger property.
     * @param aggrFunction  aggrFunction
     */
    public ScalarTypeProbeRequest<T> selectRequiredInteger(AggrFunction aggrFunction){
       selectProperty(ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY, aggrFunction);
       return this;
    }


    public ScalarTypeProbeRequest<T> unselectRequiredInteger(){
       unselectProperty(ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectOptionalInteger(){
       selectProperty(ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY);
       return this;
    }

    /**
     * fill the optionalInteger with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  optionalInteger) to fetch optionalInteger property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the optionalInteger with customized aggrFunction, TEAQL uses ({aggrFunction}(optionalInteger) AS optionalInteger to fetch optionalInteger property.
     * @param aggrFunction  aggrFunction
     */
    public ScalarTypeProbeRequest<T> selectOptionalInteger(AggrFunction aggrFunction){
       selectProperty(ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY, aggrFunction);
       return this;
    }


    public ScalarTypeProbeRequest<T> unselectOptionalInteger(){
       unselectProperty(ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectRequiredLong(){
       selectProperty(ScalarTypeProbe.REQUIRED_LONG_PROPERTY);
       return this;
    }

    /**
     * fill the requiredLong with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  requiredLong) to fetch requiredLong property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the requiredLong with customized aggrFunction, TEAQL uses ({aggrFunction}(requiredLong) AS requiredLong to fetch requiredLong property.
     * @param aggrFunction  aggrFunction
     */
    public ScalarTypeProbeRequest<T> selectRequiredLong(AggrFunction aggrFunction){
       selectProperty(ScalarTypeProbe.REQUIRED_LONG_PROPERTY, aggrFunction);
       return this;
    }


    public ScalarTypeProbeRequest<T> unselectRequiredLong(){
       unselectProperty(ScalarTypeProbe.REQUIRED_LONG_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectOptionalLong(){
       selectProperty(ScalarTypeProbe.OPTIONAL_LONG_PROPERTY);
       return this;
    }

    /**
     * fill the optionalLong with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  optionalLong) to fetch optionalLong property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the optionalLong with customized aggrFunction, TEAQL uses ({aggrFunction}(optionalLong) AS optionalLong to fetch optionalLong property.
     * @param aggrFunction  aggrFunction
     */
    public ScalarTypeProbeRequest<T> selectOptionalLong(AggrFunction aggrFunction){
       selectProperty(ScalarTypeProbe.OPTIONAL_LONG_PROPERTY, aggrFunction);
       return this;
    }


    public ScalarTypeProbeRequest<T> unselectOptionalLong(){
       unselectProperty(ScalarTypeProbe.OPTIONAL_LONG_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectRequiredDecimal(){
       selectProperty(ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY);
       return this;
    }

    /**
     * fill the requiredDecimal with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  requiredDecimal) to fetch requiredDecimal property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the requiredDecimal with customized aggrFunction, TEAQL uses ({aggrFunction}(requiredDecimal) AS requiredDecimal to fetch requiredDecimal property.
     * @param aggrFunction  aggrFunction
     */
    public ScalarTypeProbeRequest<T> selectRequiredDecimal(AggrFunction aggrFunction){
       selectProperty(ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY, aggrFunction);
       return this;
    }


    public ScalarTypeProbeRequest<T> unselectRequiredDecimal(){
       unselectProperty(ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectOptionalDecimal(){
       selectProperty(ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY);
       return this;
    }

    /**
     * fill the optionalDecimal with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  optionalDecimal) to fetch optionalDecimal property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the optionalDecimal with customized aggrFunction, TEAQL uses ({aggrFunction}(optionalDecimal) AS optionalDecimal to fetch optionalDecimal property.
     * @param aggrFunction  aggrFunction
     */
    public ScalarTypeProbeRequest<T> selectOptionalDecimal(AggrFunction aggrFunction){
       selectProperty(ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY, aggrFunction);
       return this;
    }


    public ScalarTypeProbeRequest<T> unselectOptionalDecimal(){
       unselectProperty(ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectRequiredFloat(){
       selectProperty(ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY);
       return this;
    }

    /**
     * fill the requiredFloat with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  requiredFloat) to fetch requiredFloat property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the requiredFloat with customized aggrFunction, TEAQL uses ({aggrFunction}(requiredFloat) AS requiredFloat to fetch requiredFloat property.
     * @param aggrFunction  aggrFunction
     */
    public ScalarTypeProbeRequest<T> selectRequiredFloat(AggrFunction aggrFunction){
       selectProperty(ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY, aggrFunction);
       return this;
    }


    public ScalarTypeProbeRequest<T> unselectRequiredFloat(){
       unselectProperty(ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectOptionalFloat(){
       selectProperty(ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY);
       return this;
    }

    /**
     * fill the optionalFloat with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  optionalFloat) to fetch optionalFloat property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the optionalFloat with customized aggrFunction, TEAQL uses ({aggrFunction}(optionalFloat) AS optionalFloat to fetch optionalFloat property.
     * @param aggrFunction  aggrFunction
     */
    public ScalarTypeProbeRequest<T> selectOptionalFloat(AggrFunction aggrFunction){
       selectProperty(ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY, aggrFunction);
       return this;
    }


    public ScalarTypeProbeRequest<T> unselectOptionalFloat(){
       unselectProperty(ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectRequiredDouble(){
       selectProperty(ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY);
       return this;
    }

    /**
     * fill the requiredDouble with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  requiredDouble) to fetch requiredDouble property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the requiredDouble with customized aggrFunction, TEAQL uses ({aggrFunction}(requiredDouble) AS requiredDouble to fetch requiredDouble property.
     * @param aggrFunction  aggrFunction
     */
    public ScalarTypeProbeRequest<T> selectRequiredDouble(AggrFunction aggrFunction){
       selectProperty(ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY, aggrFunction);
       return this;
    }


    public ScalarTypeProbeRequest<T> unselectRequiredDouble(){
       unselectProperty(ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectOptionalDouble(){
       selectProperty(ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY);
       return this;
    }

    /**
     * fill the optionalDouble with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  optionalDouble) to fetch optionalDouble property.
     * @param rawSqlSegment  customized rawSqlSegment
     */


    /**
     * fill the optionalDouble with customized aggrFunction, TEAQL uses ({aggrFunction}(optionalDouble) AS optionalDouble to fetch optionalDouble property.
     * @param aggrFunction  aggrFunction
     */
    public ScalarTypeProbeRequest<T> selectOptionalDouble(AggrFunction aggrFunction){
       selectProperty(ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY, aggrFunction);
       return this;
    }


    public ScalarTypeProbeRequest<T> unselectOptionalDouble(){
       unselectProperty(ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectRequiredDate(){
       selectProperty(ScalarTypeProbe.REQUIRED_DATE_PROPERTY);
       return this;
    }

    /**
     * fill the requiredDate with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  requiredDate) to fetch requiredDate property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ScalarTypeProbeRequest<T> unselectRequiredDate(){
       unselectProperty(ScalarTypeProbe.REQUIRED_DATE_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectOptionalDate(){
       selectProperty(ScalarTypeProbe.OPTIONAL_DATE_PROPERTY);
       return this;
    }

    /**
     * fill the optionalDate with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  optionalDate) to fetch optionalDate property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ScalarTypeProbeRequest<T> unselectOptionalDate(){
       unselectProperty(ScalarTypeProbe.OPTIONAL_DATE_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectRequiredTime(){
       selectProperty(ScalarTypeProbe.REQUIRED_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the requiredTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  requiredTime) to fetch requiredTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ScalarTypeProbeRequest<T> unselectRequiredTime(){
       unselectProperty(ScalarTypeProbe.REQUIRED_TIME_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectOptionalTime(){
       selectProperty(ScalarTypeProbe.OPTIONAL_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the optionalTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  optionalTime) to fetch optionalTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ScalarTypeProbeRequest<T> unselectOptionalTime(){
       unselectProperty(ScalarTypeProbe.OPTIONAL_TIME_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectRequiredDatetime(){
       selectProperty(ScalarTypeProbe.REQUIRED_DATETIME_PROPERTY);
       return this;
    }

    /**
     * fill the requiredDatetime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  requiredDatetime) to fetch requiredDatetime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ScalarTypeProbeRequest<T> unselectRequiredDatetime(){
       unselectProperty(ScalarTypeProbe.REQUIRED_DATETIME_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectOptionalDatetime(){
       selectProperty(ScalarTypeProbe.OPTIONAL_DATETIME_PROPERTY);
       return this;
    }

    /**
     * fill the optionalDatetime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  optionalDatetime) to fetch optionalDatetime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ScalarTypeProbeRequest<T> unselectOptionalDatetime(){
       unselectProperty(ScalarTypeProbe.OPTIONAL_DATETIME_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectCreateTime(){
       selectProperty(ScalarTypeProbe.CREATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the createTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  createTime) to fetch createTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ScalarTypeProbeRequest<T> unselectCreateTime(){
       unselectProperty(ScalarTypeProbe.CREATE_TIME_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectUpdateTime(){
       selectProperty(ScalarTypeProbe.UPDATE_TIME_PROPERTY);
       return this;
    }

    /**
     * fill the updateTime with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  updateTime) to fetch updateTime property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ScalarTypeProbeRequest<T> unselectUpdateTime(){
       unselectProperty(ScalarTypeProbe.UPDATE_TIME_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> selectVersion(){
       selectProperty(ScalarTypeProbe.VERSION_PROPERTY);
       return this;
    }

    /**
     * fill the version with customized rawSqlSegment, TEAQL uses ({rawSqlSegment} AS  version) to fetch version property.
     * @param rawSqlSegment  customized rawSqlSegment
     */




    public ScalarTypeProbeRequest<T> unselectVersion(){
       unselectProperty(ScalarTypeProbe.VERSION_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> withId(Operator operator, Object... values){
       return appendSearchCriteria(createIdCriteria(operator, values));
    }

    public SearchCriteria createIdCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.ID_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withIdIsNot(Long id){
       return withId(Operator.NOT_EQUAL, id);
    }

    public ScalarTypeProbeRequest<T> withIdIn(Long... id){
       return withId(Operator.IN, (Object[])id);
    }

    public ScalarTypeProbeRequest<T> withIdNotIn(Long... id){
       return withId(Operator.NOT_IN, (Object[])id);
    }
    public ScalarTypeProbeRequest<T> withIdIs(Long id){
       return withId(Operator.EQUAL, id);
    }



    public ScalarTypeProbeRequest<T> filterByTypePlatform(TypePlatform... typePlatform){
      if (typePlatform == null || typePlatform.length == 0) {
        throw new IllegalArgumentException("filterByTypePlatform parameter typePlatform cannot be empty");
      }
      return appendSearchCriteria(createTypePlatformCriteria(Operator.EQUAL, (Object[])typePlatform));
    }

    public ScalarTypeProbeRequest<T> withTypePlatform(Operator operator, Object... values){
       return appendSearchCriteria(createTypePlatformCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withTypePlatformIsUnknown(){
       return withTypePlatform(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withTypePlatformIsKnown(){
       return withTypePlatform(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createTypePlatformCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> filterByTypePlatform(Long typePlatform){
      if(typePlatform == null){
         return this;
      }
      return withTypePlatform(Operator.EQUAL, typePlatform);
    }
    public ScalarTypeProbeRequest<T> withTypePlatformMatching(TypePlatformRequest typePlatform){
       return appendSearchCriteria(new SubQuerySearchCriteria(ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, typePlatform, TypePlatform.ID_PROPERTY));
    }

    public ScalarTypeProbeRequest<T> withoutTypePlatformMatching(TypePlatformRequest typePlatform){
       return appendSearchCriteria(SearchCriteria.not(
           new SubQuerySearchCriteria(ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, typePlatform, TypePlatform.ID_PROPERTY)));
    }

    public ScalarTypeProbeRequest<T> filterByRequiredString(String... requiredString){
      if (requiredString == null || requiredString.length == 0) {
        throw new IllegalArgumentException("filterByRequiredString parameter requiredString cannot be empty");
      }
      return appendSearchCriteria(createRequiredStringCriteria(Operator.EQUAL, (Object[])requiredString));
    }

    public ScalarTypeProbeRequest<T> withRequiredString(Operator operator, Object... values){
       return appendSearchCriteria(createRequiredStringCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withRequiredStringIsUnknown(){
       return withRequiredString(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withRequiredStringIsKnown(){
       return withRequiredString(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createRequiredStringCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.REQUIRED_STRING_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withRequiredStringIsNot(String requiredString){
       return withRequiredString(Operator.NOT_EQUAL, requiredString);
    }

    public ScalarTypeProbeRequest<T> withRequiredStringIn(String... requiredString){
       return withRequiredString(Operator.IN, (Object[])requiredString);
    }

    public ScalarTypeProbeRequest<T> withRequiredStringNotIn(String... requiredString){
       return withRequiredString(Operator.NOT_IN, (Object[])requiredString);
    }
    public ScalarTypeProbeRequest<T> withRequiredStringGreaterThan(String requiredString){
       return withRequiredString(Operator.GREATER_THAN, requiredString);
    }

    public ScalarTypeProbeRequest<T> withRequiredStringGreaterThanOrEqualTo(String requiredString){
       return withRequiredString(Operator.GREATER_THAN_OR_EQUAL, requiredString);
    }

    public ScalarTypeProbeRequest<T> withRequiredStringLessThan(String requiredString){
       return withRequiredString(Operator.LESS_THAN, requiredString);
    }

    public ScalarTypeProbeRequest<T> withRequiredStringLessThanOrEqualTo(String requiredString){
       return withRequiredString(Operator.LESS_THAN_OR_EQUAL, requiredString);
    }

    public ScalarTypeProbeRequest<T> withRequiredStringBetween(String startOfRequiredString, String endOfRequiredString){
       return withRequiredString(Operator.BETWEEN, startOfRequiredString, endOfRequiredString);
    }
    public ScalarTypeProbeRequest<T> withRequiredStringStartingWith(String requiredString){
       return withRequiredString(Operator.BEGIN_WITH, requiredString);
    }
    public ScalarTypeProbeRequest<T> withRequiredStringContaining(String requiredString){
       return withRequiredString(Operator.CONTAIN, requiredString);
    }

    public ScalarTypeProbeRequest<T> withRequiredStringNotContaining(String requiredString){
       return withRequiredString(Operator.NOT_CONTAIN, requiredString);
    }

    public ScalarTypeProbeRequest<T> withRequiredStringNotStartingWith(String requiredString){
       return withRequiredString(Operator.NOT_BEGIN_WITH, requiredString);
    }

    public ScalarTypeProbeRequest<T> withRequiredStringEndingWith(String requiredString){
       return withRequiredString(Operator.END_WITH, requiredString);
    }

    public ScalarTypeProbeRequest<T> withRequiredStringNotEndingWith(String requiredString){
       return withRequiredString(Operator.NOT_END_WITH, requiredString);
    }

    public ScalarTypeProbeRequest<T> withRequiredStringIs(String requiredString){
       return withRequiredString(Operator.EQUAL, requiredString);
    }

    public ScalarTypeProbeRequest<T> withRequiredStringSoundingLike(String requiredString){
       return withRequiredString(Operator.SOUNDS_LIKE, requiredString);
    }



    public ScalarTypeProbeRequest<T> filterByOptionalString(String... optionalString){
      if (optionalString == null || optionalString.length == 0) {
        throw new IllegalArgumentException("filterByOptionalString parameter optionalString cannot be empty");
      }
      return appendSearchCriteria(createOptionalStringCriteria(Operator.EQUAL, (Object[])optionalString));
    }

    public ScalarTypeProbeRequest<T> withOptionalString(Operator operator, Object... values){
       return appendSearchCriteria(createOptionalStringCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withOptionalStringIsUnknown(){
       return withOptionalString(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withOptionalStringIsKnown(){
       return withOptionalString(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createOptionalStringCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.OPTIONAL_STRING_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withOptionalStringIsNot(String optionalString){
       return withOptionalString(Operator.NOT_EQUAL, optionalString);
    }

    public ScalarTypeProbeRequest<T> withOptionalStringIn(String... optionalString){
       return withOptionalString(Operator.IN, (Object[])optionalString);
    }

    public ScalarTypeProbeRequest<T> withOptionalStringNotIn(String... optionalString){
       return withOptionalString(Operator.NOT_IN, (Object[])optionalString);
    }
    public ScalarTypeProbeRequest<T> withOptionalStringGreaterThan(String optionalString){
       return withOptionalString(Operator.GREATER_THAN, optionalString);
    }

    public ScalarTypeProbeRequest<T> withOptionalStringGreaterThanOrEqualTo(String optionalString){
       return withOptionalString(Operator.GREATER_THAN_OR_EQUAL, optionalString);
    }

    public ScalarTypeProbeRequest<T> withOptionalStringLessThan(String optionalString){
       return withOptionalString(Operator.LESS_THAN, optionalString);
    }

    public ScalarTypeProbeRequest<T> withOptionalStringLessThanOrEqualTo(String optionalString){
       return withOptionalString(Operator.LESS_THAN_OR_EQUAL, optionalString);
    }

    public ScalarTypeProbeRequest<T> withOptionalStringBetween(String startOfOptionalString, String endOfOptionalString){
       return withOptionalString(Operator.BETWEEN, startOfOptionalString, endOfOptionalString);
    }
    public ScalarTypeProbeRequest<T> withOptionalStringStartingWith(String optionalString){
       return withOptionalString(Operator.BEGIN_WITH, optionalString);
    }
    public ScalarTypeProbeRequest<T> withOptionalStringContaining(String optionalString){
       return withOptionalString(Operator.CONTAIN, optionalString);
    }

    public ScalarTypeProbeRequest<T> withOptionalStringNotContaining(String optionalString){
       return withOptionalString(Operator.NOT_CONTAIN, optionalString);
    }

    public ScalarTypeProbeRequest<T> withOptionalStringNotStartingWith(String optionalString){
       return withOptionalString(Operator.NOT_BEGIN_WITH, optionalString);
    }

    public ScalarTypeProbeRequest<T> withOptionalStringEndingWith(String optionalString){
       return withOptionalString(Operator.END_WITH, optionalString);
    }

    public ScalarTypeProbeRequest<T> withOptionalStringNotEndingWith(String optionalString){
       return withOptionalString(Operator.NOT_END_WITH, optionalString);
    }

    public ScalarTypeProbeRequest<T> withOptionalStringIs(String optionalString){
       return withOptionalString(Operator.EQUAL, optionalString);
    }

    public ScalarTypeProbeRequest<T> withOptionalStringSoundingLike(String optionalString){
       return withOptionalString(Operator.SOUNDS_LIKE, optionalString);
    }



    public ScalarTypeProbeRequest<T> filterByLargeText(String... largeText){
      if (largeText == null || largeText.length == 0) {
        throw new IllegalArgumentException("filterByLargeText parameter largeText cannot be empty");
      }
      return appendSearchCriteria(createLargeTextCriteria(Operator.EQUAL, (Object[])largeText));
    }

    public ScalarTypeProbeRequest<T> withLargeText(Operator operator, Object... values){
       return appendSearchCriteria(createLargeTextCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withLargeTextIsUnknown(){
       return withLargeText(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withLargeTextIsKnown(){
       return withLargeText(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createLargeTextCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.LARGE_TEXT_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withLargeTextIsNot(String largeText){
       return withLargeText(Operator.NOT_EQUAL, largeText);
    }

    public ScalarTypeProbeRequest<T> withLargeTextIn(String... largeText){
       return withLargeText(Operator.IN, (Object[])largeText);
    }

    public ScalarTypeProbeRequest<T> withLargeTextNotIn(String... largeText){
       return withLargeText(Operator.NOT_IN, (Object[])largeText);
    }
    public ScalarTypeProbeRequest<T> withLargeTextGreaterThan(String largeText){
       return withLargeText(Operator.GREATER_THAN, largeText);
    }

    public ScalarTypeProbeRequest<T> withLargeTextGreaterThanOrEqualTo(String largeText){
       return withLargeText(Operator.GREATER_THAN_OR_EQUAL, largeText);
    }

    public ScalarTypeProbeRequest<T> withLargeTextLessThan(String largeText){
       return withLargeText(Operator.LESS_THAN, largeText);
    }

    public ScalarTypeProbeRequest<T> withLargeTextLessThanOrEqualTo(String largeText){
       return withLargeText(Operator.LESS_THAN_OR_EQUAL, largeText);
    }

    public ScalarTypeProbeRequest<T> withLargeTextBetween(String startOfLargeText, String endOfLargeText){
       return withLargeText(Operator.BETWEEN, startOfLargeText, endOfLargeText);
    }
    public ScalarTypeProbeRequest<T> withLargeTextIs(String largeText){
       return withLargeText(Operator.EQUAL, largeText);
    }



    public ScalarTypeProbeRequest<T> filterByRequiredBoolean(Boolean... requiredBoolean){
      if (requiredBoolean == null || requiredBoolean.length == 0) {
        throw new IllegalArgumentException("filterByRequiredBoolean parameter requiredBoolean cannot be empty");
      }
      return appendSearchCriteria(createRequiredBooleanCriteria(Operator.EQUAL, (Object[])requiredBoolean));
    }

    public ScalarTypeProbeRequest<T> withRequiredBoolean(Operator operator, Object... values){
       return appendSearchCriteria(createRequiredBooleanCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withRequiredBooleanIsUnknown(){
       return withRequiredBoolean(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withRequiredBooleanIsKnown(){
       return withRequiredBoolean(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createRequiredBooleanCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.REQUIRED_BOOLEAN_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> whichAreRequiredBoolean(){
       return withRequiredBoolean(Operator.EQUAL, true);
    }

    public ScalarTypeProbeRequest<T> whichAreNotRequiredBoolean(){
       return withRequiredBoolean(Operator.EQUAL, false);
    }


    public ScalarTypeProbeRequest<T> filterByOptionalBoolean(Boolean... optionalBoolean){
      if (optionalBoolean == null || optionalBoolean.length == 0) {
        throw new IllegalArgumentException("filterByOptionalBoolean parameter optionalBoolean cannot be empty");
      }
      return appendSearchCriteria(createOptionalBooleanCriteria(Operator.EQUAL, (Object[])optionalBoolean));
    }

    public ScalarTypeProbeRequest<T> withOptionalBoolean(Operator operator, Object... values){
       return appendSearchCriteria(createOptionalBooleanCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withOptionalBooleanIsUnknown(){
       return withOptionalBoolean(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withOptionalBooleanIsKnown(){
       return withOptionalBoolean(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createOptionalBooleanCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.OPTIONAL_BOOLEAN_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> whichAreOptionalBoolean(){
       return withOptionalBoolean(Operator.EQUAL, true);
    }

    public ScalarTypeProbeRequest<T> whichAreNotOptionalBoolean(){
       return withOptionalBoolean(Operator.EQUAL, false);
    }


    public ScalarTypeProbeRequest<T> filterByRequiredInteger(Integer... requiredInteger){
      if (requiredInteger == null || requiredInteger.length == 0) {
        throw new IllegalArgumentException("filterByRequiredInteger parameter requiredInteger cannot be empty");
      }
      return appendSearchCriteria(createRequiredIntegerCriteria(Operator.EQUAL, (Object[])requiredInteger));
    }

    public ScalarTypeProbeRequest<T> withRequiredInteger(Operator operator, Object... values){
       return appendSearchCriteria(createRequiredIntegerCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withRequiredIntegerIsUnknown(){
       return withRequiredInteger(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withRequiredIntegerIsKnown(){
       return withRequiredInteger(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createRequiredIntegerCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withRequiredIntegerIs(Integer requiredInteger){
       return withRequiredInteger(Operator.EQUAL, requiredInteger);
    }

    public ScalarTypeProbeRequest<T> withRequiredIntegerIsNot(Integer requiredInteger){
       return withRequiredInteger(Operator.NOT_EQUAL, requiredInteger);
    }

    public ScalarTypeProbeRequest<T> withRequiredIntegerIn(Integer... requiredInteger){
       return withRequiredInteger(Operator.IN, (Object[])requiredInteger);
    }

    public ScalarTypeProbeRequest<T> withRequiredIntegerNotIn(Integer... requiredInteger){
       return withRequiredInteger(Operator.NOT_IN, (Object[])requiredInteger);
    }
    public ScalarTypeProbeRequest<T> withRequiredIntegerGreaterThan(Integer requiredInteger){
       return withRequiredInteger(Operator.GREATER_THAN, requiredInteger);
    }

    public ScalarTypeProbeRequest<T> withRequiredIntegerGreaterThanOrEqualTo(Integer requiredInteger){
       return withRequiredInteger(Operator.GREATER_THAN_OR_EQUAL, requiredInteger);
    }

    public ScalarTypeProbeRequest<T> withRequiredIntegerLessThan(Integer requiredInteger){
       return withRequiredInteger(Operator.LESS_THAN, requiredInteger);
    }

    public ScalarTypeProbeRequest<T> withRequiredIntegerLessThanOrEqualTo(Integer requiredInteger){
       return withRequiredInteger(Operator.LESS_THAN_OR_EQUAL, requiredInteger);
    }

    public ScalarTypeProbeRequest<T> withRequiredIntegerBetween(Integer startOfRequiredInteger, Integer endOfRequiredInteger){
       return withRequiredInteger(Operator.BETWEEN, startOfRequiredInteger, endOfRequiredInteger);
    }



    public ScalarTypeProbeRequest<T> filterByOptionalInteger(Integer... optionalInteger){
      if (optionalInteger == null || optionalInteger.length == 0) {
        throw new IllegalArgumentException("filterByOptionalInteger parameter optionalInteger cannot be empty");
      }
      return appendSearchCriteria(createOptionalIntegerCriteria(Operator.EQUAL, (Object[])optionalInteger));
    }

    public ScalarTypeProbeRequest<T> withOptionalInteger(Operator operator, Object... values){
       return appendSearchCriteria(createOptionalIntegerCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withOptionalIntegerIsUnknown(){
       return withOptionalInteger(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withOptionalIntegerIsKnown(){
       return withOptionalInteger(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createOptionalIntegerCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withOptionalIntegerIs(Integer optionalInteger){
       return withOptionalInteger(Operator.EQUAL, optionalInteger);
    }

    public ScalarTypeProbeRequest<T> withOptionalIntegerIsNot(Integer optionalInteger){
       return withOptionalInteger(Operator.NOT_EQUAL, optionalInteger);
    }

    public ScalarTypeProbeRequest<T> withOptionalIntegerIn(Integer... optionalInteger){
       return withOptionalInteger(Operator.IN, (Object[])optionalInteger);
    }

    public ScalarTypeProbeRequest<T> withOptionalIntegerNotIn(Integer... optionalInteger){
       return withOptionalInteger(Operator.NOT_IN, (Object[])optionalInteger);
    }
    public ScalarTypeProbeRequest<T> withOptionalIntegerGreaterThan(Integer optionalInteger){
       return withOptionalInteger(Operator.GREATER_THAN, optionalInteger);
    }

    public ScalarTypeProbeRequest<T> withOptionalIntegerGreaterThanOrEqualTo(Integer optionalInteger){
       return withOptionalInteger(Operator.GREATER_THAN_OR_EQUAL, optionalInteger);
    }

    public ScalarTypeProbeRequest<T> withOptionalIntegerLessThan(Integer optionalInteger){
       return withOptionalInteger(Operator.LESS_THAN, optionalInteger);
    }

    public ScalarTypeProbeRequest<T> withOptionalIntegerLessThanOrEqualTo(Integer optionalInteger){
       return withOptionalInteger(Operator.LESS_THAN_OR_EQUAL, optionalInteger);
    }

    public ScalarTypeProbeRequest<T> withOptionalIntegerBetween(Integer startOfOptionalInteger, Integer endOfOptionalInteger){
       return withOptionalInteger(Operator.BETWEEN, startOfOptionalInteger, endOfOptionalInteger);
    }



    public ScalarTypeProbeRequest<T> filterByRequiredLong(Long... requiredLong){
      if (requiredLong == null || requiredLong.length == 0) {
        throw new IllegalArgumentException("filterByRequiredLong parameter requiredLong cannot be empty");
      }
      return appendSearchCriteria(createRequiredLongCriteria(Operator.EQUAL, (Object[])requiredLong));
    }

    public ScalarTypeProbeRequest<T> withRequiredLong(Operator operator, Object... values){
       return appendSearchCriteria(createRequiredLongCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withRequiredLongIsUnknown(){
       return withRequiredLong(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withRequiredLongIsKnown(){
       return withRequiredLong(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createRequiredLongCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.REQUIRED_LONG_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withRequiredLongIs(Long requiredLong){
       return withRequiredLong(Operator.EQUAL, requiredLong);
    }

    public ScalarTypeProbeRequest<T> withRequiredLongIsNot(Long requiredLong){
       return withRequiredLong(Operator.NOT_EQUAL, requiredLong);
    }

    public ScalarTypeProbeRequest<T> withRequiredLongIn(Long... requiredLong){
       return withRequiredLong(Operator.IN, (Object[])requiredLong);
    }

    public ScalarTypeProbeRequest<T> withRequiredLongNotIn(Long... requiredLong){
       return withRequiredLong(Operator.NOT_IN, (Object[])requiredLong);
    }
    public ScalarTypeProbeRequest<T> withRequiredLongGreaterThan(Long requiredLong){
       return withRequiredLong(Operator.GREATER_THAN, requiredLong);
    }

    public ScalarTypeProbeRequest<T> withRequiredLongGreaterThanOrEqualTo(Long requiredLong){
       return withRequiredLong(Operator.GREATER_THAN_OR_EQUAL, requiredLong);
    }

    public ScalarTypeProbeRequest<T> withRequiredLongLessThan(Long requiredLong){
       return withRequiredLong(Operator.LESS_THAN, requiredLong);
    }

    public ScalarTypeProbeRequest<T> withRequiredLongLessThanOrEqualTo(Long requiredLong){
       return withRequiredLong(Operator.LESS_THAN_OR_EQUAL, requiredLong);
    }

    public ScalarTypeProbeRequest<T> withRequiredLongBetween(Long startOfRequiredLong, Long endOfRequiredLong){
       return withRequiredLong(Operator.BETWEEN, startOfRequiredLong, endOfRequiredLong);
    }



    public ScalarTypeProbeRequest<T> filterByOptionalLong(Long... optionalLong){
      if (optionalLong == null || optionalLong.length == 0) {
        throw new IllegalArgumentException("filterByOptionalLong parameter optionalLong cannot be empty");
      }
      return appendSearchCriteria(createOptionalLongCriteria(Operator.EQUAL, (Object[])optionalLong));
    }

    public ScalarTypeProbeRequest<T> withOptionalLong(Operator operator, Object... values){
       return appendSearchCriteria(createOptionalLongCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withOptionalLongIsUnknown(){
       return withOptionalLong(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withOptionalLongIsKnown(){
       return withOptionalLong(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createOptionalLongCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.OPTIONAL_LONG_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withOptionalLongIs(Long optionalLong){
       return withOptionalLong(Operator.EQUAL, optionalLong);
    }

    public ScalarTypeProbeRequest<T> withOptionalLongIsNot(Long optionalLong){
       return withOptionalLong(Operator.NOT_EQUAL, optionalLong);
    }

    public ScalarTypeProbeRequest<T> withOptionalLongIn(Long... optionalLong){
       return withOptionalLong(Operator.IN, (Object[])optionalLong);
    }

    public ScalarTypeProbeRequest<T> withOptionalLongNotIn(Long... optionalLong){
       return withOptionalLong(Operator.NOT_IN, (Object[])optionalLong);
    }
    public ScalarTypeProbeRequest<T> withOptionalLongGreaterThan(Long optionalLong){
       return withOptionalLong(Operator.GREATER_THAN, optionalLong);
    }

    public ScalarTypeProbeRequest<T> withOptionalLongGreaterThanOrEqualTo(Long optionalLong){
       return withOptionalLong(Operator.GREATER_THAN_OR_EQUAL, optionalLong);
    }

    public ScalarTypeProbeRequest<T> withOptionalLongLessThan(Long optionalLong){
       return withOptionalLong(Operator.LESS_THAN, optionalLong);
    }

    public ScalarTypeProbeRequest<T> withOptionalLongLessThanOrEqualTo(Long optionalLong){
       return withOptionalLong(Operator.LESS_THAN_OR_EQUAL, optionalLong);
    }

    public ScalarTypeProbeRequest<T> withOptionalLongBetween(Long startOfOptionalLong, Long endOfOptionalLong){
       return withOptionalLong(Operator.BETWEEN, startOfOptionalLong, endOfOptionalLong);
    }



    public ScalarTypeProbeRequest<T> filterByRequiredDecimal(BigDecimal... requiredDecimal){
      if (requiredDecimal == null || requiredDecimal.length == 0) {
        throw new IllegalArgumentException("filterByRequiredDecimal parameter requiredDecimal cannot be empty");
      }
      return appendSearchCriteria(createRequiredDecimalCriteria(Operator.EQUAL, (Object[])requiredDecimal));
    }

    public ScalarTypeProbeRequest<T> withRequiredDecimal(Operator operator, Object... values){
       return appendSearchCriteria(createRequiredDecimalCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withRequiredDecimalIsUnknown(){
       return withRequiredDecimal(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withRequiredDecimalIsKnown(){
       return withRequiredDecimal(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createRequiredDecimalCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withRequiredDecimalIs(BigDecimal requiredDecimal){
       return withRequiredDecimal(Operator.EQUAL, requiredDecimal);
    }

    public ScalarTypeProbeRequest<T> withRequiredDecimalIsNot(BigDecimal requiredDecimal){
       return withRequiredDecimal(Operator.NOT_EQUAL, requiredDecimal);
    }

    public ScalarTypeProbeRequest<T> withRequiredDecimalIn(BigDecimal... requiredDecimal){
       return withRequiredDecimal(Operator.IN, (Object[])requiredDecimal);
    }

    public ScalarTypeProbeRequest<T> withRequiredDecimalNotIn(BigDecimal... requiredDecimal){
       return withRequiredDecimal(Operator.NOT_IN, (Object[])requiredDecimal);
    }
    public ScalarTypeProbeRequest<T> withRequiredDecimalGreaterThan(BigDecimal requiredDecimal){
       return withRequiredDecimal(Operator.GREATER_THAN, requiredDecimal);
    }

    public ScalarTypeProbeRequest<T> withRequiredDecimalGreaterThanOrEqualTo(BigDecimal requiredDecimal){
       return withRequiredDecimal(Operator.GREATER_THAN_OR_EQUAL, requiredDecimal);
    }

    public ScalarTypeProbeRequest<T> withRequiredDecimalLessThan(BigDecimal requiredDecimal){
       return withRequiredDecimal(Operator.LESS_THAN, requiredDecimal);
    }

    public ScalarTypeProbeRequest<T> withRequiredDecimalLessThanOrEqualTo(BigDecimal requiredDecimal){
       return withRequiredDecimal(Operator.LESS_THAN_OR_EQUAL, requiredDecimal);
    }

    public ScalarTypeProbeRequest<T> withRequiredDecimalBetween(BigDecimal startOfRequiredDecimal, BigDecimal endOfRequiredDecimal){
       return withRequiredDecimal(Operator.BETWEEN, startOfRequiredDecimal, endOfRequiredDecimal);
    }



    public ScalarTypeProbeRequest<T> filterByOptionalDecimal(BigDecimal... optionalDecimal){
      if (optionalDecimal == null || optionalDecimal.length == 0) {
        throw new IllegalArgumentException("filterByOptionalDecimal parameter optionalDecimal cannot be empty");
      }
      return appendSearchCriteria(createOptionalDecimalCriteria(Operator.EQUAL, (Object[])optionalDecimal));
    }

    public ScalarTypeProbeRequest<T> withOptionalDecimal(Operator operator, Object... values){
       return appendSearchCriteria(createOptionalDecimalCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withOptionalDecimalIsUnknown(){
       return withOptionalDecimal(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withOptionalDecimalIsKnown(){
       return withOptionalDecimal(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createOptionalDecimalCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withOptionalDecimalIs(BigDecimal optionalDecimal){
       return withOptionalDecimal(Operator.EQUAL, optionalDecimal);
    }

    public ScalarTypeProbeRequest<T> withOptionalDecimalIsNot(BigDecimal optionalDecimal){
       return withOptionalDecimal(Operator.NOT_EQUAL, optionalDecimal);
    }

    public ScalarTypeProbeRequest<T> withOptionalDecimalIn(BigDecimal... optionalDecimal){
       return withOptionalDecimal(Operator.IN, (Object[])optionalDecimal);
    }

    public ScalarTypeProbeRequest<T> withOptionalDecimalNotIn(BigDecimal... optionalDecimal){
       return withOptionalDecimal(Operator.NOT_IN, (Object[])optionalDecimal);
    }
    public ScalarTypeProbeRequest<T> withOptionalDecimalGreaterThan(BigDecimal optionalDecimal){
       return withOptionalDecimal(Operator.GREATER_THAN, optionalDecimal);
    }

    public ScalarTypeProbeRequest<T> withOptionalDecimalGreaterThanOrEqualTo(BigDecimal optionalDecimal){
       return withOptionalDecimal(Operator.GREATER_THAN_OR_EQUAL, optionalDecimal);
    }

    public ScalarTypeProbeRequest<T> withOptionalDecimalLessThan(BigDecimal optionalDecimal){
       return withOptionalDecimal(Operator.LESS_THAN, optionalDecimal);
    }

    public ScalarTypeProbeRequest<T> withOptionalDecimalLessThanOrEqualTo(BigDecimal optionalDecimal){
       return withOptionalDecimal(Operator.LESS_THAN_OR_EQUAL, optionalDecimal);
    }

    public ScalarTypeProbeRequest<T> withOptionalDecimalBetween(BigDecimal startOfOptionalDecimal, BigDecimal endOfOptionalDecimal){
       return withOptionalDecimal(Operator.BETWEEN, startOfOptionalDecimal, endOfOptionalDecimal);
    }



    public ScalarTypeProbeRequest<T> filterByRequiredFloat(Float... requiredFloat){
      if (requiredFloat == null || requiredFloat.length == 0) {
        throw new IllegalArgumentException("filterByRequiredFloat parameter requiredFloat cannot be empty");
      }
      return appendSearchCriteria(createRequiredFloatCriteria(Operator.EQUAL, (Object[])requiredFloat));
    }

    public ScalarTypeProbeRequest<T> withRequiredFloat(Operator operator, Object... values){
       return appendSearchCriteria(createRequiredFloatCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withRequiredFloatIsUnknown(){
       return withRequiredFloat(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withRequiredFloatIsKnown(){
       return withRequiredFloat(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createRequiredFloatCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withRequiredFloatIs(Float requiredFloat){
       return withRequiredFloat(Operator.EQUAL, requiredFloat);
    }

    public ScalarTypeProbeRequest<T> withRequiredFloatIsNot(Float requiredFloat){
       return withRequiredFloat(Operator.NOT_EQUAL, requiredFloat);
    }

    public ScalarTypeProbeRequest<T> withRequiredFloatIn(Float... requiredFloat){
       return withRequiredFloat(Operator.IN, (Object[])requiredFloat);
    }

    public ScalarTypeProbeRequest<T> withRequiredFloatNotIn(Float... requiredFloat){
       return withRequiredFloat(Operator.NOT_IN, (Object[])requiredFloat);
    }
    public ScalarTypeProbeRequest<T> withRequiredFloatGreaterThan(Float requiredFloat){
       return withRequiredFloat(Operator.GREATER_THAN, requiredFloat);
    }

    public ScalarTypeProbeRequest<T> withRequiredFloatGreaterThanOrEqualTo(Float requiredFloat){
       return withRequiredFloat(Operator.GREATER_THAN_OR_EQUAL, requiredFloat);
    }

    public ScalarTypeProbeRequest<T> withRequiredFloatLessThan(Float requiredFloat){
       return withRequiredFloat(Operator.LESS_THAN, requiredFloat);
    }

    public ScalarTypeProbeRequest<T> withRequiredFloatLessThanOrEqualTo(Float requiredFloat){
       return withRequiredFloat(Operator.LESS_THAN_OR_EQUAL, requiredFloat);
    }

    public ScalarTypeProbeRequest<T> withRequiredFloatBetween(Float startOfRequiredFloat, Float endOfRequiredFloat){
       return withRequiredFloat(Operator.BETWEEN, startOfRequiredFloat, endOfRequiredFloat);
    }



    public ScalarTypeProbeRequest<T> filterByOptionalFloat(Float... optionalFloat){
      if (optionalFloat == null || optionalFloat.length == 0) {
        throw new IllegalArgumentException("filterByOptionalFloat parameter optionalFloat cannot be empty");
      }
      return appendSearchCriteria(createOptionalFloatCriteria(Operator.EQUAL, (Object[])optionalFloat));
    }

    public ScalarTypeProbeRequest<T> withOptionalFloat(Operator operator, Object... values){
       return appendSearchCriteria(createOptionalFloatCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withOptionalFloatIsUnknown(){
       return withOptionalFloat(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withOptionalFloatIsKnown(){
       return withOptionalFloat(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createOptionalFloatCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withOptionalFloatIs(Float optionalFloat){
       return withOptionalFloat(Operator.EQUAL, optionalFloat);
    }

    public ScalarTypeProbeRequest<T> withOptionalFloatIsNot(Float optionalFloat){
       return withOptionalFloat(Operator.NOT_EQUAL, optionalFloat);
    }

    public ScalarTypeProbeRequest<T> withOptionalFloatIn(Float... optionalFloat){
       return withOptionalFloat(Operator.IN, (Object[])optionalFloat);
    }

    public ScalarTypeProbeRequest<T> withOptionalFloatNotIn(Float... optionalFloat){
       return withOptionalFloat(Operator.NOT_IN, (Object[])optionalFloat);
    }
    public ScalarTypeProbeRequest<T> withOptionalFloatGreaterThan(Float optionalFloat){
       return withOptionalFloat(Operator.GREATER_THAN, optionalFloat);
    }

    public ScalarTypeProbeRequest<T> withOptionalFloatGreaterThanOrEqualTo(Float optionalFloat){
       return withOptionalFloat(Operator.GREATER_THAN_OR_EQUAL, optionalFloat);
    }

    public ScalarTypeProbeRequest<T> withOptionalFloatLessThan(Float optionalFloat){
       return withOptionalFloat(Operator.LESS_THAN, optionalFloat);
    }

    public ScalarTypeProbeRequest<T> withOptionalFloatLessThanOrEqualTo(Float optionalFloat){
       return withOptionalFloat(Operator.LESS_THAN_OR_EQUAL, optionalFloat);
    }

    public ScalarTypeProbeRequest<T> withOptionalFloatBetween(Float startOfOptionalFloat, Float endOfOptionalFloat){
       return withOptionalFloat(Operator.BETWEEN, startOfOptionalFloat, endOfOptionalFloat);
    }



    public ScalarTypeProbeRequest<T> filterByRequiredDouble(Double... requiredDouble){
      if (requiredDouble == null || requiredDouble.length == 0) {
        throw new IllegalArgumentException("filterByRequiredDouble parameter requiredDouble cannot be empty");
      }
      return appendSearchCriteria(createRequiredDoubleCriteria(Operator.EQUAL, (Object[])requiredDouble));
    }

    public ScalarTypeProbeRequest<T> withRequiredDouble(Operator operator, Object... values){
       return appendSearchCriteria(createRequiredDoubleCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withRequiredDoubleIsUnknown(){
       return withRequiredDouble(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withRequiredDoubleIsKnown(){
       return withRequiredDouble(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createRequiredDoubleCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withRequiredDoubleIs(Double requiredDouble){
       return withRequiredDouble(Operator.EQUAL, requiredDouble);
    }

    public ScalarTypeProbeRequest<T> withRequiredDoubleIsNot(Double requiredDouble){
       return withRequiredDouble(Operator.NOT_EQUAL, requiredDouble);
    }

    public ScalarTypeProbeRequest<T> withRequiredDoubleIn(Double... requiredDouble){
       return withRequiredDouble(Operator.IN, (Object[])requiredDouble);
    }

    public ScalarTypeProbeRequest<T> withRequiredDoubleNotIn(Double... requiredDouble){
       return withRequiredDouble(Operator.NOT_IN, (Object[])requiredDouble);
    }
    public ScalarTypeProbeRequest<T> withRequiredDoubleGreaterThan(Double requiredDouble){
       return withRequiredDouble(Operator.GREATER_THAN, requiredDouble);
    }

    public ScalarTypeProbeRequest<T> withRequiredDoubleGreaterThanOrEqualTo(Double requiredDouble){
       return withRequiredDouble(Operator.GREATER_THAN_OR_EQUAL, requiredDouble);
    }

    public ScalarTypeProbeRequest<T> withRequiredDoubleLessThan(Double requiredDouble){
       return withRequiredDouble(Operator.LESS_THAN, requiredDouble);
    }

    public ScalarTypeProbeRequest<T> withRequiredDoubleLessThanOrEqualTo(Double requiredDouble){
       return withRequiredDouble(Operator.LESS_THAN_OR_EQUAL, requiredDouble);
    }

    public ScalarTypeProbeRequest<T> withRequiredDoubleBetween(Double startOfRequiredDouble, Double endOfRequiredDouble){
       return withRequiredDouble(Operator.BETWEEN, startOfRequiredDouble, endOfRequiredDouble);
    }



    public ScalarTypeProbeRequest<T> filterByOptionalDouble(Double... optionalDouble){
      if (optionalDouble == null || optionalDouble.length == 0) {
        throw new IllegalArgumentException("filterByOptionalDouble parameter optionalDouble cannot be empty");
      }
      return appendSearchCriteria(createOptionalDoubleCriteria(Operator.EQUAL, (Object[])optionalDouble));
    }

    public ScalarTypeProbeRequest<T> withOptionalDouble(Operator operator, Object... values){
       return appendSearchCriteria(createOptionalDoubleCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withOptionalDoubleIsUnknown(){
       return withOptionalDouble(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withOptionalDoubleIsKnown(){
       return withOptionalDouble(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createOptionalDoubleCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withOptionalDoubleIs(Double optionalDouble){
       return withOptionalDouble(Operator.EQUAL, optionalDouble);
    }

    public ScalarTypeProbeRequest<T> withOptionalDoubleIsNot(Double optionalDouble){
       return withOptionalDouble(Operator.NOT_EQUAL, optionalDouble);
    }

    public ScalarTypeProbeRequest<T> withOptionalDoubleIn(Double... optionalDouble){
       return withOptionalDouble(Operator.IN, (Object[])optionalDouble);
    }

    public ScalarTypeProbeRequest<T> withOptionalDoubleNotIn(Double... optionalDouble){
       return withOptionalDouble(Operator.NOT_IN, (Object[])optionalDouble);
    }
    public ScalarTypeProbeRequest<T> withOptionalDoubleGreaterThan(Double optionalDouble){
       return withOptionalDouble(Operator.GREATER_THAN, optionalDouble);
    }

    public ScalarTypeProbeRequest<T> withOptionalDoubleGreaterThanOrEqualTo(Double optionalDouble){
       return withOptionalDouble(Operator.GREATER_THAN_OR_EQUAL, optionalDouble);
    }

    public ScalarTypeProbeRequest<T> withOptionalDoubleLessThan(Double optionalDouble){
       return withOptionalDouble(Operator.LESS_THAN, optionalDouble);
    }

    public ScalarTypeProbeRequest<T> withOptionalDoubleLessThanOrEqualTo(Double optionalDouble){
       return withOptionalDouble(Operator.LESS_THAN_OR_EQUAL, optionalDouble);
    }

    public ScalarTypeProbeRequest<T> withOptionalDoubleBetween(Double startOfOptionalDouble, Double endOfOptionalDouble){
       return withOptionalDouble(Operator.BETWEEN, startOfOptionalDouble, endOfOptionalDouble);
    }



    public ScalarTypeProbeRequest<T> filterByRequiredDate(LocalDate... requiredDate){
      if (requiredDate == null || requiredDate.length == 0) {
        throw new IllegalArgumentException("filterByRequiredDate parameter requiredDate cannot be empty");
      }
      return appendSearchCriteria(createRequiredDateCriteria(Operator.EQUAL, (Object[])requiredDate));
    }

    public ScalarTypeProbeRequest<T> withRequiredDate(Operator operator, Object... values){
       return appendSearchCriteria(createRequiredDateCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withRequiredDateIsUnknown(){
       return withRequiredDate(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withRequiredDateIsKnown(){
       return withRequiredDate(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createRequiredDateCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.REQUIRED_DATE_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withRequiredDateIs(LocalDate requiredDate){
       return withRequiredDate(Operator.EQUAL, requiredDate);
    }

    public ScalarTypeProbeRequest<T> withRequiredDateIsNot(LocalDate requiredDate){
       return withRequiredDate(Operator.NOT_EQUAL, requiredDate);
    }

    public ScalarTypeProbeRequest<T> withRequiredDateIn(LocalDate... requiredDate){
       return withRequiredDate(Operator.IN, (Object[])requiredDate);
    }

    public ScalarTypeProbeRequest<T> withRequiredDateNotIn(LocalDate... requiredDate){
       return withRequiredDate(Operator.NOT_IN, (Object[])requiredDate);
    }
    public ScalarTypeProbeRequest<T> withRequiredDateGreaterThan(LocalDate requiredDate){
       return withRequiredDate(Operator.GREATER_THAN, requiredDate);
    }

    public ScalarTypeProbeRequest<T> withRequiredDateGreaterThanOrEqualTo(LocalDate requiredDate){
       return withRequiredDate(Operator.GREATER_THAN_OR_EQUAL, requiredDate);
    }

    public ScalarTypeProbeRequest<T> withRequiredDateLessThan(LocalDate requiredDate){
       return withRequiredDate(Operator.LESS_THAN, requiredDate);
    }

    public ScalarTypeProbeRequest<T> withRequiredDateLessThanOrEqualTo(LocalDate requiredDate){
       return withRequiredDate(Operator.LESS_THAN_OR_EQUAL, requiredDate);
    }

    public ScalarTypeProbeRequest<T> withRequiredDateBetween(LocalDate startOfRequiredDate, LocalDate endOfRequiredDate){
       return withRequiredDate(Operator.BETWEEN, startOfRequiredDate, endOfRequiredDate);
    }
    public ScalarTypeProbeRequest<T> withRequiredDateBefore(LocalDate requiredDate){
       return withRequiredDate(Operator.LESS_THAN, requiredDate);
    }

    public ScalarTypeProbeRequest<T> withRequiredDateBefore(Date requiredDate){
       return withRequiredDate(Operator.LESS_THAN, requiredDate);
    }

    public ScalarTypeProbeRequest<T> withRequiredDateAfter(LocalDate requiredDate){
       return withRequiredDate(Operator.GREATER_THAN, requiredDate);
    }

    public ScalarTypeProbeRequest<T> withRequiredDateAfter(Date requiredDate){
       return withRequiredDate(Operator.GREATER_THAN, requiredDate);
    }

    public ScalarTypeProbeRequest<T> withRequiredDateBetween(Date startOfRequiredDate, Date endOfRequiredDate){
       return withRequiredDate(Operator.BETWEEN, startOfRequiredDate, endOfRequiredDate);
    }




    public ScalarTypeProbeRequest<T> filterByOptionalDate(LocalDate... optionalDate){
      if (optionalDate == null || optionalDate.length == 0) {
        throw new IllegalArgumentException("filterByOptionalDate parameter optionalDate cannot be empty");
      }
      return appendSearchCriteria(createOptionalDateCriteria(Operator.EQUAL, (Object[])optionalDate));
    }

    public ScalarTypeProbeRequest<T> withOptionalDate(Operator operator, Object... values){
       return appendSearchCriteria(createOptionalDateCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withOptionalDateIsUnknown(){
       return withOptionalDate(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withOptionalDateIsKnown(){
       return withOptionalDate(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createOptionalDateCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.OPTIONAL_DATE_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withOptionalDateIs(LocalDate optionalDate){
       return withOptionalDate(Operator.EQUAL, optionalDate);
    }

    public ScalarTypeProbeRequest<T> withOptionalDateIsNot(LocalDate optionalDate){
       return withOptionalDate(Operator.NOT_EQUAL, optionalDate);
    }

    public ScalarTypeProbeRequest<T> withOptionalDateIn(LocalDate... optionalDate){
       return withOptionalDate(Operator.IN, (Object[])optionalDate);
    }

    public ScalarTypeProbeRequest<T> withOptionalDateNotIn(LocalDate... optionalDate){
       return withOptionalDate(Operator.NOT_IN, (Object[])optionalDate);
    }
    public ScalarTypeProbeRequest<T> withOptionalDateGreaterThan(LocalDate optionalDate){
       return withOptionalDate(Operator.GREATER_THAN, optionalDate);
    }

    public ScalarTypeProbeRequest<T> withOptionalDateGreaterThanOrEqualTo(LocalDate optionalDate){
       return withOptionalDate(Operator.GREATER_THAN_OR_EQUAL, optionalDate);
    }

    public ScalarTypeProbeRequest<T> withOptionalDateLessThan(LocalDate optionalDate){
       return withOptionalDate(Operator.LESS_THAN, optionalDate);
    }

    public ScalarTypeProbeRequest<T> withOptionalDateLessThanOrEqualTo(LocalDate optionalDate){
       return withOptionalDate(Operator.LESS_THAN_OR_EQUAL, optionalDate);
    }

    public ScalarTypeProbeRequest<T> withOptionalDateBetween(LocalDate startOfOptionalDate, LocalDate endOfOptionalDate){
       return withOptionalDate(Operator.BETWEEN, startOfOptionalDate, endOfOptionalDate);
    }
    public ScalarTypeProbeRequest<T> withOptionalDateBefore(LocalDate optionalDate){
       return withOptionalDate(Operator.LESS_THAN, optionalDate);
    }

    public ScalarTypeProbeRequest<T> withOptionalDateBefore(Date optionalDate){
       return withOptionalDate(Operator.LESS_THAN, optionalDate);
    }

    public ScalarTypeProbeRequest<T> withOptionalDateAfter(LocalDate optionalDate){
       return withOptionalDate(Operator.GREATER_THAN, optionalDate);
    }

    public ScalarTypeProbeRequest<T> withOptionalDateAfter(Date optionalDate){
       return withOptionalDate(Operator.GREATER_THAN, optionalDate);
    }

    public ScalarTypeProbeRequest<T> withOptionalDateBetween(Date startOfOptionalDate, Date endOfOptionalDate){
       return withOptionalDate(Operator.BETWEEN, startOfOptionalDate, endOfOptionalDate);
    }




    public ScalarTypeProbeRequest<T> filterByRequiredTime(LocalTime... requiredTime){
      if (requiredTime == null || requiredTime.length == 0) {
        throw new IllegalArgumentException("filterByRequiredTime parameter requiredTime cannot be empty");
      }
      return appendSearchCriteria(createRequiredTimeCriteria(Operator.EQUAL, (Object[])requiredTime));
    }

    public ScalarTypeProbeRequest<T> withRequiredTime(Operator operator, Object... values){
       return appendSearchCriteria(createRequiredTimeCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withRequiredTimeIsUnknown(){
       return withRequiredTime(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withRequiredTimeIsKnown(){
       return withRequiredTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createRequiredTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.REQUIRED_TIME_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withRequiredTimeIs(LocalTime requiredTime){
       return withRequiredTime(Operator.EQUAL, requiredTime);
    }

    public ScalarTypeProbeRequest<T> withRequiredTimeIsNot(LocalTime requiredTime){
       return withRequiredTime(Operator.NOT_EQUAL, requiredTime);
    }

    public ScalarTypeProbeRequest<T> withRequiredTimeIn(LocalTime... requiredTime){
       return withRequiredTime(Operator.IN, (Object[])requiredTime);
    }

    public ScalarTypeProbeRequest<T> withRequiredTimeNotIn(LocalTime... requiredTime){
       return withRequiredTime(Operator.NOT_IN, (Object[])requiredTime);
    }
    public ScalarTypeProbeRequest<T> withRequiredTimeGreaterThan(LocalTime requiredTime){
       return withRequiredTime(Operator.GREATER_THAN, requiredTime);
    }

    public ScalarTypeProbeRequest<T> withRequiredTimeGreaterThanOrEqualTo(LocalTime requiredTime){
       return withRequiredTime(Operator.GREATER_THAN_OR_EQUAL, requiredTime);
    }

    public ScalarTypeProbeRequest<T> withRequiredTimeLessThan(LocalTime requiredTime){
       return withRequiredTime(Operator.LESS_THAN, requiredTime);
    }

    public ScalarTypeProbeRequest<T> withRequiredTimeLessThanOrEqualTo(LocalTime requiredTime){
       return withRequiredTime(Operator.LESS_THAN_OR_EQUAL, requiredTime);
    }

    public ScalarTypeProbeRequest<T> withRequiredTimeBetween(LocalTime startOfRequiredTime, LocalTime endOfRequiredTime){
       return withRequiredTime(Operator.BETWEEN, startOfRequiredTime, endOfRequiredTime);
    }


    public ScalarTypeProbeRequest<T> filterByOptionalTime(LocalTime... optionalTime){
      if (optionalTime == null || optionalTime.length == 0) {
        throw new IllegalArgumentException("filterByOptionalTime parameter optionalTime cannot be empty");
      }
      return appendSearchCriteria(createOptionalTimeCriteria(Operator.EQUAL, (Object[])optionalTime));
    }

    public ScalarTypeProbeRequest<T> withOptionalTime(Operator operator, Object... values){
       return appendSearchCriteria(createOptionalTimeCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withOptionalTimeIsUnknown(){
       return withOptionalTime(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withOptionalTimeIsKnown(){
       return withOptionalTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createOptionalTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.OPTIONAL_TIME_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withOptionalTimeIs(LocalTime optionalTime){
       return withOptionalTime(Operator.EQUAL, optionalTime);
    }

    public ScalarTypeProbeRequest<T> withOptionalTimeIsNot(LocalTime optionalTime){
       return withOptionalTime(Operator.NOT_EQUAL, optionalTime);
    }

    public ScalarTypeProbeRequest<T> withOptionalTimeIn(LocalTime... optionalTime){
       return withOptionalTime(Operator.IN, (Object[])optionalTime);
    }

    public ScalarTypeProbeRequest<T> withOptionalTimeNotIn(LocalTime... optionalTime){
       return withOptionalTime(Operator.NOT_IN, (Object[])optionalTime);
    }
    public ScalarTypeProbeRequest<T> withOptionalTimeGreaterThan(LocalTime optionalTime){
       return withOptionalTime(Operator.GREATER_THAN, optionalTime);
    }

    public ScalarTypeProbeRequest<T> withOptionalTimeGreaterThanOrEqualTo(LocalTime optionalTime){
       return withOptionalTime(Operator.GREATER_THAN_OR_EQUAL, optionalTime);
    }

    public ScalarTypeProbeRequest<T> withOptionalTimeLessThan(LocalTime optionalTime){
       return withOptionalTime(Operator.LESS_THAN, optionalTime);
    }

    public ScalarTypeProbeRequest<T> withOptionalTimeLessThanOrEqualTo(LocalTime optionalTime){
       return withOptionalTime(Operator.LESS_THAN_OR_EQUAL, optionalTime);
    }

    public ScalarTypeProbeRequest<T> withOptionalTimeBetween(LocalTime startOfOptionalTime, LocalTime endOfOptionalTime){
       return withOptionalTime(Operator.BETWEEN, startOfOptionalTime, endOfOptionalTime);
    }


    public ScalarTypeProbeRequest<T> filterByRequiredDatetime(LocalDateTime... requiredDatetime){
      if (requiredDatetime == null || requiredDatetime.length == 0) {
        throw new IllegalArgumentException("filterByRequiredDatetime parameter requiredDatetime cannot be empty");
      }
      return appendSearchCriteria(createRequiredDatetimeCriteria(Operator.EQUAL, (Object[])requiredDatetime));
    }

    public ScalarTypeProbeRequest<T> withRequiredDatetime(Operator operator, Object... values){
       return appendSearchCriteria(createRequiredDatetimeCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withRequiredDatetimeIsUnknown(){
       return withRequiredDatetime(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withRequiredDatetimeIsKnown(){
       return withRequiredDatetime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createRequiredDatetimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.REQUIRED_DATETIME_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withRequiredDatetimeIs(LocalDateTime requiredDatetime){
       return withRequiredDatetime(Operator.EQUAL, requiredDatetime);
    }

    public ScalarTypeProbeRequest<T> withRequiredDatetimeIsNot(LocalDateTime requiredDatetime){
       return withRequiredDatetime(Operator.NOT_EQUAL, requiredDatetime);
    }

    public ScalarTypeProbeRequest<T> withRequiredDatetimeIn(LocalDateTime... requiredDatetime){
       return withRequiredDatetime(Operator.IN, (Object[])requiredDatetime);
    }

    public ScalarTypeProbeRequest<T> withRequiredDatetimeNotIn(LocalDateTime... requiredDatetime){
       return withRequiredDatetime(Operator.NOT_IN, (Object[])requiredDatetime);
    }
    public ScalarTypeProbeRequest<T> withRequiredDatetimeGreaterThan(LocalDateTime requiredDatetime){
       return withRequiredDatetime(Operator.GREATER_THAN, requiredDatetime);
    }

    public ScalarTypeProbeRequest<T> withRequiredDatetimeGreaterThanOrEqualTo(LocalDateTime requiredDatetime){
       return withRequiredDatetime(Operator.GREATER_THAN_OR_EQUAL, requiredDatetime);
    }

    public ScalarTypeProbeRequest<T> withRequiredDatetimeLessThan(LocalDateTime requiredDatetime){
       return withRequiredDatetime(Operator.LESS_THAN, requiredDatetime);
    }

    public ScalarTypeProbeRequest<T> withRequiredDatetimeLessThanOrEqualTo(LocalDateTime requiredDatetime){
       return withRequiredDatetime(Operator.LESS_THAN_OR_EQUAL, requiredDatetime);
    }

    public ScalarTypeProbeRequest<T> withRequiredDatetimeBetween(LocalDateTime startOfRequiredDatetime, LocalDateTime endOfRequiredDatetime){
       return withRequiredDatetime(Operator.BETWEEN, startOfRequiredDatetime, endOfRequiredDatetime);
    }
    public ScalarTypeProbeRequest<T> withRequiredDatetimeBefore(LocalDateTime requiredDatetime){
       return withRequiredDatetime(Operator.LESS_THAN, requiredDatetime);
    }

    public ScalarTypeProbeRequest<T> withRequiredDatetimeBefore(Date requiredDatetime){
       return withRequiredDatetime(Operator.LESS_THAN, requiredDatetime);
    }

    public ScalarTypeProbeRequest<T> withRequiredDatetimeAfter(LocalDateTime requiredDatetime){
       return withRequiredDatetime(Operator.GREATER_THAN, requiredDatetime);
    }

    public ScalarTypeProbeRequest<T> withRequiredDatetimeAfter(Date requiredDatetime){
       return withRequiredDatetime(Operator.GREATER_THAN, requiredDatetime);
    }

    public ScalarTypeProbeRequest<T> withRequiredDatetimeBetween(Date startOfRequiredDatetime, Date endOfRequiredDatetime){
       return withRequiredDatetime(Operator.BETWEEN, startOfRequiredDatetime, endOfRequiredDatetime);
    }




    public ScalarTypeProbeRequest<T> filterByOptionalDatetime(LocalDateTime... optionalDatetime){
      if (optionalDatetime == null || optionalDatetime.length == 0) {
        throw new IllegalArgumentException("filterByOptionalDatetime parameter optionalDatetime cannot be empty");
      }
      return appendSearchCriteria(createOptionalDatetimeCriteria(Operator.EQUAL, (Object[])optionalDatetime));
    }

    public ScalarTypeProbeRequest<T> withOptionalDatetime(Operator operator, Object... values){
       return appendSearchCriteria(createOptionalDatetimeCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withOptionalDatetimeIsUnknown(){
       return withOptionalDatetime(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withOptionalDatetimeIsKnown(){
       return withOptionalDatetime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createOptionalDatetimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.OPTIONAL_DATETIME_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withOptionalDatetimeIs(LocalDateTime optionalDatetime){
       return withOptionalDatetime(Operator.EQUAL, optionalDatetime);
    }

    public ScalarTypeProbeRequest<T> withOptionalDatetimeIsNot(LocalDateTime optionalDatetime){
       return withOptionalDatetime(Operator.NOT_EQUAL, optionalDatetime);
    }

    public ScalarTypeProbeRequest<T> withOptionalDatetimeIn(LocalDateTime... optionalDatetime){
       return withOptionalDatetime(Operator.IN, (Object[])optionalDatetime);
    }

    public ScalarTypeProbeRequest<T> withOptionalDatetimeNotIn(LocalDateTime... optionalDatetime){
       return withOptionalDatetime(Operator.NOT_IN, (Object[])optionalDatetime);
    }
    public ScalarTypeProbeRequest<T> withOptionalDatetimeGreaterThan(LocalDateTime optionalDatetime){
       return withOptionalDatetime(Operator.GREATER_THAN, optionalDatetime);
    }

    public ScalarTypeProbeRequest<T> withOptionalDatetimeGreaterThanOrEqualTo(LocalDateTime optionalDatetime){
       return withOptionalDatetime(Operator.GREATER_THAN_OR_EQUAL, optionalDatetime);
    }

    public ScalarTypeProbeRequest<T> withOptionalDatetimeLessThan(LocalDateTime optionalDatetime){
       return withOptionalDatetime(Operator.LESS_THAN, optionalDatetime);
    }

    public ScalarTypeProbeRequest<T> withOptionalDatetimeLessThanOrEqualTo(LocalDateTime optionalDatetime){
       return withOptionalDatetime(Operator.LESS_THAN_OR_EQUAL, optionalDatetime);
    }

    public ScalarTypeProbeRequest<T> withOptionalDatetimeBetween(LocalDateTime startOfOptionalDatetime, LocalDateTime endOfOptionalDatetime){
       return withOptionalDatetime(Operator.BETWEEN, startOfOptionalDatetime, endOfOptionalDatetime);
    }
    public ScalarTypeProbeRequest<T> withOptionalDatetimeBefore(LocalDateTime optionalDatetime){
       return withOptionalDatetime(Operator.LESS_THAN, optionalDatetime);
    }

    public ScalarTypeProbeRequest<T> withOptionalDatetimeBefore(Date optionalDatetime){
       return withOptionalDatetime(Operator.LESS_THAN, optionalDatetime);
    }

    public ScalarTypeProbeRequest<T> withOptionalDatetimeAfter(LocalDateTime optionalDatetime){
       return withOptionalDatetime(Operator.GREATER_THAN, optionalDatetime);
    }

    public ScalarTypeProbeRequest<T> withOptionalDatetimeAfter(Date optionalDatetime){
       return withOptionalDatetime(Operator.GREATER_THAN, optionalDatetime);
    }

    public ScalarTypeProbeRequest<T> withOptionalDatetimeBetween(Date startOfOptionalDatetime, Date endOfOptionalDatetime){
       return withOptionalDatetime(Operator.BETWEEN, startOfOptionalDatetime, endOfOptionalDatetime);
    }




    public ScalarTypeProbeRequest<T> filterByCreateTime(LocalDateTime... createTime){
      if (createTime == null || createTime.length == 0) {
        throw new IllegalArgumentException("filterByCreateTime parameter createTime cannot be empty");
      }
      return appendSearchCriteria(createCreateTimeCriteria(Operator.EQUAL, (Object[])createTime));
    }

    public ScalarTypeProbeRequest<T> withCreateTime(Operator operator, Object... values){
       return appendSearchCriteria(createCreateTimeCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withCreateTimeIsUnknown(){
       return withCreateTime(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withCreateTimeIsKnown(){
       return withCreateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createCreateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.CREATE_TIME_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withCreateTimeIs(LocalDateTime createTime){
       return withCreateTime(Operator.EQUAL, createTime);
    }

    public ScalarTypeProbeRequest<T> withCreateTimeIsNot(LocalDateTime createTime){
       return withCreateTime(Operator.NOT_EQUAL, createTime);
    }

    public ScalarTypeProbeRequest<T> withCreateTimeIn(LocalDateTime... createTime){
       return withCreateTime(Operator.IN, (Object[])createTime);
    }

    public ScalarTypeProbeRequest<T> withCreateTimeNotIn(LocalDateTime... createTime){
       return withCreateTime(Operator.NOT_IN, (Object[])createTime);
    }
    public ScalarTypeProbeRequest<T> withCreateTimeGreaterThan(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public ScalarTypeProbeRequest<T> withCreateTimeGreaterThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN_OR_EQUAL, createTime);
    }

    public ScalarTypeProbeRequest<T> withCreateTimeLessThan(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public ScalarTypeProbeRequest<T> withCreateTimeLessThanOrEqualTo(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN_OR_EQUAL, createTime);
    }

    public ScalarTypeProbeRequest<T> withCreateTimeBetween(LocalDateTime startOfCreateTime, LocalDateTime endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }
    public ScalarTypeProbeRequest<T> withCreateTimeBefore(LocalDateTime createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public ScalarTypeProbeRequest<T> withCreateTimeBefore(Date createTime){
       return withCreateTime(Operator.LESS_THAN, createTime);
    }

    public ScalarTypeProbeRequest<T> withCreateTimeAfter(LocalDateTime createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public ScalarTypeProbeRequest<T> withCreateTimeAfter(Date createTime){
       return withCreateTime(Operator.GREATER_THAN, createTime);
    }

    public ScalarTypeProbeRequest<T> withCreateTimeBetween(Date startOfCreateTime, Date endOfCreateTime){
       return withCreateTime(Operator.BETWEEN, startOfCreateTime, endOfCreateTime);
    }




    public ScalarTypeProbeRequest<T> filterByUpdateTime(LocalDateTime... updateTime){
      if (updateTime == null || updateTime.length == 0) {
        throw new IllegalArgumentException("filterByUpdateTime parameter updateTime cannot be empty");
      }
      return appendSearchCriteria(createUpdateTimeCriteria(Operator.EQUAL, (Object[])updateTime));
    }

    public ScalarTypeProbeRequest<T> withUpdateTime(Operator operator, Object... values){
       return appendSearchCriteria(createUpdateTimeCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withUpdateTimeIsUnknown(){
       return withUpdateTime(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withUpdateTimeIsKnown(){
       return withUpdateTime(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createUpdateTimeCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.UPDATE_TIME_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withUpdateTimeIs(LocalDateTime updateTime){
       return withUpdateTime(Operator.EQUAL, updateTime);
    }

    public ScalarTypeProbeRequest<T> withUpdateTimeIsNot(LocalDateTime updateTime){
       return withUpdateTime(Operator.NOT_EQUAL, updateTime);
    }

    public ScalarTypeProbeRequest<T> withUpdateTimeIn(LocalDateTime... updateTime){
       return withUpdateTime(Operator.IN, (Object[])updateTime);
    }

    public ScalarTypeProbeRequest<T> withUpdateTimeNotIn(LocalDateTime... updateTime){
       return withUpdateTime(Operator.NOT_IN, (Object[])updateTime);
    }
    public ScalarTypeProbeRequest<T> withUpdateTimeGreaterThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public ScalarTypeProbeRequest<T> withUpdateTimeGreaterThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN_OR_EQUAL, updateTime);
    }

    public ScalarTypeProbeRequest<T> withUpdateTimeLessThan(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public ScalarTypeProbeRequest<T> withUpdateTimeLessThanOrEqualTo(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN_OR_EQUAL, updateTime);
    }

    public ScalarTypeProbeRequest<T> withUpdateTimeBetween(LocalDateTime startOfUpdateTime, LocalDateTime endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
    }
    public ScalarTypeProbeRequest<T> withUpdateTimeBefore(LocalDateTime updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public ScalarTypeProbeRequest<T> withUpdateTimeBefore(Date updateTime){
       return withUpdateTime(Operator.LESS_THAN, updateTime);
    }

    public ScalarTypeProbeRequest<T> withUpdateTimeAfter(LocalDateTime updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public ScalarTypeProbeRequest<T> withUpdateTimeAfter(Date updateTime){
       return withUpdateTime(Operator.GREATER_THAN, updateTime);
    }

    public ScalarTypeProbeRequest<T> withUpdateTimeBetween(Date startOfUpdateTime, Date endOfUpdateTime){
       return withUpdateTime(Operator.BETWEEN, startOfUpdateTime, endOfUpdateTime);
    }




    public ScalarTypeProbeRequest<T> filterByVersion(Long... version){
      if (version == null || version.length == 0) {
        throw new IllegalArgumentException("filterByVersion parameter version cannot be empty");
      }
      return appendSearchCriteria(createVersionCriteria(Operator.EQUAL, (Object[])version));
    }

    public ScalarTypeProbeRequest<T> withVersion(Operator operator, Object... values){
       return appendSearchCriteria(createVersionCriteria(operator, values));
    }

    public ScalarTypeProbeRequest<T> withVersionIsUnknown(){
       return withVersion(Operator.IS_NULL);
    }

    public ScalarTypeProbeRequest<T> withVersionIsKnown(){
       return withVersion(Operator.IS_NOT_NULL);
    }

    public SearchCriteria createVersionCriteria(Operator operator, Object... values) {
        return createBasicSearchCriteria(ScalarTypeProbe.VERSION_PROPERTY, operator, values);
    }

    public ScalarTypeProbeRequest<T> withVersionIs(Long version){
       return withVersion(Operator.EQUAL, version);
    }

    public ScalarTypeProbeRequest<T> withVersionIsNot(Long version){
       return withVersion(Operator.NOT_EQUAL, version);
    }

    public ScalarTypeProbeRequest<T> withVersionIn(Long... version){
       return withVersion(Operator.IN, (Object[])version);
    }

    public ScalarTypeProbeRequest<T> withVersionNotIn(Long... version){
       return withVersion(Operator.NOT_IN, (Object[])version);
    }
    public ScalarTypeProbeRequest<T> withVersionGreaterThan(Long version){
       return withVersion(Operator.GREATER_THAN, version);
    }

    public ScalarTypeProbeRequest<T> withVersionGreaterThanOrEqualTo(Long version){
       return withVersion(Operator.GREATER_THAN_OR_EQUAL, version);
    }

    public ScalarTypeProbeRequest<T> withVersionLessThan(Long version){
       return withVersion(Operator.LESS_THAN, version);
    }

    public ScalarTypeProbeRequest<T> withVersionLessThanOrEqualTo(Long version){
       return withVersion(Operator.LESS_THAN_OR_EQUAL, version);
    }

    public ScalarTypeProbeRequest<T> withVersionBetween(Long startOfVersion, Long endOfVersion){
       return withVersion(Operator.BETWEEN, startOfVersion, endOfVersion);
    }



    public ScalarTypeProbeRequest<T> count(){
        super.count();
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "count", null, null));
        return this;
    }
    public ScalarTypeProbeRequest<T> countAs(String retName){
        super.count(retName);
        setPageCountOperation(io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.NAMED_COUNT, "countAs", retName, null));
        return this;
    }
    public ScalarTypeProbeRequest minRequiredInteger(){
        return minRequiredIntegerAs(prefix("minOf",ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY));
    }

    public ScalarTypeProbeRequest minRequiredIntegerAs(String retName){
        super.min(retName, ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest maxRequiredInteger(){
        return maxRequiredIntegerAs(prefix("maxOf",ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY));
    }

    public ScalarTypeProbeRequest maxRequiredIntegerAs(String retName){
        super.max(retName, ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sumRequiredInteger(){
        return sumRequiredIntegerAs(prefix("sumOf",ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY));
    }

    public ScalarTypeProbeRequest sumRequiredIntegerAs(String retName){
        super.sum(retName, ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest avgRequiredInteger(){
        return avgRequiredIntegerAs(prefix("avgOf",ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY));
    }

    public ScalarTypeProbeRequest avgRequiredIntegerAs(String retName){
        super.avg(retName, ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest standardDeviationRequiredInteger(){
        return standardDeviationRequiredIntegerAs(prefix("standardDeviationOf",ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY));
    }

    public ScalarTypeProbeRequest standardDeviationRequiredIntegerAs(String retName){
        super.standardDeviation(retName, ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationRequiredInteger(){
        return squareRootOfPopulationStandardDeviationRequiredIntegerAs(prefix("squareRootOfPopulationStandardDeviationOf",ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY));
    }

    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationRequiredIntegerAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sampleVarianceRequiredInteger(){
        return sampleVarianceRequiredIntegerAs(prefix("sampleVarianceOf",ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY));
    }

    public ScalarTypeProbeRequest sampleVarianceRequiredIntegerAs(String retName){
        super.sampleVariance(retName, ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest samplePopulationVarianceRequiredInteger(){
        return samplePopulationVarianceRequiredIntegerAs(prefix("samplePopulationVarianceOf",ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY));
    }

    public ScalarTypeProbeRequest samplePopulationVarianceRequiredIntegerAs(String retName){
        super.samplePopulationVariance(retName, ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest minOptionalInteger(){
        return minOptionalIntegerAs(prefix("minOf",ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY));
    }

    public ScalarTypeProbeRequest minOptionalIntegerAs(String retName){
        super.min(retName, ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest maxOptionalInteger(){
        return maxOptionalIntegerAs(prefix("maxOf",ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY));
    }

    public ScalarTypeProbeRequest maxOptionalIntegerAs(String retName){
        super.max(retName, ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sumOptionalInteger(){
        return sumOptionalIntegerAs(prefix("sumOf",ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY));
    }

    public ScalarTypeProbeRequest sumOptionalIntegerAs(String retName){
        super.sum(retName, ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest avgOptionalInteger(){
        return avgOptionalIntegerAs(prefix("avgOf",ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY));
    }

    public ScalarTypeProbeRequest avgOptionalIntegerAs(String retName){
        super.avg(retName, ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest standardDeviationOptionalInteger(){
        return standardDeviationOptionalIntegerAs(prefix("standardDeviationOf",ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY));
    }

    public ScalarTypeProbeRequest standardDeviationOptionalIntegerAs(String retName){
        super.standardDeviation(retName, ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationOptionalInteger(){
        return squareRootOfPopulationStandardDeviationOptionalIntegerAs(prefix("squareRootOfPopulationStandardDeviationOf",ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY));
    }

    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationOptionalIntegerAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sampleVarianceOptionalInteger(){
        return sampleVarianceOptionalIntegerAs(prefix("sampleVarianceOf",ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY));
    }

    public ScalarTypeProbeRequest sampleVarianceOptionalIntegerAs(String retName){
        super.sampleVariance(retName, ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest samplePopulationVarianceOptionalInteger(){
        return samplePopulationVarianceOptionalIntegerAs(prefix("samplePopulationVarianceOf",ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY));
    }

    public ScalarTypeProbeRequest samplePopulationVarianceOptionalIntegerAs(String retName){
        super.samplePopulationVariance(retName, ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest minRequiredLong(){
        return minRequiredLongAs(prefix("minOf",ScalarTypeProbe.REQUIRED_LONG_PROPERTY));
    }

    public ScalarTypeProbeRequest minRequiredLongAs(String retName){
        super.min(retName, ScalarTypeProbe.REQUIRED_LONG_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest maxRequiredLong(){
        return maxRequiredLongAs(prefix("maxOf",ScalarTypeProbe.REQUIRED_LONG_PROPERTY));
    }

    public ScalarTypeProbeRequest maxRequiredLongAs(String retName){
        super.max(retName, ScalarTypeProbe.REQUIRED_LONG_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sumRequiredLong(){
        return sumRequiredLongAs(prefix("sumOf",ScalarTypeProbe.REQUIRED_LONG_PROPERTY));
    }

    public ScalarTypeProbeRequest sumRequiredLongAs(String retName){
        super.sum(retName, ScalarTypeProbe.REQUIRED_LONG_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest avgRequiredLong(){
        return avgRequiredLongAs(prefix("avgOf",ScalarTypeProbe.REQUIRED_LONG_PROPERTY));
    }

    public ScalarTypeProbeRequest avgRequiredLongAs(String retName){
        super.avg(retName, ScalarTypeProbe.REQUIRED_LONG_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest standardDeviationRequiredLong(){
        return standardDeviationRequiredLongAs(prefix("standardDeviationOf",ScalarTypeProbe.REQUIRED_LONG_PROPERTY));
    }

    public ScalarTypeProbeRequest standardDeviationRequiredLongAs(String retName){
        super.standardDeviation(retName, ScalarTypeProbe.REQUIRED_LONG_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationRequiredLong(){
        return squareRootOfPopulationStandardDeviationRequiredLongAs(prefix("squareRootOfPopulationStandardDeviationOf",ScalarTypeProbe.REQUIRED_LONG_PROPERTY));
    }

    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationRequiredLongAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, ScalarTypeProbe.REQUIRED_LONG_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sampleVarianceRequiredLong(){
        return sampleVarianceRequiredLongAs(prefix("sampleVarianceOf",ScalarTypeProbe.REQUIRED_LONG_PROPERTY));
    }

    public ScalarTypeProbeRequest sampleVarianceRequiredLongAs(String retName){
        super.sampleVariance(retName, ScalarTypeProbe.REQUIRED_LONG_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest samplePopulationVarianceRequiredLong(){
        return samplePopulationVarianceRequiredLongAs(prefix("samplePopulationVarianceOf",ScalarTypeProbe.REQUIRED_LONG_PROPERTY));
    }

    public ScalarTypeProbeRequest samplePopulationVarianceRequiredLongAs(String retName){
        super.samplePopulationVariance(retName, ScalarTypeProbe.REQUIRED_LONG_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest minOptionalLong(){
        return minOptionalLongAs(prefix("minOf",ScalarTypeProbe.OPTIONAL_LONG_PROPERTY));
    }

    public ScalarTypeProbeRequest minOptionalLongAs(String retName){
        super.min(retName, ScalarTypeProbe.OPTIONAL_LONG_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest maxOptionalLong(){
        return maxOptionalLongAs(prefix("maxOf",ScalarTypeProbe.OPTIONAL_LONG_PROPERTY));
    }

    public ScalarTypeProbeRequest maxOptionalLongAs(String retName){
        super.max(retName, ScalarTypeProbe.OPTIONAL_LONG_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sumOptionalLong(){
        return sumOptionalLongAs(prefix("sumOf",ScalarTypeProbe.OPTIONAL_LONG_PROPERTY));
    }

    public ScalarTypeProbeRequest sumOptionalLongAs(String retName){
        super.sum(retName, ScalarTypeProbe.OPTIONAL_LONG_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest avgOptionalLong(){
        return avgOptionalLongAs(prefix("avgOf",ScalarTypeProbe.OPTIONAL_LONG_PROPERTY));
    }

    public ScalarTypeProbeRequest avgOptionalLongAs(String retName){
        super.avg(retName, ScalarTypeProbe.OPTIONAL_LONG_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest standardDeviationOptionalLong(){
        return standardDeviationOptionalLongAs(prefix("standardDeviationOf",ScalarTypeProbe.OPTIONAL_LONG_PROPERTY));
    }

    public ScalarTypeProbeRequest standardDeviationOptionalLongAs(String retName){
        super.standardDeviation(retName, ScalarTypeProbe.OPTIONAL_LONG_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationOptionalLong(){
        return squareRootOfPopulationStandardDeviationOptionalLongAs(prefix("squareRootOfPopulationStandardDeviationOf",ScalarTypeProbe.OPTIONAL_LONG_PROPERTY));
    }

    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationOptionalLongAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, ScalarTypeProbe.OPTIONAL_LONG_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sampleVarianceOptionalLong(){
        return sampleVarianceOptionalLongAs(prefix("sampleVarianceOf",ScalarTypeProbe.OPTIONAL_LONG_PROPERTY));
    }

    public ScalarTypeProbeRequest sampleVarianceOptionalLongAs(String retName){
        super.sampleVariance(retName, ScalarTypeProbe.OPTIONAL_LONG_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest samplePopulationVarianceOptionalLong(){
        return samplePopulationVarianceOptionalLongAs(prefix("samplePopulationVarianceOf",ScalarTypeProbe.OPTIONAL_LONG_PROPERTY));
    }

    public ScalarTypeProbeRequest samplePopulationVarianceOptionalLongAs(String retName){
        super.samplePopulationVariance(retName, ScalarTypeProbe.OPTIONAL_LONG_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest minRequiredDecimal(){
        return minRequiredDecimalAs(prefix("minOf",ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY));
    }

    public ScalarTypeProbeRequest minRequiredDecimalAs(String retName){
        super.min(retName, ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest maxRequiredDecimal(){
        return maxRequiredDecimalAs(prefix("maxOf",ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY));
    }

    public ScalarTypeProbeRequest maxRequiredDecimalAs(String retName){
        super.max(retName, ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sumRequiredDecimal(){
        return sumRequiredDecimalAs(prefix("sumOf",ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY));
    }

    public ScalarTypeProbeRequest sumRequiredDecimalAs(String retName){
        super.sum(retName, ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest avgRequiredDecimal(){
        return avgRequiredDecimalAs(prefix("avgOf",ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY));
    }

    public ScalarTypeProbeRequest avgRequiredDecimalAs(String retName){
        super.avg(retName, ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest standardDeviationRequiredDecimal(){
        return standardDeviationRequiredDecimalAs(prefix("standardDeviationOf",ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY));
    }

    public ScalarTypeProbeRequest standardDeviationRequiredDecimalAs(String retName){
        super.standardDeviation(retName, ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationRequiredDecimal(){
        return squareRootOfPopulationStandardDeviationRequiredDecimalAs(prefix("squareRootOfPopulationStandardDeviationOf",ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY));
    }

    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationRequiredDecimalAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sampleVarianceRequiredDecimal(){
        return sampleVarianceRequiredDecimalAs(prefix("sampleVarianceOf",ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY));
    }

    public ScalarTypeProbeRequest sampleVarianceRequiredDecimalAs(String retName){
        super.sampleVariance(retName, ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest samplePopulationVarianceRequiredDecimal(){
        return samplePopulationVarianceRequiredDecimalAs(prefix("samplePopulationVarianceOf",ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY));
    }

    public ScalarTypeProbeRequest samplePopulationVarianceRequiredDecimalAs(String retName){
        super.samplePopulationVariance(retName, ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest minOptionalDecimal(){
        return minOptionalDecimalAs(prefix("minOf",ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY));
    }

    public ScalarTypeProbeRequest minOptionalDecimalAs(String retName){
        super.min(retName, ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest maxOptionalDecimal(){
        return maxOptionalDecimalAs(prefix("maxOf",ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY));
    }

    public ScalarTypeProbeRequest maxOptionalDecimalAs(String retName){
        super.max(retName, ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sumOptionalDecimal(){
        return sumOptionalDecimalAs(prefix("sumOf",ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY));
    }

    public ScalarTypeProbeRequest sumOptionalDecimalAs(String retName){
        super.sum(retName, ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest avgOptionalDecimal(){
        return avgOptionalDecimalAs(prefix("avgOf",ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY));
    }

    public ScalarTypeProbeRequest avgOptionalDecimalAs(String retName){
        super.avg(retName, ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest standardDeviationOptionalDecimal(){
        return standardDeviationOptionalDecimalAs(prefix("standardDeviationOf",ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY));
    }

    public ScalarTypeProbeRequest standardDeviationOptionalDecimalAs(String retName){
        super.standardDeviation(retName, ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationOptionalDecimal(){
        return squareRootOfPopulationStandardDeviationOptionalDecimalAs(prefix("squareRootOfPopulationStandardDeviationOf",ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY));
    }

    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationOptionalDecimalAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sampleVarianceOptionalDecimal(){
        return sampleVarianceOptionalDecimalAs(prefix("sampleVarianceOf",ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY));
    }

    public ScalarTypeProbeRequest sampleVarianceOptionalDecimalAs(String retName){
        super.sampleVariance(retName, ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest samplePopulationVarianceOptionalDecimal(){
        return samplePopulationVarianceOptionalDecimalAs(prefix("samplePopulationVarianceOf",ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY));
    }

    public ScalarTypeProbeRequest samplePopulationVarianceOptionalDecimalAs(String retName){
        super.samplePopulationVariance(retName, ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest minRequiredFloat(){
        return minRequiredFloatAs(prefix("minOf",ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY));
    }

    public ScalarTypeProbeRequest minRequiredFloatAs(String retName){
        super.min(retName, ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest maxRequiredFloat(){
        return maxRequiredFloatAs(prefix("maxOf",ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY));
    }

    public ScalarTypeProbeRequest maxRequiredFloatAs(String retName){
        super.max(retName, ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sumRequiredFloat(){
        return sumRequiredFloatAs(prefix("sumOf",ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY));
    }

    public ScalarTypeProbeRequest sumRequiredFloatAs(String retName){
        super.sum(retName, ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest avgRequiredFloat(){
        return avgRequiredFloatAs(prefix("avgOf",ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY));
    }

    public ScalarTypeProbeRequest avgRequiredFloatAs(String retName){
        super.avg(retName, ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest standardDeviationRequiredFloat(){
        return standardDeviationRequiredFloatAs(prefix("standardDeviationOf",ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY));
    }

    public ScalarTypeProbeRequest standardDeviationRequiredFloatAs(String retName){
        super.standardDeviation(retName, ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationRequiredFloat(){
        return squareRootOfPopulationStandardDeviationRequiredFloatAs(prefix("squareRootOfPopulationStandardDeviationOf",ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY));
    }

    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationRequiredFloatAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sampleVarianceRequiredFloat(){
        return sampleVarianceRequiredFloatAs(prefix("sampleVarianceOf",ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY));
    }

    public ScalarTypeProbeRequest sampleVarianceRequiredFloatAs(String retName){
        super.sampleVariance(retName, ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest samplePopulationVarianceRequiredFloat(){
        return samplePopulationVarianceRequiredFloatAs(prefix("samplePopulationVarianceOf",ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY));
    }

    public ScalarTypeProbeRequest samplePopulationVarianceRequiredFloatAs(String retName){
        super.samplePopulationVariance(retName, ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest minOptionalFloat(){
        return minOptionalFloatAs(prefix("minOf",ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY));
    }

    public ScalarTypeProbeRequest minOptionalFloatAs(String retName){
        super.min(retName, ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest maxOptionalFloat(){
        return maxOptionalFloatAs(prefix("maxOf",ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY));
    }

    public ScalarTypeProbeRequest maxOptionalFloatAs(String retName){
        super.max(retName, ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sumOptionalFloat(){
        return sumOptionalFloatAs(prefix("sumOf",ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY));
    }

    public ScalarTypeProbeRequest sumOptionalFloatAs(String retName){
        super.sum(retName, ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest avgOptionalFloat(){
        return avgOptionalFloatAs(prefix("avgOf",ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY));
    }

    public ScalarTypeProbeRequest avgOptionalFloatAs(String retName){
        super.avg(retName, ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest standardDeviationOptionalFloat(){
        return standardDeviationOptionalFloatAs(prefix("standardDeviationOf",ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY));
    }

    public ScalarTypeProbeRequest standardDeviationOptionalFloatAs(String retName){
        super.standardDeviation(retName, ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationOptionalFloat(){
        return squareRootOfPopulationStandardDeviationOptionalFloatAs(prefix("squareRootOfPopulationStandardDeviationOf",ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY));
    }

    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationOptionalFloatAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sampleVarianceOptionalFloat(){
        return sampleVarianceOptionalFloatAs(prefix("sampleVarianceOf",ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY));
    }

    public ScalarTypeProbeRequest sampleVarianceOptionalFloatAs(String retName){
        super.sampleVariance(retName, ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest samplePopulationVarianceOptionalFloat(){
        return samplePopulationVarianceOptionalFloatAs(prefix("samplePopulationVarianceOf",ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY));
    }

    public ScalarTypeProbeRequest samplePopulationVarianceOptionalFloatAs(String retName){
        super.samplePopulationVariance(retName, ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest minRequiredDouble(){
        return minRequiredDoubleAs(prefix("minOf",ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY));
    }

    public ScalarTypeProbeRequest minRequiredDoubleAs(String retName){
        super.min(retName, ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest maxRequiredDouble(){
        return maxRequiredDoubleAs(prefix("maxOf",ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY));
    }

    public ScalarTypeProbeRequest maxRequiredDoubleAs(String retName){
        super.max(retName, ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sumRequiredDouble(){
        return sumRequiredDoubleAs(prefix("sumOf",ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY));
    }

    public ScalarTypeProbeRequest sumRequiredDoubleAs(String retName){
        super.sum(retName, ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest avgRequiredDouble(){
        return avgRequiredDoubleAs(prefix("avgOf",ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY));
    }

    public ScalarTypeProbeRequest avgRequiredDoubleAs(String retName){
        super.avg(retName, ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest standardDeviationRequiredDouble(){
        return standardDeviationRequiredDoubleAs(prefix("standardDeviationOf",ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY));
    }

    public ScalarTypeProbeRequest standardDeviationRequiredDoubleAs(String retName){
        super.standardDeviation(retName, ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationRequiredDouble(){
        return squareRootOfPopulationStandardDeviationRequiredDoubleAs(prefix("squareRootOfPopulationStandardDeviationOf",ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY));
    }

    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationRequiredDoubleAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sampleVarianceRequiredDouble(){
        return sampleVarianceRequiredDoubleAs(prefix("sampleVarianceOf",ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY));
    }

    public ScalarTypeProbeRequest sampleVarianceRequiredDoubleAs(String retName){
        super.sampleVariance(retName, ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest samplePopulationVarianceRequiredDouble(){
        return samplePopulationVarianceRequiredDoubleAs(prefix("samplePopulationVarianceOf",ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY));
    }

    public ScalarTypeProbeRequest samplePopulationVarianceRequiredDoubleAs(String retName){
        super.samplePopulationVariance(retName, ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest minOptionalDouble(){
        return minOptionalDoubleAs(prefix("minOf",ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY));
    }

    public ScalarTypeProbeRequest minOptionalDoubleAs(String retName){
        super.min(retName, ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest maxOptionalDouble(){
        return maxOptionalDoubleAs(prefix("maxOf",ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY));
    }

    public ScalarTypeProbeRequest maxOptionalDoubleAs(String retName){
        super.max(retName, ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sumOptionalDouble(){
        return sumOptionalDoubleAs(prefix("sumOf",ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY));
    }

    public ScalarTypeProbeRequest sumOptionalDoubleAs(String retName){
        super.sum(retName, ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest avgOptionalDouble(){
        return avgOptionalDoubleAs(prefix("avgOf",ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY));
    }

    public ScalarTypeProbeRequest avgOptionalDoubleAs(String retName){
        super.avg(retName, ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest standardDeviationOptionalDouble(){
        return standardDeviationOptionalDoubleAs(prefix("standardDeviationOf",ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY));
    }

    public ScalarTypeProbeRequest standardDeviationOptionalDoubleAs(String retName){
        super.standardDeviation(retName, ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationOptionalDouble(){
        return squareRootOfPopulationStandardDeviationOptionalDoubleAs(prefix("squareRootOfPopulationStandardDeviationOf",ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY));
    }

    public ScalarTypeProbeRequest squareRootOfPopulationStandardDeviationOptionalDoubleAs(String retName){
        super.squareRootOfPopulationStandardDeviation(retName, ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest sampleVarianceOptionalDouble(){
        return sampleVarianceOptionalDoubleAs(prefix("sampleVarianceOf",ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY));
    }

    public ScalarTypeProbeRequest sampleVarianceOptionalDoubleAs(String retName){
        super.sampleVariance(retName, ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest samplePopulationVarianceOptionalDouble(){
        return samplePopulationVarianceOptionalDoubleAs(prefix("samplePopulationVarianceOf",ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY));
    }

    public ScalarTypeProbeRequest samplePopulationVarianceOptionalDoubleAs(String retName){
        super.samplePopulationVariance(retName, ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY);
        return this;
    }
    public ScalarTypeProbeRequest<T> groupByTypePlatformWithDetails(){
       return groupByTypePlatformWithDetails(Q.typePlatforms().unlimited());
    }

    public ScalarTypeProbeRequest<T> groupByTypePlatformWithDetails(TypePlatformRequest subRequest){
       aggregate(ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, subRequest);
       return this;
    }


























    public ScalarTypeProbeRequest<T> groupById(){
       groupBy(ScalarTypeProbe.ID_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByIdAs(String retName){
       groupBy(retName, ScalarTypeProbe.ID_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByIdWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.ID_PROPERTY, function);
       return this;
    }
    public ScalarTypeProbeRequest<T> groupByTypePlatformWith(TypePlatformRequest subRequest){
       groupBy(ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, subRequest);
       return this;
    }
    public ScalarTypeProbeRequest<T> groupByTypePlatform(){
       groupBy(ScalarTypeProbe.TYPE_PLATFORM_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByTypePlatformAs(String retName){
       groupBy(retName, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByTypePlatformWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredString(){
       groupBy(ScalarTypeProbe.REQUIRED_STRING_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredStringAs(String retName){
       groupBy(retName, ScalarTypeProbe.REQUIRED_STRING_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredStringWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.REQUIRED_STRING_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalString(){
       groupBy(ScalarTypeProbe.OPTIONAL_STRING_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalStringAs(String retName){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_STRING_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalStringWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_STRING_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByLargeText(){
       groupBy(ScalarTypeProbe.LARGE_TEXT_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByLargeTextAs(String retName){
       groupBy(retName, ScalarTypeProbe.LARGE_TEXT_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByLargeTextWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.LARGE_TEXT_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredBoolean(){
       groupBy(ScalarTypeProbe.REQUIRED_BOOLEAN_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredBooleanAs(String retName){
       groupBy(retName, ScalarTypeProbe.REQUIRED_BOOLEAN_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredBooleanWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.REQUIRED_BOOLEAN_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalBoolean(){
       groupBy(ScalarTypeProbe.OPTIONAL_BOOLEAN_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalBooleanAs(String retName){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_BOOLEAN_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalBooleanWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_BOOLEAN_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredInteger(){
       groupBy(ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredIntegerAs(String retName){
       groupBy(retName, ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredIntegerWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalInteger(){
       groupBy(ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalIntegerAs(String retName){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalIntegerWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredLong(){
       groupBy(ScalarTypeProbe.REQUIRED_LONG_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredLongAs(String retName){
       groupBy(retName, ScalarTypeProbe.REQUIRED_LONG_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredLongWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.REQUIRED_LONG_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalLong(){
       groupBy(ScalarTypeProbe.OPTIONAL_LONG_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalLongAs(String retName){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_LONG_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalLongWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_LONG_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredDecimal(){
       groupBy(ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredDecimalAs(String retName){
       groupBy(retName, ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredDecimalWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalDecimal(){
       groupBy(ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalDecimalAs(String retName){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalDecimalWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredFloat(){
       groupBy(ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredFloatAs(String retName){
       groupBy(retName, ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredFloatWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalFloat(){
       groupBy(ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalFloatAs(String retName){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalFloatWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredDouble(){
       groupBy(ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredDoubleAs(String retName){
       groupBy(retName, ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredDoubleWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalDouble(){
       groupBy(ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalDoubleAs(String retName){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalDoubleWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredDate(){
       groupBy(ScalarTypeProbe.REQUIRED_DATE_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredDateAs(String retName){
       groupBy(retName, ScalarTypeProbe.REQUIRED_DATE_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredDateWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.REQUIRED_DATE_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalDate(){
       groupBy(ScalarTypeProbe.OPTIONAL_DATE_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalDateAs(String retName){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_DATE_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalDateWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_DATE_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredTime(){
       groupBy(ScalarTypeProbe.REQUIRED_TIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredTimeAs(String retName){
       groupBy(retName, ScalarTypeProbe.REQUIRED_TIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.REQUIRED_TIME_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalTime(){
       groupBy(ScalarTypeProbe.OPTIONAL_TIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalTimeAs(String retName){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_TIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_TIME_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredDatetime(){
       groupBy(ScalarTypeProbe.REQUIRED_DATETIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredDatetimeAs(String retName){
       groupBy(retName, ScalarTypeProbe.REQUIRED_DATETIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByRequiredDatetimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.REQUIRED_DATETIME_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalDatetime(){
       groupBy(ScalarTypeProbe.OPTIONAL_DATETIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalDatetimeAs(String retName){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_DATETIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByOptionalDatetimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.OPTIONAL_DATETIME_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByCreateTime(){
       groupBy(ScalarTypeProbe.CREATE_TIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByCreateTimeAs(String retName){
       groupBy(retName, ScalarTypeProbe.CREATE_TIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByCreateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.CREATE_TIME_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByUpdateTime(){
       groupBy(ScalarTypeProbe.UPDATE_TIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByUpdateTimeAs(String retName){
       groupBy(retName, ScalarTypeProbe.UPDATE_TIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByUpdateTimeWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.UPDATE_TIME_PROPERTY, function);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByVersion(){
       groupBy(ScalarTypeProbe.VERSION_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByVersionAs(String retName){
       groupBy(retName, ScalarTypeProbe.VERSION_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> groupByVersionWithFunction(String retName, AggrFunction function){
       groupBy(retName, ScalarTypeProbe.VERSION_PROPERTY, function);
       return this;
    }



    public ScalarTypeProbeRequest<T> orderByIdAscending(){
       addOrderByAscending(ScalarTypeProbe.ID_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByIdDescending(){
       addOrderByDescending(ScalarTypeProbe.ID_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByTypePlatformAscending(){
       addOrderByAscending(ScalarTypeProbe.TYPE_PLATFORM_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByTypePlatformDescending(){
       addOrderByDescending(ScalarTypeProbe.TYPE_PLATFORM_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredStringAscending(){
       addOrderByAscending(ScalarTypeProbe.REQUIRED_STRING_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredStringDescending(){
       addOrderByDescending(ScalarTypeProbe.REQUIRED_STRING_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> orderByRequiredStringAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(ScalarTypeProbe.REQUIRED_STRING_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredStringDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(ScalarTypeProbe.REQUIRED_STRING_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> orderByOptionalStringAscending(){
       addOrderByAscending(ScalarTypeProbe.OPTIONAL_STRING_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalStringDescending(){
       addOrderByDescending(ScalarTypeProbe.OPTIONAL_STRING_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> orderByOptionalStringAscendingUsingGBK(){
       addOrderByAscendingUsingGBK(ScalarTypeProbe.OPTIONAL_STRING_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalStringDescendingUsingGBK(){
       addOrderByDescendingUsingGBK(ScalarTypeProbe.OPTIONAL_STRING_PROPERTY);
       return this;
    }
    public ScalarTypeProbeRequest<T> orderByLargeTextAscending(){
       addOrderByAscending(ScalarTypeProbe.LARGE_TEXT_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByLargeTextDescending(){
       addOrderByDescending(ScalarTypeProbe.LARGE_TEXT_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredBooleanAscending(){
       addOrderByAscending(ScalarTypeProbe.REQUIRED_BOOLEAN_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredBooleanDescending(){
       addOrderByDescending(ScalarTypeProbe.REQUIRED_BOOLEAN_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalBooleanAscending(){
       addOrderByAscending(ScalarTypeProbe.OPTIONAL_BOOLEAN_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalBooleanDescending(){
       addOrderByDescending(ScalarTypeProbe.OPTIONAL_BOOLEAN_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredIntegerAscending(){
       addOrderByAscending(ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredIntegerDescending(){
       addOrderByDescending(ScalarTypeProbe.REQUIRED_INTEGER_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalIntegerAscending(){
       addOrderByAscending(ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalIntegerDescending(){
       addOrderByDescending(ScalarTypeProbe.OPTIONAL_INTEGER_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredLongAscending(){
       addOrderByAscending(ScalarTypeProbe.REQUIRED_LONG_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredLongDescending(){
       addOrderByDescending(ScalarTypeProbe.REQUIRED_LONG_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalLongAscending(){
       addOrderByAscending(ScalarTypeProbe.OPTIONAL_LONG_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalLongDescending(){
       addOrderByDescending(ScalarTypeProbe.OPTIONAL_LONG_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredDecimalAscending(){
       addOrderByAscending(ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredDecimalDescending(){
       addOrderByDescending(ScalarTypeProbe.REQUIRED_DECIMAL_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalDecimalAscending(){
       addOrderByAscending(ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalDecimalDescending(){
       addOrderByDescending(ScalarTypeProbe.OPTIONAL_DECIMAL_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredFloatAscending(){
       addOrderByAscending(ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredFloatDescending(){
       addOrderByDescending(ScalarTypeProbe.REQUIRED_FLOAT_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalFloatAscending(){
       addOrderByAscending(ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalFloatDescending(){
       addOrderByDescending(ScalarTypeProbe.OPTIONAL_FLOAT_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredDoubleAscending(){
       addOrderByAscending(ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredDoubleDescending(){
       addOrderByDescending(ScalarTypeProbe.REQUIRED_DOUBLE_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalDoubleAscending(){
       addOrderByAscending(ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalDoubleDescending(){
       addOrderByDescending(ScalarTypeProbe.OPTIONAL_DOUBLE_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredDateAscending(){
       addOrderByAscending(ScalarTypeProbe.REQUIRED_DATE_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredDateDescending(){
       addOrderByDescending(ScalarTypeProbe.REQUIRED_DATE_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalDateAscending(){
       addOrderByAscending(ScalarTypeProbe.OPTIONAL_DATE_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalDateDescending(){
       addOrderByDescending(ScalarTypeProbe.OPTIONAL_DATE_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredTimeAscending(){
       addOrderByAscending(ScalarTypeProbe.REQUIRED_TIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredTimeDescending(){
       addOrderByDescending(ScalarTypeProbe.REQUIRED_TIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalTimeAscending(){
       addOrderByAscending(ScalarTypeProbe.OPTIONAL_TIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalTimeDescending(){
       addOrderByDescending(ScalarTypeProbe.OPTIONAL_TIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredDatetimeAscending(){
       addOrderByAscending(ScalarTypeProbe.REQUIRED_DATETIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByRequiredDatetimeDescending(){
       addOrderByDescending(ScalarTypeProbe.REQUIRED_DATETIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalDatetimeAscending(){
       addOrderByAscending(ScalarTypeProbe.OPTIONAL_DATETIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByOptionalDatetimeDescending(){
       addOrderByDescending(ScalarTypeProbe.OPTIONAL_DATETIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByCreateTimeAscending(){
       addOrderByAscending(ScalarTypeProbe.CREATE_TIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByCreateTimeDescending(){
       addOrderByDescending(ScalarTypeProbe.CREATE_TIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByUpdateTimeAscending(){
       addOrderByAscending(ScalarTypeProbe.UPDATE_TIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByUpdateTimeDescending(){
       addOrderByDescending(ScalarTypeProbe.UPDATE_TIME_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByVersionAscending(){
       addOrderByAscending(ScalarTypeProbe.VERSION_PROPERTY);
       return this;
    }

    public ScalarTypeProbeRequest<T> orderByVersionDescending(){
       addOrderByDescending(ScalarTypeProbe.VERSION_PROPERTY);
       return this;
    }


    public TypePlatformRequest rollUpToTypePlatform(){
       TypePlatformRequest typePlatform = Q.typePlatforms().unlimited();
       this.withTypePlatformMatching(typePlatform)
           .groupByTypePlatformWith(typePlatform);
       return typePlatform;
    }


























   public ScalarTypeProbeRequest<T> facetByTypePlatformAs(String facetName, TypePlatformRequest typePlatform){
       return facetByTypePlatformAs(facetName, typePlatform, true);
   }

   public ScalarTypeProbeRequest<T> facetByTypePlatformAs(String facetName, TypePlatformRequest typePlatform, boolean includeAllFacets){
       addFacet(facetName, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY, typePlatform, includeAllFacets,
           io.teaql.core.QueryOperation.nativeMethod(io.teaql.core.QueryOperation.Kind.FACET, "facetByTypePlatformAs", facetName, ScalarTypeProbe.TYPE_PLATFORM_PROPERTY));
       return this;
   }


    /**
     * get topN records
     * @param topN  records number
     */
    public ScalarTypeProbeRequest<T> top(int topN) {
        super.top(topN);
        return this;
    }

    /** Cross-runtime bounded-query alias. */
    public ScalarTypeProbeRequest<T> limit(int limit) {
        return top(limit);
    }

    /**
     * get records from offset(inclusive) to offset+size(exclusive)
     * @param offset record offset
     * @param size records number
     */
    public ScalarTypeProbeRequest<T> offset(int offset, int size) {
        super.offset(offset, size);
        return this;
    }

    /**
     * retrieve all records
     */
    public ScalarTypeProbeRequest<T> unlimited() {
        super.unlimited();
        return this;
    }

    /**
     * get records of one page
     * @param pageNumber page number(1-based)
     * @param pageSize page size
     */
    public ScalarTypeProbeRequest<T> page(int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        return offset(offset, pageSize);
   }

    /**
     * get records of one page, default page size is 10
     * @param pageNumber page number(1-based)
     */
    public ScalarTypeProbeRequest<T> page(int pageNumber) {
        return page(pageNumber, 10);
   }
}