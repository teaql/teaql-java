package io.teaql.core.blob;

public enum BlobState {
    INITIATED, UPLOADED_UNBOUND, AVAILABLE, DELETING, DELETED;
    public String wireName() { return name().toLowerCase(java.util.Locale.ROOT); }
}
