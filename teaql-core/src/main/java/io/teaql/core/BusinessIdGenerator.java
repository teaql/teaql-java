package io.teaql.core;

import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;

/**
 * Business ID Generator Interface.
 * Generates string IDs with rules and business semantics, such as order numbers or tracking numbers.
 */
public interface BusinessIdGenerator {

    /**
     * Generate a business string ID.
     *
     * @param ctx          the current user context
     * @param entity       the entity instance being operated on
     * @param entityDesc   the entity metadata descriptor
     * @param propertyDesc the property metadata descriptor for the ID field
     * @return the formatted business sequence number
     */
    String generateBusinessId(UserContext ctx, Entity entity, EntityDescriptor entityDesc, PropertyDescriptor propertyDesc);
}
