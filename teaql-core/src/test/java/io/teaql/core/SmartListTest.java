package io.teaql.core;

import org.junit.Test;
import java.util.*;

import static org.junit.Assert.*;

public class SmartListTest {

    static class DummyEntity extends BaseEntity {
        public DummyEntity(Long id) { updateId(id); }
        @Override public String typeName() { return "dummy"; }
    }

    static class OtherEntity extends BaseEntity {
        @Override public String typeName() { return "other"; }
    }

    @Test
    public void typedEmptyListIsSharedPerEntityTypeAndImmutable() {
        SmartList<DummyEntity> first = SmartList.empty(DummyEntity.class);
        SmartList<DummyEntity> second = SmartList.empty(DummyEntity.class);
        SmartList<OtherEntity> other = SmartList.empty(OtherEntity.class);

        assertSame(first, second);
        assertNotSame(first, other);
        assertTrue(first.isEmpty());
        assertTrue(first.isSharedEmpty());
        assertFalse(new SmartList<DummyEntity>().isSharedEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> first.add(new DummyEntity(1L)));
        assertThrows(UnsupportedOperationException.class,
                () -> first.getData().add(new DummyEntity(1L)));
    }

    private UserContext createDummyContext() {
        return (UserContext) java.lang.reflect.Proxy.newProxyInstance(
            UserContext.class.getClassLoader(),
            new Class<?>[]{UserContext.class},
            (proxy, method, args) -> null
        );
    }

    @Test
    public void testBasicListOperations() {
        SmartList<DummyEntity> list = new SmartList<>();
        assertTrue(list.isEmpty());
        
        DummyEntity e1 = new DummyEntity(1L);
        DummyEntity e2 = new DummyEntity(2L);
        list.add(e1);
        list.add(e2);
        
        assertEquals(2, list.size());
        assertFalse(list.isEmpty());
        assertEquals(e1, list.first());
        assertEquals(e1, list.get(0));
        assertEquals(e2, list.get(1));
        
        list.set(1, e1);
        assertEquals(e1, list.get(1));
        
        SmartList<DummyEntity> list2 = new SmartList<>(Arrays.asList(e1, e2));
        assertEquals(2, list2.size());
        
        // test iterator and stream
        List<DummyEntity> iterated = new ArrayList<>();
        for (DummyEntity e : list2) iterated.add(e);
        assertEquals(2, iterated.size());
        assertEquals(2, list2.stream().count());
        
        list2.setData(new ArrayList<>());
        assertTrue(list2.isEmpty());
        assertTrue(list2.getData().isEmpty());
        
        SmartList<DummyEntity> list3 = new SmartList<>(null);
        assertTrue(list3.isEmpty());
    }
    
    @Test
    public void testTransformations() {
        SmartList<DummyEntity> list = new SmartList<>();
        DummyEntity e1 = new DummyEntity(1L);
        DummyEntity e2 = new DummyEntity(2L);
        list.add(e1);
        list.add(e2);
        
        Map<Long, DummyEntity> idMap = list.mapById();
        assertEquals(2, idMap.size());
        assertEquals(e1, idMap.get(1L));
        
        Map<String, List<DummyEntity>> grouped = list.groupBy(DummyEntity::typeName);
        assertEquals(1, grouped.size());
        assertEquals(2, grouped.get("dummy").size());
        
        List<Long> ids = list.toList(DummyEntity::getId);
        assertEquals(2, ids.size());
        assertEquals(1L, ids.get(0).longValue());
        
        Set<String> typeNames = list.toSet(DummyEntity::typeName);
        assertEquals(1, typeNames.size());
        
        Map<Long, DummyEntity> idMap2 = list.toIdentityMap(DummyEntity::getId);
        assertEquals(2, idMap2.size());
        
        list.removeIf(e -> e.getId() == 1L);
        assertEquals(1, list.size());
        assertEquals(2L, list.first().getId().longValue());
    }
    
