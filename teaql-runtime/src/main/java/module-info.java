module io.teaql.runtime {
    requires io.teaql.core;
    requires io.teaql.utils;

    exports io.teaql.runtime;
    exports io.teaql.runtime.memory;
    exports io.teaql.runtime.boot;

    provides io.teaql.core.spi.ContextAssembler with io.teaql.runtime.boot.CoreAssembler;
}
