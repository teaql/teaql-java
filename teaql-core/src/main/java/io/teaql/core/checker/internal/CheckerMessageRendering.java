package io.teaql.core.checker.internal;

import io.teaql.core.UserContext;
import io.teaql.core.checker.*;
import io.teaql.core.i18n.*;
import io.teaql.core.utils.SensitiveValueProjection;
import java.util.*;
import java.util.function.Consumer;

/** Connects already-projected Checker results to pure presentation, without data or clock access. */
public final class CheckerMessageRendering {
    private static final Set<String> WARNING_CODES = Set.of("CHECKER_MESSAGE_LABEL_FALLBACK",
            "CHECKER_MESSAGE_RENDER_FALLBACK", "CHECKER_MESSAGE_GENERIC_RULE");
    private CheckerMessageRendering() {}

    public static void bind(UserContext context, CheckResult result) {
        if (result.getPresentation() != null) return;
        var scope = CheckerInvocation.current(context);
        try {
            result.setPresentation(scope == null
                    ? CheckerPresentation.capture(context.checkerPresentationModel(), null, result.getLocation())
                    : scope.presentation(result.getLocation()));
        } catch (RuntimeException ignored) {
            warning(context, scope, new HashSet<>(), "CHECKER_MESSAGE_RENDER_FALLBACK");
            result.setPresentation(fallbackPresentation(result));
        }
    }

    private static CheckerPresentation fallbackPresentation(CheckResult result) {
        try { return CheckerPresentation.capture(CheckerPresentation.Model.empty(), null, result.getLocation()); }
        catch (RuntimeException ignored) { return CheckerPresentation.capture(CheckerPresentation.Model.empty(), null, null); }
    }

    private static void warning(UserContext context, CheckerInvocation scope, Set<String> localWarnings, String code) {
        String known = canonicalWarning(code);
        if (localWarnings.add(known) && (scope == null || scope.firstPresentationWarning(known))) {
            try { context.checkerMessageWarning(known); } catch (RuntimeException ignored) { /* Logging must not replace a Checker failure. */ }
        }
    }

    private static String canonicalWarning(String code) {
        return code != null && WARNING_CODES.contains(code) ? code : "CHECKER_MESSAGE_RENDER_FALLBACK";
    }

    private record Key(io.teaql.core.i18n.Locale locale, CheckerMessageCatalog.Shape shape) {}
    private record Plan(CheckerMessageCatalog.Selection selection, CheckerMessageCatalog.Template legacy,
                        io.teaql.core.i18n.Locale locale, boolean generic, boolean custom) {
        Plan project(java.util.function.UnaryOperator<String> projection) {
            return new Plan(custom ? new CheckerMessageCatalog.Selection(selection.template().projectLiterals(projection), selection.locale()) : selection,
                    legacy == null ? null : legacy.projectLiterals(projection), locale, generic, custom);
        }
    }

    /** Exactly one rule's 15 Locale x 3 shape plans, or a safe customer literal; never a callback. */
    static final class Plans {
        private final String rule;
        private final CheckerMessageCatalog catalog;
        private final Map<Key, Plan> entries;
        private final String literal;
        private Plans(String rule, CheckerMessageCatalog catalog, Map<Key, Plan> entries, String literal) {
            this.rule = rule; this.catalog = catalog; this.entries = Map.copyOf(entries); this.literal = literal;
        }
        private Plans project(List<String> secrets) {
            if (secrets.isEmpty()) return this;
            java.util.function.UnaryOperator<String> projection = text -> SensitiveValueProjection.scrub(text, secrets);
            if (literal != null) return new Plans(rule, CheckerMessageCatalog.builtin(), Map.of(), projection.apply(literal));
            Map<Key, Plan> projected = new HashMap<>(); entries.forEach((key, plan) -> projected.put(key, plan.project(projection)));
            return new Plans(rule, catalog.projectForRule(rule, projection), projected, null);
        }
        private Plan get(io.teaql.core.i18n.Locale locale, CheckerMessageCatalog.Shape shape) {
            return entries.get(new Key(locale, shape));
        }
    }

    record Rendered(String text, Plans plans) {}

    private static Plans prepare(UserContext context, String rule, List<String> secrets) {
        var catalog = context.checkerMessageCatalog().projectForRule(rule, text -> SensitiveValueProjection.scrub(text, secrets));
        var legacy = context.i18nCatalog();
        Map<Key, Plan> entries = new HashMap<>();
        for (var locale : io.teaql.core.i18n.Locale.values()) for (var shape : CheckerMessageCatalog.Shape.values()) {
            Plan plan = null;
            for (var candidate : locale == io.teaql.core.i18n.Locale.ENGLISH ? List.of(locale)
                    : List.of(locale, io.teaql.core.i18n.Locale.ENGLISH)) {
                var selection = catalog.override(candidate, rule, shape);
                if (selection != null) { plan = new Plan(selection, null, candidate, false, true); break; }
                String application = legacy.applicationMessage(candidate, rule);
                if (application != null) {
                    var template = CheckerMessageCatalog.legacyTemplate(application)
                            .projectLiterals(text -> SensitiveValueProjection.scrub(text, secrets));
                    plan = new Plan(null, template, candidate, false, false); break;
                }
                selection = CheckerMessageCatalog.builtin().lookup(candidate, rule, shape);
                if (selection != null) { plan = new Plan(selection, null, candidate, false, false); break; }
            }
            if (plan == null) plan = new Plan(CheckerMessageCatalog.builtin().select(locale, "checker.invalid", shape), null, locale, true, false);
            entries.put(new Key(locale, shape), plan);
        }
        return new Plans(rule, catalog, entries, null);
    }

