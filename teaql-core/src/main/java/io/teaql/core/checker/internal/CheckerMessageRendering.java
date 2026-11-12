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

    static String render(UserContext context, CheckResult result, int validationLength,
                         List<String> diagnosticSecrets, Set<String> localWarnings) {
        Consumer<String> warn = code -> warning(context, CheckerInvocation.current(context), localWarnings, code);
        // Unsupported explicit Locales are still typed configuration errors, not a language fallback.
        io.teaql.core.i18n.Locale locale = Objects.requireNonNullElse(context.locale(), io.teaql.core.i18n.Locale.ENGLISH);
        try {
            bind(context, result);
            var presentation = result.getPresentation();
            var catalog = context.checkerMessageCatalog();
            String rule = I18nCatalog.messageKey(result);
            var legacy = context.i18nCatalog();
            CheckerMessageCatalog.Selection selection = null;
            for (var candidate : locale == io.teaql.core.i18n.Locale.ENGLISH ? List.of(locale)
                    : List.of(locale, io.teaql.core.i18n.Locale.ENGLISH)) {
                selection = catalog.override(candidate, rule, presentation.shape());
                if (selection != null) break;
                String application = legacy.applicationMessage(candidate, rule);
                if (application != null) return I18nCatalog.renderApplicationMessage(application, Map.of(
                        "location", canonicalLocation(result.getLocation()),
                        "system", String.valueOf(scalar(result.getSystemValue())),
                        "input", String.valueOf(scalar(result.getInputValue())),
                        "input_len", String.valueOf(validationLength)));
                selection = CheckerMessageCatalog.builtin().lookup(candidate, rule, presentation.shape());
                if (selection != null) break;
            }
            if (selection == null) {
                warn.accept("CHECKER_MESSAGE_GENERIC_RULE");
                selection = CheckerMessageCatalog.builtin().select(locale, "checker.invalid", presentation.shape());
            }
            Map<String, Object> arguments = arguments(result, validationLength, selection.locale());
            var renderer = context.checkerMessageRenderer();
            // The SPI may retain its input. Do not hand it a callback capturing UserContext or an invocation.
            Set<String> queuedWarnings = new LinkedHashSet<>();
            Consumer<String> queue = code -> queuedWarnings.add(canonicalWarning(code));
            String message;
            try { message = renderer.render(new CheckerMessageRenderer.Request(presentation, catalog, selection, arguments, queue)); }
            finally { queuedWarnings.forEach(warn); }
            if (message == null || message.isBlank()) throw new IllegalArgumentException("Empty rendered Checker message");
            return renderer == CheckerMessageRenderer.STANDARD ? message : SensitiveValueProjection.scrub(message, diagnosticSecrets);
        } catch (io.teaql.core.i18n.UnsupportedLocaleException unsupported) { throw unsupported; }
        catch (RuntimeException failure) {
            // Do not disclose provider causes or lose the original rule and structured location.
            warn.accept("CHECKER_MESSAGE_RENDER_FALLBACK");
            var presentation = result.getPresentation();
            if (presentation == null) presentation = fallbackPresentation(result);
            return presentation.render(CheckerMessageCatalog.builtin(), locale, "checker.invalid", Map.of(), warn);
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
