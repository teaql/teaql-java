module io.teaql.runtime {
    requires io.teaql.core;
    requires io.teaql.utils;
    requires org.slf4j;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    exports io.teaql.runtime;
    exports io.teaql.runtime.memory;
    exports io.teaql.runtime.boot;

    provides io.teaql.core.spi.ContextAssembler with io.teaql.runtime.boot.CoreAssembler;
}
