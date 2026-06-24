module io.teaql.sql.portable {
    requires io.teaql.core;
    requires io.teaql.utils;
    requires io.teaql.utils.reflection;
    requires io.teaql.runtime;
    requires java.sql;
    requires com.fasterxml.jackson.databind;

    exports io.teaql.core.sql;
    exports io.teaql.core.sql.dialect;
    exports io.teaql.core.sql.expression;
    exports io.teaql.core.sql.portable;
}
