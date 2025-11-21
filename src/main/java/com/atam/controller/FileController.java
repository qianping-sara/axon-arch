package com.atam.controller;

import com.atam.dto.FileMetadata;
import com.atam.tools.document.GeminiFileUploadTool;
import com.google.genai.types.File;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * File Controller
 * 
 * <p>提供独立的文件上传 API，将文件上传到 Gemini Files API。
 * 
 * <p>核心功能：
 * <ul>
 *   <li>接收 PDF 文件上传（支持多文件）</li>
 *   <li>上传到 Gemini Files API</li>
 *   <li>返回文件元数据（包含 URI）供后续 Agent 使用</li>
 *   <li>支持文件复用（48 小时内有效）</li>
 * </ul>
 * 
 * <p>设计理念：
 * <ul>
 *   <li>文件上传与业务逻辑解耦</li>
 *   <li>一次上传，多个 Agent 复用</li>
 *   <li>前端控制文件生命周期</li>
 * </ul>
 * 
 * @author ATAM Copilot Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "File Management", description = "文件上传和管理 API")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final GeminiFileUploadTool fileUploadTool;

    public FileController(GeminiFileUploadTool fileUploadTool) {
        this.fileUploadTool = fileUploadTool;
    }

    /**
     * 上传 PDF 文件到 Gemini Files API
     * 
     * @param files PDF 文件列表（支持多文件上传）
     * @return 文件元数据列表（包含 URI）
     */
    @PostMapping("/upload")
    @Operation(
        summary = "上传 PDF 文件",
        description = "上传 PDF 文件到 Gemini Files API，返回文件元数据（包含 URI）。" +
                     "文件在 Gemini 服务器上保留 48 小时，可被多个 Agent 复用。"
    )
    public ResponseEntity<List<FileMetadata>> uploadFiles(
        @Parameter(description = "PDF 文件列表（最多 5 个文件，每个文件最大 50MB）")
        @RequestParam("files") List<MultipartFile> files
    ) {
        logger.info("Received file upload request with {} files", files.size());

        // Validate files
        if (files == null || files.isEmpty()) {
            logger.warn("No files provided in request");
            return ResponseEntity.badRequest().build();
        }

        if (files.size() > 5) {
            logger.warn("Too many files: {} (max 5)", files.size());
            return ResponseEntity.badRequest().build();
        }

        // Validate each file
        for (MultipartFile file : files) {
            if (file.isEmpty() || file.getSize() == 0) {
                logger.warn("Empty file detected: {}", file.getOriginalFilename());
                return ResponseEntity.badRequest().build();
            }
        }

        List<String> tempFilePaths = new ArrayList<>();

        try {
            // Save uploaded files to temp directory
            for (MultipartFile file : files) {
                String tempFilePath = saveToTempDirectory(file);
                tempFilePaths.add(tempFilePath);
                logger.debug("Saved file to temp: {}", tempFilePath);
            }

            // Upload to Gemini Files API
            List<File> uploadedFiles = fileUploadTool.uploadPdfFiles(tempFilePaths);
            logger.info("Successfully uploaded {} files to Gemini Files API", uploadedFiles.size());

            // Convert to FileMetadata DTOs
            List<FileMetadata> metadata = uploadedFiles.stream()
                .map(FileMetadata::fromGeminiFile)
                .toList();

            // Cleanup temp files
            fileUploadTool.cleanupTempFiles(tempFilePaths);
            logger.debug("Cleaned up {} temp files", tempFilePaths.size());

            return ResponseEntity.ok(metadata);

        } catch (IOException e) {
            logger.error("Failed to upload files", e);
            // Cleanup temp files on error
            fileUploadTool.cleanupTempFiles(tempFilePaths);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 保存上传的文件到临时目录
     */
    private String saveToTempDirectory(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "uploaded-file.pdf";
        }

        // Create temp file with unique name
        String uniqueFilename = UUID.randomUUID() + "-" + originalFilename;
        Path tempFile = Files.createTempFile("atam-upload-", "-" + uniqueFilename);

        // Copy uploaded file to temp location
        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

        logger.debug("Saved uploaded file: {} -> {}", originalFilename, tempFile);

        return tempFile.toString();
    }
}

