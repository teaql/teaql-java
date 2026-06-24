module io.teaql.tool.http {
    requires io.teaql.core;
    requires io.teaql.context.tools;
    requires java.net.http;

    exports io.teaql.tools.http;

    provides io.teaql.tools.spi.ToolProvider with io.teaql.tools.http.impl.HttpToolProvider;
}
