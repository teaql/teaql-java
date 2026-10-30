package io.teaql.core.checker;

import io.teaql.core.BaseEntity;
import io.teaql.core.UserContext;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/** Error identity belongs to the Checker that owns the rule, not its caller. */
public class CheckerEntityTypeTest {
    private static UserContext context() {
        Map<String,Object> attributes = new HashMap<>();
        return (UserContext) Proxy.newProxyInstance(UserContext.class.getClassLoader(), new Class<?>[]{UserContext.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getAttribute") && args.length == 1) return attributes.get(args[0]);
                    if (method.getName().equals("putAttribute")) { attributes.put((String) args[0], args[1]); return null; }
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                    throw new AssertionError("unexpected runtime/provider call: " + method.getName());
                });
    }
    private static Checker<BaseEntity> checker(String type) {
        return new Checker<>() {
            public String type() { return type; }
            public void checkAndFix(UserContext context, BaseEntity value, ObjectLocation location) { }
        };
    }
    @SuppressWarnings("unchecked") private static List<CheckResult> results(UserContext context) {
        return (List<CheckResult>) context.getAttribute(Checker.TEAQL_DATA_CHECK_RESULT);
    }
    @Test public void generatedRequiredRuleCarriesItsCheckerTypeBeforeWireProjection() {
        UserContext context = context();
        checker("PurchaseOrder").requiredCheck(context, ObjectLocation.hashRoot("contact_phone"), null);
        CheckResult result = results(context).get(0);
        assertEquals("PurchaseOrder", result.getRootType());
        assertEquals("PurchaseOrder", result.toWire(JsonFieldNamingProfile.CAMEL_CASE).entityType());
        assertEquals("/contactPhone", result.toWire(JsonFieldNamingProfile.CAMEL_CASE).instancePath());
        assertFalse(result.getNaturalLanguageStatement().isBlank());
    }
    @Test public void everyRuleFamilyGetsTheSameStableTypeWithoutChangingItsRule() {
        UserContext context = context(); Checker<BaseEntity> checker = checker("OrderItem");
        ObjectLocation location = ObjectLocation.hashRoot("unit_price");
        List<CheckResult> candidates = List.of(CheckResult.required(location), CheckResult.min(location, 1, 0),
                CheckResult.max(location, 1, 2), CheckResult.minStr(location, 2, "x"),
                CheckResult.maxStr(location, 1, "xx"), CheckResult.invalidType(location, "invalid model value"),
                CheckResult.unknownField(location));
        for (CheckResult result : candidates) {
            CheckResult.RuleId original = result.getRuleId();
            checker.appendResult(context, result);
            assertEquals("OrderItem", result.getRootType());
            assertSame(original, result.getRuleId());
        }
        assertEquals(candidates, results(context));
    }
    @Test public void explicitMoreSpecificTypeSurvivesAParentChecker() {
        UserContext context = context(); CheckResult result = CheckResult.required(ObjectLocation.hashRoot("name"));
        result.setRootType("SpecializedOrderItem");
        checker("OrderItem").appendResult(context, result);
        assertEquals("SpecializedOrderItem", result.toWire(JsonFieldNamingProfile.CAMEL_CASE).entityType());
    }
    @Test public void blankTypeIsBoundBeforeTheCustomerLocalizationHook() {
        UserContext delegate = context(); int[] translations = {0};
        UserContext context = (UserContext) Proxy.newProxyInstance(UserContext.class.getClassLoader(), new Class<?>[]{UserContext.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("translateCheckResult")) {
                        assertEquals("OrderItem", ((CheckResult) args[0]).getRootType());
                        translations[0]++; return args[0];
                    }
                    try { return method.invoke(delegate, args); }
                    catch (java.lang.reflect.InvocationTargetException error) { throw error.getCause(); }
                });
        CheckResult result = CheckResult.required(ObjectLocation.hashRoot("product_name"));
        result.setRootType(" \t");
        checker("OrderItem").appendResult(context, result);
        assertEquals(1, translations[0]);
        assertEquals("OrderItem", results(delegate).get(0).getRootType());
    }
    @Test public void nestedRuleKeepsCanonicalLocationAndAllThreeExternalProfiles() {
        UserContext context = context();
        ObjectLocation location = ObjectLocation.hashRoot("order_item_list").element(0).member("product_name");
        checker("OrderItem").requiredCheck(context, location, null);
        CheckResult result = results(context).get(0);
        for (JsonFieldNamingProfile profile : JsonFieldNamingProfile.values()) {
            WireCheckResult wire = result.toWire(profile);
            assertEquals("OrderItem", wire.entityType());
            assertEquals(location.segments(), wire.location());
        }
        assertEquals("/orderItemList/0/productName", result.toWire(JsonFieldNamingProfile.CAMEL_CASE).instancePath());
        assertEquals("/order_item_list/0/product_name", result.toWire(JsonFieldNamingProfile.SNAKE_CASE).instancePath());
        assertEquals("/OrderItemList/0/ProductName", result.toWire(JsonFieldNamingProfile.PASCAL_CASE).instancePath());
        assertEquals("order_item_list[0].product_name", result.getLocation().modelPath());
    }
    @Test public void differentCheckersInOneContextDoNotShareOrOverwriteTypes() {
        UserContext context = context(); ObjectLocation location = ObjectLocation.hashRoot("name");
        checker("PurchaseOrder").requiredCheck(context, location, null);
        CheckResult first = results(context).get(0);
        checker("OrderItem").requiredCheck(context, location, null);
        assertEquals(List.of("PurchaseOrder", "OrderItem"), results(context).stream().map(CheckResult::getRootType).toList());
        assertEquals("PurchaseOrder", first.getRootType());
    }
}
