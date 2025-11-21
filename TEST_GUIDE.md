# ATAM Copilot 测试指南

**版本**: 1.0  
**日期**: 2025-11-20

---

## 📋 前置条件

### 1. 环境变量配置

在运行测试之前，**必须**设置 Google API Key：

```bash
# 开发环境 (使用 Gemini Developer API)
export GOOGLE_API_KEY="your-google-api-key-here"
```

**⚠️ 重要提示**: 如果不设置此环境变量，测试将失败并显示错误：
```
Could not resolve placeholder 'GOOGLE_API_KEY' in value "${GOOGLE_API_KEY}"
```

**获取 API Key**:
1. 访问 [Google AI Studio](https://aistudio.google.com/app/apikey)
2. 创建新的 API Key
3. 复制 API Key 并设置到环境变量

**验证环境变量**:
```bash
# 检查环境变量是否已设置
echo $GOOGLE_API_KEY
```

---

## 🧪 运行测试

### 方法 1: 使用 Maven 运行所有测试

```bash
cd atam-copilot

# 设置环境变量
export GOOGLE_API_KEY="your-api-key"

# 运行所有测试
mvn test
```

---

### 方法 2: 运行特定测试类

#### 2.1 测试 GeminiChatService

```bash
mvn test -Dtest=GeminiChatServiceTest
```

**测试内容**:
- ✅ 服务初始化
- ✅ 获取模型信息
- ✅ 同步聊天
- ✅ 流式聊天
- ✅ Markdown 格式输出

---

#### 2.2 测试 BusinessDriverAgent

```bash
mvn test -Dtest=BusinessDriverAgentTest
```

**测试内容**:
- ✅ Agent 初始化
- ✅ Prompt 模板加载
- ✅ 使用真实 PDF 提取（同步）
- ✅ 使用真实 PDF 提取（流式）
- ✅ 多文件提取

**注意**: 此测试会使用 `/Users/qianping/Documents/Source/axon/axon-arch/Design_doc/Architecture Review_Revival_V3.3.pdf` 文件。

---

#### 2.3 测试完整集成流程

```bash
mvn test -Dtest=BusinessDriverExtractionIntegrationTest
```

**测试内容**:
- ✅ 同步提取端点
- ✅ 流式提取端点
- ✅ 使用真实 PDF 提取
- ✅ 多文件上传
- ✅ 错误处理（无文件、文件过多）

---

### 方法 3: 运行特定测试方法

```bash
# 只测试真实 PDF 提取
mvn test -Dtest=BusinessDriverAgentTest#testExtractBusinessDriversWithRealPdf

# 只测试流式提取
mvn test -Dtest=BusinessDriverAgentTest#testExtractBusinessDriversStreamWithRealPdf
```

---

## 🚀 手动测试 API

### 1. 启动应用

```bash
cd atam-copilot

# 设置环境变量
export GOOGLE_API_KEY="your-api-key"

# 启动应用
mvn spring-boot:run
```

应用将在 `http://localhost:8080` 启动。

---

### 2. 使用 Swagger UI 测试

访问: `http://localhost:8080/swagger-ui.html`

在 Swagger UI 中：
1. 找到 **Business Driver Extraction** 分组
2. 选择 `/api/v1/business-drivers/extract` 或 `/api/v1/business-drivers/extract/stream`
3. 点击 "Try it out"
4. 上传 PDF 文件
5. 点击 "Execute"

---

### 3. 使用 cURL 测试

#### 同步提取

```bash
curl -X POST http://localhost:8080/api/v1/business-drivers/extract \
  -F "files=@/Users/qianping/Documents/Source/axon/axon-arch/Design_doc/Architecture Review_Revival_V3.3.pdf" \
  -o result.md
```

#### 流式提取

```bash
curl -X POST http://localhost:8080/api/v1/business-drivers/extract/stream \
  -F "files=@/Users/qianping/Documents/Source/axon/axon-arch/Design_doc/Architecture Review_Revival_V3.3.pdf" \
  -H "Accept: text/event-stream"
```

---

## 📊 验证测试结果

### 1. 检查输出格式

提取结果应该是 Markdown 格式，包含以下部分：

```markdown
### 1. 核心愿景
[项目愿景描述]

---

### 2. 业务目标 (Business Objectives)
| ID | 目标类别 | 详细描述 | 目标值/测量 | 业务价值/影响 | 优先级 |
|:---|:---------|:---------|:------------|:--------------|:-------|
| BO-1 | ... | ... | ... | ... | High |

---

### 3. 项目背景 (Project Background)
...

### 4. 约束与依赖 (Constraints & Dependencies)
...

### 5. 关键非功能性需求 (NFRs)
...
```

---

### 2. 检查内容丰富性

验证提取结果是否：
- ✅ 保留了原文中的关键细节
- ✅ 包含具体的数字、百分比、时间范围
- ✅ 每个条目都有足够的上下文信息
- ✅ 使用了原文中的专业术语

---

## 🐛 故障排查

### 问题 1: API Key 未设置

**错误信息**:
```
IllegalStateException: Must configure either 'spring.ai.google.genai.api-key' or 'spring.ai.google.genai.project-id'
```

**解决方案**:
```bash
export GOOGLE_API_KEY="your-api-key"
```

---

### 问题 2: 测试 PDF 文件未找到

**错误信息**:
```
Test PDF not found, skipping test: /Users/qianping/Documents/Source/axon/axon-arch/Design_doc/Architecture Review_Revival_V3.3.pdf
```

**解决方案**:
确保 PDF 文件存在于指定路径，或修改测试代码中的路径。

---

### 问题 3: API 调用失败

**可能原因**:
- API Key 无效
- 网络连接问题
- API 配额超限

**解决方案**:
1. 检查 API Key 是否有效
2. 检查网络连接
3. 查看 [Google AI Studio](https://aistudio.google.com/) 的配额使用情况

---

## 📝 测试报告

测试完成后，查看测试报告：

```bash
# 查看测试结果
cat target/surefire-reports/*.txt

# 查看详细日志
cat target/surefire-reports/*.xml
```

---

**测试指南版本**: 1.0  
**最后更新**: 2025-11-20

