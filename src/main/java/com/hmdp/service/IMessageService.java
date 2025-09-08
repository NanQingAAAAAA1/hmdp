package com.hmdp.service;

import com.hmdp.dto.Result;

/**
 * 消息服务接口
 */
public interface IMessageService {

    /**
     * 获取用户消息列表
     */
    Result getMessages(Integer current, Integer size);

    /**
     * 标记消息为已读
     */
    Result markMessageAsRead(Long id);

    /**
     * 标记所有消息为已读
     */
    Result markAllMessagesAsRead();

    /**
     * 删除消息
     */
    Result deleteMessage(Long id);

    /**
     * 获取未读消息数量
     */
    Result getUnreadCount();
} 