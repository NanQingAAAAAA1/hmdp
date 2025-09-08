package com.hmdp.schedule;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class HotProductTask {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IShopService shopService;

    @Scheduled(fixedDelay = 30000)
    public void cacheHotProducts() {
        String hour = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
        String zsetKey = "product:hot:" + hour;

        Set<ZSetOperations.TypedTuple<String>> hotProducts = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(zsetKey, 0, 99);
        if (hotProducts == null || hotProducts.isEmpty()) {
            return;
        }
        for (ZSetOperations.TypedTuple<String> tuple : hotProducts) {
            if (tuple == null) continue;
            String productId = tuple.getValue();
            Double score = tuple.getScore();
            if (productId == null || score == null || score < 20D) {
                continue;
            }
            Shop shop = shopService.getById(Long.valueOf(productId));
            if (shop == null) continue;
            stringRedisTemplate.opsForValue().set(
                    "cache:shop:" + productId,
                    JSONUtil.toJsonStr(shop),
                    1, TimeUnit.HOURS
            );
        }
    }
}


