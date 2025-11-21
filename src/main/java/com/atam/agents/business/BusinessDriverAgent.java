package com.atam.agents.business;

import com.atam.service.GeminiChatService;
import com.google.genai.types.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Business Driver Agent
 *
 * <p>L2 层 Domain Intelligence Agent，负责从文档中提取业务驱动因素。
 *
 * <p>核心功能：
 * <ul>
 *   <li>接收已上传文件的 URI（文件由 FileController 独立上传）</li>
 *   <li>使用 Prompt 模板指导 AI 提取业务驱动因素</li>
 *   <li>流式输出 Markdown 格式的提取结果</li>
 * </ul>
 *
 * <p>输出格式：Markdown（支持前端流式渲染）
 *
 * <p>对应 ATAM 步骤:
 * <ul>
 *   <li>步骤 2: Present Business Drivers - 提取业务驱动因素</li>
 *   <li>步骤 5: Generate Quality Attribute Utility Tree - 生成效用树草稿</li>
 * </ul>
 *
 * <p><b>设计理念</b>：
 * <ul>
 *   <li>文件上传与业务逻辑解耦</li>
 *   <li>Agent 只负责分析，不负责文件管理</li>
 *   <li>支持文件复用（多个 Agent 可使用同一文件）</li>
 * </ul>
 *
 * @author ATAM Copilot Team
 * @since 1.0.0
 */
@Component
public class BusinessDriverAgent {

    private static final Logger logger = LoggerFactory.getLogger(BusinessDriverAgent.class);

    private final GeminiChatService geminiChatService;

    public BusinessDriverAgent(GeminiChatService geminiChatService) {
        this.geminiChatService = geminiChatService;
    }

    /**
     * 流式提取业务驱动因素
     *
     * @param fileUris 已上传文件的 URI 列表（从 Gemini Files API 获取）
     * @return 流式 Markdown 输出
     */
    public Flux<String> extractBusinessDriversStream(List<String> fileUris) {
        logger.info("Starting streaming business driver extraction for {} files", fileUris.size());

        // Validate input
        if (fileUris == null || fileUris.isEmpty()) {
            logger.error("No file URIs provided");
            return Flux.error(new IllegalArgumentException("No file URIs provided"));
        }

        try {
            // 1. 构建 File 对象（使用已上传的 URI）
            List<File> files = fileUris.stream()
                .map(uri -> File.builder().uri(uri).build())
                .toList();
            logger.debug("Constructed {} File objects from URIs", files.size());

            // 2. Load Prompt template
            String promptText = loadPromptTemplate("business-driver-extraction-markdown.st");
            logger.debug("Loaded prompt template: {} characters", promptText.length());

            // 3. Call Gemini model (streaming output) with file URIs
            return geminiChatService.streamChat(promptText, files)
                .doOnComplete(() -> logger.info("Streaming business driver extraction completed"))
                .doOnError(error -> logger.error("Streaming business driver extraction failed", error));

        } catch (IOException e) {
            logger.error("Failed to load prompt template", e);
            return Flux.error(e);
        }
    }

    /**
     * 同步提取业务驱动因素（等待完整响应）
     *
     * @param fileUris 已上传文件的 URI 列表（从 Gemini Files API 获取）
     * @return 完整的 Markdown 输出
     */
    public String extractBusinessDrivers(List<String> fileUris) {
        logger.info("Starting synchronous business driver extraction for {} files", fileUris.size());

        // Validate input
        if (fileUris == null || fileUris.isEmpty()) {
            logger.error("No file URIs provided");
            throw new IllegalArgumentException("No file URIs provided");
        }

        try {
            // 1. 构建 File 对象（使用已上传的 URI）
            List<File> files = fileUris.stream()
                .map(uri -> File.builder().uri(uri).build())
                .toList();
            logger.debug("Constructed {} File objects from URIs", files.size());

            // 2. Load Prompt template
            String promptText = loadPromptTemplate("business-driver-extraction-markdown.st");

            // 3. Call Gemini model (synchronous) with file URIs
            String result = geminiChatService.chat(promptText, files);

            logger.info("Synchronous business driver extraction completed: {} characters", result.length());
            return result;

        } catch (IOException e) {
            logger.error("Failed to extract business drivers", e);
            throw new RuntimeException("Business driver extraction failed", e);
        }
    }

