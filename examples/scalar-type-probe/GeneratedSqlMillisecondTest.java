package com.example.scalartypeconformanceservice;

import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.value.TeaQLNotLoadedException;
import io.teaql.core.sql.portable.SqlTemporalPrecisionException;
import io.teaql.jackson.TeaQLModule;
import io.teaql.runtime.TeaQLRuntime;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.Test;
import static org.junit.Assert.*;

/** New canonical SQL round trips and explicit rejection of incompatible legacy rows. */
public class GeneratedSqlMillisecondTest {
    static final List<String> ZONES = List.of("UTC", "America/New_York", "Asia/Shanghai");
    static final List<LocalDateTime> INPUTS = List.of(
            LocalDateTime.of(2024, 3, 10, 2, 30, 0, 123000000),
            LocalDateTime.of(2024, 3, 10, 2, 30, 0, 123456789),
            LocalDateTime.of(2024, 3, 10, 2, 30, 59, 999999999),
            LocalDateTime.of(1960, 2, 29, 23, 59, 59, 987654321),
            LocalDateTime.of(1969, 12, 31, 23, 59, 59, 999999999),
            LocalDateTime.of(2032, 2, 29, 12, 30));

    static int round() { return Integer.parseInt(System.getenv("TEAQL_NAME_ROUND")); }
    static GeneratedCanonicalNameTest.Fixture fixture(String name) throws Exception {
        var expected = Path.of(System.getProperty("teaql.expected.runtime.root")).toRealPath();
        for (var type : List.of(TeaQLRuntime.class, TeaQLModule.class))
            assertTrue(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().startsWith(expected));
        return new GeneratedCanonicalNameTest.Fixture(
                "jdbc:sqlite:" + Path.of(System.getenv("TEAQL_NAME_DB") + ".sql-millis-" + name).toAbsolutePath(), round());
    }
    static ScalarTypeProbe equal(GeneratedCanonicalNameTest.Fixture f, long id, LocalDateTime value) {
        return Q.scalarTypeProbes().withIdIs(id).withRequiredDatetimeIs(value).limit(1)
                .comment("Find a probe by its canonical SQL datetime")
                .purpose("Verify generated datetime predicate precision").executeForOne(f.context);
    }
    static void physical(GeneratedCanonicalNameTest.Fixture f, long id, LocalDateTime value, long version) throws Exception {
        try (var connection = DriverManager.getConnection(f.data.url);
             var statement = connection.prepareStatement("SELECT required_datetime,version FROM scalar_type_probe_data WHERE id=?")) {
            statement.setLong(1, id);
            try (var rows = statement.executeQuery()) {
                assertTrue(rows.next());
                var stored = LocalDateTime.parse(rows.getString(1).replace(' ', 'T'));
                assertEquals(value, stored); assertEquals(0, stored.getNano() % 1000000);
                assertEquals(version, rows.getLong(2)); assertFalse(rows.next());
            }
        }
    }
    static void emit(String name, int cases) throws Exception {
        System.out.println("JAVA_SQL_MILLISECOND=" + new ObjectMapper().writeValueAsString(Map.of(
                "case", name, "cases", cases, "round", round(), "passed", true,
                "legacyMigrationRejectionAccepted", name.equals("legacy-rejection"),
                "legacySqlPrecisionAccepted", false, "businessClockChanged", false)));
    }

