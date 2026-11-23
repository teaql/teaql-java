package com.example.scalartypeconformanceservice;

import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.jackson.TeaQLModule;
import io.teaql.runtime.TeaQLRuntime;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;

/** Generated typed mutation/Q/E and passive numeric JSON use the same native values. */
public class GeneratedJsonNumericTest {
    static void values(ScalarTypeProbe row, String name) {
        var e=E.scalarTypeProbe(row);
        assertEquals(name,e.getRequiredString().eval());assertEquals("",e.getOptionalString().eval());
        assertEquals("Unicode 雪",e.getLargeText().eval());
        assertEquals(Boolean.TRUE,e.isRequiredBoolean().eval());assertEquals(Boolean.FALSE,e.isOptionalBoolean().eval());
        assertEquals(Integer.valueOf(Integer.MAX_VALUE),e.getRequiredInteger().eval());assertEquals(Integer.valueOf(Integer.MIN_VALUE),e.getOptionalInteger().eval());
        assertEquals(Long.valueOf(9007199254740993L),e.getRequiredLong().eval());assertEquals(Long.valueOf(Long.MIN_VALUE),e.getOptionalLong().eval());
        assertEquals(new BigDecimal("123456789012.3456789"),e.getRequiredDecimal().eval());assertEquals(new BigDecimal("3.50"),e.getOptionalDecimal().eval());
        assertEquals(Float.valueOf(1.5f),e.getRequiredFloat().eval());assertEquals(Float.valueOf(-3.5f),e.getOptionalFloat().eval());
        assertEquals(Double.valueOf(2.25d),e.getRequiredDouble().eval());assertEquals(Double.valueOf(-0.125d),e.getOptionalDouble().eval());
    }
    @Test public void generatedExactNumericsPersistAndRoundTripInTheSelectedProfile() throws Exception {
        var profile=JsonFieldNamingProfile.fromModelValue(System.getenv("TEAQL_JSON_PROFILE"));
        int round=Integer.parseInt(System.getenv("TEAQL_NAME_ROUND"));
        var expected=Path.of(System.getProperty("teaql.expected.runtime.root")).toRealPath();
        for(var type:List.of(TeaQLRuntime.class,TeaQLModule.class))assertTrue(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().startsWith(expected));
        var f=new GeneratedCanonicalNameTest.Fixture("jdbc:sqlite:"+Path.of(System.getenv("TEAQL_NAME_DB")).toAbsolutePath(),round);
        var root=f.root("JSON numeric "+profile+" round "+round);
        var row=f.probe(root,"numeric "+round);
        row.updateOptionalString("");row.updateLargeText("Unicode 雪");row.updateOptionalBoolean(false);
        row.updateRequiredInteger(Integer.MAX_VALUE);row.updateOptionalInteger(Integer.MIN_VALUE);
        row.updateRequiredLong(9007199254740993L);row.updateOptionalLong(Long.MIN_VALUE);
        row.updateRequiredDecimal(new BigDecimal("123456789012.3456789"));row.updateOptionalDecimal(new BigDecimal("3.50"));
        row.updateRequiredFloat(1.5f);row.updateOptionalFloat(-3.5f);row.updateRequiredDouble(2.25d);row.updateOptionalDouble(-0.125d);
        row.auditAs("Persist exact generated numeric values").save(f.context);
        var loaded=f.load(row.getId());values(loaded,"numeric "+round);
        var mapper=new ObjectMapper().findAndRegisterModules().registerModule(new TeaQLModule(f.context));
        long before=f.data.calls;
        var json=mapper.readTree(mapper.writeValueAsString(loaded));
        assertEquals(row.getId().toString(),json.path(profile.render("id")).textValue());assertEquals("1",json.path(profile.render("version")).textValue());
        assertEquals("9007199254740993",json.path(profile.render("required_long")).textValue());
        assertEquals(Long.toString(Long.MIN_VALUE),json.path(profile.render("optional_long")).textValue());
        assertEquals("123456789012.3456789",json.path(profile.render("required_decimal")).textValue());
        assertEquals("3.50",json.path(profile.render("optional_decimal")).textValue());
        assertTrue(json.path(profile.render("required_integer")).isIntegralNumber());assertTrue(json.path(profile.render("required_float")).isNumber());
        var restored=mapper.treeToValue(json,ScalarTypeProbe.class);values(restored,"numeric "+round);
        assertFalse(restored.__internalHasMutationLedger());assertEquals(before,f.data.calls);
        int invalid=0;
        for(String[] test:List.of(new String[]{"required_long","9007199254740993"},new String[]{"required_integer","1.5"},
                new String[]{"required_decimal","0.1"},new String[]{"required_float","3.5e38"},new String[]{"required_double","1e-400"},
                new String[]{"required_boolean","\"PRIVATE_SCALAR_CANARY\""})){
            var error=assertThrows(JsonMappingException.class,()->mapper.readValue("{\""+profile.render(test[0])+"\":"+test[1]+"}",ScalarTypeProbe.class));
            assertEquals(profile.render(test[0]),error.getPath().get(0).getFieldName());assertFalse(error.getMessage().contains("PRIVATE_SCALAR_CANARY"));invalid++;
        }
        assertEquals(before,f.data.calls);
        var bad=f.load(row.getId());bad.updateRequiredDouble(Double.NaN);
        long beforeBadCodec=f.data.calls;
        assertThrows(JsonMappingException.class,()->mapper.writeValueAsString(bad));assertEquals(beforeBadCodec,f.data.calls);
        restored.updateRequiredString("numeric "+round+" updated");
        restored.auditAs("Save a trusted edit after exact numeric JSON reconstruction").save(f.context);
        var finalRow=f.load(row.getId());values(finalRow,"numeric "+round+" updated");assertEquals(Long.valueOf(2),finalRow.getVersion());
        var result=new LinkedHashMap<String,Object>();result.put("passed",true);result.put("profile",profile.modelValue());result.put("round",round);result.put("id",row.getId());
        result.put("rootId",root.getId());result.put("finalVersion",2);result.put("invalidInputCases",invalid);
        for(String key:List.of("exactLong","exactDecimalScale","integerBounds","nativeFloatDouble","passiveCodec","typedQAndE","decodedSave","nonfiniteOutputRejected"))result.put(key,true);
        System.out.println("JAVA_JSON_NUMERIC="+new ObjectMapper().writeValueAsString(result));
    }
}
