package io.teaql.utils.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SpringUtil implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        setContext(context);
    }

    private static void setContext(ApplicationContext context) {
        applicationContext = context;
    }

    public static <T> T getBean(Class<T> clazz) {
        return applicationContext != null ? applicationContext.getBean(clazz) : null;
    }

    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        return applicationContext != null ? applicationContext.getBeansOfType(clazz) : java.util.Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        return applicationContext != null ? (T) applicationContext.getBean(name) : null;
    }
}
