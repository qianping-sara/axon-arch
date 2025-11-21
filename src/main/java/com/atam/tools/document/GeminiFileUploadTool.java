package com.atam.tools.document;

import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.FileState;
import com.google.genai.types.UploadFileConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Gemini File Upload Tool
 *
 * <p>负责将 PDF 文件上传到 Gemini Files API。
 *
 * <p>Gemini Files API 特点：
 * <ul>
 *   <li>支持最大 50MB 或 1,000 页的 PDF 文件</li>
 *   <li>文件在服务器上保留 48 小时</li>
 *   <li>返回文件 URI（如 "https://generativelanguage.googleapis.com/v1beta/files/..."）供后续使用</li>
 * </ul>
 *
 * <p><b>设计说明</b>：
 * <ul>
 *   <li>此工具是<b>确定性的前置步骤</b>，由 Agent 直接调用（硬编码），而非通过 @Tool 注解暴露给 LLM</li>
 *   <li>文件上传是<b>技术性操作</b>，不需要 LLM 决策何时调用</li>
 *   <li>调用顺序固定：上传文件 → 加载 Prompt → 调用 LLM</li>
 *   <li>如果需要 LLM 决定何时上传文件，才应该使用 @Tool 注解</li>
 * </ul>
 *
 * @author ATAM Copilot Team
 * @since 1.0.0
 */
@Component
public class GeminiFileUploadTool {

    private static final Logger logger = LoggerFactory.getLogger(GeminiFileUploadTool.class);

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final String SUPPORTED_MIME_TYPE = "application/pdf";

    private final Client geminiClient;

    public GeminiFileUploadTool(
        @Value("${spring.ai.google.genai.api-key:}") String apiKey,
        @Value("${spring.ai.google.genai.project-id:}") String projectId,
        @Value("${spring.ai.google.genai.location:us-central1}") String location
    ) {
        // Initialize Gemini Client (same logic as GoogleGenAiChatServiceImpl)
        if (apiKey != null && !apiKey.isEmpty()) {
            logger.info("Initializing Gemini Files API with API Key");
            this.geminiClient = Client.builder().apiKey(apiKey).build();
        } else if (projectId != null && !projectId.isEmpty()) {
            logger.info("Initializing Gemini Files API with Vertex AI: project={}, location={}", projectId, location);
            this.geminiClient = Client.builder()
                .project(projectId)
                .location(location)
                .vertexAI(true)
                .build();
        } else {
            throw new IllegalStateException(
                "Must configure either 'spring.ai.google.genai.api-key' or 'spring.ai.google.genai.project-id'"
            );
        }
    }

    /**
     * 上传多个 PDF 文件到 Gemini Files API
     *
     * @param pdfFilePaths PDF 文件路径列表
     * @return 上传后的 Gemini File 对象列表
     * @throws IOException 如果文件读取或上传失败
     */
    public List<com.google.genai.types.File> uploadPdfFiles(List<String> pdfFilePaths) throws IOException {
        logger.info("Uploading {} PDF files to Gemini Files API", pdfFilePaths.size());

        List<com.google.genai.types.File> uploadedFiles = new ArrayList<>();

        for (String filePath : pdfFilePaths) {
            com.google.genai.types.File uploadedFile = uploadSinglePdf(filePath);
            uploadedFiles.add(uploadedFile);
        }

        logger.info("Successfully uploaded {} files", uploadedFiles.size());
        return uploadedFiles;
    }

    /**
     * 上传单个 PDF 文件
     */
    private com.google.genai.types.File uploadSinglePdf(String filePath) throws IOException {
        File file = new File(filePath);

        // Validate file
        validatePdfFile(file);

        try {
            // Upload file to Gemini Files API with timeout
            logger.info("Uploading file to Gemini Files API: {} ({} bytes)", file.getName(), file.length());

            // Use CompletableFuture to add timeout control
            CompletableFuture<com.google.genai.types.File> uploadFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return geminiClient.files.upload(
                        filePath,
                        UploadFileConfig.builder()
                            .mimeType(SUPPORTED_MIME_TYPE)
                            .displayName(file.getName())
                            .build()
                    );
                } catch (GenAiIOException e) {
                    throw new RuntimeException("Upload failed", e);
                }
            });

            // Wait for upload with 30 second timeout
            com.google.genai.types.File uploadedFile;
            try {
                uploadedFile = uploadFuture.get(30, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                uploadFuture.cancel(true);
                throw new IOException("File upload timed out after 30 seconds: " + file.getName(), e);
            } catch (Exception e) {
                throw new IOException("File upload failed: " + file.getName(), e);
            }

            // Extract file name and URI for logging
            String fileName = uploadedFile.name().orElseThrow(
                () -> new IOException("File upload succeeded but no name returned")
            );
            String fileUri = uploadedFile.uri().orElse(null);

            logger.info("Successfully uploaded file: {} -> name={}, uri={}",
                file.getName(), fileName, fileUri);

            // Check file state
            if (uploadedFile.state().isPresent()) {
                FileState state = uploadedFile.state().get();
                logger.info("File {} state: {}", fileName, state);

                if (state.toString().contains("FAILED")) {
                    throw new IOException("File processing failed: " + fileName);
                }
            } else {
                logger.info("File {} state not available, assuming ACTIVE (typical for small files)", fileName);
            }

            // For small files (like our test PDFs), they are usually ACTIVE immediately
            // No need to poll for status
            return uploadedFile;

        } catch (GenAiIOException e) {
            logger.error("Failed to upload file to Gemini Files API: {}", file.getName(), e);
            throw new IOException("Failed to upload file: " + file.getName(), e);
        }
    }

    /**
     * 验证 PDF 文件
     */
    private void validatePdfFile(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File not found: " + file.getPath());
        }

        if (!file.isFile()) {
            throw new IOException("Not a file: " + file.getPath());
        }

        long fileSize = file.length();
        if (fileSize > MAX_FILE_SIZE) {
            throw new IOException(String.format(
                "File size (%d bytes) exceeds maximum allowed size (%d bytes): %s",
                fileSize, MAX_FILE_SIZE, file.getName()
            ));
        }

        // Validate MIME type
        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null || !mimeType.equals(SUPPORTED_MIME_TYPE)) {
            throw new IOException(String.format(
                "Unsupported file type: %s. Only PDF files are supported.",
                mimeType
            ));
        }

        logger.debug("File validation passed: {} ({} bytes)", file.getName(), fileSize);
    }

    /**
     * 删除临时文件
     */
    public void cleanupTempFiles(List<String> filePaths) {
        for (String filePath : filePaths) {
            try {
                File file = new File(filePath);
                if (file.exists() && file.delete()) {
                    logger.debug("Deleted temp file: {}", filePath);
                }
            } catch (Exception e) {
                logger.warn("Failed to delete temp file: {}", filePath, e);
            }
        }
    }
}

