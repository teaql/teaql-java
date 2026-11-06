package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.data.dynamic.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReviewedMutationSnapshotTest {
    private final EntityKey key = new EntityKey("Probe",1L);
    private final EntityMutationLedger ledger = new EntityMutationLedger();
    private MutationPlan plan(Object value) {
        return new MutationPlan("execution","Probe.saveGraph","Probe","review mutation",
                List.of(new MutationOperation(MutationOperationKind.UPDATE,key,1L,Collections.singletonMap("value",value))));
    }
    @Test public void immutableAndNullScalarsStayEqual() {
        for (Object value : Arrays.asList(null,true,4L,3.5,new java.math.BigDecimal("123456789012.3456789"),java.time.Instant.EPOCH)) {
            var p = plan(value); ReviewedMutationSnapshot.capture(p,ledger).requireUnchanged(p,ledger);
        }
    }
    @Test public void nestedContainerAndArrayMutationCannotRewriteTheSnapshot() {
        byte[] bytes = {1,2}; Map<String,Object> value = new HashMap<>(); value.put("bytes",bytes);
        var p = plan(List.of(value)); var snapshot = ReviewedMutationSnapshot.capture(p,ledger);
        snapshot.requireUnchanged(p,ledger); bytes[1] = 3;
        assertThrows(TeaQLRuntimeException.class,() -> snapshot.requireUnchanged(p,ledger));
    }
    private static final class PreciseDate extends Date {
        int nanos = 123456789;
        @Override public java.time.Instant toInstant() { return java.time.Instant.ofEpochSecond(0,nanos); }
    }
    @Test public void mutableDateInstantRetainsNanosecondPrecision() {
        var time = new PreciseDate(); var p = plan(time);
        var snapshot = ReviewedMutationSnapshot.capture(p,ledger); snapshot.requireUnchanged(p,ledger);
        time.nanos = 123456780; assertThrows(TeaQLRuntimeException.class,() -> snapshot.requireUnchanged(p,ledger));
    }
    @Test public void equalPrimitiveAndNestedArraysAreStable() {
        var p = plan(new Object[]{new int[]{1,2},new byte[]{3},Collections.singletonMap("nullable",null)});
        ReviewedMutationSnapshot.capture(p,ledger).requireUnchanged(p,ledger);
    }
    @Test public void dynamicFieldTimeCannotMutateBehindTheReview() {
        var time = new Date(123); var p = plan(DynamicFieldMutation.set("observed_at",DynamicDataType.DATE_TIME,time));
        var snapshot = ReviewedMutationSnapshot.capture(p,ledger); time.setTime(124);
        assertThrows(TeaQLRuntimeException.class,() -> snapshot.requireUnchanged(p,ledger));
    }
    @Test public void changedReferenceIdentityIsDetected() {
        var entity = new BaseEntity(); entity.__internalSet("id",1L); var p = plan(entity);
        var snapshot = ReviewedMutationSnapshot.capture(p,ledger); entity.__internalSet("id",2L);
        assertThrows(TeaQLRuntimeException.class,() -> snapshot.requireUnchanged(p,ledger));
    }
    @Test public void changedOperationIdentityKindAndVersionAreDetected() {
        var p = plan("value"); var snapshot = ReviewedMutationSnapshot.capture(p,ledger);
        for (var operation : List.of(new MutationOperation(MutationOperationKind.DELETE,key,1L,Map.of()),
                new MutationOperation(MutationOperationKind.UPDATE,new EntityKey("Other",1L),1L,Map.of("value","value")),
                new MutationOperation(MutationOperationKind.UPDATE,key,2L,Map.of("value","value")))) {
            var changed = new MutationPlan("execution","Probe.saveGraph","Probe","review mutation",List.of(operation));
            assertThrows(TeaQLRuntimeException.class,() -> snapshot.requireUnchanged(changed,ledger));
        }
    }
    @Test public void traceAndCommentChangesAreDetectedWithoutExportingValues() {
        var p = plan("PRIVATE_VALUE"); var snapshot = ReviewedMutationSnapshot.capture(p,ledger);
        ledger.setTraceChain(key,List.of(new TraceNode("PRIVATE_TRACE")));
        var error = assertThrows(TeaQLRuntimeException.class,() -> snapshot.requireUnchanged(p,ledger));
        assertFalse(error.getMessage().contains("PRIVATE"));
        var after = ReviewedMutationSnapshot.capture(p,ledger); ledger.setComment("PRIVATE_COMMENT");
        assertThrows(TeaQLRuntimeException.class,() -> after.requireUnchanged(p,ledger));
    }
    @Test public void cyclicContainersFailWithoutRecursingUntilStackOverflow() {
        List<Object> cycle = new ArrayList<>(); cycle.add(cycle);
        assertThrows(TeaQLRuntimeException.class,() -> ReviewedMutationSnapshot.capture(plan(cycle),ledger));
    }
}
