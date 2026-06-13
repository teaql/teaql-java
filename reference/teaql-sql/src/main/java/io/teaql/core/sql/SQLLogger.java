package io.teaql.core.sql;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Marker;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;

import io.teaql.core.utils.LocalDateTimeUtil;
import io.teaql.core.utils.MapUtil;
import io.teaql.core.utils.StrUtil;

import io.teaql.core.AggregationResult;
import io.teaql.core.BaseEntity;
import io.teaql.core.UserContext;

public class SQLLogger {

    private static final char SINGLE_QUOTE = '\'';

    protected static <T> String showResult(List<T> result) {
        if (result.isEmpty()) {
            return String.format("NO ROWS");
        }
        String className = result.get(0).getClass().getSimpleName();
        if (result.size() > 1) {
            return String.format("%d*%s", result.size(), className);
        }
        String body =
                result.stream()
                        .map(
                                t -> {
                                    if (t instanceof BaseEntity) {
                                        return ((BaseEntity) t).getId().toString();
                                    }
                                    return t.toString();
                                })
                        .collect(Collectors.joining(","));
        return String.join("", className, "(", body, ")");
    }

    public static void logNamedSQL(
            Marker marker,
            UserContext userContext,
            String sql,
            Map<String, Object> paramMap,
            AggregationResult result) {
        String finalSQL = NamedParameterUtils.substituteNamedParameters(sql, null);
        List<Map<String, Object>> list = result.toList();

        boolean hasMore = false;
        if (list.size() > 3) {
            hasMore = true;
        }

        String resultString =
                list.stream()
                        .limit(3)
                        .map(item -> MapUtil.joinIgnoreNull(item, ",", "="))
                        .collect(Collectors.joining("/"));
        if (hasMore) {
            resultString = resultString + "...";
        }
        logSQLAndParameters(
                marker,
                userContext,
                finalSQL,
                NamedParameterUtils.buildValueArray(sql, paramMap),
                resultString);
    }

    public static <T> void logNamedSQL(
            Marker marker,
            UserContext userContext,
            String sql,
            Map<String, Object> paramMap,
            List<T> result) {
        String finalSQL = NamedParameterUtils.substituteNamedParameters(sql, null);
        logSQLAndParameters(
                marker,
                userContext,
                finalSQL,
                NamedParameterUtils.buildValueArray(sql, paramMap),
                showResult(result));
    }

    public static void logSQLAndParameters(
            Marker marker, UserContext userContext, String sql, Object[] parameters, String result) {

        StringBuilder finalSQL = new StringBuilder();

        char[] sqlChars = sql.toCharArray();
        int index = 0;

        Counter counter = new Counter();
        for (char ch : sqlChars) {
            counter.onChar(ch);
            if (ch == '?' && counter.outOfQuote()) {
                finalSQL.append(wrapValueInSQL(parameters[index]));
                index++;
                continue;
            }
            finalSQL.append(ch);
        }
        String comment = org.slf4j.MDC.get("comment");
        if (io.teaql.core.utils.StrUtil.isNotEmpty(comment)) {
            userContext.debug(marker, "[{}] [{}] {}", comment, result, finalSQL.toString());
        } else {
            userContext.debug(marker, "[{}] {}", result, finalSQL.toString());
        }
    }

    protected static String join(Object... objs) {
        StringBuilder internalPresentBuffer = new StringBuilder();
        for (Object o : objs) {
            if (o == null) {
                continue;
            }
            internalPresentBuffer.append(o);
        }
        return internalPresentBuffer.toString();
    }

    protected static String sqlTimeExpr(LocalDateTime dateTimeValue) {
        return LocalDateTimeUtil.formatNormal(dateTimeValue);
    }

    protected static String wrapValueInSQL(Object value) {
        if (value == null) {
            return "NULL";
        }

        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            return Arrays.asList(array).stream()
                    .limit(20)
                    .map(v -> wrapValueInSQL(v))
                    .collect(Collectors.joining(","));
        }

        if (value instanceof LocalDateTime) {
            LocalDateTime dateTimeValue = (LocalDateTime) value;
            return join("'", sqlTimeExpr(dateTimeValue), "'");
        }
        if (value instanceof Date) {
            Date dateValue = (Date) value;
            return join("'", sqlDateExpr(dateValue), "'");
        }

        if (value instanceof LocalDate) {
            LocalDate dateValue = (LocalDate) value;
            return join("'", sqlLocalDateExpr(dateValue), "'");
        }

        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean b) {
            return b.toString();
        }
        if (value instanceof String) {
            String strValue = (String) value;
            String escapedValue = StrUtil.sub(strValue, 0, 100).replace("\'", "''");
            return join("'", escapedValue, "'");
        }
        if (value instanceof Set) {

            Set setValue = (Set) value;
            return (String)
                    setValue.stream().limit(50).map(v -> wrapValueInSQL(v)).collect(Collectors.joining(","));
        }
        if (value instanceof List) {
            List setValue = (List) value;
            return (String)
                    setValue.stream().limit(10).map(v -> wrapValueInSQL(v)).collect(Collectors.joining(","));
        }

        return join("'", value.getClass(), "'");
    }

    private static Object sqlLocalDateExpr(LocalDate pDateValue) {
        return LocalDateTimeUtil.formatNormal(pDateValue);
    }

    protected static String sqlDateExpr(Date dateValue) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleDateFormat.format(dateValue);
    }

    static class Counter {
        int count = 0;

        public void onChar(char ch) {
            if (ch == SINGLE_QUOTE) {
                count++;
            }
        }

        public boolean outOfQuote() {
            return count % 2 == 0;
        }
    }
}
