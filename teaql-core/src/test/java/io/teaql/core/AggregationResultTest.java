package io.teaql.core;

import org.junit.Test;
import java.util.*;

import static org.junit.Assert.*;

public class AggregationResultTest {

    @Test
    public void testGetPropagateDimensionValues() {
        AggregationResult result = new AggregationResult();
        result.setName("Test");
        assertEquals("Test", result.getName());
        
        AggregationItem item1 = new AggregationItem();
        Map<SimpleNamedExpression, Object> dim1 = new HashMap<>();
        dim1.put(new SimpleNamedExpression("propA"), "valA1");
        dim1.put(new SimpleNamedExpression("propB"), "valB1");
        item1.setDimensions(dim1);
        
        AggregationItem item2 = new AggregationItem();
        Map<SimpleNamedExpression, Object> dim2 = new HashMap<>();
        dim2.put(new SimpleNamedExpression("propA"), "valA2");
        item2.setDimensions(dim2);
        
        result.setData(Arrays.asList(item1, item2));
        assertEquals(2, result.getData().size());
        
        List<Object> propAValues = result.getPropagateDimensionValues("propA");
        assertEquals(2, propAValues.size());
        assertTrue(propAValues.contains("valA1"));
        assertTrue(propAValues.contains("valA2"));
        
        List<Object> propBValues = result.getPropagateDimensionValues("propB");
        assertEquals(1, propBValues.size());
        assertEquals("valB1", propBValues.get(0));
        
        List<Object> propCValues = result.getPropagateDimensionValues("propC");
        assertTrue(propCValues.isEmpty());
    }
    
    @Test
    public void testToNumber() {
        AggregationResult result = new AggregationResult();
        
        // Null data
        assertEquals(Integer.valueOf(10), result.toNumber(10));
        
        // Empty data
        result.setData(new ArrayList<>());
        assertEquals(Integer.valueOf(10), result.toNumber(10));
        
        AggregationItem item = new AggregationItem();
        result.setData(Arrays.asList(item));
        
        // Empty values
        assertEquals(Integer.valueOf(10), result.toNumber(10));
        
        Map<SimpleNamedExpression, Object> values = new LinkedHashMap<>(); // Use LinkedHashMap for predictable first item
        item.setValues(values);
        
        // Null first value
        values.put(new SimpleNamedExpression("val"), null);
        assertEquals(Integer.valueOf(10), result.toNumber(10));
        
        // Number value
        values.clear();
        values.put(new SimpleNamedExpression("val"), 25.5);
        assertEquals(25.5, result.toNumber(10).doubleValue(), 0.001);
        
        // String value (Convert)
        values.clear();
        values.put(new SimpleNamedExpression("val"), "42");
        assertEquals(42L, result.toNumber(10).longValue());
    }
    
    @Test
    public void testToInt() {
        AggregationResult result = new AggregationResult();
        AggregationItem item = new AggregationItem();
        Map<SimpleNamedExpression, Object> values = new HashMap<>();
        values.put(new SimpleNamedExpression("val"), "100");
        item.setValues(values);
        result.setData(Arrays.asList(item));
        
        assertEquals(100, result.toInt());
    }
    
    @Test
    public void testToSimpleMap() {
        AggregationResult result = new AggregationResult();
        AggregationItem item1 = new AggregationItem();
        
        // Empty dimensions / values
        result.setData(Arrays.asList(item1));
        assertTrue(result.toSimpleMap().isEmpty());
        
        Map<SimpleNamedExpression, Object> dims = new LinkedHashMap<>();
        dims.put(new SimpleNamedExpression("dim"), "dimVal");
        item1.setDimensions(dims);
        assertTrue(result.toSimpleMap().isEmpty());
        
        Map<SimpleNamedExpression, Object> vals = new LinkedHashMap<>();
        vals.put(new SimpleNamedExpression("val"), 42);
        item1.setValues(vals);
        
        Map<Object, Number> map = result.toSimpleMap();
        assertEquals(1, map.size());
        assertEquals(42, map.get("dimVal").intValue());
        
        // Null dimension value
        dims.clear();
        dims.put(new SimpleNamedExpression("dim2"), null);
        assertTrue(result.toSimpleMap().isEmpty());
    }
    
    @Test
    public void testToLists() {
        AggregationResult result = new AggregationResult();
        AggregationItem item = new AggregationItem();
        
        Map<SimpleNamedExpression, Object> dims = new HashMap<>();
        dims.put(new SimpleNamedExpression("dim1"), "dimVal1");
        item.setDimensions(dims);
        
        Map<SimpleNamedExpression, Object> vals = new HashMap<>();
        vals.put(new SimpleNamedExpression("val1"), 100);
        item.setValues(vals);
        
        result.setData(Arrays.asList(item));
        
        List<Map<String, Object>> valueList = result.valueList();
        assertEquals(1, valueList.size());
        assertEquals(100, valueList.get(0).get("val1"));
        assertNull(valueList.get(0).get("dim1"));
        
        List<Map<String, Object>> list = result.toList();
        assertEquals(1, list.size());
        assertEquals(100, list.get(0).get("val1"));
        assertEquals("dimVal1", list.get(0).get("dim1"));
    }
}
