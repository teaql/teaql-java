package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.checker.*;
import io.teaql.core.meta.*;
import io.teaql.core.businessid.BusinessClock;
import java.time.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Small Context fixture; real save entry, observed provider, no generated-source edits. */
public class ContextDerivedMutationTest {
    static final LocalDateTime A = LocalDateTime.of(2026, 11, 11, 10, 0);
    static final LocalDateTime B = A.plusHours(1);
    static final List<String> CONTEXT = List.of("born_at", "touched_at", "source_ip", "last_ip", "opened_by", "changed_by");

    public static final class Probe extends BaseEntity {
        final Map<String, Object> values = new HashMap<>();
        @Override public String typeName() { return "ContextProbe"; }
        @Override public Object __internalGet(String field) {
            return field.equals("id") || field.equals("version") ? super.__internalGet(field) : values.get(field);
        }
        @Override public void __internalSet(String field, Object value) {
            if (field.equals("id") || field.equals("version")) super.__internalSet(field, value); else values.put(field, value);
        }
    }

    static final class Provider implements MutationExecutor {
        int writes;
        final Map<Long, Map<String, Object>> rows = new HashMap<>();
        public MutationResult mutate(UserContext context, PersistenceMutation mutation) {
            writes++;
            var row = (Probe) ((EntityPersistenceMutation) mutation).getEntity();
            var stored = new HashMap<>(rows.getOrDefault(row.getId(), Map.of()));
            stored.putAll(row.values);
            long version = ((Number) stored.getOrDefault("version", 0L)).longValue() + 1;
            stored.put("id", row.getId()); stored.put("version", version); rows.put(row.getId(), stored);
            var saved = loaded(row.getId(), stored);
            return new DefaultMutationResult(saved);
        }
        public String name() { return "context-fixture"; }
        public DataServiceCapabilities capabilities() { return new DataServiceCapabilities(); }
    }

    static Probe loaded(long id, Map<String, Object> values) {
        var row = new Probe();
        row.__internalSet("id", id); row.__internalSet("version", values.getOrDefault("version", 1L));
        values.forEach(row::__internalSet);
        values.keySet().forEach(row::markPropertyLoaded);
        row.markPropertyLoaded("id"); row.markPropertyLoaded("version");
        row.set$status(EntityStatus.PERSISTED);
        return row;
    }

    static boolean contextFixAllowed(Checker<Probe> checker, UserContext context, Probe row) {
        // The same test also runs against the frozen pre-fix runtime. Its
        // missing predicate takes the old unconditional automatic-Fix path.
        try {
            return (boolean) Checker.class.getMethod("needContextFix", UserContext.class, BaseEntity.class)
                    .invoke(checker, context, row);
        } catch (NoSuchMethodException legacy) { return true; }
        catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }

