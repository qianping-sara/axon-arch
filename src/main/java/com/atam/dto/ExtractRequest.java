package com.atam.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Extract Request DTO
 * 
 * <p>用于业务驱动因素提取的请求参数。
 * 
 * <p>使用场景：
 * <ul>
 *   <li>前端先调用 /api/v1/files/upload 上传文件，获取 fileUris</li>
 *   <li>然后调用 /api/v1/business-drivers/extract 传递 fileUris</li>
 *   <li>支持多个 Agent 复用同一批文件</li>
 * </ul>
 * 
 * @author ATAM Copilot Team
 * @since 1.0.0
 */
@Schema(description = "业务驱动因素提取请求")
public record ExtractRequest(
    
    @Schema(description = "已上传文件的 URI 列表", 
            example = "[\"https://generativelanguage.googleapis.com/v1beta/files/abc123\"]",
            required = true)
    List<String> fileUris
) {
    
    /**
     * 验证请求参数
     */
    public void validate() {
        if (fileUris == null || fileUris.isEmpty()) {
            throw new IllegalArgumentException("fileUris cannot be null or empty");
        }
        
        for (String uri : fileUris) {
            if (uri == null || uri.isBlank()) {
                throw new IllegalArgumentException("fileUri cannot be null or blank");
            }
        }
    }
}

