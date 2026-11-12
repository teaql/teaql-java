package io.teaql.core.checker;

import io.teaql.core.i18n.CheckerMessageCatalog;
import io.teaql.core.i18n.Locale;
import java.util.*;
import java.util.function.Consumer;

/** Immutable model-aware presentation data; holds neither an entity graph nor a UserContext. */
public final class CheckerPresentation {
    public record Field(String name, String targetType, Map<Locale, String> labels,
                        Map<Locale, String> classifiers, Set<String> aliases) {
        public Field {
            Objects.requireNonNull(name); labels = Map.copyOf(labels);
            classifiers = Map.copyOf(classifiers); aliases = Set.copyOf(aliases);
            if (name.isBlank()) throw new IllegalArgumentException("Empty model field identity");
        }
    }

    public static final class Entity {
        private final Map<Locale, String> labels;
        private final Map<String, Field> fields;

        public Entity(Map<Locale, String> labels, List<Field> fields) {
            this.labels = Map.copyOf(labels);
            Map<String, Field> indexed = new HashMap<>();
            for (Field field : fields) {
                bind(indexed, field.name(), field);
                for (String alias : field.aliases()) bind(indexed, alias, field);
            }
            this.fields = Map.copyOf(indexed);
        }

        private static void bind(Map<String, Field> indexed, String key, Field field) {
            if (key.isBlank() || indexed.putIfAbsent(key, field) != null)
                throw new IllegalArgumentException("Duplicate model field identity or alias");
        }
    }

    /** One shared immutable index per generated model revision. */
    public record Model(String revision, Map<String, Entity> entities, Map<Locale, String> currentObject) {
        private static final Model EMPTY = new Model("unconfigured", Map.of(), io.teaql.core.i18n.CheckerMessageDefaults.currentObjectLabels());
        public Model {
            Objects.requireNonNull(revision); entities = Map.copyOf(entities); currentObject = Map.copyOf(currentObject);
            if (!currentObject.containsKey(Locale.ENGLISH)) throw new IllegalArgumentException("Missing English current-object label");
        }
        public static Model empty() { return EMPTY; }

        /** Compose passive generated modules once; conflicting identities require an explicit model decision. */
        public Model merge(Model other) {
            Objects.requireNonNull(other);
            if (this == EMPTY) return other;
            if (other == EMPTY || this == other) return this;
            Map<String, Entity> combined = new HashMap<>(entities);
            Map<Locale, String> labels = new EnumMap<>(Locale.class); labels.putAll(currentObject);
            boolean changed = false;
            for (var entry : other.entities.entrySet()) {
                Entity previous = combined.putIfAbsent(entry.getKey(), entry.getValue());
                if (previous == null) changed = true;
                else if (!previous.labels.equals(entry.getValue().labels) || !previous.fields.equals(entry.getValue().fields))
                    throw new IllegalArgumentException("Conflicting Checker presentation entity metadata");
            }
            for (var entry : other.currentObject.entrySet()) {
                String previous = labels.putIfAbsent(entry.getKey(), entry.getValue());
                if (previous == null) changed = true;
                else if (!previous.equals(entry.getValue())) throw new IllegalArgumentException("Conflicting Checker current-object vocabulary");
            }
            return changed ? new Model(revision + "+" + other.revision, combined, labels) : this;
        }
    }

    private record Step(String identity, Field field, List<Integer> indices) {
        private Step { indices = List.copyOf(indices); }
    }

    private final Model model;
    private final String rootType;
    private final List<Step> steps;
    private final CheckerMessageCatalog.Shape shape;

    private CheckerPresentation(Model model, String rootType, List<Step> steps, CheckerMessageCatalog.Shape shape) {
        this.model = model; this.rootType = rootType; this.steps = List.copyOf(steps); this.shape = shape;
    }

    /** Resolve only explicit descriptor identities/aliases; never use the violation's declaring type as a guessed root. */
    public static CheckerPresentation capture(Model model, String rootType, ObjectLocation location) {
        Objects.requireNonNull(model);
        Entity entity = rootType == null ? null : model.entities().get(rootType);
        boolean unusual = entity == null;
        List<Step> steps = new ArrayList<>();
        String name = null; Field descriptor = null; List<Integer> indices = new ArrayList<>();
        Deque<WireLocationSegment> segments = new ArrayDeque<>();
        for (ObjectLocation cursor = location; cursor != null; cursor = cursor.getParent()) {
            if (cursor instanceof HashLocation hash) segments.addFirst(WireLocationSegment.property(hash.getMember()));
            else if (cursor instanceof ArrayLocation array) segments.addFirst(WireLocationSegment.index(array.getIndex()));
            else if (cursor.getClass() == ObjectLocation.class && cursor.getParent() == null) { /* root anchor, not a property */ }
            else throw new IllegalArgumentException("Unsupported location segment");
        }
        for (WireLocationSegment segment : segments) {
            if ("property".equals(segment.kind())) {
                if (name != null || !indices.isEmpty()) steps.add(new Step(name, descriptor, indices));
                name = segment.name(); indices = new ArrayList<>();
                descriptor = entity == null ? null : entity.fields.get(name);
                entity = descriptor == null || descriptor.targetType() == null ? null : model.entities().get(descriptor.targetType());
            } else {
                if (segment.index() == null || segment.index() < 0) throw new IllegalArgumentException("Invalid location index");
                indices.add(segment.index());
                if (name == null || indices.size() > 1) unusual = true;
            }
        }
        if (name != null || !indices.isEmpty()) steps.add(new Step(name, descriptor, indices));
        if (steps.isEmpty() || !steps.get(steps.size() - 1).indices().isEmpty()) unusual = true;
        var shape = unusual || steps.size() > 2 ? CheckerMessageCatalog.Shape.DEEP
                : steps.size() == 1 ? CheckerMessageCatalog.Shape.ONE : CheckerMessageCatalog.Shape.TWO;
        return new CheckerPresentation(model, rootType, steps, shape);
    }

