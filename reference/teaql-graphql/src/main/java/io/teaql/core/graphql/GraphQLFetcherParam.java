package io.teaql.core.graphql;

import io.teaql.core.UserContext;

public record GraphQLFetcherParam(
        UserContext userContext, String parentType, String field, Object[] params) {
    public GraphQLFetcherParam(UserContext userContext, String parentType, String field) {
        this(userContext, parentType, field, null);
    }
}
