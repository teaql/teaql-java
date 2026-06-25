package io.teaql.data.dynamic;

import java.util.List;
import java.util.Map;

public interface DynamicFieldsProvider {

    DynamicFieldDef loadFieldDef(DynamicFieldContext ctx, DynamicFieldRef ref);

    List<DynamicFieldDef> listFieldDefs(DynamicFieldContext ctx, String ownerType);

    DynamicFieldValues loadValues(DynamicFieldContext ctx, DynamicOwnerRef ownerRef,
                                  DynamicFieldSelection selection);

    Map<DynamicOwnerRef, DynamicFieldValues> loadValues(DynamicFieldContext ctx,
                                                        List<DynamicOwnerRef> ownerRefs,
                                                        DynamicFieldSelection selection);

    void saveValue(DynamicFieldContext ctx, DynamicSetCommand command);

    void deleteValue(DynamicFieldContext ctx, DynamicValueRef valueRef);

    DynamicFieldCapabilities capabilities();
}
