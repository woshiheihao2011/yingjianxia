package com.yingjianxia.common.core.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页查询响应封装
 * <p>
 * 所有列表查询 API 必须使用此结构返回，保证前端分页组件的统一适配。
 * </p>
 *
 * @param <T> 行数据类型
 * @author 硬件侠后端团队
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页码（从 1 开始）
     */
    private Long pageNum;

    /**
     * 每页大小
     */
    private Long pageSize;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Long totalPages;

    /**
     * 当前页数据
     */
    private List<T> records;

    /* ========== 便捷工厂方法 ========== */

    /**
     * 空结果
     */
    public static <T> PageResult<T> empty(PageQuery query) {
        return new PageResult<>(query.getPageNum(), query.getPageSize(), 0L, 0L, Collections.emptyList());
    }

    /**
     * 构造分页结果（自动计算总页数）
     */
    public static <T> PageResult<T> of(Long pageNum, Long pageSize, Long total, List<T> records) {
        long totalPages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResult<>(pageNum, pageSize, total, totalPages, records);
    }

    /**
     * 基于 MyBatis Plus IPage 适配转换
     */
    public static <T> PageResult<T> of(long pageNum, long pageSize, long total, List<T> records, @SuppressWarnings("unused") Object unused) {
        return of(pageNum, pageSize, total, records);
    }

    /**
     * 数据类型转换（entity -> vo 等场景）
     */
    public <R> PageResult<R> convert(Function<T, R> mapper) {
        List<R> newRecords = this.records == null
                ? Collections.emptyList()
                : this.records.stream().map(mapper).collect(Collectors.toList());
        return new PageResult<>(this.pageNum, this.pageSize, this.total, this.totalPages, newRecords);
    }

    public boolean hasNext() {
        return this.pageNum < this.totalPages;
    }

    public boolean hasPrevious() {
        return this.pageNum > 1 && this.totalPages > 0;
    }
}
