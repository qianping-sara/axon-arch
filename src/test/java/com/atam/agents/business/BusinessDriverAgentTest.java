package com.atam.agents.business;

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

    @Test
    void testAgentInitialization() {
        // Verify agent is properly initialized
        assertThat(businessDriverAgent).isNotNull();
    }

    @Test
    void testPromptTemplateLoading() throws IOException {
        // This test verifies that the prompt template can be loaded
        // We'll test this indirectly through the agent's methods
        
        // Create a simple test file
        Path tempFile = createTestPdfFile("test-simple.pdf");
        
        try {
            // Call agent (will load prompt template internally)
            String result = businessDriverAgent.extractBusinessDrivers(List.of(tempFile.toString()));
            
            // Verify result is not null
            assertThat(result).isNotNull();
            
            System.out.println("Extraction Result Length: " + result.length());
            
        } finally {
            // Cleanup
            Files.deleteIfExists(tempFile);
        }
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
            // Call agent for synchronous extraction
            String result = businessDriverAgent.extractBusinessDrivers(List.of(tempPdf.toString()));

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
            Files.deleteIfExists(tempPdf);
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
            // Call agent for streaming extraction
            Flux<String> resultStream = businessDriverAgent.extractBusinessDriversStream(List.of(tempPdf.toString()));

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
            Files.deleteIfExists(tempPdf);
        }
    }

    @Test
    void testExtractWithMultiplePdfs() throws IOException {
        // Create multiple test files
        Path tempFile1 = createTestPdfFile("test-multi-1.pdf");
        Path tempFile2 = createTestPdfFile("test-multi-2.pdf");

        try {
            // Call agent with multiple files
            String result = businessDriverAgent.extractBusinessDrivers(
                List.of(tempFile1.toString(), tempFile2.toString())
            );

            // Verify result
            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();

            System.out.println("Multi-file Extraction Result Length: " + result.length());

        } finally {
            // Cleanup
            Files.deleteIfExists(tempFile1);
            Files.deleteIfExists(tempFile2);
        }
    }

    /**
     * Helper method to create a test PDF file with at least one page
     */
    private Path createTestPdfFile(String filename) throws IOException {
        Path tempFile = Files.createTempFile("atam-test-", "-" + filename);

        // Write minimal PDF content with 1 page
        // This is a valid PDF with one blank page
        String minimalPdf = "%PDF-1.4\n" +
                           "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n" +
                           "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n" +
                           "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << >> >>\nendobj\n" +
                           "4 0 obj\n<< /Length 44 >>\nstream\nBT\n/F1 12 Tf\n100 700 Td\n(Test Document) Tj\nET\nendstream\nendobj\n" +
                           "xref\n0 5\n0000000000 65535 f\n0000000009 00000 n\n0000000058 00000 n\n0000000115 00000 n\n0000000229 00000 n\n" +
                           "trailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n322\n%%EOF";
        Files.writeString(tempFile, minimalPdf);

        return tempFile;
    }
}

