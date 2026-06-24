module io.teaql.core {
    requires io.teaql.utils;

    // === Public API needed by generated code ===
    exports io.teaql.core;
    exports io.teaql.core.checker;
    exports io.teaql.core.criteria;
    exports io.teaql.core.meta;
    exports io.teaql.core.parser;
    exports io.teaql.core.value;
    exports io.teaql.core.spi;
    
    uses io.teaql.core.spi.ContextAssembler;
}
