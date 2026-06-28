# TeaQL Dynamic Fields 精化设计

## 1. 定位

Dynamic Fields 是 TeaQL 的运行期领域扩展模型。

它不只服务 SaaS 多租户。普通业务系统、插件化系统、低代码配置、客户现场部署、项目级配置、部门级配置，都可能需要在不修改标准领域模型的情况下增加少量受控字段。

它不是裸 key-value、不是无约束 EAV，也不是替代标准领域模型的机制。它服务的是：

- 某个业务作用域对标准业务对象增加少量自定义字段；
- 字段有定义、类型、权限、隐私、校验、审计和生命周期；
- 字段还没有成熟到应该进入全局领域模型；
- 后续可以根据使用情况提升为标准字段、投影字段、物化字段或搜索索引字段。

一句话边界：

> Dynamic Fields 允许业务变化先在受控作用域内发生，但仍然必须经过 TeaQL 的语义、安全、审计和 provider 边界。

这里的作用域可以是：

```text
GLOBAL
APPLICATION
ORGANIZATION
TENANT
PROJECT
USER_GROUP
USER
DEPLOYMENT
```

多租户只是其中一种常见 scope，不是 Dynamic Fields 的定义前提。

## 2. 与现有 TeaQL dynamic property 的关系

当前 TeaQL 已经存在几类名为 dynamic 的能力：

- `BaseRequest.simpleDynamicProperties`：SQL select 产生的临时动态列；
- `BaseRequest.dynamicAggregateAttributes`：聚合结果挂到 entity 上；
- `Entity.addDynamicProperty()` / `BaseEntity.additionalInfo`：运行期附加返回值容器；
- `WebAction` / `WebStyle` 等 UI 辅助信息也会进入 dynamic property 容器。

这些能力可以作为返回载体或内部实现工具，但不能直接等同于本设计的 Dynamic Fields。

建议约定：

| 能力 | 当前语义 | Dynamic Fields 中的用法 |
| --- | --- | --- |
| `simpleDynamicProperties` | 查询临时表达式列 | 不作为字段定义来源，不承载权限/类型/审计 |
| `dynamicAggregateAttributes` | 聚合派生值 | 不作为动态字段存储 |
| `BaseEntity.additionalInfo` | 返回附加信息容器 | MVP 可用于装载已 select 的动态字段值 |
| 新增 `DynamicFieldValues` | 动态字段值视图 | 对外读取入口，避免裸字符串散落 |

因此第一版可以把动态字段结果放入 `additionalInfo`，但对业务代码应暴露专门 API：

```java
platform.dynamicFields().getString("customer_asset_no");
platform.dynamicFields().getBool("enabled_for_custom_flow");
platform.dynamicFields().getNumber("priority_score");
```

内部可以继续用 `BaseEntity.addDynamicProperty()` 装载结果，但建议装载为 `DynamicFieldValues` wrapper，而不是把每个字段直接作为普通 dynamic property：

```text
.dynamicFieldValues
```

序列化层再把 wrapper 展开成 `#<field_code>`。不要把每个动态字段直接打平成 `_customer_asset_no`，以免和现有临时列、聚合动态属性、UI 附加信息冲突。

## 3. 序列化与反序列化

当前 `BaseEntity.additionalInfo` 的 JSON 行为由可选 `teaql-jackson` 模块提供，需要特别处理：

- `BaseEntity` 本身不依赖 Jackson；
- 注册 `io.teaql.jackson.TeaQLModule` 后，`BaseEntityJsonSerializer` 只序列化 `id`、`version` 和 `additionalInfo` 中的所有条目（打平到顶层 JSON），忽略 `$status`、`comment`、`traceChain` 等内部字段；
- 注册 `TeaQLModule` 后，`BaseEntityJsonDeserializer` 把除 `id`/`version` 外的所有 JSON 字段通过 `putAdditional()` 放入 `additionalInfo`（注意：反序列化始终创建 `BaseEntity` 实例，而非具体子类）；
- `addDynamicProperty("abc", value)` 会序列化成顶层 `_abc`；
- `addDynamicProperty(".abc", value)` 会序列化成顶层 `abc`；
- **注意：`addDynamicProperty()` 在 value 为 null 时静默丢弃**，不会存入 `additionalInfo`。因此不能用 `addDynamicProperty()` 表达"字段已加载但值为空"，`DynamicFieldValues` wrapper 必须自行处理 null 语义（参见 §3.2）。

