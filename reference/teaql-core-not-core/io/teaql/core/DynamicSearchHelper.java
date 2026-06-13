package io.teaql.core;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.sql.Timestamp;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import io.teaql.core.utils.PageUtil;

import io.teaql.core.criteria.Operator;

class SearchField {

    String fieldName;
    boolean isDateTimeField;

    public static SearchField timeField(String fieldName) {
        SearchField searchField = new SearchField();
        searchField.setFieldName(fieldName);
        searchField.setDateTimeField(true);
        return searchField;
    }

    public static SearchField dateField(String fieldName) {
        SearchField searchField = new SearchField();
        searchField.setFieldName(fieldName);
        searchField.setDateTimeField(true);
        return searchField;
    }

    public static SearchField commonField(String fieldName) {
        SearchField searchField = new SearchField();
        searchField.setFieldName(fieldName);
        searchField.setDateTimeField(false);
        return searchField;
    }

    public static SearchField fromRequest(BaseRequest request, String fieldName) {

        if (request.isDateTimeField(fieldName)) {
            return dateField(fieldName);
        }
        return commonField(fieldName);
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public boolean isDateTimeField() {
        return isDateTimeField;
    }

    public void setDateTimeField(boolean dateTimeField) {
        isDateTimeField = dateTimeField;
    }
}

public class DynamicSearchHelper {

