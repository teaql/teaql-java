package io.teaql.core;

import java.util.List;

import io.teaql.core.checker.CheckResult;

public interface NaturalLanguageTranslator {
    List<CheckResult> translateError(Entity pEntity, List<CheckResult> errors);
}
