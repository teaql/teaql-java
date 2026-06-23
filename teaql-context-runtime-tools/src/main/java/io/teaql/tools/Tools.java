package io.teaql.tools;

import java.util.Set;

public interface Tools {

    <T> T get(Class<T> toolType);

    boolean has(Class<?> toolType);

    Set<ToolDescriptor> descriptors();
}
