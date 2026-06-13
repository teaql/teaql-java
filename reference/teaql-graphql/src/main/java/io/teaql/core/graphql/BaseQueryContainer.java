package io.teaql.core.graphql;

import java.lang.reflect.Method;
import java.util.List;

import io.teaql.core.utils.ClassUtil;
import io.teaql.core.utils.ObjUtil;

import io.teaql.core.BaseRequest;
import io.teaql.core.UserContext;

public abstract class BaseQueryContainer {
    protected abstract String type();

    public final void register(GraphQLSupport factory) {
        if (factory == null) {
            return;
        }
        Class<? extends BaseQueryContainer> thisClass = this.getClass();
        List<Method> candidates =
                ClassUtil.getPublicMethods(
                        thisClass,
                        m -> {
                            Class<?>[] parameterTypes = m.getParameterTypes();
                            if (ObjUtil.isEmpty(parameterTypes)) {
                                return false;
                            }
                            Class<?> parameterType = parameterTypes[0];
                            if (!ClassUtil.isAssignable(UserContext.class, parameterType)) {
                                return false;
                            }

                            Class<?> returnType = m.getReturnType();
                            if (!ClassUtil.isAssignable(BaseRequest.class, returnType)) {
                                return false;
                            }
                            return true;
                        });
        for (Method candidate : candidates) {
            factory.register(
                    new ReflectGraphQLFieldQuery(type() + ":" + candidate.getName(), this, candidate));
        }
    }
}
