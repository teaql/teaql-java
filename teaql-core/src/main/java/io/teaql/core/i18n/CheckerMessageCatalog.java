package io.teaql.core.i18n;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/** Immutable, data-only natural message resources. Does not change the legacy i18n schema. */
public final class CheckerMessageCatalog {
    public enum Shape { ONE, TWO, DEEP }

    private record Node(String literal, String argument) {}

    /** A compiled template interpolates once; inserted strings are never parsed as instructions. */
    public static final class Template {
        private final List<Node> nodes;

        private Template(List<Node> nodes) { this.nodes = List.copyOf(nodes); }

        public String render(Map<String, ?> arguments) {
            StringBuilder output = new StringBuilder();
            for (Node node : nodes) {
                if (node.argument() == null) output.append(node.literal());
                else {
                    Object value = arguments.get(node.argument());
                    if (value == null) throw invalid("Missing template argument");
                    Class<?> type = value.getClass();
                    if (!(value instanceof String) && type != Integer.class && type != Long.class
                            && type != Short.class && type != Byte.class && type != Double.class && type != Float.class
                            && type != java.math.BigDecimal.class && type != java.math.BigInteger.class)
                        throw invalid("Template arguments must be projected scalar data");
                    output.append(value);
                }
            }
            return output.toString();
        }
    }

    public record Selection(Template template, Locale locale) {
        public Selection { Objects.requireNonNull(template); Objects.requireNonNull(locale); }
        public String render(Map<String, ?> arguments) { return template.render(arguments); }
    }

    private record Messages(Map<String, Map<Shape, Template>> rules, Map<String, Template> patterns, String separator) {}
    private static final Set<String> CONSTRAINT_ARGS = Set.of("field", "system", "input", "input_len", "unit");
    private static final Set<String> CORE_RULES = Set.of("checker.required", "checker.min", "checker.max", "checker.minLength", "checker.maxLength", "checker.invalid");
    private static final CheckerMessageCatalog BUILTIN = loadBuiltin();
    private final Map<Locale, Messages> locales;
    private final boolean builtin;

    private CheckerMessageCatalog(Map<Locale, Messages> locales, boolean builtin) {
        this.locales = Map.copyOf(locales); this.builtin = builtin;
    }

    public static CheckerMessageCatalog builtin() { return BUILTIN; }

    /** Parse and validate completely before returning. A failed load cannot replace an active catalog. */
    public static CheckerMessageCatalog fromJson(InputStream input) { return load(input, false); }

