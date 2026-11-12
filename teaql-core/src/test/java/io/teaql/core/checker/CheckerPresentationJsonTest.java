package io.teaql.core.checker;

import io.teaql.core.i18n.CheckerMessageCatalog;
import io.teaql.core.i18n.Locale;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class CheckerPresentationJsonTest {
    private static final String MODEL = """
        {"schema":"teaql.checker-model/v1","revision":"generated-test","entities":{
          "Catalog":{"labels":{"en":"catalog","zh-CN":"目录"},"fields":[{"name":"entry_list","aliases":["entryList"],"targetType":"Entry",
            "labels":{"en":"entry","zh-CN":"明细"},"classifiers":{"zh-CN":"条"}}]},
          "Entry":{"labels":{"en":"entry","zh-CN":"明细"},"fields":[{"name":"name","labels":{"en":"name","zh-CN":"名称"}}]}
        }}
        """;
    @Test public void generatedModelResolvesAliasAndLocaleWithoutLoadingAnyEntity() {
        var model=CheckerPresentation.Model.fromJson(MODEL);
        assertEquals("generated-test",model.revision());
        var presentation=CheckerPresentation.capture(model,"Catalog",ObjectLocation.hashRoot("entryList").element(1).member("name"));
        assertEquals("The name of the 2nd entry is required.",presentation.render(
            CheckerMessageCatalog.builtin(),Locale.ENGLISH,"checker.required",Map.of(),code->fail(code)));
        assertEquals("请填写第2条明细的名称。",presentation.render(
            CheckerMessageCatalog.builtin(),Locale.CHINESE_SIMPLIFIED,"checker.required",Map.of(),code->fail(code)));
        assertThrows(UnsupportedOperationException.class,()->model.entities().clear());
    }
    @Test public void rejectsDuplicateKeysLocalesAliasesUnknownMembersAndTrailingData() {
        for(String source:new String[]{
            MODEL.replace("\"revision\":\"generated-test\"","\"revision\":\"first\",\"revision\":\"second\""),
            MODEL.replace("\"en\":\"name\"","\"en\":\"name\",\"en-US\":\"different\""),
            MODEL.replace("[\"entryList\"]","[\"entry_list\"]"),
            MODEL.replace("[\"entryList\"]","[\"entryList\",\"entryList\"]"),
            MODEL.replace("\"revision\":","\"execute\":true,\"revision\":"),
            MODEL.replace("\"teaql.checker-model/v1\"","\"teaql.i18n/v1\""),
            MODEL.replace("\"en\":\"name\"","\"en\":\"line\\nnext\""),
            MODEL+" {}"
        }) assertThrows(IllegalArgumentException.class,()->CheckerPresentation.Model.fromJson(source));
    }
    @Test public void parserFailureNeverEchoesTheSource() {
        var error=assertThrows(IllegalArgumentException.class,()->CheckerPresentation.Model.fromJson("{raw-secret-canary"));
        assertFalse(error.toString().contains("raw-secret-canary"));assertNull(error.getCause());
    }
}
