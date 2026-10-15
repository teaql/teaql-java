module io.teaql.sqlite {
    requires transitive io.teaql.core;
    requires transitive io.teaql.sql.portable;
    requires transitive io.teaql.utils;
    requires transitive io.teaql.dataservice.sql;
    requires io.teaql.provider.jdbc;
    requires com.fasterxml.jackson.databind;
    requires org.xerial.sqlitejdbc;
    requires java.sql;
    exports io.teaql.core.sqlite;
}
