# TeaQL Dynamic Fields JDBC

基于 JDBC 的动态字段持久化实现，复用现有数据库连接。

## 使用场景

绝大多数 TeaQL 应用已经有一个 JDBC 数据源（MySQL、PostgreSQL、SQLite、Oracle 等），本模块直接复用这个数据源存储动态字段，不需要额外的数据库、中间件或连接池。

适用于：

- 已有关系型数据库的项目，不想引入额外存储
- 动态字段数量和访问频率适中（不需要 Redis 级别的性能）
- 需要和业务数据在同一个数据库中（运维简单，备份一致）
- 跨数据库方言的可移植部署

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.teaql</groupId>
    <artifactId>teaql-dynamic-fields-jdbc</artifactId>
</dependency>
```

### 2. 初始化

```java
// 复用现有 DataSource
DataSource ds = existingDataSource();

// 创建 provider
JdbcDynamicFieldsProvider provider = new JdbcDynamicFieldsProvider(ds);

// 自动建表（启动时调用一次）
provider.ensureSchema();

// 注册到 facade
DynamicFieldsFacade facade = new DefaultDynamicFieldsFacade(provider);
userContext.registerCapability(DynamicFieldsFacade.class, facade);
```

### 3. 注册字段定义

```java
DynamicFieldDef def = new DynamicFieldDef();
def.setScope(DynamicFieldScope.global());
def.setOwnerType("Platform");
def.setCode("customer_asset_no");
def.setName("Customer Asset No");
def.setDataType(DynamicDataType.STRING);
def.setStatus(DynamicFieldStatus.ACTIVE);

// ID 由 DynamicFieldContext.nextId() 分配
// runtime 会将 InternalIdGenerationService 注入到 context 中
provider.registerFieldDef(ctx, def);
```

### 4. 读写值

```java
// 通过 facade（推荐）
ctx.dynamicFields()
    .purpose("Update asset number")
    .comment("Setting customer asset")
    .owner("Platform", 1001L)
    .string("customer_asset_no")
    .set("A-10086");

String value = ctx.dynamicFields()
    .owner("Platform", 1001L)
    .string("customer_asset_no")
    .get();
```

### 5. 在查询 DSL 中使用

```java
Q.platformsWithMinimalFields()
    .selectName()
    .selectDynamicFieldsWith(
        DF.fields()
            .selectString("customer_asset_no")
            .selectNumber("priority_score")
    )
    .executeForList(ctx);

// 读取结果
String assetNo = platform.dynamicFields().getString("customer_asset_no");
```

## 表结构

两张内部表，使用 `teaql_` 前缀与业务实体的 `xxx_data` 表区分。

### `teaql_dynamic_field_def` — 字段定义

| 列 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT PK | 由 `InternalIdGenerationService` 分配 |
| `scope_type` | VARCHAR(50) | 作用域类型：GLOBAL / TENANT / PROJECT |
| `scope_id` | VARCHAR(100) | 作用域 ID |
| `owner_type` | VARCHAR(100) | 挂载到哪个实体类型 |
| `code` | VARCHAR(100) | 字段编码（scope+owner+code 唯一） |
| `name` | VARCHAR(200) | 显示名称 |
| `data_type` | VARCHAR(20) | STRING / NUMBER / BOOL / DATE_TIME / ENUM |
| `status` | VARCHAR(20) | DRAFT / ACTIVE / DISABLED / DEPRECATED / DELETED |
| `visible` / `editable` / ... | SMALLINT | 权限控制标志 |
| `version` | BIGINT | 乐观锁 |

唯一索引：`(scope_type, scope_id, owner_type, code)`

### `teaql_dynamic_field_value` — 字段值

| 列 | 类型 | 说明 |
|---|---|---|
| `scope_type` | VARCHAR(50) | 冗余，避免 JOIN |
| `scope_id` | VARCHAR(100) | 冗余，避免 JOIN |
| `owner_type` | VARCHAR(100) | 实体类型 |
| `owner_id` | BIGINT | 主记录 ID |
| `field_id` | BIGINT | → `teaql_dynamic_field_def.id` |
| `string_value` | VARCHAR(4000) | STRING 类型的值 |
| `number_value` | BIGINT | NUMBER 类型的值（整数） |
| `bool_value` | SMALLINT | BOOL 类型的值 |
| `datetime_value` | BIGINT | DATE_TIME 类型的值（epoch millis） |
| `enum_value` | VARCHAR(200) | ENUM 类型的值 |
| `version` | BIGINT | 乐观锁 |

联合主键：`(scope_type, scope_id, owner_type, owner_id, field_id)`
无额外索引（主键前缀已覆盖按 owner 查询）。

## 跨方言兼容

所有列类型都选择了最大兼容性：

| 需求 | 选择 | 原因 |
|------|------|------|
| 布尔值 | `SMALLINT` | Oracle/DB2 不支持 `BOOLEAN` |
| 时间戳 | `BIGINT` (epoch millis) | 避免 `TIMESTAMP` 精度和时区差异 |
| 字符串 | `VARCHAR(4000)` | Oracle VARCHAR2 上限，无需 TEXT/CLOB |
| 建表 | `CREATE TABLE IF NOT EXISTS` | 大多数数据库支持 |
| UPSERT | 先 UPDATE 后 INSERT | 100% 跨方言，不依赖数据库特定语法 |

已验证兼容：SQLite、MySQL、PostgreSQL、Oracle、H2、DuckDB。

## ID 生成

本模块 **不自行管理 ID**。字段定义的 ID 由 `DynamicFieldContext.nextId("DynamicFieldDef")` 分配，runtime 层将 `InternalIdGenerationService` 注入到 context 中。

值表使用联合主键 `(scope_type, scope_id, owner_type, owner_id, field_id)`，无需独立 ID。

## 类清单

| 类 | 职责 |
|---|---|
| `JdbcDynamicFieldsProvider` | 核心实现，实现 `DynamicFieldsProvider` |
| `DynamicFieldsSchema` | DDL 常量 + `ensureSchema()` |

## JPMS

```java
module io.teaql.data.dynamic.jdbc {
    requires io.teaql.data.dynamic;
    requires io.teaql.provider.jdbc;
    requires io.teaql.dataservice.sql;
    requires java.sql;
    requires java.logging;

    exports io.teaql.data.dynamic.jdbc;
}
```
