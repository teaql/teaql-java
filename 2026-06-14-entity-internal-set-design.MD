# Entity Property Encapsulation: internalSet Design (RFC)

Date: 2026-06-14

## 1. Problem

Generated entity classes currently expose raw `setXxx()` methods as `public`:

```java
public class Task extends BaseEntity {
    @Deprecated
    public void setName(String name) { this.name = name; }

    @Deprecated
    public void setStatus(TaskStatus status) { this.status = status; }
}
```

Although these setters are marked `@Deprecated`, they remain fully visible and callable from
application-layer (APP) code. In IDE autocomplete, they appear alongside the legitimate `updateXxx()`
methods, creating a constant temptation for developers to bypass change tracking and directly
mutate entity state.

This violates TeaQL's core principle: **business code must interact with entities through
semantically meaningful, change-tracked `updateXxx()` methods, never through raw setters.**

Meanwhile, framework infrastructure — database hydrators, JSON deserializers, and internal
assemblers — legitimately needs to assign raw values to entity fields during object construction
from external data sources. These infrastructure paths must continue to work.

## 2. Design Goal

| Caller | `setXxx()` | `updateXxx()` | `internalSet()` / `internalGet()` |
|--------|-----------|---------------|-----------------------------------|
| APP business code | ❌ Must not exist | ✅ The only way to mutate | ❌ Should not use |
| Database Hydrator | N/A | N/A | ✅ `internalSet` for assembly |
| JSON Deserializer | N/A | N/A | ✅ `internalSet` for deserialization |
| JSON Serializer / Diff / Audit | N/A | N/A | ✅ `internalGet` for generic read |
| Entity's own `updateXxx` | N/A | N/A | ❌ Direct field assignment |

## 3. Core Design: Generated `internalSet` / `internalGet` with Switch Dispatch

### 3.1 BaseEntity (Framework Core)

`BaseEntity` declares `internalSet` as a public method. Generated subclasses override it
with a property-specific switch statement.

```java
public class BaseEntity implements Entity {
    private Long id;
    private Long version;

    public Long getId() { return id; }
    public Long getVersion() { return version; }

    // No setId(), no setVersion() — use updateId(), updateVersion()

    public BaseEntity updateId(Long id) {
        if (ObjectUtil.equal(this.id, id)) return this;
        handleUpdate(ID_PROPERTY, getId(), id);
        this.id = id;
        return this;
    }

    public BaseEntity updateVersion(Long version) {
        if (ObjectUtil.equal(this.version, version)) return this;
        handleUpdate(VERSION_PROPERTY, getVersion(), version);
        this.version = version;
        return this;
    }

    /**
     * Framework-internal property assignment channel.
     *
     * Used by database hydrators and JSON deserializers to populate entity fields
     * during object construction. This method performs raw assignment WITHOUT
     * change tracking, dirty marking, or equality checks.
     *
     * Business code MUST use updateXxx() methods instead.
     */
    @FrameworkInternal("Business code must use updateXxx() methods")
    public void internalSet(String property, Object value) {
        switch (property) {
            case "id":      this.id = (Long) value; break;
            case "version": this.version = (Long) value; break;
            default:
                throw new IllegalArgumentException(
                    typeName() + " has no property: " + property);
        }
    }

    /**
     * Framework-internal generic property read channel.
     *
     * Used by serializers, diff engines, and audit trail builders for
     * reflection-free, uniform property access on BaseEntity references.
     */
    @FrameworkInternal("Business code should use typed getXxx() methods")
    public Object internalGet(String property) {
        switch (property) {
            case "id":      return this.id;
            case "version": return this.version;
            default:
                throw new IllegalArgumentException(
                    typeName() + " has no property: " + property);
        }
    }
}
```

### 3.2 Generated Entity (e.g. Task)

Each generated entity overrides both `internalSet` and `internalGet` with its own switch branches.
All `setXxx()` methods are removed. `updateXxx()` methods assign fields directly.

