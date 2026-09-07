package com.cloude.shop.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.mapper.entity.UmsMessage;
import com.cloude.shop.mapper.mapper.UmsMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 站内信服务 —— 关键业务节点给会员发通知。
 * 发送为尽力而为：内部捕获所有异常，绝不影响主交易流程（下单/支付/发货/退款）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    public static final int TYPE_ORDER = 1;
    public static final int TYPE_PAY = 2;
    public static final int TYPE_LOGISTICS = 3;
    public static final int TYPE_AFTER_SALE = 4;
    public static final int TYPE_SYSTEM = 5;

    private final UmsMessageMapper messageMapper;

    /**
     * 发送站内信（失败仅记日志，不抛异常）
     */
    public void send(Long memberId, int type, String title, String content, String bizType, String bizId) {
        if (memberId == null) {
            return;
        }
        try {
            UmsMessage m = new UmsMessage();
            m.setMemberId(memberId);
            m.setType(type);
            m.setTitle(title);
            m.setContent(content);
            m.setBizType(bizType);
            m.setBizId(bizId);
            m.setIsRead(0);
            messageMapper.insert(m);
        } catch (Exception e) {
            log.warn("站内信发送失败 memberId={} title={} cause={}", memberId, title, e.getMessage());
        }
    }

    public Page<UmsMessage> page(Long memberId, Integer isRead, Integer pageNum, Integer pageSize) {
        return messageMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UmsMessage>()
                        .eq(UmsMessage::getMemberId, memberId)
                        .eq(isRead != null, UmsMessage::getIsRead, isRead)
                        .orderByDesc(UmsMessage::getId));
    }

    public long unreadCount(Long memberId) {
        return messageMapper.selectCount(new LambdaQueryWrapper<UmsMessage>()
                .eq(UmsMessage::getMemberId, memberId)
                .eq(UmsMessage::getIsRead, 0));
    }

    public void markRead(Long memberId, Long id) {
        messageMapper.markRead(id, memberId);
    }

    public void markAllRead(Long memberId) {
        messageMapper.markAllRead(memberId);
    }
}
