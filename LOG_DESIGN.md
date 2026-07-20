# TeaQL 日志与审计系统设计文档 (Logging & Audit System Design)

## 1. 核心设计目标 (Goals)
为了满足企业级系统的可观测性和合规性审计需求，并保持 TeaQL 极致的轻量化，本系统设计遵循以下原则：
*   **绝对零依赖 (Zero-Dependency)**：不引入 `slf4j`、`logback` 或 `log4j2` 等外部日志库，从根本上杜绝潜在的依赖冲突（Jar Hell）。
*   **可插拔后端 (Pluggable Backend)**：`teaql-runtime` 只暴露 `RuntimeLogSink` 扩展点；文件/stdout 日志实现位于可选 `teaql-runtime-log` 模块。
*   **白名单配置 (Whitelist Config)**：严格通过带有 `TEAQL_` 前缀的环境变量控制系统行为，保障运行时的安全性与隔离性。
*   **极致性能 (High Performance)**：采用有界阻塞队列（Bounded Blocking Queue）加独立后台写入线程（LogWriter Thread）实现异步落盘，绝不让磁盘 I/O 拖累核心 SQL 的执行效率。

---

## 2. 核心架构与组件划分

### 2.1 环境变量配置中心 (`TeaQLEnv`)
在 `teaql-runtime-log` 启动时，静态解析并缓存系统的环境变量与属性，过滤出以 `TEAQL_` 开头的配置项，形成只读白名单。
**支持的核心变量**：
*   `TEAQL_LOG_ENDPOINT`: 日志文件输出的绝对或相对路径（如不配置，自动降级为标准输出 `System.out`）。
*   `TEAQL_LOG_FORMAT`: 日志格式，支持 `human`（对齐美化）或 `json`（方便 ELK 采集）。
*   `TEAQL_LOG_MAX_SIZE`: 单个日志文件最大大小，如 `50M`，`1G`。
*   `TEAQL_LOG_MAX_FILES`: 触发滚动后，最多保留的历史日志文件数量。

### 2.2 实体类抽象 (Data Models)
*   **`TraceNode`**: 链路追踪节点。显式传递在 `UserContext` 内部，记录诸如“创建订单 -> 扣减库存”的业务步骤，附加在每一条 SQL 和 Audit 日志中。
*   **`SqlLogEntry`**: 记录单次数据库操作的详细信息。包含 `prettySql`（格式化后去除多余换行的 SQL）、`elapsedUs`（纳秒级精度换算的微秒耗时）、`resultSummary`（如“受影响的行数：1”）。
*   **`AuditEvent`**: 审计事件。记录数据变更的核心结构，包含 `entityType`（实体名）、`entityId`（主键）、`mutationKind`（操作类型 CREATE/UPDATE/DELETE）、以及由多个 `FieldChange`（字段的新旧值）组成的变更列表。

### 2.3 格式化工厂 (`LogFormatterFactory`)
通过工厂模式动态装配日志格式：
*   **`HumanReaderFormatter`**: 为本地调试和人工排查问题设计的优美格式：
    ```text
    [2026-06-13 17:50:21.123]-[ 1024µs]-[DEBUG]-SqlLogEntry - [查询用户详情 -> 校验权限]
          SELECT * FROM user WHERE id = 1
    ```
*   **`JsonReaderFormatter`**: 将完整的链路和审计结构打包为标准 JSON，供机器消费。

---

## 3. 执行机制 (Execution Mechanisms)

### 3.1 异步落盘模型 (Asynchronous Writing Model)
日志系统在 `LogManager` 中维护一个核心队列 `ArrayBlockingQueue<Runnable>` 和一个单例的守护线程。
无论前端产生多少并发请求，主线程只会将打包好的 `LogTask` 塞入队列。由于队列和对象的创建都在内存极速完成，主线程会立即返回，实现极致的吞吐量。
守护线程循环拉取任务并统一执行 `FileChannel` 的 `write`，将零散的写操作集中为顺序写，最大化利用 OS 的 PageCache。

### 3.2 日志滚动与保留 (Rotation & Retention)
如果在 `TeaQLEnv` 中配置了文件滚动（Size-based rotation）：
1.  **探测阶段**：写入线程内部累加每次写入的 `byte[]` 长度。当累加值超过 `TEAQL_LOG_MAX_SIZE` 阈值时触发滚动。
2.  **切割阶段**：立即关闭当前 `FileChannel`，对文件执行重命名操作，如 `teaql.log` 变更为 `teaql.log.20260613.1`，并重新打开新的空 `teaql.log`。
3.  **清理阶段**：异步扫描当前目录下的备份文件，如果超过 `TEAQL_LOG_MAX_FILES`，按照文件的最后修改时间（LastModifiedTime）删除最旧的备份。

### 3.3 运行时拦截 (Runtime Interception)
*   **SQL 执行拦截**：SQL provider 在执行前后记录耗时并调用 `UserContext.recordExecutionMetadata(...)`。默认 runtime 会将 metadata 交给注入的 `RuntimeLogSink`；`teaql-runtime-log` 的 `LogManager` 是当前文件/stdout 后端实现。
*   **Audit 生成拦截**：审计事件由运行时或 provider 在确定状态点生成，并交给注入的 `RuntimeLogSink` 后端。未引入或未注入 `teaql-runtime-log` 时，`teaql-runtime` 保持最小运行闭环，不启动日志后台线程。

---
*设计定稿时间: 2026年06月13日*
*该文档作为 TeaQL 项目核心架构约定，后续代码实现必须严格按照此蓝图推进。*
