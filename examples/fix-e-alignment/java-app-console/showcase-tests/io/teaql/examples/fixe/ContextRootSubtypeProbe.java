package io.teaql.examples.fixe;

import com.teaql.checkerfixadvanced.E;
import com.teaql.checkerfixadvanced.tenant.Tenant;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder;
import io.teaql.core.*;
import io.teaql.core.checker.CheckException;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.meta.EntityDescriptor;
import java.util.*;
import static org.junit.Assert.*;

/** Runtime-owned metadata extension; generated library bytes stay unchanged. */
final class ContextRootSubtypeProbe {
    public static final class SpecializedTenant extends Tenant {
        @Override public String typeName() { return "SpecializedTenant"; }
    }
    public static final class UnknownTenant extends Tenant {
        @Override public String typeName() { return "UnregisteredTenant"; }
    }
    static Tenant reference(Tenant value,long id) {
        value.setProperty("id",id);value.set$status(EntityStatus.REFER);return value;
    }
    static PurchaseOrder loaded(ContextRootProbe.Fixture f,Tenant root) {
        var row=f.loaded(f.saved().getId());
        // Framework-only hydration path, not a forbidden public root updater.
        row.setProperty(FieldLayout.forType(row.getClass()).memberName("tenant"),root);
        row.updateContactPhone("5552001");return row;
    }
    static Map<String,Object> reject(ContextRootProbe.Fixture f,String name,PurchaseOrder row,
            UserContext context,String expected) throws Exception {
        var before=f.counters();var stored=f.storage();String actual=null;
        try { row.auditAs("Reject invalid specialized-root ownership").save(context); }
        catch(ContextRootException error) { actual=error.getReason().name(); }
        catch(CheckException error) { actual=error.getViolates().get(0).toWire(JsonFieldNamingProfile.CAMEL_CASE).ruleId(); }
        catch(TeaQLRuntimeException error) {
            if(!"entityDescriptor UnregisteredTenant cannot be resolved".equals(error.getMessage())) throw error;
            actual="unregistered_model";
        }
        var delta=f.delta(before);assertEquals(expected,actual);assertEquals(stored,f.storage());
        assertTrue(delta.values().stream().allMatch(v->v==0));
        return Map.of("case",name,"id",row.getId(),"expected",expected,"actual",actual,
                "effects",delta,"storageUnchanged",true,"passed",true);
    }
    static void verify() throws Exception {
        var f=new ContextRootProbe.Fixture(System.getenv("TEAQL_FIX_E_DB")+".root-subtype.sqlite");
        var declared=f.metadata.resolveEntityDescriptor("Tenant");
        var subtype=new EntityDescriptor();subtype.setType("SpecializedTenant");
        subtype.setTargetType(SpecializedTenant.class);subtype.setEntitySupplier(SpecializedTenant::new);
        subtype.setParent(declared);f.metadata.register(subtype);
        var row=loaded(f,reference(new SpecializedTenant(),1));var before=f.counters();
        row.auditAs("Update an object whose loaded root uses a declared subtype").save(f.context);
        var positive=f.delta(before);assertTrue(positive.get("writes")>0);
        assertEquals(Integer.valueOf(1),positive.get("audits"));assertEquals(Integer.valueOf(0),positive.get("rootLookups"));
        assertEquals(Long.valueOf(2),row.getVersion());
        var reloaded=f.loaded(row.getId());assertEquals("5552001",E.purchaseOrder(reloaded).getContactPhone().eval());
        assertEquals(Long.valueOf(1),E.purchaseOrder(reloaded).getTenant().getId().eval());
        var cases=new ArrayList<Map<String,Object>>();
        cases.add(reject(f,"subtype-wrong-context",loaded(f,reference(new SpecializedTenant(),1)),
                f.withRoot("Tenant",f.otherRoot),"VALUE_MISMATCH"));
        var moved=loaded(f,reference(new SpecializedTenant(),1));
        moved.updateProperty(FieldLayout.forType(moved.getClass()).memberName("tenant"),reference(new SpecializedTenant(),f.otherRoot));
        cases.add(reject(f,"subtype-context-and-payload-transfer",moved,f.withRoot("Tenant",f.otherRoot),"VALUE_MISMATCH"));
        cases.add(reject(f,"unregistered-subtype",loaded(f,reference(new UnknownTenant(),1)),f.context,"unregistered_model"));
        var detached=loaded(f,reference(new SpecializedTenant(),1));subtype.setParent(null);
        try { cases.add(reject(f,"subtype-without-model-inheritance",detached,f.context,"invalid_type")); }
        finally { subtype.setParent(declared); }
        var result=new LinkedHashMap<String,Object>();result.put("passed",true);result.put("language","java");
        result.put("metadataSource","invoking-context");result.put("positiveId",row.getId());
        result.put("positiveEffects",positive);result.put("cases",cases);result.put("finalStorage",f.storage());
        result.put("generatedLibraryModified",false);result.put("queryAuthorizationInferred",false);
        System.out.println("JAVA_CONTEXT_ROOT_SUBTYPE_RESULT="+ContextRootProbe.JSON.writeValueAsString(result));
    }
}
