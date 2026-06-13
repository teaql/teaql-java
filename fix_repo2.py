file_path = "teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/PortableSQLRepository.java"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Replace ctx.info with logInfo
content = content.replace("ctx.info(", "logInfo(")

# Add ensureTableEnabled and logInfo before the last closing brace
helper_methods = """
    protected boolean ensureTableEnabled(UserContext ctx) {
        return ctx.getBool("ensureTable", true);
    }

    private void logInfo(String message) {
        System.out.println("[SQL-PORTABLE] " + message);
    }
}"""

# Replace the last closing brace (assuming it's at the very end of the file)
content = content.rstrip()
if content.endswith("}"):
    content = content[:-1] + helper_methods

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("PortableSQLRepository info logs and table enabled check successfully fixed.")
