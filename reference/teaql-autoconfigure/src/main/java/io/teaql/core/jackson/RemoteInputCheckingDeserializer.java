package io.teaql.core.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

import io.teaql.core.RemoteInput;

public class RemoteInputCheckingDeserializer extends JsonDeserializer<Object>
        implements ResolvableDeserializer, ContextualDeserializer {
    private final JsonDeserializer<Object> delegate;
    private final Class<?> beanClass;

    public RemoteInputCheckingDeserializer(JsonDeserializer<Object> delegate, Class<?> beanClass) {
        this.delegate = delegate;
        this.beanClass = beanClass;
    }

    private void checkRemoteInput(DeserializationContext ctxt) throws IOException {
        if (isControllerParameterDeserialization()) {
            if (!RemoteInput.class.isAssignableFrom(beanClass)) {
                throw ctxt.instantiationException(beanClass,
                        "Deserialization of " + beanClass.getName() + " is rejected because it does not implement " + RemoteInput.class.getName());
            }
        }
    }

    private boolean isControllerParameterDeserialization() {
        if (Boolean.getBoolean("io.teaql.core.jackson.testing.forceRemoteInputCheck")) {
            return true;
        }
        try {
            Class<?> rchClass = Class.forName("org.springframework.web.context.request.RequestContextHolder");
            Object attributes = rchClass.getMethod("getRequestAttributes").invoke(null);
            return attributes != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        checkRemoteInput(ctxt);
        return delegate != null ? delegate.deserialize(p, ctxt) : null;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt, Object intoValue) throws IOException {
        checkRemoteInput(ctxt);
        if (delegate != null) {
            return delegate.deserialize(p, ctxt, intoValue);
        }
        return intoValue;
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
            throws IOException {
        checkRemoteInput(ctxt);
        if (delegate != null) {
            return delegate.deserializeWithType(p, ctxt, typeDeserializer);
        }
        return deserialize(p, ctxt);
    }

    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException {
        if (delegate instanceof ResolvableDeserializer) {
            ((ResolvableDeserializer) delegate).resolve(ctxt);
        }
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
            throws JsonMappingException {
        if (delegate instanceof ContextualDeserializer) {
            JsonDeserializer<?> contextualDelegate = ((ContextualDeserializer) delegate).createContextual(ctxt, property);
            return new RemoteInputCheckingDeserializer((JsonDeserializer<Object>) contextualDelegate, beanClass);
        }
        return this;
    }
}
