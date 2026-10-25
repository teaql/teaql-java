module io.teaql.runtime {
    requires io.teaql.core;
    requires io.teaql.utils;
    requires com.fasterxml.jackson.databind;

    exports io.teaql.runtime;
    exports io.teaql.runtime.memory;
    exports io.teaql.runtime.boot;
    exports io.teaql.runtime.businessid;

    provides io.teaql.core.spi.ContextAssembler with io.teaql.runtime.boot.CoreAssembler;
    uses io.teaql.runtime.StandardTelemetryFactory;
}
