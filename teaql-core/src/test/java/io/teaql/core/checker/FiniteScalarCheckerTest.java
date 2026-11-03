package io.teaql.core.checker;

import io.teaql.core.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Consumer;
import org.junit.Test;
import static org.junit.Assert.*;

public class FiniteScalarCheckerTest {
    static UserContext context() {
        Map<String,Object> attributes = new HashMap<>();
        return (UserContext)Proxy.newProxyInstance(UserContext.class.getClassLoader(), new Class<?>[]{UserContext.class}, (proxy, method, args) -> {
            if (method.getName().equals("businessTime")) return java.time.LocalDateTime.of(2032,2,29,10,15,30);
            if (method.getName().equals("getAttribute")) return attributes.get(args[0]);
            if (method.getName().equals("putAttribute")) { attributes.put((String)args[0],args[1]); return null; }
            if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
                case "equals" -> proxy == args[0]; case "hashCode" -> System.identityHashCode(proxy); default -> "FiniteCheckerContext";
            };
            if (method.isDefault()) return InvocationHandler.invokeDefault(proxy,method,args);
            throw new AssertionError("unexpected provider operation: " + method.getName());
        });
    }
    static final class Probe extends BaseEntity {
        final Map<String,Object> values = new HashMap<>();
        @Override public String typeName() { return "FiniteProbe"; }
        @Override public Object __internalGet(String field) {
            return field.equals("id") || field.equals("version") ? super.__internalGet(field) : values.get(field);
        }
        @Override public void __internalSet(String field,Object value) {
            if (field.equals("id") || field.equals("version")) super.__internalSet(field,value); else values.put(field,value);
        }
    }
    static Checker<Probe> checker(Consumer<Probe> fix) {
        return new Checker<>() {
            public String type() { return "FiniteProbe"; }
            public void checkAndFix(UserContext context, Probe entity, ObjectLocation location) {
                var where = ObjectLocation.hashRoot("value");
                minNumberCheck(context, where, -10, (Number)entity.getProperty("value"));
                if (fix != null) fix.accept(entity);
                maxNumberCheck(context, where, 10, (Number)entity.getProperty("value"));
            }
        };
    }
    @Test public void standaloneRangesReportStructuredNonfiniteTypeInsteadOfBigDecimalFailure() {
        for (Number bad : List.of(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            var context = context(); var row = new Probe(); row.updateProperty("value", bad);
            var error = assertThrows(CheckException.class, () -> checker(null).checkAndFix(context,row));
            assertEquals(1,error.getViolates().size()); var result = error.getViolates().get(0);
            assertEquals(CheckResult.RuleId.INVALID_TYPE,result.getRuleId()); assertEquals("FiniteProbe",result.getRootType());
            assertEquals("value",result.getLocation().modelPath()); assertNull(result.getInputValue());
            for (var profile : JsonFieldNamingProfile.values()) assertEquals("/" + profile.render("value"),result.toWire(profile).instancePath());
        }
    }
    @Test public void aLaterCustomerRepairIsJudgedOnItsFinalValue() {
        var row = new Probe(); row.updateProperty("value",Double.NaN);
        checker(e -> e.updateProperty("value",7.5d)).checkAndFix(context(),row);
        assertEquals(7.5d,(Double)row.getProperty("value"),0);
    }
    @Test public void rejectedInvocationDoesNotPolluteTheNextStandaloneCheck() {
        var row = new Probe(); row.updateProperty("value",Double.NaN); var context = context();
        assertThrows(CheckException.class,() -> checker(null).checkAndFix(context,row));
        row.updateProperty("value",5.0d); checker(null).checkAndFix(context,row);
        assertEquals(5.0d,(Double)row.getProperty("value"),0);
    }
    @Test public void rangeHelpersWithoutAnInvocationStillReportInvalidType() {
        var context = context();
        checker(null).minNumberCheck(context,ObjectLocation.hashRoot("value"),0,Double.NaN);
        var results = (List<?>)context.getAttribute(Checker.TEAQL_DATA_CHECK_RESULT);
        assertEquals(1,results.size()); assertEquals(CheckResult.RuleId.INVALID_TYPE,((CheckResult)results.get(0)).getRuleId());
    }

    @Test public void standaloneChecksLoadedValuesWithoutAllocatingAMutationLedger() {
        var row = new Probe();
        row.__internalHydrate("value",Double.POSITIVE_INFINITY,BaseEntity.loadedPropertyIndex(Probe.class,"value"));
        assertFalse(row.__internalHasMutationLedger());
        var error = assertThrows(CheckException.class,() -> checker(null).checkAndFix(context(),row));
        assertEquals(1,error.getViolates().size());
        assertEquals("value",error.getViolates().get(0).getLocation().modelPath());
        assertFalse(row.__internalHasMutationLedger());
    }

    @Test public void standaloneFindsLateChildThroughLoadedRelationsAndCyclesWithoutRuntimeCapabilities() {
        var root = new Probe(); var first = new Probe(); var second = new Probe();
        first.updateProperty("value",1.0d); second.updateProperty("value",Double.NEGATIVE_INFINITY);
        root.__internalHydrate("children",List.of(first,second),BaseEntity.loadedPropertyIndex(Probe.class,"children"));
        second.__internalHydrate("owner",root,BaseEntity.loadedPropertyIndex(Probe.class,"owner"));
        var error = assertThrows(CheckException.class,() -> checker(null).checkAndFix(context(),root));
        assertEquals(1,error.getViolates().size());
        assertEquals("children[1].value",error.getViolates().get(0).getLocation().modelPath());
        assertFalse(root.__internalHasMutationLedger());
        second.updateProperty("value",2.0d); checker(null).checkAndFix(context(),root);
    }
}