    @Test public void generatedSaveEqualityAndEUseTheSameMillisecondBoundary() throws Exception {
        var previous = TimeZone.getDefault();
        try {
            var f = fixture("matrix"); var root = f.root("SQL precision matrix round " + round()); int checked = 0;
            for (var zone : ZONES) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                for (var input : INPUTS) {
                    var expected = input.truncatedTo(ChronoUnit.MILLIS);
                    var row = f.probe(root, "SQL millis " + zone + " " + checked);
                    row.updateRequiredDatetime(input);
                    assertEquals(input, E.scalarTypeProbe(row).getRequiredDatetime().eval());
                    row.auditAs("Persist a generated sub-millisecond datetime").save(f.context);
                    assertEquals(expected, E.scalarTypeProbe(row).getRequiredDatetime().eval());
                    assertEquals(Long.valueOf(1), row.getVersion());
                    var loaded = f.load(row.getId());
                    assertEquals(expected, E.scalarTypeProbe(loaded).getRequiredDatetime().eval());
                    assertEquals(row.getId(), equal(f, row.getId(), expected).getId());
                    assertEquals(row.getId(), equal(f, row.getId(), input).getId());
                    assertNull(equal(f, row.getId(), expected.plus(1, ChronoUnit.MILLIS)));
                    physical(f, row.getId(), expected, 1); checked++;
                }
            }
            assertEquals(18, checked); emit("matrix-save-query-e", checked);
        } finally { TimeZone.setDefault(previous); }
    }

    @Test public void generatedAuditedUpdateReadsBackCanonicalValueAndRemainsQueryable() throws Exception {
        var previous = TimeZone.getDefault();
        try {
            var f = fixture("update"); var root = f.root("SQL precision update round " + round());
            for (var zone : ZONES) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                var row = f.probe(root, "SQL millis update " + zone);
                row.updateRequiredDatetime(INPUTS.get(0)); row.auditAs("Seed canonical datetime").save(f.context);
                var update = f.load(row.getId()); var input = INPUTS.get(3); var expected = input.truncatedTo(ChronoUnit.MILLIS);
                update.updateRequiredDatetime(input); int audits = f.audits.size();
                update.auditAs("Update a generated datetime with authoritative SQL readback").save(f.context);
                assertEquals(audits + 1, f.audits.size()); assertEquals(Long.valueOf(2), update.getVersion());
                assertEquals(expected, E.scalarTypeProbe(update).getRequiredDatetime().eval());
                var loaded = equal(f, row.getId(), input); assertNotNull(loaded);
                assertEquals(expected, E.scalarTypeProbe(loaded).getRequiredDatetime().eval());
                assertEquals(Long.valueOf(2), loaded.getVersion()); physical(f, row.getId(), expected, 2);
            }
            emit("update-equality", 3);
        } finally { TimeZone.setDefault(previous); }
    }

    @Test public void nativeGeneratedJsonRetainsNanosecondsWithoutProviderOrAudit() throws Exception {
        var f = fixture("json"); var mapper = new ObjectMapper().registerModule(new TeaQLModule(f.context));
        long calls = f.data.calls; int audits = f.audits.size(), policies = f.policies.size(); int checked = 0;
        for (var zone : ZONES) {
            var zoned = mapper.copy().setTimeZone(TimeZone.getTimeZone(zone));
            for (var input : INPUTS) {
                var json = mapper.createObjectNode().put("requiredDatetime", input.toString());
                var decoded = zoned.treeToValue(json, ScalarTypeProbe.class);
                assertEquals(input, E.scalarTypeProbe(decoded).getRequiredDatetime().eval());
                var encoded = zoned.readTree(zoned.writeValueAsString(decoded));
                assertEquals(input, LocalDateTime.parse(encoded.path("requiredDatetime").textValue()));
                assertFalse(decoded.__internalHasMutationLedger()); checked++;
            }
        }
        assertEquals(calls, f.data.calls); assertEquals(audits, f.audits.size()); assertEquals(policies, f.policies.size());
        emit("native-json", checked);
    }

    @Test public void omittedDatetimeRemainsNotLoadedRatherThanBecomingNull() throws Exception {
        var f = fixture("projection"); var root = f.root("SQL precision projection round " + round());
        var row = f.probe(root, "SQL millis minimal"); row.updateRequiredDatetime(INPUTS.get(0));
        row.auditAs("Seed a bounded datetime projection").save(f.context);
        long audits = f.audits.size(), policies = f.policies.size();
        var minimal = Q.scalarTypeProbesWithMinimalFields().withIdIs(row.getId()).selectRequiredString().limit(1)
                .comment("Load a probe without its datetime field")
                .purpose("Preserve unselected datetime load state").executeForOne(f.context);
        assertEquals(row.getId(), minimal.getId());
        assertThrows(TeaQLNotLoadedException.class, () -> E.scalarTypeProbe(minimal).getRequiredDatetime().eval());
        assertEquals(audits, f.audits.size()); assertEquals(policies, f.policies.size());
        emit("minimal-projection", 1);
    }

    @Test public void legacySqlValuesRejectWithFieldDiagnosticsAndRemainPhysicallyUnchanged() throws Exception {
        var f = fixture("legacy"); var root = f.root("SQL legacy precision round " + round());
        var row = f.probe(root, "Legacy precision negative fixture");
        row.updateRequiredDatetime(INPUTS.get(0));
        row.auditAs("Seed a valid object before legacy corruption injection").save(f.context);
        String legacy = "2026-11-10 12:34:56.123456789";
        try (var connection = DriverManager.getConnection(f.data.url);
                var statement = connection.prepareStatement("UPDATE scalar_type_probe_data SET required_datetime=? WHERE id=?")) {
            // Explicit fault injection of a pre-existing incompatible representation.
            // Normal setup and all business operations use generated mutations.
            statement.setString(1, legacy); statement.setLong(2, row.getId());
            assertEquals(1, statement.executeUpdate());
        }
        long audits = f.audits.size(), policies = f.policies.size();
        for (int attempt = 0; attempt < 2; attempt++) {
            var error = assertThrows(SqlTemporalPrecisionException.class, () -> f.load(row.getId()));
            assertEquals("SQL_DATETIME_PRECISION_MISMATCH", error.getCode());
            assertEquals("ScalarTypeProbe", error.getEntityType()); assertEquals("required_datetime", error.getField());
            assertEquals("/requiredDatetime", error.getPath()); assertEquals("milliseconds", error.getExpectedPrecision());
            assertTrue(error.isMigrationRequired()); assertTrue(error.getSuggestedFix().contains("Migrate"));
            assertFalse(error.toString().contains(legacy)); assertNull(error.getCause());
        }
        var minimal = Q.scalarTypeProbesWithMinimalFields().withIdIs(row.getId()).selectRequiredString().limit(1)
                .comment("Read a projection that intentionally excludes the incompatible legacy field")
                .purpose("Keep SQL precision rejection local to selected fields").executeForOne(f.context);
        assertEquals(row.getId(), minimal.getId());
        assertThrows(TeaQLNotLoadedException.class, () -> E.scalarTypeProbe(minimal).getRequiredDatetime().eval());
        assertEquals(audits, f.audits.size()); assertEquals(policies, f.policies.size());
        try (var connection = DriverManager.getConnection(f.data.url);
                var statement = connection.prepareStatement("SELECT required_datetime,version FROM scalar_type_probe_data WHERE id=?")) {
            statement.setLong(1, row.getId());
            try (var stored = statement.executeQuery()) {
                assertTrue(stored.next()); assertEquals(legacy, stored.getString(1));
                assertEquals(1, stored.getLong(2)); assertFalse(stored.next());
            }
        }
        emit("legacy-rejection", 4);
    }
}