因此 Dynamic Fields 不能把每个动态字段直接用原始 code 放进 `additionalInfo` 顶层。否则会出现三个问题：

- 和普通临时动态属性冲突，例如 `_xxx` SQL 派生列；
- 和 UI/响应附加属性冲突，例如 action/style 类信息；
- 反序列化时无法区分“未知 JSON 字段”和“允许写入的 dynamic field”。

### 3.1 出站 JSON 形状

默认出站 JSON 推荐使用 `#<field_code>` 扁平形状：

```json
{
  "id": 1001,
  "name": "Acme Platform",
  "#customer_asset_no": "A-10086",
  "#enabled_for_custom_flow": true,
  "#priority_score": 80
}
```

内部可以通过：

```java
entity.addDynamicProperty(".#customer_asset_no", "A-10086");
```

利用 `addDynamicProperty()` 的 `.` 前缀规则把 key 存为 `#customer_asset_no`，`BaseEntityJsonSerializer` 遍历 `additionalInfo` 时会输出顶层 `#customer_asset_no`。但不能输出：

```json
{
  "_customer_asset_no": "A-10086"
}
```

也不能输出无前缀字段：

```json
{
  "customer_asset_no": "A-10086"
}
```

原因是后两种形状无法稳定区分标准字段、临时动态属性、聚合动态属性和 dynamic field。

`#` 前缀的含义应固定为：

```text
#<field_code> = Dynamic Field value
```

它的好处是动态字段值仍然在业务对象第一层，前端表格、导入导出和调试时更直观；同时又不会和标准字段名冲突。

如果响应需要同时返回字段元数据，可以保留一个系统元信息 key。`__fieldMeta` 描述的是 response field metadata，不只是 dynamic field metadata；它的 key 必须和本次响应 JSON 顶层字段名一致：

```json
{
  "id": 1001,
  "name": "Acme Platform",
  "#customer_asset_no": "A-10086",
  "#enabled_for_custom_flow": true,
  "#priority_score": 80,
  "__fieldMeta": {
    "id": {
      "type": "ID",
      "label": "ID",
      "masked": false,
      "selected": true,
      "dynamic": false
    },
    "name": {
      "type": "STRING",
      "label": "Name",
      "masked": false,
      "selected": true,
      "dynamic": false
    },
    "#customer_asset_no": {
      "type": "STRING",
      "label": "Customer Asset No",
      "masked": false,
      "selected": true,
      "dynamic": true
    },
    "#priority_score": {
      "type": "NUMBER",
      "label": "Priority Score",
      "masked": false,
      "selected": true,
      "dynamic": true
    }
  }
}
```

`__fieldMeta` 是保留 key，不是动态字段值。字段编码不能以 `#` 或 `__` 开头。`__fieldMeta` 只描述本次响应中出现或被明确 select 的字段，不是完整字段定义表。

### 3.2 Null、未加载和脱敏

`#<field_code>` 存在且值为 `null`，表示字段已加载但值为空：

```json
{
  "#customer_asset_no": null
}
```

**实现注意：** `BaseEntity.addDynamicProperty()` 在 value 为 null 时静默返回，不会存入 `additionalInfo`。因此 `DynamicFieldValues` wrapper 不能依赖 `addDynamicProperty()` 来表达 null 值。推荐 `DynamicFieldValues` 使用 `BaseEntity.putAdditional()` 直接存入 `#<field_code>: null`，或者 wrapper 自身维护已加载字段集合，序列化时由 wrapper 负责输出 null。

