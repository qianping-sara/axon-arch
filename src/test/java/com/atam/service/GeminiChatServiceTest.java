package com.atam.service;

import com.atam.service.impl.GoogleGenAiChatServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GeminiChatService
 * 
 * <p>测试 GeminiChatService 的核心功能。
 * 
 * @author ATAM Copilot Team
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("dev")
class GeminiChatServiceTest {

    @Autowired
    private GeminiChatService geminiChatService;

    @Test
    void testServiceInitialization() {
        // Verify service is properly initialized
        assertThat(geminiChatService).isNotNull();
        assertThat(geminiChatService).isInstanceOf(GoogleGenAiChatServiceImpl.class);
    }

    @Test
    void testGetModelInfo() {
        // Get model info
        GeminiChatService.ModelInfo modelInfo = geminiChatService.getModelInfo();

        // Verify model info
        assertThat(modelInfo).isNotNull();
        assertThat(modelInfo.modelName()).isNotEmpty();
        assertThat(modelInfo.provider()).isEqualTo("Google");
        assertThat(modelInfo.accessMode()).isIn("API_KEY", "VERTEX_AI");
        
        System.out.println("Model Info: " + modelInfo);
    }

    @Test
    void testSyncChat() {
        // Simple test prompt
        String prompt = "Say 'Hello, ATAM Copilot!' in one sentence.";

        // Call sync chat
        String response = geminiChatService.chat(prompt, Collections.emptyList());

        // Verify response
        assertThat(response).isNotNull();
        assertThat(response).isNotEmpty();
        assertThat(response.toLowerCase()).contains("hello");
        
        System.out.println("Sync Response: " + response);
    }
}

