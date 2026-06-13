package io.teaql.core.web;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.teaql.core.utils.BeanUtil;
import io.teaql.core.utils.CollectionUtil;
import io.teaql.core.utils.ListUtil;
import io.teaql.core.utils.ResourceUtil;
import io.teaql.core.utils.MapUtil;
import io.teaql.core.utils.BooleanUtil;
import io.teaql.core.utils.NumberUtil;
import io.teaql.core.utils.ObjectUtil;
import io.teaql.core.utils.StrUtil;

import io.teaql.core.BaseEntity;
import io.teaql.core.Entity;
import io.teaql.core.UserContext;
import io.teaql.core.DefaultUserContext;
import io.teaql.core.meta.PropertyDescriptor;

public class UITemplateRender {

    public static String messageTemplate =
            ResourceUtil.readUtf8Str("classpath:io/teaql/core/web/message.json");

    public static String kvTemplate = ResourceUtil.readUtf8Str("classpath:io/teaql/core/web/kv.json");

    public static String tableTemplate =
            ResourceUtil.readUtf8Str("classpath:io/teaql/core/web/table.json");

    public static String imagesTemplate =
            ResourceUtil.readUtf8Str("classpath:io/teaql/core/web/images.json");

    ObjectMapper mapper = new ObjectMapper();

    static String serviceRequestPopupKey(Entity request) {
        return StrUtil.format("serviceRequest:popup:{}", request.getId());
    }

    public void kv(
            UserContext ctx,
            PropertyDescriptor meta,
            Object uiField,
            Object fieldValue,
            List candidates,
            List mappedCandidates)
            throws JsonProcessingException {
        Map value = mapper.readValue(kvTemplate, Map.class);
        Object kids0 = ViewRender.getProperty(value, "kids[0]");

        setTemplatePassThrough(meta, kids0);
        ViewRender.setValue(kids0, "items", null);
        int index = 1;
        for (Object o : mappedCandidates) {
            Object id = ViewRender.getProperty(o, "id");
            ViewRender.addValue(
                    kids0,
                    "items",
                    MapUtil.builder()
                            .put("id", id == null ? index++ : id)
                            .put("title", ViewRender.getProperty(o, "title"))
                            .put("value", ViewRender.getProperty(o, "value"))
                            .build());
        }

        if (!meta.getBoolean("ui_no_label", false)) {
            ViewRender.setValue(kids0, "title", meta.getStr("zh_CN", null));
        }
        ViewRender.setValue(uiField, "value", value);
    }

    public void analytics(
            UserContext ctx,
            PropertyDescriptor meta,
            Object uiField,
            Object fieldValue,
            List candidates,
            List mappedCandidates) {
        if (candidates == null) {
            return;
        }
        int index = 1;
        ViewRender.setValue(uiField, "value", null);
        for (Object candidate : candidates) {
            Map<String, Object> newV = new HashMap<>();
            Object id = ViewRender.getProperty(candidate, "id");
            BeanUtil.setProperty(newV, "id", id == null ? index++ : id);
            BeanUtil.setProperty(newV, "value", BeanUtil.getProperty(candidate, "value"));
            BeanUtil.setProperty(
                    newV, "validateExpression", BeanUtil.getProperty(candidate, "validateExpression"));
            BeanUtil.setProperty(newV, "displayRule", BeanUtil.getProperty(candidate, "displayRule"));
            BeanUtil.setProperty(newV, "title", BeanUtil.getProperty(candidate, "name"));
            BeanUtil.setProperty(newV, "result", BeanUtil.getProperty(candidate, "result"));
            ViewRender.addValue(uiField, "value", newV);
        }
    }

    private void setTemplatePassThrough(PropertyDescriptor meta, Object uiElement) {
        meta.getAdditionalInfo()
                .forEach(
                        (k, value) -> {
                            String key = k;
                            if (!key.startsWith("ui_$template_")) {
                                return;
                            }
                            String property = StrUtil.removePrefix(key, "ui_$template_");

                            if (StrUtil.endWith(property, "_number")) {
                                property = StrUtil.removeSuffix(property, "_number");
                                ViewRender.setValue(uiElement, property, NumberUtil.parseNumber((String) value));
                                return;
                            }

                            if (StrUtil.endWith(property, "_bool")) {
                                property = StrUtil.removeSuffix(property, "_bool");
                                ViewRender.setValue(uiElement, property, BooleanUtil.toBoolean((String) value));
                                return;
                            }
                            ViewRender.setValue(uiElement, property, value);
                        });
    }

    public void message(UserContext ctx, PropertyDescriptor meta, Object uiField, String message)
            throws JsonProcessingException {
        Map value = mapper.readValue(messageTemplate, Map.class);
        BeanUtil.setProperty(value, "kids[0].text", message);
        BeanUtil.setProperty(uiField, "value", value);
    }

    public void error(UserContext ctx, PropertyDescriptor meta, Object uiField, String message)
            throws JsonProcessingException {
        Map value = mapper.readValue(messageTemplate, Map.class);
        BeanUtil.setProperty(value, "kids[0].text", message);
        BeanUtil.setProperty(value, "kids[0].style.color", "#f23030"); // rea
        BeanUtil.setProperty(uiField, "value", value);
    }

    public void json(UserContext ctx, PropertyDescriptor meta, Object uiField, Object obj)
            throws JsonProcessingException {
        if (obj == null) {
            ViewRender.setValue(uiField, "value", null);
            return;
        }
        ViewRender.setValue(uiField, "value", mapper.readValue(String.valueOf(obj), Map.class));
    }

