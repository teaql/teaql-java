module io.teaql.graphql {
    requires io.teaql.core;
    requires io.teaql.utils;
    requires spring.context;
    requires spring.boot.autoconfigure;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.graphqljava;
    requires com.graphqljava.extendedscalars;

    exports io.teaql.core.graphql;
}
