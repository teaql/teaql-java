module io.teaql.sql {
    requires io.teaql.core;
    requires io.teaql.utils;
    requires io.teaql.utils.reflection;
    requires spring.jdbc;
    requires spring.tx;
    requires java.sql;
    requires org.slf4j;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires io.teaql.coreservice.sql;
    requires io.teaql.provider.springjdbc;

    exports io.teaql.core.sql;
    exports io.teaql.core.sql.dialect;
    exports io.teaql.core.sql.expression;
}
