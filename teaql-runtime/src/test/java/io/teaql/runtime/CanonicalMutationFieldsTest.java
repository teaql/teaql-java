package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.checker.*;
import io.teaql.core.meta.*;
import java.util.*;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/** Runtime admission tests; real generated SQLite acceptance is a separate gate. */
@RunWith(Parameterized.class)
public class CanonicalMutationFieldsTest {
    @Parameterized.Parameters(name = "{0}, update={1}")
    public static Collection<Object[]> cases() {
        var cases = new ArrayList<Object[]>();
        for (String name : List.of("storage", "native", "scalar", "unknown", "equal", "unequal"))
            for (boolean update : new boolean[] {false, true}) cases.add(new Object[] {name, update});
        return cases;
    }
    private final String name;
    private final boolean update;
    public CanonicalMutationFieldsTest(String name, boolean update) { this.name = name; this.update = update; }
    static Map<String, Object> invalid(String name) {
        var fields = new LinkedHashMap<String, Object>();
        switch (name) {
            case "storage" -> fields.put("type_platform_id", 1L);
            case "native" -> fields.put("typePlatform", 1L);
            case "scalar" -> fields.put("requiredString", "PRIVATE_BAD_ALIAS");
            case "unknown" -> fields.put("unknown_field", "PRIVATE_BAD_UNKNOWN");
            default -> { fields.put("type_platform", 1L); fields.put("type_platform_id", name.equals("equal") ? 1L : 2L); }
        }
        return fields;
    }
    @Test public void rejectsPendingFieldsBeforeCheckerPolicyAndProvider() {
        var fixture = new Fixture(true, null); var entity = fixture.entity(update);
        invalid(name).forEach(entity::updateProperty);
        rejected(fixture, entity, 0);
    }
    @Test public void rejectsRawLedgerKeysEvenWithoutLocalPropertyChanges() {
        var fixture = new Fixture(true, null); var entity = fixture.entity(update);
        if (!update) entity.__internalInitializeNewEntityId(1L);
        invalid(name).forEach((field, value) -> entity.getEntityMutationLedger().set(new EntityKey("NameProbe", 1L), field, value));
        rejected(fixture, entity, 0);
    }
    @Test public void rejectsBadFixOutputBeforePolicyIdAllocationOrProvider() {
        var fixture = new Fixture(true, entity -> invalid(name).forEach(entity::updateProperty));
        rejected(fixture, fixture.entity(update), 1);
    }
    @Test public void admissionDoesNotDependOnCustomCheckerPresence() {
        var fixture = new Fixture(false, null); var entity = fixture.entity(update);
        invalid(name).forEach(entity::updateProperty);
        rejected(fixture, entity, 0);
    }
    static void rejected(Fixture fixture, Probe entity, int checkerCalls) {
        var error = assertThrows(CheckException.class, () -> entity.auditAs("reject malformed model fields").save(fixture.context));
        assertFalse(error.getViolates().isEmpty());
        for (var violation : error.getViolates()) {
            assertEquals("UNKNOWN_FIELD", violation.getRuleId().name()); assertEquals("NameProbe", violation.getRootType());
            assertNull(violation.getInputValue()); assertNotNull(violation.getLocation());
        }
        assertFalse(error.getMessage().contains("PRIVATE_BAD"));
        assertEquals(checkerCalls, fixture.checkerCalls); assertEquals(0, fixture.policyCalls);
        assertEquals(0, fixture.ids); assertEquals(0, fixture.provider.mutations); assertEquals(0, fixture.provider.begins); assertEquals(0, fixture.audits);
    }
    static final class Probe extends BaseEntity {
        final Map<String, Object> values = new HashMap<>();
        @Override public String typeName() { return "NameProbe"; }
        @Override public Object __internalGet(String field) { return field.equals("id") || field.equals("version") ? super.__internalGet(field) : values.get(field); }
        @Override public void __internalSet(String field, Object value) {
            if (field.equals("id") || field.equals("version")) super.__internalSet(field, value); else values.put(field, value);
        }
    }
    static PropertyDescriptor property(String name) { var property = new PropertyDescriptor(); property.setName(name); return property; }
    static final class Fixture {
        final SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        final EntityDescriptor descriptor = new EntityDescriptor();
        final Provider provider = new Provider();
        final DefaultUserContext context;
        int checkerCalls, policyCalls, ids, audits;
        Fixture(boolean installChecker, Consumer<Probe> fix) {
            descriptor.setType("NameProbe"); descriptor.setDataService("fixture"); descriptor.setTargetType(Probe.class); descriptor.setEntitySupplier(Probe::new);
            var children = new Relation(); children.setName("children"); children.setOwner(descriptor);
            descriptor.setProperties(new ArrayList<>(List.of(property("id"), property("version"), property("required_string"), property("type_platform"), children)));
            metadata.register(descriptor);
            var policy = new MutationPolicy() {
                public MutationPolicyIdentity identity() { return new MutationPolicyIdentity("canonical-fields", "1", "sha256:canonical-fields"); }
                public MutationDecision review(UserContext caller, MutationPlan plan) { policyCalls++; return MutationDecision.allow(); }
            };
            var runtime = TeaQLRuntime.builder().metadata(metadata).dataService("fixture", provider).executionLogging(false)
                    .idGenerationService((caller, entity) -> { ids++; return 100L + ids; })
                    .mutationPolicyRegistry(key -> Optional.of(policy)).build();
            if (installChecker) runtime.install(RuntimeModule.of().withCheckers(new Checker<Probe>() {
                public String type() { return "NameProbe"; }
                public void checkAndFix(UserContext caller, Probe entity, ObjectLocation location) {
                    checkerCalls++; markAsChecked(caller, entity); if (fix != null) fix.accept(entity);
                }
            }));
            context = new DefaultUserContext(runtime);
            context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, event) -> audits++);
        }
        Probe entity(boolean update) {
            var entity = new Probe();
            if (update) { entity.__internalSet("id", 1L); entity.__internalSet("version", 1L); entity.set$status(EntityStatus.PERSISTED); }
            entity.updateProperty("required_string", "valid"); entity.updateProperty("type_platform", 1L); return entity;
        }
    }
    static final class Provider implements MutationExecutor, TransactionExecutor {
        int mutations, begins;
        public MutationResult mutate(UserContext context, PersistenceMutation request) { mutations++; return new DefaultMutationResult(((EntityPersistenceMutation) request).getEntity()); }
        public <T> T executeInTransaction(UserContext context, TransactionCallback<T> callback) { begins++; return callback.doInTransaction(); }
        public String name() { return "fixture"; }
        public DataServiceCapabilities capabilities() { return new DataServiceCapabilities(); }
    }
}
