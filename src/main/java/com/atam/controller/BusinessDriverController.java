package com.atam.controller;

import com.atam.agents.business.BusinessDriverAgent;
import com.atam.dto.ExtractRequest;
import com.atam.dto.UtilityTreeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Business Driver Controller
 *
 * <p>提供业务驱动因素提取和效用树生成的 REST API。
 *
 * <p>核心功能：
 * <ul>
 *   <li>步骤 2: 提取业务驱动因素（接收已上传文件的 URI）</li>
 *   <li>步骤 5: 生成效用树草稿（基于已批准的业务驱动因素）</li>
 *   <li>流式或同步输出 Markdown 格式的结果</li>
 *   <li>支持文件复用（多个 Agent 可使用同一文件）</li>
 * </ul>
 *
 * @author ATAM Copilot Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/business-drivers")
@Tag(name = "Business Driver & Utility Tree", description = "业务驱动因素提取和效用树生成 API")
public class BusinessDriverController {

    private static final Logger logger = LoggerFactory.getLogger(BusinessDriverController.class);

    private final BusinessDriverAgent businessDriverAgent;

    public BusinessDriverController(BusinessDriverAgent businessDriverAgent) {
        this.businessDriverAgent = businessDriverAgent;
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
     * 流式生成效用树草稿（基于已批准的业务驱动因素）
     *
     * @param request 包含已批准的业务驱动因素（Markdown 格式）
     * @return 流式 Markdown 输出（效用树表格）
     */
    @PostMapping(value = "/utility-tree/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "流式生成效用树草稿（推荐）",
        description = "基于已批准的业务驱动因素，生成 ATAM 效用树草稿。" +
                     "效用树将业务驱动因素映射为质量属性（L1/L2）和具体场景，并评估优先级。" +
                     "对应 ATAM 步骤 5: Generate Quality Attribute Utility Tree。"
    )
    public Flux<String> generateUtilityTreeStream(
        @RequestBody UtilityTreeRequest request
    ) {
        logger.info("Received stream utility tree generation request");

        // Validate request
        try {
            request.validate();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return Flux.error(e);
        }

        // Call Agent with business drivers markdown
        return businessDriverAgent.generateUtilityTreeDraftStream(request.businessDriversMarkdown());
    }

    /**
     * 同步生成效用树草稿（基于已批准的业务驱动因素）
     *
     * @param request 包含已批准的业务驱动因素（Markdown 格式）
     * @return 完整的 Markdown 输出（效用树表格）
     */
    @PostMapping(value = "/utility-tree/generate", produces = MediaType.TEXT_PLAIN_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "同步生成效用树草稿",
        description = "基于已批准的业务驱动因素，生成 ATAM 效用树草稿。" +
                     "效用树将业务驱动因素映射为质量属性（L1/L2）和具体场景，并评估优先级。" +
                     "对应 ATAM 步骤 5: Generate Quality Attribute Utility Tree。"
    )
    public ResponseEntity<String> generateUtilityTree(
        @RequestBody UtilityTreeRequest request
    ) {
        logger.info("Received sync utility tree generation request");

        // Validate request
        try {
            request.validate();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        try {
            // Call Agent with business drivers markdown
            String result = businessDriverAgent.generateUtilityTreeDraft(request.businessDriversMarkdown());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to generate utility tree", e);
            return ResponseEntity.internalServerError()
                .body("Failed to generate utility tree: " + e.getMessage());
        }
    }
}

