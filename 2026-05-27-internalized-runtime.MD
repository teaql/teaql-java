# Internalized Runtime

Date: 2026-05-27

## Principle

User business code only works with `UserContext`.

Any capability required by business developers is exposed through `UserContext` or a semantic facade returned by `UserContext`.

Any capability required only for framework execution stays inside the internal runtime layer.

## Scope

This document applies to the Java implementation in `teaql-spring-boot-starter`.

The current public programming model already centers on `io.teaql.core.UserContext`. The next step is to make that boundary explicit and keep runtime concerns behind it.

## Public API Boundary

Business code uses `UserContext` as the entry point:

```java
public Object action(@TQLContext UserContext ctx) {
    return ctx.executeForList(request);
}
```

The starter also supports the natural Spring MVC/WebFlux style:

```java
public Object action(UserContext ctx) {
    return ctx.executeForList(request);
}
```

The following capabilities belong on `UserContext` or facades returned by it:

- Query execution
- Save and delete operations
- Aggregation
- Request-scoped attributes
- Current request and response access when running inside web flows
- Logging helpers
- Validation/checking entry points
- Translation entry points
- Current time and request-local utility methods

## Internal Runtime Boundary

`TQLResolver` is the current runtime resolver. It resolves repositories, entity metadata, and Spring beans.

This resolver is runtime infrastructure. Business code does not depend on it directly.

The runtime layer owns:

- Repository resolution
- Entity metadata resolution
- Bean lookup
- User context initialization
- Request/response holder wiring
- Checker discovery
- Translator discovery
- Framework bootstrap fallback through `GLobalResolver`

`UserContext` may delegate to these services internally, but the public programming model remains `UserContext`.

## UserContext Method Classification

These methods are business-facing API:

```java
ctx.executeForOne(...)
ctx.executeForList(...)
ctx.executeForStream(...)
ctx.aggregation(...)
ctx.saveGraph(...)
ctx.delete(...)
ctx.put(...)
ctx.getObj(...)
ctx.getStr(...)
ctx.now()
ctx.info(...)
ctx.debug(...)
ctx.warn(...)
ctx.error(...)
```

These methods are framework support API:

```java
ctx.resolveRepository(...)
ctx.resolveEntityDescriptor(...)
ctx.getBean(...)
ctx.getResolver()
ctx.setResolver(...)
ctx.init(...)
```

Framework support API remains available for compatibility while the starter moves creation and runtime wiring behind dedicated components.

## Starter Responsibilities

`teaql-autoconfigure` creates and wires `UserContext` instances.

MVC and WebFlux argument resolvers obtain `UserContext` from a factory rather than constructing it directly.

The starter owns:

- Creating the correct `UserContext` subclass from configuration
- Running `UserContextInitializer` instances
- Wiring servlet and reactive request data
- Wiring response holders
- Keeping runtime resolver details out of controllers and services

## UserContextFactory

The starter provides a `UserContextFactory`.

```java
public interface UserContextFactory {
    UserContext create(Object request);
}
```

The default implementation:

1. Reads the configured `contextClass`
2. Instantiates the `UserContext`
3. Initializes it with the current request or exchange
4. Returns the initialized context

Projects can override the factory to attach application-specific user, tenant, locale, trace, or security data.

## Controller Argument Resolution

MVC and WebFlux support both explicit and natural parameter styles:

```java
public Object action(@TQLContext UserContext ctx)
```

```java
public Object action(UserContext ctx)
```

`@TQLContext` remains valid as an explicit marker. A plain `UserContext` parameter is also resolved by the starter.

## Migration Order

1. Add `UserContextFactory` to the public starter API.
2. Register a default factory in auto-configuration.
3. Change MVC and WebFlux argument resolvers to use the factory.
4. Support plain `UserContext` parameters in addition to `@TQLContext`.
5. Mark resolver access methods as framework support API in code documentation.
6. Keep repository APIs accepting `UserContext`.
7. Update README usage examples to show `UserContext` as the only business entry point.

## Non-Goals

This change does not rename repository APIs.

This change does not remove `TQLResolver`.

This change does not require an immediate package move for existing runtime support classes.

This change does not expose a public `Runtime` object.

## Final Boundary

Application code imports and passes `UserContext`.

Starter code creates, initializes, and injects `UserContext`.

Runtime infrastructure resolves repositories, metadata, beans, request data, response data, and framework services behind `UserContext`.

