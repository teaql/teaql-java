package io.teaql.runtime;

import static org.junit.Assert.*;
import io.teaql.core.checker.CheckResult;
import io.teaql.core.checker.ObjectLocation;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/** Cached diagnostic presentation must retain its pre-mask validation length. */
public class CheckerCachedPresentationTest {
    @Rule public TestWatcher receipt = new TestWatcher() {
        @Override protected void succeeded(Description d) { System.out.println("JAVA_CHECKER_PRESENTATION_CASE="+d.getMethodName()+":passed"); }
        @Override protected void failed(Throwable e, Description d) { System.out.println("JAVA_CHECKER_PRESENTATION_CASE="+d.getMethodName()+":failed"); }
    };

    @Test public void cachedAutomaticMessageFollowsTheCurrentLocale() {
        var context=new DefaultUserContext(null);
        var result=CheckResult.required(ObjectLocation.hashRoot("product_name"));
        context.translateCheckResult(result);
        String english=result.getNaturalLanguageStatement();
        context.setLocaleCode("zh-CN").translateCheckResults(List.of(result));
        assertNotEquals(english,result.getNaturalLanguageStatement());
        // The natural-message profile is distinct from explicit legacy v1 rendering.
        assertEquals("请填写product name。\n位置：当前对象",result.getNaturalLanguageStatement());
        assertEquals("/productName",result.getLocation().instancePath());
    }

    @Test public void cachedAutomaticMessageFollowsItsRelocatedCanonicalPath() {
        var context=new DefaultUserContext(null);
        var result=CheckResult.min(ObjectLocation.hashRoot("unit_price"),2,1);
        context.translateCheckResult(result);
        result.setLocation(ObjectLocation.hashRoot("order_item_list").element(2).member("unit_price"));
        context.translateCheckResults(List.of(result));
        assertEquals("The unit price must be at least 2.\nLocation: the 3rd order item list",result.getNaturalLanguageStatement());
        assertEquals("/orderItemList/2/unitPrice",result.getLocation().instancePath());
    }

    @Test public void retranslationKeepsTheActualLengthNotTheRedactedSentinelLength() {
        var context=new DefaultUserContext(null);
        var result=CheckResult.minStr(ObjectLocation.hashRoot("password"),12,"密钥🔑abc");
        context.translateCheckResult(result);
        assertEquals("[REDACTED]",result.getInputValue());
        result.setLocation(ObjectLocation.hashRoot("order_item_list").element(2).member("password"));
        context.setLocaleCode("zh-CN").translateCheckResults(List.of(result));
        assertEquals("password至少需要12个 UTF-16 代码单元，当前长度为7。\n位置：第3项order item list",result.getNaturalLanguageStatement());
        assertEquals("/orderItemList/2/password",result.getLocation().instancePath());
        assertEquals(12,result.getSystemValue());
        assertFalse(result.getNaturalLanguageStatement().contains("密钥🔑abc"));
        result.setInputValue("short");
        context.translateCheckResults(List.of(result));
        assertEquals("password至少需要12个 UTF-16 代码单元，当前长度为5。\n位置：第3项order item list",result.getNaturalLanguageStatement());
        assertEquals("[REDACTED]",result.getInputValue());
    }

    @Test public void replacingAnAutomaticMessageWithCustomerTextStopsAutomaticRerendering() {
        var context=new DefaultUserContext(null);
        var result=CheckResult.required(ObjectLocation.hashRoot("product_name"));
        context.translateCheckResult(result);
        result.setNaturalLanguageStatement("Customer-owned explanation");
        result.setLocation(ObjectLocation.hashRoot("order_item_list").element(2).member("product_name"));
        context.setLocaleCode("zh-CN").translateCheckResults(List.of(result));
        assertEquals("Customer-owned explanation",result.getNaturalLanguageStatement());
    }

    @Test public void presentationCacheRetainsOnlyFingerprintsCountersAndFiniteSafePlans() throws Exception {
        Class<?> entry=Class.forName("io.teaql.core.checker.internal.CheckerPresentationCache$Entry");
        int fields=0;
        for (var field : entry.getDeclaredFields()) if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
            assertTrue(field.toString(),field.getType() == int.class || field.getType() == byte[].class
                    || field.getType().getName().equals("io.teaql.core.checker.internal.CheckerMessageRendering$Plans"));
            fields++;
        }
        assertEquals(4,fields);
    }
}
