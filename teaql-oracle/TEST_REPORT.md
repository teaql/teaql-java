# Oracle Integration Test Report

## Overview
This report details the integration testing performed for the `teaql-oracle` module using Oracle Database XE.

## Test Environment
- **Database**: Oracle Database 21c Express Edition (gvenzl/oracle-xe:slim docker container)
- **Framework Component**: `teaql-oracle` dialect & `JdbcSqlExecutor`
- **Application App**: Vending Machine Compose Desktop Example
- **Scenarios Tested**:
  1. Product Fetching (Pagination & OFFSET)
  2. Order Cart Addition & Checkout (Transaction & Insert)
  3. Status Checking (WHERE & IN clauses)
  4. Facet Aggregations (GROUP BY & Count)

## Compatibility Fixes Implemented
During testing, several Oracle-specific compatibility issues were identified and resolved:
1. **Schema Data Types**: Oracle lacks support for `BIGINT`, `DATETIME`, and `BOOLEAN`. We mapped these to `NUMBER(19)`, `TIMESTAMP`, and `NUMBER(1)` respectively.
2. **JDBC Result Case Sensitivity**: Oracle returns upper-cased column aliases (e.g. `CURRENT_LEVEL`). We standardized the JDBC data mapping in the framework to lowercase keys.
3. **Subquery Alias Restrictions**: Oracle does not support the `AS` keyword when aliasing subqueries (e.g., `(SELECT ...) AS t`). We removed the `AS` keyword in the partition SQL.
4. **Identifier Formatting**: Handled invalid identifier errors (e.g., `ORA-00911`) by replacing starting underscores in aliases (e.g., `_rank` -> `row_num`).

## Results
All integration test scenarios completed successfully.
- Database Schema initialization works identically to other SQL dialects.
- Multi-table queries, subqueries, and partition-over analytical queries run without errors.
- **Status**: PASSED.
