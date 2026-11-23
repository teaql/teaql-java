package io.teaql.core.checker;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.i18n.CheckerMessageDefaults;
import io.teaql.core.i18n.Locale;
import java.io.IOException;
import java.util.*;

/** Data-only metadata loader. No reflection, resource lookup or object access. */
final class CheckerPresentationJson {
    private CheckerPresentationJson() { }

    static CheckerPresentation.Model parse(String source) {
        Objects.requireNonNull(source, "source");
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
            mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
            JsonNode root = mapper.readTree(source);
            object(root, Set.of("schema", "revision", "entities", "currentObject"));
            if (!"teaql.checker-model/v1".equals(text(root.get("schema")))) throw invalid();
            String revision = text(root.get("revision"));
            JsonNode descriptors = root.get("entities");
            object(descriptors, null);
            Map<String, CheckerPresentation.Entity> entities = new LinkedHashMap<>();
            var iterator = descriptors.fields();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                String identity = text(mapper.getNodeFactory().textNode(entry.getKey()));
                JsonNode entity = entry.getValue();
                object(entity, Set.of("labels", "fields"));
                JsonNode fieldNodes = entity.get("fields");
                if (fieldNodes == null || !fieldNodes.isArray()) throw invalid();
                List<CheckerPresentation.Field> fields = new ArrayList<>();
                for (JsonNode field : fieldNodes) {
                    object(field, Set.of("name", "targetType", "labels", "classifiers", "aliases"));
                    String name = text(field.get("name"));
                    String target = field.has("targetType") ? text(field.get("targetType")) : null;
                    Set<String> aliases = new HashSet<>();
                    if (field.has("aliases")) {
                        JsonNode aliasNodes = field.get("aliases");
                        if (!aliasNodes.isArray()) throw invalid();
                        for (JsonNode alias : aliasNodes) if (!aliases.add(text(alias))) throw invalid();
                    }
                    fields.add(new CheckerPresentation.Field(name, target, labels(field.get("labels")),
                            labels(field.get("classifiers")), aliases));
                }
                entities.put(identity, new CheckerPresentation.Entity(labels(entity.get("labels")), fields));
            }
            Map<Locale, String> current = new EnumMap<>(Locale.class);
            current.putAll(CheckerMessageDefaults.currentObjectLabels());
            if (root.has("currentObject")) current.putAll(labels(root.get("currentObject")));
            return new CheckerPresentation.Model(revision, entities, current);
        } catch (IOException malformed) {
            // Parser diagnostics can contain raw source fragments; do not expose them.
            throw invalid();
        }
    }

    private static Map<Locale, String> labels(JsonNode node) {
        if (node == null) return Map.of();
        object(node, null);
        Map<Locale, String> labels = new EnumMap<>(Locale.class);
        var iterator = node.fields();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Locale locale = Locale.fromCode(entry.getKey());
            if (labels.putIfAbsent(locale, text(entry.getValue())) != null) throw invalid();
        }
        return labels;
    }

    private static void object(JsonNode node, Set<String> allowed) {
        if (node == null || !node.isObject()) throw invalid();
        if (allowed != null) node.fieldNames().forEachRemaining(key -> { if (!allowed.contains(key)) throw invalid(); });
    }

    private static String text(JsonNode node) {
        if (node == null || !node.isTextual()) throw invalid();
        String value = node.textValue();
        if (value.isBlank() || value.codePoints().anyMatch(character -> {
            int kind = Character.getType(character);
            return kind == Character.CONTROL || kind == Character.FORMAT
                    || kind == Character.LINE_SEPARATOR || kind == Character.PARAGRAPH_SEPARATOR;
        })) throw invalid();
        return value;
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Invalid Checker presentation model");
    }
}
