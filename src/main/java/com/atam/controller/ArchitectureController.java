package com.atam.controller;

import com.atam.agents.architecture.ArchitectureDesignAgent;
import com.atam.dto.ExtractRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Architecture Controller
 *
 * <p>提供架构模式和坏味道分析的 REST API。
 *
 * <p>核心功能：
 * <ul>
 *   <li>接收已上传文件的 URI（通过 FileController 上传）</li>
 *   <li>流式或同步输出 Markdown 格式的分析结果</li>
 *   <li>支持文件复用（多个 Agent 可使用同一文件）</li>
 * </ul>
 *
 * @author ATAM Copilot Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/architecture")
@Tag(name = "Architecture Analysis", description = "架构模式和坏味道分析 API")
public class ArchitectureController {

    private static final Logger logger = LoggerFactory.getLogger(ArchitectureController.class);

    private final ArchitectureDesignAgent architectureDesignAgent;

    public ArchitectureController(ArchitectureDesignAgent architectureDesignAgent) {
        this.architectureDesignAgent = architectureDesignAgent;
    }

    /**
     * 流式分析架构模式和坏味道（使用已上传文件的 URI）
     *
     * @param request 包含已上传文件 URI 列表的请求
     * @return 流式 Markdown 输出
     */
    @PostMapping(value = "/analyze/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "流式分析架构（推荐）",
        description = "使用已上传文件的 URI 进行架构模式和坏味道分析，流式输出 Markdown 格式结果。" +
                     "文件需先通过 /api/v1/files/upload 接口上传。支持文件复用。"
    )
    public Flux<String> analyzeArchitectureStream(
        @RequestBody ExtractRequest request
    ) {
        logger.info("Received stream architecture analysis request with {} file URIs", request.fileUris().size());

        // Validate request
        try {
            request.validate();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return Flux.error(e);
        }

        // Call Agent with file URIs
        return architectureDesignAgent.analyzeArchitectureStream(request.fileUris());
    }

    /**
     * 同步分析架构模式和坏味道（使用已上传文件的 URI）
     *
     * @param request 包含已上传文件 URI 列表的请求
     * @return 完整的 Markdown 输出
     */
    @PostMapping(value = "/analyze", produces = MediaType.TEXT_PLAIN_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "同步分析架构",
        description = "使用已上传文件的 URI 进行架构模式和坏味道分析，返回完整的 Markdown 格式结果。" +
                     "文件需先通过 /api/v1/files/upload 接口上传。支持文件复用。"
    )
    public ResponseEntity<String> analyzeArchitecture(
        @RequestBody ExtractRequest request
    ) {
        logger.info("Received sync architecture analysis request with {} file URIs", request.fileUris().size());

        // Validate request
        try {
            request.validate();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        try {
            // Call Agent with file URIs
            String result = architectureDesignAgent.analyzeArchitecture(request.fileUris());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to analyze architecture", e);
            return ResponseEntity.internalServerError()
                .body("Failed to analyze architecture: " + e.getMessage());
        }
    }
}

