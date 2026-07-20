module io.teaql.provider.springjdbc {
    requires io.teaql.core;
    requires io.teaql.dataservice.sql;
    requires spring.jdbc;
    requires spring.beans;
    requires java.sql;

    exports io.teaql.provider.springjdbc;
}
