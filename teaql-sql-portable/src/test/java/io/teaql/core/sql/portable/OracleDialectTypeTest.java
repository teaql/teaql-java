package io.teaql.core.sql.portable;

import static org.junit.Assert.assertEquals;

import io.teaql.core.sql.dialect.OracleDialect;
import org.junit.Test;

public class OracleDialectTypeTest {
    @Test
    public void mapsBusinessIdCounterToIntegralNumber() {
        OracleDialect dialect = new OracleDialect();
        assertEquals("NUMBER(19,0)", dialect.mapColumnType("BIGINT"));
        assertEquals("VARCHAR(512)", dialect.mapColumnType("VARCHAR(512)"));
    }
}
