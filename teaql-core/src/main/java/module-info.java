module io.teaql.core {
    requires io.teaql.utils;
    requires org.slf4j;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires java.desktop;

    // === Public API needed by generated code ===
    exports io.teaql.core;
    exports io.teaql.core.checker;
    exports io.teaql.core.criteria;
    exports io.teaql.core.meta;
    exports io.teaql.core.parser;
    exports io.teaql.core.value;
    exports io.teaql.core.log;
    exports io.teaql.core.spi;
    
    uses io.teaql.core.spi.ContextAssembler;
}
