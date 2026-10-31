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
        assertEquals(context.i18nCatalog().render(result,context.locale()),result.getNaturalLanguageStatement());
    }

    @Test public void cachedAutomaticMessageFollowsItsRelocatedCanonicalPath() {
        var context=new DefaultUserContext(null);
        var result=CheckResult.min(ObjectLocation.hashRoot("unit_price"),2,1);
        context.translateCheckResult(result);
        result.setLocation(ObjectLocation.hashRoot("order_item_list").element(2).member("unit_price"));
        context.translateCheckResults(List.of(result));
        assertEquals(context.i18nCatalog().render(result,context.locale()),result.getNaturalLanguageStatement());
        assertTrue(result.getNaturalLanguageStatement(),result.getNaturalLanguageStatement().contains("order_item_list"));
    }

    @Test public void retranslationKeepsTheActualLengthNotTheRedactedSentinelLength() {
        var context=new DefaultUserContext(null);
        var result=CheckResult.minStr(ObjectLocation.hashRoot("password"),12,"密钥🔑abc");
        context.translateCheckResult(result);
        assertEquals("[REDACTED]",result.getInputValue());
        result.setLocation(ObjectLocation.hashRoot("order_item_list").element(2).member("password"));
        context.setLocaleCode("zh-CN").translateCheckResults(List.of(result));
        assertEquals(context.i18nCatalog().render(result,context.locale(),7),result.getNaturalLanguageStatement());
        assertEquals(12,result.getSystemValue());
        assertFalse(result.getNaturalLanguageStatement().contains("密钥🔑abc"));
        result.setInputValue("short");
        context.translateCheckResults(List.of(result));
        assertEquals(context.i18nCatalog().render(result,context.locale(),5),result.getNaturalLanguageStatement());
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

    @Test public void presentationCachePayloadContainsOnlyFingerprintsAndCounters() throws Exception {
        Class<?> entry=Class.forName("io.teaql.core.checker.internal.CheckerPresentationCache$Entry");
        int fields=0;
        for (var field : entry.getDeclaredFields()) if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
            assertTrue(field.toString(),field.getType() == int.class || field.getType() == byte[].class);
            fields++;
        }
        assertEquals(3,fields);
    }
}
