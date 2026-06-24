# TeaQL Utils Split Design

## Problem

`teaql-utils` is a foundation dependency of `teaql-core`. Any dependency kept in
`teaql-utils` is effectively pulled into the base TeaQL stack.

The old module mixed several different concerns:

- pure JDK helpers
- reflection and bean mutation helpers
- Jackson JSON conversion
- Spring helpers and classpath scanning
- logging helpers
- cache helpers backed by Guava
- HTTP helper code

This makes `teaql-core` look heavier than it should be.

## Target Shape

The long-term target is:

```text
teaql-utils
  Pure JDK helpers only.

teaql-utils-reflection
  ReflectUtil, BeanUtil, and reflection-backed class helpers.

teaql-utils-json
  JSONUtil and Jackson-backed conversion helpers.

teaql-utils-spring
  SpringUtil and Spring-backed scanning helpers.
```

`teaql-core` should eventually depend only on `teaql-utils`.

## Phase 1: Dependency Slimming

Before creating new modules, remove dependencies that are not structurally
required by the active code.

Done in this phase:

- HTTP helper moved out of `teaql-utils` into `teaql-tool-http`
- `java.net.http` removed from `teaql-utils`
- `java.desktop` removed from `teaql-utils`
- `commons-io` removed from `teaql-utils`
- Guava-backed cache implementations replaced with JDK implementations
- Guava removed from `teaql-utils`
- `StaticLog` removed from `teaql-utils`
- slf4j removed from `teaql-utils`
- Jackson-backed `JSONUtil` moved to `teaql-utils-json`
- `Convert` changed to a pure JDK converter for common scalar types
- Jackson removed from `teaql-utils`
- Spring-backed `SpringUtil` and classpath scanning moved to
  `teaql-utils-spring`
- Spring removed from `teaql-utils`
- Reflection-backed `ReflectUtil` and `BeanUtil` moved to
  `teaql-utils-reflection`

After phase 1, `teaql-utils` contains only pure helper code plus the remaining
commons helpers. `teaql-core` still depends on reflection explicitly through
`teaql-utils-reflection`; removing that requires API-level decisions.

## Phase 2: JSON

Move Jackson-backed helpers into `teaql-utils-json`:

- `JSONUtil`
- the Jackson-backed part of `Convert`
- `TypeReference`, if the current API is preserved

Status: `JSONUtil` now lives in `teaql-utils-json`, and `Convert` no longer
depends on Jackson for common scalar conversions.

## Phase 3: Spring

Move Spring-specific helpers into `teaql-utils-spring`:

- `SpringUtil`
- Spring-backed classpath scanning currently inside `ClassUtil`

`ClassUtil` should either keep only pure JDK class helpers, or the scanning
method should move to a Spring-specific class.

Status: `SpringUtil` now lives in `io.teaql.utils.spring.SpringUtil`;
Spring-backed package scanning now lives in
`io.teaql.utils.spring.SpringClassUtil`. `ClassUtil` keeps only pure JDK class
helpers.

## Phase 4: Reflection

Move reflection-heavy helpers into `teaql-utils-reflection`:

- `ReflectUtil`
- `BeanUtil`
- reflection-backed parts of `ClassUtil`

Status: `ReflectUtil` and `BeanUtil` now live in
`io.teaql.utils.reflect`. `ClassUtil` currently stays in `teaql-utils` because
its active code is pure JDK class inspection/loading. `teaql-core` and SQL
modules now depend on `teaql-utils-reflection` explicitly where they still need
reflective mutation or instantiation.

This phase requires core API work. Current core usage includes:

- `Entity.getProperty()` / `Entity.setProperty()`
- `Entity.updateProperty()`
- `BaseRequest` temporary request instantiation
- `EntityDescriptor` property/relation descriptor instantiation

The final target is for generated entities and metadata to expose typed or
interface-driven mutation/instantiation paths so `teaql-core` does not need
general reflection utilities.
