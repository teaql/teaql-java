module io.teaql.data.dynamic.jdbc {
    requires io.teaql.data.dynamic;
    requires io.teaql.provider.jdbc;
    requires io.teaql.dataservice.sql;
    requires java.sql;
    requires java.logging;

    exports io.teaql.data.dynamic.jdbc;
}
