# Bug: Schema auto-migration fails on Postgres due to `<max>` varchar token mismatch

## Description
When running automatic database schema migration via `PostgresDataServiceExecutor.ensureSchema(ctx)`, the framework generates SQL DDL strings for varchar fields with a placeholder `<max>` (e.g. `VARCHAR(<max>)`).

Unlike SQL Server or other dialects, PostgreSQL does not recognize `VARCHAR(<max>)` as a valid type definition, throwing a `BadSqlGrammarException` during execution.

## Expected Behavior
The Postgres database dialect executor should automatically replace any `<max>` placeholder string with a valid PostgreSQL type definition (such as `VARCHAR(255)` or `TEXT`).

## Suggested Solution
Update `PostgresDataServiceExecutor` or its SQL formatter to automatically replace `<max>` varchar placeholders prior to DDL execution:

```java
@Override
public void execute(String sql) {
    if (sql != null) {
        sql = sql.replace("<max>", "255");
    }
    super.execute(sql);
}
```
