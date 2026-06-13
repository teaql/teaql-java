module io.teaql.autoconfigure {
    requires io.teaql.core;
    requires io.teaql.utils;
    requires spring.boot.autoconfigure;
    requires spring.boot;
    requires spring.context;
    requires spring.web;
    requires spring.webmvc;
    requires spring.webflux;
    requires spring.beans;
    requires spring.core;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires jakarta.servlet;
    requires org.slf4j;
    requires reactor.core;
    requires redisson;
    requires redisson.spring.boot.starter;

    exports io.teaql.autoconfigure;
    exports io.teaql.autoconfigure.lock;
    exports io.teaql.autoconfigure.log;
    exports io.teaql.autoconfigure.redis;
    exports io.teaql.autoconfigure.web;
    exports io.teaql.core.web;
    exports io.teaql.core.jackson;

    opens io.teaql.core.jackson to com.fasterxml.jackson.databind;
}
