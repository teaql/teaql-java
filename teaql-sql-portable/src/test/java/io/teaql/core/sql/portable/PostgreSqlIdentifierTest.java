package io.teaql.core.sql.portable;

import io.teaql.core.sql.dialect.PostgreSqlDialect;
import org.junit.Test;
import static org.junit.Assert.*;

public class PostgreSqlIdentifierTest {
    @Test public void quotesEmbeddedDelimitersWithoutChangingThePhysicalName() {
        assertEquals("\"Legacy \"\"Label\"\"\\path\"", new PostgreSqlDialect().escapeIdentifier("Legacy \"Label\"\\path"));
    }
    @Test public void preservesOrdinaryNamesAndExistingQuotedMetadata() {
        var dialect = new PostgreSqlDialect();
        assertEquals("school_data", dialect.escapeIdentifier("school_data"));
        assertEquals("\"order\"", dialect.escapeIdentifier("order"));
        assertEquals("\"Legacy Key\"", dialect.escapeIdentifier("\"Legacy Key\""));
    }
}
