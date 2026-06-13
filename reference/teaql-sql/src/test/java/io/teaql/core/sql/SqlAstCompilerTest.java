package io.teaql.core.sql;

import io.teaql.core.SearchRequest;
import io.teaql.core.UserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SqlAstCompilerTest {

    @Mock
    private SqlEntityMetadata mockMetadata;

    @Mock
    private SQLRepository mockRepository;

    @Mock
    private UserContext mockContext;

    @Mock
    private SearchRequest<?> mockRequest;

    @Test
    public void testBuildSimpleDataSQL() {
        SqlAstCompiler compiler = new SqlAstCompiler();

        // 模拟基础的表名数据
        lenient().when(mockMetadata.getThisPrimaryTableName()).thenReturn("user_table");
        lenient().when(mockMetadata.getPrimaryTableNames()).thenReturn(Collections.singletonList("user_table"));
        
        lenient().when(mockContext.getBool(SqlAstCompiler.MULTI_TABLE, false)).thenReturn(false);
        lenient().when(mockRequest.dataProperties(mockContext)).thenReturn(Collections.emptyList());

        // 模拟没有高级特性
        lenient().when(mockRequest.getPartitionProperty()).thenReturn(null);
        lenient().when(mockRequest.getSearchCriteria()).thenReturn(null);
        lenient().when(mockRequest.getOrderBy()).thenReturn(null);
        lenient().when(mockRequest.getSlice()).thenReturn(null);

        // 模拟 select
        lenient().when(mockRequest.getProjections()).thenReturn(Collections.emptyList());
        lenient().when(mockRequest.getSimpleDynamicProperties()).thenReturn(Collections.emptyList());
        
        lenient().when(mockContext.getBool(SqlAstCompiler.IGNORE_SUBTYPES, false)).thenReturn(true);

        lenient().when(mockRepository.escapeIdentifier(anyString())).thenAnswer(invocation -> "`" + invocation.getArgument(0) + "`");

        String sql = compiler.buildDataSQL(mockMetadata, mockRepository, mockContext, mockRequest, new HashMap<>());

        // 由于 select 为空，StrUtil.format("SELECT {} FROM {}", "", "`user_table`") 结果应为 "SELECT  FROM `user_table`" 
        assertEquals("SELECT  FROM `user_table`", sql);
    }

    @Test
    public void testMutationSqlWithBacktickEscape() {
        SqlAstCompiler compiler = new SqlAstCompiler();
        // Simulate MySQL style backtick
        lenient().when(mockRepository.escapeIdentifier(anyString())).thenAnswer(invocation -> "`" + invocation.getArgument(0) + "`");

        String insertSql = compiler.buildInsertSQL(mockRepository, "user", Arrays.asList("id", "name", "desc"), "trace_1");
        assertEquals("INSERT INTO `user` (`id`,`name`,`desc`) VALUES (?,?,?) /* [trace_1] */", insertSql);

        String updateSql = compiler.buildUpdatePrimarySQL(mockRepository, "user", Arrays.asList("name", "desc"), "trace_2");
        assertEquals("UPDATE `user` SET `name` = ? , `desc` = ? WHERE `id` = ? /* [trace_2] */", updateSql);

        String deleteSql = compiler.buildDeleteSQL(mockRepository, "user_version");
        assertEquals("UPDATE `user_version` SET `version` = ? WHERE `id` = ? AND `version` = ?", deleteSql);
    }

    @Test
    public void testMutationSqlWithDoubleQuoteEscape() {
        SqlAstCompiler compiler = new SqlAstCompiler();
        // Simulate PostgreSQL / Oracle style double quote
        lenient().when(mockRepository.escapeIdentifier(anyString())).thenAnswer(invocation -> "\"" + invocation.getArgument(0) + "\"");

        String insertSql = compiler.buildInsertSQL(mockRepository, "user", Arrays.asList("id", "name", "desc"), null);
        assertEquals("INSERT INTO \"user\" (\"id\",\"name\",\"desc\") VALUES (?,?,?)", insertSql);

        String updateSql = compiler.buildUpdatePrimarySQL(mockRepository, "user", Arrays.asList("name", "desc"), null);
        assertEquals("UPDATE \"user\" SET \"name\" = ? , \"desc\" = ? WHERE \"id\" = ?", updateSql);

        String deleteSql = compiler.buildDeleteSQL(mockRepository, "user_version");
        assertEquals("UPDATE \"user_version\" SET \"version\" = ? WHERE \"id\" = ? AND \"version\" = ?", deleteSql);
    }
}
