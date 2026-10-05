package com.yingjianxia.inspection.enums;

import com.yingjianxia.common.core.result.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 验机域错误码（3000~3999）
 */
@Getter
@AllArgsConstructor
public enum InspectionErrorCode implements IErrorCode {
    TEMPLATE_NOT_FOUND(3001, "验机模板不存在"),
    REPORT_NOT_FOUND(3002, "验机报告不存在"),
    REPORT_ALREADY_EXISTS(3003, "该商品已存在验机报告，不能重复创建"),
    REPORT_NOT_DONE(3004, "验机报告尚未完成"),
    REPORT_ALREADY_DONE(3005, "验机报告已完成，不可修改"),
    ITEMS_NOT_COMPLETE(3006, "还有检测项未填写完成"),
    SIGNATURE_VERIFY_FAIL(3007, "报告签名校验失败（可能被篡改）"),
    PDF_GENERATE_FAIL(3008, "PDF报告生成失败"),
    INVALID_STATUS_TRANSITION(3009, "报告状态流转不合法"),
    NO_PERMISSION(3010, "您不是该报告的检测员，无法操作"),
    PRODUCT_NOT_MATCH(3011, "商品与该报告不匹配"),
    INVALID_GRADE(3012, "评级参数无效（优/良/合格/不合格）");

    private final Integer code;
    private final String message;
}
