package io.teaql.coreservice;

import io.teaql.core.UserContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataServiceRegistryTest {

    @Test
    void testResolveExecutors() {
        // 创建一个同时实现 QueryExecutor 和 MutationExecutor 的 Mock
        class MockFullExecutor implements QueryExecutor, MutationExecutor {
            @Override
            public DataServiceCapabilities capabilities() {
                return null;
            }

            @Override
            public String name() {
                return "sql";
            }

            @Override
            public QueryResult query(UserContext ctx, QueryRequest request) {
                return null;
            }

            @Override
            public MutationResult mutate(UserContext ctx, MutationRequest request) {
                return null;
            }
        }

        // 创建一个只实现 QueryExecutor 的 Mock
        class MockQueryOnlyExecutor implements QueryExecutor {
            @Override
            public DataServiceCapabilities capabilities() {
                return null;
            }

            @Override
            public String name() {
                return "readonly";
            }

            @Override
            public QueryResult query(UserContext ctx, QueryRequest request) {
                return null;
            }
        }

        MockFullExecutor sqlExecutor = new MockFullExecutor();
        MockQueryOnlyExecutor readOnlyExecutor = new MockQueryOnlyExecutor();

        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .dataService("sql", sqlExecutor)
                .dataService("readonly", readOnlyExecutor)
                .build();

        // 验证解析通用 DataServiceExecutor
        assertNotNull(runtime.resolve("sql"));
        assertEquals(sqlExecutor, runtime.resolve("sql"));

        // 验证解析 QueryExecutor
        assertNotNull(runtime.resolveQueryExecutor("sql"));
        assertNotNull(runtime.resolveQueryExecutor("readonly"));

        // 验证解析 MutationExecutor
        assertNotNull(runtime.resolveMutationExecutor("sql"));
        
        // 当一个 Executor 没有实现 MutationExecutor 却被请求时，应该报错
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            runtime.resolveMutationExecutor("readonly");
        });
        assertTrue(exception.getMessage().contains("is not a MutationExecutor"));

        // 验证未知服务
        assertThrows(IllegalArgumentException.class, () -> {
            runtime.resolve("unknown");
        });
    }
}
