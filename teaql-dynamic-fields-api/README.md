# TeaQL Dynamic Fields API

TeaQL 运行期领域扩展模型的 API 契约层与默认内存实现。

## 定位

Dynamic Fields 允许业务系统在不修改标准领域模型的情况下，为业务对象增加受控的自定义字段。它不是裸 key-value，也不是无约束 EAV —— 每个动态字段都有定义、类型、权限、校验和生命周期。

适用场景：

- SaaS 多租户自定义字段
- 插件化系统 / 低代码配置
- 客户现场部署的项目级扩展
- 字段还没成熟到进入全局领域模型

## 模块结构

```
teaql-dynamic-fields-api            ← 本模块（零外部依赖）
├── API 接口与值对象 (20 个类)
├── InMemoryDynamicFieldsProvider   ← 内存实现（仅用于演示/测试）
└── DefaultDynamicFieldsFacade      ← 通用 facade（可复用于任何 provider）

teaql-dynamic-fields-jdbc            ← JDBC 持久化实现（已完成）
├── JdbcDynamicFieldsProvider       ← 复用现有 DataSource，自动建表
├── DynamicFieldsSchema             ← DDL 常量 + ensureSchema()
└── 两张内部表: teaql_dynamic_field_def / teaql_dynamic_field_value

teaql-core                           ← 桥接层
├── UserContext.dynamicFields()     ← 通过 capability() 委托
├── Entity.dynamicFields()          ← DynamicFieldValues wrapper
├── SearchRequest.getDynamicFieldSelection()
└── BaseRequest.selectDynamicFieldsWith(...)
```

## 快速开始

### 1. 注册字段定义

```java
InMemoryDynamicFieldsProvider provider = new InMemoryDynamicFieldsProvider();

DynamicFieldDef def = new DynamicFieldDef();
def.setScope(DynamicFieldScope.global());
def.setOwnerType("Platform");
def.setCode("customer_asset_no");
def.setName("Customer Asset No");
def.setDataType(DynamicDataType.STRING);
def.setStatus(DynamicFieldStatus.ACTIVE);
provider.registerFieldDef(def);
```

### 2. 通过 Facade 读写

```java
DefaultDynamicFieldsFacade facade = new DefaultDynamicFieldsFacade(provider);

// 写
facade.purpose("Update asset number")
      .comment("Setting customer asset")
      .owner("Platform", 1001L)
      .string("customer_asset_no")
      .set("A-10086");

// 读
String value = facade.owner("Platform", 1001L)
                     .string("customer_asset_no")
                     .get();
```

### 3. 在查询 DSL 中使用

```java
Q.platformsWithMinimalFields()
    .selectName()
    .selectCreateTime()
    .selectDynamicFieldsWith(
        DF.fields()
            .selectString("customer_asset_no")
            .selectBool("enabled_for_custom_flow")
            .selectNumber("priority_score")
    )
    .comment("List platforms with dynamic fields")
    .purpose("Load platform records with scope-defined dynamic fields")
    .executeForList(ctx);

// 读取结果
String assetNo = platform.dynamicFields().getString("customer_asset_no");
```

### 4. 注册到 UserContext

在 runtime 初始化时将 facade 注册为 capability：

```java
// 在 DefaultUserContext 或启动配置中
userContext.registerCapability(DynamicFieldsFacade.class,
    new DefaultDynamicFieldsFacade(provider));

// 业务代码即可使用
ctx.dynamicFields()
    .owner("Platform", platformId)
    .string("customer_asset_no")
    .set("A-10086");
```

## 类型体系

### 核心类一览

| 类 | 职责 |
|---|---|
| `DF` | 静态入口：`DF.fields()` 构建 `DynamicFieldSelection` |
| `DynamicFieldDef` | 字段定义（code、类型、权限、状态等） |
| `DynamicFieldValue` | 单个字段值（不可变，类型化工厂方法） |
| `DynamicFieldValues` | 值集合 wrapper（区分未加载 vs null） |
| `DynamicFieldSelection` | 声明要 select 的动态字段 |
| `DynamicFieldScope` | 作用域（scopeType + scopeId） |
| `DynamicFieldRef` | 字段引用（scope + ownerType + code） |
| `DynamicOwnerRef` | 实体引用（ownerType + ownerId） |
| `DynamicSetCommand` | 写入命令 |
| `DynamicValueRef` | 值引用（用于删除） |

