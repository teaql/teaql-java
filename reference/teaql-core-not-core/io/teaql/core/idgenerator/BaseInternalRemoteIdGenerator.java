package io.teaql.core.idgenerator;

import io.teaql.utils.json.JSONUtil;

import io.teaql.core.Entity;
import io.teaql.core.InternalIdGenerator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class BaseInternalRemoteIdGenerator implements InternalIdGenerator {

    @Override
    public Long generateId(Entity baseEntity) {
        String url = System.getProperty("id-gen-service-url", "http://localhost:8080/genId");
        String body = "{\"typeName\":\"" + baseEntity.typeName() + "\"}";
        String response = post(url, body);
        RemoteIdGenResponse result = JSONUtil.toBean(response, RemoteIdGenResponse.class);
        return result.getCurrent();
    }

    private String post(String url, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "text/plain")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            return HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString())
                    .body();
        } catch (Exception e) {
            throw new RuntimeException("Remote id generation failed", e);
        }
    }
}
