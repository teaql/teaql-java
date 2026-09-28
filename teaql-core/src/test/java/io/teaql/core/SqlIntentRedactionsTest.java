package io.teaql.core;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class SqlIntentRedactionsTest {
    @Test public void snapshotDoesNotAccumulateDescendantOrSiblingValues() {
        var root = new SqlIntentRedactions();
        root.capture(List.of(SqlParameterLogPolicy.MASKED), new Object[]{"Riverside"});
        var child = root.copy();
        child.capture(List.of(SqlParameterLogPolicy.CREDENTIAL), new Object[]{"CHILD-PASSWORD"});
        var sibling = root.copy();
        var rootValues = new java.util.ArrayList<Object>();
        var childValues = new java.util.ArrayList<Object>();
        var siblingValues = new java.util.ArrayList<Object>();
        root.appendTo(rootValues, false); child.appendTo(childValues, true); sibling.appendTo(siblingValues, false);
        assertEquals(List.of("Riverside"), rootValues);
        assertEquals(List.of("CHILD-PASSWORD"), childValues);
        assertEquals(rootValues, siblingValues);
    }
    @Test public void provenanceIsNotSerialized() throws Exception {
        var source = new SqlIntentRedactions();
        source.capture(List.of(SqlParameterLogPolicy.MASKED), new Object[]{"Riverside"});
        var metadata = new ExecutionMetadata();
        metadata.setIntentRedactions(source);
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(metadata);
        assertFalse(json.contains("intentRedactions"));
        assertFalse(json.contains("Riverside"));
        assertFalse(source.toString().contains("Riverside"));
    }
}
