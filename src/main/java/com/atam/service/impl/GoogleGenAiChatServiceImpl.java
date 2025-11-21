package com.atam.service.impl;

import com.atam.service.GeminiChatService;
import com.google.genai.Client;
import com.google.genai.ResponseStream;
import com.google.genai.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;

/**
 * Google GenAI Chat Service Implementation
 * 
 * <p>实现 GeminiChatService 接口，支持两种接入方式：
 * <ul>
 *   <li>Gemini Developer API (API Key 认证) - 开发环境</li>
 *   <li>Vertex AI (Google Cloud 项目认证) - 生产环境</li>
 * </ul>
 * 
 * <p>根据配置自动选择接入方式，无需修改代码。
 * 
 * @author ATAM Copilot Team
 * @since 1.0.0
 */
@Service
public class GoogleGenAiChatServiceImpl implements GeminiChatService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleGenAiChatServiceImpl.class);

    private final Client geminiClient;
    private final String modelName;
    private final String accessMode;
    private final Double temperature;
    private final Integer maxOutputTokens;

    public GoogleGenAiChatServiceImpl(
        @Value("${spring.ai.google.genai.api-key:}") String apiKey,
        @Value("${spring.ai.google.genai.project-id:}") String projectId,
        @Value("${spring.ai.google.genai.location:us-central1}") String location,
        @Value("${spring.ai.google.genai.chat.options.model}") String modelName,
        @Value("${spring.ai.google.genai.chat.options.temperature:0.3}") Double temperature,
        @Value("${spring.ai.google.genai.chat.options.max-output-tokens:8192}") Integer maxOutputTokens
    ) {
        this.modelName = modelName;
        this.temperature = temperature;
        this.maxOutputTokens = maxOutputTokens;

        // Auto-select API mode based on configuration
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            // Mode 1: Gemini Developer API (API Key)
            logger.info("Initializing Gemini Developer API with API Key");
            this.geminiClient = Client.builder().apiKey(apiKey).build();
            this.accessMode = "API_KEY";
        } else if (projectId != null && !projectId.isEmpty()) {
            // Mode 2: Vertex AI (Google Cloud)
            logger.info("Initializing Vertex AI with project: {}, location: {}", projectId, location);
            this.geminiClient = Client.builder()
                .project(projectId)
                .location(location)
                .vertexAI(true)
                .build();
            this.accessMode = "VERTEX_AI";
        } else {
            throw new IllegalStateException(
                "Must configure either 'spring.ai.google.genai.api-key' or 'spring.ai.google.genai.project-id'"
            );
        }

        logger.info("GoogleGenAiChatService initialized: model={}, accessMode={}", modelName, accessMode);
    }

    @Override
    public Flux<String> streamChat(String prompt, List<com.google.genai.types.File> uploadedFiles) {
        logger.debug("Stream chat request: prompt length={}, files={}",
            prompt.length(), uploadedFiles != null ? uploadedFiles.size() : 0);

        try {
            // Build content parts: file URIs + text prompt
            // 根据官方示例：Content.fromParts(Part.fromText(...), Part.fromUri(...))
            List<Part> parts = new ArrayList<>();

            // Add text prompt FIRST (根据某些示例，text 应该在前面)
            parts.add(Part.fromText(prompt));

            // Add file references
            if (uploadedFiles != null && !uploadedFiles.isEmpty()) {
                for (com.google.genai.types.File file : uploadedFiles) {
                    // 关键：必须使用 file.uri() 而不是 file.name()
                    // file.name() 返回 "files/xxx"
                    // file.uri() 返回完整 URL "https://generativelanguage.googleapis.com/v1beta/files/xxx"
                    String fileUri = file.uri().orElseThrow(
                        () -> new IllegalArgumentException("File URI is missing")
                    );
                    String mimeType = file.mimeType().orElse("application/pdf");
                    parts.add(Part.fromUri(fileUri, mimeType));
                    logger.debug("Added file reference: uri={}, mimeType={}", fileUri, mimeType);
                }
            }

            // Build content from parts
            Content content = Content.fromParts(parts.toArray(new Part[0]));

            // Build generation config
            GenerateContentConfig config = GenerateContentConfig.builder()
                .temperature(temperature.floatValue())
                .maxOutputTokens(maxOutputTokens)
                .build();

            // Call Gemini API with streaming
            ResponseStream<GenerateContentResponse> responseStream = geminiClient.models.generateContentStream(
                modelName,
                content,
                config
            );

            // Convert ResponseStream to Reactor Flux
            Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

            // Process stream in a separate thread
            new Thread(() -> {
                try {
                    responseStream.forEach(response -> {
                        if (response.candidates() != null && response.candidates().isPresent()) {
                            List<Candidate> candidates = response.candidates().get();
                            if (!candidates.isEmpty()) {
                                Candidate candidate = candidates.get(0);
                                if (candidate.content() != null && candidate.content().isPresent()) {
                                    Content responseContent = candidate.content().get();
                                    if (responseContent.parts() != null && responseContent.parts().isPresent()) {
                                        for (Part part : responseContent.parts().get()) {
                                            if (part.text() != null && part.text().isPresent()) {
                                                sink.tryEmitNext(part.text().get());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });
                    sink.tryEmitComplete();
                    logger.debug("Stream chat completed");
                } catch (Exception e) {
                    logger.error("Stream chat error", e);
                    sink.tryEmitError(e);
                }
            }).start();

            return sink.asFlux();

        } catch (Exception e) {
            logger.error("Failed to initiate stream chat", e);
            return Flux.error(e);
        }
    }

    @Override
    public String chat(String prompt, List<com.google.genai.types.File> uploadedFiles) {
        logger.debug("Sync chat request: prompt length={}, files={}",
            prompt.length(), uploadedFiles != null ? uploadedFiles.size() : 0);

        try {
            // Build content parts: file URIs + text prompt
            List<Part> parts = new ArrayList<>();

            // Add file references first
            if (uploadedFiles != null && !uploadedFiles.isEmpty()) {
                for (com.google.genai.types.File file : uploadedFiles) {
                    // 关键：必须使用 file.uri() 而不是 file.name()
                    // file.name() 返回 "files/xxx"
                    // file.uri() 返回完整 URL "https://generativelanguage.googleapis.com/v1beta/files/xxx"
                    String fileUri = file.uri().orElseThrow(
                        () -> new IllegalArgumentException("File URI is missing")
                    );
                    String mimeType = file.mimeType().orElse("application/pdf");
                    parts.add(Part.fromUri(fileUri, mimeType));
                }
                logger.debug("Added {} file references to request", uploadedFiles.size());
            }

            // Add text prompt
            parts.add(Part.fromText(prompt));

            // Build content from parts
            Content content = Content.fromParts(parts.toArray(new Part[0]));

            // Debug: log the content structure
            logger.debug("Content parts count: {}", parts.size());
            for (int i = 0; i < parts.size(); i++) {
                Part part = parts.get(i);
                logger.debug("Part {}: text={}, uri={}", i,
                    part.text().orElse(null),
                    part.fileData().map(fd -> fd.fileUri()).orElse(null));
            }

            // Build generation config
            GenerateContentConfig config = GenerateContentConfig.builder()
                .temperature(temperature.floatValue())
                .maxOutputTokens(maxOutputTokens)
                .build();

            // Call Gemini API (synchronous)
            GenerateContentResponse response = geminiClient.models.generateContent(
                modelName,
                content,
                config
            );

            // Extract text from response
            String result = response.text();
            logger.debug("Sync chat completed: response length={}", result.length());
            return result;

        } catch (Exception e) {
            logger.error("Failed to execute sync chat", e);
            throw new RuntimeException("Failed to execute sync chat", e);
        }
    }

    @Override
    public ModelInfo getModelInfo() {
        String version = extractModelVersion(modelName);
        return new ModelInfo(modelName, "Google", accessMode, version);
    }

    /**
     * Extract model version from model name
     */
    private String extractModelVersion(String modelName) {
        if (modelName.contains("3")) {
            return "3.0";
        } else if (modelName.contains("2.5")) {
            return "2.5";
        } else if (modelName.contains("2")) {
            return "2.0";
        }
        return "unknown";
    }
}

