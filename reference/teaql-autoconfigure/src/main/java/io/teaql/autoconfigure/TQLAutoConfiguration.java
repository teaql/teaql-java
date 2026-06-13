package io.teaql.autoconfigure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.teaql.core.utils.CacheUtil;
import io.teaql.core.utils.TimedCache;
import io.teaql.core.utils.BeanUtil;
import io.teaql.core.utils.Base64;
import io.teaql.core.utils.CollStreamUtil;
import io.teaql.core.utils.ObjectUtil;
import io.teaql.core.utils.ReflectUtil;
import io.teaql.core.utils.SpringUtil;
import io.teaql.core.utils.JSONUtil;

import io.teaql.core.DataConfigProperties;
import io.teaql.core.DataStore;
import io.teaql.core.Entity;
import io.teaql.core.RemoteInput;
import io.teaql.core.RequestHolder;
import io.teaql.core.ResponseHolder;
import io.teaql.core.SmartList;
import io.teaql.core.TQLResolver;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.internal.GLobalResolver;
import io.teaql.core.jackson.TeaQLModule;
import io.teaql.core.lock.LockService;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.translation.Translator;
import io.teaql.core.web.UITemplateRender;
import io.teaql.core.UserContextInitializer;
import io.teaql.autoconfigure.web.BlobObjectMessageConverter;
import io.teaql.autoconfigure.web.MultiReadFilter;
import io.teaql.autoconfigure.web.ServletUserContextInitializer;
import io.teaql.autoconfigure.lock.LocalLockService;
import io.teaql.autoconfigure.lock.RedisLockService;
import io.teaql.autoconfigure.redis.RedisStore;
import jakarta.servlet.Filter;
import reactor.core.publisher.Mono;

@Configuration
public class TQLAutoConfiguration {

    @Bean("checkers")
    public Map<String, Checker> checkers(List<Checker> checkers) {
        return CollStreamUtil.toIdentityMap(checkers, Checker::type);
    }

