package io.teaql.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.deser.Deserializers;
import java.util.HashSet;
import java.util.Set;

import io.teaql.core.BaseEntity;
import io.teaql.core.SmartList;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityMetaFactory;

public class TeaQLModule extends SimpleModule {
    public static final TeaQLModule INSTANCE = new TeaQLModule();
    private final Set<Class<?>> typedEntities = new HashSet<>();
    private boolean contextBound;

    public TeaQLModule() {
        super("TeaQL");
        addSerializer(BaseEntity.class, new BaseEntityJsonSerializer());
        addDeserializer(BaseEntity.class, new BaseEntityJsonDeserializer());
        addSerializer(SmartList.class, new SmartListAsListSerializer(SmartList.class));
    }

    /** Bind typed readers to this context's installed model, never process-global metadata. */
    public TeaQLModule(UserContext context) {
        this();
        contextBound = true;
        EntityMetaFactory metadata = EntityMetaFactory.requireFrom(context);
        for (var descriptor : metadata.allEntityDescriptors()) {
            Class<?> type = descriptor.getTargetType();
            if (type != null && BaseEntity.class.isAssignableFrom(type) && type != BaseEntity.class) {
                if (!typedEntities.add(type)) throw new IllegalArgumentException("Duplicate model entity class: " + type.getName());
                registerTyped(type, new TypedEntityJsonDeserializer(descriptor));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void registerTyped(Class<?> type, TypedEntityJsonDeserializer reader) {
        addDeserializer((Class<BaseEntity>) type, reader);
    }

    // A context-bound module must not be ignored after the serialization-only singleton.
    @Override public Object getTypeId() { return contextBound ? this : super.getTypeId(); }

    @Override public void setupModule(SetupContext setup) {
        super.setupModule(setup);
        if (!contextBound) return;
        setup.addDeserializers(new Deserializers.Base() {
            @Override public com.fasterxml.jackson.databind.JsonDeserializer<?> findBeanDeserializer(
                    com.fasterxml.jackson.databind.JavaType type,
                    com.fasterxml.jackson.databind.DeserializationConfig config,
                    com.fasterxml.jackson.databind.BeanDescription description) {
                Class<?> target = type.getRawClass();
                if (target == BaseEntity.class || !BaseEntity.class.isAssignableFrom(target) || typedEntities.contains(target)) return null;
                return new com.fasterxml.jackson.databind.JsonDeserializer<BaseEntity>() {
                    @Override public BaseEntity deserialize(com.fasterxml.jackson.core.JsonParser parser,
                            com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                        return context.reportInputMismatch(target, "Entity type is not installed in this context's model");
                    }
                };
            }
        });
    }
}
