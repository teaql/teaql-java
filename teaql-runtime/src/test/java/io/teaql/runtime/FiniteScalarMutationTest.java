package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.checker.*;
import io.teaql.core.meta.SimplePropertyType;
import java.util.*;
import java.util.function.Consumer;
import org.junit.Test;
import static io.teaql.runtime.CanonicalMutationFieldsTest.*;
import static org.junit.Assert.*;

public class FiniteScalarMutationTest {
    static final List<String> FIELDS = List.of("required_float", "optional_float", "required_double", "optional_double");
    static Object number(String field, double value) {
        if (field.endsWith("float")) return Float.valueOf((float) value);
        return Double.valueOf(value);
    }
    static Fixture fixture(boolean checker, Consumer<Probe> fix) {
        var f = new Fixture(checker, fix);
        for (String field : FIELDS) {
            var p = property(field); p.setType(new SimplePropertyType(field.endsWith("float") ? Float.class : Double.class));
            f.descriptor.getProperties().add(p);
        }
        return f;
    }
    static CheckException rejected(Fixture f, Probe row, String field) {
        var failure = assertThrows(CheckException.class, () -> row.auditAs("Reject non-finite mutation").save(f.context));
        assertTrue(failure.getViolates().stream().anyMatch(v -> v.getRuleId() == CheckResult.RuleId.INVALID_TYPE
                && v.getLocation().modelPath().endsWith(field) && "NameProbe".equals(v.getRootType()) && v.getInputValue() == null));
        assertEquals(0, f.policyCalls); assertEquals(0, f.ids); assertEquals(0, f.provider.mutations);
        assertEquals(0, f.provider.begins); assertEquals(0, f.audits);
        return failure;
    }
    static void invalid(boolean checker) {
        for (boolean update : List.of(false, true)) for (String field : FIELDS)
            for (double bad : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
                var f = fixture(checker, null); var row = f.entity(update); row.updateProperty(field, number(field, bad));
                var expected = row.getProperty(field); rejected(f, row, field);
                assertEquals(expected, row.getProperty(field)); assertTrue(row.getUpdatedProperties().contains(field));
                assertEquals(checker ? 1 : 0, f.checkerCalls);
            }
    }
    @Test public void rejectsAllNonfiniteCreateAndUpdateWithChecker() { invalid(true); }
    @Test public void rejectsAllNonfiniteCreateAndUpdateWithoutChecker() { invalid(false); }
    @Test public void customerFixMayRepairInvalidInputBeforeFinalAdmission() {
        for (boolean update : List.of(false, true)) for (String field : FIELDS)
            for (double bad : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
                var f = fixture(true, row -> row.updateProperty(field, number(field, 7.5)));
                var row = f.entity(update); row.updateProperty(field, number(field, bad));
                row.auditAs("Accept a finite final customer Fix").save(f.context);
                assertEquals(1, f.checkerCalls); assertEquals(1, f.provider.mutations); assertEquals(1, f.provider.begins);
                assertEquals(number(field, 7.5), row.getProperty(field));
            }
    }
    @Test public void customerFixCannotIntroduceNonfiniteValues() {
        for (boolean update : List.of(false, true)) for (String field : FIELDS)
            for (double bad : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
                var f = fixture(true, row -> row.updateProperty(field, number(field, bad)));
                var row = f.entity(update); row.updateProperty(field, number(field, 1.25));
                rejected(f, row, field); assertEquals(1, f.checkerCalls);
            }
    }
    @Test public void secondChildRejectsTheWholeGraphBeforeAnyTransactionAndCanBeRepaired() {
        var f = fixture(true, null); var parent = f.entity(false); var first = f.entity(false); var second = f.entity(false);
        first.updateProperty("required_double", 7.5d); second.updateProperty("optional_float", Float.POSITIVE_INFINITY);
        parent.updateProperty("children", List.of(first, second));
        var error = rejected(f, parent, "optional_float");
        assertEquals("children[1].optional_float", error.getViolates().get(0).getLocation().modelPath());
        assertEquals(7.5d, (Double)first.getProperty("required_double"), 0);
        second.updateProperty("optional_float", 4.5f);
        parent.auditAs("Retry the same graph after repairing one scalar").save(f.context);
        assertEquals(1, f.provider.begins); assertEquals(3, f.provider.mutations); assertEquals(3, f.audits);
    }
    @Test public void detachedRawLedgerIntentCannotBypassFinalNumericAdmission() {
        var f = fixture(true, null); var row = f.entity(true);
        row.getEntityMutationLedger().setOriginalVersion(new EntityKey("NameProbe", 77L),1L);
        row.getEntityMutationLedger().set(new EntityKey("NameProbe", 77L), "required_double", Double.NEGATIVE_INFINITY);
        rejected(f, row, "required_double");
        assertTrue(row.getEntityMutationLedger().currentChangeSet().changes().containsKey(new EntityKey("NameProbe", 77L)));
    }
    @Test public void untouchedPartialRelatedObjectIsNotTreatedAsAMutation() {
        var f = fixture(true, null); var row = f.entity(true); var passive = new Probe();
        passive.__internalSet("id", 2L); passive.__internalSet("version", 1L); passive.set$status(EntityStatus.PERSISTED);
        passive.__internalSet("required_double", Double.POSITIVE_INFINITY);
        row.updateProperty("children", List.of(passive));
        assertFalse(passive.__internalHasMutationLedger());
        assertTrue(io.teaql.core.checker.internal.FiniteScalarAdmission.collect(f.metadata,row,true).isEmpty());
        assertFalse(passive.__internalHasMutationLedger());
        row.auditAs("Save only the actually modified owner").save(f.context);
        assertEquals(1, f.provider.mutations); assertEquals(1, f.audits); assertTrue(passive.getUpdatedProperties().isEmpty());
    }
    @Test public void legalFiniteValuesNullAndNonnumericTextRemainUnchanged() {
        for (Object value : Arrays.asList(null, -0.0d, Double.MIN_VALUE, Double.MAX_VALUE, -0.0f, Float.MIN_VALUE, Float.MAX_VALUE)) {
            var f = fixture(true, null); var row = f.entity(false);
            row.updateProperty("required_string", "Infinity"); row.updateProperty("required_double", value);
            row.auditAs("Calibrate finite and nullable values").save(f.context);
            assertEquals(value, row.getProperty("required_double")); assertEquals(1, f.provider.mutations);
        }
    }
}
