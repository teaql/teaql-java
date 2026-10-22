package io.teaql.sqlite;

import io.teaql.core.*;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.util.List;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** A real ordinary executor and the signed receiver deliberately share storage. */
public class SqliteCommittedReadOnlyTest {
    public static final class School extends BaseEntity {
        private String name;
        @Override public String typeName(){return "school";}
        @Override public Object __internalGet(String field){return "name".equals(field)?name:super.__internalGet(field);}
        @Override public void __internalSet(String field,Object value){if("name".equals(field))name=(String)value;else super.__internalSet(field,value);}
    }
    private record Application(DefaultUserContext context,SqliteDataServiceExecutor provider){}
    private static Application application(JdbcSqlExecutor sql,io.teaql.core.meta.EntityMetaFactory metadata){
        var descriptor=metadata.resolveEntityDescriptor("school");descriptor.setTargetType(School.class);descriptor.withEntitySupplier(School::new);
        var provider=new SqliteDataServiceExecutor("default",sql,null);
        var context=new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).dataService("default",provider)
                .idGenerationService((c,e)->99L).executionLogging(false).build());
        return new Application(context,provider);
    }
    private static School school(){var s=new School();s.updateProperty("name","ordinary write");s.setComment("verify default analytical target write boundary");return s;}
    private static void readonly(Runnable action){var error=assertThrows("analytical target ordinary mutation must be read-only",RuntimeException.class,()->action.run());assertTrue(error.toString(),error.toString().contains("READ_ONLY_TARGET"));}
    @Test public void graphSaveCannotWriteThroughReceiverAuthority()throws Exception{
        try(var f=new SqliteCommittedBoundaryTest.Fixture()){
            var app=application(f.sql,EntityMetaFactory.requireFrom(f.context));
            readonly(()->app.context().saveGraph(school()));assertEquals(0,f.count("school_data"));
            assertFalse(f.receive(SqliteCommittedBoundaryTest.body()).duplicate());assertEquals(1,f.count("school_data"));
            assertEquals(1,f.count("teaql_event_inbox"));assertEquals(0,f.count("teaql_event_rejection"));
        }
    }
    @Test public void directAndBatchMutationCannotBypassReadOnlyTarget()throws Exception{
        for(boolean batch:List.of(false,true))try(var f=new SqliteCommittedBoundaryTest.Fixture()){
            var app=application(f.sql,EntityMetaFactory.requireFrom(f.context));var s=school();s.setProperty("id",99L);
            var request=new EntityPersistenceMutation(s,EntityPersistenceMutation.Action.SAVE);
            if(batch)readonly(()->app.provider().mutateBatch(app.context(),new MutationBatchRequest(request.intent(),List.of(request))));
            else readonly(()->app.provider().mutate(app.context(),request));
            assertEquals(0,f.count("school_data"));assertEquals(0,f.count("teaql_event_inbox"));
        }
    }
    @Test public void ordinarySourceIsNotMadeReadOnlyByAnotherTargetsReceiver()throws Exception{
        try(var f=new SqliteCommittedBoundaryTest.Fixture()){
            var ds=new SQLiteDataSource();ds.setUrl("jdbc:sqlite:"+f.owner.resolve("ordinary.sqlite"));var sql=new JdbcSqlExecutor(ds);
            sql.execute("CREATE TABLE school_data(id INTEGER PRIMARY KEY,version INTEGER NOT NULL,name TEXT NOT NULL)");
            var app=application(sql,EntityMetaFactory.requireFrom(f.context));app.context().saveGraph(school());
            assertEquals(1,((Number)sql.queryForList("SELECT count(*) n FROM school_data",new Object[0]).get(0).get("n")).intValue());
            assertEquals(0,f.count("school_data"));assertFalse(f.receive(SqliteCommittedBoundaryTest.body()).duplicate());
            System.out.println("CMR15_READ_ONLY_SOURCE_PASS language=java graph=1 direct=1 batch=1 source_isolated=1 replication=1");
        }
    }
}
