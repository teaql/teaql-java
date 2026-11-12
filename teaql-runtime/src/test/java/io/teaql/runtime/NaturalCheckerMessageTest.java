package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.checker.*;
import io.teaql.core.checker.internal.CheckerInvocation;
import io.teaql.core.i18n.CheckerMessageCatalog;
import io.teaql.core.i18n.CheckerMessageDefaults;
import io.teaql.core.i18n.I18nCatalog;
import io.teaql.core.i18n.Locale;
import io.teaql.core.meta.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real Checker/Context entry points with one tiny metadata graph and an existing observed save fixture. */
public class NaturalCheckerMessageTest {
    private static final String SECRET = "密钥🔑abc"; // 7 UTF-16 units, 6 Unicode scalar values.
    private static final class NamedEntity extends BaseEntity {
        private final String type;
        NamedEntity(String type) { this.type = type; }
        @Override public String typeName() { return type; }
    }
    private static Map<Locale, String> names(String english, String chinese) {
        return Map.of(Locale.ENGLISH, english, Locale.CHINESE_SIMPLIFIED, chinese);
    }
    private static CheckerPresentation.Field scalar(String field, String english, String chinese) {
        return new CheckerPresentation.Field(field, null, names(english, chinese), Map.of(), Set.of());
    }
    private static CheckerPresentation.Field relation(String field, String target, String english, String chinese) {
        return new CheckerPresentation.Field(field, target, names(english, chinese), Map.of(Locale.CHINESE_SIMPLIFIED, "条"), Set.of());
    }
    private static CheckerPresentation.Model model(String revision, String nameLabel) {
        var fields = List.of(scalar("name", nameLabel, "名称"), scalar("amount", "amount", "金额"),
                scalar("password", "password", "密码"), scalar("description", "description", "描述"),
                relation("entry_list", "Entry", "entry", "明细"));
        return new CheckerPresentation.Model(revision, Map.of(
                "Catalog", new CheckerPresentation.Entity(names("catalog", "目录"), List.of(
                        scalar("name", nameLabel, "名称"), relation("entry_list", "Entry", "entry", "明细"),
                        relation("payer", "Entry", "payer", "付款人"), relation("beneficiary", "Entry", "beneficiary", "收款人"))),
                "Entry", new CheckerPresentation.Entity(names("entry", "明细"), fields)), CheckerMessageDefaults.currentObjectLabels());
    }
    private static DefaultUserContext context() { return context(null); }
    private static DefaultUserContext context(List<String> warnings) {
        var metadata = new SimpleEntityMetaFactory();
        for (String type : List.of("Catalog", "Entry")) {
            var descriptor = new EntityDescriptor(); descriptor.setType(type);
            descriptor.setAuditMaskFields(List.of("description")); metadata.register(descriptor);
        }
        var context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).executionLogging(false).build()) {
            @Override public void checkerMessageWarning(String code) {
                if (warnings != null) warnings.add(code); else super.checkerMessageWarning(code);
            }
        };
        context.installCheckerPresentationModel(model("v1", "name"));
        return context;
    }
    private static Checker<NamedEntity> checker(String type, BiConsumer<UserContext, ObjectLocation> checks) {
        return new Checker<>() {
            public String type() { return type; }
            public void checkAndFix(UserContext context, NamedEntity entity, ObjectLocation location) { checks.accept(context, location); }
        };
    }
    private static Checker<NamedEntity> entry() { return checker("Entry", (context, location) -> {}); }
    private static CheckException run(UserContext context, BiConsumer<UserContext, ObjectLocation> work) {
        return assertThrows(CheckException.class, () -> checker("Catalog", work).checkAndFix(context, new NamedEntity("Catalog")));
    }
    private static ObjectLocation name() { return ObjectLocation.hashRoot("entry_list").element(1).member("name"); }
    private static CheckerMessageCatalog catalog(String rules) {
        String json = "{\"schema\":\"teaql.checker-messages/v1\",\"defaultLocale\":\"en\",\"locales\":{\"en\":{\"rules\":" + rules + "}}}";
        return CheckerMessageCatalog.fromJson(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }
    private static void legacy(UserContext context, String rules) {
        String json = "{\"schema\":\"teaql.i18n/v1\",\"defaultLocale\":\"en\",\"locales\":{\"en\":{\"messages\":" + rules + "}}}";
        context.installI18nCatalog(I18nCatalog.fromJson(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))));
    }

    @Test public void defaultIsEnglishUnderNonEnglishHostWithoutACustomMessageCatalog() {
        java.util.Locale previous = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.JAPANESE);
            var context = context();
            var error = run(context, (ctx, location) -> entry().requiredCheck(ctx, ObjectLocation.hashRoot("name"), null));
            assertEquals("The name is required.", error.getMessage()); assertEquals(Locale.ENGLISH, context.locale());
        } finally { java.util.Locale.setDefault(previous); }
    }

    @Test public void actualEntryCoversAllFiveConstraintsAndFifteenLocales() {
        for (var locale : Locale.values()) {
            var context = context(); context.setLocaleCode(locale.code());
            var error = run(context, (ctx, location) -> {
                var checker = entry(); checker.requiredCheck(ctx, name(), null);
                checker.minNumberCheck(ctx, ObjectLocation.hashRoot("entry_list").element(1).member("amount"), 1, 0);
                checker.maxNumberCheck(ctx, ObjectLocation.hashRoot("entry_list").element(1).member("amount"), 1, 2);
                checker.minStringCheck(ctx, ObjectLocation.hashRoot("entry_list").element(1).member("password"), 12, SECRET);
                checker.maxStringCheck(ctx, ObjectLocation.hashRoot("entry_list").element(1).member("description"), 1, SECRET);
            });
            assertEquals(5, error.getViolates().size());
            for (var result : error.getViolates()) {
                assertEquals("Entry", result.getRootType()); assertFalse(result.getNaturalLanguageStatement().contains("checker."));
                assertFalse(result.getNaturalLanguageStatement().contains(SECRET));
                assertFalse(result.getNaturalLanguageStatement().contains("{field}"));
                assertTrue(result.getLocation().instancePath().startsWith("/entryList/1/"));
                assertEquals("v1", result.getPresentation().modelRevision());
            }
            String message = error.getViolates().get(3).getNaturalLanguageStatement();
            assertTrue(message, message.contains("12")); assertTrue(message, message.contains("7"));
            assertTrue(message, message.contains(CheckerMessageDefaults.unit(locale, "utf16")));
            assertEquals("[REDACTED]", error.getViolates().get(3).getInputValue());
            assertNull(CheckerInvocation.current(context));
        }
    }

    @Test public void rootOriginRelationshipRolesAndDeepPathsSurviveTheException() {
        var context = context();
        var error = run(context, (ctx, location) -> {
            entry().requiredCheck(ctx, name(), null);
            entry().requiredCheck(ctx, ObjectLocation.hashRoot("payer").member("name"), null);
            entry().requiredCheck(ctx, ObjectLocation.hashRoot("beneficiary").member("name"), null);
            entry().requiredCheck(ctx, ObjectLocation.hashRoot("entry_list").element(0).member("entry_list").element(2).member("name"), null);
        });
        assertEquals(List.of("The name of the 2nd entry is required.", "The name of the payer is required.",
                "The name of the beneficiary is required.", "The name is required.\nLocation: the 1st entry → the 3rd entry"),
                error.getViolates().stream().map(CheckResult::getNaturalLanguageStatement).toList());
        assertEquals("Entry", error.getViolates().get(0).toWire(JsonFieldNamingProfile.CAMEL_CASE).entityType());
    }

    @Test public void exceptionAndAllWireProfilesRetainStructuredIdentityWithoutLeakingMetadata() throws Exception {
        var error = run(context(), (ctx, location) -> entry().requiredCheck(ctx, name(), null));
        var result = error.getViolates().get(0); result.setSourceInstancePath("/submitted/rows/1/name");
        var mapper = new ObjectMapper();
        for (var profile : JsonFieldNamingProfile.values()) {
            var wire = result.toWire(profile); var json = mapper.readTree(mapper.writeValueAsString(wire));
            assertEquals("required", json.get("ruleId").textValue()); assertEquals("Entry", json.get("entityType").textValue());
            assertEquals(result.getLocation().instancePath(profile), json.get("instancePath").textValue());
            assertEquals("/submitted/rows/1/name", json.get("sourceInstancePath").textValue());
            assertEquals(error.getMessage(), json.get("message").textValue());
            assertFalse(json.has("presentation")); assertFalse(json.has("origin"));
        }
        assertFalse(mapper.writeValueAsString(result).contains("\"presentation\""));
    }

    @Test public void invocationCapturesModelBeforeCustomerCodeChangesContextAndDoesNotLeakIntoNextGraph() {
        var context = context();
        var first = run(context, (ctx, location) -> {
            ctx.installCheckerPresentationModel(model("v2", "title"));
            entry().requiredCheck(ctx, name(), null);
        }).getViolates().get(0);
        assertEquals("The name of the 2nd entry is required.", first.getNaturalLanguageStatement());
        var second = run(context, (ctx, location) -> entry().requiredCheck(ctx, name(), null)).getViolates().get(0);
        assertEquals("The title of the 2nd entry is required.", second.getNaturalLanguageStatement());
        context.translateCheckResult(first);
        assertEquals("The name of the 2nd entry is required.", first.getNaturalLanguageStatement());
        first.setLocation(ObjectLocation.hashRoot("payer").member("name")); context.translateCheckResult(first);
        assertEquals("The name of the payer is required.", first.getNaturalLanguageStatement());
        assertEquals("v1", first.getPresentation().modelRevision());
    }

    @Test public void shapeOverridesLegacyOverridesAndExplicitMessagesKeepTheirPrecedence() {
        var context = context(); legacy(context, "{\"checker.required\":\"Legacy {location}\"}");
        assertEquals("Legacy entry_list[1].name", run(context, (ctx, location) -> entry().requiredCheck(ctx, name(), null)).getMessage());
        context.installCheckerMessageCatalog(catalog("{\"checker.required\":{\"one\":\"Supply {field}.\"}}"));
        assertEquals("Legacy entry_list[1].name", run(context, (ctx, location) -> entry().requiredCheck(ctx, name(), null)).getMessage());
        context.installCheckerMessageCatalog(catalog("{\"checker.required\":{\"two\":\"Supply {field} for {object}.\"}}"));
        assertEquals("Supply name for the 2nd entry.", run(context, (ctx, location) -> entry().requiredCheck(ctx, name(), null)).getMessage());
        var literal = run(context, (ctx, location) -> {
            var result = CheckResult.required(name()); result.setNaturalLanguageStatement("Customer-owned {field}"); entry().appendResult(ctx, result);
        });
        assertEquals("Customer-owned {field}", literal.getMessage());
        context.setLocaleCode("zh-CN");
        assertEquals("请填写第2条明细的名称。", run(context, (ctx, location) -> entry().requiredCheck(ctx, name(), null)).getMessage());
    }

    @Test public void englishCustomRuleFallbackSelectsEnglishOwnerGrammarBeforeBinding() {
        var context = context(); context.setLocaleCode("zh-CN");
        context.installCheckerMessageCatalog(catalog("{\"checker.unknown_field\":{\"two\":\"Check {field} for {object}.\"}}"));
        var error = run(context, (ctx, location) -> entry().appendResult(ctx, CheckResult.unknownField(name())));
        assertEquals("Check name for the 2nd entry.", error.getMessage());
        assertEquals("unknown_field", error.getViolates().get(0).toWire(JsonFieldNamingProfile.CAMEL_CASE).ruleId());
    }

    @Test public void renderingFailuresRemainLocatedCheckerFailuresWithoutProviderSecrets() {
        for (boolean blank : List.of(true, false)) {
            var context = context(); context.installCheckerMessageRenderer(request -> {
                assertThrows(UnsupportedOperationException.class, () -> request.arguments().put("field", "tampered"));
                if (blank) return " "; throw new IllegalStateException("PRIVATE_PROVIDER_CAUSE");
            });
            var error = run(context, (ctx, location) -> entry().requiredCheck(ctx, name(), null));
            assertEquals("The name of the 2nd entry is invalid.", error.getMessage());
            assertEquals("/entryList/1/name", error.getViolates().get(0).getLocation().instancePath());
            assertEquals(CheckResult.RuleId.REQUIRED, error.getViolates().get(0).getRuleId());
            assertFalse(error.toString().contains("PRIVATE_PROVIDER_CAUSE")); assertNull(error.getCause());
        }
    }

    @Test public void customRendererOnlyReceivesProjectedDataAndCannotRestoreCredentials() {
        var context = context(); context.installCheckerMessageRenderer(request -> {
            assertEquals("[REDACTED]", request.arguments().get("input"));
            assertEquals(7, request.arguments().get("input_len"));
            return "Customer explanation: " + SECRET;
        });
        var error = run(context, (ctx, location) -> entry().minStringCheck(ctx,
                ObjectLocation.hashRoot("entry_list").element(1).member("password"), 12, SECRET));
        assertEquals("Customer explanation: [REDACTED]", error.getMessage());
    }

    @Test public void missingLabelsOnlyEmitBoundedValueFreeWarningsAndDoNotChangeValidation() {
        var records = new ArrayList<String>();
        var context = context(records); context.installCheckerPresentationModel(CheckerPresentation.Model.empty());
        var error = run(context, (ctx, location) -> {
            for (int i = 0; i < 40; i++) entry().requiredCheck(ctx, name(), null);
        });
        assertEquals(40, error.getViolates().size());
        assertEquals(List.of("CHECKER_MESSAGE_LABEL_FALLBACK"), records);
        assertEquals("The name is required.\nLocation: the 2nd entry list", error.getViolates().get(0).getNaturalLanguageStatement());
    }

    @Test public void independentContextsRenderConcurrentlyWithoutCrossingLocalesOrOrigins() throws Exception {
        var executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                int index = i;
                futures.add(executor.submit(() -> {
                    var context = context(); if (index % 2 == 1) context.setLocaleCode("zh-CN");
                    return run(context, (ctx, location) -> entry().requiredCheck(ctx, name(), null)).getMessage();
                }));
            }
            for (int i = 0; i < futures.size(); i++) assertEquals(i % 2 == 0 ? "The name of the 2nd entry is required." : "请填写第2条明细的名称。", futures.get(i).get());
        } finally { executor.shutdownNow(); assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS)); }
    }

    @Test public void actualSavePreservesChildIndexAndRejectsBeforeAnyWriteOrAudit() {
        var fixture = new ContextDerivedMutationTest.Fixture();
        var model = new CheckerPresentation.Model("save-v1", Map.of("ContextProbe", new CheckerPresentation.Entity(names("probe", "样本"),
                List.of(scalar("label", "name", "名称"), relation("children", "ContextProbe", "child", "子项")))), CheckerMessageDefaults.currentObjectLabels());
        fixture.context.installCheckerPresentationModel(model);
        var parent = fixture.original(1); var sibling = fixture.original(2); var child = fixture.original(3);
        parent.__internalSet("children", List.of(sibling, child)); child.updateProperty("label", null);
        var error = assertThrows(CheckException.class, () -> fixture.save(parent));
        assertEquals("The name of the 2nd child is required.", error.getMessage());
        assertEquals("/children/1/label", error.getViolates().get(0).getLocation().instancePath());
        assertEquals(0, fixture.provider.writes); assertEquals(0, fixture.audits);
        assertEquals(ContextDerivedMutationTest.A, child.values.get("touched_at"));
        assertNull(CheckerInvocation.current(fixture.context));
    }

    @Test public void passiveGeneratedModulesInstallSharedMetadataWithoutAnyPerContextSetup() {
        var first = model("module-v1", "name");
        var other = new CheckerPresentation.Model("account-v1", Map.of("Account", new CheckerPresentation.Entity(names("account", "账户"),
                List.of(scalar("name", "account name", "账户名称")))), CheckerMessageDefaults.currentObjectLabels());
        var combined = RuntimeModule.of().withCheckerPresentation(first).and(RuntimeModule.of().withCheckerPresentation(other));
        var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory()).executionLogging(false).build();
        runtime.install(combined); runtime.install(combined);
        var context = new DefaultUserContext(runtime); var secondContext = new DefaultUserContext(runtime);
        assertSame(context.checkerPresentationModel(), secondContext.checkerPresentationModel());
        assertEquals("The name of the 2nd entry is required.", run(context, (ctx, location) -> entry().requiredCheck(ctx, name(), null)).getMessage());
        var accountError = assertThrows(CheckException.class, () -> checker("Account", (ctx, location) ->
                entry().requiredCheck(ctx, ObjectLocation.hashRoot("name"), null)).checkAndFix(context, new NamedEntity("Account")));
        assertEquals("The account name is required.", accountError.getMessage());
        assertThrows(IllegalArgumentException.class, () -> runtime.install(RuntimeModule.of().withCheckerPresentation(model("conflict", "conflicting label"))));
        assertSame(context.checkerPresentationModel(), secondContext.checkerPresentationModel());
        assertEquals("The name of the 2nd entry is required.", run(context, (ctx, location) -> entry().requiredCheck(ctx, name(), null)).getMessage());
    }

    @Test public void nestedIndependentScopesAndInvalidLocaleConfigurationDoNotOverwriteTheOuterOrigin() {
        var context = context();
        assertThrows(io.teaql.core.i18n.UnsupportedLocaleException.class, () -> context.setLocaleCode("not-a-locale"));
        assertEquals(Locale.ENGLISH, context.locale());
        var outer = run(context, (ctx, location) -> {
            try (var nested = CheckerInvocation.open(ctx, "Entry")) {
                entry().requiredCheck(ctx, ObjectLocation.hashRoot("name"), null);
                var violations = (List<CheckResult>) nested.attribute(Checker.TEAQL_DATA_CHECK_RESULT);
                assertEquals("The name is required.", violations.get(0).getNaturalLanguageStatement());
            }
            entry().requiredCheck(ctx, name(), null);
        });
        assertEquals(1, outer.getViolates().size());
        assertEquals("The name of the 2nd entry is required.", outer.getMessage());
    }

    @Test public void customWarningAdapterCannotInjectSecretsOrReplaceACheckerFailure() {
        var warnings = new ArrayList<String>(); var context = context(warnings);
        context.installCheckerMessageRenderer(request -> {
            for (var capture : request.warningSink().getClass().getDeclaredFields()) {
                assertFalse("Renderer input must not retain a Context", UserContext.class.isAssignableFrom(capture.getType()));
                assertFalse("Renderer input must not retain an invocation", CheckerInvocation.class.isAssignableFrom(capture.getType()));
            }
            request.warningSink().accept("PRIVATE_PROVIDER_CAUSE");
            request.warningSink().accept(null);
            return CheckerMessageRenderer.STANDARD.render(request);
        });
        assertEquals("The name of the 2nd entry is required.", run(context, (ctx, location) -> entry().requiredCheck(ctx, name(), null)).getMessage());
        assertEquals(List.of("CHECKER_MESSAGE_RENDER_FALLBACK"), warnings);
    }

    @Test public void installingAnInvalidApplicationCatalogIsAtomic() {
        var context = context();
        var before = catalog("{\"checker.required\":{\"two\":\"Supply {field} for {object}.\"}}");
        context.installCheckerMessageCatalog(before);
        assertThrows(IllegalArgumentException.class, () -> context.installCheckerMessageCatalog(
                catalog("{\"checker.required\":{\"two\":\"Missing owner for {field}\"}}")));
        assertSame(before, context.checkerMessageCatalog());
        assertEquals("Supply name for the 2nd entry.", run(context, (ctx, location) -> entry().requiredCheck(ctx, name(), null)).getMessage());
    }

    @Test public void legacyApplicationInterpolationIsSinglePassAndKeepsUnknownBracesLiteral() {
        assertEquals("Legacy {system} {unknown}", I18nCatalog.renderApplicationMessage("Legacy {location} {unknown}",
                Map.of("location", "{system}", "system", "SECRET")));
        String malformedLiteral = "{".repeat(10000);
        assertEquals(malformedLiteral, I18nCatalog.renderApplicationMessage(malformedLiteral, Map.of()));
        assertNull(I18nCatalog.builtin().applicationMessage(Locale.ENGLISH, "checker.required"));
    }
}
