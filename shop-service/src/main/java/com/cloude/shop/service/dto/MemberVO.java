package com.cloude.shop.service.dto;

import lombok.Data;

/**
 * 会员信息 VO（脱敏，不含密码）
 */
@Data
public class MemberVO {

    private Long id;

    private String username;

    private String nickname;

    private String phone;

    private String icon;

    public static MemberVO of(com.cloude.shop.mapper.entity.UmsMember member) {
        MemberVO vo = new MemberVO();
        vo.setId(member.getId());
        vo.setUsername(member.getUsername());
        vo.setNickname(member.getNickname());
        vo.setPhone(member.getPhone());
        vo.setIcon(member.getIcon());
        return vo;
    }
}
