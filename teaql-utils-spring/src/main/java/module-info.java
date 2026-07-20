module io.teaql.utils.spring {
    requires io.teaql.utils;
    requires static spring.context;
    requires static spring.beans;
    requires static spring.core;

    exports io.teaql.utils.spring;
}
