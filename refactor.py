import re

def refactor_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # We only want to replace calls inside methods where userContext is available.
    content = re.sub(r'database\.query\((?!userContext)(?!ctx)(.*?)\)', r'database.query(userContext, \1)', content)
    content = re.sub(r'database\.executeUpdate\((?!userContext)(?!ctx)(.*?)\)', r'database.executeUpdate(userContext, \1)', content)
    content = re.sub(r'database\.batchUpdate\((?!userContext)(?!ctx)(.*?)\)', r'database.batchUpdate(userContext, \1)', content)
    
    content = re.sub(r'database\.executeInTransaction\(\(\) -> \{', r'database.executeInTransaction(userContext, () -> {', content)
    
    content = re.sub(r'db\.query\((?!userContext)(?!ctx)(.*?)\)', r'db.query(userContext, \1)', content)
    content = re.sub(r'db\.executeUpdate\((?!userContext)(?!ctx)(.*?)\)', r'db.executeUpdate(userContext, \1)', content)
    content = re.sub(r'db\.batchUpdate\((?!userContext)(?!ctx)(.*?)\)', r'db.batchUpdate(userContext, \1)', content)
    content = re.sub(r'db\.execute\((?!userContext)(?!ctx)(.*?)\)', r'db.execute(userContext, \1)', content)

    content = re.sub(r'public void ensureSchema\(\) \{', r'public void ensureSchema(io.teaql.core.UserContext userContext) {', content)
    content = re.sub(r'private void ensureTable\(String table, String createSql\) \{', r'private void ensureTable(io.teaql.core.UserContext userContext, String table, String createSql) {', content)
    content = re.sub(r'private void ensureColumns\(String table, java.util.List<io.teaql.core.sql.SQLColumn> columns\) \{', r'private void ensureColumns(io.teaql.core.UserContext userContext, String table, java.util.List<io.teaql.core.sql.SQLColumn> columns) {', content)
    content = re.sub(r'private void ensureIndexes\(String table, java.util.List<io.teaql.core.sql.SQLColumn> columns\) \{', r'private void ensureIndexes(io.teaql.core.UserContext userContext, String table, java.util.List<io.teaql.core.sql.SQLColumn> columns) {', content)
    
    content = re.sub(r'ensureTable\((?!userContext)(.*?)\);', r'ensureTable(userContext, \1);', content)
    content = re.sub(r'ensureColumns\((?!userContext)(.*?)\);', r'ensureColumns(userContext, \1);', content)
    content = re.sub(r'ensureIndexes\((?!userContext)(.*?)\);', r'ensureIndexes(userContext, \1);', content)
    
    content = re.sub(r'database\.execute\((?!userContext)(?!ctx)(.*?)\)', r'database.execute(userContext, \1)', content)
    content = re.sub(r'database\.getTableColumns\((?!userContext)(?!ctx)(.*?)\)', r'database.getTableColumns(\1)', content) 

    with open(filepath, 'w') as f:
        f.write(content)

refactor_file("teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/PortableSQLRepository.java")

def refactor_service(filepath):
    with open(filepath, 'r') as f:
        c = f.read()
    c = re.sub(r'repository\.ensureSchema\(\);', r'repository.ensureSchema(ctx);', c)
    c = re.sub(r'dbAdapter\.executeInTransaction\((?!ctx)(.*?)\)', r'dbAdapter.executeInTransaction(ctx, \1)', c)
    with open(filepath, 'w') as f:
        f.write(c)

refactor_service("teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/PortableSQLDataService.java")
