package com.example.scalartypeconformanceservice;

import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.value.TeaQLNotLoadedException;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.TeaQLRuntime;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Raw SQL below is deliberate legacy corruption, never normal application writes. */
public class GeneratedJdbcExactReadTest {
    private record Vector(String name, String column, String expression, Object restored) {}
    private static final List<Vector> VECTORS = List.of(
            new Vector("integer-fraction", "required_integer", "1.9", 7),
            new Vector("integer-upper-overflow", "required_integer", "2147483648", 7),
            new Vector("integer-lower-overflow", "required_integer", "-2147483649", 7),
            new Vector("long-fraction", "required_long", "1.9", 8L),
            new Vector("long-rounded-overflow", "required_long", "9223372036854775808.0", 8L),
            new Vector("float-overflow", "required_float", "1e40", 1.25f),
            new Vector("double-infinity", "required_double", "1e999", 2.5d),
            new Vector("decimal-private-text", "required_decimal", "'PRIVATE_JDBC_VALUE_901'", "12.50"));

    private static ScalarTypeProbe read(GeneratedCanonicalNameTest.Fixture f, long id, boolean streaming) {
        var query = Q.scalarTypeProbes().withIdIs(id).limit(1)
                .comment("Read the exact numeric probe through generated Q")
                .purpose("Reject corrupt persisted numerics without returning a misleading entity");
        if (!streaming) return query.executeForOne(f.context);
        try (var rows = query.executeForStream(f.context)) { return rows.findFirst().orElseThrow(); }
    }

    @Test public void generatedQueriesRejectCorruptionAndPreserveUnselectedFields() throws Exception {
        var expected = Path.of(System.getProperty("teaql.expected.runtime.root")).toRealPath();
        for (var type : List.of(TeaQLRuntime.class, JdbcSqlExecutor.class))
            assertTrue(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().startsWith(expected));
        int round = Integer.parseInt(System.getenv("TEAQL_NAME_ROUND"));
        var f = new GeneratedCanonicalNameTest.Fixture("jdbc:sqlite:" + Path.of(System.getenv("TEAQL_NAME_DB")).toAbsolutePath(), round);
        var root = f.root("JDBC exact root " + round);
        var results = new ArrayList<String>();
        for (boolean streaming : List.of(false, true)) for (var v : VECTORS) {
            String name = (streaming ? "stream/" : "one/") + v.name;
            var row = f.probe(root, "JDBC exact " + round + " " + name);
            row.auditAs("Create a generated numeric corruption control").save(f.context);
            int audits = f.audits.size(), policies = f.policies.size();
            try (var c = DriverManager.getConnection(f.data.url);
                 var s = c.prepareStatement("UPDATE scalar_type_probe_data SET " + v.column + "=" + v.expression + " WHERE id=?")) {
                s.setLong(1, row.getId()); assertEquals(1, s.executeUpdate());
            }
            // Establish the physical row independently of generated hydration.
            try (var c = DriverManager.getConnection(f.data.url);
                 var s = c.prepareStatement("SELECT " + v.column + ",version FROM scalar_type_probe_data WHERE id=?")) {
                s.setLong(1, row.getId());
                try (var rows = s.executeQuery()) { assertTrue(rows.next()); assertNotNull(rows.getObject(1)); assertEquals(1, rows.getLong(2)); assertFalse(rows.next()); }
            }
            var failure = assertThrows(RuntimeException.class, () -> read(f, row.getId(), streaming));
            boolean located = false;
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                assertFalse("private stored value escaped in an error", cause.toString().contains("PRIVATE_JDBC_VALUE_901"));
                if (cause instanceof IllegalArgumentException && cause.getMessage().contains("JDBC column")) located = true;
            }
            assertTrue("missing precise typed JDBC failure for " + name, located);
            var partial = Q.scalarTypeProbesWithMinimalFields().withIdIs(row.getId()).selectRequiredString().limit(1)
                    .comment("Read only an unaffected projection")
                    .purpose("Do not clear a row or silently load an unselected corrupt field").executeForOne(f.context);
            assertEquals(row.getRequiredString(), E.scalarTypeProbe(partial).getRequiredString().eval());
            assertThrows(TeaQLNotLoadedException.class, () -> E.scalarTypeProbe(partial).getRequiredInteger().eval());
            assertEquals(audits, f.audits.size()); assertEquals(policies, f.policies.size());
            // Repair only the explicit fault injection. Normal writes above and
            // below continue to use generated audited mutation APIs.
            try (var c = DriverManager.getConnection(f.data.url);
                 var s = c.prepareStatement("UPDATE scalar_type_probe_data SET " + v.column + "=? WHERE id=?")) {
                s.setObject(1, v.restored); s.setLong(2, row.getId()); assertEquals(1, s.executeUpdate());
            }
            var loaded = read(f, row.getId(), streaming); var e = E.scalarTypeProbe(loaded);
            assertEquals(Integer.valueOf(7), e.getRequiredInteger().eval());
            assertEquals(Long.valueOf(8), e.getRequiredLong().eval());
            assertEquals(new BigDecimal("12.50"), e.getRequiredDecimal().eval());
            assertEquals(Float.valueOf(1.25f), e.getRequiredFloat().eval());
            assertEquals(Double.valueOf(2.5d), e.getRequiredDouble().eval());
            assertEquals(Long.valueOf(1), loaded.getVersion());
            assertEquals(audits, f.audits.size()); assertEquals(policies, f.policies.size());
            results.add(name);
        }
        var good = f.probe(root, "JDBC exact bounds " + round);
        good.updateRequiredInteger(Integer.MIN_VALUE); good.updateOptionalInteger(Integer.MAX_VALUE);
        good.updateRequiredLong(9007199254740993L); good.updateOptionalLong(Long.MIN_VALUE);
        good.auditAs("Persist exact native numeric boundary controls").save(f.context);
        for (boolean streaming : List.of(false, true)) {
            var loaded = read(f, good.getId(), streaming); var e = E.scalarTypeProbe(loaded);
            assertEquals(Integer.valueOf(Integer.MIN_VALUE), e.getRequiredInteger().eval());
            assertEquals(Integer.valueOf(Integer.MAX_VALUE), e.getOptionalInteger().eval());
            assertEquals(Long.valueOf(9007199254740993L), e.getRequiredLong().eval());
            assertEquals(Long.valueOf(Long.MIN_VALUE), e.getOptionalLong().eval());
            assertEquals(new BigDecimal("12.50"), e.getRequiredDecimal().eval());
            assertEquals(Long.valueOf(1), loaded.getVersion());
        }
        good.updateRequiredString("JDBC exact bounds updated " + round);
        good.auditAs("Save a real change after the numeric read controls").save(f.context);
        assertEquals(Long.valueOf(2), f.load(good.getId()).getVersion());
        var result = new LinkedHashMap<String,Object>();
        result.put("passed", true); result.put("round", round); result.put("cases", results);
        result.put("rootId", root.getId()); result.put("boundaryId", good.getId()); result.put("boundaryVersion", 2);
        for (String key : List.of("generatedQ", "loadedE", "unselectedNotLoaded", "physicalCorruptionConfirmed", "queryProducesNoMutation", "privateValuesExcluded", "sameAdapterRecovers", "exactNativeBounds")) result.put(key, true);
        System.out.println("JAVA_JDBC_EXACT_READ=" + new ObjectMapper().writeValueAsString(result));
    }
}