    /**
     * 流式生成效用树草稿
     *
     * <p>基于已批准的业务驱动因素（步骤 2 的输出），生成 ATAM 效用树草稿。
     * 效用树将业务驱动因素映射为质量属性（L1/L2）和具体场景。
     *
     * @param businessDriversMarkdown 已批准的业务驱动因素（Markdown 格式）
     * @return 流式 Markdown 输出（效用树表格）
     */
    public Flux<String> generateUtilityTreeDraftStream(String businessDriversMarkdown) {
        logger.info("Starting streaming utility tree generation");

        // Validate input
        if (businessDriversMarkdown == null || businessDriversMarkdown.isBlank()) {
            logger.error("No business drivers markdown provided");
            return Flux.error(new IllegalArgumentException("Business drivers markdown is required"));
        }

        try {
            // 1. Load Prompt template
            String promptTemplate = loadPromptTemplate("utility-tree-generation-markdown.st");
            logger.debug("Loaded utility tree prompt template: {} characters", promptTemplate.length());

            // 2. 将业务驱动因素作为上下文注入 Prompt
            String fullPrompt = promptTemplate + "\n\n## 已批准的业务驱动因素\n\n" + businessDriversMarkdown;
            logger.debug("Constructed full prompt with business drivers context");

            // 3. Call Gemini model (streaming output) - 不需要文件，只需要文本上下文
            return geminiChatService.streamChat(fullPrompt, Collections.emptyList())
                .doOnComplete(() -> logger.info("Streaming utility tree generation completed"))
                .doOnError(error -> logger.error("Streaming utility tree generation failed", error));

        } catch (IOException e) {
            logger.error("Failed to load prompt template", e);
            return Flux.error(e);
        }
    }

    /**
     * 同步生成效用树草稿（等待完整响应）
     *
     * <p>基于已批准的业务驱动因素（步骤 2 的输出），生成 ATAM 效用树草稿。
     * 效用树将业务驱动因素映射为质量属性（L1/L2）和具体场景。
     *
     * @param businessDriversMarkdown 已批准的业务驱动因素（Markdown 格式）
     * @return 完整的 Markdown 输出（效用树表格）
     */
    public String generateUtilityTreeDraft(String businessDriversMarkdown) {
        logger.info("Starting synchronous utility tree generation");

        // Validate input
        if (businessDriversMarkdown == null || businessDriversMarkdown.isBlank()) {
            logger.error("No business drivers markdown provided");
            throw new IllegalArgumentException("Business drivers markdown is required");
        }

        try {
            // 1. Load Prompt template
            String promptTemplate = loadPromptTemplate("utility-tree-generation-markdown.st");

            // 2. 将业务驱动因素作为上下文注入 Prompt
            String fullPrompt = promptTemplate + "\n\n## 已批准的业务驱动因素\n\n" + businessDriversMarkdown;

            // 3. Call Gemini model (synchronous) - 不需要文件，只需要文本上下文
            String result = geminiChatService.chat(fullPrompt, Collections.emptyList());

            logger.info("Synchronous utility tree generation completed: {} characters", result.length());
            return result;

        } catch (IOException e) {
            logger.error("Failed to generate utility tree", e);
            throw new RuntimeException("Utility tree generation failed", e);
        }
    }

    /**
     * 加载 Prompt 模板文件
     *
     * @param templateName 模板文件名（位于 resources/prompts/ 目录）
     * @return 模板内容
     */
    private String loadPromptTemplate(String templateName) throws IOException {
        ClassPathResource resource = new ClassPathResource("prompts/" + templateName);

        if (!resource.exists()) {
            throw new IOException("Prompt template not found: " + templateName);
        }

        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}

