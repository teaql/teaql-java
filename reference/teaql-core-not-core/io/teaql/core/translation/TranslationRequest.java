package io.teaql.core.translation;

import java.util.Set;

public class TranslationRequest {
    private Set<TranslationRecord> records;

    public TranslationRequest(Set<TranslationRecord> records) {
        this.records = records;
    }

    public Set<TranslationRecord> getRecords() {
        return records;
    }
}
