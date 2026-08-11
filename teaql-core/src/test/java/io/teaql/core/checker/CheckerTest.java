package io.teaql.core.checker;

import io.teaql.core.BaseEntity;
import io.teaql.core.EntityStatus;
import io.teaql.core.UserContext;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class CheckerTest {

    static class DummyEntity extends BaseEntity {
        @Override
        public String typeName() {
            return "dummy";
        }
    }

    static UserContext createDummyContext() {
        return (UserContext) java.lang.reflect.Proxy.newProxyInstance(
                UserContext.class.getClassLoader(),
                new Class[]{UserContext.class},
                new java.lang.reflect.InvocationHandler() {
                    private Map<String, Object> ctx = new HashMap<>();
                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
                        if ("putAttribute".equals(method.getName()) && args != null && args.length == 2) {
                            ctx.put((String) args[0], args[1]);
                            return null;
                        }
                        if ("getAttribute".equals(method.getName()) && args != null && args.length == 1) {
                            return ctx.get((String) args[0]);
                        }
                        if (method.getReturnType().isPrimitive()) {
                            if (method.getReturnType() == boolean.class) return false;
                            if (method.getReturnType() == int.class) return 0;
                        }
                        return null;
                    }
                });
    }

    static class DummyChecker implements Checker<DummyEntity> {
        @Override
        public String type() { return "dummy"; }
        @Override
        public void checkAndFix(UserContext ctx, DummyEntity entity, ObjectLocation location) { }
    }

    @Test
    public void testMarkAsCheckedAndNeedCheck() {
        DummyChecker checker = new DummyChecker();
        UserContext ctx = createDummyContext();
        DummyEntity entity = new DummyEntity();
        
        // Before marking, should need check
        assertTrue(checker.needCheck(ctx, entity));
        
        // Mark as checked twice to cover the list != null branch
        checker.markAsChecked(ctx, entity);
        checker.markAsChecked(ctx, entity);
        
        // After marking, should not need check
        assertFalse(checker.needCheck(ctx, entity));
        
        // Check another entity when list is not null but does not contain
        DummyEntity entity2 = new DummyEntity();
        entity2.updateId(2L);
        assertTrue(checker.needCheck(ctx, entity2));
        
        // Check null
        assertFalse(checker.needCheck(ctx, null));
        
        // Check REFER status
        DummyEntity referEntity = new DummyEntity();
        referEntity.set$status(EntityStatus.REFER);
        assertFalse(checker.needCheck(ctx, referEntity));
    }
    
    @Test
    public void testNewLocation() {
        DummyChecker checker = new DummyChecker();
        ObjectLocation loc1 = checker.newLocation(null, "member1");
        assertNotNull(loc1);
        
        ObjectLocation loc2 = checker.newLocation(loc1, "member2");
        assertNotNull(loc2);
        
        ObjectLocation loc3 = checker.newLocation(loc1, "member3", 5);
        assertNotNull(loc3);
    }
    
    @Test
    public void testRequiredCheck() {
        DummyChecker checker = new DummyChecker();
        UserContext ctx = createDummyContext();
        ObjectLocation loc = ObjectLocation.hashRoot("test");
        
        checker.requiredCheck(ctx, loc, "val");
        assertNull(ctx.getAttribute(Checker.TEAQL_DATA_CHECK_RESULT));
        
        checker.requiredCheck(ctx, loc, null);
        List<?> results = (List<?>) ctx.getAttribute(Checker.TEAQL_DATA_CHECK_RESULT);
        assertNotNull(results);
        assertEquals(1, results.size());
    }
    
    @Test
    public void testNumberChecks() {
        DummyChecker checker = new DummyChecker();
        UserContext ctx = createDummyContext();
        ObjectLocation loc = ObjectLocation.hashRoot("test");
        
        // Min check
        checker.minNumberCheck(ctx, loc, 5, 10);
        assertNull(ctx.getAttribute(Checker.TEAQL_DATA_CHECK_RESULT));
        checker.minNumberCheck(ctx, loc, 5, 3);
        List<?> results = (List<?>) ctx.getAttribute(Checker.TEAQL_DATA_CHECK_RESULT);
        assertNotNull(results);
        assertEquals(1, results.size());
        
        // Max check
        checker.maxNumberCheck(ctx, loc, 10, 5);
        assertEquals(1, results.size());
        checker.maxNumberCheck(ctx, loc, 10, 15);
        assertEquals(2, results.size());
    }
    
    @Test
    public void testStringChecks() {
        DummyChecker checker = new DummyChecker();
        UserContext ctx = createDummyContext();
        ObjectLocation loc = ObjectLocation.hashRoot("test");
        
        // Min check
        checker.minStringCheck(ctx, loc, 3, "hello");
        assertNull(ctx.getAttribute(Checker.TEAQL_DATA_CHECK_RESULT));
        checker.minStringCheck(ctx, loc, 5, "hi");
        List<?> results = (List<?>) ctx.getAttribute(Checker.TEAQL_DATA_CHECK_RESULT);
        assertNotNull(results);
        assertEquals(1, results.size());
        
        // Max check
        checker.maxStringCheck(ctx, loc, 5, "hi");
        assertEquals(1, results.size());
        checker.maxStringCheck(ctx, loc, 3, "hello");
        assertEquals(2, results.size());
    }

    @Test
    public void testDateTimeChecks() {
        DummyChecker checker = new DummyChecker();
        UserContext ctx = createDummyContext();
        ObjectLocation loc = ObjectLocation.hashRoot("test");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past = now.minusDays(1);
        LocalDateTime future = now.plusDays(1);
        
        // Min check
        checker.minDateTimeCheck(ctx, loc, past, now);
        assertNull(ctx.getAttribute(Checker.TEAQL_DATA_CHECK_RESULT));
        checker.minDateTimeCheck(ctx, loc, future, now);
        List<?> results = (List<?>) ctx.getAttribute(Checker.TEAQL_DATA_CHECK_RESULT);
        assertNotNull(results);
        assertEquals(1, results.size());
        
        // Max check
        checker.maxDateTimeCheck(ctx, loc, future, now);
        assertEquals(1, results.size());
        checker.maxDateTimeCheck(ctx, loc, past, now);
        assertEquals(2, results.size());
    }

    @Test
    public void testCheckAndFixDefault() {
        // Just calling to cover the default interface method
        DummyChecker checker = new DummyChecker();
        checker.checkAndFix(createDummyContext(), new DummyEntity());
    }
}
