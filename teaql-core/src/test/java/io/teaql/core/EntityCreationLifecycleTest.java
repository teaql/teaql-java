package io.teaql.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import org.junit.Test;
import static org.junit.Assert.*;

/** The public creation boundary, not a save-time allocation test. */
public class EntityCreationLifecycleTest {
    private static final class Fixture {
        final AtomicLong sequence = new AtomicLong(100);
        final AtomicInteger allocations = new AtomicInteger();
        InternalIdGenerationService generator = (context, entity) -> {
            allocations.incrementAndGet();
            return sequence.incrementAndGet();
        };
        UnaryOperator<BaseEntity> initializer;
        final UserContext context = (UserContext) Proxy.newProxyInstance(
                UserContext.class.getClassLoader(), new Class<?>[] {UserContext.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("capability")) {
                        assertSame(InternalIdGenerationService.class, args[0]);
                        return generator;
                    }
                    if (method.getName().equals("initializeEntity") && initializer != null)
                        return initializer.apply((BaseEntity) args[1]);
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                    throw new AssertionError("Creation must not call a provider, save, clock or query: " + method.getName());
                });
    }

    private static BaseEntity create(Fixture f) {
        return (BaseEntity) new BaseRequest.TempRequest(
                BaseEntity.class, "BaseEntity", BaseEntity::new)
                .internalComment("Create a native lifecycle control")
                .purpose("Verify identity before the creation call returns")
                .newEntity(f.context);
    }

    private static void assignedNew(BaseEntity entity, long id) {
        assertEquals(Long.valueOf(id), entity.getId());
        assertTrue(entity.isPropertyLoaded("id"));
        assertTrue(entity.newItem());
        assertNull(entity.getVersion());
        assertTrue(entity.getEntityMutationLedger().isNew(new EntityKey(entity.typeName(), id)));
    }

    @Test public void publicFactoryReturnsAnAssignedNewEntity() {
        var f = new Fixture(); var entity = create(f);
        assignedNew(entity, 101); assertEquals(1, f.allocations.get());
        assertTrue(entity.getUpdatedProperties().isEmpty());
        assertNull(entity.getOriginalVersion());
    }

    @Test public void directContextInitializationAlsoAssignsAnIdentity() {
        var f = new Fixture(); var entity = new BaseEntity();
        assertSame(entity, f.context.initializeEntity("BaseEntity", entity));
        assignedNew(entity, 101); assertEquals(1, f.allocations.get());
    }

    @Test public void repeatingInitializationDoesNotReallocateOrReplaceTheLedger() {
        var f = new Fixture(); var entity = create(f); var ledger = entity.getEntityMutationLedger();
        assertSame(entity, f.context.initializeEntity("BaseEntity", entity));
        assignedNew(entity, 101); assertSame(ledger, entity.getEntityMutationLedger());
        assertEquals(1, f.allocations.get());
    }

    @Test public void independentCreationsInOneContextHaveIndependentLedgers() {
        var f = new Fixture(); var first = create(f); var abandoned = create(f); var last = create(f);
        assignedNew(first, 101); assignedNew(abandoned, 102); assignedNew(last, 103);
        assertNotSame(first.getEntityMutationLedger(), last.getEntityMutationLedger());
        assertNotSame(first.getEntityMutationLedger(), abandoned.getEntityMutationLedger());
        assertEquals(Set.of(new EntityKey("BaseEntity", 101L)), first.getEntityMutationLedger().newKeys());
        assertEquals(Set.of(new EntityKey("BaseEntity", 103L)), last.getEntityMutationLedger().newKeys());
        assertEquals(3, f.allocations.get());
    }

    @Test public void trustedContextDefaultsRunBeforeFallbackAllocation() {
        var f = new Fixture(); f.initializer = entity -> { entity.setComment("Trusted default is ready"); return entity; };
        f.generator = (context, entity) -> {
            f.allocations.incrementAndGet(); assertSame(f.context, context);
            assertEquals("Trusted default is ready", entity.getComment()); return 501L;
        };
        assignedNew(create(f), 501); assertEquals(1, f.allocations.get());
    }

    @Test public void customInitializerCannotAccidentallyReturnAnUnassignedEntity() {
        var f = new Fixture(); f.initializer = entity -> entity;
        assignedNew(create(f), 101); assertEquals(1, f.allocations.get());
    }

    @Test public void trustedPreassignedIdentityIsPreservedWithoutAllocation() {
        var f = new Fixture(); f.initializer = entity -> { entity.setProperty("id", 777L); return entity; };
        assignedNew(create(f), 777); assertEquals(0, f.allocations.get());
    }

    @Test public void preassignedDefaultContextIdentityIsMarkedAsNew() {
        var f = new Fixture(); var entity = new BaseEntity(); entity.setProperty("id", 888L);
        assertSame(entity, f.context.initializeEntity("BaseEntity", entity));
        assignedNew(entity, 888); assertEquals(0, f.allocations.get());
    }

    @Test public void missingGeneratorFailsAtCreationRatherThanReturningATemporaryIdentity() {
        var f = new Fixture(); f.generator = null;
        var error = assertThrows(TeaQLRuntimeException.class, () -> create(f));
        assertTrue(error.getMessage().contains("InternalIdGenerationService"));
        assertEquals(0, f.allocations.get());
    }

    private static void invalidAllocatedId(Long id) {
        var f = new Fixture(); var original = new AtomicReference<BaseEntity>();
        f.initializer = entity -> { original.set(entity); return entity; };
        f.generator = (context, entity) -> { f.allocations.incrementAndGet(); return id; };
        assertThrows(TeaQLRuntimeException.class, () -> create(f));
        assertNull(original.get().getId()); assertFalse(original.get().isPropertyLoaded("id"));
        assertFalse(original.get().__internalHasMutationLedger()); assertEquals(1, f.allocations.get());
    }

    @Test public void nullAllocatedIdIsRejected() { invalidAllocatedId(null); }
    @Test public void zeroAllocatedIdIsRejected() { invalidAllocatedId(0L); }
    @Test public void negativeAllocatedIdIsRejected() { invalidAllocatedId(-1L); }

    @Test public void allocationFailurePropagatesAndAnotherCreationCanStillSucceed() {
        var f = new Fixture(); var failure = new IllegalStateException("Controlled ID service failure");
        f.generator = (context, entity) -> { throw failure; };
        assertSame(failure, assertThrows(IllegalStateException.class, () -> create(f)));
        f.generator = (context, entity) -> 909L;
        assignedNew(create(f), 909);
    }

    @Test public void initializerCannotReturnNull() {
        var f = new Fixture(); f.initializer = entity -> null;
        assertThrows(IllegalArgumentException.class, () -> create(f)); assertEquals(0, f.allocations.get());
    }

    @Test public void initializerCannotReturnAPersistedObjectAsNew() {
        var f = new Fixture(); f.initializer = entity -> { entity.set$status(EntityStatus.PERSISTED); return entity; };
        assertThrows(TeaQLRuntimeException.class, () -> create(f)); assertEquals(0, f.allocations.get());
    }

    @Test public void initializerCannotSupplyANonpositiveIdentity() {
        var f = new Fixture(); f.initializer = entity -> { entity.setProperty("id", -3L); return entity; };
        assertThrows(TeaQLRuntimeException.class, () -> create(f)); assertEquals(0, f.allocations.get());
    }

    @Test public void invalidArgumentsRejectBeforeAllocation() {
        var f = new Fixture();
        assertThrows(IllegalArgumentException.class, () -> f.context.initializeEntity(" ", new BaseEntity()));
        assertThrows(IllegalArgumentException.class, () -> f.context.initializeEntity("BaseEntity", null));
        assertEquals(0, f.allocations.get());
    }

    @Test public void concurrentFactoriesReturnUniqueIdentitiesAndPrivateLedgers() throws Exception {
        var f = new Fixture(); var executor = Executors.newFixedThreadPool(4);
        try {
            var jobs = new ArrayList<Callable<BaseEntity>>(); for (int i = 0; i < 24; i++) jobs.add(() -> create(f));
            var ids = new HashSet<Long>(); var ledgers = java.util.Collections.newSetFromMap(
                    new java.util.IdentityHashMap<EntityMutationLedger, Boolean>());
            for (var result : executor.invokeAll(jobs)) {
                var entity = result.get(); assertNotNull(entity.getId()); assertTrue(entity.getId() > 0);
                assertTrue(ids.add(entity.getId())); assertTrue(ledgers.add(entity.getEntityMutationLedger()));
                assertEquals(Set.of(new EntityKey("BaseEntity", entity.getId())), entity.getEntityMutationLedger().newKeys());
            }
            assertEquals(24, f.allocations.get());
        } finally { executor.shutdownNow(); assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS)); }
    }
}
