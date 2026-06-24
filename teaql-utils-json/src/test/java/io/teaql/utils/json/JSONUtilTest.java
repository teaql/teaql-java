package io.teaql.utils.json;

import io.teaql.core.utils.TypeReference;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JSONUtilTest {

    @Test
    public void convertsJsonValues() {
        Person person = new Person("John", 30);
        String json = JSONUtil.toJsonStr(person);
        assertTrue(json.contains("\"name\":\"John\""));

        Person parsed = JSONUtil.toBean(json, Person.class);
        assertEquals("John", parsed.getName());

        Map<String, Object> map = JSONUtil.toBean(json, new TypeReference<Map<String, Object>>() {}, true);
        assertEquals("John", map.get("name"));

        assertNotNull(JSONUtil.parseObj(json));
        assertTrue(JSONUtil.toJsonStr(null) == null || "null".equals(JSONUtil.toJsonStr(null)));
        assertNotNull(JSONUtil.toBean((String) null, Person.class));
        assertThrows(Exception.class, () -> JSONUtil.toBean((String) null, new TypeReference<Map<String, Object>>() {}, true));
        assertNotNull(JSONUtil.parseObj(null));

        assertThrows(Exception.class, () -> JSONUtil.toBean("invalid-json", Person.class));
        assertThrows(Exception.class, () -> JSONUtil.toBean("invalid-json", new TypeReference<Map<String, Object>>() {}, true));
        assertThrows(Exception.class, () -> JSONUtil.parseObj("invalid-json"));
    }

    public static class Person {
        private String name;
        private int age;

        public Person() {
        }

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
