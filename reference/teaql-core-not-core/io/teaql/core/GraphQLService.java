package io.teaql.core;

public interface GraphQLService {
    Object execute(UserContext ctx, String query);
}
