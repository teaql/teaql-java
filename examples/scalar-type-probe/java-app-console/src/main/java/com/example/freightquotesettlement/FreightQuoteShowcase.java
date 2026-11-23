package com.example.freightquotesettlement;

import com.example.freightquotesettlement.freightquote.FreightQuote;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.checker.CheckException;
import io.teaql.core.checker.CheckResult;
import io.teaql.core.value.TeaQLNotLoadedException;
import io.teaql.jackson.TeaQLModule;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sqlite.SQLiteDataSource;

public final class FreightQuoteShowcase {
    private static final BigDecimal PRECISE = new BigDecimal("123456789012.3456789");
    private static final BigDecimal TOTAL = new BigDecimal("123456789012.6456789");
    private static final long FIRST_SHIPMENT = 9_007_199_254_740_993L;
    private static final LocalDate DELIVERY_DATE = LocalDate.of(2026, 10, 31);
    private static final LocalTime PICKUP_TIME = LocalTime.of(9, 30, 15, 123_456_000);
    private static final LocalDateTime QUOTED_AT = DELIVERY_DATE.atTime(1, 45, 0, 123_000_000);

    private FreightQuoteShowcase() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !List.of("first", "restart").contains(args[1])) {
            throw new IllegalArgumentException("Supply a database and first or restart");
        }
        var database = new SQLiteDataSource();
        database.setUrl("jdbc:sqlite:" + Path.of(args[0]).toAbsolutePath());
        var jdbc = new JdbcSqlExecutor(database);
        var app = new App();
        var runtime = app.teaQLRuntime(app.entityMetaFactory(),
                app.dataServiceRegistry(app.dataServiceExecutor(jdbc, database)), app.teaQLDatabase(jdbc, database));
        var context = new CustomUserContext(runtime);
        context.ensureSchema();
        if ("first".equals(args[1])) registerQuotes(context);

        var rows = Q.freightQuotes().orderByAmountAscending().limit(4)
                .comment("Compare outstanding freight bids").purpose("Rank freight bids numerically").executeForList(context);
        require(rows.stream().map(FreightQuote::getReference).toList().equals(List.of("SEA-001", "SEA-002", "SEA-003")),
                "Unexpected quote rank or count");
        require(rows.stream().map(row -> E.freightQuote(row).getAmount().eval()).toList()
                .equals(List.of(new BigDecimal("0.10"), new BigDecimal("0.20"), PRECISE)), "Persisted amount or scale changed");
        require(rows.stream().map(FreightQuote::getShipmentNumber).toList()
                .equals(List.of(FIRST_SHIPMENT, FIRST_SHIPMENT + 1, FIRST_SHIPMENT + 2)), "Shipment numbers changed");
        require(rows.stream().map(FreightQuote::getInstructions).toList()
                .equals(Arrays.asList(null, "", "Hold for inspection")), "Null and empty instructions were conflated");
        var high = rows.get(2);
        var match = Q.freightQuotes().withAmountIs(PRECISE).limit(1)
                .comment("Find the revised offshore freight rate").purpose("Match an exact Decimal amount").executeForOne(context);
        require(match != null && Objects.equals(match.getId(), high.getId()) && high.getVersion() == 2,
                "Exact amount selected a different freight quote");
        var aggregate = Q.freightQuotesWithMinimalFields().sumAmountAs("total")
                .comment("Sum the outstanding freight bids").purpose("Keep exact base-ten precision").aggregation(context).toList();
        require(aggregate.size() == 1 && TOTAL.equals(aggregate.get(0).get("total")), "The freight total changed");
        var mapper = new ObjectMapper().findAndRegisterModules().registerModule(new TeaQLModule(context));
        verifyWire(context, mapper, high);

        high.updateAmount(null);
        try {
            high.auditAs("Reject a quote without its required amount").save(context);
            throw new AssertionError("Missing freight amount was persisted");
        } catch (CheckException failure) {
            require(failure.getViolates().stream().anyMatch(violation ->
                    violation.getRuleId() == CheckResult.RuleId.REQUIRED && violation.getLocation().modelPath().equals("amount")),
                    "Missing freight amount was rejected at the wrong field");
        }
        var retained = readQuote(context, "SEA-003");
        require(retained.getVersion() == 2 && PRECISE.equals(retained.getAmount()), "Rejected quote changed persisted state");
        System.out.println("FREIGHT_QUOTE=" + mapper.writeValueAsString(Map.of(
                "phase", args[1], "quotes", rows.size(), "total", TOTAL.toPlainString(),
                "precise", retained.getAmount().toPlainString(), "version", retained.getVersion(),
                "invalidAmountRejected", true, "sparseInstructionsNotLoaded", true)));
    }

    private static void registerQuotes(CustomUserContext context) {
        var platform = Q.platforms().withIdIs(1L).limit(1).comment("Locate the dispatch platform")
                .purpose("Attach quotes to the configured freight desk").executeForOne(context);
        require(platform != null, "Missing dispatch platform");
        record Bid(String reference, String amount, int count, String instructions) {}
        var bids = List.of(new Bid("SEA-001", "0.10", 2, null), new Bid("SEA-002", "0.20", 3, ""),
                new Bid("SEA-003", "3.50", Integer.MAX_VALUE, "Hold for inspection"));
        for (int index = 0; index < bids.size(); index++) {
            var bid = bids.get(index);
            var quote = Q.freightQuotes().comment("Prepare a freight quote")
                    .purpose("Price the shipment for its delivery window").newEntity(context);
            quote.updatePlatform(platform).updateReference(bid.reference()).updateAmount(new BigDecimal(bid.amount()))
                    .updateShipmentNumber(FIRST_SHIPMENT + index).updatePackageCount(bid.count())
                    .updateDeliveryDate(DELIVERY_DATE).updatePickupTime(PICKUP_TIME).updateQuotedAt(QUOTED_AT)
                    .updateInstructions(bid.instructions());
            quote.auditAs("Register a freight quote").save(context);
        }
        var revised = readQuote(context, "SEA-003");
        require(new BigDecimal("3.50").equals(revised.getAmount()), "The initial offshore rate lost its Decimal scale");
        revised.updateAmount(PRECISE).auditAs("Accept the revised offshore freight rate").save(context);
    }

    private static FreightQuote readQuote(CustomUserContext context, String reference) {
        var quote = Q.freightQuotes().withReferenceIs(reference).limit(1).comment("Read the selected freight quote")
                .purpose("Verify the retained delivery commitment").executeForOne(context);
        require(quote != null, "The freight quote is missing");
        return quote;
    }

    private static void verifyWire(CustomUserContext context, ObjectMapper mapper, FreightQuote high) throws Exception {
        var wire = mapper.readTree(mapper.writeValueAsString(high));
        require(PRECISE.toPlainString().equals(wire.path("amount").textValue())
                && Long.toString(FIRST_SHIPMENT + 2).equals(wire.path("shipmentNumber").textValue())
                && wire.path("packageCount").isIntegralNumber() && wire.path("packageCount").intValue() == Integer.MAX_VALUE,
                "JSON changed Decimal, Long or Integer width");
        var restored = E.freightQuote(mapper.treeToValue(wire, FreightQuote.class));
        require(PRECISE.equals(restored.getAmount().eval()) && restored.getShipmentNumber().eval() == FIRST_SHIPMENT + 2
                && DELIVERY_DATE.equals(restored.getDeliveryDate().eval()) && PICKUP_TIME.equals(restored.getPickupTime().eval())
                && QUOTED_AT.equals(restored.getQuotedAt().eval()), "Decoded quote changed a numeric or temporal value");

        var sparse = Q.freightQuotesWithMinimalFields().withIdIs(high.getId()).selectReference().limit(1)
                .comment("Read only the quote reference").purpose("Keep unselected delivery instructions distinct")
                .executeForOne(context);
        require(sparse != null, "The sparse freight quote is missing");
        var projected = mapper.readTree(mapper.writeValueAsString(sparse));
        require(!projected.has("instructions"), "Sparse JSON included unselected instructions");
        try {
            E.freightQuote(mapper.treeToValue(projected, FreightQuote.class)).getInstructions().eval();
            throw new AssertionError("Unselected instructions became a value");
        } catch (TeaQLNotLoadedException expected) {
            // The generated expression still distinguishes unloaded instructions from null.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
