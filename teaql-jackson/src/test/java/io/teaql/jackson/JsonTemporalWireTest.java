package io.teaql.jackson;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.BaseEntity;
import io.teaql.core.FieldLayout;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.time.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/** Calendar Date and wall Time are not instants; Timestamp is UTC milliseconds. */
@RunWith(Parameterized.class)
public class JsonTemporalWireTest {
    @Parameterized.Parameters(name="{0}") public static Collection<Object[]> profiles() {
        return List.of(new Object[]{JsonFieldNamingProfile.CAMEL_CASE},new Object[]{JsonFieldNamingProfile.SNAKE_CASE},new Object[]{JsonFieldNamingProfile.PASCAL_CASE});
    }
    static final Map<String,Class<?>> TYPES=Map.of("id",Long.class,"version",Long.class,"date_value",LocalDate.class,"time_value",LocalTime.class,"timestamp_value",Instant.class);
    static FieldLayout layout(Class<?> type,JsonFieldNamingProfile profile){
        var indexes=new LinkedHashMap<String,Integer>();var mappings=new LinkedHashMap<String,List<String>>();
        for(var field:TYPES.keySet().stream().sorted().toList()){indexes.put(field,indexes.size());mappings.put(field,List.of(field,field));}
        return FieldLayout.installGenerated(FieldLayout.generated(type,"temporal-wire-v1",indexes,mappings,Set.of("child_row","child_list"),profile));
    }
    public abstract static class Row extends BaseEntity {
        final Map<String,Object> values=new HashMap<>();
        @Override public String typeName(){return "TemporalWireRow";}
        @Override public void __internalSet(String field,Object value){
            if(field.equals("id")||field.equals("version"))super.__internalSet(field,value);else{markPropertyLoaded(field);values.put(field,value);}
        }
        @Override public Object __internalGet(String field){return field.equals("id")||field.equals("version")?super.__internalGet(field):values.get(field);}
    }
    public static final class CamelRow extends Row {static final FieldLayout LAYOUT=layout(CamelRow.class,JsonFieldNamingProfile.CAMEL_CASE);}
    public static final class SnakeRow extends Row {static final FieldLayout LAYOUT=layout(SnakeRow.class,JsonFieldNamingProfile.SNAKE_CASE);}
    public static final class PascalRow extends Row {static final FieldLayout LAYOUT=layout(PascalRow.class,JsonFieldNamingProfile.PASCAL_CASE);}
    private final JsonFieldNamingProfile profile;
    private final ObjectMapper mapper;
    private final Class<? extends Row> type;
    public JsonTemporalWireTest(JsonFieldNamingProfile profile){
        this.profile=profile;
        if(profile==JsonFieldNamingProfile.CAMEL_CASE){assertNotNull(CamelRow.LAYOUT);type=CamelRow.class;}
        else if(profile==JsonFieldNamingProfile.SNAKE_CASE){assertNotNull(SnakeRow.LAYOUT);type=SnakeRow.class;}
        else{assertNotNull(PascalRow.LAYOUT);type=PascalRow.class;}
        var descriptor=new EntityDescriptor();descriptor.setType("TemporalWireRow");descriptor.setTargetType(type);
        descriptor.setEntitySupplier(()->profile==JsonFieldNamingProfile.CAMEL_CASE?new CamelRow():profile==JsonFieldNamingProfile.SNAKE_CASE?new SnakeRow():new PascalRow());
        TYPES.forEach(descriptor::addSimpleProperty);var metadata=new SimpleEntityMetaFactory();metadata.register(descriptor);
        descriptor.addObjectProperty(metadata,"child_row","TemporalWireRow","child_list",type);
        var context=new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).build()) {
            @Override public LocalDateTime businessTime(){throw new AssertionError("Passive JSON must not consult Business Clock");}
        };
        mapper=new ObjectMapper().registerModule(new TeaQLModule(context));
    }
    Row read(String field,String value)throws Exception{return mapper.readValue("{\""+profile.render(field)+"\":"+value+"}",type);}
    void invalid(String field,String value){
        var error=assertThrows(JsonMappingException.class,()->read(field,value));
        assertEquals(1,error.getPath().size());assertEquals(profile.render(field),error.getPath().get(0).getFieldName());
        assertFalse(error.getMessage(),error.getMessage().contains("PRIVATE_TEMPORAL_CANARY"));
    }
    @Test public void calendarDateRoundTripsWithoutJavaTimeModule()throws Exception{
        var row=read("date_value","\"2024-02-29\"");assertEquals(LocalDate.of(2024,2,29),row.__internalGet("date_value"));
        var json=mapper.readTree(mapper.writeValueAsString(row));assertEquals("2024-02-29",json.path(profile.render("date_value")).textValue());
        assertEquals(row.__internalGet("date_value"),mapper.treeToValue(json,type).__internalGet("date_value"));
    }
    @Test public void invalidCalendarDatesDoNotNormalize() {
        for(String value:List.of("2023-02-29","2024-02-30","2024-13-01","2024-00-01","PRIVATE_TEMPORAL_CANARY"))invalid("date_value","\""+value+"\"");
    }
    @Test public void dateRejectsNumericArrayAndTimestampCoercion(){
        for(String value:List.of("0","[2024,2,29]","true","\"2024-02-29T00:00:00Z\"","\"2024-02-29+08:00\""))invalid("date_value",value);
    }
    @Test public void calendarDateIgnoresTheMappersZone()throws Exception{
        String input="{\""+profile.render("date_value")+"\":\"2011-12-30\"}";
        for(String zone:List.of("UTC","Pacific/Apia","America/New_York")){
            var configured=mapper.copy().setTimeZone(TimeZone.getTimeZone(zone));
            assertEquals(LocalDate.of(2011,12,30),configured.readValue(input,type).__internalGet("date_value"));
        }
    }
    @Test public void wallTimePreservesItsNativeNanoseconds()throws Exception{
        for(String value:List.of("00:00:00","12:30:00","23:59:59.123456789")){
            var row=read("time_value","\""+value+"\"");assertEquals(LocalTime.parse(value),row.__internalGet("time_value"));
            var json=mapper.readTree(mapper.writeValueAsString(row));assertEquals(value,json.path(profile.render("time_value")).textValue());
            assertEquals(LocalTime.parse(value),mapper.treeToValue(json,type).__internalGet("time_value"));
        }
    }
    @Test public void wallTimeRejectsRolloverZoneAndExcessPrecision(){
        for(String value:List.of("24:00:00","23:59:60","23:59:59.1234567891","12:30:00Z","12:30:00+08:00","2024-02-29T12:30:00","PRIVATE_TEMPORAL_CANARY"))invalid("time_value","\""+value+"\"");
    }
    @Test public void wallTimeRejectsArrayAndNumberCoercion(){for(String value:List.of("0","[12,30]","false"))invalid("time_value",value);}
    @Test public void timestampNormalizesOffsetsToUtcMilliseconds()throws Exception{
        var expected=Instant.parse("2024-02-29T12:34:56.123Z");
        for(String value:List.of("2024-02-29T12:34:56.123Z","2024-02-29T20:34:56.123+08:00","2024-02-29T07:34:56.123-05:00")){
            var row=read("timestamp_value","\""+value+"\"");assertEquals(expected,row.__internalGet("timestamp_value"));
            var json=mapper.readTree(mapper.writeValueAsString(row));assertEquals(expected.toString(),json.path(profile.render("timestamp_value")).textValue());
        }
    }
    @Test public void timestampBeforeEpochAndEpochRoundTripExactly()throws Exception{
        for(String value:List.of("1960-01-02T03:04:05.006Z","1970-01-01T00:00:00Z")){
            var row=read("timestamp_value","\""+value+"\"");var json=mapper.readTree(mapper.writeValueAsString(row));
            assertEquals(Instant.parse(value),mapper.treeToValue(json,type).__internalGet("timestamp_value"));
        }
    }
    @Test public void timestampRequiresOffsetAndRejectsFinerThanMilliseconds(){
        for(String value:List.of("2024-02-29T12:34:56","2024-02-29","2024-02-29T12:34:56.123456Z","2016-12-31T23:59:60Z","PRIVATE_TEMPORAL_CANARY"))invalid("timestamp_value","\""+value+"\"");
    }
    @Test public void timestampRejectsNumericAndArrayCoercion(){for(String value:List.of("0","1709210096123","true","[2024,2,29,12,34,56]"))invalid("timestamp_value",value);}
    @Test public void timestampOutputRejectsPrecisionLossAndKeepsTheDiagnosticPath()throws Exception{
        var row=read("id","1");row.__internalSet("timestamp_value",Instant.parse("2024-02-29T12:34:56.123456Z"));var state=row.__internalLoadState();
        var error=assertThrows(JsonMappingException.class,()->mapper.writeValueAsString(row));assertEquals(profile.render("timestamp_value"),error.getPath().get(0).getFieldName());
        assertSame(state,row.__internalLoadState());assertFalse(row.__internalHasMutationLedger());
    }
    @Test public void nullAndMissingTemporalValuesStayDistinct()throws Exception{
        var row=read("date_value","null");assertTrue(row.isPropertyLoaded("date_value"));assertNull(row.__internalGet("date_value"));assertFalse(row.isPropertyLoaded("time_value"));
        var json=mapper.readTree(mapper.writeValueAsString(row));assertTrue(json.path(profile.render("date_value")).isNull());assertFalse(json.has(profile.render("time_value")));
        assertFalse(row.__internalHasMutationLedger());assertTrue(row.getUpdatedProperties().isEmpty());
    }
    @Test public void temporalPolicyDoesNotInstallGlobalJavaTimeSerializers(){
        assertThrows(JsonMappingException.class,()->mapper.writeValueAsString(LocalDate.of(2024,2,29)));
    }
    @Test public void nestedTemporalErrorsRetainParentsAndArrayIndices(){
        var error=assertThrows(JsonMappingException.class,()->read("child_list","[{\""+profile.render("date_value")+"\":\"PRIVATE_TEMPORAL_CANARY\"}]"));
        assertEquals(3,error.getPath().size());assertEquals(profile.render("child_list"),error.getPath().get(0).getFieldName());assertEquals(0,error.getPath().get(1).getIndex());
        assertEquals(profile.render("date_value"),error.getPath().get(2).getFieldName());assertFalse(error.getMessage().contains("PRIVATE_TEMPORAL_CANARY"));
    }
    @Test public void registeredJavaTimeModuleCannotTurnModelDatesIntoArrays()throws Exception{
        var configured=mapper.copy().findAndRegisterModules();
        var row=configured.readValue("{\""+profile.render("date_value")+"\":\"2024-02-29\"}",type);
        var json=configured.readTree(configured.writeValueAsString(row));assertEquals("2024-02-29",json.path(profile.render("date_value")).textValue());
        assertTrue(configured.isEnabled(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
        assertTrue(configured.readTree(configured.writeValueAsString(LocalDate.of(2024,2,29))).isArray());
    }
    @Test public void registeredJavaTimeModuleCannotTurnModelTimesIntoArrays()throws Exception{
        var configured=mapper.copy().findAndRegisterModules();
        var row=configured.readValue("{\""+profile.render("time_value")+"\":\"23:59:59.123456789\"}",type);
        var json=configured.readTree(configured.writeValueAsString(row));assertEquals("23:59:59.123456789",json.path(profile.render("time_value")).textValue());
    }
    @Test public void registeredJavaTimeModuleCannotTurnModelInstantsIntoEpochNumbers()throws Exception{
        var configured=mapper.copy().findAndRegisterModules();
        var row=configured.readValue("{\""+profile.render("timestamp_value")+"\":\"2024-02-29T12:34:56.123Z\"}",type);
        var json=configured.readTree(configured.writeValueAsString(row));assertEquals("2024-02-29T12:34:56.123Z",json.path(profile.render("timestamp_value")).textValue());
    }
}
