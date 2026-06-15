import os
import glob

test_files = [
    "teaql-runtime/src/test/java/io/teaql/runtime/memory/MemoryDatabaseTest.java",
    "teaql-dm8/src/test/java/io/teaql/dm8/Dm8IntegrationTest.java",
    "teaql-sqlite/src/test/java/io/teaql/sqlite/SqliteIntegrationTest.java",
    "teaql-postgres/src/test/java/io/teaql/postgres/PostgresIntegrationTest.java",
    "teaql-mysql/src/test/java/io/teaql/mysql/MysqlIntegrationTest.java",
    "teaql-sql-portable/src/test/java/io/teaql/core/sql/portable/PortableSQLDatabaseTest.java"
]

patch = """
        @Override
        public void internalSet(String property, Object value) {
            switch (property) {
                case "title": this.title = (String) value; break;
                case "status": this.status = (String) value; break;
                default: super.internalSet(property, value);
            }
        }

        @Override
        public Object internalGet(String property) {
            switch (property) {
                case "title": return this.title;
                case "status": return this.status;
                default: return super.internalGet(property);
            }
        }
"""

for filepath in test_files:
    if not os.path.exists(filepath): continue
    with open(filepath, 'r') as f:
        content = f.read()
    
    if "public void internalSet" in content:
        continue
        
    # Find the end of public String typeName() { return "Task"; }
    # and insert the patch before the closing brace of the Task class
    
    target = 'return "Task";\n        }\n'
    if target in content:
        content = content.replace(target, target + patch)
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Patched {filepath}")

