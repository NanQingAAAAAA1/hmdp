package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.service.IMessageService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 消息功能控制器
 */
@RestController
@RequestMapping("/message")
public class MessageController {

    @Resource
    private IMessageService messageService;

    /**
     * 获取用户消息列表
     */
    @GetMapping
    public Result getMessages(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return messageService.getMessages(current, size);
    }

    /**
     * 标记消息为已读
     */
    @PostMapping("/read/{id}")
    public Result markMessageAsRead(@PathVariable Long id) {
        return messageService.markMessageAsRead(id);
    }

    /**
     * 标记所有消息为已读
     */
    @PostMapping("/read/all")
    public Result markAllMessagesAsRead() {
        return messageService.markAllMessagesAsRead();
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{id}")
    public Result deleteMessage(@PathVariable Long id) {
        return messageService.deleteMessage(id);
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread/count")
    public Result getUnreadCount() {
        return messageService.getUnreadCount();
    }
} 