package io.teaql.core.value;

import io.teaql.core.BaseEntity;
import org.junit.Test;

import static org.junit.Assert.*;

public class LoadedPropertyExpressionTest {
    static class Subject extends BaseEntity {
        private String name;
        @Override public String typeName() { return "Subject"; }
        String getName() { return name; }
        void loadName(String value) { name = value; markPropertyLoaded("name"); }
    }

    @Test
    public void distinguishesValueLoadedNullAndNotLoaded() {
        Subject value = new Subject(); value.updateId(1L); value.loadName("TeaQL");
        Expression<Subject, Subject> root = new ValueExpression<>(value);
        Expression<Subject, String> expression = new LoadedPropertyExpression<>(root, "name", Subject::getName);
        assertEquals("TeaQL", expression.eval());
        assertEquals("TeaQL", expression.orIfNull("fallback"));

        Subject loadedNull = new Subject(); loadedNull.updateId(2L); loadedNull.loadName(null);
        Expression<Subject, String> nullExpression = new LoadedPropertyExpression<>(
                new ValueExpression<>(loadedNull), "name", Subject::getName);
        assertNull(nullExpression.eval());
        assertEquals("fallback", nullExpression.orIfNull("fallback"));

        Subject partial = new Subject(); partial.updateId(3L);
        Expression<Subject, String> missing = new LoadedPropertyExpression<>(
                new ValueExpression<>(partial), "name", Subject::getName);
        TeaQLNotLoadedException error = assertThrows(TeaQLNotLoadedException.class, missing::eval);
        assertEquals("name", error.getAccessPath());
        assertThrows(TeaQLNotLoadedException.class, () -> missing.orIfNull("hidden"));
    }

    @Test
    public void fallbackDoesNotTreatEmptyStringAsNull() {
        Subject subject = new Subject(); subject.loadName("");
        Expression<Subject, String> expression = new LoadedPropertyExpression<>(
                new ValueExpression<>(subject), "name", Subject::getName);
        assertEquals("", expression.orIfNull("fallback"));
    }
}
