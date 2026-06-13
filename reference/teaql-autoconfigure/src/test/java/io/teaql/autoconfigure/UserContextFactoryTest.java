package io.teaql.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import java.lang.reflect.Method;

import io.teaql.core.DataConfigProperties;
import io.teaql.core.UserContext;
import io.teaql.core.DefaultUserContext;

public class UserContextFactoryTest {

    @Test
    void testDefaultUserContextFactoryCreation() {
        DataConfigProperties config = new DataConfigProperties();
        config.setContextClass(DefaultUserContext.class);

        UserContextFactory factory = new DefaultUserContextFactory(config);
        UserContext ctx = factory.create(new Object());

        assertNotNull(ctx);
        assertEquals(DefaultUserContext.class, ctx.getClass());
    }

    @Test
    void testTQLContextResolverSupportsParameter() throws Exception {
        UserContextFactory factory = new DefaultUserContextFactory(new DataConfigProperties());
        TQLAutoConfiguration.TQLContextResolver resolver = new TQLAutoConfiguration.TQLContextResolver(factory);

        Method method = DummyController.class.getMethod("dummyMethod", UserContext.class, UserContext.class, String.class);

        // Parameter 0: plain UserContext (ctxPlain) -> should be supported
        MethodParameter paramPlain = new MethodParameter(method, 0);
        assertTrue(resolver.supportsParameter(paramPlain));

        // Parameter 1: @TQLContext UserContext (ctxAnnotated) -> should be supported
        MethodParameter paramAnnotated = new MethodParameter(method, 1);
        assertTrue(resolver.supportsParameter(paramAnnotated));

        // Parameter 2: String (other) -> should NOT be supported
        MethodParameter paramOther = new MethodParameter(method, 2);
        assertFalse(resolver.supportsParameter(paramOther));
    }

    @Test
    void testTQLReactiveContextResolverSupportsParameter() throws Exception {
        UserContextFactory factory = new DefaultUserContextFactory(new DataConfigProperties());
        TQLAutoConfiguration.TQLReactiveContextResolver resolver = new TQLAutoConfiguration.TQLReactiveContextResolver(factory);

        Method method = DummyController.class.getMethod("dummyMethod", UserContext.class, UserContext.class, String.class);

        // Parameter 0: plain UserContext (ctxPlain) -> should be supported
        MethodParameter paramPlain = new MethodParameter(method, 0);
        assertTrue(resolver.supportsParameter(paramPlain));

        // Parameter 1: @TQLContext UserContext (ctxAnnotated) -> should be supported
        MethodParameter paramAnnotated = new MethodParameter(method, 1);
        assertTrue(resolver.supportsParameter(paramAnnotated));

        // Parameter 2: String (other) -> should NOT be supported
        MethodParameter paramOther = new MethodParameter(method, 2);
        assertFalse(resolver.supportsParameter(paramOther));
    }

    static class DummyController {
        public void dummyMethod(
                UserContext ctxPlain,
                @TQLContext UserContext ctxAnnotated,
                String other) {
        }
    }
}
