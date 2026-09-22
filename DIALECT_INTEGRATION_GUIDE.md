# 数据库方言接入指南 (Database Dialect Integration Guide)

为了保持架构的纯净性和代码的极简，接入新的数据库方言（如 PG, MySQL, Oracle 等）以及实现表结构自动同步 (`ensureSchema`) 时，**绝对禁止重写庞大的 DDL 比对逻辑**。

请所有的 AI Agent 遵循以下标准实现流程：

## 1. 架构核心思想
TeaQL 的架构分为两层：
- **`teaql-sql-portable` (便携层)**：纯净的 AST 引擎，负责所有标准 SQL 的生成、表结构差异对比、`CREATE TABLE` 和 `ALTER TABLE ... ADD COLUMN` 等通用 DDL 的生成。
- **`teaql-data-service-sql` (JDBC 适配层)**：基于 `SqlExecutionAdapter` 实现的与数据库真实通信的通道。

## 2. 如何实现 `ensureSchema` (表结构同步)
你**不需要**去手工遍历所有列、对比类型、拼接 CREATE/ALTER 语句，这些在 `PortableSQLRepository.ensureSchema()` 中已经完美实现。

在具体的数据库方言执行器（例如 `PostgresDataServiceExecutor`）中，只需执行以下 3 步：

### Step 1: 遍历所有实体模型
只从调用本次 `ensureSchema` 的 `UserContext` 获取已安装的模型。
`SchemaExecutor.Invocation` 必须由该 context 创建，不能让方言层自行绕过：
```java
SchemaExecutor.Invocation.requireContextOwned(invocation);
List<EntityDescriptor> descriptors =
        EntityMetaFactory.requireFrom(context).allEntityDescriptors();
```

不要使用 `EntityMetaFactory.get()`、`registerGlobal(...)` 或静态缓存来选择
schema 描述符。一个进程可以安装多个 Runtime Module / `TeaQLRuntime`；全局元数据会把
另一个 context 的表错误地建到当前数据源。

### Step 2: 包装局部的 `TeaQLDatabase` 提供字典数据
`PortableSQLRepository` 只需要一个底层的 `TeaQLDatabase` 接口来查询表列信息并执行 SQL。请利用 `getExecutionAdapter()` 创建一个包装类：
```java
TeaQLDatabase dbAdapter = new TeaQLDatabase() {
    @Override
    public List<Map<String, Object>> getTableColumns(String tableName) {
        // 【核心】：在这里写该数据库专属的字典查询 SQL
        // 例如 PostgreSQL:
        String sql = "SELECT column_name, data_type FROM information_schema.columns "
                + "WHERE table_name = :tableName AND table_schema = 'public'";
        return getExecutionAdapter().queryForList(
                sql, java.util.Collections.singletonMap("tableName", tableName.toLowerCase()));
    }

    @Override
    public void execute(String sql) {
        getExecutionAdapter().execute(sql);
    }
    // ... 其他 query/update 方法直接委托给 getExecutionAdapter() 即可
};
```

### Step 3: 交给 Portable 引擎执行 DDL
针对每个 `EntityDescriptor`，实例化一个带有该伪装 Adapter 的 PortableRepository，并调用其 `ensureSchema` 方法：
```java
for (EntityDescriptor descriptor : descriptors) {
    // 实例化方言的 PortableSQLRepository（例如 PostgresPortableSQLRepository，如果没有则用基类）
    PortableSQLRepository repository = new PortableSQLRepository(descriptor, dbAdapter, null);
    repository.ensurePhysicalSchema(context);
}
```

应用侧必须显式调用 `context.ensureSchema()`；安装 Runtime Module 只注册能力和元数据，
不能顺带修改生产数据库。新方言的最小验证应包含两个独立 context/metadata 实例，
证明它们只处理自己的实体，再对目标数据库执行 live schema、查询和审计写入测试。

## 3. 核心纪律
1. **彻底解耦 Spring**：在方言模块中，严禁直接使用 `JdbcTemplate` 或任何 `org.springframework` 包。全部通过 `SqlExecutionAdapter` 委托。
2. **职责极简**：方言层（后端层）只负责提供“查询数据字典的原生 SQL”和“JDBC 链接”，表结构的 Diff 对比和通用 DDL 必须收口在 Portable 引擎。
