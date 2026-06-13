package io.teaql.core.graphql;

import io.teaql.core.BaseRequest;
import io.teaql.core.UserContext;

public interface GraphQLFieldQuery {

    String id();

    BaseRequest buildQuery(UserContext userContext, Object[] parameters);

    String getRequestProperty();
}
