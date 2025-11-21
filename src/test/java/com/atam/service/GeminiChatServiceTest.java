package com.atam.service;

import com.atam.service.impl.GoogleGenAiChatServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

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

    @Test
    void testStreamChat() {
        // Simple test prompt
        String prompt = "Count from 1 to 5, one number per line.";

        // Call stream chat
        Flux<String> responseStream = geminiChatService.streamChat(prompt, Collections.emptyList());

        // Verify streaming response
        StepVerifier.create(responseStream)
            .expectNextMatches(chunk -> chunk != null && !chunk.isEmpty())
            .thenConsumeWhile(chunk -> {
                System.out.print(chunk);
                return true;
            })
            .verifyComplete();
    }

    @Test
    void testStreamChatWithMarkdownOutput() {
        // Test prompt requesting Markdown output
        String prompt = """
            Please output the following in Markdown format:
            
            # Test Document
            
            ## Section 1
            - Item 1
            - Item 2
            
            ## Section 2
            | Column 1 | Column 2 |
            |----------|----------|
            | Value 1  | Value 2  |
            """;

        // Call stream chat
        Flux<String> responseStream = geminiChatService.streamChat(prompt, Collections.emptyList());

        // Collect all chunks
        StringBuilder fullResponse = new StringBuilder();
        
        StepVerifier.create(responseStream)
            .thenConsumeWhile(chunk -> {
                fullResponse.append(chunk);
                return true;
            })
            .verifyComplete();

        // Verify Markdown format
        String response = fullResponse.toString();
        assertThat(response).contains("#");
        assertThat(response).contains("-");
        
        System.out.println("Markdown Response:\n" + response);
    }

    @Test
    void testChatWithEmptyPrompt() {
        // Test with empty prompt
        String prompt = "";

        // Call sync chat
        String response = geminiChatService.chat(prompt, Collections.emptyList());

        // Should still return a response (model might ask for clarification)
        assertThat(response).isNotNull();
        
        System.out.println("Empty Prompt Response: " + response);
    }

    @Test
    void testMultipleSequentialCalls() {
        // Test multiple sequential calls
        String prompt1 = "What is 2 + 2?";
        String prompt2 = "What is 3 + 3?";

        String response1 = geminiChatService.chat(prompt1, Collections.emptyList());
        String response2 = geminiChatService.chat(prompt2, Collections.emptyList());

        assertThat(response1).isNotNull();
        assertThat(response2).isNotNull();
        assertThat(response1).isNotEqualTo(response2);
        
        System.out.println("Response 1: " + response1);
        System.out.println("Response 2: " + response2);
    }
}

