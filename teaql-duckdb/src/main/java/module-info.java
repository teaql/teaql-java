module io.teaql.duck {
    requires transitive io.teaql.core;
    requires transitive io.teaql.dataservice.sql;
    requires transitive io.teaql.utils;
    requires com.fasterxml.jackson.databind;
    requires java.sql;
    exports io.teaql.core.duck;
}
