package io.teaql.provider.jdbc;

import java.util.*;

/** Query-local immutable column geometry and row-private values, never entity load state. */
final class JdbcColumnRow extends AbstractMap<String,Object> {
    static final class Layout {
        private final Map<String,Integer> indexes;
        private final String[] keys;
        private final int width;
        Layout(String[] labels) {
            var positions=new LinkedHashMap<String,Integer>();
            for(int index=0;index<labels.length;index++)positions.put(labels[index],index);
            indexes=Collections.unmodifiableMap(positions);
            keys=positions.keySet().toArray(String[]::new);
            width=labels.length;
        }
    }
    private final Layout layout;
    private final Object[] values;
    private Map<String,Object> expanded;
    private int modifications;
    JdbcColumnRow(Layout layout,Object[] ownedValues) {
        this.layout=Objects.requireNonNull(layout);
        if(ownedValues.length!=layout.width)throw new IllegalArgumentException("JDBC row width differs from column layout");
        values=ownedValues;
    }
    @Override public int size(){return expanded==null?layout.keys.length:expanded.size();}
    @Override public boolean containsKey(Object key){return expanded==null?layout.indexes.containsKey(key):expanded.containsKey(key);}
    @Override public Object get(Object key) {
        if(expanded!=null)return expanded.get(key);
        Integer index=layout.indexes.get(key);return index==null?null:values[index];
    }
    private Map<String,Object> expand() {
        if(expanded==null) {
            expanded=new HashMap<>();
            for(String key:layout.keys)expanded.put(key,getOriginal(key));
        }
        return expanded;
    }
    private Object getOriginal(String key){return values[layout.indexes.get(key)];}
    @Override public Object put(String key,Object value) {
        if(expanded==null) {
            Integer index=layout.indexes.get(key);
            if(index!=null){Object old=values[index];values[index]=value;return old;}
        }
        boolean present=containsKey(key);Object old=expand().put(key,value);
        if(!present)modifications++;return old;
    }
    @Override public Object remove(Object key) {
        if(!containsKey(key))return null;
        Object old=expand().remove(key);modifications++;return old;
    }
    @Override public void clear() {
        if(isEmpty())return;
        if(expanded==null)expanded=new HashMap<>();else expanded.clear();
        modifications++;
    }
    @Override public Set<Entry<String,Object>> entrySet() {
        return new AbstractSet<>() {
            @Override public int size(){return JdbcColumnRow.this.size();}
            @Override public void clear(){JdbcColumnRow.this.clear();}
            @Override public Iterator<Entry<String,Object>> iterator() {
                // Use a stable key snapshot only after this row's geometry has
                // changed. The normal readonly path shares layout.keys directly.
                String[] keys=expanded==null?layout.keys:expanded.keySet().toArray(String[]::new);
                return new Iterator<>() {
                    int position,expected=modifications;String last;boolean removable;
                    private void check(){if(expected!=modifications)throw new ConcurrentModificationException();}
                    @Override public boolean hasNext(){check();return position<keys.length;}
                    @Override public Entry<String,Object> next() {
                        if(!hasNext())throw new NoSuchElementException();
                        String key=keys[position++];last=key;removable=true;
                        return new Entry<>() {
                            public String getKey(){return key;}
                            public Object getValue(){return JdbcColumnRow.this.get(key);}
                            public Object setValue(Object value){return JdbcColumnRow.this.put(key,value);}
                            public boolean equals(Object other){return other instanceof Entry<?,?> e&&Objects.equals(key,e.getKey())&&Objects.equals(getValue(),e.getValue());}
                            public int hashCode(){return Objects.hashCode(key)^Objects.hashCode(getValue());}
                            public String toString(){return key+"="+getValue();}
                        };
                    }
                    @Override public void remove() {
                        check();if(!removable)throw new IllegalStateException();
                        JdbcColumnRow.this.remove(last);expected=modifications;removable=false;
                    }
                };
            }
        };
    }
}
