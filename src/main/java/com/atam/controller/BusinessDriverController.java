package com.atam.controller;

import com.atam.agents.business.BusinessDriverAgent;
import com.atam.dto.ExtractRequest;
import com.atam.dto.FileMetadata;
import com.atam.tools.document.GeminiFileUploadTool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business Driver Controller
 * 
 * <p>提供业务驱动因素提取的 REST API。
 * 
 * <p>核心功能：
 * <ul>
 *   <li>接收 PDF 文件上传（支持多文件）</li>
 *   <li>流式输出 Markdown 格式的提取结果</li>
 *   <li>自动清理临时文件</li>
 * </ul>
 * 
 * @author ATAM Copilot Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/business-drivers")
@Tag(name = "Business Driver Extraction", description = "业务驱动因素提取 API")
public class BusinessDriverController {

    private static final Logger logger = LoggerFactory.getLogger(BusinessDriverController.class);

    private final BusinessDriverAgent businessDriverAgent;
    private final GeminiFileUploadTool fileUploadTool;

    public BusinessDriverController(
        BusinessDriverAgent businessDriverAgent,
        GeminiFileUploadTool fileUploadTool
    ) {
        this.businessDriverAgent = businessDriverAgent;
        this.fileUploadTool = fileUploadTool;
    }

    /**
     * 流式提取业务驱动因素（使用已上传文件的 URI）
     *
     * @param request 包含已上传文件 URI 列表的请求
     * @return 流式 Markdown 输出
     */
    @PostMapping(value = "/extract/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "流式提取业务驱动因素（推荐）",
        description = "使用已上传文件的 URI 进行业务驱动因素提取，流式输出 Markdown 格式结果。" +
                     "文件需先通过 /api/v1/files/upload 接口上传。支持文件复用。"
    )
    public Flux<String> extractBusinessDriversStream(
        @RequestBody ExtractRequest request
    ) {
        logger.info("Received stream extraction request with {} file URIs", request.fileUris().size());

        // Validate request
        try {
            request.validate();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return Flux.error(e);
        }

        // Call Agent with file URIs
        return businessDriverAgent.extractBusinessDriversStream(request.fileUris());
    }

