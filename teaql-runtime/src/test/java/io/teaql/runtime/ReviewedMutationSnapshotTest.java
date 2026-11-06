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
    @Test public void traceChangeIsRejectedButLaterCommentCannotReplaceTheCapturedReason() {
        var p = plan("PRIVATE_VALUE"); var snapshot = ReviewedMutationSnapshot.capture(p,ledger);
        ledger.setTraceChain(key,List.of(new TraceNode("PRIVATE_TRACE")));
        var error = assertThrows(TeaQLRuntimeException.class,() -> snapshot.requireUnchanged(p,ledger));
        assertFalse(error.getMessage().contains("PRIVATE"));
        var after = ReviewedMutationSnapshot.capture(p,ledger); ledger.setComment("PRIVATE_COMMENT");
        after.requireUnchanged(p,ledger);
        assertEquals("review mutation",after.preparedPlan().auditReason());
    }
    @Test public void providerVersionProgressDoesNotChangeThePreparedWriteVersion() {
        var p = plan("value"); var snapshot = ReviewedMutationSnapshot.capture(p,ledger);
        var next = new MutationPlan("execution","Probe.saveGraph","Probe","review mutation",
                List.of(new MutationOperation(MutationOperationKind.UPDATE,key,2L,Map.of("value","value"))));
        assertThrows(TeaQLRuntimeException.class,() -> snapshot.requireUnchanged(next,ledger));
        snapshot.requireUnchanged(next,ledger,true);
        assertEquals(Long.valueOf(1),snapshot.preparedPlan().operations().get(0).originalVersion());
    }
    @Test public void preparedProviderOperandsAreDetachedFromCustomerPolicyOperands() {
        var metadata = new io.teaql.core.meta.SimpleEntityMetaFactory();
        var descriptor = new io.teaql.core.meta.EntityDescriptor();descriptor.setType("BaseEntity");descriptor.setEntitySupplier(BaseEntity::new);metadata.register(descriptor);
        var reference = new BaseEntity();reference.__internalSet("id",1L);
        var time = new Date(123);byte[] bytes = {1,2};var values = new HashMap<String,Object>();values.put("time",time);values.put("bytes",bytes);values.put("reference",reference);
        var p = plan(values);var snapshot = ReviewedMutationSnapshot.capture(p,ledger,metadata);
        time.setTime(456);bytes[0]=3;reference.__internalSet("id",2L);
        var detached = (Map<?,?>)snapshot.preparedPlan().operations().get(0).changedValues().get("value");
        assertEquals(123L,((Date)detached.get("time")).getTime());assertArrayEquals(new byte[]{1,2},(byte[])detached.get("bytes"));assertEquals(Long.valueOf(1),((Entity)detached.get("reference")).getId());
    }
    @Test public void cyclicContainersFailWithoutRecursingUntilStackOverflow() {
        List<Object> cycle = new ArrayList<>(); cycle.add(cycle);
        assertThrows(TeaQLRuntimeException.class,() -> ReviewedMutationSnapshot.capture(plan(cycle),ledger));
    }
}
