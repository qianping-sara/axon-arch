package com.atam.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * File Metadata DTO
 * 
 * <p>表示已上传到 Gemini Files API 的文件元数据。
 * 
 * <p>核心字段：
 * <ul>
 *   <li>fileId: Gemini 文件 ID（如 "files/abc123"）</li>
 *   <li>uri: 完整的文件 URI（如 "https://generativelanguage.googleapis.com/v1beta/files/abc123"）</li>
 *   <li>displayName: 文件显示名称</li>
 *   <li>sizeBytes: 文件大小（字节）</li>
 *   <li>mimeType: MIME 类型（如 "application/pdf"）</li>
 *   <li>state: 文件状态（ACTIVE, PROCESSING, FAILED）</li>
 * </ul>
 * 
 * <p>使用场景：
 * <ul>
 *   <li>前端上传文件后，保存返回的 fileId 和 uri</li>
 *   <li>调用不同 Agent 时，传递相同的 uri 实现文件复用</li>
 *   <li>文件在 Gemini 服务器上保留 48 小时</li>
 * </ul>
 * 
 * @author ATAM Copilot Team
 * @since 1.0.0
 */
@Schema(description = "已上传文件的元数据")
public record FileMetadata(
    
    @Schema(description = "Gemini 文件 ID", example = "files/abc123xyz")
    String fileId,
    
    @Schema(description = "文件 URI（用于后续 API 调用）", 
            example = "https://generativelanguage.googleapis.com/v1beta/files/abc123xyz")
    String uri,
    
    @Schema(description = "文件显示名称", example = "architecture-doc.pdf")
    String displayName,
    
    @Schema(description = "文件大小（字节）", example = "1024000")
    long sizeBytes,
    
    @Schema(description = "MIME 类型", example = "application/pdf")
    String mimeType,
    
    @Schema(description = "文件状态", example = "ACTIVE", 
            allowableValues = {"ACTIVE", "PROCESSING", "FAILED"})
    String state
) {
    
    /**
     * 从 Gemini File 对象创建 FileMetadata
     */
    public static FileMetadata fromGeminiFile(com.google.genai.types.File file) {
        return new FileMetadata(
            file.name().orElse(""),
            file.uri().orElse(""),
            file.displayName().orElse(""),
            file.sizeBytes().orElse(0L),
            file.mimeType().orElse("application/pdf"),
            file.state().map(state -> state.toString()).orElse("ACTIVE")
        );
    }
    
    /**
     * 检查文件是否处于 ACTIVE 状态（可用于 API 调用）
     */
    public boolean isActive() {
        return "ACTIVE".equals(state);
    }
}

