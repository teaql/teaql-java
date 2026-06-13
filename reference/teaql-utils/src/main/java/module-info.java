module io.teaql.utils {
    requires org.slf4j;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires java.desktop;
    requires java.net.http;
    requires static spring.context;      // SpringUtil — scope=provided, not transitive
    requires static spring.beans;        // BeanUtil, ClassUtil
    requires static spring.core;         // ClassUtil
    requires org.apache.commons.lang3;
    requires org.apache.commons.collections4;
    requires org.apache.commons.io;
    requires com.google.common;

    exports io.teaql.core.utils;
}
