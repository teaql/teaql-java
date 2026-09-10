package io.teaql.core.sql.portable;

import io.teaql.core.BaseEntity;
import io.teaql.core.Entity;
import io.teaql.core.TeaQLRuntimeException;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.sql.SQLColumn;
import io.teaql.core.sql.dialect.SqlDialect;
import io.teaql.core.utils.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles schema management operations for PortableSQLRepository.
 * Responsibilities:
 * - Creating tables
 * - Adding columns
 * - Ensuring initial data (root entities, constants)
 * - Managing ID space table
 */
public class PortableSchemaManager<T extends Entity> {

    private final PortableSQLRepository<T> repository;
    private final EntityDescriptor entityDescriptor;
    private final TeaQLDatabase database;
    private final SqlDialect dialect;

    public PortableSchemaManager(PortableSQLRepository<T> repository,
                                EntityDescriptor entityDescriptor,
                                TeaQLDatabase database,
                                SqlDialect dialect) {
        this.repository = repository;
        this.entityDescriptor = entityDescriptor;
        this.database = database;
        this.dialect = dialect;
    }

    /**
     * Ensure the schema matches the entity descriptor.
     * Creates tables if they don't exist, adds missing columns.
     */
    public void ensureSchema(UserContext ctx) {
        List<SQLColumn> allColumns = new ArrayList<>();
        for (PropertyDescriptor ownProperty : entityDescriptor.getOwnProperties()) {
            allColumns.addAll(SQLPropertyUtil.getColumns(ownProperty));
        }
        if (entityDescriptor.hasChildren()) {
            SQLColumn childTypeCell = new SQLColumn(repository.getThisPrimaryTableName(), repository.getChildType());
            childTypeCell.setType(repository.getChildSqlType());
            allColumns.add(childTypeCell);
        }

        Map<String, List<SQLColumn>> tableColumns = CollStreamUtil.groupByKey(allColumns, SQLColumn::getTableName);
        tableColumns.forEach((table, columns) -> {
            List<Map<String, Object>> dbTableInfo;
            try {
                dbTableInfo = database.getTableColumns(table);
            } catch (Exception e) {
                dbTableInfo = ListUtil.empty();
            }
            ensure(ctx, dbTableInfo, table, columns);
        });

        ensureInitData(ctx);
        ensureIdSpaceTable(ctx);
    }

