package io.teaql.core.sql.portable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Physical modeled DateTime precision. Does not mutate entities, clocks or codecs. */
public final class SqlTemporalPrecision {
    private SqlTemporalPrecision() { }

    public static LocalDateTime dateTime(LocalDateTime value) {
        return value == null || value.getNano() % 1_000_000 == 0
                ? value : value.truncatedTo(ChronoUnit.MILLIS);
    }

    private static Object parameter(Object value) {
        if (value instanceof LocalDateTime time) return dateTime(time);
        // SQL IN/array bindings are flat scalar collections, not arbitrary
        // object graphs. Never traverse JSON objects or recursively walk lists.
        if (value instanceof List<?> values) {
            List<Object> copy = null;
            for (int i=0;i<values.size();i++) {
                Object before=values.get(i);
                Object after=before instanceof LocalDateTime time ? dateTime(time) : before;
                if (before!=after) { if (copy==null) copy=new ArrayList<>(values);copy.set(i,after); }
            }
            return copy==null ? value : copy;
        }
        if (value instanceof LocalDateTime[] values) {
            LocalDateTime[] copy=null;
            for (int i=0;i<values.length;i++) {
                LocalDateTime after=dateTime(values[i]);
                if (values[i]!=after) { if (copy==null) copy=values.clone();copy[i]=after; }
            }
            return copy==null ? value : copy;
        }
        return value;
    }

    /** Copy only when precision changes; the caller's array/values remain owned by it. */
    public static Object[] parameters(Object[] values) {
        if (values==null) return null;
        Object[] copy=null;
        for (int i=0;i<values.length;i++) {
            Object after=parameter(values[i]);
            if (values[i]!=after) { if (copy==null) copy=values.clone();copy[i]=after; }
        }
        return copy==null ? values : copy;
    }

    public static List<Object[]> batch(List<Object[]> values) {
        if (values==null) return null;
        List<Object[]> copy=null;
        for (int i=0;i<values.size();i++) {
            Object[] before=values.get(i),after=parameters(before);
            if (before!=after) { if (copy==null) copy=new ArrayList<>(values);copy.set(i,after); }
        }
        return copy==null ? values : copy;
    }
}
