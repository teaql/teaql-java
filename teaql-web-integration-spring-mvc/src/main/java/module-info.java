module io.teaql.web.integration.springmvc {
    requires io.teaql.core;
    requires io.teaql.runtime;
    requires com.fasterxml.jackson.databind;
    requires static spring.web;

    exports io.teaql.web.integration.springmvc;
}