```java
public class Task extends BaseEntity {
    private String name;
    private TaskStatus status;
    private Platform platform;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private SmartList<TaskLog> taskLogList;

    // ===== Public API: Getters =====
    public String getName() { return name; }
    public TaskStatus getStatus() { return status; }
    public Platform getPlatform() { return platform; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public SmartList<TaskLog> getTaskLogList() { return taskLogList; }

    // ===== Public API: Update (with change tracking) =====
    public Task updateName(String name) {
        name = StrUtil.trim(name);
        if (ObjectUtil.equal(this.name, name)) return this;
        handleUpdate(NAME_PROPERTY, getName(), name);
        this.name = name;       // Direct field assignment, no setter
        return this;
    }

    public Task updateStatus(TaskStatus status) {
        if (ObjectUtil.equal(this.status, status)) return this;
        handleUpdate(STATUS_PROPERTY, getStatus(), status);
        this.status = status;   // Direct field assignment
        return this;
    }

    // ... other updateXxx methods follow the same pattern

    // ===== Framework Internal: Generated switch dispatch =====
    @Override
    @FrameworkInternal
    public void internalSet(String property, Object value) {
        switch (property) {
            case "name":        this.name = (String) value; break;
            case "status":      this.status = (TaskStatus) value; break;
            case "platform":    this.platform = (Platform) value; break;
            case "createTime":  this.createTime = (LocalDateTime) value; break;
            case "updateTime":  this.updateTime = (LocalDateTime) value; break;
            case "taskLogList": this.taskLogList = (SmartList<TaskLog>) value; break;
            default:            super.internalSet(property, value);
        }
    }

    @Override
    @FrameworkInternal
    public Object internalGet(String property) {
        switch (property) {
            case "name":        return this.name;
            case "status":      return this.status;
            case "platform":    return this.platform;
            case "createTime":  return this.createTime;
            case "updateTime":  return this.updateTime;
            case "taskLogList": return this.taskLogList;
            default:            return super.internalGet(property);
        }
    }

    // NO setXxx methods generated
}
```

### 3.3 Key Design Decisions

**Why generated switch/if-else instead of reflection?**

TeaQL is a code generation framework. We have full knowledge of every entity's properties at
generation time. Generating explicit switch branches is:
- **Zero reflection overhead**: Each case is a direct field assignment (`putfield` bytecode).
  No `Method.invoke()`, no `Field.setAccessible()`, no `MethodHandle` lookup.
- **JVM-optimized**: The JVM compiles String switch into a `hashCode()` + `equals()` lookup table,
  which is O(1) in practice.
- **Compile-time verifiable**: Typos in property names or type mismatches are caught during
  code generation, not at runtime.
- **Android-friendly**: Avoids reflection performance penalties on ART/Dalvik and sidesteps
  Android's strict reflection access restrictions.

**Why `updateXxx` uses direct field assignment instead of calling `internalSet`?**

`updateXxx()` is in the same class as the field. It can assign `this.name = name` directly.
Routing through `internalSet` would add an unnecessary String switch lookup and bypass the
change tracking that `updateXxx` has already performed. The two paths serve fundamentally
different purposes:

| Path | Change Tracking | Dirty Mark | Equality Check | Purpose |
|------|:-:|:-:|:-:|---------|
| `updateXxx()` | ✅ | ✅ | ✅ | Business mutation |
| `internalSet()` | ❌ | ❌ | ❌ | Object assembly from DB/JSON |

**Why `internalSet` is `public` instead of `protected` or package-private?**

Framework infrastructure code (database hydrators, JSON modules) lives in packages like
`io.teaql.runtime` or `io.teaql.core.sql.portable`, which are in different packages from
generated entities (e.g. `com.doublechaintech.xxx.task`). Java's `protected` and
package-private access modifiers cannot cross this package boundary for non-subclass callers.

Making `internalSet` public is acceptable because **its API surface is intentionally hostile
to casual use**:
- **No type safety**: Parameters are `(String, Object)` — no IDE autocomplete for property names, no compile-time type checking.
- **No discoverability**: Developers looking for "how to set status" will find `updateStatus(TaskStatus)` in autocomplete, not `internalSet("status", obj)`.
- **Naming signal**: The `internal` prefix and `@FrameworkInternal` annotation clearly communicate "do not use".

