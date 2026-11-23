package io.teaql.jackson;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.BaseEntity;
import io.teaql.core.FieldLayout;
import io.teaql.core.SmartList;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.math.BigDecimal;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/** Exact logical values are a model boundary, not global Jackson coercion settings. */
@RunWith(Parameterized.class)
public class JsonScalarWireTest {
    @Parameterized.Parameters(name="{0}") public static Collection<Object[]> profiles() {
        return List.of(new Object[]{JsonFieldNamingProfile.CAMEL_CASE}, new Object[]{JsonFieldNamingProfile.SNAKE_CASE}, new Object[]{JsonFieldNamingProfile.PASCAL_CASE});
    }
    static final Map<String,Class<?>> TYPES = Map.of("id",Long.class,"version",Long.class,"long_value",Long.class,
            "decimal_value",BigDecimal.class,"integer_value",Integer.class,"float_value",Float.class,
            "double_value",Double.class,"boolean_value",Boolean.class,"string_value",String.class);
    static FieldLayout layout(Class<?> type, JsonFieldNamingProfile profile) {
        var indexes=new LinkedHashMap<String,Integer>(); var mappings=new LinkedHashMap<String,List<String>>();
        for (var field : TYPES.keySet().stream().sorted().toList()) { indexes.put(field,indexes.size()); mappings.put(field,List.of(field,field)); }
        return FieldLayout.installGenerated(FieldLayout.generated(type,"scalar-wire-v1",indexes,mappings,Set.of("child_row","child_list"),profile));
    }
    public abstract static class Row extends BaseEntity {
        final Map<String,Object> values=new HashMap<>();
        @Override public String typeName() { return "ScalarWireRow"; }
        @Override public void __internalSet(String field,Object value) {
            if (field.equals("id") || field.equals("version")) super.__internalSet(field,value);
            else { markPropertyLoaded(field); values.put(field,value); }
        }
        @Override public Object __internalGet(String field) {
            return field.equals("id") || field.equals("version") ? super.__internalGet(field) : values.get(field);
        }
    }
    public static final class CamelRow extends Row { static final FieldLayout LAYOUT=layout(CamelRow.class,JsonFieldNamingProfile.CAMEL_CASE); }
    public static final class SnakeRow extends Row { static final FieldLayout LAYOUT=layout(SnakeRow.class,JsonFieldNamingProfile.SNAKE_CASE); }
    public static final class PascalRow extends Row { static final FieldLayout LAYOUT=layout(PascalRow.class,JsonFieldNamingProfile.PASCAL_CASE); }
    private final JsonFieldNamingProfile profile;
    private final ObjectMapper mapper;
    private final Class<? extends Row> type;
    public JsonScalarWireTest(JsonFieldNamingProfile profile) {
        this.profile=profile;
        if(profile==JsonFieldNamingProfile.CAMEL_CASE){assertNotNull(CamelRow.LAYOUT);type=CamelRow.class;}
        else if(profile==JsonFieldNamingProfile.SNAKE_CASE){assertNotNull(SnakeRow.LAYOUT);type=SnakeRow.class;}
        else {assertNotNull(PascalRow.LAYOUT);type=PascalRow.class;}
        var descriptor=new EntityDescriptor();descriptor.setType("ScalarWireRow");descriptor.setTargetType(type);
        descriptor.setEntitySupplier(()->profile==JsonFieldNamingProfile.CAMEL_CASE?new CamelRow():profile==JsonFieldNamingProfile.SNAKE_CASE?new SnakeRow():new PascalRow());
        TYPES.forEach(descriptor::addSimpleProperty);
        var metadata=new SimpleEntityMetaFactory();metadata.register(descriptor);
        descriptor.addObjectProperty(metadata,"child_row","ScalarWireRow","child_list",type);
        mapper=new ObjectMapper().registerModule(new TeaQLModule(new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).build())));
    }
    Row read(String field,String value) throws Exception { return mapper.readValue("{\""+profile.render(field)+"\":"+value+"}",type); }
    void invalid(String field,String value) {
        var error=assertThrows(JsonMappingException.class,()->read(field,value));
        assertEquals(1,error.getPath().size());assertEquals(profile.render(field),error.getPath().get(0).getFieldName());
        assertFalse(error.getMessage(),error.getMessage().contains("PRIVATE_SCALAR_CANARY"));
    }
    @Test public void exactLongIdentityAndVersionAreStringsWithoutChangingOrdinaryJacksonLongs() throws Exception {
        var row=read("id","\"9223372036854775807\"");row.__internalSet("version",Long.MIN_VALUE);row.__internalSet("long_value",9007199254740993L);
        var json=mapper.readTree(mapper.writeValueAsString(row));
        assertTrue(json.path(profile.render("id")).isTextual());assertEquals("9223372036854775807",json.path(profile.render("id")).textValue());
        assertEquals(Long.toString(Long.MIN_VALUE),json.path(profile.render("version")).textValue());
        assertEquals("9007199254740993",json.path(profile.render("long_value")).textValue());
        assertEquals("7",mapper.writeValueAsString(7L));
    }
    @Test public void longStringEndpointsAndSafeNumericInputRemainExact() throws Exception {
        for (long value : new long[]{Long.MIN_VALUE,Long.MAX_VALUE,9007199254740993L,0})
            assertEquals(value,read("long_value","\""+value+"\"").__internalGet("long_value"));
        assertEquals(9007199254740991L,read("long_value","9007199254740991").__internalGet("long_value"));
        assertEquals(-9007199254740991L,read("long_value","-9007199254740991").__internalGet("long_value"));
    }
    @Test public void unsafeLongNumbersAndWrongTypesRejectRatherThanCoerce() {
        for(String value:List.of("9007199254740992","-9007199254740992","1.5","true","\"1.5\"","\" 1\"","\"+1\"","\"01\"","\"9223372036854775808\"","\"-9223372036854775809\"","\"PRIVATE_SCALAR_CANARY\"")) invalid("long_value",value);
    }
    @Test public void identityAndVersionFollowTheSameExactInputBoundary() {
        for(String field:List.of("id","version"))for(String value:List.of("9007199254740992","1.5","true","\"9223372036854775808\""))invalid(field,value);
    }
    @Test public void decimalValuesAndScaleRoundTripThroughStrings() throws Exception {
        for(String value:List.of("3.50","0.1","123456789012345678901234567890.123456789","1E+3","1E-1000")){
            var row=read("decimal_value","\""+value+"\"");var expected=new BigDecimal(value);
            assertEquals(expected,row.__internalGet("decimal_value"));
            var json=mapper.readTree(mapper.writeValueAsString(row));assertTrue(json.path(profile.render("decimal_value")).isTextual());
            var restored=mapper.treeToValue(json,type);assertEquals(expected,restored.__internalGet("decimal_value"));
        }
    }
    @Test public void decimalNumericTokensAndInvalidTextRejectRatherThanLosePrecision() {
        for(String value:List.of("0.1","1","true","\"NaN\"","\"Infinity\"","\" 3.50\"","\"PRIVATE_SCALAR_CANARY\""))invalid("decimal_value",value);
    }
    @Test public void integersStayNumbersAtBothSigned32BitEndpoints() throws Exception {
        for(int value:new int[]{Integer.MIN_VALUE,Integer.MAX_VALUE,0}){
            var row=read("integer_value",Integer.toString(value));assertEquals(value,row.__internalGet("integer_value"));
            var json=mapper.readTree(mapper.writeValueAsString(row));assertTrue(json.path(profile.render("integer_value")).isIntegralNumber());assertEquals(value,json.path(profile.render("integer_value")).intValue());
        }
    }
    @Test public void integerFractionsRangesAndCoercionsAreRejected() {
        for(String value:List.of("2147483648","-2147483649","1.5","1.0","true","\"1\"","\"PRIVATE_SCALAR_CANARY\""))invalid("integer_value",value);
    }
    @Test public void floatFiniteValuesAndNegativeZeroKeepTheirNativeBits() throws Exception {
        assertEquals(Float.floatToRawIntBits(-0.0f),Float.floatToRawIntBits((Float)read("float_value","-0.0").__internalGet("float_value")));
        assertEquals(1.5f,read("float_value","1.5").__internalGet("float_value"));
        var row=read("float_value","1.5");assertTrue(mapper.readTree(mapper.writeValueAsString(row)).path(profile.render("float_value")).isNumber());
    }
    @Test public void floatOverflowUnderflowAndTextAreRejected() {
        for(String value:List.of("3.5e38","1e-50","true","\"1.5\"","\"NaN\"","\"PRIVATE_SCALAR_CANARY\""))invalid("float_value",value);
    }
    @Test public void doubleFiniteValuesAndNegativeZeroKeepTheirNativeBits() throws Exception {
        assertEquals(Double.doubleToRawLongBits(-0.0d),Double.doubleToRawLongBits((Double)read("double_value","-0.0").__internalGet("double_value")));
        assertEquals(2.25d,read("double_value","2.25").__internalGet("double_value"));
    }
    @Test public void doubleOverflowUnderflowAndTextAreRejected() {
        for(String value:List.of("1e400","1e-400","true","\"2.25\"","\"NaN\"","\"PRIVATE_SCALAR_CANARY\""))invalid("double_value",value);
    }
    @Test public void nonfiniteNativeValuesCannotBecomeJsonStringsOrNull() throws Exception {
        for(var value:List.of(Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY)){
            String field=value instanceof Float?"float_value":"double_value";var row=read("string_value","\"safe\"");row.__internalSet(field,value);
            var error=assertThrows(JsonMappingException.class,()->mapper.writeValueAsString(row));
            assertEquals(profile.render(field),error.getPath().get(0).getFieldName());
        }
    }
    @Test public void booleanAndStringTypesDoNotBorrowJacksonCoercions() {
        for(String value:List.of("1","0","\"true\"","\"PRIVATE_SCALAR_CANARY\""))invalid("boolean_value",value);
        for(String value:List.of("1","true","{}","[]"))invalid("string_value",value);
    }
    @Test public void nullAndNotLoadedRemainDistinctAndPassive() throws Exception {
        var row=read("decimal_value","null");assertTrue(row.isPropertyLoaded("decimal_value"));assertNull(row.__internalGet("decimal_value"));
        assertFalse(row.isPropertyLoaded("long_value"));assertFalse(row.__internalHasMutationLedger());
        var json=mapper.readTree(mapper.writeValueAsString(row));assertTrue(json.path(profile.render("decimal_value")).isNull());assertFalse(json.has(profile.render("long_value")));
        var string=read("string_value","\"\"");assertEquals("",string.__internalGet("string_value"));
        assertEquals(false,read("boolean_value","false").__internalGet("boolean_value"));
    }
    @Test public void nestedScalarInputDiagnosticsRetainRelationAndArrayPaths() {
        var direct=assertThrows(JsonMappingException.class,()->read("child_row","{\""+profile.render("long_value")+"\":9007199254740992}"));
        assertEquals(List.of(profile.render("child_row"),profile.render("long_value")),direct.getPath().stream().map(JsonMappingException.Reference::getFieldName).toList());
        var array=assertThrows(JsonMappingException.class,()->read("child_list","[{\""+profile.render("long_value")+"\":9007199254740992}]"));
        assertEquals(3,array.getPath().size());assertEquals(profile.render("child_list"),array.getPath().get(0).getFieldName());
        assertEquals(0,array.getPath().get(1).getIndex());assertEquals(profile.render("long_value"),array.getPath().get(2).getFieldName());
    }
    @Test public void nestedNonfiniteOutputRetainsPathAndDoesNotPoisonTheNextWrite() throws Exception {
        var child=read("float_value","1.5");child.__internalSet("float_value",Float.NaN);
        var parent=read("string_value","\"safe\"");parent.__internalSet("child_list",SmartList.takeOwnership(List.of(child)));
        var state=parent.__internalLoadState();
        var error=assertThrows(JsonMappingException.class,()->mapper.writeValueAsString(parent));
        assertEquals(3,error.getPath().size());assertEquals(profile.render("child_list"),error.getPath().get(0).getFieldName());
        assertEquals(0,error.getPath().get(1).getIndex());assertEquals(profile.render("float_value"),error.getPath().get(2).getFieldName());
        assertSame(state,parent.__internalLoadState());assertFalse(parent.__internalHasMutationLedger());assertFalse(child.__internalHasMutationLedger());
        child.__internalSet("float_value",1.5f);
        var json=mapper.readTree(mapper.writeValueAsString(parent));assertEquals(1.5,json.path(profile.render("child_list")).get(0).path(profile.render("float_value")).doubleValue(),0);
    }
}
