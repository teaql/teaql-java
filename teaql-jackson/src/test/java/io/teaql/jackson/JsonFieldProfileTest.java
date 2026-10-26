package io.teaql.jackson;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.*;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.meta.*;
import io.teaql.runtime.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/** Old-runtime fallback is test-only: the baseline still compiles and exposes wrong wire behavior. */
@RunWith(Parameterized.class)
public class JsonFieldProfileTest {
    @Parameterized.Parameters(name = "{0}") public static Collection<Object[]> profiles() {
        return List.of(new Object[]{JsonFieldNamingProfile.CAMEL_CASE}, new Object[]{JsonFieldNamingProfile.SNAKE_CASE}, new Object[]{JsonFieldNamingProfile.PASCAL_CASE});
    }
    private final JsonFieldNamingProfile profile;
    public JsonFieldProfileTest(JsonFieldNamingProfile profile) { this.profile = profile; }
    static FieldLayout layout(Class<?> type, JsonFieldNamingProfile profile) {
        var indexes = Map.of("id", 0, "version", 1, "base_url", 2, "other_name", 3);
        var mappings = Map.of("id", List.of("id", "id"), "version", List.of("version", "version"),
                "base_url", List.of("baseUrl", "private_storage_column"), "other_name", List.of("otherName", "other_name"));
        var relations = Set.of("child_row", "child_list");
        try {
            return FieldLayout.installGenerated((FieldLayout) FieldLayout.class.getMethod("generated", Class.class, String.class,
                    Map.class, Map.class, Set.class, JsonFieldNamingProfile.class, Map.class)
                    .invoke(null, type, "wire-v1", indexes, mappings, relations, profile, Map.of("legacy_url", "base_url")));
        } catch (NoSuchMethodException baseline) {
            return FieldLayout.installGenerated(FieldLayout.generated(type, "wire-v1", indexes, mappings, relations));
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error instanceof InvocationTargetException invocation ? invocation.getCause() : error);
        }
    }
    public abstract static class Row extends BaseEntity {
        String baseUrl, otherName;
        Row child;
        SmartList<Row> children;
        @Override public String typeName() { return "ProfileRow"; }
        @Override public Object __internalGet(String name) {
            return switch (name) {
                case "base_url" -> baseUrl; case "other_name" -> otherName;
                case "child_row" -> child; case "child_list" -> children;
                default -> super.__internalGet(name);
            };
        }
        @Override public void __internalSet(String name, Object value) {
            markPropertyLoaded(name);
            switch (name) {
                case "base_url" -> baseUrl = (String) value; case "other_name" -> otherName = (String) value;
                case "child_row" -> child = (Row) value; case "child_list" -> children = (SmartList<Row>) value;
                default -> super.__internalSet(name, value);
            }
        }
    }
    public static final class CamelRow extends Row { static final FieldLayout LAYOUT = layout(CamelRow.class, JsonFieldNamingProfile.CAMEL_CASE); }
    public static final class SnakeRow extends Row { static final FieldLayout LAYOUT = layout(SnakeRow.class, JsonFieldNamingProfile.SNAKE_CASE); }
    public static final class PascalRow extends Row { static final FieldLayout LAYOUT = layout(PascalRow.class, JsonFieldNamingProfile.PASCAL_CASE); }
    static final class Fixture {
        final AtomicInteger constructed = new AtomicInteger();
        final ObjectMapper mapper;
        final Class<? extends Row> type;
        Fixture(JsonFieldNamingProfile profile) {
            // Initialize generated metadata before constructing the first row.
            if (profile == JsonFieldNamingProfile.CAMEL_CASE) { assertNotNull(CamelRow.LAYOUT); type = CamelRow.class; }
            else if (profile == JsonFieldNamingProfile.SNAKE_CASE) { assertNotNull(SnakeRow.LAYOUT); type = SnakeRow.class; }
            else { assertNotNull(PascalRow.LAYOUT); type = PascalRow.class; }
            var metadata = new SimpleEntityMetaFactory(); var descriptor = new EntityDescriptor();
            descriptor.setType("ProfileRow"); descriptor.setTargetType(type);
            descriptor.setEntitySupplier(() -> { constructed.incrementAndGet(); return profile == JsonFieldNamingProfile.CAMEL_CASE ? new CamelRow()
                    : profile == JsonFieldNamingProfile.SNAKE_CASE ? new SnakeRow() : new PascalRow(); });
            descriptor.addSimpleProperty("id", Long.class); descriptor.addSimpleProperty("version", Long.class);
            descriptor.addSimpleProperty("base_url", String.class); descriptor.addSimpleProperty("other_name", String.class);
            metadata.register(descriptor); descriptor.addObjectProperty(metadata, "child_row", "ProfileRow", "child_list", type);
            var context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).build());
            mapper = new ObjectMapper().registerModule(new TeaQLModule(context));
        }
    }
    @Test public void modelProfileControlsOutputIncludingIdentityAndNamedRelations() throws Exception {
        var f = new Fixture(profile); var row = f.mapper.readValue("{\"id\":1,\"version\":2,\"base_url\":\"fixed\",\"child_row\":{\"id\":2},\"child_list\":[]}", f.type);
        var output = f.mapper.readTree(f.mapper.writeValueAsString(row));
        assertEquals(1L, output.path(profile.render("id")).asLong(-1));
        assertEquals(2L, output.path(profile.render("version")).asLong(-1));
        assertEquals("fixed", output.path(profile.render("base_url")).asText());
        assertTrue(output.has(profile.render("child_row"))); assertTrue(output.has(profile.render("child_list")));
        assertFalse(output.has("private_storage_column")); assertFalse(output.has("otherName"));
        assertEquals(output, f.mapper.readTree(f.mapper.writeValueAsString(f.mapper.treeToValue(output, f.type))));
    }
    @Test public void selectedWireNamesDecodeWithoutNativeOrStorageFallback() throws Exception {
        var f = new Fixture(profile); var input = f.mapper.createObjectNode().put(profile.render("base_url"), "wire");
        var row = f.mapper.treeToValue(input, f.type); assertEquals("wire", row.baseUrl);
        assertTrue(row.isPropertyLoaded("base_url")); assertFalse(row.isPropertyLoaded("other_name"));
        assertTrue(row.getUpdatedProperties().isEmpty()); assertFalse(row.__internalHasMutationLedger());
    }
    @Test public void declaredLegacyAliasIsInputOnlyAndCollisionsRejectEqualValues() throws Exception {
        var f = new Fixture(profile); var row = f.mapper.readValue("{\"legacy_url\":\"old\"}", f.type);
        var output = f.mapper.readTree(f.mapper.writeValueAsString(row));
        assertEquals("old", output.path(profile.render("base_url")).asText()); assertFalse(output.has("legacy_url"));
        int count = f.constructed.get();
        var input = f.mapper.createObjectNode().put(profile.render("base_url"), "same").put("legacy_url", "same");
        assertThrows(JsonMappingException.class, () -> f.mapper.treeToValue(input, f.type)); assertEquals(count, f.constructed.get());
    }
    @Test public void physicalColumnCannotEnterTheJsonVocabulary() {
        var f = new Fixture(profile);
        var error = assertThrows(JsonMappingException.class, () -> f.mapper.readValue("{\"private_storage_column\":\"PRIVATE_PROFILE_CANARY\"}", f.type));
        assertEquals(0, f.constructed.get()); assertFalse(error.getMessage().contains("PRIVATE_PROFILE_CANARY"));
    }
    @Test public void loadedNullAndMissingAreNotMergedByTheProfile() throws Exception {
        var f = new Fixture(profile); var row = f.mapper.treeToValue(f.mapper.createObjectNode().putNull(profile.render("base_url")), f.type);
        assertTrue(row.isPropertyLoaded("base_url")); assertNull(row.baseUrl); assertFalse(row.isPropertyLoaded("other_name"));
        var output = f.mapper.readTree(f.mapper.writeValueAsString(row));
        assertTrue(output.get(profile.render("base_url")).isNull()); assertFalse(output.has(profile.render("other_name")));
    }
    @Test public void duplicateLiteralKeysAreRejectedBeforeConstruction() {
        var f = new Fixture(profile); String name = profile.render("base_url");
        var error = assertThrows(java.io.IOException.class, () -> f.mapper.readValue("{\"" + name + "\":\"same\",\"" + name + "\":\"same\"}", f.type));
        assertTrue(error.getMessage(), error.getMessage().contains("Duplicate"));
        assertEquals(0, f.constructed.get());
    }
    @Test public void unselectedNativeOrOtherProfileSpellingIsNotAnImplicitAlias() {
        var f = new Fixture(profile);
        String undeclared = profile == JsonFieldNamingProfile.CAMEL_CASE ? "BaseUrl" : "baseUrl";
        assertThrows(JsonMappingException.class, () -> f.mapper.treeToValue(f.mapper.createObjectNode().put(undeclared, "private"), f.type));
        assertEquals(0, f.constructed.get());
    }
    @Test public void duplicateDetectionRestoresOnlyTheInvokingParsersPreviousFlag() throws Exception {
        var f = new Fixture(profile);
        for (boolean initiallyStrict : List.of(false, true)) {
            try (var parser = f.mapper.createParser("{\"base_url\":null}")) {
                parser.configure(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION, initiallyStrict);
                f.mapper.readValue(parser, f.type);
                assertEquals(initiallyStrict, parser.isEnabled(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION));
            }
        }
        assertFalse(f.mapper.getFactory().isEnabled(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION));
    }
}
