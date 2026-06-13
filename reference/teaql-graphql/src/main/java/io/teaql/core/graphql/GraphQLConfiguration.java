package io.teaql.core.graphql;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.teaql.core.DataConfigProperties;

@Configuration
public class GraphQLConfiguration {

    @Bean
    public GraphQLService graphqlService(DataConfigProperties config) {
        return new GraphQLService(config);
    }

    @Bean
    public GraphQLSupport graphqlQuerySupport(BaseQueryContainer[] containers) {
        SimpleGraphQLFactory simpleGraphqlQueryFactory = new SimpleGraphQLFactory();
        if (containers != null) {
            for (BaseQueryContainer container : containers) {
                container.register(simpleGraphqlQueryFactory);
            }
        }
        return simpleGraphqlQueryFactory;
    }
}
