package com.yingjianxia.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入商品结果
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "批量导入商品结果")
public class BatchImportResult {

    @Schema(description = "总行数")
    private int totalRows;

    @Schema(description = "成功导入数")
    private int successCount;

    @Schema(description = "失败数")
    private int failCount;

    @Schema(description = "失败详情")
    private List<Failure> failures = new ArrayList<>();

    @Data
    @Schema(description = "导入失败详情")
    public static class Failure {
        @Schema(description = "行号（从1开始，含表头）")
        private int row;

        @Schema(description = "商品标题（用于定位）")
        private String title;

        @Schema(description = "失败原因")
        private String reason;

        public Failure() {}

        public Failure(int row, String title, String reason) {
            this.row = row;
            this.title = title;
            this.reason = reason;
        }
    }

    /**
     * 记录失败并返回 false（便于链式调用）
     */
    public boolean addFailure(int row, String title, String reason) {
        failures.add(new Failure(row, title, reason));
        failCount++;
        return false;
    }

    /**
     * 记录成功
     */
    public void addSuccess() {
        successCount++;
    }

    /**
     * 设置总行数（不含表头）
     */
    public void setTotalDataRows(int total) {
        this.totalRows = total;
    }
}
