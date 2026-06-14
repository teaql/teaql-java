# TeaQL Context-Scoped Custom Log Sink Design (RFC)

## 1. 背景与核心问题 (Background & Problem Statement)

TEAQL `teaql-runtime` 原有的日志架构（详见 `LOG_DESIGN.md`）基于系统级环境变量 (`TEAQL_LOG_ENDPOINT`) 实现了强大的、不可绕过的异步持久化日志系统。它将 SQL 执行与审计日志集中写入 `System.out` 或是指定的物理文件中。

然而，在多租户环境、并发 Web 请求或是某些特殊的客户端场景（如 Android App 内置演示程序）中，这种“全局单例”的日志拦截方式存在局限性：
1. **上下文混淆**：在后端服务并发处理多个不同用户的请求时，如果使用一个全局的 `LogManager` Sink，所有的日志会交织在一起，难以剥离出某个特定用户/请求的日志。
2. **应用层集成困难**：应用层想要实时将特定的审计日志展示给用户（如在 Android 界面上实时回显），直接读取全局的 `teaql.log` 文件成本高昂且不灵活，重定向 `System.out` 则属于“黑盒 Hack”操作。

## 2. 核心架构升级：UserContext Scoped Log Sink

为了在**不破坏框架纯洁性与绝对零依赖**的前提下，提供极致的应用层扩展性，我们引入 `CustomLogSink` 机制，并将其生命周期严格绑定至 `UserContext`。

### 2.1 引入 `CustomLogSink` 接口
在 `teaql-core` 核心抽象中定义极简的回调接口：
```java
package io.teaql.core.log;

/**
 * 允许在应用层捕获 TeaQL Runtime 生成的格式化日志。
 * 注意：回调将在异步工作线程中执行，请勿在其中阻塞或抛出未捕获异常。
 */
public interface CustomLogSink {
    void onLog(String formattedLogContent);
}
```

### 2.2 扩展 `UserContext`
在 `io.teaql.core.UserContext` 增加对应方法的契约：
```java
void registerCustomSink(io.teaql.core.log.CustomLogSink sink);
io.teaql.core.log.CustomLogSink getCustomSink();
```
这意味着 `UserContext` 不再仅仅是业务执行的令牌，它也是日志溯源的上下文边界。

## 3. Runtime 执行时序与异步安全保障

为了维持 TEAQL 日志系统的核心理念：**“不让外部操作拖累核心 SQL 效率”**，`CustomLogSink` 采用了异步出队回调的设计。

1. **业务层发起**：业务调用 `task.save(ctx)`。
2. **Runtime 提取上下文**：`DefaultUserContext` 在执行完毕后触发 `recordExecutionMetadata(this, metadata)`。注意，此处将 `this` 传递给底层 `LogManager`。
3. **LogManager 打包**：`LogManager` 使用对应的 `LogFormatter` 生成 `formattedLogContent` 字符串，并从传递来的 `UserContext` 中提取 `CustomLogSink`。
4. **入队阶段**：`LogManager` 将 `content` 和 `customSink` 一并包装为一个匿名 `Runnable` 放入高并发的无锁/有界阻塞队列 `ArrayBlockingQueue`。**此阶段耗时极低，主业务线程立即放行。**
5. **守护线程消费与回调**：`TeaQL-LogWriter-Thread` 后台线程从队列中拉取任务，执行 `syncWrite` 时，首先检查 `customSink`。如果存在，调用 `customSink.onLog(content)`，最后继续将内容刷入到全局的日志文件或标准输出中。

## 4. 应用层集成范例 (Android / Web Application)

这一设计使得应用层可以写出高度隔离、完美线程安全的定制化回显逻辑：

```java
// 针对当前会话/请求，创建独立的 Context
RobotTaskBoardServiceUserContext ctx = new RobotTaskBoardServiceUserContextImpl();

// 注入专属于当前 Context 的定制化 Sink
ctx.registerCustomSink(logContent -> {
    // 切换到主线程更新前端 UI，不会阻塞 TeaQL 异步后台线程
    runOnUiThread(() -> {
         DatabaseMock.getInstance().addLog(logContent);
    });
});

// 执行业务逻辑，所产生的审计信息将仅仅只回调给当前 Context 绑定的 Sink
task.updateStatusToDone().save(ctx);
```

## 5. 设计结论

该方案巧妙利用了现有的异步队列架构，完全零侵入地实现了应用级日志扩展。它既满足了应用层实时展示或抓取特定请求日志的高级需求，又严格守住了系统级日志强制落盘、不阻塞业务主线程的底线。

---
*撰写时间：2026年06月14日*
