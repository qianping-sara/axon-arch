# Tool 设计指南

## 概述

本文档说明在 ATAM Copilot 项目中，何时应该使用 `@Tool` 注解，何时应该使用硬编码调用。

## 两种工具调用方式

### 1. 硬编码调用（Direct Invocation）

**适用场景**：
- ✅ 必需的前置步骤
- ✅ 确定的调用顺序
- ✅ 技术性操作（不需要业务决策）
- ✅ 每次都必须执行的操作

**实现方式**：
```java
@Component
public class MyAgent {
    private final MyTool myTool;  // 构造函数注入
    
    public MyAgent(MyTool myTool) {
        this.myTool = myTool;
    }
    
    public String process() {
        // 硬编码调用
        myTool.doSomething();
    }
}
```

**示例**：
- `GeminiFileUploadTool` - 文件上传是必需的前置步骤
- 数据验证工具 - 每次都必须验证
- 配置加载工具 - 启动时必须加载

---

### 2. @Tool 注解（Function Calling）

**适用场景**：
- ✅ 可选的外部能力
- ✅ 由 LLM 决定何时调用
- ✅ 业务决策性操作
- ✅ 外部数据查询

**实现方式**：
```java
@Component
public class MyTool {
    
    @Tool(description = "Clear description of what this tool does and when to use it")
    public String doSomething(
        @ToolParam(description = "Parameter description") String param
    ) {
        // 工具实现
        return result;
    }
}

// Agent 中使用
@Component
public class MyAgent {
    
    public String process(String userInput) {
        return ChatClient.create(chatModel)
            .prompt(userInput)
            .tools(new MyTool())  // LLM 决定是否调用
            .call()
            .content();
    }
}
```

**示例**：
- 持久化工具 - LLM 决定何时保存
- 知识库查询工具 - LLM 决定是否需要查询
- 外部 API 调用 - LLM 决定是否需要调用

---

## 决策树

```
是否每次都必须调用？
├─ 是 → 硬编码调用
│   └─ 示例：文件上传、数据验证
│
└─ 否 → 是否需要 LLM 决策何时调用？
    ├─ 是 → 使用 @Tool 注解
    │   └─ 示例：持久化、知识库查询
    │
    └─ 否 → 硬编码调用
        └─ 示例：配置加载、日志记录
```

---

## 当前项目中的工具分类

### 硬编码调用的工具

| 工具 | 原因 | 位置 |
|------|------|------|
| `GeminiFileUploadTool` | 必需的前置步骤 | `com.atam.tools.document` |

### 未来可能使用 @Tool 的工具

| 工具 | 原因 | 优先级 |
|------|------|--------|
| `PersistenceTool` | LLM 决定何时保存 | P1 |
| `ArchitectureKnowledgeTool` | LLM 决定是否查询 | P2 |
| `SchemaValidatorTool` | LLM 决定是否验证 | P2 |

---

## 最佳实践

### ✅ 好的 @Tool 描述

```java
@Tool(description = "Persist ATAM business driver artifact to database. " +
                    "Use this tool when the user confirms the extracted business drivers. " +
                    "The artifact must be in JSON format matching the BusinessDriver schema. " +
                    "Returns the unique artifact ID for future reference.")
String persistBusinessDriver(
    @ToolParam(description = "Business driver artifact in JSON format, " +
                             "must include: id, description, targetValue, priority")
    String artifact
)
```

### ❌ 不好的 @Tool 描述

```java
@Tool(description = "Save data")  // 太简单，LLM 无法理解何时使用
void save(String data)
```

---

## 参考资料

- [Spring AI Tool Calling 文档](../Design_doc/Spring_AI_Framework_Learning_Report.md#二tool-calling-function-calling-详解)
- [Phase 1 设计文档](../Design_doc/Phase1_Project_Structure_Design.md)

