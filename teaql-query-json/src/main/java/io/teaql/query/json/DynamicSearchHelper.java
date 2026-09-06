package io.teaql.query.json;

import java.util.Date;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import io.teaql.core.BaseRequest;
import io.teaql.core.SearchCriteria;
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

    public static final String WARNINGS_EXTENSION = "teaql.dynamicSearch.warnings";
    private static final Logger LOGGER = Logger.getLogger(DynamicSearchHelper.class.getName());
    private static final Set<String> SEARCH_CONTROLS =
            Set.of("_orderBy", "_start", "_size", "_page", "_pageSize");

    public static List<DynamicSearchWarning> warningsOf(BaseRequest request) {
        Object warnings = request.getExtensions().get(WARNINGS_EXTENSION);
        if (!(warnings instanceof List<?>)) {
            return Collections.emptyList();
        }
        List<DynamicSearchWarning> result = new ArrayList<>();
        for (Object warning : (List<?>) warnings) {
            if (warning instanceof DynamicSearchWarning) {
                result.add((DynamicSearchWarning) warning);
            }
        }
        return Collections.unmodifiableList(result);
    }

    protected void warnUnknownField(BaseRequest request, String clause, String fieldPath) {
        DynamicSearchWarning warning =
                new DynamicSearchWarning(
                        DynamicSearchWarning.UNKNOWN_FIELD,
                        request.getTypeName(),
                        clause,
                        fieldPath);
        Object existing = request.getExtensions().get(WARNINGS_EXTENSION);
        List<DynamicSearchWarning> warnings;
        if (existing instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<DynamicSearchWarning> typed = (List<DynamicSearchWarning>) existing;
            warnings = typed;
        }
        else {
            warnings = new ArrayList<>();
            request.putExtension(WARNINGS_EXTENSION, warnings);
        }
        warnings.add(warning);
        LOGGER.log(
                Level.WARNING,
                "Ignored unknown dynamic search field: code={0}, requestType={1}, clause={2}, fieldPath={3}",
                new Object[] {
                    warning.getCode(),
                    warning.getRequestType(),
                    warning.getClause(),
                    warning.getFieldPath()
                });
    }

    protected static JsonNode jsonFromString(String jsonExpr) {
        try {
            ObjectMapper objectMapper = new ObjectMapper()
                    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
            JsonNode jsonNode = objectMapper.readTree(jsonExpr);
            if (jsonNode == null || !jsonNode.isObject()) {
                throw new IllegalArgumentException("Dynamic search must be a JSON object");
            }
            return jsonNode;
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Dynamic search requires a valid JSON object");
        }
    }

    public void mergeClauses(BaseRequest baseRequest, JsonNode jsonExpr) {
        if (jsonExpr == null || !jsonExpr.isObject()) {
            throw new IllegalArgumentException("Dynamic search must be a JSON object");
        }
        // Validate control keys before adding any clauses. Trusted context is not search input.
        jsonExpr.fieldNames().forEachRemaining(name -> {
            if (name.startsWith("_") && !SEARCH_CONTROLS.contains(name)) {
                throw new IllegalArgumentException("Unsupported dynamic search control: " + name);
            }
            if (SEARCH_CONTROLS.contains(name) && !"_orderBy".equals(name)) {
                JsonNode value = jsonExpr.get(name);
                int minimum = "_page".equals(name) || "_pageSize".equals(name) ? 1 : 0;
                if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < minimum) {
                    throw new IllegalArgumentException("Invalid dynamic search paging control: " + name);
                }
                if (("_size".equals(name) || "_pageSize".equals(name))
                        && value.intValue() > baseRequest.hardLimit()) {
                    throw new IllegalArgumentException("Dynamic search page size exceeds hard limit");
                }
            }
        });
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
                if (!fieldName.startsWith("_")) {
                    warnUnknownField(baseRequest, "FILTER", fieldName);
                }
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
        SearchCriteria originalCriteria = rootRequest.getSearchCriteria();
        BaseRequest currentRequest = rootRequest;
        for (int i = 0; i < fieldNames.length - 1; i++) {
            Optional<BaseRequest> optional;
            try {
                optional = currentRequest.subRequestOfFieldName(fieldNames[i]);
            }
            catch (IllegalArgumentException exception) {
                rootRequest.replaceSearchCriteria(originalCriteria);
                warnUnknownField(rootRequest, "FILTER", fieldName);
                return false;
            }
            if (optional.isEmpty()) {
                rootRequest.replaceSearchCriteria(originalCriteria);
                warnUnknownField(rootRequest, "FILTER", fieldName);
                return false;
            }
            currentRequest = optional.get();
        }
        final String lastSegmentOfField = fieldNames[fieldNames.length - 1];
        if (!currentRequest.isOneOfSelfField(lastSegmentOfField)) {
            rootRequest.replaceSearchCriteria(originalCriteria);
            warnUnknownField(rootRequest, "FILTER", fieldName);
            return false;
        }
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
                throw new IllegalArgumentException("Unsupported dynamic search value or operator object");
            }
            JsonNode id = node.get("id");
            if (id.isIntegralNumber() && id.canConvertToLong()) return id.longValue();
            if (id.isTextual()) {
                try { return Long.parseLong(id.textValue()); }
                catch (NumberFormatException invalid) {
                    throw new IllegalArgumentException("Dynamic search reference id must be an integer");
                }
            }
            throw new IllegalArgumentException("Dynamic search reference id must be an integer");
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
                warnUnknownField(baseRequest, "ORDER_BY", fieldValue.asText());
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
            warnUnknownField(baseRequest, "ORDER_BY", field);
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