Compare the two alternatives a developer would see:
```java
// Clean, typed, discoverable — the natural choice
task.updateStatus(newStatus);

// Ugly, untyped, undiscoverable — screams "don't touch"
task.internalSet("status", newStatus);
```

## 4. Impact on Existing Framework Code

### 4.1 Entity.setProperty() and BeanUtil.setProperty()

The existing `Entity` interface has a `setProperty(String, Object)` default method that
delegates to `BeanUtil.setProperty()`, which uses reflection to find and invoke `setXxx()`
methods, falling back to direct field access.

With this design, `Entity.setProperty()` should be refactored to delegate to `internalSet()`:

```java
// Entity interface
default void setProperty(String propertyName, Object value) {
    if (this instanceof BaseEntity) {
        ((BaseEntity) this).internalSet(propertyName, value);
    }
}
```

This eliminates the dependency on `BeanUtil`'s reflection-based setter invocation for entity
property assignment. `BeanUtil.setProperty()` remains available for non-entity beans.

### 4.2 BaseEntity.addRelation()

`BaseEntity.addRelation()` currently calls `setProperty()`, which in turn calls `BeanUtil`.
After refactoring `setProperty()` to delegate to `internalSet()`, `addRelation()` will
automatically benefit from the generated switch path with zero reflection.

### 4.3 Database Hydrator (PortableSQLRepository / SqlDataServiceExecutor)

The hydrator code that assembles entities from `ResultSet` rows should switch from
`entity.setXxx(value)` to `entity.internalSet(propertyName, value)`. This is a straightforward
change since the hydrator already works with property names from `EntityDescriptor` metadata.

### 4.4 JSON Serialization / Deserialization

**Serialization (Entity → JSON)**: No change needed. Jackson uses public getters, which remain.

**Deserialization (JSON → Entity)**: Requires a TeaQL-specific Jackson module or custom
deserializer that routes property assignment through `internalSet()` instead of relying
on setter detection:

```java
// TeaQL Jackson Module — core deserialization logic
@Override
public Object deserialize(JsonParser p, DeserializationContext ctxt) {
    BaseEntity entity = createEntity(targetType);
    while (p.nextToken() != JsonToken.END_OBJECT) {
        String fieldName = p.getCurrentName();
        p.nextToken();
        Object value = parseValue(p, fieldName);
        entity.internalSet(fieldName, value);
    }
    return entity;
}
```

Alternatively, configure Jackson globally for field-level access:
```java
mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
mapper.setVisibility(PropertyAccessor.SETTER, Visibility.NONE);
```
This approach is simpler but less controlled.

## 5. Three-Layer Defense Against Misuse

Although `internalSet` is technically callable from APP code, three layers of defense
ensure it is never used in practice:

### 5.1 Layer 1: API Design (Passive Defense)

The absence of `setXxx()` methods removes the obvious, typed, discoverable mutation path.
`internalSet(String, Object)` is not type-safe, not auto-completable, and clearly
communicates "this is not for you". Developers will naturally gravitate toward `updateXxx()`.

### 5.2 Layer 2: `@FrameworkInternal` Annotation (IDE Warning)

```java
package io.teaql.core;

import java.lang.annotation.*;

/**
 * Marks a method as framework-internal. Business code must not call methods
 * annotated with @FrameworkInternal directly.
 *
 * IDE plugins and static analysis tools should flag any call to a
 * @FrameworkInternal method from outside framework packages as an error.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Documented
public @interface FrameworkInternal {
    /** Guidance message for developers who encounter this annotation. */
    String value() default "This method is for framework infrastructure use only.";
}
```

Integration points:
- **IntelliJ IDEA**: Custom inspection rule to flag calls to `@FrameworkInternal` methods.
- **Error Prone**: Custom `BugChecker` that reports `@FrameworkInternal` call sites as errors.
- **SpotBugs / PMD**: Custom detector for the annotation.

