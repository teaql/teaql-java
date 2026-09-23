package io.teaql.runtime;

import io.teaql.core.BusinessIdGenerator;
import io.teaql.core.Entity;
import io.teaql.core.UserContext;
import io.teaql.core.businessid.BusinessIdErrorCode;
import io.teaql.core.businessid.BusinessIdException;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.utils.StrUtil;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple in-memory implementation of BusinessIdGenerator.
 * It uses a ConcurrentHashMap to store AtomicLong sequences for each sequence key.
 * This guarantees uniqueness within a single process but is not suitable for clustered deployments.
 */
@Deprecated
public class InMemoryBusinessIdGenerator implements BusinessIdGenerator {

    private final ConcurrentMap<String, AtomicLong> sequences = new ConcurrentHashMap<>();

    @Override
    public String generateBusinessId(UserContext context, Entity entity, EntityDescriptor entityDesc, PropertyDescriptor propertyDesc) {
        String rule = propertyDesc.getAdditionalInfo().get("business_id_rule");
        if (StrUtil.isEmpty(rule)) {
            throw new IllegalArgumentException("No business_id_rule defined in metadata for " + entityDesc.getType() + "." + propertyDesc.getName());
        }

        // Parse simple rule format: "PREFIX, LENGTH" (e.g., "ORD, 6")
        String[] parts = rule.split(",");
        String prefix = parts[0].trim();
        int length = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 6;
        if (length < 1 || length > 18) {
            throw new IllegalArgumentException("Legacy Business ID digits must be between 1 and 18");
        }

        String dateStr = java.util.Objects.requireNonNull(context, "context")
                .businessDate().format(DateTimeFormatter.BASIC_ISO_DATE);
        String sequenceKey = prefix + ":" + dateStr;

        long maximum = 1;
        for (int i = 0; i < length; i++) maximum = Math.multiplyExact(maximum, 10);
        long seq = nextSequence(sequenceKey, maximum - 1);
        
        return String.format(java.util.Locale.ROOT,
                "%s%s%0" + length + "d", prefix, dateStr, seq);
    }

    private long nextSequence(String sequenceKey, long maximum) {
        AtomicLong counter = sequences.computeIfAbsent(sequenceKey, k -> new AtomicLong());
        while (true) {
            long current = counter.get();
            if (current >= maximum) {
                throw new BusinessIdException(
                        BusinessIdErrorCode.BUSINESS_ID_RANGE_EXHAUSTED,
                        "Business ID range exhausted for " + sequenceKey);
            }
            if (counter.compareAndSet(current, current + 1)) return current + 1;
        }
    }
}