    /**
     * Ensure the ID space table exists.
     */
    public void ensureIdSpaceTable(UserContext ctx) {
        List<Map<String, Object>> dbTableInfo;
        try {
            dbTableInfo = database.getTableColumns(repository.getTqlIdSpaceTable());
        } catch (Exception e) {
            dbTableInfo = ListUtil.empty();
        }
        if (!ObjectUtil.isEmpty(dbTableInfo)) return;

        String sql = "CREATE TABLE " + repository.getTqlIdSpaceTable() + " (\n"
                + "type_name varchar(100) PRIMARY KEY,\n"
                + "current_level bigint)\n";
        logInfo(sql + ";");
        if (ensureTableEnabled(ctx)) {
            try { database.execute(ctx, sql); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
        }
    }

    // ==========================================
    // Table management
    // ==========================================

    private void ensure(UserContext ctx, List<Map<String, Object>> tableInfo, String table, List<SQLColumn> columns) {
        if (tableInfo.isEmpty()) {
            createTable(ctx, table, columns);
            return;
        }
        Map<String, Map<String, Object>> fields = CollStreamUtil.toIdentityMap(
                tableInfo, m -> String.valueOf(m.get("column_name")).toLowerCase());
        for (SQLColumn column : columns) {
            String dbColumnName = column.getColumnName().toLowerCase();
            if (!fields.containsKey(dbColumnName)) {
                addColumn(ctx, column);
            }
        }
    }

    private void createTable(UserContext ctx, String table, List<SQLColumn> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(table).append(" (\n");
        sb.append(columns.stream()
                .map(column -> {
                    String dbColumn = dialect.escapeIdentifier(column.getColumnName()) + " " + dialect.mapColumnType(column.getType());
                    if (column.isIdColumn()) dbColumn += " PRIMARY KEY";
                    return dbColumn;
                })
                .collect(Collectors.joining(",\n")));
        sb.append(")\n");
        logInfo(sb + ";");
        if (ensureTableEnabled(ctx)) {
            try { database.execute(ctx, sb.toString()); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
        }
    }

    private void addColumn(UserContext ctx, SQLColumn column) {
        String sql = StrUtil.format("ALTER TABLE {} ADD COLUMN {} {}",
                dialect.escapeIdentifier(column.getTableName()),
                dialect.escapeIdentifier(column.getColumnName()),
                dialect.mapColumnType(column.getType()));
        logInfo(sql + ";");
        if (ensureTableEnabled(ctx)) {
            try { database.execute(ctx, sql); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
        }
    }

    // ==========================================
    // Initial data management
    // ==========================================

    private void ensureInitData(UserContext ctx) {
        if (entityDescriptor.isRoot()) ensureRoot(ctx);
        if (entityDescriptor.isConstant()) ensureConstant(ctx);
    }

    private void ensureRoot(UserContext ctx) {
        List<Map<String, Object>> dbRow;
        try {
            dbRow = database.query(ctx,
                    StrUtil.format("SELECT * FROM {} WHERE id = '1'", repository.tableName(entityDescriptor.getType())),
                    new Object[0]);
        } catch (Exception e) {
            dbRow = ListUtil.empty();
        }

        if (!dbRow.isEmpty()) {
            long version = Long.parseLong(String.valueOf(dbRow.get(0).get("version")));
            if (version > 0) return;
            String sql = StrUtil.format("UPDATE {} SET version = {} where id = '1'",
                    repository.tableName(entityDescriptor.getType()), -version);
            logInfo(sql + ";");
            if (ensureTableEnabled(ctx)) {
                try { database.execute(ctx, sql); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
            }
            return;
        }

        List<String> columns = new ArrayList<>();
        List<Object> rootRow = new ArrayList<>();
        for (PropertyDescriptor ownProperty : entityDescriptor.getOwnProperties()) {
            columns.add(repository.getSqlColumn(ownProperty).getColumnName());
            rootRow.add(getRootPropertyValue(ctx, ownProperty));
        }
        String sql = StrUtil.format("INSERT INTO {} ({}) VALUES ({})",
                repository.tableName(entityDescriptor.getType()),
                CollectionUtil.join(columns, ","),
                CollectionUtil.join(rootRow, ",", value -> getSqlValue(value)));
        logInfo(sql + ";");
        if (ensureTableEnabled(ctx)) {
            try { database.execute(ctx, sql); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
        }
    }

    private void ensureConstant(UserContext ctx) {
        PropertyDescriptor identifier = entityDescriptor.getIdentifier();
        List<String> candidates = identifier.getCandidates();
        List<PropertyDescriptor> ownProperties = entityDescriptor.getOwnProperties();
        List<String> columns = ownProperties.stream()
                .map(p -> repository.getSqlColumn(p).getColumnName())
                .collect(Collectors.toList());

        for (int idx = 0; idx < candidates.size(); idx++) {
            final int i = idx;
            String code = candidates.get(i);
            List<Object> oneConstant = ownProperties.stream()
                    .map(p -> getConstantPropertyValue(ctx, p, i, code))
                    .collect(Collectors.toList());

            try {
                List<Map<String, Object>> existing = database.query(ctx,
                        StrUtil.format("SELECT * FROM {} WHERE id = '{}'",
                                repository.tableName(entityDescriptor.getType()),
                                getConstantPropertyValue(ctx, entityDescriptor.findIdProperty(), i, code)),
                        new Object[0]);
                if (!existing.isEmpty()) {
                    long version = Long.parseLong(String.valueOf(existing.get(0).get("version")));
                    if (version > 0) continue;
                    String sql = StrUtil.format("UPDATE {} SET version = {} where id = '{}'",
                            repository.tableName(entityDescriptor.getType()), -version,
                            getConstantPropertyValue(ctx, entityDescriptor.findIdProperty(), i, code));
                    logInfo(sql + ";");
                    if (ensureTableEnabled(ctx)) {
                        try { database.execute(ctx, sql); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
                    }
                    continue;
                }
            } catch (Exception ignored) {
            }

            String sql = StrUtil.format("INSERT INTO {} ({}) VALUES ({})",
                    repository.tableName(entityDescriptor.getType()),
                    CollectionUtil.join(columns, ","),
                    CollectionUtil.join(oneConstant, ",", value -> getSqlValue(value)));
            logInfo(sql + ";");
            if (ensureTableEnabled(ctx)) {
                try { database.execute(ctx, sql); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
            }
        }
    }

    // ==========================================
    // Helper methods
    // ==========================================

    private Object getRootPropertyValue(UserContext ctx, PropertyDescriptor property) {
        if (property.isId()) return 1L;
        if (property.isVersion()) return 1L;
        String createFunction = property.getAdditionalInfo().get("createFunction");
        if (!ObjectUtil.isEmpty(createFunction)) return ctx.evaluate(createFunction);
        return property.getAdditionalInfo().get("candidates");
    }

    private Object getConstantPropertyValue(UserContext ctx, PropertyDescriptor property, int index, String identifier) {
        if (property.isVersion()) return 1L;
        if (BaseEntity.class.isAssignableFrom(property.getType().javaType())) return "1";
        String createFunction = property.getAdditionalInfo().get("createFunction");
        if (!ObjectUtil.isEmpty(createFunction)) return ctx.evaluate(createFunction);
        List<String> candidates = property.getCandidates();
        if (property.isIdentifier()) return identifier;
        if (property.isId()) return Math.abs((long) identifier.toUpperCase().hashCode());
        if (ObjectUtil.isNotEmpty(candidates)) return CollectionUtil.get(candidates, index);
        return null;
    }

    protected String getSqlValue(Object value) {
        if (value == null) return "NULL";
        if (value instanceof Number) return String.valueOf(value);
        if (value instanceof Boolean) return ((Boolean) value) ? "1" : "0";
        return StrUtil.wrapIfMissing(String.valueOf(value), "'", "'");
    }

    protected boolean ensureTableEnabled(UserContext ctx) {
        return ctx.getBool("ensureTable", true);
    }

    private void logInfo(String message) {
        System.out.println("[SQL-PORTABLE] " + message);
    }
}
