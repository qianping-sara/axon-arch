package com.atam.agents.business;

import com.atam.service.GeminiChatService;
import com.atam.tools.document.GeminiFileUploadTool;
import com.google.genai.types.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Business Driver Agent
 *
 * <p>L2 层 Domain Intelligence Agent，负责从文档中提取业务驱动因素。
 *
 * <p>核心功能：
 * <ul>
 *   <li>读取 PDF 文档（通过 GeminiFileUploadTool）</li>
 *   <li>使用 Prompt 模板指导 AI 提取业务驱动因素</li>
 *   <li>流式输出 Markdown 格式的提取结果</li>
 * </ul>
 *
 * <p>输出格式：Markdown（支持前端流式渲染）
 *
 * <p>对应 ATAM 步骤 2: Present Business Drivers
 *
 * <p><b>工具调用策略</b>：
 * <ul>
 *   <li><b>确定性工具</b>（如 GeminiFileUploadTool）：通过构造函数注入，硬编码调用</li>
 *   <li><b>可选工具</b>（如持久化、知识库查询）：使用 @Tool 注解，由 LLM 决定何时调用</li>
 *   <li>当前实现中，文件上传是必需的前置步骤，因此采用硬编码方式</li>
 * </ul>
 *
 * @author ATAM Copilot Team
 * @since 1.0.0
 */
@Component
public class BusinessDriverAgent {

    private static final Logger logger = LoggerFactory.getLogger(BusinessDriverAgent.class);

    private final GeminiChatService geminiChatService;
    private final GeminiFileUploadTool fileUploadTool;

    public BusinessDriverAgent(
        GeminiChatService geminiChatService,
        GeminiFileUploadTool fileUploadTool
    ) {
        this.geminiChatService = geminiChatService;
        this.fileUploadTool = fileUploadTool;
    }

    /**
     * 流式提取业务驱动因素
     * 
     * @param pdfFilePaths PDF 文件路径列表
     * @return 流式 Markdown 输出
     */
    public Flux<String> extractBusinessDriversStream(List<String> pdfFilePaths) {
        logger.info("Starting business driver extraction for {} files", pdfFilePaths.size());

        // Validate input
        if (pdfFilePaths == null || pdfFilePaths.isEmpty()) {
            logger.error("No PDF files provided");
            return Flux.error(new IllegalArgumentException("No PDF files provided"));
        }

        // Validate each file exists and is not empty
        for (String filePath : pdfFilePaths) {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                logger.error("File not found: {}", filePath);
                return Flux.error(new IllegalArgumentException("File not found: " + filePath));
            }
            if (file.length() == 0) {
                logger.error("Empty file: {}", filePath);
                return Flux.error(new IllegalArgumentException("Empty file not allowed: " + filePath));
            }
            logger.debug("File validation passed: {} ({} bytes)", file.getName(), file.length());
        }

        try {
            // 1. Upload PDF files to Gemini Files API
            List<File> uploadedFiles = fileUploadTool.uploadPdfFiles(pdfFilePaths);
            logger.info("Uploaded {} files to Gemini Files API", uploadedFiles.size());

            // 2. Load Prompt template
            String promptText = loadPromptTemplate("business-driver-extraction-markdown.st");
            logger.debug("Loaded prompt template: {} characters", promptText.length());

            // 3. Call Gemini model (streaming output) with uploaded files
            return geminiChatService.streamChat(promptText, uploadedFiles)
                .doOnComplete(() -> {
                    logger.info("Business driver extraction completed");
                    // Cleanup temp files
                    fileUploadTool.cleanupTempFiles(pdfFilePaths);
                })
                .doOnError(error -> {
                    logger.error("Business driver extraction failed", error);
                    // Cleanup temp files even on error
                    fileUploadTool.cleanupTempFiles(pdfFilePaths);
                });

        } catch (IOException e) {
            logger.error("Failed to upload files", e);
            return Flux.error(e);
        }
    }

    /**
     * 同步提取业务驱动因素（等待完整响应）
     * 
     * @param pdfFilePaths PDF 文件路径列表
     * @return 完整的 Markdown 输出
     */
    public String extractBusinessDrivers(List<String> pdfFilePaths) {
        logger.info("Starting synchronous business driver extraction for {} files", pdfFilePaths.size());

        // Validate input
        if (pdfFilePaths == null || pdfFilePaths.isEmpty()) {
            logger.error("No PDF files provided");
            throw new IllegalArgumentException("No PDF files provided");
        }

        // Validate each file exists and is not empty
        for (String filePath : pdfFilePaths) {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                logger.error("File not found: {}", filePath);
                throw new IllegalArgumentException("File not found: " + filePath);
            }
            if (file.length() == 0) {
                logger.error("Empty file: {}", filePath);
                throw new IllegalArgumentException("Empty file not allowed: " + filePath);
            }
            logger.debug("File validation passed: {} ({} bytes)", file.getName(), file.length());
        }

        try {
            // 1. Upload PDF files to Gemini Files API
            List<File> uploadedFiles = fileUploadTool.uploadPdfFiles(pdfFilePaths);

            // 2. Load Prompt template
            String promptText = loadPromptTemplate("business-driver-extraction-markdown.st");

            // 3. Call Gemini model (synchronous) with uploaded files
            String result = geminiChatService.chat(promptText, uploadedFiles);

            // 4. Cleanup
            fileUploadTool.cleanupTempFiles(pdfFilePaths);

            logger.info("Business driver extraction completed: {} characters", result.length());
            return result;

        } catch (IOException e) {
            logger.error("Failed to extract business drivers", e);
            fileUploadTool.cleanupTempFiles(pdfFilePaths);
            throw new RuntimeException("Business driver extraction failed", e);
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

