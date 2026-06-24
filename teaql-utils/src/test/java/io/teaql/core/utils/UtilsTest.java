package io.teaql.core.utils;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class UtilsTest {

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

    // =========================================================================
    // DEDICATED TESTS FOR EACH UTILITY CLASS (HAPPY PATH & EXCEPTION BRANCHES)
    // =========================================================================

    @Test
    public void testArrayUtil() {
        // Happy path
        String[] arr = {"a", "b", "c"};
        assertTrue(ArrayUtil.contains(arr, "b"));
        assertFalse(ArrayUtil.contains(arr, "z"));
        assertEquals("b", ArrayUtil.get(arr, 1));
        assertTrue(ArrayUtil.isArray(arr));
        assertEquals(3, ArrayUtil.length(arr));
        String[] sub = ArrayUtil.sub(arr, 1, 3);
        assertEquals(2, sub.length);
        assertEquals("b", sub[0]);

        // Exceptional / boundary branches
        assertEquals("c", ArrayUtil.get(arr, -1)); // Python-like negative index is safe
        assertNull(ArrayUtil.get(arr, 5));
        assertNull(ArrayUtil.get((String[]) null, 0));
        assertEquals(0, ArrayUtil.length(null));
        assertFalse(ArrayUtil.contains(null, "element"));
        assertFalse(ArrayUtil.contains(arr, null));
        assertFalse(ArrayUtil.isArray(null));
        assertFalse(ArrayUtil.isArray("not an array"));

        assertThrows(NullPointerException.class, () -> ArrayUtil.sub((String[]) null, 0, 1));
    }

    @Test
    public void testBase64() {
        // Happy path
        String orig = "Hello";
        String encoded = Base64.encode(orig);
        assertEquals(orig, Base64.decodeStr(encoded));

        byte[] origBytes = orig.getBytes(StandardCharsets.UTF_8);
        String encodedStr = Base64.encode(origBytes);
        assertArrayEquals(origBytes, Base64.decode(encodedStr));

        byte[] encodedBytes = Base64.encode(origBytes, false);
        assertArrayEquals(origBytes, Base64.decode(encodedBytes));

        // Exceptional / boundary branches
        assertThrows(NullPointerException.class, () -> Base64.encode((byte[]) null));
        assertThrows(NullPointerException.class, () -> Base64.encode((String) null));
        assertNull(Base64.decodeStr((String) null));
        assertNull(Base64.decode((byte[]) null));
        assertNull(Base64.decode((String) null));
    }

    @Test
    public void testBase64Encoder() {
        // Happy path
        byte[] orig = "Hello World".getBytes(StandardCharsets.UTF_8);
        String encoded = Base64Encoder.encodeUrlSafe(orig);
        assertNotNull(encoded);

        // Exceptional / boundary branches
        assertNull(Base64Encoder.encodeUrlSafe((byte[]) null));
    }

    @Test
    public void testBooleanUtil() {
        // Happy path
        assertTrue(BooleanUtil.toBoolean("true"));
        assertTrue(BooleanUtil.toBoolean("TRUE"));
        assertFalse(BooleanUtil.toBoolean("false"));

        // Exceptional / boundary branches
        assertFalse(BooleanUtil.toBoolean(null));
        assertFalse(BooleanUtil.toBoolean(""));
        assertFalse(BooleanUtil.toBoolean("not-a-boolean"));
    }

    @Test
    public void testCacheUtil() {
        // Happy path
        Cache<String, String> timed = CacheUtil.newTimedCache(1000);
        Cache<String, String> lru = CacheUtil.newLRUCache(10, 1000);
        assertNotNull(timed);
        assertNotNull(lru);

        // Exceptional / boundary branches
        assertDoesNotThrow(() -> CacheUtil.newTimedCache(-100));
        assertThrows(IllegalArgumentException.class, () -> CacheUtil.newLRUCache(-5, -100));
    }

    @Test
    public void testCallerUtil() {
        // Happy path
        Class<?> caller = CallerUtil.getCaller(0);
        assertNotNull(caller);

        // Exceptional / boundary branches
        assertNull(CallerUtil.getCaller(999999));
    }

    @Test
    public void testCaseInsensitiveMap() {
        // Happy path
        Map<String, String> map = new CaseInsensitiveMap<>();
        map.put("Hello", "World");
        assertTrue(map.containsKey("hello"));
        assertTrue(map.containsKey("HELLO"));
        assertEquals("World", map.get("hello"));

        map.put("hello", "NewWorld");
        assertEquals("NewWorld", map.get("HELLO"));
        assertEquals(1, map.size()); // Overwrites due to case-insensitivity

        // Exceptional / boundary branches
        assertFalse(map.containsKey(null));
        assertNull(map.get(null));
        assertNull(map.remove(null));

        map.put(null, "NullValue");
        assertTrue(map.containsKey(null));
        assertEquals("NullValue", map.get(null));
        assertEquals("NullValue", map.remove(null));
    }

    @Test
    public void testCharUtil() {
        // Happy path
        assertTrue(CharUtil.isLetter('a'));
        assertTrue(CharUtil.isLetter('Z'));
        assertFalse(CharUtil.isLetter('5'));

        // Exceptional / boundary branches
        assertFalse(CharUtil.isLetter(' '));
        assertFalse(CharUtil.isLetter('\n'));
        assertFalse(CharUtil.isLetter('!'));
    }

    @Test
    public void testCharsetUtil() {
        // Happy path
        assertEquals(StandardCharsets.UTF_8, CharsetUtil.CHARSET_UTF_8);
        assertEquals(Charset.forName("GBK"), CharsetUtil.CHARSET_GBK);
    }

    @Test
    public void testClassUtil() {
        // Happy path
        Method[] methods = ClassUtil.getPublicMethods(Person.class);
        assertTrue(methods.length > 0);

        assertTrue(ClassUtil.isAssignable(List.class, ArrayList.class));
        assertFalse(ClassUtil.isAssignable(ArrayList.class, List.class));

        assertTrue(ClassUtil.isInterface(List.class));
        assertFalse(ClassUtil.isInterface(Person.class));

        assertTrue(ClassUtil.isSimpleValueType(String.class));
        assertTrue(ClassUtil.isSimpleValueType(int.class));
        assertFalse(ClassUtil.isSimpleValueType(Person.class));

        Class<?> loaded = ClassUtil.loadClass("io.teaql.core.utils.UtilsTest$Person");
        assertEquals(Person.class, loaded);

        // Exceptional / boundary branches
        assertThrows(RuntimeException.class, () -> ClassUtil.loadClass("non.existent.ClassName"));
        assertThrows(RuntimeException.class, () -> ClassUtil.loadClass(null));
        assertThrows(NullPointerException.class, () -> ClassUtil.isSimpleValueType(null));
        assertFalse(ClassUtil.isAssignable(null, String.class));
        assertFalse(ClassUtil.isAssignable(String.class, null));
        assertThrows(NullPointerException.class, () -> ClassUtil.isInterface(null));
        assertTrue(ClassUtil.getPublicMethods(null) == null || ClassUtil.getPublicMethods(null).length == 0);
    }

    @Test
    public void testCollStreamUtil() {
        // Happy path
        List<Person> list = Arrays.asList(new Person("Alice", 20), new Person("Bob", 30));
        Map<Integer, List<Person>> group = CollStreamUtil.groupByKey(list, Person::getAge);
        assertEquals(1, group.get(20).size());

        Map<String, Person> idMap = CollStreamUtil.toIdentityMap(list, Person::getName);
        assertEquals(30, idMap.get("Bob").getAge());

        // Exceptional / boundary branches
        assertTrue(CollStreamUtil.groupByKey(null, Person::getAge).isEmpty());
        assertTrue(CollStreamUtil.groupByKey(Collections.emptyList(), Person::getAge).isEmpty());
        assertThrows(NullPointerException.class, () -> CollStreamUtil.groupByKey(list, null));

        assertTrue(CollStreamUtil.toIdentityMap(null, Person::getName).isEmpty());
        assertTrue(CollStreamUtil.toIdentityMap(Collections.emptyList(), Person::getName).isEmpty());
        assertThrows(NullPointerException.class, () -> CollStreamUtil.toIdentityMap(list, null));
    }

    @Test
    public void testCollUtil() {
        // Happy path
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a", CollUtil.getFirst(list));
        assertEquals("b", CollUtil.get(list, 1));

        // Exceptional / boundary branches
        assertNull(CollUtil.getFirst((List<String>) null));
        assertNull(CollUtil.getFirst(Collections.emptyList()));
        assertNull(CollUtil.get(null, 0));
        assertEquals("c", CollUtil.get(list, -1)); // Python-like negative index gets last element
        assertNull(CollUtil.get(list, 5));
    }

    @Test
    public void testCollectionUtil() {
        // Happy path
        List<String> list = Arrays.asList("a", "b", "c");
        assertFalse(CollectionUtil.isEmpty(list));
        assertEquals(3, CollectionUtil.size(list));
        assertEquals("a", CollectionUtil.getFirst(list));
        assertEquals("c", CollectionUtil.getLast(list));
        assertEquals("b", CollectionUtil.findOne(list, "b"::equals));
        assertEquals("a,b,c", CollectionUtil.join(list, ","));

        // Exceptional / boundary branches
        assertTrue(CollectionUtil.isEmpty((Collection<?>) null));
        assertTrue(CollectionUtil.isEmpty(new ArrayList<>()));
        assertEquals(0, CollectionUtil.size(null));
        assertNull(CollectionUtil.getFirst((List<String>) null));
        assertNull(CollectionUtil.getFirst(Collections.emptyList()));
        assertNull(CollectionUtil.getLast((List<String>) null));
        assertNull(CollectionUtil.getLast(Collections.emptyList()));
        assertNull(CollectionUtil.findOne(null, "b"::equals));
        assertThrows(NullPointerException.class, () -> CollectionUtil.findOne(list, null));
        assertNull(CollectionUtil.join((List<String>) null, ","));
    }

    @Test
    public void testCompareUtil() {
        // Happy path
        assertTrue(CompareUtil.compare(1, 2) < 0);
        assertTrue(CompareUtil.compare(2, 1) > 0);
        assertEquals(0, CompareUtil.compare(5, 5));

        // Exceptional / boundary branches
        assertTrue(CompareUtil.compare(null, 5) < 0);
        assertTrue(CompareUtil.compare(5, null) > 0);
        assertEquals(0, CompareUtil.compare((Integer) null, null));
    }

    @Test
    public void testConvert() {
        // Happy path
        assertEquals(Integer.valueOf(123), Convert.convert(Integer.class, "123"));
        assertEquals(BigDecimal.valueOf(45.67), Convert.convert(BigDecimal.class, "45.67"));

        // Exceptional / boundary branches
        assertNull(Convert.convert(Integer.class, null));
        assertThrows(Exception.class, () -> Convert.convert(Integer.class, "not-a-number"));
    }

    @Test
    public void testDateUtil() {
        // Happy path
        Date date = new Date(1716700000000L);
        LocalDateTime ldt = DateUtil.toLocalDateTime(date);
        assertNotNull(ldt);

        // Exceptional / boundary branches
        assertNull(DateUtil.toLocalDateTime((Date) null));
    }

    @Test
    public void testIdUtil() {
        // Happy path
        String uuid = IdUtil.fastSimpleUUID();
        assertEquals(32, uuid.length());
        assertFalse(uuid.contains("-"));

        long nextId = IdUtil.getSnowflakeNextId();
        assertTrue(nextId > 0);

        String nextIdStr = IdUtil.getSnowflakeNextIdStr();
        assertNotNull(nextIdStr);
        assertTrue(nextIdStr.length() > 0);
    }

    @Test
    public void testIoUtil() throws IOException {
        // Happy path
        byte[] data = "Hello Stream".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        byte[] read = IoUtil.readBytes(bais);
        assertArrayEquals(data, read);

        // Exceptional / boundary branches
        assertThrows(IllegalArgumentException.class, () -> IoUtil.readBytes(null));
        
        // InputStream that throws exception
        ByteArrayInputStream throwingStream = new ByteArrayInputStream(new byte[1]) {
            @Override
            public synchronized int read(byte[] b, int off, int len) {
                throw new RuntimeException("Simulated IO Exception");
            }
        };
        assertThrows(RuntimeException.class, () -> IoUtil.readBytes(throwingStream));
    }

    @Test
    public void testLRUCache() {
        // Happy path
        LRUCache<String, String> cache = new LRUCache<>(2, 5000);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        cache.put("d", "4");

        // Exceptional / boundary branches: at least one of them must be evicted since capacity is 2
        assertTrue(cache.get("a") == null || cache.get("b") == null);

        // Supplier get
        assertEquals("4", cache.get("d", () -> "4"));
        assertEquals("4", cache.get("d"));

        assertFalse(cache.containsKey(null));
        assertNull(cache.get(null));
        assertDoesNotThrow(() -> cache.remove(null));
        assertThrows(RuntimeException.class, () -> cache.get("missing", (java.util.function.Supplier<String>) null));
    }

    @Test
    public void testListUtil() {
        // Happy path
        List<String> empty = ListUtil.empty();
        assertTrue(empty.isEmpty());

        String[] arr = {"a", "b"};
        List<String> list = ListUtil.toList(arr);
        assertEquals(2, list.size());
        assertEquals("a", list.get(0));

        // Exceptional / boundary branches
        assertThrows(UnsupportedOperationException.class, () -> empty.add("new-elem"));
        assertTrue(ListUtil.toList((String[]) null).isEmpty());
    }

    @Test
    public void testLocalDateTimeUtil() {
        // Happy path
        LocalDateTime ldt = LocalDateTime.of(2026, 5, 26, 12, 0, 0);
        String formatted = LocalDateTimeUtil.formatNormal(ldt);
        assertEquals("2026-05-26 12:00:00", formatted);

        // Exceptional / boundary branches
        assertNull(LocalDateTimeUtil.formatNormal((LocalDateTime) null));
    }

    @Test
    public void testMapUtil() {
        // Happy path
        Map<String, Object> map = MapUtil.of("k1", "v1");
        assertEquals("v1", map.get("k1"));

        Map<Object, Object> built = MapUtil.builder()
                .put("boolTrue", true)
                .put("boolFalse", "false")
                .build();
        assertTrue(MapUtil.getBool(built, "boolTrue"));
        assertFalse(MapUtil.getBool(built, "boolFalse"));
        assertNull(MapUtil.getBool(built, "missing"));

        assertTrue(MapUtil.empty().isEmpty());

        // Exceptional / boundary branches
        assertThrows(IllegalArgumentException.class, () -> MapUtil.of(new Object[]{"k1"})); // Odd arguments
        assertNull(MapUtil.getBool(null, "key"));
    }

    @Test
    public void testNamingCase() {
        // Happy path
        assertEquals("helloWorld", NamingCase.toCamelCase("hello_world"));
        assertEquals("HelloWorld", NamingCase.toPascalCase("hello_world"));
        assertEquals("hello_world", NamingCase.toUnderlineCase("helloWorld"));

        // Exceptional / boundary branches
        assertNull(NamingCase.toCamelCase(null));
        assertNull(NamingCase.toPascalCase(null));
        assertNull(NamingCase.toUnderlineCase(null));

        assertEquals("", NamingCase.toCamelCase(""));
        assertEquals("", NamingCase.toPascalCase(""));
        assertEquals("", NamingCase.toUnderlineCase(""));
    }

    @Test
    public void testNumberUtil() {
        // Happy path
        BigDecimal a = new BigDecimal("10.0");
        BigDecimal b = new BigDecimal("5.0");
        assertEquals(new BigDecimal("15.0"), NumberUtil.add(a, b));
        assertTrue(NumberUtil.isGreater(a, b));
        assertTrue(NumberUtil.isLess(b, a));

        assertEquals(100, NumberUtil.parseNumber("100").intValue());
        assertEquals(new BigDecimal("99.9"), NumberUtil.toBigDecimal("99.9"));

        // Exceptional / boundary branches
        assertEquals(new BigDecimal("5.0"), NumberUtil.add(null, b)); // Null treats as 0 in addition
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.isGreater(null, b));
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.isLess(null, b));

        assertThrows(Exception.class, () -> NumberUtil.parseNumber("invalid-number"));
        assertThrows(Exception.class, () -> NumberUtil.toBigDecimal("invalid-number"));
        assertThrows(Exception.class, () -> NumberUtil.parseNumber(null));
        
        // Null string toBigDecimal returns either null or BigDecimal.ZERO safely
        BigDecimal res = NumberUtil.toBigDecimal((String) null);
        assertTrue(res == null || BigDecimal.ZERO.compareTo(res) == 0);
    }

    @Test
    public void testObjUtil() {
        // Happy path
        assertTrue(ObjUtil.isEmpty(null));
        assertTrue(ObjUtil.isEmpty(""));
        assertTrue(ObjUtil.isEmpty(new ArrayList<>()));
        assertFalse(ObjUtil.isEmpty("not empty"));

        // Exceptional / boundary branches
        assertTrue(ObjUtil.isEmpty(new String[0]));
        assertFalse(ObjUtil.isEmpty(new String[]{"a"}));
    }

    @Test
    public void testObjectUtil() {
        // Happy path
        assertTrue(ObjectUtil.isNull(null));
        assertFalse(ObjectUtil.isNull(""));
        assertTrue(ObjectUtil.isNotNull(""));
        assertFalse(ObjectUtil.isNotNull(null));

        assertTrue(ObjectUtil.equals("a", "a"));
        assertFalse(ObjectUtil.equals("a", "b"));

        assertEquals(5, ObjectUtil.length("hello"));
        assertEquals(3, ObjectUtil.length(new int[]{1, 2, 3}));

        // Exceptional / boundary branches
        assertTrue(ObjectUtil.equals(null, null));
        assertFalse(ObjectUtil.equals(null, "a"));
        assertEquals(0, ObjectUtil.length(null));
        assertEquals(-1, ObjectUtil.length(123)); // Non-iterable/non-array fallback length is -1
    }

    @Test
    public void testOptNullBasicTypeFromObjectGetter() {
        OptNullBasicTypeFromObjectGetter<String> getter = new OptNullBasicTypeFromObjectGetter<String>() {
            @Override
            public Object getObj(String key, Object defaultValue) {
                if ("str".equals(key)) return "hello";
                if ("num".equals(key)) return 123;
                if ("bool".equals(key)) return true;
                if ("invalidNum".equals(key)) return "not-a-number";
                return defaultValue;
            }
        };

        // Happy
        assertEquals("hello", getter.getStr("str"));
        assertEquals(Integer.valueOf(123), getter.getInt("num"));
        assertTrue(getter.getBool("bool"));

        // Exceptional / boundary branches
        assertNull(getter.getStr("missing"));
        assertEquals("default", getter.getStr("missing", "default"));

        assertNull(getter.getInt("missing"));
        assertEquals(Integer.valueOf(999), getter.getInt("missing", 999));
        assertThrows(NumberFormatException.class, () -> getter.getInt("invalidNum"));
        assertThrows(NumberFormatException.class, () -> getter.getLong("invalidNum"));
    }

    @Test
    public void testPageUtil() {
        // Happy path
        assertEquals(0, PageUtil.getStart(0, 10));
        assertEquals(10, PageUtil.getStart(1, 10));

        // Exceptional / boundary branches
        assertEquals(0, PageUtil.getStart(-5, 10));
        assertEquals(0, PageUtil.getStart(1, -10));
        assertEquals(0, PageUtil.getStart(-5, -10));
    }

    @Test
    public void testResourceUtil() {
        // Happy / Exceptional: reading resource from invalid path should throw
        assertThrows(RuntimeException.class, () -> ResourceUtil.readUtf8Str("non-existent-resource.txt"));
        assertThrows(RuntimeException.class, () -> ResourceUtil.readUtf8Str((String) null));
    }

    @Test
    public void testRowKeyTable() {
        // Happy path
        RowKeyTable<String, String, Integer> table = new RowKeyTable<>();
        table.put("r1", "c1", 100);
        assertEquals(100, table.get("r1", "c1"));

        assertEquals(100, table.remove("r1", "c1"));
        assertNull(table.get("r1", "c1"));

        // Exceptional / boundary branches
        assertNull(table.get("missing", "missing"));
        assertNull(table.get(null, null));
        assertNull(table.remove("missing", "missing"));
        assertNull(table.remove(null, null));
    }

    @Test
    public void testStrBuilder() {
        // Happy path
        StrBuilder sb = new StrBuilder();
        sb.append("Hello").append(' ').append("World");
        assertEquals("Hello World", sb.toString());
        assertEquals(11, sb.length());

        sb.clear();
        assertEquals(0, sb.length());

        // Exceptional / boundary branches
        assertThrows(NegativeArraySizeException.class, () -> new StrBuilder(-5));
        
        StrBuilder sb2 = new StrBuilder();
        assertDoesNotThrow(() -> sb2.append((String) null));
        assertTrue(sb2.toString().isEmpty() || "null".equals(sb2.toString()));
    }

    @Test
    public void testStrUtil() {
        // Happy path
        assertEquals("World", StrUtil.removePrefix("HelloWorld", "Hello"));
        assertEquals("HelloWorld", StrUtil.removePrefix("HelloWorld", "NotHello"));
        assertEquals("Hello Bob", StrUtil.format("Hello {}", "Bob"));
        assertEquals("Hello {}", StrUtil.format("Hello {}", (Object[]) null));
        assertEquals("value", StrUtil.unWrap("xvaluex", 'x'));

        // Exceptional / boundary branches
        assertNull(StrUtil.removePrefix(null, "prefix"));
        assertEquals("string", StrUtil.removePrefix("string", null));
        
        assertTrue(StrUtil.format(null, "arg").isEmpty() || "null".equals(StrUtil.format(null, "arg")));
        assertEquals("value", StrUtil.unWrap("value", 'x')); // unmatched wrapper
        assertNull(StrUtil.unWrap(null, 'x'));
    }

    @Test
    public void testStreamUtil() {
        // Happy path
        List<String> list = Arrays.asList("a", "b");
        long count = StreamUtil.of(list).count();
        assertEquals(2, count);

        // Exceptional / boundary branches
        assertThrows(IllegalArgumentException.class, () -> StreamUtil.of((Iterable<?>) null));
    }

    @Test
    public void testTemporalAccessorUtil() {
        // Happy path
        LocalDateTime ldt = LocalDateTime.of(2026, 5, 26, 12, 0, 0);
        long epoch = TemporalAccessorUtil.toEpochMilli(ldt);
        assertTrue(epoch > 0);

        // Exceptional / boundary branches
        assertThrows(NullPointerException.class, () -> TemporalAccessorUtil.toEpochMilli(null));
    }

    @Test
    public void testThreadUtil() {
        // Happy path
        assertNotNull(ThreadUtil.newExecutorByBlockingCoefficient(0.5f));

        // Exceptional / boundary branches
        assertThrows(IllegalArgumentException.class, () -> ThreadUtil.newExecutorByBlockingCoefficient(-0.1f));
        assertThrows(IllegalArgumentException.class, () -> ThreadUtil.newExecutorByBlockingCoefficient(1.5f));
    }

    @Test
    public void testTimedCache() {
        // Happy path
        TimedCache<String, String> cache = new TimedCache<>(5000);
        cache.put("a", "1");
        cache.put("b", "2", 1000);
        assertEquals("1", cache.get("a"));
        assertEquals("2", cache.get("b", true));
        assertTrue(cache.containsKey("a"));

        // Exceptional / boundary branches
        assertFalse(cache.containsKey(null));
        assertNull(cache.get(null));
        assertDoesNotThrow(() -> cache.remove(null));
        assertThrows(RuntimeException.class, () -> cache.get("missing", (java.util.function.Supplier<String>) null));
    }

    @Test
    public void testTypeReference() {
        // Happy path
        TypeReference<List<String>> ref = new TypeReference<List<String>>() {};
        assertNotNull(ref.getType());

        // Exceptional / boundary branches
        assertThrows(IllegalArgumentException.class, () -> {
            new TypeReference() {};
        });
    }

    @Test
    public void testURLDecoder() {
        // Happy path
        String encoded = "Hello+World%21";
        String decoded = URLDecoder.decode(encoded, CharsetUtil.CHARSET_UTF_8);
        assertEquals("Hello World!", decoded);

        // Exceptional / boundary branches
        assertNull(URLDecoder.decode(null, CharsetUtil.CHARSET_UTF_8));
        assertNotNull(URLDecoder.decode("Hello%G1", CharsetUtil.CHARSET_UTF_8)); // Does not throw, returns string or decoded
    }

    @Test
    public void testURLEncodeUtil() {
        // Happy path
        String decoded = "Hello World!";
        String encoded = URLEncodeUtil.encode(decoded);
        assertNotNull(encoded);
        assertNotEquals(decoded, encoded);

        // Exceptional / boundary branches
        assertNull(URLEncodeUtil.encode(null));
    }

    @Test
    public void testZipUtil() {
        // Happy path
        byte[] orig = "Hello Zip".getBytes(StandardCharsets.UTF_8);
        byte[] gzipped = ZipUtil.gzip(orig);
        assertNotNull(gzipped);
        assertTrue(gzipped.length > 0);

        byte[] unzipped = ZipUtil.unGzip(gzipped);
        assertArrayEquals(orig, unzipped);

        // Exceptional / boundary branches
        assertThrows(NullPointerException.class, () -> ZipUtil.gzip((byte[]) null));
        assertThrows(NullPointerException.class, () -> ZipUtil.unGzip((byte[]) null));
        assertThrows(RuntimeException.class, () -> ZipUtil.unGzip(new byte[]{1, 2, 3, 4})); // corrupt zip bytes
    }
}
