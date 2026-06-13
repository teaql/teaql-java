package io.teaql.core;

public interface InternalIdGenerator {

    Long generateId(Entity baseEntity);
}
