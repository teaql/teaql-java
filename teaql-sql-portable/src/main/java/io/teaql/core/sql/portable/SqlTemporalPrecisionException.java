package io.teaql.core.sql.portable;

import io.teaql.core.TeaQLRuntimeException;
import io.teaql.core.checker.ObjectLocation;

/** Value-free diagnostic for stored DateTime values incompatible with SQL precision. */
public final class SqlTemporalPrecisionException extends TeaQLRuntimeException {
    private static final String CODE = "SQL_DATETIME_PRECISION_MISMATCH";
    private static final String REMEDY = "Migrate the stored DateTime field to millisecond precision under an explicit data-migration policy, then retry. No data was automatically rewritten.";
    private final String entityType;
    private final String field;

    SqlTemporalPrecisionException(String entityType, String field) {
        super("[" + CODE + "] Stored SQL DateTime for "
                + (entityType == null ? "the modeled entity" : entityType)
                + (field == null ? "" : "." + field)
                + " has sub-millisecond precision; expected milliseconds. " + REMEDY);
        this.entityType = entityType;
        this.field = field;
    }

    public String getCode() { return CODE; }
    public String getEntityType() { return entityType; }
    /** Modeled in-process property name, never the stored value. */
    public String getField() { return field; }
    /** RFC 6901 pointer under the default JSON naming policy; empty means root. */
    public String getPath() { return field == null ? "" : ObjectLocation.hashRoot(field).instancePath(); }
    public String getExpectedPrecision() { return "milliseconds"; }
    public boolean isMigrationRequired() { return true; }
    public String getSuggestedFix() { return REMEDY; }
}
