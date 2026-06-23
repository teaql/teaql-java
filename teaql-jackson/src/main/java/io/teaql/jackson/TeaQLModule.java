package io.teaql.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;

import io.teaql.core.BaseEntity;
import io.teaql.core.SmartList;

public class TeaQLModule extends SimpleModule {
    public static final TeaQLModule INSTANCE = new TeaQLModule();

    public TeaQLModule() {
        super("TeaQL");
        setMixInAnnotation(BaseEntity.class, BaseEntityMixin.class);
        addSerializer(SmartList.class, new SmartListAsListSerializer(SmartList.class));
    }
}
