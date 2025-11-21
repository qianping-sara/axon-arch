package com.atam.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Business Driver Extraction
 * 
 * <p>测试完整的文件上传和提取流程。
 * 
 * @author ATAM Copilot Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class BusinessDriverExtractionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testSyncExtractionEndpoint() throws Exception {
        // Create test PDF file
        MockMultipartFile file = createTestPdfFile("test-sync.pdf");

        // Call sync extraction endpoint
        MvcResult result = mockMvc.perform(multipart("/api/v1/business-drivers/extract")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andReturn();

        // Verify response
        String response = result.getResponse().getContentAsString();
        assertThat(response).isNotNull();
        assertThat(response).isNotEmpty();

        System.out.println("=== Sync Extraction Response ===");
        System.out.println(response);
        System.out.println("=== End of Response ===");
    }

    @Test
    void testStreamExtractionEndpoint() throws Exception {
        // Create test PDF file
        MockMultipartFile file = createTestPdfFile("test-stream.pdf");

        // Call stream extraction endpoint
        // Note: MockMvc has limitations with Flux<String> streaming responses
        // In test environment, the response may be empty because the stream is not fully consumed
        // We just verify the endpoint returns 200 OK status
        mockMvc.perform(multipart("/api/v1/business-drivers/extract/stream")
                .file(file))
            .andExpect(status().isOk());

        // Note: To properly test streaming, we would need to use WebTestClient or TestRestTemplate
        // For now, we just verify the endpoint is accessible and returns success status
    }

    @Test
    void testExtractionWithRealPdf() throws Exception {
        // Load test PDF from resources
        ClassPathResource pdfResource = new ClassPathResource("test-data/Architecture_Review_Revival_V3.3.pdf");

        if (!pdfResource.exists()) {
            System.out.println("Test PDF not found in resources, skipping test");
            return;
        }

        byte[] pdfContent = pdfResource.getInputStream().readAllBytes();
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "Architecture_Review_Revival_V3.3.pdf",
            "application/pdf",
            pdfContent
        );

        // Call sync extraction endpoint
        MvcResult result = mockMvc.perform(multipart("/api/v1/business-drivers/extract")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andReturn();

        // Verify response
        String response = result.getResponse().getContentAsString();
        assertThat(response).isNotNull();
        assertThat(response).isNotEmpty();
        
        // Verify Markdown format
        assertThat(response).contains("#");
        
        // Verify key sections
        assertThat(response.toLowerCase()).containsAnyOf(
            "business", "objective", "constraint", "nfr"
        );

        System.out.println("=== Real PDF Extraction Result ===");
        System.out.println(response);
        System.out.println("=== End of Result ===");
        System.out.println("Total Length: " + response.length() + " characters");
    }

    @Test
    void testExtractionWithMultipleFiles() throws Exception {
        // Create multiple test PDF files
        MockMultipartFile file1 = createTestPdfFile("test-multi-1.pdf");
        MockMultipartFile file2 = createTestPdfFile("test-multi-2.pdf");

        // Call sync extraction endpoint with multiple files
        MvcResult result = mockMvc.perform(multipart("/api/v1/business-drivers/extract")
                .file(file1)
                .file(file2))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andReturn();

        // Verify response
        String response = result.getResponse().getContentAsString();
        assertThat(response).isNotNull();
        assertThat(response).isNotEmpty();

        System.out.println("=== Multi-file Extraction Result ===");
        System.out.println("Total Length: " + response.length() + " characters");
    }

    @Test
    void testExtractionWithNoFiles() throws Exception {
        // Call endpoint without files
        mockMvc.perform(multipart("/api/v1/business-drivers/extract"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testExtractionWithTooManyFiles() throws Exception {
        // Create 6 test files (exceeds limit of 5)
        MockMultipartFile[] files = new MockMultipartFile[6];
        for (int i = 0; i < 6; i++) {
            files[i] = createTestPdfFile("test-too-many-" + i + ".pdf");
        }

        // Call endpoint with too many files
        var request = multipart("/api/v1/business-drivers/extract");
        for (MockMultipartFile file : files) {
            request = request.file(file);
        }

        mockMvc.perform(request)
            .andExpect(status().isBadRequest());
    }

    @Test
    void testExtractionWithEmptyFile() throws Exception {
        // Create empty file
        MockMultipartFile emptyFile = new MockMultipartFile(
            "files",
            "empty.pdf",
            "application/pdf",
            new byte[0]  // Empty content
        );

        // Call endpoint with empty file - should be rejected
        mockMvc.perform(multipart("/api/v1/business-drivers/extract")
                .file(emptyFile))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Empty file not allowed")));
    }

    /**
     * Helper method to create a test PDF file with at least one page
     */
    private MockMultipartFile createTestPdfFile(String filename) throws IOException {
        // Create minimal PDF content with 1 page
        // This is a valid PDF with one blank page
        String minimalPdf = "%PDF-1.4\n" +
                           "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n" +
                           "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n" +
                           "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << >> >>\nendobj\n" +
                           "4 0 obj\n<< /Length 44 >>\nstream\nBT\n/F1 12 Tf\n100 700 Td\n(Test Document) Tj\nET\nendstream\nendobj\n" +
                           "xref\n0 5\n0000000000 65535 f\n0000000009 00000 n\n0000000058 00000 n\n0000000115 00000 n\n0000000229 00000 n\n" +
                           "trailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n322\n%%EOF";

        return new MockMultipartFile(
            "files",
            filename,
            "application/pdf",
            minimalPdf.getBytes()
        );
    }
}

