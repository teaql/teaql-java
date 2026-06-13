package io.teaql.core.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class HttpUtil {

    public static java.lang.String post(java.lang.String p0, java.lang.String p1) {
        return post(p0, p1, -1);
    }

    public static java.lang.String post(java.lang.String p0, java.lang.String p1, int p2) {
        try {
            HttpClient.Builder builder = HttpClient.newBuilder();
            HttpClient client = builder.build();
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(p0))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(p1));
            if (p2 > 0) {
                reqBuilder.timeout(Duration.ofMillis(p2));
            }
            HttpResponse<String> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            throw new RuntimeException("Http post failed", e);
        }
    }

    public static java.lang.String post(java.lang.String p0, java.util.Map<java.lang.String, java.lang.Object> p1) {
        return post(p0, p1, -1);
    }

    public static java.lang.String post(java.lang.String p0, java.util.Map<java.lang.String, java.lang.Object> p1, int p2) {
        StringBuilder sb = new StringBuilder();
        if (p1 != null) {
            for (Map.Entry<String, Object> entry : p1.entrySet()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(URLEncodeUtil.encode(entry.getKey()))
                  .append("=")
                  .append(URLEncodeUtil.encode(entry.getValue() != null ? entry.getValue().toString() : ""));
            }
        }
        try {
            HttpClient.Builder builder = HttpClient.newBuilder();
            HttpClient client = builder.build();
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(p0))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(sb.toString()));
            if (p2 > 0) {
                reqBuilder.timeout(Duration.ofMillis(p2));
            }
            HttpResponse<String> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            throw new RuntimeException("Http post failed", e);
        }
    }

}
