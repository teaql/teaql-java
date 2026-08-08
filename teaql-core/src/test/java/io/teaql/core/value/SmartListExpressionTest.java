package io.teaql.core.value;

import io.teaql.core.BaseEntity;
import io.teaql.core.SmartList;
import org.junit.Test;

import static org.junit.Assert.*;

public class SmartListExpressionTest {

    static class DummyEntity extends BaseEntity {
        public DummyEntity(Long id) { updateId(id); }
        @Override public String typeName() { return "dummy"; }
    }

    static class DummyExpression<T, E> implements Expression<T, E> {
        private E value;
        public DummyExpression(E value) { this.value = value; }
        @Override public E eval(T ctx) { return value; }
    }

    @Test
    public void testMethods() {
        SmartList<DummyEntity> list = new SmartList<>();
        DummyEntity e1 = new DummyEntity(1L);
        DummyEntity e2 = new DummyEntity(2L);
        list.add(e1);
        list.add(e2);

        Expression<Object, SmartList<DummyEntity>> inner = new DummyExpression<>(list);
        SmartListExpression<Object, SmartList<DummyEntity>, DummyEntity> expr = new SmartListExpression<>(inner);

        // size
        Expression<Object, Integer> sizeExpr = expr.size();
        assertEquals(Integer.valueOf(2), sizeExpr.eval(null));

        // first
        Expression<Object, DummyEntity> firstExpr = expr.first();
        assertEquals(e1, firstExpr.eval(null));

        // get
        Expression<Object, DummyEntity> get0 = expr.get(0);
        assertEquals(e1, get0.eval(null));
        
        Expression<Object, DummyEntity> get1 = expr.get(1);
        assertEquals(e2, get1.eval(null));
        
        Expression<Object, DummyEntity> getNeg = expr.get(-1);
        assertNull(getNeg.eval(null));
        
        Expression<Object, DummyEntity> getOut = expr.get(2);
        assertNull(getOut.eval(null));
    }
    
    @Test
    public void testConstructorWithFunction() {
        Expression<Object, String> inner = new DummyExpression<>("test");
        SmartListExpression<Object, String, DummyEntity> expr = new SmartListExpression<>(inner, str -> {
            SmartList<DummyEntity> list = new SmartList<>();
            if ("test".equals(str)) {
                list.add(new DummyEntity(10L));
            }
            return list;
        });
        
        assertEquals(Integer.valueOf(1), expr.size().eval(null));
    }
}
