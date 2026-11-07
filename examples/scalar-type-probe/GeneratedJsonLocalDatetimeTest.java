package com.example.scalartypeconformanceservice;

import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.value.TeaQLNotLoadedException;
import io.teaql.jackson.TeaQLModule;
import io.teaql.runtime.TeaQLRuntime;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;
import org.junit.Test;
import static org.junit.Assert.*;

/** Generated model wall-time JSON, not a claim of nanosecond SQL storage or Instant generation. */
public class GeneratedJsonLocalDatetimeTest {
    @Test public void generatedWallDatetimeSurvivesJsonMutationAndSqliteReload() throws Exception {
        var profile = JsonFieldNamingProfile.fromModelValue(System.getenv("TEAQL_JSON_PROFILE"));
        int round = Integer.parseInt(System.getenv("TEAQL_NAME_ROUND"));
        var expected = Path.of(System.getProperty("teaql.expected.runtime.root")).toRealPath();
        for (var type : List.of(TeaQLRuntime.class, TeaQLModule.class))
            assertTrue(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().startsWith(expected));
        var f = new GeneratedCanonicalNameTest.Fixture(
                "jdbc:sqlite:" + Path.of(System.getenv("TEAQL_NAME_DB")).toAbsolutePath(), round);
        var root = f.root("JSON LocalDateTime " + profile + " round " + round);
        var row = f.probe(root, "local datetime " + round);
        // A nonexistent New York wall time must not be normalized into an instant.
        var initial = LocalDateTime.of(2024, 3, 10, 2, 30, 0, 123000000);
        row.updateRequiredDatetime(initial);
        row.auditAs("Persist a zone-free generated datetime").save(f.context);
        var loaded = f.load(row.getId());
        assertEquals(initial, E.scalarTypeProbe(loaded).getRequiredDatetime().eval());
        var mapper = new ObjectMapper().registerModule(new TeaQLModule(f.context));
        long before = f.data.calls;
        int auditBefore = f.audits.size(), policiesBefore = f.policies.size();
        var field = profile.render("required_datetime");
        var json = mapper.readTree(mapper.writeValueAsString(loaded));
        assertEquals("2024-03-10T02:30:00.123", json.path(field).textValue());
        var restored = mapper.treeToValue(json, ScalarTypeProbe.class);
        assertEquals(initial, E.scalarTypeProbe(restored).getRequiredDatetime().eval());
        assertFalse(restored.__internalHasMutationLedger());
        for (var zone : List.of("UTC", "America/New_York", "Asia/Shanghai")) {
            var zoned = mapper.copy().setTimeZone(TimeZone.getTimeZone(zone));
            assertEquals(initial, E.scalarTypeProbe(zoned.treeToValue(json, ScalarTypeProbe.class)).getRequiredDatetime().eval());
            assertEquals(json, zoned.readTree(zoned.writeValueAsString(loaded)));
        }
        var explicitNull = mapper.readValue("{\"" + field + "\":null}", ScalarTypeProbe.class);
        assertNull(E.scalarTypeProbe(explicitNull).getRequiredDatetime().eval());
        var absent = mapper.readValue("{}", ScalarTypeProbe.class);
        assertThrows(TeaQLNotLoadedException.class, () -> E.scalarTypeProbe(absent).getRequiredDatetime().eval());
        assertFalse(explicitNull.__internalHasMutationLedger());
        assertFalse(absent.__internalHasMutationLedger());
        int invalid = 0;
        for (var value : List.of("\"2024-03-10T02:30:00Z\"", "\"2024-03-10T02:30:00+08:00\"",
                "\"2023-02-29T12:00:00\"", "[2024,3,10,2,30]", "0", "true", "\"PRIVATE_DATETIME_CANARY\"")) {
            var error = assertThrows(JsonMappingException.class,
                    () -> mapper.readValue("{\"" + field + "\":" + value + "}", ScalarTypeProbe.class));
            assertEquals(field, error.getPath().get(0).getFieldName());
            assertFalse(error.getMessage().contains("PRIVATE_DATETIME_CANARY"));
            invalid++;
        }
        assertEquals(before, f.data.calls);
        assertEquals(auditBefore, f.audits.size());
        assertEquals(policiesBefore, f.policies.size());
        var updated = LocalDateTime.of(1960, 2, 29, 23, 59, 59, 456000000);
        restored.updateRequiredDatetime(updated);
        restored.auditAs("Save a trusted zone-free datetime edit after JSON reconstruction").save(f.context);
        var finalRow = f.load(row.getId());
        assertEquals(Long.valueOf(2), finalRow.getVersion());
        assertEquals(updated, E.scalarTypeProbe(finalRow).getRequiredDatetime().eval());
        assertEquals("1960-02-29T23:59:59.456",
                mapper.readTree(mapper.writeValueAsString(finalRow)).path(field).textValue());
        var result = new LinkedHashMap<String, Object>();
        result.put("passed", true); result.put("profile", profile.modelValue()); result.put("round", round);
        result.put("id", row.getId()); result.put("rootId", root.getId()); result.put("finalVersion", 2);
        result.put("invalidInputCases", invalid);
        for (var key : List.of("zoneFreeIso", "timezoneIndependent", "loadedNull", "notLoaded", "passiveCodec",
                "typedQAndE", "decodedSave", "sqliteMillisReadback")) result.put(key, true);
        result.put("generatedInstantAccepted", false); result.put("sqlNanosecondsAccepted", false);
        result.put("businessClockChanged", false);
        System.out.println("JAVA_JSON_LOCAL_DATETIME=" + new ObjectMapper().writeValueAsString(result));
    }
}
