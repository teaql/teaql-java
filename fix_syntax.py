import re

with open('teaql-data-service-sql/src/main/java/io/teaql/dataservice/sql/BackendSQLRepository.java', 'r') as f:
    content = f.read()

# Fix hanging strings from /* ctx.info */ "msg";
content = re.sub(r'/\*\s*ctx\.(info|warn)\s*\*/\s*(.*?);', r'', content)

with open('teaql-data-service-sql/src/main/java/io/teaql/dataservice/sql/BackendSQLRepository.java', 'w') as f:
    f.write(content)
