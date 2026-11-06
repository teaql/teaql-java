package io.teaql.provider.springjdbc;

import io.teaql.core.BaseEntity;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;
import static org.junit.Assert.*;

/** The Spring provider must implement the same typed numeric boundary as JDBC. */
@RunWith(Parameterized.class)
public class SpringJdbcExactNumericReadTest {
    private record Vector(String name, String expression, Class<?> target, Object expected, boolean rejected) {}
    private final Vector vector;
    private final List<Connection> connections = new ArrayList<>();
    public SpringJdbcExactNumericReadTest(String name, Vector vector) { this.vector = vector; }
    private static Vector good(String name, String sql, Class<?> type, Object expected) { return new Vector(name,sql,type,expected,false); }
    private static Vector bad(String name, String sql, Class<?> type) { return new Vector(name,sql,type,null,true); }
    @Parameterized.Parameters(name="{0}") public static Collection<Object[]> vectors() {
        return List.of(
                bad("integer-fraction","CAST(1.9 AS DECIMAL(30,3))",Integer.class),
                bad("integer-overflow","CAST(2147483648 AS BIGINT)",Integer.class),
                bad("long-fraction","CAST(-1.9 AS DECIMAL(30,3))",Long.class),
                bad("long-overflow","CAST(9223372036854775808 AS DECIMAL(40,0))",Long.class),
                good("decimal-large-long","CAST(9007199254740993 AS BIGINT)",BigDecimal.class,new BigDecimal("9007199254740993")),
                good("decimal-minimum-long","CAST(-9223372036854775808 AS BIGINT)",BigDecimal.class,new BigDecimal("-9223372036854775808")),
                good("decimal-exact-text","CAST('12345678901234567890.123456789' AS VARCHAR)",BigDecimal.class,new BigDecimal("12345678901234567890.123456789")),
                bad("double-infinity","1e999",Double.class),
                bad("float-overflow","1e40",Float.class),
                bad("private-invalid-text","CAST('PRIVATE_SPRING_NUMERIC_901' AS VARCHAR)",BigDecimal.class),
                good("integer-maximum","CAST(2147483647 AS INTEGER)",Integer.class,Integer.MAX_VALUE),
                good("long-minimum","CAST(-9223372036854775808 AS BIGINT)",Long.class,Long.MIN_VALUE),
                good("finite-float","CAST(1.25 AS DOUBLE)",Float.class,1.25f))
                .stream().map(v -> new Object[]{v.name,v}).toList();
    }
    @Test public void typedReadIsExactOrRejectsWithoutPrivateValues() throws Exception {
        String url="jdbc:h2:mem:spring_exact_"+UUID.randomUUID();
        var data = new AbstractDataSource() {
            @Override public Connection getConnection() throws SQLException { var c=DriverManager.getConnection(url); connections.add(c); return c; }
            @Override public Connection getConnection(String user,String password) throws SQLException { return getConnection(); }
        };
        var adapter=new SpringJdbcSqlExecutor(new NamedParameterJdbcTemplate(data));
        String sql="SELECT "+vector.expression+" AS required_numeric";
        // SQL and the driver must succeed before a conversion rejection counts.
        var calibrated=adapter.queryForList(sql,new Object[0]);assertEquals(1,calibrated.size());assertEquals(1,calibrated.get(0).size());
        if(vector.rejected){
            var error=assertThrows(IllegalArgumentException.class,()->read(adapter,sql,vector.target));
            assertTrue(error.getMessage(),error.getMessage().contains("column 1"));
            assertTrue(error.getMessage(),error.getMessage().contains("required_numeric"));
            assertTrue(error.getMessage(),error.getMessage().contains(vector.target.getName()));
            assertFalse(error.toString().contains("PRIVATE_SPRING_NUMERIC_901"));assertNull(error.getCause());
        }else{
            Object value=null;
            try { value=read(adapter,sql,vector.target); }
            catch(RuntimeException failure){ fail("A valid typed numeric value was rejected: "+failure.getClass().getName()); }
            assertEquals(vector.name,vector.expected,value);
        }
        assertEquals(2,connections.size());assertClosed();
        assertEquals(Long.valueOf(7),read(adapter,"SELECT 7 AS required_numeric",Long.class));assertEquals(3,connections.size());assertClosed();
    }
    private Object read(SpringJdbcSqlExecutor adapter,String sql,Class<?> target){
        return adapter.query(sql,new Object[0],row->{var entity=new ScalarRow();entity.value=row.get(1,target);return entity;}).get(0).value;
    }
    private void assertClosed() throws SQLException { for(var c:connections)assertTrue("typed read leaked its owned connection",c.isClosed()); }
    private static final class ScalarRow extends BaseEntity { Object value; }
}
