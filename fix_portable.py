import os
import re

file_path = "teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/PortableSQLRepository.java"
with open(file_path, "r") as f:
    content = f.read()

content = re.sub(r'db\.query\((?!ctx)(.*?)\)', r'db.query(userContext, \1)', content)
content = re.sub(r'db\.executeUpdate\((?!ctx)(.*?)\)', r'db.executeUpdate(userContext, \1)', content)
content = re.sub(r'db\.batchUpdate\((?!ctx)(.*?)\)', r'db.batchUpdate(userContext, \1)', content)
content = re.sub(r'db\.execute\((?!ctx)(.*?)\)', r'db.execute(userContext, \1)', content)

content = re.sub(r'database\.query\((?!ctx)(?!userContext)(.*?)\)', r'database.query(userContext, \1)', content)
content = re.sub(r'database\.executeUpdate\((?!ctx)(?!userContext)(.*?)\)', r'database.executeUpdate(userContext, \1)', content)
content = re.sub(r'database\.batchUpdate\((?!ctx)(?!userContext)(.*?)\)', r'database.batchUpdate(userContext, \1)', content)
content = re.sub(r'database\.execute\((?!ctx)(?!userContext)(.*?)\)', r'database.execute(userContext, \1)', content)
content = re.sub(r'database\.executeInTransaction\((?!ctx)(?!userContext)(.*?)\)', r'database.executeInTransaction(userContext, \1)', content)

with open(file_path, "w") as f:
    f.write(content)

file2 = "teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/PortableSQLDataService.java"
with open(file2, "r") as f:
    c = f.read()
c = re.sub(r'dbAdapter\.executeInTransaction\((?!ctx)(.*?)\)', r'dbAdapter.executeInTransaction(ctx, \1)', c)
with open(file2, "w") as f:
    f.write(c)

file3 = "teaql-data-service-sql/src/main/java/io/teaql/dataservice/sql/SqlDataServiceExecutor.java"
with open(file3, "r") as f:
    c = f.read()

c = re.sub(r'public java.util.List<java.util.Map<String, Object>> query\(String sql', 'public java.util.List<java.util.Map<String, Object>> query(io.teaql.core.UserContext ctx, String sql', c)
c = re.sub(r'public int executeUpdate\(String sql', 'public int executeUpdate(io.teaql.core.UserContext ctx, String sql', c)
c = re.sub(r'public int\[\] batchUpdate\(String sql', 'public int[] batchUpdate(io.teaql.core.UserContext ctx, String sql', c)
c = re.sub(r'public void execute\(String sql', 'public void execute(io.teaql.core.UserContext ctx, String sql', c)
c = re.sub(r'public void executeInTransaction\(Runnable action', 'public void executeInTransaction(io.teaql.core.UserContext ctx, Runnable action', c)

# Add logging interception inside these methods in SqlDataServiceExecutor
c = re.sub(
r'''public java\.util\.List<java\.util\.Map<String, Object>> query\(io\.teaql\.core\.UserContext ctx, String sql, Object\[\] args\) \{
\s*return executionAdapter\.queryForList\(sql, args\);
\s*\}''', 
r'''public java.util.List<java.util.Map<String, Object>> query(io.teaql.core.UserContext ctx, String sql, Object[] args) {
                    long start = System.nanoTime();
                    java.util.List<java.util.Map<String, Object>> res = executionAdapter.queryForList(sql, args);
                    long elapsed = (System.nanoTime() - start) / 1000;
                    io.teaql.runtime.log.LogManager.getInstance().writeSqlLog(ctx.getTraceChain(), new io.teaql.runtime.log.SqlLogEntry(sql, elapsed, "Fetched " + res.size() + " rows"));
                    return res;
                }''', c)

c = re.sub(
r'''public int executeUpdate\(io\.teaql\.core\.UserContext ctx, String sql, Object\[\] args\) \{
\s*return executionAdapter\.update\(sql, args\);
\s*\}''',
r'''public int executeUpdate(io.teaql.core.UserContext ctx, String sql, Object[] args) {
                    long start = System.nanoTime();
                    int res = executionAdapter.update(sql, args);
                    long elapsed = (System.nanoTime() - start) / 1000;
                    io.teaql.runtime.log.LogManager.getInstance().writeSqlLog(ctx.getTraceChain(), new io.teaql.runtime.log.SqlLogEntry(sql, elapsed, "Affected " + res + " rows"));
                    return res;
                }''', c)

c = re.sub(
r'''public int\[\] batchUpdate\(io\.teaql\.core\.UserContext ctx, String sql, java\.util\.List<Object\[\]> batchArgs\) \{
\s*return executionAdapter\.batchUpdate\(sql, batchArgs\);
\s*\}''',
r'''public int[] batchUpdate(io.teaql.core.UserContext ctx, String sql, java.util.List<Object[]> batchArgs) {
                    long start = System.nanoTime();
                    int[] res = executionAdapter.batchUpdate(sql, batchArgs);
                    long elapsed = (System.nanoTime() - start) / 1000;
                    int total = 0; if (res != null) { for(int i: res) total += i; }
                    io.teaql.runtime.log.LogManager.getInstance().writeSqlLog(ctx.getTraceChain(), new io.teaql.runtime.log.SqlLogEntry(sql, elapsed, "Batch affected " + total + " rows"));
                    return res;
                }''', c)

c = re.sub(
r'''public void execute\(io\.teaql\.core\.UserContext ctx, String sql\) \{
\s*executionAdapter\.execute\(sql\);
\s*\}''',
r'''public void execute(io.teaql.core.UserContext ctx, String sql) {
                    long start = System.nanoTime();
                    executionAdapter.execute(sql);
                    long elapsed = (System.nanoTime() - start) / 1000;
                    io.teaql.runtime.log.LogManager.getInstance().writeSqlLog(ctx.getTraceChain(), new io.teaql.runtime.log.SqlLogEntry(sql, elapsed, "Executed"));
                }''', c)


with open(file3, "w") as f:
    f.write(c)

print("Done")
