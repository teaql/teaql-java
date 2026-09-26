package io.teaql.core.reference;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

/** Exact boundary wire shapes: a governed token string or raw diagnostic {id, version}. */
public final class ReferenceWireCodec {
    private ReferenceWireCodec() {}

    public static Object serialize(ExternalEntityReference reference) {
        if (reference instanceof ExternalEntityReference.Governed governed) {
            return governed.token();
        }
        ExternalEntityReference.Raw raw = (ExternalEntityReference.Raw) reference;
        return Map.of("id", raw.id(), "version", raw.version());
    }

    public static ExternalEntityReference deserialize(Object wire, ReferenceMode mode) {
        if (mode == ReferenceMode.GOVERNED && wire instanceof String token && token.startsWith("tqr1.")) {
            return new ExternalEntityReference.Governed(token);
        }
        if (mode == ReferenceMode.RAW && wire instanceof Map<?, ?> map && map.size() == 2) {
            Object id = map.get("id");
            Object version = map.get("version");
            Long exactId = exactLong(id);
            Long exactVersion = exactLong(version);
            if (exactId != null && exactVersion != null) {
                return new ExternalEntityReference.Raw(exactId, exactVersion);
            }
        }
        throw new RoundTripReferenceException(
                RoundTripReferenceErrorCode.INVALID_REFERENCE, "ROUND_TRIP_REFERENCE_INVALID");
    }

    private static Long exactLong(Object value) {
        try {
            if (value instanceof Byte || value instanceof Short
                    || value instanceof Integer || value instanceof Long) {
                return ((Number) value).longValue();
            }
            if (value instanceof BigInteger integer) return integer.longValueExact();
            if (value instanceof BigDecimal decimal) return decimal.longValueExact();
            return null;
        } catch (ArithmeticException ignored) {
            return null;
        }
    }
}
