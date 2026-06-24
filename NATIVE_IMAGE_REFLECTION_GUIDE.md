# Native Image Reflection Guide

TeaQL keeps the main runtime path usable without reflection-heavy entity
construction or bean mutation. This makes the core stack easier to use in
closed-world runtimes such as GraalVM native image.

## Baseline

The no-reflection baseline is:

```text
teaql-core
teaql-runtime
teaql-sql-portable
teaql-jackson
```

These modules should not depend on `teaql-utils-reflection`, `ReflectUtil`, or
`BeanUtil`.

Reflection remains available only as an explicit optional utility through
`teaql-utils-reflection`. Applications that want a native-image friendly runtime
should not depend on that module unless they also provide the required native
image reflection metadata.

## Entity Creation

Do not create entities with reflective constructors.

Use metadata-registered suppliers:

```java
EntityDescriptor descriptor = new EntityDescriptor();
descriptor.setType("Task");
descriptor.setTargetType(Task.class);
descriptor.setEntitySupplier(Task::new);
```

SQL row mapping, relation references, and generated metadata should call
`EntityDescriptor.createEntity()` instead of `Class.getDeclaredConstructor()` or
`ReflectUtil.newInstance()`.

Generated metadata should always emit the supplier beside `targetType`:

```java
taskDescriptor.setTargetType(Task.class);
taskDescriptor.setEntitySupplier(Task::new);
```

## Property Mutation

Do not depend on reflective bean setters for entity mutation.

Preferred options:

- Generated code calls typed setters directly.
- Framework code uses TeaQL entity APIs such as `internalSet` for framework-owned
  fields.
- Dynamic fields are stored through TeaQL's dynamic field/additional-info path,
  not through arbitrary Java bean mutation.

Avoid:

```java
BeanUtil.setProperty(entity, "name", value);
ReflectUtil.invoke(entity, "setName", value);
```

## JSON

Entity JSON is owned by `teaql-jackson`.

Register `TeaQLModule` instead of relying on Jackson's default bean
introspection path for TeaQL entities:

```java
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new TeaQLModule());
```

`TeaQLModule` provides explicit entity serialization and deserialization for
TeaQL entity data. Application DTOs may still use normal Jackson behavior, but
TeaQL entities should stay on the explicit serializer/deserializer path.

## SQL Result Sets

SQL result-set extraction itself is not reflection. The reflection-sensitive
part is entity creation from mapped rows.

Use `EntityDescriptor.createEntity()` for:

- the main row entity;
- relation reference entities;
- SQL property reference entities.

`teaql-sql-portable` follows this rule and should not depend on
`teaql-utils-reflection`.

## Optional Reflection

Use `teaql-utils-reflection` only for application or tool code that intentionally
needs general Java reflection:

```xml
<dependency>
    <groupId>io.teaql</groupId>
    <artifactId>teaql-utils-reflection</artifactId>
    <version>${teaql.version}</version>
</dependency>
```

This dependency is not part of the no-reflection runtime baseline.

## Checks

Useful checks before native-image work:

```bash
rg -n "ReflectUtil|BeanUtil|java\\.lang\\.reflect|setAccessible\\(|Class\\.forName\\(" \
  teaql-core teaql-runtime teaql-sql-portable teaql-jackson
```

This command should have no matches for the no-reflection baseline modules.

The whole repository may still contain reflection in optional utility modules
such as `teaql-utils-reflection`, `teaql-utils`, or `teaql-utils-json`. That is
acceptable as long as the application dependency graph for the native image does
not include those reflective paths.