字段不存在，表示未 select 或当前用户不可见。业务代码读取未加载字段时应抛出：

```text
DYNAMIC_FIELD_NOT_SELECTED
```

读取不可见字段时应抛出或按策略返回 masked 值：

```text
DYNAMIC_FIELD_NOT_VISIBLE
```

如果需要携带更多元数据，可以使用 `__fieldMeta`，而不是把值改成对象：

```json
{
  "#customer_asset_no": "A-10086",
  "__fieldMeta": {
    "#customer_asset_no": {
      "value": "A-10086",
      "type": "STRING",
      "masked": false,
      "selected": true,
      "dynamic": true
    },
    "name": {
      "type": "STRING",
      "masked": false,
      "selected": true,
      "dynamic": false
    }
  }
}
```

默认值仍保持在 `#<field_code>` 上；`__fieldMeta` 面向管理后台、调试、导入导出和审计。

### 3.3 入站 JSON 形状

反序列化必须区分普通 unknown additional property 和动态字段写入。

允许的动态字段入站形状只应该是显式 `#` 前缀：

```json
{
  "id": 1001,
  "#customer_asset_no": "A-10086"
}
```

不允许把未知顶层字段自动解释为动态字段：

```json
{
  "customer_asset_no": "A-10086"
}
```

原因：

- 注册 `TeaQLModule` 后，`BaseEntityJsonDeserializer` 会把所有未知字段通过 `putAdditional()` 放入 `additionalInfo`；
- 如果 unknown field 自动写入 dynamic fields，会绕过字段定义、权限、类型、审计和 intent；
- 拼写错误的标准字段也可能被误写成动态字段。

入站 `#<field_code>` 只能被解析为候选载荷，真正写入必须经过：

```java
ctx.dynamicFields()
```

或应用服务显式调用的 dynamic field command handler。

普通 entity save 不应因为 JSON 中带了 `#<field_code>` 就自动持久化动态字段，除非调用方进入明确的 dynamic-field write flow。

这个形状的主要影响：

- `#` 必须成为 TeaQL JSON 的保留前缀，标准字段、普通 additional property、临时动态列都不能使用这个前缀；
- 注册 `TeaQLModule` 后，`BaseEntityJsonDeserializer` 会把 `#customer_asset_no` 通过 `putAdditional()` 收进 `additionalInfo`，但 repository save 不能自动写库；
- JSONPath、前端表单和导入导出工具访问字段时通常要使用 bracket 形式，例如 `$['#customer_asset_no']`；
- OpenAPI/Schema 不能只靠普通 Java Bean 属性表达，需要补充 dynamic-field 扩展 schema；
- `__fieldMeta` 必须作为保留元数据 key 特判，不参与动态字段值写入；
- 入站 JSON 中的 `__fieldMeta` 只能忽略或作为客户端提示，不能作为字段定义、权限、类型、加密或脱敏依据；
- 服务端最终仍以 `EntityDescriptor` 和 `DynamicFieldDef` 为准；
- `__` 前缀保留给 TeaQL 系统元信息，不能作为标准字段名、动态字段 code 或普通 additional property 前缀。

### 3.4 命名空间保留

建议保留以下顶层 JSON 名称，生成器不应生成同名标准字段：

```text
#<field_code>
__fieldMeta
```

建议保留以下 `additionalInfo` 内部命名空间：

```text
#<field_code>
__fieldMeta
```

已有的临时动态属性仍可继续使用当前规则：

```text
_xxx
```

但 dynamic fields 不使用 `_xxx` 扁平命名，而使用 `#xxx` 扁平命名。

### 3.5 与其他动态属性的区分

建议把运行期附加信息分成三类语义：

