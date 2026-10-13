package io.teaql.core;

/** Generator-owned native Q spelling; never reconstructed from SQL or a relation name. */
public record QueryOperation(Kind kind, String method, String alias, String relation) {
    public enum Kind { NAMED_COUNT, FACET, RELATION_COUNT, RELATION_SELECT, AGGREGATE, PAGINATION }

    public QueryOperation {
        java.util.Objects.requireNonNull(kind, "kind");
        if (method != null && !method.matches("[A-Za-z_][A-Za-z_0-9]*"))
            throw new IllegalArgumentException("QUERY_OPERATION_INVALID: native method must be an identifier");
    }
    public static QueryOperation nativeMethod(Kind kind, String method, String alias, String relation) {
        return new QueryOperation(kind, java.util.Objects.requireNonNull(method), alias, relation);
    }
    public static QueryOperation framework(Kind kind, String alias, String relation) {
        return new QueryOperation(kind, null, alias, relation);
    }
    public String comment() {
        if (method != null) return method + "(" + (alias != null ? stringLiteral(alias)
                : kind == Kind.RELATION_SELECT ? "..." : "") + ")";
        return "framework: " + kind.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-')
                + (relation == null ? "" : " " + relation)
                + (alias == null ? "" : " " + stringLiteral(alias));
    }
    /** One renderer for Java Q string literals, independent of SQL quoting. */
    public static String stringLiteral(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 32 || c == 127) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }
}
