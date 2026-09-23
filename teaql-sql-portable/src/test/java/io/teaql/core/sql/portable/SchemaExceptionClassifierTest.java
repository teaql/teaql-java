package io.teaql.core.sql.portable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import org.junit.Test;

public class SchemaExceptionClassifierTest {

    @Test
    public void recognizesSupportedDuplicateIndexSignalsThroughWrappers() {
        assertTrue(SchemaExceptionClassifier.isDuplicateIndex(
                new RuntimeException(new SQLException("relation already exists", "42P07", 0))));
        assertTrue(SchemaExceptionClassifier.isDuplicateIndex(
                new SQLException("Duplicate key name", "42000", 1061)));
        assertTrue(SchemaExceptionClassifier.isDuplicateIndex(
                new SQLException("[SQLITE_ERROR] index idx_order already exists", null, 1)));
    }

    @Test
    public void doesNotHidePermissionConnectivityOrSyntaxFailures() {
        assertFalse(SchemaExceptionClassifier.isDuplicateIndex(
                new SQLException("permission denied", "42501", 0)));
        assertFalse(SchemaExceptionClassifier.isDuplicateIndex(
                new SQLException("connection failure", "08006", 0)));
        assertFalse(SchemaExceptionClassifier.isDuplicateIndex(
                new SQLException("syntax error", "42601", 0)));
        assertFalse(SchemaExceptionClassifier.isDuplicateIndex(
                new SQLException("table already exists", null, 1)));
        assertFalse(SchemaExceptionClassifier.isDuplicateIndex(
                new SQLException("name is already used by an existing object", "42000", 955)));
    }
}
