package io.teaql.core.sql.portable;

import io.teaql.core.*;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.sql.SQLData;
import io.teaql.core.sql.SQLEntity;
import io.teaql.core.sql.SqlAstCompiler;
import io.teaql.core.utils.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles all write operations for PortableSQLRepository.
 * Responsibilities:
 * - INSERT operations
 * - UPDATE operations
 * - DELETE (soft-delete) operations
 * - RECOVER operations
 */
public class PortableSQLWriter<T extends Entity> {

    private final PortableSQLRepository<T> repository;
    private final TeaQLDatabase database;

    public PortableSQLWriter(PortableSQLRepository<T> repository, TeaQLDatabase database) {
        this.repository = repository;
        this.database = database;
    }

    /**
     * Insert new entities into the database.
     */
    public void createInternal(UserContext userContext, Collection<T> createItems) {
        List<SQLEntity> sqlEntities = CollectionUtil.map(createItems,
                i -> convertToSQLEntityForInsert(userContext, i), true);
        if (ObjectUtil.isEmpty(sqlEntities)) return;

        SQLEntity sqlEntity = sqlEntities.get(0);
        Map<String, List<String>> tableColumns = sqlEntity.getTableColumnNames();

        Map<String, List<Object[]>> rows = new HashMap<>();
        for (SQLEntity entity : sqlEntities) {
            Map<String, List> tableColumnValues = entity.getTableColumnValues();
            for (Map.Entry<String, List> entry : tableColumnValues.entrySet()) {
                String k = entry.getKey();
                List v = entry.getValue();
                List<Object[]> values = rows.computeIfAbsent(k, key -> new ArrayList<>());
                if (repository.getAuxiliaryTableNames().contains(k) && entity.allNullExceptID(v)) continue;
                values.add(v.toArray());
            }
        }

        TreeMap<String, List<Object[]>> sorted = MapUtil.sort(rows, (t1, t2) -> {
            if (t1.equals(repository.getVersionTableName())) return -1;
            if (t2.equals(repository.getVersionTableName())) return 1;
            return 0;
        });

        sorted.forEach((k, v) -> {
            if (v.isEmpty()) return;
            List<String> columns = tableColumns.get(k);
            SqlAstCompiler compiler = new SqlAstCompiler();
            String sql = compiler.buildInsertSQL(repository, k, columns, sqlEntity.getTraceChain());
            database.batchUpdate(userContext, sql, v);
        });
    }

    /**
     * Update existing entities in the database.
     */
    public void updateInternal(UserContext userContext, Collection<T> updateItems) {
        if (ObjectUtil.isEmpty(updateItems)) return;
        List<SQLEntity> sqlEntities = CollectionUtil.map(updateItems,
                i -> convertToSQLEntityForUpdate(userContext, i), true);
        if (ObjectUtil.isEmpty(sqlEntities)) return;

        for (SQLEntity sqlEntity : sqlEntities) {
            if (sqlEntity.isEmpty()) continue;
            Map<String, List<String>> tableColumnNames = sqlEntity.getTableColumnNames();
            Map<String, List> tableColumnValues = sqlEntity.getTableColumnValues();

            AtomicBoolean versionTableUpdated = new AtomicBoolean(false);
            tableColumnValues.forEach((k, v) -> {
                List<String> columns = new ArrayList<>(tableColumnNames.get(k));
                List l = new ArrayList(v);
                boolean versionTable = repository.getVersionTableName().equals(k);
                boolean primaryTable = repository.getPrimaryTableNames().contains(k);

                if (versionTable) {
                    updateVersionTable(userContext, sqlEntity, versionTableUpdated, k, columns, l);
                    return;
                }
                if (primaryTable) {
                    updatePrimaryTable(userContext, sqlEntity, k, columns, l);
                    return;
                }
                String updateSql = repository.getDialect().buildSubsidiaryInsertSql(k, columns);
                database.executeUpdate(userContext, updateSql, l.toArray());
            });

            if (!versionTableUpdated.get()) {
                updateVersionTableVersion(userContext, sqlEntity);
            }
        }
    }

    /**
     * Soft-delete entities by negating version.
     */
    public void deleteInternal(UserContext userContext, Collection<T> entities) {
        if (ObjectUtil.isEmpty(entities)) return;
        SqlAstCompiler compiler = new SqlAstCompiler();
        String updateSql = compiler.buildDeleteSQL(repository, repository.getVersionTableName());
        List<Object[]> args = entities.stream()
                .filter(e -> e.getVersion() > 0)
                .map(e -> new Object[]{-(e.getVersion() + 1), e.getId(), e.getVersion()})
                .collect(Collectors.toList());
        int[] rets = database.batchUpdate(userContext, updateSql, args);
        for (int ret : rets) {
            if (ret != 1) throw new ConcurrentModifyException();
        }
    }

