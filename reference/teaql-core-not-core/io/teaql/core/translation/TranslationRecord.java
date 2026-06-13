package io.teaql.core.translation;

public class TranslationRecord {
    String key;
    String value;

    public TranslationRecord(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String pKey) {
        key = pKey;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String pValue) {
        value = pValue;
    }
}