| 类别 | JSON 形状 | 来源 | 是否可反序列化写库 |
| --- | --- | --- | --- |
| SQL 临时动态列 | `_xxx` | `simpleDynamicProperties` | 否 |
| 聚合/增强动态属性 | `_xxx` 或业务约定名 | `dynamicAggregateAttributes` / enhancer | 否 |
| Dynamic Fields | `#<field_code>` | `DynamicFieldsProvider` | 只能通过显式 dynamic field write flow |
| Response Field Meta | `__fieldMeta` | `EntityDescriptor` + `DynamicFieldsProvider` / facade | 否 |

这个区分必须写入 AI Agent 和代码生成器指导：看到未知 JSON 字段时，不要自动把它当作 dynamic field；只有 `#` 前缀字段才进入 dynamic field 解析，且 `__fieldMeta` 是响应字段元数据保留字段。

## 4. 分层设计

### 4.1 `teaql-dynamic-fields-api`

抽象契约层。只依赖 TeaQL core，不依赖 runtime、SQL、Spring 或具体 provider。

建议包含：

```text
DF
DynamicFieldSelection
DynamicFieldFilter
DynamicFieldOrder
DynamicFieldQuery
DynamicFieldDef
DynamicFieldRef
DynamicOwnerRef
DynamicFieldScope
DynamicFieldValue
DynamicFieldValues
DynamicDataType
DynamicLogicalType
DynamicFieldStatus
DynamicFieldCapabilities
DynamicFieldsProvider
DynamicFieldContext
DynamicFieldException
```

关键原则：

- API 层定义语义对象，不碰 `UserContext`；
- `DynamicFieldContext` 是轻量接口，由 runtime 适配；
- API 层不暴露具体动态字段表；
- filter/order 的对象模型可以先定义，但 MVP 不一定执行。

`DynamicFieldContext` 建议最小化：

```java
public interface DynamicFieldContext {
    String scopeType();
    String scopeId();
    String userId();
    String purpose();
    String comment();
    boolean strictIntent();
}
```

`scopeType()` / `scopeId()` 用来表达字段定义和值归属的业务作用域。普通单体系统可以使用 `GLOBAL/default` 或 `APPLICATION/<appCode>`；SaaS 系统可以使用 `TENANT/<tenantId>`；项目型系统可以使用 `PROJECT/<projectId>`。

如果后续需要角色、部门、区域、数据域等信息，通过 capability 或扩展接口添加，不要一开始把 `UserContext` 整体泄漏到 api。

### 4.2 `teaql`

当前仓库中 core/runtime 边界主要集中在 `teaql` 模块，`UserContext`、`Entity`、`BaseRequest`、`SearchRequest` 都在这里。

第一版建议只在 `teaql` 中增加最小桥接点：

```text
BaseEntity.dynamicFields()
SearchRequest.getDynamicFieldSelection()
BaseRequest.selectDynamicFieldsWith(...)
```

`UserContext.dynamicFields()` 通过现有的 `capability()` 机制桥接，不需要在 `UserContext` 接口上新增抽象方法：

```java
// UserContext 上添加 default 方法，委托给 capability
default DynamicFieldsFacade dynamicFields() {
    DynamicFieldsFacade facade = capability(DynamicFieldsFacade.class);
    if (facade == null) {
        throw new TeaQLRuntimeException("DynamicFieldsFacade not registered");
    }
    return facade.withContext(this);
}
```

这样 `teaql-core` 只需要知道 `DynamicFieldsFacade` 接口（定义在 `teaql-dynamic-fields-api` 中），不会引入 provider 或 runtime 依赖。

业务代码使用方式：

```java
ctx.dynamicFields()
    .purpose("Update dynamic field")
    .owner("Platform", platformId)
    .string("customer_asset_no")
    .set("A-10086");
```

facade 负责：

- 从 `UserContext` 提取 scope/user/comment/purpose；
- enforce Triple-Intent；
- 校验 owner type 和 owner id；
- 查找字段定义；
- 校验类型、状态、可见/可编辑/可查询；
- 执行权限、隐私、mask；
- 调用 provider；
- 统一包装错误。

