package io.teaql.utils.spring;

import io.teaql.core.utils.ClassUtil;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.HashSet;
import java.util.Set;

public class SpringClassUtil {

    public static Set<Class<?>> scanPackageBySuper(String packageName, Class<?> superClass) {
        Set<Class<?>> classes = new HashSet<>();
        try {
            ClassPathScanningCandidateComponentProvider provider =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(org.springframework.beans.factory.annotation.AnnotatedBeanDefinition beanDefinition) {
                        return true;
                    }
                };
            provider.addIncludeFilter(new AssignableTypeFilter(superClass));
            for (BeanDefinition beanDef : provider.findCandidateComponents(packageName)) {
                Class<?> clazz = ClassUtil.loadClass(beanDef.getBeanClassName());
                if (clazz != null) {
                    classes.add(clazz);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Scan package failed", e);
        }
        return classes;
    }
}
