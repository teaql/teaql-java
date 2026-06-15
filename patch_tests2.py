import os

test_files = [
    "teaql-dm8/src/test/java/io/teaql/dm8/Dm8IntegrationTest.java",
    "teaql-sqlite/src/test/java/io/teaql/sqlite/SqliteIntegrationTest.java",
    "teaql-postgres/src/test/java/io/teaql/postgres/PostgresIntegrationTest.java",
    "teaql-mysql/src/test/java/io/teaql/mysql/MysqlIntegrationTest.java"
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
        
    target = 'return "Task";\n'
    if target in content:
        # replace the first '}' after this target
        parts = content.split(target)
        # parts[0] + target + parts[1]
        
        # find first '}' in parts[1]
        idx = parts[1].find('}')
        if idx != -1:
            new_part1 = parts[1][:idx+1] + patch + parts[1][idx+1:]
            content = parts[0] + target + new_part1
            with open(filepath, 'w') as f:
                f.write(content)
            print(f"Patched {filepath}")

