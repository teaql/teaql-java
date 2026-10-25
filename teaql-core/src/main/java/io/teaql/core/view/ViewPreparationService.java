package io.teaql.core.view;

import io.teaql.core.UserContext;

/** Explicit final presentation boundary, after required business transactions complete. */
public interface ViewPreparationService {
    String CAPABILITY="view.v1";
    PreparedView prepare(UserContext context,Object result,ViewOptions options);
}
