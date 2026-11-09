package com.example.exactdecimalservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.exactdecimalservice.decimalentry.DecimalEntry;
import com.example.exactdecimalservice.platform.Platform;
import io.teaql.core.AggregationResult;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.runtime.memory.MemoryDataService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** The same generated Q/E/Mutation APIs run on the existing in-memory provider. */
public final class GeneratedMemoryAggregateProbe {
    private GeneratedMemoryAggregateProbe() {}

    public static void main(String[] args) throws Exception {
        var ids = new AtomicLong(1);
        var memory = new MemoryDataService("default");
        var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                .dataService("default", memory).idGenerationService((c, e) -> ids.getAndIncrement())
                .executionLogging(false).build().install(GeneratedRuntimeModule.module());
        var context = new CustomUserContext(runtime);

        // Memory has no schema or external database. Seed through generated
        // Mutation APIs; do not insert rows by reaching into provider storage.
        Platform platform = Q.platforms().comment("Create the memory example platform")
                .purpose("Own generated exact Decimal examples").newEntity(context);
        platform.updateName("Exact Decimal Memory Platform");
        platform.auditAs("Create the generated memory example root").save(context);

        var empty = total(context, List.of(-1L));
        if (empty.toList().size() != 1 || !empty.toList().get(0).containsKey("total")
                || empty.toList().get(0).get("total") != null) throw new AssertionError("empty SUM must be present null");
        var json = new ObjectMapper().valueToTree(empty.toList());
        if (!json.get(0).has("total") || !json.get(0).get("total").isNull()) throw new AssertionError("JSON lost selected null");
        if (empty.toNumber(0).intValue() != 0 || empty.toNumber(null) != null) throw new AssertionError("fallback mutated raw aggregate");

        DecimalEntry first = save(context, platform, "first", "0.1");
        DecimalEntry second = save(context, platform, "second", "0.2");
        var rows = Q.decimalEntriesWithMinimalFields().selectLabel().selectAmount()
                .withAmountIs(new BigDecimal("0.1")).limit(10)
                .comment("Read generated memory Decimal values")
                .purpose("Use typed Q and E without a database").executeForList(context);
        if (rows.size() != 1 || !"first".equals(E.decimalEntry(rows.get(0)).getLabel().eval())
                || new BigDecimal("0.1").compareTo(E.decimalEntry(rows.get(0)).getAmount().eval()) != 0)
            throw new AssertionError("generated Q/E memory round trip failed");
        Object sum = total(context, List.of(first.getId(), second.getId())).toList().get(0).get("total");
        if (!(sum instanceof BigDecimal decimal) || decimal.compareTo(new BigDecimal("0.3")) != 0)
            throw new AssertionError("memory SUM lost exact Decimal precision");

        // A populated store can still have an empty selected result. One
        // caller's explicit fallback must not contaminate a repeated query.
        if (total(context, List.of(-1L)).toList().get(0).get("total") != null)
            throw new AssertionError("empty bucket borrowed another bucket's sum");
        System.out.println("GENERATED_MEMORY_AGGREGATE_PASS q=1 e=1 mutation=1 sum=0.3 empty=null fallback=0 rows=2 sql=0");
    }

    private static DecimalEntry save(CustomUserContext context, Platform platform, String label, String amount) {
        DecimalEntry row = Q.decimalEntries().comment("Initialize an exact memory Decimal entry")
                .purpose("Exercise generated audited mutation on memory").newEntity(context);
        row.updatePlatform(platform); row.updateLabel(label); row.updateAmount(new BigDecimal(amount));
        row.auditAs("Persist the generated memory Decimal entry").save(context); return row;
    }

    private static AggregationResult total(CustomUserContext context, List<Long> ids) {
        return Q.decimalEntriesWithMinimalFields().withIdIn(ids.toArray(Long[]::new)).sumAmountAs("total")
                .comment("Sum selected generated memory entries")
                .purpose("Check exact numeric and empty-result semantics").aggregation(context);
    }
}
