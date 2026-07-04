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

public class JsonSQLProperty extends GenericSQLProperty implements SQLProperty {

    private static final ObjectMapper defaultObjectMapper = new ObjectMapper();

    protected ObjectMapper resolveMapper(UserContext ctx) {
        ObjectMapper mapper = (ObjectMapper) ctx.getObj("objectMapper");
        return mapper != null ? mapper : defaultObjectMapper;
    }

    @Override
    public List<SQLData> toDBRaw(UserContext ctx, Entity entity, Object v) {
        ObjectMapper objectMapper = resolveMapper(ctx);
        try {
            String value = objectMapper.writeValueAsString(v);
            Boolean zip = MapUtil.getBool(getAdditionalInfo(), "zip");
            if (zip != null && zip) {
                byte[] gzip = ZipUtil.gzip(value.getBytes(StandardCharsets.UTF_8));
                value = Base64.encode(gzip);
            }
            return super.toDBRaw(ctx, entity, value);
        }
        catch (JsonProcessingException pE) {
            throw new TeaQLRuntimeException(pE);
        }
    }

    @Override
    public void setPropertyValue(UserContext ctx, Entity entity, ResultSet rs) {
        if (!findName(rs, getName())) {
            return;
        }
        ObjectMapper objectMapper = resolveMapper(ctx);
        try {
            Class targetType = getType().javaType();
            Object value = getValue(rs);
            String jsonString = Convert.convert(String.class, value);
            Boolean zipped = MapUtil.getBool(getAdditionalInfo(), "zip");
            if (zipped != null && zipped) {
                byte[] decodeStr = Base64.decode(jsonString);
                byte[] bytes = ZipUtil.unGzip(decodeStr);
                jsonString = new String(bytes, StandardCharsets.UTF_8);
            }
            Object o = objectMapper.readValue(jsonString, targetType);
            entity.setProperty(getName(), o);
        }
        catch (JsonMappingException pE) {
            throw new TeaQLRuntimeException(pE);
        }
        catch (JsonProcessingException pE) {
            throw new TeaQLRuntimeException(pE);
        }
    }
}
