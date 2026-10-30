package io.teaql.core.view;

import io.teaql.core.UserContext;
import io.teaql.core.reference.ContextBoundDocumentService;

/** Explicit final presentation boundary, after required business transactions complete. */
public interface ViewPreparationService {
    String CAPABILITY="view.v1";
    PreparedView prepare(UserContext context,Object result,ViewOptions options);
    default DocumentView openDocumentForView(
            UserContext context, ContextBoundDocumentService.OpenRequest request) {
        throw new ViewPreparationException(ViewPreparationException.Code.VIEW_CAPABILITY_MISSING,
                ViewOptions.BusinessCompletion.QUERY);
    }
    default AcceptedDocumentView acceptDocumentForView(
            UserContext context, ContextBoundDocumentService.AcceptRequest request) {
        throw new ViewPreparationException(ViewPreparationException.Code.VIEW_CAPABILITY_MISSING,
                ViewOptions.BusinessCompletion.QUERY);
    }
}
