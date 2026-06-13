package io.teaql.core.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// slq repository will convert the Entity to sql entities
public class SQLEntity {
    public static final String ID = "id";
    Long id;
    Long version;
    List<SQLData> data = new ArrayList<>();
    String traceChain;

    public String getTraceChain() {
        return traceChain;
    }

    public void setTraceChain(String traceChain) {
        this.traceChain = traceChain;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        id = pId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long pVersion) {
        version = pVersion;
    }

    public List<SQLData> getData() {
        return data;
    }

    public void setData(List<SQLData> pData) {
        data = pData;
    }

    public void addPropertySQLData(List<SQLData> data) {
        if (data != null) {
            this.data.addAll(data);
        }
    }

    public void addPropertySQLData(SQLData data) {
        if (data != null) {
            this.data.add(data);
        }
    }

    public Map<String, List<String>> getTableColumnNames() {
        Map<String, List<String>> ret = new HashMap<>();
        for (SQLData datum : data) {
            String tableName = datum.getTableName();
            List<String> columnNames = ret.get(tableName);
            if (columnNames == null) {
                columnNames = new ArrayList<>();
                ret.put(tableName, columnNames);
            }
            columnNames.add(datum.getColumnName());
        }
        return ret;
    }

    public Map<String, List> getTableColumnValues() {
        Map<String, List> ret = new HashMap<>();
        for (SQLData datum : data) {
            String tableName = datum.getTableName();
            List columnValues = ret.get(tableName);
            if (columnValues == null) {
                columnValues = new ArrayList<>();
                ret.put(tableName, columnValues);
            }
            columnValues.add(datum.getValue());
        }
        return ret;
    }

    public boolean allNullExceptID(List pList) {
        if (pList == null) {
            return true;
        }
        // id is not null, we count all non-values
        int notNullCount = 0;
        for (Object o : pList) {
            if (o != null) {
                notNullCount++;
            }
        }
        return notNullCount == 1;
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }
}
