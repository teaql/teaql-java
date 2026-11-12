package io.teaql.core.i18n;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.checker.CheckerPresentation;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.checker.ObjectLocation;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CheckerMessageCatalogTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    private static Map<Locale, String> labels(JsonNode node) {
        Map<Locale, String> output = new EnumMap<>(Locale.class);
        if (node != null) node.fields().forEachRemaining(entry -> output.put(Locale.fromCode(entry.getKey()), entry.getValue().textValue()));
        return output;
    }

    private static JsonNode fixture() throws IOException {
        try (var input = CheckerMessageCatalogTest.class.getResourceAsStream("natural-cases-v1.json")) {
            var fixture = JSON.readTree(input);
            try (var roots = CheckerMessageCatalogTest.class.getResourceAsStream("natural-root-cases-v1.json")) {
                for (var rootCase : JSON.readTree(roots).get("cases"))
                    ((com.fasterxml.jackson.databind.node.ArrayNode) fixture.get("cases")).add(rootCase);
            }
            return fixture;
        }
    }

    private static CheckerPresentation.Model model(JsonNode fixture) {
        Map<String, CheckerPresentation.Entity> entities = new HashMap<>();
        fixture.get("entities").fields().forEachRemaining(entry -> {
            List<CheckerPresentation.Field> fields = new ArrayList<>();
            for (JsonNode field : entry.getValue().get("fields")) {
                Set<String> aliases = new HashSet<>();
                field.path("aliases").forEach(alias -> aliases.add(alias.textValue()));
                fields.add(new CheckerPresentation.Field(field.get("name").textValue(), field.path("targetType").textValue(),
                        labels(field.get("labels")), labels(field.get("classifiers")), aliases));
            }
            entities.put(entry.getKey(), new CheckerPresentation.Entity(labels(entry.getValue().get("labels")), fields));
        });
        return new CheckerPresentation.Model(fixture.get("revision").textValue(), entities, labels(fixture.get("currentObject")));
    }

    private static ObjectLocation location(JsonNode path) {
        ObjectLocation location = null;
        for (JsonNode step : path) {
            location = step.isIntegralNumber()
                    ? location == null ? ObjectLocation.arrayRoot(step.intValue()) : location.element(step.intValue())
                    : location == null ? ObjectLocation.hashRoot(step.textValue()) : location.member(step.textValue());
        }
        return location;
    }

    private static Map<String, Object> arguments(JsonNode node) {
        Map<String, Object> args = new HashMap<>();
        if (node != null) node.fields().forEachRemaining(entry -> args.put(entry.getKey(), entry.getValue().isNumber() ? entry.getValue().numberValue() : entry.getValue().textValue()));
        return args;
    }

    @Test public void sharedGoldenLocationsAndConstraints() throws Exception {
        var fixture = fixture(); var model = model(fixture);
        for (var test : fixture.get("cases")) {
            String name = test.get("name").textValue();
            var location = location(test.get("path"));
            String pointer = location == null ? null : location.instancePath();
            String origin = test.has("origin") ? test.get("origin").textValue() : fixture.get("origin").textValue();
            var presentation = CheckerPresentation.capture(model, origin, location);
            assertEquals(name, test.get("shape").textValue(), presentation.shape().name());
            String message = presentation.render(CheckerMessageCatalog.builtin(), Locale.fromCode(test.get("locale").textValue()),
                    test.get("rule").textValue(), arguments(test.get("arguments")), warning -> {});
            assertEquals(name, test.get("expected").textValue(), message);
            assertEquals(name, pointer, location == null ? null : location.instancePath());
            if (test.has("instancePath")) assertEquals(test.get("instancePath").textValue(), pointer);
            if (name.equals("two-list")) {
                assertEquals("/entry_list/1/name", location.instancePath(JsonFieldNamingProfile.SNAKE_CASE));
                assertEquals("/entryList/1/name", pointer);
            }
        }
    }

    @Test public void allLocalesAllRulesAllShapesAreComplete() {
        var catalog = CheckerMessageCatalog.builtin();
        for (var locale : Locale.values()) for (var shape : CheckerMessageCatalog.Shape.values())
            for (String rule : List.of("required", "min", "max", "minLength", "maxLength", "invalid")) {
                var selected = catalog.select(locale, "checker." + rule, shape);
                assertEquals(locale, selected.locale());
                String message = selected.render(Map.of("field", "FIELD", "object", "OWNER", "objectPath", "CHAIN",
                        "system", 8, "input_len", 9, "unit", "UNIT"));
                assertTrue(message, message.contains("FIELD")); assertFalse(message, message.contains("{"));
                if (shape == CheckerMessageCatalog.Shape.TWO) assertTrue(message, message.contains("OWNER"));
                if (shape == CheckerMessageCatalog.Shape.DEEP) assertTrue(message, message.contains("CHAIN"));
            }
    }

    private static CheckerMessageCatalog catalog(String locales) {
        return parse("{\"schema\":\"teaql.checker-messages/v1\",\"defaultLocale\":\"en\",\"locales\":" + locales + "}");
    }
    private static CheckerMessageCatalog parse(String source) {
        return CheckerMessageCatalog.fromJson(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
    }

    @Test public void partialOverridesAndEffectiveLocale() throws Exception {
        var overrides = catalog("{\"en\":{\"rules\":{\"checker.custom\":{\"two\":\"Supply {field} for {object}.\"},\"checker.required\":{\"one\":\"Supply {field}.\"}}}}");
        var model = model(fixture());
        var path = ObjectLocation.hashRoot("entry_list").element(1).member("name");
        var presentation = CheckerPresentation.capture(model, "Catalog", path);
        assertEquals("Supply name for the 2nd entry.", presentation.render(overrides, Locale.CHINESE_SIMPLIFIED, "checker.custom", Map.of(), warning -> {}));
        assertEquals("请填写第2条明细的名称。", presentation.render(overrides, Locale.CHINESE_SIMPLIFIED, "checker.required", Map.of(), warning -> {}));
        assertNull(overrides.override(Locale.ENGLISH, "checker.required", CheckerMessageCatalog.Shape.TWO));
        assertNotNull(overrides.override(Locale.ENGLISH, "checker.required", CheckerMessageCatalog.Shape.ONE));
    }

    @Test public void invalidCatalogsRejectWithoutReplacingTheCurrentCatalog() {
        String[] broken = {
            "{\"en\":{\"rules\":{\"checker.required\":{\"one\":\"{field} {input}\"}}}}",
            "{\"en\":{\"rules\":{\"checker.required\":{\"one\":\"{secret}\"}}}}",
            "{\"en\":{\"rules\":{\"checker.required\":{\"one\":\"\"}}}}",
            "{\"en\":{\"rules\":{\"checker.required\":{\"two\":\"{field}\"}}}}",
            "{\"en\":{\"rules\":{\"checker.required\":{\"deep\":\"{field} {object}\"}}}}",
            "{\"en\":{\"rules\":{\"checker.required\":{\"three\":\"{field}\"}}}}",
            "{\"en\":{\"rules\":{\"checker.min\":{\"one\":\"{field}\"}}}}",
            "{\"en\":{\"rules\":{\"checker.maxLength\":{\"one\":\"{field} {system} {unit} {input_len} {input}\"}}}}",
            "{\"en\":{\"rules\":{\"checker.required\":{\"one\":\"{field\"}}}}",
            "{\"en\":{\"rules\":{\"checker.required\":{\"one\":\"{field}\",\"one\":\"duplicate\"}}}}",
            "{\"en\":{\"rules\":{\"checker.required\":{\"one\":\"{field}\"}}},\"en-US\":{\"rules\":{\"checker.required\":{\"one\":\"{field}\"}}}}",
            "{\"en\":{\"locationPatterns\":{\"item\":\"{relation}\"}}}",
            "{\"en\":{\"locationPatterns\":{\"separator\":\"{input}\"}}}"
        };
        var current = CheckerMessageCatalog.builtin();
        for (String source : broken) {
            assertThrows(IllegalArgumentException.class, () -> catalog(source));
            assertSame(current, CheckerMessageCatalog.builtin());
        }
        assertThrows(IllegalArgumentException.class, () -> parse("{\"schema\":\"teaql.i18n/v1\"}"));
        assertThrows(IllegalArgumentException.class, () -> parse("{\"schema\":\"teaql.checker-messages/v1\",\"defaultLocale\":\"en\",\"locales\":{\"en\":{\"rules\":{\"checker.required\":{\"one\":\"{field}\"}}}}} {}"));
        var error = assertThrows(IllegalArgumentException.class, () -> parse("{\"schema\": SECRET_PARSE_TEXT"));
        assertFalse(error.toString().contains("SECRET_PARSE_TEXT")); assertNull(error.getCause());
    }

    @Test public void literalValuesNeverBecomeTemplateInstructions() {
        var catalog = catalog("{\"en\":{\"rules\":{\"checker.required\":{\"one\":\"{{literal}} {field}\"}}}}");
        assertEquals("{literal} {system} ${process.exit()} <script>", catalog.select(Locale.ENGLISH, "checker.required", CheckerMessageCatalog.Shape.ONE)
                .render(Map.of("field", "{system} ${process.exit()} <script>")));
    }

    @Test public void ordinalsAndLargePositionsDoNotOverflow() {
        for (String expected : List.of("1st", "2nd", "3rd", "4th", "11th", "12th", "13th", "21st", "111th")) {
            int number = Integer.parseInt(expected.replaceAll("[^0-9]", ""));
            assertEquals(expected, CheckerMessageCatalog.englishOrdinal(number));
        }
        assertEquals("the 2147483648th entry", CheckerMessageCatalog.builtin().item(Locale.ENGLISH, "entry", Integer.MAX_VALUE, ""));
        assertThrows(IllegalArgumentException.class, () -> CheckerMessageCatalog.builtin().item(Locale.ENGLISH, "entry", -1, ""));
    }

    @Test public void declaringLeafDoesNotReplaceTheOriginOrExposeIds() throws Exception {
        var result = io.teaql.core.checker.CheckResult.required(ObjectLocation.hashRoot("entry_list").element(1).member("name"));
        result.setRootType("Entry"); result.setSourceInstancePath("/submitted/entryList/1/name");
        var presentation = CheckerPresentation.capture(model(fixture()), "Catalog", result.getLocation());
        result.setNaturalLanguageStatement(presentation.render(CheckerMessageCatalog.builtin(), Locale.ENGLISH, "checker.required", Map.of(), warning -> {}));
        var wire = result.toWire(JsonFieldNamingProfile.CAMEL_CASE);
        assertEquals("Entry", wire.entityType()); assertEquals("/entryList/1/name", wire.instancePath());
        assertEquals("/submitted/entryList/1/name", wire.sourceInstancePath());
        assertEquals("The name of the 2nd entry is required.", wire.message());
    }

    @Test public void snapshotsAndCatalogsAreImmutableAcrossConcurrentOperations() throws Exception {
        var model = model(fixture()); var catalog = CheckerMessageCatalog.builtin();
        var executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<String>> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                int index = i;
                tasks.add(executor.submit(() -> CheckerPresentation.capture(model, "Catalog", ObjectLocation.hashRoot("entry_list").element(index).member("name"))
                        .render(catalog, index % 2 == 0 ? Locale.ENGLISH : Locale.CHINESE_SIMPLIFIED, "checker.required", Map.of(), warning -> {})));
            }
            for (int i = 0; i < tasks.size(); i++) assertEquals(i % 2 == 0 ? "The name of the " + CheckerMessageCatalog.englishOrdinal(i + 1) + " entry is required."
                    : "请填写第" + (i + 1) + "条明细的名称。", tasks.get(i).get());
        } finally { executor.shutdownNow(); assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS)); }
    }

    @Test public void longPathsKeepAllOwnersWithoutRecursionOrValueAccess() throws Exception {
        var model = model(fixture());
        ObjectLocation path = ObjectLocation.hashRoot("entry_list").element(0);
        for (int i = 0; i < 1000; i++) path = path.member("entry_list").element(i);
        path = path.member("name");
        var snapshot = CheckerPresentation.capture(model, "Catalog", path);
        String message = snapshot.render(CheckerMessageCatalog.builtin(), Locale.ENGLISH, "checker.required", Map.of(), warning -> {});
        assertEquals(1000, message.chars().filter(c -> c == '→').count());
        assertTrue(message.endsWith("the 1000th entry"));
    }

    @Test public void labelsCannotForgeLinesAndWarningsAreBoundedAndNonfatal() {
        Map<Locale, String> mutableLabels = new HashMap<>(); mutableLabels.put(Locale.ENGLISH, "\n\u202e\t");
        var field = new CheckerPresentation.Field("name", null, mutableLabels, Map.of(), Set.of());
        mutableLabels.put(Locale.ENGLISH, "MUTATED");
        var model = new CheckerPresentation.Model("safe", Map.of("Catalog", new CheckerPresentation.Entity(Map.of(Locale.ENGLISH, "catalog"), List.of(field))),
                Map.of(Locale.ENGLISH, "current object"));
        var presentation = CheckerPresentation.capture(model, "Catalog", ObjectLocation.hashRoot("name"));
        List<String> warnings = new ArrayList<>();
        assertEquals("The name is required.", presentation.render(CheckerMessageCatalog.builtin(), Locale.ENGLISH, "checker.required", Map.of(), warnings::add));
        assertEquals(List.of("CHECKER_MESSAGE_LABEL_FALLBACK"), warnings);
        assertEquals("The name is required.", presentation.render(CheckerMessageCatalog.builtin(), Locale.ENGLISH, "checker.required", Map.of(), code -> { throw new IllegalStateException(); }));
    }

    @Test public void arbitraryHostObjectsCannotExecuteToStringDuringInterpolation() {
        var selected = CheckerMessageCatalog.builtin().select(Locale.ENGLISH, "checker.required", CheckerMessageCatalog.Shape.ONE);
        var invoked = new java.util.concurrent.atomic.AtomicBoolean();
        Object hostile = new Object() { @Override public String toString() { invoked.set(true); return "SECRET"; } };
        assertThrows(IllegalArgumentException.class, () -> selected.render(Map.of("field", hostile)));
        assertFalse(invoked.get());
    }
}
