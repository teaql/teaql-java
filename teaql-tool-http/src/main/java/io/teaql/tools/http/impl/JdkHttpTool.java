package io.teaql.tools.http.impl;

import io.teaql.core.UserContext;
import io.teaql.tools.http.AgentHttpTool;
import io.teaql.tools.http.ExecutableHttpTool;
import io.teaql.tools.http.HttpIntentPhase;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

public class JdkHttpTool implements AgentHttpTool {
    private final UserContext ctx;
    private final HttpClient client;

    public JdkHttpTool(UserContext ctx) {
        this.ctx = ctx;
        this.client = HttpClient.newHttpClient();
    }

    @Override
    public HttpIntentPhase get(String url) {
        return new IntentPhase("GET", url, null);
    }

    @Override
    public HttpIntentPhase post(String url, Object body) {
        return new IntentPhase("POST", url, body);
    }

    private final class IntentPhase implements HttpIntentPhase {
        private final String method;
        private final String url;
        private final Object body;

        private IntentPhase(String method, String url, Object body) {
            this.method = method;
            this.url = url;
            this.body = body;
        }

        @Override
        public ExecutableHttpTool purpose(String purposeMessage) {
            return new Executable(method, url, body, "PURPOSE", purposeMessage);
        }

        @Override
        public ExecutableHttpTool auditAs(String auditMessage) {
            return new Executable(method, url, body, "AUDIT", auditMessage);
        }
    }

    private final class Executable implements ExecutableHttpTool {
        private final String method;
        private final String url;
        private final Object body;
        private final String intentType;
        private final String intent;

        private Executable(String method, String url, Object body, String intentType, String intent) {
            this.method = method;
            this.url = url;
            this.body = body;
            this.intentType = intentType;
            this.intent = intent;
        }

        @Override
        public String execute() {
            if (intent == null || intent.trim().isEmpty()) {
                throw new IllegalStateException("HTTP tool execution requires purpose or audit text.");
            }
            if (ctx != null) {
                ctx.pushTrace("HTTP " + method + " " + url + " " + intentType + ": " + intent);
            }
            try {
                HttpRequest request = buildRequest();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return response.body();
            } catch (Exception e) {
                throw new RuntimeException("HTTP tool execution failed", e);
            } finally {
                if (ctx != null) {
                    ctx.popTrace();
                }
            }
        }

        private HttpRequest buildRequest() {
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));
            if ("GET".equals(method)) {
                return builder.GET().build();
            }
            if (body instanceof Map<?, ?> values) {
                return builder
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formEncode(values)))
                        .build();
            }
            return builder
                    .header("Content-Type", "text/plain")
                    .POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : String.valueOf(body)))
                    .build();
        }
    }

    private static String formEncode(Map<?, ?> values) {
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey());
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            joiner.add(urlEncode(key) + "=" + urlEncode(value));
        }
        return joiner.toString();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