`SearchRequest.getDynamicFieldSelection()` 应作为接口方法声明在 `SearchRequest` 上，`BaseRequest` 提供具体实现（使用专用字段存储，不使用 `extensions` map）：

```java
// SearchRequest 接口
DynamicFieldSelection getDynamicFieldSelection();

// BaseRequest 实现
private DynamicFieldSelection dynamicFieldSelection;

public SearchRequest<T> selectDynamicFieldsWith(DynamicFieldSelection selection) {
    this.dynamicFieldSelection = selection;
    return (SearchRequest<T>) this;
}

@Override
public DynamicFieldSelection getDynamicFieldSelection() {
    return dynamicFieldSelection;
}
```

### 4.3 Generated Query

Query DSL 里建议保留三个最终方法名：

```java
selectDynamicFieldsWith(...)
filterByDynamicFieldsWith(...)
orderByDynamicFieldsWith(...)
```

但分期上只先实现：

```java
selectDynamicFieldsWith(...)
```

原因：

- select 可以 post-load，复杂度低；
- filter/order 会改变主 SQL query planning；
- filter/order 必须处理 join/exists、类型表、scope、field code 到 field id 的解析、排序 null 语义和索引策略；
- 过早实现容易把动态字段 SQL 细节泄漏到 core DSL。

MVP 查询示例：

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
    .purpose("Load platform records and selected scope-defined dynamic fields for display")
    .executeForList(ctx);
```

### 4.4 `teaql-dynamic-fields-teaql`

默认实现模块，使用 TeaQL 自己的模型和 repository 存储动态字段定义和值。

建议包含：

```text
TeaqlDynamicFieldsProvider
DynamicFieldDefEntity
DynamicFieldPermissionEntity
DynamicFieldValidationEntity
DynamicFieldOptionEntity
DynamicStringValueEntity
DynamicNumberValueEntity
DynamicBoolValueEntity
DynamicDateTimeValueEntity
DynamicEnumValueEntity
```

业务代码、AI Agent 和普通应用服务不应该直接访问这些表对应的 query：

```java
Q.dynamicStringValues()
Q.dynamicNumberValues()
Q.dynamicFieldDefs()
```

它们只能通过：

```java
ctx.dynamicFields()
```

或：

```java
Q.xxx().selectDynamicFieldsWith(...)
```

访问动态字段能力。

### 4.5 `teaql-sql`

当前 SQL 查询装配在 `teaql-sql` 中，`SQLRepository` 已经会把 `simpleDynamicProperties` 拼进 select，并在 mapper 里塞进 `Entity.addDynamicProperty()`。

MVP 阶段不要求 `teaql-sql` 直接理解 dynamic field filter/order。

推荐执行策略：

1. 主 query 正常执行；
2. `UserContext.executeForList()` 或 repository enhance 阶段检测 request 的 `DynamicFieldSelection`；
3. 收集 owner ids；
4. 调用 `DynamicFieldsProvider.loadValues(ctx, ownerRefs, selection)`；
5. 把结果放入 entity 的 `.dynamicFieldValues` wrapper；
6. `entity.dynamicFields()` 提供类型化读取。

第二阶段再在 `teaql-sql` 加入 dynamic field filter/order planning。

## 5. Provider 契约

Provider 是底层实现接口，不做业务 facade。

第一版建议：

```java
public interface DynamicFieldsProvider {
    DynamicFieldDef loadFieldDef(DynamicFieldContext ctx, DynamicFieldRef ref);

    List<DynamicFieldDef> listFieldDefs(DynamicFieldContext ctx, String ownerType);

    DynamicFieldValues loadValues(
            DynamicFieldContext ctx,
            DynamicOwnerRef ownerRef,
            DynamicFieldSelection selection);

