import re

file_path = "/home/philip/githome/teaql-java/teaql-sql/src/main/java/io/teaql/core/sql/SQLRepository.java"

with open(file_path, "r") as f:
    content = f.read()

# Add Dialect field
content = content.replace(
    "private SqlEntityMetadata sqlMetadata;",
    "private SqlEntityMetadata sqlMetadata;\n    private io.teaql.core.sql.dialect.SqlDialect dialect = new io.teaql.core.sql.dialect.MySqlDialect();"
)

# Add getter and setter for dialect
content = content.replace(
    "public SqlEntityMetadata getSqlMetadata() {",
    "public io.teaql.core.sql.dialect.SqlDialect getDialect() {\n        return dialect;\n    }\n\n    public void setDialect(io.teaql.core.sql.dialect.SqlDialect dialect) {\n        this.dialect = dialect;\n    }\n\n    @Override\n    public String escapeIdentifier(String identifier) {\n        return dialect.escapeIdentifier(identifier);\n    }\n\n    public SqlEntityMetadata getSqlMetadata() {"
)

# Replace prepareSubsidiaryTableSql
old_sub = """    public String prepareSubsidiaryTableSql(String tableName, List<String> tableColumns) {
        return StrUtil.format(
                "REPLACE INTO {} SET {}",
                escapeIdentifier(tableName),
                tableColumns.stream().map(c -> escapeIdentifier(c) + " = ?").collect(Collectors.joining(" , ")));
    }"""
new_sub = """    public String prepareSubsidiaryTableSql(String tableName, List<String> tableColumns) {
        return dialect.buildSubsidiaryInsertSql(tableName, tableColumns);
    }"""
content = content.replace(old_sub, new_sub)

# Replace prepareLimit
old_limit = """    protected String prepareLimit(SearchRequest request) {
        Slice slice = request.getSlice();
        if (ObjectUtil.isEmpty(slice)) {
            return null;
        }
        return StrUtil.format("LIMIT {} OFFSET {}", slice.getSize(), slice.getOffset());
    }"""
new_limit = """    protected String prepareLimit(SearchRequest request) {
        return dialect.prepareLimit(request);
    }"""
content = content.replace(old_limit, new_limit)

# Replace getPartitionSQL
old_partition = """    protected String getPartitionSQL() {

        return "SELECT * FROM (SELECT {}, (row_number() over(partition by {}{} {})) as _rank from {} {}) as t where t._rank >= {} and t._rank < {}";
    }"""
new_partition = """    protected String getPartitionSQL() {
        return dialect.getPartitionSQL();
    }"""
content = content.replace(old_partition, new_partition)

with open(file_path, "w") as f:
    f.write(content)
