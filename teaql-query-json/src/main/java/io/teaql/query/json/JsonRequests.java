package io.teaql.query.json;

import io.teaql.core.BaseRequest;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityMetaFactory;

public final class JsonRequests {

    private JsonRequests() {
    }

    @Deprecated
    public static <T extends BaseRequest<?>> T findWithJson(T request, String jsonExpression) {
        return applyJson(request, jsonExpression);
    }

    private static <T extends BaseRequest<?>> T applyJson(T request, String jsonExpression) {
        if (request == null) {
            return null;
        }
        if (jsonExpression == null || jsonExpression.trim().isEmpty()) {
            return request;
        }
        new DynamicSearchHelper().mergeClauses(request, DynamicSearchHelper.jsonFromString(jsonExpression));
        return request;
    }

    /**
     * Applies dynamic input using only metadata installed in the invoking context.
     */
    public static <T extends BaseRequest<?>> T findWithJson(
            UserContext context, T request, String jsonExpression) {
        if (request == null) {
            return null;
        }
        request.bindMetadata(EntityMetaFactory.requireFrom(context));
        return applyJson(request, jsonExpression);
    }
}
