module io.teaql.businessid.jdbc {
    requires transitive io.teaql.core;
    requires io.teaql.utils;
    requires io.teaql.sql.portable;
    requires java.logging;
    requires java.sql;

    exports io.teaql.businessid.jdbc;
}
