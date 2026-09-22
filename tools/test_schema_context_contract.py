from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]
SCHEMA_EXECUTORS = (
    "teaql-android/src/main/java/io/teaql/android/AndroidSqliteDataServiceExecutor.java",
    "teaql-db2/src/main/java/io/teaql/core/db2/DB2DataServiceExecutor.java",
    "teaql-dm8/src/main/java/io/teaql/core/dm8/Dm8DataServiceExecutor.java",
    "teaql-duckdb/src/main/java/io/teaql/core/duck/DuckDataServiceExecutor.java",
    "teaql-hana/src/main/java/io/teaql/core/hana/HanaDataServiceExecutor.java",
    "teaql-mssql/src/main/java/io/teaql/core/mssql/MssqlDataServiceExecutor.java",
    "teaql-mysql/src/main/java/io/teaql/core/mysql/MysqlDataServiceExecutor.java",
    "teaql-oracle/src/main/java/io/teaql/core/oracle/OracleDataServiceExecutor.java",
    "teaql-postgres/src/main/java/io/teaql/core/postgres/PostgresDataServiceExecutor.java",
    "teaql-snowflake/src/main/java/io/teaql/core/snowflake/SnowflakeDataServiceExecutor.java",
    "teaql-sqlite/src/main/java/io/teaql/core/sqlite/SqliteDataServiceExecutor.java",
)


class SchemaContextContractTest(unittest.TestCase):
    def test_every_schema_executor_uses_invoking_context_metadata(self):
        for relative_path in SCHEMA_EXECUTORS:
            with self.subTest(executor=relative_path):
                source = (ROOT / relative_path).read_text(encoding="utf-8")
                self.assertIn("EntityMetaFactory.requireFrom(context)", source)
                self.assertNotIn("EntityMetaFactory.get().allEntityDescriptors()", source)


if __name__ == "__main__":
    unittest.main()
