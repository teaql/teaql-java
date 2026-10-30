package io.teaql.core.value;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.BaseEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.Test;
import static org.junit.Assert.*;

/** Every public terminal must identify itself, not the helper it delegates to. */
public class ExpressionTerminalTraceTest {
    static class Row extends BaseEntity { String text; }
    static final class PrivateRow extends Row {
        int identityReads;
        @Override public Long getId() { identityReads++; return 9823456789012345L; }
        @Override public String toString() { throw new AssertionError("PRIVATE-ROOT-OBJECT"); }
    }
    static final class RootBox {
        final Row row = new Row();
        int stringReads;
        @Override public String toString() { stringReads++; return "PRIVATE-ROOT-OBJECT"; }
    }
    @FunctionalInterface interface Operation {
        void run(Expression<Row, String> expression, AtomicInteger callbacks) throws Throwable;
    }
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
                new Terminal("whenIsNotNullConsumer", "whenIsNotNull(<callback>)", (e, c) -> e.whenIsNotNull((Consumer<String>) v -> c.incrementAndGet())),
                new Terminal("whenIsEmpty", "whenIsEmpty(<callback>)", (e, c) -> e.whenIsEmpty(c::incrementAndGet)),
                new Terminal("whenNotEmptyConsumer", "whenNotEmpty(<callback>)", (e, c) -> e.whenNotEmpty((Consumer<String>) v -> c.incrementAndGet())),
                new Terminal("whenNotEmptyRunnable", "whenNotEmpty(<callback>)", (e, c) -> e.whenNotEmpty((Runnable) c::incrementAndGet)));
    }
    private Expression<Row, String> expression(Row row, AtomicInteger reads) {
        return new LoadedPropertyExpression<>(new ValueExpression<>(row,
                io.teaql.core.checker.JsonFieldNamingProfile.CAMEL_CASE, "E.row(<root>)"),
                "private_note", value -> { reads.incrementAndGet(); return value.text; });
    }

    @Test public void everyMissingTerminalRetainsItsOwnNativeCallAndNeverExecutesCallbacks() throws Exception {
        var rows = new ArrayList<Map<String, Object>>();
        var failures = new ArrayList<String>();
        for (var terminal : terminals()) {
            var row = new Row(); var callbacks = new AtomicInteger(); var reads = new AtomicInteger();
            var error = assertThrows(TeaQLNotLoadedException.class, () -> terminal.operation().run(expression(row, reads), callbacks));
            String expected = "E.row(<root>).getPrivateNote()<broken>." + terminal.suffix();
            boolean passed = expected.equals(error.getExpressionWithBrokenPoint())
                    && callbacks.get() == 0 && reads.get() == 0 && !row.__internalHasMutationLedger()
                    && !error.getMessage().contains("PRIVATE-TERMINAL-ARGUMENT");
            if (!passed) failures.add(terminal.name());
            rows.add(Map.of("name", terminal.name(), "expected", expected, "wire", error.toWire(),
                    "human", error.getMessage(), "callbacks", callbacks.get(), "getterReads", reads.get(),
                    "mutationLedgerCreated", row.__internalHasMutationLedger(), "passed", passed));
        }
        System.out.println("JAVA_NATIVE_E_TERMINALS=" + new ObjectMapper().writeValueAsString(Map.of(
                "language", "java", "passed", failures.isEmpty(), "observations", rows, "failures", failures)));
        assertTrue("Incorrect terminal reconstruction: " + failures, failures.isEmpty());
    }

    @Test public void loadedCallbacksReadTheValueOnceAndRetainNullAndEmptySemantics() throws Throwable {
        for (String value : new String[] {null, "", "loaded"}) {
            for (var terminal : terminals().stream().filter(v -> v.name().startsWith("when")).toList()) {
                var row = new Row(); row.text = value; row.markPropertyLoaded("private_note");
                var reads = new AtomicInteger(); var callbacks = new AtomicInteger();
                terminal.operation().run(expression(row, reads), callbacks);
                assertEquals(terminal.name(), 1, reads.get());
                boolean expected = switch (terminal.name()) {
                    case "whenIsNull" -> value == null;
                    case "whenIsNotNullRunnable", "whenIsNotNullConsumer" -> value != null;
                    case "whenIsEmpty" -> value == null || value.isEmpty();
                    default -> value != null && !value.isEmpty();
                };
                assertEquals(terminal.name(), expected ? 1 : 0, callbacks.get());
                assertFalse(row.__internalHasMutationLedger());
            }
        }
    }

    @Test public void nullCallbacksStillValidateNotLoadedBeforeDoingNothing() {
        var row = new Row(); var reads = new AtomicInteger(); var e = expression(row, reads);
        for (var suffix : List.of("whenIsNull(<callback>)", "whenIsNotNull(<callback>)", "whenIsEmpty(<callback>)", "whenNotEmpty(<callback>)")) {
            var error = assertThrows(TeaQLNotLoadedException.class, () -> {
                switch (suffix) {
                    case "whenIsNull(<callback>)" -> e.whenIsNull(null);
                    case "whenIsNotNull(<callback>)" -> e.whenIsNotNull((Runnable) null);
                    case "whenIsEmpty(<callback>)" -> e.whenIsEmpty(null);
                    default -> e.whenNotEmpty((Runnable) null);
                }
            });
            assertEquals("E.row(<root>).getPrivateNote()<broken>." + suffix, error.getExpressionWithBrokenPoint());
        }
        assertEquals(0, reads.get()); assertFalse(row.__internalHasMutationLedger());
    }

    @Test public void diagnosticsUseTypeLabelsWithoutReadingIdentityOrRenderingRootObjects() throws Exception {
        var row = new PrivateRow();
        var direct = assertThrows(TeaQLNotLoadedException.class, () -> expression(row, new AtomicInteger()).eval());
        var box = new RootBox();
        Expression<RootBox, Row> parent = new ValueExpression<>(box).apply(value -> value.row);
        var nested = new LoadedPropertyExpression<>(parent, "private_note", value -> value.text);
        var indirect = assertThrows(TeaQLNotLoadedException.class, nested::eval);
        boolean passed = "PrivateRow".equals(direct.getRoot()) && "RootBox".equals(indirect.getRoot())
                && row.identityReads == 0 && box.stringReads == 0
                && !direct.getMessage().contains("9823456789012345")
                && !indirect.getMessage().contains("PRIVATE-ROOT-OBJECT");
        System.out.println("JAVA_NATIVE_E_ROOTS=" + new ObjectMapper().writeValueAsString(Map.of(
                "passed", passed, "entityHuman", direct.getMessage(), "objectHuman", indirect.getMessage(),
                "identityReads", row.identityReads, "rootStringReads", box.stringReads)));
        assertTrue("Diagnostics must not inspect identity or stringify roots", passed);
        assertFalse(row.__internalHasMutationLedger()); assertFalse(box.row.__internalHasMutationLedger());
    }
}