    public void jsonArray(UserContext ctx, PropertyDescriptor meta, Object uiField, Object obj)
            throws JsonProcessingException {
        if (obj == null) {
            ViewRender.setValue(uiField, "value", null);
            return;
        }
        ViewRender.setValue(uiField, "value", mapper.readValue(String.valueOf(obj), List.class));
    }

    public void stringArray(UserContext ctx, PropertyDescriptor meta, Object uiField, Object obj)
            throws JsonProcessingException {
        if (obj == null) {
            ViewRender.setValue(uiField, "value", null);
            return;
        }
        ViewRender.setValue(
                uiField,
                "value",
                mapper.readValue(String.valueOf(obj), new TypeReference<List<String>>() {
                }));
    }

    public void images(UserContext ctx, PropertyDescriptor meta, Object uiField, Object obj)
            throws JsonProcessingException {
        if (obj == null) {
            BeanUtil.setProperty(uiField, "hidden", "true");
            return;
        }
        Map value = mapper.readValue(imagesTemplate, Map.class);
        BeanUtil.setProperty(value, "kids[0].items", mapper.readValue(String.valueOf(obj), List.class));
        ViewRender.setValue(uiField, "value", value);
    }

    public void table(
            UserContext ctx,
            PropertyDescriptor meta,
            Object uiField,
            Object fieldValue,
            List candidates,
            List mappedCandidates)
            throws JsonProcessingException {
        Map value = mapper.readValue(tableTemplate, Map.class);
        Object kids0 = ViewRender.getProperty(value, "kids[0]");

        // title
        ViewRender.setValue(kids0, "title", meta.getStr("zh_CN", null));
        ViewRender.setValue(uiField, "value", value);

        // clear data
        ViewRender.setValue(kids0, "data", null);

        Object first = CollectionUtil.getFirst(candidates);

        //
        Object backgroundColor = BeanUtil.getProperty(first, "backgroundColor");
        boolean firstIsColor = backgroundColor != null;
        if (firstIsColor) {
            BeanUtil.setProperty(kids0, "backgroundColor", backgroundColor);
            candidates = candidates.subList(1, candidates.size());
        }

        if (ObjectUtil.isEmpty(candidates)) {
            return;
        }

        Object header = CollectionUtil.getFirst(candidates);
        if (ObjectUtil.isEmpty(header)) {
            return;
        }
        Map<String, Object> headerMap = BeanUtil.toBean(header, LinkedHashMap.class);
        Set<String> columns = headerMap.keySet();

        // add header
        Map<String, Object> headerRow = new HashMap<>();
        headerRow.put("header", true);
        ViewRender.addValue(kids0, "data", headerRow);
        // add header columns
        for (String column : columns) {
            ViewRender.addValue(
                    headerRow,
                    "items",
                    MapUtil.builder().put("title", column).put("colspan", headerMap.get(column)).build());
        }

        // add data rows
        List dataRows = candidates.subList(1, candidates.size());
        if (ObjectUtil.isEmpty(dataRows)) {
            return;
        }

        for (Object dataRow : dataRows) {
            Map<String, Object> row = new HashMap<>();
            ViewRender.addValue(kids0, "data", row);

            Map<String, Object> map = BeanUtil.toBean(dataRow, Map.class);
            Set<String> keys = map.keySet();
            keys.removeAll(columns);

            for (String column : columns) {
                ViewRender.addValue(
                        row,
                        "items",
                        MapUtil.builder()
                                .put("title", BeanUtil.getProperty(dataRow, column))
                                .put("colspan", headerMap.get(column))
                                .build());
            }

            for (String key : keys) {
                ViewRender.setValue(row, key, map.get(key));
            }
        }
    }

    public Object showPop(UserContext ctx, BaseEntity data) throws Exception {
        return ServiceRequestUtil.showPop(ctx, data);
    }

    public void createStandardConfirmPopup(
            UserContext ctx,
            Entity request,
            String title,
            String text,
            String cancelActionUrl,
            String confirmActionUrl,
            String playSound) {
        Map<String, Object> popup = new HashMap<>();
        BeanUtil.setProperty(popup, "popup.title", title);
        BeanUtil.setProperty(popup, "popup.text", text);

        Map<Object, Object> confirmAction =
                MapUtil.builder()
                        .put("title", "CONFIRM")
                        .put("code", "confirm")
                        .put("linkToUrl", confirmActionUrl)
                        .build();
        if (!ObjectUtil.isEmpty(cancelActionUrl)) {
            Map<Object, Object> cancelAction =
                    MapUtil.builder()
                            .put("title", "CANCEL")
                            .put("code", "cancel")
                            .put("linkToUrl", cancelActionUrl)
                            .build();
            BeanUtil.setProperty(popup, "popup.actionList", ListUtil.of(cancelAction, confirmAction));
        }
        else {
            BeanUtil.setProperty(popup, "popup.actionList", ListUtil.of(confirmAction));
        }
        BeanUtil.setProperty(popup, "playSound", playSound);
        if (ctx instanceof DefaultUserContext dctx) {
            dctx.putInStore(serviceRequestPopupKey(request), popup, 10);
        }
    }

    public void createConfirmOnlyPopup(
            UserContext ctx,
            Entity request,
            String title,
            String text,
            String confirmAction,
            String playSound) {
        createStandardConfirmPopup(ctx, request, title, text, null, confirmAction, playSound);
    }
}
