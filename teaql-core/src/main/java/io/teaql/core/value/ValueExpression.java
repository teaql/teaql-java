package io.teaql.core.value;

import io.teaql.core.checker.JsonFieldNamingProfile;
import java.util.Objects;

public class ValueExpression<T> implements Expression<T, T> {

    private final T value;
    private final JsonFieldNamingProfile profile;
    private final ExpressionTrace trace;

    public ValueExpression(T value) {
        this(value, JsonFieldNamingProfile.CAMEL_CASE);
    }

    public ValueExpression(T value, JsonFieldNamingProfile profile) {
        this(value, profile, "E.entity(<root>)");
    }

    public ValueExpression(T value, JsonFieldNamingProfile profile, String rootExpression) {
        this.value = value;
        this.profile = Objects.requireNonNull(profile);
        this.trace = ExpressionTrace.root(rootExpression);
    }

    @Override
    public T eval(T pT) {
        return pT;
    }

    @Override
    public T $getRoot() {
        return value;
    }

    @Override public JsonFieldNamingProfile $jsonFieldNaming() { return profile; }
    @Override public ExpressionTrace $trace() { return trace; }
}
