package io.teaql.oracle;

import io.teaql.dataservice.sql.SqlDataServiceExecutor;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.Assume;
import org.junit.Test;
import static org.junit.Assert.*;

public class OracleTemporalSqlTest {
    @Test public void preparedAndDiagnosticSqlAreEquivalent() throws Exception {
        String url=System.getenv("TEAQL_TEST_ORACLE_URL"), user=System.getenv("TEAQL_TEST_ORACLE_USER"), password=System.getenv("TEAQL_TEST_ORACLE_PASSWORD");
        Assume.assumeTrue(url != null && user != null && password != null);
        try(Connection c=DriverManager.getConnection(url,user,password)) {
            drop(c); run(c,"CREATE TABLE teaql_temporal_java_fixture(id NUMBER, d DATE, local_time TIMESTAMP(3))");
            String sql="INSERT INTO teaql_temporal_java_fixture VALUES (?, ?, ?) /* ignored ? */";
            Object[] v={1,LocalDate.of(2024,2,29),LocalDateTime.of(2026,8,19,3,30,0,123_000_000)};
            try(PreparedStatement s=c.prepareStatement(sql)){s.setObject(1,v[0]);s.setObject(2,v[1]);s.setObject(3,v[2]);s.executeUpdate();}
            run(c,SqlDataServiceExecutor.debugSql(sql,v,"oracle").replaceFirst("VALUES \\(1,","VALUES (2,")); assertRows(c); drop(c);
        }
    }
    private static void run(Connection c,String q)throws SQLException{try(Statement s=c.createStatement()){s.execute(q);}}
    private static void drop(Connection c){try{run(c,"DROP TABLE teaql_temporal_java_fixture");}catch(SQLException ignored){}}
    private static void assertRows(Connection c)throws SQLException{try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT d, local_time FROM teaql_temporal_java_fixture ORDER BY id")){assertTrue(r.next());Object d=r.getObject(1),t=r.getObject(2);assertTrue(r.next());assertEquals(d,r.getObject(1));assertEquals(t,r.getObject(2));assertFalse(r.next());}}
}
