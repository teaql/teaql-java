package com.example.scalartypeconformanceservice;

import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.checker.CheckException;
import io.teaql.core.checker.CheckResult;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.*;

/** Observe the unchanged generated APIs on both the negative and repaired runtime. */
public final class JavaFiniteScalarProbe {
    static final List<String> FIELDS = List.of("required_float", "optional_float", "required_double", "optional_double");
    static final ObjectMapper JSON = new ObjectMapper();
    static void assign(ScalarTypeProbe row, String field, double number) {
        switch (field) {
            case "required_float" -> row.updateRequiredFloat((float) number);
            case "optional_float" -> row.updateOptionalFloat((float) number);
            case "required_double" -> row.updateRequiredDouble(number);
            case "optional_double" -> row.updateOptionalDouble(number);
            default -> throw new AssertionError(field);
        }
    }
    static Map<String, Object> outcome(Runnable save, String field, GeneratedCanonicalNameTest.Fixture f) {
        long provider = f.data.calls; int policies = f.policies.size(), audits = f.audits.size();
        RuntimeException error = null;
        try { save.run(); } catch (RuntimeException e) { error = e; }
        long calls = f.data.calls - provider;
        var value = new LinkedHashMap<String, Object>();
        value.put("accepted", error == null); value.put("exception", error == null ? null : error.getClass().getName());
        value.put("providerConnections", calls); value.put("policyCalls", f.policies.size() - policies);
        value.put("audits", f.audits.size() - audits);
        List<CheckResult> violations = error instanceof CheckException e ? e.getViolates() : List.of();
        boolean structured = violations.stream().anyMatch(v -> v.getRuleId() == CheckResult.RuleId.INVALID_TYPE
                && "ScalarTypeProbe".equals(v.getRootType()) && v.getLocation() != null
                && v.getLocation().modelPath().endsWith(field) && v.getInputValue() == null);
        value.put("structuredInvalidType", structured);
        value.put("canonicalLocations", violations.stream().map(v -> v.getLocation().modelPath()).toList());
        value.put("threeWireProfiles", structured && violations.stream().allMatch(v ->
                Arrays.stream(JsonFieldNamingProfile.values()).allMatch(p ->
                        v.toWire(p).instancePath().endsWith(p.render(field)) && v.toWire(p).inputValue() == null)));
        value.put("contractPass", structured && calls == 0 && f.policies.size() == policies && f.audits.size() == audits);
        return value;
    }
    static Map<String, Object> stored(GeneratedCanonicalNameTest.Fixture f, long id, String field) throws Exception {
        try (var db = DriverManager.getConnection(f.data.url);
             var query = db.prepareStatement("SELECT version," + field + " FROM scalar_type_probe_data WHERE id=?")) {
            query.setLong(1, id); try (var rows = query.executeQuery()) {
                var value = new LinkedHashMap<String, Object>();
                value.put("exists", rows.next()); if (Boolean.TRUE.equals(value.get("exists"))) {
                    value.put("version", rows.getLong(1)); Object number = rows.getObject(2);
                    value.put("number", number == null ? null : number.toString());
                } return value;
            }
        }
    }
    static void emit(Map<String, Object> value) throws Exception { System.out.println("JAVA_FINITE_SCALAR_CASE=" + JSON.writeValueAsString(value)); }
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("database round");
        int round = Integer.parseInt(args[1]);
        var origins = new LinkedHashMap<String, String>();
        for (var type : List.of(TeaQLRuntime.class, JdbcSqlExecutor.class, ScalarTypeProbe.class))
            origins.put(type.getName(), Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().toString());
        System.out.println("JAVA_FINITE_SCALAR_ORIGINS=" + JSON.writeValueAsString(origins));
        var f = new GeneratedCanonicalNameTest.Fixture("jdbc:sqlite:" + Path.of(args[0]).toAbsolutePath(), round);
        var root = f.root("Finite scalar root " + round);
        int cases = 0, passed = 0, accepted = 0, writes = 0;
        var numbers = new LinkedHashMap<String, Double>();
        numbers.put("NaN", Double.NaN); numbers.put("Infinity", Double.POSITIVE_INFINITY); numbers.put("-Infinity", Double.NEGATIVE_INFINITY);
        for (String status : List.of("create", "update")) for (String field : FIELDS) for (var number : numbers.entrySet()) {
            var row = f.probe(root, "finite " + status + " " + field + " " + number.getKey() + " " + round);
            if (status.equals("update")) row.auditAs("Create a finite update control").save(f.context);
            assign(row, field, number.getValue());
            var value = outcome(() -> row.auditAs("Reject a non-finite generated scalar").save(f.context), field, f);
            value.put("round", round); value.put("case", status); value.put("field", field); value.put("number", number.getKey());
            value.put("id", row.getId()); value.put("stored", stored(f, row.getId(), field));
            cases++; if (Boolean.TRUE.equals(value.get("contractPass"))) passed++;
            if (Boolean.TRUE.equals(value.get("accepted"))) accepted++;
            if (((Number)value.get("providerConnections")).longValue() > 0) writes++;
            emit(value);
        }
        var graphRoot = f.root("Finite graph root " + round);
        var first = f.probe(graphRoot, "finite graph first " + round); first.auditAs("Create first finite graph witness").save(f.context);
        var second = f.probe(graphRoot, "finite graph second " + round); second.auditAs("Create second finite graph witness").save(f.context);
        var graph = Q.typePlatforms().withIdIs(graphRoot.getId()).limit(1)
                .selectScalarTypeProbeListWith(Q.scalarTypeProbes().orderByIdAscending().limit(2))
                .comment("Load exactly two complete scalar graph witnesses").purpose("Reject the invalid graph before the first write").executeForOne(f.context);
        var children = graph.getScalarTypeProbeList(); if (children.size() != 2) throw new AssertionError("two graph witnesses required");
        children.get(0).updateRequiredDouble(7.5d); children.get(1).updateOptionalFloat(Float.POSITIVE_INFINITY);
        var result = outcome(() -> graph.auditAs("Reject the complete non-finite scalar graph").save(f.context), "optional_float", f);
        result.put("round", round); result.put("case", "graph"); result.put("firstId", first.getId()); result.put("secondId", second.getId());
        result.put("firstStored", stored(f, first.getId(), "required_double")); result.put("secondStored", stored(f, second.getId(), "optional_float"));
        cases++; if (Boolean.TRUE.equals(result.get("contractPass"))) passed++; emit(result);
        System.out.println("JAVA_FINITE_SCALAR_RESULT=" + JSON.writeValueAsString(Map.of("round", round, "cases", cases,
                "contractPasses", passed, "contractFailures", cases - passed, "acceptedNonfiniteSaves", accepted, "invalidSavesReachingProvider", writes,
                "completed", true, "runtimeChanged", false)));
    }
}
