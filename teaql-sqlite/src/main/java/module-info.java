module io.teaql.sqlite {
    requires transitive io.teaql.core;
    requires transitive io.teaql.sql.portable;
    requires transitive io.teaql.utils;
    requires transitive io.teaql.dataservice.sql;
    requires java.sql;
    exports io.teaql.core.sqlite;
}