    public CheckerMessageCatalog.Shape shape() { return shape; }
    public String modelRevision() { return model.revision(); }

    /** A caller relocating a violation keeps its captured origin, even after the invocation has closed. */
    public CheckerPresentation relocate(ObjectLocation location) { return capture(model, rootType, location); }

    /** Pure O(path length) rendering. Constraint arguments must already have passed diagnostic projection. */
    public String render(CheckerMessageCatalog catalog, Locale locale, String rule, Map<String, ?> safeArguments,
                         Consumer<String> warningSink) {
        Objects.requireNonNull(locale);
        return render(catalog, catalog.select(locale, rule, shape), safeArguments, warningSink);
    }

    /** Render with a previously resolved template so grammar and constraint units use that Locale. */
    public String render(CheckerMessageCatalog catalog, CheckerMessageCatalog.Selection selection,
                         Map<String, ?> safeArguments, Consumer<String> warningSink) {
        Objects.requireNonNull(catalog); Objects.requireNonNull(selection); Objects.requireNonNull(warningSink);
        Set<String> warnings = new HashSet<>();
        Consumer<String> warn = code -> {
            if (warnings.add(code)) try { warningSink.accept(code); }
            catch (RuntimeException ignored) { /* A quality-warning adapter must not replace the validation failure. */ }
        };
        Locale effective = selection.locale();
        Map<String, Object> args = new HashMap<>();
        for (String key : List.of("system", "input", "input_len", "unit"))
            if (safeArguments.containsKey(key)) args.put(key, safeArguments.get(key));
        String root = rootLabel(effective, warn);
        Step leaf = steps.isEmpty() ? null : steps.get(steps.size() - 1);
        args.put("field", leaf == null || leaf.identity() == null ? root : label(leaf, effective, warn));
        List<String> owners = new ArrayList<>();
        int ownerCount = steps.size() - (leaf != null && leaf.identity() != null && leaf.indices().isEmpty() ? 1 : 0);
        for (int i = 0; i < ownerCount; i++) {
            Step step = steps.get(i);
            String relation = step.identity() == null ? root : label(step, effective, warn);
            if (step.indices().isEmpty()) owners.add(catalog.reference(effective, relation));
            else for (int j = 0; j < step.indices().size(); j++) {
                // Multiple selectors stay separate and visible rather than losing an index.
                String itemLabel = j == 0 ? relation : currentLabel(effective, warn);
                String classifier = step.field() == null ? defaultClassifier(effective)
                        : step.field().classifiers().getOrDefault(effective, defaultClassifier(effective));
                owners.add(catalog.item(effective, itemLabel, step.indices().get(j), sanitize(classifier)));
            }
        }
        args.put("object", owners.isEmpty() ? root : owners.get(0));
        args.put("objectPath", owners.isEmpty() ? root : String.join(catalog.separator(effective), owners));
        return selection.render(args);
    }

    private String rootLabel(Locale locale, Consumer<String> warn) {
        Entity entity = rootType == null ? null : model.entities().get(rootType);
        return entity == null ? currentLabel(locale, warn) : vocabulary(entity.labels, locale, rootType, warn);
    }

    private String currentLabel(Locale locale, Consumer<String> warn) {
        return vocabulary(model.currentObject(), locale, "current object", warn);
    }

    private static String label(Step step, Locale locale, Consumer<String> warn) {
        return vocabulary(step.field() == null ? Map.of() : step.field().labels(), locale,
                step.field() == null ? step.identity() : step.field().name(), warn);
    }

    private static String vocabulary(Map<Locale, String> labels, Locale locale, String identity, Consumer<String> warn) {
        String label = labels.get(locale);
        if (label == null || label.isBlank()) {
            warn.accept("CHECKER_MESSAGE_LABEL_FALLBACK");
            label = labels.get(Locale.ENGLISH);
        }
        if (label == null || label.isBlank()) label = identity == null ? "current object" : identity.replace('_', ' ');
        String safe = sanitize(label);
        if (safe.isBlank()) {
            warn.accept("CHECKER_MESSAGE_LABEL_FALLBACK");
            safe = sanitize(identity == null ? "current object" : identity.replace('_', ' '));
            if (safe.isBlank()) safe = "current object";
        }
        return safe;
    }

    private static String sanitize(String label) {
        // Control characters and bidi overrides cannot forge a path or a new diagnostic line.
        StringBuilder safe = new StringBuilder();
        label.codePoints().forEach(c -> safe.appendCodePoint(Character.isISOControl(c) || Character.getType(c) == Character.FORMAT ? ' ' : c));
        return safe.toString().strip();
    }

    private static String defaultClassifier(Locale locale) {
        return locale == Locale.CHINESE_SIMPLIFIED || locale == Locale.CHINESE_TRADITIONAL ? "项" : "";
    }
}
