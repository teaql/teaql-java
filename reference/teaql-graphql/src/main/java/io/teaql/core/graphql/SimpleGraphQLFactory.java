package io.teaql.core.graphql;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.teaql.core.TQLException;

public class SimpleGraphQLFactory implements GraphQLSupport {

    Map<String, GraphQLFieldQuery> queries = new ConcurrentHashMap<>();

    @Override
    public GraphQLFieldQuery findFieldQuery(GraphQLFetcherParam param) {
        GraphQLFieldQuery graphqlFieldQuery = queries.get(param.parentType() + ":" + param.field());
        if (graphqlFieldQuery == null) {
            throw new TQLException(
                    "Could not find GraphqlFieldQuery for " + param.parentType() + ":" + param.field());
        }
        return graphqlFieldQuery;
    }

    public void register(GraphQLFieldQuery query) {
        queries.put(query.id(), query);
    }
}
