package com.yingjianxia.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端用户列表响应
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "管理端用户列表项")
public class AdminUserResp {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号（脱敏：139****1234）")
    private String phone;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "角色列表")
    private List<String> roles;

    @Schema(description = "状态：1=正常 2=冻结 3=封禁")
    private Integer status;

    @Schema(description = "信用分")
    private BigDecimal creditScore;

    @Schema(description = "个人简介")
    private String bio;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
