package io.teaql.tools.http.impl;

import io.teaql.core.UserContext;
import io.teaql.core.http.HttpRequestFailure;
import io.teaql.core.http.HttpRequestService;
import io.teaql.tools.http.AgentHttpTool;
import io.teaql.tools.http.ExecutableHttpTool;
import io.teaql.tools.http.HttpCommentPhase;
import io.teaql.tools.http.HttpIntentPhase;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

/** Agent tool adapter: all effects pass through the same context-owned HTTP service. */
public final class JdkHttpTool implements AgentHttpTool {
    private final UserContext context;

    public JdkHttpTool(UserContext context) { this.context = context; }

    @Override public HttpIntentPhase get(String profile, String relativePath) {
        return new IntentPhase(profile, relativePath, HttpRequestService.Method.GET, null);
    }
    @Override public HttpIntentPhase post(String profile, String relativePath, Object body) {
        return new IntentPhase(profile, relativePath, HttpRequestService.Method.POST, body);
    }
    @Override public HttpIntentPhase get(String target) {
        String[] parts = splitTarget(target);
        return get(parts[0], parts[1]);
    }
    @Override public HttpIntentPhase post(String target, Object body) {
        String[] parts = splitTarget(target);
        return post(parts[0], parts[1], body);
    }

    private static String[] splitTarget(String target) {
        int slash = target == null ? -1 : target.indexOf('/');
        if (slash <= 0 || slash == target.length() - 1
                || !target.substring(0, slash).matches("[A-Za-z][A-Za-z0-9_-]{0,63}")) {
            throw new HttpRequestFailure(HttpRequestFailure.Code.INVALID_INPUT);
        }
        return new String[] {target.substring(0, slash), target.substring(slash + 1)};
    }

    private final class IntentPhase implements HttpIntentPhase {
        private final String profile;
        private final String path;
        private final HttpRequestService.Method method;
        private final Object body;
        IntentPhase(String profile, String path, HttpRequestService.Method method, Object body) {
            this.profile = profile;
            this.path = path;
            this.method = method;
            this.body = body;
        }
        @Override public HttpCommentPhase comment(String comment) {
            return new CommentPhase(profile, path, method, body, comment);
        }
        @Override public ExecutableHttpTool purpose(String purpose) {
            return new Executable(profile, path, method, body, null,
                    HttpRequestService.IntentKind.PURPOSE, purpose);
        }
        @Override public ExecutableHttpTool auditAs(String audit) {
            return new Executable(profile, path, method, body, null,
                    HttpRequestService.IntentKind.AUDIT, audit);
        }
    }

    private final class CommentPhase implements HttpCommentPhase {
        private final String profile;
        private final String path;
        private final HttpRequestService.Method method;
        private final Object body;
        private final String comment;
        CommentPhase(String profile, String path, HttpRequestService.Method method, Object body,
                     String comment) {
            this.profile = profile;
            this.path = path;
            this.method = method;
            this.body = body;
            this.comment = comment;
        }
        @Override public ExecutableHttpTool purpose(String purpose) {
            return new Executable(profile, path, method, body, comment,
                    HttpRequestService.IntentKind.PURPOSE, purpose);
        }
        @Override public ExecutableHttpTool auditAs(String audit) {
            return new Executable(profile, path, method, body, comment,
                    HttpRequestService.IntentKind.AUDIT, audit);
        }
    }

    private final class Executable implements ExecutableHttpTool {
        private final String profile;
        private final String path;
        private final HttpRequestService.Method method;
        private final Object body;
        private final String comment;
        private final HttpRequestService.IntentKind kind;
        private final String intent;
        Executable(String profile, String path, HttpRequestService.Method method, Object body,
                   String comment, HttpRequestService.IntentKind kind, String intent) {
            this.profile = profile;
            this.path = path;
            this.method = method;
            this.body = body;
            this.comment = comment;
            this.kind = kind;
            this.intent = intent;
        }
        @Override public String execute() {
            byte[] bytes = new byte[0];
            String contentType = null;
            if (method == HttpRequestService.Method.POST) {
                if (body instanceof Map<?, ?> values) {
                    bytes = formEncode(values).getBytes(StandardCharsets.UTF_8);
                    contentType = "application/x-www-form-urlencoded";
                } else {
                    bytes = (body == null ? "" : String.valueOf(body)).getBytes(StandardCharsets.UTF_8);
                    contentType = "text/plain; charset=utf-8";
                }
            }
            HttpRequestService.Call call = new HttpRequestService.Call(profile, method, path,
                    Map.of(), bytes, contentType, comment, kind, intent,
                    HttpRequestService.RetryMode.NONE);
            if (context == null) throw new HttpRequestFailure(HttpRequestFailure.Code.INVALID_INPUT);
            return context.httpRequests().execute(context, call).text();
        }
    }

    private static String formEncode(Map<?, ?> values) {
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey());
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            joiner.add(URLEncoder.encode(key, StandardCharsets.UTF_8) + "="
                    + URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
        return joiner.toString();
    }
}
