package io.teaql.core.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class JSONObject extends LinkedHashMap<String, Object> {
    
    public JSONObject() {
        super();
    }
    
    public JSONObject(Map<String, Object> map) {
        super(map);
    }

    @SuppressWarnings("unchecked")
    public JSONObject getJSONObject(String key) {
        Object val = get(key);
        if (val instanceof Map) {
            return new JSONObject((Map<String, Object>) val);
        }
        return null;
    }

    public String getStr(String key) {
        Object val = get(key);
        return val != null ? val.toString() : null;
    }
}
