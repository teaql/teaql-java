package com.example.scalartypeconformanceservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.*;
import io.teaql.core.checker.CheckException;
import io.teaql.core.value.TeaQLNotLoadedException;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/** Explicit null is an edit, not evidence that an unselected value was null. */
public class GeneratedExplicitNullTest {
    @Test public void explicitNullAndLoadedNoopUseDifferentMutationPaths() throws Exception {
        int round=Integer.parseInt(System.getenv("TEAQL_NAME_ROUND"));
        var f=new GeneratedCanonicalNameTest.Fixture("jdbc:sqlite:"+Path.of(System.getenv("TEAQL_NAME_DB")).toAbsolutePath(),round);
        var root=f.root("Explicit null root "+round);
        var created=f.probe(root,"explicit null "+round).updateOptionalInteger(37);
        created.auditAs("Persist the explicit-null fixture").save(f.context);
        long id=created.getId();

        // This first assertion is a generated-code negative control: the old
        // equals(null,null) shortcut incorrectly skipped an explicit edit.
        var typed=Q.scalarTypeProbesWithMinimalFields().withIdIs(id).selectRequiredString().limit(1)
                .comment("Load a partial explicit-null probe").purpose("Distinguish unselected from selected null").executeForOne(f.context);
        assertFalse(typed.isPropertyLoaded("optional_integer"));
        assertThrows(TeaQLNotLoadedException.class,()->E.scalarTypeProbe(typed).getOptionalInteger().eval());
        long before=f.data.calls;int policies=f.policies.size(),audits=f.audits.size();
        typed.updateOptionalInteger(null);
        assertTrue("explicit null must select the previously missing field",typed.isPropertyLoaded("optional_integer"));
        assertNull(E.scalarTypeProbe(typed).getOptionalInteger().eval());
        assertEquals(Set.of("optional_integer"),Set.copyOf(typed.getUpdatedProperties()));
        var key=new EntityKey("ScalarTypeProbe",id);var ledger=typed.getEntityMutationLedger();
        assertTrue(ledger.currentChangeSet().changes().get(key).containsKey("optional_integer"));
        assertNull(ledger.currentChangeSet().changes().get(key).get("optional_integer"));
        assertEquals(Long.valueOf(1),typed.getOriginalVersion());
        // Unknown original value must remain unknown after multiple edits.
        typed.updateOptionalInteger(99);typed.updateOptionalInteger(null);typed.updateOptionalInteger(null);
        assertEquals(Set.of("optional_integer"),Set.copyOf(typed.getUpdatedProperties()));
        assertTrue(ledger.changedFieldNames(key).contains("optional_integer"));
        assertThrows(CheckException.class,()->typed.auditAs("Reject a still-incomplete edited object").save(f.context));
        assertEquals(before,f.data.calls);assertEquals(policies,f.policies.size());assertEquals(audits,f.audits.size());

        var generic=Q.scalarTypeProbesWithMinimalFields().withIdIs(id).selectRequiredString().limit(1)
                .comment("Load a generic explicit-null probe").purpose("Verify the runtime mutation path").executeForOne(f.context);
        before=f.data.calls;generic.updateProperty("optional_integer",null);
        assertTrue(generic.isPropertyLoaded("optional_integer"));assertNull(E.scalarTypeProbe(generic).getOptionalInteger().eval());
        assertEquals(Set.of("optional_integer"),Set.copyOf(generic.getUpdatedProperties()));
        assertTrue(generic.getEntityMutationLedger().currentChangeSet().changes().get(key).containsKey("optional_integer"));
        assertThrows(CheckException.class,()->generic.auditAs("Reject the incomplete generic edit").save(f.context));
        assertEquals(before,f.data.calls);

        var full=f.load(id);var state=full.__internalLoadState();before=f.data.calls;
        assertFalse(full.__internalHasMutationLedger());
        full.updateOptionalInteger(37);full.updateOptionalString(null);
        full.updateProperty("optional_integer",37);full.updateProperty("optional_string",null);
        full.handleUpdate("optional_integer",37,37);full.updateId(id);full.updateVersion(1L);
        assertSame(state,full.__internalLoadState());assertEquals(EntityStatus.PERSISTED,full.get$status());
        assertTrue(full.getUpdatedProperties().isEmpty());assertFalse(full.__internalHasMutationLedger());
        full.auditAs("Confirm equal loaded values produce no write").save(f.context);
        assertEquals(before,f.data.calls);assertEquals(policies,f.policies.size());assertEquals(audits,f.audits.size());
        assertEquals(Long.valueOf(1),full.getVersion());

        var reference=E.scalarTypeProbe(full).getTypePlatform().eval();var referenceState=reference.__internalLoadState();
        boolean hadLedger=reference.__internalHasMutationLedger();
        var referenceLedger=hadLedger?reference.getEntityMutationLedger():null;
        assertFalse(reference.isPropertyLoaded("version"));
        assertThrows(TeaQLRuntimeException.class,()->reference.updateVersion(null));
        assertSame(referenceState,reference.__internalLoadState());assertEquals(EntityStatus.REFER,reference.get$status());
        assertEquals(hadLedger,reference.__internalHasMutationLedger());
        if(hadLedger)assertSame(referenceLedger,reference.getEntityMutationLedger());

        full.updateOptionalInteger(null);full.auditAs("Clear the selected optional integer").save(f.context);
        assertEquals(Long.valueOf(2),full.getVersion());
        assertEquals(policies+1,f.policies.size());assertEquals(audits+1,f.audits.size());
        var plan=f.policies.get(policies);assertEquals(1,plan.operations().size());
        assertEquals("ScalarTypeProbe",plan.operations().get(0).entity().entity());
        var loaded=f.load(id);assertTrue(loaded.isPropertyLoaded("optional_integer"));
        assertNull(E.scalarTypeProbe(loaded).getOptionalInteger().eval());
        assertEquals("explicit null "+round,E.scalarTypeProbe(loaded).getRequiredString().eval());
        try(var connection=DriverManager.getConnection(f.data.url);var statement=connection.prepareStatement("SELECT optional_integer,version FROM scalar_type_probe_data WHERE id=?")){
            statement.setLong(1,id);try(var rows=statement.executeQuery()){assertTrue(rows.next());assertNull(rows.getObject(1));assertEquals(2L,rows.getLong(2));assertFalse(rows.next());}
        }
        var result=new LinkedHashMap<String,Object>();result.put("passed",true);result.put("profile","camelCase");result.put("round",round);result.put("id",id);result.put("rootId",root.getId());result.put("finalVersion",2);
        for(String flag:List.of("typedExplicitNull","genericExplicitNull","unknownBaselineRetained","partialRejectedBeforeProvider","loadedEqualNoop","referenceNullRejected","savedNullReadback"))result.put(flag,true);
        result.put("fullExampleInventoryAccepted",false);result.put("internalArtifactAccepted",false);
        System.out.println("JAVA_EXPLICIT_NULL="+new ObjectMapper().writeValueAsString(result));
    }
}
