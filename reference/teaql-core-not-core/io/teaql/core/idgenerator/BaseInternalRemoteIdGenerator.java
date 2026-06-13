package io.teaql.core.idgenerator;

import io.teaql.core.utils.HttpUtil;
import io.teaql.core.utils.JSONUtil;

import io.teaql.core.Entity;
import io.teaql.core.InternalIdGenerator;

public class BaseInternalRemoteIdGenerator implements InternalIdGenerator {

    @Override
    public Long generateId(Entity baseEntity) {
        String url = System.getProperty("id-gen-service-url", "http://localhost:8080/genId");
        String body = "{\"typeName\":\"" + baseEntity.typeName() + "\"}";
        String response = HttpUtil.post(url, body);
        RemoteIdGenResponse result = JSONUtil.toBean(response, RemoteIdGenResponse.class);
        return result.getCurrent();
    }
}
