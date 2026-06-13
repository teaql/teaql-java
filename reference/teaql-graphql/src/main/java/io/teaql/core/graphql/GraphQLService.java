package io.teaql.core.graphql;

import io.teaql.core.utils.ResourceUtil;
import io.teaql.core.utils.MapUtil;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.teaql.core.DataConfigProperties;
import io.teaql.core.TQLException;
import io.teaql.core.TeaQLConstants;
import io.teaql.core.UserContext;

public class GraphQLService implements io.teaql.core.GraphQLService {

    GraphQL graphQL;

    public GraphQLService(DataConfigProperties config) {
        SchemaParser schemaParser = new SchemaParser();
        String graphqlSchemaFile = config.getGraphqlSchemaFile();
        RuntimeWiring runtimeWiring =
                newRuntimeWiring()
                        .codeRegistry(
                                GraphQLCodeRegistry.newCodeRegistry()
                                        .defaultDataFetcher(new TeaQLDataFetcherFactory()))
                        .scalar(ExtendedScalars.GraphQLBigDecimal)
                        .scalar(ExtendedScalars.GraphQLLong)
                        .scalar(ExtendedScalars.Date)
                        .scalar(ExtendedScalars.Time)
                        .scalar(ExtendedScalars.LocalTime)
                        .scalar(ExtendedScalars.Json)
                        .build();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        TypeDefinitionRegistry typeDefinitionRegistry =
                schemaParser.parse(ResourceUtil.readUtf8Str(graphqlSchemaFile));
        schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
        GraphQLSchema graphQLSchema =
                schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
        graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }

    @Override
    public Object execute(UserContext ctx, String query) {
        ExecutionResult result =
                graphQL.execute(
                        ExecutionInput.newExecutionInput()
                                .graphQLContext(MapUtil.of(TeaQLConstants.USER_CONTEXT, ctx))
                                .query(query));
        if (result.getErrors() != null && !result.getErrors().isEmpty()) {
            throw new TQLException(result.getErrors().toString());
        }
        return result.getData();
    }
}
