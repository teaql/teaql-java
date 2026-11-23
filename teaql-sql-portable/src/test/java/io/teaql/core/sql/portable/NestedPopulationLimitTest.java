package io.teaql.core.sql.portable;

import io.teaql.core.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real SQLite population >10,000, not an SQL-string-only assertion. */
public class NestedPopulationLimitTest extends LayeredAggregationTest {
    private void seedLargePopulation() {
        metadata.resolveEntityDescriptor("StatKind").setEntitySupplier(StatKind::new);
        metadata.resolveEntityDescriptor("StatSchool").setEntitySupplier(StatSchool::new);
        database.execute("WITH RECURSIVE n(x) AS (SELECT 2000 UNION ALL SELECT x+1 FROM n WHERE x<12000) "
                + "INSERT INTO rollup_kind SELECT x,1,'large',x,1 FROM n");
        database.execute("WITH RECURSIVE n(x) AS (SELECT 2000 UNION ALL SELECT x+1 FROM n WHERE x<12000) "
                + "INSERT INTO rollup_school SELECT x,1,1,x FROM n");
    }

    @Test public void nestedDefaultPageMustNotTruncateStatisticalMembership() {
        seedLargePopulation();
        var eligible = new Request<>(StatKind.class, "StatKind") {
            @Override public StatKind internalNewEntity() { return new StatKind(); }
        }.comment("all eligible kinds");
        eligible.selectProperty("id");
        var request = new Request<>(StatSchool.class, "StatSchool").comment("count complete membership");
        request.purpose("verify nested membership beyond ten thousand rows");
        request.addAggregate("n", "id", AggrFunction.COUNT);
        request.addAggregate("total", "capacity", AggrFunction.SUM);
        request.appendSearchCriteria(new SubQuerySearchCriteria("kind", eligible, "id"));
        var result = execute(request).get(0);
        assertEquals(10006L, ((Number) result.get("n")).longValue());
        assertEquals(13301L, ((Number) result.get("total")).longValue());
        assertEquals("complete membership is embedded, not a truncated lookup", 1, database.queryTrace().size());
        String sql = database.queryTrace().get(0);
        System.out.println("NESTED_POPULATION_SQL=" + sql);
        int membership = sql.indexOf(" IN (");
        assertTrue(membership >= 0);
        assertFalse("no implicit inner page", sql.substring(membership, sql.lastIndexOf(')')).contains("LIMIT"));
    }

    @Test public void internalUnboundedPopulationMustNotAcquireOuterHardLimit() {
        seedLargePopulation();
        var root = new Request<>(StatSchool.class, "StatSchool").comment("inspect complete graph");
        root.purpose("verify framework-owned population is not a user page");
        var child = new Request<>(StatSchool.class, "StatSchool") {
            @Override public StatSchool internalNewEntity() { return new StatSchool(); }
        }.comment("load all contributing rows");
        child.selectProperty("id");
        child.unlimited();
        var nested = new QueryRequestSnapshot<>(child,
                QueryRequestScope.capture(root).deriveChild("StatSchool", child.comment()));
        assertEquals(10006, context.internalExecuteForList(nested).size());
        assertFalse(database.queryTrace().get(database.queryTrace().size()-1).contains("LIMIT"));
    }

    @Test public void explicitNestedTopNStillDefinesBusinessMembership() {
        seedLargePopulation();
        var eligible = new Request<>(StatKind.class, "StatKind") {
            @Override public StatKind internalNewEntity() { return new StatKind(); }
        }.comment("two selected kinds");
        eligible.selectProperty("id");
        eligible.top(2);
        var request = new Request<>(StatSchool.class, "StatSchool").comment("count explicit selection");
        request.purpose("explicit top N is not an implicit population ceiling");
        request.addAggregate("n", "id", AggrFunction.COUNT);
        request.appendSearchCriteria(new SubQuerySearchCriteria("kind", eligible, "id"));
        assertEquals(5L, ((Number) execute(request).get(0).get("n")).longValue());
    }
}
