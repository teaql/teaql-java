package com.example.scalartypeconformanceservice;

import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.value.TeaQLNotLoadedException;
import io.teaql.jackson.TeaQLModule;
import io.teaql.runtime.TeaQLRuntime;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/** Compiled separately for each model option; no hand edits to generated code. */
public class GeneratedJsonProfileTest {
    @Test public void generatedModelControlsItsOwnJsonBoundary() throws Exception {
        var profile = JsonFieldNamingProfile.fromModelValue(System.getenv("TEAQL_JSON_PROFILE"));
        int round = Integer.parseInt(System.getenv("TEAQL_NAME_ROUND"));
        var expected = Path.of(System.getProperty("teaql.expected.runtime.root")).toRealPath();
        for (var type : List.of(TeaQLRuntime.class, TeaQLModule.class))
            assertTrue(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().startsWith(expected));
        var f = new GeneratedCanonicalNameTest.Fixture("jdbc:sqlite:" + Path.of(System.getenv("TEAQL_NAME_DB")).toAbsolutePath(), round);
        var root = f.root("JSON profile " + profile + " round " + round);
        var value = f.probe(root, "profile " + round); value.auditAs("Persist the generated JSON profile probe").save(f.context);
        var loaded = Q.scalarTypeProbes().withIdIs(value.getId()).limit(1)
                .selectTypePlatformWith(Q.typePlatformsWithMinimalFields().selectName())
                .comment("Load the selected model JSON graph").purpose("Verify generated Q/E and its chosen wire names").executeForOne(f.context);
        var mapper = new ObjectMapper().findAndRegisterModules().registerModule(new TeaQLModule(f.context));
        long before = f.data.calls;
        var json = mapper.readTree(mapper.writeValueAsString(loaded));
        assertTrue(json.has(profile.render("id"))); assertTrue(json.has(profile.render("version")));
        assertEquals("profile " + round, json.path(profile.render("required_string")).asText());
        assertTrue(json.get(profile.render("optional_string")).isNull());
        assertEquals(root.getId().longValue(), json.path(profile.render("type_platform")).path(profile.render("id")).asLong());
        assertFalse(json.has("type_platform_id"));
        var decoded = mapper.treeToValue(json, ScalarTypeProbe.class);
        assertEquals(root.getId(), E.scalarTypeProbe(decoded).getTypePlatform().getId().eval());
        assertEquals("profile " + round, E.scalarTypeProbe(decoded).getRequiredString().eval());
        assertTrue(decoded.getUpdatedProperties().isEmpty()); assertFalse(decoded.__internalHasMutationLedger());
        String key = profile.render("required_string");
        assertThrows(java.io.IOException.class, () -> mapper.readValue("{\"" + key + "\":\"same\",\"" + key + "\":\"same\"}", ScalarTypeProbe.class));
        if (profile != JsonFieldNamingProfile.SNAKE_CASE) {
            var aliases = mapper.createObjectNode().put(key, "same").put("required_string", "same");
            assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class, () -> mapper.treeToValue(aliases, ScalarTypeProbe.class));
        }
        assertEquals(before, f.data.calls);
        var sparse = Q.scalarTypeProbesWithMinimalFields().withIdIs(value.getId()).selectRequiredString().limit(1)
                .comment("Project one generated JSON field").purpose("Preserve NotLoaded and model error paths").executeForOne(f.context);
        var sparseJson = mapper.readTree(mapper.writeValueAsString(sparse)); assertFalse(sparseJson.has(profile.render("optional_string")));
        var restored = mapper.treeToValue(sparseJson, ScalarTypeProbe.class);
        long beforeExpression = f.data.calls;
        var error = assertThrows(TeaQLNotLoadedException.class, () -> E.scalarTypeProbe(restored).getOptionalString().eval());
        String expectedPointer = switch (profile) {
            case CAMEL_CASE -> "/optionalString";
            case SNAKE_CASE -> "/optional_string";
            case PASCAL_CASE -> "/OptionalString";
        };
        var wire = error.toWire();
        assertEquals(expectedPointer, wire.instancePath());
        assertEquals(expectedPointer, wire.attemptedInstancePath());
        assertEquals(List.of(io.teaql.core.checker.WireLocationSegment.property("optional_string")), wire.location());
        assertEquals(wire.location(), wire.attemptedLocation());
        assertEquals(wire.location(), wire.suggestedFix().location());
        assertEquals("optional_string", wire.breakPoint());
        assertEquals(0, wire.breakPointIndex());
        assertEquals("NOT_LOADED", wire.code());
        assertEquals("E.scalarTypeProbe(<root>).getOptionalString().eval()", wire.originalExpression());
        assertEquals("E.scalarTypeProbe(<root>).getOptionalString()<broken>.eval()", wire.expressionWithBrokenPoint());
        assertTrue(error.getMessage(), error.getMessage().startsWith("TeaQLNotLoadedError:"));
        assertTrue(error.getMessage(), error.getMessage().contains("access_path=optionalString"));
        assertTrue(error.getMessage(), error.getMessage().contains("break_point=optional_string"));
        assertTrue(error.getMessage(), error.getMessage().contains(wire.expressionWithBrokenPoint()));
        assertFalse(error.getMessage().contains("\"instancePath\""));
        assertEquals(beforeExpression, f.data.calls);
        assertFalse(restored.__internalHasMutationLedger());
        System.out.println("GENERATED_E_MODEL_PROFILE=" + new ObjectMapper().writeValueAsString(Map.of(
                "language", "java", "profile", profile.modelValue(), "round", round, "passed", true,
                "wire", wire, "human", error.getMessage(), "providerEntries", 0, "mutationLedgerCreated", false)));
        System.out.println("JAVA_JSON_MODEL_PROFILE=" + new ObjectMapper().writeValueAsString(Map.of(
                "passed", true, "profile", profile.modelValue(), "round", round, "id", value.getId(),
                "nestedIdentity", true, "loadedNull", true, "notLoaded", true, "zeroProviderCodec", true,
                "duplicateKeysRejected", true, "modelErrorPath", error.toWire().instancePath())));
    }
}
