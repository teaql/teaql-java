module io.teaql.provider.springjdbc {
    requires io.teaql.core;
    requires io.teaql.coreservice;
    requires io.teaql.coreservice.sql;
    requires java.sql;
    requires spring.jdbc;
    requires spring.tx;
    exports io.teaql.provider.springjdbc;
}
