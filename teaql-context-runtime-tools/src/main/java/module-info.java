module io.teaql.context.tools {
    requires io.teaql.core;

    exports io.teaql.tools;
    exports io.teaql.tools.spi;

    uses io.teaql.tools.spi.ToolProvider;
}
