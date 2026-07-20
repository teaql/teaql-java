package io.teaql.data.dynamic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DynamicFieldSelection {

    private final List<DynamicFieldSelectionEntry> entries = new ArrayList<>();
    private boolean selectAll;

    public DynamicFieldSelection selectString(String... codes) {
        for (String code : codes) {
            entries.add(new DynamicFieldSelectionEntry(code, DynamicDataType.STRING));
        }
        return this;
    }

    public DynamicFieldSelection selectNumber(String... codes) {
        for (String code : codes) {
            entries.add(new DynamicFieldSelectionEntry(code, DynamicDataType.NUMBER));
        }
        return this;
    }

    public DynamicFieldSelection selectBool(String... codes) {
        for (String code : codes) {
            entries.add(new DynamicFieldSelectionEntry(code, DynamicDataType.BOOL));
        }
        return this;
    }

    public DynamicFieldSelection selectDateTime(String... codes) {
        for (String code : codes) {
            entries.add(new DynamicFieldSelectionEntry(code, DynamicDataType.DATE_TIME));
        }
        return this;
    }

    public DynamicFieldSelection selectEnum(String... codes) {
        for (String code : codes) {
            entries.add(new DynamicFieldSelectionEntry(code, DynamicDataType.ENUM));
        }
        return this;
    }

    public DynamicFieldSelection selectAll() {
        this.selectAll = true;
        return this;
    }

    public boolean isSelectAll() {
        return selectAll;
    }

    public List<DynamicFieldSelectionEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public static final class DynamicFieldSelectionEntry {
        private final String code;
        private final DynamicDataType dataType;

        public DynamicFieldSelectionEntry(String code, DynamicDataType dataType) {
            this.code = code;
            this.dataType = dataType;
        }

        public String code() {
            return code;
        }

        public DynamicDataType dataType() {
            return dataType;
        }
    }
}
