package io.teaql.core.translation;

public interface Translator {
    Translator NOOP = req -> null;

    TranslationResponse translate(TranslationRequest req);
}
