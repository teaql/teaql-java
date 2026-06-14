package io.teaql.core;

import java.lang.reflect.Method;
import java.util.List;

import io.teaql.core.utils.BeanUtil;
import io.teaql.core.utils.ReflectUtil;
import io.teaql.core.utils.StrUtil;

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
        return BeanUtil.getProperty(this, propertyName);
    }

    default void setProperty(String propertyName, Object value) {
        if (this instanceof BaseEntity) {
            ((BaseEntity) this).internalSet(propertyName, value);
        } else {
            BeanUtil.setProperty(this, propertyName, value);
        }
    }

    default Entity updateProperty(String propertyName, Object value) {
        Method method =
                ReflectUtil.getMethodByName(getClass(), "update" + StrUtil.upperFirst(propertyName));
        ReflectUtil.invoke(this, method, value);
        return this;
    }

    List<String> getUpdatedProperties();

    void addRelation(String relationName, Entity value);

    void addDynamicProperty(String propertyName, Object value);

    void appendDynamicProperty(String propertyName, Object value);

    <T> T getDynamicProperty(String propertyName);

    void markAsDeleted();

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
     * @return this entity for fluent chaining: entity.auditAs("...").save(ctx)
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
