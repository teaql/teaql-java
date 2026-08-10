package io.teaql.core;

import org.junit.Test;
import java.util.*;
import io.teaql.data.dynamic.DynamicFieldSelection;

import static org.junit.Assert.*;

public class SearchRequestTest {

    static class DummySearchRequest implements SearchRequest<Entity> {
        private String partitionProperty;
        private Slice slice = new Slice();
        private OrderBys orderBys;
        private SearchCriteria searchCriteria;
        private Aggregations aggregations;
        private List<SimpleNamedExpression> projections = new ArrayList<>();
        private List<SimpleNamedExpression> simpleDynamicProperties = new ArrayList<>();
        private Map<String, Object> extensions;

        @Override public Class<? extends Entity> returnType() { return null; }
        @Override public String comment() { return null; }
        @Override public String getPartitionProperty() { return partitionProperty; }
        @Override public void setPartitionProperty(String propertyName) { this.partitionProperty = propertyName; }
        @Override public List<SimpleNamedExpression> getProjections() { return projections; }
        @Override public List<SimpleNamedExpression> getSimpleDynamicProperties() { return simpleDynamicProperties; }
        @Override public SearchCriteria getSearchCriteria() { return searchCriteria; }
        @Override public Aggregations getAggregations() { return aggregations; }
        @Override public Map<String, SearchRequest> getPropagateAggregations() { return null; }
        @Override public Map<String, SearchRequest> getPropagateDimensions() { return null; }
        @Override public OrderBys getOrderBy() { return orderBys; }
        @Override public Slice getSlice() { return slice; }
        @Override public Map<String, SearchRequest> enhanceRelations() { return null; }
        @Override public Map<String, SearchRequest> enhanceChildren() { return null; }
        @Override public List<SimpleAggregation> getDynamicAggregateAttributes() { return null; }
        @Override public SearchRequest<Entity> appendSearchCriteria(SearchCriteria searchCriteria) { return this; }
        @Override public List<FacetRequest> getFacetRequests() { return null; }
        @Override public Map<String, Object> getExtensions() { return extensions; }

        public void setSlice(Slice slice) { this.slice = slice; }
        public void setOrderBys(OrderBys orderBys) { this.orderBys = orderBys; }
        public void setSearchCriteria(SearchCriteria searchCriteria) { this.searchCriteria = searchCriteria; }
        public void setAggregations(Aggregations aggregations) { this.aggregations = aggregations; }
        public void setExtensions(Map<String, Object> ext) { this.extensions = ext; }
    }

    @Test
    public void testDefaultMethods() {
        DummySearchRequest req = new DummySearchRequest();
        
        // getTypeName
        assertEquals("DummySearch", req.getTypeName());
        
        // getExtensions / getExtension
        assertNull(req.getExtension("key"));
        Map<String, Object> ext = new HashMap<>();
        ext.put("key", "val");
        req.setExtensions(ext);
        assertEquals("val", req.getExtension("key"));
        assertNull(req.getExtension("other"));
        
        // getSearchForText
        assertNull(req.getSearchForText());
        
        // getDynamicFieldSelection
        assertNull(req.getDynamicFieldSelection());
        
        // purpose
        assertNull(req.purpose());
        
        // tryUseSubQuery
        assertTrue(req.tryUseSubQuery());
        
        // tryCacheAggregation
        assertFalse(req.tryCacheAggregation());
        
        // getAggregateCacheTime
        assertEquals(0L, req.getAggregateCacheTime());
    }
    
    @Test
    public void testHasSimpleAgg() {
        DummySearchRequest req = new DummySearchRequest();
        assertFalse(req.hasSimpleAgg()); // null aggregations
        
        Aggregations aggs = new Aggregations();
        req.setAggregations(aggs);
        assertFalse(req.hasSimpleAgg()); // empty aggregates
        
        aggs.getAggregates().add(new SimpleNamedExpression("func"));
        assertTrue(req.hasSimpleAgg()); // has aggregates
    }

    @Test
    public void testDataProperties() {
        DummySearchRequest req = new DummySearchRequest();
        UserContext ctx = null; // assume properties(ctx) ignores ctx in simple test mock
        
        // Empty
        List<String> props = req.dataProperties(ctx);
        assertTrue(props.isEmpty());
        
        // Add projection
        SimpleNamedExpression proj = new SimpleNamedExpression("prop1");
        req.getProjections().add(proj);
        props = req.dataProperties(ctx);
        assertTrue(props.contains("prop1"));
        
        // Add dynamic properties
        SimpleNamedExpression dynProp = new SimpleNamedExpression("dynProp");
        req.getSimpleDynamicProperties().add(dynProp);
        props = req.dataProperties(ctx);
        assertTrue(props.contains("dynProp"));
        
        // Add partition property with non-zero size slice
        req.setPartitionProperty("partProp");
        props = req.dataProperties(ctx);
        assertTrue(props.contains("partProp"));
        
        // Zero size slice
        Slice zeroSlice = new Slice();
        zeroSlice.setSize(0);
        req.setSlice(zeroSlice);
        props = req.dataProperties(ctx);
        assertFalse(props.contains("partProp")); // should not be added if size == 0
        
        // Add order by
        OrderBys orderBys = new OrderBys();
        orderBys.addOrderBy(new OrderBy("orderProp", "DESC"));
        req.setOrderBys(orderBys);
        props = req.dataProperties(ctx);
        assertTrue(props.contains("orderProp"));
        
        // Add search criteria
        SearchCriteria criteria = new SearchCriteria() {
            @Override public List<String> properties(UserContext ctx) { return Arrays.asList("critProp"); }
        };
        req.setSearchCriteria(criteria);
        props = req.dataProperties(ctx);
        assertTrue(props.contains("critProp"));
    }

    @Test
    public void testAggregationProperties() {
        DummySearchRequest req = new DummySearchRequest();
        Aggregations aggs = new Aggregations();
        SimpleNamedExpression dim = new SimpleNamedExpression("dimProp");
        aggs.getSimpleDimensions().add(dim);
        req.setAggregations(aggs);
        
        UserContext ctx = null;
        List<String> props = req.aggregationProperties(ctx);
        assertTrue(props.contains("dimProp"));
        
        SearchCriteria criteria = new SearchCriteria() {
            @Override public List<String> properties(UserContext ctx) { return Arrays.asList("critPropAgg"); }
        };
        req.setSearchCriteria(criteria);
        props = req.aggregationProperties(ctx);
        assertTrue(props.contains("critPropAgg"));
    }

    @Test
    public void testDataPropertiesWithNulls() {
        SearchRequest<Entity> reqNull = new DummySearchRequest() {
            @Override public List<SimpleNamedExpression> getProjections() { return null; }
            @Override public List<SimpleNamedExpression> getSimpleDynamicProperties() { return null; }
            @Override public SearchCriteria getSearchCriteria() { return null; }
            @Override public String getPartitionProperty() { return null; }
            @Override public OrderBys getOrderBy() { return null; }
            @Override public Aggregations getAggregations() { return new Aggregations(); }
        };
        UserContext ctx = null;
        assertTrue(reqNull.dataProperties(ctx).isEmpty());
        assertTrue(reqNull.aggregationProperties(ctx).isEmpty());
    }
}
