package io.teaql.examples.inheritance;

import com.example.inheritancematchingservice.*;
import io.teaql.core.*;
import io.teaql.runtime.SafeAuditEvent;
import java.util.*;
import javax.sql.DataSource;
import static org.junit.Assert.*;

/** Reject-only, maintainer-approved fixture DDL; business I/O remains Q/E/save. */
final class GraphRollbackProbe {
    static Map<String,Object> verify(UserContext context,long id,List<SafeAuditEvent> events,
            DataSource source,String provider,int round) throws Exception {
        if(round==1)try(var connection=source.getConnection();var sql=connection.createStatement()) {
            if(provider.equals("sqlite"))sql.execute("CREATE TRIGGER owned_reject_late_school BEFORE UPDATE ON school_data WHEN NEW.name='ROLLBACK_REJECT' BEGIN SELECT CASE WHEN (SELECT name FROM platform_data WHERE id=1)=('Graph root '||NEW.id) THEN RAISE(ABORT,'owned late school fault') ELSE RAISE(ABORT,'owned fault before earlier root write') END; END;");
            else {
                sql.execute("CREATE FUNCTION owned_reject_late_school() RETURNS trigger LANGUAGE plpgsql AS $owned$ BEGIN IF NEW.name='ROLLBACK_REJECT' THEN IF (SELECT name FROM platform_data WHERE id=1)=('Graph root '||NEW.id::text) THEN RAISE EXCEPTION 'owned late school fault'; ELSE RAISE EXCEPTION 'owned fault before earlier root write'; END IF; END IF; RETURN NEW; END $owned$");
                sql.execute("CREATE TRIGGER owned_reject_late_school BEFORE UPDATE ON school_data FOR EACH ROW EXECUTE FUNCTION owned_reject_late_school()");
            }
        }
        var root=Q.platforms().withIdIs(1L)
                .selectSchoolListWith(Q.schools().withIdIs(id).limit(1)).limit(1)
                .comment("Load the complete graph for transactional fault injection")
                .purpose("Verify two logical participants commit through one audited save").executeForOne(context);
        assertNotNull(root);assertEquals(Integer.valueOf(1),E.platform(root).getSchoolList().size().eval());
        var child=root.getSchoolList().get(0);
        long rootVersion=root.getVersion();String rootName=E.platform(root).getName().eval();
        String childName=E.school(child).getName().eval();assertEquals(Long.valueOf(7),child.getVersion());
        root.updateName("Graph root "+id);child.updateName("ROLLBACK_REJECT");
        child.setComment("Correct the selected child in the shared graph");
        int before=events.size();
        RuntimeException failure=assertThrows(RuntimeException.class,()->root.auditAs("Exercise rollback after an earlier logical participant wrote").save(context));
        boolean late=false;for(Throwable cause=failure;cause!=null;cause=cause.getCause())if(String.valueOf(cause.getMessage()).contains("owned late school fault"))late=true;
        assertTrue("The reject-only trigger must observe the earlier root write",late);
        assertEquals(before,events.size());assertEquals(Long.valueOf(rootVersion),root.getVersion());
        var persistedRoot=Q.platforms().withIdIs(1L).limit(1).comment("Read the root after child failure")
                .purpose("Prove the earlier root write and its version were rolled back").executeForOne(context);
        var persistedChild=Q.schools().withIdIs(id).limit(1).comment("Read the child after injected failure")
                .purpose("Reject partial graph persistence").executeForOne(context);
        assertEquals(Long.valueOf(rootVersion),persistedRoot.getVersion());assertEquals(rootName,E.platform(persistedRoot).getName().eval());
        assertEquals(Long.valueOf(7),persistedChild.getVersion());assertEquals(childName,E.school(persistedChild).getName().eval());
        assertTrue(child.getUpdatedProperties().contains("name"));
        child.updateName("Graph child "+id);
        root.auditAs("Retry the same graph after correcting the rejected child").save(context);
        assertEquals(Long.valueOf(rootVersion+1),root.getVersion());
        var retried=Q.schools().withIdIs(id).limit(1).comment("Read the retried graph child")
                .purpose("Verify retained ledger intent commits only once").executeForOne(context);
        assertEquals(Long.valueOf(8),retried.getVersion());assertEquals("Graph child "+id,E.school(retried).getName().eval());
        assertEquals(before+2,events.size());
        return Map.of("passed",true,"id",id,"rootVersionBefore",rootVersion,"rootVersionAfter",root.getVersion(),
                "childVersionAfter",8,"failedAudits",0,"retryAudits",2,"generatedQAndE",true,"retrySameGraph",true);
    }
}
