package io.teaql.runtime;

import static org.junit.Assert.*;
import io.teaql.core.BaseEntity;
import io.teaql.core.UserContext;
import io.teaql.core.checker.*;
import io.teaql.core.checker.internal.CheckerInvocation;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/** Real context Checker boundaries, without a database or generated model. */
public class CheckerDiagnosticPrivacyTest {
    private static final String SECRET = "private-contact-314159";
    @Rule public TestWatcher receipt = new TestWatcher() {
        @Override protected void succeeded(Description d) { System.out.println("JAVA_CHECKER_PRIVACY_CASE=" + d.getMethodName() + ":passed"); }
        @Override protected void failed(Throwable e, Description d) { System.out.println("JAVA_CHECKER_PRIVACY_CASE=" + d.getMethodName() + ":failed"); }
    };
    private static UserContext context() {
        var descriptor = new EntityDescriptor(); descriptor.setType("OrderItem");
        descriptor.setAuditMaskFields(List.of("contactPhone"));
        var metadata = new SimpleEntityMetaFactory(); metadata.register(descriptor);
        return new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).build());
    }
    private static Checker<BaseEntity> checker() {
        return new Checker<>() {
            public String type() { return "OrderItem"; }
            public void checkAndFix(UserContext context, BaseEntity entity, ObjectLocation location) {}
        };
    }
    private static CheckResult invalid(String field, Object value) {
        var result = CheckResult.invalidType(ObjectLocation.hashRoot(field), "Invalid supplied value");
        result.setInputValue(value); result.setRootType("OrderItem"); return result;
    }
    @SuppressWarnings("unchecked") private static List<CheckResult> results(UserContext context) {
        return (List<CheckResult>) CheckerInvocation.attribute(context, Checker.TEAQL_DATA_CHECK_RESULT);
    }
    @Test public void modelMaskProtectsWireExceptionAndToString() throws Exception {
        var context = context(); var result = invalid("contact_phone", SECRET);
        checker().appendResult(context, result);
        assertEquals("pr******************59", result.getInputValue());
        for (String text : List.of(result.toString(), new CheckException(List.of(result)).toString(),
                new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result.toWire(JsonFieldNamingProfile.CAMEL_CASE))))
            assertFalse(text, text.contains(SECRET));
    }
    @Test public void credentialsAndStructuredCredentialsNeedNoDeclaredMask() {
        var context = context();
        for (var result : List.of(invalid("password", SECRET), invalid("profile", Map.of("access_token", SECRET)))) {
            checker().appendResult(context, result); assertEquals("[REDACTED]", result.getInputValue());
        }
    }
    @Test public void originalValidationLengthSurvivesCredentialRedactionInBothLocales() {
        var context = context(); String input = "密钥🔑abc";
        for (String locale : List.of("en-US", "zh-CN")) {
            context.setLocaleCode(locale);
            var result = CheckResult.minStr(ObjectLocation.hashRoot("password"), 12, input);
            checker().appendResult(context, result);
            assertEquals("[REDACTED]", result.getInputValue());
            // Preserve the existing Java checker unit (UTF-16), not the mask length.
            assertTrue(result.getNaturalLanguageStatement(), result.getNaturalLanguageStatement().contains("7"));
            assertFalse(result.getNaturalLanguageStatement().contains(input));
            assertEquals(12, result.getSystemValue());
        }
    }
    @Test public void nestedPathsAliasesAndProfilesAreUnchanged() {
        var context = context(); var result = invalid("contact_phone", SECRET);
        var location = ObjectLocation.hashRoot("order_item_list").element(2).member("contact_phone");
        result.setLocation(location); result.setSourceInstancePath("/items/2/legacy~0phone");
        checker().appendResult(context, result);
        assertNotEquals(SECRET, result.getInputValue()); assertSame(location, result.getLocation());
        assertEquals("OrderItem", result.getRootType());
        assertEquals("/orderItemList/2/contactPhone", result.toWire(JsonFieldNamingProfile.CAMEL_CASE).instancePath());
        assertEquals("/order_item_list/2/contact_phone", result.toWire(JsonFieldNamingProfile.SNAKE_CASE).instancePath());
        assertEquals("/OrderItemList/2/ContactPhone", result.toWire(JsonFieldNamingProfile.PASCAL_CASE).instancePath());
        assertEquals("/items/2/legacy~0phone", result.getSourceInstancePath());
    }
    @Test public void ordinaryValuesBoundsAndCustomerExplanationRemainIntact() {
        var context = context(); var numeric = CheckResult.min(ObjectLocation.hashRoot("quantity"), 2, 1);
        checker().appendResult(context, numeric);
        assertEquals(1, numeric.getInputValue()); assertEquals(2, numeric.getSystemValue());
        var custom = invalid("name", "ordinary"); custom.setNaturalLanguageStatement("customer-owned explanation");
        checker().appendResult(context, custom);
        assertEquals("ordinary", custom.getInputValue());
        assertEquals("customer-owned explanation", custom.getNaturalLanguageStatement());
    }
    @Test public void numericShortUnicodeAndNullUseTheExistingAuditAlgorithm() {
        var context = context();
        for (Object[] pair : new Object[][] {{12345L,"*****"},{"短🔑文","***"},{null,null}}) {
            var result = invalid("contact_phone", pair[0]); checker().appendResult(context, result);
            assertEquals(pair[1], result.getInputValue());
        }
    }
    @Test public void dynamicFieldIsMaskedWithoutChangingItsIdentity() {
        var result = invalid("#extra", SECRET); checker().appendResult(context(), result);
        assertNotEquals(SECRET, result.getInputValue()); assertEquals("#extra", result.getLocation().modelPath());
    }
    @Test public void maskedCollectionsDoNotMutateTheInputAndAreIdempotent() {
        var source = new ArrayList<>(List.of(SECRET, "short"));
        var context = context(); var result = invalid("contact_phone", source);
        checker().appendResult(context, result);
        assertEquals(List.of(SECRET, "short"), source);
        assertFalse(String.valueOf(result.getInputValue()).contains(SECRET));
        Object safe = result.getInputValue(); String message = result.getNaturalLanguageStatement();
        context.translateCheckResult(result);
        assertEquals(safe, result.getInputValue()); assertEquals(message, result.getNaturalLanguageStatement());
    }
    @Test public void batchTranslationScrubsEarlierAndLaterCustomMessages() {
        var a = invalid("name", "ordinary"); a.setNaturalLanguageStatement("Please fix " + SECRET + " first");
        var b = invalid("contact_phone", SECRET); b.setNaturalLanguageStatement("Contact " + SECRET + " was rejected");
        context().translateCheckResults(List.of(a,b));
        assertEquals("Please fix [REDACTED] first", a.getNaturalLanguageStatement());
        assertEquals("Contact [REDACTED] was rejected", b.getNaturalLanguageStatement());
    }
    @Test public void invocationScrubsSiblingMessagesRegardlessOfRuleOrder() {
        for (boolean sensitiveFirst : List.of(true,false)) {
            var context = context();
            try (var invocation = CheckerInvocation.open(context)) {
                var a = invalid("name", "ordinary"); a.setNaturalLanguageStatement("Please fix " + SECRET + " first");
                var b = invalid("contact_phone", SECRET);
                checker().appendResult(context, sensitiveFirst ? b : a);
                checker().appendResult(context, sensitiveFirst ? a : b);
                assertEquals("Please fix [REDACTED] first", a.getNaturalLanguageStatement());
            }
        }
    }
    @Test public void nestedIndependentInvocationDoesNotBorrowOuterSecrets() {
        var context = context();
        try (var outer = CheckerInvocation.open(context)) {
            checker().appendResult(context, invalid("contact_phone", SECRET));
            try (var inner = CheckerInvocation.open(context)) {
                var result = invalid("name", "ordinary"); result.setNaturalLanguageStatement("Separate " + SECRET);
                checker().appendResult(context, result); assertEquals("Separate " + SECRET, result.getNaturalLanguageStatement());
            }
            var result = invalid("name", "ordinary"); result.setNaturalLanguageStatement("Outer " + SECRET);
            checker().appendResult(context, result); assertEquals("Outer [REDACTED]", result.getNaturalLanguageStatement());
        }
        var outside = invalid("name", "ordinary"); outside.setNaturalLanguageStatement("Outside " + SECRET);
        checker().appendResult(context, outside); assertEquals("Outside " + SECRET, outside.getNaturalLanguageStatement());
    }
    @Test public void invalidTypeHelperCannotRestoreTheRawMessageAfterTranslation() {
        var context = context();
        try (var invocation = CheckerInvocation.open(context)) {
            checker().appendResult(context, invalid("contact_phone", SECRET));
            checker().invalidTypeCheck(context, ObjectLocation.hashRoot("name"), "Bad input " + SECRET);
            assertEquals("Bad input [REDACTED]", results(context).get(1).getNaturalLanguageStatement());
        }
    }
    @Test public void credentialsInExpectedConstraintsAreRedactedButNumericBoundsRemain() {
        var context = context(); var result = invalid("password", SECRET);
        result.setSystemValue("private-expected-password"); checker().appendResult(context, result);
        assertEquals("[REDACTED]", result.getSystemValue());
        var numeric = CheckResult.minStr(ObjectLocation.hashRoot("password"), 12, SECRET);
        checker().appendResult(context,numeric); assertEquals(12,numeric.getSystemValue());
    }
    @Test public void credentialAncestorIsProtectedWithoutRewritingThePath() {
        var result = invalid("value", SECRET);
        result.setLocation(ObjectLocation.hashRoot("credentials").member("password").member("value"));
        checker().appendResult(context(), result); assertEquals("[REDACTED]",result.getInputValue());
        assertEquals("credentials.password.value",result.getLocation().modelPath());
    }
    @Test public void shortCredentialCannotCorruptGeneratedNumericBoundsOrLengths() {
        var context = context();
        try (var invocation = CheckerInvocation.open(context)) {
            var result = CheckResult.minStr(ObjectLocation.hashRoot("password"),12,"1");
            checker().appendResult(context,result);
            String message = result.getNaturalLanguageStatement();
            assertTrue(message,message.contains("12")); assertTrue(message,message.endsWith("is 1"));
            checker().appendResult(context,invalid("password","2"));
            assertEquals(message,result.getNaturalLanguageStatement());
        }
    }
    @Test public void batchKeepsTheCustomerLocalizationHookWithoutLettingItRestoreSecrets() {
        var delegate=context(); int[] calls={0};
        UserContext customized=(UserContext)java.lang.reflect.Proxy.newProxyInstance(
            UserContext.class.getClassLoader(),new Class<?>[]{UserContext.class},(proxy,method,args) -> {
                if (method.getName().equals("translateCheckResults"))
                    return java.lang.reflect.InvocationHandler.invokeDefault(proxy,method,args);
                if (method.getName().equals("translateCheckResult")) {
                    calls[0]++; var result=(CheckResult)args[0];
                    assertNotEquals(SECRET,result.getInputValue());
                    result.setInputValue(SECRET); result.setNaturalLanguageStatement("Custom locale: "+SECRET);
                    return result;
                }
                try { return method.invoke(delegate,args); }
                catch (java.lang.reflect.InvocationTargetException error) { throw error.getCause(); }
            });
        var result=invalid("contact_phone",SECRET);
        customized.translateCheckResults(List.of(result));
        assertEquals(1,calls[0]); assertNotEquals(SECRET,result.getInputValue());
        assertEquals("Custom locale: [REDACTED]",result.getNaturalLanguageStatement());
    }
    @Test public void debugSwitchMustMatchExactlyAndNeverExposeCredentials() throws Exception {
        for (String value : List.of("ABSENT", "true", LogPrivacy.ACKNOWLEDGEMENT)) {
            var process = new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),
                "-ea","-cp",childClasspath(),
                CheckerDiagnosticPrivacyTest.class.getName(), value.equals(LogPrivacy.ACKNOWLEDGEMENT)?"debug":"safe");
            process.environment().remove(LogPrivacy.ENVIRONMENT);
            if (!value.equals("ABSENT")) process.environment().put(LogPrivacy.ENVIRONMENT,value);
            var output = Files.createTempFile("checker-privacy-child-", ".log"); Process child = null;
            try {
                child = process.redirectErrorStream(true).redirectOutput(output.toFile()).start();
                assertTrue("child timed out",child.waitFor(30,TimeUnit.SECONDS));
                assertEquals(Files.readString(output),0,child.exitValue());
            } finally {
                if (child != null && child.isAlive()) { child.destroyForcibly(); child.waitFor(); }
                Files.deleteIfExists(output);
            }
        }
    }
    private static String childClasspath() throws Exception {
        var paths = new ArrayList<String>();
        paths.add(System.getProperty("surefire.test.class.path",System.getProperty("java.class.path")));
        for (Class<?> type : List.of(CheckerDiagnosticPrivacyTest.class, LogPrivacy.class, BaseEntity.class,
                io.teaql.core.utils.SensitiveLogNames.class,io.teaql.data.dynamic.DynamicFieldsFacade.class))
            paths.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
        return String.join(java.io.File.pathSeparator,paths);
    }
    public static void main(String[] args) {
        var context=context();var business=invalid("contact_phone",SECRET);
        checker().appendResult(context,business);assertEquals(args[0].equals("debug"),SECRET.equals(business.getInputValue()));
        var password=invalid("password","private-auth-271828");password.setSystemValue("expected-auth-271828");
        checker().appendResult(context,password);assertEquals("[REDACTED]",password.getInputValue());
        assertEquals("[REDACTED]",password.getSystemValue());
    }
}