### 接口

| 接口 | 职责 |
|---|---|
| `DynamicFieldsProvider` | 底层存储 SPI（load/save/delete） |
| `DynamicFieldsFacade` | 业务层 facade（校验 + 类型检查 + 权限） |
| `DynamicFieldContext` | 轻量上下文（scope、user、intent） |
| `DynamicFieldCapabilities` | 声明 provider 支持的能力 |

### 枚举

| 枚举 | 值 |
|---|---|
| `DynamicDataType` | `STRING`, `NUMBER`, `BOOL`, `DATE_TIME`, `ENUM` |
| `DynamicLogicalType` | `PLAIN_TEXT`, `EMAIL`, `PHONE`, `URL`, `CURRENCY`, `PERCENTAGE`, `TAG`, `COLOR`, `RICH_TEXT` |
| `DynamicFieldStatus` | `DRAFT`, `ACTIVE`, `DISABLED`, `DEPRECATED`, `DELETED` |

### 错误码

| 错误码 | 含义 |
|---|---|
| `DYNAMIC_FIELD_NOT_FOUND` | 字段定义不存在 |
| `DYNAMIC_FIELD_TYPE_MISMATCH` | 读写类型与定义不匹配 |
| `DYNAMIC_FIELD_NOT_SELECTED` | 读取未 select 的字段 |
| `DYNAMIC_FIELD_NOT_VISIBLE` | 字段不可见 |
| `DYNAMIC_FIELD_NOT_EDITABLE` | 字段不可编辑 |
| `DYNAMIC_FIELD_OWNER_TYPE_MISMATCH` | 实体类型不匹配 |
| `DYNAMIC_FIELD_SCOPE_NOT_CONFIGURED` | 作用域未配置 |
| `DYNAMIC_FIELD_PROVIDER_UNSUPPORTED` | Provider 不支持该操作 |
| `DYNAMIC_FIELD_INTENT_REQUIRED` | 严格模式下缺少 purpose/comment |

## JSON 序列化约定

动态字段在 JSON 中使用 `#` 前缀，与标准字段和现有动态属性（`_` 前缀）隔离：

```json
{
  "id": 1001,
  "name": "Acme Platform",
  "#customer_asset_no": "A-10086",
  "#enabled_for_custom_flow": true,
  "#priority_score": 80
}
```

命名空间约定：

| 前缀 | 语义 | 来源 |
|---|---|---|
| 无前缀 | 标准领域字段 | 生成代码 |
| `_xxx` | SQL 临时动态列 / 聚合 | `simpleDynamicProperties` |
| `#xxx` | Dynamic Fields 值 | `DynamicFieldsProvider` |
| `__xxx` | 系统保留元数据 | 框架内部（如 `__fieldMeta`） |

- `#field: null` 表示字段已加载但值为空
- 字段不存在表示未 select 或不可见
- 入站 JSON 的 `#` 前缀字段只是候选载荷，必须通过显式 write flow 才能持久化

## 作用域（Scope）

字段定义绑定到作用域，同一个 ownerType + code 在不同作用域下可以有不同的定义：

```
GLOBAL          → 全局默认
APPLICATION     → 应用级
ORGANIZATION    → 组织级
TENANT          → 租户级
PROJECT         → 项目级
USER_GROUP      → 用户组级
USER            → 用户级
DEPLOYMENT      → 部署环境级
```

多租户只是其中一种常见 scope，不是 Dynamic Fields 的定义前提。

## 架构：如何扩展 Provider

### 当前内存实现

`InMemoryDynamicFieldsProvider` 是纯内存实现，初始化时会打印警告：

```
╔══════════════════════════════════════════════════════════════╗
║  InMemoryDynamicFieldsProvider initialized.                 ║
║  This is an IN-MEMORY implementation for DEMO purposes only.║
║  All dynamic field data will be LOST on JVM shutdown.        ║
║  Replace with a persistent provider for production use.      ║
╚══════════════════════════════════════════════════════════════╝
```

