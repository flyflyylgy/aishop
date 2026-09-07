package com.cloude.shop.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 收货地址参数
 */
@Data
public class AddressParam {

    @NotBlank(message = "收货人不能为空")
    @Size(max = 64, message = "收货人最长 64 字")
    private String receiverName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    @NotBlank(message = "收货地址不能为空")
    @Size(max = 255, message = "地址最长 255 字")
    private String receiverAddr;

    private Boolean isDefault;
}
