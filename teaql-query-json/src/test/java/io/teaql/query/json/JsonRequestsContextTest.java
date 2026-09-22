package io.teaql.query.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.teaql.core.BaseEntity;
import io.teaql.core.BaseRequest;
import io.teaql.core.Entity;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import java.lang.reflect.Proxy;
import org.junit.Test;

public class JsonRequestsContextTest {

    @Test
    public void alternatesRootOrderAndRelationParsingBetweenContextModels() {
        EntityMetaFactory previous = EntityMetaFactory.get();
        try {
            EntityMetaFactory.registerGlobal(metadata(
                    PoisonOrder.class, PoisonCustomer.class, "poisonCode", "poisonName"));
            SimpleEntityMetaFactory alpha = metadata(
                    AlphaOrder.class, AlphaCustomer.class, "alphaCode", "alphaName");
            SimpleEntityMetaFactory beta = metadata(
                    BetaOrder.class, BetaCustomer.class, "betaCode", "betaName");

            OrderRequest alphaRequest = JsonRequests.findWithJson(
                    context(alpha), new OrderRequest(),
                    "{\"alphaCode\":\"A\",\"customer.alphaName\":\"Ada\","
                            + "\"_orderBy\":[{\"field\":\"alphaCode\",\"useAsc\":true}]}");
            assertParsedWithoutGlobalFallback(alphaRequest);

            OrderRequest betaRequest = JsonRequests.findWithJson(
                    context(beta), new OrderRequest(),
                    "{\"betaCode\":\"B\",\"customer.betaName\":\"Bob\","
                            + "\"_orderBy\":[{\"field\":\"betaCode\",\"useAsc\":false}]}");
            assertParsedWithoutGlobalFallback(betaRequest);

            OrderRequest crossModel = JsonRequests.findWithJson(
                    context(alpha), new OrderRequest(), "{\"betaCode\":\"wrong model\"}");
            assertEquals(1, DynamicSearchHelper.warningsOf(crossModel).size());
            assertTrue(crossModel.getSearchCriteria() == null);
        } finally {
            EntityMetaFactory.registerGlobal(previous);
        }
    }

    private static void assertParsedWithoutGlobalFallback(OrderRequest request) {
        assertNotNull(request.getSearchCriteria());
        assertEquals(1, request.getOrderBy().getOrderBys().size());
        assertTrue(DynamicSearchHelper.warningsOf(request).isEmpty());
    }

    private static UserContext context(EntityMetaFactory metadata) {
        return (UserContext) Proxy.newProxyInstance(
                JsonRequestsContextTest.class.getClassLoader(),
                new Class<?>[] {UserContext.class},
                (proxy, method, args) -> {
                    if ("capability".equals(method.getName())
                            && args != null
                            && args.length == 1
                            && args[0] == EntityMetaFactory.class) {
                        return metadata;
                    }
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "MetadataOnlyUserContext";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new AssertionError(
                                    "Unexpected Object method: " + method.getName());
                        };
                    }
                    throw new AssertionError("Unexpected UserContext call: " + method.getName());
                });
    }

    private static SimpleEntityMetaFactory metadata(
            Class<? extends Entity> orderType,
            Class<? extends Entity> customerType,
            String orderField,
            String customerField) {
        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        EntityDescriptor customer = descriptor("Customer", customerType);
        customer.addSimpleProperty(customerField, String.class);
        metadata.register(customer);

        EntityDescriptor order = descriptor("Order", orderType);
        order.addSimpleProperty(orderField, String.class);
        order.addObjectProperty(
                metadata, "customer", "Customer", "orders", customerType);
        metadata.register(order);
        return metadata;
    }

    private static EntityDescriptor descriptor(
            String type, Class<? extends Entity> targetType) {
        EntityDescriptor descriptor = new EntityDescriptor();
        descriptor.setType(type);
        descriptor.setTargetType(targetType);
        descriptor.addSimpleProperty("id", Long.class);
        descriptor.addSimpleProperty("version", Long.class);
        return descriptor;
    }

    private static final class OrderRequest extends BaseRequest<Entity> {
        private OrderRequest() { super(Entity.class); }
        @Override public String getTypeName() { return "Order"; }
    }

    public static final class AlphaOrder extends BaseEntity {
        @Override public String typeName() { return "Order"; }
    }
    public static final class BetaOrder extends BaseEntity {
        @Override public String typeName() { return "Order"; }
    }
    public static final class PoisonOrder extends BaseEntity {
        @Override public String typeName() { return "Order"; }
    }
    public static final class AlphaCustomer extends BaseEntity {
        @Override public String typeName() { return "Customer"; }
    }
    public static final class BetaCustomer extends BaseEntity {
        @Override public String typeName() { return "Customer"; }
    }
    public static final class PoisonCustomer extends BaseEntity {
        @Override public String typeName() { return "Customer"; }
    }
}
