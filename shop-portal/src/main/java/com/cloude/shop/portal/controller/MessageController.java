package com.cloude.shop.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.api.CommonResult;
import com.cloude.shop.common.component.UserContext;
import com.cloude.shop.mapper.entity.UmsMessage;
import com.cloude.shop.service.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 前台站内信接口（会员）
 */
@Tag(name = "MessageController", description = "站内信")
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "我的消息（isRead:0未读 1已读，不传=全部）")
    @GetMapping("/page")
    public CommonResult<Page<UmsMessage>> page(@RequestParam(required = false) Integer isRead,
                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(
                messageService.page(UserContext.getUserId(), isRead, pageNum, pageSize));
    }

    @Operation(summary = "未读消息数")
    @GetMapping("/unread-count")
    public CommonResult<Map<String, Object>> unreadCount() {
        long count = messageService.unreadCount(UserContext.getUserId());
        return CommonResult.success(Map.of("count", count));
    }

    @Operation(summary = "标记单条已读")
    @PostMapping("/read/{id}")
    public CommonResult<Void> read(@PathVariable Long id) {
        messageService.markRead(UserContext.getUserId(), id);
        return CommonResult.success();
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/read-all")
    public CommonResult<Void> readAll() {
        messageService.markAllRead(UserContext.getUserId());
        return CommonResult.success();
    }
}
