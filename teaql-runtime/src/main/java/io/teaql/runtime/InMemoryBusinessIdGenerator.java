package io.teaql.runtime;

import io.teaql.core.BusinessIdGenerator;
import io.teaql.core.Entity;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.utils.StrUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple in-memory implementation of BusinessIdGenerator.
 * It uses a ConcurrentHashMap to store AtomicLong sequences for each sequence key.
 * This guarantees uniqueness within a single process but is not suitable for clustered deployments.
 */
public class InMemoryBusinessIdGenerator implements BusinessIdGenerator {

    private final ConcurrentMap<String, AtomicLong> sequences = new ConcurrentHashMap<>();

    @Override
    public String generateBusinessId(UserContext ctx, Entity entity, EntityDescriptor entityDesc, PropertyDescriptor propertyDesc) {
        String rule = propertyDesc.getAdditionalInfo().get("business_id_rule");
        if (StrUtil.isEmpty(rule)) {
            throw new IllegalArgumentException("No business_id_rule defined in metadata for " + entityDesc.getType() + "." + propertyDesc.getName());
        }

        // Parse simple rule format: "PREFIX, LENGTH" (e.g., "ORD, 6")
        String[] parts = rule.split(",");
        String prefix = parts[0].trim();
        int length = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 6;

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequenceKey = prefix + ":" + dateStr;

        long seq = nextSequence(sequenceKey);
        
        return String.format("%s%s%0" + length + "d", prefix, dateStr, seq);
    }

    private long nextSequence(String sequenceKey) {
        return sequences.computeIfAbsent(sequenceKey, k -> new AtomicLong(0)).incrementAndGet();
    }
}
