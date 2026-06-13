package io.teaql.provider.springjdbc;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SpringJdbcSqlExecutorTest {

    private EmbeddedDatabase db;
    private SpringJdbcSqlExecutor sqlAdapter;

    @Before
    public void setUp() {
        // 创建 H2 内存数据库
        db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("testdb;MODE=MySQL")
                .build();

        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(db);
        sqlAdapter = new SpringJdbcSqlExecutor(jdbcTemplate);

        // 建表
        sqlAdapter.execute("CREATE TABLE test_user (id INT PRIMARY KEY, name VARCHAR(50), age INT)");
    }

    @After
    public void tearDown() {
        if (db != null) {
            db.shutdown();
        }
    }

    @Test
    public void testExecuteUpdateAndQuery() {
        // 1. 测试 Insert (Update)
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1);
        params.put("name", "TeaQL");
        params.put("age", 5);

        int affected = sqlAdapter.update("INSERT INTO test_user (id, name, age) VALUES (:id, :name, :age)", params);
        assertEquals(1, affected);

        // 2. 测试查询单行 (queryForMap)
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("id", 1);
        Map<String, Object> result = sqlAdapter.queryForMap("SELECT * FROM test_user WHERE id = :id", queryParams);
        
        assertNotNull(result);
        assertEquals("TeaQL", result.get("name".toUpperCase())); // H2 default upper case map keys, or depends on Spring
        
        // 3. 测试查询多行 (queryForList)
        Map<String, Object> params2 = new HashMap<>();
        params2.put("id", 2);
        params2.put("name", "Java");
        params2.put("age", 25);
        sqlAdapter.update("INSERT INTO test_user (id, name, age) VALUES (:id, :name, :age)", params2);

        List<Map<String, Object>> listResult = sqlAdapter.queryForList("SELECT * FROM test_user ORDER BY id ASC", new HashMap<>());
        assertEquals(2, listResult.size());
    }

    @Test
    public void testQueryWithRowMapper() {
        // 准备数据
        Map<String, Object> params = new HashMap<>();
        params.put("id", 100);
        params.put("name", "Alice");
        params.put("age", 20);
        sqlAdapter.update("INSERT INTO test_user (id, name, age) VALUES (:id, :name, :age)", params);

        // 测试 RowMapper
        Map<String, Object> qp = new HashMap<>();
        qp.put("ageThreshold", 18);

        List<String> names = sqlAdapter.query(
            "SELECT name FROM test_user WHERE age > :ageThreshold", 
            qp, 
            (rs, rowNum) -> rs.getString("name")
        );

        assertEquals(1, names.size());
        assertEquals("Alice", names.get(0));
    }
}
