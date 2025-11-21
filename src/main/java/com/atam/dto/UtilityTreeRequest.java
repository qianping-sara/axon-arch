package com.atam.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Utility Tree Request DTO
 * 
 * <p>用于效用树草稿生成的请求参数。
 * 
 * <p>使用场景：
 * <ul>
 *   <li>前端先调用 /api/v1/business-drivers/extract 提取业务驱动因素</li>
 *   <li>用户审查并批准业务驱动因素</li>
 *   <li>然后调用 /api/v1/business-drivers/utility-tree/generate 传递已批准的业务驱动因素</li>
 *   <li>系统基于业务驱动因素生成效用树草稿（L1/L2 质量属性 + 场景 + 优先级）</li>
 * </ul>
 * 
 * <p>对应 ATAM 步骤 5: Generate Quality Attribute Utility Tree
 * 
 * @author ATAM Copilot Team
 * @since 1.0.0
 */
@Schema(description = "效用树草稿生成请求")
public record UtilityTreeRequest(
    
    @Schema(description = "已批准的业务驱动因素（Markdown 格式）", 
            example = """
                ## 业务目标 (Business Objectives)
                | ID | 目标类别 | 详细描述 | 目标值/测量 | 业务价值/影响 | 优先级 |
                |:---|:---------|:---------|:------------|:--------------|:-------|
                | BO-1 | 提升自动化率 | 实现 40% Full STP | 40% | 减少人工成本 | High |
                
                ## 非功能性需求 (NFRs)
                | ID | 属性 | 详细技术要求 | 关键目的/业务价值 | 测量标准 |
                |:---|:-----|:-------------|:------------------|:---------|
                | NFR-1 | 性能 | 端到端处理 < 5分钟 | 提升用户体验 | - 处理时间 < 5分钟 |
                """,
            required = true)
    String businessDriversMarkdown
) {
    
    /**
     * 验证请求参数
     */
    public void validate() {
        if (businessDriversMarkdown == null || businessDriversMarkdown.isBlank()) {
            throw new IllegalArgumentException("businessDriversMarkdown cannot be null or blank");
        }
        
        // 验证是否包含基本的业务驱动因素结构
        if (!businessDriversMarkdown.contains("业务目标") && 
            !businessDriversMarkdown.contains("Business Objectives")) {
            throw new IllegalArgumentException(
                "businessDriversMarkdown must contain business objectives section");
        }
    }
}

