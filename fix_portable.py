import os

path = "teaql-sql-portable/src/main/java"
for root, dirs, files in os.walk(path):
    for file in files:
        if file.endswith(".java"):
            file_path = os.path.join(root, file)
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()
            
            # Replace imports and occurrences
            new_content = content.replace("import io.teaql.core.RepositoryException;", "import io.teaql.core.TeaQLRuntimeException;")
            new_content = new_content.replace("RepositoryException", "TeaQLRuntimeException")
            
            if new_content != content:
                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(new_content)
                print(f"Updated: {file_path}")
