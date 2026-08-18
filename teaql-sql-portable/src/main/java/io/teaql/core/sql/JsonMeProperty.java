package io.teaql.core.sql;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.teaql.core.utils.Base64;
import io.teaql.core.utils.Convert;
import io.teaql.core.utils.MapUtil;
import io.teaql.core.utils.ZipUtil;

import io.teaql.core.Entity;
import io.teaql.core.TeaQLRuntimeException;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.Relation;

public class JsonMeProperty extends GenericSQLProperty {
    private static final ObjectMapper defaultObjectMapper = new ObjectMapper();

    protected ObjectMapper resolveMapper(UserContext context) {
        ObjectMapper mapper = (ObjectMapper) context.getObj("objectMapper");
        return mapper != null ? mapper : defaultObjectMapper;
    }

    public List<SQLData> toDBRaw(UserContext context, Entity entity, Object v) {
        ObjectMapper objectMapper = resolveMapper(context);
        // clean up current field
        entity.setProperty(getName(), null);
        try {
            // serialize the current entity as json string
            String value = objectMapper.writeValueAsString(entity);
            // zip if zip is enabled
            Boolean zip = MapUtil.getBool(getAdditionalInfo(), "zip");
            if (zip != null && zip) {
                byte[] gzip = ZipUtil.gzip(value.getBytes(StandardCharsets.UTF_8));
                value = Base64.encode(gzip);
            }
            return super.toDBRaw(context, entity, value);
        }
        catch (JsonProcessingException pE) {
            throw new TeaQLRuntimeException(pE);
        }
    }

    @Override
    public void setPropertyValue(UserContext context, Entity entity, ResultSet rs) {
        if (!findName(rs, getName())) {
            return;
        }
        ObjectMapper objectMapper = resolveMapper(context);
        try {
            Object value = getValue(rs);
            String jsonValue = Convert.convert(String.class, value);
            Boolean zipped = MapUtil.getBool(getAdditionalInfo(), "zip");
            if (zipped != null && zipped) {
                byte[] decodeStr = Base64.decode(jsonValue);
                byte[] bytes = ZipUtil.unGzip(decodeStr);
                jsonValue = new String(bytes, StandardCharsets.UTF_8);
            }
            entity.setProperty(getName(), jsonValue);
            Entity o = objectMapper.readValue(jsonValue, entity.getClass());
            EntityDescriptor owner = getOwner();
            List<Relation> foreignRelations = owner.getForeignRelations();
            for (Relation foreignRelation : foreignRelations) {
                String name = foreignRelation.getName();
                entity.setProperty(name, o.getProperty(name));
            }
        }
        catch (JsonMappingException pE) {
            throw new TeaQLRuntimeException(pE);
        }
        catch (JsonProcessingException pE) {
            throw new TeaQLRuntimeException(pE);
        }
    }
}
