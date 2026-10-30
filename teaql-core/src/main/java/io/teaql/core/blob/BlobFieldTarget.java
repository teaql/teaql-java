package io.teaql.core.blob;

/** Canonical model position. An upload for a new row may precede server ID allocation. */
public record BlobFieldTarget(String entity, Long entityId, String field) {
    public BlobFieldTarget {
        if (!validName(entity) || !validName(field) || entityId != null && entityId <= 0) {
            throw new BlobException(BlobException.Code.BLOB_REFERENCE_INVALID);
        }
    }
    private static boolean validName(String value) {
        return value!=null && !value.isBlank() && value.equals(value.trim())
                && value.codePointCount(0,value.length())<=128 && value.codePoints().noneMatch(Character::isISOControl);
    }
}
