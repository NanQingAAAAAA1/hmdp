package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.service.IMessageService;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hmdp.utils.RedisConstants.USER_MESSAGE_KEY;

/**
 * 消息服务实现类
 */
@Service
public class MessageServiceImpl implements IMessageService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result getMessages(Integer current, Integer size) {
        // 获取当前用户ID
        Long userId = UserHolder.getUser().getId();
        String key = USER_MESSAGE_KEY + userId;
        
        // 从Redis获取用户消息列表（这里简化实现，实际应该从数据库获取）
        List<Map<String, Object>> messages = new ArrayList<>();
        
        // 模拟一些消息数据
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> message = new HashMap<>();
            message.put("id", i);
            message.put("title", "系统通知 " + i);
            message.put("content", "这是一条系统通知消息，内容为测试消息 " + i);
            message.put("type", "SYSTEM");
            message.put("isRead", i % 2 == 0); // 偶数消息为已读
            message.put("createTime", LocalDateTime.now().minusDays(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            messages.add(message);
        }
        
        // 分页处理
        int start = (current - 1) * size;
        int end = Math.min(start + size, messages.size());
        
        if (start >= messages.size()) {
            return Result.ok(new ArrayList<>());
        }
        
        List<Map<String, Object>> pageMessages = messages.subList(start, end);
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageMessages);
        result.put("total", messages.size());
        result.put("current", current);
        result.put("size", size);
        
        return Result.ok(result);
    }

    @Override
    public Result markMessageAsRead(Long id) {
        // 标记指定消息为已读
        Long userId = UserHolder.getUser().getId();
        String key = USER_MESSAGE_KEY + userId + ":read";
        
        // 将消息ID添加到已读集合中
        stringRedisTemplate.opsForSet().add(key, id.toString());
        
        return Result.ok();
    }

    @Override
    public Result markAllMessagesAsRead() {
        // 标记所有消息为已读
        Long userId = UserHolder.getUser().getId();
        String key = USER_MESSAGE_KEY + userId + ":read";
        
        // 这里简化实现，实际应该更新数据库中的消息状态
        return Result.ok();
    }

    @Override
    public Result deleteMessage(Long id) {
        // 删除指定消息
        Long userId = UserHolder.getUser().getId();
        String key = USER_MESSAGE_KEY + userId + ":deleted";
        
        // 将消息ID添加到已删除集合中
        stringRedisTemplate.opsForSet().add(key, id.toString());
        
        return Result.ok();
    }

    @Override
    public Result getUnreadCount() {
        // 获取未读消息数量
        Long userId = UserHolder.getUser().getId();
        
        // 这里简化实现，返回一个模拟的未读数量
        int unreadCount = 3; // 模拟有3条未读消息
        
        return Result.ok(unreadCount);
    }
} 