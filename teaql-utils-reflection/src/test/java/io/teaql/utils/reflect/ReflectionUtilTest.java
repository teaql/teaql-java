package io.teaql.utils.reflect;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReflectionUtilTest {

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

    @Test
    public void testBeanUtil() {
        Person person = new Person("Alice", 25);
        Map<String, Object> map = BeanUtil.beanToMap(person);
        assertEquals("Alice", map.get("name"));
        assertEquals(25, map.get("age"));

        Person bean = BeanUtil.toBean(map, Person.class);
        assertEquals("Alice", bean.getName());
        assertEquals(25, bean.getAge());

        BeanUtil.setProperty(bean, "name", "Bob");
        assertEquals("Bob", BeanUtil.getProperty(bean, "name"));

        assertNull(BeanUtil.beanToMap(null));
        assertNull(BeanUtil.toBean(null, Person.class));
        assertNull(BeanUtil.getProperty(null, "name"));
        assertNull(BeanUtil.getProperty(bean, null));
        assertNull(BeanUtil.getProperty(bean, "nonExistentField"));

        assertThrows(Exception.class, () -> BeanUtil.setProperty(null, "name", "value"));
        assertThrows(Exception.class, () -> BeanUtil.setProperty(bean, "nonExistentField", "value"));
    }

    @Test
    public void testReflectUtil() {
        Person person = ReflectUtil.newInstance(Person.class);
        assertNotNull(person);

        ReflectUtil.invoke(person, "setName", "Bob");
        assertEquals("Bob", person.getName());

        java.lang.reflect.Field field = ReflectUtil.getField(Person.class, "name");
        assertNotNull(field);

        assertThrows(RuntimeException.class, () -> ReflectUtil.newInstance(java.io.InputStream.class));
        assertThrows(RuntimeException.class, () -> ReflectUtil.newInstance(null));

        assertNull(ReflectUtil.getField(Person.class, "nonExistentField"));
        assertThrows(IllegalArgumentException.class, () -> ReflectUtil.getField(null, "name"));

        assertThrows(RuntimeException.class, () -> ReflectUtil.invoke(person, "nonExistentMethod"));
        assertThrows(RuntimeException.class, () -> ReflectUtil.invoke(null, "setName"));
    }
}
