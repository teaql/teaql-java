package io.teaql.examples.fixe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teaql.checkerfixadvanced.E;
import com.teaql.checkerfixadvanced.GeneratedRuntimeModule;
import com.teaql.checkerfixadvanced.Q;
import io.teaql.core.CompiledRowMapper;
import io.teaql.core.Entity;
import io.teaql.core.UserContext;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.checker.ObjectLocation;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.core.value.Expression;
import io.teaql.core.value.TeaQLNotLoadedException;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.TeaQLRuntime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** Normal generated Q/E and audited SQLite saves; no generated implementations are changed. */
final class ExpressionBoundaryProbe {
    /** Count real provider entry points without changing SQL, parameters, rows or transactions. */
    static final class ObservedSql extends JdbcSqlExecutor {
        final AtomicInteger calls = new AtomicInteger();
        ObservedSql(DataSource source) { super(source); }
        @Override public List<Map<String, Object>> queryForList(String sql, Object[] args) {
            calls.incrementAndGet(); return super.queryForList(sql, args);
        }
        @Override public <T extends Entity> List<T> query(String sql, Object[] args, CompiledRowMapper<T> mapper) {
            calls.incrementAndGet(); return super.query(sql, args, mapper);
        }
        @Override public Stream<Map<String, Object>> queryForStream(String sql, Object[] args) {
            calls.incrementAndGet(); return super.queryForStream(sql, args);
        }
        @Override public <T extends Entity> Stream<T> queryForStream(String sql, Object[] args, CompiledRowMapper<T> mapper) {
            calls.incrementAndGet(); return super.queryForStream(sql, args, mapper);
        }
        @Override public void execute(String sql) { calls.incrementAndGet(); super.execute(sql); }
        @Override public int update(String sql, Object[] args) { calls.incrementAndGet(); return super.update(sql, args); }
        @Override public int[] batchUpdate(String sql, List<Object[]> args) { calls.incrementAndGet(); return super.batchUpdate(sql, args); }
        @Override public void executeInTransaction(Runnable action) { calls.incrementAndGet(); super.executeInTransaction(action); }
    }
    @FunctionalInterface interface Operation { void run(Expression<?, String> e, AtomicInteger callbacks) throws Throwable; }
    record Terminal(String name, String suffix, Operation operation) { }
    static List<Terminal> terminals() {
        return List.of(
                new Terminal("eval", "eval()", (e, c) -> e.eval()),
                new Terminal("resolve", "resolve()", (e, c) -> e.resolve()),
                new Terminal("orIfNull", "orIfNull(<fallback>)", (e, c) -> e.orIfNull("PRIVATE-TERMINAL-ARGUMENT")),
                new Terminal("orElseThrow", "orElseThrow()", (e, c) -> e.orElseThrow()),
                new Terminal("orElseThrowSupplier", "orElseThrow(<supplier>)", (e, c) -> e.orElseThrow(() -> {
                    c.incrementAndGet(); return new AssertionError("PRIVATE-TERMINAL-ARGUMENT");
                })),
                new Terminal("isNull", "isNull()", (e, c) -> e.isNull()),
                new Terminal("isNotNull", "isNotNull()", (e, c) -> e.isNotNull()),
                new Terminal("isEmpty", "isEmpty()", (e, c) -> e.isEmpty()),
                new Terminal("isNotEmpty", "isNotEmpty()", (e, c) -> e.isNotEmpty()),
                new Terminal("whenIsNull", "whenIsNull(<callback>)", (e, c) -> e.whenIsNull(c::incrementAndGet)),
                new Terminal("whenIsNotNullRunnable", "whenIsNotNull(<callback>)", (e, c) -> e.whenIsNotNull((Runnable) c::incrementAndGet)),
                new Terminal("whenIsNotNullConsumer", "whenIsNotNull(<callback>)", (e, c) -> e.whenIsNotNull((Consumer<String>) value -> c.incrementAndGet())),
                new Terminal("whenIsEmpty", "whenIsEmpty(<callback>)", (e, c) -> e.whenIsEmpty(c::incrementAndGet)),
                new Terminal("whenNotEmptyConsumer", "whenNotEmpty(<callback>)", (e, c) -> e.whenNotEmpty((Consumer<String>) value -> c.incrementAndGet())),
                new Terminal("whenNotEmptyRunnable", "whenNotEmpty(<callback>)", (e, c) -> e.whenNotEmpty((Runnable) c::incrementAndGet)));
    }
    static Map<String, Object> diagnostic(String name, TeaQLNotLoadedException error, String pointer,
            int index, String attempted, String chain, List<String> failures) {
        var wire = error.toWire();
        boolean passed = pointer.equals(wire.instancePath()) && index == wire.breakPointIndex()
                && attempted.equals(wire.attemptedInstancePath()) && chain.equals(wire.expressionWithBrokenPoint())
                && error.getMessage().contains(chain) && !error.getMessage().contains("\"instancePath\"")
                && error.getMessage().startsWith("TeaQLNotLoadedError: root=PurchaseOrder access_path=");
        if (!passed) failures.add(name);
        return Map.of("name", name, "wire", wire, "human", error.getMessage(), "passed", passed);
    }
    static void verify() throws Exception {
        String database = System.getenv("TEAQL_FIX_E_DB") + ".expression-boundaries.sqlite";
        var source = new SQLiteDataSource(); source.setUrl("jdbc:sqlite:" + database);
        var time = LocalDateTime.of(2032, 2, 29, 10, 15, 30);
        BusinessClock clock = new BusinessClock() {
            public LocalDate businessDate(UserContext context) { return time.toLocalDate(); }
            public LocalDateTime businessDateTime(UserContext context) { return time; }
        };
        try (var keeper = source.getConnection()) {
            var sql = new ObservedSql(source);
            var ids = new IdSpaceIdGenerator(new GeneratedFixEExampleTest.DatabaseBridge(sql)); ids.ensureIdSpaceTable();
            var service = new SqliteDataServiceExecutor("sqlite", sql, source);
            var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                    .dataService("default", service).dataService("sqlite", service)
                    .idGenerationService(ids).businessClock(clock).build().install(GeneratedRuntimeModule.module());
            UserContext context = new GeneratedFixEExampleTest.Context(runtime); context.ensureSchema();
            var order = Q.purchaseOrders().comment("Prepare the Java expression owner")
                    .purpose("Verify generated E boundaries with real SQLite").newEntity(context);
            order.updateContactPhone("5550391"); order.updateInternalNote(null);
            order.auditAs("Create Java E boundary owner").save(context);
            var sparse = Q.purchaseOrdersWithMinimalFields().withIdIs(order.getId()).limit(1)
                    .comment("Load identity only").purpose("Observe genuine NotLoaded data").executeForOne(context);
            var late = Q.purchaseOrdersWithMinimalFields().withIdIs(order.getId())
                    .selectTenantWith(Q.tenantsWithMinimalFields().limit(1).comment("Load target identity only"))
                    .limit(1).comment("Load relation without its name").purpose("Distinguish a missing leaf").executeForOne(context);
            var empty = Q.purchaseOrders().withIdIs(order.getId())
                    .selectOrderItemListWith(Q.orderItemsWithMinimalFields().limit(10).comment("Load an empty child list"))
                    .limit(1).comment("Load null and empty data").purpose("Verify legitimate absence").executeForOne(context);
            assertNotNull(sparse); assertNotNull(late); assertNotNull(empty); assertTrue(sql.calls.get() > 0);
            int before = sql.calls.get(); var state = sparse.__internalLoadState(); var dirty = List.copyOf(sparse.getUpdatedProperties());
            var failures = new ArrayList<String>(); var cases = new ArrayList<Map<String, Object>>();
            var earlyError = assertThrows(TeaQLNotLoadedException.class, () -> E.purchaseOrder(sparse).getTenant().getName().eval());
            cases.add(diagnostic("missing-relation", earlyError, "/tenant", 0, "/tenant/name",
                    "E.purchaseOrder(<root>).getTenant()<broken>.getName().eval()", failures));
            assertEquals(Long.valueOf(1), E.purchaseOrder(late).getTenant().getId().eval());
            var leafError = assertThrows(TeaQLNotLoadedException.class, () -> E.purchaseOrder(late).getTenant().getName().eval());
            cases.add(diagnostic("missing-late-leaf", leafError, "/tenant/name", 1, "/tenant/name",
                    "E.purchaseOrder(<root>).getTenant().getName()<broken>.eval()", failures));
            assertNull(E.purchaseOrder(empty).getInternalNote().eval());
            assertEquals(Integer.valueOf(0), E.purchaseOrder(empty).getOrderItemList().size().eval());
            assertNull(E.purchaseOrder(empty).getOrderItemList().first().getProductName().eval());
            assertNull(E.purchaseOrder(empty).getOrderItemList().get(2).getProductName().eval());
            cases.add(Map.of("name", "loaded-null-empty-and-out-of-range", "null", true, "emptyCount", 0,
                    "firstAbsent", true, "indexAbsent", true, "passed", true));
            var terminals = new ArrayList<Map<String, Object>>();
            var scalar = E.purchaseOrder(sparse).getContactPhone();
            for (var terminal : terminals()) {
                var callbacks = new AtomicInteger();
                var error = assertThrows(TeaQLNotLoadedException.class, () -> terminal.operation().run(scalar, callbacks));
                String expected = "E.purchaseOrder(<root>).getContactPhone()<broken>." + terminal.suffix();
                boolean passed = expected.equals(error.getExpressionWithBrokenPoint()) && callbacks.get() == 0
                        && !error.getMessage().contains("PRIVATE-TERMINAL-ARGUMENT")
                        && error.getMessage().startsWith("TeaQLNotLoadedError: root=PurchaseOrder access_path=");
                if (!passed) failures.add("terminal:" + terminal.name());
                terminals.add(Map.of("name", terminal.name(), "expected", expected, "wire", error.toWire(),
                        "human", error.getMessage(), "callbacks", callbacks.get(), "passed", passed));
            }
            cases.add(Map.of("name", "all-public-terminals", "observations", terminals, "passed", failures.stream().noneMatch(v -> v.startsWith("terminal:"))));
            var callbacks = new AtomicInteger();
            E.purchaseOrder(empty).getInternalNote().whenIsNull(callbacks::incrementAndGet);
            assertEquals(1, callbacks.get()); assertEquals("loaded-null-default", E.purchaseOrder(empty).getInternalNote().orIfNull("loaded-null-default"));
            assertNull(E.purchaseOrder(empty).getInternalNote().eval());
            cases.add(Map.of("name", "loaded-null-fallback", "calls", 1, "originalNullUnchanged", true, "passed", true));
            var parent = E.purchaseOrder(late).getTenant();
            var sibling = parent.getName(); var snapshot = assertThrows(TeaQLNotLoadedException.class, sibling::eval).toWire();
            assertThrows(TeaQLNotLoadedException.class, () -> parent.getPurchaseOrderList().first().getTenant().getName().eval());
            assertEquals(snapshot, assertThrows(TeaQLNotLoadedException.class, sibling::eval).toWire());
            assertEquals(sql.calls.get(), before); assertSame(state, sparse.__internalLoadState()); assertEquals(dirty, sparse.getUpdatedProperties());
            assertFalse(sparse.__internalHasMutationLedger()); assertFalse(late.__internalHasMutationLedger()); assertFalse(empty.__internalHasMutationLedger());
            cases.add(Map.of("name", "pure-evaluation", "providerCalls", 0, "observerCalibrated", true,
                    "loadedSnapshotUnchanged", true, "dirtyStateUnchanged", true, "mutationLedgerCreated", false, "siblingUnchanged", true, "passed", true));
            var item = Q.orderItems().comment("Prepare a partial child projection")
                    .purpose("Observe a real canonical list index").newEntity(context);
            item.updatePurchaseOrder(order); item.updateProductName("Java expression item"); item.updateUnitPrice(BigDecimal.ONE);
            item.auditAs("Create Java E boundary child").save(context);
            var list = Q.purchaseOrdersWithMinimalFields().withIdIs(order.getId())
                    .selectOrderItemListWith(Q.orderItemsWithMinimalFields().limit(10).comment("Load child identity only"))
                    .limit(1).comment("Load a real partial child").purpose("Mark a missing field after a real list index").executeForOne(context);
            before = sql.calls.get();
            var childError = assertThrows(TeaQLNotLoadedException.class, () -> E.purchaseOrder(list).getOrderItemList().first().getProductName().eval());
            cases.add(diagnostic("missing-child-leaf", childError, "/orderItemList/0/productName", 2, "/orderItemList/0/productName",
                    "E.purchaseOrder(<root>).getOrderItemList().first().getProductName()<broken>.eval()", failures));
            var profiles = new ArrayList<TeaQLNotLoadedException.WireError>();
            for (var profile : JsonFieldNamingProfile.values()) profiles.add(childError.toWire(profile));
            cases.add(Map.of("name", "generated-three-json-profiles", "profiles", profiles, "passed", true));
            assertEquals(Integer.valueOf(0), E.purchaseOrder(empty).getOrderItemList().size().eval());
            assertEquals(Integer.valueOf(1), E.purchaseOrder(list).getOrderItemList().size().eval()); assertEquals(before, sql.calls.get());
            cases.add(Map.of("name", "independent-projection-snapshots", "oldSize", 0, "newSize", 1, "providerCalls", 0, "passed", true));
            var literal = new TeaQLNotLoadedException("SyntheticRow", ObjectLocation.hashRoot("contact.phone/~alias").element(2).member("user_url"),
                    "user_url", JsonFieldNamingProfile.CAMEL_CASE);
            assertEquals("/contact.phone~1~0alias/2/userUrl", literal.toWire().instancePath());
            cases.add(Map.of("name", "native-literal-location", "wire", literal.toWire(), "scope", "native-location-only", "passed", true));
            var mapper = new ObjectMapper();
            for (var value : cases) System.out.println("JAVA_E_BOUNDARY_CASE=" + mapper.writeValueAsString(value));
            System.out.println("JAVA_E_BOUNDARY_RESULT=" + mapper.writeValueAsString(Map.of(
                    "language", "java", "normalGeneratedQAndE", true, "passed", failures.isEmpty(), "cases", cases,
                    "orderID", order.getId(), "childID", item.getId(), "version", E.purchaseOrder(order).getVersion().eval(),
                    "failures", failures, "fullDiagnosticContractAccepted", false, "allP0P1Complete", false)));
            assertTrue("Java generated E boundary failures: " + failures, failures.isEmpty());
        }
    }
}
