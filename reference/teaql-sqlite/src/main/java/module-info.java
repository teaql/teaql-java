module io.teaql.sqlite {
    requires io.teaql.core;
    requires io.teaql.sql;
    requires io.teaql.utils;
    requires java.sql;
    requires static spring.jdbc;
    requires static spring.tx;

    exports io.teaql.core.sqlite;
}
