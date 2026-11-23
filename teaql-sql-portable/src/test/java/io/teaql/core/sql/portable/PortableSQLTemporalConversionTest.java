package io.teaql.core.sql.portable;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.Test;

public class PortableSQLTemporalConversionTest {
    @org.junit.Test
    public void nativeImmutableTemporalValuesAreNotFormattedAndParsedAgain() {
        var date = java.time.LocalDate.of(2026, 10, 7);
        var time = java.time.LocalTime.of(12, 34, 56, 123456789);
        // SQL DateTime now has an approved millisecond carrier. The aligned
        // native value still takes the identity-preserving fast path.
        var timestamp = java.time.LocalDateTime.of(date, time.truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        org.junit.Assert.assertSame(date, PortableSQLRepository.convertTemporalColumnValue(java.time.LocalDate.class, date));
        org.junit.Assert.assertSame(time, PortableSQLRepository.convertTemporalColumnValue(java.time.LocalTime.class, time));
        org.junit.Assert.assertSame(timestamp, PortableSQLRepository.convertTemporalColumnValue(java.time.LocalDateTime.class, timestamp));
        org.junit.Assert.assertEquals(123000000, timestamp.getNano());
    }
    @Test
    public void subMillisecondSqlReadRejectsWithoutChangingTheNativeValue() {
        var nativeValue = LocalDateTime.of(2026, 11, 10, 12, 34, 56, 123456789);
        org.junit.Assert.assertThrows(SqlTemporalPrecisionException.class,
                () -> PortableSQLRepository.convertTemporalColumnValue(LocalDateTime.class, nativeValue));
        assertEquals(123456789, nativeValue.getNano());
        org.junit.Assert.assertNull(PortableSQLRepository.convertTemporalColumnValue(LocalDateTime.class, null));
    }
    @Test
    public void convertsJdbcStyleTemporalStrings() {
        String timestamp = "2026-08-12 05:25:00.0";
        assertEquals(
                LocalDateTime.of(2026, 8, 12, 5, 25),
                PortableSQLRepository.convertTemporalColumnValue(LocalDateTime.class, timestamp));
        assertEquals(
                LocalDate.of(2026, 8, 12),
                PortableSQLRepository.convertTemporalColumnValue(LocalDate.class, timestamp));
        assertEquals(
                LocalTime.of(5, 25),
                PortableSQLRepository.convertTemporalColumnValue(LocalTime.class, timestamp));
        Object vendorTemporal = new Object() {
            @Override public String toString() { return timestamp; }
        };
        assertEquals(
                LocalDateTime.of(2026, 8, 12, 5, 25),
                PortableSQLRepository.convertTemporalColumnValue(LocalDateTime.class, vendorTemporal));
    }
}
