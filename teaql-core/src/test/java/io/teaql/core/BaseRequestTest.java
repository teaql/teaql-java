package io.teaql.core;

import io.teaql.core.criteria.*;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class BaseRequestTest {

    private BaseRequest.TempRequest request;

    @Before
    public void setUp() {
        request = new BaseRequest.TempRequest(BaseEntity.class, "BaseEntity");
    }

    @Test
    public void testPrefixMethods() {
        assertEquals("sumOfAmount", request.prefixSumOf("amount"));
        assertEquals("maxOfAmount", request.prefixMaxOf("amount"));
        assertEquals("minOfAmount", request.prefixMinOf("amount"));
        assertEquals("avarageOfAmount", request.prefixAvgOf("amount"));
        assertEquals("prefix", request.prefix("prefix", null));
        assertEquals("prefix", request.prefix("prefix", ""));
    }

    @Test
    public void testSelectSelfAllAny() {
        assertEquals(request, request.selectSelf());
        assertEquals(request, request.selectAll());
        assertEquals(request, request.selectAny());
    }

    @Test
    public void testSelectUnselectProperty() {
        request.selectProperty("name");
        assertEquals(1, request.getProjections().size());
        assertEquals("name", request.getProjections().get(0).name());

        request.selectProperty("age", AggrFunction.MAX);
        assertEquals(2, request.getProjections().size());
        
        request.unselectProperty("name");
        assertEquals(1, request.getProjections().size());
        
        request.unselectProperty(null);
        assertEquals(1, request.getProjections().size());
        
        request.selectProperty(null);
        assertEquals(1, request.getProjections().size());
    }

    @Test
    public void testEnhanceRelation() {
        BaseRequest.TempRequest subReq = new BaseRequest.TempRequest(BaseEntity.class, "SubEntity");
        request.enhanceRelation("parent", subReq);
        assertEquals(subReq, request.enhanceRelations().get("parent"));
        
        request.unselectProperty("parent");
        assertNull(request.enhanceRelations().get("parent"));
    }

    @Test
    public void testSearchCriteria() {
        assertNull(request.getSearchCriteria());
        SearchCriteria criteria1 = new EQ(new PropertyReference("name"), new Parameter("name", "test", Operator.EQUAL));
        request.appendSearchCriteria(criteria1);
        assertEquals(criteria1, request.getSearchCriteria());

        SearchCriteria criteria2 = new EQ(new PropertyReference("age"), new Parameter("age", 18, Operator.EQUAL));
        request.appendSearchCriteria(criteria2);
        assertTrue(request.getSearchCriteria() instanceof AND);

        request.appendSearchCriteria(null);
        assertTrue(request.getSearchCriteria() instanceof AND);
    }

    @Test
    public void testDeletedRows() {
        SearchCriteria criteria1 = new EQ(new PropertyReference("name"), new Parameter("name", "test", Operator.EQUAL));
        request.appendSearchCriteria(new VersionSearchCriteria(criteria1));
        request.withDeletedRows();
        assertNull(request.getSearchCriteria());

        request.deletedRowsOnly();
        assertTrue(request.getSearchCriteria() instanceof VersionSearchCriteria);
    }

    @Test
    public void testSliceAndOrderBy() {
        request.top(10);
        assertEquals(10, request.getSlice().getSize());

        request.offset(5, 20);
        assertEquals(5, request.getSlice().getOffset());
        assertEquals(20, request.getSlice().getSize());

        request.unlimited();
        assertNull(request.getSlice());
        
        request.setOffset(15);
        assertEquals(15, request.getSlice().getOffset());
        assertEquals(1000, request.getSize()); // size is default 1000
        request.setSize(25);
        assertEquals(25, request.getSize());

        request.addOrderByAscending("name");
        request.addOrderByDescending("age");
        request.addOrderByAscendingUsingGBK("title");
        request.addOrderByDescendingUsingGBK("desc");
        request.addOrderBy("status", true);
        request.addOrderBy("type", false);
        assertEquals(6, request.getOrderBy().getOrderBys().size());
    }

    @Test
    public void testAggregations() {
        request.count();
        request.count("countId");
        request.countProperty("age");
        request.countProperty("countAge", "age");
        request.sum("amount");
        request.sum("sumAmount", "amount");
        request.min("price");
        request.min("minPrice", "price");
        request.max("score");
        request.max("maxScore", "score");
        request.avg("rating");
        request.avg("avgRating", "rating");
        request.standardDeviation("sd", "score");
        request.squareRootOfPopulationStandardDeviation("spsd", "score");
        request.sampleVariance("sv", "score");
        request.samplePopulationVariance("spv", "score");
        
        request.standardDeviation("score");
        request.squareRootOfPopulationStandardDeviation("score");
        request.sampleVariance("score");
        request.samplePopulationVariance("score");

        assertEquals(16, request.getAggregations().getAggregates().size());
        
        // Add same aggregate again
        request.count("countId");
        assertEquals(16, request.getAggregations().getAggregates().size());
    }

    @Test
    public void testGroupByAndDynamic() {
        request.groupBy("type");
        request.groupBy("typeGroup", "type");
        assertEquals(2, request.getAggregations().getSimpleDimensions().size());

        BaseRequest.TempRequest subReq = new BaseRequest.TempRequest(BaseEntity.class, "Sub");
        request.groupBy("category", subReq);
        assertEquals(1, request.getAggregations().getComplexDimensions().size());
        assertEquals(subReq, request.getPropagateDimensions().get("category"));

        request.aggregate("orders", subReq);
        assertEquals(subReq, request.getPropagateAggregations().get("orders"));

        request.addSimpleDynamicProperty("dynProp", new PropertyReference("prop"));
        assertEquals(1, request.getSimpleDynamicProperties().size());

        request.addAggregateDynamicProperty("aggDyn", subReq);
        request.addSingleAggregateDynamicProperty("singleAgg", subReq);
        assertEquals(2, request.getDynamicAggregateAttributes().size());
    }

    @Test
    public void testExtensions() {
        request.putExtension("ext1", "val1");
        assertEquals("val1", request.getExtensions().get("ext1"));

        request.putExtension("ext1", null);
        assertNull(request.getExtensions().get("ext1"));

        Map<String, Object> exts = new HashMap<>();
        exts.put("ext2", "val2");
        request.setExtensions(exts);
        assertEquals("val2", request.getExtensions().get("ext2"));
        
        request.setExtensions(null);
        assertNotNull(request.getExtensions());
    }

    @Test
    public void testRefineOperator() {
        Object[] manyValues = new Object[21];
        for (int i = 0; i < 21; i++) manyValues[i] = i;
        Object[] fewValues = new Object[]{1, 2};
        Object singleValue = 1;

        assertEquals(Operator.IN_LARGE, request.refineOperator(Operator.EQUAL, manyValues));
        assertEquals(Operator.IN, request.refineOperator(Operator.EQUAL, fewValues));
        assertEquals(Operator.EQUAL, request.refineOperator(Operator.EQUAL, singleValue));
        
        assertEquals(Operator.IN_LARGE, request.refineOperator(Operator.IN, manyValues));
        assertEquals(Operator.IN, request.refineOperator(Operator.IN, fewValues));
        assertEquals(Operator.EQUAL, request.refineOperator(Operator.IN, singleValue));

        assertEquals(Operator.IN_LARGE, request.refineOperator(Operator.IN_LARGE, manyValues));
        assertEquals(Operator.IN, request.refineOperator(Operator.IN_LARGE, fewValues));
        assertEquals(Operator.EQUAL, request.refineOperator(Operator.IN_LARGE, singleValue));

        assertEquals(Operator.NOT_IN_LARGE, request.refineOperator(Operator.NOT_EQUAL, manyValues));
        assertEquals(Operator.NOT_IN, request.refineOperator(Operator.NOT_EQUAL, fewValues));
        assertEquals(Operator.NOT_EQUAL, request.refineOperator(Operator.NOT_EQUAL, singleValue));
        
        assertEquals(Operator.NOT_IN_LARGE, request.refineOperator(Operator.NOT_IN, manyValues));
        assertEquals(Operator.NOT_IN, request.refineOperator(Operator.NOT_IN, fewValues));
        assertEquals(Operator.NOT_EQUAL, request.refineOperator(Operator.NOT_IN, singleValue));
        
        assertEquals(Operator.NOT_IN_LARGE, request.refineOperator(Operator.NOT_IN_LARGE, manyValues));
        assertEquals(Operator.NOT_IN, request.refineOperator(Operator.NOT_IN_LARGE, fewValues));
        assertEquals(Operator.NOT_EQUAL, request.refineOperator(Operator.NOT_IN_LARGE, singleValue));
        
        // default case
        assertEquals(Operator.GREATER_THAN, request.refineOperator(Operator.GREATER_THAN, singleValue));
    }

    @Test
    public void testPurpose() {
        request.setSearchForText("text");
        assertEquals("text", request.getSearchForText());

        request.internalComment("Load tasks").internalPurpose("Display board");
        assertEquals("Load tasks", request.comment());
        assertEquals("Display board", request.purpose());
        
        ExecutableRequest<?> exec = request.purpose("New Purpose");
        assertNotNull(exec);
        assertEquals("New Purpose", request.purpose());
    }

    @Test(expected = TeaQLRuntimeException.class)
    public void testPurposeWithoutComment() {
        new BaseRequest.TempRequest(BaseEntity.class, "BaseEntity").purpose("Display");
    }

    @Test
    public void testEnhanceChildren() {
        BaseRequest.TempRequest subReq = new BaseRequest.TempRequest(BaseEntity.class, "ChildEntity");
        request.enhanceSelf(subReq);
        assertEquals(subReq, request.enhanceChildren().get("ChildEntity"));
        
        // Enhance self with same type should be ignored
        request.enhanceSelf(request);
        assertEquals(1, request.enhanceChildren().size());
    }
    @Test
    public void testEqualsAndHashCode() {
        BaseRequest.TempRequest req1 = new BaseRequest.TempRequest(BaseEntity.class, "BaseEntity");
        BaseRequest.TempRequest req2 = new BaseRequest.TempRequest(BaseEntity.class, "BaseEntity");

        // Identity and type
        assertTrue(req1.equals(req1));
        assertFalse(req1.equals(null));
        assertFalse(req1.equals(new Object()));

        req1.searchCriteria = null;
        req2.searchCriteria = null;
        req2.aggregations = req1.aggregations;
        req2.slice = req1.slice;

        assertTrue(req1.equals(req2));

        // projections
        req1.selectProperty("name");
        assertFalse(req1.equals(req2));
        req2.selectProperty("name");
        assertTrue(req1.equals(req2));

        // simpleDynamicProperties
        req1.addSimpleDynamicProperty("dyn", new PropertyReference("prop"));
        assertFalse(req1.equals(req2));
        req2.addSimpleDynamicProperty("dyn", new PropertyReference("prop"));
        assertTrue(req1.equals(req2));

        // searchCriteria
        SearchCriteria criteria = new EQ(new PropertyReference("id"), new Parameter("id", 1, Operator.EQUAL));
        req1.appendSearchCriteria(criteria);
        assertFalse(req1.equals(req2));
        req2.appendSearchCriteria(criteria);
        assertTrue(req1.equals(req2));

        // orderBys
        req1.addOrderByAscending("id");
        assertFalse(req1.equals(req2));
        req2.addOrderByAscending("id");
        assertTrue(req1.equals(req2));

        // slice
        Slice oldSlice = req2.slice;
        req2.slice = new Slice();
        assertFalse(req1.equals(req2));
        req2.slice = oldSlice;
        assertTrue(req1.equals(req2));

        // enhanceRelations
        BaseRequest.TempRequest relReq = new BaseRequest.TempRequest(BaseEntity.class, "Rel");
        req1.enhanceRelation("parent", relReq);
        assertFalse(req1.equals(req2));
        req2.enhanceRelation("parent", relReq);
        assertTrue(req1.equals(req2));

        // dynamicAggregateAttributes
        req1.addAggregateDynamicProperty("agg", relReq);
        assertFalse(req1.equals(req2));
        req2.addAggregateDynamicProperty("agg", relReq);
        assertTrue(req1.equals(req2));

        // partitionProperty
        req1.setPartitionProperty("part");
        assertFalse(req1.equals(req2));
        req2.setPartitionProperty("part");
        assertTrue(req1.equals(req2));

        // returnType
        BaseRequest.TempRequest diffTypeReq = new BaseRequest.TempRequest(io.teaql.core.Entity.class, "ObjectLocation");
        assertFalse(req1.equals(diffTypeReq));

        // aggregations
        Aggregations oldAgg = req2.aggregations;
        req2.aggregations = new Aggregations();
        assertFalse(req1.equals(req2));
        req2.aggregations = oldAgg;
        assertTrue(req1.equals(req2));

        // propagateAggregations
        req1.aggregate("orders", relReq);
        assertFalse(req1.equals(req2));
        req2.aggregate("orders", relReq);
        assertTrue(req1.equals(req2));

        // propagateDimensions
        req1.groupBy("cat", relReq);
        assertFalse(req1.equals(req2));
        req2.groupBy("cat", relReq);
        assertTrue(req1.equals(req2));

        // enhanceChildren
        req1.enhanceSelf(relReq);
        assertFalse(req1.equals(req2));
        req2.enhanceSelf(relReq);
        assertTrue(req1.equals(req2));

        // cacheAggregation
        req1.enableAggregationCache();
        assertFalse(req1.equals(req2));
        req2.enableAggregationCache();
        assertTrue(req1.equals(req2));
        
        req1.disableAggregationCache();
        assertFalse(req1.equals(req2));
        req2.disableAggregationCache();
        assertTrue(req1.equals(req2));

        // aggregateCacheTime
        req1.propagateAggregationCache(100L);
        assertFalse(req1.equals(req2));
        req2.propagateAggregationCache(100L);
        assertTrue(req1.equals(req2));
        
        assertEquals(req1.hashCode(), req2.hashCode());
    }

    @Test
    public void testInternalMatchAny() {
        BaseRequest.TempRequest req1 = new BaseRequest.TempRequest(BaseEntity.class, "Req1");
        BaseRequest.TempRequest req2 = new BaseRequest.TempRequest(BaseEntity.class, "Req2");

        // this.searchCriteria == null
        req1.internalMatchAny(req2);
        assertNull(req1.getSearchCriteria());

        // anotherRequest.searchCriteria == null
        req1.appendSearchCriteria(new EQ(new PropertyReference("id"), new Parameter("id", 1, Operator.EQUAL)));
        req1.internalMatchAny(req2);
        assertTrue(req1.getSearchCriteria() instanceof EQ);

        // anotherRequest instanceof VersionSearchCriteria
        req2.appendSearchCriteria(new VersionSearchCriteria(new EQ(new PropertyReference("name"), new Parameter("name", "n", Operator.EQUAL))));
        req1.internalMatchAny(req2);
        assertTrue(req1.getSearchCriteria() instanceof EQ);

        // anotherRequest is NOT AND
        BaseRequest.TempRequest req3 = new BaseRequest.TempRequest(BaseEntity.class, "Req3");
        req3.searchCriteria = new EQ(new PropertyReference("age"), new Parameter("age", 18, Operator.EQUAL));
        req1.internalMatchAny(req3);
        assertEquals(2, ((AND) req1.getSearchCriteria()).getExpressions().size());

        // anotherRequest is AND
        BaseRequest.TempRequest req4 = new BaseRequest.TempRequest(BaseEntity.class, "Req4");
        AND andCriteria = new AND(new EQ(new PropertyReference("type"), new Parameter("type", "t", Operator.EQUAL)));
        req4.searchCriteria = andCriteria;
        req1.internalMatchAny(req4);
        
        // now req1's AND should have an OR added
        assertEquals(3, ((AND) req1.getSearchCriteria()).getExpressions().size());
        assertTrue(((AND) req1.getSearchCriteria()).getExpressions().get(2) instanceof OR);
    }

    @Test
    public void testRemoveTopVersionCriteria() {
        BaseRequest.TempRequest req = new BaseRequest.TempRequest(BaseEntity.class, "Req");
        
        // searchCriteria == null
        req.removeTopVersionCriteria();
        assertNull(req.getSearchCriteria());

        // instanceof VersionSearchCriteria
        req.searchCriteria = new VersionSearchCriteria(null);
        req.removeTopVersionCriteria();
        assertNull(req.getSearchCriteria());

        // !(searchCriteria instanceof AND)
        req.searchCriteria = new EQ(new PropertyReference("id"), new Parameter("id", 1, Operator.EQUAL));
        req.removeTopVersionCriteria();
        assertNotNull(req.getSearchCriteria());
        assertTrue(req.getSearchCriteria() instanceof EQ);

        // instanceof AND
        AND and = new AND(new VersionSearchCriteria(new EQ(new PropertyReference("foo"), new Parameter("foo", "bar", Operator.EQUAL))));
        and.getExpressions().add(new EQ(new PropertyReference("id"), new Parameter("id", 2, Operator.EQUAL)));
        req.searchCriteria = and;
        req.removeTopVersionCriteria();
        
        assertEquals(1, ((AND) req.getSearchCriteria()).getExpressions().size());
        assertTrue(((AND) req.getSearchCriteria()).getExpressions().get(0) instanceof EQ);
    }

    @Test
    public void testMissedBranches() {
        BaseRequest.TempRequest req = new BaseRequest.TempRequest(BaseEntity.class, "Req");
        
        // selectProperty empty cases
        req.selectProperty("", AggrFunction.MAX);
        req.selectProperty("name", null);
        req.unselectProperty("");
        
        // buildRequest map with and without dot
        Map<String, Object> m = new HashMap<>();
        m.put("validKey", "val");
        m.put("invalid.key", "val");
        req.buildRequest(m);
        
        // propagateAggregationCache
        BaseRequest.TempRequest subReq = new BaseRequest.TempRequest(BaseEntity.class, "SubReq");
        req.propagateAggregationCache(100L);
        req.addAggregateDynamicProperty("agg", subReq, true);
        
        // Operator SOUNDS_LIKE
        req.createBasicSearchCriteria("prop", Operator.SOUNDS_LIKE, new Object[]{"val"});
        
        // Operator BETWEEN with invalid args
        try {
            req.createBasicSearchCriteria("prop", Operator.BETWEEN, new Object[]{1});
        } catch (TeaQLRuntimeException e) {}
        
        // setOffset when slice is null
        req.unlimited();
        req.setOffset(5);
        
        // addFacet
        req.addFacet("facet", "rel", subReq, false);
        assertEquals(1, req.getFacetRequests().size());
    }

    @Test
    public void testMetaFactoryBranches() {
        BaseRequest.TempRequest req = new BaseRequest.TempRequest(BaseEntity.class, "Req");
        
        // getProperty when EntityMetaFactory is null
        io.teaql.core.meta.EntityMetaFactory.registerGlobal(null);
        try {
            req.isOneOfSelfField("prop");
        } catch (TeaQLRuntimeException e) {}
        
        // subRequestOfFieldName when property missing
        io.teaql.core.meta.EntityDescriptor desc = new io.teaql.core.meta.EntityDescriptor();
        desc.setType("Req");
        io.teaql.core.meta.EntityMetaFactory dummyFactory = new io.teaql.core.meta.EntityMetaFactory() {
            @Override
            public io.teaql.core.meta.EntityDescriptor resolveEntityDescriptor(String type) {
                return "Req".equals(type) ? desc : null;
            }
            @Override
            public void register(io.teaql.core.meta.EntityDescriptor type) {}
            @Override
            public java.util.List<io.teaql.core.meta.EntityDescriptor> allEntityDescriptors() { return null; }
        };
        io.teaql.core.meta.EntityMetaFactory.registerGlobal(dummyFactory);
        try {
            try {
                req.subRequestOfFieldName("missingField");
            } catch (IllegalArgumentException e) {}
            
            // subRequestOfFieldName when relation is valid
            io.teaql.core.meta.Relation rel = new io.teaql.core.meta.Relation();
            rel.setName("myRel");
            rel.setType(new io.teaql.core.meta.SimplePropertyType(BaseEntity.class));
            rel.setRelationKeeper(desc);
            desc.setProperties(java.util.Collections.singletonList(rel));
            
            req.subRequestOfFieldName("myRel");
        } finally {
            io.teaql.core.meta.EntityMetaFactory.registerGlobal(null);
        }
    }
}
