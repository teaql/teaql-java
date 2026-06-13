package io.teaql.core.meta;

public interface EntityProperty {
    String getName();
    PropertyType getType();
    EntityDescriptor getOwner();
}
