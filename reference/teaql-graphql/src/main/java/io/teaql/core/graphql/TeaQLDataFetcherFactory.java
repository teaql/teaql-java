package io.teaql.core.graphql;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetcherFactory;
import graphql.schema.DataFetcherFactoryEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.PropertyDataFetcher;

public class TeaQLDataFetcherFactory implements DataFetcherFactory {

    @Override
    public DataFetcher get(DataFetcherFactoryEnvironment environment) {
        GraphQLFieldDefinition fieldDefinition = environment.getFieldDefinition();
        GraphQLOutputType type = fieldDefinition.getType();
        if (GraphQLTypeUtil.isList(type) || GraphQLTypeUtil.isObjectType(type)) {
            return new TeaQLDataFetcher(environment);
        }
        return PropertyDataFetcher.fetching(environment.getFieldDefinition().getName());
    }
}
