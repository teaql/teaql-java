package io.teaql.core.sql.portable;

import static org.junit.Assert.assertEquals;

import io.teaql.core.sql.dialect.OracleDialect;
import io.teaql.core.sql.dialect.PostgreSqlDialect;
import org.junit.Test;

public class DialectPaginationTest {
    @Test
    public void postgresUsesLimitOffsetParameters() {
        assertEquals(
                "LIMIT :limit0 OFFSET :offset0",
                new PostgreSqlDialect().prepareParameterizedLimit(":limit0", ":offset0"));
    }

    @Test
    public void oracleUsesOffsetFetchParameters() {
        assertEquals(
                "OFFSET :offset0 ROWS FETCH NEXT :limit0 ROWS ONLY",
                new OracleDialect().prepareParameterizedLimit(":limit0", ":offset0"));
    }
}