    private static CheckerMessageCatalog load(InputStream input, boolean builtin) {
        Objects.requireNonNull(input, "input");
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
            mapper.enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
            JsonNode root = mapper.readTree(input);
            object(root, Set.of("schema", "defaultLocale", "locales"));
            if (!"teaql.checker-messages/v1".equals(text(root.get("schema"))) || !"en".equals(text(root.get("defaultLocale"))))
                throw invalid("Unsupported Checker message schema or default Locale");
            JsonNode entries = root.get("locales");
            if (entries == null || !entries.isObject() || entries.isEmpty()) throw invalid("Missing locales");
            Map<Locale, Messages> locales = new EnumMap<>(Locale.class);
            Iterator<Map.Entry<String, JsonNode>> iterator = entries.fields();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                Locale locale = Locale.fromCode(entry.getKey());
                if (locales.containsKey(locale)) throw invalid("Duplicate normalized Locale");
                JsonNode data = entry.getValue();
                object(data, Set.of("rules", "locationPatterns"));
                if (data.isEmpty()) throw invalid("Empty Locale entry");
                Map<String, Map<Shape, Template>> rules = new HashMap<>();
                JsonNode sourceRules = data.get("rules");
                if (sourceRules != null) {
                    if (!sourceRules.isObject() || sourceRules.isEmpty()) throw invalid("Empty rules");
                    var ruleIterator = sourceRules.fields();
                    while (ruleIterator.hasNext()) {
                        var rule = ruleIterator.next();
                        if (!rule.getKey().matches("checker\\.[A-Za-z][A-Za-z0-9_.-]*")) throw invalid("Invalid rule key");
                        object(rule.getValue(), Set.of("one", "two", "deep"));
                        if (rule.getValue().isEmpty()) throw invalid("Empty rule override");
                        Map<Shape, Template> shapes = new EnumMap<>(Shape.class);
                        for (Shape shape : Shape.values()) {
                            JsonNode source = rule.getValue().get(shape.name().toLowerCase(java.util.Locale.ROOT));
                            if (source == null) continue;
                            Set<String> allowed = new HashSet<>(CONSTRAINT_ARGS);
                            if (rule.getKey().equals("checker.required") || rule.getKey().equals("checker.invalid"))
                                allowed.retainAll(Set.of("field"));
                            if (rule.getKey().equals("checker.min") || rule.getKey().equals("checker.max"))
                                allowed.retainAll(Set.of("field", "system", "input"));
                            Set<String> required = new HashSet<>(Set.of("field"));
                            if (shape != Shape.ONE) {
                                String owner = shape == Shape.TWO ? "object" : "objectPath";
                                allowed.add(owner); required.add(owner);
                            }
                            if (rule.getKey().equals("checker.min") || rule.getKey().equals("checker.max")) required.add("system");
                            if (rule.getKey().equals("checker.minLength") || rule.getKey().equals("checker.maxLength")) {
                                required.addAll(Set.of("system", "input_len", "unit")); allowed.remove("input");
                            }
                            shapes.put(shape, compile(text(source), allowed, required));
                        }
                        rules.put(rule.getKey(), Map.copyOf(shapes));
                    }
                }
                Map<String, Template> patterns = new HashMap<>();
                String separator = null;
                JsonNode location = data.get("locationPatterns");
                if (location != null) {
                    object(location, Set.of("reference", "item", "separator"));
                    if (location.isEmpty()) throw invalid("Empty location override");
                    if (location.has("reference")) patterns.put("reference", compile(text(location.get("reference")), Set.of("relation"), Set.of("relation")));
                    if (location.has("item")) {
                        String source = text(location.get("item"));
                        Template compiled = compile(source, Set.of("relation", "position", "ordinal", "classifier"), Set.of("relation"));
                        if (compiled.nodes.stream().noneMatch(node -> "position".equals(node.argument()) || "ordinal".equals(node.argument())))
                            throw invalid("Collection pattern must identify position");
                        patterns.put("item", compiled);
                    }
                    if (location.has("separator")) {
                        separator = text(location.get("separator"));
                        if (separator.isEmpty() || separator.chars().anyMatch(c -> c == '{' || c == '}' || Character.isISOControl(c)))
                            throw invalid("Invalid location separator");
                    }
                }
                if (builtin && (!rules.keySet().equals(CORE_RULES)
                        || rules.values().stream().anyMatch(shapes -> shapes.size() != Shape.values().length)
                        || patterns.size() != 2 || separator == null)) throw invalid("Incomplete built-in Locale");
                locales.put(locale, new Messages(Map.copyOf(rules), Map.copyOf(patterns), separator));
            }
            if (builtin && locales.size() != Locale.values().length) throw invalid("Incomplete built-in Locale set");
            return new CheckerMessageCatalog(locales, builtin);
        } catch (IOException exception) {
            // Parser excerpts can contain a secret embedded in a malformed resource.
            throw invalid("Invalid Checker message JSON");
        }
    }

    /** Explicit override only: used to compose shape overrides ahead of legacy application text. */
    public Selection override(Locale locale, String rule, Shape shape) {
        if (builtin) return null;
        Template value = exact(locale, rule, shape);
        return value == null ? null : new Selection(value, locale);
    }

    /** Resolve template Locale first; callers must use it when rendering owner grammar. */
    public Selection select(Locale locale, String rule, Shape shape) {
        Objects.requireNonNull(locale); Objects.requireNonNull(rule); Objects.requireNonNull(shape);
        for (Locale candidate : locale == Locale.ENGLISH ? List.of(locale) : List.of(locale, Locale.ENGLISH)) {
            Template value = exact(candidate, rule, shape);
            if (value == null && !builtin) value = BUILTIN.exact(candidate, rule, shape);
            if (value != null) return new Selection(value, candidate);
        }
        Template generic = BUILTIN.exact(locale, "checker.invalid", shape);
        return new Selection(generic, locale);
    }

    public String reference(Locale locale, String relation) {
        return pattern(locale, "reference").render(Map.of("relation", relation));
    }

    public String item(Locale locale, String relation, int index, String classifier) {
        if (index < 0) throw invalid("Negative collection index");
        long position = (long) index + 1;
        return pattern(locale, "item").render(Map.of("relation", relation, "position", position,
                "ordinal", englishOrdinal(position), "classifier", classifier));
    }

    public String separator(Locale locale) {
        Messages entry = locales.get(locale);
        return entry != null && entry.separator() != null ? entry.separator() : BUILTIN.locales.get(locale).separator();
    }

    private Template exact(Locale locale, String rule, Shape shape) {
        Messages entry = locales.get(locale);
        return entry == null ? null : entry.rules().getOrDefault(rule, Map.of()).get(shape);
    }

    private Template pattern(Locale locale, String key) {
        Messages entry = locales.get(locale);
        Template value = entry == null ? null : entry.patterns().get(key);
        return value != null ? value : BUILTIN.locales.get(locale).patterns().get(key);
    }

    public static String englishOrdinal(long position) {
        if (position < 1) throw invalid("Position must be positive");
        long last = position % 100;
        String suffix = last >= 11 && last <= 13 ? "th" : switch ((int) (position % 10)) {
            case 1 -> "st"; case 2 -> "nd"; case 3 -> "rd"; default -> "th";
        };
        return position + suffix;
    }

    private static Template compile(String source, Set<String> allowed, Set<String> required) {
        if (source.isBlank()) throw invalid("Empty template");
        List<Node> nodes = new ArrayList<>(); Set<String> seen = new HashSet<>(); StringBuilder literal = new StringBuilder();
        for (int i = 0; i < source.length();) {
            char c = source.charAt(i);
            if ((c == '{' || c == '}') && i + 1 < source.length() && source.charAt(i + 1) == c) {
                literal.append(c); i += 2; continue;
            }
            if (c == '}') throw invalid("Unmatched closing brace");
            if (c != '{') { literal.append(c); i++; continue; }
            int end = source.indexOf('}', i + 1);
            if (end < 0) throw invalid("Unclosed placeholder");
            String name = source.substring(i + 1, end);
            if (!allowed.contains(name)) throw invalid("Unknown template argument");
            if (!literal.isEmpty()) { nodes.add(new Node(literal.toString(), null)); literal.setLength(0); }
            nodes.add(new Node(null, name)); seen.add(name); i = end + 1;
        }
        if (!literal.isEmpty()) nodes.add(new Node(literal.toString(), null));
        if (!seen.containsAll(required)) throw invalid("Missing required template arguments");
        return new Template(nodes);
    }

    private static void object(JsonNode node, Set<String> allowed) {
        if (node == null || !node.isObject()) throw invalid("Expected object");
        node.fieldNames().forEachRemaining(name -> { if (!allowed.contains(name)) throw invalid("Unknown catalog property"); });
    }

    private static String text(JsonNode node) {
        if (node == null || !node.isTextual()) throw invalid("Expected text");
        return node.textValue();
    }

    private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }

    private static CheckerMessageCatalog loadBuiltin() {
        try (InputStream input = CheckerMessageCatalog.class.getResourceAsStream("checker-messages-v1.json")) {
            if (input == null) throw new ExceptionInInitializerError("Missing natural Checker message catalog");
            return load(input, true);
        } catch (IOException exception) { throw new ExceptionInInitializerError("Cannot close natural Checker catalog"); }
    }
}
