module io.teaql.cloud {
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires static jdk.httpserver;

    exports io.teaql.cloud;
    exports io.teaql.cloud.nacos;
    exports io.teaql.cloud.consul;
}