    /**
     * 流式提取业务驱动因素（上传 + 提取一步完成）
     *
     * @param files PDF 文件列表（支持多文件上传）
     * @return 流式 Markdown 输出
     * @deprecated 推荐使用分离的上传和提取接口，以支持文件复用
     */
    @Deprecated
    @PostMapping(value = "/extract/stream/upload", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "流式提取业务驱动因素（上传 + 提取）",
        description = "上传 PDF 文档并流式输出 Markdown 格式的业务驱动因素提取结果。" +
                     "此接口已废弃，推荐先调用 /api/v1/files/upload 上传文件，再调用 /extract/stream 进行提取。"
    )
    public Flux<String> extractBusinessDriversStreamWithUpload(
        @Parameter(description = "PDF 文件列表（最多 5 个文件，每个文件最大 50MB）")
        @RequestParam("files") List<MultipartFile> files
    ) {
        logger.info("Received stream extraction request with {} files", files.size());

        // Validate files
        if (files == null || files.isEmpty()) {
            logger.warn("No files provided in request");
            return Flux.error(new IllegalArgumentException("No files provided"));
        }

        if (files.size() > 5) {
            logger.warn("Too many files: {} (max 5)", files.size());
            return Flux.error(new IllegalArgumentException("Maximum 5 files allowed"));
        }

        // Validate each file is not empty
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                logger.warn("Empty file detected: {}", file.getOriginalFilename());
                return Flux.error(new IllegalArgumentException(
                    "Empty file not allowed: " + file.getOriginalFilename()));
            }
            if (file.getSize() == 0) {
                logger.warn("Zero-size file detected: {}", file.getOriginalFilename());
                return Flux.error(new IllegalArgumentException(
                    "Zero-size file not allowed: " + file.getOriginalFilename()));
            }
        }

        List<String> tempFilePaths = new ArrayList<>();

        try {
            // 1. Save uploaded files to temp directory
            for (MultipartFile file : files) {
                String tempFilePath = saveToTempDirectory(file);
                tempFilePaths.add(tempFilePath);
            }

            // 2. Upload to Gemini Files API
            List<com.google.genai.types.File> uploadedFiles = fileUploadTool.uploadPdfFiles(tempFilePaths);
            logger.info("Uploaded {} files to Gemini Files API", uploadedFiles.size());

            // 3. Extract file URIs
            List<String> fileUris = uploadedFiles.stream()
                .map(f -> f.uri().orElseThrow(() -> new RuntimeException("File URI is missing")))
                .toList();

            // 4. Call agent for streaming extraction
            return businessDriverAgent.extractBusinessDriversStream(fileUris)
                .doFinally(signalType -> {
                    // Cleanup temp files after stream completes
                    fileUploadTool.cleanupTempFiles(tempFilePaths);
                    logger.debug("Cleaned up {} temp files", tempFilePaths.size());
                });

        } catch (Exception e) {
            logger.error("Failed to process uploaded files", e);
            // Cleanup temp files on error
            fileUploadTool.cleanupTempFiles(tempFilePaths);
            return Flux.error(new RuntimeException("Failed to process uploaded files", e));
        }
    }

    /**
     * 同步提取业务驱动因素（使用已上传文件的 URI）
     *
     * @param request 包含已上传文件 URI 列表的请求
     * @return 完整的 Markdown 输出
     */
    @PostMapping(value = "/extract", produces = MediaType.TEXT_PLAIN_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "同步提取业务驱动因素（推荐）",
        description = "使用已上传文件的 URI 进行业务驱动因素提取，返回完整的 Markdown 格式结果。" +
                     "文件需先通过 /api/v1/files/upload 接口上传。支持文件复用。"
    )
    public ResponseEntity<String> extractBusinessDrivers(
        @RequestBody ExtractRequest request
    ) {
        logger.info("Received sync extraction request with {} file URIs", request.fileUris().size());

        // Validate request
        try {
            request.validate();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        try {
            // Call Agent with file URIs
            String result = businessDriverAgent.extractBusinessDrivers(request.fileUris());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to extract business drivers", e);
            return ResponseEntity.internalServerError()
                .body("Failed to extract business drivers: " + e.getMessage());
        }
    }

    /**
     * 同步提取业务驱动因素（上传 + 提取一步完成）
     *
     * @param files PDF 文件列表
     * @return 完整的 Markdown 输出
     * @deprecated 推荐使用分离的上传和提取接口，以支持文件复用
     */
    @Deprecated
    @PostMapping(value = "/extract/upload", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
        summary = "同步提取业务驱动因素（上传 + 提取）",
        description = "上传 PDF 文档并返回完整的 Markdown 格式业务驱动因素提取结果。" +
                     "此接口已废弃，推荐先调用 /api/v1/files/upload 上传文件，再调用 /extract 进行提取。"
    )
    public ResponseEntity<String> extractBusinessDriversWithUpload(
        @Parameter(description = "PDF 文件列表（最多 5 个文件，每个文件最大 50MB）")
        @RequestParam("files") List<MultipartFile> files
    ) {
        logger.info("Received sync extraction request with {} files", files.size());

        // Validate files
        if (files == null || files.isEmpty()) {
            logger.warn("No files provided in request");
            return ResponseEntity.badRequest().body("No files provided");
        }

        if (files.size() > 5) {
            logger.warn("Too many files: {} (max 5)", files.size());
            return ResponseEntity.badRequest().body("Maximum 5 files allowed");
        }

        // Validate each file is not empty
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                logger.warn("Empty file detected: {}", file.getOriginalFilename());
                return ResponseEntity.badRequest().body(
                    "Empty file not allowed: " + file.getOriginalFilename());
            }
            if (file.getSize() == 0) {
                logger.warn("Zero-size file detected: {}", file.getOriginalFilename());
                return ResponseEntity.badRequest().body(
                    "Zero-size file not allowed: " + file.getOriginalFilename());
            }
        }

        List<String> tempFilePaths = new ArrayList<>();

        try {
            // 1. Save uploaded files to temp directory
            for (MultipartFile file : files) {
                String tempFilePath = saveToTempDirectory(file);
                tempFilePaths.add(tempFilePath);
            }

            // 2. Upload to Gemini Files API
            List<com.google.genai.types.File> uploadedFiles = fileUploadTool.uploadPdfFiles(tempFilePaths);
            logger.info("Uploaded {} files to Gemini Files API", uploadedFiles.size());

            // 3. Extract file URIs
            List<String> fileUris = uploadedFiles.stream()
                .map(f -> f.uri().orElseThrow(() -> new RuntimeException("File URI is missing")))
                .toList();

            // 4. Call agent for synchronous extraction
            String result = businessDriverAgent.extractBusinessDrivers(fileUris);

            // 5. Cleanup temp files
            fileUploadTool.cleanupTempFiles(tempFilePaths);
            logger.debug("Cleaned up {} temp files", tempFilePaths.size());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to extract business drivers", e);
            // Cleanup temp files on error
            fileUploadTool.cleanupTempFiles(tempFilePaths);
            return ResponseEntity.internalServerError()
                .body("Failed to extract business drivers: " + e.getMessage());
        }
    }

    /**
     * 保存上传文件到临时目录
     */
    private String saveToTempDirectory(MultipartFile file) throws IOException {
        // Create temp directory if not exists
        Path tempDir = Files.createTempDirectory("atam-copilot-uploads-");
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf("."))
            : ".pdf";
        String uniqueFilename = UUID.randomUUID() + extension;
        
        // Save file
        Path targetPath = tempDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        logger.debug("Saved uploaded file to: {}", targetPath);
        return targetPath.toString();
    }

}

