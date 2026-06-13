package io.teaql.core;

public interface InternalIdGenerationService {
    Long generateId(UserContext ctx, Entity entity);
}
