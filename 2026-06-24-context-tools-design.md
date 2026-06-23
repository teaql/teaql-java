# TeaQL Context Tools Design

## Positioning

TeaQL tools are optional context capabilities. They are not part of `teaql-core`
or `teaql-runtime`.

The base tools module should contain only:

- tool API
- tool registry
- tool policy
- service discovery
- memory-only safe tools

Tools that access external resources, add heavy dependencies, or create a
security boundary should be separate provider modules.

## Selection Model

Java does not have Cargo-style features. TeaQL uses three separate controls
instead:

1. Maven dependency / JPMS module path decides whether tool code and dependency
   jars enter the application package.
2. `ToolPolicy` decides whether the current application context allows a tool.
3. Environment acknowledgement variables only confirm dangerous behavior.

Environment variables should not be used as generic `ENABLED` switches. Enabling
belongs to `ToolPolicy`.

## Tool Categories

Memory-only tools stay in the base context tools module.

External resource tools should be separate modules, for example:

- `teaql-tool-http`
- `teaql-tool-pdf`
- `teaql-tool-excel`

Privileged tools should be separate modules and denied by default, for example:

- `teaql-tool-shell`
- `teaql-tool-filesystem`

## API Shape

`ContextTools` is only an entry point. It must not grow one static method for
every tool.

Preferred usage:

```java
Tools tools = ContextTools.builder(ctx)
        .policy(policy)
        .build();

AgentHttpTool http = tools.get(AgentHttpTool.class);
```

Compatibility shortcuts may exist for common tools, but the extensible mechanism
is type-based lookup through `Tools`.

## SPI

Tool implementations are discovered through `ServiceLoader`.

```java
public interface ToolProvider {
    ToolDescriptor descriptor();

    <T> T create(Class<T> toolType, UserContext ctx);
}
```

JPMS modules should use `uses` and `provides`:

```java
module io.teaql.tool.http {
    requires io.teaql.context.tools;

    provides io.teaql.tools.spi.ToolProvider
        with io.teaql.tools.http.HttpToolProvider;
}
```

Implementation packages should remain unexported.

## Acknowledgement Rules

Acknowledgement variables are for dangerous confirmation only. They should be
long, explicit declarations, similar to the runtime trace opt-out:

```text
TEAQL_TRACE_OFF_ACK=__i_agree_to_disable_runtime_trace_only_for_extreme_performance_testing
```

Example for shell:

```text
TEAQL_TOOL_SHELL_ACK=__i_understand_shell_tool_can_execute_os_commands
```

The presence of this variable does not enable the tool by itself. The tool still
requires:

- provider module present on module path or classpath
- `ToolPolicy` allowing the tool
- acknowledgement value matching, when the descriptor requires it