    Map<DynamicOwnerRef, DynamicFieldValues> loadValues(
            DynamicFieldContext ctx,
            List<DynamicOwnerRef> ownerRefs,
            DynamicFieldSelection selection);

    void saveValue(DynamicFieldContext ctx, DynamicSetCommand command);

    void deleteValue(DynamicFieldContext ctx, DynamicValueRef valueRef);

    DynamicFieldCapabilities capabilities();
}
```

暂缓放入第一版 provider 的方法：

```java
DynamicQueryResult query(DynamicFieldContext ctx, DynamicFieldQuery query);
```

原因是 query 会暗含 filter/order/search/facet 能力，容易把第二阶段 SQL planner 的问题提前拉进 MVP。

第一版 capabilities：

```text
sourceOfTruth
supportsTransaction
supportsBatchLoad
supportsTypedValue
supportsBasicPermission
supportsBasicAudit
```

第二阶段再加：

```text
supportsFilter
supportsSort
supportsFacet
supportsFullText
supportsMasking
supportsAdvancedPermission
```

## 6. 数据模型

字段定义表：

```text
dynamic_field_def
-----------------
id
scope_type
scope_id
owner_type
code
name
description
data_type
logical_type
required
visible
editable
filterable
sortable
searchable
exportable
importable
auditable
privacy_level
mask_rule
default_value
status
display_order
version
created_by
created_at
updated_by
updated_at
```

唯一约束：

```text
scope_type + scope_id + owner_type + code
```

值表第一版按类型拆分：

```text
dynamic_string_value
dynamic_number_value
dynamic_bool_value
dynamic_datetime_value
dynamic_enum_value
```

每张值表都应有：

```text
id
scope_type
scope_id
owner_type
owner_id
field_id
value_*
version
created_by
created_at
updated_by
updated_at
```

唯一约束：

```text
scope_type + scope_id + owner_type + owner_id + field_id
```

如果某个 provider 内部天然是多租户模型，可以把 `TENANT/<tenantId>` 映射为 `tenant_id` 或数据库分区字段；但 API 和设计层不应把 tenant 作为唯一作用域。

第一版明确不支持：

- multi-value；
- per-field 多 provider 写入；
- dynamic field join 到标准 relation；
- 跨 owner type 查询；
- filter/order/search/facet；
- 字段定义热变更后自动迁移历史值。

## 7. 生命周期和错误模型

字段状态建议：

```text
DRAFT
ACTIVE
DISABLED
DEPRECATED
DELETED
```

读取规则：

- `ACTIVE` 可读；
- `DISABLED` 默认不可写，可按配置可读；
- `DEPRECATED` 可读可写由兼容策略决定，但创建新字段时不应推荐；
- `DELETED` 不参与普通查询，只保留审计和历史处理。

错误应使用稳定 code，方便 AI Agent 和前端处理：

```text
DYNAMIC_FIELD_NOT_FOUND
DYNAMIC_FIELD_TYPE_MISMATCH
DYNAMIC_FIELD_NOT_SELECTED
DYNAMIC_FIELD_NOT_VISIBLE
DYNAMIC_FIELD_NOT_EDITABLE
DYNAMIC_FIELD_OWNER_TYPE_MISMATCH
DYNAMIC_FIELD_TENANT_MISMATCH
DYNAMIC_FIELD_PROVIDER_UNSUPPORTED
DYNAMIC_FIELD_INTENT_REQUIRED
```

如果字段未被 select 就读取：

```text
Dynamic field customer_asset_no was not selected.
```

这应是明确错误，不应静默返回 null。真正的 null 值和未加载必须区分。

## 8. 分期

### Phase 1: Select + Read/Write MVP

目标：能定义字段、写值、按 owner 批量读取、在主 query 后装载 selected dynamic fields。

范围：

- `teaql-dynamic-fields-api`；
- `UserContext.dynamicFields()` facade；
- provider registry；
- TeaQL DB source-of-truth provider；
- `selectDynamicFieldsWith(...)`；
- `DynamicFieldValues` 返回 wrapper；
- 基础类型校验；
- 基础权限/可见/可编辑校验；
- comment/purpose enforcement；
- 审计字段落库。

验收示例：

```java
ctx.dynamicFields()
    .owner("Platform", platformId)
    .string("customer_asset_no")
    .set("A-10086");

