package com.yingjianxia.product.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品发布/保存 请求 DTO
 * <p>
 * 用于新增/修改商品（草稿或提交审核）
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "商品创建/更新请求")
public class ProductSaveReq {

    @Schema(description = "商品分类ID", example = "1")
    private Integer categoryId;

    @Schema(description = "分类名称（前端传 String，后端自动映射到 categoryId）", example = "显卡")
    @JsonAlias("category")
    private String categoryName;

    @Schema(description = "商品标题 2-30字", example = "微星 RTX 4070Ti GAMING X TRIO 99新", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标题不能为空")
    @Size(min = 2, max = 30, message = "标题长度需在2-30字符")
    private String title;

    @Schema(description = "副标题（简短卖点，可选）", example = "国行在保，附赠原装配件")
    @Size(max = 50, message = "副标题过长")
    private String subTitle;

    @Schema(description = "售价(元) — 提交审核时必填，草稿可空", example = "5699.00")
    @DecimalMin(value = "0.01", message = "售价必须大于0")
    @DecimalMax(value = "1000000.00", message = "售价过高，请联系客服")
    private BigDecimal price;

    @Schema(description = "原价(划线价)", example = "6499.00")
    private BigDecimal originalPrice;

    @Schema(description = "成色: 1全新/2-99新/3-9成新/4-战损版", example = "2")
    private Integer conditionLevel;

    @Schema(description = "成色名称（前端传 String，后端自动映射到 conditionLevel）", example = "99新")
    @JsonAlias("condition")
    private String conditionName;

    @Schema(description = "购买渠道", example = "京东自营")
    @Size(max = 50, message = "购买渠道过长")
    private String purchaseChannel;

    @Schema(description = "是否箱说全（包装盒+说明书齐全）", example = "true")
    private Boolean hasBox;

    @Schema(description = "所在地", example = "浙江省杭州市")
    @Size(max = 20, message = "所在地过长")
    private String location;

    @Schema(description = "库存数量（默认1）", example = "1")
    @Min(value = 0, message = "库存不能为负数")
    @Max(value = 999, message = "单个商品库存不合理")
    private Integer stock = 1;

    @Schema(description = "库存预警阈值", example = "5")
    private Integer warningThreshold = 5;

    @Schema(description = "商品描述（Markdown或富文本）", example = "## 基本情况\n无拆无修...")
    @Size(max = 10000, message = "描述过长")
    private String description;

    @Schema(description = "是否申请验机服务（显卡/CPU强制=true，其他可选）", example = "false")
    private Boolean needInspection;

    @Schema(description = "是否直接提交审核；false=仅保存草稿", example = "true")
    private Boolean submitForReview;

    @Schema(description = "商品图片URL列表（第一张为主图，最多20张）— 提交审核时必填，草稿可空")
    private List<String> images;

    @Schema(description = "品牌名称（可选）")
    @Size(max = 50, message = "品牌名称过长")
    private String brand;

    @Schema(description = "规格参数")
    @Valid
    private List<SpecItem> specs;

    @Schema(description = "瑕疵列表（外观/功能等瑕疵描述）")
    @Valid
    private List<DefectItem> defects;

    @Schema(description = "是否包邮", example = "true")
    private Boolean shipFree;

    @Schema(description = "运费模板（描述性，如\"全国包邮（顺丰）\"）", example = "全国包邮（顺丰）")
    @Size(max = 100, message = "运费模板说明过长")
    private String shipTemplate;

    @Schema(description = "质保说明", example = "1年店保")
    @Size(max = 50, message = "质保说明过长")
    private String warranty;

    @Schema(description = "售后类型：7-days/15-days/none", example = "7-days")
    @Size(max = 20, message = "售后类型过长")
    private String aftersalesType;

    @Schema(description = "服务承诺标签（展示在买家端）", example = "[\"支持担保交易\",\"7天无理由\"]")
    private List<String> tags;

    /* ========== 嵌套 ========== */

    @Data
    @Schema(name = "规格参数项")
    public static class SpecItem {
        @Schema(description = "参数名", example = "品牌", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "参数名不能为空")
        private String name;

        @Schema(description = "参数值", example = "微星/MSI", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "参数值不能为空")
        private String value;

        @Schema(description = "排序号", defaultValue = "0")
        private Integer sort = 0;
    }

    @Data
    @Schema(name = "瑕疵项")
    public static class DefectItem {
        @Schema(description = "瑕疵部位", example = "外观")
        @NotBlank(message = "瑕疵部位不能为空")
        private String area;

        @Schema(description = "瑕疵详情", example = "右下角轻微磕碰")
        @NotBlank(message = "瑕疵详情不能为空")
        private String detail;
    }
}
