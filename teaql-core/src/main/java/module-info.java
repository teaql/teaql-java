module io.teaql.core {
    requires io.teaql.utils;
    requires transitive io.teaql.data.dynamic;
    requires com.fasterxml.jackson.databind;

    // === Public API needed by generated code ===
    exports io.teaql.core;
    exports io.teaql.core.checker;
    exports io.teaql.core.i18n;
    exports io.teaql.core.criteria;
    exports io.teaql.core.meta;
    exports io.teaql.core.parser;
    exports io.teaql.core.value;
    exports io.teaql.core.spi;
    
    uses io.teaql.core.spi.ContextAssembler;
    uses io.teaql.core.spi.RemoteCacheProvider;
    uses io.teaql.core.spi.LocalCacheProvider;
    uses io.teaql.core.spi.RemoteLockProvider;
    uses io.teaql.core.spi.LocalLockProvider;
}
