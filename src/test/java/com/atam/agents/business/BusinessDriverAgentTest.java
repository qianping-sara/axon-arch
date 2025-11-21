package com.atam.agents.business;

import com.atam.tools.document.GeminiFileUploadTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BusinessDriverAgent
 * 
 * <p>测试 BusinessDriverAgent 的核心功能。
 * 
 * @author ATAM Copilot Team
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("dev")
class BusinessDriverAgentTest {

    @Autowired
    private BusinessDriverAgent businessDriverAgent;

    @Autowired
    private GeminiFileUploadTool fileUploadTool;

    @Test
    void testAgentInitialization() {
        // Verify agent is properly initialized
        assertThat(businessDriverAgent).isNotNull();
    }

    @Test
    void testExtractBusinessDriversWithRealPdf() throws IOException {
        // Load test PDF from resources
        ClassPathResource pdfResource = new ClassPathResource("test-data/Architecture_Review_Revival_V3.3.pdf");

        if (!pdfResource.exists()) {
            System.out.println("Test PDF not found in resources, skipping test");
            return;
        }

        // Copy to temp location for processing
        Path tempPdf = Files.createTempFile("atam-test-", ".pdf");
        Files.copy(pdfResource.getInputStream(), tempPdf, StandardCopyOption.REPLACE_EXISTING);

        try {
            // 1. Upload file to Gemini Files API
            List<com.google.genai.types.File> uploadedFiles = fileUploadTool.uploadPdfFiles(List.of(tempPdf.toString()));
            assertThat(uploadedFiles).hasSize(1);

            // 2. Extract file URI
            String fileUri = uploadedFiles.get(0).uri().orElseThrow();

            // 3. Call agent for synchronous extraction
            String result = businessDriverAgent.extractBusinessDrivers(List.of(fileUri));

            // Verify result
            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();

            // Verify Markdown format
            assertThat(result).contains("#"); // Should contain headers

            // Verify key sections
            assertThat(result.toLowerCase()).containsAnyOf(
                "business objective", "业务目标",
                "constraint", "约束",
                "nfr", "非功能"
            );

            System.out.println("=== Extraction Result ===");
            System.out.println(result);
            System.out.println("=== End of Result ===");
            System.out.println("Total Length: " + result.length() + " characters");

        } finally {
            // Cleanup
            fileUploadTool.cleanupTempFiles(List.of(tempPdf.toString()));
        }
    }

    @Test
    void testExtractBusinessDriversStreamWithRealPdf() throws IOException {
        // Load test PDF from resources
        ClassPathResource pdfResource = new ClassPathResource("test-data/Architecture_Review_Revival_V3.3.pdf");

        if (!pdfResource.exists()) {
            System.out.println("Test PDF not found in resources, skipping test");
            return;
        }

        // Copy to temp location for processing
        Path tempPdf = Files.createTempFile("atam-test-stream-", ".pdf");
        Files.copy(pdfResource.getInputStream(), tempPdf, StandardCopyOption.REPLACE_EXISTING);

        try {
            // 1. Upload file to Gemini Files API
            List<com.google.genai.types.File> uploadedFiles = fileUploadTool.uploadPdfFiles(List.of(tempPdf.toString()));
            assertThat(uploadedFiles).hasSize(1);

            // 2. Extract file URI
            String fileUri = uploadedFiles.get(0).uri().orElseThrow();

            // 3. Call agent for streaming extraction
            Flux<String> resultStream = businessDriverAgent.extractBusinessDriversStream(List.of(fileUri));

            // Collect all chunks
            StringBuilder fullResult = new StringBuilder();

            StepVerifier.create(resultStream)
                .thenConsumeWhile(chunk -> {
                    System.out.print(chunk); // Print each chunk as it arrives
                    fullResult.append(chunk);
                    return true;
                })
                .verifyComplete();

            // Verify result
            String result = fullResult.toString();
            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();
            assertThat(result).contains("#");

            System.out.println("\n=== Stream Extraction Complete ===");
            System.out.println("Total Length: " + result.length() + " characters");

        } finally {
            // Cleanup
            fileUploadTool.cleanupTempFiles(List.of(tempPdf.toString()));
        }
    }

}

