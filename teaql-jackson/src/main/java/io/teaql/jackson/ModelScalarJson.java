package io.teaql.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.math.BigDecimal;

/** Logical scalar wire rules, local to model entities; never global mapper coercion. */
final class ModelScalarJson {
    private static final long SAFE_INTEGER = 9_007_199_254_740_991L;
    private static final JsonNodeFactory NODES = JsonNodeFactory.withExactBigDecimals(true);
    private ModelScalarJson() {}

    static boolean supports(Class<?> type) {
        return type == String.class || type == Boolean.class || type == boolean.class
                || type == Integer.class || type == int.class || type == Long.class || type == long.class
                || type == BigDecimal.class || type == Float.class || type == float.class
                || type == Double.class || type == double.class || ModelTemporalJson.supports(type);
    }

    static Object read(JsonNode node, Class<?> type, Object owner, String field) throws JsonMappingException {
        if (node.isNull() && !type.isPrimitive()) return null;
        if (ModelTemporalJson.supports(type)) return ModelTemporalJson.read(node, type, owner, field);
        if (type == String.class && node.isTextual()) return node.textValue();
        if ((type == Boolean.class || type == boolean.class) && node.isBoolean()) return node.booleanValue();
        if ((type == Integer.class || type == int.class) && node.isIntegralNumber() && node.canConvertToInt()) return node.intValue();
        if (type == Long.class || type == long.class) {
            if (node.isIntegralNumber() && node.canConvertToLong()) {
                long value = node.longValue();
                if (value >= -SAFE_INTEGER && value <= SAFE_INTEGER) return value;
            }
            if (node.isTextual() && canonicalInteger(node.textValue())) {
                try { return Long.parseLong(node.textValue()); } catch (NumberFormatException ignored) { /* reject below */ }
            }
        }
        if (type == BigDecimal.class && node.isTextual()) {
            try { return new BigDecimal(node.textValue()); } catch (NumberFormatException ignored) { /* reject below */ }
        }
        if ((type == Float.class || type == float.class) && node.isNumber()) {
            float value = node.floatValue();
            if (Float.isFinite(value) && (value != 0 || node.decimalValue().signum() == 0)) return value;
        }
        if ((type == Double.class || type == double.class) && node.isNumber()) {
            double value = node.doubleValue();
            if (Double.isFinite(value) && (value != 0 || node.decimalValue().signum() == 0)) return value;
        }
        throw invalid(owner, field, type.getSimpleName());
    }

    static void writeField(Object owner, String field, Object value, JsonGenerator output, SerializerProvider provider) throws IOException {
        if (value instanceof Long || value instanceof BigDecimal) {
            // BigDecimal.toString retains exact value and scale, including a
            // negative scale, without expanding attacker-sized exponents.
            output.writeStringField(field, value.toString());
        } else if (value != null && ModelTemporalJson.supports(value.getClass())) {
            ModelTemporalJson.writeField(owner, field, value, output);
        } else {
            if ((value instanceof Float floatValue && !Float.isFinite(floatValue))
                    || (value instanceof Double doubleValue && !Double.isFinite(doubleValue))) {
                throw invalid(owner, field, "finite floating-point number");
            }
            try { provider.defaultSerializeField(field, value, output); }
            catch (JsonMappingException error) { error.prependPath(owner, field); throw error; }
        }
    }

    static JsonMappingException invalid(Object owner, String field, String expected) {
        // A source-less diagnostic cannot copy private input values from the
        // parser's source reference; the wire field remains structured in path.
        var error = JsonMappingException.from((JsonParser) null, "Invalid model scalar; expected " + expected);
        error.prependPath(owner, field);
        return error;
    }

    private static boolean canonicalInteger(String value) {
        int start = !value.isEmpty() && value.charAt(0) == '-' ? 1 : 0;
        if (start == value.length() || (value.charAt(start) == '0' && start + 1 < value.length())) return false;
        for (int index = start; index < value.length(); index++) {
            char digit = value.charAt(index);
            if (digit < '0' || digit > '9') return false;
        }
        return true;
    }

    /** Preserve numeric tokens before narrowing, without changing reader/mapper flags. */
    static JsonNode readTree(JsonParser parser) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == null) token = parser.nextToken();
        if (token == null) throw JsonMappingException.from((JsonParser) null, "Expected a model JSON value");
        return switch (token) {
            case START_OBJECT -> {
                var object = NODES.objectNode();
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    if (parser.currentToken() != JsonToken.FIELD_NAME) throw JsonMappingException.from((JsonParser) null, "Expected a model JSON field");
                    String name = parser.currentName(); parser.nextToken(); object.set(name, readTree(parser));
                }
                yield object;
            }
            case START_ARRAY -> {
                var array = NODES.arrayNode();
                while (parser.nextToken() != JsonToken.END_ARRAY) array.add(readTree(parser));
                yield array;
            }
            case VALUE_NUMBER_INT -> switch (parser.getNumberType()) {
                case INT -> NODES.numberNode(parser.getIntValue());
                case LONG -> NODES.numberNode(parser.getLongValue());
                default -> NODES.numberNode(parser.getBigIntegerValue());
            };
            case VALUE_NUMBER_FLOAT -> {
                String literal = parser.getText();
                try {
                    var decimal = new BigDecimal(literal);
                    // Decimal parsing intentionally has no signed zero; native
                    // Float/Double do. Preserve it before selecting a scalar type.
                    yield decimal.signum() == 0 && literal.startsWith("-") ? NODES.numberNode(-0.0d) : NODES.numberNode(decimal);
                } catch (NumberFormatException invalid) {
                    if (parser.isNaN()) yield NODES.numberNode(parser.getDoubleValue());
                    throw JsonMappingException.from((JsonParser) null, "Unrepresentable model JSON number");
                }
            }
            case VALUE_STRING -> NODES.textNode(parser.getText());
            case VALUE_TRUE -> NODES.booleanNode(true);
            case VALUE_FALSE -> NODES.booleanNode(false);
            case VALUE_NULL -> NODES.nullNode();
            default -> throw JsonMappingException.from((JsonParser) null, "Unsupported model JSON token");
        };
    }
}
