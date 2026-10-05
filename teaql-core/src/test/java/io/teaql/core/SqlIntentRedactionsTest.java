package io.teaql.core;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class SqlIntentRedactionsTest {
    @Test public void repeatedTypedAndPhysicalCaptureIsIdempotentWithoutLosingForcedSecrets() {
        var source = new SqlIntentRedactions();
        for (int i = 0; i < 3; i++) source.capture(List.of(SqlParameterLogPolicy.MASKED), new Object[]{"SECRET"});
        var safe = new java.util.ArrayList<Object>(); source.appendTo(safe, false);
        assertEquals(List.of("SECRET"), safe);
        var child = source.copy(); child.include(source);
        child.capture(List.of(SqlParameterLogPolicy.CREDENTIAL), new Object[]{"SECRET"});
        child.capture(List.of(SqlParameterLogPolicy.MASKED), new Object[]{"NEXT"});
        var debug = new java.util.ArrayList<Object>(); child.appendTo(debug, true);
        assertEquals("forced classification must survive masked duplicates", List.of("SECRET"), debug);
        safe.clear(); source.appendTo(safe, false);
        assertEquals("descendant capture cannot mutate parent provenance", List.of("SECRET"), safe);
    }
    @Test public void inheritedPolicyAndCredentialsMatchBindingPolicy() {
        var parent = new io.teaql.core.meta.EntityDescriptor();
        var field = new io.teaql.core.meta.PropertyDescriptor();
        field.setName("name"); field.setOwner(parent);
        parent.setProperties(List.of(field));
        var child = new io.teaql.core.meta.EntityDescriptor(); child.setParent(parent);
        assertEquals(SqlParameterLogPolicy.UNKNOWN, SqlFieldLogPolicy.resolve(child, "name"));
        parent.setAuditMaskFields(List.of());
        assertEquals(SqlParameterLogPolicy.PLAIN, SqlFieldLogPolicy.resolve(child, "name"));
        child.setAuditMaskFields(List.of("name"));
        assertEquals(SqlParameterLogPolicy.MASKED, SqlFieldLogPolicy.resolve(child, "name"));
        field.with("logPolicy", "plain");
        assertEquals(SqlParameterLogPolicy.MASKED, SqlFieldLogPolicy.resolve(child, field));
        assertEquals(SqlParameterLogPolicy.CREDENTIAL, SqlFieldLogPolicy.resolve(child, "access_token"));
        assertEquals(SqlParameterLogPolicy.UNKNOWN, SqlFieldLogPolicy.resolve(child, "missing"));
    }

    @Test public void capturedEntityOldAndNewValuesSurviveLaterMutationWithoutSharingState() {
        var descriptor = new io.teaql.core.meta.EntityDescriptor();
        var field = new io.teaql.core.meta.PropertyDescriptor();
        field.setName("name"); field.setOwner(descriptor);
        descriptor.setProperties(List.of(field)); descriptor.setAuditMaskFields(List.of("name"));
        var entity = new BaseEntityTest.TestEntity();
        entity.setProperty("name", "PRIVATE-OLD"); entity.updateName("PRIVATE-NEW");
        var captured = new SqlIntentRedactions(); captured.captureEntity(entity, descriptor);
        var merged = new SqlIntentRedactions(); merged.include(captured);
        entity.updateName("PRIVATE-LATER"); entity.clearUpdatedProperties();
        captured.capture(List.of(SqlParameterLogPolicy.MASKED), new Object[]{"SOURCE-LATER"});
        var values = new java.util.ArrayList<Object>(); merged.appendTo(values, false);
        assertEquals(List.of("PRIVATE-NEW", "PRIVATE-OLD"), values);
        var debugValues = new java.util.ArrayList<Object>(); merged.appendTo(debugValues, true);
        assertTrue(debugValues.isEmpty());
    }

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
