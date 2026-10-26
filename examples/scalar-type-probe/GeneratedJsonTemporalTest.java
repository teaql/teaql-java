package com.example.scalartypeconformanceservice;

import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.jackson.TeaQLModule;
import io.teaql.runtime.TeaQLRuntime;
import java.nio.file.Path;
import java.time.*;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Generated Date/Time APIs; no redefinition of the model's LocalDateTime fields. */
public class GeneratedJsonTemporalTest {
    @Test public void generatedDateAndWallTimeStayExactAcrossJsonAndSave()throws Exception{
        var profile=JsonFieldNamingProfile.fromModelValue(System.getenv("TEAQL_JSON_PROFILE"));
        int round=Integer.parseInt(System.getenv("TEAQL_NAME_ROUND"));
        var expected=Path.of(System.getProperty("teaql.expected.runtime.root")).toRealPath();
        for(var type:List.of(TeaQLRuntime.class,TeaQLModule.class))assertTrue(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().startsWith(expected));
        var f=new GeneratedCanonicalNameTest.Fixture("jdbc:sqlite:"+Path.of(System.getenv("TEAQL_NAME_DB")).toAbsolutePath(),round);
        var root=f.root("JSON temporal "+profile+" round "+round);
        var row=f.probe(root,"temporal "+round);
        var date=LocalDate.of(1960,2,29);var time=LocalTime.of(23,59,59,123456789);
        row.updateRequiredDate(date);row.updateOptionalDate(LocalDate.of(2024,2,29));
        row.updateRequiredTime(LocalTime.MIDNIGHT);row.updateOptionalTime(time);
        row.auditAs("Persist generated calendar and wall-clock values").save(f.context);
        var loaded=f.load(row.getId());
        assertEquals(date,E.scalarTypeProbe(loaded).getRequiredDate().eval());assertEquals(time,E.scalarTypeProbe(loaded).getOptionalTime().eval());
        var nativeLocalDateTime=E.scalarTypeProbe(loaded).getRequiredDatetime().eval();
        var mapper=new ObjectMapper().findAndRegisterModules().registerModule(new TeaQLModule(f.context));
        long before=f.data.calls;
        var json=mapper.readTree(mapper.writeValueAsString(loaded));
        assertEquals("1960-02-29",json.path(profile.render("required_date")).textValue());
        assertEquals("2024-02-29",json.path(profile.render("optional_date")).textValue());
        assertEquals("00:00:00",json.path(profile.render("required_time")).textValue());
        assertEquals("23:59:59.123456789",json.path(profile.render("optional_time")).textValue());
        var restored=mapper.treeToValue(json,ScalarTypeProbe.class);
        assertEquals(date,E.scalarTypeProbe(restored).getRequiredDate().eval());assertEquals(time,E.scalarTypeProbe(restored).getOptionalTime().eval());
        assertEquals(nativeLocalDateTime,E.scalarTypeProbe(restored).getRequiredDatetime().eval());
        assertFalse(restored.__internalHasMutationLedger());assertEquals(before,f.data.calls);
        int invalid=0;
        for(String[] test:List.of(new String[]{"required_date","\"2023-02-29\""},new String[]{"required_date","[2024,2,29]"},
                new String[]{"required_time","\"23:59:00+08:00\""},new String[]{"required_time","\"23:59:59.1234567891\""},
                new String[]{"required_time","0"},new String[]{"required_date","\"PRIVATE_TEMPORAL_CANARY\""})){
            var error=assertThrows(JsonMappingException.class,()->mapper.readValue("{\""+profile.render(test[0])+"\":"+test[1]+"}",ScalarTypeProbe.class));
            assertEquals(profile.render(test[0]),error.getPath().get(0).getFieldName());assertFalse(error.getMessage().contains("PRIVATE_TEMPORAL_CANARY"));invalid++;
        }
        assertEquals(before,f.data.calls);
        restored.updateRequiredString("temporal "+round+" updated");restored.updateRequiredTime(time);
        restored.updateOptionalDate(null);restored.updateOptionalTime(null);
        restored.auditAs("Save a trusted edit after temporal JSON reconstruction").save(f.context);
        var finalRow=f.load(row.getId());assertEquals(Long.valueOf(2),finalRow.getVersion());
        assertEquals(date,E.scalarTypeProbe(finalRow).getRequiredDate().eval());assertEquals(time,E.scalarTypeProbe(finalRow).getRequiredTime().eval());
        assertNull(E.scalarTypeProbe(finalRow).getOptionalDate().eval());assertNull(E.scalarTypeProbe(finalRow).getOptionalTime().eval());
        assertEquals(nativeLocalDateTime,E.scalarTypeProbe(finalRow).getRequiredDatetime().eval());
        var result=new LinkedHashMap<String,Object>();result.put("passed",true);result.put("profile",profile.modelValue());result.put("round",round);
        result.put("id",row.getId());result.put("rootId",root.getId());result.put("finalVersion",2);result.put("invalidInputCases",invalid);
        for(String key:List.of("calendarDate","wallTimeNanoseconds","loadedNull","passiveCodec","typedQAndE","decodedSave","localDateTimeUnchanged"))result.put(key,true);
        result.put("generatedInstantAccepted",false);result.put("localDateTimeWireAccepted",false);result.put("businessClockChanged",false);
        System.out.println("JAVA_JSON_TEMPORAL="+new ObjectMapper().writeValueAsString(result));
    }
}
