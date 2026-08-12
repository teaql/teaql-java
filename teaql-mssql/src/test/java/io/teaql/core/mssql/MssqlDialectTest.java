package io.teaql.core.mssql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.teaql.core.BaseRequest;
import io.teaql.core.BaseEntity;
import org.junit.Test;

public class MssqlDialectTest {
    private static final class TestRequest extends BaseRequest<BaseEntity> {
        private TestRequest() {
            super(BaseEntity.class);
        }

        @Override
        public String getTypeName() {
            return "BaseEntity";
        }
    }

    @Test
    public void usesOffsetFetchAndWindowPartitionSyntax() {
        BaseRequest<?> request = new TestRequest().offset(5, 2);
        MssqlDialect dialect = new MssqlDialect();
        assertEquals("OFFSET 5 ROWS FETCH NEXT 2 ROWS ONLY", dialect.prepareLimit(request));
        assertEquals(
                "OFFSET :offset0 ROWS FETCH NEXT :limit0 ROWS ONLY",
                dialect.prepareParameterizedLimit(":limit0", ":offset0"));
        assertEquals(
                "ORDER BY (SELECT NULL) OFFSET :offset0 ROWS FETCH NEXT :limit0 ROWS ONLY",
                dialect.prepareParameterizedLimit(":limit0", ":offset0", false));
        assertEquals(
                "OFFSET :offset0 ROWS FETCH NEXT :limit0 ROWS ONLY",
                dialect.prepareParameterizedLimit(":limit0", ":offset0", true));
        assertTrue(dialect.getPartitionSQL().contains("row_number() over(partition by"));
    }
}
