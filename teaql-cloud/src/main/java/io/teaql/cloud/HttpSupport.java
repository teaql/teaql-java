package io.teaql.cloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

public final class HttpSupport {
    public static final ObjectMapper JSON = new ObjectMapper();

    private final String baseUrl;
    private final HttpClient client;
    private final Map<String, String> headers;

    public HttpSupport(String baseUrl, HttpClient client, Map<String, String> headers) {
        String normalized = baseUrl.contains("://") ? baseUrl : "http://" + baseUrl;
        this.baseUrl = normalized.replaceAll("/+$", "");
        this.client = client == null
                ? HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
                : client;
        this.headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public String request(String method, String path, Map<String, String> parameters, boolean form) {
        try {
            String encoded = encode(parameters);
            boolean query = !form || method.equals("GET") || method.equals("DELETE");
            URI uri = URI.create(baseUrl + path + (query && !encoded.isEmpty() ? "?" + encoded : ""));
            HttpRequest.BodyPublisher body = form && !query
                    ? HttpRequest.BodyPublishers.ofString(encoded)
                    : HttpRequest.BodyPublishers.noBody();
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5))
                    .method(method, body);
            headers.forEach(builder::header);
            if (form && !query) builder.header("Content-Type", "application/x-www-form-urlencoded");
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new CloudException(method + " " + path + " failed: " + response.statusCode() + ": " + response.body());
            }
            return response.body();
        } catch (CloudException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new CloudException(method + " " + path + " failed", e);
        }
    }

    public String json(String method, String path, Object value) {
        try {
            String payload = value == null ? "" : JSON.writeValueAsString(value);
            HttpRequest.BodyPublisher body = value == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(payload);
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(5)).method(method, body);
            headers.forEach(builder::header);
            if (value != null) builder.header("Content-Type", "application/json");
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new CloudException(method + " " + path + " failed: " + response.statusCode() + ": " + response.body());
            }
            return response.body();
        } catch (CloudException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new CloudException(method + " " + path + " failed", e);
        }
    }

    public static String encode(Map<String, String> values) {
        return values.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
