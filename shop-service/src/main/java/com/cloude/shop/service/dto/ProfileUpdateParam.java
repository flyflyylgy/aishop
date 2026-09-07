package com.cloude.shop.service.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人资料修改参数（全部可选，只改传入字段）
 */
@Data
public class ProfileUpdateParam {

    @Size(max = 32, message = "昵称最长 32 字")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 255, message = "头像 URL 最长 255 字")
    private String icon;
}
