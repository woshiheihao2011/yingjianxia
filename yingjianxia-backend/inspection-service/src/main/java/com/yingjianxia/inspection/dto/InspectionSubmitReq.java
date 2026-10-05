package com.yingjianxia.inspection.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 验机完成提交请求（检测员端）
 * <p>
 * 兼容前端字段：
 * - grade 可选（前端可能不传，后端根据 items 结果自动推导）
 * - itemNo 可选（前端用 label 字段时，按 items 列表顺序自动编号）
 * - passed 可选（前端不传时，根据 result 内容推导：含"通过/合格/正常"→true）
 */
@Data
@Schema(description = "验机报告完成提交（检测员）")
public class InspectionSubmitReq {

    @Schema(description = "报告ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "报告ID不能为空")
    private Long reportId;

    @Schema(description = "12项检测项结果", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "检测项不能为空")
    @Valid
    private List<ItemResult> items;

    @Schema(description = "综合评级 优/良/合格/不合格（可选，不传时自动推导）")
    private String grade;

    @Schema(description = "备注说明")
    private String remark;

    @Data
    public static class ItemResult {
        @Schema(description = "检测项序号 1-12（可选，不传时按列表顺序自动编号）")
        @JsonAlias({"itemNo", "no", "index"})
        private Integer itemNo;

        @Schema(description = "检测项名称（前端 label 字段兼容）")
        @JsonAlias("label")
        private String label;

        @Schema(description = "检测结果", example = "通过")
        @NotBlank
        @JsonAlias("result")
        private String result;

        @Schema(description = "是否通过（可选，不传时根据 result 推导）")
        @JsonAlias("passed")
        private Boolean passed;

        @Schema(description = "详细参数(JSON键值对文本)")
        private String detail;
    }
}
