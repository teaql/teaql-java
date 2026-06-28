# JDBC Dynamic Fields Provider 设计（定稿）

## 1. 目标

在现有 JDBC 数据源上实现 `DynamicFieldsProvider`：

- **复用同一个连接** —— 不需要额外的数据库或连接池
- **自动建表** —— 初始化时检测并创建 schema
- **跨方言** —— 兼容 TeaQL 已支持的所有数据库（SQLite、MySQL、Postgres、Oracle、DuckDB 等）
- **零方言适配代码** —— 所有列类型使用跨数据库通用类型

## 2. 模块定位

```
teaql-dynamic-fields-api          ← API 接口 + 内存实现（已完成）
teaql-dynamic-fields-jdbc         ← 本模块：JDBC 实现
teaql-core                        ← bridge
teaql-sql-portable                ← SQL 基础设施（复用）
teaql-provider-jdbc               ← JDBC 执行器（复用）
```

依赖关系：

```
teaql-dynamic-fields-jdbc
    ├── teaql-dynamic-fields-api   (API 契约)
    ├── teaql-sql-portable         (TeaQLDatabase 接口、SQL 工具)
    └── teaql-provider-jdbc        (JdbcSqlExecutor, DataSource 适配)
```

## 3. 接入方式

```java
// 现有业务 wiring
DataSource ds = existingDataSource();
JdbcSqlExecutor executor = new JdbcSqlExecutor(ds);
SqliteDataServiceExecutor sqliteExecutor = new SqliteDataServiceExecutor("main", executor, ds);

// 动态字段 provider 复用同一个 DataSource
JdbcDynamicFieldsProvider dynamicProvider = new JdbcDynamicFieldsProvider(ds);
dynamicProvider.ensureSchema();  // 自动建表

// 注册到 facade
DynamicFieldsFacade facade = new DefaultDynamicFieldsFacade(dynamicProvider);
userContext.registerCapability(DynamicFieldsFacade.class, facade);
```

## 4. 表设计

两张内部表，使用 `teaql_` 前缀，与业务实体的 `xxx_data` 表区分。

### 4.1 字段定义表 `teaql_dynamic_field_def`

```sql
CREATE TABLE IF NOT EXISTS teaql_dynamic_field_def (
    id              BIGINT PRIMARY KEY,          -- via teaql_id_space
    scope_type      VARCHAR(50)  NOT NULL,       -- GLOBAL / TENANT / PROJECT ...
    scope_id        VARCHAR(100) NOT NULL,       -- "default" / tenantId / projectId
    owner_type      VARCHAR(100) NOT NULL,       -- 挂载到哪个实体类型，如 "Platform"
    code            VARCHAR(100) NOT NULL,       -- 字段编码，如 "customer_asset_no"
    name            VARCHAR(200),                -- 显示名称
    description     VARCHAR(500),                -- 描述
    data_type       VARCHAR(20)  NOT NULL,       -- STRING / NUMBER / BOOL / DATE_TIME / ENUM
    logical_type    VARCHAR(30),                 -- PLAIN_TEXT / EMAIL / CURRENCY ... 可空
    required        SMALLINT DEFAULT 0,
    visible         SMALLINT DEFAULT 1,
    editable        SMALLINT DEFAULT 1,
    filterable      SMALLINT DEFAULT 0,
    sortable        SMALLINT DEFAULT 0,
    searchable      SMALLINT DEFAULT 0,
    exportable      SMALLINT DEFAULT 0,
    importable      SMALLINT DEFAULT 0,
    auditable       SMALLINT DEFAULT 1,
    privacy_level   VARCHAR(50),                 -- 可空
    mask_rule       VARCHAR(200),                -- 可空
    default_value   VARCHAR(500),                -- 可空
    status          VARCHAR(20)  NOT NULL,       -- DRAFT / ACTIVE / DISABLED / DEPRECATED / DELETED
    display_order   INTEGER DEFAULT 0,
    version         BIGINT  DEFAULT 1,
    created_by      VARCHAR(100),
    created_at      BIGINT,                      -- epoch millis
    updated_by      VARCHAR(100),
    updated_at      BIGINT                       -- epoch millis
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tdfd_scope_owner_code
    ON teaql_dynamic_field_def (scope_type, scope_id, owner_type, code);
```

### 4.2 字段值表 `teaql_dynamic_field_value`