    protected static JsonNode jsonFromString(String jsonExpr) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonExpr);
            return jsonNode;
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Input JSON format error: " + jsonExpr);
        }
    }

    public void mergeClauses(BaseRequest baseRequest, JsonNode jsonExpr) {
        this.addJsonFilter(baseRequest, jsonExpr); // where name='x'
        this.addJsonOrderBy(baseRequest, jsonExpr); // order by age
        this.addJsonLimiter(baseRequest, jsonExpr); // limit 0,1000
        this.addJsonPager(baseRequest, jsonExpr);
    }

    protected void addJsonPager(BaseRequest baseRequest, JsonNode jsonNode) {

        if (jsonNode == null) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

        AtomicInteger pageNumber = new AtomicInteger();
        jsonNode
                .fields()
                .forEachRemaining(
                        field -> {
                            String fieldName = field.getKey();
                            JsonNode fieldValue = field.getValue();
                            if ("_page".equals(fieldName) && fieldValue.intValue() > 0) {
                                pageNumber.set(fieldValue.intValue());
                            }
                            if ("_pageSize".equals(fieldName) && fieldValue.intValue() > 0) {
                                baseRequest.setSize(fieldValue.intValue());
                            }
                        });

        if (pageNumber.get() > 0) {
            int start = PageUtil.getStart(pageNumber.get() - 1, baseRequest.getSize());
            baseRequest.setOffset(start);
        }
    }

    public void addJsonFilter(BaseRequest baseRequest, JsonNode jsonNode) {
        if (jsonNode == null) {
            return;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();

            if (!handleChainField(baseRequest, field, jsonNode)) {
                continue;
            }
            String fieldName = field.getKey();

            if (!baseRequest.isOneOfSelfField(fieldName)) {
                continue;
            }
            JsonNode fieldValue = field.getValue();
            //      baseRequest.doAddSearchCriteria(
            //              new SimplePropertyCriteria(
            //                      fieldName, guessOperator(fieldName, fieldValue),
            // guessValue(baseRequest, fieldName, fieldValue)));

            SearchCriteria criteria =
                    baseRequest.createBasicSearchCriteria(
                            fieldName,
                            guessOperator(fieldName, fieldValue),
                            guessValue(SearchField.fromRequest(baseRequest, fieldName), fieldValue));

            baseRequest.appendSearchCriteria(criteria);
        }
    }

    protected boolean handleChainField(
            BaseRequest rootRequest, Map.Entry<String, JsonNode> field, JsonNode jsonNode) {
        String fieldName = field.getKey();
        String fieldNames[] = fieldName.split("\\.");

        if (fieldNames.length < 2) {
            return true; // need to continue
        }
        BaseRequest currentRequest = rootRequest;
        for (int i = 0; i < fieldNames.length - 1; i++) {
            Optional<BaseRequest> optional = currentRequest.subRequestOfFieldName(fieldNames[i]);
            currentRequest = optional.get();
        }
        final String lastSegmentOfField = fieldNames[fieldNames.length - 1];
        // last segment of field, use it as value
        currentRequest.appendSearchCriteria(
                currentRequest.createBasicSearchCriteria(
                        lastSegmentOfField,
                        guessOperator(lastSegmentOfField, field.getValue()),
                        guessValue(
                                SearchField.fromRequest(currentRequest, lastSegmentOfField), field.getValue())));

        return false;
    }

    public Operator guessOperator(String name, JsonNode value) {

        JsonNodeType nodeType = value.getNodeType();
        if (nodeType == JsonNodeType.STRING) {

            String valueExpr = value.asText();
            Operator operator = Operator.operatorByValue(valueExpr);
            if (operator != null) {
                return operator;
            }
            return Operator.CONTAIN;
        }
        if (nodeType == JsonNodeType.NUMBER || nodeType == JsonNodeType.BOOLEAN) {
            return Operator.EQUAL;
        }
        // ARRAY OF STRINGS
        if (value.isArray() && firstElementType(value.elements()) == JsonNodeType.STRING) {
            return Operator.IN;
        }
        // ARRAY OF NUMBERS, AND SIZE > 0

        // ARRAY OF STRINGS
        if (value.isArray() && firstElementType(value.elements()) == JsonNodeType.STRING) {
            return Operator.IN;
        }
        // ARRAY OF OBJECTs
        if (value.isArray() && firstElementType(value.elements()) == JsonNodeType.OBJECT) {
            return Operator.IN;
        }
        // ARRAY OF POJOs
        if (value.isArray() && firstElementType(value.elements()) == JsonNodeType.POJO) {
            return Operator.IN;
        }
        // Other types like number, use
        if (value.isArray() && isRange(value.elements())) {
            return Operator.BETWEEN; // this should be between
        }
        return Operator.EQUAL;
    }

    protected boolean isRange(Iterator<JsonNode> elements) {
        return countElements(elements) == 2;
        // two means range here
    }

    public int countElements(Iterator<JsonNode> elements) {
        int value = 0;

        while (elements.hasNext()) {
            elements.next();
            value++;
        }
        return value;
    }

    protected Object[] guessValue(SearchField searchField, JsonNode fieldValue) {

        if (!fieldValue.isArray()) {
            Object[] result = new Object[1];

            result[0] = unwrapValue(fieldValue);

            return result;
        }
        // for arrays here

        int count = countElements(fieldValue.elements());
        Object[] result = new Object[count];

        Iterator<JsonNode> elements = fieldValue.elements();
        JsonNodeType type = firstElementType(fieldValue.elements());
        int index = 0;

        while (elements.hasNext()) {
            JsonNode node = elements.next();
            if (searchField.isDateTimeField()) {
                result[index] = unwrapDateTimeValue(node);
                index++;
                continue;
            }
            result[index] = unwrapValue(node);

            index++;
        }

        return result;
    }

    protected Object unwrapValue(JsonNode node) {

        if (node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText().trim();
        }
        if (node.isDouble()) {
            return node.asDouble();
        }
        if (node.isFloat()) {
            return node.asDouble();
        }
        if (node.isBigInteger()) {
            return node.asLong();
        }
        if (node.isBigDecimal()) {
            return node.asDouble();
        }
        if (node.isNumber()) {
            return node.asLong();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isPojo()) {
            if (node.get("id") == null) {
                return null;
            }
            return node.get("id").asLong();
        }
        if (node.isObject()) {
            if (node.get("id") == null) {
                return null;
            }
            return node.get("id").asLong();
        }

        return node.asText().trim();

        // if (type == JsonNodeType.STRING)

    }

    public JsonNodeType firstElementType(Iterator<JsonNode> elements) {

        if (elements.hasNext()) {

            return elements.next().getNodeType();
        }
        return JsonNodeType.MISSING;
    }

    protected Object unwrapDateTimeValue(JsonNode node) {
        Object value = unwrapValue(node);
        //return new Timestamp((Long) value);
        return new Date((Long) value);
    }

    public void addJsonLimiter(BaseRequest baseRequest, JsonNode jsonNode) {
        if (jsonNode == null) {
            return;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

        jsonNode
                .fields()
                .forEachRemaining(
                        field -> {
                            String fieldName = field.getKey();
                            JsonNode fieldValue = field.getValue();
                            if ("_start".equals(fieldName)) {
                                baseRequest.setOffset(fieldValue.intValue());
                            }
                            if ("_size".equals(fieldName)) {
                                baseRequest.setSize(fieldValue.intValue());
                            }
                        });
        return;
    }

    public void addJsonOrderBy(BaseRequest baseRequest, JsonNode jsonNode) {
        if (jsonNode == null) {
            return;
        }

        JsonNode fieldValue = jsonNode.get("_orderBy");
        if (fieldValue == null) {
            return;
        }

        // single text
        if (fieldValue.isTextual()) {
            if (!baseRequest.isOneOfSelfField(fieldValue.asText())) {
                return;
            }
            this.addOrderBy(baseRequest, fieldValue.asText(), false);
            return;
        }

        if (fieldValue.isObject()) {
            addSingleJsonOrderBy(baseRequest, fieldValue);
            return;
        }
        // value is array
        if (fieldValue.isArray()) {
            fieldValue
                    .elements()
                    .forEachRemaining(
                            element -> {
                                addSingleJsonOrderBy(baseRequest, element);
                            });
            return;
        }
    }

    protected void addSingleJsonOrderBy(BaseRequest baseRequest, JsonNode jsonValueNode) {
        String field = jsonValueNode.get("field").asText();
        if (!baseRequest.isOneOfSelfField(field)) {
            return;
        }
        Boolean useAsc = jsonValueNode.get("useAsc").booleanValue();
        this.addOrderBy(baseRequest, field, useAsc);
        return;
    }

    public void addOrderBy(BaseRequest baseRequest, String property, boolean asc) {
        baseRequest.addOrderBy(property, asc);
    }
}
