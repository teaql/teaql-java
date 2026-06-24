package io.teaql.query.json;

import io.teaql.core.BaseRequest;

public final class JsonRequests {

    private JsonRequests() {
    }

    public static <T extends BaseRequest<?>> T findWithJson(T request, String jsonExpression) {
        if (request == null) {
            return null;
        }
        if (jsonExpression == null || jsonExpression.trim().isEmpty()) {
            return request;
        }
        new DynamicSearchHelper().mergeClauses(request, DynamicSearchHelper.jsonFromString(jsonExpression));
        return request;
    }
}
