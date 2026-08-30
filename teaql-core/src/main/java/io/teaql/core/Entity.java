package io.teaql.core;

import java.util.List;
import io.teaql.data.dynamic.DynamicFieldValues;

// the super interface in TEAQL repository
public interface Entity {
    Long getId();

    Long getVersion();

    default String typeName() {
        return this.getClass().getSimpleName();
    }

    default String runtimeType() {
        return typeName();
    }

    ;

    default void setRuntimeType(String runtimeType) {
    }


    boolean newItem();

    boolean updateItem();

    boolean deleteItem();

    default boolean recoverItem() {
        return false;
    }

    boolean needPersist();

    default <T> T getProperty(String propertyName) {
        if (this instanceof BaseEntity) {
            return (T) ((BaseEntity) this).__internalGet(propertyName);
        }
        throw new UnsupportedOperationException(
                "Generic property access is only available on BaseEntity implementations");
    }

    default void setProperty(String propertyName, Object value) {
        if (!(this instanceof BaseEntity be)) {
            throw new UnsupportedOperationException(
                    "Generic property assignment is only available on BaseEntity implementations");
        }
        be.__internalSet(propertyName, value);
    }

    default Entity updateProperty(String propertyName, Object value) {
        setProperty(propertyName, value);
        return this;
    }

    List<String> getUpdatedProperties();

    void addRelation(String relationName, Entity value);

    void addDynamicProperty(String propertyName, Object value);

    void appendDynamicProperty(String propertyName, Object value);

    <T> T getDynamicProperty(String propertyName);

    /**
     * Returns the dynamic field values wrapper for this entity.
     * Dynamic fields use the '#' prefix namespace in additionalInfo.
     *
     * @return DynamicFieldValues wrapper, or empty if no dynamic fields are loaded
     */
    default DynamicFieldValues dynamicFields() {
        return DynamicFieldValues.empty();
    }

    Entity markForDeletion();

    void markAsRecover();

    default String getComment() {
        return null;
    }

    default void setComment(String comment) {
    }

    /**
     * Declares the business action being performed on this entity.
     * This comment flows into the audit log and trace chain.
     * When Triple-Intent enforcement is enabled, saving without auditAs() will be rejected.
     *
     * @param action a human-readable description of the mutation intent
     * @return this entity for fluent chaining: entity.auditAs("...").save(context)
     */
    @SuppressWarnings("unchecked")
    default <T extends Entity> Audited<T> auditAs(String action) {
        return new Audited<>((T) this, action);
    }


    default String getTraceChain() {
        return null;
    }

    default void setTraceChain(String traceChain) {
    }
}
