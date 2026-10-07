package io.teaql.provider.jdbc;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class JdbcColumnRowTest {
    @Test public void nullAbsenceAndRealValuesRemainDistinct() {
        var row=new JdbcColumnRow(new JdbcColumnRow.Layout(new String[]{"null","zero","false","empty"}),new Object[]{null,0,false,""});
        assertTrue(row.containsKey("null"));assertNull(row.get("null"));
        assertFalse(row.containsKey("missing"));assertNull(row.get("missing"));
        assertEquals(0,row.get("zero"));assertEquals(false,row.get("false"));assertEquals("",row.get("empty"));
        var reference=new HashMap<String,Object>();reference.put("null",null);reference.put("zero",0);reference.put("false",false);reference.put("empty","");
        assertEquals(reference,row);assertEquals(row,reference);assertEquals(reference.hashCode(),row.hashCode());
    }
    @Test public void duplicateAndNullLabelsKeepLastColumnLikeHashMap() {
        String[] labels={"id","id",null};var layout=new JdbcColumnRow.Layout(labels);labels[0]="changed";
        var row=new JdbcColumnRow(layout,new Object[]{1,2,3});
        assertEquals(2,row.size());assertEquals(2,row.get("id"));assertEquals(3,row.get(null));assertFalse(row.containsKey("changed"));
        assertThrows(IllegalArgumentException.class,()->new JdbcColumnRow(layout,new Object[2]));
    }
    @Test public void valueAndGeometryChangesArePrivateToTheRow() {
        var layout=new JdbcColumnRow.Layout(new String[]{"id","note"});
        var first=new JdbcColumnRow(layout,new Object[]{1,null});var second=new JdbcColumnRow(layout,new Object[]{2,"private"});
        assertNull(first.put("note","changed"));assertEquals("private",second.get("note"));
        first.put("extra",null);assertTrue(first.containsKey("extra"));assertFalse(second.containsKey("extra"));
        assertEquals("changed",first.remove("note"));assertFalse(first.containsKey("note"));assertTrue(second.containsKey("note"));
        first.clear();assertTrue(first.isEmpty());assertEquals(2,second.size());
    }
    @Test public void entryValuesAndIteratorRemovalUseOnlyPrivatePayloads() {
        var row=new JdbcColumnRow(new JdbcColumnRow.Layout(new String[]{"id","note"}),new Object[]{1,"before"});
        var iterator=row.entrySet().iterator();var id=iterator.next();assertEquals(1,id.setValue(3));assertEquals(3,row.get("id"));
        var note=iterator.next();assertEquals("before",note.setValue("after"));iterator.remove();
        assertFalse(row.containsKey("note"));assertFalse(iterator.hasNext());assertThrows(IllegalStateException.class,iterator::remove);
        row.put("extra",5);assertEquals(Set.of("id","extra"),row.keySet());
        assertTrue(row.values().remove(5));assertFalse(row.containsKey("extra"));
    }
    @Test public void structuralChangesFailFastButValueUpdatesDoNot() {
        var row=new JdbcColumnRow(new JdbcColumnRow.Layout(new String[]{"id"}),new Object[]{1});
        var iterator=row.entrySet().iterator();row.put("id",2);assertEquals(2,iterator.next().getValue());
        iterator=row.entrySet().iterator();row.put("new",null);assertThrows(ConcurrentModificationException.class,iterator::hasNext);
    }
    @Test public void defaultMapOperationsPreserveNullMembershipAndViews() {
        var row=new JdbcColumnRow(new JdbcColumnRow.Layout(new String[]{"id","note"}),new Object[]{1,null});
        row.computeIfAbsent("note",key->"loaded");row.replaceAll((key,value)->value.toString());
        assertEquals(Map.of("id","1","note","loaded"),row);
        assertTrue(row.keySet().remove("id"));row.putAll(Map.of("x",3));assertEquals(2,row.size());
        assertTrue(row.entrySet().remove(new AbstractMap.SimpleEntry<>("x",3)));
        row.entrySet().clear();assertTrue(row.isEmpty());
    }
}
