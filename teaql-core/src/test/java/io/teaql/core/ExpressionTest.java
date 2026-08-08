package io.teaql.core;

import org.junit.Test;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExpressionTest {

    static class DummyExpression implements Expression {
        @Override
        public java.util.List<String> properties(UserContext ctx) {
            return null;
        }
    }

    @Test
    public void testPrivateMethods() throws Exception {
        DummyExpression expr = new DummyExpression();
        
        Method nextPropertyKeyMethod = Expression.class.getDeclaredMethod("nextPropertyKey", Map.class, String.class);
        nextPropertyKeyMethod.setAccessible(true);
        
        Map<String, Object> params = new HashMap<>();
        String result = (String) nextPropertyKeyMethod.invoke(expr, params, "prop");
        assertEquals("prop", result);
        
        params.put("prop", "val1");
        result = (String) nextPropertyKeyMethod.invoke(expr, params, "prop");
        assertEquals("prop0", result);
        
        params.put("prop0", "val2");
        result = (String) nextPropertyKeyMethod.invoke(expr, params, "prop");
        assertEquals("prop1", result);
        
        // test genNextKey digit loop
        Method genNextKeyMethod = Expression.class.getDeclaredMethod("genNextKey", String.class);
        genNextKeyMethod.setAccessible(true);
        
        assertEquals("prop0", genNextKeyMethod.invoke(expr, "prop"));
        assertEquals("prop1", genNextKeyMethod.invoke(expr, "prop0"));
        assertEquals("prop2", genNextKeyMethod.invoke(expr, "prop1"));
    }
}