    /**
     * Recover soft-deleted entities.
     */
    public void recoverInternal(UserContext userContext, Collection<T> entities) {
        if (ObjectUtil.isEmpty(entities)) return;
        SqlAstCompiler compiler = new SqlAstCompiler();
        String updateSql = compiler.buildDeleteSQL(repository, repository.getVersionTableName());
        List<Object[]> args = entities.stream()
                .filter(e -> e.getVersion() < 0)
                .map(e -> new Object[]{(-e.getVersion() + 1), e.getId(), e.getVersion()})
                .collect(Collectors.toList());
        int[] rets = database.batchUpdate(userContext, updateSql, args);
        for (int ret : rets) {
            if (ret != 1) throw new ConcurrentModifyException();
        }
    }

    // ==========================================
    // Update helpers
    // ==========================================

    private void updateVersionTable(UserContext userContext, SQLEntity sqlEntity,
                                     AtomicBoolean versionTableUpdated, String k, List<String> columns, List l) {
        versionTableUpdated.set(true);
        columns.add("version");
        l.add(sqlEntity.getVersion() + 1);
        l.add(sqlEntity.getId());
        l.add(sqlEntity.getVersion());
        SqlAstCompiler compiler = new SqlAstCompiler();
        String updateSql = compiler.buildUpdateVersionSQL(repository, k, columns, sqlEntity.getTraceChain());
        int update = database.executeUpdate(userContext, updateSql, l.toArray());
        if (update != 1) throw new ConcurrentModifyException();
    }

    private void updatePrimaryTable(UserContext userContext, SQLEntity sqlEntity, String k, List<String> columns, List l) {
        l.add(sqlEntity.getId());
        SqlAstCompiler compiler = new SqlAstCompiler();
        String updateSql = compiler.buildUpdatePrimarySQL(repository, k, columns, sqlEntity.getTraceChain());
        int update = database.executeUpdate(userContext, updateSql, l.toArray());
        if (update != 1) throw new TeaQLRuntimeException("primary table update failed");
    }

    private void updateVersionTableVersion(UserContext userContext, SQLEntity sqlEntity) {
        SqlAstCompiler compiler = new SqlAstCompiler();
        String updateSql = compiler.buildUpdateVersionTableVersionSQL(repository, repository.getVersionTableName());
        Object[] parameters = {sqlEntity.getVersion() + 1, sqlEntity.getId(), sqlEntity.getVersion()};
        int update = database.executeUpdate(userContext, updateSql, parameters);
        if (update != 1) throw new ConcurrentModifyException();
    }

    // ==========================================
    // Entity conversion
    // ==========================================

    private SQLEntity convertToSQLEntityForInsert(UserContext userContext, T entity) {
        SQLEntity sqlEntity = new SQLEntity();
        sqlEntity.setId(entity.getId());
        sqlEntity.setVersion(entity.getVersion());
        for (PropertyDescriptor pd : repository.getAllProperties()) {
            if (pd instanceof io.teaql.core.meta.Relation && !repository.shouldHandle((io.teaql.core.meta.Relation) pd)) continue;
            Object v = entity.getProperty(pd.getName());
            List<SQLData> data = convertToSQLData(userContext, entity, pd, v);
            sqlEntity.addPropertySQLData(data);
        }
        for (int i = 0; i < repository.getTypes().size() - 1; i++) {
            String tableName = repository.getPrimaryTableNames().get(i + 1);
            String type = repository.getTypes().get(i);
            SQLData childTypeCell = new SQLData();
            childTypeCell.setTableName(tableName);
            childTypeCell.setColumnName(repository.getChildType());
            childTypeCell.setValue(type);
            sqlEntity.addPropertySQLData(childTypeCell);
        }
        return sqlEntity;
    }

    private SQLEntity convertToSQLEntityForUpdate(UserContext userContext, T entity) {
        List<String> updatedProperties = entity.getUpdatedProperties();
        if (ObjectUtil.isEmpty(updatedProperties)) return null;
        SQLEntity sqlEntity = new SQLEntity();
        sqlEntity.setId(entity.getId());
        sqlEntity.setVersion(entity.getVersion());
        for (String updatedProperty : updatedProperties) {
            PropertyDescriptor property = repository.findProperty(updatedProperty);
            if (property.isId() || property.isVersion()) continue;
            Object v = entity.getProperty(property.getName());
            List<SQLData> data = convertToSQLData(userContext, entity, property, v);
            sqlEntity.addPropertySQLData(data);
        }
        return sqlEntity;
    }

    private List<SQLData> convertToSQLData(UserContext ctx, T entity, PropertyDescriptor property, Object value) {
        return SQLPropertyUtil.toDBRaw(ctx, entity, value, property);
    }
}
