package io.teaql.core;

import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.utils.SensitiveLogNames;

/** One field-policy interpretation for graph provenance and physical bindings. */
public final class SqlFieldLogPolicy {
    private SqlFieldLogPolicy() {}

    @FrameworkInternal("Diagnostic field policy, not an application authorization policy")
    public static SqlParameterLogPolicy resolve(EntityDescriptor entity, String name) {
        if (SensitiveLogNames.credential(name)) return SqlParameterLogPolicy.CREDENTIAL;
        for (var current = entity; current != null; current = current.getParent()) {
            for (var property : current.getOwnProperties()) {
                if (property.getName().equals(name)) return resolve(entity, property);
            }
        }
        return SqlParameterLogPolicy.UNKNOWN;
    }

    @FrameworkInternal("Diagnostic field policy, not an application authorization policy")
    public static SqlParameterLogPolicy resolve(EntityDescriptor entity, PropertyDescriptor property) {
        String name = property.getName();
        if (SensitiveLogNames.credential(name)) return SqlParameterLogPolicy.CREDENTIAL;
        var owner = property.getOwner();
        var declared = property.getAdditionalInfo().get("logPolicy");
        if (entity != null && entity.getAuditMaskFields().contains(name)
                || owner != null && owner.getAuditMaskFields().contains(name)
                || "masked".equalsIgnoreCase(declared)) return SqlParameterLogPolicy.MASKED;
        if ("credential".equalsIgnoreCase(declared)) return SqlParameterLogPolicy.CREDENTIAL;
        if ("plain".equalsIgnoreCase(declared)) return SqlParameterLogPolicy.PLAIN;
        var scope = owner == null ? entity : owner;
        return scope != null && scope.isAuditMaskFieldsDeclared() ? SqlParameterLogPolicy.PLAIN : SqlParameterLogPolicy.UNKNOWN;
    }
}
