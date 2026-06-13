module io.teaql.db2 {
    requires transitive io.teaql.core;
    requires transitive io.teaql.dataservice.sql;
    requires transitive io.teaql.utils;
    requires java.sql;
    exports io.teaql.core.db2;
}
