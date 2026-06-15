# TeaQL DuckDB Dialect Test Report

**Database**: DuckDB (Embedded)
**Version**: 1.0.0
**Driver**: `org.duckdb:duckdb_jdbc`

## Summary

The `teaql-duck` module provides DuckDB support. All integration tests for DuckDB passed perfectly without any errors using a file-backed in-process database (`jdbc:duckdb:./test_duckdb.db`). DuckDB offers excellent compatibility with standard PostgreSQL syntax, which allows our framework to run seamlessly by leveraging `PortableSqlDialect` alongside a minor `<max>` token hotfix.

### Key Learnings
1. **In-Memory Volatility**: The `jdbc:duckdb:` URL (with no file path) creates a completely isolated database per `Connection`. To avoid tables disappearing across database operations when a connection closes, a file-backed URL like `jdbc:duckdb:test.db` should be used.
2. **VARCHAR Syntax**: DuckDB natively supports unbounded `VARCHAR` lengths. Standard SQL `VARCHAR(max)` is not recognized. The `DuckDataServiceExecutor` actively patches `<max>` to `255` before execution, ensuring the syntax remains valid while meeting constraints.
3. **Pagination & Partitioning**: DuckDB smoothly handles `LIMIT/OFFSET` as well as complex analytic functions like `row_number() over(partition by ...)` without requiring special adaptations.

## Test Results

- **Data Insertion**: 🟢 Passed
- **Simple Query**: 🟢 Passed
- **Joins and Relations**: 🟢 Passed
- **Complex Aggregation (Group By / Facet)**: 🟢 Passed
- **Window Functions (Pagination with Partition)**: 🟢 Passed
- **Transactions**: 🟢 Passed

## Conclusion

DuckDB support is fully verified and stable.
