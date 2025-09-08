package com.hmdp.aop;

import com.hmdp.common.annotation.HotProduct;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Aspect
@Component
public class HotProductAspect {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(hotProduct)")
    public Object around(ProceedingJoinPoint joinPoint, HotProduct hotProduct) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0 && args[0] instanceof Long) {
            Long productId = (Long) args[0];
            String hour = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
            String zsetKey = "product:hot:" + hour;
            stringRedisTemplate.opsForZSet().incrementScore(zsetKey, productId.toString(), 1D);
        }
        return joinPoint.proceed();
    }
}


