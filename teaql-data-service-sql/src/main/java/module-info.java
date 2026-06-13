module io.teaql.dataservice.sql {
    requires io.teaql.core;
    requires transitive io.teaql.sql.portable;
    requires java.sql;

    exports io.teaql.dataservice.sql;
}
