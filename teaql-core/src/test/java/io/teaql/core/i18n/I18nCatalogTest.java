package io.teaql.core.i18n;

import static org.junit.Assert.*;
import io.teaql.core.checker.CheckResult;
import io.teaql.core.checker.ObjectLocation;
import java.util.List;
import org.junit.Test;

public class I18nCatalogTest {
    private final ObjectLocation location = ObjectLocation.hashRoot("name");

    @Test public void rendersFiveRulesForAllFifteenLocales() {
        List<CheckResult> results = List.of(
            CheckResult.required(location), CheckResult.min(location, 2, 1),
            CheckResult.max(location, 2, 3), CheckResult.minStr(location, 2, "a"),
            CheckResult.maxStr(location, 2, "abc"));
        int cells = 0;
        for (Locale locale : Locale.values()) for (CheckResult result : results) {
            String message = I18nCatalog.builtin().render(result, locale);
            assertFalse(message.startsWith("checker."));
            assertFalse(message.contains("{location}"));
            assertFalse(message.contains("{system}"));
            cells++;
        }
        assertEquals(75, cells);
    }

    @Test public void normalizesAliasesAndRejectsUnsupportedCodes() {
        assertEquals(Locale.ENGLISH, Locale.fromCode("EN_us"));
        assertEquals(Locale.CHINESE_SIMPLIFIED, Locale.fromCode("ZH_hans"));
        assertEquals(Locale.CHINESE_TRADITIONAL, Locale.fromCode("zh-HK"));
        assertEquals(Locale.FILIPINO, Locale.fromCode("tl"));
        assertThrows(UnsupportedLocaleException.class, () -> Locale.fromCode("xx"));
    }
}