```sql
CREATE TABLE IF NOT EXISTS teaql_dynamic_field_value (
    scope_type      VARCHAR(50)  NOT NULL,       -- 冗余：避免 JOIN def 表即可按 scope 过滤
    scope_id        VARCHAR(100) NOT NULL,       -- 冗余：同上
    owner_type      VARCHAR(100) NOT NULL,       -- 实体类型
    owner_id        BIGINT       NOT NULL,       -- 主记录 ID
    field_id        BIGINT       NOT NULL,       -- → teaql_dynamic_field_def.id
    string_value    VARCHAR(4000),               -- data_type=STRING 时使用
    number_value    BIGINT,                      -- data_type=NUMBER 时使用（整数）
    bool_value      SMALLINT,                    -- data_type=BOOL 时使用
    datetime_value  BIGINT,                      -- data_type=DATE_TIME 时使用，epoch millis
    enum_value      VARCHAR(200),                -- data_type=ENUM 时使用
    version         BIGINT  DEFAULT 1,
    updated_by      VARCHAR(100),
    updated_at      BIGINT,                      -- epoch millis
    PRIMARY KEY (scope_type, scope_id, owner_type, owner_id, field_id)
);
```

> **不建额外索引。** 联合主键的前缀 `(scope_type, scope_id, owner_type, owner_id)` 已经覆盖按 owner 批量查询的场景。

### 4.3 设计决策总结

| 决策 | 结论 | 理由 |
|------|------|------|
| 值表方案 | **统一表** | 1 次查询装载所有字段，批量效率最高 |
| number 精度 | **BIGINT** | MVP 阶段只支持整数，跨方言最安全 |
| string 存储 | **VARCHAR(4000)** | Oracle VARCHAR2 上限，无需 TEXT/CLOB 方言适配 |
| BOOLEAN 存储 | **SMALLINT** | Oracle/DB2 不支持 BOOLEAN 列类型 |
| 时间戳存储 | **BIGINT (epoch millis)** | 跨方言最安全，无时区/精度问题 |
| UPSERT 策略 | **先 UPDATE 后 INSERT** | 100% 跨方言兼容 |
| ID 生成 | **teaql_id_space**（仅 def 表） | value 表无独立 ID，联合主键 |
| value 表主键 | **联合主键** (scope+owner+field) | 值依附于主记录，无需独立 ID |
| value 表 created_by/at | **不加** | 生命周期跟随主记录 |
| scope 冗余 | **冗余在 value 表** | 避免 JOIN def 表的简单查询 |
| 额外 owner 索引 | **不加** | 主键前缀已覆盖 |
| 表名前缀 | **teaql_** | 框架内部表，与 `xxx_data` 业务表区分 |
| 模块名 | **teaql-dynamic-fields-jdbc** | 明确底层技术 |

## 5. 核心 SQL

### 5.1 字段定义 CRUD

```sql
-- 按 scope + ownerType + code 查询
SELECT * FROM teaql_dynamic_field_def
WHERE scope_type = ? AND scope_id = ? AND owner_type = ? AND code = ?

-- 列出某 ownerType 的所有定义
SELECT * FROM teaql_dynamic_field_def
WHERE scope_type = ? AND scope_id = ? AND owner_type = ?
ORDER BY display_order

-- 插入（ID 由 teaql_id_space 生成）
INSERT INTO teaql_dynamic_field_def (
    id, scope_type, scope_id, owner_type, code, name, description,
    data_type, logical_type, required, visible, editable,
    filterable, sortable, searchable, exportable, importable, auditable,
    privacy_level, mask_rule, default_value, status, display_order,
    version, created_by, created_at, updated_by, updated_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?)

-- 更新（乐观锁）
UPDATE teaql_dynamic_field_def
SET name=?, description=?, status=?, ..., version=version+1, updated_by=?, updated_at=?
WHERE id = ? AND version = ?
```

### 5.2 值读写

