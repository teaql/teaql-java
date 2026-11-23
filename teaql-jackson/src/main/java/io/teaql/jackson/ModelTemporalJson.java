package io.teaql.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/** Passive temporal values: no clock, implicit zone, provider or mapper-wide settings. */
final class ModelTemporalJson {
    private ModelTemporalJson() {}

    static boolean supports(Class<?> type) {
        return type == LocalDate.class || type == LocalTime.class
                || type == LocalDateTime.class || type == Instant.class;
    }

    static Object read(JsonNode node, Class<?> type, Object owner, String field) throws JsonMappingException {
        if (node.isTextual()) {
            try {
                String text = node.textValue();
                if (type == LocalDate.class) return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
                if (type == LocalTime.class) return LocalTime.parse(text, DateTimeFormatter.ISO_LOCAL_TIME);
                // A wall date-time has no offset or zone. Never route it through
                // an instant parser or mapper-wide date/time coercion settings.
                if (type == LocalDateTime.class) return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                // Parsing an offset date-time (not Instant.parse) rejects leap
                // seconds rather than silently normalizing second 60 to 59.
                Instant instant = OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
                requireMillis(instant);
                return instant;
            } catch (DateTimeException | ArithmeticException invalid) {
                // Do not attach the parse exception: its message contains input.
            }
        }
        throw ModelScalarJson.invalid(owner, field, type == Instant.class ? "offset Timestamp at millisecond precision" : type.getSimpleName() + " ISO string");
    }

    static void writeField(Object owner, String field, Object value, JsonGenerator output) throws IOException {
        String wire;
        try {
            if (value instanceof LocalDate date) wire = DateTimeFormatter.ISO_LOCAL_DATE.format(date);
            else if (value instanceof LocalTime time) wire = DateTimeFormatter.ISO_LOCAL_TIME.format(time);
            else if (value instanceof LocalDateTime dateTime) wire = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime);
            else {
                Instant instant = (Instant) value;
                requireMillis(instant);
                wire = instant.toString();
            }
        } catch (DateTimeException | ArithmeticException invalid) {
            throw ModelScalarJson.invalid(owner, field, "representable temporal value without precision loss");
        }
        output.writeStringField(field, wire);
    }

    private static void requireMillis(Instant instant) {
        if (instant.getNano() % 1_000_000 != 0) throw new DateTimeException("Timestamp precision exceeds milliseconds");
        instant.toEpochMilli(); // Refuse instants outside the signed epoch-millisecond carrier.
    }
}
