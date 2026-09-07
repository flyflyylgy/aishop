package com.cloude.shop.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 登录成功返回
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenVO {

    private String token;

    private Long userId;

    private String username;

    /** member / admin */
    private String role;

    /** 权限码集合（仅管理员登录返回，用于前端菜单/按钮显隐） */
    private Set<String> perms;

    public TokenVO(String token, Long userId, String username, String role) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.role = role;
    }
}
