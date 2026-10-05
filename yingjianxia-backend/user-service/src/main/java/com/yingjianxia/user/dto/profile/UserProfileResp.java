package com.yingjianxia.user.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 个人中心资料 响应 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "用户资料响应")
public class UserProfileResp {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "手机号(脱敏)", example = "138****5678")
    private String phone;

    @Schema(description = "邮箱(脱敏)")
    private String email;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "个人简介")
    private String bio;

    @Schema(description = "性别 male/female/secret")
    private String gender;

    @Schema(description = "生日 yyyy-MM-dd")
    private String birthday;

    @Schema(description = "常住地区")
    private String region;

    @Schema(description = "信用分 0.0-5.0", example = "4.8")
    private BigDecimal creditScore;

    @Schema(description = "成交笔数")
    private Integer transactionCount;

    @Schema(description = "本月收入")
    private BigDecimal monthlyIncome;

    @Schema(description = "是否优质卖家")
    private Boolean sellerVerified;

    @Schema(description = "实名状态: 0未认证 1已通过 2未通过")
    private Integer realNameStatus;

    @Schema(description = "已开通的店铺ID（未开通为null）")
    private Long shopId;

    @Schema(description = "账号状态: 1正常 2冻结 3封禁")
    private Integer status;

    @Schema(description = "注册时间")
    private LocalDateTime createdAt;
}
