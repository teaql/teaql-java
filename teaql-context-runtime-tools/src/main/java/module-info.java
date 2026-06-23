module io.teaql.context.runtime.tools {
    requires io.teaql.core;

    exports io.teaql.tools;
    exports io.teaql.tools.spi;

    uses io.teaql.tools.spi.ToolProvider;
    provides io.teaql.tools.spi.ToolProvider with io.teaql.tools.impl.AgentToolProviderImpl;
    provides io.teaql.tools.spi.AgentToolProvider with io.teaql.tools.impl.AgentToolProviderImpl;
}
