package com.yingjianxia.user.dto.profile;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 收货地址新增/修改 请求 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "地址新增/修改请求")
public class AddressReq {

    @Schema(description = "收件人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "收件人不能为空")
    @Size(max = 50, message = "收件人姓名过长")
    @JsonAlias("recipient")
    private String receiverName;

    @Schema(description = "联系电话", example = "13812345678", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    @Schema(description = "省", example = "浙江省", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "省份不能为空")
    @Size(max = 20, message = "省份名称过长")
    private String province;

    @Schema(description = "市", example = "杭州市", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "城市不能为空")
    @Size(max = 20, message = "城市名称过长")
    private String city;

    @Schema(description = "区/县", example = "西湖区")
    @Size(max = 20, message = "区县名称过长")
    private String district;

    @Schema(description = "详细地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 200, message = "详细地址过长")
    private String detail;

    @Schema(description = "是否默认地址")
    @NotNull(message = "默认标记不能为空")
    private Boolean isDefault;

    @Schema(description = "地址标签（家/公司/学校等），可选")
    @Size(max = 20, message = "标签过长")
    private String label;
}
