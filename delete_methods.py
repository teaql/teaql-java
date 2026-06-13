import re

file_path = "/home/philip/githome/teaql-java/teaql-sql/src/main/java/io/teaql/core/sql/SQLRepository.java"

with open(file_path, "r") as f:
    lines = f.readlines()

methods_to_remove = [
    "collectDataTables",
    "tableAlias",
    "joinTables",
    "collectSelectSql",
    "prepareOrderBy",
    "prepareCondition",
    "collectAggregationSelectSql",
    "collectAggregationGroupBySql",
    "collectAggregationTables"
]

for method in methods_to_remove:
    # find line with method signature
    start_line = -1
    for i, line in enumerate(lines):
        if method + "(" in line and ("public" in line or "protected" in line or "private" in line):
            start_line = i
            break
            
    if start_line == -1:
        continue
        
    # find open brace
    brace_count = 0
    in_method = False
    end_line = -1
    
    for i in range(start_line, len(lines)):
        brace_count += lines[i].count('{')
        brace_count -= lines[i].count('}')
        if lines[i].count('{') > 0:
            in_method = True
            
        if in_method and brace_count == 0:
            end_line = i
            break
            
    if start_line != -1 and end_line != -1:
        print(f"sed -i '{start_line+1},{end_line+1}d' {file_path}")
