package io.teaql.core.sql.portable;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.Test;

public class PortableSQLTemporalConversionTest {
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