```sql
-- 按单个 owner 读取（核心查询）
SELECT v.field_id, d.code, d.data_type,
       v.string_value, v.number_value, v.bool_value, v.datetime_value, v.enum_value
FROM teaql_dynamic_field_value v
JOIN teaql_dynamic_field_def d ON v.field_id = d.id
WHERE v.scope_type = ? AND v.scope_id = ?
  AND v.owner_type = ? AND v.owner_id = ?

-- 按多个 owner 批量读取（post-load 核心路径）
SELECT v.owner_id, v.field_id, d.code, d.data_type,
       v.string_value, v.number_value, v.bool_value, v.datetime_value, v.enum_value
FROM teaql_dynamic_field_value v
JOIN teaql_dynamic_field_def d ON v.field_id = d.id
WHERE v.scope_type = ? AND v.scope_id = ?
  AND v.owner_type = ? AND v.owner_id IN (?, ?, ?, ...)

-- 写入：先 UPDATE
UPDATE teaql_dynamic_field_value
SET string_value=?, number_value=?, bool_value=?, datetime_value=?, enum_value=?,
    version=version+1, updated_by=?, updated_at=?
WHERE scope_type=? AND scope_id=? AND owner_type=? AND owner_id=? AND field_id=?

-- 写入：UPDATE 影响 0 行则 INSERT
INSERT INTO teaql_dynamic_field_value (
    scope_type, scope_id, owner_type, owner_id, field_id,
    string_value, number_value, bool_value, datetime_value, enum_value,
    version, updated_by, updated_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)

-- 删除值
DELETE FROM teaql_dynamic_field_value
WHERE scope_type=? AND scope_id=? AND owner_type=? AND owner_id=? AND field_id=?
```

## 6. Schema 自动创建

```java
public void ensureSchema() {
    // 1. teaql_id_space（可能已存在，复用已有的）
    tryExecute(DDL_ID_SPACE);

    // 2. teaql_dynamic_field_def + 唯一索引
    tryExecute(DDL_FIELD_DEF);
    tryExecute(IDX_FIELD_DEF_UK);

    // 3. teaql_dynamic_field_value（联合主键，无额外索引）
    tryExecute(DDL_FIELD_VALUE);

    LOG.info("Dynamic fields schema ensured.");
}

private void tryExecute(String ddl) {
    try {
        database.execute(ddl);
    } catch (Exception e) {
        // 已存在，忽略
        LOG.fine("Schema element may already exist: " + e.getMessage());
    }
}
```

## 7. ID 生成

仅 `teaql_dynamic_field_def` 需要 ID 生成，复用 `teaql_id_space`：

```sql
-- 初始化（ensureSchema 时插入，忽略已存在）
INSERT INTO teaql_id_space (type_name, current_level) VALUES ('DynamicFieldDef', 100000)

-- 分配 ID
UPDATE teaql_id_space SET current_level = current_level + 1 WHERE type_name = 'DynamicFieldDef'
SELECT current_level FROM teaql_id_space WHERE type_name = 'DynamicFieldDef'
```

`teaql_dynamic_field_value` 无需 ID 生成 —— 联合主键 `(scope_type, scope_id, owner_type, owner_id, field_id)` 即是唯一标识。

## 8. Post-Load 集成

动态字段装载发生在主查询之后（在 `teaql-runtime` 中实现，不在本模块）：

```java
// enhance 阶段
SmartList<T> results = internalExecuteForList(request);

DynamicFieldSelection dfSelection = request.getDynamicFieldSelection();
if (dfSelection != null && !results.isEmpty()) {
    DynamicFieldsFacade facade = ctx.dynamicFields();

    // 收集 owner ids
    List<DynamicOwnerRef> ownerRefs = results.stream()
        .map(e -> DynamicOwnerRef.of(request.getTypeName(), e.getId()))
        .collect(toList());

    // 批量加载（1 次 SQL：WHERE owner_id IN (...)）
    Map<DynamicOwnerRef, DynamicFieldValues> valuesMap =
        provider.loadValues(ctx, ownerRefs, dfSelection);

    // 装载到 entity
    for (T entity : results) {
        DynamicOwnerRef ref = DynamicOwnerRef.of(request.getTypeName(), entity.getId());
        DynamicFieldValues values = valuesMap.getOrDefault(ref, DynamicFieldValues.empty());
        ((BaseEntity) entity).setDynamicFieldValues(values);
    }
}
```

## 9. 类清单

```
teaql-dynamic-fields-jdbc/
└── io.teaql.data.dynamic.jdbc
    ├── JdbcDynamicFieldsProvider.java   ← 核心：实现 DynamicFieldsProvider
    ├── DynamicFieldsSchema.java         ← DDL 常量 + ensureSchema()
    └── DynamicFieldsIdGenerator.java    ← 复用 teaql_id_space 的 ID 生成
```
