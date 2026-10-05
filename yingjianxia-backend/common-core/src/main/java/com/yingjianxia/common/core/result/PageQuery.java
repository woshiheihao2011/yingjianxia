package com.yingjianxia.common.core.result;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分页查询基础参数
 * <p>
 * 所有分页查询 DTO 继承此类，即可获得统一的分页参数 + 排序能力。
 * </p>
 *
 * @author 硬件侠后端团队
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 最大允许每页条数（防查爆 DB）
     */
    private static final long MAX_PAGE_SIZE = 500L;

    /**
     * 默认每页条数
     */
    private static final long DEFAULT_PAGE_SIZE = 10L;

    /**
     * 当前页码（从 1 开始）
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小为 1")
    private Long pageNum = 1L;

    /**
     * 每页大小
     */
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = MAX_PAGE_SIZE, message = "每页条数不能超过 " + MAX_PAGE_SIZE)
    private Long pageSize = DEFAULT_PAGE_SIZE;

    /**
     * 排序字段列表（可选），格式：[{@code "createTime"}, {@code "-updateTime"}]，
     * 前缀 {@code -} 表示降序，无符号表示升序。
     */
    private List<String> sortBy;

    /**
     * 兼容简单的排序字段传参（单字段）：sortColumn + sortDirection
     */
    private String sortColumn;
    private String sortDirection; // "asc" 或 "desc"

    /* ========== 便捷方法 ========== */

    /**
     * 计算 MySQL LIMIT 偏移量
     */
    public long getOffset() {
        return (Math.max(pageNum - 1, 0)) * pageSize;
    }

    /**
     * 快速构造
     */
    public static PageQuery of(long pageNum, long pageSize) {
        return new PageQuery(pageNum, pageSize, null, null, null);
    }

    /**
     * 是否存在排序
     */
    public boolean hasSort() {
        return (sortBy != null && !sortBy.isEmpty()) || sortColumn != null;
    }
}