    @Test
    public void testAggregationsAndTotalCount() {
        SmartList<DummyEntity> list = new SmartList<>();
        list.add(new DummyEntity(1L));
        list.add(new DummyEntity(2L));
        
        // No aggregations
        assertEquals(2, list.getTotalCount());
        
        AggregationResult res = new AggregationResult();
        AggregationItem item = new AggregationItem();
        Map<SimpleNamedExpression, Object> vals = new HashMap<>();
        vals.put(new SimpleNamedExpression(TeaQLConstants.ROOT_LIST_PARAMETER_NAME), 10);
        vals.put(new SimpleNamedExpression("stringProp"), "strValue");
        item.setValues(vals);
        res.setData(Arrays.asList(item));
        
        list.addAggregationResult(createDummyContext(), res);
        assertEquals(1, list.getAggregationResults().size());
        
        // Number properties (ROOT_LIST_PARAMETER_NAME)
        assertEquals(10, list.getTotalCount());
        
        Map<String, Object> allProps = list.aggregationProperties();
        assertEquals(2, allProps.size());
        
        Map<String, Object> numProps = list.aggregationNumberProperties();
        assertEquals(1, numProps.size());
        assertEquals(10, numProps.get(TeaQLConstants.ROOT_LIST_PARAMETER_NAME));
        
        list.setAggregationResults(new ArrayList<>());
        assertTrue(list.getAggregationResults().isEmpty());
        
        // test null property value
        AggregationResult resNull = new AggregationResult();
        AggregationItem itemNull = new AggregationItem();
        Map<SimpleNamedExpression, Object> valsNull = new HashMap<>();
        valsNull.put(new SimpleNamedExpression("nullProp"), null);
        itemNull.setValues(valsNull);
        resNull.setData(Arrays.asList(itemNull));
        list.addAggregationResult(createDummyContext(), resNull);
        
        assertTrue(list.aggregationProperties().containsKey("nullProp"));
        assertNull(list.aggregationProperties().get("nullProp"));
    }
    
    @Test(expected = IllegalStateException.class)
    public void testTotalCountException() {
        SmartList<DummyEntity> list = new SmartList<>();
        AggregationResult res = new AggregationResult();
        AggregationItem item = new AggregationItem();
        Map<SimpleNamedExpression, Object> vals = new HashMap<>();
        // Wrong type for count
        vals.put(new SimpleNamedExpression(TeaQLConstants.ROOT_LIST_PARAMETER_NAME), "not-a-number");
        item.setValues(vals);
        res.setData(Arrays.asList(item));
        list.addAggregationResult(createDummyContext(), res);
        
        list.getExactTotalCount();
    }
    
    @Test
    public void testTotalCountEmptyAgg() {
        SmartList<DummyEntity> list = new SmartList<>();
        list.add(new DummyEntity(1L));
        AggregationResult res = new AggregationResult();
        AggregationItem item = new AggregationItem();
        Map<SimpleNamedExpression, Object> vals = new HashMap<>();
        vals.put(new SimpleNamedExpression("otherProp"), 10);
        item.setValues(vals);
        res.setData(Arrays.asList(item));
        list.addAggregationResult(createDummyContext(), res);
        
        assertNull(list.getExactTotalCount());
        assertEquals(1, list.getTotalCountOrSize());
    }
    
    @Test
    public void testFacets() {
        SmartList<DummyEntity> list = new SmartList<>();
        SmartList<DummyEntity> facet = new SmartList<>();
        list.addFacet("f1", facet);
        
        assertEquals(1, list.getFacets().size());
        assertEquals(facet, list.getFacet("f1"));
        
        list.removeFacet("f1");
        assertTrue(list.getFacets().isEmpty());
        
        list.addFacet("f2", facet);
        list.clearFacets();
        assertTrue(list.getFacets().isEmpty());
        
        Map<String, SmartList<?>> facetMap = new HashMap<>();
        facetMap.put("f3", facet);
        list.setFacets(facetMap);
        assertEquals(1, list.getFacets().size());
    }
    
    @Test
    public void testSave() {
        SmartList<DummyEntity> list = new SmartList<>();
        list.save(createDummyContext());
    }

    @Test
    public void testTotalCountEmptyNumberProps() {
        SmartList<DummyEntity> list = new SmartList<>();
        list.add(new DummyEntity(1L));
        AggregationResult res = new AggregationResult();
        AggregationItem item = new AggregationItem();
        Map<SimpleNamedExpression, Object> vals = new HashMap<>();
        vals.put(new SimpleNamedExpression("stringProp"), "strValue");
        item.setValues(vals);
        res.setData(Arrays.asList(item));
        list.addAggregationResult(createDummyContext(), res);
        
        assertEquals(1, list.getTotalCount());
    }

    @Test
    public void testAggregationPropertiesWithClassEmpty() {
        SmartList<DummyEntity> list = new SmartList<>();
        assertTrue(list.aggregationProperties(Number.class).isEmpty());
    }

    @Test
    public void testTotalCountExceptionBranch() {
        SmartList<DummyEntity> list = new SmartList<>();
        AggregationItem item = new AggregationItem();
        item.addValue(new SimpleNamedExpression(TeaQLConstants.ROOT_LIST_PARAMETER_NAME), 1.5);
        AggregationResult result = new AggregationResult();
        result.setData(List.of(item));
        list.addAggregationResult(createDummyContext(), result);
        try {
            list.getTotalCount();
            fail("Expected exception");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("exact non-negative long"));
        }
    }
}