    @Bean
    @ConfigurationProperties(prefix = "teaql")
    public DataConfigProperties dataConfig() {
        return new DataConfigProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public EntityMetaFactory entityMetaFactory() {
        return new SimpleEntityMetaFactory();
    }

    @Bean
    @ConditionalOnMissingBean(Translator.class)
    public Translator translator() {
        return Translator.NOOP;
    }

    @Bean
    @ConditionalOnMissingBean(name = "templateRender")
    public UITemplateRender templateRender() {
        return new UITemplateRender();
    }


    @ConditionalOnClass(name = "org.redisson.api.RedissonClient")
    @Configuration
    public static class RedissonConfiguration {
        @Bean
        @ConditionalOnBean(RedissonClient.class)
        public RedissonAutoConfigurationCustomizer codec() {
            return config -> {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.enableDefaultTyping(
                        ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);
                config.setCodec(new JsonJacksonCodec(objectMapper));
            };
        }

        @Bean
        @ConditionalOnMissingBean(DataStore.class)
        @ConditionalOnBean(RedissonClient.class)
        public DataStore redisDataStore(RedissonClient redissonClient) {
            return new RedisStore(redissonClient);
        }

        @Bean
        @ConditionalOnMissingBean(LockService.class)
        @ConditionalOnBean(RedissonClient.class)
        public LockService redisLockService(RedissonClient redissonClient) {
            return new RedisLockService(redissonClient);
        }
    }


    @Bean
    @ConditionalOnMissingBean(DataStore.class)
    public DataStore memDataStore() {
        TimedCache<Object, Object> cache = CacheUtil.newTimedCache(0);
        return new DataStore() {
            @Override
            public void put(String key, Object object) {
                cache.put(key, object);
            }

            @Override
            public void put(String key, Object object, long timeout) {
                cache.put(key, object, timeout);
            }

            @Override
            public <T> T get(String key) {
                return (T) cache.get(key);
            }

            @Override
            public <T> T getAndRemove(String key) {
                T t = get(key);
                cache.remove(key);
                return t;
            }

            @Override
            public <T> T get(String key, Supplier<T> supplier) {
                if (containsKey(key)) {
                    return get(key);
                }
                T v = supplier.get();
                put(key, v);
                return v;
            }

            @Override
            public void remove(String key) {
                cache.remove(key);
            }

            @Override
            public boolean containsKey(String key) {
                return cache.containsKey(key);
            }
        };
    }


    @Bean
    @ConditionalOnMissingBean(LockService.class)
    public LockService simpleLockService() {
        return new LocalLockService();
    }


    @Bean
    @ConditionalOnProperty(
            prefix = "teaql",
            name = "useSmartListAsList",
            havingValue = "true",
            matchIfMissing = true)
    public Jackson2ObjectMapperBuilderCustomizer smartListSerializer() {
        return jacksonObjectMapperBuilder -> {
            jacksonObjectMapperBuilder.postConfigurer(
                    mapper -> {
                        mapper.registerModule(TeaQLModule.INSTANCE);
                        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    });
        };
    }

    @Bean
    @ConditionalOnMissingBean(TQLResolver.class)
    public TQLResolver tqlResolver() {
        TQLResolver tqlResolver =
                new TQLResolver() {
                    @Override
                    public <T> T getBean(Class<T> clazz) {
                        return SpringUtil.getBean(clazz);
                    }

                    @Override
                    public <T> List<T> getBeans(Class<T> clazz) {
                        Map<String, T> beansOfType = SpringUtil.getBeansOfType(clazz);
                        if (ObjectUtil.isEmpty(beansOfType)) {
                            return Collections.emptyList();
                        }
                        ArrayList<T> list = new ArrayList<>(beansOfType.values());
                        list.sort(AnnotationAwareOrderComparator.INSTANCE);
                        return list;
                    }

                    @Override
                    public <T> T getBean(String name) {
                        return SpringUtil.getBean(name);
                    }
                };
        GLobalResolver.registerResolver(tqlResolver);
        return tqlResolver;
    }

    @Bean
    @ConditionalOnMissingBean(name = "blobObjectMessageConverter")
    public BlobObjectMessageConverter blobObjectMessageConverter() {
        return new BlobObjectMessageConverter();
    }

    @Bean("multiReadFilter")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @Order
    public Filter multiReadRequest() {
        return new MultiReadFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public UserContextFactory userContextFactory(DataConfigProperties config) {
        return new DefaultUserContextFactory(config);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @Order
    public UserContextInitializer servletInitializer() {
        return new ServletUserContextInitializer();
    }

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class TQLContextResolver implements HandlerMethodArgumentResolver {
        private final UserContextFactory userContextFactory;

        public TQLContextResolver(UserContextFactory userContextFactory) {
            this.userContextFactory = userContextFactory;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(TQLContext.class)
                    || UserContext.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory)
                throws Exception {
            return userContextFactory.create(webRequest);
        }

        @Bean
        public WebMvcConfigurer tqlConfigure(
                TQLContextResolver tqlResolver, BlobObjectMessageConverter blobObjectMessageConverter) {
            return new WebMvcConfigurer() {
                @Override
                public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                    resolvers.add(tqlResolver);
                }

                @Override
                public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                    converters.removeIf(c -> c == blobObjectMessageConverter);
                    converters.add(0, blobObjectMessageConverter);
                }
            };
        }
    }

    @ControllerAdvice
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class TeaQLRequestAdvice {
        @org.springframework.web.bind.annotation.InitBinder
        public void initBinder(org.springframework.web.bind.WebDataBinder binder) {
            Object target = binder.getTarget();
            if (target != null) {
                Class<?> clazz = target.getClass();
                if (Entity.class.isAssignableFrom(clazz) && !RemoteInput.class.isAssignableFrom(clazz)) {
                    throw new IllegalArgumentException("Binding of " + clazz.getName() + " is rejected because it does not implement " + RemoteInput.class.getName());
                }
            }
        }
    }

    @ControllerAdvice
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class TeaQLResponseAdvice implements ResponseBodyAdvice {
        @Override
        public boolean supports(MethodParameter returnType, Class converterType) {
            return true;
        }

        @Override
        public Object beforeBodyWrite(
                Object body,
                MethodParameter returnType,
                MediaType selectedContentType,
                Class selectedConverterType,
                ServerHttpRequest request,
                ServerHttpResponse response) {
            handleXClass(body, response);
            handleToast(body, response);
            return body;
        }

        private void handleXClass(Object body, ServerHttpResponse response) {
            String xClass = response.getHeaders().getFirst(UserContext.X_CLASS);
            if (ObjectUtil.isEmpty(xClass) && body != null) {
                response.getHeaders().set(UserContext.X_CLASS, body.getClass().getName());
            }
        }

        private void handleToast(Object body, ServerHttpResponse response) {
            String command = response.getHeaders().getFirst("command");
            if (command == null) {
                String toast = response.getHeaders().getFirst("toast");
                response.getHeaders().remove("toast");
                if (toast != null) {
                    try {
                        Object toastObj = JSONUtil.toBean(Base64.decodeStr(toast), Map.class);
                        BeanUtil.setProperty(body, "toast", toastObj);
                        Object playSound = BeanUtil.getProperty(toastObj, "playSound");
                        if (playSound != null) {
                            BeanUtil.setProperty(body, "playSound", playSound);
                        }
                    }
                    catch (Exception e) {
                        // ignore toast setting
                    }
                }
            }
        }
    }

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public static class TQLReactiveContextResolver
            implements org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver {
        private final UserContextFactory userContextFactory;

        public TQLReactiveContextResolver(UserContextFactory userContextFactory) {
            this.userContextFactory = userContextFactory;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(TQLContext.class)
                    || UserContext.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Mono<Object> resolveArgument(
                MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {
            return Mono.just(userContextFactory.create(exchange));
        }

        @Bean
        @ConditionalOnMissingBean
        public WebFluxConfigurer webFluxConfigurer(TQLReactiveContextResolver resolver) {
            return new WebFluxConfigurer() {
                @Override
                public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
                    configurer.addCustomResolver(resolver);
                }
            };
        }
    }
}
