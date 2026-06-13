# TeaQL 运行时架构设计：基于 SPI 的元数据与上下文自举模型

## 1. 架构愿景
TeaQL 致力于提供一个“极度轻量、高度可移植、零反射开销”的数据中间件运行底座。为了实现一套代码既能在重型的 Spring Boot 云端服务器中运行，又能毫无负担地在 Android、IoT 设备以及 Serverless（云函数）等受限环境运行，我们摒弃了传统的“大量代码生成+反射织入”模式，确立了 **“元数据驱动 + SPI 模块自举上下文”** 的核心运行时架构。

## 2. 核心设计哲学

### 2.1 剥离“What”与“How”
* **生成代码只负责“What（元数据）”**：未来的代码生成器不再生成任何底层连接和具体的方言执行逻辑，只生成纯粹的领域元数据（如 `TableName`、`Columns` 描述）。这使得上层生成的产物体积无限趋近于零。
* **运行时引擎负责“How（如何执行）”**：所有的底层实现逻辑全权交由统一的 `TeaQLRuntime` 和 `UserContext` 决定。

### 2.2 铁打的组件引擎，流水的 `UserContext`
我们从传统的单例模式进化为了 **“享元（Flyweight） + 请求管线（Request Pipeline）”** 模式：
1. **全局冷启动（Cold Boot）**：系统启动时，框架通过 SPI 扫描，仅且只有一次地初始化好那些重量级组件（如 `LogManager` 守护线程、数据库连接池、复杂的方言 `Dialect` 实例），并将它们缓存。
2. **每次请求瞬间创建（Per-Request Context）**：当一次查询或一个 HTTP 请求发生时，框架 `new` 出一个全新的极轻量的 `UserContext`。随后，瞬间把冷启动备好的那些“重量级组件”的 **内存引用（References）** 挂载给这个新建的 Context。

**性能表现**：在老旧的 i7 移动处理器上，单线程创建一个配置齐备的 `UserContext` 耗时仅需 **500 纳秒（0.5 微秒）**，实现了单核每秒近 200 万次的惊人并发创建率，彻底超越 Spring Boot 的代理对象生成性能。

## 3. 模块自举与即插即用（Plug-and-Play）

框架核心不再依赖任何特定的数据库或运行环境，一切配置都由“引入的包”来自动决议。

### 3.1 核心 SPI：`ContextAssembler`
所有底层组件（如 `teaql-sqlite`、`teaql-postgres`、`teaql-spring`）在被用户引入时，通过实现 `ContextAssembler` 接口“自我唤醒”：
```java
public interface ContextAssembler extends Comparable<ContextAssembler> {
    // 1. 指定装配优先级（核心层优先，然后是方言层，最后是连接池等执行器层）
    int getOrder();

    // 2. 冷启动：初始化本模块全局资源（全局执行一次）
    default void initGlobalResources() {}

    // 3. 热挂载：把自己的引用瞬间赋给每次新创建的 UserContext
    void mountTo(UserContext ctx);
}
```

### 3.2 联邦数据路由（Federated Data Routing）机制
框架摒弃了全局强绑定单一数据源的模式，引入了基于实体元数据的**智能数据服务路由（Data Service Routing）**：

1. **实体自带路由标记**：每个 `EntityDescriptor` 都包含一个 `dataService` 属性（默认值为 `"sql"`）。这表示一个系统中，`UserEntity` 可以指向 `"postgres_db"`，而 `LogEntity` 可以指向 `"sqlite_local"`。
2. **注册表（Registry）汇聚执行器**：`TeaQLRuntime` 和 `UserContext` 内部不直接写死任何 `DataServiceExecutor`，而是持有一个 `DataServiceRegistry`（数据服务注册表）。
3. **SPI 模块只负责注册**：例如 `PostgresContextAssembler` 的职责不是霸占全局执行权，而是在冷启动时实例化自己的 `PostgresDataServiceExecutor`，并以特定名称（如 `"postgres_db"`）注册到 `DataServiceRegistry` 中。
4. **引擎层精准派发**：当执行 `ctx.saveGraph(userEntity)` 或在启动时调用 `ensureSchema` 时，运行时大脑（Runtime）会：
   - 提取对象的元数据：`dataService = userEntity.descriptor().getDataService()`
   - 根据标识去 `DataServiceRegistry` 提取对应的专属 `DataServiceExecutor`。
   - 将该对象的 SQL 生成与持久化任务，精准分发给具体的数据库方言执行。

这种设计使得同一套业务代码，甚至在同一次 HTTP 请求内，可以完美串接云端 PostgreSQL 写业务数据、边缘端 SQLite 存日志、缓存 Redis 做会话的跨存储联邦架构。

### 3.3 跨平台自适应路由能力
通过这种 SPI 层层叠加组装（Layered Assembly）机制，框架获得了无限的环境适应力：
* **在 Android 环境下**：开发者只要引入了 `teaql-sqlite` 包，底层的 Assembler 会自动嗅探环境，将本地的 SQLite 执行引擎挂载到 `UserContext`。业务层代码可以直接无缝执行。
* **在 Spring 环境下**：引入了 `teaql-spring` 后，Assembler 会去抽取 Spring 的 `JdbcTemplate` 或数据源，接管执行权。
* **多数据源隔离**：开发者可以通过环境变量指定不同的装配策略，在同一次程序中并行跑出多个隔离的 `UserContext`，实现左手读本地 SQLite，右手写云端 MySQL 的降维打击。

## 4. 性能基线测试（Performance Baseline）

为了证明该架构的极限性能，我们在极其苛刻的老旧移动端硬件上进行了压测，以下为基准测试数据：

### 4.1 测试环境
* **CPU**: Intel(R) Core(TM) i7-10750H @ 2.60GHz (6核 12线程, 2020年移动端芯片)
* **内存环境**: 单线程、单核执行
* **测试用例**: 连续生成 1,000,000（一百万）个完整的 `UserContext`

### 4.2 压测结果
* **引擎冷启动耗时 (Cold Boot)**: **约 52 毫秒**
  *(涵盖了从类路径下扫描所有 META-INF 的 SPI 实现，并初始化带有后台并发写入线程的工业级 LogManager 的全过程。)*
* **创建 100万 个上下文总耗时**: **541 毫秒**
* **单次 UserContext 极速装配耗时**: **541 纳秒（约 0.5 微秒）**
  *(该耗时几乎完全等同于 `new ConcurrentHashMap()` 与 `new ArrayList()` 的 Java 底层堆内存分配耗时，模块装配/引用的挂载耗时几乎为 0。)*

### 4.3 性能结论
在单核运行环境下，TeaQL Runtime 能够以 **每秒将近 200 万次** 的极速吞吐量源源不断地生成隔离的运行上下文。
与传统 Spring Boot 框架动辄数秒的冷启动时间和基于反射/代理织入的沉重请求作用域（Request Scope）相比，TeaQL 的运行时在冷启动速度上实现了 **几何级数的降维打击（约快 60 倍）**。这种“纳秒级”的上下文隔离能力，使得 TeaQL 成为 Android 移动端中间件、IoT 边缘计算以及 Serverless 极速冷启动的完美选择。

## 5. 总结
TeaQL 的 SPI 运行时架构将系统彻底分为“无状态的绝对纯净核心”与“环境自适应的模块化组装插件”。它不仅杜绝了配置灾难和依赖地狱，更是凭借原生 Java 纯粹的面向对象和内存指针优势，将运行性能推向了裸机级别的物理极限。
