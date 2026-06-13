package io.teaql.core;

import java.util.List;
import java.util.Objects;

public class SimpleNamedExpression implements Expression {
    String name;
    Expression expression;

    public SimpleNamedExpression(String name, Expression expression) {
        if (expression == null) {
            throw new TeaQLRuntimeException("SimpleNamedExpression expression cannot be null");
        }
        this.name = name;
        this.expression = expression;
    }

    public SimpleNamedExpression(String propertyName) {
        this(propertyName, new PropertyReference(propertyName));
    }

    public String name() {
        return this.name;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public List<String> properties(UserContext ctx) {
        return expression.properties(ctx);
    }

    @Override
    public boolean equals(Object pO) {
        if (this == pO) return true;
        if (!(pO instanceof SimpleNamedExpression that)) return false;
        return Objects.equals(name, that.name) && Objects.equals(getExpression(), that.getExpression());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, getExpression());
    }
}
