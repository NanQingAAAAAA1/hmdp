package com.hmdp;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;


@SpringBootTest
public class HyperLogLogTest {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testHyperLogLog() {
        // 准备数组，装用户数据
        String[] users = new String[1000];
        int index = 0;

        for (int i = 1; i <= 1000000; i++) {
            // 赋值
            users[index++] = "user_" + i;

            // 每1000条发送一次
            if (i % 1000 == 0) {
                stringRedisTemplate.opsForHyperLogLog().add("hll", users);
                index = 0;  // 重置数组索引
            }
        }

        // 统计数量
        Long size = stringRedisTemplate.opsForHyperLogLog().size("hll");
        System.out.println("size = " + size);
    }
}
