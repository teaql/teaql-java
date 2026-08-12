package io.teaql.core.db2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.teaql.core.BaseEntity;
import io.teaql.core.BaseRequest;
import org.junit.Test;

public class DB2DialectTest {
    private static final class TestRequest extends BaseRequest<BaseEntity> {
        private TestRequest() { super(BaseEntity.class); }
        @Override public String getTypeName() { return "BaseEntity"; }
    }

    @Test
    public void usesDb2PaginationAndWindowSyntax() {
        DB2Dialect dialect = new DB2Dialect();
        assertEquals("OFFSET 5 ROWS FETCH NEXT 2 ROWS ONLY", dialect.prepareLimit(new TestRequest().offset(5, 2)));
        assertEquals("OFFSET :offset0 ROWS FETCH NEXT :limit0 ROWS ONLY",
                dialect.prepareParameterizedLimit(":limit0", ":offset0"));
        assertTrue(dialect.getPartitionSQL().contains("row_number() over(partition by"));
        assertTrue(dialect.getPartitionSQL().contains("as rank_"));
    }
}
