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

### 3.2 跨平台路由能力
通过这种 SPI 层层叠加组装（Layered Assembly）机制，框架获得了无限的环境适应力：
* **在 Android 环境下**：开发者只要引入了 `teaql-sqlite` 包，底层的 Assembler 会自动嗅探环境，将本地的 SQLite 执行引擎挂载到 `UserContext`。业务层代码可以直接无缝执行。
* **在 Spring 环境下**：引入了 `teaql-spring` 后，Assembler 会去抽取 Spring 的 `JdbcTemplate` 或数据源，接管执行权。
* **多数据源隔离**：开发者可以通过环境变量指定不同的装配策略，在同一次程序中并行跑出多个隔离的 `UserContext`，实现左手读本地 SQLite，右手写云端 MySQL 的降维打击。

## 4. 总结
TeaQL 的 SPI 运行时架构将系统彻底分为“无状态的绝对纯净核心”与“环境自适应的模块化组装插件”。它不仅杜绝了配置灾难和依赖地狱，更是凭借原生 Java 纯粹的面向对象和内存指针优势，将运行性能推向了裸机级别的物理极限。