    static Rendered render(UserContext context, CheckResult result, int validationLength,
                         List<String> diagnosticSecrets, Set<String> localWarnings) {
        Consumer<String> warn = code -> warning(context, CheckerInvocation.current(context), localWarnings, code);
        // Unsupported explicit Locales are still typed configuration errors, not a language fallback.
        io.teaql.core.i18n.Locale locale = Objects.requireNonNullElse(context.locale(), io.teaql.core.i18n.Locale.ENGLISH);
        try {
            bind(context, result);
            var presentation = result.getPresentation();
            String rule = I18nCatalog.messageKey(result);
            var previous = CheckerPresentationCache.plans(result);
            boolean initial = previous == null || !previous.rule.equals(rule);
            Plans plans = (initial ? prepare(context, rule, diagnosticSecrets) : previous).project(diagnosticSecrets);
            if (plans.literal != null) return new Rendered(plans.literal, plans);
            Plan plan = plans.get(locale, presentation.shape());
            if (plan.generic()) warn.accept("CHECKER_MESSAGE_GENERIC_RULE");
            Map<String, Object> arguments = arguments(result, validationLength, plan.locale());
            if (plan.legacy() != null) {
                arguments.put("location", canonicalLocation(result.getLocation()));
                arguments.putIfAbsent("input", "null"); arguments.putIfAbsent("system", "null");
                return new Rendered(plan.legacy().render(arguments), plans);
            }
            var renderer = initial ? context.checkerMessageRenderer() : CheckerMessageRenderer.STANDARD;
            if (renderer == CheckerMessageRenderer.STANDARD)
                return new Rendered(presentation.render(plans.catalog, plan.selection(), arguments, warn), plans);
            // The SPI may retain its input. Do not hand it a callback capturing UserContext or an invocation.
            Set<String> queuedWarnings = new LinkedHashSet<>();
            Consumer<String> queue = code -> queuedWarnings.add(canonicalWarning(code));
            String message;
            try { message = renderer.render(new CheckerMessageRenderer.Request(presentation, plans.catalog, plan.selection(), arguments, queue)); }
            finally { queuedWarnings.forEach(warn); }
            if (message == null || message.isBlank()) throw new IllegalArgumentException("Empty rendered Checker message");
            message = SensitiveValueProjection.scrub(message, diagnosticSecrets);
            return new Rendered(message, new Plans(rule, CheckerMessageCatalog.builtin(), Map.of(), message));
        } catch (io.teaql.core.i18n.UnsupportedLocaleException unsupported) { throw unsupported; }
        catch (RuntimeException failure) {
            // Do not disclose provider causes or lose the original rule and structured location.
            warn.accept("CHECKER_MESSAGE_RENDER_FALLBACK");
            var presentation = result.getPresentation();
            if (presentation == null) presentation = fallbackPresentation(result);
            // Failed customer callbacks are not retried during exception aggregation or relocation.
            Map<Key, Plan> generic = new HashMap<>();
            for (var candidate : io.teaql.core.i18n.Locale.values()) for (var shape : CheckerMessageCatalog.Shape.values())
                generic.put(new Key(candidate, shape), new Plan(CheckerMessageCatalog.builtin().select(candidate, "checker.invalid", shape), null, candidate, false, false));
            Plans plans = new Plans(I18nCatalog.messageKey(result), CheckerMessageCatalog.builtin(), generic, null);
            return new Rendered(presentation.render(plans.catalog, plans.get(locale, presentation.shape()).selection(), Map.of(), warn), plans);
        }
    }

    private static Map<String, Object> arguments(CheckResult result, int validationLength, io.teaql.core.i18n.Locale locale) {
        Map<String, Object> values = new HashMap<>();
        if (result.getSystemValue() != null) values.put("system", scalar(result.getSystemValue()));
        if (result.getInputValue() != null) values.put("input", scalar(result.getInputValue()));
        values.put("input_len", validationLength);
        // Java's existing String/CharSequence length constraints measure UTF-16 code units.
        values.put("unit", CheckerMessageDefaults.unit(locale, "utf16"));
        return values;
    }

    private static String canonicalLocation(ObjectLocation location) {
        Deque<ObjectLocation> steps = new ArrayDeque<>();
        for (var cursor = location; cursor != null; cursor = cursor.getParent()) steps.addFirst(cursor);
        StringBuilder path = new StringBuilder();
        for (var step : steps) {
            if (step instanceof HashLocation hash) {
                if (!path.isEmpty()) path.append('.'); path.append(hash.getMember());
            } else if (step instanceof ArrayLocation array) path.append('[').append(array.getIndex()).append(']');
        }
        return path.toString();
    }

    private static Object scalar(Object value) {
        if (value == null) return "null";
        Class<?> type = value.getClass();
        if (type == String.class || type == Integer.class || type == Long.class || type == Short.class
                || type == Byte.class || type == Double.class || type == Float.class
                || type == java.math.BigDecimal.class || type == java.math.BigInteger.class) return value;
        if (type == Boolean.class || type == java.time.LocalDate.class || type == java.time.LocalDateTime.class
                || type == java.time.Instant.class) return value.toString();
        return "[value omitted]";
    }
}
