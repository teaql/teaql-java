package io.teaql.core.utils;

import java.util.HashMap;
import java.util.Map;

public class RowKeyTable<R, C, V> {
    private final Map<R, Map<C, V>> table = new HashMap<>();

    public void put(R row, C col, V val) {
        table.computeIfAbsent(row, k -> new HashMap<>()).put(col, val);
    }

    public V get(R row, C col) {
        Map<C, V> rowMap = table.get(row);
        if (rowMap == null) {
            return null;
        }
        return rowMap.get(col);
    }

    public V remove(R row, C col) {
        Map<C, V> rowMap = table.get(row);
        if (rowMap == null) {
            return null;
        }
        V val = rowMap.remove(col);
        if (rowMap.isEmpty()) {
            table.remove(row);
        }
        return val;
    }
}
