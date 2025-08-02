package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设置写入redis的数据格式
 */
@Data
public class RedisData {
    private LocalDateTime expireTime; // 逻辑过期时间
    private Object data; // 过期时间里的数据
}