Platform platform = Q.platformsWithMinimalFields()
    .filterById(platformId)
    .selectName()
    .selectDynamicFieldsWith(DF.fields().selectString("customer_asset_no"))
    .comment("Load platform custom asset number")
    .purpose("Display scope-defined custom field")
    .executeForOne(ctx);

String assetNo = platform.dynamicFields().getString("customer_asset_no");
```

### Phase 2: SQL Filter/Order

目标：动态字段参与主查询条件和排序。

范围：

- request 中携带 `DynamicFieldFilters` / `DynamicFieldOrders`；
- `teaql-sql` planner 转换为 exists/join；
- field code 在执行前解析为 field id；
- 类型表选择；
- null 排序语义；
- filterable/sortable capability 检查；
- 索引建议和 explain 验证。

### Phase 3: Search/Facet/Promotion

目标：动态字段进入搜索、facet 和模型演化流程。

范围：

- search provider；
- facet；
- export/import；
- promoted field 分析；
- 物化字段或标准领域字段迁移工具。

## 9. 需要修改原设计的关键点

1. 不建议第一版把 `filterByDynamicFieldsWith(...)` 和 `orderByDynamicFieldsWith(...)` 作为同等 MVP。
   它们可以先定义设计和命名，但实现应放到第二阶段。

2. 不建议把每个动态字段直接映射为 `entity.getDynamicProperty("field_code")`。
   应提供 `entity.dynamicFields()` wrapper，并区分未加载和值为 null。

3. `DynamicFieldsProvider.query(...)` 不建议进入第一版 provider 契约。
   第一版 provider 只负责 field def、value load/save/delete。

4. `teaql-sql` 第一版不需要直接改主 SQL。
   select 动态字段通过 post-load 完成，更符合当前 repository/enhance 风格。

5. 设计应显式声明现有 `simpleDynamicProperties` / `dynamicAggregateAttributes` 与 Dynamic Fields 的差异。
   这能避免后续实现把临时计算列误当成动态字段 source-of-truth。

6. 设计必须明确序列化和反序列化边界。
   Dynamic Fields 出站使用 `#<field_code>` 扁平字段；入站未知顶层字段不能自动写入动态字段，只有显式 `#` 前缀字段才可进入 dynamic field 解析，而且仍需显式 dynamic field write flow 才能持久化。

7. 设计不应把 Dynamic Fields 定位为多租户专用。
   多租户是一个 scope 实现；普通系统同样需要 global/application/project/user 等作用域下的动态领域扩展。

## 10. 最终推荐结构

```text
teaql-dynamic-fields-api
  DF
  DynamicFieldSelection
  DynamicFieldDef / Ref / OwnerRef
  DynamicFieldScope
  DynamicFieldValue / DynamicFieldValues
  DynamicFieldsProvider
  DynamicFieldContext
  DynamicFieldCapabilities

teaql
  UserContext.dynamicFields()
  DynamicFieldsFacade
  DynamicFieldsProviderRegistry
  BaseRequest.selectDynamicFieldsWith(...)
  BaseEntity.dynamicFields()

teaql-dynamic-fields-teaql
  TeaqlDynamicFieldsProvider
  Dynamic field definition/value entities
  TeaQL DB source-of-truth implementation

teaql-sql
  Phase 1: no core SQL planner change required
  Phase 2: dynamic field filter/order SQL planning
```

最终原则：

> 第一版先把动态字段作为受控、类型化、可审计的作用域字段值装载能力做实；等 select/read/write 稳定后，再让 filter/order/search 进入 SQL planner 和搜索 provider。