    static final class Fixture {
        final Provider provider = new Provider();
        final DefaultUserContext context;
        int checks, fixes, audits;
        Fixture() {
            var descriptor = new EntityDescriptor(); descriptor.setType("ContextProbe");
            descriptor.setTargetType(Probe.class); descriptor.setEntitySupplier(Probe::new);
            descriptor.setDataService("context-fixture");
            var props = new ArrayList<PropertyDescriptor>();
            for (String name : List.of("id", "version", "label")) {
                var property = new PropertyDescriptor(); property.setName(name); props.add(property);
            }
            for (String name : CONTEXT) {
                var property = new PropertyDescriptor(); property.setName(name);
                property.with(name.equals("born_at") || name.equals("source_ip") || name.equals("opened_by")
                        ? "createFunction" : "updateFunction", "contextValue");
                props.add(property);
            }
            var children = new Relation(); children.setName("children"); children.setOwner(descriptor); props.add(children);
            descriptor.setProperties(props);
            var metadata = new SimpleEntityMetaFactory(); metadata.register(descriptor);
            var clock = new BusinessClock() {
                public LocalDate businessDate(UserContext caller) { return businessDateTime(caller).toLocalDate(); }
                public LocalDateTime businessDateTime(UserContext caller) { return (LocalDateTime) caller.getAttribute("fixture_now"); }
            };
            var runtime = TeaQLRuntime.builder().metadata(metadata).dataService("context-fixture", provider)
                    .businessClock(clock).executionLogging(false).build();
            runtime.install(RuntimeModule.of().withCheckers(new Checker<Probe>() {
                public String type() { return "ContextProbe"; }
                // Validation must remain callable even for a clean object.
                public boolean needCheck(UserContext caller, Probe row) { return row.get$status() != EntityStatus.REFER; }
                public void checkAndFix(UserContext caller, Probe row, ObjectLocation location) {
                    checks++;
                    if (contextFixAllowed(this, caller, row)) {
                        fixes++;
                        if (row.isNew()) {
                            row.updateProperty("born_at", caller.businessTime());
                            row.updateProperty("source_ip", caller.getAttribute("fixture_ip"));
                            row.updateProperty("opened_by", caller.getAttribute("fixture_actor"));
                        }
                        row.updateProperty("touched_at", caller.businessTime());
                        row.updateProperty("last_ip", caller.getAttribute("fixture_ip"));
                        row.updateProperty("changed_by", caller.getAttribute("fixture_actor"));
                    }
                    requiredCheck(caller, newLocation(location, "label"), row.values.get("label"));
                    if ("NORMALIZE".equals(row.values.get("label"))) row.updateProperty("label", "original");
                    if ("INVALID".equals(row.values.get("label")))
                        invalidTypeCheck(caller, newLocation(location, "label"), "fixture rejection");
                }
            }));
            context = new DefaultUserContext(runtime);
            context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, event) -> audits++);
            use(A, "192.0.2.10", "alice");
        }
        void use(LocalDateTime now, String ip, String actor) {
            context.putAttribute("fixture_now", now); context.putAttribute("fixture_ip", ip); context.putAttribute("fixture_actor", actor);
        }
        Probe original(long id) {
            var data = new HashMap<String, Object>(); data.put("label", "original"); data.put("version", 1L);
            data.put("born_at", A); data.put("touched_at", A); data.put("source_ip", "192.0.2.10");
            data.put("last_ip", "192.0.2.10"); data.put("opened_by", "alice"); data.put("changed_by", "alice");
            provider.rows.put(id, new HashMap<>(data));
            return loaded(id, data);
        }
        void save(Probe row) { row.auditAs("verify Context-derived save semantics").save(context); }
    }

    @Test public void differentContextAloneCannotCreateAnUpdateButCheckStillRuns() {
        var f = new Fixture(); var row = f.original(1); var before = new HashMap<>(row.values);
        f.use(B, "192.0.2.20", "bob"); f.save(row); f.save(row);
        assertEquals(before, row.values); assertEquals(Long.valueOf(1), row.getVersion());
        assertEquals(0, f.provider.writes); assertEquals(0, f.audits); assertEquals(0, f.fixes);
        assertEquals(2, f.checks);
    }

    @Test public void realBusinessChangeUsesUpdateContextAndPreservesCreationContext() {
        var f = new Fixture(); var row = f.original(1); f.use(B, "192.0.2.20", "bob");
        row.updateProperty("label", "changed"); f.save(row);
        assertEquals(A, row.values.get("born_at")); assertEquals("192.0.2.10", row.values.get("source_ip"));
        assertEquals("alice", row.values.get("opened_by")); assertEquals(B, row.values.get("touched_at"));
        assertEquals("192.0.2.20", row.values.get("last_ip")); assertEquals("bob", row.values.get("changed_by"));
        assertEquals(1, f.provider.writes); assertEquals(1, f.audits);
    }

    @Test public void repairedBusinessEditCannotLeaveAnAutomaticContextOnlyUpdate() {
        var f = new Fixture(); var row = f.original(1); f.use(B, "192.0.2.20", "bob");
        row.updateProperty("label", "NORMALIZE"); f.save(row);
        assertEquals("original", row.values.get("label"));
        assertEquals(0, f.provider.writes); assertEquals(0, f.audits);
        assertEquals(Long.valueOf(1), row.getVersion());
        assertEquals(A, row.values.get("touched_at")); assertEquals("alice", row.values.get("changed_by"));
        assertTrue(row.getUpdatedProperties().isEmpty());
    }

    @Test public void creationIsEligibleWithoutCallerDirtyFields() {
        var f = new Fixture(); var row = new Probe(); row.__internalInitializeNewEntityId(7L);
        row.__internalSet("label", "default input"); assertTrue(row.getUpdatedProperties().isEmpty());
        f.save(row); assertEquals(A, row.values.get("born_at")); assertEquals("192.0.2.10", row.values.get("source_ip"));
        assertEquals("alice", row.values.get("changed_by")); assertEquals(1, f.provider.writes);
    }

    @Test public void changedChildDoesNotRefreshParentOrSiblingContext() {
        var f = new Fixture(); var parent = f.original(1); var child = f.original(2); var sibling = f.original(3);
        parent.__internalSet("children", List.of(child, sibling));
        f.use(B, "192.0.2.20", "bob"); child.updateProperty("label", "changed child"); f.save(parent);
        assertEquals(A, parent.values.get("touched_at")); assertEquals("alice", sibling.values.get("changed_by"));
        assertEquals(B, child.values.get("touched_at")); assertEquals(1, f.provider.writes);
    }

    @Test public void failedCheckRemovesAutomaticContextChangesButKeepsCallerIntent() {
        var f = new Fixture(); var row = f.original(1); f.use(B, "192.0.2.20", "bob");
        row.updateProperty("label", "INVALID");
        var error = assertThrows(CheckException.class, () -> f.save(row));
        assertEquals("ContextProbe", error.getViolates().get(0).getRootType());
        assertEquals("label", error.getViolates().get(0).getLocation().modelPath());
        assertEquals("/label", error.getViolates().get(0).getLocation().instancePath());
        assertEquals(A, row.values.get("touched_at")); assertEquals("192.0.2.10", row.values.get("last_ip"));
        assertEquals("alice", row.values.get("changed_by")); assertEquals("INVALID", row.values.get("label"));
        assertEquals(Set.of("label"), new HashSet<>(row.getUpdatedProperties()));
        assertEquals(0, f.provider.writes); assertEquals(0, f.audits);
        row.updateProperty("label", "original"); f.save(row);
        assertEquals(0, f.provider.writes); assertEquals(A, row.values.get("touched_at"));
        row.updateProperty("label", "retry"); f.save(row);
        assertEquals(1, f.provider.writes); assertEquals("bob", row.values.get("changed_by"));
    }

    @Test public void invalidChildReportsItsExactObjectFieldAndIndex() {
        var f = new Fixture(); var parent = f.original(1); var sibling = f.original(2); var child = f.original(3);
        parent.__internalSet("children", List.of(sibling, child));
        f.use(B, "192.0.2.20", "bob"); child.updateProperty("label", "INVALID");
        var error = assertThrows(CheckException.class, () -> f.save(parent));
        var violation = error.getViolates().get(0);
        assertEquals("ContextProbe", violation.getRootType());
        assertEquals("children[1].label", violation.getLocation().modelPath());
        assertEquals("/children/1/label", violation.getLocation().instancePath());
        assertEquals(0, f.provider.writes); assertEquals(0, f.audits);
        assertEquals(A, child.values.get("touched_at"));
        assertEquals("INVALID", child.values.get("label"));
    }
}
