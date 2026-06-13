import re

with open('teaql-data-service-sql/src/main/java/io/teaql/dataservice/sql/BackendSQLRepository.java', 'r') as f:
    content = f.read()

# Package
content = content.replace('package io.teaql.core.sql;', 'package io.teaql.dataservice.sql;\n\nimport io.teaql.core.sql.portable.*;\nimport io.teaql.core.sql.portable.expression.*;\nimport io.teaql.core.sql.portable.dialect.*;')

# Class name
content = content.replace('public class SQLRepository', 'public class BackendSQLRepository')
content = content.replace('SQLRepository(', 'BackendSQLRepository(')
content = content.replace('SQLRepository ', 'BackendSQLRepository ')

# Spring imports -> custom imports
content = re.sub(r'import org\.springframework\..*?;\n', '', content)
content = content.replace('import javax.sql.DataSource;', 'import javax.sql.DataSource;\nimport java.sql.Connection;')

# Replace NamedParameterJdbcTemplate with SqlExecutionAdapter
content = content.replace('private final NamedParameterJdbcTemplate jdbcTemplate;', 'private final SqlExecutionAdapter sqlExecutionAdapter;')
content = content.replace('this.jdbcTemplate = new NamedParameterJdbcTemplate(this.dataSource);', '')
content = content.replace('jdbcTemplate.getJdbcTemplate().execute', 'sqlExecutionAdapter.update')
content = content.replace('jdbcTemplate.getJdbcTemplate().update', 'sqlExecutionAdapter.update')
content = content.replace('jdbcTemplate.getJdbcTemplate().batchUpdate', 'sqlExecutionAdapter.batchUpdate')
content = content.replace('jdbcTemplate.queryForStream', 'sqlExecutionAdapter.queryForStream')
content = content.replace('jdbcTemplate.query', 'sqlExecutionAdapter.query')

# Replace DataAccessException with RuntimeException
content = content.replace('DataAccessException', 'RuntimeException')

# Replace RowMapper with SqlRowMapper
content = content.replace('RowMapper<', 'SqlRowMapper<')

# The constructor needs to take SqlExecutionAdapter
content = re.sub(r'public BackendSQLRepository\(EntityDescriptor entityDescriptor, DataSource dataSource\) \{',
                 r'public BackendSQLRepository(EntityDescriptor entityDescriptor, DataSource dataSource, SqlExecutionAdapter sqlExecutionAdapter) {\n        this.sqlExecutionAdapter = sqlExecutionAdapter;',
                 content)

with open('teaql-data-service-sql/src/main/java/io/teaql/dataservice/sql/BackendSQLRepository.java', 'w') as f:
    f.write(content)