它的 `capabilities()` 返回 `sourceOfTruth=false`，表明不能作为生产数据源。

### 扩展方式

实现 `DynamicFieldsProvider` 接口，用 `DefaultDynamicFieldsFacade` 包装即可：

```
teaql-dynamic-fields-api          ← API + 内存实现（已完成）
teaql-dynamic-fields-jdbc         ← JDBC 持久化实现（已完成）
teaql-dynamic-fields-redis        ← Redis 实现（按需）
teaql-dynamic-fields-elasticsearch← ES 实现（按需）
```

#### JDBC 持久化（已完成）

`teaql-dynamic-fields-jdbc` 模块复用现有 DataSource，自动建表，跨方言兼容：

```java
// 复用同一个 DataSource
JdbcDynamicFieldsProvider provider = new JdbcDynamicFieldsProvider(existingDataSource);
provider.ensureSchema();  // 自动建表

// 注册字段定义（ID 由 runtime 的 InternalIdGenerationService 分配）
DynamicFieldDef def = new DynamicFieldDef();
def.setScope(DynamicFieldScope.global());
def.setOwnerType("Platform");
def.setCode("customer_asset_no");
def.setDataType(DynamicDataType.STRING);
def.setStatus(DynamicFieldStatus.ACTIVE);
provider.registerFieldDef(ctx, def);
```

详见 [teaql-dynamic-fields-jdbc/README.md](../teaql-dynamic-fields-jdbc/README.md)。

#### 注册到 UserContext

```java
// 开发/测试环境 —— 内存实现（数据不持久化）
DynamicFieldsProvider provider = new InMemoryDynamicFieldsProvider();

// 生产环境 —— JDBC 实现（复用现有数据库）
DynamicFieldsProvider provider = new JdbcDynamicFieldsProvider(dataSource);
provider.ensureSchema();

// 两种环境使用同一个 facade
DynamicFieldsFacade facade = new DefaultDynamicFieldsFacade(provider, scope);
userContext.registerCapability(DynamicFieldsFacade.class, facade);
```

#### 自定义 Facade

如果需要更复杂的校验逻辑（如基于角色的权限、数据脱敏、多级 scope 解析），可以扩展或替换 `DefaultDynamicFieldsFacade`：

```java
public class EnterpriseFieldsFacade extends DefaultDynamicFieldsFacade {

    @Override
    public DynamicFieldsFacade withContext(Object userContext) {
        // 从 UserContext 中提取 tenant、role、数据域等信息
        // 解析多级 scope 优先级
        // 注入权限和脱敏策略
    }
}
```

### 关键设计约束

| 约束 | 原因 |
|---|---|
| API 模块零依赖 | 任何环境（Android、GraalVM、测试）都能使用 |
| `DefaultDynamicFieldsFacade` 与 provider 无关 | 校验逻辑只写一次，所有 provider 复用 |
| `DynamicFieldValues` 区分 null 和未加载 | 避免歧义，前端可以正确渲染 |
| `#` 前缀是保留命名空间 | 标准字段、SQL 动态列、系统元数据各有命名空间 |
| provider `capabilities()` 声明能力 | runtime 可以根据能力选择执行策略 |

## 分期路线

| 阶段 | 范围 | 状态 |
|---|---|---|
| **Phase 1: Select + Read/Write** | API 层、内存实现、core 桥接、`selectDynamicFieldsWith`、`DynamicFieldValues` | ✅ 已完成 |
| **Phase 2: SQL Filter/Order** | 动态字段参与主查询 WHERE/ORDER BY、类型表 join、索引策略 | 计划中 |
| **Phase 3: Search/Facet/Promotion** | 搜索引擎集成、facet、export/import、字段提升为标准字段 | 计划中 |

## JPMS

```java
module io.teaql.data.dynamic {
    requires java.logging;
    exports io.teaql.data.dynamic;
}
```

`teaql-core` 通过 `requires transitive io.teaql.data.dynamic` 传递导出，所有下游模块自动可见。
