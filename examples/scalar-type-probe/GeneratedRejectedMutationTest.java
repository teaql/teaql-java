package com.example.scalartypeconformanceservice;

import com.example.scalartypeconformanceservice.typeplatform.TypePlatform;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.*;
import io.teaql.core.checker.CheckException;
import io.teaql.core.value.TeaQLNotLoadedException;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.jackson.TeaQLModule;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real generated references must remain NotLoaded after rejected changes. */
public class GeneratedRejectedMutationTest {
    static void unchanged(TypePlatform reference,LoadState state){
        assertSame(state,reference.__internalLoadState());assertEquals(EntityStatus.REFER,reference.get$status());
        assertNull(reference.getVersion());assertNull(reference.getOriginalVersion());assertNull(reference.__internalGet("name"));
        assertFalse(reference.isPropertyLoaded("name"));assertFalse(reference.isPropertyLoaded("version"));
        assertFalse(reference.__internalHasMutationLedger());assertTrue(reference.getUpdatedProperties().isEmpty());
    }
    @Test public void rejectedChangesDoNotCorruptTheGeneratedGraph()throws Exception{
        int round=Integer.parseInt(System.getenv("TEAQL_NAME_ROUND"));
        var expected=Path.of(System.getProperty("teaql.expected.runtime.root")).toRealPath();
        for(var type:List.of(TeaQLRuntime.class,TeaQLModule.class))assertTrue(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().startsWith(expected));
        var f=new GeneratedCanonicalNameTest.Fixture("jdbc:sqlite:"+Path.of(System.getenv("TEAQL_NAME_DB")).toAbsolutePath(),round);
        var root=f.root("Rejected mutation root "+round);var created=f.probe(root,"rejected "+round);
        created.auditAs("Persist the rejected-mutation graph").save(f.context);
        var owner=f.load(created.getId());
        var reference=E.scalarTypeProbe(owner).getTypePlatform().eval();
        assertEquals(root.getId(),E.scalarTypeProbe(owner).getTypePlatform().getId().eval());
        var state=reference.__internalLoadState();unchanged(reference,state);
        long before=f.data.calls;int auditCount=f.audits.size(),policyCount=f.policies.size();int rejected=0;
        assertThrows(TeaQLRuntimeException.class,()->reference.updateName("must not appear"));unchanged(reference,state);rejected++;
        assertThrows(TeaQLRuntimeException.class,()->reference.updateProperty("name","must not appear either"));unchanged(reference,state);rejected++;
        assertThrows(TeaQLRuntimeException.class,()->reference.updateVersion(9L));unchanged(reference,state);rejected++;
        // Explicit negative fault injection through the legacy runtime cache
        // seam; this is not an application API discovery/recommendation.
        assertThrows(TeaQLRuntimeException.class,()->reference.cacheRelation("negative_cache",owner));unchanged(reference,state);rejected++;
        assertFalse(reference.isPropertyLoaded("negative_cache"));
        var missing=assertThrows(TeaQLNotLoadedException.class,()->E.scalarTypeProbe(owner).getTypePlatform().getName().eval());
        assertEquals("/typePlatform/name",missing.toWire().instancePath());
        assertEquals(before,f.data.calls);assertEquals(auditCount,f.audits.size());assertEquals(policyCount,f.policies.size());

        var partial=Q.scalarTypeProbesWithMinimalFields().withIdIs(created.getId()).selectRequiredString().limit(1)
                .comment("Load a partial mutation rejection probe").purpose("Keep missing fields distinct from rejected assignments").executeForOne(f.context);
        var partialState=partial.__internalLoadState();assertFalse(partial.__internalHasMutationLedger());before=f.data.calls;
        assertThrows(ClassCastException.class,()->partial.updateProperty("required_integer","wrong type"));rejected++;
        assertSame(partialState,partial.__internalLoadState());assertFalse(partial.isPropertyLoaded("required_integer"));
        assertFalse(partial.__internalHasMutationLedger());
        assertThrows(TeaQLNotLoadedException.class,()->E.scalarTypeProbe(partial).getRequiredInteger().eval());
        assertEquals(before,f.data.calls);
        partial.updateRequiredString("keep pending partial intent");
        assertThrows(CheckException.class,()->partial.auditAs("Reject an incomplete modified object").save(f.context));rejected++;
        assertEquals(before,f.data.calls);assertTrue(partial.needPersist());assertEquals(Long.valueOf(1),partial.getVersion());
        assertEquals(auditCount,f.audits.size());assertEquals(policyCount,f.policies.size());

        owner.updateRequiredString("rejected "+round+" saved");
        owner.auditAs("Save the complete owner after independent rejected changes").save(f.context);
        assertEquals(Long.valueOf(2),owner.getVersion());assertEquals(policyCount+1,f.policies.size());assertEquals(auditCount+1,f.audits.size());
        var plan=f.policies.get(policyCount);assertEquals(1,plan.operations().size());assertEquals(MutationOperationKind.UPDATE,plan.operations().get(0).kind());
        assertEquals("ScalarTypeProbe",plan.operations().get(0).entity().entity());
        f.oracle(owner.getId(),"rejected "+round+" saved",2,root.getId());
        assertEquals("rejected "+round+" saved",E.scalarTypeProbe(f.load(owner.getId())).getRequiredString().eval());
        try(var connection=DriverManager.getConnection(f.data.url);var statement=connection.prepareStatement("SELECT name,version FROM type_platform_data WHERE id=?")){
            statement.setLong(1,root.getId());try(var rows=statement.executeQuery()){assertTrue(rows.next());assertEquals("Rejected mutation root "+round,rows.getString(1));assertEquals(1L,rows.getLong(2));assertFalse(rows.next());}
        }
        var result=new LinkedHashMap<String,Object>();result.put("passed",true);result.put("profile","camelCase");result.put("round",round);result.put("id",owner.getId());result.put("rootId",root.getId());
        result.put("rejectedCalls",rejected);result.put("finalVersion",2);result.put("path",missing.toWire().instancePath());
        for(String key:List.of("payloadRetained","loadStateRetained","ledgerRetained","referenceNotLoaded","failedCastNotLoaded","zeroProviderOnRejection","completeOwnerSave","unrelatedParentUnchanged"))result.put(key,true);
        result.put("generatedEqualNullAccepted",false);
        System.out.println("JAVA_REJECTED_MUTATION="+new ObjectMapper().writeValueAsString(result));
    }
}
