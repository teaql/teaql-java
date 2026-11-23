package io.teaql.core;

import io.teaql.core.checker.Checker;
import io.teaql.core.checker.CheckResult;
import io.teaql.core.checker.ObjectLocation;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Pure canonical-root admission using real loaded state and mutation ledger semantics. */
public class ContextRootOwnershipTest {
    static final class Row extends BaseEntity {
        Entity root;
        @Override public String typeName() { return "PurchaseOrder"; }
        @Override public Object __internalGet(String field) { return field.equals("domain_root") ? root : super.__internalGet(field); }
        @Override public void __internalSet(String field,Object value) {
            if(field.equals("domain_root")) { root=(Entity)value; markPropertyLoaded(field); }
            else super.__internalSet(field,value);
        }
    }
    static class Ref extends BaseEntity {
        @Override public String typeName() { return "Tenant"; }
        Ref(long id) { setProperty("id",id); set$status(EntityStatus.REFER); }
    }
    static final class SpecializedRef extends Ref {
        SpecializedRef(long id) { super(id); }
        @Override public String typeName() { return "SpecializedTenant"; }
    }
    static class UnrelatedRef extends BaseEntity {
        UnrelatedRef(long id) { setProperty("id",id); set$status(EntityStatus.REFER); }
        @Override public String typeName() { return "OtherTenant"; }
    }
    static final Checker<Row> CHECKER = new Checker<>() {
        public String type() { return "PurchaseOrder"; }
        public void checkAndFix(UserContext context,Row row,ObjectLocation location) {}
    };
    static UserContext context(long root) {
        Map<String,Object> values=new HashMap<>();
        UserContext context=new UserContextTest.DummyUserContext() {
            public void putAttribute(String key,Object value) { values.put(key,value); }
            public Object getAttribute(String key) { return values.get(key); }
            public <T> T getAttribute(String key,Class<T> type) { return type.cast(values.get(key)); }
        };
        return context.withActiveRoot(new ContextEntityRef("Tenant",root));
    }
    static Row loaded() {
        var row=new Row();row.setProperty("id",41L);row.setProperty("version",1L);
        row.setProperty("domain_root",new Ref(1));row.set$status(EntityStatus.PERSISTED);return row;
    }
    static void verify(UserContext context,Row row) {
        CHECKER.verifyContextRoot(context,row,"domain_root","Tenant",ObjectLocation.hashRoot("domain_root"));
    }
    @Test public void changingContextAndPayloadCannotReplaceTheOriginalOwner() {
        var row=loaded();row.updateProperty("domain_root",new Ref(2));
        assertEquals(ContextRootException.Reason.VALUE_MISMATCH,
                assertThrows(ContextRootException.class,()->verify(context(2),row)).getReason());
        assertEquals(Long.valueOf(1),((Entity)row.__internalPropertyChange("domain_root").getOldValue()).getId());
    }
    @Test public void repeatedChangesKeepTheFirstLoadedOwner() {
        var row=loaded();row.updateProperty("domain_root",new Ref(2));row.updateProperty("domain_root",new Ref(3));
        assertThrows(ContextRootException.class,()->verify(context(3),row));
        assertEquals(Long.valueOf(1),((Entity)row.getOldValue("domain_root")).getId());
    }
    @Test public void rawLedgerCannotMoveAnUnchangedGetter() {
        var row=loaded();row.getEntityMutationLedger().set(new EntityKey(row.typeName(),row.getId()),"domain_root",2L);
        assertThrows(ContextRootException.class,()->verify(context(1),row));assertEquals(Long.valueOf(1),row.root.getId());
    }
    @Test public void equalRawLedgerRootIsAllowedWithoutRewritingIntent() {
        var row=loaded();var key=new EntityKey(row.typeName(),row.getId());var ledger=row.getEntityMutationLedger();ledger.set(key,"domain_root",1L);
        verify(context(1),row);assertTrue(ledger.containsField(key,"domain_root"));assertEquals(1L,ledger.get(key,"domain_root"));
    }
    @Test public void explicitRawNullIsNotConfusedWithNoChange() {
        var row=loaded();var key=new EntityKey(row.typeName(),row.getId());var ledger=row.getEntityMutationLedger();ledger.set(key,"domain_root",null);
        var context=context(1);verify(context,row);assertTrue(ledger.containsField(key,"domain_root"));assertInvalid(context);
    }
    @Test public void aPreviouslyUnloadedRootCannotBeMadeAuthoritativeByAnUpdate() {
        var row=new Row();row.setProperty("id",41L);row.setProperty("version",1L);row.set$status(EntityStatus.PERSISTED);
        row.updateProperty("domain_root",new Ref(1));var context=context(1);verify(context,row);assertInvalid(context);
    }
    @Test public void readingOriginalOwnershipAllocatesNoLedgerAndChangesNoLoadState() {
        var row=loaded();var state=row.__internalLoadState();assertFalse(row.__internalHasMutationLedger());verify(context(1),row);
        assertFalse(row.__internalHasMutationLedger());assertSame(state,row.__internalLoadState());assertNull(row.__internalPropertyChange("domain_root"));
    }
    @Test public void deleteAndRecoveryKeepTheLoadedOwner() {
        for(boolean recovery:List.of(false,true)) {
            var row=loaded();if(recovery) {row.set$status(EntityStatus.PERSISTED_DELETED);row.markAsRecover();}else row.markForDeletion();
            assertTrue(row.needPersist());assertThrows(ContextRootException.class,()->verify(context(2),row));
        }
    }
    @Test public void containsFieldReadsAllChangeFramesWithoutAllocatingOne() {
        var ledger=new EntityMutationLedger();var key=new EntityKey("PurchaseOrder",41L);
        assertFalse(ledger.containsField(key,"domain_root"));ledger.set(key,"domain_root",null);ledger.pushChangeSet();
        assertTrue(ledger.containsField(key,"domain_root"));assertNull(ledger.get(key,"domain_root"));ledger.set(key,"domain_root",1L);
        assertEquals(1L,ledger.get(key,"domain_root"));ledger.popChangeSet();assertTrue(ledger.containsField(key,"domain_root"));assertNull(ledger.get(key,"domain_root"));
    }
    static UserContext modelContext(boolean related) {
        var metadata=new io.teaql.core.meta.SimpleEntityMetaFactory();
        var root=new io.teaql.core.meta.EntityDescriptor();root.setType("Tenant");root.setTargetType(Ref.class);metadata.register(root);
        var specialized=new io.teaql.core.meta.EntityDescriptor();specialized.setType("SpecializedTenant");specialized.setTargetType(SpecializedRef.class);
        if(related) specialized.setParent(root);metadata.register(specialized);
        var other=new io.teaql.core.meta.EntityDescriptor();other.setType("OtherTenant");other.setTargetType(UnrelatedRef.class);metadata.register(other);
        Map<String,Object> values=new HashMap<>();
        var context=new UserContextTest.DummyUserContext() {
            @Override public <T> T capability(Class<T> type) { return type==io.teaql.core.meta.EntityMetaFactory.class?type.cast(metadata):null; }
            @Override public void putAttribute(String key,Object value) { values.put(key,value); }
            @Override public Object getAttribute(String key) { return values.get(key); }
            @Override public <T> T getAttribute(String key,Class<T> type) { return type.cast(values.get(key)); }
        };
        return context.withActiveRoot(new ContextEntityRef("Tenant",1L));
    }
    @Test public void aModelDeclaredRootSubtypeIsAcceptedWithoutCreatingMutationState() {
        var row=loaded();row.setProperty("domain_root",new SpecializedRef(1));
        var state=row.__internalLoadState();var context=modelContext(true);verify(context,row);
        assertNull(context.getAttribute(Checker.TEAQL_DATA_CHECK_RESULT));
        assertFalse(row.__internalHasMutationLedger());assertSame(state,row.__internalLoadState());
    }
    @Test public void rootSubtypeProvenanceDoesNotPermitTransferToAnotherRoot() {
        var row=loaded();row.setProperty("domain_root",new SpecializedRef(1));row.updateProperty("domain_root",new SpecializedRef(2));
        var context=modelContext(true);context.withActiveRoot(new ContextEntityRef("Tenant",2L));
        assertThrows(ContextRootException.class,()->verify(context,row));
    }
    @Test public void subtypeAcceptanceUsesOnlyThisContextsDeclaredModel() {
        var row=loaded();row.setProperty("domain_root",new SpecializedRef(1));
        var unrelated=modelContext(false);verify(unrelated,row);assertInvalid(unrelated);
        var related=modelContext(true);verify(related,row);assertNull(related.getAttribute(Checker.TEAQL_DATA_CHECK_RESULT));
        var absent=context(1);verify(absent,row);assertInvalid(absent);
    }
    @Test public void sameIdOnAnUnrelatedRootTypeIsStillRejected() {
        var row=loaded();row.setProperty("domain_root",new UnrelatedRef(1));
        var context=modelContext(true);verify(context,row);assertInvalid(context);
    }
    @Test public void aSubtypeNameNotRegisteredInThisContextFailsAsAFieldViolation() {
        var row=loaded();row.setProperty("domain_root",new Ref(1) {
            @Override public String typeName() { return "UnregisteredTenant"; }
        });
        var context=modelContext(true);verify(context,row);assertInvalid(context);
    }
    @Test public void declaredInheritanceCannotSubstituteAnIncompatibleJavaReference() {
        var row=loaded();row.setProperty("domain_root",new UnrelatedRef(1) {
            @Override public String typeName() { return "SpecializedTenant"; }
        });
        var context=modelContext(true);verify(context,row);assertInvalid(context);
    }
    @SuppressWarnings("unchecked") static void assertInvalid(UserContext context) {
        var results=(List<CheckResult>)context.getAttribute(Checker.TEAQL_DATA_CHECK_RESULT);assertNotNull(results);assertEquals(1,results.size());
        var result=results.get(0);assertEquals("PurchaseOrder",result.getRootType());assertEquals("domain_root",result.getLocation().modelPath());
        assertEquals("invalid_type",result.toWire(io.teaql.core.checker.JsonFieldNamingProfile.CAMEL_CASE).ruleId());
    }
}
