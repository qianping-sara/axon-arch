package com.atam.service;

import com.google.genai.types.File;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Gemini Chat Service Interface
 * 
 * <p>抽象层接口，用于隔离业务逻辑和具体的 AI 实现。
 * 支持两种 Gemini 接入方式：
 * <ul>
 *   <li>Gemini Developer API (API Key 认证)</li>
 *   <li>Vertex AI (Google Cloud 项目认证)</li>
 * </ul>
 * 
 * <p>实现类会根据配置自动选择接入方式，业务层无需关心底层实现。
 * 
 * @author ATAM Copilot Team
 * @since 1.0.0
 */
public interface GeminiChatService {

    /**
     * 流式聊天（支持实时输出）
     *
     * @param prompt 提示词
     * @param uploadedFiles Gemini Files API 上传后返回的 File 对象列表（可选）
     * @return 流式响应（Markdown 格式）
     */
    Flux<String> streamChat(String prompt, List<File> uploadedFiles);

    /**
     * 同步聊天（等待完整响应）
     *
     * @param prompt 提示词
     * @param uploadedFiles Gemini Files API 上传后返回的 File 对象列表（可选）
     * @return 完整响应（Markdown 格式）
     */
    String chat(String prompt, List<File> uploadedFiles);

    /**
     * 获取当前模型信息
     * 
     * @return 模型信息
     */
    ModelInfo getModelInfo();

    /**
     * 模型信息记录
     * 
     * @param modelName 模型名称（如 gemini-3-pro-preview, gemini-2.5-flash）
     * @param provider 提供商（Google）
     * @param accessMode 接入方式（API_KEY 或 VERTEX_AI）
     * @param version 模型版本（如 3.0, 2.5）
     */
    record ModelInfo(
        String modelName,
        String provider,
        String accessMode,
        String version
    ) {}
}

