package io.teaql.core;

import java.util.List;

import io.teaql.core.utils.ObjectUtil;

import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;

public interface TQLResolver {
    default Repository resolveRepository(String type) {
        List<Repository> beans = getBeans(Repository.class);
        if (ObjectUtil.isNotEmpty(beans)) {
            for (Repository bean : beans) {
                EntityDescriptor entityDescriptor = bean.getEntityDescriptor();
                if (entityDescriptor.getType().equals(type)) {
                    return bean;
                }
            }
        }
        return null;
    }

    default EntityDescriptor resolveEntityDescriptor(String type) {
        EntityMetaFactory bean = getBean(EntityMetaFactory.class);
        if (bean != null) {
            return bean.resolveEntityDescriptor(type);
        }
        return null;
    }

    <T> T getBean(Class<T> clazz);

    <T> List<T> getBeans(Class<T> clazz);

    <T> T getBean(String name);
}
