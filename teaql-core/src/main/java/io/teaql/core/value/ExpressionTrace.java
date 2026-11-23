package io.teaql.core.value;

import io.teaql.core.checker.ObjectLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Value-free native E call trace. Canonical identity never comes from parsing display text. */
public final class ExpressionTrace {
    private record Step(String call, ObjectLocation location, boolean traverses) { }
    private final String root;
    private final List<Step> steps;
    private final ObjectLocation location;

    private ExpressionTrace(String root, List<Step> steps, ObjectLocation location) {
        this.root = Objects.requireNonNull(root);
        this.steps = List.copyOf(steps);
        this.location = location;
    }

    public static ExpressionTrace root(String expression) {
        return new ExpressionTrace(expression, List.of(), null);
    }

    public ExpressionTrace member(String property, String method) {
        return append(method + "()", location == null ? ObjectLocation.hashRoot(property) : location.member(property), true);
    }

    public ExpressionTrace element(int index, boolean first) {
        return append(first ? "first()" : "get(" + index + ")",
                location == null ? ObjectLocation.arrayRoot(index) : location.element(index), true);
    }

    public ExpressionTrace operation(String call) { return append(call, location, false); }

    private ExpressionTrace append(String call, ObjectLocation target, boolean traverses) {
        List<Step> next = new ArrayList<>(steps);
        next.add(new Step(call, target, traverses));
        return new ExpressionTrace(root, next, target);
    }

    public ObjectLocation location() { return location; }

    public String render(ObjectLocation missing) {
        StringBuilder result = new StringBuilder(root);
        boolean marked = false;
        for (Step step : steps) {
            result.append('.').append(step.call());
            if (!marked && missing != null && step.traverses()
                    && step.location().segments().equals(missing.segments())) {
                result.append("<broken>");
                marked = true;
            }
        }
        return result.toString();
    }
}
