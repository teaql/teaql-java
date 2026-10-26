package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.checker.CheckException;
import io.teaql.core.meta.EntityDescriptor;
import java.util.*;
import org.junit.Test;
import static io.teaql.runtime.CanonicalMutationFieldsTest.*;
import static org.junit.Assert.*;

public class CanonicalMutationFieldControlsTest {
    @Test public void canonicalNamesAreAcceptedWithoutRenamingThem() {
        var fixture = new Fixture(true, null); var entity = fixture.entity(false);
        entity.auditAs("calibrate native admission").save(fixture.context);
        assertEquals(1, fixture.checkerCalls); assertEquals(1, fixture.policyCalls); assertEquals(1, fixture.ids);
        assertEquals(1, fixture.provider.mutations); assertEquals(1, fixture.provider.begins); assertEquals(1, fixture.audits);
    }
    @Test public void explicitModelFieldMayLegitimatelyEndInId() {
        var fixture = new Fixture(true, null); fixture.descriptor.getProperties().add(property("type_platform_id"));
        var entity = fixture.entity(false); entity.updateProperty("type_platform_id", 4L);
        entity.auditAs("preserve explicitly modeled suffix").save(fixture.context); assertEquals(1, fixture.provider.mutations);
    }
    @Test public void emptyLegacyMetadataDoesNotInventAFieldContract() {
        var fixture = new Fixture(false, null); fixture.descriptor.setProperties(List.of());
        var entity = fixture.entity(false); entity.updateProperty("requiredString", "legacy");
        entity.auditAs("preserve manual empty metadata").save(fixture.context); assertEquals(1, fixture.provider.mutations);
    }
    @Test public void inheritedCanonicalPropertiesRemainValid() {
        var fixture = new Fixture(true, null); var parent = new EntityDescriptor(); parent.setType("BaseProbe");
        parent.setProperties(List.of(property("inherited_name"))); fixture.descriptor.setParent(parent);
        var entity = fixture.entity(false); entity.updateProperty("inherited_name", "inherited");
        entity.auditAs("use inherited model field").save(fixture.context); assertEquals(1, fixture.provider.mutations);
    }
    @Test public void invalidDescendantIsRejectedBeforeAnyGraphHook() {
        var fixture = new Fixture(true, null); var parent = fixture.entity(false); var child = fixture.entity(false);
        child.updateProperty("requiredString", "PRIVATE_BAD_CHILD"); parent.updateProperty("children", List.of(child));
        var error = assertThrows(CheckException.class, () -> parent.auditAs("reject bad descendant").save(fixture.context));
        assertEquals("children[0].requiredString", error.getViolates().get(0).getLocation().modelPath());
        assertEquals(0, fixture.checkerCalls); assertEquals(0, fixture.provider.mutations); assertEquals(0, fixture.ids);
    }
    @Test public void failureDoesNotPoisonIndependentSaveOnTheSameContext() {
        var fixture = new Fixture(true, null); var bad = fixture.entity(false); bad.updateProperty("typePlatform", 1L);
        assertThrows(CheckException.class, () -> bad.auditAs("reject bad first graph").save(fixture.context));
        var good = fixture.entity(false); good.auditAs("save independent valid graph").save(fixture.context);
        assertEquals(1, fixture.provider.mutations); assertEquals(1, fixture.policyCalls); assertEquals(1, fixture.audits);
    }
    @Test public void detachedLedgerChangesCannotBypassEarlyAdmission() {
        var fixture = new Fixture(true, null); var entity = fixture.entity(false);
        entity.getEntityMutationLedger().set(new EntityKey("NameProbe", 900L), "typePlatform", 1L);
        assertThrows(CheckException.class, () -> entity.auditAs("reject detached alias").save(fixture.context));
        assertEquals(0, fixture.ids); assertEquals(0, fixture.checkerCalls); assertEquals(0, fixture.policyCalls);
        assertEquals(0, fixture.provider.begins); assertEquals(0, fixture.provider.mutations);
    }
    @Test public void planAdmissionRejectsAliasesButAllowsOnlyTypedDynamicIntents() {
        var fixture = new Fixture(false, null);
        for (var name : List.of("storage", "native", "scalar", "unknown", "equal", "unequal")) {
            var plan = new MutationPlan("canonical-plan", "NameProbe.saveGraph", "NameProbe", "inspect canonical plan",
                    List.of(new MutationOperation(MutationOperationKind.UPDATE, new EntityKey("NameProbe", 1L), 1L, invalid(name))));
            assertThrows(CheckException.class, () -> CanonicalMutationFields.validatePlan(fixture.context, fixture.metadata, plan));
        }
        var dynamic = io.teaql.data.dynamic.DynamicFieldMutation.set("rating", io.teaql.data.dynamic.DynamicDataType.NUMBER, 3);
        var plan = new MutationPlan("typed-dynamic", "NameProbe.saveGraph", "NameProbe", "inspect typed extension",
                List.of(new MutationOperation(MutationOperationKind.UPDATE, new EntityKey("NameProbe", 1L), 1L, Map.of("#rating", dynamic))));
        CanonicalMutationFields.validatePlan(fixture.context, fixture.metadata, plan);
        var untyped = new MutationPlan("untyped-dynamic", "NameProbe.saveGraph", "NameProbe", "reject untyped extension",
                List.of(new MutationOperation(MutationOperationKind.UPDATE, new EntityKey("NameProbe", 1L), 1L, Map.of("#rating", 3))));
        assertThrows(CheckException.class, () -> CanonicalMutationFields.validatePlan(fixture.context, fixture.metadata, untyped));
    }
    @Test public void fixedLedgerDoesNotAcquireTheTypedDynamicChannelByPrefix() {
        var fixture = new Fixture(true, null); var entity = fixture.entity(true);
        var dynamic = io.teaql.data.dynamic.DynamicFieldMutation.set("rating", io.teaql.data.dynamic.DynamicDataType.NUMBER, 3);
        entity.getEntityMutationLedger().set(new EntityKey("NameProbe", 1L), "#rating", dynamic);
        assertThrows(CheckException.class, () -> entity.auditAs("reject wrong ledger channel").save(fixture.context));
        assertEquals(0, fixture.provider.mutations); assertEquals(0, fixture.checkerCalls);
    }
}
