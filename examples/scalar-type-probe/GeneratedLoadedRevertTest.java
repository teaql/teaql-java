package com.example.scalartypeconformanceservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.*;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/** Native Q/E must agree with the value actually committed from the ledger. */
public class GeneratedLoadedRevertTest {
    @Test public void restoringKnownValuesCannotSaveIntermediateValues() throws Exception {
        int round=Integer.parseInt(System.getenv("TEAQL_NAME_ROUND"));
        var f=new GeneratedCanonicalNameTest.Fixture("jdbc:sqlite:"+Path.of(System.getenv("TEAQL_NAME_DB")).toAbsolutePath(),round);
        var root=f.root("Loaded revert root "+round);var created=f.probe(root,"loaded revert "+round).updateOptionalInteger(37);
        created.auditAs("Persist the loaded-revert fixture").save(f.context);long id=created.getId();
        var full=f.load(id);var state=full.__internalLoadState();long before=f.data.calls;int policies=f.policies.size(),audits=f.audits.size();
        full.updateOptionalInteger(99);full.updateOptionalInteger(37);
        assertEquals(Integer.valueOf(37),E.scalarTypeProbe(full).getOptionalInteger().eval());
        full.auditAs("Save after restoring the original integer").save(f.context);
        // Check actual storage before the no-op counters. The native baseline
        // retains the intermediate intent; the real save rewrites the restored
        // value but incorrectly bumps version from 1 to 2.
        try(var connection=DriverManager.getConnection(f.data.url);var statement=connection.prepareStatement("SELECT optional_integer,version FROM scalar_type_probe_data WHERE id=?")){
            statement.setLong(1,id);try(var rows=statement.executeQuery()){assertTrue(rows.next());assertEquals("restored value must match persisted value",37,rows.getInt(1));assertEquals(1L,rows.getLong(2));assertFalse(rows.next());}
        }
        assertEquals(before,f.data.calls);assertEquals(policies,f.policies.size());assertEquals(audits,f.audits.size());
        assertSame(state,full.__internalLoadState());assertEquals(EntityStatus.PERSISTED,full.get$status());assertEquals(Long.valueOf(1),full.getVersion());

        var nullOwner=f.load(id);before=f.data.calls;nullOwner.updateOptionalString("discard this intermediate value");nullOwner.updateOptionalString(null);
        assertNull(E.scalarTypeProbe(nullOwner).getOptionalString().eval());
        nullOwner.auditAs("Save after restoring the original null").save(f.context);
        assertEquals(before,f.data.calls);assertEquals(policies,f.policies.size());assertEquals(audits,f.audits.size());assertEquals(Long.valueOf(1),nullOwner.getVersion());

        var changed=f.load(id);changed.updateRequiredString("loaded revert "+round+" saved");changed.updateOptionalInteger(99);changed.updateOptionalInteger(37);
        assertEquals(Set.of("required_string"),Set.copyOf(changed.getUpdatedProperties()));
        changed.auditAs("Save only the genuinely changed field").save(f.context);
        assertEquals(policies+1,f.policies.size());assertEquals(audits+1,f.audits.size());assertEquals(Long.valueOf(2),changed.getVersion());
        var plan=f.policies.get(policies);assertEquals(1,plan.operations().size());
        // This evaluated model declares update_time=updateTime(): a genuine
        // business edit must retain that normal Checker/Fix contribution.
        assertEquals(Set.of("required_string","update_time"),plan.operations().get(0).changedValues().keySet());
        assertNotNull(plan.operations().get(0).changedValues().get("update_time"));
        var loaded=f.load(id);assertEquals(Integer.valueOf(37),E.scalarTypeProbe(loaded).getOptionalInteger().eval());assertNull(E.scalarTypeProbe(loaded).getOptionalString().eval());
        assertEquals("loaded revert "+round+" saved",E.scalarTypeProbe(loaded).getRequiredString().eval());
        assertNotNull(E.scalarTypeProbe(loaded).getUpdateTime().eval());
        try(var connection=DriverManager.getConnection(f.data.url);var statement=connection.prepareStatement("SELECT version,name FROM type_platform_data WHERE id=?")){
            statement.setLong(1,root.getId());try(var rows=statement.executeQuery()){assertTrue(rows.next());assertEquals(1L,rows.getLong(1));assertEquals("Loaded revert root "+round,rows.getString(2));assertFalse(rows.next());}
        }
        var result=new LinkedHashMap<String,Object>();result.put("passed",true);result.put("profile","camelCase");result.put("round",round);result.put("id",id);result.put("rootId",root.getId());result.put("finalVersion",2);
        for(String flag:List.of("knownValueRestored","knownNullRestored","emptyPlanNoWrite","otherFieldPreserved","typedQAndE","versionUnchangedUntilRealEdit","unrelatedParentUnchanged","managedUpdateTimeRetained"))result.put(flag,true);
        result.put("fullExampleInventoryAccepted",false);result.put("internalArtifactAccepted",false);
        System.out.println("JAVA_LOADED_REVERT="+new ObjectMapper().writeValueAsString(result));
    }
}
