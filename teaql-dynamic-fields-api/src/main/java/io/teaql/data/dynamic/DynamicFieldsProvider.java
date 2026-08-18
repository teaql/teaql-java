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
}
