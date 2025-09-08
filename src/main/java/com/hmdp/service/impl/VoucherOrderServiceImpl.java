package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();
    
    // 添加线程停止标志
    private final AtomicBoolean running = new AtomicBoolean(true);

    @PostConstruct // 当前类初始化完毕后再去执行
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }
    
    @PreDestroy // 应用关闭时执行
    private void destroy() {
        log.info("正在停止VoucherOrderHandler线程...");
        running.set(false);
        SECKILL_ORDER_EXECUTOR.shutdown();
        try {
            if (!SECKILL_ORDER_EXECUTOR.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                SECKILL_ORDER_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            SECKILL_ORDER_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("VoucherOrderHandler线程已停止");
    }

     private class VoucherOrderHandler implements Runnable {
        String queueName = "stream.orders";
        @Override
         public void run() {
            while (running.get()) {
                try {
                    // 检查Redis连接状态
                    if (!isRedisConnectionAvailable()) {
                        log.warn("Redis连接不可用，等待重连...");
                        Thread.sleep(1000);
                        continue;
                    }
                    
                    // 1.获取消息队列中的订单消息 XREAD GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS streams.order >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                        Consumer.from("g1", "c1"),
                        StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                        StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    // 2.判断消息获取是否成功
                    if (list == null || list.isEmpty()) {
                        // 2.1 如果获取失败，说明没有消息，继续下一次循环
                        continue;
                    }
                    // 3.解析消息中的订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> values = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    // 4.如果获取成功，可以下单
                    handleVoucherOrder(voucherOrder);
                    // 5.ACK确认 SACK stream.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    if (running.get()) {
                        log.error("处理订单异常", e);
                        try {
                            handlePendingList();
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        log.info("线程正在停止，退出处理循环");
                        break;
                    }
                }
            }
        }

         private void handlePendingList() throws InterruptedException {
             while (running.get()) {
                 try {
                     // 检查Redis连接状态
                     if (!isRedisConnectionAvailable()) {
                         log.warn("Redis连接不可用，等待重连...");
                         Thread.sleep(1000);
                         continue;
                     }
                     
                     // 1.获取PendingList中的订单消息 XREAD GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS streams.order 0
                     List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                         Consumer.from("g1", "c1"),
                         StreamReadOptions.empty().count(1),
                         StreamOffset.create(queueName, ReadOffset.from("0"))
                     );
                     // 2.判断消息获取是否成功
                     if (list == null || list.isEmpty()) {
                         // 2.1 如果获取失败，说明PendingList没有消息，结束循环
                         break;
                     }
                     // 3.解析消息中的订单信息
                     MapRecord<String, Object, Object> record = list.get(0);
                     Map<Object, Object> values = record.getValue();
                     VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                     // 4.如果获取成功，可以下单
                     handleVoucherOrder(voucherOrder);
                     // 5.ACK确认 SACK stream.orders g1 id
                     stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                 } catch (Exception e) {
                     if (running.get()) {
                         log.error("处理异常", e);
                         Thread.sleep(20);
                     } else {
                         log.info("线程正在停止，退出PendingList处理");
                         break;
                     }
                 }
             }
         }
     }
     
     /**
      * 检查Redis连接是否可用
      */
     private boolean isRedisConnectionAvailable() {
         try {
             stringRedisTemplate.opsForValue().get("test_connection");
             return true;
         } catch (Exception e) {
             return false;
         }
     }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        // 1.获取用户
        Long userId = voucherOrder.getUserId();
        // 2.创建锁对象
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 3.获取锁对象
        boolean isLock = lock.tryLock();
        // 4.判断是否获取锁成功
        if(!isLock) {
            // 获取锁失败，返回错误或重试
            log.error("不允许重复下单");
            return;
        }
        try {
            // 注意：由于是spring的事务是放在threadLocal中，此时的是多线程，事务会失效
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 获取用户
        Long userId = UserHolder.getUser().getId();
        // 获取订单id
        long orderId = redisIdWorker.nextId("order");
        // 1.执行lua脚本
        Long result = stringRedisTemplate.execute(
            SECKILL_SCRIPT,
            Collections.emptyList(),
            voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );
        int r = result.intValue();
        // 2.判断结果是否为0
        if (r != 0) {
            // 2.1.不为0 ，代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        // 2.2 为0，有购买资格，把下单信息保存到阻塞队列
        VoucherOrder voucherOrder = new VoucherOrder();
        // 2.3 订单id
        voucherOrder.setId(orderId);
        // 2.4 用户id
        voucherOrder.setUserId(userId);
        // 2.5 代金券id
        voucherOrder.setVoucherId(voucherId);
        // 2.6 放入阻塞队列
//        orderTasks.add(voucherOrder);
        // 3.返回订单id
        return Result.ok(orderId);
    }

//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        //获取用户
//        Long userId = UserHolder.getUser().getId();
//        long orderId = redisIdWorker.nextId("order");
//        // 1.执行lua脚本
//        Long result = stringRedisTemplate.execute(
//            SECKILL_SCRIPT,
//            Collections.emptyList(),
//            voucherId.toString(), userId.toString(), String.valueOf(orderId)
//        );
//        int r = result.intValue();
//        // 2.判断结果是否为0
//        if (r != 0) {
//            // 2.1.不为0 ，代表没有购买资格
//            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
//        }
//        // 2.2 为0，有购买资格，把下单信息保存到阻塞队列
//        VoucherOrder voucherOrder = new VoucherOrder();
//        // 2.3 订单id
//        voucherOrder.setId(orderId);
//        // 2.4 用户id
//        voucherOrder.setUserId(userId);
//        // 2.5 代金券id
//        voucherOrder.setVoucherId(voucherId);
//        // 2.6 放入阻塞队列
//        orderTasks.add(voucherOrder);
//        // 3.返回订单id
//        return Result.ok(orderId);
//    }

    @Transactional(rollbackFor = Exception.class)
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        // 5.1.查询订单
        Long count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        // 5.2.判断是否存在
        if (count > 0) {
            // 用户已经购买过了
            log.error("用户已经购买过了");
            return;
        }

        // 6.扣减库存
        log.info("开始扣减库存，voucherId: {}", voucherOrder.getVoucherId());
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1") // set stock = stock - 1
                .eq("voucher_id", voucherOrder.getVoucherId()).gt("stock", 0) // where voucher_id = ? and stock > 0
                .update();
        if (!success) {
            // 扣减失败
            log.error("库存不足，voucherId: {}", voucherOrder.getVoucherId());
            return;
        }
        log.info("库存扣减成功，voucherId: {}", voucherOrder.getVoucherId());
        save(voucherOrder);
        log.info("订单保存成功，orderId: {}", voucherOrder.getId());

        // 验证库存是否真的扣减了
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherOrder.getVoucherId());
        log.info("当前库存: {}, voucherId: {}", seckillVoucher.getStock(), voucherOrder.getVoucherId());
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        // 5.1 查询订单
        Long count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 5.2 判断是否存在
        if (count > 0) {
            // 用户已购买过
            return Result.fail("用户已经购买过！");
        }
        // 6.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1") // set stock = stock - 1
                .eq("voucher_id", voucherId).gt("stock", 0) // where id = ? and stock > 0
                .update();
        if (!success) {
            // 扣减失败
            return Result.fail("库存不足！");
        }
        // 7.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 7.1 订单id
        Long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 7.2 用户id
        voucherOrder.setUserId(userId);
        // 7.3 代金券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        // 8.返回订单id
        return Result.ok(orderId);
    }
}
