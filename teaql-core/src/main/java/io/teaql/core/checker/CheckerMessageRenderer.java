package io.teaql.core.checker;

import io.teaql.core.i18n.CheckerMessageCatalog;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Trusted application SPI. Input has projected scalar arguments, never entities or a mutable Context. */
@FunctionalInterface
public interface CheckerMessageRenderer {
    String render(Request request);

    record Request(CheckerPresentation presentation, CheckerMessageCatalog catalog,
                   CheckerMessageCatalog.Selection selection, Map<String, Object> arguments,
                   Consumer<String> warningSink) {
        public Request {
            Objects.requireNonNull(presentation); Objects.requireNonNull(catalog);
            Objects.requireNonNull(selection); arguments = Map.copyOf(arguments); Objects.requireNonNull(warningSink);
        }
    }

    CheckerMessageRenderer STANDARD = request -> request.presentation().render(request.catalog(),
            request.selection(), request.arguments(), request.warningSink());
}
