package io.teaql.core;

import java.util.Collections;
import java.util.List;

/**
 * @author Jackytin
 * <p>the related properties
 */
public interface PropertyAware {

    default List<String> properties(UserContext ctx) {
        return Collections.emptyList();
    }
}