### 5.3 Layer 3: Architecture Test (CI Hard Gate)

Using ArchUnit (or equivalent) in the project's test suite:

```java
@Test
void businessCodeMustNotCallInternalSet() {
    noClasses()
        .that().resideInAPackage("com.teaql.taskboard..")  // APP layer
        .should().callMethod(BaseEntity.class, "internalSet",
                             String.class, Object.class)
        .check(importedClasses);
}
```

This test runs in CI and fails the build if any APP-layer class calls `internalSet`.
It is a **hard gate** — no exceptions, no overrides.

## 6. Generator Change Summary

| Generated Artifact | Before | After |
|--------------------|--------|-------|
| `public void setXxx(T value)` | Generated with `@Deprecated` | **Removed entirely** |
| `public T updateXxx(T value)` | Calls `setXxx(value)` internally | **Direct field assignment** (`this.field = value`) |
| `public void internalSet(String, Object)` | Does not exist | **New**: generated switch/if-else per entity |
| `public Object internalGet(String)` | Does not exist | **New**: generated switch/if-else per entity |
| `Entity.setId()` / `Entity.setVersion()` | `void setId(Long)` / `void setVersion(Long)` | **Replaced by** `updateId(Long)` / `updateVersion(Long)` on `BaseEntity` |
| `@FrameworkInternal` annotation | Does not exist | **New**: defined in `io.teaql.core` |

## 7. Migration Path

1. **Phase 1**: Add `internalSet()` to `BaseEntity` and the `@FrameworkInternal` annotation
   in `teaql-core`. This is backward-compatible — existing setters still work.

2. **Phase 2**: Update the code generator to produce `internalSet()` overrides in each entity.
   Generate `updateXxx()` with direct field assignment. Stop generating `setXxx()` methods.

3. **Phase 3**: Update framework infrastructure (`PortableSQLRepository`, `BeanUtil` path,
   JSON modules) to use `internalSet()` instead of setter invocation.

4. **Phase 4**: Add ArchUnit tests in example projects. Remove any remaining `setXxx()` calls
   from APP code. All APP mutation goes through `updateXxx()`, all infrastructure goes through
   `internalSet()`.

## 8. Resolved Decisions

1. **`internalGet(String): Object` — YES, generate it.**
   Each entity generates a symmetric `internalGet` alongside `internalSet`. This provides a
   reflection-free, uniform generic read path for framework infrastructure (serializers, diff
   engines, audit trail builders). Public getters remain the preferred API for typed access
   in business code.

2. **`Entity.setId()` / `Entity.setVersion()` — replace with `updateId()` / `updateVersion()`.**
   The `Entity` interface will no longer declare `void setId(Long)` or `void setVersion(Long)`.
   Instead, `BaseEntity` provides `updateId(Long)` and `updateVersion(Long)` which follow the
   same pattern as all other `updateXxx()` methods: equality check, change tracking via
   `handleUpdate()`, and direct field assignment. Framework infrastructure that needs to assign
   id/version without change tracking (e.g. during hydration) uses `internalSet("id", value)`.

   This change:
   - Removes `setId` / `setVersion` from the `Entity` interface contract.
   - Provides `updateId` / `updateVersion` on `BaseEntity` for business use (e.g. seed data).
   - Routes framework hydration through `internalSet`, consistent with all other properties.

## 9. Remaining Open Question

1. **Seed data initialization style.**
   With `updateId(Long)` available on `BaseEntity`, seed data initialization can use the
   standard business API:
   ```java
   Platform platform = new Platform();
   platform.updateId(1L)
           .updateName("Android Local Platform")
           .auditAs("Seed data")
           .save(ctx);
   ```
   This is cleaner than `internalSet("id", 1L)` and fully participates in change tracking.
   However, in bulk migration or import scenarios where change tracking is undesirable,
   `internalSet` remains available as the raw assignment path. The choice between the two
   depends on whether the caller wants audit trail semantics.
