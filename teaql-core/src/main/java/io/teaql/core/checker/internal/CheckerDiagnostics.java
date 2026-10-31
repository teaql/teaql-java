package io.teaql.core.checker.internal;

import io.teaql.core.UserContext;
import io.teaql.core.checker.*;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.utils.NamingCase;
import io.teaql.core.utils.SensitiveLogNames;
import io.teaql.core.utils.SensitiveValueProjection;
import java.util.*;

/** Framework output projection. Never modifies an entity or stores raw input in an error. */
public final class CheckerDiagnostics {
    private CheckerDiagnostics() {}

    public static void translate(UserContext context, List<CheckResult> results) {
        translate(context,results,false);
    }

    /** Keep the existing per-result customer localization hook in the batch API. */
    public static void translateBatch(UserContext context, List<CheckResult> results) {
        translate(context,results,true);
    }

    private static void translate(UserContext context, List<CheckResult> results, boolean invokeHook) {
        var scope=CheckerInvocation.current(context);
        List<String> secrets=scope == null ? new ArrayList<>() : scope.diagnosticSecrets();
        boolean allow=SensitiveValueProjection.plaintextEnabled();
        int[] lengths=new int[results.size()];
        boolean[] automatic=new boolean[results.size()];
        for (int i=0;i<results.size();i++) {
            var result=results.get(i); Object raw=result.getInputValue();
            Integer previousLength=CheckerPresentationCache.validationLength(result);
            automatic[i]=previousLength != null;
            lengths[i]=previousLength != null ? previousLength : raw instanceof CharSequence cs ? cs.length() : 0;
            boolean credential=false;
            for (ObjectLocation location=result.getLocation();location!=null;location=location.getParent())
                if (location instanceof HashLocation hash && SensitiveLogNames.credential(hash.getMember())) credential=true;
            result.setInputValue(project(raw,credential || SensitiveValueProjection.hasCredentials(raw),
                !allow && modelMasked(context,result),secrets));
            Object expected=result.getSystemValue();
            result.setSystemValue(project(expected,SensitiveValueProjection.hasCredentials(expected)
                || (credential && expected != null && !(expected instanceof Number)),false,secrets));
        }
        for (int i=0;i<results.size();i++) {
            var result=results.get(i); String message=result.getNaturalLanguageStatement();
            if (message == null || message.isBlank() || automatic[i]) {
                message=context.i18nCatalog().render(result,context.locale(),lengths[i]);
                result.setNaturalLanguageStatement(message);
                CheckerPresentationCache.remember(result,lengths[i]);
            } else {
                CheckerPresentationCache.forget(result);
                result.setNaturalLanguageStatement(SensitiveValueProjection.scrub(message,secrets));
            }
            // Rendering already uses safe values. Scrubbing a generated message
            // again could corrupt its numeric bound/length when a secret is "1".
        }
        // An earlier callback can mention a field whose violation is discovered
        // later. Redact it before the graph's exception can escape.
        if (scope != null && scope.attribute(Checker.TEAQL_DATA_CHECK_RESULT) instanceof List<?> previous)
            for (Object value : previous) if (value instanceof CheckResult result && !CheckerPresentationCache.isAutomatic(result))
                result.setNaturalLanguageStatement(SensitiveValueProjection.scrub(result.getNaturalLanguageStatement(),secrets));
        if (invokeHook) for (var result : results) {
            String before=result.getNaturalLanguageStatement();
            context.translateCheckResult(result);
            if (!Objects.equals(before,result.getNaturalLanguageStatement())) {
                CheckerPresentationCache.forget(result);
                result.setNaturalLanguageStatement(SensitiveValueProjection.scrub(result.getNaturalLanguageStatement(),secrets));
            }
            // Customer hooks may replace values too. Apply the output boundary
            // once more without recursively invoking the customer hook.
            translate(context,List.of(result),false);
        }
    }

    private static Object project(Object value, boolean credential, boolean masked, List<String> secrets) {
        if (value == null || (!credential && !masked)) return value;
        Object safe=credential ? SensitiveValueProjection.REDACTED : SensitiveValueProjection.maskBusinessValue(value);
        if (!Objects.equals(value,safe)) SensitiveValueProjection.collectStrings(value,secrets);
        return safe;
    }
    private static boolean modelMasked(UserContext context, CheckResult result) {
        ObjectLocation location=result.getLocation();
        while (location != null && !(location instanceof HashLocation)) location=location.getParent();
        if (!(location instanceof HashLocation hash)) return false;
        String field=hash.getMember(); if (field.startsWith("#")) return true;
        if (result.getInputValue() == null || result.getRootType() == null) return false;
        EntityMetaFactory factory=context.capability(EntityMetaFactory.class); if (factory == null) return false;
        // Do not consult the process-global factory or derive entity identity from a display path.
        var descriptor=factory.allEntityDescriptors().stream()
            .filter(d -> result.getRootType().equals(d.getType())).findFirst().orElse(null);
        for (var current=descriptor;current!=null;current=current.getParent())
            for (String masked : current.getAuditMaskFields())
                if (field.equals(masked) || field.equals(NamingCase.toUnderlineCase(masked))) return true;
        return false;
    }
}
