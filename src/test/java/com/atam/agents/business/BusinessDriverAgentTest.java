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

    @Test
    void testGenerateUtilityTreeDraft() {
        // Prepare sample business drivers markdown (simulating step 2 output)
        String businessDriversMarkdown = """
            ## 业务目标 (Business Objectives)

            | ID | 目标类别 | 详细描述 | 目标值/测量 | 业务价值/影响 | 优先级 |
            |:---|:---------|:---------|:------------|:--------------|:-------|
            | BO-1 | 提升自动化率与处理效率 | 实现 40% Full STP（全自动直通处理） | 40% 案件无需人工干预 | 减少人工成本，提升处理速度 | High |
            | BO-2 | 优化体验 | 消除"转椅操作"，提供统一视图 | 理赔员无需切换系统 | 提升工作效率，减少错误 | High |

            ## 约束与依赖 (Constraints & Dependencies)

            | ID | 类型 | 详细描述 | 时间/影响 | 应对策略 |
            |:---|:-----|:---------|:----------|:---------|
            | C-1 | 外部依赖 | 必须集成 BaNCS 核心系统 | 高影响 | 确保 API 稳定性和容错机制 |

            ## 关键非功能性需求 (NFRs)

            | ID | 属性 | 详细技术要求 | 关键目的/业务价值 | 测量标准 |
            |:---|:-----|:-------------|:------------------|:---------|
            | NFR-1 | 性能 | 端到端处理时间 < 5 分钟 | 提升用户体验 | - 处理时间 < 5分钟<br>- 准确率 100% |
            | NFR-2 | 可靠性 | 系统故障时自动降级 | 确保业务连续性 | - 故障恢复时间 < 1分钟 |
            """;

        // Call agent to generate utility tree
        String result = businessDriverAgent.generateUtilityTreeDraft(businessDriversMarkdown);

        // Verify result
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();

        // Verify Markdown format
        assertThat(result).contains("#"); // Should contain headers
        assertThat(result).contains("|"); // Should contain table

        // Verify key sections
        assertThat(result.toLowerCase()).containsAnyOf(
            "utility tree", "效用树",
            "quality attribute", "质量属性"
        );

        // Verify L1 quality attributes (at least some of them)
        assertThat(result).containsAnyOf(
            "性能效率", "Performance",
            "可靠性", "Reliability",
            "可修改性", "Modifiability"
        );

        // Verify priority format
        assertThat(result).containsPattern("\\([HML], [HML]\\)"); // (H, H), (M, L), etc.

        // Verify scenario IDs
        assertThat(result).containsAnyOf("A1", "B1", "C1", "D1"); // Scenario IDs

        System.out.println("=== Utility Tree Generation Result ===");
        System.out.println(result);
        System.out.println("=== End of Result ===");
        System.out.println("Total Length: " + result.length() + " characters");
    }

    @Test
    void testGenerateUtilityTreeDraftStream() {
        // Prepare sample business drivers markdown
        String businessDriversMarkdown = """
            ## 业务目标 (Business Objectives)

            | ID | 目标类别 | 详细描述 | 目标值/测量 | 业务价值/影响 | 优先级 |
            |:---|:---------|:---------|:------------|:--------------|:-------|
            | BO-1 | 提升自动化率 | 实现 40% Full STP | 40% | 减少成本 | High |

            ## 关键非功能性需求 (NFRs)

            | ID | 属性 | 详细技术要求 | 关键目的/业务价值 | 测量标准 |
            |:---|:-----|:-------------|:------------------|:---------|
            | NFR-1 | 性能 | 处理时间 < 5分钟 | 提升体验 | - 时间 < 5分钟 |
            """;

        // Call agent for streaming generation
        Flux<String> resultStream = businessDriverAgent.generateUtilityTreeDraftStream(businessDriversMarkdown);

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
        assertThat(result).contains("|");

        System.out.println("\n=== Stream Utility Tree Generation Complete ===");
        System.out.println("Total Length: " + result.length() + " characters");
    }

}

