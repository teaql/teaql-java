package io.teaql.core.sqlite;

import io.teaql.provider.jdbc.JdbcSqlExecutor;
import java.sql.Connection;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class SqliteGbkFunctionsTest {
    @Test public void encodedBytesUsePostgresCompatibleProfileAndKeepNullDistinctFromEmpty() throws Exception {
        try(var source=new SingleConnectionDataSource("jdbc:sqlite::memory:")) {
            var jdbc=new JdbcSqlExecutor(source);
            new SqliteDataServiceExecutor("sqlite",jdbc,source);
            var row=jdbc.queryForList("SELECT hex(teaql_gbk(?)) bytes,teaql_gbk(NULL) n,"
                    +"typeof(teaql_gbk('')) emptyType,length(teaql_gbk('')) emptySize",
                    new Object[]{"€阿文中"}).get(0);
            assertEquals("80B0A2CEC4D6D0",row.get("bytes"));
            assertNull(row.get("n"));assertEquals("blob",row.get("emptytype"));
            assertEquals(0,((Number)row.get("emptysize")).intValue());
        }
    }

    @Test public void unmappablePrivateUseNulAndNonTextInputsFailWithoutEchoingPrivateValues() throws Exception {
        try(var source=new SingleConnectionDataSource("jdbc:sqlite::memory:")) {
            var jdbc=new JdbcSqlExecutor(source);
            new SqliteDataServiceExecutor("sqlite",jdbc,source);
            for(Object input:List.of("PRIVATE-GBK-CANARY😀","PRIVATE-GBK-CANARY\uE000",
                    "PRIVATE-GBK-CANARY\u01F9","PRIVATE-GBK-CANARY\u0000",7L,1.5,new byte[]{0x31})) {
                var error=assertThrows(RuntimeException.class,()->jdbc.queryForList("SELECT teaql_gbk(?)",new Object[]{input}));
                boolean classified=false;
                for(Throwable cause=error;cause!=null;cause=cause.getCause()) {
                    String message=String.valueOf(cause.getMessage());
                    assertFalse(message.contains("PRIVATE-GBK-CANARY"));
                    classified|=message.contains("GBK requires TEXT")||message.contains("GBK cannot encode");
                }
                assertTrue("Expected a sanitized typed encoding diagnostic",classified);
            }
        }
    }

    @Test public void everyPhysicalConnectionHasTheEncoderBeforeItsFirstQuery() throws Exception {
        var source=new org.sqlite.SQLiteDataSource();source.setUrl("jdbc:sqlite::memory:");
        var jdbc=new JdbcSqlExecutor(source);new SqliteDataServiceExecutor("sqlite",jdbc,source);
        for(int attempt=0;attempt<2;attempt++)
            assertEquals("80",jdbc.queryForList("SELECT hex(teaql_gbk('€')) b",new Object[0]).get(0).get("b"));
    }
}
