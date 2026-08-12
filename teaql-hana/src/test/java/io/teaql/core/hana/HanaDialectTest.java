package io.teaql.core.hana;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.teaql.core.BaseEntity;
import io.teaql.core.BaseRequest;
import org.junit.Test;

public class HanaDialectTest {
    private static final class TestRequest extends BaseRequest<BaseEntity> {
        private TestRequest() { super(BaseEntity.class); }
        @Override public String getTypeName() { return "BaseEntity"; }
    }

    @Test
    public void usesHanaPaginationWindowAndLargeText() {
        HanaDialect dialect = new HanaDialect();
        assertEquals("LIMIT 2 OFFSET 5", dialect.prepareLimit(new TestRequest().offset(5, 2)));
        assertTrue(dialect.getPartitionSQL().contains("row_number() over(partition by"));
        assertEquals("NCLOB", dialect.mapColumnType("LARGE_TEXT"));
    }
}
