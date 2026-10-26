package io.teaql.core;

import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/** Rejected operations cannot rewrite payload, availability, lifecycle or intent. */
@RunWith(Parameterized.class)
public class RejectedMutationStateTest {
    enum Availability {NOT_LOADED, NULL, VALUE}
    @Parameterized.Parameters(name="{0}-{1}") public static Collection<Object[]> cases(){
        var result=new ArrayList<Object[]>();
        for(var state:List.of(EntityStatus.REFER,EntityStatus.PERSISTED_DELETED,EntityStatus.UPDATED_RECOVER))
            for(var availability:Availability.values())result.add(new Object[]{state,availability});
        return result;
    }
    static class Row extends BaseEntity {
        static final FieldLayout LAYOUT=FieldLayout.installGenerated(FieldLayout.generated(Row.class,"rejected-mutation-v1",
                Map.of("id",0,"version",1,"name",2,"count",3),
                Map.of("id",List.of("id","id"),"version",List.of("version","version"),"name",List.of("name","name"),"count",List.of("count","count")),Set.of("owner")));
        String name;Integer count;Row owner;
        @Override public String typeName(){return "GuardedRow";}
        @Override public Object __internalGet(String field){return switch(field){case "name"->name;case "count"->count;case "owner"->owner;default->super.__internalGet(field);};}
        @Override public void __internalSet(String field,Object value){
            markPropertyLoaded(field);
            switch(field){case "name"->name=(String)value;case "count"->count=(Integer)value;case "owner"->owner=(Row)value;default->super.__internalSet(field,value);}
        }
        Row updateName(String value){
            // Same ordering as the current generated typed updater.
            if(Objects.equals(name,value))return this;
            handleUpdate("name",name,value);name=value;return this;
        }
    }
    static Row row(EntityStatus status,Availability availability){
        assertNotNull(Row.LAYOUT);var row=new Row();row.__internalHydrate("id",100L,0);
        if(status!=EntityStatus.REFER&&status!=EntityStatus.NEW)row.__internalHydrate("version",status==EntityStatus.PERSISTED_DELETED||status==EntityStatus.UPDATED_RECOVER?-7L:7L,1);
        if(availability!=Availability.NOT_LOADED)row.__internalHydrate("name",availability==Availability.VALUE?"kept":null,2);
        row.setComment("keep existing reason");row.setTraceChain(List.of(new TraceNode(TraceKind.AUDIT_REASON,row.typeName(),row.getId(),"keep existing lineage")));
        if(status==EntityStatus.UPDATED_RECOVER){row.set$status(EntityStatus.PERSISTED_DELETED);row.markAsRecover();}
        else row.set$status(status);
        return row;
    }
    static Row foreign(long id){var row=new Row();row.__internalHydrate("id",id,0);row.set$status(EntityStatus.REFER);return row;}
    static final class Snapshot {
        final LoadState state;final EntityStatus lifecycle;final String name,comment;final Integer count;final Long id,version,originalVersion;
        final Object owner;final List<TraceNode> trace;final List<String> updated;final EntityMutationLedger ledger;
        final Map<EntityKey,Map<String,Object>> changes;final Set<EntityKey> created,deleted,recovered;
        Snapshot(Row row){
            state=row.__internalLoadState();lifecycle=row.get$status();name=row.name;count=row.count;id=row.getId();version=row.getVersion();originalVersion=row.getOriginalVersion();
            owner=row.getProperty("owner");comment=row.getComment();trace=row.getTraceChain();updated=List.copyOf(row.getUpdatedProperties());
            ledger=row.__internalHasMutationLedger()?row.getEntityMutationLedger():null;
            changes=ledger==null?Map.of():ledger.currentChangeSet().changes();created=ledger==null?Set.of():Set.copyOf(ledger.newKeys());
            deleted=ledger==null?Set.of():Set.copyOf(ledger.deletedKeys());recovered=ledger==null?Set.of():Set.copyOf(ledger.recoveredKeys());
        }
        void unchanged(Row row){
            assertSame("rejection must retain the exact immutable load state",state,row.__internalLoadState());
            assertEquals(lifecycle,row.get$status());assertEquals(name,row.name);assertEquals(count,row.count);assertEquals(id,row.getId());assertEquals(version,row.getVersion());
            assertEquals(originalVersion,row.getOriginalVersion());assertSame(owner,row.getProperty("owner"));assertEquals(comment,row.getComment());assertEquals(trace,row.getTraceChain());
            assertEquals(updated,row.getUpdatedProperties());assertEquals(ledger!=null,row.__internalHasMutationLedger());
            if(ledger!=null){assertSame(ledger,row.getEntityMutationLedger());assertEquals(changes,ledger.currentChangeSet().changes());assertEquals(created,ledger.newKeys());assertEquals(deleted,ledger.deletedKeys());assertEquals(recovered,ledger.recoveredKeys());}
        }
    }
    private final EntityStatus status;private final Availability availability;
    public RejectedMutationStateTest(EntityStatus status,Availability availability){this.status=status;this.availability=availability;}
    void reject(Row row,Runnable action){var before=new Snapshot(row);assertThrows(TeaQLRuntimeException.class,action::run);before.unchanged(row);}
    @Test public void typedUpdateRejectsBeforeChangingAvailability(){var row=row(status,availability);reject(row,()->row.updateName("attempted typed value"));}
    @Test public void genericUpdateRejectsBeforeChangingPayload(){var row=row(status,availability);reject(row,()->row.updateProperty("name","attempted generic value"));}
    @Test public void versionUpdateRejectsBeforeClaimingASelectedVersion(){var row=row(status,availability);reject(row,()->row.updateVersion(9L));}
    @Test public void identityUpdateRejectsWithoutChangingTheKey(){var row=row(status,availability);reject(row,()->row.updateId(101L));}
    @Test public void directUpdateHookRejectsBeforeChangingAvailability(){var row=row(status,availability);reject(row,()->row.handleUpdate("name",row.name,"attempted hook value"));}
    @Test public void cacheAttachmentRejectsBeforeAddingAReference(){var row=row(status,availability);reject(row,()->row.cacheRelation("owner",foreign(200)));}
    @Test public void cacheReplacementRejectsBeforeOverwritingAPreviousReference(){
        var row=row(status,availability);row.set$status(EntityStatus.NEW);row.cacheRelation("owner",foreign(200));row.set$status(status);
        reject(row,()->row.cacheRelation("owner",foreign(201)));
    }
    @Test public void deletionRejectionAlreadyRetainsTheExistingState(){var row=row(status,availability);reject(row,row::markForDeletion);}
}
