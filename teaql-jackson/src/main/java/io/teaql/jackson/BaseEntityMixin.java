package io.teaql.jackson;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.teaql.core.EntityStatus;

public abstract class BaseEntityMixin {

    @JsonIgnore
    abstract String getSubType();

    @JsonIgnore
    abstract List<String> getUpdatedProperties();

    @JsonIgnore
    abstract String getComment();

    @JsonIgnore
    abstract String getTraceChain();

    @JsonIgnore
    abstract EntityStatus get$status();

    @JsonAnyGetter
    abstract Map<String, Object> getAdditionalInfo();

    @JsonAnySetter
    abstract void putAdditional(String propertyName, Object value);
}
