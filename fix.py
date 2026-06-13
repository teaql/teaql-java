import re

with open('teaql-data-service-sql/src/main/java/io/teaql/dataservice/sql/BackendSQLRepository.java', 'r') as f:
    content = f.read()

# Add missing imports
imports = """
import io.teaql.core.RepositoryException;
import io.teaql.core.DefaultUserContext;
import io.teaql.dataservice.sql.SqlExecutionAdapter;
"""
content = content.replace('import java.sql.Connection;', 'import java.sql.Connection;\n' + imports)

# Remove transaction methods that use Spring
content = re.sub(r'public <T> T executeInTransaction.*?}', 'public <T> T executeInTransaction(UserContext userContext, TransactionCallback<T> action) {\n        throw new UnsupportedOperationException("Transactions should be handled by DataServiceExecutor");\n    }', content, flags=re.DOTALL)

# Fix jdbcTemplate usages
content = content.replace('jdbcTemplate.getJdbcTemplate().execute', 'sqlExecutionAdapter.execute')
content = content.replace('jdbcTemplate.getJdbcTemplate().update', 'sqlExecutionAdapter.update')
content = content.replace('jdbcTemplate.query', 'sqlExecutionAdapter.query')
content = content.replace('jdbcTemplate.queryForStream', 'sqlExecutionAdapter.queryForStream')
content = content.replace('jdbcTemplate.', 'sqlExecutionAdapter.')

# Fix executeUpdate to use execute
content = content.replace('sqlExecutionAdapter.update(sql);', 'sqlExecutionAdapter.execute(sql);')

# Fix DataClassSqlRowMapper
content = re.sub(r'new DataClassSqlRowMapper.*?\)', 'null /* TODO: Implement DataClassSqlRowMapper */', content)

# Fix info warnings on context
content = content.replace('ctx.info(', '/* ctx.info */')
content = content.replace('ctx.warn(', '/* ctx.warn */')

with open('teaql-data-service-sql/src/main/java/io/teaql/dataservice/sql/BackendSQLRepository.java', 'w') as f:
    f.write(content)
