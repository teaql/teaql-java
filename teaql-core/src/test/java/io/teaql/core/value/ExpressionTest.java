package io.teaql.core.value;

import org.junit.Test;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class ExpressionTest {

    static class SimpleExpression implements Expression<Object, String> {
        private String value;
        public SimpleExpression(String value) { this.value = value; }
        @Override public String eval(Object o) { return value; }
    }

    @Test
    public void testApplyAndEval() {
        Expression<Object, String> expr = new SimpleExpression("hello");
        Expression<Object, Integer> lengthExpr = expr.apply(String::length);
        assertEquals(Integer.valueOf(5), lengthExpr.eval(null));
        
        assertNull(expr.$getRoot());
        assertEquals("hello", expr.eval());
        assertEquals("hello", expr.resolve());
    }

    @Test
    public void testOrIfNull() {
        Expression<Object, String> exprNull = new SimpleExpression(null);
        assertEquals("default", exprNull.orIfNull("default"));
        
        Expression<Object, String> exprEmpty = new SimpleExpression("");
		assertEquals("", exprEmpty.orIfNull("default"));
        
        Expression<Object, String> exprVal = new SimpleExpression("val");
        assertEquals("val", exprVal.orIfNull("default"));
    }

    @Test
    public void testOrElseThrow() throws Throwable {
        Expression<Object, String> exprVal = new SimpleExpression("val");
        assertEquals("val", exprVal.orElseThrow());
        assertEquals("val", exprVal.orElseThrow(() -> new RuntimeException("err")));
    }

    @Test(expected = NoSuchElementException.class)
    public void testOrElseThrowException() {
        Expression<Object, String> exprNull = new SimpleExpression(null);
        exprNull.orElseThrow();
    }

    @Test(expected = RuntimeException.class)
    public void testOrElseThrowSupplier() throws Throwable {
        Expression<Object, String> exprNull = new SimpleExpression(null);
        exprNull.orElseThrow(() -> new RuntimeException("err"));
    }

    @Test
    public void testStatusMethods() {
        Expression<Object, String> exprNull = new SimpleExpression(null);
        assertTrue(exprNull.isNull());
        assertFalse(exprNull.isNotNull());
        assertTrue(exprNull.isEmpty());
        assertFalse(exprNull.isNotEmpty());
        
        Expression<Object, String> exprEmpty = new SimpleExpression("");
        assertFalse(exprEmpty.isNull());
        assertTrue(exprEmpty.isNotNull());
        assertTrue(exprEmpty.isEmpty());
        assertFalse(exprEmpty.isNotEmpty());
        
        Expression<Object, String> exprVal = new SimpleExpression("val");
        assertFalse(exprVal.isNull());
        assertTrue(exprVal.isNotNull());
        assertFalse(exprVal.isEmpty());
        assertTrue(exprVal.isNotEmpty());
    }

    @Test
    public void testWhenMethods() {
        AtomicBoolean flag = new AtomicBoolean(false);
        Expression<Object, String> exprNull = new SimpleExpression(null);
        
        exprNull.whenIsNull(() -> flag.set(true));
        assertTrue(flag.get());
        
        flag.set(false);
        exprNull.whenIsNull(null); // should not NPE
        
        exprNull.whenIsNotNull((Runnable) () -> flag.set(true));
        assertFalse(flag.get());
        
        exprNull.whenIsNotNull((java.util.function.Consumer<String>) v -> flag.set(true));
        assertFalse(flag.get());
        
        exprNull.whenIsEmpty(() -> flag.set(true));
        assertTrue(flag.get());
        
        flag.set(false);
        exprNull.whenNotEmpty((Runnable) () -> flag.set(true));
        assertFalse(flag.get());
        
        exprNull.whenNotEmpty((java.util.function.Consumer<String>) v -> flag.set(true));
        assertFalse(flag.get());
        
        Expression<Object, String> exprVal = new SimpleExpression("val");
        flag.set(false);
        exprVal.whenIsNotNull((Runnable) () -> flag.set(true));
        assertTrue(flag.get());
        
        flag.set(false);
        exprVal.whenIsNotNull((java.util.function.Consumer<String>) v -> {
            assertEquals("val", v);
            flag.set(true);
        });
        assertTrue(flag.get());
        
        flag.set(false);
        exprVal.whenNotEmpty((Runnable) () -> flag.set(true));
        assertTrue(flag.get());
        
        flag.set(false);
        exprVal.whenNotEmpty((java.util.function.Consumer<String>) v -> {
            assertEquals("val", v);
            flag.set(true);
        });
        assertTrue(flag.get());

        // Cover remaining false/null branches
        exprVal.whenIsNull(() -> flag.set(true));
        exprVal.whenIsEmpty(() -> flag.set(true));
        
        exprVal.whenIsNotNull((Runnable) null);
        exprVal.whenIsNotNull((java.util.function.Consumer<String>) null);
        exprVal.whenNotEmpty((Runnable) null);
        exprVal.whenNotEmpty((java.util.function.Consumer<String>) null);
    }
}
