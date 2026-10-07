package io.teaql.data.dynamic;

import java.util.List;
import java.util.Map;

public interface DynamicFieldsProvider {

    DynamicFieldDef loadFieldDef(DynamicFieldContext context, DynamicFieldRef ref);

    List<DynamicFieldDef> listFieldDefs(DynamicFieldContext context, String ownerType);

    DynamicFieldValues loadValues(DynamicFieldContext context, DynamicOwnerRef ownerRef,
                                  DynamicFieldSelection selection);

    Map<DynamicOwnerRef, DynamicFieldValues> loadValues(DynamicFieldContext context,
                                                        List<DynamicOwnerRef> ownerRefs,
                                                        DynamicFieldSelection selection);

    void saveValue(DynamicFieldContext context, DynamicSetCommand command);

    void deleteValue(DynamicFieldContext context, DynamicValueRef valueRef);

    DynamicFieldCapabilities capabilities();

    /** Prepare definitions without reading owner values. Durable providers attach provenance. */
    default DynamicFieldMetadata metadata(DynamicFieldContext context, String ownerType) {
        return DynamicFieldMetadata.fromDefinitions(listFieldDefs(context, ownerType));
    }

    /** A matching URL/DataSource is insufficient: the graph must own this exact active executor. */
    default boolean participatesInGraphTransaction(Object resource) { return false; }

    default void validateGraphMutation(DynamicFieldContext context, DynamicGraphMutation mutation) { }
}
