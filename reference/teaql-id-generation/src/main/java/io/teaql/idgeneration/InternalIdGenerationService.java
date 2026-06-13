package io.teaql.idgeneration;

import io.teaql.core.Entity;
import io.teaql.core.UserContext;

public interface InternalIdGenerationService {
    Long generateId(UserContext ctx, Entity entity);
}
