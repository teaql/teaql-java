package io.teaql.core.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SqlLogRendererTest {
    @Test void rendersOnlySafeCallbackValuesAndIgnoresQuotedPlaceholders() {
        var calls = new AtomicInteger();
        String sql = "-- ?\nUPDATE \"customer?\" SET name = ?, active = ? WHERE note = '?' /* ? */";
        String rendered = SqlLogRenderer.render(sql, 2, i -> {
            calls.incrementAndGet();
            return i == 0 ? "'Ri*****de' /* masked */" : "TRUE";
        }, "sqlite");
        assertEquals(2, calls.get());
        assertEquals("-- ?\nUPDATE \"customer?\" SET name = 'Ri*****de' /* masked */, active = TRUE WHERE note = '?' /* ? */", rendered);
        assertFalse(rendered.contains("Riverside"));
    }

    @Test void strictBindingCountsFailClosedWithoutValuesInErrors() {
        for (String sql : List.of("SELECT ?, ?", "SELECT 1", "SELECT 'secret-canary ?",
                "SELECT ? /* unterminated", "SELECT /*! ? */ ?")) {
            var error = assertThrows(IllegalArgumentException.class,
                    () -> SqlLogRenderer.render(sql, 1, i -> "'secret-canary'", "sqlite"));
            assertFalse(error.getMessage().contains("secret-canary"));
            assertFalse(error.getMessage().contains(sql));
        }
        assertThrows(IllegalArgumentException.class, () -> SqlLogRenderer.render("?", 1, i -> null, "sqlite"));
        assertThrows(IllegalArgumentException.class, () -> SqlLogRenderer.render("", -1, i -> "NULL", "sqlite"));
    }

    @Test void respectsDialectQuotedRegionsAndEscapedOperators() {
        assertEquals("SELECT `a``?`, 'x\\\'?y', 10", SqlLogRenderer.render(
                "SELECT `a``?`, 'x\\\'?y', ?", 1, i -> "10", "mysql"));
        assertEquals("SELECT [a]]?], 10", SqlLogRenderer.render("SELECT [a]]?], ?", 1, i -> "10", "mssql"));
        assertEquals("SELECT $$?$$, $body$?$body$, data ? 'a', 10 /* outer /* ? */ ? */",
                SqlLogRenderer.render("SELECT $$?$$, $body$?$body$, data ?? 'a', ? /* outer /* ? */ ? */",
                        1, i -> "10", "postgresql"));
        assertThrows(IllegalArgumentException.class, () -> SqlLogRenderer.render("SELECT $body$?", 0, i -> "NULL", "postgresql"));
        assertThrows(IllegalArgumentException.class, () -> SqlLogRenderer.render("SELECT E'\\\'?', ?", 1, i -> "NULL", "postgresql"));
        assertThrows(IllegalArgumentException.class, () -> SqlLogRenderer.render("SELECT ? # ?", 1, i -> "NULL", "mysql"));
    }

    @Test void retainsTypedTemporalLiterals() {
        var date = LocalDate.of(2024, 2, 29);
        var time = LocalDateTime.of(2026, 8, 19, 3, 30, 0, 123_000_000);
        assertEquals("'2024-02-29'", SqlLogRenderer.literal(date, "sqlite"));
        assertEquals("DATE '2024-02-29'", SqlLogRenderer.literal(date, "postgresql"));
        assertEquals("CAST('2024-02-29' AS DATE)", SqlLogRenderer.literal(date, "mysql"));
        assertEquals("TIMESTAMP '2026-08-19 03:30:00.123'", SqlLogRenderer.literal(time, "postgresql"));
        assertEquals("CAST('2026-08-19 03:30:00.123' AS DATETIME(3))", SqlLogRenderer.literal(time, "mysql"));
        assertEquals("CAST('2026-08-19 03:30:00.123' AS DATETIME2(3))", SqlLogRenderer.literal(time, "mssql"));
    }

    @Test void safeTypedLiteralsDoNotInvokeUnknownObjectToString() {
        assertEquals("NULL", SqlLogRenderer.literal(null, "sqlite"));
        assertEquals("TRUE", SqlLogRenderer.literal(true, "sqlite"));
        assertEquals("128.000001", SqlLogRenderer.literal(new BigDecimal("128.000001"), "sqlite"));
        assertEquals("'O''Reilly'", SqlLogRenderer.literal("O'Reilly", "sqlite"));
        assertEquals("'a\\\\b'", SqlLogRenderer.literal("a\\b", "mysql"));
        assertEquals("X'00ff'", SqlLogRenderer.literal(new byte[]{0, (byte) 255}, "sqlite"));
        assertEquals("decode('00ff','hex')", SqlLogRenderer.literal(new byte[]{0, (byte) 255}, "postgresql"));
        assertThrows(IllegalArgumentException.class, () -> SqlLogRenderer.literal(Double.NaN, "sqlite"));
        assertThrows(IllegalArgumentException.class, () -> SqlLogRenderer.literal(Double.POSITIVE_INFINITY, "sqlite"));
        assertThrows(IllegalArgumentException.class, () -> SqlLogRenderer.literal("canary\0value", "sqlite"));
        Object secret = new Object() { @Override public String toString() { fail("must not stringify unknown values"); return "secret"; } };
        assertThrows(IllegalArgumentException.class, () -> SqlLogRenderer.literal(secret, "sqlite"));
    }
}
