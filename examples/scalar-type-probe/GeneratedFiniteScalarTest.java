package com.example.scalartypeconformanceservice;

import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import org.junit.Test;
import static org.junit.Assert.*;

/** Ordinary writes use generated Q/E and audited Mutation; SQL is a labelled fault writer only. */
public class GeneratedFiniteScalarTest {
    static int round() { return Integer.parseInt(System.getenv("TEAQL_NAME_ROUND")); }
    static String database(String suffix) { return "jdbc:sqlite:" + Path.of(System.getenv("TEAQL_NAME_DB") + suffix).toAbsolutePath(); }
    static void rejected(Map<String,Object> result) {
        assertEquals(result.toString(),Boolean.TRUE,result.get("contractPass")); assertEquals(Boolean.FALSE,result.get("accepted"));
    }
    static void emit(String marker,Map<String,Object> value) throws Exception {
        System.out.println(marker + "=" + new ObjectMapper().writeValueAsString(value));
    }
    @Test public void originalTwentyFiveNegativeScenariosNowRejectBeforeProvider() throws Exception {
        var expected = Path.of(System.getProperty("teaql.expected.runtime.root")).toRealPath();
        var origins = new LinkedHashMap<String,Object>();
        for (var type : List.of(io.teaql.core.UserContext.class,io.teaql.core.checker.Checker.class,
                io.teaql.runtime.TeaQLRuntime.class,io.teaql.provider.jdbc.JdbcSqlExecutor.class)) {
            var origin = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
            assertTrue(type.getName(),origin.startsWith(expected)); origins.put(type.getName(),origin.toString());
        }
        emit("JAVA_FINITE_OFFICIAL_ORIGINS",origins);
        var bytes = new ByteArrayOutputStream(); PrintStream original = System.out;
        try (var captured = new PrintStream(new OutputStream() {
            public void write(int value) { original.write(value); bytes.write(value); }
            public void write(byte[] value,int offset,int length) { original.write(value,offset,length); bytes.write(value,offset,length); }
        },true,StandardCharsets.UTF_8)) {
            System.setOut(captured);
            try { JavaFiniteScalarProbe.main(new String[]{System.getenv("TEAQL_NAME_DB"),Integer.toString(round())}); }
            finally { System.setOut(original); }
        }
        int cases = 0, summaries = 0; var mapper = new ObjectMapper();
        for (String line : bytes.toString(StandardCharsets.UTF_8).split("\\R")) {
            if (line.startsWith("JAVA_FINITE_SCALAR_CASE=")) {
                var value = mapper.readTree(line.substring("JAVA_FINITE_SCALAR_CASE=".length()));
                assertTrue(value.toString(),value.path("contractPass").asBoolean()); cases++;
            }
            if (line.startsWith("JAVA_FINITE_SCALAR_RESULT=")) {
                var value = mapper.readTree(line.substring("JAVA_FINITE_SCALAR_RESULT=".length()));
                assertEquals(25,value.path("cases").asInt()); assertEquals(0,value.path("contractFailures").asInt()); summaries++;
            }
        }
        assertEquals(25,cases); assertEquals(1,summaries);
    }
    @Test public void actualCustomerFixCanRejectAndRepairEachGeneratedNumericField() throws Exception {
        var action = new AtomicReference<Consumer<ScalarTypeProbe>>(); var calls = new AtomicInteger();
        var f = new GeneratedCanonicalNameTest.Fixture(database(".finite-fix"),round(),row -> {
            if (action.get() != null) { calls.incrementAndGet(); action.get().accept(row); }
        });
        var root = f.root("Finite Fix root " + round()); int rejected = 0, repaired = 0;
        for (String status : List.of("create","update")) for (String field : JavaFiniteScalarProbe.FIELDS)
            for (double bad : new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) {
                var row = f.probe(root,"Finite post-Fix " + status + " " + field + " " + bad + " " + round());
                if (status.equals("update")) {
                    row.auditAs("Create a finite post-Fix control").save(f.context);
                    row.updateRequiredString(row.getRequiredString() + " requested update");
                }
                action.set(value -> JavaFiniteScalarProbe.assign(value,field,bad)); calls.set(0);
                rejected(JavaFiniteScalarProbe.outcome(() -> row.auditAs("Reject actual customer Fix non-finite output").save(f.context),field,f));
                assertEquals(1,calls.get()); rejected++; action.set(null);
            }
        for (String field : JavaFiniteScalarProbe.FIELDS) {
            var row = f.probe(root,"Finite repaired " + field + " " + round()); JavaFiniteScalarProbe.assign(row,field,Double.NaN);
            action.set(value -> JavaFiniteScalarProbe.assign(value,field,7.5d)); calls.set(0);
            row.auditAs("Accept a finite repair from the actual customer Fix").save(f.context);
            assertEquals(1,calls.get()); action.set(null); var stored = JavaFiniteScalarProbe.stored(f,row.getId(),field);
            assertEquals("7.5",stored.get("number")); assertEquals(1L,stored.get("version")); repaired++;
        }
        emit("JAVA_FINITE_CUSTOM_FIX",Map.of("round",round(),"rejected",rejected,"repaired",repaired,"oneFixPerSave",true,"passed",true));
    }
    @Test public void corruptLegacyInfinityFailsQueryAndLeavesSparseProjectionNotLoaded() throws Exception {
        var f = new GeneratedCanonicalNameTest.Fixture(database(".finite-legacy"),round());
        var root = f.root("Finite legacy root " + round()); int cases = 0;
        for (String field : JavaFiniteScalarProbe.FIELDS) for (double bad : new double[]{Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) {
            var row = f.probe(root,"Finite legacy " + field + " " + bad + " " + round());
            row.auditAs("Create a finite legacy witness").save(f.context);
            // Negative fixture: bypass the runtime to corrupt one test-owned cell.
            try (var db = DriverManager.getConnection(f.data.url); var update = db.prepareStatement(
                    "UPDATE scalar_type_probe_data SET " + field + "=? WHERE id=?")) {
                update.setDouble(1,bad); update.setLong(2,row.getId()); assertEquals(1,update.executeUpdate());
            }
            var error = assertThrows(RuntimeException.class,() -> f.load(row.getId())); boolean exact = false;
            // Generated Q uses typed reads. Their exact-numeric boundary reports
            // the projected JDBC label and target type, without the private value
            // or its original conversion cause. The untyped legacy message is
            // deliberately not the contract of this compiled mapper.
            var target = field.endsWith("float") ? Float.class : Double.class;
            var diagnostic = java.util.regex.Pattern.compile("Invalid JDBC column [1-9][0-9]* \\(" +
                    field.replace("_", "") + "\\) as " + java.util.regex.Pattern.quote(target.getName()));
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if (cause instanceof IllegalArgumentException && cause.getMessage() != null
                        && diagnostic.matcher(cause.getMessage()).matches()) {
                    assertNull("typed conversion must not retain the untrusted value in a cause",cause.getCause());
                    exact = true;
                }
            }
            assertTrue("the actual corrupt column must be identified",exact);
            var sparse = Q.scalarTypeProbesWithMinimalFields().withIdIs(row.getId()).selectRequiredString().limit(1)
                    .comment("Read only the safe legacy projection").purpose("Preserve NotLoaded rather than inventing null").executeForOne(f.context);
            assertEquals(row.getRequiredString(),E.scalarTypeProbe(sparse).getRequiredString().eval());
            assertThrows(io.teaql.core.value.TeaQLNotLoadedException.class,() -> E.scalarTypeProbe(sparse).getRequiredDouble().eval());
            var stored = JavaFiniteScalarProbe.stored(f,row.getId(),field);
            assertEquals(Double.toString(bad),stored.get("number")); assertEquals(1L,stored.get("version")); cases++;
        }
        emit("JAVA_FINITE_LEGACY",Map.of("round",round(),"cases",cases,"unchangedVersions",true,"passed",true));
    }
    @Test public void sameGeneratedGraphRetriesAfterOnlyTheInvalidChildIsRepaired() throws Exception {
        var f = new GeneratedCanonicalNameTest.Fixture(database(".finite-retry"),round()); var root = f.root("Finite retry root " + round());
        var first = f.probe(root,"Finite retry first " + round()); first.auditAs("Create first retry witness").save(f.context);
        var second = f.probe(root,"Finite retry second " + round()); second.auditAs("Create second retry witness").save(f.context);
        var graph = Q.typePlatforms().withIdIs(root.getId()).limit(1)
                .selectScalarTypeProbeListWith(Q.scalarTypeProbes().orderByIdAscending().limit(2))
                .comment("Load two complete retry witnesses").purpose("Prove rejected graph intent survives until repair").executeForOne(f.context);
        var a = graph.getScalarTypeProbeList().get(0); var b = graph.getScalarTypeProbeList().get(1);
        a.updateRequiredDouble(7.5d); b.updateOptionalFloat(Float.POSITIVE_INFINITY);
        rejected(JavaFiniteScalarProbe.outcome(() -> graph.auditAs("Reject the late invalid graph child").save(f.context),"optional_float",f));
        assertEquals(1L,JavaFiniteScalarProbe.stored(f,first.getId(),"required_double").get("version"));
        b.updateOptionalFloat(-4.5f); graph.auditAs("Retry the same retained graph after scalar repair").save(f.context);
        assertEquals(2L,JavaFiniteScalarProbe.stored(f,first.getId(),"required_double").get("version"));
        assertEquals(2L,JavaFiniteScalarProbe.stored(f,second.getId(),"optional_float").get("version"));
        assertEquals(Double.valueOf(7.5),E.scalarTypeProbe(f.load(first.getId())).getRequiredDouble().eval());
        assertEquals(Float.valueOf(-4.5f),E.scalarTypeProbe(f.load(second.getId())).getOptionalFloat().eval());
        emit("JAVA_FINITE_GRAPH_RETRY",Map.of("round",round(),"children",2,"version",2,"sameGraph",true,"passed",true));
    }
}
