module io.teaql.blob.reference {
    requires transitive io.teaql.core;
    requires transitive io.teaql.runtime;
    requires com.fasterxml.jackson.databind;
    requires transitive io.teaql.dataservice.sql;
    exports io.teaql.blob.reference;
}
